package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IStopUserCallback;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.google.android.collect.Sets;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class UserRestrictionsUtils {
    private static final String TAG = "UserRestrictionsUtils";
    public static final Set<String> USER_RESTRICTIONS = newSetWithUniqueCheck(new String[]{"no_config_wifi", "no_modify_accounts", "no_install_apps", "no_uninstall_apps", "no_share_location", "no_install_unknown_sources", "no_config_bluetooth", "no_usb_file_transfer", "no_config_credentials", "no_remove_user", "no_debugging_features", "no_config_vpn", "no_config_tethering", "no_network_reset", "no_factory_reset", "no_add_user", "ensure_verify_apps", "no_config_cell_broadcasts", "no_config_mobile_networks", "no_control_apps", "no_physical_media", "no_unmute_microphone", "no_adjust_volume", "no_outgoing_calls", "no_sms", "no_fun", "no_create_windows", "no_cross_profile_copy_paste", "no_outgoing_beam", "no_wallpaper", "no_safe_boot", "allow_parent_profile_app_linking", "no_record_audio", "no_camera", "no_run_in_background", "no_data_roaming", "no_set_user_icon", "no_set_wallpaper"});
    private static final Set<String> NON_PERSIST_USER_RESTRICTIONS = Sets.newArraySet(new String[]{"no_record_audio"});
    private static final Set<String> DEVICE_OWNER_ONLY_RESTRICTIONS = Sets.newArraySet(new String[]{"no_usb_file_transfer", "no_config_tethering", "no_network_reset", "no_factory_reset", "no_add_user", "no_config_cell_broadcasts", "no_config_mobile_networks", "no_physical_media", "no_sms", "no_fun", "no_safe_boot", "no_create_windows", "no_data_roaming"});
    private static final Set<String> IMMUTABLE_BY_OWNERS = Sets.newArraySet(new String[]{"no_record_audio", "no_wallpaper"});
    private static final Set<String> GLOBAL_RESTRICTIONS = Sets.newArraySet(new String[]{"no_adjust_volume", "no_run_in_background", "no_unmute_microphone"});

    private UserRestrictionsUtils() {
    }

    private static Set<String> newSetWithUniqueCheck(String[] strings) {
        Set<String> ret = Sets.newArraySet(strings);
        Preconditions.checkState(ret.size() == strings.length);
        return ret;
    }

    public static boolean isValidRestriction(String restriction) {
        if (!USER_RESTRICTIONS.contains(restriction)) {
            Slog.e(TAG, "Unknown restriction: " + restriction);
            return false;
        }
        return true;
    }

    public static void writeRestrictions(XmlSerializer serializer, Bundle restrictions, String tag) throws IOException {
        if (restrictions == null) {
            return;
        }
        serializer.startTag(null, tag);
        for (String key : restrictions.keySet()) {
            if (!NON_PERSIST_USER_RESTRICTIONS.contains(key)) {
                if (USER_RESTRICTIONS.contains(key)) {
                    if (restrictions.getBoolean(key)) {
                        serializer.attribute(null, key, "true");
                    }
                } else {
                    Log.w(TAG, "Unknown user restriction detected: " + key);
                }
            }
        }
        serializer.endTag(null, tag);
    }

    public static void readRestrictions(XmlPullParser parser, Bundle restrictions) throws IOException {
        for (String key : USER_RESTRICTIONS) {
            String value = parser.getAttributeValue(null, key);
            if (value != null) {
                restrictions.putBoolean(key, Boolean.parseBoolean(value));
            }
        }
    }

    public static Bundle nonNull(Bundle in) {
        return in != null ? in : new Bundle();
    }

    public static boolean isEmpty(Bundle in) {
        return in == null || in.size() == 0;
    }

    public static Bundle clone(Bundle in) {
        return in != null ? new Bundle(in) : new Bundle();
    }

    public static void merge(Bundle dest, Bundle in) {
        Preconditions.checkNotNull(dest);
        Preconditions.checkArgument(dest != in);
        if (in == null) {
            return;
        }
        for (String key : in.keySet()) {
            if (in.getBoolean(key, false)) {
                dest.putBoolean(key, true);
            }
        }
    }

    public static boolean canDeviceOwnerChange(String restriction) {
        return !IMMUTABLE_BY_OWNERS.contains(restriction);
    }

    public static boolean canProfileOwnerChange(String restriction, int userId) {
        if (IMMUTABLE_BY_OWNERS.contains(restriction)) {
            return false;
        }
        return userId == 0 || !DEVICE_OWNER_ONLY_RESTRICTIONS.contains(restriction);
    }

    public static void sortToGlobalAndLocal(Bundle in, Bundle global, Bundle local) {
        if (in == null || in.size() == 0) {
            return;
        }
        for (String key : in.keySet()) {
            if (in.getBoolean(key)) {
                if (DEVICE_OWNER_ONLY_RESTRICTIONS.contains(key) || GLOBAL_RESTRICTIONS.contains(key)) {
                    global.putBoolean(key, true);
                } else {
                    local.putBoolean(key, true);
                }
            }
        }
    }

    public static boolean areEqual(Bundle a, Bundle b) {
        if (a == b) {
            return true;
        }
        if (isEmpty(a)) {
            return isEmpty(b);
        }
        if (isEmpty(b)) {
            return false;
        }
        for (String key : a.keySet()) {
            if (a.getBoolean(key) != b.getBoolean(key)) {
                return false;
            }
        }
        for (String key2 : b.keySet()) {
            if (a.getBoolean(key2) != b.getBoolean(key2)) {
                return false;
            }
        }
        return true;
    }

    public static void applyUserRestrictions(Context context, int userId, Bundle newRestrictions, Bundle prevRestrictions) {
        for (String key : USER_RESTRICTIONS) {
            boolean newValue = newRestrictions.getBoolean(key);
            boolean prevValue = prevRestrictions.getBoolean(key);
            if (newValue != prevValue) {
                applyUserRestriction(context, userId, key, newValue);
            }
        }
    }

    private static void applyUserRestriction(Context context, int userId, String key, boolean newValue) {
        ContentResolver cr = context.getContentResolver();
        long id = Binder.clearCallingIdentity();
        try {
            if (key.equals("no_config_wifi")) {
                if (newValue) {
                    Settings.Secure.putIntForUser(cr, "wifi_networks_available_notification_on", 0, userId);
                }
            } else if (key.equals("no_data_roaming")) {
                if (newValue) {
                    SubscriptionManager subscriptionManager = new SubscriptionManager(context);
                    List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                    if (subscriptionInfoList != null) {
                        for (SubscriptionInfo subInfo : subscriptionInfoList) {
                            Settings.Global.putStringForUser(cr, "data_roaming" + subInfo.getSubscriptionId(), "0", userId);
                        }
                    }
                    Settings.Global.putStringForUser(cr, "data_roaming", "0", userId);
                }
            } else if (key.equals("no_share_location")) {
                if (newValue) {
                    Settings.Secure.putIntForUser(cr, "location_mode", 0, userId);
                }
            } else if (key.equals("no_debugging_features")) {
                if (newValue && userId == 0) {
                    Settings.Global.putStringForUser(cr, "adb_enabled", "0", userId);
                }
            } else if (key.equals("ensure_verify_apps")) {
                if (newValue) {
                    Settings.Global.putStringForUser(context.getContentResolver(), "package_verifier_enable", "1", userId);
                    Settings.Global.putStringForUser(context.getContentResolver(), "verifier_verify_adb_installs", "1", userId);
                }
            } else if (key.equals("no_install_unknown_sources")) {
                if (newValue) {
                    Settings.Secure.putIntForUser(cr, "install_non_market_apps", 0, userId);
                }
            } else if (key.equals("no_run_in_background")) {
                if (newValue) {
                    int currentUser = ActivityManager.getCurrentUser();
                    if (currentUser != userId && userId != 0) {
                        try {
                            ActivityManagerNative.getDefault().stopUser(userId, false, (IStopUserCallback) null);
                        } catch (RemoteException e) {
                            throw e.rethrowAsRuntimeException();
                        }
                    }
                }
            } else if (key.equals("no_safe_boot")) {
                Settings.Global.putInt(context.getContentResolver(), "safe_boot_disallowed", newValue ? 1 : 0);
            }
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    public static void dumpRestrictions(PrintWriter pw, String prefix, Bundle restrictions) {
        boolean noneSet = true;
        if (restrictions != null) {
            for (String key : restrictions.keySet()) {
                if (restrictions.getBoolean(key, false)) {
                    pw.println(prefix + key);
                    noneSet = false;
                }
            }
            if (!noneSet) {
                return;
            }
            pw.println(prefix + "none");
            return;
        }
        pw.println(prefix + "null");
    }
}
