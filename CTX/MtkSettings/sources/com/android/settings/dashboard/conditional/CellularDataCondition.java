package com.android.settings.dashboard.conditional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import com.android.settings.R;
import com.android.settings.Settings;

public class CellularDataCondition extends Condition {
    private static final IntentFilter DATA_CONNECTION_FILTER = new IntentFilter("android.intent.action.ANY_DATA_STATE");
    private final Receiver mReceiver;

    public CellularDataCondition(ConditionManager conditionManager) {
        super(conditionManager);
        this.mReceiver = new Receiver();
    }

    @Override
    public void refreshState() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mManager.getContext().getSystemService(ConnectivityManager.class);
        TelephonyManager telephonyManager = (TelephonyManager) this.mManager.getContext().getSystemService(TelephonyManager.class);
        if (!connectivityManager.isNetworkSupported(0) || telephonyManager.getSimState() != 5) {
            setActive(false);
        } else {
            setActive(!telephonyManager.isDataEnabled());
        }
    }

    @Override
    protected BroadcastReceiver getReceiver() {
        return this.mReceiver;
    }

    @Override
    protected IntentFilter getIntentFilter() {
        return DATA_CONNECTION_FILTER;
    }

    @Override
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_cellular_off);
    }

    @Override
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_cellular_title);
    }

    @Override
    public CharSequence getSummary() {
        return this.mManager.getContext().getString(R.string.condition_cellular_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getString(R.string.condition_turn_on)};
    }

    @Override
    public void onPrimaryClick() {
        this.mManager.getContext().startActivity(new Intent(this.mManager.getContext(), (Class<?>) Settings.DataUsageSummaryActivity.class).addFlags(268435456));
    }

    @Override
    public void onActionClick(int i) {
        if (i == 0) {
            ((TelephonyManager) this.mManager.getContext().getSystemService(TelephonyManager.class)).setDataEnabled(true);
            setActive(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + i);
        }
    }

    @Override
    public int getMetricsConstant() {
        return 380;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            CellularDataCondition cellularDataCondition;
            if ("android.intent.action.ANY_DATA_STATE".equals(intent.getAction()) && (cellularDataCondition = (CellularDataCondition) ConditionManager.get(context).getCondition(CellularDataCondition.class)) != null) {
                cellularDataCondition.refreshState();
            }
        }
    }
}
