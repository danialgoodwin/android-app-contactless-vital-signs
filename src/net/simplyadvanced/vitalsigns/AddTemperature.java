package net.simplyadvanced.vitalsigns;

import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class AddTemperature extends Activity {
    public static final String PREFS_NAME = "MyPrefsFile";
	EditText mEditTextTemperature;
	SharedPreferences settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_temperature);
        
        mEditTextTemperature = (EditText) findViewById(R.id.editTextTemperature);
        

    	settings = getSharedPreferences(PREFS_NAME, 0);
    	

        mEditTextTemperature.setText(settings.getString("temperature", ""));
    }

    public void Save(View v) {
    	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
    	editor.putString("temperature", mEditTextTemperature.getText().toString());
    	editor.commit(); // This line saves the edits

    	finish(); // Navigate back on stack
	}
    public void Cancel(View v) {
    	finish(); // Navigate back on stack
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_add_temperature, menu);
        return true;
    }
}
