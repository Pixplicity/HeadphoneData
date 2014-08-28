package com.pixplicity.headphonedata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Derived from http://www.devlper.com/2010/12/android-audio-recording-part-2/
 * 
 * @author Pixplicity
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";

    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private Button mBtRecord, mBtPlay;

    private AudioRecord mRecorder = null;
    private int mBufferSize = 0;
    private Thread mRecordingThread = null;
    private boolean mIsRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtRecord = (Button) findViewById(R.id.bt_record);
        mBtRecord.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View _v) {
                if (mIsRecording) {
                    stopRecording();
                    mBtRecord.setText(R.string.start_recording);
                } else {
                    startRecording();
                    mBtRecord.setText(R.string.stop_recording);
                }
            }
        });
        mBtPlay = (Button) findViewById(R.id.bt_play);
        // TODO not working
        mBtPlay.setVisibility(View.GONE);
        mBtPlay.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View _v) {
                playRecording();
            }
        });
    }

    private void playRecording() {
        // TODO
        /*
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                CHANNEL_CONFIG_OUT, ENCODING, mBuffer.length * 2, AudioTrack.MODE_STATIC);
        audioTrack.write(mBuffer, 0, mBuffer.length);
        audioTrack.play();
        audioTrack.release();
        */
    }

    private String getFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        return file.getAbsolutePath() + "/" + System.currentTimeMillis()
                + AUDIO_RECORDER_FILE_EXT_WAV;
    }

    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists()) {
            tempFile.delete();
        }

        return file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE;
    }

    private void startRecording() {
        mBufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING);
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, mBufferSize);

        mRecorder.startRecording();

        mIsRecording = true;

        mRecordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");

        mRecordingThread.start();
    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[mBufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int read = 0;

        if (null != os) {
            while (mIsRecording) {
                read = mRecorder.read(data, 0, mBufferSize);

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (null != mRecorder) {
            mIsRecording = false;

            mRecorder.stop();
            mRecorder.release();

            mRecorder = null;
            mRecordingThread = null;
        }

        copyWaveFile(getTempFilename(), getFilename());
        deleteTempFile();

        new AlertDialog.Builder(this)
                .setTitle("Recoding complete")
                .setMessage("Saved as:\n" + getFilename())
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());

        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

        byte[] data = new byte[mBufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            Log.d(TAG, "File size: " + totalDataLen);

            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) (totalDataLen >> 8 & 0xff);
        header[6] = (byte) (totalDataLen >> 16 & 0xff);
        header[7] = (byte) (totalDataLen >> 24 & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) (longSampleRate >> 8 & 0xff);
        header[26] = (byte) (longSampleRate >> 16 & 0xff);
        header[27] = (byte) (longSampleRate >> 24 & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) (byteRate >> 8 & 0xff);
        header[30] = (byte) (byteRate >> 16 & 0xff);
        header[31] = (byte) (byteRate >> 24 & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) (totalAudioLen >> 8 & 0xff);
        header[42] = (byte) (totalAudioLen >> 16 & 0xff);
        header[43] = (byte) (totalAudioLen >> 24 & 0xff);

        out.write(header, 0, 44);
    }

}
