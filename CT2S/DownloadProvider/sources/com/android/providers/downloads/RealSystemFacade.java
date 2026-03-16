package com.android.providers.downloads;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

class RealSystemFacade implements SystemFacade {
    private Context mContext;

    public RealSystemFacade(Context context) {
        this.mContext = context;
    }

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public NetworkInfo getActiveNetworkInfo(int uid) {
        ConnectivityManager connectivity = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (connectivity == null) {
            Log.w("DownloadManager", "couldn't get connectivity manager");
            return null;
        }
        NetworkInfo activeInfo = connectivity.getActiveNetworkInfoForUid(uid);
        if (activeInfo == null && Constants.LOGVV) {
            Log.v("DownloadManager", "network is not available");
            return activeInfo;
        }
        return activeInfo;
    }

    @Override
    public boolean isActiveNetworkMetered() {
        ConnectivityManager conn = ConnectivityManager.from(this.mContext);
        return conn.isActiveNetworkMetered();
    }

    @Override
    public boolean isNetworkRoaming() {
        ConnectivityManager connectivity = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (connectivity == null) {
            Log.w("DownloadManager", "couldn't get connectivity manager");
            return false;
        }
        NetworkInfo info = connectivity.getActiveNetworkInfo();
        boolean isMobile = info != null && info.getType() == 0;
        boolean isRoaming = isMobile && TelephonyManager.getDefault().isNetworkRoaming();
        if (Constants.LOGVV && isRoaming) {
            Log.v("DownloadManager", "network is roaming");
        }
        return isRoaming;
    }

    @Override
    public Long getMaxBytesOverMobile() {
        return DownloadManager.getMaxBytesOverMobile(this.mContext);
    }

    @Override
    public Long getRecommendedMaxBytesOverMobile() {
        return DownloadManager.getRecommendedMaxBytesOverMobile(this.mContext);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        this.mContext.sendBroadcast(intent);
    }

    @Override
    public boolean userOwnsPackage(int uid, String packageName) throws PackageManager.NameNotFoundException {
        return this.mContext.getPackageManager().getApplicationInfo(packageName, 0).uid == uid;
    }
}
