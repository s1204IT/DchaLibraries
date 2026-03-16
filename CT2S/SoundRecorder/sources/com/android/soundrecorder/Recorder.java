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

    public void saveState(Bundle recorderState) {
        recorderState.putString("sample_path", this.mSampleFile.getAbsolutePath());
        recorderState.putInt("sample_length", this.mSampleLength);
    }

    public int getMaxAmplitude() {
        if (this.mState != 1) {
            return 0;
        }
        return this.mRecorder.getMaxAmplitude();
    }

    public void restoreState(Bundle recorderState) {
        int sampleLength;
        String samplePath = recorderState.getString("sample_path");
        if (samplePath != null && (sampleLength = recorderState.getInt("sample_length", -1)) != -1) {
            File file = new File(samplePath);
            if (file.exists()) {
                if (this.mSampleFile == null || this.mSampleFile.getAbsolutePath().compareTo(file.getAbsolutePath()) != 0) {
                    delete();
                    this.mSampleFile = file;
                    this.mSampleLength = sampleLength;
                    signalStateChanged(0);
                }
            }
        }
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        this.mOnStateChangedListener = listener;
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

    public void startRecording(int outputfileformat, String extension, Context context, int bitRate) {
        boolean isInCall = true;
        stop();
        if (this.mSampleFile == null) {
            File sampleDir = Environment.getExternalStorageDirectory();
            if (!sampleDir.canWrite()) {
                sampleDir = new File("/sdcard/sdcard");
            }
            try {
                this.mSampleFile = File.createTempFile("recording", extension, sampleDir);
            } catch (IOException e) {
                setError(1);
                return;
            }
        }
        this.mRecorder = new MediaRecorder();
        this.mRecorder.setAudioSource(1);
        this.mRecorder.setOutputFormat(outputfileformat);
        this.mRecorder.setAudioEncoder(1);
        this.mRecorder.setAudioEncodingBitRate(bitRate);
        this.mRecorder.setOutputFile(this.mSampleFile.getAbsolutePath());
        try {
            this.mRecorder.prepare();
            try {
                this.mRecorder.start();
                this.mSampleStart = System.currentTimeMillis();
                setState(1);
            } catch (RuntimeException e2) {
                AudioManager audioMngr = (AudioManager) context.getSystemService("audio");
                if (audioMngr.getMode() != 2 && audioMngr.getMode() != 3) {
                    isInCall = false;
                }
                if (isInCall) {
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
        if (this.mRecorder != null) {
            this.mRecorder.stop();
            this.mRecorder.release();
            this.mRecorder = null;
            this.mSampleLength = (int) ((System.currentTimeMillis() - this.mSampleStart) / 1000);
            setState(0);
        }
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
        if (this.mPlayer != null) {
            this.mPlayer.stop();
            this.mPlayer.release();
            this.mPlayer = null;
            setState(0);
        }
    }

    public void stop() {
        stopRecording();
        stopPlayback();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        stop();
        setError(1);
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stop();
    }

    private void setState(int state) {
        if (state != this.mState) {
            this.mState = state;
            signalStateChanged(this.mState);
        }
    }

    private void signalStateChanged(int state) {
        if (this.mOnStateChangedListener != null) {
            this.mOnStateChangedListener.onStateChanged(state);
        }
    }

    private void setError(int error) {
        if (this.mOnStateChangedListener != null) {
            this.mOnStateChangedListener.onError(error);
        }
    }
}
