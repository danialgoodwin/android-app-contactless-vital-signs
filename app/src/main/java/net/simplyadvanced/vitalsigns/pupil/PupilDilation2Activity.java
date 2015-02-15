package net.simplyadvanced.vitalsigns.pupil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import net.simplyadvanced.vitalsigns.R;

public class PupilDilation2Activity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pupil_dilation2);

        Button pdn2 = (Button) findViewById(R.id.PDnext2);
        pdn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(PupilDilation2Activity.this, PupilDilation3Activity.class));
            }
        });
    }

}
