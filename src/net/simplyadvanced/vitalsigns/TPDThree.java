package net.simplyadvanced.vitalsigns;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TPDThree extends Activity implements OnClickListener {
	private TPDThree _activity;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		_activity = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tpd_three);
		
		Button pdn3 = (Button) findViewById(R.id.PDnext3);
		pdn3.setOnClickListener(_activity);
	}

	public void onClick(View v) {
		finish();
		startActivity(new Intent("net.simplyadvanced.vitalsigns.TPDFOUR"));
	}

}
