package com.mediatek.server.telecom.ext;

import android.os.Bundle;

public interface IGttEventExt {
    int getGttProperties();

    boolean handleGttEvent(String str, Bundle bundle, Object obj, Object obj2);
}
