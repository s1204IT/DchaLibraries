package com.android.mms.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
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
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private final SparseArray<MmsNetworkManager> mNetworkManagerCache = new SparseArray<>();
    private IMms.Stub mStub = new IMms.Stub() {
        public void sendMessage(int subId, String callingPkg, Uri contentUri, String locationUrl, Bundle configOverrides, PendingIntent sentIntent) throws RemoteException {
            Log.d("MmsService", "sendMessage");
            MmsService.this.enforceSystemUid();
            SendRequest request = new SendRequest(MmsService.this, MmsService.this.checkSubId(subId), contentUri, locationUrl, sentIntent, callingPkg, configOverrides);
            String carrierMessagingServicePackage = MmsService.this.getCarrierMessagingServicePackageIfExists();
            if (carrierMessagingServicePackage != null) {
                Log.d("MmsService", "sending message by carrier app");
                request.trySendingByCarrierApp(MmsService.this, carrierMessagingServicePackage);
            } else {
                MmsService.this.addSimRequest(request);
            }
        }

        public void downloadMessage(int subId, String callingPkg, String locationUrl, Uri contentUri, Bundle configOverrides, PendingIntent downloadedIntent) throws RemoteException {
            Log.d("MmsService", "downloadMessage: " + locationUrl);
            MmsService.this.enforceSystemUid();
            DownloadRequest request = new DownloadRequest(MmsService.this, MmsService.this.checkSubId(subId), locationUrl, contentUri, downloadedIntent, callingPkg, configOverrides);
            String carrierMessagingServicePackage = MmsService.this.getCarrierMessagingServicePackageIfExists();
            if (carrierMessagingServicePackage != null) {
                Log.d("MmsService", "downloading message by carrier app");
                request.tryDownloadingByCarrierApp(MmsService.this, carrierMessagingServicePackage);
            } else {
                MmsService.this.addSimRequest(request);
            }
        }

        public Bundle getCarrierConfigValues(int subId) {
            Log.d("MmsService", "getCarrierConfigValues");
            MmsConfig mmsConfig = MmsConfigManager.getInstance().getMmsConfigBySubId(MmsService.this.checkSubId(subId));
            return mmsConfig == null ? new Bundle() : mmsConfig.getCarrierConfigValues();
        }

        public Uri importTextMessage(String callingPkg, String address, int type, String text, long timestampMillis, boolean seen, boolean read) {
            Log.d("MmsService", "importTextMessage");
            MmsService.this.enforceSystemUid();
            return MmsService.this.importSms(address, type, text, timestampMillis, seen, read, callingPkg);
        }

        public Uri importMultimediaMessage(String callingPkg, Uri contentUri, String messageId, long timestampSecs, boolean seen, boolean read) {
            Log.d("MmsService", "importMultimediaMessage");
            MmsService.this.enforceSystemUid();
            return MmsService.this.importMms(contentUri, messageId, timestampSecs, seen, read, callingPkg);
        }

        public boolean deleteStoredMessage(String callingPkg, Uri messageUri) throws RemoteException {
            Log.d("MmsService", "deleteStoredMessage " + messageUri);
            MmsService.this.enforceSystemUid();
            if (!MmsService.isSmsMmsContentUri(messageUri)) {
                Log.e("MmsService", "deleteStoredMessage: invalid message URI: " + messageUri.toString());
                return false;
            }
            long identity = Binder.clearCallingIdentity();
            try {
            } catch (SQLiteException e) {
                Log.e("MmsService", "deleteStoredMessage: failed to delete", e);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
            if (MmsService.this.getContentResolver().delete(messageUri, null, null) == 1) {
                return true;
            }
            Log.e("MmsService", "deleteStoredMessage: failed to delete");
            return false;
        }

        public boolean deleteStoredConversation(String callingPkg, long conversationId) throws RemoteException {
            Log.d("MmsService", "deleteStoredConversation " + conversationId);
            MmsService.this.enforceSystemUid();
            if (conversationId == -1) {
                Log.e("MmsService", "deleteStoredConversation: invalid thread id");
                return false;
            }
            Uri uri = ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, conversationId);
            long identity = Binder.clearCallingIdentity();
            try {
            } catch (SQLiteException e) {
                Log.e("MmsService", "deleteStoredConversation: failed to delete", e);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
            if (MmsService.this.getContentResolver().delete(uri, null, null) == 1) {
                return true;
            }
            Log.e("MmsService", "deleteStoredConversation: failed to delete");
            return false;
        }

        public boolean updateStoredMessageStatus(String callingPkg, Uri messageUri, ContentValues statusValues) throws RemoteException {
            Log.d("MmsService", "updateStoredMessageStatus " + messageUri);
            MmsService.this.enforceSystemUid();
            return MmsService.this.updateMessageStatus(messageUri, statusValues);
        }

        public boolean archiveStoredConversation(String callingPkg, long conversationId, boolean archived) throws RemoteException {
            Log.d("MmsService", "archiveStoredConversation " + conversationId + " " + archived);
            if (conversationId != -1) {
                return MmsService.this.archiveConversation(conversationId, archived);
            }
            Log.e("MmsService", "archiveStoredConversation: invalid thread id");
            return false;
        }

        public Uri addTextMessageDraft(String callingPkg, String address, String text) throws RemoteException {
            Log.d("MmsService", "addTextMessageDraft");
            MmsService.this.enforceSystemUid();
            return MmsService.this.addSmsDraft(address, text, callingPkg);
        }

        public Uri addMultimediaMessageDraft(String callingPkg, Uri contentUri) throws RemoteException {
            Log.d("MmsService", "addMultimediaMessageDraft");
            MmsService.this.enforceSystemUid();
            return MmsService.this.addMmsDraft(contentUri, callingPkg);
        }

        public void sendStoredMessage(int subId, String callingPkg, Uri messageUri, Bundle configOverrides, PendingIntent sentIntent) throws RemoteException {
            throw new UnsupportedOperationException();
        }

        public void setAutoPersisting(String callingPkg, boolean enabled) throws RemoteException {
            Log.d("MmsService", "setAutoPersisting " + enabled);
            MmsService.this.enforceSystemUid();
            SharedPreferences preferences = MmsService.this.getSharedPreferences("mmspref", 0);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("autopersisting", enabled);
            editor.apply();
        }

        public boolean getAutoPersisting() throws RemoteException {
            Log.d("MmsService", "getAutoPersisting");
            return MmsService.this.getAutoPersistingPref();
        }
    };
    private final RequestQueue[] mRunningRequestQueues = new RequestQueue[2];

    static int access$110(MmsService x0) {
        int i = x0.mRunningRequestCount;
        x0.mRunningRequestCount = i - 1;
        return i;
    }

    private class RequestQueue extends Handler {
        public RequestQueue(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            MmsRequest request = (MmsRequest) msg.obj;
            if (request != null) {
                try {
                    request.execute(MmsService.this, MmsService.this.getNetworkManager(request.getSubId()));
                    synchronized (MmsService.this) {
                        MmsService.access$110(MmsService.this);
                        if (MmsService.this.mRunningRequestCount <= 0) {
                            MmsService.this.movePendingSimRequestsToRunningSynchronized();
                        }
                    }
                    return;
                } catch (Throwable th) {
                    synchronized (MmsService.this) {
                        MmsService.access$110(MmsService.this);
                        if (MmsService.this.mRunningRequestCount <= 0) {
                            MmsService.this.movePendingSimRequestsToRunningSynchronized();
                        }
                        throw th;
                    }
                }
            }
            Log.e("MmsService", "RequestQueue: handling empty request");
        }
    }

    private MmsNetworkManager getNetworkManager(int subId) {
        MmsNetworkManager manager;
        synchronized (this.mNetworkManagerCache) {
            manager = this.mNetworkManagerCache.get(subId);
            if (manager == null) {
                manager = new MmsNetworkManager(this, subId);
                this.mNetworkManagerCache.put(subId, manager);
            }
        }
        return manager;
    }

    private void enforceSystemUid() {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only system can call this service");
        }
    }

    private int checkSubId(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            throw new RuntimeException("Invalid subId " + subId);
        }
        if (subId == Integer.MAX_VALUE) {
            return SubscriptionManager.getDefaultSmsSubId();
        }
        return subId;
    }

    private String getCarrierMessagingServicePackageIfExists() {
        Intent intent = new Intent("android.service.carrier.CarrierMessagingService");
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
        List<String> carrierPackages = telephonyManager.getCarrierPackageNamesForIntent(intent);
        if (carrierPackages == null || carrierPackages.size() != 1) {
            return null;
        }
        return carrierPackages.get(0);
    }

    private void startRequestQueueIfNeeded(int queueIndex) {
        if (queueIndex < 0 || queueIndex >= this.mRunningRequestQueues.length) {
            Log.e("MmsService", "Start request queue if needed: invalid queue " + queueIndex);
            return;
        }
        synchronized (this) {
            if (this.mRunningRequestQueues[queueIndex] == null) {
                HandlerThread thread = new HandlerThread("MmsService RequestQueue " + queueIndex);
                thread.start();
                this.mRunningRequestQueues[queueIndex] = new RequestQueue(thread.getLooper());
            }
        }
    }

    @Override
    public void addSimRequest(MmsRequest request) {
        if (request == null) {
            Log.e("MmsService", "Add running or pending: empty request");
            return;
        }
        Log.d("MmsService", "Current running=" + this.mRunningRequestCount + ", current subId=" + this.mCurrentSubId + ", pending=" + this.mPendingSimRequestQueue.size());
        synchronized (this) {
            if (this.mPendingSimRequestQueue.size() > 0 || (this.mRunningRequestCount > 0 && request.getSubId() != this.mCurrentSubId)) {
                Log.d("MmsService", "Add request to pending queue. Request subId=" + request.getSubId() + ", current subId=" + this.mCurrentSubId);
                this.mPendingSimRequestQueue.add(request);
                if (this.mRunningRequestCount <= 0) {
                    Log.e("MmsService", "Nothing's running but queue's not empty");
                    movePendingSimRequestsToRunningSynchronized();
                }
            } else {
                addToRunningRequestQueueSynchronized(request);
            }
        }
    }

    private void addToRunningRequestQueueSynchronized(MmsRequest request) {
        Log.d("MmsService", "Add request to running queue for subId " + request.getSubId());
        this.mCurrentSubId = request.getSubId();
        this.mRunningRequestCount++;
        int queue = request.getQueueType();
        startRequestQueueIfNeeded(queue);
        Message message = Message.obtain();
        message.obj = request;
        this.mRunningRequestQueues[queue].sendMessage(message);
    }

    private void movePendingSimRequestsToRunningSynchronized() {
        Log.d("MmsService", "Schedule requests pending on SIM");
        this.mCurrentSubId = -1;
        while (this.mPendingSimRequestQueue.size() > 0) {
            MmsRequest request = this.mPendingSimRequestQueue.peek();
            if (request != null) {
                if (!SubscriptionManager.isValidSubscriptionId(this.mCurrentSubId) || this.mCurrentSubId == request.getSubId()) {
                    this.mPendingSimRequestQueue.remove();
                    addToRunningRequestQueueSynchronized(request);
                } else {
                    return;
                }
            } else {
                Log.e("MmsService", "Schedule pending: found empty request");
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
        Log.d("MmsService", "onCreate");
        MmsConfigManager.getInstance().init(this);
        synchronized (this) {
            this.mCurrentSubId = -1;
            this.mRunningRequestCount = 0;
        }
    }

    private Uri importSms(String address, int type, String text, long timestampMillis, boolean seen, boolean read, String creator) {
        Uri insertUri = null;
        switch (type) {
            case 0:
                insertUri = Telephony.Sms.Inbox.CONTENT_URI;
                break;
            case 1:
                insertUri = Telephony.Sms.Sent.CONTENT_URI;
                break;
        }
        if (insertUri == null) {
            Log.e("MmsService", "importTextMessage: invalid message type for importing: " + type);
            return null;
        }
        ContentValues values = new ContentValues(6);
        values.put("address", address);
        values.put("date", Long.valueOf(timestampMillis));
        values.put("seen", Integer.valueOf(seen ? 1 : 0));
        values.put("read", Integer.valueOf(read ? 1 : 0));
        values.put("body", text);
        if (!TextUtils.isEmpty(creator)) {
            values.put("creator", creator);
        }
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                return getContentResolver().insert(insertUri, values);
            } catch (SQLiteException e) {
                Log.e("MmsService", "importTextMessage: failed to persist imported text message", e);
                Binder.restoreCallingIdentity(identity);
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private Uri importMms(Uri contentUri, String messageId, long timestampSecs, boolean seen, boolean read, String creator) {
        byte[] pduData = readPduFromContentUri(contentUri, 8388608);
        if (pduData == null || pduData.length < 1) {
            Log.e("MmsService", "importMessage: empty PDU");
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            GenericPdu pdu = parsePduForAnyCarrier(pduData);
            if (pdu == null) {
                Log.e("MmsService", "importMessage: can't parse input PDU");
                return null;
            }
            Uri insertUri = null;
            if (pdu instanceof SendReq) {
                insertUri = Telephony.Mms.Sent.CONTENT_URI;
            } else if ((pdu instanceof RetrieveConf) || (pdu instanceof NotificationInd) || (pdu instanceof DeliveryInd) || (pdu instanceof ReadOrigInd)) {
                insertUri = Telephony.Mms.Inbox.CONTENT_URI;
            }
            if (insertUri == null) {
                Log.e("MmsService", "importMessage; invalid MMS type: " + pdu.getClass().getCanonicalName());
                return null;
            }
            PduPersister persister = PduPersister.getPduPersister(this);
            Uri uri = persister.persist(pdu, insertUri, true, true, (HashMap) null);
            if (uri == null) {
                Log.e("MmsService", "importMessage: failed to persist message");
                return null;
            }
            ContentValues values = new ContentValues(5);
            if (!TextUtils.isEmpty(messageId)) {
                values.put("m_id", messageId);
            }
            if (timestampSecs != -1) {
                values.put("date", Long.valueOf(timestampSecs));
            }
            values.put("read", Integer.valueOf(seen ? 1 : 0));
            values.put("seen", Integer.valueOf(read ? 1 : 0));
            if (!TextUtils.isEmpty(creator)) {
                values.put("creator", creator);
            }
            if (SqliteWrapper.update(this, getContentResolver(), uri, values, (String) null, (String[]) null) != 1) {
                Log.e("MmsService", "importMessage: failed to update message");
            }
            return uri;
        } catch (MmsException e) {
            Log.e("MmsService", "importMessage: failed to persist message", e);
            return null;
        } catch (RuntimeException e2) {
            Log.e("MmsService", "importMessage: failed to parse input PDU", e2);
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static boolean isSmsMmsContentUri(Uri uri) {
        String uriString = uri.toString();
        return (uriString.startsWith("content://sms/") || uriString.startsWith("content://mms/")) && ContentUris.parseId(uri) != -1;
    }

    private boolean updateMessageStatus(Uri messageUri, ContentValues statusValues) {
        Integer val;
        boolean z = false;
        if (!isSmsMmsContentUri(messageUri)) {
            Log.e("MmsService", "updateMessageStatus: invalid messageUri: " + messageUri.toString());
        } else if (statusValues == null) {
            Log.w("MmsService", "updateMessageStatus: empty values to update");
        } else {
            ContentValues values = new ContentValues();
            if (statusValues.containsKey("read")) {
                Integer val2 = statusValues.getAsInteger("read");
                if (val2 != null) {
                    values.put("read", val2);
                }
            } else if (statusValues.containsKey("seen") && (val = statusValues.getAsInteger("seen")) != null) {
                values.put("seen", val);
            }
            if (values.size() < 1) {
                Log.w("MmsService", "updateMessageStatus: no value to update");
            } else {
                long identity = Binder.clearCallingIdentity();
                try {
                    if (getContentResolver().update(messageUri, values, null, null) != 1) {
                        Log.e("MmsService", "updateMessageStatus: failed to update database");
                    } else {
                        Binder.restoreCallingIdentity(identity);
                        z = true;
                    }
                } catch (SQLiteException e) {
                    Log.e("MmsService", "updateMessageStatus: failed to update database", e);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
        return z;
    }

    private boolean archiveConversation(long conversationId, boolean archived) {
        ContentValues values = new ContentValues(1);
        values.put("archived", Integer.valueOf(archived ? 1 : 0));
        long identity = Binder.clearCallingIdentity();
        try {
            if (getContentResolver().update(Telephony.Threads.CONTENT_URI, values, "_id=?", new String[]{Long.toString(conversationId)}) == 1) {
                return true;
            }
            Log.e("MmsService", "archiveConversation: failed to update database");
            return false;
        } catch (SQLiteException e) {
            Log.e("MmsService", "archiveConversation: failed to update database", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
        Binder.restoreCallingIdentity(identity);
    }

    private Uri addSmsDraft(String address, String text, String creator) {
        ContentValues values = new ContentValues(5);
        values.put("address", address);
        values.put("body", text);
        values.put("read", (Integer) 1);
        values.put("seen", (Integer) 1);
        if (!TextUtils.isEmpty(creator)) {
            values.put("creator", creator);
        }
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                return getContentResolver().insert(Telephony.Sms.Draft.CONTENT_URI, values);
            } catch (SQLiteException e) {
                Log.e("MmsService", "addSmsDraft: failed to store draft message", e);
                Binder.restoreCallingIdentity(identity);
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private Uri addMmsDraft(Uri contentUri, String creator) {
        byte[] pduData = readPduFromContentUri(contentUri, 8388608);
        if (pduData == null || pduData.length < 1) {
            Log.e("MmsService", "addMmsDraft: empty PDU");
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            GenericPdu pdu = parsePduForAnyCarrier(pduData);
            if (pdu == null) {
                Log.e("MmsService", "addMmsDraft: can't parse input PDU");
                return null;
            }
            if (!(pdu instanceof SendReq)) {
                Log.e("MmsService", "addMmsDraft; invalid MMS type: " + pdu.getClass().getCanonicalName());
                return null;
            }
            PduPersister persister = PduPersister.getPduPersister(this);
            Uri uri = persister.persist(pdu, Telephony.Mms.Draft.CONTENT_URI, true, true, (HashMap) null);
            if (uri == null) {
                Log.e("MmsService", "addMmsDraft: failed to persist message");
                return null;
            }
            ContentValues values = new ContentValues(3);
            values.put("read", (Integer) 1);
            values.put("seen", (Integer) 1);
            if (!TextUtils.isEmpty(creator)) {
                values.put("creator", creator);
            }
            if (SqliteWrapper.update(this, getContentResolver(), uri, values, (String) null, (String[]) null) != 1) {
                Log.e("MmsService", "addMmsDraft: failed to update message");
            }
            return uri;
        } catch (MmsException e) {
            Log.e("MmsService", "addMmsDraft: failed to persist message", e);
            return null;
        } catch (RuntimeException e2) {
            Log.e("MmsService", "addMmsDraft: failed to parse input PDU", e2);
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static GenericPdu parsePduForAnyCarrier(byte[] data) {
        GenericPdu pdu = null;
        try {
            pdu = new PduParser(data, true).parse();
        } catch (RuntimeException e) {
            Log.d("MmsService", "parsePduForAnyCarrier: Failed to parse PDU with content disposition", e);
        }
        if (pdu == null) {
            try {
                GenericPdu pdu2 = new PduParser(data, false).parse();
                return pdu2;
            } catch (RuntimeException e2) {
                Log.d("MmsService", "parsePduForAnyCarrier: Failed to parse PDU without content disposition", e2);
                return pdu;
            }
        }
        return pdu;
    }

    @Override
    public boolean getAutoPersistingPref() {
        SharedPreferences preferences = getSharedPreferences("mmspref", 0);
        return preferences.getBoolean("autopersisting", false);
    }

    @Override
    public byte[] readPduFromContentUri(final Uri contentUri, final int maxSize) {
        Callable<byte[]> copyPduToArray = new Callable<byte[]>() {
            @Override
            public byte[] call() throws Throwable {
                ParcelFileDescriptor.AutoCloseInputStream inStream;
                byte[] bArrCopyOf = null;
                ParcelFileDescriptor.AutoCloseInputStream inStream2 = null;
                try {
                    try {
                        ContentResolver cr = MmsService.this.getContentResolver();
                        ParcelFileDescriptor pduFd = cr.openFileDescriptor(contentUri, "r");
                        inStream = new ParcelFileDescriptor.AutoCloseInputStream(pduFd);
                    } catch (Throwable th) {
                        th = th;
                        if (inStream2 != null) {
                            try {
                                inStream2.close();
                            } catch (IOException e) {
                            }
                        }
                        throw th;
                    }
                } catch (IOException e2) {
                    ex = e2;
                    Log.e("MmsService", "MmsService.readPduFromContentUri: IO exception reading PDU", ex);
                    if (inStream2 != null) {
                        try {
                            inStream2.close();
                        } catch (IOException e3) {
                        }
                    }
                    return bArrCopyOf;
                }
                try {
                    byte[] tempBody = new byte[maxSize + 1];
                    int bytesRead = inStream.read(tempBody, 0, maxSize + 1);
                    if (bytesRead == 0) {
                        Log.e("MmsService", "MmsService.readPduFromContentUri: empty PDU");
                        if (inStream != null) {
                            try {
                                inStream.close();
                            } catch (IOException e4) {
                            }
                        }
                        inStream2 = inStream;
                    } else if (bytesRead <= maxSize) {
                        bArrCopyOf = Arrays.copyOf(tempBody, bytesRead);
                        if (inStream != null) {
                            try {
                                inStream.close();
                            } catch (IOException e5) {
                            }
                        }
                        inStream2 = inStream;
                    } else {
                        Log.e("MmsService", "MmsService.readPduFromContentUri: PDU too large");
                        if (inStream != null) {
                            try {
                                inStream.close();
                            } catch (IOException e6) {
                            }
                        }
                        inStream2 = inStream;
                    }
                } catch (IOException e7) {
                    ex = e7;
                    inStream2 = inStream;
                    Log.e("MmsService", "MmsService.readPduFromContentUri: IO exception reading PDU", ex);
                    if (inStream2 != null) {
                    }
                } catch (Throwable th2) {
                    th = th2;
                    inStream2 = inStream;
                    if (inStream2 != null) {
                    }
                    throw th;
                }
                return bArrCopyOf;
            }
        };
        Future<byte[]> pendingResult = this.mExecutor.submit(copyPduToArray);
        try {
            return pendingResult.get(30000L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            pendingResult.cancel(true);
            return null;
        }
    }

    @Override
    public boolean writePduToContentUri(final Uri contentUri, final byte[] pdu) {
        Callable<Boolean> copyDownloadedPduToOutput = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Throwable {
                Boolean bool;
                ParcelFileDescriptor.AutoCloseOutputStream outStream;
                ParcelFileDescriptor.AutoCloseOutputStream outStream2 = null;
                try {
                    try {
                        ContentResolver cr = MmsService.this.getContentResolver();
                        ParcelFileDescriptor pduFd = cr.openFileDescriptor(contentUri, "w");
                        outStream = new ParcelFileDescriptor.AutoCloseOutputStream(pduFd);
                    } catch (IOException e) {
                    }
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    outStream.write(pdu);
                    bool = Boolean.TRUE;
                    if (outStream != null) {
                        try {
                            outStream.close();
                        } catch (IOException e2) {
                        }
                    }
                } catch (IOException e3) {
                    outStream2 = outStream;
                    bool = Boolean.FALSE;
                    if (outStream2 != null) {
                        try {
                            outStream2.close();
                        } catch (IOException e4) {
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    outStream2 = outStream;
                    if (outStream2 != null) {
                        try {
                            outStream2.close();
                        } catch (IOException e5) {
                        }
                    }
                    throw th;
                }
                return bool;
            }
        };
        Future<Boolean> pendingResult = this.mExecutor.submit(copyDownloadedPduToOutput);
        try {
            Boolean succeeded = pendingResult.get(30000L, TimeUnit.MILLISECONDS);
            return succeeded == Boolean.TRUE;
        } catch (Exception e) {
            pendingResult.cancel(true);
            return false;
        }
    }
}
