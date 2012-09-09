package net.simplyadvanced.vitalsigns;

import java.io.IOException;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.support.v4.app.NavUtils;

public class TestVitalSigns extends Activity implements SurfaceHolder.Callback {
	SurfaceView surfaceView1;
	SurfaceHolder surfaceHolder;
	Camera camera;
	//CameraView v;
	boolean isPreviewing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_vital_signs);

        //getWindow().setFormat(PixelFormat.UNKNOWN); // What does it do?
        surfaceView1 = (SurfaceView) findViewById(R.id.surfaceView1);
        surfaceHolder = surfaceView1.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated, but required on Android versions prior to 3.0
        
        
        
        //v = new CameraView(this);
        //v.setOnTouchListener(this);
        //setContentView(v);
        
    }
    
    
    @Override
	protected void onPause() {
		super.onPause();
		//v.pause();
		
//		if(camera != null && isPreviewing) {
//			camera.stopPreview();
//			camera.release();
//			camera = null;
//		    isPreviewing = false;
//		}
		
	}
	@Override
	protected void onResume() {
		super.onResume();
		//v.resume();

//        if(!isPreviewing) {
//        	camera = Camera.open();
//        	if(camera != null) {
//        		try {
//        			camera.setPreviewDisplay(surfaceHolder);
//        		    camera.startPreview();
//        		    isPreviewing = true;
//        		} catch (IOException e) {
//        		    e.printStackTrace();
//        		}
//        	}
//        }
        
	}




//	public class CameraView extends SurfaceView implements Runnable { // Just going to be a thread
//		Thread t = null;
//		SurfaceHolder holder;
//		boolean isItOkay = false;
//		
//		public CameraView(Context context) {
//			super(context);
//			holder = getHolder();
//		}
//
//		@Override
//		public void run() {
//			while(isItOkay == true) {
//				//perform canvas drawing
//				if(!holder.getSurface().isValid()) { // If surface is not available, then don't draw
//					continue;
//				}
//				Canvas c = holder.lockCanvas();
//				c.drawARGB(255, 150, 150, 10); // argb
//				holder.unlockCanvasAndPost(c);
//				
//			}
//		}
//		
//		public void pause() {
//			isItOkay = false;
//			while(true) {
//				try {
//					t.join();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				} finally {}
//				break;
//			}
//			t = null;
//		}
//		public void resume() {
//			isItOkay = true;
//			t = new Thread(this);
//			t.start();
//		}
//    	
//    }
    
	
	



	@Override
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
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		camera = Camera.open();
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		camera.stopPreview();
		camera.release();
		camera = null;
	    isPreviewing = false;
	}
    

	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_test_vital_signs, menu);
        return true;
    }
    
}
