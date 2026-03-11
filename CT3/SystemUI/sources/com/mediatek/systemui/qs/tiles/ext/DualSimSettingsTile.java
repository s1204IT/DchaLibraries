package com.mediatek.systemui.qs.tiles.ext;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BenesseExtension;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.systemui.qs.QSTile;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;

public class DualSimSettingsTile extends QSTile<QSTile.BooleanState> {
    private static final Intent DUAL_SIM_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$SimSettingsActivity"));
    private static final String TAG = "DualSimSettingsTile";
    private final IconIdWrapper mDisableIconIdWrapper;
    private final IconIdWrapper mEnableIconIdWrapper;
    private BroadcastReceiver mSimStateIntentReceiver;
    private CharSequence mTileLabel;

    public DualSimSettingsTile(QSTile.Host host) {
        super(host);
        this.mEnableIconIdWrapper = new IconIdWrapper();
        this.mDisableIconIdWrapper = new IconIdWrapper();
        this.mSimStateIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(DualSimSettingsTile.TAG, "onReceive action is " + action);
                if (!action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                    return;
                }
                String stateExtra = intent.getStringExtra("ss");
                Log.d(DualSimSettingsTile.TAG, "onReceive action is " + action + " stateExtra=" + stateExtra);
                if ("ABSENT".equals(stateExtra)) {
                    DualSimSettingsTile.this.handleRefreshState(false);
                } else {
                    DualSimSettingsTile.this.handleRefreshState(true);
                }
            }
        };
        registerSimStateReceiver();
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
    }

    @Override
    public int getMetricsCategory() {
        return 111;
    }

    @Override
    public CharSequence getTileLabel() {
        this.mTileLabel = PluginManager.getQuickSettingsPlugin(this.mContext).getTileLabel("dulsimsettings");
        return this.mTileLabel;
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected void handleLongClick() {
        handleClick();
    }

    @Override
    protected void handleClick() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        long subId = SubscriptionManager.getDefaultDataSubscriptionId();
        Log.d(TAG, "handleClick, " + DUAL_SIM_SETTINGS);
        DUAL_SIM_SETTINGS.putExtra("subscription", subId);
        this.mHost.startActivityDismissingKeyguard(DUAL_SIM_SETTINGS);
        refreshState();
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        Boolean simInserted = (Boolean) arg;
        Log.d(TAG, "handleUpdateState,  simInserted=" + simInserted);
        IQuickSettingsPlugin quickSettingsPlugin = PluginManager.getQuickSettingsPlugin(this.mContext);
        if (simInserted != null && simInserted.booleanValue()) {
            state.label = quickSettingsPlugin.customizeDualSimSettingsTile(false, this.mDisableIconIdWrapper, "");
            state.icon = QsIconWrapper.get(this.mDisableIconIdWrapper.getIconId(), this.mDisableIconIdWrapper);
        } else {
            state.label = quickSettingsPlugin.customizeDualSimSettingsTile(true, this.mEnableIconIdWrapper, "");
            state.icon = QsIconWrapper.get(this.mEnableIconIdWrapper.getIconId(), this.mEnableIconIdWrapper);
        }
        this.mTileLabel = state.label;
    }

    private void registerSimStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mContext.registerReceiver(this.mSimStateIntentReceiver, filter);
    }
}
