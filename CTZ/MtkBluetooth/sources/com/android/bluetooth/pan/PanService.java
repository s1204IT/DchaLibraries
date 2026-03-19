package com.android.bluetooth.pan;

import android.R;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothPan;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.os.Binder;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.ServiceManager;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
    private static PanService sPanService;
    private ArrayList<String> mBluetoothIfaceAddresses;
    private int mMaxPanDevices;
    private String mNapIfaceAddr;
    private boolean mNativeAvailable;
    private BluetoothTetheringNetworkFactory mNetworkFactory;
    private HashMap<BluetoothDevice, BluetoothPanDevice> mPanDevices;
    private String mPanIfName;
    private boolean mTetherOn = false;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 11) {
                switch (i) {
                    case 1:
                        BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                        if (!PanService.this.connectPanNative(Utils.getByteAddress(bluetoothDevice), 2, 1)) {
                            PanService.this.handlePanDeviceStateChange(bluetoothDevice, null, 1, 2, 1);
                            PanService.this.handlePanDeviceStateChange(bluetoothDevice, null, 0, 2, 1);
                        }
                        break;
                    case 2:
                        BluetoothDevice bluetoothDevice2 = (BluetoothDevice) message.obj;
                        if (!PanService.this.disconnectPanNative(Utils.getByteAddress(bluetoothDevice2))) {
                            PanService.this.handlePanDeviceStateChange(bluetoothDevice2, PanService.this.mPanIfName, 3, 2, 1);
                            PanService.this.handlePanDeviceStateChange(bluetoothDevice2, PanService.this.mPanIfName, 0, 2, 1);
                        }
                        break;
                }
            }
            ConnectState connectState = (ConnectState) message.obj;
            PanService.this.handlePanDeviceStateChange(PanService.this.getDevice(connectState.addr), PanService.this.mPanIfName, PanService.convertHalState(connectState.state), connectState.local_role, connectState.remote_role);
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
    public ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothPanBinder(this);
    }

    public static synchronized PanService getPanService() {
        if (sPanService == null) {
            Log.w(TAG, "getPanService(): service is null");
            return null;
        }
        if (!sPanService.isAvailable()) {
            Log.w(TAG, "getPanService(): service is not available ");
            return null;
        }
        return sPanService;
    }

    private static synchronized void setPanService(PanService panService) {
        sPanService = panService;
    }

    @Override
    protected boolean start() {
        this.mPanDevices = new HashMap<>();
        this.mBluetoothIfaceAddresses = new ArrayList<>();
        try {
            this.mMaxPanDevices = getResources().getInteger(R.integer.config_defaultNightDisplayCustomEndTime);
        } catch (Resources.NotFoundException e) {
            this.mMaxPanDevices = 5;
        }
        initializeNative();
        this.mNativeAvailable = true;
        this.mNetworkFactory = new BluetoothTetheringNetworkFactory(getBaseContext(), getMainLooper(), this);
        setPanService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        this.mHandler.removeCallbacksAndMessages(null);
        return true;
    }

    @Override
    protected void cleanup() {
        setPanService(null);
        if (this.mNativeAvailable) {
            cleanupNative();
            this.mNativeAvailable = false;
        }
        if (this.mPanDevices != null) {
            for (BluetoothDevice bluetoothDevice : getDevicesMatchingConnectionStates(new int[]{1, 2, 3})) {
                BluetoothPanDevice bluetoothPanDevice = this.mPanDevices.get(bluetoothDevice);
                Log.d(TAG, "panDevice: " + bluetoothPanDevice + " device address: " + bluetoothDevice);
                if (bluetoothPanDevice != null) {
                    handlePanDeviceStateChange(bluetoothDevice, this.mPanIfName, 0, bluetoothPanDevice.mLocalRole, bluetoothPanDevice.mRemoteRole);
                }
            }
            this.mPanDevices.clear();
        }
    }

    private static class BluetoothPanBinder extends IBluetoothPan.Stub implements ProfileService.IProfileServiceBinder {
        private PanService mService;

        BluetoothPanBinder(PanService panService) {
            this.mService = panService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
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

        public boolean connect(BluetoothDevice bluetoothDevice) {
            PanService service = getService();
            if (service == null) {
                return false;
            }
            return service.connect(bluetoothDevice);
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            PanService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(bluetoothDevice);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            PanService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        private boolean isPanNapOn() {
            PanService service = getService();
            if (service == null) {
                return false;
            }
            return service.isPanNapOn();
        }

        private boolean isPanUOn() {
            PanService service = getService();
            if (service == null) {
                return false;
            }
            return service.isPanUOn();
        }

        public boolean isTetheringOn() {
            PanService service = getService();
            if (service == null) {
                return false;
            }
            return service.isTetheringOn();
        }

        public void setBluetoothTethering(boolean z) {
            PanService service = getService();
            if (service != null) {
                Log.d(PanService.TAG, "setBluetoothTethering: " + z + ", mTetherOn: " + service.mTetherOn);
                service.setBluetoothTethering(z);
            }
        }

        public List<BluetoothDevice> getConnectedDevices() {
            PanService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            PanService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (getConnectionState(bluetoothDevice) != 0) {
            Log.e(TAG, "Pan Device not disconnected: " + bluetoothDevice);
            return false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, bluetoothDevice));
        return true;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, bluetoothDevice));
        return true;
    }

    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        BluetoothPanDevice bluetoothPanDevice = this.mPanDevices.get(bluetoothDevice);
        if (bluetoothPanDevice == null) {
            return 0;
        }
        return bluetoothPanDevice.mState;
    }

    boolean isPanNapOn() {
        return (getPanLocalRoleNative() & 1) != 0;
    }

    boolean isPanUOn() {
        return (getPanLocalRoleNative() & 2) != 0;
    }

    public boolean isTetheringOn() {
        return this.mTetherOn;
    }

    void setBluetoothTethering(boolean z) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Context baseContext = getBaseContext();
        String opPackageName = baseContext.getOpPackageName();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            ConnectivityManager.enforceTetherChangePermission(baseContext, opPackageName);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            if (((UserManager) getSystemService("user")).hasUserRestriction("no_config_tethering") && z) {
                throw new SecurityException("DISALLOW_CONFIG_TETHERING is enabled for this user.");
            }
            if (this.mTetherOn != z) {
                this.mTetherOn = z;
                Iterator<BluetoothDevice> it = getConnectedDevices().iterator();
                while (it.hasNext()) {
                    disconnect(it.next());
                }
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        if (bluetoothDevice == null) {
            throw new IllegalArgumentException("Null device");
        }
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothPanPriorityKey(bluetoothDevice.getAddress()), i);
        return true;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            throw new IllegalArgumentException("Null device");
        }
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothPanPriorityKey(bluetoothDevice.getAddress()), -1);
    }

    public List<BluetoothDevice> getConnectedDevices() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return getDevicesMatchingConnectionStates(new int[]{2});
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        ArrayList arrayList = new ArrayList();
        for (BluetoothDevice bluetoothDevice : this.mPanDevices.keySet()) {
            int connectionState = getConnectionState(bluetoothDevice);
            int length = iArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                if (iArr[i] != connectionState) {
                    i++;
                } else {
                    arrayList.add(bluetoothDevice);
                    break;
                }
            }
        }
        return arrayList;
    }

    protected static class ConnectState {
        public byte[] addr;
        public int error;
        public int local_role;
        public int remote_role;
        public int state;

        public ConnectState(byte[] bArr, int i, int i2, int i3, int i4) {
            this.addr = bArr;
            this.state = i;
            this.error = i2;
            this.local_role = i3;
            this.remote_role = i4;
        }
    }

    private void onConnectStateChanged(byte[] bArr, int i, int i2, int i3, int i4) {
        Message messageObtainMessage = this.mHandler.obtainMessage(11);
        messageObtainMessage.obj = new ConnectState(bArr, i, i2, i3, i4);
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void onControlStateChanged(int i, int i2, int i3, String str) {
        if (i3 == 0) {
            this.mPanIfName = str;
        }
    }

    private static int convertHalState(int i) {
        switch (i) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                Log.e(TAG, "bad pan connection state: " + i);
                break;
        }
        return 0;
    }

    void handlePanDeviceStateChange(BluetoothDevice bluetoothDevice, String str, int i, int i2, int i3) {
        int i4;
        BluetoothPanDevice bluetoothPanDevice = this.mPanDevices.get(bluetoothDevice);
        if (bluetoothPanDevice == null) {
            Log.i(TAG, "state " + i + " Num of connected pan devices: " + this.mPanDevices.size());
            i4 = 0;
            this.mPanDevices.put(bluetoothDevice, new BluetoothPanDevice(i, str, i2, i3));
        } else {
            int i5 = bluetoothPanDevice.mState;
            bluetoothPanDevice.mState = i;
            bluetoothPanDevice.mLocalRole = i2;
            bluetoothPanDevice.mRemoteRole = i3;
            bluetoothPanDevice.mIface = str;
            i4 = i5;
        }
        if (i4 == 0 && i == 3) {
            Log.d(TAG, "Ignoring state change from " + i4 + " to " + i);
            this.mPanDevices.remove(bluetoothDevice);
            return;
        }
        Log.d(TAG, "handlePanDeviceStateChange preState: " + i4 + " state: " + i);
        if (i4 == i) {
            return;
        }
        if (i3 == 2) {
            if (i == 2) {
                if (!this.mTetherOn || i2 == 2) {
                    Log.d(TAG, "handlePanDeviceStateChange BT tethering is off/Local role is PANU drop the connection");
                    this.mPanDevices.remove(bluetoothDevice);
                    disconnectPanNative(Utils.getByteAddress(bluetoothDevice));
                    return;
                }
                Log.d(TAG, "handlePanDeviceStateChange LOCAL_NAP_ROLE:REMOTE_PANU_ROLE");
                if (this.mNapIfaceAddr == null) {
                    this.mNapIfaceAddr = startTethering(str);
                    if (this.mNapIfaceAddr == null) {
                        Log.e(TAG, "Error seting up tether interface");
                        this.mPanDevices.remove(bluetoothDevice);
                        disconnectPanNative(Utils.getByteAddress(bluetoothDevice));
                        return;
                    }
                }
            } else if (i == 0) {
                this.mPanDevices.remove(bluetoothDevice);
                Log.i(TAG, "remote(PANU) is disconnected, Remaining connected PANU devices: " + this.mPanDevices.size());
                if (this.mNapIfaceAddr != null && this.mPanDevices.size() == 0) {
                    stopTethering(str);
                    this.mNapIfaceAddr = null;
                }
            }
        } else if (this.mNetworkFactory != null) {
            Log.d(TAG, "handlePanDeviceStateChange LOCAL_PANU_ROLE:REMOTE_NAP_ROLE state = " + i + ", prevState = " + i4);
            if (i == 2) {
                this.mNetworkFactory.startReverseTether(str);
            } else if (i == 0) {
                this.mNetworkFactory.stopReverseTether();
                this.mPanDevices.remove(bluetoothDevice);
            }
        }
        if (i == 2) {
            MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.PAN);
        }
        Log.d(TAG, "Pan Device state : device: " + bluetoothDevice + " State:" + i4 + "->" + i);
        Intent intent = new Intent("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i4);
        intent.putExtra("android.bluetooth.profile.extra.STATE", i);
        intent.putExtra("android.bluetooth.pan.extra.LOCAL_ROLE", i2);
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private String startTethering(String str) {
        return configureBtIface(true, str);
    }

    private String stopTethering(String str) {
        return configureBtIface(false, str);
    }

    private String configureBtIface(boolean z, String str) {
        String str2;
        InetAddress address;
        Log.i(TAG, "configureBtIface: " + str + " enable: " + z);
        INetworkManagementService iNetworkManagementServiceAsInterface = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService("connectivity");
        connectivityManager.getTetherableBluetoothRegexs();
        boolean z2 = false;
        String[] strArr = new String[0];
        try {
            String[] strArrListInterfaces = iNetworkManagementServiceAsInterface.listInterfaces();
            int length = strArrListInterfaces.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                if (!strArrListInterfaces[i].equals(str)) {
                    i++;
                } else {
                    z2 = true;
                    break;
                }
            }
            if (!z2) {
                return null;
            }
            try {
                InterfaceConfiguration interfaceConfig = iNetworkManagementServiceAsInterface.getInterfaceConfig(str);
                if (interfaceConfig == null) {
                    return null;
                }
                LinkAddress linkAddress = interfaceConfig.getLinkAddress();
                if (linkAddress == null || (address = linkAddress.getAddress()) == null || address.equals(NetworkUtils.numericToInetAddress("0.0.0.0")) || address.equals(NetworkUtils.numericToInetAddress("::0"))) {
                    InetAddress inetAddressNumericToInetAddress = NetworkUtils.numericToInetAddress(BLUETOOTH_IFACE_ADDR_START);
                    str2 = BLUETOOTH_IFACE_ADDR_START;
                    address = inetAddressNumericToInetAddress;
                } else {
                    str2 = null;
                }
                interfaceConfig.setLinkAddress(new LinkAddress(address, 24));
                if (z) {
                    interfaceConfig.setInterfaceUp();
                } else {
                    interfaceConfig.setInterfaceDown();
                }
                interfaceConfig.clearFlag("running");
                iNetworkManagementServiceAsInterface.setInterfaceConfig(str, interfaceConfig);
                if (z) {
                    int iTether = connectivityManager.tether(str);
                    if (iTether != 0) {
                        Log.e(TAG, "Error tethering " + str + " tetherStatus: " + iTether);
                        return null;
                    }
                } else {
                    Log.i(TAG, "Untethered: " + str + " untetherStatus: " + connectivityManager.untether(str));
                }
                return str2;
            } catch (Exception e) {
                Log.e(TAG, "Error configuring interface " + str + ", :" + e);
                return null;
            }
        } catch (Exception e2) {
            Log.e(TAG, "Error listing Interfaces :" + e2);
            return null;
        }
    }

    private List<BluetoothDevice> getConnectedPanDevices() {
        ArrayList arrayList = new ArrayList();
        for (BluetoothDevice bluetoothDevice : this.mPanDevices.keySet()) {
            if (getPanDeviceConnectionState(bluetoothDevice) == 2) {
                arrayList.add(bluetoothDevice);
            }
        }
        return arrayList;
    }

    private int getPanDeviceConnectionState(BluetoothDevice bluetoothDevice) {
        BluetoothPanDevice bluetoothPanDevice = this.mPanDevices.get(bluetoothDevice);
        if (bluetoothPanDevice == null) {
            return 0;
        }
        return bluetoothPanDevice.mState;
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        println(sb, "mMaxPanDevices: " + this.mMaxPanDevices);
        println(sb, "mPanIfName: " + this.mPanIfName);
        println(sb, "mTetherOn: " + this.mTetherOn);
        println(sb, "mPanDevices:");
        for (BluetoothDevice bluetoothDevice : this.mPanDevices.keySet()) {
            println(sb, "  " + bluetoothDevice + " : " + this.mPanDevices.get(bluetoothDevice));
        }
    }

    private class BluetoothPanDevice {
        private String mIface;
        private int mLocalRole;
        private int mRemoteRole;
        private int mState;

        BluetoothPanDevice(int i, String str, int i2, int i3) {
            this.mState = i;
            this.mIface = str;
            this.mLocalRole = i2;
            this.mRemoteRole = i3;
        }
    }
}
