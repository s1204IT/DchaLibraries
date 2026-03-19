package com.android.bluetooth.map;

import android.util.Log;
import com.android.bluetooth.SignedLongLong;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class BluetoothMapAppParams {
    private static final int ATTACHMENT = 10;
    private static final int ATTACHMENT_LEN = 1;
    private static final int CHARSET = 20;
    private static final int CHARSET_LEN = 1;
    public static final int CHARSET_NATIVE = 0;
    public static final int CHARSET_UTF8 = 1;
    private static final int CHAT_STATE = 31;
    private static final int CHAT_STATE_CONVO_ID = 36;
    private static final int CHAT_STATE_CONVO_ID_LEN = 16;
    private static final int CHAT_STATE_LEN = 1;
    private static final int CONVO_LISTING_SIZE = 33;
    private static final int CONVO_LISTING_SIZE_LEN = 2;
    private static final int CONVO_LIST_VER_COUNTER = 27;
    private static final int CONVO_LIST_VER_COUNTER_LEN = 16;
    private static final int CONVO_PARAMETER_MASK = 40;
    private static final int CONVO_PARAMETER_MASK_LEN = 4;
    private static final int DATABASE_INDETIFIER = 26;
    private static final int DATABASE_INDETIFIER_LEN = 16;
    private static final int FILTER_CONVO_ID = 32;
    private static final int FILTER_CONVO_ID_LEN = 32;
    private static final int FILTER_MESSAGE_HANDLE = 38;
    private static final int FILTER_MESSAGE_HANDLE_LEN = 16;
    private static final int FILTER_MESSAGE_TYPE = 3;
    private static final int FILTER_MESSAGE_TYPE_LEN = 1;
    public static final int FILTER_MSG_TYPE_MASK = 31;
    public static final int FILTER_NO_EMAIL = 4;
    public static final int FILTER_NO_IM = 16;
    public static final int FILTER_NO_MMS = 8;
    public static final int FILTER_NO_SMS_CDMA = 2;
    public static final int FILTER_NO_SMS_GSM = 1;
    private static final int FILTER_ORIGINATOR = 8;
    private static final int FILTER_PERIOD_BEGIN = 4;
    private static final int FILTER_PERIOD_END = 5;
    private static final int FILTER_PRESENCE = 34;
    private static final int FILTER_PRESENCE_LEN = 1;
    private static final int FILTER_PRIORITY = 9;
    private static final int FILTER_PRIORITY_LEN = 1;
    private static final int FILTER_READ_STATUS = 6;
    private static final int FILTER_READ_STATUS_LEN = 1;
    private static final int FILTER_RECIPIENT = 7;
    private static final int FILTER_UID_PRESENT = 35;
    private static final int FILTER_UID_PRESENT_LEN = 1;
    private static final int FOLDER_LISTING_SIZE = 17;
    private static final int FOLDER_LISTING_SIZE_LEN = 2;
    private static final int FOLDER_VER_COUNTER = 37;
    private static final int FOLDER_VER_COUNTER_LEN = 16;
    private static final int FRACTION_DELIVER = 22;
    public static final int FRACTION_DELIVER_LAST = 1;
    private static final int FRACTION_DELIVER_LEN = 1;
    public static final int FRACTION_DELIVER_MORE = 0;
    private static final int FRACTION_REQUEST = 21;
    public static final int FRACTION_REQUEST_FIRST = 0;
    private static final int FRACTION_REQUEST_LEN = 1;
    public static final int FRACTION_REQUEST_NEXT = 1;
    public static final int INVALID_VALUE_PARAMETER = -1;
    private static final int LAST_ACTIVITY = 30;
    private static final int MAS_INSTANCE_ID = 15;
    private static final int MAS_INSTANCE_ID_LEN = 1;
    private static final int MAX_LIST_COUNT = 1;
    private static final int MAX_LIST_COUNT_LEN = 2;
    private static final int MESSAGE_LISTING_SIZE = 18;
    private static final int MESSAGE_LISTING_SIZE_LEN = 2;
    private static final int MSE_TIME = 25;
    private static final int NEW_MESSAGE = 13;
    private static final int NEW_MESSAGE_LEN = 1;
    private static final int NOTIFICATION_FILTER = 39;
    private static final int NOTIFICATION_FILTER_LEN = 4;
    private static final int NOTIFICATION_STATUS = 14;
    private static final int NOTIFICATION_STATUS_LEN = 1;
    public static final int NOTIFICATION_STATUS_NO = 0;
    public static final int NOTIFICATION_STATUS_YES = 1;
    private static final int PARAMETER_MASK = 16;
    private static final int PARAMETER_MASK_LEN = 4;
    private static final int PRESENCE_AVAILABLE = 28;
    private static final int PRESENCE_AVAILABLE_LEN = 1;
    private static final int PRESENCE_TEXT = 29;
    private static final int RETRY = 12;
    private static final int RETRY_LEN = 1;
    private static final int START_OFFSET = 2;
    private static final int START_OFFSET_LEN = 2;
    private static final int STATUS_INDICATOR = 23;
    public static final int STATUS_INDICATOR_DELETED = 1;
    private static final int STATUS_INDICATOR_LEN = 1;
    public static final int STATUS_INDICATOR_READ = 0;
    private static final int STATUS_VALUE = 24;
    private static final int STATUS_VALUE_LEN = 1;
    public static final int STATUS_VALUE_NO = 0;
    public static final int STATUS_VALUE_YES = 1;
    private static final int SUBJECT_LENGTH = 19;
    private static final int SUBJECT_LENGTH_LEN = 1;
    private static final String TAG = "BluetoothMapAppParams";
    private static final int TRANSPARENT = 11;
    private static final int TRANSPARENT_LEN = 1;
    private int mMaxListCount = -1;
    private int mStartOffset = -1;
    private int mFilterMessageType = -1;
    private long mFilterPeriodBegin = -1;
    private long mFilterPeriodEnd = -1;
    private int mFilterReadStatus = -1;
    private String mFilterRecipient = null;
    private String mFilterOriginator = null;
    private int mFilterPriority = -1;
    private int mAttachment = -1;
    private int mTransparent = -1;
    private int mRetry = -1;
    private int mNewMessage = -1;
    private int mNotificationStatus = -1;
    private long mNotificationFilter = -1;
    private int mMasInstanceId = -1;
    private long mParameterMask = -1;
    private int mFolderListingSize = -1;
    private int mMessageListingSize = -1;
    private int mConvoListingSize = -1;
    private int mSubjectLength = -1;
    private int mCharset = -1;
    private int mFractionRequest = -1;
    private int mFractionDeliver = -1;
    private int mStatusIndicator = -1;
    private int mStatusValue = -1;
    private long mMseTime = -1;
    private long mConvoListingVerCounterLow = -1;
    private long mConvoListingVerCounterHigh = -1;
    private long mDatabaseIdentifierLow = -1;
    private long mDatabaseIdentifierHigh = -1;
    private long mFolderVerCounterLow = -1;
    private long mFolderVerCounterHigh = -1;
    private int mPresenceAvailability = -1;
    private String mPresenceStatus = null;
    private long mLastActivity = -1;
    private int mChatState = -1;
    private SignedLongLong mFilterConvoId = null;
    private int mFilterPresence = -1;
    private int mFilterUidPresent = -1;
    private SignedLongLong mChatStateConvoId = null;
    private long mFilterMsgHandle = -1;
    private long mConvoParameterMask = -1;

    public BluetoothMapAppParams() {
    }

    public BluetoothMapAppParams(byte[] bArr) throws ParseException, IllegalArgumentException {
        parseParams(bArr);
    }

    private void parseParams(byte[] bArr) throws ParseException, IllegalArgumentException {
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        byteBufferWrap.order(ByteOrder.BIG_ENDIAN);
        int i = 0;
        while (i < bArr.length) {
            int i2 = i + 1;
            int i3 = bArr[i] & 255;
            int i4 = i2 + 1;
            int i5 = bArr[i2] & 255;
            switch (i3) {
                case 1:
                    if (i5 != 2) {
                        Log.w(TAG, "MAX_LIST_COUNT: Wrong length received: " + i5 + " expected: 2");
                    } else {
                        setMaxListCount(byteBufferWrap.getShort(i4) & 65535);
                    }
                    break;
                case 2:
                    if (i5 != 2) {
                        Log.w(TAG, "START_OFFSET: Wrong length received: " + i5 + " expected: 2");
                    } else {
                        setStartOffset(byteBufferWrap.getShort(i4) & 65535);
                    }
                    break;
                case 3:
                    if (i5 != 1) {
                        Log.w(TAG, "FILTER_MESSAGE_TYPE: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setFilterMessageType(bArr[i4] & 31);
                    }
                    break;
                case 4:
                    if (i5 != 0) {
                        setFilterPeriodBegin(new String(bArr, i4, i5));
                    } else {
                        Log.w(TAG, "FILTER_PERIOD_BEGIN: Wrong length received: " + i5 + " expected to be more than 0");
                    }
                    break;
                case 5:
                    if (i5 != 0) {
                        setFilterPeriodEnd(new String(bArr, i4, i5));
                    } else {
                        Log.w(TAG, "FILTER_PERIOD_END: Wrong length received: " + i5 + " expected to be more than 0");
                    }
                    break;
                case 6:
                    if (i5 != 1) {
                        Log.w(TAG, "FILTER_READ_STATUS: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setFilterReadStatus(bArr[i4] & 3);
                    }
                    break;
                case 7:
                    if (i5 != 0) {
                        setFilterRecipient(new String(bArr, i4, i5));
                    } else {
                        Log.w(TAG, "FILTER_RECIPIENT: Wrong length received: " + i5 + " expected to be more than 0");
                    }
                    break;
                case 8:
                    if (i5 != 0) {
                        setFilterOriginator(new String(bArr, i4, i5));
                    } else {
                        Log.w(TAG, "FILTER_ORIGINATOR: Wrong length received: " + i5 + " expected to be more than 0");
                    }
                    break;
                case 9:
                    if (i5 != 1) {
                        Log.w(TAG, "FILTER_PRIORITY: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setFilterPriority(bArr[i4] & 3);
                    }
                    break;
                case 10:
                    if (i5 != 1) {
                        Log.w(TAG, "ATTACHMENT: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setAttachment(bArr[i4] & 1);
                    }
                    break;
                case 11:
                    if (i5 != 1) {
                        Log.w(TAG, "TRANSPARENT: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setTransparent(bArr[i4] & 1);
                    }
                    break;
                case 12:
                    if (i5 != 1) {
                        Log.w(TAG, "RETRY: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setRetry(bArr[i4] & 1);
                    }
                    break;
                case 13:
                    if (i5 != 1) {
                        Log.w(TAG, "NEW_MESSAGE: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setNewMessage(bArr[i4] & 1);
                    }
                    break;
                case 14:
                    if (i5 != 1) {
                        Log.w(TAG, "NOTIFICATION_STATUS: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setNotificationStatus(bArr[i4] & 1);
                    }
                    break;
                case 15:
                    if (i5 != 1) {
                        Log.w(TAG, "MAS_INSTANCE_ID: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setMasInstanceId(bArr[i4] & 255);
                    }
                    break;
                case 16:
                    if (i5 == 4) {
                        setParameterMask(4294967295L & ((long) byteBufferWrap.getInt(i4)));
                    } else {
                        Log.w(TAG, "PARAMETER_MASK: Wrong length received: " + i5 + " expected: 4");
                    }
                    break;
                case 17:
                    if (i5 != 2) {
                        Log.w(TAG, "FOLDER_LISTING_SIZE: Wrong length received: " + i5 + " expected: 2");
                    } else {
                        setFolderListingSize(byteBufferWrap.getShort(i4) & 65535);
                    }
                    break;
                case 18:
                    if (i5 != 2) {
                        Log.w(TAG, "MESSAGE_LISTING_SIZE: Wrong length received: " + i5 + " expected: 2");
                    } else {
                        setMessageListingSize(byteBufferWrap.getShort(i4) & 65535);
                    }
                    break;
                case 19:
                    if (i5 != 1) {
                        Log.w(TAG, "SUBJECT_LENGTH: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setSubjectLength(bArr[i4] & 255);
                    }
                    break;
                case 20:
                    if (i5 != 1) {
                        Log.w(TAG, "CHARSET: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setCharset(bArr[i4] & 1);
                    }
                    break;
                case 21:
                    if (i5 != 1) {
                        Log.w(TAG, "FRACTION_REQUEST: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setFractionRequest(bArr[i4] & 1);
                    }
                    break;
                case 22:
                    if (i5 != 1) {
                        Log.w(TAG, "FRACTION_DELIVER: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setFractionDeliver(bArr[i4] & 1);
                    }
                    break;
                case 23:
                    if (i5 != 1) {
                        Log.w(TAG, "STATUS_INDICATOR: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setStatusIndicator(bArr[i4] & 1);
                    }
                    break;
                case 24:
                    if (i5 != 1) {
                        Log.w(TAG, "STATUS_VALUER: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setStatusValue(bArr[i4] & 1);
                    }
                    break;
                case 25:
                    setMseTime(new String(bArr, i4, i5));
                    break;
                case 26:
                    if (i5 != 16) {
                        Log.w(TAG, "DATABASE_IDENTIFIER: Wrong length received: " + i5 + " expected: 16");
                    } else {
                        setDatabaseIdentifier(byteBufferWrap.getLong(i4), byteBufferWrap.getLong(i4 + 8));
                    }
                    break;
                case 27:
                    if (i5 != 16) {
                        Log.w(TAG, "CONVO_LIST_VER_COUNTER: Wrong length received: " + i5 + " expected: 16");
                    } else {
                        setConvoListingVerCounter(byteBufferWrap.getLong(i4), byteBufferWrap.getLong(i4 + 8));
                    }
                    break;
                case 28:
                    if (i5 != 1) {
                        Log.w(TAG, "PRESENCE_AVAILABLE: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setPresenceAvailability(bArr[i4]);
                    }
                    break;
                case 29:
                    if (i5 != 0) {
                        setPresenceStatus(new String(bArr, i4, i5));
                    } else {
                        Log.w(TAG, "PRESENCE_STATUS: Wrong length received: " + i5 + " expected to be more than 0");
                    }
                    break;
                case 30:
                    if (i5 != 0) {
                        setLastActivity(new String(bArr, i4, i5));
                    } else {
                        Log.w(TAG, "LAST_ACTIVITY: Wrong length received: " + i5 + " expected to be more than 0");
                    }
                    break;
                case 31:
                    if (i5 != 1) {
                        Log.w(TAG, "CHAT_STATE: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setChatState(bArr[i4]);
                    }
                    break;
                case 32:
                    if (i5 != 0 && i5 <= 32) {
                        setFilterConvoId(new String(bArr, i4, i5));
                    } else {
                        Log.w(TAG, "FILTER_CONVO_ID: Wrong length received: " + i5 + " expected: 32");
                    }
                    break;
                case 33:
                    if (i5 != 2) {
                        Log.w(TAG, "LISTING_SIZE: Wrong length received: " + i5 + " expected: 2");
                    } else {
                        setConvoListingSize(byteBufferWrap.getShort(i4) & 65535);
                    }
                    break;
                case 34:
                    if (i5 != 1) {
                        Log.w(TAG, "FILTER_PRESENCE: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setFilterPresence(bArr[i4]);
                    }
                    break;
                case 35:
                    if (i5 != 1) {
                        Log.w(TAG, "FILTER_UID_PRESENT: Wrong length received: " + i5 + " expected: 1");
                    } else {
                        setFilterUidPresent(bArr[i4] & 1);
                    }
                    break;
                case 36:
                    if (i5 != 16) {
                        Log.w(TAG, "CHAT_STATE_CONVO_ID: Wrong length received: " + i5 + " expected: 16");
                    } else {
                        int i6 = i4 + 8;
                        setChatStateConvoId(byteBufferWrap.getLong(i4), byteBufferWrap.getLong(i6));
                        Log.d(TAG, "CHAT_STATE_CONVO_ID: convo id MSB=" + BluetoothMapUtils.getLongAsString(byteBufferWrap.getLong(i4)) + ", LSB(+8)=" + BluetoothMapUtils.getLongAsString(byteBufferWrap.getLong(i6)));
                    }
                    break;
                case 37:
                    break;
                case 38:
                    if (i5 != 0 && i5 <= 16) {
                        setFilterMsgHandle(new String(bArr, i4, i5));
                    } else {
                        Log.w(TAG, "FILTER_MESSAGE_HANDLE: Wrong length received: " + i5 + " expected: 16");
                    }
                    break;
                case 39:
                    if (i5 == 4) {
                        setNotificationFilter(4294967295L & ((long) byteBufferWrap.getInt(i4)));
                    } else {
                        Log.w(TAG, "NOTIFICATION_FILTER: Wrong length received: " + i5 + " expected: 4");
                    }
                    break;
                case 40:
                    if (i5 != 4) {
                        Log.w(TAG, "CONVO_PARAMETER_MASK: Wrong length received: " + i5 + " expected: 4");
                    } else {
                        setConvoParameterMask(4294967295L & ((long) byteBufferWrap.getInt(i4)));
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown TagId received ( 0x" + Integer.toString(i3, 16) + "), skipping...");
                    break;
            }
            i = i4 + i5;
        }
    }

    private int getParamMaxLength() throws UnsupportedEncodingException {
        int length = 173 + (getFilterPeriodBegin() == -1 ? 0 : 20) + (getFilterPeriodEnd() == -1 ? 0 : 20);
        if (getFilterRecipient() != null) {
            length += getFilterRecipient().getBytes("UTF-8").length;
        }
        if (getFilterOriginator() != null) {
            length += getFilterOriginator().getBytes("UTF-8").length;
        }
        int length2 = length + (getMseTime() == -1 ? 0 : 20);
        if (getPresenceStatus() != null) {
            length2 += getPresenceStatus().getBytes("UTF-8").length;
        }
        return length2 + (getLastActivity() == -1 ? 0 : 20);
    }

    public byte[] encodeParams() throws UnsupportedEncodingException {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(getParamMaxLength());
        byteBufferAllocate.order(ByteOrder.BIG_ENDIAN);
        if (getMaxListCount() != -1) {
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) 2);
            byteBufferAllocate.putShort((short) getMaxListCount());
        }
        if (getStartOffset() != -1) {
            byteBufferAllocate.put((byte) 2);
            byteBufferAllocate.put((byte) 2);
            byteBufferAllocate.putShort((short) getStartOffset());
        }
        if (getFilterMessageType() != -1) {
            byteBufferAllocate.put((byte) 3);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getFilterMessageType());
        }
        if (getFilterPeriodBegin() != -1) {
            byteBufferAllocate.put((byte) 4);
            byteBufferAllocate.put((byte) getFilterPeriodBeginString().getBytes("UTF-8").length);
            byteBufferAllocate.put(getFilterPeriodBeginString().getBytes("UTF-8"));
        }
        if (getFilterPeriodEnd() != -1) {
            byteBufferAllocate.put((byte) 5);
            byteBufferAllocate.put((byte) getFilterPeriodEndString().getBytes("UTF-8").length);
            byteBufferAllocate.put(getFilterPeriodEndString().getBytes("UTF-8"));
        }
        if (getFilterReadStatus() != -1) {
            byteBufferAllocate.put((byte) 6);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getFilterReadStatus());
        }
        if (getFilterRecipient() != null) {
            byteBufferAllocate.put((byte) 7);
            byteBufferAllocate.put((byte) getFilterRecipient().getBytes("UTF-8").length);
            byteBufferAllocate.put(getFilterRecipient().getBytes("UTF-8"));
        }
        if (getFilterOriginator() != null) {
            byteBufferAllocate.put((byte) 8);
            byteBufferAllocate.put((byte) getFilterOriginator().getBytes("UTF-8").length);
            byteBufferAllocate.put(getFilterOriginator().getBytes("UTF-8"));
        }
        if (getFilterPriority() != -1) {
            byteBufferAllocate.put((byte) 9);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getFilterPriority());
        }
        if (getAttachment() != -1) {
            byteBufferAllocate.put((byte) 10);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getAttachment());
        }
        if (getTransparent() != -1) {
            byteBufferAllocate.put((byte) 11);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getTransparent());
        }
        if (getRetry() != -1) {
            byteBufferAllocate.put((byte) 12);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getRetry());
        }
        if (getNewMessage() != -1) {
            byteBufferAllocate.put((byte) 13);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getNewMessage());
        }
        if (getNotificationStatus() != -1) {
            byteBufferAllocate.put((byte) 14);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.putShort((short) getNotificationStatus());
        }
        if (getNotificationFilter() != -1) {
            byteBufferAllocate.put((byte) 39);
            byteBufferAllocate.put((byte) 4);
            byteBufferAllocate.putInt((int) getNotificationFilter());
        }
        if (getMasInstanceId() != -1) {
            byteBufferAllocate.put((byte) 15);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getMasInstanceId());
        }
        if (getParameterMask() != -1) {
            byteBufferAllocate.put((byte) 16);
            byteBufferAllocate.put((byte) 4);
            byteBufferAllocate.putInt((int) getParameterMask());
        }
        if (getFolderListingSize() != -1) {
            byteBufferAllocate.put((byte) 17);
            byteBufferAllocate.put((byte) 2);
            byteBufferAllocate.putShort((short) getFolderListingSize());
        }
        if (getMessageListingSize() != -1) {
            byteBufferAllocate.put((byte) 18);
            byteBufferAllocate.put((byte) 2);
            byteBufferAllocate.putShort((short) getMessageListingSize());
        }
        if (getSubjectLength() != -1) {
            byteBufferAllocate.put((byte) 19);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getSubjectLength());
        }
        if (getCharset() != -1) {
            byteBufferAllocate.put((byte) 20);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getCharset());
        }
        if (getFractionRequest() != -1) {
            byteBufferAllocate.put((byte) 21);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getFractionRequest());
        }
        if (getFractionDeliver() != -1) {
            byteBufferAllocate.put((byte) 22);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getFractionDeliver());
        }
        if (getStatusIndicator() != -1) {
            byteBufferAllocate.put((byte) 23);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getStatusIndicator());
        }
        if (getStatusValue() != -1) {
            byteBufferAllocate.put((byte) 24);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.put((byte) getStatusValue());
        }
        if (getMseTime() != -1) {
            byteBufferAllocate.put((byte) 25);
            byteBufferAllocate.put((byte) getMseTimeString().getBytes("UTF-8").length);
            byteBufferAllocate.put(getMseTimeString().getBytes("UTF-8"));
        }
        if (getDatabaseIdentifier() != null) {
            byteBufferAllocate.put((byte) 26);
            byteBufferAllocate.put((byte) 16);
            byteBufferAllocate.put(getDatabaseIdentifier());
        }
        if (getConvoListingVerCounter() != null) {
            byteBufferAllocate.put((byte) 27);
            byteBufferAllocate.put((byte) 16);
            byteBufferAllocate.put(getConvoListingVerCounter());
        }
        if (getPresenceAvailability() != -1) {
            byteBufferAllocate.put((byte) 28);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.putInt(getPresenceAvailability());
        }
        if (getPresenceStatus() != null) {
            byteBufferAllocate.put((byte) 29);
            byteBufferAllocate.put((byte) getPresenceStatus().getBytes("UTF-8").length);
            byteBufferAllocate.put(getPresenceStatus().getBytes());
        }
        if (getLastActivity() != -1) {
            byteBufferAllocate.put((byte) 30);
            byteBufferAllocate.put((byte) getLastActivityString().getBytes("UTF-8").length);
            byteBufferAllocate.put(getLastActivityString().getBytes());
        }
        if (getChatState() != -1) {
            byteBufferAllocate.put((byte) 31);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.putShort((short) getChatState());
        }
        if (getFilterConvoId() != null) {
            byteBufferAllocate.put((byte) 32);
            byteBufferAllocate.put((byte) 32);
            byteBufferAllocate.putLong(getFilterConvoId().getMostSignificantBits());
            byteBufferAllocate.putLong(getFilterConvoId().getLeastSignificantBits());
        }
        if (getConvoListingSize() != -1) {
            byteBufferAllocate.put((byte) 33);
            byteBufferAllocate.put((byte) 2);
            byteBufferAllocate.putShort((short) getConvoListingSize());
        }
        if (getFilterPresence() != -1) {
            byteBufferAllocate.put((byte) 34);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.putShort((short) getFilterPresence());
        }
        if (getFilterUidPresent() != -1) {
            byteBufferAllocate.put((byte) 35);
            byteBufferAllocate.put((byte) 1);
            byteBufferAllocate.putShort((short) getFilterUidPresent());
        }
        if (getChatStateConvoId() != null) {
            byteBufferAllocate.put((byte) 36);
            byteBufferAllocate.put((byte) 16);
            byteBufferAllocate.putLong(getChatStateConvoId().getMostSignificantBits());
            byteBufferAllocate.putLong(getChatStateConvoId().getLeastSignificantBits());
        }
        if (getFolderVerCounter() != null) {
            byteBufferAllocate.put((byte) 37);
            byteBufferAllocate.put((byte) 16);
            byteBufferAllocate.put(getFolderVerCounter());
        }
        if (getFilterMsgHandle() != -1) {
            byteBufferAllocate.put((byte) 38);
            byteBufferAllocate.put((byte) 16);
            byteBufferAllocate.putLong(getFilterMsgHandle());
        }
        if (getConvoParameterMask() != -1) {
            byteBufferAllocate.put((byte) 40);
            byteBufferAllocate.put((byte) 4);
            byteBufferAllocate.putInt((int) getConvoParameterMask());
        }
        return Arrays.copyOfRange(byteBufferAllocate.array(), byteBufferAllocate.arrayOffset(), byteBufferAllocate.arrayOffset() + byteBufferAllocate.position());
    }

    public int getMaxListCount() {
        return this.mMaxListCount;
    }

    public void setMaxListCount(int i) throws IllegalArgumentException {
        if (i < 0 || i > 65535) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFF");
        }
        this.mMaxListCount = i;
    }

    public int getStartOffset() {
        return this.mStartOffset;
    }

    public void setStartOffset(int i) throws IllegalArgumentException {
        if (i < 0 || i > 65535) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFF");
        }
        this.mStartOffset = i;
    }

    public int getFilterMessageType() {
        return this.mFilterMessageType;
    }

    public void setFilterMessageType(int i) throws IllegalArgumentException {
        if (i < 0 || i > 31) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x001F");
        }
        this.mFilterMessageType = i;
    }

    public long getFilterPeriodBegin() {
        return this.mFilterPeriodBegin;
    }

    public String getFilterPeriodBeginString() {
        return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date(this.mFilterPeriodBegin));
    }

    public void setFilterPeriodBegin(long j) {
        this.mFilterPeriodBegin = j;
    }

    public void setFilterPeriodBegin(String str) throws ParseException {
        this.mFilterPeriodBegin = new SimpleDateFormat("yyyyMMdd'T'HHmmss").parse(str).getTime();
    }

    public long getFilterLastActivityBegin() {
        return this.mFilterPeriodBegin;
    }

    public String getFilterLastActivityBeginString() {
        return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date(this.mFilterPeriodBegin));
    }

    public void setFilterLastActivityBegin(long j) {
        this.mFilterPeriodBegin = j;
    }

    public void setFilterLastActivityBegin(String str) throws ParseException {
        this.mFilterPeriodBegin = new SimpleDateFormat("yyyyMMdd'T'HHmmss").parse(str).getTime();
    }

    public long getFilterPeriodEnd() {
        return this.mFilterPeriodEnd;
    }

    public long getFilterLastActivityEnd() {
        return this.mFilterPeriodEnd;
    }

    public String getFilterLastActivityEndString() {
        return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date(this.mFilterPeriodEnd));
    }

    public void setFilterLastActivityEnd(long j) {
        this.mFilterPeriodEnd = j;
    }

    public void setFilterPeriodEnd(String str) throws ParseException {
        this.mFilterPeriodEnd = new SimpleDateFormat("yyyyMMdd'T'HHmmss").parse(str).getTime();
    }

    public String getFilterPeriodEndString() {
        return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date(this.mFilterPeriodEnd));
    }

    public void setFilterPeriodEnd(long j) {
        this.mFilterPeriodEnd = j;
    }

    public void setFilterLastActivityEnd(String str) throws ParseException {
        this.mFilterPeriodEnd = new SimpleDateFormat("yyyyMMdd'T'HHmmss").parse(str).getTime();
    }

    public int getFilterReadStatus() {
        return this.mFilterReadStatus;
    }

    public void setFilterReadStatus(int i) throws IllegalArgumentException {
        if (i < 0 || i > 2) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0002");
        }
        this.mFilterReadStatus = i;
    }

    public String getFilterRecipient() {
        return this.mFilterRecipient;
    }

    public void setFilterRecipient(String str) {
        this.mFilterRecipient = str;
    }

    public String getFilterOriginator() {
        return this.mFilterOriginator;
    }

    public void setFilterOriginator(String str) {
        this.mFilterOriginator = str;
    }

    public int getFilterPriority() {
        return this.mFilterPriority;
    }

    public void setFilterPriority(int i) throws IllegalArgumentException {
        if (i < 0 || i > 2) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0002");
        }
        this.mFilterPriority = i;
    }

    public void setDatabaseIdentifier(long j, long j2) {
        this.mDatabaseIdentifierHigh = j;
        this.mDatabaseIdentifierLow = j2;
    }

    public byte[] getDatabaseIdentifier() {
        if (this.mDatabaseIdentifierLow != -1 && this.mDatabaseIdentifierHigh != -1) {
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
            byteBufferAllocate.putLong(this.mDatabaseIdentifierHigh);
            byteBufferAllocate.putLong(this.mDatabaseIdentifierLow);
            return byteBufferAllocate.array();
        }
        return null;
    }

    public void setConvoListingVerCounter(long j, long j2) {
        this.mConvoListingVerCounterHigh = j2;
        this.mConvoListingVerCounterLow = j;
    }

    public byte[] getConvoListingVerCounter() {
        if (this.mConvoListingVerCounterHigh != -1 && this.mConvoListingVerCounterLow != -1) {
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
            byteBufferAllocate.putLong(this.mConvoListingVerCounterHigh);
            byteBufferAllocate.putLong(this.mConvoListingVerCounterLow);
            return byteBufferAllocate.array();
        }
        return null;
    }

    public void setFolderVerCounter(long j, long j2) {
        this.mFolderVerCounterHigh = j2;
        this.mFolderVerCounterLow = j;
    }

    public byte[] getFolderVerCounter() {
        if (this.mFolderVerCounterHigh != -1 && this.mFolderVerCounterLow != -1) {
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
            byteBufferAllocate.putLong(this.mFolderVerCounterHigh);
            byteBufferAllocate.putLong(this.mFolderVerCounterLow);
            return byteBufferAllocate.array();
        }
        return null;
    }

    public SignedLongLong getChatStateConvoId() {
        return this.mChatStateConvoId;
    }

    public byte[] getChatStateConvoIdByteArray() {
        if (this.mChatStateConvoId != null) {
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
            byteBufferAllocate.putLong(this.mChatStateConvoId.getMostSignificantBits());
            byteBufferAllocate.putLong(this.mChatStateConvoId.getLeastSignificantBits());
            return byteBufferAllocate.array();
        }
        return null;
    }

    public String getChatStateConvoIdString() {
        return new String(getChatStateConvoIdByteArray());
    }

    public void setChatStateConvoId(long j, long j2) {
        this.mChatStateConvoId = new SignedLongLong(j2, j);
    }

    public void setFilterMsgHandle(String str) {
        try {
            this.mFilterMsgHandle = BluetoothMapUtils.getLongFromString(str);
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Error creating long from handle string", e);
        }
    }

    public long getFilterMsgHandle() {
        return this.mFilterMsgHandle;
    }

    public String getFilterMsgHandleString() {
        if (this.mFilterMsgHandle != -1) {
            return BluetoothMapUtils.getLongAsString(this.mFilterMsgHandle);
        }
        return null;
    }

    public int getFilterUidPresent() {
        return this.mFilterUidPresent;
    }

    public void setFilterUidPresent(int i) {
        if (i < 0 || i > 255) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x00FF");
        }
        this.mFilterUidPresent = i;
    }

    public int getFilterPresence() {
        return this.mFilterPresence;
    }

    public SignedLongLong getFilterConvoId() {
        return this.mFilterConvoId;
    }

    public String getFilterConvoIdString() {
        if (this.mFilterConvoId != null) {
            return BluetoothMapUtils.getLongAsString(this.mFilterConvoId.getLeastSignificantBits());
        }
        return null;
    }

    public void setFilterConvoId(String str) {
        try {
            this.mFilterConvoId = SignedLongLong.fromString(str);
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Error creating long from id string", e);
        }
    }

    public void setChatState(int i) {
        if (i < 0 || i > 255) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x00FF");
        }
        this.mChatState = i;
    }

    public int getChatState() {
        return this.mChatState;
    }

    public long getLastActivity() {
        return this.mLastActivity;
    }

    public String getLastActivityString() {
        return new SimpleDateFormat("yyyyMMdd'T'HHmmssZ").format(new Date(this.mLastActivity));
    }

    public void setLastActivity(long j) {
        this.mLastActivity = j;
    }

    public void setLastActivity(String str) throws ParseException {
        this.mLastActivity = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ").parse(str).getTime();
    }

    public void setPresenceStatus(String str) {
        this.mPresenceStatus = str;
    }

    public String getPresenceStatus() {
        return this.mPresenceStatus;
    }

    public void setFilterPresence(int i) {
        if (i < 0 || i > 65535) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFF");
        }
        this.mFilterPresence = i;
    }

    public void setPresenceAvailability(int i) {
        if (i < 0 || i > 255) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x00FF");
        }
        this.mPresenceAvailability = i;
    }

    public int getPresenceAvailability() {
        return this.mPresenceAvailability;
    }

    public int getSubjectLength() {
        return this.mSubjectLength;
    }

    public int getAttachment() {
        return this.mAttachment;
    }

    public void setAttachment(int i) throws IllegalArgumentException {
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        }
        this.mAttachment = i;
    }

    public int getTransparent() {
        return this.mTransparent;
    }

    public void setTransparent(int i) throws IllegalArgumentException {
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        }
        this.mTransparent = i;
    }

    public int getRetry() {
        return this.mRetry;
    }

    public void setRetry(int i) throws IllegalArgumentException {
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        }
        this.mRetry = i;
    }

    public int getNewMessage() {
        return this.mNewMessage;
    }

    public void setNewMessage(int i) throws IllegalArgumentException {
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        }
        this.mNewMessage = i;
    }

    public int getNotificationStatus() {
        return this.mNotificationStatus;
    }

    public void setNotificationStatus(int i) throws IllegalArgumentException {
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        }
        this.mNotificationStatus = i;
    }

    public long getNotificationFilter() {
        return this.mNotificationFilter;
    }

    public void setNotificationFilter(long j) throws IllegalArgumentException {
        if (j < 0 || j > 4294967295L) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFFFFFFL");
        }
        this.mNotificationFilter = j;
    }

    public int getMasInstanceId() {
        return this.mMasInstanceId;
    }

    public void setMasInstanceId(int i) {
        if (i < 0 || i > 255) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x00FF");
        }
        this.mMasInstanceId = i;
    }

    public long getParameterMask() {
        return this.mParameterMask;
    }

    public void setParameterMask(long j) {
        if (j < 0 || j > 4294967295L) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFFFFFF");
        }
        this.mParameterMask = j;
    }

    public void setConvoParameterMask(long j) {
        if (j < 0 || j > 4294967295L) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFFFFFF");
        }
        this.mConvoParameterMask = j;
    }

    public long getConvoParameterMask() {
        return this.mConvoParameterMask;
    }

    public int getFolderListingSize() {
        return this.mFolderListingSize;
    }

    public void setFolderListingSize(int i) {
        if (i < 0 || i > 65535) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFF");
        }
        this.mFolderListingSize = i;
    }

    public int getMessageListingSize() {
        return this.mMessageListingSize;
    }

    public void setMessageListingSize(int i) {
        if (i < 0 || i > 65535) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFF");
        }
        this.mMessageListingSize = i;
    }

    public int getConvoListingSize() {
        return this.mConvoListingSize;
    }

    public void setConvoListingSize(int i) {
        if (i < 0 || i > 65535) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFF");
        }
        this.mConvoListingSize = i;
    }

    public void setSubjectLength(int i) {
        if (i < 0 || i > 255) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x00FF");
        }
        this.mSubjectLength = i;
    }

    public int getCharset() {
        return this.mCharset;
    }

    public void setCharset(int i) {
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("Out of range: " + i + ", valid range is 0x0000 to 0x0001");
        }
        this.mCharset = i;
    }

    public int getFractionRequest() {
        return this.mFractionRequest;
    }

    public void setFractionRequest(int i) {
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        }
        this.mFractionRequest = i;
    }

    public int getFractionDeliver() {
        return this.mFractionDeliver;
    }

    public void setFractionDeliver(int i) {
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        }
        this.mFractionDeliver = i;
    }

    public int getStatusIndicator() {
        return this.mStatusIndicator;
    }

    public void setStatusIndicator(int i) {
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        }
        this.mStatusIndicator = i;
    }

    public int getStatusValue() {
        return this.mStatusValue;
    }

    public void setStatusValue(int i) {
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        }
        this.mStatusValue = i;
    }

    public long getMseTime() {
        return this.mMseTime;
    }

    public String getMseTimeString() {
        return new SimpleDateFormat("yyyyMMdd'T'HHmmssZ").format(new Date(getMseTime()));
    }

    public void setMseTime(long j) {
        this.mMseTime = j;
    }

    public void setMseTime(String str) throws ParseException {
        this.mMseTime = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ").parse(str).getTime();
    }
}
