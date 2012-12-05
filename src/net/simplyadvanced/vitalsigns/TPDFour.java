package net.simplyadvanced.vitalsigns;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TPDFour extends Activity implements OnClickListener {
	private TPDFour _activity;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		_activity = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tpd_four);
		
		Button pdn4 = (Button) findViewById(R.id.PDnext4);
		pdn4.setOnClickListener(_activity);
	}

	public void onClick(View v) {
		finish();
	}

}
