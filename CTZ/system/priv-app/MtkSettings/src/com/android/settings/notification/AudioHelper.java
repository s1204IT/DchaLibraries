package com.android.settings.notification;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settings.Utils;

/* loaded from: classes.dex */
public class AudioHelper {
    private AudioManager mAudioManager;
    private Context mContext;

    public AudioHelper(Context context) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
    }

    public boolean isSingleVolume() {
        return AudioSystem.isSingleVolume(this.mContext);
    }

    public int getManagedProfileId(UserManager userManager) {
        return Utils.getManagedProfileId(userManager, UserHandle.myUserId());
    }

    public boolean isUserUnlocked(UserManager userManager, int i) {
        return userManager.isUserUnlocked(i);
    }

    public Context createPackageContextAsUser(int i) {
        return Utils.createPackageContextAsUser(this.mContext, i);
    }

    public int getRingerModeInternal() {
        return this.mAudioManager.getRingerModeInternal();
    }

    public int getStreamVolume(int i) {
        return this.mAudioManager.getStreamVolume(i);
    }

    public boolean setStreamVolume(int i, int i2) {
        this.mAudioManager.setStreamVolume(i, i2, 0);
        return true;
    }

    public int getMaxVolume(int i) {
        return this.mAudioManager.getStreamMaxVolume(i);
    }
}
