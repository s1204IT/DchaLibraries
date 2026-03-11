package com.mediatek.settings.ext;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceScreen;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mediatek.settings.ext.IWifiExt;

public class DefaultWifiExt implements IWifiExt {
    private static final String TAG = "DefaultWifiExt";
    private Context mContext;

    public DefaultWifiExt(Context context) {
        this.mContext = context;
    }

    @Override
    public void setAPNetworkId(WifiConfiguration wifiConfig) {
    }

    @Override
    public void setAPPriority(int apPriority) {
    }

    @Override
    public void setPriorityView(LinearLayout priorityLayout, WifiConfiguration wifiConfig, boolean isEdit) {
    }

    @Override
    public void setSecurityText(TextView view) {
    }

    @Override
    public void addDisconnectButton(AlertDialog dialog, boolean edit, NetworkInfo.DetailedState state, WifiConfiguration wifiConfig) {
    }

    @Override
    public int getPriority(int priority) {
        return priority;
    }

    @Override
    public void setProxyText(TextView view) {
    }

    @Override
    public void initConnectView(Activity activity, PreferenceScreen screen) {
    }

    @Override
    public void initNetworkInfoView(PreferenceScreen screen) {
    }

    @Override
    public void refreshNetworkInfoView() {
    }

    @Override
    public void initPreference(ContentResolver contentResolver) {
    }

    @Override
    public void setSleepPolicyPreference(ListPreference sleepPolicyPref, String[] sleepPolicyEntries, String[] sleepPolicyValues) {
    }

    @Override
    public void hideWifiConfigInfo(IWifiExt.Builder builder, Context context) {
    }

    @Override
    public void setEapMethodArray(ArrayAdapter adapter, String ssid, int security) {
    }

    @Override
    public int getEapMethodbySpinnerPos(int spinnerPos, String ssid, int security) {
        return spinnerPos;
    }

    @Override
    public int getPosByEapMethod(int spinnerPos, String ssid, int security) {
        return spinnerPos;
    }
}
