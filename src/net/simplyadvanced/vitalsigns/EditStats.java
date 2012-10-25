package net.simplyadvanced.vitalsigns;

import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class EditStats extends Activity {
	EditText mEditTextAge, editTextSex, editTextWeight, editTextHeight, editTextPosition;
    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_stats);
        
        mEditTextAge = (EditText) findViewById(R.id.editTextAge); // Connects variables here to ids in xml
        editTextSex = (EditText) findViewById(R.id.editTextSex);
        editTextWeight = (EditText) findViewById(R.id.editTextWeight);
        editTextHeight = (EditText) findViewById(R.id.editTextHeight);
        editTextPosition = (EditText) findViewById(R.id.editTextPosition);
        
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0); // Load saved stats
        mEditTextAge.setText(settings.getInt("age", 23) + "");
        editTextSex.setText(settings.getString("sex", "Male"));
        editTextWeight.setText(settings.getInt("weight", 160) + "");
        editTextHeight.setText(settings.getInt("height", 75) + "");
        editTextPosition.setText(settings.getString("position", "Sitting"));
    }

    
    public void Save(View v) {
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
    	editor.putInt("age", Integer.parseInt(mEditTextAge.getText().toString()));
    	editor.putString("sex", editTextSex.getText().toString());
    	editor.putInt("weight", Integer.parseInt(editTextWeight.getText().toString()));
    	editor.putInt("height", Integer.parseInt(editTextHeight.getText().toString()));
    	editor.putString("position", editTextPosition.getText().toString());
    	editor.commit(); // This line saves the edits

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
