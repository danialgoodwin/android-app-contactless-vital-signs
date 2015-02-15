package net.simplyadvanced.vitalsigns.pupil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import net.simplyadvanced.vitalsigns.R;

public class PupilDilation3Activity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pupil_dilation3);

        Button pdn = (Button) findViewById(R.id.PDnext3);
        pdn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(PupilDilation3Activity.this, PupilDilation4Activity.class));
            }
        });
    }

}
