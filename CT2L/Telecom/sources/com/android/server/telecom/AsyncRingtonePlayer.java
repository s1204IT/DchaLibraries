package com.android.server.telecom;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import com.android.internal.util.Preconditions;

class AsyncRingtonePlayer {
    private final Context mContext;
    private Handler mHandler;
    private Ringtone mRingtone;

    AsyncRingtonePlayer(Context context) {
        this.mContext = context;
    }

    void play(Uri uri) {
        Log.d(this, "Posting play.", new Object[0]);
        postMessage(1, true, uri);
    }

    void stop() {
        Log.d(this, "Posting stop.", new Object[0]);
        postMessage(2, false, null);
    }

    private void postMessage(int i, boolean z, Uri uri) {
        synchronized (this) {
            if (this.mHandler == null && z) {
                this.mHandler = getNewHandler();
            }
            if (this.mHandler == null) {
                Log.d(this, "Message %d skipped because there is no handler.", Integer.valueOf(i));
            } else {
                this.mHandler.obtainMessage(i, uri).sendToTarget();
            }
        }
    }

    private Handler getNewHandler() {
        Preconditions.checkState(this.mHandler == null);
        HandlerThread handlerThread = new HandlerThread("ringtone-player");
        handlerThread.start();
        return new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        AsyncRingtonePlayer.this.handlePlay((Uri) message.obj);
                        break;
                    case 2:
                        AsyncRingtonePlayer.this.handleStop();
                        break;
                    case 3:
                        AsyncRingtonePlayer.this.handleRepeat();
                        break;
                }
            }
        };
    }

    private void handlePlay(Uri uri) {
        if (!this.mHandler.hasMessages(2)) {
            ThreadUtil.checkNotOnMainThread();
            Log.i(this, "Play ringtone.", new Object[0]);
            if (this.mRingtone == null) {
                this.mRingtone = getRingtone(uri);
                if (this.mRingtone == null) {
                    handleStop();
                    return;
                }
            }
            handleRepeat();
        }
    }

    private void handleRepeat() {
        if (this.mRingtone != null) {
            if (this.mRingtone.isPlaying()) {
                Log.d(this, "Ringtone already playing.", new Object[0]);
            } else {
                this.mRingtone.play();
                Log.i(this, "Repeat ringtone.", new Object[0]);
            }
            synchronized (this) {
                if (!this.mHandler.hasMessages(3)) {
                    this.mHandler.sendEmptyMessageDelayed(3, 3000L);
                }
            }
        }
    }

    private void handleStop() {
        ThreadUtil.checkNotOnMainThread();
        Log.i(this, "Stop ringtone.", new Object[0]);
        if (this.mRingtone != null) {
            Log.d(this, "Ringtone.stop() invoked.", new Object[0]);
            this.mRingtone.stop();
            this.mRingtone = null;
        }
        synchronized (this) {
            this.mHandler.removeMessages(3);
            if (this.mHandler.hasMessages(1)) {
                Log.v(this, "Keeping alive ringtone thread for subsequent play request.", new Object[0]);
            } else {
                this.mHandler.removeMessages(2);
                this.mHandler.getLooper().quitSafely();
                this.mHandler = null;
                Log.v(this, "Handler cleared.", new Object[0]);
            }
        }
    }

    private Ringtone getRingtone(Uri uri) {
        if (uri == null) {
            uri = Settings.System.DEFAULT_RINGTONE_URI;
        }
        Ringtone ringtone = RingtoneManager.getRingtone(this.mContext, uri);
        if (ringtone != null) {
            ringtone.setStreamType(2);
        }
        return ringtone;
    }
}
