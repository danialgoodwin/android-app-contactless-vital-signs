package net.simplyadvanced.vitalsigns.bodytemperature;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.simplyadvanced.vitalsigns.R;
import net.simplyadvanced.vitalsigns.setting.UserStatsSettingsActivity;
import net.simplyadvanced.vitalsigns.util.FastIcaRgb;
import net.simplyadvanced.vitalsigns.util.Fft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class BodyTemperatureActivity extends Activity {

    private TextView mTextViewAge, mTextViewSex, mTextViewWeight, mTextViewHeight, mTextViewPosition;
    private TextView mTextViewBloodPressure, mTextViewHeartRate;
    private TextView mBlue, mDebug;
    public static final String PREFS_NAME = "MyPrefsFile";
    private Camera mCamera;
    private CameraPreview mPreview;
    private SharedPreferences settings;
    private FrameLayout preview;
    private int previewWidth = 0, previewHeight = 0;

    /* Heart Rate Related Variables */
    private ArrayList<Double> arrayRed = new ArrayList<Double>();
    private ArrayList<Double> arrayGreen = new ArrayList<Double>();
    private ArrayList<Double> arrayBlue = new ArrayList<Double>();
    private int heartRateFrameLength = 32;
    private double[] outRed = new double[heartRateFrameLength];
    private double[] outGreen = new double[heartRateFrameLength];
    private double[] outBlue = new double[heartRateFrameLength];
    private int systolicPressure = 0, diastolicPressure = 0, temperature = 0;
    private double heartRate = 0;

    /*Frame Frequency*/
    private long samplingFrequency;

    /* Writing to SD card */
    private boolean mExternalStorageAvailable = false;
    private boolean mExternalStorageWriteable = false;
    private String fileDataRed = "";
    private String fileDataGreen = "";
    private String fileDataBlue = "";

    /* Settings */
    private boolean displayEnglishUnits = true;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // Hide the window title
        setContentView(R.layout.activity_body_temperature);

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

    @Override
    protected void onResume() {
        super.onResume();
        loadPatientEditableStats();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        //tempReleaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    public void setBloodPressure(double heartRate, int age, String sex, int weight, int height, String position) {
        double R = 18.5; // Average R = 18.31; // Vascular resistance // Very hard to calculate from person to person
        double Q = (sex.equalsIgnoreCase("Male") || sex.equalsIgnoreCase("M"))?5:4.5; // Liters per minute of blood through heart
        double ejectionTime = (!position.equalsIgnoreCase("Laying Down"))?386-1.64*heartRate:364.5-1.23*heartRate; // WAS ()?376-1.64*heartRate:354.5-1.23*heartRate; // ()?sitting:supine
        double bodySurfaceArea = 0.007184*(Math.pow(weight,0.425))*(Math.pow(height,0.725));
        double strokeVolume = -6.6 + 0.25*(ejectionTime-35) - 0.62*heartRate + 40.4*bodySurfaceArea - 0.51*age; // Volume of blood pumped from heart in one beat
        double pulsePressure = strokeVolume / ((0.013*weight - 0.007*age-0.004*heartRate)+1.307);
        double meanPulsePressure = Q*R;

        systolicPressure = (int) (meanPulsePressure + 3/2*pulsePressure);
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
            mCamera.setPreviewCallback(new Camera.PreviewCallback() { // Gets called for every frame
                public void onPreviewFrame(byte[] data, Camera c) {
//					int centerX = (previewWidth / 2), centerY = (previewHeight / 2);
//					int sampleWidth = 9, sampleHeight = 9;
//


                    //previewWidth = 100;//c.getParameters().getPreviewSize().width;
                    //previewHeight = 100;//c.getParameters().getPreviewSize().height;

                    //FileOutputStream outStream = null;
//					try {
//						YuvImage yuvimage = new YuvImage(data,ImageFormat.NV21,c.getParameters().getPreviewSize().width,c.getParameters().getPreviewSize().height,null);
//						ByteArrayOutputStream baos = new ByteArrayOutputStream();
//						yuvimage.compressToJpeg(new Rect(0,0,previewWidth,previewHeight), 80, baos);
//
//						data = baos.toByteArray();
//
//						//outStream = new FileOutputStream(String.format("/sdcard/%d.jpg", System.currentTimeMillis()));
//						//outStream.write(baos.toByteArray());
//						//outStream.close();
//
//						Log.d(TAG, "onPreviewFrame - wrote bytes: " + data.length);
//					} /*catch (FileNotFoundException e) { e.printStackTrace();
//					} catch (IOException e) { e.printStackTrace();
//					}*/ finally { }
                    //Preview.this.invalidate();



                    int previewNumberOfPixels = previewWidth * previewHeight;
                    int[] pixels = new int[previewNumberOfPixels];

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
//			        red /= 81;
//			        green /= 81;
//			        blue /= 81;

                    decodeYUV(pixels, data, previewWidth, previewHeight); // Good, works

                    int r = 0, g = 0, b = 0; // Works, good, was int
                    for(int k = 0; k < pixels.length; k++) { // Good, works
                        r += Color.red(pixels[k]);   //1.164(Y-16)                + 2.018(U-128);
                        g += Color.green(pixels[k]); //1.164(Y-16) - 0.813(V-128) - 0.391(U-128);
                        b += Color.blue(pixels[k]);  //1.164(Y-16) + 1.596(V-128);
                    }
                    r /= pixels.length;
                    g /= pixels.length;
                    b /= pixels.length;

//					r = R/previewNumberOfPixels;
//					g = G/previewNumberOfPixels;
//					b = B/previewNumberOfPixels;

                    //Camera.Parameters parameters = mCamera.getParameters();
                    //int[] previewFPSRange = new int[2];
                    //parameters.getPreviewFpsRange(previewFPSRange); // Android API 9+
                    mBlue.setText("RGB: " + r + "," + g + "," + b); // YCbCr_420_SP (NV21) format

                    if(arrayRed.size() == 0) {
                        samplingFrequency = System.nanoTime(); // Start time
                    }

                    if(arrayRed.size() < heartRateFrameLength) {
                        fileDataRed += r + " "; // a string
                        fileDataGreen += g + " "; // a string
                        fileDataBlue += b + " "; // a string
                        arrayRed.add((double) r);
                        arrayGreen.add((double) g);
                        arrayBlue.add((double) b);
                        mTextViewBloodPressure.setText("Blood Pressure: in " + (heartRateFrameLength-arrayRed.size()+1) + ".."); // Shows how long until measurement will display
                        mTextViewHeartRate.setText("Heart Rate: in " + (heartRateFrameLength-arrayRed.size()) + "..");
                    }
                    else if(arrayRed.size() == heartRateFrameLength) { // So that these functions don't run every frame preview, just on the 32nd one // TODO add sound when finish
                        writeToTextFile(fileDataRed, "red"); // file located root/VitalSigns
                        writeToTextFile(fileDataGreen, "green"); // file located root/VitalSigns
                        writeToTextFile(fileDataBlue, "blue"); // file located root/VitalSigns

                        samplingFrequency = System.nanoTime() - samplingFrequency; // Minus end time = length of heartRateFrameLength frames
                        samplingFrequency /= 1000000000; // Length of time to get 600 frames in seconds
                        samplingFrequency = heartRateFrameLength / samplingFrequency; // Frames per second in seconds

                        for(int a=0; a<heartRateFrameLength; a++) {
                            outRed[a] = (Double) arrayRed.get(a);
                            outGreen[a] = (Double) arrayGreen.get(a);
                            outBlue[a] = (Double) arrayBlue.get(a);
                        }

                        FastIcaRgb.preICA(outRed, outGreen, outBlue, heartRateFrameLength, outRed, outGreen, outBlue); // heartRateFrameLength = 32 for now
                        double heartRateFrequency = Fft.FFT(outGreen, heartRateFrameLength, (double) samplingFrequency);
                        if (heartRateFrequency == 0) {
                            mTextViewHeartRate.setText("Heart Rate: Error, try again");
                            mTextViewBloodPressure.setText("Blood Pressure: Error, try again");
                        } else {
                            heartRate = Math.round((heartRateFrequency * 60) * 100) / 100;

                            mTextViewHeartRate.setText("Heart Rate: " + heartRate);
                            mTextViewBloodPressure.setText("Blood Pressure: in 0.."); // Just informing the user that BP almost calculated
                            mDebug.setText("Fps: " + samplingFrequency);
                            setBloodPressure(heartRate, settings.getInt("age", 25), settings.getString("sex", "Male"), settings.getInt("weight", 160), settings.getInt("height", 70), settings.getString("position", "Sitting"));

                            saveSharedPreference("heartRate",(int)heartRate);
                            arrayRed.add(1.0); // Ensures this if-statement is only ran once by making arrayRed.size() one bigger than heartRateLength
                        }
                    }
                    else {
                        // do nothing
                    }
                }
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

            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            Camera.Size optimalSize = getOptimalPreviewSize(sizes, w, h);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);

            mCamera.setParameters(parameters);

            try { // start preview with new settings
                mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }

        private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
            final double ASPECT_TOLERANCE = 0.05;
            double targetRatio = (double) w / h;
            if (sizes == null) return null;

            Camera.Size optimalSize = null;
            double minDiff = Double.MAX_VALUE;

            int targetHeight = h;

            // Try to find an size match aspect ratio and size
            for (Camera.Size size : sizes) {
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
                for (Camera.Size size : sizes) {
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
        startActivity(new Intent(this, UserStatsSettingsActivity.class));
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
        getMenuInflater().inflate(R.menu.activity_common, menu);
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
                displayEnglishUnits = !displayEnglishUnits; // Switch between English and Metric
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
