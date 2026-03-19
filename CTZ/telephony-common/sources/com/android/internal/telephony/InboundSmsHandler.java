package com.android.internal.telephony;

import android.R;
import android.app.ActivityManager;
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
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CarrierServicesSmsFilter;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.util.HexDump;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class InboundSmsHandler extends StateMachine {
    public static final int ADDRESS_COLUMN = 6;
    public static final int COUNT_COLUMN = 5;
    public static final int DATE_COLUMN = 3;
    protected static final boolean DBG = true;
    public static final int DESTINATION_PORT_COLUMN = 2;
    public static final int DISPLAY_ADDRESS_COLUMN = 9;
    protected static final int EVENT_BROADCAST_COMPLETE = 3;
    public static final int EVENT_BROADCAST_SMS = 2;
    public static final int EVENT_INJECT_SMS = 8;
    public static final int EVENT_NEW_SMS = 1;
    private static final int EVENT_RELEASE_WAKELOCK = 5;
    protected static final int EVENT_RETURN_TO_IDLE = 4;
    public static final int EVENT_START_ACCEPTING_SMS = 6;
    private static final int EVENT_UPDATE_PHONE_OBJECT = 7;
    public static final int ID_COLUMN = 7;
    public static final int MESSAGE_BODY_COLUMN = 8;
    private static final int NOTIFICATION_ID_NEW_MESSAGE = 1;
    private static final String NOTIFICATION_TAG = "InboundSmsHandler";
    public static final int PDU_COLUMN = 0;
    public static final int REFERENCE_NUMBER_COLUMN = 4;
    public static final String SELECT_BY_ID = "_id=?";
    public static final int SEQUENCE_COLUMN = 1;
    private static final boolean VDBG = false;
    private static final int WAKELOCK_TIMEOUT = 3000;
    protected final int DELETE_PERMANENTLY;
    protected final int MARK_DELETED;
    protected CellBroadcastHandler mCellBroadcastHandler;
    protected final Context mContext;
    protected DefaultState mDefaultState;
    protected DeliveringState mDeliveringState;
    IDeviceIdleController mDeviceIdleController;
    protected IdleState mIdleState;
    protected Phone mPhone;
    protected final ContentResolver mResolver;
    private final boolean mSmsReceiveDisabled;
    protected StartupState mStartupState;
    public SmsStorageMonitor mStorageMonitor;
    protected UserManager mUserManager;
    protected WaitingState mWaitingState;
    private final PowerManager.WakeLock mWakeLock;
    private int mWakeLockTimeout;
    protected final WapPushOverSms mWapPush;
    protected static final String[] PDU_PROJECTION = {"pdu"};
    protected static final String[] PDU_SEQUENCE_PORT_PROJECTION = {"pdu", "sequence", "destination_port", "display_originating_addr"};
    protected static final Map<Integer, Integer> PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING = new HashMap<Integer, Integer>() {
        {
            put(0, 0);
            put(1, 1);
            put(2, 2);
            put(9, 3);
        }
    };
    public static final Uri sRawUri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "raw");
    public static final Uri sRawUriPermanentDelete = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "raw/permanentDelete");
    private static String ACTION_OPEN_SMS_APP = "com.android.internal.telephony.OPEN_DEFAULT_SMS_APP";

    protected abstract void acknowledgeLastIncomingSms(boolean z, int i, Message message);

    protected abstract int dispatchMessageRadioSpecific(SmsMessageBase smsMessageBase);

    protected abstract boolean is3gpp2();

    protected InboundSmsHandler(String str, Context context, SmsStorageMonitor smsStorageMonitor, Phone phone, CellBroadcastHandler cellBroadcastHandler) {
        super(str);
        this.mDefaultState = new DefaultState();
        this.mStartupState = new StartupState();
        this.mIdleState = new IdleState();
        this.mDeliveringState = new DeliveringState();
        this.mWaitingState = new WaitingState();
        this.DELETE_PERMANENTLY = 1;
        this.MARK_DELETED = 2;
        this.mContext = context;
        this.mStorageMonitor = smsStorageMonitor;
        this.mPhone = phone;
        this.mCellBroadcastHandler = cellBroadcastHandler;
        this.mResolver = context.getContentResolver();
        this.mWapPush = TelephonyComponentFactory.getInstance().makeWapPushOverSms(context);
        this.mSmsReceiveDisabled = !TelephonyManager.from(this.mContext).getSmsReceiveCapableForPhone(this.mPhone.getPhoneId(), this.mContext.getResources().getBoolean(R.^attr-private.notificationHeaderIconSize));
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
    }

    protected InboundSmsHandler(String str, Context context, SmsStorageMonitor smsStorageMonitor, Phone phone, CellBroadcastHandler cellBroadcastHandler, Object obj) {
        super(str);
        this.mDefaultState = new DefaultState();
        this.mStartupState = new StartupState();
        this.mIdleState = new IdleState();
        this.mDeliveringState = new DeliveringState();
        this.mWaitingState = new WaitingState();
        this.DELETE_PERMANENTLY = 1;
        this.MARK_DELETED = 2;
        this.mContext = context;
        this.mStorageMonitor = smsStorageMonitor;
        this.mPhone = phone;
        this.mCellBroadcastHandler = cellBroadcastHandler;
        this.mResolver = context.getContentResolver();
        this.mWapPush = TelephonyComponentFactory.getInstance().makeWapPushOverSms(context);
        this.mSmsReceiveDisabled = !TelephonyManager.from(this.mContext).getSmsReceiveCapableForPhone(this.mPhone.getPhoneId(), this.mContext.getResources().getBoolean(R.^attr-private.notificationHeaderIconSize));
        this.mWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, str);
        this.mWakeLock.acquire();
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mDeviceIdleController = TelephonyComponentFactory.getInstance().getIDeviceIdleController();
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
    }

    public Phone getPhone() {
        return this.mPhone;
    }

    public class DefaultState extends State {
        public DefaultState() {
        }

        public boolean processMessage(Message message) {
            if (message.what == 7) {
                InboundSmsHandler.this.onUpdatePhoneObject((Phone) message.obj);
                return true;
            }
            String str = "processMessage: unhandled message type " + message.what + " currState=" + InboundSmsHandler.this.getCurrentState().getName();
            if (Build.IS_DEBUGGABLE) {
                InboundSmsHandler.this.loge("---- Dumping InboundSmsHandler ----");
                InboundSmsHandler.this.loge("Total records=" + InboundSmsHandler.this.getLogRecCount());
                for (int iMax = Math.max(InboundSmsHandler.this.getLogRecSize() + (-20), 0); iMax < InboundSmsHandler.this.getLogRecSize(); iMax++) {
                    InboundSmsHandler.this.loge("Rec[%d]: %s\n" + iMax + InboundSmsHandler.this.getLogRec(iMax).toString());
                }
                InboundSmsHandler.this.loge("---- Dumped InboundSmsHandler ----");
                throw new RuntimeException(str);
            }
            InboundSmsHandler.this.loge(str);
            return true;
        }
    }

    public class StartupState extends State {
        public StartupState() {
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Startup state");
            InboundSmsHandler.this.setWakeLockTimeout(0);
        }

        public boolean processMessage(Message message) {
            InboundSmsHandler.this.log("StartupState.processMessage:" + message.what);
            int i = message.what;
            if (i != 6) {
                if (i != 8) {
                    switch (i) {
                    }
                    return true;
                }
                InboundSmsHandler.this.deferMessage(message);
                return true;
            }
            InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mIdleState);
            return true;
        }
    }

    public class IdleState extends State {
        public IdleState() {
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Idle state");
            InboundSmsHandler.this.sendMessageDelayed(5, InboundSmsHandler.this.getWakeLockTimeout());
        }

        public void exit() {
            InboundSmsHandler.this.mWakeLock.acquire();
            InboundSmsHandler.this.log("acquired wakelock, leaving Idle state");
        }

        public boolean processMessage(Message message) {
            InboundSmsHandler.this.log("IdleState.processMessage:" + message.what);
            InboundSmsHandler.this.log("Idle state processing message type " + message.what);
            switch (message.what) {
                case 1:
                case 2:
                case 8:
                    InboundSmsHandler.this.deferMessage(message);
                    InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mDeliveringState);
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

    public class DeliveringState extends State {
        public DeliveringState() {
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Delivering state");
        }

        public void exit() {
            InboundSmsHandler.this.log("leaving Delivering state");
        }

        public boolean processMessage(Message message) {
            InboundSmsHandler.this.log("DeliveringState.processMessage:" + message.what);
            switch (message.what) {
                case 1:
                    InboundSmsHandler.this.handleNewSms((AsyncResult) message.obj);
                    InboundSmsHandler.this.sendMessage(4);
                    break;
                case 2:
                    if (InboundSmsHandler.this.processMessagePart((InboundSmsTracker) message.obj)) {
                        InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mWaitingState);
                    } else {
                        InboundSmsHandler.this.log("No broadcast sent on processing EVENT_BROADCAST_SMS in Delivering state. Return to Idle state");
                        InboundSmsHandler.this.sendMessage(4);
                    }
                    break;
                case 4:
                    InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mIdleState);
                    break;
                case 5:
                    InboundSmsHandler.this.mWakeLock.release();
                    if (!InboundSmsHandler.this.mWakeLock.isHeld()) {
                        InboundSmsHandler.this.loge("mWakeLock released while delivering/broadcasting!");
                    }
                    break;
                case 8:
                    InboundSmsHandler.this.handleInjectSms((AsyncResult) message.obj);
                    InboundSmsHandler.this.sendMessage(4);
                    break;
            }
            return true;
        }
    }

    public class WaitingState extends State {
        public WaitingState() {
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Waiting state");
        }

        public void exit() {
            InboundSmsHandler.this.log("exiting Waiting state");
            InboundSmsHandler.this.setWakeLockTimeout(InboundSmsHandler.WAKELOCK_TIMEOUT);
        }

        public boolean processMessage(Message message) {
            InboundSmsHandler.this.log("WaitingState.processMessage:" + message.what);
            switch (message.what) {
                case 2:
                    InboundSmsHandler.this.deferMessage(message);
                    break;
                case 3:
                    InboundSmsHandler.this.sendMessage(4);
                    InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mDeliveringState);
                    break;
            }
            return true;
        }
    }

    private void handleNewSms(AsyncResult asyncResult) {
        int iDispatchMessage;
        if (asyncResult.exception != null) {
            loge("Exception processing incoming SMS: " + asyncResult.exception);
            return;
        }
        try {
            iDispatchMessage = dispatchMessage(((SmsMessage) asyncResult.result).mWrappedSmsMessage);
        } catch (RuntimeException e) {
            loge("Exception dispatching message", e);
            iDispatchMessage = 2;
        }
        if (iDispatchMessage != -1) {
            notifyAndAcknowledgeLastIncomingSms(iDispatchMessage == 1, iDispatchMessage, null);
        }
    }

    private void handleInjectSms(AsyncResult asyncResult) {
        SmsDispatchersController.SmsInjectionCallback smsInjectionCallback;
        int iDispatchMessage = 2;
        try {
            smsInjectionCallback = (SmsDispatchersController.SmsInjectionCallback) asyncResult.userObj;
            try {
                SmsMessage smsMessage = (SmsMessage) asyncResult.result;
                if (smsMessage != null) {
                    iDispatchMessage = dispatchMessage(smsMessage.mWrappedSmsMessage);
                }
            } catch (RuntimeException e) {
                e = e;
                loge("Exception dispatching message", e);
            }
        } catch (RuntimeException e2) {
            e = e2;
            smsInjectionCallback = null;
        }
        if (smsInjectionCallback != null) {
            smsInjectionCallback.onSmsInjectedResult(iDispatchMessage);
        }
    }

    private int dispatchMessage(SmsMessageBase smsMessageBase) {
        if (smsMessageBase == null) {
            loge("dispatchSmsMessage: message is null");
            return 2;
        }
        if (this.mSmsReceiveDisabled) {
            log("Received short message on device which doesn't support receiving SMS. Ignored.");
            return 1;
        }
        boolean zIsOnlyCoreApps = false;
        try {
            zIsOnlyCoreApps = IPackageManager.Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
        } catch (RemoteException e) {
        }
        if (zIsOnlyCoreApps) {
            log("Received a short message in encrypted state. Rejecting.");
            return 2;
        }
        return dispatchMessageRadioSpecific(smsMessageBase);
    }

    protected void onUpdatePhoneObject(Phone phone) {
        this.mPhone = phone;
        this.mStorageMonitor = this.mPhone.mSmsStorageMonitor;
        log("onUpdatePhoneObject: phone=" + this.mPhone.getClass().getSimpleName());
    }

    private void notifyAndAcknowledgeLastIncomingSms(boolean z, int i, Message message) {
        if (!z) {
            Intent intent = new Intent("android.provider.Telephony.SMS_REJECTED");
            intent.putExtra("result", i);
            intent.addFlags(16777216);
            this.mContext.sendBroadcast(intent, "android.permission.RECEIVE_SMS");
        }
        acknowledgeLastIncomingSms(z, i, message);
    }

    protected int dispatchNormalMessage(SmsMessageBase smsMessageBase) {
        int i;
        InboundSmsTracker inboundSmsTrackerMakeInboundSmsTracker;
        SmsHeader userDataHeader = smsMessageBase.getUserDataHeader();
        if (userDataHeader == null || userDataHeader.concatRef == null) {
            if (userDataHeader == null || userDataHeader.portAddrs == null) {
                i = -1;
            } else {
                int i2 = userDataHeader.portAddrs.destPort;
                log("destination port: " + i2);
                i = i2;
            }
            inboundSmsTrackerMakeInboundSmsTracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(smsMessageBase.getPdu(), smsMessageBase.getTimestampMillis(), i, is3gpp2(), false, smsMessageBase.getOriginatingAddress(), smsMessageBase.getDisplayOriginatingAddress(), smsMessageBase.getMessageBody());
        } else {
            SmsHeader.ConcatRef concatRef = userDataHeader.concatRef;
            SmsHeader.PortAddrs portAddrs = userDataHeader.portAddrs;
            inboundSmsTrackerMakeInboundSmsTracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(smsMessageBase.getPdu(), smsMessageBase.getTimestampMillis(), portAddrs != null ? portAddrs.destPort : -1, is3gpp2(), smsMessageBase.getOriginatingAddress(), smsMessageBase.getDisplayOriginatingAddress(), concatRef.refNumber, concatRef.seqNumber, concatRef.msgCount, false, smsMessageBase.getMessageBody());
        }
        return addTrackerToRawTableAndSendMessage(inboundSmsTrackerMakeInboundSmsTracker, inboundSmsTrackerMakeInboundSmsTracker.getDestPort() == -1);
    }

    protected int addTrackerToRawTableAndSendMessage(InboundSmsTracker inboundSmsTracker, boolean z) {
        int iAddTrackerToRawTable = addTrackerToRawTable(inboundSmsTracker, z);
        if (iAddTrackerToRawTable != 1) {
            return iAddTrackerToRawTable != 5 ? 2 : 1;
        }
        sendMessage(2, inboundSmsTracker);
        return 1;
    }

    protected boolean processMessagePart(InboundSmsTracker inboundSmsTracker) throws Throwable {
        Cursor cursorQuery;
        byte[][] bArr;
        boolean zIsBlocked;
        int realDestPort;
        int messageCount = inboundSmsTracker.getMessageCount();
        int destPort = inboundSmsTracker.getDestPort();
        if (messageCount <= 0) {
            loge("processMessagePart: returning false due to invalid message count " + messageCount);
            return false;
        }
        Cursor cursor = null;
        if (messageCount == 1) {
            bArr = new byte[][]{inboundSmsTracker.getPdu()};
            zIsBlocked = BlockChecker.isBlocked(this.mContext, inboundSmsTracker.getDisplayAddress(), null);
        } else {
            try {
                try {
                    cursorQuery = this.mResolver.query(sRawUri, PDU_SEQUENCE_PORT_PROJECTION, inboundSmsTracker.getQueryForSegments(), new String[]{inboundSmsTracker.getAddress(), Integer.toString(inboundSmsTracker.getReferenceNumber()), Integer.toString(inboundSmsTracker.getMessageCount())}, null);
                } catch (SQLException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
                cursorQuery = cursor;
            }
            try {
                if (cursorQuery.getCount() < messageCount) {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return false;
                }
                bArr = new byte[messageCount][];
                zIsBlocked = false;
                while (cursorQuery.moveToNext()) {
                    int i = cursorQuery.getInt(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(1).intValue()) - inboundSmsTracker.getIndexOffset();
                    if (i >= bArr.length || i < 0) {
                        loge(String.format("processMessagePart: invalid seqNumber = %d, messageCount = %d", Integer.valueOf(i + inboundSmsTracker.getIndexOffset()), Integer.valueOf(messageCount)));
                    } else {
                        bArr[i] = HexDump.hexStringToByteArray(cursorQuery.getString(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(0).intValue()));
                        if (i == 0 && !cursorQuery.isNull(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(2).intValue()) && (realDestPort = InboundSmsTracker.getRealDestPort(cursorQuery.getInt(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(2).intValue()))) != -1) {
                            destPort = realDestPort;
                        }
                        if (!zIsBlocked) {
                            zIsBlocked = BlockChecker.isBlocked(this.mContext, cursorQuery.getString(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(9).intValue()), null);
                        }
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
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
        }
        int i2 = destPort;
        List listAsList = Arrays.asList(bArr);
        if (listAsList.size() == 0 || listAsList.contains(null)) {
            StringBuilder sb = new StringBuilder();
            sb.append("processMessagePart: returning false due to ");
            sb.append(listAsList.size() == 0 ? "pduList.size() == 0" : "pduList.contains(null)");
            loge(sb.toString());
            return false;
        }
        SmsBroadcastReceiver smsBroadcastReceiver = new SmsBroadcastReceiver(inboundSmsTracker);
        if (!this.mUserManager.isUserUnlocked()) {
            return processMessagePartWithUserLocked(inboundSmsTracker, bArr, i2, smsBroadcastReceiver);
        }
        if (i2 != 2948) {
            if (zIsBlocked) {
                deleteFromRawTable(inboundSmsTracker.getDeleteWhere(), inboundSmsTracker.getDeleteWhereArgs(), 1);
                return false;
            }
            if (!filterSms(bArr, i2, inboundSmsTracker, smsBroadcastReceiver, true)) {
                dispatchSmsDeliveryIntent(bArr, inboundSmsTracker.getFormat(), i2, smsBroadcastReceiver);
            }
            return true;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (byte[] userData : bArr) {
            if (!inboundSmsTracker.is3gpp2()) {
                SmsMessage smsMessageCreateFromPdu = SmsMessage.createFromPdu(userData, "3gpp");
                if (smsMessageCreateFromPdu == null) {
                    loge("processMessagePart: SmsMessage.createFromPdu returned null");
                    return false;
                }
                userData = smsMessageCreateFromPdu.getUserData();
            }
            byteArrayOutputStream.write(userData, 0, userData.length);
        }
        int iDispatchWapPdu = this.mWapPush.dispatchWapPdu(byteArrayOutputStream.toByteArray(), smsBroadcastReceiver, this);
        log("dispatchWapPdu() returned " + iDispatchWapPdu);
        if (iDispatchWapPdu == -1) {
            return true;
        }
        deleteFromRawTable(inboundSmsTracker.getDeleteWhere(), inboundSmsTracker.getDeleteWhereArgs(), 2);
        return false;
    }

    protected boolean processMessagePartWithUserLocked(InboundSmsTracker inboundSmsTracker, byte[][] bArr, int i, SmsBroadcastReceiver smsBroadcastReceiver) {
        log("Credential-encrypted storage not available. Port: " + i);
        if (i == 2948 && this.mWapPush.isWapPushForMms(bArr[0], this)) {
            showNewMessageNotification();
            return false;
        }
        if (i != -1) {
            return false;
        }
        if (filterSms(bArr, i, inboundSmsTracker, smsBroadcastReceiver, false)) {
            return true;
        }
        showNewMessageNotification();
        return false;
    }

    private void showNewMessageNotification() {
        if (!StorageManager.isFileEncryptedNativeOrEmulated()) {
            return;
        }
        log("Show new message notification.");
        ((NotificationManager) this.mContext.getSystemService("notification")).notify(NOTIFICATION_TAG, 1, new Notification.Builder(this.mContext).setSmallIcon(R.drawable.sym_action_chat).setAutoCancel(true).setVisibility(1).setDefaults(-1).setContentTitle(this.mContext.getString(R.string.ext_media_status_unmountable)).setContentText(this.mContext.getString(R.string.ext_media_status_removed)).setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_OPEN_SMS_APP), 1140850688)).setChannelId(NotificationChannelController.CHANNEL_ID_SMS).build());
    }

    static void cancelNewMessageNotification(Context context) {
        ((NotificationManager) context.getSystemService("notification")).cancel(NOTIFICATION_TAG, 1);
    }

    protected boolean filterSms(byte[][] bArr, int i, InboundSmsTracker inboundSmsTracker, SmsBroadcastReceiver smsBroadcastReceiver, boolean z) {
        if (new CarrierServicesSmsFilter(this.mContext, this.mPhone, bArr, i, inboundSmsTracker.getFormat(), new CarrierServicesSmsFilterCallback(bArr, i, inboundSmsTracker.getFormat(), smsBroadcastReceiver, z), getName()).filter()) {
            return true;
        }
        if (VisualVoicemailSmsFilter.filter(this.mContext, bArr, inboundSmsTracker.getFormat(), i, this.mPhone.getSubId())) {
            log("Visual voicemail SMS dropped");
            dropSms(smsBroadcastReceiver);
            return true;
        }
        return false;
    }

    public void dispatchIntent(Intent intent, String str, int i, Bundle bundle, BroadcastReceiver broadcastReceiver, UserHandle userHandle) {
        int[] runningUserIds;
        UserInfo userInfo;
        intent.addFlags(134217728);
        String action = intent.getAction();
        if ("android.provider.Telephony.SMS_DELIVER".equals(action) || "android.provider.Telephony.SMS_RECEIVED".equals(action) || "android.provider.Telephony.WAP_PUSH_DELIVER".equals(action) || "android.provider.Telephony.WAP_PUSH_RECEIVED".equals(action)) {
            intent.addFlags(268435456);
        }
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        if (userHandle.equals(UserHandle.ALL)) {
            try {
                runningUserIds = ActivityManager.getService().getRunningUserIds();
            } catch (RemoteException e) {
                runningUserIds = null;
            }
            if (runningUserIds == null) {
                runningUserIds = new int[]{userHandle.getIdentifier()};
            }
            for (int length = runningUserIds.length - 1; length >= 0; length--) {
                UserHandle userHandle2 = new UserHandle(runningUserIds[length]);
                if (runningUserIds[length] == 0 || (!this.mUserManager.hasUserRestriction("no_sms", userHandle2) && (userInfo = this.mUserManager.getUserInfo(runningUserIds[length])) != null && !userInfo.isManagedProfile())) {
                    this.mContext.sendOrderedBroadcastAsUser(intent, userHandle2, str, i, bundle, runningUserIds[length] == 0 ? broadcastReceiver : null, getHandler(), -1, null, null);
                }
            }
            return;
        }
        this.mContext.sendOrderedBroadcastAsUser(intent, userHandle, str, i, bundle, broadcastReceiver, getHandler(), -1, null, null);
    }

    protected void deleteFromRawTable(String str, String[] strArr, int i) {
        int iDelete = this.mResolver.delete(i == 1 ? sRawUriPermanentDelete : sRawUri, str, strArr);
        if (iDelete == 0) {
            loge("No rows were deleted from raw table!");
            return;
        }
        log("Deleted " + iDelete + " rows from raw table.");
    }

    protected Bundle handleSmsWhitelisting(ComponentName componentName) {
        String packageName;
        String str;
        if (componentName != null) {
            packageName = componentName.getPackageName();
            str = "sms-app";
        } else {
            packageName = this.mContext.getPackageName();
            str = "sms-broadcast";
        }
        try {
            long jAddPowerSaveTempWhitelistAppForSms = this.mDeviceIdleController.addPowerSaveTempWhitelistAppForSms(packageName, 0, str);
            BroadcastOptions broadcastOptionsMakeBasic = BroadcastOptions.makeBasic();
            broadcastOptionsMakeBasic.setTemporaryAppWhitelistDuration(jAddPowerSaveTempWhitelistAppForSms);
            return broadcastOptionsMakeBasic.toBundle();
        } catch (RemoteException e) {
            return null;
        }
    }

    protected void dispatchSmsDeliveryIntent(byte[][] bArr, String str, int i, SmsBroadcastReceiver smsBroadcastReceiver) {
        Uri uriWriteInboxMessage;
        Intent intent = new Intent();
        intent.putExtra("pdus", (Serializable) bArr);
        intent.putExtra("format", str);
        if (i == -1) {
            intent.setAction("android.provider.Telephony.SMS_DELIVER");
            ComponentName defaultSmsApplication = SmsApplication.getDefaultSmsApplication(this.mContext, true);
            if (defaultSmsApplication != null) {
                intent.setComponent(defaultSmsApplication);
                log("Delivering SMS to: " + defaultSmsApplication.getPackageName() + " " + defaultSmsApplication.getClassName());
            } else {
                intent.setComponent(null);
            }
            if (SmsManager.getDefault().getAutoPersisting() && (uriWriteInboxMessage = writeInboxMessage(intent)) != null) {
                intent.putExtra("uri", uriWriteInboxMessage.toString());
            }
            if (this.mPhone.getAppSmsManager().handleSmsReceivedIntent(intent)) {
                dropSms(smsBroadcastReceiver);
                return;
            }
        } else {
            intent.setAction("android.intent.action.DATA_SMS_RECEIVED");
            intent.setData(Uri.parse("sms://localhost:" + i));
            intent.setComponent(null);
            intent.addFlags(16777216);
        }
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, handleSmsWhitelisting(intent.getComponent()), smsBroadcastReceiver, UserHandle.SYSTEM);
    }

    protected boolean duplicateExists(InboundSmsTracker inboundSmsTracker) throws Throwable {
        String queryForMultiPartDuplicates;
        Cursor cursorOnGetDupCursor;
        String address = inboundSmsTracker.getAddress();
        String string = Integer.toString(inboundSmsTracker.getReferenceNumber());
        String string2 = Integer.toString(inboundSmsTracker.getMessageCount());
        String string3 = Integer.toString(inboundSmsTracker.getSequenceNumber());
        String string4 = Long.toString(inboundSmsTracker.getTimestamp());
        String messageBody = inboundSmsTracker.getMessageBody();
        if (inboundSmsTracker.getMessageCount() == 1) {
            queryForMultiPartDuplicates = "address=? AND reference_number=? AND count=? AND sequence=? AND date=? AND message_body=?";
        } else {
            queryForMultiPartDuplicates = inboundSmsTracker.getQueryForMultiPartDuplicates();
        }
        try {
            cursorOnGetDupCursor = onGetDupCursor(sRawUri, PDU_PROJECTION, queryForMultiPartDuplicates, new String[]{address, string, string2, string3, string4, messageBody}, null);
            if (cursorOnGetDupCursor != null) {
                try {
                    if (cursorOnGetDupCursor.moveToNext()) {
                        loge("Discarding duplicate message segment, refNumber=" + string + " seqNumber=" + string3 + " count=" + string2);
                        String string5 = cursorOnGetDupCursor.getString(0);
                        byte[] pdu = inboundSmsTracker.getPdu();
                        byte[] bArrHexStringToByteArray = HexDump.hexStringToByteArray(string5);
                        if (!Arrays.equals(bArrHexStringToByteArray, inboundSmsTracker.getPdu())) {
                            loge("Warning: dup message segment PDU of length " + pdu.length + " is different from existing PDU of length " + bArrHexStringToByteArray.length);
                        }
                        if (cursorOnGetDupCursor != null) {
                            cursorOnGetDupCursor.close();
                        }
                        return true;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorOnGetDupCursor != null) {
                        cursorOnGetDupCursor.close();
                    }
                    throw th;
                }
            }
            if (cursorOnGetDupCursor != null) {
                cursorOnGetDupCursor.close();
            }
            return false;
        } catch (Throwable th2) {
            th = th2;
            cursorOnGetDupCursor = null;
        }
    }

    protected int addTrackerToRawTable(InboundSmsTracker inboundSmsTracker, boolean z) {
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
        String address = inboundSmsTracker.getAddress();
        String string = Integer.toString(inboundSmsTracker.getReferenceNumber());
        String string2 = Integer.toString(inboundSmsTracker.getMessageCount());
        Uri uriInsert = this.mResolver.insert(sRawUri, inboundSmsTracker.getContentValues());
        log("URI of new row -> " + uriInsert);
        try {
            long id = ContentUris.parseId(uriInsert);
            if (inboundSmsTracker.getMessageCount() == 1) {
                inboundSmsTracker.setDeleteWhere(SELECT_BY_ID, new String[]{Long.toString(id)});
            } else {
                inboundSmsTracker.setDeleteWhere(inboundSmsTracker.getQueryForSegments(), new String[]{address, string, string2});
            }
            return 1;
        } catch (Exception e2) {
            loge("error parsing URI for new row: " + uriInsert, e2);
            return 2;
        }
    }

    public static boolean isCurrentFormat3gpp2() {
        return 2 == TelephonyManager.getDefault().getCurrentPhoneType();
    }

    protected final class SmsBroadcastReceiver extends BroadcastReceiver {
        private long mBroadcastTimeNano = System.nanoTime();
        private final String mDeleteWhere;
        private final String[] mDeleteWhereArgs;

        public SmsBroadcastReceiver(InboundSmsTracker inboundSmsTracker) {
            this.mDeleteWhere = inboundSmsTracker.getDeleteWhere();
            this.mDeleteWhereArgs = inboundSmsTracker.getDeleteWhereArgs();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle;
            String action = intent.getAction();
            if (action.equals("android.provider.Telephony.SMS_DELIVER")) {
                intent.setAction("android.provider.Telephony.SMS_RECEIVED");
                intent.addFlags(16777216);
                intent.setComponent(null);
                InboundSmsHandler.this.dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, InboundSmsHandler.this.handleSmsWhitelisting(null), this, UserHandle.ALL);
                return;
            }
            if (action.equals("android.provider.Telephony.WAP_PUSH_DELIVER")) {
                intent.setAction("android.provider.Telephony.WAP_PUSH_RECEIVED");
                intent.setComponent(null);
                intent.addFlags(16777216);
                try {
                    long jAddPowerSaveTempWhitelistAppForMms = InboundSmsHandler.this.mDeviceIdleController.addPowerSaveTempWhitelistAppForMms(InboundSmsHandler.this.mContext.getPackageName(), 0, "mms-broadcast");
                    BroadcastOptions broadcastOptionsMakeBasic = BroadcastOptions.makeBasic();
                    broadcastOptionsMakeBasic.setTemporaryAppWhitelistDuration(jAddPowerSaveTempWhitelistAppForMms);
                    bundle = broadcastOptionsMakeBasic.toBundle();
                } catch (RemoteException e) {
                    bundle = null;
                }
                String type = intent.getType();
                InboundSmsHandler.this.dispatchIntent(intent, WapPushOverSms.getPermissionForType(type), WapPushOverSms.getAppOpsPermissionForIntent(type), bundle, this, UserHandle.SYSTEM);
                return;
            }
            if (!"android.intent.action.DATA_SMS_RECEIVED".equals(action) && !"android.provider.Telephony.SMS_RECEIVED".equals(action) && !"android.intent.action.DATA_SMS_RECEIVED".equals(action) && !"android.provider.Telephony.WAP_PUSH_RECEIVED".equals(action)) {
                InboundSmsHandler.this.loge("unexpected BroadcastReceiver action: " + action);
            }
            int resultCode = getResultCode();
            if (resultCode != -1 && resultCode != 1) {
                InboundSmsHandler.this.loge("a broadcast receiver set the result code to " + resultCode + ", deleting from raw table anyway!");
            } else {
                InboundSmsHandler.this.log("successful broadcast, deleting from raw table.");
            }
            InboundSmsHandler.this.deleteFromRawTable(this.mDeleteWhere, this.mDeleteWhereArgs, 2);
            InboundSmsHandler.this.sendMessage(3);
            int iNanoTime = (int) ((System.nanoTime() - this.mBroadcastTimeNano) / 1000000);
            if (iNanoTime >= 5000) {
                InboundSmsHandler.this.loge("Slow ordered broadcast completion time: " + iNanoTime + " ms");
                return;
            }
            InboundSmsHandler.this.log("ordered broadcast completed in: " + iNanoTime + " ms");
        }
    }

    public final class CarrierServicesSmsFilterCallback implements CarrierServicesSmsFilter.CarrierServicesSmsFilterCallbackInterface {
        private final int mDestPort;
        private final byte[][] mPdus;
        private final SmsBroadcastReceiver mSmsBroadcastReceiver;
        private final String mSmsFormat;
        private final boolean mUserUnlocked;

        public CarrierServicesSmsFilterCallback(byte[][] bArr, int i, String str, SmsBroadcastReceiver smsBroadcastReceiver, boolean z) {
            this.mPdus = bArr;
            this.mDestPort = i;
            this.mSmsFormat = str;
            this.mSmsBroadcastReceiver = smsBroadcastReceiver;
            this.mUserUnlocked = z;
        }

        @Override
        public void onFilterComplete(int i) {
            InboundSmsHandler.this.logv("onFilterComplete: result is " + i);
            if ((i & 1) != 0) {
                InboundSmsHandler.this.dropSms(this.mSmsBroadcastReceiver);
                return;
            }
            if (VisualVoicemailSmsFilter.filter(InboundSmsHandler.this.mContext, this.mPdus, this.mSmsFormat, this.mDestPort, InboundSmsHandler.this.mPhone.getSubId())) {
                InboundSmsHandler.this.log("Visual voicemail SMS dropped");
                InboundSmsHandler.this.dropSms(this.mSmsBroadcastReceiver);
            } else {
                if (!this.mUserUnlocked) {
                    if (!InboundSmsHandler.this.isSkipNotifyFlagSet(i)) {
                        InboundSmsHandler.this.showNewMessageNotification();
                    }
                    InboundSmsHandler.this.sendMessage(3);
                    return;
                }
                InboundSmsHandler.this.dispatchSmsDeliveryIntent(this.mPdus, this.mSmsFormat, this.mDestPort, this.mSmsBroadcastReceiver);
            }
        }
    }

    private void dropSms(SmsBroadcastReceiver smsBroadcastReceiver) {
        deleteFromRawTable(smsBroadcastReceiver.mDeleteWhere, smsBroadcastReceiver.mDeleteWhereArgs, 2);
        sendMessage(3);
    }

    private boolean isSkipNotifyFlagSet(int i) {
        return (i & 2) > 0;
    }

    protected void log(String str) {
        Rlog.d(getName(), str);
    }

    protected void loge(String str) {
        Rlog.e(getName(), str);
    }

    protected void loge(String str, Throwable th) {
        Rlog.e(getName(), str, th);
    }

    protected Uri writeInboxMessage(Intent intent) {
        SmsMessage[] messagesFromIntent = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messagesFromIntent == null || messagesFromIntent.length < 1) {
            loge("Failed to parse SMS pdu");
            return null;
        }
        for (SmsMessage smsMessage : messagesFromIntent) {
            try {
                smsMessage.getDisplayMessageBody();
            } catch (NullPointerException e) {
                loge("NPE inside SmsMessage");
                return null;
            }
        }
        ContentValues smsMessage2 = parseSmsMessage(messagesFromIntent);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mContext.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, smsMessage2);
        } catch (Exception e2) {
            loge("Failed to persist inbox message", e2);
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private static ContentValues parseSmsMessage(SmsMessage[] smsMessageArr) {
        SmsMessage smsMessage = smsMessageArr[0];
        ContentValues contentValues = new ContentValues();
        contentValues.put("address", smsMessage.getDisplayOriginatingAddress());
        contentValues.put("body", buildMessageBodyFromPdus(smsMessageArr));
        contentValues.put("date_sent", Long.valueOf(smsMessage.getTimestampMillis()));
        contentValues.put("date", Long.valueOf(System.currentTimeMillis()));
        contentValues.put("protocol", Integer.valueOf(smsMessage.getProtocolIdentifier()));
        contentValues.put("seen", (Integer) 0);
        contentValues.put("read", (Integer) 0);
        String pseudoSubject = smsMessage.getPseudoSubject();
        if (!TextUtils.isEmpty(pseudoSubject)) {
            contentValues.put("subject", pseudoSubject);
        }
        contentValues.put("reply_path_present", Integer.valueOf(smsMessage.isReplyPathPresent() ? 1 : 0));
        contentValues.put("service_center", smsMessage.getServiceCenterAddress());
        return contentValues;
    }

    private static String buildMessageBodyFromPdus(SmsMessage[] smsMessageArr) {
        if (smsMessageArr.length == 1) {
            return replaceFormFeeds(smsMessageArr[0].getDisplayMessageBody());
        }
        StringBuilder sb = new StringBuilder();
        for (SmsMessage smsMessage : smsMessageArr) {
            sb.append(smsMessage.getDisplayMessageBody());
        }
        return replaceFormFeeds(sb.toString());
    }

    private static String replaceFormFeeds(String str) {
        return str == null ? "" : str.replace('\f', '\n');
    }

    @VisibleForTesting
    public PowerManager.WakeLock getWakeLock() {
        return this.mWakeLock;
    }

    @VisibleForTesting
    public int getWakeLockTimeout() {
        return this.mWakeLockTimeout;
    }

    private void setWakeLockTimeout(int i) {
        this.mWakeLockTimeout = i;
    }

    private static class NewMessageNotificationActionReceiver extends BroadcastReceiver {
        private NewMessageNotificationActionReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (InboundSmsHandler.ACTION_OPEN_SMS_APP.equals(intent.getAction())) {
                context.startActivity(context.getPackageManager().getLaunchIntentForPackage(Telephony.Sms.getDefaultSmsPackage(context)));
            }
        }
    }

    static void registerNewMessageNotificationActionHandler(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_OPEN_SMS_APP);
        context.registerReceiver(new NewMessageNotificationActionReceiver(), intentFilter);
    }

    protected Cursor onGetDupCursor(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return this.mResolver.query(uri, strArr, str, strArr2, str2);
    }
}
