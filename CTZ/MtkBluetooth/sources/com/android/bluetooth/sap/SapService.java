package com.android.bluetooth.sap;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothSap;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.sdp.SdpManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@TargetApi(5)
public class SapService extends ProfileService {
    private static final int CREATE_RETRY_TIME = 10;
    public static final boolean DEBUG = false;
    public static final int MSG_ACQUIRE_WAKE_LOCK = 5005;
    public static final int MSG_CHANGE_STATE = 5007;
    public static final int MSG_RELEASE_WAKE_LOCK = 5006;
    public static final int MSG_SERVERSESSION_CLOSE = 5000;
    public static final int MSG_SESSION_DISCONNECTED = 5002;
    public static final int MSG_SESSION_ESTABLISHED = 5001;
    private static final int RELEASE_WAKE_LOCK_DELAY = 1000;
    private static final String SDP_SAP_SERVICE_NAME = "SIM Access";
    private static final int SDP_SAP_VERSION = 258;
    private static final int SHUTDOWN = 3;
    private static final int START_LISTENER = 1;
    private static final String TAG = "SapService";
    public static final String USER_CONFIRM_TIMEOUT_ACTION = "com.android.bluetooth.sap.USER_CONFIRM_TIMEOUT";
    private static final int USER_CONFIRM_TIMEOUT_VALUE = 25000;
    private static final int USER_TIMEOUT = 2;
    public static final boolean VERBOSE = false;
    private static SapService sSapService;
    private BluetoothAdapter mAdapter;
    private volatile boolean mInterrupted;
    private static String sRemoteDeviceName = null;
    private static final ParcelUuid[] SAP_UUIDS = {BluetoothUuid.SAP};
    private PowerManager.WakeLock mWakeLock = null;
    private SocketAcceptThread mAcceptThread = null;
    private BluetoothServerSocket mServerSocket = null;
    private int mSdpHandle = -1;
    private BluetoothSocket mConnSocket = null;
    private BluetoothDevice mRemoteDevice = null;
    private SapServer mSapServer = null;
    private AlarmManager mAlarmManager = null;
    private boolean mRemoveTimeoutMsg = false;
    private boolean mIsWaitingAuthorization = false;
    private boolean mIsRegistered = false;
    private final Handler mSessionStatusHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            switch (i) {
                case 1:
                    if (SapService.this.mAdapter.isEnabled()) {
                        SapService.this.startRfcommSocketListener();
                    }
                    break;
                case 2:
                    if (SapService.this.mIsWaitingAuthorization) {
                        SapService.this.sendCancelUserConfirmationIntent(SapService.this.mRemoteDevice);
                        SapService.this.cancelUserTimeoutAlarm();
                        SapService.this.mIsWaitingAuthorization = false;
                        SapService.this.stopSapServerSession();
                    }
                    break;
                case 3:
                    SapService.this.closeService();
                    break;
                default:
                    switch (i) {
                        case SapService.MSG_SERVERSESSION_CLOSE:
                            SapService.this.stopSapServerSession();
                            break;
                        case SapService.MSG_SESSION_ESTABLISHED:
                        case SapService.MSG_SESSION_DISCONNECTED:
                            break;
                        default:
                            switch (i) {
                                case SapService.MSG_ACQUIRE_WAKE_LOCK:
                                    if (SapService.this.mWakeLock == null) {
                                        PowerManager powerManager = (PowerManager) SapService.this.getSystemService("power");
                                        SapService.this.mWakeLock = powerManager.newWakeLock(1, "StartingObexMapTransaction");
                                        SapService.this.mWakeLock.setReferenceCounted(false);
                                    }
                                    if (!SapService.this.mWakeLock.isHeld()) {
                                        SapService.this.mWakeLock.acquire();
                                    }
                                    SapService.this.mSessionStatusHandler.removeMessages(SapService.MSG_RELEASE_WAKE_LOCK);
                                    SapService.this.mSessionStatusHandler.sendMessageDelayed(SapService.this.mSessionStatusHandler.obtainMessage(SapService.MSG_RELEASE_WAKE_LOCK), 1000L);
                                    break;
                                case SapService.MSG_RELEASE_WAKE_LOCK:
                                    if (SapService.this.mWakeLock != null) {
                                        SapService.this.mWakeLock.release();
                                    }
                                    break;
                                case SapService.MSG_CHANGE_STATE:
                                    SapService.this.setState(message.arg1);
                                    break;
                            }
                            break;
                    }
                    break;
            }
        }
    };
    private SapBroadcastReceiver mSapReceiver = new SapBroadcastReceiver();
    private int mState = 0;

    public static void notifyUpdateWakeLock(Handler handler) {
        if (handler != null) {
            Message messageObtain = Message.obtain(handler);
            messageObtain.what = MSG_ACQUIRE_WAKE_LOCK;
            messageObtain.sendToTarget();
        }
    }

    private void removeSdpRecord() {
        if (this.mAdapter != null && this.mSdpHandle >= 0 && SdpManager.getDefaultManager() != null) {
            SdpManager.getDefaultManager().removeSdpRecord(this.mSdpHandle);
            this.mSdpHandle = -1;
        }
    }

    private void startRfcommSocketListener() {
        if (this.mAcceptThread == null) {
            this.mAcceptThread = new SocketAcceptThread();
            this.mAcceptThread.setName("SapAcceptThread");
            this.mAcceptThread.start();
        }
    }

    private boolean initSocket() {
        int i = 0;
        boolean z = false;
        while (true) {
            if (i < 10 && !this.mInterrupted) {
                z = true;
                try {
                    this.mServerSocket = this.mAdapter.listenUsingRfcommOn(16, true, true);
                    removeSdpRecord();
                    this.mSdpHandle = SdpManager.getDefaultManager().createSapsRecord(SDP_SAP_SERVICE_NAME, this.mServerSocket.getChannel(), 258);
                } catch (Exception e) {
                    Log.e(TAG, "Error create RfcommServerSocket ", e);
                    z = false;
                }
                if (!z && this.mAdapter != null) {
                    int state = this.mAdapter.getState();
                    if (state != 11 && state != 12) {
                        Log.w(TAG, "initServerSocket failed as BT is (being) turned off");
                        break;
                    }
                    try {
                        Thread.sleep(300L);
                    } catch (InterruptedException e2) {
                        Log.e(TAG, "socketAcceptThread thread was interrupted (3)", e2);
                    }
                    i++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        if (!z) {
            Log.e(TAG, "Error to create listening socket after 10 try");
        }
        return z;
    }

    private synchronized void closeServerSocket() {
        if (this.mServerSocket != null) {
            try {
                this.mServerSocket.close();
                this.mServerSocket = null;
            } catch (IOException e) {
                Log.e(TAG, "Close Server Socket error: ", e);
            }
        }
    }

    private synchronized void closeConnectionSocket() {
        if (this.mConnSocket != null) {
            try {
                this.mConnSocket.close();
                this.mConnSocket = null;
            } catch (IOException e) {
                Log.e(TAG, "Close Connection Socket error: ", e);
            }
        }
    }

    private void closeService() {
        this.mInterrupted = true;
        closeServerSocket();
        if (this.mAcceptThread != null) {
            try {
                this.mAcceptThread.shutdown();
                this.mAcceptThread.join();
                this.mAcceptThread = null;
            } catch (InterruptedException e) {
                Log.w(TAG, "mAcceptThread close error", e);
            }
        }
        if (this.mWakeLock != null) {
            this.mSessionStatusHandler.removeMessages(MSG_ACQUIRE_WAKE_LOCK);
            this.mSessionStatusHandler.removeMessages(MSG_RELEASE_WAKE_LOCK);
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
        closeConnectionSocket();
    }

    private void startSapServerSession() throws IOException {
        if (this.mWakeLock == null) {
            this.mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(1, "StartingSapTransaction");
            this.mWakeLock.setReferenceCounted(false);
            this.mWakeLock.acquire();
        }
        this.mSapServer = new SapServer(this.mSessionStatusHandler, this, this.mConnSocket.getInputStream(), this.mConnSocket.getOutputStream());
        this.mSapServer.start();
        this.mSessionStatusHandler.removeMessages(MSG_RELEASE_WAKE_LOCK);
        this.mSessionStatusHandler.sendMessageDelayed(this.mSessionStatusHandler.obtainMessage(MSG_RELEASE_WAKE_LOCK), 1000L);
    }

    private void stopSapServerSession() {
        this.mAcceptThread = null;
        closeConnectionSocket();
        closeServerSocket();
        setState(0);
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
        if (this.mAdapter.isEnabled()) {
            startRfcommSocketListener();
        }
    }

    private class SocketAcceptThread extends Thread {
        private boolean mStopped;

        private SocketAcceptThread() {
            this.mStopped = false;
        }

        @Override
        public void run() {
            if (SapService.this.mServerSocket == null && !SapService.this.initSocket()) {
                return;
            }
            while (!this.mStopped) {
                try {
                } catch (IOException e) {
                    this.mStopped = true;
                }
                if (SapService.this.mServerSocket == null) {
                    Log.w(SapService.TAG, "mServerSocket is null");
                    return;
                }
                SapService.this.mConnSocket = SapService.this.mServerSocket.accept();
                synchronized (SapService.this) {
                    if (SapService.this.mConnSocket == null) {
                        Log.w(SapService.TAG, "mConnSocket is null");
                        return;
                    }
                    SapService.this.mRemoteDevice = SapService.this.mConnSocket.getRemoteDevice();
                    if (SapService.this.mRemoteDevice != null) {
                        String unused = SapService.sRemoteDeviceName = SapService.this.mRemoteDevice.getName();
                        if (TextUtils.isEmpty(SapService.sRemoteDeviceName)) {
                            String unused2 = SapService.sRemoteDeviceName = SapService.this.getString(R.string.defaultname);
                        }
                        int simAccessPermission = SapService.this.mRemoteDevice.getSimAccessPermission();
                        if (simAccessPermission == 1) {
                            try {
                                SapService.this.startSapServerSession();
                            } catch (IOException e2) {
                                Log.e(SapService.TAG, "catch exception starting obex server session", e2);
                            }
                        } else if (simAccessPermission == 2) {
                            Log.w(SapService.TAG, "Can't connect with " + SapService.sRemoteDeviceName + " as access is rejected");
                            if (SapService.this.mSessionStatusHandler != null) {
                                SapService.this.mSessionStatusHandler.sendEmptyMessage(SapService.MSG_SERVERSESSION_CLOSE);
                            }
                        } else {
                            Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST");
                            intent.setPackage(SapService.this.getString(R.string.pairing_ui_package));
                            intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 4);
                            intent.putExtra("android.bluetooth.device.extra.DEVICE", SapService.this.mRemoteDevice);
                            intent.putExtra("android.bluetooth.device.extra.PACKAGE_NAME", SapService.this.getPackageName());
                            SapService.this.mIsWaitingAuthorization = true;
                            SapService.this.setUserTimeoutAlarm();
                            SapService.this.sendBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
                        }
                        this.mStopped = true;
                    } else {
                        Log.i(SapService.TAG, "getRemoteDevice() = null");
                        return;
                    }
                    this.mStopped = true;
                }
            }
        }

        void shutdown() {
            this.mStopped = true;
            interrupt();
        }
    }

    private void setState(int i) {
        setState(i, 1);
    }

    private synchronized void setState(int i, int i2) {
        if (i != this.mState) {
            if (i == 2) {
                MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.SAP);
            }
            int i3 = this.mState;
            this.mState = i;
            Intent intent = new Intent("android.bluetooth.sap.profile.action.CONNECTION_STATE_CHANGED");
            intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i3);
            intent.putExtra("android.bluetooth.profile.extra.STATE", this.mState);
            intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mRemoteDevice);
            sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
        }
    }

    public int getState() {
        return this.mState;
    }

    public BluetoothDevice getRemoteDevice() {
        return this.mRemoteDevice;
    }

    public static String getRemoteDeviceName() {
        return sRemoteDeviceName;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        boolean z;
        synchronized (this) {
            z = false;
            if (getRemoteDevice().equals(bluetoothDevice) && this.mState == 2) {
                closeConnectionSocket();
                setState(0, 2);
                z = true;
            }
        }
        return z;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        ArrayList arrayList = new ArrayList();
        synchronized (this) {
            if (this.mState == 2 && this.mRemoteDevice != null) {
                arrayList.add(this.mRemoteDevice);
            }
        }
        return arrayList;
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        ArrayList arrayList = new ArrayList();
        Set<BluetoothDevice> bondedDevices = this.mAdapter.getBondedDevices();
        synchronized (this) {
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                if (BluetoothUuid.containsAnyUuid(bluetoothDevice.getUuids(), SAP_UUIDS)) {
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

    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        synchronized (this) {
            return (getState() == 2 && getRemoteDevice().equals(bluetoothDevice)) ? 2 : 0;
        }
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothSapPriorityKey(bluetoothDevice.getAddress()), i);
        return true;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothSapPriorityKey(bluetoothDevice.getAddress()), -1);
    }

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new SapBinder(this);
    }

    @Override
    protected boolean start() {
        Log.v(TAG, "start()");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        intentFilter.addAction(USER_CONFIRM_TIMEOUT_ACTION);
        try {
            registerReceiver(this.mSapReceiver, intentFilter);
            this.mIsRegistered = true;
        } catch (Exception e) {
            Log.w(TAG, "Unable to register sap receiver", e);
        }
        this.mInterrupted = false;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mSessionStatusHandler.sendMessage(this.mSessionStatusHandler.obtainMessage(1));
        setSapService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        Log.v(TAG, "stop()");
        if (!this.mIsRegistered) {
            Log.i(TAG, "Avoid unregister when receiver it is not registered");
            return true;
        }
        setSapService(null);
        try {
            this.mIsRegistered = false;
            unregisterReceiver(this.mSapReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Unable to unregister sap receiver", e);
        }
        setState(0, 2);
        sendShutdownMessage();
        return true;
    }

    @Override
    public void cleanup() {
        setState(0, 2);
        closeService();
        if (this.mSessionStatusHandler != null) {
            this.mSessionStatusHandler.removeCallbacksAndMessages(null);
        }
    }

    @VisibleForTesting
    public static synchronized SapService getSapService() {
        if (sSapService == null) {
            Log.w(TAG, "getSapService(): service is null");
            return null;
        }
        if (!sSapService.isAvailable()) {
            Log.w(TAG, "getSapService(): service is not available");
            return null;
        }
        return sSapService;
    }

    private static synchronized void setSapService(SapService sapService) {
        sSapService = sapService;
    }

    private void setUserTimeoutAlarm() {
        cancelUserTimeoutAlarm();
        this.mRemoveTimeoutMsg = true;
        this.mAlarmManager.set(0, System.currentTimeMillis() + 25000, PendingIntent.getBroadcast(this, 0, new Intent(USER_CONFIRM_TIMEOUT_ACTION), 0));
    }

    private void cancelUserTimeoutAlarm() {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) getSystemService(NotificationCompat.CATEGORY_ALARM);
        }
        if (this.mRemoveTimeoutMsg) {
            this.mAlarmManager.cancel(PendingIntent.getBroadcast(this, 0, new Intent(USER_CONFIRM_TIMEOUT_ACTION), 0));
            this.mRemoveTimeoutMsg = false;
        }
    }

    private void sendCancelUserConfirmationIntent(BluetoothDevice bluetoothDevice) {
        Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL");
        intent.setPackage(getString(R.string.pairing_ui_package));
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 4);
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private void sendShutdownMessage() {
        if (this.mRemoveTimeoutMsg) {
            sendBroadcast(new Intent(USER_CONFIRM_TIMEOUT_ACTION), ProfileService.BLUETOOTH_PERM);
            this.mIsWaitingAuthorization = false;
            cancelUserTimeoutAlarm();
        }
        removeSdpRecord();
        this.mSessionStatusHandler.removeCallbacksAndMessages(null);
        this.mSessionStatusHandler.obtainMessage(3).sendToTarget();
    }

    private void sendConnectTimeoutMessage() {
        if (this.mSessionStatusHandler != null) {
            this.mSessionStatusHandler.obtainMessage(2).sendToTarget();
        }
    }

    private class SapBroadcastReceiver extends BroadcastReceiver {
        private SapBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                int intExtra = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
                if (intExtra == 13) {
                    SapService.this.sendShutdownMessage();
                    return;
                } else {
                    if (intExtra == 12) {
                        SapService.this.mSessionStatusHandler.sendMessage(SapService.this.mSessionStatusHandler.obtainMessage(1));
                        return;
                    }
                    return;
                }
            }
            if (action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY")) {
                Log.v(SapService.TAG, " - Received BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY");
                if (SapService.this.mIsWaitingAuthorization) {
                    SapService.this.mIsWaitingAuthorization = false;
                    if (intent.getIntExtra("android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT", 2) == 1) {
                        if (intent.getBooleanExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", false)) {
                            SapService.this.mRemoteDevice.setSimAccessPermission(1);
                        }
                        try {
                            if (SapService.this.mConnSocket != null) {
                                SapService.this.startSapServerSession();
                            } else {
                                SapService.this.stopSapServerSession();
                            }
                        } catch (IOException e) {
                            Log.e(SapService.TAG, "Caught the error: ", e);
                        }
                        return;
                    }
                    if (intent.getBooleanExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", false)) {
                        SapService.this.mRemoteDevice.setSimAccessPermission(2);
                    }
                    SapService.this.mSessionStatusHandler.sendEmptyMessage(SapService.MSG_SERVERSESSION_CLOSE);
                    return;
                }
                return;
            }
            if (action.equals(SapService.USER_CONFIRM_TIMEOUT_ACTION)) {
                SapService.this.sendConnectTimeoutMessage();
                return;
            }
            if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED") && SapService.this.mIsWaitingAuthorization) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (SapService.this.mRemoteDevice != null && bluetoothDevice != null) {
                    if (SapService.this.mRemoteDevice.equals(bluetoothDevice)) {
                        if (SapService.this.mRemoveTimeoutMsg) {
                            SapService.this.mSessionStatusHandler.removeMessages(2);
                            SapService.this.mSessionStatusHandler.obtainMessage(2).sendToTarget();
                        }
                        SapService.this.setState(0);
                        SapService.this.mSessionStatusHandler.sendEmptyMessage(SapService.MSG_SERVERSESSION_CLOSE);
                        return;
                    }
                    return;
                }
                Log.i(SapService.TAG, "Unexpected error!");
            }
        }
    }

    private static class SapBinder extends IBluetoothSap.Stub implements ProfileService.IProfileServiceBinder {
        private SapService mService;

        private SapService getService() {
            if (!Utils.checkCaller()) {
                Log.w(SapService.TAG, "call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            this.mService.enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
            return this.mService;
        }

        SapBinder(SapService sapService) {
            Log.v(SapService.TAG, "SapBinder()");
            this.mService = sapService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        public int getState() {
            Log.v(SapService.TAG, "getState()");
            if (getService() == null) {
                return 0;
            }
            return getService().getState();
        }

        public BluetoothDevice getClient() {
            Log.v(SapService.TAG, "getClient()");
            SapService service = getService();
            if (service == null) {
                return null;
            }
            Log.v(SapService.TAG, "getClient() - returning " + service.getRemoteDevice());
            return service.getRemoteDevice();
        }

        public boolean isConnected(BluetoothDevice bluetoothDevice) {
            Log.v(SapService.TAG, "isConnected()");
            SapService service = getService();
            return service != null && service.getState() == 2 && service.getRemoteDevice().equals(bluetoothDevice);
        }

        public boolean connect(BluetoothDevice bluetoothDevice) {
            Log.v(SapService.TAG, "connect()");
            return getService() == null ? false : false;
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            Log.v(SapService.TAG, "disconnect()");
            SapService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(bluetoothDevice);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            Log.v(SapService.TAG, "getConnectedDevices()");
            SapService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            Log.v(SapService.TAG, "getDevicesMatchingConnectionStates()");
            SapService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            Log.v(SapService.TAG, "getConnectionState()");
            SapService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
            SapService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(bluetoothDevice, i);
        }

        public int getPriority(BluetoothDevice bluetoothDevice) {
            SapService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(bluetoothDevice);
        }
    }
}
