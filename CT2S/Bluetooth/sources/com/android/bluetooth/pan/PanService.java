package com.android.bluetooth.pan;

import android.R;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothPan;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.ServiceManager;
import android.os.UserManager;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.ProfileService;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PanService extends ProfileService {
    private static final String BLUETOOTH_IFACE_ADDR_START = "192.168.44.1";
    private static final int BLUETOOTH_MAX_PAN_CONNECTIONS = 5;
    private static final int BLUETOOTH_PREFIX_LENGTH = 24;
    private static final int CONN_STATE_CONNECTED = 0;
    private static final int CONN_STATE_CONNECTING = 1;
    private static final int CONN_STATE_DISCONNECTED = 2;
    private static final int CONN_STATE_DISCONNECTING = 3;
    private static final boolean DBG = false;
    private static final int MESSAGE_CONNECT = 1;
    private static final int MESSAGE_CONNECT_STATE_CHANGED = 11;
    private static final int MESSAGE_DISCONNECT = 2;
    private static final String TAG = "PanService";
    private ArrayList<String> mBluetoothIfaceAddresses;
    private int mMaxPanDevices;
    private boolean mNativeAvailable;
    private BluetoothTetheringNetworkFactory mNetworkFactory;
    private HashMap<BluetoothDevice, BluetoothPanDevice> mPanDevices;
    private String mPanIfName;
    private boolean mTetherOn = DBG;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    if (!PanService.this.connectPanNative(Utils.getByteAddress(device), 2, 1)) {
                        PanService.this.handlePanDeviceStateChange(device, null, 1, 2, 1);
                        PanService.this.handlePanDeviceStateChange(device, null, 0, 2, 1);
                    }
                    break;
                case 2:
                    BluetoothDevice device2 = (BluetoothDevice) msg.obj;
                    if (!PanService.this.disconnectPanNative(Utils.getByteAddress(device2))) {
                        PanService.this.handlePanDeviceStateChange(device2, PanService.this.mPanIfName, 3, 2, 1);
                        PanService.this.handlePanDeviceStateChange(device2, PanService.this.mPanIfName, 0, 2, 1);
                    }
                    break;
                case 11:
                    ConnectState cs = (ConnectState) msg.obj;
                    PanService.this.handlePanDeviceStateChange(PanService.this.getDevice(cs.addr), PanService.this.mPanIfName, PanService.convertHalState(cs.state), cs.local_role, cs.remote_role);
                    break;
            }
        }
    };

    private static native void classInitNative();

    private native void cleanupNative();

    private native boolean connectPanNative(byte[] bArr, int i, int i2);

    private native boolean disconnectPanNative(byte[] bArr);

    private native boolean enablePanNative(int i);

    private native int getPanLocalRoleNative();

    private native void initializeNative();

    static {
        classInitNative();
    }

    @Override
    protected String getName() {
        return TAG;
    }

    @Override
    public ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothPanBinder(this);
    }

    @Override
    protected boolean start() {
        this.mPanDevices = new HashMap<>();
        this.mBluetoothIfaceAddresses = new ArrayList<>();
        try {
            this.mMaxPanDevices = getResources().getInteger(R.integer.button_pressed_animation_duration);
        } catch (Resources.NotFoundException e) {
            this.mMaxPanDevices = 5;
        }
        initializeNative();
        this.mNativeAvailable = true;
        this.mNetworkFactory = new BluetoothTetheringNetworkFactory(getBaseContext(), getMainLooper(), this);
        return true;
    }

    @Override
    protected boolean stop() {
        this.mHandler.removeCallbacksAndMessages(null);
        return true;
    }

    @Override
    protected boolean cleanup() {
        if (this.mNativeAvailable) {
            cleanupNative();
            this.mNativeAvailable = DBG;
        }
        if (this.mPanDevices != null) {
            List<BluetoothDevice> DevList = getConnectedDevices();
            for (BluetoothDevice dev : DevList) {
                handlePanDeviceStateChange(dev, this.mPanIfName, 0, 2, 1);
            }
            this.mPanDevices.clear();
        }
        if (this.mBluetoothIfaceAddresses != null) {
            this.mBluetoothIfaceAddresses.clear();
        }
        return true;
    }

    private static class BluetoothPanBinder extends IBluetoothPan.Stub implements ProfileService.IProfileServiceBinder {
        private PanService mService;

        public BluetoothPanBinder(PanService svc) {
            this.mService = svc;
        }

        @Override
        public boolean cleanup() {
            this.mService = null;
            return true;
        }

        private PanService getService() {
            if (!Utils.checkCaller()) {
                Log.w(PanService.TAG, "Pan call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        public boolean connect(BluetoothDevice device) {
            PanService service = getService();
            return service == null ? PanService.DBG : service.connect(device);
        }

        public boolean disconnect(BluetoothDevice device) {
            PanService service = getService();
            return service == null ? PanService.DBG : service.disconnect(device);
        }

        public int getConnectionState(BluetoothDevice device) {
            PanService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(device);
        }

        private boolean isPanNapOn() {
            PanService service = getService();
            return service == null ? PanService.DBG : service.isPanNapOn();
        }

        private boolean isPanUOn() {
            PanService service = getService();
            return service.isPanUOn();
        }

        public boolean isTetheringOn() {
            PanService service = getService();
            return service == null ? PanService.DBG : service.isTetheringOn();
        }

        public void setBluetoothTethering(boolean value) {
            PanService service = getService();
            if (service != null) {
                Log.d(PanService.TAG, "setBluetoothTethering: " + value + ", mTetherOn: " + service.mTetherOn);
                service.setBluetoothTethering(value);
            }
        }

        public List<BluetoothDevice> getConnectedDevices() {
            PanService service = getService();
            return service == null ? new ArrayList(0) : service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
            PanService service = getService();
            return service == null ? new ArrayList(0) : service.getDevicesMatchingConnectionStates(states);
        }
    }

    boolean connect(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (getConnectionState(device) != 0) {
            Log.e(TAG, "Pan Device not disconnected: " + device);
            return DBG;
        }
        Message msg = this.mHandler.obtainMessage(1, device);
        this.mHandler.sendMessage(msg);
        return true;
    }

    boolean disconnect(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Message msg = this.mHandler.obtainMessage(2, device);
        this.mHandler.sendMessage(msg);
        return true;
    }

    int getConnectionState(BluetoothDevice device) {
        BluetoothPanDevice panDevice = this.mPanDevices.get(device);
        if (panDevice == null) {
            return 0;
        }
        return panDevice.mState;
    }

    boolean isPanNapOn() {
        if ((getPanLocalRoleNative() & 1) != 0) {
            return true;
        }
        return DBG;
    }

    boolean isPanUOn() {
        if ((getPanLocalRoleNative() & 2) != 0) {
            return true;
        }
        return DBG;
    }

    boolean isTetheringOn() {
        return this.mTetherOn;
    }

    void setBluetoothTethering(boolean value) {
        ConnectivityManager.enforceTetherChangePermission(getBaseContext());
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH_ADMIN permission");
        UserManager um = (UserManager) getSystemService("user");
        if (um.hasUserRestriction("no_config_tethering")) {
            throw new SecurityException("DISALLOW_CONFIG_TETHERING is enabled for this user.");
        }
        if (this.mTetherOn != value) {
            this.mTetherOn = value;
            List<BluetoothDevice> DevList = getConnectedDevices();
            for (BluetoothDevice dev : DevList) {
                disconnect(dev);
            }
        }
    }

    List<BluetoothDevice> getConnectedDevices() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        List<BluetoothDevice> devices = getDevicesMatchingConnectionStates(new int[]{2});
        return devices;
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        List<BluetoothDevice> panDevices = new ArrayList<>();
        for (BluetoothDevice device : this.mPanDevices.keySet()) {
            int panDeviceState = getConnectionState(device);
            int len$ = states.length;
            int i$ = 0;
            while (true) {
                if (i$ < len$) {
                    int state = states[i$];
                    if (state != panDeviceState) {
                        i$++;
                    } else {
                        panDevices.add(device);
                        break;
                    }
                }
            }
        }
        return panDevices;
    }

    protected static class ConnectState {
        byte[] addr;
        int error;
        int local_role;
        int remote_role;
        int state;

        public ConnectState(byte[] address, int state, int error, int local_role, int remote_role) {
            this.addr = address;
            this.state = state;
            this.error = error;
            this.local_role = local_role;
            this.remote_role = remote_role;
        }
    }

    private void onConnectStateChanged(byte[] address, int state, int error, int local_role, int remote_role) {
        Message msg = this.mHandler.obtainMessage(11);
        msg.obj = new ConnectState(address, state, error, local_role, remote_role);
        this.mHandler.sendMessage(msg);
    }

    private void onControlStateChanged(int local_role, int state, int error, String ifname) {
        if (error == 0) {
            this.mPanIfName = ifname;
        }
    }

    private static int convertHalState(int halState) {
        switch (halState) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                Log.e(TAG, "bad pan connection state: " + halState);
                break;
        }
        return 0;
    }

    void handlePanDeviceStateChange(BluetoothDevice device, String iface, int state, int local_role, int remote_role) {
        int prevState;
        String ifaceAddr = null;
        BluetoothPanDevice panDevice = this.mPanDevices.get(device);
        if (panDevice == null) {
            prevState = 0;
        } else {
            prevState = panDevice.mState;
            ifaceAddr = panDevice.mIfaceAddr;
        }
        if (prevState == 0 && state == 3) {
            Log.d(TAG, "Ignoring state change from " + prevState + " to " + state);
            return;
        }
        Log.d(TAG, "handlePanDeviceStateChange preState: " + prevState + " state: " + state);
        if (prevState != state) {
            if (remote_role == 2) {
                if (state == 2) {
                    if (!this.mTetherOn || local_role == 2) {
                        Log.d(TAG, "handlePanDeviceStateChange BT tethering is off/Local role is PANU drop the connection");
                        disconnectPanNative(Utils.getByteAddress(device));
                        return;
                    } else {
                        Log.d(TAG, "handlePanDeviceStateChange LOCAL_NAP_ROLE:REMOTE_PANU_ROLE");
                        ifaceAddr = enableTethering(iface);
                        if (ifaceAddr == null) {
                            Log.e(TAG, "Error seting up tether interface");
                        }
                    }
                } else if (state == 0 && ifaceAddr != null) {
                    this.mBluetoothIfaceAddresses.remove(ifaceAddr);
                    ifaceAddr = null;
                }
            } else if (this.mNetworkFactory != null) {
                Log.d(TAG, "handlePanDeviceStateChange LOCAL_PANU_ROLE:REMOTE_NAP_ROLE state = " + state + ", prevState = " + prevState);
                if (state == 2) {
                    this.mNetworkFactory.startReverseTether(iface);
                } else if (state == 0 && (prevState == 2 || prevState == 3)) {
                    this.mNetworkFactory.stopReverseTether();
                }
            }
            if (panDevice == null) {
                this.mPanDevices.put(device, new BluetoothPanDevice(state, ifaceAddr, iface, local_role));
            } else {
                panDevice.mState = state;
                panDevice.mIfaceAddr = ifaceAddr;
                panDevice.mLocalRole = local_role;
                panDevice.mIface = iface;
            }
            Log.d(TAG, "Pan Device state : device: " + device + " State:" + prevState + "->" + state);
            notifyProfileConnectionStateChanged(device, 5, state, prevState);
            Intent intent = new Intent("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
            intent.putExtra("android.bluetooth.device.extra.DEVICE", device);
            intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", prevState);
            intent.putExtra("android.bluetooth.profile.extra.STATE", state);
            intent.putExtra("android.bluetooth.pan.extra.LOCAL_ROLE", local_role);
            sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
        }
    }

    private String enableTethering(String iface) {
        String address;
        InetAddress addr;
        IBinder b = ServiceManager.getService("network_management");
        INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        cm.getTetherableBluetoothRegexs();
        String[] strArr = new String[0];
        try {
            String[] currentIfaces = service.listInterfaces();
            boolean found = DBG;
            int len$ = currentIfaces.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                String currIface = currentIfaces[i$];
                if (!currIface.equals(iface)) {
                    i$++;
                } else {
                    found = true;
                    break;
                }
            }
            if (!found || (address = createNewTetheringAddressLocked()) == null) {
                return null;
            }
            try {
                InterfaceConfiguration ifcg = service.getInterfaceConfig(iface);
                if (ifcg != null) {
                    LinkAddress linkAddr = ifcg.getLinkAddress();
                    if (linkAddr == null || (addr = linkAddr.getAddress()) == null || addr.equals(NetworkUtils.numericToInetAddress("0.0.0.0")) || addr.equals(NetworkUtils.numericToInetAddress("::0"))) {
                        addr = NetworkUtils.numericToInetAddress(address);
                    }
                    ifcg.setInterfaceUp();
                    ifcg.setLinkAddress(new LinkAddress(addr, 24));
                    ifcg.clearFlag("running");
                    service.setInterfaceConfig(iface, ifcg);
                    if (cm.tether(iface) != 0) {
                        Log.e(TAG, "Error tethering " + iface);
                        return address;
                    }
                    return address;
                }
                return address;
            } catch (Exception e) {
                Log.e(TAG, "Error configuring interface " + iface + ", :" + e);
                return null;
            }
        } catch (Exception e2) {
            Log.e(TAG, "Error listing Interfaces :" + e2);
            return null;
        }
    }

    private String createNewTetheringAddressLocked() {
        if (getConnectedPanDevices().size() == this.mMaxPanDevices) {
            return null;
        }
        String address = BLUETOOTH_IFACE_ADDR_START;
        while (this.mBluetoothIfaceAddresses.contains(address)) {
            String[] addr = address.split("\\.");
            Integer newIp = Integer.valueOf(Integer.parseInt(addr[2]) + 1);
            address = address.replace(addr[2], newIp.toString());
        }
        this.mBluetoothIfaceAddresses.add(address);
        return address;
    }

    private List<BluetoothDevice> getConnectedPanDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        for (BluetoothDevice device : this.mPanDevices.keySet()) {
            if (getPanDeviceConnectionState(device) == 2) {
                devices.add(device);
            }
        }
        return devices;
    }

    private int getPanDeviceConnectionState(BluetoothDevice device) {
        BluetoothPanDevice panDevice = this.mPanDevices.get(device);
        if (panDevice == null) {
            return 0;
        }
        return panDevice.mState;
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        println(sb, "mMaxPanDevices: " + this.mMaxPanDevices);
        println(sb, "mPanIfName: " + this.mPanIfName);
        println(sb, "mTetherOn: " + this.mTetherOn);
        println(sb, "mPanDevices:");
        for (BluetoothDevice device : this.mPanDevices.keySet()) {
            println(sb, "  " + device + " : " + this.mPanDevices.get(device));
        }
        println(sb, "mBluetoothIfaceAddresses:");
        for (String address : this.mBluetoothIfaceAddresses) {
            println(sb, "  " + address);
        }
    }

    private class BluetoothPanDevice {
        private String mIface;
        private String mIfaceAddr;
        private int mLocalRole;
        private int mState;

        BluetoothPanDevice(int state, String ifaceAddr, String iface, int localRole) {
            this.mState = state;
            this.mIfaceAddr = ifaceAddr;
            this.mIface = iface;
            this.mLocalRole = localRole;
        }
    }
}
