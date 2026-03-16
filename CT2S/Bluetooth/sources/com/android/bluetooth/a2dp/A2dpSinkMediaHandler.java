package com.android.bluetooth.a2dp;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Process;
import android.util.Log;

public class A2dpSinkMediaHandler {
    private static final int A2DP_SINK_MEDIA_CONNECT = 0;
    private static final int A2DP_SINK_MEDIA_DISCONNECT = 1;
    private static final int A2DP_SINK_MEDIA_NOT_PLAYING = 3;
    private static final int A2DP_SINK_MEDIA_PLAYING = 2;
    private static final boolean DBG = true;
    private static final boolean INIT_A2DP_SINK_MEDIA = true;
    private static final String TAG = "A2dpSinkMediaHandler";
    private static int audioRecordAudioTrackStatus = 1;
    private static int bufferSizeAudioRecordTrack = 10240;
    private static int sampleRateInHz = 44100;
    private Thread sinkMediaThread = null;
    private AudioRecord audioRecord = null;
    private AudioManager audioManager = null;
    private AudioTrack audioTrack = null;
    public boolean instantiateAudioRecordTrackForMediaPlay = false;

    public A2dpSinkMediaHandler() {
        Log.d(TAG, "Constructor of A2dpSinkMediaHandler");
    }

    public void instantiateAudioRecordAudioTrackForMediaPlay() {
        Log.w(TAG, "instantiate audioRecord for A2DP Sink...");
        this.sinkMediaThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int bufferSize = A2dpSinkMediaHandler.bufferSizeAudioRecordTrack / 20;
                short[] buffer = new short[bufferSize];
                int count = 0;
                long buffcount = 0;
                long atbuffcount = 0;
                long atcount = 0;
                Process.setThreadPriority(-19);
                A2dpSinkMediaHandler.this.audioRecord.startRecording();
                A2dpSinkMediaHandler.this.audioTrack.setPlaybackRate(44100);
                A2dpSinkMediaHandler.this.audioTrack.play();
                while (!A2dpSinkMediaHandler.this.sinkMediaThread.isInterrupted()) {
                    try {
                        int bufferRead = A2dpSinkMediaHandler.this.audioRecord.read(buffer, 0, bufferSize);
                        Log.w(A2dpSinkMediaHandler.TAG, "bufferRead : " + bufferRead);
                        if (bufferRead > 0) {
                            count++;
                            buffcount += (long) bufferRead;
                            int bufferWrite = A2dpSinkMediaHandler.this.audioTrack.write(buffer, 0, bufferRead);
                            atcount++;
                            atbuffcount += (long) bufferWrite;
                            Log.w(A2dpSinkMediaHandler.TAG, "B.bufferWrite : " + bufferWrite);
                        } else {
                            Log.w(A2dpSinkMediaHandler.TAG, "AudioRecord else : ");
                        }
                    } catch (Throwable t) {
                        Log.w(A2dpSinkMediaHandler.TAG, "Audio Record : Read write failed");
                        t.printStackTrace();
                    }
                }
                Log.w(A2dpSinkMediaHandler.TAG, "Media Stopped");
                A2dpSinkMediaHandler.this.sinkMediaThread = null;
            }
        });
        this.sinkMediaThread.start();
    }

    public void initAudioRecordAudioTrack() {
        if (audioRecordAudioTrackStatus == 1) {
            audioRecordAudioTrackStatus = 0;
            Log.w(TAG, "initAudioRecordAudioTrack");
            this.audioRecord = new AudioRecord(0, sampleRateInHz, 12, 2, bufferSizeAudioRecordTrack);
            this.audioTrack = new AudioTrack(3, sampleRateInHz, 12, 2, bufferSizeAudioRecordTrack, 1);
            return;
        }
        Log.w(TAG, "initAudioRecordAudioTrack instance already running");
    }

    public void deInitAudioRecordAudioTrack() {
        if (audioRecordAudioTrackStatus == 2) {
            startRecordingPlaying(false);
        }
        if (audioRecordAudioTrackStatus == 3) {
            audioRecordAudioTrackStatus = 1;
            Log.w(TAG, "deInitAudioRecordAudioTrack");
            this.audioRecord.release();
            this.audioRecord = null;
            this.audioTrack.release();
            this.audioTrack = null;
            return;
        }
        Log.w(TAG, "deInitAudioRecordAudioTrack already done");
    }

    public void setAudioRecordAudioTrackStatus(boolean status) {
        this.instantiateAudioRecordTrackForMediaPlay = status;
        if (this.instantiateAudioRecordTrackForMediaPlay) {
            initAudioRecordAudioTrack();
        } else {
            deInitAudioRecordAudioTrack();
        }
    }

    public void startRecordingPlaying(boolean start) {
        Log.d(TAG, "startRecordingPlaying: start = " + start + ", status = " + audioRecordAudioTrackStatus);
        if (start) {
            if (audioRecordAudioTrackStatus == 0 || 3 == audioRecordAudioTrackStatus) {
                audioRecordAudioTrackStatus = 2;
                instantiateAudioRecordAudioTrackForMediaPlay();
                return;
            }
            return;
        }
        if (audioRecordAudioTrackStatus == 2 && this.sinkMediaThread != null) {
            Log.w(TAG, "Interuppt Thread");
            audioRecordAudioTrackStatus = 3;
            this.sinkMediaThread.interrupt();
            Log.w(TAG, "Audio Track: " + this.audioTrack);
            if (this.audioTrack != null) {
                Log.w(TAG, "stop Audio Track");
                this.audioTrack.stop();
                Log.w(TAG, "after stopping Audio Track");
            }
            Log.w(TAG, "Audio Record: " + this.audioRecord);
            if (this.audioRecord != null) {
                Log.w(TAG, "stop Audio Record");
                this.audioRecord.stop();
                Log.w(TAG, "after stopping Audio Record");
            }
        }
    }
}
