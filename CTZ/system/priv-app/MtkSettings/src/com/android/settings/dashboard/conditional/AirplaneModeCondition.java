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
import java.io.IOException;

/* loaded from: classes.dex */
public class AirplaneModeCondition extends Condition {
    private final Receiver mReceiver;
    public static String TAG = "APM_Condition";
    private static final IntentFilter AIRPLANE_MODE_FILTER = new IntentFilter("android.intent.action.AIRPLANE_MODE");

    public AirplaneModeCondition(ConditionManager conditionManager) {
        super(conditionManager);
        this.mReceiver = new Receiver();
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void refreshState() throws IllegalStateException, IOException, IllegalArgumentException {
        Log.d(TAG, "APM condition refreshed");
        setActive(WirelessUtils.isAirplaneModeOn(this.mManager.getContext()));
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    protected BroadcastReceiver getReceiver() {
        return this.mReceiver;
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    protected IntentFilter getIntentFilter() {
        return AIRPLANE_MODE_FILTER;
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_airplane);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    protected void setActive(boolean z) throws IllegalStateException, IOException, IllegalArgumentException {
        super.setActive(z);
        Log.d(TAG, "setActive was called with " + z);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_airplane_title);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getSummary() {
        return this.mManager.getContext().getString(R.string.condition_airplane_summary);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getString(R.string.condition_turn_off)};
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onPrimaryClick() {
        this.mManager.getContext().startActivity(new Intent("android.settings.WIRELESS_SETTINGS").addFlags(268435456));
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onActionClick(int i) throws IllegalStateException, IOException, IllegalArgumentException {
        if (i == 0) {
            ConnectivityManager.from(this.mManager.getContext()).setAirplaneMode(false);
            setActive(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + i);
        }
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public int getMetricsConstant() {
        return 377;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) throws IllegalStateException, IOException, IllegalArgumentException {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                ((AirplaneModeCondition) ConditionManager.get(context).getCondition(AirplaneModeCondition.class)).refreshState();
            }
        }
    }
}
