package net.simplyadvanced.vitalsigns;

import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class TestOxygenSaturation extends Activity {
    public static final String PREFS_NAME = "MyPrefsFile";
	EditText mEditTextInputTemperature;
	TextView mTextViewOutputTemperature;
	SharedPreferences settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_oxygen_saturation);
        
        mEditTextInputTemperature = (EditText) findViewById(R.id.editTextInputTemperature); // Connects variables here to ids in xml
        mTextViewOutputTemperature = (TextView) findViewById(R.id.textViewOutputTemperature); 
        
    	settings = getSharedPreferences(PREFS_NAME, 0); // Load saved stats
        mEditTextInputTemperature.setText( String.format("%.2f",settings.getFloat("temperature", 0)) );
    }

    public void Save(View v) {
    	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
    	editor.putFloat("skinTemperature", Float.parseFloat(mEditTextInputTemperature.getText().toString()));
    	editor.commit(); // This line saves the edits
    	
    	float ambientTemp = 70; //TODO: link to variable
    	float measurement = settings.getFloat("skinTemperature", 88);
    	float modifiedTemp = (float) convertTemp((double)measurement, (double)ambientTemp);
    	
    	editor.putFloat("internalTemperature", modifiedTemp);
    	editor.commit();
    	
    	//finish(); // Navigate back on stack
	}
    public void Cancel(View v) {
    	//finish(); // Navigate back on stack
	}

	public double convertTemp(double measuredSkinTemperature, double atmosphericTemperature) {
		//etemp: external (measured) temperature, atemp: ambient temperature
		double factor = 3; //approximate constant factor irregardless of C/F
		double itemp = 1/(factor-1)*(factor*measuredSkinTemperature - atmosphericTemperature);
		return itemp;
	}
    
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_test_oxygen_saturation, menu);
        return true;
    }

    
}
