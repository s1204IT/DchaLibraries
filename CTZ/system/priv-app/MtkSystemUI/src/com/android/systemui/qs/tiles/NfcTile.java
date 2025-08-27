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

/* loaded from: classes.dex */
public class NfcTile extends QSTileImpl<QSTile.BooleanState> {
    private NfcAdapter mAdapter;
    private boolean mListening;
    private BroadcastReceiver mNfcReceiver;

    public NfcTile(QSHost qSHost) {
        super(qSHost);
        this.mNfcReceiver = new BroadcastReceiver() { // from class: com.android.systemui.qs.tiles.NfcTile.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                NfcTile.this.refreshState();
            }
        };
    }

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        this.mListening = z;
        if (this.mListening) {
            this.mContext.registerReceiver(this.mNfcReceiver, new IntentFilter("android.nfc.action.ADAPTER_STATE_CHANGED"));
        } else {
            this.mContext.unregisterReceiver(this.mNfcReceiver);
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.nfc");
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUserSwitch(int i) {
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.settings.NFC_SETTINGS");
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        if (!getAdapter().isEnabled()) {
            getAdapter().enable();
        } else {
            getAdapter().disable();
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleSecondaryClick() {
        handleClick();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_nfc_label);
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
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

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return 800;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
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
