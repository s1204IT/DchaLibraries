package com.mediatek.services.telephony;

import android.R;
import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.SubscriptionInfoHelper;

public class RoamingSettings extends PreferenceActivity {
    private static int sSubId;
    private Context mContext;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("RoamingSettings", "action: " + action);
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                RoamingSettings.this.finish();
                return;
            }
            if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                String stringExtra = intent.getStringExtra("ss");
                Log.d("RoamingSettings", "[CDMA]simStatus: " + stringExtra);
                if ("ABSENT".equals(stringExtra)) {
                    RoamingSettings.this.finish();
                    return;
                }
                return;
            }
            if (action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED") && intent.getIntExtra("simDetectStatus", 4) != 4) {
                RoamingSettings.this.finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        sSubId = getIntent().getIntExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, SubscriptionManager.getDefaultSubscriptionId());
        super.onCreate(bundle);
        getFragmentManager().beginTransaction().replace(R.id.content, new SettingsFragment()).commit();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        this.mContext = getApplicationContext();
        initIntentFilter();
        registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    public static class SettingsFragment extends PreferenceFragment {
        private RoamingSwitchPreference mDomesticButtonDataRoam;
        private RoamingSwitchPreference mDomesticButtonVoiceRoam;
        private MyHandler mHandler;
        private RoamingSwitchPreference mInternationalButtonDataRoam;
        private RoamingSwitchPreference mInternationalButtonVoiceRoam;
        private Integer[] mRoamingSettings = new Integer[6];
        Preference.OnPreferenceChangeListener mRoamingSettingsListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                Phone phone;
                boolean z;
                boolean z2;
                Boolean bool = (Boolean) obj;
                if (((SwitchPreference) preference).isChecked() == bool.booleanValue()) {
                    Log.i("RoamingSettings", preference.getKey() + " : " + String.valueOf(obj) + " not changed");
                    return false;
                }
                String key = preference.getKey();
                boolean zBooleanValue = bool.booleanValue();
                if (TelephonyManager.getDefault().getSimCount() == 1) {
                    Log.d("RoamingSettings", "simCount 1 and update roaming value");
                    phone = PhoneFactory.getDefaultPhone();
                } else {
                    phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(RoamingSettings.sSubId));
                    Log.d("RoamingSettings", "update for subId " + RoamingSettings.sSubId + "and update roaming value");
                }
                SettingsFragment.this.mRoamingSettings[0] = Integer.valueOf(SubscriptionManager.getPhoneId(RoamingSettings.sSubId));
                byte b = -1;
                int iHashCode = key.hashCode();
                if (iHashCode != -1845242659) {
                    if (iHashCode != -1524641507) {
                        if (iHashCode != -593383571) {
                            if (iHashCode == 44485261 && key.equals("international_data_roaming_settings")) {
                                b = 3;
                            }
                        } else if (key.equals("domestic_voice_text_roaming_settings")) {
                            b = 0;
                        }
                    } else if (key.equals("domestic_data_roaming_settings")) {
                        b = 1;
                    }
                } else if (key.equals("international_voice_text_roaming_settings")) {
                    b = 2;
                }
                switch (b) {
                    case 0:
                        Log.d("RoamingSettings", "pref: " + key + "new value: " + obj + " value: " + (zBooleanValue ? 1 : 0));
                        ContentResolver contentResolver = phone.getContext().getContentResolver();
                        StringBuilder sb = new StringBuilder();
                        sb.append("domestic_voice_text_roaming");
                        sb.append(RoamingSettings.sSubId);
                        Settings.Global.putInt(contentResolver, sb.toString(), zBooleanValue ? 1 : 0);
                        int i = Boolean.valueOf(SettingsFragment.this.mDomesticButtonDataRoam.isChecked()).booleanValue() ? 1 : 0;
                        if (zBooleanValue) {
                            SettingsFragment.this.mDomesticButtonDataRoam.setEnabled(true);
                            Log.d("RoamingSettings", "pref: DOMESTIC_DATA_ROAMING_SETTINGS " + obj + " value: " + i);
                            ContentResolver contentResolver2 = phone.getContext().getContentResolver();
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("domestic_data_roaming");
                            sb2.append(RoamingSettings.sSubId);
                            Settings.Global.putInt(contentResolver2, sb2.toString(), i);
                            Settings.Global.putInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, i);
                            z2 = false;
                            z = false;
                            SettingsFragment.this.mRoamingSettings[1] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_voice_text_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[2] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_data_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[3] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[4] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                            SettingsFragment.this.mRoamingSettings[5] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                            int[] iArr = new int[6];
                            for (int i2 = 0; i2 < SettingsFragment.this.mRoamingSettings.length; i2++) {
                                iArr[i2] = Integer.valueOf(SettingsFragment.this.mRoamingSettings[i2].intValue()).intValue();
                                Log.d("RoamingSettings", "Send to MD: RoamingSettings[" + i2 + "]: " + iArr[i2]);
                            }
                            PhoneFactory.getPhone(phone.getPhoneId()).setRoamingEnable(iArr, SettingsFragment.this.mHandler.obtainMessage(1));
                            if (z2) {
                                Settings.Global.putInt(phone.getContext().getContentResolver(), "international_data_roaming" + RoamingSettings.sSubId, 1);
                            }
                            if (z) {
                                Settings.Global.putInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 1);
                                Settings.Global.putInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 1);
                            }
                            break;
                        } else {
                            boolean z3 = i == 1;
                            SettingsFragment.this.mDomesticButtonDataRoam.setEnabled(false);
                            Settings.Global.putInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0);
                            Settings.Global.putInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0);
                            z = z3;
                            z2 = false;
                            SettingsFragment.this.mRoamingSettings[1] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_voice_text_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[2] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_data_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[3] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[4] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                            SettingsFragment.this.mRoamingSettings[5] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                            int[] iArr2 = new int[6];
                            while (i2 < SettingsFragment.this.mRoamingSettings.length) {
                            }
                            PhoneFactory.getPhone(phone.getPhoneId()).setRoamingEnable(iArr2, SettingsFragment.this.mHandler.obtainMessage(1));
                            if (z2) {
                            }
                            if (z) {
                            }
                        }
                        break;
                    case 1:
                        Log.d("RoamingSettings", "pref: " + key + "new value: " + obj);
                        ContentResolver contentResolver3 = phone.getContext().getContentResolver();
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("domestic_data_roaming");
                        sb3.append(RoamingSettings.sSubId);
                        Settings.Global.putInt(contentResolver3, sb3.toString(), zBooleanValue ? 1 : 0);
                        Settings.Global.putInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, zBooleanValue ? 1 : 0);
                        RoamingSettingsReceiver.notifyRoamingSettingsChanged(key);
                        z2 = false;
                        z = false;
                        SettingsFragment.this.mRoamingSettings[1] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_voice_text_roaming" + RoamingSettings.sSubId, 1));
                        SettingsFragment.this.mRoamingSettings[2] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_data_roaming" + RoamingSettings.sSubId, 1));
                        SettingsFragment.this.mRoamingSettings[3] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming" + RoamingSettings.sSubId, 1));
                        SettingsFragment.this.mRoamingSettings[4] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                        SettingsFragment.this.mRoamingSettings[5] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                        int[] iArr22 = new int[6];
                        while (i2 < SettingsFragment.this.mRoamingSettings.length) {
                        }
                        PhoneFactory.getPhone(phone.getPhoneId()).setRoamingEnable(iArr22, SettingsFragment.this.mHandler.obtainMessage(1));
                        if (z2) {
                        }
                        if (z) {
                        }
                        break;
                    case 2:
                        Log.d("RoamingSettings", "pref: " + key + "new value: " + obj);
                        ContentResolver contentResolver4 = phone.getContext().getContentResolver();
                        StringBuilder sb4 = new StringBuilder();
                        sb4.append("international_voice_text_roaming");
                        sb4.append(RoamingSettings.sSubId);
                        Settings.Global.putInt(contentResolver4, sb4.toString(), zBooleanValue ? 1 : 0);
                        boolean zBooleanValue2 = Boolean.valueOf(SettingsFragment.this.mInternationalButtonDataRoam.isChecked()).booleanValue();
                        if (zBooleanValue) {
                            SettingsFragment.this.mInternationalButtonDataRoam.setEnabled(true);
                            Log.d("RoamingSettings", "pref: INTERNATIONAL_DATA_ROAMING " + obj + " value: " + (zBooleanValue2 ? 1 : 0));
                            ContentResolver contentResolver5 = phone.getContext().getContentResolver();
                            StringBuilder sb5 = new StringBuilder();
                            sb5.append("international_data_roaming");
                            sb5.append(RoamingSettings.sSubId);
                            Settings.Global.putInt(contentResolver5, sb5.toString(), zBooleanValue2 ? 1 : 0);
                            z2 = false;
                            z = false;
                            SettingsFragment.this.mRoamingSettings[1] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_voice_text_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[2] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_data_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[3] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[4] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                            SettingsFragment.this.mRoamingSettings[5] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                            int[] iArr222 = new int[6];
                            while (i2 < SettingsFragment.this.mRoamingSettings.length) {
                            }
                            PhoneFactory.getPhone(phone.getPhoneId()).setRoamingEnable(iArr222, SettingsFragment.this.mHandler.obtainMessage(1));
                            if (z2) {
                            }
                            if (z) {
                            }
                        } else {
                            boolean z4 = zBooleanValue2;
                            SettingsFragment.this.mInternationalButtonDataRoam.setEnabled(false);
                            Settings.Global.putInt(phone.getContext().getContentResolver(), "international_data_roaming" + RoamingSettings.sSubId, 0);
                            z = false;
                            z2 = z4;
                            SettingsFragment.this.mRoamingSettings[1] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_voice_text_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[2] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_data_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[3] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming" + RoamingSettings.sSubId, 1));
                            SettingsFragment.this.mRoamingSettings[4] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                            SettingsFragment.this.mRoamingSettings[5] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                            int[] iArr2222 = new int[6];
                            while (i2 < SettingsFragment.this.mRoamingSettings.length) {
                            }
                            PhoneFactory.getPhone(phone.getPhoneId()).setRoamingEnable(iArr2222, SettingsFragment.this.mHandler.obtainMessage(1));
                            if (z2) {
                            }
                            if (z) {
                            }
                        }
                        break;
                    case 3:
                        Log.d("RoamingSettings", "pref: " + key + "new value: " + obj);
                        ContentResolver contentResolver6 = phone.getContext().getContentResolver();
                        StringBuilder sb6 = new StringBuilder();
                        sb6.append("international_data_roaming");
                        sb6.append(RoamingSettings.sSubId);
                        Settings.Global.putInt(contentResolver6, sb6.toString(), zBooleanValue ? 1 : 0);
                        RoamingSettingsReceiver.notifyRoamingSettingsChanged(key);
                        z2 = false;
                        z = false;
                        SettingsFragment.this.mRoamingSettings[1] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_voice_text_roaming" + RoamingSettings.sSubId, 1));
                        SettingsFragment.this.mRoamingSettings[2] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_data_roaming" + RoamingSettings.sSubId, 1));
                        SettingsFragment.this.mRoamingSettings[3] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming" + RoamingSettings.sSubId, 1));
                        SettingsFragment.this.mRoamingSettings[4] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                        SettingsFragment.this.mRoamingSettings[5] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + RoamingSettings.sSubId, 0));
                        int[] iArr22222 = new int[6];
                        while (i2 < SettingsFragment.this.mRoamingSettings.length) {
                        }
                        PhoneFactory.getPhone(phone.getPhoneId()).setRoamingEnable(iArr22222, SettingsFragment.this.mHandler.obtainMessage(1));
                        if (z2) {
                        }
                        if (z) {
                        }
                        break;
                }
                return false;
            }
        };

        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            this.mHandler = new MyHandler();
            addPreferencesFromResource(com.android.phone.R.xml.mtk_roaming_preference);
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            this.mDomesticButtonVoiceRoam = (RoamingSwitchPreference) preferenceScreen.findPreference("domestic_voice_text_roaming_settings");
            this.mDomesticButtonVoiceRoam.setOnPreferenceChangeListener(this.mRoamingSettingsListener);
            this.mDomesticButtonDataRoam = (RoamingSwitchPreference) preferenceScreen.findPreference("domestic_data_roaming_settings");
            this.mDomesticButtonDataRoam.setOnPreferenceChangeListener(this.mRoamingSettingsListener);
            this.mInternationalButtonVoiceRoam = (RoamingSwitchPreference) preferenceScreen.findPreference("international_voice_text_roaming_settings");
            this.mInternationalButtonVoiceRoam.setOnPreferenceChangeListener(this.mRoamingSettingsListener);
            this.mInternationalButtonDataRoam = (RoamingSwitchPreference) preferenceScreen.findPreference("international_data_roaming_settings");
            this.mInternationalButtonDataRoam.setOnPreferenceChangeListener(this.mRoamingSettingsListener);
            initRoamingSettings(RoamingSettings.sSubId);
        }

        private void initRoamingSettings(int i) {
            Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i));
            Log.d("RoamingSettings", "update for subId " + i + "and update roaming value");
            try {
                boolean z = true;
                int i2 = Settings.Global.getInt(phone.getContext().getContentResolver(), "international_voice_text_roaming" + i, 1);
                int i3 = Settings.Global.getInt(phone.getContext().getContentResolver(), "international_data_roaming" + i, 1);
                int i4 = Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming" + i, 1);
                int i5 = Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + i, 0);
                this.mRoamingSettings[5] = Integer.valueOf(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + i, 0));
                boolean z2 = i2 == 1;
                boolean z3 = i3 == 1;
                boolean z4 = i4 == 1;
                if (i5 != 1) {
                    z = false;
                }
                this.mDomesticButtonVoiceRoam.setChecked(z4);
                this.mDomesticButtonDataRoam.setChecked(z);
                this.mInternationalButtonVoiceRoam.setChecked(z2);
                this.mInternationalButtonDataRoam.setChecked(z3);
                if (!z4) {
                    this.mDomesticButtonDataRoam.setEnabled(false);
                }
                if (!z2) {
                    this.mInternationalButtonDataRoam.setEnabled(false);
                }
            } catch (Exception e) {
                Log.e("RoamingSettings", "Caught exception");
            }
        }

        private class MyHandler extends Handler {
            private MyHandler() {
            }

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 0:
                        SettingsFragment.this.handleQueryRoamingSettings(message);
                        break;
                    case 1:
                        SettingsFragment.this.handleSetRoamingSettings();
                        break;
                }
            }
        }

        private void handleQueryRoamingSettings(Message message) {
            Log.d("RoamingSettings", "handleQueryRoamingSettings ");
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null) {
                Log.d("RoamingSettings", "handleGetFemtocellList with exception = " + asyncResult.exception);
                if (!(asyncResult.exception instanceof CommandException) || asyncResult.exception.getCommandError() != CommandException.Error.ABORTED) {
                    return;
                } else {
                    return;
                }
            }
            Log.d("RoamingSettings", "handleQueryRoamingSettings no exception");
            this.mRoamingSettings = (Integer[]) asyncResult.result;
        }

        private void handleSetRoamingSettings() {
            Log.d("RoamingSettings", "handleSetRoamingSettings ");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.android.phone.R.menu.roaming_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            finish();
            return true;
        }
        if (itemId == com.android.phone.R.id.roaming_alert_settings) {
            Intent intent = new Intent(this, (Class<?>) GuardSettings.class);
            intent.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, sSubId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void initIntentFilter() {
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        this.mIntentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        this.mIntentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
    }
}
