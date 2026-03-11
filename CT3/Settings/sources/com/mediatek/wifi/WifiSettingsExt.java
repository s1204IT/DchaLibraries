package com.mediatek.wifi;

import android.content.ContentResolver;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.wifi.AccessPoint;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiSettingsExt;

public class WifiSettingsExt {
    private Context mActivity;
    private IWifiSettingsExt mExt;

    public WifiSettingsExt(Context context) {
        this.mActivity = context;
    }

    public void onCreate() {
        this.mExt = UtilsExt.getWifiSettingsPlugin(this.mActivity);
    }

    public void onActivityCreated(SettingsPreferenceFragment fragment, WifiManager wifiManager) {
        this.mExt.registerPriorityObserver(this.mActivity.getContentResolver());
        this.mExt.addCategories(fragment.getPreferenceScreen());
    }

    public void updatePriority() {
        Log.d("WifiSettingsExt", "mConnectListener or mSaveListener");
        this.mExt.updatePriority();
    }

    public void onResume() {
        this.mExt.updatePriority();
    }

    public void onCreateContextMenu(ContextMenu menu, NetworkInfo.DetailedState state, AccessPoint accessPoint) {
        this.mExt.updateContextMenu(menu, 101, state);
    }

    public boolean onContextItemSelected(MenuItem item, WifiConfiguration wifiConfig) {
        switch (item.getItemId()) {
            case 101:
                this.mExt.disconnect(wifiConfig);
                return true;
            default:
                return false;
        }
    }

    public void recordPriority(WifiConfiguration config) {
        if (config != null) {
            this.mExt.recordPriority(config.priority);
        } else {
            this.mExt.recordPriority(-1);
        }
    }

    public void submit(WifiConfiguration config, AccessPoint accessPoint, NetworkInfo.DetailedState state) {
        Log.d("WifiSettingsExt", "submit, config = " + config);
        if (config != null) {
            if (config.networkId != -1 && accessPoint != null) {
                Log.d("WifiSettingsExt", "submit, setNewPriority");
                this.mExt.setNewPriority(config);
            } else {
                Log.d("WifiSettingsExt", "submit, updatePriorityAfterSubmit");
                this.mExt.updatePriorityAfterSubmit(config);
            }
        }
        Log.d("WifiSettingsExt", "submit, setLastConnectedConfig");
        this.mExt.setLastConnectedConfig(config);
    }

    public void unregisterPriorityObserver(ContentResolver cr) {
        this.mExt.unregisterPriorityObserver(cr);
    }

    public void addPreference(PreferenceScreen screen, Preference preference, boolean isConfiged) {
        this.mExt.addPreference(screen, preference, isConfiged);
    }

    public void emptyCategory(PreferenceScreen screen) {
        this.mExt.emptyCategory(screen);
    }

    public void emptyScreen(PreferenceScreen screen) {
        this.mExt.emptyScreen(screen);
    }

    public void refreshCategory(PreferenceScreen screen) {
        this.mExt.refreshCategory(screen);
    }
}
