package com.android.server.telecom;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.session.MediaSession;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telecom.Log;
import android.view.KeyEvent;
import com.android.server.telecom.TelecomSystem;

public class HeadsetMediaButton extends CallsManagerListenerBase {
    private static final AudioAttributes AUDIO_ATTRIBUTES = new AudioAttributes.Builder().setContentType(1).setUsage(2).build();
    private final CallsManager mCallsManager;
    private final Context mContext;
    private KeyEvent mLastHookEvent;
    private final TelecomSystem.SyncRoot mLock;
    private MediaSession mSession;
    private final MediaSession.Callback mSessionCallback = new MediaSession.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent intent) {
            boolean zHandleCallMediaButton;
            try {
                Log.startSession("HMB.oMBE");
                KeyEvent keyEvent = (KeyEvent) intent.getParcelableExtra("android.intent.extra.KEY_EVENT");
                Log.v(this, "SessionCallback.onMediaButton()...  event = %s.", new Object[]{keyEvent});
                if (keyEvent == null || !(keyEvent.getKeyCode() == 79 || keyEvent.getKeyCode() == 85)) {
                    return true;
                }
                synchronized (HeadsetMediaButton.this.mLock) {
                    Log.v(this, "SessionCallback: HEADSETHOOK/MEDIA_PLAY_PAUSE", new Object[0]);
                    zHandleCallMediaButton = HeadsetMediaButton.this.handleCallMediaButton(keyEvent);
                    Log.v(this, "==> handleCallMediaButton(): consumed = %b.", new Object[]{Boolean.valueOf(zHandleCallMediaButton)});
                }
                return zHandleCallMediaButton;
            } finally {
                Log.endSession();
            }
        }
    };
    private final Handler mMediaSessionHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case CallState.NEW:
                    MediaSession mediaSession = new MediaSession(HeadsetMediaButton.this.mContext, HeadsetMediaButton.class.getSimpleName());
                    mediaSession.setCallback(HeadsetMediaButton.this.mSessionCallback);
                    mediaSession.setFlags(65537);
                    mediaSession.setPlaybackToLocal(HeadsetMediaButton.AUDIO_ATTRIBUTES);
                    HeadsetMediaButton.this.mSession = mediaSession;
                    break;
                case 1:
                    if (HeadsetMediaButton.this.mSession != null) {
                        boolean z = message.arg1 != 0;
                        if (z != HeadsetMediaButton.this.mSession.isActive()) {
                            HeadsetMediaButton.this.mSession.setActive(z);
                        }
                    }
                    break;
            }
        }
    };

    public HeadsetMediaButton(Context context, CallsManager callsManager, TelecomSystem.SyncRoot syncRoot) {
        this.mContext = context;
        this.mCallsManager = callsManager;
        this.mLock = syncRoot;
        this.mMediaSessionHandler.obtainMessage(0).sendToTarget();
    }

    private boolean handleCallMediaButton(KeyEvent keyEvent) {
        Log.d(this, "handleCallMediaButton()...%s %s", new Object[]{Integer.valueOf(keyEvent.getAction()), Integer.valueOf(keyEvent.getRepeatCount())});
        if (keyEvent.getAction() == 0) {
            this.mLastHookEvent = keyEvent;
        }
        if (keyEvent.isLongPress()) {
            return this.mCallsManager.onMediaButton(2);
        }
        if (keyEvent.getAction() == 1 && this.mLastHookEvent != null && this.mLastHookEvent.getRepeatCount() == 0) {
            return this.mCallsManager.onMediaButton(1);
        }
        if (keyEvent.getAction() != 0) {
            this.mLastHookEvent = null;
        }
        return true;
    }

    @Override
    public void onCallAdded(Call call) {
        if (call.isExternalCall()) {
            return;
        }
        this.mMediaSessionHandler.obtainMessage(1, 1, 0).sendToTarget();
    }

    @Override
    public void onCallRemoved(Call call) {
        if (!call.isExternalCall() && !this.mCallsManager.hasAnyCalls()) {
            this.mMediaSessionHandler.obtainMessage(1, 0, 0).sendToTarget();
        }
    }

    @Override
    public void onExternalCallChanged(Call call, boolean z) {
        if (z) {
            onCallRemoved(call);
        } else {
            onCallAdded(call);
        }
    }
}
