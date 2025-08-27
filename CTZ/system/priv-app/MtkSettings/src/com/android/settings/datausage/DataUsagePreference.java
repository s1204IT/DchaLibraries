package com.android.settings.datausage;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.util.FeatureFlagUtils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.datausage.TemplatePreference;
import com.android.settingslib.net.DataUsageController;

/* loaded from: classes.dex */
public class DataUsagePreference extends Preference implements TemplatePreference {
    private int mSubId;
    private NetworkTemplate mTemplate;
    private int mTitleRes;

    public DataUsagePreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, new int[]{R.attr.title}, TypedArrayUtils.getAttr(context, com.android.settings.R.attr.preferenceStyle, R.attr.preferenceStyle), 0);
        this.mTitleRes = typedArrayObtainStyledAttributes.getResourceId(0, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override // com.android.settings.datausage.TemplatePreference
    public void setTemplate(NetworkTemplate networkTemplate, int i, TemplatePreference.NetworkServices networkServices) {
        this.mTemplate = networkTemplate;
        this.mSubId = i;
        DataUsageController.DataUsageInfo dataUsageInfo = new DataUsageController(getContext()).getDataUsageInfo(this.mTemplate);
        if (FeatureFlagUtils.isEnabled(getContext(), "settings_data_usage_v2") && this.mTemplate.isMatchRuleMobile()) {
            setTitle(com.android.settings.R.string.app_cellular_data_usage);
        } else {
            setTitle(this.mTitleRes);
            setSummary(getContext().getString(com.android.settings.R.string.data_usage_template, DataUsageUtils.formatDataUsage(getContext(), dataUsageInfo.usageLevel), dataUsageInfo.period));
        }
        setIntent(getIntent());
    }

    @Override // android.support.v7.preference.Preference
    public Intent getIntent() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("network_template", this.mTemplate);
        bundle.putInt("sub_id", this.mSubId);
        SubSettingLauncher sourceMetricsCategory = new SubSettingLauncher(getContext()).setArguments(bundle).setDestination(DataUsageList.class.getName()).setSourceMetricsCategory(0);
        if (FeatureFlagUtils.isEnabled(getContext(), "settings_data_usage_v2")) {
            if (this.mTemplate.isMatchRuleMobile()) {
                sourceMetricsCategory.setTitle(com.android.settings.R.string.app_cellular_data_usage);
            } else {
                sourceMetricsCategory.setTitle(this.mTitleRes);
            }
        } else if (this.mTitleRes > 0) {
            sourceMetricsCategory.setTitle(this.mTitleRes);
        } else {
            sourceMetricsCategory.setTitle(getTitle());
        }
        return sourceMetricsCategory.toIntent();
    }
}
