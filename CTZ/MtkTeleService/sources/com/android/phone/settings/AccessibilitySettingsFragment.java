package com.android.phone.settings;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.CallSettingUtils;

public class AccessibilitySettingsFragment extends PreferenceFragment {
    private static final String LOG_TAG = AccessibilitySettingsFragment.class.getSimpleName();
    private AudioManager mAudioManager;
    private SwitchPreference mButtonDualMic;
    private SwitchPreference mButtonHac;
    private SwitchPreference mButtonRtt;
    private TtyModeListPreference mButtonTty;
    private Context mContext;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int i, String str) {
            Log.d(AccessibilitySettingsFragment.LOG_TAG, "PhoneStateListener.onCallStateChanged: state=" + i);
            Preference preferenceFindPreference = AccessibilitySettingsFragment.this.getPreferenceScreen().findPreference("button_tty_mode_key");
            if (preferenceFindPreference != null) {
                preferenceFindPreference.setEnabled(((ImsManager.isVolteEnabledByPlatform(AccessibilitySettingsFragment.this.mContext) && AccessibilitySettingsFragment.this.getVolteTtySupported()) && !AccessibilitySettingsFragment.this.isVideoCallOrConferenceInProgress()) || i == 0);
            }
            ExtensionManager.getAccessibilitySettingsExt().handleCallStateChanged(AccessibilitySettingsFragment.this, i, R.array.tty_mode_entries, R.array.tty_mode_values);
        }
    };
    private final ContentObserver mTtyObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) {
            if (AccessibilitySettingsFragment.this.mButtonTty != null) {
                Log.d(AccessibilitySettingsFragment.LOG_TAG, " --- TTY mode changed ---");
                AccessibilitySettingsFragment.this.mButtonTty.init();
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mContext = getActivity().getApplicationContext();
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        addPreferencesFromResource(R.xml.accessibility_settings);
        this.mButtonTty = (TtyModeListPreference) findPreference(getResources().getString(R.string.tty_mode_key));
        this.mButtonHac = (SwitchPreference) findPreference("button_hac_key");
        this.mButtonRtt = (SwitchPreference) findPreference("button_rtt_key");
        if (PhoneGlobals.getInstance().phoneMgr.isTtyModeSupported()) {
            this.mButtonTty.init();
        } else {
            getPreferenceScreen().removePreference(this.mButtonTty);
            this.mButtonTty = null;
        }
        if (CallSettingUtils.isHacSupport()) {
            this.mButtonHac.setChecked(Settings.System.getInt(this.mContext.getContentResolver(), "hearing_aid", 0) == 1);
        } else {
            getPreferenceScreen().removePreference(this.mButtonHac);
            this.mButtonHac = null;
        }
        if (PhoneGlobals.getInstance().phoneMgr.isRttSupported()) {
            this.mButtonRtt.setChecked(Settings.Secure.getInt(this.mContext.getContentResolver(), "rtt_calling_mode", 0) != 0);
        } else {
            getPreferenceScreen().removePreference(this.mButtonRtt);
            getPreferenceScreen().removePreference(findPreference("button_rtt_more_information_key"));
            this.mButtonRtt = null;
        }
        initUi(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        super.onResume();
        ((TelephonyManager) this.mContext.getSystemService("phone")).listen(this.mPhoneStateListener, 32);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("preferred_tty_mode"), false, this.mTtyObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        ((TelephonyManager) this.mContext.getSystemService("phone")).listen(this.mPhoneStateListener, 0);
        this.mContext.getContentResolver().unregisterContentObserver(this.mTtyObserver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int i;
        if (preference == this.mButtonTty) {
            return true;
        }
        if (preference == this.mButtonHac) {
            if (!this.mButtonHac.isChecked()) {
                i = 0;
            } else {
                i = 1;
            }
            Settings.System.putInt(this.mContext.getContentResolver(), "hearing_aid", i);
            this.mAudioManager.setParameter(SettingsConstants.HAC_KEY, i == 1 ? SettingsConstants.HAC_VAL_ON : SettingsConstants.HAC_VAL_OFF);
            return true;
        }
        if (preference == this.mButtonRtt) {
            Log.i(LOG_TAG, "RTT setting changed -- now " + this.mButtonRtt.isChecked());
            Settings.Secure.putInt(this.mContext.getContentResolver(), "rtt_calling_mode", this.mButtonRtt.isChecked() ? 1 : 0);
            ImsManager.getInstance(getContext(), SubscriptionManager.getDefaultVoicePhoneId()).setRttEnabled(this.mButtonRtt.isChecked());
            return true;
        }
        return onPreferenceTreeClick(preference);
    }

    private boolean getVolteTtySupported() {
        return ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfig().getBoolean("carrier_volte_tty_supported_bool");
    }

    private boolean isVideoCallOrConferenceInProgress() {
        Phone[] phones = PhoneFactory.getPhones();
        if (phones == null) {
            Log.d(LOG_TAG, "isVideoCallOrConferenceInProgress: No phones found.");
            return false;
        }
        for (Phone phone : phones) {
            if (phone.isImsVideoCallOrConferencePresent()) {
                return true;
            }
        }
        return false;
    }

    private void initUi(PreferenceScreen preferenceScreen) {
        this.mButtonDualMic = (SwitchPreference) preferenceScreen.findPreference("button_dual_mic_key");
        if (CallSettingUtils.isMtkDualMicSupport()) {
            this.mButtonDualMic.setChecked(CallSettingUtils.isDualMicModeEnabled());
        } else {
            preferenceScreen.removePreference(this.mButtonDualMic);
            this.mButtonDualMic = null;
        }
    }

    private boolean onPreferenceTreeClick(Preference preference) {
        if (preference == this.mButtonDualMic) {
            Log.d(LOG_TAG, "mButtonDualmic turn on: " + this.mButtonDualMic.isChecked());
            CallSettingUtils.setDualMicMode(this.mButtonDualMic.isChecked() ? SettingsConstants.DUA_VAL_ON : SettingsConstants.DUAL_VAL_OFF);
            return true;
        }
        return false;
    }
}
