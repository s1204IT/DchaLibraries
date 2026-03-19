package com.android.bluetooth.mapclient;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothMapClient;
import android.bluetooth.SdpMasRecord;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v4.media.MediaPlayer2;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.ProfileService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapClientService extends ProfileService {
    private static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    static final boolean DBG = false;
    static final int MAXIMUM_CONNECTED_DEVICES = 4;
    private static final String TAG = "MapClientService";
    static final boolean VDBG = false;
    private static MapClientService sMapClientService;
    private BluetoothAdapter mAdapter;
    private Map<BluetoothDevice, MceStateMachine> mMapInstanceMap = new ConcurrentHashMap(1);
    private MapBroadcastReceiver mMapReceiver = new MapBroadcastReceiver();
    private MnsService mMnsServer;

    public static synchronized MapClientService getMapClientService() {
        if (sMapClientService == null) {
            Log.w(TAG, "getMapClientService(): service is null");
            return null;
        }
        if (!sMapClientService.isAvailable()) {
            Log.w(TAG, "getMapClientService(): service is not available ");
            return null;
        }
        return sMapClientService;
    }

    private static synchronized void setMapClientService(MapClientService mapClientService) {
        sMapClientService = mapClientService;
    }

    @VisibleForTesting
    Map<BluetoothDevice, MceStateMachine> getInstanceMap() {
        return this.mMapInstanceMap;
    }

    public synchronized boolean connect(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            throw new IllegalArgumentException("Null device");
        }
        if (this.mMapInstanceMap.get(bluetoothDevice) == null) {
            if (this.mMapInstanceMap.size() < 4) {
                addDeviceToMapAndConnect(bluetoothDevice);
                return true;
            }
            removeUncleanAccounts();
            if (this.mMapInstanceMap.size() < 4) {
                addDeviceToMapAndConnect(bluetoothDevice);
                return true;
            }
            Log.e(TAG, "Maxed out on the number of allowed MAP connections. Connect request rejected on " + bluetoothDevice);
            return false;
        }
        int connectionState = getConnectionState(bluetoothDevice);
        if (connectionState != 2 && connectionState != 1) {
            this.mMapInstanceMap.remove(bluetoothDevice);
            addDeviceToMapAndConnect(bluetoothDevice);
            return true;
        }
        Log.w(TAG, "Received connect request while already connecting/connected.");
        return true;
    }

    private synchronized void addDeviceToMapAndConnect(BluetoothDevice bluetoothDevice) {
        this.mMapInstanceMap.put(bluetoothDevice, new MceStateMachine(this, bluetoothDevice));
    }

    public synchronized boolean disconnect(BluetoothDevice bluetoothDevice) {
        MceStateMachine mceStateMachine = this.mMapInstanceMap.get(bluetoothDevice);
        if (mceStateMachine == null) {
            return false;
        }
        int state = mceStateMachine.getState();
        if (state != 2 && state != 1) {
            return false;
        }
        mceStateMachine.disconnect();
        return true;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        return getDevicesMatchingConnectionStates(new int[]{2});
    }

    MceStateMachine getMceStateMachineForDevice(BluetoothDevice bluetoothDevice) {
        return this.mMapInstanceMap.get(bluetoothDevice);
    }

    public synchronized List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        ArrayList arrayList;
        Log.d(TAG, "getDevicesMatchingConnectionStates" + Arrays.toString(iArr));
        arrayList = new ArrayList();
        for (BluetoothDevice bluetoothDevice : this.mAdapter.getBondedDevices()) {
            int connectionState = getConnectionState(bluetoothDevice);
            Log.d(TAG, "Device: " + bluetoothDevice + "State: " + connectionState);
            for (int i : iArr) {
                if (connectionState == i) {
                    arrayList.add(bluetoothDevice);
                }
            }
        }
        Log.d(TAG, arrayList.toString());
        return arrayList;
    }

    public synchronized int getConnectionState(BluetoothDevice bluetoothDevice) {
        MceStateMachine mceStateMachine;
        mceStateMachine = this.mMapInstanceMap.get(bluetoothDevice);
        return mceStateMachine == null ? 0 : mceStateMachine.getState();
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothMapClientPriorityKey(bluetoothDevice.getAddress()), i);
        return true;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothMapClientPriorityKey(bluetoothDevice.getAddress()), -1);
    }

    public synchronized boolean sendMessage(BluetoothDevice bluetoothDevice, Uri[] uriArr, String str, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        boolean z;
        MceStateMachine mceStateMachine = this.mMapInstanceMap.get(bluetoothDevice);
        if (mceStateMachine != null) {
            z = mceStateMachine.sendMapMessage(uriArr, str, pendingIntent, pendingIntent2);
        }
        return z;
    }

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new Binder(this);
    }

    @Override
    protected boolean start() {
        Log.e(TAG, "start()");
        if (this.mMnsServer == null) {
            this.mMnsServer = MapUtils.newMnsServiceInstance(this);
            if (this.mMnsServer == null) {
                Log.w(TAG, "MnsService is *not* created!");
                return false;
            }
        }
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.SDP_RECORD");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        registerReceiver(this.mMapReceiver, intentFilter);
        removeUncleanAccounts();
        setMapClientService(this);
        return true;
    }

    @Override
    protected synchronized boolean stop() {
        unregisterReceiver(this.mMapReceiver);
        if (this.mMnsServer != null) {
            this.mMnsServer.stop();
        }
        for (MceStateMachine mceStateMachine : this.mMapInstanceMap.values()) {
            if (mceStateMachine.getState() == 2) {
                mceStateMachine.disconnect();
            }
            mceStateMachine.doQuit();
        }
        return true;
    }

    @Override
    protected void cleanup() {
        removeUncleanAccounts();
        setMapClientService(null);
    }

    void cleanupDevice(BluetoothDevice bluetoothDevice) {
        synchronized (this.mMapInstanceMap) {
            if (this.mMapInstanceMap.get(bluetoothDevice) != null) {
                this.mMapInstanceMap.remove(bluetoothDevice);
            }
        }
    }

    @VisibleForTesting
    void removeUncleanAccounts() {
        Iterator<Map.Entry<BluetoothDevice, MceStateMachine>> it = this.mMapInstanceMap.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().getState() == 0) {
                it.remove();
            }
        }
    }

    public synchronized boolean getUnreadMessages(BluetoothDevice bluetoothDevice) {
        MceStateMachine mceStateMachine = this.mMapInstanceMap.get(bluetoothDevice);
        if (mceStateMachine == null) {
            return false;
        }
        return mceStateMachine.getUnreadMessages();
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        ProfileService.println(sb, "# Services Connected: " + this.mMapInstanceMap.size());
        Iterator<MceStateMachine> it = this.mMapInstanceMap.values().iterator();
        while (it.hasNext()) {
            it.next().dump(sb);
        }
    }

    private static class Binder extends IBluetoothMapClient.Stub implements ProfileService.IProfileServiceBinder {
        private MapClientService mService;

        Binder(MapClientService mapClientService) {
            this.mService = mapClientService;
        }

        private MapClientService getService() {
            if (!Utils.checkCaller()) {
                Log.w(MapClientService.TAG, "MAP call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            this.mService.enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
            return this.mService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        public boolean isConnected(BluetoothDevice bluetoothDevice) {
            MapClientService service = getService();
            return service != null && service.getConnectionState(bluetoothDevice) == 2;
        }

        public boolean connect(BluetoothDevice bluetoothDevice) {
            MapClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.connect(bluetoothDevice);
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            MapClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(bluetoothDevice);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            MapClientService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            MapClientService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            MapClientService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
            MapClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(bluetoothDevice, i);
        }

        public int getPriority(BluetoothDevice bluetoothDevice) {
            MapClientService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(bluetoothDevice);
        }

        public boolean sendMessage(BluetoothDevice bluetoothDevice, Uri[] uriArr, String str, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
            MapClientService service = getService();
            if (service == null) {
                return false;
            }
            Log.d(MapClientService.TAG, "Checking Permission of sendMessage");
            this.mService.enforceCallingOrSelfPermission("android.permission.SEND_SMS", "Need SEND_SMS permission");
            return service.sendMessage(bluetoothDevice, uriArr, str, pendingIntent, pendingIntent2);
        }

        public boolean getUnreadMessages(BluetoothDevice bluetoothDevice) {
            MapClientService service = getService();
            if (service == null) {
                return false;
            }
            this.mService.enforceCallingOrSelfPermission("android.permission.READ_SMS", "Need READ_SMS permission");
            return service.getUnreadMessages(bluetoothDevice);
        }
    }

    private class MapBroadcastReceiver extends BroadcastReceiver {
        private MapBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals("android.bluetooth.device.action.ACL_DISCONNECTED") && !action.equals("android.bluetooth.device.action.SDP_RECORD")) {
                return;
            }
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            if (bluetoothDevice != null) {
                MceStateMachine mceStateMachine = (MceStateMachine) MapClientService.this.mMapInstanceMap.get(bluetoothDevice);
                if (mceStateMachine == null) {
                    Log.e(MapClientService.TAG, "No Statemachine found for the device from broadcast");
                    return;
                }
                if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED") && mceStateMachine.getState() == 2) {
                    mceStateMachine.disconnect();
                }
                if (action.equals("android.bluetooth.device.action.SDP_RECORD") && ((ParcelUuid) intent.getParcelableExtra("android.bluetooth.device.extra.UUID")).equals(BluetoothUuid.MAS)) {
                    SdpMasRecord parcelableExtra = intent.getParcelableExtra("android.bluetooth.device.extra.SDP_RECORD");
                    int intExtra = intent.getIntExtra("android.bluetooth.device.extra.SDP_SEARCH_STATUS", -1);
                    if (parcelableExtra == null) {
                        Log.w(MapClientService.TAG, "SDP search ended with no MAS record. Status: " + intExtra);
                        return;
                    }
                    mceStateMachine.obtainMessage(MediaPlayer2.MEDIAPLAYER2_STATE_ERROR, parcelableExtra).sendToTarget();
                    return;
                }
                return;
            }
            Log.e(MapClientService.TAG, "broadcast has NO device param!");
        }
    }
}
