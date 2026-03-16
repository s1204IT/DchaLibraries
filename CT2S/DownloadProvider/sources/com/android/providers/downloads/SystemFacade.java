package com.android.providers.downloads;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;

interface SystemFacade {
    long currentTimeMillis();

    NetworkInfo getActiveNetworkInfo(int i);

    Long getMaxBytesOverMobile();

    Long getRecommendedMaxBytesOverMobile();

    boolean isActiveNetworkMetered();

    boolean isNetworkRoaming();

    void sendBroadcast(Intent intent);

    boolean userOwnsPackage(int i, String str) throws PackageManager.NameNotFoundException;
}
