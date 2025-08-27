package com.mediatek.settings.ext;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.telecom.PhoneAccountHandle;
import android.telephony.SubscriptionInfo;
import android.view.View;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class DefaultSimManagementExt implements ISimManagementExt {
    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void onResume(Context context) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void onPause() {
    }

    public void updateSimEditorPref(PreferenceFragment preferenceFragment) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void updateDefaultSmsSummary(Preference preference) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void showChangeDataConnDialog(PreferenceFragment preferenceFragment, boolean z) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void hideSimEditorView(View view, Context context) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void setSmsAutoItemIcon(ImageView imageView, int i, int i2) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void initAutoItemForSms(ArrayList<String> arrayList, ArrayList<SubscriptionInfo> arrayList2) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void setDataState(int i) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void setDataStateEnable(int i) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void customizeListArray(List<String> list) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void customizeSubscriptionInfoArray(List<SubscriptionInfo> list) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public boolean isSimDialogNeeded() {
        return true;
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public boolean useCtTestcard() {
        return false;
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void setRadioPowerState(int i, boolean z) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public SubscriptionInfo setDefaultSubId(Context context, SubscriptionInfo subscriptionInfo, String str) {
        return subscriptionInfo;
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public PhoneAccountHandle setDefaultCallValue(PhoneAccountHandle phoneAccountHandle) {
        return phoneAccountHandle;
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void configSimPreferenceScreen(Preference preference, String str, int i) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void updateList(ArrayList<String> arrayList, ArrayList<SubscriptionInfo> arrayList2, int i) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public boolean simDialogOnClick(int i, int i2, Context context) {
        return false;
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void setCurrNetworkIcon(ImageView imageView, int i, int i2) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void setPrefSummary(Preference preference, String str) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void initPlugin(PreferenceFragment preferenceFragment) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void handleEvent(PreferenceFragment preferenceFragment, Context context, Preference preference) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void updatePrefState() {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void onDestroy() {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void onCreate() {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void customBroadcast(Intent intent) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void customRegisteBroadcast(IntentFilter intentFilter) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void customizeMainCapabily(boolean z, int i) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public boolean isNeedAskFirstItemForSms() {
        return true;
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public int getDefaultSmsClickContentExt(List<SubscriptionInfo> list, int i, int i2) {
        return i2;
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void initPrimarySim(PreferenceFragment preferenceFragment) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void onPreferenceClick(Context context) {
    }

    @Override // com.mediatek.settings.ext.ISimManagementExt
    public void subChangeUpdatePrimarySIM() {
    }
}
