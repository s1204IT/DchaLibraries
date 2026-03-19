package com.android.bluetooth.map;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import com.android.bluetooth.DeviceWorkArounds;
import com.android.bluetooth.SignedLongLong;
import com.android.bluetooth.map.BluetoothMapUtils;
import com.android.bluetooth.map.BluetoothMapbMessageMime;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.bluetooth.opp.BluetoothShare;
import com.google.android.mms.pdu.CharacterSets;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@TargetApi(19)
public class BluetoothMapContent {
    public static final long CONVO_PARAMETER_MASK_ALL_ENABLED = 4294967295L;
    public static final long CONVO_PARAMETER_MASK_DEFAULT = 225;
    private static final int CONVO_PARAM_MASK_CONVO_LAST_ACTIVITY = 2;
    private static final int CONVO_PARAM_MASK_CONVO_NAME = 1;
    private static final int CONVO_PARAM_MASK_CONVO_READ_STATUS = 4;
    private static final int CONVO_PARAM_MASK_CONVO_SUMMARY = 16;
    private static final int CONVO_PARAM_MASK_CONVO_VERSION_COUNTER = 8;
    private static final int CONVO_PARAM_MASK_PARTTICIPANTS = 32;
    private static final int CONVO_PARAM_MASK_PART_CHAT_STATE = 256;
    private static final int CONVO_PARAM_MASK_PART_DISP_NAME = 128;
    private static final int CONVO_PARAM_MASK_PART_LAST_ACTIVITY = 512;
    private static final int CONVO_PARAM_MASK_PART_NAME = 2048;
    private static final int CONVO_PARAM_MASK_PART_PRESENCE = 4096;
    private static final int CONVO_PARAM_MASK_PART_PRESENCE_TEXT = 8192;
    private static final int CONVO_PARAM_MASK_PART_PRIORITY = 16384;
    private static final int CONVO_PARAM_MASK_PART_UCI = 64;
    private static final int CONVO_PARAM_MASK_PART_X_BT_UID = 1024;
    private static final int FILTER_READ_STATUS_ALL = 0;
    private static final int FILTER_READ_STATUS_READ_ONLY = 2;
    private static final int FILTER_READ_STATUS_UNREAD_ONLY = 1;
    public static final String INSERT_ADDRES_TOKEN = "insert-address-token";
    public static final Uri MAP_CONTENT_URI;
    public static final int MAP_MESSAGE_CHARSET_NATIVE = 0;
    public static final int MAP_MESSAGE_CHARSET_UTF8 = 1;
    private static final int MASK_ATTACHMENT_MIME = 4194304;
    private static final int MASK_ATTACHMENT_SIZE = 1024;
    private static final int MASK_CONVERSATION_ID = 262144;
    private static final int MASK_CONVERSATION_NAME = 524288;
    private static final int MASK_DATETIME = 2;
    private static final int MASK_DELIVERY_STATUS = 131072;
    private static final int MASK_FOLDER_TYPE = 1048576;
    private static final int MASK_PRIORITY = 2048;
    private static final int MASK_PROTECTED = 16384;
    private static final int MASK_READ = 4096;
    private static final int MASK_RECEPTION_STATUS = 256;
    private static final int MASK_RECIPIENT_ADDRESSING = 32;
    private static final int MASK_RECIPIENT_NAME = 16;
    private static final int MASK_REPLYTO_ADDRESSING = 32768;
    private static final int MASK_SENDER_ADDRESSING = 8;
    private static final int MASK_SENDER_NAME = 4;
    private static final int MASK_SENT = 8192;
    private static final int MASK_SIZE = 128;
    private static final int MASK_SUBJECT = 1;
    private static final int MASK_TEXT = 512;
    private static final int MASK_TYPE = 64;
    public static final Uri MMSSMS_CONTENT_URI;
    public static final int MMS_BCC = 129;
    public static final int MMS_CC = 130;
    public static final int MMS_FROM = 137;
    private static final int MMS_SMS_THREAD_COL_DATE;
    private static final int MMS_SMS_THREAD_COL_ID;
    private static final int MMS_SMS_THREAD_COL_READ;
    private static final int MMS_SMS_THREAD_COL_RECIPIENT_IDS;
    private static final int MMS_SMS_THREAD_COL_SNIPPET;
    private static final int MMS_SMS_THREAD_COL_SNIPPET_CS;
    public static final int MMS_TO = 151;
    public static final long PARAMETER_MASK_ALL_ENABLED = 4294967295L;
    private static final String[] RECIPIENT_ID_PROJECTION;
    static final String[] SIMID_PROJECTION;
    private static final String TAG = "BluetoothMapContent";
    static final String[] THREADID_PROJECTION;
    private static final boolean V = false;
    private final BluetoothMapAccountItem mAccount;
    private final String mBaseUri;
    private final Context mContext;
    private final BluetoothMapMasInstance mMasInstance;
    private final ContentResolver mResolver;
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final String INTERESTED_MESSAGE_TYPE_CLAUSE = String.format("( %s = %d OR %s = %d OR %s = %d )", "m_type", 128, "m_type", 132, "m_type", 130);
    static final String[] SMS_PROJECTION = {"_id", "thread_id", "address", BluetoothMapContract.MessageColumns.BODY, BluetoothMapContract.MessageColumns.DATE, BluetoothMapContract.FILTER_READ_STATUS, BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE, "status", "locked", "error_code", "sub_id"};
    static final String[] MMS_PROJECTION = {"_id", "thread_id", "m_id", "m_size", "sub", "ct_t", "text_only", BluetoothMapContract.MessageColumns.DATE, "date_sent", BluetoothMapContract.FILTER_READ_STATUS, "msg_box", "st", "pri"};
    static final String[] SMS_CONVO_PROJECTION = {"_id", "thread_id", "address", BluetoothMapContract.MessageColumns.DATE, BluetoothMapContract.FILTER_READ_STATUS, BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE, "status", "locked", "error_code"};
    static final String[] MMS_CONVO_PROJECTION = {"_id", "thread_id", "m_id", "m_size", "sub", "ct_t", "text_only", BluetoothMapContract.MessageColumns.DATE, "date_sent", BluetoothMapContract.FILTER_READ_STATUS, "msg_box", "st", "pri", "address"};
    private static final String[] MMS_SMS_THREAD_PROJECTION = {"_id", BluetoothMapContract.MessageColumns.DATE, "snippet", "snippet_cs", BluetoothMapContract.FILTER_READ_STATUS, "recipient_ids"};
    private static final String[] CONVO_VERSION_PROJECTION = {"thread_id", "thread_name", BluetoothMapContract.ConversationColumns.READ_STATUS, BluetoothMapContract.ConversationColumns.LAST_THREAD_ACTIVITY, BluetoothMapContract.ConversationColumns.SUMMARY};
    private String mMessageVersion = "1.0";
    private int mRemoteFeatureMask = 31;
    private int mMsgListingVersion = 10;

    static {
        List listAsList = Arrays.asList(MMS_SMS_THREAD_PROJECTION);
        MMS_SMS_THREAD_COL_ID = listAsList.indexOf("_id");
        MMS_SMS_THREAD_COL_DATE = listAsList.indexOf(BluetoothMapContract.MessageColumns.DATE);
        MMS_SMS_THREAD_COL_SNIPPET = listAsList.indexOf("snippet");
        MMS_SMS_THREAD_COL_SNIPPET_CS = listAsList.indexOf("snippet_cs");
        MMS_SMS_THREAD_COL_READ = listAsList.indexOf(BluetoothMapContract.FILTER_READ_STATUS);
        MMS_SMS_THREAD_COL_RECIPIENT_IDS = listAsList.indexOf("recipient_ids");
        THREADID_PROJECTION = new String[]{"_id"};
        SIMID_PROJECTION = new String[]{"transport_type", "_id", "sub_id", "sub_id"};
        RECIPIENT_ID_PROJECTION = new String[]{"recipient_ids"};
        MAP_CONTENT_URI = Uri.withAppendedPath(Telephony.MmsSms.CONTENT_URI, "conversations/map");
        MMSSMS_CONTENT_URI = Uri.withAppendedPath(Telephony.MmsSms.CONTENT_URI, "conversations");
    }

    private class FilterInfo {
        public static final int TYPE_EMAIL = 2;
        public static final int TYPE_IM = 3;
        public static final int TYPE_MMS = 1;
        public static final int TYPE_SMS = 0;
        public int mContactColBtUid;
        public int mContactColChatState;
        public int mContactColContactUci;
        public int mContactColLastActive;
        public int mContactColName;
        public int mContactColNickname;
        public int mContactColPresenceState;
        public int mContactColPresenceText;
        public int mContactColPriority;
        public int mConvoColConvoId;
        public int mConvoColLastActivity;
        public int mConvoColName;
        public int mConvoColRead;
        public int mConvoColSummary;
        public int mConvoColVersionCounter;
        public int mMessageColAccountId;
        public int mMessageColAttachment;
        public int mMessageColAttachmentMime;
        public int mMessageColAttachmentSize;
        public int mMessageColBccAddress;
        public int mMessageColBody;
        public int mMessageColCcAddress;
        public int mMessageColDate;
        public int mMessageColDelivery;
        public int mMessageColFolder;
        public int mMessageColFromAddress;
        public int mMessageColId;
        public int mMessageColPriority;
        public int mMessageColProtected;
        public int mMessageColRead;
        public int mMessageColReception;
        public int mMessageColReplyTo;
        public int mMessageColSize;
        public int mMessageColSubject;
        public int mMessageColThreadId;
        public int mMessageColThreadName;
        public int mMessageColToAddress;
        public int mMmsColAttachmentSize;
        public int mMmsColDate;
        public int mMmsColFolder;
        public int mMmsColId;
        public int mMmsColRead;
        public int mMmsColSize;
        public int mMmsColSubject;
        public int mMmsColTextOnly;
        public int mMmsColThreadId;
        int mMsgType;
        String mPhoneAlphaTag;
        String mPhoneNum;
        int mPhoneType;
        public int mSmsColAddress;
        public int mSmsColDate;
        public int mSmsColFolder;
        public int mSmsColId;
        public int mSmsColRead;
        public int mSmsColSubject;
        public int mSmsColThreadId;
        public int mSmsColType;

        private FilterInfo() {
            this.mMsgType = 0;
            this.mPhoneType = 0;
            this.mPhoneNum = null;
            this.mPhoneAlphaTag = null;
            this.mMessageColId = -1;
            this.mMessageColDate = -1;
            this.mMessageColBody = -1;
            this.mMessageColSubject = -1;
            this.mMessageColFolder = -1;
            this.mMessageColRead = -1;
            this.mMessageColSize = -1;
            this.mMessageColFromAddress = -1;
            this.mMessageColToAddress = -1;
            this.mMessageColCcAddress = -1;
            this.mMessageColBccAddress = -1;
            this.mMessageColReplyTo = -1;
            this.mMessageColAccountId = -1;
            this.mMessageColAttachment = -1;
            this.mMessageColAttachmentSize = -1;
            this.mMessageColAttachmentMime = -1;
            this.mMessageColPriority = -1;
            this.mMessageColProtected = -1;
            this.mMessageColReception = -1;
            this.mMessageColDelivery = -1;
            this.mMessageColThreadId = -1;
            this.mMessageColThreadName = -1;
            this.mSmsColFolder = -1;
            this.mSmsColRead = -1;
            this.mSmsColId = -1;
            this.mSmsColSubject = -1;
            this.mSmsColAddress = -1;
            this.mSmsColDate = -1;
            this.mSmsColType = -1;
            this.mSmsColThreadId = -1;
            this.mMmsColRead = -1;
            this.mMmsColFolder = -1;
            this.mMmsColAttachmentSize = -1;
            this.mMmsColTextOnly = -1;
            this.mMmsColId = -1;
            this.mMmsColSize = -1;
            this.mMmsColDate = -1;
            this.mMmsColSubject = -1;
            this.mMmsColThreadId = -1;
            this.mConvoColConvoId = -1;
            this.mConvoColLastActivity = -1;
            this.mConvoColName = -1;
            this.mConvoColRead = -1;
            this.mConvoColVersionCounter = -1;
            this.mConvoColSummary = -1;
            this.mContactColBtUid = -1;
            this.mContactColChatState = -1;
            this.mContactColContactUci = -1;
            this.mContactColNickname = -1;
            this.mContactColLastActive = -1;
            this.mContactColName = -1;
            this.mContactColPresenceState = -1;
            this.mContactColPresenceText = -1;
            this.mContactColPriority = -1;
        }

        FilterInfo(BluetoothMapContent bluetoothMapContent, AnonymousClass1 anonymousClass1) {
            this();
        }

        public void setMessageColumns(Cursor cursor) {
            this.mMessageColId = cursor.getColumnIndex("_id");
            this.mMessageColDate = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.DATE);
            this.mMessageColSubject = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.SUBJECT);
            this.mMessageColFolder = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.FOLDER_ID);
            this.mMessageColRead = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.FLAG_READ);
            this.mMessageColSize = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.MESSAGE_SIZE);
            this.mMessageColFromAddress = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.FROM_LIST);
            this.mMessageColToAddress = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.TO_LIST);
            this.mMessageColAttachment = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.FLAG_ATTACHMENT);
            this.mMessageColAttachmentSize = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.ATTACHMENT_SIZE);
            this.mMessageColPriority = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.FLAG_HIGH_PRIORITY);
            this.mMessageColProtected = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.FLAG_PROTECTED);
            this.mMessageColReception = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.RECEPTION_STATE);
            this.mMessageColDelivery = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.DEVILERY_STATE);
            this.mMessageColThreadId = cursor.getColumnIndex("thread_id");
        }

        public void setEmailMessageColumns(Cursor cursor) {
            setMessageColumns(cursor);
            this.mMessageColCcAddress = cursor.getColumnIndex(BluetoothMapContract.EmailMessageColumns.CC_LIST);
            this.mMessageColBccAddress = cursor.getColumnIndex(BluetoothMapContract.EmailMessageColumns.BCC_LIST);
            this.mMessageColReplyTo = cursor.getColumnIndex(BluetoothMapContract.EmailMessageColumns.REPLY_TO_LIST);
        }

        public void setImMessageColumns(Cursor cursor) {
            setMessageColumns(cursor);
            this.mMessageColThreadName = cursor.getColumnIndex("thread_name");
            this.mMessageColAttachmentMime = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.ATTACHMENT_MINE_TYPES);
            this.mMessageColBody = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.BODY);
        }

        public void setEmailImConvoColumns(Cursor cursor) {
            this.mConvoColConvoId = cursor.getColumnIndex("thread_id");
            this.mConvoColLastActivity = cursor.getColumnIndex(BluetoothMapContract.ConversationColumns.LAST_THREAD_ACTIVITY);
            this.mConvoColName = cursor.getColumnIndex("thread_name");
            this.mConvoColRead = cursor.getColumnIndex(BluetoothMapContract.ConversationColumns.READ_STATUS);
            this.mConvoColVersionCounter = cursor.getColumnIndex(BluetoothMapContract.ConversationColumns.VERSION_COUNTER);
            this.mConvoColSummary = cursor.getColumnIndex(BluetoothMapContract.ConversationColumns.SUMMARY);
            setEmailImConvoContactColumns(cursor);
        }

        public void setEmailImConvoContactColumns(Cursor cursor) {
            this.mContactColBtUid = cursor.getColumnIndex(BluetoothMapContract.ConvoContactColumns.X_BT_UID);
            this.mContactColChatState = cursor.getColumnIndex(BluetoothMapContract.ChatStatusColumns.CHAT_STATE);
            this.mContactColContactUci = cursor.getColumnIndex(BluetoothMapContract.ConvoContactColumns.UCI);
            this.mContactColNickname = cursor.getColumnIndex(BluetoothMapContract.ConvoContactColumns.NICKNAME);
            this.mContactColLastActive = cursor.getColumnIndex(BluetoothMapContract.ChatStatusColumns.LAST_ACTIVE);
            this.mContactColName = cursor.getColumnIndex("name");
            this.mContactColPresenceState = cursor.getColumnIndex(BluetoothMapContract.PresenceColumns.PRESENCE_STATE);
            this.mContactColPresenceText = cursor.getColumnIndex(BluetoothMapContract.PresenceColumns.STATUS_TEXT);
            this.mContactColPriority = cursor.getColumnIndex(BluetoothMapContract.PresenceColumns.PRIORITY);
        }

        public void setSmsColumns(Cursor cursor) {
            this.mSmsColId = cursor.getColumnIndex("_id");
            this.mSmsColFolder = cursor.getColumnIndex(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE);
            this.mSmsColRead = cursor.getColumnIndex(BluetoothMapContract.FILTER_READ_STATUS);
            this.mSmsColSubject = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.BODY);
            this.mSmsColAddress = cursor.getColumnIndex("address");
            this.mSmsColDate = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.DATE);
            this.mSmsColType = cursor.getColumnIndex(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE);
            this.mSmsColThreadId = cursor.getColumnIndex("thread_id");
        }

        public void setMmsColumns(Cursor cursor) {
            this.mMmsColId = cursor.getColumnIndex("_id");
            this.mMmsColFolder = cursor.getColumnIndex("msg_box");
            this.mMmsColRead = cursor.getColumnIndex(BluetoothMapContract.FILTER_READ_STATUS);
            this.mMmsColAttachmentSize = cursor.getColumnIndex("m_size");
            this.mMmsColTextOnly = cursor.getColumnIndex("text_only");
            this.mMmsColSize = cursor.getColumnIndex("m_size");
            this.mMmsColDate = cursor.getColumnIndex(BluetoothMapContract.MessageColumns.DATE);
            this.mMmsColSubject = cursor.getColumnIndex("sub");
            this.mMmsColThreadId = cursor.getColumnIndex("thread_id");
        }
    }

    public BluetoothMapContent(Context context, BluetoothMapAccountItem bluetoothMapAccountItem, BluetoothMapMasInstance bluetoothMapMasInstance) {
        this.mContext = context;
        this.mResolver = this.mContext.getContentResolver();
        this.mMasInstance = bluetoothMapMasInstance;
        if (this.mResolver == null && D) {
            Log.d(TAG, "getContentResolver failed");
        }
        if (bluetoothMapAccountItem != null) {
            this.mBaseUri = bluetoothMapAccountItem.mBase_uri + "/";
            this.mAccount = bluetoothMapAccountItem;
            return;
        }
        this.mBaseUri = null;
        this.mAccount = null;
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    private void setProtected(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        if ((bluetoothMapAppParams.getParameterMask() & PlaybackStateCompat.ACTION_PREPARE) != 0) {
            String str = "no";
            if ((filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) && cursor.getInt(filterInfo.mMessageColProtected) == 1) {
                str = "yes";
            }
            bluetoothMapMessageListingElement.setProtect(str);
        }
    }

    private void setThreadId(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        long j = 0;
        if ((bluetoothMapAppParams.getParameterMask() & PlaybackStateCompat.ACTION_SET_REPEAT_MODE) != 0) {
            BluetoothMapUtils.TYPE type = BluetoothMapUtils.TYPE.SMS_GSM;
            if (filterInfo.mMsgType == 0) {
                j = cursor.getLong(filterInfo.mSmsColThreadId);
            } else if (filterInfo.mMsgType == 1) {
                j = cursor.getLong(filterInfo.mMmsColThreadId);
                type = BluetoothMapUtils.TYPE.MMS;
            } else if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
                j = cursor.getLong(filterInfo.mMessageColThreadId);
                type = BluetoothMapUtils.TYPE.EMAIL;
            }
            bluetoothMapMessageListingElement.setThreadId(j, type);
        }
    }

    private void setThreadName(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        if ((bluetoothMapAppParams.getParameterMask() & PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED) != 0 && filterInfo.mMsgType == 3) {
            bluetoothMapMessageListingElement.setThreadName(cursor.getString(filterInfo.mMessageColThreadName));
        }
    }

    private void setSent(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        String str;
        if ((bluetoothMapAppParams.getParameterMask() & PlaybackStateCompat.ACTION_PLAY_FROM_URI) != 0) {
            int i = 0;
            if (filterInfo.mMsgType == 0) {
                i = cursor.getInt(filterInfo.mSmsColFolder);
            } else if (filterInfo.mMsgType == 1) {
                i = cursor.getInt(filterInfo.mMmsColFolder);
            } else if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
                i = cursor.getInt(filterInfo.mMessageColFolder);
            }
            if (i == 2) {
                str = "yes";
            } else {
                str = "no";
            }
            bluetoothMapMessageListingElement.setSent(str);
        }
    }

    private void setRead(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        int i;
        if (filterInfo.mMsgType == 0) {
            i = cursor.getInt(filterInfo.mSmsColRead);
        } else if (filterInfo.mMsgType == 1) {
            i = cursor.getInt(filterInfo.mMmsColRead);
        } else if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
            i = cursor.getInt(filterInfo.mMessageColRead);
        } else {
            i = 0;
        }
        bluetoothMapMessageListingElement.setRead(i == 1, (bluetoothMapAppParams.getParameterMask() & PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM) != 0);
    }

    private void setConvoRead(BluetoothMapConvoListingElement bluetoothMapConvoListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        bluetoothMapConvoListingElement.setRead(cursor.getInt(filterInfo.mConvoColRead) == 1, (bluetoothMapAppParams.getParameterMask() & PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM) != 0);
    }

    private void setPriority(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        if ((bluetoothMapAppParams.getParameterMask() & PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH) != 0) {
            String str = "no";
            if ((filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) && cursor.getInt(filterInfo.mMessageColPriority) == 1) {
                str = "yes";
            }
            int i = 0;
            if (filterInfo.mMsgType == 1) {
                i = cursor.getInt(cursor.getColumnIndex("pri"));
            }
            if (i == 130) {
                str = "yes";
            }
            bluetoothMapMessageListingElement.setPriority(str);
        }
    }

    private void setAttachment(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        if ((bluetoothMapAppParams.getParameterMask() & PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) != 0) {
            int i = 0;
            String string = null;
            if (filterInfo.mMsgType == 1) {
                if (cursor.getInt(filterInfo.mMmsColTextOnly) == 0 && (i = cursor.getInt(filterInfo.mMmsColAttachmentSize)) <= 0) {
                    if (D) {
                        Log.d(TAG, "Error in message database, size reported as: " + i + " Changing size to 1");
                    }
                    i = 1;
                }
            } else if (filterInfo.mMsgType == 2) {
                int i2 = cursor.getInt(filterInfo.mMessageColAttachment);
                int i3 = cursor.getInt(filterInfo.mMessageColAttachmentSize);
                if (i2 == 1 && i3 == 0) {
                    if (D) {
                        Log.d(TAG, "Error in message database, attachment size reported as: " + i3 + " Changing size to 1");
                    }
                    i = 1;
                } else {
                    i = i3;
                }
            } else if (filterInfo.mMsgType == 3) {
                int i4 = cursor.getInt(filterInfo.mMessageColAttachment);
                int i5 = cursor.getInt(filterInfo.mMessageColAttachmentSize);
                if (i4 == 1 && i5 == 0) {
                    string = cursor.getString(filterInfo.mMessageColAttachmentMime);
                    i = 1;
                } else {
                    i = i5;
                }
            }
            bluetoothMapMessageListingElement.setAttachmentSize(i);
            if (this.mMsgListingVersion > 10 && (bluetoothMapAppParams.getParameterMask() & 4194304) != 0) {
                bluetoothMapMessageListingElement.setAttachmentMimeTypes(string);
            }
        }
    }

    private void setText(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        String str;
        if ((bluetoothMapAppParams.getParameterMask() & 512) != 0) {
            String str2 = "";
            if (filterInfo.mMsgType == 0) {
                str2 = "yes";
            } else if (filterInfo.mMsgType == 1) {
                if (cursor.getInt(filterInfo.mMmsColTextOnly) == 1) {
                    str = "yes";
                } else {
                    String textPartsMms = getTextPartsMms(this.mResolver, cursor.getLong(filterInfo.mMmsColId));
                    if (textPartsMms != null && textPartsMms.length() > 0) {
                        str = "yes";
                    } else {
                        str = "no";
                    }
                }
                str2 = str;
            } else if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
                str2 = "yes";
            }
            bluetoothMapMessageListingElement.setText(str2);
        }
    }

    private void setReceptionStatus(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        if ((bluetoothMapAppParams.getParameterMask() & 256) != 0) {
            bluetoothMapMessageListingElement.setReceptionStatus(BluetoothMapContract.RECEPTION_STATE_COMPLETE);
        }
    }

    private void setDeliveryStatus(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        if ((bluetoothMapAppParams.getParameterMask() & PlaybackStateCompat.ACTION_PREPARE_FROM_URI) != 0) {
            String string = BluetoothMapContract.DELIVERY_STATE_DELIVERED;
            if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
                string = cursor.getString(filterInfo.mMessageColDelivery);
            }
            bluetoothMapMessageListingElement.setDeliveryStatus(string);
        }
    }

    private void setSize(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        if ((bluetoothMapAppParams.getParameterMask() & 128) != 0) {
            int length = 0;
            if (filterInfo.mMsgType == 0) {
                length = cursor.getString(filterInfo.mSmsColSubject).length();
            } else if (filterInfo.mMsgType == 1) {
                length = cursor.getInt(filterInfo.mMmsColSize);
                String subject = bluetoothMapMessageListingElement.getSubject();
                if (subject == null || subject.length() == 0) {
                    setSubject(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                }
                if (subject != null && subject.length() != 0) {
                    length += subject.length();
                }
            } else if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
                length = cursor.getInt(filterInfo.mMessageColSize);
            }
            if (length <= 0) {
                if (D) {
                    Log.d(TAG, "Error in message database, size reported as: " + length + " Changing size to 1");
                }
                length = 1;
            }
            bluetoothMapMessageListingElement.setSize(length);
        }
    }

    private BluetoothMapUtils.TYPE getType(Cursor cursor, FilterInfo filterInfo) {
        if (filterInfo.mMsgType == 0) {
            if (filterInfo.mPhoneType == 2) {
                return BluetoothMapUtils.TYPE.SMS_CDMA;
            }
            return BluetoothMapUtils.TYPE.SMS_GSM;
        }
        if (filterInfo.mMsgType == 1) {
            return BluetoothMapUtils.TYPE.MMS;
        }
        if (filterInfo.mMsgType == 2) {
            return BluetoothMapUtils.TYPE.EMAIL;
        }
        if (filterInfo.mMsgType == 3) {
            return BluetoothMapUtils.TYPE.IM;
        }
        return null;
    }

    private void setFolderType(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        if ((bluetoothMapAppParams.getParameterMask() & PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED) != 0) {
            String str = null;
            if (filterInfo.mMsgType == 0) {
                int i = cursor.getInt(filterInfo.mSmsColFolder);
                str = i == 1 ? BluetoothMapContract.FOLDER_NAME_INBOX : i == 2 ? BluetoothMapContract.FOLDER_NAME_SENT : i == 3 ? BluetoothMapContract.FOLDER_NAME_DRAFT : (i == 4 || i == 5 || i == 6) ? BluetoothMapContract.FOLDER_NAME_OUTBOX : BluetoothMapContract.FOLDER_NAME_DELETED;
            } else if (filterInfo.mMsgType == 1) {
                int i2 = cursor.getInt(filterInfo.mMmsColFolder);
                str = i2 == 1 ? BluetoothMapContract.FOLDER_NAME_INBOX : i2 == 2 ? BluetoothMapContract.FOLDER_NAME_SENT : i2 == 3 ? BluetoothMapContract.FOLDER_NAME_DRAFT : i2 == 4 ? BluetoothMapContract.FOLDER_NAME_OUTBOX : BluetoothMapContract.FOLDER_NAME_DELETED;
            } else if (filterInfo.mMsgType != 2 && filterInfo.mMsgType == 3) {
                long j = cursor.getInt(filterInfo.mMessageColFolder);
                str = j == 1 ? BluetoothMapContract.FOLDER_NAME_INBOX : j == 2 ? BluetoothMapContract.FOLDER_NAME_SENT : j == 3 ? BluetoothMapContract.FOLDER_NAME_DRAFT : j == 4 ? BluetoothMapContract.FOLDER_NAME_OUTBOX : j == 5 ? BluetoothMapContract.FOLDER_NAME_DELETED : "OTHER";
            }
            bluetoothMapMessageListingElement.setFolderType(str);
        }
    }

    private String getRecipientNameEmail(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo) {
        String string = cursor.getString(filterInfo.mMessageColToAddress);
        String string2 = cursor.getString(filterInfo.mMessageColCcAddress);
        String string3 = cursor.getString(filterInfo.mMessageColBccAddress);
        StringBuilder sb = new StringBuilder();
        boolean z = true;
        if (string != null) {
            Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(string);
            if (rfc822TokenArr.length != 0) {
                if (D) {
                    Log.d(TAG, "toName count= " + rfc822TokenArr.length);
                }
                boolean z2 = true;
                int i = 0;
                while (i < rfc822TokenArr.length) {
                    String name = rfc822TokenArr[i].getName();
                    if (!z2) {
                        sb.append("; ");
                    }
                    sb.append(name);
                    i++;
                    z2 = false;
                }
            }
            if (string2 != null) {
                sb.append("; ");
            }
        }
        if (string2 != null) {
            Rfc822Token[] rfc822TokenArr2 = Rfc822Tokenizer.tokenize(string2);
            if (rfc822TokenArr2.length != 0) {
                if (D) {
                    Log.d(TAG, "ccName count= " + rfc822TokenArr2.length);
                }
                boolean z3 = true;
                int i2 = 0;
                while (i2 < rfc822TokenArr2.length) {
                    String name2 = rfc822TokenArr2[i2].getName();
                    if (!z3) {
                        sb.append("; ");
                    }
                    sb.append(name2);
                    i2++;
                    z3 = false;
                }
            }
            if (string3 != null) {
                sb.append("; ");
            }
        }
        if (string3 != null) {
            Rfc822Token[] rfc822TokenArr3 = Rfc822Tokenizer.tokenize(string3);
            if (rfc822TokenArr3.length != 0) {
                if (D) {
                    Log.d(TAG, "bccName count= " + rfc822TokenArr3.length);
                }
                int i3 = 0;
                while (i3 < rfc822TokenArr3.length) {
                    String name3 = rfc822TokenArr3[i3].getName();
                    if (!z) {
                        sb.append("; ");
                    }
                    sb.append(name3);
                    i3++;
                    z = false;
                }
            }
        }
        return sb.toString();
    }

    private String getRecipientAddressingEmail(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo) {
        String string = cursor.getString(filterInfo.mMessageColToAddress);
        String string2 = cursor.getString(filterInfo.mMessageColCcAddress);
        String string3 = cursor.getString(filterInfo.mMessageColBccAddress);
        StringBuilder sb = new StringBuilder();
        boolean z = true;
        if (string != null) {
            Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(string);
            if (rfc822TokenArr.length != 0) {
                if (D) {
                    Log.d(TAG, "toAddress count= " + rfc822TokenArr.length);
                }
                boolean z2 = true;
                int i = 0;
                while (i < rfc822TokenArr.length) {
                    String address = rfc822TokenArr[i].getAddress();
                    if (!z2) {
                        sb.append("; ");
                    }
                    sb.append(address);
                    i++;
                    z2 = false;
                }
            }
            if (string2 != null) {
                sb.append("; ");
            }
        }
        if (string2 != null) {
            Rfc822Token[] rfc822TokenArr2 = Rfc822Tokenizer.tokenize(string2);
            if (rfc822TokenArr2.length != 0) {
                if (D) {
                    Log.d(TAG, "ccAddress count= " + rfc822TokenArr2.length);
                }
                boolean z3 = true;
                int i2 = 0;
                while (i2 < rfc822TokenArr2.length) {
                    String address2 = rfc822TokenArr2[i2].getAddress();
                    if (!z3) {
                        sb.append("; ");
                    }
                    sb.append(address2);
                    i2++;
                    z3 = false;
                }
            }
            if (string3 != null) {
                sb.append("; ");
            }
        }
        if (string3 != null) {
            Rfc822Token[] rfc822TokenArr3 = Rfc822Tokenizer.tokenize(string3);
            if (rfc822TokenArr3.length != 0) {
                if (D) {
                    Log.d(TAG, "bccAddress count= " + rfc822TokenArr3.length);
                }
                int i3 = 0;
                while (i3 < rfc822TokenArr3.length) {
                    String address3 = rfc822TokenArr3[i3].getAddress();
                    if (!z) {
                        sb.append("; ");
                    }
                    sb.append(address3);
                    i3++;
                    z = false;
                }
            }
        }
        return sb.toString();
    }

    private void setRecipientAddressing(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) throws Throwable {
        String string;
        String string2;
        if ((bluetoothMapAppParams.getParameterMask() & 32) != 0) {
            String recipientAddressingEmail = null;
            if (filterInfo.mMsgType == 0) {
                int i = cursor.getInt(filterInfo.mSmsColType);
                if (i == 1) {
                    string = filterInfo.mPhoneNum;
                } else {
                    string = cursor.getString(cursor.getColumnIndex("address"));
                }
                recipientAddressingEmail = (string == null && i == 3 && (string2 = cursor.getString(cursor.getColumnIndex("thread_id"))) != null) ? getCanonicalAddressSms(this.mResolver, Integer.valueOf(string2).intValue()) : string;
            } else if (filterInfo.mMsgType == 1) {
                recipientAddressingEmail = getAddressMms(this.mResolver, cursor.getLong(cursor.getColumnIndex("_id")), 151);
            } else if (filterInfo.mMsgType == 2) {
                recipientAddressingEmail = getRecipientAddressingEmail(bluetoothMapMessageListingElement, cursor, filterInfo);
            }
            if (recipientAddressingEmail == null) {
                recipientAddressingEmail = "";
            }
            bluetoothMapMessageListingElement.setRecipientAddressing(recipientAddressingEmail);
        }
    }

    private void setRecipientName(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) throws Throwable {
        String recipientAddressing;
        if ((bluetoothMapAppParams.getParameterMask() & 16) != 0) {
            String recipientNameEmail = null;
            if (filterInfo.mMsgType == 0) {
                if (cursor.getInt(filterInfo.mSmsColType) != 1) {
                    String string = cursor.getString(filterInfo.mSmsColAddress);
                    if (string != null && !string.isEmpty()) {
                        recipientNameEmail = getContactNameFromPhone(string, this.mResolver);
                    }
                } else {
                    recipientNameEmail = filterInfo.mPhoneAlphaTag;
                }
            } else if (filterInfo.mMsgType == 1) {
                long j = cursor.getLong(filterInfo.mMmsColId);
                if (bluetoothMapMessageListingElement.getRecipientAddressing() != null) {
                    recipientAddressing = getAddressMms(this.mResolver, j, 151);
                } else {
                    recipientAddressing = bluetoothMapMessageListingElement.getRecipientAddressing();
                }
                if (recipientAddressing != null && !recipientAddressing.isEmpty()) {
                    recipientNameEmail = getContactNameFromPhone(recipientAddressing, this.mResolver);
                }
            } else if (filterInfo.mMsgType == 2) {
                recipientNameEmail = getRecipientNameEmail(bluetoothMapMessageListingElement, cursor, filterInfo);
            }
            if (recipientNameEmail == null) {
                recipientNameEmail = "";
            }
            bluetoothMapMessageListingElement.setRecipientName(recipientNameEmail);
        }
    }

    private void setSenderAddressing(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) throws Throwable {
        String numberBySubID;
        if ((bluetoothMapAppParams.getParameterMask() & 8) != 0) {
            String string = "";
            if (filterInfo.mMsgType == 0) {
                if (cursor.getInt(filterInfo.mSmsColType) == 1) {
                    numberBySubID = cursor.getString(filterInfo.mSmsColAddress);
                } else {
                    numberBySubID = BluetoothMapSimManager.getNumberBySubID(cursor.getLong(cursor.getColumnIndex("sub_id")));
                }
                if (numberBySubID != null) {
                    String strExtractNetworkPortion = PhoneNumberUtils.extractNetworkPortion(numberBySubID);
                    string = (strExtractNetworkPortion == null || strExtractNetworkPortion.length() < 2 || Boolean.valueOf(PhoneNumberUtils.stripSeparators(numberBySubID).matches("[0-9]*[a-zA-Z]+[0-9]*")).booleanValue()) ? numberBySubID : strExtractNetworkPortion;
                }
            } else if (filterInfo.mMsgType == 1) {
                String addressMms = getAddressMms(this.mResolver, cursor.getLong(filterInfo.mMmsColId), 137);
                String strExtractNetworkPortion2 = PhoneNumberUtils.extractNetworkPortion(addressMms);
                string = (strExtractNetworkPortion2 == null || strExtractNetworkPortion2.length() < 1) ? addressMms : strExtractNetworkPortion2;
            } else if (filterInfo.mMsgType == 2) {
                Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(cursor.getString(filterInfo.mMessageColFromAddress));
                if (rfc822TokenArr.length != 0) {
                    if (D) {
                        Log.d(TAG, "Originator count= " + rfc822TokenArr.length);
                    }
                    String str = "";
                    boolean z = true;
                    int i = 0;
                    while (i < rfc822TokenArr.length) {
                        String[] strArr = {rfc822TokenArr[i].getAddress()};
                        rfc822TokenArr[i].getName();
                        if (!z) {
                            str = str + "; ";
                        }
                        str = str + strArr[0];
                        i++;
                        z = false;
                    }
                    string = str;
                }
            } else if (filterInfo.mMsgType == 3) {
                long j = cursor.getLong(filterInfo.mMessageColFromAddress);
                Uri uri = Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_CONVOCONTACT);
                Cursor cursorQuery = this.mResolver.query(uri, BluetoothMapContract.BT_CONTACT_PROJECTION, "convo_id = " + j, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            string = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.ConvoContactColumns.UCI));
                        }
                    } finally {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    }
                }
            }
            if (string == null) {
                string = "";
            }
            bluetoothMapMessageListingElement.setSenderAddressing(string);
        }
    }

    private void setSenderName(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) throws Throwable {
        String senderAddressing;
        if ((bluetoothMapAppParams.getParameterMask() & 4) != 0) {
            String string = "";
            if (filterInfo.mMsgType == 0) {
                if (cursor.getInt(cursor.getColumnIndex(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE)) == 1) {
                    String string2 = cursor.getString(filterInfo.mSmsColAddress);
                    if (string2 != null && !string2.isEmpty()) {
                        string = getContactNameFromPhone(string2, this.mResolver);
                    }
                } else {
                    string = filterInfo.mPhoneAlphaTag;
                }
            } else if (filterInfo.mMsgType == 1) {
                long j = cursor.getLong(filterInfo.mMmsColId);
                if (bluetoothMapMessageListingElement.getSenderAddressing() != null) {
                    senderAddressing = getAddressMms(this.mResolver, j, 137);
                } else {
                    senderAddressing = bluetoothMapMessageListingElement.getSenderAddressing();
                }
                if (senderAddressing != null && !senderAddressing.isEmpty()) {
                    string = getContactNameFromPhone(senderAddressing, this.mResolver);
                }
            } else if (filterInfo.mMsgType == 2) {
                Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(cursor.getString(filterInfo.mMessageColFromAddress));
                if (rfc822TokenArr.length != 0) {
                    if (D) {
                        Log.d(TAG, "Originator count= " + rfc822TokenArr.length);
                    }
                    String str = "";
                    boolean z = true;
                    int i = 0;
                    while (i < rfc822TokenArr.length) {
                        new String[1][0] = rfc822TokenArr[i].getAddress();
                        String name = rfc822TokenArr[i].getName();
                        if (!z) {
                            str = str + "; ";
                        }
                        str = str + name;
                        i++;
                        z = false;
                    }
                    string = str;
                }
            } else if (filterInfo.mMsgType == 3) {
                long j2 = cursor.getLong(filterInfo.mMessageColFromAddress);
                Uri uri = Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_CONVOCONTACT);
                Cursor cursorQuery = this.mResolver.query(uri, BluetoothMapContract.BT_CONTACT_PROJECTION, "convo_id = " + j2, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            string = cursorQuery.getString(cursorQuery.getColumnIndex("name"));
                        }
                    } finally {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    }
                }
            }
            if (string == null) {
                string = "";
            }
            bluetoothMapMessageListingElement.setSenderName(string);
        }
    }

    private void setDateTime(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        long j = 0;
        if ((bluetoothMapAppParams.getParameterMask() & 2) != 0) {
            if (filterInfo.mMsgType == 0) {
                j = cursor.getLong(filterInfo.mSmsColDate);
            } else if (filterInfo.mMsgType == 1) {
                j = cursor.getLong(filterInfo.mMmsColDate) * 1000;
            } else if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
                j = cursor.getLong(filterInfo.mMessageColDate);
            }
            bluetoothMapMessageListingElement.setDateTime(j);
        }
    }

    private void setLastActivity(BluetoothMapConvoListingElement bluetoothMapConvoListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        long j;
        if (filterInfo.mMsgType == 0 || filterInfo.mMsgType == 1) {
            j = cursor.getLong(MMS_SMS_THREAD_COL_DATE);
        } else if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
            j = cursor.getLong(filterInfo.mConvoColLastActivity);
        } else {
            j = 0;
        }
        bluetoothMapConvoListingElement.setLastActivity(j);
    }

    public static String getTextPartsMms(ContentResolver contentResolver, long j) {
        String string;
        String str = "";
        Cursor cursorQuery = contentResolver.query(Uri.parse(new String(Telephony.Mms.CONTENT_URI + "/" + j + "/part")), null, new String("mid=" + j), null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    do {
                        if (cursorQuery.getString(cursorQuery.getColumnIndex("ct")).equals("text/plain") && (string = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessagePartColumns.TEXT))) != null) {
                            str = str + string;
                        }
                    } while (cursorQuery.moveToNext());
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        return str;
    }

    private void setSubject(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        String string = "";
        int subjectLength = bluetoothMapAppParams.getSubjectLength();
        if (subjectLength == -1) {
            subjectLength = 256;
        }
        if (DeviceWorkArounds.addressStartsWith(BluetoothMapService.getRemoteDevice().getAddress(), DeviceWorkArounds.HONDA_CARKIT) || (bluetoothMapAppParams.getParameterMask() & 1) != 0) {
            if (filterInfo.mMsgType == 0) {
                string = cursor.getString(filterInfo.mSmsColSubject);
            } else if (filterInfo.mMsgType == 1) {
                string = cursor.getString(filterInfo.mMmsColSubject);
                if (string == null || string.length() == 0) {
                    string = getTextPartsMms(this.mResolver, cursor.getLong(filterInfo.mMmsColId));
                }
            } else if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
                string = cursor.getString(filterInfo.mMessageColSubject);
            }
            if (string != null && string.length() > subjectLength) {
                string = string.substring(0, subjectLength);
            } else if (string == null) {
                string = "";
            }
            bluetoothMapMessageListingElement.setSubject(string);
        }
    }

    private void setHandle(BluetoothMapMessageListingElement bluetoothMapMessageListingElement, Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        long j;
        if (filterInfo.mMsgType == 0) {
            j = cursor.getLong(filterInfo.mSmsColId);
        } else if (filterInfo.mMsgType == 1) {
            j = cursor.getLong(filterInfo.mMmsColId);
        } else if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
            j = cursor.getLong(filterInfo.mMessageColId);
        } else {
            j = -1;
        }
        bluetoothMapMessageListingElement.setHandle(j);
    }

    private BluetoothMapMessageListingElement element(Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        BluetoothMapMessageListingElement bluetoothMapMessageListingElement = new BluetoothMapMessageListingElement();
        setHandle(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
        setDateTime(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
        bluetoothMapMessageListingElement.setType(getType(cursor, filterInfo), (bluetoothMapAppParams.getParameterMask() & 64) != 0);
        setRead(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
        bluetoothMapMessageListingElement.setCursorIndex(cursor.getPosition());
        return bluetoothMapMessageListingElement;
    }

    private BluetoothMapConvoListingElement createConvoElement(Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        BluetoothMapConvoListingElement bluetoothMapConvoListingElement = new BluetoothMapConvoListingElement();
        setLastActivity(bluetoothMapConvoListingElement, cursor, filterInfo, bluetoothMapAppParams);
        bluetoothMapConvoListingElement.setType(getType(cursor, filterInfo));
        bluetoothMapConvoListingElement.setCursorIndex(cursor.getPosition());
        return bluetoothMapConvoListingElement;
    }

    public static String getContactNameFromPhone(String str, ContentResolver contentResolver) throws Throwable {
        Cursor cursorQuery;
        String string = null;
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            cursorQuery = contentResolver.query(Uri.withAppendedPath(ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI, Uri.encode(str)), new String[]{"_id", "display_name"}, "in_visible_group=1", null, "display_name ASC");
            if (cursorQuery != null) {
                try {
                    int columnIndex = cursorQuery.getColumnIndex("display_name");
                    if (cursorQuery.getCount() >= 1) {
                        cursorQuery.moveToFirst();
                        string = cursorQuery.getString(columnIndex);
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return string;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    public static String getCanonicalAddressSms(ContentResolver contentResolver, int i) throws Throwable {
        String string;
        Throwable th;
        Cursor cursorQuery;
        Uri uriBuild = Telephony.MmsSms.CONTENT_URI.buildUpon().appendPath("canonical-addresses").build();
        String str = "";
        Cursor cursor = null;
        try {
            Cursor cursorQuery2 = contentResolver.query(Telephony.Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build(), RECIPIENT_ID_PROJECTION, "_id=" + i, null, null);
            if (cursorQuery2 != null) {
                try {
                    if (!cursorQuery2.moveToFirst()) {
                        string = null;
                    } else {
                        string = cursorQuery2.getString(0);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    cursor = cursorQuery2;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery2 != null) {
                cursorQuery2.close();
            } else {
                cursor = cursorQuery2;
            }
            if (string != null) {
                String str2 = "";
                for (String str3 : string.split(" ")) {
                    if (str2.length() != 0) {
                        str2 = str2 + " OR ";
                    }
                    str2 = str2 + "_id=" + str3;
                }
                try {
                    cursorQuery = contentResolver.query(uriBuild, null, str2, null, null);
                    if (cursorQuery != null) {
                        try {
                            if (cursorQuery.moveToFirst()) {
                                do {
                                    if (str.length() != 0) {
                                        str = str + ";";
                                    }
                                    str = str + cursorQuery.getString(cursorQuery.getColumnIndex("address"));
                                } while (cursorQuery.moveToNext());
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            throw th;
                        }
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                } catch (Throwable th4) {
                    Cursor cursor2 = cursor;
                    th = th4;
                    cursorQuery = cursor2;
                }
            }
            return str;
        } catch (Throwable th5) {
            th = th5;
        }
    }

    public static String getAddressMms(ContentResolver contentResolver, long j, int i) throws Throwable {
        Throwable th;
        Cursor cursorQuery;
        String str = new String("msg_id=" + j + " AND type=" + i);
        String string = null;
        try {
            cursorQuery = contentResolver.query(Uri.parse(new String(Telephony.Mms.CONTENT_URI + "/" + j + "/addr")), new String[]{"address"}, str, null, null);
            try {
                int columnIndex = cursorQuery.getColumnIndex("address");
                if (cursorQuery != null && cursorQuery.moveToFirst()) {
                    string = cursorQuery.getString(columnIndex);
                    if (string.equals(INSERT_ADDRES_TOKEN)) {
                        string = "";
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return string;
            } catch (Throwable th2) {
                th = th2;
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            cursorQuery = null;
        }
    }

    private boolean matchRecipientMms(Cursor cursor, FilterInfo filterInfo, String str) throws Throwable {
        String addressMms = getAddressMms(this.mResolver, cursor.getLong(cursor.getColumnIndex("_id")), 151);
        if (addressMms == null || addressMms.length() <= 0) {
            return false;
        }
        if (addressMms.matches(str)) {
            return true;
        }
        String contactNameFromPhone = getContactNameFromPhone(addressMms, this.mResolver);
        return contactNameFromPhone != null && contactNameFromPhone.length() > 0 && contactNameFromPhone.matches(str);
    }

    private boolean matchRecipientSms(Cursor cursor, FilterInfo filterInfo, String str) throws Throwable {
        if (cursor.getInt(cursor.getColumnIndex(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE)) == 1) {
            long j = cursor.getLong(cursor.getColumnIndex("sub_id"));
            String numberBySubID = BluetoothMapSimManager.getNumberBySubID(j);
            Log.d(TAG, "[matchRecipientSms] subId = " + j + " number = " + numberBySubID);
            String str2 = filterInfo.mPhoneAlphaTag;
            return (numberBySubID != null && numberBySubID.length() > 0 && numberBySubID.matches(str)) || (str2 != null && str2.length() > 0 && str2.matches(str));
        }
        String string = cursor.getString(cursor.getColumnIndex("address"));
        if (string == null || string.length() <= 0) {
            return false;
        }
        if (string.matches(str)) {
            return true;
        }
        String contactNameFromPhone = getContactNameFromPhone(string, this.mResolver);
        return contactNameFromPhone != null && contactNameFromPhone.length() > 0 && contactNameFromPhone.matches(str);
    }

    private boolean matchRecipient(Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        String filterRecipient = bluetoothMapAppParams.getFilterRecipient();
        if (filterRecipient == null || filterRecipient.length() <= 0) {
            return true;
        }
        String str = ".*" + filterRecipient.replace("*", ".*") + ".*";
        if (filterInfo.mMsgType != 0) {
            if (filterInfo.mMsgType == 1) {
                return matchRecipientMms(cursor, filterInfo, str);
            }
            if (D) {
                Log.d(TAG, "matchRecipient: Unknown msg type: " + filterInfo.mMsgType);
            }
            return false;
        }
        return matchRecipientSms(cursor, filterInfo, str);
    }

    private boolean matchOriginatorMms(Cursor cursor, FilterInfo filterInfo, String str) throws Throwable {
        String addressMms = getAddressMms(this.mResolver, cursor.getLong(cursor.getColumnIndex("_id")), 137);
        if (addressMms == null || addressMms.length() <= 0) {
            return false;
        }
        if (addressMms.matches(str)) {
            return true;
        }
        String contactNameFromPhone = getContactNameFromPhone(addressMms, this.mResolver);
        return contactNameFromPhone != null && contactNameFromPhone.length() > 0 && contactNameFromPhone.matches(str);
    }

    private boolean matchOriginatorSms(Cursor cursor, FilterInfo filterInfo, String str) throws Throwable {
        if (cursor.getInt(cursor.getColumnIndex(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE)) == 1) {
            String string = cursor.getString(cursor.getColumnIndex("address"));
            if (string == null || string.length() <= 0) {
                return false;
            }
            if (string.matches(str)) {
                return true;
            }
            String contactNameFromPhone = getContactNameFromPhone(string, this.mResolver);
            return contactNameFromPhone != null && contactNameFromPhone.length() > 0 && contactNameFromPhone.matches(str);
        }
        long j = cursor.getLong(cursor.getColumnIndex("sub_id"));
        String numberBySubID = BluetoothMapSimManager.getNumberBySubID(j);
        Log.d(TAG, "[matchOriginatorSms] subId = " + j + " number = " + numberBySubID);
        String str2 = filterInfo.mPhoneAlphaTag;
        return (numberBySubID != null && numberBySubID.length() > 0 && numberBySubID.matches(str)) || (str2 != null && str2.length() > 0 && str2.matches(str));
    }

    private boolean matchOriginator(Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        String filterOriginator = bluetoothMapAppParams.getFilterOriginator();
        if (filterOriginator == null || filterOriginator.length() <= 0) {
            return true;
        }
        String str = ".*" + filterOriginator.replace("*", ".*") + ".*";
        if (filterInfo.mMsgType != 0) {
            if (filterInfo.mMsgType == 1) {
                return matchOriginatorMms(cursor, filterInfo, str);
            }
            if (D) {
                Log.d(TAG, "matchOriginator: Unknown msg type: " + filterInfo.mMsgType);
            }
            return false;
        }
        return matchOriginatorSms(cursor, filterInfo, str);
    }

    private boolean matchAddresses(Cursor cursor, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        return matchOriginator(cursor, filterInfo, bluetoothMapAppParams) && matchRecipient(cursor, filterInfo, bluetoothMapAppParams);
    }

    private String setWhereFilterFolderTypeSms(String str) {
        if (BluetoothMapContract.FOLDER_NAME_INBOX.equalsIgnoreCase(str)) {
            return "type = 1 AND thread_id <> -1";
        }
        if (BluetoothMapContract.FOLDER_NAME_OUTBOX.equalsIgnoreCase(str)) {
            return "(type = 4 OR type = 5 OR type = 6) AND thread_id <> -1";
        }
        if (BluetoothMapContract.FOLDER_NAME_SENT.equalsIgnoreCase(str)) {
            return "type = 2 AND thread_id <> -1";
        }
        if (BluetoothMapContract.FOLDER_NAME_DRAFT.equalsIgnoreCase(str)) {
            return "type = 3 AND (thread_id IS NULL OR thread_id <> -1 )";
        }
        if (!BluetoothMapContract.FOLDER_NAME_DELETED.equalsIgnoreCase(str)) {
            return "";
        }
        return "thread_id = -1";
    }

    private String setWhereFilterFolderTypeMms(String str) {
        if (BluetoothMapContract.FOLDER_NAME_INBOX.equalsIgnoreCase(str)) {
            return "msg_box = 1 AND thread_id <> -1";
        }
        if (BluetoothMapContract.FOLDER_NAME_OUTBOX.equalsIgnoreCase(str)) {
            return "msg_box = 4 AND thread_id <> -1";
        }
        if (BluetoothMapContract.FOLDER_NAME_SENT.equalsIgnoreCase(str)) {
            return "msg_box = 2 AND thread_id <> -1";
        }
        if (BluetoothMapContract.FOLDER_NAME_DRAFT.equalsIgnoreCase(str)) {
            return "msg_box = 3 AND (thread_id IS NULL OR thread_id <> -1 )";
        }
        if (!BluetoothMapContract.FOLDER_NAME_DELETED.equalsIgnoreCase(str)) {
            return "";
        }
        return "thread_id = -1";
    }

    private String setWhereFilterFolderTypeEmail(long j) {
        if (j >= 0) {
            return "folder_id = " + j;
        }
        Log.e(TAG, "setWhereFilterFolderTypeEmail: not valid!");
        throw new IllegalArgumentException("Invalid folder ID");
    }

    private String setWhereFilterFolderTypeIm(long j) {
        if (j > 0) {
            return "folder_id = " + j;
        }
        Log.e(TAG, "setWhereFilterFolderTypeIm: not valid!");
        throw new IllegalArgumentException("Invalid folder ID");
    }

    private String setWhereFilterFolderType(BluetoothMapFolderElement bluetoothMapFolderElement, FilterInfo filterInfo) {
        if (bluetoothMapFolderElement.shouldIgnore()) {
            return "1=1";
        }
        if (filterInfo.mMsgType == 0) {
            return setWhereFilterFolderTypeSms(bluetoothMapFolderElement.getName());
        }
        if (filterInfo.mMsgType == 1) {
            return setWhereFilterFolderTypeMms(bluetoothMapFolderElement.getName());
        }
        if (filterInfo.mMsgType == 2) {
            return setWhereFilterFolderTypeEmail(bluetoothMapFolderElement.getFolderId());
        }
        if (filterInfo.mMsgType != 3) {
            return "1=1";
        }
        return setWhereFilterFolderTypeIm(bluetoothMapFolderElement.getFolderId());
    }

    private String setWhereFilterReadStatus(BluetoothMapAppParams bluetoothMapAppParams, FilterInfo filterInfo) {
        String str = "";
        if (bluetoothMapAppParams.getFilterReadStatus() == -1) {
            return "";
        }
        if (filterInfo.mMsgType == 0) {
            if ((bluetoothMapAppParams.getFilterReadStatus() & 1) != 0) {
                str = " AND read= 0";
            }
            if ((bluetoothMapAppParams.getFilterReadStatus() & 2) != 0) {
                return " AND read= 1";
            }
            return str;
        }
        if (filterInfo.mMsgType == 1) {
            if ((bluetoothMapAppParams.getFilterReadStatus() & 1) != 0) {
                str = " AND read= 0";
            }
            if ((bluetoothMapAppParams.getFilterReadStatus() & 2) != 0) {
                return " AND read= 1";
            }
            return str;
        }
        if (filterInfo.mMsgType != 2 && filterInfo.mMsgType != 3) {
            return "";
        }
        if ((bluetoothMapAppParams.getFilterReadStatus() & 1) != 0) {
            str = " AND flag_read= 0";
        }
        if ((bluetoothMapAppParams.getFilterReadStatus() & 2) != 0) {
            return " AND flag_read= 1";
        }
        return str;
    }

    private String setWhereFilterPeriod(BluetoothMapAppParams bluetoothMapAppParams, FilterInfo filterInfo) {
        String str = "";
        if (bluetoothMapAppParams.getFilterPeriodBegin() != -1) {
            if (filterInfo.mMsgType == 0) {
                str = " AND date >= " + bluetoothMapAppParams.getFilterPeriodBegin();
            } else if (filterInfo.mMsgType == 1) {
                str = " AND date >= " + (bluetoothMapAppParams.getFilterPeriodBegin() / 1000);
            } else if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
                str = " AND date >= " + bluetoothMapAppParams.getFilterPeriodBegin();
            }
        }
        if (bluetoothMapAppParams.getFilterPeriodEnd() != -1) {
            if (filterInfo.mMsgType == 0) {
                return str + " AND date < " + bluetoothMapAppParams.getFilterPeriodEnd();
            }
            if (filterInfo.mMsgType == 1) {
                return str + " AND date < " + (bluetoothMapAppParams.getFilterPeriodEnd() / 1000);
            }
            if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
                return str + " AND date < " + bluetoothMapAppParams.getFilterPeriodEnd();
            }
            return str;
        }
        return str;
    }

    private String setWhereFilterLastActivity(BluetoothMapAppParams bluetoothMapAppParams, FilterInfo filterInfo) {
        String str = "";
        if (bluetoothMapAppParams.getFilterLastActivityBegin() != -1) {
            if (filterInfo.mMsgType == 0) {
                str = " AND date >= " + bluetoothMapAppParams.getFilterLastActivityBegin();
            } else if (filterInfo.mMsgType == 1) {
                str = " AND date >= " + (bluetoothMapAppParams.getFilterLastActivityBegin() / 1000);
            } else if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
                str = " AND last_thread_activity >= " + bluetoothMapAppParams.getFilterPeriodBegin();
            }
        }
        if (bluetoothMapAppParams.getFilterLastActivityEnd() != -1) {
            if (filterInfo.mMsgType == 0) {
                return str + " AND date < " + bluetoothMapAppParams.getFilterLastActivityEnd();
            }
            if (filterInfo.mMsgType == 1) {
                return str + " AND date < " + (bluetoothMapAppParams.getFilterPeriodEnd() / 1000);
            }
            if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
                return str + " AND last_thread_activity < " + bluetoothMapAppParams.getFilterLastActivityEnd();
            }
            return str;
        }
        return str;
    }

    private String setWhereFilterOriginatorEmail(BluetoothMapAppParams bluetoothMapAppParams) {
        String filterOriginator = bluetoothMapAppParams.getFilterOriginator();
        if (filterOriginator == null || filterOriginator.length() <= 0) {
            return "";
        }
        return " AND from_list LIKE '%" + filterOriginator.replace("*", "%") + "%'";
    }

    private String setWhereFilterOriginatorIM(BluetoothMapAppParams bluetoothMapAppParams) {
        String filterOriginator = bluetoothMapAppParams.getFilterOriginator();
        if (filterOriginator == null || filterOriginator.length() <= 0) {
            return "";
        }
        return " AND from_list LIKE '%" + filterOriginator.replace("*", "%") + "%'";
    }

    private String setWhereFilterPriority(BluetoothMapAppParams bluetoothMapAppParams, FilterInfo filterInfo) {
        String str = "";
        int filterPriority = bluetoothMapAppParams.getFilterPriority();
        if (filterInfo.mMsgType == 1) {
            if (filterPriority == 2) {
                str = " AND pri<=" + Integer.toString(129);
            } else if (filterPriority == 1) {
                str = " AND pri=" + Integer.toString(130);
            }
        }
        if (filterInfo.mMsgType == 2 || filterInfo.mMsgType == 3) {
            if (filterPriority == 2) {
                return str + " AND high_priority!=1";
            }
            if (filterPriority == 1) {
                return str + " AND high_priority=1";
            }
            return str;
        }
        return str;
    }

    private String setWhereFilterRecipientEmail(BluetoothMapAppParams bluetoothMapAppParams) {
        String filterRecipient = bluetoothMapAppParams.getFilterRecipient();
        if (filterRecipient == null || filterRecipient.length() <= 0) {
            return "";
        }
        String strReplace = filterRecipient.replace("*", "%");
        return " AND (to_list LIKE '%" + strReplace + "%' OR " + BluetoothMapContract.EmailMessageColumns.CC_LIST + " LIKE '%" + strReplace + "%' OR " + BluetoothMapContract.EmailMessageColumns.BCC_LIST + " LIKE '%" + strReplace + "%' )";
    }

    private String setWhereFilterMessageHandle(BluetoothMapAppParams bluetoothMapAppParams, FilterInfo filterInfo) {
        long cpHandle;
        String filterMsgHandleString = bluetoothMapAppParams.getFilterMsgHandleString();
        if (filterMsgHandleString != null) {
            cpHandle = BluetoothMapUtils.getCpHandle(filterMsgHandleString);
            if (D) {
                Log.d(TAG, "id: " + cpHandle);
            }
        } else {
            cpHandle = -1;
        }
        if (cpHandle == -1) {
            return "";
        }
        if (filterInfo.mMsgType == 0) {
            return " AND _id = " + cpHandle;
        }
        if (filterInfo.mMsgType == 1) {
            return " AND _id = " + cpHandle;
        }
        if (filterInfo.mMsgType != 2 && filterInfo.mMsgType != 3) {
            return "";
        }
        return " AND _id = " + cpHandle;
    }

    private String setWhereFilterThreadId(BluetoothMapAppParams bluetoothMapAppParams, FilterInfo filterInfo) {
        long msgHandleAsLong;
        String filterConvoIdString = bluetoothMapAppParams.getFilterConvoIdString();
        if (filterConvoIdString != null) {
            msgHandleAsLong = BluetoothMapUtils.getMsgHandleAsLong(filterConvoIdString);
            if (D) {
                Log.d(TAG, "id: " + msgHandleAsLong);
            }
        } else {
            msgHandleAsLong = -1;
        }
        if (msgHandleAsLong <= 0) {
            return "";
        }
        if (filterInfo.mMsgType == 0) {
            return " AND thread_id = " + msgHandleAsLong;
        }
        if (filterInfo.mMsgType == 1) {
            return " AND thread_id = " + msgHandleAsLong;
        }
        if (filterInfo.mMsgType != 2 && filterInfo.mMsgType != 3) {
            return "";
        }
        return " AND thread_id = " + msgHandleAsLong;
    }

    private String setWhereFilter(BluetoothMapFolderElement bluetoothMapFolderElement, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        String str = "" + setWhereFilterFolderType(bluetoothMapFolderElement, filterInfo);
        String whereFilterMessageHandle = setWhereFilterMessageHandle(bluetoothMapAppParams, filterInfo);
        if (whereFilterMessageHandle.isEmpty()) {
            String str2 = ((str + setWhereFilterReadStatus(bluetoothMapAppParams, filterInfo)) + setWhereFilterPriority(bluetoothMapAppParams, filterInfo)) + setWhereFilterPeriod(bluetoothMapAppParams, filterInfo);
            if (filterInfo.mMsgType == 2) {
                str2 = (str2 + setWhereFilterOriginatorEmail(bluetoothMapAppParams)) + setWhereFilterRecipientEmail(bluetoothMapAppParams);
            }
            if (filterInfo.mMsgType == 3) {
                str2 = str2 + setWhereFilterOriginatorIM(bluetoothMapAppParams);
            }
            return str2 + setWhereFilterThreadId(bluetoothMapAppParams, filterInfo);
        }
        return str + whereFilterMessageHandle;
    }

    private void setConvoWhereFilterSmsMms(StringBuilder sb, ArrayList<String> arrayList, FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        if (smsSelected(filterInfo, bluetoothMapAppParams) || mmsSelected(bluetoothMapAppParams)) {
            if (bluetoothMapAppParams.getFilterReadStatus() != -1) {
                if ((bluetoothMapAppParams.getFilterReadStatus() & 1) != 0) {
                    sb.append(" AND ");
                    sb.append(BluetoothMapContract.FILTER_READ_STATUS);
                    sb.append(" = 0");
                }
                if ((bluetoothMapAppParams.getFilterReadStatus() & 2) != 0) {
                    sb.append(" AND ");
                    sb.append(BluetoothMapContract.FILTER_READ_STATUS);
                    sb.append(" = 1");
                }
            }
            if (bluetoothMapAppParams.getFilterLastActivityBegin() != -1) {
                sb.append(" AND ");
                sb.append(BluetoothMapContract.MessageColumns.DATE);
                sb.append(" >= ");
                sb.append(bluetoothMapAppParams.getFilterLastActivityBegin());
            }
            if (bluetoothMapAppParams.getFilterLastActivityEnd() != -1) {
                sb.append(" AND ");
                sb.append(BluetoothMapContract.MessageColumns.DATE);
                sb.append(" <= ");
                sb.append(bluetoothMapAppParams.getFilterLastActivityEnd());
            }
            long leastSignificantBits = bluetoothMapAppParams.getFilterConvoId() != null ? bluetoothMapAppParams.getFilterConvoId().getLeastSignificantBits() : -1L;
            if (leastSignificantBits > 0) {
                sb.append(" AND ");
                sb.append("_id");
                sb.append(" = ");
                sb.append(Long.toString(leastSignificantBits));
            }
        }
    }

    private boolean smsSelected(FilterInfo filterInfo, BluetoothMapAppParams bluetoothMapAppParams) {
        int filterMessageType = bluetoothMapAppParams.getFilterMessageType();
        int i = filterInfo.mPhoneType;
        if (D) {
            Log.d(TAG, "smsSelected msgType: " + filterMessageType);
        }
        if (filterMessageType == -1 || (filterMessageType & 3) == 0) {
            return true;
        }
        if ((filterMessageType & 1) == 0 && i == 1) {
            return true;
        }
        return (filterMessageType & 2) == 0 && i == 2;
    }

    private boolean mmsSelected(BluetoothMapAppParams bluetoothMapAppParams) {
        int filterMessageType = bluetoothMapAppParams.getFilterMessageType();
        if (D) {
            Log.d(TAG, "mmsSelected msgType: " + filterMessageType);
        }
        return filterMessageType == -1 || (filterMessageType & 8) == 0;
    }

    private boolean emailSelected(BluetoothMapAppParams bluetoothMapAppParams) {
        int filterMessageType = bluetoothMapAppParams.getFilterMessageType();
        if (D) {
            Log.d(TAG, "emailSelected msgType: " + filterMessageType);
        }
        return filterMessageType == -1 || (filterMessageType & 4) == 0;
    }

    private boolean imSelected(BluetoothMapAppParams bluetoothMapAppParams) {
        int filterMessageType = bluetoothMapAppParams.getFilterMessageType();
        if (D) {
            Log.d(TAG, "imSelected msgType: " + filterMessageType);
        }
        return filterMessageType == -1 || (filterMessageType & 16) == 0;
    }

    private void setFilterInfo(FilterInfo filterInfo) {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (telephonyManager != null) {
            filterInfo.mPhoneType = telephonyManager.getPhoneType();
            filterInfo.mPhoneNum = telephonyManager.getLine1Number();
            filterInfo.mPhoneAlphaTag = telephonyManager.getLine1AlphaTag();
            if (D) {
                Log.d(TAG, "phone type = " + filterInfo.mPhoneType + " phone num = " + filterInfo.mPhoneNum + " phone alpha tag = " + filterInfo.mPhoneAlphaTag);
            }
        }
    }

    public BluetoothMapMessageListing msgListing(BluetoothMapFolderElement bluetoothMapFolderElement, BluetoothMapAppParams bluetoothMapAppParams) throws Throwable {
        Cursor cursorQuery;
        Cursor cursorQuery2;
        Cursor cursorQuery3;
        int size;
        int i;
        Cursor cursor;
        if (D) {
            Log.d(TAG, "msgListing: messageType = " + bluetoothMapAppParams.getFilterMessageType());
        }
        BluetoothMapMessageListing bluetoothMapMessageListing = new BluetoothMapMessageListing();
        if (bluetoothMapAppParams.getParameterMask() == -1 || bluetoothMapAppParams.getParameterMask() == 0) {
            bluetoothMapAppParams.setParameterMask(4294967295L);
        }
        Cursor cursorQuery4 = null;
        FilterInfo filterInfo = new FilterInfo(this, 0 == true ? 1 : 0);
        setFilterInfo(filterInfo);
        String str = "";
        bluetoothMapAppParams.getMaxListCount();
        int startOffset = bluetoothMapAppParams.getStartOffset();
        if (bluetoothMapAppParams.getMaxListCount() > 0) {
            str = " LIMIT " + (bluetoothMapAppParams.getMaxListCount() + bluetoothMapAppParams.getStartOffset());
        }
        try {
            int i2 = 1;
            int i3 = 0;
            if (!smsSelected(filterInfo, bluetoothMapAppParams) || !bluetoothMapFolderElement.hasSmsMmsContent()) {
                cursorQuery4 = null;
                if (mmsSelected(bluetoothMapAppParams) || !bluetoothMapFolderElement.hasSmsMmsContent()) {
                    cursorQuery2 = null;
                    if (emailSelected(bluetoothMapAppParams) && bluetoothMapFolderElement.hasEmailContent()) {
                        if (bluetoothMapAppParams.getFilterMessageType() == 27) {
                            str = " LIMIT " + bluetoothMapAppParams.getMaxListCount() + " OFFSET " + bluetoothMapAppParams.getStartOffset();
                            if (D) {
                                Log.d(TAG, "Email Limit => " + str);
                            }
                            startOffset = 0;
                        }
                        filterInfo.mMsgType = 2;
                        String whereFilter = setWhereFilter(bluetoothMapFolderElement, filterInfo, bluetoothMapAppParams);
                        if (!whereFilter.isEmpty()) {
                            if (D) {
                                Log.d(TAG, "msgType: " + filterInfo.mMsgType + " where: " + whereFilter);
                            }
                            Uri uri = Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_MESSAGE);
                            cursorQuery3 = this.mResolver.query(uri, BluetoothMapContract.BT_MESSAGE_PROJECTION, whereFilter, null, "date DESC" + str);
                            if (cursorQuery3 != null) {
                                try {
                                    filterInfo.setEmailMessageColumns(cursorQuery3);
                                    if (D) {
                                        Log.d(TAG, "Found " + cursorQuery3.getCount() + " email messages.");
                                    }
                                    while (cursorQuery3.moveToNext()) {
                                        bluetoothMapMessageListing.add(element(cursorQuery3, filterInfo, bluetoothMapAppParams));
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    cursorQuery = null;
                                }
                            }
                        }
                        if (imSelected(bluetoothMapAppParams)) {
                            cursorQuery = null;
                        }
                        bluetoothMapMessageListing.sort();
                        bluetoothMapMessageListing.segment(bluetoothMapAppParams.getMaxListCount(), startOffset);
                        List<BluetoothMapMessageListingElement> list = bluetoothMapMessageListing.getList();
                        size = list.size();
                        i = 0;
                        Cursor cursor2 = null;
                        while (i < size) {
                        }
                        if (cursorQuery3 != null) {
                        }
                        if (cursorQuery4 != null) {
                        }
                        if (cursorQuery2 != null) {
                        }
                        if (cursorQuery != null) {
                        }
                        if (D) {
                        }
                        return bluetoothMapMessageListing;
                    }
                    cursorQuery3 = null;
                    if (imSelected(bluetoothMapAppParams) && bluetoothMapFolderElement.hasImContent()) {
                        if (bluetoothMapAppParams.getFilterMessageType() == 15) {
                            str = " LIMIT " + bluetoothMapAppParams.getMaxListCount() + " OFFSET " + bluetoothMapAppParams.getStartOffset();
                            if (D) {
                                Log.d(TAG, "IM Limit => " + str);
                            }
                            startOffset = 0;
                        }
                        filterInfo.mMsgType = 3;
                        String whereFilter2 = setWhereFilter(bluetoothMapFolderElement, filterInfo, bluetoothMapAppParams);
                        if (D) {
                            Log.d(TAG, "msgType: " + filterInfo.mMsgType + " where: " + whereFilter2);
                        }
                        Uri uri2 = Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_MESSAGE);
                        cursorQuery = this.mResolver.query(uri2, BluetoothMapContract.BT_INSTANT_MESSAGE_PROJECTION, whereFilter2, null, "date DESC" + str);
                        if (cursorQuery != null) {
                            try {
                                filterInfo.setImMessageColumns(cursorQuery);
                                if (D) {
                                    Log.d(TAG, "Found " + cursorQuery.getCount() + " im messages.");
                                }
                                while (cursorQuery.moveToNext()) {
                                    bluetoothMapMessageListing.add(element(cursorQuery, filterInfo, bluetoothMapAppParams));
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                    } else {
                        cursorQuery = null;
                    }
                    bluetoothMapMessageListing.sort();
                    bluetoothMapMessageListing.segment(bluetoothMapAppParams.getMaxListCount(), startOffset);
                    List<BluetoothMapMessageListingElement> list2 = bluetoothMapMessageListing.getList();
                    size = list2.size();
                    i = 0;
                    Cursor cursor22 = null;
                    while (i < size) {
                        BluetoothMapMessageListingElement bluetoothMapMessageListingElement = list2.get(i);
                        BluetoothMapUtils.TYPE type = bluetoothMapMessageListingElement.getType();
                        if (cursorQuery4 != null && (BluetoothMapUtils.TYPE.SMS_GSM.equals(type) || BluetoothMapUtils.TYPE.SMS_CDMA.equals(type))) {
                            filterInfo.mMsgType = i3;
                            cursor = cursorQuery4;
                        } else if (cursorQuery2 == null || !BluetoothMapUtils.TYPE.MMS.equals(type)) {
                            if (cursorQuery3 != null && BluetoothMapUtils.TYPE.EMAIL.equals(type)) {
                                filterInfo.mMsgType = 2;
                                cursor = cursorQuery3;
                            } else if (cursorQuery == null || !BluetoothMapUtils.TYPE.IM.equals(type)) {
                                cursor = cursor22;
                            } else {
                                filterInfo.mMsgType = 3;
                                cursor = cursorQuery;
                            }
                            if (cursor == null) {
                                cursor.moveToPosition(bluetoothMapMessageListingElement.getCursorIndex());
                                setSenderAddressing(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                setSenderName(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                setRecipientAddressing(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                setRecipientName(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                setSubject(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                setSize(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                setText(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                setPriority(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                setSent(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                setProtected(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                setReceptionStatus(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                setAttachment(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                if (this.mMsgListingVersion > 10) {
                                    setDeliveryStatus(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                    setThreadId(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                    setThreadName(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                    setFolderType(bluetoothMapMessageListingElement, cursor, filterInfo, bluetoothMapAppParams);
                                }
                            }
                            i++;
                            cursor22 = cursor;
                            i2 = 1;
                            i3 = 0;
                        } else {
                            filterInfo.mMsgType = i2;
                            cursor = cursorQuery2;
                        }
                        if (cursor == null) {
                        }
                        i++;
                        cursor22 = cursor;
                        i2 = 1;
                        i3 = 0;
                    }
                    if (cursorQuery3 != null) {
                        cursorQuery3.close();
                    }
                    if (cursorQuery4 != null) {
                        cursorQuery4.close();
                    }
                    if (cursorQuery2 != null) {
                        cursorQuery2.close();
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    if (D) {
                        Log.d(TAG, "messagelisting end");
                    }
                    return bluetoothMapMessageListing;
                }
                if (bluetoothMapAppParams.getFilterMessageType() == 23) {
                    str = " LIMIT " + bluetoothMapAppParams.getMaxListCount() + " OFFSET " + bluetoothMapAppParams.getStartOffset();
                    if (D) {
                        Log.d(TAG, "MMS Limit => " + str);
                    }
                    startOffset = 0;
                }
                filterInfo.mMsgType = 1;
                String str2 = setWhereFilter(bluetoothMapFolderElement, filterInfo, bluetoothMapAppParams) + " AND " + INTERESTED_MESSAGE_TYPE_CLAUSE;
                if (!str2.isEmpty()) {
                    if (D) {
                        Log.d(TAG, "msgType: " + filterInfo.mMsgType + " where: " + str2);
                    }
                    cursorQuery2 = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION, str2, null, "date DESC" + str);
                    if (cursorQuery2 != null) {
                        try {
                            filterInfo.setMmsColumns(cursorQuery2);
                            if (D) {
                                Log.d(TAG, "Found " + cursorQuery2.getCount() + " mms messages.");
                            }
                            while (cursorQuery2.moveToNext()) {
                                if (matchAddresses(cursorQuery2, filterInfo, bluetoothMapAppParams)) {
                                    bluetoothMapMessageListing.add(element(cursorQuery2, filterInfo, bluetoothMapAppParams));
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            cursorQuery = null;
                            cursorQuery3 = null;
                        }
                    }
                }
                if (emailSelected(bluetoothMapAppParams)) {
                    cursorQuery3 = null;
                    if (imSelected(bluetoothMapAppParams)) {
                    }
                    bluetoothMapMessageListing.sort();
                    bluetoothMapMessageListing.segment(bluetoothMapAppParams.getMaxListCount(), startOffset);
                    List<BluetoothMapMessageListingElement> list22 = bluetoothMapMessageListing.getList();
                    size = list22.size();
                    i = 0;
                    Cursor cursor222 = null;
                    while (i < size) {
                    }
                    if (cursorQuery3 != null) {
                    }
                    if (cursorQuery4 != null) {
                    }
                    if (cursorQuery2 != null) {
                    }
                    if (cursorQuery != null) {
                    }
                    if (D) {
                    }
                    return bluetoothMapMessageListing;
                }
            } else if (bluetoothMapAppParams.getFilterMessageType() != 29) {
                try {
                    if (bluetoothMapAppParams.getFilterMessageType() == 30) {
                        str = " LIMIT " + bluetoothMapAppParams.getMaxListCount() + " OFFSET " + bluetoothMapAppParams.getStartOffset();
                        if (D) {
                            Log.d(TAG, "SMS Limit => " + str);
                        }
                        startOffset = 0;
                    }
                    filterInfo.mMsgType = 0;
                    if (bluetoothMapAppParams.getFilterPriority() != 1) {
                        String whereFilter3 = setWhereFilter(bluetoothMapFolderElement, filterInfo, bluetoothMapAppParams);
                        if (D) {
                            Log.d(TAG, "msgType: " + filterInfo.mMsgType + " where: " + whereFilter3);
                        }
                        cursorQuery4 = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, whereFilter3, null, "date DESC" + str);
                        if (cursorQuery4 != null) {
                            try {
                                if (D) {
                                    Log.d(TAG, "Found " + cursorQuery4.getCount() + " sms messages.");
                                }
                                filterInfo.setSmsColumns(cursorQuery4);
                                while (cursorQuery4.moveToNext()) {
                                    if (matchAddresses(cursorQuery4, filterInfo, bluetoothMapAppParams)) {
                                        bluetoothMapMessageListing.add(element(cursorQuery4, filterInfo, bluetoothMapAppParams));
                                    }
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                cursorQuery = null;
                                cursorQuery2 = null;
                                cursorQuery3 = null;
                                if (cursorQuery3 != null) {
                                }
                                if (cursorQuery4 != null) {
                                }
                                if (cursorQuery2 != null) {
                                }
                                if (cursorQuery != null) {
                                }
                                throw th;
                            }
                        }
                    }
                    if (mmsSelected(bluetoothMapAppParams)) {
                        cursorQuery2 = null;
                        if (emailSelected(bluetoothMapAppParams)) {
                        }
                    }
                } catch (Throwable th5) {
                    th = th5;
                    cursorQuery = null;
                    cursorQuery2 = null;
                    cursorQuery3 = null;
                }
            }
        } catch (Throwable th6) {
            th = th6;
            cursorQuery = null;
            cursorQuery4 = null;
        }
        if (cursorQuery3 != null) {
            cursorQuery3.close();
        }
        if (cursorQuery4 != null) {
            cursorQuery4.close();
        }
        if (cursorQuery2 != null) {
            cursorQuery2.close();
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        throw th;
    }

    public int msgListingSize(BluetoothMapFolderElement bluetoothMapFolderElement, BluetoothMapAppParams bluetoothMapAppParams) {
        Cursor cursorQuery;
        if (D) {
            Log.d(TAG, "msgListingSize: folder = " + bluetoothMapFolderElement.getName());
        }
        FilterInfo filterInfo = new FilterInfo(this, null);
        setFilterInfo(filterInfo);
        int count = 0;
        if (smsSelected(filterInfo, bluetoothMapAppParams) && bluetoothMapFolderElement.hasSmsMmsContent()) {
            filterInfo.mMsgType = 0;
            cursorQuery = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, setWhereFilter(bluetoothMapFolderElement, filterInfo, bluetoothMapAppParams), null, "date DESC");
            if (cursorQuery != null) {
                try {
                    count = cursorQuery.getCount();
                } finally {
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
        if (mmsSelected(bluetoothMapAppParams) && bluetoothMapFolderElement.hasSmsMmsContent()) {
            filterInfo.mMsgType = 1;
            cursorQuery = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION, setWhereFilter(bluetoothMapFolderElement, filterInfo, bluetoothMapAppParams), null, "date DESC");
            if (cursorQuery != null) {
                try {
                    count += cursorQuery.getCount();
                } finally {
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
        if (emailSelected(bluetoothMapAppParams) && bluetoothMapFolderElement.hasEmailContent()) {
            filterInfo.mMsgType = 2;
            String whereFilter = setWhereFilter(bluetoothMapFolderElement, filterInfo, bluetoothMapAppParams);
            if (!whereFilter.isEmpty()) {
                Cursor cursorQuery2 = this.mResolver.query(Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_MESSAGE), BluetoothMapContract.BT_MESSAGE_PROJECTION, whereFilter, null, "date DESC");
                if (cursorQuery2 != null) {
                    try {
                        count += cursorQuery2.getCount();
                    } finally {
                        if (cursorQuery2 != null) {
                            cursorQuery2.close();
                        }
                    }
                }
                if (cursorQuery2 != null) {
                    cursorQuery2.close();
                }
            }
        }
        if (imSelected(bluetoothMapAppParams) && bluetoothMapFolderElement.hasImContent()) {
            filterInfo.mMsgType = 3;
            String whereFilter2 = setWhereFilter(bluetoothMapFolderElement, filterInfo, bluetoothMapAppParams);
            if (!whereFilter2.isEmpty()) {
                Cursor cursorQuery3 = this.mResolver.query(Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_MESSAGE), BluetoothMapContract.BT_INSTANT_MESSAGE_PROJECTION, whereFilter2, null, "date DESC");
                if (cursorQuery3 != null) {
                    try {
                        count += cursorQuery3.getCount();
                    } finally {
                        if (cursorQuery3 != null) {
                            cursorQuery3.close();
                        }
                    }
                }
                if (cursorQuery3 != null) {
                    cursorQuery3.close();
                }
            }
        }
        if (D) {
            Log.d(TAG, "msgListingSize: size = " + count);
        }
        return count;
    }

    public boolean msgListingHasUnread(BluetoothMapFolderElement bluetoothMapFolderElement, BluetoothMapAppParams bluetoothMapAppParams) {
        int count;
        Cursor cursorQuery;
        if (D) {
            Log.d(TAG, "msgListingHasUnread: folder = " + bluetoothMapFolderElement.getName());
        }
        FilterInfo filterInfo = new FilterInfo(this, null);
        setFilterInfo(filterInfo);
        if (smsSelected(filterInfo, bluetoothMapAppParams) && bluetoothMapFolderElement.hasSmsMmsContent()) {
            filterInfo.mMsgType = 0;
            Cursor cursorQuery2 = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, (setWhereFilterFolderType(bluetoothMapFolderElement, filterInfo) + " AND read=0 ") + setWhereFilterPeriod(bluetoothMapAppParams, filterInfo), null, "date DESC");
            if (cursorQuery2 != null) {
                try {
                    count = cursorQuery2.getCount();
                } finally {
                    if (cursorQuery2 != null) {
                        cursorQuery2.close();
                    }
                }
            } else {
                count = 0;
            }
            if (cursorQuery2 != null) {
                cursorQuery2.close();
            }
        } else {
            count = 0;
        }
        if (mmsSelected(bluetoothMapAppParams) && bluetoothMapFolderElement.hasSmsMmsContent()) {
            filterInfo.mMsgType = 1;
            cursorQuery = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION, (setWhereFilterFolderType(bluetoothMapFolderElement, filterInfo) + " AND read=0 ") + setWhereFilterPeriod(bluetoothMapAppParams, filterInfo), null, "date DESC");
            if (cursorQuery != null) {
                try {
                    count += cursorQuery.getCount();
                } finally {
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
        if (emailSelected(bluetoothMapAppParams) && bluetoothMapFolderElement.getFolderId() != -1) {
            filterInfo.mMsgType = 2;
            String whereFilterFolderType = setWhereFilterFolderType(bluetoothMapFolderElement, filterInfo);
            if (!whereFilterFolderType.isEmpty()) {
                cursorQuery = this.mResolver.query(Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_MESSAGE), BluetoothMapContract.BT_MESSAGE_PROJECTION, (whereFilterFolderType + " AND flag_read=0 ") + setWhereFilterPeriod(bluetoothMapAppParams, filterInfo), null, "date DESC");
                if (cursorQuery != null) {
                    try {
                        count += cursorQuery.getCount();
                    } finally {
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        if (imSelected(bluetoothMapAppParams) && bluetoothMapFolderElement.hasImContent()) {
            filterInfo.mMsgType = 3;
            String whereFilter = setWhereFilter(bluetoothMapFolderElement, filterInfo, bluetoothMapAppParams);
            if (!whereFilter.isEmpty()) {
                cursorQuery = this.mResolver.query(Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_MESSAGE), BluetoothMapContract.BT_INSTANT_MESSAGE_PROJECTION, (whereFilter + " AND flag_read=0 ") + setWhereFilterPeriod(bluetoothMapAppParams, filterInfo), null, "date DESC");
                if (cursorQuery != null) {
                    try {
                        count += cursorQuery.getCount();
                    } finally {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        if (D) {
            Log.d(TAG, "msgListingHasUnread: numUnread = " + count);
        }
        return count > 0;
    }

    public BluetoothMapConvoListing convoListing(BluetoothMapAppParams bluetoothMapAppParams, boolean z) throws Throwable {
        Cursor cursorQuery;
        int i;
        Cursor cursor;
        String[] strArr;
        if (D) {
            Log.d(TAG, "convoListing:  messageType = " + bluetoothMapAppParams.getFilterMessageType());
        }
        BluetoothMapConvoListing bluetoothMapConvoListing = new BluetoothMapConvoListing();
        if (bluetoothMapAppParams.getConvoParameterMask() == -1 || bluetoothMapAppParams.getConvoParameterMask() == 0) {
            bluetoothMapAppParams.setConvoParameterMask(225L);
            if (D) {
                Log.v(TAG, "convoListing(): appParameterMask is zero or not present, changing to default: " + bluetoothMapAppParams.getConvoParameterMask());
            }
        }
        ?? r11 = 0;
        r11 = 0;
        FilterInfo filterInfo = new FilterInfo(this, r11);
        setFilterInfo(filterInfo);
        int i2 = 0;
        int startOffset = z ? 0 : bluetoothMapAppParams.getStartOffset();
        int i3 = (~bluetoothMapAppParams.getFilterMessageType()) & 31;
        int maxListCount = bluetoothMapAppParams.getMaxListCount() + bluetoothMapAppParams.getStartOffset();
        try {
            if (!smsSelected(filterInfo, bluetoothMapAppParams)) {
                try {
                    if (mmsSelected(bluetoothMapAppParams)) {
                        String str = "";
                        if (!z && bluetoothMapAppParams.getMaxListCount() > 0 && bluetoothMapAppParams.getFilterRecipient() == null) {
                            str = " LIMIT " + maxListCount;
                        }
                        StringBuilder sb = new StringBuilder("date DESC");
                        if (!z && ((i3 & (-4)) | 8) == 0 && bluetoothMapAppParams.getFilterRecipient() == null) {
                            str = " LIMIT " + bluetoothMapAppParams.getMaxListCount() + " OFFSET " + bluetoothMapAppParams.getStartOffset();
                            if (D) {
                                Log.d(TAG, "SMS Limit => " + str);
                            }
                            i = 0;
                        } else {
                            i = startOffset;
                        }
                        StringBuilder sb2 = new StringBuilder(120);
                        ArrayList arrayList = new ArrayList(12);
                        sb2.append("1=1 ");
                        setConvoWhereFilterSmsMms(sb2, arrayList, filterInfo, bluetoothMapAppParams);
                        if (arrayList.size() > 0) {
                            String[] strArr2 = new String[arrayList.size()];
                            arrayList.toArray(strArr2);
                            strArr = strArr2;
                        } else {
                            strArr = null;
                        }
                        Uri uriBuild = Telephony.Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();
                        sb.append(str);
                        if (D) {
                            Log.d(TAG, "Query using selection: " + sb2.toString() + " - sortOrder: " + sb.toString());
                        }
                        Cursor cursorQuery2 = this.mResolver.query(uriBuild, MMS_SMS_THREAD_PROJECTION, sb2.toString(), strArr, sb.toString());
                        if (cursorQuery2 != null) {
                            try {
                                if (D) {
                                    try {
                                        Log.d(TAG, "Found " + cursorQuery2.getCount() + " sms/mms conversations.");
                                    } catch (Throwable th) {
                                        th = th;
                                        cursorQuery = null;
                                        r11 = cursorQuery2;
                                    }
                                }
                                cursorQuery2.moveToPosition(-1);
                                if (bluetoothMapAppParams.getFilterRecipient() == null) {
                                    int i4 = 0;
                                    while (cursorQuery2.moveToNext()) {
                                        bluetoothMapConvoListing.add(createConvoElement(cursorQuery2, filterInfo, bluetoothMapAppParams));
                                        i4++;
                                        if (!z && i4 >= maxListCount) {
                                            break;
                                        }
                                    }
                                } else {
                                    SmsMmsContacts smsMmsContacts = new SmsMmsContacts();
                                    while (cursorQuery2.moveToNext()) {
                                        BluetoothMapConvoListingElement bluetoothMapConvoListingElementCreateConvoElement = createConvoElement(cursorQuery2, filterInfo, bluetoothMapAppParams);
                                        SmsMmsContacts smsMmsContacts2 = smsMmsContacts;
                                        if (addSmsMmsContacts(bluetoothMapConvoListingElementCreateConvoElement, smsMmsContacts, cursorQuery2.getString(MMS_SMS_THREAD_COL_RECIPIENT_IDS), bluetoothMapAppParams.getFilterRecipient(), bluetoothMapAppParams)) {
                                            bluetoothMapConvoListing.add(bluetoothMapConvoListingElementCreateConvoElement);
                                            if (!z && maxListCount <= 0) {
                                                break;
                                            }
                                        }
                                        smsMmsContacts = smsMmsContacts2;
                                    }
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                r11 = cursorQuery2;
                                cursorQuery = null;
                                if (cursorQuery != null) {
                                }
                                if (r11 != 0) {
                                }
                                if (D) {
                                }
                                throw th;
                            }
                        }
                        r11 = cursorQuery2;
                    } else {
                        i = startOffset;
                    }
                    try {
                        int i5 = 2;
                        if (emailSelected(bluetoothMapAppParams) || imSelected(bluetoothMapAppParams)) {
                            if (emailSelected(bluetoothMapAppParams)) {
                                filterInfo.mMsgType = 2;
                            } else if (imSelected(bluetoothMapAppParams)) {
                                filterInfo.mMsgType = 3;
                            }
                            if (D) {
                                Log.d(TAG, "msgType: " + filterInfo.mMsgType);
                            }
                            cursorQuery = this.mResolver.query(appendConvoListQueryParameters(bluetoothMapAppParams, Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_CONVERSATION)), BluetoothMapContract.BT_CONVERSATION_PROJECTION, null, null, "last_thread_activity DESC, thread_id ASC");
                            if (cursorQuery != null) {
                                try {
                                    filterInfo.setEmailImConvoColumns(cursorQuery);
                                    boolean zMoveToNext = cursorQuery.moveToNext();
                                    if (D) {
                                        Log.d(TAG, "Found " + cursorQuery.getCount() + " EMAIL/IM conversations. isValid = " + zMoveToNext);
                                    }
                                    int i6 = 0;
                                    while (zMoveToNext && (z || i6 < maxListCount)) {
                                        long j = cursorQuery.getLong(filterInfo.mConvoColConvoId);
                                        i6++;
                                        bluetoothMapConvoListing.add(createConvoElement(cursorQuery, filterInfo, bluetoothMapAppParams));
                                        while (cursorQuery.getLong(filterInfo.mConvoColConvoId) == j && (zMoveToNext = cursorQuery.moveToNext())) {
                                        }
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                        } else {
                            cursorQuery = null;
                        }
                        if (D) {
                            Log.d(TAG, "Done adding conversations - list size:" + bluetoothMapConvoListing.getCount());
                        }
                        if (z) {
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            if (r11 != 0) {
                                r11.close();
                            }
                            if (D) {
                                Log.d(TAG, "conversation end");
                            }
                            return bluetoothMapConvoListing;
                        }
                        bluetoothMapConvoListing.sort();
                        bluetoothMapConvoListing.segment(bluetoothMapAppParams.getMaxListCount(), i);
                        List<BluetoothMapConvoListingElement> list = bluetoothMapConvoListing.getList();
                        int size = list.size();
                        SmsMmsContacts smsMmsContacts3 = new SmsMmsContacts();
                        while (i2 < size) {
                            BluetoothMapConvoListingElement bluetoothMapConvoListingElement = list.get(i2);
                            switch (AnonymousClass1.$SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[bluetoothMapConvoListingElement.getType().ordinal()]) {
                                case 1:
                                case 2:
                                case 3:
                                    if (r11 != 0) {
                                        populateSmsMmsConvoElement(bluetoothMapConvoListingElement, r11, bluetoothMapAppParams, smsMmsContacts3);
                                    }
                                    if (D) {
                                        filterInfo.mMsgType = 3;
                                    }
                                    cursor = null;
                                    break;
                                case 4:
                                    filterInfo.mMsgType = i5;
                                    cursor = cursorQuery;
                                    break;
                                case 5:
                                    filterInfo.mMsgType = 3;
                                    cursor = cursorQuery;
                                    break;
                                default:
                                    cursor = null;
                                    break;
                            }
                            if (D) {
                                Log.d(TAG, "Working on cursor of type " + filterInfo.mMsgType);
                            }
                            if (cursor != null) {
                                populateImEmailConvoElement(bluetoothMapConvoListingElement, cursor, bluetoothMapAppParams, filterInfo);
                            } else if (D) {
                                Log.d(TAG, "tmpCursor is Null - something is wrong - or the message is of type SMS/MMS");
                            }
                            i2++;
                            i5 = 2;
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        if (r11 != 0) {
                            r11.close();
                        }
                        if (D) {
                            Log.d(TAG, "conversation end");
                        }
                        return bluetoothMapConvoListing;
                    } catch (Throwable th4) {
                        th = th4;
                        cursorQuery = null;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    cursorQuery = null;
                }
            }
        } catch (Throwable th6) {
            th = th6;
            cursorQuery = null;
            r11 = 0;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        if (r11 != 0) {
            r11.close();
        }
        if (D) {
            Log.d(TAG, "conversation end");
        }
        throw th;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE = new int[BluetoothMapUtils.TYPE.values().length];

        static {
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[BluetoothMapUtils.TYPE.SMS_CDMA.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[BluetoothMapUtils.TYPE.SMS_GSM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[BluetoothMapUtils.TYPE.MMS.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[BluetoothMapUtils.TYPE.EMAIL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[BluetoothMapUtils.TYPE.IM.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    boolean refreshSmsMmsConvoVersions() {
        boolean z;
        Cursor cursorQuery = this.mResolver.query(Telephony.Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build(), MMS_SMS_THREAD_PROJECTION, null, null, "date DESC");
        boolean z2 = false;
        if (cursorQuery != null) {
            try {
                if (D) {
                    Log.d(TAG, "Found " + cursorQuery.getCount() + " sms/mms conversations.");
                }
                cursorQuery.moveToPosition(-1);
                synchronized (getSmsMmsConvoList()) {
                    HashMap<Long, BluetoothMapConvoListingElement> map = new HashMap<>(Math.max(getSmsMmsConvoList().size(), cursorQuery.getCount()));
                    boolean z3 = false;
                    while (cursorQuery.moveToNext()) {
                        Long lValueOf = Long.valueOf(cursorQuery.getLong(MMS_SMS_THREAD_COL_ID));
                        BluetoothMapConvoListingElement bluetoothMapConvoListingElementRemove = getSmsMmsConvoList().remove(lValueOf);
                        if (bluetoothMapConvoListingElementRemove == null) {
                            bluetoothMapConvoListingElementRemove = new BluetoothMapConvoListingElement();
                            bluetoothMapConvoListingElementRemove.setConvoId(1L, lValueOf.longValue());
                            bluetoothMapConvoListingElementRemove.setVersionCounter(0L);
                            z3 = true;
                        }
                        long j = cursorQuery.getLong(MMS_SMS_THREAD_COL_DATE);
                        boolean z4 = cursorQuery.getInt(MMS_SMS_THREAD_COL_READ) == 1;
                        if (j == bluetoothMapConvoListingElementRemove.getLastActivity()) {
                            z = false;
                        } else {
                            bluetoothMapConvoListingElementRemove.setLastActivity(j);
                            z = true;
                        }
                        if (z4 != bluetoothMapConvoListingElementRemove.getReadBool()) {
                            bluetoothMapConvoListingElementRemove.setRead(z4, false);
                            z = true;
                        }
                        String string = cursorQuery.getString(MMS_SMS_THREAD_COL_RECIPIENT_IDS);
                        if (!string.equals(bluetoothMapConvoListingElementRemove.getSmsMmsContacts())) {
                            bluetoothMapConvoListingElementRemove.setSmsMmsContacts(string);
                            z3 = true;
                        }
                        if (z) {
                            bluetoothMapConvoListingElementRemove.incrementVersionCounter();
                            z3 = true;
                        }
                        map.put(lValueOf, bluetoothMapConvoListingElementRemove);
                    }
                    z2 = getSmsMmsConvoList().size() != 0 ? true : z3;
                    setSmsMmsConvoList(map);
                }
                if (z2) {
                    this.mMasInstance.updateSmsMmsConvoListVersionCounter();
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        return z2;
    }

    boolean refreshImEmailConvoVersions() {
        boolean z;
        FilterInfo filterInfo = new FilterInfo(this, null);
        Cursor cursorQuery = this.mResolver.query(Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_CONVERSATION), CONVO_VERSION_PROJECTION, null, null, "last_thread_activity DESC, thread_id ASC");
        if (cursorQuery != null) {
            try {
                filterInfo.setEmailImConvoColumns(cursorQuery);
                boolean zMoveToNext = cursorQuery.moveToNext();
                synchronized (getImEmailConvoList()) {
                    HashMap<Long, BluetoothMapConvoListingElement> map = new HashMap<>(Math.max(getImEmailConvoList().size(), cursorQuery.getCount()));
                    boolean z2 = false;
                    boolean z3 = false;
                    while (zMoveToNext) {
                        long j = cursorQuery.getLong(filterInfo.mConvoColConvoId);
                        BluetoothMapConvoListingElement bluetoothMapConvoListingElementRemove = getImEmailConvoList().remove(Long.valueOf(j));
                        if (bluetoothMapConvoListingElementRemove == null) {
                            bluetoothMapConvoListingElementRemove = new BluetoothMapConvoListingElement();
                            bluetoothMapConvoListingElementRemove.setConvoId(2L, j);
                            bluetoothMapConvoListingElementRemove.setVersionCounter(0L);
                            z2 = true;
                        }
                        String string = cursorQuery.getString(filterInfo.mConvoColName);
                        String string2 = cursorQuery.getString(filterInfo.mConvoColSummary);
                        boolean zMoveToNext2 = zMoveToNext;
                        long j2 = cursorQuery.getLong(filterInfo.mConvoColLastActivity);
                        boolean z4 = cursorQuery.getInt(filterInfo.mConvoColRead) == 1;
                        if (j2 != bluetoothMapConvoListingElementRemove.getLastActivity()) {
                            bluetoothMapConvoListingElementRemove.setLastActivity(j2);
                            z3 = true;
                        }
                        if (z4 != bluetoothMapConvoListingElementRemove.getReadBool()) {
                            bluetoothMapConvoListingElementRemove.setRead(z4, false);
                            z3 = true;
                        }
                        if (string != null && !string.equals(bluetoothMapConvoListingElementRemove.getName())) {
                            bluetoothMapConvoListingElementRemove.setName(string);
                            z3 = true;
                        }
                        if (string2 != null && !string2.equals(bluetoothMapConvoListingElementRemove.getFullSummary())) {
                            bluetoothMapConvoListingElementRemove.setSummary(string2);
                            z3 = true;
                        }
                        while (cursorQuery.getLong(filterInfo.mConvoColConvoId) == j && (zMoveToNext2 = cursorQuery.moveToNext())) {
                        }
                        zMoveToNext = zMoveToNext2;
                        if (z3) {
                            bluetoothMapConvoListingElementRemove.incrementVersionCounter();
                            z2 = true;
                        }
                        map.put(Long.valueOf(j), bluetoothMapConvoListingElementRemove);
                    }
                    z = getImEmailConvoList().size() != 0 ? true : z2;
                    setImEmailConvoList(map);
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        } else {
            z = false;
        }
        if (z) {
            this.mMasInstance.updateImEmailConvoListVersionCounter();
        }
        return z;
    }

    private void updateSmsMmsConvoVersion(Cursor cursor, BluetoothMapConvoListingElement bluetoothMapConvoListingElement) {
        boolean z;
        boolean z2;
        long cpConvoId = bluetoothMapConvoListingElement.getCpConvoId();
        BluetoothMapConvoListingElement bluetoothMapConvoListingElement2 = getSmsMmsConvoList().get(Long.valueOf(cpConvoId));
        if (bluetoothMapConvoListingElement2 != null) {
            z = false;
        } else {
            bluetoothMapConvoListingElement2 = new BluetoothMapConvoListingElement();
            getSmsMmsConvoList().put(Long.valueOf(cpConvoId), bluetoothMapConvoListingElement2);
            bluetoothMapConvoListingElement2.setConvoId(1L, cpConvoId);
            bluetoothMapConvoListingElement2.setVersionCounter(0L);
            z = true;
        }
        long j = cursor.getLong(MMS_SMS_THREAD_COL_DATE);
        boolean z3 = cursor.getInt(MMS_SMS_THREAD_COL_READ) == 1;
        if (j == bluetoothMapConvoListingElement2.getLastActivity()) {
            z2 = false;
        } else {
            bluetoothMapConvoListingElement2.setLastActivity(j);
            z2 = true;
        }
        if (z3 != bluetoothMapConvoListingElement2.getReadBool()) {
            bluetoothMapConvoListingElement2.setRead(z3, false);
            z2 = true;
        }
        if (z2) {
            bluetoothMapConvoListingElement2.incrementVersionCounter();
            z = true;
        }
        if (z) {
            this.mMasInstance.updateSmsMmsConvoListVersionCounter();
        }
        bluetoothMapConvoListingElement.setVersionCounter(bluetoothMapConvoListingElement2.getVersionCounter());
    }

    private void updateImEmailConvoVersion(Cursor cursor, FilterInfo filterInfo, BluetoothMapConvoListingElement bluetoothMapConvoListingElement) {
        boolean z;
        boolean z2;
        long cpConvoId = bluetoothMapConvoListingElement.getCpConvoId();
        BluetoothMapConvoListingElement bluetoothMapConvoListingElement2 = getImEmailConvoList().get(Long.valueOf(cpConvoId));
        if (bluetoothMapConvoListingElement2 != null) {
            z = false;
        } else {
            bluetoothMapConvoListingElement2 = new BluetoothMapConvoListingElement();
            bluetoothMapConvoListingElement2.setConvoId(2L, cpConvoId);
            getImEmailConvoList().put(Long.valueOf(cpConvoId), bluetoothMapConvoListingElement2);
            bluetoothMapConvoListingElement2.setVersionCounter(0L);
            z = true;
        }
        String string = cursor.getString(filterInfo.mConvoColName);
        long j = cursor.getLong(filterInfo.mConvoColLastActivity);
        boolean z3 = cursor.getInt(filterInfo.mConvoColRead) == 1;
        if (j == bluetoothMapConvoListingElement2.getLastActivity()) {
            z2 = false;
        } else {
            bluetoothMapConvoListingElement2.setLastActivity(j);
            z2 = true;
        }
        if (z3 != bluetoothMapConvoListingElement2.getReadBool()) {
            bluetoothMapConvoListingElement2.setRead(z3, false);
            z2 = true;
        }
        if (string != null && !string.equals(bluetoothMapConvoListingElement2.getName())) {
            bluetoothMapConvoListingElement2.setName(string);
            z2 = true;
        }
        if (z2) {
            bluetoothMapConvoListingElement2.incrementVersionCounter();
            z = true;
        }
        if (z) {
            this.mMasInstance.updateImEmailConvoListVersionCounter();
        }
        bluetoothMapConvoListingElement.setVersionCounter(bluetoothMapConvoListingElement2.getVersionCounter());
    }

    private void populateSmsMmsConvoElement(BluetoothMapConvoListingElement bluetoothMapConvoListingElement, Cursor cursor, BluetoothMapAppParams bluetoothMapAppParams, SmsMmsContacts smsMmsContacts) {
        cursor.moveToPosition(bluetoothMapConvoListingElement.getCursorIndex());
        int convoParameterMask = (int) bluetoothMapAppParams.getConvoParameterMask();
        bluetoothMapConvoListingElement.setConvoId(1L, cursor.getLong(MMS_SMS_THREAD_COL_ID));
        boolean z = cursor.getInt(MMS_SMS_THREAD_COL_READ) == 1;
        if ((convoParameterMask & 4) != 0) {
            bluetoothMapConvoListingElement.setRead(z, true);
        } else {
            bluetoothMapConvoListingElement.setRead(z, false);
        }
        if ((convoParameterMask & 2) != 0) {
            bluetoothMapConvoListingElement.setLastActivity(cursor.getLong(MMS_SMS_THREAD_COL_DATE));
        } else {
            bluetoothMapConvoListingElement.setLastActivity(-1L);
        }
        if ((convoParameterMask & 8) != 0) {
            updateSmsMmsConvoVersion(cursor, bluetoothMapConvoListingElement);
        }
        if ((convoParameterMask & 1) != 0) {
            bluetoothMapConvoListingElement.setName("");
        }
        if ((convoParameterMask & 16) != 0) {
            String string = cursor.getString(MMS_SMS_THREAD_COL_SNIPPET);
            String string2 = cursor.getString(MMS_SMS_THREAD_COL_SNIPPET_CS);
            if (string != null && string2 != null && !string2.equals("UTF-8")) {
                try {
                    string = new String(string.getBytes(string2), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "populateSmsMmsConvoElement: " + e);
                }
            }
            bluetoothMapConvoListingElement.setSummary(string);
        }
        if ((convoParameterMask & 32) != 0 && bluetoothMapAppParams.getFilterRecipient() == null) {
            addSmsMmsContacts(bluetoothMapConvoListingElement, smsMmsContacts, cursor.getString(MMS_SMS_THREAD_COL_RECIPIENT_IDS), null, bluetoothMapAppParams);
        }
    }

    private void populateImEmailConvoElement(BluetoothMapConvoListingElement bluetoothMapConvoListingElement, Cursor cursor, BluetoothMapAppParams bluetoothMapAppParams, FilterInfo filterInfo) {
        cursor.moveToPosition(bluetoothMapConvoListingElement.getCursorIndex());
        int convoParameterMask = (int) bluetoothMapAppParams.getConvoParameterMask();
        long j = cursor.getLong(filterInfo.mConvoColConvoId);
        bluetoothMapConvoListingElement.setConvoId(2L, j);
        if ((convoParameterMask & 1) != 0) {
            bluetoothMapConvoListingElement.setName(cursor.getString(filterInfo.mConvoColName));
        }
        bluetoothMapConvoListingElement.setRead(1 == cursor.getInt(filterInfo.mConvoColRead), (convoParameterMask & 4) != 0);
        long j2 = cursor.getLong(filterInfo.mConvoColLastActivity);
        if ((convoParameterMask & 2) != 0) {
            bluetoothMapConvoListingElement.setLastActivity(j2);
        } else {
            bluetoothMapConvoListingElement.setLastActivity(-1L);
        }
        if ((convoParameterMask & 8) != 0) {
            updateImEmailConvoVersion(cursor, filterInfo, bluetoothMapConvoListingElement);
        }
        if ((convoParameterMask & 16) != 0) {
            bluetoothMapConvoListingElement.setSummary(cursor.getString(filterInfo.mConvoColSummary));
        }
        if ((convoParameterMask & 32) != 0) {
            do {
                BluetoothMapConvoContactElement bluetoothMapConvoContactElement = new BluetoothMapConvoContactElement();
                if ((convoParameterMask & 1024) != 0) {
                    bluetoothMapConvoContactElement.setBtUid(new SignedLongLong(cursor.getLong(filterInfo.mContactColBtUid), 0L));
                }
                if ((convoParameterMask & 256) != 0) {
                    bluetoothMapConvoContactElement.setChatState(cursor.getInt(filterInfo.mContactColChatState));
                }
                if ((convoParameterMask & 4096) != 0) {
                    bluetoothMapConvoContactElement.setPresenceAvailability(cursor.getInt(filterInfo.mContactColPresenceState));
                }
                if ((convoParameterMask & 8192) != 0) {
                    bluetoothMapConvoContactElement.setPresenceStatus(cursor.getString(filterInfo.mContactColPresenceText));
                }
                if ((convoParameterMask & 16384) != 0) {
                    bluetoothMapConvoContactElement.setPriority(cursor.getInt(filterInfo.mContactColPriority));
                }
                if ((convoParameterMask & 128) != 0) {
                    bluetoothMapConvoContactElement.setDisplayName(cursor.getString(filterInfo.mContactColNickname));
                }
                if ((convoParameterMask & 64) != 0) {
                    bluetoothMapConvoContactElement.setContactId(cursor.getString(filterInfo.mContactColContactUci));
                }
                if ((convoParameterMask & 512) != 0) {
                    bluetoothMapConvoContactElement.setLastActivity(cursor.getLong(filterInfo.mContactColLastActive));
                }
                if ((convoParameterMask & 2048) != 0) {
                    bluetoothMapConvoContactElement.setName(cursor.getString(filterInfo.mContactColName));
                }
                bluetoothMapConvoListingElement.addContact(bluetoothMapConvoContactElement);
                if (!cursor.moveToNext()) {
                    return;
                }
            } while (cursor.getLong(filterInfo.mConvoColConvoId) == j);
        }
    }

    private Uri appendConvoListQueryParameters(BluetoothMapAppParams bluetoothMapAppParams, Uri uri) {
        Uri.Builder builderBuildUpon = uri.buildUpon();
        String filterRecipient = bluetoothMapAppParams.getFilterRecipient();
        if (filterRecipient != null) {
            builderBuildUpon.appendQueryParameter(BluetoothMapContract.FILTER_ORIGINATOR_SUBSTRING, filterRecipient.trim().replace("*", "%"));
        }
        long filterLastActivityBegin = bluetoothMapAppParams.getFilterLastActivityBegin();
        if (filterLastActivityBegin > 0) {
            builderBuildUpon.appendQueryParameter(BluetoothMapContract.FILTER_PERIOD_BEGIN, Long.toString(filterLastActivityBegin));
        }
        long filterLastActivityEnd = bluetoothMapAppParams.getFilterLastActivityEnd();
        if (filterLastActivityEnd > 0) {
            builderBuildUpon.appendQueryParameter(BluetoothMapContract.FILTER_PERIOD_END, Long.toString(filterLastActivityEnd));
        }
        int filterReadStatus = bluetoothMapAppParams.getFilterReadStatus();
        if (filterReadStatus > 0) {
            if (filterReadStatus == 1) {
                builderBuildUpon.appendQueryParameter(BluetoothMapContract.FILTER_READ_STATUS, "false");
            } else if (filterReadStatus == 2) {
                builderBuildUpon.appendQueryParameter(BluetoothMapContract.FILTER_READ_STATUS, "true");
            }
        }
        long leastSignificantBits = -1;
        if (bluetoothMapAppParams.getFilterConvoId() != null) {
            leastSignificantBits = bluetoothMapAppParams.getFilterConvoId().getLeastSignificantBits();
        }
        if (leastSignificantBits > 0) {
            builderBuildUpon.appendQueryParameter("thread_id", Long.toString(leastSignificantBits));
        }
        return builderBuildUpon.build();
    }

    private boolean addSmsMmsContacts(BluetoothMapConvoListingElement bluetoothMapConvoListingElement, SmsMmsContacts smsMmsContacts, String str, String str2, BluetoothMapAppParams bluetoothMapAppParams) {
        BluetoothMapConvoContactElement bluetoothMapConvoContactElementCreateFromMapContact;
        int convoParameterMask = (int) bluetoothMapAppParams.getConvoParameterMask();
        String[] strArrSplit = str.split(" ");
        long[] jArr = new long[strArrSplit.length];
        String strTrim = str2 != null ? str2.trim() : str2;
        boolean z = false;
        for (int i = 0; i < strArrSplit.length; i++) {
            try {
                long j = Long.parseLong(strArrSplit[i]);
                jArr[i] = j;
                if (strTrim != null) {
                    String phoneNumber = smsMmsContacts.getPhoneNumber(this.mResolver, j);
                    if (phoneNumber != null) {
                        MapContact contactNameFromPhone = smsMmsContacts.getContactNameFromPhone(phoneNumber, this.mResolver, strTrim);
                        if (D) {
                            Log.d(TAG, "  id " + j + ": " + phoneNumber);
                            if (contactNameFromPhone != null) {
                                Log.d(TAG, "  contact name: " + contactNameFromPhone.getName() + "  X-BT-UID: " + contactNameFromPhone.getXBtUid());
                            }
                        }
                        if (contactNameFromPhone != null) {
                            z = true;
                        }
                    }
                }
            } catch (NumberFormatException e) {
            }
        }
        if (z) {
            z = false;
            for (long j2 : jArr) {
                String phoneNumber2 = smsMmsContacts.getPhoneNumber(this.mResolver, j2);
                if (phoneNumber2 != null) {
                    MapContact contactNameFromPhone2 = smsMmsContacts.getContactNameFromPhone(phoneNumber2, this.mResolver);
                    if (contactNameFromPhone2 == null) {
                        bluetoothMapConvoContactElementCreateFromMapContact = new BluetoothMapConvoContactElement();
                        if ((convoParameterMask & 2048) != 0) {
                            bluetoothMapConvoContactElementCreateFromMapContact.setName(phoneNumber2);
                        }
                        if ((convoParameterMask & 64) != 0) {
                            bluetoothMapConvoContactElementCreateFromMapContact.setContactId(phoneNumber2);
                        }
                    } else {
                        bluetoothMapConvoContactElementCreateFromMapContact = BluetoothMapConvoContactElement.createFromMapContact(contactNameFromPhone2, phoneNumber2);
                        if ((convoParameterMask & 64) == 0) {
                            bluetoothMapConvoContactElementCreateFromMapContact.setContactId(null);
                        }
                        if ((convoParameterMask & 1024) == 0) {
                            bluetoothMapConvoContactElementCreateFromMapContact.setBtUid(null);
                        }
                        if ((convoParameterMask & 128) == 0) {
                            bluetoothMapConvoContactElementCreateFromMapContact.setDisplayName(null);
                        }
                    }
                    bluetoothMapConvoListingElement.addContact(bluetoothMapConvoContactElementCreateFromMapContact);
                    z = true;
                }
            }
        }
        return z;
    }

    private String getFolderName(int i, int i2) {
        if (i2 == -1) {
            return BluetoothMapContract.FOLDER_NAME_DELETED;
        }
        switch (i) {
            case 1:
                return BluetoothMapContract.FOLDER_NAME_INBOX;
            case 2:
                return BluetoothMapContract.FOLDER_NAME_SENT;
            case 3:
                return BluetoothMapContract.FOLDER_NAME_DRAFT;
            case 4:
            case 5:
            case 6:
                return BluetoothMapContract.FOLDER_NAME_OUTBOX;
            default:
                return "";
        }
    }

    public byte[] getMessage(String str, BluetoothMapAppParams bluetoothMapAppParams, BluetoothMapFolderElement bluetoothMapFolderElement, String str2) throws UnsupportedEncodingException {
        BluetoothMapUtils.TYPE msgTypeFromHandle = BluetoothMapUtils.getMsgTypeFromHandle(str);
        this.mMessageVersion = str2;
        long cpHandle = BluetoothMapUtils.getCpHandle(str);
        if (bluetoothMapAppParams.getFractionRequest() == 1) {
            throw new IllegalArgumentException("FRACTION_REQUEST_NEXT does not make sence as we always return the full message.");
        }
        switch (AnonymousClass1.$SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[msgTypeFromHandle.ordinal()]) {
            case 1:
            case 2:
                return getSmsMessage(cpHandle, bluetoothMapAppParams.getCharset());
            case 3:
                return getMmsMessage(cpHandle, bluetoothMapAppParams);
            case 4:
                return getEmailMessage(cpHandle, bluetoothMapAppParams, bluetoothMapFolderElement);
            case 5:
                return getIMMessage(cpHandle, bluetoothMapAppParams, bluetoothMapFolderElement);
            default:
                throw new IllegalArgumentException("Invalid message handle.");
        }
    }

    private String setVCardFromPhoneNumber(BluetoothMapbMessage bluetoothMapbMessage, String str, boolean z) throws Throwable {
        String string;
        String string2;
        Cursor cursor;
        String[] strArr = new String[1];
        String[] strArr2 = null;
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        int i = 0;
        strArr[0] = str;
        Cursor cursorQuery = this.mResolver.query(Uri.withAppendedPath(ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI, Uri.encode(str)), new String[]{"_id", "display_name"}, "in_visible_group=1", null, "_id ASC");
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    string = cursorQuery.getString(cursorQuery.getColumnIndex("_id"));
                    string2 = cursorQuery.getString(cursorQuery.getColumnIndex("display_name"));
                } else {
                    string = null;
                    string2 = null;
                }
            } finally {
                close(cursorQuery);
            }
        }
        if (string != null) {
            try {
                cursorQuery = this.mResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, "contact_id = ?", new String[]{string}, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            strArr2 = new String[cursorQuery.getCount()];
                            while (true) {
                                int i2 = i + 1;
                                strArr2[i] = cursorQuery.getString(cursorQuery.getColumnIndex("data1"));
                                if (cursorQuery == null || !cursorQuery.moveToNext()) {
                                    break;
                                }
                                i = i2;
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        close(cursor);
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                cursor = null;
            }
        }
        String[] strArr3 = strArr2;
        if (z) {
            bluetoothMapbMessage.addOriginator(string2, string2, strArr, strArr3, null, null);
        } else {
            bluetoothMapbMessage.addRecipient(string2, string2, strArr, strArr3, null, null);
        }
        return string2;
    }

    public byte[] getSmsMessage(long j, int i) throws UnsupportedEncodingException {
        BluetoothMapbMessageSms bluetoothMapbMessageSms = new BluetoothMapbMessageSms();
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        Cursor cursorQuery = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, "_ID = " + j, null, null);
        if (cursorQuery == null || !cursorQuery.moveToFirst()) {
            throw new IllegalArgumentException("SMS handle not found");
        }
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    if (telephonyManager.getPhoneType() == 2) {
                        bluetoothMapbMessageSms.setType(BluetoothMapUtils.TYPE.SMS_CDMA);
                    } else {
                        bluetoothMapbMessageSms.setType(BluetoothMapUtils.TYPE.SMS_GSM);
                    }
                    bluetoothMapbMessageSms.setVersionString(this.mMessageVersion);
                    if (cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.FILTER_READ_STATUS)).equalsIgnoreCase("1")) {
                        bluetoothMapbMessageSms.setStatus(true);
                    } else {
                        bluetoothMapbMessageSms.setStatus(false);
                    }
                    int i2 = cursorQuery.getInt(cursorQuery.getColumnIndex(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE));
                    int i3 = cursorQuery.getInt(cursorQuery.getColumnIndex("thread_id"));
                    bluetoothMapbMessageSms.setFolder(getFolderName(i2, i3));
                    String string = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.BODY));
                    String string2 = cursorQuery.getString(cursorQuery.getColumnIndex("address"));
                    if (string2 == null && i2 == 3) {
                        string2 = getCanonicalAddressSms(this.mResolver, i3);
                    }
                    long j2 = cursorQuery.getLong(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.DATE));
                    if (i2 == 1) {
                        setVCardFromPhoneNumber(bluetoothMapbMessageSms, string2, true);
                    } else {
                        setVCardFromPhoneNumber(bluetoothMapbMessageSms, string2, false);
                    }
                    if (i == 0) {
                        if (i2 == 1) {
                            bluetoothMapbMessageSms.setSmsBodyPdus(BluetoothMapSmsPdu.getDeliverPdus(string, string2, j2));
                        } else {
                            bluetoothMapbMessageSms.setSmsBodyPdus(BluetoothMapSmsPdu.getSubmitPdus(string, string2));
                        }
                    } else {
                        bluetoothMapbMessageSms.setSmsBody(string);
                    }
                    return bluetoothMapbMessageSms.encode();
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
        return bluetoothMapbMessageSms.encode();
    }

    private void extractMmsAddresses(long j, BluetoothMapbMessageMime bluetoothMapbMessageMime) {
        Cursor cursorQuery = this.mResolver.query(Uri.parse(new String(Telephony.Mms.CONTENT_URI + "/" + j + "/addr")), null, new String("msg_id=" + j), null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    do {
                        String string = cursorQuery.getString(cursorQuery.getColumnIndex("address"));
                        if (!string.equals(INSERT_ADDRES_TOKEN)) {
                            int iIntValue = Integer.valueOf(cursorQuery.getInt(cursorQuery.getColumnIndex(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE))).intValue();
                            if (iIntValue == 137) {
                                bluetoothMapbMessageMime.addFrom(setVCardFromPhoneNumber(bluetoothMapbMessageMime, string, true), string);
                            } else if (iIntValue == 151) {
                                bluetoothMapbMessageMime.addTo(setVCardFromPhoneNumber(bluetoothMapbMessageMime, string, false), string);
                            } else {
                                switch (iIntValue) {
                                    case 129:
                                        bluetoothMapbMessageMime.addBcc(setVCardFromPhoneNumber(bluetoothMapbMessageMime, string, false), string);
                                        break;
                                    case 130:
                                        bluetoothMapbMessageMime.addCc(setVCardFromPhoneNumber(bluetoothMapbMessageMime, string, false), string);
                                        break;
                                }
                            }
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

    private byte[] readRawDataPart(Uri uri, long j) throws Throwable {
        InputStream inputStreamOpenInputStream;
        Uri uri2 = Uri.parse(new String(uri + "/" + j));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[8192];
        try {
            inputStreamOpenInputStream = this.mResolver.openInputStream(uri2);
        } catch (IOException e) {
            e = e;
            inputStreamOpenInputStream = null;
        } catch (Throwable th) {
            th = th;
            inputStreamOpenInputStream = null;
        }
        while (true) {
            try {
                try {
                    int i = inputStreamOpenInputStream.read(bArr);
                    if (i == -1) {
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        close(byteArrayOutputStream);
                        close(inputStreamOpenInputStream);
                        return byteArray;
                    }
                    byteArrayOutputStream.write(bArr, 0, i);
                } catch (IOException e2) {
                    e = e2;
                    Log.w(TAG, "Error reading part data", e);
                    close(byteArrayOutputStream);
                    close(inputStreamOpenInputStream);
                    return null;
                }
            } catch (Throwable th2) {
                th = th2;
            }
            th = th2;
            close(byteArrayOutputStream);
            close(inputStreamOpenInputStream);
            throw th;
        }
    }

    private void extractMmsParts(long j, BluetoothMapbMessageMime bluetoothMapbMessageMime) {
        Cursor cursorQuery = this.mResolver.query(Uri.parse(new String(Telephony.Mms.CONTENT_URI + "/" + j + "/part")), null, new String("mid=" + j), null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    do {
                        Long lValueOf = Long.valueOf(cursorQuery.getLong(cursorQuery.getColumnIndex("_id")));
                        String string = cursorQuery.getString(cursorQuery.getColumnIndex("ct"));
                        String string2 = cursorQuery.getString(cursorQuery.getColumnIndex("name"));
                        String string3 = cursorQuery.getString(cursorQuery.getColumnIndex("chset"));
                        String string4 = cursorQuery.getString(cursorQuery.getColumnIndex("fn"));
                        String string5 = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessagePartColumns.TEXT));
                        Integer.valueOf(cursorQuery.getInt(cursorQuery.getColumnIndex(BluetoothShare._DATA)));
                        String string6 = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessagePartColumns.CONTENT_ID));
                        String string7 = cursorQuery.getString(cursorQuery.getColumnIndex("cl"));
                        String string8 = cursorQuery.getString(cursorQuery.getColumnIndex("cd"));
                        BluetoothMapbMessageMime.MimePart mimePartAddMimePart = bluetoothMapbMessageMime.addMimePart();
                        mimePartAddMimePart.mContentType = string;
                        mimePartAddMimePart.mPartName = string2;
                        mimePartAddMimePart.mContentId = string6;
                        mimePartAddMimePart.mContentLocation = string7;
                        mimePartAddMimePart.mContentDisposition = string8;
                        if (string5 != null) {
                            try {
                                mimePartAddMimePart.mData = string5.getBytes("UTF-8");
                                mimePartAddMimePart.mCharsetName = "utf-8";
                            } catch (UnsupportedEncodingException e) {
                                Log.d(TAG, "extractMmsParts", e);
                                mimePartAddMimePart.mData = null;
                                mimePartAddMimePart.mCharsetName = null;
                            } catch (NumberFormatException e2) {
                                Log.d(TAG, "extractMmsParts", e2);
                                mimePartAddMimePart.mData = null;
                                mimePartAddMimePart.mCharsetName = null;
                            }
                        } else {
                            mimePartAddMimePart.mData = readRawDataPart(Uri.parse(Telephony.Mms.CONTENT_URI + "/part"), lValueOf.longValue());
                            if (string3 != null) {
                                mimePartAddMimePart.mCharsetName = CharacterSets.getMimeName(Integer.parseInt(string3));
                            }
                        }
                        mimePartAddMimePart.mFileName = string4;
                    } while (cursorQuery.moveToNext());
                    bluetoothMapbMessageMime.updateCharset();
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
    }

    private void extractIMParts(long j, BluetoothMapbMessageMime bluetoothMapbMessageMime) {
        Cursor cursorQuery = this.mResolver.query(Uri.parse(new String(this.mBaseUri + BluetoothMapContract.TABLE_MESSAGE + "/" + j + "/part")), null, new String("_id=" + j), null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                do {
                    Long lValueOf = Long.valueOf(cursorQuery.getLong(cursorQuery.getColumnIndex("_id")));
                    String string = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessagePartColumns.CHARSET));
                    String string2 = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessagePartColumns.FILENAME));
                    String string3 = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessagePartColumns.TEXT));
                    String string4 = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessagePartColumns.RAW_DATA));
                    String string5 = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessagePartColumns.CONTENT_ID));
                    BluetoothMapbMessageMime.MimePart mimePartAddMimePart = bluetoothMapbMessageMime.addMimePart();
                    mimePartAddMimePart.mContentId = string5;
                    try {
                        if (string3.equalsIgnoreCase("yes")) {
                            mimePartAddMimePart.mData = string4.getBytes("UTF-8");
                            mimePartAddMimePart.mCharsetName = "utf-8";
                        } else {
                            mimePartAddMimePart.mData = readRawDataPart(Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_MESSAGE_PART), lValueOf.longValue());
                            if (string != null) {
                                mimePartAddMimePart.mCharsetName = CharacterSets.getMimeName(Integer.parseInt(string));
                            }
                        }
                    } catch (UnsupportedEncodingException e) {
                        Log.d(TAG, "extractIMParts", e);
                        mimePartAddMimePart.mData = null;
                        mimePartAddMimePart.mCharsetName = null;
                    } catch (NumberFormatException e2) {
                        Log.d(TAG, "extractIMParts", e2);
                        mimePartAddMimePart.mData = null;
                        mimePartAddMimePart.mCharsetName = null;
                    }
                    mimePartAddMimePart.mFileName = string2;
                } while (cursorQuery.moveToNext());
            }
            bluetoothMapbMessageMime.updateCharset();
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    public byte[] getMmsMessage(long j, BluetoothMapAppParams bluetoothMapAppParams) throws UnsupportedEncodingException {
        if (bluetoothMapAppParams.getCharset() == 0) {
            throw new IllegalArgumentException("MMS charset native not allowed for MMS - must be utf-8");
        }
        BluetoothMapbMessageMime bluetoothMapbMessageMime = new BluetoothMapbMessageMime();
        Cursor cursorQuery = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION, "_ID = " + j, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    bluetoothMapbMessageMime.setType(BluetoothMapUtils.TYPE.MMS);
                    bluetoothMapbMessageMime.setVersionString(this.mMessageVersion);
                    if (cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.FILTER_READ_STATUS)).equalsIgnoreCase("1")) {
                        bluetoothMapbMessageMime.setStatus(true);
                    } else {
                        bluetoothMapbMessageMime.setStatus(false);
                    }
                    bluetoothMapbMessageMime.setFolder(getFolderName(cursorQuery.getInt(cursorQuery.getColumnIndex("msg_box")), cursorQuery.getInt(cursorQuery.getColumnIndex("thread_id"))));
                    bluetoothMapbMessageMime.setSubject(cursorQuery.getString(cursorQuery.getColumnIndex("sub")));
                    bluetoothMapbMessageMime.setMessageId(cursorQuery.getString(cursorQuery.getColumnIndex("m_id")));
                    bluetoothMapbMessageMime.setContentType(cursorQuery.getString(cursorQuery.getColumnIndex("ct_t")));
                    bluetoothMapbMessageMime.setDate(cursorQuery.getLong(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.DATE)) * 1000);
                    bluetoothMapbMessageMime.setTextOnly(cursorQuery.getInt(cursorQuery.getColumnIndex("text_only")) != 0);
                    bluetoothMapbMessageMime.setIncludeAttachments(bluetoothMapAppParams.getAttachment() != 0);
                    extractMmsParts(j, bluetoothMapbMessageMime);
                    extractMmsAddresses(j, bluetoothMapbMessageMime);
                    return bluetoothMapbMessageMime.encode();
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
        return bluetoothMapbMessageMime.encode();
    }

    public byte[] getEmailMessage(long j, BluetoothMapAppParams bluetoothMapAppParams, BluetoothMapFolderElement bluetoothMapFolderElement) throws UnsupportedEncodingException {
        ?? r3;
        Throwable th;
        ?? fileInputStream;
        StringBuilder sb;
        if (D && bluetoothMapAppParams != null) {
            Log.d(TAG, "TYPE_MESSAGE (GET): Attachment = " + bluetoothMapAppParams.getAttachment() + ", Charset = " + bluetoothMapAppParams.getCharset() + ", FractionRequest = " + bluetoothMapAppParams.getFractionRequest());
        }
        if (bluetoothMapAppParams.getCharset() == 0) {
            throw new IllegalArgumentException("EMAIL charset not UTF-8");
        }
        BluetoothMapbMessageEmail bluetoothMapbMessageEmail = new BluetoothMapbMessageEmail();
        Uri uri = Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_MESSAGE);
        Cursor cursorQuery = this.mResolver.query(uri, BluetoothMapContract.BT_MESSAGE_PROJECTION, "_ID = " + j, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    try {
                        if (bluetoothMapAppParams.getFractionRequest() != -1 && !cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.RECEPTION_STATE)).equalsIgnoreCase(BluetoothMapContract.RECEPTION_STATE_COMPLETE)) {
                            Log.w(TAG, "getEmailMessage - receptionState not COMPLETE -  Not Implemented!");
                        }
                        String string = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.FLAG_READ));
                        int i = 0;
                        if (string == null || !string.equalsIgnoreCase("1")) {
                            bluetoothMapbMessageEmail.setStatus(false);
                        } else {
                            bluetoothMapbMessageEmail.setStatus(true);
                        }
                        bluetoothMapbMessageEmail.setType(BluetoothMapUtils.TYPE.EMAIL);
                        bluetoothMapbMessageEmail.setVersionString(this.mMessageVersion);
                        bluetoothMapbMessageEmail.setCompleteFolder(bluetoothMapFolderElement.getFolderById(cursorQuery.getLong(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.FOLDER_ID))).getFullPath());
                        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.TO_LIST)));
                        if (rfc822TokenArr.length != 0) {
                            if (D) {
                                Log.d(TAG, "Recipient count= " + rfc822TokenArr.length);
                            }
                            int i2 = 0;
                            while (i2 < rfc822TokenArr.length) {
                                String[] strArr = new String[1];
                                strArr[i] = rfc822TokenArr[i2].getAddress();
                                String name = rfc822TokenArr[i2].getName();
                                bluetoothMapbMessageEmail.addRecipient(name, name, null, strArr, null, null);
                                i2++;
                                i = i;
                                rfc822TokenArr = rfc822TokenArr;
                            }
                        }
                        int i3 = i;
                        Rfc822Token[] rfc822TokenArr2 = Rfc822Tokenizer.tokenize(cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.FROM_LIST)));
                        if (rfc822TokenArr2.length != 0) {
                            if (D) {
                                Log.d(TAG, "Originator count= " + rfc822TokenArr2.length);
                            }
                            int i4 = i3;
                            while (i4 < rfc822TokenArr2.length) {
                                String[] strArr2 = new String[1];
                                strArr2[i3] = rfc822TokenArr2[i4].getAddress();
                                String name2 = rfc822TokenArr2[i4].getName();
                                int i5 = i4;
                                Rfc822Token[] rfc822TokenArr3 = rfc822TokenArr2;
                                bluetoothMapbMessageEmail.addOriginator(name2, name2, null, strArr2, null, null);
                                i4 = i5 + 1;
                                rfc822TokenArr2 = rfc822TokenArr3;
                            }
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        ?? OpenFileDescriptor = Uri.parse(uri + "/" + j + (bluetoothMapAppParams.getAttachment() == 0 ? "/NO_ATTACHMENTS" : ""));
                        ?? r2 = 0;
                        r2 = 0;
                        r2 = 0;
                        r2 = 0;
                        r2 = 0;
                        r2 = 0;
                        r2 = 0;
                        ?? r22 = 0;
                        r2 = 0;
                        try {
                            try {
                                try {
                                    OpenFileDescriptor = this.mResolver.openFileDescriptor(OpenFileDescriptor, "r");
                                    try {
                                        fileInputStream = new FileInputStream(OpenFileDescriptor.getFileDescriptor());
                                    } catch (FileNotFoundException e) {
                                        e = e;
                                    } catch (IOException e2) {
                                        e = e2;
                                    } catch (NullPointerException e3) {
                                        e = e3;
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                                try {
                                    sb = new StringBuilder("");
                                    r2 = new byte[1024];
                                } catch (FileNotFoundException e4) {
                                    e = e4;
                                    r2 = fileInputStream;
                                    Log.w(TAG, e);
                                    if (r2 != 0) {
                                        try {
                                            r2.close();
                                        } catch (IOException e5) {
                                        }
                                    }
                                    if (OpenFileDescriptor != 0) {
                                        OpenFileDescriptor.close();
                                        OpenFileDescriptor = OpenFileDescriptor;
                                        r2 = r2;
                                    }
                                    byte[] bArrEncode = bluetoothMapbMessageEmail.encode();
                                    if (cursorQuery != null) {
                                    }
                                    return bArrEncode;
                                } catch (IOException e6) {
                                    e = e6;
                                    r2 = fileInputStream;
                                    Log.w(TAG, e);
                                    if (r2 != 0) {
                                        try {
                                            r2.close();
                                        } catch (IOException e7) {
                                        }
                                    }
                                    if (OpenFileDescriptor != 0) {
                                        OpenFileDescriptor.close();
                                        OpenFileDescriptor = OpenFileDescriptor;
                                        r2 = r2;
                                    }
                                    byte[] bArrEncode2 = bluetoothMapbMessageEmail.encode();
                                    if (cursorQuery != null) {
                                    }
                                    return bArrEncode2;
                                } catch (NullPointerException e8) {
                                    e = e8;
                                    r2 = fileInputStream;
                                    Log.w(TAG, e);
                                    if (r2 != 0) {
                                        try {
                                            r2.close();
                                        } catch (IOException e9) {
                                        }
                                    }
                                    if (OpenFileDescriptor != 0) {
                                        OpenFileDescriptor.close();
                                        OpenFileDescriptor = OpenFileDescriptor;
                                        r2 = r2;
                                    }
                                    byte[] bArrEncode22 = bluetoothMapbMessageEmail.encode();
                                    if (cursorQuery != null) {
                                    }
                                    return bArrEncode22;
                                } catch (Throwable th3) {
                                    th = th3;
                                    r2 = fileInputStream;
                                    r3 = OpenFileDescriptor;
                                    th = th;
                                    r22 = r2;
                                    if (r22 != 0) {
                                        try {
                                            r22.close();
                                        } catch (IOException e10) {
                                        }
                                    }
                                    if (r3 != 0) {
                                        throw th;
                                    }
                                    try {
                                        r3.close();
                                        throw th;
                                    } catch (IOException e11) {
                                        throw th;
                                    }
                                }
                            } catch (FileNotFoundException e12) {
                                e = e12;
                                OpenFileDescriptor = 0;
                            } catch (IOException e13) {
                                e = e13;
                                OpenFileDescriptor = 0;
                            } catch (NullPointerException e14) {
                                e = e14;
                                OpenFileDescriptor = 0;
                            } catch (Throwable th4) {
                                th = th4;
                                r3 = 0;
                                if (r22 != 0) {
                                }
                                if (r3 != 0) {
                                }
                            }
                        } catch (IOException e15) {
                        }
                        while (true) {
                            int i6 = fileInputStream.read(r2);
                            if (i6 == -1) {
                                break;
                            }
                            sb.append(new String((byte[]) r2, i3, i6));
                            byte[] bArrEncode222 = bluetoothMapbMessageEmail.encode();
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return bArrEncode222;
                        }
                        bluetoothMapbMessageEmail.setEmailBody(sb.toString());
                        fileInputStream.close();
                        if (OpenFileDescriptor != 0) {
                            OpenFileDescriptor.close();
                            OpenFileDescriptor = OpenFileDescriptor;
                            r2 = r2;
                        }
                        byte[] bArrEncode2222 = bluetoothMapbMessageEmail.encode();
                        if (cursorQuery != null) {
                        }
                        return bArrEncode2222;
                    } finally {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    }
                }
            } catch (Throwable th5) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                throw th5;
            }
        }
        throw new IllegalArgumentException("EMAIL handle not found");
    }

    public byte[] getIMMessage(long j, BluetoothMapAppParams bluetoothMapAppParams, BluetoothMapFolderElement bluetoothMapFolderElement) throws Throwable {
        if (bluetoothMapAppParams.getCharset() == 0) {
            throw new IllegalArgumentException("IM charset native not allowed for IM - must be utf-8");
        }
        BluetoothMapbMessageMime bluetoothMapbMessageMime = new BluetoothMapbMessageMime();
        Uri uri = Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_MESSAGE);
        Cursor cursorQuery = this.mResolver.query(uri, BluetoothMapContract.BT_MESSAGE_PROJECTION, "_ID = " + j, null, null);
        if (cursorQuery != null) {
            Cursor cursor = null;
            try {
                if (cursorQuery.moveToFirst()) {
                    bluetoothMapbMessageMime.setType(BluetoothMapUtils.TYPE.IM);
                    bluetoothMapbMessageMime.setVersionString(this.mMessageVersion);
                    if (cursorQuery.getInt(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.FLAG_READ)) == 1) {
                        bluetoothMapbMessageMime.setStatus(true);
                    } else {
                        bluetoothMapbMessageMime.setStatus(false);
                    }
                    long j2 = cursorQuery.getInt(cursorQuery.getColumnIndex("thread_id"));
                    long j3 = cursorQuery.getLong(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.FOLDER_ID));
                    bluetoothMapbMessageMime.setCompleteFolder(bluetoothMapFolderElement.getFolderById(j3).getFullPath());
                    bluetoothMapbMessageMime.setSubject(cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.SUBJECT)));
                    bluetoothMapbMessageMime.setMessageId(cursorQuery.getString(cursorQuery.getColumnIndex("_id")));
                    bluetoothMapbMessageMime.setDate(cursorQuery.getLong(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.DATE)));
                    bluetoothMapbMessageMime.setTextOnly(cursorQuery.getInt(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.ATTACHMENT_SIZE)) == 0);
                    bluetoothMapbMessageMime.setIncludeAttachments(bluetoothMapAppParams.getAttachment() != 0);
                    BluetoothMapbMessageMime.MimePart mimePartAddMimePart = bluetoothMapbMessageMime.addMimePart();
                    mimePartAddMimePart.mData = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothMapContract.MessageColumns.BODY)).getBytes("UTF-8");
                    mimePartAddMimePart.mCharsetName = "utf-8";
                    mimePartAddMimePart.mContentId = "0";
                    mimePartAddMimePart.mContentType = "text/plain";
                    bluetoothMapbMessageMime.updateCharset();
                    Uri uri2 = Uri.parse(this.mBaseUri + BluetoothMapContract.TABLE_CONVOCONTACT);
                    Cursor cursorQuery2 = this.mResolver.query(uri2, BluetoothMapContract.BT_CONTACT_PROJECTION, "convo_id = " + j2, null, null);
                    if (cursorQuery2 != null) {
                        try {
                            if (cursorQuery2.moveToFirst()) {
                                String string = cursorQuery2.getString(cursorQuery2.getColumnIndex("name"));
                                String[] strArr = {cursorQuery2.getString(cursorQuery2.getColumnIndex(BluetoothMapContract.ConvoContactColumns.X_BT_UID))};
                                String string2 = cursorQuery2.getString(cursorQuery2.getColumnIndex(BluetoothMapContract.ConvoContactColumns.NICKNAME));
                                String[] strArr2 = {this.mAccount.getUciFull()};
                                String[] strArr3 = {cursorQuery2.getString(cursorQuery2.getColumnIndex(BluetoothMapContract.ConvoContactColumns.UCI))};
                                if (j3 == 2 || j3 == 4) {
                                    bluetoothMapbMessageMime.addRecipient(string2, string, null, null, strArr, strArr3);
                                    bluetoothMapbMessageMime.addOriginator(null, strArr2);
                                } else {
                                    bluetoothMapbMessageMime.addOriginator(string2, string, null, null, strArr, strArr3);
                                    bluetoothMapbMessageMime.addRecipient(null, strArr2);
                                }
                            }
                        } catch (Throwable th) {
                            th = th;
                            cursor = cursorQuery2;
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            if (cursor != null) {
                                cursor.close();
                            }
                            throw th;
                        }
                    }
                    byte[] bArrEncode = bluetoothMapbMessageMime.encode();
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    if (cursorQuery2 != null) {
                        cursorQuery2.close();
                    }
                    return bArrEncode;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        throw new IllegalArgumentException("IM handle not found");
    }

    public void setRemoteFeatureMask(int i) {
        this.mRemoteFeatureMask = i;
        if ((this.mRemoteFeatureMask & 512) == 512) {
            this.mMsgListingVersion = 11;
        }
    }

    public int getRemoteFeatureMask() {
        return this.mRemoteFeatureMask;
    }

    HashMap<Long, BluetoothMapConvoListingElement> getSmsMmsConvoList() {
        return this.mMasInstance.getSmsMmsConvoList();
    }

    void setSmsMmsConvoList(HashMap<Long, BluetoothMapConvoListingElement> map) {
        this.mMasInstance.setSmsMmsConvoList(map);
    }

    HashMap<Long, BluetoothMapConvoListingElement> getImEmailConvoList() {
        return this.mMasInstance.getImEmailConvoList();
    }

    void setImEmailConvoList(HashMap<Long, BluetoothMapConvoListingElement> map) {
        this.mMasInstance.setImEmailConvoList(map);
    }

    public long getThreadIdByNumber(String str) {
        long j = -1;
        if (TextUtils.isEmpty(str)) {
            return -1L;
        }
        Log.d(TAG, "[getThreadId]: number = " + str);
        Cursor cursorQuery = this.mResolver.query(MAP_CONTENT_URI, THREADID_PROJECTION, str, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() == 1 && cursorQuery.moveToFirst()) {
                    j = cursorQuery.getLong(cursorQuery.getColumnIndex("_id"));
                }
            } finally {
                cursorQuery.close();
            }
        }
        return j;
    }

    public long getSubIdByThread(long j) {
        Log.d(TAG, "[getSubIdByThread]: threadId = " + j);
        Cursor cursorQuery = this.mResolver.query(Uri.withAppendedPath(MMSSMS_CONTENT_URI, String.valueOf(j)), SIMID_PROJECTION, null, null, null);
        long j2 = -1;
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToLast()) {
                    String string = cursorQuery.getString(cursorQuery.getColumnIndex("transport_type"));
                    if (string.equals("sms") || string.equals("mms")) {
                        j2 = cursorQuery.getLong(cursorQuery.getColumnIndex("sub_id"));
                    }
                }
            } finally {
                cursorQuery.close();
            }
        }
        return j2;
    }
}
