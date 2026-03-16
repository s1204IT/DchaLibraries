package com.android.providers.settings;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.backup.IBackupManager;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import java.util.Locale;

public class SettingsHelper {
    private AudioManager mAudioManager;
    private Context mContext;
    private TelephonyManager mTelephonyManager;

    public SettingsHelper(Context context) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
    }

    public boolean restoreValue(String name, String value) {
        if ("screen_brightness".equals(name)) {
            setBrightness(Integer.parseInt(value));
        } else if ("sound_effects_enabled".equals(name)) {
            setSoundEffects(Integer.parseInt(value) == 1);
        } else {
            if ("location_providers_allowed".equals(name)) {
                setGpsLocation(value);
                return false;
            }
            if ("backup_auto_restore".equals(name)) {
                setAutoRestore(Integer.parseInt(value) == 1);
            } else {
                if (isAlreadyConfiguredCriticalAccessibilitySetting(name)) {
                    return false;
                }
                if ("ringtone".equals(name) || "notification_sound".equals(name)) {
                    setRingtone(name, value);
                    return false;
                }
            }
        }
        return true;
    }

    public String onBackupValue(String name, String value) {
        if ("ringtone".equals(name) || "notification_sound".equals(name)) {
            if (value == null) {
                if ("ringtone".equals(name)) {
                    if (this.mTelephonyManager != null && this.mTelephonyManager.isVoiceCapable()) {
                        return "_silent";
                    }
                    return null;
                }
                return "_silent";
            }
            return getCanonicalRingtoneValue(value);
        }
        return value;
    }

    private void setRingtone(String name, String value) {
        Uri ringtoneUri;
        if (value != null) {
            if ("_silent".equals(value)) {
                ringtoneUri = null;
            } else {
                Uri canonicalUri = Uri.parse(value);
                ringtoneUri = this.mContext.getContentResolver().uncanonicalize(canonicalUri);
                if (ringtoneUri == null) {
                    return;
                }
            }
            int ringtoneType = "ringtone".equals(name) ? 1 : 2;
            RingtoneManager.setActualDefaultRingtoneUri(this.mContext, ringtoneType, ringtoneUri);
        }
    }

    private String getCanonicalRingtoneValue(String value) {
        Uri ringtoneUri = Uri.parse(value);
        Uri canonicalUri = this.mContext.getContentResolver().canonicalize(ringtoneUri);
        if (canonicalUri == null) {
            return null;
        }
        return canonicalUri.toString();
    }

    private boolean isAlreadyConfiguredCriticalAccessibilitySetting(String name) {
        return ("accessibility_enabled".equals(name) || "accessibility_script_injection".equals(name) || "speak_password".equals(name) || "touch_exploration_enabled".equals(name)) ? Settings.Secure.getInt(this.mContext.getContentResolver(), name, 0) != 0 : ("touch_exploration_granted_accessibility_services".equals(name) || "enabled_accessibility_services".equals(name)) && !TextUtils.isEmpty(Settings.Secure.getString(this.mContext.getContentResolver(), name));
    }

    private void setAutoRestore(boolean enabled) {
        try {
            IBackupManager bm = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
            if (bm != null) {
                bm.setAutoRestore(enabled);
            }
        } catch (RemoteException e) {
        }
    }

    private void setGpsLocation(String value) {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        if (!um.hasUserRestriction("no_share_location")) {
            boolean enabled = "gps".equals(value) || value.startsWith("gps,") || value.endsWith(",gps") || value.contains(",gps,");
            Settings.Secure.setLocationProviderEnabled(this.mContext.getContentResolver(), "gps", enabled);
        }
    }

    private void setSoundEffects(boolean enable) {
        if (enable) {
            this.mAudioManager.loadSoundEffects();
        } else {
            this.mAudioManager.unloadSoundEffects();
        }
    }

    private void setBrightness(int brightness) {
        try {
            IPowerManager power = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
            if (power != null) {
                power.setTemporaryScreenBrightnessSettingOverride(brightness);
            }
        } catch (RemoteException e) {
        }
    }

    byte[] getLocaleData() {
        Configuration conf = this.mContext.getResources().getConfiguration();
        Locale loc = conf.locale;
        String localeString = loc.getLanguage();
        String country = loc.getCountry();
        if (!TextUtils.isEmpty(country)) {
            localeString = localeString + "-" + country;
        }
        return localeString.getBytes();
    }

    void setLocaleData(byte[] data, int size) {
        Configuration conf = this.mContext.getResources().getConfiguration();
        if (!conf.userSetLocale) {
            String[] availableLocales = this.mContext.getAssets().getLocales();
            String localeCode = new String(data, 0, size).replace('_', '-');
            Locale loc = null;
            int i = 0;
            while (true) {
                if (i >= availableLocales.length) {
                    break;
                }
                if (!availableLocales[i].equals(localeCode)) {
                    i++;
                } else {
                    loc = Locale.forLanguageTag(localeCode);
                    break;
                }
            }
            if (loc != null) {
                try {
                    IActivityManager am = ActivityManagerNative.getDefault();
                    Configuration config = am.getConfiguration();
                    config.locale = loc;
                    config.userSetLocale = true;
                    am.updateConfiguration(config);
                } catch (RemoteException e) {
                }
            }
        }
    }

    void applyAudioSettings() {
        AudioManager am = new AudioManager(this.mContext);
        am.reloadAudioSettings();
    }
}
