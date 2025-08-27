package com.mediatek.settings.advancedcalling;

import android.R;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.ims.internal.MtkImsManagerEx;
import com.mediatek.settings.sim.TelephonyUtils;

/* loaded from: classes.dex */
public class AdvancedWifiCallingSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener {
    private static boolean sSwitchFlag = true;
    private ListPreference mButtonRoaming;
    private Preference mButtonUpdateECC;
    private Context mContext;
    private IntentFilter mIntentFilter;
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private boolean mValidListener = false;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() { // from class: com.mediatek.settings.advancedcalling.AdvancedWifiCallingSettings.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("OP12AdvancedWifiCallingSettings", "onReceive()... " + action);
            if (action.equals("com.android.ims.REGISTRATION_ERROR")) {
                Log.d("OP12AdvancedWifiCallingSettings", "IMS Registration error, disable WFC Switch");
                setResultCode(0);
                AdvancedWifiCallingSettings.this.mSwitch.setChecked(false);
                AdvancedWifiCallingSettings.this.showAlert(intent);
                return;
            }
            if (action.equals("com.android.intent.action.IMS_CONFIG_CHANGED")) {
                Log.d("OP12AdvancedWifiCallingSettings", "config changed, finish WFC activity");
                AdvancedWifiCallingSettings.this.getActivity().finish();
            } else if (action.equals("android.intent.action.PHONE_STATE")) {
                Log.d("OP12AdvancedWifiCallingSettings", "Phone state changed, so update the screen");
                AdvancedWifiCallingSettings.this.updateScreen();
            }
        }
    };

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitch = this.mSwitchBar.getSwitch();
        this.mSwitchBar.show();
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
    }

    private void showAlert(Intent intent) {
        CharSequence charSequenceExtra = intent.getCharSequenceExtra("alertTitle");
        CharSequence charSequenceExtra2 = intent.getCharSequenceExtra("alertMessage");
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
        builder.setMessage(charSequenceExtra2).setTitle(charSequenceExtra).setIcon(R.drawable.ic_dialog_alert).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null);
        builder.create().show();
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 105;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(com.android.settings.R.xml.advanced_wificalling_settings);
        this.mContext = getActivity();
        this.mButtonUpdateECC = findPreference("update_emergency_address_key");
        this.mButtonRoaming = (ListPreference) findPreference("roaming_mode");
        this.mButtonRoaming.setOnPreferenceChangeListener(this);
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("com.android.ims.REGISTRATION_ERROR");
        this.mIntentFilter.addAction("android.intent.action.PHONE_STATE");
        this.mIntentFilter.addAction("com.android.intent.action.IMS_CONFIG_CHANGED");
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        if (ImsManager.isWfcEnabledByPlatform(this.mContext)) {
            this.mSwitchBar.addOnSwitchChangeListener(this);
            this.mValidListener = true;
        }
        boolean zIsWfcEnabledByUser = ImsManager.isWfcEnabledByUser(this.mContext);
        this.mSwitch.setChecked(zIsWfcEnabledByUser);
        this.mButtonUpdateECC.setEnabled(zIsWfcEnabledByUser);
        this.mButtonRoaming.setEnabled(zIsWfcEnabledByUser);
        int wfcMode = MtkImsManager.getWfcMode(this.mContext, true, 0);
        Log.d("OP12AdvancedWifiCallingSettings", "WFC RoamingMode : " + wfcMode);
        this.mButtonRoaming.setValue(Integer.toString(wfcMode));
        updateScreen();
        this.mContext.registerReceiver(this.mIntentReceiver, this.mIntentFilter);
        Intent intent = getActivity().getIntent();
        if (intent.getBooleanExtra("alertShow", false)) {
            showAlert(intent);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        if (this.mValidListener) {
            this.mValidListener = false;
            this.mSwitchBar.removeOnSwitchChangeListener(this);
        }
        this.mContext.unregisterReceiver(this.mIntentReceiver);
    }

    @Override // com.android.settings.widget.SwitchBar.OnSwitchChangeListener
    public void onSwitchChanged(final Switch r6, boolean z) {
        int defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
        final int phoneId = SubscriptionManager.getPhoneId(defaultSubscriptionId);
        Log.d("OP12AdvancedWifiCallingSettings", "OnSwitchChanged, subId :" + defaultSubscriptionId + " phoneId :" + phoneId);
        if (isInSwitchProcess()) {
            Log.d("OP12AdvancedWifiCallingSettings", "[onClick] Switching process ongoing");
            Toast.makeText(getActivity(), com.android.settings.R.string.Switch_not_in_use_string, 0).show();
            this.mSwitch.setChecked(!z);
            return;
        }
        if (z) {
            Log.d("OP12AdvancedWifiCallingSettings", "Wifi Switch checked");
            MtkImsManager.setWfcSetting(this.mContext, z, phoneId);
            this.mButtonUpdateECC.setEnabled(z);
            this.mButtonRoaming.setEnabled(z);
            Log.d("OP12AdvancedWifiCallingSettings", "Wifi Calling ON");
            sSwitchFlag = true;
            if (Settings.Global.getInt(getContentResolver(), "wifi_sleep_policy", 2) != 2) {
                Settings.Global.putInt(getContentResolver(), "wifi_sleep_policy", 2);
                AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
                builder.setCancelable(false);
                builder.setMessage(this.mContext.getString(com.android.settings.R.string.wifi_sleep_policy_msg));
                builder.setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null);
                builder.create().show();
                return;
            }
            return;
        }
        Log.d("OP12AdvancedWifiCallingSettings", "Wifi Switch Unchecked");
        if (sSwitchFlag) {
            r6.setChecked(true);
            this.mSwitchBar.setTextViewLabelAndBackground(true);
            AlertDialog.Builder builder2 = new AlertDialog.Builder(this.mContext);
            builder2.setCancelable(false);
            builder2.setMessage(this.mContext.getString(com.android.settings.R.string.advance_wifi_calling_disable_msg));
            builder2.setPositiveButton(com.android.settings.R.string.turn_off_wifi_calling, new DialogInterface.OnClickListener() { // from class: com.mediatek.settings.advancedcalling.AdvancedWifiCallingSettings.2
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    boolean unused = AdvancedWifiCallingSettings.sSwitchFlag = false;
                    r6.setChecked(false);
                    AdvancedWifiCallingSettings.this.mSwitchBar.setTextViewLabelAndBackground(false);
                    MtkImsManager.setWfcSetting(AdvancedWifiCallingSettings.this.mContext, false, phoneId);
                    AdvancedWifiCallingSettings.this.mButtonUpdateECC.setEnabled(false);
                    AdvancedWifiCallingSettings.this.mButtonRoaming.setEnabled(false);
                    Log.d("OP12AdvancedWifiCallingSettings", "Wifi Calling OFF");
                }
            });
            builder2.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.mediatek.settings.advancedcalling.AdvancedWifiCallingSettings.3
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    boolean unused = AdvancedWifiCallingSettings.sSwitchFlag = true;
                }
            });
            builder2.create().show();
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mButtonRoaming) {
            String str = (String) obj;
            this.mButtonRoaming.setValue(str);
            int iIntValue = Integer.valueOf(str).intValue();
            int wfcMode = MtkImsManager.getWfcMode(this.mContext, true, 0);
            Log.d("OP12AdvancedWifiCallingSettings", "onPreferenceChange, buttonMode: " + iIntValue + "\ncurrentMode: " + wfcMode);
            if (iIntValue != wfcMode) {
                MtkImsManager.setWfcMode(this.mContext, iIntValue, true, 0);
                Log.d("OP12AdvancedWifiCallingSettings", "set WFC Roaming mode : " + iIntValue);
            }
        }
        return true;
    }

    private boolean isInSwitchProcess() {
        try {
            int imsState = MtkImsManagerEx.getInstance().getImsState(TelephonyUtils.getMainCapabilityPhoneId());
            Log.d("OP12AdvancedWifiCallingSettings", "isInSwitchProcess , imsState = " + imsState);
            return imsState == 3 || imsState == 2;
        } catch (ImsException e) {
            return false;
        }
    }

    private void updateScreen() {
        SettingsActivity settingsActivity = (SettingsActivity) getActivity();
        if (settingsActivity == null) {
            return;
        }
        boolean zIsChecked = this.mSwitchBar.getSwitch().isChecked();
        boolean z = !TelecomManager.from(settingsActivity).isInCall();
        Log.d("OP12AdvancedWifiCallingSettings", "updateScreen: isWfcEnabled: " + zIsChecked + ", isCallStateIdle: " + z);
        this.mSwitchBar.setEnabled(z);
        this.mButtonUpdateECC.setEnabled(zIsChecked && z);
        this.mButtonRoaming.setEnabled(zIsChecked && z);
    }
}
