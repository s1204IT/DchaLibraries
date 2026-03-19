package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.widget.Switch;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class NfcTile extends QSTileImpl<QSTile.BooleanState> {
    private NfcAdapter mAdapter;
    private boolean mListening;
    private BroadcastReceiver mNfcReceiver;

    public NfcTile(QSHost qSHost) {
        super(qSHost);
        this.mNfcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NfcTile.this.refreshState();
            }
        };
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void handleSetListening(boolean z) {
        this.mListening = z;
        if (this.mListening) {
            this.mContext.registerReceiver(this.mNfcReceiver, new IntentFilter("android.nfc.action.ADAPTER_STATE_CHANGED"));
        } else {
            this.mContext.unregisterReceiver(this.mNfcReceiver);
        }
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.nfc");
    }

    @Override
    protected void handleUserSwitch(int i) {
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent("android.settings.NFC_SETTINGS");
    }

    @Override
    protected void handleClick() {
        if (!getAdapter().isEnabled()) {
            getAdapter().enable();
        } else {
            getAdapter().disable();
        }
    }

    @Override
    protected void handleSecondaryClick() {
        handleClick();
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_nfc_label);
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        Drawable drawable = this.mContext.getDrawable(R.drawable.ic_qs_nfc_enabled);
        Drawable drawable2 = this.mContext.getDrawable(R.drawable.ic_qs_nfc_disabled);
        if (getAdapter() == null) {
            return;
        }
        booleanState.value = getAdapter().isEnabled();
        booleanState.label = this.mContext.getString(R.string.quick_settings_nfc_label);
        if (!booleanState.value) {
            drawable = drawable2;
        }
        booleanState.icon = new QSTileImpl.DrawableIcon(drawable);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        booleanState.contentDescription = booleanState.label;
    }

    @Override
    public int getMetricsCategory() {
        return 800;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.quick_settings_nfc_on);
        }
        return this.mContext.getString(R.string.quick_settings_nfc_off);
    }

    private NfcAdapter getAdapter() {
        if (this.mAdapter == null) {
            try {
                this.mAdapter = NfcAdapter.getNfcAdapter(this.mContext);
            } catch (UnsupportedOperationException e) {
                this.mAdapter = null;
            }
        }
        return this.mAdapter;
    }
}
