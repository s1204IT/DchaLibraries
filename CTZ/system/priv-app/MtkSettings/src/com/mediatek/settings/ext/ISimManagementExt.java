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
public interface ISimManagementExt {
    void configSimPreferenceScreen(Preference preference, String str, int i);

    void customBroadcast(Intent intent);

    void customRegisteBroadcast(IntentFilter intentFilter);

    void customizeListArray(List<String> list);

    void customizeMainCapabily(boolean z, int i);

    void customizeSubscriptionInfoArray(List<SubscriptionInfo> list);

    int getDefaultSmsClickContentExt(List<SubscriptionInfo> list, int i, int i2);

    void handleEvent(PreferenceFragment preferenceFragment, Context context, Preference preference);

    void hideSimEditorView(View view, Context context);

    void initAutoItemForSms(ArrayList<String> arrayList, ArrayList<SubscriptionInfo> arrayList2);

    void initPlugin(PreferenceFragment preferenceFragment);

    void initPrimarySim(PreferenceFragment preferenceFragment);

    boolean isNeedAskFirstItemForSms();

    boolean isSimDialogNeeded();

    void onCreate();

    void onDestroy();

    void onPause();

    void onPreferenceClick(Context context);

    void onResume(Context context);

    void setCurrNetworkIcon(ImageView imageView, int i, int i2);

    void setDataState(int i);

    void setDataStateEnable(int i);

    PhoneAccountHandle setDefaultCallValue(PhoneAccountHandle phoneAccountHandle);

    SubscriptionInfo setDefaultSubId(Context context, SubscriptionInfo subscriptionInfo, String str);

    void setPrefSummary(Preference preference, String str);

    void setRadioPowerState(int i, boolean z);

    void setSmsAutoItemIcon(ImageView imageView, int i, int i2);

    void showChangeDataConnDialog(PreferenceFragment preferenceFragment, boolean z);

    boolean simDialogOnClick(int i, int i2, Context context);

    void subChangeUpdatePrimarySIM();

    void updateDefaultSmsSummary(Preference preference);

    void updateList(ArrayList<String> arrayList, ArrayList<SubscriptionInfo> arrayList2, int i);

    void updatePrefState();

    boolean useCtTestcard();
}
