package com.android.internal.telephony;

import android.R;
import android.content.Context;
import android.hardware.radio.V1_0.ApnTypes;
import android.hardware.radio.V1_0.Carrier;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.CdmaSmsAck;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.CdmaSmsSubaddress;
import android.hardware.radio.V1_0.CdmaSmsWriteArgs;
import android.hardware.radio.V1_0.CellInfoCdma;
import android.hardware.radio.V1_0.CellInfoGsm;
import android.hardware.radio.V1_0.CellInfoLte;
import android.hardware.radio.V1_0.CellInfoWcdma;
import android.hardware.radio.V1_0.DataProfileInfo;
import android.hardware.radio.V1_0.Dial;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.GsmSmsMessage;
import android.hardware.radio.V1_0.HardwareConfigModem;
import android.hardware.radio.V1_0.IRadio;
import android.hardware.radio.V1_0.IccIo;
import android.hardware.radio.V1_0.ImsSmsMessage;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.NvWriteItem;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SelectUiccSub;
import android.hardware.radio.V1_0.SimApdu;
import android.hardware.radio.V1_0.SmsWriteArgs;
import android.hardware.radio.V1_0.UusInfo;
import android.hardware.radio.V1_1.KeepaliveRequest;
import android.hardware.radio.V1_1.RadioAccessSpecifier;
import android.hardware.radio.deprecated.V1_0.IOemHook;
import android.net.ConnectivityManager;
import android.net.KeepalivePacketData;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.telephony.CellIdentityCdma;
import android.telephony.CellInfo;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.ClientRequestStats;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.ModemActivityInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneNumberUtils;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyHistogram;
import android.telephony.data.DataProfile;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.cat.ComprehensionTlv;
import com.android.internal.telephony.cat.ComprehensionTlvTag;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.dataconnection.KeepaliveStatus;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.nano.TelephonyProto;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.util.Preconditions;
import com.google.android.mms.pdu.CharacterSets;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class RIL extends BaseCommands implements CommandsInterface {
    private static final int DEFAULT_ACK_WAKE_LOCK_TIMEOUT_MS = 200;
    protected static final int DEFAULT_BLOCKING_MESSAGE_RESPONSE_TIMEOUT_MS = 2000;
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT_MS = 60000;
    static final String EMPTY_ALPHA_LONG = "";
    static final String EMPTY_ALPHA_SHORT = "";
    static final int EVENT_ACK_WAKE_LOCK_TIMEOUT = 4;
    protected static final int EVENT_BLOCKING_RESPONSE_TIMEOUT = 5;
    protected static final int EVENT_RADIO_PROXY_DEAD = 6;
    static final int EVENT_WAKE_LOCK_TIMEOUT = 2;
    public static final int FOR_ACK_WAKELOCK = 1;
    public static final int FOR_WAKELOCK = 0;
    public static final int INVALID_WAKELOCK = -1;
    protected static final int IRADIO_GET_SERVICE_DELAY_MILLIS = 4000;
    static final String RILJ_ACK_WAKELOCK_NAME = "RILJ_ACK_WL";
    public static final boolean RILJ_LOGD = true;
    public static final boolean RILJ_LOGV = false;
    static final String RILJ_LOG_TAG = "RILJ";
    static final String RILJ_WAKELOCK_TAG = "*telephony-radio*";
    static final int RIL_HISTOGRAM_BUCKET_COUNT = 5;
    final PowerManager.WakeLock mAckWakeLock;
    final int mAckWakeLockTimeout;
    volatile int mAckWlSequenceNum;
    private WorkSource mActiveWakelockWorkSource;
    private final ClientWakelockTracker mClientWakelockTracker;
    protected boolean mIsMobileNetworkSupported;
    Object[] mLastNITZTimeInfo;
    private TelephonyMetrics mMetrics;
    protected OemHookIndication mOemHookIndication;
    protected volatile IOemHook mOemHookProxy;
    protected OemHookResponse mOemHookResponse;
    protected final Integer mPhoneId;
    protected WorkSource mRILDefaultWorkSource;
    protected RadioIndication mRadioIndication;
    volatile IRadio mRadioProxy;
    protected final AtomicLong mRadioProxyCookie;
    protected final RadioProxyDeathRecipient mRadioProxyDeathRecipient;
    protected RadioResponse mRadioResponse;
    public SparseArray<RILRequest> mRequestList;
    protected final RilHandler mRilHandler;
    public AtomicBoolean mTestingEmergencyCall;
    final PowerManager.WakeLock mWakeLock;
    int mWakeLockCount;
    final int mWakeLockTimeout;
    volatile int mWlSequenceNum;
    static SparseArray<TelephonyHistogram> mRilTimeHistograms = new SparseArray<>();
    protected static final String[] HIDL_SERVICE_NAME = {"slot1", "slot2", "slot3"};

    public static List<TelephonyHistogram> getTelephonyRILTimingHistograms() {
        ArrayList arrayList;
        synchronized (mRilTimeHistograms) {
            arrayList = new ArrayList(mRilTimeHistograms.size());
            for (int i = 0; i < mRilTimeHistograms.size(); i++) {
                arrayList.add(new TelephonyHistogram(mRilTimeHistograms.valueAt(i)));
            }
        }
        return arrayList;
    }

    @VisibleForTesting
    public class RilHandler extends Handler {
        public RilHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 2) {
                synchronized (RIL.this.mRequestList) {
                    if (message.arg1 == RIL.this.mWlSequenceNum) {
                        if (RIL.this.clearWakeLock(0)) {
                            int size = RIL.this.mRequestList.size();
                            Rlog.d(RIL.RILJ_LOG_TAG, "WAKE_LOCK_TIMEOUT  mRequestList=" + size);
                            for (int i2 = 0; i2 < size; i2++) {
                                RILRequest rILRequestValueAt = RIL.this.mRequestList.valueAt(i2);
                                Rlog.d(RIL.RILJ_LOG_TAG, i2 + ": [" + rILRequestValueAt.mSerial + "] " + RIL.requestToString(rILRequestValueAt.mRequest));
                            }
                        }
                    }
                }
                return;
            }
            switch (i) {
                case 4:
                    if (message.arg1 == RIL.this.mAckWlSequenceNum) {
                        RIL.this.clearWakeLock(1);
                        return;
                    }
                    return;
                case 5:
                    RILRequest rILRequestFindAndRemoveRequestFromList = RIL.this.findAndRemoveRequestFromList(message.arg1);
                    if (rILRequestFindAndRemoveRequestFromList != null) {
                        if (rILRequestFindAndRemoveRequestFromList.mResult != null) {
                            AsyncResult.forMessage(rILRequestFindAndRemoveRequestFromList.mResult, RIL.getResponseForTimedOutRILRequest(rILRequestFindAndRemoveRequestFromList), (Throwable) null);
                            rILRequestFindAndRemoveRequestFromList.mResult.sendToTarget();
                            RIL.this.mMetrics.writeOnRilTimeoutResponse(RIL.this.mPhoneId.intValue(), rILRequestFindAndRemoveRequestFromList.mSerial, rILRequestFindAndRemoveRequestFromList.mRequest);
                        }
                        RIL.this.decrementWakeLock(rILRequestFindAndRemoveRequestFromList);
                        rILRequestFindAndRemoveRequestFromList.release();
                        return;
                    }
                    return;
                case 6:
                    RIL.this.riljLog("handleMessage: EVENT_RADIO_PROXY_DEAD cookie = " + message.obj + " mRadioProxyCookie = " + RIL.this.mRadioProxyCookie.get());
                    if (((Long) message.obj).longValue() == RIL.this.mRadioProxyCookie.get()) {
                        RIL.this.resetProxyAndRequestList();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private static Object getResponseForTimedOutRILRequest(RILRequest rILRequest) {
        if (rILRequest != null && rILRequest.mRequest == 135) {
            return new ModemActivityInfo(0L, 0, 0, new int[5], 0, 0);
        }
        return null;
    }

    final class RadioProxyDeathRecipient implements IHwBinder.DeathRecipient {
        RadioProxyDeathRecipient() {
        }

        @Override
        public void serviceDied(long j) {
            RIL.this.riljLog("serviceDied");
            RIL.this.mRilHandler.sendMessage(RIL.this.mRilHandler.obtainMessage(6, Long.valueOf(j)));
        }
    }

    protected void resetProxyAndRequestList() {
        this.mRadioProxy = null;
        this.mOemHookProxy = null;
        this.mRadioProxyCookie.incrementAndGet();
        setRadioState(CommandsInterface.RadioState.RADIO_UNAVAILABLE);
        RILRequest.resetSerial();
        clearRequestList(1, false);
        getRadioProxy(null);
        getOemHookProxy(null);
    }

    @VisibleForTesting
    public IRadio getRadioProxy(Message message) {
        if (!this.mIsMobileNetworkSupported) {
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(1));
                message.sendToTarget();
            }
            return null;
        }
        if (this.mRadioProxy != null) {
            return this.mRadioProxy;
        }
        try {
            this.mRadioProxy = IRadio.getService(HIDL_SERVICE_NAME[this.mPhoneId == null ? 0 : this.mPhoneId.intValue()], true);
            if (this.mRadioProxy != null) {
                this.mRadioProxy.linkToDeath(this.mRadioProxyDeathRecipient, this.mRadioProxyCookie.incrementAndGet());
                this.mRadioProxy.setResponseFunctions(this.mRadioResponse, this.mRadioIndication);
            } else {
                riljLoge("getRadioProxy: mRadioProxy == null");
            }
        } catch (RemoteException | RuntimeException e) {
            this.mRadioProxy = null;
            riljLoge("RadioProxy getService/setResponseFunctions: " + e);
        }
        if (this.mRadioProxy == null) {
            riljLoge("getRadioProxy: mRadioProxy == null");
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(1));
                message.sendToTarget();
            }
        }
        return this.mRadioProxy;
    }

    @VisibleForTesting
    public IOemHook getOemHookProxy(Message message) {
        if (!this.mIsMobileNetworkSupported) {
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(1));
                message.sendToTarget();
            }
            return null;
        }
        if (this.mOemHookProxy != null) {
            return this.mOemHookProxy;
        }
        try {
            this.mOemHookProxy = IOemHook.getService(HIDL_SERVICE_NAME[this.mPhoneId == null ? 0 : this.mPhoneId.intValue()], true);
            if (this.mOemHookProxy != null) {
                this.mOemHookProxy.setResponseFunctions(this.mOemHookResponse, this.mOemHookIndication);
            } else {
                riljLoge("getOemHookProxy: mOemHookProxy == null");
            }
        } catch (RemoteException | RuntimeException e) {
            this.mOemHookProxy = null;
            riljLoge("OemHookProxy getService/setResponseFunctions: " + e);
        }
        if (this.mOemHookProxy == null && message != null) {
            AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(1));
            message.sendToTarget();
        }
        return this.mOemHookProxy;
    }

    @VisibleForTesting
    public RIL() {
        super(null);
        this.mClientWakelockTracker = new ClientWakelockTracker();
        this.mWlSequenceNum = 0;
        this.mAckWlSequenceNum = 0;
        this.mRequestList = new SparseArray<>();
        this.mTestingEmergencyCall = new AtomicBoolean(false);
        this.mMetrics = TelephonyMetrics.getInstance();
        this.mRadioProxy = null;
        this.mOemHookProxy = null;
        this.mRadioProxyCookie = new AtomicLong(0L);
        this.mAckWakeLock = null;
        this.mWakeLock = null;
        this.mRadioProxyDeathRecipient = null;
        this.mRilHandler = null;
        this.mPhoneId = 0;
        this.mAckWakeLockTimeout = 0;
        this.mWakeLockTimeout = 0;
    }

    public RIL(Context context, int i, int i2) {
        this(context, i, i2, null);
    }

    public RIL(Context context, int i, int i2, Integer num) {
        super(context);
        this.mClientWakelockTracker = new ClientWakelockTracker();
        this.mWlSequenceNum = 0;
        this.mAckWlSequenceNum = 0;
        this.mRequestList = new SparseArray<>();
        this.mTestingEmergencyCall = new AtomicBoolean(false);
        this.mMetrics = TelephonyMetrics.getInstance();
        this.mRadioProxy = null;
        this.mOemHookProxy = null;
        this.mRadioProxyCookie = new AtomicLong(0L);
        riljLog("RIL: init preferredNetworkType=" + i + " cdmaSubscription=" + i2 + ")");
        this.mContext = context;
        this.mCdmaSubscription = i2;
        this.mPreferredNetworkType = i;
        this.mPhoneType = 0;
        this.mPhoneId = num;
        this.mIsMobileNetworkSupported = ((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0);
        this.mRadioResponse = new RadioResponse(this);
        this.mRadioIndication = new RadioIndication(this);
        this.mOemHookResponse = new OemHookResponse(this);
        this.mOemHookIndication = new OemHookIndication(this);
        this.mRilHandler = new RilHandler();
        this.mRadioProxyDeathRecipient = new RadioProxyDeathRecipient();
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, RILJ_WAKELOCK_TAG);
        this.mWakeLock.setReferenceCounted(false);
        this.mAckWakeLock = powerManager.newWakeLock(1, RILJ_ACK_WAKELOCK_NAME);
        this.mAckWakeLock.setReferenceCounted(false);
        this.mWakeLockTimeout = SystemProperties.getInt("ro.ril.wake_lock_timeout", 60000);
        this.mAckWakeLockTimeout = SystemProperties.getInt("ro.ril.wake_lock_timeout", 200);
        this.mWakeLockCount = 0;
        this.mRILDefaultWorkSource = new WorkSource(context.getApplicationInfo().uid, context.getPackageName());
        TelephonyDevController.getInstance().registerRIL(this);
        getRadioProxy(null);
        getOemHookProxy(null);
    }

    @Override
    public void setOnNITZTime(Handler handler, int i, Object obj) {
        super.setOnNITZTime(handler, i, obj);
        if (this.mLastNITZTimeInfo != null) {
            this.mNITZTimeRegistrant.notifyRegistrant(new AsyncResult((Object) null, this.mLastNITZTimeInfo, (Throwable) null));
        }
    }

    private void addRequest(RILRequest rILRequest) {
        acquireWakeLock(rILRequest, 0);
        synchronized (this.mRequestList) {
            rILRequest.mStartTimeMs = SystemClock.elapsedRealtime();
            this.mRequestList.append(rILRequest.mSerial, rILRequest);
        }
    }

    protected RILRequest obtainRequest(int i, Message message, WorkSource workSource) {
        RILRequest rILRequestObtain = RILRequest.obtain(i, message, workSource);
        addRequest(rILRequestObtain);
        return rILRequestObtain;
    }

    protected void handleRadioProxyExceptionForRR(RILRequest rILRequest, String str, Exception exc) {
        riljLoge(str + ": " + exc);
        resetProxyAndRequestList();
    }

    protected String convertNullToEmptyString(String str) {
        return str != null ? str : "";
    }

    @Override
    public void getIccCardStatus(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(1, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getIccCardStatus(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getIccCardStatus", e);
            }
        }
    }

    @Override
    public void getIccSlotsStatus(Message message) {
        if (message != null) {
            AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(6));
            message.sendToTarget();
        }
    }

    @Override
    public void setLogicalToPhysicalSlotMapping(int[] iArr, Message message) {
        if (message != null) {
            AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(6));
            message.sendToTarget();
        }
    }

    @Override
    public void supplyIccPin(String str, Message message) {
        supplyIccPinForApp(str, null, message);
    }

    @Override
    public void supplyIccPinForApp(String str, String str2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " aid = " + str2);
            try {
                radioProxy.supplyIccPinForApp(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), convertNullToEmptyString(str2));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "supplyIccPinForApp", e);
            }
        }
    }

    @Override
    public void supplyIccPuk(String str, String str2, Message message) {
        supplyIccPukForApp(str, str2, null, message);
    }

    @Override
    public void supplyIccPukForApp(String str, String str2, String str3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(3, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " aid = " + str3);
            try {
                radioProxy.supplyIccPukForApp(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), convertNullToEmptyString(str2), convertNullToEmptyString(str3));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "supplyIccPukForApp", e);
            }
        }
    }

    @Override
    public void supplyIccPin2(String str, Message message) {
        supplyIccPin2ForApp(str, null, message);
    }

    @Override
    public void supplyIccPin2ForApp(String str, String str2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(4, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " aid = " + str2);
            try {
                radioProxy.supplyIccPin2ForApp(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), convertNullToEmptyString(str2));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "supplyIccPin2ForApp", e);
            }
        }
    }

    @Override
    public void supplyIccPuk2(String str, String str2, Message message) {
        supplyIccPuk2ForApp(str, str2, null, message);
    }

    @Override
    public void supplyIccPuk2ForApp(String str, String str2, String str3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(5, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " aid = " + str3);
            try {
                radioProxy.supplyIccPuk2ForApp(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), convertNullToEmptyString(str2), convertNullToEmptyString(str3));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "supplyIccPuk2ForApp", e);
            }
        }
    }

    @Override
    public void changeIccPin(String str, String str2, Message message) {
        changeIccPinForApp(str, str2, null, message);
    }

    @Override
    public void changeIccPinForApp(String str, String str2, String str3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(6, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " oldPin = " + str + " newPin = " + str2 + " aid = " + str3);
            try {
                radioProxy.changeIccPinForApp(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), convertNullToEmptyString(str2), convertNullToEmptyString(str3));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "changeIccPinForApp", e);
            }
        }
    }

    @Override
    public void changeIccPin2(String str, String str2, Message message) {
        changeIccPin2ForApp(str, str2, null, message);
    }

    @Override
    public void changeIccPin2ForApp(String str, String str2, String str3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(7, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " oldPin = " + str + " newPin = " + str2 + " aid = " + str3);
            try {
                radioProxy.changeIccPin2ForApp(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), convertNullToEmptyString(str2), convertNullToEmptyString(str3));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "changeIccPin2ForApp", e);
            }
        }
    }

    @Override
    public void supplyNetworkDepersonalization(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(8, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " netpin = " + str);
            try {
                radioProxy.supplyNetworkDepersonalization(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "supplyNetworkDepersonalization", e);
            }
        }
    }

    @Override
    public void getCurrentCalls(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(9, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getCurrentCalls(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getCurrentCalls", e);
            }
        }
    }

    @Override
    public void dial(String str, int i, Message message) {
        dial(str, i, null, message);
    }

    @Override
    public void dial(String str, int i, UUSInfo uUSInfo, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(10, message, this.mRILDefaultWorkSource);
            Dial dial = new Dial();
            dial.address = convertNullToEmptyString(str);
            dial.clir = i;
            if (uUSInfo != null) {
                UusInfo uusInfo = new UusInfo();
                uusInfo.uusType = uUSInfo.getType();
                uusInfo.uusDcs = uUSInfo.getDcs();
                uusInfo.uusData = new String(uUSInfo.getUserData());
                dial.uusInfo.add(uusInfo);
            }
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.dial(rILRequestObtainRequest.mSerial, dial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "dial", e);
            }
        }
    }

    @Override
    public void getIMSI(Message message) {
        getIMSIForApp(null, message);
    }

    @Override
    public void getIMSIForApp(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(11, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + ">  " + requestToString(rILRequestObtainRequest.mRequest) + " aid = " + str);
            try {
                radioProxy.getImsiForApp(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getIMSIForApp", e);
            }
        }
    }

    @Override
    public void hangupConnection(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(12, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " gsmIndex = " + i);
            try {
                radioProxy.hangup(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "hangupConnection", e);
            }
        }
    }

    @Override
    public void hangupWaitingOrBackground(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(13, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.hangupWaitingOrBackground(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "hangupWaitingOrBackground", e);
            }
        }
    }

    @Override
    public void hangupForegroundResumeBackground(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(14, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.hangupForegroundResumeBackground(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "hangupForegroundResumeBackground", e);
            }
        }
    }

    @Override
    public void switchWaitingOrHoldingAndActive(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(15, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.switchWaitingOrHoldingAndActive(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "switchWaitingOrHoldingAndActive", e);
            }
        }
    }

    @Override
    public void conference(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(16, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.conference(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "conference", e);
            }
        }
    }

    @Override
    public void rejectCall(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(17, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.rejectCall(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "rejectCall", e);
            }
        }
    }

    @Override
    public void getLastCallFailCause(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(18, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getLastCallFailCause(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getLastCallFailCause", e);
            }
        }
    }

    @Override
    public void getSignalStrength(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(19, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getSignalStrength(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getSignalStrength", e);
            }
        }
    }

    @Override
    public void getVoiceRegistrationState(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(20, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getVoiceRegistrationState(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getVoiceRegistrationState", e);
            }
        }
    }

    @Override
    public void getDataRegistrationState(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(21, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getDataRegistrationState(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getDataRegistrationState", e);
            }
        }
    }

    @Override
    public void getOperator(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(22, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getOperator(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getOperator", e);
            }
        }
    }

    @Override
    public void setRadioPower(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(23, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " on = " + z);
            try {
                radioProxy.setRadioPower(rILRequestObtainRequest.mSerial, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setRadioPower", e);
            }
        }
    }

    @Override
    public void sendDtmf(char c, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(24, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.sendDtmf(rILRequestObtainRequest.mSerial, c + "");
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendDtmf", e);
            }
        }
    }

    private GsmSmsMessage constructGsmSendSmsRilRequest(String str, String str2) {
        GsmSmsMessage gsmSmsMessage = new GsmSmsMessage();
        if (str == null) {
            str = "";
        }
        gsmSmsMessage.smscPdu = str;
        if (str2 == null) {
            str2 = "";
        }
        gsmSmsMessage.pdu = str2;
        return gsmSmsMessage;
    }

    @Override
    public void sendSMS(String str, String str2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(25, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.sendSms(rILRequestObtainRequest.mSerial, constructGsmSendSmsRilRequest(str, str2));
                this.mMetrics.writeRilSendSms(this.mPhoneId.intValue(), rILRequestObtainRequest.mSerial, 1, 1);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendSMS", e);
            }
        }
    }

    @Override
    public void sendSMSExpectMore(String str, String str2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(26, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.sendSMSExpectMore(rILRequestObtainRequest.mSerial, constructGsmSendSmsRilRequest(str, str2));
                this.mMetrics.writeRilSendSms(this.mPhoneId.intValue(), rILRequestObtainRequest.mSerial, 1, 1);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendSMSExpectMore", e);
            }
        }
    }

    private static int convertToHalMvnoType(String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != 102338) {
            if (iHashCode != 114097) {
                b = (iHashCode == 3236474 && str.equals("imsi")) ? (byte) 0 : (byte) -1;
            } else if (str.equals("spn")) {
                b = 2;
            }
        } else if (str.equals("gid")) {
            b = 1;
        }
        switch (b) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            default:
                return 0;
        }
    }

    private static DataProfileInfo convertToHalDataProfile(DataProfile dataProfile) {
        DataProfileInfo dataProfileInfo = new DataProfileInfo();
        dataProfileInfo.profileId = dataProfile.getProfileId();
        dataProfileInfo.apn = dataProfile.getApn();
        dataProfileInfo.protocol = dataProfile.getProtocol();
        dataProfileInfo.roamingProtocol = dataProfile.getRoamingProtocol();
        dataProfileInfo.authType = dataProfile.getAuthType();
        dataProfileInfo.user = dataProfile.getUserName();
        dataProfileInfo.password = dataProfile.getPassword();
        dataProfileInfo.type = dataProfile.getType();
        dataProfileInfo.maxConnsTime = dataProfile.getMaxConnsTime();
        dataProfileInfo.maxConns = dataProfile.getMaxConns();
        dataProfileInfo.waitTime = dataProfile.getWaitTime();
        dataProfileInfo.enabled = dataProfile.isEnabled();
        dataProfileInfo.supportedApnTypesBitmap = dataProfile.getSupportedApnTypesBitmap();
        dataProfileInfo.bearerBitmap = dataProfile.getBearerBitmap();
        dataProfileInfo.mtu = dataProfile.getMtu();
        dataProfileInfo.mvnoType = convertToHalMvnoType(dataProfile.getMvnoType());
        dataProfileInfo.mvnoMatchData = dataProfile.getMvnoMatchData();
        return dataProfileInfo;
    }

    private static int convertToHalResetNvType(int i) {
        switch (i) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            default:
                return -1;
        }
    }

    @Override
    public void setupDataCall(int i, DataProfile dataProfile, boolean z, boolean z2, int i2, LinkProperties linkProperties, Message message) {
        ServiceState serviceState;
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(27, message, this.mRILDefaultWorkSource);
            DataProfileInfo dataProfileInfoConvertToHalDataProfile = convertToHalDataProfile(dataProfile);
            android.hardware.radio.V1_2.IRadio iRadioCastFrom = android.hardware.radio.V1_2.IRadio.castFrom((IHwInterface) radioProxy);
            try {
                if (iRadioCastFrom == null) {
                    int rilDataRadioTechnology = 0;
                    Phone phone = PhoneFactory.getPhone(this.mPhoneId.intValue());
                    if (phone != null && (serviceState = phone.getServiceState()) != null) {
                        rilDataRadioTechnology = serviceState.getRilDataRadioTechnology();
                    }
                    int i3 = rilDataRadioTechnology;
                    riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ",dataRat=" + i3 + ",isRoaming=" + z + ",allowRoaming=" + z2 + "," + dataProfile);
                    radioProxy.setupDataCall(rILRequestObtainRequest.mSerial, i3, dataProfileInfoConvertToHalDataProfile, dataProfile.isModemCognitive(), z2, z);
                    return;
                }
                ArrayList<String> arrayList = new ArrayList<>();
                ArrayList<String> arrayList2 = new ArrayList<>();
                if (linkProperties != null) {
                    Iterator it = linkProperties.getAddresses().iterator();
                    while (it.hasNext()) {
                        arrayList.add(((InetAddress) it.next()).getHostAddress());
                    }
                    Iterator<InetAddress> it2 = linkProperties.getDnsServers().iterator();
                    while (it2.hasNext()) {
                        arrayList2.add(it2.next().getHostAddress());
                    }
                }
                riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ",accessNetworkType=" + i + ",isRoaming=" + z + ",allowRoaming=" + z2 + "," + dataProfile + ",addresses=" + arrayList + ",dnses=" + arrayList2);
                iRadioCastFrom.setupDataCall_1_2(rILRequestObtainRequest.mSerial, i, dataProfileInfoConvertToHalDataProfile, dataProfile.isModemCognitive(), z2, z, i2, arrayList, arrayList2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setupDataCall", e);
            }
        }
    }

    @Override
    public void iccIO(int i, int i2, String str, int i3, int i4, int i5, String str2, String str3, Message message) {
        iccIOForApp(i, i2, str, i3, i4, i5, str2, str3, null, message);
    }

    @Override
    public void iccIOForApp(int i, int i2, String str, int i3, int i4, int i5, String str2, String str3, String str4, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(28, message, this.mRILDefaultWorkSource);
            if (Build.IS_DEBUGGABLE) {
                riljLog(rILRequestObtainRequest.serialString() + "> iccIO: " + requestToString(rILRequestObtainRequest.mRequest) + " command = 0x" + Integer.toHexString(i) + " fileId = 0x" + Integer.toHexString(i2) + " path = " + str + " p1 = " + i3 + " p2 = " + i4 + " p3 =  data = " + str2 + " aid = " + str4);
            } else {
                riljLog(rILRequestObtainRequest.serialString() + "> iccIO: " + requestToString(rILRequestObtainRequest.mRequest));
            }
            IccIo iccIo = new IccIo();
            iccIo.command = i;
            iccIo.fileId = i2;
            iccIo.path = convertNullToEmptyString(str);
            iccIo.p1 = i3;
            iccIo.p2 = i4;
            iccIo.p3 = i5;
            iccIo.data = convertNullToEmptyString(str2);
            iccIo.pin2 = convertNullToEmptyString(str3);
            iccIo.aid = convertNullToEmptyString(str4);
            try {
                radioProxy.iccIOForApp(rILRequestObtainRequest.mSerial, iccIo);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "iccIOForApp", e);
            }
        }
    }

    @Override
    public void sendUSSD(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(29, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " ussd = *******");
            try {
                radioProxy.sendUssd(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendUSSD", e);
            }
        }
    }

    @Override
    public void cancelPendingUssd(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(30, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.cancelPendingUssd(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "cancelPendingUssd", e);
            }
        }
    }

    @Override
    public void getCLIR(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(31, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getClir(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getCLIR", e);
            }
        }
    }

    @Override
    public void setCLIR(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(32, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " clirMode = " + i);
            try {
                radioProxy.setClir(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCLIR", e);
            }
        }
    }

    @Override
    public void queryCallForwardStatus(int i, int i2, String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(33, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " cfreason = " + i + " serviceClass = " + i2);
            android.hardware.radio.V1_0.CallForwardInfo callForwardInfo = new android.hardware.radio.V1_0.CallForwardInfo();
            callForwardInfo.reason = i;
            callForwardInfo.serviceClass = i2;
            callForwardInfo.toa = PhoneNumberUtils.toaFromString(str);
            callForwardInfo.number = convertNullToEmptyString(str);
            callForwardInfo.timeSeconds = 0;
            try {
                radioProxy.getCallForwardStatus(rILRequestObtainRequest.mSerial, callForwardInfo);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryCallForwardStatus", e);
            }
        }
    }

    @Override
    public void setCallForward(int i, int i2, int i3, String str, int i4, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(34, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " action = " + i + " cfReason = " + i2 + " serviceClass = " + i3 + " timeSeconds = " + i4);
            android.hardware.radio.V1_0.CallForwardInfo callForwardInfo = new android.hardware.radio.V1_0.CallForwardInfo();
            callForwardInfo.status = i;
            callForwardInfo.reason = i2;
            callForwardInfo.serviceClass = i3;
            callForwardInfo.toa = PhoneNumberUtils.toaFromString(str);
            callForwardInfo.number = convertNullToEmptyString(str);
            callForwardInfo.timeSeconds = i4;
            try {
                radioProxy.setCallForward(rILRequestObtainRequest.mSerial, callForwardInfo);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCallForward", e);
            }
        }
    }

    @Override
    public void queryCallWaiting(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(35, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " serviceClass = " + i);
            try {
                radioProxy.getCallWaiting(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryCallWaiting", e);
            }
        }
    }

    @Override
    public void setCallWaiting(boolean z, int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(36, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " enable = " + z + " serviceClass = " + i);
            try {
                radioProxy.setCallWaiting(rILRequestObtainRequest.mSerial, z, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCallWaiting", e);
            }
        }
    }

    @Override
    public void acknowledgeLastIncomingGsmSms(boolean z, int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(37, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " success = " + z + " cause = " + i);
            try {
                radioProxy.acknowledgeLastIncomingGsmSms(rILRequestObtainRequest.mSerial, z, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "acknowledgeLastIncomingGsmSms", e);
            }
        }
    }

    @Override
    public void acceptCall(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(40, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.acceptCall(rILRequestObtainRequest.mSerial);
                this.mMetrics.writeRilAnswer(this.mPhoneId.intValue(), rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "acceptCall", e);
            }
        }
    }

    @Override
    public void deactivateDataCall(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(41, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " cid = " + i + " reason = " + i2);
            android.hardware.radio.V1_2.IRadio iRadioCastFrom = android.hardware.radio.V1_2.IRadio.castFrom((IHwInterface) radioProxy);
            try {
                if (iRadioCastFrom == null) {
                    radioProxy.deactivateDataCall(rILRequestObtainRequest.mSerial, i, i2 == 2);
                } else {
                    iRadioCastFrom.deactivateDataCall_1_2(rILRequestObtainRequest.mSerial, i, i2);
                }
                this.mMetrics.writeRilDeactivateDataCall(this.mPhoneId.intValue(), rILRequestObtainRequest.mSerial, i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "deactivateDataCall", e);
            }
        }
    }

    @Override
    public void queryFacilityLock(String str, String str2, int i, Message message) {
        queryFacilityLockForApp(str, str2, i, null, message);
    }

    @Override
    public void queryFacilityLockForApp(String str, String str2, int i, String str3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(42, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " facility = " + str + " serviceClass = " + i + " appId = " + str3);
            try {
                radioProxy.getFacilityLockForApp(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), convertNullToEmptyString(str2), i, convertNullToEmptyString(str3));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getFacilityLockForApp", e);
            }
        }
    }

    @Override
    public void setFacilityLock(String str, boolean z, String str2, int i, Message message) {
        setFacilityLockForApp(str, z, str2, i, null, message);
    }

    @Override
    public void setFacilityLockForApp(String str, boolean z, String str2, int i, String str3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(43, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " facility = " + str + " lockstate = " + z + " serviceClass = " + i + " appId = " + str3);
            try {
                radioProxy.setFacilityLockForApp(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), z, convertNullToEmptyString(str2), i, convertNullToEmptyString(str3));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setFacilityLockForApp", e);
            }
        }
    }

    @Override
    public void changeBarringPassword(String str, String str2, String str3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(44, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + "facility = " + str);
            try {
                radioProxy.setBarringPassword(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), convertNullToEmptyString(str2), convertNullToEmptyString(str3));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "changeBarringPassword", e);
            }
        }
    }

    @Override
    public void getNetworkSelectionMode(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(45, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getNetworkSelectionMode(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getNetworkSelectionMode", e);
            }
        }
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(46, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setNetworkSelectionModeAutomatic(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setNetworkSelectionModeAutomatic", e);
            }
        }
    }

    @Override
    public void setNetworkSelectionModeManual(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(47, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " operatorNumeric = " + str);
            try {
                radioProxy.setNetworkSelectionModeManual(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setNetworkSelectionModeManual", e);
            }
        }
    }

    @Override
    public void getAvailableNetworks(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(48, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getAvailableNetworks(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getAvailableNetworks", e);
            }
        }
    }

    private RadioAccessSpecifier convertRadioAccessSpecifierToRadioHAL(android.telephony.RadioAccessSpecifier radioAccessSpecifier) {
        ArrayList<Integer> arrayList;
        RadioAccessSpecifier radioAccessSpecifier2 = new RadioAccessSpecifier();
        radioAccessSpecifier2.radioAccessNetwork = radioAccessSpecifier.getRadioAccessNetwork();
        switch (radioAccessSpecifier.getRadioAccessNetwork()) {
            case 1:
                arrayList = radioAccessSpecifier2.geranBands;
                break;
            case 2:
                arrayList = radioAccessSpecifier2.utranBands;
                break;
            case 3:
                arrayList = radioAccessSpecifier2.eutranBands;
                break;
            default:
                Log.wtf(RILJ_LOG_TAG, "radioAccessNetwork " + radioAccessSpecifier.getRadioAccessNetwork() + " not supported!");
                return null;
        }
        if (radioAccessSpecifier.getBands() != null) {
            for (int i : radioAccessSpecifier.getBands()) {
                arrayList.add(Integer.valueOf(i));
            }
        }
        if (radioAccessSpecifier.getChannels() != null) {
            for (int i2 : radioAccessSpecifier.getChannels()) {
                radioAccessSpecifier2.channels.add(Integer.valueOf(i2));
            }
        }
        return radioAccessSpecifier2;
    }

    @Override
    public void startNetworkScan(NetworkScanRequest networkScanRequest, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            android.hardware.radio.V1_2.IRadio iRadioCastFrom = android.hardware.radio.V1_2.IRadio.castFrom((IHwInterface) radioProxy);
            int i = 0;
            if (iRadioCastFrom != null) {
                android.hardware.radio.V1_2.NetworkScanRequest networkScanRequest2 = new android.hardware.radio.V1_2.NetworkScanRequest();
                networkScanRequest2.type = networkScanRequest.getScanType();
                networkScanRequest2.interval = networkScanRequest.getSearchPeriodicity();
                networkScanRequest2.maxSearchTime = networkScanRequest.getMaxSearchTime();
                networkScanRequest2.incrementalResultsPeriodicity = networkScanRequest.getIncrementalResultsPeriodicity();
                networkScanRequest2.incrementalResults = networkScanRequest.getIncrementalResults();
                android.telephony.RadioAccessSpecifier[] specifiers = networkScanRequest.getSpecifiers();
                int length = specifiers.length;
                while (i < length) {
                    RadioAccessSpecifier radioAccessSpecifierConvertRadioAccessSpecifierToRadioHAL = convertRadioAccessSpecifierToRadioHAL(specifiers[i]);
                    if (radioAccessSpecifierConvertRadioAccessSpecifierToRadioHAL == null) {
                        return;
                    }
                    networkScanRequest2.specifiers.add(radioAccessSpecifierConvertRadioAccessSpecifierToRadioHAL);
                    i++;
                }
                networkScanRequest2.mccMncs.addAll(networkScanRequest.getPlmns());
                RILRequest rILRequestObtainRequest = obtainRequest(142, message, this.mRILDefaultWorkSource);
                riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
                try {
                    iRadioCastFrom.startNetworkScan_1_2(rILRequestObtainRequest.mSerial, networkScanRequest2);
                    return;
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rILRequestObtainRequest, "startNetworkScan", e);
                    return;
                }
            }
            android.hardware.radio.V1_1.IRadio iRadioCastFrom2 = android.hardware.radio.V1_1.IRadio.castFrom((IHwInterface) radioProxy);
            if (iRadioCastFrom2 == null) {
                if (message != null) {
                    AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(6));
                    message.sendToTarget();
                    return;
                }
                return;
            }
            android.hardware.radio.V1_1.NetworkScanRequest networkScanRequest3 = new android.hardware.radio.V1_1.NetworkScanRequest();
            networkScanRequest3.type = networkScanRequest.getScanType();
            networkScanRequest3.interval = networkScanRequest.getSearchPeriodicity();
            android.telephony.RadioAccessSpecifier[] specifiers2 = networkScanRequest.getSpecifiers();
            int length2 = specifiers2.length;
            while (i < length2) {
                RadioAccessSpecifier radioAccessSpecifierConvertRadioAccessSpecifierToRadioHAL2 = convertRadioAccessSpecifierToRadioHAL(specifiers2[i]);
                if (radioAccessSpecifierConvertRadioAccessSpecifierToRadioHAL2 == null) {
                    return;
                }
                networkScanRequest3.specifiers.add(radioAccessSpecifierConvertRadioAccessSpecifierToRadioHAL2);
                i++;
            }
            RILRequest rILRequestObtainRequest2 = obtainRequest(142, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest2.serialString() + "> " + requestToString(rILRequestObtainRequest2.mRequest));
            try {
                iRadioCastFrom2.startNetworkScan(rILRequestObtainRequest2.mSerial, networkScanRequest3);
            } catch (RemoteException | RuntimeException e2) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest2, "startNetworkScan", e2);
            }
        }
    }

    @Override
    public void stopNetworkScan(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            android.hardware.radio.V1_1.IRadio iRadioCastFrom = android.hardware.radio.V1_1.IRadio.castFrom((IHwInterface) radioProxy);
            if (iRadioCastFrom == null) {
                if (message != null) {
                    AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(6));
                    message.sendToTarget();
                    return;
                }
                return;
            }
            RILRequest rILRequestObtainRequest = obtainRequest(143, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                iRadioCastFrom.stopNetworkScan(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "stopNetworkScan", e);
            }
        }
    }

    @Override
    public void startDtmf(char c, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(49, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.startDtmf(rILRequestObtainRequest.mSerial, c + "");
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "startDtmf", e);
            }
        }
    }

    @Override
    public void stopDtmf(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(50, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.stopDtmf(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "stopDtmf", e);
            }
        }
    }

    @Override
    public void separateConnection(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(52, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " gsmIndex = " + i);
            try {
                radioProxy.separateConnection(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "separateConnection", e);
            }
        }
    }

    @Override
    public void getBasebandVersion(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(51, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getBasebandVersion(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getBasebandVersion", e);
            }
        }
    }

    @Override
    public void setMute(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(53, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " enableMute = " + z);
            try {
                radioProxy.setMute(rILRequestObtainRequest.mSerial, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setMute", e);
            }
        }
    }

    @Override
    public void getMute(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(54, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getMute(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getMute", e);
            }
        }
    }

    @Override
    public void queryCLIP(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(55, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getClip(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryCLIP", e);
            }
        }
    }

    @Override
    @Deprecated
    public void getPDPContextList(Message message) {
        getDataCallList(message);
    }

    @Override
    public void getDataCallList(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(57, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getDataCallList(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getDataCallList", e);
            }
        }
    }

    @Override
    public void invokeOemRilRequestRaw(byte[] bArr, Message message) {
        IOemHook oemHookProxy = getOemHookProxy(message);
        if (oemHookProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(59, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + "[" + IccUtils.bytesToHexString(bArr) + "]");
            try {
                oemHookProxy.sendRequestRaw(rILRequestObtainRequest.mSerial, primitiveArrayToArrayList(bArr));
                return;
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "invokeOemRilRequestRaw", e);
                return;
            }
        }
        riljLog("Radio Oem Hook Service is disabled for P and later devices. ");
    }

    @Override
    public void invokeOemRilRequestStrings(String[] strArr, Message message) {
        IOemHook oemHookProxy = getOemHookProxy(message);
        if (oemHookProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(60, message, this.mRILDefaultWorkSource);
            String str = "";
            for (String str2 : strArr) {
                str = str + str2 + " ";
            }
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " strings = " + str);
            try {
                oemHookProxy.sendRequestStrings(rILRequestObtainRequest.mSerial, new ArrayList<>(Arrays.asList(strArr)));
                return;
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "invokeOemRilRequestStrings", e);
                return;
            }
        }
        riljLog("Radio Oem Hook Service is disabled for P and later devices. ");
    }

    @Override
    public void setSuppServiceNotifications(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(62, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " enable = " + z);
            try {
                radioProxy.setSuppServiceNotifications(rILRequestObtainRequest.mSerial, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setSuppServiceNotifications", e);
            }
        }
    }

    @Override
    public void writeSmsToSim(int i, String str, String str2, Message message) {
        int iTranslateStatus = translateStatus(i);
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(63, message, this.mRILDefaultWorkSource);
            SmsWriteArgs smsWriteArgs = new SmsWriteArgs();
            smsWriteArgs.status = iTranslateStatus;
            smsWriteArgs.smsc = convertNullToEmptyString(str);
            smsWriteArgs.pdu = convertNullToEmptyString(str2);
            try {
                radioProxy.writeSmsToSim(rILRequestObtainRequest.mSerial, smsWriteArgs);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "writeSmsToSim", e);
            }
        }
    }

    @Override
    public void deleteSmsOnSim(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(64, message, this.mRILDefaultWorkSource);
            try {
                radioProxy.deleteSmsOnSim(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "deleteSmsOnSim", e);
            }
        }
    }

    @Override
    public void setBandMode(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(65, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " bandMode = " + i);
            try {
                radioProxy.setBandMode(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setBandMode", e);
            }
        }
    }

    @Override
    public void queryAvailableBandMode(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(66, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getAvailableBandModes(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryAvailableBandMode", e);
            }
        }
    }

    @Override
    public void sendEnvelope(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(69, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " contents = " + str);
            try {
                radioProxy.sendEnvelope(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendEnvelope", e);
            }
        }
    }

    @Override
    public void sendTerminalResponse(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(70, message, this.mRILDefaultWorkSource);
            StringBuilder sb = new StringBuilder();
            sb.append(rILRequestObtainRequest.serialString());
            sb.append("> ");
            sb.append(requestToString(rILRequestObtainRequest.mRequest));
            sb.append(" contents = ");
            sb.append(Build.IS_DEBUGGABLE ? str : censoredTerminalResponse(str));
            riljLog(sb.toString());
            try {
                radioProxy.sendTerminalResponseToSim(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendTerminalResponse", e);
            }
        }
    }

    private String censoredTerminalResponse(String str) {
        try {
            byte[] bArrHexStringToBytes = IccUtils.hexStringToBytes(str);
            if (bArrHexStringToBytes != null) {
                int valueIndex = 0;
                for (ComprehensionTlv comprehensionTlv : ComprehensionTlv.decodeMany(bArrHexStringToBytes, 0)) {
                    if (ComprehensionTlvTag.TEXT_STRING.value() == comprehensionTlv.getTag()) {
                        str = str.toLowerCase().replace(IccUtils.bytesToHexString(Arrays.copyOfRange(comprehensionTlv.getRawValue(), valueIndex, comprehensionTlv.getValueIndex() + comprehensionTlv.getLength())), "********");
                    }
                    valueIndex = comprehensionTlv.getValueIndex() + comprehensionTlv.getLength();
                }
                return str;
            }
            return str;
        } catch (Exception e) {
            Rlog.e(RILJ_LOG_TAG, "Could not censor the terminal response: " + e);
            return null;
        }
    }

    @Override
    public void sendEnvelopeWithStatus(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(107, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " contents = " + str);
            try {
                radioProxy.sendEnvelopeWithStatus(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendEnvelopeWithStatus", e);
            }
        }
    }

    @Override
    public void explicitCallTransfer(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(72, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.explicitCallTransfer(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "explicitCallTransfer", e);
            }
        }
    }

    @Override
    public void setPreferredNetworkType(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(73, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " networkType = " + i);
            this.mPreferredNetworkType = i;
            this.mMetrics.writeSetPreferredNetworkType(this.mPhoneId.intValue(), i);
            try {
                radioProxy.setPreferredNetworkType(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setPreferredNetworkType", e);
            }
        }
    }

    @Override
    public void getPreferredNetworkType(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(74, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getPreferredNetworkType(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getPreferredNetworkType", e);
            }
        }
    }

    @Override
    public void getNeighboringCids(Message message, WorkSource workSource) {
        WorkSource deafultWorkSourceIfInvalid = getDeafultWorkSourceIfInvalid(workSource);
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(75, message, deafultWorkSourceIfInvalid);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getNeighboringCids(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getNeighboringCids", e);
            }
        }
    }

    @Override
    public void setLocationUpdates(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(76, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " enable = " + z);
            try {
                radioProxy.setLocationUpdates(rILRequestObtainRequest.mSerial, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setLocationUpdates", e);
            }
        }
    }

    @Override
    public void setCdmaSubscriptionSource(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(77, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " cdmaSubscription = " + i);
            try {
                radioProxy.setCdmaSubscriptionSource(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCdmaSubscriptionSource", e);
            }
        }
    }

    @Override
    public void queryCdmaRoamingPreference(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(79, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getCdmaRoamingPreference(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryCdmaRoamingPreference", e);
            }
        }
    }

    @Override
    public void setCdmaRoamingPreference(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(78, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " cdmaRoamingType = " + i);
            try {
                radioProxy.setCdmaRoamingPreference(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCdmaRoamingPreference", e);
            }
        }
    }

    @Override
    public void queryTTYMode(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(81, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getTTYMode(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryTTYMode", e);
            }
        }
    }

    @Override
    public void setTTYMode(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(80, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " ttyMode = " + i);
            try {
                radioProxy.setTTYMode(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setTTYMode", e);
            }
        }
    }

    @Override
    public void setPreferredVoicePrivacy(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(82, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " enable = " + z);
            try {
                radioProxy.setPreferredVoicePrivacy(rILRequestObtainRequest.mSerial, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setPreferredVoicePrivacy", e);
            }
        }
    }

    @Override
    public void getPreferredVoicePrivacy(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(83, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getPreferredVoicePrivacy(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getPreferredVoicePrivacy", e);
            }
        }
    }

    @Override
    public void sendCDMAFeatureCode(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(84, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " featureCode = " + str);
            try {
                radioProxy.sendCDMAFeatureCode(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendCDMAFeatureCode", e);
            }
        }
    }

    @Override
    public void sendBurstDtmf(String str, int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(85, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " dtmfString = " + str + " on = " + i + " off = " + i2);
            try {
                radioProxy.sendBurstDtmf(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendBurstDtmf", e);
            }
        }
    }

    protected void constructCdmaSendSmsRilRequest(CdmaSmsMessage cdmaSmsMessage, byte[] bArr) {
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
        try {
            cdmaSmsMessage.teleserviceId = dataInputStream.readInt();
            boolean z = true;
            cdmaSmsMessage.isServicePresent = ((byte) dataInputStream.readInt()) == 1;
            cdmaSmsMessage.serviceCategory = dataInputStream.readInt();
            cdmaSmsMessage.address.digitMode = dataInputStream.read();
            cdmaSmsMessage.address.numberMode = dataInputStream.read();
            cdmaSmsMessage.address.numberType = dataInputStream.read();
            cdmaSmsMessage.address.numberPlan = dataInputStream.read();
            byte b = (byte) dataInputStream.read();
            for (int i = 0; i < b; i++) {
                cdmaSmsMessage.address.digits.add(Byte.valueOf(dataInputStream.readByte()));
            }
            cdmaSmsMessage.subAddress.subaddressType = dataInputStream.read();
            CdmaSmsSubaddress cdmaSmsSubaddress = cdmaSmsMessage.subAddress;
            if (((byte) dataInputStream.read()) != 1) {
                z = false;
            }
            cdmaSmsSubaddress.odd = z;
            byte b2 = (byte) dataInputStream.read();
            for (int i2 = 0; i2 < b2; i2++) {
                cdmaSmsMessage.subAddress.digits.add(Byte.valueOf(dataInputStream.readByte()));
            }
            int i3 = dataInputStream.read();
            for (int i4 = 0; i4 < i3; i4++) {
                cdmaSmsMessage.bearerData.add(Byte.valueOf(dataInputStream.readByte()));
            }
        } catch (IOException e) {
            riljLog("sendSmsCdma: conversion from input stream to object failed: " + e);
        }
    }

    @Override
    public void sendCdmaSms(byte[] bArr, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(87, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            CdmaSmsMessage cdmaSmsMessage = new CdmaSmsMessage();
            constructCdmaSendSmsRilRequest(cdmaSmsMessage, bArr);
            try {
                radioProxy.sendCdmaSms(rILRequestObtainRequest.mSerial, cdmaSmsMessage);
                this.mMetrics.writeRilSendSms(this.mPhoneId.intValue(), rILRequestObtainRequest.mSerial, 2, 2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendCdmaSms", e);
            }
        }
    }

    @Override
    public void acknowledgeLastIncomingCdmaSms(boolean z, int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(88, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " success = " + z + " cause = " + i);
            CdmaSmsAck cdmaSmsAck = new CdmaSmsAck();
            cdmaSmsAck.errorClass = !z ? 1 : 0;
            cdmaSmsAck.smsCauseCode = i;
            try {
                radioProxy.acknowledgeLastIncomingCdmaSms(rILRequestObtainRequest.mSerial, cdmaSmsAck);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "acknowledgeLastIncomingCdmaSms", e);
            }
        }
    }

    @Override
    public void getGsmBroadcastConfig(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(89, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getGsmBroadcastConfig(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getGsmBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] smsBroadcastConfigInfoArr, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(90, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " with " + smsBroadcastConfigInfoArr.length + " configs : ");
            for (int i = 0; i < smsBroadcastConfigInfoArr.length; i++) {
                riljLog(smsBroadcastConfigInfoArr[i].toString());
            }
            ArrayList<GsmBroadcastSmsConfigInfo> arrayList = new ArrayList<>();
            int length = smsBroadcastConfigInfoArr.length;
            for (int i2 = 0; i2 < length; i2++) {
                GsmBroadcastSmsConfigInfo gsmBroadcastSmsConfigInfo = new GsmBroadcastSmsConfigInfo();
                gsmBroadcastSmsConfigInfo.fromServiceId = smsBroadcastConfigInfoArr[i2].getFromServiceId();
                gsmBroadcastSmsConfigInfo.toServiceId = smsBroadcastConfigInfoArr[i2].getToServiceId();
                gsmBroadcastSmsConfigInfo.fromCodeScheme = smsBroadcastConfigInfoArr[i2].getFromCodeScheme();
                gsmBroadcastSmsConfigInfo.toCodeScheme = smsBroadcastConfigInfoArr[i2].getToCodeScheme();
                gsmBroadcastSmsConfigInfo.selected = smsBroadcastConfigInfoArr[i2].isSelected();
                arrayList.add(gsmBroadcastSmsConfigInfo);
            }
            try {
                radioProxy.setGsmBroadcastConfig(rILRequestObtainRequest.mSerial, arrayList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setGsmBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setGsmBroadcastActivation(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(91, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " activate = " + z);
            try {
                radioProxy.setGsmBroadcastActivation(rILRequestObtainRequest.mSerial, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setGsmBroadcastActivation", e);
            }
        }
    }

    @Override
    public void getCdmaBroadcastConfig(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(92, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getCdmaBroadcastConfig(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getCdmaBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] cdmaSmsBroadcastConfigInfoArr, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(93, message, this.mRILDefaultWorkSource);
            ArrayList<CdmaBroadcastSmsConfigInfo> arrayList = new ArrayList<>();
            for (CdmaSmsBroadcastConfigInfo cdmaSmsBroadcastConfigInfo : cdmaSmsBroadcastConfigInfoArr) {
                for (int fromServiceCategory = cdmaSmsBroadcastConfigInfo.getFromServiceCategory(); fromServiceCategory <= cdmaSmsBroadcastConfigInfo.getToServiceCategory(); fromServiceCategory++) {
                    CdmaBroadcastSmsConfigInfo cdmaBroadcastSmsConfigInfo = new CdmaBroadcastSmsConfigInfo();
                    cdmaBroadcastSmsConfigInfo.serviceCategory = fromServiceCategory;
                    cdmaBroadcastSmsConfigInfo.language = cdmaSmsBroadcastConfigInfo.getLanguage();
                    cdmaBroadcastSmsConfigInfo.selected = cdmaSmsBroadcastConfigInfo.isSelected();
                    arrayList.add(cdmaBroadcastSmsConfigInfo);
                }
            }
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " with " + arrayList.size() + " configs : ");
            Iterator<CdmaBroadcastSmsConfigInfo> it = arrayList.iterator();
            while (it.hasNext()) {
                riljLog(it.next().toString());
            }
            try {
                radioProxy.setCdmaBroadcastConfig(rILRequestObtainRequest.mSerial, arrayList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCdmaBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setCdmaBroadcastActivation(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(94, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " activate = " + z);
            try {
                radioProxy.setCdmaBroadcastActivation(rILRequestObtainRequest.mSerial, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCdmaBroadcastActivation", e);
            }
        }
    }

    @Override
    public void getCDMASubscription(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(95, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getCDMASubscription(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getCDMASubscription", e);
            }
        }
    }

    @Override
    public void writeSmsToRuim(int i, String str, Message message) {
        int iTranslateStatus = translateStatus(i);
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(96, message, this.mRILDefaultWorkSource);
            CdmaSmsWriteArgs cdmaSmsWriteArgs = new CdmaSmsWriteArgs();
            cdmaSmsWriteArgs.status = iTranslateStatus;
            constructCdmaSendSmsRilRequest(cdmaSmsWriteArgs.message, str.getBytes());
            try {
                radioProxy.writeSmsToRuim(rILRequestObtainRequest.mSerial, cdmaSmsWriteArgs);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "writeSmsToRuim", e);
            }
        }
    }

    @Override
    public void deleteSmsOnRuim(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(97, message, this.mRILDefaultWorkSource);
            try {
                radioProxy.deleteSmsOnRuim(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "deleteSmsOnRuim", e);
            }
        }
    }

    @Override
    public void getDeviceIdentity(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(98, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getDeviceIdentity(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getDeviceIdentity", e);
            }
        }
    }

    @Override
    public void exitEmergencyCallbackMode(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(99, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.exitEmergencyCallbackMode(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "exitEmergencyCallbackMode", e);
            }
        }
    }

    @Override
    public void getSmscAddress(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(100, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getSmscAddress(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getSmscAddress", e);
            }
        }
    }

    @Override
    public void setSmscAddress(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(101, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " address = " + str);
            try {
                radioProxy.setSmscAddress(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setSmscAddress", e);
            }
        }
    }

    @Override
    public void reportSmsMemoryStatus(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(102, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " available = " + z);
            try {
                radioProxy.reportSmsMemoryStatus(rILRequestObtainRequest.mSerial, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "reportSmsMemoryStatus", e);
            }
        }
    }

    @Override
    public void reportStkServiceIsRunning(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(103, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.reportStkServiceIsRunning(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "reportStkServiceIsRunning", e);
            }
        }
    }

    @Override
    public void getCdmaSubscriptionSource(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(104, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getCdmaSubscriptionSource(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getCdmaSubscriptionSource", e);
            }
        }
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPdu(boolean z, String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(106, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " success = " + z);
            try {
                radioProxy.acknowledgeIncomingGsmSmsWithPdu(rILRequestObtainRequest.mSerial, z, convertNullToEmptyString(str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "acknowledgeIncomingGsmSmsWithPdu", e);
            }
        }
    }

    @Override
    public void getVoiceRadioTechnology(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(108, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getVoiceRadioTechnology(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getVoiceRadioTechnology", e);
            }
        }
    }

    @Override
    public void getCellInfoList(Message message, WorkSource workSource) {
        WorkSource deafultWorkSourceIfInvalid = getDeafultWorkSourceIfInvalid(workSource);
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(109, message, deafultWorkSourceIfInvalid);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getCellInfoList(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getCellInfoList", e);
            }
        }
    }

    @Override
    public void setCellInfoListRate(int i, Message message, WorkSource workSource) {
        WorkSource deafultWorkSourceIfInvalid = getDeafultWorkSourceIfInvalid(workSource);
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(110, message, deafultWorkSourceIfInvalid);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " rateInMillis = " + i);
            try {
                radioProxy.setCellInfoListRate(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCellInfoListRate", e);
            }
        }
    }

    public void setCellInfoListRate() {
        setCellInfoListRate(KeepaliveStatus.INVALID_HANDLE, null, this.mRILDefaultWorkSource);
    }

    @Override
    public void setInitialAttachApn(DataProfile dataProfile, boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(111, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + dataProfile);
            try {
                radioProxy.setInitialAttachApn(rILRequestObtainRequest.mSerial, convertToHalDataProfile(dataProfile), dataProfile.isModemCognitive(), z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setInitialAttachApn", e);
            }
        }
    }

    @Override
    public void getImsRegistrationState(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(112, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getImsRegistrationState(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getImsRegistrationState", e);
            }
        }
    }

    @Override
    public void sendImsGsmSms(String str, String str2, int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(113, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            ImsSmsMessage imsSmsMessage = new ImsSmsMessage();
            imsSmsMessage.tech = 1;
            imsSmsMessage.retry = ((byte) i) >= 1;
            imsSmsMessage.messageRef = i2;
            imsSmsMessage.gsmMessage.add(constructGsmSendSmsRilRequest(str, str2));
            try {
                radioProxy.sendImsSms(rILRequestObtainRequest.mSerial, imsSmsMessage);
                this.mMetrics.writeRilSendSms(this.mPhoneId.intValue(), rILRequestObtainRequest.mSerial, 3, 1);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendImsGsmSms", e);
            }
        }
    }

    @Override
    public void sendImsCdmaSms(byte[] bArr, int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(113, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            ImsSmsMessage imsSmsMessage = new ImsSmsMessage();
            imsSmsMessage.tech = 2;
            imsSmsMessage.retry = ((byte) i) >= 1;
            imsSmsMessage.messageRef = i2;
            CdmaSmsMessage cdmaSmsMessage = new CdmaSmsMessage();
            constructCdmaSendSmsRilRequest(cdmaSmsMessage, bArr);
            imsSmsMessage.cdmaMessage.add(cdmaSmsMessage);
            try {
                radioProxy.sendImsSms(rILRequestObtainRequest.mSerial, imsSmsMessage);
                this.mMetrics.writeRilSendSms(this.mPhoneId.intValue(), rILRequestObtainRequest.mSerial, 3, 2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendImsCdmaSms", e);
            }
        }
    }

    private SimApdu createSimApdu(int i, int i2, int i3, int i4, int i5, int i6, String str) {
        SimApdu simApdu = new SimApdu();
        simApdu.sessionId = i;
        simApdu.cla = i2;
        simApdu.instruction = i3;
        simApdu.p1 = i4;
        simApdu.p2 = i5;
        simApdu.p3 = i6;
        simApdu.data = convertNullToEmptyString(str);
        return simApdu;
    }

    @Override
    public void iccTransmitApduBasicChannel(int i, int i2, int i3, int i4, int i5, String str, Message message) {
        int i6;
        int i7;
        int i8;
        int i9;
        String str2;
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(114, message, this.mRILDefaultWorkSource);
            if (Build.IS_DEBUGGABLE) {
                StringBuilder sb = new StringBuilder();
                sb.append(rILRequestObtainRequest.serialString());
                sb.append("> ");
                sb.append(requestToString(rILRequestObtainRequest.mRequest));
                sb.append(" cla = ");
                i6 = i;
                sb.append(i6);
                sb.append(" instruction = ");
                i7 = i2;
                sb.append(i7);
                sb.append(" p1 = ");
                i8 = i3;
                sb.append(i8);
                sb.append(" p2 =  p3 = ");
                i9 = i5;
                sb.append(i9);
                sb.append(" data = ");
                str2 = str;
                sb.append(str2);
                riljLog(sb.toString());
            } else {
                i6 = i;
                i7 = i2;
                i8 = i3;
                i9 = i5;
                str2 = str;
                riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            }
            try {
                radioProxy.iccTransmitApduBasicChannel(rILRequestObtainRequest.mSerial, createSimApdu(0, i6, i7, i8, i4, i9, str2));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "iccTransmitApduBasicChannel", e);
            }
        }
    }

    @Override
    public void iccOpenLogicalChannel(String str, int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(115, message, this.mRILDefaultWorkSource);
            if (Build.IS_DEBUGGABLE) {
                riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " aid = " + str + " p2 = " + i);
            } else {
                riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            }
            try {
                radioProxy.iccOpenLogicalChannel(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "iccOpenLogicalChannel", e);
            }
        }
    }

    @Override
    public void iccCloseLogicalChannel(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(116, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " channel = " + i);
            try {
                radioProxy.iccCloseLogicalChannel(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "iccCloseLogicalChannel", e);
            }
        }
    }

    @Override
    public void iccTransmitApduLogicalChannel(int i, int i2, int i3, int i4, int i5, int i6, String str, Message message) {
        if (i <= 0) {
            throw new RuntimeException("Invalid channel in iccTransmitApduLogicalChannel: " + i);
        }
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(117, message, this.mRILDefaultWorkSource);
            if (Build.IS_DEBUGGABLE) {
                riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " channel = " + i + " cla = " + i2 + " instruction = " + i3 + " p1 = " + i4 + " p2 =  p3 = " + i6 + " data = " + str);
            } else {
                riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            }
            try {
                radioProxy.iccTransmitApduLogicalChannel(rILRequestObtainRequest.mSerial, createSimApdu(i, i2, i3, i4, i5, i6, str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "iccTransmitApduLogicalChannel", e);
            }
        }
    }

    @Override
    public void nvReadItem(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(118, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " itemId = " + i);
            try {
                radioProxy.nvReadItem(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "nvReadItem", e);
            }
        }
    }

    @Override
    public void nvWriteItem(int i, String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(119, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " itemId = " + i + " itemValue = " + str);
            NvWriteItem nvWriteItem = new NvWriteItem();
            nvWriteItem.itemId = i;
            nvWriteItem.value = convertNullToEmptyString(str);
            try {
                radioProxy.nvWriteItem(rILRequestObtainRequest.mSerial, nvWriteItem);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "nvWriteItem", e);
            }
        }
    }

    @Override
    public void nvWriteCdmaPrl(byte[] bArr, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(120, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " PreferredRoamingList = 0x" + IccUtils.bytesToHexString(bArr));
            ArrayList<Byte> arrayList = new ArrayList<>();
            for (byte b : bArr) {
                arrayList.add(Byte.valueOf(b));
            }
            try {
                radioProxy.nvWriteCdmaPrl(rILRequestObtainRequest.mSerial, arrayList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "nvWriteCdmaPrl", e);
            }
        }
    }

    @Override
    public void nvResetConfig(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(121, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " resetType = " + i);
            try {
                radioProxy.nvResetConfig(rILRequestObtainRequest.mSerial, convertToHalResetNvType(i));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "nvResetConfig", e);
            }
        }
    }

    @Override
    public void setUiccSubscription(int i, int i2, int i3, int i4, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(122, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " slot = " + i + " appIndex = " + i2 + " subId = " + i3 + " subStatus = " + i4);
            SelectUiccSub selectUiccSub = new SelectUiccSub();
            selectUiccSub.slot = i;
            selectUiccSub.appIndex = i2;
            selectUiccSub.subType = i3;
            selectUiccSub.actStatus = i4;
            try {
                radioProxy.setUiccSubscription(rILRequestObtainRequest.mSerial, selectUiccSub);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setUiccSubscription", e);
            }
        }
    }

    @Override
    public void setDataAllowed(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(123, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " allowed = " + z);
            try {
                radioProxy.setDataAllowed(rILRequestObtainRequest.mSerial, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setDataAllowed", e);
            }
        }
    }

    @Override
    public void getHardwareConfig(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(124, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getHardwareConfig(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getHardwareConfig", e);
            }
        }
    }

    @Override
    public void requestIccSimAuthentication(int i, String str, String str2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(125, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.requestIccSimAuthentication(rILRequestObtainRequest.mSerial, i, convertNullToEmptyString(str), convertNullToEmptyString(str2));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "requestIccSimAuthentication", e);
            }
        }
    }

    @Override
    public void setDataProfile(DataProfile[] dataProfileArr, boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(128, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " with data profiles : ");
            int length = dataProfileArr.length;
            for (int i = 0; i < length; i++) {
                riljLog(dataProfileArr[i].toString());
            }
            ArrayList<DataProfileInfo> arrayList = new ArrayList<>();
            for (DataProfile dataProfile : dataProfileArr) {
                arrayList.add(convertToHalDataProfile(dataProfile));
            }
            try {
                radioProxy.setDataProfile(rILRequestObtainRequest.mSerial, arrayList, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setDataProfile", e);
            }
        }
    }

    @Override
    public void requestShutdown(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(129, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.requestShutdown(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "requestShutdown", e);
            }
        }
    }

    @Override
    public void getRadioCapability(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(130, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getRadioCapability(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getRadioCapability", e);
            }
        }
    }

    @Override
    public void setRadioCapability(RadioCapability radioCapability, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(131, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " RadioCapability = " + radioCapability.toString());
            android.hardware.radio.V1_0.RadioCapability radioCapability2 = new android.hardware.radio.V1_0.RadioCapability();
            radioCapability2.session = radioCapability.getSession();
            radioCapability2.phase = radioCapability.getPhase();
            radioCapability2.raf = radioCapability.getRadioAccessFamily();
            radioCapability2.logicalModemUuid = convertNullToEmptyString(radioCapability.getLogicalModemUuid());
            radioCapability2.status = radioCapability.getStatus();
            try {
                radioProxy.setRadioCapability(rILRequestObtainRequest.mSerial, radioCapability2);
            } catch (Exception e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setRadioCapability", e);
            }
        }
    }

    @Override
    public void startLceService(int i, boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (android.hardware.radio.V1_2.IRadio.castFrom((IHwInterface) radioProxy) == null && radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(132, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " reportIntervalMs = " + i + " pullMode = " + z);
            try {
                radioProxy.startLceService(rILRequestObtainRequest.mSerial, i, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "startLceService", e);
            }
        }
    }

    @Override
    public void stopLceService(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (android.hardware.radio.V1_2.IRadio.castFrom((IHwInterface) radioProxy) == null && radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(133, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.stopLceService(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "stopLceService", e);
            }
        }
    }

    @Override
    @Deprecated
    public void pullLceData(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(134, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.pullLceData(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "pullLceData", e);
            }
        }
    }

    @Override
    public void getModemActivityInfo(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(135, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getModemActivityInfo(rILRequestObtainRequest.mSerial);
                Message messageObtainMessage = this.mRilHandler.obtainMessage(5);
                messageObtainMessage.obj = null;
                messageObtainMessage.arg1 = rILRequestObtainRequest.mSerial;
                this.mRilHandler.sendMessageDelayed(messageObtainMessage, 2000L);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getModemActivityInfo", e);
            }
        }
    }

    @Override
    public void setAllowedCarriers(List<android.service.carrier.CarrierIdentifier> list, Message message) {
        String gid2;
        int i;
        Preconditions.checkNotNull(list, "Allowed carriers list cannot be null.");
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(136, message, this.mRILDefaultWorkSource);
            String str = "";
            for (int i2 = 0; i2 < list.size(); i2++) {
                str = str + list.get(i2) + " ";
            }
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " carriers = " + str);
            boolean z = list.size() == 0;
            CarrierRestrictions carrierRestrictions = new CarrierRestrictions();
            for (android.service.carrier.CarrierIdentifier carrierIdentifier : list) {
                Carrier carrier = new Carrier();
                carrier.mcc = convertNullToEmptyString(carrierIdentifier.getMcc());
                carrier.mnc = convertNullToEmptyString(carrierIdentifier.getMnc());
                if (!TextUtils.isEmpty(carrierIdentifier.getSpn())) {
                    gid2 = carrierIdentifier.getSpn();
                    i = 1;
                } else if (!TextUtils.isEmpty(carrierIdentifier.getImsi())) {
                    i = 2;
                    gid2 = carrierIdentifier.getImsi();
                } else if (!TextUtils.isEmpty(carrierIdentifier.getGid1())) {
                    i = 3;
                    gid2 = carrierIdentifier.getGid1();
                } else if (!TextUtils.isEmpty(carrierIdentifier.getGid2())) {
                    i = 4;
                    gid2 = carrierIdentifier.getGid2();
                } else {
                    gid2 = null;
                    i = 0;
                }
                carrier.matchType = i;
                carrier.matchData = convertNullToEmptyString(gid2);
                carrierRestrictions.allowedCarriers.add(carrier);
            }
            try {
                radioProxy.setAllowedCarriers(rILRequestObtainRequest.mSerial, z, carrierRestrictions);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setAllowedCarriers", e);
            }
        }
    }

    @Override
    public void getAllowedCarriers(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(137, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getAllowedCarriers(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getAllowedCarriers", e);
            }
        }
    }

    @Override
    public void sendDeviceState(int i, boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(138, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " " + i + ":" + z);
            try {
                radioProxy.sendDeviceState(rILRequestObtainRequest.mSerial, i, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendDeviceState", e);
            }
        }
    }

    @Override
    public void setUnsolResponseFilter(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(139, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " " + i);
            android.hardware.radio.V1_2.IRadio iRadioCastFrom = android.hardware.radio.V1_2.IRadio.castFrom((IHwInterface) radioProxy);
            if (iRadioCastFrom != null) {
                try {
                    iRadioCastFrom.setIndicationFilter_1_2(rILRequestObtainRequest.mSerial, i);
                    return;
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setIndicationFilter_1_2", e);
                    return;
                }
            }
            try {
                radioProxy.setIndicationFilter(rILRequestObtainRequest.mSerial, i & 7);
            } catch (RemoteException | RuntimeException e2) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setIndicationFilter", e2);
            }
        }
    }

    @Override
    public void setSignalStrengthReportingCriteria(int i, int i2, int[] iArr, int i3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            android.hardware.radio.V1_2.IRadio iRadioCastFrom = android.hardware.radio.V1_2.IRadio.castFrom((IHwInterface) radioProxy);
            if (iRadioCastFrom == null) {
                riljLoge("setSignalStrengthReportingCriteria ignored. RadioProxy 1.2 is null!");
                return;
            }
            RILRequest rILRequestObtainRequest = obtainRequest(148, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                iRadioCastFrom.setSignalStrengthReportingCriteria(rILRequestObtainRequest.mSerial, i, i2, primitiveArrayToArrayList(iArr), convertRanToHalRan(i3));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setSignalStrengthReportingCriteria", e);
            }
        }
    }

    @Override
    public void setLinkCapacityReportingCriteria(int i, int i2, int i3, int[] iArr, int[] iArr2, int i4, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            android.hardware.radio.V1_2.IRadio iRadioCastFrom = android.hardware.radio.V1_2.IRadio.castFrom((IHwInterface) radioProxy);
            if (iRadioCastFrom != null) {
                RILRequest rILRequestObtainRequest = obtainRequest(149, message, this.mRILDefaultWorkSource);
                riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
                try {
                    iRadioCastFrom.setLinkCapacityReportingCriteria(rILRequestObtainRequest.mSerial, i, i2, i3, primitiveArrayToArrayList(iArr), primitiveArrayToArrayList(iArr2), convertRanToHalRan(i4));
                    return;
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setLinkCapacityReportingCriteria", e);
                    return;
                }
            }
            riljLoge("setLinkCapacityReportingCriteria ignored. RadioProxy 1.2 is null!");
        }
    }

    private static int convertRanToHalRan(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                return 0;
        }
    }

    @Override
    public void setSimCardPower(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(140, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " " + i);
            android.hardware.radio.V1_1.IRadio iRadioCastFrom = android.hardware.radio.V1_1.IRadio.castFrom((IHwInterface) radioProxy);
            if (iRadioCastFrom == null) {
                try {
                    switch (i) {
                        case 0:
                            radioProxy.setSimCardPower(rILRequestObtainRequest.mSerial, false);
                            break;
                        case 1:
                            radioProxy.setSimCardPower(rILRequestObtainRequest.mSerial, true);
                            break;
                        default:
                            if (message != null) {
                                AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(6));
                                message.sendToTarget();
                            } else {
                                return;
                            }
                            break;
                    }
                    return;
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setSimCardPower", e);
                    return;
                }
            }
            try {
                iRadioCastFrom.setSimCardPower_1_1(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e2) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setSimCardPower", e2);
            }
        }
    }

    @Override
    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo, Message message) {
        Preconditions.checkNotNull(imsiEncryptionInfo, "ImsiEncryptionInfo cannot be null.");
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            android.hardware.radio.V1_1.IRadio iRadioCastFrom = android.hardware.radio.V1_1.IRadio.castFrom((IHwInterface) radioProxy);
            if (iRadioCastFrom == null) {
                if (message != null) {
                    AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(6));
                    message.sendToTarget();
                    return;
                }
                return;
            }
            RILRequest rILRequestObtainRequest = obtainRequest(141, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                android.hardware.radio.V1_1.ImsiEncryptionInfo imsiEncryptionInfo2 = new android.hardware.radio.V1_1.ImsiEncryptionInfo();
                imsiEncryptionInfo2.mnc = imsiEncryptionInfo.getMnc();
                imsiEncryptionInfo2.mcc = imsiEncryptionInfo.getMcc();
                imsiEncryptionInfo2.keyIdentifier = imsiEncryptionInfo.getKeyIdentifier();
                if (imsiEncryptionInfo.getExpirationTime() != null) {
                    imsiEncryptionInfo2.expirationTime = imsiEncryptionInfo.getExpirationTime().getTime();
                }
                for (byte b : imsiEncryptionInfo.getPublicKey().getEncoded()) {
                    imsiEncryptionInfo2.carrierKey.add(new Byte(b));
                }
                iRadioCastFrom.setCarrierInfoForImsiEncryption(rILRequestObtainRequest.mSerial, imsiEncryptionInfo2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCarrierInfoForImsiEncryption", e);
            }
        }
    }

    @Override
    public void startNattKeepalive(int i, KeepalivePacketData keepalivePacketData, int i2, Message message) {
        Preconditions.checkNotNull(keepalivePacketData, "KeepaliveRequest cannot be null.");
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy == null) {
            riljLoge("Radio Proxy object is null!");
            return;
        }
        android.hardware.radio.V1_1.IRadio iRadioCastFrom = android.hardware.radio.V1_1.IRadio.castFrom((IHwInterface) radioProxy);
        if (iRadioCastFrom == null) {
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(6));
                message.sendToTarget();
                return;
            }
            return;
        }
        RILRequest rILRequestObtainRequest = obtainRequest(146, message, this.mRILDefaultWorkSource);
        riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
        try {
            KeepaliveRequest keepaliveRequest = new KeepaliveRequest();
            keepaliveRequest.cid = i;
            if (keepalivePacketData.dstAddress instanceof Inet4Address) {
                keepaliveRequest.type = 0;
            } else if (keepalivePacketData.dstAddress instanceof Inet6Address) {
                keepaliveRequest.type = 1;
            } else {
                AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(44));
                message.sendToTarget();
                return;
            }
            appendPrimitiveArrayToArrayList(keepalivePacketData.srcAddress.getAddress(), keepaliveRequest.sourceAddress);
            keepaliveRequest.sourcePort = keepalivePacketData.srcPort;
            appendPrimitiveArrayToArrayList(keepalivePacketData.dstAddress.getAddress(), keepaliveRequest.destinationAddress);
            keepaliveRequest.destinationPort = keepalivePacketData.dstPort;
            iRadioCastFrom.startKeepalive(rILRequestObtainRequest.mSerial, keepaliveRequest);
        } catch (RemoteException | RuntimeException e) {
            handleRadioProxyExceptionForRR(rILRequestObtainRequest, "startNattKeepalive", e);
        }
    }

    @Override
    public void stopNattKeepalive(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy == null) {
            Rlog.e(RILJ_LOG_TAG, "Radio Proxy object is null!");
            return;
        }
        android.hardware.radio.V1_1.IRadio iRadioCastFrom = android.hardware.radio.V1_1.IRadio.castFrom((IHwInterface) radioProxy);
        if (iRadioCastFrom == null) {
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(6));
                message.sendToTarget();
                return;
            }
            return;
        }
        RILRequest rILRequestObtainRequest = obtainRequest(147, message, this.mRILDefaultWorkSource);
        riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
        try {
            iRadioCastFrom.stopKeepalive(rILRequestObtainRequest.mSerial, i);
        } catch (RemoteException | RuntimeException e) {
            handleRadioProxyExceptionForRR(rILRequestObtainRequest, "stopNattKeepalive", e);
        }
    }

    @Override
    public void getIMEI(Message message) {
        throw new RuntimeException("getIMEI not expected to be called");
    }

    @Override
    public void getIMEISV(Message message) {
        throw new RuntimeException("getIMEISV not expected to be called");
    }

    @Override
    @Deprecated
    public void getLastPdpFailCause(Message message) {
        throw new RuntimeException("getLastPdpFailCause not expected to be called");
    }

    @Override
    public void getLastDataCallFailCause(Message message) {
        throw new RuntimeException("getLastDataCallFailCause not expected to be called");
    }

    protected int translateStatus(int i) {
        int i2 = i & 7;
        if (i2 == 1) {
            return 1;
        }
        if (i2 == 3) {
            return 0;
        }
        if (i2 != 5) {
            return i2 != 7 ? 1 : 2;
        }
        return 3;
    }

    @Override
    public void resetRadio(Message message) {
        throw new RuntimeException("resetRadio not expected to be called");
    }

    @Override
    public void handleCallSetupRequestFromSim(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(71, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.handleStkCallSetupRequestFromSim(rILRequestObtainRequest.mSerial, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getAllowedCarriers", e);
            }
        }
    }

    public void processIndication(int i) {
        if (i == 1) {
            sendAck();
            riljLog("Unsol response received; Sending ack to ril.cpp");
        }
    }

    void processRequestAck(int i) {
        RILRequest rILRequest;
        synchronized (this.mRequestList) {
            rILRequest = this.mRequestList.get(i);
        }
        if (rILRequest == null) {
            Rlog.w(RILJ_LOG_TAG, "processRequestAck: Unexpected solicited ack response! serial: " + i);
            return;
        }
        decrementWakeLock(rILRequest);
        riljLog(rILRequest.serialString() + " Ack < " + requestToString(rILRequest.mRequest));
    }

    @VisibleForTesting
    public RILRequest processResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequest;
        int i = radioResponseInfo.serial;
        int i2 = radioResponseInfo.error;
        int i3 = radioResponseInfo.type;
        if (i3 == 1) {
            synchronized (this.mRequestList) {
                rILRequest = this.mRequestList.get(i);
            }
            if (rILRequest == null) {
                Rlog.w(RILJ_LOG_TAG, "Unexpected solicited ack response! sn: " + i);
            } else {
                decrementWakeLock(rILRequest);
                riljLog(rILRequest.serialString() + " Ack < " + requestToString(rILRequest.mRequest));
            }
            return rILRequest;
        }
        RILRequest rILRequestFindAndRemoveRequestFromList = findAndRemoveRequestFromList(i);
        if (rILRequestFindAndRemoveRequestFromList == null) {
            Rlog.e(RILJ_LOG_TAG, "processResponse: Unexpected response! serial: " + i + " error: " + i2);
            return null;
        }
        addToRilHistogram(rILRequestFindAndRemoveRequestFromList);
        if (i3 == 2) {
            sendAck();
            riljLog("Response received for " + rILRequestFindAndRemoveRequestFromList.serialString() + " " + requestToString(rILRequestFindAndRemoveRequestFromList.mRequest) + " Sending ack to ril.cpp");
        }
        int i4 = rILRequestFindAndRemoveRequestFromList.mRequest;
        if (i4 == 3 || i4 == 5) {
            if (this.mIccStatusChangedRegistrants != null) {
                riljLog("ON enter sim puk fakeSimStatusChanged: reg count=" + this.mIccStatusChangedRegistrants.size());
                this.mIccStatusChangedRegistrants.notifyRegistrants();
            }
        } else if (i4 == 129) {
            setRadioState(CommandsInterface.RadioState.RADIO_UNAVAILABLE);
        }
        if (i2 != 0) {
            int i5 = rILRequestFindAndRemoveRequestFromList.mRequest;
            if (i5 == 2 || i5 == 4 || i5 == 43) {
                if (this.mIccStatusChangedRegistrants != null) {
                    riljLog("ON some errors fakeSimStatusChanged: reg count=" + this.mIccStatusChangedRegistrants.size());
                    this.mIccStatusChangedRegistrants.notifyRegistrants();
                }
            } else {
                switch (i5) {
                    case 6:
                    case 7:
                    default:
                        return rILRequestFindAndRemoveRequestFromList;
                }
            }
        } else if (rILRequestFindAndRemoveRequestFromList.mRequest == 14 && this.mTestingEmergencyCall.getAndSet(false) && this.mEmergencyCallbackModeRegistrant != null) {
            riljLog("testing emergency call, notify ECM Registrants");
            this.mEmergencyCallbackModeRegistrant.notifyRegistrant();
        }
        return rILRequestFindAndRemoveRequestFromList;
    }

    @VisibleForTesting
    public void processResponseDone(RILRequest rILRequest, RadioResponseInfo radioResponseInfo, Object obj) {
        if (radioResponseInfo.error == 0) {
            riljLog(rILRequest.serialString() + "< " + requestToString(rILRequest.mRequest) + " " + retToString(rILRequest.mRequest, obj));
        } else {
            riljLog(rILRequest.serialString() + "< " + requestToString(rILRequest.mRequest) + " error " + radioResponseInfo.error);
            rILRequest.onError(radioResponseInfo.error, obj);
        }
        this.mMetrics.writeOnRilSolicitedResponse(this.mPhoneId.intValue(), rILRequest.mSerial, radioResponseInfo.error, rILRequest.mRequest, obj);
        if (rILRequest != null) {
            if (radioResponseInfo.type == 0) {
                decrementWakeLock(rILRequest);
            }
            rILRequest.release();
        }
    }

    private void sendAck() {
        RILRequest rILRequestObtain = RILRequest.obtain(800, null, this.mRILDefaultWorkSource);
        acquireWakeLock(rILRequestObtain, 1);
        IRadio radioProxy = getRadioProxy(null);
        if (radioProxy != null) {
            try {
                radioProxy.responseAcknowledgement();
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtain, "sendAck", e);
                riljLoge("sendAck: " + e);
            }
        } else {
            Rlog.e(RILJ_LOG_TAG, "Error trying to send ack, radioProxy = null");
        }
        rILRequestObtain.release();
    }

    private WorkSource getDeafultWorkSourceIfInvalid(WorkSource workSource) {
        if (workSource == null) {
            return this.mRILDefaultWorkSource;
        }
        return workSource;
    }

    private void acquireWakeLock(RILRequest rILRequest, int i) {
        synchronized (rILRequest) {
            if (rILRequest.mWakeLockType != -1) {
                Rlog.d(RILJ_LOG_TAG, "Failed to aquire wakelock for " + rILRequest.serialString());
                return;
            }
            switch (i) {
                case 0:
                    synchronized (this.mWakeLock) {
                        this.mWakeLock.acquire();
                        this.mWakeLockCount++;
                        this.mWlSequenceNum++;
                        if (!this.mClientWakelockTracker.isClientActive(rILRequest.getWorkSourceClientId())) {
                            if (this.mActiveWakelockWorkSource != null) {
                                this.mActiveWakelockWorkSource.add(rILRequest.mWorkSource);
                            } else {
                                this.mActiveWakelockWorkSource = rILRequest.mWorkSource;
                            }
                            this.mWakeLock.setWorkSource(this.mActiveWakelockWorkSource);
                        }
                        this.mClientWakelockTracker.startTracking(rILRequest.mClientId, rILRequest.mRequest, rILRequest.mSerial, this.mWakeLockCount);
                        Message messageObtainMessage = this.mRilHandler.obtainMessage(2);
                        messageObtainMessage.arg1 = this.mWlSequenceNum;
                        this.mRilHandler.sendMessageDelayed(messageObtainMessage, this.mWakeLockTimeout);
                        break;
                    }
                    rILRequest.mWakeLockType = i;
                    return;
                case 1:
                    synchronized (this.mAckWakeLock) {
                        this.mAckWakeLock.acquire();
                        this.mAckWlSequenceNum++;
                        Message messageObtainMessage2 = this.mRilHandler.obtainMessage(4);
                        messageObtainMessage2.arg1 = this.mAckWlSequenceNum;
                        this.mRilHandler.sendMessageDelayed(messageObtainMessage2, this.mAckWakeLockTimeout);
                        break;
                    }
                    rILRequest.mWakeLockType = i;
                    return;
                default:
                    Rlog.w(RILJ_LOG_TAG, "Acquiring Invalid Wakelock type " + i);
                    return;
            }
        }
    }

    @VisibleForTesting
    public PowerManager.WakeLock getWakeLock(int i) {
        return i == 0 ? this.mWakeLock : this.mAckWakeLock;
    }

    @VisibleForTesting
    public RilHandler getRilHandler() {
        return this.mRilHandler;
    }

    @VisibleForTesting
    public SparseArray<RILRequest> getRilRequestList() {
        return this.mRequestList;
    }

    private void decrementWakeLock(RILRequest rILRequest) {
        synchronized (rILRequest) {
            switch (rILRequest.mWakeLockType) {
                case -1:
                    rILRequest.mWakeLockType = -1;
                    break;
                case 0:
                    synchronized (this.mWakeLock) {
                        this.mClientWakelockTracker.stopTracking(rILRequest.mClientId, rILRequest.mRequest, rILRequest.mSerial, this.mWakeLockCount > 1 ? this.mWakeLockCount - 1 : 0);
                        if (!this.mClientWakelockTracker.isClientActive(rILRequest.getWorkSourceClientId()) && this.mActiveWakelockWorkSource != null) {
                            this.mActiveWakelockWorkSource.remove(rILRequest.mWorkSource);
                            if (this.mActiveWakelockWorkSource.size() == 0) {
                                this.mActiveWakelockWorkSource = null;
                            }
                            this.mWakeLock.setWorkSource(this.mActiveWakelockWorkSource);
                        }
                        if (this.mWakeLockCount > 1) {
                            this.mWakeLockCount--;
                        } else {
                            this.mWakeLockCount = 0;
                            this.mWakeLock.release();
                        }
                        break;
                    }
                    rILRequest.mWakeLockType = -1;
                    break;
                case 1:
                    rILRequest.mWakeLockType = -1;
                    break;
                default:
                    Rlog.w(RILJ_LOG_TAG, "Decrementing Invalid Wakelock type " + rILRequest.mWakeLockType);
                    rILRequest.mWakeLockType = -1;
                    break;
            }
        }
    }

    private boolean clearWakeLock(int i) {
        if (i == 0) {
            synchronized (this.mWakeLock) {
                if (this.mWakeLockCount == 0 && !this.mWakeLock.isHeld()) {
                    return false;
                }
                Rlog.d(RILJ_LOG_TAG, "NOTE: mWakeLockCount is " + this.mWakeLockCount + "at time of clearing");
                this.mWakeLockCount = 0;
                this.mWakeLock.release();
                this.mClientWakelockTracker.stopTrackingAll();
                this.mActiveWakelockWorkSource = null;
                return true;
            }
        }
        synchronized (this.mAckWakeLock) {
            if (!this.mAckWakeLock.isHeld()) {
                return false;
            }
            this.mAckWakeLock.release();
            return true;
        }
    }

    private void clearRequestList(int i, boolean z) {
        synchronized (this.mRequestList) {
            int size = this.mRequestList.size();
            if (z) {
                Rlog.d(RILJ_LOG_TAG, "clearRequestList  mWakeLockCount=" + this.mWakeLockCount + " mRequestList=" + size);
            }
            for (int i2 = 0; i2 < size; i2++) {
                RILRequest rILRequestValueAt = this.mRequestList.valueAt(i2);
                if (z) {
                    Rlog.d(RILJ_LOG_TAG, i2 + ": [" + rILRequestValueAt.mSerial + "] " + requestToString(rILRequestValueAt.mRequest));
                }
                rILRequestValueAt.onError(i, null);
                decrementWakeLock(rILRequestValueAt);
                rILRequestValueAt.release();
            }
            this.mRequestList.clear();
        }
    }

    private RILRequest findAndRemoveRequestFromList(int i) {
        RILRequest rILRequest;
        synchronized (this.mRequestList) {
            rILRequest = this.mRequestList.get(i);
            if (rILRequest != null) {
                this.mRequestList.remove(i);
            }
        }
        return rILRequest;
    }

    private void addToRilHistogram(RILRequest rILRequest) {
        int iElapsedRealtime = (int) (SystemClock.elapsedRealtime() - rILRequest.mStartTimeMs);
        synchronized (mRilTimeHistograms) {
            TelephonyHistogram telephonyHistogram = mRilTimeHistograms.get(rILRequest.mRequest);
            if (telephonyHistogram == null) {
                telephonyHistogram = new TelephonyHistogram(1, rILRequest.mRequest, 5);
                mRilTimeHistograms.put(rILRequest.mRequest, telephonyHistogram);
            }
            telephonyHistogram.addTimeTaken(iElapsedRealtime);
        }
    }

    RadioCapability makeStaticRadioCapability() {
        int iRafTypeFromString;
        String string = this.mContext.getResources().getString(R.string.anr_title);
        if (!TextUtils.isEmpty(string)) {
            iRafTypeFromString = RadioAccessFamily.rafTypeFromString(string);
        } else {
            iRafTypeFromString = 1;
        }
        RadioCapability radioCapability = new RadioCapability(this.mPhoneId.intValue(), 0, 0, iRafTypeFromString, "", 1);
        riljLog("Faking RIL_REQUEST_GET_RADIO_CAPABILITY response using " + iRafTypeFromString);
        return radioCapability;
    }

    protected static String retToString(int i, Object obj) {
        if (obj != null && i != 11 && i != 115 && i != 117) {
            switch (i) {
                case 38:
                case 39:
                    return "";
                default:
                    int i2 = 1;
                    if (obj instanceof int[]) {
                        int[] iArr = (int[]) obj;
                        int length = iArr.length;
                        StringBuilder sb = new StringBuilder("{");
                        if (length > 0) {
                            sb.append(iArr[0]);
                            while (i2 < length) {
                                sb.append(", ");
                                sb.append(iArr[i2]);
                                i2++;
                            }
                        }
                        sb.append("}");
                        return sb.toString();
                    }
                    if (obj instanceof String[]) {
                        String[] strArr = (String[]) obj;
                        int length2 = strArr.length;
                        StringBuilder sb2 = new StringBuilder("{");
                        if (length2 > 0) {
                            sb2.append(strArr[0]);
                            while (i2 < length2) {
                                sb2.append(", ");
                                sb2.append(strArr[i2]);
                                i2++;
                            }
                        }
                        sb2.append("}");
                        return sb2.toString();
                    }
                    if (i == 9) {
                        StringBuilder sb3 = new StringBuilder("{");
                        for (DriverCall driverCall : (ArrayList) obj) {
                            sb3.append("[");
                            sb3.append(driverCall);
                            sb3.append("] ");
                        }
                        sb3.append("}");
                        return sb3.toString();
                    }
                    if (i == 75) {
                        StringBuilder sb4 = new StringBuilder("{");
                        for (NeighboringCellInfo neighboringCellInfo : (ArrayList) obj) {
                            sb4.append("[");
                            sb4.append(neighboringCellInfo);
                            sb4.append("] ");
                        }
                        sb4.append("}");
                        return sb4.toString();
                    }
                    if (i == 33) {
                        StringBuilder sb5 = new StringBuilder("{");
                        for (CallForwardInfo callForwardInfo : (CallForwardInfo[]) obj) {
                            sb5.append("[");
                            sb5.append(callForwardInfo);
                            sb5.append("] ");
                        }
                        sb5.append("}");
                        return sb5.toString();
                    }
                    if (i == 124) {
                        StringBuilder sb6 = new StringBuilder(" ");
                        for (HardwareConfig hardwareConfig : (ArrayList) obj) {
                            sb6.append("[");
                            sb6.append(hardwareConfig);
                            sb6.append("] ");
                        }
                        return sb6.toString();
                    }
                    return obj.toString();
            }
        }
        return "";
    }

    void writeMetricsNewSms(int i, int i2) {
        this.mMetrics.writeRilNewSms(this.mPhoneId.intValue(), i, i2);
    }

    void writeMetricsCallRing(char[] cArr) {
        this.mMetrics.writeRilCallRing(this.mPhoneId.intValue(), cArr);
    }

    void writeMetricsSrvcc(int i) {
        this.mMetrics.writeRilSrvcc(this.mPhoneId.intValue(), i);
    }

    void writeMetricsModemRestartEvent(String str) {
        this.mMetrics.writeModemRestartEvent(this.mPhoneId.intValue(), str);
    }

    public void notifyRegistrantsRilConnectionChanged(int i) {
        this.mRilVersion = i;
        if (this.mRilConnectedRegistrants != null) {
            this.mRilConnectedRegistrants.notifyRegistrants(new AsyncResult((Object) null, new Integer(i), (Throwable) null));
        }
    }

    void notifyRegistrantsCdmaInfoRec(CdmaInformationRecords cdmaInformationRecords) {
        if (cdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaDisplayInfoRec) {
            if (this.mDisplayInfoRegistrants != null) {
                unsljLogRet(1027, cdmaInformationRecords.record);
                this.mDisplayInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, cdmaInformationRecords.record, (Throwable) null));
                return;
            }
            return;
        }
        if (cdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaSignalInfoRec) {
            if (this.mSignalInfoRegistrants != null) {
                unsljLogRet(1027, cdmaInformationRecords.record);
                this.mSignalInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, cdmaInformationRecords.record, (Throwable) null));
                return;
            }
            return;
        }
        if (cdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaNumberInfoRec) {
            if (this.mNumberInfoRegistrants != null) {
                unsljLogRet(1027, cdmaInformationRecords.record);
                this.mNumberInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, cdmaInformationRecords.record, (Throwable) null));
                return;
            }
            return;
        }
        if (cdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaRedirectingNumberInfoRec) {
            if (this.mRedirNumInfoRegistrants != null) {
                unsljLogRet(1027, cdmaInformationRecords.record);
                this.mRedirNumInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, cdmaInformationRecords.record, (Throwable) null));
                return;
            }
            return;
        }
        if (cdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaLineControlInfoRec) {
            if (this.mLineControlInfoRegistrants != null) {
                unsljLogRet(1027, cdmaInformationRecords.record);
                this.mLineControlInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, cdmaInformationRecords.record, (Throwable) null));
                return;
            }
            return;
        }
        if (cdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaT53ClirInfoRec) {
            if (this.mT53ClirInfoRegistrants != null) {
                unsljLogRet(1027, cdmaInformationRecords.record);
                this.mT53ClirInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, cdmaInformationRecords.record, (Throwable) null));
                return;
            }
            return;
        }
        if ((cdmaInformationRecords.record instanceof CdmaInformationRecords.CdmaT53AudioControlInfoRec) && this.mT53AudCntrlInfoRegistrants != null) {
            unsljLogRet(1027, cdmaInformationRecords.record);
            this.mT53AudCntrlInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, cdmaInformationRecords.record, (Throwable) null));
        }
    }

    protected static String requestToString(int i) {
        if (i != 800) {
            switch (i) {
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
                case 23:
                    return "RADIO_POWER";
                case 24:
                    return "DTMF";
                case 25:
                    return "SEND_SMS";
                case 26:
                    return "SEND_SMS_EXPECT_MORE";
                case 27:
                    return "SETUP_DATA_CALL";
                case 28:
                    return "SIM_IO";
                case 29:
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
                case 43:
                    return "SET_FACILITY_LOCK";
                case 44:
                    return "CHANGE_BARRING_PASSWORD";
                case 45:
                    return "QUERY_NETWORK_SELECTION_MODE";
                case 46:
                    return "SET_NETWORK_SELECTION_AUTOMATIC";
                case 47:
                    return "SET_NETWORK_SELECTION_MANUAL";
                case 48:
                    return "QUERY_AVAILABLE_NETWORKS ";
                case 49:
                    return "DTMF_START";
                case 50:
                    return "DTMF_STOP";
                case 51:
                    return "BASEBAND_VERSION";
                case 52:
                    return "SEPARATE_CONNECTION";
                case 53:
                    return "SET_MUTE";
                case 54:
                    return "GET_MUTE";
                case 55:
                    return "QUERY_CLIP";
                case 56:
                    return "LAST_DATA_CALL_FAIL_CAUSE";
                case 57:
                    return "DATA_CALL_LIST";
                case 58:
                    return "RESET_RADIO";
                case 59:
                    return "OEM_HOOK_RAW";
                case 60:
                    return "OEM_HOOK_STRINGS";
                case 61:
                    return "SCREEN_STATE";
                case 62:
                    return "SET_SUPP_SVC_NOTIFICATION";
                case 63:
                    return "WRITE_SMS_TO_SIM";
                case 64:
                    return "DELETE_SMS_ON_SIM";
                case 65:
                    return "SET_BAND_MODE";
                case 66:
                    return "QUERY_AVAILABLE_BAND_MODE";
                case TelephonyProto.RilErrno.RIL_E_INVALID_RESPONSE:
                    return "REQUEST_STK_GET_PROFILE";
                case 68:
                    return "REQUEST_STK_SET_PROFILE";
                case 69:
                    return "REQUEST_STK_SEND_ENVELOPE_COMMAND";
                case 70:
                    return "REQUEST_STK_SEND_TERMINAL_RESPONSE";
                case 71:
                    return "REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM";
                case 72:
                    return "REQUEST_EXPLICIT_CALL_TRANSFER";
                case 73:
                    return "REQUEST_SET_PREFERRED_NETWORK_TYPE";
                case 74:
                    return "REQUEST_GET_PREFERRED_NETWORK_TYPE";
                case 75:
                    return "REQUEST_GET_NEIGHBORING_CELL_IDS";
                case 76:
                    return "REQUEST_SET_LOCATION_UPDATES";
                case 77:
                    return "RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE";
                case 78:
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
                case 96:
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
                default:
                    switch (i) {
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
                        case 136:
                            return "RIL_REQUEST_SET_ALLOWED_CARRIERS";
                        case 137:
                            return "RIL_REQUEST_GET_ALLOWED_CARRIERS";
                        case 138:
                            return "RIL_REQUEST_SEND_DEVICE_STATE";
                        case 139:
                            return "RIL_REQUEST_SET_UNSOLICITED_RESPONSE_FILTER";
                        case 140:
                            return "RIL_REQUEST_SET_SIM_CARD_POWER";
                        case 141:
                            return "RIL_REQUEST_SET_CARRIER_INFO_IMSI_ENCRYPTION";
                        case 142:
                            return "RIL_REQUEST_START_NETWORK_SCAN";
                        case 143:
                            return "RIL_REQUEST_STOP_NETWORK_SCAN";
                        case 144:
                            return "RIL_REQUEST_GET_SLOT_STATUS";
                        case 145:
                            return "RIL_REQUEST_SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING";
                        case 146:
                            return "RIL_REQUEST_START_KEEPALIVE";
                        case 147:
                            return "RIL_REQUEST_STOP_KEEPALIVE";
                        case 148:
                            return "RIL_REQUEST_SET_SIGNAL_STRENGTH_REPORTING_CRITERIA";
                        case 149:
                            return "RIL_REQUEST_SET_LINK_CAPACITY_REPORTING_CRITERIA";
                        default:
                            try {
                                return (String) Class.forName("com.mediatek.internal.telephony.MtkRIL").getDeclaredMethod("requestToStringEx", Integer.class).invoke(null, Integer.valueOf(i));
                            } catch (Exception e) {
                                return "<unknown request>";
                            }
                    }
            }
        }
        return "RIL_RESPONSE_ACKNOWLEDGEMENT";
    }

    protected static String responseToString(int i) {
        switch (i) {
            case 1000:
                return "UNSOL_RESPONSE_RADIO_STATE_CHANGED";
            case 1001:
                return "UNSOL_RESPONSE_CALL_STATE_CHANGED";
            case 1002:
                return "UNSOL_RESPONSE_NETWORK_STATE_CHANGED";
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
            case ApnTypes.ALL:
                return "UNSOL_RESTRICTED_STATE_CHANGED";
            case android.hardware.radio.V1_0.RadioAccessFamily.HSUPA:
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
            case 1041:
            default:
                try {
                    return (String) Class.forName("com.mediatek.internal.telephony.MtkRIL").getDeclaredMethod("responseToStringEx", Integer.class).invoke(null, Integer.valueOf(i));
                } catch (Exception e) {
                    return "<unknown response>";
                }
            case 1042:
                return "RIL_UNSOL_RADIO_CAPABILITY";
            case 1043:
                return "UNSOL_ON_SS";
            case 1044:
                return "UNSOL_STK_CC_ALPHA_NOTIFY";
            case 1045:
                return "UNSOL_LCE_INFO_RECV";
            case 1046:
                return "UNSOL_PCO_DATA";
            case 1047:
                return "UNSOL_MODEM_RESTART";
            case 1048:
                return "RIL_UNSOL_CARRIER_INFO_IMSI_ENCRYPTION";
            case 1049:
                return "RIL_UNSOL_NETWORK_SCAN_RESULT";
            case 1050:
                return "RIL_UNSOL_ICC_SLOT_STATUS";
            case 1051:
                return "RIL_UNSOL_KEEPALIVE_STATUS";
            case 1052:
                return "RIL_UNSOL_PHYSICAL_CHANNEL_CONFIG";
        }
    }

    public void riljLog(String str) {
        String str2;
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        if (this.mPhoneId != null) {
            str2 = " [SUB" + this.mPhoneId + "]";
        } else {
            str2 = "";
        }
        sb.append(str2);
        Rlog.d(RILJ_LOG_TAG, sb.toString());
    }

    public void riljLoge(String str) {
        String str2;
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        if (this.mPhoneId != null) {
            str2 = " [SUB" + this.mPhoneId + "]";
        } else {
            str2 = "";
        }
        sb.append(str2);
        Rlog.e(RILJ_LOG_TAG, sb.toString());
    }

    public void riljLoge(String str, Exception exc) {
        String str2;
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        if (this.mPhoneId != null) {
            str2 = " [SUB" + this.mPhoneId + "]";
        } else {
            str2 = "";
        }
        sb.append(str2);
        Rlog.e(RILJ_LOG_TAG, sb.toString(), exc);
    }

    public void riljLogv(String str) {
        String str2;
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        if (this.mPhoneId != null) {
            str2 = " [SUB" + this.mPhoneId + "]";
        } else {
            str2 = "";
        }
        sb.append(str2);
        Rlog.v(RILJ_LOG_TAG, sb.toString());
    }

    public void unsljLog(int i) {
        riljLog("[UNSL]< " + responseToString(i));
    }

    public void unsljLogMore(int i, String str) {
        riljLog("[UNSL]< " + responseToString(i) + " " + str);
    }

    public void unsljLogRet(int i, Object obj) {
        riljLog("[UNSL]< " + responseToString(i) + " " + retToString(i, obj));
    }

    public void unsljLogvRet(int i, Object obj) {
        riljLogv("[UNSL]< " + responseToString(i) + " " + retToString(i, obj));
    }

    @Override
    public void setPhoneType(int i) {
        riljLog("setPhoneType=" + i + " old value=" + this.mPhoneType);
        this.mPhoneType = i;
    }

    @Override
    public void testingEmergencyCall() {
        riljLog("testingEmergencyCall");
        this.mTestingEmergencyCall.set(true);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("RIL: " + this);
        printWriter.println(" mWakeLock=" + this.mWakeLock);
        printWriter.println(" mWakeLockTimeout=" + this.mWakeLockTimeout);
        synchronized (this.mRequestList) {
            synchronized (this.mWakeLock) {
                printWriter.println(" mWakeLockCount=" + this.mWakeLockCount);
            }
            int size = this.mRequestList.size();
            printWriter.println(" mRequestList count=" + size);
            for (int i = 0; i < size; i++) {
                RILRequest rILRequestValueAt = this.mRequestList.valueAt(i);
                printWriter.println("  [" + rILRequestValueAt.mSerial + "] " + requestToString(rILRequestValueAt.mRequest));
            }
        }
        printWriter.println(" mLastNITZTimeInfo=" + Arrays.toString(this.mLastNITZTimeInfo));
        printWriter.println(" mTestingEmergencyCall=" + this.mTestingEmergencyCall.get());
        this.mClientWakelockTracker.dumpClientRequestTracker(printWriter);
    }

    @Override
    public List<ClientRequestStats> getClientRequestStats() {
        return this.mClientWakelockTracker.getClientRequestStats();
    }

    public static void appendPrimitiveArrayToArrayList(byte[] bArr, ArrayList<Byte> arrayList) {
        for (byte b : bArr) {
            arrayList.add(Byte.valueOf(b));
        }
    }

    public static ArrayList<Byte> primitiveArrayToArrayList(byte[] bArr) {
        ArrayList<Byte> arrayList = new ArrayList<>(bArr.length);
        for (byte b : bArr) {
            arrayList.add(Byte.valueOf(b));
        }
        return arrayList;
    }

    public static ArrayList<Integer> primitiveArrayToArrayList(int[] iArr) {
        ArrayList<Integer> arrayList = new ArrayList<>(iArr.length);
        for (int i : iArr) {
            arrayList.add(Integer.valueOf(i));
        }
        return arrayList;
    }

    public static byte[] arrayListToPrimitiveArray(ArrayList<Byte> arrayList) {
        byte[] bArr = new byte[arrayList.size()];
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = arrayList.get(i).byteValue();
        }
        return bArr;
    }

    protected ArrayList<HardwareConfig> convertHalHwConfigList(ArrayList<android.hardware.radio.V1_0.HardwareConfig> arrayList, RIL ril) {
        HardwareConfig hardwareConfig;
        ArrayList<HardwareConfig> arrayList2 = new ArrayList<>(arrayList.size());
        for (android.hardware.radio.V1_0.HardwareConfig hardwareConfig2 : arrayList) {
            int i = hardwareConfig2.type;
            switch (i) {
                case 0:
                    hardwareConfig = new HardwareConfig(i);
                    HardwareConfigModem hardwareConfigModem = hardwareConfig2.modem.get(0);
                    hardwareConfig.assignModem(hardwareConfig2.uuid, hardwareConfig2.state, hardwareConfigModem.rilModel, hardwareConfigModem.rat, hardwareConfigModem.maxVoice, hardwareConfigModem.maxData, hardwareConfigModem.maxStandby);
                    break;
                case 1:
                    hardwareConfig = new HardwareConfig(i);
                    hardwareConfig.assignSim(hardwareConfig2.uuid, hardwareConfig2.state, hardwareConfig2.sim.get(0).modemUuid);
                    break;
                default:
                    throw new RuntimeException("RIL_REQUEST_GET_HARDWARE_CONFIG invalid hardward type:" + i);
            }
            arrayList2.add(hardwareConfig);
        }
        return arrayList2;
    }

    static RadioCapability convertHalRadioCapability(android.hardware.radio.V1_0.RadioCapability radioCapability, RIL ril) {
        int i = radioCapability.session;
        int i2 = radioCapability.phase;
        int i3 = radioCapability.raf;
        String str = radioCapability.logicalModemUuid;
        int i4 = radioCapability.status;
        ril.riljLog("convertHalRadioCapability: session=" + i + ", phase=" + i2 + ", rat=" + i3 + ", logicModemUuid=" + str + ", status=" + i4);
        return new RadioCapability(ril.mPhoneId.intValue(), i, i2, i3, str, i4);
    }

    static LinkCapacityEstimate convertHalLceData(LceDataInfo lceDataInfo, RIL ril) {
        LinkCapacityEstimate linkCapacityEstimate = new LinkCapacityEstimate(lceDataInfo.lastHopCapacityKbps, Byte.toUnsignedInt(lceDataInfo.confidenceLevel), lceDataInfo.lceSuspended ? 1 : 0);
        ril.riljLog("LCE capacity information received:" + linkCapacityEstimate);
        return linkCapacityEstimate;
    }

    static LinkCapacityEstimate convertHalLceData(android.hardware.radio.V1_2.LinkCapacityEstimate linkCapacityEstimate, RIL ril) {
        LinkCapacityEstimate linkCapacityEstimate2 = new LinkCapacityEstimate(linkCapacityEstimate.downlinkCapacityKbps, linkCapacityEstimate.uplinkCapacityKbps);
        ril.riljLog("LCE capacity information received:" + linkCapacityEstimate2);
        return linkCapacityEstimate2;
    }

    private static void writeToParcelForGsm(Parcel parcel, int i, int i2, int i3, int i4, String str, String str2, String str3, String str4, int i5, int i6, int i7) {
        parcel.writeInt(1);
        parcel.writeString(str);
        parcel.writeString(str2);
        parcel.writeString(str3);
        parcel.writeString(str4);
        parcel.writeInt(i);
        parcel.writeInt(i2);
        parcel.writeInt(i3);
        parcel.writeInt(i4);
        parcel.writeInt(i5);
        parcel.writeInt(i6);
        parcel.writeInt(i7);
    }

    private static void writeToParcelForCdma(Parcel parcel, int i, int i2, int i3, int i4, int i5, String str, String str2, int i6, int i7, int i8, int i9, int i10) {
        new CellIdentityCdma(i, i2, i3, i4, i5, str, str2).writeToParcel(parcel, 0);
        new CellSignalStrengthCdma(i6, i7, i8, i9, i10).writeToParcel(parcel, 0);
    }

    private static void writeToParcelForLte(Parcel parcel, int i, int i2, int i3, int i4, int i5, String str, String str2, String str3, String str4, int i6, int i7, int i8, int i9, int i10, int i11) {
        parcel.writeInt(3);
        parcel.writeString(str);
        parcel.writeString(str2);
        parcel.writeString(str3);
        parcel.writeString(str4);
        parcel.writeInt(i);
        parcel.writeInt(i2);
        parcel.writeInt(i3);
        parcel.writeInt(i4);
        parcel.writeInt(i5);
        parcel.writeInt(i6);
        parcel.writeInt(i7);
        parcel.writeInt(i8);
        parcel.writeInt(i9);
        parcel.writeInt(i10);
        parcel.writeInt(i11);
    }

    private static void writeToParcelForWcdma(Parcel parcel, int i, int i2, int i3, int i4, String str, String str2, String str3, String str4, int i5, int i6) {
        parcel.writeInt(4);
        parcel.writeString(str);
        parcel.writeString(str2);
        parcel.writeString(str3);
        parcel.writeString(str4);
        parcel.writeInt(i);
        parcel.writeInt(i2);
        parcel.writeInt(i3);
        parcel.writeInt(i4);
        parcel.writeInt(i5);
        parcel.writeInt(i6);
    }

    @VisibleForTesting
    public static ArrayList<CellInfo> convertHalCellInfoList(ArrayList<android.hardware.radio.V1_0.CellInfo> arrayList) {
        Iterator<android.hardware.radio.V1_0.CellInfo> it;
        int i;
        Parcel parcel;
        ArrayList<CellInfo> arrayList2 = new ArrayList<>(arrayList.size());
        Iterator<android.hardware.radio.V1_0.CellInfo> it2 = arrayList.iterator();
        while (it2.hasNext()) {
            android.hardware.radio.V1_0.CellInfo next = it2.next();
            Parcel parcelObtain = Parcel.obtain();
            parcelObtain.writeInt(next.cellInfoType);
            parcelObtain.writeInt(next.registered ? 1 : 0);
            parcelObtain.writeInt(next.timeStampType);
            parcelObtain.writeLong(next.timeStamp);
            parcelObtain.writeInt(KeepaliveStatus.INVALID_HANDLE);
            switch (next.cellInfoType) {
                case 1:
                    it = it2;
                    i = 0;
                    parcel = parcelObtain;
                    CellInfoGsm cellInfoGsm = next.gsm.get(0);
                    writeToParcelForGsm(parcel, cellInfoGsm.cellIdentityGsm.lac, cellInfoGsm.cellIdentityGsm.cid, cellInfoGsm.cellIdentityGsm.arfcn, Byte.toUnsignedInt(cellInfoGsm.cellIdentityGsm.bsic), cellInfoGsm.cellIdentityGsm.mcc, cellInfoGsm.cellIdentityGsm.mnc, "", "", cellInfoGsm.signalStrengthGsm.signalStrength, cellInfoGsm.signalStrengthGsm.bitErrorRate, cellInfoGsm.signalStrengthGsm.timingAdvance);
                    break;
                case 2:
                    it = it2;
                    parcel = parcelObtain;
                    CellInfoCdma cellInfoCdma = next.cdma.get(0);
                    writeToParcelForCdma(parcel, cellInfoCdma.cellIdentityCdma.networkId, cellInfoCdma.cellIdentityCdma.systemId, cellInfoCdma.cellIdentityCdma.baseStationId, cellInfoCdma.cellIdentityCdma.longitude, cellInfoCdma.cellIdentityCdma.latitude, "", "", cellInfoCdma.signalStrengthCdma.dbm, cellInfoCdma.signalStrengthCdma.ecio, cellInfoCdma.signalStrengthEvdo.dbm, cellInfoCdma.signalStrengthEvdo.ecio, cellInfoCdma.signalStrengthEvdo.signalNoiseRatio);
                    i = 0;
                    break;
                case 3:
                    CellInfoLte cellInfoLte = next.lte.get(0);
                    it = it2;
                    parcel = parcelObtain;
                    writeToParcelForLte(parcelObtain, cellInfoLte.cellIdentityLte.ci, cellInfoLte.cellIdentityLte.pci, cellInfoLte.cellIdentityLte.tac, cellInfoLte.cellIdentityLte.earfcn, KeepaliveStatus.INVALID_HANDLE, cellInfoLte.cellIdentityLte.mcc, cellInfoLte.cellIdentityLte.mnc, "", "", cellInfoLte.signalStrengthLte.signalStrength, cellInfoLte.signalStrengthLte.rsrp, cellInfoLte.signalStrengthLte.rsrq, cellInfoLte.signalStrengthLte.rssnr, cellInfoLte.signalStrengthLte.cqi, cellInfoLte.signalStrengthLte.timingAdvance);
                    i = 0;
                    break;
                case 4:
                    CellInfoWcdma cellInfoWcdma = next.wcdma.get(0);
                    writeToParcelForWcdma(parcelObtain, cellInfoWcdma.cellIdentityWcdma.lac, cellInfoWcdma.cellIdentityWcdma.cid, cellInfoWcdma.cellIdentityWcdma.psc, cellInfoWcdma.cellIdentityWcdma.uarfcn, cellInfoWcdma.cellIdentityWcdma.mcc, cellInfoWcdma.cellIdentityWcdma.mnc, "", "", cellInfoWcdma.signalStrengthWcdma.signalStrength, cellInfoWcdma.signalStrengthWcdma.bitErrorRate);
                    it = it2;
                    i = 0;
                    parcel = parcelObtain;
                    break;
                default:
                    throw new RuntimeException("unexpected cellinfotype: " + next.cellInfoType);
            }
            Parcel parcel2 = parcel;
            parcel2.setDataPosition(i);
            CellInfo cellInfo = (CellInfo) CellInfo.CREATOR.createFromParcel(parcel2);
            parcel2.recycle();
            arrayList2.add(cellInfo);
            it2 = it;
        }
        return arrayList2;
    }

    @VisibleForTesting
    public static ArrayList<CellInfo> convertHalCellInfoList_1_2(ArrayList<android.hardware.radio.V1_2.CellInfo> arrayList) {
        ArrayList<CellInfo> arrayList2;
        Iterator<android.hardware.radio.V1_2.CellInfo> it;
        int i;
        Parcel parcel;
        ArrayList<CellInfo> arrayList3 = new ArrayList<>(arrayList.size());
        Iterator<android.hardware.radio.V1_2.CellInfo> it2 = arrayList.iterator();
        while (it2.hasNext()) {
            android.hardware.radio.V1_2.CellInfo next = it2.next();
            Parcel parcelObtain = Parcel.obtain();
            parcelObtain.writeInt(next.cellInfoType);
            parcelObtain.writeInt(next.registered ? 1 : 0);
            parcelObtain.writeInt(next.timeStampType);
            parcelObtain.writeLong(next.timeStamp);
            parcelObtain.writeInt(next.connectionStatus);
            switch (next.cellInfoType) {
                case 1:
                    arrayList2 = arrayList3;
                    it = it2;
                    i = 0;
                    parcel = parcelObtain;
                    android.hardware.radio.V1_2.CellInfoGsm cellInfoGsm = next.gsm.get(0);
                    writeToParcelForGsm(parcel, cellInfoGsm.cellIdentityGsm.base.lac, cellInfoGsm.cellIdentityGsm.base.cid, cellInfoGsm.cellIdentityGsm.base.arfcn, Byte.toUnsignedInt(cellInfoGsm.cellIdentityGsm.base.bsic), cellInfoGsm.cellIdentityGsm.base.mcc, cellInfoGsm.cellIdentityGsm.base.mnc, cellInfoGsm.cellIdentityGsm.operatorNames.alphaLong, cellInfoGsm.cellIdentityGsm.operatorNames.alphaShort, cellInfoGsm.signalStrengthGsm.signalStrength, cellInfoGsm.signalStrengthGsm.bitErrorRate, cellInfoGsm.signalStrengthGsm.timingAdvance);
                    break;
                case 2:
                    arrayList2 = arrayList3;
                    it = it2;
                    i = 0;
                    parcel = parcelObtain;
                    android.hardware.radio.V1_2.CellInfoCdma cellInfoCdma = next.cdma.get(0);
                    writeToParcelForCdma(parcel, cellInfoCdma.cellIdentityCdma.base.networkId, cellInfoCdma.cellIdentityCdma.base.systemId, cellInfoCdma.cellIdentityCdma.base.baseStationId, cellInfoCdma.cellIdentityCdma.base.longitude, cellInfoCdma.cellIdentityCdma.base.latitude, cellInfoCdma.cellIdentityCdma.operatorNames.alphaLong, cellInfoCdma.cellIdentityCdma.operatorNames.alphaShort, cellInfoCdma.signalStrengthCdma.dbm, cellInfoCdma.signalStrengthCdma.ecio, cellInfoCdma.signalStrengthEvdo.dbm, cellInfoCdma.signalStrengthEvdo.ecio, cellInfoCdma.signalStrengthEvdo.signalNoiseRatio);
                    break;
                case 3:
                    android.hardware.radio.V1_2.CellInfoLte cellInfoLte = next.lte.get(0);
                    it = it2;
                    arrayList2 = arrayList3;
                    i = 0;
                    parcel = parcelObtain;
                    writeToParcelForLte(parcelObtain, cellInfoLte.cellIdentityLte.base.ci, cellInfoLte.cellIdentityLte.base.pci, cellInfoLte.cellIdentityLte.base.tac, cellInfoLte.cellIdentityLte.base.earfcn, cellInfoLte.cellIdentityLte.bandwidth, cellInfoLte.cellIdentityLte.base.mcc, cellInfoLte.cellIdentityLte.base.mnc, cellInfoLte.cellIdentityLte.operatorNames.alphaLong, cellInfoLte.cellIdentityLte.operatorNames.alphaShort, cellInfoLte.signalStrengthLte.signalStrength, cellInfoLte.signalStrengthLte.rsrp, cellInfoLte.signalStrengthLte.rsrq, cellInfoLte.signalStrengthLte.rssnr, cellInfoLte.signalStrengthLte.cqi, cellInfoLte.signalStrengthLte.timingAdvance);
                    break;
                case 4:
                    android.hardware.radio.V1_2.CellInfoWcdma cellInfoWcdma = next.wcdma.get(0);
                    writeToParcelForWcdma(parcelObtain, cellInfoWcdma.cellIdentityWcdma.base.lac, cellInfoWcdma.cellIdentityWcdma.base.cid, cellInfoWcdma.cellIdentityWcdma.base.psc, cellInfoWcdma.cellIdentityWcdma.base.uarfcn, cellInfoWcdma.cellIdentityWcdma.base.mcc, cellInfoWcdma.cellIdentityWcdma.base.mnc, cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaLong, cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaShort, cellInfoWcdma.signalStrengthWcdma.base.signalStrength, cellInfoWcdma.signalStrengthWcdma.base.bitErrorRate);
                    arrayList2 = arrayList3;
                    it = it2;
                    i = 0;
                    parcel = parcelObtain;
                    break;
                default:
                    throw new RuntimeException("unexpected cellinfotype: " + next.cellInfoType);
            }
            Parcel parcel2 = parcel;
            parcel2.setDataPosition(i);
            CellInfo cellInfo = (CellInfo) CellInfo.CREATOR.createFromParcel(parcel2);
            parcel2.recycle();
            ArrayList<CellInfo> arrayList4 = arrayList2;
            arrayList4.add(cellInfo);
            arrayList3 = arrayList4;
            it2 = it;
        }
        return arrayList3;
    }

    @VisibleForTesting
    public static SignalStrength convertHalSignalStrength(android.hardware.radio.V1_0.SignalStrength signalStrength) {
        int i;
        if (signalStrength.tdScdma.rscp >= 25 && signalStrength.tdScdma.rscp <= 120) {
            i = (-signalStrength.tdScdma.rscp) + 120;
        } else {
            i = 255;
        }
        return new SignalStrength(signalStrength.gw.signalStrength, signalStrength.gw.bitErrorRate, signalStrength.cdma.dbm, signalStrength.cdma.ecio, signalStrength.evdo.dbm, signalStrength.evdo.ecio, signalStrength.evdo.signalNoiseRatio, signalStrength.lte.signalStrength, signalStrength.lte.rsrp, signalStrength.lte.rsrq, signalStrength.lte.rssnr, signalStrength.lte.cqi, i);
    }

    @VisibleForTesting
    public static SignalStrength convertHalSignalStrength_1_2(android.hardware.radio.V1_2.SignalStrength signalStrength) {
        return new SignalStrength(signalStrength.gsm.signalStrength, signalStrength.gsm.bitErrorRate, signalStrength.cdma.dbm, signalStrength.cdma.ecio, signalStrength.evdo.dbm, signalStrength.evdo.ecio, signalStrength.evdo.signalNoiseRatio, signalStrength.lte.signalStrength, signalStrength.lte.rsrp, signalStrength.lte.rsrq, signalStrength.lte.rssnr, signalStrength.lte.cqi, signalStrength.tdScdma.rscp, signalStrength.wcdma.base.signalStrength, signalStrength.wcdma.rscp);
    }
}
