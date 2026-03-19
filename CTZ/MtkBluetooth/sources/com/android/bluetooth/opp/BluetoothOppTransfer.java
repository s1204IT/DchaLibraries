package com.android.bluetooth.opp;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.SdpOppOpsRecord;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.bluetooth.BluetoothObexTransport;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.bluetooth.opp.BluetoothOppBatch;
import java.io.File;
import java.io.IOException;
import javax.obex.ObexTransport;

public class BluetoothOppTransfer implements BluetoothOppBatch.BluetoothOppBatchListener {
    private static final int CONNECT_RETRY_TIME = 100;
    private static final int CONNECT_WAIT_TIMEOUT = 45000;
    private static final int SOCKET_ERROR_RETRY = 13;
    private static final String SOCKET_LINK_KEY_ERROR = "Invalid exchange";
    private static final String TAG = "BtOppTransfer";
    private static final int TRANSPORT_CONNECTED = 11;
    private static final int TRANSPORT_ERROR = 10;
    private BluetoothAdapter mAdapter;
    private BluetoothOppBatch mBatch;
    private OppConnectionReceiver mBluetoothReceiver;
    private SocketConnectThread mConnectThread;
    private Context mContext;
    private BluetoothOppShareInfo mCurrentShare;
    private BluetoothDevice mDevice;
    private HandlerThread mHandlerThread;
    private BluetoothOppObexSession mSession;
    private EventHandler mSessionHandler;
    private long mTimestamp;
    private ObexTransport mTransport;
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;

    private class OppConnectionReceiver extends BroadcastReceiver {
        private OppConnectionReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothOppTransfer.D) {
                Log.d(BluetoothOppTransfer.TAG, " Action :" + action);
            }
            if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED")) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (bluetoothDevice == null || BluetoothOppTransfer.this.mBatch == null || BluetoothOppTransfer.this.mCurrentShare == null) {
                    Log.e(BluetoothOppTransfer.TAG, "device : " + bluetoothDevice + " mBatch :" + BluetoothOppTransfer.this.mBatch + " mCurrentShare :" + BluetoothOppTransfer.this.mCurrentShare);
                    return;
                }
                try {
                    if (BluetoothOppTransfer.V) {
                        Log.v(BluetoothOppTransfer.TAG, "Device :" + bluetoothDevice + "- OPP device: " + BluetoothOppTransfer.this.mBatch.mDestination + " \n mCurrentShare.mConfirm == " + BluetoothOppTransfer.this.mCurrentShare.mConfirm);
                    }
                    if (bluetoothDevice.equals(BluetoothOppTransfer.this.mBatch.mDestination) && BluetoothOppTransfer.this.mCurrentShare.mConfirm == 0) {
                        if (BluetoothOppTransfer.V) {
                            Log.v(BluetoothOppTransfer.TAG, "ACTION_ACL_DISCONNECTED to be processed for batch: " + BluetoothOppTransfer.this.mBatch.mId);
                        }
                        BluetoothOppTransfer.this.mSessionHandler.removeMessages(4);
                        BluetoothOppTransfer.this.mSessionHandler.sendMessage(BluetoothOppTransfer.this.mSessionHandler.obtainMessage(4));
                        return;
                    }
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
            if (action.equals("android.bluetooth.device.action.SDP_RECORD")) {
                ParcelUuid parcelUuid = (ParcelUuid) intent.getParcelableExtra("android.bluetooth.device.extra.UUID");
                if (BluetoothOppTransfer.D) {
                    Log.d(BluetoothOppTransfer.TAG, "Received UUID: " + parcelUuid.toString());
                    Log.d(BluetoothOppTransfer.TAG, "expected UUID: " + BluetoothUuid.ObexObjectPush.toString());
                }
                if (parcelUuid.equals(BluetoothUuid.ObexObjectPush)) {
                    Log.d(BluetoothOppTransfer.TAG, " -> status: " + intent.getIntExtra("android.bluetooth.device.extra.SDP_SEARCH_STATUS", -1));
                    BluetoothDevice bluetoothDevice2 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    if (BluetoothOppTransfer.this.mDevice != null) {
                        if (!bluetoothDevice2.getAddress().equalsIgnoreCase(BluetoothOppTransfer.this.mDevice.getAddress())) {
                            Log.w(BluetoothOppTransfer.TAG, " OPP SDP search for wrong device, ignoring!!");
                            return;
                        }
                        SdpOppOpsRecord parcelableExtra = intent.getParcelableExtra("android.bluetooth.device.extra.SDP_RECORD");
                        if (parcelableExtra == null) {
                            Log.w(BluetoothOppTransfer.TAG, " Invalid SDP , ignoring !!");
                            BluetoothOppTransfer.this.mConnectThread = BluetoothOppTransfer.this.new SocketConnectThread(BluetoothOppTransfer.this.mBatch.mDestination, false, false, -1);
                            BluetoothOppTransfer.this.mConnectThread.start();
                            BluetoothOppTransfer.this.mDevice = null;
                            return;
                        }
                        BluetoothOppTransfer.this.mConnectThread = BluetoothOppTransfer.this.new SocketConnectThread(BluetoothOppTransfer.this.mDevice, false, true, parcelableExtra.getL2capPsm());
                        BluetoothOppTransfer.this.mConnectThread.start();
                        BluetoothOppTransfer.this.mDevice = null;
                        return;
                    }
                    Log.w(BluetoothOppTransfer.TAG, "OPP SDP search, target device is null, ignoring result");
                }
            }
        }
    }

    public BluetoothOppTransfer(Context context, BluetoothOppBatch bluetoothOppBatch, BluetoothOppObexSession bluetoothOppObexSession) {
        this.mContext = context;
        this.mBatch = bluetoothOppBatch;
        this.mSession = bluetoothOppObexSession;
        this.mBatch.registerListern(this);
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public BluetoothOppTransfer(Context context, BluetoothOppBatch bluetoothOppBatch) {
        this(context, bluetoothOppBatch, null);
    }

    public int getBatchId() {
        return this.mBatch.mId;
    }

    private class EventHandler extends Handler {
        EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 13) {
                BluetoothOppTransfer.this.mConnectThread = BluetoothOppTransfer.this.new SocketConnectThread((BluetoothDevice) message.obj, true);
                BluetoothOppTransfer.this.mConnectThread.start();
            }
            switch (i) {
                case 0:
                    BluetoothOppShareInfo bluetoothOppShareInfo = (BluetoothOppShareInfo) message.obj;
                    if (BluetoothOppTransfer.V) {
                        Log.v(BluetoothOppTransfer.TAG, "receive MSG_SHARE_COMPLETE for info " + bluetoothOppShareInfo.mId);
                    }
                    if (BluetoothOppTransfer.this.mBatch.mDirection == 0) {
                        BluetoothOppTransfer.this.mCurrentShare = BluetoothOppTransfer.this.mBatch.getPendingShare();
                        if (BluetoothOppTransfer.this.mCurrentShare != null) {
                            if (BluetoothOppTransfer.V) {
                                Log.v(BluetoothOppTransfer.TAG, "continue session for info " + BluetoothOppTransfer.this.mCurrentShare.mId + " from batch " + BluetoothOppTransfer.this.mBatch.mId);
                            }
                            BluetoothOppTransfer.this.processCurrentShare();
                        } else {
                            if (BluetoothOppTransfer.V) {
                                Log.v(BluetoothOppTransfer.TAG, "Batch " + BluetoothOppTransfer.this.mBatch.mId + " is done");
                            }
                            BluetoothOppTransfer.this.mSession.stop();
                        }
                    }
                    break;
                case 1:
                    BluetoothOppTransfer.this.cleanUp();
                    BluetoothOppShareInfo bluetoothOppShareInfo2 = (BluetoothOppShareInfo) message.obj;
                    if (BluetoothOppTransfer.V) {
                        Log.v(BluetoothOppTransfer.TAG, "receive MSG_SESSION_COMPLETE for batch " + BluetoothOppTransfer.this.mBatch.mId);
                    }
                    BluetoothOppTransfer.this.mBatch.mStatus = 2;
                    BluetoothOppTransfer.this.tickShareStatus(bluetoothOppShareInfo2);
                    break;
                case 2:
                    if (BluetoothOppTransfer.V) {
                        Log.v(BluetoothOppTransfer.TAG, "receive MSG_SESSION_ERROR for batch " + BluetoothOppTransfer.this.mBatch.mId);
                    }
                    BluetoothOppTransfer.this.cleanUp();
                    try {
                        BluetoothOppShareInfo bluetoothOppShareInfo3 = (BluetoothOppShareInfo) message.obj;
                        if (BluetoothOppTransfer.this.mSession != null) {
                            BluetoothOppTransfer.this.mSession.stop();
                        }
                        BluetoothOppTransfer.this.mBatch.mStatus = 3;
                        BluetoothOppTransfer.this.markBatchFailed(bluetoothOppShareInfo3.mStatus);
                        BluetoothOppTransfer.this.tickShareStatus(BluetoothOppTransfer.this.mCurrentShare);
                    } catch (Exception e) {
                        Log.e(BluetoothOppTransfer.TAG, "Exception while handling MSG_SESSION_ERROR");
                        e.printStackTrace();
                        return;
                    }
                    break;
                case 3:
                    if (BluetoothOppTransfer.V) {
                        Log.v(BluetoothOppTransfer.TAG, "receive MSG_SHARE_INTERRUPTED for batch " + BluetoothOppTransfer.this.mBatch.mId);
                    }
                    BluetoothOppShareInfo bluetoothOppShareInfo4 = (BluetoothOppShareInfo) message.obj;
                    if (BluetoothOppTransfer.this.mBatch.mDirection == 0) {
                        try {
                            if (BluetoothOppTransfer.this.mTransport != null) {
                                BluetoothOppTransfer.this.mTransport.close();
                            } else {
                                Log.v(BluetoothOppTransfer.TAG, "receive MSG_SHARE_INTERRUPTED but mTransport = null");
                            }
                        } catch (IOException e2) {
                            Log.e(BluetoothOppTransfer.TAG, "failed to close mTransport");
                        }
                        if (BluetoothOppTransfer.V) {
                            Log.v(BluetoothOppTransfer.TAG, "mTransport closed ");
                        }
                        BluetoothOppTransfer.this.mBatch.mStatus = 3;
                        if (bluetoothOppShareInfo4 != null) {
                            BluetoothOppTransfer.this.markBatchFailed(bluetoothOppShareInfo4.mStatus);
                        } else {
                            BluetoothOppTransfer.this.markBatchFailed(BluetoothShare.STATUS_UNKNOWN_ERROR);
                        }
                        BluetoothOppTransfer.this.tickShareStatus(BluetoothOppTransfer.this.mCurrentShare);
                    }
                    break;
                case 4:
                    if (BluetoothOppTransfer.V) {
                        Log.v(BluetoothOppTransfer.TAG, "receive MSG_CONNECT_TIMEOUT for batch " + BluetoothOppTransfer.this.mBatch.mId);
                    }
                    if (BluetoothOppTransfer.this.mBatch.mDirection == 0) {
                        try {
                            if (BluetoothOppTransfer.this.mTransport != null) {
                                BluetoothOppTransfer.this.mTransport.close();
                            } else {
                                Log.v(BluetoothOppTransfer.TAG, "receive MSG_SHARE_INTERRUPTED but mTransport = null");
                            }
                        } catch (IOException e3) {
                            Log.e(BluetoothOppTransfer.TAG, "failed to close mTransport");
                        }
                        if (BluetoothOppTransfer.V) {
                            Log.v(BluetoothOppTransfer.TAG, "mTransport closed ");
                        }
                    } else {
                        ((NotificationManager) BluetoothOppTransfer.this.mContext.getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION)).cancel(BluetoothOppTransfer.this.mCurrentShare.mId);
                        BluetoothOppTransfer.this.mContext.sendBroadcast(new Intent(BluetoothShare.USER_CONFIRMATION_TIMEOUT_ACTION));
                        BluetoothOppTransfer.this.markShareTimeout(BluetoothOppTransfer.this.mCurrentShare);
                    }
                    break;
                case 5:
                    if (BluetoothOppTransfer.V) {
                        Log.v(BluetoothOppTransfer.TAG, "receive MSG_FAST_ERROR");
                    }
                    BluetoothOppTransfer.this.mBatch.mStatus = 3;
                    BluetoothOppTransfer.this.interrupt(((Integer) message.obj).intValue());
                    BluetoothOppTransfer.this.tickShareStatus(BluetoothOppTransfer.this.mCurrentShare);
                    break;
                default:
                    switch (i) {
                        case 10:
                            if (BluetoothOppTransfer.V) {
                                Log.v(BluetoothOppTransfer.TAG, "receive TRANSPORT_ERROR msg");
                            }
                            BluetoothOppTransfer.this.mConnectThread = null;
                            BluetoothOppTransfer.this.markBatchFailed(BluetoothShare.STATUS_CONNECTION_ERROR);
                            BluetoothOppTransfer.this.mBatch.mStatus = 3;
                            break;
                        case 11:
                            if (BluetoothOppTransfer.V) {
                                Log.v(BluetoothOppTransfer.TAG, "Transfer receive TRANSPORT_CONNECTED msg");
                            }
                            BluetoothOppTransfer.this.mConnectThread = null;
                            BluetoothOppTransfer.this.mTransport = (ObexTransport) message.obj;
                            BluetoothOppTransfer.this.startObexSession();
                            break;
                    }
                    break;
            }
        }
    }

    private void markShareTimeout(BluetoothOppShareInfo bluetoothOppShareInfo) {
        Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + bluetoothOppShareInfo.mId);
        ContentValues contentValues = new ContentValues();
        contentValues.put("confirm", (Integer) 4);
        this.mContext.getContentResolver().update(uri, contentValues, null, null);
    }

    private void markBatchFailed(int i) {
        synchronized (this) {
            try {
                wait(1000L);
            } catch (InterruptedException e) {
                if (V) {
                    Log.v(TAG, "Interrupted waiting for markBatchFailed");
                }
            }
        }
        if (D) {
            Log.d(TAG, "Mark all ShareInfo in the batch as failed");
        }
        if (this.mCurrentShare != null) {
            if (V) {
                Log.v(TAG, "Current share has status " + this.mCurrentShare.mStatus);
            }
            if (BluetoothShare.isStatusError(this.mCurrentShare.mStatus)) {
                i = this.mCurrentShare.mStatus;
            }
            if (this.mCurrentShare.mDirection == 1 && this.mCurrentShare.mFilename != null) {
                new File(this.mCurrentShare.mFilename).delete();
            }
        }
        if (this.mBatch == null) {
            return;
        }
        BluetoothOppShareInfo pendingShare = this.mBatch.getPendingShare();
        while (pendingShare != null) {
            if (pendingShare.mStatus < 200) {
                pendingShare.mStatus = i;
                Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + pendingShare.mId);
                ContentValues contentValues = new ContentValues();
                contentValues.put("status", Integer.valueOf(pendingShare.mStatus));
                if (pendingShare.mDirection == 0) {
                    BluetoothOppSendFileInfo sendFileInfo = BluetoothOppUtility.getSendFileInfo(pendingShare.mUri);
                    BluetoothOppUtility.closeSendFileInfo(pendingShare.mUri);
                    if (sendFileInfo.mFileName != null) {
                        contentValues.put(BluetoothShare.FILENAME_HINT, sendFileInfo.mFileName);
                        contentValues.put(BluetoothShare.TOTAL_BYTES, Long.valueOf(sendFileInfo.mLength));
                        contentValues.put(BluetoothShare.MIMETYPE, sendFileInfo.mMimetype);
                    }
                } else if (pendingShare.mStatus < 200 && pendingShare.mFilename != null) {
                    new File(pendingShare.mFilename).delete();
                }
                this.mContext.getContentResolver().update(uri, contentValues, null, null);
                Constants.sendIntentIfCompleted(this.mContext, uri, pendingShare.mStatus);
            }
            pendingShare = this.mBatch.getPendingShare();
        }
    }

    public void start() {
        if (!this.mAdapter.isEnabled()) {
            Log.e(TAG, "Can't start transfer when Bluetooth is disabled for " + this.mBatch.mId);
            markBatchFailed(BluetoothShare.STATUS_UNKNOWN_ERROR);
            this.mBatch.mStatus = 3;
            return;
        }
        if (this.mHandlerThread == null) {
            if (V) {
                Log.v(TAG, "Create handler thread for batch " + this.mBatch.mId);
            }
            this.mHandlerThread = new HandlerThread("BtOpp Transfer Handler", 10);
            this.mHandlerThread.start();
            this.mSessionHandler = new EventHandler(this.mHandlerThread.getLooper());
            registerConnectionreceiver();
            if (this.mBatch.mDirection == 0) {
                startConnectSession();
            } else if (this.mBatch.mDirection == 1) {
                startObexSession();
            }
        }
    }

    public void stop() {
        if (V) {
            Log.v(TAG, "stop");
        }
        if (this.mSession != null) {
            if (V) {
                Log.v(TAG, "Stop mSession");
            }
            this.mSession.stop();
        }
        cleanUp();
        if (this.mConnectThread != null) {
            try {
                this.mConnectThread.interrupt();
                if (V) {
                    Log.v(TAG, "waiting for connect thread to terminate");
                }
                this.mConnectThread.join();
            } catch (InterruptedException e) {
                if (V) {
                    Log.v(TAG, "Interrupted waiting for connect thread to join");
                }
            }
            this.mConnectThread = null;
        }
        synchronized (this) {
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quit();
                this.mHandlerThread.interrupt();
                this.mHandlerThread = null;
            }
        }
    }

    public void interrupt(int i) {
        if (V) {
            Log.v(TAG, "interrupt");
        }
        if (this.mCurrentShare != null) {
            if (V) {
                Log.v(TAG, "Current share has status " + this.mCurrentShare.mStatus);
            }
            if (BluetoothShare.isStatusError(this.mCurrentShare.mStatus)) {
                i = this.mCurrentShare.mStatus;
            }
            if (this.mCurrentShare.mDirection == 1 && this.mCurrentShare.mFilename != null) {
                new File(this.mCurrentShare.mFilename).delete();
            }
        }
        if (this.mBatch == null) {
            return;
        }
        BluetoothOppShareInfo pendingShare = this.mBatch.getPendingShare();
        while (pendingShare != null) {
            if (pendingShare.mStatus < 200) {
                Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + pendingShare.mId);
                ContentValues contentValues = new ContentValues();
                if (pendingShare.mDirection == 0) {
                    BluetoothOppSendFileInfo sendFileInfo = BluetoothOppUtility.getSendFileInfo(pendingShare.mUri);
                    BluetoothOppUtility.closeSendFileInfo(pendingShare.mUri);
                    if (sendFileInfo.mFileName != null) {
                        contentValues.put(BluetoothShare.FILENAME_HINT, sendFileInfo.mFileName);
                        contentValues.put(BluetoothShare.TOTAL_BYTES, Long.valueOf(sendFileInfo.mLength));
                        contentValues.put(BluetoothShare.MIMETYPE, sendFileInfo.mMimetype);
                    }
                } else if (pendingShare.mStatus < 200 && pendingShare.mFilename != null) {
                    new File(pendingShare.mFilename).delete();
                }
                pendingShare.mStatus = i;
                contentValues.put("status", Integer.valueOf(pendingShare.mStatus));
                this.mContext.getContentResolver().update(uri, contentValues, null, null);
                Constants.sendIntentIfCompleted(this.mContext, uri, pendingShare.mStatus);
                this.mBatch.RemoveShareInfo(pendingShare);
            }
            pendingShare = this.mBatch.getPendingShare();
        }
        cleanUp();
        if (this.mConnectThread != null) {
            try {
                this.mConnectThread.interrupt();
                if (V) {
                    Log.v(TAG, "waiting for connect thread to terminate");
                }
                this.mConnectThread.join();
                if (V) {
                    Log.v(TAG, "thread terminated!");
                }
            } catch (InterruptedException e) {
                if (V) {
                    Log.v(TAG, "Interrupted waiting for connect thread to join");
                }
            }
            this.mConnectThread = null;
        } else if (V) {
            Log.v(TAG, "mConnectThread is null");
        }
        if (this.mSession != null) {
            if (V) {
                Log.v(TAG, "mSession forceInterupt");
            }
            this.mSession.forceInterupt();
        }
        synchronized (this) {
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quit();
                this.mHandlerThread.interrupt();
                this.mHandlerThread = null;
            }
        }
    }

    private void startObexSession() {
        this.mBatch.mStatus = 1;
        this.mCurrentShare = this.mBatch.getPendingShare();
        if (this.mCurrentShare == null) {
            Log.e(TAG, "Unexpected error happened !");
            return;
        }
        if (V) {
            Log.v(TAG, "Start session for info " + this.mCurrentShare.mId + " for batch " + this.mBatch.mId);
        }
        if (this.mBatch.mDirection == 0) {
            if (V) {
                Log.v(TAG, "Create Client session with transport " + this.mTransport.toString());
            }
            this.mSession = new BluetoothOppObexClientSession(this.mContext, this.mTransport);
        } else if (this.mBatch.mDirection == 1) {
            if (this.mSession == null) {
                Log.e(TAG, "Unexpected error happened !");
                markBatchFailed(BluetoothShare.STATUS_UNKNOWN_ERROR);
                this.mBatch.mStatus = 3;
                return;
            } else if (V) {
                Log.v(TAG, "Transfer has Server session" + this.mSession.toString());
            }
        }
        this.mSession.start(this.mSessionHandler, this.mBatch.getNumShares());
        processCurrentShare();
    }

    private void registerConnectionreceiver() {
        synchronized (this) {
            try {
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "mBluetoothReceiver Registered already ", e);
            }
            if (this.mBluetoothReceiver == null) {
                this.mBluetoothReceiver = new OppConnectionReceiver();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
                intentFilter.addAction("android.bluetooth.device.action.SDP_RECORD");
                this.mContext.registerReceiver(this.mBluetoothReceiver, intentFilter);
                if (V) {
                    Log.v(TAG, "Registered mBluetoothReceiver");
                }
            }
        }
    }

    private void processCurrentShare() {
        if (V) {
            Log.v(TAG, "processCurrentShare" + this.mCurrentShare.mId);
        }
        this.mSession.addShare(this.mCurrentShare);
        if (this.mCurrentShare.mConfirm == 5) {
            confirmStatusChanged();
        }
    }

    public void confirmStatusChanged() {
        Thread thread = new Thread("Server Unblock thread") {
            @Override
            public void run() {
                synchronized (BluetoothOppTransfer.this.mSession) {
                    BluetoothOppTransfer.this.mSession.unblock();
                    BluetoothOppTransfer.this.mSession.notify();
                }
            }
        };
        if (V) {
            Log.v(TAG, "confirmStatusChanged to unblock mSession" + this.mSession.toString());
        }
        thread.start();
    }

    private void startConnectSession() {
        this.mDevice = this.mBatch.mDestination;
        if (!this.mBatch.mDestination.sdpSearch(BluetoothUuid.ObexObjectPush)) {
            if (D) {
                Log.d(TAG, "SDP failed, start rfcomm connect directly");
            }
            this.mDevice = null;
            this.mConnectThread = new SocketConnectThread(this.mBatch.mDestination, false, false, -1);
            this.mConnectThread.start();
        }
    }

    private class SocketConnectThread extends Thread {
        private BluetoothSocket mBtSocket;
        private final int mChannel;
        private final BluetoothDevice mDevice;
        private final String mHost;
        private boolean mIsConnected;
        private boolean mIsInterrupted;
        private int mL2cChannel;
        private boolean mRetry;
        private boolean mSdpInitiated;
        private long mTimestamp;

        SocketConnectThread(BluetoothDevice bluetoothDevice, boolean z) {
            super("Socket Connect Thread");
            this.mL2cChannel = 0;
            this.mBtSocket = null;
            this.mRetry = false;
            this.mSdpInitiated = false;
            this.mIsInterrupted = false;
            this.mDevice = bluetoothDevice;
            this.mHost = null;
            this.mChannel = -1;
            this.mIsConnected = false;
            this.mRetry = z;
            this.mSdpInitiated = false;
        }

        SocketConnectThread(BluetoothDevice bluetoothDevice, boolean z, boolean z2, int i) {
            super("Socket Connect Thread");
            this.mL2cChannel = 0;
            this.mBtSocket = null;
            this.mRetry = false;
            this.mSdpInitiated = false;
            this.mIsInterrupted = false;
            this.mDevice = bluetoothDevice;
            this.mHost = null;
            this.mChannel = -1;
            this.mIsConnected = false;
            this.mRetry = z;
            this.mSdpInitiated = z2;
            this.mL2cChannel = i;
        }

        @Override
        public void interrupt() {
            if (BluetoothOppTransfer.D) {
                Log.d(BluetoothOppTransfer.TAG, "start interrupt :" + this.mBtSocket);
            }
            this.mIsInterrupted = true;
            if (this.mBtSocket != null) {
                try {
                    this.mBtSocket.close();
                } catch (IOException e) {
                    Log.v(BluetoothOppTransfer.TAG, "Error when close socket");
                }
            }
        }

        private void connectRfcommSocket() {
            if (BluetoothOppTransfer.V) {
                Log.v(BluetoothOppTransfer.TAG, "connectRfcommSocket");
            }
            try {
                if (this.mIsInterrupted) {
                    Log.d(BluetoothOppTransfer.TAG, "connectRfcommSocket interrupted");
                    BluetoothOppTransfer.this.markConnectionFailed(this.mBtSocket);
                    return;
                }
                this.mBtSocket = this.mDevice.createInsecureRfcommSocketToServiceRecord(BluetoothUuid.ObexObjectPush.getUuid());
                try {
                    this.mBtSocket.connect();
                    if (BluetoothOppTransfer.V) {
                        Log.v(BluetoothOppTransfer.TAG, "Rfcomm socket connection attempt took " + (System.currentTimeMillis() - this.mTimestamp) + " ms");
                    }
                    BluetoothObexTransport bluetoothObexTransport = new BluetoothObexTransport(this.mBtSocket);
                    BluetoothOppPreference.getInstance(BluetoothOppTransfer.this.mContext).setName(this.mDevice, this.mDevice.getName());
                    if (BluetoothOppTransfer.V) {
                        Log.v(BluetoothOppTransfer.TAG, "Send transport message " + bluetoothObexTransport.toString());
                    }
                    BluetoothOppTransfer.this.mSessionHandler.obtainMessage(11, bluetoothObexTransport).sendToTarget();
                } catch (IOException e) {
                    Log.e(BluetoothOppTransfer.TAG, "Rfcomm socket connect exception", e);
                    if (this.mRetry || !e.getMessage().equals(BluetoothOppTransfer.SOCKET_LINK_KEY_ERROR)) {
                        BluetoothOppTransfer.this.markConnectionFailed(this.mBtSocket);
                    } else {
                        BluetoothOppTransfer.this.mSessionHandler.sendMessageDelayed(BluetoothOppTransfer.this.mSessionHandler.obtainMessage(13, -1, -1, this.mDevice), 1500L);
                    }
                }
            } catch (IOException e2) {
                Log.e(BluetoothOppTransfer.TAG, "Rfcomm socket create error", e2);
                BluetoothOppTransfer.this.markConnectionFailed(this.mBtSocket);
            }
        }

        @Override
        public void run() {
            this.mTimestamp = System.currentTimeMillis();
            if (BluetoothOppTransfer.D) {
                Log.d(BluetoothOppTransfer.TAG, "sdp initiated = " + this.mSdpInitiated + " l2cChannel :" + this.mL2cChannel);
            }
            if (!this.mSdpInitiated || this.mL2cChannel < 0) {
                Log.d(BluetoothOppTransfer.TAG, "sdp not initiated, connecting on rfcomm");
                connectRfcommSocket();
                return;
            }
            this.mSdpInitiated = false;
            try {
                if (this.mIsInterrupted) {
                    Log.e(BluetoothOppTransfer.TAG, "btSocket connect interrupted ");
                    BluetoothOppTransfer.this.markConnectionFailed(this.mBtSocket);
                    return;
                }
                this.mBtSocket = this.mDevice.createInsecureL2capSocket(this.mL2cChannel);
                try {
                    this.mBtSocket.connect();
                    if (BluetoothOppTransfer.V) {
                        Log.v(BluetoothOppTransfer.TAG, "L2cap socket connection attempt took " + (System.currentTimeMillis() - this.mTimestamp) + " ms");
                    }
                    BluetoothObexTransport bluetoothObexTransport = new BluetoothObexTransport(this.mBtSocket);
                    BluetoothOppPreference.getInstance(BluetoothOppTransfer.this.mContext).setName(this.mDevice, this.mDevice.getName());
                    if (BluetoothOppTransfer.V) {
                        Log.v(BluetoothOppTransfer.TAG, "Send transport message " + bluetoothObexTransport.toString());
                    }
                    BluetoothOppTransfer.this.mSessionHandler.obtainMessage(11, bluetoothObexTransport).sendToTarget();
                } catch (IOException e) {
                    Log.e(BluetoothOppTransfer.TAG, "L2cap socket connect exception", e);
                    try {
                        this.mBtSocket.close();
                    } catch (IOException e2) {
                        Log.e(BluetoothOppTransfer.TAG, "Bluetooth socket close error ", e2);
                    }
                    connectRfcommSocket();
                }
            } catch (IOException e3) {
                Log.e(BluetoothOppTransfer.TAG, "L2cap socket create error", e3);
                connectRfcommSocket();
            }
        }
    }

    private void markConnectionFailed(BluetoothSocket bluetoothSocket) {
        if (V) {
            Log.v(TAG, "markConnectionFailed " + bluetoothSocket);
        }
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                if (V) {
                    Log.e(TAG, "Error when close socket");
                }
            }
        }
        this.mSessionHandler.obtainMessage(10).sendToTarget();
    }

    private void tickShareStatus(BluetoothOppShareInfo bluetoothOppShareInfo) {
        if (bluetoothOppShareInfo == null) {
            Log.d(TAG, "Share is null");
            return;
        }
        Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + bluetoothOppShareInfo.mId);
        ContentValues contentValues = new ContentValues();
        contentValues.put(BluetoothShare.DIRECTION, Integer.valueOf(bluetoothOppShareInfo.mDirection));
        this.mContext.getContentResolver().update(uri, contentValues, null, null);
    }

    @Override
    public void onShareAdded(int i) {
        if (this.mBatch.getPendingShare().mDirection == 1) {
            this.mCurrentShare = this.mBatch.getPendingShare();
            if (this.mCurrentShare != null) {
                if (this.mCurrentShare.mConfirm == 2 || this.mCurrentShare.mConfirm == 5) {
                    if (V) {
                        Log.v(TAG, "Transfer continue session for info " + this.mCurrentShare.mId + " from batch " + this.mBatch.mId);
                    }
                    processCurrentShare();
                    confirmStatusChanged();
                }
            }
        }
    }

    @Override
    public void onShareDeleted(int i) {
    }

    @Override
    public void onBatchCanceled() {
        if (V) {
            Log.v(TAG, "Transfer on Batch canceled");
        }
        stop();
        this.mBatch.mStatus = 2;
    }

    private void cleanUp() {
        synchronized (this) {
            try {
            } catch (Exception e) {
                Log.e(TAG, "Exception:unregisterReceiver");
                e.printStackTrace();
            }
            if (this.mBluetoothReceiver != null) {
                this.mContext.unregisterReceiver(this.mBluetoothReceiver);
                this.mBluetoothReceiver = null;
            }
        }
    }
}
