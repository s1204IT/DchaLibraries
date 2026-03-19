package com.mediatek.phone.ext;

import android.content.Context;

public class DefaultSsRoamingServiceExt implements ISsRoamingServiceExt {
    @Override
    public void registerSsRoamingReceiver(Context context) {
    }

    @Override
    public boolean isNotificationForRoamingAllowed(Context context) {
        return true;
    }
}
