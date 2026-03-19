package com.android.bluetooth.mapclient;

import android.support.annotation.VisibleForTesting;

class MapUtils {
    private static MnsService sMnsService = null;

    MapUtils() {
    }

    @VisibleForTesting
    static void setMnsService(MnsService mnsService) {
        sMnsService = mnsService;
    }

    static MnsService newMnsServiceInstance(MapClientService mapClientService) {
        return sMnsService == null ? new MnsService(mapClientService) : sMnsService;
    }
}
