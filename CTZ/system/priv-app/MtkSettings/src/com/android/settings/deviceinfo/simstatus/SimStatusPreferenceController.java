package com.android.settings.deviceinfo.simstatus;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.deviceinfo.AbstractSimStatusImeiInfoPreferenceController;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class SimStatusPreferenceController extends AbstractSimStatusImeiInfoPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnCreate, OnDestroy {
    private final Fragment mFragment;
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener;
    private final List<Preference> mPreferenceList;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;

    public SimStatusPreferenceController(Context context, Fragment fragment, Lifecycle lifecycle) {
        super(context);
        this.mPreferenceList = new ArrayList();
        this.mOnSubscriptionsChangeListener = new SubscriptionManager.OnSubscriptionsChangedListener() { // from class: com.android.settings.deviceinfo.simstatus.SimStatusPreferenceController.1
            @Override // android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
            public void onSubscriptionsChanged() {
                Log.d("SimStatusPreferenceController", "onSubscriptionsChanged");
                SimStatusPreferenceController.this.updateState(null);
            }
        };
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mSubscriptionManager = (SubscriptionManager) context.getSystemService("telephony_subscription_service");
        this.mFragment = fragment;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "sim_status";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference(getPreferenceKey());
        if (!isAvailable() || preferenceFindPreference == null || !preferenceFindPreference.isVisible()) {
            return;
        }
        this.mPreferenceList.add(preferenceFindPreference);
        int order = preferenceFindPreference.getOrder();
        for (int i = 1; i < this.mTelephonyManager.getPhoneCount(); i++) {
            Preference preferenceCreateNewPreference = createNewPreference(preferenceScreen.getContext());
            preferenceCreateNewPreference.setOrder(order + i);
            preferenceCreateNewPreference.setKey("sim_status" + i);
            preferenceScreen.addPreference(preferenceCreateNewPreference);
            this.mPreferenceList.add(preferenceCreateNewPreference);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        for (int i = 0; i < this.mPreferenceList.size(); i++) {
            Preference preference2 = this.mPreferenceList.get(i);
            preference2.setTitle(getPreferenceTitle(i));
            preference2.setSummary(getCarrierName(i));
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        int iIndexOf = this.mPreferenceList.indexOf(preference);
        if (iIndexOf == -1) {
            return false;
        }
        SimStatusDialogFragment.show(this.mFragment, iIndexOf, getPreferenceTitle(iIndexOf));
        return true;
    }

    private String getPreferenceTitle(int i) {
        if (this.mTelephonyManager.getPhoneCount() > 1) {
            return this.mContext.getString(R.string.sim_status_title_sim_slot, Integer.valueOf(i + 1));
        }
        return this.mContext.getString(R.string.sim_status_title);
    }

    private CharSequence getCarrierName(int i) {
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList != null) {
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                if (subscriptionInfo.getSimSlotIndex() == i) {
                    return subscriptionInfo.getCarrierName();
                }
            }
        }
        return this.mContext.getText(R.string.device_info_not_available);
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
