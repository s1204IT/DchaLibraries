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

public class DefaultSimManagementExt implements ISimManagementExt {
    @Override
    public void onResume(Context context) {
    }

    @Override
    public void onPause() {
    }

    public void updateSimEditorPref(PreferenceFragment preferenceFragment) {
    }

    @Override
    public void updateDefaultSmsSummary(Preference preference) {
    }

    @Override
    public void showChangeDataConnDialog(PreferenceFragment preferenceFragment, boolean z) {
    }

    @Override
    public void hideSimEditorView(View view, Context context) {
    }

    @Override
    public void setSmsAutoItemIcon(ImageView imageView, int i, int i2) {
    }

    @Override
    public void initAutoItemForSms(ArrayList<String> arrayList, ArrayList<SubscriptionInfo> arrayList2) {
    }

    @Override
    public void setDataState(int i) {
    }

    @Override
    public void setDataStateEnable(int i) {
    }

    @Override
    public void customizeListArray(List<String> list) {
    }

    @Override
    public void customizeSubscriptionInfoArray(List<SubscriptionInfo> list) {
    }

    @Override
    public boolean isSimDialogNeeded() {
        return true;
    }

    @Override
    public boolean useCtTestcard() {
        return false;
    }

    @Override
    public void setRadioPowerState(int i, boolean z) {
    }

    @Override
    public SubscriptionInfo setDefaultSubId(Context context, SubscriptionInfo subscriptionInfo, String str) {
        return subscriptionInfo;
    }

    @Override
    public PhoneAccountHandle setDefaultCallValue(PhoneAccountHandle phoneAccountHandle) {
        return phoneAccountHandle;
    }

    @Override
    public void configSimPreferenceScreen(Preference preference, String str, int i) {
    }

    @Override
    public void updateList(ArrayList<String> arrayList, ArrayList<SubscriptionInfo> arrayList2, int i) {
    }

    @Override
    public boolean simDialogOnClick(int i, int i2, Context context) {
        return false;
    }

    @Override
    public void setCurrNetworkIcon(ImageView imageView, int i, int i2) {
    }

    @Override
    public void setPrefSummary(Preference preference, String str) {
    }

    @Override
    public void initPlugin(PreferenceFragment preferenceFragment) {
    }

    @Override
    public void handleEvent(PreferenceFragment preferenceFragment, Context context, Preference preference) {
    }

    @Override
    public void updatePrefState() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void customBroadcast(Intent intent) {
    }

    @Override
    public void customRegisteBroadcast(IntentFilter intentFilter) {
    }

    @Override
    public void customizeMainCapabily(boolean z, int i) {
    }

    @Override
    public boolean isNeedAskFirstItemForSms() {
        return true;
    }

    @Override
    public int getDefaultSmsClickContentExt(List<SubscriptionInfo> list, int i, int i2) {
        return i2;
    }

    @Override
    public void initPrimarySim(PreferenceFragment preferenceFragment) {
    }

    @Override
    public void onPreferenceClick(Context context) {
    }

    @Override
    public void subChangeUpdatePrimarySIM() {
    }
}
