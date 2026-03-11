package com.mediatek.keyguard.ext;

import android.content.Context;

public interface IOperatorSIMString {
    String getOperatorSIMString(String str, int i, SIMChangedTag sIMChangedTag, Context context);

    public enum SIMChangedTag {
        SIMTOUIM,
        UIMSIM,
        DELSIM;

        public static SIMChangedTag[] valuesCustom() {
            return values();
        }
    }
}
