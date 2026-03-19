package com.mediatek.server.telecom.ext;

import android.os.Bundle;

public class DefaultRttEventExt implements IRttEventExt {
    @Override
    public boolean handleRttEvent(String str, Bundle bundle, Object obj, Object obj2) {
        return false;
    }

    @Override
    public int getRttProperties() {
        return 0;
    }
}
