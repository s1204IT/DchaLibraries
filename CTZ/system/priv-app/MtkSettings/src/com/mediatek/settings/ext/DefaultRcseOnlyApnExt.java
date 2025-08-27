package com.mediatek.settings.ext;

import com.mediatek.settings.ext.IRcseOnlyApnExt;

/* loaded from: classes.dex */
public class DefaultRcseOnlyApnExt implements IRcseOnlyApnExt {
    @Override // com.mediatek.settings.ext.IRcseOnlyApnExt
    public boolean isRcseOnlyApnEnabled(String str) {
        return true;
    }

    @Override // com.mediatek.settings.ext.IRcseOnlyApnExt
    public void onCreate(IRcseOnlyApnExt.OnRcseOnlyApnStateChangedListener onRcseOnlyApnStateChangedListener, int i) {
    }

    @Override // com.mediatek.settings.ext.IRcseOnlyApnExt
    public void onDestory() {
    }
}
