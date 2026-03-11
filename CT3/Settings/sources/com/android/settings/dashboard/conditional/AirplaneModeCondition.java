package com.android.settings.dashboard.conditional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settingslib.WirelessUtils;

public class AirplaneModeCondition extends Condition {
    public AirplaneModeCondition(ConditionManager conditionManager) {
        super(conditionManager);
    }

    @Override
    public void refreshState() {
        setActive(WirelessUtils.isAirplaneModeOn(this.mManager.getContext()));
    }

    @Override
    protected Class<?> getReceiverClass() {
        return Receiver.class;
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(this.mManager.getContext(), R.drawable.ic_airplane);
    }

    @Override
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_airplane_title);
    }

    @Override
    public CharSequence getSummary() {
        return this.mManager.getContext().getString(R.string.condition_airplane_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getString(R.string.condition_turn_off)};
    }

    @Override
    public void onPrimaryClick() {
        this.mManager.getContext().startActivity(new Intent(this.mManager.getContext(), (Class<?>) Settings.WirelessSettingsActivity.class));
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            ConnectivityManager.from(this.mManager.getContext()).setAirplaneMode(false);
            setActive(false);
            return;
        }
        throw new IllegalArgumentException("Unexpected index " + index);
    }

    @Override
    public int getMetricsConstant() {
        return 377;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                return;
            }
            ((AirplaneModeCondition) ConditionManager.get(context).getCondition(AirplaneModeCondition.class)).refreshState();
        }
    }
}
