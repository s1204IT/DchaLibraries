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

/* loaded from: classes.dex */
public class DefaultWifiExt implements IWifiExt {
    private static final String TAG = "DefaultWifiExt";
    private Context mContext;

    public DefaultWifiExt(Context context) {
        this.mContext = context;
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void setAPNetworkId(WifiConfiguration wifiConfiguration) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void setAPPriority(int i) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void setPriorityView(LinearLayout linearLayout, WifiConfiguration wifiConfiguration, boolean z) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void setSecurityText(TextView textView) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void addDisconnectButton(AlertDialog alertDialog, boolean z, NetworkInfo.DetailedState detailedState, WifiConfiguration wifiConfiguration) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public int getPriority(int i) {
        return i;
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void setProxyText(TextView textView) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void initConnectView(Activity activity, PreferenceScreen preferenceScreen) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void initNetworkInfoView(PreferenceScreen preferenceScreen) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void refreshNetworkInfoView() {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void initPreference(ContentResolver contentResolver) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void setSleepPolicyPreference(ListPreference listPreference, String[] strArr, String[] strArr2) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void hideWifiConfigInfo(IWifiExt.Builder builder, Context context) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void setEapMethodArray(ArrayAdapter arrayAdapter, String str, int i) {
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public int getEapMethodbySpinnerPos(int i, String str, int i2) {
        return i;
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public int getPosByEapMethod(int i, String str, int i2) {
        return i;
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public Object createWifiPreferenceController(Context context, Object obj) {
        return null;
    }

    @Override // com.mediatek.settings.ext.IWifiExt
    public void addPreferenceController(Object obj, Object obj2) {
    }
}
