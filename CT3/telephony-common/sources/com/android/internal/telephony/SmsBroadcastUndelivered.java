package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.os.UserManager;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import java.util.HashMap;
import java.util.HashSet;

public class SmsBroadcastUndelivered {
    private static final boolean DBG = true;
    static final long PARTIAL_SEGMENT_EXPIRE_AGE = 2592000000L;
    private static final String TAG = "SmsBroadcastUndelivered";
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Rlog.d(SmsBroadcastUndelivered.TAG, "Received broadcast " + intent.getAction());
            if (!"android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                return;
            }
            new ScanRawTableThread(SmsBroadcastUndelivered.this, context, null).start();
        }
    };
    private final CdmaInboundSmsHandler mCdmaInboundSmsHandler;
    private final GsmInboundSmsHandler mGsmInboundSmsHandler;
    private final Phone mPhone;
    private final ContentResolver mResolver;
    private static final String[] PDU_PENDING_MESSAGE_PROJECTION = {"pdu", "sequence", "destination_port", "date", "reference_number", "count", "address", "_id", "message_body", "sub_id"};
    private static SmsBroadcastUndelivered[] instance = new SmsBroadcastUndelivered[TelephonyManager.getDefault().getPhoneCount()];

    private class ScanRawTableThread extends Thread {
        private final Context context;

        ScanRawTableThread(SmsBroadcastUndelivered this$0, Context context, ScanRawTableThread scanRawTableThread) {
            this(context);
        }

        private ScanRawTableThread(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            SmsBroadcastUndelivered.this.scanRawTable();
            InboundSmsHandler.cancelNewMessageNotification(this.context);
        }
    }

    public static void initialize(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler, Phone phone) {
        if (instance[phone.getPhoneId()] == null) {
            Rlog.d(TAG, "Phone " + phone.getPhoneId() + " call initialize");
            instance[phone.getPhoneId()] = new SmsBroadcastUndelivered(context, gsmInboundSmsHandler, cdmaInboundSmsHandler, phone);
        }
        if (gsmInboundSmsHandler != null) {
            gsmInboundSmsHandler.sendMessage(6);
        }
        if (cdmaInboundSmsHandler == null) {
            return;
        }
        cdmaInboundSmsHandler.sendMessage(6);
    }

    private SmsBroadcastUndelivered(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler, Phone phone) {
        this.mResolver = context.getContentResolver();
        this.mGsmInboundSmsHandler = gsmInboundSmsHandler;
        this.mCdmaInboundSmsHandler = cdmaInboundSmsHandler;
        this.mPhone = phone;
        UserManager userManager = (UserManager) context.getSystemService("user");
        if (userManager.isUserUnlocked()) {
            new ScanRawTableThread(this, context, null).start();
            return;
        }
        Rlog.d(TAG, "Phone " + this.mPhone.getPhoneId() + " register user unlock event");
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_UNLOCKED");
        context.registerReceiver(this.mBroadcastReceiver, userFilter);
    }

    private void scanRawTable() {
        Rlog.d(TAG, "scanning raw table for undelivered messages");
        long startTime = System.nanoTime();
        HashMap<SmsReferenceKey, Integer> multiPartReceivedCount = new HashMap<>(4);
        HashSet<SmsReferenceKey> oldMultiPartMessages = new HashSet<>(4);
        Cursor cursor = null;
        try {
            try {
                Cursor cursor2 = this.mResolver.query(InboundSmsHandler.sRawUri, PDU_PENDING_MESSAGE_PROJECTION, "deleted = 0", null, null);
                if (cursor2 == null) {
                    Rlog.e(TAG, "error getting pending message cursor");
                    if (cursor2 != null) {
                        cursor2.close();
                    }
                    Rlog.d(TAG, "finished scanning raw table in " + ((System.nanoTime() - startTime) / 1000000) + " ms");
                    return;
                }
                boolean isCurrentFormat3gpp2 = InboundSmsHandler.isCurrentFormat3gpp2();
                while (cursor2.moveToNext()) {
                    try {
                        InboundSmsTracker tracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(cursor2, isCurrentFormat3gpp2);
                        if (tracker.getMessageCount() != 1) {
                            SmsReferenceKey reference = new SmsReferenceKey(tracker);
                            Integer receivedCount = multiPartReceivedCount.get(reference);
                            if (receivedCount == null) {
                                multiPartReceivedCount.put(reference, 1);
                                if (tracker.getTimestamp() < System.currentTimeMillis() - PARTIAL_SEGMENT_EXPIRE_AGE) {
                                    oldMultiPartMessages.add(reference);
                                }
                            } else {
                                int newCount = receivedCount.intValue() + 1;
                                if (newCount == tracker.getMessageCount()) {
                                    Rlog.d(TAG, "found complete multi-part message");
                                    if (tracker.getSubId() == this.mPhone.getSubId()) {
                                        Rlog.d(TAG, "New sms on raw table, subId: " + tracker.getSubId());
                                        broadcastSms(tracker);
                                    }
                                    oldMultiPartMessages.remove(reference);
                                } else {
                                    multiPartReceivedCount.put(reference, Integer.valueOf(newCount));
                                }
                            }
                        } else if (tracker.getSubId() == this.mPhone.getSubId()) {
                            Rlog.d(TAG, "New sms on raw table, subId: " + tracker.getSubId());
                            broadcastSms(tracker);
                        }
                    } catch (IllegalArgumentException e) {
                        Rlog.e(TAG, "error loading SmsTracker: " + e);
                    }
                }
                for (SmsReferenceKey message : oldMultiPartMessages) {
                    int rows = this.mResolver.delete(InboundSmsHandler.sRawUriPermanentDelete, InboundSmsHandler.SELECT_BY_REFERENCE, message.getDeleteWhereArgs());
                    if (rows == 0) {
                        Rlog.e(TAG, "No rows were deleted from raw table!");
                    } else {
                        Rlog.d(TAG, "Deleted " + rows + " rows from raw table for incomplete " + message.mMessageCount + " part message");
                    }
                }
                if (cursor2 != null) {
                    cursor2.close();
                }
                Rlog.d(TAG, "finished scanning raw table in " + ((System.nanoTime() - startTime) / 1000000) + " ms");
            } catch (SQLException e2) {
                Rlog.e(TAG, "error reading pending SMS messages", e2);
                if (0 != 0) {
                    cursor.close();
                }
                Rlog.d(TAG, "finished scanning raw table in " + ((System.nanoTime() - startTime) / 1000000) + " ms");
            }
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            Rlog.d(TAG, "finished scanning raw table in " + ((System.nanoTime() - startTime) / 1000000) + " ms");
            throw th;
        }
    }

    private void broadcastSms(InboundSmsTracker tracker) {
        InboundSmsHandler handler;
        if (tracker.is3gpp2()) {
            handler = this.mCdmaInboundSmsHandler;
        } else {
            handler = this.mGsmInboundSmsHandler;
        }
        if (handler != null) {
            handler.sendMessage(2, tracker);
        } else {
            Rlog.e(TAG, "null handler for " + tracker.getFormat() + " format, can't deliver.");
        }
    }

    private static class SmsReferenceKey {
        final String mAddress;
        final int mMessageCount;
        final int mReferenceNumber;
        final long mSubId;

        SmsReferenceKey(InboundSmsTracker tracker) {
            this.mAddress = tracker.getAddress();
            this.mReferenceNumber = tracker.getReferenceNumber();
            this.mMessageCount = tracker.getMessageCount();
            this.mSubId = tracker.getSubId();
        }

        String[] getDeleteWhereArgs() {
            return new String[]{this.mAddress, Integer.toString(this.mReferenceNumber), Integer.toString(this.mMessageCount), Long.toString(this.mSubId)};
        }

        public int hashCode() {
            return (((((int) this.mSubId) * 63) + (this.mReferenceNumber * 31) + this.mMessageCount) * 31) + this.mAddress.hashCode();
        }

        public boolean equals(Object obj) {
            return (obj instanceof SmsReferenceKey) && obj.mAddress.equals(this.mAddress) && obj.mReferenceNumber == this.mReferenceNumber && obj.mMessageCount == this.mMessageCount && obj.mSubId == this.mSubId;
        }
    }
}
