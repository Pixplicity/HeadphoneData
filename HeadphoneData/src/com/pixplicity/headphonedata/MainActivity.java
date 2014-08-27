package com.pixplicity.headphonedata;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_CONFIGURATION_MONO; // =
                                                                                      // AudioFormat.CHANNEL_IN_MONO;
    private static final int SAMPLE_RATE = 44100;

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startAudioRecording();
    }

    private void startAudioRecording() {
        int minSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING);
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                CHANNEL_CONFIG, ENCODING, minSize);

        int readSize = minSize;
        int index = 0;
        short[] buffer = new short[readSize];
        Log.d(TAG, "starting...");
        audioRecord.startRecording();
        Log.d(TAG, "reading " + readSize + " bytes...");
        audioRecord.read(buffer, index, readSize);
        String msg = "[";
        for (int i = 0; i < buffer.length; i++) {
            msg += buffer[i] + ", ";
        }
        msg += "]";
        Log.d(TAG, "read " + readSize + " bytes: " + msg);
        Log.d(TAG, "stopping...");
        audioRecord.stop();
        Log.d(TAG, "stopped");

        audioRecord.release();
    }
}
