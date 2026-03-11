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
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.Arrays;

public class WfdSettingsExt {
    public static final ArrayList<Integer> DEVICE_RESOLUTION_LIST = new ArrayList<>(Arrays.asList(2, 3));
    private Context mContext;
    private SwitchPreference mDevicePref;
    private DisplayManager mDisplayManager;
    private WifiP2pDevice mP2pDevice;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("@M_WfdSettingsExt", "receive action: " + action);
            if (!"android.net.wifi.p2p.THIS_DEVICE_CHANGED".equals(action)) {
                return;
            }
            WfdSettingsExt.this.mP2pDevice = (WifiP2pDevice) intent.getParcelableExtra("wifiP2pDevice");
            WfdSettingsExt.this.updateDeviceName();
        }
    };

    public WfdSettingsExt(Context context) {
        this.mContext = context;
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
    }

    public void onCreateOptionMenu(Menu menu, WifiDisplayStatus status) {
        boolean z = true;
        int currentResolution = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_display_max_resolution", 0);
        Log.d("@M_WfdSettingsExt", "current resolution is " + currentResolution);
        if (!DEVICE_RESOLUTION_LIST.contains(Integer.valueOf(currentResolution))) {
            return;
        }
        MenuItem menuItemAdd = menu.add(0, 2, 0, R.string.wfd_change_resolution_menu_title);
        if (status.getFeatureState() != 3 || status.getActiveDisplayState() == 1) {
            z = false;
        }
        menuItemAdd.setEnabled(z).setShowAsAction(0);
    }

    public boolean onOptionMenuSelected(MenuItem item, FragmentManager fragmentManager) {
        if (item.getItemId() == 2) {
            new WfdChangeResolutionFragment().show(fragmentManager, "change resolution");
            return true;
        }
        return false;
    }

    public boolean addAdditionalPreference(PreferenceScreen preferenceScreen, boolean available) {
        if (!available || !FeatureOption.MTK_WFD_SINK_SUPPORT) {
            return false;
        }
        if (this.mDevicePref == null) {
            this.mDevicePref = new SwitchPreference(this.mContext);
            if (this.mContext.getResources().getBoolean(android.R.^attr-private.frameDuration)) {
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
        ProgressCategory cat = new ProgressCategory(this.mContext, null, 0);
        cat.setEmptyTextRes(R.string.wifi_display_no_devices_found);
        cat.setOrder(3);
        cat.setTitle(R.string.wfd_device_category);
        preferenceScreen.addPreference(cat);
        return true;
    }

    public void onStart() {
        Log.d("@M_WfdSettingsExt", "onStart");
        if (!FeatureOption.MTK_WFD_SINK_SUPPORT) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    public void onStop() {
        Log.d("@M_WfdSettingsExt", "onStop");
        if (!FeatureOption.MTK_WFD_SINK_SUPPORT) {
            return;
        }
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    public void updateDeviceName() {
        if (this.mP2pDevice == null || this.mDevicePref == null) {
            return;
        }
        if (TextUtils.isEmpty(this.mP2pDevice.deviceName)) {
            this.mDevicePref.setTitle(this.mP2pDevice.deviceAddress);
        } else {
            this.mDevicePref.setTitle(this.mP2pDevice.deviceName);
        }
    }

    public void handleWfdStatusChanged(WifiDisplayStatus status) {
        if (!FeatureOption.MTK_WFD_SINK_SUPPORT) {
            return;
        }
        boolean bStateOn = status != null && status.getFeatureState() == 3;
        Log.d("@M_WfdSettingsExt", "handleWfdStatusChanged bStateOn: " + bStateOn);
        if (bStateOn) {
            int wfdState = status.getActiveDisplayState();
            Log.d("@M_WfdSettingsExt", "handleWfdStatusChanged wfdState: " + wfdState);
            handleWfdStateChanged(wfdState, isSinkMode());
            return;
        }
        handleWfdStateChanged(0, isSinkMode());
    }

    private void handleWfdStateChanged(int wfdState, boolean sinkMode) {
        switch (wfdState) {
            case DefaultWfcSettingsExt.RESUME:
                if (!sinkMode) {
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
            case DefaultWfcSettingsExt.PAUSE:
                if (!sinkMode && this.mDevicePref != null) {
                    this.mDevicePref.setEnabled(false);
                    break;
                }
                break;
            case DefaultWfcSettingsExt.CREATE:
                if (!sinkMode && this.mDevicePref != null) {
                    this.mDevicePref.setEnabled(false);
                    break;
                }
                break;
        }
    }

    public void prepareWfdConnect() {
        if (!FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
            return;
        }
        Intent intent = new Intent();
        intent.setClassName("com.mediatek.floatmenu", "com.mediatek.floatmenu.FloatMenuService");
        this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
    }

    private boolean isSinkMode() {
        return this.mDisplayManager.isSinkEnabled();
    }
}
