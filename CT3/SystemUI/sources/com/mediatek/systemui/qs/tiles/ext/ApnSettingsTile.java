package com.mediatek.systemui.qs.tiles.ext;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.BenesseExtension;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.systemui.qs.QSTile;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.util.SIMHelper;

public class ApnSettingsTile extends QSTile<QSTile.State> {
    private static final Intent APN_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$ApnSettingsActivity"));
    private static final boolean DEBUG = true;
    private static final String TAG = "ApnSettingsTile";
    private boolean mApnSettingsEnabled;
    private String mApnStateLabel;
    private final IconIdWrapper mDisableApnStateIconWrapper;
    private final IconIdWrapper mEnableApnStateIconWrapper;
    private boolean mIsAirplaneMode;
    private boolean mIsWifiOnly;
    private boolean mListening;
    private final PhoneStateListener mPhoneStateListener;
    private final BroadcastReceiver mReceiver;
    private final SubscriptionManager mSubscriptionManager;
    private CharSequence mTileLabel;
    private final UserManager mUm;

    public ApnSettingsTile(QSTile.Host host) {
        super(host);
        this.mEnableApnStateIconWrapper = new IconIdWrapper();
        this.mDisableApnStateIconWrapper = new IconIdWrapper();
        this.mApnStateLabel = "";
        this.mApnSettingsEnabled = false;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(ApnSettingsTile.TAG, "onReceive(), action: " + action);
                if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                    boolean enabled = intent.getBooleanExtra("state", false);
                    Log.d(ApnSettingsTile.TAG, "onReceive(), airline mode changed: state is " + enabled);
                    ApnSettingsTile.this.updateState();
                } else if (action.equals("android.intent.action.ACTION_EF_CSP_CONTENT_NOTIFY") || action.equals("android.intent.action.MSIM_MODE") || action.equals("android.intent.action.ACTION_MD_TYPE_CHANGE") || action.equals("mediatek.intent.action.LOCATED_PLMN_CHANGED") || action.equals("android.intent.action.ACTION_SET_PHONE_RAT_FAMILY_DONE") || action.equals("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE") || action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
                    ApnSettingsTile.this.updateState();
                }
            }
        };
        this.mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                Log.d(ApnSettingsTile.TAG, "onCallStateChanged call state is " + state);
                switch (state) {
                    case 0:
                        ApnSettingsTile.this.updateState();
                        break;
                }
            }
        };
        this.mSubscriptionManager = SubscriptionManager.from(this.mContext);
        this.mUm = (UserManager) this.mContext.getSystemService("user");
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mIsWifiOnly = !cm.isNetworkSupported(0) ? DEBUG : false;
        updateState();
    }

    @Override
    public QSTile.State newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public CharSequence getTileLabel() {
        this.mTileLabel = PluginManager.getQuickSettingsPlugin(this.mContext).getTileLabel("apnsettings");
        return this.mTileLabel;
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public void setListening(boolean listening) {
        Log.d(TAG, "setListening(), listening = " + listening);
        if (this.mListening == listening) {
            return;
        }
        this.mListening = listening;
        if (listening) {
            IntentFilter mIntentFilter = new IntentFilter();
            mIntentFilter.addAction("android.intent.action.AIRPLANE_MODE");
            mIntentFilter.addAction("android.intent.action.ACTION_EF_CSP_CONTENT_NOTIFY");
            mIntentFilter.addAction("android.intent.action.MSIM_MODE");
            mIntentFilter.addAction("android.intent.action.ACTION_MD_TYPE_CHANGE");
            mIntentFilter.addAction("mediatek.intent.action.LOCATED_PLMN_CHANGED");
            mIntentFilter.addAction("android.intent.action.ACTION_SET_PHONE_RAT_FAMILY_DONE");
            mIntentFilter.addAction("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
            mIntentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
            this.mContext.registerReceiver(this.mReceiver, mIntentFilter);
            TelephonyManager.getDefault().listen(this.mPhoneStateListener, 32);
            return;
        }
        this.mContext.unregisterReceiver(this.mReceiver);
        TelephonyManager.getDefault().listen(this.mPhoneStateListener, 0);
    }

    @Override
    public int getMetricsCategory() {
        return 111;
    }

    @Override
    protected void handleLongClick() {
        handleClick();
    }

    @Override
    protected void handleClick() {
        updateState();
        Log.d(TAG, "handleClick(), mApnSettingsEnabled = " + this.mApnSettingsEnabled);
        if (!this.mApnSettingsEnabled || BenesseExtension.getDchaState() != 0) {
            return;
        }
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        APN_SETTINGS.putExtra("sub_id", subId);
        Log.d(TAG, "handleClick(), " + APN_SETTINGS);
        this.mHost.startActivityDismissingKeyguard(APN_SETTINGS);
    }

    @Override
    protected void handleUpdateState(QSTile.State state, Object arg) {
        if (this.mApnSettingsEnabled) {
            state.icon = QsIconWrapper.get(this.mEnableApnStateIconWrapper.getIconId(), this.mEnableApnStateIconWrapper);
        } else {
            state.icon = QsIconWrapper.get(this.mDisableApnStateIconWrapper.getIconId(), this.mDisableApnStateIconWrapper);
        }
        state.label = this.mApnStateLabel;
        state.contentDescription = this.mApnStateLabel;
    }

    public final void updateState() {
        boolean enabled = false;
        this.mIsAirplaneMode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0 ? DEBUG : false;
        boolean isSecondaryUser = (UserHandle.myUserId() == 0 && ActivityManager.getCurrentUser() == 0) ? false : DEBUG;
        boolean isRestricted = this.mUm.hasUserRestriction("no_config_mobile_networks");
        if (this.mIsWifiOnly || isSecondaryUser || isRestricted) {
            enabled = false;
            Log.d(TAG, "updateState(), isSecondaryUser = " + isSecondaryUser + ", mIsWifiOnly = " + this.mIsWifiOnly + ", isRestricted = " + isRestricted);
        } else {
            int simNum = this.mSubscriptionManager.getActiveSubscriptionInfoCount();
            int callState = TelephonyManager.getDefault().getCallState();
            boolean isIdle = callState == 0 ? DEBUG : false;
            if (!this.mIsAirplaneMode && simNum > 0 && isIdle && !isAllRadioOff()) {
                enabled = DEBUG;
            }
            Log.d(TAG, "updateState(), mIsAirplaneMode = " + this.mIsAirplaneMode + ", simNum = " + simNum + ", callstate = " + callState + ", isIdle = " + isIdle);
        }
        this.mApnSettingsEnabled = enabled;
        Log.d(TAG, "updateState(), mApnSettingsEnabled = " + this.mApnSettingsEnabled);
        updateStateResources();
        refreshState();
    }

    private final void updateStateResources() {
        if (this.mApnSettingsEnabled) {
            this.mApnStateLabel = PluginManager.getQuickSettingsPlugin(this.mContext).customizeApnSettingsTile(this.mApnSettingsEnabled, this.mEnableApnStateIconWrapper, this.mApnStateLabel);
        } else {
            this.mApnStateLabel = PluginManager.getQuickSettingsPlugin(this.mContext).customizeApnSettingsTile(this.mApnSettingsEnabled, this.mDisableApnStateIconWrapper, this.mApnStateLabel);
        }
    }

    private boolean isAllRadioOff() {
        int[] subIds = this.mSubscriptionManager.getActiveSubscriptionIdList();
        if (subIds == null || subIds.length <= 0) {
            return DEBUG;
        }
        for (int subId : subIds) {
            if (SIMHelper.isRadioOn(subId)) {
                return false;
            }
        }
        return DEBUG;
    }
}
