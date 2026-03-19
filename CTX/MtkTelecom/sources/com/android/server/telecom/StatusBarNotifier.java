package com.android.server.telecom;

import android.app.StatusBarManager;
import android.content.Context;
import android.telecom.Log;
import com.android.internal.annotations.VisibleForTesting;

@VisibleForTesting
public class StatusBarNotifier extends CallsManagerListenerBase {
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

    @VisibleForTesting
    public void notifyMute(boolean z) {
        if (!this.mCallsManager.hasAnyCalls()) {
            z = false;
        }
        if (this.mIsShowingMute == z) {
            return;
        }
        Log.d(this, "Mute status bar icon being set to %b", new Object[]{Boolean.valueOf(z)});
        if (z) {
            this.mStatusBarManager.setIcon("mute", android.R.drawable.stat_notify_call_mute, 0, this.mContext.getString(R.string.accessibility_call_muted));
        } else {
            this.mStatusBarManager.removeIcon("mute");
        }
        this.mIsShowingMute = z;
    }

    @VisibleForTesting
    public void notifySpeakerphone(boolean z) {
        if (!this.mCallsManager.hasAnyCalls()) {
            z = false;
        }
        if (this.mIsShowingSpeakerphone == z) {
            return;
        }
        Log.d(this, "Speakerphone status bar icon being set to %b", new Object[]{Boolean.valueOf(z)});
        if (z) {
            this.mStatusBarManager.setIcon("speakerphone", android.R.drawable.stat_sys_speakerphone, 0, this.mContext.getString(R.string.accessibility_speakerphone_enabled));
        } else {
            this.mStatusBarManager.removeIcon("speakerphone");
        }
        this.mIsShowingSpeakerphone = z;
    }

    public boolean isStatusBarShowingMute() {
        return this.mIsShowingMute;
    }

    public boolean isStatusBarShowingSpeaker() {
        return this.mIsShowingSpeakerphone;
    }
}
