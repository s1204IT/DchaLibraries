package com.mediatek.settings.wfd;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.android.settings.ProgressCategory;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;
import java.util.ArrayList;
import java.util.Arrays;

public class WfdChangeResolution {
    public static final ArrayList<Integer> DEVICE_RESOLUTION_LIST = new ArrayList<>(Arrays.asList(2, 3));
    private Context mContext;
    private SwitchPreference mDevicePref;
    private DisplayManager mDisplayManager;
    private WifiP2pDevice mP2pDevice;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("@M_WfdChangeResolution", "receive action: " + action);
            if ("android.net.wifi.p2p.THIS_DEVICE_CHANGED".equals(action)) {
                WfdChangeResolution.this.mP2pDevice = (WifiP2pDevice) intent.getParcelableExtra("wifiP2pDevice");
                WfdChangeResolution.this.updateDeviceName();
            }
        }
    };

    public WfdChangeResolution(Context context) {
        this.mContext = context;
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
    }

    public void onCreateOptionMenu(Menu menu, WifiDisplayStatus wifiDisplayStatus) {
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_display_max_resolution", 0);
        Log.d("@M_WfdChangeResolution", "current resolution is " + i);
        if (DEVICE_RESOLUTION_LIST.contains(Integer.valueOf(i))) {
            menu.add(0, 2, 0, R.string.wfd_change_resolution_menu_title).setEnabled(wifiDisplayStatus.getFeatureState() == 3 && wifiDisplayStatus.getActiveDisplayState() != 1).setShowAsAction(0);
        }
    }

    public boolean onOptionMenuSelected(MenuItem menuItem, FragmentManager fragmentManager) {
        if (menuItem.getItemId() == 2) {
            new WfdChangeResolutionFragment().show(fragmentManager, "change resolution");
            return true;
        }
        return false;
    }

    public boolean addAdditionalPreference(PreferenceScreen preferenceScreen, boolean z) {
        if (!z || !FeatureOption.MTK_WFD_SINK_SUPPORT) {
            return false;
        }
        if (this.mDevicePref == null) {
            this.mDevicePref = new SwitchPreference(this.mContext);
            if (this.mContext.getResources().getBoolean(android.R.^attr-private.popupPromptView)) {
                this.mDevicePref.setIcon(R.drawable.ic_wfd_cellphone);
            } else {
                this.mDevicePref.setIcon(R.drawable.ic_wfd_laptop);
            }
            this.mDevicePref.setPersistent(false);
            this.mDevicePref.setSummary(R.string.wfd_sink_summary);
            this.mDevicePref.setOrder(2);
            Intent intent = new Intent("mediatek.settings.WFD_SINK_SETTINGS");
            intent.setFlags(268435456);
            this.mDevicePref.setIntent(intent);
        }
        preferenceScreen.addPreference(this.mDevicePref);
        updateDeviceName();
        ProgressCategory progressCategory = new ProgressCategory(this.mContext, null, 0);
        progressCategory.setEmptyTextRes(R.string.wifi_display_no_devices_found);
        progressCategory.setOrder(3);
        progressCategory.setTitle(R.string.wfd_device_category);
        preferenceScreen.addPreference(progressCategory);
        return true;
    }

    public void onStart() {
        Log.d("@M_WfdChangeResolution", "onStart");
        if (FeatureOption.MTK_WFD_SINK_SUPPORT) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
            this.mContext.registerReceiver(this.mReceiver, intentFilter);
        }
    }

    public void onStop() {
        Log.d("@M_WfdChangeResolution", "onStop");
        if (FeatureOption.MTK_WFD_SINK_SUPPORT) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
    }

    public void updateDeviceName() {
        if (this.mP2pDevice != null && this.mDevicePref != null) {
            if (TextUtils.isEmpty(this.mP2pDevice.deviceName)) {
                this.mDevicePref.setTitle(this.mP2pDevice.deviceAddress);
            } else {
                this.mDevicePref.setTitle(this.mP2pDevice.deviceName);
            }
        }
    }

    public void handleWfdStatusChanged(WifiDisplayStatus wifiDisplayStatus) {
        if (!FeatureOption.MTK_WFD_SINK_SUPPORT) {
            return;
        }
        boolean z = wifiDisplayStatus != null && wifiDisplayStatus.getFeatureState() == 3;
        Log.d("@M_WfdChangeResolution", "handleWfdStatusChanged bStateOn: " + z);
        if (z) {
            int activeDisplayState = wifiDisplayStatus.getActiveDisplayState();
            Log.d("@M_WfdChangeResolution", "handleWfdStatusChanged wfdState: " + activeDisplayState);
            handleWfdStateChanged(activeDisplayState, isSinkMode());
            return;
        }
        handleWfdStateChanged(0, isSinkMode());
    }

    private void handleWfdStateChanged(int i, boolean z) {
        switch (i) {
            case 0:
                if (!z) {
                    if (this.mDevicePref != null) {
                        this.mDevicePref.setEnabled(true);
                        this.mDevicePref.setChecked(false);
                    }
                    if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                        Intent intent = new Intent();
                        intent.setClassName("com.mediatek.floatmenu", "com.mediatek.floatmenu.FloatMenuService");
                        this.mContext.stopServiceAsUser(intent, UserHandle.CURRENT);
                    }
                }
                break;
            case 1:
                if (!z && this.mDevicePref != null) {
                    this.mDevicePref.setEnabled(false);
                    break;
                }
                break;
            case 2:
                if (!z && this.mDevicePref != null) {
                    this.mDevicePref.setEnabled(false);
                    break;
                }
                break;
        }
    }

    public void prepareWfdConnect() {
        if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
            Intent intent = new Intent();
            intent.setClassName("com.mediatek.floatmenu", "com.mediatek.floatmenu.FloatMenuService");
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        }
    }

    private boolean isSinkMode() {
        return this.mDisplayManager.isSinkEnabled();
    }
}
