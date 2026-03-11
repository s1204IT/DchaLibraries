package com.mediatek.settings;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import com.android.settings.R;
import com.mediatek.usp.UspManager;
import java.util.Map;

public class PhoneConfigurationSettings extends ListPreference implements Preference.OnPreferenceChangeListener {
    private AlertDialog mCarrierConfigAlertDialog;
    private Context mContext;
    private BroadcastReceiver mIntentReceiver;
    private String mSelectedPhoneConfig;
    private UspManager mUspManager;

    public PhoneConfigurationSettings(Context context) {
        this(context, null);
    }

    public PhoneConfigurationSettings(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                Log.d("PhoneConfigurationSettings", "onReceive():" + action);
                if (!action.equals("android.intent.action.PHONE_STATE")) {
                    return;
                }
                boolean isCallStateIdle = !TelecomManager.from(context2).isInCall();
                Log.d("PhoneConfigurationSettings", "isCallStateIdle:" + isCallStateIdle);
                PhoneConfigurationSettings.this.setEnabled(isCallStateIdle);
            }
        };
        this.mContext = context;
        this.mUspManager = (UspManager) this.mContext.getSystemService("uniservice-pack");
        setTitle(R.string.preferred_phone_configuration_title);
        setDialogTitle(R.string.preferred_phone_configuration_dialogtitle);
        setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        this.mSelectedPhoneConfig = (String) newValue;
        if (getValue().equals(this.mSelectedPhoneConfig)) {
            Log.d("PhoneConfigurationSettings", "onPreferenceChange: pref not changed, so return:" + this.mSelectedPhoneConfig);
            return true;
        }
        Log.d("PhoneConfigurationSettings", "onPreferenceChange: set mSelectedPhoneConfig " + this.mSelectedPhoneConfig);
        String message = this.mContext.getResources().getString(R.string.preferred_phone_configuration_dialog_message, getEntries()[findIndexOfValue(this.mSelectedPhoneConfig)]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
        builder.setMessage(message).setIconAttribute(android.R.attr.alertDialogIcon).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PhoneConfigurationSettings.this.handleCarrierConfigChange();
            }
        }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PhoneConfigurationSettings.this.setValue(PhoneConfigurationSettings.this.mUspManager.getActiveOpPack());
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                PhoneConfigurationSettings.this.setValue(PhoneConfigurationSettings.this.mUspManager.getActiveOpPack());
            }
        });
        this.mCarrierConfigAlertDialog = builder.show();
        return true;
    }

    public void initPreference() {
        Map<String, String> opList = this.mUspManager.getAllOpPackList();
        Log.d("PhoneConfigurationSettings", "opList:" + opList);
        if (opList != null) {
            TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            String mccMnc = telephonyManager.getSimOperatorNumericForPhone(SubscriptionManager.getDefaultVoicePhoneId());
            String currentCarrierId = this.mUspManager.getOpPackFromSimInfo(mccMnc);
            Log.d("PhoneConfigurationSettings", "mccMnc:" + mccMnc + ",currentCarrierId:" + currentCarrierId);
            String selectedOpPackId = this.mUspManager.getActiveOpPack();
            Log.d("PhoneConfigurationSettings", "selectedOpPackId: " + selectedOpPackId);
            int size = (selectedOpPackId == null || selectedOpPackId.isEmpty()) ? opList.size() + 1 : opList.size();
            CharSequence[] choices = new CharSequence[size];
            CharSequence[] values = new CharSequence[size];
            int index = 0;
            for (Map.Entry<String, String> pair : opList.entrySet()) {
                String choice = pair.getValue();
                if (currentCarrierId != null && currentCarrierId.equals(pair.getKey())) {
                    choice = choice + this.mContext.getResources().getString(R.string.recommended);
                }
                values[index] = pair.getKey();
                choices[index] = choice;
                Log.d("PhoneConfigurationSettings", "value[" + index + "]: " + values[index] + "-->Choice[" + index + "]: " + choices[index]);
                index++;
            }
            if (selectedOpPackId == null || selectedOpPackId.isEmpty()) {
                values[index] = "00";
                choices[index] = this.mContext.getResources().getString(R.string.om_package_name);
                selectedOpPackId = "00";
            }
            setEntries(choices);
            setEntryValues(values);
            setValue(selectedOpPackId);
            setSummary(getEntry());
            boolean isCallStateIdle = !TelecomManager.from(this.mContext).isInCall();
            Log.d("PhoneConfigurationSettings", "isCallStateIdle:" + isCallStateIdle);
            setEnabled(isCallStateIdle);
        } else {
            setEnabled(false);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PHONE_STATE");
        this.mContext.registerReceiver(this.mIntentReceiver, filter);
    }

    public void deinitPreference() {
        this.mContext.unregisterReceiver(this.mIntentReceiver);
    }

    public void handleCarrierConfigChange() {
        Log.d("PhoneConfigurationSettings", "new value:" + this.mSelectedPhoneConfig);
        setValue(this.mSelectedPhoneConfig);
        Log.d("PhoneConfigurationSettings", "entry:" + getEntry());
        Log.d("PhoneConfigurationSettings", "value:" + getValue());
        this.mUspManager.setOpPackActive(getValue().toString());
        setSummary(getEntry());
    }
}
