package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.util.HexDump;
import java.util.ArrayList;
import java.util.Iterator;

public class MtkConcatenatedSmsFwk implements IMtkConcatenatedSmsFwk {
    private static final String MTK_KEY_EMS_WAITING_MISSING_SEGMENT_TIME_INT = "mtk_ems_waiting_missing_segment_time";
    private static final String TAG = "MtkConSmsFwk";
    protected Context mContext;
    private ArrayList<MtkTimerRecord> mMtkTimerRecords = new ArrayList<>(5);
    protected int mPhoneId = -1;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(IMtkConcatenatedSmsFwk.ACTION_CLEAR_OUT_SEGMENTS)) {
                int intExtra = intent.getIntExtra("phone", -1);
                synchronized (MtkConcatenatedSmsFwk.this) {
                    if (intExtra == MtkConcatenatedSmsFwk.this.mPhoneId) {
                        MtkConcatenatedSmsFwk.this.checkOutOfDateSegments(true);
                        MtkConcatenatedSmsFwk.this.checkOutOfDateSegments(false);
                    }
                }
            }
        }
    };
    private ContentResolver mResolver;
    private InboundSmsTracker mTracker;
    private static final boolean ENG = "eng".equals(Build.TYPE);
    private static final Uri mRawUri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "raw");
    private static final String[] CONCATE_PROJECTION = {"reference_number", "count", "sequence"};
    private static final String[] PDU_SEQUENCE_PORT_PROJECTION = {"pdu", "sequence", "destination_port"};
    private static final String[] PDU_SEQUENCE_PORT_UPLOAD_PROJECTION = {"pdu", "sequence", "destination_port", IMtkConcatenatedSmsFwk.UPLOAD_FLAG_TAG};
    private static final String[] OUT_OF_DATE_PROJECTION = {"recv_time", "address", "reference_number", "count", "sub_id"};
    protected static int DELAYED_TIME = 60000;

    public MtkConcatenatedSmsFwk(Context context) {
        this.mContext = null;
        this.mResolver = null;
        if (context == null) {
            Rlog.d(TAG, "FAIL! context is null");
            return;
        }
        this.mContext = context;
        this.mResolver = this.mContext.getContentResolver();
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter(IMtkConcatenatedSmsFwk.ACTION_CLEAR_OUT_SEGMENTS), "android.permission.BROADCAST_SMS", null);
    }

    @Override
    public synchronized void setPhoneId(int i) {
        this.mPhoneId = i;
    }

    private void registerAlarmManager() {
    }

    private synchronized void deleteOutOfDateSegments(String str, int i, int i2, int i3) {
        try {
            Rlog.d(TAG, "deleteOutOfDateSegments remove " + this.mResolver.delete(mRawUri, "address=? AND reference_number=? AND count=? AND sub_id=?", new String[]{str, Integer.toString(i), Integer.toString(i2), Integer.toString(getSubIdUsingPhoneId())}) + " out of date segments, ref =  " + i);
        } catch (SQLException e) {
            Rlog.d(TAG, "FAIL! SQLException");
        }
    }

    private synchronized void checkOutOfDateSegments(boolean z) {
        Cursor cursorQuery;
        int i;
        Cursor cursor = null;
        try {
            try {
                try {
                    i = 0;
                    cursorQuery = this.mResolver.query(mRawUri, OUT_OF_DATE_PROJECTION, "sub_id=?", new String[]{Integer.toString(getSubIdUsingPhoneId())}, null);
                } catch (SQLException e) {
                }
            } catch (Throwable th) {
                th = th;
                cursorQuery = cursor;
            }
            try {
                if (cursorQuery != null) {
                    int columnIndex = cursorQuery.getColumnIndex("recv_time");
                    int columnIndex2 = cursorQuery.getColumnIndex("address");
                    int columnIndex3 = cursorQuery.getColumnIndex("reference_number");
                    int columnIndex4 = cursorQuery.getColumnIndex("count");
                    int columnIndex5 = cursorQuery.getColumnIndex("sub_id");
                    int count = cursorQuery.getCount();
                    Rlog.d(TAG, "checkOutOfDateSegments cursor size=" + count + ", phoneId=" + Integer.toString(this.mPhoneId));
                    while (i < count) {
                        cursorQuery.moveToNext();
                        long j = cursorQuery.getLong(columnIndex);
                        long jCurrentTimeMillis = System.currentTimeMillis();
                        int i2 = columnIndex;
                        MtkTimerRecord mtkTimerRecordQueryTimerRecord = queryTimerRecord(cursorQuery.getString(columnIndex2), cursorQuery.getInt(columnIndex3), cursorQuery.getInt(columnIndex4), z);
                        if (mtkTimerRecordQueryTimerRecord != null) {
                            deleteTimerRecord(mtkTimerRecordQueryTimerRecord);
                        }
                        if (ENG) {
                            Rlog.d(TAG, "currtime=" + jCurrentTimeMillis + ", recv_time=" + j);
                        }
                        if (jCurrentTimeMillis - j >= 43200000) {
                            deleteOutOfDateSegments(cursorQuery.getString(columnIndex2), cursorQuery.getInt(columnIndex3), cursorQuery.getInt(columnIndex4), SubscriptionManager.getPhoneId(cursorQuery.getInt(columnIndex5)));
                        }
                        i++;
                        columnIndex = i2;
                    }
                } else {
                    Rlog.d(TAG, "FAIL! cursor is null");
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (SQLException e2) {
                cursor = cursorQuery;
                Rlog.d(TAG, "FAIL! SQLException");
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th2) {
                th = th2;
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            throw th3;
        }
    }

    @Override
    public synchronized boolean isFirstConcatenatedSegment(String str, int i, boolean z) {
        boolean z2;
        Cursor cursorQuery;
        Cursor cursor = null;
        try {
            try {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("address=? AND reference_number=? AND sub_id=?AND deleted=0");
                    sb.append(z ? IMtkConcatenatedSmsFwk.SQL_3GPP2_SMS : IMtkConcatenatedSmsFwk.SQL_3GPP_SMS);
                    cursorQuery = this.mResolver.query(mRawUri, CONCATE_PROJECTION, sb.toString(), new String[]{str, Integer.toString(i), Integer.toString(getSubIdUsingPhoneId())}, null);
                } catch (Throwable th) {
                    throw th;
                }
            } catch (SQLException e) {
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            if (cursorQuery != null) {
                z2 = cursorQuery.getCount() == 0;
            } else {
                Rlog.d(TAG, "FAIL! cursor is null");
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (SQLException e2) {
            cursor = cursorQuery;
            Rlog.d(TAG, "FAIL! SQLException");
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th3) {
            th = th3;
            cursor = cursorQuery;
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
        Rlog.d(TAG, "isFirstConcatenatedSegment: " + str + "/" + i + "result =" + z2);
        return z2;
    }

    @Override
    public synchronized boolean isLastConcatenatedSegment(String str, int i, int i2, boolean z) {
        boolean z2;
        boolean z3;
        Cursor cursorQuery;
        Cursor cursor = null;
        z2 = false;
        try {
            try {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("address=? AND reference_number=? AND sub_id=?");
                    sb.append(z ? IMtkConcatenatedSmsFwk.SQL_3GPP2_SMS : IMtkConcatenatedSmsFwk.SQL_3GPP_SMS);
                    z3 = true;
                    cursorQuery = this.mResolver.query(mRawUri, CONCATE_PROJECTION, sb.toString(), new String[]{str, Integer.toString(i), Integer.toString(getSubIdUsingPhoneId())}, null);
                } catch (Throwable th) {
                    throw th;
                }
            } catch (SQLException e) {
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            if (cursorQuery != null) {
                if (cursorQuery.getCount() != i2 - 1) {
                    z3 = false;
                }
                z2 = z3;
            } else {
                Rlog.d(TAG, "FAIL! cursor is null");
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (SQLException e2) {
            cursor = cursorQuery;
            Rlog.d(TAG, "FAIL! SQLException");
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th3) {
            th = th3;
            cursor = cursorQuery;
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
        Rlog.d(TAG, "isLastConcatenatedSegment: " + str + "/" + i + " result =" + z2);
        return z2;
    }

    @Override
    public void startTimer(Handler handler, Object obj) {
        Rlog.d(TAG, "call startTimer");
        if (!checkParamsForMessageOperation(handler, obj)) {
            Rlog.d(TAG, "FAIL! invalid params");
        } else {
            addTimerRecord((MtkTimerRecord) obj);
            handler.sendMessageDelayed(handler.obtainMessage(IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, obj), getDelayedTime());
        }
    }

    @Override
    public void cancelTimer(Handler handler, Object obj) {
        Rlog.d(TAG, "call cancelTimer");
        if (!checkParamsForMessageOperation(handler, obj)) {
            Rlog.d(TAG, "FAIL! invalid params");
        } else {
            handler.removeMessages(IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, obj);
            deleteTimerRecord((MtkTimerRecord) obj);
        }
    }

    @Override
    public void refreshTimer(Handler handler, Object obj) {
        Rlog.d(TAG, "call refreshTimer");
        if (!checkParamsForMessageOperation(handler, obj)) {
            Rlog.d(TAG, "FAIL! invalid params");
        } else {
            handler.removeMessages(IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, obj);
            handler.sendMessageDelayed(handler.obtainMessage(IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, obj), getDelayedTime());
        }
    }

    @Override
    public synchronized MtkTimerRecord queryTimerRecord(String str, int i, int i2, boolean z) {
        MtkTimerRecord mtkTimerRecord;
        InboundSmsTracker inboundSmsTracker;
        mtkTimerRecord = null;
        Iterator<MtkTimerRecord> it = this.mMtkTimerRecords.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            MtkTimerRecord next = it.next();
            if (next.address.equals(str) && next.refNumber == i && next.msgCount == i2 && (inboundSmsTracker = (InboundSmsTracker) next.mTracker) != null && inboundSmsTracker.is3gpp2() == z) {
                mtkTimerRecord = next;
                break;
            }
        }
        Rlog.d(TAG, "queryTimerRecord find record by [" + str + ", " + i + ", " + i2 + "] result = " + mtkTimerRecord);
        return mtkTimerRecord;
    }

    private void addTimerRecord(MtkTimerRecord mtkTimerRecord) {
        Rlog.d(TAG, "call addTimerRecord");
        Iterator<MtkTimerRecord> it = this.mMtkTimerRecords.iterator();
        while (it.hasNext()) {
            if (it.next() == mtkTimerRecord) {
                Rlog.d(TAG, "duplicated addTimerRecord object be found");
                return;
            }
        }
        this.mMtkTimerRecords.add(mtkTimerRecord);
    }

    private void deleteTimerRecord(MtkTimerRecord mtkTimerRecord) {
        if (this.mMtkTimerRecords == null || this.mMtkTimerRecords.size() == 0) {
            Rlog.d(TAG, "no record can be removed ");
            return;
        }
        int size = this.mMtkTimerRecords.size();
        this.mMtkTimerRecords.remove(mtkTimerRecord);
        int size2 = size - this.mMtkTimerRecords.size();
        if (size2 > 0) {
            Rlog.d(TAG, "deleteTimerRecord" + size2);
            return;
        }
        Rlog.d(TAG, "no record be removed");
    }

    private boolean checkParamsForMessageOperation(Handler handler, Object obj) {
        Rlog.d(TAG, "call checkParamsForMessageOperation");
        if (handler == null) {
            Rlog.d(TAG, "FAIL! handler is null");
            return false;
        }
        if (obj == null) {
            Rlog.d(TAG, "FAIL! record is null");
            return false;
        }
        if (!(obj instanceof MtkTimerRecord)) {
            Rlog.d(TAG, "FAIL! param r is not MtkTimerRecord object");
            return false;
        }
        return true;
    }

    @Override
    public synchronized byte[][] queryExistedSegments(MtkTimerRecord mtkTimerRecord) {
        Cursor cursorQuery;
        byte[][] bArr;
        int realDestPort;
        ?? Is3gpp2 = ((InboundSmsTracker) mtkTimerRecord.mTracker).is3gpp2();
        try {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("address=? AND reference_number=? AND sub_id=? AND count=?");
                sb.append(Is3gpp2 != 0 ? IMtkConcatenatedSmsFwk.SQL_3GPP2_SMS : IMtkConcatenatedSmsFwk.SQL_3GPP_SMS);
                cursorQuery = this.mResolver.query(mRawUri, PDU_SEQUENCE_PORT_PROJECTION, sb.toString(), new String[]{mtkTimerRecord.address, Integer.toString(mtkTimerRecord.refNumber), Integer.toString(getSubIdUsingPhoneId()), Integer.toString(mtkTimerRecord.msgCount)}, null);
                try {
                    if (cursorQuery != null) {
                        byte[][] bArr2 = new byte[mtkTimerRecord.msgCount][];
                        int columnIndex = cursorQuery.getColumnIndex("sequence");
                        int columnIndex2 = cursorQuery.getColumnIndex("pdu");
                        int columnIndex3 = cursorQuery.getColumnIndex("destination_port");
                        int count = cursorQuery.getCount();
                        Rlog.d(TAG, "queryExistedSegments columnSeqence =" + columnIndex + "; columnPdu = " + columnIndex2 + "; columnPort =" + columnIndex3 + " miss " + (mtkTimerRecord.msgCount - count) + " segment(s)");
                        for (int i = 0; i < count; i++) {
                            cursorQuery.moveToNext();
                            int i2 = cursorQuery.getInt(columnIndex);
                            if (ENG) {
                                Rlog.d(TAG, "queried segment " + i2 + ", ref = " + mtkTimerRecord.refNumber);
                            }
                            int i3 = i2 - 1;
                            bArr2[i3] = HexDump.hexStringToByteArray(cursorQuery.getString(columnIndex2));
                            if (bArr2[i3] == null && ENG) {
                                Rlog.d(TAG, "miss segment " + i2 + ", ref = " + mtkTimerRecord.refNumber);
                            }
                            if (!cursorQuery.isNull(columnIndex3) && (realDestPort = InboundSmsTracker.getRealDestPort(cursorQuery.getInt(columnIndex3))) != -1) {
                                Rlog.d(TAG, "segment contain port " + realDestPort);
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                return null;
                            }
                        }
                        bArr = new byte[count][];
                        int length = bArr2.length;
                        int i4 = 0;
                        for (int i5 = 0; i5 < length; i5++) {
                            if (bArr2[i5] != null) {
                                bArr[i4] = bArr2[i5];
                                i4++;
                            }
                        }
                    } else {
                        Rlog.d(TAG, "FAIL! cursor is null");
                        bArr = null;
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return bArr;
                } catch (SQLException e) {
                    Rlog.d(TAG, "FAIL! SQLException");
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                if (Is3gpp2 != 0) {
                    Is3gpp2.close();
                }
                throw th;
            }
        } catch (SQLException e2) {
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
            Is3gpp2 = 0;
            if (Is3gpp2 != 0) {
            }
            throw th;
        }
    }

    @Override
    public synchronized void deleteExistedSegments(MtkTimerRecord mtkTimerRecord) {
        boolean zIs3gpp2 = ((InboundSmsTracker) mtkTimerRecord.mTracker).is3gpp2();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("address=? AND reference_number=? AND sub_id=?");
            sb.append(zIs3gpp2 ? IMtkConcatenatedSmsFwk.SQL_3GPP2_SMS : IMtkConcatenatedSmsFwk.SQL_3GPP_SMS);
            Rlog.d(TAG, "deleteExistedSegments remove " + this.mResolver.delete(mRawUri, sb.toString(), new String[]{mtkTimerRecord.address, Integer.toString(mtkTimerRecord.refNumber), Integer.toString(getSubIdUsingPhoneId())}) + " segments, ref =  " + mtkTimerRecord.refNumber);
        } catch (SQLException e) {
            Rlog.d(TAG, "FAIL! SQLException");
        }
        deleteTimerRecord(mtkTimerRecord);
    }

    @Override
    public synchronized int getUploadFlag(MtkTimerRecord mtkTimerRecord) {
        Cursor cursorQuery;
        boolean zIs3gpp2 = ((InboundSmsTracker) mtkTimerRecord.mTracker).is3gpp2();
        Cursor cursor = null;
        try {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("address=? AND reference_number=? AND sub_id=?");
                sb.append(zIs3gpp2 ? IMtkConcatenatedSmsFwk.SQL_3GPP2_SMS : IMtkConcatenatedSmsFwk.SQL_3GPP_SMS);
                cursorQuery = this.mResolver.query(mRawUri, PDU_SEQUENCE_PORT_UPLOAD_PROJECTION, sb.toString(), new String[]{mtkTimerRecord.address, Integer.toString(mtkTimerRecord.refNumber), Integer.toString(getSubIdUsingPhoneId())}, null);
            } catch (SQLException e) {
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            if (cursorQuery == null) {
                Rlog.d(TAG, "FAIL! cursor is null");
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return -1;
            }
            while (cursorQuery.moveToNext()) {
                if (cursorQuery.getInt(cursorQuery.getColumnIndex(IMtkConcatenatedSmsFwk.UPLOAD_FLAG_TAG)) == 2) {
                    Rlog.d(TAG, "getUploadFlag find update segment");
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return 2;
                }
            }
            Rlog.d(TAG, "getUploadFlag all segments are new");
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return 1;
        } catch (SQLException e2) {
            cursor = cursorQuery;
            Rlog.d(TAG, "FAIL! SQLException, fail to query upload_flag");
            if (cursor != null) {
                cursor.close();
            }
            return -1;
        } catch (Throwable th2) {
            cursor = cursorQuery;
            th = th2;
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    @Override
    public synchronized void setUploadFlag(MtkTimerRecord mtkTimerRecord) {
        boolean zIs3gpp2 = ((InboundSmsTracker) mtkTimerRecord.mTracker).is3gpp2();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("address=? AND reference_number=? AND sub_id=? AND upload_flag<>?");
            sb.append(zIs3gpp2 ? IMtkConcatenatedSmsFwk.SQL_3GPP2_SMS : IMtkConcatenatedSmsFwk.SQL_3GPP_SMS);
            String string = sb.toString();
            String[] strArr = {mtkTimerRecord.address, Integer.toString(mtkTimerRecord.refNumber), Integer.toString(getSubIdUsingPhoneId()), Integer.toString(2)};
            ContentValues contentValues = new ContentValues();
            contentValues.put(IMtkConcatenatedSmsFwk.UPLOAD_FLAG_TAG, (Integer) 2);
            Rlog.d(TAG, "setUploadFlag update count: " + this.mResolver.update(mRawUri, contentValues, string, strArr));
        } catch (SQLException e) {
            Rlog.d(TAG, "FAIL! SQLException, fail to update upload flag");
        }
    }

    private synchronized int getDelayedTime() {
        int i;
        PersistableBundle configForSubId = ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfigForSubId(getSubIdUsingPhoneId());
        i = DELAYED_TIME;
        if (configForSubId != null) {
            i = configForSubId.getInt(MTK_KEY_EMS_WAITING_MISSING_SEGMENT_TIME_INT) * 1000;
        }
        Rlog.d(TAG, "getDelayedTime " + i);
        return i;
    }

    private int getSubIdUsingPhoneId() {
        SubscriptionController subscriptionController = SubscriptionController.getInstance();
        int subIdUsingPhoneId = subscriptionController != null ? subscriptionController.getSubIdUsingPhoneId(this.mPhoneId) : -1;
        if (ENG) {
            Rlog.d(TAG, "[getSubIdUsingPhoneId] subId " + subIdUsingPhoneId + ", phoneId " + this.mPhoneId);
        }
        return subIdUsingPhoneId;
    }
}
