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
    public void setAPNetworkId(WifiConfiguration wifiConfiguration) {
    }

    @Override
    public void setAPPriority(int i) {
    }

    @Override
    public void setPriorityView(LinearLayout linearLayout, WifiConfiguration wifiConfiguration, boolean z) {
    }

    @Override
    public void setSecurityText(TextView textView) {
    }

    @Override
    public void addDisconnectButton(AlertDialog alertDialog, boolean z, NetworkInfo.DetailedState detailedState, WifiConfiguration wifiConfiguration) {
    }

    @Override
    public int getPriority(int i) {
        return i;
    }

    @Override
    public void setProxyText(TextView textView) {
    }

    @Override
    public void initConnectView(Activity activity, PreferenceScreen preferenceScreen) {
    }

    @Override
    public void initNetworkInfoView(PreferenceScreen preferenceScreen) {
    }

    @Override
    public void refreshNetworkInfoView() {
    }

    @Override
    public void initPreference(ContentResolver contentResolver) {
    }

    @Override
    public void setSleepPolicyPreference(ListPreference listPreference, String[] strArr, String[] strArr2) {
    }

    @Override
    public void hideWifiConfigInfo(IWifiExt.Builder builder, Context context) {
    }

    @Override
    public void setEapMethodArray(ArrayAdapter arrayAdapter, String str, int i) {
    }

    @Override
    public int getEapMethodbySpinnerPos(int i, String str, int i2) {
        return i;
    }

    @Override
    public int getPosByEapMethod(int i, String str, int i2) {
        return i;
    }

    @Override
    public Object createWifiPreferenceController(Context context, Object obj) {
        return null;
    }

    @Override
    public void addPreferenceController(Object obj, Object obj2) {
    }
}
