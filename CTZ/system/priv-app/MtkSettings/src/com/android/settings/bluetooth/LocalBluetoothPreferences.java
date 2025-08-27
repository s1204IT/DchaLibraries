package com.android.settings.bluetooth;

import android.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/* loaded from: classes.dex */
final class LocalBluetoothPreferences {
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("bluetooth_settings", 0);
    }

    static boolean shouldShowDialogInForeground(Context context, String str, String str2) {
        LocalBluetoothManager localBtManager = Utils.getLocalBtManager(context);
        if (localBtManager == null) {
            Log.v("LocalBluetoothPreferences", "manager == null - do not show dialog.");
            return false;
        }
        if (localBtManager.isForegroundActivity()) {
            return true;
        }
        if ((context.getResources().getConfiguration().uiMode & 5) == 5) {
            Log.v("LocalBluetoothPreferences", "in appliance mode - do not show dialog.");
            return false;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences.getLong("discoverable_end_timestamp", 0L) + 60000 > jCurrentTimeMillis) {
            return true;
        }
        LocalBluetoothAdapter bluetoothAdapter = localBtManager.getBluetoothAdapter();
        if (bluetoothAdapter != null && (bluetoothAdapter.isDiscovering() || bluetoothAdapter.getDiscoveryEndMillis() + 60000 > jCurrentTimeMillis)) {
            return true;
        }
        if (str != null && str.equals(sharedPreferences.getString("last_selected_device", null)) && sharedPreferences.getLong("last_selected_device_time", 0L) + 60000 > jCurrentTimeMillis) {
            return true;
        }
        if (!TextUtils.isEmpty(str2) && str2.equals(context.getString(R.string.app_streaming_blocked_message))) {
            Log.v("LocalBluetoothPreferences", "showing dialog for packaged keyboard");
            return true;
        }
        Log.v("LocalBluetoothPreferences", "Found no reason to show the dialog - do not show dialog.");
        return false;
    }

    static void persistSelectedDeviceInPicker(Context context, String str) {
        SharedPreferences.Editor editorEdit = getSharedPreferences(context).edit();
        editorEdit.putString("last_selected_device", str);
        editorEdit.putLong("last_selected_device_time", System.currentTimeMillis());
        editorEdit.apply();
    }

    static void persistDiscoverableEndTimestamp(Context context, long j) {
        SharedPreferences.Editor editorEdit = getSharedPreferences(context).edit();
        editorEdit.putLong("discoverable_end_timestamp", j);
        editorEdit.apply();
    }
}
