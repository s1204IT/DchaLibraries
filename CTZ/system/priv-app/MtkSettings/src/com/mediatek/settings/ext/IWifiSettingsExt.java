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
public interface IWifiSettingsExt {
    void addPreference(PreferenceScreen preferenceScreen, PreferenceCategory preferenceCategory, Preference preference, boolean z);

    void addRefreshPreference(PreferenceScreen preferenceScreen, Object obj, boolean z);

    boolean customRefreshButtonClick(Preference preference);

    void customRefreshButtonStatus(boolean z);

    boolean disconnect(MenuItem menuItem, WifiConfiguration wifiConfiguration);

    void emptyCategory(PreferenceScreen preferenceScreen);

    void emptyConneCategory(PreferenceScreen preferenceScreen);

    void emptyScreen(PreferenceScreen preferenceScreen);

    void init(PreferenceScreen preferenceScreen);

    void recordPriority(WifiConfiguration wifiConfiguration);

    void refreshCategory(PreferenceScreen preferenceScreen);

    void registerPriorityObserver(ContentResolver contentResolver);

    boolean removeConnectedAccessPointPreference();

    void setLastConnectedConfig(WifiConfiguration wifiConfiguration);

    void setNewPriority(WifiConfiguration wifiConfiguration);

    void submit(WifiConfiguration wifiConfiguration, AccessPoint accessPoint, NetworkInfo.DetailedState detailedState);

    void unregisterPriorityObserver(ContentResolver contentResolver);

    void updateContextMenu(ContextMenu contextMenu, int i, NetworkInfo.DetailedState detailedState);

    void updatePriority();

    void updatePriorityAfterSubmit(WifiConfiguration wifiConfiguration);
}
