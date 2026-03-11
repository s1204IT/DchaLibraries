package com.mediatek.settings.ext;

import com.mediatek.settings.ext.IRcseOnlyApnExt;

public class DefaultRcseOnlyApnExt implements IRcseOnlyApnExt {
    @Override
    public boolean isRcseOnlyApnEnabled(String type) {
        return true;
    }

    @Override
    public void onCreate(IRcseOnlyApnExt.OnRcseOnlyApnStateChangedListener listener, int subId) {
    }

    @Override
    public void onDestory() {
    }
}
