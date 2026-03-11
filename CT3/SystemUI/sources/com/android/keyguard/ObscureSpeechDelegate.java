package com.android.keyguard;

import android.R;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

class ObscureSpeechDelegate extends View.AccessibilityDelegate {
    static boolean sAnnouncedHeadset = false;
    private final AudioManager mAudioManager;
    private final ContentResolver mContentResolver;

    public ObscureSpeechDelegate(Context context) {
        this.mContentResolver = context.getContentResolver();
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    @Override
    public void sendAccessibilityEvent(View host, int eventType) {
        super.sendAccessibilityEvent(host, eventType);
        if (eventType != 32768 || sAnnouncedHeadset || !shouldObscureSpeech()) {
            return;
        }
        sAnnouncedHeadset = true;
        host.announceForAccessibility(host.getContext().getString(R.string.httpErrorOk));
    }

    @Override
    public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(host, event);
        if (event.getEventType() == 16384 || !shouldObscureSpeech()) {
            return;
        }
        event.getText().clear();
        event.setContentDescription(host.getContext().getString(R.string.httpErrorProxyAuth));
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(host, info);
        if (!shouldObscureSpeech()) {
            return;
        }
        Context ctx = host.getContext();
        info.setText(null);
        info.setContentDescription(ctx.getString(R.string.httpErrorProxyAuth));
    }

    private boolean shouldObscureSpeech() {
        return (Settings.Secure.getIntForUser(this.mContentResolver, "speak_password", 0, -2) != 0 || this.mAudioManager.isWiredHeadsetOn() || this.mAudioManager.isBluetoothA2dpOn()) ? false : true;
    }
}
