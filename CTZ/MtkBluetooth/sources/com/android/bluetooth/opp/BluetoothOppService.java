package com.android.bluetooth.opp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteException;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.BluetoothObexTransport;
import com.android.bluetooth.IObexConnectionHandler;
import com.android.bluetooth.ObexServerSockets;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.sap.SapMessage;
import com.android.bluetooth.sdp.SdpManager;
import com.android.vcard.VCardConfig;
import com.google.android.collect.Lists;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import javax.obex.ObexTransport;

public class BluetoothOppService extends ProfileService implements IObexConnectionHandler {
    private static final String INVISIBLE = "visibility=1";
    private static final int MEDIA_SCANNED = 2;
    private static final int MEDIA_SCANNED_FAILED = 3;
    private static final int MSG_INCOMING_BTOPP_CONNECTION = 100;
    private static final int MSG_INCOMING_CONNECTION_RETRY = 4;
    private static final int START_LISTENER = 1;
    private static final int STOP_LISTENER = 200;
    private static final String TAG = "BtOppService";
    private static final String WHERE_CONFIRM_PENDING_INBOUND = "direction=1 AND confirm=0";
    private static final String WHERE_INBOUND_SUCCESS = "direction=1 AND status=200 AND visibility=1";
    private static final String WHERE_INVISIBLE_UNCONFIRMED = "(status>=200 AND visibility=1) OR (direction=1 AND confirm=0)";
    private static BluetoothOppService mInstance;
    private static BluetoothOppService sBluetoothOppService;
    boolean mAcceptNewConnections;
    private BluetoothAdapter mAdapter;
    private int mBatchId;
    public ArrayList<BluetoothOppBatch> mBatches;
    private int mIncomingRetries;
    private boolean mListenStarted;
    private boolean mMediaScanInProgress;
    private CharArrayBuffer mNewChars;
    private BluetoothOppNotification mNotifier;
    private BluetoothShareContentObserver mObserver;
    private CharArrayBuffer mOldChars;
    private ObexTransport mPendingConnection;
    private boolean mPendingUpdate;
    private BluetoothOppObexServerSession mServerSession;
    private ObexServerSockets mServerSocket;
    public BluetoothOppTransfer mServerTransfer;
    private ArrayList<BluetoothOppShareInfo> mShares;
    public BluetoothOppTransfer mTransfer;
    private UpdateThread mUpdateThread;
    private boolean mUpdateThreadRunning;
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;
    private static final byte[] SUPPORTED_OPP_FORMAT = {1, 2, 3, 4, -1};
    private int mOppSdpHandle = -1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 100) {
                if (BluetoothOppService.D) {
                    Log.d(BluetoothOppService.TAG, "Get incoming connection");
                }
                ObexTransport obexTransport = (ObexTransport) message.obj;
                if (BluetoothOppService.this.mBatches.size() != 0 || BluetoothOppService.this.mPendingConnection != null) {
                    if (BluetoothOppService.this.mPendingConnection != null) {
                        Log.w(BluetoothOppService.TAG, "OPP busy! Reject connection");
                        try {
                            obexTransport.close();
                            return;
                        } catch (IOException e) {
                            Log.e(BluetoothOppService.TAG, "close tranport error");
                            return;
                        }
                    }
                    Log.i(BluetoothOppService.TAG, "OPP busy! Retry after 1 second");
                    BluetoothOppService.this.mIncomingRetries++;
                    BluetoothOppService.this.mPendingConnection = obexTransport;
                    Message messageObtain = Message.obtain(BluetoothOppService.this.mHandler);
                    messageObtain.what = 4;
                    BluetoothOppService.this.mHandler.sendMessageDelayed(messageObtain, 1000L);
                    return;
                }
                Log.i(BluetoothOppService.TAG, "Start Obex Server");
                BluetoothOppService.this.createServerSession(obexTransport);
                return;
            }
            if (i != 200) {
                switch (i) {
                    case 1:
                        if (BluetoothOppService.this.mAdapter != null && BluetoothOppService.this.mAdapter.isEnabled()) {
                            BluetoothOppService.this.startSocketListener();
                            return;
                        }
                        return;
                    case 2:
                        if (BluetoothOppService.V) {
                            Log.v(BluetoothOppService.TAG, "Update mInfo.id " + message.arg1 + " for data uri= " + message.obj.toString());
                        }
                        ContentValues contentValues = new ContentValues();
                        Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + message.arg1);
                        contentValues.put("scanned", (Integer) 1);
                        contentValues.put("uri", message.obj.toString());
                        contentValues.put(BluetoothShare.MIMETYPE, BluetoothOppService.this.getContentResolver().getType(Uri.parse(message.obj.toString())));
                        BluetoothOppService.this.getContentResolver().update(uri, contentValues, null, null);
                        synchronized (BluetoothOppService.this) {
                            BluetoothOppService.this.mMediaScanInProgress = false;
                            break;
                        }
                        return;
                    case 3:
                        Log.v(BluetoothOppService.TAG, "Update mInfo.id " + message.arg1 + " for MEDIA_SCANNED_FAILED");
                        ContentValues contentValues2 = new ContentValues();
                        Uri uri2 = Uri.parse(BluetoothShare.CONTENT_URI + "/" + message.arg1);
                        contentValues2.put("scanned", (Integer) 2);
                        BluetoothOppService.this.getContentResolver().update(uri2, contentValues2, null, null);
                        synchronized (BluetoothOppService.this) {
                            BluetoothOppService.this.mMediaScanInProgress = false;
                            break;
                        }
                        return;
                    case 4:
                        if (BluetoothOppService.this.mBatches.size() != 0) {
                            if (BluetoothOppService.this.mIncomingRetries == 20) {
                                Log.w(BluetoothOppService.TAG, "Retried 20 seconds, reject connection");
                                try {
                                    BluetoothOppService.this.mPendingConnection.close();
                                    break;
                                } catch (IOException e2) {
                                    Log.e(BluetoothOppService.TAG, "close tranport error");
                                }
                                if (BluetoothOppService.this.mServerSocket != null) {
                                    BluetoothOppService.this.acceptNewConnections();
                                }
                                BluetoothOppService.this.mIncomingRetries = 0;
                                BluetoothOppService.this.mPendingConnection = null;
                                return;
                            }
                            Log.i(BluetoothOppService.TAG, "OPP busy! Retry after 1 second");
                            BluetoothOppService.this.mIncomingRetries++;
                            Message messageObtain2 = Message.obtain(BluetoothOppService.this.mHandler);
                            messageObtain2.what = 4;
                            BluetoothOppService.this.mHandler.sendMessageDelayed(messageObtain2, 1000L);
                            return;
                        }
                        Log.i(BluetoothOppService.TAG, "Start Obex Server");
                        BluetoothOppService.this.createServerSession(BluetoothOppService.this.mPendingConnection);
                        BluetoothOppService.this.mIncomingRetries = 0;
                        BluetoothOppService.this.mPendingConnection = null;
                        return;
                    default:
                        return;
                }
            }
            BluetoothOppService.this.stopListeners();
            BluetoothOppService.this.mListenStarted = false;
            if (BluetoothOppService.this.mServerTransfer != null) {
                BluetoothOppService.this.mServerTransfer.onBatchCanceled();
                BluetoothOppService.this.mServerTransfer = null;
            }
            if (BluetoothOppService.this.mTransfer != null) {
                BluetoothOppService.this.mTransfer.onBatchCanceled();
                BluetoothOppService.this.mTransfer = null;
            }
            BluetoothOppService.this.unregisterReceivers();
            synchronized (BluetoothOppService.this) {
                if (BluetoothOppService.this.mUpdateThread != null) {
                    BluetoothOppService.this.mUpdateThread.interrupt();
                }
            }
            while (BluetoothOppService.this.mUpdateThread != null && BluetoothOppService.this.mUpdateThreadRunning) {
                try {
                    Thread.sleep(50L);
                } catch (Exception e3) {
                    Log.e(BluetoothOppService.TAG, "Thread sleep", e3);
                }
            }
            synchronized (BluetoothOppService.this) {
                if (BluetoothOppService.this.mUpdateThread != null) {
                    try {
                        BluetoothOppService.this.mUpdateThread.join();
                    } catch (InterruptedException e4) {
                        Log.e(BluetoothOppService.TAG, "Interrupted", e4);
                    }
                    BluetoothOppService.this.mUpdateThread = null;
                }
            }
            if (BluetoothOppService.this.mNotifier != null) {
                BluetoothOppService.this.mNotifier.cancelNotifications();
            }
        }
    };
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                switch (intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE)) {
                    case 12:
                        if (BluetoothOppService.V) {
                            Log.v(BluetoothOppService.TAG, "Bluetooth state changed: STATE_ON");
                        }
                        BluetoothOppService.this.startListener();
                        synchronized (this) {
                            if (BluetoothOppManager.getInstance(context).mSendingFlag) {
                                BluetoothOppManager.getInstance(context).mSendingFlag = false;
                                if (BenesseExtension.getDchaState() == 0) {
                                    Intent intent2 = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
                                    intent2.putExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false);
                                    intent2.putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 2);
                                    intent2.putExtra("android.bluetooth.devicepicker.extra.LAUNCH_PACKAGE", "com.android.bluetooth");
                                    intent2.putExtra("android.bluetooth.devicepicker.extra.DEVICE_PICKER_LAUNCH_CLASS", BluetoothOppReceiver.class.getName());
                                    intent2.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
                                    context.startActivity(intent2);
                                } else {
                                    return;
                                }
                            }
                            return;
                        }
                    case 13:
                        if (BluetoothOppService.V) {
                            Log.v(BluetoothOppService.TAG, "Bluetooth state changed: STATE_TURNING_OFF");
                        }
                        BluetoothOppService.this.mHandler.sendMessage(BluetoothOppService.this.mHandler.obtainMessage(200));
                        BluetoothOppService.this.clearPendingTask(context);
                        return;
                    default:
                        return;
                }
            }
        }
    };

    private class BluetoothShareContentObserver extends ContentObserver {
        BluetoothShareContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean z) {
            if (BluetoothOppService.V) {
                Log.v(BluetoothOppService.TAG, "ContentObserver received notification");
            }
            BluetoothOppService.this.updateFromProvider();
        }
    }

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new OppBinder(this);
    }

    private static class OppBinder extends Binder implements ProfileService.IProfileServiceBinder {
        OppBinder(BluetoothOppService bluetoothOppService) {
        }

        @Override
        public void cleanup() {
        }
    }

    @Override
    protected void create() {
        if (V) {
            Log.v(TAG, "onCreate");
        }
        mInstance = this;
        this.mShares = Lists.newArrayList();
        this.mBatches = Lists.newArrayList();
        this.mBatchId = 1;
        final ContentResolver contentResolver = getContentResolver();
        new Thread("trimDatabase") {
            @Override
            public void run() {
                BluetoothOppService.trimDatabase(contentResolver);
            }
        }.start();
        registerReceiver(this.mBluetoothReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        synchronized (this) {
            if (this.mAdapter == null) {
                Log.w(TAG, "Local BT device is not enabled");
            }
        }
        if (V) {
            BluetoothOppPreference bluetoothOppPreference = BluetoothOppPreference.getInstance(this);
            if (bluetoothOppPreference != null) {
                bluetoothOppPreference.dump();
            } else {
                Log.w(TAG, "BluetoothOppPreference.getInstance returned null.");
            }
        }
    }

    @Override
    public boolean start() {
        if (V) {
            Log.v(TAG, "start()");
        }
        this.mObserver = new BluetoothShareContentObserver();
        getContentResolver().registerContentObserver(BluetoothShare.CONTENT_URI, true, this.mObserver);
        this.mNotifier = new BluetoothOppNotification(this);
        this.mNotifier.mNotificationMgr.cancelAll();
        this.mNotifier.updateNotification();
        updateFromProvider();
        setBluetoothOppService(this);
        return true;
    }

    @Override
    public boolean stop() {
        setBluetoothOppService(null);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(200));
        return true;
    }

    private void startListener() {
        if (!this.mListenStarted && this.mAdapter.isEnabled()) {
            if (V) {
                Log.v(TAG, "Starting RfcommListener");
            }
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1));
            this.mListenStarted = true;
        }
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        if (this.mShares.size() > 0) {
            println(sb, "Shares:");
            for (BluetoothOppShareInfo bluetoothOppShareInfo : this.mShares) {
                String str = bluetoothOppShareInfo.mDirection == 0 ? " -> " : " <- ";
                println(sb, "  " + new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date(bluetoothOppShareInfo.mTimestamp)) + str + bluetoothOppShareInfo.mCurrentBytes + "/" + bluetoothOppShareInfo.mTotalBytes);
            }
        }
    }

    @VisibleForTesting
    public static synchronized BluetoothOppService getBluetoothOppService() {
        if (sBluetoothOppService == null) {
            Log.w(TAG, "getBluetoothOppService(): service is null");
            return null;
        }
        if (!sBluetoothOppService.isAvailable()) {
            Log.w(TAG, "getBluetoothOppService(): service is not available");
            return null;
        }
        return sBluetoothOppService;
    }

    private static synchronized void setBluetoothOppService(BluetoothOppService bluetoothOppService) {
        if (D) {
            Log.d(TAG, "setBluetoothOppService(): set to: " + bluetoothOppService);
        }
        sBluetoothOppService = bluetoothOppService;
    }

    private void startSocketListener() {
        if (D) {
            Log.d(TAG, "start Socket Listeners");
        }
        stopListeners();
        this.mServerSocket = ObexServerSockets.createInsecureWithFixedChannels(this, 12, SdpManager.OPP_L2CAP_PSM);
        acceptNewConnections();
        SdpManager defaultManager = SdpManager.getDefaultManager();
        if (defaultManager == null || this.mServerSocket == null) {
            Log.e(TAG, "ERROR:serversocket object is NULL  sdp manager :" + defaultManager + " mServerSocket:" + this.mServerSocket);
            return;
        }
        this.mOppSdpHandle = defaultManager.createOppOpsRecord("OBEX Object Push", this.mServerSocket.getRfcommChannel(), this.mServerSocket.getL2capPsm(), SapMessage.ID_RIL_UNSOL_DISCONNECT_IND, SUPPORTED_OPP_FORMAT);
        if (D) {
            Log.d(TAG, "mOppSdpHandle :" + this.mOppSdpHandle);
        }
    }

    @Override
    protected void cleanup() {
        if (V) {
            Log.v(TAG, "onDestroy");
        }
        stopListeners();
        if (this.mBatches != null) {
            this.mBatches.clear();
        }
        if (this.mShares != null) {
            this.mShares.clear();
        }
        if (this.mHandler != null) {
            this.mHandler.removeCallbacksAndMessages(null);
        }
    }

    private void unregisterReceivers() {
        try {
            if (this.mObserver != null) {
                getContentResolver().unregisterContentObserver(this.mObserver);
                this.mObserver = null;
            }
            unregisterReceiver(this.mBluetoothReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "unregisterReceivers " + e.toString());
        }
    }

    private void createServerSession(ObexTransport obexTransport) {
        this.mServerSession = new BluetoothOppObexServerSession(this, obexTransport, this);
        this.mServerSession.preStart();
        if (D) {
            Log.d(TAG, "Get ServerSession " + this.mServerSession.toString() + " for incoming connection" + obexTransport.toString());
        }
    }

    private void updateFromProvider() {
        synchronized (this) {
            this.mPendingUpdate = true;
            if (this.mUpdateThread == null) {
                this.mUpdateThread = new UpdateThread();
                this.mUpdateThread.start();
                this.mUpdateThreadRunning = true;
            }
        }
    }

    private class UpdateThread extends Thread {
        private boolean mIsInterrupted;

        UpdateThread() {
            super("Bluetooth Share Service");
            this.mIsInterrupted = false;
        }

        @Override
        public void interrupt() {
            this.mIsInterrupted = true;
            if (BluetoothOppService.D) {
                Log.d(BluetoothOppService.TAG, "OPP UpdateThread interrupted ");
            }
            super.interrupt();
        }

        @Override
        public void run() {
            int i;
            Process.setThreadPriority(10);
            while (!this.mIsInterrupted) {
                synchronized (BluetoothOppService.this) {
                    if (BluetoothOppService.this.mUpdateThread == this) {
                        if (BluetoothOppService.V) {
                            Log.v(BluetoothOppService.TAG, "pendingUpdate is " + BluetoothOppService.this.mPendingUpdate + " sListenStarted is " + BluetoothOppService.this.mListenStarted + " isInterrupted :" + this.mIsInterrupted);
                        }
                        Cursor cursorQuery = null;
                        if (!BluetoothOppService.this.mPendingUpdate) {
                            BluetoothOppService.this.mUpdateThread = null;
                            BluetoothOppService.this.mUpdateThreadRunning = false;
                            return;
                        }
                        BluetoothOppService.this.mPendingUpdate = false;
                        try {
                            cursorQuery = BluetoothOppService.this.getContentResolver().query(BluetoothShare.CONTENT_URI, null, null, null, "_id");
                        } catch (Exception e) {
                            Log.e(BluetoothOppService.TAG, "SQLite exception occur : " + e.toString());
                        }
                        if (cursorQuery == null) {
                            BluetoothOppService.this.mUpdateThreadRunning = false;
                            return;
                        }
                        cursorQuery.moveToFirst();
                        boolean zIsAfterLast = cursorQuery.isAfterLast();
                        int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("_id");
                        int i2 = 0;
                        while (true) {
                            if (!zIsAfterLast || (i2 < BluetoothOppService.this.mShares.size() && BluetoothOppService.this.mListenStarted)) {
                                if (zIsAfterLast) {
                                    if (BluetoothOppService.this.mShares.size() != 0 && BluetoothOppService.V) {
                                        Log.v(BluetoothOppService.TAG, "Array update: trimming " + ((BluetoothOppShareInfo) BluetoothOppService.this.mShares.get(i2)).mId + " @ " + i2);
                                    }
                                    BluetoothOppService.this.deleteShare(i2);
                                } else {
                                    int i3 = cursorQuery.getInt(columnIndexOrThrow);
                                    if (i2 == BluetoothOppService.this.mShares.size()) {
                                        BluetoothOppService.this.insertShare(cursorQuery, i2);
                                        if (BluetoothOppService.V) {
                                            Log.v(BluetoothOppService.TAG, "Array update: inserting " + i3 + " @ " + i2);
                                        }
                                        i2++;
                                        cursorQuery.moveToNext();
                                        zIsAfterLast = cursorQuery.isAfterLast();
                                    } else {
                                        if (BluetoothOppService.this.mShares.size() != 0) {
                                            i = ((BluetoothOppShareInfo) BluetoothOppService.this.mShares.get(i2)).mId;
                                        } else {
                                            i = 0;
                                        }
                                        if (i < i3) {
                                            if (BluetoothOppService.V) {
                                                Log.v(BluetoothOppService.TAG, "Array update: removing " + i + " @ " + i2);
                                            }
                                            BluetoothOppService.this.deleteShare(i2);
                                        } else if (i == i3) {
                                            BluetoothOppService.this.updateShare(cursorQuery, i2);
                                            BluetoothOppService.this.scanFileIfNeeded(i2);
                                            i2++;
                                            cursorQuery.moveToNext();
                                            zIsAfterLast = cursorQuery.isAfterLast();
                                        } else {
                                            if (BluetoothOppService.V) {
                                                Log.v(BluetoothOppService.TAG, "Array update: appending " + i3 + " @ " + i2);
                                            }
                                            BluetoothOppService.this.insertShare(cursorQuery, i2);
                                            i2++;
                                            cursorQuery.moveToNext();
                                            zIsAfterLast = cursorQuery.isAfterLast();
                                        }
                                    }
                                }
                            }
                        }
                        BluetoothOppService.this.mNotifier.updateNotification();
                        cursorQuery.close();
                    } else {
                        BluetoothOppService.this.mUpdateThreadRunning = false;
                        throw new IllegalStateException("multiple UpdateThreads in BluetoothOppService");
                    }
                }
            }
            BluetoothOppService.this.mUpdateThreadRunning = false;
        }
    }

    private void insertShare(Cursor cursor, int i) {
        Uri uri;
        BluetoothOppSendFileInfo sendFileInfo;
        String string = cursor.getString(cursor.getColumnIndexOrThrow("uri"));
        if (string != null) {
            uri = Uri.parse(string);
            if (D) {
                Log.d(TAG, "insertShare parsed URI: " + uri);
            }
        } else {
            uri = null;
            if (D) {
                Log.e(TAG, "insertShare found null URI at cursor!");
            }
        }
        BluetoothOppShareInfo bluetoothOppShareInfo = new BluetoothOppShareInfo(cursor.getInt(cursor.getColumnIndexOrThrow("_id")), uri, cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.FILENAME_HINT)), cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare._DATA)), cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.MIMETYPE)), cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.DIRECTION)), cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.DESTINATION)), cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.VISIBILITY)), cursor.getInt(cursor.getColumnIndexOrThrow("confirm")), cursor.getInt(cursor.getColumnIndexOrThrow("status")), cursor.getLong(cursor.getColumnIndexOrThrow(BluetoothShare.TOTAL_BYTES)), cursor.getLong(cursor.getColumnIndexOrThrow(BluetoothShare.CURRENT_BYTES)), cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")), cursor.getInt(cursor.getColumnIndexOrThrow("scanned")) != 0);
        if (V) {
            Log.v(TAG, "Service adding new entry");
            Log.v(TAG, "ID      : " + bluetoothOppShareInfo.mId);
            Log.v(TAG, "URI     : " + bluetoothOppShareInfo.mUri);
            Log.v(TAG, "HINT    : " + bluetoothOppShareInfo.mHint);
            Log.v(TAG, "FILENAME: " + bluetoothOppShareInfo.mFilename);
            Log.v(TAG, "MIMETYPE: " + bluetoothOppShareInfo.mMimetype);
            Log.v(TAG, "DIRECTION: " + bluetoothOppShareInfo.mDirection);
            Log.v(TAG, "DESTINAT: " + bluetoothOppShareInfo.mDestination);
            Log.v(TAG, "VISIBILI: " + bluetoothOppShareInfo.mVisibility);
            Log.v(TAG, "CONFIRM : " + bluetoothOppShareInfo.mConfirm);
            Log.v(TAG, "STATUS  : " + bluetoothOppShareInfo.mStatus);
            Log.v(TAG, "TOTAL   : " + bluetoothOppShareInfo.mTotalBytes);
            Log.v(TAG, "CURRENT : " + bluetoothOppShareInfo.mCurrentBytes);
            Log.v(TAG, "TIMESTAMP : " + bluetoothOppShareInfo.mTimestamp);
            Log.v(TAG, "SCANNED : " + bluetoothOppShareInfo.mMediaScanned);
        }
        this.mShares.add(i, bluetoothOppShareInfo);
        if (bluetoothOppShareInfo.isObsolete()) {
            Constants.updateShareStatus(this, bluetoothOppShareInfo.mId, BluetoothShare.STATUS_UNKNOWN_ERROR);
        }
        if (bluetoothOppShareInfo.isReadyToStart()) {
            if (bluetoothOppShareInfo.mDirection == 0 && ((sendFileInfo = BluetoothOppUtility.getSendFileInfo(bluetoothOppShareInfo.mUri)) == null || sendFileInfo.mInputStream == null)) {
                Log.e(TAG, "Can't open file for OUTBOUND info " + bluetoothOppShareInfo.mId);
                Constants.updateShareStatus(this, bluetoothOppShareInfo.mId, BluetoothShare.STATUS_BAD_REQUEST);
                BluetoothOppUtility.closeSendFileInfo(bluetoothOppShareInfo.mUri);
                return;
            }
            if (this.mBatches.size() == 0) {
                BluetoothOppBatch bluetoothOppBatch = new BluetoothOppBatch(this, bluetoothOppShareInfo);
                bluetoothOppBatch.mId = this.mBatchId;
                this.mBatchId++;
                this.mBatches.add(bluetoothOppBatch);
                if (bluetoothOppShareInfo.mDirection == 0) {
                    if (V) {
                        Log.v(TAG, "Service create new Batch " + bluetoothOppBatch.mId + " for OUTBOUND info " + bluetoothOppShareInfo.mId);
                    }
                    this.mTransfer = new BluetoothOppTransfer(this, bluetoothOppBatch);
                } else if (bluetoothOppShareInfo.mDirection == 1) {
                    if (V) {
                        Log.v(TAG, "Service create new Batch " + bluetoothOppBatch.mId + " for INBOUND info " + bluetoothOppShareInfo.mId);
                    }
                    this.mServerTransfer = new BluetoothOppTransfer(this, bluetoothOppBatch, this.mServerSession);
                }
                if (bluetoothOppShareInfo.mDirection == 0 && this.mTransfer != null) {
                    if (V) {
                        Log.v(TAG, "Service start transfer new Batch " + bluetoothOppBatch.mId + " for info " + bluetoothOppShareInfo.mId);
                    }
                    this.mTransfer.start();
                    return;
                }
                if (bluetoothOppShareInfo.mDirection == 1 && this.mServerTransfer != null) {
                    if (V) {
                        Log.v(TAG, "Service start server transfer new Batch " + bluetoothOppBatch.mId + " for info " + bluetoothOppShareInfo.mId);
                    }
                    this.mServerTransfer.start();
                    return;
                }
                return;
            }
            int iFindBatchWithTimeStamp = findBatchWithTimeStamp(bluetoothOppShareInfo.mTimestamp);
            if (iFindBatchWithTimeStamp != -1) {
                if (V) {
                    Log.v(TAG, "Service add info " + bluetoothOppShareInfo.mId + " to existing batch " + this.mBatches.get(iFindBatchWithTimeStamp).mId);
                }
                this.mBatches.get(iFindBatchWithTimeStamp).addShare(bluetoothOppShareInfo);
                return;
            }
            BluetoothOppBatch bluetoothOppBatch2 = new BluetoothOppBatch(this, bluetoothOppShareInfo);
            bluetoothOppBatch2.mId = this.mBatchId;
            this.mBatchId++;
            this.mBatches.add(bluetoothOppBatch2);
            if (V) {
                Log.v(TAG, "Service add new Batch " + bluetoothOppBatch2.mId + " for info " + bluetoothOppShareInfo.mId);
            }
        }
    }

    private void updateShare(Cursor cursor, int i) {
        BluetoothOppShareInfo bluetoothOppShareInfo = this.mShares.get(i);
        int columnIndexOrThrow = cursor.getColumnIndexOrThrow("status");
        bluetoothOppShareInfo.mId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
        if (bluetoothOppShareInfo.mUri != null) {
            bluetoothOppShareInfo.mUri = Uri.parse(stringFromCursor(bluetoothOppShareInfo.mUri.toString(), cursor, "uri"));
        } else if (D) {
            Log.w(TAG, "updateShare() called for ID " + bluetoothOppShareInfo.mId + " with null URI");
        }
        bluetoothOppShareInfo.mHint = stringFromCursor(bluetoothOppShareInfo.mHint, cursor, BluetoothShare.FILENAME_HINT);
        bluetoothOppShareInfo.mFilename = stringFromCursor(bluetoothOppShareInfo.mFilename, cursor, BluetoothShare._DATA);
        bluetoothOppShareInfo.mMimetype = stringFromCursor(bluetoothOppShareInfo.mMimetype, cursor, BluetoothShare.MIMETYPE);
        bluetoothOppShareInfo.mDirection = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.DIRECTION));
        bluetoothOppShareInfo.mDestination = stringFromCursor(bluetoothOppShareInfo.mDestination, cursor, BluetoothShare.DESTINATION);
        int i2 = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.VISIBILITY));
        int i3 = cursor.getInt(cursor.getColumnIndexOrThrow("confirm"));
        if (bluetoothOppShareInfo.mVisibility == 0 && i2 != 0 && (BluetoothShare.isStatusCompleted(bluetoothOppShareInfo.mStatus) || i3 == 0)) {
            this.mNotifier.mNotificationMgr.cancel(bluetoothOppShareInfo.mId);
        }
        bluetoothOppShareInfo.mVisibility = i2;
        boolean z = bluetoothOppShareInfo.mConfirm == 0 && i3 != 0;
        bluetoothOppShareInfo.mConfirm = cursor.getInt(cursor.getColumnIndexOrThrow("confirm"));
        int i4 = cursor.getInt(columnIndexOrThrow);
        if (BluetoothShare.isStatusCompleted(bluetoothOppShareInfo.mStatus)) {
            this.mNotifier.mNotificationMgr.cancel(bluetoothOppShareInfo.mId);
        }
        bluetoothOppShareInfo.mStatus = i4;
        bluetoothOppShareInfo.mTotalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(BluetoothShare.TOTAL_BYTES));
        bluetoothOppShareInfo.mCurrentBytes = cursor.getLong(cursor.getColumnIndexOrThrow(BluetoothShare.CURRENT_BYTES));
        bluetoothOppShareInfo.mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
        bluetoothOppShareInfo.mMediaScanned = cursor.getInt(cursor.getColumnIndexOrThrow("scanned")) != 0;
        if (z) {
            if (V) {
                Log.v(TAG, "Service handle info " + bluetoothOppShareInfo.mId + " confirmation updated");
            }
            int iFindBatchWithTimeStamp = findBatchWithTimeStamp(bluetoothOppShareInfo.mTimestamp);
            if (iFindBatchWithTimeStamp != -1) {
                BluetoothOppBatch bluetoothOppBatch = this.mBatches.get(iFindBatchWithTimeStamp);
                if (this.mServerTransfer != null && bluetoothOppBatch.mId == this.mServerTransfer.getBatchId()) {
                    this.mServerTransfer.confirmStatusChanged();
                }
            }
        }
        int iFindBatchWithTimeStamp2 = findBatchWithTimeStamp(bluetoothOppShareInfo.mTimestamp);
        if (iFindBatchWithTimeStamp2 != -1) {
            BluetoothOppBatch bluetoothOppBatch2 = this.mBatches.get(iFindBatchWithTimeStamp2);
            if (bluetoothOppBatch2.mStatus == 2 || bluetoothOppBatch2.mStatus == 3) {
                if (V) {
                    Log.v(TAG, "Batch " + bluetoothOppBatch2.mId + " is finished");
                }
                if (bluetoothOppBatch2.mDirection == 0) {
                    if (this.mTransfer == null) {
                        Log.e(TAG, "Unexpected error! mTransfer is null");
                    } else if (bluetoothOppBatch2.mId == this.mTransfer.getBatchId()) {
                        this.mTransfer.stop();
                    } else {
                        Log.e(TAG, "Unexpected error! batch id " + bluetoothOppBatch2.mId + " doesn't match mTransfer id " + this.mTransfer.getBatchId());
                    }
                    this.mTransfer = null;
                } else {
                    if (this.mServerTransfer == null) {
                        Log.e(TAG, "Unexpected error! mServerTransfer is null");
                    } else if (bluetoothOppBatch2.mId == this.mServerTransfer.getBatchId()) {
                        this.mServerTransfer.stop();
                    } else {
                        Log.e(TAG, "Unexpected error! batch id " + bluetoothOppBatch2.mId + " doesn't match mServerTransfer id " + this.mServerTransfer.getBatchId());
                    }
                    this.mServerTransfer = null;
                }
                removeBatch(bluetoothOppBatch2);
            }
        }
    }

    private void deleteShare(int i) {
        BluetoothOppShareInfo bluetoothOppShareInfo = this.mShares.get(i);
        int iFindBatchWithTimeStamp = findBatchWithTimeStamp(bluetoothOppShareInfo.mTimestamp);
        if (iFindBatchWithTimeStamp != -1) {
            BluetoothOppBatch bluetoothOppBatch = this.mBatches.get(iFindBatchWithTimeStamp);
            if (bluetoothOppBatch.hasShare(bluetoothOppShareInfo)) {
                if (V) {
                    Log.v(TAG, "Service cancel batch for share " + bluetoothOppShareInfo.mId);
                }
                bluetoothOppBatch.cancelBatch();
            }
            if (bluetoothOppBatch.isEmpty()) {
                if (V) {
                    Log.v(TAG, "Service remove batch  " + bluetoothOppBatch.mId);
                }
                removeBatch(bluetoothOppBatch);
            }
        }
        this.mShares.remove(i);
    }

    private String stringFromCursor(String str, Cursor cursor, String str2) {
        int columnIndexOrThrow = cursor.getColumnIndexOrThrow(str2);
        if (str == null) {
            return cursor.getString(columnIndexOrThrow);
        }
        if (this.mNewChars == null) {
            this.mNewChars = new CharArrayBuffer(128);
        }
        cursor.copyStringToBuffer(columnIndexOrThrow, this.mNewChars);
        int i = this.mNewChars.sizeCopied;
        if (i != str.length()) {
            return cursor.getString(columnIndexOrThrow);
        }
        if (this.mOldChars == null || this.mOldChars.sizeCopied < i) {
            this.mOldChars = new CharArrayBuffer(i);
        }
        char[] cArr = this.mOldChars.data;
        char[] cArr2 = this.mNewChars.data;
        str.getChars(0, i, cArr, 0);
        for (int i2 = i - 1; i2 >= 0; i2--) {
            if (cArr[i2] != cArr2[i2]) {
                return new String(cArr2, 0, i);
            }
        }
        return str;
    }

    private int findBatchWithTimeStamp(long j) {
        for (int size = this.mBatches.size() - 1; size >= 0; size--) {
            if (this.mBatches.get(size).mTimestamp == j) {
                return size;
            }
        }
        return -1;
    }

    private void removeBatch(BluetoothOppBatch bluetoothOppBatch) {
        if (V) {
            Log.v(TAG, "Remove batch " + bluetoothOppBatch.mId);
        }
        this.mBatches.remove(bluetoothOppBatch);
        if (this.mBatches.size() > 0) {
            for (BluetoothOppBatch bluetoothOppBatch2 : this.mBatches) {
                if (bluetoothOppBatch2.mStatus == 1) {
                    return;
                }
                if (bluetoothOppBatch2.mDirection == 0) {
                    if (V) {
                        Log.v(TAG, "Start pending outbound batch " + bluetoothOppBatch2.mId);
                    }
                    this.mTransfer = new BluetoothOppTransfer(this, bluetoothOppBatch2);
                    this.mTransfer.start();
                    return;
                }
                if (bluetoothOppBatch2.mDirection == 1 && this.mServerSession != null) {
                    if (V) {
                        Log.v(TAG, "Start pending inbound batch " + bluetoothOppBatch2.mId);
                    }
                    this.mServerTransfer = new BluetoothOppTransfer(this, bluetoothOppBatch2, this.mServerSession);
                    this.mServerTransfer.start();
                    if (bluetoothOppBatch2.getPendingShare() != null && bluetoothOppBatch2.getPendingShare().mConfirm == 1) {
                        this.mServerTransfer.confirmStatusChanged();
                        return;
                    }
                    return;
                }
            }
        }
    }

    private void scanFileIfNeeded(int i) {
        boolean z;
        BluetoothOppShareInfo bluetoothOppShareInfo = this.mShares.get(i);
        if (!BluetoothShare.isStatusSuccess(bluetoothOppShareInfo.mStatus) || bluetoothOppShareInfo.mDirection != 1 || bluetoothOppShareInfo.mMediaScanned || bluetoothOppShareInfo.mConfirm == 5) {
            z = false;
        } else {
            z = true;
        }
        if (!z) {
            return;
        }
        synchronized (this) {
            if (D) {
                Log.d(TAG, "Scanning file " + bluetoothOppShareInfo.mFilename);
            }
            if (!this.mMediaScanInProgress) {
                this.mMediaScanInProgress = true;
                new MediaScannerNotifier(this, bluetoothOppShareInfo, this.mHandler);
            }
        }
    }

    private static void trimDatabase(ContentResolver contentResolver) {
        try {
            int iDelete = contentResolver.delete(BluetoothShare.CONTENT_URI, WHERE_INVISIBLE_UNCONFIRMED, null);
            if (V) {
                Log.v(TAG, "Deleted shares, number = " + iDelete);
            }
            Cursor cursorQuery = contentResolver.query(BluetoothShare.CONTENT_URI, new String[]{"_id"}, WHERE_INBOUND_SUCCESS, null, "_id");
            if (cursorQuery == null) {
                return;
            }
            int count = cursorQuery.getCount();
            if (count > 50 && cursorQuery.moveToPosition(count - 50)) {
                long j = cursorQuery.getLong(cursorQuery.getColumnIndexOrThrow("_id"));
                int iDelete2 = contentResolver.delete(BluetoothShare.CONTENT_URI, "_id < " + j, null);
                if (V) {
                    Log.v(TAG, "Deleted old inbound success share: " + iDelete2);
                }
            }
            cursorQuery.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private static class MediaScannerNotifier implements MediaScannerConnection.MediaScannerConnectionClient {
        private Handler mCallback;
        private MediaScannerConnection mConnection;
        private Context mContext;
        private BluetoothOppShareInfo mInfo;

        MediaScannerNotifier(Context context, BluetoothOppShareInfo bluetoothOppShareInfo, Handler handler) {
            this.mContext = context;
            this.mInfo = bluetoothOppShareInfo;
            this.mCallback = handler;
            this.mConnection = new MediaScannerConnection(this.mContext, this);
            if (BluetoothOppService.V) {
                Log.v(BluetoothOppService.TAG, "Connecting to MediaScannerConnection ");
            }
            this.mConnection.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            if (BluetoothOppService.V) {
                Log.v(BluetoothOppService.TAG, "MediaScannerConnection onMediaScannerConnected");
            }
            this.mConnection.scanFile(this.mInfo.mFilename, this.mInfo.mMimetype);
        }

        @Override
        public void onScanCompleted(String str, Uri uri) {
            try {
                try {
                    if (BluetoothOppService.V) {
                        Log.v(BluetoothOppService.TAG, "MediaScannerConnection onScanCompleted");
                        Log.v(BluetoothOppService.TAG, "MediaScannerConnection path is " + str);
                        Log.v(BluetoothOppService.TAG, "MediaScannerConnection Uri is " + uri);
                    }
                    if (uri != null) {
                        Message messageObtain = Message.obtain();
                        messageObtain.setTarget(this.mCallback);
                        messageObtain.what = 2;
                        messageObtain.arg1 = this.mInfo.mId;
                        messageObtain.obj = uri;
                        messageObtain.sendToTarget();
                    } else {
                        Message messageObtain2 = Message.obtain();
                        messageObtain2.setTarget(this.mCallback);
                        messageObtain2.what = 3;
                        messageObtain2.arg1 = this.mInfo.mId;
                        messageObtain2.sendToTarget();
                    }
                } catch (Exception e) {
                    Log.v(BluetoothOppService.TAG, "!!!MediaScannerConnection exception: " + e);
                    if (BluetoothOppService.V) {
                    }
                }
                if (BluetoothOppService.V) {
                    Log.v(BluetoothOppService.TAG, "MediaScannerConnection disconnect");
                }
                this.mConnection.disconnect();
            } catch (Throwable th) {
                if (BluetoothOppService.V) {
                    Log.v(BluetoothOppService.TAG, "MediaScannerConnection disconnect");
                }
                this.mConnection.disconnect();
                throw th;
            }
        }
    }

    public static BluetoothOppService getInstance() {
        Log.d(TAG, "getInstance ++");
        return mInstance;
    }

    private void stopListeners() {
        if (this.mAdapter != null && this.mOppSdpHandle >= 0 && SdpManager.getDefaultManager() != null) {
            if (D) {
                Log.d(TAG, "Removing SDP record mOppSdpHandle :" + this.mOppSdpHandle);
            }
            Log.d(TAG, "RemoveSDPrecord returns " + SdpManager.getDefaultManager().removeSdpRecord(this.mOppSdpHandle));
            this.mOppSdpHandle = -1;
        }
        if (this.mServerSocket != null) {
            this.mServerSocket.shutdown(false);
            this.mServerSocket = null;
        }
        if (D) {
            Log.d(TAG, "stopListeners: mServerSocket is null");
        }
    }

    @Override
    public boolean onConnect(BluetoothDevice bluetoothDevice, BluetoothSocket bluetoothSocket) {
        if (D) {
            Log.d(TAG, " onConnect BluetoothSocket :" + bluetoothSocket + " \n :device :" + bluetoothDevice);
        }
        if (!this.mAcceptNewConnections) {
            Log.d(TAG, " onConnect BluetoothSocket :" + bluetoothSocket + " rejected");
            return false;
        }
        BluetoothObexTransport bluetoothObexTransport = new BluetoothObexTransport(bluetoothSocket);
        Message messageObtainMessage = this.mHandler.obtainMessage(100);
        messageObtainMessage.obj = bluetoothObexTransport;
        messageObtainMessage.sendToTarget();
        this.mAcceptNewConnections = false;
        return true;
    }

    @Override
    public void onAcceptFailed() {
        Log.d(TAG, " onAcceptFailed:");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1));
    }

    void acceptNewConnections() {
        this.mAcceptNewConnections = true;
    }

    private void clearPendingTask(Context context) {
        Cursor cursorQuery;
        try {
            cursorQuery = context.getContentResolver().query(BluetoothShare.CONTENT_URI, null, "confirm == '0' AND (visibility IS NULL OR visibility == '0')", null, "_id");
        } catch (Exception e) {
            Log.e(TAG, "SQLite exception occur : " + e.toString());
            cursorQuery = null;
        }
        if (cursorQuery == null) {
            return;
        }
        cursorQuery.moveToFirst();
        while (!cursorQuery.isAfterLast()) {
            try {
                int i = cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("_id"));
                if (V) {
                    Log.d(TAG, "Clear pending task id = " + i);
                }
                Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + i);
                ContentValues contentValues = new ContentValues();
                contentValues.put("confirm", (Integer) 3);
                context.getContentResolver().update(uri, contentValues, null, null);
                cursorQuery.moveToNext();
            } catch (CursorIndexOutOfBoundsException e2) {
                Log.e(TAG, "SQLite exception occur : " + e2.toString());
                return;
            }
        }
    }
}
