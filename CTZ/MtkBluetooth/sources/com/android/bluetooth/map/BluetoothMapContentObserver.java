package com.android.bluetooth.map;

import android.annotation.TargetApi;
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
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.EnvironmentCompat;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import com.android.bluetooth.map.BluetoothMapUtils;
import com.android.bluetooth.map.BluetoothMapbMessage;
import com.android.bluetooth.map.BluetoothMapbMessageMime;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.vcard.VCardBuilder;
import com.android.vcard.VCardConfig;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.xmlpull.v1.XmlSerializer;

@TargetApi(19)
public class BluetoothMapContentObserver {
    private static final String ACTION_MESSAGE_DELIVERY = "com.android.bluetooth.BluetoothMapContentObserver.action.MESSAGE_DELIVERY";
    static final String ACTION_MESSAGE_SENT = "com.android.bluetooth.BluetoothMapContentObserver.action.MESSAGE_SENT";
    private static final int CONVERT_MMS_TO_SMS_PART_COUNT = 10;
    public static final int DELETED_THREAD_ID = -1;
    private static final long EVENT_FILTER_CONVERSATION_CHANGED = 1024;
    private static final long EVENT_FILTER_DELIVERY_FAILED = 64;
    private static final long EVENT_FILTER_DELIVERY_SUCCESS = 32;
    private static final long EVENT_FILTER_MEMORY_AVAILABLE = 256;
    private static final long EVENT_FILTER_MEMORY_FULL = 128;
    private static final long EVENT_FILTER_MESSAGE_DELETED = 2;
    private static final long EVENT_FILTER_MESSAGE_REMOVED = 8192;
    private static final long EVENT_FILTER_MESSAGE_SHIFT = 4;
    private static final long EVENT_FILTER_NEW_MESSAGE = 1;
    private static final long EVENT_FILTER_PARTICIPANT_CHATSTATE_CHANGED = 4096;
    private static final long EVENT_FILTER_PARTICIPANT_PRESENCE_CHANGED = 2048;
    private static final long EVENT_FILTER_READ_STATUS_CHANGED = 512;
    private static final long EVENT_FILTER_SENDING_FAILED = 16;
    private static final long EVENT_FILTER_SENDING_SUCCESS = 8;
    private static final String EVENT_TYPE_CHAT_STATE = "ParticipantChatStateChanged";
    private static final String EVENT_TYPE_CONVERSATION = "ConversationChanged";
    private static final String EVENT_TYPE_DELETE = "MessageDeleted";
    private static final String EVENT_TYPE_DELEVERY_SUCCESS = "DeliverySuccess";
    private static final String EVENT_TYPE_DELIVERY_FAILURE = "DeliveryFailure";
    private static final String EVENT_TYPE_NEW = "NewMessage";
    private static final String EVENT_TYPE_PRESENCE = "ParticipantPresenceChanged";
    private static final String EVENT_TYPE_READ_STATUS = "ReadStatusChanged";
    private static final String EVENT_TYPE_REMOVED = "MessageRemoved";
    private static final String EVENT_TYPE_SENDING_FAILURE = "SendingFailure";
    private static final String EVENT_TYPE_SENDING_SUCCESS = "SendingSuccess";
    private static final String EVENT_TYPE_SHIFT = "MessageShift";
    public static final String EXTRA_MESSAGE_SENT_HANDLE = "HANDLE";
    public static final String EXTRA_MESSAGE_SENT_RESULT = "result";
    public static final String EXTRA_MESSAGE_SENT_RETRY = "retry";
    public static final String EXTRA_MESSAGE_SENT_TIMESTAMP = "timestamp";
    public static final String EXTRA_MESSAGE_SENT_TRANSPARENT = "transparent";
    public static final String EXTRA_MESSAGE_SENT_URI = "uri";
    private static final HashMap<Integer, String> FOLDER_MMS_MAP;
    private static final String[] ID_PROJECTION;
    public static final int MESSAGE_TYPE_RETRIEVE_CONF = 132;
    private static final long PROVIDER_ANR_TIMEOUT = 20000;
    private static final String TAG = "BluetoothMapContentObserver";
    private static final Uri UPDATE_STATUS_URI;
    private static final boolean V = false;
    private static final String[] folderMms;
    private BluetoothMapAccountItem mAccount;
    private String mAuthority;
    private CeBroadcastReceiver mCeBroadcastReceiver;
    private Map<String, BluetoothMapConvoContactElement> mContactList;
    private Uri mContactUri;
    private Context mContext;
    private boolean mEnableSmsMms;
    private int mMasId;
    private BluetoothMapMasInstance mMasInstance;
    private Uri mMessageUri;
    private BluetoothMnsObexClient mMnsClient;
    private Map<Long, Msg> mMsgListMms;
    private Map<Long, Msg> mMsgListMsg;
    private Map<Long, Msg> mMsgListSms;
    private ContentProviderClient mProviderClient;
    private ContentResolver mResolver;
    private SmsBroadcastReceiver mSmsBroadcastReceiver;
    private BluetoothMapUtils.TYPE mSmsType;
    private static final boolean D = BluetoothMapService.DEBUG;
    public static final String EXTRA_MESSAGE_SENT_MSG_TYPE = "type";
    static final String[] SMS_PROJECTION = {"_id", "thread_id", "address", BluetoothMapContract.MessageColumns.BODY, BluetoothMapContract.MessageColumns.DATE, BluetoothMapContract.FILTER_READ_STATUS, EXTRA_MESSAGE_SENT_MSG_TYPE, "status", "locked", "error_code", "sub_id"};
    static final String[] SMS_PROJECTION_SHORT = {"_id", "thread_id", EXTRA_MESSAGE_SENT_MSG_TYPE, BluetoothMapContract.FILTER_READ_STATUS};
    static final String[] SMS_PROJECTION_SHORT_EXT = {"_id", "thread_id", "address", BluetoothMapContract.MessageColumns.BODY, BluetoothMapContract.MessageColumns.DATE, BluetoothMapContract.FILTER_READ_STATUS, EXTRA_MESSAGE_SENT_MSG_TYPE};
    static final String[] MMS_PROJECTION_SHORT = {"_id", "thread_id", "m_type", "msg_box", BluetoothMapContract.FILTER_READ_STATUS};
    static final String[] MMS_PROJECTION_SHORT_EXT = {"_id", "thread_id", "m_type", "msg_box", BluetoothMapContract.FILTER_READ_STATUS, BluetoothMapContract.MessageColumns.DATE, "sub", "pri"};
    static final String[] MSG_PROJECTION_SHORT = {"_id", BluetoothMapContract.MessageColumns.FOLDER_ID, BluetoothMapContract.MessageColumns.FLAG_READ};
    static final String[] MSG_PROJECTION_SHORT_EXT = {"_id", BluetoothMapContract.MessageColumns.FOLDER_ID, BluetoothMapContract.MessageColumns.FLAG_READ, BluetoothMapContract.MessageColumns.DATE, BluetoothMapContract.MessageColumns.SUBJECT, BluetoothMapContract.MessageColumns.FROM_LIST, BluetoothMapContract.MessageColumns.FLAG_HIGH_PRIORITY};
    static final String[] MSG_PROJECTION_SHORT_EXT2 = {"_id", BluetoothMapContract.MessageColumns.FOLDER_ID, BluetoothMapContract.MessageColumns.FLAG_READ, BluetoothMapContract.MessageColumns.DATE, BluetoothMapContract.MessageColumns.SUBJECT, BluetoothMapContract.MessageColumns.FROM_LIST, BluetoothMapContract.MessageColumns.FLAG_HIGH_PRIORITY, "thread_id", "thread_name"};
    private static final HashMap<Integer, String> FOLDER_SMS_MAP = new HashMap<>();
    private boolean mObserverRegistered = false;
    private int mMapSupportedFeatures = 31;
    private int mMapEventReportVersion = 10;
    private BluetoothMapFolderElement mFolders = new BluetoothMapFolderElement("DUMMY", null);
    private boolean mTransmitEvents = true;
    private volatile long mEventFilter = 4294967295L;
    private boolean mStorageUnlocked = false;
    private boolean mInitialized = false;
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) throws Throwable {
            onChange(z, null);
        }

        @Override
        public void onChange(boolean z, Uri uri) throws Throwable {
            if (uri != null) {
                if (!BluetoothMapContentObserver.this.mStorageUnlocked) {
                    Log.v(BluetoothMapContentObserver.TAG, "Ignore events until storage is completely unlocked");
                    return;
                } else if (uri.toString().contains(BluetoothMapContract.TABLE_CONVOCONTACT)) {
                    BluetoothMapContentObserver.this.handleContactListChanges(uri);
                    return;
                } else {
                    BluetoothMapContentObserver.this.handleMsgListChanges(uri);
                    return;
                }
            }
            Log.w(BluetoothMapContentObserver.TAG, "onChange() with URI == null - not handled.");
        }
    };
    private Map<Long, PushMsgInfo> mPushMsgList = Collections.synchronizedMap(new HashMap());
    private PhoneStateListener mPhoneListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.d(BluetoothMapContentObserver.TAG, "Phone service state change: " + serviceState.getState());
            if (serviceState.getState() == 0) {
                BluetoothMapContentObserver.this.resendPendingMessages();
            }
        }
    };

    static {
        FOLDER_SMS_MAP.put(1, BluetoothMapContract.FOLDER_NAME_INBOX);
        FOLDER_SMS_MAP.put(2, BluetoothMapContract.FOLDER_NAME_SENT);
        FOLDER_SMS_MAP.put(3, BluetoothMapContract.FOLDER_NAME_DRAFT);
        FOLDER_SMS_MAP.put(4, BluetoothMapContract.FOLDER_NAME_OUTBOX);
        FOLDER_SMS_MAP.put(5, BluetoothMapContract.FOLDER_NAME_OUTBOX);
        FOLDER_SMS_MAP.put(6, BluetoothMapContract.FOLDER_NAME_OUTBOX);
        FOLDER_MMS_MAP = new HashMap<>();
        FOLDER_MMS_MAP.put(1, BluetoothMapContract.FOLDER_NAME_INBOX);
        FOLDER_MMS_MAP.put(2, BluetoothMapContract.FOLDER_NAME_SENT);
        FOLDER_MMS_MAP.put(3, BluetoothMapContract.FOLDER_NAME_DRAFT);
        FOLDER_MMS_MAP.put(4, BluetoothMapContract.FOLDER_NAME_OUTBOX);
        folderMms = new String[]{"", BluetoothMapContract.FOLDER_NAME_INBOX, BluetoothMapContract.FOLDER_NAME_SENT, BluetoothMapContract.FOLDER_NAME_DRAFT, BluetoothMapContract.FOLDER_NAME_OUTBOX, BluetoothMapContract.FOLDER_NAME_OUTBOX};
        ID_PROJECTION = new String[]{"_id"};
        UPDATE_STATUS_URI = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "/status");
    }

    public BluetoothMapContentObserver(Context context, BluetoothMnsObexClient bluetoothMnsObexClient, BluetoothMapMasInstance bluetoothMapMasInstance, BluetoothMapAccountItem bluetoothMapAccountItem, boolean z) throws RemoteException {
        boolean z2;
        this.mProviderClient = null;
        this.mMasInstance = null;
        this.mEnableSmsMms = false;
        this.mAuthority = null;
        this.mMessageUri = null;
        this.mContactUri = null;
        this.mSmsBroadcastReceiver = new SmsBroadcastReceiver();
        this.mCeBroadcastReceiver = new CeBroadcastReceiver();
        this.mMsgListSms = null;
        this.mMsgListMms = null;
        this.mMsgListMsg = null;
        this.mContactList = null;
        this.mContext = context;
        this.mResolver = this.mContext.getContentResolver();
        this.mAccount = bluetoothMapAccountItem;
        this.mMasInstance = bluetoothMapMasInstance;
        this.mMasId = this.mMasInstance.getMasId();
        setObserverRemoteFeatureMask(this.mMasInstance.getRemoteFeatureMask());
        if (bluetoothMapAccountItem != null) {
            this.mAuthority = Uri.parse(bluetoothMapAccountItem.mBase_uri).getAuthority();
            this.mMessageUri = Uri.parse(bluetoothMapAccountItem.mBase_uri + "/" + BluetoothMapContract.TABLE_MESSAGE);
            if (this.mAccount.getType() == BluetoothMapUtils.TYPE.IM) {
                this.mContactUri = Uri.parse(bluetoothMapAccountItem.mBase_uri + "/" + BluetoothMapContract.TABLE_CONVOCONTACT);
            }
            this.mProviderClient = this.mResolver.acquireUnstableContentProviderClient(this.mAuthority);
            if (this.mProviderClient == null) {
                throw new RemoteException("Failed to acquire provider for " + this.mAuthority);
            }
            this.mProviderClient.setDetectNotResponding(PROVIDER_ANR_TIMEOUT);
            this.mContactList = this.mMasInstance.getContactList();
            if (this.mContactList == null) {
                setContactList(new HashMap(), false);
                initContactsList();
            }
        }
        this.mEnableSmsMms = z;
        this.mSmsType = getSmsType();
        this.mMnsClient = bluetoothMnsObexClient;
        this.mMsgListSms = this.mMasInstance.getMsgListSms();
        if (!this.mEnableSmsMms) {
            z2 = false;
        } else {
            if (this.mMsgListSms == null) {
                setMsgListSms(new HashMap(), false);
                z2 = true;
            } else {
                z2 = false;
            }
            this.mMsgListMms = this.mMasInstance.getMsgListMms();
            if (this.mMsgListMms == null) {
                setMsgListMms(new HashMap(), false);
                z2 = true;
            }
        }
        if (this.mAccount != null) {
            this.mMsgListMsg = this.mMasInstance.getMsgListMsg();
            if (this.mMsgListMsg == null) {
                setMsgListMsg(new HashMap(), false);
                z2 = true;
            }
        }
        if (z2) {
            initMsgList();
        }
    }

    public int getObserverRemoteFeatureMask() {
        return this.mMapSupportedFeatures;
    }

    public void setObserverRemoteFeatureMask(int i) {
        this.mMapSupportedFeatures = i & 127;
        if ((this.mMapSupportedFeatures & 64) != 0) {
            this.mMapEventReportVersion = 11;
        }
        if ((128 & this.mMapSupportedFeatures) != 0) {
            this.mMapEventReportVersion = 12;
        } else if ((49152 & this.mMapSupportedFeatures) != 0) {
            Log.w(TAG, "setObserverRemoteFeatureMask: Extended Event Reports 1.2 is not set eventhough PARTICIPANT_PRESENCE_CHANGE_BIT or PARTICIPANT_CHAT_STATE_CHANGE_BIT were set, mMapSupportedFeatures=" + this.mMapSupportedFeatures);
        }
        if (D) {
            Log.d(TAG, "setObserverRemoteFeatureMask: mMapEventReportVersion=" + this.mMapEventReportVersion + " mMapSupportedFeatures=" + this.mMapSupportedFeatures);
        }
    }

    private Map<Long, Msg> getMsgListSms() {
        return this.mMsgListSms;
    }

    private void setMsgListSms(Map<Long, Msg> map, boolean z) {
        this.mMsgListSms = map;
        if (z) {
            this.mMasInstance.updateFolderVersionCounter();
        }
        this.mMasInstance.setMsgListSms(map);
    }

    private Map<Long, Msg> getMsgListMms() {
        return this.mMsgListMms;
    }

    private void setMsgListMms(Map<Long, Msg> map, boolean z) {
        this.mMsgListMms = map;
        if (z) {
            this.mMasInstance.updateFolderVersionCounter();
        }
        this.mMasInstance.setMsgListMms(map);
    }

    private Map<Long, Msg> getMsgListMsg() {
        return this.mMsgListMsg;
    }

    private void setMsgListMsg(Map<Long, Msg> map, boolean z) {
        this.mMsgListMsg = map;
        if (z) {
            this.mMasInstance.updateFolderVersionCounter();
        }
        this.mMasInstance.setMsgListMsg(map);
    }

    private Map<String, BluetoothMapConvoContactElement> getContactList() {
        return this.mContactList;
    }

    private void setContactList(Map<String, BluetoothMapConvoContactElement> map, boolean z) {
        this.mContactList = map;
        if (z) {
            this.mMasInstance.updateImEmailConvoListVersionCounter();
        }
        this.mMasInstance.setContactList(map);
    }

    private static boolean sendEventNewMessage(long j) {
        return (j & 1) > 0;
    }

    private static boolean sendEventMessageDeleted(long j) {
        return (j & 2) > 0;
    }

    private static boolean sendEventMessageShift(long j) {
        return (j & 4) > 0;
    }

    private static boolean sendEventSendingSuccess(long j) {
        return (j & 8) > 0;
    }

    private static boolean sendEventSendingFailed(long j) {
        return (j & 16) > 0;
    }

    private static boolean sendEventDeliverySuccess(long j) {
        return (j & 32) > 0;
    }

    private static boolean sendEventDeliveryFailed(long j) {
        return (j & 64) > 0;
    }

    private static boolean sendEventReadStatusChanged(long j) {
        return (j & 512) > 0;
    }

    private static boolean sendEventConversationChanged(long j) {
        return (j & 1024) > 0;
    }

    private static boolean sendEventParticipantPresenceChanged(long j) {
        return (j & 2048) > 0;
    }

    private static boolean sendEventParticipantChatstateChanged(long j) {
        return (j & 4096) > 0;
    }

    private static boolean sendEventMessageRemoved(long j) {
        return (j & 8192) > 0;
    }

    private BluetoothMapUtils.TYPE getSmsType() {
        if (((TelephonyManager) this.mContext.getSystemService("phone")).getPhoneType() == 2) {
            return BluetoothMapUtils.TYPE.SMS_CDMA;
        }
        return BluetoothMapUtils.TYPE.SMS_GSM;
    }

    private static String getSmsFolderName(int i) {
        String str = FOLDER_SMS_MAP.get(Integer.valueOf(i));
        if (str != null) {
            return str;
        }
        Log.e(TAG, "New SMS mailbox types have been introduced, without an update in BT...");
        return "Unknown";
    }

    private static String getMmsFolderName(int i) {
        String str = FOLDER_MMS_MAP.get(Integer.valueOf(i));
        if (str != null) {
            return str;
        }
        Log.e(TAG, "New MMS mailboxes have been introduced, without an update in BT...");
        return "Unknown";
    }

    public void setFolderStructure(BluetoothMapFolderElement bluetoothMapFolderElement) {
        this.mFolders = bluetoothMapFolderElement;
    }

    private class ConvoContactInfo {
        public int mContactColBtUid;
        public int mContactColChatState;
        public int mContactColContactId;
        public int mContactColConvoId;
        public int mContactColLastActive;
        public int mContactColLastOnline;
        public int mContactColName;
        public int mContactColNickname;
        public int mContactColPresenceState;
        public int mContactColPresenceText;
        public int mContactColPriority;
        public int mContactColUci;
        public int mConvoColConvoId;
        public int mConvoColLastActivity;
        public int mConvoColName;

        private ConvoContactInfo() {
            this.mConvoColConvoId = -1;
            this.mConvoColLastActivity = -1;
            this.mConvoColName = -1;
            this.mContactColUci = -1;
            this.mContactColConvoId = -1;
            this.mContactColName = -1;
            this.mContactColNickname = -1;
            this.mContactColBtUid = -1;
            this.mContactColChatState = -1;
            this.mContactColContactId = -1;
            this.mContactColLastActive = -1;
            this.mContactColPresenceState = -1;
            this.mContactColPresenceText = -1;
            this.mContactColPriority = -1;
            this.mContactColLastOnline = -1;
        }

        public void setConvoColunms(Cursor cursor) {
            this.mContactColConvoId = cursor.getColumnIndex(BluetoothMapContract.ConvoContactColumns.CONVO_ID);
            this.mContactColName = cursor.getColumnIndex("name");
            this.mContactColNickname = cursor.getColumnIndex(BluetoothMapContract.ConvoContactColumns.NICKNAME);
            this.mContactColBtUid = cursor.getColumnIndex(BluetoothMapContract.ConvoContactColumns.X_BT_UID);
            this.mContactColChatState = cursor.getColumnIndex(BluetoothMapContract.ChatStatusColumns.CHAT_STATE);
            this.mContactColUci = cursor.getColumnIndex(BluetoothMapContract.ConvoContactColumns.UCI);
            this.mContactColNickname = cursor.getColumnIndex(BluetoothMapContract.ConvoContactColumns.NICKNAME);
            this.mContactColLastActive = cursor.getColumnIndex(BluetoothMapContract.ChatStatusColumns.LAST_ACTIVE);
            this.mContactColName = cursor.getColumnIndex("name");
            this.mContactColPresenceState = cursor.getColumnIndex(BluetoothMapContract.PresenceColumns.PRESENCE_STATE);
            this.mContactColPresenceText = cursor.getColumnIndex(BluetoothMapContract.PresenceColumns.STATUS_TEXT);
            this.mContactColPriority = cursor.getColumnIndex(BluetoothMapContract.PresenceColumns.PRIORITY);
            this.mContactColLastOnline = cursor.getColumnIndex(BluetoothMapContract.PresenceColumns.LAST_ONLINE);
        }
    }

    private class Event {
        static final String PATH = "telecom/msg/";
        public int chatState;
        public long conversationID;
        public String conversationName;
        public String datetime;
        public String eventType;
        public String folder;
        public long handle;
        public BluetoothMapUtils.TYPE msgType;
        public String oldFolder;
        public int presenceState;
        public String presenceStatus;
        public String priority;
        public String senderName;
        public String subject;
        public String uci;

        private void setFolderPath(String str, BluetoothMapUtils.TYPE type) {
            if (str != null) {
                if (type == BluetoothMapUtils.TYPE.EMAIL || type == BluetoothMapUtils.TYPE.IM) {
                    this.folder = str;
                    return;
                }
                this.folder = PATH + str;
                return;
            }
            this.folder = null;
        }

        Event(String str, long j, String str2, String str3, BluetoothMapUtils.TYPE type) {
            this.folder = null;
            this.oldFolder = null;
            this.datetime = null;
            this.uci = null;
            this.subject = null;
            this.senderName = null;
            this.priority = null;
            this.conversationName = null;
            this.conversationID = -1L;
            this.presenceState = 0;
            this.presenceStatus = null;
            this.chatState = 0;
            this.eventType = str;
            this.handle = j;
            setFolderPath(str2, type);
            if (str3 != null) {
                if (type == BluetoothMapUtils.TYPE.EMAIL || type == BluetoothMapUtils.TYPE.IM) {
                    this.oldFolder = str3;
                } else {
                    this.oldFolder = PATH + str3;
                }
            } else {
                this.oldFolder = null;
            }
            this.msgType = type;
        }

        Event(String str, long j, String str2, BluetoothMapUtils.TYPE type) {
            this.folder = null;
            this.oldFolder = null;
            this.datetime = null;
            this.uci = null;
            this.subject = null;
            this.senderName = null;
            this.priority = null;
            this.conversationName = null;
            this.conversationID = -1L;
            this.presenceState = 0;
            this.presenceStatus = null;
            this.chatState = 0;
            this.eventType = str;
            this.handle = j;
            setFolderPath(str2, type);
            this.msgType = type;
        }

        Event(String str, long j, String str2, BluetoothMapUtils.TYPE type, String str3, String str4, String str5, String str6) {
            this.folder = null;
            this.oldFolder = null;
            this.datetime = null;
            this.uci = null;
            this.subject = null;
            this.senderName = null;
            this.priority = null;
            this.conversationName = null;
            this.conversationID = -1L;
            this.presenceState = 0;
            this.presenceStatus = null;
            this.chatState = 0;
            this.eventType = str;
            this.handle = j;
            setFolderPath(str2, type);
            this.msgType = type;
            this.datetime = str3;
            if (str4 != null) {
                this.subject = BluetoothMapUtils.stripInvalidChars(str4);
            }
            if (str5 != null) {
                this.senderName = BluetoothMapUtils.stripInvalidChars(str5);
            }
            this.priority = str6;
        }

        Event(String str, long j, String str2, BluetoothMapUtils.TYPE type, String str3, String str4, String str5, String str6, long j2, String str7) {
            this.folder = null;
            this.oldFolder = null;
            this.datetime = null;
            this.uci = null;
            this.subject = null;
            this.senderName = null;
            this.priority = null;
            this.conversationName = null;
            this.conversationID = -1L;
            this.presenceState = 0;
            this.presenceStatus = null;
            this.chatState = 0;
            this.eventType = str;
            this.handle = j;
            setFolderPath(str2, type);
            this.msgType = type;
            this.datetime = str3;
            if (str4 != null) {
                this.subject = BluetoothMapUtils.stripInvalidChars(str4);
            }
            if (str5 != null) {
                this.senderName = BluetoothMapUtils.stripInvalidChars(str5);
            }
            if (j2 != 0) {
                this.conversationID = j2;
            }
            if (str7 != null) {
                this.conversationName = BluetoothMapUtils.stripInvalidChars(str7);
            }
            this.priority = str6;
        }

        Event(String str, String str2, BluetoothMapUtils.TYPE type, String str3, String str4, String str5, long j, String str6, int i, String str7, int i2) {
            this.folder = null;
            this.oldFolder = null;
            this.datetime = null;
            this.uci = null;
            this.subject = null;
            this.senderName = null;
            this.priority = null;
            this.conversationName = null;
            this.conversationID = -1L;
            this.presenceState = 0;
            this.presenceStatus = null;
            this.chatState = 0;
            this.eventType = str;
            this.uci = str2;
            this.msgType = type;
            if (str3 != null) {
                this.senderName = BluetoothMapUtils.stripInvalidChars(str3);
            }
            this.priority = str4;
            this.datetime = str5;
            if (j != 0) {
                this.conversationID = j;
            }
            if (str6 != null) {
                this.conversationName = BluetoothMapUtils.stripInvalidChars(str6);
            }
            if (i != 0) {
                this.presenceState = i;
            }
            if (str7 != null) {
                this.presenceStatus = BluetoothMapUtils.stripInvalidChars(str7);
            }
            if (i2 != 0) {
                this.chatState = i2;
            }
        }

        public byte[] encode() throws UnsupportedEncodingException {
            StringWriter stringWriter = new StringWriter();
            XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
            try {
                xmlSerializerNewSerializer.setOutput(stringWriter);
                xmlSerializerNewSerializer.startDocument("UTF-8", true);
                xmlSerializerNewSerializer.text(VCardBuilder.VCARD_END_OF_LINE);
                xmlSerializerNewSerializer.startTag("", "MAP-event-report");
                if (BluetoothMapContentObserver.this.mMapEventReportVersion != 12) {
                    if (BluetoothMapContentObserver.this.mMapEventReportVersion == 11) {
                        xmlSerializerNewSerializer.attribute("", "version", "1.1");
                    } else {
                        xmlSerializerNewSerializer.attribute("", "version", "1.0");
                    }
                } else {
                    xmlSerializerNewSerializer.attribute("", "version", "1.2");
                }
                xmlSerializerNewSerializer.startTag("", NotificationCompat.CATEGORY_EVENT);
                xmlSerializerNewSerializer.attribute("", BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE, this.eventType);
                if (this.eventType.equals(BluetoothMapContentObserver.EVENT_TYPE_CONVERSATION) || this.eventType.equals(BluetoothMapContentObserver.EVENT_TYPE_PRESENCE) || this.eventType.equals(BluetoothMapContentObserver.EVENT_TYPE_CHAT_STATE)) {
                    xmlSerializerNewSerializer.attribute("", "participant_uci", this.uci);
                } else {
                    xmlSerializerNewSerializer.attribute("", "handle", BluetoothMapUtils.getMapHandle(this.handle, this.msgType));
                }
                if (this.folder != null) {
                    xmlSerializerNewSerializer.attribute("", "folder", this.folder);
                }
                if (this.oldFolder != null) {
                    xmlSerializerNewSerializer.attribute("", "old_folder", this.oldFolder);
                }
                if (this.msgType != null) {
                    xmlSerializerNewSerializer.attribute("", "msg_type", this.msgType.name());
                }
                if (this.datetime != null) {
                    xmlSerializerNewSerializer.attribute("", "datetime", this.datetime);
                }
                if (this.subject != null) {
                    xmlSerializerNewSerializer.attribute("", BluetoothMapContract.MessageColumns.SUBJECT, this.subject.substring(0, this.subject.length() < 256 ? this.subject.length() : 256));
                }
                if (this.senderName != null) {
                    xmlSerializerNewSerializer.attribute("", "sender_name", this.senderName.substring(0, this.senderName.length() < 256 ? this.senderName.length() : 255));
                }
                if (this.priority != null) {
                    xmlSerializerNewSerializer.attribute("", BluetoothMapContract.PresenceColumns.PRIORITY, this.priority);
                }
                if (BluetoothMapContentObserver.this.mMapEventReportVersion > 11) {
                    if (this.conversationName != null) {
                        xmlSerializerNewSerializer.attribute("", "conversation_name", this.conversationName);
                    }
                    if (this.conversationID != -1) {
                        xmlSerializerNewSerializer.attribute("", "conversation_id", BluetoothMapUtils.getMapConvoHandle(this.conversationID, this.msgType));
                    }
                    if (this.eventType.equals(BluetoothMapContentObserver.EVENT_TYPE_PRESENCE)) {
                        if (this.presenceState != 0) {
                            xmlSerializerNewSerializer.attribute("", "presence_availability", String.valueOf(this.presenceState));
                        }
                        if (this.presenceStatus != null) {
                            xmlSerializerNewSerializer.attribute("", "presence_status", this.presenceStatus.substring(0, this.presenceStatus.length() < 256 ? this.subject.length() : 256));
                        }
                    }
                    if (this.eventType.equals(BluetoothMapContentObserver.EVENT_TYPE_PRESENCE) && this.chatState != 0) {
                        xmlSerializerNewSerializer.attribute("", BluetoothMapContract.ChatStatusColumns.CHAT_STATE, String.valueOf(this.chatState));
                    }
                }
                xmlSerializerNewSerializer.endTag("", NotificationCompat.CATEGORY_EVENT);
                xmlSerializerNewSerializer.endTag("", "MAP-event-report");
                xmlSerializerNewSerializer.endDocument();
            } catch (IOException e) {
                if (BluetoothMapContentObserver.D) {
                    Log.w(BluetoothMapContentObserver.TAG, e);
                }
            } catch (IllegalArgumentException e2) {
                if (BluetoothMapContentObserver.D) {
                    Log.w(BluetoothMapContentObserver.TAG, e2);
                }
            } catch (IllegalStateException e3) {
                if (BluetoothMapContentObserver.D) {
                    Log.w(BluetoothMapContentObserver.TAG, e3);
                }
            }
            return stringWriter.toString().getBytes("UTF-8");
        }
    }

    class Msg {
        public int flagRead;
        public long folderId;
        public long id;
        public boolean localInitiatedSend;
        public long oldFolderId;
        public int threadId;
        public boolean transparent;
        public int type;

        Msg(long j, int i, int i2, int i3) {
            this.folderId = -1L;
            this.oldFolderId = -1L;
            this.localInitiatedSend = false;
            this.transparent = false;
            this.flagRead = -1;
            this.id = j;
            this.type = i;
            this.threadId = i2;
            this.flagRead = i3;
        }

        Msg(long j, long j2, int i) {
            this.folderId = -1L;
            this.oldFolderId = -1L;
            this.localInitiatedSend = false;
            this.transparent = false;
            this.flagRead = -1;
            this.id = j;
            this.folderId = j2;
            this.flagRead = i;
        }

        public int hashCode() {
            return 31 + ((int) (this.id ^ (this.id >>> 32)));
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass() && this.id == ((Msg) obj).id) {
                return true;
            }
            return false;
        }
    }

    public int setNotificationRegistration(int i) throws RemoteException {
        if (D) {
            Log.d(TAG, "setNotificationRegistration() enter");
        }
        if (this.mMnsClient == null) {
            return 211;
        }
        Handler messageHandler = this.mMnsClient.getMessageHandler();
        if (messageHandler != null) {
            Message messageObtainMessage = messageHandler.obtainMessage();
            if (this.mMnsClient.isValidMnsRecord()) {
                messageObtainMessage.what = 1;
            } else {
                messageObtainMessage.what = 3;
                if (this.mMnsClient.mMnsLstRegRqst != null && this.mMnsClient.mMnsLstRegRqst.isSearchInProgress()) {
                    if (i == 1) {
                        return 211;
                    }
                    messageObtainMessage.what = 1;
                }
            }
            messageObtainMessage.arg1 = this.mMasId;
            messageObtainMessage.arg2 = i;
            messageHandler.sendMessageDelayed(messageObtainMessage, 10L);
            if (D) {
                Log.d(TAG, "setNotificationRegistration() send : " + messageObtainMessage.what + " to MNS ");
                return 160;
            }
            return 160;
        }
        if (D) {
            Log.d(TAG, "setNotificationRegistration() Unable to send registration request");
        }
        return 211;
    }

    boolean eventMaskContainsContacts(long j) {
        return sendEventParticipantPresenceChanged(j);
    }

    boolean eventMaskContainsCovo(long j) {
        return sendEventConversationChanged(j) || sendEventParticipantChatstateChanged(j);
    }

    synchronized void setNotificationFilter(long j) {
        long j2 = this.mEventFilter;
        this.mEventFilter = j;
        if (!eventMaskContainsContacts(j2)) {
            eventMaskContainsContacts(j);
        }
        if (!eventMaskContainsCovo(j2)) {
            eventMaskContainsCovo(j);
        }
    }

    public void registerObserver() throws RemoteException {
        if (this.mObserverRegistered) {
            return;
        }
        if (this.mAccount != null) {
            this.mProviderClient = this.mResolver.acquireUnstableContentProviderClient(this.mAuthority);
            if (this.mProviderClient == null) {
                throw new RemoteException("Failed to acquire provider for " + this.mAuthority);
            }
            this.mProviderClient.setDetectNotResponding(PROVIDER_ANR_TIMEOUT);
            if (this.mAccount.getType() == BluetoothMapUtils.TYPE.IM) {
                initContactsList();
            }
        }
        initMsgList();
        if (this.mEnableSmsMms) {
            this.mResolver.registerContentObserver(Telephony.MmsSms.CONTENT_URI, false, this.mObserver);
            this.mObserverRegistered = true;
        }
        if (this.mAccount != null) {
            Uri uri = Uri.parse(this.mAccount.mBase_uri_no_account + "/" + BluetoothMapContract.TABLE_MESSAGE);
            if (D) {
                Log.d(TAG, "Registering observer for: " + uri);
            }
            this.mResolver.registerContentObserver(uri, true, this.mObserver);
            Uri uri2 = Uri.parse(this.mAccount.mBase_uri + "/" + BluetoothMapContract.TABLE_MESSAGE);
            if (D) {
                Log.d(TAG, "Registering observer for: " + uri2);
            }
            this.mResolver.registerContentObserver(uri2, true, this.mObserver);
            if (this.mAccount.getType() == BluetoothMapUtils.TYPE.IM) {
                Uri uri3 = Uri.parse(this.mAccount.mBase_uri_no_account + "/" + BluetoothMapContract.TABLE_CONVOCONTACT);
                if (D) {
                    Log.d(TAG, "Registering observer for: " + uri3);
                }
                this.mResolver.registerContentObserver(uri3, true, this.mObserver);
                Uri uri4 = Uri.parse(this.mAccount.mBase_uri + "/" + BluetoothMapContract.TABLE_CONVOCONTACT);
                if (D) {
                    Log.d(TAG, "Registering observer for: " + uri4);
                }
                this.mResolver.registerContentObserver(uri4, true, this.mObserver);
            }
            this.mObserverRegistered = true;
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

    void refreshFolderVersionCounter() {
        if (this.mObserverRegistered) {
            return;
        }
        this.mTransmitEvents = false;
        try {
            if (this.mEnableSmsMms) {
                handleMsgListChangesSms();
                handleMsgListChangesMms();
            }
            if (this.mAccount != null) {
                try {
                    handleMsgListChangesMsg(this.mMessageUri);
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to update FolderVersionCounter. - Not fatal, but can cause undesirable user experience!", e);
                }
            }
        } finally {
            this.mTransmitEvents = true;
        }
    }

    void refreshConvoListVersionCounter() {
        if (this.mObserverRegistered) {
            return;
        }
        this.mTransmitEvents = false;
        try {
            if (this.mAccount != null && this.mContactUri != null) {
                handleContactListChanges(this.mContactUri);
            }
        } finally {
            this.mTransmitEvents = true;
        }
    }

    private void sendEvent(Event event) {
        if (!this.mTransmitEvents) {
            return;
        }
        if (D) {
            Log.d(TAG, "sendEvent: " + event.eventType + " " + event.handle + " " + event.folder + " " + event.oldFolder + " " + event.msgType.name() + " " + event.datetime + " " + event.subject + " " + event.senderName + " " + event.priority);
        }
        if (this.mMnsClient == null || !this.mMnsClient.isConnected()) {
            Log.d(TAG, "sendEvent: No MNS client registered or connected- don't send event");
            return;
        }
        long j = this.mEventFilter;
        if (Objects.equals(event.eventType, EVENT_TYPE_NEW)) {
            if (!sendEventNewMessage(j)) {
                if (D) {
                    Log.d(TAG, "Skip sending event of type: " + event.eventType);
                    return;
                }
                return;
            }
        } else if (Objects.equals(event.eventType, EVENT_TYPE_DELETE)) {
            if (!sendEventMessageDeleted(j)) {
                if (D) {
                    Log.d(TAG, "Skip sending event of type: " + event.eventType);
                    return;
                }
                return;
            }
        } else if (Objects.equals(event.eventType, EVENT_TYPE_REMOVED)) {
            if (!sendEventMessageRemoved(j)) {
                if (D) {
                    Log.d(TAG, "Skip sending event of type: " + event.eventType);
                    return;
                }
                return;
            }
        } else if (Objects.equals(event.eventType, EVENT_TYPE_SHIFT)) {
            if (!sendEventMessageShift(j)) {
                if (D) {
                    Log.d(TAG, "Skip sending event of type: " + event.eventType);
                    return;
                }
                return;
            }
        } else if (Objects.equals(event.eventType, EVENT_TYPE_DELEVERY_SUCCESS)) {
            if (!sendEventDeliverySuccess(j)) {
                if (D) {
                    Log.d(TAG, "Skip sending event of type: " + event.eventType);
                    return;
                }
                return;
            }
        } else if (Objects.equals(event.eventType, EVENT_TYPE_SENDING_SUCCESS)) {
            if (!sendEventSendingSuccess(j)) {
                if (D) {
                    Log.d(TAG, "Skip sending event of type: " + event.eventType);
                    return;
                }
                return;
            }
        } else if (Objects.equals(event.eventType, EVENT_TYPE_SENDING_FAILURE)) {
            if (!sendEventSendingFailed(j)) {
                if (D) {
                    Log.d(TAG, "Skip sending event of type: " + event.eventType);
                    return;
                }
                return;
            }
        } else if (Objects.equals(event.eventType, EVENT_TYPE_DELIVERY_FAILURE)) {
            if (!sendEventDeliveryFailed(j)) {
                if (D) {
                    Log.d(TAG, "Skip sending event of type: " + event.eventType);
                    return;
                }
                return;
            }
        } else if (Objects.equals(event.eventType, EVENT_TYPE_READ_STATUS)) {
            if (!sendEventReadStatusChanged(j)) {
                if (D) {
                    Log.d(TAG, "Skip sending event of type: " + event.eventType);
                    return;
                }
                return;
            }
        } else if (Objects.equals(event.eventType, EVENT_TYPE_CONVERSATION)) {
            if (!sendEventConversationChanged(j)) {
                if (D) {
                    Log.d(TAG, "Skip sending event of type: " + event.eventType);
                    return;
                }
                return;
            }
        } else if (Objects.equals(event.eventType, EVENT_TYPE_PRESENCE)) {
            if (!sendEventParticipantPresenceChanged(j)) {
                if (D) {
                    Log.d(TAG, "Skip sending event of type: " + event.eventType);
                    return;
                }
                return;
            }
        } else if (Objects.equals(event.eventType, EVENT_TYPE_CHAT_STATE) && !sendEventParticipantChatstateChanged(j)) {
            if (D) {
                Log.d(TAG, "Skip sending event of type: " + event.eventType);
                return;
            }
            return;
        }
        try {
            this.mMnsClient.sendEvent(event.encode(), this.mMasId);
        } catch (UnsupportedEncodingException e) {
            if (D) {
                Log.e(TAG, "Exception - should not happen: ", e);
            }
        }
    }

    private void initMsgList() throws RemoteException {
        UserManager userManager = UserManager.get(this.mContext);
        if (userManager == null || !userManager.isUserUnlocked()) {
            return;
        }
        if (this.mEnableSmsMms) {
            HashMap map = new HashMap();
            try {
                Cursor cursorQuery = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION_SHORT, null, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            do {
                                long j = cursorQuery.getLong(cursorQuery.getColumnIndex("_id"));
                                map.put(Long.valueOf(j), new Msg(j, cursorQuery.getInt(cursorQuery.getColumnIndex(EXTRA_MESSAGE_SENT_MSG_TYPE)), cursorQuery.getInt(cursorQuery.getColumnIndex("thread_id")), cursorQuery.getInt(cursorQuery.getColumnIndex(BluetoothMapContract.FILTER_READ_STATUS))));
                            } while (cursorQuery.moveToNext());
                        }
                    } finally {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                synchronized (getMsgListSms()) {
                    getMsgListSms().clear();
                    setMsgListSms(map, true);
                }
                HashMap map2 = new HashMap();
                cursorQuery = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION_SHORT, null, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            do {
                                long j2 = cursorQuery.getLong(cursorQuery.getColumnIndex("_id"));
                                map2.put(Long.valueOf(j2), new Msg(j2, cursorQuery.getInt(cursorQuery.getColumnIndex("msg_box")), cursorQuery.getInt(cursorQuery.getColumnIndex("thread_id")), cursorQuery.getInt(cursorQuery.getColumnIndex(BluetoothMapContract.FILTER_READ_STATUS))));
                            } while (cursorQuery.moveToNext());
                        }
                    } finally {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    }
                }
                synchronized (getMsgListMms()) {
                    getMsgListMms().clear();
                    setMsgListMms(map2, true);
                }
            } catch (SQLiteException e) {
                Log.e(TAG, "Failed to initialize the list of messages: " + e.toString());
                return;
            }
        }
        if (this.mAccount != null) {
            HashMap map3 = new HashMap();
            Cursor cursorQuery2 = this.mProviderClient.query(this.mMessageUri, MSG_PROJECTION_SHORT, null, null, null);
            if (cursorQuery2 != null) {
                try {
                    if (cursorQuery2.moveToFirst()) {
                        do {
                            long j3 = cursorQuery2.getLong(cursorQuery2.getColumnIndex("_id"));
                            map3.put(Long.valueOf(j3), new Msg(j3, cursorQuery2.getInt(cursorQuery2.getColumnIndex(BluetoothMapContract.MessageColumns.FOLDER_ID)), cursorQuery2.getInt(cursorQuery2.getColumnIndex(BluetoothMapContract.MessageColumns.FLAG_READ))));
                        } while (cursorQuery2.moveToNext());
                    }
                } finally {
                    if (cursorQuery2 != null) {
                        cursorQuery2.close();
                    }
                }
            }
            synchronized (getMsgListMsg()) {
                getMsgListMsg().clear();
                setMsgListMsg(map3, true);
            }
        }
    }

    private void initContactsList() throws RemoteException {
        ConvoContactInfo convoContactInfo;
        if (this.mContactUri == null) {
            if (D) {
                Log.d(TAG, "initContactsList() no mContactUri - nothing to init");
                return;
            }
            return;
        }
        Cursor cursorQuery = this.mProviderClient.query(this.mContactUri, BluetoothMapContract.BT_CONTACT_CHATSTATE_PRESENCE_PROJECTION, null, null, null);
        HashMap map = new HashMap();
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    ConvoContactInfo convoContactInfo2 = new ConvoContactInfo();
                    convoContactInfo2.setConvoColunms(cursorQuery);
                    while (true) {
                        if (cursorQuery.getLong(convoContactInfo2.mContactColConvoId) != 0) {
                            String string = cursorQuery.getString(convoContactInfo2.mContactColUci);
                            convoContactInfo = convoContactInfo2;
                            map.put(string, new BluetoothMapConvoContactElement(string, cursorQuery.getString(convoContactInfo2.mContactColName), cursorQuery.getString(convoContactInfo2.mContactColNickname), cursorQuery.getString(convoContactInfo2.mContactColPresenceText), cursorQuery.getInt(convoContactInfo2.mContactColPresenceState), cursorQuery.getLong(convoContactInfo2.mContactColLastActive), cursorQuery.getInt(convoContactInfo2.mContactColChatState), cursorQuery.getInt(convoContactInfo2.mContactColPriority), cursorQuery.getString(convoContactInfo2.mContactColBtUid)));
                        } else {
                            convoContactInfo = convoContactInfo2;
                        }
                        if (!cursorQuery.moveToNext()) {
                            break;
                        } else {
                            convoContactInfo2 = convoContactInfo;
                        }
                    }
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        synchronized (getContactList()) {
            getContactList().clear();
            setContactList(map, true);
        }
    }

    private void handleMsgListChangesSms() {
        Cursor cursorQuery;
        Cursor cursor;
        Cursor cursor2;
        boolean z;
        Msg msg;
        int i;
        int i2;
        boolean z2;
        HashMap map;
        HashMap map2;
        int i3;
        Event event;
        String line1AlphaTag;
        String str;
        Event event2;
        HashMap map3 = new HashMap();
        synchronized (getMsgListSms()) {
            int i4 = 10;
            if (this.mMapEventReportVersion == 10) {
                cursorQuery = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION_SHORT, null, null, null);
            } else {
                cursorQuery = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION_SHORT_EXT, null, null, null);
            }
            Cursor cursor3 = cursorQuery;
            boolean z3 = false;
            if (cursor3 != null) {
                try {
                    if (cursor3.moveToFirst()) {
                        while (true) {
                            long j = cursor3.getLong(cursor3.getColumnIndex("_id"));
                            int i5 = cursor3.getInt(cursor3.getColumnIndex(EXTRA_MESSAGE_SENT_MSG_TYPE));
                            int i6 = cursor3.getInt(cursor3.getColumnIndex("thread_id"));
                            int i7 = cursor3.getInt(cursor3.getColumnIndex(BluetoothMapContract.FILTER_READ_STATUS));
                            Msg msgRemove = getMsgListSms().remove(Long.valueOf(j));
                            if (msgRemove == null) {
                                map3.put(Long.valueOf(j), new Msg(j, i5, i6, i7));
                                if (this.mTransmitEvents && this.mMapEventReportVersion > i4) {
                                    String dateTimeString = BluetoothMapUtils.getDateTimeString(cursor3.getLong(cursor3.getColumnIndex(BluetoothMapContract.MessageColumns.DATE)));
                                    String string = cursor3.getString(cursor3.getColumnIndex(BluetoothMapContract.MessageColumns.BODY));
                                    if (string == null) {
                                        string = "";
                                    }
                                    String str2 = string;
                                    String line1Number = "";
                                    if (i5 == 1) {
                                        line1Number = cursor3.getString(cursor3.getColumnIndex("address"));
                                        if (line1Number != null && !line1Number.isEmpty()) {
                                            line1AlphaTag = BluetoothMapContent.getContactNameFromPhone(line1Number, this.mResolver);
                                            if (line1AlphaTag == null || line1AlphaTag.isEmpty()) {
                                            }
                                            str = line1AlphaTag;
                                        }
                                        str = line1Number;
                                    } else {
                                        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
                                        if (telephonyManager != null) {
                                            line1Number = telephonyManager.getLine1Number();
                                            line1AlphaTag = telephonyManager.getLine1AlphaTag();
                                            if (line1AlphaTag != null) {
                                                if (line1AlphaTag.isEmpty()) {
                                                }
                                                str = line1AlphaTag;
                                            }
                                        }
                                        str = line1Number;
                                    }
                                    if (this.mMapEventReportVersion == 11) {
                                        event2 = new Event(EVENT_TYPE_NEW, j, getSmsFolderName(i5), this.mSmsType, dateTimeString, str2, str, "no");
                                        cursor2 = cursor3;
                                        map2 = map3;
                                        z = true;
                                        i3 = i4;
                                    } else {
                                        String str3 = str;
                                        cursor2 = cursor3;
                                        z = true;
                                        map2 = map3;
                                        i3 = i4;
                                        try {
                                            event2 = new Event(EVENT_TYPE_NEW, j, getSmsFolderName(i5), this.mSmsType, dateTimeString, str2, str3, "no", i6, null);
                                        } catch (Throwable th) {
                                            th = th;
                                            cursor = cursor2;
                                            if (cursor != null) {
                                            }
                                            throw th;
                                        }
                                    }
                                    event = event2;
                                } else {
                                    cursor2 = cursor3;
                                    map2 = map3;
                                    z = true;
                                    i3 = i4;
                                    event = new Event(EVENT_TYPE_NEW, j, getSmsFolderName(i5), null, this.mSmsType);
                                }
                                sendEvent(event);
                                i2 = i3;
                                z3 = z;
                                map = map2;
                            } else {
                                cursor2 = cursor3;
                                HashMap map4 = map3;
                                z = true;
                                if (i5 != msgRemove.type) {
                                    Log.d(TAG, "new type: " + i5 + " old type: " + msgRemove.type);
                                    String smsFolderName = getSmsFolderName(msgRemove.type);
                                    if (!smsFolderName.equalsIgnoreCase(getSmsFolderName(i5))) {
                                        msg = msgRemove;
                                        i = i7;
                                        sendEvent(new Event(EVENT_TYPE_SHIFT, j, getSmsFolderName(i5), smsFolderName, this.mSmsType));
                                    } else {
                                        msg = msgRemove;
                                        i = i7;
                                    }
                                    msg.type = i5;
                                } else {
                                    msg = msgRemove;
                                    i = i7;
                                    if (i6 != msg.threadId) {
                                        Log.d(TAG, "Message delete change: type: " + i5 + " old type: " + msg.type + "\n    threadId: " + i6 + " old threadId: " + msg.threadId);
                                        if (i6 == -1) {
                                            sendEvent(new Event(EVENT_TYPE_DELETE, j, getSmsFolderName(msg.type), null, this.mSmsType));
                                            msg.threadId = i6;
                                        } else {
                                            sendEvent(new Event(EVENT_TYPE_SHIFT, j, getSmsFolderName(msg.type), BluetoothMapContract.FOLDER_NAME_DELETED, this.mSmsType));
                                            msg.threadId = i6;
                                        }
                                    }
                                    if (i == msg.flagRead) {
                                        msg.flagRead = i;
                                        i2 = 10;
                                        if (this.mMapEventReportVersion > 10) {
                                            sendEvent(new Event(EVENT_TYPE_READ_STATUS, j, getSmsFolderName(msg.type), this.mSmsType));
                                        }
                                        z2 = true;
                                    } else {
                                        i2 = 10;
                                        z2 = z3;
                                    }
                                    Long lValueOf = Long.valueOf(j);
                                    map = map4;
                                    map.put(lValueOf, msg);
                                    z3 = z2;
                                }
                                z3 = true;
                                if (i == msg.flagRead) {
                                }
                                Long lValueOf2 = Long.valueOf(j);
                                map = map4;
                                map.put(lValueOf2, msg);
                                z3 = z2;
                            }
                            cursor = cursor2;
                            try {
                                if (!cursor.moveToNext()) {
                                    break;
                                }
                                cursor3 = cursor;
                                i4 = i2;
                                map3 = map;
                            } catch (Throwable th2) {
                                th = th2;
                                if (cursor != null) {
                                    cursor.close();
                                }
                                throw th;
                            }
                        }
                    } else {
                        cursor = cursor3;
                        map = map3;
                        z = true;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    cursor = cursor3;
                }
            }
            boolean z4 = z3;
            if (cursor != null) {
                cursor.close();
            }
            for (Msg msg2 : getMsgListSms().values()) {
                sendEvent(new Event(EVENT_TYPE_DELETE, msg2.id, getSmsFolderName(msg2.type), null, this.mSmsType));
                z4 = z;
            }
            setMsgListSms(map, z4);
        }
    }

    private void handleMsgListChangesMms() {
        Cursor cursorQuery;
        Cursor cursor;
        Cursor cursor2;
        int i;
        Msg msg;
        int i2;
        HashMap map;
        boolean z;
        HashMap map2;
        Event event;
        Event event2;
        HashMap map3 = new HashMap();
        synchronized (getMsgListMms()) {
            int i3 = 10;
            if (this.mMapEventReportVersion == 10) {
                cursorQuery = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION_SHORT, null, null, null);
            } else {
                cursorQuery = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION_SHORT_EXT, null, null, null);
            }
            Cursor cursor3 = cursorQuery;
            if (cursor3 != null) {
                try {
                    if (cursor3.moveToFirst()) {
                        boolean z2 = false;
                        while (true) {
                            long j = cursor3.getLong(cursor3.getColumnIndex("_id"));
                            int i4 = cursor3.getInt(cursor3.getColumnIndex("msg_box"));
                            int i5 = cursor3.getInt(cursor3.getColumnIndex("m_type"));
                            int i6 = cursor3.getInt(cursor3.getColumnIndex("thread_id"));
                            int i7 = cursor3.getInt(cursor3.getColumnIndex(BluetoothMapContract.FILTER_READ_STATUS));
                            Msg msgRemove = getMsgListMms().remove(Long.valueOf(j));
                            if (msgRemove == null) {
                                if (!getMmsFolderName(i4).equalsIgnoreCase(BluetoothMapContract.FOLDER_NAME_INBOX) || i5 == 132) {
                                    map3.put(Long.valueOf(j), new Msg(j, i4, i6, i7));
                                    if (this.mTransmitEvents && this.mMapEventReportVersion != i3) {
                                        String dateTimeString = BluetoothMapUtils.getDateTimeString(cursor3.getLong(cursor3.getColumnIndex(BluetoothMapContract.MessageColumns.DATE)));
                                        String string = cursor3.getString(cursor3.getColumnIndex("sub"));
                                        if (string == null || string.length() == 0) {
                                            string = BluetoothMapContent.getTextPartsMms(this.mResolver, j);
                                            if (string == null) {
                                                string = "";
                                            }
                                        }
                                        String str = string;
                                        int i8 = cursor3.getInt(cursor3.getColumnIndex("pri"));
                                        Log.d(TAG, "TEMP handleMsgListChangesMms, newMessage 'read' state: " + i7 + "priority: " + i8);
                                        String addressMms = BluetoothMapContent.getAddressMms(this.mResolver, j, 137);
                                        if (addressMms == null) {
                                            addressMms = "";
                                        }
                                        String str2 = addressMms;
                                        String str3 = i8 == 130 ? "yes" : "no";
                                        if (this.mMapEventReportVersion == 11) {
                                            event2 = new Event(EVENT_TYPE_NEW, j, getMmsFolderName(i4), BluetoothMapUtils.TYPE.MMS, dateTimeString, str, str2, str3);
                                            cursor2 = cursor3;
                                            map2 = map3;
                                        } else {
                                            cursor2 = cursor3;
                                            map2 = map3;
                                            try {
                                                event2 = new Event(EVENT_TYPE_NEW, j, getMmsFolderName(i4), BluetoothMapUtils.TYPE.MMS, dateTimeString, str, str2, str3, i6, null);
                                            } catch (Throwable th) {
                                                th = th;
                                                cursor = cursor2;
                                                if (cursor != null) {
                                                }
                                                throw th;
                                            }
                                        }
                                        event = event2;
                                    } else {
                                        cursor2 = cursor3;
                                        map2 = map3;
                                        event = new Event(EVENT_TYPE_NEW, j, getMmsFolderName(i4), null, BluetoothMapUtils.TYPE.MMS);
                                    }
                                    sendEvent(event);
                                } else {
                                    cursor2 = cursor3;
                                    map2 = map3;
                                }
                                z2 = true;
                                map = map2;
                                i2 = 10;
                            } else {
                                cursor2 = cursor3;
                                HashMap map4 = map3;
                                if (i4 != msgRemove.type) {
                                    Log.d(TAG, "new type: " + i4 + " old type: " + msgRemove.type);
                                    if (!msgRemove.localInitiatedSend) {
                                        msg = msgRemove;
                                        i = i7;
                                        sendEvent(new Event(EVENT_TYPE_SHIFT, j, getMmsFolderName(i4), getMmsFolderName(msgRemove.type), BluetoothMapUtils.TYPE.MMS));
                                    } else {
                                        msg = msgRemove;
                                        i = i7;
                                    }
                                    msg.type = i4;
                                    if (getMmsFolderName(i4).equalsIgnoreCase(BluetoothMapContract.FOLDER_NAME_SENT) && msg.localInitiatedSend) {
                                        msg.localInitiatedSend = false;
                                        sendEvent(new Event(EVENT_TYPE_SENDING_SUCCESS, j, getMmsFolderName(i4), null, BluetoothMapUtils.TYPE.MMS));
                                    }
                                } else {
                                    i = i7;
                                    msg = msgRemove;
                                    if (i6 != msg.threadId) {
                                        Log.d(TAG, "Message delete change: type: " + i4 + " old type: " + msg.type + "\n    threadId: " + i6 + " old threadId: " + msg.threadId);
                                        if (i6 == -1) {
                                            sendEvent(new Event(EVENT_TYPE_DELETE, j, getMmsFolderName(msg.type), null, BluetoothMapUtils.TYPE.MMS));
                                            msg.threadId = i6;
                                        } else {
                                            sendEvent(new Event(EVENT_TYPE_SHIFT, j, getMmsFolderName(msg.type), BluetoothMapContract.FOLDER_NAME_DELETED, BluetoothMapUtils.TYPE.MMS));
                                            msg.threadId = i6;
                                        }
                                    }
                                    if (i == msg.flagRead) {
                                        msg.flagRead = i;
                                        i2 = 10;
                                        if (this.mMapEventReportVersion > 10) {
                                            sendEvent(new Event(EVENT_TYPE_READ_STATUS, j, getMmsFolderName(msg.type), BluetoothMapUtils.TYPE.MMS));
                                        }
                                        z2 = true;
                                    } else {
                                        i2 = 10;
                                    }
                                    Long lValueOf = Long.valueOf(j);
                                    map = map4;
                                    map.put(lValueOf, msg);
                                }
                                z2 = true;
                                if (i == msg.flagRead) {
                                }
                                Long lValueOf2 = Long.valueOf(j);
                                map = map4;
                                map.put(lValueOf2, msg);
                            }
                            cursor = cursor2;
                            try {
                                if (!cursor.moveToNext()) {
                                    break;
                                }
                                cursor3 = cursor;
                                i3 = i2;
                                map3 = map;
                            } catch (Throwable th2) {
                                th = th2;
                                if (cursor != null) {
                                    cursor.close();
                                }
                                throw th;
                            }
                        }
                        z = z2;
                    } else {
                        cursor = cursor3;
                        map = map3;
                        z = false;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    cursor = cursor3;
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            boolean z3 = z;
            for (Msg msg2 : getMsgListMms().values()) {
                sendEvent(new Event(EVENT_TYPE_DELETE, msg2.id, getMmsFolderName(msg2.type), null, BluetoothMapUtils.TYPE.MMS));
                z3 = true;
            }
            setMsgListMms(map, z3);
        }
    }

    private void handleMsgListChangesMsg(Uri uri) throws RemoteException {
        Cursor cursor;
        int i;
        Cursor cursor2;
        int i2;
        Msg msg;
        boolean z;
        int i3;
        char c;
        HashMap map;
        int i4;
        HashMap map2;
        Event event;
        Event event2;
        String str;
        HashMap map3 = new HashMap();
        int i5 = 11;
        Cursor cursorQuery = this.mMapEventReportVersion == 10 ? this.mProviderClient.query(this.mMessageUri, MSG_PROJECTION_SHORT, null, null, null) : this.mMapEventReportVersion == 11 ? this.mProviderClient.query(this.mMessageUri, MSG_PROJECTION_SHORT_EXT, null, null, null) : this.mProviderClient.query(this.mMessageUri, MSG_PROJECTION_SHORT_EXT2, null, null, null);
        synchronized (getMsgListMsg()) {
            int i6 = 1;
            try {
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            int i7 = 0;
                            while (true) {
                                long j = cursorQuery.getLong(cursorQuery.getColumnIndex("_id"));
                                int i8 = cursorQuery.getInt(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.FOLDER_ID));
                                int i9 = cursorQuery.getInt(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.FLAG_READ));
                                Msg msgRemove = getMsgListMsg().remove(Long.valueOf(j));
                                long j2 = i8;
                                BluetoothMapFolderElement folderById = this.mFolders.getFolderById(j2);
                                String fullPath = folderById != null ? folderById.getFullPath() : EnvironmentCompat.MEDIA_UNKNOWN;
                                if (msgRemove == null) {
                                    Cursor cursor3 = cursorQuery;
                                    try {
                                        map3.put(Long.valueOf(j), new Msg(j, i8, 0, i9));
                                        if (this.mMapEventReportVersion != 10) {
                                            try {
                                                String dateTimeString = BluetoothMapUtils.getDateTimeString(cursor3.getLong(cursor3.getColumnIndex(BluetoothMapContract.MessageColumns.DATE)));
                                                String string = cursor3.getString(cursor3.getColumnIndex(BluetoothMapContract.MessageColumns.SUBJECT));
                                                String string2 = cursor3.getString(cursor3.getColumnIndex(BluetoothMapContract.MessageColumns.FROM_LIST));
                                                String str2 = cursor3.getInt(cursor3.getColumnIndex(BluetoothMapContract.MessageColumns.FLAG_HIGH_PRIORITY)) == i6 ? "yes" : "no";
                                                if (this.mMapEventReportVersion == i5) {
                                                    i = i6;
                                                    map2 = map3;
                                                    try {
                                                        event2 = new Event(EVENT_TYPE_NEW, j, fullPath, this.mAccount.getType(), dateTimeString, string, string2, str2);
                                                        cursor2 = cursor3;
                                                        i2 = i5;
                                                        event = event2;
                                                    } catch (Throwable th) {
                                                        th = th;
                                                        cursor = cursor3;
                                                        if (cursor != null) {
                                                        }
                                                        throw th;
                                                    }
                                                } else {
                                                    i = i6;
                                                    map2 = map3;
                                                    try {
                                                        cursor2 = cursor3;
                                                        i2 = i5;
                                                        try {
                                                            event2 = new Event(EVENT_TYPE_NEW, j, fullPath, this.mAccount.getType(), dateTimeString, string, string2, str2, cursor3.getLong(cursor3.getColumnIndex("thread_id")), cursor3.getString(cursor3.getColumnIndex("thread_name")));
                                                            event = event2;
                                                        } catch (Throwable th2) {
                                                            th = th2;
                                                            cursor = cursor2;
                                                            if (cursor != null) {
                                                            }
                                                            throw th;
                                                        }
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                        cursor = cursor3;
                                                        if (cursor != null) {
                                                        }
                                                        throw th;
                                                    }
                                                }
                                            } catch (Throwable th4) {
                                                th = th4;
                                                cursor = cursor3;
                                            }
                                        } else {
                                            i = i6;
                                            i2 = i5;
                                            map2 = map3;
                                            cursor2 = cursor3;
                                            event = new Event(EVENT_TYPE_NEW, j, fullPath, null, BluetoothMapUtils.TYPE.EMAIL);
                                        }
                                        sendEvent(event);
                                        i7 = i;
                                        map = map2;
                                        c = '\n';
                                        z = false;
                                    } catch (Throwable th5) {
                                        th = th5;
                                        cursor = cursor3;
                                    }
                                } else {
                                    i = i6;
                                    cursor2 = cursorQuery;
                                    i2 = i5;
                                    HashMap map4 = map3;
                                    if (j2 == msgRemove.folderId || msgRemove.folderId == -1) {
                                        msg = msgRemove;
                                        z = false;
                                        i3 = i7;
                                    } else {
                                        if (D) {
                                            Log.d(TAG, "new folderId: " + i8 + " old folderId: " + msgRemove.folderId);
                                        }
                                        BluetoothMapFolderElement folderById2 = this.mFolders.getFolderById(msgRemove.folderId);
                                        String fullPath2 = folderById2 != null ? folderById2.getFullPath() : EnvironmentCompat.MEDIA_UNKNOWN;
                                        BluetoothMapFolderElement folderByName = this.mFolders.getFolderByName(BluetoothMapContract.FOLDER_NAME_DELETED);
                                        BluetoothMapFolderElement folderByName2 = this.mFolders.getFolderByName(BluetoothMapContract.FOLDER_NAME_SENT);
                                        if (folderByName == null || folderByName.getFolderId() != j2) {
                                            msg = msgRemove;
                                            if (folderByName2 == null || folderByName2.getFolderId() != j2 || !msg.localInitiatedSend) {
                                                z = false;
                                                if (!fullPath2.equalsIgnoreCase("root")) {
                                                    sendEvent(new Event(EVENT_TYPE_SHIFT, j, fullPath, fullPath2, this.mAccount.getType()));
                                                }
                                            } else if (msg.transparent) {
                                                this.mResolver.delete(ContentUris.withAppendedId(this.mMessageUri, j), null, null);
                                            } else {
                                                z = false;
                                                msg.localInitiatedSend = false;
                                                sendEvent(new Event(EVENT_TYPE_SENDING_SUCCESS, msg.id, fullPath2, null, this.mAccount.getType()));
                                            }
                                            msg.folderId = j2;
                                            i3 = i;
                                        } else {
                                            msg = msgRemove;
                                            sendEvent(new Event(EVENT_TYPE_DELETE, msgRemove.id, fullPath2, null, this.mAccount.getType()));
                                        }
                                        z = false;
                                        msg.folderId = j2;
                                        i3 = i;
                                    }
                                    if (i9 != msg.flagRead) {
                                        c = '\n';
                                        if (this.mMapEventReportVersion > 10) {
                                            sendEvent(new Event(EVENT_TYPE_READ_STATUS, j, fullPath, this.mAccount.getType()));
                                            msg.flagRead = i9;
                                        }
                                        i3 = i;
                                    } else {
                                        c = '\n';
                                    }
                                    map = map4;
                                    map.put(Long.valueOf(j), msg);
                                    i7 = i3;
                                }
                                cursor = cursor2;
                                try {
                                    if (!cursor.moveToNext()) {
                                        break;
                                    }
                                    cursorQuery = cursor;
                                    map3 = map;
                                    i5 = i2;
                                    i6 = i;
                                } catch (Throwable th6) {
                                    th = th6;
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                    throw th;
                                }
                            }
                            i4 = i7;
                        } else {
                            i = 1;
                            cursor = cursorQuery;
                            map = map3;
                            z = false;
                            i4 = 0;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        cursor = cursorQuery;
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
                boolean z2 = i4;
                for (Msg msg2 : getMsgListMsg().values()) {
                    BluetoothMapFolderElement folderById3 = this.mFolders.getFolderById(msg2.folderId);
                    String fullPath3 = folderById3 != null ? folderById3.getFullPath() : EnvironmentCompat.MEDIA_UNKNOWN;
                    if (msg2.localInitiatedSend) {
                        msg2.localInitiatedSend = z;
                        String str3 = msg2.transparent ? null : fullPath3;
                        sendEvent(new Event(EVENT_TYPE_SENDING_SUCCESS, msg2.id, str3, null, this.mAccount.getType()));
                        str = str3;
                    } else {
                        str = fullPath3;
                    }
                    if (!msg2.transparent) {
                        sendEvent(new Event(EVENT_TYPE_DELETE, msg2.id, str, null, this.mAccount.getType()));
                    }
                    z2 = i;
                }
                setMsgListMsg(map, z2);
            } finally {
            }
        }
    }

    private void handleMsgListChanges(Uri uri) {
        if (uri.getAuthority().equals(this.mAuthority)) {
            try {
                if (D) {
                    Log.d(TAG, "handleMsgListChanges: account type = " + this.mAccount.getType().toString());
                }
                handleMsgListChangesMsg(uri);
            } catch (RemoteException e) {
                this.mMasInstance.restartObexServerSession();
                Log.w(TAG, "Problems contacting the ContentProvider in mas Instance " + this.mMasId + " restaring ObexServerSession");
            }
        }
        if (this.mEnableSmsMms) {
            handleMsgListChangesSms();
            handleMsgListChangesMms();
        }
    }

    private void handleContactListChanges(Uri uri) throws Throwable {
        Cursor cursor;
        int i;
        int i2;
        BluetoothMapConvoContactElement bluetoothMapConvoContactElement;
        String str;
        Cursor cursor2;
        boolean z;
        ConvoContactInfo convoContactInfo;
        BluetoothMapConvoContactElement bluetoothMapConvoContactElement2;
        Cursor cursor3;
        HashMap map;
        HashMap map2;
        Cursor cursor4;
        ConvoContactInfo convoContactInfo2;
        if (uri.getAuthority().equals(this.mAuthority)) {
            boolean z2 = false;
            Cursor cursor5 = null;
            try {
                try {
                    ConvoContactInfo convoContactInfo3 = new ConvoContactInfo();
                    int i3 = 10;
                    if (this.mMapEventReportVersion == 10) {
                        return;
                    }
                    int i4 = 11;
                    if (this.mMapEventReportVersion != 11) {
                        Cursor cursorQuery = this.mProviderClient.query(this.mContactUri, BluetoothMapContract.BT_CONTACT_CHATSTATE_PRESENCE_PROJECTION, null, null, null);
                        try {
                            convoContactInfo3.setConvoColunms(cursorQuery);
                            HashMap map3 = new HashMap(getContactList().size());
                            synchronized (getContactList()) {
                                if (cursorQuery != null) {
                                    try {
                                        if (cursorQuery.moveToFirst()) {
                                            while (true) {
                                                String string = cursorQuery.getString(convoContactInfo3.mContactColUci);
                                                long j = cursorQuery.getLong(convoContactInfo3.mContactColConvoId);
                                                if (j != 0) {
                                                    BluetoothMapConvoContactElement bluetoothMapConvoContactElementRemove = getContactList().remove(string);
                                                    if (bluetoothMapConvoContactElementRemove == null) {
                                                        if (this.mMapEventReportVersion != i3 && this.mMapEventReportVersion != i4) {
                                                            String string2 = cursorQuery.getString(convoContactInfo3.mContactColName);
                                                            String string3 = cursorQuery.getString(convoContactInfo3.mContactColNickname);
                                                            String string4 = cursorQuery.getString(convoContactInfo3.mContactColPresenceText);
                                                            int i5 = cursorQuery.getInt(convoContactInfo3.mContactColPresenceState);
                                                            long j2 = cursorQuery.getLong(convoContactInfo3.mContactColLastActive);
                                                            int i6 = cursorQuery.getInt(convoContactInfo3.mContactColChatState);
                                                            int i7 = cursorQuery.getInt(convoContactInfo3.mContactColPriority);
                                                            map3.put(string, new BluetoothMapConvoContactElement(string, string2, string3, string4, i5, j2, i6, i7, cursorQuery.getString(convoContactInfo3.mContactColBtUid)));
                                                            map2 = map3;
                                                            cursor4 = cursorQuery;
                                                            i = i4;
                                                            i2 = i3;
                                                            convoContactInfo2 = convoContactInfo3;
                                                            try {
                                                                sendEvent(new Event(EVENT_TYPE_CONVERSATION, string, this.mAccount.getType(), string2, String.valueOf(i7), BluetoothMapUtils.getDateTimeString(j2), j, null, i5, string4, i6));
                                                            } catch (Throwable th) {
                                                                th = th;
                                                                cursor = cursor4;
                                                            }
                                                        } else {
                                                            map2 = map3;
                                                            cursor4 = cursorQuery;
                                                            i = i4;
                                                            i2 = i3;
                                                            convoContactInfo2 = convoContactInfo3;
                                                        }
                                                        z = true;
                                                        map = map2;
                                                        cursor3 = cursor4;
                                                        convoContactInfo = convoContactInfo2;
                                                    } else {
                                                        HashMap map4 = map3;
                                                        Cursor cursor6 = cursorQuery;
                                                        i = i4;
                                                        i2 = i3;
                                                        try {
                                                            int i8 = cursor6.getInt(convoContactInfo3.mContactColPresenceState);
                                                            String string5 = cursor6.getString(convoContactInfo3.mContactColPresenceText);
                                                            String presenceStatus = bluetoothMapConvoContactElementRemove.getPresenceStatus();
                                                            if (bluetoothMapConvoContactElementRemove.getPresenceAvailability() != i8 || !Objects.equals(presenceStatus, string5)) {
                                                                long j3 = cursor6.getLong(convoContactInfo3.mContactColLastOnline);
                                                                bluetoothMapConvoContactElementRemove.setPresenceAvailability(i8);
                                                                bluetoothMapConvoContactElementRemove.setLastActivity(j3);
                                                                if (presenceStatus != null && !presenceStatus.equals(string5)) {
                                                                    bluetoothMapConvoContactElementRemove.setPresenceStatus(string5);
                                                                }
                                                                bluetoothMapConvoContactElement = bluetoothMapConvoContactElementRemove;
                                                                str = string;
                                                                cursor2 = cursor6;
                                                                z = z2;
                                                                convoContactInfo = convoContactInfo3;
                                                                try {
                                                                    sendEvent(new Event(EVENT_TYPE_PRESENCE, string, this.mAccount.getType(), bluetoothMapConvoContactElementRemove.getName(), String.valueOf(bluetoothMapConvoContactElementRemove.getPriority()), BluetoothMapUtils.getDateTimeString(j3), j, null, i8, string5, 0));
                                                                } catch (Throwable th2) {
                                                                    th = th2;
                                                                    cursor = cursor2;
                                                                }
                                                            } else {
                                                                z = z2;
                                                                bluetoothMapConvoContactElement = bluetoothMapConvoContactElementRemove;
                                                                str = string;
                                                                cursor2 = cursor6;
                                                                convoContactInfo = convoContactInfo3;
                                                            }
                                                            Cursor cursor7 = cursor2;
                                                            try {
                                                                int i9 = cursor7.getInt(convoContactInfo.mContactColChatState);
                                                                BluetoothMapConvoContactElement bluetoothMapConvoContactElement3 = bluetoothMapConvoContactElement;
                                                                if (bluetoothMapConvoContactElement3.getChatState() != i9) {
                                                                    long j4 = cursor7.getLong(convoContactInfo.mContactColLastActive);
                                                                    bluetoothMapConvoContactElement3.setLastActivity(j4);
                                                                    bluetoothMapConvoContactElement3.setChatState(i9);
                                                                    bluetoothMapConvoContactElement2 = bluetoothMapConvoContactElement3;
                                                                    cursor3 = cursor7;
                                                                    try {
                                                                        sendEvent(new Event(EVENT_TYPE_CHAT_STATE, str, this.mAccount.getType(), bluetoothMapConvoContactElement3.getName(), String.valueOf(bluetoothMapConvoContactElement3.getPriority()), BluetoothMapUtils.getDateTimeString(j4), j, null, 0, null, i9));
                                                                    } catch (Throwable th3) {
                                                                        th = th3;
                                                                        cursor = cursor3;
                                                                    }
                                                                } else {
                                                                    bluetoothMapConvoContactElement2 = bluetoothMapConvoContactElement3;
                                                                    cursor3 = cursor7;
                                                                }
                                                                map = map4;
                                                                map.put(str, bluetoothMapConvoContactElement2);
                                                            } catch (Throwable th4) {
                                                                th = th4;
                                                                cursor = cursor7;
                                                            }
                                                        } catch (Throwable th5) {
                                                            th = th5;
                                                            cursor = cursor6;
                                                        }
                                                    }
                                                } else {
                                                    z = z2;
                                                    map = map3;
                                                    cursor3 = cursorQuery;
                                                    i = i4;
                                                    i2 = i3;
                                                    convoContactInfo = convoContactInfo3;
                                                }
                                                cursor = cursor3;
                                                try {
                                                    if (!cursor.moveToNext()) {
                                                        break;
                                                    }
                                                    convoContactInfo3 = convoContactInfo;
                                                    map3 = map;
                                                    cursorQuery = cursor;
                                                    i4 = i;
                                                    i3 = i2;
                                                    z2 = z;
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                }
                                            }
                                            z2 = z;
                                        } else {
                                            map = map3;
                                            cursor = cursorQuery;
                                        }
                                    } catch (Throwable th7) {
                                        th = th7;
                                        cursor = cursorQuery;
                                    }
                                }
                                if (getContactList().size() > 0) {
                                    z2 = true;
                                }
                                setContactList(map, z2);
                            }
                            try {
                                throw th;
                            } catch (Throwable th8) {
                                th = th8;
                                cursor5 = cursor;
                                if (cursor5 != null) {
                                }
                                throw th;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            cursor = cursorQuery;
                        }
                    } else {
                        return;
                    }
                } catch (RemoteException e) {
                    this.mMasInstance.restartObexServerSession();
                    Log.w(TAG, "Problems contacting the ContentProvider in mas Instance " + this.mMasId + " restaring ObexServerSession");
                    return;
                }
            } catch (Throwable th10) {
                th = th10;
            }
            if (cursor5 != null) {
                cursor5.close();
            }
            throw th;
        }
    }

    private boolean setEmailMessageStatusDelete(BluetoothMapFolderElement bluetoothMapFolderElement, String str, long j, int i) {
        boolean z;
        Uri uri = Uri.parse(str + BluetoothMapContract.TABLE_MESSAGE);
        ContentValues contentValues = new ContentValues();
        BluetoothMapFolderElement folderByName = this.mFolders.getFolderByName(BluetoothMapContract.FOLDER_NAME_DELETED);
        contentValues.put("_id", Long.valueOf(j));
        synchronized (getMsgListMsg()) {
            Msg msg = getMsgListMsg().get(Long.valueOf(j));
            long folderId = -1;
            z = true;
            if (i != 1) {
                if (i == 0 && msg != null && folderByName != null && msg.folderId == folderByName.getFolderId()) {
                    BluetoothMapFolderElement folderByName2 = bluetoothMapFolderElement.getFolderByName(BluetoothMapContract.FOLDER_NAME_INBOX);
                    if (msg != null && msg.oldFolderId != -1) {
                        folderId = msg.oldFolderId;
                    } else {
                        if (folderByName2 != null) {
                            folderId = folderByName2.getFolderId();
                        }
                        if (D) {
                            Log.d(TAG, "We did not delete the message, hence the old folder is unknown. Moving to inbox.");
                        }
                    }
                    contentValues.put(BluetoothMapContract.MessageColumns.FOLDER_ID, Long.valueOf(folderId));
                    if (this.mResolver.update(uri, contentValues, null, null) > 0) {
                        if (folderByName2 != null) {
                            msg.folderId = folderByName2.getFolderId();
                        } else {
                            msg.folderId = folderId;
                        }
                    } else {
                        if (D) {
                            Log.d(TAG, "We did not delete the message, hence the old folder is unknown. Moving to inbox.");
                        }
                        z = false;
                    }
                } else {
                    z = false;
                }
            }
            if (folderByName != null) {
                folderId = folderByName.getFolderId();
            }
            contentValues.put(BluetoothMapContract.MessageColumns.FOLDER_ID, Long.valueOf(folderId));
            if (this.mResolver.update(uri, contentValues, null, null) <= 0) {
                Log.w(TAG, "Msg: " + j + " - Set delete status " + i + " failed for folderId " + folderId);
                z = false;
            } else {
                if (msg != null) {
                    msg.oldFolderId = msg.folderId;
                    msg.folderId = folderId;
                }
                if (D) {
                    Log.d(TAG, "Deleted MSG: " + j + " from folderId: " + folderId);
                }
            }
        }
        if (!z) {
            Log.w(TAG, "Set delete status " + i + " failed.");
        }
        return z;
    }

    private void updateThreadId(Uri uri, String str, long j) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(str, Long.valueOf(j));
        this.mResolver.update(uri, contentValues, null, null);
    }

    private boolean deleteMessageMms(long j) {
        boolean z;
        Uri uriWithAppendedId = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, j);
        Cursor cursorQuery = this.mResolver.query(uriWithAppendedId, null, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    if (cursorQuery.getInt(cursorQuery.getColumnIndex("thread_id")) != -1) {
                        synchronized (getMsgListMms()) {
                            Msg msg = getMsgListMms().get(Long.valueOf(j));
                            if (msg != null) {
                                msg.threadId = -1;
                            }
                        }
                        updateThreadId(uriWithAppendedId, "thread_id", -1L);
                    } else {
                        synchronized (getMsgListMms()) {
                            getMsgListMms().remove(Long.valueOf(j));
                        }
                        this.mResolver.delete(uriWithAppendedId, null, null);
                    }
                    z = true;
                } else {
                    z = false;
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        return z;
    }

    private boolean unDeleteMessageMms(long j) {
        String addressMms;
        Uri uriWithAppendedId = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, j);
        Cursor cursorQuery = this.mResolver.query(uriWithAppendedId, null, null, null, null);
        boolean z = false;
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    int i = cursorQuery.getInt(cursorQuery.getColumnIndex("thread_id"));
                    if (i == -1) {
                        long j2 = cursorQuery.getLong(cursorQuery.getColumnIndex("_id"));
                        if (cursorQuery.getInt(cursorQuery.getColumnIndex("msg_box")) == 1) {
                            addressMms = BluetoothMapContent.getAddressMms(this.mResolver, j2, 137);
                        } else {
                            addressMms = BluetoothMapContent.getAddressMms(this.mResolver, j2, 151);
                        }
                        HashSet hashSet = new HashSet();
                        hashSet.addAll(Arrays.asList(addressMms));
                        Long lValueOf = Long.valueOf(Telephony.Threads.getOrCreateThreadId(this.mContext, hashSet));
                        synchronized (getMsgListMms()) {
                            Msg msg = getMsgListMms().get(Long.valueOf(j));
                            if (msg != null) {
                                msg.threadId = lValueOf.intValue();
                                msg.type = 1;
                            }
                        }
                        updateThreadId(uriWithAppendedId, "thread_id", lValueOf.longValue());
                    } else {
                        Log.d(TAG, "Message not in deleted folder: handle " + j + " threadId " + i);
                    }
                    z = true;
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        return z;
    }

    private boolean deleteMessageSms(long j) {
        boolean z;
        Uri uriWithAppendedId = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, j);
        Cursor cursorQuery = this.mResolver.query(uriWithAppendedId, null, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    if (cursorQuery.getInt(cursorQuery.getColumnIndex("thread_id")) != -1) {
                        synchronized (getMsgListSms()) {
                            Msg msg = getMsgListSms().get(Long.valueOf(j));
                            if (msg != null) {
                                msg.threadId = -1;
                            }
                        }
                        updateThreadId(uriWithAppendedId, "thread_id", -1L);
                    } else {
                        synchronized (getMsgListSms()) {
                            getMsgListSms().remove(Long.valueOf(j));
                        }
                        this.mResolver.delete(uriWithAppendedId, null, null);
                    }
                    z = true;
                } else {
                    z = false;
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        return z;
    }

    private boolean unDeleteMessageSms(long j) {
        Uri uriWithAppendedId = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, j);
        Cursor cursorQuery = this.mResolver.query(uriWithAppendedId, null, null, null, null);
        boolean z = false;
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    int i = cursorQuery.getInt(cursorQuery.getColumnIndex("thread_id"));
                    if (i == -1) {
                        String string = cursorQuery.getString(cursorQuery.getColumnIndex("address"));
                        HashSet hashSet = new HashSet();
                        hashSet.addAll(Arrays.asList(string));
                        Long lValueOf = Long.valueOf(Telephony.Threads.getOrCreateThreadId(this.mContext, hashSet));
                        synchronized (getMsgListSms()) {
                            Msg msg = getMsgListSms().get(Long.valueOf(j));
                            if (msg != null) {
                                msg.threadId = lValueOf.intValue();
                                msg.type = 1;
                            }
                        }
                        updateThreadId(uriWithAppendedId, "thread_id", lValueOf.longValue());
                    } else {
                        Log.d(TAG, "Message not in deleted folder: handle " + j + " threadId " + i);
                    }
                    z = true;
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        return z;
    }

    public boolean setMessageStatusDeleted(long j, BluetoothMapUtils.TYPE type, BluetoothMapFolderElement bluetoothMapFolderElement, String str, int i) {
        if (D) {
            Log.d(TAG, "setMessageStatusDeleted: handle " + j + " type " + type + " value " + i);
        }
        if (type == BluetoothMapUtils.TYPE.EMAIL) {
            return setEmailMessageStatusDelete(bluetoothMapFolderElement, str, j, i);
        }
        if (type == BluetoothMapUtils.TYPE.IM) {
            if (D) {
                Log.d(TAG, "setMessageStatusDeleted: IM not handled");
            }
        } else if (i == 1) {
            if (type == BluetoothMapUtils.TYPE.SMS_GSM || type == BluetoothMapUtils.TYPE.SMS_CDMA) {
                return deleteMessageSms(j);
            }
            if (type == BluetoothMapUtils.TYPE.MMS) {
                return deleteMessageMms(j);
            }
        } else if (i == 0) {
            if (type == BluetoothMapUtils.TYPE.SMS_GSM || type == BluetoothMapUtils.TYPE.SMS_CDMA) {
                return unDeleteMessageSms(j);
            }
            if (type == BluetoothMapUtils.TYPE.MMS) {
                return unDeleteMessageMms(j);
            }
        }
        return false;
    }

    public boolean setMessageStatusRead(long j, BluetoothMapUtils.TYPE type, String str, int i) throws RemoteException {
        int iUpdate;
        if (D) {
            Log.d(TAG, "setMessageStatusRead: handle " + j + " type " + type + " value " + i);
        }
        if (type == BluetoothMapUtils.TYPE.SMS_GSM || type == BluetoothMapUtils.TYPE.SMS_CDMA) {
            Uri uriWithAppendedId = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, j);
            ContentValues contentValues = new ContentValues();
            contentValues.put(BluetoothMapContract.FILTER_READ_STATUS, Integer.valueOf(i));
            contentValues.put("seen", Integer.valueOf(i));
            String string = contentValues.toString();
            if (D) {
                Log.d(TAG, " -> SMS Uri: " + uriWithAppendedId.toString() + " values " + string);
            }
            synchronized (getMsgListSms()) {
                Msg msg = getMsgListSms().get(Long.valueOf(j));
                if (msg != null) {
                    msg.flagRead = i;
                }
            }
            iUpdate = this.mResolver.update(uriWithAppendedId, contentValues, null, null);
            if (D) {
                Log.d(TAG, " -> " + iUpdate + " rows updated!");
            }
        } else if (type == BluetoothMapUtils.TYPE.MMS) {
            Uri uriWithAppendedId2 = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, j);
            if (D) {
                Log.d(TAG, " -> MMS Uri: " + uriWithAppendedId2.toString());
            }
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put(BluetoothMapContract.FILTER_READ_STATUS, Integer.valueOf(i));
            synchronized (getMsgListMms()) {
                Msg msg2 = getMsgListMms().get(Long.valueOf(j));
                if (msg2 != null) {
                    msg2.flagRead = i;
                }
            }
            iUpdate = this.mResolver.update(uriWithAppendedId2, contentValues2, null, null);
            if (D) {
                Log.d(TAG, " -> " + iUpdate + " rows updated!");
            }
        } else if (type == BluetoothMapUtils.TYPE.EMAIL || type == BluetoothMapUtils.TYPE.IM) {
            Uri uri = this.mMessageUri;
            ContentValues contentValues3 = new ContentValues();
            contentValues3.put(BluetoothMapContract.MessageColumns.FLAG_READ, Integer.valueOf(i));
            contentValues3.put("_id", Long.valueOf(j));
            synchronized (getMsgListMsg()) {
                Msg msg3 = getMsgListMsg().get(Long.valueOf(j));
                if (msg3 != null) {
                    msg3.flagRead = i;
                }
            }
            iUpdate = this.mProviderClient.update(uri, contentValues3, null, null);
        } else {
            iUpdate = 0;
        }
        return iUpdate > 0;
    }

    private class PushMsgInfo {
        public long id;
        public int parts;
        public int partsDelivered;
        public int partsSent;
        public String phone;
        public int retry;
        public int transparent;
        public Uri uri;
        public boolean resend = false;
        public boolean sendInProgress = false;
        public boolean failedSent = false;
        public int statusDelivered = 0;
        public long timestamp = 0;

        PushMsgInfo(long j, int i, int i2, String str, Uri uri) {
            this.id = j;
            this.transparent = i;
            this.retry = i2;
            this.phone = str;
            this.uri = uri;
        }
    }

    public long pushMessage(BluetoothMapbMessage bluetoothMapbMessage, BluetoothMapFolderElement bluetoothMapFolderElement, BluetoothMapAppParams bluetoothMapAppParams, String str, long j) throws RemoteException, IOException, IllegalArgumentException {
        Iterator<BluetoothMapbMessage.VCard> it;
        long j2;
        long j3;
        String smsBody;
        Uri uriAddMessageToUri;
        Cursor cursor;
        long j4;
        Object obj;
        FileOutputStream fileOutputStream;
        ParcelFileDescriptor parcelFileDescriptor;
        Throwable th;
        long j5;
        ParcelFileDescriptor parcelFileDescriptorOpenFile;
        BluetoothMapbMessage bluetoothMapbMessage2 = bluetoothMapbMessage;
        long j6 = j;
        if (D) {
            Log.d(TAG, "pushMessage");
        }
        ArrayList<BluetoothMapbMessage.VCard> recipients = bluetoothMapbMessage.getRecipients();
        int transparent = bluetoothMapAppParams.getTransparent() == -1 ? 0 : bluetoothMapAppParams.getTransparent();
        int retry = bluetoothMapAppParams.getRetry();
        bluetoothMapAppParams.getCharset();
        long j7 = -1;
        if (recipients == null) {
            if (!bluetoothMapFolderElement.getName().equalsIgnoreCase(BluetoothMapContract.FOLDER_NAME_DRAFT)) {
                Log.e(TAG, "Trying to send a message with no recipients");
                return -1L;
            }
            BluetoothMapbMessage.VCard vCard = new BluetoothMapbMessage.VCard("", "", null, null, 0);
            ArrayList<BluetoothMapbMessage.VCard> arrayList = new ArrayList<>();
            arrayList.add(vCard);
            Log.w(TAG, "Added empty recipient to draft message");
            recipients = arrayList;
        }
        if (!bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.EMAIL)) {
            Iterator<BluetoothMapbMessage.VCard> it2 = recipients.iterator();
            long jSendMmsMessage = -1;
            while (it2.hasNext()) {
                BluetoothMapbMessage.VCard next = it2.next();
                if (next.getEnvLevel() == 0) {
                    String firstPhoneNumber = next.getFirstPhoneNumber();
                    if (TextUtils.isEmpty(firstPhoneNumber)) {
                        firstPhoneNumber = queryNumber(this.mContext, next.getName());
                        if (TextUtils.isEmpty(firstPhoneNumber)) {
                            firstPhoneNumber = queryNumber(this.mContext, next.getFormatName());
                        }
                    }
                    String str2 = firstPhoneNumber;
                    if (TextUtils.isEmpty(str2)) {
                        throw new IllegalArgumentException("No Phone number");
                    }
                    next.getFirstEmail();
                    String name = bluetoothMapFolderElement.getName();
                    if (bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.MMS)) {
                        BluetoothMapbMessageMime bluetoothMapbMessageMime = (BluetoothMapbMessageMime) bluetoothMapbMessage2;
                        if (bluetoothMapbMessageMime.getTextOnly()) {
                            smsBody = bluetoothMapbMessageMime.getMessageAsText();
                            int size = SmsManager.getDefault().divideMessage(smsBody).size();
                            if (size <= 10) {
                                if (D) {
                                    Log.d(TAG, "pushMessage - converting MMS to SMS, sms parts=" + size);
                                }
                                bluetoothMapbMessage2.setType(this.mSmsType);
                                if (bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.MMS)) {
                                    jSendMmsMessage = sendMmsMessage(name, str2, (BluetoothMapbMessageMime) bluetoothMapbMessage2, transparent, retry, j6);
                                    j2 = j6;
                                    it = it2;
                                } else {
                                    Iterator<BluetoothMapbMessage.VCard> it3 = it2;
                                    if (!bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.SMS_GSM) && !bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.SMS_CDMA)) {
                                        if (!D) {
                                            return -1L;
                                        }
                                        Log.d(TAG, "pushMessage - failure on type ");
                                        return -1L;
                                    }
                                    if (smsBody == null) {
                                        smsBody = ((BluetoothMapbMessageSms) bluetoothMapbMessage2).getSmsBody();
                                    }
                                    String str3 = smsBody;
                                    if (TextUtils.isEmpty(str3)) {
                                        Log.d(TAG, "PushMsg: Empty msgBody ");
                                        throw new IllegalArgumentException("push EMPTY message: Invalid Body");
                                    }
                                    Uri uri = Uri.parse(Telephony.Sms.CONTENT_URI + "/" + name);
                                    synchronized (getMsgListSms()) {
                                        if (j6 >= 0) {
                                            try {
                                                uriAddMessageToUri = Telephony.Sms.addMessageToUri((int) j6, this.mResolver, uri, str2, str3, "", Long.valueOf(System.currentTimeMillis()), false, true);
                                            } finally {
                                            }
                                        } else {
                                            uriAddMessageToUri = Telephony.Sms.addMessageToUri(this.mResolver, uri, str2, str3, "", Long.valueOf(System.currentTimeMillis()), false, true);
                                        }
                                        Uri uri2 = uriAddMessageToUri;
                                        if (uri2 == null) {
                                            if (D) {
                                                Log.d(TAG, "pushMessage - failure on add to uri " + uri);
                                            }
                                            return -1L;
                                        }
                                        Cursor cursorQuery = this.mResolver.query(uri2, SMS_PROJECTION_SHORT, null, null, null);
                                        if (cursorQuery != null) {
                                            try {
                                                if (cursorQuery.moveToFirst()) {
                                                    long j8 = cursorQuery.getLong(cursorQuery.getColumnIndex("_id"));
                                                    it = it3;
                                                    try {
                                                        getMsgListSms().put(Long.valueOf(j8), new Msg(j8, cursorQuery.getInt(cursorQuery.getColumnIndex(EXTRA_MESSAGE_SENT_MSG_TYPE)), cursorQuery.getInt(cursorQuery.getColumnIndex("thread_id")), cursorQuery.getInt(cursorQuery.getColumnIndex(BluetoothMapContract.FILTER_READ_STATUS))));
                                                        cursorQuery.close();
                                                        if (cursorQuery != null) {
                                                            cursorQuery.close();
                                                        }
                                                        j4 = Long.parseLong(uri2.getLastPathSegment());
                                                        if (name.equalsIgnoreCase(BluetoothMapContract.FOLDER_NAME_OUTBOX)) {
                                                            PushMsgInfo pushMsgInfo = new PushMsgInfo(j4, transparent, retry, str2, uri2);
                                                            this.mPushMsgList.put(Long.valueOf(j4), pushMsgInfo);
                                                            j2 = j;
                                                            sendMessage(pushMsgInfo, str3, j2);
                                                        } else {
                                                            j2 = j;
                                                        }
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                        cursor = cursorQuery;
                                                        if (cursor != null) {
                                                        }
                                                        throw th;
                                                    }
                                                }
                                            } catch (Throwable th3) {
                                                th = th3;
                                                cursor = cursorQuery;
                                            }
                                        }
                                        cursor = cursorQuery;
                                        try {
                                            Log.w(TAG, "Message: " + uri2 + " no longer exist!");
                                            if (cursor != null) {
                                                cursor.close();
                                            }
                                            return -1L;
                                        } catch (Throwable th4) {
                                            th = th4;
                                            if (cursor != null) {
                                                cursor.close();
                                            }
                                            throw th;
                                        }
                                    }
                                    jSendMmsMessage = j4;
                                }
                                j3 = -1;
                            } else {
                                if (D) {
                                    Log.d(TAG, "pushMessage - MMS text only but to big to convert to SMS");
                                }
                                smsBody = null;
                                if (bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.MMS)) {
                                }
                                j3 = -1;
                            }
                        } else {
                            smsBody = null;
                            if (bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.MMS)) {
                            }
                            j3 = -1;
                        }
                    }
                } else {
                    it = it2;
                    j2 = j6;
                    j3 = j7;
                }
                j6 = j2;
                j7 = j3;
                it2 = it;
                bluetoothMapbMessage2 = bluetoothMapbMessage;
            }
            return jSendMmsMessage;
        }
        String emailBody = ((BluetoothMapbMessageEmail) bluetoothMapbMessage2).getEmailBody();
        Uri uri3 = Uri.parse(str + BluetoothMapContract.TABLE_MESSAGE);
        if (D) {
            Log.d(TAG, "pushMessage - uriInsert= " + uri3.toString() + ", intoFolder id=" + bluetoothMapFolderElement.getFolderId());
        }
        synchronized (getMsgListMsg()) {
            ContentValues contentValues = new ContentValues();
            long folderId = bluetoothMapFolderElement.getFolderId();
            String[] strArrSplit = new String[2];
            long time = Calendar.getInstance().getTime().getTime();
            String firstEmail = recipients.get(recipients.size() - 1).getFirstEmail();
            String[] strArrSplit2 = emailBody.split(VCardBuilder.VCARD_END_OF_LINE);
            int i = 0;
            while (true) {
                if (i >= strArrSplit2.length) {
                    break;
                }
                if (strArrSplit2[i].contains("Subject:")) {
                    strArrSplit = strArrSplit2[i].split("Subject:", 2);
                    break;
                }
                i++;
            }
            contentValues.put(BluetoothMapContract.MessageColumns.FOLDER_ID, Long.valueOf(folderId));
            contentValues.put(BluetoothMapContract.MessageColumns.SUBJECT, strArrSplit[1]);
            contentValues.put(BluetoothMapContract.MessageColumns.DATE, Long.valueOf(time));
            contentValues.put(BluetoothMapContract.MessageColumns.TO_LIST, firstEmail);
            Log.d(TAG, "[pushMessage] folderId = " + folderId);
            Log.d(TAG, "[pushMessage] timestamp = " + time);
            Log.d(TAG, "[pushMessage] address = " + firstEmail);
            Log.d(TAG, "[pushMessage] subject = " + strArrSplit[1]);
            Log.d(TAG, "[pushMessage] list all vCard information:");
            for (int i2 = 0; i2 < recipients.size(); i2++) {
                Log.d(TAG, "i=" + i2 + ", envelope=" + recipients.get(i2).getEnvLevel() + ", name=" + recipients.get(i2).getName());
                String[] email = recipients.get(i2).getEmail();
                for (int i3 = 0; i3 < email.length; i3++) {
                    Log.d(TAG, "j=" + i3 + ", vCardEmailAddress=" + email[i3]);
                }
            }
            Uri uriInsert = this.mProviderClient.insert(uri3, contentValues);
            if (D) {
                Log.d(TAG, "pushMessage - uriNew= " + uriInsert.toString());
            }
            try {
                j5 = Long.parseLong(uriInsert.getLastPathSegment());
            } catch (Throwable th5) {
                th = th5;
                obj = uriInsert;
                fileOutputStream = null;
            }
            try {
                parcelFileDescriptorOpenFile = this.mProviderClient.openFile(uriInsert, "w");
                try {
                    fileOutputStream = new FileOutputStream(parcelFileDescriptorOpenFile.getFileDescriptor());
                } catch (FileNotFoundException e) {
                    e = e;
                } catch (NullPointerException e2) {
                    e = e2;
                }
            } catch (FileNotFoundException e3) {
                e = e3;
            } catch (NullPointerException e4) {
                e = e4;
            } catch (Throwable th6) {
                th = th6;
                fileOutputStream = null;
                parcelFileDescriptor = 0;
                if (fileOutputStream == null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e5) {
                        Log.w(TAG, e5);
                    }
                    if (parcelFileDescriptor != 0) {
                        throw th;
                    }
                    try {
                        parcelFileDescriptor.close();
                        throw th;
                    } catch (IOException e6) {
                        Log.w(TAG, e6);
                        throw th;
                    }
                }
                if (parcelFileDescriptor != 0) {
                }
            }
            try {
                fileOutputStream.write(emailBody.getBytes(), 0, emailBody.getBytes().length);
                try {
                    fileOutputStream.close();
                } catch (IOException e7) {
                    Log.w(TAG, e7);
                }
                if (parcelFileDescriptorOpenFile != null) {
                    try {
                        parcelFileDescriptorOpenFile.close();
                    } catch (IOException e8) {
                        Log.w(TAG, e8);
                    }
                }
                Msg msg = new Msg(j5, folderId, 1);
                msg.transparent = transparent == 1;
                if (folderId == bluetoothMapFolderElement.getFolderByName(BluetoothMapContract.FOLDER_NAME_OUTBOX).getFolderId()) {
                    msg.localInitiatedSend = true;
                }
                getMsgListMsg().put(Long.valueOf(j5), msg);
            } catch (FileNotFoundException e9) {
                e = e9;
                Log.w(TAG, e);
                throw new IOException("Unable to open file stream");
            } catch (NullPointerException e10) {
                e = e10;
                Log.w(TAG, e);
                throw new IllegalArgumentException("Unable to parse message.");
            } catch (Throwable th7) {
                th = th7;
                obj = parcelFileDescriptorOpenFile;
                th = th;
                parcelFileDescriptor = obj;
                if (fileOutputStream == null) {
                }
            }
        }
        return j5;
    }

    public long sendMmsMessage(String str, String str2, BluetoothMapbMessageMime bluetoothMapbMessageMime, int i, int i2, long j) {
        if (str != null && (str.equalsIgnoreCase(BluetoothMapContract.FOLDER_NAME_OUTBOX) || str.equalsIgnoreCase(BluetoothMapContract.FOLDER_NAME_DRAFT))) {
            long jPushMmsToFolder = pushMmsToFolder(3, str2, bluetoothMapbMessageMime, j);
            if (-1 != jPushMmsToFolder && str.equalsIgnoreCase(BluetoothMapContract.FOLDER_NAME_OUTBOX)) {
                Uri uriBuild = MmsFileProvider.CONTENT_URI.buildUpon().appendPath(Long.toString(jPushMmsToFolder)).build();
                Intent intent = new Intent(ACTION_MESSAGE_SENT);
                intent.setType("message/" + Long.toString(jPushMmsToFolder));
                intent.putExtra(EXTRA_MESSAGE_SENT_MSG_TYPE, BluetoothMapUtils.TYPE.MMS.ordinal());
                intent.putExtra(EXTRA_MESSAGE_SENT_HANDLE, jPushMmsToFolder);
                intent.putExtra(EXTRA_MESSAGE_SENT_TRANSPARENT, i);
                intent.putExtra(EXTRA_MESSAGE_SENT_RETRY, i2);
                SmsManager.getSmsManagerForSubscriptionId((int) j).sendMultimediaMessage(this.mContext, uriBuild, null, null, PendingIntent.getBroadcast(this.mContext, 0, intent, 0));
            }
            return jPushMmsToFolder;
        }
        throw new IllegalArgumentException("Cannot push message to other folders than outbox/draft");
    }

    private void moveDraftToOutbox(long j) {
        moveMmsToFolder(j, this.mResolver, 4);
    }

    private static void moveMmsToFolder(long j, ContentResolver contentResolver, int i) {
        if (j != -1) {
            String str = " _id= " + j;
            Uri uri = Telephony.Mms.CONTENT_URI;
            Cursor cursorQuery = contentResolver.query(uri, null, str, null, null);
            try {
                if (cursorQuery != null) {
                    if (cursorQuery.getCount() > 0) {
                        cursorQuery.moveToFirst();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("msg_box", Integer.valueOf(i));
                        contentResolver.update(uri, contentValues, str, null);
                        if (D) {
                            Log.d(TAG, "moved MMS message to " + getMmsFolderName(i));
                        }
                    }
                } else {
                    Log.w(TAG, "Could not move MMS message to " + getMmsFolderName(i));
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
    }

    private long pushMmsToFolder(int i, String str, BluetoothMapbMessageMime bluetoothMapbMessageMime, long j) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("msg_box", Integer.valueOf(i));
        contentValues.put(BluetoothMapContract.FILTER_READ_STATUS, (Integer) 0);
        contentValues.put("seen", (Integer) 0);
        if (bluetoothMapbMessageMime.getSubject() != null) {
            contentValues.put("sub", bluetoothMapbMessageMime.getSubject());
        } else {
            contentValues.put("sub", "");
        }
        if (bluetoothMapbMessageMime.getSubject() != null && bluetoothMapbMessageMime.getSubject().length() > 0) {
            contentValues.put("sub_cs", (Integer) 106);
        }
        contentValues.put("ct_t", "application/vnd.wap.multipart.related");
        contentValues.put("exp", (Integer) 604800);
        contentValues.put("m_cls", "personal");
        contentValues.put("m_type", (Integer) 128);
        contentValues.put("v", (Integer) 18);
        contentValues.put("pri", (Integer) 129);
        contentValues.put("rr", (Integer) 129);
        contentValues.put("tr_id", "T" + Long.toHexString(System.currentTimeMillis()));
        contentValues.put("d_rpt", (Integer) 129);
        contentValues.put("locked", (Integer) 0);
        contentValues.put("sub_id", Long.valueOf(j));
        if (bluetoothMapbMessageMime.getTextOnly()) {
            contentValues.put("text_only", (Boolean) true);
        }
        contentValues.put("m_size", Integer.valueOf(bluetoothMapbMessageMime.getSize()));
        HashSet hashSet = new HashSet();
        hashSet.addAll(Arrays.asList(str));
        contentValues.put("thread_id", Long.valueOf(Telephony.Threads.getOrCreateThreadId(this.mContext, hashSet)));
        Uri uri = Telephony.Mms.CONTENT_URI;
        synchronized (getMsgListMms()) {
            Uri uriInsert = this.mResolver.insert(uri, contentValues);
            if (uriInsert == null) {
                Log.e(TAG, "Unabled to insert MMS " + contentValues + "Uri: " + uriInsert);
                return -1L;
            }
            Cursor cursorQuery = this.mResolver.query(uriInsert, MMS_PROJECTION_SHORT, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        long j2 = cursorQuery.getLong(cursorQuery.getColumnIndex("_id"));
                        Msg msg = new Msg(j2, cursorQuery.getInt(cursorQuery.getColumnIndex("msg_box")), cursorQuery.getInt(cursorQuery.getColumnIndex("thread_id")), cursorQuery.getInt(cursorQuery.getColumnIndex(BluetoothMapContract.FILTER_READ_STATUS)));
                        msg.localInitiatedSend = true;
                        getMsgListMms().put(Long.valueOf(j2), msg);
                        cursorQuery.close();
                    }
                } finally {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
            long j3 = Long.parseLong(uriInsert.getLastPathSegment());
            try {
                if (bluetoothMapbMessageMime.getMimeParts() == null) {
                    Log.w(TAG, "No MMS parts present...");
                } else {
                    for (BluetoothMapbMessageMime.MimePart mimePart : bluetoothMapbMessageMime.getMimeParts()) {
                        contentValues.clear();
                        if (mimePart.mContentType != null && mimePart.mContentType.toUpperCase().contains("TEXT")) {
                            contentValues.put("ct", "text/plain");
                            contentValues.put("chset", (Integer) 106);
                            if (mimePart.mPartName != null) {
                                contentValues.put("fn", mimePart.mPartName);
                                contentValues.put("name", mimePart.mPartName);
                            } else {
                                contentValues.put("fn", "text_1.txt");
                                contentValues.put("name", "text_1.txt");
                            }
                            if (mimePart.mContentId != null) {
                                contentValues.put(BluetoothMapContract.MessagePartColumns.CONTENT_ID, mimePart.mContentId);
                            } else if (mimePart.mPartName != null) {
                                contentValues.put(BluetoothMapContract.MessagePartColumns.CONTENT_ID, "<" + mimePart.mPartName + ">");
                            } else {
                                contentValues.put(BluetoothMapContract.MessagePartColumns.CONTENT_ID, "<text_1>");
                            }
                            if (mimePart.mContentLocation != null) {
                                contentValues.put("cl", mimePart.mContentLocation);
                            } else if (mimePart.mPartName != null) {
                                contentValues.put("cl", mimePart.mPartName + ".txt");
                            } else {
                                contentValues.put("cl", "text_1.txt");
                            }
                            if (mimePart.mContentDisposition != null) {
                                contentValues.put("cd", mimePart.mContentDisposition);
                            }
                            contentValues.put(BluetoothMapContract.MessagePartColumns.TEXT, mimePart.getDataAsString());
                            this.mResolver.insert(Uri.parse(Telephony.Mms.CONTENT_URI + "/" + j3 + "/part"), contentValues);
                        } else if (mimePart.mContentType != null && mimePart.mContentType.toUpperCase().contains("SMIL")) {
                            contentValues.put("seq", (Integer) (-1));
                            contentValues.put("ct", "application/smil");
                            if (mimePart.mContentId != null) {
                                contentValues.put(BluetoothMapContract.MessagePartColumns.CONTENT_ID, mimePart.mContentId);
                            } else {
                                contentValues.put(BluetoothMapContract.MessagePartColumns.CONTENT_ID, "<smil_1>");
                            }
                            if (mimePart.mContentLocation != null) {
                                contentValues.put("cl", mimePart.mContentLocation);
                            } else {
                                contentValues.put("cl", "smil_1.xml");
                            }
                            if (mimePart.mContentDisposition != null) {
                                contentValues.put("cd", mimePart.mContentDisposition);
                            }
                            contentValues.put("fn", "smil.xml");
                            contentValues.put("name", "smil.xml");
                            contentValues.put(BluetoothMapContract.MessagePartColumns.TEXT, new String(mimePart.mData, "UTF-8"));
                            this.mResolver.insert(Uri.parse(Telephony.Mms.CONTENT_URI + "/" + j3 + "/part"), contentValues);
                        } else {
                            writeMmsDataPart(j3, mimePart, 1);
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, e);
            } catch (IOException e2) {
                Log.w(TAG, e2);
            }
            contentValues.clear();
            contentValues.put("contact_id", "null");
            contentValues.put("address", BluetoothMapContent.INSERT_ADDRES_TOKEN);
            contentValues.put(EXTRA_MESSAGE_SENT_MSG_TYPE, (Integer) 137);
            contentValues.put(BluetoothMapContract.MessagePartColumns.CHARSET, (Integer) 106);
            this.mResolver.insert(Uri.parse(Telephony.Mms.CONTENT_URI + "/" + j3 + "/addr"), contentValues);
            contentValues.clear();
            contentValues.put("contact_id", "null");
            contentValues.put("address", str);
            contentValues.put(EXTRA_MESSAGE_SENT_MSG_TYPE, (Integer) 151);
            contentValues.put(BluetoothMapContract.MessagePartColumns.CHARSET, (Integer) 106);
            this.mResolver.insert(Uri.parse(Telephony.Mms.CONTENT_URI + "/" + j3 + "/addr"), contentValues);
            return j3;
        }
    }

    private void writeMmsDataPart(long j, BluetoothMapbMessageMime.MimePart mimePart, int i) throws IOException {
        ContentValues contentValues = new ContentValues();
        contentValues.put("mid", Long.valueOf(j));
        if (mimePart.mContentType != null) {
            contentValues.put("ct", mimePart.mContentType);
        } else {
            Log.w(TAG, "MMS has no CONTENT_TYPE for part " + i);
        }
        if (mimePart.mContentId != null) {
            contentValues.put(BluetoothMapContract.MessagePartColumns.CONTENT_ID, mimePart.mContentId);
        } else if (mimePart.mPartName != null) {
            contentValues.put(BluetoothMapContract.MessagePartColumns.CONTENT_ID, "<" + mimePart.mPartName + ">");
        } else {
            contentValues.put(BluetoothMapContract.MessagePartColumns.CONTENT_ID, "<part_" + i + ">");
        }
        if (mimePart.mContentLocation != null) {
            contentValues.put("cl", mimePart.mContentLocation);
        } else if (mimePart.mPartName != null) {
            contentValues.put("cl", mimePart.mPartName + ".dat");
        } else {
            contentValues.put("cl", "part_" + i + ".dat");
        }
        if (mimePart.mContentDisposition != null) {
            contentValues.put("cd", mimePart.mContentDisposition);
        }
        if (mimePart.mPartName != null) {
            contentValues.put("fn", mimePart.mPartName);
            contentValues.put("name", mimePart.mPartName);
        } else {
            contentValues.put("fn", "part_" + i + ".dat");
            contentValues.put("name", "part_" + i + ".dat");
        }
        OutputStream outputStreamOpenOutputStream = this.mResolver.openOutputStream(this.mResolver.insert(Uri.parse(Telephony.Mms.CONTENT_URI + "/" + j + "/part"), contentValues));
        outputStreamOpenOutputStream.write(mimePart.mData);
        outputStreamOpenOutputStream.close();
    }

    public void sendMessage(PushMsgInfo pushMsgInfo, String str, long j) {
        if (BluetoothMapSimManager.getSubInfoNumber() <= 0) {
            Log.d(TAG, "sendMessage empty subid");
            return;
        }
        SmsManager smsManagerForSubscriptionId = SmsManager.getSmsManagerForSubscriptionId((int) j);
        ArrayList<String> arrayListDivideMessage = smsManagerForSubscriptionId.divideMessage(str);
        pushMsgInfo.parts = arrayListDivideMessage.size();
        pushMsgInfo.timestamp = Calendar.getInstance().getTime().getTime();
        pushMsgInfo.partsDelivered = 0;
        pushMsgInfo.partsSent = 0;
        ArrayList arrayList = new ArrayList(pushMsgInfo.parts);
        ArrayList arrayList2 = new ArrayList(pushMsgInfo.parts);
        if (arrayListDivideMessage != null && arrayListDivideMessage.size() > 0) {
            for (int i = 0; i < pushMsgInfo.parts; i++) {
                Intent intent = new Intent(ACTION_MESSAGE_DELIVERY, (Uri) null);
                intent.setType("message/" + Long.toString(pushMsgInfo.id) + pushMsgInfo.timestamp + i);
                intent.putExtra(EXTRA_MESSAGE_SENT_HANDLE, pushMsgInfo.id);
                intent.putExtra("timestamp", pushMsgInfo.timestamp);
                PendingIntent broadcast = PendingIntent.getBroadcast(this.mContext, 0, intent, VCardConfig.FLAG_CONVERT_PHONETIC_NAME_STRINGS);
                Intent intent2 = new Intent(ACTION_MESSAGE_SENT, (Uri) null);
                intent2.setType("message/" + Long.toString(pushMsgInfo.id) + pushMsgInfo.timestamp + i);
                intent2.putExtra(EXTRA_MESSAGE_SENT_HANDLE, pushMsgInfo.id);
                intent2.putExtra("uri", pushMsgInfo.uri.toString());
                intent2.putExtra(EXTRA_MESSAGE_SENT_RETRY, pushMsgInfo.retry);
                intent2.putExtra(EXTRA_MESSAGE_SENT_TRANSPARENT, pushMsgInfo.transparent);
                PendingIntent broadcast2 = PendingIntent.getBroadcast(this.mContext, 0, intent2, VCardConfig.FLAG_CONVERT_PHONETIC_NAME_STRINGS);
                arrayList.add(broadcast);
                arrayList2.add(broadcast2);
            }
            Log.d(TAG, "sendMessage to " + pushMsgInfo.phone);
            if (arrayListDivideMessage.size() == 1) {
                smsManagerForSubscriptionId.sendTextMessageWithoutPersisting(pushMsgInfo.phone, null, arrayListDivideMessage.get(0), (PendingIntent) arrayList2.get(0), (PendingIntent) arrayList.get(0));
            } else {
                smsManagerForSubscriptionId.sendMultipartTextMessageWithoutPersisting(pushMsgInfo.phone, null, arrayListDivideMessage, arrayList2, arrayList);
            }
        }
    }

    private class SmsBroadcastReceiver extends BroadcastReceiver {
        private SmsBroadcastReceiver() {
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
            long longExtra = intent.getLongExtra(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_HANDLE, -1L);
            PushMsgInfo pushMsgInfo = (PushMsgInfo) BluetoothMapContentObserver.this.mPushMsgList.get(Long.valueOf(longExtra));
            Log.d(BluetoothMapContentObserver.TAG, "onReceive: action" + action);
            if (pushMsgInfo == null) {
                Log.d(BluetoothMapContentObserver.TAG, "onReceive: no msgInfo found for handle " + longExtra);
                return;
            }
            if (action.equals(BluetoothMapContentObserver.ACTION_MESSAGE_SENT)) {
                int intExtra = intent.getIntExtra(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_RESULT, 0);
                pushMsgInfo.partsSent++;
                if (intExtra != -1) {
                    pushMsgInfo.failedSent = true;
                }
                if (BluetoothMapContentObserver.D) {
                    Log.d(BluetoothMapContentObserver.TAG, "onReceive: msgInfo.partsSent = " + pushMsgInfo.partsSent + ", msgInfo.parts = " + pushMsgInfo.parts + " result = " + intExtra);
                }
                if (pushMsgInfo.partsSent == pushMsgInfo.parts) {
                    actionMessageSent(context, intent, pushMsgInfo, longExtra);
                }
            } else if (action.equals(BluetoothMapContentObserver.ACTION_MESSAGE_DELIVERY)) {
                if (pushMsgInfo.timestamp == intent.getLongExtra("timestamp", 0L)) {
                    pushMsgInfo.partsDelivered++;
                    byte[] byteArrayExtra = intent.getByteArrayExtra("pdu");
                    String stringExtra = intent.getStringExtra("format");
                    SmsMessage smsMessageCreateFromPdu = SmsMessage.createFromPdu(byteArrayExtra, stringExtra);
                    if (smsMessageCreateFromPdu == null) {
                        Log.d(BluetoothMapContentObserver.TAG, "actionMessageDelivery: Can't get message from pdu");
                        return;
                    }
                    int status = smsMessageCreateFromPdu.getStatus();
                    if (!TextUtils.isEmpty(stringExtra) && stringExtra.equals("3gpp2")) {
                        status >>= 16;
                        if (BluetoothMapContentObserver.D) {
                            Log.d(BluetoothMapContentObserver.TAG, "onReceive actionMessageDelivery: 3gpp2 pdu " + status);
                        }
                    }
                    if (status == 2) {
                        status = 0;
                    }
                    if (status == 0) {
                        Telephony.Sms.moveMessageToFolder(BluetoothMapContentObserver.this.mContext, pushMsgInfo.uri, 2, 0);
                    } else {
                        pushMsgInfo.statusDelivered = status;
                        if (BluetoothMapContentObserver.D) {
                            Log.d(BluetoothMapContentObserver.TAG, "msgInfo.statusDelivered = " + status);
                        }
                        Telephony.Sms.moveMessageToFolder(BluetoothMapContentObserver.this.mContext, pushMsgInfo.uri, 5, 0);
                    }
                }
                if (pushMsgInfo.partsDelivered == pushMsgInfo.parts) {
                    actionMessageDelivery(context, intent, pushMsgInfo);
                }
            } else {
                Log.d(BluetoothMapContentObserver.TAG, "[onReceive]: Unknown action " + action);
            }
            if (pushMsgInfo.partsSent == pushMsgInfo.parts && pushMsgInfo.partsDelivered == pushMsgInfo.parts) {
                BluetoothMapContentObserver.this.mPushMsgList.remove(Long.valueOf(pushMsgInfo.id));
            }
        }

        private void actionMessageSent(Context context, Intent intent, PushMsgInfo pushMsgInfo, long j) {
            if (BluetoothMapContentObserver.D) {
                Log.d(BluetoothMapContentObserver.TAG, "actionMessageSent(): msgInfo.failedSent = " + pushMsgInfo.failedSent);
            }
            boolean z = false;
            pushMsgInfo.sendInProgress = false;
            if (!pushMsgInfo.failedSent) {
                if (BluetoothMapContentObserver.D) {
                    Log.d(BluetoothMapContentObserver.TAG, "actionMessageSent: result OK");
                }
                if (pushMsgInfo.transparent == 0) {
                    if (!Telephony.Sms.moveMessageToFolder(context, pushMsgInfo.uri, 2, 0)) {
                        Log.w(BluetoothMapContentObserver.TAG, "Failed to move " + pushMsgInfo.uri + " to SENT");
                    }
                } else {
                    z = true;
                }
                BluetoothMapContentObserver.this.sendEvent(BluetoothMapContentObserver.this.new Event(BluetoothMapContentObserver.EVENT_TYPE_SENDING_SUCCESS, pushMsgInfo.id, BluetoothMapContentObserver.getSmsFolderName(2), null, BluetoothMapContentObserver.this.mSmsType));
            } else if (pushMsgInfo.retry == 1) {
                pushMsgInfo.resend = true;
                pushMsgInfo.partsSent = 0;
                pushMsgInfo.failedSent = false;
                BluetoothMapContentObserver.this.sendEvent(BluetoothMapContentObserver.this.new Event(BluetoothMapContentObserver.EVENT_TYPE_SENDING_FAILURE, pushMsgInfo.id, BluetoothMapContentObserver.getSmsFolderName(4), null, BluetoothMapContentObserver.this.mSmsType));
            } else {
                if (pushMsgInfo.transparent == 0) {
                    if (!Telephony.Sms.moveMessageToFolder(context, pushMsgInfo.uri, 5, 0)) {
                        Log.w(BluetoothMapContentObserver.TAG, "Failed to move " + pushMsgInfo.uri + " to FAILED");
                    }
                } else {
                    z = true;
                }
                BluetoothMapContentObserver.this.sendEvent(BluetoothMapContentObserver.this.new Event(BluetoothMapContentObserver.EVENT_TYPE_SENDING_FAILURE, pushMsgInfo.id, BluetoothMapContentObserver.getSmsFolderName(5), null, BluetoothMapContentObserver.this.mSmsType));
            }
            if (z) {
                synchronized (BluetoothMapContentObserver.this.getMsgListSms()) {
                    BluetoothMapContentObserver.this.getMsgListSms().remove(Long.valueOf(pushMsgInfo.id));
                }
                BluetoothMapContentObserver.this.mResolver.delete(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, j), null, null);
            }
        }

        private void actionMessageDelivery(Context context, Intent intent, PushMsgInfo pushMsgInfo) {
            int i;
            Uri data = intent.getData();
            pushMsgInfo.sendInProgress = false;
            Cursor cursorQuery = BluetoothMapContentObserver.this.mResolver.query(pushMsgInfo.uri, BluetoothMapContentObserver.ID_PROJECTION, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        Uri uriWithAppendedId = ContentUris.withAppendedId(BluetoothMapContentObserver.UPDATE_STATUS_URI, cursorQuery.getInt(0));
                        byte[] byteArrayExtra = intent.getByteArrayExtra("pdu");
                        String stringExtra = intent.getStringExtra("format");
                        SmsMessage smsMessageCreateFromPdu = SmsMessage.createFromPdu(byteArrayExtra, stringExtra);
                        if (smsMessageCreateFromPdu == null) {
                            Log.d(BluetoothMapContentObserver.TAG, "actionMessageDelivery: Can't get message from pdu");
                            if (cursorQuery == null) {
                                return;
                            }
                            cursorQuery.close();
                            return;
                        }
                        int status = smsMessageCreateFromPdu.getStatus();
                        if (!TextUtils.isEmpty(stringExtra) && stringExtra.equals("3gpp2")) {
                            i = status >> 16;
                            if (BluetoothMapContentObserver.D) {
                                Log.d(BluetoothMapContentObserver.TAG, "actionMessageDelivery: 3gpp2 pdu " + i);
                            }
                        } else {
                            i = status;
                        }
                        if (i == 2) {
                            i = 0;
                        }
                        pushMsgInfo.statusDelivered = i;
                        if (BluetoothMapContentObserver.D) {
                            Log.d(BluetoothMapContentObserver.TAG, "actionMessageDelivery: uri=" + data + ", status=" + pushMsgInfo.statusDelivered);
                        }
                        ContentValues contentValues = new ContentValues(3);
                        contentValues.put("status", Integer.valueOf(pushMsgInfo.statusDelivered));
                        contentValues.put("date_sent", Long.valueOf(System.currentTimeMillis()));
                        contentValues.put(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE, (Integer) 2);
                        BluetoothMapContentObserver.this.mResolver.update(uriWithAppendedId, contentValues, null, null);
                    } else {
                        Log.d(BluetoothMapContentObserver.TAG, "Can't find message for status update: " + data);
                    }
                } finally {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
            if (pushMsgInfo.statusDelivered == 0) {
                BluetoothMapContentObserver.this.sendEvent(BluetoothMapContentObserver.this.new Event(BluetoothMapContentObserver.EVENT_TYPE_DELEVERY_SUCCESS, pushMsgInfo.id, BluetoothMapContentObserver.getSmsFolderName(2), null, BluetoothMapContentObserver.this.mSmsType));
            } else {
                BluetoothMapContentObserver.this.sendEvent(BluetoothMapContentObserver.this.new Event(BluetoothMapContentObserver.EVENT_TYPE_DELIVERY_FAILURE, pushMsgInfo.id, BluetoothMapContentObserver.getSmsFolderName(2), null, BluetoothMapContentObserver.this.mSmsType));
            }
        }
    }

    private class CeBroadcastReceiver extends BroadcastReceiver {
        private CeBroadcastReceiver() {
        }

        public void register() {
            UserManager userManager = UserManager.get(BluetoothMapContentObserver.this.mContext);
            if (userManager == null || userManager.isUserUnlocked()) {
                BluetoothMapContentObserver.this.mStorageUnlocked = true;
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
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
            Log.d(BluetoothMapContentObserver.TAG, "onReceive: action" + action);
            if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                try {
                    BluetoothMapContentObserver.this.initMsgList();
                } catch (RemoteException e) {
                    Log.e(BluetoothMapContentObserver.TAG, "Error initializing SMS/MMS message lists.");
                }
                Iterator it = BluetoothMapContentObserver.FOLDER_SMS_MAP.values().iterator();
                while (it.hasNext()) {
                    BluetoothMapContentObserver.this.sendEvent(BluetoothMapContentObserver.this.new Event(BluetoothMapContentObserver.EVENT_TYPE_NEW, -1L, (String) it.next(), BluetoothMapContentObserver.this.mSmsType));
                }
                BluetoothMapContentObserver.this.mStorageUnlocked = true;
                unregister();
                return;
            }
            Log.d(BluetoothMapContentObserver.TAG, "onReceive: Unknown action " + action);
        }
    }

    public static void actionMmsSent(Context context, Intent intent, int i, Map<Long, Msg> map) {
        if (D) {
            Log.d(TAG, "actionMmsSent()");
        }
        int intExtra = intent.getIntExtra(EXTRA_MESSAGE_SENT_TRANSPARENT, 0);
        long longExtra = intent.getLongExtra(EXTRA_MESSAGE_SENT_HANDLE, -1L);
        if (longExtra < 0) {
            Log.w(TAG, "Intent received for an invalid handle");
            return;
        }
        ContentResolver contentResolver = context.getContentResolver();
        if (intExtra == 1) {
            Uri uriWithAppendedId = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, longExtra);
            if (map != null) {
                synchronized (map) {
                    map.remove(Long.valueOf(longExtra));
                }
            }
            if (D) {
                Log.d(TAG, "Transparent in use - delete");
            }
            contentResolver.delete(uriWithAppendedId, null, null);
            return;
        }
        if (i == -1) {
            moveMmsToFolder(longExtra, contentResolver, 2);
            return;
        }
        if (map != null) {
            synchronized (map) {
                Msg msg = map.get(Long.valueOf(longExtra));
                if (msg != null) {
                    msg.type = 4;
                }
            }
        }
        moveMmsToFolder(longExtra, contentResolver, 4);
    }

    public static void actionMessageSentDisconnected(Context context, Intent intent, int i) {
        if (BluetoothMapUtils.TYPE.fromOrdinal(intent.getIntExtra(EXTRA_MESSAGE_SENT_MSG_TYPE, BluetoothMapUtils.TYPE.NONE.ordinal())) == BluetoothMapUtils.TYPE.MMS) {
            actionMmsSent(context, intent, i, null);
        } else {
            actionSmsSentDisconnected(context, intent, i);
        }
    }

    public static void actionSmsSentDisconnected(Context context, Intent intent, int i) {
        if (Binder.getCallingPid() != Process.myPid() || context.checkCallingOrSelfPermission("android.Manifest.permission.WRITE_SMS") != 0) {
            Log.w(TAG, "actionSmsSentDisconnected: Not allowed to delete SMS/MMS messages");
            return;
        }
        int intExtra = intent.getIntExtra(EXTRA_MESSAGE_SENT_TRANSPARENT, 0);
        String stringExtra = intent.getStringExtra("uri");
        if (stringExtra == null) {
            return;
        }
        Uri uri = Uri.parse(stringExtra);
        boolean z = true;
        if (i == -1) {
            Log.d(TAG, "actionMessageSentDisconnected: result OK");
            if (intExtra == 0) {
                if (!Telephony.Sms.moveMessageToFolder(context, uri, 2, 0)) {
                    Log.d(TAG, "Failed to move " + uri + " to SENT");
                }
                z = false;
            }
        } else if (intExtra == 0) {
            if (!Telephony.Sms.moveMessageToFolder(context, uri, 5, 0)) {
                Log.d(TAG, "Failed to move " + uri + " to FAILED");
            }
            z = false;
        }
        if (z) {
            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null) {
                contentResolver.delete(uri, null, null);
            } else {
                Log.w(TAG, "Unable to get resolver");
            }
        }
    }

    private void registerPhoneServiceStateListener() {
        ((TelephonyManager) this.mContext.getSystemService("phone")).listen(this.mPhoneListener, 1);
    }

    private void unRegisterPhoneServiceStateListener() {
        ((TelephonyManager) this.mContext.getSystemService("phone")).listen(this.mPhoneListener, 0);
    }

    private void resendPendingMessages() {
        UserManager userManager = UserManager.get(this.mContext);
        if (userManager != null && userManager.isUserUnlocked()) {
            Cursor cursorQuery = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, "type = 4", null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        do {
                            long j = cursorQuery.getLong(cursorQuery.getColumnIndex("_id"));
                            String string = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.BODY));
                            long j2 = cursorQuery.getLong(cursorQuery.getColumnIndex("sub_id"));
                            PushMsgInfo pushMsgInfo = this.mPushMsgList.get(Long.valueOf(j));
                            if (pushMsgInfo != null && pushMsgInfo.resend && !pushMsgInfo.sendInProgress) {
                                pushMsgInfo.sendInProgress = true;
                                sendMessage(pushMsgInfo, string, j2);
                            }
                        } while (cursorQuery.moveToNext());
                    }
                } finally {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
        }
    }

    private void failPendingMessages() {
        Cursor cursorQuery = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, "type = 4", null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    do {
                        long j = cursorQuery.getLong(cursorQuery.getColumnIndex("_id"));
                        cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.BODY));
                        PushMsgInfo pushMsgInfo = this.mPushMsgList.get(Long.valueOf(j));
                        if (pushMsgInfo != null && pushMsgInfo.resend) {
                            Telephony.Sms.moveMessageToFolder(this.mContext, pushMsgInfo.uri, 5, 0);
                        }
                    } while (cursorQuery.moveToNext());
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
    }

    private void removeDeletedMessages() {
        this.mResolver.delete(Telephony.Sms.CONTENT_URI, "thread_id = -1", null);
    }

    public void init() {
        if (this.mSmsBroadcastReceiver != null) {
            this.mSmsBroadcastReceiver.register();
        }
        if (this.mCeBroadcastReceiver != null) {
            this.mCeBroadcastReceiver.register();
        }
        registerPhoneServiceStateListener();
        this.mInitialized = true;
    }

    public void deinit() {
        this.mInitialized = false;
        unregisterObserver();
        if (this.mSmsBroadcastReceiver != null) {
            this.mSmsBroadcastReceiver.unregister();
        }
        unRegisterPhoneServiceStateListener();
        if (UserManager.get(this.mContext).isUserUnlocked()) {
            failPendingMessages();
            removeDeletedMessages();
        }
        this.mPushMsgList.clear();
    }

    public boolean handleSmsSendIntent(Context context, Intent intent) {
        if (BluetoothMapUtils.TYPE.fromOrdinal(intent.getIntExtra(EXTRA_MESSAGE_SENT_MSG_TYPE, BluetoothMapUtils.TYPE.NONE.ordinal())) == BluetoothMapUtils.TYPE.MMS) {
            return handleMmsSendIntent(context, intent);
        }
        if (this.mInitialized) {
            this.mSmsBroadcastReceiver.onReceive(context, intent);
            return true;
        }
        return false;
    }

    public boolean handleMmsSendIntent(Context context, Intent intent) {
        if (D) {
            Log.w(TAG, "handleMmsSendIntent()");
        }
        if (!this.mMnsClient.isConnected()) {
            if (D) {
                Log.w(TAG, "MNS not connected - use static handling");
            }
            return false;
        }
        long longExtra = intent.getLongExtra(EXTRA_MESSAGE_SENT_HANDLE, -1L);
        int intExtra = intent.getIntExtra(EXTRA_MESSAGE_SENT_RESULT, 0);
        actionMmsSent(context, intent, intExtra, getMsgListMms());
        if (longExtra < 0) {
            Log.w(TAG, "Intent received for an invalid handle");
            return true;
        }
        if (intExtra != -1) {
            if (this.mObserverRegistered) {
                sendEvent(new Event(EVENT_TYPE_SENDING_FAILURE, longExtra, getMmsFolderName(4), null, BluetoothMapUtils.TYPE.MMS));
            }
        } else if (intent.getIntExtra(EXTRA_MESSAGE_SENT_TRANSPARENT, 0) != 0 && this.mObserverRegistered) {
            sendEvent(new Event(EVENT_TYPE_SENDING_SUCCESS, longExtra, getMmsFolderName(4), null, BluetoothMapUtils.TYPE.MMS));
        }
        return true;
    }

    private String queryNumber(Context context, String str) {
        String string = "";
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        String strValueOf = "";
        Uri uri = ContactsContract.Data.CONTENT_URI;
        Cursor cursorQuery = context.getContentResolver().query(uri, new String[]{"raw_contact_id"}, "mimetype='vnd.android.cursor.item/name' AND data1='" + str + "'", null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() == 1 && cursorQuery.moveToFirst()) {
                    strValueOf = String.valueOf(cursorQuery.getInt(0));
                }
            } finally {
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        cursorQuery = context.getContentResolver().query(uri, new String[]{"data1"}, "mimetype='vnd.android.cursor.item/phone_v2' AND raw_contact_id=?", new String[]{strValueOf}, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() == 1 && cursorQuery.moveToFirst()) {
                    string = cursorQuery.getString(0);
                }
            } finally {
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return string;
    }
}
