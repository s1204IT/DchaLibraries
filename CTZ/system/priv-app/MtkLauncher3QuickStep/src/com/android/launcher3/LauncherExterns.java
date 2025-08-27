package com.android.launcher3;

import android.content.SharedPreferences;
import com.android.launcher3.Launcher;

/* loaded from: classes.dex */
public interface LauncherExterns {
    SharedPreferences getSharedPrefs();

    boolean setLauncherCallbacks(LauncherCallbacks launcherCallbacks);

    void setLauncherOverlay(Launcher.LauncherOverlay launcherOverlay);
}
