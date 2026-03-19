package com.mediatek.android.mms.pdu;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.hardware.radio.V1_0.RadioCdmaSmsConst;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.google.android.mms.ContentType;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.AcknowledgeInd;
import com.google.android.mms.pdu.DeliveryInd;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.NotifyRespInd;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.ReadOrigInd;
import com.google.android.mms.pdu.ReadRecInd;
import com.google.android.mms.util.PduCacheEntry;
import com.google.android.mms.util.SqliteWrapper;
import com.mediatek.internal.telephony.cat.BipUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import mediatek.telephony.MtkTelephony;

public class MtkPduPersister extends PduPersister {
    static final boolean $assertionsDisabled = false;
    private static final int PDU_COLUMN_DATE_SENT = 28;
    private static final int PDU_COLUMN_READ = 27;
    private static final String[] PDU_PROJECTION = {"_id", "msg_box", "thread_id", "retr_txt", "sub", "ct_l", "ct_t", "m_cls", "m_id", "resp_txt", "tr_id", "ct_cls", "d_rpt", "m_type", "v", "pri", "rr", "read_status", "rpt_a", "retr_st", "st", "date", "d_tm", "exp", "m_size", "sub_cs", "retr_txt_cs", "read", "date_sent"};
    private static final String TAG = "MtkPduPersister";
    protected static MtkPduPersister sPersister;
    private boolean mBackupRestore;

    protected MtkPduPersister(Context context) {
        super(context);
        this.mBackupRestore = false;
        if (LONG_COLUMN_INDEX_MAP != null) {
            LONG_COLUMN_INDEX_MAP.put(201, 28);
        }
        if (LONG_COLUMN_NAME_MAP != null) {
            LONG_COLUMN_NAME_MAP.put(201, "date_sent");
        }
    }

    public static MtkPduPersister getPduPersister(Context context) {
        if (sPersister == null) {
            sPersister = new MtkPduPersister(context);
        } else if (!context.equals(sPersister.mContext)) {
            sPersister.release();
            sPersister = new MtkPduPersister(context);
        }
        return sPersister;
    }

    private byte[] getByteArrayFromPartColumn2(Cursor cursor, int i) {
        if (!cursor.isNull(i)) {
            return cursor.getString(i).getBytes();
        }
        return null;
    }

    protected void persistAddress(long j, int i, EncodedStringValue[] encodedStringValueArr) {
        ArrayList arrayList = new ArrayList();
        ContentValues contentValues = new ContentValues();
        Uri uri = Uri.parse("content://mms/" + j + "/addr");
        if (encodedStringValueArr == null) {
            return;
        }
        for (EncodedStringValue encodedStringValue : encodedStringValueArr) {
            arrayList.add(toIsoString(encodedStringValue.getTextString()));
            arrayList.add(String.valueOf(encodedStringValue.getCharacterSet()));
            arrayList.add(String.valueOf(i));
        }
        contentValues.putStringArrayList("addresses", arrayList);
        SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, contentValues);
    }

    protected PduPart[] loadParts(long j) throws MmsException {
        byte[] textString;
        InputStream inputStreamOpenInputStream;
        Throwable th;
        IOException e;
        Cursor cursorQuery = SqliteWrapper.query(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + j + "/part"), PART_PROJECTION, (String) null, (String[]) null, (String) null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() != 0) {
                    MtkPduPart[] mtkPduPartArr = new MtkPduPart[cursorQuery.getCount()];
                    int i = 0;
                    while (cursorQuery.moveToNext()) {
                        MtkPduPart mtkPduPart = new MtkPduPart();
                        Integer integerFromPartColumn = getIntegerFromPartColumn(cursorQuery, 1);
                        if (integerFromPartColumn != null) {
                            mtkPduPart.setCharset(integerFromPartColumn.intValue());
                        }
                        byte[] byteArrayFromPartColumn = getByteArrayFromPartColumn(cursorQuery, 2);
                        if (byteArrayFromPartColumn != null) {
                            mtkPduPart.setContentDisposition(byteArrayFromPartColumn);
                        }
                        byte[] byteArrayFromPartColumn2 = getByteArrayFromPartColumn(cursorQuery, 3);
                        if (byteArrayFromPartColumn2 != null) {
                            mtkPduPart.setContentId(byteArrayFromPartColumn2);
                        }
                        byte[] byteArrayFromPartColumn3 = getByteArrayFromPartColumn(cursorQuery, 4);
                        if (byteArrayFromPartColumn3 != null) {
                            mtkPduPart.setContentLocation(byteArrayFromPartColumn3);
                        }
                        byte[] byteArrayFromPartColumn4 = getByteArrayFromPartColumn(cursorQuery, 5);
                        if (byteArrayFromPartColumn4 != null) {
                            mtkPduPart.setContentType(byteArrayFromPartColumn4);
                            byte[] byteArrayFromPartColumn22 = getByteArrayFromPartColumn2(cursorQuery, 6);
                            if (byteArrayFromPartColumn22 != null) {
                                mtkPduPart.setFilename(byteArrayFromPartColumn22);
                            }
                            byte[] byteArrayFromPartColumn23 = getByteArrayFromPartColumn2(cursorQuery, 7);
                            if (byteArrayFromPartColumn23 != null) {
                                mtkPduPart.setName(byteArrayFromPartColumn23);
                            }
                            Uri uri = Uri.parse("content://mms/part/" + cursorQuery.getLong(0));
                            mtkPduPart.setDataUri(uri);
                            String isoString = toIsoString(byteArrayFromPartColumn4);
                            if (!ContentType.isImageType(isoString) && !ContentType.isAudioType(isoString) && !ContentType.isVideoType(isoString)) {
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                if ("text/plain".equals(isoString) || "application/smil".equals(isoString) || "text/html".equals(isoString)) {
                                    String string = cursorQuery.getString(8);
                                    if (integerFromPartColumn != null) {
                                        int iIntValue = integerFromPartColumn.intValue();
                                        if (string == null) {
                                            string = "";
                                        }
                                        textString = new MtkEncodedStringValue(iIntValue, string).getTextString();
                                    } else {
                                        if (string == null) {
                                            string = "";
                                        }
                                        textString = new MtkEncodedStringValue(string).getTextString();
                                    }
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
                                mtkPduPart.setData(byteArrayOutputStream.toByteArray());
                            }
                            mtkPduPartArr[i] = mtkPduPart;
                            i++;
                        } else {
                            throw new MmsException("Content-Type must be set.");
                        }
                    }
                    return mtkPduPartArr;
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

    public Uri persistPart(PduPart pduPart, long j, HashMap<Uri, InputStream> map) throws MmsException {
        Uri uri = Uri.parse("content://mms/" + j + "/part");
        ContentValues contentValues = new ContentValues(8);
        String partContentType = getPartContentType(pduPart);
        if (partContentType != null) {
            if ("image/jpg".equals(partContentType)) {
                partContentType = "image/jpeg";
            }
            contentValues.put("ct", partContentType);
            if ("application/smil".equals(partContentType)) {
                contentValues.put("seq", (Integer) (-1));
            }
            int charset = pduPart.getCharset();
            if (charset != 0 && !"application/smil".equals(partContentType)) {
                contentValues.put("chset", Integer.valueOf(charset));
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
                contentValues.put(BipUtils.KEY_QOS_CID, toIsoString(pduPart.getContentId()));
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

    public Uri persist(GenericPdu genericPdu, Uri uri, boolean z, boolean z2) throws MmsException {
        return persist(genericPdu, uri, z, z2, null);
    }

    public Uri persist(GenericPdu genericPdu, Uri uri) throws MmsException {
        return persist(genericPdu, uri, true, false);
    }

    protected void loadRecipients(int i, HashSet<String> hashSet, HashMap<Integer, EncodedStringValue[]> map, boolean z) {
        EncodedStringValue[] encodedStringValueArr = map.get(Integer.valueOf(i));
        if (encodedStringValueArr == null) {
            return;
        }
        if (z && encodedStringValueArr.length == 1) {
            return;
        }
        int[] activeSubscriptionIdList = SubscriptionManager.from(this.mContext).getActiveSubscriptionIdList();
        for (EncodedStringValue encodedStringValue : encodedStringValueArr) {
            if (encodedStringValue != null) {
                String string = encodedStringValue.getString();
                Log.d(TAG, "length = " + activeSubscriptionIdList.length);
                if (activeSubscriptionIdList.length == 0) {
                    hashSet.add(string);
                } else {
                    for (int i2 : activeSubscriptionIdList) {
                        Log.d(TAG, "subid = " + i2);
                        String line1Number = z ? this.mTelephonyManager.getLine1Number(i2) : null;
                        if ((line1Number == null || !PhoneNumberUtils.compare(string, line1Number)) && !hashSet.contains(string)) {
                            hashSet.add(string);
                        }
                    }
                }
            }
        }
    }

    public Cursor getPendingMessages(long j) {
        Uri.Builder builderBuildUpon = Telephony.MmsSms.PendingMessages.CONTENT_URI.buildUpon();
        builderBuildUpon.appendQueryParameter("protocol", "mms");
        return SqliteWrapper.query(this.mContext, this.mContentResolver, builderBuildUpon.build(), (String[]) null, "err_type < ? AND due_time <= ? AND msg_id in ( SELECT msg_id FROM pdu  WHERE msg_box <>2  AND m_type <>132  )", new String[]{String.valueOf(10), String.valueOf(j)}, "due_time");
    }

    public GenericPdu load(Uri uri) throws MmsException {
        PduPart[] pduPartArrLoadParts;
        ReadOrigInd mtkSendReq;
        try {
            synchronized (PDU_CACHE_INSTANCE) {
                if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                    try {
                        PDU_CACHE_INSTANCE.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "load: ", e);
                    }
                    PduCacheEntry pduCacheEntry = (PduCacheEntry) PDU_CACHE_INSTANCE.get(uri);
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
                Cursor cursorQuery = SqliteWrapper.query(this.mContext, this.mContentResolver, uri, PDU_PROJECTION, (String) null, (String[]) null, (String) null);
                MtkPduHeaders mtkPduHeaders = new MtkPduHeaders();
                long id = ContentUris.parseId(uri);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.getCount() == 1 && cursorQuery.moveToFirst()) {
                            int i = cursorQuery.getInt(1);
                            long j = cursorQuery.getLong(2);
                            for (Map.Entry entry : ENCODED_STRING_COLUMN_INDEX_MAP.entrySet()) {
                                setEncodedStringValueToHeaders(cursorQuery, ((Integer) entry.getValue()).intValue(), mtkPduHeaders, ((Integer) entry.getKey()).intValue());
                            }
                            for (Map.Entry entry2 : TEXT_STRING_COLUMN_INDEX_MAP.entrySet()) {
                                setTextStringToHeaders(cursorQuery, ((Integer) entry2.getValue()).intValue(), mtkPduHeaders, ((Integer) entry2.getKey()).intValue());
                            }
                            for (Map.Entry entry3 : OCTET_COLUMN_INDEX_MAP.entrySet()) {
                                setOctetToHeaders(cursorQuery, ((Integer) entry3.getValue()).intValue(), mtkPduHeaders, ((Integer) entry3.getKey()).intValue());
                            }
                            for (Map.Entry entry4 : LONG_COLUMN_INDEX_MAP.entrySet()) {
                                setLongToHeaders(cursorQuery, ((Integer) entry4.getValue()).intValue(), mtkPduHeaders, ((Integer) entry4.getKey()).intValue());
                            }
                            if (this.mBackupRestore) {
                                Log.i(TAG, "load for backuprestore");
                                if (!cursorQuery.isNull(27)) {
                                    int i2 = cursorQuery.getInt(27);
                                    Log.i(TAG, "read value=" + i2);
                                    if (i2 == 1) {
                                        mtkPduHeaders.setOctet(128, 155);
                                    }
                                }
                            }
                            if (id == -1) {
                                throw new MmsException("Error! ID of the message: -1.");
                            }
                            loadAddress(id, mtkPduHeaders);
                            int octet = mtkPduHeaders.getOctet(140);
                            PduBody pduBody = new PduBody();
                            if ((octet == 132 || octet == 128) && (pduPartArrLoadParts = loadParts(id)) != null) {
                                for (PduPart pduPart : pduPartArrLoadParts) {
                                    pduBody.addPart(pduPart);
                                }
                            }
                            switch (octet) {
                                case 128:
                                    mtkSendReq = new MtkSendReq(mtkPduHeaders, pduBody);
                                    break;
                                case 129:
                                case MtkPduHeaders.STATE_SKIP_RETRYING:
                                case 138:
                                case 139:
                                case 140:
                                case 141:
                                case 142:
                                case 143:
                                case 144:
                                case 145:
                                case MtkPduPart.P_DATE:
                                case 147:
                                case 148:
                                case 149:
                                case 150:
                                case 151:
                                    throw new MmsException("Unsupported PDU type: " + Integer.toHexString(octet));
                                case 130:
                                    mtkSendReq = new NotificationInd(mtkPduHeaders);
                                    break;
                                case RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX:
                                    mtkSendReq = new NotifyRespInd(mtkPduHeaders);
                                    break;
                                case 132:
                                    mtkSendReq = new MtkRetrieveConf(mtkPduHeaders, pduBody);
                                    break;
                                case 133:
                                    mtkSendReq = new AcknowledgeInd(mtkPduHeaders);
                                    break;
                                case RadioCdmaSmsConst.UDH_VAR_PIC_SIZE:
                                    mtkSendReq = new DeliveryInd(mtkPduHeaders);
                                    break;
                                case 135:
                                    mtkSendReq = new ReadRecInd(mtkPduHeaders);
                                    break;
                                case 136:
                                    mtkSendReq = new ReadOrigInd(mtkPduHeaders);
                                    break;
                                default:
                                    throw new MmsException("Unrecognized PDU type: " + Integer.toHexString(octet));
                            }
                            synchronized (PDU_CACHE_INSTANCE) {
                                PDU_CACHE_INSTANCE.put(uri, new PduCacheEntry(mtkSendReq, i, j));
                                PDU_CACHE_INSTANCE.setUpdating(uri, false);
                                PDU_CACHE_INSTANCE.notifyAll();
                            }
                            return mtkSendReq;
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

    public Uri persist(GenericPdu genericPdu, Uri uri, boolean z, boolean z2, HashMap<Uri, InputStream> map) throws MmsException {
        long id;
        int i;
        int i2;
        int dataLength;
        long j;
        Uri uriInsert;
        long id2;
        PduBody body;
        PduBody pduBody;
        int[] iArr;
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
        Log.d(TAG, "persist uri " + uri);
        PduHeaders pduHeaders = genericPdu.getPduHeaders();
        ContentValues contentValues = new ContentValues();
        for (Map.Entry entry : ENCODED_STRING_COLUMN_NAME_MAP.entrySet()) {
            int iIntValue = ((Integer) entry.getKey()).intValue();
            EncodedStringValue encodedStringValue = pduHeaders.getEncodedStringValue(iIntValue);
            if (encodedStringValue != null) {
                String str = (String) CHARSET_COLUMN_NAME_MAP.get(Integer.valueOf(iIntValue));
                contentValues.put((String) entry.getValue(), toIsoString(encodedStringValue.getTextString()));
                contentValues.put(str, Integer.valueOf(encodedStringValue.getCharacterSet()));
            }
        }
        for (Map.Entry entry2 : TEXT_STRING_COLUMN_NAME_MAP.entrySet()) {
            byte[] textString = pduHeaders.getTextString(((Integer) entry2.getKey()).intValue());
            if (textString != null) {
                contentValues.put((String) entry2.getValue(), toIsoString(textString));
            }
        }
        for (Map.Entry entry3 : OCTET_COLUMN_NAME_MAP.entrySet()) {
            int octet = pduHeaders.getOctet(((Integer) entry3.getKey()).intValue());
            if (octet != 0) {
                contentValues.put((String) entry3.getValue(), Integer.valueOf(octet));
            }
        }
        if (this.mBackupRestore) {
            Log.i(TAG, "add READ");
            int octet2 = pduHeaders.getOctet(155);
            Log.i(TAG, "READ=" + octet2);
            if (octet2 == 0) {
                contentValues.put("read", Integer.valueOf(octet2));
            } else if (octet2 == 128) {
                contentValues.put("read", (Integer) 1);
            } else {
                contentValues.put("read", (Integer) 0);
            }
        }
        for (Map.Entry entry4 : LONG_COLUMN_NAME_MAP.entrySet()) {
            long longInteger = pduHeaders.getLongInteger(((Integer) entry4.getKey()).intValue());
            if (longInteger != -1) {
                contentValues.put((String) entry4.getValue(), Long.valueOf(longInteger));
            }
        }
        HashMap<Integer, EncodedStringValue[]> map2 = new HashMap<>(ADDRESS_FIELDS.length);
        int[] iArr2 = ADDRESS_FIELDS;
        int length = iArr2.length;
        int i3 = 0;
        while (i3 < length) {
            int i4 = iArr2[i3];
            EncodedStringValue[] encodedStringValues = null;
            if (i4 == 137) {
                EncodedStringValue encodedStringValue2 = pduHeaders.getEncodedStringValue(i4);
                if (encodedStringValue2 != null) {
                    iArr = iArr2;
                    encodedStringValues = new EncodedStringValue[]{encodedStringValue2};
                } else {
                    iArr = iArr2;
                }
            } else {
                iArr = iArr2;
                encodedStringValues = pduHeaders.getEncodedStringValues(i4);
            }
            map2.put(Integer.valueOf(i4), encodedStringValues);
            i3++;
            iArr2 = iArr;
        }
        HashSet<String> hashSet = new HashSet<>();
        int messageType = genericPdu.getMessageType();
        if (messageType == 130 || messageType == 132) {
            i = 128;
        } else {
            i = 128;
            if (messageType == 128) {
            }
            Log.d(TAG, "persist part begin ");
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (!(genericPdu instanceof MultimediaMessagePdu) && (body = ((MultimediaMessagePdu) genericPdu).getBody()) != null) {
                int partsNum = body.getPartsNum();
                i2 = partsNum > 2 ? 0 : 1;
                int i5 = 0;
                dataLength = 0;
                while (i5 < partsNum) {
                    PduPart part = body.getPart(i5);
                    dataLength += part.getDataLength();
                    persistPart(part, jCurrentTimeMillis, map);
                    String partContentType = getPartContentType(part);
                    if (partContentType != null) {
                        pduBody = body;
                        if (!"application/smil".equals(partContentType) && !"text/plain".equals(partContentType)) {
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
            Log.d(TAG, "persist pdu begin ");
            contentValues.put("need_notify", (Boolean) false);
            if (this.mBackupRestore) {
                contentValues.put("seen", (Integer) 1);
            }
            if (!z3) {
                long j2 = id;
                j = jCurrentTimeMillis;
                SqliteWrapper.update(this.mContext, this.mContentResolver, uri, contentValues, (String) null, (String[]) null);
                uriInsert = uri;
                id2 = j2;
            } else {
                j = jCurrentTimeMillis;
                uriInsert = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, contentValues);
                if (uriInsert == null) {
                    throw new MmsException("persist() failed: return null.");
                }
                id2 = ContentUris.parseId(uriInsert);
            }
            Log.d(TAG, "persist address begin ");
            for (int i6 : ADDRESS_FIELDS) {
                EncodedStringValue[] encodedStringValueArr = map2.get(Integer.valueOf(i6));
                if (encodedStringValueArr != null) {
                    persistAddress(id2, i6, encodedStringValueArr);
                }
            }
            Log.d(TAG, "persist update part begin ");
            ContentValues contentValues2 = new ContentValues(1);
            contentValues2.put("mid", Long.valueOf(id2));
            contentValues2.put("need_notify", (Boolean) true);
            SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + j + "/part"), contentValues2, (String) null, (String[]) null);
            PDU_CACHE_INSTANCE.purge(uri);
            Log.d(TAG, "persist purge end ");
            if (z3) {
                return Uri.parse(uri + "/" + id2);
            }
            return uriInsert;
        }
        if (messageType != i) {
            if (messageType == 130 || messageType == 132) {
                loadRecipients(MtkPduHeaders.STATE_SKIP_RETRYING, hashSet, map2, false);
                if (z2) {
                    loadRecipients(151, hashSet, map2, true);
                    loadRecipients(130, hashSet, map2, true);
                }
            }
        } else {
            loadRecipients(151, hashSet, map2, false);
        }
        long orCreateThreadId = 0;
        if (z && !hashSet.isEmpty()) {
            orCreateThreadId = Telephony.Threads.getOrCreateThreadId(this.mContext, hashSet);
        }
        contentValues.put("thread_id", Long.valueOf(orCreateThreadId));
        Log.d(TAG, "persist part begin ");
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        if (!(genericPdu instanceof MultimediaMessagePdu)) {
            i2 = 1;
            dataLength = 0;
        }
        contentValues.put("text_only", Integer.valueOf(i2));
        if (contentValues.getAsInteger("m_size") == null) {
        }
        Log.d(TAG, "persist pdu begin ");
        contentValues.put("need_notify", (Boolean) false);
        if (this.mBackupRestore) {
        }
        if (!z3) {
        }
        Log.d(TAG, "persist address begin ");
        while (i < r5) {
        }
        Log.d(TAG, "persist update part begin ");
        ContentValues contentValues22 = new ContentValues(1);
        contentValues22.put("mid", Long.valueOf(id2));
        contentValues22.put("need_notify", (Boolean) true);
        SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + j + "/part"), contentValues22, (String) null, (String[]) null);
        PDU_CACHE_INSTANCE.purge(uri);
        Log.d(TAG, "persist purge end ");
        if (z3) {
        }
    }

    public GenericPdu load(Uri uri, boolean z) throws MmsException {
        Log.i("MMSLog", "load for backuprestore");
        this.mBackupRestore = z;
        return load(uri);
    }

    public Uri persist(GenericPdu genericPdu, Uri uri, boolean z) throws MmsException {
        Log.i("MMSLog", "persist for backuprestore");
        this.mBackupRestore = z;
        return persist(genericPdu, uri, true, false);
    }

    public Uri persistEx(GenericPdu genericPdu, Uri uri, HashMap<String, String> map) throws MmsException {
        Log.i("MMSLog", "Call persist_ex 1");
        return persistForBackupRestore(genericPdu, uri, map);
    }

    public Uri persistEx(GenericPdu genericPdu, Uri uri, boolean z, HashMap<String, String> map) throws MmsException {
        Log.i("MMSLog", "Call persist_ex 2");
        this.mBackupRestore = z;
        return persistForBackupRestore(genericPdu, uri, map);
    }

    private Uri persistForBackupRestore(GenericPdu genericPdu, Uri uri, HashMap<String, String> map) throws MmsException {
        long id;
        EncodedStringValue[] encodedStringValueArr;
        String str;
        int i;
        Uri uriInsert;
        PduBody body;
        int[] iArr;
        EncodedStringValue[] encodedStringValues;
        int i2;
        if (uri == null) {
            throw new MmsException("Uri may not be null.");
        }
        try {
            id = ContentUris.parseId(uri);
        } catch (NumberFormatException e) {
            id = -1;
        }
        boolean z = id != -1;
        if (!z && MESSAGE_BOX_MAP.get(uri) == null) {
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
        PduHeaders pduHeaders = genericPdu.getPduHeaders();
        ContentValues contentValues = new ContentValues();
        for (Map.Entry entry : ENCODED_STRING_COLUMN_NAME_MAP.entrySet()) {
            int iIntValue = ((Integer) entry.getKey()).intValue();
            EncodedStringValue encodedStringValue = pduHeaders.getEncodedStringValue(iIntValue);
            if (encodedStringValue != null) {
                String str2 = (String) CHARSET_COLUMN_NAME_MAP.get(Integer.valueOf(iIntValue));
                contentValues.put((String) entry.getValue(), toIsoString(encodedStringValue.getTextString()));
                contentValues.put(str2, Integer.valueOf(encodedStringValue.getCharacterSet()));
            }
        }
        for (Map.Entry entry2 : TEXT_STRING_COLUMN_NAME_MAP.entrySet()) {
            byte[] textString = pduHeaders.getTextString(((Integer) entry2.getKey()).intValue());
            if (textString != null) {
                contentValues.put((String) entry2.getValue(), toIsoString(textString));
            }
        }
        for (Map.Entry entry3 : OCTET_COLUMN_NAME_MAP.entrySet()) {
            int octet = pduHeaders.getOctet(((Integer) entry3.getKey()).intValue());
            if (octet != 0) {
                contentValues.put((String) entry3.getValue(), Integer.valueOf(octet));
            }
        }
        if (this.mBackupRestore) {
            if (map != null) {
                i2 = Integer.parseInt(map.get("read"));
            } else {
                i2 = 0;
            }
            contentValues.put("read", Integer.valueOf(i2));
            long j = 0;
            if (map != null && map.get("m_size") != null) {
                j = Long.parseLong(map.get("m_size"));
            }
            contentValues.put("m_size", Long.valueOf(j));
        }
        for (Map.Entry entry4 : LONG_COLUMN_NAME_MAP.entrySet()) {
            long longInteger = pduHeaders.getLongInteger(((Integer) entry4.getKey()).intValue());
            if (longInteger != -1) {
                contentValues.put((String) entry4.getValue(), Long.valueOf(longInteger));
            }
        }
        HashMap map2 = new HashMap(ADDRESS_FIELDS.length);
        int[] iArr2 = ADDRESS_FIELDS;
        int length = iArr2.length;
        int i3 = 0;
        while (i3 < length) {
            int i4 = iArr2[i3];
            if (i4 == 137) {
                EncodedStringValue encodedStringValue2 = pduHeaders.getEncodedStringValue(i4);
                if (encodedStringValue2 != null) {
                    iArr = iArr2;
                    encodedStringValues = new EncodedStringValue[]{encodedStringValue2};
                } else {
                    iArr = iArr2;
                    encodedStringValues = null;
                }
            } else {
                iArr = iArr2;
                encodedStringValues = pduHeaders.getEncodedStringValues(i4);
            }
            map2.put(Integer.valueOf(i4), encodedStringValues);
            i3++;
            iArr2 = iArr;
        }
        HashSet hashSet = new HashSet();
        int messageType = genericPdu.getMessageType();
        if (messageType == 130 || messageType == 132 || messageType == 128) {
            if (messageType == 128) {
                encodedStringValueArr = (EncodedStringValue[]) map2.get(151);
            } else if (messageType == 130 || messageType == 132) {
                encodedStringValueArr = (EncodedStringValue[]) map2.get(Integer.valueOf(MtkPduHeaders.STATE_SKIP_RETRYING));
            } else {
                encodedStringValueArr = null;
            }
            if (encodedStringValueArr != null) {
                for (EncodedStringValue encodedStringValue3 : encodedStringValueArr) {
                    if (encodedStringValue3 != null) {
                        hashSet.add(encodedStringValue3.getString());
                    }
                }
            }
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (map != null) {
                str = map.get("index");
            } else {
                str = null;
            }
            if (!hashSet.isEmpty()) {
                contentValues.put("thread_id", Long.valueOf(MtkTelephony.MtkThreads.getOrCreateThreadId(this.mContext, hashSet, str)));
            }
            Log.d("MMSLog", "BR_TEST: getThreadId=" + (System.currentTimeMillis() - jCurrentTimeMillis));
        }
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        Log.d(TAG, "persist pdu begin ");
        contentValues.put("need_notify", (Boolean) true);
        if (this.mBackupRestore) {
            contentValues.put("seen", (Integer) 1);
        }
        int i5 = -1;
        if (map != null) {
            i5 = Integer.parseInt(map.get("sub_id"));
            i = Integer.parseInt(map.get("locked"));
        } else {
            i = 0;
        }
        contentValues.put("sub_id", Integer.valueOf(i5));
        contentValues.put("locked", Integer.valueOf(i));
        if (z) {
            SqliteWrapper.update(this.mContext, this.mContentResolver, uri, contentValues, (String) null, (String[]) null);
            uriInsert = uri;
        } else {
            uriInsert = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, contentValues);
            if (uriInsert == null) {
                throw new MmsException("persist() failed: return null.");
            }
            id = ContentUris.parseId(uriInsert);
        }
        Log.d("MMSLog", "BR_TEST: parse time persistPdu=" + (System.currentTimeMillis() - jCurrentTimeMillis2));
        Log.d(TAG, "persist address begin ");
        long jCurrentTimeMillis3 = System.currentTimeMillis();
        for (int i6 : ADDRESS_FIELDS) {
            EncodedStringValue[] encodedStringValueArr2 = (EncodedStringValue[]) map2.get(Integer.valueOf(i6));
            if (encodedStringValueArr2 != null) {
                persistAddress(id, i6, encodedStringValueArr2);
            }
        }
        Log.d("MMSLog", "BR_TEST: parse time persistAddress=" + (System.currentTimeMillis() - jCurrentTimeMillis3));
        Log.d(TAG, "persist part begin ");
        if ((genericPdu instanceof MultimediaMessagePdu) && (body = ((MultimediaMessagePdu) genericPdu).getBody()) != null) {
            int partsNum = body.getPartsNum();
            long jCurrentTimeMillis4 = System.currentTimeMillis();
            for (int i7 = 0; i7 < partsNum; i7++) {
                persistPart(body.getPart(i7), id, null);
            }
            Log.d("MMSLog", "BR_TEST: parse time PersistPart=" + (System.currentTimeMillis() - jCurrentTimeMillis4));
        }
        if (!z) {
            return Uri.parse(uri + "/" + id);
        }
        return uriInsert;
    }
}
