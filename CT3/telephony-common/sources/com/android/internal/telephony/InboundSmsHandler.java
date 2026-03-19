package com.android.internal.telephony;

import android.R;
import android.app.ActivityManagerNative;
import android.app.BroadcastOptions;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IDeviceIdleController;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.Telephony;
import android.service.carrier.ICarrierMessagingCallback;
import android.service.carrier.ICarrierMessagingService;
import android.service.carrier.MessagePdu;
import android.telephony.CarrierMessagingServiceManager;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.EventLog;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.HexDump;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.mediatek.common.MPlugin;
import com.mediatek.common.sms.IConcatenatedSmsFwkExt;
import com.mediatek.common.sms.TimerRecord;
import com.mediatek.internal.telephony.ppl.IPplSmsFilter;
import com.mediatek.internal.telephony.ppl.PplSmsFilterExtension;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class InboundSmsHandler extends StateMachine {
    public static final int ADDRESS_COLUMN = 6;
    public static final int COUNT_COLUMN = 5;
    public static final int DATE_COLUMN = 3;
    protected static final boolean DBG = true;
    public static final int DESTINATION_PORT_COLUMN = 2;
    private static final int EVENT_BROADCAST_COMPLETE = 3;
    public static final int EVENT_BROADCAST_SMS = 2;
    public static final int EVENT_INJECT_SMS = 8;
    public static final int EVENT_NEW_SMS = 1;
    private static final int EVENT_RELEASE_WAKELOCK = 5;
    private static final int EVENT_RETURN_TO_IDLE = 4;
    public static final int EVENT_START_ACCEPTING_SMS = 6;
    private static final int EVENT_UPDATE_PHONE_OBJECT = 7;
    public static final int ID_COLUMN = 7;
    public static final int MESSAGE_BODY_COLUMN = 8;
    private static final int NOTIFICATION_ID_NEW_MESSAGE = 1;
    private static final String NOTIFICATION_TAG = "InboundSmsHandler";
    public static final int PDU_COLUMN = 0;
    public static final int REFERENCE_NUMBER_COLUMN = 4;
    public static final String SELECT_BY_ID = "_id=?";
    public static final String SELECT_BY_REFERENCE = "address=? AND reference_number=? AND count=? AND deleted=0 AND sub_id=?";
    public static final int SEQUENCE_COLUMN = 1;
    public static final int SUB_ID_COLUMN = 9;
    private static final boolean VDBG = false;
    private static final int WAKELOCK_TIMEOUT = 3000;
    private final int DELETE_PERMANENTLY;
    private final int MARK_DELETED;
    protected CellBroadcastHandler mCellBroadcastHandler;
    private IConcatenatedSmsFwkExt mConcatenatedSmsFwkExt;
    protected final Context mContext;
    private final DefaultState mDefaultState;
    private final DeliveringState mDeliveringState;
    IDeviceIdleController mDeviceIdleController;
    private final IdleState mIdleState;
    protected Phone mPhone;
    private BroadcastReceiver mPhonePrivacyLockReceiver;
    private IPplSmsFilter mPplSmsFilter;
    protected Object mRawLock;
    private final ContentResolver mResolver;
    private final boolean mSmsReceiveDisabled;
    private final StartupState mStartupState;
    public SmsStorageMonitor mStorageMonitor;
    private UserManager mUserManager;
    private final WaitingState mWaitingState;
    private final PowerManager.WakeLock mWakeLock;
    private final WapPushOverSms mWapPush;
    private static final String[] PDU_PROJECTION = {"pdu"};
    private static final String[] PDU_SEQUENCE_PORT_PROJECTION = {"pdu", "sequence", "destination_port"};
    private static final boolean ENG = "eng".equals(Build.TYPE);
    protected static final Uri sRawUri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "raw");
    protected static final Uri sRawUriPermanentDelete = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "raw/permanentDelete");

    protected abstract void acknowledgeLastIncomingSms(boolean z, int i, Message message);

    protected abstract int dispatchMessageRadioSpecific(SmsMessageBase smsMessageBase);

    protected abstract boolean is3gpp2();

    protected InboundSmsHandler(String str, Context context, SmsStorageMonitor smsStorageMonitor, Phone phone, CellBroadcastHandler cellBroadcastHandler) {
        super(str);
        this.mDefaultState = new DefaultState(this, null);
        this.mStartupState = new StartupState(this, 0 == true ? 1 : 0);
        this.mIdleState = new IdleState(this, 0 == true ? 1 : 0);
        this.mDeliveringState = new DeliveringState(this, 0 == true ? 1 : 0);
        this.mWaitingState = new WaitingState(this, 0 == true ? 1 : 0);
        this.DELETE_PERMANENTLY = 1;
        this.MARK_DELETED = 2;
        this.mRawLock = new Object();
        this.mConcatenatedSmsFwkExt = null;
        this.mPplSmsFilter = null;
        this.mPhonePrivacyLockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String intentFormat = intent.getStringExtra("format");
                int subId = intent.getIntExtra("subscription", -1);
                if (InboundSmsHandler.ENG) {
                    InboundSmsHandler.this.log("[Moms] intentFormat =" + intentFormat + ", subId=" + subId);
                }
                if (subId != InboundSmsHandler.this.mPhone.getSubId()) {
                    return;
                }
                if ((!InboundSmsHandler.this.is3gpp2() || intentFormat.compareTo(SmsMessage.FORMAT_3GPP2) != 0) && (InboundSmsHandler.this.is3gpp2() || intentFormat.compareTo(SmsMessage.FORMAT_3GPP) != 0)) {
                    return;
                }
                if (intent.getAction().equals(Telephony.Sms.Intents.PRIVACY_LOCK_SMS_RECEIVED_ACTION) && InboundSmsHandler.this.phonePrivacyLockCheck(intent) != 0) {
                    setResultCode(101);
                } else {
                    setResultCode(100);
                }
            }
        };
        this.mContext = context;
        this.mStorageMonitor = smsStorageMonitor;
        this.mPhone = phone;
        this.mCellBroadcastHandler = cellBroadcastHandler;
        this.mResolver = context.getContentResolver();
        this.mWapPush = new WapPushOverSms(context);
        this.mSmsReceiveDisabled = TelephonyManager.from(this.mContext).getSmsReceiveCapableForPhone(this.mPhone.getPhoneId(), this.mContext.getResources().getBoolean(R.^attr-private.fromLeft)) ? VDBG : true;
        this.mWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, str);
        this.mWakeLock.acquire();
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mDeviceIdleController = TelephonyComponentFactory.getInstance().getIDeviceIdleController();
        addState(this.mDefaultState);
        addState(this.mStartupState, this.mDefaultState);
        addState(this.mIdleState, this.mDefaultState);
        addState(this.mDeliveringState, this.mDefaultState);
        addState(this.mWaitingState, this.mDeliveringState);
        setInitialState(this.mStartupState);
        log("created InboundSmsHandler");
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            try {
                this.mConcatenatedSmsFwkExt = (IConcatenatedSmsFwkExt) MPlugin.createInstance(IConcatenatedSmsFwkExt.class.getName(), this.mContext);
                if (this.mConcatenatedSmsFwkExt != null) {
                    this.mConcatenatedSmsFwkExt.setPhoneId(this.mPhone.getPhoneId());
                    log("initial IConcatenatedSmsFwkExt done, actual class name is " + this.mConcatenatedSmsFwkExt.getClass().getName());
                } else {
                    log("FAIL! intial mConcatenatedSmsFwkExt");
                }
            } catch (RuntimeException e) {
                loge("FAIL! No IConcatenatedSmsFwkExt");
            }
        }
        this.mPplSmsFilter = new PplSmsFilterExtension(this.mContext);
        if (!SmsConstants.isPrivacyLockSupport()) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Telephony.Sms.Intents.PRIVACY_LOCK_SMS_RECEIVED_ACTION);
        this.mContext.registerReceiver(this.mPhonePrivacyLockReceiver, intentFilter);
    }

    public void dispose() {
        quit();
    }

    public void updatePhoneObject(Phone phone) {
        sendMessage(7, phone);
    }

    protected void onQuitting() {
        this.mWapPush.dispose();
        while (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        if (!SmsConstants.isPrivacyLockSupport()) {
            return;
        }
        this.mContext.unregisterReceiver(this.mPhonePrivacyLockReceiver);
    }

    public Phone getPhone() {
        return this.mPhone;
    }

    private class DefaultState extends State {
        DefaultState(InboundSmsHandler this$0, DefaultState defaultState) {
            this();
        }

        private DefaultState() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 7:
                    InboundSmsHandler.this.onUpdatePhoneObject((Phone) msg.obj);
                    return true;
                default:
                    String errorText = "processMessage: unhandled message type " + msg.what + " currState=" + InboundSmsHandler.this.getCurrentState().getName();
                    if (Build.IS_DEBUGGABLE) {
                        InboundSmsHandler.this.loge("---- Dumping InboundSmsHandler ----");
                        InboundSmsHandler.this.loge("Total records=" + InboundSmsHandler.this.getLogRecCount());
                        for (int i = Math.max(InboundSmsHandler.this.getLogRecSize() - 20, 0); i < InboundSmsHandler.this.getLogRecSize(); i++) {
                            InboundSmsHandler.this.loge("Rec[%d]: %s\n" + i + InboundSmsHandler.this.getLogRec(i).toString());
                        }
                        InboundSmsHandler.this.loge("---- Dumped InboundSmsHandler ----");
                        throw new RuntimeException(errorText);
                    }
                    InboundSmsHandler.this.loge(errorText);
                    return true;
            }
        }
    }

    private class StartupState extends State {
        StartupState(InboundSmsHandler this$0, StartupState startupState) {
            this();
        }

        private StartupState() {
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler.this.log("StartupState.processMessage:" + msg.what);
            switch (msg.what) {
                case 1:
                case 2:
                case 8:
                case 3001:
                    InboundSmsHandler.this.deferMessage(msg);
                    break;
                case 6:
                    InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mIdleState);
                    break;
            }
            return true;
        }
    }

    private class IdleState extends State {
        IdleState(InboundSmsHandler this$0, IdleState idleState) {
            this();
        }

        private IdleState() {
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Idle state");
            InboundSmsHandler.this.sendMessageDelayed(5, 3000L);
        }

        public void exit() {
            InboundSmsHandler.this.mWakeLock.acquire();
            InboundSmsHandler.this.log("acquired wakelock, leaving Idle state");
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler.this.log("IdleState.processMessage:" + msg.what);
            InboundSmsHandler.this.log("Idle state processing message type " + msg.what);
            switch (msg.what) {
                case 1:
                case 2:
                case 8:
                case 3001:
                    InboundSmsHandler.this.deferMessage(msg);
                    InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mDeliveringState);
                    break;
                case 4:
                    break;
                case 5:
                    InboundSmsHandler.this.mWakeLock.release();
                    if (InboundSmsHandler.this.mWakeLock.isHeld()) {
                        InboundSmsHandler.this.log("mWakeLock is still held after release");
                    } else {
                        InboundSmsHandler.this.log("mWakeLock released");
                    }
                    break;
            }
            return true;
        }
    }

    private class DeliveringState extends State {
        DeliveringState(InboundSmsHandler this$0, DeliveringState deliveringState) {
            this();
        }

        private DeliveringState() {
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Delivering state");
        }

        public void exit() {
            InboundSmsHandler.this.log("leaving Delivering state");
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler.this.log("DeliveringState.processMessage:" + msg.what);
            switch (msg.what) {
                case 1:
                    InboundSmsHandler.this.handleNewSms((AsyncResult) msg.obj);
                    InboundSmsHandler.this.sendMessage(4);
                    return true;
                case 2:
                    InboundSmsTracker inboundSmsTracker = (InboundSmsTracker) msg.obj;
                    if (InboundSmsHandler.this.processMessagePart(inboundSmsTracker)) {
                        InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mWaitingState);
                    } else {
                        InboundSmsHandler.this.log("No broadcast sent on processing EVENT_BROADCAST_SMS in Delivering state. Return to Idle state");
                        InboundSmsHandler.this.sendMessage(4);
                    }
                    return true;
                case 4:
                    InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mIdleState);
                    return true;
                case 5:
                    InboundSmsHandler.this.mWakeLock.release();
                    if (!InboundSmsHandler.this.mWakeLock.isHeld()) {
                        InboundSmsHandler.this.loge("mWakeLock released while delivering/broadcasting!");
                    }
                    return true;
                case 8:
                    InboundSmsHandler.this.handleInjectSms((AsyncResult) msg.obj);
                    InboundSmsHandler.this.sendMessage(4);
                    return true;
                case 3001:
                    if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                        if (InboundSmsHandler.this.dispatchConcateSmsParts((TimerRecord) msg.obj)) {
                            InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mWaitingState);
                        } else {
                            InboundSmsHandler.this.loge("Unexpected result for dispatching SMS segments");
                            InboundSmsHandler.this.sendMessage(4);
                        }
                        return true;
                    }
                    return InboundSmsHandler.VDBG;
                default:
                    return InboundSmsHandler.VDBG;
            }
        }
    }

    private class WaitingState extends State {
        WaitingState(InboundSmsHandler this$0, WaitingState waitingState) {
            this();
        }

        private WaitingState() {
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler.this.log("WaitingState.processMessage:" + msg.what);
            switch (msg.what) {
                case 2:
                case 3001:
                    InboundSmsHandler.this.deferMessage(msg);
                    break;
                case 3:
                    InboundSmsHandler.this.sendMessage(4);
                    InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mDeliveringState);
                    break;
                case 4:
                    break;
            }
            return true;
        }
    }

    private void handleNewSms(AsyncResult ar) {
        int result;
        if (ar.exception != null) {
            loge("Exception processing incoming SMS: " + ar.exception);
            return;
        }
        try {
            SmsMessage sms = (SmsMessage) ar.result;
            result = dispatchMessage(sms.mWrappedSmsMessage);
        } catch (RuntimeException ex) {
            loge("Exception dispatching message", ex);
            result = 2;
        }
        if (result == -1) {
            return;
        }
        boolean handled = result != 1 ? VDBG : true;
        notifyAndAcknowledgeLastIncomingSms(handled, result, null);
    }

    private void handleInjectSms(AsyncResult ar) {
        int result;
        PendingIntent receivedIntent = null;
        try {
            receivedIntent = (PendingIntent) ar.userObj;
            SmsMessage sms = (SmsMessage) ar.result;
            if (sms == null) {
                result = 2;
            } else {
                result = dispatchMessage(sms.mWrappedSmsMessage);
            }
        } catch (RuntimeException ex) {
            loge("Exception dispatching message", ex);
            result = 2;
        }
        if (receivedIntent == null) {
            return;
        }
        try {
            receivedIntent.send(result);
        } catch (PendingIntent.CanceledException e) {
        }
    }

    private int dispatchMessage(SmsMessageBase smsb) {
        if (smsb == null) {
            loge("dispatchSmsMessage: message is null");
            return 2;
        }
        if (this.mSmsReceiveDisabled) {
            log("Received short message on device which doesn't support receiving SMS. Ignored.");
            return 1;
        }
        boolean onlyCore = VDBG;
        try {
            onlyCore = IPackageManager.Stub.asInterface(ServiceManager.getService(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME)).isOnlyCoreApps();
        } catch (RemoteException e) {
        }
        if (onlyCore) {
            log("Received a short message in encrypted state. Rejecting.");
            return 2;
        }
        return dispatchMessageRadioSpecific(smsb);
    }

    protected void onUpdatePhoneObject(Phone phone) {
        this.mPhone = phone;
        this.mStorageMonitor = this.mPhone.mSmsStorageMonitor;
        log("onUpdatePhoneObject: phone=" + this.mPhone.getClass().getSimpleName());
    }

    private void notifyAndAcknowledgeLastIncomingSms(boolean success, int result, Message response) {
        if (!success) {
            Intent intent = new Intent(Telephony.Sms.Intents.SMS_REJECTED_ACTION);
            intent.putExtra("result", result);
            this.mContext.sendBroadcast(intent, "android.permission.RECEIVE_SMS");
        }
        acknowledgeLastIncomingSms(success, result, response);
    }

    protected int dispatchNormalMessage(SmsMessageBase sms) {
        InboundSmsTracker tracker;
        SmsHeader smsHeader = sms.getUserDataHeader();
        if (smsHeader == null || smsHeader.concatRef == null) {
            int destPort = -1;
            if (smsHeader != null && smsHeader.portAddrs != null) {
                destPort = smsHeader.portAddrs.destPort;
                log("destination port: " + destPort);
            }
            tracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(this.mPhone.getSubId(), sms.getPdu(), sms.getTimestampMillis(), destPort, is3gpp2(), VDBG, sms.getDisplayOriginatingAddress(), sms.getMessageBody());
        } else {
            SmsHeader.ConcatRef concatRef = smsHeader.concatRef;
            SmsHeader.PortAddrs portAddrs = smsHeader.portAddrs;
            tracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(this.mPhone.getSubId(), sms.getPdu(), sms.getTimestampMillis(), portAddrs != null ? portAddrs.destPort : -1, is3gpp2(), sms.getDisplayOriginatingAddress(), concatRef.refNumber, concatRef.seqNumber, concatRef.msgCount, VDBG, sms.getMessageBody());
        }
        return addTrackerToRawTableAndSendMessage(tracker, tracker.getDestPort() == -1 ? true : VDBG);
    }

    protected int addTrackerToRawTableAndSendMessage(InboundSmsTracker tracker, boolean deDup) {
        switch (addTrackerToRawTable(tracker, deDup)) {
            case 1:
                sendMessage(2, tracker);
                return 1;
            case 5:
                return 1;
            default:
                return 2;
        }
    }

    private boolean processMessagePart(InboundSmsTracker tracker) {
        byte[][] pdus;
        int result;
        int messageCount = tracker.getMessageCount();
        int destPort = tracker.getDestPort();
        if (messageCount <= 0) {
            EventLog.writeEvent(1397638484, "72298611", -1, String.format("processMessagePart: invalid messageCount = %d", Integer.valueOf(messageCount)));
            return VDBG;
        }
        if (messageCount == 1) {
            pdus = new byte[][]{tracker.getPdu()};
        } else {
            synchronized (this.mRawLock) {
                Cursor cursor = null;
                try {
                    try {
                        String address = tracker.getAddress();
                        String refNumber = Integer.toString(tracker.getReferenceNumber());
                        String count = Integer.toString(tracker.getMessageCount());
                        String subId = Integer.toString(this.mPhone.getSubId());
                        String[] whereArgs = {address, refNumber, count, subId};
                        Cursor cursor2 = this.mResolver.query(sRawUri, PDU_SEQUENCE_PORT_PROJECTION, SELECT_BY_REFERENCE, whereArgs, null);
                        int cursorCount = cursor2.getCount();
                        if (cursorCount < messageCount) {
                            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1") && tracker.getIndexOffset() == 1 && tracker.getDestPort() == -1) {
                                if (ENG) {
                                    log("ConcatenatedSmsFwkExt: refresh timer, ref = " + tracker.getReferenceNumber());
                                }
                                TimerRecord record = this.mConcatenatedSmsFwkExt.queryTimerRecord(tracker.getAddress(), tracker.getReferenceNumber(), tracker.getMessageCount());
                                if (record != null) {
                                    this.mConcatenatedSmsFwkExt.refreshTimer(getHandler(), record);
                                } else if (ENG) {
                                    log("ConcatenatedSmsFwkExt: fail to get TimerRecord to refresh timer");
                                }
                            }
                            if (cursor2 != null) {
                                cursor2.close();
                            }
                            return VDBG;
                        }
                        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1") && tracker.getIndexOffset() == 1 && tracker.getDestPort() == -1) {
                            if (ENG) {
                                log("ConcatenatedSmsFwkExt: cancel timer, ref = " + tracker.getReferenceNumber());
                            }
                            TimerRecord record2 = this.mConcatenatedSmsFwkExt.queryTimerRecord(tracker.getAddress(), tracker.getReferenceNumber(), tracker.getMessageCount());
                            if (record2 != null) {
                                this.mConcatenatedSmsFwkExt.cancelTimer(getHandler(), record2);
                            } else if (ENG) {
                                log("ConcatenatedSmsFwkExt: fail to get TimerRecord to cancel timer");
                            }
                        }
                        pdus = new byte[messageCount][];
                        while (cursor2.moveToNext()) {
                            int index = cursor2.getInt(1) - tracker.getIndexOffset();
                            if (index >= pdus.length || index < 0) {
                                EventLog.writeEvent(1397638484, "72298611", -1, String.format("processMessagePart: invalid seqNumber = %d, messageCount = %d", Integer.valueOf(tracker.getIndexOffset() + index), Integer.valueOf(messageCount)));
                            } else {
                                pdus[index] = HexDump.hexStringToByteArray(cursor2.getString(0));
                                if (index == 0 && !cursor2.isNull(2)) {
                                    int port = InboundSmsTracker.getRealDestPort(cursor2.getInt(2));
                                    if (port != -1) {
                                        destPort = port;
                                    }
                                }
                            }
                        }
                        if (cursor2 != null) {
                            cursor2.close();
                        }
                    } catch (SQLException e) {
                        loge("Can't access multipart SMS database", e);
                    }
                } finally {
                    if (0 != 0) {
                        cursor.close();
                    }
                }
            }
        }
        List<byte[]> pduList = Arrays.asList(pdus);
        if (pduList.size() == 0 || pduList.contains(null)) {
            loge("processMessagePart: returning false due to " + (pduList.size() == 0 ? "pduList.size() == 0" : "pduList.contains(null)"));
            return VDBG;
        }
        if (!this.mUserManager.isUserUnlocked()) {
            return processMessagePartWithUserLocked(tracker, pdus, destPort);
        }
        SmsBroadcastReceiver resultReceiver = new SmsBroadcastReceiver(tracker);
        if (destPort != 2948) {
            if (BlockChecker.isBlocked(this.mContext, tracker.getAddress())) {
                deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(), 1);
                return VDBG;
            }
            boolean carrierAppInvoked = filterSmsWithCarrierOrSystemApp(pdus, destPort, tracker, resultReceiver, true);
            if (carrierAppInvoked) {
                return true;
            }
            dispatchSmsDeliveryIntent(pdus, tracker.getFormat(), destPort, resultReceiver, 0);
            return true;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] pdu : pdus) {
            if (!tracker.is3gpp2()) {
                SmsMessage msg = SmsMessage.createFromPdu(pdu, SmsMessage.FORMAT_3GPP);
                if (msg == null) {
                    loge("processMessagePart: SmsMessage.createFromPdu returned null");
                    return VDBG;
                }
                pdu = msg.getUserData();
            }
            output.write(pdu, 0, pdu.length);
        }
        if (SmsConstants.isWapPushSupport()) {
            log("dispatch wap push pdu with addr & sc addr");
            Bundle bundle = new Bundle();
            if (tracker.is3gpp2WapPdu()) {
                bundle.putString("address", tracker.getAddress());
                bundle.putString("service_center", UsimPBMemInfo.STRING_NOT_SET);
            } else {
                SmsMessage sms = SmsMessage.createFromPdu(pdus[0], tracker.getFormat());
                if (sms != null) {
                    bundle.putString("address", sms.getOriginatingAddress());
                    String sca = sms.getServiceCenterAddress();
                    if (sca == null) {
                        sca = UsimPBMemInfo.STRING_NOT_SET;
                    }
                    bundle.putString("service_center", sca);
                }
            }
            result = this.mWapPush.dispatchWapPdu(output.toByteArray(), resultReceiver, this, bundle);
        } else {
            log("dispatch wap push pdu");
            result = this.mWapPush.dispatchWapPdu(output.toByteArray(), resultReceiver, this);
        }
        log("dispatchWapPdu() returned " + result);
        if (result == -1) {
            return true;
        }
        deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(), 2);
        return VDBG;
    }

    private boolean processMessagePartWithUserLocked(InboundSmsTracker tracker, byte[][] pdus, int destPort) {
        log("Credential-encrypted storage not available. Port: " + destPort);
        if (destPort == 2948 && this.mWapPush.isWapPushForMms(pdus[0], this)) {
            showNewMessageNotification();
            return VDBG;
        }
        if (destPort != -1) {
            return VDBG;
        }
        boolean carrierAppInvoked = filterSmsWithCarrierOrSystemApp(pdus, destPort, tracker, null, VDBG);
        if (carrierAppInvoked) {
            return true;
        }
        showNewMessageNotification();
        return VDBG;
    }

    private void showNewMessageNotification() {
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            log("Show new message notification.");
            Intent intent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_MESSAGING");
            Notification.Builder mBuilder = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.sym_action_chat).setAutoCancel(true).setVisibility(1).setDefaults(-1).setContentTitle(this.mContext.getString(R.string.matches_found)).setContentText(this.mContext.getString(R.string.maximize_button_text)).setContentIntent(PendingIntent.getActivity(this.mContext, 1, intent, 0));
            NotificationManager mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            mNotificationManager.notify(NOTIFICATION_TAG, 1, mBuilder.build());
        }
    }

    static void cancelNewMessageNotification(Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService("notification");
        mNotificationManager.cancel(NOTIFICATION_TAG, 1);
    }

    private boolean filterSmsWithCarrierOrSystemApp(byte[][] pdus, int destPort, InboundSmsTracker tracker, SmsBroadcastReceiver resultReceiver, boolean userUnlocked) {
        List<String> carrierPackages = null;
        UiccCard card = UiccController.getInstance().getUiccCard(this.mPhone.getPhoneId());
        if (card != null) {
            carrierPackages = card.getCarrierPackageNamesForIntent(this.mContext.getPackageManager(), new Intent("android.service.carrier.CarrierMessagingService"));
        } else {
            loge("UiccCard not initialized.");
        }
        List<String> systemPackages = getSystemAppForIntent(new Intent("android.service.carrier.CarrierMessagingService"));
        if (carrierPackages != null && carrierPackages.size() == 1) {
            log("Found carrier package.");
            CarrierSmsFilter smsFilter = new CarrierSmsFilter(pdus, destPort, tracker.getFormat(), resultReceiver, 0);
            CarrierSmsFilterCallback smsFilterCallback = new CarrierSmsFilterCallback(smsFilter, userUnlocked);
            smsFilter.filterSms(carrierPackages.get(0), smsFilterCallback);
            return true;
        }
        if (systemPackages != null && systemPackages.size() == 1) {
            log("Found system package.");
            CarrierSmsFilter smsFilter2 = new CarrierSmsFilter(pdus, destPort, tracker.getFormat(), resultReceiver, 0);
            CarrierSmsFilterCallback smsFilterCallback2 = new CarrierSmsFilterCallback(smsFilter2, userUnlocked);
            smsFilter2.filterSms(systemPackages.get(0), smsFilterCallback2);
            return true;
        }
        logv("Unable to find carrier package: " + carrierPackages + ", nor systemPackages: " + systemPackages);
        return VDBG;
    }

    private List<String> getSystemAppForIntent(Intent intent) {
        List<String> packages = new ArrayList<>();
        PackageManager packageManager = this.mContext.getPackageManager();
        List<ResolveInfo> receivers = packageManager.queryIntentServices(intent, 0);
        for (ResolveInfo info : receivers) {
            if (info.serviceInfo == null) {
                loge("Can't get service information from " + info);
            } else {
                String packageName = info.serviceInfo.packageName;
                if (packageManager.checkPermission("android.permission.CARRIER_FILTER_SMS", packageName) == 0) {
                    packages.add(packageName);
                    log("getSystemAppForIntent: added package " + packageName);
                }
            }
        }
        return packages;
    }

    public void dispatchIntent(Intent intent, String permission, int appOp, Bundle opts, BroadcastReceiver resultReceiver, UserHandle user) {
        UserInfo info;
        intent.addFlags(134217728);
        intent.putExtra("rTime", System.currentTimeMillis());
        String action = intent.getAction();
        if (Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action) || Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action) || Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION.equals(action) || Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION.equals(action)) {
            intent.addFlags(268435456);
        }
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        if (!user.equals(UserHandle.ALL)) {
            this.mContext.sendOrderedBroadcastAsUser(intent, user, permission, appOp, opts, resultReceiver, getHandler(), -1, null, null);
            return;
        }
        int[] users = null;
        try {
            users = ActivityManagerNative.getDefault().getRunningUserIds();
        } catch (RemoteException e) {
        }
        if (users == null) {
            users = new int[]{user.getIdentifier()};
        }
        for (int i = users.length - 1; i >= 0; i--) {
            UserHandle targetUser = new UserHandle(users[i]);
            if (users[i] == 0 || (!this.mUserManager.hasUserRestriction("no_sms", targetUser) && (info = this.mUserManager.getUserInfo(users[i])) != null && !info.isManagedProfile())) {
                this.mContext.sendOrderedBroadcastAsUser(intent, targetUser, permission, appOp, opts, users[i] == 0 ? resultReceiver : null, getHandler(), -1, null, null);
            }
        }
    }

    private void deleteFromRawTable(String deleteWhere, String[] deleteWhereArgs, int deleteType) {
        Uri uri = deleteType == 1 ? sRawUriPermanentDelete : sRawUri;
        if (deleteWhere == null && deleteWhereArgs == null) {
            loge("No rows need be deleted from raw table!");
            return;
        }
        synchronized (this.mRawLock) {
            int rows = this.mResolver.delete(uri, deleteWhere, deleteWhereArgs);
            if (rows == 0) {
                loge("No rows were deleted from raw table!");
            } else {
                log("Deleted " + rows + " rows from raw table.");
            }
        }
    }

    private Bundle handleSmsWhitelisting(ComponentName target) {
        String pkgName;
        String reason;
        if (target != null) {
            pkgName = target.getPackageName();
            reason = "sms-app";
        } else {
            pkgName = this.mContext.getPackageName();
            reason = "sms-broadcast";
        }
        try {
            long duration = this.mDeviceIdleController.addPowerSaveTempWhitelistAppForSms(pkgName, 0, reason);
            BroadcastOptions bopts = BroadcastOptions.makeBasic();
            bopts.setTemporaryAppWhitelistDuration(duration);
            return bopts.toBundle();
        } catch (RemoteException e) {
            return null;
        }
    }

    private void dispatchSmsDeliveryIntent(byte[][] bArr, String format, int destPort, BroadcastReceiver resultReceiver, int longSmsUploadFlag) {
        SmsHeader udh;
        Uri uri;
        Intent intent = new Intent();
        intent.putExtra(IPplSmsFilter.KEY_PDUS, (Serializable) bArr);
        intent.putExtra("format", format);
        if (destPort == -1) {
            intent.setAction(Telephony.Sms.Intents.SMS_DELIVER_ACTION);
            ComponentName componentName = SmsApplication.getDefaultSmsApplication(this.mContext, true);
            if (componentName != null) {
                intent.setComponent(componentName);
                log("Delivering SMS to: " + componentName.getPackageName() + " " + componentName.getClassName());
            } else {
                intent.setComponent(null);
            }
            if (SmsManager.getDefault().getAutoPersisting() && (uri = writeInboxMessage(intent)) != null) {
                intent.putExtra("uri", uri.toString());
            }
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                int uploadFlag = longSmsUploadFlag;
                if (longSmsUploadFlag == 0) {
                    uploadFlag = 1;
                    SmsMessage msg = SmsMessage.createFromPdu(bArr[0], format);
                    if (msg != null && (udh = msg.getUserDataHeader()) != null && udh.concatRef != null) {
                        TimerRecord tr = new TimerRecord(msg.getOriginatingAddress(), udh.concatRef.refNumber, udh.concatRef.msgCount, (Object) null);
                        synchronized (this.mRawLock) {
                            uploadFlag = this.mConcatenatedSmsFwkExt.getUploadFlag(tr);
                        }
                    }
                    if (ENG) {
                        log("uploadFlag=" + uploadFlag);
                    }
                }
                if (uploadFlag == 2 || uploadFlag == 1) {
                    intent.putExtra(Telephony.Sms.UPLOAD_FLAG, uploadFlag);
                }
            }
            if (SmsConstants.isPrivacyLockSupport()) {
                intent.setAction(Telephony.Sms.Intents.PRIVACY_LOCK_SMS_RECEIVED_ACTION);
                intent.setComponent(null);
            }
        } else {
            intent.setAction(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION);
            intent.setData(Uri.parse("sms://localhost:" + destPort));
            intent.setComponent(null);
        }
        Bundle options = handleSmsWhitelisting(intent.getComponent());
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, options, resultReceiver, UserHandle.SYSTEM);
    }

    private int addTrackerToRawTable(InboundSmsTracker tracker, boolean deDup) {
        String address = tracker.getAddress();
        String refNumber = Integer.toString(tracker.getReferenceNumber());
        String count = Integer.toString(tracker.getMessageCount());
        String subId = Integer.toString(this.mPhone.getSubId());
        synchronized (this.mRawLock) {
            if (deDup) {
                Cursor cursor = null;
                try {
                    try {
                        int sequence = tracker.getSequenceNumber();
                        String seqNumber = Integer.toString(sequence);
                        String date = Long.toString(tracker.getTimestamp());
                        String messageBody = tracker.getMessageBody();
                        cursor = this.mResolver.query(sRawUri, PDU_PROJECTION, "address=? AND reference_number=? AND count=? AND sequence=? AND date=? AND message_body=? AND sub_id=?", new String[]{address, refNumber, count, seqNumber, date, messageBody, subId}, null);
                        if (cursor.moveToNext()) {
                            loge("Discarding duplicate message segment, refNumber=" + refNumber + " seqNumber=" + seqNumber + " count=" + count);
                            String oldPduString = cursor.getString(0);
                            byte[] pdu = tracker.getPdu();
                            byte[] oldPdu = HexDump.hexStringToByteArray(oldPduString);
                            if (!Arrays.equals(oldPdu, tracker.getPdu())) {
                                loge("Warning: dup message segment PDU of length " + pdu.length + " is different from existing PDU of length " + oldPdu.length);
                            }
                            if (cursor != null) {
                                cursor.close();
                            }
                            return 5;
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    } catch (SQLException e) {
                        loge("Can't access SMS database", e);
                        if (cursor == null) {
                            return 2;
                        }
                        cursor.close();
                        return 2;
                    }
                } catch (Throwable th) {
                    if (cursor != null) {
                    }
                    throw th;
                }
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
            logd("Skipped message de-duping logic");
            boolean isFirstSegment = VDBG;
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1") && tracker.getReferenceNumber() != -1) {
                isFirstSegment = this.mConcatenatedSmsFwkExt.isFirstConcatenatedSegment(tracker.getAddress(), tracker.getReferenceNumber());
            }
            ContentValues values = tracker.getContentValues();
            Uri newUri = this.mResolver.insert(sRawUri, values);
            log("URI of new row -> " + newUri);
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1") && tracker.getIndexOffset() == 1 && tracker.getDestPort() == -1 && isFirstSegment) {
                if (ENG) {
                    log("ConcatenatedSmsFwkExt: start a new timer, the first segment, ref = " + tracker.getReferenceNumber());
                }
                TimerRecord record = new TimerRecord(tracker.getAddress(), tracker.getReferenceNumber(), tracker.getMessageCount(), tracker);
                if (record == null && ENG) {
                    log("ConcatenatedSmsFwkExt: fail to new TimerRecord to start timer");
                }
                this.mConcatenatedSmsFwkExt.startTimer(getHandler(), record);
            }
            try {
                long rowId = ContentUris.parseId(newUri);
                if (tracker.getMessageCount() == 1) {
                    tracker.setDeleteWhere(SELECT_BY_ID, new String[]{Long.toString(rowId)});
                } else {
                    String[] deleteWhereArgs = {address, refNumber, count, subId};
                    tracker.setDeleteWhere(SELECT_BY_REFERENCE, deleteWhereArgs);
                }
                return 1;
            } catch (Exception e2) {
                loge("error parsing URI for new row: " + newUri, e2);
                return 2;
            }
        }
    }

    static boolean isCurrentFormat3gpp2() {
        int activePhone = TelephonyManager.getDefault().getCurrentPhoneType();
        if (2 == activePhone) {
            return true;
        }
        return VDBG;
    }

    private final class SmsBroadcastReceiver extends BroadcastReceiver {
        private long mBroadcastTimeNano = System.nanoTime();
        private final String mDeleteWhere;
        private final String[] mDeleteWhereArgs;

        SmsBroadcastReceiver(InboundSmsTracker tracker) {
            this.mDeleteWhere = tracker.getDeleteWhere();
            this.mDeleteWhereArgs = tracker.getDeleteWhereArgs();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
                intent.setAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
                intent.setComponent(null);
                Bundle options = InboundSmsHandler.this.handleSmsWhitelisting(null);
                InboundSmsHandler.this.dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, options, this, UserHandle.ALL);
                return;
            }
            if (action.equals(Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION)) {
                intent.setAction(Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION);
                intent.setComponent(null);
                Bundle options2 = null;
                try {
                    long duration = InboundSmsHandler.this.mDeviceIdleController.addPowerSaveTempWhitelistAppForMms(InboundSmsHandler.this.mContext.getPackageName(), 0, "mms-broadcast");
                    BroadcastOptions bopts = BroadcastOptions.makeBasic();
                    bopts.setTemporaryAppWhitelistDuration(duration);
                    options2 = bopts.toBundle();
                } catch (RemoteException e) {
                }
                String mimeType = intent.getType();
                InboundSmsHandler.this.dispatchIntent(intent, WapPushOverSms.getPermissionForType(mimeType), WapPushOverSms.getAppOpsPermissionForIntent(mimeType), options2, this, UserHandle.SYSTEM);
                return;
            }
            if (action.equals(Telephony.Sms.Intents.PRIVACY_LOCK_SMS_RECEIVED_ACTION)) {
                Bundle options3 = InboundSmsHandler.this.handleSmsWhitelisting(null);
                int rc = getResultCode();
                if (rc == 101) {
                    InboundSmsHandler.this.log("[PPL] Reject by phone privacy lock and delete from raw table. Result code:" + rc);
                    InboundSmsHandler.this.deleteFromRawTable(this.mDeleteWhere, this.mDeleteWhereArgs, 2);
                    InboundSmsHandler.this.sendMessage(3);
                    return;
                }
                if (InboundSmsHandler.ENG) {
                    InboundSmsHandler.this.log("[PPL] Permit to dispatch, send sms default application first. Result code:" + rc);
                }
                intent.setAction(Telephony.Sms.Intents.SMS_DELIVER_ACTION);
                ComponentName componentName = SmsApplication.getDefaultSmsApplication(InboundSmsHandler.this.mContext, true);
                if (componentName != null) {
                    intent.setComponent(componentName);
                    InboundSmsHandler.this.log("Delivering SMS to: " + componentName.getPackageName() + " " + componentName.getClassName());
                }
                InboundSmsHandler.this.dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, options3, this, UserHandle.OWNER);
                return;
            }
            if (!Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION.equals(action) && !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action) && !Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION.equals(action) && !Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION.equals(action)) {
                InboundSmsHandler.this.loge("unexpected BroadcastReceiver action: " + action);
            }
            int rc2 = getResultCode();
            if (rc2 != -1 && rc2 != 1) {
                InboundSmsHandler.this.loge("a broadcast receiver set the result code to " + rc2 + ", deleting from raw table anyway!");
            } else {
                InboundSmsHandler.this.log("successful broadcast, deleting from raw table.");
            }
            InboundSmsHandler.this.deleteFromRawTable(this.mDeleteWhere, this.mDeleteWhereArgs, 2);
            InboundSmsHandler.this.sendMessage(3);
            int durationMillis = (int) ((System.nanoTime() - this.mBroadcastTimeNano) / 1000000);
            if (durationMillis >= 5000) {
                InboundSmsHandler.this.loge("Slow ordered broadcast completion time: " + durationMillis + " ms");
            } else {
                InboundSmsHandler.this.log("ordered broadcast completed in: " + durationMillis + " ms");
            }
        }
    }

    private final class CarrierSmsFilter extends CarrierMessagingServiceManager {
        private final int mDestPort;
        private final byte[][] mPdus;
        private final SmsBroadcastReceiver mSmsBroadcastReceiver;
        private volatile CarrierSmsFilterCallback mSmsFilterCallback;
        private final String mSmsFormat;
        private final int mUploadFlag;

        CarrierSmsFilter(InboundSmsHandler this$0, byte[][] pdus, int destPort, String smsFormat, SmsBroadcastReceiver smsBroadcastReceiver) {
            this(pdus, destPort, smsFormat, smsBroadcastReceiver, 0);
        }

        CarrierSmsFilter(byte[][] pdus, int destPort, String smsFormat, SmsBroadcastReceiver smsBroadcastReceiver, int uploadFlag) {
            this.mPdus = pdus;
            this.mDestPort = destPort;
            this.mSmsFormat = smsFormat;
            this.mSmsBroadcastReceiver = smsBroadcastReceiver;
            this.mUploadFlag = uploadFlag;
        }

        void filterSms(String carrierPackageName, CarrierSmsFilterCallback smsFilterCallback) {
            this.mSmsFilterCallback = smsFilterCallback;
            if (!bindToCarrierMessagingService(InboundSmsHandler.this.mContext, carrierPackageName)) {
                InboundSmsHandler.this.loge("bindService() for carrier messaging service failed");
                smsFilterCallback.onFilterComplete(0);
            } else {
                InboundSmsHandler.this.logv("bindService() for carrier messaging service succeeded");
            }
        }

        @Override
        protected void onServiceReady(ICarrierMessagingService carrierMessagingService) {
            try {
                carrierMessagingService.filterSms(new MessagePdu(Arrays.asList(this.mPdus)), this.mSmsFormat, this.mDestPort, InboundSmsHandler.this.mPhone.getSubId(), this.mSmsFilterCallback);
            } catch (RemoteException e) {
                InboundSmsHandler.this.loge("Exception filtering the SMS: " + e);
                this.mSmsFilterCallback.onFilterComplete(0);
            }
        }
    }

    private final class CarrierSmsFilterCallback extends ICarrierMessagingCallback.Stub {
        private final CarrierSmsFilter mSmsFilter;
        private final boolean mUserUnlocked;

        CarrierSmsFilterCallback(CarrierSmsFilter smsFilter, boolean userUnlocked) {
            this.mSmsFilter = smsFilter;
            this.mUserUnlocked = userUnlocked;
        }

        public void onFilterComplete(int result) {
            this.mSmsFilter.disposeConnection(InboundSmsHandler.this.mContext);
            InboundSmsHandler.this.logv("onFilterComplete: result is " + result);
            if ((result & 1) == 0) {
                if (this.mUserUnlocked) {
                    InboundSmsHandler.this.dispatchSmsDeliveryIntent(this.mSmsFilter.mPdus, this.mSmsFilter.mSmsFormat, this.mSmsFilter.mDestPort, this.mSmsFilter.mSmsBroadcastReceiver, this.mSmsFilter.mUploadFlag);
                    return;
                }
                if (!InboundSmsHandler.this.isSkipNotifyFlagSet(result)) {
                    InboundSmsHandler.this.showNewMessageNotification();
                }
                InboundSmsHandler.this.sendMessage(3);
                return;
            }
            long token = Binder.clearCallingIdentity();
            try {
                InboundSmsHandler.this.deleteFromRawTable(this.mSmsFilter.mSmsBroadcastReceiver.mDeleteWhere, this.mSmsFilter.mSmsBroadcastReceiver.mDeleteWhereArgs, 2);
                Binder.restoreCallingIdentity(token);
                InboundSmsHandler.this.sendMessage(3);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        }

        public void onSendSmsComplete(int result, int messageRef) {
            InboundSmsHandler.this.loge("Unexpected onSendSmsComplete call with result: " + result);
        }

        public void onSendMultipartSmsComplete(int result, int[] messageRefs) {
            InboundSmsHandler.this.loge("Unexpected onSendMultipartSmsComplete call with result: " + result);
        }

        public void onSendMmsComplete(int result, byte[] sendConfPdu) {
            InboundSmsHandler.this.loge("Unexpected onSendMmsComplete call with result: " + result);
        }

        public void onDownloadMmsComplete(int result) {
            InboundSmsHandler.this.loge("Unexpected onDownloadMmsComplete call with result: " + result);
        }
    }

    private boolean isSkipNotifyFlagSet(int callbackResult) {
        if ((callbackResult & 2) > 0) {
            return true;
        }
        return VDBG;
    }

    protected void log(String s) {
        Rlog.d(getName(), s);
    }

    protected void loge(String s) {
        Rlog.e(getName(), s);
    }

    protected void loge(String s, Throwable e) {
        Rlog.e(getName(), s, e);
    }

    private Uri writeInboxMessage(Intent intent) {
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length < 1) {
            loge("Failed to parse SMS pdu");
            return null;
        }
        for (SmsMessage sms : messages) {
            try {
                sms.getDisplayMessageBody();
            } catch (NullPointerException e) {
                loge("NPE inside SmsMessage");
                return null;
            }
        }
        ContentValues values = parseSmsMessage(messages);
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mContext.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
        } catch (Exception e2) {
            loge("Failed to persist inbox message", e2);
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static ContentValues parseSmsMessage(SmsMessage[] msgs) {
        SmsMessage sms = msgs[0];
        ContentValues values = new ContentValues();
        values.put("address", sms.getDisplayOriginatingAddress());
        values.put("body", buildMessageBodyFromPdus(msgs));
        values.put("date_sent", Long.valueOf(sms.getTimestampMillis()));
        values.put("date", Long.valueOf(System.currentTimeMillis()));
        values.put("protocol", Integer.valueOf(sms.getProtocolIdentifier()));
        values.put("seen", (Integer) 0);
        values.put("read", (Integer) 0);
        String subject = sms.getPseudoSubject();
        if (!TextUtils.isEmpty(subject)) {
            values.put("subject", subject);
        }
        values.put(Telephony.TextBasedSmsColumns.REPLY_PATH_PRESENT, Integer.valueOf(sms.isReplyPathPresent() ? 1 : 0));
        values.put("service_center", sms.getServiceCenterAddress());
        return values;
    }

    private static String buildMessageBodyFromPdus(SmsMessage[] msgs) {
        if (msgs.length == 1) {
            return replaceFormFeeds(msgs[0].getDisplayMessageBody());
        }
        StringBuilder body = new StringBuilder();
        for (SmsMessage msg : msgs) {
            body.append(msg.getDisplayMessageBody());
        }
        return replaceFormFeeds(body.toString());
    }

    private static String replaceFormFeeds(String s) {
        return s == null ? UsimPBMemInfo.STRING_NOT_SET : s.replace('\f', '\n');
    }

    public PowerManager.WakeLock getWakeLock() {
        return this.mWakeLock;
    }

    public int getWakeLockTimeout() {
        return WAKELOCK_TIMEOUT;
    }

    protected boolean dispatchConcateSmsParts(TimerRecord record) {
        boolean handled = VDBG;
        if (record == null) {
            if (ENG) {
                log("ConcatenatedSmsFwkExt: null TimerRecord in msg");
                return VDBG;
            }
            return VDBG;
        }
        if (ENG) {
            log("ConcatenatedSmsFwkExt: timer is expired, dispatch existed segments. refNumber = " + record.refNumber);
        }
        InboundSmsTracker smsTracker = (InboundSmsTracker) record.mTracker;
        SmsBroadcastReceiver receiver = new SmsBroadcastReceiver(smsTracker);
        synchronized (this.mRawLock) {
            byte[][] pdus = this.mConcatenatedSmsFwkExt.queryExistedSegments(record);
            if (!this.mUserManager.isUserUnlocked()) {
                log("dispatchConcateSmsParts: device is still locked so delete segment(s), ref = " + record.refNumber);
                this.mConcatenatedSmsFwkExt.deleteExistedSegments(record);
                return processMessagePartWithUserLocked(smsTracker, pdus, -1);
            }
            if (BlockChecker.isBlocked(this.mContext, smsTracker.getAddress())) {
                log("dispatchConcateSmsParts: block phone number, number = " + smsTracker.getAddress());
                this.mConcatenatedSmsFwkExt.deleteExistedSegments(record);
                deleteFromRawTable(smsTracker.getDeleteWhere(), smsTracker.getDeleteWhereArgs(), 1);
                return VDBG;
            }
            if (pdus != null && pdus.length > 0) {
                int flag = this.mConcatenatedSmsFwkExt.getUploadFlag(record);
                if (flag == 2 || flag == 1) {
                    this.mConcatenatedSmsFwkExt.setUploadFlag(record);
                    List<String> carrierPackages = null;
                    UiccCard card = UiccController.getInstance().getUiccCard(this.mPhone.getPhoneId());
                    if (card != null) {
                        carrierPackages = card.getCarrierPackageNamesForIntent(this.mContext.getPackageManager(), new Intent("android.service.carrier.CarrierMessagingService"));
                    } else {
                        loge("UiccCard not initialized.");
                    }
                    List<String> systemPackages = getSystemAppForIntent(new Intent("android.service.carrier.CarrierMessagingService"));
                    if (carrierPackages != null && carrierPackages.size() == 1) {
                        log("Found carrier package.");
                        CarrierSmsFilter smsFilter = new CarrierSmsFilter(pdus, -1, smsTracker.getFormat(), receiver, flag);
                        CarrierSmsFilterCallback smsFilterCallback = new CarrierSmsFilterCallback(smsFilter, true);
                        smsFilter.filterSms(carrierPackages.get(0), smsFilterCallback);
                    } else if (systemPackages != null && systemPackages.size() == 1) {
                        log("Found system package.");
                        CarrierSmsFilter smsFilter2 = new CarrierSmsFilter(pdus, -1, smsTracker.getFormat(), receiver, flag);
                        CarrierSmsFilterCallback smsFilterCallback2 = new CarrierSmsFilterCallback(smsFilter2, true);
                        smsFilter2.filterSms(systemPackages.get(0), smsFilterCallback2);
                    } else {
                        logv("Unable to find carrier package: " + carrierPackages + ", nor systemPackages: " + systemPackages);
                        dispatchSmsDeliveryIntent(pdus, smsTracker.getFormat(), -1, receiver, flag);
                    }
                    handled = true;
                } else if (ENG) {
                    log("ConcatenatedSmsFwkExt: invalid upload flag");
                }
            } else if (ENG) {
                log("ConcatenatedSmsFwkExt: no pdus to be dispatched");
            }
            if (ENG) {
                log("ConcatenatedSmsFwkExt: delete segment(s), tracker = " + ((InboundSmsTracker) record.mTracker));
            }
            this.mConcatenatedSmsFwkExt.deleteExistedSegments(record);
            return handled;
        }
    }

    protected int phonePrivacyLockCheck(Intent intent) {
        if (!SmsConstants.isPrivacyLockSupport() || !SmsConstants.isPrivacyLockSupport()) {
            return 0;
        }
        Bundle bundle = new Bundle();
        Object[] messages = (Object[]) intent.getExtra(IPplSmsFilter.KEY_PDUS);
        ?? r3 = new byte[messages.length][];
        for (int i = 0; i < messages.length; i++) {
            r3[i] = (byte[]) messages[i];
        }
        bundle.putSerializable(IPplSmsFilter.KEY_PDUS, r3);
        bundle.putString("format", (String) intent.getExtra("format"));
        bundle.putInt(IPplSmsFilter.KEY_SUB_ID, this.mPhone.getSubId());
        bundle.putInt(IPplSmsFilter.KEY_SMS_TYPE, 0);
        boolean pplResult = this.mPplSmsFilter.pplFilter(bundle);
        if (ENG) {
            log("[Moms] Phone privacy check end, Need to filter(result) = " + pplResult);
        }
        if (!pplResult) {
            return 0;
        }
        return -1;
    }
}
