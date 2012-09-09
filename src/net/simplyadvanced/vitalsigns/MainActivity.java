package net.simplyadvanced.vitalsigns;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v4.app.NavUtils;

public class MainActivity extends Activity {
	private MainActivity _activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        _activity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }


	public void goToTestVitalSigns(View v) {
    	startActivity(new Intent(_activity, TestVitalSigns.class));
	}
	public void goTo(View v) {
		setAlertDialogs(null, "Ex: Heart Rate", "Right Here", "The is for a single vital sign. To be complete.");
	}
    
	public void setAlertDialogs(String mode, String title, String location, String description) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title)
			   .setMessage(description + "\n\n" + "\n\nLocation: " + location)
		       .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
//		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
//		           public void onClick(DialogInterface dialog, int id) {
//		                dialog.cancel();
//		           }
//		       });
		AlertDialog alert = builder.create();
		alert.show();
	}
    
    
}
