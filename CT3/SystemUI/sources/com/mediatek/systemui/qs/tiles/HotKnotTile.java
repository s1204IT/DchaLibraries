package com.mediatek.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BenesseExtension;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.mediatek.hotknot.HotKnotAdapter;
import com.mediatek.systemui.statusbar.policy.HotKnotController;

public class HotKnotTile extends QSTile<QSTile.BooleanState> {
    private final HotKnotController mController;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisable;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mEnable;
    private boolean mListening;
    private final BroadcastReceiver mReceiver;

    public HotKnotTile(QSTile.Host host) {
        super(host);
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_signal_hotknot_enable_animation, R.drawable.ic_signal_hotknot_disable);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_signal_hotknot_disable_animation, R.drawable.ic_signal_hotknot_enable);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!"com.mediatek.hotknot.action.ADAPTER_STATE_CHANGED".equals(intent.getAction())) {
                    return;
                }
                Log.d("HotKnotTile", "HotKnotAdapter onReceive DAPTER_STATE_CHANGED");
                HotKnotTile.this.refreshState();
            }
        };
        this.mController = host.getHotKnotController();
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (this.mListening == listening) {
            return;
        }
        this.mListening = listening;
        if (listening) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.mediatek.hotknot.action.ADAPTER_STATE_CHANGED");
            this.mContext.registerReceiver(this.mReceiver, filter);
            return;
        }
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    @Override
    protected void handleClick() {
        HotKnotAdapter adapter = this.mController.getAdapter();
        boolean desiredState = !this.mController.isHotKnotOn();
        Log.d("HotKnotTile", "hotknot desiredState=" + desiredState);
        if (desiredState) {
            adapter.enable();
        } else {
            adapter.disable();
        }
    }

    @Override
    protected void handleLongClick() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("mediatek.settings.HOTKNOT_SETTINGS");
        intent.setFlags(335544320);
        this.mHost.startActivityDismissingKeyguard(intent);
    }

    @Override
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent("mediatek.settings.HOTKNOT_SETTINGS");
        intent.setFlags(335544320);
        return intent;
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_hotknot_label);
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.label = this.mContext.getString(R.string.quick_settings_hotknot_label);
        boolean desiredState = this.mController.isHotKnotOn();
        Log.d("HotKnotTile", "HotKnot UpdateState desiredState=" + desiredState);
        if (desiredState) {
            state.icon = this.mEnable;
        } else {
            state.icon = this.mDisable;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 111;
    }
}
