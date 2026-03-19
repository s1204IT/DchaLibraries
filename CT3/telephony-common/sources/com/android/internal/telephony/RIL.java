package com.android.internal.telephony;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.ModemActivityInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.SignalStrength;
import android.telephony.SmsMessage;
import android.telephony.SmsParameters;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.SparseArray;
import android.view.Display;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.dataconnection.DataCallResponse;
import com.android.internal.telephony.dataconnection.DataProfile;
import com.android.internal.telephony.dataconnection.DcFailCause;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SsData;
import com.android.internal.telephony.gsm.SuppCrssNotification;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.SpnOverride;
import com.android.internal.telephony.uicc.UiccController;
import com.google.android.mms.pdu.CharacterSets;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.IServiceStateExt;
import com.mediatek.common.telephony.gsm.PBEntry;
import com.mediatek.common.telephony.gsm.PBMemStorage;
import com.mediatek.internal.telephony.CellBroadcastConfigInfo;
import com.mediatek.internal.telephony.EtwsNotification;
import com.mediatek.internal.telephony.FemtoCellInfo;
import com.mediatek.internal.telephony.IccSmsStorageStatus;
import com.mediatek.internal.telephony.NetworkInfoWithAcT;
import com.mediatek.internal.telephony.PseudoBSRecord;
import com.mediatek.internal.telephony.SrvccCallContext;
import com.mediatek.internal.telephony.dataconnection.IaExtendParam;
import com.mediatek.internal.telephony.gsm.GsmVTProvider;
import com.mediatek.internal.telephony.ppl.IPplSmsFilter;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.PhbEntry;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import com.mediatek.internal.telephony.worldphone.WorldMode;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RIL extends BaseCommands implements CommandsInterface {
    private static final int CARD_TYPE_CSIM = 4;
    private static final int CARD_TYPE_RUIM = 8;
    private static final int CARD_TYPE_SIM = 1;
    private static final int CARD_TYPE_USIM = 2;
    private static final int CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES = 31;
    private static final int CDMA_BSI_NO_OF_INTS_STRUCT = 3;
    private static final int DEFAULT_ACK_WAKE_LOCK_TIMEOUT_MS = 200;
    private static final int DEFAULT_BLOCKING_MESSAGE_RESPONSE_TIMEOUT_MS = 2000;
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT_MS = 60000;
    static final int EVENT_ACK_WAKE_LOCK_TIMEOUT = 4;
    static final int EVENT_BLOCKING_RESPONSE_TIMEOUT = 5;
    static final int EVENT_SEND = 1;
    static final int EVENT_SEND_ACK = 3;
    static final int EVENT_WAKE_LOCK_TIMEOUT = 2;
    public static final int FOR_ACK_WAKELOCK = 1;
    public static final int FOR_WAKELOCK = 0;
    public static final int INVALID_WAKELOCK = -1;
    static final int RADIO_SCREEN_OFF = 0;
    static final int RADIO_SCREEN_ON = 1;
    static final int RADIO_SCREEN_UNSET = -1;
    static final int RESPONSE_SOLICITED = 0;
    static final int RESPONSE_SOLICITED_ACK = 2;
    static final int RESPONSE_SOLICITED_ACK_EXP = 3;
    static final int RESPONSE_UNSOLICITED = 1;
    static final int RESPONSE_UNSOLICITED_ACK_EXP = 4;
    static final String RILJ_ACK_WAKELOCK_NAME = "RILJ_ACK_WL";
    static final boolean RILJ_LOGD = true;
    static final boolean RILJ_LOGV = false;
    static final String RILJ_LOG_TAG = "RILJ";
    static final int RIL_MAX_COMMAND_BYTES = 20480;
    static final int SOCKET_OPEN_RETRY_MILLIS = 4000;
    final PowerManager.WakeLock mAckWakeLock;
    final int mAckWakeLockTimeout;
    volatile int mAckWlSequenceNum;
    private final BroadcastReceiver mAirplaneModeListener;
    private final BroadcastReceiver mBatteryStateListener;
    Display mDefaultDisplay;
    int mDefaultDisplayState;
    private final DisplayManager.DisplayListener mDisplayListener;
    private dtmfQueueHandler mDtmfReqQueue;
    private TelephonyEventLog mEventLog;
    private final Handler mHandler;
    private Integer mInstanceId;
    BroadcastReceiver mIntentReceiver;
    boolean mIsDevicePlugged;
    Object[] mLastNITZTimeInfo;
    private int mPreviousPreferredType;
    int mRadioScreenState;
    RILReceiver mReceiver;
    Thread mReceiverThread;
    SparseArray<RILRequest> mRequestList;
    RILSender mSender;
    HandlerThread mSenderThread;
    private IServiceStateExt mServiceStateExt;
    LocalSocket mSocket;
    AtomicBoolean mTestingEmergencyCall;
    final PowerManager.WakeLock mWakeLock;
    int mWakeLockCount;
    final int mWakeLockTimeout;
    volatile int mWlSequenceNum;
    static final String[] SOCKET_NAME_RIL = {"rild", "rild2", "rild3"};
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"gsm.ril.fulluicctype", "gsm.ril.fulluicctype.2", "gsm.ril.fulluicctype.3", "gsm.ril.fulluicctype.4"};

    private class dtmfQueueHandler {
        private boolean mDtmfStatus;
        public final int MAXIMUM_DTMF_REQUEST = 32;
        private final boolean DTMF_STATUS_START = true;
        private final boolean DTMF_STATUS_STOP = RIL.RILJ_LOGV;
        private Vector mDtmfQueue = new Vector(32);
        private RILRequest mPendingCHLDRequest = null;
        private boolean mIsSendChldRequest = RIL.RILJ_LOGV;

        public dtmfQueueHandler() {
            this.mDtmfStatus = RIL.RILJ_LOGV;
            this.mDtmfStatus = RIL.RILJ_LOGV;
        }

        public void start() {
            this.mDtmfStatus = true;
        }

        public void stop() {
            this.mDtmfStatus = RIL.RILJ_LOGV;
        }

        public boolean isStart() {
            if (this.mDtmfStatus) {
                return true;
            }
            return RIL.RILJ_LOGV;
        }

        public void add(RILRequest o) {
            this.mDtmfQueue.addElement(o);
        }

        public void remove(RILRequest o) {
            this.mDtmfQueue.remove(o);
        }

        public void remove(int idx) {
            this.mDtmfQueue.removeElementAt(idx);
        }

        public RILRequest get() {
            return (RILRequest) this.mDtmfQueue.get(0);
        }

        public int size() {
            return this.mDtmfQueue.size();
        }

        public void setPendingRequest(RILRequest r) {
            this.mPendingCHLDRequest = r;
        }

        public RILRequest getPendingRequest() {
            return this.mPendingCHLDRequest;
        }

        public void setSendChldRequest() {
            this.mIsSendChldRequest = true;
        }

        public void resetSendChldRequest() {
            this.mIsSendChldRequest = RIL.RILJ_LOGV;
        }

        public boolean hasSendChldRequest() {
            RIL.this.riljLog("mIsSendChldRequest = " + this.mIsSendChldRequest);
            return this.mIsSendChldRequest;
        }
    }

    class RILSender extends Handler implements Runnable {
        byte[] dataLength;

        public RILSender(Looper looper) {
            super(looper);
            this.dataLength = new byte[4];
        }

        @Override
        public void run() {
        }

        @Override
        public void handleMessage(Message msg) {
            RILRequest rr = (RILRequest) msg.obj;
            switch (msg.what) {
                case 1:
                case 3:
                    try {
                        LocalSocket s = RIL.this.mSocket;
                        if (s == null) {
                            rr.onError(1, null);
                            RIL.this.decrementWakeLock(rr);
                            rr.release();
                            return;
                        }
                        byte[] data = rr.mParcel.marshall();
                        if (msg.what != 3) {
                            synchronized (RIL.this.mRequestList) {
                                RIL.this.mRequestList.append(rr.mSerial, rr);
                                rr.mParcel.recycle();
                                rr.mParcel = null;
                            }
                        } else {
                            rr.mParcel.recycle();
                            rr.mParcel = null;
                        }
                        if (data.length > RIL.RIL_MAX_COMMAND_BYTES) {
                            throw new RuntimeException("Parcel larger than max bytes allowed! " + data.length);
                        }
                        byte[] bArr = this.dataLength;
                        this.dataLength[1] = 0;
                        bArr[0] = 0;
                        this.dataLength[2] = (byte) ((data.length >> 8) & 255);
                        this.dataLength[3] = (byte) (data.length & 255);
                        s.getOutputStream().write(this.dataLength);
                        s.getOutputStream().write(data);
                        if (msg.what != 3) {
                            return;
                        }
                        rr.release();
                        return;
                    } catch (IOException ex) {
                        Rlog.e(RIL.RILJ_LOG_TAG, "IOException", ex);
                        RILRequest req = RIL.this.findAndRemoveRequestFromList(rr.mSerial);
                        if (req == null) {
                            return;
                        }
                        rr.onError(1, null);
                        RIL.this.decrementWakeLock(rr);
                        rr.release();
                        return;
                    } catch (RuntimeException exc) {
                        Rlog.e(RIL.RILJ_LOG_TAG, "Uncaught exception ", exc);
                        RILRequest req2 = RIL.this.findAndRemoveRequestFromList(rr.mSerial);
                        if (req2 == null) {
                            return;
                        }
                        rr.onError(2, null);
                        RIL.this.decrementWakeLock(rr);
                        rr.release();
                        return;
                    }
                case 2:
                    synchronized (RIL.this.mRequestList) {
                        if (msg.arg1 == RIL.this.mWlSequenceNum && RIL.this.clearWakeLock(0)) {
                            int count = RIL.this.mRequestList.size();
                            Rlog.d(RIL.RILJ_LOG_TAG, "WAKE_LOCK_TIMEOUT  mRequestList=" + count);
                            for (int i = 0; i < count; i++) {
                                RILRequest rr2 = RIL.this.mRequestList.valueAt(i);
                                Rlog.d(RIL.RILJ_LOG_TAG, i + ": [" + rr2.mSerial + "] " + RIL.requestToString(rr2.mRequest));
                            }
                        }
                    }
                    return;
                case 4:
                    if (msg.arg1 != RIL.this.mAckWlSequenceNum || !RIL.this.clearWakeLock(1)) {
                        return;
                    }
                    Rlog.d(RIL.RILJ_LOG_TAG, "ACK_WAKE_LOCK_TIMEOUT");
                    return;
                case 5:
                    int serial = msg.arg1;
                    RILRequest rr3 = RIL.this.findAndRemoveRequestFromList(serial);
                    if (rr3 == null) {
                        return;
                    }
                    if (rr3.mResult != null) {
                        Object timeoutResponse = RIL.getResponseForTimedOutRILRequest(rr3);
                        AsyncResult.forMessage(rr3.mResult, timeoutResponse, (Throwable) null);
                        rr3.mResult.sendToTarget();
                        RIL.this.mEventLog.writeOnRilTimeoutResponse(rr3.mSerial, rr3.mRequest);
                    }
                    RIL.this.decrementWakeLock(rr3);
                    rr3.release();
                    return;
                default:
                    return;
            }
        }
    }

    private static Object getResponseForTimedOutRILRequest(RILRequest rr) {
        if (rr == null) {
            return null;
        }
        switch (rr.mRequest) {
            case 135:
                Object timeoutResponse = new ModemActivityInfo(0L, 0, 0, new int[5], 0, 0);
                break;
        }
        return null;
    }

    private static int readRilMessage(InputStream is, byte[] buffer) throws IOException {
        int offset = 0;
        int remaining = 4;
        do {
            int countRead = is.read(buffer, offset, remaining);
            if (countRead < 0) {
                Rlog.e(RILJ_LOG_TAG, "Hit EOS reading message length");
                return -1;
            }
            offset += countRead;
            remaining -= countRead;
        } while (remaining > 0);
        int messageLength = ((buffer[0] & PplMessageManager.Type.INVALID) << 24) | ((buffer[1] & PplMessageManager.Type.INVALID) << 16) | ((buffer[2] & PplMessageManager.Type.INVALID) << 8) | (buffer[3] & PplMessageManager.Type.INVALID);
        int offset2 = 0;
        int remaining2 = messageLength;
        do {
            int countRead2 = is.read(buffer, offset2, remaining2);
            if (countRead2 < 0) {
                Rlog.e(RILJ_LOG_TAG, "Hit EOS reading message.  messageLength=" + messageLength + " remaining=" + remaining2);
                return -1;
            }
            offset2 += countRead2;
            remaining2 -= countRead2;
        } while (remaining2 > 0);
        return messageLength;
    }

    class RILReceiver implements Runnable {
        byte[] buffer = new byte[RIL.RIL_MAX_COMMAND_BYTES];

        RILReceiver() {
        }

        @Override
        public void run() {
            String rilSocket;
            LocalSocket s;
            int retryCount = 0;
            while (true) {
                LocalSocket s2 = null;
                try {
                    if (RIL.this.mInstanceId == null || RIL.this.mInstanceId.intValue() == 0) {
                        rilSocket = RIL.SOCKET_NAME_RIL[0];
                    } else {
                        rilSocket = RIL.SOCKET_NAME_RIL[RIL.this.mInstanceId.intValue()];
                    }
                    RIL.this.riljLog("rilSocket[" + RIL.this.mInstanceId + "] = " + rilSocket);
                    try {
                        s = new LocalSocket();
                    } catch (IOException e) {
                    }
                    try {
                        try {
                            LocalSocketAddress l = new LocalSocketAddress(rilSocket, LocalSocketAddress.Namespace.RESERVED);
                            s.connect(l);
                            retryCount = 0;
                            RIL.this.mSocket = s;
                            Rlog.i(RIL.RILJ_LOG_TAG, "(" + RIL.this.mInstanceId + ") Connected to '" + rilSocket + "' socket");
                            synchronized (RIL.this.mDtmfReqQueue) {
                                RIL.this.riljLog("queue size  " + RIL.this.mDtmfReqQueue.size());
                                for (int i = RIL.this.mDtmfReqQueue.size() - 1; i >= 0; i--) {
                                    RIL.this.mDtmfReqQueue.remove(i);
                                }
                                RIL.this.riljLog("queue size  after " + RIL.this.mDtmfReqQueue.size());
                                if (RIL.this.mDtmfReqQueue.getPendingRequest() != null) {
                                    RIL.this.riljLog("reset pending switch request");
                                    RILRequest pendingRequest = RIL.this.mDtmfReqQueue.getPendingRequest();
                                    if (pendingRequest.mResult != null) {
                                        AsyncResult.forMessage(pendingRequest.mResult, (Object) null, (Throwable) null);
                                        pendingRequest.mResult.sendToTarget();
                                    }
                                    RIL.this.mDtmfReqQueue.resetSendChldRequest();
                                    RIL.this.mDtmfReqQueue.setPendingRequest(null);
                                }
                            }
                            try {
                                InputStream is = RIL.this.mSocket.getInputStream();
                                while (true) {
                                    int length = RIL.readRilMessage(is, this.buffer);
                                    if (length < 0) {
                                        break;
                                    }
                                    Parcel p = Parcel.obtain();
                                    p.unmarshall(this.buffer, 0, length);
                                    p.setDataPosition(0);
                                    RIL.this.processResponse(p);
                                    p.recycle();
                                }
                            } catch (IOException ex) {
                                Rlog.i(RIL.RILJ_LOG_TAG, "'" + rilSocket + "' socket closed", ex);
                            } catch (Throwable tr) {
                                Rlog.e(RIL.RILJ_LOG_TAG, "Uncaught exception read length=0Exception:" + tr.toString());
                            }
                            Rlog.i(RIL.RILJ_LOG_TAG, "(" + RIL.this.mInstanceId + ") Disconnected from '" + rilSocket + "' socket");
                            RIL.this.setRadioState(CommandsInterface.RadioState.RADIO_UNAVAILABLE);
                            try {
                                RIL.this.mSocket.close();
                            } catch (IOException e2) {
                            }
                            RIL.this.mSocket = null;
                            RILRequest.resetSerial();
                            RIL.this.clearRequestList(1, RIL.RILJ_LOGV);
                        } catch (IOException e3) {
                            s2 = s;
                            if (s2 != null) {
                                try {
                                    s2.close();
                                } catch (IOException e4) {
                                }
                            }
                            if (retryCount == 8) {
                                Rlog.e(RIL.RILJ_LOG_TAG, "Couldn't find '" + rilSocket + "' socket after " + retryCount + " times, continuing to retry silently");
                            } else if (retryCount >= 0 && retryCount < 8) {
                                Rlog.i(RIL.RILJ_LOG_TAG, "Couldn't find '" + rilSocket + "' socket; retrying after timeout");
                            }
                            try {
                                Thread.sleep(4000L);
                            } catch (InterruptedException e5) {
                            }
                            retryCount++;
                        }
                    } catch (Throwable th) {
                        tr = th;
                        Rlog.e(RIL.RILJ_LOG_TAG, "Uncaught exception", tr);
                        RIL.this.notifyRegistrantsRilConnectionChanged(-1);
                        return;
                    }
                } catch (Throwable th2) {
                    tr = th2;
                }
            }
        }
    }

    public RIL(Context context, int preferredNetworkType, int cdmaSubscription) {
        this(context, preferredNetworkType, cdmaSubscription, null);
    }

    public RIL(Context context, int preferredNetworkType, int cdmaSubscription, Integer instanceId) {
        super(context);
        this.mDefaultDisplayState = 0;
        this.mRadioScreenState = -1;
        this.mIsDevicePlugged = RILJ_LOGV;
        this.mWlSequenceNum = 0;
        this.mAckWlSequenceNum = 0;
        this.mRequestList = new SparseArray<>();
        this.mTestingEmergencyCall = new AtomicBoolean(RILJ_LOGV);
        this.mHandler = new Handler();
        this.mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
            }

            @Override
            public void onDisplayChanged(int displayId) {
                RIL.this.riljLog("onDisplayChanged: displayId = " + displayId);
                if (displayId != 0) {
                    return;
                }
                int oldState = RIL.this.mDefaultDisplayState;
                RIL.this.mDefaultDisplayState = RIL.this.mDefaultDisplay.getState();
                if (RIL.this.mDefaultDisplayState == oldState) {
                    return;
                }
                RIL.this.updateScreenState();
            }
        };
        this.mBatteryStateListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                boolean z = RIL.RILJ_LOGV;
                boolean oldState = RIL.this.mIsDevicePlugged;
                RIL ril = RIL.this;
                if (intent.getIntExtra("plugged", 0) != 0) {
                    z = true;
                }
                ril.mIsDevicePlugged = z;
                if (RIL.this.mIsDevicePlugged == oldState) {
                    return;
                }
                RIL.this.updateScreenState();
            }
        };
        this.mAirplaneModeListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                boolean bAirplaneModeOn = intent.getBooleanExtra("state", RIL.RILJ_LOGV);
                Rlog.d(RIL.RILJ_LOG_TAG, "ACTION_AIRPLANE_MODE_CHANGED, bAirplaneModeOn = " + bAirplaneModeOn);
                if (!bAirplaneModeOn) {
                    return;
                }
                Rlog.d(RIL.RILJ_LOG_TAG, "Clean CFU status.");
                RIL.this.mCfuReturnValue = null;
            }
        };
        this.mPreviousPreferredType = -1;
        this.mDtmfReqQueue = new dtmfQueueHandler();
        this.mIntentReceiver = new BroadcastReceiver() {
            private static final int MODE_CDMA_ASSERT = 31;
            private static final int MODE_CDMA_RESET = 32;
            private static final int MODE_CDMA_RILD_NE = 103;
            private static final int MODE_GSM_RILD_NE = 101;
            private static final int MODE_PHONE_PROCESS_JE = 100;

            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("com.mtk.TEST_TRM")) {
                    int mode = intent.getIntExtra("mode", 2);
                    Rlog.d(RIL.RILJ_LOG_TAG, "RIL received com.mtk.TEST_TRM, mode = " + mode + ", mInstanceIds = " + RIL.this.mInstanceId);
                    if (mode == 100) {
                        throw new RuntimeException("UserTriggerPhoneJE");
                    }
                    RIL.this.setTrm(mode, null);
                    return;
                }
                Rlog.w(RIL.RILJ_LOG_TAG, "RIL received unexpected Intent: " + intent.getAction());
            }
        };
        riljLog("RIL(context, preferredNetworkType=" + preferredNetworkType + " cdmaSubscription=" + cdmaSubscription + ")");
        this.mContext = context;
        this.mCdmaSubscription = cdmaSubscription;
        this.mPreferredNetworkType = preferredNetworkType;
        this.mPhoneType = 0;
        this.mInstanceId = instanceId;
        this.mEventLog = new TelephonyEventLog(this.mInstanceId.intValue());
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, RILJ_LOG_TAG);
        this.mWakeLock.setReferenceCounted(RILJ_LOGV);
        this.mAckWakeLock = pm.newWakeLock(1, RILJ_ACK_WAKELOCK_NAME);
        this.mAckWakeLock.setReferenceCounted(RILJ_LOGV);
        this.mWakeLockTimeout = SystemProperties.getInt("ro.ril.wake_lock_timeout", 60000);
        this.mAckWakeLockTimeout = SystemProperties.getInt("ro.ril.wake_lock_timeout", 200);
        this.mWakeLockCount = 0;
        this.mSenderThread = new HandlerThread("RILSender" + this.mInstanceId);
        this.mSenderThread.start();
        Looper looper = this.mSenderThread.getLooper();
        this.mSender = new RILSender(looper);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (!cm.isNetworkSupported(0)) {
            riljLog("Not starting RILReceiver: wifi-only");
        } else {
            riljLog("Starting RILReceiver" + this.mInstanceId);
            this.mReceiver = new RILReceiver();
            this.mReceiverThread = new Thread(this.mReceiver, "RILReceiver" + this.mInstanceId);
            this.mReceiverThread.start();
            DisplayManager dm = (DisplayManager) context.getSystemService("display");
            this.mDefaultDisplay = dm.getDisplay(0);
            this.mDefaultDisplayState = this.mDefaultDisplay.getState();
            dm.registerDisplayListener(this.mDisplayListener, null);
            this.mDefaultDisplayState = this.mDefaultDisplay.getState();
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.mtk.TEST_TRM");
            context.registerReceiver(this.mIntentReceiver, filter);
            IntentFilter filterAirplane = new IntentFilter();
            filterAirplane.addAction("android.intent.action.AIRPLANE_MODE");
            context.registerReceiver(this.mAirplaneModeListener, filterAirplane);
        }
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return;
        }
        try {
            this.mServiceStateExt = (IServiceStateExt) MPlugin.createInstance(IServiceStateExt.class.getName(), context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getVoiceRadioTechnology(Message result) {
        RILRequest rr = RILRequest.obtain(108, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getImsRegistrationState(Message result) {
        RILRequest rr = RILRequest.obtain(CharacterSets.ISO_8859_16, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setOnNITZTime(Handler h, int what, Object obj) {
        super.setOnNITZTime(h, what, obj);
        if (this.mLastNITZTimeInfo == null) {
            return;
        }
        this.mNITZTimeRegistrant.notifyRegistrant(new AsyncResult((Object) null, this.mLastNITZTimeInfo, (Throwable) null));
    }

    @Override
    public void getIccCardStatus(Message result) {
        RILRequest rr = RILRequest.obtain(1, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setUiccSubscription(int slotId, int appIndex, int subId, int subStatus, Message result) {
        RILRequest rr = RILRequest.obtain(122, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " slot: " + slotId + " appIndex: " + appIndex + " subId: " + subId + " subStatus: " + subStatus);
        rr.mParcel.writeInt(slotId);
        rr.mParcel.writeInt(appIndex);
        rr.mParcel.writeInt(subId);
        rr.mParcel.writeInt(subStatus);
        send(rr);
    }

    @Override
    public void setDataAllowed(boolean allowed, Message result) {
        RILRequest rr = RILRequest.obtain(123, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " allowed: " + allowed);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(allowed ? 1 : 0);
        send(rr);
    }

    public void setPsRegistration(boolean register, int mode, Message result) {
        RILRequest rr = RILRequest.obtain(2151, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " register: " + register + ", mode: " + mode);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(register ? 1 : 0);
        rr.mParcel.writeInt(mode);
        send(rr);
    }

    @Override
    public void supplyIccPin(String pin, Message result) {
        supplyIccPinForApp(pin, null, result);
    }

    @Override
    public void supplyIccPinForApp(String pin, String aid, Message result) {
        RILRequest rr = RILRequest.obtain(2, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(2);
        rr.mParcel.writeString(pin);
        rr.mParcel.writeString(aid);
        send(rr);
    }

    @Override
    public void supplyIccPuk(String puk, String newPin, Message result) {
        supplyIccPukForApp(puk, newPin, null, result);
    }

    @Override
    public void supplyIccPukForApp(String puk, String newPin, String aid, Message result) {
        RILRequest rr = RILRequest.obtain(3, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(puk);
        rr.mParcel.writeString(newPin);
        rr.mParcel.writeString(aid);
        send(rr);
    }

    @Override
    public void supplyIccPin2(String pin, Message result) {
        supplyIccPin2ForApp(pin, null, result);
    }

    @Override
    public void supplyIccPin2ForApp(String pin, String aid, Message result) {
        RILRequest rr = RILRequest.obtain(4, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(2);
        rr.mParcel.writeString(pin);
        rr.mParcel.writeString(aid);
        send(rr);
    }

    @Override
    public void supplyIccPuk2(String puk2, String newPin2, Message result) {
        supplyIccPuk2ForApp(puk2, newPin2, null, result);
    }

    @Override
    public void supplyIccPuk2ForApp(String puk, String newPin2, String aid, Message result) {
        RILRequest rr = RILRequest.obtain(5, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(puk);
        rr.mParcel.writeString(newPin2);
        rr.mParcel.writeString(aid);
        send(rr);
    }

    @Override
    public void changeIccPin(String oldPin, String newPin, Message result) {
        changeIccPinForApp(oldPin, newPin, null, result);
    }

    @Override
    public void changeIccPinForApp(String oldPin, String newPin, String aid, Message result) {
        RILRequest rr = RILRequest.obtain(6, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(oldPin);
        rr.mParcel.writeString(newPin);
        rr.mParcel.writeString(aid);
        send(rr);
    }

    @Override
    public void changeIccPin2(String oldPin2, String newPin2, Message result) {
        changeIccPin2ForApp(oldPin2, newPin2, null, result);
    }

    @Override
    public void changeIccPin2ForApp(String oldPin2, String newPin2, String aid, Message result) {
        RILRequest rr = RILRequest.obtain(7, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(oldPin2);
        rr.mParcel.writeString(newPin2);
        rr.mParcel.writeString(aid);
        send(rr);
    }

    @Override
    public void changeBarringPassword(String facility, String oldPwd, String newPwd, Message result) {
        RILRequest rr = RILRequest.obtain(44, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(facility);
        rr.mParcel.writeString(oldPwd);
        rr.mParcel.writeString(newPwd);
        send(rr);
    }

    @Override
    public void supplyNetworkDepersonalization(String netpin, Message result) {
        RILRequest rr = RILRequest.obtain(8, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(1);
        rr.mParcel.writeString(netpin);
        send(rr);
    }

    @Override
    public void getCurrentCalls(Message result) {
        RILRequest rr = RILRequest.obtain(9, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    @Deprecated
    public void getPDPContextList(Message result) {
        getDataCallList(result);
    }

    @Override
    public void getDataCallList(Message result) {
        RILRequest rr = RILRequest.obtain(57, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void dial(String address, int clirMode, Message result) {
        dial(address, clirMode, null, result);
    }

    @Override
    public void dial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        if (!PhoneNumberUtils.isUriNumber(address)) {
            RILRequest rr = RILRequest.obtain(10, result);
            rr.mParcel.writeString(address);
            rr.mParcel.writeInt(clirMode);
            if (uusInfo == null) {
                rr.mParcel.writeInt(0);
            } else {
                rr.mParcel.writeInt(1);
                rr.mParcel.writeInt(uusInfo.getType());
                rr.mParcel.writeInt(uusInfo.getDcs());
                rr.mParcel.writeByteArray(uusInfo.getUserData());
            }
            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            this.mEventLog.writeRilDial(rr.mSerial, clirMode, uusInfo);
            send(rr);
            return;
        }
        RILRequest rr2 = RILRequest.obtain(2087, result);
        rr2.mParcel.writeString(address);
        riljLog(rr2.serialString() + "> " + requestToString(rr2.mRequest));
        this.mEventLog.writeRilDial(rr2.mSerial, clirMode, uusInfo);
        send(rr2);
    }

    @Override
    public void getIMSI(Message result) {
        getIMSIForApp(null, result);
    }

    @Override
    public void getIMSIForApp(String aid, Message result) {
        RILRequest rr = RILRequest.obtain(11, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeString(aid);
        riljLog(rr.serialString() + "> getIMSI: " + requestToString(rr.mRequest) + " aid: " + aid);
        send(rr);
    }

    @Override
    public void getIMEI(Message result) {
        RILRequest rr = RILRequest.obtain(38, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getIMEISV(Message result) {
        RILRequest rr = RILRequest.obtain(39, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void hangupConnection(int gsmIndex, Message result) {
        riljLog("hangupConnection: gsmIndex=" + gsmIndex);
        RILRequest rr = RILRequest.obtain(12, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + gsmIndex);
        this.mEventLog.writeRilHangup(rr.mSerial, 12, gsmIndex);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(gsmIndex);
        send(rr);
    }

    @Override
    public void hangupWaitingOrBackground(Message result) {
        RILRequest rr = RILRequest.obtain(13, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        this.mEventLog.writeRilHangup(rr.mSerial, 13, -1);
        send(rr);
    }

    @Override
    public void hangupForegroundResumeBackground(Message result) {
        RILRequest rr = RILRequest.obtain(14, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        this.mEventLog.writeRilHangup(rr.mSerial, 14, -1);
        send(rr);
    }

    @Override
    public void switchWaitingOrHoldingAndActive(Message result) {
        RILRequest rr = RILRequest.obtain(15, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        handleChldRelatedRequest(rr);
    }

    @Override
    public void conference(Message result) {
        RILRequest rr = RILRequest.obtain(16, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        handleChldRelatedRequest(rr);
    }

    @Override
    public void setPreferredVoicePrivacy(boolean enable, Message result) {
        RILRequest rr = RILRequest.obtain(82, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(enable ? 1 : 0);
        send(rr);
    }

    @Override
    public void getPreferredVoicePrivacy(Message result) {
        RILRequest rr = RILRequest.obtain(83, result);
        send(rr);
    }

    @Override
    public void separateConnection(int gsmIndex, Message result) {
        RILRequest rr = RILRequest.obtain(52, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + gsmIndex);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(gsmIndex);
        handleChldRelatedRequest(rr);
    }

    @Override
    public void acceptCall(Message result) {
        RILRequest rr = RILRequest.obtain(40, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        this.mEventLog.writeRilAnswer(rr.mSerial);
        send(rr);
    }

    @Override
    public void rejectCall(Message result) {
        RILRequest rr = RILRequest.obtain(17, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void explicitCallTransfer(Message result) {
        RILRequest rr = RILRequest.obtain(72, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        handleChldRelatedRequest(rr);
    }

    @Override
    public void getLastCallFailCause(Message result) {
        RILRequest rr = RILRequest.obtain(18, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    @Deprecated
    public void getLastPdpFailCause(Message result) {
        getLastDataCallFailCause(result);
    }

    @Override
    public void getLastDataCallFailCause(Message result) {
        RILRequest rr = RILRequest.obtain(56, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setMute(boolean enableMute, Message response) {
        RILRequest rr = RILRequest.obtain(53, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + enableMute);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(enableMute ? 1 : 0);
        send(rr);
    }

    @Override
    public void getMute(Message response) {
        RILRequest rr = RILRequest.obtain(54, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getSignalStrength(Message result) {
        RILRequest rr = RILRequest.obtain(19, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getVoiceRegistrationState(Message result) {
        RILRequest rr = RILRequest.obtain(20, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getDataRegistrationState(Message result) {
        RILRequest rr = RILRequest.obtain(21, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getOperator(Message result) {
        RILRequest rr = RILRequest.obtain(22, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getHardwareConfig(Message result) {
        RILRequest rr = RILRequest.obtain(124, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void sendDtmf(char c, Message result) {
        RILRequest rr = RILRequest.obtain(24, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeString(Character.toString(c));
        send(rr);
    }

    @Override
    public void startDtmf(char c, Message result) {
        synchronized (this.mDtmfReqQueue) {
            if (!this.mDtmfReqQueue.hasSendChldRequest()) {
                int size = this.mDtmfReqQueue.size();
                this.mDtmfReqQueue.getClass();
                if (size < 32) {
                    if (!this.mDtmfReqQueue.isStart()) {
                        RILRequest rr = RILRequest.obtain(49, result);
                        rr.mParcel.writeString(Character.toString(c));
                        this.mDtmfReqQueue.start();
                        this.mDtmfReqQueue.add(rr);
                        if (this.mDtmfReqQueue.size() == 1) {
                            riljLog("send start dtmf");
                            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                            send(rr);
                        }
                    } else {
                        riljLog("DTMF status conflict, want to start DTMF when status is " + this.mDtmfReqQueue.isStart());
                    }
                }
            }
        }
    }

    @Override
    public void stopDtmf(Message result) {
        synchronized (this.mDtmfReqQueue) {
            if (!this.mDtmfReqQueue.hasSendChldRequest()) {
                int size = this.mDtmfReqQueue.size();
                this.mDtmfReqQueue.getClass();
                if (size < 32) {
                    if (this.mDtmfReqQueue.isStart()) {
                        RILRequest rr = RILRequest.obtain(50, result);
                        this.mDtmfReqQueue.stop();
                        this.mDtmfReqQueue.add(rr);
                        if (this.mDtmfReqQueue.size() == 1) {
                            riljLog("send stop dtmf");
                            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                            send(rr);
                        }
                    } else {
                        riljLog("DTMF status conflict, want to start DTMF when status is " + this.mDtmfReqQueue.isStart());
                    }
                }
            }
        }
    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message result) {
        RILRequest rr = RILRequest.obtain(85, result);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(dtmfString);
        rr.mParcel.writeString(Integer.toString(on));
        rr.mParcel.writeString(Integer.toString(off));
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " : " + dtmfString);
        send(rr);
    }

    private void constructGsmSendSmsRilRequest(RILRequest rr, String smscPDU, String pdu) {
        rr.mParcel.writeInt(2);
        rr.mParcel.writeString(smscPDU);
        rr.mParcel.writeString(pdu);
    }

    @Override
    public void sendSMS(String smscPDU, String pdu, Message result) {
        RILRequest rr = RILRequest.obtain(25, result);
        constructGsmSendSmsRilRequest(rr, smscPDU, pdu);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        this.mEventLog.writeRilSendSms(rr.mSerial, rr.mRequest);
        send(rr);
    }

    @Override
    public void sendSMSExpectMore(String smscPDU, String pdu, Message result) {
        RILRequest rr = RILRequest.obtain(26, result);
        constructGsmSendSmsRilRequest(rr, smscPDU, pdu);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        this.mEventLog.writeRilSendSms(rr.mSerial, rr.mRequest);
        send(rr);
    }

    private void constructCdmaSendSmsRilRequest(RILRequest rr, byte[] pdu) {
        ByteArrayInputStream bais = new ByteArrayInputStream(pdu);
        DataInputStream dis = new DataInputStream(bais);
        try {
            rr.mParcel.writeInt(dis.readInt());
            rr.mParcel.writeByte((byte) dis.readInt());
            rr.mParcel.writeInt(dis.readInt());
            rr.mParcel.writeInt(dis.read());
            rr.mParcel.writeInt(dis.read());
            rr.mParcel.writeInt(dis.read());
            rr.mParcel.writeInt(dis.read());
            int address_nbr_of_digits = (byte) dis.read();
            rr.mParcel.writeByte((byte) address_nbr_of_digits);
            for (int i = 0; i < address_nbr_of_digits; i++) {
                rr.mParcel.writeByte(dis.readByte());
            }
            rr.mParcel.writeInt(dis.read());
            rr.mParcel.writeByte((byte) dis.read());
            int subaddr_nbr_of_digits = (byte) dis.read();
            rr.mParcel.writeByte((byte) subaddr_nbr_of_digits);
            for (int i2 = 0; i2 < subaddr_nbr_of_digits; i2++) {
                rr.mParcel.writeByte(dis.readByte());
            }
            int bearerDataLength = dis.read();
            rr.mParcel.writeInt(bearerDataLength);
            for (int i3 = 0; i3 < bearerDataLength; i3++) {
                rr.mParcel.writeByte(dis.readByte());
            }
        } catch (IOException ex) {
            riljLog("sendSmsCdma: conversion from input stream to object failed: " + ex);
        }
    }

    @Override
    public void sendCdmaSms(byte[] pdu, Message result) {
        RILRequest rr = RILRequest.obtain(87, result);
        constructCdmaSendSmsRilRequest(rr, pdu);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        this.mEventLog.writeRilSendSms(rr.mSerial, rr.mRequest);
        send(rr);
    }

    @Override
    public void sendImsGsmSms(String smscPDU, String pdu, int retry, int messageRef, Message result) {
        RILRequest rr = RILRequest.obtain(CharacterSets.GBK, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeByte((byte) retry);
        rr.mParcel.writeInt(messageRef);
        constructGsmSendSmsRilRequest(rr, smscPDU, pdu);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        this.mEventLog.writeRilSendSms(rr.mSerial, rr.mRequest);
        send(rr);
    }

    @Override
    public void sendImsCdmaSms(byte[] pdu, int retry, int messageRef, Message result) {
        RILRequest rr = RILRequest.obtain(CharacterSets.GBK, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeByte((byte) retry);
        rr.mParcel.writeInt(messageRef);
        constructCdmaSendSmsRilRequest(rr, pdu);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        this.mEventLog.writeRilSendSms(rr.mSerial, rr.mRequest);
        send(rr);
    }

    @Override
    public void deleteSmsOnSim(int index, Message response) {
        RILRequest rr = RILRequest.obtain(64, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(index);
        send(rr);
    }

    @Override
    public void deleteSmsOnRuim(int index, Message response) {
        RILRequest rr = RILRequest.obtain(97, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(index);
        send(rr);
    }

    @Override
    public void writeSmsToSim(int status, String smsc, String pdu, Message response) {
        int status2 = translateStatus(status);
        RILRequest rr = RILRequest.obtain(63, response);
        rr.mParcel.writeInt(status2);
        rr.mParcel.writeString(pdu);
        rr.mParcel.writeString(smsc);
        send(rr);
    }

    @Override
    public void writeSmsToRuim(int status, String pdu, Message response) {
        int status2 = translateStatus(status);
        RILRequest rr = RILRequest.obtain(96, response);
        rr.mParcel.writeInt(status2);
        constructCdmaSendSmsRilRequest(rr, IccUtils.hexStringToBytes(pdu));
        send(rr);
    }

    private int translateStatus(int status) {
        switch (status & 7) {
        }
        return 1;
    }

    @Override
    public void syncApnTableToRds(String[] strings, Message response) {
        RILRequest rr = RILRequest.obtain(2155, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeStringArray(strings);
        send(rr);
    }

    @Override
    public void setupDataCall(int radioTechnology, int profile, String apn, String user, String password, int authType, String protocol, Message result) {
        setupDataCall(radioTechnology, profile, apn, user, password, authType, protocol, 1, result);
    }

    @Override
    public void setupDataCall(int radioTechnology, int profile, String apn, String user, String password, int authType, String protocol, int interfaceId, Message result) {
        RILRequest rr = RILRequest.obtain(27, result);
        rr.mParcel.writeInt(8);
        rr.mParcel.writeString(Integer.toString(radioTechnology + 2));
        rr.mParcel.writeString(Integer.toString(profile));
        rr.mParcel.writeString(apn);
        rr.mParcel.writeString(user);
        rr.mParcel.writeString(password);
        rr.mParcel.writeString(Integer.toString(authType));
        rr.mParcel.writeString(protocol);
        rr.mParcel.writeString(Integer.toString(interfaceId));
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + radioTechnology + " " + profile + " " + apn + " " + user + " " + password + " " + authType + " " + protocol + " " + interfaceId);
        send(rr);
    }

    @Override
    public void deactivateDataCall(int cid, int reason, Message result) {
        RILRequest rr = RILRequest.obtain(41, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeString(Integer.toString(cid));
        rr.mParcel.writeString(Integer.toString(reason));
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + cid + " " + reason);
        this.mEventLog.writeRilDeactivateDataCall(rr.mSerial, cid, reason);
        send(rr);
    }

    @Override
    public void setRadioPower(boolean on, Message result) {
        RILRequest rr = RILRequest.obtain(23, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(on ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + (on ? " on" : " off"));
        send(rr);
    }

    @Override
    public void requestShutdown(Message result) {
        RILRequest rr = RILRequest.obtain(129, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setModemPower(boolean power, Message result) {
        RILRequest rr;
        riljLog("Set Modem power as: " + power);
        if (power) {
            rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_RESUME_RECEIVED, result);
        } else {
            rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_UPDATE, result);
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setSuppServiceNotifications(boolean enable, Message result) {
        RILRequest rr = RILRequest.obtain(62, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(enable ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void acknowledgeLastIncomingGsmSms(boolean success, int cause, Message result) {
        RILRequest rr = RILRequest.obtain(37, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(success ? 1 : 0);
        rr.mParcel.writeInt(cause);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + success + " " + cause);
        send(rr);
    }

    @Override
    public void acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message result) {
        RILRequest rr = RILRequest.obtain(88, result);
        rr.mParcel.writeInt(success ? 0 : 1);
        rr.mParcel.writeInt(cause);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + success + " " + cause);
        send(rr);
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message result) {
        RILRequest rr = RILRequest.obtain(106, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeString(success ? "1" : "0");
        rr.mParcel.writeString(ackPdu);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ' ' + success + " [" + ackPdu + ']');
        send(rr);
    }

    @Override
    public void iccIO(int command, int fileid, String path, int p1, int p2, int p3, String data, String pin2, Message result) {
        iccIOForApp(command, fileid, path, p1, p2, p3, data, pin2, null, result);
    }

    @Override
    public void iccIOForApp(int command, int fileid, String path, int p1, int p2, int p3, String data, String pin2, String aid, Message result) {
        RILRequest rr = RILRequest.obtain(28, result);
        rr.mParcel.writeInt(command);
        rr.mParcel.writeInt(fileid);
        rr.mParcel.writeString(path);
        rr.mParcel.writeInt(p1);
        rr.mParcel.writeInt(p2);
        rr.mParcel.writeInt(p3);
        rr.mParcel.writeString(data);
        rr.mParcel.writeString(pin2);
        rr.mParcel.writeString(aid);
        riljLog(rr.serialString() + "> iccIO: " + requestToString(rr.mRequest) + " 0x" + Integer.toHexString(command) + " 0x" + Integer.toHexString(fileid) + "  path: " + path + "," + p1 + "," + p2 + "," + p3 + " aid: " + aid);
        send(rr);
    }

    @Override
    public void getCLIR(Message result) {
        RILRequest rr = RILRequest.obtain(31, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setCLIR(int clirMode, Message result) {
        RILRequest rr = RILRequest.obtain(32, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(clirMode);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + clirMode);
        send(rr);
    }

    @Override
    public void queryCallWaiting(int serviceClass, Message response) {
        RILRequest rr = RILRequest.obtain(35, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(serviceClass);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + serviceClass);
        send(rr);
    }

    @Override
    public void setCallWaiting(boolean enable, int serviceClass, Message response) {
        RILRequest rr = RILRequest.obtain(36, response);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(enable ? 1 : 0);
        rr.mParcel.writeInt(serviceClass);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + enable + ", " + serviceClass);
        send(rr);
    }

    @Override
    public void getCOLP(Message result) {
        RILRequest rr = RILRequest.obtain(2000, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setCOLP(boolean enable, Message result) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_START, result);
        rr.mParcel.writeInt(1);
        if (enable) {
            rr.mParcel.writeInt(1);
        } else {
            rr.mParcel.writeInt(0);
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + enable);
        send(rr);
    }

    @Override
    public void getCOLR(Message result) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_START_CONFERENCE, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message response) {
        RILRequest rr = RILRequest.obtain(46, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setNetworkSelectionModeManual(String operatorNumeric, Message response) {
        RILRequest rr = RILRequest.obtain(47, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + operatorNumeric);
        rr.mParcel.writeString(operatorNumeric);
        send(rr);
    }

    @Override
    public void getNetworkSelectionMode(Message response) {
        RILRequest rr = RILRequest.obtain(45, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getAvailableNetworks(Message response) {
        RILRequest rr = RILRequest.obtain(2074, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void cancelAvailableNetworks(Message response) {
        RILRequest rr = RILRequest.obtain(2062, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setCallForward(int action, int cfReason, int serviceClass, String number, int timeSeconds, Message response) {
        RILRequest rr = RILRequest.obtain(34, response);
        rr.mParcel.writeInt(action);
        rr.mParcel.writeInt(cfReason);
        rr.mParcel.writeInt(serviceClass);
        rr.mParcel.writeInt(PhoneNumberUtils.toaFromString(number));
        rr.mParcel.writeString(number);
        rr.mParcel.writeInt(timeSeconds);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + action + " " + cfReason + " " + serviceClass + timeSeconds);
        send(rr);
    }

    @Override
    public void queryCallForwardStatus(int cfReason, int serviceClass, String number, Message response) {
        RILRequest rr = RILRequest.obtain(33, response);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(cfReason);
        rr.mParcel.writeInt(serviceClass);
        rr.mParcel.writeInt(PhoneNumberUtils.toaFromString(number));
        rr.mParcel.writeString(number);
        rr.mParcel.writeInt(0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + cfReason + " " + serviceClass);
        send(rr);
    }

    @Override
    public void queryCLIP(Message response) {
        RILRequest rr = RILRequest.obtain(55, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getBasebandVersion(Message response) {
        RILRequest rr = RILRequest.obtain(51, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void queryFacilityLock(String facility, String password, int serviceClass, Message response) {
        queryFacilityLockForApp(facility, password, serviceClass, null, response);
    }

    @Override
    public void queryFacilityLockForApp(String facility, String password, int serviceClass, String appId, Message response) {
        RILRequest rr = RILRequest.obtain(42, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " [" + facility + " " + serviceClass + " " + appId + "]");
        rr.mParcel.writeInt(4);
        rr.mParcel.writeString(facility);
        rr.mParcel.writeString(password);
        rr.mParcel.writeString(Integer.toString(serviceClass));
        rr.mParcel.writeString(appId);
        send(rr);
    }

    @Override
    public void setFacilityLock(String facility, boolean lockState, String password, int serviceClass, Message response) {
        setFacilityLockForApp(facility, lockState, password, serviceClass, null, response);
    }

    @Override
    public void setFacilityLockForApp(String facility, boolean lockState, String password, int serviceClass, String appId, Message response) {
        RILRequest rr = RILRequest.obtain(43, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " [" + facility + " " + lockState + " " + serviceClass + " " + appId + "]");
        rr.mParcel.writeInt(5);
        rr.mParcel.writeString(facility);
        String lockString = lockState ? "1" : "0";
        rr.mParcel.writeString(lockString);
        rr.mParcel.writeString(password);
        rr.mParcel.writeString(Integer.toString(serviceClass));
        rr.mParcel.writeString(appId);
        send(rr);
    }

    @Override
    public void sendUSSD(String ussdString, Message response) {
        RILRequest rr = RILRequest.obtain(29, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " *******");
        rr.mParcel.writeString(ussdString);
        send(rr);
    }

    @Override
    public void sendCNAPSS(String cnapssString, Message response) {
        RILRequest rr = RILRequest.obtain(2075, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + cnapssString);
        rr.mParcel.writeString(cnapssString);
        send(rr);
    }

    @Override
    public void cancelPendingUssd(Message response) {
        RILRequest rr = RILRequest.obtain(30, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void resetRadio(Message result) {
        RILRequest rr = RILRequest.obtain(58, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void invokeOemRilRequestRaw(byte[] data, Message response) {
        RILRequest rr = RILRequest.obtain(59, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + "[" + IccUtils.bytesToHexString(data) + "]");
        rr.mParcel.writeByteArray(data);
        send(rr);
    }

    @Override
    public void invokeOemRilRequestStrings(String[] strings, Message response) {
        RILRequest rr = RILRequest.obtain(60, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeStringArray(strings);
        send(rr);
    }

    @Override
    public void setBandMode(int bandMode, Message response) {
        RILRequest rr = RILRequest.obtain(65, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(bandMode);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + bandMode);
        send(rr);
    }

    @Override
    public void setBandMode(int[] bandMode, Message response) {
        RILRequest rr = RILRequest.obtain(65, response);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeInt(bandMode[0]);
        rr.mParcel.writeInt(bandMode[1]);
        rr.mParcel.writeInt(bandMode[2]);
        Rlog.d(RILJ_LOG_TAG, "Set band modes: " + bandMode[1] + ", " + bandMode[2]);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + bandMode);
        send(rr);
    }

    @Override
    public void queryAvailableBandMode(Message response) {
        RILRequest rr = RILRequest.obtain(66, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void sendTerminalResponse(String contents, Message response) {
        RILRequest rr = RILRequest.obtain(70, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeString(contents);
        send(rr);
    }

    @Override
    public void sendEnvelope(String contents, Message response) {
        RILRequest rr = RILRequest.obtain(69, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeString(contents);
        send(rr);
    }

    @Override
    public void sendEnvelopeWithStatus(String contents, Message response) {
        RILRequest rr = RILRequest.obtain(107, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + '[' + contents + ']');
        rr.mParcel.writeString(contents);
        send(rr);
    }

    @Override
    public void handleCallSetupRequestFromSim(boolean accept, int resCode, Message response) {
        RILRequest rr = RILRequest.obtain(71, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        int[] param = new int[1];
        if (resCode == 33 || resCode == 32) {
            param[0] = resCode;
        } else {
            param[0] = accept ? 1 : 0;
        }
        rr.mParcel.writeIntArray(param);
        send(rr);
    }

    @Override
    public void setPreferredNetworkType(int networkType, Message response) {
        RILRequest rr = RILRequest.obtain(73, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(networkType);
        this.mPreviousPreferredType = this.mPreferredNetworkType;
        this.mPreferredNetworkType = networkType;
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " : " + networkType);
        this.mEventLog.writeSetPreferredNetworkType(networkType);
        send(rr);
    }

    @Override
    public void getPreferredNetworkType(Message response) {
        RILRequest rr = RILRequest.obtain(74, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getNeighboringCids(Message response) {
        RILRequest rr = RILRequest.obtain(75, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setLocationUpdates(boolean enable, Message response) {
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        if (!pm.isScreenOn() || enable) {
            RILRequest rr = RILRequest.obtain(76, response);
            rr.mParcel.writeInt(1);
            rr.mParcel.writeInt(enable ? 1 : 0);
            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + enable);
            send(rr);
        }
    }

    @Override
    public void getSmscAddress(Message result) {
        RILRequest rr = RILRequest.obtain(100, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setSmscAddress(String address, Message result) {
        RILRequest rr = RILRequest.obtain(101, result);
        rr.mParcel.writeString(address);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " : " + address);
        send(rr);
    }

    @Override
    public void reportSmsMemoryStatus(boolean available, Message result) {
        RILRequest rr = RILRequest.obtain(CallFailCause.RECOVERY_ON_TIMER_EXPIRY, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(available ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + available);
        send(rr);
    }

    @Override
    public void reportStkServiceIsRunning(Message result) {
        RILRequest rr = RILRequest.obtain(103, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getGsmBroadcastConfig(Message response) {
        RILRequest rr = RILRequest.obtain(89, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] config, Message response) {
        RILRequest rr = RILRequest.obtain(90, response);
        int numOfConfig = config.length;
        rr.mParcel.writeInt(numOfConfig);
        for (int i = 0; i < numOfConfig; i++) {
            rr.mParcel.writeInt(config[i].getFromServiceId());
            rr.mParcel.writeInt(config[i].getToServiceId());
            rr.mParcel.writeInt(config[i].getFromCodeScheme());
            rr.mParcel.writeInt(config[i].getToCodeScheme());
            rr.mParcel.writeInt(config[i].isSelected() ? 1 : 0);
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " with " + numOfConfig + " configs : ");
        for (SmsBroadcastConfigInfo smsBroadcastConfigInfo : config) {
            riljLog(smsBroadcastConfigInfo.toString());
        }
        send(rr);
    }

    @Override
    public void setGsmBroadcastActivation(boolean activate, Message response) {
        RILRequest rr = RILRequest.obtain(91, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(activate ? 0 : 1);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    private void updateScreenState() {
        int oldState = this.mRadioScreenState;
        riljLog("defaultDisplayState: " + this.mDefaultDisplayState + ", isDevicePlugged: " + this.mIsDevicePlugged);
        this.mRadioScreenState = this.mDefaultDisplayState != 1 ? 1 : 0;
        if (this.mRadioScreenState == oldState) {
            return;
        }
        sendScreenState(this.mRadioScreenState == 1);
    }

    @Override
    public void sendScreenState(boolean on) {
        RILRequest rr = RILRequest.obtain(61, null);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(on ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + on);
        send(rr);
    }

    @Override
    protected void onRadioAvailable() {
        updateScreenState();
        sendScreenState(this.mRadioScreenState != 1 ? RILJ_LOGV : true);
    }

    private CommandsInterface.RadioState getRadioStateFromInt(int stateInt) {
        switch (stateInt) {
            case 0:
                CommandsInterface.RadioState state = CommandsInterface.RadioState.RADIO_OFF;
                return state;
            case 1:
                CommandsInterface.RadioState state2 = CommandsInterface.RadioState.RADIO_UNAVAILABLE;
                return state2;
            case 10:
                CommandsInterface.RadioState state3 = CommandsInterface.RadioState.RADIO_ON;
                return state3;
            default:
                throw new RuntimeException("Unrecognized RIL_RadioState: " + stateInt);
        }
    }

    private void switchToRadioState(CommandsInterface.RadioState newState) {
        CommandsInterface.RadioState oldState = this.mState;
        setRadioState(newState);
        if (newState == oldState) {
            return;
        }
        Intent intent = new Intent("android.intent.action.RADIO_STATE_CHANGED");
        intent.putExtra("radio_state", newState);
        intent.putExtra(IPplSmsFilter.KEY_SUB_ID, SubscriptionManager.getSubIdUsingPhoneId(this.mInstanceId.intValue()));
        this.mContext.sendBroadcast(intent);
    }

    private void acquireWakeLock(RILRequest rr, int wakeLockType) {
        synchronized (rr) {
            if (rr.mWakeLockType != -1) {
                Rlog.d(RILJ_LOG_TAG, "Failed to aquire wakelock for " + rr.serialString());
                return;
            }
            switch (wakeLockType) {
                case 0:
                    synchronized (this.mWakeLock) {
                        if (this.mWakeLockCount == 0) {
                            this.mWakeLock.acquire();
                        }
                        this.mWakeLockCount++;
                        this.mWlSequenceNum++;
                        Message msg = this.mSender.obtainMessage(2);
                        msg.arg1 = this.mWlSequenceNum;
                        this.mSender.sendMessageDelayed(msg, this.mWakeLockTimeout);
                    }
                    rr.mWakeLockType = wakeLockType;
                    return;
                case 1:
                    synchronized (this.mAckWakeLock) {
                        if (!this.mAckWakeLock.isHeld()) {
                            this.mAckWakeLock.acquire();
                        }
                        this.mAckWlSequenceNum++;
                        Message msg2 = this.mSender.obtainMessage(4);
                        msg2.arg1 = this.mAckWlSequenceNum;
                        this.mSender.sendMessageDelayed(msg2, this.mAckWakeLockTimeout);
                    }
                    rr.mWakeLockType = wakeLockType;
                    return;
                default:
                    Rlog.w(RILJ_LOG_TAG, "Acquiring Invalid Wakelock type " + wakeLockType);
                    return;
            }
        }
    }

    private void decrementWakeLock(RILRequest rr) {
        synchronized (rr) {
            switch (rr.mWakeLockType) {
                case -1:
                case 1:
                    rr.mWakeLockType = -1;
                    break;
                case 0:
                    synchronized (this.mWakeLock) {
                        if (this.mWakeLockCount > 1) {
                            this.mWakeLockCount--;
                        } else {
                            this.mWakeLockCount = 0;
                            this.mWakeLock.release();
                        }
                    }
                    rr.mWakeLockType = -1;
                    break;
                default:
                    Rlog.w(RILJ_LOG_TAG, "Decrementing Invalid Wakelock type " + rr.mWakeLockType);
                    rr.mWakeLockType = -1;
                    break;
            }
        }
    }

    private boolean clearWakeLock(int wakeLockType) {
        if (wakeLockType == 0) {
            synchronized (this.mWakeLock) {
                if (this.mWakeLockCount == 0 && !this.mWakeLock.isHeld()) {
                    return RILJ_LOGV;
                }
                Rlog.d(RILJ_LOG_TAG, "NOTE: mWakeLockCount is " + this.mWakeLockCount + "at time of clearing");
                this.mWakeLockCount = 0;
                this.mWakeLock.release();
                return true;
            }
        }
        synchronized (this.mAckWakeLock) {
            if (!this.mAckWakeLock.isHeld()) {
                return RILJ_LOGV;
            }
            this.mAckWakeLock.release();
            return true;
        }
    }

    private void send(RILRequest rr) {
        if (this.mSocket == null) {
            rr.onError(1, null);
            rr.release();
        } else {
            Message msg = this.mSender.obtainMessage(1, rr);
            acquireWakeLock(rr, 0);
            msg.sendToTarget();
        }
    }

    private void processResponse(Parcel p) {
        RILRequest rr;
        int type = p.readInt();
        if (type == 1 || type == 4) {
            processUnsolicited(p, type);
            return;
        }
        if (type == 0 || type == 3) {
            RILRequest rr2 = processSolicited(p, type);
            if (rr2 == null) {
                return;
            }
            if (type == 0) {
                decrementWakeLock(rr2);
            }
            rr2.release();
            return;
        }
        if (type != 2) {
            return;
        }
        int serial = p.readInt();
        synchronized (this.mRequestList) {
            rr = this.mRequestList.get(serial);
        }
        if (rr == null) {
            Rlog.w(RILJ_LOG_TAG, "Unexpected solicited ack response! sn: " + serial);
        } else {
            decrementWakeLock(rr);
            riljLog(rr.serialString() + " Ack < " + requestToString(rr.mRequest));
        }
    }

    private void clearRequestList(int error, boolean loggable) {
        synchronized (this.mRequestList) {
            int count = this.mRequestList.size();
            if (loggable) {
                Rlog.d(RILJ_LOG_TAG, "clearRequestList  mWakeLockCount=" + this.mWakeLockCount + " mRequestList=" + count);
            }
            for (int i = 0; i < count; i++) {
                RILRequest rr = this.mRequestList.valueAt(i);
                if (loggable) {
                    Rlog.d(RILJ_LOG_TAG, i + ": [" + rr.mSerial + "] " + requestToString(rr.mRequest));
                }
                rr.onError(error, null);
                decrementWakeLock(rr);
                rr.release();
            }
            this.mRequestList.clear();
        }
    }

    private RILRequest findAndRemoveRequestFromList(int serial) {
        RILRequest rr;
        synchronized (this.mRequestList) {
            rr = this.mRequestList.get(serial);
            if (rr != null) {
                this.mRequestList.remove(serial);
            }
        }
        return rr;
    }

    private RILRequest processSolicited(Parcel p, int type) {
        int serial = p.readInt();
        int error = p.readInt();
        RILRequest rr = findAndRemoveRequestFromList(serial);
        if (rr == null) {
            Rlog.w(RILJ_LOG_TAG, "Unexpected solicited response! sn: " + serial + " error: " + error);
            return null;
        }
        if (getRilVersion() >= 13 && type == 3) {
            RILRequest response = RILRequest.obtain(800, null);
            Message msg = this.mSender.obtainMessage(3, response);
            acquireWakeLock(rr, 1);
            msg.sendToTarget();
            riljLog("Response received for " + rr.serialString() + " " + requestToString(rr.mRequest) + " Sending ack to ril.cpp");
        }
        if (rr.mRequest == 49 || rr.mRequest == 50) {
            synchronized (this.mDtmfReqQueue) {
                this.mDtmfReqQueue.remove(rr);
                riljLog("remove first item in dtmf queue done, size = " + this.mDtmfReqQueue.size());
                if (this.mDtmfReqQueue.size() > 0) {
                    RILRequest rr2 = this.mDtmfReqQueue.get();
                    riljLog(rr2.serialString() + "> " + requestToString(rr2.mRequest));
                    send(rr2);
                } else if (this.mDtmfReqQueue.getPendingRequest() != null) {
                    riljLog("send pending switch request");
                    send(this.mDtmfReqQueue.getPendingRequest());
                    this.mDtmfReqQueue.setSendChldRequest();
                    this.mDtmfReqQueue.setPendingRequest(null);
                }
            }
        }
        Object ret = null;
        if (rr.mRequest == 48 || rr.mRequest == 2074) {
            this.mGetAvailableNetworkDoneRegistrant.notifyRegistrants();
        }
        if (rr.mRequest == 73) {
            if (error != 0 && this.mPreviousPreferredType != -1) {
                riljLog("restore mPreferredNetworkType from " + this.mPreferredNetworkType + " to " + this.mPreviousPreferredType);
                this.mPreferredNetworkType = this.mPreviousPreferredType;
            }
            this.mPreviousPreferredType = -1;
        }
        if (rr.mRequest == 15 || rr.mRequest == 16 || rr.mRequest == 52 || rr.mRequest == 72) {
            riljLog("clear mIsSendChldRequest");
            this.mDtmfReqQueue.resetSendChldRequest();
        }
        if (error == 0 || p.dataAvail() > 0) {
            try {
                switch (rr.mRequest) {
                    case 1:
                        ret = responseIccCardStatus(p);
                        break;
                    case 2:
                        ret = responseInts(p);
                        break;
                    case 3:
                        ret = responseInts(p);
                        break;
                    case 4:
                        ret = responseInts(p);
                        break;
                    case 5:
                        ret = responseInts(p);
                        break;
                    case 6:
                        ret = responseInts(p);
                        break;
                    case 7:
                        ret = responseInts(p);
                        break;
                    case 8:
                        ret = responseInts(p);
                        break;
                    case 9:
                        ret = responseCallList(p);
                        break;
                    case 10:
                        ret = responseVoid(p);
                        break;
                    case 11:
                        ret = responseString(p);
                        break;
                    case 12:
                        ret = responseVoid(p);
                        break;
                    case 13:
                        ret = responseVoid(p);
                        break;
                    case 14:
                        if (this.mTestingEmergencyCall.getAndSet(RILJ_LOGV) && this.mEmergencyCallbackModeRegistrant != null) {
                            riljLog("testing emergency call, notify ECM Registrants");
                            this.mEmergencyCallbackModeRegistrant.notifyRegistrant();
                        }
                        ret = responseVoid(p);
                        break;
                    case 15:
                        ret = responseVoid(p);
                        break;
                    case 16:
                        ret = responseVoid(p);
                        break;
                    case 17:
                        ret = responseVoid(p);
                        break;
                    case 18:
                        ret = responseFailCause(p);
                        break;
                    case 19:
                        ret = responseSignalStrength(p);
                        break;
                    case 20:
                        ret = responseStrings(p);
                        break;
                    case 21:
                        ret = responseStrings(p);
                        break;
                    case 22:
                        ret = responseStrings(p);
                        break;
                    case SmsHeader.ELT_ID_OBJECT_DISTR_INDICATOR:
                        ret = responseVoid(p);
                        break;
                    case 24:
                        ret = responseVoid(p);
                        break;
                    case 25:
                        ret = responseSMS(p);
                        break;
                    case 26:
                        ret = responseSMS(p);
                        break;
                    case CallFailCause.DESTINATION_OUT_OF_ORDER:
                        ret = responseSetupDataCall(p);
                        break;
                    case CallFailCause.INVALID_NUMBER_FORMAT:
                        ret = responseICC_IO(p);
                        break;
                    case CallFailCause.FACILITY_REJECTED:
                        ret = responseVoid(p);
                        break;
                    case 30:
                        ret = responseVoid(p);
                        break;
                    case 31:
                        ret = responseInts(p);
                        break;
                    case 32:
                        ret = responseVoid(p);
                        break;
                    case 33:
                        ret = responseCallForward(p);
                        break;
                    case 34:
                        ret = responseVoid(p);
                        break;
                    case 35:
                        ret = responseInts(p);
                        break;
                    case 36:
                        ret = responseVoid(p);
                        break;
                    case 37:
                        ret = responseVoid(p);
                        break;
                    case 38:
                        ret = responseString(p);
                        break;
                    case 39:
                        ret = responseString(p);
                        break;
                    case 40:
                        ret = responseVoid(p);
                        break;
                    case 41:
                        ret = responseVoid(p);
                        break;
                    case 42:
                        ret = responseInts(p);
                        break;
                    case CallFailCause.ACCESS_INFORMATION_DISCARDED:
                        ret = responseInts(p);
                        break;
                    case CallFailCause.CHANNEL_NOT_AVAIL:
                        ret = responseVoid(p);
                        break;
                    case 45:
                        ret = responseInts(p);
                        break;
                    case CatService.MSG_ID_CACHED_DISPLAY_TEXT_TIMEOUT:
                        ret = responseVoid(p);
                        break;
                    case 47:
                        ret = responseVoid(p);
                        break;
                    case 48:
                        ret = responseOperatorInfos(p);
                        break;
                    case 49:
                        ret = responseVoid(p);
                        break;
                    case 50:
                        ret = responseVoid(p);
                        break;
                    case RadioNVItems.RIL_NV_CDMA_PRL_VERSION:
                        ret = responseString(p);
                        break;
                    case RadioNVItems.RIL_NV_CDMA_BC10:
                        ret = responseVoid(p);
                        break;
                    case 53:
                        ret = responseVoid(p);
                        break;
                    case RadioNVItems.RIL_NV_CDMA_SO68:
                        ret = responseInts(p);
                        break;
                    case 55:
                        ret = responseInts(p);
                        break;
                    case 56:
                        ret = responseInts(p);
                        break;
                    case 57:
                        ret = responseDataCallList(p);
                        break;
                    case 58:
                        ret = responseVoid(p);
                        break;
                    case RadioNVItems.RIL_NV_CDMA_EHRPD_FORCED:
                        ret = responseRaw(p);
                        break;
                    case 60:
                        ret = responseStrings(p);
                        break;
                    case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_2:
                        ret = responseVoid(p);
                        break;
                    case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_3:
                        ret = responseVoid(p);
                        break;
                    case 63:
                        ret = responseInts(p);
                        break;
                    case 64:
                        ret = responseVoid(p);
                        break;
                    case CallFailCause.BEARER_NOT_IMPLEMENT:
                        ret = responseVoid(p);
                        break;
                    case 66:
                        ret = responseInts(p);
                        break;
                    case 67:
                        ret = responseString(p);
                        break;
                    case CallFailCause.ACM_LIMIT_EXCEEDED:
                        ret = responseVoid(p);
                        break;
                    case CallFailCause.FACILITY_NOT_IMPLEMENT:
                        ret = responseString(p);
                        break;
                    case 70:
                        ret = responseVoid(p);
                        break;
                    case 71:
                        ret = responseInts(p);
                        break;
                    case 72:
                        ret = responseVoid(p);
                        break;
                    case 73:
                        ret = responseSetPreferredNetworkType(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_SCAN_PRIORITY_25:
                        ret = responseGetPreferredNetworkType(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_SCAN_PRIORITY_26:
                        ret = responseCellList(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_SCAN_PRIORITY_41:
                        ret = responseVoid(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_HIDDEN_BAND_PRIORITY_25:
                        ret = responseVoid(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_HIDDEN_BAND_PRIORITY_26:
                        ret = responseVoid(p);
                        break;
                    case 79:
                        ret = responseInts(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_NEXT_SCAN:
                        ret = responseVoid(p);
                        break;
                    case 81:
                        ret = responseInts(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_BSR_MAX_TIME:
                        ret = responseVoid(p);
                        break;
                    case 83:
                        ret = responseInts(p);
                        break;
                    case 84:
                        ret = responseVoid(p);
                        break;
                    case 85:
                        ret = responseVoid(p);
                        break;
                    case 86:
                        ret = responseVoid(p);
                        break;
                    case 87:
                        ret = responseSMS(p);
                        break;
                    case CallFailCause.INCOMPATIBLE_DESTINATION:
                        ret = responseVoid(p);
                        break;
                    case 89:
                        ret = responseGmsBroadcastConfig(p);
                        break;
                    case 90:
                        ret = responseVoid(p);
                        break;
                    case CallFailCause.INVALID_TRANSIT_NETWORK_SELECTION:
                        ret = responseVoid(p);
                        break;
                    case 92:
                        ret = responseCdmaBroadcastConfig(p);
                        break;
                    case 93:
                        ret = responseVoid(p);
                        break;
                    case 94:
                        ret = responseVoid(p);
                        break;
                    case CallFailCause.SEMANTICALLY_INCORRECT_MESSAGE:
                        ret = responseStrings(p);
                        break;
                    case 96:
                        ret = responseInts(p);
                        break;
                    case CallFailCause.MESSAGE_TYPE_NON_EXISTENT:
                        ret = responseVoid(p);
                        break;
                    case CallFailCause.MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE:
                        ret = responseStrings(p);
                        break;
                    case CallFailCause.IE_NON_EXISTENT_OR_NOT_IMPLEMENTED:
                        ret = responseVoid(p);
                        break;
                    case 100:
                        ret = responseString(p);
                        break;
                    case 101:
                        ret = responseVoid(p);
                        break;
                    case CallFailCause.RECOVERY_ON_TIMER_EXPIRY:
                        ret = responseVoid(p);
                        break;
                    case 103:
                        ret = responseVoid(p);
                        break;
                    case CharacterSets.ISO_2022_CN:
                        ret = responseInts(p);
                        break;
                    case CharacterSets.ISO_2022_CN_EXT:
                        ret = !SystemProperties.get("ro.mtk_tc1_feature").equals("1") ? responseString(p) : responseStringEncodeBase64(p);
                        break;
                    case 106:
                        ret = responseVoid(p);
                        break;
                    case 107:
                        ret = responseICC_IO(p);
                        break;
                    case 108:
                        ret = responseInts(p);
                        break;
                    case CharacterSets.ISO_8859_13:
                        ret = responseCellInfoList(p);
                        break;
                    case CharacterSets.ISO_8859_14:
                        ret = responseVoid(p);
                        break;
                    case 111:
                        ret = responseVoid(p);
                        break;
                    case CharacterSets.ISO_8859_16:
                        ret = responseInts(p);
                        break;
                    case CharacterSets.GBK:
                        ret = responseSMS(p);
                        break;
                    case CharacterSets.GB18030:
                        ret = responseICC_IO(p);
                        break;
                    case 115:
                        ret = responseInts(p);
                        break;
                    case 116:
                        ret = responseVoid(p);
                        break;
                    case 117:
                        ret = responseICC_IO(p);
                        break;
                    case 118:
                        ret = responseString(p);
                        break;
                    case 119:
                        ret = responseVoid(p);
                        break;
                    case 120:
                        ret = responseVoid(p);
                        break;
                    case 121:
                        ret = responseVoid(p);
                        break;
                    case 122:
                        ret = responseVoid(p);
                        break;
                    case 123:
                        ret = responseVoid(p);
                        break;
                    case 124:
                        ret = responseHardwareConfig(p);
                        break;
                    case 125:
                        ret = responseICC_IOBase64(p);
                        break;
                    case 128:
                        ret = responseVoid(p);
                        break;
                    case 129:
                        ret = responseVoid(p);
                        break;
                    case 130:
                        ret = responseRadioCapability(p);
                        break;
                    case 131:
                        ret = responseRadioCapability(p);
                        break;
                    case 132:
                        ret = responseLceStatus(p);
                        break;
                    case 133:
                        ret = responseLceStatus(p);
                        break;
                    case 134:
                        ret = responseLceData(p);
                        break;
                    case 135:
                        ret = responseActivityData(p);
                        break;
                    case 2000:
                        ret = responseInts(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_START:
                        ret = responseVoid(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_START_CONFERENCE:
                        ret = responseInts(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_UPDATE:
                        ret = responseVoid(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_STARTED:
                        ret = responseInts(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_START_FAILED:
                        ret = responseVoid(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_TERMINATED:
                        ret = responsePhbEntries(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_HOLD_FAILED:
                        ret = responseInts(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_HOLD_RECEIVED:
                        ret = responseInts(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_RESUMED:
                        ret = responseVoid(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_RESUME_RECEIVED:
                        ret = responseVoid(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_UPDATED:
                        ret = responseSimSmsMemoryStatus(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_UPDATE_FAILED:
                        ret = responseInts(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_MERGED:
                        ret = responseNetworkInfoWithActs(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_MERGE_FAILED:
                        ret = responseVoid(p);
                        break;
                    case 2025:
                        ret = responseInts(p);
                        break;
                    case 2026:
                        ret = responseVoid(p);
                        break;
                    case 2027:
                        ret = responseVoid(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CONFERENCE_PARTICIPANTS_STATE_CHANGED:
                        ret = responseStrings(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_MULTIPARTY_STATE_CHANGED:
                        ret = responseInts(p);
                        break;
                    case TelephonyEventLog.TAG_IMS_CALL_STATE:
                        ret = responseVoid(p);
                        break;
                    case 2031:
                        ret = responseInts(p);
                        break;
                    case 2033:
                        ret = responseInts(p);
                        break;
                    case 2034:
                        ret = responseGetPhbMemStorage(p);
                        break;
                    case 2035:
                        responseVoid(p);
                        break;
                    case 2036:
                        ret = responseReadPhbEntryExt(p);
                        break;
                    case 2037:
                        ret = responseVoid(p);
                        break;
                    case 2038:
                        ret = responseSmsParams(p);
                        break;
                    case 2039:
                        ret = responseVoid(p);
                        break;
                    case 2042:
                        ret = responseString(p);
                        break;
                    case 2043:
                        ret = responseVoid(p);
                        break;
                    case 2044:
                        ret = responseVoid(p);
                        break;
                    case 2045:
                        ret = responseCbConfig(p);
                        break;
                    case 2047:
                        ret = responseVoid(p);
                        break;
                    case 2048:
                        ret = responseVoid(p);
                        break;
                    case 2050:
                        ret = responseVoid(p);
                        break;
                    case CharacterSets.CP864:
                        ret = responseVoid(p);
                        break;
                    case 2058:
                        ret = responseVoid(p);
                        break;
                    case 2059:
                        ret = responseFemtoCellInfos(p);
                        break;
                    case 2060:
                        ret = responseVoid(p);
                        break;
                    case 2061:
                        ret = responseVoid(p);
                        break;
                    case 2062:
                        ret = responseVoid(p);
                        break;
                    case 2063:
                        ret = responseVoid(p);
                        break;
                    case 2064:
                        ret = responseVoid(p);
                        break;
                    case 2065:
                        ret = responseVoid(p);
                        break;
                    case 2066:
                        ret = responseVoid(p);
                        break;
                    case 2067:
                        ret = responseVoid(p);
                        break;
                    case 2068:
                        ret = responseVoid(p);
                        break;
                    case 2069:
                        ret = responseICC_IO(p);
                        break;
                    case 2070:
                        ret = responseInts(p);
                        break;
                    case 2071:
                        ret = responseIccCardStatus(p);
                        break;
                    case 2072:
                        ret = responseICC_IO(p);
                        break;
                    case 2073:
                        ret = responseVoid(p);
                        break;
                    case 2074:
                        ret = responseOperatorInfosWithAct(p);
                        break;
                    case 2076:
                        ret = responseVoid(p);
                        break;
                    case 2083:
                        ret = responseVoid(p);
                        break;
                    case CharacterSets.KOI8_R:
                        ret = responseVoid(p);
                        break;
                    case CharacterSets.HZ_GB_2312:
                        responseString(p);
                        break;
                    case 2086:
                        responseString(p);
                        break;
                    case 2087:
                        ret = responseVoid(p);
                        break;
                    case CharacterSets.KOI8_U:
                        ret = responseVoid(p);
                        break;
                    case 2089:
                        ret = responseVoid(p);
                        break;
                    case 2090:
                        ret = responseVoid(p);
                        break;
                    case 2091:
                        ret = responseVoid(p);
                        break;
                    case 2092:
                        ret = responseVoid(p);
                        break;
                    case 2093:
                        ret = responseVoid(p);
                        break;
                    case 2094:
                        ret = responseVoid(p);
                        break;
                    case 2095:
                        ret = responseVoid(p);
                        break;
                    case 2100:
                        ret = responseVoid(p);
                        break;
                    case CharacterSets.BIG5_HKSCS:
                        ret = responseVoid(p);
                        break;
                    case 2102:
                        ret = responseVoid(p);
                        break;
                    case 2103:
                        ret = responseVoid(p);
                        break;
                    case 2104:
                        ret = responseVoid(p);
                        break;
                    case 2108:
                        ret = responseVoid(p);
                        break;
                    case 2110:
                        ret = responseVoid(p);
                        break;
                    case 2111:
                        ret = responseVoid(p);
                        break;
                    case 2131:
                        ret = responseVoid(p);
                        break;
                    case 2134:
                        ret = responseVoid(p);
                        break;
                    case 2142:
                        ret = responseVoid(p);
                        break;
                    case 2143:
                        ret = responseInts(p);
                        break;
                    case 2144:
                        ret = responseString(p);
                        break;
                    case 2145:
                        ret = responseString(p);
                        break;
                    case 2146:
                        ret = responsePhbEntries(p);
                        break;
                    case 2147:
                        ret = responseStrings(p);
                        break;
                    case 2151:
                        ret = responseVoid(p);
                        break;
                    case 2152:
                        ret = responseVoid(p);
                        break;
                    case 2153:
                        ret = responseVoid(p);
                        break;
                    case 2154:
                        ret = responseInts(p);
                        break;
                    case 2155:
                        ret = responseVoid(p);
                        break;
                    default:
                        throw new RuntimeException("Unrecognized solicited response: " + rr.mRequest);
                }
            } catch (Throwable tr) {
                Rlog.w(RILJ_LOG_TAG, rr.serialString() + "< " + requestToString(rr.mRequest) + " exception, possible invalid RIL response", tr);
                if (rr.mResult != null) {
                    AsyncResult.forMessage(rr.mResult, (Object) null, tr);
                    rr.mResult.sendToTarget();
                }
                return rr;
            }
        }
        if (rr.mRequest == 129) {
            riljLog("Response to RIL_REQUEST_SHUTDOWN received. Error is " + error + " Setting Radio State to Unavailable regardless of error.");
            setRadioState(CommandsInterface.RadioState.RADIO_UNAVAILABLE);
        }
        switch (rr.mRequest) {
            case 3:
            case 5:
                if (this.mIccStatusChangedRegistrants != null) {
                    riljLog("ON enter sim puk fakeSimStatusChanged: reg count=" + this.mIccStatusChangedRegistrants.size());
                    this.mIccStatusChangedRegistrants.notifyRegistrants();
                }
                break;
        }
        if (error != 0) {
            switch (rr.mRequest) {
                case 2:
                case 4:
                case 6:
                case 7:
                case CallFailCause.ACCESS_INFORMATION_DISCARDED:
                    if (this.mIccStatusChangedRegistrants != null) {
                        riljLog("ON some errors fakeSimStatusChanged: reg count=" + this.mIccStatusChangedRegistrants.size());
                        this.mIccStatusChangedRegistrants.notifyRegistrants();
                    }
                    break;
                case 130:
                    if (6 == error || 2 == error) {
                        ret = makeStaticRadioCapability();
                        error = 0;
                    }
                    break;
                case 135:
                    ret = new ModemActivityInfo(0L, 0, 0, new int[5], 0, 0);
                    error = 0;
                    break;
            }
            switch (rr.mRequest) {
                case CallFailCause.DESTINATION_OUT_OF_ORDER:
                    Intent intent = new Intent("com.mediatek.log2server.EXCEPTION_HAPPEND");
                    intent.putExtra("Reason", "SmartLogging");
                    intent.putExtra("from_where", "RIL");
                    this.mContext.sendBroadcast(intent);
                    riljLog("Broadcast for SmartLogging -DATA failure - may recover");
                    break;
            }
            if (error != 0) {
                rr.onError(error, ret);
            }
        }
        if (error == 0) {
            riljLog(rr.serialString() + "< " + requestToString(rr.mRequest) + " " + retToString(rr.mRequest, ret));
            if (rr.mResult != null) {
                AsyncResult.forMessage(rr.mResult, ret, (Throwable) null);
                rr.mResult.sendToTarget();
            }
        }
        this.mEventLog.writeOnRilSolicitedResponse(rr.mSerial, error, rr.mRequest, ret);
        return rr;
    }

    private RadioCapability makeStaticRadioCapability() {
        int raf = 1;
        String rafString = this.mContext.getResources().getString(R.string.PERSOSUBSTATE_RUIM_CORPORATE_SUCCESS);
        if (!TextUtils.isEmpty(rafString)) {
            raf = RadioAccessFamily.rafTypeFromString(rafString);
        }
        RadioCapability rc = new RadioCapability(this.mInstanceId.intValue(), 0, 0, raf, UsimPBMemInfo.STRING_NOT_SET, 1);
        riljLog("Faking RIL_REQUEST_GET_RADIO_CAPABILITY response using " + raf);
        return rc;
    }

    static String retToString(int req, Object ret) {
        if (ret == null) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        switch (req) {
            case 11:
            case 38:
            case 39:
            case 115:
            case 117:
                return UsimPBMemInfo.STRING_NOT_SET;
            default:
                if (ret instanceof int[]) {
                    int[] intArray = (int[]) ret;
                    int length = intArray.length;
                    StringBuilder sb = new StringBuilder("{");
                    if (length > 0) {
                        sb.append(intArray[0]);
                        for (int i = 1; i < length; i++) {
                            sb.append(", ").append(intArray[i]);
                        }
                    }
                    sb.append("}");
                    String s = sb.toString();
                    return s;
                }
                if (ret instanceof String[]) {
                    String[] strings = (String[]) ret;
                    int length2 = strings.length;
                    StringBuilder sb2 = new StringBuilder("{");
                    if (length2 > 0) {
                        sb2.append(strings[0]);
                        for (int i2 = 1; i2 < length2; i2++) {
                            sb2.append(", ").append(strings[i2]);
                        }
                    }
                    sb2.append("}");
                    String s2 = sb2.toString();
                    return s2;
                }
                if (req == 9) {
                    ArrayList<DriverCall> calls = (ArrayList) ret;
                    StringBuilder sb3 = new StringBuilder("{");
                    for (DriverCall dc : calls) {
                        sb3.append("[").append(dc).append("] ");
                    }
                    sb3.append("}");
                    String s3 = sb3.toString();
                    return s3;
                }
                if (req == 75) {
                    ArrayList<NeighboringCellInfo> cells = (ArrayList) ret;
                    StringBuilder sb4 = new StringBuilder("{");
                    for (NeighboringCellInfo cell : cells) {
                        sb4.append("[").append(cell).append("] ");
                    }
                    sb4.append("}");
                    String s4 = sb4.toString();
                    return s4;
                }
                if (req == 33) {
                    CallForwardInfo[] cinfo = (CallForwardInfo[]) ret;
                    StringBuilder sb5 = new StringBuilder("{");
                    for (CallForwardInfo callForwardInfo : cinfo) {
                        sb5.append("[").append(callForwardInfo).append("] ");
                    }
                    sb5.append("}");
                    String s5 = sb5.toString();
                    return s5;
                }
                if (req == 124) {
                    ArrayList<HardwareConfig> hwcfgs = (ArrayList) ret;
                    StringBuilder sb6 = new StringBuilder(" ");
                    for (HardwareConfig hwcfg : hwcfgs) {
                        sb6.append("[").append(hwcfg).append("] ");
                    }
                    String s6 = sb6.toString();
                    return s6;
                }
                String s7 = ret.toString();
                return s7;
        }
    }

    private void processUnsolicited(Parcel p, int type) {
        Object ret;
        Object obj;
        int response = p.readInt();
        if (getRilVersion() >= 13 && type == 4) {
            RILRequest rr = RILRequest.obtain(800, null);
            Message msg = this.mSender.obtainMessage(3, rr);
            acquireWakeLock(rr, 1);
            msg.sendToTarget();
            riljLog("Unsol response received for " + responseToString(response) + " Sending ack to ril.cpp");
        }
        try {
            switch (response) {
                case 1000:
                    ret = responseVoid(p);
                    break;
                case 1001:
                    ret = responseVoid(p);
                    break;
                case 1002:
                    ret = responseStrings(p);
                    break;
                case 1003:
                    ret = responseString(p);
                    break;
                case 1004:
                    ret = responseString(p);
                    break;
                case 1005:
                    ret = responseInts(p);
                    break;
                case 1006:
                    ret = responseStrings(p);
                    break;
                case 1008:
                    ret = responseString(p);
                    break;
                case 1009:
                    ret = responseSignalStrength(p);
                    break;
                case GsmVTProvider.SESSION_EVENT_START_COUNTER:
                    ret = responseDataCallList(p);
                    break;
                case 1011:
                    ret = responseSuppServiceNotification(p);
                    break;
                case 1012:
                    ret = responseVoid(p);
                    break;
                case CharacterSets.UTF_16BE:
                    ret = responseString(p);
                    break;
                case CharacterSets.UTF_16LE:
                    ret = responseString(p);
                    break;
                case CharacterSets.UTF_16:
                    ret = responseInts(p);
                    break;
                case CharacterSets.CESU_8:
                    ret = responseVoid(p);
                    break;
                case CharacterSets.UTF_32:
                    ret = responseSimRefresh(p);
                    break;
                case CharacterSets.UTF_32BE:
                    ret = responseCallRing(p);
                    break;
                case CharacterSets.UTF_32LE:
                    ret = responseVoid(p);
                    break;
                case CharacterSets.BOCU_1:
                    ret = responseCdmaSms(p);
                    break;
                case 1021:
                    ret = responseRaw(p);
                    break;
                case 1022:
                    ret = responseVoid(p);
                    break;
                case 1023:
                    ret = responseInts(p);
                    break;
                case 1024:
                    ret = responseVoid(p);
                    break;
                case 1025:
                    ret = responseCdmaCallWaiting(p);
                    break;
                case 1026:
                    ret = responseInts(p);
                    break;
                case 1027:
                    ret = responseCdmaInformationRecord(p);
                    break;
                case 1028:
                    ret = responseRaw(p);
                    break;
                case 1029:
                    ret = responseInts(p);
                    break;
                case 1030:
                    ret = responseVoid(p);
                    break;
                case 1031:
                    ret = responseInts(p);
                    break;
                case 1032:
                    ret = responseInts(p);
                    break;
                case 1033:
                    ret = responseVoid(p);
                    break;
                case 1034:
                    ret = responseInts(p);
                    break;
                case 1035:
                    ret = responseInts(p);
                    break;
                case 1036:
                    ret = responseCellInfoList(p);
                    break;
                case 1037:
                    ret = responseVoid(p);
                    break;
                case 1038:
                    ret = responseInts(p);
                    break;
                case 1039:
                    ret = responseInts(p);
                    break;
                case 1040:
                    ret = responseHardwareConfig(p);
                    break;
                case 1042:
                    ret = responseRadioCapability(p);
                    break;
                case 1043:
                    ret = responseSsData(p);
                    break;
                case 1044:
                    ret = responseStrings(p);
                    break;
                case 1045:
                    ret = responseLceData(p);
                    break;
                case 3000:
                    ret = responseStrings(p);
                    break;
                case 3001:
                    ret = responseStrings(p);
                    break;
                case 3002:
                    ret = responseInts(p);
                    break;
                case 3004:
                    ret = responseVoid(p);
                    break;
                case 3005:
                    ret = responseVoid(p);
                    break;
                case 3006:
                    ret = responseInts(p);
                    break;
                case 3008:
                    ret = responseInts(p);
                    break;
                case 3009:
                    ret = responseInts(p);
                    break;
                case 3010:
                    ret = responseInts(p);
                    break;
                case 3011:
                    ret = responseStrings(p);
                    break;
                case 3012:
                    ret = responseInts(p);
                    break;
                case 3013:
                    ret = responseInts(p);
                    break;
                case 3015:
                    ret = responseVoid(p);
                    break;
                case 3016:
                    ret = responseInts(p);
                    break;
                case 3017:
                    ret = responseVoid(p);
                    break;
                case 3018:
                    ret = responseVoid(p);
                    break;
                case 3019:
                    ret = responseEtwsNotification(p);
                    break;
                case 3020:
                    ret = responseStrings(p);
                    break;
                case 3021:
                    ret = responseInts(p);
                    break;
                case 3022:
                    ret = responseInts(p);
                    break;
                case 3023:
                    ret = responseStrings(p);
                    break;
                case 3024:
                    ret = responseVoid(p);
                    break;
                case 3025:
                    ret = responseInts(p);
                    break;
                case 3026:
                    ret = responseInts(p);
                    break;
                case 3027:
                    ret = responseInts(p);
                    break;
                case 3028:
                    ret = responseInts(p);
                    break;
                case 3029:
                    ret = responseInts(p);
                    break;
                case 3033:
                    ret = responseStrings(p);
                    break;
                case 3034:
                    ret = responseInts(p);
                    break;
                case 3035:
                    ret = responseInts(p);
                    break;
                case 3036:
                    ret = responseCrssNotification(p);
                    break;
                case 3037:
                    ret = responseStrings(p);
                    break;
                case 3038:
                    ret = responseStrings(p);
                    break;
                case 3039:
                    ret = responseVoid(p);
                    break;
                case 3040:
                    ret = responseInts(p);
                    break;
                case 3041:
                    ret = responseStrings(p);
                    break;
                case 3042:
                    ret = responseInts(p);
                    break;
                case 3043:
                    ret = responseStrings(p);
                    break;
                case 3044:
                    ret = responseInts(p);
                    break;
                case 3045:
                    ret = responseInts(p);
                    break;
                case 3046:
                    ret = responseInts(p);
                    break;
                case 3047:
                    ret = responseInts(p);
                    break;
                case 3048:
                    ret = responseVoid(p);
                    break;
                case 3049:
                    ret = responseInts(p);
                    break;
                case 3051:
                    ret = responseStrings(p);
                    break;
                case 3052:
                    ret = responseInts(p);
                    break;
                case 3054:
                    ret = responseInts(p);
                    break;
                case 3058:
                    ret = responseVoid(p);
                    break;
                case 3060:
                    ret = responseString(p);
                    break;
                case 3061:
                    ret = responseInts(p);
                    break;
                case 3062:
                    ret = responseInts(p);
                    break;
                case 3063:
                    ret = responseVoid(p);
                    break;
                case 3065:
                    ret = responseVoid(p);
                    break;
                case 3068:
                    ret = responseVoid(p);
                    break;
                case 3071:
                    ret = responseInts(p);
                    break;
                case 3074:
                    ret = responseVoid(p);
                    break;
                case 3081:
                    ret = responseInts(p);
                    break;
                case 3082:
                    ret = responseInts(p);
                    break;
                case 3083:
                    ret = responseInts(p);
                    break;
                case 3093:
                    ret = responseInts(p);
                    break;
                case 3095:
                    ret = responseInts(p);
                    break;
                case 3096:
                    ret = responseInts(p);
                    break;
                case 3098:
                    ret = responseVoid(p);
                    break;
                case 3099:
                    ret = responseInts(p);
                    break;
                case 3100:
                    ret = responseString(p);
                    break;
                case 3103:
                    ret = responseInts(p);
                    break;
                default:
                    throw new RuntimeException("Unrecognized unsol response: " + response);
            }
            switch (response) {
                case 1000:
                    CommandsInterface.RadioState newState = getRadioStateFromInt(p.readInt());
                    unsljLogMore(response, newState.toString());
                    switchToRadioState(newState);
                    return;
                case 1001:
                    unsljLog(response);
                    this.mCallStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
                    return;
                case 1002:
                    unsljLogvRet(response, ret);
                    this.mVoiceNetworkStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                    return;
                case 1003:
                    unsljLog(response);
                    this.mEventLog.writeRilNewSms(response);
                    String[] a = new String[2];
                    a[1] = (String) ret;
                    SmsMessage sms = SmsMessage.newFromCMT(a);
                    if (this.mGsmSmsRegistrant != null) {
                        this.mGsmSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, sms, (Throwable) null));
                        return;
                    }
                    return;
                case 1004:
                    unsljLogRet(response, ret);
                    if (this.mSmsStatusRegistrant != null) {
                        this.mSmsStatusRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1005:
                    unsljLogRet(response, ret);
                    int[] smsIndex = (int[]) ret;
                    if (smsIndex.length != 1) {
                        riljLog(" NEW_SMS_ON_SIM ERROR with wrong length " + smsIndex.length);
                        return;
                    } else {
                        if (this.mSmsOnSimRegistrant != null) {
                            this.mSmsOnSimRegistrant.notifyRegistrant(new AsyncResult((Object) null, smsIndex, (Throwable) null));
                            return;
                        }
                        return;
                    }
                case 1006:
                    String[] resp = (String[]) ret;
                    if (resp.length < 2) {
                        resp = new String[]{((String[]) ret)[0], null};
                    }
                    unsljLogMore(response, resp[0]);
                    if (this.mUSSDRegistrant != null) {
                        this.mUSSDRegistrant.notifyRegistrant(new AsyncResult((Object) null, resp, (Throwable) null));
                        return;
                    }
                    return;
                case 1008:
                    unsljLogRet(response, ret);
                    long nitzReceiveTime = p.readLong();
                    Object[] result = {ret, Long.valueOf(nitzReceiveTime)};
                    boolean ignoreNitz = SystemProperties.getBoolean("telephony.test.ignore.nitz", RILJ_LOGV);
                    if (ignoreNitz) {
                        riljLog("ignoring UNSOL_NITZ_TIME_RECEIVED");
                        return;
                    }
                    if (this.mNITZTimeRegistrant != null) {
                        this.mNITZTimeRegistrant.notifyRegistrant(new AsyncResult((Object) null, result, (Throwable) null));
                    }
                    this.mLastNITZTimeInfo = result;
                    return;
                case 1009:
                    if (this.mSignalStrengthRegistrant != null) {
                        this.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case GsmVTProvider.SESSION_EVENT_START_COUNTER:
                    unsljLogRet(response, ret);
                    this.mDataNetworkStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                    return;
                case 1011:
                    unsljLogRet(response, ret);
                    if (this.mSsnRegistrant != null) {
                        this.mSsnRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1012:
                    unsljLog(response);
                    if (this.mCatSessionEndRegistrant != null) {
                        this.mCatSessionEndRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case CharacterSets.UTF_16BE:
                    unsljLog(response);
                    if (this.mCatProCmdRegistrant != null) {
                        this.mCatProCmdRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case CharacterSets.UTF_16LE:
                    unsljLog(response);
                    if (this.mCatEventRegistrant != null) {
                        this.mCatEventRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case CharacterSets.UTF_16:
                    unsljLogRet(response, ret);
                    if (this.mCatCallSetUpRegistrant != null) {
                        this.mCatCallSetUpRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case CharacterSets.CESU_8:
                    unsljLog(response);
                    if (this.mIccSmsFullRegistrant != null) {
                        this.mIccSmsFullRegistrant.notifyRegistrant();
                        return;
                    } else {
                        Rlog.d(RILJ_LOG_TAG, "Cache sim sms full event");
                        this.mIsSmsSimFull = true;
                        return;
                    }
                case CharacterSets.UTF_32:
                    unsljLogRet(response, ret);
                    if (this.mIccRefreshRegistrants != null) {
                        this.mIccRefreshRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case CharacterSets.UTF_32BE:
                    unsljLogRet(response, ret);
                    if (this.mRingRegistrant != null) {
                        this.mRingRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case CharacterSets.UTF_32LE:
                    unsljLog(response);
                    if (this.mIccStatusChangedRegistrants != null) {
                        this.mIccStatusChangedRegistrants.notifyRegistrants();
                        return;
                    }
                    return;
                case CharacterSets.BOCU_1:
                    unsljLog(response);
                    this.mEventLog.writeRilNewSms(response);
                    SmsMessage sms2 = (SmsMessage) ret;
                    if (this.mCdmaSmsRegistrant != null) {
                        this.mCdmaSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, sms2, (Throwable) null));
                        return;
                    }
                    return;
                case 1021:
                    unsljLogvRet(response, IccUtils.bytesToHexString((byte[]) ret));
                    if (this.mGsmBroadcastSmsRegistrant != null) {
                        this.mGsmBroadcastSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1022:
                    unsljLog(response);
                    if (this.mIccSmsFullRegistrant != null) {
                        this.mIccSmsFullRegistrant.notifyRegistrant();
                        return;
                    }
                    return;
                case 1023:
                    unsljLogvRet(response, ret);
                    if (this.mRestrictedStateRegistrant != null) {
                        this.mRestrictedStateRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1024:
                    unsljLog(response);
                    if (this.mEmergencyCallbackModeRegistrant != null) {
                        this.mEmergencyCallbackModeRegistrant.notifyRegistrant();
                        return;
                    }
                    return;
                case 1025:
                    unsljLogRet(response, ret);
                    if (this.mCallWaitingInfoRegistrants != null) {
                        this.mCallWaitingInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1026:
                    unsljLogRet(response, ret);
                    if (this.mOtaProvisionRegistrants != null) {
                        this.mOtaProvisionRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1027:
                    try {
                        ArrayList<CdmaInformationRecords> listInfoRecs = (ArrayList) ret;
                        for (CdmaInformationRecords rec : listInfoRecs) {
                            unsljLogRet(response, rec);
                            notifyRegistrantsCdmaInfoRec(rec);
                        }
                        return;
                    } catch (ClassCastException e) {
                        Rlog.e(RILJ_LOG_TAG, "Unexpected exception casting to listInfoRecs", e);
                        return;
                    }
                case 1028:
                    unsljLogvRet(response, IccUtils.bytesToHexString((byte[]) ret));
                    if (this.mUnsolOemHookRawRegistrant != null) {
                        this.mUnsolOemHookRawRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1029:
                    unsljLogvRet(response, ret);
                    if (this.mRingbackToneRegistrants != null) {
                        boolean playtone = ((int[]) ret)[0] == 1 ? true : RILJ_LOGV;
                        this.mRingbackToneRegistrants.notifyRegistrants(new AsyncResult((Object) null, Boolean.valueOf(playtone), (Throwable) null));
                        return;
                    }
                    return;
                case 1030:
                    unsljLogRet(response, ret);
                    if (this.mResendIncallMuteRegistrants != null) {
                        this.mResendIncallMuteRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1031:
                    unsljLogRet(response, ret);
                    if (this.mCdmaSubscriptionChangedRegistrants != null) {
                        this.mCdmaSubscriptionChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1032:
                    unsljLogRet(response, ret);
                    if (this.mCdmaPrlChangedRegistrants != null) {
                        this.mCdmaPrlChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1033:
                    unsljLogRet(response, ret);
                    if (this.mExitEmergencyCallbackModeRegistrants != null) {
                        this.mExitEmergencyCallbackModeRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
                        return;
                    }
                    return;
                case 1034:
                    unsljLogRet(response, ret);
                    if (TelephonyManager.getDefault().getMultiSimConfiguration() == TelephonyManager.MultiSimVariants.DSDA || this.mInstanceId.intValue() == 0) {
                        setEccList();
                    }
                    setCdmaSubscriptionSource(this.mCdmaSubscription, null);
                    setCellInfoListRate(Integer.MAX_VALUE, null);
                    notifyRegistrantsRilConnectionChanged(((int[]) ret)[0]);
                    if (this.mDefaultDisplayState == 2) {
                        sendScreenState(true);
                        return;
                    } else if (this.mDefaultDisplayState == 1) {
                        sendScreenState(RILJ_LOGV);
                        return;
                    } else {
                        riljLog("not setScreenState mDefaultDisplayState=" + this.mDefaultDisplayState);
                        return;
                    }
                case 1035:
                    unsljLogRet(response, ret);
                    this.mNewVoiceTech[0] = ((int[]) ret)[0];
                    if (this.mVoiceRadioTechChangedRegistrants != null) {
                        this.mVoiceRadioTechChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1036:
                    unsljLogRet(response, ret);
                    if (this.mRilCellInfoListRegistrants != null) {
                        this.mRilCellInfoListRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1037:
                    unsljLog(response);
                    this.mImsNetworkStateChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
                    return;
                case 1038:
                    unsljLogRet(response, ret);
                    if (this.mSubscriptionStatusRegistrants != null) {
                        this.mSubscriptionStatusRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1039:
                    unsljLogRet(response, ret);
                    this.mEventLog.writeRilSrvcc(((int[]) ret)[0]);
                    if (this.mSrvccStateRegistrants != null) {
                        this.mSrvccStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1040:
                    unsljLogRet(response, ret);
                    if (this.mHardwareConfigChangeRegistrants != null) {
                        this.mHardwareConfigChangeRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1042:
                    unsljLogRet(response, ret);
                    this.mRadioCapability = (RadioCapability) ret;
                    if (this.mPhoneRadioCapabilityChangedRegistrants != null) {
                        this.mPhoneRadioCapabilityChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1043:
                    unsljLogRet(response, ret);
                    if (this.mSsRegistrant != null) {
                        this.mSsRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1044:
                    unsljLogRet(response, ret);
                    if (this.mCatCcAlphaRegistrant != null) {
                        this.mCatCcAlphaRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1045:
                    unsljLogRet(response, ret);
                    if (this.mLceInfoRegistrant != null) {
                        this.mLceInfoRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3000:
                    unsljLogvRet(response, ret);
                    if (this.mNeighboringInfoRegistrants != null) {
                        this.mNeighboringInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3001:
                    unsljLogvRet(response, ret);
                    if (ret != null) {
                        String[] networkinfo = (String[]) ret;
                        int nwinfo_type = Integer.parseInt(networkinfo[0]);
                        if (nwinfo_type == 401 || nwinfo_type == 402 || nwinfo_type == 403) {
                            Intent intent = new Intent("com.mediatek.log2server.EXCEPTION_HAPPEND");
                            intent.putExtra("Reason", "SmartLogging");
                            intent.putExtra("from_where", "RIL");
                            this.mContext.sendBroadcast(intent);
                            riljLog("Broadcast for SmartLogging " + nwinfo_type);
                            return;
                        }
                    }
                    if (this.mNetworkInfoRegistrants != null) {
                        this.mNetworkInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3002:
                    unsljLogRet(response, ret);
                    if (this.mPhbReadyRegistrants != null) {
                        this.mPhbReadyRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3004:
                    unsljLog(response);
                    if (this.mMeSmsFullRegistrant != null) {
                        this.mMeSmsFullRegistrant.notifyRegistrant();
                        return;
                    }
                    return;
                case 3005:
                    unsljLog(response);
                    if (this.mSmsReadyRegistrants.size() != 0) {
                        this.mSmsReadyRegistrants.notifyRegistrants();
                        return;
                    } else {
                        Rlog.d(RILJ_LOG_TAG, "Cache sms ready event");
                        this.mIsSmsReady = true;
                        return;
                    }
                case 3006:
                    unsljLogRet(response, ret);
                    if (this.mSimMissing != null) {
                        this.mSimMissing.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3008:
                    unsljLogRet(response, ret);
                    if (this.mSimRecovery != null) {
                        this.mSimRecovery.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3009:
                    unsljLogRet(response, ret);
                    if (this.mVirtualSimOn != null) {
                        this.mVirtualSimOn.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3010:
                    unsljLogRet(response, ret);
                    if (this.mVirtualSimOff != null) {
                        this.mVirtualSimOff.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3011:
                    unsljLogvRet(response, ret);
                    if (this.mInvalidSimInfoRegistrant != null) {
                        this.mInvalidSimInfoRegistrant.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3012:
                    unsljLog(response);
                    int[] stat = ret != null ? (int[]) ret : null;
                    this.mPsNetworkStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, stat, (Throwable) null));
                    return;
                case 3013:
                    unsljLog(response);
                    if (ret != null) {
                        int[] acmt = (int[]) ret;
                        if (acmt.length == 2) {
                            int error_type = Integer.valueOf(acmt[0]).intValue();
                            int error_cause = acmt[1];
                            if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                                return;
                            }
                            try {
                                if (this.mServiceStateExt.needBrodcastAcmt(error_type, error_cause)) {
                                    Intent intent2 = new Intent("mediatek.intent.action.acmt_nw_service_status");
                                    intent2.putExtra("CauseCode", acmt[1]);
                                    intent2.putExtra("CauseType", acmt[0]);
                                    this.mContext.sendBroadcast(intent2);
                                    riljLog("Broadcast for ACMT: com.VendorName.CauseCode " + acmt[1] + "," + acmt[0]);
                                    return;
                                }
                                return;
                            } catch (RuntimeException e2) {
                                e2.printStackTrace();
                                return;
                            }
                        }
                        return;
                    }
                    return;
                case 3015:
                    unsljLog(response);
                    if (this.mImeiLockRegistrant != null) {
                        this.mImeiLockRegistrant.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
                        return;
                    }
                    return;
                case 3016:
                    unsljLog(response);
                    if (ret != null) {
                        int[] emmrrs = (int[]) ret;
                        int ps_status = Integer.valueOf(emmrrs[0]).intValue();
                        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                            return;
                        }
                        try {
                            if (this.mServiceStateExt.isBroadcastEmmrrsPsResume(ps_status)) {
                                riljLog("Broadcast for EMMRRS: android.intent.action.EMMRRS_PS_RESUME ");
                                return;
                            }
                            return;
                        } catch (RuntimeException e3) {
                            e3.printStackTrace();
                            return;
                        }
                    }
                    return;
                case 3017:
                    unsljLogRet(response, ret);
                    if (this.mSimPlugOutRegistrants != null) {
                        this.mSimPlugOutRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                    }
                    this.mCfuReturnValue = null;
                    int[] retCfValue = {0, 1};
                    riljLog("Notify CFU change to disable due to sim plug out.");
                    this.mCallForwardingInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, retCfValue, (Throwable) null));
                    return;
                case 3018:
                    unsljLogRet(response, ret);
                    if (this.mSimPlugInRegistrants != null) {
                        this.mSimPlugInRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3019:
                    unsljLog(response);
                    if (this.mEtwsNotificationRegistrant != null) {
                        this.mEtwsNotificationRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3020:
                    unsljLogvRet(response, ret);
                    obj = this.mWPMonitor;
                    synchronized (obj) {
                        if (this.mPlmnChangeNotificationRegistrant.size() > 0) {
                            riljLog("ECOPS,notify mPlmnChangeNotificationRegistrant");
                            this.mPlmnChangeNotificationRegistrant.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        } else {
                            this.mEcopsReturnValue = ret;
                        }
                        break;
                    }
                    break;
                case 3021:
                    unsljLogvRet(response, ret);
                    obj = this.mWPMonitor;
                    synchronized (obj) {
                        if (this.mRegistrationSuspendedRegistrant != null) {
                            riljLog("EMSR, notify mRegistrationSuspendedRegistrant");
                            this.mRegistrationSuspendedRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        } else {
                            this.mEmsrReturnValue = ret;
                        }
                        break;
                    }
                    break;
                case 3022:
                    if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                        return;
                    }
                    unsljLogvRet(response, ret);
                    if (this.mStkEvdlCallRegistrant != null) {
                        this.mStkEvdlCallRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3023:
                    unsljLogvRet(response, ret);
                    this.mFemtoCellInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                    return;
                case 3024:
                    unsljLogRet(response, ret);
                    if (this.mStkSetupMenuResetRegistrant != null) {
                        this.mStkSetupMenuResetRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3025:
                    unsljLog(response);
                    if (this.mSessionChangedRegistrants != null) {
                        this.mSessionChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3026:
                    unsljLog(response);
                    if (this.mEconfSrvccRegistrants != null) {
                        this.mEconfSrvccRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3027:
                    unsljLog(response);
                    if (this.mImsEnableRegistrants != null) {
                        this.mImsEnableRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3028:
                    unsljLog(response);
                    if (this.mImsDisableRegistrants != null) {
                        this.mImsDisableRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3029:
                    unsljLog(response);
                    if (this.mImsRegistrationInfoRegistrants != null) {
                        this.mImsRegistrationInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3033:
                    unsljLog(response);
                    if (this.mEconfResultRegistrants != null) {
                        riljLog("Notify ECONF result");
                        String[] econfResult = (String[]) ret;
                        riljLog("ECONF result = " + econfResult[3]);
                        this.mEconfResultRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3034:
                    unsljLogRet(response, ret);
                    if (this.mMelockRegistrants != null) {
                        this.mMelockRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3035:
                    unsljLogRet(response, ret);
                    if (this.mCallForwardingInfoRegistrants != null) {
                        if (((int[]) ret)[0] == 1) {
                        }
                        boolean bIsLine1 = ((int[]) ret)[1] == 1 ? true : RILJ_LOGV;
                        if (bIsLine1) {
                            this.mCfuReturnValue = ret;
                            this.mCallForwardingInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                            return;
                        }
                        return;
                    }
                    return;
                case 3036:
                    unsljLogRet(response, ret);
                    if (this.mCallRelatedSuppSvcRegistrant != null) {
                        this.mCallRelatedSuppSvcRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3037:
                    unsljLogvRet(response, ret);
                    if (this.mIncomingCallIndicationRegistrant != null) {
                        this.mIncomingCallIndicationRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3038:
                    unsljLogvRet(response, ret);
                    int simCipherStatus = Integer.parseInt(((String[]) ret)[0]);
                    int sessionStatus = Integer.parseInt(((String[]) ret)[1]);
                    int csStatus = Integer.parseInt(((String[]) ret)[2]);
                    int psStatus = Integer.parseInt(((String[]) ret)[3]);
                    riljLog("RIL_UNSOL_CIPHER_INDICATION :" + simCipherStatus + " " + sessionStatus + " " + csStatus + " " + psStatus);
                    int[] cipherResult = {simCipherStatus, csStatus, psStatus};
                    if (this.mCipherIndicationRegistrant != null) {
                        this.mCipherIndicationRegistrant.notifyRegistrants(new AsyncResult((Object) null, cipherResult, (Throwable) null));
                        return;
                    }
                    return;
                case 3039:
                    unsljLog(response);
                    if (this.mCommonSlotNoChangedRegistrants != null) {
                        this.mCommonSlotNoChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
                        return;
                    }
                    return;
                case 3040:
                    unsljLog(response);
                    if (this.mDataAllowedRegistrants != null) {
                        this.mDataAllowedRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3041:
                    unsljLogvRet(response, ret);
                    if (this.mStkCallCtrlRegistrant != null) {
                        this.mStkCallCtrlRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3042:
                    unsljLogRet(response, ret);
                    if (this.mEpsNetworkFeatureSupportRegistrants != null) {
                        this.mEpsNetworkFeatureSupportRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3043:
                    unsljLog(response);
                    if (this.mCallInfoRegistrants != null) {
                        this.mCallInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3044:
                    unsljLog(response);
                    if (this.mEpsNetworkFeatureInfoRegistrants != null) {
                        this.mEpsNetworkFeatureInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3045:
                    unsljLog(response);
                    if (this.mSrvccHandoverInfoIndicationRegistrants != null) {
                        this.mSrvccHandoverInfoIndicationRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3046:
                    unsljLogvRet(response, ret);
                    if (this.mSpeechCodecInfoRegistrant != null) {
                        this.mSpeechCodecInfoRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3047:
                    unsljLogRet(response, ret);
                    return;
                case 3048:
                    unsljLog(response);
                    this.mRemoveRestrictEutranRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
                    return;
                case 3049:
                    unsljLog(response);
                    if (this.mSsacBarringInfoRegistrants != null) {
                        this.mSsacBarringInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3051:
                    unsljLogvRet(response, ret);
                    if (this.mAbnormalEventRegistrant != null) {
                        this.mAbnormalEventRegistrant.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3052:
                    unsljLog(response);
                    if (this.mEmergencyBearerSupportInfoRegistrants != null) {
                        this.mEmergencyBearerSupportInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3054:
                    unsljLogvRet(response, ret);
                    int[] rat = (int[]) ret;
                    riljLog("Notify RIL_UNSOL_GMSS_RAT_CHANGED result rat = " + rat);
                    if (this.mGmssRatChangedRegistrant != null) {
                        this.mGmssRatChangedRegistrant.notifyRegistrants(new AsyncResult((Object) null, rat, (Throwable) null));
                        return;
                    }
                    return;
                case 3058:
                    unsljLog(response);
                    if (this.mImsiRefreshDoneRegistrant != null) {
                        this.mImsiRefreshDoneRegistrant.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3060:
                    unsljLog(response);
                    if (this.mBipProCmdRegistrant != null) {
                        this.mBipProCmdRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3061:
                    unsljLog(response);
                    if (ret != null) {
                        int[] state = (int[]) ret;
                        boolean retvalue = state[0] == 0 ? WorldMode.updateSwitchingState(true) : WorldMode.updateSwitchingState(RILJ_LOGV);
                        if (retvalue) {
                            Intent intent3 = new Intent("android.intent.action.ACTION_WORLD_MODE_CHANGED");
                            intent3.putExtra("worldModeState", Integer.valueOf(state[0]));
                            this.mContext.sendBroadcast(intent3);
                            riljLog("Broadcast for WorldModeChanged: state=" + state[0]);
                            return;
                        }
                        return;
                    }
                    return;
                case 3062:
                    unsljLogvRet(response, ret);
                    if (this.mVtStatusInfoRegistrants != null) {
                        this.mVtStatusInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3063:
                    unsljLogvRet(response, ret);
                    if (this.mVtRingRegistrants != null) {
                        this.mVtRingRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3065:
                    unsljLog(response);
                    this.mResetAttachApnRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
                    return;
                case 3068:
                    unsljLogRet(response, ret);
                    if (this.mTrayPlugInRegistrants != null) {
                        this.mTrayPlugInRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3071:
                    unsljLogRet(response, ret);
                    if (this.mLteAccessStratumStateRegistrants != null) {
                        this.mLteAccessStratumStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3074:
                    unsljLogRet(response, ret);
                    if (this.mAcceptedRegistrant != null) {
                        this.mAcceptedRegistrant.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3081:
                    unsljLogRet(response, ret);
                    if (this.mNetworkExistRegistrants != null) {
                        this.mNetworkExistRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3082:
                    unsljLogRet(response, ret);
                    if (this.mModulationRegistrants != null) {
                        this.mModulationRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3083:
                    unsljLogRet(response, ret);
                    if (this.mNetworkEventRegistrants != null) {
                        this.mNetworkEventRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3093:
                    unsljLogRet(response, ret);
                    if (ret != null) {
                        int[] status = (int[]) ret;
                        Intent intent4 = new Intent("mediatek.intent.action.EMBMS_SESSION_STATUS_CHANGED");
                        intent4.putExtra("phone", this.mInstanceId);
                        intent4.putExtra("isActived", status[0]);
                        this.mContext.sendBroadcast(intent4);
                        return;
                    }
                    return;
                case 3095:
                    unsljLogRet(response, ret);
                    if (this.mPcoStatusRegistrant != null) {
                        this.mPcoStatusRegistrant.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 3096:
                    unsljLogRet(response, ret);
                    this.mAttachApnChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                    return;
                case 3098:
                    unsljLog(response);
                    String[] testTriggerOtasp = {"AT+CDV=*22899", UsimPBMemInfo.STRING_NOT_SET, "DESTRILD:C2K"};
                    invokeOemRilRequestStrings(testTriggerOtasp, null);
                    return;
                case 3099:
                    unsljLogvRet(response, ret);
                    if (this.mCallRedialStateRegistrants != null) {
                        int redialState = ((int[]) ret)[0];
                        this.mCallRedialStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, Integer.valueOf(redialState), (Throwable) null));
                        return;
                    }
                    return;
                case 3100:
                    unsljLog(response);
                    if (this.mCDMACardEsnMeidRegistrant != null) {
                        this.mCDMACardEsnMeidRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    } else {
                        this.mEspOrMeid = ret;
                        return;
                    }
                case 3103:
                    unsljLogRet(response, ret);
                    int phoneId = this.mInstanceId.intValue();
                    int[] msgs = (int[]) ret;
                    int size = msgs[0];
                    riljLog("PseudoBSRecord: phoneId=" + phoneId + ", size=" + size);
                    if (size <= 0 || size > 2) {
                        return;
                    }
                    Parcelable[] parcelableArr = new PseudoBSRecord[size];
                    for (int i = 0; i < size; i++) {
                        PseudoBSRecord record = new PseudoBSRecord(msgs[(i * 6) + 1], msgs[(i * 6) + 2], msgs[(i * 6) + 3], msgs[(i * 6) + 4], msgs[(i * 6) + 5], msgs[(i * 6) + 6]);
                        parcelableArr[i] = record;
                    }
                    Intent intent5 = new Intent("android.intent.action.ACTION_PSEUDO_BS_DETECTED");
                    intent5.putExtra(TelephonyEventLog.DATA_KEY_PHONE_ID, phoneId);
                    intent5.putExtra("pseudoInfo", parcelableArr);
                    this.mContext.sendBroadcast(intent5);
                    return;
                default:
                    return;
            }
        } catch (Throwable tr) {
            Rlog.e(RILJ_LOG_TAG, "Exception processing unsol response: " + response + "Exception:" + tr.toString());
        }
    }

    private void notifyRegistrantsRilConnectionChanged(int rilVer) {
        this.mRilVersion = rilVer;
        if (this.mRilConnectedRegistrants == null) {
            return;
        }
        this.mRilConnectedRegistrants.notifyRegistrants(new AsyncResult((Object) null, new Integer(rilVer), (Throwable) null));
    }

    private Object responseInts(Parcel p) {
        int numInts = p.readInt();
        int[] response = new int[numInts];
        for (int i = 0; i < numInts; i++) {
            response[i] = p.readInt();
        }
        return response;
    }

    private Object responseFailCause(Parcel p) {
        LastCallFailCause failCause = new LastCallFailCause();
        failCause.causeCode = p.readInt();
        if (p.dataAvail() > 0) {
            failCause.vendorCause = p.readString();
        }
        return failCause;
    }

    private Object responseVoid(Parcel p) {
        return null;
    }

    private Object responseCallForward(Parcel p) {
        int numInfos = p.readInt();
        CallForwardInfo[] infos = new CallForwardInfo[numInfos];
        for (int i = 0; i < numInfos; i++) {
            infos[i] = new CallForwardInfo();
            infos[i].status = p.readInt();
            infos[i].reason = p.readInt();
            infos[i].serviceClass = p.readInt();
            infos[i].toa = p.readInt();
            infos[i].number = p.readString();
            infos[i].timeSeconds = p.readInt();
        }
        return infos;
    }

    private Object responseSuppServiceNotification(Parcel p) {
        SuppServiceNotification notification = new SuppServiceNotification();
        notification.notificationType = p.readInt();
        notification.code = p.readInt();
        notification.index = p.readInt();
        notification.type = p.readInt();
        notification.number = p.readString();
        return notification;
    }

    private Object responseCdmaSms(Parcel p) {
        SmsMessage sms = SmsMessage.newFromParcel(p);
        return sms;
    }

    private Object responseString(Parcel p) {
        String response = p.readString();
        return response;
    }

    private Object responseStrings(Parcel p) {
        String[] response = p.readStringArray();
        return response;
    }

    private Object responseStringEncodeBase64(Parcel p) {
        String response = p.readString();
        riljLog("responseStringEncodeBase64 - Response = " + response);
        byte[] auth_output = new byte[response.length() / 2];
        for (int i = 0; i < auth_output.length; i++) {
            auth_output[i] = (byte) (auth_output[i] | (Character.digit(response.charAt(i * 2), 16) * 16));
            auth_output[i] = (byte) (auth_output[i] | Character.digit(response.charAt((i * 2) + 1), 16));
        }
        String response2 = Base64.encodeToString(auth_output, 2);
        riljLog("responseStringEncodeBase64 - Encoded Response = " + response2);
        return response2;
    }

    private Object responseRaw(Parcel p) {
        byte[] response = p.createByteArray();
        return response;
    }

    private Object responseSMS(Parcel p) {
        int messageRef = p.readInt();
        String ackPDU = p.readString();
        int errorCode = p.readInt();
        SmsResponse response = new SmsResponse(messageRef, ackPDU, errorCode);
        return response;
    }

    private Object responseICC_IO(Parcel p) {
        int sw1 = p.readInt();
        int sw2 = p.readInt();
        String s = p.readString();
        return new IccIoResult(sw1, sw2, s);
    }

    private Object responseICC_IOBase64(Parcel p) {
        int sw1 = p.readInt();
        int sw2 = p.readInt();
        String s = p.readString();
        return new IccIoResult(sw1, sw2, Base64.decode(s, 0));
    }

    private Object responseIccCardStatus(Parcel p) {
        IccCardStatus cardStatus = new IccCardStatus();
        cardStatus.setCardState(p.readInt());
        cardStatus.setUniversalPinState(p.readInt());
        cardStatus.mGsmUmtsSubscriptionAppIndex = p.readInt();
        cardStatus.mCdmaSubscriptionAppIndex = p.readInt();
        cardStatus.mImsSubscriptionAppIndex = p.readInt();
        int numApplications = p.readInt();
        if (numApplications > 8) {
            numApplications = 8;
        }
        cardStatus.mApplications = new IccCardApplicationStatus[numApplications];
        for (int i = 0; i < numApplications; i++) {
            IccCardApplicationStatus appStatus = new IccCardApplicationStatus();
            appStatus.app_type = appStatus.AppTypeFromRILInt(p.readInt());
            appStatus.app_state = appStatus.AppStateFromRILInt(p.readInt());
            appStatus.perso_substate = appStatus.PersoSubstateFromRILInt(p.readInt());
            appStatus.aid = p.readString();
            appStatus.app_label = p.readString();
            appStatus.pin1_replaced = p.readInt();
            appStatus.pin1 = appStatus.PinStateFromRILInt(p.readInt());
            appStatus.pin2 = appStatus.PinStateFromRILInt(p.readInt());
            cardStatus.mApplications[i] = appStatus;
        }
        return cardStatus;
    }

    private Object responseSimRefresh(Parcel p) {
        IccRefreshResponse response = new IccRefreshResponse();
        response.refreshResult = p.readInt();
        response.efId = p.readInt();
        response.aid = p.readString();
        return response;
    }

    private Object responseCallList(Parcel p) {
        int num = p.readInt();
        ArrayList<DriverCall> response = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            DriverCall dc = new DriverCall();
            dc.state = DriverCall.stateFromCLCC(p.readInt());
            dc.index = p.readInt();
            dc.TOA = p.readInt();
            dc.isMpty = p.readInt() != 0;
            dc.isMT = p.readInt() != 0;
            dc.als = p.readInt();
            int voiceSettings = p.readInt();
            dc.isVoice = voiceSettings != 0;
            dc.isVideo = !dc.isVoice;
            riljLog("isVoice = " + dc.isVoice + ", isVideo = " + dc.isVideo);
            dc.isVoicePrivacy = p.readInt() != 0;
            dc.number = p.readString();
            int np = p.readInt();
            dc.numberPresentation = DriverCall.presentationFromCLIP(np);
            dc.name = p.readString();
            dc.namePresentation = DriverCall.presentationFromCLIP(p.readInt());
            int uusInfoPresent = p.readInt();
            if (uusInfoPresent == 1) {
                dc.uusInfo = new UUSInfo();
                dc.uusInfo.setType(p.readInt());
                dc.uusInfo.setDcs(p.readInt());
                byte[] userData = p.createByteArray();
                dc.uusInfo.setUserData(userData);
                riljLogv(String.format("Incoming UUS : type=%d, dcs=%d, length=%d", Integer.valueOf(dc.uusInfo.getType()), Integer.valueOf(dc.uusInfo.getDcs()), Integer.valueOf(dc.uusInfo.getUserData().length)));
                riljLogv("Incoming UUS : data (string)=" + new String(dc.uusInfo.getUserData()));
                riljLogv("Incoming UUS : data (hex): " + IccUtils.bytesToHexString(dc.uusInfo.getUserData()));
            } else {
                riljLogv("Incoming UUS : NOT present!");
            }
            dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);
            response.add(dc);
            if (dc.isVoicePrivacy) {
                this.mVoicePrivacyOnRegistrants.notifyRegistrants();
                riljLog("InCall VoicePrivacy is enabled");
            } else {
                this.mVoicePrivacyOffRegistrants.notifyRegistrants();
                riljLog("InCall VoicePrivacy is disabled");
            }
        }
        Collections.sort(response);
        if (num == 0 && this.mTestingEmergencyCall.getAndSet(RILJ_LOGV) && this.mEmergencyCallbackModeRegistrant != null) {
            riljLog("responseCallList: call ended, testing emergency call, notify ECM Registrants");
            this.mEmergencyCallbackModeRegistrant.notifyRegistrant();
        }
        return response;
    }

    private DataCallResponse getDataCallResponse(Parcel p, int version) {
        DataCallResponse dataCall = new DataCallResponse();
        dataCall.version = version;
        if (version < 5) {
            dataCall.cid = p.readInt();
            dataCall.active = p.readInt();
            dataCall.type = p.readString();
            String addresses = p.readString();
            if (!TextUtils.isEmpty(addresses)) {
                dataCall.addresses = addresses.split(" ");
            }
        } else {
            dataCall.status = p.readInt();
            dataCall.suggestedRetryTime = p.readInt();
            dataCall.cid = p.readInt();
            dataCall.active = p.readInt();
            dataCall.type = p.readString();
            dataCall.ifname = p.readString();
            if (dataCall.status == DcFailCause.NONE.getErrorCode() && TextUtils.isEmpty(dataCall.ifname)) {
                throw new RuntimeException("getDataCallResponse, no ifname");
            }
            String addresses2 = p.readString();
            if (!TextUtils.isEmpty(addresses2)) {
                dataCall.addresses = addresses2.split(" ");
            }
            String dnses = p.readString();
            if (!TextUtils.isEmpty(dnses)) {
                dataCall.dnses = dnses.split(" ");
            }
            String gateways = p.readString();
            if (!TextUtils.isEmpty(gateways)) {
                dataCall.gateways = gateways.split(" ");
            }
            String pcscf = p.readString();
            if (!TextUtils.isEmpty(pcscf)) {
                dataCall.pcscf = pcscf.split(" ");
            }
            dataCall.mtu = p.readInt();
            dataCall.rat = p.readInt();
        }
        return dataCall;
    }

    private Object responseDataCallList(Parcel p) {
        int ver = p.readInt();
        int num = p.readInt();
        riljLog("responseDataCallList ver=" + ver + " num=" + num);
        ArrayList<DataCallResponse> response = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            response.add(getDataCallResponse(p, ver));
        }
        this.mEventLog.writeRilDataCallList(response);
        return response;
    }

    private Object responseSetupDataCall(Parcel p) {
        int ver = p.readInt();
        int num = p.readInt();
        if (ver < 5) {
            DataCallResponse dataCall = new DataCallResponse();
            dataCall.version = ver;
            dataCall.cid = Integer.parseInt(p.readString());
            dataCall.ifname = p.readString();
            if (TextUtils.isEmpty(dataCall.ifname)) {
                throw new RuntimeException("RIL_REQUEST_SETUP_DATA_CALL response, no ifname");
            }
            String addresses = p.readString();
            if (!TextUtils.isEmpty(addresses)) {
                dataCall.addresses = addresses.split(" ");
            }
            if (num >= 4) {
                String dnses = p.readString();
                riljLog("responseSetupDataCall got dnses=" + dnses);
                if (!TextUtils.isEmpty(dnses)) {
                    dataCall.dnses = dnses.split(" ");
                }
            }
            if (num >= 5) {
                String gateways = p.readString();
                riljLog("responseSetupDataCall got gateways=" + gateways);
                if (!TextUtils.isEmpty(gateways)) {
                    dataCall.gateways = gateways.split(" ");
                }
            }
            if (num >= 6) {
                String pcscf = p.readString();
                riljLog("responseSetupDataCall got pcscf=" + pcscf);
                if (!TextUtils.isEmpty(pcscf)) {
                    dataCall.pcscf = pcscf.split(" ");
                    return dataCall;
                }
                return dataCall;
            }
            return dataCall;
        }
        if (num != 1) {
            throw new RuntimeException("RIL_REQUEST_SETUP_DATA_CALL resp. expecting 1 RIL_Data_Call_response_v5 got " + num);
        }
        return getDataCallResponse(p, ver);
    }

    private Object responseOperatorInfos(Parcel p) {
        String strOperatorLong;
        String[] strings = (String[]) responseStrings(p);
        SpnOverride spnOverride = SpnOverride.getInstance();
        if (strings.length % 4 != 0) {
            throw new RuntimeException("RIL_REQUEST_QUERY_AVAILABLE_NETWORKS: invalid response. Got " + strings.length + " strings, expected multible of 4");
        }
        ArrayList<OperatorInfo> ret = new ArrayList<>(strings.length / 4);
        for (int i = 0; i < strings.length; i += 4) {
            if (spnOverride.containsCarrierEx(strings[i + 2])) {
                strOperatorLong = spnOverride.getSpnEx(strings[i + 2]);
            } else {
                strOperatorLong = strings[i + 0];
            }
            ret.add(new OperatorInfo(strOperatorLong, strings[i + 1], strings[i + 2], strings[i + 3]));
        }
        return ret;
    }

    private Object responseOperatorInfosWithAct(Parcel p) {
        String sCphsOns;
        String[] strings = (String[]) responseStrings(p);
        if (strings.length % 5 != 0) {
            throw new RuntimeException("RIL_REQUEST_QUERY_AVAILABLE_NETWORKS_WITH_ACT: invalid response. Got " + strings.length + " strings, expected multible of 5");
        }
        String lacStr = SystemProperties.get("gsm.cops.lac");
        boolean lacValid = RILJ_LOGV;
        int lacIndex = 0;
        Rlog.d(RILJ_LOG_TAG, "lacStr = " + lacStr + " lacStr.length=" + lacStr.length() + " strings.length=" + strings.length);
        if (lacStr.length() > 0 && lacStr.length() % 4 == 0 && lacStr.length() / 4 == strings.length / 5) {
            Rlog.d(RILJ_LOG_TAG, "lacValid set to true");
            lacValid = true;
        }
        SystemProperties.set("gsm.cops.lac", UsimPBMemInfo.STRING_NOT_SET);
        ArrayList<OperatorInfo> ret = new ArrayList<>(strings.length / 5);
        for (int i = 0; i < strings.length; i += 5) {
            if (strings[i + 2] != null) {
                strings[i + 0] = SpnOverride.getInstance().lookupOperatorName(SubscriptionManager.getSubIdUsingPhoneId(this.mInstanceId.intValue()), strings[i + 2], true, this.mContext);
                strings[i + 1] = SpnOverride.getInstance().lookupOperatorName(SubscriptionManager.getSubIdUsingPhoneId(this.mInstanceId.intValue()), strings[i + 2], RILJ_LOGV, this.mContext);
                riljLog("lookup RIL responseOperator(), longAlpha= " + strings[i + 0] + ",shortAlpha= " + strings[i + 1] + ",numeric=" + strings[i + 2]);
            }
            String longName = lookupOperatorNameFromNetwork(SubscriptionManager.getSubIdUsingPhoneId(this.mInstanceId.intValue()), strings[i + 2], true);
            String shortName = lookupOperatorNameFromNetwork(SubscriptionManager.getSubIdUsingPhoneId(this.mInstanceId.intValue()), strings[i + 2], RILJ_LOGV);
            if (!TextUtils.isEmpty(longName)) {
                strings[i + 0] = longName;
            }
            if (!TextUtils.isEmpty(shortName)) {
                strings[i + 1] = shortName;
            }
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    strings[i + 0] = this.mServiceStateExt.updateOpAlphaLongForHK(strings[i + 0], strings[i + 2], this.mInstanceId.intValue());
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
            riljLog("lookupOperatorNameFromNetwork in responseOperatorInfosWithAct(),updated longAlpha= " + strings[i + 0] + ",shortAlpha= " + strings[i + 1] + ",numeric=" + strings[i + 2]);
            if (lacValid && strings[i + 0] != null) {
                this.mInstanceId.intValue();
                UiccController uiccController = UiccController.getInstance();
                SIMRecords simRecord = (SIMRecords) uiccController.getIccRecords(this.mInstanceId.intValue(), 1);
                String lac = lacStr.substring(lacIndex, lacIndex + 4);
                Rlog.d(RILJ_LOG_TAG, "lacIndex=" + lacIndex + " lacValue=-1 lac=" + lac + " plmn numeric=" + strings[i + 2] + " plmn name" + strings[i + 0]);
                if (lac != UsimPBMemInfo.STRING_NOT_SET) {
                    int lacValue = Integer.parseInt(lac, 16);
                    lacIndex += 4;
                    if (lacValue != 65534) {
                        String sEons = simRecord.getEonsIfExist(strings[i + 2], lacValue, true);
                        if (sEons == null || sEons.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                            String mSimOperatorNumeric = simRecord.getOperatorNumeric();
                            if (mSimOperatorNumeric != null && mSimOperatorNumeric.equals(strings[i + 2]) && (sCphsOns = simRecord.getSIMCPHSOns()) != null) {
                                strings[i + 0] = sCphsOns;
                                Rlog.d(RILJ_LOG_TAG, "plmn name update to CPHS Ons: " + strings[i + 0]);
                            }
                        } else {
                            strings[i + 0] = sEons;
                            Rlog.d(RILJ_LOG_TAG, "plmn name update to Eons: " + strings[i + 0]);
                        }
                    } else {
                        Rlog.d(RILJ_LOG_TAG, "invalid lac ignored");
                    }
                }
            }
            strings[i + 0] = strings[i + 0].concat(" " + strings[i + 4]);
            strings[i + 1] = strings[i + 1].concat(" " + strings[i + 4]);
            ret.add(new OperatorInfo(strings[i + 0], strings[i + 1], strings[i + 2], strings[i + 3]));
        }
        return ret;
    }

    private Object responseCellList(Parcel p) {
        int num = p.readInt();
        ArrayList<NeighboringCellInfo> response = new ArrayList<>();
        int radioType = SystemProperties.getInt("gsm.enbr.rat", 1);
        riljLog("gsm.enbr.rat=" + radioType);
        if (radioType != 0) {
            for (int i = 0; i < num; i++) {
                int rssi = p.readInt();
                String location = p.readString();
                NeighboringCellInfo cell = new NeighboringCellInfo(rssi, location, radioType);
                response.add(cell);
            }
        }
        return response;
    }

    private Object responseSetPreferredNetworkType(Parcel p) {
        int count = getRequestCount(73);
        if (count == 0) {
            Intent intent = new Intent("android.intent.action.ACTION_RAT_CHANGED");
            intent.putExtra("phone", this.mInstanceId);
            intent.putExtra("rat", this.mPreferredNetworkType);
            this.mContext.sendBroadcast(intent);
        }
        riljLog("SetRatRequestCount: " + count);
        return null;
    }

    private Object responseGetPreferredNetworkType(Parcel p) {
        int[] response = (int[]) responseInts(p);
        if (response.length >= 1) {
        }
        return response;
    }

    private int getRequestCount(int reuestId) {
        int count = 0;
        synchronized (this.mRequestList) {
            int s = this.mRequestList.size();
            for (int i = 0; i < s; i++) {
                RILRequest rr = this.mRequestList.valueAt(i);
                if (rr != null && rr.mRequest == reuestId) {
                    count++;
                }
            }
        }
        return count;
    }

    private Object responseGmsBroadcastConfig(Parcel p) {
        int num = p.readInt();
        ArrayList<SmsBroadcastConfigInfo> response = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            int fromId = p.readInt();
            int toId = p.readInt();
            int fromScheme = p.readInt();
            int toScheme = p.readInt();
            boolean selected = p.readInt() == 1 ? true : RILJ_LOGV;
            SmsBroadcastConfigInfo info = new SmsBroadcastConfigInfo(fromId, toId, fromScheme, toScheme, selected);
            response.add(info);
        }
        return response;
    }

    private Object responseCdmaBroadcastConfig(Parcel p) {
        int[] response;
        int numServiceCategories = p.readInt();
        if (numServiceCategories == 0) {
            response = new int[94];
            response[0] = 31;
            for (int i = 1; i < 94; i += 3) {
                response[i + 0] = i / 3;
                response[i + 1] = 1;
                response[i + 2] = 0;
            }
        } else {
            int numInts = (numServiceCategories * 3) + 1;
            response = new int[numInts];
            response[0] = numServiceCategories;
            for (int i2 = 1; i2 < numInts; i2++) {
                response[i2] = p.readInt();
            }
        }
        return response;
    }

    private Object responseSignalStrength(Parcel p) {
        SignalStrength signalStrength = SignalStrength.makeSignalStrengthFromRilParcel(p);
        return signalStrength;
    }

    private ArrayList<CdmaInformationRecords> responseCdmaInformationRecord(Parcel p) {
        int numberOfInfoRecs = p.readInt();
        ArrayList<CdmaInformationRecords> response = new ArrayList<>(numberOfInfoRecs);
        for (int i = 0; i < numberOfInfoRecs; i++) {
            CdmaInformationRecords InfoRec = new CdmaInformationRecords(p);
            response.add(InfoRec);
        }
        return response;
    }

    private Object responseCdmaCallWaiting(Parcel p) {
        CdmaCallWaitingNotification notification = new CdmaCallWaitingNotification();
        notification.number = p.readString();
        notification.numberPresentation = CdmaCallWaitingNotification.presentationFromCLIP(p.readInt());
        notification.name = p.readString();
        notification.namePresentation = notification.numberPresentation;
        notification.isPresent = p.readInt();
        notification.signalType = p.readInt();
        notification.alertPitch = p.readInt();
        notification.signal = p.readInt();
        notification.numberType = p.readInt();
        notification.numberPlan = p.readInt();
        return notification;
    }

    private Object responseCallRing(Parcel p) {
        char[] response = {(char) p.readInt(), (char) p.readInt(), (char) p.readInt(), (char) p.readInt()};
        this.mEventLog.writeRilCallRing(response);
        return response;
    }

    private Object responseFemtoCellInfos(Parcel p) {
        String actStr;
        int rat;
        String[] strings = (String[]) responseStrings(p);
        if (strings.length % 6 != 0) {
            throw new RuntimeException("RIL_REQUEST_GET_FEMTOCELL_LIST: invalid response. Got " + strings.length + " strings, expected multible of 6");
        }
        ArrayList<FemtoCellInfo> ret = new ArrayList<>(strings.length / 6);
        for (int i = 0; i < strings.length; i += 6) {
            if (strings[i + 1] != null && strings[i + 1].startsWith("uCs2")) {
                Rlog.d(RILJ_LOG_TAG, "responseOperatorInfos handling UCS2 format name");
                try {
                    strings[i + 0] = new String(IccUtils.hexStringToBytes(strings[i + 1].substring(4)), "UTF-16");
                } catch (UnsupportedEncodingException e) {
                    Rlog.d(RILJ_LOG_TAG, "responseOperatorInfos UnsupportedEncodingException");
                }
            }
            if (strings[i + 1] != null && (strings[i + 1].equals(UsimPBMemInfo.STRING_NOT_SET) || strings[i + 1].equals(strings[i + 0]))) {
                Rlog.d(RILJ_LOG_TAG, "lookup RIL responseFemtoCellInfos() for plmn id= " + strings[i + 0]);
            }
            if (strings[i + 2].equals(Phone.ACT_TYPE_LTE)) {
                actStr = Phone.LTE_INDICATOR;
                rat = 14;
            } else if (strings[i + 2].equals(Phone.ACT_TYPE_UTRAN)) {
                actStr = Phone.UTRAN_INDICATOR;
                rat = 3;
            } else {
                actStr = "2G";
                rat = 1;
            }
            String property_name = this.mInstanceId.intValue() > 0 ? "gsm.baseband.capability" + (this.mInstanceId.intValue() + 1) : "gsm.baseband.capability";
            int basebandCapability = SystemProperties.getInt(property_name, 3);
            Rlog.d(RILJ_LOG_TAG, "property_name=" + property_name + ",basebandCapability=" + basebandCapability);
            if (3 < basebandCapability) {
                strings[i + 1] = strings[i + 1].concat(" " + actStr);
            }
            String hnbName = new String(IccUtils.hexStringToBytes(strings[i + 5]));
            Rlog.d(RILJ_LOG_TAG, "FemtoCellInfo(" + strings[i + 3] + "," + strings[i + 4] + "," + strings[i + 5] + "," + strings[i + 0] + "," + strings[i + 1] + "," + rat + ")hnbName=" + hnbName);
            ret.add(new FemtoCellInfo(Integer.parseInt(strings[i + 3]), Integer.parseInt(strings[i + 4]), hnbName, strings[i + 0], strings[i + 1], rat));
        }
        return ret;
    }

    private void notifyRegistrantsCdmaInfoRec(CdmaInformationRecords infoRec) {
        if (infoRec.record instanceof CdmaInformationRecords.CdmaDisplayInfoRec) {
            if (this.mDisplayInfoRegistrants == null) {
                return;
            }
            unsljLogRet(1027, infoRec.record);
            this.mDisplayInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
            return;
        }
        if (infoRec.record instanceof CdmaInformationRecords.CdmaSignalInfoRec) {
            if (this.mSignalInfoRegistrants == null) {
                return;
            }
            unsljLogRet(1027, infoRec.record);
            this.mSignalInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
            return;
        }
        if (infoRec.record instanceof CdmaInformationRecords.CdmaNumberInfoRec) {
            if (this.mNumberInfoRegistrants == null) {
                return;
            }
            unsljLogRet(1027, infoRec.record);
            this.mNumberInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
            return;
        }
        if (infoRec.record instanceof CdmaInformationRecords.CdmaRedirectingNumberInfoRec) {
            if (this.mRedirNumInfoRegistrants == null) {
                return;
            }
            unsljLogRet(1027, infoRec.record);
            this.mRedirNumInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
            return;
        }
        if (infoRec.record instanceof CdmaInformationRecords.CdmaLineControlInfoRec) {
            if (this.mLineControlInfoRegistrants == null) {
                return;
            }
            unsljLogRet(1027, infoRec.record);
            this.mLineControlInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
            return;
        }
        if (infoRec.record instanceof CdmaInformationRecords.CdmaT53ClirInfoRec) {
            if (this.mT53ClirInfoRegistrants == null) {
                return;
            }
            unsljLogRet(1027, infoRec.record);
            this.mT53ClirInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
            return;
        }
        if (!(infoRec.record instanceof CdmaInformationRecords.CdmaT53AudioControlInfoRec) || this.mT53AudCntrlInfoRegistrants == null) {
            return;
        }
        unsljLogRet(1027, infoRec.record);
        this.mT53AudCntrlInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
    }

    private ArrayList<CellInfo> responseCellInfoList(Parcel p) {
        int numberOfInfoRecs = p.readInt();
        ArrayList<CellInfo> response = new ArrayList<>(numberOfInfoRecs);
        for (int i = 0; i < numberOfInfoRecs; i++) {
            CellInfo InfoRec = (CellInfo) CellInfo.CREATOR.createFromParcel(p);
            response.add(InfoRec);
        }
        return response;
    }

    private Object responseHardwareConfig(Parcel p) {
        HardwareConfig hw;
        int num = p.readInt();
        ArrayList<HardwareConfig> response = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            int type = p.readInt();
            switch (type) {
                case 0:
                    hw = new HardwareConfig(type);
                    hw.assignModem(p.readString(), p.readInt(), p.readInt(), p.readInt(), p.readInt(), p.readInt(), p.readInt());
                    break;
                case 1:
                    hw = new HardwareConfig(type);
                    hw.assignSim(p.readString(), p.readInt(), p.readString());
                    break;
                default:
                    throw new RuntimeException("RIL_REQUEST_GET_HARDWARE_CONFIG invalid hardward type:" + type);
            }
            response.add(hw);
        }
        return response;
    }

    private Object responseRadioCapability(Parcel p) {
        int version = p.readInt();
        int session = p.readInt();
        int phase = p.readInt();
        int rat = p.readInt();
        String logicModemUuid = p.readString();
        int status = p.readInt();
        riljLog("responseRadioCapability: version= " + version + ", session=" + session + ", phase=" + phase + ", rat=" + rat + ", logicModemUuid=" + logicModemUuid + ", status=" + status);
        RadioCapability rc = new RadioCapability(this.mInstanceId.intValue(), session, phase, rat, logicModemUuid, status);
        return rc;
    }

    private Object responseLceData(Parcel p) {
        ArrayList<Integer> capacityResponse = new ArrayList<>();
        int capacityDownKbps = p.readInt();
        int confidenceLevel = p.readByte();
        int lceSuspended = p.readByte();
        riljLog("LCE capacity information received: capacity=" + capacityDownKbps + " confidence=" + confidenceLevel + " lceSuspended=" + lceSuspended);
        capacityResponse.add(Integer.valueOf(capacityDownKbps));
        capacityResponse.add(Integer.valueOf(confidenceLevel));
        capacityResponse.add(Integer.valueOf(lceSuspended));
        return capacityResponse;
    }

    private Object responseLceStatus(Parcel p) {
        ArrayList<Integer> statusResponse = new ArrayList<>();
        int lceStatus = p.readByte();
        int actualInterval = p.readInt();
        riljLog("LCE status information received: lceStatus=" + lceStatus + " actualInterval=" + actualInterval);
        statusResponse.add(Integer.valueOf(lceStatus));
        statusResponse.add(Integer.valueOf(actualInterval));
        return statusResponse;
    }

    private Object responseActivityData(Parcel p) {
        int sleepModeTimeMs = p.readInt();
        int idleModeTimeMs = p.readInt();
        int[] txModeTimeMs = new int[5];
        for (int i = 0; i < 5; i++) {
            txModeTimeMs[i] = p.readInt();
        }
        int rxModeTimeMs = p.readInt();
        riljLog("Modem activity info received: sleepModeTimeMs=" + sleepModeTimeMs + " idleModeTimeMs=" + idleModeTimeMs + " txModeTimeMs[]=" + Arrays.toString(txModeTimeMs) + " rxModeTimeMs=" + rxModeTimeMs);
        return new ModemActivityInfo(SystemClock.elapsedRealtime(), sleepModeTimeMs, idleModeTimeMs, txModeTimeMs, rxModeTimeMs, 0);
    }

    static String requestToString(int request) {
        switch (request) {
            case 1:
                return "GET_SIM_STATUS";
            case 2:
                return "ENTER_SIM_PIN";
            case 3:
                return "ENTER_SIM_PUK";
            case 4:
                return "ENTER_SIM_PIN2";
            case 5:
                return "ENTER_SIM_PUK2";
            case 6:
                return "CHANGE_SIM_PIN";
            case 7:
                return "CHANGE_SIM_PIN2";
            case 8:
                return "ENTER_NETWORK_DEPERSONALIZATION";
            case 9:
                return "GET_CURRENT_CALLS";
            case 10:
                return "DIAL";
            case 11:
                return "GET_IMSI";
            case 12:
                return "HANGUP";
            case 13:
                return "HANGUP_WAITING_OR_BACKGROUND";
            case 14:
                return "HANGUP_FOREGROUND_RESUME_BACKGROUND";
            case 15:
                return "REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE";
            case 16:
                return "CONFERENCE";
            case 17:
                return "UDUB";
            case 18:
                return "LAST_CALL_FAIL_CAUSE";
            case 19:
                return "SIGNAL_STRENGTH";
            case 20:
                return "VOICE_REGISTRATION_STATE";
            case 21:
                return "DATA_REGISTRATION_STATE";
            case 22:
                return "OPERATOR";
            case SmsHeader.ELT_ID_OBJECT_DISTR_INDICATOR:
                return "RADIO_POWER";
            case 24:
                return "DTMF";
            case 25:
                return "SEND_SMS";
            case 26:
                return "SEND_SMS_EXPECT_MORE";
            case CallFailCause.DESTINATION_OUT_OF_ORDER:
                return "SETUP_DATA_CALL";
            case CallFailCause.INVALID_NUMBER_FORMAT:
                return "SIM_IO";
            case CallFailCause.FACILITY_REJECTED:
                return "SEND_USSD";
            case 30:
                return "CANCEL_USSD";
            case 31:
                return "GET_CLIR";
            case 32:
                return "SET_CLIR";
            case 33:
                return "QUERY_CALL_FORWARD_STATUS";
            case 34:
                return "SET_CALL_FORWARD";
            case 35:
                return "QUERY_CALL_WAITING";
            case 36:
                return "SET_CALL_WAITING";
            case 37:
                return "SMS_ACKNOWLEDGE";
            case 38:
                return "GET_IMEI";
            case 39:
                return "GET_IMEISV";
            case 40:
                return "ANSWER";
            case 41:
                return "DEACTIVATE_DATA_CALL";
            case 42:
                return "QUERY_FACILITY_LOCK";
            case CallFailCause.ACCESS_INFORMATION_DISCARDED:
                return "SET_FACILITY_LOCK";
            case CallFailCause.CHANNEL_NOT_AVAIL:
                return "CHANGE_BARRING_PASSWORD";
            case 45:
                return "QUERY_NETWORK_SELECTION_MODE";
            case CatService.MSG_ID_CACHED_DISPLAY_TEXT_TIMEOUT:
                return "SET_NETWORK_SELECTION_AUTOMATIC";
            case 47:
                return "SET_NETWORK_SELECTION_MANUAL";
            case 48:
                return "QUERY_AVAILABLE_NETWORKS ";
            case 49:
                return "DTMF_START";
            case 50:
                return "DTMF_STOP";
            case RadioNVItems.RIL_NV_CDMA_PRL_VERSION:
                return "BASEBAND_VERSION";
            case RadioNVItems.RIL_NV_CDMA_BC10:
                return "SEPARATE_CONNECTION";
            case 53:
                return "SET_MUTE";
            case RadioNVItems.RIL_NV_CDMA_SO68:
                return "GET_MUTE";
            case 55:
                return "QUERY_CLIP";
            case 56:
                return "LAST_DATA_CALL_FAIL_CAUSE";
            case 57:
                return "DATA_CALL_LIST";
            case 58:
                return "RESET_RADIO";
            case RadioNVItems.RIL_NV_CDMA_EHRPD_FORCED:
                return "OEM_HOOK_RAW";
            case 60:
                return "OEM_HOOK_STRINGS";
            case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_2:
                return "SCREEN_STATE";
            case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_3:
                return "SET_SUPP_SVC_NOTIFICATION";
            case 63:
                return "WRITE_SMS_TO_SIM";
            case 64:
                return "DELETE_SMS_ON_SIM";
            case CallFailCause.BEARER_NOT_IMPLEMENT:
                return "SET_BAND_MODE";
            case 66:
                return "QUERY_AVAILABLE_BAND_MODE";
            case 67:
                return "REQUEST_STK_GET_PROFILE";
            case CallFailCause.ACM_LIMIT_EXCEEDED:
                return "REQUEST_STK_SET_PROFILE";
            case CallFailCause.FACILITY_NOT_IMPLEMENT:
                return "REQUEST_STK_SEND_ENVELOPE_COMMAND";
            case 70:
                return "REQUEST_STK_SEND_TERMINAL_RESPONSE";
            case 71:
                return "REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM";
            case 72:
                return "REQUEST_EXPLICIT_CALL_TRANSFER";
            case 73:
                return "REQUEST_SET_PREFERRED_NETWORK_TYPE";
            case RadioNVItems.RIL_NV_LTE_SCAN_PRIORITY_25:
                return "REQUEST_GET_PREFERRED_NETWORK_TYPE";
            case RadioNVItems.RIL_NV_LTE_SCAN_PRIORITY_26:
                return "REQUEST_GET_NEIGHBORING_CELL_IDS";
            case RadioNVItems.RIL_NV_LTE_SCAN_PRIORITY_41:
                return "REQUEST_SET_LOCATION_UPDATES";
            case RadioNVItems.RIL_NV_LTE_HIDDEN_BAND_PRIORITY_25:
                return "RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE";
            case RadioNVItems.RIL_NV_LTE_HIDDEN_BAND_PRIORITY_26:
                return "RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE";
            case 79:
                return "RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE";
            case RadioNVItems.RIL_NV_LTE_NEXT_SCAN:
                return "RIL_REQUEST_SET_TTY_MODE";
            case 81:
                return "RIL_REQUEST_QUERY_TTY_MODE";
            case RadioNVItems.RIL_NV_LTE_BSR_MAX_TIME:
                return "RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE";
            case 83:
                return "RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE";
            case 84:
                return "RIL_REQUEST_CDMA_FLASH";
            case 85:
                return "RIL_REQUEST_CDMA_BURST_DTMF";
            case 86:
                return "RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY";
            case 87:
                return "RIL_REQUEST_CDMA_SEND_SMS";
            case CallFailCause.INCOMPATIBLE_DESTINATION:
                return "RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE";
            case 89:
                return "RIL_REQUEST_GSM_GET_BROADCAST_CONFIG";
            case 90:
                return "RIL_REQUEST_GSM_SET_BROADCAST_CONFIG";
            case CallFailCause.INVALID_TRANSIT_NETWORK_SELECTION:
                return "RIL_REQUEST_GSM_BROADCAST_ACTIVATION";
            case 92:
                return "RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG";
            case 93:
                return "RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG";
            case 94:
                return "RIL_REQUEST_CDMA_BROADCAST_ACTIVATION";
            case CallFailCause.SEMANTICALLY_INCORRECT_MESSAGE:
                return "RIL_REQUEST_CDMA_SUBSCRIPTION";
            case 96:
                return "RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM";
            case CallFailCause.MESSAGE_TYPE_NON_EXISTENT:
                return "RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM";
            case CallFailCause.MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE:
                return "RIL_REQUEST_DEVICE_IDENTITY";
            case CallFailCause.IE_NON_EXISTENT_OR_NOT_IMPLEMENTED:
                return "REQUEST_EXIT_EMERGENCY_CALLBACK_MODE";
            case 100:
                return "RIL_REQUEST_GET_SMSC_ADDRESS";
            case 101:
                return "RIL_REQUEST_SET_SMSC_ADDRESS";
            case CallFailCause.RECOVERY_ON_TIMER_EXPIRY:
                return "RIL_REQUEST_REPORT_SMS_MEMORY_STATUS";
            case 103:
                return "RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING";
            case CharacterSets.ISO_2022_CN:
                return "RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE";
            case CharacterSets.ISO_2022_CN_EXT:
                return "RIL_REQUEST_ISIM_AUTHENTICATION";
            case 106:
                return "RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU";
            case 107:
                return "RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS";
            case 108:
                return "RIL_REQUEST_VOICE_RADIO_TECH";
            case CharacterSets.ISO_8859_13:
                return "RIL_REQUEST_GET_CELL_INFO_LIST";
            case CharacterSets.ISO_8859_14:
                return "RIL_REQUEST_SET_CELL_INFO_LIST_RATE";
            case 111:
                return "RIL_REQUEST_SET_INITIAL_ATTACH_APN";
            case CharacterSets.ISO_8859_16:
                return "RIL_REQUEST_IMS_REGISTRATION_STATE";
            case CharacterSets.GBK:
                return "RIL_REQUEST_IMS_SEND_SMS";
            case CharacterSets.GB18030:
                return "RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC";
            case 115:
                return "RIL_REQUEST_SIM_OPEN_CHANNEL";
            case 116:
                return "RIL_REQUEST_SIM_CLOSE_CHANNEL";
            case 117:
                return "RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL";
            case 118:
                return "RIL_REQUEST_NV_READ_ITEM";
            case 119:
                return "RIL_REQUEST_NV_WRITE_ITEM";
            case 120:
                return "RIL_REQUEST_NV_WRITE_CDMA_PRL";
            case 121:
                return "RIL_REQUEST_NV_RESET_CONFIG";
            case 122:
                return "RIL_REQUEST_SET_UICC_SUBSCRIPTION";
            case 123:
                return "RIL_REQUEST_ALLOW_DATA";
            case 124:
                return "GET_HARDWARE_CONFIG";
            case 125:
                return "RIL_REQUEST_SIM_AUTHENTICATION";
            case 128:
                return "RIL_REQUEST_SET_DATA_PROFILE";
            case 129:
                return "RIL_REQUEST_SHUTDOWN";
            case 130:
                return "RIL_REQUEST_GET_RADIO_CAPABILITY";
            case 131:
                return "RIL_REQUEST_SET_RADIO_CAPABILITY";
            case 132:
                return "RIL_REQUEST_START_LCE";
            case 133:
                return "RIL_REQUEST_STOP_LCE";
            case 134:
                return "RIL_REQUEST_PULL_LCEDATA";
            case 135:
                return "RIL_REQUEST_GET_ACTIVITY_INFO";
            case 800:
                return "RIL_RESPONSE_ACKNOWLEDGEMENT";
            case 2000:
                return "GET_COLP";
            case TelephonyEventLog.TAG_IMS_CALL_START:
                return "SET_COLP";
            case TelephonyEventLog.TAG_IMS_CALL_START_CONFERENCE:
                return "GET_COLR";
            case TelephonyEventLog.TAG_IMS_CALL_UPDATE:
                return "MODEM_POWEROFF";
            case TelephonyEventLog.TAG_IMS_CALL_STARTED:
                return "RIL_REQUEST_QUERY_PHB_STORAGE_INFO";
            case TelephonyEventLog.TAG_IMS_CALL_START_FAILED:
                return "RIL_REQUEST_WRITE_PHB_ENTRY";
            case TelephonyEventLog.TAG_IMS_CALL_TERMINATED:
                return "RIL_REQUEST_READ_PHB_ENTRY";
            case TelephonyEventLog.TAG_IMS_CALL_HOLD_FAILED:
                return "QUERY_SIM_NETWORK_LOCK";
            case TelephonyEventLog.TAG_IMS_CALL_HOLD_RECEIVED:
                return "SET_SIM_NETWORK_LOCK";
            case TelephonyEventLog.TAG_IMS_CALL_RESUMED:
                return "SET_NETWORK_SELECTION_MANUAL_WITH_ACT";
            case TelephonyEventLog.TAG_IMS_CALL_RESUME_RECEIVED:
                return "MODEM_POWERON";
            case TelephonyEventLog.TAG_IMS_CALL_UPDATED:
                return "RIL_REQUEST_GET_SMS_SIM_MEM_STATUS";
            case TelephonyEventLog.TAG_IMS_CALL_UPDATE_FAILED:
                return "RIL_REQUEST_GET_POL_CAPABILITY";
            case TelephonyEventLog.TAG_IMS_CALL_MERGED:
                return "RIL_REQUEST_GET_POL_LIST";
            case TelephonyEventLog.TAG_IMS_CALL_MERGE_FAILED:
                return "RIL_REQUEST_SET_POL_ENTRY";
            case 2025:
                return "RIL_REQUEST_QUERY_UPB_CAPABILITY";
            case 2026:
                return "RIL_REQUEST_EDIT_UPB_ENTRY";
            case 2027:
                return "RIL_REQUEST_DELETE_UPB_ENTRY";
            case TelephonyEventLog.TAG_IMS_CONFERENCE_PARTICIPANTS_STATE_CHANGED:
                return "RIL_REQUEST_READ_UPB_GAS_LIST";
            case TelephonyEventLog.TAG_IMS_MULTIPARTY_STATE_CHANGED:
                return "RIL_REQUEST_READ_UPB_GRP";
            case TelephonyEventLog.TAG_IMS_CALL_STATE:
                return "RIL_REQUEST_WRITE_UPB_GRP";
            case 2031:
                return "RIL_REQUEST_SET_TRM";
            case 2033:
                return "RIL_REQUEST_GET_PHB_STRING_LENGTH";
            case 2034:
                return "RIL_REQUEST_GET_PHB_MEM_STORAGE";
            case 2035:
                return "RIL_REQUEST_SET_PHB_MEM_STORAGE";
            case 2036:
                return "RIL_REQUEST_READ_PHB_ENTRY_EXT";
            case 2037:
                return "RIL_REQUEST_WRITE_PHB_ENTRY_EXT";
            case 2038:
                return "RIL_REQUEST_GET_SMS_PARAMS";
            case 2039:
                return "RIL_REQUEST_SET_SMS_PARAMS";
            case 2042:
                return "SIM_GET_ATR";
            case 2043:
                return "RIL_REQUEST_SET_CB_CHANNEL_CONFIG_INFO";
            case 2044:
                return "RIL_REQUEST_SET_CB_LANGUAGE_CONFIG_INFO";
            case 2045:
                return "RIL_REQUEST_GET_CB_CONFIG_INFO";
            case 2047:
                return "RIL_REQUEST_SET_ETWS";
            case 2048:
                return "RIL_REQUEST_SET_FD_MODE";
            case 2050:
                return "RIL_REQUEST_RESUME_REGISTRATION";
            case CharacterSets.CP864:
                return "RIL_REQUEST_STORE_MODEM_TYPE";
            case 2058:
                return "RIL_REQUEST_STK_EVDL_CALL_BY_AP";
            case 2059:
                return "RIL_REQUEST_GET_FEMTOCELL_LIST";
            case 2060:
                return "RIL_REQUEST_ABORT_FEMTOCELL_LIST";
            case 2061:
                return "RIL_REQUEST_SELECT_FEMTOCELL";
            case 2062:
                return "ABORT_QUERY_AVAILABLE_NETWORKS";
            case 2063:
                return "HANGUP_ALL";
            case 2064:
                return "FORCE_RELEASE_CALL";
            case 2065:
                return "SET_CALL_INDICATION";
            case 2066:
                return "EMERGENCY_DIAL";
            case 2067:
                return "SET_ECC_SERVICE_CATEGORY";
            case 2068:
                return "SET_ECC_LIST";
            case 2069:
                return "RIL_REQUEST_GENERAL_SIM_AUTH";
            case 2070:
                return "RIL_REQUEST_OPEN_ICC_APPLICATION";
            case 2071:
                return "RIL_REQUEST_GET_ICC_APPLICATION_STATUS";
            case 2072:
                return "SIM_IO_EX";
            case 2073:
                return "RIL_REQUEST_SET_IMS_ENABLE";
            case 2074:
                return "QUERY_AVAILABLE_NETWORKS_WITH_ACT";
            case 2075:
                return "SEND_CNAP";
            case 2076:
                return "RIL_REQUEST_SET_CLIP";
            case 2083:
                return "RIL_REQUEST_REMOVE_CB_MESSAGE";
            case CharacterSets.KOI8_R:
                return "RIL_REQUEST_SET_DATA_CENTRIC";
            case CharacterSets.HZ_GB_2312:
                return "RIL_REQUEST_ADD_IMS_CONFERENCE_CALL_MEMBER";
            case 2086:
                return "RIL_REQUEST_REMOVE_IMS_CONFERENCE_CALL_MEMBER";
            case 2087:
                return "RIL_REQUEST_DIAL_WITH_SIP_URI";
            case CharacterSets.KOI8_U:
                return "RIL_REQUEST_RESUNME_CALL";
            case 2089:
                return "SET_SPEECH_CODEC_INFO";
            case 2090:
                return "RIL_REQUEST_SET_DATA_ON_TO_MD";
            case 2091:
                return "RIL_REQUEST_SET_REMOVE_RESTRICT_EUTRAN_MODE";
            case 2092:
                return "RIL_REQUEST_SET_IMS_CALL_STATUS";
            case 2093:
                return "RIL_REQUEST_VT_DIAL";
            case 2094:
                return "VOICE_ACCEPT";
            case 2095:
                return "RIL_REQUEST_REPLACE_VT_CALL";
            case 2100:
                return "RIL_REQUEST_CONFERENCE_DIAL";
            case CharacterSets.BIG5_HKSCS:
                return "RIL_REQUEST_SET_SRVCC_CALL_CONTEXT_TRANSFER";
            case 2102:
                return "RIL_REQUEST_UPDATE_IMS_REGISTRATION_STATUS";
            case 2103:
                return "RIL_REQUEST_RELOAD_MODEM_TYPE";
            case 2104:
                return "RIL_REQUEST_HOLD_CALL";
            case 2108:
                return "RIL_REQUEST_ENABLE_MD3_SLEEP";
            case 2110:
                return "RIL_REQUEST_SET_LTE_ACCESS_STRATUM_REPORT";
            case 2111:
                return "RIL_REQUEST_SET_LTE_UPLINK_DATA_TRANSFER";
            case 2131:
                return "RIL_REQUEST_VT_DIAL_WITH_SIP_URI";
            case 2134:
                return "RIL_REQUEST_SYNC_APN_TABLE";
            case 2142:
                return "RIL_REQUEST_SWITCH_MODE_FOR_ECC";
            case 2143:
                return "RIL_REQUEST_QUERY_UPB_AVAILABLE";
            case 2144:
                return "RIL_REQUEST_READ_EMAIL_ENTRY";
            case 2145:
                return "RIL_REQUEST_READ_SNE_ENTRY";
            case 2146:
                return "RIL_REQUEST_READ_ANR_ENTRY";
            case 2147:
                return "RIL_REQUEST_READ_UPB_AAS_LIST";
            case 2148:
                return "RIL_REQUEST_SET_STK_UTK_MODE";
            case 2151:
                return "RIL_REQUEST_SET_PS_REGISTRATION";
            case 2152:
                return "RIL_REQUEST_SYNC_DATA_SETTINGS_TO_MD";
            case 2153:
                return "RIL_REQUEST_SET_PSEUDO_BS_ENABLE";
            case 2154:
                return "RIL_REQUEST_GET_PSEUDO_BS_RECORDS";
            case 2155:
                return "RIL_REQUEST_SYNC_APN_TABLE_TO_RDS";
            default:
                return "<unknown request>";
        }
    }

    static String responseToString(int request) {
        switch (request) {
            case 1000:
                return "UNSOL_RESPONSE_RADIO_STATE_CHANGED";
            case 1001:
                return "UNSOL_RESPONSE_CALL_STATE_CHANGED";
            case 1002:
                return "UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED";
            case 1003:
                return "UNSOL_RESPONSE_NEW_SMS";
            case 1004:
                return "UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT";
            case 1005:
                return "UNSOL_RESPONSE_NEW_SMS_ON_SIM";
            case 1006:
                return "UNSOL_ON_USSD";
            case 1007:
                return "UNSOL_ON_USSD_REQUEST";
            case 1008:
                return "UNSOL_NITZ_TIME_RECEIVED";
            case 1009:
                return "UNSOL_SIGNAL_STRENGTH";
            case GsmVTProvider.SESSION_EVENT_START_COUNTER:
                return "UNSOL_DATA_CALL_LIST_CHANGED";
            case 1011:
                return "UNSOL_SUPP_SVC_NOTIFICATION";
            case 1012:
                return "UNSOL_STK_SESSION_END";
            case CharacterSets.UTF_16BE:
                return "UNSOL_STK_PROACTIVE_COMMAND";
            case CharacterSets.UTF_16LE:
                return "UNSOL_STK_EVENT_NOTIFY";
            case CharacterSets.UTF_16:
                return "UNSOL_STK_CALL_SETUP";
            case CharacterSets.CESU_8:
                return "UNSOL_SIM_SMS_STORAGE_FULL";
            case CharacterSets.UTF_32:
                return "UNSOL_SIM_REFRESH";
            case CharacterSets.UTF_32BE:
                return "UNSOL_CALL_RING";
            case CharacterSets.UTF_32LE:
                return "UNSOL_RESPONSE_SIM_STATUS_CHANGED";
            case CharacterSets.BOCU_1:
                return "UNSOL_RESPONSE_CDMA_NEW_SMS";
            case 1021:
                return "UNSOL_RESPONSE_NEW_BROADCAST_SMS";
            case 1022:
                return "UNSOL_CDMA_RUIM_SMS_STORAGE_FULL";
            case 1023:
                return "UNSOL_RESTRICTED_STATE_CHANGED";
            case 1024:
                return "UNSOL_ENTER_EMERGENCY_CALLBACK_MODE";
            case 1025:
                return "UNSOL_CDMA_CALL_WAITING";
            case 1026:
                return "UNSOL_CDMA_OTA_PROVISION_STATUS";
            case 1027:
                return "UNSOL_CDMA_INFO_REC";
            case 1028:
                return "UNSOL_OEM_HOOK_RAW";
            case 1029:
                return "UNSOL_RINGBACK_TONE";
            case 1030:
                return "UNSOL_RESEND_INCALL_MUTE";
            case 1031:
                return "CDMA_SUBSCRIPTION_SOURCE_CHANGED";
            case 1032:
                return "UNSOL_CDMA_PRL_CHANGED";
            case 1033:
                return "UNSOL_EXIT_EMERGENCY_CALLBACK_MODE";
            case 1034:
                return "UNSOL_RIL_CONNECTED";
            case 1035:
                return "UNSOL_VOICE_RADIO_TECH_CHANGED";
            case 1036:
                return "UNSOL_CELL_INFO_LIST";
            case 1037:
                return "UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED";
            case 1038:
                return "RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED";
            case 1039:
                return "UNSOL_SRVCC_STATE_NOTIFY";
            case 1040:
                return "RIL_UNSOL_HARDWARE_CONFIG_CHANGED";
            case 1042:
                return "RIL_UNSOL_RADIO_CAPABILITY";
            case 1043:
                return "UNSOL_ON_SS";
            case 1044:
                return "UNSOL_STK_CC_ALPHA_NOTIFY";
            case 1045:
                return "UNSOL_LCE_INFO_RECV";
            case 3000:
                return "UNSOL_NEIGHBORING_CELL_INFO";
            case 3001:
                return "UNSOL_NETWORK_INFO";
            case 3002:
                return "UNSOL_PHB_READY_NOTIFICATION";
            case 3004:
                return "RIL_UNSOL_ME_SMS_STORAGE_FULL";
            case 3005:
                return "RIL_UNSOL_SMS_READY_NOTIFICATION";
            case 3006:
                return "UNSOL_SIM_MISSING";
            case 3008:
                return "UNSOL_SIM_RECOVERY";
            case 3009:
                return "UNSOL_VIRTUAL_SIM_ON";
            case 3010:
                return "UNSOL_VIRTUAL_SIM_ON_OFF";
            case 3011:
                return "RIL_UNSOL_INVALID_SIM";
            case 3012:
                return "UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED";
            case 3013:
                return "UNSOL_ACMT_INFO";
            case 3015:
                return "UNSOL_IMEI_LOCK";
            case 3016:
                return "UNSOL_RESPONSE_MMRR_STATUS_CHANGED";
            case 3017:
                return "UNSOL_SIM_PLUG_OUT";
            case 3018:
                return "UNSOL_SIM_PLUG_IN";
            case 3019:
                return "RIL_UNSOL_RESPONSE_ETWS_NOTIFICATION";
            case 3020:
                return "RIL_UNSOL_RESPONSE_PLMN_CHANGED";
            case 3021:
                return "RIL_UNSOL_RESPONSE_REGISTRATION_SUSPENDED";
            case 3022:
                return "RIL_UNSOL_STK_EVDL_CALL";
            case 3023:
                return "RIL_UNSOL_FEMTOCELL_INFO";
            case 3024:
                return "RIL_UNSOL_STK_SETUP_MENU_RESET";
            case 3025:
                return "RIL_UNSOL_APPLICATION_SESSION_ID_CHANGED";
            case 3026:
                return "RIL_UNSOL_ECONF_SRVCC_INDICATION";
            case 3027:
                return "RIL_UNSOL_IMS_ENABLE_DONE";
            case 3028:
                return "RIL_UNSOL_IMS_DISABLE_DONE";
            case 3029:
                return "RIL_UNSOL_IMS_REGISTRATION_INFO";
            case 3033:
                return "RIL_UNSOL_ECONF_RESULT_INDICATION";
            case 3034:
                return "RIL_UNSOL_MELOCK_NOTIFICATION";
            case 3035:
                return "UNSOL_CALL_FORWARDING";
            case 3036:
                return "UNSOL_CRSS_NOTIFICATION";
            case 3037:
                return "UNSOL_INCOMING_CALL_INDICATION";
            case 3038:
                return "UNSOL_CIPHER_INDICATION";
            case 3039:
                return "RIL_UNSOL_SIM_COMMON_SLOT_NO_CHANGED";
            case 3040:
                return "RIL_UNSOL_DATA_ALLOWED";
            case 3041:
                return "RIL_UNSOL_STK_CALL_CTRL";
            case 3043:
                return "RIL_UNSOL_CALL_INFO_INDICATION";
            case 3044:
                return "RIL_UNSOL_VOLTE_EPS_NETWORK_FEATURE_INFO";
            case 3045:
                return "RIL_UNSOL_SRVCC_HANDOVER_INFO_INDICATION";
            case 3046:
                return "UNSOL_SPEECH_CODEC_INFO";
            case 3047:
                return "RIL_UNSOL_MD_STATE_CHANGE";
            case 3048:
                return "RIL_UNSOL_REMOVE_RESTRICT_EUTRAN";
            case 3049:
                return "RIL_UNSOL_SSAC_BARRING_INFO";
            case 3052:
                return "RIL_UNSOL_EMERGENCY_BEARER_SUPPORT_NOTIFY";
            case 3054:
                return "RIL_UNSOL_GMSS_RAT_CHANGED";
            case 3058:
                return "RIL_UNSOL_IMSI_REFRESH_DONE";
            case 3059:
                return "UNSOL_EUSIM_READY";
            case 3060:
                return "UNSOL_STK_BIP_PROACTIVE_COMMAND";
            case 3061:
                return "RIL_UNSOL_WORLD_MODE_CHANGED";
            case 3062:
                return "UNSOL_VT_STATUS_INFO";
            case 3063:
                return "UNSOL_VT_RING_INFO";
            case 3065:
                return "RIL_UNSOL_SET_ATTACH_APN";
            case 3068:
                return "UNSOL_TRAY_PLUG_IN";
            case 3071:
                return "RIL_UNSOL_LTE_ACCESS_STRATUM_STATE_CHANGE";
            case 3074:
                return "RIL_UNSOL_CDMA_CALL_ACCEPTED";
            case 3081:
                return "UNSOL_NETWORK_EXIST";
            case 3082:
                return "RIL_UNSOL_MODULATION_INFO";
            case 3083:
                return "UNSOL_NETWORK_EVENT";
            case 3093:
                return "RIL_UNSOL_EMBMS_SESSION_STATUS";
            case 3095:
                return "RIL_UNSOL_PCO_STATUS";
            case 3096:
                return "RIL_UNSOL_DATA_ATTACH_APN_CHANGED";
            case 3098:
                return "RIL_UNSOL_TRIGGER_OTASP";
            case 3099:
                return "UNSOL_CALL_REDIAL_STATE";
            case 3100:
                return "RIL_UNSOL_CDMA_CARD_INITIAL_ESN_OR_MEID";
            case 3103:
                return "RIL_UNSOL_PSEUDO_BS_INFO_LIST";
            default:
                return "<unknown response>";
        }
    }

    private void riljLog(String msg) {
        Rlog.d(RILJ_LOG_TAG, msg + (this.mInstanceId != null ? " [SUB" + this.mInstanceId + "]" : UsimPBMemInfo.STRING_NOT_SET));
    }

    private void riljLogv(String msg) {
        Rlog.v(RILJ_LOG_TAG, msg + (this.mInstanceId != null ? " [SUB" + this.mInstanceId + "]" : UsimPBMemInfo.STRING_NOT_SET));
    }

    private void unsljLog(int response) {
        riljLog("[UNSL]< " + responseToString(response));
    }

    private void unsljLogMore(int response, String more) {
        riljLog("[UNSL]< " + responseToString(response) + " " + more);
    }

    private void unsljLogRet(int response, Object ret) {
        riljLog("[UNSL]< " + responseToString(response) + " " + retToString(response, ret));
    }

    private void unsljLogvRet(int response, Object ret) {
        riljLogv("[UNSL]< " + responseToString(response) + " " + retToString(response, ret));
    }

    private Object responseSsData(Parcel p) {
        SsData ssData = new SsData();
        ssData.serviceType = ssData.ServiceTypeFromRILInt(p.readInt());
        ssData.requestType = ssData.RequestTypeFromRILInt(p.readInt());
        ssData.teleserviceType = ssData.TeleserviceTypeFromRILInt(p.readInt());
        ssData.serviceClass = p.readInt();
        ssData.result = p.readInt();
        int num = p.readInt();
        if (ssData.serviceType.isTypeCF() && ssData.requestType.isTypeInterrogation()) {
            ssData.cfInfo = new CallForwardInfo[num];
            for (int i = 0; i < num; i++) {
                ssData.cfInfo[i] = new CallForwardInfo();
                ssData.cfInfo[i].status = p.readInt();
                ssData.cfInfo[i].reason = p.readInt();
                ssData.cfInfo[i].serviceClass = p.readInt();
                ssData.cfInfo[i].toa = p.readInt();
                ssData.cfInfo[i].number = p.readString();
                ssData.cfInfo[i].timeSeconds = p.readInt();
                riljLog("[SS Data] CF Info " + i + " : " + ssData.cfInfo[i]);
            }
        } else {
            ssData.ssInfo = new int[num];
            for (int i2 = 0; i2 < num; i2++) {
                ssData.ssInfo[i2] = p.readInt();
                riljLog("[SS Data] SS Info " + i2 + " : " + ssData.ssInfo[i2]);
            }
        }
        return ssData;
    }

    @Override
    public void getDeviceIdentity(Message response) {
        RILRequest rr = RILRequest.obtain(98, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getCDMASubscription(Message response) {
        RILRequest rr = RILRequest.obtain(95, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setPhoneType(int phoneType) {
        riljLog("setPhoneType=" + phoneType + " old value=" + this.mPhoneType);
        this.mPhoneType = phoneType;
    }

    @Override
    public void queryCdmaRoamingPreference(Message response) {
        RILRequest rr = RILRequest.obtain(79, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setCdmaRoamingPreference(int cdmaRoamingType, Message response) {
        RILRequest rr = RILRequest.obtain(78, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(cdmaRoamingType);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " : " + cdmaRoamingType);
        send(rr);
    }

    @Override
    public void setCdmaSubscriptionSource(int cdmaSubscription, Message response) {
        RILRequest rr = RILRequest.obtain(77, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(cdmaSubscription);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " : " + cdmaSubscription);
        send(rr);
    }

    @Override
    public void getCdmaSubscriptionSource(Message response) {
        RILRequest rr = RILRequest.obtain(CharacterSets.ISO_2022_CN, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void queryTTYMode(Message response) {
        RILRequest rr = RILRequest.obtain(81, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setTTYMode(int ttyMode, Message response) {
        RILRequest rr = RILRequest.obtain(80, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(ttyMode);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " : " + ttyMode);
        send(rr);
    }

    @Override
    public void sendCDMAFeatureCode(String FeatureCode, Message response) {
        RILRequest rr = RILRequest.obtain(84, response);
        rr.mParcel.writeString(FeatureCode);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " : " + FeatureCode);
        send(rr);
    }

    @Override
    public void getCdmaBroadcastConfig(Message response) {
        RILRequest rr = RILRequest.obtain(92, response);
        send(rr);
    }

    @Override
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs, Message response) {
        RILRequest rr = RILRequest.obtain(93, response);
        ArrayList<CdmaSmsBroadcastConfigInfo> processedConfigs = new ArrayList<>();
        for (CdmaSmsBroadcastConfigInfo config : configs) {
            for (int i = config.getFromServiceCategory(); i <= config.getToServiceCategory(); i++) {
                processedConfigs.add(new CdmaSmsBroadcastConfigInfo(i, i, config.getLanguage(), config.isSelected()));
            }
        }
        CdmaSmsBroadcastConfigInfo[] rilConfigs = (CdmaSmsBroadcastConfigInfo[]) processedConfigs.toArray(configs);
        rr.mParcel.writeInt(rilConfigs.length);
        for (int i2 = 0; i2 < rilConfigs.length; i2++) {
            rr.mParcel.writeInt(rilConfigs[i2].getFromServiceCategory());
            rr.mParcel.writeInt(rilConfigs[i2].getLanguage());
            rr.mParcel.writeInt(rilConfigs[i2].isSelected() ? 1 : 0);
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " with " + rilConfigs.length + " configs : ");
        for (CdmaSmsBroadcastConfigInfo cdmaSmsBroadcastConfigInfo : rilConfigs) {
            riljLog(cdmaSmsBroadcastConfigInfo.toString());
        }
        send(rr);
    }

    @Override
    public void setCdmaBroadcastActivation(boolean activate, Message response) {
        RILRequest rr = RILRequest.obtain(94, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(activate ? 0 : 1);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void exitEmergencyCallbackMode(Message response) {
        RILRequest rr = RILRequest.obtain(99, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void requestIsimAuthentication(String nonce, Message response) {
        RILRequest rr = RILRequest.obtain(CharacterSets.ISO_2022_CN_EXT, response);
        if (SystemProperties.get("ro.mtk_tc1_feature").equals("1")) {
            byte[] result = Base64.decode(nonce, 0);
            StringBuilder mStringBuilder = new StringBuilder(result.length * 2);
            for (byte mByte : result) {
                mStringBuilder.append(String.format("%02x", Integer.valueOf(mByte & PplMessageManager.Type.INVALID)));
            }
            nonce = mStringBuilder.toString();
            riljLog("requestIsimAuthentication - nonce = " + nonce);
        }
        rr.mParcel.writeString(nonce);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void requestIccSimAuthentication(int authContext, String data, String aid, Message response) {
        RILRequest rr = RILRequest.obtain(125, response);
        rr.mParcel.writeInt(authContext);
        rr.mParcel.writeString(data);
        rr.mParcel.writeString(aid);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getCellInfoList(Message result) {
        RILRequest rr = RILRequest.obtain(CharacterSets.ISO_8859_13, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setCellInfoListRate(int rateInMillis, Message response) {
        riljLog("setCellInfoListRate: " + rateInMillis);
        RILRequest rr = RILRequest.obtain(CharacterSets.ISO_8859_14, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(rateInMillis);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setInitialAttachApn(String apn, String protocol, int authType, String username, String password, Message result) {
        IaExtendParam param = new IaExtendParam();
        setInitialAttachApn(apn, protocol, authType, username, password, param, result);
    }

    @Override
    public void setInitialAttachApn(String apn, String protocol, int authType, String username, String password, Object obj, Message result) {
        RILRequest rr = RILRequest.obtain(111, result);
        riljLog("Set RIL_REQUEST_SET_INITIAL_ATTACH_APN");
        rr.mParcel.writeString(apn);
        rr.mParcel.writeString(protocol);
        IaExtendParam param = (IaExtendParam) obj;
        rr.mParcel.writeString(param.mRoamingProtocol);
        rr.mParcel.writeInt(authType);
        rr.mParcel.writeString(username);
        rr.mParcel.writeString(password);
        rr.mParcel.writeString(param.mOperatorNumeric);
        rr.mParcel.writeInt(param.mCanHandleIms ? 1 : 0);
        rr.mParcel.writeStringArray(param.mDualApnPlmnList);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ", apn:" + apn + ", protocol:" + protocol + ", authType:" + authType + ", username:" + username + ", password:" + password + " ," + param);
        send(rr);
    }

    @Override
    public void setDataProfile(DataProfile[] dps, Message result) {
        riljLog("Set RIL_REQUEST_SET_DATA_PROFILE");
        RILRequest rr = RILRequest.obtain(128, null);
        DataProfile.toParcel(rr.mParcel, dps);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " with " + dps + " Data Profiles : ");
        for (DataProfile dataProfile : dps) {
            riljLog(dataProfile.toString());
        }
        send(rr);
    }

    @Override
    public void testingEmergencyCall() {
        riljLog("testingEmergencyCall");
        this.mTestingEmergencyCall.set(true);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("RIL: " + this);
        pw.println(" mSocket=" + this.mSocket);
        pw.println(" mSenderThread=" + this.mSenderThread);
        pw.println(" mSender=" + this.mSender);
        pw.println(" mReceiverThread=" + this.mReceiverThread);
        pw.println(" mReceiver=" + this.mReceiver);
        pw.println(" mWakeLock=" + this.mWakeLock);
        pw.println(" mWakeLockTimeout=" + this.mWakeLockTimeout);
        synchronized (this.mRequestList) {
            synchronized (this.mWakeLock) {
                pw.println(" mWakeLockCount=" + this.mWakeLockCount);
            }
            int count = this.mRequestList.size();
            pw.println(" mRequestList count=" + count);
            for (int i = 0; i < count; i++) {
                RILRequest rr = this.mRequestList.valueAt(i);
                pw.println("  [" + rr.mSerial + "] " + requestToString(rr.mRequest));
            }
        }
        pw.println(" mLastNITZTimeInfo=" + Arrays.toString(this.mLastNITZTimeInfo));
        pw.println(" mTestingEmergencyCall=" + this.mTestingEmergencyCall.get());
    }

    @Override
    public void iccOpenLogicalChannel(String AID, Message response) {
        RILRequest rr = RILRequest.obtain(115, response);
        rr.mParcel.writeString(AID);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void iccCloseLogicalChannel(int channel, Message response) {
        RILRequest rr = RILRequest.obtain(116, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(channel);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void iccTransmitApduLogicalChannel(int channel, int cla, int instruction, int p1, int p2, int p3, String data, Message response) {
        if (channel <= 0) {
            throw new RuntimeException("Invalid channel in iccTransmitApduLogicalChannel: " + channel);
        }
        iccTransmitApduHelper(117, channel, cla, instruction, p1, p2, p3, data, response);
    }

    @Override
    public void iccTransmitApduBasicChannel(int cla, int instruction, int p1, int p2, int p3, String data, Message response) {
        iccTransmitApduHelper(CharacterSets.GB18030, 0, cla, instruction, p1, p2, p3, data, response);
    }

    private void iccTransmitApduHelper(int rilCommand, int channel, int cla, int instruction, int p1, int p2, int p3, String data, Message response) {
        RILRequest rr = RILRequest.obtain(rilCommand, response);
        rr.mParcel.writeInt(channel);
        rr.mParcel.writeInt(cla);
        rr.mParcel.writeInt(instruction);
        rr.mParcel.writeInt(p1);
        rr.mParcel.writeInt(p2);
        rr.mParcel.writeInt(p3);
        rr.mParcel.writeString(data);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void nvReadItem(int itemID, Message response) {
        RILRequest rr = RILRequest.obtain(118, response);
        rr.mParcel.writeInt(itemID);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ' ' + itemID);
        send(rr);
    }

    @Override
    public void nvWriteItem(int itemID, String itemValue, Message response) {
        RILRequest rr = RILRequest.obtain(119, response);
        rr.mParcel.writeInt(itemID);
        rr.mParcel.writeString(itemValue);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ' ' + itemID + ": " + itemValue);
        send(rr);
    }

    @Override
    public void nvWriteCdmaPrl(byte[] preferredRoamingList, Message response) {
        RILRequest rr = RILRequest.obtain(120, response);
        rr.mParcel.writeByteArray(preferredRoamingList);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " (" + preferredRoamingList.length + " bytes)");
        send(rr);
    }

    @Override
    public void nvResetConfig(int resetType, Message response) {
        RILRequest rr = RILRequest.obtain(121, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(resetType);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ' ' + resetType);
        send(rr);
    }

    @Override
    public void setRadioCapability(RadioCapability rc, Message response) {
        RILRequest rr = RILRequest.obtain(131, response);
        rr.mParcel.writeInt(rc.getVersion());
        rr.mParcel.writeInt(rc.getSession());
        rr.mParcel.writeInt(rc.getPhase());
        rr.mParcel.writeInt(rc.getRadioAccessFamily());
        rr.mParcel.writeString(rc.getLogicalModemUuid());
        rr.mParcel.writeInt(rc.getStatus());
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + rc.toString());
        send(rr);
    }

    @Override
    public void getRadioCapability(Message response) {
        RILRequest rr = RILRequest.obtain(130, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void startLceService(int reportIntervalMs, boolean pullMode, Message response) {
        RILRequest rr = RILRequest.obtain(132, response);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(reportIntervalMs);
        rr.mParcel.writeInt(pullMode ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void stopLceService(Message response) {
        RILRequest rr = RILRequest.obtain(133, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void pullLceData(Message response) {
        RILRequest rr = RILRequest.obtain(134, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getModemActivityInfo(Message response) {
        RILRequest rr = RILRequest.obtain(135, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setTrm(int mode, Message result) {
        RILRequest rr = RILRequest.obtain(2031, null);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(mode);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setStkEvdlCallByAP(int enabled, Message response) {
        RILRequest rr = RILRequest.obtain(2058, response);
        riljLog(rr.serialString() + ">>> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(enabled);
        send(rr);
    }

    @Override
    public void hangupAll(Message result) {
        RILRequest rr = RILRequest.obtain(2063, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void forceReleaseCall(int index, Message result) {
        RILRequest rr = RILRequest.obtain(2064, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(index);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + index);
        send(rr);
    }

    @Override
    public void setCallIndication(int mode, int callId, int seqNumber, Message result) {
        RILRequest rr = RILRequest.obtain(2065, result);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeInt(mode);
        rr.mParcel.writeInt(callId);
        rr.mParcel.writeInt(seqNumber);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + mode + ", " + callId + ", " + seqNumber);
        send(rr);
    }

    @Override
    public void emergencyDial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        RILRequest rr = RILRequest.obtain(2066, result);
        rr.mParcel.writeString(address);
        rr.mParcel.writeInt(clirMode);
        if (uusInfo == null) {
            rr.mParcel.writeInt(0);
        } else {
            rr.mParcel.writeInt(1);
            rr.mParcel.writeInt(uusInfo.getType());
            rr.mParcel.writeInt(uusInfo.getDcs());
            rr.mParcel.writeByteArray(uusInfo.getUserData());
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setEccServiceCategory(int serviceCategory) {
        RILRequest rr = RILRequest.obtain(2067, null);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(serviceCategory);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + serviceCategory);
        send(rr);
    }

    private void setEccList() {
        RILRequest rr = RILRequest.obtain(2068, null);
        ArrayList<PhoneNumberUtils.EccEntry> eccList = PhoneNumberUtils.getEccList();
        rr.mParcel.writeInt(eccList.size() * 3);
        for (PhoneNumberUtils.EccEntry entry : eccList) {
            rr.mParcel.writeString(entry.getEcc());
            rr.mParcel.writeString(entry.getCategory());
            String strCondition = entry.getCondition();
            if (strCondition.equals(Phone.ACT_TYPE_UTRAN) || !TextUtils.isEmpty(entry.getPlmn())) {
                strCondition = "0";
            }
            rr.mParcel.writeString(strCondition);
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setSpeechCodecInfo(boolean enable, Message response) {
        RILRequest rr = RILRequest.obtain(2089, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(enable ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + enable);
        send(rr);
    }

    @Override
    public void vtDial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        if (!PhoneNumberUtils.isUriNumber(address)) {
            RILRequest rr = RILRequest.obtain(2093, result);
            rr.mParcel.writeString(address);
            rr.mParcel.writeInt(clirMode);
            if (uusInfo == null) {
                rr.mParcel.writeInt(0);
            } else {
                rr.mParcel.writeInt(1);
                rr.mParcel.writeInt(uusInfo.getType());
                rr.mParcel.writeInt(uusInfo.getDcs());
                rr.mParcel.writeByteArray(uusInfo.getUserData());
            }
            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            send(rr);
            return;
        }
        RILRequest rr2 = RILRequest.obtain(2131, result);
        rr2.mParcel.writeString(address);
        riljLog(rr2.serialString() + "> " + requestToString(rr2.mRequest));
        send(rr2);
    }

    @Override
    public void acceptVtCallWithVoiceOnly(int callId, Message result) {
        RILRequest rr = RILRequest.obtain(2094, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + callId);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(callId);
        send(rr);
    }

    @Override
    public void replaceVtCall(int index, Message result) {
        RILRequest rr = RILRequest.obtain(2095, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(index);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setDataOnToMD(boolean enable, Message result) {
        RILRequest rr = RILRequest.obtain(2090, result);
        int type = enable ? 1 : 0;
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(type);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + type);
        send(rr);
    }

    @Override
    public void setRemoveRestrictEutranMode(boolean enable, Message result) {
        RILRequest rr = RILRequest.obtain(2091, result);
        int type = enable ? 1 : 0;
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(type);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + type);
        send(rr);
    }

    @Override
    public void syncApnTable(String index, String apnClass, String apn, String apnType, String apnBearer, String apnEnable, String apnTime, String maxConn, String maxConnTime, String waitTime, String throttlingTime, String inactiveTimer, Message result) {
        riljLog("syncApnTable:wapn = " + index);
        RILRequest rr = RILRequest.obtain(2134, result);
        rr.mParcel.writeInt(12);
        rr.mParcel.writeString(index);
        rr.mParcel.writeString(apnClass);
        rr.mParcel.writeString(apn);
        rr.mParcel.writeString(apnType);
        rr.mParcel.writeString(apnBearer);
        rr.mParcel.writeString(apnEnable);
        rr.mParcel.writeString(apnTime);
        rr.mParcel.writeString(maxConn);
        rr.mParcel.writeString(maxConnTime);
        rr.mParcel.writeString(waitTime);
        rr.mParcel.writeString(throttlingTime);
        rr.mParcel.writeString(inactiveTimer);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + index + " " + apnClass + " " + apn + " " + apnType + " " + apnEnable + " " + apnTime + " " + maxConn + " " + maxConnTime + " " + waitTime + " " + throttlingTime + " " + inactiveTimer);
        send(rr);
    }

    @Override
    public void syncDataSettingsToMd(boolean dataSetting, boolean dataRoamingSetting, Message result) {
        RILRequest rr = RILRequest.obtain(2152, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(dataSetting ? 1 : 0);
        rr.mParcel.writeInt(dataRoamingSetting ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + dataSetting + " " + dataRoamingSetting);
        send(rr);
    }

    @Override
    public void openIccApplication(int application, Message response) {
        RILRequest rr = RILRequest.obtain(2070, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(application);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ", application = " + application);
        send(rr);
    }

    @Override
    public void getIccApplicationStatus(int sessionId, Message result) {
        RILRequest rr = RILRequest.obtain(2071, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(sessionId);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ", session = " + sessionId);
        send(rr);
    }

    @Override
    public void queryNetworkLock(int category, Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_HOLD_FAILED, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        riljLog("queryNetworkLock:" + category);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(category);
        send(rr);
    }

    @Override
    public void setNetworkLock(int catagory, int lockop, String password, String data_imsi, String gid1, String gid2, Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_HOLD_RECEIVED, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        riljLog("setNetworkLock:" + catagory + ", " + lockop + ", " + password + ", " + data_imsi + ", " + gid1 + ", " + gid2);
        rr.mParcel.writeInt(6);
        rr.mParcel.writeString(Integer.toString(catagory));
        rr.mParcel.writeString(Integer.toString(lockop));
        if (password != null) {
            rr.mParcel.writeString(password);
        } else {
            rr.mParcel.writeString(UsimPBMemInfo.STRING_NOT_SET);
        }
        rr.mParcel.writeString(data_imsi);
        rr.mParcel.writeString(gid1);
        rr.mParcel.writeString(gid2);
        send(rr);
    }

    @Override
    public void doGeneralSimAuthentication(int sessionId, int mode, int tag, String param1, String param2, Message response) {
        RILRequest rr = RILRequest.obtain(2069, response);
        rr.mParcel.writeInt(sessionId);
        rr.mParcel.writeInt(mode);
        if (param1 == null || param1.length() <= 0) {
            rr.mParcel.writeString(param1);
        } else {
            String length = Integer.toHexString(param1.length() / 2);
            rr.mParcel.writeString(sessionId == 0 ? param1 : ((length.length() % 2 == 1 ? "0" : UsimPBMemInfo.STRING_NOT_SET) + length) + param1);
        }
        if (param2 == null || param2.length() <= 0) {
            rr.mParcel.writeString(param2);
        } else {
            String length2 = Integer.toHexString(param2.length() / 2);
            rr.mParcel.writeString(sessionId == 0 ? param2 : ((length2.length() % 2 == 1 ? "0" : UsimPBMemInfo.STRING_NOT_SET) + length2) + param2);
        }
        if (mode == 1) {
            rr.mParcel.writeInt(tag);
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": session = " + sessionId + ",mode = " + mode + ",tag = " + tag + ", " + param1 + ", " + param2);
        send(rr);
    }

    @Override
    public void iccGetATR(Message result) {
        RILRequest rr = RILRequest.obtain(2042, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setSimPower(int mode, Message result) {
        RILRequest rr = RILRequest.obtain(2133, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(mode);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + mode);
        send(rr);
    }

    @Override
    public void queryPhbStorageInfo(int type, Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_STARTED, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(type);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + type);
        send(rr);
    }

    @Override
    public void writePhbEntry(PhbEntry entry, Message result) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_START_FAILED, result);
        rr.mParcel.writeInt(entry.type);
        rr.mParcel.writeInt(entry.index);
        rr.mParcel.writeString(entry.number);
        rr.mParcel.writeInt(entry.ton);
        rr.mParcel.writeString(entry.alphaId);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + entry);
        send(rr);
    }

    @Override
    public void ReadPhbEntry(int type, int bIndex, int eIndex, Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_TERMINATED, response);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeInt(type);
        rr.mParcel.writeInt(bIndex);
        rr.mParcel.writeInt(eIndex);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + type + " begin: " + bIndex + " end: " + eIndex);
        send(rr);
    }

    private Object responsePhbEntries(Parcel p) {
        int numerOfEntries = p.readInt();
        PhbEntry[] response = new PhbEntry[numerOfEntries];
        Rlog.d(RILJ_LOG_TAG, "Number: " + numerOfEntries);
        for (int i = 0; i < numerOfEntries; i++) {
            response[i] = new PhbEntry();
            response[i].type = p.readInt();
            response[i].index = p.readInt();
            response[i].number = p.readString();
            response[i].ton = p.readInt();
            response[i].alphaId = p.readString();
        }
        return response;
    }

    @Override
    public void queryUPBCapability(Message response) {
        RILRequest rr = RILRequest.obtain(2025, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void editUPBEntry(int entryType, int adnIndex, int entryIndex, String strVal, String tonForNum, String aasAnrIndex, Message response) {
        RILRequest rr = RILRequest.obtain(2026, response);
        if (entryType == 0) {
            rr.mParcel.writeInt(6);
        } else {
            rr.mParcel.writeInt(4);
        }
        rr.mParcel.writeString(Integer.toString(entryType));
        rr.mParcel.writeString(Integer.toString(adnIndex));
        rr.mParcel.writeString(Integer.toString(entryIndex));
        rr.mParcel.writeString(strVal);
        if (entryType == 0) {
            rr.mParcel.writeString(tonForNum);
            rr.mParcel.writeString(aasAnrIndex);
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void editUPBEntry(int entryType, int adnIndex, int entryIndex, String strVal, String tonForNum, Message response) {
        editUPBEntry(entryType, adnIndex, entryIndex, strVal, tonForNum, null, response);
    }

    @Override
    public void deleteUPBEntry(int entryType, int adnIndex, int entryIndex, Message response) {
        RILRequest rr = RILRequest.obtain(2027, response);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeInt(entryType);
        rr.mParcel.writeInt(adnIndex);
        rr.mParcel.writeInt(entryIndex);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void readUPBGasList(int startIndex, int endIndex, Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CONFERENCE_PARTICIPANTS_STATE_CHANGED, response);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(startIndex);
        rr.mParcel.writeInt(endIndex);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void readUPBGrpEntry(int adnIndex, Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_MULTIPARTY_STATE_CHANGED, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(adnIndex);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void writeUPBGrpEntry(int adnIndex, int[] grpIds, Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_STATE, response);
        int nLen = grpIds.length;
        rr.mParcel.writeInt(nLen + 1);
        rr.mParcel.writeInt(adnIndex);
        for (int i : grpIds) {
            rr.mParcel.writeInt(i);
        }
        riljLog("writeUPBGrpEntry nLen is " + nLen);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void queryUPBAvailable(int eftype, int fileIndex, Message response) {
        RILRequest rr = RILRequest.obtain(2143, response);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(eftype);
        rr.mParcel.writeInt(fileIndex);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void readUPBEmailEntry(int adnIndex, int fileIndex, Message response) {
        RILRequest rr = RILRequest.obtain(2144, response);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(adnIndex);
        rr.mParcel.writeInt(fileIndex);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void readUPBSneEntry(int adnIndex, int fileIndex, Message response) {
        RILRequest rr = RILRequest.obtain(2145, response);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(adnIndex);
        rr.mParcel.writeInt(fileIndex);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void readUPBAnrEntry(int adnIndex, int fileIndex, Message response) {
        RILRequest rr = RILRequest.obtain(2146, response);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(adnIndex);
        rr.mParcel.writeInt(fileIndex);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void readUPBAasList(int startIndex, int endIndex, Message response) {
        RILRequest rr = RILRequest.obtain(2147, response);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(startIndex);
        rr.mParcel.writeInt(endIndex);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    private Object responseGetPhbMemStorage(Parcel p) {
        PBMemStorage response = PBMemStorage.createFromParcel(p);
        riljLog("responseGetPhbMemStorage:" + response);
        return response;
    }

    private Object responseReadPhbEntryExt(Parcel p) {
        int numerOfEntries = p.readInt();
        PBEntry[] response = new PBEntry[numerOfEntries];
        Rlog.d(RILJ_LOG_TAG, "responseReadPhbEntryExt Number: " + numerOfEntries);
        for (int i = 0; i < numerOfEntries; i++) {
            response[i] = new PBEntry();
            response[i].setIndex1(p.readInt());
            response[i].setNumber(p.readString());
            response[i].setType(p.readInt());
            response[i].setText(getAdnRecordFromPBEntry(p.readString()));
            response[i].setHidden(p.readInt());
            response[i].setGroup(p.readString());
            response[i].setAdnumber(p.readString());
            response[i].setAdtype(p.readInt());
            response[i].setSecondtext(p.readString());
            response[i].setEmail(getEmailRecordFromPBEntry(p.readString()));
            Rlog.d(RILJ_LOG_TAG, "responseReadPhbEntryExt[" + i + "] " + response[i].toString());
        }
        return response;
    }

    public static String convertKSC5601(String input) {
        String strKSC;
        Rlog.d(RILJ_LOG_TAG, "convertKSC5601");
        try {
            byte[] inData = IccUtils.hexStringToBytes(input.substring(4));
            if (inData == null || (strKSC = new String(inData, "KSC5601")) == null) {
                return UsimPBMemInfo.STRING_NOT_SET;
            }
            int ucslen = strKSC.length();
            while (ucslen > 0 && strKSC.charAt(ucslen - 1) == 63735) {
                ucslen--;
            }
            String output = strKSC.substring(0, ucslen);
            return output;
        } catch (UnsupportedEncodingException ex) {
            Rlog.d(RILJ_LOG_TAG, "Implausible UnsupportedEncodingException : " + ex);
            return UsimPBMemInfo.STRING_NOT_SET;
        }
    }

    public static String getEmailRecordFromPBEntry(String text) {
        String email;
        if (text == null) {
            return null;
        }
        if (text.trim().length() > 2 && text.startsWith("FEFE")) {
            email = convertKSC5601(text);
        } else {
            email = text;
        }
        Rlog.d(RILJ_LOG_TAG, "getEmailRecordFromPBEntry - email = " + email);
        return email;
    }

    public static String getAdnRecordFromPBEntry(String text) {
        if (text == null) {
            return null;
        }
        String alphaId = UsimPBMemInfo.STRING_NOT_SET;
        if (text.trim().length() > 2 && text.startsWith("FEFE")) {
            alphaId = convertKSC5601(text);
        } else {
            Rlog.d(RILJ_LOG_TAG, "getRecordFromPBEntry - Not KSC5601 Data");
            try {
                byte[] ba = IccUtils.hexStringToBytes(text);
                if (ba == null) {
                    return null;
                }
                String alphaId2 = new String(ba, 0, text.length() / 2, "utf-16be");
                alphaId = alphaId2;
            } catch (UnsupportedEncodingException ex) {
                Rlog.d(RILJ_LOG_TAG, "Implausible UnsupportedEncodingException : " + ex);
            }
        }
        Rlog.d(RILJ_LOG_TAG, "getRecordFromPBEntry - alphaId = " + alphaId);
        return alphaId;
    }

    @Override
    public void getPhoneBookStringsLength(Message result) {
        RILRequest rr = RILRequest.obtain(2033, result);
        riljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getPhoneBookMemStorage(Message result) {
        RILRequest rr = RILRequest.obtain(2034, result);
        riljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setPhoneBookMemStorage(String storage, String password, Message result) {
        RILRequest rr = RILRequest.obtain(2035, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeString(storage);
        rr.mParcel.writeString(password);
        riljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void readPhoneBookEntryExt(int index1, int index2, Message result) {
        RILRequest rr = RILRequest.obtain(2036, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(index1);
        rr.mParcel.writeInt(index2);
        riljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void writePhoneBookEntryExt(PBEntry entry, Message result) {
        RILRequest rr = RILRequest.obtain(2037, result);
        rr.mParcel.writeInt(entry.getIndex1());
        rr.mParcel.writeString(entry.getNumber());
        rr.mParcel.writeInt(entry.getType());
        rr.mParcel.writeString(entry.getText());
        rr.mParcel.writeInt(entry.getHidden());
        rr.mParcel.writeString(entry.getGroup());
        rr.mParcel.writeString(entry.getAdnumber());
        rr.mParcel.writeInt(entry.getAdtype());
        rr.mParcel.writeString(entry.getSecondtext());
        rr.mParcel.writeString(entry.getEmail());
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + entry);
        send(rr);
    }

    @Override
    public void getSmsParameters(Message response) {
        RILRequest rr = RILRequest.obtain(2038, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    private Object responseSmsParams(Parcel p) {
        int format = p.readInt();
        int vp = p.readInt();
        int pid = p.readInt();
        int dcs = p.readInt();
        return new SmsParameters(format, vp, pid, dcs);
    }

    @Override
    public void setSmsParameters(SmsParameters params, Message response) {
        RILRequest rr = RILRequest.obtain(2039, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(4);
        rr.mParcel.writeInt(params.format);
        rr.mParcel.writeInt(params.vp);
        rr.mParcel.writeInt(params.pid);
        rr.mParcel.writeInt(params.dcs);
        send(rr);
    }

    @Override
    public void getSmsSimMemoryStatus(Message result) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_UPDATED, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    private Object responseSimSmsMemoryStatus(Parcel p) {
        IccSmsStorageStatus response = new IccSmsStorageStatus();
        response.mUsed = p.readInt();
        response.mTotal = p.readInt();
        return response;
    }

    @Override
    public void setEtws(int mode, Message result) {
        RILRequest rr = RILRequest.obtain(2047, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(mode);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + mode);
        send(rr);
    }

    @Override
    public void setCellBroadcastChannelConfigInfo(String config, int cb_set_type, Message response) {
        RILRequest rr = RILRequest.obtain(2043, response);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeString(config);
        rr.mParcel.writeString(Integer.toString(cb_set_type));
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setCellBroadcastLanguageConfigInfo(String config, Message response) {
        RILRequest rr = RILRequest.obtain(2044, response);
        rr.mParcel.writeString(config);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void queryCellBroadcastConfigInfo(Message response) {
        RILRequest rr = RILRequest.obtain(2045, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    private Object responseCbConfig(Parcel p) {
        int mode = p.readInt();
        String channels = p.readString();
        String languages = p.readString();
        boolean allOn = p.readInt() == 1 ? true : RILJ_LOGV;
        return new CellBroadcastConfigInfo(mode, channels, languages, allOn);
    }

    @Override
    public void removeCellBroadcastMsg(int channelId, int serialId, Message response) {
        RILRequest rr = RILRequest.obtain(2083, response);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(channelId);
        rr.mParcel.writeInt(serialId);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + channelId + ", " + serialId);
        send(rr);
    }

    private Object responseEtwsNotification(Parcel p) {
        EtwsNotification response = new EtwsNotification();
        response.warningType = p.readInt();
        response.messageId = p.readInt();
        response.serialNumber = p.readInt();
        response.plmnId = p.readString();
        response.securityInfo = p.readString();
        return response;
    }

    @Override
    public void storeModemType(int modemType, Message response) {
        RILRequest rr = RILRequest.obtain(CharacterSets.CP864, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(modemType);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void reloadModemType(int modemType, Message response) {
        RILRequest rr = RILRequest.obtain(2103, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(modemType);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    private Object responseCrssNotification(Parcel p) {
        SuppCrssNotification notification = new SuppCrssNotification();
        notification.code = p.readInt();
        notification.type = p.readInt();
        notification.number = p.readString();
        notification.alphaid = p.readString();
        notification.cli_validity = p.readInt();
        return notification;
    }

    private void handleChldRelatedRequest(RILRequest rr) {
        int j;
        synchronized (this.mDtmfReqQueue) {
            int queueSize = this.mDtmfReqQueue.size();
            if (queueSize > 0) {
                RILRequest rr2 = this.mDtmfReqQueue.get();
                if (rr2.mRequest == 49) {
                    riljLog("DTMF queue isn't 0, first request is START, send stop dtmf and pending switch");
                    if (queueSize > 1) {
                        j = 2;
                    } else {
                        j = 1;
                    }
                    riljLog("queue size  " + this.mDtmfReqQueue.size());
                    for (int i = queueSize - 1; i >= j; i--) {
                        this.mDtmfReqQueue.remove(i);
                    }
                    riljLog("queue size  after " + this.mDtmfReqQueue.size());
                    if (this.mDtmfReqQueue.size() == 1) {
                        RILRequest rr3 = RILRequest.obtain(50, null);
                        riljLog("add dummy stop dtmf request");
                        this.mDtmfReqQueue.stop();
                        this.mDtmfReqQueue.add(rr3);
                    }
                } else {
                    riljLog("DTMF queue isn't 0, first request is STOP, penging switch");
                    for (int i2 = queueSize - 1; i2 >= 1; i2--) {
                        this.mDtmfReqQueue.remove(i2);
                    }
                }
                if (this.mDtmfReqQueue.getPendingRequest() != null) {
                    RILRequest pendingRequest = this.mDtmfReqQueue.getPendingRequest();
                    if (pendingRequest.mResult != null) {
                        AsyncResult.forMessage(pendingRequest.mResult, (Object) null, (Throwable) null);
                        pendingRequest.mResult.sendToTarget();
                    }
                }
                this.mDtmfReqQueue.setPendingRequest(rr);
            } else {
                riljLog("DTMF queue is 0, send switch Immediately");
                this.mDtmfReqQueue.setSendChldRequest();
                send(rr);
            }
        }
    }

    @Override
    public void conferenceDial(String[] participants, int clirMode, boolean isVideoCall, Message result) {
        RILRequest rr = RILRequest.obtain(2100, result);
        int numberOfParticipants = participants.length;
        int numberOfStrings = numberOfParticipants + 2 + 1;
        List<String> participantList = Arrays.asList(participants);
        Rlog.d(RILJ_LOG_TAG, "conferenceDial: numberOfParticipants " + numberOfParticipants + "numberOfStrings:" + numberOfStrings);
        rr.mParcel.writeInt(numberOfStrings);
        if (isVideoCall) {
            rr.mParcel.writeString(Integer.toString(1));
        } else {
            rr.mParcel.writeString(Integer.toString(0));
        }
        rr.mParcel.writeString(Integer.toString(numberOfParticipants));
        for (String dialNumber : participantList) {
            rr.mParcel.writeString(dialNumber);
            Rlog.d(RILJ_LOG_TAG, "conferenceDial: dialnumber " + dialNumber);
        }
        rr.mParcel.writeString(Integer.toString(clirMode));
        Rlog.d(RILJ_LOG_TAG, "conferenceDial: clirMode " + clirMode);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void addConferenceMember(int confCallId, String address, int callIdToAdd, Message response) {
        RILRequest rr = RILRequest.obtain(CharacterSets.HZ_GB_2312, response);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(Integer.toString(confCallId));
        rr.mParcel.writeString(address);
        rr.mParcel.writeString(Integer.toString(callIdToAdd));
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void removeConferenceMember(int confCallId, String address, int callIdToRemove, Message response) {
        RILRequest rr = RILRequest.obtain(2086, response);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(Integer.toString(confCallId));
        rr.mParcel.writeString(address);
        rr.mParcel.writeString(Integer.toString(callIdToRemove));
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void resumeCall(int callIdToResume, Message response) {
        RILRequest rr = RILRequest.obtain(CharacterSets.KOI8_U, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(callIdToResume);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void holdCall(int callIdToHold, Message response) {
        RILRequest rr = RILRequest.obtain(2104, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(callIdToHold);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void changeBarringPassword(String facility, String oldPwd, String newPwd, String newCfm, Message result) {
        RILRequest rr = RILRequest.obtain(44, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(4);
        rr.mParcel.writeString(facility);
        rr.mParcel.writeString(oldPwd);
        rr.mParcel.writeString(newPwd);
        rr.mParcel.writeString(newCfm);
        send(rr);
    }

    @Override
    public void setCLIP(boolean enable, Message result) {
        RILRequest rr = RILRequest.obtain(2076, result);
        rr.mParcel.writeInt(1);
        if (enable) {
            rr.mParcel.writeInt(1);
        } else {
            rr.mParcel.writeInt(0);
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + enable);
        send(rr);
    }

    @Override
    public String lookupOperatorNameFromNetwork(long subId, String numeric, boolean desireLongName) {
        String nitzOperatorName;
        String nitzOperatorName2;
        int phoneId = SubscriptionManager.getPhoneId((int) subId);
        String nitzOperatorNumeric = TelephonyManager.getTelephonyProperty(phoneId, "persist.radio.nitz_oper_code", UsimPBMemInfo.STRING_NOT_SET);
        if (numeric == null || !numeric.equals(nitzOperatorNumeric)) {
            nitzOperatorName = null;
        } else if (desireLongName) {
            String nitzOperatorName3 = TelephonyManager.getTelephonyProperty(phoneId, "persist.radio.nitz_oper_lname", UsimPBMemInfo.STRING_NOT_SET);
            nitzOperatorName = nitzOperatorName3;
        } else {
            String nitzOperatorName4 = TelephonyManager.getTelephonyProperty(phoneId, "persist.radio.nitz_oper_sname", UsimPBMemInfo.STRING_NOT_SET);
            nitzOperatorName = nitzOperatorName4;
        }
        if (nitzOperatorName == null || !nitzOperatorName.startsWith("uCs2")) {
            nitzOperatorName2 = nitzOperatorName;
        } else {
            riljLog("lookupOperatorNameFromNetwork handling UCS2 format name");
            try {
                nitzOperatorName2 = new String(IccUtils.hexStringToBytes(nitzOperatorName.substring(4)), "UTF-16");
            } catch (UnsupportedEncodingException e) {
                riljLog("lookupOperatorNameFromNetwork UnsupportedEncodingException");
                nitzOperatorName2 = nitzOperatorName;
            }
        }
        riljLog("lookupOperatorNameFromNetwork numeric= " + numeric + ",subId= " + subId + ",nitzOperatorNumeric= " + nitzOperatorNumeric + ",nitzOperatorName= " + nitzOperatorName2);
        return nitzOperatorName2;
    }

    @Override
    public void setNetworkSelectionModeManualWithAct(String operatorNumeric, String act, Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_RESUMED, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + operatorNumeric + UsimPBMemInfo.STRING_NOT_SET + act);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(operatorNumeric);
        rr.mParcel.writeString(act);
        rr.mParcel.writeString("0");
        send(rr);
    }

    private Object responseNetworkInfoWithActs(Parcel p) {
        String[] strings = (String[]) responseStrings(p);
        if (strings.length % 4 != 0) {
            throw new RuntimeException("RIL_REQUEST_GET_POL_LIST: invalid response. Got " + strings.length + " strings, expected multible of 5");
        }
        ArrayList<NetworkInfoWithAcT> ret = new ArrayList<>(strings.length / 4);
        int nAct = 0;
        int nIndex = 0;
        for (int i = 0; i < strings.length; i += 4) {
            String strOperName = null;
            String strOperNumeric = null;
            if (strings[i] != null) {
                nIndex = Integer.parseInt(strings[i]);
            } else {
                Rlog.d(RILJ_LOG_TAG, "responseNetworkInfoWithActs: no invalid index. i is " + i);
            }
            if (strings[i + 1] != null) {
                int format = Integer.parseInt(strings[i + 1]);
                switch (format) {
                    case 0:
                    case 1:
                        strOperName = strings[i + 2];
                        break;
                    case 2:
                        if (strings[i + 2] != null) {
                            strOperNumeric = strings[i + 2];
                            strOperName = SpnOverride.getInstance().lookupOperatorName(SubscriptionManager.getSubIdUsingPhoneId(this.mInstanceId.intValue()), strings[i + 2], true, this.mContext);
                        }
                        break;
                }
            }
            if (strings[i + 3] != null) {
                nAct = Integer.parseInt(strings[i + 3]);
            } else {
                Rlog.d(RILJ_LOG_TAG, "responseNetworkInfoWithActs: no invalid Act. i is " + i);
            }
            if (strOperNumeric != null && !strOperNumeric.equals("?????")) {
                ret.add(new NetworkInfoWithAcT(strOperName, strOperNumeric, nAct, nIndex));
            } else {
                Rlog.d(RILJ_LOG_TAG, "responseNetworkInfoWithActs: invalid oper. i is " + i);
            }
        }
        return ret;
    }

    @Override
    public void setNetworkSelectionModeSemiAutomatic(String operatorNumeric, String act, Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_RESUMED, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + operatorNumeric + UsimPBMemInfo.STRING_NOT_SET + act);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(operatorNumeric);
        rr.mParcel.writeString(act);
        rr.mParcel.writeString("1");
        send(rr);
    }

    @Override
    public void getPOLCapabilty(Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_UPDATE_FAILED, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getCurrentPOLList(Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_MERGED, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setPOLEntry(int index, String numeric, int nAct, Message response) {
        RILRequest rr = RILRequest.obtain(TelephonyEventLog.TAG_IMS_CALL_MERGE_FAILED, response);
        if (numeric == null || numeric.length() == 0) {
            rr.mParcel.writeInt(1);
            rr.mParcel.writeString(Integer.toString(index));
        } else {
            rr.mParcel.writeInt(3);
            rr.mParcel.writeString(Integer.toString(index));
            rr.mParcel.writeString(numeric);
            rr.mParcel.writeString(Integer.toString(nAct));
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getFemtoCellList(String operatorNumeric, int rat, Message response) {
        RILRequest rr = RILRequest.obtain(2059, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(2);
        rr.mParcel.writeString(operatorNumeric);
        rr.mParcel.writeString(Integer.toString(rat));
        send(rr);
    }

    @Override
    public void abortFemtoCellList(Message response) {
        RILRequest rr = RILRequest.obtain(2060, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void selectFemtoCell(FemtoCellInfo femtocell, Message response) {
        RILRequest rr = RILRequest.obtain(2061, response);
        int act = femtocell.getCsgRat();
        int act2 = act == 14 ? 7 : act == 3 ? 2 : 0;
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " csgId=" + femtocell.getCsgId() + " plmn=" + femtocell.getOperatorNumeric() + " rat=" + femtocell.getCsgRat() + " act=" + act2);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(femtocell.getOperatorNumeric());
        rr.mParcel.writeString(Integer.toString(act2));
        rr.mParcel.writeString(Integer.toString(femtocell.getCsgId()));
        send(rr);
    }

    @Override
    public void setLteAccessStratumReport(boolean enable, Message result) {
        RILRequest rr = RILRequest.obtain(2110, result);
        int type = enable ? 1 : 0;
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(type);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + type);
        send(rr);
    }

    @Override
    public void setLteUplinkDataTransfer(int state, int interfaceId, Message result) {
        RILRequest rr = RILRequest.obtain(2111, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(state);
        rr.mParcel.writeInt(interfaceId);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " state = " + state + ", interfaceId = " + interfaceId);
        send(rr);
    }

    @Override
    public boolean isGettingAvailableNetworks() {
        synchronized (this.mRequestList) {
            int s = this.mRequestList.size();
            for (int i = 0; i < s; i++) {
                RILRequest rr = this.mRequestList.valueAt(i);
                if (rr != null && (rr.mRequest == 48 || rr.mRequest == 2074)) {
                    return true;
                }
            }
            return RILJ_LOGV;
        }
    }

    @Override
    public void setIMSEnabled(boolean enable, Message response) {
        RILRequest rr = RILRequest.obtain(2073, response);
        rr.mParcel.writeInt(1);
        if (enable) {
            rr.mParcel.writeInt(1);
        } else {
            rr.mParcel.writeInt(0);
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setFDMode(int mode, int parameter1, int parameter2, Message response) {
        RILRequest rr = RILRequest.obtain(2048, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        if (mode == 0 || mode == 1) {
            rr.mParcel.writeInt(1);
            rr.mParcel.writeInt(mode);
        } else if (mode == 3) {
            rr.mParcel.writeInt(2);
            rr.mParcel.writeInt(mode);
            rr.mParcel.writeInt(parameter1);
        } else if (mode == 2) {
            rr.mParcel.writeInt(3);
            rr.mParcel.writeInt(mode);
            rr.mParcel.writeInt(parameter1);
            rr.mParcel.writeInt(parameter2);
        }
        send(rr);
    }

    @Override
    public void setDataCentric(boolean enable, Message response) {
        riljLog("setDataCentric");
        RILRequest rr = RILRequest.obtain(CharacterSets.KOI8_R, response);
        rr.mParcel.writeInt(1);
        if (enable) {
            rr.mParcel.writeInt(1);
        } else {
            rr.mParcel.writeInt(0);
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setImsCallStatus(boolean existed, Message response) {
        RILRequest rr = RILRequest.obtain(2092, null);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(existed ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setSrvccCallContextTransfer(int numberOfCall, SrvccCallContext[] callList) {
        RILRequest rr = RILRequest.obtain(CharacterSets.BIG5_HKSCS, null);
        if (numberOfCall <= 0 || callList == null) {
            return;
        }
        rr.mParcel.writeInt((numberOfCall * 9) + 1);
        rr.mParcel.writeString(Integer.toString(numberOfCall));
        for (int i = 0; i < numberOfCall; i++) {
            rr.mParcel.writeString(Integer.toString(callList[i].getCallId()));
            rr.mParcel.writeString(Integer.toString(callList[i].getCallMode()));
            rr.mParcel.writeString(Integer.toString(callList[i].getCallDirection()));
            rr.mParcel.writeString(Integer.toString(callList[i].getCallState()));
            rr.mParcel.writeString(Integer.toString(callList[i].getEccCategory()));
            rr.mParcel.writeString(Integer.toString(callList[i].getNumberType()));
            rr.mParcel.writeString(callList[i].getNumber());
            rr.mParcel.writeString(callList[i].getName());
            rr.mParcel.writeString(Integer.toString(callList[i].getCliValidity()));
        }
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void updateImsRegistrationStatus(int regState, int regType, int reason) {
        RILRequest rr = RILRequest.obtain(2102, null);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeInt(regState);
        rr.mParcel.writeInt(regType);
        rr.mParcel.writeInt(reason);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public int getDisplayState() {
        return this.mDefaultDisplayState;
    }

    @Override
    public void setRegistrationSuspendEnabled(int enabled, Message response) {
        RILRequest rr = RILRequest.obtain(2049, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(enabled);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setResumeRegistration(int sessionId, Message response) {
        RILRequest rr = RILRequest.obtain(2050, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(sessionId);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void triggerModeSwitchByEcc(int mode, Message response) {
        RILRequest rr = RILRequest.obtain(2142, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(mode);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ", mode:" + mode);
        send(rr);
        Message msg = this.mSender.obtainMessage(5);
        msg.obj = null;
        msg.arg1 = rr.mSerial;
        this.mSender.sendMessageDelayed(msg, 2000L);
    }

    @Override
    public void enablePseudoBSMonitor(boolean reportOn, int reportRateInMinutes, Message response) {
        RILRequest rr = RILRequest.obtain(2153, response);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(reportOn ? 1 : 0);
        rr.mParcel.writeInt(reportRateInMinutes);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void disablePseudoBSMonitor(Message response) {
        RILRequest rr = RILRequest.obtain(2153, response);
        rr.mParcel.writeInt(3);
        rr.mParcel.writeInt(0);
        rr.mParcel.writeInt(0);
        rr.mParcel.writeInt(0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void queryPseudoBSRecords(Message response) {
        RILRequest rr = RILRequest.obtain(2154, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }
}
