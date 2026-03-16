package com.android.bluetooth.ftp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.btservice.AdapterService;
import com.android.vcard.VCardConfig;
import java.io.IOException;
import javax.obex.ServerSession;

public class BluetoothFtpService extends Service {
    public static final String ACCESS_ALLOWED_ACTION = "com.android.bluetooth.ftp.accessallowed";
    public static final String ACCESS_DISALLOWED_ACTION = "com.android.bluetooth.ftp.accessdisallowed";
    public static final String ACCESS_REQUEST_ACTION = "com.android.bluetooth.ftp.accessrequest";
    public static final String AUTH_CANCELLED_ACTION = "com.android.bluetooth.ftp.authcancelled";
    public static final String AUTH_CHALL_ACTION = "com.android.bluetooth.ftp.authchall";
    public static final String AUTH_RESPONSE_ACTION = "com.android.bluetooth.ftp.authresponse";
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    private static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    public static final boolean DEBUG = true;
    public static final String EXTRA_ALWAYS_ALLOWED = "com.android.bluetooth.ftp.alwaysallowed";
    public static final String EXTRA_SESSION_KEY = "com.android.bluetooth.ftp.sessionkey";
    private static final int MSG_INTERNAL_AUTH_TIMEOUT = 3;
    private static final int MSG_INTERNAL_START_LISTENER = 1;
    private static final int MSG_INTERNAL_USER_TIMEOUT = 2;
    public static final int MSG_OBEX_AUTH_CHALL = 5007;
    public static final int MSG_SERVERSESSION_CLOSE = 5004;
    public static final int MSG_SESSION_DISCONNECTED = 5006;
    public static final int MSG_SESSION_ESTABLISHED = 5005;
    private static final int NOTIFICATION_ID_ACCESS = -1000005;
    private static final int NOTIFICATION_ID_AUTH = -1000006;
    private static final int PORT_NUM = 20;
    private static final String TAG = "BluetoothFtpService";
    public static final String THIS_PACKAGE_NAME = "com.android.bluetooth";
    public static final String USER_CONFIRM_TIMEOUT_ACTION = "com.android.bluetooth.ftp.userconfirmtimeout";
    private static final int USER_CONFIRM_TIMEOUT_VALUE = 30000;
    public static final boolean VERBOSE = true;
    private static String sRemoteDeviceName = null;
    private BluetoothAdapter mAdapter;
    private volatile boolean mInterrupted;
    private int mState;
    private PowerManager.WakeLock mWakeLock;
    private SocketAcceptThread mAcceptThread = null;
    private BluetoothFtpAuthenticator mAuth = null;
    private BluetoothServerSocket mServerSocket = null;
    private BluetoothSocket mConnSocket = null;
    private BluetoothDevice mRemoteDevice = null;
    private boolean mHasStarted = false;
    private int mStartId = -1;
    private BluetoothFtpObexServer mFtpServer = null;
    private ServerSession mServerSession = null;
    private final Handler mSessionStatusHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.v(BluetoothFtpService.TAG, "Handler(): got msg=" + msg.what);
            switch (msg.what) {
                case 1:
                    if (BluetoothFtpService.this.mAdapter.isEnabled()) {
                        BluetoothFtpService.this.startRfcommSocketListener();
                    } else {
                        BluetoothFtpService.this.closeService();
                    }
                    break;
                case 2:
                    Intent intent = new Intent(BluetoothFtpService.USER_CONFIRM_TIMEOUT_ACTION);
                    BluetoothFtpService.this.sendBroadcast(intent);
                    BluetoothFtpService.this.removeFtpNotification(BluetoothFtpService.NOTIFICATION_ID_ACCESS);
                    BluetoothFtpService.this.stopObexServerSession();
                    break;
                case 3:
                    Intent i = new Intent(BluetoothFtpService.USER_CONFIRM_TIMEOUT_ACTION);
                    BluetoothFtpService.this.sendBroadcast(i);
                    BluetoothFtpService.this.removeFtpNotification(BluetoothFtpService.NOTIFICATION_ID_AUTH);
                    BluetoothFtpService.this.notifyAuthCancelled();
                    BluetoothFtpService.this.stopObexServerSession();
                    break;
                case 5004:
                    BluetoothFtpService.this.stopObexServerSession();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Ftp Service onCreate");
        Log.i(TAG, "Ftp Service onCreate");
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!this.mHasStarted) {
            this.mHasStarted = true;
            Log.v(TAG, "Starting FTP service");
            int state = this.mAdapter.getState();
            if (state == 12) {
                this.mSessionStatusHandler.sendMessage(this.mSessionStatusHandler.obtainMessage(1));
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Ftp Service onStartCommand");
        int retCode = super.onStartCommand(intent, flags, startId);
        if (retCode == 1) {
            this.mStartId = startId;
            if (this.mAdapter == null) {
                Log.w(TAG, "Stopping BluetoothFtpService: device does not have BT or device is not ready");
                closeService();
            } else if (intent != null) {
                parseIntent(intent);
            }
        }
        return retCode;
    }

    private void parseIntent(Intent intent) {
        String action = intent.getStringExtra(AdapterService.EXTRA_ACTION);
        Log.v(TAG, "action: " + action);
        int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
        boolean removeTimeoutMsg = true;
        if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
            removeTimeoutMsg = false;
            if (state == 13) {
                closeService();
            }
        } else if (action.equals(ACCESS_ALLOWED_ACTION)) {
            try {
                if (this.mConnSocket != null) {
                    startObexServerSession();
                } else {
                    stopObexServerSession();
                }
            } catch (IOException ex) {
                Log.e(TAG, "Caught the error: " + ex.toString());
            }
        } else if (action.equals(ACCESS_DISALLOWED_ACTION)) {
            stopObexServerSession();
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
        Log.v(TAG, "Ftp Service onDestroy");
        super.onDestroy();
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
        closeService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Ftp Service onBind");
        return null;
    }

    private void startRfcommSocketListener() {
        Log.v(TAG, "Ftp Service startRfcommSocketListener");
        if (this.mServerSocket == null && !initSocket()) {
            closeService();
        } else if (this.mAcceptThread == null) {
            this.mAcceptThread = new SocketAcceptThread();
            this.mAcceptThread.setName("BluetoothFtpAcceptThread");
            this.mAcceptThread.start();
        }
    }

    private final boolean initSocket() {
        Log.v(TAG, "Ftp Service initSocket");
        boolean initSocketOK = true;
        for (int i = 0; i < 10 && !this.mInterrupted; i++) {
            try {
                this.mServerSocket = this.mAdapter.listenUsingInsecureRfcommWithServiceRecord("Obex File Transfer(FTP)", BluetoothUuid.FTP.getUuid());
            } catch (IOException e) {
                Log.e(TAG, "Error create RfcommServerSocket " + e.toString());
                initSocketOK = false;
            }
            if (initSocketOK) {
                break;
            }
            synchronized (this) {
                try {
                    Log.v(TAG, "wait 300 ms");
                    Thread.sleep(300L);
                } catch (InterruptedException e2) {
                    Log.e(TAG, "socketAcceptThread thread was interrupted (3)");
                    this.mInterrupted = true;
                }
            }
        }
        if (initSocketOK) {
            Log.v(TAG, "Succeed to create listening socket on channel 20");
        } else {
            Log.e(TAG, "Error to create listening socket after 10 try");
        }
        return initSocketOK;
    }

    private final void closeSocket(boolean server, boolean accept) throws IOException {
        if (server) {
            this.mInterrupted = true;
            if (this.mServerSocket != null) {
                this.mServerSocket.close();
            }
        }
        if (accept && this.mConnSocket != null) {
            this.mConnSocket.close();
        }
    }

    private final void closeService() {
        Log.v(TAG, "Ftp Service closeService");
        if (this.mAcceptThread != null) {
            this.mAcceptThread.shutdown();
        }
        try {
            closeSocket(true, true);
        } catch (IOException ex) {
            Log.e(TAG, "CloseSocket error: " + ex);
        }
        if (this.mAcceptThread != null) {
            try {
                this.mAcceptThread.join();
                this.mAcceptThread = null;
            } catch (InterruptedException ex2) {
                Log.w(TAG, "mAcceptThread close error" + ex2);
            }
        }
        this.mServerSocket = null;
        this.mConnSocket = null;
        if (this.mServerSession != null) {
            this.mServerSession.close();
            this.mServerSession = null;
        }
        this.mHasStarted = false;
        if (stopSelfResult(this.mStartId)) {
            Log.v(TAG, "successfully stopped ftp service");
        }
    }

    private final void startObexServerSession() throws IOException {
        Log.v(TAG, "Ftp Service startObexServerSession");
        if (this.mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService("power");
            this.mWakeLock = pm.newWakeLock(1, "StartingObexFtpTransaction");
            this.mWakeLock.setReferenceCounted(false);
        }
        if (!this.mWakeLock.isHeld()) {
            Log.e(TAG, "Acquire partial wake lock");
            this.mWakeLock.acquire();
        }
        this.mFtpServer = new BluetoothFtpObexServer(this.mSessionStatusHandler, this);
        synchronized (this) {
            this.mAuth = new BluetoothFtpAuthenticator(this.mSessionStatusHandler);
            this.mAuth.setChallenged(false);
            this.mAuth.setCancelled(false);
        }
        BluetoothFtpRfcommTransport transport = new BluetoothFtpRfcommTransport(this.mConnSocket);
        this.mServerSession = new ServerSession(transport, this.mFtpServer, this.mAuth);
        Log.v(TAG, "startObexServerSession() success!");
    }

    private void stopObexServerSession() {
        Log.v(TAG, "Ftp Service stopObexServerSession");
        if (this.mWakeLock != null) {
            if (this.mWakeLock.isHeld()) {
                Log.e(TAG, "Release full wake lock");
                this.mWakeLock.release();
                this.mWakeLock = null;
            } else {
                this.mWakeLock = null;
            }
        }
        if (this.mServerSession != null) {
            this.mServerSession.close();
            this.mServerSession = null;
        }
        this.mAcceptThread = null;
        try {
            closeSocket(false, true);
            this.mConnSocket = null;
        } catch (IOException e) {
            Log.e(TAG, "closeSocket error: " + e.toString());
        }
        if (this.mAdapter.isEnabled()) {
            startRfcommSocketListener();
        }
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
            while (!this.stopped) {
                try {
                    BluetoothFtpService.this.mConnSocket = BluetoothFtpService.this.mServerSocket.accept();
                    BluetoothFtpService.this.mRemoteDevice = BluetoothFtpService.this.mConnSocket.getRemoteDevice();
                    if (BluetoothFtpService.this.mRemoteDevice != null) {
                        String unused = BluetoothFtpService.sRemoteDeviceName = BluetoothFtpService.this.mRemoteDevice.getName();
                        if (TextUtils.isEmpty(BluetoothFtpService.sRemoteDeviceName)) {
                            String unused2 = BluetoothFtpService.sRemoteDeviceName = BluetoothFtpService.this.getString(R.string.defaultname);
                        }
                        BluetoothFtpService.this.createFtpNotification(BluetoothFtpService.ACCESS_REQUEST_ACTION);
                        Log.i(BluetoothFtpService.TAG, "incomming connection accepted from: " + BluetoothFtpService.sRemoteDeviceName);
                        BluetoothFtpService.this.mSessionStatusHandler.sendMessageDelayed(BluetoothFtpService.this.mSessionStatusHandler.obtainMessage(2), 30000L);
                        this.stopped = true;
                    } else {
                        Log.i(BluetoothFtpService.TAG, "getRemoteDevice() = null");
                        return;
                    }
                } catch (IOException ex) {
                    if (!this.stopped) {
                        this.stopped = true;
                        Log.v(BluetoothFtpService.TAG, "Accept exception: " + ex.toString());
                    } else {
                        return;
                    }
                } catch (NullPointerException ex2) {
                    if (!this.stopped) {
                        Log.v(BluetoothFtpService.TAG, "Accept null pointer exception: " + ex2.toString());
                    } else {
                        return;
                    }
                }
            }
        }

        void shutdown() {
            this.stopped = true;
            interrupt();
        }
    }

    private void createFtpNotification(String action) {
        NotificationManager nm = (NotificationManager) getSystemService("notification");
        Intent clickIntent = new Intent();
        clickIntent.setClass(this, BluetoothFtpActivity.class);
        clickIntent.addFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
        clickIntent.setAction(action);
        Intent deleteIntent = new Intent();
        deleteIntent.setClass(this, BluetoothFtpReceiver.class);
        String name = getRemoteDeviceName();
        if (action.equals(ACCESS_REQUEST_ACTION)) {
            deleteIntent.setAction(ACCESS_DISALLOWED_ACTION);
            Notification notification = new Notification(android.R.drawable.stat_sys_data_bluetooth, getString(R.string.ftp_notif_ticker), System.currentTimeMillis());
            notification.setLatestEventInfo(this, getString(R.string.ftp_notif_title), getString(R.string.ftp_notif_message, new Object[]{name}), PendingIntent.getActivity(this, 0, clickIntent, 0));
            notification.flags |= 16;
            notification.flags |= 8;
            notification.defaults = 1;
            notification.deleteIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, 0);
            nm.notify(NOTIFICATION_ID_ACCESS, notification);
            return;
        }
        if (action.equals(AUTH_CHALL_ACTION)) {
            deleteIntent.setAction(AUTH_CANCELLED_ACTION);
            Notification notification2 = new Notification(android.R.drawable.stat_sys_data_bluetooth, getString(R.string.ftp_notif_ticker), System.currentTimeMillis());
            notification2.setLatestEventInfo(this, getString(R.string.ftp_notif_title), getString(R.string.ftp_notif_message, new Object[]{name}), PendingIntent.getActivity(this, 0, clickIntent, 0));
            notification2.flags |= 16;
            notification2.flags |= 8;
            notification2.defaults = 1;
            notification2.deleteIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, 0);
            nm.notify(NOTIFICATION_ID_AUTH, notification2);
        }
    }

    private void removeFtpNotification(int id) {
        Context context = getApplicationContext();
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        nm.cancel(id);
    }

    public static String getRemoteDeviceName() {
        return sRemoteDeviceName;
    }
}
