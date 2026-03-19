package com.android.bluetooth.pbapclient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothPbapClient;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.CallLog;
import android.provider.Settings;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.ProfileService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PbapClientService extends ProfileService {
    private static final boolean DBG = false;
    private static final int MAXIMUM_DEVICES = 10;
    private static final String TAG = "PbapClientService";
    private static PbapClientService sPbapClientService;
    private Map<BluetoothDevice, PbapClientStateMachine> mPbapClientStateMachineMap = new ConcurrentHashMap();
    private PbapBroadcastReceiver mPbapBroadcastReceiver = new PbapBroadcastReceiver();

    @Override
    public ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothPbapClientBinder(this);
    }

    @Override
    protected boolean start() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        try {
            registerReceiver(this.mPbapBroadcastReceiver, intentFilter);
        } catch (Exception e) {
            Log.w(TAG, "Unable to register pbapclient receiver", e);
        }
        removeUncleanAccounts();
        setPbapClientService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        try {
            unregisterReceiver(this.mPbapBroadcastReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Unable to unregister pbapclient receiver", e);
        }
        Iterator<PbapClientStateMachine> it = this.mPbapClientStateMachineMap.values().iterator();
        while (it.hasNext()) {
            it.next().doQuit();
        }
        return true;
    }

    @Override
    protected void cleanup() {
        removeUncleanAccounts();
        setPbapClientService(null);
    }

    void cleanupDevice(BluetoothDevice bluetoothDevice) {
        Log.w(TAG, "Cleanup device: " + bluetoothDevice);
        synchronized (this.mPbapClientStateMachineMap) {
            if (this.mPbapClientStateMachineMap.get(bluetoothDevice) != null) {
                this.mPbapClientStateMachineMap.remove(bluetoothDevice);
            }
        }
    }

    private void removeUncleanAccounts() {
        AccountManager accountManager = AccountManager.get(this);
        Account[] accountsByType = accountManager.getAccountsByType(getString(R.string.pbap_account_type));
        Log.w(TAG, "Found " + accountsByType.length + " unclean accounts");
        for (Account account : accountsByType) {
            Log.w(TAG, "Deleting " + account);
            accountManager.removeAccountExplicitly(account);
        }
        try {
            getContentResolver().delete(CallLog.Calls.CONTENT_URI, null, null);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Call Logs could not be deleted, they may not exist yet.");
        }
    }

    private class PbapBroadcastReceiver extends BroadcastReceiver {
        private PbapBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(PbapClientService.TAG, "onReceive" + action);
            if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED")) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (PbapClientService.this.getConnectionState(bluetoothDevice) == 2) {
                    PbapClientService.this.disconnect(bluetoothDevice);
                    return;
                }
                return;
            }
            if (action.equals("android.intent.action.USER_UNLOCKED")) {
                Iterator it = PbapClientService.this.mPbapClientStateMachineMap.values().iterator();
                while (it.hasNext()) {
                    ((PbapClientStateMachine) it.next()).resumeDownload();
                }
            }
        }
    }

    private static class BluetoothPbapClientBinder extends IBluetoothPbapClient.Stub implements ProfileService.IProfileServiceBinder {
        private PbapClientService mService;

        BluetoothPbapClientBinder(PbapClientService pbapClientService) {
            this.mService = pbapClientService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        private PbapClientService getService() {
            if (!Utils.checkCaller()) {
                Log.w(PbapClientService.TAG, "PbapClient call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        public boolean connect(BluetoothDevice bluetoothDevice) {
            PbapClientService service = getService();
            if (service == null) {
                Log.e(PbapClientService.TAG, "PbapClient Binder connect no service");
                return false;
            }
            return service.connect(bluetoothDevice);
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            PbapClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(bluetoothDevice);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            PbapClientService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            PbapClientService service = getService();
            if (service != null) {
                return service.getDevicesMatchingConnectionStates(iArr);
            }
            return new ArrayList(0);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            PbapClientService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
            PbapClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(bluetoothDevice, i);
        }

        public int getPriority(BluetoothDevice bluetoothDevice) {
            PbapClientService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(bluetoothDevice);
        }
    }

    public static synchronized PbapClientService getPbapClientService() {
        if (sPbapClientService == null) {
            Log.w(TAG, "getPbapClientService(): service is null");
            return null;
        }
        if (!sPbapClientService.isAvailable()) {
            Log.w(TAG, "getPbapClientService(): service is not available");
            return null;
        }
        return sPbapClientService;
    }

    private static synchronized void setPbapClientService(PbapClientService pbapClientService) {
        sPbapClientService = pbapClientService;
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            throw new IllegalArgumentException("Null device");
        }
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        Log.d(TAG, "Received request to ConnectPBAPPhonebook " + bluetoothDevice.getAddress());
        if (getPriority(bluetoothDevice) <= 0) {
            return false;
        }
        synchronized (this.mPbapClientStateMachineMap) {
            if (this.mPbapClientStateMachineMap.get(bluetoothDevice) == null && this.mPbapClientStateMachineMap.size() < 10) {
                PbapClientStateMachine pbapClientStateMachine = new PbapClientStateMachine(this, bluetoothDevice);
                pbapClientStateMachine.start();
                this.mPbapClientStateMachineMap.put(bluetoothDevice, pbapClientStateMachine);
                return true;
            }
            Log.w(TAG, "Received connect request while already connecting/connected.");
            return false;
        }
    }

    boolean disconnect(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            throw new IllegalArgumentException("Null device");
        }
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        PbapClientStateMachine pbapClientStateMachine = this.mPbapClientStateMachineMap.get(bluetoothDevice);
        if (pbapClientStateMachine != null) {
            pbapClientStateMachine.disconnect(bluetoothDevice);
            return true;
        }
        Log.w(TAG, "disconnect() called on unconnected device.");
        return false;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return getDevicesMatchingConnectionStates(new int[]{2});
    }

    private List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        ArrayList arrayList = new ArrayList(0);
        for (Map.Entry<BluetoothDevice, PbapClientStateMachine> entry : this.mPbapClientStateMachineMap.entrySet()) {
            int connectionState = entry.getValue().getConnectionState();
            int length = iArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                if (connectionState != iArr[i]) {
                    i++;
                } else {
                    arrayList.add(entry.getKey());
                    break;
                }
            }
        }
        return arrayList;
    }

    int getConnectionState(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            throw new IllegalArgumentException("Null device");
        }
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        PbapClientStateMachine pbapClientStateMachine = this.mPbapClientStateMachineMap.get(bluetoothDevice);
        if (pbapClientStateMachine == null) {
            return 0;
        }
        return pbapClientStateMachine.getConnectionState(bluetoothDevice);
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        if (bluetoothDevice == null) {
            throw new IllegalArgumentException("Null device");
        }
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothPbapClientPriorityKey(bluetoothDevice.getAddress()), i);
        return true;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            throw new IllegalArgumentException("Null device");
        }
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothPbapClientPriorityKey(bluetoothDevice.getAddress()), -1);
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        Iterator<PbapClientStateMachine> it = this.mPbapClientStateMachineMap.values().iterator();
        while (it.hasNext()) {
            it.next().dump(sb);
        }
    }
}
