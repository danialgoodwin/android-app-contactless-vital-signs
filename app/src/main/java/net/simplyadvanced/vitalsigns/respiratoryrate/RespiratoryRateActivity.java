package net.simplyadvanced.vitalsigns.respiratoryrate;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import net.simplyadvanced.vitalsigns.R;

import java.io.IOException;

public class RespiratoryRateActivity extends Activity {
    private static final String LOG_TAG = "AudioRecordTest";

    private static String mFileName =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/audiorecordtest.3gp";

    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;

    private Button mButtonRecordStart, mButtonPlayback;

    boolean mStartRecording = true;
    boolean mStartPlaying = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respiratory_rate);

        mButtonRecordStart = (Button) findViewById(R.id.buttonRecordStart);
        mButtonPlayback = (Button) findViewById(R.id.buttonPlayback);
    }

    public void onClickRecordStart(View v) {
        onRecord(mStartRecording);
        if (mStartRecording) {
            mButtonRecordStart.setText("Stop recording");
        } else {
            mButtonRecordStart.setText("Start recording");
        }
        mStartRecording = !mStartRecording;
    }

    public void onClickRecordStop(View v) {

    }

    public void onClickPlayback(View v) {
        onPlay(mStartPlaying);
        if (mStartPlaying) {
            mButtonPlayback.setText("Stop playing");
        } else {
            mButtonPlayback.setText("Start playing");
        }
        mStartPlaying = !mStartPlaying;
    }



    /** Audio Filter Stuff */
    //private double[] source = null;
    //private int count = (Integer) null;
    //private int sampleRate = (Integer) null;


    //AudioFilter mAudioFilter = new AudioFilter();
    //mAudioFilter.calculate(/*double[]*/ source, /*int*/ count, /*int*/ samplerate); // Edits the audio file passed in

    // I should uncomment this
    //AudioFilter.calculate(/*double[]*/source, /*int*/ count, /*int*/ sampleRate); // Edits the audio file passed in



    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

}
