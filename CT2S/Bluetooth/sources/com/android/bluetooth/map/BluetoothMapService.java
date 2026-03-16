package com.android.bluetooth.map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothMap;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class BluetoothMapService extends ProfileService {
    private static final String ACCESS_AUTHORITY_CLASS = "com.android.settings.bluetooth.BluetoothPermissionRequest";
    private static final String ACCESS_AUTHORITY_PACKAGE = "com.android.settings";
    public static final String ACTION_SHOW_MAPS_EMAIL_SETTINGS = "android.btmap.intent.action.SHOW_MAPS_EMAIL_SETTINGS";
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    private static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    public static final boolean DEBUG = true;
    private static final int DISCONNECT_MAP = 3;
    private static final int MAS_ID_SMS_MMS = 0;
    public static final int MSG_ACQUIRE_WAKE_LOCK = 5005;
    public static final int MSG_MAS_CONNECT = 5003;
    public static final int MSG_MAS_CONNECT_CANCEL = 5004;
    public static final int MSG_RELEASE_WAKE_LOCK = 5006;
    public static final int MSG_SERVERSESSION_CLOSE = 5000;
    public static final int MSG_SESSION_DISCONNECTED = 5002;
    public static final int MSG_SESSION_ESTABLISHED = 5001;
    private static final int RELEASE_WAKE_LOCK_DELAY = 10000;
    private static final int SHUTDOWN = 4;
    private static final int START_LISTENER = 1;
    private static final String TAG = "BluetoothMapService";
    private static final int UPDATE_MAS_INSTANCES = 5;
    public static final int UPDATE_MAS_INSTANCES_ACCOUNT_ADDED = 0;
    public static final int UPDATE_MAS_INSTANCES_ACCOUNT_DISCONNECT = 3;
    public static final int UPDATE_MAS_INSTANCES_ACCOUNT_REMOVED = 1;
    public static final int UPDATE_MAS_INSTANCES_ACCOUNT_RENAMED = 2;
    public static final String USER_CONFIRM_TIMEOUT_ACTION = "com.android.bluetooth.map.USER_CONFIRM_TIMEOUT";
    private static final int USER_CONFIRM_TIMEOUT_VALUE = 25000;
    private static final int USER_TIMEOUT = 2;
    public static final boolean VERBOSE = false;
    private BluetoothAdapter mAdapter;
    private static String sRemoteDeviceName = null;
    private static final ParcelUuid[] MAP_UUIDS = {BluetoothUuid.MAP, BluetoothUuid.MNS};
    private PowerManager.WakeLock mWakeLock = null;
    private BluetoothMnsObexClient mBluetoothMnsObexClient = null;
    private SparseArray<BluetoothMapMasInstance> mMasInstances = new SparseArray<>(1);
    private HashMap<BluetoothMapEmailSettingsItem, BluetoothMapMasInstance> mMasInstanceMap = new HashMap<>(1);
    private BluetoothDevice mRemoteDevice = null;
    private ArrayList<BluetoothMapEmailSettingsItem> mEnabledAccounts = null;
    private BluetoothMapEmailAppObserver mAppObserver = null;
    private AlarmManager mAlarmManager = null;
    private boolean mIsWaitingAuthorization = false;
    private boolean mRemoveTimeoutMsg = false;
    private int mPermission = 0;
    private boolean mAccountChanged = false;
    private final Handler mSessionStatusHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (BluetoothMapService.this.mAdapter.isEnabled()) {
                        BluetoothMapService.this.startRfcommSocketListeners();
                    }
                    break;
                case 2:
                    if (BluetoothMapService.this.mIsWaitingAuthorization) {
                        Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL");
                        intent.setClassName(BluetoothMapService.ACCESS_AUTHORITY_PACKAGE, BluetoothMapService.ACCESS_AUTHORITY_CLASS);
                        intent.putExtra("android.bluetooth.device.extra.DEVICE", BluetoothMapService.this.mRemoteDevice);
                        intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 3);
                        BluetoothMapService.this.sendBroadcast(intent);
                        BluetoothMapService.this.cancelUserTimeoutAlarm();
                        BluetoothMapService.this.mIsWaitingAuthorization = false;
                        BluetoothMapService.this.stopObexServerSessions(-1);
                    }
                    break;
                case 3:
                    BluetoothMapService.this.disconnectMap((BluetoothDevice) msg.obj);
                    break;
                case 4:
                    BluetoothMapService.this.closeService();
                    break;
                case 5:
                    BluetoothMapService.this.updateMasInstancesHandler();
                    break;
                case 5000:
                    BluetoothMapService.this.stopObexServerSessions(msg.arg1);
                    break;
                case 5003:
                    BluetoothMapService.this.onConnectHandler(msg.arg1);
                    break;
                case 5004:
                    BluetoothMapService.this.stopObexServerSessions(-1);
                    break;
                case 5005:
                    if (BluetoothMapService.this.mWakeLock == null) {
                        PowerManager pm = (PowerManager) BluetoothMapService.this.getSystemService("power");
                        BluetoothMapService.this.mWakeLock = pm.newWakeLock(1, "StartingObexMapTransaction");
                        BluetoothMapService.this.mWakeLock.setReferenceCounted(false);
                    }
                    if (!BluetoothMapService.this.mWakeLock.isHeld()) {
                        BluetoothMapService.this.mWakeLock.acquire();
                        Log.i(BluetoothMapService.TAG, "  Acquired Wake Lock by message");
                    }
                    BluetoothMapService.this.mSessionStatusHandler.removeMessages(5006);
                    BluetoothMapService.this.mSessionStatusHandler.sendMessageDelayed(BluetoothMapService.this.mSessionStatusHandler.obtainMessage(5006), 10000L);
                    break;
                case 5006:
                    if (BluetoothMapService.this.mWakeLock != null) {
                        BluetoothMapService.this.mWakeLock.release();
                        Log.i(BluetoothMapService.TAG, "  Released Wake Lock by message");
                    }
                    break;
            }
        }
    };
    private MapBroadcastReceiver mMapReceiver = new MapBroadcastReceiver();
    private int mState = 0;

    private final void closeService() {
        Log.d(TAG, "MAP Service closeService in");
        if (this.mBluetoothMnsObexClient != null) {
            this.mBluetoothMnsObexClient.shutdown();
            this.mBluetoothMnsObexClient = null;
        }
        int c = this.mMasInstances.size();
        for (int i = 0; i < c; i++) {
            this.mMasInstances.valueAt(i).shutdown();
        }
        this.mMasInstances.clear();
        if (this.mSessionStatusHandler != null) {
            this.mSessionStatusHandler.removeCallbacksAndMessages(null);
        }
        this.mIsWaitingAuthorization = false;
        this.mPermission = 0;
        setState(0);
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
        this.mRemoteDevice = null;
    }

    private final void startRfcommSocketListeners() {
        int c = this.mMasInstances.size();
        for (int i = 0; i < c; i++) {
            this.mMasInstances.valueAt(i).startRfcommSocketListener();
        }
    }

    private final void startObexServerSessions() {
        Log.d(TAG, "Map Service START ObexServerSessions()");
        if (this.mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService("power");
            this.mWakeLock = pm.newWakeLock(1, "StartingObexMapTransaction");
            this.mWakeLock.setReferenceCounted(false);
            this.mWakeLock.acquire();
        }
        if (this.mBluetoothMnsObexClient == null) {
            this.mBluetoothMnsObexClient = new BluetoothMnsObexClient(this.mRemoteDevice, this.mSessionStatusHandler);
        }
        boolean connected = false;
        int c = this.mMasInstances.size();
        for (int i = 0; i < c; i++) {
            try {
                if (this.mMasInstances.valueAt(i).startObexServerSession(this.mBluetoothMnsObexClient)) {
                    connected = true;
                }
            } catch (RemoteException e) {
                Log.w(TAG, "RemoteException occured while starting an obexServerSession restarting the listener", e);
                this.mMasInstances.valueAt(i).restartObexServerSession();
            } catch (IOException e2) {
                Log.w(TAG, "IOException occured while starting an obexServerSession restarting the listener", e2);
                this.mMasInstances.valueAt(i).restartObexServerSession();
            }
        }
        if (connected) {
            setState(2);
        }
        this.mSessionStatusHandler.removeMessages(5006);
        this.mSessionStatusHandler.sendMessageDelayed(this.mSessionStatusHandler.obtainMessage(5006), 10000L);
    }

    public Handler getHandler() {
        return this.mSessionStatusHandler;
    }

    private void stopObexServerSessions(int masId) {
        Log.d(TAG, "MAP Service STOP ObexServerSessions()");
        boolean lastMasInst = true;
        if (masId != -1) {
            int c = this.mMasInstances.size();
            for (int i = 0; i < c; i++) {
                BluetoothMapMasInstance masInst = this.mMasInstances.valueAt(i);
                if (masInst.getMasId() != masId && masInst.isStarted()) {
                    lastMasInst = false;
                }
            }
        }
        if (this.mBluetoothMnsObexClient != null && lastMasInst) {
            this.mBluetoothMnsObexClient.shutdown();
            this.mBluetoothMnsObexClient = null;
        }
        BluetoothMapMasInstance masInst2 = this.mMasInstances.get(masId);
        if (masInst2 != null) {
            masInst2.restartObexServerSession();
        } else {
            int c2 = this.mMasInstances.size();
            for (int i2 = 0; i2 < c2; i2++) {
                this.mMasInstances.valueAt(i2).restartObexServerSession();
            }
        }
        if (lastMasInst) {
            setState(0);
            this.mPermission = 0;
            this.mRemoteDevice = null;
            if (this.mAccountChanged) {
                updateMasInstances(3);
            }
        }
        if (this.mWakeLock != null && lastMasInst) {
            this.mSessionStatusHandler.removeMessages(5005);
            this.mSessionStatusHandler.removeMessages(5006);
            this.mWakeLock.release();
        }
    }

    private void onConnectHandler(int masId) {
        if (!this.mIsWaitingAuthorization && this.mRemoteDevice != null) {
            BluetoothMapMasInstance masInst = this.mMasInstances.get(masId);
            Log.d(TAG, "mPermission = " + this.mPermission);
            if (this.mPermission == 1) {
                try {
                    Log.d(TAG, "incoming connection accepted from: " + sRemoteDeviceName + " automatically as trusted device");
                    if (this.mBluetoothMnsObexClient != null && masInst != null) {
                        masInst.startObexServerSession(this.mBluetoothMnsObexClient);
                    } else {
                        startObexServerSessions();
                    }
                } catch (RemoteException ex) {
                    Log.e(TAG, "catch RemoteException starting obex server session", ex);
                } catch (IOException ex2) {
                    Log.e(TAG, "catch IOException starting obex server session", ex2);
                }
            }
        }
    }

    public int getState() {
        return this.mState;
    }

    public BluetoothDevice getRemoteDevice() {
        return this.mRemoteDevice;
    }

    private void setState(int state) {
        setState(state, 1);
    }

    private synchronized void setState(int state, int result) {
        if (state != this.mState) {
            Log.d(TAG, "Map state " + this.mState + " -> " + state + ", result = " + result);
            int prevState = this.mState;
            this.mState = state;
            Intent intent = new Intent("android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED");
            intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", prevState);
            intent.putExtra("android.bluetooth.profile.extra.STATE", this.mState);
            intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mRemoteDevice);
            sendBroadcast(intent, "android.permission.BLUETOOTH");
            AdapterService s = AdapterService.getAdapterService();
            if (s != null) {
                s.onProfileConnectionStateChanged(this.mRemoteDevice, 9, this.mState, prevState);
            }
        }
    }

    public static String getRemoteDeviceName() {
        return sRemoteDeviceName;
    }

    public boolean disconnect(BluetoothDevice device) {
        this.mSessionStatusHandler.sendMessage(this.mSessionStatusHandler.obtainMessage(3, 0, 0, device));
        return true;
    }

    public boolean disconnectMap(BluetoothDevice device) {
        Log.d(TAG, "disconnectMap");
        if (!getRemoteDevice().equals(device)) {
            return false;
        }
        switch (this.mState) {
            case 2:
                sendShutdownMessage();
                stopObexServerSessions(-1);
                break;
        }
        return false;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        synchronized (this) {
            if (this.mState == 2 && this.mRemoteDevice != null) {
                devices.add(this.mRemoteDevice);
            }
        }
        return devices;
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        List<BluetoothDevice> deviceList = new ArrayList<>();
        Set<BluetoothDevice> bondedDevices = this.mAdapter.getBondedDevices();
        synchronized (this) {
            for (BluetoothDevice device : bondedDevices) {
                ParcelUuid[] featureUuids = device.getUuids();
                if (BluetoothUuid.containsAnyUuid(featureUuids, MAP_UUIDS)) {
                    int connectionState = getConnectionState(device);
                    for (int i : states) {
                        if (connectionState == i) {
                            deviceList.add(device);
                        }
                    }
                }
            }
        }
        return deviceList;
    }

    public int getConnectionState(BluetoothDevice device) {
        int i = 2;
        synchronized (this) {
            if (getState() != 2 || !getRemoteDevice().equals(device)) {
                i = 0;
            }
        }
        return i;
    }

    public boolean setPriority(BluetoothDevice device, int priority) {
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothMapPriorityKey(device.getAddress()), priority);
        Log.d(TAG, "Saved priority " + device + " = " + priority);
        return true;
    }

    public int getPriority(BluetoothDevice device) {
        int priority = Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothMapPriorityKey(device.getAddress()), -1);
        return priority;
    }

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothMapBinder(this);
    }

    @Override
    protected boolean start() {
        Log.d(TAG, "start()");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        filter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        filter.addAction(ACTION_SHOW_MAPS_EMAIL_SETTINGS);
        filter.addAction(USER_CONFIRM_TIMEOUT_ACTION);
        IntentFilter filterMessageSent = new IntentFilter();
        filterMessageSent.addAction(BluetoothMapContentObserver.ACTION_MESSAGE_SENT);
        try {
            filterMessageSent.addDataType("message/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.e(TAG, "Wrong mime type!!!", e);
        }
        try {
            registerReceiver(this.mMapReceiver, filter);
            registerReceiver(this.mMapReceiver, filterMessageSent, "android.permission.WRITE_SMS", null);
        } catch (Exception e2) {
            Log.w(TAG, "Unable to register map receiver", e2);
        }
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mAppObserver = new BluetoothMapEmailAppObserver(this, this);
        this.mEnabledAccounts = this.mAppObserver.getEnabledAccountItems();
        createMasInstances();
        this.mSessionStatusHandler.sendMessage(this.mSessionStatusHandler.obtainMessage(1));
        return true;
    }

    public void updateMasInstances(int action) {
        this.mSessionStatusHandler.obtainMessage(5, action, 0).sendToTarget();
    }

    private boolean updateMasInstancesHandler() {
        Log.d(TAG, "updateMasInstancesHandler() state = " + getState());
        boolean changed = false;
        if (getState() == 0) {
            ArrayList<BluetoothMapEmailSettingsItem> newAccountList = this.mAppObserver.getEnabledAccountItems();
            ArrayList<BluetoothMapEmailSettingsItem> newAccounts = new ArrayList<>();
            ArrayList<BluetoothMapEmailSettingsItem> removedAccounts = this.mEnabledAccounts;
            for (BluetoothMapEmailSettingsItem account : newAccountList) {
                if (!removedAccounts.remove(account)) {
                    newAccounts.add(account);
                }
            }
            if (removedAccounts != null) {
                for (BluetoothMapEmailSettingsItem account2 : removedAccounts) {
                    BluetoothMapMasInstance masInst = this.mMasInstanceMap.remove(account2);
                    Log.d(TAG, "  Removing account: " + account2 + " masInst = " + masInst);
                    if (masInst != null) {
                        masInst.shutdown();
                        this.mMasInstances.remove(masInst.getMasId());
                        changed = true;
                    }
                }
            }
            if (newAccounts != null) {
                for (BluetoothMapEmailSettingsItem account3 : newAccounts) {
                    Log.d(TAG, "  Adding account: " + account3);
                    int masId = getNextMasId();
                    BluetoothMapMasInstance newInst = new BluetoothMapMasInstance(this, this, account3, masId, false);
                    this.mMasInstances.append(masId, newInst);
                    this.mMasInstanceMap.put(account3, newInst);
                    changed = true;
                    if (this.mAdapter.isEnabled()) {
                        newInst.startRfcommSocketListener();
                    }
                }
            }
            this.mEnabledAccounts = newAccountList;
            this.mAccountChanged = false;
        } else {
            this.mAccountChanged = true;
        }
        return changed;
    }

    private int getNextMasId() {
        int largestMasId = 0;
        int c = this.mMasInstances.size();
        for (int i = 0; i < c; i++) {
            int masId = this.mMasInstances.keyAt(i);
            if (masId > largestMasId) {
                largestMasId = masId;
            }
        }
        if (largestMasId < 255) {
            return largestMasId + 1;
        }
        for (int i2 = 1; i2 <= 255; i2++) {
            if (this.mMasInstances.get(i2) == null) {
                return i2;
            }
        }
        return 255;
    }

    private void createMasInstances() {
        int masId = 0;
        BluetoothMapMasInstance smsMmsInst = new BluetoothMapMasInstance(this, this, null, 0, true);
        this.mMasInstances.append(0, smsMmsInst);
        this.mMasInstanceMap.put(null, smsMmsInst);
        for (BluetoothMapEmailSettingsItem account : this.mEnabledAccounts) {
            masId++;
            BluetoothMapMasInstance newInst = new BluetoothMapMasInstance(this, this, account, masId, false);
            this.mMasInstances.append(masId, newInst);
            this.mMasInstanceMap.put(account, newInst);
        }
    }

    @Override
    protected boolean stop() {
        Log.d(TAG, "stop()");
        try {
            unregisterReceiver(this.mMapReceiver);
            this.mAppObserver.shutdown();
        } catch (Exception e) {
            Log.w(TAG, "Unable to unregister map receiver", e);
        }
        setState(0, 2);
        sendShutdownMessage();
        return true;
    }

    @Override
    public boolean cleanup() {
        Log.d(TAG, "cleanup()");
        setState(0, 2);
        closeService();
        return true;
    }

    public boolean onConnect(BluetoothDevice remoteDevice, BluetoothMapMasInstance masInst) {
        boolean z = true;
        boolean sendIntent = false;
        boolean cancelConnection = false;
        synchronized (this) {
            if (this.mRemoteDevice == null) {
                this.mRemoteDevice = remoteDevice;
                sRemoteDeviceName = this.mRemoteDevice.getName();
                if (TextUtils.isEmpty(sRemoteDeviceName)) {
                    sRemoteDeviceName = getString(R.string.defaultname);
                }
                this.mPermission = this.mRemoteDevice.getMessageAccessPermission();
                if (this.mPermission == 0) {
                    sendIntent = true;
                    this.mIsWaitingAuthorization = true;
                    setUserTimeoutAlarm();
                } else if (this.mPermission == 2) {
                    cancelConnection = true;
                }
            } else if (!this.mRemoteDevice.equals(remoteDevice)) {
                Log.w(TAG, "Unexpected connection from a second Remote Device received. name: " + (remoteDevice == null ? "unknown" : remoteDevice.getName()));
                z = false;
            }
            if (sendIntent) {
                Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST");
                intent.setClassName(ACCESS_AUTHORITY_PACKAGE, ACCESS_AUTHORITY_CLASS);
                intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 3);
                intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mRemoteDevice);
                sendOrderedBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
                Log.d(TAG, "waiting for authorization for connection from: " + sRemoteDeviceName);
            } else if (cancelConnection) {
                sendConnectCancelMessage();
            } else if (this.mPermission == 1) {
                sendConnectMessage(masInst.getMasId());
            }
        }
        return z;
    }

    private void setUserTimeoutAlarm() {
        Log.d(TAG, "SetUserTimeOutAlarm()");
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) getSystemService("alarm");
        }
        this.mRemoveTimeoutMsg = true;
        Intent timeoutIntent = new Intent(USER_CONFIRM_TIMEOUT_ACTION);
        PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, timeoutIntent, 0);
        this.mAlarmManager.set(0, System.currentTimeMillis() + 25000, pIntent);
    }

    private void cancelUserTimeoutAlarm() {
        Log.d(TAG, "cancelUserTimeOutAlarm()");
        Intent intent = new Intent(this, (Class<?>) BluetoothMapService.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService("alarm");
        alarmManager.cancel(sender);
        this.mRemoveTimeoutMsg = false;
    }

    private void sendConnectMessage(int masId) {
        if (this.mSessionStatusHandler != null) {
            Message msg = this.mSessionStatusHandler.obtainMessage(5003, masId, 0);
            msg.sendToTarget();
        }
    }

    private void sendConnectTimeoutMessage() {
        Log.d(TAG, "sendConnectTimeoutMessage()");
        if (this.mSessionStatusHandler != null) {
            Message msg = this.mSessionStatusHandler.obtainMessage(2);
            msg.sendToTarget();
        }
    }

    private void sendConnectCancelMessage() {
        if (this.mSessionStatusHandler != null) {
            Message msg = this.mSessionStatusHandler.obtainMessage(5004);
            msg.sendToTarget();
        }
    }

    private void sendShutdownMessage() {
        if (this.mRemoveTimeoutMsg) {
            Intent timeoutIntent = new Intent(USER_CONFIRM_TIMEOUT_ACTION);
            sendBroadcast(timeoutIntent, "android.permission.BLUETOOTH");
            this.mIsWaitingAuthorization = false;
            cancelUserTimeoutAlarm();
        }
        this.mSessionStatusHandler.removeCallbacksAndMessages(null);
        this.mSessionStatusHandler.obtainMessage(4).sendToTarget();
    }

    private class MapBroadcastReceiver extends BroadcastReceiver {
        private MapBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothMapMasInstance masInst;
            Log.d(BluetoothMapService.TAG, "onReceive");
            String action = intent.getAction();
            if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
                if (state == 13) {
                    Log.d(BluetoothMapService.TAG, "STATE_TURNING_OFF");
                    BluetoothMapService.this.sendShutdownMessage();
                    return;
                } else {
                    if (state == 12) {
                        Log.d(BluetoothMapService.TAG, "STATE_ON");
                        BluetoothMapService.this.mSessionStatusHandler.sendMessage(BluetoothMapService.this.mSessionStatusHandler.obtainMessage(1));
                        return;
                    }
                    return;
                }
            }
            if (action.equals(BluetoothMapService.USER_CONFIRM_TIMEOUT_ACTION)) {
                Log.d(BluetoothMapService.TAG, "USER_CONFIRM_TIMEOUT ACTION Received.");
                BluetoothMapService.this.sendConnectTimeoutMessage();
                return;
            }
            if (action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY")) {
                int requestType = intent.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                Log.d(BluetoothMapService.TAG, "Received ACTION_CONNECTION_ACCESS_REPLY:" + requestType + "isWaitingAuthorization:" + BluetoothMapService.this.mIsWaitingAuthorization);
                if (BluetoothMapService.this.mIsWaitingAuthorization && requestType == 3) {
                    BluetoothMapService.this.mIsWaitingAuthorization = false;
                    if (BluetoothMapService.this.mRemoveTimeoutMsg) {
                        BluetoothMapService.this.mSessionStatusHandler.removeMessages(2);
                        BluetoothMapService.this.cancelUserTimeoutAlarm();
                        BluetoothMapService.this.setState(0);
                    }
                    if (intent.getIntExtra("android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT", 2) == 1) {
                        BluetoothMapService.this.mPermission = 1;
                        if (intent.getBooleanExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", false)) {
                            Log.d(BluetoothMapService.TAG, "setMessageAccessPermission(ACCESS_ALLOWED) result=" + BluetoothMapService.this.mRemoteDevice.setMessageAccessPermission(1));
                        }
                        BluetoothMapService.this.sendConnectMessage(-1);
                        return;
                    }
                    BluetoothMapService.this.mPermission = 2;
                    if (intent.getBooleanExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", false)) {
                        Log.d(BluetoothMapService.TAG, "setMessageAccessPermission(ACCESS_REJECTED) result=" + BluetoothMapService.this.mRemoteDevice.setMessageAccessPermission(2));
                    }
                    BluetoothMapService.this.sendConnectCancelMessage();
                    return;
                }
                return;
            }
            if (action.equals(BluetoothMapService.ACTION_SHOW_MAPS_EMAIL_SETTINGS)) {
                Log.v(BluetoothMapService.TAG, "Received ACTION_SHOW_MAPS_EMAIL_SETTINGS.");
                Intent in = new Intent(context, (Class<?>) BluetoothMapEmailSettings.class);
                in.setFlags(335544320);
                context.startActivity(in);
                return;
            }
            if (action.equals(BluetoothMapContentObserver.ACTION_MESSAGE_SENT)) {
                int result = getResultCode();
                boolean handled = false;
                if (BluetoothMapService.this.mMasInstances != null && (masInst = (BluetoothMapMasInstance) BluetoothMapService.this.mMasInstances.get(0)) != null) {
                    intent.putExtra(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_RESULT, result);
                    if (masInst.handleSmsSendIntent(context, intent)) {
                        handled = true;
                    }
                }
                if (!handled) {
                    try {
                        BluetoothMapContentObserver.actionMessageSentDisconnected(context, intent, result);
                        return;
                    } catch (IllegalArgumentException e) {
                        return;
                    }
                }
                return;
            }
            if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED") && BluetoothMapService.this.mIsWaitingAuthorization) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (BluetoothMapService.this.mRemoteDevice == null || device == null) {
                    Log.e(BluetoothMapService.TAG, "Unexpected error!");
                    return;
                }
                Log.d(BluetoothMapService.TAG, "ACL disconnected for " + device);
                if (BluetoothMapService.this.mRemoteDevice.equals(device) && BluetoothMapService.this.mRemoveTimeoutMsg) {
                    BluetoothMapService.this.mSessionStatusHandler.removeMessages(2);
                    Intent timeoutIntent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL");
                    timeoutIntent.putExtra("android.bluetooth.device.extra.DEVICE", BluetoothMapService.this.mRemoteDevice);
                    timeoutIntent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 3);
                    BluetoothMapService.this.sendBroadcast(timeoutIntent, "android.permission.BLUETOOTH");
                    BluetoothMapService.this.mIsWaitingAuthorization = false;
                    BluetoothMapService.this.mRemoveTimeoutMsg = false;
                }
            }
        }
    }

    private static class BluetoothMapBinder extends IBluetoothMap.Stub implements ProfileService.IProfileServiceBinder {
        private BluetoothMapService mService;

        private BluetoothMapService getService() {
            if (!Utils.checkCaller()) {
                Log.w(BluetoothMapService.TAG, "MAP call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            this.mService.enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
            return this.mService;
        }

        BluetoothMapBinder(BluetoothMapService service) {
            this.mService = service;
        }

        @Override
        public boolean cleanup() {
            this.mService = null;
            return true;
        }

        public int getState() {
            BluetoothMapService service = getService();
            if (service == null) {
                return 0;
            }
            return getService().getState();
        }

        public BluetoothDevice getClient() {
            BluetoothMapService service = getService();
            if (service == null) {
                return null;
            }
            Log.v(BluetoothMapService.TAG, "getClient() - returning " + service.getRemoteDevice());
            return service.getRemoteDevice();
        }

        public boolean isConnected(BluetoothDevice device) {
            BluetoothMapService service = getService();
            return service != null && service.getState() == 2 && service.getRemoteDevice().equals(device);
        }

        public boolean connect(BluetoothDevice device) {
            BluetoothMapService service = getService();
            if (service == null) {
            }
            return false;
        }

        public boolean disconnect(BluetoothDevice device) {
            BluetoothMapService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(device);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            BluetoothMapService service = getService();
            return service == null ? new ArrayList(0) : service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
            BluetoothMapService service = getService();
            return service == null ? new ArrayList(0) : service.getDevicesMatchingConnectionStates(states);
        }

        public int getConnectionState(BluetoothDevice device) {
            BluetoothMapService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(device);
        }

        public boolean setPriority(BluetoothDevice device, int priority) {
            BluetoothMapService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(device, priority);
        }

        public int getPriority(BluetoothDevice device) {
            BluetoothMapService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(device);
        }
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        println(sb, "mRemoteDevice: " + this.mRemoteDevice);
        println(sb, "sRemoteDeviceName: " + sRemoteDeviceName);
        println(sb, "mState: " + this.mState);
        println(sb, "mAppObserver: " + this.mAppObserver);
        println(sb, "mIsWaitingAuthorization: " + this.mIsWaitingAuthorization);
        println(sb, "mRemoveTimeoutMsg: " + this.mRemoveTimeoutMsg);
        println(sb, "mPermission: " + this.mPermission);
        println(sb, "mAccountChanged: " + this.mAccountChanged);
        println(sb, "mBluetoothMnsObexClient: " + this.mBluetoothMnsObexClient);
        println(sb, "mMasInstanceMap:");
        for (BluetoothMapEmailSettingsItem key : this.mMasInstanceMap.keySet()) {
            println(sb, "  " + key + " : " + this.mMasInstanceMap.get(key));
        }
        println(sb, "mEnabledAccounts:");
        for (BluetoothMapEmailSettingsItem account : this.mEnabledAccounts) {
            println(sb, "  " + account);
        }
    }
}
