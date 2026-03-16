package com.android.ex.chips;

import android.os.Build;

public class ChipsUtil {
    public static boolean supportsChipsUi() {
        return Build.VERSION.SDK_INT >= 14;
    }
}
