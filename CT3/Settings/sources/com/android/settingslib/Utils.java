package com.android.settingslib;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.UserManager;
import com.android.internal.util.UserIcons;
import com.android.settingslib.drawable.UserIconDrawable;
import java.text.NumberFormat;

public class Utils {
    private static String sPermissionControllerPackageName;
    private static String sServicesSystemSharedLibPackageName;
    private static String sSharedSystemSharedLibPackageName;
    private static Signature[] sSystemSignature;

    public static int getTetheringLabel(ConnectivityManager cm) {
        String[] usbRegexs = cm.getTetherableUsbRegexs();
        String[] wifiRegexs = cm.getTetherableWifiRegexs();
        String[] bluetoothRegexs = cm.getTetherableBluetoothRegexs();
        boolean usbAvailable = usbRegexs.length != 0;
        boolean wifiAvailable = wifiRegexs.length != 0;
        boolean bluetoothAvailable = bluetoothRegexs.length != 0;
        if (wifiAvailable && usbAvailable && bluetoothAvailable) {
            return R$string.tether_settings_title_all;
        }
        if (wifiAvailable && usbAvailable) {
            return R$string.tether_settings_title_all;
        }
        if (wifiAvailable && bluetoothAvailable) {
            return R$string.tether_settings_title_all;
        }
        if (wifiAvailable) {
            return R$string.tether_settings_title_wifi;
        }
        if (usbAvailable && bluetoothAvailable) {
            return R$string.tether_settings_title_usb_bluetooth;
        }
        if (usbAvailable) {
            return R$string.tether_settings_title_usb;
        }
        return R$string.tether_settings_title_bluetooth;
    }

    public static String getUserLabel(Context context, UserInfo info) {
        String string = info != null ? info.name : null;
        if (info.isManagedProfile()) {
            return context.getString(R$string.managed_user_title);
        }
        if (info.isGuest()) {
            string = context.getString(R$string.user_guest);
        }
        if (string == null && info != null) {
            string = Integer.toString(info.id);
        } else if (info == null) {
            string = context.getString(R$string.unknown);
        }
        return context.getResources().getString(R$string.running_process_item_user_label, string);
    }

    public static UserIconDrawable getUserIcon(Context context, UserManager um, UserInfo user) {
        Bitmap icon;
        int iconSize = UserIconDrawable.getSizeForList(context);
        if (user.isManagedProfile()) {
            Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.drawable.dropdown_ic_arrow_focused_holo_dark);
            return new UserIconDrawable(iconSize).setIcon(b).bake();
        }
        if (user.iconPath != null && (icon = um.getUserIcon(user.id)) != null) {
            return new UserIconDrawable(iconSize).setIcon(icon).bake();
        }
        return new UserIconDrawable(iconSize).setIconDrawable(UserIcons.getDefaultUserIcon(user.id, false)).bake();
    }

    public static String formatPercentage(long amount, long total) {
        return formatPercentage(amount / total);
    }

    public static String formatPercentage(int percentage) {
        return formatPercentage(((double) percentage) / 100.0d);
    }

    private static String formatPercentage(double percentage) {
        return NumberFormat.getPercentInstance().format(percentage);
    }

    public static int getBatteryLevel(Intent batteryChangedIntent) {
        int level = batteryChangedIntent.getIntExtra("level", 0);
        int scale = batteryChangedIntent.getIntExtra("scale", 100);
        return (level * 100) / scale;
    }

    public static String getBatteryStatus(Resources res, Intent batteryChangedIntent) {
        return getBatteryStatus(res, batteryChangedIntent, false);
    }

    public static String getBatteryStatus(Resources res, Intent batteryChangedIntent, boolean shortString) {
        int resId;
        int plugType = batteryChangedIntent.getIntExtra("plugged", 0);
        int status = batteryChangedIntent.getIntExtra("status", 1);
        if (status == 2) {
            if (plugType == 1) {
                resId = shortString ? R$string.battery_info_status_charging_ac_short : R$string.battery_info_status_charging_ac;
            } else if (plugType == 2) {
                resId = shortString ? R$string.battery_info_status_charging_usb_short : R$string.battery_info_status_charging_usb;
            } else if (plugType == 4) {
                resId = shortString ? R$string.battery_info_status_charging_wireless_short : R$string.battery_info_status_charging_wireless;
            } else {
                resId = R$string.battery_info_status_charging;
            }
            String statusString = res.getString(resId);
            return statusString;
        }
        if (status == 3) {
            String statusString2 = res.getString(R$string.battery_info_status_discharging);
            return statusString2;
        }
        if (status == 4) {
            String statusString3 = res.getString(R$string.battery_info_status_not_charging);
            return statusString3;
        }
        if (status == 5) {
            String statusString4 = res.getString(R$string.battery_info_status_full);
            return statusString4;
        }
        String statusString5 = res.getString(R$string.battery_info_status_unknown);
        return statusString5;
    }

    public static boolean isSystemPackage(PackageManager pm, PackageInfo pkg) {
        if (sSystemSignature == null) {
            sSystemSignature = new Signature[]{getSystemSignature(pm)};
        }
        if (sPermissionControllerPackageName == null) {
            sPermissionControllerPackageName = pm.getPermissionControllerPackageName();
        }
        if (sServicesSystemSharedLibPackageName == null) {
            sServicesSystemSharedLibPackageName = pm.getServicesSystemSharedLibraryPackageName();
        }
        if (sSharedSystemSharedLibPackageName == null) {
            sSharedSystemSharedLibPackageName = pm.getSharedSystemSharedLibraryPackageName();
        }
        if ((sSystemSignature[0] != null && sSystemSignature[0].equals(getFirstSignature(pkg))) || pkg.packageName.equals(sPermissionControllerPackageName) || pkg.packageName.equals(sServicesSystemSharedLibPackageName)) {
            return true;
        }
        return pkg.packageName.equals(sSharedSystemSharedLibPackageName);
    }

    private static Signature getFirstSignature(PackageInfo pkg) {
        if (pkg == null || pkg.signatures == null || pkg.signatures.length <= 0) {
            return null;
        }
        return pkg.signatures[0];
    }

    private static Signature getSystemSignature(PackageManager pm) {
        try {
            PackageInfo sys = pm.getPackageInfo("android", 64);
            return getFirstSignature(sys);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
