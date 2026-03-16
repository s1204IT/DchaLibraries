package com.android.server.telecom;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.session.MediaSession;
import android.view.KeyEvent;

final class HeadsetMediaButton extends CallsManagerListenerBase {
    private static final AudioAttributes AUDIO_ATTRIBUTES = new AudioAttributes.Builder().setContentType(1).setUsage(2).build();
    private final CallsManager mCallsManager;
    private final MediaSession mSession;
    private final MediaSession.Callback mSessionCallback = new MediaSession.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent intent) {
            KeyEvent keyEvent = (KeyEvent) intent.getParcelableExtra("android.intent.extra.KEY_EVENT");
            Log.v(this, "SessionCallback.onMediaButton()...  event = %s.", keyEvent);
            if (keyEvent == null || keyEvent.getKeyCode() != 79) {
                return true;
            }
            Log.v(this, "SessionCallback: HEADSETHOOK", new Object[0]);
            boolean zHandleHeadsetHook = HeadsetMediaButton.this.handleHeadsetHook(keyEvent);
            Log.v(this, "==> handleHeadsetHook(): consumed = %b.", Boolean.valueOf(zHandleHeadsetHook));
            return zHandleHeadsetHook;
        }
    };

    HeadsetMediaButton(Context context, CallsManager callsManager) {
        this.mCallsManager = callsManager;
        this.mSession = new MediaSession(context, HeadsetMediaButton.class.getSimpleName());
        this.mSession.setCallback(this.mSessionCallback);
        this.mSession.setFlags(65537);
        this.mSession.setPlaybackToLocal(AUDIO_ATTRIBUTES);
    }

    private boolean handleHeadsetHook(KeyEvent keyEvent) {
        Log.d(this, "handleHeadsetHook()...%s %s", Integer.valueOf(keyEvent.getAction()), Integer.valueOf(keyEvent.getRepeatCount()));
        if (keyEvent.isLongPress()) {
            return this.mCallsManager.onMediaButton(2);
        }
        if (keyEvent.getAction() == 1 && keyEvent.getRepeatCount() == 0) {
            return this.mCallsManager.onMediaButton(1);
        }
        return true;
    }

    @Override
    public void onCallAdded(Call call) {
        if (!this.mSession.isActive()) {
            this.mSession.setActive(true);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        if (!this.mCallsManager.hasAnyCalls() && this.mSession.isActive()) {
            this.mSession.setActive(false);
        }
    }
}
