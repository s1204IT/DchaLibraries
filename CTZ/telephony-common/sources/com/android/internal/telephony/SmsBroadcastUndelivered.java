package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import java.util.HashMap;
import java.util.HashSet;

public class SmsBroadcastUndelivered {
    protected static final boolean DBG = true;
    protected static final long DEFAULT_PARTIAL_SEGMENT_EXPIRE_AGE = 2592000000L;
    private static final String[] PDU_PENDING_MESSAGE_PROJECTION = {"pdu", "sequence", "destination_port", "date", "reference_number", "count", "address", HbpcdLookup.ID, "message_body", "display_originating_addr"};
    private static final String TAG = "SmsBroadcastUndelivered";
    private static SmsBroadcastUndelivered instance;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Rlog.d(SmsBroadcastUndelivered.TAG, "Received broadcast " + intent.getAction());
            if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                new ScanRawTableThread(context).start();
            }
        }
    };
    protected final CdmaInboundSmsHandler mCdmaInboundSmsHandler;
    protected final GsmInboundSmsHandler mGsmInboundSmsHandler;
    protected final ContentResolver mResolver;

    private class ScanRawTableThread extends Thread {
        private final Context context;

        private ScanRawTableThread(Context context) {
            this.context = context;
        }

        @Override
        public void run() throws Throwable {
            SmsBroadcastUndelivered.this.scanRawTable(this.context);
            InboundSmsHandler.cancelNewMessageNotification(this.context);
        }
    }

    public static void initialize(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler) {
        if (instance == null) {
            instance = new SmsBroadcastUndelivered(context, gsmInboundSmsHandler, cdmaInboundSmsHandler);
        }
        if (gsmInboundSmsHandler != null) {
            gsmInboundSmsHandler.sendMessage(6);
        }
        if (cdmaInboundSmsHandler != null) {
            cdmaInboundSmsHandler.sendMessage(6);
        }
    }

    protected SmsBroadcastUndelivered(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler) {
        this.mResolver = context.getContentResolver();
        this.mGsmInboundSmsHandler = gsmInboundSmsHandler;
        this.mCdmaInboundSmsHandler = cdmaInboundSmsHandler;
        if (((UserManager) context.getSystemService("user")).isUserUnlocked()) {
            new ScanRawTableThread(context).start();
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    protected void scanRawTable(Context context) throws Throwable {
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
                cursorQuery = this.mResolver.query(InboundSmsHandler.sRawUri, PDU_PENDING_MESSAGE_PROJECTION, "deleted = 0", null, null);
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
        boolean zIsCurrentFormat3gpp2 = InboundSmsHandler.isCurrentFormat3gpp2();
        while (cursorQuery.moveToNext()) {
            try {
                InboundSmsTracker inboundSmsTrackerMakeInboundSmsTracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(cursorQuery, zIsCurrentFormat3gpp2);
                if (inboundSmsTrackerMakeInboundSmsTracker.getMessageCount() == 1) {
                    broadcastSms(inboundSmsTrackerMakeInboundSmsTracker);
                } else {
                    SmsReferenceKey smsReferenceKey = new SmsReferenceKey(inboundSmsTrackerMakeInboundSmsTracker);
                    Integer num = (Integer) map.get(smsReferenceKey);
                    if (num == null) {
                        map.put(smsReferenceKey, 1);
                        if (inboundSmsTrackerMakeInboundSmsTracker.getTimestamp() < System.currentTimeMillis() - getUndeliveredSmsExpirationTime(context)) {
                            hashSet.add(smsReferenceKey);
                        }
                    } else {
                        int iIntValue = num.intValue() + 1;
                        if (iIntValue == inboundSmsTrackerMakeInboundSmsTracker.getMessageCount()) {
                            Rlog.d(TAG, "found complete multi-part message");
                            broadcastSms(inboundSmsTrackerMakeInboundSmsTracker);
                            hashSet.remove(smsReferenceKey);
                        } else {
                            map.put(smsReferenceKey, Integer.valueOf(iIntValue));
                        }
                    }
                }
            } catch (IllegalArgumentException e3) {
                Rlog.e(TAG, "error loading SmsTracker: " + e3);
            }
        }
        for (SmsReferenceKey smsReferenceKey2 : hashSet) {
            int iDelete = this.mResolver.delete(InboundSmsHandler.sRawUriPermanentDelete, smsReferenceKey2.getDeleteWhere(), smsReferenceKey2.getDeleteWhereArgs());
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
        InboundSmsHandler inboundSmsHandler;
        if (inboundSmsTracker.is3gpp2()) {
            inboundSmsHandler = this.mCdmaInboundSmsHandler;
        } else {
            inboundSmsHandler = this.mGsmInboundSmsHandler;
        }
        if (inboundSmsHandler != null) {
            inboundSmsHandler.sendMessage(2, inboundSmsTracker);
            return;
        }
        Rlog.e(TAG, "null handler for " + inboundSmsTracker.getFormat() + " format, can't deliver.");
    }

    protected long getUndeliveredSmsExpirationTime(Context context) {
        PersistableBundle configForSubId = ((CarrierConfigManager) context.getSystemService("carrier_config")).getConfigForSubId(SubscriptionManager.getDefaultSmsSubscriptionId());
        return configForSubId != null ? configForSubId.getLong("undelivered_sms_message_expiration_time", DEFAULT_PARTIAL_SEGMENT_EXPIRE_AGE) : DEFAULT_PARTIAL_SEGMENT_EXPIRE_AGE;
    }

    private static class SmsReferenceKey {
        final String mAddress;
        final int mMessageCount;
        final String mQuery;
        final int mReferenceNumber;

        SmsReferenceKey(InboundSmsTracker inboundSmsTracker) {
            this.mAddress = inboundSmsTracker.getAddress();
            this.mReferenceNumber = inboundSmsTracker.getReferenceNumber();
            this.mMessageCount = inboundSmsTracker.getMessageCount();
            this.mQuery = inboundSmsTracker.getQueryForSegments();
        }

        String[] getDeleteWhereArgs() {
            return new String[]{this.mAddress, Integer.toString(this.mReferenceNumber), Integer.toString(this.mMessageCount)};
        }

        String getDeleteWhere() {
            return this.mQuery;
        }

        public int hashCode() {
            return (((this.mReferenceNumber * 31) + this.mMessageCount) * 31) + this.mAddress.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof SmsReferenceKey)) {
                return false;
            }
            SmsReferenceKey smsReferenceKey = (SmsReferenceKey) obj;
            return smsReferenceKey.mAddress.equals(this.mAddress) && smsReferenceKey.mReferenceNumber == this.mReferenceNumber && smsReferenceKey.mMessageCount == this.mMessageCount;
        }
    }
}
