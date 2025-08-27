package com.android.settings.datausage;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.util.FeatureFlagUtils;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.datausage.CellDataPreference;
import com.android.settings.datausage.TemplatePreference;

/* loaded from: classes.dex */
public class BillingCyclePreference extends Preference implements TemplatePreference {
    private final CellDataPreference.DataStateListener mListener;
    private TemplatePreference.NetworkServices mServices;
    private int mSubId;
    private NetworkTemplate mTemplate;

    public BillingCyclePreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mListener = new CellDataPreference.DataStateListener() { // from class: com.android.settings.datausage.BillingCyclePreference.1
            @Override // android.database.ContentObserver
            public void onChange(boolean z) {
                BillingCyclePreference.this.updateEnabled();
            }
        };
    }

    @Override // android.support.v7.preference.Preference
    public void onAttached() {
        super.onAttached();
        this.mListener.setListener(true, this.mSubId, getContext());
    }

    @Override // android.support.v7.preference.Preference
    public void onDetached() {
        this.mListener.setListener(false, this.mSubId, getContext());
        super.onDetached();
    }

    @Override // com.android.settings.datausage.TemplatePreference
    public void setTemplate(NetworkTemplate networkTemplate, int i, TemplatePreference.NetworkServices networkServices) {
        this.mTemplate = networkTemplate;
        this.mSubId = i;
        this.mServices = networkServices;
        int policyCycleDay = networkServices.mPolicyEditor.getPolicyCycleDay(this.mTemplate);
        if (FeatureFlagUtils.isEnabled(getContext(), "settings_data_usage_v2") || policyCycleDay == -1) {
            setSummary((CharSequence) null);
        } else {
            setSummary(getContext().getString(R.string.billing_cycle_fragment_summary, Integer.valueOf(policyCycleDay)));
        }
        setIntent(getIntent());
    }

    private void updateEnabled() {
        try {
            setEnabled(this.mServices.mNetworkService.isBandwidthControlEnabled() && this.mServices.mTelephonyManager.getDataEnabled(this.mSubId) && this.mServices.mUserManager.isAdminUser());
        } catch (RemoteException e) {
            setEnabled(false);
        }
    }

    @Override // android.support.v7.preference.Preference
    public Intent getIntent() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("network_template", this.mTemplate);
        bundle.putInt("sub_id", this.mSubId);
        return new SubSettingLauncher(getContext()).setDestination(BillingCycleSettings.class.getName()).setArguments(bundle).setTitle(R.string.billing_cycle).setSourceMetricsCategory(0).toIntent();
    }
}
