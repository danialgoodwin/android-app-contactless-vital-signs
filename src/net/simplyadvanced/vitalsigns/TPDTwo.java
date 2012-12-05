package net.simplyadvanced.vitalsigns;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TPDTwo extends Activity implements OnClickListener {
	private TPDTwo _activity;
		@Override
	protected void onCreate(Bundle savedInstanceState) {
		_activity = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tpd_two);
		
		Button pdn2 = (Button) findViewById(R.id.PDnext2);
		pdn2.setOnClickListener(_activity);
	}

	public void onClick(View v) {
		finish();
		startActivity(new Intent("net.simplyadvanced.vitalsigns.TPDTHREE"));	
	}
	
	

}
