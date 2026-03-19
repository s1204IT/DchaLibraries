package com.google.android.mms.pdu;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.drm.DrmManagerClient;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.gsm.SmsCbConstants;
import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MmsException;
import com.google.android.mms.util.DownloadDrmHelper;
import com.google.android.mms.util.DrmConvertSession;
import com.google.android.mms.util.PduCache;
import com.google.android.mms.util.PduCacheEntry;
import com.google.android.mms.util.SqliteWrapper;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PduPersister {

    static final boolean f38assertionsDisabled;
    private static final int[] ADDRESS_FIELDS;
    private static final HashMap<Integer, Integer> CHARSET_COLUMN_INDEX_MAP;
    private static final HashMap<Integer, String> CHARSET_COLUMN_NAME_MAP;
    private static final boolean DEBUG = false;
    private static final long DUMMY_THREAD_ID = Long.MAX_VALUE;
    private static final HashMap<Integer, Integer> ENCODED_STRING_COLUMN_INDEX_MAP;
    private static final HashMap<Integer, String> ENCODED_STRING_COLUMN_NAME_MAP;
    private static final boolean LOCAL_LOGV = false;
    private static final HashMap<Integer, Integer> LONG_COLUMN_INDEX_MAP;
    private static final HashMap<Integer, String> LONG_COLUMN_NAME_MAP;
    private static final HashMap<Uri, Integer> MESSAGE_BOX_MAP;
    private static final HashMap<Integer, Integer> OCTET_COLUMN_INDEX_MAP;
    private static final HashMap<Integer, String> OCTET_COLUMN_NAME_MAP;
    private static final int PART_COLUMN_CHARSET = 1;
    private static final int PART_COLUMN_CONTENT_DISPOSITION = 2;
    private static final int PART_COLUMN_CONTENT_ID = 3;
    private static final int PART_COLUMN_CONTENT_LOCATION = 4;
    private static final int PART_COLUMN_CONTENT_TYPE = 5;
    private static final int PART_COLUMN_FILENAME = 6;
    private static final int PART_COLUMN_ID = 0;
    private static final int PART_COLUMN_NAME = 7;
    private static final int PART_COLUMN_TEXT = 8;
    private static final String[] PART_PROJECTION;
    private static final PduCache PDU_CACHE_INSTANCE;
    private static final int PDU_COLUMN_CONTENT_CLASS = 11;
    private static final int PDU_COLUMN_CONTENT_LOCATION = 5;
    private static final int PDU_COLUMN_CONTENT_TYPE = 6;
    private static final int PDU_COLUMN_DATE = 21;
    private static final int PDU_COLUMN_DATE_SENT = 28;
    private static final int PDU_COLUMN_DELIVERY_REPORT = 12;
    private static final int PDU_COLUMN_DELIVERY_TIME = 22;
    private static final int PDU_COLUMN_EXPIRY = 23;
    private static final int PDU_COLUMN_ID = 0;
    private static final int PDU_COLUMN_MESSAGE_BOX = 1;
    private static final int PDU_COLUMN_MESSAGE_CLASS = 7;
    private static final int PDU_COLUMN_MESSAGE_ID = 8;
    private static final int PDU_COLUMN_MESSAGE_SIZE = 24;
    private static final int PDU_COLUMN_MESSAGE_TYPE = 13;
    private static final int PDU_COLUMN_MMS_VERSION = 14;
    private static final int PDU_COLUMN_PRIORITY = 15;
    private static final int PDU_COLUMN_READ = 27;
    private static final int PDU_COLUMN_READ_REPORT = 16;
    private static final int PDU_COLUMN_READ_STATUS = 17;
    private static final int PDU_COLUMN_REPORT_ALLOWED = 18;
    private static final int PDU_COLUMN_RESPONSE_TEXT = 9;
    private static final int PDU_COLUMN_RETRIEVE_STATUS = 19;
    private static final int PDU_COLUMN_RETRIEVE_TEXT = 3;
    private static final int PDU_COLUMN_RETRIEVE_TEXT_CHARSET = 26;
    private static final int PDU_COLUMN_STATUS = 20;
    private static final int PDU_COLUMN_SUBJECT = 4;
    private static final int PDU_COLUMN_SUBJECT_CHARSET = 25;
    private static final int PDU_COLUMN_THREAD_ID = 2;
    private static final int PDU_COLUMN_TRANSACTION_ID = 10;
    private static final String[] PDU_PROJECTION;
    public static final int PROC_STATUS_COMPLETED = 3;
    public static final int PROC_STATUS_PERMANENTLY_FAILURE = 2;
    public static final int PROC_STATUS_TRANSIENT_FAILURE = 1;
    private static final String TAG = "PduPersister";
    public static final String TEMPORARY_DRM_OBJECT_URI = "content://mms/9223372036854775807/part";
    private static final HashMap<Integer, Integer> TEXT_STRING_COLUMN_INDEX_MAP;
    private static final HashMap<Integer, String> TEXT_STRING_COLUMN_NAME_MAP;
    private static PduPersister sPersister;
    private boolean mBackupRestore = false;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final DrmManagerClient mDrmManagerClient;
    private final TelephonyManager mTelephonyManager;

    static {
        f38assertionsDisabled = !PduPersister.class.desiredAssertionStatus();
        ADDRESS_FIELDS = new int[]{129, 130, 137, 151};
        PDU_PROJECTION = new String[]{"_id", Telephony.BaseMmsColumns.MESSAGE_BOX, "thread_id", Telephony.BaseMmsColumns.RETRIEVE_TEXT, Telephony.BaseMmsColumns.SUBJECT, Telephony.BaseMmsColumns.CONTENT_LOCATION, Telephony.BaseMmsColumns.CONTENT_TYPE, Telephony.BaseMmsColumns.MESSAGE_CLASS, Telephony.BaseMmsColumns.MESSAGE_ID, Telephony.BaseMmsColumns.RESPONSE_TEXT, Telephony.BaseMmsColumns.TRANSACTION_ID, Telephony.BaseMmsColumns.CONTENT_CLASS, Telephony.BaseMmsColumns.DELIVERY_REPORT, Telephony.BaseMmsColumns.MESSAGE_TYPE, Telephony.BaseMmsColumns.MMS_VERSION, Telephony.BaseMmsColumns.PRIORITY, Telephony.BaseMmsColumns.READ_REPORT, Telephony.BaseMmsColumns.READ_STATUS, Telephony.BaseMmsColumns.REPORT_ALLOWED, Telephony.BaseMmsColumns.RETRIEVE_STATUS, Telephony.BaseMmsColumns.STATUS, "date", Telephony.BaseMmsColumns.DELIVERY_TIME, Telephony.BaseMmsColumns.EXPIRY, Telephony.BaseMmsColumns.MESSAGE_SIZE, Telephony.BaseMmsColumns.SUBJECT_CHARSET, Telephony.BaseMmsColumns.RETRIEVE_TEXT_CHARSET, "read", "date_sent"};
        PART_PROJECTION = new String[]{"_id", Telephony.Mms.Part.CHARSET, Telephony.Mms.Part.CONTENT_DISPOSITION, "cid", Telephony.Mms.Part.CONTENT_LOCATION, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part.FILENAME, "name", "text"};
        MESSAGE_BOX_MAP = new HashMap<>();
        MESSAGE_BOX_MAP.put(Telephony.Mms.Inbox.CONTENT_URI, 1);
        MESSAGE_BOX_MAP.put(Telephony.Mms.Sent.CONTENT_URI, 2);
        MESSAGE_BOX_MAP.put(Telephony.Mms.Draft.CONTENT_URI, 3);
        MESSAGE_BOX_MAP.put(Telephony.Mms.Outbox.CONTENT_URI, 4);
        CHARSET_COLUMN_INDEX_MAP = new HashMap<>();
        CHARSET_COLUMN_INDEX_MAP.put(150, 25);
        CHARSET_COLUMN_INDEX_MAP.put(154, 26);
        CHARSET_COLUMN_NAME_MAP = new HashMap<>();
        CHARSET_COLUMN_NAME_MAP.put(150, Telephony.BaseMmsColumns.SUBJECT_CHARSET);
        CHARSET_COLUMN_NAME_MAP.put(154, Telephony.BaseMmsColumns.RETRIEVE_TEXT_CHARSET);
        ENCODED_STRING_COLUMN_INDEX_MAP = new HashMap<>();
        ENCODED_STRING_COLUMN_INDEX_MAP.put(154, 3);
        ENCODED_STRING_COLUMN_INDEX_MAP.put(150, 4);
        ENCODED_STRING_COLUMN_NAME_MAP = new HashMap<>();
        ENCODED_STRING_COLUMN_NAME_MAP.put(154, Telephony.BaseMmsColumns.RETRIEVE_TEXT);
        ENCODED_STRING_COLUMN_NAME_MAP.put(150, Telephony.BaseMmsColumns.SUBJECT);
        TEXT_STRING_COLUMN_INDEX_MAP = new HashMap<>();
        TEXT_STRING_COLUMN_INDEX_MAP.put(131, 5);
        TEXT_STRING_COLUMN_INDEX_MAP.put(132, 6);
        TEXT_STRING_COLUMN_INDEX_MAP.put(138, 7);
        TEXT_STRING_COLUMN_INDEX_MAP.put(139, 8);
        TEXT_STRING_COLUMN_INDEX_MAP.put(147, 9);
        TEXT_STRING_COLUMN_INDEX_MAP.put(152, 10);
        TEXT_STRING_COLUMN_NAME_MAP = new HashMap<>();
        TEXT_STRING_COLUMN_NAME_MAP.put(131, Telephony.BaseMmsColumns.CONTENT_LOCATION);
        TEXT_STRING_COLUMN_NAME_MAP.put(132, Telephony.BaseMmsColumns.CONTENT_TYPE);
        TEXT_STRING_COLUMN_NAME_MAP.put(138, Telephony.BaseMmsColumns.MESSAGE_CLASS);
        TEXT_STRING_COLUMN_NAME_MAP.put(139, Telephony.BaseMmsColumns.MESSAGE_ID);
        TEXT_STRING_COLUMN_NAME_MAP.put(147, Telephony.BaseMmsColumns.RESPONSE_TEXT);
        TEXT_STRING_COLUMN_NAME_MAP.put(152, Telephony.BaseMmsColumns.TRANSACTION_ID);
        OCTET_COLUMN_INDEX_MAP = new HashMap<>();
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(PduHeaders.CONTENT_CLASS), 11);
        OCTET_COLUMN_INDEX_MAP.put(134, 12);
        OCTET_COLUMN_INDEX_MAP.put(140, 13);
        OCTET_COLUMN_INDEX_MAP.put(141, 14);
        OCTET_COLUMN_INDEX_MAP.put(143, 15);
        OCTET_COLUMN_INDEX_MAP.put(144, 16);
        OCTET_COLUMN_INDEX_MAP.put(155, 17);
        OCTET_COLUMN_INDEX_MAP.put(145, 18);
        OCTET_COLUMN_INDEX_MAP.put(153, 19);
        OCTET_COLUMN_INDEX_MAP.put(149, 20);
        OCTET_COLUMN_NAME_MAP = new HashMap<>();
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(PduHeaders.CONTENT_CLASS), Telephony.BaseMmsColumns.CONTENT_CLASS);
        OCTET_COLUMN_NAME_MAP.put(134, Telephony.BaseMmsColumns.DELIVERY_REPORT);
        OCTET_COLUMN_NAME_MAP.put(140, Telephony.BaseMmsColumns.MESSAGE_TYPE);
        OCTET_COLUMN_NAME_MAP.put(141, Telephony.BaseMmsColumns.MMS_VERSION);
        OCTET_COLUMN_NAME_MAP.put(143, Telephony.BaseMmsColumns.PRIORITY);
        OCTET_COLUMN_NAME_MAP.put(144, Telephony.BaseMmsColumns.READ_REPORT);
        OCTET_COLUMN_NAME_MAP.put(155, Telephony.BaseMmsColumns.READ_STATUS);
        OCTET_COLUMN_NAME_MAP.put(145, Telephony.BaseMmsColumns.REPORT_ALLOWED);
        OCTET_COLUMN_NAME_MAP.put(153, Telephony.BaseMmsColumns.RETRIEVE_STATUS);
        OCTET_COLUMN_NAME_MAP.put(149, Telephony.BaseMmsColumns.STATUS);
        LONG_COLUMN_INDEX_MAP = new HashMap<>();
        LONG_COLUMN_INDEX_MAP.put(133, 21);
        LONG_COLUMN_INDEX_MAP.put(135, 22);
        LONG_COLUMN_INDEX_MAP.put(136, 23);
        LONG_COLUMN_INDEX_MAP.put(142, 24);
        LONG_COLUMN_NAME_MAP = new HashMap<>();
        LONG_COLUMN_NAME_MAP.put(133, "date");
        LONG_COLUMN_NAME_MAP.put(135, Telephony.BaseMmsColumns.DELIVERY_TIME);
        LONG_COLUMN_NAME_MAP.put(136, Telephony.BaseMmsColumns.EXPIRY);
        LONG_COLUMN_NAME_MAP.put(142, Telephony.BaseMmsColumns.MESSAGE_SIZE);
        PDU_CACHE_INSTANCE = PduCache.getInstance();
        LONG_COLUMN_INDEX_MAP.put(Integer.valueOf(PduHeaders.DATE_SENT), 28);
        LONG_COLUMN_NAME_MAP.put(Integer.valueOf(PduHeaders.DATE_SENT), "date_sent");
    }

    private PduPersister(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mDrmManagerClient = new DrmManagerClient(context);
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
    }

    public static PduPersister getPduPersister(Context context) {
        if (sPersister == null) {
            sPersister = new PduPersister(context);
        } else if (!context.equals(sPersister.mContext)) {
            sPersister.release();
            sPersister = new PduPersister(context);
        }
        return sPersister;
    }

    private void setEncodedStringValueToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        String s = c.getString(columnIndex);
        if (s == null || s.length() <= 0) {
            return;
        }
        int charsetColumnIndex = CHARSET_COLUMN_INDEX_MAP.get(Integer.valueOf(mapColumn)).intValue();
        int charset = c.getInt(charsetColumnIndex);
        EncodedStringValue value = new EncodedStringValue(charset, getBytes(s));
        headers.setEncodedStringValue(value, mapColumn);
    }

    private void setTextStringToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        String s = c.getString(columnIndex);
        if (s == null) {
            return;
        }
        headers.setTextString(getBytes(s), mapColumn);
    }

    private void setOctetToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) throws InvalidHeaderValueException {
        if (c.isNull(columnIndex)) {
            return;
        }
        int b = c.getInt(columnIndex);
        headers.setOctet(b, mapColumn);
    }

    private void setLongToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        if (c.isNull(columnIndex)) {
            return;
        }
        long l = c.getLong(columnIndex);
        headers.setLongInteger(l, mapColumn);
    }

    private Integer getIntegerFromPartColumn(Cursor c, int columnIndex) {
        if (!c.isNull(columnIndex)) {
            return Integer.valueOf(c.getInt(columnIndex));
        }
        return null;
    }

    private byte[] getByteArrayFromPartColumn(Cursor c, int columnIndex) {
        if (!c.isNull(columnIndex)) {
            return getBytes(c.getString(columnIndex));
        }
        return null;
    }

    private byte[] getByteArrayFromPartColumn2(Cursor c, int columnIndex) {
        if (!c.isNull(columnIndex)) {
            return c.getString(columnIndex).getBytes();
        }
        return null;
    }

    private PduPart[] loadParts(long msgId) throws MmsException {
        byte[] blob;
        Cursor c = SqliteWrapper.query(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + msgId + "/part"), PART_PROJECTION, null, null, null);
        if (c != null) {
            try {
                if (c.getCount() != 0) {
                    int partCount = c.getCount();
                    PduPart[] parts = new PduPart[partCount];
                    int partIdx = 0;
                    while (c.moveToNext()) {
                        PduPart part = new PduPart();
                        Integer charset = getIntegerFromPartColumn(c, 1);
                        if (charset != null) {
                            part.setCharset(charset.intValue());
                        }
                        byte[] contentDisposition = getByteArrayFromPartColumn(c, 2);
                        if (contentDisposition != null) {
                            part.setContentDisposition(contentDisposition);
                        }
                        byte[] contentId = getByteArrayFromPartColumn(c, 3);
                        if (contentId != null) {
                            part.setContentId(contentId);
                        }
                        byte[] contentLocation = getByteArrayFromPartColumn(c, 4);
                        if (contentLocation != null) {
                            part.setContentLocation(contentLocation);
                        }
                        byte[] contentType = getByteArrayFromPartColumn(c, 5);
                        if (contentType != null) {
                            part.setContentType(contentType);
                            byte[] fileName = getByteArrayFromPartColumn(c, 6);
                            if (fileName != null) {
                                part.setFilename(fileName);
                            }
                            byte[] name = getByteArrayFromPartColumn(c, 7);
                            if (name != null) {
                                part.setName(name);
                            }
                            long partId = c.getLong(0);
                            Uri partURI = Uri.parse("content://mms/part/" + partId);
                            part.setDataUri(partURI);
                            String type = toIsoString(contentType);
                            if (!ContentType.isImageType(type) && !ContentType.isAudioType(type) && !ContentType.isVideoType(type)) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                InputStream is = null;
                                if (ContentType.TEXT_PLAIN.equals(type) || ContentType.APP_SMIL.equals(type) || ContentType.TEXT_HTML.equals(type)) {
                                    String text = c.getString(8);
                                    if (charset != null) {
                                        int iIntValue = charset.intValue();
                                        if (text == null) {
                                            text = UsimPBMemInfo.STRING_NOT_SET;
                                        }
                                        blob = new EncodedStringValue(iIntValue, text).getTextString();
                                    } else {
                                        if (text == null) {
                                            text = UsimPBMemInfo.STRING_NOT_SET;
                                        }
                                        blob = new EncodedStringValue(text).getTextString();
                                    }
                                    baos.write(blob, 0, blob.length);
                                } else {
                                    try {
                                        try {
                                            is = this.mContentResolver.openInputStream(partURI);
                                            byte[] buffer = new byte[256];
                                            for (int len = is.read(buffer); len >= 0; len = is.read(buffer)) {
                                                baos.write(buffer, 0, len);
                                            }
                                        } catch (IOException e) {
                                            Log.e(TAG, "Failed to load part data", e);
                                            c.close();
                                            throw new MmsException(e);
                                        }
                                    } finally {
                                        if (is != null) {
                                            try {
                                                is.close();
                                            } catch (IOException e2) {
                                                Log.e(TAG, "Failed to close stream", e2);
                                            }
                                        }
                                    }
                                }
                                part.setData(baos.toByteArray());
                            }
                            parts[partIdx] = part;
                            partIdx++;
                        } else {
                            throw new MmsException("Content-Type must be set.");
                        }
                    }
                    if (c != null) {
                        c.close();
                    }
                    return parts;
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        return null;
    }

    private void loadAddress(long msgId, PduHeaders headers) {
        Cursor c = SqliteWrapper.query(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + msgId + "/addr"), new String[]{"address", Telephony.Mms.Addr.CHARSET, "type"}, null, null, null);
        if (c == null) {
            return;
        }
        while (c.moveToNext()) {
            try {
                String addr = c.getString(0);
                if (!TextUtils.isEmpty(addr)) {
                    int addrType = c.getInt(2);
                    switch (addrType) {
                        case 129:
                        case 130:
                        case 151:
                            headers.appendEncodedStringValue(new EncodedStringValue(c.getInt(1), getBytes(addr)), addrType);
                            break;
                        case 137:
                            headers.setEncodedStringValue(new EncodedStringValue(c.getInt(1), getBytes(addr)), addrType);
                            break;
                        default:
                            Log.e(TAG, "Unknown address type: " + addrType);
                            break;
                    }
                }
            } finally {
                c.close();
            }
        }
    }

    public GenericPdu load(Uri uri) throws Throwable {
        PduPart[] parts;
        GenericPdu pdu;
        PduCacheEntry cacheEntry = null;
        try {
            synchronized (PDU_CACHE_INSTANCE) {
                try {
                    if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                        try {
                            PDU_CACHE_INSTANCE.wait();
                        } catch (InterruptedException e) {
                            Log.e(TAG, "load: ", e);
                        }
                        cacheEntry = PDU_CACHE_INSTANCE.get(uri);
                        if (cacheEntry != null) {
                            GenericPdu pdu2 = cacheEntry.getPdu();
                            synchronized (PDU_CACHE_INSTANCE) {
                                PDU_CACHE_INSTANCE.setUpdating(uri, false);
                                PDU_CACHE_INSTANCE.notifyAll();
                            }
                            return pdu2;
                        }
                    }
                    try {
                        PDU_CACHE_INSTANCE.setUpdating(uri, true);
                        try {
                            Cursor c = SqliteWrapper.query(this.mContext, this.mContentResolver, uri, PDU_PROJECTION, null, null, null);
                            PduHeaders headers = new PduHeaders();
                            long msgId = ContentUris.parseId(uri);
                            if (c != null) {
                                try {
                                    if (c.getCount() == 1 && c.moveToFirst()) {
                                        int msgBox = c.getInt(1);
                                        long threadId = c.getLong(2);
                                        Set<Map.Entry<Integer, Integer>> set = ENCODED_STRING_COLUMN_INDEX_MAP.entrySet();
                                        for (Map.Entry<Integer, Integer> e2 : set) {
                                            setEncodedStringValueToHeaders(c, e2.getValue().intValue(), headers, e2.getKey().intValue());
                                        }
                                        Set<Map.Entry<Integer, Integer>> set2 = TEXT_STRING_COLUMN_INDEX_MAP.entrySet();
                                        for (Map.Entry<Integer, Integer> e3 : set2) {
                                            setTextStringToHeaders(c, e3.getValue().intValue(), headers, e3.getKey().intValue());
                                        }
                                        Set<Map.Entry<Integer, Integer>> set3 = OCTET_COLUMN_INDEX_MAP.entrySet();
                                        for (Map.Entry<Integer, Integer> e4 : set3) {
                                            setOctetToHeaders(c, e4.getValue().intValue(), headers, e4.getKey().intValue());
                                        }
                                        Set<Map.Entry<Integer, Integer>> set4 = LONG_COLUMN_INDEX_MAP.entrySet();
                                        for (Map.Entry<Integer, Integer> e5 : set4) {
                                            setLongToHeaders(c, e5.getValue().intValue(), headers, e5.getKey().intValue());
                                        }
                                        if (this.mBackupRestore) {
                                            Log.i(TAG, "load for backuprestore");
                                            if (!c.isNull(27)) {
                                                int b = c.getInt(27);
                                                Log.i(TAG, "read value=" + b);
                                                if (b == 1) {
                                                    headers.setOctet(128, 155);
                                                }
                                            }
                                        }
                                        if (msgId == -1) {
                                            throw new MmsException("Error! ID of the message: -1.");
                                        }
                                        loadAddress(msgId, headers);
                                        int msgType = headers.getOctet(140);
                                        PduBody body = new PduBody();
                                        if ((msgType == 132 || msgType == 128) && (parts = loadParts(msgId)) != null) {
                                            for (PduPart pduPart : parts) {
                                                body.addPart(pduPart);
                                            }
                                        }
                                        switch (msgType) {
                                            case 128:
                                                pdu = new SendReq(headers, body);
                                                break;
                                            case 129:
                                            case 137:
                                            case 138:
                                            case 139:
                                            case 140:
                                            case 141:
                                            case 142:
                                            case 143:
                                            case 144:
                                            case 145:
                                            case 146:
                                            case 147:
                                            case 148:
                                            case 149:
                                            case 150:
                                            case 151:
                                                throw new MmsException("Unsupported PDU type: " + Integer.toHexString(msgType));
                                            case 130:
                                                pdu = new NotificationInd(headers);
                                                break;
                                            case 131:
                                                pdu = new NotifyRespInd(headers);
                                                break;
                                            case 132:
                                                pdu = new RetrieveConf(headers, body);
                                                break;
                                            case 133:
                                                pdu = new AcknowledgeInd(headers);
                                                break;
                                            case 134:
                                                pdu = new DeliveryInd(headers);
                                                break;
                                            case 135:
                                                pdu = new ReadRecInd(headers);
                                                break;
                                            case 136:
                                                pdu = new ReadOrigInd(headers);
                                                break;
                                            default:
                                                throw new MmsException("Unrecognized PDU type: " + Integer.toHexString(msgType));
                                        }
                                        synchronized (PDU_CACHE_INSTANCE) {
                                            if (pdu != null) {
                                                try {
                                                    if (!f38assertionsDisabled) {
                                                        if (!(PDU_CACHE_INSTANCE.get(uri) == null)) {
                                                            throw new AssertionError();
                                                        }
                                                    }
                                                    PduCacheEntry cacheEntry2 = new PduCacheEntry(pdu, msgBox, threadId);
                                                    try {
                                                        PDU_CACHE_INSTANCE.put(uri, cacheEntry2);
                                                    } catch (Throwable th) {
                                                        th = th;
                                                        throw th;
                                                    }
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    throw th;
                                                }
                                            }
                                            PDU_CACHE_INSTANCE.setUpdating(uri, false);
                                            PDU_CACHE_INSTANCE.notifyAll();
                                            return pdu;
                                        }
                                    }
                                } finally {
                                    if (c != null) {
                                        c.close();
                                    }
                                }
                            }
                            throw new MmsException("Bad uri: " + uri);
                        } catch (Throwable th3) {
                            th = th3;
                            synchronized (PDU_CACHE_INSTANCE) {
                                PDU_CACHE_INSTANCE.setUpdating(uri, false);
                                PDU_CACHE_INSTANCE.notifyAll();
                            }
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        throw th;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    throw th;
                }
            }
        } catch (Throwable th6) {
            th = th6;
        }
    }

    private void persistAddress(long msgId, int type, EncodedStringValue[] array) {
        ArrayList<String> strValues = new ArrayList<>();
        ContentValues values = new ContentValues();
        Uri uri = Uri.parse("content://mms/" + msgId + "/addr");
        if (array == null) {
            return;
        }
        for (EncodedStringValue addr : array) {
            strValues.add(toIsoString(addr.getTextString()));
            strValues.add(String.valueOf(addr.getCharacterSet()));
            strValues.add(String.valueOf(type));
        }
        values.putStringArrayList("addresses", strValues);
        SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, values);
    }

    private static String getPartContentType(PduPart part) {
        if (part.getContentType() == null) {
            return null;
        }
        return toIsoString(part.getContentType());
    }

    public Uri persistPart(PduPart part, long msgId, HashMap<Uri, InputStream> preOpenedFiles) throws Throwable {
        Uri uri = Uri.parse("content://mms/" + msgId + "/part");
        ContentValues values = new ContentValues(8);
        String contentType = getPartContentType(part);
        if (contentType != null) {
            if (ContentType.IMAGE_JPG.equals(contentType)) {
                contentType = ContentType.IMAGE_JPEG;
            }
            values.put(Telephony.Mms.Part.CONTENT_TYPE, contentType);
            if (ContentType.APP_SMIL.equals(contentType)) {
                values.put(Telephony.Mms.Part.SEQ, (Integer) (-1));
            }
            int charset = part.getCharset();
            if (charset != 0 && !ContentType.APP_SMIL.equals(contentType)) {
                values.put(Telephony.Mms.Part.CHARSET, Integer.valueOf(charset));
            }
            if (part.getFilename() != null) {
                String fileName = new String(part.getFilename());
                values.put(Telephony.Mms.Part.FILENAME, fileName);
            }
            if (part.getName() != null) {
                String name = new String(part.getName());
                values.put("name", name);
            }
            if (part.getContentDisposition() != null) {
                Object value = toIsoString(part.getContentDisposition());
                values.put(Telephony.Mms.Part.CONTENT_DISPOSITION, (String) value);
            }
            if (part.getContentId() != null) {
                Object value2 = toIsoString(part.getContentId());
                values.put("cid", (String) value2);
            }
            if (part.getContentLocation() != null) {
                Object value3 = toIsoString(part.getContentLocation());
                values.put(Telephony.Mms.Part.CONTENT_LOCATION, (String) value3);
            }
            Uri res = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, values);
            if (res == null) {
                throw new MmsException("Failed to persist part, return null.");
            }
            persistData(part, res, contentType, preOpenedFiles);
            part.setDataUri(res);
            return res;
        }
        throw new MmsException("MIME type of the part must be set.");
    }

    private void persistData(PduPart part, Uri uri, String contentType, HashMap<Uri, InputStream> preOpenedFiles) throws Throwable {
        OutputStream os = null;
        InputStream is = null;
        DrmConvertSession drmConvertSession = null;
        String path = null;
        try {
            try {
                byte[] data = part.getData();
                if (ContentType.TEXT_PLAIN.equals(contentType) || ContentType.APP_SMIL.equals(contentType) || ContentType.TEXT_HTML.equals(contentType)) {
                    ContentValues cv = new ContentValues();
                    if (data == null) {
                        Log.v("MMSLog", "convert data from null to empty.");
                        data = new String(UsimPBMemInfo.STRING_NOT_SET).getBytes("utf-8");
                    }
                    int charset = part.getCharset();
                    if (charset == 0 || ContentType.APP_SMIL.equals(contentType)) {
                        cv.put("text", new EncodedStringValue(data).getString());
                    } else {
                        cv.put("text", new EncodedStringValue(charset, data).getString());
                    }
                    if (this.mContentResolver.update(uri, cv, null, null) != 1) {
                        throw new MmsException("unable to update " + uri.toString());
                    }
                } else {
                    boolean isDrm = DownloadDrmHelper.isDrmConvertNeeded(contentType);
                    if (isDrm) {
                        if (uri != null) {
                            try {
                                path = convertUriToPath(this.mContext, uri);
                                File f = new File(path);
                                if (f.length() > 0) {
                                    return;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Can't get file info for: " + part.getDataUri(), e);
                            }
                        }
                        drmConvertSession = DrmConvertSession.open(this.mContext, contentType);
                        if (drmConvertSession == null) {
                            throw new MmsException("Mimetype " + contentType + " can not be converted.");
                        }
                    }
                    os = this.mContentResolver.openOutputStream(uri);
                    if (data == null) {
                        Uri dataUri = part.getDataUri();
                        if (dataUri != null && dataUri != uri) {
                            if (preOpenedFiles != null && preOpenedFiles.containsKey(dataUri)) {
                                is = preOpenedFiles.get(dataUri);
                            }
                            if (is == null) {
                                is = this.mContentResolver.openInputStream(dataUri);
                            }
                            if (is != null) {
                                byte[] buffer = new byte[SmsCbConstants.SERIAL_NUMBER_ETWS_EMERGENCY_USER_ALERT];
                                while (true) {
                                    int len = is.read(buffer);
                                    if (len == -1) {
                                        break;
                                    }
                                    if (isDrm) {
                                        byte[] convertedData = drmConvertSession.convert(buffer, len);
                                        if (convertedData == null) {
                                            throw new MmsException("Error converting drm data.");
                                        }
                                        os.write(convertedData, 0, convertedData.length);
                                    } else {
                                        os.write(buffer, 0, len);
                                    }
                                }
                            } else {
                                Log.d(TAG, "the valude of ContentResolver.openInputStream() is null. InputStream uri:" + dataUri);
                                if (os != null) {
                                    try {
                                        os.close();
                                    } catch (IOException e2) {
                                        Log.e(TAG, "IOException while closing: " + os, e2);
                                    }
                                }
                                if (is != null) {
                                    try {
                                        is.close();
                                    } catch (IOException e3) {
                                        Log.e(TAG, "IOException while closing: " + is, e3);
                                    }
                                }
                                if (drmConvertSession != null) {
                                    drmConvertSession.close(path);
                                    File f2 = new File(path);
                                    ContentValues values = new ContentValues(0);
                                    SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + f2.getName()), values, null, null);
                                    return;
                                }
                                return;
                            }
                        } else {
                            Log.w(TAG, "Can't find data for this part.");
                            if (os != null) {
                                try {
                                    os.close();
                                } catch (IOException e4) {
                                    Log.e(TAG, "IOException while closing: " + os, e4);
                                }
                            }
                            if (drmConvertSession != null) {
                                drmConvertSession.close(path);
                                File f3 = new File(path);
                                ContentValues values2 = new ContentValues(0);
                                SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + f3.getName()), values2, null, null);
                                return;
                            }
                            return;
                        }
                    } else if (isDrm) {
                        byte[] convertedData2 = drmConvertSession.convert(data, data.length);
                        if (convertedData2 == null) {
                            throw new MmsException("Error converting drm data.");
                        }
                        os.write(convertedData2, 0, convertedData2.length);
                    } else {
                        os.write(data);
                    }
                }
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e5) {
                        Log.e(TAG, "IOException while closing: " + os, e5);
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e6) {
                        Log.e(TAG, "IOException while closing: " + is, e6);
                    }
                }
                if (drmConvertSession != null) {
                    drmConvertSession.close(path);
                    File f4 = new File(path);
                    ContentValues values3 = new ContentValues(0);
                    SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + f4.getName()), values3, null, null);
                }
            } finally {
            }
        } catch (FileNotFoundException e7) {
            Log.e(TAG, "Failed to open Input/Output stream.", e7);
            throw new MmsException(e7);
        } catch (IOException e8) {
            Log.e(TAG, "Failed to read/write data.", e8);
            throw new MmsException(e8);
        }
    }

    public static String convertUriToPath(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals(UsimPBMemInfo.STRING_NOT_SET) || scheme.equals("file")) {
            return uri.getPath();
        }
        if (scheme.equals("http")) {
            return uri.toString();
        }
        if (scheme.equals("content")) {
            String[] projection = {"_data"};
            Cursor cursor = null;
            try {
                try {
                    Cursor cursor2 = context.getContentResolver().query(uri, projection, null, null, null);
                    if (cursor2 == null || cursor2.getCount() == 0 || !cursor2.moveToFirst()) {
                        throw new IllegalArgumentException("Given Uri could not be found in media store");
                    }
                    int pathIndex = cursor2.getColumnIndexOrThrow("_data");
                    String path = cursor2.getString(pathIndex);
                    if (cursor2 != null) {
                        cursor2.close();
                        return path;
                    }
                    return path;
                } catch (SQLiteException e) {
                    throw new IllegalArgumentException("Given Uri is not formatted in a way so that it can be found in media store.");
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    cursor.close();
                }
                throw th;
            }
        }
        throw new IllegalArgumentException("Given Uri scheme is not supported");
    }

    private void updateAddress(long msgId, int type, EncodedStringValue[] array) {
        SqliteWrapper.delete(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + msgId + "/addr"), "type=" + type, null);
        persistAddress(msgId, type, array);
    }

    public void updateHeaders(Uri uri, SendReq sendReq) {
        synchronized (PDU_CACHE_INSTANCE) {
            if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                try {
                    PDU_CACHE_INSTANCE.wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "updateHeaders: ", e);
                }
            }
        }
        PDU_CACHE_INSTANCE.purge(uri);
        ContentValues values = new ContentValues(10);
        byte[] contentType = sendReq.getContentType();
        if (contentType != null) {
            values.put(Telephony.BaseMmsColumns.CONTENT_TYPE, toIsoString(contentType));
        }
        long date = sendReq.getDate();
        if (date != -1) {
            values.put("date", Long.valueOf(date));
        }
        int deliveryReport = sendReq.getDeliveryReport();
        if (deliveryReport != 0) {
            values.put(Telephony.BaseMmsColumns.DELIVERY_REPORT, Integer.valueOf(deliveryReport));
        }
        long expiry = sendReq.getExpiry();
        if (expiry != -1) {
            values.put(Telephony.BaseMmsColumns.EXPIRY, Long.valueOf(expiry));
        }
        byte[] msgClass = sendReq.getMessageClass();
        if (msgClass != null) {
            values.put(Telephony.BaseMmsColumns.MESSAGE_CLASS, toIsoString(msgClass));
        }
        int priority = sendReq.getPriority();
        if (priority != 0) {
            values.put(Telephony.BaseMmsColumns.PRIORITY, Integer.valueOf(priority));
        }
        int readReport = sendReq.getReadReport();
        if (readReport != 0) {
            values.put(Telephony.BaseMmsColumns.READ_REPORT, Integer.valueOf(readReport));
        }
        byte[] transId = sendReq.getTransactionId();
        if (transId != null) {
            values.put(Telephony.BaseMmsColumns.TRANSACTION_ID, toIsoString(transId));
        }
        EncodedStringValue subject = sendReq.getSubject();
        if (subject != null) {
            values.put(Telephony.BaseMmsColumns.SUBJECT, toIsoString(subject.getTextString()));
            values.put(Telephony.BaseMmsColumns.SUBJECT_CHARSET, Integer.valueOf(subject.getCharacterSet()));
        } else {
            values.put(Telephony.BaseMmsColumns.SUBJECT, UsimPBMemInfo.STRING_NOT_SET);
        }
        long messageSize = sendReq.getMessageSize();
        if (messageSize > 0) {
            values.put(Telephony.BaseMmsColumns.MESSAGE_SIZE, Long.valueOf(messageSize));
        }
        PduHeaders headers = sendReq.getPduHeaders();
        HashSet<String> recipients = new HashSet<>();
        for (int addrType : ADDRESS_FIELDS) {
            EncodedStringValue[] array = null;
            if (addrType == 137) {
                EncodedStringValue v = headers.getEncodedStringValue(addrType);
                if (v != null) {
                    array = new EncodedStringValue[]{v};
                }
            } else {
                array = headers.getEncodedStringValues(addrType);
            }
            if (array != null) {
                long msgId = ContentUris.parseId(uri);
                updateAddress(msgId, addrType, array);
                if (addrType == 151) {
                    for (EncodedStringValue v2 : array) {
                        if (v2 != null) {
                            recipients.add(v2.getString());
                        }
                    }
                }
            }
            if (addrType == 130 && array == null) {
                long msgId2 = ContentUris.parseId(uri);
                updateAddress(msgId2, addrType, array);
            }
        }
        if (!recipients.isEmpty()) {
            long threadId = Telephony.Threads.getOrCreateThreadId(this.mContext, recipients);
            values.put("thread_id", Long.valueOf(threadId));
        }
        SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
    }

    private void updatePart(Uri uri, PduPart part, HashMap<Uri, InputStream> preOpenedFiles) throws Throwable {
        ContentValues values = new ContentValues(7);
        int charset = part.getCharset();
        if (charset != 0) {
            values.put(Telephony.Mms.Part.CHARSET, Integer.valueOf(charset));
        }
        if (part.getContentType() != null) {
            String contentType = toIsoString(part.getContentType());
            values.put(Telephony.Mms.Part.CONTENT_TYPE, contentType);
            if (part.getFilename() != null) {
                String fileName = new String(part.getFilename());
                values.put(Telephony.Mms.Part.FILENAME, fileName);
            }
            if (part.getName() != null) {
                String name = new String(part.getName());
                values.put("name", name);
            }
            if (part.getContentDisposition() != null) {
                Object value = toIsoString(part.getContentDisposition());
                values.put(Telephony.Mms.Part.CONTENT_DISPOSITION, (String) value);
            }
            if (part.getContentId() != null) {
                Object value2 = toIsoString(part.getContentId());
                values.put("cid", (String) value2);
            }
            if (part.getContentLocation() != null) {
                Object value3 = toIsoString(part.getContentLocation());
                values.put(Telephony.Mms.Part.CONTENT_LOCATION, (String) value3);
            }
            SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
            if (part.getData() == null && uri == part.getDataUri()) {
                return;
            }
            persistData(part, uri, contentType, preOpenedFiles);
            return;
        }
        throw new MmsException("MIME type of the part must be set.");
    }

    public void updateParts(Uri uri, PduBody body, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        try {
            synchronized (PDU_CACHE_INSTANCE) {
                if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                    try {
                        PDU_CACHE_INSTANCE.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "updateParts: ", e);
                    }
                    PduCacheEntry cacheEntry = PDU_CACHE_INSTANCE.get(uri);
                    if (cacheEntry != null) {
                        ((MultimediaMessagePdu) cacheEntry.getPdu()).setBody(body);
                    }
                    PDU_CACHE_INSTANCE.setUpdating(uri, true);
                } else {
                    PDU_CACHE_INSTANCE.setUpdating(uri, true);
                }
            }
            ArrayList<PduPart> toBeCreated = new ArrayList<>();
            HashMap<Uri, PduPart> toBeUpdated = new HashMap<>();
            int partsNum = body.getPartsNum();
            StringBuilder filter = new StringBuilder().append('(');
            int skippedCount = 0;
            int updateCount = 0;
            for (int i = 0; i < partsNum; i++) {
                PduPart part = body.getPart(i);
                Uri partUri = part.getDataUri();
                if (partUri == null || !partUri.getAuthority().startsWith("mms")) {
                    toBeCreated.add(part);
                    updateCount++;
                } else {
                    if (part.needUpdate()) {
                        toBeUpdated.put(partUri, part);
                        updateCount++;
                    } else {
                        skippedCount++;
                    }
                    if (filter.length() > 1) {
                        filter.append(" AND ");
                    }
                    filter.append("_id");
                    filter.append("!=");
                    DatabaseUtils.appendEscapedSQLString(filter, partUri.getLastPathSegment());
                }
            }
            filter.append(')');
            long msgId = ContentUris.parseId(uri);
            SqliteWrapper.delete(this.mContext, this.mContentResolver, Uri.parse(Telephony.Mms.CONTENT_URI + "/" + msgId + "/part"), filter.length() > 2 ? filter.toString() : null, null);
            Iterator part$iterator = toBeCreated.iterator();
            while (part$iterator.hasNext()) {
                persistPart((PduPart) part$iterator.next(), msgId, preOpenedFiles);
            }
            for (Map.Entry<Uri, PduPart> e2 : toBeUpdated.entrySet()) {
                updatePart(e2.getKey(), e2.getValue(), preOpenedFiles);
            }
            synchronized (PDU_CACHE_INSTANCE) {
                PDU_CACHE_INSTANCE.setUpdating(uri, false);
                PDU_CACHE_INSTANCE.notifyAll();
            }
        } catch (Throwable th) {
            synchronized (PDU_CACHE_INSTANCE) {
                PDU_CACHE_INSTANCE.setUpdating(uri, false);
                PDU_CACHE_INSTANCE.notifyAll();
                throw th;
            }
        }
    }

    public Uri persist(GenericPdu pdu, Uri uri, boolean createThreadId, boolean groupMmsEnabled) throws MmsException {
        return persist(pdu, uri, createThreadId, groupMmsEnabled, null);
    }

    public Uri persist(GenericPdu pdu, Uri uri) throws MmsException {
        return persist(pdu, uri, true, false);
    }

    public Uri persist(GenericPdu pdu, Uri uri, boolean createThreadId, boolean groupMmsEnabled, HashMap<Uri, InputStream> preOpenedFiles) throws Throwable {
        Uri res;
        PduBody body;
        if (uri == null) {
            throw new MmsException("Uri may not be null.");
        }
        long msgId = -1;
        try {
            msgId = ContentUris.parseId(uri);
        } catch (NumberFormatException e) {
        }
        boolean existingUri = msgId != -1;
        if (!existingUri && MESSAGE_BOX_MAP.get(uri) == null) {
            throw new MmsException("Bad destination, must be one of content://mms/inbox, content://mms/sent, content://mms/drafts, content://mms/outbox, content://mms/temp.");
        }
        synchronized (PDU_CACHE_INSTANCE) {
            if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                try {
                    PDU_CACHE_INSTANCE.wait();
                } catch (InterruptedException e2) {
                    Log.e(TAG, "persist1: ", e2);
                }
            }
        }
        PDU_CACHE_INSTANCE.purge(uri);
        Log.d(TAG, "persist uri " + uri);
        PduHeaders header = pdu.getPduHeaders();
        ContentValues values = new ContentValues();
        Set<Map.Entry<Integer, String>> set = ENCODED_STRING_COLUMN_NAME_MAP.entrySet();
        for (Map.Entry<Integer, String> e3 : set) {
            int field = e3.getKey().intValue();
            EncodedStringValue encodedString = header.getEncodedStringValue(field);
            if (encodedString != null) {
                String charsetColumn = CHARSET_COLUMN_NAME_MAP.get(Integer.valueOf(field));
                values.put(e3.getValue(), toIsoString(encodedString.getTextString()));
                values.put(charsetColumn, Integer.valueOf(encodedString.getCharacterSet()));
            }
        }
        Set<Map.Entry<Integer, String>> set2 = TEXT_STRING_COLUMN_NAME_MAP.entrySet();
        for (Map.Entry<Integer, String> e4 : set2) {
            byte[] text = header.getTextString(e4.getKey().intValue());
            if (text != null) {
                values.put(e4.getValue(), toIsoString(text));
            }
        }
        Set<Map.Entry<Integer, String>> set3 = OCTET_COLUMN_NAME_MAP.entrySet();
        for (Map.Entry<Integer, String> e5 : set3) {
            int b = header.getOctet(e5.getKey().intValue());
            if (b != 0) {
                values.put(e5.getValue(), Integer.valueOf(b));
            }
        }
        if (this.mBackupRestore) {
            Log.i(TAG, "add READ");
            int b2 = header.getOctet(155);
            Log.i(TAG, "READ=" + b2);
            if (b2 == 0) {
                values.put("read", Integer.valueOf(b2));
            } else if (b2 == 128) {
                values.put("read", (Integer) 1);
            } else {
                values.put("read", (Integer) 0);
            }
        }
        Set<Map.Entry<Integer, String>> set4 = LONG_COLUMN_NAME_MAP.entrySet();
        for (Map.Entry<Integer, String> e6 : set4) {
            long l = header.getLongInteger(e6.getKey().intValue());
            if (l != -1) {
                values.put(e6.getValue(), Long.valueOf(l));
            }
        }
        HashMap<Integer, EncodedStringValue[]> addressMap = new HashMap<>(ADDRESS_FIELDS.length);
        for (int addrType : ADDRESS_FIELDS) {
            EncodedStringValue[] array = null;
            if (addrType == 137) {
                EncodedStringValue v = header.getEncodedStringValue(addrType);
                if (v != null) {
                    array = new EncodedStringValue[]{v};
                }
            } else {
                array = header.getEncodedStringValues(addrType);
            }
            addressMap.put(Integer.valueOf(addrType), array);
        }
        HashSet<String> recipients = new HashSet<>();
        int msgType = pdu.getMessageType();
        if (msgType == 130 || msgType == 132 || msgType == 128) {
            switch (msgType) {
                case 128:
                    loadRecipients(151, recipients, addressMap, false);
                    break;
                case 130:
                case 132:
                    loadRecipients(137, recipients, addressMap, false);
                    if (groupMmsEnabled) {
                        loadRecipients(151, recipients, addressMap, true);
                        loadRecipients(130, recipients, addressMap, true);
                    }
                    break;
            }
            long threadId = 0;
            if (createThreadId && !recipients.isEmpty()) {
                threadId = Telephony.Threads.getOrCreateThreadId(this.mContext, recipients);
            }
            values.put("thread_id", Long.valueOf(threadId));
        }
        Log.d(TAG, "persist part begin ");
        long dummyId = System.currentTimeMillis();
        boolean textOnly = true;
        int messageSize = 0;
        if ((pdu instanceof MultimediaMessagePdu) && (body = ((MultimediaMessagePdu) pdu).getBody()) != null) {
            int partsNum = body.getPartsNum();
            if (partsNum > 2) {
                textOnly = false;
            }
            for (int i = 0; i < partsNum; i++) {
                PduPart part = body.getPart(i);
                messageSize += part.getDataLength();
                persistPart(part, dummyId, preOpenedFiles);
                String contentType = getPartContentType(part);
                if (contentType != null && !ContentType.APP_SMIL.equals(contentType) && !ContentType.TEXT_PLAIN.equals(contentType)) {
                    textOnly = false;
                }
            }
        }
        values.put(Telephony.BaseMmsColumns.TEXT_ONLY, Integer.valueOf(textOnly ? 1 : 0));
        if (values.getAsInteger(Telephony.BaseMmsColumns.MESSAGE_SIZE) == null) {
            values.put(Telephony.BaseMmsColumns.MESSAGE_SIZE, Integer.valueOf(messageSize));
        }
        Log.d(TAG, "persist pdu begin ");
        values.put("need_notify", (Boolean) false);
        if (this.mBackupRestore) {
            values.put("seen", (Integer) 1);
        }
        if (existingUri) {
            res = uri;
            SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
        } else {
            res = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, values);
            if (res == null) {
                throw new MmsException("persist() failed: return null.");
            }
            msgId = ContentUris.parseId(res);
        }
        Log.d(TAG, "persist address begin ");
        for (int addrType2 : ADDRESS_FIELDS) {
            EncodedStringValue[] array2 = addressMap.get(Integer.valueOf(addrType2));
            if (array2 != null) {
                persistAddress(msgId, addrType2, array2);
            }
        }
        Log.d(TAG, "persist update part begin ");
        ContentValues values2 = new ContentValues(1);
        values2.put(Telephony.Mms.Part.MSG_ID, Long.valueOf(msgId));
        values2.put("need_notify", (Boolean) true);
        SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + dummyId + "/part"), values2, null, null);
        PDU_CACHE_INSTANCE.purge(uri);
        Log.d(TAG, "persist purge end ");
        if (!existingUri) {
            return Uri.parse(uri + "/" + msgId);
        }
        return res;
    }

    private void loadRecipients(int addressType, HashSet<String> recipients, HashMap<Integer, EncodedStringValue[]> addressMap, boolean excludeMyNumber) {
        EncodedStringValue[] array = addressMap.get(Integer.valueOf(addressType));
        if (array == null) {
            return;
        }
        if (excludeMyNumber && array.length == 1) {
            return;
        }
        int[] SubIdList = SubscriptionManager.from(this.mContext).getActiveSubscriptionIdList();
        for (EncodedStringValue v : array) {
            if (v != null) {
                String number = v.getString();
                Log.d(TAG, "number = " + number);
                Log.d(TAG, "length = " + SubIdList.length);
                if (SubIdList.length == 0) {
                    Log.d(TAG, "recipients add number = " + number);
                    recipients.add(number);
                } else {
                    for (int subid : SubIdList) {
                        Log.d(TAG, "subid = " + subid);
                        String myNumber = excludeMyNumber ? this.mTelephonyManager.getLine1Number(subid) : null;
                        if ((myNumber == null || !PhoneNumberUtils.compare(number, myNumber)) && !recipients.contains(number)) {
                            recipients.add(number);
                        }
                    }
                }
            }
        }
    }

    public Uri move(Uri from, Uri to) throws MmsException {
        long msgId = ContentUris.parseId(from);
        if (msgId == -1) {
            throw new MmsException("Error! ID of the message: -1.");
        }
        Integer msgBox = MESSAGE_BOX_MAP.get(to);
        if (msgBox == null) {
            throw new MmsException("Bad destination, must be one of content://mms/inbox, content://mms/sent, content://mms/drafts, content://mms/outbox, content://mms/temp.");
        }
        ContentValues values = new ContentValues(1);
        values.put(Telephony.BaseMmsColumns.MESSAGE_BOX, msgBox);
        SqliteWrapper.update(this.mContext, this.mContentResolver, from, values, null, null);
        return ContentUris.withAppendedId(to, msgId);
    }

    public static String toIsoString(byte[] bytes) {
        try {
            return new String(bytes, CharacterSets.MIMENAME_ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "ISO_8859_1 must be supported!", e);
            return UsimPBMemInfo.STRING_NOT_SET;
        } catch (NullPointerException e2) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
    }

    public static byte[] getBytes(String data) {
        try {
            return data.getBytes(CharacterSets.MIMENAME_ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "ISO_8859_1 must be supported!", e);
            return new byte[0];
        }
    }

    public void release() {
        Uri uri = Uri.parse(TEMPORARY_DRM_OBJECT_URI);
        SqliteWrapper.delete(this.mContext, this.mContentResolver, uri, null, null);
    }

    public Cursor getPendingMessages(long dueTime) {
        Uri.Builder uriBuilder = Telephony.MmsSms.PendingMessages.CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter("protocol", "mms");
        String[] selectionArgs = {String.valueOf(10), String.valueOf(dueTime)};
        return SqliteWrapper.query(this.mContext, this.mContentResolver, uriBuilder.build(), null, "err_type < ? AND due_time <= ?", selectionArgs, Telephony.MmsSms.PendingMessages.DUE_TIME);
    }

    public GenericPdu load(Uri uri, boolean backupRestore) throws MmsException {
        Log.i("MMSLog", "load for backuprestore");
        this.mBackupRestore = backupRestore;
        return load(uri);
    }

    public Uri persist(GenericPdu pdu, Uri uri, boolean backupRestore) throws MmsException {
        Log.i("MMSLog", "persist for backuprestore");
        this.mBackupRestore = backupRestore;
        return persist(pdu, uri, true, false);
    }

    public Uri persistEx(GenericPdu pdu, Uri uri, HashMap<String, String> attach) throws MmsException {
        Log.i("MMSLog", "Call persist_ex 1");
        return persistForBackupRestore(pdu, uri, attach);
    }

    public Uri persistEx(GenericPdu pdu, Uri uri, boolean backupRestore, HashMap<String, String> attach) throws MmsException {
        Log.i("MMSLog", "Call persist_ex 2");
        this.mBackupRestore = backupRestore;
        return persistForBackupRestore(pdu, uri, attach);
    }

    private Uri persistForBackupRestore(GenericPdu genericPdu, Uri uri, HashMap<String, String> attach) throws Throwable {
        Uri res;
        PduBody body;
        if (uri == null) {
            throw new MmsException("Uri may not be null.");
        }
        long msgId = -1;
        try {
            msgId = ContentUris.parseId(uri);
        } catch (NumberFormatException e) {
        }
        boolean existingUri = msgId != -1;
        if (!existingUri && MESSAGE_BOX_MAP.get(uri) == null) {
            throw new MmsException("Bad destination, must be one of content://mms/inbox, content://mms/sent, content://mms/drafts, content://mms/outbox, content://mms/temp.");
        }
        synchronized (PDU_CACHE_INSTANCE) {
            if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                try {
                    PDU_CACHE_INSTANCE.wait();
                } catch (InterruptedException e2) {
                    Log.e(TAG, "persist1: ", e2);
                }
            }
        }
        PDU_CACHE_INSTANCE.purge(uri);
        Log.d(TAG, "persist uri " + uri);
        PduHeaders header = genericPdu.getPduHeaders();
        ContentValues values = new ContentValues();
        Set<Map.Entry<Integer, String>> set = ENCODED_STRING_COLUMN_NAME_MAP.entrySet();
        for (Map.Entry<Integer, String> e3 : set) {
            int field = e3.getKey().intValue();
            EncodedStringValue encodedString = header.getEncodedStringValue(field);
            if (encodedString != null) {
                String charsetColumn = CHARSET_COLUMN_NAME_MAP.get(Integer.valueOf(field));
                values.put(e3.getValue(), toIsoString(encodedString.getTextString()));
                values.put(charsetColumn, Integer.valueOf(encodedString.getCharacterSet()));
            }
        }
        Set<Map.Entry<Integer, String>> set2 = TEXT_STRING_COLUMN_NAME_MAP.entrySet();
        for (Map.Entry<Integer, String> e4 : set2) {
            byte[] text = header.getTextString(e4.getKey().intValue());
            if (text != null) {
                values.put(e4.getValue(), toIsoString(text));
            }
        }
        Set<Map.Entry<Integer, String>> set3 = OCTET_COLUMN_NAME_MAP.entrySet();
        for (Map.Entry<Integer, String> e5 : set3) {
            int b = header.getOctet(e5.getKey().intValue());
            if (b != 0) {
                values.put(e5.getValue(), Integer.valueOf(b));
            }
        }
        if (this.mBackupRestore) {
            int read = 0;
            if (attach != null) {
                read = Integer.parseInt(attach.get("read"));
            }
            values.put("read", Integer.valueOf(read));
            long size = 0;
            if (attach != null && attach.get(Telephony.BaseMmsColumns.MESSAGE_SIZE) != null) {
                size = Long.parseLong(attach.get(Telephony.BaseMmsColumns.MESSAGE_SIZE));
            }
            values.put(Telephony.BaseMmsColumns.MESSAGE_SIZE, Long.valueOf(size));
        }
        Set<Map.Entry<Integer, String>> set4 = LONG_COLUMN_NAME_MAP.entrySet();
        for (Map.Entry<Integer, String> e6 : set4) {
            long l = header.getLongInteger(e6.getKey().intValue());
            if (l != -1) {
                values.put(e6.getValue(), Long.valueOf(l));
            }
        }
        HashMap<Integer, EncodedStringValue[]> addressMap = new HashMap<>(ADDRESS_FIELDS.length);
        for (int addrType : ADDRESS_FIELDS) {
            EncodedStringValue[] array = null;
            if (addrType == 137) {
                EncodedStringValue v = header.getEncodedStringValue(addrType);
                if (v != null) {
                    array = new EncodedStringValue[]{v};
                }
            } else {
                array = header.getEncodedStringValues(addrType);
            }
            addressMap.put(Integer.valueOf(addrType), array);
        }
        HashSet<String> recipients = new HashSet<>();
        int msgType = genericPdu.getMessageType();
        if (msgType == 130 || msgType == 132 || msgType == 128) {
            EncodedStringValue[] array2 = null;
            switch (msgType) {
                case 128:
                    EncodedStringValue[] array3 = addressMap.get(151);
                    array2 = array3;
                    break;
                case 130:
                case 132:
                    EncodedStringValue[] array4 = addressMap.get(137);
                    array2 = array4;
                    break;
            }
            if (array2 != null) {
                for (EncodedStringValue v2 : array2) {
                    if (v2 != null) {
                        recipients.add(v2.getString());
                    }
                }
            }
            long time_1 = System.currentTimeMillis();
            String backupRestore = null;
            if (attach != null) {
                String backupRestore2 = attach.get("index");
                backupRestore = backupRestore2;
            }
            if (!recipients.isEmpty()) {
                long threadId = Telephony.Threads.getOrCreateThreadId(this.mContext, recipients, backupRestore);
                values.put("thread_id", Long.valueOf(threadId));
            }
            long time_2 = System.currentTimeMillis();
            long getThreadId_t = time_2 - time_1;
            Log.d("MMSLog", "BR_TEST: getThreadId=" + getThreadId_t);
        }
        long time_12 = System.currentTimeMillis();
        Log.d(TAG, "persist pdu begin ");
        values.put("need_notify", (Boolean) true);
        if (this.mBackupRestore) {
            values.put("seen", (Integer) 1);
        }
        int sub_id = -1;
        int locked = 0;
        if (attach != null) {
            sub_id = Integer.parseInt(attach.get("sub_id"));
            locked = Integer.parseInt(attach.get("locked"));
        }
        values.put("sub_id", Integer.valueOf(sub_id));
        values.put("locked", Integer.valueOf(locked));
        if (existingUri) {
            res = uri;
            SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
        } else {
            res = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, values);
            if (res == null) {
                throw new MmsException("persist() failed: return null.");
            }
            msgId = ContentUris.parseId(res);
        }
        long time_22 = System.currentTimeMillis();
        long persistPdu_t = time_22 - time_12;
        Log.d("MMSLog", "BR_TEST: parse time persistPdu=" + persistPdu_t);
        Log.d(TAG, "persist address begin ");
        long time_13 = System.currentTimeMillis();
        for (int addrType2 : ADDRESS_FIELDS) {
            EncodedStringValue[] array5 = addressMap.get(Integer.valueOf(addrType2));
            if (array5 != null) {
                persistAddress(msgId, addrType2, array5);
            }
        }
        long time_23 = System.currentTimeMillis();
        long persistAddress_t = time_23 - time_13;
        Log.d("MMSLog", "BR_TEST: parse time persistAddress=" + persistAddress_t);
        Log.d(TAG, "persist part begin ");
        if ((genericPdu instanceof MultimediaMessagePdu) && (body = genericPdu.getBody()) != null) {
            int partsNum = body.getPartsNum();
            long time_14 = System.currentTimeMillis();
            for (int i = 0; i < partsNum; i++) {
                PduPart part = body.getPart(i);
                persistPart(part, msgId, null);
            }
            long time_24 = System.currentTimeMillis();
            long persistPart_t = time_24 - time_14;
            Log.d("MMSLog", "BR_TEST: parse time PersistPart=" + persistPart_t);
        }
        if (!existingUri) {
            return Uri.parse(uri + "/" + msgId);
        }
        return res;
    }
}
