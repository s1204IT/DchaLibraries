package com.android.settings.bluetooth;

import android.R;
import android.app.QueuedWork;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

final class LocalBluetoothPreferences {
    private LocalBluetoothPreferences() {
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("bluetooth_settings", 0);
    }

    static long getDiscoverableEndTimestamp(Context context) {
        return getSharedPreferences(context).getLong("discoverable_end_timestamp", 0L);
    }

    static boolean shouldShowDialogInForeground(Context context, String deviceAddress, String deviceName) {
        LocalBluetoothManager manager = Utils.getLocalBtManager(context);
        if (manager == null) {
            Log.v("LocalBluetoothPreferences", "manager == null - do not show dialog.");
            return false;
        }
        if (manager.isForegroundActivity()) {
            return true;
        }
        if ((context.getResources().getConfiguration().uiMode & 5) == 5) {
            Log.v("LocalBluetoothPreferences", "in appliance mode - do not show dialog.");
            return false;
        }
        long currentTimeMillis = System.currentTimeMillis();
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        long lastDiscoverableEndTime = sharedPreferences.getLong("discoverable_end_timestamp", 0L);
        if (60000 + lastDiscoverableEndTime > currentTimeMillis) {
            return true;
        }
        LocalBluetoothAdapter adapter = manager.getBluetoothAdapter();
        if ((adapter != null && adapter.isDiscovering()) || sharedPreferences.getLong("last_discovering_time", 0L) + 60000 > currentTimeMillis) {
            return true;
        }
        if (deviceAddress != null) {
            String lastSelectedDevice = sharedPreferences.getString("last_selected_device", null);
            if (deviceAddress.equals(lastSelectedDevice)) {
                long lastDeviceSelectedTime = sharedPreferences.getLong("last_selected_device_time", 0L);
                if (60000 + lastDeviceSelectedTime > currentTimeMillis) {
                    return true;
                }
            }
        }
        if (!TextUtils.isEmpty(deviceName)) {
            String packagedKeyboardName = context.getString(R.string.PERSOSUBSTATE_RUIM_HRPD_IN_PROGRESS);
            if (deviceName.equals(packagedKeyboardName)) {
                Log.v("LocalBluetoothPreferences", "showing dialog for packaged keyboard");
                return true;
            }
        }
        Log.v("LocalBluetoothPreferences", "Found no reason to show the dialog - do not show dialog.");
        return false;
    }

    static void persistSelectedDeviceInPicker(Context context, String deviceAddress) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString("last_selected_device", deviceAddress);
        editor.putLong("last_selected_device_time", System.currentTimeMillis());
        editor.apply();
    }

    static void persistDiscoverableEndTimestamp(Context context, long endTimestamp) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putLong("discoverable_end_timestamp", endTimestamp);
        editor.apply();
    }

    static void persistDiscoveringTimestamp(final Context context) {
        QueuedWork.singleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                SharedPreferences.Editor editor = LocalBluetoothPreferences.getSharedPreferences(context).edit();
                editor.putLong("last_discovering_time", System.currentTimeMillis());
                editor.apply();
            }
        });
    }

    static boolean hasDockAutoConnectSetting(Context context, String addr) {
        return getSharedPreferences(context).contains("auto_connect_to_dock" + addr);
    }

    static boolean getDockAutoConnectSetting(Context context, String addr) {
        return getSharedPreferences(context).getBoolean("auto_connect_to_dock" + addr, false);
    }

    static void saveDockAutoConnectSetting(Context context, String addr, boolean autoConnect) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean("auto_connect_to_dock" + addr, autoConnect);
        editor.apply();
    }

    static void removeDockAutoConnectSetting(Context context, String addr) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.remove("auto_connect_to_dock" + addr);
        editor.apply();
    }
}
