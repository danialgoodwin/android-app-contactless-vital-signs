package net.simplyadvanced.vitalsigns;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera.Face;
import android.view.SurfaceView;
import android.view.View;

@TargetApi(14)
public class DrawRect extends View{

	public DrawRect(Context context,Face[] faces) {
		super(context);
	}

	protected void onDraw(Canvas canvas, Face[] faces) {
		super.onDraw(canvas);
		
		int left = faces[0].rect.left;
		int top = faces[0].rect.top;
		int right = faces[0].rect.right;
		int bottom = faces[0].rect.bottom;
		
		Rect faceRect = new Rect();
		faceRect.set(left, top, right, bottom);
		
		Paint blue = new Paint();
		blue.setColor(Color.BLUE);
		blue.setStyle(Paint.Style.STROKE);
		//Don't know exactly how this will show up, but we might have to draw lines instead of a rectangle?
		
		canvas.drawRect(faceRect, blue); 
		
//		canvas.drawLine(left, top, right, top, blue);
//		canvas.drawLine(left, top, left, bottom, blue);
//		canvas.drawLine(right, top, right, bottom, blue);
//		canvas.drawLine(left, bottom, right, bottom, blue);
			
		invalidate();
		//not sure if this is what I want here, running into confusion on if I want to draw
		//this over the preview? or bitmap of the preview?
	}

}
