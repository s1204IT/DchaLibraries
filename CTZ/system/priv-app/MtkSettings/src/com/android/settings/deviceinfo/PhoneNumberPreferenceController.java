package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class PhoneNumberPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnCreate, OnDestroy {
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener;
    private final List<Preference> mPreferenceList;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;

    public PhoneNumberPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        this.mPreferenceList = new ArrayList();
        this.mOnSubscriptionsChangeListener = new SubscriptionManager.OnSubscriptionsChangedListener() { // from class: com.android.settings.deviceinfo.PhoneNumberPreferenceController.1
            @Override // android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
            public void onSubscriptionsChanged() {
                Log.d("PhoneNumberPreferenceController", "onSubscriptionsChanged");
                PhoneNumberPreferenceController.this.updateState(null);
            }
        };
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mSubscriptionManager = (SubscriptionManager) context.getSystemService("telephony_subscription_service");
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "phone_number";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mTelephonyManager.isVoiceCapable();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference(getPreferenceKey());
        this.mPreferenceList.add(preferenceFindPreference);
        int order = preferenceFindPreference.getOrder();
        for (int i = 1; i < this.mTelephonyManager.getPhoneCount(); i++) {
            Preference preferenceCreateNewPreference = createNewPreference(preferenceScreen.getContext());
            preferenceCreateNewPreference.setOrder(order + i);
            preferenceCreateNewPreference.setKey("phone_number" + i);
            preferenceScreen.addPreference(preferenceCreateNewPreference);
            this.mPreferenceList.add(preferenceCreateNewPreference);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        for (int i = 0; i < this.mPreferenceList.size(); i++) {
            Preference preference2 = this.mPreferenceList.get(i);
            preference2.setTitle(getPreferenceTitle(i));
            preference2.setSummary(getPhoneNumber(i));
        }
    }

    private CharSequence getPhoneNumber(int i) {
        SubscriptionInfo subscriptionInfo = getSubscriptionInfo(i);
        if (subscriptionInfo == null) {
            return this.mContext.getString(R.string.device_info_default);
        }
        return getFormattedPhoneNumber(subscriptionInfo);
    }

    private CharSequence getPreferenceTitle(int i) {
        if (this.mTelephonyManager.getPhoneCount() > 1) {
            return this.mContext.getString(R.string.status_number_sim_slot, Integer.valueOf(i + 1));
        }
        return this.mContext.getString(R.string.status_number);
    }

    SubscriptionInfo getSubscriptionInfo(int i) {
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList != null) {
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                if (subscriptionInfo.getSimSlotIndex() == i) {
                    return subscriptionInfo;
                }
            }
            return null;
        }
        return null;
    }

    CharSequence getFormattedPhoneNumber(SubscriptionInfo subscriptionInfo) {
        String formattedPhoneNumber = DeviceInfoUtils.getFormattedPhoneNumber(this.mContext, subscriptionInfo);
        return TextUtils.isEmpty(formattedPhoneNumber) ? this.mContext.getString(R.string.device_info_default) : BidiFormatter.getInstance().unicodeWrap(formattedPhoneNumber, TextDirectionHeuristics.LTR);
    }

    Preference createNewPreference(Context context) {
        return new Preference(context);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnCreate
    public void onCreate(Bundle bundle) {
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnDestroy
    public void onDestroy() {
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
    }
}
