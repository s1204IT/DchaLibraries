package com.mediatek.phone.ext;

import android.content.Context;

public interface ISsRoamingServiceExt {
    boolean isNotificationForRoamingAllowed(Context context);

    void registerSsRoamingReceiver(Context context);
}
