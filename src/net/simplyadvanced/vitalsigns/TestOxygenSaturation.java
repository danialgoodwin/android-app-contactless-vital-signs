package net.simplyadvanced.vitalsigns;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class TestOxygenSaturation extends Activity {
	TestOxygenSaturation _activity;
	EditText mEditText1, mEditText2;
	
    int config = 1; // 1 yes, 0 no // Select "1" if user is using PPG because IR will be higher than usual

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _activity = this;
        setContentView(R.layout.activity_test_oxygen_saturation);
        
        mEditText1 = (EditText) findViewById(R.id.editText1);
        mEditText2 = (EditText) findViewById(R.id.editText2);
        
    }
    
	public void onClickCalculateO2(View v) {
		calculateO2(Double.parseDouble(mEditText1.getText().toString()), Double.parseDouble(mEditText2.getText().toString()));
	}
    
    
    public void calculateO2(double r, double nearInfrared) { // TODO: change red to array
		double scaledown = 1;
		double red = r;	//red intensity of face // replace 9 with red
		double nir = nearInfrared; //nir intensity of face // nearInfrared // TODO: replace .5 with nearInfrared
		double result = 0;	//o2 level of people
		
		if(config == 1) {
			scaledown = configure(red, nir);
			nir = nir/scaledown;
			result = o2_level(red, nir);
			config = 0; // So that it is only ran once
		} else {
			result = o2_level(red, nir);
			Toast.makeText(_activity, "result: " + result , Toast.LENGTH_LONG).show();
		}
	}
	
	public double o2_level(double hbO2, double hb) {
		Toast.makeText(_activity, "result: " + hbO2/(hbO2+hb), Toast.LENGTH_LONG).show();
		return hbO2/(hbO2+hb);
	}
	
	public double configure(double red, double nir) {
		double target_o2_level = .98;
		double hbO2 = red;
		double hb = nir;
		double scaledown = 1;
		if(.02*hbO2 < hb) {
			scaledown = hb / (.02 * hbO2);
		}
		return scaledown;
	}
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_test_oxygen_saturation, menu);
        return true;
    }

    
}
