package com.mediatek.phone.ext;

import android.util.Log;

public class DefaultTtyModeListPreferenceExt implements ITtyModeListPreferenceExt {
    @Override
    public void handleWfcUpdateAndShowMessage(int i) {
        Log.d("DefaultTtyModeListPreferenceExt", "handleWfcUpdateAndShowMessage");
    }
}
