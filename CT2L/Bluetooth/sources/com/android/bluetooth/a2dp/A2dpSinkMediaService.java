package com.android.bluetooth.a2dp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class A2dpSinkMediaService extends Service {
    private static final String TAG = "A2dpSinkMediaService";
    private A2dpSinkMediaHandler mA2dpSinkMediaHandler = new A2dpSinkMediaHandler();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.bluetooth.a2dp-sink.profile.action.PLAYING_STATE_CHANGED")) {
                int newState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
                Log.d(A2dpSinkMediaService.TAG, "Received ACTION_PLAYING_STATE_CHANGED intent, newState = " + newState);
                if (newState == 11) {
                    A2dpSinkMediaService.this.mA2dpSinkMediaHandler.startRecordingPlaying(false);
                    return;
                } else {
                    if (newState == 10) {
                        A2dpSinkMediaService.this.mA2dpSinkMediaHandler.startRecordingPlaying(true);
                        return;
                    }
                    return;
                }
            }
            if (intent.getAction().equals("android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED")) {
                int newState2 = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
                Log.d(A2dpSinkMediaService.TAG, "Received ACTION_CONNECTION_STATE_CHANGED intent, newState = " + newState2);
                if (newState2 == 0) {
                    A2dpSinkMediaService.this.mA2dpSinkMediaHandler.deInitAudioRecordAudioTrack();
                } else if (newState2 == 2) {
                    A2dpSinkMediaService.this.mA2dpSinkMediaHandler.initAudioRecordAudioTrack();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        IntentFilter filter = new IntentFilter("android.bluetooth.a2dp-sink.profile.action.PLAYING_STATE_CHANGED");
        filter.addAction("android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED");
        registerReceiver(this.mReceiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        unregisterReceiver(this.mReceiver);
    }
}
