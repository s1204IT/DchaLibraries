package com.mediatek.settings.ext;

import com.mediatek.settings.ext.IRcseOnlyApnExt;

public class DefaultRcseOnlyApnExt implements IRcseOnlyApnExt {
    @Override
    public boolean isRcseOnlyApnEnabled(String str) {
        return true;
    }

    @Override
    public void onCreate(IRcseOnlyApnExt.OnRcseOnlyApnStateChangedListener onRcseOnlyApnStateChangedListener, int i) {
    }

    @Override
    public void onDestory() {
    }
}
