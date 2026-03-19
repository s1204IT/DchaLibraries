package com.android.browser;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.WebView;

public class NetworkStateHandler {
    Activity mActivity;
    Controller mController;
    private boolean mIsNetworkUp;
    private IntentFilter mNetworkStateChangedFilter;
    private BroadcastReceiver mNetworkStateIntentReceiver;

    public NetworkStateHandler(Activity activity, Controller controller) {
        this.mActivity = activity;
        this.mController = controller;
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) this.mActivity.getSystemService("connectivity")).getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            this.mIsNetworkUp = activeNetworkInfo.isAvailable();
        } else {
            Extensions.getNetworkStateHandlerPlugin(this.mActivity).promptUserToEnableData(this.mActivity);
        }
        this.mNetworkStateChangedFilter = new IntentFilter();
        this.mNetworkStateChangedFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mNetworkStateIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    String typeName = networkInfo.getTypeName();
                    String subtypeName = networkInfo.getSubtypeName();
                    NetworkStateHandler.this.sendNetworkType(typeName.toLowerCase(), subtypeName != null ? subtypeName.toLowerCase() : "");
                    BrowserSettings.getInstance().updateConnectionType();
                    NetworkStateHandler.this.onNetworkToggle(!intent.getBooleanExtra("noConnectivity", false));
                }
            }
        };
    }

    void onPause() {
        this.mActivity.unregisterReceiver(this.mNetworkStateIntentReceiver);
    }

    void onResume() {
        this.mActivity.registerReceiver(this.mNetworkStateIntentReceiver, this.mNetworkStateChangedFilter);
        BrowserSettings.getInstance().updateConnectionType();
    }

    void onNetworkToggle(boolean z) {
        if (z == this.mIsNetworkUp) {
            return;
        }
        this.mIsNetworkUp = z;
        WebView currentWebView = this.mController.getCurrentWebView();
        if (currentWebView != null) {
            currentWebView.setNetworkAvailable(z);
        }
    }

    boolean isNetworkUp() {
        return this.mIsNetworkUp;
    }

    private void sendNetworkType(String str, String str2) {
        this.mController.getCurrentWebView();
    }
}
