package com.mediatek.settings.ext;

import android.content.Context;
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

    public void updateSimEditorPref(PreferenceFragment pref) {
    }

    @Override
    public void updateDefaultSmsSummary(Preference pref) {
    }

    @Override
    public void showChangeDataConnDialog(PreferenceFragment prefFragment, boolean isResumed) {
    }

    @Override
    public void hideSimEditorView(View view, Context context) {
    }

    @Override
    public void setSmsAutoItemIcon(ImageView view, int dialogId, int position) {
    }

    @Override
    public void initAutoItemForSms(ArrayList<String> list, ArrayList<SubscriptionInfo> smsSubInfoList) {
    }

    @Override
    public void setDataState(int subId) {
    }

    @Override
    public void setDataStateEnable(int subId) {
    }

    @Override
    public void customizeListArray(List<String> strings) {
    }

    @Override
    public void customizeSubscriptionInfoArray(List<SubscriptionInfo> subscriptionInfo) {
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
    public void setRadioPowerState(int subId, boolean turnOn) {
    }

    @Override
    public SubscriptionInfo setDefaultSubId(Context context, SubscriptionInfo sir, String type) {
        return sir;
    }

    @Override
    public PhoneAccountHandle setDefaultCallValue(PhoneAccountHandle phoneAccount) {
        return phoneAccount;
    }

    @Override
    public void configSimPreferenceScreen(Preference simPref, String type, int size) {
    }

    @Override
    public void updateList(ArrayList<String> list, ArrayList<SubscriptionInfo> smsSubInfoList, int selectableSubInfoLength) {
    }

    @Override
    public boolean simDialogOnClick(int id, int value, Context context) {
        return false;
    }

    @Override
    public void setCurrNetworkIcon(ImageView icon, int id, int position) {
    }

    @Override
    public void setPrefSummary(Preference simPref, String type) {
    }
}
