package com.android.providers.settings;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.icu.util.ULocale;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.LocaleList;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.LocalePicker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class SettingsHelper {
    private static final ArraySet<String> sBroadcastOnRestore = new ArraySet<>(4);
    private static SettingsLookup sGlobalLookup;
    private static SettingsLookup sSecureLookup;
    private static SettingsLookup sSystemLookup;
    private AudioManager mAudioManager;
    private Context mContext;
    private TelephonyManager mTelephonyManager;

    private interface SettingsLookup {
        String lookup(ContentResolver contentResolver, String str, int i);
    }

    static {
        sBroadcastOnRestore.add("enabled_notification_listeners");
        sBroadcastOnRestore.add("enabled_vr_listeners");
        sBroadcastOnRestore.add("enabled_accessibility_services");
        sBroadcastOnRestore.add("bluetooth_on");
        sSystemLookup = new SettingsLookup() {
            @Override
            public String lookup(ContentResolver contentResolver, String str, int i) {
                return Settings.System.getStringForUser(contentResolver, str, i);
            }
        };
        sSecureLookup = new SettingsLookup() {
            @Override
            public String lookup(ContentResolver contentResolver, String str, int i) {
                return Settings.Secure.getStringForUser(contentResolver, str, i);
            }
        };
        sGlobalLookup = new SettingsLookup() {
            @Override
            public String lookup(ContentResolver contentResolver, String str, int i) {
                return Settings.Global.getStringForUser(contentResolver, str, i);
            }
        };
    }

    public SettingsHelper(Context context) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
    }

    public void restoreValue(Context context, ContentResolver contentResolver, ContentValues contentValues, Uri uri, String str, String str2, int i) {
        boolean z;
        String strLookup;
        SettingsLookup settingsLookup = uri.equals(Settings.Secure.CONTENT_URI) ? sSecureLookup : uri.equals(Settings.System.CONTENT_URI) ? sSystemLookup : sGlobalLookup;
        if (sBroadcastOnRestore.contains(str)) {
            strLookup = settingsLookup.lookup(contentResolver, str, 0);
            z = true;
        } else {
            z = false;
            strLookup = null;
        }
        try {
            if ("sound_effects_enabled".equals(str)) {
                setSoundEffects(Integer.parseInt(str2) == 1);
            } else {
                if ("location_providers_allowed".equals(str)) {
                    setGpsLocation(str2);
                    if (z) {
                        context.sendBroadcastAsUser(new Intent("android.os.action.SETTING_RESTORED").setPackage("android").addFlags(1073741824).putExtra("setting_name", str).putExtra("new_value", str2).putExtra("previous_value", strLookup).putExtra("restored_from_sdk_int", i), UserHandle.SYSTEM, null);
                        return;
                    }
                    return;
                }
                if (!"backup_auto_restore".equals(str)) {
                    if (isAlreadyConfiguredCriticalAccessibilitySetting(str)) {
                        if (z) {
                            context.sendBroadcastAsUser(new Intent("android.os.action.SETTING_RESTORED").setPackage("android").addFlags(1073741824).putExtra("setting_name", str).putExtra("new_value", str2).putExtra("previous_value", strLookup).putExtra("restored_from_sdk_int", i), UserHandle.SYSTEM, null);
                            return;
                        }
                        return;
                    } else {
                        if (!"ringtone".equals(str)) {
                            if ("notification_sound".equals(str)) {
                            }
                        }
                        setRingtone(str, str2);
                        if (z) {
                            context.sendBroadcastAsUser(new Intent("android.os.action.SETTING_RESTORED").setPackage("android").addFlags(1073741824).putExtra("setting_name", str).putExtra("new_value", str2).putExtra("previous_value", strLookup).putExtra("restored_from_sdk_int", i), UserHandle.SYSTEM, null);
                            return;
                        }
                        return;
                    }
                }
                setAutoRestore(Integer.parseInt(str2) == 1);
            }
            contentValues.clear();
            contentValues.put("name", str);
            contentValues.put("value", str2);
            contentResolver.insert(uri, contentValues);
            if (z) {
                context.sendBroadcastAsUser(new Intent("android.os.action.SETTING_RESTORED").setPackage("android").addFlags(1073741824).putExtra("setting_name", str).putExtra("new_value", str2).putExtra("previous_value", strLookup).putExtra("restored_from_sdk_int", i), UserHandle.SYSTEM, null);
            }
        } catch (Exception e) {
        } catch (Throwable th) {
            if (z) {
                context.sendBroadcastAsUser(new Intent("android.os.action.SETTING_RESTORED").setPackage("android").addFlags(1073741824).putExtra("setting_name", str).putExtra("new_value", str2).putExtra("previous_value", strLookup).putExtra("restored_from_sdk_int", i), UserHandle.SYSTEM, null);
            }
            throw th;
        }
    }

    public String onBackupValue(String str, String str2) {
        if ("ringtone".equals(str) || "notification_sound".equals(str)) {
            if (str2 == null) {
                if ("ringtone".equals(str)) {
                    if (this.mTelephonyManager != null && this.mTelephonyManager.isVoiceCapable()) {
                        return "_silent";
                    }
                    return null;
                }
                return "_silent";
            }
            return getCanonicalRingtoneValue(str2);
        }
        return str2;
    }

    private void setRingtone(String str, String str2) {
        Uri uriUncanonicalize;
        if (str2 == null) {
            return;
        }
        if ("_silent".equals(str2)) {
            uriUncanonicalize = null;
        } else {
            uriUncanonicalize = this.mContext.getContentResolver().uncanonicalize(Uri.parse(str2));
            if (uriUncanonicalize == null) {
                return;
            }
        }
        RingtoneManager.setActualDefaultRingtoneUri(this.mContext, "ringtone".equals(str) ? 1 : 2, uriUncanonicalize);
    }

    private String getCanonicalRingtoneValue(String str) {
        Uri uriCanonicalize = this.mContext.getContentResolver().canonicalize(Uri.parse(str));
        if (uriCanonicalize == null) {
            return null;
        }
        return uriCanonicalize.toString();
    }

    private boolean isAlreadyConfiguredCriticalAccessibilitySetting(String str) {
        switch (str) {
            case "accessibility_enabled":
            case "speak_password":
            case "touch_exploration_enabled":
            case "accessibility_display_daltonizer_enabled":
            case "accessibility_display_magnification_enabled":
            case "accessibility_display_magnification_navbar_enabled":
            case "ui_night_mode":
                if (Settings.Secure.getInt(this.mContext.getContentResolver(), str, 0) == 0) {
                    break;
                }
                break;
            case "font_scale":
                if (Settings.System.getFloat(this.mContext.getContentResolver(), str, 1.0f) == 1.0f) {
                    break;
                }
                break;
        }
        return false;
    }

    private void setAutoRestore(boolean z) {
        try {
            IBackupManager iBackupManagerAsInterface = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
            if (iBackupManagerAsInterface != null) {
                iBackupManagerAsInterface.setAutoRestore(z);
            }
        } catch (RemoteException e) {
        }
    }

    private void setGpsLocation(String str) {
        if (((UserManager) this.mContext.getSystemService("user")).hasUserRestriction("no_share_location")) {
            return;
        }
        ((LocationManager) this.mContext.getSystemService("location")).setProviderEnabledForUser("gps", "gps".equals(str) || str.startsWith("gps,") || str.endsWith(",gps") || str.contains(",gps,"), Process.myUserHandle());
    }

    private void setSoundEffects(boolean z) {
        if (z) {
            this.mAudioManager.loadSoundEffects();
        } else {
            this.mAudioManager.unloadSoundEffects();
        }
    }

    byte[] getLocaleData() {
        return this.mContext.getResources().getConfiguration().getLocales().toLanguageTags().getBytes();
    }

    private static Locale toFullLocale(Locale locale) {
        if (locale.getScript().isEmpty() || locale.getCountry().isEmpty()) {
            return ULocale.addLikelySubtags(ULocale.forLocale(locale)).toLocale();
        }
        return locale;
    }

    @VisibleForTesting
    public static LocaleList resolveLocales(LocaleList localeList, LocaleList localeList2, String[] strArr) {
        HashMap map = new HashMap(strArr.length);
        for (String str : strArr) {
            Locale localeForLanguageTag = Locale.forLanguageTag(str);
            map.put(toFullLocale(localeForLanguageTag), localeForLanguageTag);
        }
        ArrayList arrayList = new ArrayList(localeList2.size());
        for (int i = 0; i < localeList2.size(); i++) {
            Locale locale = localeList2.get(i);
            map.remove(toFullLocale(locale));
            arrayList.add(locale);
        }
        for (int i2 = 0; i2 < localeList.size(); i2++) {
            Locale locale2 = (Locale) map.remove(toFullLocale(localeList.get(i2)));
            if (locale2 != null) {
                arrayList.add(locale2);
            }
        }
        if (arrayList.size() == localeList2.size()) {
            return localeList2;
        }
        return new LocaleList((Locale[]) arrayList.toArray(new Locale[arrayList.size()]));
    }

    void setLocaleData(byte[] bArr, int i) {
        Configuration configuration = this.mContext.getResources().getConfiguration();
        LocaleList localeListForLanguageTags = LocaleList.forLanguageTags(new String(bArr, 0, i).replace('_', '-'));
        if (localeListForLanguageTags.isEmpty()) {
            return;
        }
        String[] supportedLocales = LocalePicker.getSupportedLocales(this.mContext);
        LocaleList locales = configuration.getLocales();
        LocaleList localeListResolveLocales = resolveLocales(localeListForLanguageTags, locales, supportedLocales);
        if (localeListResolveLocales.equals(locales)) {
            return;
        }
        try {
            IActivityManager service = ActivityManager.getService();
            Configuration configuration2 = service.getConfiguration();
            configuration2.setLocales(localeListResolveLocales);
            configuration2.userSetLocale = true;
            service.updatePersistentConfiguration(configuration2);
        } catch (RemoteException e) {
        }
    }

    void applyAudioSettings() {
        new AudioManager(this.mContext).reloadAudioSettings();
    }
}
