package com.android.server.pm;

import android.app.ActivityManager;
import android.app.IStopUserCallback;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.Preconditions;
import com.android.server.audio.AudioService;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.wm.WindowManagerService;
import com.google.android.collect.Sets;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class UserRestrictionsUtils {
    private static final String TAG = "UserRestrictionsUtils";
    public static final Set<String> USER_RESTRICTIONS = newSetWithUniqueCheck(new String[]{"no_config_wifi", "no_config_locale", "no_modify_accounts", "no_install_apps", "no_uninstall_apps", "no_share_location", "no_install_unknown_sources", "no_config_bluetooth", "no_bluetooth", "no_bluetooth_sharing", "no_usb_file_transfer", "no_config_credentials", "no_remove_user", "no_remove_managed_profile", "no_debugging_features", "no_config_vpn", "no_config_date_time", "no_config_tethering", "no_network_reset", "no_factory_reset", "no_add_user", "no_add_managed_profile", "ensure_verify_apps", "no_config_cell_broadcasts", "no_config_mobile_networks", "no_control_apps", "no_physical_media", "no_unmute_microphone", "no_adjust_volume", "no_outgoing_calls", "no_sms", "no_fun", "no_create_windows", "no_system_error_dialogs", "no_cross_profile_copy_paste", "no_outgoing_beam", "no_wallpaper", "no_safe_boot", "allow_parent_profile_app_linking", "no_record_audio", "no_camera", "no_run_in_background", "no_data_roaming", "no_set_user_icon", "no_set_wallpaper", "no_oem_unlock", "disallow_unmute_device", "no_autofill", "no_user_switch", "no_unified_password", "no_config_location", "no_airplane_mode", "no_config_brightness", "no_sharing_into_profile", "no_ambient_display", "no_config_screen_timeout", "no_printing"});
    private static final Set<String> NON_PERSIST_USER_RESTRICTIONS = Sets.newArraySet(new String[]{"no_record_audio"});
    private static final Set<String> PRIMARY_USER_ONLY_RESTRICTIONS = Sets.newArraySet(new String[]{"no_bluetooth", "no_usb_file_transfer", "no_config_tethering", "no_network_reset", "no_factory_reset", "no_add_user", "no_config_cell_broadcasts", "no_config_mobile_networks", "no_physical_media", "no_sms", "no_fun", "no_safe_boot", "no_create_windows", "no_data_roaming", "no_airplane_mode"});
    private static final Set<String> DEVICE_OWNER_ONLY_RESTRICTIONS = Sets.newArraySet(new String[]{"no_user_switch"});
    private static final Set<String> IMMUTABLE_BY_OWNERS = Sets.newArraySet(new String[]{"no_record_audio", "no_wallpaper", "no_oem_unlock"});
    private static final Set<String> GLOBAL_RESTRICTIONS = Sets.newArraySet(new String[]{"no_adjust_volume", "no_bluetooth_sharing", "no_config_date_time", "no_system_error_dialogs", "no_run_in_background", "no_unmute_microphone", "disallow_unmute_device"});
    private static final Set<String> DEFAULT_ENABLED_FOR_DEVICE_OWNERS = Sets.newArraySet(new String[]{"no_add_managed_profile"});
    private static final Set<String> DEFAULT_ENABLED_FOR_MANAGED_PROFILES = Sets.newArraySet(new String[]{"no_bluetooth_sharing"});
    private static final Set<String> PROFILE_GLOBAL_RESTRICTIONS = Sets.newArraySet(new String[]{"ensure_verify_apps", "no_airplane_mode"});

    private UserRestrictionsUtils() {
    }

    private static Set<String> newSetWithUniqueCheck(String[] strArr) {
        ArraySet arraySetNewArraySet = Sets.newArraySet(strArr);
        Preconditions.checkState(arraySetNewArraySet.size() == strArr.length);
        return arraySetNewArraySet;
    }

    public static boolean isValidRestriction(String str) {
        if (!USER_RESTRICTIONS.contains(str)) {
            Slog.e(TAG, "Unknown restriction: " + str);
            return false;
        }
        return true;
    }

    public static void writeRestrictions(XmlSerializer xmlSerializer, Bundle bundle, String str) throws IOException {
        if (bundle == null) {
            return;
        }
        xmlSerializer.startTag(null, str);
        for (String str2 : bundle.keySet()) {
            if (!NON_PERSIST_USER_RESTRICTIONS.contains(str2)) {
                if (USER_RESTRICTIONS.contains(str2)) {
                    if (bundle.getBoolean(str2)) {
                        xmlSerializer.attribute(null, str2, "true");
                    }
                } else {
                    Log.w(TAG, "Unknown user restriction detected: " + str2);
                }
            }
        }
        xmlSerializer.endTag(null, str);
    }

    public static void readRestrictions(XmlPullParser xmlPullParser, Bundle bundle) {
        bundle.clear();
        for (String str : USER_RESTRICTIONS) {
            String attributeValue = xmlPullParser.getAttributeValue(null, str);
            if (attributeValue != null) {
                bundle.putBoolean(str, Boolean.parseBoolean(attributeValue));
            }
        }
    }

    public static Bundle readRestrictions(XmlPullParser xmlPullParser) {
        Bundle bundle = new Bundle();
        readRestrictions(xmlPullParser, bundle);
        return bundle;
    }

    public static Bundle nonNull(Bundle bundle) {
        return bundle != null ? bundle : new Bundle();
    }

    public static boolean isEmpty(Bundle bundle) {
        return bundle == null || bundle.size() == 0;
    }

    public static boolean contains(Bundle bundle, String str) {
        return bundle != null && bundle.getBoolean(str);
    }

    public static Bundle clone(Bundle bundle) {
        return bundle != null ? new Bundle(bundle) : new Bundle();
    }

    public static void merge(Bundle bundle, Bundle bundle2) {
        Preconditions.checkNotNull(bundle);
        Preconditions.checkArgument(bundle != bundle2);
        if (bundle2 == null) {
            return;
        }
        for (String str : bundle2.keySet()) {
            if (bundle2.getBoolean(str, false)) {
                bundle.putBoolean(str, true);
            }
        }
    }

    public static Bundle mergeAll(SparseArray<Bundle> sparseArray) {
        if (sparseArray.size() == 0) {
            return null;
        }
        Bundle bundle = new Bundle();
        for (int i = 0; i < sparseArray.size(); i++) {
            merge(bundle, sparseArray.valueAt(i));
        }
        return bundle;
    }

    public static boolean canDeviceOwnerChange(String str) {
        return !IMMUTABLE_BY_OWNERS.contains(str);
    }

    public static boolean canProfileOwnerChange(String str, int i) {
        return (IMMUTABLE_BY_OWNERS.contains(str) || DEVICE_OWNER_ONLY_RESTRICTIONS.contains(str) || (i != 0 && PRIMARY_USER_ONLY_RESTRICTIONS.contains(str))) ? false : true;
    }

    public static Set<String> getDefaultEnabledForDeviceOwner() {
        return DEFAULT_ENABLED_FOR_DEVICE_OWNERS;
    }

    public static Set<String> getDefaultEnabledForManagedProfiles() {
        return DEFAULT_ENABLED_FOR_MANAGED_PROFILES;
    }

    public static void sortToGlobalAndLocal(Bundle bundle, boolean z, int i, Bundle bundle2, Bundle bundle3) {
        if (i == 2) {
            bundle2.putBoolean("no_camera", true);
        } else if (i == 1) {
            bundle3.putBoolean("no_camera", true);
        }
        if (bundle == null || bundle.size() == 0) {
            return;
        }
        for (String str : bundle.keySet()) {
            if (bundle.getBoolean(str)) {
                if (isGlobal(z, str)) {
                    bundle2.putBoolean(str, true);
                } else {
                    bundle3.putBoolean(str, true);
                }
            }
        }
    }

    private static boolean isGlobal(boolean z, String str) {
        return (z && (PRIMARY_USER_ONLY_RESTRICTIONS.contains(str) || GLOBAL_RESTRICTIONS.contains(str))) || PROFILE_GLOBAL_RESTRICTIONS.contains(str) || DEVICE_OWNER_ONLY_RESTRICTIONS.contains(str);
    }

    public static boolean areEqual(Bundle bundle, Bundle bundle2) {
        if (bundle == bundle2) {
            return true;
        }
        if (isEmpty(bundle)) {
            return isEmpty(bundle2);
        }
        if (isEmpty(bundle2)) {
            return false;
        }
        for (String str : bundle.keySet()) {
            if (bundle.getBoolean(str) != bundle2.getBoolean(str)) {
                return false;
            }
        }
        for (String str2 : bundle2.keySet()) {
            if (bundle.getBoolean(str2) != bundle2.getBoolean(str2)) {
                return false;
            }
        }
        return true;
    }

    public static void applyUserRestrictions(Context context, int i, Bundle bundle, Bundle bundle2) {
        for (String str : USER_RESTRICTIONS) {
            boolean z = bundle.getBoolean(str);
            if (z != bundle2.getBoolean(str)) {
                applyUserRestriction(context, i, str, z);
            }
        }
    }

    private static void applyUserRestriction(Context context, int i, String str, boolean z) {
        boolean z2;
        ContentResolver contentResolver = context.getContentResolver();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            z2 = true;
            switch (str) {
                case "no_data_roaming":
                    if (z) {
                        List<SubscriptionInfo> activeSubscriptionInfoList = ((SubscriptionManager) context.getSystemService(SubscriptionManager.class)).getActiveSubscriptionInfoList();
                        if (activeSubscriptionInfoList != null) {
                            Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
                            while (it.hasNext()) {
                                Settings.Global.putStringForUser(contentResolver, "data_roaming" + it.next().getSubscriptionId(), "0", i);
                            }
                        }
                        Settings.Global.putStringForUser(contentResolver, "data_roaming", "0", i);
                    }
                    return;
                case "no_share_location":
                    if (z) {
                        Settings.Secure.putIntForUser(contentResolver, "location_mode", 0, i);
                    }
                    return;
                case "no_debugging_features":
                    if (z && i == 0) {
                        Settings.Global.putStringForUser(contentResolver, "adb_enabled", "0", i);
                    }
                    return;
                case "ensure_verify_apps":
                    if (z) {
                        Settings.Global.putStringForUser(context.getContentResolver(), "package_verifier_enable", "1", i);
                        Settings.Global.putStringForUser(context.getContentResolver(), "verifier_verify_adb_installs", "1", i);
                    }
                    return;
                case "no_install_unknown_sources":
                    Settings.Secure.putIntForUser(contentResolver, "install_non_market_apps", !z ? 1 : 0, i);
                    return;
                case "no_run_in_background":
                    if (z) {
                        if (ActivityManager.getCurrentUser() != i && i != 0) {
                            try {
                                ActivityManager.getService().stopUser(i, false, (IStopUserCallback) null);
                            } catch (RemoteException e) {
                                throw e.rethrowAsRuntimeException();
                            }
                        }
                        break;
                    }
                    return;
                case "no_safe_boot":
                    Settings.Global.putInt(context.getContentResolver(), "safe_boot_disallowed", z ? 1 : 0);
                    return;
                case "no_airplane_mode":
                    if (z) {
                        if (Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 1) {
                            z2 = false;
                        }
                        if (z2) {
                            Settings.Global.putInt(context.getContentResolver(), "airplane_mode_on", 0);
                            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
                            intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, false);
                            context.sendBroadcastAsUser(intent, UserHandle.ALL);
                        }
                    }
                    return;
                case "no_ambient_display":
                    if (z) {
                        Settings.Secure.putIntForUser(context.getContentResolver(), "doze_enabled", 0, i);
                        Settings.Secure.putIntForUser(context.getContentResolver(), "doze_always_on", 0, i);
                        Settings.Secure.putIntForUser(context.getContentResolver(), "doze_pulse_on_pick_up", 0, i);
                        Settings.Secure.putIntForUser(context.getContentResolver(), "doze_pulse_on_long_press", 0, i);
                        Settings.Secure.putIntForUser(context.getContentResolver(), "doze_pulse_on_double_tap", 0, i);
                    }
                    return;
                case "no_config_location":
                    if (z) {
                        Settings.Global.putString(context.getContentResolver(), "location_global_kill_switch", "0");
                    }
                    return;
                default:
                    return;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static boolean isSettingRestrictedForUser(Context context, String str, int i, String str2, int i2) {
        byte b;
        String str3;
        Preconditions.checkNotNull(str);
        UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
        boolean z = false;
        switch (str.hashCode()) {
            case -1796809747:
                b = !str.equals("location_mode") ? (byte) -1 : (byte) 0;
                break;
            case -1500478207:
                if (str.equals("location_providers_allowed")) {
                    b = 1;
                    break;
                }
                break;
            case -1490222856:
                if (str.equals("doze_enabled")) {
                    b = 11;
                    break;
                }
                break;
            case -1115710219:
                if (str.equals("verifier_verify_adb_installs")) {
                    b = 5;
                    break;
                }
                break;
            case -970351711:
                if (str.equals("adb_enabled")) {
                    b = 3;
                    break;
                }
                break;
            case -693072130:
                if (str.equals("screen_brightness_mode")) {
                    b = 18;
                    break;
                }
                break;
            case -623873498:
                if (str.equals("always_on_vpn_app")) {
                    b = 7;
                    break;
                }
                break;
            case -416662510:
                if (str.equals("preferred_network_mode")) {
                    b = 6;
                    break;
                }
                break;
            case -101820922:
                if (str.equals("doze_always_on")) {
                    b = 12;
                    break;
                }
                break;
            case -32505807:
                if (str.equals("doze_pulse_on_long_press")) {
                    b = 14;
                    break;
                }
                break;
            case 58027029:
                if (str.equals("safe_boot_disallowed")) {
                    b = 9;
                    break;
                }
                break;
            case 258514750:
                if (str.equals("screen_off_timeout")) {
                    b = 21;
                    break;
                }
                break;
            case 720635155:
                if (str.equals("package_verifier_enable")) {
                    b = 4;
                    break;
                }
                break;
            case 926123534:
                if (str.equals("airplane_mode_on")) {
                    b = 10;
                    break;
                }
                break;
            case 1073289638:
                if (str.equals("doze_pulse_on_double_tap")) {
                    b = UsbDescriptor.DESCRIPTORTYPE_BOS;
                    break;
                }
                break;
            case 1275530062:
                if (str.equals("auto_time_zone")) {
                    b = 20;
                    break;
                }
                break;
            case 1307734371:
                if (str.equals("location_global_kill_switch")) {
                    b = UsbDescriptor.DESCRIPTORTYPE_CAPABILITY;
                    break;
                }
                break;
            case 1602982312:
                if (str.equals("doze_pulse_on_pick_up")) {
                    b = UsbACInterface.ACI_SAMPLE_RATE_CONVERTER;
                    break;
                }
                break;
            case 1646894952:
                if (str.equals("always_on_vpn_lockdown")) {
                    b = 8;
                    break;
                }
                break;
            case 1661297501:
                if (str.equals("auto_time")) {
                    b = 19;
                    break;
                }
                break;
            case 1701140351:
                if (str.equals("install_non_market_apps")) {
                    b = 2;
                    break;
                }
                break;
            case 1735689732:
                if (str.equals("screen_brightness")) {
                    b = 17;
                    break;
                }
                break;
        }
        switch (b) {
            case 0:
                if (userManager.hasUserRestriction("no_config_location", UserHandle.of(i)) && i2 != 1000) {
                    return true;
                }
                if (String.valueOf(0).equals(str2)) {
                    return false;
                }
                str3 = "no_share_location";
                break;
                break;
            case 1:
                if (userManager.hasUserRestriction("no_config_location", UserHandle.of(i)) && i2 != 1000) {
                    return true;
                }
                if (str2 != null && str2.startsWith("-")) {
                    return false;
                }
                str3 = "no_share_location";
                break;
                break;
            case 2:
                if ("0".equals(str2)) {
                    return false;
                }
                str3 = "no_install_unknown_sources";
                break;
            case 3:
                if ("0".equals(str2)) {
                    return false;
                }
                str3 = "no_debugging_features";
                break;
            case 4:
            case 5:
                if ("1".equals(str2)) {
                    return false;
                }
                str3 = "ensure_verify_apps";
                break;
            case 6:
                str3 = "no_config_mobile_networks";
                break;
            case 7:
            case 8:
                int appId = UserHandle.getAppId(i2);
                if (appId == 1000 || appId == 0) {
                    return false;
                }
                str3 = "no_config_vpn";
                break;
            case 9:
                if ("1".equals(str2)) {
                    return false;
                }
                str3 = "no_safe_boot";
                break;
            case 10:
                if ("0".equals(str2)) {
                    return false;
                }
                str3 = "no_airplane_mode";
                break;
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                if ("0".equals(str2)) {
                    return false;
                }
                str3 = "no_ambient_display";
                break;
            case 16:
                if ("0".equals(str2)) {
                    return false;
                }
                str3 = "no_config_location";
                z = true;
                break;
            case 17:
            case 18:
                if (i2 == 1000) {
                    return false;
                }
                str3 = "no_config_brightness";
                break;
            case WindowManagerService.H.REPORT_WINDOWS_CHANGE:
                DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
                if (devicePolicyManager != null && devicePolicyManager.getAutoTimeRequired() && "0".equals(str2)) {
                    return true;
                }
                if (i2 == 1000) {
                    return false;
                }
                str3 = "no_config_date_time";
                break;
            case 20:
                if (i2 == 1000) {
                    return false;
                }
                str3 = "no_config_date_time";
                break;
            case BackupHandler.MSG_OP_COMPLETE:
                if (i2 == 1000) {
                    return false;
                }
                str3 = "no_config_screen_timeout";
                break;
            default:
                if (!str.startsWith("data_roaming") || "0".equals(str2)) {
                    return false;
                }
                str3 = "no_data_roaming";
                break;
        }
        if (z) {
            return userManager.hasUserRestrictionOnAnyUser(str3);
        }
        return userManager.hasUserRestriction(str3, UserHandle.of(i));
    }

    public static void dumpRestrictions(PrintWriter printWriter, String str, Bundle bundle) {
        if (bundle != null) {
            boolean z = true;
            for (String str2 : bundle.keySet()) {
                if (bundle.getBoolean(str2, false)) {
                    printWriter.println(str + str2);
                    z = false;
                }
            }
            if (z) {
                printWriter.println(str + "none");
                return;
            }
            return;
        }
        printWriter.println(str + "null");
    }

    public static void moveRestriction(String str, SparseArray<Bundle> sparseArray, SparseArray<Bundle> sparseArray2) {
        int i = 0;
        while (i < sparseArray.size()) {
            int iKeyAt = sparseArray.keyAt(i);
            Bundle bundleValueAt = sparseArray.valueAt(i);
            if (contains(bundleValueAt, str)) {
                bundleValueAt.remove(str);
                Bundle bundle = sparseArray2.get(iKeyAt);
                if (bundle == null) {
                    bundle = new Bundle();
                    sparseArray2.append(iKeyAt, bundle);
                }
                bundle.putBoolean(str, true);
                if (bundleValueAt.isEmpty()) {
                    sparseArray.removeAt(i);
                    i--;
                }
            }
            i++;
        }
    }

    public static boolean restrictionsChanged(Bundle bundle, Bundle bundle2, String... strArr) {
        if (strArr.length == 0) {
            return areEqual(bundle, bundle2);
        }
        for (String str : strArr) {
            if (bundle.getBoolean(str, false) != bundle2.getBoolean(str, false)) {
                return true;
            }
        }
        return false;
    }
}
