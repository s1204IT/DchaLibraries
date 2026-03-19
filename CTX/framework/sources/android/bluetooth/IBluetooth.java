package android.bluetooth;

import android.bluetooth.IBluetoothCallback;
import android.bluetooth.IBluetoothSocketManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.ResultReceiver;

public interface IBluetooth extends IInterface {
    boolean cancelBondProcess(BluetoothDevice bluetoothDevice) throws RemoteException;

    boolean cancelDiscovery() throws RemoteException;

    boolean createBond(BluetoothDevice bluetoothDevice, int i) throws RemoteException;

    boolean createBondOutOfBand(BluetoothDevice bluetoothDevice, int i, OobData oobData) throws RemoteException;

    boolean disable() throws RemoteException;

    boolean enable() throws RemoteException;

    boolean enableNoAutoConnect() throws RemoteException;

    boolean factoryReset() throws RemoteException;

    boolean fetchRemoteUuids(BluetoothDevice bluetoothDevice) throws RemoteException;

    int getAdapterConnectionState() throws RemoteException;

    String getAddress() throws RemoteException;

    int getBatteryLevel(BluetoothDevice bluetoothDevice) throws RemoteException;

    BluetoothClass getBluetoothClass() throws RemoteException;

    int getBondState(BluetoothDevice bluetoothDevice) throws RemoteException;

    BluetoothDevice[] getBondedDevices() throws RemoteException;

    int getConnectionState(BluetoothDevice bluetoothDevice) throws RemoteException;

    int getDiscoverableTimeout() throws RemoteException;

    long getDiscoveryEndMillis() throws RemoteException;

    int getLeMaximumAdvertisingDataLength() throws RemoteException;

    int getMaxConnectedAudioDevices() throws RemoteException;

    int getMessageAccessPermission(BluetoothDevice bluetoothDevice) throws RemoteException;

    String getName() throws RemoteException;

    int getPhonebookAccessPermission(BluetoothDevice bluetoothDevice) throws RemoteException;

    int getProfileConnectionState(int i) throws RemoteException;

    String getRemoteAlias(BluetoothDevice bluetoothDevice) throws RemoteException;

    int getRemoteClass(BluetoothDevice bluetoothDevice) throws RemoteException;

    String getRemoteName(BluetoothDevice bluetoothDevice) throws RemoteException;

    int getRemoteType(BluetoothDevice bluetoothDevice) throws RemoteException;

    ParcelUuid[] getRemoteUuids(BluetoothDevice bluetoothDevice) throws RemoteException;

    int getScanMode() throws RemoteException;

    int getSimAccessPermission(BluetoothDevice bluetoothDevice) throws RemoteException;

    IBluetoothSocketManager getSocketManager() throws RemoteException;

    int getState() throws RemoteException;

    long getSupportedProfiles() throws RemoteException;

    ParcelUuid[] getUuids() throws RemoteException;

    boolean isActivityAndEnergyReportingSupported() throws RemoteException;

    boolean isBondingInitiatedLocally(BluetoothDevice bluetoothDevice) throws RemoteException;

    boolean isDiscovering() throws RemoteException;

    boolean isEnabled() throws RemoteException;

    boolean isLe2MPhySupported() throws RemoteException;

    boolean isLeCodedPhySupported() throws RemoteException;

    boolean isLeExtendedAdvertisingSupported() throws RemoteException;

    boolean isLePeriodicAdvertisingSupported() throws RemoteException;

    boolean isMultiAdvertisementSupported() throws RemoteException;

    boolean isOffloadedFilteringSupported() throws RemoteException;

    boolean isOffloadedScanBatchingSupported() throws RemoteException;

    void onBrEdrDown() throws RemoteException;

    void onLeServiceUp() throws RemoteException;

    void registerCallback(IBluetoothCallback iBluetoothCallback) throws RemoteException;

    boolean removeBond(BluetoothDevice bluetoothDevice) throws RemoteException;

    BluetoothActivityEnergyInfo reportActivityInfo() throws RemoteException;

    void requestActivityInfo(ResultReceiver resultReceiver) throws RemoteException;

    boolean sdpSearch(BluetoothDevice bluetoothDevice, ParcelUuid parcelUuid) throws RemoteException;

    void sendConnectionStateChange(BluetoothDevice bluetoothDevice, int i, int i2, int i3) throws RemoteException;

    boolean setBluetoothClass(BluetoothClass bluetoothClass) throws RemoteException;

    boolean setDiscoverableTimeout(int i) throws RemoteException;

    boolean setMessageAccessPermission(BluetoothDevice bluetoothDevice, int i) throws RemoteException;

    boolean setName(String str) throws RemoteException;

    boolean setPairingConfirmation(BluetoothDevice bluetoothDevice, boolean z) throws RemoteException;

    boolean setPasskey(BluetoothDevice bluetoothDevice, boolean z, int i, byte[] bArr) throws RemoteException;

    boolean setPhonebookAccessPermission(BluetoothDevice bluetoothDevice, int i) throws RemoteException;

    boolean setPin(BluetoothDevice bluetoothDevice, boolean z, int i, byte[] bArr) throws RemoteException;

    boolean setRemoteAlias(BluetoothDevice bluetoothDevice, String str) throws RemoteException;

    boolean setScanMode(int i, int i2) throws RemoteException;

    boolean setSimAccessPermission(BluetoothDevice bluetoothDevice, int i) throws RemoteException;

    boolean startDiscovery() throws RemoteException;

    void unregisterCallback(IBluetoothCallback iBluetoothCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements IBluetooth {
        private static final String DESCRIPTOR = "android.bluetooth.IBluetooth";
        static final int TRANSACTION_cancelBondProcess = 25;
        static final int TRANSACTION_cancelDiscovery = 17;
        static final int TRANSACTION_createBond = 23;
        static final int TRANSACTION_createBondOutOfBand = 24;
        static final int TRANSACTION_disable = 5;
        static final int TRANSACTION_enable = 3;
        static final int TRANSACTION_enableNoAutoConnect = 4;
        static final int TRANSACTION_factoryReset = 54;
        static final int TRANSACTION_fetchRemoteUuids = 37;
        static final int TRANSACTION_getAdapterConnectionState = 20;
        static final int TRANSACTION_getAddress = 6;
        static final int TRANSACTION_getBatteryLevel = 39;
        static final int TRANSACTION_getBluetoothClass = 10;
        static final int TRANSACTION_getBondState = 27;
        static final int TRANSACTION_getBondedDevices = 22;
        static final int TRANSACTION_getConnectionState = 30;
        static final int TRANSACTION_getDiscoverableTimeout = 14;
        static final int TRANSACTION_getDiscoveryEndMillis = 19;
        static final int TRANSACTION_getLeMaximumAdvertisingDataLength = 63;
        static final int TRANSACTION_getMaxConnectedAudioDevices = 40;
        static final int TRANSACTION_getMessageAccessPermission = 46;
        static final int TRANSACTION_getName = 9;
        static final int TRANSACTION_getPhonebookAccessPermission = 44;
        static final int TRANSACTION_getProfileConnectionState = 21;
        static final int TRANSACTION_getRemoteAlias = 33;
        static final int TRANSACTION_getRemoteClass = 35;
        static final int TRANSACTION_getRemoteName = 31;
        static final int TRANSACTION_getRemoteType = 32;
        static final int TRANSACTION_getRemoteUuids = 36;
        static final int TRANSACTION_getScanMode = 12;
        static final int TRANSACTION_getSimAccessPermission = 48;
        static final int TRANSACTION_getSocketManager = 53;
        static final int TRANSACTION_getState = 2;
        static final int TRANSACTION_getSupportedProfiles = 29;
        static final int TRANSACTION_getUuids = 7;
        static final int TRANSACTION_isActivityAndEnergyReportingSupported = 58;
        static final int TRANSACTION_isBondingInitiatedLocally = 28;
        static final int TRANSACTION_isDiscovering = 18;
        static final int TRANSACTION_isEnabled = 1;
        static final int TRANSACTION_isLe2MPhySupported = 59;
        static final int TRANSACTION_isLeCodedPhySupported = 60;
        static final int TRANSACTION_isLeExtendedAdvertisingSupported = 61;
        static final int TRANSACTION_isLePeriodicAdvertisingSupported = 62;
        static final int TRANSACTION_isMultiAdvertisementSupported = 55;
        static final int TRANSACTION_isOffloadedFilteringSupported = 56;
        static final int TRANSACTION_isOffloadedScanBatchingSupported = 57;
        static final int TRANSACTION_onBrEdrDown = 67;
        static final int TRANSACTION_onLeServiceUp = 66;
        static final int TRANSACTION_registerCallback = 51;
        static final int TRANSACTION_removeBond = 26;
        static final int TRANSACTION_reportActivityInfo = 64;
        static final int TRANSACTION_requestActivityInfo = 65;
        static final int TRANSACTION_sdpSearch = 38;
        static final int TRANSACTION_sendConnectionStateChange = 50;
        static final int TRANSACTION_setBluetoothClass = 11;
        static final int TRANSACTION_setDiscoverableTimeout = 15;
        static final int TRANSACTION_setMessageAccessPermission = 47;
        static final int TRANSACTION_setName = 8;
        static final int TRANSACTION_setPairingConfirmation = 43;
        static final int TRANSACTION_setPasskey = 42;
        static final int TRANSACTION_setPhonebookAccessPermission = 45;
        static final int TRANSACTION_setPin = 41;
        static final int TRANSACTION_setRemoteAlias = 34;
        static final int TRANSACTION_setScanMode = 13;
        static final int TRANSACTION_setSimAccessPermission = 49;
        static final int TRANSACTION_startDiscovery = 16;
        static final int TRANSACTION_unregisterCallback = 52;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBluetooth asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IBluetooth)) {
                return (IBluetooth) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            BluetoothDevice bluetoothDeviceCreateFromParcel;
            BluetoothDevice bluetoothDeviceCreateFromParcel2;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsEnabled = isEnabled();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsEnabled ? 1 : 0);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int state = getState();
                    parcel2.writeNoException();
                    parcel2.writeInt(state);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zEnable = enable();
                    parcel2.writeNoException();
                    parcel2.writeInt(zEnable ? 1 : 0);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zEnableNoAutoConnect = enableNoAutoConnect();
                    parcel2.writeNoException();
                    parcel2.writeInt(zEnableNoAutoConnect ? 1 : 0);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zDisable = disable();
                    parcel2.writeNoException();
                    parcel2.writeInt(zDisable ? 1 : 0);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    String address = getAddress();
                    parcel2.writeNoException();
                    parcel2.writeString(address);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParcelUuid[] uuids = getUuids();
                    parcel2.writeNoException();
                    parcel2.writeTypedArray(uuids, 1);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean name = setName(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(name ? 1 : 0);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    String name2 = getName();
                    parcel2.writeNoException();
                    parcel2.writeString(name2);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    BluetoothClass bluetoothClass = getBluetoothClass();
                    parcel2.writeNoException();
                    if (bluetoothClass != null) {
                        parcel2.writeInt(1);
                        bluetoothClass.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean bluetoothClass2 = setBluetoothClass(parcel.readInt() != 0 ? BluetoothClass.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(bluetoothClass2 ? 1 : 0);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    int scanMode = getScanMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(scanMode);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean scanMode2 = setScanMode(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(scanMode2 ? 1 : 0);
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    int discoverableTimeout = getDiscoverableTimeout();
                    parcel2.writeNoException();
                    parcel2.writeInt(discoverableTimeout);
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean discoverableTimeout2 = setDiscoverableTimeout(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(discoverableTimeout2 ? 1 : 0);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zStartDiscovery = startDiscovery();
                    parcel2.writeNoException();
                    parcel2.writeInt(zStartDiscovery ? 1 : 0);
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zCancelDiscovery = cancelDiscovery();
                    parcel2.writeNoException();
                    parcel2.writeInt(zCancelDiscovery ? 1 : 0);
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsDiscovering = isDiscovering();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsDiscovering ? 1 : 0);
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    long discoveryEndMillis = getDiscoveryEndMillis();
                    parcel2.writeNoException();
                    parcel2.writeLong(discoveryEndMillis);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    int adapterConnectionState = getAdapterConnectionState();
                    parcel2.writeNoException();
                    parcel2.writeInt(adapterConnectionState);
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    int profileConnectionState = getProfileConnectionState(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(profileConnectionState);
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    BluetoothDevice[] bondedDevices = getBondedDevices();
                    parcel2.writeNoException();
                    parcel2.writeTypedArray(bondedDevices, 1);
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zCreateBond = createBond(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCreateBond ? 1 : 0);
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel = BluetoothDevice.CREATOR.createFromParcel(parcel);
                    } else {
                        bluetoothDeviceCreateFromParcel = null;
                    }
                    boolean zCreateBondOutOfBand = createBondOutOfBand(bluetoothDeviceCreateFromParcel, parcel.readInt(), parcel.readInt() != 0 ? OobData.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zCreateBondOutOfBand ? 1 : 0);
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zCancelBondProcess = cancelBondProcess(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zCancelBondProcess ? 1 : 0);
                    return true;
                case 26:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRemoveBond = removeBond(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zRemoveBond ? 1 : 0);
                    return true;
                case 27:
                    parcel.enforceInterface(DESCRIPTOR);
                    int bondState = getBondState(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(bondState);
                    return true;
                case 28:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsBondingInitiatedLocally = isBondingInitiatedLocally(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsBondingInitiatedLocally ? 1 : 0);
                    return true;
                case 29:
                    parcel.enforceInterface(DESCRIPTOR);
                    long supportedProfiles = getSupportedProfiles();
                    parcel2.writeNoException();
                    parcel2.writeLong(supportedProfiles);
                    return true;
                case 30:
                    parcel.enforceInterface(DESCRIPTOR);
                    int connectionState = getConnectionState(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(connectionState);
                    return true;
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    String remoteName = getRemoteName(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeString(remoteName);
                    return true;
                case 32:
                    parcel.enforceInterface(DESCRIPTOR);
                    int remoteType = getRemoteType(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(remoteType);
                    return true;
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    String remoteAlias = getRemoteAlias(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeString(remoteAlias);
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean remoteAlias2 = setRemoteAlias(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(remoteAlias2 ? 1 : 0);
                    return true;
                case 35:
                    parcel.enforceInterface(DESCRIPTOR);
                    int remoteClass = getRemoteClass(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(remoteClass);
                    return true;
                case 36:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParcelUuid[] remoteUuids = getRemoteUuids(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeTypedArray(remoteUuids, 1);
                    return true;
                case 37:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zFetchRemoteUuids = fetchRemoteUuids(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zFetchRemoteUuids ? 1 : 0);
                    return true;
                case 38:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel2 = BluetoothDevice.CREATOR.createFromParcel(parcel);
                    } else {
                        bluetoothDeviceCreateFromParcel2 = null;
                    }
                    boolean zSdpSearch = sdpSearch(bluetoothDeviceCreateFromParcel2, parcel.readInt() != 0 ? ParcelUuid.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zSdpSearch ? 1 : 0);
                    return true;
                case 39:
                    parcel.enforceInterface(DESCRIPTOR);
                    int batteryLevel = getBatteryLevel(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(batteryLevel);
                    return true;
                case 40:
                    parcel.enforceInterface(DESCRIPTOR);
                    int maxConnectedAudioDevices = getMaxConnectedAudioDevices();
                    parcel2.writeNoException();
                    parcel2.writeInt(maxConnectedAudioDevices);
                    return true;
                case 41:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean pin = setPin(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0, parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(pin ? 1 : 0);
                    return true;
                case 42:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean passkey = setPasskey(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0, parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(passkey ? 1 : 0);
                    return true;
                case 43:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean pairingConfirmation = setPairingConfirmation(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(pairingConfirmation ? 1 : 0);
                    return true;
                case 44:
                    parcel.enforceInterface(DESCRIPTOR);
                    int phonebookAccessPermission = getPhonebookAccessPermission(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(phonebookAccessPermission);
                    return true;
                case 45:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean phonebookAccessPermission2 = setPhonebookAccessPermission(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(phonebookAccessPermission2 ? 1 : 0);
                    return true;
                case 46:
                    parcel.enforceInterface(DESCRIPTOR);
                    int messageAccessPermission = getMessageAccessPermission(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(messageAccessPermission);
                    return true;
                case 47:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean messageAccessPermission2 = setMessageAccessPermission(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(messageAccessPermission2 ? 1 : 0);
                    return true;
                case 48:
                    parcel.enforceInterface(DESCRIPTOR);
                    int simAccessPermission = getSimAccessPermission(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(simAccessPermission);
                    return true;
                case 49:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean simAccessPermission2 = setSimAccessPermission(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(simAccessPermission2 ? 1 : 0);
                    return true;
                case 50:
                    parcel.enforceInterface(DESCRIPTOR);
                    sendConnectionStateChange(parcel.readInt() != 0 ? BluetoothDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 51:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerCallback(IBluetoothCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 52:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterCallback(IBluetoothCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 53:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBluetoothSocketManager socketManager = getSocketManager();
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(socketManager != null ? socketManager.asBinder() : null);
                    return true;
                case 54:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zFactoryReset = factoryReset();
                    parcel2.writeNoException();
                    parcel2.writeInt(zFactoryReset ? 1 : 0);
                    return true;
                case 55:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsMultiAdvertisementSupported = isMultiAdvertisementSupported();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsMultiAdvertisementSupported ? 1 : 0);
                    return true;
                case 56:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsOffloadedFilteringSupported = isOffloadedFilteringSupported();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsOffloadedFilteringSupported ? 1 : 0);
                    return true;
                case 57:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsOffloadedScanBatchingSupported = isOffloadedScanBatchingSupported();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsOffloadedScanBatchingSupported ? 1 : 0);
                    return true;
                case 58:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsActivityAndEnergyReportingSupported = isActivityAndEnergyReportingSupported();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsActivityAndEnergyReportingSupported ? 1 : 0);
                    return true;
                case 59:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsLe2MPhySupported = isLe2MPhySupported();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsLe2MPhySupported ? 1 : 0);
                    return true;
                case 60:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsLeCodedPhySupported = isLeCodedPhySupported();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsLeCodedPhySupported ? 1 : 0);
                    return true;
                case 61:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsLeExtendedAdvertisingSupported = isLeExtendedAdvertisingSupported();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsLeExtendedAdvertisingSupported ? 1 : 0);
                    return true;
                case 62:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsLePeriodicAdvertisingSupported = isLePeriodicAdvertisingSupported();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsLePeriodicAdvertisingSupported ? 1 : 0);
                    return true;
                case 63:
                    parcel.enforceInterface(DESCRIPTOR);
                    int leMaximumAdvertisingDataLength = getLeMaximumAdvertisingDataLength();
                    parcel2.writeNoException();
                    parcel2.writeInt(leMaximumAdvertisingDataLength);
                    return true;
                case 64:
                    parcel.enforceInterface(DESCRIPTOR);
                    BluetoothActivityEnergyInfo bluetoothActivityEnergyInfoReportActivityInfo = reportActivityInfo();
                    parcel2.writeNoException();
                    if (bluetoothActivityEnergyInfoReportActivityInfo != null) {
                        parcel2.writeInt(1);
                        bluetoothActivityEnergyInfoReportActivityInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 65:
                    parcel.enforceInterface(DESCRIPTOR);
                    requestActivityInfo(parcel.readInt() != 0 ? ResultReceiver.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 66:
                    parcel.enforceInterface(DESCRIPTOR);
                    onLeServiceUp();
                    parcel2.writeNoException();
                    return true;
                case 67:
                    parcel.enforceInterface(DESCRIPTOR);
                    onBrEdrDown();
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IBluetooth {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public boolean isEnabled() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean enable() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean enableNoAutoConnect() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean disable() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getAddress() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParcelUuid[] getUuids() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (ParcelUuid[]) parcelObtain2.createTypedArray(ParcelUuid.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setName(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getName() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public BluetoothClass getBluetoothClass() throws RemoteException {
                BluetoothClass bluetoothClassCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        bluetoothClassCreateFromParcel = BluetoothClass.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        bluetoothClassCreateFromParcel = null;
                    }
                    return bluetoothClassCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setBluetoothClass(BluetoothClass bluetoothClass) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothClass != null) {
                        parcelObtain.writeInt(1);
                        bluetoothClass.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getScanMode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setScanMode(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getDiscoverableTimeout() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setDiscoverableTimeout(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean startDiscovery() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean cancelDiscovery() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isDiscovering() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long getDiscoveryEndMillis() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getAdapterConnectionState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getProfileConnectionState(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public BluetoothDevice[] getBondedDevices() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (BluetoothDevice[]) parcelObtain2.createTypedArray(BluetoothDevice.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean createBond(BluetoothDevice bluetoothDevice, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean createBondOutOfBand(BluetoothDevice bluetoothDevice, int i, OobData oobData) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    if (oobData != null) {
                        parcelObtain.writeInt(1);
                        oobData.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean cancelBondProcess(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean removeBond(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getBondState(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isBondingInitiatedLocally(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long getSupportedProfiles() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getConnectionState(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getRemoteName(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getRemoteType(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getRemoteAlias(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setRemoteAlias(BluetoothDevice bluetoothDevice, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    this.mRemote.transact(34, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getRemoteClass(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(35, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParcelUuid[] getRemoteUuids(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(36, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (ParcelUuid[]) parcelObtain2.createTypedArray(ParcelUuid.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean fetchRemoteUuids(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(37, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean sdpSearch(BluetoothDevice bluetoothDevice, ParcelUuid parcelUuid) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (parcelUuid != null) {
                        parcelObtain.writeInt(1);
                        parcelUuid.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(38, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getBatteryLevel(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(39, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getMaxConnectedAudioDevices() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setPin(BluetoothDevice bluetoothDevice, boolean z, int i, byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z2 = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(41, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z2 = false;
                    }
                    return z2;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setPasskey(BluetoothDevice bluetoothDevice, boolean z, int i, byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z2 = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(42, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z2 = false;
                    }
                    return z2;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setPairingConfirmation(BluetoothDevice bluetoothDevice, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z2 = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(43, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z2 = false;
                    }
                    return z2;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getPhonebookAccessPermission(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(44, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setPhonebookAccessPermission(BluetoothDevice bluetoothDevice, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(45, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getMessageAccessPermission(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(46, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setMessageAccessPermission(BluetoothDevice bluetoothDevice, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(47, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getSimAccessPermission(BluetoothDevice bluetoothDevice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(48, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setSimAccessPermission(BluetoothDevice bluetoothDevice, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(49, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendConnectionStateChange(BluetoothDevice bluetoothDevice, int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bluetoothDevice != null) {
                        parcelObtain.writeInt(1);
                        bluetoothDevice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(50, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerCallback(IBluetoothCallback iBluetoothCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBluetoothCallback != null ? iBluetoothCallback.asBinder() : null);
                    this.mRemote.transact(51, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterCallback(IBluetoothCallback iBluetoothCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBluetoothCallback != null ? iBluetoothCallback.asBinder() : null);
                    this.mRemote.transact(52, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IBluetoothSocketManager getSocketManager() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(53, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IBluetoothSocketManager.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean factoryReset() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(54, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isMultiAdvertisementSupported() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(55, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isOffloadedFilteringSupported() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(56, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isOffloadedScanBatchingSupported() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(57, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isActivityAndEnergyReportingSupported() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(58, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isLe2MPhySupported() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(59, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isLeCodedPhySupported() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(60, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isLeExtendedAdvertisingSupported() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(61, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isLePeriodicAdvertisingSupported() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(62, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getLeMaximumAdvertisingDataLength() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(63, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public BluetoothActivityEnergyInfo reportActivityInfo() throws RemoteException {
                BluetoothActivityEnergyInfo bluetoothActivityEnergyInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(64, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        bluetoothActivityEnergyInfoCreateFromParcel = BluetoothActivityEnergyInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        bluetoothActivityEnergyInfoCreateFromParcel = null;
                    }
                    return bluetoothActivityEnergyInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void requestActivityInfo(ResultReceiver resultReceiver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (resultReceiver != null) {
                        parcelObtain.writeInt(1);
                        resultReceiver.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(65, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onLeServiceUp() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(66, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onBrEdrDown() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(67, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
