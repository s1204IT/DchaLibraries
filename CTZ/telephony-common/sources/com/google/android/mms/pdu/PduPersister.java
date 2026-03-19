package com.google.android.mms.pdu;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.drm.DrmManagerClient;
import android.hardware.radio.V1_0.RadioAccessFamily;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.HbpcdLookup;
import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MmsException;
import com.google.android.mms.util.DownloadDrmHelper;
import com.google.android.mms.util.DrmConvertSession;
import com.google.android.mms.util.PduCache;
import com.google.android.mms.util.PduCacheEntry;
import com.google.android.mms.util.SqliteWrapper;
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

public class PduPersister {
    static final boolean $assertionsDisabled = false;
    protected static final HashMap<Integer, Integer> CHARSET_COLUMN_INDEX_MAP;
    protected static final HashMap<Integer, String> CHARSET_COLUMN_NAME_MAP;
    private static final boolean DEBUG = false;
    private static final long DUMMY_THREAD_ID = Long.MAX_VALUE;
    protected static final HashMap<Integer, Integer> ENCODED_STRING_COLUMN_INDEX_MAP;
    protected static final HashMap<Integer, String> ENCODED_STRING_COLUMN_NAME_MAP;
    protected static final boolean LOCAL_LOGV = false;
    protected static final HashMap<Integer, Integer> LONG_COLUMN_INDEX_MAP;
    protected static final HashMap<Integer, String> LONG_COLUMN_NAME_MAP;
    protected static final HashMap<Integer, Integer> OCTET_COLUMN_INDEX_MAP;
    protected static final HashMap<Integer, String> OCTET_COLUMN_NAME_MAP;
    protected static final int PART_COLUMN_CHARSET = 1;
    protected static final int PART_COLUMN_CONTENT_DISPOSITION = 2;
    protected static final int PART_COLUMN_CONTENT_ID = 3;
    protected static final int PART_COLUMN_CONTENT_LOCATION = 4;
    protected static final int PART_COLUMN_CONTENT_TYPE = 5;
    protected static final int PART_COLUMN_FILENAME = 6;
    protected static final int PART_COLUMN_ID = 0;
    protected static final int PART_COLUMN_NAME = 7;
    protected static final int PART_COLUMN_TEXT = 8;
    protected static final PduCache PDU_CACHE_INSTANCE;
    protected static final int PDU_COLUMN_CONTENT_CLASS = 11;
    protected static final int PDU_COLUMN_CONTENT_LOCATION = 5;
    protected static final int PDU_COLUMN_CONTENT_TYPE = 6;
    protected static final int PDU_COLUMN_DATE = 21;
    protected static final int PDU_COLUMN_DELIVERY_REPORT = 12;
    protected static final int PDU_COLUMN_DELIVERY_TIME = 22;
    protected static final int PDU_COLUMN_EXPIRY = 23;
    protected static final int PDU_COLUMN_ID = 0;
    protected static final int PDU_COLUMN_MESSAGE_BOX = 1;
    protected static final int PDU_COLUMN_MESSAGE_CLASS = 7;
    protected static final int PDU_COLUMN_MESSAGE_ID = 8;
    protected static final int PDU_COLUMN_MESSAGE_SIZE = 24;
    protected static final int PDU_COLUMN_MESSAGE_TYPE = 13;
    protected static final int PDU_COLUMN_MMS_VERSION = 14;
    protected static final int PDU_COLUMN_PRIORITY = 15;
    protected static final int PDU_COLUMN_READ_REPORT = 16;
    protected static final int PDU_COLUMN_READ_STATUS = 17;
    protected static final int PDU_COLUMN_REPORT_ALLOWED = 18;
    protected static final int PDU_COLUMN_RESPONSE_TEXT = 9;
    protected static final int PDU_COLUMN_RETRIEVE_STATUS = 19;
    protected static final int PDU_COLUMN_RETRIEVE_TEXT = 3;
    protected static final int PDU_COLUMN_RETRIEVE_TEXT_CHARSET = 26;
    protected static final int PDU_COLUMN_STATUS = 20;
    protected static final int PDU_COLUMN_SUBJECT = 4;
    protected static final int PDU_COLUMN_SUBJECT_CHARSET = 25;
    protected static final int PDU_COLUMN_THREAD_ID = 2;
    protected static final int PDU_COLUMN_TRANSACTION_ID = 10;
    public static final int PROC_STATUS_COMPLETED = 3;
    public static final int PROC_STATUS_PERMANENTLY_FAILURE = 2;
    public static final int PROC_STATUS_TRANSIENT_FAILURE = 1;
    private static final String TAG = "PduPersister";
    public static final String TEMPORARY_DRM_OBJECT_URI = "content://mms/9223372036854775807/part";
    protected static final HashMap<Integer, Integer> TEXT_STRING_COLUMN_INDEX_MAP;
    protected static final HashMap<Integer, String> TEXT_STRING_COLUMN_NAME_MAP;
    protected static PduPersister sPersister;
    protected final ContentResolver mContentResolver;
    public final Context mContext;
    private final DrmManagerClient mDrmManagerClient;
    protected final TelephonyManager mTelephonyManager;
    protected static final int[] ADDRESS_FIELDS = {129, 130, 137, 151};
    private static final String[] PDU_PROJECTION = {HbpcdLookup.ID, "msg_box", "thread_id", "retr_txt", "sub", "ct_l", "ct_t", "m_cls", "m_id", "resp_txt", "tr_id", "ct_cls", "d_rpt", "m_type", "v", "pri", "rr", "read_status", "rpt_a", "retr_st", "st", "date", "d_tm", "exp", "m_size", "sub_cs", "retr_txt_cs"};
    protected static final String[] PART_PROJECTION = {HbpcdLookup.ID, "chset", "cd", "cid", "cl", "ct", "fn", "name", "text"};
    protected static final HashMap<Uri, Integer> MESSAGE_BOX_MAP = new HashMap<>();

    static {
        MESSAGE_BOX_MAP.put(Telephony.Mms.Inbox.CONTENT_URI, 1);
        MESSAGE_BOX_MAP.put(Telephony.Mms.Sent.CONTENT_URI, 2);
        MESSAGE_BOX_MAP.put(Telephony.Mms.Draft.CONTENT_URI, 3);
        MESSAGE_BOX_MAP.put(Telephony.Mms.Outbox.CONTENT_URI, 4);
        CHARSET_COLUMN_INDEX_MAP = new HashMap<>();
        CHARSET_COLUMN_INDEX_MAP.put(150, 25);
        CHARSET_COLUMN_INDEX_MAP.put(154, 26);
        CHARSET_COLUMN_NAME_MAP = new HashMap<>();
        CHARSET_COLUMN_NAME_MAP.put(150, "sub_cs");
        CHARSET_COLUMN_NAME_MAP.put(154, "retr_txt_cs");
        ENCODED_STRING_COLUMN_INDEX_MAP = new HashMap<>();
        ENCODED_STRING_COLUMN_INDEX_MAP.put(154, 3);
        ENCODED_STRING_COLUMN_INDEX_MAP.put(150, 4);
        ENCODED_STRING_COLUMN_NAME_MAP = new HashMap<>();
        ENCODED_STRING_COLUMN_NAME_MAP.put(154, "retr_txt");
        ENCODED_STRING_COLUMN_NAME_MAP.put(150, "sub");
        TEXT_STRING_COLUMN_INDEX_MAP = new HashMap<>();
        TEXT_STRING_COLUMN_INDEX_MAP.put(131, 5);
        TEXT_STRING_COLUMN_INDEX_MAP.put(132, 6);
        TEXT_STRING_COLUMN_INDEX_MAP.put(138, 7);
        TEXT_STRING_COLUMN_INDEX_MAP.put(139, 8);
        TEXT_STRING_COLUMN_INDEX_MAP.put(147, 9);
        TEXT_STRING_COLUMN_INDEX_MAP.put(152, 10);
        TEXT_STRING_COLUMN_NAME_MAP = new HashMap<>();
        TEXT_STRING_COLUMN_NAME_MAP.put(131, "ct_l");
        TEXT_STRING_COLUMN_NAME_MAP.put(132, "ct_t");
        TEXT_STRING_COLUMN_NAME_MAP.put(138, "m_cls");
        TEXT_STRING_COLUMN_NAME_MAP.put(139, "m_id");
        TEXT_STRING_COLUMN_NAME_MAP.put(147, "resp_txt");
        TEXT_STRING_COLUMN_NAME_MAP.put(152, "tr_id");
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
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(PduHeaders.CONTENT_CLASS), "ct_cls");
        OCTET_COLUMN_NAME_MAP.put(134, "d_rpt");
        OCTET_COLUMN_NAME_MAP.put(140, "m_type");
        OCTET_COLUMN_NAME_MAP.put(141, "v");
        OCTET_COLUMN_NAME_MAP.put(143, "pri");
        OCTET_COLUMN_NAME_MAP.put(144, "rr");
        OCTET_COLUMN_NAME_MAP.put(155, "read_status");
        OCTET_COLUMN_NAME_MAP.put(145, "rpt_a");
        OCTET_COLUMN_NAME_MAP.put(153, "retr_st");
        OCTET_COLUMN_NAME_MAP.put(149, "st");
        LONG_COLUMN_INDEX_MAP = new HashMap<>();
        LONG_COLUMN_INDEX_MAP.put(133, 21);
        LONG_COLUMN_INDEX_MAP.put(135, 22);
        LONG_COLUMN_INDEX_MAP.put(136, 23);
        LONG_COLUMN_INDEX_MAP.put(142, 24);
        LONG_COLUMN_NAME_MAP = new HashMap<>();
        LONG_COLUMN_NAME_MAP.put(133, "date");
        LONG_COLUMN_NAME_MAP.put(135, "d_tm");
        LONG_COLUMN_NAME_MAP.put(136, "exp");
        LONG_COLUMN_NAME_MAP.put(142, "m_size");
        PDU_CACHE_INSTANCE = PduCache.getInstance();
    }

    protected PduPersister(Context context) {
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

    protected void setEncodedStringValueToHeaders(Cursor cursor, int i, PduHeaders pduHeaders, int i2) {
        String string = cursor.getString(i);
        if (string != null && string.length() > 0) {
            pduHeaders.setEncodedStringValue(new EncodedStringValue(cursor.getInt(CHARSET_COLUMN_INDEX_MAP.get(Integer.valueOf(i2)).intValue()), getBytes(string)), i2);
        }
    }

    protected void setTextStringToHeaders(Cursor cursor, int i, PduHeaders pduHeaders, int i2) {
        String string = cursor.getString(i);
        if (string != null) {
            pduHeaders.setTextString(getBytes(string), i2);
        }
    }

    protected void setOctetToHeaders(Cursor cursor, int i, PduHeaders pduHeaders, int i2) throws InvalidHeaderValueException {
        if (!cursor.isNull(i)) {
            pduHeaders.setOctet(cursor.getInt(i), i2);
        }
    }

    protected void setLongToHeaders(Cursor cursor, int i, PduHeaders pduHeaders, int i2) {
        if (!cursor.isNull(i)) {
            pduHeaders.setLongInteger(cursor.getLong(i), i2);
        }
    }

    protected Integer getIntegerFromPartColumn(Cursor cursor, int i) {
        if (!cursor.isNull(i)) {
            return Integer.valueOf(cursor.getInt(i));
        }
        return null;
    }

    protected byte[] getByteArrayFromPartColumn(Cursor cursor, int i) {
        if (!cursor.isNull(i)) {
            return getBytes(cursor.getString(i));
        }
        return null;
    }

    protected PduPart[] loadParts(long j) throws MmsException {
        InputStream inputStreamOpenInputStream;
        Throwable th;
        IOException e;
        Cursor cursorQuery = SqliteWrapper.query(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + j + "/part"), PART_PROJECTION, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() != 0) {
                    PduPart[] pduPartArr = new PduPart[cursorQuery.getCount()];
                    int i = 0;
                    while (cursorQuery.moveToNext()) {
                        PduPart pduPart = new PduPart();
                        Integer integerFromPartColumn = getIntegerFromPartColumn(cursorQuery, 1);
                        if (integerFromPartColumn != null) {
                            pduPart.setCharset(integerFromPartColumn.intValue());
                        }
                        byte[] byteArrayFromPartColumn = getByteArrayFromPartColumn(cursorQuery, 2);
                        if (byteArrayFromPartColumn != null) {
                            pduPart.setContentDisposition(byteArrayFromPartColumn);
                        }
                        byte[] byteArrayFromPartColumn2 = getByteArrayFromPartColumn(cursorQuery, 3);
                        if (byteArrayFromPartColumn2 != null) {
                            pduPart.setContentId(byteArrayFromPartColumn2);
                        }
                        byte[] byteArrayFromPartColumn3 = getByteArrayFromPartColumn(cursorQuery, 4);
                        if (byteArrayFromPartColumn3 != null) {
                            pduPart.setContentLocation(byteArrayFromPartColumn3);
                        }
                        byte[] byteArrayFromPartColumn4 = getByteArrayFromPartColumn(cursorQuery, 5);
                        if (byteArrayFromPartColumn4 != null) {
                            pduPart.setContentType(byteArrayFromPartColumn4);
                            byte[] byteArrayFromPartColumn5 = getByteArrayFromPartColumn(cursorQuery, 6);
                            if (byteArrayFromPartColumn5 != null) {
                                pduPart.setFilename(byteArrayFromPartColumn5);
                            }
                            byte[] byteArrayFromPartColumn6 = getByteArrayFromPartColumn(cursorQuery, 7);
                            if (byteArrayFromPartColumn6 != null) {
                                pduPart.setName(byteArrayFromPartColumn6);
                            }
                            Uri uri = Uri.parse("content://mms/part/" + cursorQuery.getLong(0));
                            pduPart.setDataUri(uri);
                            String isoString = toIsoString(byteArrayFromPartColumn4);
                            if (!ContentType.isImageType(isoString) && !ContentType.isAudioType(isoString) && !ContentType.isVideoType(isoString)) {
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                if (ContentType.TEXT_PLAIN.equals(isoString) || ContentType.APP_SMIL.equals(isoString) || ContentType.TEXT_HTML.equals(isoString)) {
                                    String string = cursorQuery.getString(8);
                                    if (string == null) {
                                        string = "";
                                    }
                                    byte[] textString = new EncodedStringValue(string).getTextString();
                                    byteArrayOutputStream.write(textString, 0, textString.length);
                                } else {
                                    try {
                                        inputStreamOpenInputStream = this.mContentResolver.openInputStream(uri);
                                        try {
                                            try {
                                                byte[] bArr = new byte[256];
                                                for (int i2 = inputStreamOpenInputStream.read(bArr); i2 >= 0; i2 = inputStreamOpenInputStream.read(bArr)) {
                                                    byteArrayOutputStream.write(bArr, 0, i2);
                                                }
                                                if (inputStreamOpenInputStream != null) {
                                                    try {
                                                        inputStreamOpenInputStream.close();
                                                    } catch (IOException e2) {
                                                        Log.e(TAG, "Failed to close stream", e2);
                                                    }
                                                }
                                            } catch (IOException e3) {
                                                e = e3;
                                                Log.e(TAG, "Failed to load part data", e);
                                                cursorQuery.close();
                                                throw new MmsException(e);
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                            if (inputStreamOpenInputStream != null) {
                                                try {
                                                    inputStreamOpenInputStream.close();
                                                } catch (IOException e4) {
                                                    Log.e(TAG, "Failed to close stream", e4);
                                                }
                                            }
                                            throw th;
                                        }
                                    } catch (IOException e5) {
                                        inputStreamOpenInputStream = null;
                                        e = e5;
                                    } catch (Throwable th3) {
                                        inputStreamOpenInputStream = null;
                                        th = th3;
                                        if (inputStreamOpenInputStream != null) {
                                        }
                                        throw th;
                                    }
                                }
                                pduPart.setData(byteArrayOutputStream.toByteArray());
                            }
                            pduPartArr[i] = pduPart;
                            i++;
                        } else {
                            throw new MmsException("Content-Type must be set.");
                        }
                    }
                    return pduPartArr;
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
        return null;
    }

    protected void loadAddress(long j, PduHeaders pduHeaders) {
        Cursor cursorQuery = SqliteWrapper.query(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + j + "/addr"), new String[]{"address", "charset", "type"}, null, null, null);
        if (cursorQuery != null) {
            while (cursorQuery.moveToNext()) {
                try {
                    String string = cursorQuery.getString(0);
                    if (!TextUtils.isEmpty(string)) {
                        int i = cursorQuery.getInt(2);
                        if (i == 137) {
                            pduHeaders.setEncodedStringValue(new EncodedStringValue(cursorQuery.getInt(1), getBytes(string)), i);
                        } else {
                            if (i != 151) {
                                switch (i) {
                                    case 129:
                                    case 130:
                                        break;
                                    default:
                                        Log.e(TAG, "Unknown address type: " + i);
                                        continue;
                                }
                            }
                            pduHeaders.appendEncodedStringValue(new EncodedStringValue(cursorQuery.getInt(1), getBytes(string)), i);
                        }
                    }
                } finally {
                    cursorQuery.close();
                }
            }
        }
    }

    public GenericPdu load(Uri uri) throws MmsException {
        PduPart[] pduPartArrLoadParts;
        GenericPdu sendReq;
        try {
            synchronized (PDU_CACHE_INSTANCE) {
                if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                    try {
                        PDU_CACHE_INSTANCE.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "load: ", e);
                    }
                    PduCacheEntry pduCacheEntry = PDU_CACHE_INSTANCE.get(uri);
                    if (pduCacheEntry != null) {
                        GenericPdu pdu = pduCacheEntry.getPdu();
                        synchronized (PDU_CACHE_INSTANCE) {
                            PDU_CACHE_INSTANCE.setUpdating(uri, false);
                            PDU_CACHE_INSTANCE.notifyAll();
                        }
                        return pdu;
                    }
                }
                PDU_CACHE_INSTANCE.setUpdating(uri, true);
                Cursor cursorQuery = SqliteWrapper.query(this.mContext, this.mContentResolver, uri, PDU_PROJECTION, null, null, null);
                PduHeaders pduHeaders = new PduHeaders();
                long id = ContentUris.parseId(uri);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.getCount() == 1 && cursorQuery.moveToFirst()) {
                            int i = cursorQuery.getInt(1);
                            long j = cursorQuery.getLong(2);
                            for (Map.Entry<Integer, Integer> entry : ENCODED_STRING_COLUMN_INDEX_MAP.entrySet()) {
                                setEncodedStringValueToHeaders(cursorQuery, entry.getValue().intValue(), pduHeaders, entry.getKey().intValue());
                            }
                            for (Map.Entry<Integer, Integer> entry2 : TEXT_STRING_COLUMN_INDEX_MAP.entrySet()) {
                                setTextStringToHeaders(cursorQuery, entry2.getValue().intValue(), pduHeaders, entry2.getKey().intValue());
                            }
                            for (Map.Entry<Integer, Integer> entry3 : OCTET_COLUMN_INDEX_MAP.entrySet()) {
                                setOctetToHeaders(cursorQuery, entry3.getValue().intValue(), pduHeaders, entry3.getKey().intValue());
                            }
                            for (Map.Entry<Integer, Integer> entry4 : LONG_COLUMN_INDEX_MAP.entrySet()) {
                                setLongToHeaders(cursorQuery, entry4.getValue().intValue(), pduHeaders, entry4.getKey().intValue());
                            }
                            if (id == -1) {
                                throw new MmsException("Error! ID of the message: -1.");
                            }
                            loadAddress(id, pduHeaders);
                            int octet = pduHeaders.getOctet(140);
                            PduBody pduBody = new PduBody();
                            if ((octet == 132 || octet == 128) && (pduPartArrLoadParts = loadParts(id)) != null) {
                                for (PduPart pduPart : pduPartArrLoadParts) {
                                    pduBody.addPart(pduPart);
                                }
                            }
                            switch (octet) {
                                case 128:
                                    sendReq = new SendReq(pduHeaders, pduBody);
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
                                    throw new MmsException("Unsupported PDU type: " + Integer.toHexString(octet));
                                case 130:
                                    sendReq = new NotificationInd(pduHeaders);
                                    break;
                                case 131:
                                    sendReq = new NotifyRespInd(pduHeaders);
                                    break;
                                case 132:
                                    sendReq = new RetrieveConf(pduHeaders, pduBody);
                                    break;
                                case 133:
                                    sendReq = new AcknowledgeInd(pduHeaders);
                                    break;
                                case 134:
                                    sendReq = new DeliveryInd(pduHeaders);
                                    break;
                                case 135:
                                    sendReq = new ReadRecInd(pduHeaders);
                                    break;
                                case 136:
                                    sendReq = new ReadOrigInd(pduHeaders);
                                    break;
                                default:
                                    throw new MmsException("Unrecognized PDU type: " + Integer.toHexString(octet));
                            }
                            synchronized (PDU_CACHE_INSTANCE) {
                                PDU_CACHE_INSTANCE.put(uri, new PduCacheEntry(sendReq, i, j));
                                PDU_CACHE_INSTANCE.setUpdating(uri, false);
                                PDU_CACHE_INSTANCE.notifyAll();
                            }
                            return sendReq;
                        }
                    } finally {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    }
                }
                throw new MmsException("Bad uri: " + uri);
            }
        } catch (Throwable th) {
            synchronized (PDU_CACHE_INSTANCE) {
                PDU_CACHE_INSTANCE.setUpdating(uri, false);
                PDU_CACHE_INSTANCE.notifyAll();
                throw th;
            }
        }
    }

    protected void persistAddress(long j, int i, EncodedStringValue[] encodedStringValueArr) {
        ContentValues contentValues = new ContentValues(3);
        for (EncodedStringValue encodedStringValue : encodedStringValueArr) {
            contentValues.clear();
            contentValues.put("address", toIsoString(encodedStringValue.getTextString()));
            contentValues.put("charset", Integer.valueOf(encodedStringValue.getCharacterSet()));
            contentValues.put("type", Integer.valueOf(i));
            SqliteWrapper.insert(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + j + "/addr"), contentValues);
        }
    }

    protected static String getPartContentType(PduPart pduPart) {
        if (pduPart.getContentType() == null) {
            return null;
        }
        return toIsoString(pduPart.getContentType());
    }

    public Uri persistPart(PduPart pduPart, long j, HashMap<Uri, InputStream> map) throws Throwable {
        Uri uri = Uri.parse("content://mms/" + j + "/part");
        ContentValues contentValues = new ContentValues(8);
        int charset = pduPart.getCharset();
        if (charset != 0) {
            contentValues.put("chset", Integer.valueOf(charset));
        }
        String partContentType = getPartContentType(pduPart);
        if (partContentType != null) {
            if (ContentType.IMAGE_JPG.equals(partContentType)) {
                partContentType = ContentType.IMAGE_JPEG;
            }
            contentValues.put("ct", partContentType);
            if (ContentType.APP_SMIL.equals(partContentType)) {
                contentValues.put("seq", (Integer) (-1));
            }
            if (pduPart.getFilename() != null) {
                contentValues.put("fn", new String(pduPart.getFilename()));
            }
            if (pduPart.getName() != null) {
                contentValues.put("name", new String(pduPart.getName()));
            }
            if (pduPart.getContentDisposition() != null) {
                contentValues.put("cd", toIsoString(pduPart.getContentDisposition()));
            }
            if (pduPart.getContentId() != null) {
                contentValues.put("cid", toIsoString(pduPart.getContentId()));
            }
            if (pduPart.getContentLocation() != null) {
                contentValues.put("cl", toIsoString(pduPart.getContentLocation()));
            }
            Uri uriInsert = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, contentValues);
            if (uriInsert == null) {
                throw new MmsException("Failed to persist part, return null.");
            }
            persistData(pduPart, uriInsert, partContentType, map);
            pduPart.setDataUri(uriInsert);
            return uriInsert;
        }
        throw new MmsException("MIME type of the part must be set.");
    }

    protected void persistData(PduPart pduPart, Uri uri, String str, HashMap<Uri, InputStream> map) throws Throwable {
        String strConvertUriToPath;
        DrmConvertSession drmConvertSessionOpen;
        OutputStream outputStreamOpenOutputStream;
        OutputStream outputStream = null;
        outputStream = null;
        inputStreamOpenInputStream = null;
        inputStreamOpenInputStream = null;
        inputStreamOpenInputStream = null;
        inputStreamOpenInputStream = null;
        outputStream = null;
        InputStream inputStreamOpenInputStream = null;
        outputStream = null;
        try {
            try {
                byte[] data = pduPart.getData();
                if (ContentType.TEXT_PLAIN.equals(str) || ContentType.APP_SMIL.equals(str) || ContentType.TEXT_HTML.equals(str)) {
                    ContentValues contentValues = new ContentValues();
                    if (data == null) {
                        data = new String("").getBytes("utf-8");
                    }
                    contentValues.put("text", new EncodedStringValue(data).getString());
                    if (this.mContentResolver.update(uri, contentValues, null, null) != 1) {
                        throw new MmsException("unable to update " + uri.toString());
                    }
                    outputStreamOpenOutputStream = null;
                    strConvertUriToPath = null;
                    drmConvertSessionOpen = null;
                } else {
                    boolean zIsDrmConvertNeeded = DownloadDrmHelper.isDrmConvertNeeded(str);
                    try {
                        if (zIsDrmConvertNeeded) {
                            if (uri != 0) {
                                try {
                                    strConvertUriToPath = convertUriToPath(this.mContext, uri);
                                    try {
                                        try {
                                            if (new File(strConvertUriToPath).length() > 0) {
                                                return;
                                            }
                                        } catch (Exception e) {
                                            e = e;
                                            Log.e(TAG, "Can't get file info for: " + pduPart.getDataUri(), e);
                                        }
                                    } catch (FileNotFoundException e2) {
                                        e = e2;
                                        Log.e(TAG, "Failed to open Input/Output stream.", e);
                                        throw new MmsException(e);
                                    } catch (IOException e3) {
                                        e = e3;
                                        Log.e(TAG, "Failed to read/write data.", e);
                                        throw new MmsException(e);
                                    } catch (Throwable th) {
                                        th = th;
                                        uri = 0;
                                        drmConvertSessionOpen = null;
                                        if (outputStream != null) {
                                            try {
                                                outputStream.close();
                                            } catch (IOException e4) {
                                                Log.e(TAG, "IOException while closing: " + outputStream, e4);
                                            }
                                        }
                                        if (uri != 0) {
                                            try {
                                                uri.close();
                                            } catch (IOException e5) {
                                                Log.e(TAG, "IOException while closing: " + uri, e5);
                                            }
                                        }
                                        if (drmConvertSessionOpen != null) {
                                            throw th;
                                        }
                                        drmConvertSessionOpen.close(strConvertUriToPath);
                                        File file = new File(strConvertUriToPath);
                                        ContentValues contentValues2 = new ContentValues(0);
                                        SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + file.getName()), contentValues2, null, null);
                                        throw th;
                                    }
                                } catch (Exception e6) {
                                    e = e6;
                                    strConvertUriToPath = null;
                                }
                            } else {
                                strConvertUriToPath = null;
                            }
                            drmConvertSessionOpen = DrmConvertSession.open(this.mContext, str);
                            if (drmConvertSessionOpen == null) {
                                throw new MmsException("Mimetype " + str + " can not be converted.");
                            }
                        } else {
                            strConvertUriToPath = null;
                            drmConvertSessionOpen = null;
                        }
                        outputStreamOpenOutputStream = this.mContentResolver.openOutputStream(uri);
                        try {
                            if (data == null) {
                                Uri dataUri = pduPart.getDataUri();
                                if (dataUri != null && !dataUri.equals(uri)) {
                                    if (map != null && map.containsKey(dataUri)) {
                                        inputStreamOpenInputStream = map.get(dataUri);
                                    }
                                    if (inputStreamOpenInputStream == null) {
                                        inputStreamOpenInputStream = this.mContentResolver.openInputStream(dataUri);
                                    }
                                    byte[] bArr = new byte[RadioAccessFamily.EHRPD];
                                    while (true) {
                                        int i = inputStreamOpenInputStream.read(bArr);
                                        if (i == -1) {
                                            break;
                                        }
                                        if (zIsDrmConvertNeeded) {
                                            byte[] bArrConvert = drmConvertSessionOpen.convert(bArr, i);
                                            if (bArrConvert == null) {
                                                throw new MmsException("Error converting drm data.");
                                            }
                                            outputStreamOpenOutputStream.write(bArrConvert, 0, bArrConvert.length);
                                        } else {
                                            outputStreamOpenOutputStream.write(bArr, 0, i);
                                        }
                                    }
                                }
                                Log.w(TAG, "Can't find data for this part.");
                                if (outputStreamOpenOutputStream != null) {
                                    try {
                                        outputStreamOpenOutputStream.close();
                                    } catch (IOException e7) {
                                        Log.e(TAG, "IOException while closing: " + outputStreamOpenOutputStream, e7);
                                    }
                                }
                                if (drmConvertSessionOpen != null) {
                                    drmConvertSessionOpen.close(strConvertUriToPath);
                                    File file2 = new File(strConvertUriToPath);
                                    ContentValues contentValues3 = new ContentValues(0);
                                    SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + file2.getName()), contentValues3, null, null);
                                    return;
                                }
                                return;
                            }
                            if (zIsDrmConvertNeeded) {
                                byte[] bArrConvert2 = drmConvertSessionOpen.convert(data, data.length);
                                if (bArrConvert2 == null) {
                                    throw new MmsException("Error converting drm data.");
                                }
                                outputStreamOpenOutputStream.write(bArrConvert2, 0, bArrConvert2.length);
                            } else {
                                outputStreamOpenOutputStream.write(data);
                            }
                        } catch (FileNotFoundException e8) {
                            e = e8;
                            Log.e(TAG, "Failed to open Input/Output stream.", e);
                            throw new MmsException(e);
                        } catch (IOException e9) {
                            e = e9;
                            Log.e(TAG, "Failed to read/write data.", e);
                            throw new MmsException(e);
                        } catch (Throwable th2) {
                            th = th2;
                            uri = 0;
                            outputStream = outputStreamOpenOutputStream;
                            if (outputStream != null) {
                            }
                            if (uri != 0) {
                            }
                            if (drmConvertSessionOpen != null) {
                            }
                        }
                    } catch (FileNotFoundException e10) {
                        e = e10;
                    } catch (IOException e11) {
                        e = e11;
                    } catch (Throwable th3) {
                        th = th3;
                        uri = 0;
                    }
                }
                if (outputStreamOpenOutputStream != null) {
                    try {
                        outputStreamOpenOutputStream.close();
                    } catch (IOException e12) {
                        Log.e(TAG, "IOException while closing: " + outputStreamOpenOutputStream, e12);
                    }
                }
                if (inputStreamOpenInputStream != null) {
                    try {
                        inputStreamOpenInputStream.close();
                    } catch (IOException e13) {
                        Log.e(TAG, "IOException while closing: " + inputStreamOpenInputStream, e13);
                    }
                }
                if (drmConvertSessionOpen != null) {
                    drmConvertSessionOpen.close(strConvertUriToPath);
                    File file3 = new File(strConvertUriToPath);
                    ContentValues contentValues4 = new ContentValues(0);
                    SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + file3.getName()), contentValues4, null, null);
                }
            } catch (Throwable th4) {
                th = th4;
            }
        } catch (FileNotFoundException e14) {
            e = e14;
        } catch (IOException e15) {
            e = e15;
        } catch (Throwable th5) {
            th = th5;
            uri = 0;
            strConvertUriToPath = null;
            drmConvertSessionOpen = null;
        }
    }

    public static String convertUriToPath(Context context, Uri uri) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor = null;
        if (uri == null) {
            return null;
        }
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("") || scheme.equals("file")) {
            return uri.getPath();
        }
        if (scheme.equals("http")) {
            return uri.toString();
        }
        if (!scheme.equals("content")) {
            throw new IllegalArgumentException("Given Uri scheme is not supported");
        }
        try {
            try {
                cursorQuery = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.getCount() != 0 && cursorQuery.moveToFirst()) {
                            String string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("_data"));
                            if (cursorQuery == null) {
                                return string;
                            }
                            cursorQuery.close();
                            return string;
                        }
                    } catch (SQLiteException e) {
                        cursor = cursorQuery;
                        throw new IllegalArgumentException("Given Uri is not formatted in a way so that it can be found in media store.");
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
                throw new IllegalArgumentException("Given Uri could not be found in media store");
            } catch (SQLiteException e2) {
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = cursor;
        }
    }

    protected void updateAddress(long j, int i, EncodedStringValue[] encodedStringValueArr) {
        Context context = this.mContext;
        ContentResolver contentResolver = this.mContentResolver;
        Uri uri = Uri.parse("content://mms/" + j + "/addr");
        StringBuilder sb = new StringBuilder();
        sb.append("type=");
        sb.append(i);
        SqliteWrapper.delete(context, contentResolver, uri, sb.toString(), null);
        persistAddress(j, i, encodedStringValueArr);
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
        ContentValues contentValues = new ContentValues(10);
        byte[] contentType = sendReq.getContentType();
        if (contentType != null) {
            contentValues.put("ct_t", toIsoString(contentType));
        }
        long date = sendReq.getDate();
        if (date != -1) {
            contentValues.put("date", Long.valueOf(date));
        }
        int deliveryReport = sendReq.getDeliveryReport();
        if (deliveryReport != 0) {
            contentValues.put("d_rpt", Integer.valueOf(deliveryReport));
        }
        long expiry = sendReq.getExpiry();
        if (expiry != -1) {
            contentValues.put("exp", Long.valueOf(expiry));
        }
        byte[] messageClass = sendReq.getMessageClass();
        if (messageClass != null) {
            contentValues.put("m_cls", toIsoString(messageClass));
        }
        int priority = sendReq.getPriority();
        if (priority != 0) {
            contentValues.put("pri", Integer.valueOf(priority));
        }
        int readReport = sendReq.getReadReport();
        if (readReport != 0) {
            contentValues.put("rr", Integer.valueOf(readReport));
        }
        byte[] transactionId = sendReq.getTransactionId();
        if (transactionId != null) {
            contentValues.put("tr_id", toIsoString(transactionId));
        }
        EncodedStringValue subject = sendReq.getSubject();
        if (subject != null) {
            contentValues.put("sub", toIsoString(subject.getTextString()));
            contentValues.put("sub_cs", Integer.valueOf(subject.getCharacterSet()));
        } else {
            contentValues.put("sub", "");
        }
        long messageSize = sendReq.getMessageSize();
        if (messageSize > 0) {
            contentValues.put("m_size", Long.valueOf(messageSize));
        }
        PduHeaders pduHeaders = sendReq.getPduHeaders();
        HashSet hashSet = new HashSet();
        for (int i : ADDRESS_FIELDS) {
            EncodedStringValue[] encodedStringValues = null;
            if (i == 137) {
                EncodedStringValue encodedStringValue = pduHeaders.getEncodedStringValue(i);
                if (encodedStringValue != null) {
                    encodedStringValues = new EncodedStringValue[]{encodedStringValue};
                }
            } else {
                encodedStringValues = pduHeaders.getEncodedStringValues(i);
            }
            if (encodedStringValues != null) {
                updateAddress(ContentUris.parseId(uri), i, encodedStringValues);
                if (i == 151) {
                    for (EncodedStringValue encodedStringValue2 : encodedStringValues) {
                        if (encodedStringValue2 != null) {
                            hashSet.add(encodedStringValue2.getString());
                        }
                    }
                }
            }
        }
        if (!hashSet.isEmpty()) {
            contentValues.put("thread_id", Long.valueOf(Telephony.Threads.getOrCreateThreadId(this.mContext, hashSet)));
        }
        SqliteWrapper.update(this.mContext, this.mContentResolver, uri, contentValues, null, null);
    }

    protected void updatePart(Uri uri, PduPart pduPart, HashMap<Uri, InputStream> map) throws Throwable {
        ContentValues contentValues = new ContentValues(7);
        int charset = pduPart.getCharset();
        if (charset != 0) {
            contentValues.put("chset", Integer.valueOf(charset));
        }
        if (pduPart.getContentType() != null) {
            String isoString = toIsoString(pduPart.getContentType());
            contentValues.put("ct", isoString);
            if (pduPart.getFilename() != null) {
                contentValues.put("fn", new String(pduPart.getFilename()));
            }
            if (pduPart.getName() != null) {
                contentValues.put("name", new String(pduPart.getName()));
            }
            if (pduPart.getContentDisposition() != null) {
                contentValues.put("cd", toIsoString(pduPart.getContentDisposition()));
            }
            if (pduPart.getContentId() != null) {
                contentValues.put("cid", toIsoString(pduPart.getContentId()));
            }
            if (pduPart.getContentLocation() != null) {
                contentValues.put("cl", toIsoString(pduPart.getContentLocation()));
            }
            SqliteWrapper.update(this.mContext, this.mContentResolver, uri, contentValues, null, null);
            if (pduPart.getData() != null || !uri.equals(pduPart.getDataUri())) {
                persistData(pduPart, uri, isoString, map);
                return;
            }
            return;
        }
        throw new MmsException("MIME type of the part must be set.");
    }

    public void updateParts(Uri uri, PduBody pduBody, HashMap<Uri, InputStream> map) throws MmsException {
        try {
            synchronized (PDU_CACHE_INSTANCE) {
                if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                    try {
                        PDU_CACHE_INSTANCE.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "updateParts: ", e);
                    }
                    PduCacheEntry pduCacheEntry = PDU_CACHE_INSTANCE.get(uri);
                    if (pduCacheEntry != null) {
                        ((MultimediaMessagePdu) pduCacheEntry.getPdu()).setBody(pduBody);
                    }
                    PDU_CACHE_INSTANCE.setUpdating(uri, true);
                } else {
                    PDU_CACHE_INSTANCE.setUpdating(uri, true);
                }
            }
            ArrayList arrayList = new ArrayList();
            HashMap map2 = new HashMap();
            int partsNum = pduBody.getPartsNum();
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            for (int i = 0; i < partsNum; i++) {
                PduPart part = pduBody.getPart(i);
                Uri dataUri = part.getDataUri();
                if (dataUri == null || TextUtils.isEmpty(dataUri.getAuthority()) || !dataUri.getAuthority().startsWith("mms")) {
                    arrayList.add(part);
                } else {
                    map2.put(dataUri, part);
                    if (sb.length() > 1) {
                        sb.append(" AND ");
                    }
                    sb.append(HbpcdLookup.ID);
                    sb.append("!=");
                    DatabaseUtils.appendEscapedSQLString(sb, dataUri.getLastPathSegment());
                }
            }
            sb.append(')');
            long id = ContentUris.parseId(uri);
            SqliteWrapper.delete(this.mContext, this.mContentResolver, Uri.parse(Telephony.Mms.CONTENT_URI + "/" + id + "/part"), sb.length() > 2 ? sb.toString() : null, null);
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                persistPart((PduPart) it.next(), id, map);
            }
            for (Map.Entry entry : map2.entrySet()) {
                updatePart((Uri) entry.getKey(), (PduPart) entry.getValue(), map);
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

    public Uri persist(GenericPdu genericPdu, Uri uri, boolean z, boolean z2, HashMap<Uri, InputStream> map) throws Throwable {
        long id;
        int i;
        int i2;
        int dataLength;
        int i3;
        Uri uriInsert;
        PduBody body;
        PduBody pduBody;
        if (uri == null) {
            throw new MmsException("Uri may not be null.");
        }
        try {
            id = ContentUris.parseId(uri);
        } catch (NumberFormatException e) {
            id = -1;
        }
        boolean z3 = id != -1;
        if (!z3 && MESSAGE_BOX_MAP.get(uri) == null) {
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
        PduHeaders pduHeaders = genericPdu.getPduHeaders();
        ContentValues contentValues = new ContentValues();
        for (Map.Entry<Integer, String> entry : ENCODED_STRING_COLUMN_NAME_MAP.entrySet()) {
            int iIntValue = entry.getKey().intValue();
            EncodedStringValue encodedStringValue = pduHeaders.getEncodedStringValue(iIntValue);
            if (encodedStringValue != null) {
                String str = CHARSET_COLUMN_NAME_MAP.get(Integer.valueOf(iIntValue));
                contentValues.put(entry.getValue(), toIsoString(encodedStringValue.getTextString()));
                contentValues.put(str, Integer.valueOf(encodedStringValue.getCharacterSet()));
            }
        }
        for (Map.Entry<Integer, String> entry2 : TEXT_STRING_COLUMN_NAME_MAP.entrySet()) {
            byte[] textString = pduHeaders.getTextString(entry2.getKey().intValue());
            if (textString != null) {
                contentValues.put(entry2.getValue(), toIsoString(textString));
            }
        }
        for (Map.Entry<Integer, String> entry3 : OCTET_COLUMN_NAME_MAP.entrySet()) {
            int octet = pduHeaders.getOctet(entry3.getKey().intValue());
            if (octet != 0) {
                contentValues.put(entry3.getValue(), Integer.valueOf(octet));
            }
        }
        for (Map.Entry<Integer, String> entry4 : LONG_COLUMN_NAME_MAP.entrySet()) {
            long longInteger = pduHeaders.getLongInteger(entry4.getKey().intValue());
            if (longInteger != -1) {
                contentValues.put(entry4.getValue(), Long.valueOf(longInteger));
            }
        }
        HashMap<Integer, EncodedStringValue[]> map2 = new HashMap<>(ADDRESS_FIELDS.length);
        for (int i4 : ADDRESS_FIELDS) {
            EncodedStringValue[] encodedStringValues = null;
            if (i4 == 137) {
                EncodedStringValue encodedStringValue2 = pduHeaders.getEncodedStringValue(i4);
                if (encodedStringValue2 != null) {
                    encodedStringValues = new EncodedStringValue[]{encodedStringValue2};
                }
            } else {
                encodedStringValues = pduHeaders.getEncodedStringValues(i4);
            }
            map2.put(Integer.valueOf(i4), encodedStringValues);
        }
        HashSet<String> hashSet = new HashSet<>();
        int messageType = genericPdu.getMessageType();
        if (messageType == 130 || messageType == 132 || messageType == 128) {
            if (messageType == 128) {
                i = 0;
                loadRecipients(151, hashSet, map2, false);
            } else if (messageType == 130 || messageType == 132) {
                i = 0;
                loadRecipients(137, hashSet, map2, false);
                if (z2) {
                    loadRecipients(151, hashSet, map2, true);
                    loadRecipients(130, hashSet, map2, true);
                }
            } else {
                i = 0;
            }
            long orCreateThreadId = 0;
            if (z && !hashSet.isEmpty()) {
                orCreateThreadId = Telephony.Threads.getOrCreateThreadId(this.mContext, hashSet);
            }
            contentValues.put("thread_id", Long.valueOf(orCreateThreadId));
        } else {
            i = 0;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        if ((genericPdu instanceof MultimediaMessagePdu) && (body = ((MultimediaMessagePdu) genericPdu).getBody()) != null) {
            int partsNum = body.getPartsNum();
            i2 = partsNum > 2 ? i : 1;
            int i5 = i;
            dataLength = i5;
            while (i5 < partsNum) {
                PduPart part = body.getPart(i5);
                dataLength += part.getDataLength();
                persistPart(part, jCurrentTimeMillis, map);
                String partContentType = getPartContentType(part);
                if (partContentType != null) {
                    pduBody = body;
                    if (!ContentType.APP_SMIL.equals(partContentType) && !ContentType.TEXT_PLAIN.equals(partContentType)) {
                        i2 = 0;
                    }
                } else {
                    pduBody = body;
                }
                i5++;
                body = pduBody;
            }
        } else {
            i2 = 1;
            dataLength = 0;
        }
        contentValues.put("text_only", Integer.valueOf(i2));
        if (contentValues.getAsInteger("m_size") == null) {
            contentValues.put("m_size", Integer.valueOf(dataLength));
        }
        if (z3) {
            i3 = 0;
            SqliteWrapper.update(this.mContext, this.mContentResolver, uri, contentValues, null, null);
            uriInsert = uri;
        } else {
            i3 = 0;
            uriInsert = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, contentValues);
            if (uriInsert == null) {
                throw new MmsException("persist() failed: return null.");
            }
            id = ContentUris.parseId(uriInsert);
        }
        ContentValues contentValues2 = new ContentValues(1);
        contentValues2.put("mid", Long.valueOf(id));
        SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + jCurrentTimeMillis + "/part"), contentValues2, null, null);
        if (!z3) {
            uriInsert = Uri.parse(uri + "/" + id);
        }
        int[] iArr = ADDRESS_FIELDS;
        int length = iArr.length;
        while (i3 < length) {
            int i6 = iArr[i3];
            EncodedStringValue[] encodedStringValueArr = map2.get(Integer.valueOf(i6));
            if (encodedStringValueArr != null) {
                persistAddress(id, i6, encodedStringValueArr);
            }
            i3++;
        }
        return uriInsert;
    }

    protected void loadRecipients(int i, HashSet<String> hashSet, HashMap<Integer, EncodedStringValue[]> map, boolean z) {
        EncodedStringValue[] encodedStringValueArr = map.get(Integer.valueOf(i));
        if (encodedStringValueArr == null) {
            return;
        }
        if (z && encodedStringValueArr.length == 1) {
            return;
        }
        SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(this.mContext);
        HashSet hashSet2 = new HashSet();
        if (z) {
            for (int i2 : subscriptionManagerFrom.getActiveSubscriptionIdList()) {
                String line1Number = this.mTelephonyManager.getLine1Number(i2);
                if (line1Number != null) {
                    hashSet2.add(line1Number);
                }
            }
        }
        for (EncodedStringValue encodedStringValue : encodedStringValueArr) {
            if (encodedStringValue != null) {
                String string = encodedStringValue.getString();
                if (z) {
                    Iterator it = hashSet2.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        if (!PhoneNumberUtils.compare(string, (String) it.next()) && !hashSet.contains(string)) {
                            hashSet.add(string);
                            break;
                        }
                    }
                } else if (!hashSet.contains(string)) {
                    hashSet.add(string);
                }
            }
        }
    }

    public Uri move(Uri uri, Uri uri2) throws MmsException {
        long id = ContentUris.parseId(uri);
        if (id == -1) {
            throw new MmsException("Error! ID of the message: -1.");
        }
        Integer num = MESSAGE_BOX_MAP.get(uri2);
        if (num == null) {
            throw new MmsException("Bad destination, must be one of content://mms/inbox, content://mms/sent, content://mms/drafts, content://mms/outbox, content://mms/temp.");
        }
        ContentValues contentValues = new ContentValues(1);
        contentValues.put("msg_box", num);
        SqliteWrapper.update(this.mContext, this.mContentResolver, uri, contentValues, null, null);
        return ContentUris.withAppendedId(uri2, id);
    }

    public static String toIsoString(byte[] bArr) {
        try {
            return new String(bArr, CharacterSets.MIMENAME_ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "ISO_8859_1 must be supported!", e);
            return "";
        }
    }

    public static byte[] getBytes(String str) {
        try {
            return str.getBytes(CharacterSets.MIMENAME_ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "ISO_8859_1 must be supported!", e);
            return new byte[0];
        }
    }

    public void release() {
        SqliteWrapper.delete(this.mContext, this.mContentResolver, Uri.parse(TEMPORARY_DRM_OBJECT_URI), null, null);
    }

    public Cursor getPendingMessages(long j) {
        Uri.Builder builderBuildUpon = Telephony.MmsSms.PendingMessages.CONTENT_URI.buildUpon();
        builderBuildUpon.appendQueryParameter("protocol", "mms");
        return SqliteWrapper.query(this.mContext, this.mContentResolver, builderBuildUpon.build(), null, "err_type < ? AND due_time <= ?", new String[]{String.valueOf(10), String.valueOf(j)}, "due_time");
    }
}
