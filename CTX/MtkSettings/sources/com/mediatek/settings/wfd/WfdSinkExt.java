package com.mediatek.settings.wfd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;

public class WfdSinkExt {
    private Context mContext;
    private DisplayManager mDisplayManager;
    private WfdSinkSurfaceFragment mSinkFragment;
    private Toast mSinkToast;
    private int mPreWfdState = -1;
    private boolean mUiPortrait = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("@M_WfdSinkExt", "receive action: " + action);
            if ("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED".equals(action)) {
                WfdSinkExt.this.handleWfdStatusChanged(WfdSinkExt.this.mDisplayManager.getWifiDisplayStatus());
            } else if ("com.mediatek.wfd.portrait".equals(action)) {
                WfdSinkExt.this.mUiPortrait = true;
            }
        }
    };

    public WfdSinkExt() {
    }

    public WfdSinkExt(Context context) {
        this.mContext = context;
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
    }

    public void onStart() {
        Log.d("@M_WfdSinkExt", "onStart");
        if (FeatureOption.MTK_WFD_SINK_SUPPORT) {
            handleWfdStatusChanged(this.mDisplayManager.getWifiDisplayStatus());
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
            intentFilter.addAction("com.mediatek.wfd.portrait");
            this.mContext.registerReceiver(this.mReceiver, intentFilter);
        }
    }

    public void onStop() {
        Log.d("@M_WfdSinkExt", "onStop");
        if (FeatureOption.MTK_WFD_SINK_SUPPORT) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
    }

    public void setupWfdSinkConnection(Surface surface) {
        Log.d("@M_WfdSinkExt", "setupWfdSinkConnection");
        setWfdMode(true);
        waitWfdSinkConnection(surface);
    }

    public void disconnectWfdSinkConnection() {
        Log.d("@M_WfdSinkExt", "disconnectWfdSinkConnection");
        this.mDisplayManager.disconnectWifiDisplay();
        setWfdMode(false);
        Log.d("@M_WfdSinkExt", "after disconnectWfdSinkConnection");
    }

    public void registerSinkFragment(WfdSinkSurfaceFragment wfdSinkSurfaceFragment) {
        this.mSinkFragment = wfdSinkSurfaceFragment;
    }

    public void handleWfdStatusChanged(WifiDisplayStatus wifiDisplayStatus) {
        boolean z = wifiDisplayStatus != null && wifiDisplayStatus.getFeatureState() == 3;
        Log.d("@M_WfdSinkExt", "handleWfdStatusChanged bStateOn: " + z);
        if (z) {
            int activeDisplayState = wifiDisplayStatus.getActiveDisplayState();
            Log.d("@M_WfdSinkExt", "handleWfdStatusChanged wfdState: " + activeDisplayState);
            handleWfdStateChanged(activeDisplayState, isSinkMode());
            this.mPreWfdState = activeDisplayState;
            return;
        }
        handleWfdStateChanged(0, isSinkMode());
        this.mPreWfdState = -1;
    }

    private void handleWfdStateChanged(int i, boolean z) {
        switch (i) {
            case 0:
                if (z) {
                    Log.d("@M_WfdSinkExt", "dismiss fragment");
                    if (this.mSinkFragment != null) {
                        this.mSinkFragment.dismissAllowingStateLoss();
                    }
                    setWfdMode(false);
                }
                if (this.mPreWfdState == 2) {
                    showToast(false);
                }
                this.mUiPortrait = false;
                break;
            case 2:
                if (z) {
                    Log.d("@M_WfdSinkExt", "mUiPortrait: " + this.mUiPortrait);
                    this.mSinkFragment.requestOrientation(this.mUiPortrait);
                    SharedPreferences sharedPreferences = this.mContext.getSharedPreferences("wifi_display", 0);
                    if (sharedPreferences.getBoolean("wifi_display_hide_guide", true) && this.mSinkFragment != null) {
                        this.mSinkFragment.addWfdSinkGuide();
                        sharedPreferences.edit().putBoolean("wifi_display_hide_guide", false).commit();
                    }
                    if (this.mPreWfdState != 2) {
                        showToast(true);
                    }
                }
                this.mUiPortrait = false;
                break;
        }
    }

    private void showToast(boolean z) {
        if (this.mSinkToast != null) {
            this.mSinkToast.cancel();
        }
        this.mSinkToast = Toast.makeText(this.mContext, z ? R.string.wfd_sink_toast_enjoy : R.string.wfd_sink_toast_disconnect, z ? 1 : 0);
        this.mSinkToast.show();
    }

    private boolean isSinkMode() {
        return this.mDisplayManager.isSinkEnabled();
    }

    private void setWfdMode(boolean z) {
        Log.d("@M_WfdSinkExt", "setWfdMode " + z);
        this.mDisplayManager.enableSink(z);
    }

    private void waitWfdSinkConnection(Surface surface) {
        this.mDisplayManager.waitWifiDisplayConnection(surface);
    }

    public void sendUibcEvent(String str) {
        this.mDisplayManager.sendUibcInputEvent(str);
    }
}
