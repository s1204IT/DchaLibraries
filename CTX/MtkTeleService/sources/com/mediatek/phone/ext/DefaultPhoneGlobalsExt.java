package com.mediatek.phone.ext;

import android.content.Context;
import android.util.Log;

public class DefaultPhoneGlobalsExt implements IPhoneGlobalsExt {
    @Override
    public void handlePrimarySimUpdate(Context context, int i) {
        Log.d("DefaultPhoneGlobalsExt", "Default Called");
    }
}
