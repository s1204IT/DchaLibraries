package com.android.bluetooth.sap;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.radio.V1_0.ISap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.vcard.VCardConfig;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

public class SapServer extends Thread implements Handler.Callback {
    public static final boolean DEBUG = false;
    private static final int DISCONNECT_TIMEOUT_IMMEDIATE = 5000;
    private static final int DISCONNECT_TIMEOUT_RFCOMM = 2000;
    public static final int ISAP_GET_SERVICE_DELAY_MILLIS = 3000;
    public static final int NOTIFICATION_ID = 17301632;
    public static final String SAP_DISCONNECT_ACTION = "com.android.bluetooth.sap.action.DISCONNECT_ACTION";
    public static final String SAP_DISCONNECT_TYPE_EXTRA = "com.android.bluetooth.sap.extra.DISCONNECT_TYPE";
    public static final int SAP_MSG_RFC_REPLY = 0;
    public static final int SAP_MSG_RIL_CONNECT = 1;
    public static final int SAP_MSG_RIL_IND = 3;
    public static final int SAP_MSG_RIL_REQ = 2;
    private static final String SAP_NOTIFICATION_CHANNEL = "sap_notification_channel";
    public static final int SAP_PROXY_DEAD = 5;
    public static final int SAP_RIL_SOCK_CLOSED = 4;
    public static final String SAP_STATUS_IND_ACTION = "com.android.bluetooth.sap.action.STATUS_IND_ACTION";
    public static final String SAP_STATUS_IND_EXTRA = "com.android.bluetooth.sap.extra.STATUS_IND_TYPE";
    private static final String TAG = "SapServer";
    private static final String TAG_HANDLER = "SapServerHandler";
    public static final boolean VERBOSE = false;
    private Context mContext;
    private BroadcastReceiver mIntentReceiver;
    private BufferedInputStream mRfcommIn;
    private BufferedOutputStream mRfcommOut;
    private Handler mSapServiceHandler;
    private SAP_STATE mState = SAP_STATE.DISCONNECTED;
    private SapRilReceiver mRilBtReceiver = null;
    private Handler mSapHandler = null;
    private HandlerThread mHandlerThread = null;
    private boolean mIsLocalInitDisconnect = false;
    private CountDownLatch mDeinitSignal = new CountDownLatch(1);
    private PendingIntent mPendingDiscIntent = null;
    private int mMaxMsgSize = 0;
    private int mTestMode = -1;

    private enum SAP_STATE {
        DISCONNECTED,
        CONNECTING,
        CONNECTING_CALL_ONGOING,
        CONNECTED,
        CONNECTED_BUSY,
        DISCONNECTING
    }

    public SapServer(Handler handler, Context context, InputStream inputStream, OutputStream outputStream) {
        this.mContext = null;
        this.mRfcommOut = null;
        this.mRfcommIn = null;
        this.mSapServiceHandler = null;
        this.mContext = context;
        this.mSapServiceHandler = handler;
        this.mRfcommIn = new BufferedInputStream(inputStream);
        this.mRfcommOut = new BufferedOutputStream(outputStream);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction(SAP_DISCONNECT_ACTION);
        intentFilter.addAction(SAP_STATUS_IND_ACTION);
        this.mIntentReceiver = new SapServerBroadcastReceiver();
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter);
    }

    private class SapServerBroadcastReceiver extends BroadcastReceiver {
        private SapServerBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String stringExtra;
            if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
                if (SapServer.this.mState == SAP_STATE.CONNECTING_CALL_ONGOING && (stringExtra = intent.getStringExtra("state")) != null && stringExtra.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    SapMessage sapMessage = new SapMessage(0);
                    sapMessage.setMaxMsgSize(SapServer.this.mMaxMsgSize);
                    SapServer.this.onConnectRequest(sapMessage);
                    return;
                }
                return;
            }
            if (intent.getAction().equals(SapServer.SAP_DISCONNECT_ACTION)) {
                int intExtra = intent.getIntExtra(SapServer.SAP_DISCONNECT_TYPE_EXTRA, 0);
                Log.v(SapServer.TAG, " - Received SAP_DISCONNECT_ACTION type: " + intExtra);
                if (intExtra == 257) {
                    SapServer.this.shutdown();
                    return;
                } else {
                    if (SapServer.this.mState != SAP_STATE.DISCONNECTED && SapServer.this.mState != SAP_STATE.DISCONNECTING) {
                        SapServer.this.sendDisconnectInd(intExtra);
                        return;
                    }
                    return;
                }
            }
            if (intent.getAction().equals(SapServer.SAP_STATUS_IND_ACTION)) {
                int intExtra2 = intent.getIntExtra(SapServer.SAP_STATUS_IND_EXTRA, 2);
                Log.v(SapServer.TAG, " - Received SAP_STATUS_IND_ACTION type: " + intExtra2);
                SapMessage sapMessage2 = new SapMessage(17);
                sapMessage2.setStatusChange(intExtra2);
                SapServer.this.sendClientMessage(sapMessage2);
                return;
            }
            Log.w(SapServer.TAG, "RIL-BT received unexpected Intent: " + intent.getAction());
        }
    }

    public void setTestMode(int i) {
    }

    private void sendDisconnectInd(int i) {
        if (i != 256) {
            SapMessage sapMessage = new SapMessage(4);
            sapMessage.setDisconnectionType(i);
            sendClientMessage(sapMessage);
            if (i == 0) {
                setNotification(1, VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
                return;
            } else {
                if (i == 1) {
                    startDisconnectTimer(256, 5000);
                    return;
                }
                return;
            }
        }
        SapMessage sapMessage2 = new SapMessage(2);
        clearPendingRilResponses(sapMessage2);
        changeState(SAP_STATE.DISCONNECTING);
        sendRilThreadMessage(sapMessage2);
        this.mIsLocalInitDisconnect = true;
    }

    void setNotification(int i, int i2) {
        String string;
        String string2;
        String string3;
        String string4;
        Notification notificationBuild;
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION);
        notificationManager.createNotificationChannel(new NotificationChannel(SAP_NOTIFICATION_CHANNEL, this.mContext.getString(R.string.bluetooth_sap_notif_title), 4));
        int i3 = i2 | VCardConfig.FLAG_APPEND_TYPE_PARAM;
        Boolean boolValueOf = Boolean.valueOf(Boolean.parseBoolean(SystemProperties.get("bt.sap.pts")));
        Intent intent = new Intent(SAP_DISCONNECT_ACTION);
        if (i == 0) {
            string = this.mContext.getString(R.string.bluetooth_sap_notif_title);
            string2 = this.mContext.getString(R.string.bluetooth_sap_notif_disconnect_button);
            string3 = this.mContext.getString(R.string.bluetooth_sap_notif_message);
            string4 = this.mContext.getString(R.string.bluetooth_sap_notif_ticker);
        } else {
            string = this.mContext.getString(R.string.bluetooth_sap_notif_title);
            string2 = this.mContext.getString(R.string.bluetooth_sap_notif_force_disconnect_button);
            string3 = this.mContext.getString(R.string.bluetooth_sap_notif_disconnecting);
            string4 = this.mContext.getString(R.string.bluetooth_sap_notif_ticker);
        }
        if (!boolValueOf.booleanValue()) {
            intent.putExtra(SAP_DISCONNECT_TYPE_EXTRA, i);
            notificationBuild = new Notification.Builder(this.mContext, SAP_NOTIFICATION_CHANNEL).setOngoing(true).addAction(17301632, string2, PendingIntent.getBroadcast(this.mContext, i, intent, i3)).setContentTitle(string).setTicker(string4).setContentText(string3).setSmallIcon(17301632).setAutoCancel(false).setPriority(2).setOnlyAlertOnce(true).setLocalOnly(true).build();
        } else {
            intent.putExtra(SAP_DISCONNECT_TYPE_EXTRA, 0);
            Intent intent2 = new Intent(SAP_DISCONNECT_ACTION);
            intent2.putExtra(SAP_DISCONNECT_TYPE_EXTRA, 1);
            notificationBuild = new Notification.Builder(this.mContext, SAP_NOTIFICATION_CHANNEL).setOngoing(true).addAction(17301632, this.mContext.getString(R.string.bluetooth_sap_notif_disconnect_button), PendingIntent.getBroadcast(this.mContext, 0, intent, i3)).addAction(17301632, this.mContext.getString(R.string.bluetooth_sap_notif_force_disconnect_button), PendingIntent.getBroadcast(this.mContext, 1, intent2, i3)).setContentTitle(string).setTicker(string4).setContentText(string3).setSmallIcon(17301632).setAutoCancel(false).setPriority(2).setOnlyAlertOnce(true).setLocalOnly(true).build();
        }
        notificationBuild.flags |= 40;
        notificationManager.notify(17301632, notificationBuild);
    }

    void clearNotification() {
        ((NotificationManager) this.mContext.getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION)).cancel(17301632);
    }

    @Override
    public void run() {
        try {
            try {
                try {
                    try {
                        Process.setThreadPriority(10);
                        this.mHandlerThread = new HandlerThread(TAG_HANDLER, 10);
                        this.mHandlerThread.start();
                        this.mSapHandler = new Handler(this.mHandlerThread.getLooper(), this);
                        this.mRilBtReceiver = new SapRilReceiver(this.mSapHandler, this.mSapServiceHandler);
                        boolean z = false;
                        while (!z) {
                            int i = this.mRfcommIn.read();
                            if (i == -1) {
                                z = true;
                            } else {
                                SapMessage message = SapMessage.readMessage(i, this.mRfcommIn);
                                SapService.notifyUpdateWakeLock(this.mSapServiceHandler);
                                if (message == null || this.mState == SAP_STATE.DISCONNECTING) {
                                    Log.e(TAG, "Unable to parse message.");
                                    sendClientMessage(new SapMessage(18));
                                } else {
                                    if (i == 0) {
                                        onConnectRequest(message);
                                    } else if (i != 2) {
                                        if (i == 9 || i == 13) {
                                            clearPendingRilResponses(message);
                                        } else {
                                            if (i == 19 && this.mState == SAP_STATE.CONNECTED && message.getTransportProtocol() != 0 && message.getTransportProtocol() != 1) {
                                                Log.w(TAG, "Invalid TransportProtocol received:" + message.getTransportProtocol());
                                                sendClientMessage(new SapMessage(18));
                                                message = null;
                                            }
                                            if (this.mState != SAP_STATE.CONNECTED) {
                                                Log.w(TAG, "Message received in STATE != CONNECTED - state = " + this.mState.name());
                                                sendClientMessage(new SapMessage(18));
                                            }
                                        }
                                        if (message == null && message.getSendToRil()) {
                                            changeState(SAP_STATE.CONNECTED_BUSY);
                                            sendRilThreadMessage(message);
                                        }
                                    } else if (this.mState == SAP_STATE.CONNECTING_CALL_ONGOING) {
                                        Log.d(TAG, "disconnect received when call was ongoing, send disconnect response");
                                        changeState(SAP_STATE.DISCONNECTING);
                                        sendClientMessage(new SapMessage(3));
                                    } else {
                                        clearPendingRilResponses(message);
                                        changeState(SAP_STATE.DISCONNECTING);
                                        sendRilThreadMessage(message);
                                        stopDisconnectTimer();
                                    }
                                    message = null;
                                    if (message == null) {
                                    }
                                }
                            }
                        }
                        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
                        if ((defaultAdapter != null ? defaultAdapter.getState() : -1) != 12) {
                            this.mDeinitSignal.countDown();
                        }
                        stopDisconnectTimer();
                        if (this.mState == SAP_STATE.CONNECTING_CALL_ONGOING) {
                            changeState(SAP_STATE.DISCONNECTED);
                        } else if (this.mState != SAP_STATE.DISCONNECTED) {
                            if (this.mState != SAP_STATE.DISCONNECTING && !this.mIsLocalInitDisconnect) {
                                sendDisconnectInd(256);
                            }
                            try {
                                this.mDeinitSignal.await();
                            } catch (InterruptedException e) {
                                Log.e(TAG, "Interrupt received while waitinf for de-init to complete", e);
                            }
                        }
                        if (this.mIntentReceiver != null) {
                            try {
                                this.mContext.unregisterReceiver(this.mIntentReceiver);
                            } catch (IllegalArgumentException e2) {
                                Log.e(TAG, "Receiver not registered.");
                            }
                            this.mIntentReceiver = null;
                        }
                        stopDisconnectTimer();
                        clearNotification();
                        if (this.mHandlerThread != null) {
                            try {
                                this.mHandlerThread.quit();
                                this.mHandlerThread.join();
                                this.mHandlerThread = null;
                            } catch (InterruptedException e3) {
                            }
                        }
                        if (this.mRilBtReceiver != null) {
                            this.mRilBtReceiver.resetSapProxy();
                            this.mRilBtReceiver = null;
                        }
                        if (this.mRfcommIn != null) {
                            try {
                                this.mRfcommIn.close();
                                this.mRfcommIn = null;
                            } catch (IOException e4) {
                            }
                        }
                        if (this.mRfcommOut != null) {
                            try {
                                this.mRfcommOut.close();
                                this.mRfcommOut = null;
                            } catch (IOException e5) {
                            }
                        }
                    } catch (Throwable th) {
                        BluetoothAdapter defaultAdapter2 = BluetoothAdapter.getDefaultAdapter();
                        if ((defaultAdapter2 != null ? defaultAdapter2.getState() : -1) != 12) {
                            this.mDeinitSignal.countDown();
                        }
                        stopDisconnectTimer();
                        if (this.mState == SAP_STATE.CONNECTING_CALL_ONGOING) {
                            changeState(SAP_STATE.DISCONNECTED);
                        } else if (this.mState != SAP_STATE.DISCONNECTED) {
                            if (this.mState != SAP_STATE.DISCONNECTING && !this.mIsLocalInitDisconnect) {
                                sendDisconnectInd(256);
                            }
                            try {
                                this.mDeinitSignal.await();
                            } catch (InterruptedException e6) {
                                Log.e(TAG, "Interrupt received while waitinf for de-init to complete", e6);
                            }
                        }
                        if (this.mIntentReceiver != null) {
                            try {
                                this.mContext.unregisterReceiver(this.mIntentReceiver);
                            } catch (IllegalArgumentException e7) {
                                Log.e(TAG, "Receiver not registered.");
                            }
                            this.mIntentReceiver = null;
                        }
                        stopDisconnectTimer();
                        clearNotification();
                        if (this.mHandlerThread != null) {
                            try {
                                this.mHandlerThread.quit();
                                this.mHandlerThread.join();
                                this.mHandlerThread = null;
                            } catch (InterruptedException e8) {
                            }
                        }
                        if (this.mRilBtReceiver != null) {
                            this.mRilBtReceiver.resetSapProxy();
                            this.mRilBtReceiver = null;
                        }
                        if (this.mRfcommIn != null) {
                            try {
                                this.mRfcommIn.close();
                                this.mRfcommIn = null;
                            } catch (IOException e9) {
                            }
                        }
                        if (this.mRfcommOut != null) {
                            try {
                                this.mRfcommOut.close();
                                this.mRfcommOut = null;
                            } catch (IOException e10) {
                            }
                        }
                        if (this.mSapServiceHandler != null) {
                            Message messageObtain = Message.obtain(this.mSapServiceHandler);
                            messageObtain.what = 5000;
                            messageObtain.sendToTarget();
                        }
                        Log.i(TAG, "All done exiting thread...");
                        throw th;
                    }
                } catch (NullPointerException e11) {
                    Log.w(TAG, e11);
                    BluetoothAdapter defaultAdapter3 = BluetoothAdapter.getDefaultAdapter();
                    if ((defaultAdapter3 != null ? defaultAdapter3.getState() : -1) != 12) {
                        this.mDeinitSignal.countDown();
                    }
                    stopDisconnectTimer();
                    if (this.mState == SAP_STATE.CONNECTING_CALL_ONGOING) {
                        changeState(SAP_STATE.DISCONNECTED);
                    } else if (this.mState != SAP_STATE.DISCONNECTED) {
                        if (this.mState != SAP_STATE.DISCONNECTING && !this.mIsLocalInitDisconnect) {
                            sendDisconnectInd(256);
                        }
                        try {
                            this.mDeinitSignal.await();
                        } catch (InterruptedException e12) {
                            Log.e(TAG, "Interrupt received while waitinf for de-init to complete", e12);
                        }
                    }
                    if (this.mIntentReceiver != null) {
                        try {
                            this.mContext.unregisterReceiver(this.mIntentReceiver);
                        } catch (IllegalArgumentException e13) {
                            Log.e(TAG, "Receiver not registered.");
                        }
                        this.mIntentReceiver = null;
                    }
                    stopDisconnectTimer();
                    clearNotification();
                    if (this.mHandlerThread != null) {
                        try {
                            this.mHandlerThread.quit();
                            this.mHandlerThread.join();
                            this.mHandlerThread = null;
                        } catch (InterruptedException e14) {
                        }
                    }
                    if (this.mRilBtReceiver != null) {
                        this.mRilBtReceiver.resetSapProxy();
                        this.mRilBtReceiver = null;
                    }
                    if (this.mRfcommIn != null) {
                        try {
                            this.mRfcommIn.close();
                            this.mRfcommIn = null;
                        } catch (IOException e15) {
                        }
                    }
                    if (this.mRfcommOut != null) {
                        try {
                            this.mRfcommOut.close();
                            this.mRfcommOut = null;
                        } catch (IOException e16) {
                        }
                    }
                    if (this.mSapServiceHandler != null) {
                    }
                }
            } catch (IOException e17) {
                Log.i(TAG, "IOException received, this is probably a shutdown signal, cleaning up...");
                BluetoothAdapter defaultAdapter4 = BluetoothAdapter.getDefaultAdapter();
                if ((defaultAdapter4 != null ? defaultAdapter4.getState() : -1) != 12) {
                    this.mDeinitSignal.countDown();
                }
                stopDisconnectTimer();
                if (this.mState == SAP_STATE.CONNECTING_CALL_ONGOING) {
                    changeState(SAP_STATE.DISCONNECTED);
                } else if (this.mState != SAP_STATE.DISCONNECTED) {
                    if (this.mState != SAP_STATE.DISCONNECTING && !this.mIsLocalInitDisconnect) {
                        sendDisconnectInd(256);
                    }
                    try {
                        this.mDeinitSignal.await();
                    } catch (InterruptedException e18) {
                        Log.e(TAG, "Interrupt received while waitinf for de-init to complete", e18);
                    }
                }
                if (this.mIntentReceiver != null) {
                    try {
                        this.mContext.unregisterReceiver(this.mIntentReceiver);
                    } catch (IllegalArgumentException e19) {
                        Log.e(TAG, "Receiver not registered.");
                    }
                    this.mIntentReceiver = null;
                }
                stopDisconnectTimer();
                clearNotification();
                if (this.mHandlerThread != null) {
                    try {
                        this.mHandlerThread.quit();
                        this.mHandlerThread.join();
                        this.mHandlerThread = null;
                    } catch (InterruptedException e20) {
                    }
                }
                if (this.mRilBtReceiver != null) {
                    this.mRilBtReceiver.resetSapProxy();
                    this.mRilBtReceiver = null;
                }
                if (this.mRfcommIn != null) {
                    try {
                        this.mRfcommIn.close();
                        this.mRfcommIn = null;
                    } catch (IOException e21) {
                    }
                }
                if (this.mRfcommOut != null) {
                    try {
                        this.mRfcommOut.close();
                        this.mRfcommOut = null;
                    } catch (IOException e22) {
                    }
                }
                if (this.mSapServiceHandler != null) {
                }
            }
        } catch (Exception e23) {
            Log.w(TAG, e23);
            BluetoothAdapter defaultAdapter5 = BluetoothAdapter.getDefaultAdapter();
            if ((defaultAdapter5 != null ? defaultAdapter5.getState() : -1) != 12) {
                this.mDeinitSignal.countDown();
            }
            stopDisconnectTimer();
            if (this.mState == SAP_STATE.CONNECTING_CALL_ONGOING) {
                changeState(SAP_STATE.DISCONNECTED);
            } else if (this.mState != SAP_STATE.DISCONNECTED) {
                if (this.mState != SAP_STATE.DISCONNECTING && !this.mIsLocalInitDisconnect) {
                    sendDisconnectInd(256);
                }
                try {
                    this.mDeinitSignal.await();
                } catch (InterruptedException e24) {
                    Log.e(TAG, "Interrupt received while waitinf for de-init to complete", e24);
                }
            }
            if (this.mIntentReceiver != null) {
                try {
                    this.mContext.unregisterReceiver(this.mIntentReceiver);
                } catch (IllegalArgumentException e25) {
                    Log.e(TAG, "Receiver not registered.");
                }
                this.mIntentReceiver = null;
            }
            stopDisconnectTimer();
            clearNotification();
            if (this.mHandlerThread != null) {
                try {
                    this.mHandlerThread.quit();
                    this.mHandlerThread.join();
                    this.mHandlerThread = null;
                } catch (InterruptedException e26) {
                }
            }
            if (this.mRilBtReceiver != null) {
                this.mRilBtReceiver.resetSapProxy();
                this.mRilBtReceiver = null;
            }
            if (this.mRfcommIn != null) {
                try {
                    this.mRfcommIn.close();
                    this.mRfcommIn = null;
                } catch (IOException e27) {
                }
            }
            if (this.mRfcommOut != null) {
                try {
                    this.mRfcommOut.close();
                    this.mRfcommOut = null;
                } catch (IOException e28) {
                }
            }
            if (this.mSapServiceHandler != null) {
            }
        }
        if (this.mSapServiceHandler != null) {
            Message messageObtain2 = Message.obtain(this.mSapServiceHandler);
            messageObtain2.what = 5000;
            messageObtain2.sendToTarget();
        }
        Log.i(TAG, "All done exiting thread...");
    }

    private void onConnectRequest(SapMessage sapMessage) {
        SapMessage sapMessage2 = new SapMessage(1);
        if (this.mState == SAP_STATE.CONNECTING) {
            sendRilMessage(sapMessage);
            stopDisconnectTimer();
        } else {
            if (this.mState != SAP_STATE.DISCONNECTED && this.mState != SAP_STATE.CONNECTING_CALL_ONGOING) {
                sapMessage2.setConnectionStatus(1);
            } else {
                this.mMaxMsgSize = sapMessage.getMaxMsgSize();
                if (isCallOngoing()) {
                    sapMessage2.setConnectionStatus(4);
                } else {
                    changeState(SAP_STATE.CONNECTING);
                    if (this.mRilBtReceiver != null) {
                        this.mRilBtReceiver.sendRilConnectMessage();
                    } else {
                        sapMessage2 = new SapMessage(1);
                        sapMessage2.setConnectionStatus(1);
                        sendClientMessage(sapMessage2);
                    }
                }
            }
            if (sapMessage2 == null) {
                sendClientMessage(sapMessage2);
                return;
            }
            return;
        }
        sapMessage2 = null;
        if (sapMessage2 == null) {
        }
    }

    private void clearPendingRilResponses(SapMessage sapMessage) {
        if (this.mState == SAP_STATE.CONNECTED_BUSY) {
            sapMessage.setClearRilQueue(true);
        }
    }

    private void sendClientMessage(SapMessage sapMessage) {
        this.mSapHandler.sendMessage(this.mSapHandler.obtainMessage(0, sapMessage));
    }

    private void sendRilThreadMessage(SapMessage sapMessage) {
        this.mSapHandler.sendMessage(this.mSapHandler.obtainMessage(2, sapMessage));
    }

    private boolean isCallOngoing() {
        if (((TelephonyManager) this.mContext.getSystemService("phone")).getCallState() == 0) {
            return false;
        }
        return true;
    }

    private void changeState(SAP_STATE sap_state) {
        synchronized (this) {
            this.mState = sap_state;
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case 0:
                handleRfcommReply((SapMessage) message.obj);
                return true;
            case 1:
                if (this.mTestMode != -1) {
                    SapMessage sapMessage = new SapMessage(513);
                    sapMessage.setTestMode(this.mTestMode);
                    sendRilMessage(sapMessage);
                    this.mTestMode = -1;
                }
                SapMessage sapMessage2 = new SapMessage(0);
                sapMessage2.setMaxMsgSize(this.mMaxMsgSize);
                sendRilMessage(sapMessage2);
                return true;
            case 2:
                SapMessage sapMessage3 = (SapMessage) message.obj;
                if (sapMessage3 != null) {
                    sendRilMessage(sapMessage3);
                    return true;
                }
                return true;
            case 3:
                handleRilInd((SapMessage) message.obj);
                return true;
            case 4:
                startDisconnectTimer(257, DISCONNECT_TIMEOUT_RFCOMM);
                return true;
            case 5:
                if (((Long) message.obj).longValue() == this.mRilBtReceiver.mSapProxyCookie.get()) {
                    this.mRilBtReceiver.notifyShutdown();
                    this.mRilBtReceiver.resetSapProxy();
                    this.mRilBtReceiver.getSapProxy();
                    return true;
                }
                return true;
            default:
                return false;
        }
    }

    private void shutdown() {
        try {
            if (this.mRfcommOut != null) {
                this.mRfcommOut.close();
            }
        } catch (IOException e) {
        }
        try {
            if (this.mRfcommIn != null) {
                this.mRfcommIn.close();
            }
        } catch (IOException e2) {
        }
        this.mRfcommIn = null;
        this.mRfcommOut = null;
        stopDisconnectTimer();
        clearNotification();
    }

    private void startDisconnectTimer(int i, int i2) {
        stopDisconnectTimer();
        synchronized (this) {
            Intent intent = new Intent(SAP_DISCONNECT_ACTION);
            intent.putExtra(SAP_DISCONNECT_TYPE_EXTRA, i);
            AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService(NotificationCompat.CATEGORY_ALARM);
            this.mPendingDiscIntent = PendingIntent.getBroadcast(this.mContext, i, intent, VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            alarmManager.set(2, SystemClock.elapsedRealtime() + ((long) i2), this.mPendingDiscIntent);
        }
    }

    private void stopDisconnectTimer() {
        synchronized (this) {
            if (this.mPendingDiscIntent != null) {
                ((AlarmManager) this.mContext.getSystemService(NotificationCompat.CATEGORY_ALARM)).cancel(this.mPendingDiscIntent);
                this.mPendingDiscIntent.cancel();
                this.mPendingDiscIntent = null;
            }
        }
    }

    private void handleRfcommReply(SapMessage sapMessage) {
        if (sapMessage != null) {
            int msgType = sapMessage.getMsgType();
            if (msgType != 1) {
                if (msgType != 3) {
                    if (msgType == 17) {
                        if (this.mState == SAP_STATE.DISCONNECTED || this.mState == SAP_STATE.CONNECTING || this.mState == SAP_STATE.DISCONNECTING) {
                            sapMessage = null;
                        }
                        if (this.mSapServiceHandler != null && this.mState == SAP_STATE.CONNECTED) {
                            Message messageObtain = Message.obtain(this.mSapServiceHandler);
                            messageObtain.what = SapService.MSG_CHANGE_STATE;
                            messageObtain.arg1 = 2;
                            messageObtain.sendToTarget();
                            setNotification(0, 0);
                        }
                    }
                } else if (this.mState == SAP_STATE.DISCONNECTING) {
                    sapMessage = new SapMessage(3);
                    changeState(SAP_STATE.DISCONNECTED);
                    startDisconnectTimer(257, DISCONNECT_TIMEOUT_RFCOMM);
                    this.mDeinitSignal.countDown();
                } else {
                    this.mDeinitSignal.countDown();
                    if (this.mIsLocalInitDisconnect) {
                        shutdown();
                        sapMessage = null;
                    } else {
                        startDisconnectTimer(257, DISCONNECT_TIMEOUT_RFCOMM);
                    }
                }
            } else if (this.mState == SAP_STATE.CONNECTING_CALL_ONGOING) {
                if (sapMessage.getConnectionStatus() == 0) {
                    changeState(SAP_STATE.CONNECTED);
                }
                sapMessage = null;
            } else if (sapMessage.getConnectionStatus() == 0) {
                changeState(SAP_STATE.CONNECTED);
            } else if (sapMessage.getConnectionStatus() == 4) {
                changeState(SAP_STATE.CONNECTING_CALL_ONGOING);
            } else if (sapMessage.getConnectionStatus() != 0) {
                startDisconnectTimer(256, DISCONNECT_TIMEOUT_RFCOMM);
            }
        }
        if (this.mState == SAP_STATE.CONNECTED_BUSY && SapMessage.getNumPendingRilMessages() == 0) {
            changeState(SAP_STATE.CONNECTED);
        }
        if (sapMessage != null) {
            sendReply(sapMessage);
        }
    }

    private void handleRilInd(SapMessage sapMessage) {
        if (sapMessage != null && sapMessage.getMsgType() == 258) {
            if (this.mState != SAP_STATE.DISCONNECTED && this.mState != SAP_STATE.DISCONNECTING) {
                SapMessage sapMessage2 = new SapMessage(4);
                sapMessage2.setDisconnectionType(sapMessage.getDisconnectionType());
                sendClientMessage(sapMessage2);
                return;
            }
            sendDisconnectInd(sapMessage.getDisconnectionType());
        }
    }

    private void sendRilMessage(SapMessage sapMessage) {
        Log.d(TAG_HANDLER, "sendRilMessage: calling getSapProxy");
        synchronized (this.mRilBtReceiver.getSapProxyLock()) {
            ISap sapProxy = this.mRilBtReceiver.getSapProxy();
            if (sapProxy == null) {
                Log.e(TAG_HANDLER, "sendRilMessage: Unable to send message to RIL; sapProxy is null");
                sendClientMessage(new SapMessage(18));
                return;
            }
            try {
                sapMessage.send(sapProxy);
            } catch (RemoteException | RuntimeException e) {
                Log.e(TAG_HANDLER, "sendRilMessage: Unable to send message to RIL: " + e);
                sendClientMessage(new SapMessage(18));
                this.mRilBtReceiver.notifyShutdown();
                this.mRilBtReceiver.resetSapProxy();
            } catch (IllegalArgumentException e2) {
                Log.e(TAG_HANDLER, "sendRilMessage: IllegalArgumentException", e2);
                sendClientMessage(new SapMessage(18));
            }
        }
    }

    private void sendReply(SapMessage sapMessage) {
        if (this.mRfcommOut != null) {
            try {
                sapMessage.write(this.mRfcommOut);
                this.mRfcommOut.flush();
            } catch (IOException e) {
                Log.w(TAG_HANDLER, e);
            }
        }
    }

    private static String getMessageName(int i) {
        switch (i) {
            case 0:
                return "SAP_MSG_REPLY";
            case 1:
                return "SAP_MSG_RIL_CONNECT";
            case 2:
                return "SAP_MSG_RIL_REQ";
            case 3:
                return "SAP_MSG_RIL_IND";
            default:
                return "Unknown message ID";
        }
    }
}
