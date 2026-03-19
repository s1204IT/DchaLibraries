package com.android.bluetooth.map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothMap;
import android.bluetooth.SdpMnsRecord;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.EnvironmentCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BluetoothMapService extends ProfileService {
    static final String ACTION_SHOW_MAPS_SETTINGS = "android.btmap.intent.action.SHOW_MAPS_SETTINGS";
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    private static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    private static final int DISCONNECT_MAP = 3;
    private static final int MAS_ID_SMS_MMS = 0;
    static final int MSG_ACQUIRE_WAKE_LOCK = 5005;
    static final int MSG_MAS_CONNECT = 5003;
    static final int MSG_MAS_CONNECT_CANCEL = 5004;
    static final int MSG_MNS_SDP_SEARCH = 5007;
    static final int MSG_OBSERVER_REGISTRATION = 5008;
    static final int MSG_RELEASE_WAKE_LOCK = 5006;
    static final int MSG_SERVERSESSION_CLOSE = 5000;
    static final int MSG_SESSION_DISCONNECTED = 5002;
    static final int MSG_SESSION_ESTABLISHED = 5001;
    private static final int RELEASE_WAKE_LOCK_DELAY = 10000;
    private static final int SHUTDOWN = 4;
    private static final int START_LISTENER = 1;
    private static final String TAG = "BluetoothMapService";
    private static final int UPDATE_MAS_INSTANCES = 5;
    static final int UPDATE_MAS_INSTANCES_ACCOUNT_ADDED = 0;
    static final int UPDATE_MAS_INSTANCES_ACCOUNT_DISCONNECT = 3;
    static final int UPDATE_MAS_INSTANCES_ACCOUNT_REMOVED = 1;
    static final int UPDATE_MAS_INSTANCES_ACCOUNT_RENAMED = 2;
    public static final String USER_CONFIRM_TIMEOUT_ACTION = "com.android.bluetooth.map.USER_CONFIRM_TIMEOUT";
    private static final int USER_CONFIRM_TIMEOUT_VALUE = 25000;
    private static final int USER_TIMEOUT = 2;
    public static final boolean VERBOSE = false;
    private static BluetoothMapService sBluetoothMapService;
    private BluetoothAdapter mAdapter;
    private MapServiceMessageHandler mSessionStatusHandler;
    public static final boolean DEBUG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static BluetoothDevice sRemoteDevice = null;
    private static String sRemoteDeviceName = null;
    private static final ParcelUuid[] MAP_UUIDS = {BluetoothUuid.MAP, BluetoothUuid.MNS};
    private PowerManager.WakeLock mWakeLock = null;
    private BluetoothMnsObexClient mBluetoothMnsObexClient = null;
    private SparseArray<BluetoothMapMasInstance> mMasInstances = new SparseArray<>(1);
    private HashMap<BluetoothMapAccountItem, BluetoothMapMasInstance> mMasInstanceMap = new HashMap<>(1);
    private ArrayList<BluetoothMapAccountItem> mEnabledAccounts = null;
    private BluetoothMapAppObserver mAppObserver = null;
    private AlarmManager mAlarmManager = null;
    private boolean mIsWaitingAuthorization = false;
    private boolean mRemoveTimeoutMsg = false;
    private boolean mRegisteredMapReceiver = false;
    private int mPermission = 0;
    private boolean mAccountChanged = false;
    private boolean mSdpSearchInitiated = false;
    private SdpMnsRecord mMnsRecord = null;
    private boolean mServiceStarted = false;
    private boolean mSmsCapable = true;
    private MapBroadcastReceiver mMapReceiver = new MapBroadcastReceiver();
    private int mState = 0;

    private synchronized void closeService() {
        if (DEBUG) {
            Log.d(TAG, "closeService() in");
        }
        if (this.mBluetoothMnsObexClient != null) {
            this.mBluetoothMnsObexClient.shutdown();
            this.mBluetoothMnsObexClient = null;
        }
        int size = this.mMasInstances.size();
        for (int i = 0; i < size; i++) {
            this.mMasInstances.valueAt(i).shutdown();
        }
        this.mMasInstances.clear();
        this.mIsWaitingAuthorization = false;
        this.mPermission = 0;
        setState(0);
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
        sRemoteDevice = null;
        if (this.mSessionStatusHandler == null) {
            return;
        }
        this.mSessionStatusHandler.removeCallbacksAndMessages(null);
        Looper looper = this.mSessionStatusHandler.getLooper();
        if (looper != null) {
            looper.quit();
        }
        this.mSessionStatusHandler = null;
    }

    private void startSocketListeners(int i) {
        if (i == -1) {
            int size = this.mMasInstances.size();
            for (int i2 = 0; i2 < size; i2++) {
                this.mMasInstances.valueAt(i2).startSocketListeners();
            }
            return;
        }
        BluetoothMapMasInstance bluetoothMapMasInstance = this.mMasInstances.get(i);
        if (bluetoothMapMasInstance != null) {
            bluetoothMapMasInstance.startSocketListeners();
            return;
        }
        Log.w(TAG, "startSocketListeners(): Invalid MasId: " + i);
    }

    private void startObexServerSessions() {
        if (DEBUG) {
            Log.d(TAG, "Map Service START ObexServerSessions()");
        }
        if (this.mWakeLock == null) {
            this.mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(1, "StartingObexMapTransaction");
            this.mWakeLock.setReferenceCounted(false);
            this.mWakeLock.acquire();
        }
        if (this.mBluetoothMnsObexClient == null) {
            this.mBluetoothMnsObexClient = new BluetoothMnsObexClient(sRemoteDevice, this.mMnsRecord, this.mSessionStatusHandler);
        }
        int size = this.mMasInstances.size();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            try {
                if (this.mMasInstances.valueAt(i).startObexServerSession(this.mBluetoothMnsObexClient)) {
                    z = true;
                }
            } catch (RemoteException e) {
                Log.w(TAG, "RemoteException occured while starting an obexServerSession restarting the listener", e);
                this.mMasInstances.valueAt(i).restartObexServerSession();
            } catch (IOException e2) {
                Log.w(TAG, "IOException occured while starting an obexServerSession restarting the listener", e2);
                this.mMasInstances.valueAt(i).restartObexServerSession();
            }
        }
        if (z) {
            setState(2);
        }
        this.mSessionStatusHandler.removeMessages(5006);
        this.mSessionStatusHandler.sendMessageDelayed(this.mSessionStatusHandler.obtainMessage(5006), 10000L);
    }

    public Handler getHandler() {
        return this.mSessionStatusHandler;
    }

    private void stopObexServerSessions(int i) {
        if (DEBUG) {
            Log.d(TAG, "MAP Service STOP ObexServerSessions()");
        }
        boolean z = true;
        if (i != -1) {
            int size = this.mMasInstances.size();
            boolean z2 = true;
            for (int i2 = 0; i2 < size; i2++) {
                BluetoothMapMasInstance bluetoothMapMasInstanceValueAt = this.mMasInstances.valueAt(i2);
                if (bluetoothMapMasInstanceValueAt.getMasId() != i && bluetoothMapMasInstanceValueAt.isStarted()) {
                    z2 = false;
                }
            }
            z = z2;
        }
        if (this.mBluetoothMnsObexClient != null && z) {
            this.mBluetoothMnsObexClient.shutdown();
            this.mBluetoothMnsObexClient = null;
        }
        BluetoothMapMasInstance bluetoothMapMasInstance = this.mMasInstances.get(i);
        if (bluetoothMapMasInstance != null) {
            bluetoothMapMasInstance.restartObexServerSession();
        } else if (i == -1) {
            int size2 = this.mMasInstances.size();
            for (int i3 = 0; i3 < size2; i3++) {
                this.mMasInstances.valueAt(i3).restartObexServerSession();
            }
        }
        if (z) {
            setState(0);
            this.mPermission = 0;
            sRemoteDevice = null;
            if (this.mAccountChanged) {
                updateMasInstances(3);
            }
        }
        if (this.mWakeLock != null && z) {
            this.mSessionStatusHandler.removeMessages(5005);
            this.mSessionStatusHandler.removeMessages(5006);
            this.mWakeLock.release();
        }
    }

    private final class MapServiceMessageHandler extends Handler {
        private MapServiceMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            switch (i) {
                case 1:
                    if (BluetoothMapService.this.mAdapter.isEnabled()) {
                        BluetoothMapService.this.startSocketListeners(message.arg1);
                    }
                    break;
                case 2:
                    if (BluetoothMapService.this.mIsWaitingAuthorization) {
                        Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL");
                        intent.setPackage(BluetoothMapService.this.getString(R.string.pairing_ui_package));
                        intent.putExtra("android.bluetooth.device.extra.DEVICE", BluetoothMapService.sRemoteDevice);
                        intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 3);
                        BluetoothMapService.this.sendBroadcast(intent);
                        BluetoothMapService.this.cancelUserTimeoutAlarm();
                        BluetoothMapService.this.mIsWaitingAuthorization = false;
                        BluetoothMapService.this.stopObexServerSessions(-1);
                    }
                    break;
                case 3:
                    BluetoothMapService.this.disconnectMap((BluetoothDevice) message.obj);
                    break;
                case 4:
                    BluetoothMapService.this.closeService();
                    break;
                case 5:
                    BluetoothMapService.this.updateMasInstancesHandler();
                    break;
                default:
                    switch (i) {
                        case 5000:
                            BluetoothMapService.this.stopObexServerSessions(message.arg1);
                            break;
                        case BluetoothMapService.MSG_MAS_CONNECT:
                            BluetoothMapService.this.onConnectHandler(message.arg1);
                            break;
                        case BluetoothMapService.MSG_MAS_CONNECT_CANCEL:
                            BluetoothMapService.this.stopObexServerSessions(-1);
                            break;
                        case 5005:
                            if (BluetoothMapService.this.mWakeLock == null) {
                                BluetoothMapService.this.mWakeLock = ((PowerManager) BluetoothMapService.this.getSystemService("power")).newWakeLock(1, "StartingObexMapTransaction");
                                BluetoothMapService.this.mWakeLock.setReferenceCounted(false);
                            }
                            if (!BluetoothMapService.this.mWakeLock.isHeld()) {
                                BluetoothMapService.this.mWakeLock.acquire();
                                if (BluetoothMapService.DEBUG) {
                                    Log.d(BluetoothMapService.TAG, "  Acquired Wake Lock by message");
                                }
                            }
                            BluetoothMapService.this.mSessionStatusHandler.removeMessages(5006);
                            BluetoothMapService.this.mSessionStatusHandler.sendMessageDelayed(BluetoothMapService.this.mSessionStatusHandler.obtainMessage(5006), 10000L);
                            break;
                        case 5006:
                            if (BluetoothMapService.this.mWakeLock != null) {
                                BluetoothMapService.this.mWakeLock.release();
                                if (BluetoothMapService.DEBUG) {
                                    Log.d(BluetoothMapService.TAG, "  Released Wake Lock by message");
                                }
                            }
                            break;
                        case 5007:
                            if (BluetoothMapService.sRemoteDevice != null) {
                                if (BluetoothMapService.DEBUG) {
                                    Log.d(BluetoothMapService.TAG, "MNS SDP Initiate Search ..");
                                }
                                BluetoothMapService.sRemoteDevice.sdpSearch(BluetoothMnsObexClient.BLUETOOTH_UUID_OBEX_MNS);
                            } else {
                                Log.w(BluetoothMapService.TAG, "remoteDevice info not available");
                            }
                            break;
                        case BluetoothMapService.MSG_OBSERVER_REGISTRATION:
                            if (BluetoothMapService.DEBUG) {
                                Log.d(BluetoothMapService.TAG, "ContentObserver Registration MASID: " + message.arg1 + " Enable: " + message.arg2);
                            }
                            BluetoothMapMasInstance bluetoothMapMasInstance = (BluetoothMapMasInstance) BluetoothMapService.this.mMasInstances.get(message.arg1);
                            if (bluetoothMapMasInstance != null && bluetoothMapMasInstance.mObserver != null) {
                                try {
                                    if (message.arg2 == 1) {
                                        bluetoothMapMasInstance.mObserver.registerObserver();
                                    } else {
                                        bluetoothMapMasInstance.mObserver.unregisterObserver();
                                    }
                                } catch (RemoteException e) {
                                    Log.e(BluetoothMapService.TAG, "ContentObserverRegistarion Failed: " + e);
                                    return;
                                }
                                break;
                            }
                            break;
                    }
                    break;
            }
        }
    }

    private void onConnectHandler(int i) {
        if (this.mIsWaitingAuthorization || sRemoteDevice == null || this.mSdpSearchInitiated) {
            return;
        }
        BluetoothMapMasInstance bluetoothMapMasInstance = this.mMasInstances.get(i);
        if (DEBUG) {
            Log.d(TAG, "mPermission = " + this.mPermission);
        }
        if (this.mPermission == 1) {
            try {
                if (this.mBluetoothMnsObexClient != null && bluetoothMapMasInstance != null) {
                    bluetoothMapMasInstance.startObexServerSession(this.mBluetoothMnsObexClient);
                } else {
                    startObexServerSessions();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "catch RemoteException starting obex server session", e);
            } catch (IOException e2) {
                Log.e(TAG, "catch IOException starting obex server session", e2);
            }
        }
    }

    public int getState() {
        return this.mState;
    }

    public static BluetoothDevice getRemoteDevice() {
        return sRemoteDevice;
    }

    private void setState(int i) {
        setState(i, 1);
    }

    private synchronized void setState(int i, int i2) {
        if (i != this.mState) {
            if (DEBUG) {
                Log.d(TAG, "Map state " + this.mState + " -> " + i + ", result = " + i2);
            }
            int i3 = this.mState;
            this.mState = i;
            Intent intent = new Intent("android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED");
            intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i3);
            intent.putExtra("android.bluetooth.profile.extra.STATE", this.mState);
            intent.putExtra("android.bluetooth.device.extra.DEVICE", sRemoteDevice);
            sendBroadcast(intent, "android.permission.BLUETOOTH");
        }
    }

    void disconnect(BluetoothDevice bluetoothDevice) {
        this.mSessionStatusHandler.sendMessage(this.mSessionStatusHandler.obtainMessage(3, 0, 0, bluetoothDevice));
    }

    void disconnectMap(BluetoothDevice bluetoothDevice) {
        if (DEBUG) {
            Log.d(TAG, "disconnectMap");
        }
        if (getRemoteDevice() != null && getRemoteDevice().equals(bluetoothDevice) && this.mState == 2) {
            stopObexServerSessions(-1);
        }
    }

    private List<BluetoothDevice> getConnectedDevices() {
        ArrayList arrayList = new ArrayList();
        synchronized (this) {
            if (this.mState == 2 && sRemoteDevice != null) {
                arrayList.add(sRemoteDevice);
            }
        }
        return arrayList;
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        ArrayList arrayList = new ArrayList();
        Set<BluetoothDevice> bondedDevices = this.mAdapter.getBondedDevices();
        if (bondedDevices == null) {
            return arrayList;
        }
        synchronized (this) {
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                if (BluetoothUuid.containsAnyUuid(bluetoothDevice.getUuids(), MAP_UUIDS)) {
                    int connectionState = getConnectionState(bluetoothDevice);
                    for (int i : iArr) {
                        if (connectionState == i) {
                            arrayList.add(bluetoothDevice);
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    int getConnectionState(BluetoothDevice bluetoothDevice) {
        synchronized (this) {
            return (getState() == 2 && getRemoteDevice().equals(bluetoothDevice)) ? 2 : 0;
        }
    }

    boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        return Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothMapPriorityKey(bluetoothDevice.getAddress()), i);
    }

    int getPriority(BluetoothDevice bluetoothDevice) {
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothMapPriorityKey(bluetoothDevice.getAddress()), -1);
    }

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothMapBinder(this);
    }

    @Override
    protected boolean start() {
        if (DEBUG) {
            Log.d(TAG, "start()");
        }
        HandlerThread handlerThread = new HandlerThread("BluetoothMapHandler");
        handlerThread.start();
        this.mSessionStatusHandler = new MapServiceMessageHandler(handlerThread.getLooper());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        intentFilter.addAction("android.bluetooth.device.action.SDP_RECORD");
        intentFilter.addAction(ACTION_SHOW_MAPS_SETTINGS);
        intentFilter.addAction(USER_CONFIRM_TIMEOUT_ACTION);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.android.bluetooth.BluetoothMapContentObserver.action.MESSAGE_SENT");
        try {
            intentFilter2.addDataType("message/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.e(TAG, "Wrong mime type!!!", e);
        }
        if (!this.mRegisteredMapReceiver) {
            registerReceiver(this.mMapReceiver, intentFilter);
            registerReceiver(this.mMapReceiver, intentFilter2);
            this.mRegisteredMapReceiver = true;
        }
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mAppObserver = new BluetoothMapAppObserver(this, this);
        this.mSmsCapable = getResources().getBoolean(android.R.^attr-private.notificationHeaderIconSize);
        this.mEnabledAccounts = this.mAppObserver.getEnabledAccountItems();
        createMasInstances();
        sendStartListenerMessage(-1);
        setBluetoothMapService(this);
        this.mServiceStarted = true;
        return true;
    }

    @VisibleForTesting
    public static synchronized BluetoothMapService getBluetoothMapService() {
        if (sBluetoothMapService == null) {
            Log.w(TAG, "getBluetoothMapService(): service is null");
            return null;
        }
        if (!sBluetoothMapService.isAvailable()) {
            Log.w(TAG, "getBluetoothMapService(): service is not available");
            return null;
        }
        return sBluetoothMapService;
    }

    private static synchronized void setBluetoothMapService(BluetoothMapService bluetoothMapService) {
        if (DEBUG) {
            Log.d(TAG, "setBluetoothMapService(): set to: " + bluetoothMapService);
        }
        sBluetoothMapService = bluetoothMapService;
    }

    void updateMasInstances(int i) {
        this.mSessionStatusHandler.obtainMessage(5, i, 0).sendToTarget();
    }

    private void updateMasInstancesHandler() {
        if (DEBUG) {
            Log.d(TAG, "updateMasInstancesHandler() state = " + getState());
        }
        if (getState() != 0) {
            this.mAccountChanged = true;
            return;
        }
        ArrayList<BluetoothMapAccountItem> enabledAccountItems = this.mAppObserver.getEnabledAccountItems();
        ArrayList<BluetoothMapAccountItem> arrayList = new ArrayList();
        for (BluetoothMapAccountItem bluetoothMapAccountItem : enabledAccountItems) {
            if (!this.mEnabledAccounts.remove(bluetoothMapAccountItem)) {
                arrayList.add(bluetoothMapAccountItem);
            }
        }
        if (this.mEnabledAccounts.size() > 0) {
            Iterator<BluetoothMapAccountItem> it = this.mEnabledAccounts.iterator();
            while (it.hasNext()) {
                BluetoothMapMasInstance bluetoothMapMasInstanceRemove = this.mMasInstanceMap.remove(it.next());
                if (bluetoothMapMasInstanceRemove != null) {
                    bluetoothMapMasInstanceRemove.shutdown();
                    this.mMasInstances.remove(bluetoothMapMasInstanceRemove.getMasId());
                }
            }
        }
        for (BluetoothMapAccountItem bluetoothMapAccountItem2 : arrayList) {
            int nextMasId = getNextMasId();
            BluetoothMapMasInstance bluetoothMapMasInstance = new BluetoothMapMasInstance(this, this, bluetoothMapAccountItem2, nextMasId, false);
            this.mMasInstances.append(nextMasId, bluetoothMapMasInstance);
            this.mMasInstanceMap.put(bluetoothMapAccountItem2, bluetoothMapMasInstance);
            if (this.mAdapter.isEnabled()) {
                bluetoothMapMasInstance.startSocketListeners();
            }
        }
        this.mEnabledAccounts = enabledAccountItems;
        this.mAccountChanged = false;
    }

    private int getNextMasId() {
        int size = this.mMasInstances.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            int iKeyAt = this.mMasInstances.keyAt(i2);
            if (iKeyAt > i) {
                i = iKeyAt;
            }
        }
        if (i < 255) {
            return i + 1;
        }
        for (int i3 = 1; i3 <= 255; i3++) {
            if (this.mMasInstances.get(i3) == null) {
                return i3;
            }
        }
        return 255;
    }

    private void createMasInstances() {
        int i = 0;
        if (this.mSmsCapable) {
            BluetoothMapMasInstance bluetoothMapMasInstance = new BluetoothMapMasInstance(this, this, null, 0, true);
            this.mMasInstances.append(0, bluetoothMapMasInstance);
            this.mMasInstanceMap.put(null, bluetoothMapMasInstance);
            i = 1;
        }
        for (BluetoothMapAccountItem bluetoothMapAccountItem : this.mEnabledAccounts) {
            BluetoothMapMasInstance bluetoothMapMasInstance2 = new BluetoothMapMasInstance(this, this, bluetoothMapAccountItem, i, false);
            this.mMasInstances.append(i, bluetoothMapMasInstance2);
            this.mMasInstanceMap.put(bluetoothMapAccountItem, bluetoothMapMasInstance2);
            i++;
        }
    }

    @Override
    protected boolean stop() {
        if (DEBUG) {
            Log.d(TAG, "stop()");
        }
        if (!this.mServiceStarted) {
            if (DEBUG) {
                Log.d(TAG, "mServiceStarted is false - Ignoring");
            }
            return true;
        }
        setBluetoothMapService(null);
        this.mServiceStarted = false;
        if (this.mRegisteredMapReceiver) {
            this.mRegisteredMapReceiver = false;
            unregisterReceiver(this.mMapReceiver);
            this.mAppObserver.shutdown();
        }
        sendShutdownMessage();
        return true;
    }

    public boolean onConnect(BluetoothDevice bluetoothDevice, BluetoothMapMasInstance bluetoothMapMasInstance) {
        boolean z;
        synchronized (this) {
            boolean z2 = false;
            if (sRemoteDevice == null) {
                sRemoteDevice = bluetoothDevice;
                sRemoteDeviceName = sRemoteDevice.getName();
                if (TextUtils.isEmpty(sRemoteDeviceName)) {
                    sRemoteDeviceName = getString(R.string.defaultname);
                }
                this.mPermission = sRemoteDevice.getMessageAccessPermission();
                if (this.mPermission == 0) {
                    this.mIsWaitingAuthorization = true;
                    setUserTimeoutAlarm();
                    z = false;
                    z2 = true;
                } else if (this.mPermission == 2) {
                    z = true;
                } else if (this.mPermission == 1) {
                    sRemoteDevice.sdpSearch(BluetoothMnsObexClient.BLUETOOTH_UUID_OBEX_MNS);
                    this.mSdpSearchInitiated = true;
                }
                if (!z2) {
                    Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST");
                    intent.setPackage(getString(R.string.pairing_ui_package));
                    intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 3);
                    intent.putExtra("android.bluetooth.device.extra.DEVICE", sRemoteDevice);
                    sendOrderedBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
                } else if (z) {
                    sendConnectCancelMessage();
                } else if (this.mPermission == 1) {
                    sendConnectMessage(bluetoothMapMasInstance.getMasId());
                    MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.MAP);
                }
                return true;
            }
            if (!sRemoteDevice.equals(bluetoothDevice)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Unexpected connection from a second Remote Device received. name: ");
                sb.append(bluetoothDevice == null ? EnvironmentCompat.MEDIA_UNKNOWN : bluetoothDevice.getName());
                Log.w(TAG, sb.toString());
                return false;
            }
            z = false;
            if (!z2) {
            }
            return true;
        }
    }

    private void setUserTimeoutAlarm() {
        if (DEBUG) {
            Log.d(TAG, "SetUserTimeOutAlarm()");
        }
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) getSystemService(NotificationCompat.CATEGORY_ALARM);
        }
        this.mRemoveTimeoutMsg = true;
        this.mAlarmManager.set(0, System.currentTimeMillis() + 25000, PendingIntent.getBroadcast(this, 0, new Intent(USER_CONFIRM_TIMEOUT_ACTION), 0));
    }

    private void cancelUserTimeoutAlarm() {
        if (DEBUG) {
            Log.d(TAG, "cancelUserTimeOutAlarm()");
        }
        PendingIntent broadcast = PendingIntent.getBroadcast(this, 0, new Intent(USER_CONFIRM_TIMEOUT_ACTION), 0);
        broadcast.cancel();
        ((AlarmManager) getSystemService(NotificationCompat.CATEGORY_ALARM)).cancel(broadcast);
        this.mRemoveTimeoutMsg = false;
    }

    void sendStartListenerMessage(int i) {
        if (this.mSessionStatusHandler != null && !this.mSessionStatusHandler.hasMessages(1)) {
            this.mSessionStatusHandler.sendMessageDelayed(this.mSessionStatusHandler.obtainMessage(1, i, 0), 20L);
        } else if (this.mSessionStatusHandler != null && DEBUG) {
            Log.w(TAG, "mSessionStatusHandler START_LISTENER message already in Queue");
        }
    }

    private void sendConnectMessage(int i) {
        if (this.mSessionStatusHandler != null) {
            this.mSessionStatusHandler.sendMessageDelayed(this.mSessionStatusHandler.obtainMessage(MSG_MAS_CONNECT, i, 0), 20L);
        }
    }

    private void sendConnectTimeoutMessage() {
        if (DEBUG) {
            Log.d(TAG, "sendConnectTimeoutMessage()");
        }
        if (this.mSessionStatusHandler != null) {
            this.mSessionStatusHandler.obtainMessage(2).sendToTarget();
        }
    }

    private void sendConnectCancelMessage() {
        if (this.mSessionStatusHandler != null) {
            this.mSessionStatusHandler.obtainMessage(MSG_MAS_CONNECT_CANCEL).sendToTarget();
        }
    }

    private void sendShutdownMessage() {
        if (this.mRemoveTimeoutMsg) {
            sendBroadcast(new Intent(USER_CONFIRM_TIMEOUT_ACTION), "android.permission.BLUETOOTH");
            this.mIsWaitingAuthorization = false;
            cancelUserTimeoutAlarm();
        }
        if (this.mSessionStatusHandler == null) {
            Log.w(TAG, "mSessionStatusHandler is null");
            return;
        }
        if (this.mSessionStatusHandler.hasMessages(4)) {
            if (DEBUG) {
                Log.w(TAG, "mSessionStatusHandler shutdown message already in Queue");
                return;
            }
            return;
        }
        try {
            this.mSessionStatusHandler.removeCallbacksAndMessages(null);
            if (!this.mSessionStatusHandler.sendMessage(this.mSessionStatusHandler.obtainMessage(4))) {
                Log.w(TAG, "mSessionStatusHandler shutdown message could not be sent");
            } else if (DEBUG) {
                Log.e(TAG, "mSessionStatusHandler.sendMessage() dispatched shutdown message");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "mSessionStatusHandler already null, no need send shutdown msg", e);
        }
    }

    private class MapBroadcastReceiver extends BroadcastReceiver {
        private MapBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothMapMasInstance bluetoothMapMasInstance;
            if (BluetoothMapService.DEBUG) {
                Log.d(BluetoothMapService.TAG, "onReceive");
            }
            String action = intent.getAction();
            if (BluetoothMapService.DEBUG) {
                Log.d(BluetoothMapService.TAG, "onReceive: " + action);
            }
            if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                int intExtra = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
                if (intExtra == 13) {
                    if (BluetoothMapService.DEBUG) {
                        Log.d(BluetoothMapService.TAG, "STATE_TURNING_OFF");
                    }
                    BluetoothMapService.this.sendShutdownMessage();
                    return;
                } else {
                    if (intExtra == 12) {
                        if (BluetoothMapService.DEBUG) {
                            Log.d(BluetoothMapService.TAG, "STATE_ON");
                        }
                        BluetoothMapService.this.sendStartListenerMessage(-1);
                        return;
                    }
                    return;
                }
            }
            if (action.equals(BluetoothMapService.USER_CONFIRM_TIMEOUT_ACTION)) {
                if (BluetoothMapService.DEBUG) {
                    Log.d(BluetoothMapService.TAG, "USER_CONFIRM_TIMEOUT ACTION Received.");
                }
                BluetoothMapService.this.sendConnectTimeoutMessage();
                return;
            }
            boolean zHandleSmsSendIntent = false;
            if (action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY")) {
                int intExtra2 = intent.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                if (BluetoothMapService.DEBUG) {
                    Log.d(BluetoothMapService.TAG, "Received ACTION_CONNECTION_ACCESS_REPLY:" + intExtra2 + "isWaitingAuthorization:" + BluetoothMapService.this.mIsWaitingAuthorization);
                }
                if (BluetoothMapService.this.mIsWaitingAuthorization && intExtra2 == 3) {
                    BluetoothMapService.this.mIsWaitingAuthorization = false;
                    if (BluetoothMapService.this.mRemoveTimeoutMsg) {
                        BluetoothMapService.this.mSessionStatusHandler.removeMessages(2);
                        BluetoothMapService.this.cancelUserTimeoutAlarm();
                        BluetoothMapService.this.setState(0);
                    }
                    if (intent.getIntExtra("android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT", 2) == 1) {
                        BluetoothMapService.this.mPermission = 1;
                        if (intent.getBooleanExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", false)) {
                            boolean messageAccessPermission = BluetoothMapService.sRemoteDevice.setMessageAccessPermission(1);
                            if (BluetoothMapService.DEBUG) {
                                Log.d(BluetoothMapService.TAG, "setMessageAccessPermission(ACCESS_ALLOWED) result=" + messageAccessPermission);
                            }
                        }
                        BluetoothMapService.sRemoteDevice.sdpSearch(BluetoothMnsObexClient.BLUETOOTH_UUID_OBEX_MNS);
                        BluetoothMapService.this.mSdpSearchInitiated = true;
                        return;
                    }
                    BluetoothMapService.this.mPermission = 2;
                    if (intent.getBooleanExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", false)) {
                        boolean messageAccessPermission2 = BluetoothMapService.sRemoteDevice.setMessageAccessPermission(2);
                        if (BluetoothMapService.DEBUG) {
                            Log.d(BluetoothMapService.TAG, "setMessageAccessPermission(ACCESS_REJECTED) result=" + messageAccessPermission2);
                        }
                    }
                    BluetoothMapService.this.sendConnectCancelMessage();
                    return;
                }
                return;
            }
            if (action.equals("android.bluetooth.device.action.SDP_RECORD")) {
                if (BluetoothMapService.DEBUG) {
                    Log.d(BluetoothMapService.TAG, "Received ACTION_SDP_RECORD.");
                }
                if (((ParcelUuid) intent.getParcelableExtra("android.bluetooth.device.extra.UUID")).equals(BluetoothMnsObexClient.BLUETOOTH_UUID_OBEX_MNS)) {
                    BluetoothMapService.this.mMnsRecord = intent.getParcelableExtra("android.bluetooth.device.extra.SDP_RECORD");
                    int intExtra3 = intent.getIntExtra("android.bluetooth.device.extra.SDP_SEARCH_STATUS", -1);
                    if (BluetoothMapService.this.mBluetoothMnsObexClient != null && !BluetoothMapService.this.mSdpSearchInitiated) {
                        BluetoothMapService.this.mBluetoothMnsObexClient.setMnsRecord(BluetoothMapService.this.mMnsRecord);
                    }
                    if (intExtra3 != -1 && BluetoothMapService.this.mMnsRecord != null) {
                        int size = BluetoothMapService.this.mMasInstances.size();
                        for (int i = 0; i < size; i++) {
                            ((BluetoothMapMasInstance) BluetoothMapService.this.mMasInstances.valueAt(i)).setRemoteFeatureMask(BluetoothMapService.this.mMnsRecord.getSupportedFeatures());
                        }
                    }
                    if (BluetoothMapService.this.mSdpSearchInitiated) {
                        BluetoothMapService.this.mSdpSearchInitiated = false;
                        BluetoothMapService.this.sendConnectMessage(-1);
                        return;
                    }
                    return;
                }
                return;
            }
            if (action.equals(BluetoothMapService.ACTION_SHOW_MAPS_SETTINGS)) {
                Intent intent2 = new Intent(context, (Class<?>) BluetoothMapSettings.class);
                intent2.setFlags(335544320);
                context.startActivity(intent2);
                return;
            }
            if (!action.equals("com.android.bluetooth.BluetoothMapContentObserver.action.MESSAGE_SENT")) {
                if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED") && BluetoothMapService.this.mIsWaitingAuthorization) {
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    if (BluetoothMapService.sRemoteDevice != null && bluetoothDevice != null) {
                        if (BluetoothMapService.sRemoteDevice.equals(bluetoothDevice)) {
                            BluetoothMapService.this.mSessionStatusHandler.removeMessages(2);
                            BluetoothMapService.this.mSessionStatusHandler.obtainMessage(2).sendToTarget();
                            return;
                        }
                        return;
                    }
                    Log.e(BluetoothMapService.TAG, "Unexpected error!");
                    return;
                }
                return;
            }
            int resultCode = getResultCode();
            if (BluetoothMapService.this.mSmsCapable && BluetoothMapService.this.mMasInstances != null && (bluetoothMapMasInstance = (BluetoothMapMasInstance) BluetoothMapService.this.mMasInstances.get(0)) != null) {
                intent.putExtra(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_RESULT, resultCode);
                zHandleSmsSendIntent = bluetoothMapMasInstance.handleSmsSendIntent(context, intent);
            }
            if (!zHandleSmsSendIntent) {
                BluetoothMapContentObserver.actionMessageSentDisconnected(context, intent, resultCode);
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

        BluetoothMapBinder(BluetoothMapService bluetoothMapService) {
            this.mService = bluetoothMapService;
        }

        @Override
        public synchronized void cleanup() {
            this.mService = null;
        }

        public int getState() {
            if (getService() == null) {
                return 0;
            }
            return getService().getState();
        }

        public BluetoothDevice getClient() {
            if (getService() == null) {
                return null;
            }
            return BluetoothMapService.getRemoteDevice();
        }

        public boolean isConnected(BluetoothDevice bluetoothDevice) {
            BluetoothMapService service = getService();
            return service != null && service.getState() == 2 && BluetoothMapService.getRemoteDevice().equals(bluetoothDevice);
        }

        public boolean connect(BluetoothDevice bluetoothDevice) {
            return getService() == null ? false : false;
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            BluetoothMapService service = getService();
            if (service == null) {
                return false;
            }
            service.disconnect(bluetoothDevice);
            return true;
        }

        public List<BluetoothDevice> getConnectedDevices() {
            BluetoothMapService service = getService();
            if (service != null) {
                return service.getConnectedDevices();
            }
            return new ArrayList(0);
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            BluetoothMapService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            BluetoothMapService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
            BluetoothMapService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(bluetoothDevice, i);
        }

        public int getPriority(BluetoothDevice bluetoothDevice) {
            BluetoothMapService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(bluetoothDevice);
        }
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        println(sb, "mRemoteDevice: " + sRemoteDevice);
        println(sb, "sRemoteDeviceName: " + sRemoteDeviceName);
        println(sb, "mState: " + this.mState);
        println(sb, "mAppObserver: " + this.mAppObserver);
        println(sb, "mIsWaitingAuthorization: " + this.mIsWaitingAuthorization);
        println(sb, "mRemoveTimeoutMsg: " + this.mRemoveTimeoutMsg);
        println(sb, "mPermission: " + this.mPermission);
        println(sb, "mAccountChanged: " + this.mAccountChanged);
        println(sb, "mBluetoothMnsObexClient: " + this.mBluetoothMnsObexClient);
        println(sb, "mMasInstanceMap:");
        for (BluetoothMapAccountItem bluetoothMapAccountItem : this.mMasInstanceMap.keySet()) {
            println(sb, "  " + bluetoothMapAccountItem + " : " + this.mMasInstanceMap.get(bluetoothMapAccountItem));
        }
        println(sb, "mEnabledAccounts:");
        Iterator<BluetoothMapAccountItem> it = this.mEnabledAccounts.iterator();
        while (it.hasNext()) {
            println(sb, "  " + it.next());
        }
    }
}
