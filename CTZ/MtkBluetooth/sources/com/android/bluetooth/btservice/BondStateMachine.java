package com.android.bluetooth.btservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.OobData;
import android.content.Intent;
import android.os.Message;
import android.os.UserHandle;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.a2dpsink.A2dpSinkService;
import com.android.bluetooth.btservice.RemoteDevices;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.hfpclient.HeadsetClientService;
import com.android.bluetooth.hid.HidHostService;
import com.android.bluetooth.pbapclient.PbapClientService;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.vcard.VCardConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

final class BondStateMachine extends StateMachine {
    static final int BONDING_STATE_CHANGE = 4;
    static final int BOND_STATE_BONDED = 2;
    static final int BOND_STATE_BONDING = 1;
    static final int BOND_STATE_NONE = 0;
    static final int CANCEL_BOND = 2;
    static final int CREATE_BOND = 1;
    private static final boolean DBG = false;
    public static final String OOBDATA = "oobdata";
    static final int PIN_REQUEST = 6;
    static final int REMOVE_BOND = 3;
    static final int SSP_REQUEST = 5;
    private static final String TAG = "BluetoothBondStateMachine";
    static final int UUID_UPDATE = 10;
    private BluetoothAdapter mAdapter;
    private AdapterProperties mAdapterProperties;
    private AdapterService mAdapterService;
    private final ArrayList<BluetoothDevice> mDevices;

    @VisibleForTesting
    Set<BluetoothDevice> mPendingBondedDevices;
    private PendingCommandState mPendingCommandState;
    private RemoteDevices mRemoteDevices;
    private StableState mStableState;
    private boolean mStateMachineInitiated;

    private BondStateMachine(AdapterService adapterService, AdapterProperties adapterProperties, RemoteDevices remoteDevices) {
        super("BondStateMachine:");
        this.mPendingCommandState = new PendingCommandState();
        this.mStableState = new StableState();
        this.mStateMachineInitiated = false;
        this.mDevices = new ArrayList<>();
        this.mPendingBondedDevices = new HashSet();
        addState(this.mStableState);
        addState(this.mPendingCommandState);
        this.mRemoteDevices = remoteDevices;
        this.mAdapterService = adapterService;
        this.mAdapterProperties = adapterProperties;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        setInitialState(this.mStableState);
    }

    public static BondStateMachine make(AdapterService adapterService, AdapterProperties adapterProperties, RemoteDevices remoteDevices) {
        Log.d(TAG, "make");
        BondStateMachine bondStateMachine = new BondStateMachine(adapterService, adapterProperties, remoteDevices);
        bondStateMachine.start();
        return bondStateMachine;
    }

    public void doQuit() {
        if (this.mStateMachineInitiated) {
            quitNow();
        } else {
            quit();
        }
    }

    private void cleanup() {
        this.mAdapterService = null;
        this.mRemoteDevices = null;
        this.mAdapterProperties = null;
    }

    protected void onQuitting() {
        cleanup();
    }

    private class StableState extends State {
        private StableState() {
        }

        public void enter() {
            BondStateMachine.this.infoLog("StableState(): Entering Off State");
            BondStateMachine.this.mStateMachineInitiated = true;
        }

        public boolean processMessage(Message message) {
            BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
            int i = message.what;
            if (i == 1) {
                OobData oobData = null;
                if (message.getData() != null) {
                    oobData = (OobData) message.getData().getParcelable(BondStateMachine.OOBDATA);
                }
                BondStateMachine.this.createBond(bluetoothDevice, message.arg1, oobData, true);
            } else if (i != 10) {
                switch (i) {
                    case 3:
                        BondStateMachine.this.removeBond(bluetoothDevice, true);
                        break;
                    case 4:
                        int i2 = message.arg1;
                        if (i2 == 11) {
                            if (!BondStateMachine.this.mDevices.contains(bluetoothDevice)) {
                                BondStateMachine.this.mDevices.add(bluetoothDevice);
                            }
                            BondStateMachine.this.sendIntent(bluetoothDevice, i2, 0);
                            BondStateMachine.this.transitionTo(BondStateMachine.this.mPendingCommandState);
                        } else if (i2 == 10) {
                            BondStateMachine.this.sendIntent(bluetoothDevice, i2, 0);
                        } else {
                            Log.e(BondStateMachine.TAG, "In stable state, received invalid newState: " + BondStateMachine.this.state2str(i2));
                        }
                        break;
                    default:
                        Log.e(BondStateMachine.TAG, "Received unhandled state: " + message.what);
                        return false;
                }
            } else if (BondStateMachine.this.mPendingBondedDevices.contains(bluetoothDevice)) {
                BondStateMachine.this.sendIntent(bluetoothDevice, 12, 0);
            }
            return true;
        }
    }

    private class PendingCommandState extends State {
        private PendingCommandState() {
        }

        public void enter() {
            BondStateMachine.this.infoLog("Entering PendingCommandState State");
        }

        public boolean processMessage(Message message) {
            boolean zCreateBond;
            BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
            RemoteDevices.DeviceProperties deviceProperties = BondStateMachine.this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
            if (BondStateMachine.this.mDevices.contains(bluetoothDevice) && message.what != 2 && message.what != 4 && message.what != 5 && message.what != 6) {
                BondStateMachine.this.deferMessage(message);
                return true;
            }
            switch (message.what) {
                case 1:
                    OobData oobData = null;
                    if (message.getData() != null) {
                        oobData = (OobData) message.getData().getParcelable(BondStateMachine.OOBDATA);
                    }
                    zCreateBond = BondStateMachine.this.createBond(bluetoothDevice, message.arg1, oobData, false);
                    if (zCreateBond) {
                        BondStateMachine.this.mDevices.add(bluetoothDevice);
                    }
                    return true;
                case 2:
                    zCreateBond = BondStateMachine.this.cancelBond(bluetoothDevice);
                    if (zCreateBond) {
                    }
                    return true;
                case 3:
                    zCreateBond = BondStateMachine.this.removeBond(bluetoothDevice, false);
                    if (zCreateBond) {
                    }
                    return true;
                case 4:
                    int i = message.arg1;
                    BondStateMachine.this.sendIntent(bluetoothDevice, i, BondStateMachine.this.getUnbondReasonFromHALCode(message.arg2));
                    if (i != 11) {
                        if (i == 10 && !BondStateMachine.this.mDevices.contains(bluetoothDevice) && BondStateMachine.this.mDevices.size() != 0) {
                            BondStateMachine.this.infoLog("not transitioning to stable state");
                        } else {
                            boolean z = !BondStateMachine.this.mDevices.remove(bluetoothDevice);
                            if (BondStateMachine.this.mDevices.isEmpty()) {
                                BondStateMachine.this.transitionTo(BondStateMachine.this.mStableState);
                                z = false;
                            }
                            if (i == 10) {
                                BondStateMachine.this.mAdapterService.setPhonebookAccessPermission(bluetoothDevice, 0);
                                BondStateMachine.this.mAdapterService.setMessageAccessPermission(bluetoothDevice, 0);
                                BondStateMachine.this.mAdapterService.setSimAccessPermission(bluetoothDevice, 0);
                                BondStateMachine.this.clearProfilePriority(bluetoothDevice);
                            }
                            zCreateBond = z;
                        }
                    } else {
                        zCreateBond = !BondStateMachine.this.mDevices.contains(bluetoothDevice);
                    }
                    if (zCreateBond) {
                    }
                    return true;
                case 5:
                    BondStateMachine.this.sendDisplayPinIntent(deviceProperties.getAddress(), message.arg1, message.arg2);
                    if (zCreateBond) {
                    }
                    return true;
                case 6:
                    int deviceClass = bluetoothDevice.getBluetoothClass().getDeviceClass();
                    if (deviceClass == 1344 || deviceClass == 1472) {
                        BondStateMachine.this.sendDisplayPinIntent(deviceProperties.getAddress(), 100000 + ((int) Math.floor(Math.random() * 899999.0d)), 5);
                    } else if (message.arg2 == 1) {
                        BondStateMachine.this.sendDisplayPinIntent(deviceProperties.getAddress(), 0, 7);
                    } else {
                        BondStateMachine.this.sendDisplayPinIntent(deviceProperties.getAddress(), 0, 0);
                    }
                    if (zCreateBond) {
                    }
                    return true;
                default:
                    Log.e(BondStateMachine.TAG, "Received unhandled event:" + message.what);
                    return false;
            }
        }
    }

    private boolean cancelBond(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice.getBondState() == 11) {
            if (!this.mAdapterService.cancelBondNative(Utils.getBytesFromAddress(bluetoothDevice.getAddress()))) {
                Log.e(TAG, "Unexpected error while cancelling bond:");
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean removeBond(BluetoothDevice bluetoothDevice, boolean z) {
        if (bluetoothDevice.getBondState() == 12) {
            if (!this.mAdapterService.removeBondNative(Utils.getBytesFromAddress(bluetoothDevice.getAddress()))) {
                Log.e(TAG, "Unexpected error while removing bond:");
                return false;
            }
            if (z) {
                transitionTo(this.mPendingCommandState);
                return true;
            }
            return true;
        }
        return false;
    }

    private boolean createBond(BluetoothDevice bluetoothDevice, int i, OobData oobData, boolean z) {
        boolean zCreateBondNative;
        if (bluetoothDevice.getBondState() != 10) {
            return false;
        }
        infoLog("Bond address is:" + bluetoothDevice);
        byte[] bytesFromAddress = Utils.getBytesFromAddress(bluetoothDevice.getAddress());
        if (oobData != null) {
            zCreateBondNative = this.mAdapterService.createBondOutOfBandNative(bytesFromAddress, i, oobData);
        } else {
            zCreateBondNative = this.mAdapterService.createBondNative(bytesFromAddress, i);
        }
        if (!zCreateBondNative) {
            sendIntent(bluetoothDevice, 10, 9);
            return false;
        }
        if (z) {
            transitionTo(this.mPendingCommandState);
            return true;
        }
        return true;
    }

    private void sendDisplayPinIntent(byte[] bArr, int i, int i2) {
        Intent intent = new Intent("android.bluetooth.device.action.PAIRING_REQUEST");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mRemoteDevices.getDevice(bArr));
        if (i != 0) {
            intent.putExtra("android.bluetooth.device.extra.PAIRING_KEY", i);
        }
        intent.putExtra("android.bluetooth.device.extra.PAIRING_VARIANT", i2);
        intent.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
        intent.addFlags(16777216);
        AdapterService adapterService = this.mAdapterService;
        AdapterService adapterService2 = this.mAdapterService;
        adapterService.sendOrderedBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
    }

    @VisibleForTesting
    void sendIntent(BluetoothDevice bluetoothDevice, int i, int i2) {
        int bondState;
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (i != 10 && i != 11 && i != 12) {
            infoLog("Invalid bond state " + i);
            return;
        }
        if (deviceProperties != null) {
            bondState = deviceProperties.getBondState();
        } else {
            bondState = 10;
        }
        if (this.mPendingBondedDevices.contains(bluetoothDevice)) {
            this.mPendingBondedDevices.remove(bluetoothDevice);
            if (bondState == 12) {
                if (i == 11) {
                    this.mAdapterProperties.onBondStateChanged(bluetoothDevice, i);
                }
                bondState = 11;
            } else {
                throw new IllegalArgumentException("Invalid old state " + bondState);
            }
        }
        if (bondState == i) {
            return;
        }
        this.mAdapterProperties.onBondStateChanged(bluetoothDevice, i);
        if (deviceProperties != null && ((deviceProperties.getDeviceType() == 1 || deviceProperties.getDeviceType() == 3) && i == 12 && deviceProperties.getUuids() == null)) {
            infoLog(bluetoothDevice + " is bonded, wait for SDP complete to broadcast bonded intent");
            if (!this.mPendingBondedDevices.contains(bluetoothDevice)) {
                this.mPendingBondedDevices.add(bluetoothDevice);
            }
            if (bondState != 10) {
                return;
            } else {
                i = 11;
            }
        }
        Intent intent = new Intent("android.bluetooth.device.action.BOND_STATE_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.device.extra.BOND_STATE", i);
        intent.putExtra("android.bluetooth.device.extra.PREVIOUS_BOND_STATE", bondState);
        if (i == 10) {
            intent.putExtra("android.bluetooth.device.extra.REASON", i2);
        }
        this.mAdapterService.sendBroadcastAsUser(intent, UserHandle.ALL, ProfileService.BLUETOOTH_PERM);
        infoLog("Bond State Change Intent:" + bluetoothDevice + " " + state2str(bondState) + " => " + state2str(i));
    }

    void bondStateChangeCallback(int i, byte[] bArr, int i2) {
        BluetoothDevice device = this.mRemoteDevices.getDevice(bArr);
        if (device == null) {
            infoLog("No record of the device:" + device);
            device = this.mAdapter.getRemoteDevice(Utils.getAddressStringFromByte(bArr));
        }
        infoLog("bondStateChangeCallback: Status: " + i + " Address: " + device + " newState: " + i2);
        Message messageObtainMessage = obtainMessage(4);
        messageObtainMessage.obj = device;
        if (i2 == 2) {
            messageObtainMessage.arg1 = 12;
        } else if (i2 == 1) {
            messageObtainMessage.arg1 = 11;
        } else {
            messageObtainMessage.arg1 = 10;
        }
        messageObtainMessage.arg2 = i;
        sendMessage(messageObtainMessage);
    }

    void sspRequestCallback(byte[] bArr, byte[] bArr2, int i, int i2, int i3) {
        BluetoothDevice device;
        int i4;
        if (this.mRemoteDevices.getDevice(bArr) == null) {
            this.mRemoteDevices.addDeviceProperties(bArr);
        }
        infoLog("sspRequestCallback: " + bArr + " name: " + bArr2 + " cod: " + i + " pairingVariant " + i2 + " passkey: " + i3);
        boolean z = false;
        int i5 = 1;
        switch (i2) {
            case 0:
                i4 = 2;
                i5 = i4;
                z = true;
                device = this.mRemoteDevices.getDevice(bArr);
                if (device == null) {
                    warnLog("Device is not known for:" + Utils.getAddressStringFromByte(bArr));
                    this.mRemoteDevices.addDeviceProperties(bArr);
                    device = this.mRemoteDevices.getDevice(bArr);
                }
                Message messageObtainMessage = obtainMessage(5);
                messageObtainMessage.obj = device;
                if (z) {
                    messageObtainMessage.arg1 = i3;
                }
                messageObtainMessage.arg2 = i5;
                sendMessage(messageObtainMessage);
                break;
            case 2:
                i5 = 3;
            case 1:
                device = this.mRemoteDevices.getDevice(bArr);
                if (device == null) {
                }
                Message messageObtainMessage2 = obtainMessage(5);
                messageObtainMessage2.obj = device;
                if (z) {
                }
                messageObtainMessage2.arg2 = i5;
                sendMessage(messageObtainMessage2);
                break;
            case 3:
                i4 = 4;
                i5 = i4;
                z = true;
                device = this.mRemoteDevices.getDevice(bArr);
                if (device == null) {
                }
                Message messageObtainMessage22 = obtainMessage(5);
                messageObtainMessage22.obj = device;
                if (z) {
                }
                messageObtainMessage22.arg2 = i5;
                sendMessage(messageObtainMessage22);
                break;
            default:
                errorLog("SSP Pairing variant not present");
                break;
        }
    }

    void pinRequestCallback(byte[] bArr, byte[] bArr2, int i, boolean z) {
        BluetoothDevice device = this.mRemoteDevices.getDevice(bArr);
        if (device == null) {
            this.mRemoteDevices.addDeviceProperties(bArr);
        }
        infoLog("pinRequestCallback: " + bArr + " name:" + bArr2 + " cod:" + i);
        Message messageObtainMessage = obtainMessage(6);
        messageObtainMessage.obj = device;
        messageObtainMessage.arg2 = z ? 1 : 0;
        sendMessage(messageObtainMessage);
    }

    private void clearProfilePriority(BluetoothDevice bluetoothDevice) {
        HidHostService hidHostService = HidHostService.getHidHostService();
        A2dpService a2dpService = A2dpService.getA2dpService();
        HeadsetService headsetService = HeadsetService.getHeadsetService();
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        A2dpSinkService a2dpSinkService = A2dpSinkService.getA2dpSinkService();
        PbapClientService pbapClientService = PbapClientService.getPbapClientService();
        if (hidHostService != null) {
            hidHostService.setPriority(bluetoothDevice, -1);
        }
        if (a2dpService != null) {
            a2dpService.setPriority(bluetoothDevice, -1);
        }
        if (headsetService != null) {
            headsetService.setPriority(bluetoothDevice, -1);
        }
        if (headsetClientService != null) {
            headsetClientService.setPriority(bluetoothDevice, -1);
        }
        if (a2dpSinkService != null) {
            a2dpSinkService.setPriority(bluetoothDevice, -1);
        }
        if (pbapClientService != null) {
            pbapClientService.setPriority(bluetoothDevice, -1);
        }
    }

    private String state2str(int i) {
        if (i == 10) {
            return "BOND_NONE";
        }
        if (i == 11) {
            return "BOND_BONDING";
        }
        if (i == 12) {
            return "BOND_BONDED";
        }
        return "UNKNOWN(" + i + ")";
    }

    private void infoLog(String str) {
        Log.i(TAG, str);
    }

    private void errorLog(String str) {
        Log.e(TAG, str);
    }

    private void warnLog(String str) {
        Log.w(TAG, str);
    }

    private int getUnbondReasonFromHALCode(int i) {
        if (i == 0) {
            return 0;
        }
        if (i == 10) {
            return 4;
        }
        if (i == 9) {
            return 1;
        }
        if (i == 11) {
            return 2;
        }
        if (i != 12) {
            return 9;
        }
        return 6;
    }
}
