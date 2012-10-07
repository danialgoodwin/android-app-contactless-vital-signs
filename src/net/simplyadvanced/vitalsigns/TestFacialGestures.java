package net.simplyadvanced.vitalsigns;

import java.io.IOException;

import android.hardware.Camera;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.support.v4.app.NavUtils;

public class TestFacialGestures extends Activity implements SurfaceHolder.Callback {
	SurfaceView surfaceView1;
	SurfaceHolder surfaceHolder;
	Camera camera;
	//CameraView v;
	boolean isPreviewing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_facial_gestures);
        
        //getWindow().setFormat(PixelFormat.UNKNOWN); // What does it do?
        surfaceView1 = (SurfaceView) findViewById(R.id.surfaceView1);
        surfaceHolder = surfaceView1.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated, but required on Android versions prior to 3.0
    }
    
    
    
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		camera.stopPreview();
		isPreviewing = false;
		
		if(camera != null) {
    		try {
    			camera.setPreviewDisplay(surfaceHolder);
    			camera.setDisplayOrientation(90); // Needed to have camera in correct orientation
    		    camera.startPreview();
    		    isPreviewing = true;
    		} catch (IOException e) {
    		    e.printStackTrace();
    		}
    	}
	}
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		camera = Camera.open();
	}
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		camera.stopPreview();
		camera.release();
		camera = null;
	    isPreviewing = false;
	}
	
	
    
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_test_facial_gestures, menu);
        return true;
    }

    
}
