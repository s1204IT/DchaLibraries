package com.android.internal.telephony;

import android.app.ActivityManagerNative;
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
import android.os.PowerManager;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SignalStrength;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.SparseArray;
import android.view.Display;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.cdma.SignalToneUtil;
import com.android.internal.telephony.dataconnection.DataCallResponse;
import com.android.internal.telephony.dataconnection.DataProfile;
import com.android.internal.telephony.dataconnection.DcFailCause;
import com.android.internal.telephony.gsm.CallFailCause;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SsData;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.google.android.mms.pdu.CharacterSets;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RIL extends BaseCommands implements CommandsInterface {
    private static final int CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES = 31;
    private static final int CDMA_BSI_NO_OF_INTS_STRUCT = 3;
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT = 60000;
    static final int EVENT_SEND = 1;
    static final int EVENT_WAKE_LOCK_TIMEOUT = 2;
    static final int RESPONSE_SOLICITED = 0;
    static final int RESPONSE_UNSOLICITED = 1;
    static final boolean RILJ_LOGD = true;
    static final boolean RILJ_LOGV = false;
    static final String RILJ_LOG_TAG = "RILJ";
    static final int RIL_MAX_COMMAND_BYTES = 8192;
    static final String[] SOCKET_NAME_RIL = {"rild", "rild2", "rild3"};
    static final int SOCKET_OPEN_RETRY_MILLIS = 4000;
    Display mDefaultDisplay;
    int mDefaultDisplayState;
    private final DisplayManager.DisplayListener mDisplayListener;
    private Integer mInstanceId;
    Object mLastNITZTimeInfo;
    SmsMessage mLastSms;
    RILReceiver mReceiver;
    Thread mReceiverThread;
    SparseArray<RILRequest> mRequestList;
    RILSender mSender;
    HandlerThread mSenderThread;
    LocalSocket mSocket;
    private Boolean mSubInfoReady;
    private Handler mSupportedRafHandler;
    AtomicBoolean mTestingEmergencyCall;
    PowerManager.WakeLock mWakeLock;
    int mWakeLockCount;
    final int mWakeLockTimeout;
    private final BroadcastReceiver sReceiver;

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
                    try {
                        LocalSocket s = RIL.this.mSocket;
                        if (s == null) {
                            rr.onError(1, null);
                            rr.release();
                            RIL.this.decrementWakeLock();
                            return;
                        }
                        synchronized (RIL.this.mRequestList) {
                            RIL.this.mRequestList.append(rr.mSerial, rr);
                            break;
                        }
                        byte[] data = rr.mParcel.marshall();
                        rr.mParcel.recycle();
                        rr.mParcel = null;
                        if (data.length > 8192) {
                            throw new RuntimeException("Parcel larger than max bytes allowed! " + data.length);
                        }
                        byte[] bArr = this.dataLength;
                        this.dataLength[1] = 0;
                        bArr[0] = 0;
                        this.dataLength[2] = (byte) ((data.length >> 8) & 255);
                        this.dataLength[3] = (byte) (data.length & 255);
                        s.getOutputStream().write(this.dataLength);
                        s.getOutputStream().write(data);
                        return;
                    } catch (IOException ex) {
                        Rlog.e(RIL.RILJ_LOG_TAG, "IOException", ex);
                        RILRequest req = RIL.this.findAndRemoveRequestFromList(rr.mSerial);
                        if (req != null) {
                            rr.onError(1, null);
                            rr.release();
                            RIL.this.decrementWakeLock();
                            return;
                        }
                        return;
                    } catch (RuntimeException exc) {
                        Rlog.e(RIL.RILJ_LOG_TAG, "Uncaught exception ", exc);
                        RILRequest req2 = RIL.this.findAndRemoveRequestFromList(rr.mSerial);
                        if (req2 != null) {
                            rr.onError(2, null);
                            rr.release();
                            RIL.this.decrementWakeLock();
                            return;
                        }
                        return;
                    }
                case 2:
                    synchronized (RIL.this.mRequestList) {
                        if (RIL.this.clearWakeLock()) {
                            int count = RIL.this.mRequestList.size();
                            Rlog.d(RIL.RILJ_LOG_TAG, "WAKE_LOCK_TIMEOUT  mRequestList=" + count);
                            for (int i = 0; i < count; i++) {
                                RILRequest rr2 = RIL.this.mRequestList.valueAt(i);
                                Rlog.d(RIL.RILJ_LOG_TAG, i + ": [" + rr2.mSerial + "] " + RIL.requestToString(rr2.mRequest));
                            }
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
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
        int messageLength = ((buffer[0] & 255) << 24) | ((buffer[1] & 255) << 16) | ((buffer[2] & 255) << 8) | (buffer[3] & 255);
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
        byte[] buffer = new byte[8192];

        RILReceiver() {
        }

        @Override
        public void run() {
            String rilSocket;
            int retryCount = 0;
            while (true) {
                LocalSocket s = null;
                try {
                    if (RIL.this.mInstanceId != null && RIL.this.mInstanceId.intValue() != 0) {
                        rilSocket = RIL.SOCKET_NAME_RIL[RIL.this.mInstanceId.intValue()];
                    } else {
                        rilSocket = RIL.SOCKET_NAME_RIL[0];
                    }
                    try {
                        LocalSocket s2 = new LocalSocket();
                        try {
                            try {
                                LocalSocketAddress l = new LocalSocketAddress(rilSocket, LocalSocketAddress.Namespace.RESERVED);
                                s2.connect(l);
                                retryCount = 0;
                                RIL.this.mSocket = s2;
                                Rlog.i(RIL.RILJ_LOG_TAG, "(" + RIL.this.mInstanceId + ") Connected to '" + rilSocket + "' socket");
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
                                } catch (IOException e) {
                                }
                                RIL.this.mSocket = null;
                                RILRequest.resetSerial();
                                RIL.this.clearRequestList(1, false);
                            } catch (IOException e2) {
                                s = s2;
                                if (s != null) {
                                    try {
                                        s.close();
                                    } catch (IOException e3) {
                                    }
                                }
                                if (retryCount == 8) {
                                    Rlog.e(RIL.RILJ_LOG_TAG, "Couldn't find '" + rilSocket + "' socket after " + retryCount + " times, continuing to retry silently");
                                } else if (retryCount >= 0 && retryCount < 8) {
                                    Rlog.i(RIL.RILJ_LOG_TAG, "Couldn't find '" + rilSocket + "' socket; retrying after timeout");
                                }
                                try {
                                    Thread.sleep(4000L);
                                } catch (InterruptedException e4) {
                                }
                                retryCount++;
                            }
                        } catch (Throwable th) {
                            tr = th;
                            Rlog.e(RIL.RILJ_LOG_TAG, "Uncaught exception", tr);
                            RIL.this.notifyRegistrantsRilConnectionChanged(-1);
                            return;
                        }
                    } catch (IOException e5) {
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
        this.mSubInfoReady = false;
        this.mRequestList = new SparseArray<>();
        this.mTestingEmergencyCall = new AtomicBoolean(false);
        this.mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
            }

            @Override
            public void onDisplayChanged(int displayId) {
                if (displayId == 0) {
                    RIL.this.updateScreenState();
                }
            }
        };
        this.sReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE")) {
                    SubscriptionController mSubscriptionController = SubscriptionController.getInstance();
                    int[] subId = mSubscriptionController.getSubIdUsingSlotId(RIL.this.mInstanceId.intValue());
                    if (subId == null || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
                        RIL.this.mSubInfoReady = false;
                    } else {
                        RIL.this.mSubInfoReady = true;
                    }
                    if (RIL.this.mLastSms != null && RIL.this.mSubInfoReady.booleanValue()) {
                        RIL.this.riljLog("Send cached sms when receive ACTION_SUBINFO_CONTENT_CHANGE broadcast");
                        RIL.this.mGsmSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, RIL.this.mLastSms, (Throwable) null));
                        RIL.this.mLastSms = null;
                    }
                }
            }
        };
        this.mSupportedRafHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                AsyncResult ar = (AsyncResult) msg.obj;
                RadioCapability rc = (RadioCapability) ar.result;
                if (ar.exception != null) {
                    RIL.this.riljLog("Get supported radio access family fail");
                    return;
                }
                RIL.this.mSupportedRaf = rc.getRadioAccessFamily();
                RIL.this.riljLog("Supported radio access family=" + RIL.this.mSupportedRaf);
            }
        };
        riljLog("RIL(context, preferredNetworkType=" + preferredNetworkType + " cdmaSubscription=" + cdmaSubscription + ")");
        this.mContext = context;
        this.mCdmaSubscription = cdmaSubscription;
        this.mPreferredNetworkType = preferredNetworkType;
        this.mPhoneType = 0;
        this.mInstanceId = instanceId;
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, RILJ_LOG_TAG);
        this.mWakeLock.setReferenceCounted(false);
        this.mWakeLockTimeout = SystemProperties.getInt("ro.ril.wake_lock_timeout", 60000);
        this.mWakeLockCount = 0;
        IntentFilter intentFilter = new IntentFilter("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
        this.mContext.registerReceiver(this.sReceiver, intentFilter);
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
            dm.registerDisplayListener(this.mDisplayListener, null);
        }
        TelephonyDevController.getInstance();
        TelephonyDevController.registerRIL(this);
    }

    @Override
    public void getVoiceRadioTechnology(Message result) {
        RILRequest rr = RILRequest.obtain(108, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void getImsRegistrationState(Message result) {
        RILRequest rr = RILRequest.obtain(112, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setOnNITZTime(Handler h, int what, Object obj) {
        super.setOnNITZTime(h, what, obj);
        if (this.mLastNITZTimeInfo != null) {
            this.mNITZTimeRegistrant.notifyRegistrant(new AsyncResult((Object) null, this.mLastNITZTimeInfo, (Throwable) null));
        }
    }

    @Override
    public void setOnNewGsmSms(Handler h, int what, Object obj) {
        super.setOnNewGsmSms(h, what, obj);
        if (this.mLastSms != null && this.mSubInfoReady.booleanValue()) {
            riljLog("Send cached sms when receive ACTION_SUBINFO_CONTENT_CHANGE broadcast");
            this.mGsmSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, this.mLastSms, (Throwable) null));
            this.mLastSms = null;
        }
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
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(allowed ? 1 : 0);
        send(rr);
    }

    @Override
    public void getCPPhonebookStatus(Message result) {
        RILRequest rr = RILRequest.obtain(512, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
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
    public void changeBarringPassword(String facility, String oldPwd, String newPwd, String newPwdVerify, Message result) {
        RILRequest rr = RILRequest.obtain(44, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeInt(4);
        rr.mParcel.writeString(facility);
        rr.mParcel.writeString(oldPwd);
        rr.mParcel.writeString(newPwd);
        rr.mParcel.writeString(newPwdVerify);
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
        send(rr);
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
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(gsmIndex);
        send(rr);
    }

    @Override
    public void hangupWaitingOrBackground(Message result) {
        RILRequest rr = RILRequest.obtain(13, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void hangupForegroundResumeBackground(Message result) {
        RILRequest rr = RILRequest.obtain(14, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void switchWaitingOrHoldingAndActive(Message result) {
        RILRequest rr = RILRequest.obtain(15, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void conference(Message result) {
        RILRequest rr = RILRequest.obtain(16, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
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
        send(rr);
    }

    @Override
    public void acceptCall(Message result) {
        RILRequest rr = RILRequest.obtain(40, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
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
        send(rr);
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
        RILRequest rr = RILRequest.obtain(49, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        rr.mParcel.writeString(Character.toString(c));
        send(rr);
    }

    @Override
    public void stopDtmf(Message result) {
        RILRequest rr = RILRequest.obtain(50, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
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
        send(rr);
    }

    @Override
    public void sendSMSExpectMore(String smscPDU, String pdu, Message result) {
        RILRequest rr = RILRequest.obtain(26, result);
        constructGsmSendSmsRilRequest(rr, smscPDU, pdu);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
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
        send(rr);
    }

    @Override
    public void sendImsGsmSms(String smscPDU, String pdu, int retry, int messageRef, Message result) {
        RILRequest rr = RILRequest.obtain(113, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeByte((byte) retry);
        rr.mParcel.writeInt(messageRef);
        constructGsmSendSmsRilRequest(rr, smscPDU, pdu);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void sendImsCdmaSms(byte[] pdu, int retry, int messageRef, Message result) {
        RILRequest rr = RILRequest.obtain(113, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeByte((byte) retry);
        rr.mParcel.writeInt(messageRef);
        constructCdmaSendSmsRilRequest(rr, pdu);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
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
        rr.mParcel.writeString(pdu);
        send(rr);
    }

    private int translateStatus(int status) {
        switch (status & 7) {
            case 1:
            case 2:
            case 4:
            case 6:
            default:
                return 1;
            case 3:
                return 0;
            case 5:
                return 3;
            case 7:
                return 2;
        }
    }

    @Override
    public void setupDataCall(String radioTechnology, String profile, String apn, String user, String password, String authType, String protocol, Message result) {
        RILRequest rr = RILRequest.obtain(27, result);
        rr.mParcel.writeInt(7);
        rr.mParcel.writeString(radioTechnology);
        rr.mParcel.writeString(profile);
        rr.mParcel.writeString(apn);
        rr.mParcel.writeString(user);
        rr.mParcel.writeString(password);
        rr.mParcel.writeString(authType);
        rr.mParcel.writeString(protocol);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + radioTechnology + " " + profile + " " + apn + " " + user + " " + password + " " + authType + " " + protocol);
        send(rr);
    }

    @Override
    public void deactivateDataCall(int cid, int reason, Message result) {
        RILRequest rr = RILRequest.obtain(41, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeString(Integer.toString(cid));
        rr.mParcel.writeString(Integer.toString(reason));
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + cid + " " + reason);
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
    public void setNetworkSelectionModeManualExt(String operatorNumeric, int rat, Message response) {
        RILRequest rr = RILRequest.obtain(502, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + operatorNumeric + " " + rat);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeString(null);
        rr.mParcel.writeString(null);
        rr.mParcel.writeString(operatorNumeric);
        rr.mParcel.writeInt(rat);
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
        RILRequest rr = RILRequest.obtain(48, response);
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
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + "[" + com.android.internal.telephony.uicc.IccUtils.bytesToHexString(data) + "]");
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
    public void handleCallSetupRequestFromSim(boolean accept, Message response) {
        RILRequest rr = RILRequest.obtain(71, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        int[] param = new int[1];
        param[0] = accept ? 1 : 0;
        rr.mParcel.writeIntArray(param);
        send(rr);
    }

    @Override
    public void setPreferredNetworkType(int networkType, Message response) {
        RILRequest rr = RILRequest.obtain(73, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(networkType);
        this.mPreferredNetworkType = networkType;
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " : " + networkType);
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
        RILRequest rr = RILRequest.obtain(76, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(enable ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + enable);
        send(rr);
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
        RILRequest rr = RILRequest.obtain(102, result);
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
        int oldState = this.mDefaultDisplayState;
        this.mDefaultDisplayState = this.mDefaultDisplay.getState();
        if (this.mDefaultDisplayState != oldState) {
            if (oldState != 2 && this.mDefaultDisplayState == 2) {
                sendScreenState(true);
            } else if ((oldState == 2 || oldState == 0) && this.mDefaultDisplayState != 2) {
                sendScreenState(false);
            }
        }
    }

    private void sendScreenState(boolean on) {
        RILRequest rr = RILRequest.obtain(61, null);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(on ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + on);
        send(rr);
    }

    @Override
    protected void onRadioAvailable() {
        updateScreenState();
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
        setRadioState(newState);
    }

    private void acquireWakeLock() {
        synchronized (this.mWakeLock) {
            this.mWakeLock.acquire();
            this.mWakeLockCount++;
            this.mSender.removeMessages(2);
            Message msg = this.mSender.obtainMessage(2);
            this.mSender.sendMessageDelayed(msg, this.mWakeLockTimeout);
        }
    }

    private void decrementWakeLock() {
        synchronized (this.mWakeLock) {
            if (this.mWakeLockCount > 1) {
                this.mWakeLockCount--;
            } else {
                this.mWakeLockCount = 0;
                this.mWakeLock.release();
                this.mSender.removeMessages(2);
            }
        }
    }

    private boolean clearWakeLock() {
        boolean z = false;
        synchronized (this.mWakeLock) {
            if (this.mWakeLockCount != 0 || this.mWakeLock.isHeld()) {
                Rlog.d(RILJ_LOG_TAG, "NOTE: mWakeLockCount is " + this.mWakeLockCount + "at time of clearing");
                this.mWakeLockCount = 0;
                this.mWakeLock.release();
                this.mSender.removeMessages(2);
                z = true;
            }
        }
        return z;
    }

    private void send(RILRequest rr) {
        if (this.mSocket == null) {
            rr.onError(1, null);
            rr.release();
        } else {
            Message msg = this.mSender.obtainMessage(1, rr);
            acquireWakeLock();
            msg.sendToTarget();
        }
    }

    private void processResponse(Parcel p) {
        RILRequest rr;
        int type = p.readInt();
        if (type == 1) {
            processUnsolicited(p);
        } else if (type == 0 && (rr = processSolicited(p)) != null) {
            rr.release();
            decrementWakeLock();
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
                rr.release();
                decrementWakeLock();
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

    private RILRequest processSolicited(Parcel p) {
        int serial = p.readInt();
        int error = p.readInt();
        RILRequest rr = findAndRemoveRequestFromList(serial);
        if (rr == null) {
            Rlog.w(RILJ_LOG_TAG, "Unexpected solicited response! sn: " + serial + " error: " + error);
            return null;
        }
        Object ret = null;
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
                        if (this.mTestingEmergencyCall.getAndSet(false) && this.mEmergencyCallbackModeRegistrant != null) {
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
                        ret = responseInts(p);
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
                    case SmsHeader.ELT_ID_STANDARD_WVG_OBJECT:
                        ret = responseVoid(p);
                        break;
                    case SmsHeader.ELT_ID_CHARACTER_SIZE_WVG_OBJECT:
                        ret = responseSMS(p);
                        break;
                    case SmsHeader.ELT_ID_EXTENDED_OBJECT_DATA_REQUEST_CMD:
                        ret = responseSMS(p);
                        break;
                    case 27:
                        ret = responseSetupDataCall(p);
                        break;
                    case 28:
                        ret = responseICC_IO(p);
                        break;
                    case 29:
                        ret = responseVoid(p);
                        break;
                    case CallFailCause.STATUS_ENQUIRY:
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
                    case RadioNVItems.RIL_NV_MIP_PROFILE_HA_SPI:
                        ret = responseString(p);
                        break;
                    case 39:
                        ret = responseString(p);
                        break;
                    case RadioNVItems.RIL_NV_MIP_PROFILE_MN_HA_SS:
                        ret = responseVoid(p);
                        break;
                    case 41:
                        ret = responseVoid(p);
                        break;
                    case 42:
                        ret = responseInts(p);
                        break;
                    case 43:
                        ret = responseInts(p);
                        break;
                    case CallFailCause.CHANNEL_NOT_AVAIL:
                        ret = responseVoid(p);
                        break;
                    case 45:
                        ret = responseInts(p);
                        break;
                    case 46:
                        ret = responseVoid(p);
                        break;
                    case WspTypeDecoder.PARAMETER_ID_X_WAP_APPLICATION_ID:
                        ret = responseVoid(p);
                        break;
                    case 48:
                        ret = responseOperatorInfos(p);
                        break;
                    case CallFailCause.QOS_NOT_AVAIL:
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
                    case RadioNVItems.RIL_NV_CDMA_BC14:
                        ret = responseVoid(p);
                        break;
                    case RadioNVItems.RIL_NV_CDMA_SO68:
                        ret = responseInts(p);
                        break;
                    case RadioNVItems.RIL_NV_CDMA_SO73_COP0:
                        ret = responseInts(p);
                        break;
                    case RadioNVItems.RIL_NV_CDMA_SO73_COP1TO7:
                        ret = responseInts(p);
                        break;
                    case RadioNVItems.RIL_NV_CDMA_1X_ADVANCED_ENABLED:
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
                    case 61:
                        ret = responseVoid(p);
                        break;
                    case 62:
                        ret = responseVoid(p);
                        break;
                    case SignalToneUtil.IS95_CONST_IR_SIG_TONE_NO_TONE:
                        ret = responseInts(p);
                        break;
                    case 64:
                        ret = responseVoid(p);
                        break;
                    case 65:
                        ret = responseVoid(p);
                        break;
                    case 66:
                        ret = responseInts(p);
                        break;
                    case 67:
                        ret = responseString(p);
                        break;
                    case 68:
                        ret = responseVoid(p);
                        break;
                    case 69:
                        ret = responseString(p);
                        break;
                    case 70:
                        ret = responseVoid(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_BAND_ENABLE_25:
                        ret = responseInts(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_BAND_ENABLE_26:
                        ret = responseVoid(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_BAND_ENABLE_41:
                        ret = responseVoid(p);
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
                    case RadioNVItems.RIL_NV_LTE_HIDDEN_BAND_PRIORITY_41:
                        ret = responseInts(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_NEXT_SCAN:
                        ret = responseVoid(p);
                        break;
                    case RadioNVItems.RIL_NV_LTE_BSR_TIMER:
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
                    case 88:
                        ret = responseVoid(p);
                        break;
                    case 89:
                        ret = responseGmsBroadcastConfig(p);
                        break;
                    case 90:
                        ret = responseVoid(p);
                        break;
                    case 91:
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
                    case 95:
                        ret = responseStrings(p);
                        break;
                    case CommandsInterface.CDMA_SMS_FAIL_CAUSE_ENCODING_PROBLEM:
                        ret = responseInts(p);
                        break;
                    case 97:
                        ret = responseVoid(p);
                        break;
                    case 98:
                        ret = responseStrings(p);
                        break;
                    case 99:
                        ret = responseVoid(p);
                        break;
                    case 100:
                        ret = responseString(p);
                        break;
                    case 101:
                        ret = responseVoid(p);
                        break;
                    case 102:
                        ret = responseVoid(p);
                        break;
                    case 103:
                        ret = responseVoid(p);
                        break;
                    case 104:
                        ret = responseInts(p);
                        break;
                    case 105:
                        ret = responseString(p);
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
                    case 109:
                        ret = responseCellInfoList(p);
                        break;
                    case 110:
                        ret = responseVoid(p);
                        break;
                    case 111:
                        ret = responseVoid(p);
                        break;
                    case 112:
                        ret = responseInts(p);
                        break;
                    case 113:
                        ret = responseSMS(p);
                        break;
                    case 114:
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
                    case 500:
                        ret = responseVoid(p);
                        break;
                    case 501:
                        ret = responseVoid(p);
                        break;
                    case 502:
                        ret = responseVoid(p);
                        break;
                    case 503:
                        ret = responseInts(p);
                        break;
                    case 504:
                        ret = responseVoid(p);
                        break;
                    case 505:
                        ret = responseVoid(p);
                        break;
                    case 506:
                        ret = responseInts(p);
                        break;
                    case 507:
                        ret = responseVoid(p);
                        break;
                    case 508:
                        ret = responseInts(p);
                        break;
                    case 509:
                        ret = responseVoid(p);
                        break;
                    case 510:
                        ret = responseInts(p);
                        break;
                    case 511:
                        ret = responseRaw(p);
                        break;
                    case 512:
                        ret = responseInts(p);
                        break;
                    default:
                        throw new RuntimeException("Unrecognized solicited response: " + rr.mRequest);
                }
            } catch (Throwable tr) {
                Rlog.w(RILJ_LOG_TAG, rr.serialString() + "< " + requestToString(rr.mRequest) + " exception, possible invalid RIL response", tr);
                if (rr.mResult != null) {
                    AsyncResult.forMessage(rr.mResult, (Object) null, tr);
                    rr.mResult.sendToTarget();
                    return rr;
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
                case 43:
                    if (this.mIccStatusChangedRegistrants != null) {
                        riljLog("ON some errors fakeSimStatusChanged: reg count=" + this.mIccStatusChangedRegistrants.size());
                        this.mIccStatusChangedRegistrants.notifyRegistrants();
                    }
                    break;
            }
            rr.onError(error, ret);
            return rr;
        }
        riljLog(rr.serialString() + "< " + requestToString(rr.mRequest) + " " + retToString(rr.mRequest, ret));
        if (rr.mResult != null) {
            AsyncResult.forMessage(rr.mResult, ret, (Throwable) null);
            rr.mResult.sendToTarget();
            return rr;
        }
        return rr;
    }

    static String retToString(int req, Object ret) {
        if (ret == null) {
            return "";
        }
        switch (req) {
            case 11:
            case RadioNVItems.RIL_NV_MIP_PROFILE_HA_SPI:
            case 39:
            case 115:
            case 117:
                return "";
            default:
                if (ret instanceof int[]) {
                    int[] intArray = (int[]) ret;
                    int length = intArray.length;
                    StringBuilder sb = new StringBuilder("{");
                    if (length > 0) {
                        sb.append(intArray[0]);
                        for (int i = 0 + 1; i < length; i++) {
                            sb.append(", ").append(intArray[i]);
                        }
                    }
                    sb.append("}");
                    return sb.toString();
                }
                if (ret instanceof String[]) {
                    String[] strings = (String[]) ret;
                    int length2 = strings.length;
                    StringBuilder sb2 = new StringBuilder("{");
                    if (length2 > 0) {
                        sb2.append(strings[0]);
                        for (int i2 = 0 + 1; i2 < length2; i2++) {
                            sb2.append(", ").append(strings[i2]);
                        }
                    }
                    sb2.append("}");
                    return sb2.toString();
                }
                if (req == 9) {
                    ArrayList<DriverCall> calls = (ArrayList) ret;
                    StringBuilder sb3 = new StringBuilder(" ");
                    for (DriverCall dc : calls) {
                        sb3.append("[").append(dc).append("] ");
                    }
                    return sb3.toString();
                }
                if (req == 75) {
                    ArrayList<NeighboringCellInfo> cells = (ArrayList) ret;
                    StringBuilder sb4 = new StringBuilder(" ");
                    for (NeighboringCellInfo cell : cells) {
                        sb4.append(cell).append(" ");
                    }
                    return sb4.toString();
                }
                if (req != 124) {
                    if (req == 59) {
                        return com.android.internal.telephony.uicc.IccUtils.bytesToHexString((byte[]) ret);
                    }
                    String s = ret.toString();
                    return s;
                }
                ArrayList<HardwareConfig> hwcfgs = (ArrayList) ret;
                StringBuilder sb5 = new StringBuilder(" ");
                for (HardwareConfig hwcfg : hwcfgs) {
                    sb5.append("[").append(hwcfg).append("] ");
                }
                return sb5.toString();
        }
    }

    private void processUnsolicited(Parcel p) {
        Object ret;
        int response = p.readInt();
        try {
            switch (response) {
                case 1000:
                    ret = responseVoid(p);
                    break;
                case 1001:
                    ret = responseVoid(p);
                    break;
                case 1002:
                    ret = responseVoid(p);
                    break;
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_REORDER:
                    ret = responseString(p);
                    break;
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_SO_REJECT:
                    ret = responseString(p);
                    break;
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_RETRY_ORDER:
                    ret = responseInts(p);
                    break;
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_ACCESS_FAILURE:
                    ret = responseStrings(p);
                    break;
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_NOT_EMERGENCY:
                    ret = responseString(p);
                    break;
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_ACCESS_BLOCKED:
                    ret = responseSignalStrength(p);
                    break;
                case 1010:
                    ret = responseDataCallList(p);
                    break;
                case 1011:
                    ret = responseSuppServiceNotification(p);
                    break;
                case 1012:
                    ret = responseVoid(p);
                    break;
                case 1013:
                    ret = responseString(p);
                    break;
                case 1014:
                    ret = responseString(p);
                    break;
                case CharacterSets.UTF_16:
                    ret = responseInts(p);
                    break;
                case 1016:
                    ret = responseVoid(p);
                    break;
                case 1017:
                    ret = responseSimRefresh(p);
                    break;
                case 1018:
                    ret = responseCallRing(p);
                    break;
                case 1019:
                    ret = responseVoid(p);
                    break;
                case 1020:
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
                    ret = responseString(p);
                    break;
                case 1500:
                    ret = responseString(p);
                    break;
                case 1501:
                    ret = responseString(p);
                    break;
                case 1502:
                    ret = responseString(p);
                    break;
                case 1503:
                    ret = responseString(p);
                    break;
                case 1504:
                    ret = responseString(p);
                    break;
                case 1505:
                    ret = responseVoid(p);
                    break;
                case 1506:
                    ret = responseInts(p);
                    break;
                case 1507:
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
                    unsljLog(response);
                    this.mVoiceNetworkStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
                    return;
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_REORDER:
                    unsljLog(response);
                    String[] a = new String[2];
                    a[1] = (String) ret;
                    SmsMessage sms = SmsMessage.newFromCMT(a);
                    if (this.mGsmSmsRegistrant != null && this.mSubInfoReady.booleanValue()) {
                        this.mGsmSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, sms, (Throwable) null));
                        return;
                    } else {
                        riljLog("Cache sms in case sms registrant isnt registered yet");
                        this.mLastSms = sms;
                        return;
                    }
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_SO_REJECT:
                    unsljLogRet(response, ret);
                    if (this.mSmsStatusRegistrant != null) {
                        this.mSmsStatusRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_RETRY_ORDER:
                    unsljLogRet(response, ret);
                    int[] smsIndex = (int[]) ret;
                    if (smsIndex.length == 1) {
                        if (this.mSmsOnSimRegistrant != null) {
                            this.mSmsOnSimRegistrant.notifyRegistrant(new AsyncResult((Object) null, smsIndex, (Throwable) null));
                            return;
                        }
                        return;
                    }
                    riljLog(" NEW_SMS_ON_SIM ERROR with wrong length " + smsIndex.length);
                    return;
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_ACCESS_FAILURE:
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
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_NOT_EMERGENCY:
                    unsljLogRet(response, ret);
                    long nitzReceiveTime = p.readLong();
                    Object[] result = {ret, Long.valueOf(nitzReceiveTime)};
                    boolean ignoreNitz = SystemProperties.getBoolean("telephony.test.ignore.nitz", false);
                    if (ignoreNitz) {
                        riljLog("ignoring UNSOL_NITZ_TIME_RECEIVED");
                        return;
                    }
                    if (this.mNITZTimeRegistrant != null) {
                        this.mNITZTimeRegistrant.notifyRegistrant(new AsyncResult((Object) null, result, (Throwable) null));
                    }
                    this.mLastNITZTimeInfo = result;
                    return;
                case com.android.internal.telephony.cdma.CallFailCause.CDMA_ACCESS_BLOCKED:
                    if (this.mSignalStrengthRegistrant != null) {
                        this.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1010:
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
                case 1013:
                    unsljLog(response);
                    if (this.mCatProCmdRegistrant != null) {
                        this.mCatProCmdRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1014:
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
                case 1016:
                    unsljLog(response);
                    if (this.mIccSmsFullRegistrant != null) {
                        this.mIccSmsFullRegistrant.notifyRegistrant();
                        return;
                    }
                    return;
                case 1017:
                    unsljLogRet(response, ret);
                    if (this.mIccRefreshRegistrants != null) {
                        this.mIccRefreshRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1018:
                    unsljLogRet(response, ret);
                    if (this.mRingRegistrant != null) {
                        this.mRingRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1019:
                    unsljLog(response);
                    this.mIccStatusChaned = true;
                    if (this.mIccStatusChangedRegistrants != null) {
                        this.mIccStatusChangedRegistrants.notifyRegistrants();
                        return;
                    }
                    return;
                case 1020:
                    unsljLog(response);
                    SmsMessage sms2 = (SmsMessage) ret;
                    if (this.mCdmaSmsRegistrant != null) {
                        this.mCdmaSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, sms2, (Throwable) null));
                        return;
                    }
                    return;
                case 1021:
                    unsljLogvRet(response, com.android.internal.telephony.uicc.IccUtils.bytesToHexString((byte[]) ret));
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
                    unsljLogvRet(response, com.android.internal.telephony.uicc.IccUtils.bytesToHexString((byte[]) ret));
                    if (this.mUnsolOemHookRawRegistrant != null) {
                        this.mUnsolOemHookRawRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1029:
                    unsljLogvRet(response, ret);
                    if (this.mRingbackToneRegistrants != null) {
                        boolean playtone = ((int[]) ret)[0] == 1;
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
                    getRadioCapability(this.mSupportedRafHandler.obtainMessage());
                    setRadioPower(false, null);
                    setPreferredNetworkType(this.mPreferredNetworkType, null);
                    setCdmaSubscriptionSource(this.mCdmaSubscription, null);
                    setCellInfoListRate(Integer.MAX_VALUE, null);
                    notifyRegistrantsRilConnectionChanged(((int[]) ret)[0]);
                    return;
                case 1035:
                    unsljLogRet(response, ret);
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
                case 1500:
                    unsljLogRet(response, ret);
                    if (this.mCatCallSetUpStatusRegistrant != null) {
                        this.mCatCallSetUpStatusRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1501:
                    unsljLogRet(response, ret);
                    if (this.mCatCallSetUpResultRegistrant != null) {
                        this.mCatCallSetUpResultRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1502:
                    unsljLogRet(response, ret);
                    if (this.mCatSendSmStatusRegistrant != null) {
                        this.mCatSendSmStatusRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1503:
                    unsljLogRet(response, ret);
                    if (this.mCatSendSmResultRegistrant != null) {
                        this.mCatSendSmResultRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1504:
                    unsljLogRet(response, ret);
                    if (this.mCatSendUssdResultRegistrant != null) {
                        this.mCatSendUssdResultRegistrant.notifyRegistrant(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
                    return;
                case 1505:
                    unsljLogRet(response, ret);
                    Intent intent = new Intent("android.intent.action.CP_PHONEBOOK_INITED");
                    intent.putExtra("slot", this.mInstanceId);
                    ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
                    return;
                case 1506:
                    unsljLogRet(response, ret);
                    return;
                case 1507:
                    unsljLogRet(response, ret);
                    if (this.mSimHotPlugRegistrants != null) {
                        this.mSimHotPlugRegistrants.notifyRegistrants(new AsyncResult((Object) null, ret, (Throwable) null));
                        return;
                    }
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
        if (this.mRilConnectedRegistrants != null) {
            this.mRilConnectedRegistrants.notifyRegistrants(new AsyncResult((Object) null, new Integer(rilVer), (Throwable) null));
        }
    }

    private Object responseInts(Parcel p) {
        int numInts = p.readInt();
        int[] response = new int[numInts];
        for (int i = 0; i < numInts; i++) {
            response[i] = p.readInt();
        }
        return response;
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
                riljLogv("Incoming UUS : data (hex): " + com.android.internal.telephony.uicc.IccUtils.bytesToHexString(dc.uusInfo.getUserData()));
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
        if (num == 0 && this.mTestingEmergencyCall.getAndSet(false) && this.mEmergencyCallbackModeRegistrant != null) {
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
            if (version >= 10) {
                String pcscf = p.readString();
                if (!TextUtils.isEmpty(pcscf)) {
                    dataCall.pcscf = pcscf.split(" ");
                }
            }
            if (version >= 11) {
                dataCall.mtu = p.readInt();
            }
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
            throw new RuntimeException("RIL_REQUEST_SETUP_DATA_CALL response expecting 1 RIL_Data_Call_response_v5 got " + num);
        }
        return getDataCallResponse(p, ver);
    }

    private Object responseOperatorInfos(Parcel p) {
        String[] strings = (String[]) responseStrings(p);
        if (strings.length % 5 != 0) {
            throw new RuntimeException("RIL_REQUEST_QUERY_AVAILABLE_NETWORKS: invalid response. Got " + strings.length + " strings, expected multible of 4");
        }
        ArrayList<OperatorInfo> ret = new ArrayList<>(strings.length / 5);
        for (int i = 0; i < strings.length; i += 5) {
            ret.add(new OperatorInfo(strings[i + 0], strings[i + 1], strings[i + 2], strings[i + 3], strings[i + 4]));
        }
        return ret;
    }

    private Object responseCellList(Parcel p) {
        int num = p.readInt();
        ArrayList<NeighboringCellInfo> response = new ArrayList<>();
        int[] subId = SubscriptionManager.getSubId(this.mInstanceId.intValue());
        int radioType = ((TelephonyManager) this.mContext.getSystemService("phone")).getDataNetworkType(subId[0]);
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

    private Object responseGetPreferredNetworkType(Parcel p) {
        int[] response = (int[]) responseInts(p);
        if (response.length >= 1) {
            this.mPreferredNetworkType = response[0];
        }
        return response;
    }

    private Object responseGmsBroadcastConfig(Parcel p) {
        int num = p.readInt();
        ArrayList<SmsBroadcastConfigInfo> response = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            int fromId = p.readInt();
            int toId = p.readInt();
            int fromScheme = p.readInt();
            int toScheme = p.readInt();
            boolean selected = p.readInt() == 1;
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
        return response;
    }

    private void notifyRegistrantsCdmaInfoRec(CdmaInformationRecords infoRec) {
        if (infoRec.record instanceof CdmaInformationRecords.CdmaDisplayInfoRec) {
            if (this.mDisplayInfoRegistrants != null) {
                unsljLogRet(1027, infoRec.record);
                this.mDisplayInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
                return;
            }
            return;
        }
        if (infoRec.record instanceof CdmaInformationRecords.CdmaSignalInfoRec) {
            if (this.mSignalInfoRegistrants != null) {
                unsljLogRet(1027, infoRec.record);
                this.mSignalInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
                return;
            }
            return;
        }
        if (infoRec.record instanceof CdmaInformationRecords.CdmaNumberInfoRec) {
            if (this.mNumberInfoRegistrants != null) {
                unsljLogRet(1027, infoRec.record);
                this.mNumberInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
                return;
            }
            return;
        }
        if (infoRec.record instanceof CdmaInformationRecords.CdmaRedirectingNumberInfoRec) {
            if (this.mRedirNumInfoRegistrants != null) {
                unsljLogRet(1027, infoRec.record);
                this.mRedirNumInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
                return;
            }
            return;
        }
        if (infoRec.record instanceof CdmaInformationRecords.CdmaLineControlInfoRec) {
            if (this.mLineControlInfoRegistrants != null) {
                unsljLogRet(1027, infoRec.record);
                this.mLineControlInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
                return;
            }
            return;
        }
        if (infoRec.record instanceof CdmaInformationRecords.CdmaT53ClirInfoRec) {
            if (this.mT53ClirInfoRegistrants != null) {
                unsljLogRet(1027, infoRec.record);
                this.mT53ClirInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
                return;
            }
            return;
        }
        if ((infoRec.record instanceof CdmaInformationRecords.CdmaT53AudioControlInfoRec) && this.mT53AudCntrlInfoRegistrants != null) {
            unsljLogRet(1027, infoRec.record);
            this.mT53AudCntrlInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, infoRec.record, (Throwable) null));
        }
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
            case SmsHeader.ELT_ID_STANDARD_WVG_OBJECT:
                return "DTMF";
            case SmsHeader.ELT_ID_CHARACTER_SIZE_WVG_OBJECT:
                return "SEND_SMS";
            case SmsHeader.ELT_ID_EXTENDED_OBJECT_DATA_REQUEST_CMD:
                return "SEND_SMS_EXPECT_MORE";
            case 27:
                return "SETUP_DATA_CALL";
            case 28:
                return "SIM_IO";
            case 29:
                return "SEND_USSD";
            case CallFailCause.STATUS_ENQUIRY:
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
            case RadioNVItems.RIL_NV_MIP_PROFILE_HA_SPI:
                return "GET_IMEI";
            case 39:
                return "GET_IMEISV";
            case RadioNVItems.RIL_NV_MIP_PROFILE_MN_HA_SS:
                return "ANSWER";
            case 41:
                return "DEACTIVATE_DATA_CALL";
            case 42:
                return "QUERY_FACILITY_LOCK";
            case 43:
                return "SET_FACILITY_LOCK";
            case CallFailCause.CHANNEL_NOT_AVAIL:
                return "CHANGE_BARRING_PASSWORD";
            case 45:
                return "QUERY_NETWORK_SELECTION_MODE";
            case 46:
                return "SET_NETWORK_SELECTION_AUTOMATIC";
            case WspTypeDecoder.PARAMETER_ID_X_WAP_APPLICATION_ID:
                return "SET_NETWORK_SELECTION_MANUAL";
            case 48:
                return "QUERY_AVAILABLE_NETWORKS ";
            case CallFailCause.QOS_NOT_AVAIL:
                return "DTMF_START";
            case 50:
                return "DTMF_STOP";
            case RadioNVItems.RIL_NV_CDMA_PRL_VERSION:
                return "BASEBAND_VERSION";
            case RadioNVItems.RIL_NV_CDMA_BC10:
                return "SEPARATE_CONNECTION";
            case RadioNVItems.RIL_NV_CDMA_BC14:
                return "SET_MUTE";
            case RadioNVItems.RIL_NV_CDMA_SO68:
                return "GET_MUTE";
            case RadioNVItems.RIL_NV_CDMA_SO73_COP0:
                return "QUERY_CLIP";
            case RadioNVItems.RIL_NV_CDMA_SO73_COP1TO7:
                return "LAST_DATA_CALL_FAIL_CAUSE";
            case RadioNVItems.RIL_NV_CDMA_1X_ADVANCED_ENABLED:
                return "DATA_CALL_LIST";
            case 58:
                return "RESET_RADIO";
            case RadioNVItems.RIL_NV_CDMA_EHRPD_FORCED:
                return "OEM_HOOK_RAW";
            case 60:
                return "OEM_HOOK_STRINGS";
            case 61:
                return "SCREEN_STATE";
            case 62:
                return "SET_SUPP_SVC_NOTIFICATION";
            case SignalToneUtil.IS95_CONST_IR_SIG_TONE_NO_TONE:
                return "WRITE_SMS_TO_SIM";
            case 64:
                return "DELETE_SMS_ON_SIM";
            case 65:
                return "SET_BAND_MODE";
            case 66:
                return "QUERY_AVAILABLE_BAND_MODE";
            case 67:
                return "REQUEST_STK_GET_PROFILE";
            case 68:
                return "REQUEST_STK_SET_PROFILE";
            case 69:
                return "REQUEST_STK_SEND_ENVELOPE_COMMAND";
            case 70:
                return "REQUEST_STK_SEND_TERMINAL_RESPONSE";
            case RadioNVItems.RIL_NV_LTE_BAND_ENABLE_25:
                return "REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM";
            case RadioNVItems.RIL_NV_LTE_BAND_ENABLE_26:
                return "REQUEST_EXPLICIT_CALL_TRANSFER";
            case RadioNVItems.RIL_NV_LTE_BAND_ENABLE_41:
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
            case RadioNVItems.RIL_NV_LTE_HIDDEN_BAND_PRIORITY_41:
                return "RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE";
            case RadioNVItems.RIL_NV_LTE_NEXT_SCAN:
                return "RIL_REQUEST_SET_TTY_MODE";
            case RadioNVItems.RIL_NV_LTE_BSR_TIMER:
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
            case 88:
                return "RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE";
            case 89:
                return "RIL_REQUEST_GSM_GET_BROADCAST_CONFIG";
            case 90:
                return "RIL_REQUEST_GSM_SET_BROADCAST_CONFIG";
            case 91:
                return "RIL_REQUEST_GSM_BROADCAST_ACTIVATION";
            case 92:
                return "RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG";
            case 93:
                return "RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG";
            case 94:
                return "RIL_REQUEST_CDMA_BROADCAST_ACTIVATION";
            case 95:
                return "RIL_REQUEST_CDMA_SUBSCRIPTION";
            case CommandsInterface.CDMA_SMS_FAIL_CAUSE_ENCODING_PROBLEM:
                return "RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM";
            case 97:
                return "RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM";
            case 98:
                return "RIL_REQUEST_DEVICE_IDENTITY";
            case 99:
                return "REQUEST_EXIT_EMERGENCY_CALLBACK_MODE";
            case 100:
                return "RIL_REQUEST_GET_SMSC_ADDRESS";
            case 101:
                return "RIL_REQUEST_SET_SMSC_ADDRESS";
            case 102:
                return "RIL_REQUEST_REPORT_SMS_MEMORY_STATUS";
            case 103:
                return "RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING";
            case 104:
                return "RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE";
            case 105:
                return "RIL_REQUEST_ISIM_AUTHENTICATION";
            case 106:
                return "RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU";
            case 107:
                return "RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS";
            case 108:
                return "RIL_REQUEST_VOICE_RADIO_TECH";
            case 109:
                return "RIL_REQUEST_GET_CELL_INFO_LIST";
            case 110:
                return "RIL_REQUEST_SET_CELL_INFO_LIST_RATE";
            case 111:
                return "RIL_REQUEST_SET_INITIAL_ATTACH_APN";
            case 112:
                return "RIL_REQUEST_IMS_REGISTRATION_STATE";
            case 113:
                return "RIL_REQUEST_IMS_SEND_SMS";
            case 114:
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
            case 500:
                return "DIAL_VT";
            case 501:
                return "HANGUP_VT";
            case 502:
                return "SET_NETWORK_SELECTION_MANUAL_EXT";
            case 503:
                return "QUERY_COLP";
            case 504:
                return "SET_CLIP";
            case 505:
                return "SET_COLP";
            case 506:
                return "GET_CNAP";
            case 507:
                return "SET_CNAP";
            case 508:
                return "QUERY_COLR";
            case 509:
                return "SET_COLR";
            case 510:
                return "LOCK_INFO";
            case 511:
                return "SIM_GET_ATR";
            case 512:
                return "GET_CP_PHONEBOOK_STATUS";
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
            case com.android.internal.telephony.cdma.CallFailCause.CDMA_REORDER:
                return "UNSOL_RESPONSE_NEW_SMS";
            case com.android.internal.telephony.cdma.CallFailCause.CDMA_SO_REJECT:
                return "UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT";
            case com.android.internal.telephony.cdma.CallFailCause.CDMA_RETRY_ORDER:
                return "UNSOL_RESPONSE_NEW_SMS_ON_SIM";
            case com.android.internal.telephony.cdma.CallFailCause.CDMA_ACCESS_FAILURE:
                return "UNSOL_ON_USSD";
            case com.android.internal.telephony.cdma.CallFailCause.CDMA_PREEMPTED:
                return "UNSOL_ON_USSD_REQUEST";
            case com.android.internal.telephony.cdma.CallFailCause.CDMA_NOT_EMERGENCY:
                return "UNSOL_NITZ_TIME_RECEIVED";
            case com.android.internal.telephony.cdma.CallFailCause.CDMA_ACCESS_BLOCKED:
                return "UNSOL_SIGNAL_STRENGTH";
            case 1010:
                return "UNSOL_DATA_CALL_LIST_CHANGED";
            case 1011:
                return "UNSOL_SUPP_SVC_NOTIFICATION";
            case 1012:
                return "UNSOL_STK_SESSION_END";
            case 1013:
                return "UNSOL_STK_PROACTIVE_COMMAND";
            case 1014:
                return "UNSOL_STK_EVENT_NOTIFY";
            case CharacterSets.UTF_16:
                return "UNSOL_STK_CALL_SETUP";
            case 1016:
                return "UNSOL_SIM_SMS_STORAGE_FULL";
            case 1017:
                return "UNSOL_SIM_REFRESH";
            case 1018:
                return "UNSOL_CALL_RING";
            case 1019:
                return "UNSOL_RESPONSE_SIM_STATUS_CHANGED";
            case 1020:
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
            case 1500:
                return "UNSOL_STK_CALL_SETUP_STATUS";
            case 1501:
                return "UNSOL_STK_CALL_SETUP_RESULT";
            case 1502:
                return "UNSOL_STK_SEND_SM_STATUS";
            case 1503:
                return "UNSOL_STK_SEND_SM_RESULT";
            case 1504:
                return "UNSOL_STK_SEND_USSD_RESULT";
            case 1505:
                return "UNSOL_CP_PHONEBOOK_INITED";
            case 1506:
                return "UNSOL_VZW_PCO_ACTION";
            case 1507:
                return "UNSOL_RIL_UNSOL_SIM_HOT_PLUG_EVENT";
            default:
                return "<unknown response>";
        }
    }

    private void riljLog(String msg) {
        Rlog.d(RILJ_LOG_TAG, msg + (this.mInstanceId != null ? " [SUB" + this.mInstanceId + "]" : ""));
    }

    private void riljLogv(String msg) {
        Rlog.v(RILJ_LOG_TAG, msg + (this.mInstanceId != null ? " [SUB" + this.mInstanceId + "]" : ""));
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
        RILRequest rr = RILRequest.obtain(104, response);
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
        RILRequest rr = RILRequest.obtain(105, response);
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
        RILRequest rr = RILRequest.obtain(109, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setCellInfoListRate(int rateInMillis, Message response) {
        riljLog("setCellInfoListRate: " + rateInMillis);
        RILRequest rr = RILRequest.obtain(110, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(rateInMillis);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void setInitialAttachApn(String apn, String protocol, int authType, String username, String password, Message result) {
        RILRequest rr = RILRequest.obtain(111, null);
        riljLog("Set RIL_REQUEST_SET_INITIAL_ATTACH_APN");
        rr.mParcel.writeString(apn);
        rr.mParcel.writeString(protocol);
        rr.mParcel.writeInt(authType);
        rr.mParcel.writeString(username);
        rr.mParcel.writeString(password);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ", apn:" + apn + ", protocol:" + protocol + ", authType:" + authType + ", username:" + username + ", password:" + password);
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
        pw.println(" mLastNITZTimeInfo=" + this.mLastNITZTimeInfo);
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
        iccTransmitApduHelper(114, 0, cla, instruction, p1, p2, p3, data, response);
    }

    @Override
    public void iccGetAtr(Message response) {
        RILRequest rr = RILRequest.obtain(511, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
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
    public void activateCLIP(boolean activate, Message response) {
        RILRequest rr = RILRequest.obtain(504, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(activate ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + activate);
        send(rr);
    }

    @Override
    public void queryCOLP(Message response) {
        RILRequest rr = RILRequest.obtain(503, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void activateCOLP(boolean activate, Message response) {
        RILRequest rr = RILRequest.obtain(505, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(activate ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + activate);
        send(rr);
    }

    @Override
    public void queryCOLR(Message response) {
        RILRequest rr = RILRequest.obtain(508, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void activateCOLR(boolean activate, Message response) {
        RILRequest rr = RILRequest.obtain(509, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(activate ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + activate);
        send(rr);
    }

    @Override
    public void queryCNAP(Message response) {
        RILRequest rr = RILRequest.obtain(506, response);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void activateCNAP(boolean activate, Message response) {
        RILRequest rr = RILRequest.obtain(507, response);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(activate ? 1 : 0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + activate);
        send(rr);
    }

    @Override
    public void dialVT(String address, int clirMode, Message result) {
        RILRequest rr = RILRequest.obtain(500, result);
        rr.mParcel.writeString(address);
        rr.mParcel.writeInt(clirMode);
        rr.mParcel.writeInt(0);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void hangupVTConnection(int reason, Message result) {
        riljLog("hangupVTConnection: reason=" + reason);
        RILRequest rr = RILRequest.obtain(501, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + reason);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(reason);
        send(rr);
    }

    @Override
    public void getSIMLockInfo(Message result) {
        RILRequest rr = RILRequest.obtain(510, result);
        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }
}
