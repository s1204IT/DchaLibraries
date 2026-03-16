package com.android.settings.sim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;

public class SimEnabler implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private final Context mContext;
    private boolean mCurEnabled;
    private final String mEnableTitlePrefix;
    private PhoneStateListener mPhoneStateListener;
    private final SwitchPreference mSimEnablePref;
    private final int mSimId;
    private boolean mSwitchEnabled;
    private TelephonyManager mTelephonyManager;
    private final Preference mUnlockPref;
    private String mTag = "SimEnabler";
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                boolean airplaneEnabled = Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
                Log.d(SimEnabler.this.mTag, "airplaneEnabled: " + airplaneEnabled);
                SimEnabler.this.mSwitchEnabled = Dsds.hasIcc(SimEnabler.this.mSimId) && !airplaneEnabled;
                SimEnabler.this.mSimEnablePref.setEnabled(SimEnabler.this.mSwitchEnabled);
            }
        }
    };

    private String getCurrentNetworkName() {
        TelephonyManager tm = TelephonyManager.from(this.mContext);
        int[] sub = SubscriptionManager.getSubId(this.mSimId);
        if (sub == null || !SubscriptionManager.isValidSubscriptionId(sub[0])) {
            return "";
        }
        int actualDataNetworkType = tm.getDataNetworkType(sub[0]);
        int actualVoiceNetworkType = tm.getVoiceNetworkType(sub[0]);
        if (actualDataNetworkType != 0) {
            String networktype = TelephonyManager.getNetworkTypeName(actualDataNetworkType);
            return networktype;
        }
        if (actualVoiceNetworkType == 0) {
            return "";
        }
        String networktype2 = TelephonyManager.getNetworkTypeName(actualVoiceNetworkType);
        return networktype2;
    }

    private String getNetworkHint() {
        TelephonyManager tm = TelephonyManager.from(this.mContext);
        int[] sub = SubscriptionManager.getSubId(this.mSimId);
        if (sub == null || !SubscriptionManager.isValidSubscriptionId(sub[0])) {
            return "";
        }
        String ret = tm.getNetworkOperatorName(sub[0]);
        if (!TextUtils.isEmpty(ret)) {
            String ret2 = ": " + ret;
            String networkName = getCurrentNetworkName();
            if (!"UNKNOWN".equals(networkName)) {
                return ret2 + " - " + networkName;
            }
            return ret2;
        }
        return ret;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == this.mSimEnablePref) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setClassName("com.android.phone", "com.android.phone.MobileNetworkSettings");
            intent.putExtra("phone", this.mSimId);
            this.mContext.startActivity(intent);
            return true;
        }
        Log.e(this.mTag, "Unexpected preference = " + preference);
        return true;
    }

    static String getSimEnableKey(int simId) {
        return "toggle_sim" + (simId + 1);
    }

    static String getSimUnlockKey(int simId) {
        return "enter_pin_sim" + (simId + 1);
    }

    public SimEnabler(Context context, SwitchPreference switchPref, Preference unlockPref, int simId) {
        this.mContext = context;
        this.mSimId = simId;
        this.mTag += (this.mSimId + 1);
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mSimEnablePref = switchPref;
        this.mSimEnablePref.setKey(getSimEnableKey(this.mSimId));
        this.mSimEnablePref.setPersistent(false);
        this.mSimEnablePref.setDisableDependentsState(false);
        this.mSimEnablePref.setPersistent(false);
        this.mEnableTitlePrefix = this.mContext.getString(R.string.enable_sim) + (this.mSimId + 1);
        this.mSimEnablePref.setTitle(this.mEnableTitlePrefix + getNetworkHint());
        this.mSimEnablePref.setOnPreferenceChangeListener(this);
        this.mUnlockPref = unlockPref;
        if (this.mUnlockPref != null) {
            this.mUnlockPref.setKey(getSimUnlockKey(this.mSimId));
            String title = this.mContext.getString(R.string.unlock_sim_pin) + (this.mSimId + 1);
            this.mUnlockPref.setTitle(title);
            this.mUnlockPref.setSummary(this.mContext.getString(R.string.unlock_sim_pin_summary));
        }
        int[] subIds = SubscriptionManager.getSubId(this.mSimId);
        if (subIds != null) {
            this.mPhoneStateListener = new PhoneStateListener(subIds[0]) {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    SimEnabler.this.onSimEnableChanged(serviceState);
                }
            };
        }
        this.mCurEnabled = isSimEnabled();
    }

    private void updateSimUnlockPref() {
        if (this.mUnlockPref != null) {
            TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
            this.mUnlockPref.setEnabled(false);
            int state = tm.getSimState(this.mSimId);
            if (state == 2 || state == 3) {
                this.mUnlockPref.setEnabled(true);
            }
        }
    }

    public void resume() {
        boolean z = false;
        boolean simEnabled = isSimEnabled();
        this.mSimEnablePref.setEnabled(false);
        this.mSimEnablePref.setChecked(simEnabled);
        this.mSimEnablePref.setTitle(this.mEnableTitlePrefix + getNetworkHint());
        this.mSimEnablePref.setSummary(simEnabled ? null : this.mContext.getString(R.string.enable_sim_summary));
        boolean airplaneEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0;
        if (Dsds.hasIcc(this.mSimId) && !airplaneEnabled) {
            z = true;
        }
        this.mSwitchEnabled = z;
        this.mSimEnablePref.setEnabled(this.mSwitchEnabled);
        this.mCurEnabled = isSimEnabled();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
        this.mSimEnablePref.setOnPreferenceClickListener(this);
        if (this.mUnlockPref != null) {
            this.mUnlockPref.setOnPreferenceClickListener(this);
            updateSimUnlockPref();
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    public void pause() {
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        if (this.mUnlockPref != null) {
            this.mUnlockPref.setOnPreferenceClickListener(null);
        }
        this.mSimEnablePref.setOnPreferenceClickListener(null);
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private boolean isSimEnabled() {
        return this.mSimId == PhoneConstants.SimId.SIM1.ordinal() ? Settings.Global.getInt(this.mContext.getContentResolver(), "enable_sim1", 1) != 0 : Settings.Global.getInt(this.mContext.getContentResolver(), "enable_sim2", 1) != 0;
    }

    private void enableSim(boolean enabling) {
        this.mCurEnabled = enabling;
        this.mSimEnablePref.setSummary(enabling ? R.string.enabling_sim : R.string.disabling_sim);
        if (PhoneConstants.SimId.SIM1.ordinal() == this.mSimId) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "enable_sim1", enabling ? 1 : 0);
        } else {
            Settings.Global.putInt(this.mContext.getContentResolver(), "enable_sim2", enabling ? 1 : 0);
        }
        Intent intent = new Intent("android.intent.action.SIM_ENABLE_CHANGED");
        intent.putExtra("state", enabling);
        intent.putExtra("phone", this.mSimId);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void onSimEnableChanged(ServiceState serviceState) {
        int state = serviceState.getState();
        Log.d(this.mTag, "onSimEnableChanged, state: " + state);
        this.mSimEnablePref.setTitle(this.mEnableTitlePrefix + getNetworkHint());
        if (state == 0) {
            if (this.mCurEnabled) {
                this.mSimEnablePref.setChecked(true);
                this.mSimEnablePref.setSummary((CharSequence) null);
                this.mSimEnablePref.setEnabled(this.mSwitchEnabled);
                return;
            }
            return;
        }
        if (state == 3 && !this.mCurEnabled) {
            this.mSimEnablePref.setChecked(false);
            this.mSimEnablePref.setSummary(this.mContext.getString(R.string.enable_sim_summary));
            this.mSimEnablePref.setEnabled(this.mSwitchEnabled);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        this.mSimEnablePref.setEnabled(false);
        enableSim(((Boolean) newValue).booleanValue());
        return true;
    }
}
