package com.mediatek.keyguard.ext;

import android.content.Context;
import com.mediatek.keyguard.ext.IOperatorSIMString;

public class DefaultOperatorSIMString implements IOperatorSIMString {
    @Override
    public String getOperatorSIMString(String str, int i, IOperatorSIMString.SIMChangedTag sIMChangedTag, Context context) {
        return str;
    }
}
