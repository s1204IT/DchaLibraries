package com.android.settings.dashboard.conditional;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.NetworkPolicyManager;
import com.android.settings.R;
import com.android.settings.Settings;

public class BackgroundDataCondition extends Condition {
    public BackgroundDataCondition(ConditionManager manager) {
        super(manager);
    }

    @Override
    public void refreshState() {
        setActive(NetworkPolicyManager.from(this.mManager.getContext()).getRestrictBackground());
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(this.mManager.getContext(), R.drawable.ic_data_saver);
    }

    @Override
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_bg_data_title);
    }

    @Override
    public CharSequence getSummary() {
        return this.mManager.getContext().getString(R.string.condition_bg_data_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getString(R.string.condition_turn_off)};
    }

    @Override
    public void onPrimaryClick() {
        this.mManager.getContext().startActivity(new Intent(this.mManager.getContext(), (Class<?>) Settings.DataUsageSummaryActivity.class));
    }

    @Override
    public int getMetricsConstant() {
        return 378;
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            NetworkPolicyManager.from(this.mManager.getContext()).setRestrictBackground(false);
            setActive(false);
            return;
        }
        throw new IllegalArgumentException("Unexpected index " + index);
    }
}
