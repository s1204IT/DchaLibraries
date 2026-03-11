package com.android.settingslib;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.UserIcons;
import com.android.settingslib.drawable.UserIconDrawable;
import com.android.settingslib.wrapper.LocationManagerWrapper;
import java.text.NumberFormat;

public class Utils {

    @VisibleForTesting
    static final String STORAGE_MANAGER_SHOW_OPT_IN_PROPERTY = "ro.storage_manager.show_opt_in";
    static final int[] WIFI_PIE = {android.R.drawable.ic_media_route_connecting_dark_27_mtrl, android.R.drawable.ic_media_route_connecting_dark_28_mtrl, android.R.drawable.ic_media_route_connecting_dark_29_mtrl, android.R.drawable.ic_media_route_connecting_dark_30_mtrl, android.R.drawable.ic_media_route_connecting_dark_material};
    private static String sPermissionControllerPackageName;
    private static String sServicesSystemSharedLibPackageName;
    private static String sSharedSystemSharedLibPackageName;
    private static Signature[] sSystemSignature;

    public static void updateLocationEnabled(Context context, boolean z, int i, int i2) {
        Settings.Secure.putIntForUser(context.getContentResolver(), "location_changer", i2, i);
        Intent intent = new Intent("com.android.settings.location.MODE_CHANGING");
        int intForUser = Settings.Secure.getIntForUser(context.getContentResolver(), "location_mode", 0, i);
        int i3 = z ? 3 : 0;
        intent.putExtra("CURRENT_MODE", intForUser);
        intent.putExtra("NEW_MODE", i3);
        context.sendBroadcastAsUser(intent, UserHandle.of(i), "android.permission.WRITE_SECURE_SETTINGS");
        new LocationManagerWrapper((LocationManager) context.getSystemService("location")).setLocationEnabledForUser(z, UserHandle.of(i));
    }

    public static int getTetheringLabel(ConnectivityManager connectivityManager) {
        String[] tetherableUsbRegexs = connectivityManager.getTetherableUsbRegexs();
        String[] tetherableWifiRegexs = connectivityManager.getTetherableWifiRegexs();
        String[] tetherableBluetoothRegexs = connectivityManager.getTetherableBluetoothRegexs();
        boolean z = tetherableUsbRegexs.length != 0;
        boolean z2 = tetherableWifiRegexs.length != 0;
        boolean z3 = tetherableBluetoothRegexs.length != 0;
        if (z2 && z && z3) {
            return R.string.tether_settings_title_all;
        }
        if (z2 && z) {
            return R.string.tether_settings_title_all;
        }
        if (z2 && z3) {
            return R.string.tether_settings_title_all;
        }
        if (z2) {
            return R.string.tether_settings_title_wifi;
        }
        if (z && z3) {
            return R.string.tether_settings_title_usb_bluetooth;
        }
        if (z) {
            return R.string.tether_settings_title_usb;
        }
        return R.string.tether_settings_title_bluetooth;
    }

    public static String getUserLabel(Context context, UserInfo userInfo) {
        String string = userInfo != null ? userInfo.name : null;
        if (userInfo.isManagedProfile()) {
            return context.getString(R.string.managed_user_title);
        }
        if (userInfo.isGuest()) {
            string = context.getString(R.string.user_guest);
        }
        if (string == null && userInfo != null) {
            string = Integer.toString(userInfo.id);
        } else if (userInfo == null) {
            string = context.getString(R.string.unknown);
        }
        return context.getResources().getString(R.string.running_process_item_user_label, string);
    }

    public static Drawable getUserIcon(Context context, UserManager userManager, UserInfo userInfo) {
        Bitmap userIcon;
        int sizeForList = UserIconDrawable.getSizeForList(context);
        if (userInfo.isManagedProfile()) {
            Drawable managedUserDrawable = UserIconDrawable.getManagedUserDrawable(context);
            managedUserDrawable.setBounds(0, 0, sizeForList, sizeForList);
            return managedUserDrawable;
        }
        if (userInfo.iconPath != null && (userIcon = userManager.getUserIcon(userInfo.id)) != null) {
            return new UserIconDrawable(sizeForList).setIcon(userIcon).bake();
        }
        return new UserIconDrawable(sizeForList).setIconDrawable(UserIcons.getDefaultUserIcon(context.getResources(), userInfo.id, false)).bake();
    }

    public static String formatPercentage(double d, boolean z) {
        return formatPercentage(z ? Math.round((float) d) : (int) d);
    }

    public static String formatPercentage(long j, long j2) {
        return formatPercentage(j / j2);
    }

    public static String formatPercentage(int i) {
        return formatPercentage(((double) i) / 100.0d);
    }

    public static String formatPercentage(double d) {
        return NumberFormat.getPercentInstance().format(d);
    }

    public static int getBatteryLevel(Intent intent) {
        return (intent.getIntExtra("level", 0) * 100) / intent.getIntExtra("scale", 100);
    }

    public static String getBatteryStatus(Resources resources, Intent intent) {
        int intExtra = intent.getIntExtra("status", 1);
        if (intExtra == 2) {
            return resources.getString(R.string.battery_info_status_charging);
        }
        if (intExtra == 3) {
            return resources.getString(R.string.battery_info_status_discharging);
        }
        if (intExtra == 4) {
            return resources.getString(R.string.battery_info_status_not_charging);
        }
        if (intExtra == 5) {
            return resources.getString(R.string.battery_info_status_full);
        }
        return resources.getString(R.string.battery_info_status_unknown);
    }

    public static int getColorAccent(Context context) {
        return getColorAttr(context, android.R.attr.colorAccent);
    }

    public static int getDefaultColor(Context context, int i) {
        return context.getResources().getColorStateList(i, context.getTheme()).getDefaultColor();
    }

    public static int getColorAttr(Context context, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        int color = typedArrayObtainStyledAttributes.getColor(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        return color;
    }

    public static boolean isSystemPackage(Resources resources, PackageManager packageManager, PackageInfo packageInfo) {
        if (sSystemSignature == null) {
            sSystemSignature = new Signature[]{getSystemSignature(packageManager)};
        }
        if (sPermissionControllerPackageName == null) {
            sPermissionControllerPackageName = packageManager.getPermissionControllerPackageName();
        }
        if (sServicesSystemSharedLibPackageName == null) {
            sServicesSystemSharedLibPackageName = packageManager.getServicesSystemSharedLibraryPackageName();
        }
        if (sSharedSystemSharedLibPackageName == null) {
            sSharedSystemSharedLibPackageName = packageManager.getSharedSystemSharedLibraryPackageName();
        }
        return (sSystemSignature[0] != null && sSystemSignature[0].equals(getFirstSignature(packageInfo))) || packageInfo.packageName.equals(sPermissionControllerPackageName) || packageInfo.packageName.equals(sServicesSystemSharedLibPackageName) || packageInfo.packageName.equals(sSharedSystemSharedLibPackageName) || packageInfo.packageName.equals("com.android.printspooler") || isDeviceProvisioningPackage(resources, packageInfo.packageName);
    }

    private static Signature getFirstSignature(PackageInfo packageInfo) {
        if (packageInfo != null && packageInfo.signatures != null && packageInfo.signatures.length > 0) {
            return packageInfo.signatures[0];
        }
        return null;
    }

    private static Signature getSystemSignature(PackageManager packageManager) {
        try {
            return getFirstSignature(packageManager.getPackageInfo("android", 64));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static boolean isDeviceProvisioningPackage(Resources resources, String str) {
        String string = resources.getString(android.R.string.activity_resolver_use_once);
        return string != null && string.equals(str);
    }

    public static int getWifiIconResource(int i) {
        if (i < 0 || i >= WIFI_PIE.length) {
            throw new IllegalArgumentException("No Wifi icon found for level: " + i);
        }
        return WIFI_PIE[i];
    }

    public static int getDefaultStorageManagerDaysToRetain(Resources resources) {
        try {
            return resources.getInteger(android.R.integer.config_dynamicPowerSavingsDefaultDisableThreshold);
        } catch (Resources.NotFoundException e) {
            return 90;
        }
    }

    public static boolean isWifiOnly(Context context) {
        return !((ConnectivityManager) context.getSystemService(ConnectivityManager.class)).isNetworkSupported(0);
    }

    public static boolean isStorageManagerEnabled(Context context) {
        boolean z;
        try {
            z = !SystemProperties.getBoolean(STORAGE_MANAGER_SHOW_OPT_IN_PROPERTY, true);
        } catch (Resources.NotFoundException e) {
            z = false;
        }
        return Settings.Secure.getInt(context.getContentResolver(), "automatic_storage_manager_enabled", z ? 1 : 0) != 0;
    }

    public static boolean isAudioModeOngoingCall(Context context) {
        int mode = ((AudioManager) context.getSystemService(AudioManager.class)).getMode();
        return mode == 1 || mode == 2 || mode == 3;
    }
}
