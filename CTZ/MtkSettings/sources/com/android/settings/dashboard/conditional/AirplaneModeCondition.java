package com.android.settings.dashboard.conditional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.util.Log;
import com.android.settings.R;
import com.android.settingslib.WirelessUtils;

public class AirplaneModeCondition extends Condition {
    private final Receiver mReceiver;
    public static String TAG = "APM_Condition";
    private static final IntentFilter AIRPLANE_MODE_FILTER = new IntentFilter("android.intent.action.AIRPLANE_MODE");

    public AirplaneModeCondition(ConditionManager conditionManager) {
        super(conditionManager);
        this.mReceiver = new Receiver();
    }

    @Override
    public void refreshState() {
        Log.d(TAG, "APM condition refreshed");
        setActive(WirelessUtils.isAirplaneModeOn(this.mManager.getContext()));
    }

    @Override
    protected BroadcastReceiver getReceiver() {
        return this.mReceiver;
    }

    @Override
    protected IntentFilter getIntentFilter() {
        return AIRPLANE_MODE_FILTER;
    }

    @Override
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_airplane);
    }

    @Override
    protected void setActive(boolean z) {
        super.setActive(z);
        Log.d(TAG, "setActive was called with " + z);
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
        this.mManager.getContext().startActivity(new Intent("android.settings.WIRELESS_SETTINGS").addFlags(268435456));
    }

    @Override
    public void onActionClick(int i) {
        if (i == 0) {
            ConnectivityManager.from(this.mManager.getContext()).setAirplaneMode(false);
            setActive(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + i);
        }
    }

    @Override
    public int getMetricsConstant() {
        return 377;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                ((AirplaneModeCondition) ConditionManager.get(context).getCondition(AirplaneModeCondition.class)).refreshState();
            }
        }
    }
}
