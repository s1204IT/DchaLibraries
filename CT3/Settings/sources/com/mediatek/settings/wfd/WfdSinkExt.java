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
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

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
            } else {
                if (!"com.mediatek.wfd.portrait".equals(action)) {
                    return;
                }
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
        if (!FeatureOption.MTK_WFD_SINK_SUPPORT) {
            return;
        }
        WifiDisplayStatus wfdStatus = this.mDisplayManager.getWifiDisplayStatus();
        handleWfdStatusChanged(wfdStatus);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
        filter.addAction("com.mediatek.wfd.portrait");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    public void onStop() {
        Log.d("@M_WfdSinkExt", "onStop");
        if (!FeatureOption.MTK_WFD_SINK_SUPPORT) {
            return;
        }
        this.mContext.unregisterReceiver(this.mReceiver);
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

    public void registerSinkFragment(WfdSinkSurfaceFragment fragment) {
        this.mSinkFragment = fragment;
    }

    public void handleWfdStatusChanged(WifiDisplayStatus status) {
        boolean bStateOn = status != null && status.getFeatureState() == 3;
        Log.d("@M_WfdSinkExt", "handleWfdStatusChanged bStateOn: " + bStateOn);
        if (bStateOn) {
            int wfdState = status.getActiveDisplayState();
            Log.d("@M_WfdSinkExt", "handleWfdStatusChanged wfdState: " + wfdState);
            handleWfdStateChanged(wfdState, isSinkMode());
            this.mPreWfdState = wfdState;
            return;
        }
        handleWfdStateChanged(0, isSinkMode());
        this.mPreWfdState = -1;
    }

    private void handleWfdStateChanged(int wfdState, boolean sinkMode) {
        switch (wfdState) {
            case DefaultWfcSettingsExt.RESUME:
                if (sinkMode) {
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
            case DefaultWfcSettingsExt.CREATE:
                if (sinkMode) {
                    Log.d("@M_WfdSinkExt", "mUiPortrait: " + this.mUiPortrait);
                    this.mSinkFragment.requestOrientation(this.mUiPortrait);
                    SharedPreferences preferences = this.mContext.getSharedPreferences("wifi_display", 0);
                    boolean showGuide = preferences.getBoolean("wifi_display_hide_guide", true);
                    if (showGuide && this.mSinkFragment != null) {
                        this.mSinkFragment.addWfdSinkGuide();
                        preferences.edit().putBoolean("wifi_display_hide_guide", false).commit();
                    }
                    if (this.mPreWfdState != 2) {
                        showToast(true);
                    }
                }
                this.mUiPortrait = false;
                break;
        }
    }

    private void showToast(boolean connected) {
        if (this.mSinkToast != null) {
            this.mSinkToast.cancel();
        }
        this.mSinkToast = Toast.makeText(this.mContext, connected ? R.string.wfd_sink_toast_enjoy : R.string.wfd_sink_toast_disconnect, connected ? 1 : 0);
        this.mSinkToast.show();
    }

    private boolean isSinkMode() {
        return this.mDisplayManager.isSinkEnabled();
    }

    private void setWfdMode(boolean sink) {
        Log.d("@M_WfdSinkExt", "setWfdMode " + sink);
        this.mDisplayManager.enableSink(sink);
    }

    private void waitWfdSinkConnection(Surface surface) {
        this.mDisplayManager.waitWifiDisplayConnection(surface);
    }

    public void sendUibcEvent(String eventDesc) {
        this.mDisplayManager.sendUibcInputEvent(eventDesc);
    }
}
