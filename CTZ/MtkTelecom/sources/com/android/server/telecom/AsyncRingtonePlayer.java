package com.android.server.telecom;

import android.media.Ringtone;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.telecom.Log;
import android.telecom.Logging.EventManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.Preconditions;

@VisibleForTesting
public class AsyncRingtonePlayer {
    private Handler mHandler;
    private Ringtone mRingtone;

    public void play(RingtoneFactory ringtoneFactory, Call call) {
        Log.d(this, "Posting play.", new Object[0]);
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = ringtoneFactory;
        someArgsObtain.arg2 = call;
        postMessage(1, true, someArgsObtain);
    }

    public void stop() {
        Log.d(this, "Posting stop.", new Object[0]);
        postMessage(2, false, null);
    }

    private void postMessage(int i, boolean z, SomeArgs someArgs) {
        synchronized (this) {
            if (this.mHandler == null && z) {
                this.mHandler = getNewHandler();
            }
            if (this.mHandler == null) {
                Log.d(this, "Message %d skipped because there is no handler.", new Object[]{Integer.valueOf(i)});
            } else if (i == 3 && someArgs != null) {
                int iIntValue = ((Integer) someArgs.arg1).intValue();
                someArgs.recycle();
                Log.d(this, "Message %d post delayed with %d ms", new Object[]{Integer.valueOf(i), Integer.valueOf(iIntValue)});
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(i), iIntValue);
            } else {
                this.mHandler.obtainMessage(i, someArgs).sendToTarget();
            }
        }
    }

    private Handler getNewHandler() {
        boolean z = true;
        Preconditions.checkState(this.mHandler == null);
        HandlerThread handlerThread = new HandlerThread("ringtone-player");
        handlerThread.start();
        return new Handler(handlerThread.getLooper(), null, z) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        AsyncRingtonePlayer.this.handlePlay((SomeArgs) message.obj);
                        break;
                    case CallState.SELECT_PHONE_ACCOUNT:
                        AsyncRingtonePlayer.this.handleStop();
                        break;
                    case CallState.DIALING:
                        AsyncRingtonePlayer.this.handleRepeat();
                        break;
                }
            }
        };
    }

    private void handlePlay(SomeArgs someArgs) {
        RingtoneFactory ringtoneFactory = (RingtoneFactory) someArgs.arg1;
        Call call = (Call) someArgs.arg2;
        someArgs.recycle();
        if (this.mHandler.hasMessages(2)) {
            return;
        }
        if (Uri.EMPTY.equals(call.getRingtone())) {
            this.mRingtone = null;
            return;
        }
        ThreadUtil.checkNotOnMainThread();
        Log.i(this, "Play ringtone.", new Object[0]);
        if (this.mRingtone == null) {
            this.mRingtone = ringtoneFactory.getRingtone(call);
            if (this.mRingtone == null) {
                Uri ringtone = call.getRingtone();
                Log.addEvent((EventManager.Loggable) null, "ERROR", "Failed to get ringtone from factory. Skipping ringing. Uri was: " + (ringtone == null ? "null" : ringtone.toSafeString()));
                return;
            }
        }
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = 200;
        postMessage(3, true, someArgsObtain);
    }

    private void handleRepeat() {
        if (this.mRingtone == null) {
            return;
        }
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
}
