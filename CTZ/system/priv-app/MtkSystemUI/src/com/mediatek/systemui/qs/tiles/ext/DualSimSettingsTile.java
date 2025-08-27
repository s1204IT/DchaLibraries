package com.mediatek.systemui.qs.tiles.ext;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;

/* loaded from: classes.dex */
public class DualSimSettingsTile extends QSTileImpl<QSTile.BooleanState> {
    private static final Intent DUAL_SIM_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$SimSettingsActivity"));
    private final IconIdWrapper mDisableIconIdWrapper;
    private final IconIdWrapper mEnableIconIdWrapper;
    private BroadcastReceiver mSimStateIntentReceiver;
    private CharSequence mTileLabel;

    public DualSimSettingsTile(QSHost qSHost) {
        super(qSHost);
        this.mEnableIconIdWrapper = new IconIdWrapper();
        this.mDisableIconIdWrapper = new IconIdWrapper();
        this.mSimStateIntentReceiver = new BroadcastReceiver() { // from class: com.mediatek.systemui.qs.tiles.ext.DualSimSettingsTile.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("DualSimSettingsTile", "onReceive action is " + action);
                if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                    String stringExtra = intent.getStringExtra("ss");
                    Log.d("DualSimSettingsTile", "onReceive action is " + action + " stateExtra=" + stringExtra);
                    if ("ABSENT".equals(stringExtra)) {
                        DualSimSettingsTile.this.handleRefreshState(false);
                    } else {
                        DualSimSettingsTile.this.handleRefreshState(true);
                    }
                }
            }
        };
        registerSimStateReceiver();
    }

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return R.styleable.AppCompatTheme_windowActionBar;
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        this.mTileLabel = OpSystemUICustomizationFactoryBase.getOpFactory(this.mContext).makeQuickSettings(this.mContext).getTileLabel("dulsimsettings");
        return this.mTileLabel;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return null;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleLongClick() {
        handleClick();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        long defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        Log.d("DualSimSettingsTile", "handleClick, " + DUAL_SIM_SETTINGS);
        DUAL_SIM_SETTINGS.putExtra("subscription", defaultDataSubscriptionId);
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(DUAL_SIM_SETTINGS, 0);
        refreshState();
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        Boolean bool = (Boolean) obj;
        Log.d("DualSimSettingsTile", "handleUpdateState,  simInserted=" + bool);
        IQuickSettingsPlugin iQuickSettingsPluginMakeQuickSettings = OpSystemUICustomizationFactoryBase.getOpFactory(this.mContext).makeQuickSettings(this.mContext);
        if (bool != null && bool.booleanValue()) {
            booleanState.label = iQuickSettingsPluginMakeQuickSettings.customizeDualSimSettingsTile(false, this.mDisableIconIdWrapper, "");
            booleanState.icon = QsIconWrapper.get(this.mDisableIconIdWrapper.getIconId(), this.mDisableIconIdWrapper);
        } else {
            booleanState.label = iQuickSettingsPluginMakeQuickSettings.customizeDualSimSettingsTile(true, this.mEnableIconIdWrapper, "");
            booleanState.icon = QsIconWrapper.get(this.mEnableIconIdWrapper.getIconId(), this.mEnableIconIdWrapper);
        }
        this.mTileLabel = booleanState.label;
    }

    private void registerSimStateReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mContext.registerReceiver(this.mSimStateIntentReceiver, intentFilter);
    }
}
