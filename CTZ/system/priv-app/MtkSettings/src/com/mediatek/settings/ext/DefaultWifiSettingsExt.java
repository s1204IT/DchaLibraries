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

/* loaded from: classes.dex */
public class DefaultWifiSettingsExt implements IWifiSettingsExt {
    private static final String TAG = "DefaultWifiSettingsExt";

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void registerPriorityObserver(ContentResolver contentResolver) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void unregisterPriorityObserver(ContentResolver contentResolver) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void setLastConnectedConfig(WifiConfiguration wifiConfiguration) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void updatePriority() {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void updateContextMenu(ContextMenu contextMenu, int i, NetworkInfo.DetailedState detailedState) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void emptyCategory(PreferenceScreen preferenceScreen) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void emptyScreen(PreferenceScreen preferenceScreen) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void refreshCategory(PreferenceScreen preferenceScreen) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void recordPriority(WifiConfiguration wifiConfiguration) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void setNewPriority(WifiConfiguration wifiConfiguration) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void updatePriorityAfterSubmit(WifiConfiguration wifiConfiguration) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public boolean disconnect(MenuItem menuItem, WifiConfiguration wifiConfiguration) {
        return false;
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void addPreference(PreferenceScreen preferenceScreen, PreferenceCategory preferenceCategory, Preference preference, boolean z) {
        if (preferenceCategory != null) {
            preferenceCategory.addPreference(preference);
        }
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void init(PreferenceScreen preferenceScreen) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public boolean removeConnectedAccessPointPreference() {
        return false;
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void emptyConneCategory(PreferenceScreen preferenceScreen) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void submit(WifiConfiguration wifiConfiguration, AccessPoint accessPoint, NetworkInfo.DetailedState detailedState) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void addRefreshPreference(PreferenceScreen preferenceScreen, Object obj, boolean z) {
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public boolean customRefreshButtonClick(Preference preference) {
        return false;
    }

    @Override // com.mediatek.settings.ext.IWifiSettingsExt
    public void customRefreshButtonStatus(boolean z) {
    }
}
