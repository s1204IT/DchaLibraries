package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

/* loaded from: classes.dex */
public class AirplaneModeTile extends QSTileImpl<QSTile.BooleanState> {
    private final QSTile.Icon mIcon;
    private boolean mListening;
    private final BroadcastReceiver mReceiver;
    private final GlobalSetting mSetting;

    public AirplaneModeTile(QSHost qSHost) {
        super(qSHost);
        this.mIcon = QSTileImpl.ResourceIcon.get(R.drawable.ic_signal_airplane);
        this.mReceiver = new BroadcastReceiver() { // from class: com.android.systemui.qs.tiles.AirplaneModeTile.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                    AirplaneModeTile.this.refreshState();
                }
            }
        };
        this.mSetting = new GlobalSetting(this.mContext, this.mHandler, "airplane_mode_on") { // from class: com.android.systemui.qs.tiles.AirplaneModeTile.1
            @Override // com.android.systemui.qs.GlobalSetting
            protected void handleValueChanged(int i) {
                if (AirplaneModeTile.DEBUG) {
                    Log.d(AirplaneModeTile.this.TAG, "Received Gloable changes to " + i);
                }
                AirplaneModeTile.this.handleRefreshState(Integer.valueOf(i));
            }
        };
    }

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        QSTile.BooleanState booleanState = new QSTile.BooleanState();
        if (this.mSetting != null && this.mSetting.getValue() == 1) {
            booleanState.value = true;
        }
        return booleanState;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleClick() {
        boolean z = ((QSTile.BooleanState) this.mState).value;
        MetricsLogger.action(this.mContext, getMetricsCategory(), !z);
        if (!z && Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(new Intent("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS"), 0);
            if (DEBUG) {
                Log.w(this.TAG, "AirplaneModeTile click not work!");
                return;
            }
            return;
        }
        setEnabled(!z);
    }

    private void setEnabled(boolean z) {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (DEBUG) {
            Log.d(this.TAG, "AirplaneModeTile will set enable to " + z);
        }
        connectivityManager.setAirplaneMode(z);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.settings.AIRPLANE_MODE_SETTINGS");
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.airplane_mode);
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        boolean z;
        checkIfRestrictionEnforcedByAdminOnly(booleanState, "no_airplane_mode");
        if ((obj instanceof Integer ? ((Integer) obj).intValue() : this.mSetting.getValue()) == 0) {
            z = false;
        } else {
            z = true;
        }
        booleanState.value = z;
        booleanState.label = this.mContext.getString(R.string.airplane_mode);
        booleanState.icon = this.mIcon;
        if (booleanState.slash == null) {
            booleanState.slash = new QSTile.SlashState();
        }
        booleanState.slash.isSlashed = !z;
        booleanState.state = z ? 2 : 1;
        booleanState.contentDescription = booleanState.label;
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return com.android.systemui.plugins.R.styleable.AppCompatTheme_windowActionBarOverlay;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_airplane_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_airplane_changed_off);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        if (this.mListening == z) {
            return;
        }
        this.mListening = z;
        if (z) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
            this.mContext.registerReceiver(this.mReceiver, intentFilter);
        } else {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
        this.mSetting.setListening(z);
    }
}
