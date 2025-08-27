package com.mediatek.keyguard.ext;

import android.content.Context;
import com.mediatek.keyguard.ext.IOperatorSIMString;

/* loaded from: classes.dex */
public class DefaultOperatorSIMString implements IOperatorSIMString {
    @Override // com.mediatek.keyguard.ext.IOperatorSIMString
    public String getOperatorSIMString(String str, int i, IOperatorSIMString.SIMChangedTag sIMChangedTag, Context context) {
        return str;
    }
}
