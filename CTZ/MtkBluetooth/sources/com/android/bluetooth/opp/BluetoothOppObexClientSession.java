package com.android.bluetooth.opp;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.R;
import com.android.bluetooth.btservice.MetricsLogger;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.obex.ClientOperation;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.ObexTransport;

public class BluetoothOppObexClientSession implements BluetoothOppObexSession {
    private static final String TAG = "BtOppObexClient";
    private Handler mCallback;
    private Context mContext;
    private volatile boolean mInterrupted;
    private int mNumFilesAttemptedToSend;
    private ClientThread mThread;
    private ObexTransport mTransport;
    private volatile boolean mWaitingForRemote;
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;

    static int access$408(BluetoothOppObexClientSession bluetoothOppObexClientSession) {
        int i = bluetoothOppObexClientSession.mNumFilesAttemptedToSend;
        bluetoothOppObexClientSession.mNumFilesAttemptedToSend = i + 1;
        return i;
    }

    public BluetoothOppObexClientSession(Context context, ObexTransport obexTransport) {
        if (obexTransport == null) {
            throw new NullPointerException("transport is null");
        }
        this.mContext = context;
        this.mTransport = obexTransport;
    }

    @Override
    public void start(Handler handler, int i) {
        if (D) {
            Log.d(TAG, "Start!");
        }
        this.mCallback = handler;
        this.mThread = new ClientThread(this.mContext, this.mTransport, i);
        this.mThread.start();
    }

    @Override
    public void stop() {
        if (D) {
            Log.d(TAG, "Stop!");
        }
        synchronized (this) {
            if (this.mThread != null) {
                this.mInterrupted = true;
                try {
                    this.mThread.interrupt();
                    if (V) {
                        Log.v(TAG, "waiting for thread to terminate");
                    }
                    this.mThread.join();
                    this.mThread = null;
                } catch (Exception e) {
                    if (V) {
                        Log.v(TAG, "Interrupted waiting for thread to join");
                    }
                }
            }
        }
        BluetoothOppUtility.cancelNotification(this.mContext);
        this.mCallback = null;
    }

    @Override
    public void forceInterupt() {
        if (D) {
            Log.d(TAG, "forceInterupt!");
        }
        synchronized (this) {
            if (this.mThread != null) {
                this.mInterrupted = true;
                this.mThread.interrupt();
                if (V) {
                    Log.v(TAG, "interrupt");
                }
                this.mThread.disconnect();
                if (V) {
                    Log.v(TAG, "disconnect");
                }
                this.mThread = null;
            }
        }
        this.mCallback = null;
    }

    @Override
    public void addShare(BluetoothOppShareInfo bluetoothOppShareInfo) {
        this.mThread.addShare(bluetoothOppShareInfo);
    }

    private static int readFully(InputStream inputStream, byte[] bArr, int i) throws IOException {
        int i2 = 0;
        while (i2 < i) {
            int i3 = inputStream.read(bArr, i2, i - i2);
            if (i3 <= 0) {
                break;
            }
            i2 += i3;
        }
        return i2;
    }

    private class ClientThread extends Thread {
        private static final int SLEEP_TIME = 500;
        private boolean mConnected;
        private Context mContext1;
        private ClientSession mCs;
        private BluetoothOppSendFileInfo mFileInfo;
        private BluetoothOppShareInfo mInfo;
        private int mNumShares;
        private ObexTransport mTransport1;
        private volatile boolean mWaitingForShare;
        private PowerManager.WakeLock mWakeLock;

        ClientThread(Context context, ObexTransport obexTransport, int i) {
            super("BtOpp ClientThread");
            this.mFileInfo = null;
            this.mConnected = false;
            this.mContext1 = context;
            this.mTransport1 = obexTransport;
            this.mWaitingForShare = true;
            BluetoothOppObexClientSession.this.mWaitingForRemote = false;
            this.mNumShares = i;
            this.mWakeLock = ((PowerManager) this.mContext1.getSystemService("power")).newWakeLock(1, BluetoothOppObexClientSession.TAG);
        }

        public void addShare(BluetoothOppShareInfo bluetoothOppShareInfo) {
            this.mInfo = bluetoothOppShareInfo;
            this.mFileInfo = processShareInfo();
            this.mWaitingForShare = false;
        }

        @Override
        public void run() throws Throwable {
            Process.setThreadPriority(1);
            if (BluetoothOppObexClientSession.V) {
                Log.v(BluetoothOppObexClientSession.TAG, "acquire partial WakeLock");
            }
            this.mWakeLock.acquire();
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                if (BluetoothOppObexClientSession.V) {
                    Log.v(BluetoothOppObexClientSession.TAG, "Client thread was interrupted (1), exiting");
                }
                BluetoothOppObexClientSession.this.mInterrupted = true;
            }
            if (!BluetoothOppObexClientSession.this.mInterrupted) {
                connect(this.mNumShares);
            }
            BluetoothOppObexClientSession.this.mNumFilesAttemptedToSend = 0;
            while (!BluetoothOppObexClientSession.this.mInterrupted) {
                if (this.mWaitingForShare) {
                    try {
                        if (BluetoothOppObexClientSession.D) {
                            Log.d(BluetoothOppObexClientSession.TAG, "Client thread waiting for next share, sleep for 500");
                        }
                        Thread.sleep(500L);
                    } catch (InterruptedException e2) {
                    }
                } else {
                    doSend();
                }
            }
            disconnect();
            if (this.mWakeLock.isHeld()) {
                if (BluetoothOppObexClientSession.V) {
                    Log.v(BluetoothOppObexClientSession.TAG, "release partial WakeLock");
                }
                this.mWakeLock.release();
            }
            if (BluetoothOppObexClientSession.this.mNumFilesAttemptedToSend > 0) {
                MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.OPP);
            }
            if (BluetoothOppObexClientSession.this.mCallback != null) {
                Message messageObtain = Message.obtain(BluetoothOppObexClientSession.this.mCallback);
                messageObtain.what = 1;
                messageObtain.obj = this.mInfo;
                messageObtain.sendToTarget();
            }
        }

        private void disconnect() {
            try {
                if (this.mCs != null) {
                    this.mCs.disconnect((HeaderSet) null);
                }
                this.mCs = null;
                if (BluetoothOppObexClientSession.D) {
                    Log.d(BluetoothOppObexClientSession.TAG, "OBEX session disconnected");
                }
            } catch (IOException e) {
                Log.w(BluetoothOppObexClientSession.TAG, "OBEX session disconnect error" + e);
            }
            try {
                if (this.mCs != null) {
                    if (BluetoothOppObexClientSession.D) {
                        Log.d(BluetoothOppObexClientSession.TAG, "OBEX session close mCs");
                    }
                    this.mCs.close();
                    if (BluetoothOppObexClientSession.D) {
                        Log.d(BluetoothOppObexClientSession.TAG, "OBEX session closed");
                    }
                }
            } catch (IOException e2) {
                Log.w(BluetoothOppObexClientSession.TAG, "OBEX session close error" + e2);
            }
            if (this.mTransport1 != null) {
                try {
                    this.mTransport1.close();
                } catch (IOException e3) {
                    Log.e(BluetoothOppObexClientSession.TAG, "mTransport.close error");
                }
            }
        }

        private void connect(int i) {
            if (BluetoothOppObexClientSession.D) {
                Log.d(BluetoothOppObexClientSession.TAG, "Create ClientSession with transport " + this.mTransport1.toString());
            }
            try {
                this.mCs = new ClientSession(this.mTransport1);
                this.mConnected = true;
            } catch (IOException e) {
                Log.e(BluetoothOppObexClientSession.TAG, "OBEX session create error");
            }
            if (this.mConnected) {
                this.mConnected = false;
                HeaderSet headerSet = new HeaderSet();
                headerSet.setHeader(BluetoothShare.STATUS_RUNNING, Long.valueOf(i));
                synchronized (this) {
                    BluetoothOppObexClientSession.this.mWaitingForRemote = true;
                }
                try {
                    this.mCs.connect(headerSet);
                    if (BluetoothOppObexClientSession.D) {
                        Log.d(BluetoothOppObexClientSession.TAG, "OBEX session created");
                    }
                    this.mConnected = true;
                } catch (IOException e2) {
                    Log.e(BluetoothOppObexClientSession.TAG, "OBEX session connect error");
                }
            }
            synchronized (this) {
                BluetoothOppObexClientSession.this.mWaitingForRemote = false;
            }
        }

        private void doSend() throws Throwable {
            int i;
            int iSendFile = 200;
            while (this.mFileInfo == null) {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    iSendFile = BluetoothShare.STATUS_CANCELED;
                }
            }
            if (!this.mConnected) {
                iSendFile = BluetoothShare.STATUS_CONNECTION_ERROR;
            }
            if (iSendFile == 200) {
                if (this.mFileInfo.mFileName != null) {
                    iSendFile = sendFile(this.mFileInfo);
                } else {
                    iSendFile = this.mFileInfo.mStatus;
                }
                this.mWaitingForShare = true;
            } else {
                Constants.updateShareStatus(this.mContext1, this.mInfo.mId, iSendFile);
            }
            if (BluetoothOppObexClientSession.this.mCallback != null) {
                Message messageObtain = Message.obtain(BluetoothOppObexClientSession.this.mCallback);
                if (iSendFile == 200) {
                    i = 0;
                } else {
                    i = 2;
                }
                messageObtain.what = i;
                this.mInfo.mStatus = iSendFile;
                messageObtain.obj = this.mInfo;
                messageObtain.sendToTarget();
            }
        }

        private BluetoothOppSendFileInfo processShareInfo() {
            if (BluetoothOppObexClientSession.V) {
                Log.v(BluetoothOppObexClientSession.TAG, "Client thread processShareInfo() " + this.mInfo.mId);
            }
            BluetoothOppSendFileInfo sendFileInfo = BluetoothOppUtility.getSendFileInfo(this.mInfo.mUri);
            if (sendFileInfo.mFileName == null || sendFileInfo.mLength == 0) {
                if (BluetoothOppObexClientSession.V) {
                    Log.v(BluetoothOppObexClientSession.TAG, "BluetoothOppSendFileInfo get invalid file");
                }
                Constants.updateShareStatus(this.mContext1, this.mInfo.mId, sendFileInfo.mStatus);
            } else {
                if (BluetoothOppObexClientSession.V) {
                    Log.v(BluetoothOppObexClientSession.TAG, "Generate BluetoothOppSendFileInfo:");
                    Log.v(BluetoothOppObexClientSession.TAG, "filepath  :" + sendFileInfo.mFilePath);
                    Log.v(BluetoothOppObexClientSession.TAG, "filename  :" + sendFileInfo.mFileName);
                    Log.v(BluetoothOppObexClientSession.TAG, "length    :" + sendFileInfo.mLength);
                    Log.v(BluetoothOppObexClientSession.TAG, "mimetype  :" + sendFileInfo.mMimetype);
                }
                ContentValues contentValues = new ContentValues();
                Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + this.mInfo.mId);
                contentValues.put(BluetoothShare._DATA, sendFileInfo.mFilePath);
                contentValues.put(BluetoothShare.FILENAME_HINT, sendFileInfo.mFileName);
                contentValues.put(BluetoothShare.TOTAL_BYTES, Long.valueOf(sendFileInfo.mLength));
                contentValues.put(BluetoothShare.MIMETYPE, sendFileInfo.mMimetype);
                this.mContext1.getContentResolver().update(uri, contentValues, null, null);
            }
            return sendFileInfo;
        }

        private int sendFile(BluetoothOppSendFileInfo bluetoothOppSendFileInfo) throws Throwable {
            Throwable th;
            ClientOperation clientOperation;
            InputStream inputStreamOpenInputStream;
            int i;
            ?? r21;
            ?? r212;
            boolean z;
            boolean z2;
            boolean z3;
            int i2;
            int i3;
            ?? r4;
            ?? r42;
            ?? r43;
            boolean z4;
            boolean z5;
            boolean z6;
            boolean z7;
            long j;
            boolean z8;
            OutputStream outputStreamOpenOutputStream;
            boolean z9;
            boolean z10;
            int i4;
            boolean z11;
            boolean z12;
            boolean z13;
            boolean z14;
            boolean z15;
            boolean z16;
            boolean z17;
            boolean z18;
            boolean z19;
            boolean z20;
            long jElapsedRealtime;
            long j2;
            long j3;
            long j4;
            int i5;
            boolean z21;
            int i6;
            OutputStream outputStream;
            InputStream inputStream;
            boolean z22;
            int i7;
            byte[] bArr;
            OutputStream outputStream2;
            Log.i(BluetoothOppObexClientSession.TAG, "sendFile ++, fileName = " + bluetoothOppSendFileInfo.mFileName);
            ?? r44 = 1;
            if (bluetoothOppSendFileInfo.mFilePath != null) {
                BluetoothOppObexClientSession.this.mContext.getString(R.string.notification_sending, bluetoothOppSendFileInfo.mFilePath);
            } else {
                BluetoothOppObexClientSession.this.mContext.getString(R.string.notification_sending, bluetoothOppSendFileInfo.mFileName);
            }
            Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + this.mInfo.mId);
            HeaderSet headerSet = new HeaderSet();
            int i8 = BluetoothShare.STATUS_OBEX_DATA_ERROR;
            long j5 = 0;
            OutputStream outputStream3 = null;
            int i9 = 200;
            try {
                try {
                    synchronized (this) {
                        try {
                            BluetoothOppObexClientSession.this.mWaitingForRemote = true;
                        } catch (Throwable th2) {
                            th = th2;
                            while (true) {
                                try {
                                    try {
                                        break;
                                    } catch (IOException e) {
                                        e = e;
                                        outputStream3 = null;
                                        clientOperation = null;
                                        inputStreamOpenInputStream = null;
                                        i8 = 200;
                                        j5 = 0;
                                        z3 = false;
                                        handleSendException(e.toString());
                                        e.printStackTrace();
                                        try {
                                            BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                            ?? r45 = z3;
                                            if (z3) {
                                            }
                                        } catch (IOException e2) {
                                            e = e2;
                                            i2 = i8;
                                        }
                                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                        return i3;
                                    } catch (IndexOutOfBoundsException e3) {
                                        e = e3;
                                        outputStream3 = null;
                                        clientOperation = null;
                                        inputStreamOpenInputStream = null;
                                        i8 = 200;
                                        j5 = 0;
                                        z2 = false;
                                        handleSendException(e.toString());
                                        e.printStackTrace();
                                        try {
                                            BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                            ?? r46 = z2;
                                            if (z2) {
                                            }
                                        } catch (IOException e4) {
                                            e = e4;
                                            i2 = i8;
                                        }
                                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                        return i3;
                                    } catch (NullPointerException e5) {
                                        e = e5;
                                        outputStream3 = null;
                                        clientOperation = null;
                                        inputStreamOpenInputStream = null;
                                        i8 = 200;
                                        j5 = 0;
                                        z = false;
                                        handleSendException(e.toString());
                                        e.printStackTrace();
                                        try {
                                            BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                            ?? r47 = z;
                                            if (z) {
                                            }
                                        } catch (IOException e6) {
                                            e = e6;
                                            i2 = i8;
                                        }
                                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                        return i3;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        outputStream3 = null;
                                        clientOperation = null;
                                        inputStreamOpenInputStream = null;
                                        i = 200;
                                        j5 = 0;
                                        r212 = 0;
                                        try {
                                            BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                            if (r212 == 0) {
                                            }
                                            if (inputStreamOpenInputStream != null) {
                                            }
                                            if (clientOperation != null) {
                                            }
                                            Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                                            throw th;
                                        } catch (IOException e7) {
                                            Log.e(BluetoothOppObexClientSession.TAG, "Error when closing stream after send");
                                            e7.printStackTrace();
                                            if (j5 == bluetoothOppSendFileInfo.mLength) {
                                                throw th;
                                            }
                                            Constants.updateShareStatus(this.mContext1, this.mInfo.mId, BluetoothShare.STATUS_FORBIDDEN);
                                            throw th;
                                        }
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                }
                            }
                            throw th;
                        }
                    }
                } catch (Throwable th5) {
                    th = th5;
                    r21 = r44;
                }
            } catch (IOException e8) {
                e = e8;
                clientOperation = null;
                inputStreamOpenInputStream = null;
                i8 = 200;
            } catch (IndexOutOfBoundsException e9) {
                e = e9;
                clientOperation = null;
                inputStreamOpenInputStream = null;
                i8 = 200;
            } catch (NullPointerException e10) {
                e = e10;
                clientOperation = null;
                inputStreamOpenInputStream = null;
                i8 = 200;
            } catch (Throwable th6) {
                th = th6;
                clientOperation = null;
                inputStreamOpenInputStream = null;
                i = 200;
            }
            try {
                if (BluetoothOppObexClientSession.V) {
                    Log.v(BluetoothOppObexClientSession.TAG, "Set header items for " + bluetoothOppSendFileInfo.mFileName);
                }
                headerSet.setHeader(1, bluetoothOppSendFileInfo.mFileName);
                headerSet.setHeader(66, bluetoothOppSendFileInfo.mMimetype);
                BluetoothOppObexClientSession.applyRemoteDeviceQuirks(headerSet, this.mInfo.mDestination, bluetoothOppSendFileInfo.mFileName);
                Constants.updateShareStatus(this.mContext1, this.mInfo.mId, BluetoothShare.STATUS_RUNNING);
                headerSet.setHeader(195, Long.valueOf(bluetoothOppSendFileInfo.mLength));
                if (BluetoothOppObexClientSession.V) {
                    Log.v(BluetoothOppObexClientSession.TAG, "put headerset for " + bluetoothOppSendFileInfo.mFileName);
                }
                clientOperation = (ClientOperation) this.mCs.put(headerSet);
                z7 = false;
            } catch (IOException e11) {
                try {
                    Constants.updateShareStatus(this.mContext1, this.mInfo.mId, BluetoothShare.STATUS_OBEX_DATA_ERROR);
                    Log.e(BluetoothOppObexClientSession.TAG, "Error when put HeaderSet ");
                    z7 = true;
                    i9 = 496;
                    clientOperation = null;
                } catch (IOException e12) {
                    e = e12;
                    z3 = false;
                    clientOperation = null;
                    inputStreamOpenInputStream = null;
                    handleSendException(e.toString());
                    e.printStackTrace();
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    ?? r452 = z3;
                    if (z3) {
                    }
                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                    return i3;
                } catch (IndexOutOfBoundsException e13) {
                    e = e13;
                    z2 = false;
                    clientOperation = null;
                    inputStreamOpenInputStream = null;
                    handleSendException(e.toString());
                    e.printStackTrace();
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    ?? r462 = z2;
                    if (z2) {
                    }
                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                    return i3;
                } catch (NullPointerException e14) {
                    e = e14;
                    z = false;
                    clientOperation = null;
                    inputStreamOpenInputStream = null;
                    handleSendException(e.toString());
                    e.printStackTrace();
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    ?? r472 = z;
                    if (z) {
                    }
                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                    return i3;
                } catch (Throwable th7) {
                    th = th7;
                    r212 = 0;
                    i = 496;
                    clientOperation = null;
                    inputStreamOpenInputStream = null;
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    if (r212 == 0) {
                    }
                    if (inputStreamOpenInputStream != null) {
                    }
                    if (clientOperation != null) {
                    }
                    Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                    throw th;
                }
            } catch (IllegalArgumentException e15) {
                try {
                    Constants.updateShareStatus(this.mContext1, this.mInfo.mId, BluetoothShare.STATUS_OBEX_DATA_ERROR);
                    Log.e(BluetoothOppObexClientSession.TAG, "Error setting header items for request: " + e15);
                    z7 = true;
                    i9 = 496;
                    clientOperation = null;
                } catch (IOException e16) {
                    e = e16;
                    clientOperation = null;
                    inputStreamOpenInputStream = null;
                    z3 = false;
                    handleSendException(e.toString());
                    e.printStackTrace();
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    ?? r4522 = z3;
                    if (z3) {
                    }
                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                    return i3;
                } catch (IndexOutOfBoundsException e17) {
                    e = e17;
                    clientOperation = null;
                    inputStreamOpenInputStream = null;
                    z2 = false;
                    handleSendException(e.toString());
                    e.printStackTrace();
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    ?? r4622 = z2;
                    if (z2) {
                    }
                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                    return i3;
                } catch (NullPointerException e18) {
                    e = e18;
                    clientOperation = null;
                    inputStreamOpenInputStream = null;
                    z = false;
                    handleSendException(e.toString());
                    e.printStackTrace();
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    ?? r4722 = z;
                    if (z) {
                    }
                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                    return i3;
                } catch (Throwable th8) {
                    th = th8;
                    i = 496;
                    clientOperation = null;
                    inputStreamOpenInputStream = null;
                    r212 = 0;
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    if (r212 == 0) {
                    }
                    if (inputStreamOpenInputStream != null) {
                    }
                    if (clientOperation != null) {
                    }
                    Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                    throw th;
                }
            } catch (IndexOutOfBoundsException e19) {
                e = e19;
                z6 = false;
                clientOperation = null;
                inputStreamOpenInputStream = null;
                i8 = i9;
                z2 = z6;
                handleSendException(e.toString());
                e.printStackTrace();
                BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                ?? r46222 = z2;
                if (z2) {
                }
                BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                return i3;
            } catch (NullPointerException e20) {
                e = e20;
                z5 = false;
                clientOperation = null;
                inputStreamOpenInputStream = null;
                i8 = i9;
                z = z5;
                handleSendException(e.toString());
                e.printStackTrace();
                BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                ?? r47222 = z;
                if (z) {
                }
                BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                return i3;
            } catch (Throwable th9) {
                th = th9;
                z4 = false;
                clientOperation = null;
                inputStreamOpenInputStream = null;
                i = i9;
                r212 = z4;
                BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                if (r212 == 0) {
                }
                if (inputStreamOpenInputStream != null) {
                }
                if (clientOperation != null) {
                }
                Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                throw th;
            }
            synchronized (this) {
                try {
                    BluetoothOppObexClientSession.this.mWaitingForRemote = false;
                } catch (Throwable th10) {
                    th = th10;
                    j = 0;
                    while (true) {
                        try {
                            try {
                                break;
                            } catch (IOException e21) {
                                e = e21;
                                z3 = z7 ? 1 : 0;
                                outputStream3 = null;
                                inputStreamOpenInputStream = null;
                                i8 = i9;
                                j5 = 0;
                                handleSendException(e.toString());
                                e.printStackTrace();
                                BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                ?? r45222 = z3;
                                if (z3) {
                                }
                                BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                return i3;
                            } catch (IndexOutOfBoundsException e22) {
                                e = e22;
                                z2 = z7 ? 1 : 0;
                                outputStream3 = null;
                                inputStreamOpenInputStream = null;
                                i8 = i9;
                                j5 = 0;
                                handleSendException(e.toString());
                                e.printStackTrace();
                                BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                ?? r462222 = z2;
                                if (z2) {
                                }
                                BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                return i3;
                            } catch (NullPointerException e23) {
                                e = e23;
                                z = z7 ? 1 : 0;
                                outputStream3 = null;
                                inputStreamOpenInputStream = null;
                                i8 = i9;
                                j5 = 0;
                                handleSendException(e.toString());
                                e.printStackTrace();
                                BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                ?? r472222 = z;
                                if (z) {
                                }
                                BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                return i3;
                            } catch (Throwable th11) {
                                th = th11;
                                z8 = z7 ? 1 : 0;
                                outputStream3 = null;
                                inputStreamOpenInputStream = null;
                                i = i9;
                                j5 = j;
                                r212 = z8;
                                BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                if (r212 == 0) {
                                }
                                if (inputStreamOpenInputStream != null) {
                                }
                                if (clientOperation != null) {
                                }
                                Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                                throw th;
                            }
                        } catch (Throwable th12) {
                            th = th12;
                        }
                    }
                    throw th;
                }
            }
            if (z7) {
                outputStreamOpenOutputStream = null;
                inputStreamOpenInputStream = null;
            } else {
                try {
                    if (BluetoothOppObexClientSession.V) {
                        Log.v(BluetoothOppObexClientSession.TAG, "openOutputStream " + bluetoothOppSendFileInfo.mFileName);
                    }
                    outputStreamOpenOutputStream = clientOperation.openOutputStream();
                } catch (IOException e24) {
                    outputStreamOpenOutputStream = null;
                } catch (IndexOutOfBoundsException e25) {
                    e = e25;
                    inputStreamOpenInputStream = null;
                    z6 = z7 ? 1 : 0;
                } catch (NullPointerException e26) {
                    e = e26;
                    inputStreamOpenInputStream = null;
                    z5 = z7 ? 1 : 0;
                } catch (Throwable th13) {
                    th = th13;
                    inputStreamOpenInputStream = null;
                    z4 = z7 ? 1 : 0;
                }
                try {
                    inputStreamOpenInputStream = clientOperation.openInputStream();
                } catch (IOException e27) {
                    try {
                        Constants.updateShareStatus(this.mContext1, this.mInfo.mId, BluetoothShare.STATUS_OBEX_DATA_ERROR);
                        Log.e(BluetoothOppObexClientSession.TAG, "Error when openOutputStream");
                        z7 = true;
                        inputStreamOpenInputStream = null;
                    } catch (IOException e28) {
                        e = e28;
                        inputStreamOpenInputStream = null;
                        z3 = z7;
                        outputStream3 = outputStreamOpenOutputStream;
                        handleSendException(e.toString());
                        e.printStackTrace();
                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                        ?? r452222 = z3;
                        if (z3) {
                        }
                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                        return i3;
                    } catch (IndexOutOfBoundsException e29) {
                        e = e29;
                        inputStreamOpenInputStream = null;
                        z2 = z7;
                        outputStream3 = outputStreamOpenOutputStream;
                        handleSendException(e.toString());
                        e.printStackTrace();
                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                        ?? r4622222 = z2;
                        if (z2) {
                        }
                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                        return i3;
                    } catch (NullPointerException e30) {
                        e = e30;
                        inputStreamOpenInputStream = null;
                        z = z7;
                        outputStream3 = outputStreamOpenOutputStream;
                        handleSendException(e.toString());
                        e.printStackTrace();
                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                        ?? r4722222 = z;
                        if (z) {
                        }
                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                        return i3;
                    } catch (Throwable th14) {
                        th = th14;
                        i = 496;
                        inputStreamOpenInputStream = null;
                        z9 = z7;
                        z10 = z9;
                        outputStream3 = outputStreamOpenOutputStream;
                        r212 = z10;
                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                        if (r212 == 0) {
                        }
                        if (inputStreamOpenInputStream != null) {
                        }
                        if (clientOperation != null) {
                        }
                        Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                        throw th;
                    }
                } catch (IndexOutOfBoundsException e31) {
                    e = e31;
                    inputStreamOpenInputStream = null;
                    z6 = z7 ? 1 : 0;
                    outputStream3 = outputStreamOpenOutputStream;
                    i8 = i9;
                    z2 = z6;
                    handleSendException(e.toString());
                    e.printStackTrace();
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    ?? r46222222 = z2;
                    if (z2) {
                    }
                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                    return i3;
                } catch (NullPointerException e32) {
                    e = e32;
                    inputStreamOpenInputStream = null;
                    z5 = z7 ? 1 : 0;
                    outputStream3 = outputStreamOpenOutputStream;
                    i8 = i9;
                    z = z5;
                    handleSendException(e.toString());
                    e.printStackTrace();
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    ?? r47222222 = z;
                    if (z) {
                    }
                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                    return i3;
                } catch (Throwable th15) {
                    th = th15;
                    inputStreamOpenInputStream = null;
                    z4 = z7 ? 1 : 0;
                    outputStream3 = outputStreamOpenOutputStream;
                    i = i9;
                    r212 = z4;
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    if (r212 == 0) {
                    }
                    if (inputStreamOpenInputStream != null) {
                    }
                    if (clientOperation != null) {
                    }
                    Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                    throw th;
                }
            }
            i8 = i9;
            z7 = z7;
            if (!z7) {
                try {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(BluetoothShare.CURRENT_BYTES, (Integer) 0);
                    contentValues.put("status", Integer.valueOf(BluetoothShare.STATUS_RUNNING));
                    this.mContext1.getContentResolver().update(uri, contentValues, null, null);
                } catch (IOException e33) {
                    e = e33;
                    z3 = z7;
                    outputStream3 = outputStreamOpenOutputStream;
                    handleSendException(e.toString());
                    e.printStackTrace();
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    ?? r4522222 = z3;
                    if (z3) {
                    }
                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                    return i3;
                } catch (IndexOutOfBoundsException e34) {
                    e = e34;
                    z2 = z7;
                    outputStream3 = outputStreamOpenOutputStream;
                    handleSendException(e.toString());
                    e.printStackTrace();
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    ?? r462222222 = z2;
                    if (z2) {
                    }
                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                    return i3;
                } catch (NullPointerException e35) {
                    e = e35;
                    z = z7;
                    outputStream3 = outputStreamOpenOutputStream;
                    handleSendException(e.toString());
                    e.printStackTrace();
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    ?? r472222222 = z;
                    if (z) {
                    }
                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                    return i3;
                } catch (Throwable th16) {
                    th = th16;
                    i = i8;
                    z9 = z7;
                    z10 = z9;
                    outputStream3 = outputStreamOpenOutputStream;
                    r212 = z10;
                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                    if (r212 == 0) {
                    }
                    if (inputStreamOpenInputStream != null) {
                    }
                    if (clientOperation != null) {
                    }
                    Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                    throw th;
                }
            }
            if (!z7) {
                try {
                    long jElapsedRealtime2 = SystemClock.elapsedRealtime();
                    int maxPacketSize = clientOperation.getMaxPacketSize();
                    byte[] bArr2 = new byte[maxPacketSize];
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(bluetoothOppSendFileInfo.mInputStream, 16384);
                    if (!BluetoothOppObexClientSession.this.mInterrupted) {
                        try {
                            if (0 != bluetoothOppSendFileInfo.mLength) {
                                int fully = BluetoothOppObexClientSession.readFully(bufferedInputStream, bArr2, maxPacketSize);
                                if (BluetoothOppObexClientSession.this.mCallback != null) {
                                    try {
                                        Handler handler = BluetoothOppObexClientSession.this.mCallback;
                                        Message messageObtainMessage = BluetoothOppObexClientSession.this.mCallback.obtainMessage(4);
                                        z19 = z7 ? 1 : 0;
                                        try {
                                            handler.sendMessageDelayed(messageObtainMessage, 50000L);
                                            z20 = z19;
                                        } catch (IOException e36) {
                                            e = e36;
                                            outputStream3 = outputStreamOpenOutputStream;
                                            z3 = z19;
                                            j5 = 0;
                                            handleSendException(e.toString());
                                            e.printStackTrace();
                                            BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                            ?? r45222222 = z3;
                                            if (z3) {
                                            }
                                            BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                            return i3;
                                        } catch (IndexOutOfBoundsException e37) {
                                            e = e37;
                                            outputStream3 = outputStreamOpenOutputStream;
                                            z2 = z19;
                                            j5 = 0;
                                            handleSendException(e.toString());
                                            e.printStackTrace();
                                            BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                            ?? r4622222222 = z2;
                                            if (z2) {
                                            }
                                            BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                            return i3;
                                        } catch (NullPointerException e38) {
                                            e = e38;
                                            outputStream3 = outputStreamOpenOutputStream;
                                            z = z19;
                                            j5 = 0;
                                            handleSendException(e.toString());
                                            e.printStackTrace();
                                            BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                            ?? r4722222222 = z;
                                            if (z) {
                                            }
                                            BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                            return i3;
                                        } catch (Throwable th17) {
                                            th = th17;
                                            th = th;
                                            i = i8;
                                            outputStream3 = outputStreamOpenOutputStream;
                                            j5 = 0;
                                            r212 = z19;
                                            BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                            if (r212 == 0) {
                                            }
                                            if (inputStreamOpenInputStream != null) {
                                            }
                                            if (clientOperation != null) {
                                            }
                                            Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                                            throw th;
                                        }
                                    } catch (IOException e39) {
                                        e = e39;
                                        z19 = z7 ? 1 : 0;
                                    } catch (IndexOutOfBoundsException e40) {
                                        e = e40;
                                        z19 = z7 ? 1 : 0;
                                    } catch (NullPointerException e41) {
                                        e = e41;
                                        z19 = z7 ? 1 : 0;
                                    } catch (Throwable th18) {
                                        th = th18;
                                        z19 = z7 ? 1 : 0;
                                    }
                                } else {
                                    z20 = z7 ? 1 : 0;
                                }
                                try {
                                    synchronized (this) {
                                        try {
                                            BluetoothOppObexClientSession.this.mWaitingForRemote = true;
                                        } catch (Throwable th19) {
                                            th = th19;
                                            j = 0;
                                            while (true) {
                                                try {
                                                    try {
                                                        break;
                                                    } catch (IOException e42) {
                                                        e = e42;
                                                        outputStream3 = outputStreamOpenOutputStream;
                                                        j5 = j;
                                                        z17 = z20;
                                                        z3 = z17;
                                                        handleSendException(e.toString());
                                                        e.printStackTrace();
                                                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                        ?? r452222222 = z3;
                                                        if (z3) {
                                                        }
                                                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                                        return i3;
                                                    } catch (IndexOutOfBoundsException e43) {
                                                        e = e43;
                                                        outputStream3 = outputStreamOpenOutputStream;
                                                        j5 = j;
                                                        z16 = z20;
                                                        z2 = z16;
                                                        handleSendException(e.toString());
                                                        e.printStackTrace();
                                                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                        ?? r46222222222 = z2;
                                                        if (z2) {
                                                        }
                                                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                                        return i3;
                                                    } catch (NullPointerException e44) {
                                                        e = e44;
                                                        outputStream3 = outputStreamOpenOutputStream;
                                                        j5 = j;
                                                        z15 = z20;
                                                        z = z15;
                                                        handleSendException(e.toString());
                                                        e.printStackTrace();
                                                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                        ?? r47222222222 = z;
                                                        if (z) {
                                                        }
                                                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                                        return i3;
                                                    } catch (Throwable th20) {
                                                        th = th20;
                                                        th = th;
                                                        i = i8;
                                                        outputStream3 = outputStreamOpenOutputStream;
                                                        z8 = z20;
                                                        j5 = j;
                                                        r212 = z8;
                                                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                        if (r212 == 0) {
                                                        }
                                                        if (inputStreamOpenInputStream != null) {
                                                        }
                                                        if (clientOperation != null) {
                                                        }
                                                        Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                                                        throw th;
                                                    }
                                                } catch (Throwable th21) {
                                                    th = th21;
                                                }
                                            }
                                            throw th;
                                        }
                                    }
                                    outputStreamOpenOutputStream.write(bArr2, 0, fully);
                                    jElapsedRealtime = 0;
                                    j2 = 0 + ((long) fully);
                                } catch (IOException e45) {
                                    e = e45;
                                    j = 0;
                                } catch (IndexOutOfBoundsException e46) {
                                    e = e46;
                                    j = 0;
                                } catch (NullPointerException e47) {
                                    e = e47;
                                    j = 0;
                                } catch (Throwable th22) {
                                    th = th22;
                                    j = 0;
                                }
                                try {
                                    if (j2 == bluetoothOppSendFileInfo.mLength) {
                                        outputStreamOpenOutputStream.close();
                                        outputStream3 = null;
                                    } else {
                                        outputStream3 = outputStreamOpenOutputStream;
                                    }
                                    try {
                                        int responseCode = clientOperation.getResponseCode();
                                        if (BluetoothOppObexClientSession.this.mCallback != null) {
                                            BluetoothOppObexClientSession.this.mCallback.removeMessages(4);
                                        }
                                        synchronized (this) {
                                            BluetoothOppObexClientSession.this.mWaitingForRemote = false;
                                        }
                                        if (responseCode == 144 || responseCode == 160) {
                                            if (BluetoothOppObexClientSession.V) {
                                                Log.v(BluetoothOppObexClientSession.TAG, "Remote accept");
                                            }
                                            ContentValues contentValues2 = new ContentValues();
                                            contentValues2.put(BluetoothShare.CURRENT_BYTES, Long.valueOf(j2));
                                            this.mContext1.getContentResolver().update(uri, contentValues2, null, null);
                                            BluetoothOppObexClientSession.access$408(BluetoothOppObexClientSession.this);
                                            j3 = jElapsedRealtime2;
                                            j4 = 0;
                                            i5 = responseCode;
                                            j5 = j2;
                                            z21 = true;
                                        } else {
                                            Log.i(BluetoothOppObexClientSession.TAG, "Remote reject, Response code is " + responseCode);
                                            j3 = jElapsedRealtime2;
                                            j4 = 0L;
                                            i5 = responseCode;
                                            j5 = j2;
                                            z21 = false;
                                        }
                                    } catch (IOException e48) {
                                        e = e48;
                                        j5 = j2;
                                        z17 = z20;
                                        z3 = z17;
                                        handleSendException(e.toString());
                                        e.printStackTrace();
                                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                        ?? r4522222222 = z3;
                                        if (z3) {
                                        }
                                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                        return i3;
                                    } catch (IndexOutOfBoundsException e49) {
                                        e = e49;
                                        j5 = j2;
                                        z16 = z20;
                                        z2 = z16;
                                        handleSendException(e.toString());
                                        e.printStackTrace();
                                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                        ?? r462222222222 = z2;
                                        if (z2) {
                                        }
                                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                        return i3;
                                    } catch (NullPointerException e50) {
                                        e = e50;
                                        j5 = j2;
                                        z15 = z20;
                                        z = z15;
                                        handleSendException(e.toString());
                                        e.printStackTrace();
                                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                        ?? r472222222222 = z;
                                        if (z) {
                                        }
                                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                        return i3;
                                    } catch (Throwable th23) {
                                        th = th23;
                                        j5 = j2;
                                        r21 = z20;
                                        i = i8;
                                        r212 = r21;
                                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                        if (r212 == 0) {
                                        }
                                        if (inputStreamOpenInputStream != null) {
                                        }
                                        if (clientOperation != null) {
                                        }
                                        Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                                        throw th;
                                    }
                                } catch (IOException e51) {
                                    e = e51;
                                    j5 = j2;
                                    z14 = z20;
                                    outputStream3 = outputStreamOpenOutputStream;
                                    z17 = z14;
                                    z3 = z17;
                                    handleSendException(e.toString());
                                    e.printStackTrace();
                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                    ?? r45222222222 = z3;
                                    if (z3) {
                                    }
                                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                    return i3;
                                } catch (IndexOutOfBoundsException e52) {
                                    e = e52;
                                    j5 = j2;
                                    z13 = z20;
                                    outputStream3 = outputStreamOpenOutputStream;
                                    z16 = z13;
                                    z2 = z16;
                                    handleSendException(e.toString());
                                    e.printStackTrace();
                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                    ?? r4622222222222 = z2;
                                    if (z2) {
                                    }
                                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                    return i3;
                                } catch (NullPointerException e53) {
                                    e = e53;
                                    j5 = j2;
                                    z12 = z20;
                                    outputStream3 = outputStreamOpenOutputStream;
                                    z15 = z12;
                                    z = z15;
                                    handleSendException(e.toString());
                                    e.printStackTrace();
                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                    ?? r4722222222222 = z;
                                    if (z) {
                                    }
                                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                    return i3;
                                } catch (Throwable th24) {
                                    th = th24;
                                    j5 = j2;
                                    z18 = z20;
                                    i = i8;
                                    z10 = z18;
                                    outputStream3 = outputStreamOpenOutputStream;
                                    r212 = z10;
                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                    if (r212 == 0) {
                                    }
                                    if (inputStreamOpenInputStream != null) {
                                    }
                                    if (clientOperation != null) {
                                    }
                                    Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                                    throw th;
                                }
                            } else {
                                jElapsedRealtime = 0;
                                z20 = z7 ? 1 : 0;
                                outputStream3 = outputStreamOpenOutputStream;
                                j3 = jElapsedRealtime2;
                                j5 = 0;
                                j4 = 0;
                                z21 = false;
                                i5 = -1;
                            }
                            while (!BluetoothOppObexClientSession.this.mInterrupted && z21) {
                                try {
                                    inputStream = inputStreamOpenInputStream;
                                    try {
                                        if (j5 >= bluetoothOppSendFileInfo.mLength) {
                                            i6 = i8;
                                            outputStream = outputStream3;
                                            break;
                                        }
                                        if (BluetoothOppObexClientSession.V) {
                                            try {
                                                jElapsedRealtime = SystemClock.elapsedRealtime();
                                            } catch (IOException e54) {
                                                e = e54;
                                                z3 = z20 ? 1 : 0;
                                                inputStreamOpenInputStream = inputStream;
                                                handleSendException(e.toString());
                                                e.printStackTrace();
                                                BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                ?? r452222222222 = z3;
                                                if (z3) {
                                                }
                                                BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                                return i3;
                                            } catch (IndexOutOfBoundsException e55) {
                                                e = e55;
                                                z2 = z20 ? 1 : 0;
                                                inputStreamOpenInputStream = inputStream;
                                                handleSendException(e.toString());
                                                e.printStackTrace();
                                                BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                ?? r46222222222222 = z2;
                                                if (z2) {
                                                }
                                                BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                                return i3;
                                            } catch (NullPointerException e56) {
                                                e = e56;
                                                z = z20 ? 1 : 0;
                                                inputStreamOpenInputStream = inputStream;
                                                handleSendException(e.toString());
                                                e.printStackTrace();
                                                BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                ?? r47222222222222 = z;
                                                if (z) {
                                                }
                                                BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                                return i3;
                                            } catch (Throwable th25) {
                                                th = th25;
                                                i = i8;
                                                inputStreamOpenInputStream = inputStream;
                                                r212 = z20;
                                                BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                if (r212 == 0) {
                                                }
                                                if (inputStreamOpenInputStream != null) {
                                                }
                                                if (clientOperation != null) {
                                                }
                                                Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                                                throw th;
                                            }
                                        }
                                        int i10 = bufferedInputStream.read(bArr2, 0, maxPacketSize);
                                        outputStream3.write(bArr2, 0, i10);
                                        int responseCode2 = clientOperation.getResponseCode();
                                        if (BluetoothOppObexClientSession.V) {
                                            StringBuilder sb = new StringBuilder();
                                            i7 = maxPacketSize;
                                            sb.append("Response code is ");
                                            sb.append(responseCode2);
                                            Log.v(BluetoothOppObexClientSession.TAG, sb.toString());
                                        } else {
                                            i7 = maxPacketSize;
                                        }
                                        if (responseCode2 == 144 || responseCode2 == 160) {
                                            boolean z23 = z21;
                                            j5 += (long) i10;
                                            long jElapsedRealtime3 = SystemClock.elapsedRealtime();
                                            if (BluetoothOppObexClientSession.V) {
                                                StringBuilder sb2 = new StringBuilder();
                                                i4 = i8;
                                                try {
                                                    sb2.append("Sending file position = ");
                                                    sb2.append(j5);
                                                    sb2.append(" readLength ");
                                                    sb2.append(i10);
                                                    sb2.append(" bytes took ");
                                                    bArr = bArr2;
                                                    outputStream2 = outputStream3;
                                                    try {
                                                        try {
                                                            sb2.append(jElapsedRealtime3 - jElapsedRealtime);
                                                            sb2.append(" ms");
                                                            Log.v(BluetoothOppObexClientSession.TAG, sb2.toString());
                                                        } catch (IOException e57) {
                                                            e = e57;
                                                            z3 = z20 ? 1 : 0;
                                                            inputStreamOpenInputStream = inputStream;
                                                            i8 = i4;
                                                            outputStream3 = outputStream2;
                                                            handleSendException(e.toString());
                                                            e.printStackTrace();
                                                            BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                            ?? r4522222222222 = z3;
                                                            if (z3) {
                                                                i3 = i8;
                                                                r43 = r4522222222222;
                                                                if (inputStreamOpenInputStream != null) {
                                                                    try {
                                                                        inputStreamOpenInputStream.close();
                                                                    } catch (IOException e58) {
                                                                        e = e58;
                                                                        i2 = i3;
                                                                        Log.e(BluetoothOppObexClientSession.TAG, "Error when closing stream after send");
                                                                        e.printStackTrace();
                                                                        if (j5 != bluetoothOppSendFileInfo.mLength) {
                                                                        }
                                                                    }
                                                                }
                                                                if (clientOperation != null) {
                                                                    clientOperation.close();
                                                                }
                                                                Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i3);
                                                                r44 = r43;
                                                            } else {
                                                                if (outputStream3 != null) {
                                                                    outputStream3.close();
                                                                }
                                                                int responseCode3 = clientOperation.getResponseCode();
                                                                if (responseCode3 != -1) {
                                                                    ?? r48 = z3;
                                                                    if (BluetoothOppObexClientSession.V) {
                                                                        String str = "Get response code " + responseCode3;
                                                                        Log.v(BluetoothOppObexClientSession.TAG, str);
                                                                        r48 = str;
                                                                    }
                                                                    r4522222222222 = r48;
                                                                    if (responseCode3 != 160) {
                                                                        String str2 = "Response error code is " + responseCode3;
                                                                        Log.i(BluetoothOppObexClientSession.TAG, str2);
                                                                        i3 = responseCode3 == 207 ? BluetoothShare.STATUS_NOT_ACCEPTABLE : BluetoothShare.STATUS_UNHANDLED_OBEX_CODE;
                                                                        if (responseCode3 != 195) {
                                                                            r43 = str2;
                                                                            if (responseCode3 == 198) {
                                                                                i3 = BluetoothShare.STATUS_FORBIDDEN;
                                                                                r43 = str2;
                                                                            }
                                                                        }
                                                                    }
                                                                } else {
                                                                    i3 = BluetoothShare.STATUS_CONNECTION_ERROR;
                                                                    r43 = z3;
                                                                }
                                                                if (inputStreamOpenInputStream != null) {
                                                                }
                                                                if (clientOperation != null) {
                                                                }
                                                                Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i3);
                                                                r44 = r43;
                                                            }
                                                            BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                                            return i3;
                                                        }
                                                    } catch (IndexOutOfBoundsException e59) {
                                                        e = e59;
                                                        z2 = z20 ? 1 : 0;
                                                        inputStreamOpenInputStream = inputStream;
                                                        i8 = i4;
                                                        outputStream3 = outputStream2;
                                                        handleSendException(e.toString());
                                                        e.printStackTrace();
                                                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                        ?? r462222222222222 = z2;
                                                        if (z2) {
                                                            i3 = i8;
                                                            r42 = r462222222222222;
                                                            if (inputStreamOpenInputStream != null) {
                                                                try {
                                                                    inputStreamOpenInputStream.close();
                                                                } catch (IOException e60) {
                                                                    e = e60;
                                                                    i2 = i3;
                                                                    Log.e(BluetoothOppObexClientSession.TAG, "Error when closing stream after send");
                                                                    e.printStackTrace();
                                                                    if (j5 != bluetoothOppSendFileInfo.mLength) {
                                                                    }
                                                                }
                                                            }
                                                            if (clientOperation != null) {
                                                                clientOperation.close();
                                                            }
                                                            Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i3);
                                                            r44 = r42;
                                                        } else {
                                                            if (outputStream3 != null) {
                                                                outputStream3.close();
                                                            }
                                                            int responseCode4 = clientOperation.getResponseCode();
                                                            if (responseCode4 != -1) {
                                                                ?? r49 = z2;
                                                                if (BluetoothOppObexClientSession.V) {
                                                                    String str3 = "Get response code " + responseCode4;
                                                                    Log.v(BluetoothOppObexClientSession.TAG, str3);
                                                                    r49 = str3;
                                                                }
                                                                r462222222222222 = r49;
                                                                if (responseCode4 != 160) {
                                                                    String str4 = "Response error code is " + responseCode4;
                                                                    Log.i(BluetoothOppObexClientSession.TAG, str4);
                                                                    i3 = responseCode4 == 207 ? BluetoothShare.STATUS_NOT_ACCEPTABLE : BluetoothShare.STATUS_UNHANDLED_OBEX_CODE;
                                                                    if (responseCode4 != 195) {
                                                                        r42 = str4;
                                                                        if (responseCode4 == 198) {
                                                                            i3 = BluetoothShare.STATUS_FORBIDDEN;
                                                                            r42 = str4;
                                                                        }
                                                                    }
                                                                }
                                                            } else {
                                                                i3 = BluetoothShare.STATUS_CONNECTION_ERROR;
                                                                r42 = z2;
                                                            }
                                                            if (inputStreamOpenInputStream != null) {
                                                            }
                                                            if (clientOperation != null) {
                                                            }
                                                            Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i3);
                                                            r44 = r42;
                                                        }
                                                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                                        return i3;
                                                    } catch (NullPointerException e61) {
                                                        e = e61;
                                                        z = z20 ? 1 : 0;
                                                        inputStreamOpenInputStream = inputStream;
                                                        i8 = i4;
                                                        outputStream3 = outputStream2;
                                                        handleSendException(e.toString());
                                                        e.printStackTrace();
                                                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                        ?? r472222222222222 = z;
                                                        if (z) {
                                                            i3 = i8;
                                                            r4 = r472222222222222;
                                                            if (inputStreamOpenInputStream != null) {
                                                                try {
                                                                    inputStreamOpenInputStream.close();
                                                                } catch (IOException e62) {
                                                                    e = e62;
                                                                    i2 = i3;
                                                                    Log.e(BluetoothOppObexClientSession.TAG, "Error when closing stream after send");
                                                                    e.printStackTrace();
                                                                    if (j5 != bluetoothOppSendFileInfo.mLength) {
                                                                        Constants.updateShareStatus(this.mContext1, this.mInfo.mId, BluetoothShare.STATUS_FORBIDDEN);
                                                                        i3 = BluetoothShare.STATUS_FORBIDDEN;
                                                                    } else {
                                                                        i3 = i2;
                                                                    }
                                                                }
                                                            }
                                                            if (clientOperation != null) {
                                                                clientOperation.close();
                                                            }
                                                            Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i3);
                                                            r44 = r4;
                                                        } else {
                                                            if (outputStream3 != null) {
                                                                outputStream3.close();
                                                            }
                                                            int responseCode5 = clientOperation.getResponseCode();
                                                            if (responseCode5 != -1) {
                                                                ?? r410 = z;
                                                                if (BluetoothOppObexClientSession.V) {
                                                                    String str5 = "Get response code " + responseCode5;
                                                                    Log.v(BluetoothOppObexClientSession.TAG, str5);
                                                                    r410 = str5;
                                                                }
                                                                r472222222222222 = r410;
                                                                if (responseCode5 != 160) {
                                                                    String str6 = "Response error code is " + responseCode5;
                                                                    Log.i(BluetoothOppObexClientSession.TAG, str6);
                                                                    i3 = responseCode5 == 207 ? BluetoothShare.STATUS_NOT_ACCEPTABLE : BluetoothShare.STATUS_UNHANDLED_OBEX_CODE;
                                                                    if (responseCode5 != 195) {
                                                                        r4 = str6;
                                                                        if (responseCode5 == 198) {
                                                                            i3 = BluetoothShare.STATUS_FORBIDDEN;
                                                                            r4 = str6;
                                                                        }
                                                                    }
                                                                }
                                                            } else {
                                                                i3 = BluetoothShare.STATUS_CONNECTION_ERROR;
                                                                r4 = z;
                                                            }
                                                            if (inputStreamOpenInputStream != null) {
                                                            }
                                                            if (clientOperation != null) {
                                                            }
                                                            Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i3);
                                                            r44 = r4;
                                                        }
                                                        BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                                        return i3;
                                                    } catch (Throwable th26) {
                                                        th = th26;
                                                        inputStreamOpenInputStream = inputStream;
                                                        i = i4;
                                                        outputStream3 = outputStream2;
                                                        r212 = z20;
                                                        BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                        if (r212 == 0) {
                                                            if (outputStream3 != null) {
                                                                outputStream3.close();
                                                            }
                                                            int responseCode6 = clientOperation.getResponseCode();
                                                            if (responseCode6 != -1) {
                                                                if (BluetoothOppObexClientSession.V) {
                                                                    Log.v(BluetoothOppObexClientSession.TAG, "Get response code " + responseCode6);
                                                                }
                                                                if (responseCode6 != 160) {
                                                                    Log.i(BluetoothOppObexClientSession.TAG, "Response error code is " + responseCode6);
                                                                    i = responseCode6 == 207 ? BluetoothShare.STATUS_NOT_ACCEPTABLE : BluetoothShare.STATUS_UNHANDLED_OBEX_CODE;
                                                                    if (responseCode6 == 195 || responseCode6 == 198) {
                                                                        i = BluetoothShare.STATUS_FORBIDDEN;
                                                                    }
                                                                }
                                                            } else {
                                                                i = BluetoothShare.STATUS_CONNECTION_ERROR;
                                                            }
                                                        }
                                                        if (inputStreamOpenInputStream != null) {
                                                            inputStreamOpenInputStream.close();
                                                        }
                                                        if (clientOperation != null) {
                                                            clientOperation.close();
                                                        }
                                                        Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                                                        throw th;
                                                    }
                                                } catch (IOException e63) {
                                                    e = e63;
                                                    z3 = z20 ? 1 : 0;
                                                    inputStreamOpenInputStream = inputStream;
                                                    i8 = i4;
                                                } catch (IndexOutOfBoundsException e64) {
                                                    e = e64;
                                                    z2 = z20 ? 1 : 0;
                                                    inputStreamOpenInputStream = inputStream;
                                                    i8 = i4;
                                                } catch (NullPointerException e65) {
                                                    e = e65;
                                                    z = z20 ? 1 : 0;
                                                    inputStreamOpenInputStream = inputStream;
                                                    i8 = i4;
                                                } catch (Throwable th27) {
                                                    th = th27;
                                                    th = th;
                                                    inputStreamOpenInputStream = inputStream;
                                                    z11 = z20;
                                                    i = i4;
                                                    r212 = z11;
                                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                                    if (r212 == 0) {
                                                    }
                                                    if (inputStreamOpenInputStream != null) {
                                                    }
                                                    if (clientOperation != null) {
                                                    }
                                                    Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                                                    throw th;
                                                }
                                            } else {
                                                i4 = i8;
                                                bArr = bArr2;
                                                outputStream2 = outputStream3;
                                            }
                                            long j6 = (100 * j5) / bluetoothOppSendFileInfo.mLength;
                                            if (j6 > j4 || jElapsedRealtime3 - j3 > 10000) {
                                                ContentValues contentValues3 = new ContentValues();
                                                contentValues3.put(BluetoothShare.CURRENT_BYTES, Long.valueOf(j5));
                                                this.mContext1.getContentResolver().update(uri, contentValues3, null, null);
                                                j3 = jElapsedRealtime3;
                                                j4 = j6;
                                            }
                                            inputStreamOpenInputStream = inputStream;
                                            maxPacketSize = i7;
                                            z21 = z23;
                                            i8 = i4;
                                            bArr2 = bArr;
                                            outputStream3 = outputStream2;
                                            i5 = responseCode2;
                                        } else {
                                            i5 = responseCode2;
                                            inputStreamOpenInputStream = inputStream;
                                            maxPacketSize = i7;
                                            z21 = false;
                                        }
                                    } catch (IOException e66) {
                                        e = e66;
                                    } catch (IndexOutOfBoundsException e67) {
                                        e = e67;
                                    } catch (NullPointerException e68) {
                                        e = e68;
                                    } catch (Throwable th28) {
                                        th = th28;
                                        i4 = i8;
                                    }
                                } catch (IOException e69) {
                                    e = e69;
                                    z17 = z20;
                                    z3 = z17;
                                    handleSendException(e.toString());
                                    e.printStackTrace();
                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                    ?? r45222222222222 = z3;
                                    if (z3) {
                                    }
                                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                    return i3;
                                } catch (IndexOutOfBoundsException e70) {
                                    e = e70;
                                    z16 = z20;
                                    z2 = z16;
                                    handleSendException(e.toString());
                                    e.printStackTrace();
                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                    ?? r4622222222222222 = z2;
                                    if (z2) {
                                    }
                                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                    return i3;
                                } catch (NullPointerException e71) {
                                    e = e71;
                                    z15 = z20;
                                    z = z15;
                                    handleSendException(e.toString());
                                    e.printStackTrace();
                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                    ?? r4722222222222222 = z;
                                    if (z) {
                                    }
                                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                    return i3;
                                } catch (Throwable th29) {
                                    i4 = i8;
                                    th = th29;
                                    z11 = z20;
                                }
                            }
                            i6 = i8;
                            outputStream = outputStream3;
                            inputStream = inputStreamOpenInputStream;
                            try {
                                bufferedInputStream.close();
                            } catch (IOException e72) {
                            }
                            if (i5 == 195 || i5 == 198) {
                                Log.i(BluetoothOppObexClientSession.TAG, "Remote reject file " + bluetoothOppSendFileInfo.mFileName + " length " + bluetoothOppSendFileInfo.mLength);
                                outputStreamOpenOutputStream = outputStream;
                                i3 = BluetoothShare.STATUS_FORBIDDEN;
                                z22 = z20;
                            } else if (i5 == 207) {
                                Log.i(BluetoothOppObexClientSession.TAG, "Remote reject file type " + bluetoothOppSendFileInfo.mMimetype);
                                outputStreamOpenOutputStream = outputStream;
                                i3 = BluetoothShare.STATUS_NOT_ACCEPTABLE;
                                z22 = z20;
                            } else if (BluetoothOppObexClientSession.this.mInterrupted || j5 != bluetoothOppSendFileInfo.mLength) {
                                i8 = BluetoothShare.STATUS_CANCELED;
                                try {
                                    clientOperation.abort();
                                    Log.i(BluetoothOppObexClientSession.TAG, "SendFile interrupted when send out file " + bluetoothOppSendFileInfo.mFileName + " at " + j5 + " of " + bluetoothOppSendFileInfo.mLength);
                                    i3 = 490;
                                    outputStreamOpenOutputStream = outputStream;
                                    z22 = true;
                                } catch (IOException e73) {
                                    e = e73;
                                    inputStreamOpenInputStream = inputStream;
                                    outputStream3 = outputStream;
                                    z3 = true;
                                    handleSendException(e.toString());
                                    e.printStackTrace();
                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                    ?? r452222222222222 = z3;
                                    if (z3) {
                                    }
                                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                    return i3;
                                } catch (IndexOutOfBoundsException e74) {
                                    e = e74;
                                    inputStreamOpenInputStream = inputStream;
                                    outputStream3 = outputStream;
                                    z2 = true;
                                    handleSendException(e.toString());
                                    e.printStackTrace();
                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                    ?? r46222222222222222 = z2;
                                    if (z2) {
                                    }
                                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                    return i3;
                                } catch (NullPointerException e75) {
                                    e = e75;
                                    inputStreamOpenInputStream = inputStream;
                                    outputStream3 = outputStream;
                                    z = true;
                                    handleSendException(e.toString());
                                    e.printStackTrace();
                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                    ?? r47222222222222222 = z;
                                    if (z) {
                                    }
                                    BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                                    return i3;
                                } catch (Throwable th30) {
                                    th = th30;
                                    i = 490;
                                    inputStreamOpenInputStream = inputStream;
                                    outputStream3 = outputStream;
                                    r212 = 1;
                                    BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
                                    if (r212 == 0) {
                                    }
                                    if (inputStreamOpenInputStream != null) {
                                    }
                                    if (clientOperation != null) {
                                    }
                                    Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i);
                                    throw th;
                                }
                            } else {
                                Log.i(BluetoothOppObexClientSession.TAG, "SendFile finished send out file " + bluetoothOppSendFileInfo.mFileName + " length " + bluetoothOppSendFileInfo.mLength);
                                i3 = i6;
                                outputStreamOpenOutputStream = outputStream;
                                z22 = z20;
                            }
                        } catch (IOException e76) {
                            e = e76;
                            z14 = z7 ? 1 : 0;
                        } catch (IndexOutOfBoundsException e77) {
                            e = e77;
                            z13 = z7 ? 1 : 0;
                        } catch (NullPointerException e78) {
                            e = e78;
                            z12 = z7 ? 1 : 0;
                        } catch (Throwable th31) {
                            z18 = z7 ? 1 : 0;
                            th = th31;
                        }
                    }
                } catch (IOException e79) {
                    e = e79;
                    z14 = z7 ? 1 : 0;
                } catch (IndexOutOfBoundsException e80) {
                    e = e80;
                    z13 = z7 ? 1 : 0;
                } catch (NullPointerException e81) {
                    e = e81;
                    z12 = z7 ? 1 : 0;
                } catch (Throwable th32) {
                    i4 = i8;
                    z11 = z7 ? 1 : 0;
                    th = th32;
                    outputStream3 = outputStreamOpenOutputStream;
                }
                BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
                return i3;
            }
            z22 = z7 ? 1 : 0;
            inputStream = inputStreamOpenInputStream;
            i3 = i8;
            BluetoothOppUtility.closeSendFileInfo(this.mInfo.mUri);
            if (!z22) {
                if (outputStreamOpenOutputStream != null) {
                    outputStreamOpenOutputStream.close();
                }
                int responseCode7 = clientOperation.getResponseCode();
                if (responseCode7 != -1) {
                    if (BluetoothOppObexClientSession.V) {
                        Log.v(BluetoothOppObexClientSession.TAG, "Get response code " + responseCode7);
                    }
                    if (responseCode7 != 160) {
                        Log.i(BluetoothOppObexClientSession.TAG, "Response error code is " + responseCode7);
                        i3 = responseCode7 == 207 ? BluetoothShare.STATUS_NOT_ACCEPTABLE : BluetoothShare.STATUS_UNHANDLED_OBEX_CODE;
                        if (responseCode7 == 195 || responseCode7 == 198) {
                            i3 = BluetoothShare.STATUS_FORBIDDEN;
                        }
                    }
                } else {
                    i3 = BluetoothShare.STATUS_CONNECTION_ERROR;
                }
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (clientOperation != null) {
                clientOperation.close();
            }
            Constants.updateShareStatus(this.mContext1, this.mInfo.mId, i3);
            BluetoothOppUtility.cancelNotification(BluetoothOppObexClientSession.this.mContext);
            return i3;
        }

        private void handleSendException(String str) {
            Log.e(BluetoothOppObexClientSession.TAG, "Error when sending file: " + str);
            if (BluetoothOppObexClientSession.this.mCallback != null) {
                Message messageObtain = Message.obtain(BluetoothOppObexClientSession.this.mCallback);
                messageObtain.what = 5;
                if (str.contains("IOException")) {
                    messageObtain.obj = Integer.valueOf(BluetoothShare.STATUS_CANCELED);
                } else {
                    messageObtain.obj = Integer.valueOf(BluetoothShare.STATUS_CONNECTION_ERROR);
                }
                messageObtain.sendToTarget();
            }
            Constants.updateShareStatus(this.mContext1, this.mInfo.mId, BluetoothShare.STATUS_OBEX_DATA_ERROR);
            if (BluetoothOppObexClientSession.this.mCallback != null) {
                BluetoothOppObexClientSession.this.mCallback.removeMessages(4);
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            synchronized (this) {
                if (BluetoothOppObexClientSession.this.mWaitingForRemote) {
                    if (BluetoothOppObexClientSession.V) {
                        Log.v(BluetoothOppObexClientSession.TAG, "Interrupted when waitingForRemote");
                    }
                    try {
                        this.mTransport1.close();
                    } catch (IOException e) {
                        Log.e(BluetoothOppObexClientSession.TAG, "mTransport.close error");
                    }
                    if (BluetoothOppObexClientSession.this.mCallback != null) {
                        Message messageObtain = Message.obtain(BluetoothOppObexClientSession.this.mCallback);
                        messageObtain.what = 3;
                        if (this.mInfo != null) {
                            messageObtain.obj = this.mInfo;
                        }
                        messageObtain.sendToTarget();
                    }
                }
            }
        }
    }

    public static void applyRemoteDeviceQuirks(HeaderSet headerSet, String str, String str2) {
        if (str != null && str.startsWith("00:04:48")) {
            char[] charArray = str2.toCharArray();
            boolean z = true;
            boolean z2 = false;
            for (int length = charArray.length - 1; length >= 0; length--) {
                if (charArray[length] == '.') {
                    if (!z) {
                        charArray[length] = '_';
                        z2 = true;
                    }
                    z = false;
                }
            }
            if (z2) {
                String str3 = new String(charArray);
                headerSet.setHeader(1, str3);
                Log.i(TAG, "Sending file \"" + str2 + "\" as \"" + str3 + "\" to workaround Poloroid filename quirk");
            }
        }
    }

    @Override
    public void unblock() {
    }
}
