package com.mediatek.phone.ext;

import android.util.Log;

public class DefaultEmergencyDialerExt implements IEmergencyDialerExt {
    @Override
    public String getDialogText(Object obj, int i, long j) {
        Log.d("DefaultEmergencyDialerExt", "getDialogText");
        return null;
    }
}
