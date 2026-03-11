package com.android.settings.vpn2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.security.KeyStore;

public class VpnUtils {
    public static String getLockdownVpn() {
        byte[] value = KeyStore.getInstance().get("LOCKDOWN_VPN");
        if (value == null) {
            return null;
        }
        return new String(value);
    }

    public static void clearLockdownVpn(Context context) {
        KeyStore.getInstance().delete("LOCKDOWN_VPN");
        ((ConnectivityManager) context.getSystemService(ConnectivityManager.class)).updateLockdownVpn();
    }

    public static void setLockdownVpn(Context context, String lockdownKey) {
        KeyStore.getInstance().put("LOCKDOWN_VPN", lockdownKey.getBytes(), -1, 0);
        ((ConnectivityManager) context.getSystemService(ConnectivityManager.class)).updateLockdownVpn();
    }

    public static boolean isVpnLockdown(String key) {
        return key.equals(getLockdownVpn());
    }
}
