package com.android.mms.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.internal.telephony.IMms;
import com.android.mms.service.MmsRequest;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.DeliveryInd;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.ReadOrigInd;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.util.SqliteWrapper;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MmsService extends Service implements MmsRequest.RequestManager {
    private int mCurrentSubId;
    private int mRunningRequestCount;
    private final Queue<MmsRequest> mPendingSimRequestQueue = new ArrayDeque();
    private final ExecutorService mPduTransferExecutor = Executors.newCachedThreadPool();
    private final SparseArray<MmsNetworkManager> mNetworkManagerCache = new SparseArray<>();
    private final ExecutorService[] mRunningRequestExecutors = new ExecutorService[2];
    private IMms.Stub mStub = new IMms.Stub() {
        public void sendMessage(int i, String str, Uri uri, String str2, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
            LogUtil.d("sendMessage: " + uri);
            MmsService.this.enforceSystemUid();
            int iCheckSubId = MmsService.this.checkSubId(i);
            if (!isActiveSubId(iCheckSubId)) {
                sendErrorInPendingIntent(pendingIntent);
                return;
            }
            SendRequest sendRequest = new SendRequest(MmsService.this, iCheckSubId, uri, str2, pendingIntent, str, bundle, MmsService.this);
            String carrierMessagingServicePackageIfExists = MmsService.this.getCarrierMessagingServicePackageIfExists();
            if (carrierMessagingServicePackageIfExists != null) {
                LogUtil.d(sendRequest.toString(), "sending message by carrier app");
                sendRequest.trySendingByCarrierApp(MmsService.this, carrierMessagingServicePackageIfExists);
            } else {
                MmsService.this.addSimRequest(sendRequest);
            }
        }

        public void downloadMessage(int i, String str, String str2, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
            LogUtil.d("downloadMessage: " + MmsHttpClient.redactUrlForNonVerbose(str2));
            MmsService.this.enforceSystemUid();
            DownloadRequest downloadRequest = new DownloadRequest(MmsService.this, MmsService.this.checkSubId(i), str2, uri, pendingIntent, str, bundle, MmsService.this);
            String carrierMessagingServicePackageIfExists = MmsService.this.getCarrierMessagingServicePackageIfExists();
            if (carrierMessagingServicePackageIfExists != null) {
                LogUtil.d(downloadRequest.toString(), "downloading message by carrier app");
                downloadRequest.tryDownloadingByCarrierApp(MmsService.this, carrierMessagingServicePackageIfExists);
            } else {
                MmsService.this.addSimRequest(downloadRequest);
            }
        }

        public Bundle getCarrierConfigValues(int i) {
            LogUtil.d("getCarrierConfigValues");
            Bundle mmsConfigBySubId = MmsConfigManager.getInstance().getMmsConfigBySubId(MmsService.this.checkSubId(i));
            if (mmsConfigBySubId == null) {
                return new Bundle();
            }
            return mmsConfigBySubId;
        }

        public Uri importTextMessage(String str, String str2, int i, String str3, long j, boolean z, boolean z2) {
            LogUtil.d("importTextMessage");
            MmsService.this.enforceSystemUid();
            return MmsService.this.importSms(str2, i, str3, j, z, z2, str);
        }

        public Uri importMultimediaMessage(String str, Uri uri, String str2, long j, boolean z, boolean z2) {
            LogUtil.d("importMultimediaMessage");
            MmsService.this.enforceSystemUid();
            return MmsService.this.importMms(uri, str2, j, z, z2, str);
        }

        public boolean deleteStoredMessage(String str, Uri uri) throws RemoteException {
            LogUtil.d("deleteStoredMessage " + uri);
            MmsService.this.enforceSystemUid();
            if (!MmsService.isSmsMmsContentUri(uri)) {
                LogUtil.e("deleteStoredMessage: invalid message URI: " + uri.toString());
                return false;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    if (MmsService.this.getContentResolver().delete(uri, null, null) != 1) {
                        LogUtil.e("deleteStoredMessage: failed to delete");
                        return false;
                    }
                } catch (SQLiteException e) {
                    LogUtil.e("deleteStoredMessage: failed to delete", e);
                }
                return true;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean deleteStoredConversation(String str, long j) throws RemoteException {
            LogUtil.d("deleteStoredConversation " + j);
            MmsService.this.enforceSystemUid();
            if (j == -1) {
                LogUtil.e("deleteStoredConversation: invalid thread id");
                return false;
            }
            Uri uriWithAppendedId = ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, j);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    if (MmsService.this.getContentResolver().delete(uriWithAppendedId, null, null) != 1) {
                        LogUtil.e("deleteStoredConversation: failed to delete");
                        return false;
                    }
                } catch (SQLiteException e) {
                    LogUtil.e("deleteStoredConversation: failed to delete", e);
                }
                return true;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean updateStoredMessageStatus(String str, Uri uri, ContentValues contentValues) throws RemoteException {
            LogUtil.d("updateStoredMessageStatus " + uri);
            MmsService.this.enforceSystemUid();
            return MmsService.this.updateMessageStatus(uri, contentValues);
        }

        public boolean archiveStoredConversation(String str, long j, boolean z) throws RemoteException {
            LogUtil.d("archiveStoredConversation " + j + " " + z);
            if (j != -1) {
                return MmsService.this.archiveConversation(j, z);
            }
            LogUtil.e("archiveStoredConversation: invalid thread id");
            return false;
        }

        public Uri addTextMessageDraft(String str, String str2, String str3) throws RemoteException {
            LogUtil.d("addTextMessageDraft");
            MmsService.this.enforceSystemUid();
            return MmsService.this.addSmsDraft(str2, str3, str);
        }

        public Uri addMultimediaMessageDraft(String str, Uri uri) throws RemoteException {
            LogUtil.d("addMultimediaMessageDraft");
            MmsService.this.enforceSystemUid();
            return MmsService.this.addMmsDraft(uri, str);
        }

        public void sendStoredMessage(int i, String str, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
            throw new UnsupportedOperationException();
        }

        public void setAutoPersisting(String str, boolean z) throws RemoteException {
            LogUtil.d("setAutoPersisting " + z);
            MmsService.this.enforceSystemUid();
            SharedPreferences.Editor editorEdit = MmsService.this.getSharedPreferences("mmspref", 0).edit();
            editorEdit.putBoolean("autopersisting", z);
            editorEdit.apply();
        }

        public boolean getAutoPersisting() throws RemoteException {
            return MmsService.this.getAutoPersistingPref();
        }

        private boolean isActiveSubId(int i) {
            return SubscriptionManager.from(MmsService.this).isActiveSubId(i);
        }

        private void sendErrorInPendingIntent(PendingIntent pendingIntent) {
            if (pendingIntent != null) {
                try {
                    pendingIntent.send(8);
                } catch (PendingIntent.CanceledException e) {
                }
            }
        }
    };

    static int access$1110(MmsService mmsService) {
        int i = mmsService.mRunningRequestCount;
        mmsService.mRunningRequestCount = i - 1;
        return i;
    }

    private MmsNetworkManager getNetworkManager(int i) {
        MmsNetworkManager mmsNetworkManager;
        synchronized (this.mNetworkManagerCache) {
            mmsNetworkManager = this.mNetworkManagerCache.get(i);
            if (mmsNetworkManager == null) {
                mmsNetworkManager = new MmsNetworkManager(this, i);
                this.mNetworkManagerCache.put(i, mmsNetworkManager);
            }
        }
        return mmsNetworkManager;
    }

    private void enforceSystemUid() {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only system can call this service");
        }
    }

    private int checkSubId(int i) {
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            throw new RuntimeException("Invalid subId " + i);
        }
        if (i == Integer.MAX_VALUE) {
            return SubscriptionManager.getDefaultSmsSubscriptionId();
        }
        return i;
    }

    private String getCarrierMessagingServicePackageIfExists() {
        List carrierPackageNamesForIntent = ((TelephonyManager) getSystemService("phone")).getCarrierPackageNamesForIntent(new Intent("android.service.carrier.CarrierMessagingService"));
        if (carrierPackageNamesForIntent == null || carrierPackageNamesForIntent.size() != 1) {
            return null;
        }
        return (String) carrierPackageNamesForIntent.get(0);
    }

    @Override
    public void addSimRequest(MmsRequest mmsRequest) {
        if (mmsRequest == null) {
            LogUtil.e("Add running or pending: empty request");
            return;
        }
        LogUtil.d("Current running=" + this.mRunningRequestCount + ", current subId=" + this.mCurrentSubId + ", pending=" + this.mPendingSimRequestQueue.size());
        synchronized (this) {
            if (this.mPendingSimRequestQueue.size() > 0 || (this.mRunningRequestCount > 0 && mmsRequest.getSubId() != this.mCurrentSubId)) {
                LogUtil.d("Add request to pending queue. Request subId=" + mmsRequest.getSubId() + ", current subId=" + this.mCurrentSubId);
                this.mPendingSimRequestQueue.add(mmsRequest);
                if (this.mRunningRequestCount <= 0) {
                    LogUtil.e("Nothing's running but queue's not empty");
                    movePendingSimRequestsToRunningSynchronized();
                }
            } else {
                addToRunningRequestQueueSynchronized(mmsRequest);
            }
        }
    }

    private void addToRunningRequestQueueSynchronized(final MmsRequest mmsRequest) {
        LogUtil.d("Add request to running queue for subId " + mmsRequest.getSubId());
        int queueType = mmsRequest.getQueueType();
        if (queueType < 0 || queueType >= this.mRunningRequestExecutors.length) {
            LogUtil.e("Invalid request queue index for running request");
            return;
        }
        this.mRunningRequestCount++;
        this.mCurrentSubId = mmsRequest.getSubId();
        this.mRunningRequestExecutors[queueType].execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mmsRequest.execute(MmsService.this, MmsService.this.getNetworkManager(mmsRequest.getSubId()));
                    synchronized (MmsService.this) {
                        MmsService.access$1110(MmsService.this);
                        if (MmsService.this.mRunningRequestCount <= 0) {
                            MmsService.this.movePendingSimRequestsToRunningSynchronized();
                        }
                    }
                } catch (Throwable th) {
                    synchronized (MmsService.this) {
                        MmsService.access$1110(MmsService.this);
                        if (MmsService.this.mRunningRequestCount <= 0) {
                            MmsService.this.movePendingSimRequestsToRunningSynchronized();
                        }
                        throw th;
                    }
                }
            }
        });
    }

    private void movePendingSimRequestsToRunningSynchronized() {
        LogUtil.d("Schedule requests pending on SIM");
        this.mCurrentSubId = -1;
        while (this.mPendingSimRequestQueue.size() > 0) {
            MmsRequest mmsRequestPeek = this.mPendingSimRequestQueue.peek();
            if (mmsRequestPeek != null) {
                if (!SubscriptionManager.isValidSubscriptionId(this.mCurrentSubId) || this.mCurrentSubId == mmsRequestPeek.getSubId()) {
                    this.mPendingSimRequestQueue.remove();
                    addToRunningRequestQueueSynchronized(mmsRequestPeek);
                } else {
                    return;
                }
            } else {
                LogUtil.e("Schedule pending: found empty request");
                this.mPendingSimRequestQueue.remove();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mStub;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d("onCreate");
        MmsConfigManager.getInstance().init(this);
        for (int i = 0; i < this.mRunningRequestExecutors.length; i++) {
            this.mRunningRequestExecutors[i] = Executors.newFixedThreadPool(4);
        }
        synchronized (this) {
            this.mCurrentSubId = -1;
            this.mRunningRequestCount = 0;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d("onDestroy");
        for (ExecutorService executorService : this.mRunningRequestExecutors) {
            executorService.shutdown();
        }
    }

    private Uri importSms(String str, int i, String str2, long j, boolean z, boolean z2, String str3) {
        Uri uri;
        switch (i) {
            case 0:
                uri = Telephony.Sms.Inbox.CONTENT_URI;
                break;
            case 1:
                uri = Telephony.Sms.Sent.CONTENT_URI;
                break;
            default:
                uri = null;
                break;
        }
        if (uri == null) {
            LogUtil.e("importTextMessage: invalid message type for importing: " + i);
            return null;
        }
        ContentValues contentValues = new ContentValues(6);
        contentValues.put("address", str);
        contentValues.put("date", Long.valueOf(j));
        contentValues.put("seen", Integer.valueOf(z ? 1 : 0));
        contentValues.put("read", Integer.valueOf(z2 ? 1 : 0));
        contentValues.put("body", str2);
        if (!TextUtils.isEmpty(str3)) {
            contentValues.put("creator", str3);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return getContentResolver().insert(uri, contentValues);
        } catch (SQLiteException e) {
            LogUtil.e("importTextMessage: failed to persist imported text message", e);
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private Uri importMms(Uri uri, String str, long j, boolean z, boolean z2, String str2) {
        Uri uri2;
        Uri uri3;
        byte[] pduFromContentUri = readPduFromContentUri(uri, 8388608);
        if (pduFromContentUri == null || pduFromContentUri.length < 1) {
            LogUtil.e("importMessage: empty PDU");
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                GenericPdu pduForAnyCarrier = parsePduForAnyCarrier(pduFromContentUri);
                if (pduForAnyCarrier == null) {
                    LogUtil.e("importMessage: can't parse input PDU");
                    return null;
                }
                if (pduForAnyCarrier instanceof SendReq) {
                    uri2 = Telephony.Mms.Sent.CONTENT_URI;
                } else {
                    if (!(pduForAnyCarrier instanceof RetrieveConf) && !(pduForAnyCarrier instanceof NotificationInd) && !(pduForAnyCarrier instanceof DeliveryInd) && !(pduForAnyCarrier instanceof ReadOrigInd)) {
                        uri3 = null;
                        if (uri3 != null) {
                            LogUtil.e("importMessage; invalid MMS type: " + pduForAnyCarrier.getClass().getCanonicalName());
                            return null;
                        }
                        Uri uriPersist = PduPersister.getPduPersister(this).persist(pduForAnyCarrier, uri3, true, true, (HashMap) null);
                        if (uriPersist == null) {
                            LogUtil.e("importMessage: failed to persist message");
                            return null;
                        }
                        ContentValues contentValues = new ContentValues(5);
                        if (!TextUtils.isEmpty(str)) {
                            contentValues.put("m_id", str);
                        }
                        if (j != -1) {
                            contentValues.put("date", Long.valueOf(j));
                        }
                        contentValues.put("read", Integer.valueOf(z ? 1 : 0));
                        contentValues.put("seen", Integer.valueOf(z2 ? 1 : 0));
                        if (!TextUtils.isEmpty(str2)) {
                            contentValues.put("creator", str2);
                        }
                        if (SqliteWrapper.update(this, getContentResolver(), uriPersist, contentValues, (String) null, (String[]) null) != 1) {
                            LogUtil.e("importMessage: failed to update message");
                        }
                        return uriPersist;
                    }
                    uri2 = Telephony.Mms.Inbox.CONTENT_URI;
                }
                uri3 = uri2;
                if (uri3 != null) {
                }
            } catch (RuntimeException e) {
                LogUtil.e("importMessage: failed to parse input PDU", e);
                return null;
            } catch (MmsException e2) {
                LogUtil.e("importMessage: failed to persist message", (Throwable) e2);
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private static boolean isSmsMmsContentUri(Uri uri) {
        String string = uri.toString();
        return (string.startsWith("content://sms/") || string.startsWith("content://mms/")) && ContentUris.parseId(uri) != -1;
    }

    private boolean updateMessageStatus(Uri uri, ContentValues contentValues) {
        Integer asInteger;
        if (!isSmsMmsContentUri(uri)) {
            LogUtil.e("updateMessageStatus: invalid messageUri: " + uri.toString());
            return false;
        }
        if (contentValues == null) {
            LogUtil.w("updateMessageStatus: empty values to update");
            return false;
        }
        ContentValues contentValues2 = new ContentValues();
        if (contentValues.containsKey("read")) {
            Integer asInteger2 = contentValues.getAsInteger("read");
            if (asInteger2 != null) {
                contentValues2.put("read", asInteger2);
            }
        } else if (contentValues.containsKey("seen") && (asInteger = contentValues.getAsInteger("seen")) != null) {
            contentValues2.put("seen", asInteger);
        }
        if (contentValues2.size() < 1) {
            LogUtil.w("updateMessageStatus: no value to update");
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (getContentResolver().update(uri, contentValues2, null, null) == 1) {
                return true;
            }
            LogUtil.e("updateMessageStatus: failed to update database");
            return false;
        } catch (SQLiteException e) {
            LogUtil.e("updateMessageStatus: failed to update database", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean archiveConversation(long j, boolean z) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put("archived", Integer.valueOf(z ? 1 : 0));
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (getContentResolver().update(Telephony.Threads.CONTENT_URI, contentValues, "_id=?", new String[]{Long.toString(j)}) == 1) {
                return true;
            }
            LogUtil.e("archiveConversation: failed to update database");
            return false;
        } catch (SQLiteException e) {
            LogUtil.e("archiveConversation: failed to update database", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private Uri addSmsDraft(String str, String str2, String str3) {
        ContentValues contentValues = new ContentValues(5);
        contentValues.put("address", str);
        contentValues.put("body", str2);
        contentValues.put("read", (Integer) 1);
        contentValues.put("seen", (Integer) 1);
        if (!TextUtils.isEmpty(str3)) {
            contentValues.put("creator", str3);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                return getContentResolver().insert(Telephony.Sms.Draft.CONTENT_URI, contentValues);
            } catch (SQLiteException e) {
                LogUtil.e("addSmsDraft: failed to store draft message", e);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private Uri addMmsDraft(Uri uri, String str) {
        byte[] pduFromContentUri = readPduFromContentUri(uri, 8388608);
        if (pduFromContentUri == null || pduFromContentUri.length < 1) {
            LogUtil.e("addMmsDraft: empty PDU");
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                GenericPdu pduForAnyCarrier = parsePduForAnyCarrier(pduFromContentUri);
                if (pduForAnyCarrier == null) {
                    LogUtil.e("addMmsDraft: can't parse input PDU");
                    return null;
                }
                if (!(pduForAnyCarrier instanceof SendReq)) {
                    LogUtil.e("addMmsDraft; invalid MMS type: " + pduForAnyCarrier.getClass().getCanonicalName());
                    return null;
                }
                Uri uriPersist = PduPersister.getPduPersister(this).persist(pduForAnyCarrier, Telephony.Mms.Draft.CONTENT_URI, true, true, (HashMap) null);
                if (uriPersist == null) {
                    LogUtil.e("addMmsDraft: failed to persist message");
                    return null;
                }
                ContentValues contentValues = new ContentValues(3);
                contentValues.put("read", (Integer) 1);
                contentValues.put("seen", (Integer) 1);
                if (!TextUtils.isEmpty(str)) {
                    contentValues.put("creator", str);
                }
                if (SqliteWrapper.update(this, getContentResolver(), uriPersist, contentValues, (String) null, (String[]) null) != 1) {
                    LogUtil.e("addMmsDraft: failed to update message");
                }
                return uriPersist;
            } catch (MmsException e) {
                LogUtil.e("addMmsDraft: failed to persist message", (Throwable) e);
                return null;
            } catch (RuntimeException e2) {
                LogUtil.e("addMmsDraft: failed to parse input PDU", e2);
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private static GenericPdu parsePduForAnyCarrier(byte[] bArr) {
        GenericPdu genericPdu;
        try {
            genericPdu = new PduParser(bArr, true).parse();
        } catch (RuntimeException e) {
            LogUtil.w("parsePduForAnyCarrier: Failed to parse PDU with content disposition", e);
            genericPdu = null;
        }
        if (genericPdu == null) {
            try {
                return new PduParser(bArr, false).parse();
            } catch (RuntimeException e2) {
                LogUtil.w("parsePduForAnyCarrier: Failed to parse PDU without content disposition", e2);
            }
        }
        return genericPdu;
    }

    @Override
    public boolean getAutoPersistingPref() {
        return getSharedPreferences("mmspref", 0).getBoolean("autopersisting", false);
    }

    @Override
    public byte[] readPduFromContentUri(final Uri uri, final int i) {
        if (uri == null) {
            return null;
        }
        Future futureSubmit = this.mPduTransferExecutor.submit(new Callable<byte[]>() {
            @Override
            public byte[] call() throws Throwable {
                Throwable th;
                ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream;
                ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream2 = null;
                try {
                    try {
                        autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(MmsService.this.getContentResolver().openFileDescriptor(uri, "r"));
                        try {
                            byte[] bArr = new byte[i + 1];
                            int i2 = autoCloseInputStream.read(bArr, 0, i + 1);
                            if (i2 == 0) {
                                LogUtil.e("Read empty PDU");
                                try {
                                    autoCloseInputStream.close();
                                } catch (IOException e) {
                                }
                                return null;
                            }
                            if (i2 <= i) {
                                byte[] bArrCopyOf = Arrays.copyOf(bArr, i2);
                                try {
                                    autoCloseInputStream.close();
                                } catch (IOException e2) {
                                }
                                return bArrCopyOf;
                            }
                            LogUtil.e("PDU read is too large");
                            try {
                                autoCloseInputStream.close();
                            } catch (IOException e3) {
                            }
                            return null;
                        } catch (IOException e4) {
                            e = e4;
                            LogUtil.e("IO exception reading PDU", e);
                            if (autoCloseInputStream != null) {
                                try {
                                    autoCloseInputStream.close();
                                } catch (IOException e5) {
                                }
                            }
                            return null;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (0 != 0) {
                            try {
                                autoCloseInputStream2.close();
                            } catch (IOException e6) {
                            }
                        }
                        throw th;
                    }
                } catch (IOException e7) {
                    e = e7;
                    autoCloseInputStream = null;
                } catch (Throwable th3) {
                    th = th3;
                    if (0 != 0) {
                    }
                    throw th;
                }
            }
        });
        try {
            return (byte[]) futureSubmit.get(30000L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            futureSubmit.cancel(true);
            return null;
        }
    }

    @Override
    public boolean writePduToContentUri(final Uri uri, final byte[] bArr) {
        if (uri == null || bArr == null) {
            return false;
        }
        Future futureSubmit = this.mPduTransferExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Throwable {
                ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream;
                ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream2 = null;
                try {
                    try {
                        autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(MmsService.this.getContentResolver().openFileDescriptor(uri, "w"));
                    } catch (Throwable th) {
                        th = th;
                    }
                } catch (IOException e) {
                    e = e;
                }
                try {
                    autoCloseOutputStream.write(bArr);
                    Boolean bool = Boolean.TRUE;
                    try {
                        autoCloseOutputStream.close();
                    } catch (IOException e2) {
                    }
                    return bool;
                } catch (IOException e3) {
                    e = e3;
                    autoCloseOutputStream2 = autoCloseOutputStream;
                    LogUtil.e("IO exception writing PDU", e);
                    Boolean bool2 = Boolean.FALSE;
                    if (autoCloseOutputStream2 != null) {
                        try {
                            autoCloseOutputStream2.close();
                        } catch (IOException e4) {
                        }
                    }
                    return bool2;
                } catch (Throwable th2) {
                    th = th2;
                    autoCloseOutputStream2 = autoCloseOutputStream;
                    if (autoCloseOutputStream2 != null) {
                        try {
                            autoCloseOutputStream2.close();
                        } catch (IOException e5) {
                        }
                    }
                    throw th;
                }
            }
        });
        try {
            return ((Boolean) futureSubmit.get(30000L, TimeUnit.MILLISECONDS)).booleanValue();
        } catch (Exception e) {
            futureSubmit.cancel(true);
            return false;
        }
    }
}
