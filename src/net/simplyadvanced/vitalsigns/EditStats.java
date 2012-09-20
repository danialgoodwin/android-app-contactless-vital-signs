package net.simplyadvanced.vitalsigns;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.support.v4.app.NavUtils;

public class EditStats extends Activity {
	EditText mEditTextAge, editTextSex, editTextWeight, editTextHeight;
    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_stats);
        
        mEditTextAge = (EditText) findViewById(R.id.editTextAge); // Connects variables here to ids in xml
        editTextSex = (EditText) findViewById(R.id.editTextSex);
        editTextWeight = (EditText) findViewById(R.id.editTextWeight);
        editTextHeight = (EditText) findViewById(R.id.editTextHeight);
        
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0); // Load saved stats
        mEditTextAge.setText(settings.getInt("age", 25) + "");
        editTextSex.setText(settings.getString("sex", "Lala"));
        editTextWeight.setText(settings.getInt("weight", 160) + "");
        editTextHeight.setText(settings.getInt("height", 70) + "");
    }

    
    public void Save(View v) {
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
    	editor.putInt("age", Integer.parseInt(mEditTextAge.getText().toString()));
    	editor.putString("sex", editTextSex.getText().toString());
    	editor.putInt("weight", Integer.parseInt(editTextWeight.getText().toString()));
    	editor.putInt("height", Integer.parseInt(editTextHeight.getText().toString()));
    	editor.commit(); // This actually saves the edits

    	finish(); // Navigate back on stack
	}
    public void Cancel(View v) {
    	finish(); // Navigate back on stack
	}
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_edit_stats, menu);
        return true;
    }

    
}
