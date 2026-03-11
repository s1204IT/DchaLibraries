package com.android.settings.dashboard.conditional;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.PersistableBundle;
import android.service.notification.ZenModeConfig;
import com.android.settings.R;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class DndCondition extends Condition {
    private ZenModeConfig mConfig;
    private int mZen;

    public DndCondition(ConditionManager manager) {
        super(manager);
    }

    @Override
    public void refreshState() {
        NotificationManager notificationManager = (NotificationManager) this.mManager.getContext().getSystemService(NotificationManager.class);
        this.mZen = notificationManager.getZenMode();
        boolean zenModeEnabled = this.mZen != 0;
        if (zenModeEnabled) {
            this.mConfig = notificationManager.getZenModeConfig();
        } else {
            this.mConfig = null;
        }
        setActive(zenModeEnabled);
    }

    @Override
    boolean saveState(PersistableBundle bundle) {
        bundle.putInt("state", this.mZen);
        return super.saveState(bundle);
    }

    @Override
    void restoreState(PersistableBundle bundle) {
        super.restoreState(bundle);
        this.mZen = bundle.getInt("state", 0);
    }

    @Override
    protected Class<?> getReceiverClass() {
        return Receiver.class;
    }

    private CharSequence getZenState() {
        switch (this.mZen) {
            case DefaultWfcSettingsExt.PAUSE:
                return this.mManager.getContext().getString(R.string.zen_mode_option_important_interruptions);
            case DefaultWfcSettingsExt.CREATE:
                return this.mManager.getContext().getString(R.string.zen_mode_option_no_interruptions);
            case DefaultWfcSettingsExt.DESTROY:
                return this.mManager.getContext().getString(R.string.zen_mode_option_alarms);
            default:
                return null;
        }
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(this.mManager.getContext(), R.drawable.ic_zen);
    }

    @Override
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_zen_title, getZenState());
    }

    @Override
    public CharSequence getSummary() {
        boolean isForever;
        if (this.mConfig == null || this.mConfig.manualRule == null) {
            isForever = false;
        } else {
            isForever = this.mConfig.manualRule.conditionId == null;
        }
        return isForever ? this.mManager.getContext().getString(android.R.string.lockscreen_instructions_when_pattern_enabled) : ZenModeConfig.getConditionSummary(this.mManager.getContext(), this.mConfig, ActivityManager.getCurrentUser(), false);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getString(R.string.condition_turn_off)};
    }

    @Override
    public void onPrimaryClick() {
        StatusBarManager statusBar = (StatusBarManager) this.mManager.getContext().getSystemService(StatusBarManager.class);
        statusBar.expandSettingsPanel("dnd");
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            NotificationManager notificationManager = (NotificationManager) this.mManager.getContext().getSystemService(NotificationManager.class);
            notificationManager.setZenMode(0, null, "DndCondition");
            setActive(false);
            return;
        }
        throw new IllegalArgumentException("Unexpected index " + index);
    }

    @Override
    public int getMetricsConstant() {
        return 381;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.app.action.INTERRUPTION_FILTER_CHANGED_INTERNAL".equals(intent.getAction())) {
                return;
            }
            ((DndCondition) ConditionManager.get(context).getCondition(DndCondition.class)).refreshState();
        }
    }
}
