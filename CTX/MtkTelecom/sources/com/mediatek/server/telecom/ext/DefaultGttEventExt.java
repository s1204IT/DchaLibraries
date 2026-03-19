package com.mediatek.server.telecom.ext;

import android.os.Bundle;

public class DefaultGttEventExt implements IGttEventExt {
    @Override
    public boolean handleGttEvent(String str, Bundle bundle, Object obj, Object obj2) {
        return false;
    }

    @Override
    public int getGttProperties() {
        return 0;
    }
}
