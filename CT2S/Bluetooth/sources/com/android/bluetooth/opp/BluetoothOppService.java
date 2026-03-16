package com.android.bluetooth.opp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;
import com.android.bluetooth.hfp.BluetoothCmeError;
import com.google.android.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import javax.obex.ObexTransport;

public class BluetoothOppService extends Service {
    private static final boolean D = true;
    private static final int MEDIA_SCANNED = 2;
    private static final int MEDIA_SCANNED_FAILED = 3;
    private static final int MSG_INCOMING_CONNECTION_RETRY = 4;
    private static final int START_LISTENER = 1;
    private static final int STOP_LISTENER = 200;
    private static final String TAG = "BtOppService";
    private static final boolean V = false;
    private BluetoothAdapter mAdapter;
    private int mBatchId;
    private ArrayList<BluetoothOppBatch> mBatchs;
    private boolean mMediaScanInProgress;
    private CharArrayBuffer mNewChars;
    private BluetoothOppNotification mNotifier;
    private BluetoothShareContentObserver mObserver;
    private CharArrayBuffer mOldChars;
    private boolean mPendingUpdate;
    private PowerManager mPowerManager;
    private BluetoothOppObexServerSession mServerSession;
    private BluetoothOppTransfer mServerTransfer;
    private ArrayList<BluetoothOppShareInfo> mShares;
    private BluetoothOppRfcommListener mSocketListener;
    private BluetoothOppTransfer mTransfer;
    private UpdateThread mUpdateThread;
    private boolean userAccepted = false;
    private boolean mListenStarted = false;
    private int mIncomingRetries = 0;
    private ObexTransport mPendingConnection = null;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (BluetoothOppService.this.mAdapter.isEnabled()) {
                        BluetoothOppService.this.startSocketListener();
                        return;
                    }
                    return;
                case 2:
                    ContentValues updateValues = new ContentValues();
                    Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + msg.arg1);
                    updateValues.put(Constants.MEDIA_SCANNED, (Integer) 1);
                    updateValues.put("uri", msg.obj.toString());
                    updateValues.put(BluetoothShare.MIMETYPE, BluetoothOppService.this.getContentResolver().getType(Uri.parse(msg.obj.toString())));
                    BluetoothOppService.this.getContentResolver().update(contentUri, updateValues, null, null);
                    synchronized (BluetoothOppService.this) {
                        BluetoothOppService.this.mMediaScanInProgress = false;
                        break;
                    }
                    return;
                case 3:
                    Log.v(BluetoothOppService.TAG, "Update mInfo.id " + msg.arg1 + " for MEDIA_SCANNED_FAILED");
                    ContentValues updateValues1 = new ContentValues();
                    Uri contentUri1 = Uri.parse(BluetoothShare.CONTENT_URI + "/" + msg.arg1);
                    updateValues1.put(Constants.MEDIA_SCANNED, (Integer) 2);
                    BluetoothOppService.this.getContentResolver().update(contentUri1, updateValues1, null, null);
                    synchronized (BluetoothOppService.this) {
                        BluetoothOppService.this.mMediaScanInProgress = false;
                        break;
                    }
                    return;
                case 4:
                    if (BluetoothOppService.this.mBatchs.size() != 0) {
                        if (BluetoothOppService.this.mIncomingRetries == 20) {
                            Log.w(BluetoothOppService.TAG, "Retried 20 seconds, reject connection");
                            try {
                                BluetoothOppService.this.mPendingConnection.close();
                                break;
                            } catch (IOException e) {
                                Log.e(BluetoothOppService.TAG, "close tranport error");
                            }
                            BluetoothOppService.this.mIncomingRetries = 0;
                            BluetoothOppService.this.mPendingConnection = null;
                            return;
                        }
                        Log.i(BluetoothOppService.TAG, "OPP busy! Retry after 1 second");
                        BluetoothOppService.this.mIncomingRetries++;
                        Message msg2 = Message.obtain(BluetoothOppService.this.mHandler);
                        msg2.what = 4;
                        BluetoothOppService.this.mHandler.sendMessageDelayed(msg2, 1000L);
                        return;
                    }
                    Log.i(BluetoothOppService.TAG, "Start Obex Server");
                    BluetoothOppService.this.createServerSession(BluetoothOppService.this.mPendingConnection);
                    BluetoothOppService.this.mIncomingRetries = 0;
                    BluetoothOppService.this.mPendingConnection = null;
                    return;
                case 100:
                    Log.d(BluetoothOppService.TAG, "Get incoming connection");
                    ObexTransport transport = (ObexTransport) msg.obj;
                    if (BluetoothOppService.this.mBatchs.size() != 0 || BluetoothOppService.this.mPendingConnection != null) {
                        if (BluetoothOppService.this.mPendingConnection != null) {
                            Log.w(BluetoothOppService.TAG, "OPP busy! Reject connection");
                            try {
                                transport.close();
                                return;
                            } catch (IOException e2) {
                                Log.e(BluetoothOppService.TAG, "close tranport error");
                                return;
                            }
                        }
                        Log.i(BluetoothOppService.TAG, "OPP busy! Retry after 1 second");
                        BluetoothOppService.this.mIncomingRetries++;
                        BluetoothOppService.this.mPendingConnection = transport;
                        Message msg1 = Message.obtain(BluetoothOppService.this.mHandler);
                        msg1.what = 4;
                        BluetoothOppService.this.mHandler.sendMessageDelayed(msg1, 1000L);
                        return;
                    }
                    Log.i(BluetoothOppService.TAG, "Start Obex Server");
                    BluetoothOppService.this.createServerSession(transport);
                    return;
                case 200:
                    if (BluetoothOppService.this.mSocketListener != null) {
                        BluetoothOppService.this.mSocketListener.stop();
                    }
                    BluetoothOppService.this.mListenStarted = false;
                    if (BluetoothOppService.this.mServerTransfer != null) {
                        BluetoothOppService.this.mServerTransfer.onBatchCanceled();
                        BluetoothOppService.this.mServerTransfer = null;
                    }
                    if (BluetoothOppService.this.mTransfer != null) {
                        BluetoothOppService.this.mTransfer.onBatchCanceled();
                        BluetoothOppService.this.mTransfer = null;
                    }
                    synchronized (BluetoothOppService.this) {
                        if (BluetoothOppService.this.mUpdateThread == null) {
                            BluetoothOppService.this.stopSelf();
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                switch (intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE)) {
                    case 12:
                        BluetoothOppService.this.startSocketListener();
                        break;
                    case BluetoothCmeError.SIM_FAILURE:
                        BluetoothOppService.this.mHandler.sendMessage(BluetoothOppService.this.mHandler.obtainMessage(200));
                        break;
                }
            }
        }
    };

    private class BluetoothShareContentObserver extends ContentObserver {
        public BluetoothShareContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            BluetoothOppService.this.updateFromProvider();
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        throw new UnsupportedOperationException("Cannot bind to Bluetooth OPP Service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mSocketListener = new BluetoothOppRfcommListener(this.mAdapter);
        this.mShares = Lists.newArrayList();
        this.mBatchs = Lists.newArrayList();
        this.mObserver = new BluetoothShareContentObserver();
        getContentResolver().registerContentObserver(BluetoothShare.CONTENT_URI, true, this.mObserver);
        this.mBatchId = 1;
        this.mNotifier = new BluetoothOppNotification(this);
        this.mNotifier.mNotificationMgr.cancelAll();
        this.mNotifier.updateNotification();
        final ContentResolver contentResolver = getContentResolver();
        new Thread("trimDatabase") {
            @Override
            public void run() {
                BluetoothOppService.trimDatabase(contentResolver);
            }
        }.start();
        IntentFilter filter = new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED");
        registerReceiver(this.mBluetoothReceiver, filter);
        synchronized (this) {
            if (this.mAdapter == null) {
                Log.w(TAG, "Local BT device is not enabled");
            } else {
                startListener();
            }
        }
        updateFromProvider();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (this.mAdapter == null) {
            Log.w(TAG, "Local BT device is not enabled");
        } else {
            startListener();
        }
        updateFromProvider();
        return 2;
    }

    private void startListener() {
        if (!this.mListenStarted && this.mAdapter.isEnabled()) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1));
            this.mListenStarted = true;
        }
    }

    private void startSocketListener() {
        this.mSocketListener.start(this.mHandler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(this.mObserver);
        unregisterReceiver(this.mBluetoothReceiver);
        this.mSocketListener.stop();
        if (this.mBatchs != null) {
            this.mBatchs.clear();
        }
        if (this.mShares != null) {
            this.mShares.clear();
        }
        if (this.mHandler != null) {
            this.mHandler.removeCallbacksAndMessages(null);
        }
    }

    private void createServerSession(ObexTransport transport) {
        this.mServerSession = new BluetoothOppObexServerSession(this, transport);
        this.mServerSession.preStart();
        Log.d(TAG, "Get ServerSession " + this.mServerSession.toString() + " for incoming connection" + transport.toString());
    }

    private void updateFromProvider() {
        synchronized (this) {
            this.mPendingUpdate = true;
            if (this.mUpdateThread == null) {
                this.mUpdateThread = new UpdateThread();
                this.mUpdateThread.start();
            }
        }
    }

    private class UpdateThread extends Thread {
        public UpdateThread() {
            super("Bluetooth Share Service");
        }

        @Override
        public void run() {
            Process.setThreadPriority(10);
            boolean keepService = false;
            while (true) {
                synchronized (BluetoothOppService.this) {
                    if (BluetoothOppService.this.mUpdateThread == this) {
                        if (!BluetoothOppService.this.mPendingUpdate) {
                            BluetoothOppService.this.mUpdateThread = null;
                            if (!keepService && !BluetoothOppService.this.mListenStarted) {
                                BluetoothOppService.this.stopSelf();
                                return;
                            }
                            return;
                        }
                        BluetoothOppService.this.mPendingUpdate = false;
                        Cursor cursor = BluetoothOppService.this.getContentResolver().query(BluetoothShare.CONTENT_URI, null, null, null, "_id");
                        if (cursor != null) {
                            cursor.moveToFirst();
                            int arrayPos = 0;
                            keepService = false;
                            boolean isAfterLast = cursor.isAfterLast();
                            int idColumn = cursor.getColumnIndexOrThrow("_id");
                            while (true) {
                                if (!isAfterLast || arrayPos < BluetoothOppService.this.mShares.size()) {
                                    if (isAfterLast) {
                                        if (BluetoothOppService.this.mShares.size() != 0) {
                                        }
                                        if (BluetoothOppService.this.shouldScanFile(arrayPos)) {
                                            BluetoothOppService.this.scanFile(null, arrayPos);
                                        }
                                        BluetoothOppService.this.deleteShare(arrayPos);
                                    } else {
                                        int id = cursor.getInt(idColumn);
                                        if (arrayPos == BluetoothOppService.this.mShares.size()) {
                                            BluetoothOppService.this.insertShare(cursor, arrayPos);
                                            if (BluetoothOppService.this.shouldScanFile(arrayPos) && !BluetoothOppService.this.scanFile(cursor, arrayPos)) {
                                                keepService = true;
                                            }
                                            if (BluetoothOppService.this.visibleNotification(arrayPos)) {
                                                keepService = true;
                                            }
                                            if (BluetoothOppService.this.needAction(arrayPos)) {
                                                keepService = true;
                                            }
                                            arrayPos++;
                                            cursor.moveToNext();
                                            isAfterLast = cursor.isAfterLast();
                                        } else {
                                            int arrayId = 0;
                                            if (BluetoothOppService.this.mShares.size() != 0) {
                                                arrayId = ((BluetoothOppShareInfo) BluetoothOppService.this.mShares.get(arrayPos)).mId;
                                            }
                                            if (arrayId < id) {
                                                if (BluetoothOppService.this.shouldScanFile(arrayPos)) {
                                                    BluetoothOppService.this.scanFile(null, arrayPos);
                                                }
                                                BluetoothOppService.this.deleteShare(arrayPos);
                                            } else if (arrayId == id) {
                                                BluetoothOppService.this.updateShare(cursor, arrayPos, BluetoothOppService.this.userAccepted);
                                                if (BluetoothOppService.this.shouldScanFile(arrayPos) && !BluetoothOppService.this.scanFile(cursor, arrayPos)) {
                                                    keepService = true;
                                                }
                                                if (BluetoothOppService.this.visibleNotification(arrayPos)) {
                                                    keepService = true;
                                                }
                                                if (BluetoothOppService.this.needAction(arrayPos)) {
                                                    keepService = true;
                                                }
                                                arrayPos++;
                                                cursor.moveToNext();
                                                isAfterLast = cursor.isAfterLast();
                                            } else {
                                                BluetoothOppService.this.insertShare(cursor, arrayPos);
                                                if (BluetoothOppService.this.shouldScanFile(arrayPos) && !BluetoothOppService.this.scanFile(cursor, arrayPos)) {
                                                    keepService = true;
                                                }
                                                if (BluetoothOppService.this.visibleNotification(arrayPos)) {
                                                    keepService = true;
                                                }
                                                if (BluetoothOppService.this.needAction(arrayPos)) {
                                                    keepService = true;
                                                }
                                                arrayPos++;
                                                cursor.moveToNext();
                                                isAfterLast = cursor.isAfterLast();
                                            }
                                        }
                                    }
                                }
                            }
                            BluetoothOppService.this.mNotifier.updateNotification();
                            cursor.close();
                        } else {
                            return;
                        }
                    } else {
                        throw new IllegalStateException("multiple UpdateThreads in BluetoothOppService");
                    }
                }
            }
        }
    }

    private void insertShare(Cursor cursor, int arrayPos) {
        Uri uri;
        BluetoothOppSendFileInfo sendFileInfo;
        String uriString = cursor.getString(cursor.getColumnIndexOrThrow("uri"));
        if (uriString != null) {
            uri = Uri.parse(uriString);
            Log.d(TAG, "insertShare parsed URI: " + uri);
        } else {
            uri = null;
            Log.e(TAG, "insertShare found null URI at cursor!");
        }
        BluetoothOppShareInfo info = new BluetoothOppShareInfo(cursor.getInt(cursor.getColumnIndexOrThrow("_id")), uri, cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.FILENAME_HINT)), cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare._DATA)), cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.MIMETYPE)), cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.DIRECTION)), cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.DESTINATION)), cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.VISIBILITY)), cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.USER_CONFIRMATION)), cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.STATUS)), cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.TOTAL_BYTES)), cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.CURRENT_BYTES)), cursor.getInt(cursor.getColumnIndexOrThrow("timestamp")), cursor.getInt(cursor.getColumnIndexOrThrow(Constants.MEDIA_SCANNED)) != 0);
        this.mShares.add(arrayPos, info);
        if (info.isObsolete()) {
            Constants.updateShareStatus(this, info.mId, BluetoothShare.STATUS_UNKNOWN_ERROR);
        }
        if (info.isReadyToStart()) {
            if (info.mDirection == 0 && ((sendFileInfo = BluetoothOppUtility.getSendFileInfo(info.mUri)) == null || sendFileInfo.mInputStream == null)) {
                Log.e(TAG, "Can't open file for OUTBOUND info " + info.mId);
                Constants.updateShareStatus(this, info.mId, BluetoothShare.STATUS_BAD_REQUEST);
                BluetoothOppUtility.closeSendFileInfo(info.mUri);
                return;
            }
            if (this.mBatchs.size() == 0) {
                BluetoothOppBatch newBatch = new BluetoothOppBatch(this, info);
                newBatch.mId = this.mBatchId;
                this.mBatchId++;
                this.mBatchs.add(newBatch);
                if (info.mDirection == 0) {
                    this.mTransfer = new BluetoothOppTransfer(this, this.mPowerManager, newBatch);
                } else if (info.mDirection == 1) {
                    this.mServerTransfer = new BluetoothOppTransfer(this, this.mPowerManager, newBatch, this.mServerSession);
                }
                if (info.mDirection == 0 && this.mTransfer != null) {
                    this.mTransfer.start();
                    return;
                } else {
                    if (info.mDirection == 1 && this.mServerTransfer != null) {
                        this.mServerTransfer.start();
                        return;
                    }
                    return;
                }
            }
            int i = findBatchWithTimeStamp(info.mTimestamp);
            if (i != -1) {
                this.mBatchs.get(i).addShare(info);
                return;
            }
            BluetoothOppBatch newBatch2 = new BluetoothOppBatch(this, info);
            newBatch2.mId = this.mBatchId;
            this.mBatchId++;
            this.mBatchs.add(newBatch2);
        }
    }

    private void updateShare(Cursor cursor, int arrayPos, boolean userAccepted) {
        int i;
        BluetoothOppShareInfo info = this.mShares.get(arrayPos);
        int statusColumn = cursor.getColumnIndexOrThrow(BluetoothShare.STATUS);
        info.mId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
        if (info.mUri != null) {
            info.mUri = Uri.parse(stringFromCursor(info.mUri.toString(), cursor, "uri"));
        } else {
            Log.w(TAG, "updateShare() called for ID " + info.mId + " with null URI");
        }
        info.mHint = stringFromCursor(info.mHint, cursor, BluetoothShare.FILENAME_HINT);
        info.mFilename = stringFromCursor(info.mFilename, cursor, BluetoothShare._DATA);
        info.mMimetype = stringFromCursor(info.mMimetype, cursor, BluetoothShare.MIMETYPE);
        info.mDirection = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.DIRECTION));
        info.mDestination = stringFromCursor(info.mDestination, cursor, BluetoothShare.DESTINATION);
        int newVisibility = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.VISIBILITY));
        boolean confirmUpdated = false;
        int newConfirm = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.USER_CONFIRMATION));
        if (info.mVisibility == 0 && newVisibility != 0 && (BluetoothShare.isStatusCompleted(info.mStatus) || newConfirm == 0)) {
            this.mNotifier.mNotificationMgr.cancel(info.mId);
        }
        info.mVisibility = newVisibility;
        if (info.mConfirm == 0 && newConfirm != 0) {
            confirmUpdated = true;
        }
        info.mConfirm = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.USER_CONFIRMATION));
        int newStatus = cursor.getInt(statusColumn);
        if (!BluetoothShare.isStatusCompleted(info.mStatus) && BluetoothShare.isStatusCompleted(newStatus)) {
            this.mNotifier.mNotificationMgr.cancel(info.mId);
        }
        info.mStatus = newStatus;
        info.mTotalBytes = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.TOTAL_BYTES));
        info.mCurrentBytes = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.CURRENT_BYTES));
        info.mTimestamp = cursor.getInt(cursor.getColumnIndexOrThrow("timestamp"));
        info.mMediaScanned = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.MEDIA_SCANNED)) != 0;
        if (confirmUpdated && (i = findBatchWithTimeStamp(info.mTimestamp)) != -1) {
            BluetoothOppBatch batch = this.mBatchs.get(i);
            if (this.mServerTransfer != null && batch.mId == this.mServerTransfer.getBatchId()) {
                this.mServerTransfer.confirmStatusChanged();
            }
        }
        int i2 = findBatchWithTimeStamp(info.mTimestamp);
        if (i2 != -1) {
            BluetoothOppBatch batch2 = this.mBatchs.get(i2);
            if (batch2.mStatus == 2 || batch2.mStatus == 3) {
                if (batch2.mDirection == 0) {
                    if (this.mTransfer == null) {
                        Log.e(TAG, "Unexpected error! mTransfer is null");
                    } else if (batch2.mId == this.mTransfer.getBatchId()) {
                        this.mTransfer.stop();
                    } else {
                        Log.e(TAG, "Unexpected error! batch id " + batch2.mId + " doesn't match mTransfer id " + this.mTransfer.getBatchId());
                    }
                    this.mTransfer = null;
                } else {
                    if (this.mServerTransfer == null) {
                        Log.e(TAG, "Unexpected error! mServerTransfer is null");
                    } else if (batch2.mId == this.mServerTransfer.getBatchId()) {
                        this.mServerTransfer.stop();
                    } else {
                        Log.e(TAG, "Unexpected error! batch id " + batch2.mId + " doesn't match mServerTransfer id " + this.mServerTransfer.getBatchId());
                    }
                    this.mServerTransfer = null;
                }
                removeBatch(batch2);
            }
        }
    }

    private void deleteShare(int arrayPos) {
        BluetoothOppShareInfo info = this.mShares.get(arrayPos);
        int i = findBatchWithTimeStamp(info.mTimestamp);
        if (i != -1) {
            BluetoothOppBatch batch = this.mBatchs.get(i);
            if (batch.hasShare(info)) {
                batch.cancelBatch();
            }
            if (batch.isEmpty()) {
                removeBatch(batch);
            }
        }
        this.mShares.remove(arrayPos);
    }

    private String stringFromCursor(String old, Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        if (old == null) {
            return cursor.getString(index);
        }
        if (this.mNewChars == null) {
            this.mNewChars = new CharArrayBuffer(128);
        }
        cursor.copyStringToBuffer(index, this.mNewChars);
        int length = this.mNewChars.sizeCopied;
        if (length != old.length()) {
            return cursor.getString(index);
        }
        if (this.mOldChars == null || this.mOldChars.sizeCopied < length) {
            this.mOldChars = new CharArrayBuffer(length);
        }
        char[] oldArray = this.mOldChars.data;
        char[] newArray = this.mNewChars.data;
        old.getChars(0, length, oldArray, 0);
        for (int i = length - 1; i >= 0; i--) {
            if (oldArray[i] != newArray[i]) {
                return new String(newArray, 0, length);
            }
        }
        return old;
    }

    private int findBatchWithTimeStamp(long timestamp) {
        for (int i = this.mBatchs.size() - 1; i >= 0; i--) {
            if (this.mBatchs.get(i).mTimestamp == timestamp) {
                return i;
            }
        }
        return -1;
    }

    private void removeBatch(BluetoothOppBatch batch) {
        this.mBatchs.remove(batch);
        if (this.mBatchs.size() > 0) {
            for (int i = 0; i < this.mBatchs.size(); i++) {
                BluetoothOppBatch nextBatch = this.mBatchs.get(i);
                if (nextBatch.mStatus != 1) {
                    if (nextBatch.mDirection == 0) {
                        this.mTransfer = new BluetoothOppTransfer(this, this.mPowerManager, nextBatch);
                        this.mTransfer.start();
                        return;
                    } else {
                        if (nextBatch.mDirection == 1 && this.mServerSession != null) {
                            this.mServerTransfer = new BluetoothOppTransfer(this, this.mPowerManager, nextBatch, this.mServerSession);
                            this.mServerTransfer.start();
                            if (nextBatch.getPendingShare().mConfirm == 1) {
                                this.mServerTransfer.confirmStatusChanged();
                                return;
                            }
                            return;
                        }
                    }
                } else {
                    return;
                }
            }
        }
    }

    private boolean needAction(int arrayPos) {
        BluetoothOppShareInfo info = this.mShares.get(arrayPos);
        return !BluetoothShare.isStatusCompleted(info.mStatus);
    }

    private boolean visibleNotification(int arrayPos) {
        BluetoothOppShareInfo info = this.mShares.get(arrayPos);
        return info.hasCompletionNotification();
    }

    private boolean scanFile(Cursor cursor, int arrayPos) {
        boolean z = true;
        BluetoothOppShareInfo info = this.mShares.get(arrayPos);
        synchronized (this) {
            Log.d(TAG, "Scanning file " + info.mFilename);
            if (this.mMediaScanInProgress) {
                z = false;
            } else {
                this.mMediaScanInProgress = true;
                new MediaScannerNotifier(this, info, this.mHandler);
            }
        }
        return z;
    }

    private boolean shouldScanFile(int arrayPos) {
        BluetoothOppShareInfo info = this.mShares.get(arrayPos);
        return BluetoothShare.isStatusSuccess(info.mStatus) && info.mDirection == 1 && !info.mMediaScanned && info.mConfirm != 5;
    }

    private static void trimDatabase(ContentResolver contentResolver) {
        contentResolver.delete(BluetoothShare.CONTENT_URI, "direction=0 AND status>=200 AND visibility=1", null);
        contentResolver.delete(BluetoothShare.CONTENT_URI, "direction=1 AND status>200 AND visibility=1", null);
        Cursor cursor = contentResolver.query(BluetoothShare.CONTENT_URI, new String[]{"_id"}, "direction=1 AND status=200 AND visibility=1", null, "_id");
        if (cursor != null) {
            int recordNum = cursor.getCount();
            if (recordNum > 1000) {
                int numToDelete = recordNum - 1000;
                if (cursor.moveToPosition(numToDelete)) {
                    int columnId = cursor.getColumnIndexOrThrow("_id");
                    long id = cursor.getLong(columnId);
                    contentResolver.delete(BluetoothShare.CONTENT_URI, "_id < " + id, null);
                }
            }
            cursor.close();
        }
    }

    private static class MediaScannerNotifier implements MediaScannerConnection.MediaScannerConnectionClient {
        private Handler mCallback;
        private MediaScannerConnection mConnection;
        private Context mContext;
        private BluetoothOppShareInfo mInfo;

        public MediaScannerNotifier(Context context, BluetoothOppShareInfo info, Handler handler) {
            this.mContext = context;
            this.mInfo = info;
            this.mCallback = handler;
            this.mConnection = new MediaScannerConnection(this.mContext, this);
            this.mConnection.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            this.mConnection.scanFile(this.mInfo.mFilename, this.mInfo.mMimetype);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            try {
                if (uri != null) {
                    Message msg = Message.obtain();
                    msg.setTarget(this.mCallback);
                    msg.what = 2;
                    msg.arg1 = this.mInfo.mId;
                    msg.obj = uri;
                    msg.sendToTarget();
                } else {
                    Message msg2 = Message.obtain();
                    msg2.setTarget(this.mCallback);
                    msg2.what = 3;
                    msg2.arg1 = this.mInfo.mId;
                    msg2.sendToTarget();
                }
            } catch (Exception ex) {
                Log.v(BluetoothOppService.TAG, "!!!MediaScannerConnection exception: " + ex);
            } finally {
                this.mConnection.disconnect();
            }
        }
    }
}
