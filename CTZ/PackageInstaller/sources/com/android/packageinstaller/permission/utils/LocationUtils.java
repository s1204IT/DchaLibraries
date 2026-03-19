package com.android.packageinstaller.permission.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.ILocationManager;
import android.os.BenesseExtension;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import com.android.packageinstaller.R;

public class LocationUtils {
    public static void showLocationDialog(final Context context, CharSequence charSequence) {
        new AlertDialog.Builder(context).setIcon(R.drawable.ic_dialog_alert_material).setTitle(android.R.string.dialog_alert_title).setMessage(context.getString(R.string.location_warning, charSequence)).setNegativeButton(R.string.ok, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.location_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (BenesseExtension.getDchaState() != 0) {
                    return;
                }
                context.startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            }
        }).show();
    }

    public static boolean isLocationEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "location_mode", 0) != 0;
    }

    public static boolean isLocationGroupAndProvider(String str, String str2) {
        return "android.permission-group.LOCATION".equals(str) && isNetworkLocationProvider(str2);
    }

    private static boolean isNetworkLocationProvider(String str) {
        try {
            return str.equals(ILocationManager.Stub.asInterface(ServiceManager.getService("location")).getNetworkProviderPackage());
        } catch (RemoteException e) {
            return false;
        }
    }
}
