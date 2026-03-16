package com.android.server.telecom;

import android.app.StatusBarManager;
import android.content.Context;

final class StatusBarNotifier extends CallsManagerListenerBase {
    private final CallsManager mCallsManager;
    private final Context mContext;
    private boolean mIsShowingMute;
    private boolean mIsShowingSpeakerphone;
    private final StatusBarManager mStatusBarManager;

    StatusBarNotifier(Context context, CallsManager callsManager) {
        this.mContext = context;
        this.mCallsManager = callsManager;
        this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
    }

    @Override
    public void onCallRemoved(Call call) {
        if (!this.mCallsManager.hasAnyCalls()) {
            notifyMute(false);
            notifySpeakerphone(false);
        }
    }

    void notifyMute(boolean z) {
        if (!this.mCallsManager.hasAnyCalls()) {
            z = false;
        }
        if (this.mIsShowingMute != z) {
            Log.d(this, "Mute status bar icon being set to %b", Boolean.valueOf(z));
            if (z) {
                this.mStatusBarManager.setIcon("mute", android.R.drawable.stat_notify_call_mute, 0, this.mContext.getString(R.string.accessibility_call_muted));
            } else {
                this.mStatusBarManager.removeIcon("mute");
            }
            this.mIsShowingMute = z;
        }
    }

    void notifySpeakerphone(boolean z) {
        if (!this.mCallsManager.hasAnyCalls()) {
            z = false;
        }
        if (this.mIsShowingSpeakerphone != z) {
            Log.d(this, "Speakerphone status bar icon being set to %b", Boolean.valueOf(z));
            if (z) {
                this.mStatusBarManager.setIcon("speakerphone", android.R.drawable.stat_sys_speakerphone, 0, this.mContext.getString(R.string.accessibility_speakerphone_enabled));
            } else {
                this.mStatusBarManager.removeIcon("speakerphone");
            }
            this.mIsShowingSpeakerphone = z;
        }
    }
}
