package net.simplyadvanced.vitalsigns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

import net.simplyadvanced.vitalsigns.heartrate.FFT;
import net.simplyadvanced.vitalsigns.heartrate.FastICA_RGB;

public class TestBloodPressure extends Activity {
	private TestBloodPressure _activity;
	TextView mTextViewAge, mTextViewSex, mTextViewWeight, mTextViewHeight, mTextViewBloodPressure;
	TextView mDebug, mRed, mGreen, mBlue;
    public static final String PREFS_NAME = "MyPrefsFile";
    private Camera mCamera;
    private CameraPreview mPreview;
    SharedPreferences settings;
    int previewWidth = 0, previewHeight = 0;

    ArrayList<Double> arrayRed, arrayGreen, arrayBlue;
    //double[] dArrayRed, arrayGreen, arrayBlue;
    int lengthOfPreview = 0;
    
    /* Heart Rate Variables */
    double[] outRed, outGreen, outBlue;
    double[] heartRateFrequency;
    int heartRateFrameLength = 32;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        _activity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_blood_pressure);
        //requestWindowFeature(Window.FEATURE_NO_TITLE); // Hide the window title

        mTextViewAge = (TextView) findViewById(R.id.textViewAge); // Connects variables here to ids in xml
        mTextViewSex = (TextView) findViewById(R.id.textViewSex);
        mTextViewWeight = (TextView) findViewById(R.id.textViewWeight);
        mTextViewHeight = (TextView) findViewById(R.id.textViewHeight);
        mTextViewBloodPressure = (TextView) findViewById(R.id.textViewBloodPressure);
        mDebug = (TextView) findViewById(R.id.debug);
        mRed = (TextView) findViewById(R.id.red);
        mGreen = (TextView) findViewById(R.id.green);
        mBlue = (TextView) findViewById(R.id.blue);
        
        loadPatientEditableStats();

    	mCamera = getCameraInstance(); // Create an instance of Camera
    	
        mPreview = new CameraPreview(this, mCamera); // Create our Preview view and set it as the content of our activity
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
       
        settings = getSharedPreferences(PREFS_NAME, 0); // Load saved stats
        setBloodPressure(/*first param,*/ settings.getInt("age", 25), settings.getString("sex", "Male"), settings.getInt("weight", 160), settings.getInt("height", 70));

    }

    protected void onResume() {
    	super.onResume();
    	loadPatientEditableStats();        
        setBloodPressure(/*first param,*/ settings.getInt("age", 25), settings.getString("sex", "Male"), settings.getInt("weight", 160), settings.getInt("height", 70));
    }
    protected void onPause() {
    	super.onPause();
    	//tempReleaseCamera();
    }
    protected void onDestroy() {
    	super.onDestroy();
    	releaseCamera();
    }
    
    public void setBloodPressure(/*double[][] cameraData OR red[], green[], blue[], time[85123456]*/ int age, String sex, int weight, int height) {
    	int systolicPressure;
    	int diastolicPressure;
    	double bodySurfaceArea, strokeVolume, pulsePressure;
    	double ejectionTime = .35, heartRate = 60, meanPulsePressure = 100; // TODO calculate each of these with data from camera, except mPP
    	
    	bodySurfaceArea = 0.007184*(Math.pow(weight,0.425))*(Math.pow(height,0.725));
        strokeVolume = -6.6 + 0.25*(ejectionTime-35) - 0.62*heartRate + 40.4*bodySurfaceArea - 0.51*age; // Volume of blood pumped from heart in one beat
        pulsePressure = strokeVolume / ((0.013*weight - 0.007*age-0.004*heartRate)+1.307);
        
        systolicPressure = (int) (meanPulsePressure + 2/3*pulsePressure);
        diastolicPressure = (int) (meanPulsePressure - pulsePressure/3);
    	
    	mTextViewBloodPressure.setText("Blood Pressure: " + systolicPressure + "/" + diastolicPressure);
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

        @SuppressWarnings("deprecation")
		public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            mHolder = getHolder(); // Install a SurfaceHolder.Callback so we get notified when the underlying surface is created and destroyed
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated setting, but required on Android versions prior to 3.0
        }

        public void surfaceCreated(SurfaceHolder holder) { // The Surface has been created, now tell the camera where to draw the preview

//            try {
//                //mCamera.setDisplayOrientation(90);
//                //mCamera.setPreviewDisplay(holder);
//                //mCamera.startPreview();
//            } catch (IOException e) {
//                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
//            }
        	
        	mCamera.setPreviewCallback(new PreviewCallback() { // Gets called for every frame
				@Override
				public void onPreviewFrame(byte[] data, Camera c) {
//					final String TAG = "onPreviewFrame";
//					long timeAtStart = System.currentTimeMillis();
//					Log.d(TAG, "Width and Height Retrieved As: " + previewWidth + ", " + previewHeight);
					
//					int centerX = (previewWidth / 2), centerY = (previewHeight / 2);
//					int sampleWidth = 9, sampleHeight = 9;
					int[] pixels = new int[previewWidth * previewHeight];
//					
//					int tempNum, red = 0, green = 0, blue = 0;
//					for(int i =0; i<81; i++) {
//			            tempNum = (Integer) pixels[i];
//			            Log.d("lookingFor", "Pixel Num: " + Color.blue(tempNum));
//			            red += Color.red(tempNum);
//			            green += Color.green(tempNum);
//			            blue += Color.blue(tempNum);
//			            Log.d("lookingFor", "current Blue: " + Color.blue(tempNum));
//			            Log.d("lookingFor", "added blue: " + blue);
//			        }
//			        Log.v("lookingFor", blue + " " + red + " " + green);
//			        red /= 81;
//			        green /= 81;
//			        blue /= 81;
					
					
					decodeYUV(pixels, data, previewWidth, previewHeight);
					
					int r = 0, g = 0, b = 0;
					for(int i = 0; i < pixels.length; i++) {
						r += Color.red(pixels[i]);   //1.164(Y-16)                + 2.018(U-128);
						g += Color.green(pixels[i]); //1.164(Y-16) - 0.813(V-128) - 0.391(U-128);
						b += Color.blue(pixels[i]);  //1.164(Y-16) + 1.596(V-128);
					}
					r /= pixels.length;
					g /= pixels.length;
					b /= pixels.length;

		            Camera.Parameters parameters = mCamera.getParameters();
		            int[] previewFPSRange = new int[2];
		            parameters.getPreviewFpsRange(previewFPSRange);
					mRed.setText("Fps: " + previewFPSRange[0] + previewFPSRange[1]);
			        mGreen.setText("data.length: " + data.length);
			        mBlue.setText("RGB: " + r + "," + g + "," + b); // YCbCr_420_SP (NV21) format
			        
			        while(arrayRed.size() < 32) {
				        arrayRed.add((double) r);
				        arrayGreen.add((double) g);
				        arrayBlue.add((double) b);
			        }
			        
			        if(arrayRed.size() == 32) { // So that these functions don't run every frame preview, just on the 32nd one
				        for(int a=0; a<32; a++) {
				        	outRed[a] = (Double) arrayRed.get(a);
				        	outGreen[a] = (Double) arrayGreen.get(a);
				        	outBlue[a] = (Double) arrayBlue.get(a);
				        }
				        
				        outRed = new double[heartRateFrameLength]; // heartRateFrameLength = 32 for now
				        FastICA_RGB.preICA(outRed, outGreen, outBlue, heartRateFrameLength, outRed, outGreen, outBlue);
				        
				        heartRateFrequency = new double[heartRateFrameLength];
				        FFT.fft(outGreen, heartRateFrameLength, heartRateFrequency);
				        
				        double temp = 0;
				        for(int a=0; a<32; a++) {
				        	if(temp < heartRateFrequency[a]) {
				        		temp = heartRateFrequency[a];
				        	}
					        System.out.println(String.valueOf(a) +": " + Double.toString(heartRateFrequency[a]));
				        }
			        }
			        
				}
        	});
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        	// Surface will be destroyed when we return, so stop the preview.
            // Because the CameraDevice object is not a shared resource, it's very important to release it when the activity is paused.
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

        	previewWidth = w;
        	previewHeight = h;
        	//mDebug.setText("format: " + Integer.toString(format));
        	
            if (mHolder.getSurface() == null) {
              // preview surface does not exist
              return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
              // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here
            
            
            Camera.Parameters parameters = mCamera.getParameters();

            List<Size> sizes = parameters.getSupportedPreviewSizes();
            Size optimalSize = getOptimalPreviewSize(sizes, w, h);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
//            List<Integer> supportedFormats = parameters.getSupportedPreviewFormats();
//            if(supportedFormats.contains(ImageFormat.RGB_565)) {
//            	parameters.setPreviewFormat(ImageFormat.RGB_565);
//            	Toast.makeText(_activity, "Preview Format will be RGB_565", Toast.LENGTH_LONG).show();
//            }
//            else {
            	Toast.makeText(_activity, "Preview Format will be NV16 (YCbCr)", Toast.LENGTH_LONG).show();
//            }
            
            //mBlue.setText("Fps: " + parameters.getPreviewFrameRate()); // DEBUG
            
            mCamera.setParameters(parameters);
            

            // start preview with new settings
            try {
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
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
    private void tempReleaseCamera() {
        if (mCamera != null) {
            mCamera.lock();        // lock camera for later use
            mCamera = null;
        }
    }

	public void goToEditStats(View v) {
    	startActivity(new Intent(_activity, EditStats.class));
	}
	public void getRGB(View v) {
		final String TAG = "getRGB";
		long timeAtStart = System.currentTimeMillis();
        Log.d(TAG, "Width and Height Retrieved As: " + previewWidth + ", " + previewHeight);
        Bitmap b = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config. RGB_565);
        Canvas c = new Canvas(b);
        CameraPreview view = (CameraPreview) ((ViewGroup) findViewById(R.id.camera_preview)).getChildAt(0);
        view.draw(c);
        int centerX = (previewWidth / 2);
        int centerY = (previewHeight / 2);

        //test = b.getPixel(240, 350);
        int sampleWidth = 9;
        int sampleHeight = 9;
        int[] pixels = new int[sampleWidth * sampleHeight];
        b.getPixels(pixels, 0, sampleWidth, centerX - 4, centerY - 4, sampleWidth, sampleHeight);
        int tempNum;
        int red = 0, green = 0, blue = 0;
        Log.d("lookingFor", "test: " + pixels[1]);
        for(int i =0; i<81; i++) {
            tempNum = (Integer) pixels[i];
            Log.d("lookingFor", "Pixel Num: " + Color.blue(tempNum));
            red += Color.red(tempNum);
            green += Color.green(tempNum);
            blue += Color.blue(tempNum);
            Log.d("lookingFor", "current Blue: " + Color.blue(tempNum));
            Log.d("lookingFor", "added blue: " + blue);
        }
        Log.v("lookingFor", blue + " " + red + " " + green);
        red /= 81;
        green /= 81;
        blue /= 81;

        Log.d(TAG, "RGB at (" + centerX + ", " + centerY + " ) - R:" + red + " G:" + green + " B:" + blue);
        long timeAtEnd = System.currentTimeMillis();
        long totalTime = timeAtEnd - timeAtStart;
        Log.d(TAG, "Fetching the color took " + totalTime + " milliseconds");
        mDebug.setText("Total Time: " + totalTime + " ms");
        mRed.setText("RGB: " + red + "," + green + "," + blue);
        //mGreen.setText("Green: " + green);
        //mBlue.setText("Blue: " + blue);
	}
    
	
	public void decodeYUV(int[] out, byte[] fg, int width, int height)
	        throws NullPointerException, IllegalArgumentException {
	    int sz = width * height;
	    if (out == null)
	        throw new NullPointerException("buffer out is null");
	    if (out.length < sz)
	        throw new IllegalArgumentException("buffer out size " + out.length
	                + " < minimum " + sz);
	    if (fg == null)
	        throw new NullPointerException("buffer 'fg' is null");
	    if (fg.length < sz)
	        throw new IllegalArgumentException("buffer fg size " + fg.length
	                + " < minimum " + sz * 3 / 2);
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
	            int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1)
	                    + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
	            if (G < 0) G = 0;
	            else if (G > 255) G = 255;
	            int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
	            if (B < 0) B = 0;
	            else if (B > 255) B = 255;
	            out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
	        }
	    }
	}
	
	private void loadPatientEditableStats() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0); // Load saved stats
        mTextViewAge.setText("Age: " + settings.getInt("age", 25));
        mTextViewSex.setText("Sex: " + settings.getString("sex", "Male"));
        mTextViewWeight.setText("Weight: " + settings.getInt("weight", 160) + " pounds");
        mTextViewHeight.setText("Height: " + settings.getInt("height", 70) + " inches");
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_test_blood_pressure, menu);
        return true;
    }

    
}
