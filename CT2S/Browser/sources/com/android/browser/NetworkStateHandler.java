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
        ConnectivityManager cm = (ConnectivityManager) this.mActivity.getSystemService("connectivity");
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null) {
            this.mIsNetworkUp = info.isAvailable();
        }
        this.mNetworkStateChangedFilter = new IntentFilter();
        this.mNetworkStateChangedFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mNetworkStateIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    NetworkInfo info2 = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    String typeName = info2.getTypeName();
                    String subtypeName = info2.getSubtypeName();
                    NetworkStateHandler.this.sendNetworkType(typeName.toLowerCase(), subtypeName != null ? subtypeName.toLowerCase() : "");
                    BrowserSettings.getInstance().updateConnectionType();
                    boolean noConnection = intent.getBooleanExtra("noConnectivity", false);
                    NetworkStateHandler.this.onNetworkToggle(!noConnection);
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

    void onNetworkToggle(boolean up) {
        if (up != this.mIsNetworkUp) {
            this.mIsNetworkUp = up;
            WebView w = this.mController.getCurrentWebView();
            if (w != null) {
                w.setNetworkAvailable(up);
            }
        }
    }

    boolean isNetworkUp() {
        return this.mIsNetworkUp;
    }

    private void sendNetworkType(String type, String subtype) {
        this.mController.getCurrentWebView();
    }
}
