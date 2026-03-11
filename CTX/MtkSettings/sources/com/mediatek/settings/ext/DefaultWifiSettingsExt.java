package com.mediatek.settings.ext;

import android.content.ContentResolver;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.view.ContextMenu;
import android.view.MenuItem;
import com.android.settingslib.wifi.AccessPoint;

public class DefaultWifiSettingsExt implements IWifiSettingsExt {
    private static final String TAG = "DefaultWifiSettingsExt";

    @Override
    public void registerPriorityObserver(ContentResolver contentResolver) {
    }

    @Override
    public void unregisterPriorityObserver(ContentResolver contentResolver) {
    }

    @Override
    public void setLastConnectedConfig(WifiConfiguration wifiConfiguration) {
    }

    @Override
    public void updatePriority() {
    }

    @Override
    public void updateContextMenu(ContextMenu contextMenu, int i, NetworkInfo.DetailedState detailedState) {
    }

    @Override
    public void emptyCategory(PreferenceScreen preferenceScreen) {
    }

    @Override
    public void emptyScreen(PreferenceScreen preferenceScreen) {
    }

    @Override
    public void refreshCategory(PreferenceScreen preferenceScreen) {
    }

    @Override
    public void recordPriority(WifiConfiguration wifiConfiguration) {
    }

    @Override
    public void setNewPriority(WifiConfiguration wifiConfiguration) {
    }

    @Override
    public void updatePriorityAfterSubmit(WifiConfiguration wifiConfiguration) {
    }

    @Override
    public boolean disconnect(MenuItem menuItem, WifiConfiguration wifiConfiguration) {
        return false;
    }

    @Override
    public void addPreference(PreferenceScreen preferenceScreen, PreferenceCategory preferenceCategory, Preference preference, boolean z) {
        if (preferenceCategory != null) {
            preferenceCategory.addPreference(preference);
        }
    }

    @Override
    public void init(PreferenceScreen preferenceScreen) {
    }

    @Override
    public boolean removeConnectedAccessPointPreference() {
        return false;
    }

    @Override
    public void emptyConneCategory(PreferenceScreen preferenceScreen) {
    }

    @Override
    public void submit(WifiConfiguration wifiConfiguration, AccessPoint accessPoint, NetworkInfo.DetailedState detailedState) {
    }

    @Override
    public void addRefreshPreference(PreferenceScreen preferenceScreen, Object obj, boolean z) {
    }

    @Override
    public boolean customRefreshButtonClick(Preference preference) {
        return false;
    }

    @Override
    public void customRefreshButtonStatus(boolean z) {
    }
}
