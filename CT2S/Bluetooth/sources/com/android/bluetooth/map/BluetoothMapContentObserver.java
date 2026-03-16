package com.android.bluetooth.map;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.Telephony;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.util.Xml;
import com.android.bluetooth.map.BluetoothMapUtils;
import com.android.bluetooth.map.BluetoothMapbMessage;
import com.android.bluetooth.map.BluetoothMapbMessageMms;
import com.android.bluetooth.opp.BluetoothShare;
import com.android.vcard.VCardBuilder;
import com.android.vcard.VCardConfig;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlSerializer;

public class BluetoothMapContentObserver {
    private static final String ACTION_MESSAGE_DELIVERY = "com.android.bluetooth.BluetoothMapContentObserver.action.MESSAGE_DELIVERY";
    public static final String ACTION_MESSAGE_SENT = "com.android.bluetooth.BluetoothMapContentObserver.action.MESSAGE_SENT";
    private static final int CONVERT_MMS_TO_SMS_PART_COUNT = 10;
    private static final boolean D = true;
    public static final int DELETED_THREAD_ID = -1;
    private static final String EVENT_TYPE_DELETE = "MessageDeleted";
    private static final String EVENT_TYPE_DELEVERY_SUCCESS = "DeliverySuccess";
    private static final String EVENT_TYPE_DELIVERY_FAILURE = "DeliveryFailure";
    private static final String EVENT_TYPE_NEW = "NewMessage";
    private static final String EVENT_TYPE_SENDING_FAILURE = "SendingFailure";
    private static final String EVENT_TYPE_SENDING_SUCCESS = "SendingSuccess";
    private static final String EVENT_TYPE_SHIFT = "MessageShift";
    public static final String EXTRA_MESSAGE_SENT_HANDLE = "HANDLE";
    public static final String EXTRA_MESSAGE_SENT_RESULT = "result";
    public static final String EXTRA_MESSAGE_SENT_RETRY = "retry";
    public static final String EXTRA_MESSAGE_SENT_TIMESTAMP = "timestamp";
    public static final String EXTRA_MESSAGE_SENT_TRANSPARENT = "transparent";
    public static final String EXTRA_MESSAGE_SENT_URI = "uri";
    public static final int MESSAGE_TYPE_RETRIEVE_CONF = 132;
    private static final long PROVIDER_ANR_TIMEOUT = 20000;
    private static final String TAG = "BluetoothMapContentObserver";
    private static final boolean V = false;
    private BluetoothMapEmailSettingsItem mAccount;
    private String mAuthority;
    private Context mContext;
    private boolean mEnableSmsMms;
    private int mMasId;
    private BluetoothMapMasInstance mMasInstance;
    private Uri mMessageUri;
    private BluetoothMnsObexClient mMnsClient;
    private ContentProviderClient mProviderClient;
    private ContentResolver mResolver;
    private BluetoothMapUtils.TYPE mSmsType;
    static final String[] SMS_PROJECTION = {"_id", "thread_id", "address", "body", "date", "read", "type", BluetoothShare.STATUS, "locked", "error_code"};
    static final String[] SMS_PROJECTION_SHORT = {"_id", "thread_id", "type"};
    static final String[] MMS_PROJECTION_SHORT = {"_id", "thread_id", "m_type", "msg_box"};
    static final String[] EMAIL_PROJECTION_SHORT = {"_id", "folder_id", "flag_read"};
    private static final String[] folderSms = {"", "inbox", "sent", "draft", "outbox", "outbox", "outbox", "inbox", "inbox"};
    private static final String[] folderMms = {"", "inbox", "sent", "draft", "outbox"};
    private boolean mObserverRegistered = false;
    private BluetoothMapFolderElement mFolders = new BluetoothMapFolderElement("DUMMY", null);
    private final ContentObserver mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            BluetoothMapContentObserver.this.handleMsgListChanges(uri);
        }
    };
    private Map<Long, Msg> mMsgListSms = new HashMap();
    private Map<Long, Msg> mMsgListMms = new HashMap();
    private Map<Long, Msg> mMsgListEmail = new HashMap();
    private Map<Long, PushMsgInfo> mPushMsgList = Collections.synchronizedMap(new HashMap());
    private SmsBroadcastReceiver mSmsBroadcastReceiver = new SmsBroadcastReceiver();
    private boolean mInitialized = false;
    private PhoneStateListener mPhoneListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.d(BluetoothMapContentObserver.TAG, "Phone service state change: " + serviceState.getState());
            if (serviceState.getState() == 0) {
                BluetoothMapContentObserver.this.resendPendingMessages();
            }
        }
    };

    private static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

    public BluetoothMapContentObserver(Context context, BluetoothMnsObexClient mnsClient, BluetoothMapMasInstance masInstance, BluetoothMapEmailSettingsItem account, boolean enableSmsMms) throws RemoteException {
        this.mProviderClient = null;
        this.mMasInstance = null;
        this.mEnableSmsMms = false;
        this.mAuthority = null;
        this.mMessageUri = null;
        this.mContext = context;
        this.mResolver = this.mContext.getContentResolver();
        this.mAccount = account;
        this.mMasInstance = masInstance;
        this.mMasId = this.mMasInstance.getMasId();
        if (account != null) {
            this.mAuthority = Uri.parse(account.mBase_uri).getAuthority();
            this.mMessageUri = Uri.parse(account.mBase_uri + "/Message");
            this.mProviderClient = this.mResolver.acquireUnstableContentProviderClient(this.mAuthority);
            if (this.mProviderClient == null) {
                throw new RemoteException("Failed to acquire provider for " + this.mAuthority);
            }
            this.mProviderClient.setDetectNotResponding(PROVIDER_ANR_TIMEOUT);
        }
        this.mEnableSmsMms = enableSmsMms;
        this.mSmsType = getSmsType();
        this.mMnsClient = mnsClient;
    }

    public void setFolderStructure(BluetoothMapFolderElement folderStructure) {
        this.mFolders = folderStructure;
    }

    private BluetoothMapUtils.TYPE getSmsType() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tm.getPhoneType() == 1) {
            BluetoothMapUtils.TYPE smsType = BluetoothMapUtils.TYPE.SMS_GSM;
            return smsType;
        }
        if (tm.getPhoneType() != 2) {
            return null;
        }
        BluetoothMapUtils.TYPE smsType2 = BluetoothMapUtils.TYPE.SMS_CDMA;
        return smsType2;
    }

    private class Event {
        static final String PATH = "telecom/msg/";
        String eventType;
        String folder;
        long handle;
        BluetoothMapUtils.TYPE msgType;
        String oldFolder;

        public Event(String eventType, long handle, String folder, String oldFolder, BluetoothMapUtils.TYPE msgType) {
            this.eventType = eventType;
            this.handle = handle;
            if (folder != null) {
                if (msgType == BluetoothMapUtils.TYPE.EMAIL) {
                    this.folder = folder;
                } else {
                    this.folder = PATH + folder;
                }
            } else {
                this.folder = null;
            }
            if (oldFolder != null) {
                if (msgType == BluetoothMapUtils.TYPE.EMAIL) {
                    this.oldFolder = oldFolder;
                } else {
                    this.oldFolder = PATH + oldFolder;
                }
            } else {
                this.oldFolder = null;
            }
            this.msgType = msgType;
        }

        public byte[] encode() throws UnsupportedEncodingException {
            StringWriter sw = new StringWriter();
            XmlSerializer xmlEvtReport = Xml.newSerializer();
            try {
                xmlEvtReport.setOutput(sw);
                xmlEvtReport.startDocument(null, null);
                xmlEvtReport.text(VCardBuilder.VCARD_END_OF_LINE);
                xmlEvtReport.startTag("", "MAP-event-report");
                xmlEvtReport.attribute("", "version", "1.0");
                xmlEvtReport.startTag("", "event");
                xmlEvtReport.attribute("", "type", this.eventType);
                xmlEvtReport.attribute("", "handle", BluetoothMapUtils.getMapHandle(this.handle, this.msgType));
                if (this.folder != null) {
                    xmlEvtReport.attribute("", "folder", this.folder);
                }
                if (this.oldFolder != null) {
                    xmlEvtReport.attribute("", "old_folder", this.oldFolder);
                }
                xmlEvtReport.attribute("", "msg_type", this.msgType.name());
                xmlEvtReport.endTag("", "event");
                xmlEvtReport.endTag("", "MAP-event-report");
                xmlEvtReport.endDocument();
            } catch (IOException e) {
                Log.w(BluetoothMapContentObserver.TAG, e);
            } catch (IllegalArgumentException e2) {
                Log.w(BluetoothMapContentObserver.TAG, e2);
            } catch (IllegalStateException e3) {
                Log.w(BluetoothMapContentObserver.TAG, e3);
            }
            return sw.toString().getBytes("UTF-8");
        }
    }

    private class Msg {
        long folderId;
        long id;
        boolean localInitiatedSend;
        long oldFolderId;
        int threadId;
        boolean transparent;
        int type;

        public Msg(long id, int type, int threadId) {
            this.folderId = -1L;
            this.oldFolderId = -1L;
            this.localInitiatedSend = false;
            this.transparent = false;
            this.id = id;
            this.type = type;
            this.threadId = threadId;
        }

        public Msg(long id, long folderId) {
            this.folderId = -1L;
            this.oldFolderId = -1L;
            this.localInitiatedSend = false;
            this.transparent = false;
            this.id = id;
            this.folderId = folderId;
        }

        public int hashCode() {
            int result = ((int) (this.id ^ (this.id >>> 32))) + 31;
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                Msg other = (Msg) obj;
                return this.id == other.id;
            }
            return false;
        }
    }

    public int setNotificationRegistration(int notificationStatus) throws RemoteException {
        Log.d(TAG, "setNotificationRegistration() enter");
        Handler mns = this.mMnsClient.getMessageHandler();
        if (mns != null) {
            Message msg = mns.obtainMessage();
            msg.what = 1;
            msg.arg1 = this.mMasId;
            msg.arg2 = notificationStatus;
            mns.sendMessageDelayed(msg, 10L);
            Log.d(TAG, "setNotificationRegistration() MSG_MNS_NOTIFICATION_REGISTRATION send to MNS");
            if (notificationStatus == 1) {
                registerObserver();
            } else {
                unregisterObserver();
            }
            return 160;
        }
        Log.d(TAG, "setNotificationRegistration() Unable to send registration request");
        return 211;
    }

    public void registerObserver() throws RemoteException {
        if (!this.mObserverRegistered) {
            if (this.mEnableSmsMms) {
                this.mResolver.registerContentObserver(Telephony.MmsSms.CONTENT_URI, false, this.mObserver);
                this.mObserverRegistered = true;
            }
            if (this.mAccount != null) {
                this.mProviderClient = this.mResolver.acquireUnstableContentProviderClient(this.mAuthority);
                if (this.mProviderClient == null) {
                    throw new RemoteException("Failed to acquire provider for " + this.mAuthority);
                }
                this.mProviderClient.setDetectNotResponding(PROVIDER_ANR_TIMEOUT);
                Uri uri = Uri.parse(this.mAccount.mBase_uri_no_account + "/Message");
                Log.d(TAG, "Registering observer for: " + uri);
                this.mResolver.registerContentObserver(uri, true, this.mObserver);
                Uri uri2 = Uri.parse(this.mAccount.mBase_uri + "/Message");
                Log.d(TAG, "Registering observer for: " + uri2);
                this.mResolver.registerContentObserver(uri2, true, this.mObserver);
                this.mObserverRegistered = true;
            }
            initMsgList();
        }
    }

    public void unregisterObserver() {
        this.mResolver.unregisterContentObserver(this.mObserver);
        this.mObserverRegistered = false;
        if (this.mProviderClient != null) {
            this.mProviderClient.release();
            this.mProviderClient = null;
        }
    }

    private void sendEvent(Event evt) {
        Log.d(TAG, "sendEvent: " + evt.eventType + " " + evt.handle + " " + evt.folder + " " + evt.oldFolder + " " + evt.msgType.name());
        if (this.mMnsClient == null || !this.mMnsClient.isConnected()) {
            Log.d(TAG, "sendEvent: No MNS client registered or connected- don't send event");
        } else {
            try {
                this.mMnsClient.sendEvent(evt.encode(), this.mMasId);
            } catch (UnsupportedEncodingException e) {
            }
        }
    }

    private void initMsgList() throws RemoteException {
        Cursor c;
        if (this.mEnableSmsMms) {
            HashMap<Long, Msg> msgListSms = new HashMap<>();
            c = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION_SHORT, null, null, null);
            while (c != null) {
                try {
                    if (!c.moveToNext()) {
                        break;
                    }
                    long id = c.getLong(c.getColumnIndex("_id"));
                    int type = c.getInt(c.getColumnIndex("type"));
                    int threadId = c.getInt(c.getColumnIndex("thread_id"));
                    Msg msg = new Msg(id, type, threadId);
                    msgListSms.put(Long.valueOf(id), msg);
                } finally {
                }
            }
            close(c);
            synchronized (this.mMsgListSms) {
                this.mMsgListSms.clear();
                this.mMsgListSms = msgListSms;
            }
            HashMap<Long, Msg> msgListMms = new HashMap<>();
            c = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION_SHORT, null, null, null);
            while (c != null) {
                try {
                    if (!c.moveToNext()) {
                        break;
                    }
                    long id2 = c.getLong(c.getColumnIndex("_id"));
                    int type2 = c.getInt(c.getColumnIndex("msg_box"));
                    int threadId2 = c.getInt(c.getColumnIndex("thread_id"));
                    Msg msg2 = new Msg(id2, type2, threadId2);
                    msgListMms.put(Long.valueOf(id2), msg2);
                } finally {
                }
            }
            close(c);
            synchronized (this.mMsgListMms) {
                this.mMsgListMms.clear();
                this.mMsgListMms = msgListMms;
            }
        }
        if (this.mAccount != null) {
            HashMap<Long, Msg> msgListEmail = new HashMap<>();
            Uri uri = this.mMessageUri;
            c = this.mProviderClient.query(uri, EMAIL_PROJECTION_SHORT, null, null, null);
            while (c != null) {
                try {
                    if (!c.moveToNext()) {
                        break;
                    }
                    long id3 = c.getLong(c.getColumnIndex("_id"));
                    long folderId = c.getInt(c.getColumnIndex("folder_id"));
                    Msg msg3 = new Msg(id3, folderId);
                    msgListEmail.put(Long.valueOf(id3), msg3);
                } finally {
                }
            }
            close(c);
            synchronized (this.mMsgListEmail) {
                this.mMsgListEmail.clear();
                this.mMsgListEmail = msgListEmail;
            }
        }
    }

    private void handleMsgListChangesSms() {
        HashMap<Long, Msg> msgListSms = new HashMap<>();
        Cursor c = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION_SHORT, null, null, null);
        synchronized (this.mMsgListSms) {
            while (c != null) {
                try {
                    if (!c.moveToNext()) {
                        break;
                    }
                    long id = c.getLong(c.getColumnIndex("_id"));
                    int type = c.getInt(c.getColumnIndex("type"));
                    int threadId = c.getInt(c.getColumnIndex("thread_id"));
                    Msg msg = this.mMsgListSms.remove(Long.valueOf(id));
                    if (msg == null) {
                        msgListSms.put(Long.valueOf(id), new Msg(id, type, threadId));
                        Event evt = new Event(EVENT_TYPE_NEW, id, folderSms[type], null, this.mSmsType);
                        sendEvent(evt);
                    } else {
                        if (type != msg.type) {
                            Log.d(TAG, "new type: " + type + " old type: " + msg.type);
                            String oldFolder = folderSms[msg.type];
                            String newFolder = folderSms[type];
                            if (!oldFolder.equals(newFolder)) {
                                Event evt2 = new Event(EVENT_TYPE_SHIFT, id, folderSms[type], oldFolder, this.mSmsType);
                                sendEvent(evt2);
                            }
                            msg.type = type;
                        } else if (threadId != msg.threadId) {
                            Log.d(TAG, "Message delete change: type: " + type + " old type: " + msg.type + "\n    threadId: " + threadId + " old threadId: " + msg.threadId);
                            if (threadId == -1) {
                                Event evt3 = new Event(EVENT_TYPE_DELETE, id, "deleted", folderSms[msg.type], this.mSmsType);
                                sendEvent(evt3);
                                msg.threadId = threadId;
                            } else {
                                Event evt4 = new Event(EVENT_TYPE_SHIFT, id, folderSms[msg.type], "deleted", this.mSmsType);
                                sendEvent(evt4);
                                msg.threadId = threadId;
                            }
                        }
                        msgListSms.put(Long.valueOf(id), msg);
                    }
                } catch (Throwable th) {
                    close(c);
                    throw th;
                }
            }
            close(c);
            for (Msg msg2 : this.mMsgListSms.values()) {
                Event evt5 = new Event(EVENT_TYPE_DELETE, msg2.id, "deleted", folderSms[msg2.type], this.mSmsType);
                sendEvent(evt5);
            }
            this.mMsgListSms = msgListSms;
        }
    }

    private void handleMsgListChangesMms() {
        HashMap<Long, Msg> msgListMms = new HashMap<>();
        Cursor c = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION_SHORT, null, null, null);
        synchronized (this.mMsgListMms) {
            while (c != null) {
                try {
                    if (!c.moveToNext()) {
                        break;
                    }
                    long id = c.getLong(c.getColumnIndex("_id"));
                    int type = c.getInt(c.getColumnIndex("msg_box"));
                    int mtype = c.getInt(c.getColumnIndex("m_type"));
                    int threadId = c.getInt(c.getColumnIndex("thread_id"));
                    Msg msg = this.mMsgListMms.remove(Long.valueOf(id));
                    if (msg == null) {
                        if (!folderMms[type].equals("inbox") || mtype == 132) {
                            msgListMms.put(Long.valueOf(id), new Msg(id, type, threadId));
                            Event evt = new Event(EVENT_TYPE_NEW, id, folderMms[type], null, BluetoothMapUtils.TYPE.MMS);
                            sendEvent(evt);
                        }
                    } else {
                        if (type != msg.type) {
                            Log.d(TAG, "new type: " + type + " old type: " + msg.type);
                            if (!msg.localInitiatedSend) {
                                Event evt2 = new Event(EVENT_TYPE_SHIFT, id, folderMms[type], folderMms[msg.type], BluetoothMapUtils.TYPE.MMS);
                                sendEvent(evt2);
                            }
                            msg.type = type;
                            if (folderMms[type].equals("sent") && msg.localInitiatedSend) {
                                msg.localInitiatedSend = false;
                                Event evt3 = new Event(EVENT_TYPE_SENDING_SUCCESS, id, folderSms[type], null, BluetoothMapUtils.TYPE.MMS);
                                sendEvent(evt3);
                            }
                        } else if (threadId != msg.threadId) {
                            Log.d(TAG, "Message delete change: type: " + type + " old type: " + msg.type + "\n    threadId: " + threadId + " old threadId: " + msg.threadId);
                            if (threadId == -1) {
                                Event evt4 = new Event(EVENT_TYPE_DELETE, id, "deleted", folderMms[msg.type], BluetoothMapUtils.TYPE.MMS);
                                sendEvent(evt4);
                                msg.threadId = threadId;
                            } else {
                                Event evt5 = new Event(EVENT_TYPE_SHIFT, id, folderMms[msg.type], "deleted", BluetoothMapUtils.TYPE.MMS);
                                sendEvent(evt5);
                                msg.threadId = threadId;
                            }
                        }
                        msgListMms.put(Long.valueOf(id), msg);
                    }
                } catch (Throwable th) {
                    close(c);
                    throw th;
                }
            }
            close(c);
            for (Msg msg2 : this.mMsgListMms.values()) {
                Event evt6 = new Event(EVENT_TYPE_DELETE, msg2.id, "deleted", folderMms[msg2.type], BluetoothMapUtils.TYPE.MMS);
                sendEvent(evt6);
            }
            this.mMsgListMms = msgListMms;
        }
    }

    private void handleMsgListChangesEmail(Uri uri) throws RemoteException {
        String oldFolder;
        String newFolder;
        String oldFolder2;
        HashMap<Long, Msg> msgListEmail = new HashMap<>();
        Cursor c = this.mProviderClient.query(this.mMessageUri, EMAIL_PROJECTION_SHORT, null, null, null);
        synchronized (this.mMsgListEmail) {
            while (c != null) {
                try {
                    if (!c.moveToNext()) {
                        break;
                    }
                    long id = c.getLong(c.getColumnIndex("_id"));
                    int folderId = c.getInt(c.getColumnIndex("folder_id"));
                    Msg msg = this.mMsgListEmail.remove(Long.valueOf(id));
                    BluetoothMapFolderElement folderElement = this.mFolders.getEmailFolderById(folderId);
                    if (folderElement != null) {
                        newFolder = folderElement.getFullPath();
                    } else {
                        newFolder = "unknown";
                    }
                    if (msg == null) {
                        msgListEmail.put(Long.valueOf(id), new Msg(id, folderId));
                        Event evt = new Event(EVENT_TYPE_NEW, id, newFolder, null, BluetoothMapUtils.TYPE.EMAIL);
                        sendEvent(evt);
                    } else {
                        if (folderId != msg.folderId) {
                            Log.d(TAG, "new folderId: " + folderId + " old folderId: " + msg.folderId);
                            BluetoothMapFolderElement oldFolderElement = this.mFolders.getEmailFolderById(msg.folderId);
                            if (oldFolderElement != null) {
                                oldFolder2 = oldFolderElement.getFullPath();
                            } else {
                                oldFolder2 = "unknown";
                            }
                            BluetoothMapFolderElement deletedFolder = this.mFolders.getEmailFolderByName("deleted");
                            BluetoothMapFolderElement sentFolder = this.mFolders.getEmailFolderByName("sent");
                            if (deletedFolder != null && deletedFolder.getEmailFolderId() == folderId) {
                                Event evt2 = new Event(EVENT_TYPE_DELETE, msg.id, newFolder, oldFolder2, BluetoothMapUtils.TYPE.EMAIL);
                                sendEvent(evt2);
                            } else if (sentFolder != null && sentFolder.getEmailFolderId() == folderId && msg.localInitiatedSend) {
                                if (msg.transparent) {
                                    this.mResolver.delete(ContentUris.withAppendedId(this.mMessageUri, id), null, null);
                                } else {
                                    msg.localInitiatedSend = false;
                                    Event evt3 = new Event(EVENT_TYPE_SENDING_SUCCESS, msg.id, oldFolder2, null, BluetoothMapUtils.TYPE.EMAIL);
                                    sendEvent(evt3);
                                }
                            } else {
                                Event evt4 = new Event(EVENT_TYPE_SHIFT, id, newFolder, oldFolder2, BluetoothMapUtils.TYPE.EMAIL);
                                sendEvent(evt4);
                            }
                            msg.folderId = folderId;
                        }
                        msgListEmail.put(Long.valueOf(id), msg);
                    }
                } catch (Throwable th) {
                    close(c);
                    throw th;
                }
            }
            close(c);
            for (Msg msg2 : this.mMsgListEmail.values()) {
                BluetoothMapFolderElement oldFolderElement2 = this.mFolders.getEmailFolderById(msg2.folderId);
                if (oldFolderElement2 != null) {
                    oldFolder = oldFolderElement2.getFullPath();
                } else {
                    oldFolder = "unknown";
                }
                if (msg2.localInitiatedSend) {
                    msg2.localInitiatedSend = false;
                    if (msg2.transparent) {
                        oldFolder = null;
                    }
                    Event evt5 = new Event(EVENT_TYPE_SENDING_SUCCESS, msg2.id, oldFolder, null, BluetoothMapUtils.TYPE.EMAIL);
                    sendEvent(evt5);
                }
                if (!msg2.transparent) {
                    Event evt6 = new Event(EVENT_TYPE_DELETE, msg2.id, null, oldFolder, BluetoothMapUtils.TYPE.EMAIL);
                    sendEvent(evt6);
                }
            }
            this.mMsgListEmail = msgListEmail;
        }
    }

    private void handleMsgListChanges(Uri uri) {
        if (uri.getAuthority().equals(this.mAuthority)) {
            try {
                handleMsgListChangesEmail(uri);
                return;
            } catch (RemoteException e) {
                this.mMasInstance.restartObexServerSession();
                Log.w(TAG, "Problems contacting the ContentProvider in mas Instance " + this.mMasId + " restaring ObexServerSession");
                return;
            }
        }
        handleMsgListChangesSms();
        handleMsgListChangesMms();
    }

    private boolean setEmailMessageStatusDelete(BluetoothMapFolderElement mCurrentFolder, String uriStr, long handle, int status) {
        boolean res = false;
        Uri uri = Uri.parse(uriStr + "Message");
        ContentValues contentValues = new ContentValues();
        BluetoothMapFolderElement deleteFolder = this.mFolders.getEmailFolderByName("deleted");
        contentValues.put("_id", Long.valueOf(handle));
        synchronized (this.mMsgListEmail) {
            Msg msg = this.mMsgListEmail.get(Long.valueOf(handle));
            if (status == 1) {
                long folderId = -1;
                if (deleteFolder != null) {
                    folderId = deleteFolder.getEmailFolderId();
                }
                contentValues.put("folder_id", Long.valueOf(folderId));
                int updateCount = this.mResolver.update(uri, contentValues, null, null);
                if (updateCount > 0) {
                    res = true;
                    if (msg != null) {
                        msg.oldFolderId = msg.folderId;
                        msg.folderId = folderId;
                    }
                    Log.d(TAG, "Deleted MSG: " + handle + " from folderId: " + folderId);
                } else {
                    Log.w(TAG, "Msg: " + handle + " - Set delete status " + status + " failed for folderId " + folderId);
                }
            } else if (status == 0 && msg != null && deleteFolder != null && msg.folderId == deleteFolder.getEmailFolderId()) {
                long folderId2 = -1;
                if (msg != null && msg.oldFolderId != -1) {
                    folderId2 = msg.oldFolderId;
                } else {
                    BluetoothMapFolderElement inboxFolder = mCurrentFolder.getEmailFolderByName("inbox");
                    if (inboxFolder != null) {
                        folderId2 = inboxFolder.getEmailFolderId();
                    }
                    Log.d(TAG, "We did not delete the message, hence the old folder is unknown. Moving to inbox.");
                }
                contentValues.put("folder_id", Long.valueOf(folderId2));
                int updateCount2 = this.mResolver.update(uri, contentValues, null, null);
                if (updateCount2 > 0) {
                    res = true;
                    msg.folderId = folderId2;
                } else {
                    Log.d(TAG, "We did not delete the message, hence the old folder is unknown. Moving to inbox.");
                }
            }
        }
        if (!res) {
            Log.w(TAG, "Set delete status " + status + " failed.");
        }
        return res;
    }

    private void updateThreadId(Uri uri, String valueString, long threadId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(valueString, Long.valueOf(threadId));
        this.mResolver.update(uri, contentValues, null, null);
    }

    private boolean deleteMessageMms(long handle) {
        boolean res = false;
        Uri uri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, handle);
        Cursor c = this.mResolver.query(uri, null, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int threadId = c.getInt(c.getColumnIndex("thread_id"));
                    if (threadId != -1) {
                        synchronized (this.mMsgListMms) {
                            Msg msg = this.mMsgListMms.get(Long.valueOf(handle));
                            if (msg != null) {
                                msg.threadId = -1;
                            }
                        }
                        updateThreadId(uri, "thread_id", -1L);
                    } else {
                        synchronized (this.mMsgListMms) {
                            this.mMsgListMms.remove(Long.valueOf(handle));
                        }
                        this.mResolver.delete(uri, null, null);
                    }
                    res = true;
                }
            } finally {
                close(c);
            }
        }
        return res;
    }

    private boolean unDeleteMessageMms(long handle) {
        String address;
        boolean res = false;
        Uri uri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, handle);
        Cursor c = this.mResolver.query(uri, null, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int threadId = c.getInt(c.getColumnIndex("thread_id"));
                    if (threadId == -1) {
                        long id = c.getLong(c.getColumnIndex("_id"));
                        int msgBox = c.getInt(c.getColumnIndex("msg_box"));
                        if (msgBox == 1) {
                            address = BluetoothMapContent.getAddressMms(this.mResolver, id, BluetoothMapContent.MMS_FROM);
                        } else {
                            address = BluetoothMapContent.getAddressMms(this.mResolver, id, BluetoothMapContent.MMS_TO);
                        }
                        Set<String> recipients = new HashSet<>();
                        recipients.addAll(Arrays.asList(address));
                        Long oldThreadId = Long.valueOf(Telephony.Threads.getOrCreateThreadId(this.mContext, recipients));
                        synchronized (this.mMsgListMms) {
                            Msg msg = this.mMsgListMms.get(Long.valueOf(handle));
                            if (msg != null) {
                                msg.threadId = oldThreadId.intValue();
                            }
                        }
                        updateThreadId(uri, "thread_id", oldThreadId.longValue());
                    } else {
                        Log.d(TAG, "Message not in deleted folder: handle " + handle + " threadId " + threadId);
                    }
                    res = true;
                }
            } finally {
                close(c);
            }
        }
        return res;
    }

    private boolean deleteMessageSms(long handle) {
        boolean res = false;
        Uri uri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, handle);
        Cursor c = this.mResolver.query(uri, null, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int threadId = c.getInt(c.getColumnIndex("thread_id"));
                    if (threadId != -1) {
                        synchronized (this.mMsgListSms) {
                            Msg msg = this.mMsgListSms.get(Long.valueOf(handle));
                            if (msg != null) {
                                msg.threadId = -1;
                            }
                        }
                        updateThreadId(uri, "thread_id", -1L);
                    } else {
                        synchronized (this.mMsgListSms) {
                            this.mMsgListSms.remove(Long.valueOf(handle));
                        }
                        this.mResolver.delete(uri, null, null);
                    }
                    res = true;
                }
            } finally {
                close(c);
            }
        }
        return res;
    }

    private boolean unDeleteMessageSms(long handle) {
        boolean res = false;
        Uri uri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, handle);
        Cursor c = this.mResolver.query(uri, null, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int threadId = c.getInt(c.getColumnIndex("thread_id"));
                    if (threadId == -1) {
                        String address = c.getString(c.getColumnIndex("address"));
                        Set<String> recipients = new HashSet<>();
                        recipients.addAll(Arrays.asList(address));
                        Long oldThreadId = Long.valueOf(Telephony.Threads.getOrCreateThreadId(this.mContext, recipients));
                        synchronized (this.mMsgListSms) {
                            Msg msg = this.mMsgListSms.get(Long.valueOf(handle));
                            if (msg != null) {
                                msg.threadId = oldThreadId.intValue();
                            }
                        }
                        updateThreadId(uri, "thread_id", oldThreadId.longValue());
                    } else {
                        Log.d(TAG, "Message not in deleted folder: handle " + handle + " threadId " + threadId);
                    }
                    res = true;
                }
            } finally {
                close(c);
            }
        }
        return res;
    }

    public boolean setMessageStatusDeleted(long handle, BluetoothMapUtils.TYPE type, BluetoothMapFolderElement mCurrentFolder, String uriStr, int statusValue) {
        Log.d(TAG, "setMessageStatusDeleted: handle " + handle + " type " + type + " value " + statusValue);
        if (type == BluetoothMapUtils.TYPE.EMAIL) {
            boolean res = setEmailMessageStatusDelete(mCurrentFolder, uriStr, handle, statusValue);
            return res;
        }
        if (statusValue == 1) {
            if (type == BluetoothMapUtils.TYPE.SMS_GSM || type == BluetoothMapUtils.TYPE.SMS_CDMA) {
                boolean res2 = deleteMessageSms(handle);
                return res2;
            }
            if (type != BluetoothMapUtils.TYPE.MMS) {
                return false;
            }
            boolean res3 = deleteMessageMms(handle);
            return res3;
        }
        if (statusValue != 0) {
            return false;
        }
        if (type == BluetoothMapUtils.TYPE.SMS_GSM || type == BluetoothMapUtils.TYPE.SMS_CDMA) {
            boolean res4 = unDeleteMessageSms(handle);
            return res4;
        }
        if (type != BluetoothMapUtils.TYPE.MMS) {
            return false;
        }
        boolean res5 = unDeleteMessageMms(handle);
        return res5;
    }

    public boolean setMessageStatusRead(long handle, BluetoothMapUtils.TYPE type, String uriStr, int statusValue) throws RemoteException {
        int count = 0;
        Log.d(TAG, "setMessageStatusRead: handle " + handle + " type " + type + " value " + statusValue);
        if (type == BluetoothMapUtils.TYPE.SMS_GSM || type == BluetoothMapUtils.TYPE.SMS_CDMA) {
            Uri uri = Telephony.Sms.Inbox.CONTENT_URI;
            ContentValues contentValues = new ContentValues();
            contentValues.put("read", Integer.valueOf(statusValue));
            contentValues.put("seen", Integer.valueOf(statusValue));
            String where = "_id=" + handle;
            String values = contentValues.toString();
            Log.d(TAG, " -> SMS Uri: " + uri.toString() + " Where " + where + " values " + values);
            count = this.mResolver.update(uri, contentValues, where, null);
            Log.d(TAG, " -> " + count + " rows updated!");
        } else if (type == BluetoothMapUtils.TYPE.MMS) {
            Uri uri2 = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, handle);
            Log.d(TAG, " -> MMS Uri: " + uri2.toString());
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("read", Integer.valueOf(statusValue));
            count = this.mResolver.update(uri2, contentValues2, null, null);
            Log.d(TAG, " -> " + count + " rows updated!");
        }
        if (type == BluetoothMapUtils.TYPE.EMAIL) {
            Uri uri3 = this.mMessageUri;
            ContentValues contentValues3 = new ContentValues();
            contentValues3.put("flag_read", Integer.valueOf(statusValue));
            contentValues3.put("_id", Long.valueOf(handle));
            count = this.mProviderClient.update(uri3, contentValues3, null, null);
        }
        return count > 0;
    }

    private class PushMsgInfo {
        long id;
        int parts;
        int partsDelivered;
        int partsSent;
        String phone;
        int retry;
        int transparent;
        Uri uri;
        boolean resend = false;
        boolean sendInProgress = false;
        boolean failedSent = false;
        int statusDelivered = 0;
        long timestamp = 0;

        public PushMsgInfo(long id, int transparent, int retry, String phone, Uri uri) {
            this.id = id;
            this.transparent = transparent;
            this.retry = retry;
            this.phone = phone;
            this.uri = uri;
        }
    }

    public long pushMessage(BluetoothMapbMessage msg, BluetoothMapFolderElement folderElement, BluetoothMapAppParams ap, String emailBaseUri) throws Throwable {
        FileOutputStream os;
        Log.d(TAG, "pushMessage");
        ArrayList<BluetoothMapbMessage.vCard> recipientList = msg.getRecipients();
        int transparent = ap.getTransparent() == -1 ? 0 : ap.getTransparent();
        int retry = ap.getRetry();
        ap.getCharset();
        long handle = -1;
        if (recipientList == null) {
            Log.d(TAG, "empty recipient list");
            return -1L;
        }
        if (msg.getType().equals(BluetoothMapUtils.TYPE.EMAIL)) {
            String msgBody = ((BluetoothMapbMessageEmail) msg).getEmailBody();
            FileOutputStream os2 = null;
            ParcelFileDescriptor fdOut = null;
            Uri uriInsert = Uri.parse(emailBaseUri + "Message");
            Log.d(TAG, "pushMessage - uriInsert= " + uriInsert.toString() + ", intoFolder id=" + folderElement.getEmailFolderId());
            synchronized (this.mMsgListEmail) {
                try {
                    ContentValues values = new ContentValues();
                    long folderId = folderElement.getEmailFolderId();
                    values.put("folder_id", Long.valueOf(folderId));
                    Uri uriNew = this.mProviderClient.insert(uriInsert, values);
                    Log.d(TAG, "pushMessage - uriNew= " + uriNew.toString());
                    handle = Long.parseLong(uriNew.getLastPathSegment());
                    try {
                        try {
                            fdOut = this.mProviderClient.openFile(uriNew, "w");
                            os = new FileOutputStream(fdOut.getFileDescriptor());
                        } catch (Throwable th) {
                            th = th;
                        }
                    } catch (FileNotFoundException e) {
                        e = e;
                    } catch (NullPointerException e2) {
                        e = e2;
                    }
                    try {
                        os.write(msgBody.getBytes(), 0, msgBody.getBytes().length);
                        if (os != null) {
                            try {
                                try {
                                    os.close();
                                } catch (IOException e3) {
                                    Log.w(TAG, e3);
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                        if (fdOut != null) {
                            try {
                                fdOut.close();
                            } catch (IOException e4) {
                                Log.w(TAG, e4);
                            }
                        }
                        Msg newMsg = new Msg(handle, folderId);
                        newMsg.transparent = transparent == 1;
                        if (folderId == folderElement.getEmailFolderByName("outbox").getEmailFolderId()) {
                            newMsg.localInitiatedSend = true;
                        }
                        this.mMsgListEmail.put(Long.valueOf(handle), newMsg);
                    } catch (FileNotFoundException e5) {
                        e = e5;
                        Log.w(TAG, e);
                        throw new IOException("Unable to open file stream");
                    } catch (NullPointerException e6) {
                        e = e6;
                        Log.w(TAG, e);
                        throw new IllegalArgumentException("Unable to parse message.");
                    } catch (Throwable th3) {
                        th = th3;
                        os2 = os;
                        if (os2 != null) {
                            try {
                                os2.close();
                            } catch (IOException e7) {
                                Log.w(TAG, e7);
                            }
                        }
                        if (fdOut != null) {
                            try {
                                fdOut.close();
                            } catch (IOException e8) {
                                Log.w(TAG, e8);
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    throw th;
                }
            }
        } else {
            for (BluetoothMapbMessage.vCard recipient : recipientList) {
                if (recipient.getEnvLevel() == 0) {
                    String phone = recipient.getFirstPhoneNumber();
                    recipient.getFirstEmail();
                    String folder = folderElement.getName();
                    String msgBody2 = null;
                    if (msg.getType().equals(BluetoothMapUtils.TYPE.MMS) && ((BluetoothMapbMessageMms) msg).getTextOnly()) {
                        msgBody2 = ((BluetoothMapbMessageMms) msg).getMessageAsText();
                        SmsManager smsMng = SmsManager.getDefault();
                        ArrayList<String> parts = smsMng.divideMessage(msgBody2);
                        int smsParts = parts.size();
                        if (smsParts <= 10) {
                            Log.d(TAG, "pushMessage - converting MMS to SMS, sms parts=" + smsParts);
                            msg.setType(this.mSmsType);
                        } else {
                            Log.d(TAG, "pushMessage - MMS text only but to big to convert to SMS");
                            msgBody2 = null;
                        }
                    }
                    if (msg.getType().equals(BluetoothMapUtils.TYPE.MMS)) {
                        handle = sendMmsMessage(folder, phone, (BluetoothMapbMessageMms) msg);
                    } else {
                        if (msg.getType().equals(BluetoothMapUtils.TYPE.SMS_GSM) || msg.getType().equals(BluetoothMapUtils.TYPE.SMS_CDMA)) {
                            if (msgBody2 == null) {
                                msgBody2 = ((BluetoothMapbMessageSms) msg).getSmsBody();
                            }
                            Uri contentUri = Uri.parse(Telephony.Sms.CONTENT_URI + "/" + folder);
                            synchronized (this.mMsgListSms) {
                                Uri uri = Telephony.Sms.addMessageToUri(this.mResolver, contentUri, phone, msgBody2, "", Long.valueOf(System.currentTimeMillis()), false, true);
                                if (uri == null) {
                                    Log.d(TAG, "pushMessage - failure on add to uri " + contentUri);
                                    return -1L;
                                }
                                Cursor c = this.mResolver.query(uri, SMS_PROJECTION_SHORT, null, null, null);
                                if (c != null) {
                                    try {
                                        if (c.moveToFirst()) {
                                            long id = c.getLong(c.getColumnIndex("_id"));
                                            int type = c.getInt(c.getColumnIndex("type"));
                                            int threadId = c.getInt(c.getColumnIndex("thread_id"));
                                            this.mMsgListSms.put(Long.valueOf(id), new Msg(id, type, threadId));
                                            close(c);
                                            handle = Long.parseLong(uri.getLastPathSegment());
                                            if (folder.equals("outbox")) {
                                                PushMsgInfo msgInfo = new PushMsgInfo(handle, transparent, retry, phone, uri);
                                                this.mPushMsgList.put(Long.valueOf(handle), msgInfo);
                                                sendMessage(msgInfo, msgBody2);
                                            }
                                        }
                                    } finally {
                                        close(c);
                                    }
                                }
                                return -1L;
                            }
                        }
                        Log.d(TAG, "pushMessage - failure on type ");
                        return -1L;
                    }
                }
            }
        }
        return handle;
    }

    public long sendMmsMessage(String folder, String to_address, BluetoothMapbMessageMms msg) {
        if (folder != null && (folder.equalsIgnoreCase("outbox") || folder.equalsIgnoreCase("draft"))) {
            long handle = pushMmsToFolder(3, to_address, msg);
            if (-1 != handle && folder.equalsIgnoreCase("outbox")) {
                moveDraftToOutbox(handle);
                Intent sendIntent = new Intent("android.intent.action.MMS_SEND_OUTBOX_MSG");
                Log.d(TAG, "broadcasting intent: " + sendIntent.toString());
                this.mContext.sendBroadcast(sendIntent);
            }
            return handle;
        }
        throw new IllegalArgumentException("Cannot push message to other folders than outbox/draft");
    }

    private void moveDraftToOutbox(long handle) {
        if (handle == -1) {
            String whereClause = " _id= " + handle;
            Uri uri = Telephony.Mms.CONTENT_URI;
            Cursor queryResult = this.mResolver.query(uri, null, whereClause, null, null);
            if (queryResult != null) {
                try {
                    if (queryResult.moveToFirst()) {
                        ContentValues data = new ContentValues();
                        data.put("msg_box", (Integer) 4);
                        this.mResolver.update(uri, data, whereClause, null);
                        Log.d(TAG, "Moved draft MMS to outbox");
                    } else {
                        Log.d(TAG, "Could not move draft to outbox ");
                    }
                } finally {
                    queryResult.close();
                }
            }
        }
    }

    private long pushMmsToFolder(int folder, String to_address, BluetoothMapbMessageMms msg) {
        long handle;
        ContentValues values = new ContentValues();
        values.put("msg_box", Integer.valueOf(folder));
        values.put("read", (Integer) 0);
        values.put("seen", (Integer) 0);
        if (msg.getSubject() != null) {
            values.put("sub", msg.getSubject());
        } else {
            values.put("sub", "");
        }
        if (msg.getSubject() != null && msg.getSubject().length() > 0) {
            values.put("sub_cs", (Integer) 106);
        }
        values.put("ct_t", "application/vnd.wap.multipart.related");
        values.put("exp", (Integer) 604800);
        values.put("m_cls", "personal");
        values.put("m_type", (Integer) 128);
        values.put("v", (Integer) 18);
        values.put("pri", Integer.valueOf(BluetoothMapContent.MMS_BCC));
        values.put("rr", Integer.valueOf(BluetoothMapContent.MMS_BCC));
        values.put("tr_id", "T" + Long.toHexString(System.currentTimeMillis()));
        values.put("d_rpt", Integer.valueOf(BluetoothMapContent.MMS_BCC));
        values.put("locked", (Integer) 0);
        if (msg.getTextOnly()) {
            values.put("text_only", (Boolean) true);
        }
        values.put("m_size", Integer.valueOf(msg.getSize()));
        Set<String> recipients = new HashSet<>();
        recipients.addAll(Arrays.asList(to_address));
        values.put("thread_id", Long.valueOf(Telephony.Threads.getOrCreateThreadId(this.mContext, recipients)));
        Uri uri = Telephony.Mms.CONTENT_URI;
        synchronized (this.mMsgListMms) {
            Uri uri2 = this.mResolver.insert(uri, values);
            if (uri2 == null) {
                Log.e(TAG, "Unabled to insert MMS " + values + "Uri: " + uri2);
                handle = -1;
            } else {
                Cursor c = this.mResolver.query(uri2, MMS_PROJECTION_SHORT, null, null, null);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            long id = c.getLong(c.getColumnIndex("_id"));
                            int type = c.getInt(c.getColumnIndex("msg_box"));
                            int threadId = c.getInt(c.getColumnIndex("thread_id"));
                            Msg newMsg = new Msg(id, type, threadId);
                            newMsg.localInitiatedSend = true;
                            this.mMsgListMms.put(Long.valueOf(id), newMsg);
                        }
                    } finally {
                        close(c);
                    }
                }
                handle = Long.parseLong(uri2.getLastPathSegment());
                try {
                    if (msg.getMimeParts() == null) {
                        Log.w(TAG, "No MMS parts present...");
                    } else {
                        int count = 0;
                        for (BluetoothMapbMessageMms.MimePart part : msg.getMimeParts()) {
                            count++;
                            values.clear();
                            if (part.mContentType != null && part.mContentType.toUpperCase().contains("TEXT")) {
                                values.put("ct", "text/plain");
                                values.put("chset", (Integer) 106);
                                if (part.mPartName != null) {
                                    values.put("fn", part.mPartName);
                                    values.put("name", part.mPartName);
                                } else {
                                    values.put("fn", "text_" + count + ".txt");
                                    values.put("name", "text_" + count + ".txt");
                                }
                                if (part.mContentId != null) {
                                    values.put("cid", part.mContentId);
                                } else if (part.mPartName != null) {
                                    values.put("cid", "<" + part.mPartName + ">");
                                } else {
                                    values.put("cid", "<text_" + count + ">");
                                }
                                if (part.mContentLocation != null) {
                                    values.put("cl", part.mContentLocation);
                                } else if (part.mPartName != null) {
                                    values.put("cl", part.mPartName + ".txt");
                                } else {
                                    values.put("cl", "text_" + count + ".txt");
                                }
                                if (part.mContentDisposition != null) {
                                    values.put("cd", part.mContentDisposition);
                                }
                                values.put("text", part.getDataAsString());
                                Uri uri3 = Uri.parse(Telephony.Mms.CONTENT_URI + "/" + handle + "/part");
                                uri2 = this.mResolver.insert(uri3, values);
                            } else if (part.mContentType != null && part.mContentType.toUpperCase().contains("SMIL")) {
                                values.put("seq", (Integer) (-1));
                                values.put("ct", "application/smil");
                                if (part.mContentId != null) {
                                    values.put("cid", part.mContentId);
                                } else {
                                    values.put("cid", "<smil_" + count + ">");
                                }
                                if (part.mContentLocation != null) {
                                    values.put("cl", part.mContentLocation);
                                } else {
                                    values.put("cl", "smil_" + count + ".xml");
                                }
                                if (part.mContentDisposition != null) {
                                    values.put("cd", part.mContentDisposition);
                                }
                                values.put("fn", "smil.xml");
                                values.put("name", "smil.xml");
                                values.put("text", new String(part.mData, "UTF-8"));
                                Uri uri4 = Uri.parse(Telephony.Mms.CONTENT_URI + "/" + handle + "/part");
                                uri2 = this.mResolver.insert(uri4, values);
                            } else {
                                writeMmsDataPart(handle, part, count);
                            }
                            if (uri2 != null) {
                            }
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.w(TAG, e);
                } catch (IOException e2) {
                    Log.w(TAG, e2);
                }
                values.clear();
                values.put("contact_id", "null");
                values.put("address", BluetoothMapContent.INSERT_ADDRES_TOKEN);
                values.put("type", Integer.valueOf(BluetoothMapContent.MMS_FROM));
                values.put("charset", (Integer) 106);
                Uri uri5 = Uri.parse(Telephony.Mms.CONTENT_URI + "/" + handle + "/addr");
                if (this.mResolver.insert(uri5, values) != null) {
                }
                values.clear();
                values.put("contact_id", "null");
                values.put("address", to_address);
                values.put("type", Integer.valueOf(BluetoothMapContent.MMS_TO));
                values.put("charset", (Integer) 106);
                Uri uri6 = Uri.parse(Telephony.Mms.CONTENT_URI + "/" + handle + "/addr");
                if (this.mResolver.insert(uri6, values) != null) {
                }
            }
        }
        return handle;
    }

    private void writeMmsDataPart(long handle, BluetoothMapbMessageMms.MimePart part, int count) throws IOException {
        ContentValues values = new ContentValues();
        values.put("mid", Long.valueOf(handle));
        if (part.mContentType != null) {
            values.put("ct", part.mContentType);
        } else {
            Log.w(TAG, "MMS has no CONTENT_TYPE for part " + count);
        }
        if (part.mContentId != null) {
            values.put("cid", part.mContentId);
        } else if (part.mPartName != null) {
            values.put("cid", "<" + part.mPartName + ">");
        } else {
            values.put("cid", "<part_" + count + ">");
        }
        if (part.mContentLocation != null) {
            values.put("cl", part.mContentLocation);
        } else if (part.mPartName != null) {
            values.put("cl", part.mPartName + ".dat");
        } else {
            values.put("cl", "part_" + count + ".dat");
        }
        if (part.mContentDisposition != null) {
            values.put("cd", part.mContentDisposition);
        }
        if (part.mPartName != null) {
            values.put("fn", part.mPartName);
            values.put("name", part.mPartName);
        } else {
            values.put("fn", "part_" + count + ".dat");
            values.put("name", "part_" + count + ".dat");
        }
        Uri partUri = Uri.parse(Telephony.Mms.CONTENT_URI + "/" + handle + "/part");
        Uri res = this.mResolver.insert(partUri, values);
        OutputStream os = this.mResolver.openOutputStream(res);
        os.write(part.mData);
        os.close();
    }

    public void sendMessage(PushMsgInfo msgInfo, String msgBody) {
        SmsManager smsMng = SmsManager.getDefault();
        ArrayList<String> parts = smsMng.divideMessage(msgBody);
        msgInfo.parts = parts.size();
        msgInfo.timestamp = Calendar.getInstance().getTime().getTime();
        msgInfo.partsDelivered = 0;
        msgInfo.partsSent = 0;
        ArrayList<PendingIntent> deliveryIntents = new ArrayList<>(msgInfo.parts);
        ArrayList<PendingIntent> sentIntents = new ArrayList<>(msgInfo.parts);
        for (int i = 0; i < msgInfo.parts; i++) {
            Intent intentDelivery = new Intent(ACTION_MESSAGE_DELIVERY, (Uri) null);
            intentDelivery.setType("message/" + Long.toString(msgInfo.id) + msgInfo.timestamp + i);
            intentDelivery.putExtra(EXTRA_MESSAGE_SENT_HANDLE, msgInfo.id);
            intentDelivery.putExtra("timestamp", msgInfo.timestamp);
            PendingIntent pendingIntentDelivery = PendingIntent.getBroadcast(this.mContext, 0, intentDelivery, VCardConfig.FLAG_CONVERT_PHONETIC_NAME_STRINGS);
            Intent intentSent = new Intent(ACTION_MESSAGE_SENT, (Uri) null);
            intentSent.setType("message/" + Long.toString(msgInfo.id) + msgInfo.timestamp + i);
            intentSent.putExtra(EXTRA_MESSAGE_SENT_HANDLE, msgInfo.id);
            intentSent.putExtra("uri", msgInfo.uri.toString());
            intentSent.putExtra(EXTRA_MESSAGE_SENT_RETRY, msgInfo.retry);
            intentSent.putExtra(EXTRA_MESSAGE_SENT_TRANSPARENT, msgInfo.transparent);
            PendingIntent pendingIntentSent = PendingIntent.getBroadcast(this.mContext, 0, intentSent, VCardConfig.FLAG_CONVERT_PHONETIC_NAME_STRINGS);
            deliveryIntents.add(pendingIntentDelivery);
            sentIntents.add(pendingIntentSent);
        }
        Log.d(TAG, "sendMessage to " + msgInfo.phone);
        smsMng.sendMultipartTextMessage(msgInfo.phone, null, parts, sentIntents, deliveryIntents);
    }

    private class SmsBroadcastReceiver extends BroadcastReceiver {
        private final String[] ID_PROJECTION;
        private final Uri UPDATE_STATUS_URI;

        private SmsBroadcastReceiver() {
            this.ID_PROJECTION = new String[]{"_id"};
            this.UPDATE_STATUS_URI = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "/status");
        }

        public void register() {
            Handler handler = new Handler(Looper.getMainLooper());
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothMapContentObserver.ACTION_MESSAGE_DELIVERY);
            try {
                intentFilter.addDataType("message/*");
            } catch (IntentFilter.MalformedMimeTypeException e) {
                Log.e(BluetoothMapContentObserver.TAG, "Wrong mime type!!!", e);
            }
            BluetoothMapContentObserver.this.mContext.registerReceiver(this, intentFilter, null, handler);
        }

        public void unregister() {
            try {
                BluetoothMapContentObserver.this.mContext.unregisterReceiver(this);
            } catch (IllegalArgumentException e) {
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            long handle = intent.getLongExtra(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_HANDLE, -1L);
            PushMsgInfo msgInfo = (PushMsgInfo) BluetoothMapContentObserver.this.mPushMsgList.get(Long.valueOf(handle));
            Log.d(BluetoothMapContentObserver.TAG, "onReceive: action" + action);
            if (msgInfo == null) {
                Log.d(BluetoothMapContentObserver.TAG, "onReceive: no msgInfo found for handle " + handle);
                return;
            }
            if (action.equals(BluetoothMapContentObserver.ACTION_MESSAGE_SENT)) {
                int result = intent.getIntExtra(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_RESULT, 0);
                msgInfo.partsSent++;
                if (result != -1) {
                    msgInfo.failedSent = true;
                }
                Log.d(BluetoothMapContentObserver.TAG, "onReceive: msgInfo.partsSent = " + msgInfo.partsSent + ", msgInfo.parts = " + msgInfo.parts + " result = " + result);
                if (msgInfo.partsSent == msgInfo.parts) {
                    actionMessageSent(context, intent, msgInfo);
                    return;
                }
                return;
            }
            if (action.equals(BluetoothMapContentObserver.ACTION_MESSAGE_DELIVERY)) {
                long timestamp = intent.getLongExtra("timestamp", 0L);
                if (msgInfo.timestamp == timestamp) {
                    msgInfo.partsDelivered++;
                    byte[] pdu = intent.getByteArrayExtra("pdu");
                    String format = intent.getStringExtra("format");
                    SmsMessage message = SmsMessage.createFromPdu(pdu, format);
                    if (message == null) {
                        Log.d(BluetoothMapContentObserver.TAG, "actionMessageDelivery: Can't get message from pdu");
                        return;
                    } else {
                        int status = message.getStatus();
                        if (status != 0) {
                            msgInfo.statusDelivered = status;
                        }
                    }
                }
                if (msgInfo.partsDelivered == msgInfo.parts) {
                    actionMessageDelivery(context, intent, msgInfo);
                    return;
                }
                return;
            }
            Log.d(BluetoothMapContentObserver.TAG, "onReceive: Unknown action " + action);
        }

        private void actionMessageSent(Context context, Intent intent, PushMsgInfo msgInfo) {
            boolean delete = false;
            Log.d(BluetoothMapContentObserver.TAG, "actionMessageSent(): msgInfo.failedSent = " + msgInfo.failedSent);
            msgInfo.sendInProgress = false;
            if (!msgInfo.failedSent) {
                Log.d(BluetoothMapContentObserver.TAG, "actionMessageSent: result OK");
                if (msgInfo.transparent == 0) {
                    if (!Telephony.Sms.moveMessageToFolder(context, msgInfo.uri, 2, 0)) {
                        Log.w(BluetoothMapContentObserver.TAG, "Failed to move " + msgInfo.uri + " to SENT");
                    }
                } else {
                    delete = true;
                }
                Event evt = BluetoothMapContentObserver.this.new Event(BluetoothMapContentObserver.EVENT_TYPE_SENDING_SUCCESS, msgInfo.id, BluetoothMapContentObserver.folderSms[2], null, BluetoothMapContentObserver.this.mSmsType);
                BluetoothMapContentObserver.this.sendEvent(evt);
            } else if (msgInfo.retry == 1) {
                msgInfo.resend = true;
                msgInfo.partsSent = 0;
                msgInfo.failedSent = false;
                Event evt2 = BluetoothMapContentObserver.this.new Event(BluetoothMapContentObserver.EVENT_TYPE_SENDING_FAILURE, msgInfo.id, BluetoothMapContentObserver.folderSms[4], null, BluetoothMapContentObserver.this.mSmsType);
                BluetoothMapContentObserver.this.sendEvent(evt2);
            } else {
                if (msgInfo.transparent == 0) {
                    if (!Telephony.Sms.moveMessageToFolder(context, msgInfo.uri, 5, 0)) {
                        Log.w(BluetoothMapContentObserver.TAG, "Failed to move " + msgInfo.uri + " to FAILED");
                    }
                } else {
                    delete = true;
                }
                Event evt3 = BluetoothMapContentObserver.this.new Event(BluetoothMapContentObserver.EVENT_TYPE_SENDING_FAILURE, msgInfo.id, BluetoothMapContentObserver.folderSms[5], null, BluetoothMapContentObserver.this.mSmsType);
                BluetoothMapContentObserver.this.sendEvent(evt3);
            }
            if (delete) {
                synchronized (BluetoothMapContentObserver.this.mMsgListSms) {
                    BluetoothMapContentObserver.this.mMsgListSms.remove(Long.valueOf(msgInfo.id));
                }
                BluetoothMapContentObserver.this.mResolver.delete(msgInfo.uri, null, null);
            }
        }

        private void actionMessageDelivery(Context context, Intent intent, PushMsgInfo msgInfo) {
            Uri messageUri = intent.getData();
            msgInfo.sendInProgress = false;
            Cursor cursor = BluetoothMapContentObserver.this.mResolver.query(msgInfo.uri, this.ID_PROJECTION, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    int messageId = cursor.getInt(0);
                    Uri updateUri = ContentUris.withAppendedId(this.UPDATE_STATUS_URI, messageId);
                    Log.d(BluetoothMapContentObserver.TAG, "actionMessageDelivery: uri=" + messageUri + ", status=" + msgInfo.statusDelivered);
                    ContentValues contentValues = new ContentValues(2);
                    contentValues.put(BluetoothShare.STATUS, Integer.valueOf(msgInfo.statusDelivered));
                    contentValues.put("date_sent", Long.valueOf(System.currentTimeMillis()));
                    BluetoothMapContentObserver.this.mResolver.update(updateUri, contentValues, null, null);
                } else {
                    Log.d(BluetoothMapContentObserver.TAG, "Can't find message for status update: " + messageUri);
                }
                cursor.close();
                if (msgInfo.statusDelivered == 0) {
                    Event evt = BluetoothMapContentObserver.this.new Event(BluetoothMapContentObserver.EVENT_TYPE_DELEVERY_SUCCESS, msgInfo.id, BluetoothMapContentObserver.folderSms[2], null, BluetoothMapContentObserver.this.mSmsType);
                    BluetoothMapContentObserver.this.sendEvent(evt);
                } else {
                    Event evt2 = BluetoothMapContentObserver.this.new Event(BluetoothMapContentObserver.EVENT_TYPE_SENDING_FAILURE, msgInfo.id, BluetoothMapContentObserver.folderSms[2], null, BluetoothMapContentObserver.this.mSmsType);
                    BluetoothMapContentObserver.this.sendEvent(evt2);
                }
                BluetoothMapContentObserver.this.mPushMsgList.remove(Long.valueOf(msgInfo.id));
            } catch (Throwable th) {
                cursor.close();
                throw th;
            }
        }
    }

    public static void actionMessageSentDisconnected(Context context, Intent intent, int result) {
        if (context.checkCallingOrSelfPermission("android.permission.WRITE_SMS") != 0) {
            Log.w(TAG, "actionMessageSentDisconnected: Not allowed to delete SMS/MMS messages");
            EventLog.writeEvent(1397638484, "b/22343270", Integer.valueOf(Binder.getCallingUid()), "");
            return;
        }
        boolean delete = false;
        int transparent = intent.getIntExtra(EXTRA_MESSAGE_SENT_TRANSPARENT, 0);
        String uriString = intent.getStringExtra("uri");
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            if (result == -1) {
                Log.d(TAG, "actionMessageSentDisconnected: result OK");
                if (transparent == 0) {
                    if (!Telephony.Sms.moveMessageToFolder(context, uri, 2, 0)) {
                        Log.d(TAG, "Failed to move " + uri + " to SENT");
                    }
                } else {
                    delete = true;
                }
            } else if (transparent == 0) {
                if (!Telephony.Sms.moveMessageToFolder(context, uri, 5, 0)) {
                    Log.d(TAG, "Failed to move " + uri + " to FAILED");
                }
            } else {
                delete = true;
            }
            if (delete) {
                ContentResolver resolver = context.getContentResolver();
                if (resolver != null) {
                    resolver.delete(uri, null, null);
                } else {
                    Log.w(TAG, "Unable to get resolver");
                }
            }
        }
    }

    private void registerPhoneServiceStateListener() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        tm.listen(this.mPhoneListener, 1);
    }

    private void unRegisterPhoneServiceStateListener() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        tm.listen(this.mPhoneListener, 0);
    }

    private void resendPendingMessages() {
        Cursor c = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, "type = 4", null, null);
        while (c != null) {
            try {
                if (!c.moveToNext()) {
                    break;
                }
                long id = c.getLong(c.getColumnIndex("_id"));
                String msgBody = c.getString(c.getColumnIndex("body"));
                PushMsgInfo msgInfo = this.mPushMsgList.get(Long.valueOf(id));
                if (msgInfo != null && msgInfo.resend && !msgInfo.sendInProgress) {
                    msgInfo.sendInProgress = true;
                    sendMessage(msgInfo, msgBody);
                }
            } finally {
                close(c);
            }
        }
    }

    private void failPendingMessages() {
        Cursor c = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, "type = 4", null, null);
        if (c != null) {
            while (c != null) {
                try {
                    if (!c.moveToNext()) {
                        break;
                    }
                    long id = c.getLong(c.getColumnIndex("_id"));
                    c.getString(c.getColumnIndex("body"));
                    PushMsgInfo msgInfo = this.mPushMsgList.get(Long.valueOf(id));
                    if (msgInfo != null && msgInfo.resend) {
                        Telephony.Sms.moveMessageToFolder(this.mContext, msgInfo.uri, 5, 0);
                    }
                } finally {
                    close(c);
                }
            }
        }
    }

    private void removeDeletedMessages() {
        this.mResolver.delete(Telephony.Sms.CONTENT_URI, "thread_id = -1", null);
    }

    public void init() {
        this.mSmsBroadcastReceiver.register();
        registerPhoneServiceStateListener();
        this.mInitialized = true;
    }

    public void deinit() {
        this.mInitialized = false;
        unregisterObserver();
        this.mSmsBroadcastReceiver.unregister();
        unRegisterPhoneServiceStateListener();
        failPendingMessages();
        removeDeletedMessages();
    }

    public boolean handleSmsSendIntent(Context context, Intent intent) {
        if (!this.mInitialized) {
            return false;
        }
        this.mSmsBroadcastReceiver.onReceive(context, intent);
        return true;
    }
}
