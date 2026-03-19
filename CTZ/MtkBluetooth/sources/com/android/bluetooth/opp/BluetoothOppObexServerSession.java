package com.android.bluetooth.opp;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.BluetoothObexTransport;
import com.android.bluetooth.btservice.MetricsLogger;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.obex.Authenticator;
import javax.obex.HeaderSet;
import javax.obex.ObexTransport;
import javax.obex.Operation;
import javax.obex.ServerRequestHandler;
import javax.obex.ServerSession;

public class BluetoothOppObexServerSession extends ServerRequestHandler implements BluetoothOppObexSession {
    private static final String TAG = "BtOppObexServer";
    private BluetoothOppService mBluetoothOppService;
    private Context mContext;
    private BluetoothOppReceiveFileInfo mFileInfo;
    private BluetoothOppShareInfo mInfo;
    private int mLocalShareInfoId;
    private int mNumFilesAttemptedToReceive;
    private PowerManager.WakeLock mPartialWakeLock;
    private ServerSession mSession;
    private long mTimestamp;
    private ObexTransport mTransport;
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;
    private Handler mCallback = null;
    private boolean mServerBlocking = true;
    private int mAccepted = 0;
    private boolean mInterrupted = false;
    boolean mTimeoutMsgSent = false;

    public BluetoothOppObexServerSession(Context context, ObexTransport obexTransport, BluetoothOppService bluetoothOppService) {
        this.mContext = context;
        this.mTransport = obexTransport;
        this.mBluetoothOppService = bluetoothOppService;
        this.mPartialWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, TAG);
        this.mPartialWakeLock.setReferenceCounted(false);
    }

    @Override
    public void unblock() {
        this.mServerBlocking = false;
    }

    public void preStart() {
        try {
            if (D) {
                Log.d(TAG, "Create ServerSession with transport " + this.mTransport.toString());
            }
            this.mSession = new ServerSession(this.mTransport, this, (Authenticator) null);
        } catch (IOException e) {
            Log.e(TAG, "Create server session error" + e);
        }
    }

    @Override
    public void start(Handler handler, int i) {
        if (D) {
            Log.d(TAG, "Start!");
        }
        this.mCallback = handler;
    }

    @Override
    public void stop() {
        if (D) {
            Log.d(TAG, "Stop!");
        }
        this.mInterrupted = true;
        if (this.mSession != null) {
            try {
                this.mSession.close();
                this.mTransport.close();
            } catch (IOException e) {
                Log.e(TAG, "close mTransport error" + e);
            }
        }
        this.mCallback = null;
        this.mSession = null;
    }

    @Override
    public void forceInterupt() {
        stop();
    }

    @Override
    public void addShare(BluetoothOppShareInfo bluetoothOppShareInfo) {
        if (D) {
            Log.d(TAG, "addShare for id " + bluetoothOppShareInfo.mId);
        }
        this.mInfo = bluetoothOppShareInfo;
        this.mFileInfo = processShareInfo();
    }

    public int onPut(Operation operation) {
        String remoteAddress;
        int iReceiveFile;
        int i;
        if (D) {
            Log.d(TAG, "onPut " + operation.toString());
        }
        if (this.mAccepted == 3) {
            return 195;
        }
        if (this.mTransport instanceof BluetoothObexTransport) {
            remoteAddress = ((BluetoothObexTransport) this.mTransport).getRemoteAddress();
        } else {
            remoteAddress = "FF:FF:FF:00:00:00";
        }
        boolean zIsWhitelisted = BluetoothOppManager.getInstance(this.mContext).isWhitelisted(remoteAddress);
        try {
            HeaderSet receivedHeader = operation.getReceivedHeader();
            if (V) {
                Constants.logHeader(receivedHeader);
            }
            String str = (String) receivedHeader.getHeader(1);
            Long l = (Long) receivedHeader.getHeader(195);
            String str2 = (String) receivedHeader.getHeader(66);
            if (l.longValue() == 0) {
                if (D) {
                    Log.w(TAG, "length is 0, reject the transfer");
                    return 203;
                }
                return 203;
            }
            if (str == null || str.isEmpty()) {
                if (D) {
                    Log.w(TAG, "name is null or empty, reject the transfer");
                }
                return BluetoothShare.STATUS_RUNNING;
            }
            int iLastIndexOf = str.lastIndexOf(".");
            if (iLastIndexOf < 0 && str2 == null) {
                if (D) {
                    Log.w(TAG, "There is no file extension or mime type, reject the transfer");
                }
                return BluetoothShare.STATUS_RUNNING;
            }
            String lowerCase = str.substring(iLastIndexOf + 1).toLowerCase();
            String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(lowerCase);
            if (V) {
                Log.v(TAG, "Mimetype guessed from extension " + lowerCase + " is " + mimeTypeFromExtension);
            }
            if (mimeTypeFromExtension == null) {
                if (str2 == null) {
                    if (D) {
                        Log.w(TAG, "Can't get mimetype, reject the transfer");
                    }
                    return 207;
                }
            } else {
                str2 = mimeTypeFromExtension;
            }
            String lowerCase2 = str2.toLowerCase();
            if (lowerCase2 == null || (!zIsWhitelisted && !Constants.mimeTypeMatches(lowerCase2, Constants.ACCEPTABLE_SHARE_INBOUND_TYPES))) {
                if (D) {
                    Log.w(TAG, "mimeType is null or in unacceptable list, reject the transfer");
                }
                return 207;
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put(BluetoothShare.FILENAME_HINT, str);
            contentValues.put(BluetoothShare.TOTAL_BYTES, l);
            contentValues.put(BluetoothShare.MIMETYPE, lowerCase2);
            contentValues.put(BluetoothShare.DESTINATION, remoteAddress);
            contentValues.put(BluetoothShare.DIRECTION, (Integer) 1);
            contentValues.put("timestamp", Long.valueOf(this.mTimestamp));
            if (!this.mServerBlocking && (this.mAccepted == 1 || this.mAccepted == 2)) {
                contentValues.put("confirm", (Integer) 2);
            }
            if (zIsWhitelisted) {
                contentValues.put("confirm", (Integer) 5);
            }
            Uri uriInsert = this.mContext.getContentResolver().insert(BluetoothShare.CONTENT_URI, contentValues);
            this.mLocalShareInfoId = Integer.parseInt(uriInsert.getPathSegments().get(1));
            if (V) {
                Log.v(TAG, "insert contentUri: " + uriInsert);
                Log.v(TAG, "mLocalShareInfoId = " + this.mLocalShareInfoId);
            }
            synchronized (this) {
                this.mPartialWakeLock.acquire();
                this.mServerBlocking = true;
                while (this.mServerBlocking) {
                    try {
                        wait(1000L);
                        if (this.mCallback != null && !this.mTimeoutMsgSent) {
                            this.mCallback.sendMessageDelayed(this.mCallback.obtainMessage(4), 50000L);
                            this.mTimeoutMsgSent = true;
                            if (V) {
                                Log.v(TAG, "MSG_CONNECT_TIMEOUT sent");
                            }
                        }
                    } catch (InterruptedException e) {
                        if (V) {
                            Log.v(TAG, "Interrupted in onPut blocking");
                        }
                    }
                }
            }
            if (D) {
                Log.d(TAG, "Server unblocked ");
            }
            synchronized (this) {
                if (this.mCallback != null && this.mTimeoutMsgSent) {
                    this.mCallback.removeMessages(4);
                }
            }
            if (this.mInfo.mId != this.mLocalShareInfoId) {
                Log.e(TAG, "Unexpected error!");
            }
            this.mAccepted = this.mInfo.mConfirm;
            if (V) {
                Log.v(TAG, "after confirm: userAccepted=" + this.mAccepted);
            }
            int i2 = 160;
            if (this.mAccepted == 1 || this.mAccepted == 2 || this.mAccepted == 5) {
                this.mNumFilesAttemptedToReceive++;
                if (this.mFileInfo.mFileName == null) {
                    iReceiveFile = this.mFileInfo.mStatus;
                    this.mInfo.mStatus = this.mFileInfo.mStatus;
                    Constants.updateShareStatus(this.mContext, this.mInfo.mId, iReceiveFile);
                    i2 = 208;
                } else {
                    iReceiveFile = 200;
                }
                if (this.mFileInfo.mFileName != null) {
                    ContentValues contentValues2 = new ContentValues();
                    Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + this.mInfo.mId);
                    contentValues2.put(BluetoothShare._DATA, this.mFileInfo.mFileName);
                    contentValues2.put("status", Integer.valueOf(BluetoothShare.STATUS_RUNNING));
                    this.mContext.getContentResolver().update(uri, contentValues2, null, null);
                    iReceiveFile = receiveFile(this.mFileInfo, operation);
                    i = iReceiveFile == 200 ? i2 : 208;
                    Constants.updateShareStatus(this.mContext, this.mInfo.mId, iReceiveFile);
                } else {
                    i = i2;
                }
                if (iReceiveFile == 200) {
                    Message messageObtain = Message.obtain(this.mCallback, 0);
                    messageObtain.obj = this.mInfo;
                    messageObtain.sendToTarget();
                    return i;
                }
                if (this.mCallback != null) {
                    Message messageObtain2 = Message.obtain(this.mCallback, 2);
                    this.mInfo.mStatus = iReceiveFile;
                    messageObtain2.obj = this.mInfo;
                    messageObtain2.sendToTarget();
                }
                try {
                    if (this.mFileInfo != null && this.mFileInfo.mFileName != null) {
                        if (V) {
                            Log.d(TAG, "Deleting not completed file");
                        }
                        new File(this.mFileInfo.mFileName).delete();
                        return i;
                    }
                    return i;
                } catch (Exception e2) {
                    if (V) {
                        e2.printStackTrace();
                        Log.e(TAG, "exception while deleting file");
                        return i;
                    }
                    return i;
                }
            }
            if (this.mAccepted != 3 && this.mAccepted != 4) {
                return 160;
            }
            Log.i(TAG, "Rejected incoming request");
            if (this.mFileInfo.mFileName != null) {
                try {
                    this.mFileInfo.mOutputStream.close();
                } catch (IOException e3) {
                    Log.e(TAG, "error close file stream");
                }
                new File(this.mFileInfo.mFileName).delete();
            }
            Constants.updateShareStatus(this.mContext, this.mInfo.mId, BluetoothShare.STATUS_CANCELED);
            Message messageObtain3 = Message.obtain(this.mCallback);
            messageObtain3.what = 3;
            this.mInfo.mStatus = BluetoothShare.STATUS_CANCELED;
            messageObtain3.obj = this.mInfo;
            messageObtain3.sendToTarget();
            return 195;
        } catch (IOException e4) {
            Log.e(TAG, "onPut: getReceivedHeaders error " + e4);
            return BluetoothShare.STATUS_RUNNING;
        }
    }

    private int receiveFile(BluetoothOppReceiveFileInfo bluetoothOppReceiveFileInfo, Operation operation) {
        boolean z;
        InputStream inputStreamOpenInputStream;
        int i;
        BufferedOutputStream bufferedOutputStream;
        int i2;
        int i3;
        byte[] bArr;
        InputStream inputStream;
        BluetoothOppReceiveFileInfo bluetoothOppReceiveFileInfo2 = bluetoothOppReceiveFileInfo;
        int i4 = 0;
        int i5 = -1;
        try {
            inputStreamOpenInputStream = operation.openInputStream();
            z = false;
            i = -1;
        } catch (IOException e) {
            Log.e(TAG, "Error when openInputStream");
            z = true;
            inputStreamOpenInputStream = null;
            i = BluetoothShare.STATUS_OBEX_DATA_ERROR;
        }
        Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + this.mInfo.mId);
        if (!z) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(BluetoothShare._DATA, bluetoothOppReceiveFileInfo2.mFileName);
            this.mContext.getContentResolver().update(uri, contentValues, null, null);
        }
        if (!z) {
            bufferedOutputStream = new BufferedOutputStream(bluetoothOppReceiveFileInfo2.mOutputStream, 65536);
        } else {
            bufferedOutputStream = null;
        }
        long j = 0;
        if (!z) {
            byte[] bArr2 = new byte[operation.getMaxPacketSize()];
            long j2 = 0;
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            long jElapsedRealtime2 = 0;
            while (true) {
                try {
                } catch (IOException e2) {
                    Log.e(TAG, "Error when receiving file: " + e2);
                    i3 = "Abort Received".equals(e2.getMessage()) ? BluetoothShare.STATUS_CANCELED : BluetoothShare.STATUS_OBEX_DATA_ERROR;
                }
                if (this.mInterrupted) {
                    i2 = i;
                    break;
                }
                i2 = i;
                if (j == bluetoothOppReceiveFileInfo2.mLength) {
                    break;
                }
                if (V) {
                    jElapsedRealtime2 = SystemClock.elapsedRealtime();
                }
                int i6 = inputStreamOpenInputStream.read(bArr2);
                if (i6 == i5) {
                    break;
                }
                bufferedOutputStream.write(bArr2, i4, i6);
                j += (long) i6;
                long j3 = (100 * j) / bluetoothOppReceiveFileInfo2.mLength;
                long jElapsedRealtime3 = SystemClock.elapsedRealtime();
                if (V) {
                    bArr = bArr2;
                    StringBuilder sb = new StringBuilder();
                    inputStream = inputStreamOpenInputStream;
                    sb.append("Receive file position = ");
                    sb.append(j);
                    sb.append(" readLength ");
                    sb.append(i6);
                    sb.append(" bytes took ");
                    sb.append(jElapsedRealtime3 - jElapsedRealtime2);
                    sb.append(" ms");
                    Log.v(TAG, sb.toString());
                } else {
                    bArr = bArr2;
                    inputStream = inputStreamOpenInputStream;
                }
                if (j3 <= j2 && jElapsedRealtime3 - jElapsedRealtime <= 10000) {
                    i = i2;
                    bArr2 = bArr;
                    inputStreamOpenInputStream = inputStream;
                    bluetoothOppReceiveFileInfo2 = bluetoothOppReceiveFileInfo;
                    i4 = 0;
                    i5 = -1;
                } else {
                    ContentValues contentValues2 = new ContentValues();
                    contentValues2.put(BluetoothShare.CURRENT_BYTES, Long.valueOf(j));
                    this.mContext.getContentResolver().update(uri, contentValues2, null, null);
                    j2 = j3;
                    jElapsedRealtime = jElapsedRealtime3;
                    i = i2;
                    bArr2 = bArr;
                    inputStreamOpenInputStream = inputStream;
                    bluetoothOppReceiveFileInfo2 = bluetoothOppReceiveFileInfo;
                    i4 = 0;
                    i5 = -1;
                }
                if (!this.mInterrupted) {
                    if (D) {
                        Log.d(TAG, "receiving file interrupted by user.");
                    }
                    i3 = BluetoothShare.STATUS_CANCELED;
                } else if (j == bluetoothOppReceiveFileInfo.mLength) {
                    if (D) {
                        Log.d(TAG, "Receiving file completed for " + bluetoothOppReceiveFileInfo.mFileName);
                    }
                    i3 = 200;
                } else {
                    if (D) {
                        Log.d(TAG, "Reading file failed at " + j + " of " + bluetoothOppReceiveFileInfo.mLength);
                    }
                    if (i3 == -1) {
                        i3 = BluetoothShare.STATUS_UNKNOWN_ERROR;
                    }
                }
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "Error when closing stream after send");
                    }
                }
                BluetoothOppUtility.cancelNotification(this.mContext);
                return i3;
            }
        }
        i2 = i;
        i3 = i2;
        if (!this.mInterrupted) {
        }
        if (bufferedOutputStream != null) {
        }
        BluetoothOppUtility.cancelNotification(this.mContext);
        return i3;
    }

    private BluetoothOppReceiveFileInfo processShareInfo() {
        if (D) {
            Log.d(TAG, "processShareInfo() " + this.mInfo.mId);
        }
        BluetoothOppReceiveFileInfo bluetoothOppReceiveFileInfoGenerateFileInfo = BluetoothOppReceiveFileInfo.generateFileInfo(this.mContext, this.mInfo.mId);
        if (V) {
            Log.v(TAG, "Generate BluetoothOppReceiveFileInfo:");
            Log.v(TAG, "filename  :" + bluetoothOppReceiveFileInfoGenerateFileInfo.mFileName);
            Log.v(TAG, "length    :" + bluetoothOppReceiveFileInfoGenerateFileInfo.mLength);
            Log.v(TAG, "status    :" + bluetoothOppReceiveFileInfoGenerateFileInfo.mStatus);
        }
        return bluetoothOppReceiveFileInfoGenerateFileInfo;
    }

    public int onConnect(HeaderSet headerSet, HeaderSet headerSet2) {
        String remoteAddress;
        if (D) {
            Log.d(TAG, "onConnect");
        }
        if (V) {
            Constants.logHeader(headerSet);
        }
        try {
            byte[] bArr = (byte[]) headerSet.getHeader(70);
            if (V) {
                Log.v(TAG, "onConnect(): uuid =" + Arrays.toString(bArr));
            }
            if (bArr != null) {
                return 198;
            }
            Long l = (Long) headerSet.getHeader(BluetoothShare.STATUS_RUNNING);
            if (this.mTransport instanceof BluetoothObexTransport) {
                remoteAddress = ((BluetoothObexTransport) this.mTransport).getRemoteAddress();
            } else {
                remoteAddress = "FF:FF:FF:00:00:00";
            }
            if (BluetoothOppManager.getInstance(this.mContext).isWhitelisted(remoteAddress)) {
                Intent intent = new Intent("android.nfc.handover.intent.action.HANDOVER_STARTED");
                if (l != null) {
                    intent.putExtra("android.nfc.handover.intent.extra.OBJECT_COUNT", l.intValue());
                } else {
                    intent.putExtra("android.nfc.handover.intent.extra.OBJECT_COUNT", -1);
                }
                intent.putExtra("android.nfc.handover.intent.extra.ADDRESS", remoteAddress);
                this.mContext.sendBroadcast(intent, "android.permission.NFC_HANDOVER_STATUS");
            }
            this.mTimestamp = System.currentTimeMillis();
            this.mNumFilesAttemptedToReceive = 0;
            return 160;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return 208;
        }
    }

    public void onDisconnect(HeaderSet headerSet, HeaderSet headerSet2) {
        if (D) {
            Log.d(TAG, "onDisconnect");
        }
        if (this.mNumFilesAttemptedToReceive > 0) {
            MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.OPP);
        }
        headerSet2.responseCode = 160;
    }

    private synchronized void releaseWakeLocks() {
        if (this.mPartialWakeLock.isHeld()) {
            this.mPartialWakeLock.release();
        }
    }

    public void onClose() {
        if (D) {
            Log.d(TAG, "onClose");
        }
        releaseWakeLocks();
        this.mBluetoothOppService.acceptNewConnections();
        BluetoothOppUtility.cancelNotification(this.mContext);
        if (this.mCallback != null) {
            Message messageObtain = Message.obtain(this.mCallback);
            messageObtain.what = 1;
            messageObtain.obj = this.mInfo;
            messageObtain.sendToTarget();
        }
    }
}
