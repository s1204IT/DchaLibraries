package com.mediatek.duraspeed.manager;

import android.content.Context;
import android.content.Intent;

public interface IDuraSpeedNative {
    boolean isDuraSpeedEnabled();

    void onActivityIdle(Context context, Intent intent);

    void onBeforeActivitySwitch(String str, String str2, boolean z, int i);

    void onSystemReady();

    void onWakefulnessChanged(int i);

    void startDuraSpeedService(Context context);

    void triggerMemory(int i);
}
