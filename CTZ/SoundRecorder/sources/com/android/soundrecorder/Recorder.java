package com.android.soundrecorder;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import java.io.File;
import java.io.IOException;

public class Recorder implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    int mState = 0;
    OnStateChangedListener mOnStateChangedListener = null;
    long mSampleStart = 0;
    int mSampleLength = 0;
    File mSampleFile = null;
    MediaRecorder mRecorder = null;
    MediaPlayer mPlayer = null;

    public interface OnStateChangedListener {
        void onError(int i);

        void onStateChanged(int i);
    }

    public void saveState(Bundle bundle) {
        bundle.putString("sample_path", this.mSampleFile.getAbsolutePath());
        bundle.putInt("sample_length", this.mSampleLength);
    }

    public int getMaxAmplitude() {
        if (this.mState != 1) {
            return 0;
        }
        return this.mRecorder.getMaxAmplitude();
    }

    public void restoreState(Bundle bundle) {
        int i;
        String string = bundle.getString("sample_path");
        if (string == null || (i = bundle.getInt("sample_length", -1)) == -1) {
            return;
        }
        File file = new File(string);
        if (!file.exists()) {
            return;
        }
        if (this.mSampleFile != null && this.mSampleFile.getAbsolutePath().compareTo(file.getAbsolutePath()) == 0) {
            return;
        }
        delete();
        this.mSampleFile = file;
        this.mSampleLength = i;
        signalStateChanged(0);
    }

    public void setOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        this.mOnStateChangedListener = onStateChangedListener;
    }

    public int state() {
        return this.mState;
    }

    public int progress() {
        if (this.mState == 1 || this.mState == 2) {
            return (int) ((System.currentTimeMillis() - this.mSampleStart) / 1000);
        }
        return 0;
    }

    public int sampleLength() {
        return this.mSampleLength;
    }

    public File sampleFile() {
        return this.mSampleFile;
    }

    public void delete() {
        stop();
        if (this.mSampleFile != null) {
            this.mSampleFile.delete();
        }
        this.mSampleFile = null;
        this.mSampleLength = 0;
        signalStateChanged(0);
    }

    public void clear() {
        stop();
        this.mSampleLength = 0;
        signalStateChanged(0);
    }

    public void startRecording(int i, String str, Context context) {
        stop();
        boolean z = true;
        if (this.mSampleFile == null) {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            if (!externalStorageDirectory.canWrite()) {
                externalStorageDirectory = new File("/sdcard/sdcard");
            }
            try {
                this.mSampleFile = File.createTempFile("recording", str, externalStorageDirectory);
            } catch (IOException e) {
                setError(1);
                return;
            }
        }
        this.mRecorder = new MediaRecorder();
        this.mRecorder.setAudioSource(1);
        this.mRecorder.setOutputFormat(i);
        this.mRecorder.setAudioEncoder(1);
        this.mRecorder.setOutputFile(this.mSampleFile.getAbsolutePath());
        try {
            this.mRecorder.prepare();
            try {
                this.mRecorder.start();
                this.mSampleStart = System.currentTimeMillis();
                setState(1);
            } catch (RuntimeException e2) {
                AudioManager audioManager = (AudioManager) context.getSystemService("audio");
                if (audioManager.getMode() != 2 && audioManager.getMode() != 3) {
                    z = false;
                }
                if (z) {
                    setError(3);
                } else {
                    setError(2);
                }
                this.mRecorder.reset();
                this.mRecorder.release();
                this.mRecorder = null;
            }
        } catch (IOException e3) {
            setError(2);
            this.mRecorder.reset();
            this.mRecorder.release();
            this.mRecorder = null;
        }
    }

    public void stopRecording() {
        if (this.mRecorder == null) {
            return;
        }
        this.mRecorder.stop();
        this.mRecorder.release();
        this.mRecorder = null;
        this.mSampleLength = (int) ((System.currentTimeMillis() - this.mSampleStart) / 1000);
        setState(0);
    }

    public void startPlayback() {
        stop();
        this.mPlayer = new MediaPlayer();
        try {
            this.mPlayer.setDataSource(this.mSampleFile.getAbsolutePath());
            this.mPlayer.setOnCompletionListener(this);
            this.mPlayer.setOnErrorListener(this);
            this.mPlayer.prepare();
            this.mPlayer.start();
            this.mSampleStart = System.currentTimeMillis();
            setState(2);
        } catch (IOException e) {
            setError(1);
            this.mPlayer = null;
        } catch (IllegalArgumentException e2) {
            setError(2);
            this.mPlayer = null;
        }
    }

    public void stopPlayback() {
        if (this.mPlayer == null) {
            return;
        }
        this.mPlayer.stop();
        this.mPlayer.release();
        this.mPlayer = null;
        setState(0);
    }

    public void stop() {
        stopRecording();
        stopPlayback();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        stop();
        setError(1);
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stop();
    }

    private void setState(int i) {
        if (i == this.mState) {
            return;
        }
        this.mState = i;
        signalStateChanged(this.mState);
    }

    private void signalStateChanged(int i) {
        if (this.mOnStateChangedListener != null) {
            this.mOnStateChangedListener.onStateChanged(i);
        }
    }

    private void setError(int i) {
        if (this.mOnStateChangedListener != null) {
            this.mOnStateChangedListener.onError(i);
        }
    }
}
