package com.android.settings.dashboard.conditional;

import android.app.NotificationManager;
import android.graphics.drawable.Drawable;
import com.android.settings.R;

public class RingerMutedCondition extends AbnormalRingerConditionBase {
    private final NotificationManager mNotificationManager;

    RingerMutedCondition(ConditionManager conditionManager) {
        super(conditionManager);
        this.mNotificationManager = (NotificationManager) this.mManager.getContext().getSystemService("notification");
    }

    @Override
    public void refreshState() {
        int zenMode;
        boolean z = false;
        if (this.mNotificationManager != null) {
            zenMode = this.mNotificationManager.getZenMode();
        } else {
            zenMode = 0;
        }
        boolean z2 = zenMode != 0;
        if ((this.mAudioManager.getRingerModeInternal() == 0) && !z2) {
            z = true;
        }
        setActive(z);
    }

    @Override
    public int getMetricsConstant() {
        return 1368;
    }

    @Override
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_notifications_off_24dp);
    }

    @Override
    public CharSequence getTitle() {
        return this.mManager.getContext().getText(R.string.condition_device_muted_title);
    }

    @Override
    public CharSequence getSummary() {
        return this.mManager.getContext().getText(R.string.condition_device_muted_summary);
    }
}
