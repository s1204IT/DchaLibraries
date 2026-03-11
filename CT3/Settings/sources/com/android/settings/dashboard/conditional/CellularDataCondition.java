package com.android.settings.dashboard.conditional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import com.android.settings.R;
import com.android.settings.Settings;

public class CellularDataCondition extends Condition {
    public CellularDataCondition(ConditionManager manager) {
        super(manager);
    }

    @Override
    public void refreshState() {
        ConnectivityManager connectivity = (ConnectivityManager) this.mManager.getContext().getSystemService(ConnectivityManager.class);
        TelephonyManager telephony = (TelephonyManager) this.mManager.getContext().getSystemService(TelephonyManager.class);
        if (!connectivity.isNetworkSupported(0) || telephony.getSimState() != 5) {
            setActive(false);
        } else {
            setActive(telephony.getDataEnabled() ? false : true);
        }
    }

    @Override
    protected Class<?> getReceiverClass() {
        return Receiver.class;
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(this.mManager.getContext(), R.drawable.ic_cellular_off);
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
        this.mManager.getContext().startActivity(new Intent(this.mManager.getContext(), (Class<?>) Settings.DataUsageSummaryActivity.class));
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            TelephonyManager telephony = (TelephonyManager) this.mManager.getContext().getSystemService(TelephonyManager.class);
            telephony.setDataEnabled(true);
            setActive(false);
            return;
        }
        throw new IllegalArgumentException("Unexpected index " + index);
    }

    @Override
    public int getMetricsConstant() {
        return 380;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.intent.action.ANY_DATA_STATE".equals(intent.getAction())) {
                return;
            }
            ((CellularDataCondition) ConditionManager.get(context).getCondition(CellularDataCondition.class)).refreshState();
        }
    }
}
