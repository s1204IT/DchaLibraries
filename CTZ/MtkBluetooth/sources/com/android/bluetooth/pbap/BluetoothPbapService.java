package com.android.bluetooth.pbap;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.IBluetoothPbap;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.bluetooth.IObexConnectionHandler;
import com.android.bluetooth.ObexServerSockets;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.sdp.SdpManager;
import com.android.bluetooth.util.DevicePolicyUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class BluetoothPbapService extends ProfileService implements IObexConnectionHandler {
    private static final String ACCESS_AUTHORITY_CLASS = "com.android.settings.bluetooth.BluetoothPermissionRequest";
    private static final String ACCESS_AUTHORITY_PACKAGE = "com.android.settings";
    static final String AUTH_CANCELLED_ACTION = "com.android.bluetooth.pbap.authcancelled";
    static final String AUTH_CHALL_ACTION = "com.android.bluetooth.pbap.authchall";
    static final String AUTH_RESPONSE_ACTION = "com.android.bluetooth.pbap.authresponse";
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    static final int CHECK_SECONDARY_VERSION_COUNTER = 6;
    static final int CONTACTS_LOADED = 5;
    public static final boolean DEBUG = true;
    static final String EXTRA_DEVICE = "com.android.bluetooth.pbap.device";
    static final String EXTRA_SESSION_KEY = "com.android.bluetooth.pbap.sessionkey";
    static final int LOAD_CONTACTS = 4;
    static final int MSG_ACQUIRE_WAKE_LOCK = 5004;
    static final int MSG_RELEASE_WAKE_LOCK = 5005;
    static final int MSG_STATE_MACHINE_DONE = 5006;
    private static final int PBAP_NOTIFICATION_ID_END = 2000000;
    private static final int PBAP_NOTIFICATION_ID_START = 1000000;
    static final int RELEASE_WAKE_LOCK_DELAY = 10000;
    static final int ROLLOVER_COUNTERS = 7;
    private static final int SDP_PBAP_SERVER_VERSION = 258;
    private static final int SDP_PBAP_SUPPORTED_FEATURES = 543;
    private static final int SDP_PBAP_SUPPORTED_REPOSITORIES = 1;
    static final int SHUTDOWN = 3;
    static final int START_LISTENER = 1;
    private static final String TAG = "BluetoothPbapService";
    static final String THIS_PACKAGE_NAME = "com.android.bluetooth";
    static final String USER_CONFIRM_TIMEOUT_ACTION = "com.android.bluetooth.pbap.userconfirmtimeout";
    static final int USER_CONFIRM_TIMEOUT_VALUE = 30000;
    static final int USER_TIMEOUT = 2;
    private static BluetoothPbapService sBluetoothPbapService;
    private static String sLocalPhoneName;
    private static String sLocalPhoneNum;
    private BluetoothPbapContentObserver mContactChangeObserver;
    protected Context mContext;
    private HandlerThread mHandlerThread;
    private PbapHandler mSessionStatusHandler;
    private Thread mThreadLoadContacts;
    private Thread mThreadUpdateSecVersionCounter;
    private PowerManager.WakeLock mWakeLock;
    public static final boolean VERBOSE = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    public static final boolean USER_MODE = SystemProperties.get("ro.build.type", "").equals("user");
    private ObexServerSockets mServerSockets = null;
    private int mSdpHandle = -1;
    private final HashMap<BluetoothDevice, PbapStateMachine> mPbapStateMachineMap = new HashMap<>();
    private volatile int mNextNotificationId = PBAP_NOTIFICATION_ID_START;
    private boolean mContactsLoaded = false;
    private BroadcastReceiver mPbapReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothPbapService.this.parseIntent(intent);
        }
    };

    private class BluetoothPbapContentObserver extends ContentObserver {
        BluetoothPbapContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean z) {
            Log.d(BluetoothPbapService.TAG, " onChange on contact uri ");
            BluetoothPbapService.this.sendUpdateRequest();
        }
    }

    private void sendUpdateRequest() {
        if (this.mContactsLoaded && !this.mSessionStatusHandler.hasMessages(6)) {
            this.mSessionStatusHandler.sendMessage(this.mSessionStatusHandler.obtainMessage(6));
        }
    }

    private void parseIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "action: " + action);
        if ("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY".equals(action)) {
            if (intent.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2) != 2) {
                return;
            }
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            synchronized (this.mPbapStateMachineMap) {
                PbapStateMachine pbapStateMachine = this.mPbapStateMachineMap.get(bluetoothDevice);
                if (pbapStateMachine == null) {
                    Log.w(TAG, "device not connected! device=" + bluetoothDevice);
                    return;
                }
                this.mSessionStatusHandler.removeMessages(2, pbapStateMachine);
                int intExtra = intent.getIntExtra("android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT", 2);
                boolean booleanExtra = intent.getBooleanExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", false);
                if (intExtra == 1) {
                    if (booleanExtra) {
                        bluetoothDevice.setPhonebookAccessPermission(1);
                        if (VERBOSE) {
                            Log.v(TAG, "setPhonebookAccessPermission(ACCESS_ALLOWED)");
                        }
                    }
                    pbapStateMachine.sendMessage(1);
                } else {
                    if (booleanExtra) {
                        bluetoothDevice.setPhonebookAccessPermission(2);
                        if (VERBOSE) {
                            Log.v(TAG, "setPhonebookAccessPermission(ACCESS_REJECTED)");
                        }
                    }
                    pbapStateMachine.sendMessage(2);
                }
                return;
            }
        }
        if (AUTH_RESPONSE_ACTION.equals(action)) {
            String stringExtra = intent.getStringExtra(EXTRA_SESSION_KEY);
            BluetoothDevice bluetoothDevice2 = (BluetoothDevice) intent.getParcelableExtra(EXTRA_DEVICE);
            synchronized (this.mPbapStateMachineMap) {
                PbapStateMachine pbapStateMachine2 = this.mPbapStateMachineMap.get(bluetoothDevice2);
                if (pbapStateMachine2 == null) {
                    return;
                }
                pbapStateMachine2.sendMessage(pbapStateMachine2.obtainMessage(7, stringExtra));
                return;
            }
        }
        if (AUTH_CANCELLED_ACTION.equals(action)) {
            BluetoothDevice bluetoothDevice3 = (BluetoothDevice) intent.getParcelableExtra(EXTRA_DEVICE);
            synchronized (this.mPbapStateMachineMap) {
                PbapStateMachine pbapStateMachine3 = this.mPbapStateMachineMap.get(bluetoothDevice3);
                if (pbapStateMachine3 == null) {
                    return;
                }
                pbapStateMachine3.sendMessage(8);
                return;
            }
        }
        Log.w(TAG, "Unhandled intent action: " + action);
    }

    private void closeService() {
        if (VERBOSE) {
            Log.v(TAG, "Pbap Service closeService");
        }
        BluetoothPbapUtils.savePbapParams(this);
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
        cleanUpServerSocket();
        if (this.mSessionStatusHandler != null) {
            this.mSessionStatusHandler.removeCallbacksAndMessages(null);
        }
    }

    private void cleanUpServerSocket() {
        synchronized (this.mPbapStateMachineMap) {
            Iterator<PbapStateMachine> it = this.mPbapStateMachineMap.values().iterator();
            while (it.hasNext()) {
                it.next().sendMessage(3);
            }
        }
        cleanUpSdpRecord();
        if (this.mServerSockets != null) {
            this.mServerSockets.shutdown(false);
            this.mServerSockets = null;
        }
    }

    private void createSdpRecord() {
        if (this.mSdpHandle > -1) {
            Log.w(TAG, "createSdpRecord, SDP record already created");
        }
        try {
            this.mSdpHandle = SdpManager.getDefaultManager().createPbapPseRecord("OBEX Phonebook Access Server", this.mServerSockets.getRfcommChannel(), this.mServerSockets.getL2capPsm(), 258, 1, SDP_PBAP_SUPPORTED_FEATURES);
            Log.d(TAG, "created Sdp record, mSdpHandle=" + this.mSdpHandle);
        } catch (NullPointerException e) {
            Log.e(TAG, "createSdpRecord:SdpManager is null,return", e);
        }
    }

    private void cleanUpSdpRecord() {
        if (this.mSdpHandle < 0) {
            Log.w(TAG, "cleanUpSdpRecord, SDP record never created");
            return;
        }
        int i = this.mSdpHandle;
        this.mSdpHandle = -1;
        SdpManager defaultManager = SdpManager.getDefaultManager();
        Log.d(TAG, "cleanUpSdpRecord, mSdpHandle=" + i);
        if (defaultManager == null || AdapterService.getAdapterService() == null) {
            Log.e(TAG, "sdpManager is null");
        } else if (!defaultManager.removeSdpRecord(i)) {
            Log.w(TAG, "cleanUpSdpRecord, removeSdpRecord failed, sdpHandle=" + i);
        }
    }

    private class PbapHandler extends Handler {
        private PbapHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (BluetoothPbapService.VERBOSE) {
                Log.v(BluetoothPbapService.TAG, "Handler(): got msg=" + message.what);
            }
            int i = message.what;
            switch (i) {
                case 1:
                    BluetoothPbapService.this.mServerSockets = ObexServerSockets.createWithFixedChannels(BluetoothPbapService.this, 19, SdpManager.PBAP_L2CAP_PSM);
                    if (BluetoothPbapService.this.mServerSockets != null) {
                        BluetoothPbapService.this.createSdpRecord();
                        BluetoothPbapUtils.fetchPbapParams(BluetoothPbapService.this.mContext);
                        return;
                    } else {
                        Log.w(BluetoothPbapService.TAG, "ObexServerSockets.create() returned null");
                        return;
                    }
                case 2:
                    Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL");
                    intent.setPackage(BluetoothPbapService.this.getString(R.string.pairing_ui_package));
                    PbapStateMachine pbapStateMachine = (PbapStateMachine) message.obj;
                    intent.putExtra("android.bluetooth.device.extra.DEVICE", pbapStateMachine.getRemoteDevice());
                    intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                    BluetoothPbapService.this.sendBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
                    pbapStateMachine.sendMessage(2);
                    return;
                case 3:
                    BluetoothPbapService.this.closeService();
                    return;
                case 4:
                    BluetoothPbapService.this.loadAllContacts();
                    return;
                case 5:
                    BluetoothPbapService.this.mContactsLoaded = true;
                    return;
                case 6:
                    BluetoothPbapService.this.updateSecondaryVersion();
                    return;
                case 7:
                    BluetoothPbapUtils.rolloverCounters();
                    return;
                default:
                    switch (i) {
                        case BluetoothPbapService.MSG_ACQUIRE_WAKE_LOCK:
                            if (BluetoothPbapService.this.mWakeLock == null) {
                                BluetoothPbapService.this.mWakeLock = ((PowerManager) BluetoothPbapService.this.getSystemService("power")).newWakeLock(1, "StartingObexPbapTransaction");
                                BluetoothPbapService.this.mWakeLock.setReferenceCounted(false);
                                BluetoothPbapService.this.mWakeLock.acquire();
                                Log.w(BluetoothPbapService.TAG, "Acquire Wake Lock");
                            }
                            BluetoothPbapService.this.mSessionStatusHandler.removeMessages(5005);
                            BluetoothPbapService.this.mSessionStatusHandler.sendMessageDelayed(BluetoothPbapService.this.mSessionStatusHandler.obtainMessage(5005), 10000L);
                            return;
                        case 5005:
                            if (BluetoothPbapService.this.mWakeLock != null) {
                                BluetoothPbapService.this.mWakeLock.release();
                                BluetoothPbapService.this.mWakeLock = null;
                                return;
                            }
                            return;
                        case 5006:
                            PbapStateMachine pbapStateMachine2 = (PbapStateMachine) message.obj;
                            BluetoothDevice remoteDevice = pbapStateMachine2.getRemoteDevice();
                            pbapStateMachine2.quitNow();
                            synchronized (BluetoothPbapService.this.mPbapStateMachineMap) {
                                BluetoothPbapService.this.mPbapStateMachineMap.remove(remoteDevice);
                                break;
                            }
                            return;
                        default:
                            return;
                    }
            }
        }
    }

    int getConnectionState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        if (this.mPbapStateMachineMap == null) {
            return 0;
        }
        synchronized (this.mPbapStateMachineMap) {
            PbapStateMachine pbapStateMachine = this.mPbapStateMachineMap.get(bluetoothDevice);
            if (pbapStateMachine == null) {
                return 0;
            }
            return pbapStateMachine.getConnectionState();
        }
    }

    List<BluetoothDevice> getConnectedDevices() {
        ArrayList arrayList;
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        if (this.mPbapStateMachineMap == null) {
            return new ArrayList();
        }
        synchronized (this.mPbapStateMachineMap) {
            arrayList = new ArrayList(this.mPbapStateMachineMap.keySet());
        }
        return arrayList;
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        ArrayList arrayList = new ArrayList();
        if (this.mPbapStateMachineMap == null || iArr == null) {
            return arrayList;
        }
        synchronized (this.mPbapStateMachineMap) {
            for (int i : iArr) {
                for (BluetoothDevice bluetoothDevice : this.mPbapStateMachineMap.keySet()) {
                    if (i == this.mPbapStateMachineMap.get(bluetoothDevice).getConnectionState()) {
                        arrayList.add(bluetoothDevice);
                    }
                }
            }
        }
        return arrayList;
    }

    void disconnect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        synchronized (this.mPbapStateMachineMap) {
            PbapStateMachine pbapStateMachine = this.mPbapStateMachineMap.get(bluetoothDevice);
            if (pbapStateMachine != null) {
                pbapStateMachine.sendMessage(3);
            }
        }
    }

    static String getLocalPhoneNum() {
        return sLocalPhoneNum;
    }

    static String getLocalPhoneName() {
        return sLocalPhoneName;
    }

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new PbapBinder(this);
    }

    @Override
    protected boolean start() {
        if (VERBOSE) {
            Log.v(TAG, "start()");
        }
        this.mContext = this;
        this.mContactsLoaded = false;
        this.mHandlerThread = new HandlerThread("PbapHandlerThread");
        this.mHandlerThread.start();
        this.mSessionStatusHandler = new PbapHandler(this.mHandlerThread.getLooper());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        intentFilter.addAction(AUTH_RESPONSE_ACTION);
        intentFilter.addAction(AUTH_CANCELLED_ACTION);
        BluetoothPbapConfig.init(this);
        registerReceiver(this.mPbapReceiver, intentFilter);
        try {
            this.mContactChangeObserver = new BluetoothPbapContentObserver();
            getContentResolver().registerContentObserver(DevicePolicyUtils.getEnterprisePhoneUri(this), false, this.mContactChangeObserver);
        } catch (SQLiteException e) {
            Log.e(TAG, "SQLite exception: " + e);
        } catch (IllegalStateException e2) {
            Log.e(TAG, "Illegal state exception, content observer is already registered");
        }
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
        if (telephonyManager != null) {
            sLocalPhoneNum = telephonyManager.getLine1Number();
            sLocalPhoneName = telephonyManager.getLine1AlphaTag();
            if (TextUtils.isEmpty(sLocalPhoneName)) {
                sLocalPhoneName = getString(R.string.localPhoneName);
            }
        }
        this.mSessionStatusHandler.sendMessage(this.mSessionStatusHandler.obtainMessage(4));
        this.mSessionStatusHandler.sendMessage(this.mSessionStatusHandler.obtainMessage(1));
        setBluetoothPbapService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        if (VERBOSE) {
            Log.v(TAG, "stop()");
        }
        setBluetoothPbapService(null);
        if (this.mSessionStatusHandler != null) {
            this.mSessionStatusHandler.obtainMessage(3).sendToTarget();
        }
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quitSafely();
        }
        this.mContactsLoaded = false;
        if (this.mContactChangeObserver == null) {
            Log.i(TAG, "Avoid unregister when receiver it is not registered");
            return true;
        }
        unregisterReceiver(this.mPbapReceiver);
        getContentResolver().unregisterContentObserver(this.mContactChangeObserver);
        this.mContactChangeObserver = null;
        return true;
    }

    @VisibleForTesting
    public static synchronized BluetoothPbapService getBluetoothPbapService() {
        if (sBluetoothPbapService == null) {
            Log.w(TAG, "getBluetoothPbapService(): service is null");
            return null;
        }
        if (!sBluetoothPbapService.isAvailable()) {
            Log.w(TAG, "getBluetoothPbapService(): service is not available");
            return null;
        }
        return sBluetoothPbapService;
    }

    private static synchronized void setBluetoothPbapService(BluetoothPbapService bluetoothPbapService) {
        Log.d(TAG, "setBluetoothPbapService(): set to: " + bluetoothPbapService);
        sBluetoothPbapService = bluetoothPbapService;
    }

    @Override
    protected void setCurrentUser(int i) {
        Log.i(TAG, "setCurrentUser(" + i + ")");
        if (((UserManager) getSystemService("user")).isUserUnlocked(i)) {
            setUserUnlocked(i);
        }
    }

    @Override
    protected void setUserUnlocked(int i) {
        Log.i(TAG, "setUserUnlocked(" + i + ")");
        sendUpdateRequest();
    }

    private static class PbapBinder extends IBluetoothPbap.Stub implements ProfileService.IProfileServiceBinder {
        private BluetoothPbapService mService;

        private BluetoothPbapService getService() {
            if (!Utils.checkCaller()) {
                Log.w(BluetoothPbapService.TAG, "not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        PbapBinder(BluetoothPbapService bluetoothPbapService) {
            if (BluetoothPbapService.VERBOSE) {
                Log.v(BluetoothPbapService.TAG, "PbapBinder()");
            }
            this.mService = bluetoothPbapService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        public List<BluetoothDevice> getConnectedDevices() {
            Log.d(BluetoothPbapService.TAG, "getConnectedDevices");
            BluetoothPbapService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            Log.d(BluetoothPbapService.TAG, "getDevicesMatchingConnectionStates");
            BluetoothPbapService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            Log.d(BluetoothPbapService.TAG, "getConnectionState: " + bluetoothDevice);
            BluetoothPbapService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public void disconnect(BluetoothDevice bluetoothDevice) {
            Log.d(BluetoothPbapService.TAG, "disconnect");
            BluetoothPbapService service = getService();
            if (service == null) {
                return;
            }
            service.disconnect(bluetoothDevice);
        }
    }

    @Override
    public boolean onConnect(BluetoothDevice bluetoothDevice, BluetoothSocket bluetoothSocket) {
        if (bluetoothDevice == null || bluetoothSocket == null) {
            Log.e(TAG, "onConnect(): Unexpected null. remoteDevice=" + bluetoothDevice + " socket=" + bluetoothSocket);
            return false;
        }
        PbapStateMachine pbapStateMachineMake = PbapStateMachine.make(this, this.mHandlerThread.getLooper(), bluetoothDevice, bluetoothSocket, this, this.mSessionStatusHandler, this.mNextNotificationId);
        this.mNextNotificationId++;
        if (this.mNextNotificationId == PBAP_NOTIFICATION_ID_END) {
            this.mNextNotificationId = PBAP_NOTIFICATION_ID_START;
        }
        synchronized (this.mPbapStateMachineMap) {
            this.mPbapStateMachineMap.put(bluetoothDevice, pbapStateMachineMake);
        }
        pbapStateMachineMake.sendMessage(4);
        return true;
    }

    @VisibleForTesting(otherwise = 3)
    public void checkOrGetPhonebookPermission(PbapStateMachine pbapStateMachine) {
        BluetoothDevice remoteDevice = pbapStateMachine.getRemoteDevice();
        int phonebookAccessPermission = remoteDevice.getPhonebookAccessPermission();
        Log.d(TAG, "getPhonebookAccessPermission() = " + phonebookAccessPermission);
        if (phonebookAccessPermission == 1) {
            pbapStateMachine.sendMessage(1);
            return;
        }
        if (phonebookAccessPermission == 2) {
            pbapStateMachine.sendMessage(2);
            return;
        }
        Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST");
        intent.setClassName(ACCESS_AUTHORITY_PACKAGE, ACCESS_AUTHORITY_CLASS);
        intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", remoteDevice);
        intent.putExtra("android.bluetooth.device.extra.PACKAGE_NAME", getPackageName());
        sendOrderedBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
        if (VERBOSE) {
            Log.v(TAG, "waiting for authorization for connection from: " + remoteDevice);
        }
        this.mSessionStatusHandler.sendMessageDelayed(this.mSessionStatusHandler.obtainMessage(2, pbapStateMachine), 30000L);
    }

    @Override
    public synchronized void onAcceptFailed() {
        Log.w(TAG, "PBAP server socket accept thread failed. Restarting the server socket");
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
        cleanUpServerSocket();
        if (this.mSessionStatusHandler != null) {
            this.mSessionStatusHandler.removeCallbacksAndMessages(null);
        }
        synchronized (this.mPbapStateMachineMap) {
            this.mPbapStateMachineMap.clear();
        }
        this.mSessionStatusHandler.sendMessage(this.mSessionStatusHandler.obtainMessage(1));
    }

    private void loadAllContacts() {
        if (this.mThreadLoadContacts == null) {
            this.mThreadLoadContacts = new Thread(new Runnable() {
                @Override
                public void run() {
                    BluetoothPbapUtils.loadAllContacts(BluetoothPbapService.this.mContext, BluetoothPbapService.this.mSessionStatusHandler);
                    BluetoothPbapService.this.mThreadLoadContacts = null;
                }
            });
            this.mThreadLoadContacts.start();
        }
    }

    private void updateSecondaryVersion() {
        if (this.mThreadUpdateSecVersionCounter == null) {
            this.mThreadUpdateSecVersionCounter = new Thread(new Runnable() {
                @Override
                public void run() {
                    BluetoothPbapUtils.updateSecondaryVersionCounter(BluetoothPbapService.this.mContext, BluetoothPbapService.this.mSessionStatusHandler);
                    BluetoothPbapService.this.mThreadUpdateSecVersionCounter = null;
                }
            });
            this.mThreadUpdateSecVersionCounter.start();
        }
    }
}
