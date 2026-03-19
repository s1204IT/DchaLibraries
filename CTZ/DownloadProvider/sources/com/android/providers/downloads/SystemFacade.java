package com.android.providers.downloads;

import android.app.job.JobParameters;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLContext;

interface SystemFacade {
    long currentTimeMillis();

    long getMaxBytesOverMobile();

    Network getNetwork(JobParameters jobParameters);

    NetworkCapabilities getNetworkCapabilities(Network network);

    NetworkInfo getNetworkInfo(Network network, int i, boolean z);

    long getRecommendedMaxBytesOverMobile();

    SSLContext getSSLContextForPackage(Context context, String str) throws GeneralSecurityException;

    boolean isCleartextTrafficPermitted(String str, String str2);

    void sendBroadcast(Intent intent);

    boolean userOwnsPackage(int i, String str) throws PackageManager.NameNotFoundException;
}
