package com.mediatek.server.telecom.ext;

import android.content.Context;

public interface IRttUtilExt {
    void makeRttAudioController(Context context, Object obj);

    void onCallStateChanged(Object obj, int i, int i2);

    void onForegroundCallChanged(Object obj, Object obj2);
}
