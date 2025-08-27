package com.android.settings.dashboard.conditional;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.PersistableBundle;
import android.service.notification.ZenModeConfig;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.ZenModeSettings;

/* loaded from: classes.dex */
public class DndCondition extends Condition {
    static final IntentFilter DND_FILTER = new IntentFilter("android.app.action.INTERRUPTION_FILTER_CHANGED_INTERNAL");
    protected ZenModeConfig mConfig;
    private final Receiver mReceiver;
    private boolean mRegistered;
    private int mZen;

    public DndCondition(ConditionManager conditionManager) {
        super(conditionManager);
        this.mReceiver = new Receiver();
        this.mManager.getContext().registerReceiver(this.mReceiver, DND_FILTER);
        this.mRegistered = true;
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void refreshState() {
        NotificationManager notificationManager = (NotificationManager) this.mManager.getContext().getSystemService(NotificationManager.class);
        this.mZen = notificationManager.getZenMode();
        boolean z = this.mZen != 0;
        if (z) {
            this.mConfig = notificationManager.getZenModeConfig();
        } else {
            this.mConfig = null;
        }
        setActive(z);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    boolean saveState(PersistableBundle persistableBundle) {
        persistableBundle.putInt("state", this.mZen);
        return super.saveState(persistableBundle);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    void restoreState(PersistableBundle persistableBundle) {
        super.restoreState(persistableBundle);
        this.mZen = persistableBundle.getInt("state", 0);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_do_not_disturb_on_24dp);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_zen_title);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getSummary() {
        return ZenModeConfig.getDescription(this.mManager.getContext(), this.mZen != 0, this.mConfig, true);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getString(R.string.condition_turn_off)};
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onPrimaryClick() {
        new SubSettingLauncher(this.mManager.getContext()).setDestination(ZenModeSettings.class.getName()).setSourceMetricsCategory(35).setTitle(R.string.zen_mode_settings_title).addFlags(268435456).launch();
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onActionClick(int i) {
        if (i == 0) {
            ((NotificationManager) this.mManager.getContext().getSystemService(NotificationManager.class)).setZenMode(0, null, "DndCondition");
            setActive(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + i);
        }
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public int getMetricsConstant() {
        return 381;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Condition condition;
            if ("android.app.action.INTERRUPTION_FILTER_CHANGED_INTERNAL".equals(intent.getAction()) && (condition = ConditionManager.get(context).getCondition(DndCondition.class)) != null) {
                condition.refreshState();
            }
        }
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onResume() {
        if (!this.mRegistered) {
            this.mManager.getContext().registerReceiver(this.mReceiver, DND_FILTER);
            this.mRegistered = true;
        }
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onPause() {
        if (this.mRegistered) {
            this.mManager.getContext().unregisterReceiver(this.mReceiver);
            this.mRegistered = false;
        }
    }
}
