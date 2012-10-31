package net.simplyadvanced.vitalsigns;

import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.Math;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

import net.simplyadvanced.vitalsigns.heartrate.fft;
import net.simplyadvanced.vitalsigns.heartrate.FastICA_RGB;

public class TestBloodPressure extends Activity {
	private TestBloodPressure _activity;
	TextView mTextViewAge, mTextViewSex, mTextViewWeight, mTextViewHeight, mTextViewPosition;
	TextView mTextViewBloodPressure, mTextViewHeartRate;
	TextView mBlue, mDebug;
    public static final String PREFS_NAME = "MyPrefsFile";
    private Camera mCamera;
    private CameraPreview mPreview;
    SharedPreferences settings;
    FrameLayout preview;
    int previewWidth = 0, previewHeight = 0; // Defined in surfaceChanged()

    /* Heart Rate Related Variables */
    int heartRateFrameLength = 300;
    double[] arrayRed = new double[heartRateFrameLength]; //ArrayList<Double> arrayRed = new ArrayList<Double>();
    double[] arrayGreen = new double[heartRateFrameLength]; //ArrayList<Double> arrayGreen = new ArrayList<Double>();
    double[] arrayBlue = new double[heartRateFrameLength]; //ArrayList<Double> arrayBlue = new ArrayList<Double>();
    double[] outRed = new double[heartRateFrameLength];
    double[] outGreen = new double[heartRateFrameLength];
    double[] outBlue = new double[heartRateFrameLength];
    int systolicPressure = 0, diastolicPressure = 0, temperature = 0;
    double heartRate = 0;
    short frameNumber = 0;
    
    /*Frame Frequency*/
    long samplingFrequency;
    
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

        mTextViewAge = (TextView) findViewById(R.id.textViewAge); // Connects variables here to id's in xml, must be done in order to access id's in the layout (xml)
        mTextViewSex = (TextView) findViewById(R.id.textViewSex);
        mTextViewWeight = (TextView) findViewById(R.id.textViewWeight);
        mTextViewHeight = (TextView) findViewById(R.id.textViewHeight);
        mTextViewPosition = (TextView) findViewById(R.id.textViewPosition);
        mTextViewBloodPressure = (TextView) findViewById(R.id.textViewBloodPressure);
        mTextViewHeartRate = (TextView) findViewById(R.id.textViewHeartRate);
        mBlue = (TextView) findViewById(R.id.blue);
        mDebug = (TextView) findViewById(R.id.debug);

        settings = getSharedPreferences(PREFS_NAME, 0); // Load saved stats // Only done once while app is running
        loadPatientEditableStats(); // Show saved patient stats: age, sex, weight, height, position
        checkMediaAvailability(); // Check to see if sd card is available to write, using mExternalStorageAvailable and mExternalStorageWriteable

    	mCamera = getCameraInstance(); // Create an instance of Camera
    	
        mPreview = new CameraPreview(this, mCamera); // Create our Preview view and set it as the content of our activity
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    protected void onResume() {
    	super.onResume();
    	loadPatientEditableStats();
    }
    protected void onPause() {
    	super.onPause();
    	mCamera.setPreviewCallback(null);
    	mCamera.stopPreview();
    	//tempReleaseCamera();
    }
    protected void onDestroy() {
    	super.onDestroy();
    	releaseCamera();
    }
    
    public void setBloodPressure(double heartRate, int age, String sex, int weight, int height, String position) {
    	double R = 17.6; // Dan's R // Average R = 18.31; // Vascular resistance // Very hard to calculate from person to person
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

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    
    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private static final String TAG = "Exception";
		private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            mHolder = getHolder(); // Install a SurfaceHolder.Callback so we get notified when the underlying surface is created and destroyed
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated setting, but required on Android versions prior to 3.0
        }

        public void surfaceCreated(SurfaceHolder holder) { // The Surface has been created, now tell the camera where to draw the preview
        	//previewWidth = mCamera.getParameters().getPreviewSize().width;
        	//previewHeight = mCamera.getParameters().getPreviewSize().height;

        	/** Uncomment (and setPreviewCallbackWithBuffer() and uncomment "c.addCallbackBuffer(data)") to get ~30 fps instead of ~15*/
//        	int dataBufferSize = (int)(previewWidth*previewHeight*(ImageFormat.getBitsPerPixel(mCamera.getParameters().getPreviewFormat())/8.0)); //460800
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//        	mCamera.addCallbackBuffer(new byte[dataBufferSize]);
        	
        	mCamera.setPreviewCallback(new PreviewCallback() { // Gets called for every frame
        		public void onPreviewFrame(byte[] data, Camera c) {
					//int centerX = (previewWidth / 2), centerY = (previewHeight / 2);
                	previewWidth = mCamera.getParameters().getPreviewSize().width;
                	previewHeight = mCamera.getParameters().getPreviewSize().height;
                	int left = 50, top = 50, right = 100, bottom = 100; // Edit wanted coordinates here.
                	int smallPreviewWidth = right - left;
                	int smallPreviewHeight = bottom - top;
                	
//                	byte[] dataSelection = new byte[smallPreviewWidth*smallPreviewHeight * 8];
//                	Log.d("DEBUG RGB", "DEBUG: dataSelection.length: " + dataSelection.length);
//                	
//                	int dataSelectionCount = 0;
//                	for(int i = top; i < bottom; i++) {
//                    	Log.d("DEBUG RGB", "DEBUG: dataSelectionCount: " + dataSelectionCount);
//                		for(int j = left; j < right; j++) {
//                			if(i == bottom-1) Log.d("DEBUG RGB", "DEBUG: dataSelectionCount: " + dataSelectionCount);
//                			dataSelection[dataSelectionCount++] = data[i*smallPreviewWidth+j];
//                		}
//                	}
        			
        			/** Trying to analyze part of the screen*/
                	ByteArrayOutputStream outstr = new ByteArrayOutputStream();
                    Rect rect = new Rect(left, top, right, bottom);
                    YuvImage yuvimage = new YuvImage(data,ImageFormat.NV21,previewWidth,previewHeight,null);
                    yuvimage.compressToJpeg(rect, 100, outstr);
                    Bitmap bmp = BitmapFactory.decodeByteArray(outstr.toByteArray(), 0, outstr.size());
					
                    int r = 0, g = 0, b = 0;
                    int[] pix = new int[smallPreviewWidth * smallPreviewHeight];
                    bmp.getPixels(pix, 0, smallPreviewWidth, 0, 0, smallPreviewWidth, smallPreviewHeight);
                    
                	for(int i = 0; i < smallPreviewHeight; i++) {
	            		for(int j = 0; j < smallPreviewWidth; j++) {
	                        int index = i * smallPreviewWidth + j;
	                        r += (pix[index] >> 16) & 0xff;     //bitwise shifting
	                        g += (pix[index] >> 8) & 0xff;
	                        b += pix[index] & 0xff;
	                        //pix[index] = 0xff000000 | (r << 16) | (g << 8) | b; // to restore the values after RGB modification, use this statement, with adjustment above
	            		}
	            	}
                    
                    
					int numberOfPixelsToAnalyze = smallPreviewWidth * smallPreviewHeight;
//					for(int k = 0; k < numberOfPixelsToAnalyze; k++) {
//						r += Color.red(pixels[k]);   //1.164(Y-16)                + 2.018(U-128);
//						g += Color.green(pixels[k]); //1.164(Y-16) - 0.813(V-128) - 0.391(U-128);
//						b += Color.blue(pixels[k]);  //1.164(Y-16) + 1.596(V-128);
//					}
					r /= numberOfPixelsToAnalyze;
					g /= numberOfPixelsToAnalyze;
					b /= numberOfPixelsToAnalyze;
					
		            //Camera.Parameters parameters = mCamera.getParameters();
		            //int[] previewFPSRange = new int[2];
		            //parameters.getPreviewFpsRange(previewFPSRange); // Android API 9+
        			
			        mBlue.setText("RGB: " + r + "," + g + "," + b); // YCbCr_420_SP (NV21) format

			        if(frameNumber == 0) {
			        	samplingFrequency = System.nanoTime(); // Start time
			        	Log.d("DEBUG RGB", "DEBUG: samplingFrequency: " + samplingFrequency);
			        }
			        
			        if(frameNumber < heartRateFrameLength) {
			        	fileDataRed += r + " "; // a string
			        	fileDataGreen += g + " "; // a string
			        	fileDataBlue += b + " "; // a string
				        arrayRed[frameNumber] = ((double) r);
				        arrayGreen[frameNumber] = ((double) g);
				        arrayBlue[frameNumber] = ((double) b);
				        mTextViewBloodPressure.setText("Blood Pressure: in " + (heartRateFrameLength-frameNumber+1) + ".."); // Shows how long until measurement will display
				        mTextViewHeartRate.setText("Heart Rate: in " + (heartRateFrameLength-frameNumber) + "..");
			        	frameNumber++;
			        }
			        else if(frameNumber == heartRateFrameLength) { // So that these functions don't run every frame preview, just on the 32nd one // TODO add sound when finish
				        samplingFrequency = System.nanoTime() - samplingFrequency; // Minus end time = length of heartRateFrameLength frames
				        double finalSamplingFrequency = samplingFrequency / (double)1000000000; // Length of time to get frames in seconds
			        	finalSamplingFrequency = heartRateFrameLength / finalSamplingFrequency; // Frames per second in seconds
				        
				        FastICA_RGB.preICA(arrayRed, arrayGreen, arrayBlue, heartRateFrameLength, arrayRed, arrayGreen, arrayBlue); // heartRateFrameLength = 300 frames for now
				        double heartRateFrequency = fft.FFT(arrayGreen, heartRateFrameLength,  finalSamplingFrequency);
			        	Log.d("DEBUG RGB", "DEBUG: finalSamplingFrequency: " + finalSamplingFrequency);
			        	heartRate = heartRateFrequency * 60;

			            mTextViewHeartRate.setText("Heart Rate: " + heartRate);
			        	mTextViewBloodPressure.setText("Blood Pressure: in 0.."); // Just informing the user that BP almost calculated
			        	mDebug.setText("Fps: " + finalSamplingFrequency);
			            setBloodPressure(heartRate, settings.getInt("age", 25), settings.getString("sex", "Male"), settings.getInt("weight", 160), settings.getInt("height", 70), settings.getString("position", "Sitting"));
				        
			            saveSharedPreference("heartRate",(int)heartRate);
				        writeToTextFile(fileDataRed, "red"); // file located root/VitalSigns
				        writeToTextFile(fileDataGreen, "green"); // file located root/VitalSigns
				        writeToTextFile(fileDataBlue, "blue"); // file located root/VitalSigns
			        	frameNumber++; // Ensures this if-statement is only ran once by making frameNumber one bigger than heartRateLength
			        }
			        else {
			        	// do nothing
			        }
			        
			        //c.addCallbackBuffer(data);
				} // END onPreviewFrame()
        	});
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        	// Surface will be destroyed when we return, so stop the preview.
            // Because the CameraDevice object is not a shared resource, it's very important to release it when the activity is paused.
            //mCamera.setPreviewCallback(null);
            //mCamera.stopPreview();
            //releaseCamera(); // same as mCamera.release();
        }
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here
            // Make sure to stop the preview before resizing or reformatting it.
        	previewWidth = w;
        	previewHeight = h;
        	
            if(mHolder.getSurface() == null) { // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or reformatting changes here
            Camera.Parameters parameters = mCamera.getParameters();

            List<Size> sizes = parameters.getSupportedPreviewSizes();
            Size optimalSize = getOptimalPreviewSize(sizes, w, h);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            parameters.setPreviewFrameRate(30); // TODO test
            
            mCamera.setParameters(parameters);
            
            try { // start preview with new settings
                mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }

        private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
            final double ASPECT_TOLERANCE = 0.05;
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

            // Cannot find the match of aspect ratio, ignore the requirement
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
    }
    
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }
    private void tempReleaseCamera() {
        if (mCamera != null) {
            mCamera.lock(); // lock camera for later use
            mCamera = null;
        }
    }

	public void goToEditStats(View v) {
    	startActivity(new Intent(_activity, EditStats.class));
	}
	
	// The one we have been using for a long time, but just not right now
	public void decodeYUV(int[] out, byte[] fg, int width, int height) throws NullPointerException, IllegalArgumentException {
	    int sz = width * height;
	    if (out == null) throw new NullPointerException("buffer out is null");
	    if (out.length < sz) throw new IllegalArgumentException("buffer out size " + out.length + " < minimum " + sz);
	    if (fg == null) throw new NullPointerException("buffer 'fg' is null");
	    if (fg.length < sz) throw new IllegalArgumentException("buffer fg size " + fg.length + " < minimum " + sz * 3 / 2);
	    int i, j;
	    int Y, Cr = 0, Cb = 0;
	    for (j = 0; j < height; j++) {
	        int pixPtr = j * width;
	        final int jDiv2 = j >> 1;
	        for (i = 0; i < width; i++) {
	            Y = fg[pixPtr];
	            if (Y < 0) Y += 255;
	            if ((i & 0x1) != 1) {
	                final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
		        	Log.d("DEBUG RGB", "DEBUG: cOff: " + cOff);
	                Cb = fg[cOff];
	                if (Cb < 0) Cb += 127;
	                else Cb -= 128;
	                Cr = fg[cOff + 1];
	                if (Cr < 0) Cr += 127;
	                else Cr -= 128;
	            }
	            
	            int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
	            if (R < 0) R = 0;
	            else if (R > 255) R = 255;
	            int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1) + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
	            if (G < 0) G = 0;
	            else if (G > 255) G = 255;
	            int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
	            if (B < 0) B = 0;
	            else if (B > 255) B = 255;
	            
	            out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
	        }
	    }
	}
	static public void decodeYUV2(int[] rgb, byte[] yuv420sp, int width, int height) { // Just another possible way of decoding
	    final int frameSize = width * height;

	    for (int j = 0, yp = 0; j < height; j++) {
	        int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
	        for (int i = 0; i < width; i++, yp++) {
	            int y = (0xff & ((int) yuv420sp[yp])) - 16;
	            if (y < 0) y = 0;
	            if ((i & 1) == 0) {
	                v = (0xff & yuv420sp[uvp++]) - 128;
	                u = (0xff & yuv420sp[uvp++]) - 128;
	            }
	            int y1192 = 1192 * y;
	            int r = (y1192 + 1634 * v);
	            int g = (y1192 - 833 * v - 400 * u);
	            int b = (y1192 + 2066 * u);
//				R += 1.164*(Y-16)                  + 2.018*(Cr-128);
//				G += 1.164*(Y-16) - 0.813*(Cb-128) - 0.391*(Cr-128);
//				B += 1.164*(Y-16) + 1.596*(Cb-128);

	            if (r < 0) r = 0; else if (r > 262143) r = 262143;
	            if (g < 0) g = 0; else if (g > 262143) g = 262143;
	            if (b < 0) b = 0; else if (b > 262143) b = 262143;

	            rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
	        }
	    }
	}
	
	private void loadPatientEditableStats() {
		displayEnglishUnits = settings.getBoolean("displayEnglishUnits", true);
        if(displayEnglishUnits) {
	        mTextViewAge.setText("Age: " + settings.getInt("age", 23));
	        mTextViewSex.setText("Sex: " + settings.getString("sex", "Male"));
	        mTextViewWeight.setText("Weight: " + settings.getInt("weight", 160) + " pounds");
	        mTextViewHeight.setText("Height: " + settings.getInt("height", 75) + " inches");
	        mTextViewPosition.setText("Position: " + settings.getString("position", "Sitting"));
        } else {
	        mTextViewAge.setText("Age: " + settings.getInt("age", 23));
	        mTextViewSex.setText("Sex: " + settings.getString("sex", "Male"));
	        mTextViewWeight.setText("Weight: " + settings.getInt("weight", 73) + " kg");
	        mTextViewHeight.setText("Height: " + settings.getInt("height", 75) + " cm");
	        mTextViewPosition.setText("Position: " + settings.getString("position", "Sitting"));
        }
	}
	
	private void sendEmail(String to, String message) {
		try {
			Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{to});
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Vital Signs");
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
			//emailIntent.setType("text/plain");
			emailIntent.setType("vnd.android.cursor.dir/email"); // Or "text/plain" "text/html" "plain/text"
			//startActivity(emailIntent);
			startActivity(Intent.createChooser(emailIntent, "Send email:"));
			//finish();
		} catch (ActivityNotFoundException e) {
            Log.e("Emailing contact", "Email failed", e);
		}
	}
	private void sendSMS(String phoneNumber, String message) {
//		Uri smsUri = Uri.parse("sms:" + phoneNumber);
//		Intent intent = new Intent(Intent.ACTION_VIEW, smsUri);
//		intent.putExtra("sms_body", message);
//		intent.setType("vnd.android-dir/mms-sms"); 
//		startActivity(intent);
		SmsManager sms = SmsManager.getDefault();
	    sms.sendTextMessage(phoneNumber, null, message, null, null);
	}
	
	private void checkMediaAvailability() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    mExternalStorageAvailable = mExternalStorageWriteable = true; // We can read and write the media
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
	}
	
	private void writeToTextFile(String data, String fileName) {
    	File sdCard = Environment.getExternalStorageDirectory();
    	File directory = new File (sdCard.getAbsolutePath() + "/VitalSigns");
    	directory.mkdirs();
    	File file = new File(directory, fileName + ".txt");

    	FileOutputStream fOut;
		try {
			fOut = new FileOutputStream(file);
	    	OutputStreamWriter osw = new OutputStreamWriter(fOut);
	    	osw.write(data);
	    	osw.flush();
	    	osw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void saveSharedPreference(String key, int value) {
    	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
    	editor.putInt(key, value);
    	editor.commit(); // This line saves the edits
	}
	private void saveSharedPreference(String key, boolean value) {
    	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
    	editor.putBoolean(key, value);
    	editor.commit(); // This line saves the edits
	}
	private void saveSharedPreference(String key, String value) {
    	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
    	editor.putString(key, value);
    	editor.commit(); // This line saves the edits
	}
	
	public void playSound() { // TODO play sound when finished calculating heart rate
        new Thread() {
        	public void run() {
                //MediaPlayer mp = MediaPlayer.create(_activity, R.raw.mysound);   
                //mp.start();
            }
        }.start();
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_test_blood_pressure, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
            	// Do nothing
                return true;
            case R.id.menu_convertUnits:
            	displayEnglishUnits = (displayEnglishUnits==false)?true:false; // Switch between English and Metric
            	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
            	if(displayEnglishUnits) {
                	editor.putInt("weight", (int)(settings.getInt("weight", 73)*2.20462));
                	editor.putInt("height", (int)(settings.getInt("height", 190)/2.54));
                	editor.putBoolean("displayEnglishUnits", displayEnglishUnits);
                	editor.commit(); // This line saves the edits
        	        mTextViewWeight.setText("Weight: " + settings.getInt("weight", 73) + " pounds");
        	        mTextViewHeight.setText("Height: " + settings.getInt("height", 190) + " inches");
            	} else { // Metric
                	editor.putInt("weight", (int)(settings.getInt("weight", 160)/2.20462));
                	editor.putInt("height", (int)(settings.getInt("height", 75)*2.54));
                	editor.putBoolean("displayEnglishUnits", displayEnglishUnits);
                	editor.commit(); // This line saves the edits
            		mTextViewWeight.setText("Weight: " + settings.getInt("weight", 160) + " kg");
            		mTextViewHeight.setText("Height: " + settings.getInt("height", 75) + " cm");
            	}
                return true;
            case R.id.menu_sendEmail:
            	sendEmail("danialgoodwin@gmail.com", "Heart Rate: " + heartRate + " bpm\nBlood Pressure: " + systolicPressure + "/" + diastolicPressure + "\nTemperature: " + temperature + ((displayEnglishUnits==true)?" F":" C"));
                return true;
            case R.id.menu_sendSMS:
            	sendSMS("8132859689", "Heart Rate: " + heartRate + " bpm\nBlood Pressure: " + systolicPressure + "/" + diastolicPressure + "\nTemperature: " + temperature + ((displayEnglishUnits==true)?" F":" C"));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
}
