package net.simplyadvanced.vitalsigns;

import java.io.ByteArrayOutputStream;
import java.lang.Math;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.simplyadvanced.vitalsigns.heartrate.fft;
import net.simplyadvanced.vitalsigns.heartrate.FastICA_RGB;

public class TestBloodPressure extends Activity {
	private TestBloodPressure _activity;
    private static final String PREFS_NAME = "MyPrefsFile";
    private SharedPreferences settings;

    private RelativeLayout mRelativeLayoutRoot;
    private FrameLayout mFrameLayoutCameraPreview;
    private SurfaceView mSurfaceViewCameraPreview;
    private ImageView mImageViewRectangle0;
    private Camera mCamera;
    static int defaultCameraId = 0;
    
    private TextView mTextViewHeartRate, mTextViewBloodPressure, mTextViewTemperature, mTextViewFace0Coordinates, mTextViewDebug, mTextViewAge, mTextViewSex, mTextViewHeight, mTextViewWeight, mTextViewPosition;
    
    private int previewWidth = 0, previewHeight = 0; // Defined in surfaceChanged()

    /* Heart Rate Related Variables */
    int heartRateFrameLength = 256;
    double[] arrayRed = new double[heartRateFrameLength]; //ArrayList<Double> arrayRed = new ArrayList<Double>();
    double[] arrayGreen = new double[heartRateFrameLength]; //ArrayList<Double> arrayGreen = new ArrayList<Double>();
    double[] arrayBlue = new double[heartRateFrameLength]; //ArrayList<Double> arrayBlue = new ArrayList<Double>();
    int systolicPressure = 0, diastolicPressure = 0, temperature = 0;
    double heartRate = 0;
    int frameNumber = 0;
    
    /* Frame Frequency */
    long samplingFrequency;
    
    /* Face Detection Variables */
    int numberOfFacesCurrentlyDetected = 0;
    int faceLeft0 = 0, faceTop0 = 0, faceRight0 = 0, faceBottom0 = 0;
    int faceLeft1 = 0, faceTop1 = 0, faceRight1 = 0, faceBottom1 = 0;
    int faceLeft2 = 0, faceTop2 = 0, faceRight2 = 0, faceBottom2 = 0;
    
    /* Writing to SD card */
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	String fileDataRed = "";
	String fileDataGreen = "";
	String fileDataBlue = "";
	
	/* Settings */
	boolean displayEnglishUnits = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        _activity = this;
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // Hide the window title
        setContentView(R.layout.activity_test_blood_pressure);

        settings = getSharedPreferences(PREFS_NAME, 0); // Load saved stats // Only done once while app is running

        mTextViewHeartRate        = (TextView) findViewById(R.id.textView0);
        mTextViewBloodPressure    = (TextView) findViewById(R.id.textView1);
        mTextViewTemperature      = (TextView) findViewById(R.id.textView2);
        mTextViewFace0Coordinates = (TextView) findViewById(R.id.textView3);
        mTextViewDebug            = (TextView) findViewById(R.id.textView4);
        mTextViewAge              = (TextView) findViewById(R.id.textViewRightSide0);
        mTextViewSex              = (TextView) findViewById(R.id.textViewRightSide1);
        mTextViewHeight           = (TextView) findViewById(R.id.textViewRightSide2);
        mTextViewWeight           = (TextView) findViewById(R.id.textViewRightSide3);
        mTextViewPosition         = (TextView) findViewById(R.id.textViewRightSide4);

        mRelativeLayoutRoot       = (RelativeLayout) findViewById(R.id.relativeLayoutRoot);
        mFrameLayoutCameraPreview = (FrameLayout) findViewById(R.id.frameLayoutCameraPreview);
        mSurfaceViewCameraPreview = (SurfaceView) findViewById(R.id.surfaceViewCameraPreview);
        mImageViewRectangle0      = (ImageView) findViewById(R.id.imageViewRectangle0);

        mCamera = getCameraInstance();
        mFrameLayoutCameraPreview.addView(new CameraPreview(_activity, mCamera)); // Create and add camera preview to screen
    }

    protected void onResume() {
    	super.onResume();
    	loadPatientEditableStats();
    }
    protected void onPause() {
    	super.onPause();
    }
    protected void onDestroy() {
    	super.onDestroy();
    }

    public void setBloodPressure(double heartRate, int age, String sex, int weight, int height, String position) {
    	double R = 18.31; // Dan's R = 17.6 // Average R = 18.31; // Vascular resistance // Very hard to calculate from person to person
    	double Q = (sex.equalsIgnoreCase("Male") || sex.equalsIgnoreCase("M"))?5:4.5; // Liters per minute of blood through heart
    	double ejectionTime = (position.equalsIgnoreCase("sitting"))?376-1.64*heartRate:354.5-1.23*heartRate; // ()?sitting:supine
    	double bodySurfaceArea = 0.007184*(Math.pow(weight,0.425))*(Math.pow(height,0.725));
        double strokeVolume = -6.6 + 0.25*(ejectionTime-35) - 0.62*heartRate + 40.4*bodySurfaceArea - 0.51*age; // Volume of blood pumped from heart in one beat
        double pulsePressure = strokeVolume / ((0.013*weight - 0.007*age-0.004*heartRate)+1.307);
    	double meanPulsePressure = Q*R;
        
    	systolicPressure = (int) (meanPulsePressure + 2/3*pulsePressure);
    	diastolicPressure = (int) (meanPulsePressure - pulsePressure/3);
    	
    	mTextViewBloodPressure.setText("Blood Pressure: " + systolicPressure + "/" + diastolicPressure);
    	saveSharedPreference("systolicPressure",systolicPressure);
    	saveSharedPreference("diastolicPressure",diastolicPressure);
    }
    
    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        Size mPreviewSize;
        List<Size> mSupportedPreviewSizes;
        
        public CameraPreview(Context context, Camera camera) {
            super(context);
            SurfaceHolder mHolder;
            mCamera = camera;
            
            mHolder = getHolder(); // Install a SurfaceHolder.Callback so we get notified when the underlying surface is created and destroyed
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated, but required on Android versions prior to 3.0
	        
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        }

        public void surfaceCreated(SurfaceHolder holder) { // The Surface has been created, now tell the camera where to draw the preview
            try {
            	mCamera.setPreviewDisplay(holder);
    	        mCamera.startPreview();
            }
            catch (Exception e) { } // Camera is not available (in use or does not exist)
        } // END surfaceCreated()
        public void surfaceDestroyed(SurfaceHolder holder) { // Called right before surface is destroyed
            // Because the CameraDevice object is not a shared resource, it's very important to release it when the activity is paused.
            if (mCamera != null) {
                mCamera.setPreviewCallback(null); // This is for manually added buffers/threads // Use setPreviewCallback() for automatic buffers
                mCamera.stopPreview();
                mCamera.release(); // release the camera for other applications
                mCamera = null;
            }
            Toast.makeText(_activity, "surfaceDestroyed()", Toast.LENGTH_SHORT).show();
        }
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            if(holder.getSurface() == null) { return; } // preview surface does not exist // WAS mHolder
            mCamera.stopPreview(); // stop preview before making changes
        	previewWidth = w;
        	previewHeight = h;
        	
        	mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, w, h);
        	Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.setParameters(parameters);
            requestLayout();
            

        	previewWidth = mPreviewSize.width;
        	previewHeight = mPreviewSize.height;

            try {
            	setCameraDisplayOrientation(_activity, defaultCameraId, mCamera);
    			mCamera.setFaceDetectionListener(new MyFaceDetectionListener());
                mCamera.setPreviewDisplay(holder); // WAS mHolder
                mCamera.startPreview();
                
    		    startFaceDetection(); // start face detection feature
            } catch (Exception e) { } // Error starting camera preview
            
            
            
            
            /* Uncomment below to manually add buffers, aka get ~30 fps */
//          int dataBufferSize= previewHeight * previewWidth * (ImageFormat.getBitsPerPixel(mCamera.getParameters().getPreviewFormat()) / 8);
//          mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//          mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//          mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//          mCamera.addCallbackBuffer(new byte[dataBufferSize]);
            mCamera.setPreviewCallback(new PreviewCallback() { // Gets called for every frame  // For manually added buffers/threads // Use setPreviewCallback() for automatic buffers
        		public void onPreviewFrame(byte[] data, Camera c) { // NOTE: not ran if buffer isn't big enough for data // NOTE: not all devices have cameras that support preview sizes at the same aspect ratio as the device's display
	        		if(numberOfFacesCurrentlyDetected == 0) {
	        			mTextViewFace0Coordinates.setText("Face Rectangle: No Face Detected");
	        			frameNumber = 0;
//				    	mImageViewRectangle0.bringToFront();
//				    	mImageViewRectangle0.setPadding(300, 400, 0, 0);
				    	//mImageViewRectangle0.setPadding(c.getParameters().getPictureSize().width/2-75, c.getParameters().getPictureSize().height/2-100, 0, 0);
	        		} else {
	        			//int top = faceLeft0, right = faceTop0, bottom = faceRight0, left = faceBottom0; // because coordinate systems are different
	        			//int top = faceLeft0+1000, right = faceTop0+1000, bottom = faceRight0+1000, left = faceBottom0+1000; // because coordinate systems are different
	        			//int top = faceLeft0+1000, left = faceTop0+1000, bottom = faceRight0+1000, right = faceBottom0+1000; // because coordinate systems are different and backwards
	        			int left = faceLeft0+1000, top = faceTop0+1000, right = faceRight0+1000, bottom = faceBottom0+1000;
	        			//int left = 50, top = 50, right = 100, bottom = 100; // NOTE: Negative values not accepted
			        	Log.d("DEBUG", "DEBUG: Actual Rect left,top,right,bottom = " + left + "," + top + "," + right + "," + bottom); //
	                	//int smallPreviewWidth = right - left;
	        			//int smallPreviewWidth = left - right; // because coordinate system is different
	        			int smallPreviewWidth = right - left+1; // because coordinate system is different and backwards // 731
	                	int smallPreviewHeight = bottom - top+1; // 1300
						int numberOfPixelsToAnalyze = smallPreviewWidth * smallPreviewHeight; // The number of pixels in the Face Rect
						
						smallPreviewHeight =  smallPreviewHeight * previewHeight / 2000 ;// because backwards // 468
						smallPreviewWidth = smallPreviewWidth * previewWidth / 2000 ; // because backwards // 467
						
						top = top * previewHeight / 2000; // 
						left = left * previewWidth / 2000; // 
						int topEnd = top+smallPreviewHeight; // 
						int leftEnd = left+smallPreviewWidth; // 
						
//				    	mImageViewRectangle0.bringToFront();
				    	mImageViewRectangle0.setPadding(left, top, 0, 0);
				    	mImageViewRectangle0.forceLayout();
				    	//mImageViewRectangle0.invalidate();
						
	        			/** Trying to analyze part of the screen*/
	                	ByteArrayOutputStream outstr = new ByteArrayOutputStream();
			        	Log.d("DEBUG", "DEBUG: PreviewWidth,Height = " + previewWidth + "," + previewHeight); // 540,922
			        	Log.d("DEBUG", "DEBUG: smallPreviewWidth,Height = " + smallPreviewWidth + "," + smallPreviewHeight); // 540,922
			        	Log.d("DEBUG", "DEBUG: Rect left, top, right, bottom = " + top + "," + left + "," + topEnd + "," + leftEnd); // 0,0, 772,1372
	                    Rect rect = new Rect(left, top, leftEnd, topEnd);
	                    YuvImage yuvimage = new YuvImage(data,ImageFormat.NV21,previewWidth,previewHeight,null); // Create YUV image from byte[]
	                    yuvimage.compressToJpeg(rect, 100, outstr);                                              // Convert YUV image to Jpeg // NOTE: changes Rect's size
	                    Bitmap bmp = BitmapFactory.decodeByteArray(outstr.toByteArray(), 0, outstr.size());      // Convert Jpeg to Bitmap

			        	smallPreviewWidth = bmp.getWidth();
			        	smallPreviewHeight = bmp.getHeight();
			        	Log.d("DEBUG", "DEBUG: Bitmap Width,Height = " + smallPreviewWidth + "," + smallPreviewHeight);
			        	
	                    int r = 0, g = 0, b = 0;
	                    int[] pix = new int[numberOfPixelsToAnalyze];
	                    bmp.getPixels(pix, 0, smallPreviewWidth, 0, 0, smallPreviewWidth, smallPreviewHeight);
	                    
	                	for(int i = 0; i < smallPreviewHeight; i++) {
		            		for(int j = 0; j < smallPreviewWidth; j++) {
		                        int index = i * smallPreviewWidth + j;
		                        r += (pix[index] >> 16) & 0xff; //bitwise shifting
		                        g += (pix[index] >> 8) & 0xff;
		                        b += pix[index] & 0xff;
		                        //pix[index] = 0xff000000 | (r << 16) | (g << 8) | b; // to restore the values after RGB modification, use this statement, with adjustment above
		            		}
		            	}
	                    
						r /= numberOfPixelsToAnalyze;
						g /= numberOfPixelsToAnalyze;
						b /= numberOfPixelsToAnalyze;
	        			
				        if(frameNumber < heartRateFrameLength) {
					        mTextViewDebug.setText("RGB: " + r + "," + g + "," + b);
					        if(frameNumber == 0) {
					        	samplingFrequency = System.nanoTime(); // Start time
					        	//Log.d("DEBUG RGB", "DEBUG: samplingFrequency: " + samplingFrequency);
					        }
					        
				        	fileDataRed += r + " "; // a string to be saved on SD card
				        	fileDataGreen += g + " "; // a string to be saved on SD card
				        	fileDataBlue += b + " "; // a string to be saved on SD card
				        	
					        arrayRed[frameNumber] = ((double) r);
					        arrayGreen[frameNumber] = ((double) g);
					        arrayBlue[frameNumber] = ((double) b);
	
					        mTextViewHeartRate.setText("Heart Rate: in " + (heartRateFrameLength-frameNumber) + "..");
					        mTextViewBloodPressure.setText("Blood Pressure: in " + (heartRateFrameLength-frameNumber+1) + ".."); // Shows how long until measurement will display
				        	frameNumber++;
				        }
				        else if(frameNumber == heartRateFrameLength) { // So that these functions don't run every frame preview, just on the 32nd one // TODO add sound when finish
					        samplingFrequency = System.nanoTime() - samplingFrequency; // Minus end time = length of heartRateFrameLength frames
					        double finalSamplingFrequency = samplingFrequency / (double)1000000000; // Length of time to get frames in seconds
				        	finalSamplingFrequency = heartRateFrameLength / finalSamplingFrequency; // Frames per second in seconds
					        
					        FastICA_RGB.preICA(arrayRed, arrayGreen, arrayBlue, heartRateFrameLength, arrayRed, arrayGreen, arrayBlue); // heartRateFrameLength = 300 frames for now
					        double heartRateFrequency = fft.FFT(arrayGreen, heartRateFrameLength,  finalSamplingFrequency);
				        	heartRate = heartRateFrequency * 60;
	
				            mTextViewHeartRate.setText("Heart Rate: " + heartRate);
				        	mTextViewBloodPressure.setText("Blood Pressure: in 0.."); // Just informing the user that BP almost calculated
				        	mTextViewDebug.setText("Fps: " + finalSamplingFrequency);
				            setBloodPressure(heartRate, settings.getInt("age", 25), settings.getString("sex", "Male"), settings.getInt("weight", 160), settings.getInt("height", 70), settings.getString("position", "Sitting"));
					        
				            saveSharedPreference("heartRate",(int)heartRate);
				        	frameNumber++; // Ensures this if-statement is only ran once by making frameNumber one bigger than heartRateLength
				        }
				        else {
				        	// do nothing
				        }
	        		}
			        //mCamera.addCallbackBuffer(data); // "mCamera.addCallbackBuffer(data);" For manually added buffers/threads, aka ~18 fps
				} // END onPreviewFrame()
        	}); // END mCamera.setPreviewCallback()
            
            
            
            
            
            
        } // END surfaceChanged
        
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // We purposely disregard child measurements because act as a
            // wrapper to a SurfaceView that centers the camera preview instead
            // of stretching it.
            final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            setMeasuredDimension(width, height);

            if (mSupportedPreviewSizes != null) {
                mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
            }
        }

		

//        @Override
//        protected void onLayout(boolean changed, int l, int t, int r, int b) {
//            if (changed && getChildCount() > 0) {
//                final View child = getChildAt(0);
//
//                final int width = r - l;
//                final int height = b - t;
//
//                int previewWidth = width;
//                int previewHeight = height;
//                if (mPreviewSize != null) {
//                    previewWidth = mPreviewSize.width;
//                    previewHeight = mPreviewSize.height;
//                }
//
//                // Center the child SurfaceView within the parent.
//                if (width * previewHeight > height * previewWidth) {
//                    final int scaledChildWidth = previewWidth * height / previewHeight;
//                    child.layout((width - scaledChildWidth) / 2, 0,
//                            (width + scaledChildWidth) / 2, height);
//                } else {
//                    final int scaledChildHeight = previewHeight * width / previewWidth;
//                    child.layout(0, (height - scaledChildHeight) / 2,
//                            width, (height + scaledChildHeight) / 2);
//                }
//            }
//        }
        
    } // END class CameraPreview

    /** A safe way to get an instance of the Camera object. Call in onCreate() */
    public static Camera getCameraInstance() {
        int numberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the default camera
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                defaultCameraId = i;
            }
        }
    	
        Camera c = null;
        try {
            c = Camera.open(defaultCameraId); // attempt to get a Camera instance
        }
        catch (Exception e) { } // Camera is not available (in use or does not exist)
        return c; // returns null if camera is unavailable
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    class MyFaceDetectionListener implements Camera.FaceDetectionListener {
	    public void onFaceDetection(Face[] faces, Camera camera) {
	    	numberOfFacesCurrentlyDetected = faces.length;
	        if (numberOfFacesCurrentlyDetected > 0) {
	            faceLeft0   = faces[0].rect.left;
	            faceTop0    = faces[0].rect.top;
	            faceRight0  = faces[0].rect.right;
	            faceBottom0 = faces[0].rect.bottom;
		    	mTextViewFace0Coordinates.setText("Face Rectangle: (" + faceLeft0 + "," + faceTop0 + "), (" + faceRight0 + "," + faceBottom0 + ")");
		    	
		    	mImageViewRectangle0.bringToFront();
		    	mImageViewRectangle0.setPadding(100, 100, 0, 0);
		    	
		    	// Try 4
//		    	Bitmap bitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.RGB_565);
//		        Paint paint = new Paint();
//		        paint.setColor(Color.BLUE);
//		        Canvas canvas = new Canvas(bitmap);
//		        canvas.drawColor(Color.TRANSPARENT);
//		        canvas.drawRect(25, 50, 75, 150, paint);
//		        mImageViewRectangle0.setImageBitmap(bitmap);
		    	
		    	// Try 3
		    	//mRectImage0.setPadding(left0, top0, previewWidth-right0, previewHeight-bottom0);
		    	//mRectImage0.bringToFront();
		    	
		    	// Try 2
//		    	ShapeDrawable rect = new ShapeDrawable(new RectShape());
//		        rect.getPaint().setColor(Color.GREEN);
//		        rect.setBounds(left0, top0, right0, bottom0);
//		        ImageView view1 = new ImageView(_activity);
//		        view1.setImageDrawable(rect);
//		        LinearLayout frame = (LinearLayout)findViewById(R.id.linearLayout1);
//		        frame.addView(view1);
		        
		        // Try 1
		    	//DrawRect drawRect = new DrawRect(_activity, faces);
		    	//setContentView(drawRect);
	        }
//	        if (numberOfFacesCurrentlyDetected > 1) {
//	            int left1   = faces[1].rect.left;
//	            int top1    = faces[1].rect.top;
//	            int right1  = faces[1].rect.right;
//	            int bottom1 = faces[1].rect.bottom;
//		    	mTextViewFace1Coordinates.setText("Face Rectangle: (" + left1 + "," + top1 + "), (" + right1 + "," + bottom1 + ")");
//	        }
//	        if (numberOfFacesCurrentlyDetected > 2) {
//	            int left2   = faces[2].rect.left;
//	            int top2    = faces[2].rect.top;
//	            int right2  = faces[2].rect.right;
//	            int bottom2 = faces[2].rect.bottom;
//		    	mTextViewFace2Coordinates.setText("Face Rectangle: (" + left2 + "," + top2 + "), (" + right2 + "," + bottom2 + ")");
//	        }
	    }
	}
	public void startFaceDetection() {
	    // Try starting Face Detection
	    Camera.Parameters params = mCamera.getParameters();

	    // start face detection only *after* preview has started
	    if (params.getMaxNumDetectedFaces() > 0) {
	    	//Toast.makeText(_activity, "Max Num Faces Allows: " + params.getMaxNumDetectedFaces(), Toast.LENGTH_LONG).show();
	        // camera supports face detection, so can start it:
	        mCamera.startFaceDetection();
	    }
	}
    
	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.2;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

	
	
	
	
	
	
	
	
	
	

    public void onClickGoToEditStats(View v) {
    	startActivity(new Intent(_activity, EditStats.class));
	}
	public void onClickGoToTemperature(View v) {
    	startActivity(new Intent(_activity, AddTemperature.class));
	}
	
	private void loadPatientEditableStats() {
		displayEnglishUnits = settings.getBoolean("displayEnglishUnits", true);
        if(displayEnglishUnits) {
	        mTextViewAge.setText("Age: " + settings.getInt("age", 23));
	        mTextViewSex.setText("Sex: " + settings.getString("sex", "Male"));
	        mTextViewWeight.setText("Weight: " + settings.getInt("weight", 160) + " pounds");
	        mTextViewHeight.setText("Height: " + settings.getInt("height", 75) + " inches");
	        mTextViewPosition.setText("Position: " + settings.getString("position", "Sitting"));
	        mTextViewTemperature.setText("Temperature: " + settings.getString("temperature", " Click to add.."));
        } else {
	        mTextViewAge.setText("Age: " + settings.getInt("age", 23));
	        mTextViewSex.setText("Sex: " + settings.getString("sex", "Male"));
	        mTextViewWeight.setText("Weight: " + settings.getInt("weight", 73) + " kg");
	        mTextViewHeight.setText("Height: " + settings.getInt("height", 75) + " cm");
	        mTextViewPosition.setText("Position: " + settings.getString("position", "Sitting"));
	        mTextViewTemperature.setText("Temperature: " + settings.getString("temperature", " Click to add.."));
        }
	}
    
	private void saveSharedPreference(String key, int value) {
    	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
    	editor.putInt(key, value);
    	editor.commit(); // This line saves the edits
	}
	
    
} // END class TestBloodPressure
