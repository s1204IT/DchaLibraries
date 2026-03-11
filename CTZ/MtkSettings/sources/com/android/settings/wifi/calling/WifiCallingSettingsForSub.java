package com.android.settings.wifi.calling;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.widget.SwitchBar;
import com.mediatek.ims.internal.MtkImsManagerEx;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWfcSettingsExt;
import com.mediatek.settings.sim.SimHotSwapHandler;

public class WifiCallingSettingsForSub extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener {
    private ListPreference mButtonWfcMode;
    private ListPreference mButtonWfcRoamingMode;
    private TextView mEmptyView;
    private ImsManager mImsManager;
    private IntentFilter mIntentFilter;
    private boolean mRemoveWfcPreferenceMode;
    private SimHotSwapHandler mSimHotSwapHandler;
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private TelephonyManager mTelephonyManager;
    private Preference mUpdateAddress;
    IWfcSettingsExt mWfcExt;
    private boolean mValidListener = false;
    private boolean mEditableWfcMode = true;
    private boolean mEditableWfcRoamingMode = true;
    private int mSubId = -1;
    private boolean mAlertAlreadyShowed = false;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int i, String str) {
            boolean z;
            boolean z2;
            PersistableBundle configForSubId;
            SettingsActivity settingsActivity = (SettingsActivity) WifiCallingSettingsForSub.this.getActivity();
            boolean zIsNonTtyOrTtyOnVolteEnabled = WifiCallingSettingsForSub.this.mImsManager.isNonTtyOrTtyOnVolteEnabled();
            boolean z3 = false;
            boolean z4 = WifiCallingSettingsForSub.this.mSwitchBar.isChecked() && zIsNonTtyOrTtyOnVolteEnabled;
            WifiCallingSettingsForSub.this.mSwitchBar.setEnabled(i == 0 && zIsNonTtyOrTtyOnVolteEnabled);
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager) settingsActivity.getSystemService("carrier_config");
            if (carrierConfigManager != null && (configForSubId = carrierConfigManager.getConfigForSubId(this.mSubId.intValue())) != null) {
                z2 = configForSubId.getBoolean("editable_wfc_mode_bool");
                z = configForSubId.getBoolean("editable_wfc_roaming_mode_bool");
            } else {
                z = false;
                z2 = true;
            }
            Preference preferenceFindPreference = WifiCallingSettingsForSub.this.getPreferenceScreen().findPreference("wifi_calling_mode");
            if (preferenceFindPreference != null) {
                preferenceFindPreference.setEnabled(z4 && z2 && i == 0);
            }
            Preference preferenceFindPreference2 = WifiCallingSettingsForSub.this.getPreferenceScreen().findPreference("wifi_calling_roaming_mode");
            if (preferenceFindPreference2 != null) {
                if (z4 && z && i == 0) {
                    z3 = true;
                }
                preferenceFindPreference2.setEnabled(z3);
            }
        }
    };
    private final Preference.OnPreferenceClickListener mUpdateAddressListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent carrierActivityIntent = WifiCallingSettingsForSub.this.getCarrierActivityIntent();
            if (carrierActivityIntent != null) {
                carrierActivityIntent.putExtra("EXTRA_LAUNCH_CARRIER_APP", 1);
                WifiCallingSettingsForSub.this.startActivity(carrierActivityIntent);
            }
            return true;
        }
    };
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("WifiCallingForSub", "onReceive, action=" + action);
            if (action.equals("com.android.ims.REGISTRATION_ERROR")) {
                setResultCode(0);
                WifiCallingSettingsForSub.this.showAlert(intent);
            } else if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                int intExtra = intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1);
                int phoneId = SubscriptionManager.getPhoneId(WifiCallingSettingsForSub.this.mSubId);
                if (intExtra != -1 && intExtra == phoneId && !WifiCallingSettingsForSub.this.mImsManager.isWfcEnabledByPlatform()) {
                    Log.d("WifiCallingForSub", "Carrier config changed, finish WFC activity");
                    WifiCallingSettingsForSub.this.getActivity().finish();
                }
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        SettingsActivity settingsActivity = (SettingsActivity) getActivity();
        this.mEmptyView = (TextView) getView().findViewById(R.id.empty);
        setEmptyView(this.mEmptyView);
        this.mEmptyView.setText(settingsActivity.getString(com.android.settings.R.string.wifi_calling_off_explanation) + settingsActivity.getString(com.android.settings.R.string.wifi_calling_off_explanation_2));
        this.mSwitchBar = (SwitchBar) getView().findViewById(com.android.settings.R.id.switch_bar);
        this.mSwitchBar.show();
        this.mSwitch = this.mSwitchBar.getSwitch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
    }

    public void showAlert(Intent intent) {
        Activity activity = getActivity();
        CharSequence charSequenceExtra = intent.getCharSequenceExtra("alertTitle");
        CharSequence charSequenceExtra2 = intent.getCharSequenceExtra("alertMessage");
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(charSequenceExtra2).setTitle(charSequenceExtra).setIcon(R.drawable.ic_dialog_alert).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null);
        builder.create().show();
    }

    @Override
    public int getMetricsCategory() {
        return 1230;
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(com.android.settings.R.xml.wifi_calling_settings);
        this.mWfcExt = UtilsExt.getWfcSettingsExt(getActivity());
        this.mWfcExt.initPlugin(this);
        if (getArguments() != null && getArguments().containsKey("subId")) {
            this.mSubId = getArguments().getInt("subId");
        } else if (bundle != null) {
            this.mSubId = bundle.getInt("subId", -1);
        }
        this.mImsManager = ImsManager.getInstance(getActivity(), SubscriptionManager.getPhoneId(this.mSubId));
        this.mButtonWfcMode = (ListPreference) findPreference("wifi_calling_mode");
        this.mButtonWfcMode.setOnPreferenceChangeListener(this);
        this.mButtonWfcRoamingMode = (ListPreference) findPreference("wifi_calling_roaming_mode");
        this.mButtonWfcRoamingMode.setOnPreferenceChangeListener(this);
        this.mUpdateAddress = findPreference("emergency_address_key");
        this.mUpdateAddress.setOnPreferenceClickListener(this.mUpdateAddressListener);
        this.mWfcExt.addOtherCustomPreference();
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("com.android.ims.REGISTRATION_ERROR");
        this.mIntentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        this.mTelephonyManager = TelephonyManager.from(getActivity()).createForSubscriptionId(this.mSubId);
        this.mSimHotSwapHandler = new SimHotSwapHandler(getActivity());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d("WifiCallingForSub", "onSimHotSwap, finish Activity.");
                WifiCallingSettingsForSub.this.getActivity().finish();
            }
        });
        this.mWfcExt.onWfcSettingsEvent(2);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putInt("subId", this.mSubId);
        super.onSaveInstanceState(bundle);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(com.android.settings.R.layout.wifi_calling_settings_preferences, viewGroup, false);
        ViewGroup viewGroup2 = (ViewGroup) viewInflate.findViewById(com.android.settings.R.id.prefs_container);
        Utils.prepareCustomPreferencesList(viewGroup, viewInflate, viewGroup2, false);
        viewGroup2.addView(super.onCreateView(layoutInflater, viewGroup2, bundle));
        return viewInflate;
    }

    private void updateBody() {
        boolean z;
        boolean z2;
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) getSystemService("carrier_config");
        if (carrierConfigManager != null && (configForSubId = carrierConfigManager.getConfigForSubId(this.mSubId)) != null) {
            this.mEditableWfcMode = configForSubId.getBoolean("editable_wfc_mode_bool");
            this.mEditableWfcRoamingMode = configForSubId.getBoolean("editable_wfc_roaming_mode_bool");
            z2 = configForSubId.getBoolean("carrier_wfc_supports_wifi_only_bool", true);
            this.mRemoveWfcPreferenceMode = configForSubId.getBoolean("mtk_wfc_remove_preference_mode_bool", false);
            Log.d("WifiCallingForSub", "updateBody, removeWfcPreferenceMode=" + this.mRemoveWfcPreferenceMode);
            z = configForSubId.getBoolean("mtk_key_update_wificalling_by_tty", false);
            Log.d("WifiCallingForSub", "[TTY]WFC disableWifiCalling" + z);
        } else {
            z = false;
            z2 = true;
        }
        if (!z2) {
            this.mButtonWfcMode.setEntries(com.android.settings.R.array.wifi_calling_mode_choices_without_wifi_only);
            this.mButtonWfcMode.setEntryValues(com.android.settings.R.array.wifi_calling_mode_values_without_wifi_only);
            this.mButtonWfcRoamingMode.setEntries(com.android.settings.R.array.wifi_calling_mode_choices_v2_without_wifi_only);
            this.mButtonWfcRoamingMode.setEntryValues(com.android.settings.R.array.wifi_calling_mode_values_without_wifi_only);
        }
        boolean z3 = this.mImsManager.isWfcEnabledByUser() && this.mImsManager.isNonTtyOrTtyOnVolteEnabled();
        this.mSwitch.setChecked(z3);
        int wfcMode = this.mImsManager.getWfcMode(false);
        int wfcMode2 = this.mImsManager.getWfcMode(true);
        this.mButtonWfcMode.setValue(Integer.toString(wfcMode));
        this.mButtonWfcRoamingMode.setValue(Integer.toString(wfcMode2));
        updateButtonWfcMode(z3, wfcMode, wfcMode2);
        updateEnabledState();
        boolean zIsNonTtyOrTtyOnVolteEnabled = this.mImsManager.isNonTtyOrTtyOnVolteEnabled();
        Log.d("WifiCallingForSub", "[TTY]isNonTtyOrTtyOnVolteEnabled :" + zIsNonTtyOrTtyOnVolteEnabled + " wfcEnabled :" + z3);
        if (!zIsNonTtyOrTtyOnVolteEnabled && z && !z3) {
            Activity activity = getActivity();
            Toast.makeText(activity, activity.getString(com.android.settings.R.string.tty_wfc_disable_wfc_setting_message), 1).show();
        }
        this.mWfcExt.updateWfcModePreference(getPreferenceScreen(), this.mButtonWfcMode, z3, wfcMode);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("WifiCallingForSub", "onResume");
        Activity activity = getActivity();
        updateBody();
        if (this.mImsManager.isWfcEnabledByPlatform()) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
            this.mSwitchBar.addOnSwitchChangeListener(this);
            this.mValidListener = true;
        }
        activity.registerReceiver(this.mIntentReceiver, this.mIntentFilter);
        Intent intent = getActivity().getIntent();
        if (intent.getBooleanExtra("alertShow", false) && !this.mAlertAlreadyShowed) {
            showAlert(intent);
            this.mAlertAlreadyShowed = true;
        }
        this.mWfcExt.onWfcSettingsEvent(0);
    }

    @Override
    public void onPause() {
        super.onPause();
        Activity activity = getActivity();
        if (this.mValidListener) {
            this.mValidListener = false;
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
            this.mSwitchBar.removeOnSwitchChangeListener(this);
        }
        activity.unregisterReceiver(this.mIntentReceiver);
        this.mWfcExt.onWfcSettingsEvent(1);
    }

    @Override
    public void onDestroy() {
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        this.mWfcExt.onWfcSettingsEvent(3);
        super.onDestroy();
    }

    @Override
    public void onSwitchChanged(Switch r4, boolean z) {
        Log.d("WifiCallingForSub", "onSwitchChanged(" + z + ")");
        if (isInSwitchProcess()) {
            Log.d("WifiCallingForSub", "onSwitchChanged, switching process is ongoing.");
            Toast.makeText(getActivity(), com.android.settings.R.string.Switch_not_in_use_string, 0).show();
            this.mSwitchBar.setChecked(!z);
        } else {
            if (!z) {
                updateWfcMode(false);
                return;
            }
            Intent carrierActivityIntent = getCarrierActivityIntent();
            if (carrierActivityIntent != null) {
                carrierActivityIntent.putExtra("EXTRA_LAUNCH_CARRIER_APP", 0);
                startActivityForResult(carrierActivityIntent, 1);
            } else {
                updateWfcMode(true);
            }
        }
    }

    public Intent getCarrierActivityIntent() {
        PersistableBundle configForSubId;
        ComponentName componentNameUnflattenFromString;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) getActivity().getSystemService(CarrierConfigManager.class);
        if (carrierConfigManager == null || (configForSubId = carrierConfigManager.getConfigForSubId(this.mSubId)) == null) {
            return null;
        }
        String string = configForSubId.getString("wfc_emergency_address_carrier_app_string");
        if (TextUtils.isEmpty(string) || (componentNameUnflattenFromString = ComponentName.unflattenFromString(string)) == null || !isPackageExist(getActivity(), componentNameUnflattenFromString)) {
            return null;
        }
        Intent intent = new Intent();
        intent.setComponent(componentNameUnflattenFromString);
        return intent;
    }

    private void updateWfcMode(boolean z) {
        Log.i("WifiCallingForSub", "updateWfcMode(" + z + ")");
        this.mImsManager.setWfcSetting(z);
        int wfcMode = this.mImsManager.getWfcMode(false);
        updateButtonWfcMode(z, wfcMode, this.mImsManager.getWfcMode(true));
        this.mWfcExt.updateWfcModePreference(getPreferenceScreen(), this.mButtonWfcMode, z, wfcMode);
        if (z) {
            this.mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), wfcMode);
        } else {
            this.mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), -1);
        }
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        getActivity();
        if (i == 1) {
            Log.d("WifiCallingForSub", "WFC emergency address activity result = " + i2);
            if (i2 == -1) {
                updateWfcMode(true);
            }
        }
    }

    private void updateButtonWfcMode(boolean z, int i, int i2) {
        Log.d("WifiCallingForSub", "updateButtonWfcMode, wfcEnabled=" + z);
        this.mButtonWfcMode.setSummary(getWfcModeSummary(i));
        this.mButtonWfcMode.setEnabled(z && this.mEditableWfcMode);
        this.mButtonWfcRoamingMode.setEnabled(z && this.mEditableWfcRoamingMode);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        boolean z2 = getCarrierActivityIntent() != null;
        if (z) {
            if (this.mEditableWfcMode) {
                preferenceScreen.addPreference(this.mButtonWfcMode);
            } else {
                preferenceScreen.removePreference(this.mButtonWfcMode);
            }
            if (this.mEditableWfcRoamingMode) {
                preferenceScreen.addPreference(this.mButtonWfcRoamingMode);
            } else {
                preferenceScreen.removePreference(this.mButtonWfcRoamingMode);
            }
            if (z2) {
                preferenceScreen.addPreference(this.mUpdateAddress);
            } else {
                preferenceScreen.removePreference(this.mUpdateAddress);
            }
            if (this.mRemoveWfcPreferenceMode) {
                preferenceScreen.removePreference(this.mButtonWfcMode);
                return;
            }
            return;
        }
        preferenceScreen.removePreference(this.mButtonWfcMode);
        preferenceScreen.removePreference(this.mButtonWfcRoamingMode);
        preferenceScreen.removePreference(this.mUpdateAddress);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mButtonWfcMode) {
            Log.d("WifiCallingForSub", "onPreferenceChange mButtonWfcMode " + obj);
            String str = (String) obj;
            this.mButtonWfcMode.setValue(str);
            int iIntValue = Integer.valueOf(str).intValue();
            if (iIntValue != this.mImsManager.getWfcMode(false)) {
                this.mImsManager.setWfcMode(iIntValue, false);
                this.mButtonWfcMode.setSummary(getWfcModeSummary(iIntValue));
                this.mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), iIntValue);
            }
            if (!this.mEditableWfcRoamingMode && iIntValue != this.mImsManager.getWfcMode(true)) {
                this.mImsManager.setWfcMode(iIntValue, true);
            }
        } else if (preference == this.mButtonWfcRoamingMode) {
            String str2 = (String) obj;
            this.mButtonWfcRoamingMode.setValue(str2);
            int iIntValue2 = Integer.valueOf(str2).intValue();
            if (iIntValue2 != this.mImsManager.getWfcMode(true)) {
                this.mImsManager.setWfcMode(iIntValue2, true);
                this.mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), iIntValue2);
            }
        }
        return true;
    }

    private int getWfcModeSummary(int i) {
        Log.d("WifiCallingForSub", "getWfcModeSummary, wfcMode=" + i);
        if (this.mImsManager.isWfcEnabledByUser()) {
            switch (i) {
                case 0:
                    return R.string.next_button_label;
                case 1:
                    return R.string.news_notification_channel_label;
                case 2:
                    return R.string.noApplications;
                default:
                    Log.e("WifiCallingForSub", "Unexpected WFC mode value: " + i);
                    break;
            }
        }
        return R.string.notification_channel_network_available;
    }

    private boolean isInSwitchProcess() {
        try {
            int imsState = MtkImsManagerEx.getInstance().getImsState(SubscriptionManager.getPhoneId(this.mSubId));
            Log.d("WifiCallingForSub", "isInSwitchProcess, imsState=" + imsState);
            return imsState == 3 || imsState == 2;
        } catch (ImsException e) {
            return false;
        }
    }

    private void updateEnabledState() {
        boolean zIsInCall = TelecomManager.from(getActivity()).isInCall();
        Log.d("WifiCallingForSub", "updateEnabledState, inCall=" + zIsInCall);
        if (zIsInCall) {
            this.mSwitchBar.setEnabled(false);
            this.mButtonWfcMode.setEnabled(false);
            this.mButtonWfcRoamingMode.setEnabled(false);
        }
    }

    private static boolean isPackageExist(Context context, ComponentName componentName) {
        try {
            context.getPackageManager().getActivityInfo(componentName, 0);
            Log.d("WifiCallingForSub", "package exists.");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("WifiCallingForSub", "package does not exist.");
            return false;
        }
    }
}
