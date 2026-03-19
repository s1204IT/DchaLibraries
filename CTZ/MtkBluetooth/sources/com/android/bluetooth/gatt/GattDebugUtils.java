package com.android.bluetooth.gatt;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import java.util.UUID;

class GattDebugUtils {
    private static final String ACTION_GATT_PAIRING_CONFIG = "android.bluetooth.action.GATT_PAIRING_CONFIG";
    private static final String ACTION_GATT_TEST_CONNECT = "android.bluetooth.action.GATT_TEST_CONNECT";
    private static final String ACTION_GATT_TEST_DISCONNECT = "android.bluetooth.action.GATT_TEST_DISCONNECT";
    private static final String ACTION_GATT_TEST_DISCOVER = "android.bluetooth.action.GATT_TEST_DISCOVER";
    private static final String ACTION_GATT_TEST_ENABLE = "android.bluetooth.action.GATT_TEST_ENABLE";
    private static final String ACTION_GATT_TEST_USAGE = "android.bluetooth.action.GATT_TEST_USAGE";
    private static final boolean DEBUG_ADMIN = true;
    private static final String EXTRA_ADDRESS = "address";
    private static final String EXTRA_ADDR_TYPE = "addr_type";
    private static final String EXTRA_AUTH_REQ = "auth_req";
    private static final String EXTRA_EHANDLE = "end";
    private static final String EXTRA_ENABLE = "enable";
    private static final String EXTRA_INIT_KEY = "init_key";
    private static final String EXTRA_IO_CAP = "io_cap";
    private static final String EXTRA_MAX_KEY = "max_key";
    private static final String EXTRA_RESP_KEY = "resp_key";
    private static final String EXTRA_SHANDLE = "start";
    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_UUID = "uuid";
    private static final String TAG = "BtGatt.DebugUtils";

    GattDebugUtils() {
    }

    static boolean handleDebugAction(GattService gattService, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "handleDebugAction() action=" + action);
        if (ACTION_GATT_TEST_USAGE.equals(action)) {
            logUsageInfo();
        } else if (ACTION_GATT_TEST_ENABLE.equals(action)) {
            gattService.gattTestCommand(1, null, null, intent.getBooleanExtra(EXTRA_ENABLE, true) ? 1 : 0, 0, 0, 0, 0);
        } else if (ACTION_GATT_TEST_CONNECT.equals(action)) {
            gattService.gattTestCommand(2, null, intent.getStringExtra(EXTRA_ADDRESS), intent.getIntExtra("type", 2), intent.getIntExtra(EXTRA_ADDR_TYPE, 0), 0, 0, 0);
        } else if (ACTION_GATT_TEST_DISCONNECT.equals(action)) {
            gattService.gattTestCommand(3, null, null, 0, 0, 0, 0, 0);
        } else if (ACTION_GATT_TEST_DISCOVER.equals(action)) {
            gattService.gattTestCommand(4, getUuidExtra(intent), null, intent.getIntExtra("type", 1), getHandleExtra(intent, EXTRA_SHANDLE, 1), getHandleExtra(intent, EXTRA_EHANDLE, 65535), 0, 0);
        } else {
            if (!ACTION_GATT_PAIRING_CONFIG.equals(action)) {
                return false;
            }
            gattService.gattTestCommand(240, null, null, intent.getIntExtra(EXTRA_AUTH_REQ, 5), intent.getIntExtra(EXTRA_IO_CAP, 4), intent.getIntExtra(EXTRA_INIT_KEY, 7), intent.getIntExtra(EXTRA_RESP_KEY, 7), intent.getIntExtra(EXTRA_MAX_KEY, 16));
        }
        return true;
    }

    private static int getHandleExtra(Intent intent, String str, int i) {
        Bundle extras = intent.getExtras();
        Object obj = extras != null ? extras.get(str) : null;
        if (obj != null && obj.getClass().getName().equals("java.lang.String")) {
            try {
                return Integer.parseInt(extras.getString(str), 16);
            } catch (NumberFormatException e) {
                return i;
            }
        }
        return intent.getIntExtra(str, i);
    }

    private static UUID getUuidExtra(Intent intent) {
        String stringExtra = intent.getStringExtra(EXTRA_UUID);
        if (stringExtra != null && stringExtra.length() == 4) {
            stringExtra = String.format("0000%s-0000-1000-8000-00805f9b34fb", stringExtra);
        }
        if (stringExtra != null) {
            return UUID.fromString(stringExtra);
        }
        return null;
    }

    private static void logUsageInfo() {
        Log.i(TAG, "------------ GATT TEST ACTIONS  ----------------\nGATT_TEST_ENABLE\n  [--ez enable <bool>] Enable or disable,\n                       defaults to true (enable).\n\nGATT_TEST_CONNECT\n   --es address <bda>\n  [--ei addr_type <type>] Possible values:\n                         0 = Static (default)\n                         1 = Random\n\n  [--ei type <type>]   Default is 2 (LE Only)\n\nGATT_TEST_DISCONNECT\n\nGATT_TEST_DISCOVER\n  [--ei type <type>]   Possible values:\n                         1 = Discover all services (default)\n                         2 = Discover services by UUID\n                         3 = Discover included services\n                         4 = Discover characteristics\n                         5 = Discover descriptors\n\n  [--es uuid <uuid>]   Optional; Can be either full 128-bit\n                       UUID hex string, or 4 hex characters\n                       for 16-bit UUIDs.\n\n  [--ei start <hdl>]   Start of handle range (default 1)\n  [--ei end <hdl>]     End of handle range (default 65355)\n    or\n  [--es start <hdl>]   Start of handle range (hex format)\n  [--es end <hdl>]     End of handle range (hex format)\n\nGATT_PAIRING_CONFIG\n  [--ei auth_req]      Authentication flag (default 5)\n  [--ei io_cap]        IO capabilities (default 4)\n  [--ei init_key]      Initial key size (default 7)\n  [--ei resp_key]      Response key size (default 7)\n  [--ei max_key]       Maximum key size (default 16)\n------------------------------------------------");
    }
}
