package com.mediatek.internal.telephony.uicc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.phb.CsimPhbUtil;
import com.mediatek.internal.telephony.phb.MtkAdnRecordCache;
import com.mediatek.internal.telephony.ppl.PplControlData;
import com.mediatek.internal.telephony.uicc.IccServiceInfo;
import java.util.Arrays;

public class MtkRuimRecords extends RuimRecords implements MtkIccConstants {
    public static final int C2K_PHB_NOT_READY = 2;
    public static final int C2K_PHB_READY = 3;
    private static final int CSIM_FDN_SERVICE_MASK_ACTIVE = 1;
    private static final int CSIM_FDN_SERVICE_MASK_EXIST = 2;
    private static final int EVENT_DELAYED_SEND_PHB_CHANGE = 503;
    private static final int EVENT_GET_EST_DONE = 501;
    private static final int EVENT_PHB_READY = 504;
    private static final int EVENT_RADIO_STATE_CHANGED = 502;
    public static final int GSM_PHB_NOT_READY = 0;
    public static final int GSM_PHB_READY = 1;
    static final String LOG_TAG = "MtkRuimRecords";
    private static final int MCC_LEN = 3;
    public static final int PHB_DELAY_SEND_TIME = 500;
    static final String PROPERTY_RIL_C2K_PHB_READY = "vendor.cdma.sim.ril.phbready";
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
    static final String PROPERTY_RIL_GSM_PHB_READY = "vendor.gsm.sim.ril.phbready";
    private static final int RUIM_FDN_SERVICE_MASK_EXIST_ACTIVE = 48;
    private static final int RUIM_FDN_SERVICE_MASK_EXIST_INACTIVE = 16;
    private boolean mDispose;
    private byte[] mEnableService;
    private final BroadcastReceiver mIntentReceiver;
    private boolean mPendingPhbNotify;
    private boolean mPhbReady;
    private boolean mPhbWaitSub;
    private Phone mPhone;
    private int mPhoneId;
    private String mRuimImsi;
    private byte[] mSimService;
    private int mSubId;

    public MtkRuimRecords(MtkUiccCardApplication mtkUiccCardApplication, Context context, CommandsInterface commandsInterface) {
        super(mtkUiccCardApplication, context, commandsInterface);
        this.mRuimImsi = null;
        this.mPhoneId = -1;
        this.mPendingPhbNotify = false;
        this.mSubId = -1;
        this.mPhbReady = false;
        this.mPhbWaitSub = false;
        this.mDispose = false;
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (!action.equals("android.intent.action.RADIO_TECHNOLOGY")) {
                    if (action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED") && MtkRuimRecords.this.mParentApp != null) {
                        MtkRuimRecords.this.log("[onReceive] onReceive ACTION_SUBINFO_RECORD_UPDATED mPhbWaitSub: " + MtkRuimRecords.this.mPhbWaitSub);
                        if (MtkRuimRecords.this.mPhbWaitSub) {
                            MtkRuimRecords.this.mPhbWaitSub = false;
                            MtkRuimRecords.this.broadcastPhbStateChangedIntent(MtkRuimRecords.this.mPhbReady);
                            return;
                        }
                        return;
                    }
                    if (!action.equals("android.intent.action.SIM_STATE_CHANGED") || MtkRuimRecords.this.mParentApp == null) {
                        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
                            MtkRuimRecords.this.log("[onReceive] ACTION_BOOT_COMPLETED mPendingPhbNotify : " + MtkRuimRecords.this.mPendingPhbNotify);
                            if (MtkRuimRecords.this.mPendingPhbNotify) {
                                MtkRuimRecords.this.broadcastPhbStateChangedIntent(MtkRuimRecords.this.isPhbReady());
                                MtkRuimRecords.this.mPendingPhbNotify = false;
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    int intExtra = intent.getIntExtra("slot", 0);
                    String stringExtra = intent.getStringExtra("ss");
                    if (intExtra == MtkRuimRecords.this.mPhoneId) {
                        String telephonyProperty = CsimPhbUtil.isUsingGsmPhbReady(MtkRuimRecords.this.mFh) ? TelephonyManager.getTelephonyProperty(MtkRuimRecords.this.mPhoneId, MtkRuimRecords.PROPERTY_RIL_GSM_PHB_READY, "false") : TelephonyManager.getTelephonyProperty(MtkRuimRecords.this.mPhoneId, MtkRuimRecords.PROPERTY_RIL_C2K_PHB_READY, "false");
                        MtkRuimRecords.this.log("sim state: " + stringExtra + ", mPhbReady: " + MtkRuimRecords.this.mPhbReady + ",strPhbReady: " + telephonyProperty.equals("true"));
                        if ("READY".equals(stringExtra)) {
                            if (MtkRuimRecords.this.mPhbReady || !telephonyProperty.equals("true")) {
                                if (true == MtkRuimRecords.this.mPhbWaitSub && telephonyProperty.equals("true")) {
                                    MtkRuimRecords.this.log("mPhbWaitSub is " + MtkRuimRecords.this.mPhbWaitSub + ", broadcast if need");
                                    MtkRuimRecords.this.mPhbWaitSub = false;
                                    MtkRuimRecords.this.broadcastPhbStateChangedIntent(MtkRuimRecords.this.mPhbReady);
                                    return;
                                }
                                return;
                            }
                            MtkRuimRecords.this.mPhbReady = true;
                            MtkRuimRecords.this.broadcastPhbStateChangedIntent(MtkRuimRecords.this.mPhbReady);
                            return;
                        }
                        return;
                    }
                    return;
                }
                int intExtra2 = intent.getIntExtra("phone", -1);
                MtkRuimRecords.this.log("[onReceive] ACTION_RADIO_TECHNOLOGY_CHANGED phoneId : " + intExtra2);
                if (MtkRuimRecords.this.mParentApp != null && MtkRuimRecords.this.mParentApp.getPhoneId() == intExtra2) {
                    String stringExtra2 = intent.getStringExtra("phoneName");
                    int intExtra3 = intent.getIntExtra("subscription", -1);
                    MtkRuimRecords.this.log("[onReceive] ACTION_RADIO_TECHNOLOGY_CHANGED activePhoneName: " + stringExtra2 + ", subId : " + intExtra3 + ", phoneId: " + intExtra2);
                    if ("CDMA".equals(stringExtra2)) {
                        MtkRuimRecords.this.broadcastPhbStateChangedIntent(false);
                        MtkRuimRecords.this.sendMessageDelayed(MtkRuimRecords.this.obtainMessage(503), 500L);
                        MtkRuimRecords.this.mAdnCache.reset();
                    }
                }
            }
        };
        this.mPhoneId = mtkUiccCardApplication.getPhoneId();
        this.mPhone = PhoneFactory.getPhone(mtkUiccCardApplication.getPhoneId());
        log("MtkRuimRecords X ctor this=" + this);
        this.mAdnCache = new MtkAdnRecordCache(this.mFh, commandsInterface, mtkUiccCardApplication);
        this.mCi.registerForPhbReady(this, 504, null);
        this.mCi.registerForRadioStateChanged(this, 502, (Object) null);
        this.mAdnCache.reset();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        intentFilter.addAction("android.intent.action.RADIO_TECHNOLOGY");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter);
        log("updateIccRecords in IccPhoneBookeInterfaceManager");
        if (this.mPhone != null && this.mPhone.getIccPhoneBookInterfaceManager() != null) {
            this.mPhone.getIccPhoneBookInterfaceManager().updateIccRecords(this);
        }
        if (isPhbReady()) {
            this.mPhbReady = true;
            broadcastPhbStateChangedIntent(this.mPhbReady);
        }
    }

    public void dispose() {
        log("Disposing MtkRuimRecords " + this);
        this.mDispose = true;
        if (!isCdma4GDualModeCard()) {
            log("dispose, reset operator numeric, name and country iso");
            this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), "");
            this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), "");
            this.mTelephonyManager.setSimCountryIsoForPhone(this.mParentApp.getPhoneId(), "");
        }
        if (this.mPhbReady) {
            log("Disposing RuimRecords set PHB unready");
            this.mPhbReady = false;
            broadcastPhbStateChangedIntent(this.mPhbReady);
        }
        this.mParentApp.unregisterForReady(this);
        this.mPhbWaitSub = false;
        this.mCi.unregisterForRadioStateChanged(this);
        this.mCi.unregisterForPhbReady(this);
        this.mContext.unregisterReceiver(this.mIntentReceiver);
        this.mPendingPhbNotify = false;
        super.dispose();
    }

    protected void resetRecords() {
        super.resetRecords();
    }

    public String getOperatorNumeric() {
        String imsi = getIMSI();
        if (imsi == null) {
            return null;
        }
        if (this.mMncLength != -1 && this.mMncLength != 0) {
            return imsi.substring(0, 3 + this.mMncLength);
        }
        return imsi.substring(0, 3 + MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi.substring(0, 3))));
    }

    public void handleMessage(Message message) {
        boolean z;
        if (this.mDestroyed.get()) {
            loge("Received message " + message + "[" + message.what + "] while being destroyed. Ignoring.");
            return;
        }
        try {
            int i = message.what;
            if (i != 17) {
                if (i == 501) {
                    log("Event EVENT_GET_EST_DONE Received");
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception != null) {
                        logi("EVENT_GET_EST_DONE failed");
                    } else {
                        this.mEnableService = (byte[]) asyncResult.result;
                        log("mEnableService[0]: " + ((int) this.mEnableService[0]) + ", mEnableService.length: " + this.mEnableService.length);
                        updateIccFdnStatus();
                    }
                } else {
                    switch (i) {
                        case 503:
                            this.mPhbReady = isPhbReady();
                            log("[EVENT_DELAYED_SEND_PHB_CHANGE] isReady : " + this.mPhbReady);
                            broadcastPhbStateChangedIntent(this.mPhbReady);
                            break;
                        case 504:
                            AsyncResult asyncResult2 = (AsyncResult) message.obj;
                            log("[DBG]EVENT_PHB_READY ar:" + asyncResult2);
                            if (asyncResult2 != null && asyncResult2.exception == null && asyncResult2.result != null) {
                                int[] iArr = (int[]) asyncResult2.result;
                                this.mParentApp.getPhoneId();
                                int simStateForSlotIndex = SubscriptionController.getInstance().getSimStateForSlotIndex(this.mPhoneId);
                                if (simStateForSlotIndex == 4 || simStateForSlotIndex == 2) {
                                    z = true;
                                } else {
                                    z = false;
                                }
                                updatePhbStatus(iArr[0], z);
                                updateIccFdnStatus();
                            }
                            break;
                        default:
                            super.handleMessage(message);
                            break;
                    }
                }
            } else {
                log("Event EVENT_GET_SST_DONE Received");
                AsyncResult asyncResult3 = (AsyncResult) message.obj;
                if (asyncResult3.exception != null) {
                    logi("EVENT_GET_SST_DONE failed");
                } else {
                    this.mSimService = (byte[]) asyncResult3.result;
                    log("mSimService[0]: " + ((int) this.mSimService[0]) + ", data.length: " + this.mSimService.length);
                    updateIccFdnStatus();
                }
            }
        } catch (RuntimeException e) {
            Rlog.w(LOG_TAG, "Exception parsing RUIM record", e);
        }
    }

    protected void onAllRecordsLoaded() {
        super.onAllRecordsLoaded();
        log("onAllRecordsLoaded, mParentApp.getType() = " + this.mParentApp.getType());
        if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_RUIM) {
            this.mFh.loadEFTransparent(28466, obtainMessage(17));
        } else if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_CSIM) {
            this.mFh.loadEFTransparent(28466, obtainMessage(17));
            this.mFh.loadEFTransparent(MtkIccConstants.EF_EST, obtainMessage(501));
        }
    }

    public int getDisplayRule(ServiceState serviceState) {
        String serviceProviderName = getServiceProviderName();
        String uiccProfileForPhone = UiccController.getInstance().getUiccProfileForPhone(this.mPhoneId);
        StringBuilder sb = new StringBuilder();
        sb.append("getDisplayRule uiccProfile is ");
        sb.append((Object) (uiccProfileForPhone != null ? uiccProfileForPhone : "null"));
        log(sb.toString());
        if (uiccProfileForPhone != null && uiccProfileForPhone.getOperatorBrandOverride() != null) {
            log("getDisplayRule, getOperatorBrandOverride is not null");
            return 2;
        }
        if (!this.mCsimSpnDisplayCondition) {
            log("getDisplayRule, no EF_SPN");
            return 2;
        }
        if (!TextUtils.isEmpty(serviceProviderName) && !serviceProviderName.equals("")) {
            log("getDisplayRule, show spn");
            return 1;
        }
        log("getDisplayRule, show plmn");
        return 2;
    }

    protected void log(String str) {
        Rlog.d(LOG_TAG, "[MtkRuimRecords] " + str + " (phoneId " + this.mPhoneId + ")");
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, "[MtkRuimRecords] " + str + " (phoneId " + this.mPhoneId + ")");
    }

    protected void logi(String str) {
        Rlog.i(LOG_TAG, "[MtkRuimRecords] " + str + " (phoneId " + this.mPhoneId + ")");
    }

    public IccServiceInfo.IccServiceStatus getSIMServiceStatus(IccServiceInfo.IccService iccService) {
        IccServiceInfo.IccServiceStatus iccServiceStatus = IccServiceInfo.IccServiceStatus.UNKNOWN;
        if (this.mParentApp == null) {
            log("getSIMServiceStatus enService: " + iccService + ", mParentApp = null.");
            return iccServiceStatus;
        }
        log("getSIMServiceStatus enService: " + iccService + ", mParentApp.getType(): " + this.mParentApp.getType());
        if (iccService == IccServiceInfo.IccService.FDN && this.mSimService != null && this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_RUIM) {
            log("getSIMServiceStatus mSimService[0]: " + ((int) this.mSimService[0]));
            if ((this.mSimService[0] & 48) == 48) {
                return IccServiceInfo.IccServiceStatus.ACTIVATED;
            }
            if ((this.mSimService[0] & PplControlData.STATUS_WIPE_REQUESTED) == 16) {
                return IccServiceInfo.IccServiceStatus.INACTIVATED;
            }
            return IccServiceInfo.IccServiceStatus.NOT_EXIST_IN_SIM;
        }
        if (iccService == IccServiceInfo.IccService.FDN && this.mSimService != null && this.mEnableService != null && this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_CSIM) {
            log("getSIMServiceStatus mSimService[0]: " + ((int) this.mSimService[0]) + ", mEnableService[0]: " + ((int) this.mEnableService[0]));
            if ((this.mSimService[0] & 2) == 2 && (this.mEnableService[0] & 1) == 1) {
                return IccServiceInfo.IccServiceStatus.ACTIVATED;
            }
            if ((this.mSimService[0] & 2) == 2) {
                return IccServiceInfo.IccServiceStatus.INACTIVATED;
            }
            return IccServiceInfo.IccServiceStatus.NOT_EXIST_IN_USIM;
        }
        return iccServiceStatus;
    }

    public boolean isCdmaOnly() {
        String[] strArrSplit;
        if (this.mPhoneId < 0 || this.mPhoneId >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            log("isCdmaOnly: invalid PhoneId " + this.mPhoneId);
            return false;
        }
        String str = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[this.mPhoneId]);
        if (str != null && str.length() > 0) {
            strArrSplit = str.split(",");
        } else {
            strArrSplit = null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("isCdmaOnly PhoneId ");
        sb.append(this.mPhoneId);
        sb.append(", prop value= ");
        sb.append(str);
        sb.append(", size= ");
        sb.append(strArrSplit != null ? strArrSplit.length : 0);
        log(sb.toString());
        return (strArrSplit == null || Arrays.asList(strArrSplit).contains("USIM") || Arrays.asList(strArrSplit).contains("SIM")) ? false : true;
    }

    public boolean isCdma4GDualModeCard() {
        String[] strArrSplit;
        if (this.mPhoneId < 0 || this.mPhoneId >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            log("isCdma4GDualModeCard: invalid PhoneId " + this.mPhoneId);
            return false;
        }
        String str = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[this.mPhoneId]);
        if (str != null && str.length() > 0) {
            strArrSplit = str.split(",");
        } else {
            strArrSplit = null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("isCdma4GDualModeCard PhoneId ");
        sb.append(this.mPhoneId);
        sb.append(", prop value= ");
        sb.append(str);
        sb.append(", size= ");
        sb.append(strArrSplit != null ? strArrSplit.length : 0);
        log(sb.toString());
        return strArrSplit != null && Arrays.asList(strArrSplit).contains("USIM") && Arrays.asList(strArrSplit).contains("CSIM");
    }

    protected void updateIccFdnStatus() {
        log("updateIccFdnStatus mParentAPP=" + this.mParentApp + "  getSIMServiceStatus(Phone.IccService.FDN)=" + getSIMServiceStatus(IccServiceInfo.IccService.FDN) + "  IccServiceStatus.ACTIVATE=" + IccServiceInfo.IccServiceStatus.ACTIVATED);
        if (this.mParentApp != null && getSIMServiceStatus(IccServiceInfo.IccService.FDN) == IccServiceInfo.IccServiceStatus.ACTIVATED) {
            this.mParentApp.queryFdn();
        }
    }

    public boolean isPhbReady() {
        String telephonyProperty;
        StringBuilder sb = new StringBuilder();
        sb.append("[isPhbReady] Start mPhbReady: ");
        sb.append(this.mPhbReady ? "true" : "false");
        log(sb.toString());
        String str = "";
        if (this.mParentApp == null) {
            return false;
        }
        if (CsimPhbUtil.isUsingGsmPhbReady(this.mFh)) {
            telephonyProperty = TelephonyManager.getTelephonyProperty(this.mPhoneId, PROPERTY_RIL_GSM_PHB_READY, "false");
        } else {
            telephonyProperty = TelephonyManager.getTelephonyProperty(this.mPhoneId, PROPERTY_RIL_C2K_PHB_READY, "false");
        }
        String str2 = SystemProperties.get("gsm.sim.state");
        if (str2 != null && str2.length() > 0) {
            String[] strArrSplit = str2.split(",");
            if (this.mPhoneId >= 0 && this.mPhoneId < strArrSplit.length && strArrSplit[this.mPhoneId] != null) {
                str = strArrSplit[this.mPhoneId];
            }
        }
        boolean z = str.equals("NETWORK_LOCKED") || str.equals("PIN_REQUIRED");
        log("[isPhbReady] End strPhbReady: " + telephonyProperty + ", strAllSimState: " + str2);
        return telephonyProperty.equals("true") && !z;
    }

    private void broadcastPhbStateChangedIntent(boolean z) {
        if (this.mPhone != null && this.mPhone.getPhoneType() != 2 && (!this.mDispose || z)) {
            log("broadcastPhbStateChangedIntent, Not active Phone.");
            return;
        }
        log("broadcastPhbStateChangedIntent, mPhbReady " + this.mPhbReady);
        if (z) {
            int[] subId = SubscriptionManager.getSubId(this.mPhoneId);
            if (subId != null && subId.length > 0) {
                this.mSubId = subId[0];
            }
            if (this.mSubId <= 0) {
                log("broadcastPhbStateChangedIntent, mSubId <= 0");
                this.mPhbWaitSub = true;
                return;
            }
        } else if (this.mSubId <= 0) {
            log("broadcastPhbStateChangedIntent, isReady == false and mSubId <= 0");
            return;
        }
        if (!SystemProperties.get("sys.boot_completed").equals("1")) {
            log("broadcastPhbStateChangedIntent, boot not completed");
            this.mPendingPhbNotify = true;
            return;
        }
        Intent intent = new Intent("mediatek.intent.action.PHB_STATE_CHANGED");
        intent.putExtra("ready", z);
        intent.putExtra("subscription", this.mSubId);
        log("Broadcasting intent ACTION_PHB_STATE_CHANGED " + z + " sub id " + this.mSubId + " phoneId " + this.mParentApp.getPhoneId());
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        Intent intent2 = new Intent("mediatek.intent.action.PHB_STATE_CHANGED");
        intent2.putExtra("ready", z);
        intent2.putExtra("subscription", this.mSubId);
        intent2.setPackage("com.mediatek.simprocessor");
        log("Broadcasting intent ACTION_PHB_STATE_CHANGED to package: simprocessor");
        this.mContext.sendBroadcastAsUser(intent2, UserHandle.ALL);
        if (!z) {
            this.mSubId = -1;
        }
    }

    private void updatePhbStatus(int i, boolean z) {
        boolean z2;
        log("[updatePhbStatus] status: " + i + ", isSimLocked: " + z + ", mPhbReady: " + this.mPhbReady);
        if (CsimPhbUtil.isUsingGsmPhbReady(this.mFh)) {
            if (i != 1) {
                if (i != 0) {
                    log("[updatePhbStatus] not GSM PHB status");
                    return;
                }
                z2 = false;
            }
            z2 = true;
        } else {
            if (i != 3) {
                if (i != 2) {
                    log("[updatePhbStatus] not C2K PHB status");
                    return;
                }
                z2 = false;
            }
            z2 = true;
        }
        if (z2) {
            if (!z) {
                if (!this.mPhbReady) {
                    this.mPhbReady = true;
                    broadcastPhbStateChangedIntent(this.mPhbReady);
                    return;
                } else {
                    broadcastPhbStateChangedIntent(this.mPhbReady);
                    return;
                }
            }
            log("[updatePhbStatus] phb ready but sim is not ready.");
            this.mPhbReady = false;
            broadcastPhbStateChangedIntent(this.mPhbReady);
            return;
        }
        if (this.mPhbReady) {
            this.mAdnCache.reset();
            this.mPhbReady = false;
            broadcastPhbStateChangedIntent(this.mPhbReady);
            return;
        }
        broadcastPhbStateChangedIntent(this.mPhbReady);
    }

    protected void onGetImsiDone(String str) {
        if (this.mImsi != null && !this.mImsi.equals("") && this.mImsi.length() >= 3) {
            SystemProperties.set("vendor.cdma.icc.operator.mcc", this.mImsi.substring(0, 3));
        }
        if (this.mImsi != null && !this.mImsi.equals(this.mRuimImsi)) {
            this.mRuimImsi = this.mImsi;
            this.mImsiReadyRegistrants.notifyRegistrants();
            log("MtkRuimRecords: mImsiReadyRegistrants.notifyRegistrants");
        }
    }

    protected void handleRefresh(IccRefreshResponse iccRefreshResponse) {
        if (iccRefreshResponse == null) {
            log("handleRefresh received without input");
            return;
        }
        if (iccRefreshResponse.aid != null && !TextUtils.isEmpty(iccRefreshResponse.aid) && !iccRefreshResponse.aid.equals(this.mParentApp.getAid()) && iccRefreshResponse.refreshResult != 4) {
            return;
        }
        int i = iccRefreshResponse.refreshResult;
        if (i != 4) {
            switch (i) {
                case 1:
                    log("handleRefresh with SIM_REFRESH_INIT");
                    handleFileUpdate(-1);
                    break;
                case 2:
                    log("handleRefresh with SIM_REFRESH_RESET");
                    break;
                default:
                    log("handleRefresh,callback to super");
                    super.handleRefresh(iccRefreshResponse);
                    break;
            }
            return;
        }
        log("handleRefresh with REFRESH_INIT_FULL_FILE_UPDATED");
        handleFileUpdate(-1);
    }

    public void onReady() {
        this.mLockedRecordsReqReason = 0;
        super.onReady();
    }

    protected void onLocked(int i) {
        this.mRecordsRequested = false;
        this.mLoaded.set(false);
        if (this.mLockedRecordsReqReason != 0) {
            this.mLockedRecordsReqReason = i == 32 ? 1 : 2;
            this.mRecordsToLoad++;
            onRecordLoaded();
            return;
        }
        super.onLocked(i);
    }

    protected void handleFileUpdate(int i) {
        this.mLoaded.set(false);
        super.handleFileUpdate(i);
    }
}
