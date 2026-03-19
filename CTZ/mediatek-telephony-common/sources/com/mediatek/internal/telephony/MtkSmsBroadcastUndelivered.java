package com.mediatek.internal.telephony;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.os.UserManager;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsBroadcastUndelivered;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import java.util.HashMap;
import java.util.HashSet;

public class MtkSmsBroadcastUndelivered extends SmsBroadcastUndelivered {
    private static final String TAG = "MtkSmsBroadcastUndelivered";
    private final Phone mPhone;
    private static final String[] PDU_PENDING_MESSAGE_PROJECTION = {"pdu", "sequence", "destination_port", "date", "reference_number", "count", "address", "_id", "message_body", "display_originating_addr", "sub_id"};
    private static MtkSmsBroadcastUndelivered[] instance = new MtkSmsBroadcastUndelivered[TelephonyManager.getDefault().getPhoneCount()];

    public static void initialize(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler) {
        int phoneId = gsmInboundSmsHandler.getPhone().getPhoneId();
        if (instance[phoneId] == null) {
            Rlog.d(TAG, "Phone " + phoneId + " call initialize");
            instance[phoneId] = new MtkSmsBroadcastUndelivered(context, gsmInboundSmsHandler, cdmaInboundSmsHandler);
        }
        if (gsmInboundSmsHandler != null) {
            gsmInboundSmsHandler.sendMessage(6);
        }
        if (cdmaInboundSmsHandler != null) {
            cdmaInboundSmsHandler.sendMessage(6);
        }
    }

    private MtkSmsBroadcastUndelivered(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler) {
        super(context, gsmInboundSmsHandler, cdmaInboundSmsHandler);
        this.mPhone = gsmInboundSmsHandler.getPhone();
        if (!((UserManager) context.getSystemService(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER)).isUserUnlocked()) {
            Rlog.d(TAG, "Phone " + this.mPhone.getPhoneId() + " register user unlock event");
        }
    }

    protected void scanRawTable(Context context) throws Throwable {
        scanRawTable(context, false);
        scanRawTable(context, true);
    }

    private void scanRawTable(Context context, boolean z) throws Throwable {
        Cursor cursorQuery;
        String str;
        StringBuilder sb;
        Rlog.d(TAG, "scanning raw table for undelivered messages");
        long jNanoTime = System.nanoTime();
        HashMap map = new HashMap(4);
        HashSet<SmsReferenceKey> hashSet = new HashSet(4);
        Cursor cursor = null;
        try {
            try {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("deleted = 0");
                sb2.append(z ? IMtkConcatenatedSmsFwk.SQL_3GPP2_SMS : IMtkConcatenatedSmsFwk.SQL_3GPP_SMS);
                cursorQuery = this.mResolver.query(InboundSmsHandler.sRawUri, PDU_PENDING_MESSAGE_PROJECTION, sb2.toString(), null, null);
            } catch (SQLException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
            cursorQuery = cursor;
        }
        try {
        } catch (SQLException e2) {
            e = e2;
            cursor = cursorQuery;
            Rlog.e(TAG, "error reading pending SMS messages", e);
            if (cursor != null) {
                cursor.close();
            }
            str = TAG;
            sb = new StringBuilder();
        } catch (Throwable th2) {
            th = th2;
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            Rlog.d(TAG, "finished scanning raw table in " + ((System.nanoTime() - jNanoTime) / 1000000) + " ms");
            throw th;
        }
        if (cursorQuery == null) {
            Rlog.e(TAG, "error getting pending message cursor");
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            Rlog.d(TAG, "finished scanning raw table in " + ((System.nanoTime() - jNanoTime) / 1000000) + " ms");
            return;
        }
        while (cursorQuery.moveToNext()) {
            try {
                MtkInboundSmsTracker mtkInboundSmsTracker = (MtkInboundSmsTracker) TelephonyComponentFactory.getInstance().makeInboundSmsTracker(cursorQuery, z);
                if (mtkInboundSmsTracker.getMessageCount() != 1) {
                    SmsReferenceKey smsReferenceKey = new SmsReferenceKey(mtkInboundSmsTracker);
                    Integer num = (Integer) map.get(smsReferenceKey);
                    if (num == null) {
                        map.put(smsReferenceKey, 1);
                        if (mtkInboundSmsTracker.getTimestamp() < System.currentTimeMillis() - getUndeliveredSmsExpirationTime(context)) {
                            hashSet.add(smsReferenceKey);
                        }
                    } else {
                        int iIntValue = num.intValue() + 1;
                        if (iIntValue == mtkInboundSmsTracker.getMessageCount()) {
                            Rlog.d(TAG, "found complete multi-part message");
                            if (mtkInboundSmsTracker.getSubId() == this.mPhone.getSubId()) {
                                Rlog.d(TAG, "New sms on raw table, subId: " + mtkInboundSmsTracker.getSubId());
                                broadcastSms(mtkInboundSmsTracker);
                            }
                            hashSet.remove(smsReferenceKey);
                        } else {
                            map.put(smsReferenceKey, Integer.valueOf(iIntValue));
                        }
                    }
                } else if (mtkInboundSmsTracker.getSubId() == this.mPhone.getSubId()) {
                    Rlog.d(TAG, "New sms on raw table, subId: " + mtkInboundSmsTracker.getSubId());
                    broadcastSms(mtkInboundSmsTracker);
                }
            } catch (IllegalArgumentException e3) {
                Rlog.e(TAG, "error loading SmsTracker: " + e3);
            }
        }
        for (SmsReferenceKey smsReferenceKey2 : hashSet) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("address=? AND reference_number=? AND count=? AND deleted=0 AND sub_id=?");
            sb3.append(z ? IMtkConcatenatedSmsFwk.SQL_3GPP2_SMS : IMtkConcatenatedSmsFwk.SQL_3GPP_SMS);
            int iDelete = this.mResolver.delete(InboundSmsHandler.sRawUriPermanentDelete, sb3.toString(), smsReferenceKey2.getDeleteWhereArgs());
            if (iDelete == 0) {
                Rlog.e(TAG, "No rows were deleted from raw table!");
            } else {
                Rlog.d(TAG, "Deleted " + iDelete + " rows from raw table for incomplete " + smsReferenceKey2.mMessageCount + " part message");
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        str = TAG;
        sb = new StringBuilder();
        sb.append("finished scanning raw table in ");
        sb.append((System.nanoTime() - jNanoTime) / 1000000);
        sb.append(" ms");
        Rlog.d(str, sb.toString());
    }

    protected void broadcastSms(InboundSmsTracker inboundSmsTracker) {
        CdmaInboundSmsHandler cdmaInboundSmsHandler;
        if (inboundSmsTracker.is3gpp2()) {
            cdmaInboundSmsHandler = this.mCdmaInboundSmsHandler;
        } else {
            cdmaInboundSmsHandler = this.mGsmInboundSmsHandler;
        }
        if (cdmaInboundSmsHandler != null) {
            cdmaInboundSmsHandler.sendMessage(2, inboundSmsTracker);
            return;
        }
        Rlog.e(TAG, "null handler for " + inboundSmsTracker.getFormat() + " format, can't deliver.");
    }

    private static class SmsReferenceKey {
        final String mAddress;
        final int mMessageCount;
        final int mReferenceNumber;
        final long mSubId;

        SmsReferenceKey(MtkInboundSmsTracker mtkInboundSmsTracker) {
            this.mAddress = mtkInboundSmsTracker.getAddress();
            this.mReferenceNumber = mtkInboundSmsTracker.getReferenceNumber();
            this.mMessageCount = mtkInboundSmsTracker.getMessageCount();
            this.mSubId = mtkInboundSmsTracker.getSubId();
        }

        String[] getDeleteWhereArgs() {
            return new String[]{this.mAddress, Integer.toString(this.mReferenceNumber), Integer.toString(this.mMessageCount), Long.toString(this.mSubId)};
        }

        public int hashCode() {
            return (((((int) this.mSubId) * 63) + (this.mReferenceNumber * 31) + this.mMessageCount) * 31) + this.mAddress.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof SmsReferenceKey)) {
                return false;
            }
            SmsReferenceKey smsReferenceKey = (SmsReferenceKey) obj;
            return smsReferenceKey.mAddress.equals(this.mAddress) && smsReferenceKey.mReferenceNumber == this.mReferenceNumber && smsReferenceKey.mMessageCount == this.mMessageCount && smsReferenceKey.mSubId == this.mSubId;
        }
    }
}
