package com.android.settings.dashboard.conditional;

import android.graphics.drawable.Drawable;
import com.android.settings.R;

/* loaded from: classes.dex */
public class RingerVibrateCondition extends AbnormalRingerConditionBase {
    RingerVibrateCondition(ConditionManager conditionManager) {
        super(conditionManager);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void refreshState() {
        setActive(this.mAudioManager.getRingerModeInternal() == 1);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public int getMetricsConstant() {
        return 1369;
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_volume_ringer_vibrate);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getTitle() {
        return this.mManager.getContext().getText(R.string.condition_device_vibrate_title);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getSummary() {
        return this.mManager.getContext().getText(R.string.condition_device_vibrate_summary);
    }
}
