package com.mediatek.services.telephony;

import android.R;
import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.SubscriptionInfoHelper;

public class GuardSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static int sSubId;
    private Context mContext;

    @Override
    protected void onCreate(Bundle bundle) {
        sSubId = getIntent().getIntExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, SubscriptionManager.getDefaultSubscriptionId());
        super.onCreate(bundle);
        getFragmentManager().beginTransaction().replace(R.id.content, new GuardSettingsFragment()).commit();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        this.mContext = getApplicationContext();
    }

    public static class GuardSettingsFragment extends PreferenceFragment {
        private RoamingSwitchPreference mDomesticButtonDataRoamGuard;
        private RoamingSwitchPreference mDomesticButtonVoiceRoamGuard;
        private RoamingSwitchPreference mInternationalButtonDataRoamGuard;
        private RoamingSwitchPreference mInternationalButtonVoiceRoamGuard;
        Preference.OnPreferenceChangeListener mRoamingGuardListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                Phone phone;
                byte b;
                Boolean bool = (Boolean) obj;
                if (((SwitchPreference) preference).isChecked() == bool.booleanValue()) {
                    Log.i("GuardSettings", preference.getKey() + " : " + String.valueOf(obj) + " not changed");
                    return false;
                }
                String key = preference.getKey();
                boolean zBooleanValue = bool.booleanValue();
                if (TelephonyManager.getDefault().getSimCount() == 1) {
                    Log.d("GuardSettings", "simCount 1 and update roaming value");
                    phone = PhoneFactory.getDefaultPhone();
                } else {
                    phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(GuardSettings.sSubId));
                    Log.d("GuardSettings", "update for subId " + GuardSettings.sSubId + "and update roaming value");
                }
                int iHashCode = key.hashCode();
                if (iHashCode != -1407390191) {
                    if (iHashCode != -630409093) {
                        if (iHashCode != -453803135) {
                            b = (iHashCode == 170114027 && key.equals("domestic_data_roaming_guard")) ? (byte) 1 : (byte) -1;
                        } else if (key.equals("international_voice_roaming_guard")) {
                            b = 2;
                        }
                    } else if (key.equals("international_data_roaming_guard")) {
                        b = 3;
                    }
                } else if (key.equals("domestic_voice_roaming_guard")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                        Log.d("GuardSettings", "pref: " + key + "new value: " + obj);
                        ContentResolver contentResolver = phone.getContext().getContentResolver();
                        StringBuilder sb = new StringBuilder();
                        sb.append("domestic_voice_text_roaming_guard");
                        sb.append(GuardSettings.sSubId);
                        Settings.Global.putInt(contentResolver, sb.toString(), zBooleanValue ? 1 : 0);
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("pref: ");
                        sb2.append(key);
                        sb2.append("new value: ");
                        sb2.append(Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming_guard" + GuardSettings.sSubId, -1));
                        sb2.append("sub id");
                        sb2.append(GuardSettings.sSubId);
                        Log.d("GuardSettings", sb2.toString());
                        break;
                    case 1:
                        Log.d("GuardSettings", "pref: " + key + "new value: " + obj);
                        ContentResolver contentResolver2 = phone.getContext().getContentResolver();
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("domestic_data_roaming_guard");
                        sb3.append(GuardSettings.sSubId);
                        Settings.Global.putInt(contentResolver2, sb3.toString(), zBooleanValue ? 1 : 0);
                        RoamingSettingsReceiver.notifyRoamingSettingsChanged(key);
                        break;
                    case 2:
                        Log.d("GuardSettings", "pref: " + key + "new value: " + obj);
                        ContentResolver contentResolver3 = phone.getContext().getContentResolver();
                        StringBuilder sb4 = new StringBuilder();
                        sb4.append("international_voice_roaming_guard");
                        sb4.append(GuardSettings.sSubId);
                        Settings.Global.putInt(contentResolver3, sb4.toString(), zBooleanValue ? 1 : 0);
                        StringBuilder sb5 = new StringBuilder();
                        sb5.append("pref: ");
                        sb5.append(key);
                        sb5.append("new value: ");
                        sb5.append(Settings.Global.getInt(phone.getContext().getContentResolver(), "international_voice_roaming_guard" + GuardSettings.sSubId, -1));
                        sb5.append("sub id");
                        sb5.append(GuardSettings.sSubId);
                        Log.d("GuardSettings", sb5.toString());
                        break;
                    case 3:
                        Log.d("GuardSettings", "pref: " + key + "new value: " + obj);
                        ContentResolver contentResolver4 = phone.getContext().getContentResolver();
                        StringBuilder sb6 = new StringBuilder();
                        sb6.append("international_data_roaming_guard");
                        sb6.append(GuardSettings.sSubId);
                        Settings.Global.putInt(contentResolver4, sb6.toString(), zBooleanValue ? 1 : 0);
                        RoamingSettingsReceiver.notifyRoamingSettingsChanged(key);
                        break;
                }
                return false;
            }
        };

        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            addPreferencesFromResource(com.android.phone.R.xml.mtk_guard_prefernce);
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            this.mDomesticButtonVoiceRoamGuard = (RoamingSwitchPreference) preferenceScreen.findPreference("domestic_voice_roaming_guard");
            this.mDomesticButtonVoiceRoamGuard.setOnPreferenceChangeListener(this.mRoamingGuardListener);
            this.mDomesticButtonDataRoamGuard = (RoamingSwitchPreference) preferenceScreen.findPreference("domestic_data_roaming_guard");
            this.mDomesticButtonDataRoamGuard.setOnPreferenceChangeListener(this.mRoamingGuardListener);
            this.mInternationalButtonVoiceRoamGuard = (RoamingSwitchPreference) preferenceScreen.findPreference("international_voice_roaming_guard");
            this.mInternationalButtonVoiceRoamGuard.setOnPreferenceChangeListener(this.mRoamingGuardListener);
            this.mInternationalButtonDataRoamGuard = (RoamingSwitchPreference) preferenceScreen.findPreference("international_data_roaming_guard");
            this.mInternationalButtonDataRoamGuard.setOnPreferenceChangeListener(this.mRoamingGuardListener);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        return false;
    }
}
