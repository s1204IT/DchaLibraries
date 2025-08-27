package com.android.settings.dashboard.conditional;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.NetworkPolicyManager;
import android.util.FeatureFlagUtils;
import com.android.settings.R;
import com.android.settings.Settings;

/* loaded from: classes.dex */
public class BackgroundDataCondition extends Condition {
    public BackgroundDataCondition(ConditionManager conditionManager) {
        super(conditionManager);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void refreshState() {
        setActive(NetworkPolicyManager.from(this.mManager.getContext()).getRestrictBackground());
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_data_saver);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_bg_data_title);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getSummary() {
        return this.mManager.getContext().getString(R.string.condition_bg_data_summary);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getString(R.string.condition_turn_off)};
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onPrimaryClick() {
        Class cls;
        if (FeatureFlagUtils.isEnabled(this.mManager.getContext(), "settings_data_usage_v2")) {
            cls = Settings.DataUsageSummaryActivity.class;
        } else {
            cls = Settings.DataUsageSummaryLegacyActivity.class;
        }
        this.mManager.getContext().startActivity(new Intent(this.mManager.getContext(), (Class<?>) cls).addFlags(268435456));
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public int getMetricsConstant() {
        return 378;
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onActionClick(int i) {
        if (i == 0) {
            NetworkPolicyManager.from(this.mManager.getContext()).setRestrictBackground(false);
            setActive(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + i);
        }
    }
}
