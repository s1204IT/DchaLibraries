package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.radio.V1_0.CdmaSmsWriteArgs;
import android.hardware.radio.V1_0.Dial;
import android.hardware.radio.V1_0.HardwareConfigModem;
import android.hardware.radio.V1_0.UusInfo;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.HardwareConfig;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILRequest;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.android.mms.pdu.MtkCharacterSets;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.ims.MtkDedicateDataCallResponse;
import com.mediatek.internal.telephony.ims.MtkPacketFilterInfo;
import com.mediatek.internal.telephony.ims.MtkQosStatus;
import com.mediatek.internal.telephony.ims.MtkTftParameter;
import com.mediatek.internal.telephony.ims.MtkTftStatus;
import com.mediatek.internal.telephony.phb.PBEntry;
import com.mediatek.internal.telephony.phb.PhbEntry;
import com.mediatek.internal.telephony.uicc.MtkSIMRecords;
import com.mediatek.telephony.internal.telephony.vsim.ExternalSimConstants;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.TimeZone;
import java.util.Vector;
import mediatek.telephony.MtkSmsParameters;
import vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx;
import vendor.mediatek.hardware.radio.V3_0.DedicateDataCall;
import vendor.mediatek.hardware.radio.V3_0.IRadio;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryExt;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryStructure;
import vendor.mediatek.hardware.radio.V3_0.PktFilter;
import vendor.mediatek.hardware.radio.V3_0.SimAuthStructure;
import vendor.mediatek.hardware.radio.V3_0.SmsParams;

public class MtkRIL extends RIL {
    public static final int CF_REASON_NOT_REGISTERED = 6;
    static final String RILJ_LOG_TAG = "MtkRILJ";
    public static final int SERVICE_CLASS_LINE2 = 256;
    public static final int SERVICE_CLASS_MTK_MAX = 512;
    public static final int SERVICE_CLASS_VIDEO = 512;
    private ArrayList<String> hide_plmns;
    protected RegistrantList mAttachApnChangedRegistrants;
    protected RegistrantList mBipProCmdRegistrant;
    protected Registrant mCDMACardEsnMeidRegistrant;
    protected RegistrantList mCallForwardingInfoRegistrants;
    protected Registrant mCallRelatedSuppSvcRegistrant;
    protected RegistrantList mCdmaCallAcceptedRegistrant;
    Object mCfuReturnValue;
    protected RegistrantList mCipherIndicationRegistrant;
    protected RegistrantList mCsNetworkStateRegistrants;
    protected RegistrantList mDataAllowedRegistrants;
    protected RegistrantList mDedicatedBearerActivedRegistrants;
    protected RegistrantList mDedicatedBearerDeactivatedRegistrants;
    protected RegistrantList mDedicatedBearerModifiedRegistrants;
    protected RegistrantList mDsbpStateRegistrant;
    DtmfQueueHandler mDtmfReqQueue;
    protected RegistrantList mEconfSrvccRegistrants;
    protected Object mEcopsReturnValue;
    protected RegistrantList mEmbmsAtInfoNotificationRegistrant;
    protected RegistrantList mEmbmsSessionStatusNotificationRegistrant;
    protected Object mEmsrReturnValue;
    protected Object mEspOrMeid;
    protected Registrant mEtwsNotificationRegistrant;
    protected RegistrantList mFemtoCellInfoRegistrants;
    protected RegistrantList mGmssRatChangedRegistrant;
    protected RegistrantList mImeiLockRegistrant;
    protected RegistrantList mImsiRefreshDoneRegistrant;
    protected Registrant mIncomingCallIndicationRegistrant;
    public Integer mInstanceId;
    BroadcastReceiver mIntentReceiver;
    protected RegistrantList mInvalidSimInfoRegistrant;
    public boolean mIsSmsReady;
    protected RegistrantList mLteAccessStratumStateRegistrants;
    protected RegistrantList mMccMncRegistrants;
    protected RegistrantList mMdDataRetryCountResetRegistrants;
    protected Registrant mMeSmsFullRegistrant;
    protected RegistrantList mModulationRegistrants;
    protected Context mMtkContext;
    boolean mMtkRilJIntiDone;
    private IMtkRilOp mMtkRilOp;
    protected RegistrantList mNetworkEventRegistrants;
    protected RegistrantList mNetworkInfoRegistrant;
    protected RegistrantList mNetworkRejectRegistrants;
    protected RegistrantList mPcoDataAfterAttachedRegistrants;
    public RegistrantList mPhbReadyRegistrants;
    protected RegistrantList mPlmnChangeNotificationRegistrant;
    protected RegistrantList mPsNetworkStateRegistrants;
    protected RegistrantList mPseudoCellInfoRegistrants;
    MtkRadioIndication mRadioIndicationMtk;
    volatile IRadio mRadioProxyMtk;
    MtkRadioResponse mRadioResponseMtk;
    protected Registrant mRegistrationSuspendedRegistrant;
    protected RegistrantList mRemoveRestrictEutranRegistrants;
    protected RegistrantList mResetAttachApnRegistrants;
    protected RegistrantList mSignalStrengthWithWcdmaEcioRegistrants;
    protected RegistrantList mSimCommonSlotNoChanged;
    protected RegistrantList mSimMissing;
    protected RegistrantList mSimPlugIn;
    protected RegistrantList mSimPlugOut;
    protected RegistrantList mSimRecovery;
    protected RegistrantList mSimTrayPlugIn;
    Object mSmlSlotLockInfo;
    protected RegistrantList mSmlSlotLockInfoChanged;
    protected RegistrantList mSmsReadyRegistrants;
    protected Registrant mSpeechCodecInfoRegistrant;
    protected RegistrantList mStkSetupMenuResetRegistrant;
    protected RegistrantList mTxPowerRegistrant;
    protected RegistrantList mTxPowerStatusRegistrant;
    protected Registrant mUnsolOemHookRegistrant;
    protected RegistrantList mVirtualSimOff;
    protected RegistrantList mVirtualSimOn;
    protected RegistrantList mVsimIndicationRegistrants;
    protected Object mWPMonitor;

    class DtmfQueueHandler {
        private boolean mDtmfStatus;
        public final int MAXIMUM_DTMF_REQUEST = 32;
        private final boolean DTMF_STATUS_START = true;
        private final boolean DTMF_STATUS_STOP = false;
        private Vector mDtmfQueue = new Vector(32);
        private DtmfQueueRR mPendingCHLDRequest = null;
        private boolean mIsSendChldRequest = false;

        public class DtmfQueueRR {
            public Object[] params;
            public RILRequest rr;

            public DtmfQueueRR(RILRequest rILRequest, Object[] objArr) {
                this.rr = rILRequest;
                this.params = objArr;
            }
        }

        public DtmfQueueHandler() {
            this.mDtmfStatus = false;
            this.mDtmfStatus = false;
        }

        public void start() {
            this.mDtmfStatus = true;
        }

        public void stop() {
            this.mDtmfStatus = false;
        }

        public boolean isStart() {
            return this.mDtmfStatus;
        }

        public void add(DtmfQueueRR dtmfQueueRR) {
            this.mDtmfQueue.addElement(dtmfQueueRR);
        }

        public void remove(DtmfQueueRR dtmfQueueRR) {
            this.mDtmfQueue.remove(dtmfQueueRR);
        }

        public void remove(int i) {
            this.mDtmfQueue.removeElementAt(i);
        }

        public DtmfQueueRR get() {
            return (DtmfQueueRR) this.mDtmfQueue.get(0);
        }

        public int size() {
            return this.mDtmfQueue.size();
        }

        public void setPendingRequest(DtmfQueueRR dtmfQueueRR) {
            this.mPendingCHLDRequest = dtmfQueueRR;
        }

        public DtmfQueueRR getPendingRequest() {
            return this.mPendingCHLDRequest;
        }

        public void setSendChldRequest() {
            this.mIsSendChldRequest = true;
        }

        public void resetSendChldRequest() {
            this.mIsSendChldRequest = false;
        }

        public boolean hasSendChldRequest() {
            MtkRIL.this.mtkRiljLog("mIsSendChldRequest = " + this.mIsSendChldRequest);
            return this.mIsSendChldRequest;
        }

        public DtmfQueueRR buildDtmfQueueRR(RILRequest rILRequest, Object[] objArr) {
            if (rILRequest == null) {
                return null;
            }
            MtkRIL.this.mtkRiljLog("DtmfQueueHandler.buildDtmfQueueRR build ([" + rILRequest.mSerial + "] reqId=" + rILRequest.mRequest + ")");
            return new DtmfQueueRR(rILRequest, objArr);
        }
    }

    @VisibleForTesting
    public MtkRIL() {
        this.mRadioProxyMtk = null;
        this.mMtkRilJIntiDone = false;
        this.mCdmaCallAcceptedRegistrant = new RegistrantList();
        this.mCipherIndicationRegistrant = new RegistrantList();
        this.mFemtoCellInfoRegistrants = new RegistrantList();
        this.mEmbmsSessionStatusNotificationRegistrant = new RegistrantList();
        this.mEmbmsAtInfoNotificationRegistrant = new RegistrantList();
        this.mPhbReadyRegistrants = new RegistrantList();
        this.mCallForwardingInfoRegistrants = new RegistrantList();
        this.mTxPowerRegistrant = new RegistrantList();
        this.mTxPowerStatusRegistrant = new RegistrantList();
        this.mCfuReturnValue = null;
        this.mMtkRilOp = null;
        this.mDtmfReqQueue = new DtmfQueueHandler();
        this.mPlmnChangeNotificationRegistrant = new RegistrantList();
        this.mEmsrReturnValue = null;
        this.mEcopsReturnValue = null;
        this.mWPMonitor = new Object();
        this.mGmssRatChangedRegistrant = new RegistrantList();
        this.mResetAttachApnRegistrants = new RegistrantList();
        this.mAttachApnChangedRegistrants = new RegistrantList();
        this.mPcoDataAfterAttachedRegistrants = new RegistrantList();
        this.mRemoveRestrictEutranRegistrants = new RegistrantList();
        this.mMdDataRetryCountResetRegistrants = new RegistrantList();
        this.mIntentReceiver = new BroadcastReceiver() {
            private static final int MODE_CDMA_ASSERT = 31;
            private static final int MODE_CDMA_RESET = 32;
            private static final int MODE_CDMA_RILD_NE = 103;
            private static final int MODE_GSM_RILD_NE = 101;
            private static final int MODE_PHONE_PROCESS_JE = 100;

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.mtk.TEST_TRM")) {
                    int intExtra = intent.getIntExtra("mode", 2);
                    Rlog.d(MtkRIL.RILJ_LOG_TAG, "RIL received com.mtk.TEST_TRM, mode = " + intExtra + ", mInstanceIds = " + MtkRIL.this.mInstanceId);
                    if (intExtra == 100) {
                        throw new RuntimeException("UserTriggerPhoneJE");
                    }
                    MtkRIL.this.setTrm(intExtra, null);
                    return;
                }
                Rlog.w(MtkRIL.RILJ_LOG_TAG, "RIL received unexpected Intent: " + intent.getAction());
            }
        };
        this.hide_plmns = new ArrayList<>();
        this.mCsNetworkStateRegistrants = new RegistrantList();
        this.mSignalStrengthWithWcdmaEcioRegistrants = new RegistrantList();
        this.mVirtualSimOn = new RegistrantList();
        this.mVirtualSimOff = new RegistrantList();
        this.mImeiLockRegistrant = new RegistrantList();
        this.mImsiRefreshDoneRegistrant = new RegistrantList();
        this.mInvalidSimInfoRegistrant = new RegistrantList();
        this.mNetworkEventRegistrants = new RegistrantList();
        this.mNetworkRejectRegistrants = new RegistrantList();
        this.mModulationRegistrants = new RegistrantList();
        this.mIsSmsReady = false;
        this.mSmsReadyRegistrants = new RegistrantList();
        this.mEspOrMeid = null;
        this.mPsNetworkStateRegistrants = new RegistrantList();
        this.mNetworkInfoRegistrant = new RegistrantList();
        this.mDataAllowedRegistrants = new RegistrantList();
        this.mPseudoCellInfoRegistrants = new RegistrantList();
        this.mSimTrayPlugIn = new RegistrantList();
        this.mSimCommonSlotNoChanged = new RegistrantList();
        this.mBipProCmdRegistrant = new RegistrantList();
        this.mStkSetupMenuResetRegistrant = new RegistrantList();
        this.mLteAccessStratumStateRegistrants = new RegistrantList();
        this.mSimPlugIn = new RegistrantList();
        this.mSimPlugOut = new RegistrantList();
        this.mSimMissing = new RegistrantList();
        this.mSimRecovery = new RegistrantList();
        this.mSmlSlotLockInfoChanged = new RegistrantList();
        this.mSmlSlotLockInfo = null;
        this.mEconfSrvccRegistrants = new RegistrantList();
        this.mMccMncRegistrants = new RegistrantList();
        this.mVsimIndicationRegistrants = new RegistrantList();
        this.mDedicatedBearerActivedRegistrants = new RegistrantList();
        this.mDedicatedBearerModifiedRegistrants = new RegistrantList();
        this.mDedicatedBearerDeactivatedRegistrants = new RegistrantList();
        this.mDsbpStateRegistrant = new RegistrantList();
    }

    public MtkRIL(Context context, int i, int i2, Integer num) {
        super(context, i, i2, num);
        this.mRadioProxyMtk = null;
        this.mMtkRilJIntiDone = false;
        this.mCdmaCallAcceptedRegistrant = new RegistrantList();
        this.mCipherIndicationRegistrant = new RegistrantList();
        this.mFemtoCellInfoRegistrants = new RegistrantList();
        this.mEmbmsSessionStatusNotificationRegistrant = new RegistrantList();
        this.mEmbmsAtInfoNotificationRegistrant = new RegistrantList();
        this.mPhbReadyRegistrants = new RegistrantList();
        this.mCallForwardingInfoRegistrants = new RegistrantList();
        this.mTxPowerRegistrant = new RegistrantList();
        this.mTxPowerStatusRegistrant = new RegistrantList();
        this.mCfuReturnValue = null;
        this.mMtkRilOp = null;
        this.mDtmfReqQueue = new DtmfQueueHandler();
        this.mPlmnChangeNotificationRegistrant = new RegistrantList();
        this.mEmsrReturnValue = null;
        this.mEcopsReturnValue = null;
        this.mWPMonitor = new Object();
        this.mGmssRatChangedRegistrant = new RegistrantList();
        this.mResetAttachApnRegistrants = new RegistrantList();
        this.mAttachApnChangedRegistrants = new RegistrantList();
        this.mPcoDataAfterAttachedRegistrants = new RegistrantList();
        this.mRemoveRestrictEutranRegistrants = new RegistrantList();
        this.mMdDataRetryCountResetRegistrants = new RegistrantList();
        this.mIntentReceiver = new BroadcastReceiver() {
            private static final int MODE_CDMA_ASSERT = 31;
            private static final int MODE_CDMA_RESET = 32;
            private static final int MODE_CDMA_RILD_NE = 103;
            private static final int MODE_GSM_RILD_NE = 101;
            private static final int MODE_PHONE_PROCESS_JE = 100;

            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("com.mtk.TEST_TRM")) {
                    int intExtra = intent.getIntExtra("mode", 2);
                    Rlog.d(MtkRIL.RILJ_LOG_TAG, "RIL received com.mtk.TEST_TRM, mode = " + intExtra + ", mInstanceIds = " + MtkRIL.this.mInstanceId);
                    if (intExtra == 100) {
                        throw new RuntimeException("UserTriggerPhoneJE");
                    }
                    MtkRIL.this.setTrm(intExtra, null);
                    return;
                }
                Rlog.w(MtkRIL.RILJ_LOG_TAG, "RIL received unexpected Intent: " + intent.getAction());
            }
        };
        this.hide_plmns = new ArrayList<>();
        this.mCsNetworkStateRegistrants = new RegistrantList();
        this.mSignalStrengthWithWcdmaEcioRegistrants = new RegistrantList();
        this.mVirtualSimOn = new RegistrantList();
        this.mVirtualSimOff = new RegistrantList();
        this.mImeiLockRegistrant = new RegistrantList();
        this.mImsiRefreshDoneRegistrant = new RegistrantList();
        this.mInvalidSimInfoRegistrant = new RegistrantList();
        this.mNetworkEventRegistrants = new RegistrantList();
        this.mNetworkRejectRegistrants = new RegistrantList();
        this.mModulationRegistrants = new RegistrantList();
        this.mIsSmsReady = false;
        this.mSmsReadyRegistrants = new RegistrantList();
        this.mEspOrMeid = null;
        this.mPsNetworkStateRegistrants = new RegistrantList();
        this.mNetworkInfoRegistrant = new RegistrantList();
        this.mDataAllowedRegistrants = new RegistrantList();
        this.mPseudoCellInfoRegistrants = new RegistrantList();
        this.mSimTrayPlugIn = new RegistrantList();
        this.mSimCommonSlotNoChanged = new RegistrantList();
        this.mBipProCmdRegistrant = new RegistrantList();
        this.mStkSetupMenuResetRegistrant = new RegistrantList();
        this.mLteAccessStratumStateRegistrants = new RegistrantList();
        this.mSimPlugIn = new RegistrantList();
        this.mSimPlugOut = new RegistrantList();
        this.mSimMissing = new RegistrantList();
        this.mSimRecovery = new RegistrantList();
        this.mSmlSlotLockInfoChanged = new RegistrantList();
        this.mSmlSlotLockInfo = null;
        this.mEconfSrvccRegistrants = new RegistrantList();
        this.mMccMncRegistrants = new RegistrantList();
        this.mVsimIndicationRegistrants = new RegistrantList();
        this.mDedicatedBearerActivedRegistrants = new RegistrantList();
        this.mDedicatedBearerModifiedRegistrants = new RegistrantList();
        this.mDedicatedBearerDeactivatedRegistrants = new RegistrantList();
        this.mDsbpStateRegistrant = new RegistrantList();
        Rlog.d(RILJ_LOG_TAG, "constructor: sub = " + num);
        this.mMtkContext = context;
        this.mInstanceId = num;
        this.mRadioResponseMtk = new MtkRadioResponse(this);
        this.mRadioIndicationMtk = new MtkRadioIndication(this);
        if (num.intValue() == 0) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.mtk.TEST_TRM");
            context.registerReceiver(this.mIntentReceiver, intentFilter);
        }
        this.mMtkRilJIntiDone = true;
        getRadioProxy((Message) null);
        getRilOp();
    }

    public IRadio getRadioProxy(Message message) {
        if (!this.mMtkRilJIntiDone) {
            return null;
        }
        if (!this.mIsMobileNetworkSupported) {
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(1));
                message.sendToTarget();
            }
            return null;
        }
        if (this.mRadioProxyMtk != null) {
            return this.mRadioProxyMtk;
        }
        try {
            this.mRadioProxyMtk = IRadio.getService(HIDL_SERVICE_NAME[this.mPhoneId == null ? 0 : this.mPhoneId.intValue()], false);
            if (this.mRadioProxyMtk != null) {
                this.mRadioProxyMtk.linkToDeath(this.mRadioProxyDeathRecipient, this.mRadioProxyCookie.incrementAndGet());
                this.mRadioProxyMtk.setResponseFunctionsMtk(this.mRadioResponseMtk, this.mRadioIndicationMtk);
                this.mRadioProxyMtk.setResponseFunctions(this.mRadioResponse, this.mRadioIndication);
                if (this.mDtmfReqQueue != null) {
                    synchronized (this.mDtmfReqQueue) {
                        Rlog.d(RILJ_LOG_TAG, "queue size  " + this.mDtmfReqQueue.size());
                        for (int size = this.mDtmfReqQueue.size() - 1; size >= 0; size--) {
                            this.mDtmfReqQueue.remove(size);
                        }
                        if (this.mDtmfReqQueue.getPendingRequest() != null) {
                            Rlog.d(RILJ_LOG_TAG, "reset pending switch request");
                            RILRequest rILRequest = this.mDtmfReqQueue.getPendingRequest().rr;
                            if (rILRequest.mResult != null) {
                                AsyncResult.forMessage(rILRequest.mResult, (Object) null, (Throwable) null);
                                rILRequest.mResult.sendToTarget();
                            }
                            this.mDtmfReqQueue.resetSendChldRequest();
                            this.mDtmfReqQueue.setPendingRequest(null);
                        }
                    }
                }
            } else {
                Rlog.e(RILJ_LOG_TAG, "getRadioProxy: mRadioProxy == null");
            }
        } catch (RemoteException | RuntimeException e) {
            this.mRadioProxyMtk = null;
            Rlog.e(RILJ_LOG_TAG, "RadioProxy getService/setResponseFunctions: " + e);
        }
        if (this.mRadioProxyMtk == null) {
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(1));
                message.sendToTarget();
            }
            this.mRilHandler.sendMessageDelayed(this.mRilHandler.obtainMessage(6, Long.valueOf(this.mRadioProxyCookie.get())), 4000L);
        }
        return this.mRadioProxyMtk;
    }

    public IMtkRilOp getRilOp() {
        Rlog.d(RILJ_LOG_TAG, "getRilOp");
        if (this.mMtkRilOp != null) {
            return this.mMtkRilOp;
        }
        if ("0".equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, "0"))) {
            Rlog.d(RILJ_LOG_TAG, "mMtkRilOp init fail, because OM load");
            return null;
        }
        try {
            Class<?> cls = Class.forName("com.mediatek.opcommon.telephony.MtkRilOp");
            Rlog.d(RILJ_LOG_TAG, "class = " + cls);
            Constructor<?> constructor = cls.getConstructor(Context.class, Integer.TYPE, Integer.TYPE, Integer.class);
            Rlog.d(RILJ_LOG_TAG, "constructor function = " + constructor);
            this.mMtkRilOp = (IMtkRilOp) constructor.newInstance(this.mContext, Integer.valueOf(this.mPreferredNetworkType), Integer.valueOf(this.mCdmaSubscription), this.mPhoneId);
            return this.mMtkRilOp;
        } catch (Exception e) {
            Rlog.d(RILJ_LOG_TAG, "mMtkRilOp init fail");
            e.printStackTrace();
            return null;
        }
    }

    protected void resetProxyAndRequestList() {
        this.mRadioProxyMtk = null;
        super.resetProxyAndRequestList();
    }

    public static String requestToStringEx(Integer num) {
        String str;
        int iIntValue = num.intValue();
        switch (iIntValue) {
            case 59:
                str = "OEM_HOOK_RAW";
                break;
            case 60:
                str = "OEM_HOOK_STRINGS";
                break;
            default:
                switch (iIntValue) {
                    case MtkGsmCdmaPhone.EVENT_USSI_CSFB:
                        str = "RIL_REQUEST_MODEM_POWERON";
                        break;
                    case MtkGsmCdmaPhone.EVENT_GET_CLIR_COMPLETE:
                        str = "RIL_REQUEST_MODEM_POWEROFF";
                        break;
                    case MtkGsmCdmaPhone.EVENT_SET_CALL_BARRING_COMPLETE:
                        str = "SET_NETWORK_SELECTION_MANUAL_WITH_ACT";
                        break;
                    case MtkGsmCdmaPhone.EVENT_GET_CALL_BARRING_COMPLETE:
                        str = "QUERY_AVAILABLE_NETWORKS_WITH_ACT";
                        break;
                    case 2007:
                        str = "ABORT_QUERY_AVAILABLE_NETWORKS";
                        break;
                    default:
                        switch (iIntValue) {
                            case 2009:
                                str = "RIL_REQUEST_GSM_SET_BROADCAST_LANGUAGE";
                                break;
                            case 2010:
                                str = "RIL_REQUEST_GSM_GET_BROADCAST_LANGUAGE";
                                break;
                            case 2011:
                                str = "RIL_REQUEST_GET_SMS_SIM_MEM_STATUS";
                                break;
                            case 2012:
                                str = "RIL_REQUEST_GET_SMS_PARAMS";
                                break;
                            case 2013:
                                str = "RIL_REQUEST_SET_SMS_PARAMS";
                                break;
                            case 2014:
                                str = "RIL_REQUEST_SET_ETWS";
                                break;
                            case 2015:
                                str = "RIL_REQUEST_REMOVE_CB_MESSAGE";
                                break;
                            case 2016:
                                str = "SET_CALL_INDICATION";
                                break;
                            case 2017:
                                str = "EMERGENCY_DIAL";
                                break;
                            case 2018:
                                str = "SET_ECC_SERVICE_CATEGORY";
                                break;
                            case 2019:
                                str = "HANGUP_ALL";
                                break;
                            default:
                                switch (iIntValue) {
                                    case 2021:
                                        str = "RIL_REQUEST_SET_PSEUDO_CELL_MODE";
                                        break;
                                    case 2022:
                                        str = "RIL_REQUEST_GET_PSEUDO_CELL_INFO";
                                        break;
                                    case 2023:
                                        str = "RIL_REQUEST_SWITCH_MODE_FOR_ECC";
                                        break;
                                    case 2024:
                                        str = "RIL_REQUEST_GET_SMS_RUIM_MEM_STATUS";
                                        break;
                                    case MtkCharacterSets.GB_2312:
                                        str = "RIL_REQUEST_SET_FD_MODE";
                                        break;
                                    case 2026:
                                        str = "RIL_REQUEST_RELOAD_MODEM_TYPE";
                                        break;
                                    case MtkCharacterSets.MACINTOSH:
                                        str = "RIL_REQUEST_STORE_MODEM_TYPE";
                                        break;
                                    case 2028:
                                        str = "RIL_REQUEST_SET_TRM";
                                        break;
                                    default:
                                        switch (iIntValue) {
                                            case 2035:
                                                str = "CURRENT_STATUS";
                                                break;
                                            case 2036:
                                                str = "RIL_REQUEST_QUERY_PHB_STORAGE_INFO";
                                                break;
                                            case 2037:
                                                str = "RIL_REQUEST_WRITE_PHB_ENTRY";
                                                break;
                                            case 2038:
                                                str = "RIL_REQUEST_READ_PHB_ENTRY";
                                                break;
                                            case 2039:
                                                str = "RIL_REQUEST_QUERY_UPB_CAPABILITY";
                                                break;
                                            case 2040:
                                                str = "RIL_REQUEST_EDIT_UPB_ENTRY";
                                                break;
                                            case 2041:
                                                str = "RIL_REQUEST_DELETE_UPB_ENTRY";
                                                break;
                                            case 2042:
                                                str = "RIL_REQUEST_READ_UPB_GAS_LIST";
                                                break;
                                            case 2043:
                                                str = "RIL_REQUEST_READ_UPB_GRP";
                                                break;
                                            case 2044:
                                                str = "RIL_REQUEST_WRITE_UPB_GRP";
                                                break;
                                            case 2045:
                                                str = "RIL_REQUEST_GET_PHB_STRING_LENGTH";
                                                break;
                                            case 2046:
                                                str = "RIL_REQUEST_GET_PHB_MEM_STORAGE";
                                                break;
                                            case 2047:
                                                str = "RIL_REQUEST_SET_PHB_MEM_STORAGE";
                                                break;
                                            case 2048:
                                                str = "RIL_REQUEST_READ_PHB_ENTRY_EXT";
                                                break;
                                            case 2049:
                                                str = "RIL_REQUEST_WRITE_PHB_ENTRY_EXT";
                                                break;
                                            case 2050:
                                                str = "RIL_REQUEST_QUERY_UPB_AVAILABLE";
                                                break;
                                            case MtkCharacterSets.CP864:
                                                str = "RIL_REQUEST_READ_EMAIL_ENTRY";
                                                break;
                                            case 2052:
                                                str = "RIL_REQUEST_READ_SNE_ENTRY";
                                                break;
                                            case 2053:
                                                str = "RIL_REQUEST_READ_ANR_ENTRY";
                                                break;
                                            case 2054:
                                                str = "RIL_REQUEST_READ_UPB_AAS_LIST";
                                                break;
                                            case 2055:
                                                str = "REQUEST_GET_FEMTOCELL_LIST";
                                                break;
                                            case 2056:
                                                str = "REQUEST_ABORT_FEMTOCELL_LIST";
                                                break;
                                            case 2057:
                                                str = "REQUEST_SELECT_FEMTOCELL";
                                                break;
                                            case 2058:
                                                str = "REQUEST_QUERY_FEMTOCELL_SYSTEM_SELECTION_MODE";
                                                break;
                                            case 2059:
                                                str = "REQUEST_SET_FEMTOCELL_SYSTEM_SELECTION_MODE";
                                                break;
                                            case 2060:
                                                str = "RIL_REQUEST_EMBMS_AT_CMD";
                                                break;
                                            default:
                                                switch (iIntValue) {
                                                    case 2062:
                                                        str = "RIL_REQUEST_SYNC_DATA_SETTINGS_TO_MD";
                                                        break;
                                                    case 2063:
                                                        str = "RIL_REQUEST_RESET_MD_DATA_RETRY_COUNT";
                                                        break;
                                                    default:
                                                        switch (iIntValue) {
                                                            case 2065:
                                                                str = "RIL_REQUEST_SET_LTE_ACCESS_STRATUM_REPORT";
                                                                break;
                                                            case 2066:
                                                                str = "RIL_REQUEST_SET_LTE_UPLINK_DATA_TRANSFER";
                                                                break;
                                                            case 2067:
                                                                str = "RIL_REQUEST_QUERY_SIM_NETWORK_LOCK";
                                                                break;
                                                            case 2068:
                                                                str = "RIL_REQUEST_SET_SIM_NETWORK_LOCK";
                                                                break;
                                                            default:
                                                                switch (iIntValue) {
                                                                    case 2100:
                                                                        str = "RIL_REQUEST_SET_REMOVE_RESTRICT_EUTRAN_MODE";
                                                                        break;
                                                                    case MtkCharacterSets.BIG5_HKSCS:
                                                                        str = "RIL_REQUEST_VSS_ANTENNA_CONF";
                                                                        break;
                                                                    case 2102:
                                                                        str = "RIL_REQUEST_VSS_ANTENNA_INFO";
                                                                        break;
                                                                    case 2103:
                                                                        str = "RIL_REQUEST_SET_CLIP";
                                                                        break;
                                                                    case 2104:
                                                                        str = "RIL_REQUEST_GET_COLP";
                                                                        break;
                                                                    case 2105:
                                                                        str = "RIL_REQUEST_GET_COLR";
                                                                        break;
                                                                    case 2106:
                                                                        str = "RIL_REQUEST_SEND_CNAP";
                                                                        break;
                                                                    case 2107:
                                                                        str = "RIL_REQUEST_GET_POL_CAPABILITY";
                                                                        break;
                                                                    case 2108:
                                                                        str = "RIL_REQUEST_GET_POL_LIST";
                                                                        break;
                                                                    case 2109:
                                                                        str = "RIL_REQUEST_SET_POL_ENTRY";
                                                                        break;
                                                                    case 2110:
                                                                        str = "ECC_PREFERRED_RAT";
                                                                        break;
                                                                    case 2111:
                                                                        str = "SET_ROAMING_ENABLE";
                                                                        break;
                                                                    case 2112:
                                                                        str = "GET_ROAMING_ENABLE";
                                                                        break;
                                                                    case 2113:
                                                                        str = "RIL_REQUEST_VSIM_NOTIFICATION";
                                                                        break;
                                                                    case 2114:
                                                                        str = "RIL_REQUEST_VSIM_OPERATION";
                                                                        break;
                                                                    case 2115:
                                                                        str = "RIL_REQUEST_GET_GSM_SMS_BROADCAST_ACTIVATION";
                                                                        break;
                                                                    case 2116:
                                                                        str = "RIL_REQUEST_SET_WIFI_ENABLED";
                                                                        break;
                                                                    case 2117:
                                                                        str = "RIL_REQUEST_SET_WIFI_ASSOCIATED";
                                                                        break;
                                                                    case 2118:
                                                                        str = "RIL_REQUEST_SET_WIFI_SIGNAL_LEVEL";
                                                                        break;
                                                                    case 2119:
                                                                        str = "RIL_REQUEST_SET_WIFI_IP_ADDRESS";
                                                                        break;
                                                                    case 2120:
                                                                        str = "RIL_REQUEST_SET_GEO_LOCATION";
                                                                        break;
                                                                    case 2121:
                                                                        str = "RIL_REQUEST_SET_EMERGENCY_ADDRESS_ID";
                                                                        break;
                                                                    default:
                                                                        switch (iIntValue) {
                                                                            case 2123:
                                                                                str = "RIL_REQUEST_SET_COLP";
                                                                                break;
                                                                            case 2124:
                                                                                str = "RIL_REQUEST_SET_COLR";
                                                                                break;
                                                                            case 2125:
                                                                                str = "RIL_REQUEST_QUERY_CALL_FORWARD_IN_TIME_SLOT";
                                                                                break;
                                                                            case 2126:
                                                                                str = "RIL_REQUEST_SET_CALL_FORWARD_IN_TIME_SLOT";
                                                                                break;
                                                                            default:
                                                                                switch (iIntValue) {
                                                                                    case 2129:
                                                                                        str = "RIL_REQUEST_SET_E911_STATE";
                                                                                        break;
                                                                                    case 2130:
                                                                                        str = "RIL_REQUEST_SET_SERVICE_STATE";
                                                                                        break;
                                                                                    default:
                                                                                        switch (iIntValue) {
                                                                                            case 2133:
                                                                                                str = "RIL_REQUEST_IMS_SEND_SMS_EX";
                                                                                                break;
                                                                                            case 2134:
                                                                                                str = "RIL_REQUEST_SET_SMS_FWK_READY";
                                                                                                break;
                                                                                            default:
                                                                                                switch (iIntValue) {
                                                                                                    case 2144:
                                                                                                        str = "RIL_REQUEST_DATA_CONNECTION_ATTACH";
                                                                                                        break;
                                                                                                    case 2145:
                                                                                                        str = "RIL_REQUEST_DATA_CONNECTION_DETACH";
                                                                                                        break;
                                                                                                    case 2146:
                                                                                                        str = "RIL_REQUEST_RESET_ALL_CONNECTIONS";
                                                                                                        break;
                                                                                                    case 2147:
                                                                                                        str = "RIL_REQUEST_SET_VOICE_PREFER_STATUS";
                                                                                                        break;
                                                                                                    case 2148:
                                                                                                        str = "RIL_REQUEST_SET_ECC_NUM";
                                                                                                        break;
                                                                                                    case 2149:
                                                                                                        str = "RIL_REQUEST_GET_ECC_NUM";
                                                                                                        break;
                                                                                                    case 2150:
                                                                                                        str = "RIL_REQUEST_RESTART_RILD";
                                                                                                        break;
                                                                                                    case 2151:
                                                                                                        str = "RIL_REQUEST_SET_LTE_RELEASE_VERSION";
                                                                                                        break;
                                                                                                    case 2152:
                                                                                                        str = "RIL_REQUEST_GET_LTE_RELEASE_VERSION";
                                                                                                        break;
                                                                                                    case 2153:
                                                                                                        str = "RIL_REQUEST_SIGNAL_STRENGTH_WITH_WCDMA_ECIO";
                                                                                                        break;
                                                                                                    case 2154:
                                                                                                        str = "RIL_REQUEST_REPORT_AIRPLANE_MODE";
                                                                                                        break;
                                                                                                    case 2155:
                                                                                                        str = "RIL_REQUEST_REPORT_SIM_MODE";
                                                                                                        break;
                                                                                                    case 2156:
                                                                                                        str = "RIL_REQUEST_SET_SILENT_REBOOT";
                                                                                                        break;
                                                                                                    case 2157:
                                                                                                        str = "RIL_REQUEST_SET_PHONEBOOK_READY";
                                                                                                        break;
                                                                                                    case 2158:
                                                                                                        str = "RIL_REQUEST_SET_TX_POWER_STATUS";
                                                                                                        break;
                                                                                                    case 2159:
                                                                                                        str = "RIL_REQUEST_SETPROP_IMS_HANDOVER";
                                                                                                        break;
                                                                                                    case 2160:
                                                                                                        str = "RIL_REQUEST_SET_PDN_REUSE";
                                                                                                        break;
                                                                                                    case 2161:
                                                                                                        str = "RIL_REQUEST_SET_OVERRIDE_APN";
                                                                                                        break;
                                                                                                    case 2162:
                                                                                                        str = "RIL_REQUEST_SET_PDN_NAME_REUSE";
                                                                                                        break;
                                                                                                    default:
                                                                                                        switch (iIntValue) {
                                                                                                            case 2168:
                                                                                                                str = "RIL_REQUEST_SET_SS_PROPERTY";
                                                                                                                break;
                                                                                                            case 2169:
                                                                                                                str = "RIL_REQUEST_GET_SS_PROPERTY";
                                                                                                                break;
                                                                                                            default:
                                                                                                                switch (iIntValue) {
                                                                                                                    case MtkGsmCdmaPhone.EVENT_IMS_UT_DONE:
                                                                                                                        str = "RIL_REQUEST_RESUME_REGISTRATION";
                                                                                                                        break;
                                                                                                                    case 2030:
                                                                                                                        str = "RIL_REQUEST_SET_ECC_LIST";
                                                                                                                        break;
                                                                                                                    case MtkCallFailCause.CM_MM_RR_CONNECTION_RELEASE:
                                                                                                                        str = "RIL_REQUEST_SET_OPERATOR_CONFIGURATION";
                                                                                                                        break;
                                                                                                                    case 2171:
                                                                                                                        str = "RIL_REQUEST_ENTER_DEVICE_NETWORK_DEPERSONALIZATION";
                                                                                                                        break;
                                                                                                                    default:
                                                                                                                        str = "<unknown request> " + num;
                                                                                                                        break;
                                                                                                                }
                                                                                                                break;
                                                                                                        }
                                                                                                        break;
                                                                                                }
                                                                                                break;
                                                                                        }
                                                                                        break;
                                                                                }
                                                                                break;
                                                                        }
                                                                        break;
                                                                }
                                                                break;
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                        break;
                                }
                                break;
                        }
                        break;
                }
                break;
        }
        return "MTK: " + str;
    }

    public static String responseToStringEx(Integer num) {
        String str;
        int iIntValue = num.intValue();
        switch (iIntValue) {
            case 3000:
                str = "RIL_UNSOL_RESPONSE_PLMN_CHANGED";
                break;
            case IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS:
                str = "RIL_UNSOL_RESPONSE_REGISTRATION_SUSPENDED";
                break;
            default:
                switch (iIntValue) {
                    case 3014:
                        str = "RIL_UNSOL_DATA_ALLOWED";
                        break;
                    case 3015:
                        str = "UNSOL_INCOMING_CALL_INDICATION";
                        break;
                    case 3016:
                        str = "RIL_UNSOL_INVALID_SIM";
                        break;
                    case 3017:
                        str = "RIL_UNSOL_PSEUDO_CELL_INFO";
                        break;
                    case 3018:
                        str = "RIL_UNSOL_NETWORK_EVENT";
                        break;
                    case 3019:
                        str = "RIL_UNSOL_MODULATION_INFO";
                        break;
                    case 3020:
                        str = "RIL_UNSOL_RESET_ATTACH_APN";
                        break;
                    case 3021:
                        str = "RIL_UNSOL_DATA_ATTACH_APN_CHANGED";
                        break;
                    case 3022:
                        str = "RIL_UNSOL_WORLD_MODE_CHANGED";
                        break;
                    case 3023:
                        str = "RIL_UNSOL_CDMA_CARD_INITIAL_ESN_OR_MEID";
                        break;
                    case 3024:
                        str = "UNSOL_CIPHER_INDICATION";
                        break;
                    case 3025:
                        str = "UNSOL_CRSS_NOTIFICATION";
                        break;
                    default:
                        switch (iIntValue) {
                            case 3027:
                                str = "UNSOL_SPEECH_CODEC_INFO";
                                break;
                            case 3028:
                                str = "UNSOL_PHB_READY_NOTIFICATION";
                                break;
                            case 3029:
                                str = "UNSOL_FEMTOCELL_INFO";
                                break;
                            case 3030:
                                str = "UNSOL_NETWORK_INFO";
                                break;
                            default:
                                switch (iIntValue) {
                                    case 3053:
                                        str = "RIL_UNSOL_PCO_DATA_AFTER_ATTACHED";
                                        break;
                                    case 3054:
                                        str = "RIL_UNSOL_EMBMS_SESSION_STATUS";
                                        break;
                                    case 3055:
                                        str = "RIL_UNSOL_EMBMS_AT_INFO";
                                        break;
                                    default:
                                        switch (iIntValue) {
                                            case 3059:
                                                str = "RIL_UNSOL_MD_DATA_RETRY_COUNT_RESET";
                                                break;
                                            case 3060:
                                                str = "RIL_UNSOL_REMOVE_RESTRICT_EUTRAN";
                                                break;
                                            default:
                                                switch (iIntValue) {
                                                    case 3069:
                                                        str = "UNSOL_CDMA_CALL_ACCEPTED";
                                                        break;
                                                    case 3070:
                                                        str = "UNSOL_CALL_FORWARDING";
                                                        break;
                                                    default:
                                                        switch (iIntValue) {
                                                            case 3074:
                                                                str = "RIL_UNSOL_VSIM_OPERATION_INDICATION";
                                                                break;
                                                            case 3075:
                                                                str = "RIL_UNSOL_MOBILE_WIFI_ROVEOUT";
                                                                break;
                                                            case 3076:
                                                                str = "RIL_UNSOL_MOBILE_WIFI_HANDOVER";
                                                                break;
                                                            case 3077:
                                                                str = "RIL_UNSOL_ACTIVE_WIFI_PDN_COUNT";
                                                                break;
                                                            case 3078:
                                                                str = "RIL_UNSOL_WIFI_RSSI_MONITORING_CONFIG";
                                                                break;
                                                            case 3079:
                                                                str = "RIL_UNSOL_WIFI_PDN_ERROR";
                                                                break;
                                                            case 3080:
                                                                str = "RIL_UNSOL_REQUEST_GEO_LOCATION";
                                                                break;
                                                            case 3081:
                                                                str = "RIL_UNSOL_WFC_PDN_STATE";
                                                                break;
                                                            case 3082:
                                                                return "RIL_UNSOL_DEDICATE_BEARER_ACTIVATED";
                                                            case 3083:
                                                                return "RIL_UNSOL_DEDICATE_BEARER_MODIFIED";
                                                            case 3084:
                                                                return "RIL_UNSOL_DEDICATE_BEARER_DEACTIVATED";
                                                            default:
                                                                switch (iIntValue) {
                                                                    case 3095:
                                                                        str = "RIL_UNSOL_ECC_NUM";
                                                                        break;
                                                                    case 3096:
                                                                        str = "RIL_UNSOL_MCCMNC_CHANGED";
                                                                        break;
                                                                    case 3097:
                                                                        str = "UNSOL_SIGNAL_STRENGTH_WITH_WCDMA_ECIO";
                                                                        break;
                                                                    default:
                                                                        switch (iIntValue) {
                                                                            case 3114:
                                                                                str = "RIL_UNSOL_DSBP_STATE_CHANGED";
                                                                                break;
                                                                            case 3115:
                                                                                str = "RIL_UNSOL_SIM_SLOT_LOCK_POLICY_NOTIFY";
                                                                                break;
                                                                            default:
                                                                                switch (iIntValue) {
                                                                                    case 1028:
                                                                                        str = "UNSOL_OEM_HOOK_RAW";
                                                                                        break;
                                                                                    case 3003:
                                                                                        str = "RIL_UNSOL_GMSS_RAT_CHANGED";
                                                                                        break;
                                                                                    case 3062:
                                                                                        str = "RIL_UNSOL_LTE_ACCESS_STRATUM_STATE_CHANGE";
                                                                                        break;
                                                                                    case 3072:
                                                                                        str = "RIL_UNSOL_ECONF_SRVCC_INDICATION";
                                                                                        break;
                                                                                    case 3086:
                                                                                        str = "RIL_UNSOL_NATT_KEEP_ALIVE_CHANGED";
                                                                                        break;
                                                                                    case 3088:
                                                                                        str = "RIL_UNSOL_WIFI_PDN_OOS";
                                                                                        break;
                                                                                    case 3109:
                                                                                        str = "RIL_UNSOL_NETWORK_REJECT_CAUSE";
                                                                                        break;
                                                                                    default:
                                                                                        str = "<unknown response>";
                                                                                        break;
                                                                                }
                                                                                break;
                                                                        }
                                                                        break;
                                                                }
                                                                break;
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                        break;
                                }
                                break;
                        }
                        break;
                }
                break;
        }
        return "MTK: " + str;
    }

    public void registerForCsNetworkStateChanged(Handler handler, int i, Object obj) {
        this.mCsNetworkStateRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForCsNetworkStateChanged(Handler handler) {
        this.mCsNetworkStateRegistrants.remove(handler);
    }

    public void registerForSignalStrengthWithWcdmaEcioChanged(Handler handler, int i, Object obj) {
        this.mSignalStrengthWithWcdmaEcioRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForignalStrengthWithWcdmaEcioChanged(Handler handler) {
        this.mSignalStrengthWithWcdmaEcioRegistrants.remove(handler);
    }

    protected static String retToString(int i, Object obj) {
        return RIL.retToString(i, obj);
    }

    public void setTrm(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2028, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setTrm(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setTrm", e);
            }
        }
    }

    public void getATR(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2001, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getATR(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getATR", e);
            }
        }
    }

    public void getIccid(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2142, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getIccid(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getIccid", e);
            }
        }
    }

    public void setSimPower(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(ExternalSimConstants.MSG_ID_CAPABILITY_SWITCH_DONE, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setSimPower(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setSimPower", e);
            }
        }
    }

    public void registerForVirtualSimOn(Handler handler, int i, Object obj) {
        this.mVirtualSimOn.add(new Registrant(handler, i, obj));
    }

    public void unregisterForVirtualSimOn(Handler handler) {
        this.mVirtualSimOn.remove(handler);
    }

    public void registerForVirtualSimOff(Handler handler, int i, Object obj) {
        this.mVirtualSimOff.add(new Registrant(handler, i, obj));
    }

    public void unregisterForVirtualSimOff(Handler handler) {
        this.mVirtualSimOff.remove(handler);
    }

    public void registerForIMEILock(Handler handler, int i, Object obj) {
        this.mImeiLockRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unregisterForIMEILock(Handler handler) {
        this.mImeiLockRegistrant.remove(handler);
    }

    public void registerForImsiRefreshDone(Handler handler, int i, Object obj) {
        this.mImsiRefreshDoneRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unregisterForImsiRefreshDone(Handler handler) {
        this.mImsiRefreshDoneRegistrant.remove(handler);
    }

    private SimAuthStructure convertToHalSimAuthStructure(int i, int i2, int i3, String str, String str2) {
        SimAuthStructure simAuthStructure = new SimAuthStructure();
        simAuthStructure.sessionId = i;
        simAuthStructure.mode = i2;
        if (str != null && str.length() > 0) {
            String hexString = Integer.toHexString(str.length() / 2);
            StringBuilder sb = new StringBuilder();
            sb.append(hexString.length() % 2 == 1 ? "0" : "");
            sb.append(hexString);
            String string = sb.toString();
            if (i != 0) {
                str = string + str;
            }
            simAuthStructure.param1 = convertNullToEmptyString(str);
        } else {
            simAuthStructure.param1 = convertNullToEmptyString(str);
        }
        if (str2 != null && str2.length() > 0) {
            String hexString2 = Integer.toHexString(str2.length() / 2);
            StringBuilder sb2 = new StringBuilder();
            sb2.append(hexString2.length() % 2 == 1 ? "0" : "");
            sb2.append(hexString2);
            String string2 = sb2.toString();
            if (i != 0) {
                str2 = string2 + str2;
            }
            simAuthStructure.param2 = convertNullToEmptyString(str2);
        } else {
            simAuthStructure.param2 = convertNullToEmptyString(str2);
        }
        if (i2 == 1) {
            simAuthStructure.tag = i3;
        }
        return simAuthStructure;
    }

    public void doGeneralSimAuthentication(int i, int i2, int i3, String str, String str2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2064, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.doGeneralSimAuthentication(rILRequestObtainRequest.mSerial, convertToHalSimAuthStructure(i, i2, i3, str, str2));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "doGeneralSimAuthentication", e);
            }
        }
    }

    public String lookupOperatorName(int i, String str, boolean z, int i2) {
        String telephonyProperty;
        String operatorNumeric;
        String eonsIfExist;
        MtkSIMRecords iccRecords = UiccController.getInstance().getIccRecords(this.mInstanceId.intValue(), 1);
        Rlog.d(RILJ_LOG_TAG, "subId=" + i + " numeric=" + str + " desireLongName=" + z + " nLac=" + i2);
        if (this.mPhoneType == 1) {
            if (i2 != 65534 && i2 != -1) {
                if (iccRecords != null) {
                    try {
                        eonsIfExist = iccRecords.getEonsIfExist(str, i2, z);
                    } catch (RuntimeException e) {
                        Rlog.e(RILJ_LOG_TAG, "Exception while getEonsIfExist. " + e);
                        eonsIfExist = null;
                    }
                } else {
                    eonsIfExist = null;
                }
                if (eonsIfExist != null && !eonsIfExist.equals("")) {
                    Rlog.d(RILJ_LOG_TAG, "plmn name update to Eons: " + eonsIfExist);
                    return eonsIfExist;
                }
            } else {
                Rlog.d(RILJ_LOG_TAG, "invalid lac ignored");
            }
            if (iccRecords != null) {
                operatorNumeric = iccRecords.getOperatorNumeric();
            } else {
                operatorNumeric = null;
            }
            if (operatorNumeric != null && operatorNumeric.equals(str)) {
                String sIMCPHSOns = iccRecords != null ? iccRecords.getSIMCPHSOns() : null;
                if (!TextUtils.isEmpty(sIMCPHSOns)) {
                    Rlog.d(RILJ_LOG_TAG, "plmn name update to CPHS Ons: " + sIMCPHSOns);
                    return sIMCPHSOns;
                }
            }
        }
        int phoneId = SubscriptionManager.getPhoneId(i);
        String telephonyProperty2 = TelephonyManager.getTelephonyProperty(phoneId, "persist.vendor.radio.nitz_oper_code", "");
        if (str != null && str.equals(telephonyProperty2)) {
            if (z) {
                telephonyProperty = TelephonyManager.getTelephonyProperty(phoneId, "persist.vendor.radio.nitz_oper_lname", "");
            } else {
                telephonyProperty = TelephonyManager.getTelephonyProperty(phoneId, "persist.vendor.radio.nitz_oper_sname", "");
            }
            if (telephonyProperty != null && telephonyProperty.startsWith("uCs2")) {
                Rlog.d(RILJ_LOG_TAG, "lookupOperatorName() handling UCS2 format name");
                try {
                    telephonyProperty = new String(IccUtils.hexStringToBytes(telephonyProperty.substring(4)), "UTF-16");
                } catch (UnsupportedEncodingException e2) {
                    Rlog.d(RILJ_LOG_TAG, "lookupOperatorName() UnsupportedEncodingException");
                }
            }
            Rlog.d(RILJ_LOG_TAG, "plmn name update to Nitz: " + telephonyProperty);
            if (!TextUtils.isEmpty(telephonyProperty)) {
                return telephonyProperty;
            }
        }
        if (str == null) {
            return null;
        }
        String strLookupOperatorName = MtkServiceStateTracker.lookupOperatorName(this.mMtkContext, i, str, z);
        Rlog.d(RILJ_LOG_TAG, "plmn name update to MVNO: " + strLookupOperatorName);
        return strLookupOperatorName;
    }

    public void setNetworkSelectionModeManualWithAct(String str, String str2, int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(MtkGsmCdmaPhone.EVENT_SET_CALL_BARRING_COMPLETE, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " operatorNumeric = " + str);
            try {
                radioProxy.setNetworkSelectionModeManualWithAct(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), convertNullToEmptyString(str2), Integer.toString(i));
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setNetworkSelectionModeManual", e);
            }
        }
    }

    public boolean hidePLMN(String str) {
        Iterator<String> it = this.hide_plmns.iterator();
        while (it.hasNext()) {
            if (it.next().equals(str)) {
                return true;
            }
        }
        return false;
    }

    public void getAvailableNetworksWithAct(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(MtkGsmCdmaPhone.EVENT_GET_CALL_BARRING_COMPLETE, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getAvailableNetworksWithAct(rILRequestObtainRequest.mSerial);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getAvailableNetworks", e);
            }
        }
    }

    public void cancelAvailableNetworks(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2007, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.cancelAvailableNetworks(rILRequestObtainRequest.mSerial);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getAvailableNetworks", e);
            }
        }
    }

    public void getFemtoCellList(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2055, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getFemtocellList(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getFemtoCellList", e);
            }
        }
    }

    public void abortFemtoCellList(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2056, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.abortFemtocellList(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "abortFemtoCellList", e);
            }
        }
    }

    public void selectFemtoCell(FemtoCellInfo femtoCellInfo, Message message) {
        int i;
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2057, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            int csgRat = femtoCellInfo.getCsgRat();
            if (csgRat == 14) {
                i = 7;
            } else if (csgRat == 3) {
                i = 2;
            } else {
                i = 0;
            }
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " csgId=" + femtoCellInfo.getCsgId() + " plmn=" + femtoCellInfo.getOperatorNumeric() + " rat=" + femtoCellInfo.getCsgRat() + " act=" + i);
            try {
                radioProxy.selectFemtocell(rILRequestObtainRequest.mSerial, convertNullToEmptyString(femtoCellInfo.getOperatorNumeric()), convertNullToEmptyString(Integer.toString(i)), convertNullToEmptyString(Integer.toString(femtoCellInfo.getCsgId())));
            } catch (Exception e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "selectFemtoCell", e);
            }
        }
    }

    public void queryFemtoCellSystemSelectionMode(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2058, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.queryFemtoCellSystemSelectionMode(rILRequestObtainRequest.mSerial);
            } catch (Exception e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryFemtoCellSystemSelectionMode", e);
            }
        }
    }

    public void setFemtoCellSystemSelectionMode(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2059, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " mode=" + i);
            try {
                radioProxy.setFemtoCellSystemSelectionMode(rILRequestObtainRequest.mSerial, i);
            } catch (Exception e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setFemtoCellSystemSelectionMode", e);
            }
        }
    }

    public void getSignalStrengthWithWcdmaEcio(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2153, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getSignalStrengthWithWcdmaEcio(rILRequestObtainRequest.mSerial);
            } catch (Exception e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getSignalStrength", e);
            }
        }
    }

    public void setModemPower(boolean z, Message message) {
        RILRequest rILRequestObtainRequest;
        mtkRiljLog("Set Modem power as: " + z);
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            if (z) {
                rILRequestObtainRequest = obtainRequest(MtkGsmCdmaPhone.EVENT_USSI_CSFB, message, this.mRILDefaultWorkSource);
            } else {
                rILRequestObtainRequest = obtainRequest(MtkGsmCdmaPhone.EVENT_GET_CLIR_COMPLETE, message, this.mRILDefaultWorkSource);
            }
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " " + z);
            try {
                radioProxy.setModemPower(rILRequestObtainRequest.mSerial, z);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setModemPower", e);
            }
        }
    }

    public void setInvalidSimInfo(Handler handler, int i, Object obj) {
        this.mInvalidSimInfoRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unSetInvalidSimInfo(Handler handler) {
        this.mInvalidSimInfoRegistrant.remove(handler);
    }

    public void registerForNetworkEvent(Handler handler, int i, Object obj) {
        this.mNetworkEventRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForNetworkEvent(Handler handler) {
        this.mNetworkEventRegistrants.remove(handler);
    }

    public void registerForNetworkReject(Handler handler, int i, Object obj) {
        this.mNetworkRejectRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForNetworkReject(Handler handler) {
        this.mNetworkRejectRegistrants.remove(handler);
    }

    public void registerForModulation(Handler handler, int i, Object obj) {
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.registerForModulation(handler, i, obj);
        }
    }

    public void unregisterForModulation(Handler handler) {
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.unregisterForModulation(handler);
        }
    }

    public void registerForFemtoCellInfo(Handler handler, int i, Object obj) {
        this.mFemtoCellInfoRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForFemtoCellInfo(Handler handler) {
        this.mFemtoCellInfoRegistrants.remove(handler);
    }

    public void registerForSmsReady(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mSmsReadyRegistrants.add(registrant);
        if (this.mIsSmsReady) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForSmsReady(Handler handler) {
        this.mSmsReadyRegistrants.remove(handler);
    }

    public void setOnMeSmsFull(Handler handler, int i, Object obj) {
        this.mMeSmsFullRegistrant = new Registrant(handler, i, obj);
    }

    public void unSetOnMeSmsFull(Handler handler) {
        this.mMeSmsFullRegistrant.clear();
    }

    public void setOnEtwsNotification(Handler handler, int i, Object obj) {
        this.mEtwsNotificationRegistrant = new Registrant(handler, i, obj);
    }

    public void unSetOnEtwsNotification(Handler handler) {
        this.mEtwsNotificationRegistrant.clear();
    }

    public void getSmsParameters(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2012, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getSmsParameters(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getSmsParameters", e);
            }
        }
    }

    public void setSmsParameters(MtkSmsParameters mtkSmsParameters, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2013, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            SmsParams smsParams = new SmsParams();
            smsParams.dcs = mtkSmsParameters.dcs;
            smsParams.format = mtkSmsParameters.format;
            smsParams.pid = mtkSmsParameters.pid;
            smsParams.vp = mtkSmsParameters.vp;
            try {
                radioProxy.setSmsParameters(rILRequestObtainRequest.mSerial, smsParams);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setSmsParameters", e);
            }
        }
    }

    public void setEtws(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2014, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setEtws(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setEtws", e);
            }
        }
    }

    public void removeCellBroadcastMsg(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2015, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " " + i + ", " + i2);
            try {
                radioProxy.removeCbMsg(rILRequestObtainRequest.mSerial, i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "removeCellBroadcastMsg", e);
            }
        }
    }

    public void getSmsSimMemoryStatus(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2011, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getSmsMemStatus(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getSmsSimMemoryStatus", e);
            }
        }
    }

    public void setGsmBroadcastLangs(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2009, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ", lang:" + str);
            try {
                radioProxy.setGsmBroadcastLangs(rILRequestObtainRequest.mSerial, str);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setGsmBroadcastLangs", e);
            }
        }
    }

    public void getGsmBroadcastLangs(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2010, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getGsmBroadcastLangs(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getGsmBroadcastLangs", e);
            }
        }
    }

    public void getGsmBroadcastActivation(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2115, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getGsmBroadcastActivation(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getGsmBroadcastActivation", e);
            }
        }
    }

    public void setCDMACardInitalEsnMeid(Handler handler, int i, Object obj) {
        this.mCDMACardEsnMeidRegistrant = new Registrant(handler, i, obj);
        if (this.mEspOrMeid != null) {
            this.mCDMACardEsnMeidRegistrant.notifyRegistrant(new AsyncResult((Object) null, this.mEspOrMeid, (Throwable) null));
        }
    }

    public void writeSmsToRuim(int i, String str, Message message) {
        int iTranslateStatus = translateStatus(i);
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(96, message, this.mRILDefaultWorkSource);
            CdmaSmsWriteArgs cdmaSmsWriteArgs = new CdmaSmsWriteArgs();
            cdmaSmsWriteArgs.status = iTranslateStatus;
            constructCdmaSendSmsRilRequest(cdmaSmsWriteArgs.message, IccUtils.hexStringToBytes(str));
            try {
                radioProxy.writeSmsToRuim(rILRequestObtainRequest.mSerial, cdmaSmsWriteArgs);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "writeSmsToRuim", e);
            }
        }
    }

    public void setSmsFwkReady(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2134, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setSmsFwkReady(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setSmsFwkReady", e);
            }
        }
    }

    public void registerForPsNetworkStateChanged(Handler handler, int i, Object obj) {
        this.mPsNetworkStateRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForPsNetworkStateChanged(Handler handler) {
        this.mPsNetworkStateRegistrants.remove(handler);
    }

    public void registerForNetworkInfo(Handler handler, int i, Object obj) {
        this.mNetworkInfoRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unregisterForNetworkInfo(Handler handler) {
        this.mNetworkInfoRegistrant.remove(handler);
    }

    public void changeBarringPassword(String str, String str2, String str3, String str4, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(44, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + "facility = " + str);
            try {
                radioProxy.setBarringPasswordCheckedByNW(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), convertNullToEmptyString(str2), convertNullToEmptyString(str3), convertNullToEmptyString(str4));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "changeBarringPasswordCheckedByNW", e);
            }
        }
    }

    public void setCLIP(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2103, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " clipEnable = " + i);
            try {
                radioProxy.setClip(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCLIP", e);
            }
        }
    }

    public void getCOLP(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2104, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getColp(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getCOLP", e);
            }
        }
    }

    public void getCOLR(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2105, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getColr(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getCOLR", e);
            }
        }
    }

    public void sendCNAP(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2106, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + "CNAP string = " + str);
            try {
                radioProxy.sendCnap(rILRequestObtainRequest.mSerial, str);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendCNAP", e);
            }
        }
    }

    public void setCOLR(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2124, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " colrEnable = " + i);
            try {
                radioProxy.setColr(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCOLR", e);
            }
        }
    }

    public void setCOLP(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2123, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " colpEnable = " + i);
            try {
                radioProxy.setColp(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCOLP", e);
            }
        }
    }

    public void queryCallForwardInTimeSlotStatus(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2125, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " cfreason = " + i + " serviceClass = " + i2);
            CallForwardInfoEx callForwardInfoEx = new CallForwardInfoEx();
            callForwardInfoEx.reason = i;
            callForwardInfoEx.serviceClass = i2;
            callForwardInfoEx.toa = PhoneNumberUtils.toaFromString("");
            callForwardInfoEx.number = convertNullToEmptyString("");
            callForwardInfoEx.timeSeconds = 0;
            callForwardInfoEx.timeSlotBegin = convertNullToEmptyString("");
            callForwardInfoEx.timeSlotEnd = convertNullToEmptyString("");
            try {
                radioProxy.queryCallForwardInTimeSlotStatus(rILRequestObtainRequest.mSerial, callForwardInfoEx);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryCallForwardInTimeSlotStatus", e);
            }
        }
    }

    public void setCallForwardInTimeSlot(int i, int i2, int i3, String str, int i4, long[] jArr, Message message) {
        String str2 = "";
        String str3 = "";
        if (jArr != null && jArr.length == 2) {
            for (int i5 = 0; i5 < jArr.length; i5++) {
                Date date = new Date(jArr[i5]);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                if (i5 == 0) {
                    str2 = simpleDateFormat.format(date);
                } else {
                    str3 = simpleDateFormat.format(date);
                }
            }
        }
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2126, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " action = " + i + " cfReason = " + i2 + " serviceClass = " + i3 + " timeSeconds = " + i4 + "timeSlot = [" + str2 + ":" + str3 + "]");
            CallForwardInfoEx callForwardInfoEx = new CallForwardInfoEx();
            callForwardInfoEx.status = i;
            callForwardInfoEx.reason = i2;
            callForwardInfoEx.serviceClass = i3;
            callForwardInfoEx.toa = PhoneNumberUtils.toaFromString(str);
            callForwardInfoEx.number = convertNullToEmptyString(str);
            callForwardInfoEx.timeSeconds = i4;
            callForwardInfoEx.timeSlotBegin = convertNullToEmptyString(str2);
            callForwardInfoEx.timeSlotEnd = convertNullToEmptyString(str3);
            try {
                radioProxy.setCallForwardInTimeSlot(rILRequestObtainRequest.mSerial, callForwardInfoEx);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCallForwardInTimeSlot", e);
            }
        }
    }

    public void runGbaAuthentication(String str, String str2, boolean z, int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2127, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + ">  " + requestToString(rILRequestObtainRequest.mRequest) + " nafFqdn = " + str + " nafSecureProtocolId = " + str2 + " forceRun = " + z + " netId = " + i);
            try {
                radioProxy.runGbaAuthentication(rILRequestObtainRequest.mSerial, str, str2, z, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "runGbaAuthentication", e);
            }
        }
    }

    public void registerForDataAllowed(Handler handler, int i, Object obj) {
        this.mDataAllowedRegistrants.add(new Registrant(handler, i, obj));
    }

    public void sendEmbmsAtCommand(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2060, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " data: " + str);
            try {
                radioProxy.sendEmbmsAtCommand(rILRequestObtainRequest.mSerial, str);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendEmbmsAtCommand", e);
            }
        }
    }

    public void setEmbmsSessionStatusNotification(Handler handler, int i, Object obj) {
        this.mEmbmsSessionStatusNotificationRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unSetEmbmsSessionStatusNotification(Handler handler) {
        this.mEmbmsSessionStatusNotificationRegistrant.remove(handler);
    }

    public void setAtInfoNotification(Handler handler, int i, Object obj) {
        this.mEmbmsAtInfoNotificationRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unSetAtInfoNotification(Handler handler) {
        this.mEmbmsAtInfoNotificationRegistrant.remove(handler);
    }

    public void unregisterForDataAllowed(Handler handler) {
        this.mDataAllowedRegistrants.remove(handler);
    }

    public void registerForCallForwardingInfo(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        Rlog.d(RILJ_LOG_TAG, "call registerForCallForwardingInfo, Handler : " + handler);
        this.mCallForwardingInfoRegistrants.add(registrant);
        if (this.mCfuReturnValue != null) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, this.mCfuReturnValue, (Throwable) null));
        }
    }

    public void unregisterForCallForwardingInfo(Handler handler) {
        this.mCallForwardingInfoRegistrants.remove(handler);
    }

    public void setOnIncomingCallIndication(Handler handler, int i, Object obj) {
        this.mIncomingCallIndicationRegistrant = new Registrant(handler, i, obj);
    }

    public void unsetOnIncomingCallIndication(Handler handler) {
        this.mIncomingCallIndicationRegistrant.clear();
    }

    public void setOnCallRelatedSuppSvc(Handler handler, int i, Object obj) {
        this.mCallRelatedSuppSvcRegistrant = new Registrant(handler, i, obj);
    }

    public void unSetOnCallRelatedSuppSvc(Handler handler) {
        this.mCallRelatedSuppSvcRegistrant.clear();
    }

    public void registerForCipherIndication(Handler handler, int i, Object obj) {
        this.mCipherIndicationRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unregisterForCipherIndication(Handler handler) {
        this.mCipherIndicationRegistrant.remove(handler);
    }

    public void registerForCdmaCallAccepted(Handler handler, int i, Object obj) {
        this.mCdmaCallAcceptedRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unregisterForCdmaCallAccepted(Handler handler) {
        this.mCdmaCallAcceptedRegistrant.remove(handler);
    }

    private void handleChldRelatedRequest(RILRequest rILRequest, Object[] objArr) {
        int i;
        synchronized (this.mDtmfReqQueue) {
            int size = this.mDtmfReqQueue.size();
            if (size > 0) {
                if (this.mDtmfReqQueue.get().rr.mRequest == 49) {
                    Rlog.d(RILJ_LOG_TAG, "DTMF queue isn't 0, first request is START, send stop dtmf and pending switch");
                    if (size > 1) {
                        i = 2;
                    } else {
                        i = 1;
                    }
                    Rlog.d(RILJ_LOG_TAG, "queue size  " + this.mDtmfReqQueue.size());
                    for (int i2 = size - 1; i2 >= i; i2--) {
                        this.mDtmfReqQueue.remove(i2);
                    }
                    if (this.mDtmfReqQueue.size() == 1) {
                        Rlog.d(RILJ_LOG_TAG, "add dummy stop dtmf request");
                        RILRequest rILRequestObtainRequest = obtainRequest(50, null, this.mRILDefaultWorkSource);
                        new Class[1][0] = Integer.TYPE;
                        DtmfQueueHandler.DtmfQueueRR dtmfQueueRRBuildDtmfQueueRR = this.mDtmfReqQueue.buildDtmfQueueRR(rILRequestObtainRequest, new Object[]{Integer.valueOf(rILRequestObtainRequest.mSerial)});
                        this.mDtmfReqQueue.stop();
                        this.mDtmfReqQueue.add(dtmfQueueRRBuildDtmfQueueRR);
                    }
                } else {
                    Rlog.d(RILJ_LOG_TAG, "DTMF queue isn't 0, first request is STOP, penging switch");
                    for (int i3 = size - 1; i3 >= 1; i3--) {
                        this.mDtmfReqQueue.remove(i3);
                    }
                }
                if (this.mDtmfReqQueue.getPendingRequest() != null) {
                    RILRequest rILRequest2 = this.mDtmfReqQueue.getPendingRequest().rr;
                    if (rILRequest2.mResult != null) {
                        AsyncResult.forMessage(rILRequest2.mResult, (Object) null, (Throwable) null);
                        rILRequest2.mResult.sendToTarget();
                    }
                }
                this.mDtmfReqQueue.setPendingRequest(this.mDtmfReqQueue.buildDtmfQueueRR(rILRequest, objArr));
            } else {
                Rlog.d(RILJ_LOG_TAG, "DTMF queue is 0, send switch Immediately");
                this.mDtmfReqQueue.setSendChldRequest();
                sendDtmfQueueRR(this.mDtmfReqQueue.buildDtmfQueueRR(rILRequest, objArr));
            }
        }
    }

    public void sendDtmfQueueRR(DtmfQueueHandler.DtmfQueueRR dtmfQueueRR) {
        RILRequest rILRequest = dtmfQueueRR.rr;
        IRadio radioProxy = getRadioProxy(rILRequest.mResult);
        if (radioProxy == null) {
            mtkRiljLoge("get RadioProxy null. ([" + rILRequest.serialString() + "] request: " + requestToString(rILRequest.mRequest) + ")");
            return;
        }
        mtkRiljLog(rILRequest.serialString() + "> " + requestToString(rILRequest.mRequest) + " (by DtmfQueueRR)");
        try {
            switch (rILRequest.mRequest) {
                case 15:
                    radioProxy.switchWaitingOrHoldingAndActive(rILRequest.mSerial);
                    break;
                case 16:
                    radioProxy.conference(rILRequest.mSerial);
                    break;
                case 49:
                    Object[] objArr = dtmfQueueRR.params;
                    if (objArr.length != 1) {
                        mtkRiljLoge("request " + requestToString(rILRequest.mRequest) + " params error. (" + objArr.toString() + ")");
                    } else {
                        char cCharValue = ((Character) objArr[0]).charValue();
                        radioProxy.startDtmf(rILRequest.mSerial, cCharValue + "");
                    }
                    break;
                case 50:
                    radioProxy.stopDtmf(rILRequest.mSerial);
                    break;
                case 52:
                    Object[] objArr2 = dtmfQueueRR.params;
                    if (objArr2.length != 1) {
                        mtkRiljLoge("request " + requestToString(rILRequest.mRequest) + " params error. (" + Arrays.toString(objArr2) + ")");
                    } else {
                        radioProxy.separateConnection(rILRequest.mSerial, ((Integer) objArr2[0]).intValue());
                    }
                    break;
                case 72:
                    radioProxy.explicitCallTransfer(rILRequest.mSerial);
                    break;
                default:
                    mtkRiljLoge("get RadioProxy null. ([" + rILRequest.serialString() + "] request: " + requestToString(rILRequest.mRequest) + ")");
                    break;
            }
        } catch (RemoteException | RuntimeException e) {
            handleRadioProxyExceptionForRR(rILRequest, "DtmfQueueRR(" + requestToString(rILRequest.mRequest) + ")", e);
        }
    }

    public void handleDtmfQueueNext(int i) {
        DtmfQueueHandler.DtmfQueueRR dtmfQueueRR;
        mtkRiljLog("handleDtmfQueueNext (serial = " + i);
        synchronized (this.mDtmfReqQueue) {
            int i2 = 0;
            while (true) {
                if (i2 < this.mDtmfReqQueue.mDtmfQueue.size()) {
                    dtmfQueueRR = (DtmfQueueHandler.DtmfQueueRR) this.mDtmfReqQueue.mDtmfQueue.get(i2);
                    if (dtmfQueueRR != null && dtmfQueueRR.rr.mSerial == i) {
                        break;
                    } else {
                        i2++;
                    }
                } else {
                    dtmfQueueRR = null;
                    break;
                }
            }
            if (dtmfQueueRR == null) {
                mtkRiljLoge("cannot find serial " + i + " from mDtmfQueue. (size = " + this.mDtmfReqQueue.size() + ")");
            } else {
                this.mDtmfReqQueue.remove(dtmfQueueRR);
                mtkRiljLog("remove first item in dtmf queue done. (size = " + this.mDtmfReqQueue.size() + ")");
            }
            if (this.mDtmfReqQueue.size() > 0) {
                DtmfQueueHandler.DtmfQueueRR dtmfQueueRR2 = this.mDtmfReqQueue.get();
                RILRequest rILRequest = dtmfQueueRR2.rr;
                mtkRiljLog(rILRequest.serialString() + "> " + requestToString(rILRequest.mRequest));
                sendDtmfQueueRR(dtmfQueueRR2);
            } else if (this.mDtmfReqQueue.getPendingRequest() != null) {
                mtkRiljLog("send pending switch request");
                sendDtmfQueueRR(this.mDtmfReqQueue.getPendingRequest());
                this.mDtmfReqQueue.setSendChldRequest();
                this.mDtmfReqQueue.setPendingRequest(null);
            }
        }
    }

    public void switchWaitingOrHoldingAndActive(Message message) {
        if (getRadioProxy(message) != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(15, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            handleChldRelatedRequest(rILRequestObtainRequest, null);
        }
    }

    public void conference(Message message) {
        if (getRadioProxy(message) != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(16, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            handleChldRelatedRequest(rILRequestObtainRequest, null);
        }
    }

    public void separateConnection(int i, Message message) {
        if (getRadioProxy(message) != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(52, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " gsmIndex = " + i);
            handleChldRelatedRequest(rILRequestObtainRequest, new Object[]{Integer.valueOf(i)});
        }
    }

    public void explicitCallTransfer(Message message) {
        if (getRadioProxy(message) != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(72, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            handleChldRelatedRequest(rILRequestObtainRequest, null);
        }
    }

    public void startDtmf(char c, Message message) {
        synchronized (this.mDtmfReqQueue) {
            if (!this.mDtmfReqQueue.hasSendChldRequest()) {
                int size = this.mDtmfReqQueue.size();
                Objects.requireNonNull(this.mDtmfReqQueue);
                if (size < 32) {
                    if (!this.mDtmfReqQueue.isStart()) {
                        if (getRadioProxy(message) != null) {
                            RILRequest rILRequestObtainRequest = obtainRequest(49, message, this.mRILDefaultWorkSource);
                            this.mDtmfReqQueue.start();
                            DtmfQueueHandler.DtmfQueueRR dtmfQueueRRBuildDtmfQueueRR = this.mDtmfReqQueue.buildDtmfQueueRR(rILRequestObtainRequest, new Object[]{Character.valueOf(c)});
                            this.mDtmfReqQueue.add(dtmfQueueRRBuildDtmfQueueRR);
                            if (this.mDtmfReqQueue.size() == 1) {
                                mtkRiljLog("send start dtmf");
                                mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
                                sendDtmfQueueRR(dtmfQueueRRBuildDtmfQueueRR);
                            }
                        }
                    } else {
                        mtkRiljLog("DTMF status conflict, want to start DTMF when status is " + this.mDtmfReqQueue.isStart());
                    }
                }
            }
        }
    }

    public void stopDtmf(Message message) {
        synchronized (this.mDtmfReqQueue) {
            if (!this.mDtmfReqQueue.hasSendChldRequest()) {
                int size = this.mDtmfReqQueue.size();
                Objects.requireNonNull(this.mDtmfReqQueue);
                if (size < 32) {
                    if (this.mDtmfReqQueue.isStart()) {
                        if (getRadioProxy(message) != null) {
                            RILRequest rILRequestObtainRequest = obtainRequest(50, message, this.mRILDefaultWorkSource);
                            this.mDtmfReqQueue.stop();
                            DtmfQueueHandler.DtmfQueueRR dtmfQueueRRBuildDtmfQueueRR = this.mDtmfReqQueue.buildDtmfQueueRR(rILRequestObtainRequest, null);
                            this.mDtmfReqQueue.add(dtmfQueueRRBuildDtmfQueueRR);
                            if (this.mDtmfReqQueue.size() == 1) {
                                mtkRiljLog("send stop dtmf");
                                mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
                                sendDtmfQueueRR(dtmfQueueRRBuildDtmfQueueRR);
                            }
                        }
                    } else {
                        mtkRiljLog("DTMF status conflict, want to start DTMF when status is " + this.mDtmfReqQueue.isStart());
                    }
                }
            }
        }
    }

    public void hangupAll(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2019, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.hangupAll(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "hangupAll", e);
            }
        }
    }

    public void setCallIndication(int i, int i2, int i3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2016, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " " + i + ", " + i2 + ", " + i3);
            try {
                radioProxy.setCallIndication(rILRequestObtainRequest.mSerial, i, i2, i3);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCallIndication", e);
            }
        }
    }

    public void emergencyDial(String str, int i, UUSInfo uUSInfo, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2017, message, this.mRILDefaultWorkSource);
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
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.emergencyDial(rILRequestObtainRequest.mSerial, dial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "emergencyDial", e);
            }
        }
    }

    public void setEccServiceCategory(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2018, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " serviceCategory=" + i);
            try {
                radioProxy.setEccServiceCategory(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setEccServiceCategory", e);
            }
        }
    }

    public void setCurrentStatus(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2035, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " airplaneMode=" + i + " imsReg=" + i2);
            try {
                radioProxy.currentStatus(rILRequestObtainRequest.mSerial, i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setCurrentStatus", e);
            }
        }
    }

    public void setEccPreferredRat(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2110, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " phoneType=" + i);
            try {
                radioProxy.eccPreferredRat(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setEccPreferredRat", e);
            }
        }
    }

    public void setEccList() {
        IRadio radioProxy = getRadioProxy((Message) null);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2030, null, this.mRILDefaultWorkSource);
            String[] strArr = {"", ""};
            int i = 0;
            for (String str : MtkPhoneNumberUtils.getEccList()) {
                if (i >= 2) {
                    break;
                }
                strArr[i] = str;
                i++;
            }
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " ecc1: " + strArr[0] + ", ecc2: " + strArr[1]);
            try {
                radioProxy.setEccList(rILRequestObtainRequest.mSerial, strArr[0], strArr[1]);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setEccList", e);
            }
        }
    }

    public void setOnSpeechCodecInfo(Handler handler, int i, Object obj) {
        this.mSpeechCodecInfoRegistrant = new Registrant(handler, i, obj);
    }

    public void unSetOnSpeechCodecInfo(Handler handler) {
        if (this.mSpeechCodecInfoRegistrant != null && this.mSpeechCodecInfoRegistrant.getHandler() == handler) {
            this.mSpeechCodecInfoRegistrant.clear();
            this.mSpeechCodecInfoRegistrant = null;
        }
    }

    public void setVoicePreferStatus(int i) {
        IRadio radioProxy = getRadioProxy((Message) null);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2147, null, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " status: " + i);
            try {
                radioProxy.setVoicePreferStatus(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setVoicePreferStatus", e);
            }
        }
    }

    public void setEccNum(String str, String str2) {
        IRadio radioProxy = getRadioProxy((Message) null);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2148, null, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " eccListWithCard: " + str + ", eccListNoCard: " + str2);
            try {
                radioProxy.setEccNum(rILRequestObtainRequest.mSerial, str, str2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setEccNum", e);
            }
        }
    }

    public void getEccNum() {
        IRadio radioProxy = getRadioProxy((Message) null);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2149, null, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getEccNum(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getEccNum", e);
            }
        }
    }

    public void registerForPseudoCellInfo(Handler handler, int i, Object obj) {
        this.mPseudoCellInfoRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForPseudoCellInfo(Handler handler) {
        this.mPseudoCellInfoRegistrants.remove(handler);
    }

    public void setApcMode(int i, boolean z, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2021, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " " + i + ", " + z + ", " + i2);
            try {
                radioProxy.setApcMode(rILRequestObtainRequest.mSerial, i, !z ? 0 : 1, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setApcMode", e);
            }
        }
    }

    public void getApcInfo(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2022, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getApcInfo(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getApcInfo", e);
            }
        }
    }

    public void triggerModeSwitchByEcc(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2023, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.triggerModeSwitchByEcc(rILRequestObtainRequest.mSerial, i);
                Message messageObtainMessage = this.mRilHandler.obtainMessage(5);
                messageObtainMessage.obj = null;
                messageObtainMessage.arg1 = rILRequestObtainRequest.mSerial;
                this.mRilHandler.sendMessageDelayed(messageObtainMessage, 2000L);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "triggerModeSwitchByEcc", e);
            }
        }
    }

    public void getSmsRuimMemoryStatus(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2024, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getSmsRuimMemoryStatus(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getSmsRuimMemoryStatus", e);
            }
        }
    }

    public void setFdMode(int i, int i2, int i3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(MtkCharacterSets.GB_2312, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setFdMode(rILRequestObtainRequest.mSerial, i, i2, i3);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setFdMode", e);
            }
        }
    }

    protected ArrayList<HardwareConfig> convertHalHwConfigList(ArrayList<android.hardware.radio.V1_0.HardwareConfig> arrayList, RIL ril) {
        MtkHardwareConfig mtkHardwareConfig;
        ArrayList<HardwareConfig> arrayList2 = new ArrayList<>(arrayList.size());
        for (android.hardware.radio.V1_0.HardwareConfig hardwareConfig : arrayList) {
            int i = hardwareConfig.type;
            switch (i) {
                case 0:
                    mtkHardwareConfig = new MtkHardwareConfig(i);
                    HardwareConfigModem hardwareConfigModem = hardwareConfig.modem.get(0);
                    mtkHardwareConfig.assignModem(hardwareConfig.uuid, hardwareConfig.state, hardwareConfigModem.rilModel, hardwareConfigModem.rat, hardwareConfigModem.maxVoice, hardwareConfigModem.maxData, hardwareConfigModem.maxStandby);
                    break;
                case 1:
                    mtkHardwareConfig = new MtkHardwareConfig(i);
                    mtkHardwareConfig.assignSim(hardwareConfig.uuid, hardwareConfig.state, hardwareConfig.sim.get(0).modemUuid);
                    break;
                default:
                    throw new RuntimeException("RIL_REQUEST_GET_HARDWARE_CONFIG invalid hardward type:" + i);
            }
            arrayList2.add(mtkHardwareConfig);
        }
        return arrayList2;
    }

    public void setOnPlmnChangeNotification(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        synchronized (this.mWPMonitor) {
            this.mPlmnChangeNotificationRegistrant.add(registrant);
            if (this.mEcopsReturnValue != null) {
                registrant.notifyRegistrant(new AsyncResult((Object) null, this.mEcopsReturnValue, (Throwable) null));
                this.mEcopsReturnValue = null;
            }
        }
    }

    public void unSetOnPlmnChangeNotification(Handler handler) {
        synchronized (this.mWPMonitor) {
            this.mPlmnChangeNotificationRegistrant.remove(handler);
        }
    }

    public void setOnRegistrationSuspended(Handler handler, int i, Object obj) {
        synchronized (this.mWPMonitor) {
            this.mRegistrationSuspendedRegistrant = new Registrant(handler, i, obj);
            if (this.mEmsrReturnValue != null) {
                this.mRegistrationSuspendedRegistrant.notifyRegistrant(new AsyncResult((Object) null, this.mEmsrReturnValue, (Throwable) null));
                this.mEmsrReturnValue = null;
            }
        }
    }

    public void unSetOnRegistrationSuspended(Handler handler) {
        synchronized (this.mWPMonitor) {
            this.mRegistrationSuspendedRegistrant.clear();
        }
    }

    public void registerForGmssRatChanged(Handler handler, int i, Object obj) {
        this.mGmssRatChangedRegistrant.add(new Registrant(handler, i, obj));
    }

    public void setResumeRegistration(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(MtkGsmCdmaPhone.EVENT_IMS_UT_DONE, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " sessionId = " + i);
            try {
                radioProxy.setResumeRegistration(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setResumeRegistration", e);
            }
        }
    }

    public void setPropImsHandover(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2159, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " value = " + str);
            try {
                radioProxy.setPropImsHandover(rILRequestObtainRequest.mSerial, str);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setPropImsHandover", e);
            }
        }
    }

    public void storeModemType(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(MtkCharacterSets.MACINTOSH, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " modemType = " + i);
            try {
                radioProxy.storeModemType(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "storeModemType", e);
            }
        }
    }

    public void reloadModemType(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2026, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " modemType = " + i);
            try {
                radioProxy.reloadModemType(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "reloadModemType", e);
            }
        }
    }

    public void handleStkCallSetupRequestFromSimWithResCode(boolean z, int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2029, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            int[] iArr = new int[1];
            if (i == 33 || i == 32) {
                iArr[0] = i;
            } else {
                iArr[0] = z ? 1 : 0;
            }
            try {
                radioProxy.handleStkCallSetupRequestFromSimWithResCode(rILRequestObtainRequest.mSerial, iArr[0]);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "handleStkCallSetupRequestFromSimWithResCode", e);
            }
        }
    }

    public void setPdnReuse(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2160, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setPdnReuse(rILRequestObtainRequest.mSerial, str);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setPdnReuse", e);
            }
        }
    }

    public void setOverrideApn(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2161, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setOverrideApn(rILRequestObtainRequest.mSerial, str);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setOverrideApn", e);
            }
        }
    }

    public void setPdnNameReuse(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2162, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setPdnNameReuse(rILRequestObtainRequest.mSerial, str);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setPdnNameReuse", e);
            }
        }
    }

    public void registerForSimTrayPlugIn(Handler handler, int i, Object obj) {
        this.mSimTrayPlugIn.add(new Registrant(handler, i, obj));
    }

    public void unregisterForSimTrayPlugIn(Handler handler) {
        this.mSimTrayPlugIn.remove(handler);
    }

    public void registerForCommonSlotNoChanged(Handler handler, int i, Object obj) {
        this.mSimCommonSlotNoChanged.add(new Registrant(handler, i, obj));
    }

    public void unregisterForCommonSlotNoChanged(Handler handler) {
        this.mSimCommonSlotNoChanged.remove(handler);
    }

    public void resetRadio(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(58, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.resetRadio(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "resetRadio", e);
            }
        }
    }

    public void restartRILD(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2150, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.restartRILD(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "restartRILD", e);
            }
        }
    }

    public void setOnBipProactiveCmd(Handler handler, int i, Object obj) {
        this.mBipProCmdRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unSetOnBipProactiveCmd(Handler handler) {
        this.mBipProCmdRegistrant.remove(handler);
    }

    public void setOnStkSetupMenuReset(Handler handler, int i, Object obj) {
        this.mStkSetupMenuResetRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unSetOnStkSetupMenuReset(Handler handler) {
        this.mStkSetupMenuResetRegistrant.remove(handler);
    }

    public void queryNetworkLock(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2067, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.queryNetworkLock(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryNetworkLock", e);
            }
        }
    }

    public void setNetworkLock(int i, int i2, String str, String str2, String str3, String str4, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2068, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            if (str == null) {
                str = "";
            }
            String str5 = str;
            if (str2 == null) {
                str2 = "";
            }
            String str6 = str2;
            if (str3 == null) {
                str3 = "";
            }
            String str7 = str3;
            if (str4 == null) {
                str4 = "";
            }
            try {
                radioProxy.setNetworkLock(rILRequestObtainRequest.mSerial, i, i2, str5, str6, str7, str4);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setNetworkLock", e);
            }
        }
    }

    public void supplyDepersonalization(String str, int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2143, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " netpin = " + str + " type = " + i);
            try {
                radioProxy.supplyDepersonalization(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "supplyNetworkDepersonalization", e);
            }
        }
    }

    public void registerForResetAttachApn(Handler handler, int i, Object obj) {
        this.mResetAttachApnRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForResetAttachApn(Handler handler) {
        this.mResetAttachApnRegistrants.remove(handler);
    }

    public void registerForAttachApnChanged(Handler handler, int i, Object obj) {
        this.mAttachApnChangedRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForAttachApnChanged(Handler handler) {
        this.mAttachApnChangedRegistrants.remove(handler);
    }

    public void registerForLteAccessStratumState(Handler handler, int i, Object obj) {
        this.mLteAccessStratumStateRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForLteAccessStratumState(Handler handler) {
        this.mLteAccessStratumStateRegistrants.remove(handler);
    }

    public void setLteAccessStratumReport(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2065, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ": " + (z ? 1 : 0));
            try {
                radioProxy.setLteAccessStratumReport(rILRequestObtainRequest.mSerial, z ? 1 : 0);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setLteAccessStratumReport", e);
            }
        }
    }

    public void setLteUplinkDataTransfer(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2066, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " state = " + i + ", interfaceId = " + i2);
            try {
                radioProxy.setLteUplinkDataTransfer(rILRequestObtainRequest.mSerial, i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setLteUplinkDataTransfer", e);
            }
        }
    }

    public void registerForSimPlugIn(Handler handler, int i, Object obj) {
        this.mSimPlugIn.add(new Registrant(handler, i, obj));
    }

    public void unregisterForSimPlugIn(Handler handler) {
        this.mSimPlugIn.remove(handler);
    }

    public void registerForSimPlugOut(Handler handler, int i, Object obj) {
        this.mSimPlugOut.add(new Registrant(handler, i, obj));
    }

    public void unregisterForSimPlugOut(Handler handler) {
        this.mSimPlugOut.remove(handler);
    }

    public void registerForSimMissing(Handler handler, int i, Object obj) {
        this.mSimMissing.add(new Registrant(handler, i, obj));
    }

    public void unregisterForSimMissing(Handler handler) {
        this.mSimMissing.remove(handler);
    }

    public void registerForSimRecovery(Handler handler, int i, Object obj) {
        this.mSimRecovery.add(new Registrant(handler, i, obj));
    }

    public void unregisterForSimRecovery(Handler handler) {
        this.mSimRecovery.remove(handler);
    }

    public void registerForSmlSlotLockInfoChanged(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mSmlSlotLockInfoChanged.add(registrant);
        if (this.mSmlSlotLockInfo != null) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, this.mSmlSlotLockInfo, (Throwable) null));
        }
    }

    public void unregisterForSmlSlotLockInfoChanged(Handler handler) {
        this.mSmlSlotLockInfoChanged.remove(handler);
    }

    public void supplyDeviceNetworkDepersonalization(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2171, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.supplyDeviceNetworkDepersonalization(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "supplyDeviceNetworkDepersonalization", e);
            }
        }
    }

    public void registerForPhbReady(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        Rlog.d(RILJ_LOG_TAG, "call registerForPhbReady Handler : " + handler);
        this.mPhbReadyRegistrants.add(registrant);
    }

    public void unregisterForPhbReady(Handler handler) {
        this.mPhbReadyRegistrants.remove(handler);
    }

    public void queryPhbStorageInfo(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2036, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ": " + i);
            try {
                radioProxy.queryPhbStorageInfo(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryPhbStorageInfo", e);
            }
        }
    }

    private PhbEntryStructure convertToHalPhbEntryStructure(PhbEntry phbEntry) {
        PhbEntryStructure phbEntryStructure = new PhbEntryStructure();
        phbEntryStructure.type = phbEntry.type;
        phbEntryStructure.index = phbEntry.index;
        phbEntryStructure.number = convertNullToEmptyString(phbEntry.number);
        phbEntryStructure.ton = phbEntry.ton;
        phbEntryStructure.alphaId = convertNullToEmptyString(phbEntry.alphaId);
        return phbEntryStructure;
    }

    public void writePhbEntry(PhbEntry phbEntry, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2037, message, this.mRILDefaultWorkSource);
            PhbEntryStructure phbEntryStructureConvertToHalPhbEntryStructure = convertToHalPhbEntryStructure(phbEntry);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ": " + phbEntry);
            try {
                radioProxy.writePhbEntry(rILRequestObtainRequest.mSerial, phbEntryStructureConvertToHalPhbEntryStructure);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "writePhbEntry", e);
            }
        }
    }

    public void readPhbEntry(int i, int i2, int i3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2038, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ": " + i + " begin: " + i2 + " end: " + i3);
            try {
                radioProxy.readPhbEntry(rILRequestObtainRequest.mSerial, i, i2, i3);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "readPhbEntry", e);
            }
        }
    }

    public void queryUPBCapability(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2039, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.queryUPBCapability(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryUPBCapability", e);
            }
        }
    }

    public void editUPBEntry(int i, int i2, int i3, String str, String str2, String str3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2040, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add(Integer.toString(i));
            arrayList.add(Integer.toString(i2));
            arrayList.add(Integer.toString(i3));
            arrayList.add(str);
            if (i == 0) {
                arrayList.add(str2);
                arrayList.add(str3);
            }
            try {
                radioProxy.editUPBEntry(rILRequestObtainRequest.mSerial, arrayList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "editUPBEntry", e);
            }
        }
    }

    public void editUPBEntry(int i, int i2, int i3, String str, String str2, Message message) {
        editUPBEntry(i, i2, i3, str, str2, null, message);
    }

    public void deleteUPBEntry(int i, int i2, int i3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2041, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ": " + i + " adnIndex: " + i2 + " entryIndex: " + i3);
            try {
                radioProxy.deleteUPBEntry(rILRequestObtainRequest.mSerial, i, i2, i3);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "deleteUPBEntry", e);
            }
        }
    }

    public void readUPBGasList(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2042, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ":  startIndex: " + i + " endIndex: " + i2);
            try {
                radioProxy.readUPBGasList(rILRequestObtainRequest.mSerial, i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "readUPBGasList", e);
            }
        }
    }

    public void readUPBGrpEntry(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2043, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ":  adnIndex: " + i);
            try {
                radioProxy.readUPBGrpEntry(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "readUPBGrpEntry", e);
            }
        }
    }

    public void writeUPBGrpEntry(int i, int[] iArr, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2044, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ":  adnIndex: " + i + " nLen: " + iArr.length);
            ArrayList<Integer> arrayList = new ArrayList<>(iArr.length);
            for (int i2 : iArr) {
                arrayList.add(Integer.valueOf(i2));
            }
            try {
                radioProxy.writeUPBGrpEntry(rILRequestObtainRequest.mSerial, i, arrayList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "writeUPBGrpEntry", e);
            }
        }
    }

    public void getPhoneBookStringsLength(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2045, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> :::" + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getPhoneBookStringsLength(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getPhoneBookStringsLength", e);
            }
        }
    }

    public void getPhoneBookMemStorage(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2046, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> :::" + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getPhoneBookMemStorage(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getPhoneBookMemStorage", e);
            }
        }
    }

    public void setPhoneBookMemStorage(String str, String str2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2047, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> :::" + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setPhoneBookMemStorage(rILRequestObtainRequest.mSerial, convertNullToEmptyString(str), convertNullToEmptyString(str2));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "writeUPBGrpEntry", e);
            }
        }
    }

    public void readPhoneBookEntryExt(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2048, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> :::" + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.readPhoneBookEntryExt(rILRequestObtainRequest.mSerial, i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "readPhoneBookEntryExt", e);
            }
        }
    }

    private static PhbEntryExt convertToHalPhbEntryExt(PBEntry pBEntry) {
        PhbEntryExt phbEntryExt = new PhbEntryExt();
        phbEntryExt.index = pBEntry.getIndex1();
        phbEntryExt.number = pBEntry.getNumber();
        phbEntryExt.type = pBEntry.getType();
        phbEntryExt.text = pBEntry.getText();
        phbEntryExt.hidden = pBEntry.getHidden();
        phbEntryExt.group = pBEntry.getGroup();
        phbEntryExt.adnumber = pBEntry.getAdnumber();
        phbEntryExt.adtype = pBEntry.getAdtype();
        phbEntryExt.secondtext = pBEntry.getSecondtext();
        phbEntryExt.email = pBEntry.getEmail();
        return phbEntryExt;
    }

    public void writePhoneBookEntryExt(PBEntry pBEntry, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2049, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> :::" + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.writePhoneBookEntryExt(rILRequestObtainRequest.mSerial, convertToHalPhbEntryExt(pBEntry));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "writePhoneBookEntryExt", e);
            }
        }
    }

    public void queryUPBAvailable(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2050, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " eftype: " + i + " fileIndex: " + i2);
            try {
                radioProxy.queryUPBAvailable(rILRequestObtainRequest.mSerial, i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "queryUPBAvailable", e);
            }
        }
    }

    public void readUPBEmailEntry(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(MtkCharacterSets.CP864, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " adnIndex: " + i + " fileIndex: " + i2);
            try {
                radioProxy.readUPBEmailEntry(rILRequestObtainRequest.mSerial, i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "readUPBEmailEntry", e);
            }
        }
    }

    public void readUPBSneEntry(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2052, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " adnIndex: " + i + " fileIndex: " + i2);
            try {
                radioProxy.readUPBSneEntry(rILRequestObtainRequest.mSerial, i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "readUPBSneEntry", e);
            }
        }
    }

    public void readUPBAnrEntry(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2053, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " adnIndex: " + i + " fileIndex: " + i2);
            try {
                radioProxy.readUPBAnrEntry(rILRequestObtainRequest.mSerial, i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "readUPBAnrEntry", e);
            }
        }
    }

    public void readUPBAasList(int i, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2054, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " startIndex: " + i + " endIndex: " + i2);
            try {
                radioProxy.readUPBAasList(rILRequestObtainRequest.mSerial, i, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "readUPBAasList", e);
            }
        }
    }

    public void setPhonebookReady(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2157, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " ready = " + i);
            try {
                radioProxy.setPhonebookReady(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setPhonebookReady", e);
            }
        }
    }

    public void setRxTestConfig(int i, Message message) {
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.setRxTestConfig(i, message);
        }
    }

    public void getRxTestResult(Message message) {
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.getRxTestResult(message);
        }
    }

    public void getPOLCapability(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2107, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getPOLCapability(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getPOLCapability", e);
            }
        }
    }

    public void getCurrentPOLList(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2108, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getCurrentPOLList(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getCurrentPOLList", e);
            }
        }
    }

    public void setPOLEntry(int i, String str, int i2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2109, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setPOLEntry(rILRequestObtainRequest.mSerial, i, str, i2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setPOLEntry", e);
            }
        }
    }

    public void registerForPcoDataAfterAttached(Handler handler, int i, Object obj) {
        this.mPcoDataAfterAttachedRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForPcoDataAfterAttached(Handler handler) {
        this.mPcoDataAfterAttachedRegistrants.remove(handler);
    }

    public void syncDataSettingsToMd(int[] iArr, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2062, message, this.mRILDefaultWorkSource);
            ArrayList<Integer> arrayList = new ArrayList<>(iArr.length);
            for (int i : iArr) {
                arrayList.add(Integer.valueOf(i));
            }
            try {
                radioProxy.syncDataSettingsToMd(rILRequestObtainRequest.mSerial, arrayList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "syncDataSettingsToMd", e);
            }
        }
    }

    public void resetMdDataRetryCount(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2063, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ": " + str);
            try {
                radioProxy.resetMdDataRetryCount(rILRequestObtainRequest.mSerial, str);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "resetMdDataRetryCount", e);
            }
        }
    }

    public void registerForMdDataRetryCountReset(Handler handler, int i, Object obj) {
        this.mMdDataRetryCountResetRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForMdDataRetryCountReset(Handler handler) {
        this.mMdDataRetryCountResetRegistrants.remove(handler);
    }

    public void setRemoveRestrictEutranMode(boolean z, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2100, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ": " + (z ? 1 : 0));
            try {
                radioProxy.setRemoveRestrictEutranMode(rILRequestObtainRequest.mSerial, z ? 1 : 0);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setRemoveRestrictEutranMode", e);
            }
        }
    }

    public void registerForRemoveRestrictEutran(Handler handler, int i, Object obj) {
        this.mRemoveRestrictEutranRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForRemoveRestrictEutran(Handler handler) {
        this.mRemoveRestrictEutranRegistrants.remove(handler);
    }

    public void registerForEconfSrvcc(Handler handler, int i, Object obj) {
        this.mEconfSrvccRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForEconfSrvcc(Handler handler) {
        this.mEconfSrvccRegistrants.remove(handler);
    }

    public void setRoamingEnable(int[] iArr, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2111, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            ArrayList<Integer> arrayList = new ArrayList<>(iArr.length);
            for (int i : iArr) {
                arrayList.add(Integer.valueOf(i));
            }
            try {
                radioProxy.setRoamingEnable(rILRequestObtainRequest.mSerial, arrayList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setRoamingEnable", e);
            }
        }
    }

    public void getRoamingEnable(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2112, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getRoamingEnable(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getRoamingEnable", e);
            }
        }
    }

    public void setLteReleaseVersion(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2151, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " mode = " + i);
            try {
                radioProxy.setLteReleaseVersion(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setLteReleaseVersion", e);
            }
        }
    }

    public void getLteReleaseVersion(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2152, message, this.mRILDefaultWorkSource);
            riljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.getLteReleaseVersion(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getLteReleaseVersion", e);
            }
        }
    }

    public void registerForMccMncChanged(Handler handler, int i, Object obj) {
        this.mMccMncRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForMccMncChanged(Handler handler) {
        this.mMccMncRegistrants.remove(handler);
    }

    public void registerForVsimIndication(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        mtkRiljLog("registerForVsimIndication called...");
        this.mVsimIndicationRegistrants.add(registrant);
    }

    public void unregisterForVsimIndication(Handler handler) {
        mtkRiljLog("unregisterForVsimIndication called...");
        this.mVsimIndicationRegistrants.remove(handler);
    }

    public boolean sendVsimNotification(int i, int i2, int i3, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy == null) {
            return false;
        }
        RILRequest rILRequestObtainRequest = obtainRequest(2113, message, this.mRILDefaultWorkSource);
        mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ", eventId: " + i2 + ", simTpye: " + i3);
        try {
            radioProxy.sendVsimNotification(rILRequestObtainRequest.mSerial, i, i2, i3);
            return true;
        } catch (RemoteException e) {
            handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendVsimNotification", e);
            return false;
        }
    }

    public boolean sendVsimOperation(int i, int i2, int i3, int i4, byte[] bArr, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy == null) {
            return false;
        }
        RILRequest rILRequestObtainRequest = obtainRequest(2114, message, this.mRILDefaultWorkSource);
        mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
        ArrayList<Byte> arrayList = new ArrayList<>();
        for (int i5 = 0; i5 < bArr.length; i5++) {
            arrayList.add(Byte.valueOf(bArr[i5]));
        }
        try {
            radioProxy.sendVsimOperation(rILRequestObtainRequest.mSerial, i, i2, i3, i4, arrayList);
            return true;
        } catch (RemoteException e) {
            handleRadioProxyExceptionForRR(rILRequestObtainRequest, "sendVsimOperation", e);
            return false;
        }
    }

    public void registerForDedicatedBearerActivated(Handler handler, int i, Object obj) {
        this.mDedicatedBearerActivedRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForDedicatedBearerActivated(Handler handler) {
        this.mDedicatedBearerActivedRegistrants.remove(handler);
    }

    public void registerForDedicatedBearerModified(Handler handler, int i, Object obj) {
        this.mDedicatedBearerModifiedRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForDedicatedBearerModified(Handler handler) {
        this.mDedicatedBearerModifiedRegistrants.remove(handler);
    }

    public void registerForDedicatedBearerDeactivationed(Handler handler, int i, Object obj) {
        this.mDedicatedBearerDeactivatedRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForDedicatedBearerDeactivationed(Handler handler) {
        this.mDedicatedBearerDeactivatedRegistrants.remove(handler);
    }

    public MtkDedicateDataCallResponse convertDedicatedDataCallResult(DedicateDataCall dedicateDataCall) {
        MtkQosStatus mtkQosStatus;
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        MtkQosStatus mtkQosStatus2;
        MtkTftStatus mtkTftStatus;
        DedicateDataCall dedicateDataCall2 = dedicateDataCall;
        int i8 = dedicateDataCall2.ddcId;
        int i9 = dedicateDataCall2.interfaceId;
        int i10 = dedicateDataCall2.primaryCid;
        int i11 = dedicateDataCall2.cid;
        int i12 = dedicateDataCall2.active;
        int i13 = dedicateDataCall2.signalingFlag;
        int i14 = dedicateDataCall2.bearerId;
        int i15 = dedicateDataCall2.failCause;
        mtkRiljLog("ddcResult.hasQos: " + dedicateDataCall2.hasQos);
        if (dedicateDataCall2.hasQos != 0) {
            mtkQosStatus = new MtkQosStatus(dedicateDataCall2.qos.qci, dedicateDataCall2.qos.dlGbr, dedicateDataCall2.qos.ulGbr, dedicateDataCall2.qos.dlMbr, dedicateDataCall2.qos.ulMbr);
        } else {
            mtkQosStatus = null;
        }
        mtkRiljLog("ddcResult.hasTft: " + dedicateDataCall2.hasTft);
        if (dedicateDataCall2.hasTft != 0) {
            int i16 = dedicateDataCall2.tft.operation;
            ArrayList arrayList = new ArrayList();
            Iterator<PktFilter> it = dedicateDataCall2.tft.pfList.iterator();
            while (it.hasNext()) {
                PktFilter next = it.next();
                ArrayList arrayList2 = arrayList;
                arrayList2.add(new MtkPacketFilterInfo(next.id, next.precedence, next.direction, next.networkPfIdentifier, next.bitmap, next.address, next.mask, next.protocolNextHeader, next.localPortLow, next.localPortHigh, next.remotePortLow, next.remotePortHigh, next.spi, next.tos, next.tosMask, next.flowLabel));
                arrayList = arrayList2;
                it = it;
                mtkQosStatus = mtkQosStatus;
                i15 = i15;
                i14 = i14;
                i13 = i13;
                i12 = i12;
                i11 = i11;
                i10 = i10;
                i9 = i9;
                i16 = i16;
            }
            i = i9;
            i2 = i10;
            i3 = i11;
            i4 = i12;
            i5 = i13;
            i6 = i14;
            i7 = i15;
            mtkQosStatus2 = mtkQosStatus;
            dedicateDataCall2 = dedicateDataCall;
            mtkTftStatus = new MtkTftStatus(i16, arrayList, new MtkTftParameter(dedicateDataCall2.tft.tftParameter.linkedPfList));
        } else {
            i = i9;
            i2 = i10;
            i3 = i11;
            i4 = i12;
            i5 = i13;
            i6 = i14;
            i7 = i15;
            mtkQosStatus2 = mtkQosStatus;
            mtkTftStatus = null;
        }
        return new MtkDedicateDataCallResponse(i, i2, i3, i4, i5, i6, i7, mtkQosStatus2, mtkTftStatus, dedicateDataCall2.pcscf);
    }

    public void setE911State(int i, Message message) {
        if (getRadioProxy(message) != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2129, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + ", state: " + i);
        }
    }

    public void setServiceStateToModem(int i, int i2, int i3, int i4, int i5, int i6, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2130, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " voiceRegState: " + i + " dataRegState: " + i2 + " voiceRoamingType: " + i3 + " dataRoamingType: " + i4 + " rilVoiceRegState: " + i5 + " rilDataRegState:" + i6);
            try {
                radioProxy.setServiceStateToModem(rILRequestObtainRequest.mSerial, i, i2, i3, i4, i5, i6);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setServiceStateToModem", e);
            }
        }
    }

    public void dataConnectionAttach(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2144, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.dataConnectionAttach(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "dataConnectionAttach", e);
            }
        }
    }

    public void dataConnectionDetach(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2145, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.dataConnectionDetach(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "dataConnectionDetach", e);
            }
        }
    }

    public void resetAllConnections(Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2146, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.resetAllConnections(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "resetAllConnections", e);
            }
        }
    }

    public void setOnUnsolOemHookRaw(Handler handler, int i, Object obj) {
        this.mUnsolOemHookRegistrant = new Registrant(handler, i, obj);
    }

    public void unSetOnUnsolOemHookRaw(Handler handler) {
        if (this.mUnsolOemHookRegistrant != null && this.mUnsolOemHookRegistrant.getHandler() == handler) {
            this.mUnsolOemHookRegistrant.clear();
            this.mUnsolOemHookRegistrant = null;
        }
    }

    public void invokeOemRilRequestRaw(byte[] bArr, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(59, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + "[" + IccUtils.bytesToHexString(bArr) + "]");
            try {
                radioProxy.sendRequestRaw(rILRequestObtainRequest.mSerial, primitiveArrayToArrayList(bArr));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "invokeOemRilRequestStrings", e);
            }
        }
    }

    public void invokeOemRilRequestStrings(String[] strArr, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(60, message, this.mRILDefaultWorkSource);
            String str = "";
            for (String str2 : strArr) {
                str = str + str2 + " ";
            }
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " strings = " + str);
            try {
                radioProxy.sendRequestStrings(rILRequestObtainRequest.mSerial, new ArrayList<>(Arrays.asList(strArr)));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "invokeOemRilRequestStrings", e);
            }
        }
    }

    public void setDisable2G(boolean z, Message message) {
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.setDisable2G(z, message);
        }
    }

    public void getDisable2G(Message message) {
        IMtkRilOp rilOp = getRilOp();
        if (rilOp != null) {
            rilOp.getDisable2G(message);
        }
    }

    public void registerForTxPower(Handler handler, int i, Object obj) {
        this.mTxPowerRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unregisterForTxPower(Handler handler) {
        this.mTxPowerRegistrant.remove(handler);
    }

    public void registerForTxPowerStatus(Handler handler, int i, Object obj) {
        this.mTxPowerStatusRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unregisterForTxPowerStatus(Handler handler) {
        this.mTxPowerStatusRegistrant.remove(handler);
    }

    public void setTxPowerStatus(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2158, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setTxPowerStatus(rILRequestObtainRequest.mSerial, i);
                Message messageObtainMessage = this.mRilHandler.obtainMessage(5);
                messageObtainMessage.obj = null;
                messageObtainMessage.arg1 = rILRequestObtainRequest.mSerial;
                this.mRilHandler.sendMessageDelayed(messageObtainMessage, 2000L);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setTxPowerStatus", e);
            }
        }
    }

    public void reportAirplaneMode(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2154, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.reportAirplaneMode(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "reportAirplaneMode", e);
            }
        }
    }

    public void reportSimMode(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2155, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.reportSimMode(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "reportSimMode", e);
            }
        }
    }

    public void setSilentReboot(int i, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2156, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioProxy.setSilentReboot(rILRequestObtainRequest.mSerial, i);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setSilentReboot", e);
            }
        }
    }

    public void setOperatorConfiguration(int i, String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(MtkCallFailCause.CM_MM_RR_CONNECTION_RELEASE, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToStringEx(Integer.valueOf(rILRequestObtainRequest.mRequest)) + " type=" + i + ", data=" + str);
            try {
                radioProxy.setOperatorConfiguration(rILRequestObtainRequest.mSerial, i, str);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setOperatorConfiguration", e);
            }
        }
    }

    public void setSuppServProperty(String str, String str2, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2168, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToStringEx(Integer.valueOf(rILRequestObtainRequest.mRequest)) + " name=" + str + ", value=" + str2);
            try {
                radioProxy.setSuppServProperty(rILRequestObtainRequest.mSerial, str, str2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "setSuppServProperty", e);
            }
        }
    }

    public void getSuppServProperty(String str, Message message) {
        IRadio radioProxy = getRadioProxy(message);
        if (radioProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(2169, message, this.mRILDefaultWorkSource);
            mtkRiljLog(rILRequestObtainRequest.serialString() + "> " + requestToStringEx(Integer.valueOf(rILRequestObtainRequest.mRequest)) + " name=" + str);
            try {
                radioProxy.getSuppServProperty(rILRequestObtainRequest.mSerial, str);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rILRequestObtainRequest, "getSuppServProperty", e);
            }
        }
    }

    public void registerForDsbpStateChanged(Handler handler, int i, Object obj) {
        this.mDsbpStateRegistrant.add(new Registrant(handler, i, obj));
    }

    public void unregisterForDsbpStateChanged(Handler handler) {
        this.mDsbpStateRegistrant.remove(handler);
    }

    public void mtkRiljLog(String str) {
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

    public void mtkRiljLoge(String str) {
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
}
