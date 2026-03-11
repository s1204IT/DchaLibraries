package com.android.settings.dashboard.conditional;

import android.graphics.drawable.Drawable;
import com.android.settings.R;

public class RingerVibrateCondition extends AbnormalRingerConditionBase {
    RingerVibrateCondition(ConditionManager conditionManager) {
        super(conditionManager);
    }

    @Override
    public void refreshState() {
        setActive(this.mAudioManager.getRingerModeInternal() == 1);
    }

    @Override
    public int getMetricsConstant() {
        return 1369;
    }

    @Override
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_volume_ringer_vibrate);
    }

    @Override
    public CharSequence getTitle() {
        return this.mManager.getContext().getText(R.string.condition_device_vibrate_title);
    }

    @Override
    public CharSequence getSummary() {
        return this.mManager.getContext().getText(R.string.condition_device_vibrate_summary);
    }
}
