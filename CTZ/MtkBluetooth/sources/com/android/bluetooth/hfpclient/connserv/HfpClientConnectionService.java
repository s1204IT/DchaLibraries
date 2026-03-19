package com.android.bluetooth.hfpclient.connserv;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;
import com.android.bluetooth.hfpclient.HeadsetClientService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HfpClientConnectionService extends ConnectionService {
    private static final boolean DBG = true;
    public static final String HFP_SCHEME = "hfpc";
    private static final String TAG = "HfpClientConnService";
    private BluetoothAdapter mAdapter;
    private BluetoothHeadsetClient mHeadsetProfile;
    private TelecomManager mTelecomManager;
    private final Map<BluetoothDevice, HfpClientDeviceBlock> mDeviceBlocks = new HashMap();
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(HfpClientConnectionService.TAG, "onReceive " + intent);
            String action = intent != null ? intent.getAction() : null;
            if (!"android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
                if ("android.bluetooth.headsetclient.profile.action.AG_CALL_CHANGED".equals(action)) {
                    BluetoothHeadsetClientCall parcelableExtra = intent.getParcelableExtra("android.bluetooth.headsetclient.extra.CALL");
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    HfpClientDeviceBlock hfpClientDeviceBlockFindBlockForDevice = HfpClientConnectionService.this.findBlockForDevice(parcelableExtra.getDevice());
                    if (hfpClientDeviceBlockFindBlockForDevice == null) {
                        Log.w(HfpClientConnectionService.TAG, "Call changed but no block for device " + bluetoothDevice);
                        return;
                    }
                    hfpClientDeviceBlockFindBlockForDevice.handleCall(parcelableExtra);
                    return;
                }
                return;
            }
            BluetoothDevice bluetoothDevice2 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            int intExtra = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
            if (intExtra == 2) {
                Log.d(HfpClientConnectionService.TAG, "Established connection with " + bluetoothDevice2);
                if (HfpClientConnectionService.this.createBlockForDevice(bluetoothDevice2) == null) {
                    Log.w(HfpClientConnectionService.TAG, "Block already exists for device " + bluetoothDevice2 + " ignoring.");
                    return;
                }
                return;
            }
            if (intExtra == 0) {
                Log.d(HfpClientConnectionService.TAG, "Disconnecting from " + bluetoothDevice2);
                synchronized (HfpClientConnectionService.this) {
                    HfpClientDeviceBlock hfpClientDeviceBlock = (HfpClientDeviceBlock) HfpClientConnectionService.this.mDeviceBlocks.remove(bluetoothDevice2);
                    if (hfpClientDeviceBlock == null) {
                        Log.w(HfpClientConnectionService.TAG, "Disconnect for device but no block " + bluetoothDevice2);
                        return;
                    }
                    hfpClientDeviceBlock.cleanup();
                }
            }
        }
    };
    BluetoothProfile.ServiceListener mServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            Log.d(HfpClientConnectionService.TAG, "onServiceConnected");
            HfpClientConnectionService.this.mHeadsetProfile = (BluetoothHeadsetClient) bluetoothProfile;
            List<BluetoothDevice> connectedDevices = HfpClientConnectionService.this.mHeadsetProfile.getConnectedDevices();
            if (connectedDevices == null) {
                Log.w(HfpClientConnectionService.TAG, "No connected or more than one connected devices found." + connectedDevices);
                return;
            }
            for (BluetoothDevice bluetoothDevice : connectedDevices) {
                Log.d(HfpClientConnectionService.TAG, "Creating phone account for device " + bluetoothDevice);
                HfpClientConnectionService.this.createBlockForDevice(bluetoothDevice);
            }
        }

        @Override
        public void onServiceDisconnected(int i) {
            Log.d(HfpClientConnectionService.TAG, "onServiceDisconnected " + i);
            HfpClientConnectionService.this.mHeadsetProfile = null;
            HfpClientConnectionService.this.disconnectAll();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mTelecomManager = (TelecomManager) getSystemService("telecom");
        if (this.mTelecomManager != null) {
            this.mTelecomManager.clearPhoneAccounts();
        }
        this.mAdapter.getProfileProxy(this, this.mServiceListener, 16);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        if (this.mHeadsetProfile != null) {
            this.mAdapter.closeProfileProxy(16, this.mHeadsetProfile);
        }
        try {
            unregisterReceiver(this.mBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver was not registered.");
        }
        disconnectAll();
    }

    private synchronized void disconnectAll() {
        Iterator<Map.Entry<BluetoothDevice, HfpClientDeviceBlock>> it = this.mDeviceBlocks.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().cleanup();
            it.remove();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        Log.d(TAG, "onStartCommand " + intent);
        if (intent != null && intent.getBooleanExtra(HeadsetClientService.HFP_CLIENT_STOP_TAG, false)) {
            stopSelf();
            return 0;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headsetclient.profile.action.AG_CALL_CHANGED");
        registerReceiver(this.mBroadcastReceiver, intentFilter);
        return 1;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle phoneAccountHandle, ConnectionRequest connectionRequest) {
        Log.d(TAG, "onCreateIncomingConnection " + phoneAccountHandle + " req: " + connectionRequest);
        HfpClientDeviceBlock hfpClientDeviceBlockFindBlockForHandle = findBlockForHandle(phoneAccountHandle);
        if (hfpClientDeviceBlockFindBlockForHandle == null) {
            Log.w(TAG, "HfpClient does not support having a connection manager");
            return null;
        }
        return hfpClientDeviceBlockFindBlockForHandle.onCreateIncomingConnection((BluetoothHeadsetClientCall) connectionRequest.getExtras().getParcelable("android.telecom.extra.INCOMING_CALL_EXTRAS"));
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle phoneAccountHandle, ConnectionRequest connectionRequest) {
        Log.d(TAG, "onCreateOutgoingConnection " + phoneAccountHandle);
        HfpClientDeviceBlock hfpClientDeviceBlockFindBlockForHandle = findBlockForHandle(phoneAccountHandle);
        if (hfpClientDeviceBlockFindBlockForHandle == null) {
            Log.w(TAG, "HfpClient does not support having a connection manager");
            return null;
        }
        return hfpClientDeviceBlockFindBlockForHandle.onCreateOutgoingConnection(connectionRequest.getAddress());
    }

    public Connection onCreateUnknownConnection(PhoneAccountHandle phoneAccountHandle, ConnectionRequest connectionRequest) {
        Log.d(TAG, "onCreateUnknownConnection " + phoneAccountHandle);
        HfpClientDeviceBlock hfpClientDeviceBlockFindBlockForHandle = findBlockForHandle(phoneAccountHandle);
        if (hfpClientDeviceBlockFindBlockForHandle == null) {
            Log.w(TAG, "HfpClient does not support having a connection manager");
            return null;
        }
        return hfpClientDeviceBlockFindBlockForHandle.onCreateUnknownConnection((BluetoothHeadsetClientCall) connectionRequest.getExtras().getParcelable("android.telecom.extra.OUTGOING_CALL_EXTRAS"));
    }

    @Override
    public void onConference(Connection connection, Connection connection2) {
        Log.d(TAG, "onConference " + connection + " " + connection2);
        BluetoothDevice device = ((HfpClientConnection) connection).getDevice();
        BluetoothDevice device2 = ((HfpClientConnection) connection2).getDevice();
        if (!Objects.equals(device, device2)) {
            Log.e(TAG, "Cannot conference calls from two different devices bd1 " + device + " bd2 " + device2 + " conn1 " + connection + "connection2 " + connection2);
            return;
        }
        findBlockForDevice(device).onConference(connection, connection2);
    }

    private BluetoothDevice getDevice(PhoneAccountHandle phoneAccountHandle) {
        return this.mAdapter.getRemoteDevice(this.mTelecomManager.getPhoneAccount(phoneAccountHandle).getAddress().getSchemeSpecificPart());
    }

    synchronized HfpClientDeviceBlock createBlockForDevice(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "Creating block for device " + bluetoothDevice);
        if (this.mDeviceBlocks.containsKey(bluetoothDevice)) {
            Log.e(TAG, "Device already exists " + bluetoothDevice + " blocks " + this.mDeviceBlocks);
            return null;
        }
        HfpClientDeviceBlock hfpClientDeviceBlock = new HfpClientDeviceBlock(this, bluetoothDevice, this.mHeadsetProfile);
        this.mDeviceBlocks.put(bluetoothDevice, hfpClientDeviceBlock);
        return hfpClientDeviceBlock;
    }

    synchronized HfpClientDeviceBlock findBlockForDevice(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "Finding block for device " + bluetoothDevice + " blocks " + this.mDeviceBlocks);
        return this.mDeviceBlocks.get(bluetoothDevice);
    }

    synchronized HfpClientDeviceBlock findBlockForHandle(PhoneAccountHandle phoneAccountHandle) {
        BluetoothDevice remoteDevice;
        String schemeSpecificPart = this.mTelecomManager.getPhoneAccount(phoneAccountHandle).getAddress().getSchemeSpecificPart();
        remoteDevice = this.mAdapter.getRemoteDevice(schemeSpecificPart);
        Log.d(TAG, "Finding block for handle " + phoneAccountHandle + " device " + schemeSpecificPart);
        return this.mDeviceBlocks.get(remoteDevice);
    }

    public static PhoneAccount createAccount(Context context, BluetoothDevice bluetoothDevice) {
        PhoneAccount phoneAccountBuild = new PhoneAccount.Builder(new PhoneAccountHandle(new ComponentName(context, (Class<?>) HfpClientConnectionService.class), bluetoothDevice.getAddress()), "HFP " + bluetoothDevice.toString()).setAddress(Uri.fromParts(HFP_SCHEME, bluetoothDevice.getAddress(), null)).setSupportedUriSchemes(Arrays.asList("tel")).setCapabilities(2).build();
        Log.d(TAG, "phoneaccount: " + phoneAccountBuild);
        return phoneAccountBuild;
    }

    public static boolean hasHfpClientEcc(BluetoothHeadsetClient bluetoothHeadsetClient, BluetoothDevice bluetoothDevice) {
        Bundle currentAgEvents = bluetoothHeadsetClient.getCurrentAgEvents(bluetoothDevice);
        return currentAgEvents != null && currentAgEvents.getBoolean("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_ECC", false);
    }
}
