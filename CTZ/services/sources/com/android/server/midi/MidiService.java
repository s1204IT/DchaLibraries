package com.android.server.midi;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.media.midi.IBluetoothMidiService;
import android.media.midi.IMidiDeviceListener;
import android.media.midi.IMidiDeviceOpenCallback;
import android.media.midi.IMidiDeviceServer;
import android.media.midi.IMidiManager;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceStatus;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import com.android.server.pm.Settings;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MidiService extends IMidiManager.Stub {
    private static final MidiDeviceInfo[] EMPTY_DEVICE_INFO_ARRAY = new MidiDeviceInfo[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String TAG = "MidiService";
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final HashMap<IBinder, Client> mClients = new HashMap<>();
    private final HashMap<MidiDeviceInfo, Device> mDevicesByInfo = new HashMap<>();
    private final HashMap<BluetoothDevice, Device> mBluetoothDevices = new HashMap<>();
    private final HashMap<IBinder, Device> mDevicesByServer = new HashMap<>();
    private int mNextDeviceId = 1;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageAdded(String str, int i) throws Throwable {
            MidiService.this.addPackageDeviceServers(str);
        }

        public void onPackageModified(String str) throws Throwable {
            MidiService.this.removePackageDeviceServers(str);
            MidiService.this.addPackageDeviceServers(str);
        }

        public void onPackageRemoved(String str, int i) {
            MidiService.this.removePackageDeviceServers(str);
        }
    };
    private int mBluetoothServiceUid = -1;

    public static class Lifecycle extends SystemService {
        private MidiService mMidiService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mMidiService = new MidiService(getContext());
            publishBinderService("midi", this.mMidiService);
        }

        @Override
        public void onUnlockUser(int i) throws Throwable {
            if (i == 0) {
                this.mMidiService.onUnlockUser();
            }
        }
    }

    private final class Client implements IBinder.DeathRecipient {
        private final IBinder mToken;
        private final HashMap<IBinder, IMidiDeviceListener> mListeners = new HashMap<>();
        private final HashMap<IBinder, DeviceConnection> mDeviceConnections = new HashMap<>();
        private final int mUid = Binder.getCallingUid();
        private final int mPid = Binder.getCallingPid();

        public Client(IBinder iBinder) {
            this.mToken = iBinder;
        }

        public int getUid() {
            return this.mUid;
        }

        public void addListener(IMidiDeviceListener iMidiDeviceListener) {
            this.mListeners.put(iMidiDeviceListener.asBinder(), iMidiDeviceListener);
        }

        public void removeListener(IMidiDeviceListener iMidiDeviceListener) {
            this.mListeners.remove(iMidiDeviceListener.asBinder());
            if (this.mListeners.size() == 0 && this.mDeviceConnections.size() == 0) {
                close();
            }
        }

        public void addDeviceConnection(Device device, IMidiDeviceOpenCallback iMidiDeviceOpenCallback) {
            DeviceConnection deviceConnection = MidiService.this.new DeviceConnection(device, this, iMidiDeviceOpenCallback);
            this.mDeviceConnections.put(deviceConnection.getToken(), deviceConnection);
            device.addDeviceConnection(deviceConnection);
        }

        public void removeDeviceConnection(IBinder iBinder) {
            DeviceConnection deviceConnectionRemove = this.mDeviceConnections.remove(iBinder);
            if (deviceConnectionRemove != null) {
                deviceConnectionRemove.getDevice().removeDeviceConnection(deviceConnectionRemove);
            }
            if (this.mListeners.size() == 0 && this.mDeviceConnections.size() == 0) {
                close();
            }
        }

        public void removeDeviceConnection(DeviceConnection deviceConnection) {
            this.mDeviceConnections.remove(deviceConnection.getToken());
            if (this.mListeners.size() == 0 && this.mDeviceConnections.size() == 0) {
                close();
            }
        }

        public void deviceAdded(Device device) {
            if (device.isUidAllowed(this.mUid)) {
                MidiDeviceInfo deviceInfo = device.getDeviceInfo();
                try {
                    Iterator<IMidiDeviceListener> it = this.mListeners.values().iterator();
                    while (it.hasNext()) {
                        it.next().onDeviceAdded(deviceInfo);
                    }
                } catch (RemoteException e) {
                    Log.e(MidiService.TAG, "remote exception", e);
                }
            }
        }

        public void deviceRemoved(Device device) {
            if (device.isUidAllowed(this.mUid)) {
                MidiDeviceInfo deviceInfo = device.getDeviceInfo();
                try {
                    Iterator<IMidiDeviceListener> it = this.mListeners.values().iterator();
                    while (it.hasNext()) {
                        it.next().onDeviceRemoved(deviceInfo);
                    }
                } catch (RemoteException e) {
                    Log.e(MidiService.TAG, "remote exception", e);
                }
            }
        }

        public void deviceStatusChanged(Device device, MidiDeviceStatus midiDeviceStatus) {
            if (device.isUidAllowed(this.mUid)) {
                try {
                    Iterator<IMidiDeviceListener> it = this.mListeners.values().iterator();
                    while (it.hasNext()) {
                        it.next().onDeviceStatusChanged(midiDeviceStatus);
                    }
                } catch (RemoteException e) {
                    Log.e(MidiService.TAG, "remote exception", e);
                }
            }
        }

        private void close() {
            synchronized (MidiService.this.mClients) {
                MidiService.this.mClients.remove(this.mToken);
                this.mToken.unlinkToDeath(this, 0);
            }
            for (DeviceConnection deviceConnection : this.mDeviceConnections.values()) {
                deviceConnection.getDevice().removeDeviceConnection(deviceConnection);
            }
        }

        @Override
        public void binderDied() {
            Log.d(MidiService.TAG, "Client died: " + this);
            close();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("Client: UID: ");
            sb.append(this.mUid);
            sb.append(" PID: ");
            sb.append(this.mPid);
            sb.append(" listener count: ");
            sb.append(this.mListeners.size());
            sb.append(" Device Connections:");
            for (DeviceConnection deviceConnection : this.mDeviceConnections.values()) {
                sb.append(" <device ");
                sb.append(deviceConnection.getDevice().getDeviceInfo().getId());
                sb.append(">");
            }
            return sb.toString();
        }
    }

    private Client getClient(IBinder iBinder) {
        Client client;
        synchronized (this.mClients) {
            client = this.mClients.get(iBinder);
            if (client == null) {
                client = new Client(iBinder);
                try {
                    iBinder.linkToDeath(client, 0);
                    this.mClients.put(iBinder, client);
                } catch (RemoteException e) {
                    return null;
                }
            }
        }
        return client;
    }

    private final class Device implements IBinder.DeathRecipient {
        private final BluetoothDevice mBluetoothDevice;
        private final ArrayList<DeviceConnection> mDeviceConnections;
        private MidiDeviceInfo mDeviceInfo;
        private MidiDeviceStatus mDeviceStatus;
        private IMidiDeviceServer mServer;
        private ServiceConnection mServiceConnection;
        private final ServiceInfo mServiceInfo;
        private final int mUid;

        public Device(IMidiDeviceServer iMidiDeviceServer, MidiDeviceInfo midiDeviceInfo, ServiceInfo serviceInfo, int i) {
            this.mDeviceConnections = new ArrayList<>();
            this.mDeviceInfo = midiDeviceInfo;
            this.mServiceInfo = serviceInfo;
            this.mUid = i;
            this.mBluetoothDevice = (BluetoothDevice) midiDeviceInfo.getProperties().getParcelable("bluetooth_device");
            setDeviceServer(iMidiDeviceServer);
        }

        public Device(BluetoothDevice bluetoothDevice) {
            this.mDeviceConnections = new ArrayList<>();
            this.mBluetoothDevice = bluetoothDevice;
            this.mServiceInfo = null;
            this.mUid = MidiService.this.mBluetoothServiceUid;
        }

        private void setDeviceServer(IMidiDeviceServer iMidiDeviceServer) {
            if (iMidiDeviceServer != null) {
                if (this.mServer != null) {
                    Log.e(MidiService.TAG, "mServer already set in setDeviceServer");
                    return;
                }
                IBinder iBinderAsBinder = iMidiDeviceServer.asBinder();
                try {
                    iBinderAsBinder.linkToDeath(this, 0);
                    this.mServer = iMidiDeviceServer;
                    MidiService.this.mDevicesByServer.put(iBinderAsBinder, this);
                } catch (RemoteException e) {
                    this.mServer = null;
                    return;
                }
            } else if (this.mServer != null) {
                iMidiDeviceServer = this.mServer;
                this.mServer = null;
                IBinder iBinderAsBinder2 = iMidiDeviceServer.asBinder();
                MidiService.this.mDevicesByServer.remove(iBinderAsBinder2);
                try {
                    iMidiDeviceServer.closeDevice();
                    iBinderAsBinder2.unlinkToDeath(this, 0);
                } catch (RemoteException e2) {
                }
            }
            if (this.mDeviceConnections != null) {
                Iterator<DeviceConnection> it = this.mDeviceConnections.iterator();
                while (it.hasNext()) {
                    it.next().notifyClient(iMidiDeviceServer);
                }
            }
        }

        public MidiDeviceInfo getDeviceInfo() {
            return this.mDeviceInfo;
        }

        public void setDeviceInfo(MidiDeviceInfo midiDeviceInfo) {
            this.mDeviceInfo = midiDeviceInfo;
        }

        public MidiDeviceStatus getDeviceStatus() {
            return this.mDeviceStatus;
        }

        public void setDeviceStatus(MidiDeviceStatus midiDeviceStatus) {
            this.mDeviceStatus = midiDeviceStatus;
        }

        public IMidiDeviceServer getDeviceServer() {
            return this.mServer;
        }

        public ServiceInfo getServiceInfo() {
            return this.mServiceInfo;
        }

        public String getPackageName() {
            if (this.mServiceInfo == null) {
                return null;
            }
            return this.mServiceInfo.packageName;
        }

        public int getUid() {
            return this.mUid;
        }

        public boolean isUidAllowed(int i) {
            return !this.mDeviceInfo.isPrivate() || this.mUid == i;
        }

        public void addDeviceConnection(DeviceConnection deviceConnection) {
            Intent intent;
            synchronized (this.mDeviceConnections) {
                if (this.mServer != null) {
                    this.mDeviceConnections.add(deviceConnection);
                    deviceConnection.notifyClient(this.mServer);
                } else if (this.mServiceConnection == null && (this.mServiceInfo != null || this.mBluetoothDevice != null)) {
                    this.mDeviceConnections.add(deviceConnection);
                    this.mServiceConnection = new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                            IMidiDeviceServer iMidiDeviceServerAsInterface;
                            if (Device.this.mBluetoothDevice != null) {
                                try {
                                    iMidiDeviceServerAsInterface = IMidiDeviceServer.Stub.asInterface(IBluetoothMidiService.Stub.asInterface(iBinder).addBluetoothDevice(Device.this.mBluetoothDevice));
                                } catch (RemoteException e) {
                                    Log.e(MidiService.TAG, "Could not call addBluetoothDevice()", e);
                                    iMidiDeviceServerAsInterface = null;
                                }
                            } else {
                                iMidiDeviceServerAsInterface = IMidiDeviceServer.Stub.asInterface(iBinder);
                            }
                            Device.this.setDeviceServer(iMidiDeviceServerAsInterface);
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName componentName) {
                            Device.this.setDeviceServer(null);
                            Device.this.mServiceConnection = null;
                        }
                    };
                    if (this.mBluetoothDevice != null) {
                        intent = new Intent("android.media.midi.BluetoothMidiService");
                        intent.setComponent(new ComponentName("com.android.bluetoothmidiservice", "com.android.bluetoothmidiservice.BluetoothMidiService"));
                    } else {
                        intent = new Intent("android.media.midi.MidiDeviceService");
                        intent.setComponent(new ComponentName(this.mServiceInfo.packageName, this.mServiceInfo.name));
                    }
                    if (!MidiService.this.mContext.bindService(intent, this.mServiceConnection, 1)) {
                        Log.e(MidiService.TAG, "Unable to bind service: " + intent);
                        setDeviceServer(null);
                        this.mServiceConnection = null;
                    }
                } else {
                    Log.e(MidiService.TAG, "No way to connect to device in addDeviceConnection");
                    deviceConnection.notifyClient(null);
                }
            }
        }

        public void removeDeviceConnection(DeviceConnection deviceConnection) {
            synchronized (this.mDeviceConnections) {
                this.mDeviceConnections.remove(deviceConnection);
                if (this.mDeviceConnections.size() == 0 && this.mServiceConnection != null) {
                    MidiService.this.mContext.unbindService(this.mServiceConnection);
                    this.mServiceConnection = null;
                    if (this.mBluetoothDevice != null) {
                        synchronized (MidiService.this.mDevicesByInfo) {
                            closeLocked();
                        }
                    } else {
                        setDeviceServer(null);
                    }
                }
            }
        }

        public void closeLocked() {
            synchronized (this.mDeviceConnections) {
                for (DeviceConnection deviceConnection : this.mDeviceConnections) {
                    deviceConnection.getClient().removeDeviceConnection(deviceConnection);
                }
                this.mDeviceConnections.clear();
            }
            setDeviceServer(null);
            if (this.mServiceInfo == null) {
                MidiService.this.removeDeviceLocked(this);
            } else {
                this.mDeviceStatus = new MidiDeviceStatus(this.mDeviceInfo);
            }
            if (this.mBluetoothDevice != null) {
                MidiService.this.mBluetoothDevices.remove(this.mBluetoothDevice);
            }
        }

        @Override
        public void binderDied() {
            Log.d(MidiService.TAG, "Device died: " + this);
            synchronized (MidiService.this.mDevicesByInfo) {
                closeLocked();
            }
        }

        public String toString() {
            return "Device Info: " + this.mDeviceInfo + " Status: " + this.mDeviceStatus + " UID: " + this.mUid + " DeviceConnection count: " + this.mDeviceConnections.size() + " mServiceConnection: " + this.mServiceConnection;
        }
    }

    private final class DeviceConnection {
        private IMidiDeviceOpenCallback mCallback;
        private final Client mClient;
        private final Device mDevice;
        private final IBinder mToken = new Binder();

        public DeviceConnection(Device device, Client client, IMidiDeviceOpenCallback iMidiDeviceOpenCallback) {
            this.mDevice = device;
            this.mClient = client;
            this.mCallback = iMidiDeviceOpenCallback;
        }

        public Device getDevice() {
            return this.mDevice;
        }

        public Client getClient() {
            return this.mClient;
        }

        public IBinder getToken() {
            return this.mToken;
        }

        public void notifyClient(IMidiDeviceServer iMidiDeviceServer) {
            if (this.mCallback != null) {
                try {
                    this.mCallback.onDeviceOpened(iMidiDeviceServer, iMidiDeviceServer == null ? null : this.mToken);
                } catch (RemoteException e) {
                }
                this.mCallback = null;
            }
        }

        public String toString() {
            return "DeviceConnection Device ID: " + this.mDevice.getDeviceInfo().getId();
        }
    }

    public MidiService(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }

    private void onUnlockUser() throws Throwable {
        PackageInfo packageInfo;
        this.mPackageMonitor.register(this.mContext, (Looper) null, true);
        List<ResolveInfo> listQueryIntentServices = this.mPackageManager.queryIntentServices(new Intent("android.media.midi.MidiDeviceService"), 128);
        if (listQueryIntentServices != null) {
            int size = listQueryIntentServices.size();
            for (int i = 0; i < size; i++) {
                ServiceInfo serviceInfo = listQueryIntentServices.get(i).serviceInfo;
                if (serviceInfo != null) {
                    addPackageDeviceServer(serviceInfo);
                }
            }
        }
        try {
            packageInfo = this.mPackageManager.getPackageInfo("com.android.bluetoothmidiservice", 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
        }
        if (packageInfo != null && packageInfo.applicationInfo != null) {
            this.mBluetoothServiceUid = packageInfo.applicationInfo.uid;
        } else {
            this.mBluetoothServiceUid = -1;
        }
    }

    public void registerListener(IBinder iBinder, IMidiDeviceListener iMidiDeviceListener) {
        Client client = getClient(iBinder);
        if (client == null) {
            return;
        }
        client.addListener(iMidiDeviceListener);
        updateStickyDeviceStatus(client.mUid, iMidiDeviceListener);
    }

    public void unregisterListener(IBinder iBinder, IMidiDeviceListener iMidiDeviceListener) {
        Client client = getClient(iBinder);
        if (client == null) {
            return;
        }
        client.removeListener(iMidiDeviceListener);
    }

    private void updateStickyDeviceStatus(int i, IMidiDeviceListener iMidiDeviceListener) {
        synchronized (this.mDevicesByInfo) {
            for (Device device : this.mDevicesByInfo.values()) {
                if (device.isUidAllowed(i)) {
                    try {
                        MidiDeviceStatus deviceStatus = device.getDeviceStatus();
                        if (deviceStatus != null) {
                            iMidiDeviceListener.onDeviceStatusChanged(deviceStatus);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote exception", e);
                    }
                }
            }
        }
    }

    public MidiDeviceInfo[] getDevices() {
        ArrayList arrayList = new ArrayList();
        int callingUid = Binder.getCallingUid();
        synchronized (this.mDevicesByInfo) {
            for (Device device : this.mDevicesByInfo.values()) {
                if (device.isUidAllowed(callingUid)) {
                    arrayList.add(device.getDeviceInfo());
                }
            }
        }
        return (MidiDeviceInfo[]) arrayList.toArray(EMPTY_DEVICE_INFO_ARRAY);
    }

    public void openDevice(IBinder iBinder, MidiDeviceInfo midiDeviceInfo, IMidiDeviceOpenCallback iMidiDeviceOpenCallback) {
        Device device;
        Client client = getClient(iBinder);
        if (client == null) {
            return;
        }
        synchronized (this.mDevicesByInfo) {
            device = this.mDevicesByInfo.get(midiDeviceInfo);
            if (device == null) {
                throw new IllegalArgumentException("device does not exist: " + midiDeviceInfo);
            }
            if (!device.isUidAllowed(Binder.getCallingUid())) {
                throw new SecurityException("Attempt to open private device with wrong UID");
            }
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            client.addDeviceConnection(device, iMidiDeviceOpenCallback);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void openBluetoothDevice(IBinder iBinder, BluetoothDevice bluetoothDevice, IMidiDeviceOpenCallback iMidiDeviceOpenCallback) {
        Device device;
        Client client = getClient(iBinder);
        if (client == null) {
            return;
        }
        synchronized (this.mDevicesByInfo) {
            device = this.mBluetoothDevices.get(bluetoothDevice);
            if (device == null) {
                device = new Device(bluetoothDevice);
                this.mBluetoothDevices.put(bluetoothDevice, device);
            }
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            client.addDeviceConnection(device, iMidiDeviceOpenCallback);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void closeDevice(IBinder iBinder, IBinder iBinder2) {
        Client client = getClient(iBinder);
        if (client == null) {
            return;
        }
        client.removeDeviceConnection(iBinder2);
    }

    public MidiDeviceInfo registerDeviceServer(IMidiDeviceServer iMidiDeviceServer, int i, int i2, String[] strArr, String[] strArr2, Bundle bundle, int i3) {
        MidiDeviceInfo midiDeviceInfoAddDeviceLocked;
        int callingUid = Binder.getCallingUid();
        if (i3 != 1 || callingUid == 1000) {
            if (i3 == 3 && callingUid != this.mBluetoothServiceUid) {
                throw new SecurityException("only MidiBluetoothService can create Bluetooth devices");
            }
            synchronized (this.mDevicesByInfo) {
                midiDeviceInfoAddDeviceLocked = addDeviceLocked(i3, i, i2, strArr, strArr2, bundle, iMidiDeviceServer, null, false, callingUid);
            }
            return midiDeviceInfoAddDeviceLocked;
        }
        throw new SecurityException("only system can create USB devices");
    }

    public void unregisterDeviceServer(IMidiDeviceServer iMidiDeviceServer) {
        synchronized (this.mDevicesByInfo) {
            Device device = this.mDevicesByServer.get(iMidiDeviceServer.asBinder());
            if (device != null) {
                device.closeLocked();
            }
        }
    }

    public MidiDeviceInfo getServiceDeviceInfo(String str, String str2) {
        synchronized (this.mDevicesByInfo) {
            for (Device device : this.mDevicesByInfo.values()) {
                ServiceInfo serviceInfo = device.getServiceInfo();
                if (serviceInfo != null && str.equals(serviceInfo.packageName) && str2.equals(serviceInfo.name)) {
                    return device.getDeviceInfo();
                }
            }
            return null;
        }
    }

    public MidiDeviceStatus getDeviceStatus(MidiDeviceInfo midiDeviceInfo) {
        Device device = this.mDevicesByInfo.get(midiDeviceInfo);
        if (device == null) {
            throw new IllegalArgumentException("no such device for " + midiDeviceInfo);
        }
        return device.getDeviceStatus();
    }

    public void setDeviceStatus(IMidiDeviceServer iMidiDeviceServer, MidiDeviceStatus midiDeviceStatus) {
        Device device = this.mDevicesByServer.get(iMidiDeviceServer.asBinder());
        if (device != null) {
            if (Binder.getCallingUid() != device.getUid()) {
                throw new SecurityException("setDeviceStatus() caller UID " + Binder.getCallingUid() + " does not match device's UID " + device.getUid());
            }
            device.setDeviceStatus(midiDeviceStatus);
            notifyDeviceStatusChanged(device, midiDeviceStatus);
        }
    }

    private void notifyDeviceStatusChanged(Device device, MidiDeviceStatus midiDeviceStatus) {
        synchronized (this.mClients) {
            Iterator<Client> it = this.mClients.values().iterator();
            while (it.hasNext()) {
                it.next().deviceStatusChanged(device, midiDeviceStatus);
            }
        }
    }

    private MidiDeviceInfo addDeviceLocked(int i, int i2, int i3, String[] strArr, String[] strArr2, Bundle bundle, IMidiDeviceServer iMidiDeviceServer, ServiceInfo serviceInfo, boolean z, int i4) {
        BluetoothDevice bluetoothDevice;
        Device device;
        int i5 = this.mNextDeviceId;
        this.mNextDeviceId = i5 + 1;
        MidiDeviceInfo midiDeviceInfo = new MidiDeviceInfo(i, i5, i2, i3, strArr, strArr2, bundle, z);
        Device device2 = null;
        if (iMidiDeviceServer != null) {
            try {
                iMidiDeviceServer.setDeviceInfo(midiDeviceInfo);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in setDeviceInfo()");
                return null;
            }
        }
        if (i != 3) {
            bluetoothDevice = null;
        } else {
            BluetoothDevice bluetoothDevice2 = (BluetoothDevice) bundle.getParcelable("bluetooth_device");
            Device device3 = this.mBluetoothDevices.get(bluetoothDevice2);
            if (device3 != null) {
                device3.setDeviceInfo(midiDeviceInfo);
            }
            bluetoothDevice = bluetoothDevice2;
            device2 = device3;
        }
        if (device2 == null) {
            device = new Device(iMidiDeviceServer, midiDeviceInfo, serviceInfo, i4);
        } else {
            device = device2;
        }
        this.mDevicesByInfo.put(midiDeviceInfo, device);
        if (bluetoothDevice != null) {
            this.mBluetoothDevices.put(bluetoothDevice, device);
        }
        synchronized (this.mClients) {
            Iterator<Client> it = this.mClients.values().iterator();
            while (it.hasNext()) {
                it.next().deviceAdded(device);
            }
        }
        return midiDeviceInfo;
    }

    private void removeDeviceLocked(Device device) {
        IMidiDeviceServer deviceServer = device.getDeviceServer();
        if (deviceServer != null) {
            this.mDevicesByServer.remove(deviceServer.asBinder());
        }
        this.mDevicesByInfo.remove(device.getDeviceInfo());
        synchronized (this.mClients) {
            Iterator<Client> it = this.mClients.values().iterator();
            while (it.hasNext()) {
                it.next().deviceRemoved(device);
            }
        }
    }

    private void addPackageDeviceServers(String str) throws Throwable {
        try {
            ServiceInfo[] serviceInfoArr = this.mPackageManager.getPackageInfo(str, 132).services;
            if (serviceInfoArr == null) {
                return;
            }
            for (ServiceInfo serviceInfo : serviceInfoArr) {
                addPackageDeviceServer(serviceInfo);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "handlePackageUpdate could not find package " + str, e);
        }
    }

    private void addPackageDeviceServer(ServiceInfo serviceInfo) throws Throwable {
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        XmlResourceParser xmlResourceParser;
        int i;
        ArrayList arrayList;
        ArrayList arrayList2;
        HashMap<MidiDeviceInfo, Device> map;
        MidiService midiService;
        String attributeValue;
        String attributeValue2;
        MidiService midiService2 = this;
        try {
            xmlResourceParserLoadXmlMetaData = serviceInfo.loadXmlMetaData(midiService2.mPackageManager, "android.media.midi.MidiDeviceService");
            if (xmlResourceParserLoadXmlMetaData == null) {
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                    return;
                }
                return;
            }
            try {
                if (!"android.permission.BIND_MIDI_DEVICE_SERVICE".equals(serviceInfo.permission)) {
                    Log.w(TAG, "Skipping MIDI device service " + serviceInfo.packageName + ": it does not require the permission android.permission.BIND_MIDI_DEVICE_SERVICE");
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        xmlResourceParserLoadXmlMetaData.close();
                        return;
                    }
                    return;
                }
                ArrayList arrayList3 = new ArrayList();
                ArrayList arrayList4 = new ArrayList();
                int i2 = 0;
                int i3 = 0;
                int i4 = 0;
                boolean z = false;
                Bundle bundle = null;
                while (true) {
                    int next = xmlResourceParserLoadXmlMetaData.next();
                    if (next == 1) {
                        break;
                    }
                    if (next == 2) {
                        String name = xmlResourceParserLoadXmlMetaData.getName();
                        if ("device".equals(name)) {
                            if (bundle != null) {
                                Log.w(TAG, "nested <device> elements in metadata for " + serviceInfo.packageName);
                                i = i2;
                                arrayList = arrayList4;
                                arrayList2 = arrayList3;
                            } else {
                                Bundle bundle2 = new Bundle();
                                bundle2.putParcelable("service_info", serviceInfo);
                                int attributeCount = xmlResourceParserLoadXmlMetaData.getAttributeCount();
                                int i5 = i2;
                                boolean zEquals = i5;
                                while (i5 < attributeCount) {
                                    String attributeName = xmlResourceParserLoadXmlMetaData.getAttributeName(i5);
                                    String attributeValue3 = xmlResourceParserLoadXmlMetaData.getAttributeValue(i5);
                                    if ("private".equals(attributeName)) {
                                        zEquals = "true".equals(attributeValue3);
                                    } else {
                                        bundle2.putString(attributeName, attributeValue3);
                                    }
                                    i5++;
                                }
                                bundle = bundle2;
                                z = zEquals;
                                i3 = i2;
                                i4 = i3;
                                i = i2;
                                arrayList = arrayList4;
                                arrayList2 = arrayList3;
                            }
                        } else if (!"input-port".equals(name)) {
                            if ("output-port".equals(name)) {
                                if (bundle == null) {
                                    Log.w(TAG, "<output-port> outside of <device> in metadata for " + serviceInfo.packageName);
                                    i = i2;
                                    arrayList = arrayList4;
                                    arrayList2 = arrayList3;
                                } else {
                                    int i6 = i4 + 1;
                                    int attributeCount2 = xmlResourceParserLoadXmlMetaData.getAttributeCount();
                                    int i7 = i2;
                                    while (true) {
                                        if (i7 >= attributeCount2) {
                                            attributeValue = null;
                                            break;
                                        }
                                        String attributeName2 = xmlResourceParserLoadXmlMetaData.getAttributeName(i7);
                                        attributeValue = xmlResourceParserLoadXmlMetaData.getAttributeValue(i7);
                                        if (Settings.ATTR_NAME.equals(attributeName2)) {
                                            break;
                                        } else {
                                            i7++;
                                        }
                                    }
                                    arrayList4.add(attributeValue);
                                    i4 = i6;
                                }
                            }
                            i = i2;
                            arrayList = arrayList4;
                            arrayList2 = arrayList3;
                        } else if (bundle == null) {
                            Log.w(TAG, "<input-port> outside of <device> in metadata for " + serviceInfo.packageName);
                            i = i2;
                            arrayList = arrayList4;
                            arrayList2 = arrayList3;
                        } else {
                            int i8 = i3 + 1;
                            int attributeCount3 = xmlResourceParserLoadXmlMetaData.getAttributeCount();
                            int i9 = i2;
                            while (true) {
                                if (i9 >= attributeCount3) {
                                    attributeValue2 = null;
                                    break;
                                }
                                String attributeName3 = xmlResourceParserLoadXmlMetaData.getAttributeName(i9);
                                attributeValue2 = xmlResourceParserLoadXmlMetaData.getAttributeValue(i9);
                                if (Settings.ATTR_NAME.equals(attributeName3)) {
                                    break;
                                } else {
                                    i9++;
                                }
                            }
                            arrayList3.add(attributeValue2);
                            i3 = i8;
                            i = i2;
                            arrayList = arrayList4;
                            arrayList2 = arrayList3;
                        }
                    } else if (next != 3 || !"device".equals(xmlResourceParserLoadXmlMetaData.getName()) || bundle == null) {
                        i = i2;
                        arrayList = arrayList4;
                        arrayList2 = arrayList3;
                    } else if (i3 == 0 && i4 == 0) {
                        Log.w(TAG, "<device> with no ports in metadata for " + serviceInfo.packageName);
                        i = i2;
                        arrayList = arrayList4;
                        arrayList2 = arrayList3;
                    } else {
                        try {
                            int i10 = midiService2.mPackageManager.getApplicationInfo(serviceInfo.packageName, i2).uid;
                            HashMap<MidiDeviceInfo, Device> map2 = midiService2.mDevicesByInfo;
                            synchronized (map2) {
                                try {
                                    midiService = midiService2;
                                    map = map2;
                                    i = i2;
                                    arrayList = arrayList4;
                                    arrayList2 = arrayList3;
                                } catch (Throwable th) {
                                    th = th;
                                    map = map2;
                                    throw th;
                                }
                                try {
                                    midiService.addDeviceLocked(2, i3, i4, (String[]) arrayList3.toArray(EMPTY_STRING_ARRAY), (String[]) arrayList4.toArray(EMPTY_STRING_ARRAY), bundle, null, serviceInfo, z, i10);
                                    arrayList2.clear();
                                    arrayList.clear();
                                    bundle = null;
                                } catch (Throwable th2) {
                                    th = th2;
                                    throw th;
                                }
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            i = i2;
                            arrayList = arrayList4;
                            arrayList2 = arrayList3;
                            Log.e(TAG, "could not fetch ApplicationInfo for " + serviceInfo.packageName);
                        }
                    }
                    arrayList3 = arrayList2;
                    arrayList4 = arrayList;
                    i2 = i;
                    midiService2 = this;
                }
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
            } catch (Exception e2) {
                e = e2;
                xmlResourceParser = xmlResourceParserLoadXmlMetaData;
                try {
                    Log.w(TAG, "Unable to load component info " + serviceInfo.toString(), e);
                    if (xmlResourceParser != null) {
                        xmlResourceParser.close();
                    }
                } catch (Throwable th3) {
                    th = th3;
                    xmlResourceParserLoadXmlMetaData = xmlResourceParser;
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        xmlResourceParserLoadXmlMetaData.close();
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                if (xmlResourceParserLoadXmlMetaData != null) {
                }
                throw th;
            }
        } catch (Exception e3) {
            e = e3;
            xmlResourceParser = null;
        } catch (Throwable th5) {
            th = th5;
            xmlResourceParserLoadXmlMetaData = null;
        }
    }

    private void removePackageDeviceServers(String str) {
        synchronized (this.mDevicesByInfo) {
            Iterator<Device> it = this.mDevicesByInfo.values().iterator();
            while (it.hasNext()) {
                Device next = it.next();
                if (str.equals(next.getPackageName())) {
                    it.remove();
                    removeDeviceLocked(next);
                }
            }
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            indentingPrintWriter.println("MIDI Manager State:");
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.println("Devices:");
            indentingPrintWriter.increaseIndent();
            synchronized (this.mDevicesByInfo) {
                Iterator<Device> it = this.mDevicesByInfo.values().iterator();
                while (it.hasNext()) {
                    indentingPrintWriter.println(it.next().toString());
                }
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println("Clients:");
            indentingPrintWriter.increaseIndent();
            synchronized (this.mClients) {
                Iterator<Client> it2 = this.mClients.values().iterator();
                while (it2.hasNext()) {
                    indentingPrintWriter.println(it2.next().toString());
                }
            }
            indentingPrintWriter.decreaseIndent();
        }
    }
}
