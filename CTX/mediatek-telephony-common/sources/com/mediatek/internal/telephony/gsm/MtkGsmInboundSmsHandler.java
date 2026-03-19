package com.mediatek.internal.telephony.gsm;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import com.android.internal.telephony.BlockChecker;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.android.internal.util.HexDump;
import com.mediatek.internal.telephony.IMtkConcatenatedSmsFwk;
import com.mediatek.internal.telephony.IMtkDupSmsFilter;
import com.mediatek.internal.telephony.MtkInboundSmsTracker;
import com.mediatek.internal.telephony.MtkTimerRecord;
import com.mediatek.internal.telephony.MtkWapPushOverSms;
import com.mediatek.internal.telephony.OpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.ppl.IPplSmsFilter;
import com.mediatek.internal.telephony.ppl.PplSmsFilterExtension;
import com.mediatek.internal.telephony.util.MtkSmsCommonUtil;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import mediatek.telephony.MtkSmsMessage;

public class MtkGsmInboundSmsHandler extends GsmInboundSmsHandler {
    private static final boolean ENG = "eng".equals(Build.TYPE);
    private static final int RESULT_SMS_ACCEPT_BY_PPL = 1;
    private static final int RESULT_SMS_REJECT_BY_PPL = 0;
    private static final String SMS_CONCAT_WAIT_PROPERTY = "ro.vendor.mtk_sms_concat_wait_support";
    private IMtkConcatenatedSmsFwk mConcatenatedSmsFwk;
    private IMtkDupSmsFilter mDupSmsFilter;
    private PplSmsFilterExtension mPplSmsFilter;
    protected Object mRawLock;
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory;

    public MtkGsmInboundSmsHandler(Context context, SmsStorageMonitor smsStorageMonitor, Phone phone) {
        super("MtkGsmInboundSmsHandler-" + phone.getPhoneId(), context, smsStorageMonitor, phone);
        this.mTelephonyCustomizationFactory = null;
        this.mDupSmsFilter = null;
        this.mConcatenatedSmsFwk = null;
        this.mPplSmsFilter = null;
        this.mRawLock = new Object();
        this.mDefaultState = new MtkDefaultState();
        this.mStartupState = new MtkStartupState();
        this.mIdleState = new MtkIdleState();
        this.mDeliveringState = new MtkDeliveringState();
        this.mWaitingState = new MtkWaitingState();
        addState(this.mDefaultState);
        addState(this.mStartupState, this.mDefaultState);
        addState(this.mIdleState, this.mDefaultState);
        addState(this.mDeliveringState, this.mDefaultState);
        addState(this.mWaitingState, this.mDeliveringState);
        setInitialState(this.mStartupState);
        log("created InboundSmsHandler from MtkGsmInboundSmsHandler");
        this.mTelephonyCustomizationFactory = OpTelephonyCustomizationUtils.getOpFactory(context);
        if (this.mTelephonyCustomizationFactory != null) {
            this.mDupSmsFilter = this.mTelephonyCustomizationFactory.makeMtkDupSmsFilter(context);
            if (SystemProperties.get(SMS_CONCAT_WAIT_PROPERTY).equals("1")) {
                this.mConcatenatedSmsFwk = this.mTelephonyCustomizationFactory.makeMtkConcatenatedSmsFwk(context);
            }
        }
        if (this.mDupSmsFilter != null) {
            this.mDupSmsFilter.setPhoneId(this.mPhone.getPhoneId());
            log("initial IMtkDupSmsFilter done, actual class name is " + this.mDupSmsFilter.getClass().getName());
        } else {
            log("FAIL! intial IMtkDupSmsFilter");
        }
        if (this.mConcatenatedSmsFwk != null) {
            this.mConcatenatedSmsFwk.setPhoneId(this.mPhone.getPhoneId());
            log("initial IMtkConcatenatedSmsFwk done, actual class name is " + this.mConcatenatedSmsFwk.getClass().getName());
            return;
        }
        log("FAIL! intial IMtkConcatenatedSmsFwk");
    }

    public static MtkGsmInboundSmsHandler makeInboundSmsHandler(Context context, SmsStorageMonitor smsStorageMonitor, Phone phone) {
        MtkGsmInboundSmsHandler mtkGsmInboundSmsHandler = new MtkGsmInboundSmsHandler(context, smsStorageMonitor, phone);
        mtkGsmInboundSmsHandler.start();
        return mtkGsmInboundSmsHandler;
    }

    private class MtkDefaultState extends InboundSmsHandler.DefaultState {
        private MtkDefaultState() {
            super(MtkGsmInboundSmsHandler.this);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            return super.processMessage(message);
        }
    }

    private class MtkStartupState extends InboundSmsHandler.StartupState {
        private MtkStartupState() {
            super(MtkGsmInboundSmsHandler.this);
        }

        public boolean processMessage(Message message) {
            if (message.what == 3001) {
                MtkGsmInboundSmsHandler.this.log("MtkStartupState.processMessage:" + message.what);
                MtkGsmInboundSmsHandler.this.deferMessage(message);
                return true;
            }
            return super.processMessage(message);
        }
    }

    private class MtkIdleState extends InboundSmsHandler.IdleState {
        private MtkIdleState() {
            super(MtkGsmInboundSmsHandler.this);
        }

        public boolean processMessage(Message message) {
            if (message.what == 3001) {
                MtkGsmInboundSmsHandler.this.log("MtkIdleState.processMessage:" + message.what);
                MtkGsmInboundSmsHandler.this.deferMessage(message);
                MtkGsmInboundSmsHandler.this.transitionTo(MtkGsmInboundSmsHandler.this.mDeliveringState);
                return true;
            }
            return super.processMessage(message);
        }
    }

    private class MtkDeliveringState extends InboundSmsHandler.DeliveringState {
        private MtkDeliveringState() {
            super(MtkGsmInboundSmsHandler.this);
        }

        public boolean processMessage(Message message) {
            if (message.what == 3001) {
                MtkGsmInboundSmsHandler.this.log("MtkDeliveringState.processMessage:" + message.what);
                if (MtkGsmInboundSmsHandler.this.dispatchConcateSmsParts((MtkTimerRecord) message.obj)) {
                    MtkGsmInboundSmsHandler.this.transitionTo(MtkGsmInboundSmsHandler.this.mWaitingState);
                    return true;
                }
                MtkGsmInboundSmsHandler.this.loge("Unexpected result for dispatching SMS segments");
                MtkGsmInboundSmsHandler.this.sendMessage(4);
                return true;
            }
            return super.processMessage(message);
        }
    }

    private class MtkWaitingState extends InboundSmsHandler.WaitingState {
        private MtkWaitingState() {
            super(MtkGsmInboundSmsHandler.this);
        }

        public boolean processMessage(Message message) {
            if (message.what == 3001) {
                MtkGsmInboundSmsHandler.this.log("MtkWaitingState.processMessage:" + message.what);
                MtkGsmInboundSmsHandler.this.deferMessage(message);
                return true;
            }
            return super.processMessage(message);
        }
    }

    protected int dispatchNormalMessage(SmsMessageBase smsMessageBase) {
        int i;
        MtkInboundSmsTracker mtkInboundSmsTracker;
        SmsHeader userDataHeader = smsMessageBase.getUserDataHeader();
        if (userDataHeader == null || userDataHeader.concatRef == null || userDataHeader.concatRef.msgCount == 1) {
            if (userDataHeader == null || userDataHeader.portAddrs == null) {
                i = -1;
            } else {
                int i2 = userDataHeader.portAddrs.destPort;
                log("destination port: " + i2);
                i = i2;
            }
            mtkInboundSmsTracker = (MtkInboundSmsTracker) TelephonyComponentFactory.getInstance().makeInboundSmsTracker(smsMessageBase.getPdu(), smsMessageBase.getTimestampMillis(), i, is3gpp2(), false, smsMessageBase.getOriginatingAddress(), smsMessageBase.getDisplayOriginatingAddress(), smsMessageBase.getMessageBody());
            mtkInboundSmsTracker.setSubId(this.mPhone.getSubId());
        } else {
            SmsHeader.ConcatRef concatRef = userDataHeader.concatRef;
            SmsHeader.PortAddrs portAddrs = userDataHeader.portAddrs;
            mtkInboundSmsTracker = (MtkInboundSmsTracker) TelephonyComponentFactory.getInstance().makeInboundSmsTracker(smsMessageBase.getPdu(), smsMessageBase.getTimestampMillis(), portAddrs != null ? portAddrs.destPort : -1, is3gpp2(), smsMessageBase.getOriginatingAddress(), smsMessageBase.getDisplayOriginatingAddress(), concatRef.refNumber, concatRef.seqNumber, concatRef.msgCount, false, smsMessageBase.getMessageBody());
            mtkInboundSmsTracker.setSubId(this.mPhone.getSubId());
        }
        if (ENG) {
            log("created tracker: " + mtkInboundSmsTracker);
        }
        return addTrackerToRawTableAndSendMessage(mtkInboundSmsTracker, mtkInboundSmsTracker.getDestPort() == -1);
    }

    protected boolean dispatchConcateSmsParts(MtkTimerRecord mtkTimerRecord) {
        boolean z = false;
        if (mtkTimerRecord == null) {
            if (ENG) {
                log("ConcatenatedSmsFwkExt: null TimerRecord in msg");
            }
            return false;
        }
        if (ENG) {
            log("ConcatenatedSmsFwkExt: timer is expired, dispatch existed segments. refNumber = " + mtkTimerRecord.refNumber);
        }
        MtkInboundSmsTracker mtkInboundSmsTracker = (MtkInboundSmsTracker) mtkTimerRecord.mTracker;
        InboundSmsHandler.SmsBroadcastReceiver smsBroadcastReceiver = new InboundSmsHandler.SmsBroadcastReceiver(this, mtkInboundSmsTracker);
        synchronized (this.mRawLock) {
            byte[][] bArrQueryExistedSegments = this.mConcatenatedSmsFwk.queryExistedSegments(mtkTimerRecord);
            if (bArrQueryExistedSegments != null) {
                List listAsList = Arrays.asList(bArrQueryExistedSegments);
                if (listAsList.size() != 0 && !listAsList.contains(null)) {
                    if (checkPplPermission(bArrQueryExistedSegments, mtkInboundSmsTracker.getFormat()) != 1) {
                        log("The message was blocked by Ppl! don't prompt to user");
                        deleteFromRawTable(mtkInboundSmsTracker.getDeleteWhere(), mtkInboundSmsTracker.getDeleteWhereArgs(), 1);
                        return false;
                    }
                    if (!this.mUserManager.isUserUnlocked()) {
                        log("dispatchConcateSmsParts: device is still locked so delete segment(s), ref = " + mtkTimerRecord.refNumber);
                        this.mConcatenatedSmsFwk.deleteExistedSegments(mtkTimerRecord);
                        return processMessagePartWithUserLocked(mtkInboundSmsTracker, bArrQueryExistedSegments, -1, smsBroadcastReceiver);
                    }
                    if (BlockChecker.isBlocked(this.mContext, mtkInboundSmsTracker.getAddress())) {
                        log("dispatchConcateSmsParts: block phone number, number = " + mtkInboundSmsTracker.getAddress());
                        this.mConcatenatedSmsFwk.deleteExistedSegments(mtkTimerRecord);
                        deleteFromRawTable(mtkInboundSmsTracker.getDeleteWhere(), mtkInboundSmsTracker.getDeleteWhereArgs(), 1);
                        return false;
                    }
                    if (bArrQueryExistedSegments != null && bArrQueryExistedSegments.length > 0) {
                        if (!filterSms(bArrQueryExistedSegments, mtkInboundSmsTracker.getDestPort(), mtkInboundSmsTracker, smsBroadcastReceiver, true)) {
                            dispatchSmsDeliveryIntent(bArrQueryExistedSegments, mtkInboundSmsTracker.getFormat(), mtkInboundSmsTracker.getDestPort(), smsBroadcastReceiver);
                        }
                        z = true;
                    } else if (ENG) {
                        log("ConcatenatedSmsFwkExt: no pdus to be dispatched");
                    }
                    if (ENG) {
                        log("ConcatenatedSmsFwkExt: delete segment(s), tracker = " + ((InboundSmsTracker) mtkTimerRecord.mTracker));
                    }
                    this.mConcatenatedSmsFwk.deleteExistedSegments(mtkTimerRecord);
                    return z;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("dispatchConcateSmsParts: returning false due to ");
                sb.append(listAsList.size() == 0 ? "pduList.size() == 0" : "pduList.contains(null)");
                loge(sb.toString());
                return false;
            }
            loge("dispatchConcateSmsParts: there is at least one segment with dest port");
            return false;
        }
    }

    protected boolean processMessagePart(InboundSmsTracker inboundSmsTracker) {
        Cursor cursorQuery;
        boolean zIsBlocked;
        byte[][] bArr;
        int i;
        int realDestPort;
        int iDispatchWapPdu;
        int messageCount = inboundSmsTracker.getMessageCount();
        int destPort = inboundSmsTracker.getDestPort();
        Cursor cursor = null;
        if (messageCount == 1) {
            byte[][] bArr2 = {inboundSmsTracker.getPdu()};
            i = destPort;
            zIsBlocked = BlockChecker.isBlocked(this.mContext, inboundSmsTracker.getDisplayAddress());
            bArr = bArr2;
        } else {
            synchronized (this.mRawLock) {
                try {
                    try {
                        try {
                            cursorQuery = this.mResolver.query(sRawUri, PDU_SEQUENCE_PORT_PROJECTION, "address=? AND reference_number=? AND count=? AND deleted=0 AND sub_id=? AND (destination_port & 131072=131072)", new String[]{inboundSmsTracker.getAddress(), Integer.toString(inboundSmsTracker.getReferenceNumber()), Integer.toString(inboundSmsTracker.getMessageCount()), Integer.toString(this.mPhone.getSubId())}, null);
                        } catch (SQLException e) {
                            e = e;
                        }
                    } catch (Throwable th) {
                        th = th;
                        cursorQuery = cursor;
                    }
                    try {
                        if (cursorQuery.getCount() < messageCount) {
                            if (inboundSmsTracker.getIndexOffset() == 1 && inboundSmsTracker.getDestPort() == -1 && this.mConcatenatedSmsFwk != null) {
                                if (ENG) {
                                    log("MtkConcatenatedSmsFwk: refresh timer, ref = " + inboundSmsTracker.getReferenceNumber());
                                }
                                MtkTimerRecord mtkTimerRecordQueryTimerRecord = this.mConcatenatedSmsFwk.queryTimerRecord(inboundSmsTracker.getAddress(), inboundSmsTracker.getReferenceNumber(), inboundSmsTracker.getMessageCount(), false);
                                if (mtkTimerRecordQueryTimerRecord != null) {
                                    this.mConcatenatedSmsFwk.refreshTimer(getHandler(), mtkTimerRecordQueryTimerRecord);
                                } else if (ENG) {
                                    log("MtkConcatenatedSmsFwk: fail to get TimerRecord to refresh timer");
                                }
                            }
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return false;
                        }
                        if (inboundSmsTracker.getIndexOffset() == 1 && inboundSmsTracker.getDestPort() == -1 && this.mConcatenatedSmsFwk != null) {
                            if (ENG) {
                                log("MtkConcatenatedSmsFwk: cancel timer, ref = " + inboundSmsTracker.getReferenceNumber());
                            }
                            MtkTimerRecord mtkTimerRecordQueryTimerRecord2 = this.mConcatenatedSmsFwk.queryTimerRecord(inboundSmsTracker.getAddress(), inboundSmsTracker.getReferenceNumber(), inboundSmsTracker.getMessageCount(), false);
                            if (mtkTimerRecordQueryTimerRecord2 != null) {
                                this.mConcatenatedSmsFwk.cancelTimer(getHandler(), mtkTimerRecordQueryTimerRecord2);
                            } else if (ENG) {
                                log("MtkConcatenatedSmsFwk: fail to get TimerRecord to cancel timer");
                            }
                        }
                        byte[][] bArr3 = new byte[messageCount][];
                        zIsBlocked = false;
                        while (cursorQuery.moveToNext()) {
                            int i2 = cursorQuery.getInt(((Integer) PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(1)).intValue()) - inboundSmsTracker.getIndexOffset();
                            bArr3[i2] = HexDump.hexStringToByteArray(cursorQuery.getString(((Integer) PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(0)).intValue()));
                            if (i2 == 0 && !cursorQuery.isNull(((Integer) PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(2)).intValue()) && (realDestPort = InboundSmsTracker.getRealDestPort(cursorQuery.getInt(((Integer) PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(2)).intValue()))) != -1) {
                                destPort = realDestPort;
                            }
                            if (!zIsBlocked) {
                                zIsBlocked = BlockChecker.isBlocked(this.mContext, cursorQuery.getString(((Integer) PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(9)).intValue()));
                            }
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        bArr = bArr3;
                        i = destPort;
                    } catch (SQLException e2) {
                        e = e2;
                        cursor = cursorQuery;
                        loge("Can't access multipart SMS database", e);
                        if (cursor != null) {
                            cursor.close();
                        }
                        return false;
                    } catch (Throwable th2) {
                        th = th2;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                } finally {
                }
            }
        }
        List listAsList = Arrays.asList(bArr);
        if (listAsList.size() == 0 || listAsList.contains(null)) {
            StringBuilder sb = new StringBuilder();
            sb.append("processMessagePart: returning false due to ");
            sb.append(listAsList.size() == 0 ? "pduList.size() == 0" : "pduList.contains(null)");
            loge(sb.toString());
            return false;
        }
        BroadcastReceiver smsBroadcastReceiver = new InboundSmsHandler.SmsBroadcastReceiver(this, inboundSmsTracker);
        if (checkPplPermission(bArr, inboundSmsTracker.getFormat()) != 1) {
            log("The message was blocked by Ppl! don't prompt to user");
            deleteFromRawTable(inboundSmsTracker.getDeleteWhere(), inboundSmsTracker.getDeleteWhereArgs(), 1);
            return false;
        }
        if (!this.mUserManager.isUserUnlocked()) {
            return processMessagePartWithUserLocked(inboundSmsTracker, bArr, i, smsBroadcastReceiver);
        }
        if (i != 2948) {
            if (zIsBlocked) {
                deleteFromRawTable(inboundSmsTracker.getDeleteWhere(), inboundSmsTracker.getDeleteWhereArgs(), 1);
                return false;
            }
            if (!filterSms(bArr, i, inboundSmsTracker, smsBroadcastReceiver, true)) {
                dispatchSmsDeliveryIntent(bArr, inboundSmsTracker.getFormat(), i, smsBroadcastReceiver);
            }
            return true;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (byte[] userData : bArr) {
            if (!inboundSmsTracker.is3gpp2()) {
                MtkSmsMessage mtkSmsMessageCreateFromPdu = MtkSmsMessage.createFromPdu(userData, "3gpp");
                if (mtkSmsMessageCreateFromPdu == null) {
                    loge("processMessagePart: SmsMessage.createFromPdu returned null");
                    return false;
                }
                userData = mtkSmsMessageCreateFromPdu.getUserData();
            }
            byteArrayOutputStream.write(userData, 0, userData.length);
        }
        if (MtkSmsCommonUtil.isWapPushSupport()) {
            log("dispatch wap push pdu with addr & sc addr");
            Bundle bundle = new Bundle();
            if (((MtkInboundSmsTracker) inboundSmsTracker).is3gpp2WapPdu()) {
                bundle.putString("address", inboundSmsTracker.getAddress());
                bundle.putString("service_center", "");
            } else {
                MtkSmsMessage mtkSmsMessageCreateFromPdu2 = MtkSmsMessage.createFromPdu(bArr[0], inboundSmsTracker.getFormat());
                if (mtkSmsMessageCreateFromPdu2 != null) {
                    bundle.putString("address", mtkSmsMessageCreateFromPdu2.getOriginatingAddress());
                    String serviceCenterAddress = mtkSmsMessageCreateFromPdu2.getServiceCenterAddress();
                    if (serviceCenterAddress == null) {
                        serviceCenterAddress = "";
                    }
                    bundle.putString("service_center", serviceCenterAddress);
                }
            }
            iDispatchWapPdu = ((MtkWapPushOverSms) this.mWapPush).dispatchWapPdu(byteArrayOutputStream.toByteArray(), smsBroadcastReceiver, this, bundle);
        } else {
            log("dispatch wap push pdu");
            iDispatchWapPdu = this.mWapPush.dispatchWapPdu(byteArrayOutputStream.toByteArray(), smsBroadcastReceiver, this);
        }
        log("dispatchWapPdu() returned " + iDispatchWapPdu);
        if (iDispatchWapPdu == -1) {
            return true;
        }
        deleteFromRawTable(inboundSmsTracker.getDeleteWhere(), inboundSmsTracker.getDeleteWhereArgs(), 2);
        return false;
    }

    protected void onQuitting() {
        super.onQuitting();
    }

    public void dispatchIntent(Intent intent, String str, int i, Bundle bundle, BroadcastReceiver broadcastReceiver, UserHandle userHandle) {
        intent.putExtra("rTime", System.currentTimeMillis());
        super.dispatchIntent(intent, str, i, bundle, broadcastReceiver, userHandle);
    }

    protected void deleteFromRawTable(String str, String[] strArr, int i) {
        Uri uri = i == 1 ? sRawUriPermanentDelete : sRawUri;
        if (str == null && strArr == null) {
            loge("No rows need be deleted from raw table!");
            return;
        }
        synchronized (this.mRawLock) {
            int iDelete = this.mResolver.delete(uri, str, strArr);
            if (iDelete == 0) {
                loge("No rows were deleted from raw table!");
            } else {
                log("Deleted " + iDelete + " rows from raw table.");
            }
        }
    }

    protected boolean duplicateExists(InboundSmsTracker inboundSmsTracker) throws Throwable {
        String str;
        Cursor cursorQuery;
        String address = inboundSmsTracker.getAddress();
        String string = Integer.toString(inboundSmsTracker.getReferenceNumber());
        String string2 = Integer.toString(inboundSmsTracker.getMessageCount());
        String string3 = Integer.toString(this.mPhone.getSubId());
        String string4 = Integer.toString(inboundSmsTracker.getSequenceNumber());
        String string5 = Long.toString(inboundSmsTracker.getTimestamp());
        String messageBody = inboundSmsTracker.getMessageBody();
        if (inboundSmsTracker.getMessageCount() == 1) {
            str = "address=? AND reference_number=? AND count=? AND sequence=? AND date=? AND message_body=? AND sub_id=?";
        } else {
            str = "address=? AND reference_number=? AND count=? AND sequence=? AND ((date=? AND message_body=?) OR deleted=0) AND sub_id=?";
        }
        try {
            cursorQuery = this.mResolver.query(sRawUri, PDU_PROJECTION, str, new String[]{address, string, string2, string4, string5, messageBody, string3}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToNext()) {
                        loge("Discarding duplicate message segment, refNumber=" + string + " seqNumber=" + string4 + " count=" + string2);
                        if (ENG) {
                            loge("address=" + address + " date=" + string5 + " messageBody=" + messageBody);
                        }
                        String string6 = cursorQuery.getString(0);
                        byte[] pdu = inboundSmsTracker.getPdu();
                        byte[] bArrHexStringToByteArray = HexDump.hexStringToByteArray(string6);
                        if (!Arrays.equals(bArrHexStringToByteArray, inboundSmsTracker.getPdu())) {
                            loge("Warning: dup message segment PDU of length " + pdu.length + " is different from existing PDU of length " + bArrHexStringToByteArray.length);
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return true;
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
            return false;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    protected int addTrackerToRawTable(InboundSmsTracker inboundSmsTracker, boolean z) {
        boolean zIsFirstConcatenatedSegment;
        synchronized (this.mRawLock) {
            try {
                if (z) {
                    try {
                        if (duplicateExists(inboundSmsTracker)) {
                            return 5;
                        }
                    } catch (SQLException e) {
                        loge("Can't access SMS database", e);
                        return 2;
                    }
                } else {
                    logd("Skipped message de-duping logic");
                }
                if (inboundSmsTracker.getReferenceNumber() != -1 && this.mConcatenatedSmsFwk != null) {
                    zIsFirstConcatenatedSegment = this.mConcatenatedSmsFwk.isFirstConcatenatedSegment(inboundSmsTracker.getAddress(), inboundSmsTracker.getReferenceNumber(), false);
                } else {
                    zIsFirstConcatenatedSegment = false;
                }
                String address = inboundSmsTracker.getAddress();
                String string = Integer.toString(inboundSmsTracker.getReferenceNumber());
                String string2 = Integer.toString(inboundSmsTracker.getMessageCount());
                ContentValues contentValues = inboundSmsTracker.getContentValues();
                if (ENG) {
                    log("adding content values to raw table: " + contentValues.toString());
                }
                Uri uriInsert = this.mResolver.insert(sRawUri, contentValues);
                log("URI of new row -> " + uriInsert);
                if (inboundSmsTracker.getIndexOffset() == 1 && inboundSmsTracker.getDestPort() == -1 && zIsFirstConcatenatedSegment && this.mConcatenatedSmsFwk != null) {
                    if (ENG) {
                        log("MtkConcatenatedSmsFwk: start a new timer, the first segment, ref = " + inboundSmsTracker.getReferenceNumber());
                    }
                    this.mConcatenatedSmsFwk.startTimer(getHandler(), new MtkTimerRecord(inboundSmsTracker.getAddress(), inboundSmsTracker.getReferenceNumber(), inboundSmsTracker.getMessageCount(), inboundSmsTracker));
                }
                try {
                    long id = ContentUris.parseId(uriInsert);
                    if (inboundSmsTracker.getMessageCount() == 1) {
                        inboundSmsTracker.setDeleteWhere("_id=?", new String[]{Long.toString(id)});
                    } else {
                        inboundSmsTracker.setDeleteWhere("address=? AND reference_number=? AND count=? AND deleted=0 AND sub_id=? AND (destination_port & 131072=131072)", new String[]{address, string, string2, Integer.toString(((MtkInboundSmsTracker) inboundSmsTracker).getSubId())});
                    }
                    return 1;
                } catch (Exception e2) {
                    loge("error parsing URI for new row: " + uriInsert, e2);
                    return 2;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private int checkPplPermission(byte[][] bArr, String str) {
        if (((is3gpp2() && str.compareTo("3gpp2") == 0) || (!is3gpp2() && str.compareTo("3gpp") == 0)) && phonePrivacyLockCheck(bArr, str) != 0) {
            return 0;
        }
        return 1;
    }

    protected int phonePrivacyLockCheck(byte[][] bArr, String str) {
        if (!MtkSmsCommonUtil.isPrivacyLockSupport()) {
            return 0;
        }
        if (this.mPplSmsFilter == null) {
            this.mPplSmsFilter = new PplSmsFilterExtension(this.mContext);
        }
        Bundle bundle = new Bundle();
        PplSmsFilterExtension pplSmsFilterExtension = this.mPplSmsFilter;
        bundle.putSerializable(IPplSmsFilter.KEY_PDUS, bArr);
        PplSmsFilterExtension pplSmsFilterExtension2 = this.mPplSmsFilter;
        bundle.putString(IPplSmsFilter.KEY_FORMAT, str);
        PplSmsFilterExtension pplSmsFilterExtension3 = this.mPplSmsFilter;
        bundle.putInt(IPplSmsFilter.KEY_SUB_ID, this.mPhone.getSubId());
        PplSmsFilterExtension pplSmsFilterExtension4 = this.mPplSmsFilter;
        bundle.putInt(IPplSmsFilter.KEY_SMS_TYPE, 0);
        boolean zPplFilter = this.mPplSmsFilter.pplFilter(bundle);
        if (ENG) {
            log("[Ppl] Phone privacy check end, Need to filter(result) = " + zPplFilter);
        }
        return zPplFilter ? -1 : 0;
    }
}
