package net.simplyadvanced.vitalsigns.pupil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import net.simplyadvanced.vitalsigns.R;

public class PupilDilationActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pupil_dilation);

        Button pdn = (Button) findViewById(R.id.PDnext);
        pdn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(PupilDilationActivity.this, PupilDilation2Activity.class));
            }
        });
    }

}
