package net.simplyadvanced.vitalsigns;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.support.v4.app.NavUtils;

public class TestPupilDilation extends Activity implements OnClickListener {
	private TestPupilDilation _activity;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	_activity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_pupil_dilation);
        
        Button pdn = (Button) findViewById(R.id.PDnext);
        pdn.setOnClickListener(_activity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_test_pupil_dilation, menu);
        return true;
    }

	public void onClick(View v) {
		finish();
		startActivity(new Intent("net.simplyadvanced.vitalsigns.TPDTWO"));
	}

    
}
