package com.android.bluetooth.pbap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothPbap;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.vcard.VCardConfig;
import java.io.IOException;
import javax.obex.ServerSession;

public class BluetoothPbapService extends Service {
    private static final String ACCESS_AUTHORITY_CLASS = "com.android.settings.bluetooth.BluetoothPermissionRequest";
    private static final String ACCESS_AUTHORITY_PACKAGE = "com.android.settings";
    public static final String AUTH_CANCELLED_ACTION = "com.android.bluetooth.pbap.authcancelled";
    public static final String AUTH_CHALL_ACTION = "com.android.bluetooth.pbap.authchall";
    public static final String AUTH_RESPONSE_ACTION = "com.android.bluetooth.pbap.authresponse";
    private static final int AUTH_TIMEOUT = 3;
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    private static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    public static final boolean DEBUG = true;
    public static final String EXTRA_SESSION_KEY = "com.android.bluetooth.pbap.sessionkey";
    public static final int MSG_ACQUIRE_WAKE_LOCK = 5004;
    public static final int MSG_OBEX_AUTH_CHALL = 5003;
    public static final int MSG_RELEASE_WAKE_LOCK = 5005;
    public static final int MSG_SERVERSESSION_CLOSE = 5000;
    public static final int MSG_SESSION_DISCONNECTED = 5002;
    public static final int MSG_SESSION_ESTABLISHED = 5001;
    private static final int NOTIFICATION_ID_ACCESS = -1000001;
    private static final int NOTIFICATION_ID_AUTH = -1000002;
    private static final int RELEASE_WAKE_LOCK_DELAY = 10000;
    private static final int START_LISTENER = 1;
    private static final String TAG = "BluetoothPbapService";
    public static final String THIS_PACKAGE_NAME = "com.android.bluetooth";
    public static final String USER_CONFIRM_TIMEOUT_ACTION = "com.android.bluetooth.pbap.userconfirmtimeout";
    private static final int USER_CONFIRM_TIMEOUT_VALUE = 30000;
    private static final int USER_TIMEOUT = 2;
    public static final boolean VERBOSE = false;
    private BluetoothAdapter mAdapter;
    private volatile boolean mInterrupted;
    private BluetoothPbapObexServer mPbapServer;
    private static String sLocalPhoneNum = null;
    private static String sLocalPhoneName = null;
    private static String sRemoteDeviceName = null;
    private PowerManager.WakeLock mWakeLock = null;
    private SocketAcceptThread mAcceptThread = null;
    private BluetoothPbapAuthenticator mAuth = null;
    private ServerSession mServerSession = null;
    private BluetoothServerSocket mServerSocket = null;
    private BluetoothSocket mConnSocket = null;
    private BluetoothDevice mRemoteDevice = null;
    private boolean mHasStarted = false;
    private int mStartId = -1;
    private boolean mIsWaitingAuthorization = false;
    private final Handler mSessionStatusHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (BluetoothPbapService.this.mAdapter.isEnabled()) {
                        BluetoothPbapService.this.startRfcommSocketListener();
                    } else {
                        BluetoothPbapService.this.closeService();
                    }
                    break;
                case 2:
                    Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL");
                    intent.putExtra("android.bluetooth.device.extra.DEVICE", BluetoothPbapService.this.mRemoteDevice);
                    intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                    BluetoothPbapService.this.sendBroadcast(intent);
                    BluetoothPbapService.this.mIsWaitingAuthorization = false;
                    BluetoothPbapService.this.stopObexServerSession();
                    break;
                case 3:
                    Intent i = new Intent(BluetoothPbapService.USER_CONFIRM_TIMEOUT_ACTION);
                    BluetoothPbapService.this.sendBroadcast(i);
                    BluetoothPbapService.this.removePbapNotification(BluetoothPbapService.NOTIFICATION_ID_AUTH);
                    BluetoothPbapService.this.notifyAuthCancelled();
                    break;
                case 5000:
                    BluetoothPbapService.this.stopObexServerSession();
                    break;
                case 5003:
                    BluetoothPbapService.this.createPbapNotification(BluetoothPbapService.AUTH_CHALL_ACTION);
                    BluetoothPbapService.this.mSessionStatusHandler.sendMessageDelayed(BluetoothPbapService.this.mSessionStatusHandler.obtainMessage(3), 30000L);
                    break;
                case 5004:
                    if (BluetoothPbapService.this.mWakeLock == null) {
                        PowerManager pm = (PowerManager) BluetoothPbapService.this.getSystemService("power");
                        BluetoothPbapService.this.mWakeLock = pm.newWakeLock(1, "StartingObexPbapTransaction");
                        BluetoothPbapService.this.mWakeLock.setReferenceCounted(false);
                        BluetoothPbapService.this.mWakeLock.acquire();
                        Log.w(BluetoothPbapService.TAG, "Acquire Wake Lock");
                    }
                    BluetoothPbapService.this.mSessionStatusHandler.removeMessages(5005);
                    BluetoothPbapService.this.mSessionStatusHandler.sendMessageDelayed(BluetoothPbapService.this.mSessionStatusHandler.obtainMessage(5005), 10000L);
                    break;
                case 5005:
                    if (BluetoothPbapService.this.mWakeLock != null) {
                        BluetoothPbapService.this.mWakeLock.release();
                        BluetoothPbapService.this.mWakeLock = null;
                        Log.w(BluetoothPbapService.TAG, "Release Wake Lock");
                    }
                    break;
            }
        }
    };
    private final IBluetoothPbap.Stub mBinder = new IBluetoothPbap.Stub() {
        public int getState() {
            Log.d(BluetoothPbapService.TAG, "getState " + BluetoothPbapService.this.mState);
            if (!Utils.checkCaller()) {
                Log.w(BluetoothPbapService.TAG, "getState(): not allowed for non-active user");
                return 0;
            }
            BluetoothPbapService.this.enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
            return BluetoothPbapService.this.mState;
        }

        public BluetoothDevice getClient() {
            Log.d(BluetoothPbapService.TAG, "getClient" + BluetoothPbapService.this.mRemoteDevice);
            if (!Utils.checkCaller()) {
                Log.w(BluetoothPbapService.TAG, "getClient(): not allowed for non-active user");
                return null;
            }
            BluetoothPbapService.this.enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
            if (BluetoothPbapService.this.mState != 0) {
                return BluetoothPbapService.this.mRemoteDevice;
            }
            return null;
        }

        public boolean isConnected(BluetoothDevice device) {
            if (!Utils.checkCaller()) {
                Log.w(BluetoothPbapService.TAG, "isConnected(): not allowed for non-active user");
                return false;
            }
            BluetoothPbapService.this.enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
            return BluetoothPbapService.this.mState == 2 && BluetoothPbapService.this.mRemoteDevice.equals(device);
        }

        public boolean connect(BluetoothDevice device) {
            if (!Utils.checkCaller()) {
                Log.w(BluetoothPbapService.TAG, "connect(): not allowed for non-active user");
            } else {
                BluetoothPbapService.this.enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
            }
            return false;
        }

        public void disconnect() {
            Log.d(BluetoothPbapService.TAG, "disconnect");
            if (!Utils.checkCaller()) {
                Log.w(BluetoothPbapService.TAG, "disconnect(): not allowed for non-active user");
                return;
            }
            BluetoothPbapService.this.enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
            synchronized (BluetoothPbapService.this) {
                switch (BluetoothPbapService.this.mState) {
                    case 2:
                        if (BluetoothPbapService.this.mServerSession != null) {
                            BluetoothPbapService.this.mServerSession.close();
                            BluetoothPbapService.this.mServerSession = null;
                        }
                        BluetoothPbapService.this.closeConnectionSocket();
                        BluetoothPbapService.this.setState(0, 2);
                        break;
                }
            }
        }
    };
    private int mState = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mInterrupted = false;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!this.mHasStarted) {
            this.mHasStarted = true;
            BluetoothPbapConfig.init(this);
            int state = this.mAdapter.getState();
            if (state == 12) {
                this.mSessionStatusHandler.sendMessage(this.mSessionStatusHandler.obtainMessage(1));
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.mStartId = startId;
        if (this.mAdapter == null) {
            Log.w(TAG, "Stopping BluetoothPbapService: device does not have BT or device is not ready");
            closeService();
            return 2;
        }
        if (intent != null) {
            parseIntent(intent);
            return 2;
        }
        return 2;
    }

    private void parseIntent(Intent intent) {
        String action = intent.getStringExtra(AdapterService.EXTRA_ACTION);
        int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
        boolean removeTimeoutMsg = true;
        if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
            if (state == 13) {
                if (this.mSessionStatusHandler.hasMessages(2)) {
                    Intent timeoutIntent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL");
                    timeoutIntent.setClassName(ACCESS_AUTHORITY_PACKAGE, ACCESS_AUTHORITY_CLASS);
                    timeoutIntent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                    sendBroadcast(timeoutIntent, "android.permission.BLUETOOTH_ADMIN");
                }
                closeService();
            } else {
                removeTimeoutMsg = false;
            }
        } else if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED") && this.mIsWaitingAuthorization) {
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            if (this.mRemoteDevice == null || device == null) {
                Log.e(TAG, "Unexpected error!");
                return;
            }
            Log.d(TAG, "ACL disconnected for " + device);
            if (this.mRemoteDevice.equals(device)) {
                Intent cancelIntent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL");
                cancelIntent.putExtra("android.bluetooth.device.extra.DEVICE", device);
                cancelIntent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                sendBroadcast(cancelIntent);
                this.mIsWaitingAuthorization = false;
                stopObexServerSession();
            }
        } else if (action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY")) {
            int requestType = intent.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
            if (this.mIsWaitingAuthorization && requestType == 2) {
                this.mIsWaitingAuthorization = false;
                if (intent.getIntExtra("android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT", 2) == 1) {
                    if (intent.getBooleanExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", false)) {
                        this.mRemoteDevice.setPhonebookAccessPermission(1);
                    }
                    try {
                        if (this.mConnSocket != null) {
                            startObexServerSession();
                        } else {
                            stopObexServerSession();
                        }
                    } catch (IOException ex) {
                        Log.e(TAG, "Caught the error: " + ex.toString());
                    }
                } else {
                    if (intent.getBooleanExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", false)) {
                        this.mRemoteDevice.setPhonebookAccessPermission(2);
                    }
                    stopObexServerSession();
                }
            } else {
                return;
            }
        } else if (action.equals(AUTH_RESPONSE_ACTION)) {
            String sessionkey = intent.getStringExtra(EXTRA_SESSION_KEY);
            notifyAuthKeyInput(sessionkey);
        } else if (action.equals(AUTH_CANCELLED_ACTION)) {
            notifyAuthCancelled();
        } else {
            removeTimeoutMsg = false;
        }
        if (removeTimeoutMsg) {
            this.mSessionStatusHandler.removeMessages(2);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setState(0, 2);
        closeService();
        if (this.mSessionStatusHandler != null) {
            this.mSessionStatusHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    private void startRfcommSocketListener() {
        if (this.mAcceptThread == null) {
            this.mAcceptThread = new SocketAcceptThread();
            this.mAcceptThread.setName("BluetoothPbapAcceptThread");
            this.mAcceptThread.start();
        }
    }

    private final boolean initSocket() {
        boolean initSocketOK = false;
        int i = 0;
        while (true) {
            if (i < 10 && !this.mInterrupted) {
                initSocketOK = true;
                try {
                    this.mServerSocket = this.mAdapter.listenUsingEncryptedRfcommWithServiceRecord("OBEX Phonebook Access Server", BluetoothUuid.PBAP_PSE.getUuid());
                } catch (IOException e) {
                    Log.e(TAG, "Error create RfcommServerSocket " + e.toString());
                    initSocketOK = false;
                }
                if (!initSocketOK && this.mAdapter != null) {
                    int state = this.mAdapter.getState();
                    if (state != 11 && state != 12) {
                        Log.w(TAG, "initServerSocket failed as BT is (being) turned off");
                        break;
                    }
                    try {
                        Thread.sleep(300L);
                        i++;
                    } catch (InterruptedException e2) {
                        Log.e(TAG, "socketAcceptThread thread was interrupted (3)");
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        if (this.mInterrupted) {
            initSocketOK = false;
            closeServerSocket();
        }
        if (!initSocketOK) {
            Log.e(TAG, "Error to create listening socket after 10 try");
        }
        return initSocketOK;
    }

    private final synchronized void closeServerSocket() {
        if (this.mServerSocket != null) {
            try {
                this.mServerSocket.close();
                this.mServerSocket = null;
            } catch (IOException ex) {
                Log.e(TAG, "Close Server Socket error: " + ex);
            }
        }
    }

    private final synchronized void closeConnectionSocket() {
        if (this.mConnSocket != null) {
            try {
                this.mConnSocket.close();
                this.mConnSocket = null;
            } catch (IOException e) {
                Log.e(TAG, "Close Connection Socket error: " + e.toString());
            }
        }
    }

    private final void closeService() {
        this.mInterrupted = true;
        closeServerSocket();
        if (this.mAcceptThread != null) {
            try {
                this.mAcceptThread.shutdown();
                this.mAcceptThread.join();
                this.mAcceptThread = null;
            } catch (InterruptedException ex) {
                Log.w(TAG, "mAcceptThread close error" + ex);
            }
        }
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
        if (this.mServerSession != null) {
            this.mServerSession.close();
            this.mServerSession = null;
        }
        closeConnectionSocket();
        this.mHasStarted = false;
        if (this.mStartId != -1 && stopSelfResult(this.mStartId)) {
            this.mStartId = -1;
        }
    }

    private final void startObexServerSession() throws IOException {
        if (this.mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService("power");
            this.mWakeLock = pm.newWakeLock(1, "StartingObexPbapTransaction");
            this.mWakeLock.setReferenceCounted(false);
            this.mWakeLock.acquire();
        }
        TelephonyManager tm = (TelephonyManager) getSystemService("phone");
        if (tm != null) {
            sLocalPhoneNum = tm.getLine1Number();
            sLocalPhoneName = tm.getLine1AlphaTag();
            if (TextUtils.isEmpty(sLocalPhoneName)) {
                sLocalPhoneName = getString(R.string.localPhoneName);
            }
        }
        this.mPbapServer = new BluetoothPbapObexServer(this.mSessionStatusHandler, this);
        synchronized (this) {
            this.mAuth = new BluetoothPbapAuthenticator(this.mSessionStatusHandler);
            this.mAuth.setChallenged(false);
            this.mAuth.setCancelled(false);
        }
        BluetoothPbapRfcommTransport transport = new BluetoothPbapRfcommTransport(this.mConnSocket);
        this.mServerSession = new ServerSession(transport, this.mPbapServer, this.mAuth);
        setState(2);
        this.mSessionStatusHandler.removeMessages(5005);
        this.mSessionStatusHandler.sendMessageDelayed(this.mSessionStatusHandler.obtainMessage(5005), 10000L);
    }

    private void stopObexServerSession() {
        this.mSessionStatusHandler.removeMessages(5004);
        this.mSessionStatusHandler.removeMessages(5005);
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
        if (this.mServerSession != null) {
            this.mServerSession.close();
            this.mServerSession = null;
        }
        this.mAcceptThread = null;
        closeConnectionSocket();
        if (this.mAdapter.isEnabled()) {
            startRfcommSocketListener();
        }
        setState(0);
    }

    private void notifyAuthKeyInput(String key) {
        synchronized (this.mAuth) {
            if (key != null) {
                this.mAuth.setSessionKey(key);
                this.mAuth.setChallenged(true);
                this.mAuth.notify();
            } else {
                this.mAuth.setChallenged(true);
                this.mAuth.notify();
            }
        }
    }

    private void notifyAuthCancelled() {
        synchronized (this.mAuth) {
            this.mAuth.setCancelled(true);
            this.mAuth.notify();
        }
    }

    private class SocketAcceptThread extends Thread {
        private boolean stopped;

        private SocketAcceptThread() {
            this.stopped = false;
        }

        @Override
        public void run() {
            BluetoothServerSocket serverSocket;
            if (BluetoothPbapService.this.mServerSocket != null || BluetoothPbapService.this.initSocket()) {
                while (!this.stopped) {
                    try {
                        serverSocket = BluetoothPbapService.this.mServerSocket;
                    } catch (IOException e) {
                        this.stopped = true;
                    }
                    if (serverSocket == null) {
                        Log.w(BluetoothPbapService.TAG, "mServerSocket is null");
                        return;
                    }
                    BluetoothPbapService.this.mConnSocket = serverSocket.accept();
                    synchronized (BluetoothPbapService.this) {
                        if (BluetoothPbapService.this.mConnSocket == null) {
                            Log.w(BluetoothPbapService.TAG, "mConnSocket is null");
                            return;
                        }
                        BluetoothPbapService.this.mRemoteDevice = BluetoothPbapService.this.mConnSocket.getRemoteDevice();
                        if (BluetoothPbapService.this.mRemoteDevice != null) {
                            String unused = BluetoothPbapService.sRemoteDeviceName = BluetoothPbapService.this.mRemoteDevice.getName();
                            if (TextUtils.isEmpty(BluetoothPbapService.sRemoteDeviceName)) {
                                String unused2 = BluetoothPbapService.sRemoteDeviceName = BluetoothPbapService.this.getString(R.string.defaultname);
                            }
                            int permission = BluetoothPbapService.this.mRemoteDevice.getPhonebookAccessPermission();
                            if (permission == 1) {
                                try {
                                    BluetoothPbapService.this.startObexServerSession();
                                } catch (IOException ex) {
                                    Log.e(BluetoothPbapService.TAG, "Caught exception starting obex server session" + ex.toString());
                                }
                            } else if (permission == 2) {
                                BluetoothPbapService.this.stopObexServerSession();
                            } else {
                                Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST");
                                intent.setClassName(BluetoothPbapService.ACCESS_AUTHORITY_PACKAGE, BluetoothPbapService.ACCESS_AUTHORITY_CLASS);
                                intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                                intent.putExtra("android.bluetooth.device.extra.DEVICE", BluetoothPbapService.this.mRemoteDevice);
                                intent.putExtra("android.bluetooth.device.extra.PACKAGE_NAME", BluetoothPbapService.this.getPackageName());
                                intent.putExtra("android.bluetooth.device.extra.CLASS_NAME", BluetoothPbapReceiver.class.getName());
                                BluetoothPbapService.this.mIsWaitingAuthorization = true;
                                BluetoothPbapService.this.sendOrderedBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
                                BluetoothPbapService.this.mSessionStatusHandler.sendMessageDelayed(BluetoothPbapService.this.mSessionStatusHandler.obtainMessage(2), 30000L);
                            }
                            this.stopped = true;
                        } else {
                            Log.i(BluetoothPbapService.TAG, "getRemoteDevice() = null");
                            return;
                        }
                        this.stopped = true;
                    }
                }
            }
        }

        void shutdown() {
            this.stopped = true;
            interrupt();
        }
    }

    private void setState(int state) {
        setState(state, 1);
    }

    private synchronized void setState(int state, int result) {
        if (state != this.mState) {
            Log.d(TAG, "Pbap state " + this.mState + " -> " + state + ", result = " + result);
            int prevState = this.mState;
            this.mState = state;
            Intent intent = new Intent("android.bluetooth.pbap.intent.action.PBAP_STATE_CHANGED");
            intent.putExtra("android.bluetooth.pbap.intent.PBAP_PREVIOUS_STATE", prevState);
            intent.putExtra("android.bluetooth.pbap.intent.PBAP_STATE", this.mState);
            intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mRemoteDevice);
            sendBroadcast(intent, "android.permission.BLUETOOTH");
            AdapterService s = AdapterService.getAdapterService();
            if (s != null) {
                s.onProfileConnectionStateChanged(this.mRemoteDevice, 6, this.mState, prevState);
            }
        }
    }

    private void createPbapNotification(String action) {
        NotificationManager nm = (NotificationManager) getSystemService("notification");
        Intent clickIntent = new Intent();
        clickIntent.setClass(this, BluetoothPbapActivity.class);
        clickIntent.addFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
        clickIntent.setAction(action);
        Intent deleteIntent = new Intent();
        deleteIntent.setClass(this, BluetoothPbapReceiver.class);
        String name = getRemoteDeviceName();
        if (action.equals(AUTH_CHALL_ACTION)) {
            deleteIntent.setAction(AUTH_CANCELLED_ACTION);
            Notification notification = new Notification(android.R.drawable.stat_sys_data_bluetooth, getString(R.string.auth_notif_ticker), System.currentTimeMillis());
            notification.color = getResources().getColor(android.R.color.system_accent3_600);
            notification.setLatestEventInfo(this, getString(R.string.auth_notif_title), getString(R.string.auth_notif_message, new Object[]{name}), PendingIntent.getActivity(this, 0, clickIntent, 0));
            notification.flags |= 16;
            notification.flags |= 8;
            notification.defaults = 1;
            notification.deleteIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, 0);
            nm.notify(NOTIFICATION_ID_AUTH, notification);
        }
    }

    private void removePbapNotification(int id) {
        NotificationManager nm = (NotificationManager) getSystemService("notification");
        nm.cancel(id);
    }

    public static String getLocalPhoneNum() {
        return sLocalPhoneNum;
    }

    public static String getLocalPhoneName() {
        return sLocalPhoneName;
    }

    public static String getRemoteDeviceName() {
        return sRemoteDeviceName;
    }
}
