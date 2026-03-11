package com.mediatek.settings.ext;

import android.content.ContentResolver;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.ContextMenu;

public class DefaultWifiSettingsExt implements IWifiSettingsExt {
    private static final String TAG = "DefaultWifiSettingsExt";

    @Override
    public void registerPriorityObserver(ContentResolver contentResolver) {
    }

    @Override
    public void unregisterPriorityObserver(ContentResolver contentResolver) {
    }

    @Override
    public void setLastConnectedConfig(WifiConfiguration config) {
    }

    @Override
    public void updatePriority() {
    }

    @Override
    public void updateContextMenu(ContextMenu menu, int menuId, NetworkInfo.DetailedState state) {
    }

    @Override
    public void emptyCategory(PreferenceScreen screen) {
        screen.removeAll();
    }

    @Override
    public void emptyScreen(PreferenceScreen screen) {
        screen.removeAll();
    }

    @Override
    public void refreshCategory(PreferenceScreen screen) {
    }

    @Override
    public void recordPriority(int selectPriority) {
    }

    @Override
    public void setNewPriority(WifiConfiguration config) {
    }

    @Override
    public void updatePriorityAfterSubmit(WifiConfiguration config) {
    }

    @Override
    public void disconnect(WifiConfiguration wifiConfig) {
    }

    @Override
    public void addPreference(PreferenceScreen screen, Preference preference, boolean isConfiged) {
        if (screen == null) {
            return;
        }
        screen.addPreference(preference);
    }

    @Override
    public void addCategories(PreferenceScreen screen) {
    }
}
