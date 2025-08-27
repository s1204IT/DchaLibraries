package com.android.settings.dashboard.conditional;

import android.graphics.drawable.Drawable;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.display.NightDisplaySettings;

/* loaded from: classes.dex */
public final class NightDisplayCondition extends Condition implements ColorDisplayController.Callback {
    private ColorDisplayController mController;

    NightDisplayCondition(ConditionManager conditionManager) {
        super(conditionManager);
        this.mController = new ColorDisplayController(conditionManager.getContext());
        this.mController.setListener(this);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public int getMetricsConstant() {
        return 492;
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_settings_night_display);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_night_display_title);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getSummary() {
        return this.mManager.getContext().getString(R.string.condition_night_display_summary);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getString(R.string.condition_turn_off)};
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onPrimaryClick() {
        new SubSettingLauncher(this.mManager.getContext()).setDestination(NightDisplaySettings.class.getName()).setSourceMetricsCategory(35).setTitle(R.string.night_display_title).addFlags(268435456).launch();
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onActionClick(int i) {
        if (i == 0) {
            this.mController.setActivated(false);
            return;
        }
        throw new IllegalArgumentException("Unexpected index " + i);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void refreshState() {
        setActive(this.mController.isActivated());
    }

    public void onActivated(boolean z) {
        refreshState();
    }
}
