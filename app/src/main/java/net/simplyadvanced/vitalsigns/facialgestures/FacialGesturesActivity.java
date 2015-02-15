package net.simplyadvanced.vitalsigns.facialgestures;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.simplyadvanced.vitalsigns.R;

import java.io.IOException;

public class FacialGesturesActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "DEBUG";
    private TextView mTextViewFace0Coordinates, mTextViewFace1Coordinates, mTextViewFace2Coordinates, mTextViewFace3Coordinates;
    private ImageView mRectImage0, mRectImage1, mRectImage2;
    private SurfaceView surfaceView1;
    private SurfaceHolder surfaceHolder;
    private Camera mCamera;
    private boolean isPreviewing = false;
    private int previewWidth, previewHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facial_gestures);

        mTextViewFace0Coordinates = (TextView) findViewById(R.id.textViewFace0Coordinates);
        mTextViewFace1Coordinates = (TextView) findViewById(R.id.textViewFace1Coordinates);
        mTextViewFace2Coordinates = (TextView) findViewById(R.id.textViewFace2Coordinates);
        mTextViewFace3Coordinates = (TextView) findViewById(R.id.textViewFace3Coordinates);
        mRectImage0 = (ImageView) findViewById(R.id.rectImage0);

        surfaceView1 = (SurfaceView) findViewById(R.id.surfaceView1);
        surfaceHolder = surfaceView1.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated, but required on Android versions prior to 3.0
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.stopPreview();
        isPreviewing = false;
        previewWidth = width;
        previewHeight = height;

        // Make any parameter changes here, while camera is not previewing

        if(mCamera != null) {
            try {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.setDisplayOrientation(90); // Needed to have camera in correct orientation
                mCamera.setFaceDetectionListener(new MyFaceDetectionListener());
                mCamera.startPreview();
                isPreviewing = true;
                startFaceDetection(); // start face detection feature
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

            startFaceDetection(); // start face detection feature

        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            isPreviewing = false;
        }
    }



    class MyFaceDetectionListener implements Camera.FaceDetectionListener {
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length == 0) {
                mTextViewFace3Coordinates.setText("Confidence Score: 0%");
            } else if (faces.length > 0) {
                int left0   = faces[0].rect.left;
                int top0    = faces[0].rect.top;
                int right0  = faces[0].rect.right;
                int bottom0 = faces[0].rect.bottom;
                mTextViewFace0Coordinates.setText("Face Rectangle: (" + left0 + "," + top0 + "), (" + right0 + "," + bottom0 + ")");
                mTextViewFace3Coordinates.setText("Confidence Score: " + faces[0].score + "%");

                try {
                    int leftEyeX = faces[0].leftEye.x;
                    int leftEyeY = faces[0].leftEye.y;
                    int rightEyeX = faces[0].rightEye.x;
                    int rightEyeY = faces[0].leftEye.y;
                    mTextViewFace1Coordinates.setText("Left,Right Eye: (" + leftEyeX + "," + leftEyeY + "), (" + rightEyeX + "," + rightEyeY + ")");
                } catch (Exception e) {
                    mTextViewFace1Coordinates.setText("Left,Right Eye: not supported");
                }

                try {
                    int mouthX = faces[0].mouth.x;
                    int mouthY = faces[0].mouth.y;
                    mTextViewFace2Coordinates.setText("Mouth: (" + mouthX + "," + mouthY + ")");
                } catch (Exception e) {
                    mTextViewFace2Coordinates.setText("Mouth: not supported");
                }


                // TODO: Use this somewhere to help draw rectangles around faces
//	            Matrix matrix = new Matrix();
//	            CameraInfo info = CameraHolder.instance().getCameraInfo()[0];
//	            // Need mirror for front camera.
//	            boolean mirror = (info.facing == CameraInfo.CAMERA_FACING_FRONT);
//	            matrix.setScale(mirror ? -1 : 1, 1);
//	            // This is the value for android.hardware.Camera.setDisplayOrientation.
//	            matrix.postRotate(90);
//	            // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
//	            // UI coordinates range from (0, 0) to (width, height).
//	            matrix.postScale(surfaceView1.getWidth() / 2000f, surfaceView1.getHeight() / 2000f);
//	            matrix.postTranslate(surfaceView1.getWidth() / 2f, surfaceView1.getHeight() / 2f);


//		    	mRectImage0.setPadding(left0, top0, previewWidth-right0, previewHeight-bottom0);
//		    	mRectImage0.bringToFront();


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
        }
    }
    public void startFaceDetection() {
        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();

        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0) {
            Toast.makeText(this, "Max Num Faces Allows: " + params.getMaxNumDetectedFaces(), Toast.LENGTH_LONG).show();
            // camera supports face detection, so can start it:
            mCamera.startFaceDetection();
        }
    }

}
