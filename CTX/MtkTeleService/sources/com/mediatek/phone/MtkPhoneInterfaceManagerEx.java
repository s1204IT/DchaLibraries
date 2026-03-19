package com.mediatek.phone;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.telephony.CellInfo;
import android.telephony.LocationAccessPolicy;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.phone.PhoneGlobals;
import com.android.phone.TimeConsumingPreferenceActivity;
import com.android.phone.settings.SettingsConstants;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.PseudoCellInfo;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.phb.MtkIccPhoneBookInterfaceManager;
import com.mediatek.internal.telephony.selfactivation.ISelfActivation;
import com.mediatek.internal.telephony.uicc.MtkUiccProfile;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.ArrayList;
import java.util.List;

public class MtkPhoneInterfaceManagerEx extends IMtkTelephonyEx.Stub {
    private static MtkPhoneInterfaceManagerEx sInstance;
    private PhoneGlobals mApp;
    private AppOpsManager mAppOps;
    private boolean mIsLastEccIms;
    private Phone mPhone;
    private SharedPreferences mTelephonySharedPreferences;
    private UserManager mUserManager;
    private static final String[] PROPERTY_RIL_TEST_SIM = {"vendor.gsm.sim.ril.testsim", "vendor.gsm.sim.ril.testsim.2", "vendor.gsm.sim.ril.testsim.3", "vendor.gsm.sim.ril.testsim.4"};
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
    private static final String[] PROPERTY_UIM_SUBSCRIBER_ID = {"vendor.ril.uim.subscriberid.1", "vendor.ril.uim.subscriberid.2", "vendor.ril.uim.subscriberid.3", "vendor.ril.uim.subscriberid.4"};
    private static final String[] PROPERTY_RIL_CT3G = {"vendor.gsm.ril.ct3g", "vendor.gsm.ril.ct3g.2", "vendor.gsm.ril.ct3g.3", "vendor.gsm.ril.ct3g.4"};
    private static final String[] PROPERTY_M_LOG_TAG = {"persist.log.tag.DCT", "persist.log.tag.MtkDCT", "persist.log.tag.RIL-DATA", "persist.log.tag.C2K_RIL-DATA", "persist.log.tag.GsmCdmaPhone", "persist.log.tag.SSDecisonMaker", "persist.log.tag.GsmMmiCode", "persist.log.tag.RpSsController", "persist.log.tag.RIL-SS", "persist.log.tag.RILMD2-SS", "persist.log.tag.CapaSwitch", "persist.log.tag.DSSelector", "persist.log.tag.DSSExt", "persist.log.tag.Op01DSSExt", "persist.log.tag.Op02DSSExt", "persist.log.tag.Op09DSSExt", "persist.log.tag.Op18DSSExt", "persist.log.tag.DSSelectorUtil", "persist.log.tag.Op01SimSwitch", "persist.log.tag.Op02SimSwitch", "persist.log.tag.Op18SimSwitch", "persist.log.tag.DcFcMgr", "persist.log.tag.DC-1", "persist.log.tag.DC-2", "persist.log.tag.RetryManager", "persist.log.tag.IccProvider", "persist.log.tag.IccPhoneBookIM", "persist.log.tag.AdnRecordCache", "persist.log.tag.AdnRecordLoader", "persist.log.tag.AdnRecord", "persist.log.tag.RIL-PHB", "persist.log.tag.MtkIccProvider", "persist.log.tag.MtkIccPHBIM", "persist.log.tag.MtkAdnRecord", "persist.log.tag.MtkRecordLoader", "persist.log.tag.RpPhbController", "persist.log.tag.RmcPhbReq", "persist.log.tag.RmcPhbUrc", "persist.log.tag.RtcPhb", "persist.log.tag.RIL-SMS", "persist.log.tag.DupSmsFilterExt", "persist.log.tag.VT", "persist.log.tag.ImsVTProvider", "persist.log.tag.IccCardProxy", "persist.log.tag.IsimFileHandler", "persist.log.tag.IsimRecords", "persist.log.tag.SIMRecords", "persist.log.tag.SpnOverride", "persist.log.tag.UiccCard", "persist.log.tag.UiccController", "persist.log.tag.RIL-SIM", "persist.log.tag.CountryDetector", "persist.log.tag.DataDispatcher", "persist.log.tag.ImsService", "persist.log.tag.IMS_RILA", "persist.log.tag.IMSRILRequest", "persist.log.tag.ImsManager", "persist.log.tag.ImsApp", "persist.log.tag.ImsBaseCommands", "persist.log.tag.MtkImsManager", "persist.log.tag.MtkImsService", "persist.log.tag.RP_IMS", "persist.log.tag.RtcIms", "persist.log.tag.RtcImsConference", "persist.log.tag.RtcImsDialog", "persist.log.tag.RmcImsCtlUrcHdl", "persist.log.tag.RmcImsCtlReqHdl", "persist.log.tag.ImsCall", "persist.log.tag.ImsPhone", "persist.log.tag.ImsPhoneCall", "persist.log.tag.ImsPhoneBase", "persist.log.tag.ImsCallSession", "persist.log.tag.ImsCallProfile", "persist.log.tag.ImsEcbm", "persist.log.tag.ImsEcbmProxy", "persist.log.tag.OperatorUtils", "persist.log.tag.WfoApp", "persist.log.tag.GbaApp", "persist.log.tag.GbaBsfProcedure", "persist.log.tag.GbaBsfResponse", "persist.log.tag.GbaDebugParam", "persist.log.tag.GbaService", "persist.log.tag.SresResponse", "persist.log.tag.ImsUtService", "persist.log.tag.SimservType", "persist.log.tag.SimservsTest", "persist.log.tag.ImsUt", "persist.log.tag.SSDecisonMaker", "persist.log.tag.SuppSrvConfig", "persist.log.tag.ECCCallHelper", "persist.log.tag.GsmConnection", "persist.log.tag.TelephonyConf", "persist.log.tag.TeleConfCtrler", "persist.log.tag.TelephonyConn", "persist.log.tag.TeleConnService", "persist.log.tag.ECCRetryHandler", "persist.log.tag.ECCNumUtils", "persist.log.tag.ECCRuleHandler", "persist.log.tag.SuppMsgMgr", "persist.log.tag.ECCSwitchPhone", "persist.log.tag.GsmCdmaConn", "persist.log.tag.GsmCdmaPhone", "persist.log.tag.Phone", "persist.log.tag.RIL-CC", "persist.log.tag.RpCallControl", "persist.log.tag.RpAudioControl", "persist.log.tag.GsmCallTkrHlpr", "persist.log.tag.MtkPhoneNotifr", "persist.log.tag.MtkGsmCdmaConn", "persist.log.tag.RadioManager", "persist.log.tag.RIL_Mux", "persist.log.tag.RIL-OEM", "persist.log.tag.RIL", "persist.log.tag.RIL_UIM_SOCKET", "persist.log.tag.RILD", "persist.log.tag.RIL-RP", "persist.log.tag.RfxMessage", "persist.log.tag.RfxDebugInfo", "persist.log.tag.RfxTimer", "persist.log.tag.RfxObject", "persist.log.tag.SlotQueueEntry", "persist.log.tag.RfxAction", "persist.log.tag.RFX", "persist.log.tag.RpRadioMessage", "persist.log.tag.RpModemMessage", "persist.log.tag.PhoneFactory", "persist.log.tag.ProxyController", "persist.log.tag.SpnOverride", "persist.log.tag.RfxDefDestUtils", "persist.log.tag.RfxSM", "persist.log.tag.RfxSocketSM", "persist.log.tag.RfxDT", "persist.log.tag.RpCdmaOemCtrl", "persist.log.tag.RpRadioCtrl", "persist.log.tag.RpMDCtrl", "persist.log.tag.RpCdmaRadioCtrl", "persist.log.tag.RpFOUtils", "persist.log.tag.C2K_RIL-SIM", "persist.log.tag.MtkGsmCdmaPhone", "persist.log.tag.MtkRILJ", "persist.log.tag.MtkRadioInd", "persist.log.tag.MtkRadioResp", "persist.log.tag.ExternalSimMgr", "persist.log.tag.VsimAdaptor", "persist.log.tag.MGsmSMSDisp", "persist.log.tag.MSimSmsIStatus", "persist.log.tag.MSmsStorageMtr", "persist.log.tag.MSmsUsageMtr", "persist.log.tag.Mtk_RIL_ImsSms", "persist.log.tag.MtkConSmsFwk", "persist.log.tag.MtkCsimFH", "persist.log.tag.MtkDupSmsFilter", "persist.log.tag.MtkIccSmsIntMgr", "persist.log.tag.MtkIsimFH", "persist.log.tag.MtkRuimFH", "persist.log.tag.MtkSIMFH", "persist.log.tag.MtkSIMRecords", "persist.log.tag.MtkSmsCbHeader", "persist.log.tag.MtkSmsManager", "persist.log.tag.MtkSmsMessage", "persist.log.tag.MtkSpnOverride", "persist.log.tag.MtkUiccCardApp", "persist.log.tag.MtkUiccCtrl", "persist.log.tag.MtkUsimFH", "persist.log.tag.RpRilClientCtrl", "persist.log.tag.RilMalClient", "persist.log.tag.RpSimController", "persist.log.tag.MtkSubCtrl", "persist.log.tag.RP_DAC", "persist.log.tag.NetAgentService", "persist.log.tag.NetLnkEventHdlr", "persist.log.tag.RmcDcCommon", "persist.log.tag.RmcDcDefault", "persist.log.tag.RtcDC", "persist.log.tag.RilClient", "persist.log.tag.RmcCommSimReq", "persist.log.tag.RmcCdmaSimRequest", "persist.log.tag.RmcGsmSimRequest", "persist.log.tag.RmcCommSimUrc", "persist.log.tag.RmcGsmSimUrc", "persist.log.tag.RtcCommSimCtrl", "persist.log.tag.RmcCommSimOpReq", "persist.log.tag.RtcRadioCont", "persist.log.tag.MtkRetryManager", "persist.log.tag.RmcDcPdnManager", "persist.log.tag.RmcDcReqHandler", "persist.log.tag.RmcDcUtility", "persist.log.tag.RfxIdToMsgId", "persist.log.tag.RfxOpUtils", "persist.log.tag.RfxMclMessenger", "persist.log.tag.RfxRilAdapter", "persist.log.tag.RfxFragEnc", "persist.log.tag.RfxStatusMgr", "persist.log.tag.RmcRadioReq", "persist.log.tag.RmcCapa", "persist.log.tag.RtcCapa", "persist.log.tag.RpMalController", "persist.log.tag.WORLDMODE", "persist.log.tag.RtcWp", "persist.log.tag.RmcWp", "persist.log.tag.RmcOpRadioReq", "persist.log.tag.RP_DC", "persist.log.tag.RfxRilUtils", "persist.log.tag.RtcNwCtrl", "persist.log.tag.RmcCdmaSimUrc", "persist.log.tag.MtkPhoneNumberUtils", "persist.log.tag.RmcOemHandler", "persist.log.tag.CarrierExpressServiceImpl", "persist.log.tag.CarrierExpressServiceImplExt", "persist.log.tag.PhoneConfigurationSettings", "persist.log.tag.RfxContFactory", "persist.log.tag.RfxChannelMgr", "persist.log.tag.RfxMclDisThread", "persist.log.tag.RfxCloneMgr", "persist.log.tag.RfxHandlerMgr", "persist.log.tag.RtcModeCont", "persist.log.tag.RIL-SocListen", "persist.log.tag.RIL-Netlink"};
    private static final String[] PROPERTY_M_LOG_TAG_V = {"persist.log.tag.NetworkStats", "persist.log.tag.NetworkPolicy", "persist.log.tag.RTC_DAC", "persist.log.tag.RmcEmbmsReq", "persist.log.tag.RmcEmbmsUrc", "persist.log.tag.RtcEmbmsUtil", "persist.log.tag.RtcEmbmsAt", "persist.log.tag.MtkEmbmsAdaptor", "persist.log.tag.MTKSST", "persist.log.tag.RmcNwHdlr", "persist.log.tag.RmcNwReqHdlr", "persist.log.tag.RmcNwRTReqHdlr", "persist.log.tag.RmcRatSwHdlr", "persist.log.tag.RtcRatSwCtrl", "persist.log.tag.MtkPhoneSwitcher"};
    private static final String[] PROPERTY_M_LOG_TAG_COMMON_RIL = {"persist.log.tag.AT", "persist.log.tag.RILMUXD", "persist.log.tag.RILC-MTK", "persist.log.tag.RILC", "persist.log.tag.RfxMainThread", "persist.log.tag.RfxRoot", "persist.log.tag.RfxRilAdapter", "persist.log.tag.RfxController", "persist.log.tag.RILC-RP", "persist.log.tag.RfxTransUtils", "persist.log.tag.RfxMclDisThread", "persist.log.tag.RfxCloneMgr", "persist.log.tag.RfxHandlerMgr", "persist.log.tag.RfxIdToStr", "persist.log.tag.RfxDisThread", "persist.log.tag.RfxMclStatusMgr", "persist.log.tag.RIL-Fusion", "persist.log.tag.RfxContFactory", "persist.log.tag.RfxChannelMgr", "persist.log.tag.RIL-Parcel", "persist.log.tag.RIL-Socket"};
    private static final String[] ICCRECORD_PROPERTY_ICCID = {"vendor.ril.iccid.sim1", "vendor.ril.iccid.sim2", "vendor.ril.iccid.sim3", "vendor.ril.iccid.sim4"};
    private QueryAdnInfoThread mAdnInfoThread = null;
    private final Object mAdnInfoLock = new Object();
    private boolean mIsEccInProgress = false;
    private SimAuth mSimAuthThread = null;
    private String[] PROPERTY_ICCID_SIM = {"vendor.ril.iccid.sim1", "vendor.ril.iccid.sim2", "vendor.ril.iccid.sim3", "vendor.ril.iccid.sim4"};
    private boolean mIsSmlLockMode = SystemProperties.get("ro.vendor.sim_me_lock_mode", "").equals("3");
    protected MainThreadHandler mMainThreadHandler = new MainThreadHandler();
    private SubscriptionController mSubscriptionController = SubscriptionController.getInstance();

    public static final class MtkIccAPDUArgument {
        public int channel;
        public int cla;
        public int command;
        public String data;
        public int family;
        public int p1;
        public int p2;
        public int p3;
        public String pathId;
        public String pin2;
        public int slotId;

        public MtkIccAPDUArgument(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, String str) {
            this.channel = i3;
            this.cla = i4;
            this.command = i5;
            this.p1 = i6;
            this.p2 = i7;
            this.p3 = i8;
            this.pathId = str;
            this.data = null;
            this.pin2 = null;
            this.slotId = i;
            this.family = i2;
        }

        public MtkIccAPDUArgument(int i, int i2, int i3, int i4, int i5, int i6, String str, String str2, String str3) {
            SubscriptionManager.getSlotIndex(SubscriptionManager.getDefaultSubscriptionId());
            this.channel = i;
            this.cla = i2;
            this.command = i3;
            this.p1 = i4;
            this.p2 = i5;
            this.p3 = i6;
            this.pathId = str2;
            this.data = str;
            this.pin2 = str3;
            this.slotId = this.slotId;
        }
    }

    private static final class MainThreadRequest {
        public Object argument;
        public Object result;
        public Integer subId;

        public MainThreadRequest(Object obj, Integer num) {
            this.subId = -1;
            this.argument = obj;
            if (num != null) {
                this.subId = num;
            }
        }
    }

    private class EcmExitReceiver extends BroadcastReceiver {
        private MainThreadRequest mRequest;
        private int mSubId;

        public EcmExitReceiver(int i, MainThreadRequest mainThreadRequest) {
            this.mSubId = i;
            this.mRequest = mainThreadRequest;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED") && !intent.getBooleanExtra("phoneinECMState", false) && intent.getExtras().getInt("subscription") == this.mSubId) {
                this.mRequest.result = MtkPhoneInterfaceManagerEx.this.new EcmExitResult(this);
                synchronized (this.mRequest) {
                    this.mRequest.notifyAll();
                }
            }
        }
    }

    private class EcmExitResult {
        private EcmExitReceiver mReceiver;

        public EcmExitResult(EcmExitReceiver ecmExitReceiver) {
            this.mReceiver = ecmExitReceiver;
        }

        public EcmExitReceiver getReceiver() {
            return this.mReceiver;
        }
    }

    public class MainThreadHandler extends Handler {
        public MainThreadHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            MtkPhoneInterfaceManagerEx.log("MainThreadHandler.handleMessage : " + message.what);
            int i = message.what;
            switch (i) {
                case 35:
                    MainThreadRequest mainThreadRequest = (MainThreadRequest) message.obj;
                    boolean zBooleanValue = ((Boolean) mainThreadRequest.argument).booleanValue();
                    MtkPhoneInterfaceManagerEx.log("CMD_SET_LTE_ACCESS_STRATUM_STATE: enabled " + zBooleanValue + "subId" + mainThreadRequest.subId);
                    MtkPhoneInterfaceManagerEx.this.mPhone = MtkPhoneInterfaceManagerEx.getPhone(mainThreadRequest.subId.intValue());
                    if (MtkPhoneInterfaceManagerEx.this.mPhone != null) {
                        MtkPhoneInterfaceManagerEx.this.mPhone.mDcTracker.onSetLteAccessStratumReport(Boolean.valueOf(zBooleanValue).booleanValue(), obtainMessage(36, mainThreadRequest));
                        return;
                    } else {
                        MtkPhoneInterfaceManagerEx.loge("setLteAccessStratumReport: No MainPhone");
                        mainThreadRequest.result = new Boolean(false);
                        synchronized (mainThreadRequest) {
                            mainThreadRequest.notifyAll();
                            break;
                        }
                        return;
                    }
                case 36:
                    MtkPhoneInterfaceManagerEx.log("EVENT_SET_LTE_ACCESS_STRATUM_STATE_DONE");
                    handleNullReturnEvent(message, "setLteAccessStratumReport");
                    return;
                case 37:
                    MainThreadRequest mainThreadRequest2 = (MainThreadRequest) message.obj;
                    int iIntValue = ((Integer) mainThreadRequest2.argument).intValue();
                    MtkPhoneInterfaceManagerEx.log("CMD_SET_LTE_UPLINK_DATA_TRANSFER_STATE: state " + iIntValue + "subId " + mainThreadRequest2.subId);
                    MtkPhoneInterfaceManagerEx.this.mPhone = MtkPhoneInterfaceManagerEx.getPhone(mainThreadRequest2.subId.intValue());
                    if (MtkPhoneInterfaceManagerEx.this.mPhone != null) {
                        MtkPhoneInterfaceManagerEx.this.mPhone.mDcTracker.onSetLteUplinkDataTransfer(Integer.valueOf(iIntValue).intValue(), obtainMessage(38, mainThreadRequest2));
                        return;
                    } else {
                        MtkPhoneInterfaceManagerEx.loge("setLteUplinkDataTransfer: No MainPhone");
                        mainThreadRequest2.result = new Boolean(false);
                        synchronized (mainThreadRequest2) {
                            mainThreadRequest2.notifyAll();
                            break;
                        }
                        return;
                    }
                case 38:
                    MtkPhoneInterfaceManagerEx.log("EVENT_SET_LTE_UPLINK_DATA_TRANSFER_STATE_DONE");
                    handleNullReturnEvent(message, "setLteUplinkDataTransfer");
                    return;
                default:
                    switch (i) {
                        case 42:
                        case 43:
                            MtkPhoneInterfaceManagerEx.log("handle RX_TEST");
                            AsyncResult asyncResult = (AsyncResult) message.obj;
                            RxTestObject rxTestObject = (RxTestObject) asyncResult.userObj;
                            synchronized (rxTestObject.lockObj) {
                                if (asyncResult.exception != null) {
                                    MtkPhoneInterfaceManagerEx.log("RX_TEST: error ret null, e=" + asyncResult.exception);
                                    rxTestObject.result = null;
                                } else {
                                    rxTestObject.result = (int[]) asyncResult.result;
                                }
                                rxTestObject.lockObj.notify();
                                MtkPhoneInterfaceManagerEx.log("RX_TEST notify result");
                                break;
                            }
                            return;
                        case 44:
                            MainThreadRequest mainThreadRequest3 = (MainThreadRequest) message.obj;
                            Integer num = (Integer) mainThreadRequest3.argument;
                            if (MtkPhoneInterfaceManagerEx.getPhone(num.intValue()).isInEcm()) {
                                IntentFilter intentFilter = new IntentFilter();
                                intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
                                EcmExitReceiver ecmExitReceiver = MtkPhoneInterfaceManagerEx.this.new EcmExitReceiver(num.intValue(), mainThreadRequest3);
                                MtkPhoneInterfaceManagerEx.log("Exit ECBM mode receiver " + ecmExitReceiver);
                                MtkPhoneInterfaceManagerEx.this.mApp.registerReceiver(ecmExitReceiver, intentFilter);
                                MtkPhoneInterfaceManagerEx.getPhone(num.intValue()).exitEmergencyCallbackMode();
                                return;
                            }
                            mainThreadRequest3.result = MtkPhoneInterfaceManagerEx.this.new EcmExitResult(null);
                            synchronized (mainThreadRequest3) {
                                mainThreadRequest3.notifyAll();
                                break;
                            }
                            return;
                        default:
                            switch (i) {
                                case 102:
                                    MainThreadRequest mainThreadRequest4 = (MainThreadRequest) message.obj;
                                    MtkIccAPDUArgument mtkIccAPDUArgument = (MtkIccAPDUArgument) mainThreadRequest4.argument;
                                    IccCard uiccCardFromRequest = MtkPhoneInterfaceManagerEx.this.getUiccCardFromRequest(mainThreadRequest4);
                                    if (uiccCardFromRequest == null) {
                                        MtkPhoneInterfaceManagerEx.loge("iccExchangeSimIOExUsingSlot: No UICC");
                                        mainThreadRequest4.result = new IccIoResult(111, 0, (byte[]) null);
                                        synchronized (mainThreadRequest4) {
                                            mainThreadRequest4.notifyAll();
                                            break;
                                        }
                                        return;
                                    }
                                    uiccCardFromRequest.iccExchangeSimIOEx(mtkIccAPDUArgument.cla, mtkIccAPDUArgument.command, mtkIccAPDUArgument.p1, mtkIccAPDUArgument.p2, mtkIccAPDUArgument.p3, mtkIccAPDUArgument.pathId, mtkIccAPDUArgument.data, mtkIccAPDUArgument.pin2, obtainMessage(103, mainThreadRequest4));
                                    return;
                                case 103:
                                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                                    MainThreadRequest mainThreadRequest5 = (MainThreadRequest) asyncResult2.userObj;
                                    MtkPhoneInterfaceManagerEx.log("EVENT_EXCHANGE_SIM_IO_EX_DONE");
                                    if (asyncResult2.exception == null && asyncResult2.result != null) {
                                        mainThreadRequest5.result = asyncResult2.result;
                                    } else {
                                        mainThreadRequest5.result = new IccIoResult(111, 0, (byte[]) null);
                                        if (asyncResult2.result == null) {
                                            MtkPhoneInterfaceManagerEx.loge("iccExchangeSimIOExUsingSlot: Empty response");
                                        } else if (asyncResult2.exception != null && (asyncResult2.exception instanceof CommandException)) {
                                            MtkPhoneInterfaceManagerEx.loge("iccExchangeSimIOExUsingSlot: CommandException: " + asyncResult2.exception);
                                        } else {
                                            MtkPhoneInterfaceManagerEx.loge("iccExchangeSimIOExUsingSlot: Unknown exception");
                                        }
                                    }
                                    synchronized (mainThreadRequest5) {
                                        mainThreadRequest5.notifyAll();
                                        break;
                                    }
                                    return;
                                case 104:
                                    MainThreadRequest mainThreadRequest6 = (MainThreadRequest) message.obj;
                                    IccCard uiccCardFromRequest2 = MtkPhoneInterfaceManagerEx.this.getUiccCardFromRequest(mainThreadRequest6);
                                    if (uiccCardFromRequest2 == null) {
                                        MtkPhoneInterfaceManagerEx.loge("get ATR: No UICC");
                                        mainThreadRequest6.result = "";
                                        synchronized (mainThreadRequest6) {
                                            mainThreadRequest6.notifyAll();
                                            break;
                                        }
                                        return;
                                    }
                                    uiccCardFromRequest2.iccGetAtr(obtainMessage(105, mainThreadRequest6));
                                    return;
                                case 105:
                                    AsyncResult asyncResult3 = (AsyncResult) message.obj;
                                    MainThreadRequest mainThreadRequest7 = (MainThreadRequest) asyncResult3.userObj;
                                    if (asyncResult3.exception == null) {
                                        MtkPhoneInterfaceManagerEx.log("EVENT_GET_ATR_DONE, no exception");
                                        mainThreadRequest7.result = asyncResult3.result;
                                    } else {
                                        MtkPhoneInterfaceManagerEx.loge("EVENT_GET_ATR_DONE, exception happens");
                                        mainThreadRequest7.result = "";
                                    }
                                    synchronized (mainThreadRequest7) {
                                        mainThreadRequest7.notifyAll();
                                        break;
                                    }
                                    return;
                                default:
                                    switch (i) {
                                        case 108:
                                            MainThreadRequest mainThreadRequest8 = (MainThreadRequest) message.obj;
                                            MtkIccAPDUArgument mtkIccAPDUArgument2 = (MtkIccAPDUArgument) mainThreadRequest8.argument;
                                            MtkPhoneInterfaceManagerEx.log("CMD_LOAD_EF_TRANSPARENT: slot " + mtkIccAPDUArgument2.slotId);
                                            IccFileHandler iccFileHandler = UiccController.getInstance().getIccFileHandler(mtkIccAPDUArgument2.slotId, mtkIccAPDUArgument2.family);
                                            if (iccFileHandler == null) {
                                                MtkPhoneInterfaceManagerEx.loge("loadEFTransparent: No UICC");
                                                mainThreadRequest8.result = new AsyncResult((Object) null, (Object) null, (Throwable) null);
                                                synchronized (mainThreadRequest8) {
                                                    mainThreadRequest8.notifyAll();
                                                    break;
                                                }
                                                return;
                                            }
                                            iccFileHandler.loadEFTransparent(mtkIccAPDUArgument2.cla, mtkIccAPDUArgument2.pathId, obtainMessage(109, mainThreadRequest8));
                                            return;
                                        case 109:
                                            MtkPhoneInterfaceManagerEx.log("EVENT_LOAD_EF_TRANSPARENT_DONE");
                                            AsyncResult asyncResult4 = (AsyncResult) message.obj;
                                            MainThreadRequest mainThreadRequest9 = (MainThreadRequest) asyncResult4.userObj;
                                            if (asyncResult4.exception == null && asyncResult4.result != null) {
                                                mainThreadRequest9.result = new AsyncResult((Object) null, (byte[]) asyncResult4.result, (Throwable) null);
                                            } else {
                                                mainThreadRequest9.result = new AsyncResult((Object) null, (Object) null, (Throwable) null);
                                            }
                                            synchronized (mainThreadRequest9) {
                                                mainThreadRequest9.notifyAll();
                                                break;
                                            }
                                            return;
                                        case 110:
                                            MainThreadRequest mainThreadRequest10 = (MainThreadRequest) message.obj;
                                            MtkIccAPDUArgument mtkIccAPDUArgument3 = (MtkIccAPDUArgument) mainThreadRequest10.argument;
                                            MtkPhoneInterfaceManagerEx.log("CMD_LOAD_EF_LINEARFIXEDALL: slot " + mtkIccAPDUArgument3.slotId);
                                            IccFileHandler iccFileHandler2 = UiccController.getInstance().getIccFileHandler(mtkIccAPDUArgument3.slotId, mtkIccAPDUArgument3.family);
                                            if (iccFileHandler2 == null) {
                                                MtkPhoneInterfaceManagerEx.loge("loadEFLinearFixedAll: No UICC");
                                                mainThreadRequest10.result = new AsyncResult((Object) null, (Object) null, (Throwable) null);
                                                synchronized (mainThreadRequest10) {
                                                    mainThreadRequest10.notifyAll();
                                                    break;
                                                }
                                                return;
                                            }
                                            iccFileHandler2.loadEFLinearFixedAll(mtkIccAPDUArgument3.cla, mtkIccAPDUArgument3.pathId, obtainMessage(111, mainThreadRequest10));
                                            return;
                                        case 111:
                                            MtkPhoneInterfaceManagerEx.log("EVENT_LOAD_EF_LINEARFIXEDALL_DONE");
                                            AsyncResult asyncResult5 = (AsyncResult) message.obj;
                                            MainThreadRequest mainThreadRequest11 = (MainThreadRequest) asyncResult5.userObj;
                                            if (asyncResult5.exception == null && asyncResult5.result != null) {
                                                mainThreadRequest11.result = new AsyncResult((Object) null, (ArrayList) asyncResult5.result, (Throwable) null);
                                            } else {
                                                mainThreadRequest11.result = new AsyncResult((Object) null, (Object) null, (Throwable) null);
                                            }
                                            synchronized (mainThreadRequest11) {
                                                mainThreadRequest11.notifyAll();
                                                break;
                                            }
                                            return;
                                        case 112:
                                            MainThreadRequest mainThreadRequest12 = (MainThreadRequest) message.obj;
                                            if (mainThreadRequest12.subId.intValue() != -1) {
                                                MtkPhoneInterfaceManagerEx.this.mPhone = MtkPhoneInterfaceManagerEx.getPhone(mainThreadRequest12.subId.intValue());
                                            }
                                            if (MtkPhoneInterfaceManagerEx.this.mPhone == null) {
                                                Log.e("MtkPhoneIntfMgrEx", "MainThreadHandler: mPhone == null ");
                                                return;
                                            } else {
                                                MtkPhoneInterfaceManagerEx.this.mPhone.invokeOemRilRequestRaw((byte[]) mainThreadRequest12.argument, obtainMessage(113, mainThreadRequest12));
                                                return;
                                            }
                                        case 113:
                                            AsyncResult asyncResult6 = (AsyncResult) message.obj;
                                            MainThreadRequest mainThreadRequest13 = (MainThreadRequest) asyncResult6.userObj;
                                            mainThreadRequest13.result = asyncResult6;
                                            synchronized (mainThreadRequest13) {
                                                mainThreadRequest13.notifyAll();
                                                break;
                                            }
                                            return;
                                        default:
                                            Log.w("MtkPhoneIntfMgrEx", "MainThreadHandler: unexpected message code: " + message.what);
                                            return;
                                    }
                            }
                    }
            }
        }

        private void handleNullReturnEvent(Message message, String str) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            MainThreadRequest mainThreadRequest = (MainThreadRequest) asyncResult.userObj;
            if (asyncResult.exception == null) {
                mainThreadRequest.result = true;
            } else {
                mainThreadRequest.result = false;
                if (asyncResult.exception instanceof CommandException) {
                    MtkPhoneInterfaceManagerEx.loge(str + ": CommandException: " + asyncResult.exception);
                } else {
                    MtkPhoneInterfaceManagerEx.loge(str + ": Unknown exception");
                }
            }
            synchronized (mainThreadRequest) {
                mainThreadRequest.notifyAll();
            }
        }
    }

    private Object sendRequest(int i, Object obj) {
        return sendRequest(i, obj, -1);
    }

    private Object sendRequest(int i, Object obj, Integer num) {
        if (Looper.myLooper() == this.mMainThreadHandler.getLooper()) {
            throw new RuntimeException("This method will deadlock if called from the main thread.");
        }
        MainThreadRequest mainThreadRequest = new MainThreadRequest(obj, num);
        this.mMainThreadHandler.obtainMessage(i, mainThreadRequest).sendToTarget();
        synchronized (mainThreadRequest) {
            while (mainThreadRequest.result == null) {
                try {
                    mainThreadRequest.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return mainThreadRequest.result;
    }

    public static MtkPhoneInterfaceManagerEx init(PhoneGlobals phoneGlobals, Phone phone) {
        MtkPhoneInterfaceManagerEx mtkPhoneInterfaceManagerEx;
        synchronized (MtkPhoneInterfaceManagerEx.class) {
            if (sInstance == null) {
                sInstance = new MtkPhoneInterfaceManagerEx(phoneGlobals, phone);
            } else {
                Log.wtf("MtkPhoneIntfMgrEx", "init() called multiple times!  sInstance = " + sInstance);
            }
            mtkPhoneInterfaceManagerEx = sInstance;
        }
        return mtkPhoneInterfaceManagerEx;
    }

    protected MtkPhoneInterfaceManagerEx(PhoneGlobals phoneGlobals, Phone phone) {
        this.mApp = phoneGlobals;
        this.mPhone = phone;
        this.mUserManager = (UserManager) phoneGlobals.getSystemService("user");
        this.mAppOps = (AppOpsManager) phoneGlobals.getSystemService("appops");
        this.mTelephonySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        publish();
    }

    private void publish() {
        Log.i("MtkPhoneIntfMgrEx", "publish: " + this);
        ServiceManager.addService("phoneEx", this);
    }

    private Phone getPhoneFromRequest(MainThreadRequest mainThreadRequest) {
        return mainThreadRequest.subId.intValue() == -1 ? this.mPhone : getPhone(mainThreadRequest.subId.intValue());
    }

    private static Phone getPhone(int i) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(i);
        if (phoneId < 0) {
            phoneId = Integer.MAX_VALUE;
        }
        return PhoneFactory.getPhone(phoneId);
    }

    protected static void log(String str) {
        Log.e("MtkPhoneIntfMgrEx", str);
    }

    protected static void loge(String str) {
        Log.e("MtkPhoneIntfMgrEx", str);
    }

    public void setTelLog(boolean z) {
        log("setTelLog enable = " + z);
        int i = 0;
        if (SystemProperties.getInt("persist.vendor.log.tel_log_ctrl", 0) != 1) {
            return;
        }
        if (z) {
            for (String str : PROPERTY_M_LOG_TAG_COMMON_RIL) {
                SystemProperties.set(str, "D");
            }
            for (String str2 : PROPERTY_M_LOG_TAG) {
                SystemProperties.set(str2, "D");
            }
            String[] strArr = PROPERTY_M_LOG_TAG_V;
            int length = strArr.length;
            while (i < length) {
                SystemProperties.set(strArr[i], "V");
                i++;
            }
            return;
        }
        for (String str3 : PROPERTY_M_LOG_TAG_COMMON_RIL) {
            SystemProperties.set(str3, "I");
        }
        if (!SystemProperties.get("ro.build.type").equals("eng")) {
            for (String str4 : PROPERTY_M_LOG_TAG) {
                SystemProperties.set(str4, "I");
            }
            String[] strArr2 = PROPERTY_M_LOG_TAG_V;
            int length2 = strArr2.length;
            while (i < length2) {
                SystemProperties.set(strArr2[i], "I");
                i++;
            }
        }
    }

    private class UnlockSim extends Thread {
        private boolean mDone;
        private Handler mHandler;
        private boolean mResult;
        private int mRetryCount;
        private int mSIMMELockRetryCount;
        private MtkUiccProfile mSimCard;
        private int mUnlockResult;
        private int mVerifyResult;

        public UnlockSim(IccCard iccCard) {
            this.mSimCard = null;
            this.mDone = false;
            this.mResult = false;
            this.mVerifyResult = -1;
            this.mSIMMELockRetryCount = -1;
            this.mRetryCount = -1;
            this.mUnlockResult = 0;
            if (iccCard instanceof MtkUiccProfile) {
                this.mSimCard = (MtkUiccProfile) iccCard;
            } else {
                MtkPhoneInterfaceManagerEx.log("UnlockSim: Not MtkUiccProfile instance.");
            }
        }

        public UnlockSim() {
            this.mSimCard = null;
            this.mDone = false;
            this.mResult = false;
            this.mVerifyResult = -1;
            this.mSIMMELockRetryCount = -1;
            this.mRetryCount = -1;
            this.mUnlockResult = 0;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (this) {
                this.mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        switch (message.what) {
                            case 100:
                                synchronized (UnlockSim.this) {
                                    int[] iArr = (int[]) asyncResult.result;
                                    if (asyncResult.exception != null) {
                                        MtkPhoneInterfaceManagerEx.log("Query network lock fail");
                                        UnlockSim.this.mResult = false;
                                    } else {
                                        UnlockSim.this.mSIMMELockRetryCount = iArr[2];
                                        MtkPhoneInterfaceManagerEx.log("[SIMQUERY] Category = " + iArr[0] + " ,Network status =" + iArr[1] + " ,Retry count = " + iArr[2]);
                                        UnlockSim.this.mResult = true;
                                    }
                                    UnlockSim.this.mDone = true;
                                    UnlockSim.this.notifyAll();
                                    break;
                                }
                                return;
                            case 101:
                                MtkPhoneInterfaceManagerEx.log("SUPPLY_NETWORK_LOCK_COMPLETE");
                                synchronized (UnlockSim.this) {
                                    if (asyncResult.exception == null || !(asyncResult.exception instanceof CommandException)) {
                                        UnlockSim.this.mVerifyResult = 0;
                                    } else {
                                        MtkPhoneInterfaceManagerEx.log("ar.exception " + asyncResult.exception);
                                        if (asyncResult.exception.getCommandError() == CommandException.Error.PASSWORD_INCORRECT) {
                                            UnlockSim.this.mVerifyResult = 1;
                                        } else {
                                            UnlockSim.this.mVerifyResult = 2;
                                        }
                                    }
                                    UnlockSim.this.mDone = true;
                                    UnlockSim.this.notifyAll();
                                    break;
                                }
                                return;
                            case 102:
                                MtkPhoneInterfaceManagerEx.log("SET_DEVICE_NETWORK_LOCK_COMPLETE");
                                synchronized (UnlockSim.this) {
                                    UnlockSim.this.mRetryCount = message.arg1;
                                    if (asyncResult.exception == null) {
                                        UnlockSim.this.mUnlockResult = 3;
                                    } else if (!(asyncResult.exception instanceof CommandException) || asyncResult.exception.getCommandError() != CommandException.Error.PASSWORD_INCORRECT) {
                                        UnlockSim.this.mUnlockResult = 2;
                                    } else {
                                        UnlockSim.this.mUnlockResult = 1;
                                    }
                                    UnlockSim.this.mDone = true;
                                    UnlockSim.this.notifyAll();
                                    break;
                                }
                                return;
                            default:
                                return;
                        }
                    }
                };
                notifyAll();
            }
            Looper.loop();
        }

        synchronized Bundle queryNetworkLock(int i) {
            Bundle bundle;
            while (this.mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            MtkPhoneInterfaceManagerEx.log("Enter queryNetworkLock");
            this.mSimCard.queryIccNetworkLock(i, Message.obtain(this.mHandler, 100));
            while (!this.mDone) {
                try {
                    MtkPhoneInterfaceManagerEx.log("wait for done");
                    wait();
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                }
            }
            try {
                this.mHandler.getLooper().quit();
            } catch (NullPointerException e3) {
                MtkPhoneInterfaceManagerEx.loge("queryNetworkLock Null looper");
                e3.printStackTrace();
            }
            if (this.mHandler.getLooper().getThread() != null) {
                this.mHandler.getLooper().getThread().interrupt();
                bundle = new Bundle();
                bundle.putBoolean("com.mediatek.phone.QUERY_SIMME_LOCK_RESULT", this.mResult);
                bundle.putInt("com.mediatek.phone.SIMME_LOCK_LEFT_COUNT", this.mSIMMELockRetryCount);
                MtkPhoneInterfaceManagerEx.log("done");
            } else {
                bundle = new Bundle();
                bundle.putBoolean("com.mediatek.phone.QUERY_SIMME_LOCK_RESULT", this.mResult);
                bundle.putInt("com.mediatek.phone.SIMME_LOCK_LEFT_COUNT", this.mSIMMELockRetryCount);
                MtkPhoneInterfaceManagerEx.log("done");
            }
            return bundle;
        }

        synchronized int supplyNetworkLock(String str) {
            while (this.mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            MtkPhoneInterfaceManagerEx.log("Enter supplyNetworkLock");
            this.mSimCard.supplyNetworkDepersonalization(str, Message.obtain(this.mHandler, 101));
            while (!this.mDone) {
                try {
                    MtkPhoneInterfaceManagerEx.log("wait for done");
                    wait();
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                }
            }
            try {
                this.mHandler.getLooper().quit();
            } catch (NullPointerException e3) {
                MtkPhoneInterfaceManagerEx.loge("supplyNetworkLock Null looper");
                e3.printStackTrace();
            }
            if (this.mHandler.getLooper().getThread() != null) {
                this.mHandler.getLooper().getThread().interrupt();
                MtkPhoneInterfaceManagerEx.log("done");
            } else {
                MtkPhoneInterfaceManagerEx.log("done");
            }
            return this.mVerifyResult;
        }

        synchronized int[] supplyDeviceNetworkLock(String str) {
            while (this.mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            UiccController.getInstance().supplyDeviceNetworkDepersonalization(str, Message.obtain(this.mHandler, 102));
            while (!this.mDone) {
                try {
                    Log.d("MtkPhoneIntfMgrEx", "wait for done");
                    wait();
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                }
            }
            MtkPhoneInterfaceManagerEx.log("done");
            return new int[]{this.mUnlockResult, this.mRetryCount};
        }
    }

    public Bundle queryNetworkLock(int i, int i2) {
        log("queryNetworkLock");
        UnlockSim unlockSim = new UnlockSim(getPhone(i).getIccCard());
        if (unlockSim.mSimCard == null) {
            return null;
        }
        unlockSim.start();
        return unlockSim.queryNetworkLock(i2);
    }

    public int supplyNetworkDepersonalization(int i, String str) {
        log("supplyNetworkDepersonalization");
        UnlockSim unlockSim = new UnlockSim(getPhone(i).getIccCard());
        unlockSim.start();
        return unlockSim.supplyNetworkLock(str);
    }

    public int[] supplyDeviceNetworkDepersonalization(String str) {
        log("supplyDeviceNetworkDepersonalization");
        int[] iArr = new int[2];
        if (TextUtils.isEmpty(str) || str.length() < 1) {
            iArr[0] = -1;
            iArr[1] = -1;
            return iArr;
        }
        if (this.mIsSmlLockMode) {
            int simLockPolicy = MtkTelephonyManagerEx.getDefault().getSimLockPolicy();
            if (simLockPolicy == -1) {
                iArr[0] = -1;
                iArr[1] = -1;
                return iArr;
            }
            if (simLockPolicy == 0) {
                iArr[0] = 0;
                iArr[1] = -1;
                return iArr;
            }
            UnlockSim unlockSim = new UnlockSim();
            unlockSim.start();
            return unlockSim.supplyDeviceNetworkLock(str);
        }
        iArr[0] = 0;
        iArr[1] = -1;
        return iArr;
    }

    public void repollIccStateForNetworkLock(int i, boolean z) {
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            getPhone(i).getIccCard().repollIccStateForModemSmlChangeFeatrue(z);
        } else {
            log("Not Support in Single SIM.");
        }
    }

    public boolean isInHomeNetwork(int i) {
        boolean zInSameCountry;
        ServiceState serviceState;
        int phoneId = SubscriptionManager.getPhoneId(i);
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null && (serviceState = phone.getServiceState()) != null) {
            zInSameCountry = inSameCountry(phoneId, serviceState.getVoiceOperatorNumeric());
        } else {
            zInSameCountry = false;
        }
        log("isInHomeNetwork, subId=" + i + " ,phoneId=" + phoneId + " ,isInHomeNetwork=" + zInSameCountry);
        return zInSameCountry;
    }

    private static final boolean inSameCountry(int i, String str) {
        boolean z = true;
        if (TextUtils.isEmpty(str) || str.length() < 5 || !TextUtils.isDigitsOnly(str)) {
            log("inSameCountry, Not a valid network, phoneId=" + i + ", operatorNumeric=" + str);
            return true;
        }
        String homeOperatorNumeric = getHomeOperatorNumeric(i);
        if (TextUtils.isEmpty(homeOperatorNumeric) || homeOperatorNumeric.length() < 5 || !TextUtils.isDigitsOnly(homeOperatorNumeric)) {
            log("inSameCountry, Not a valid SIM MCC, phoneId=" + i + ", homeNumeric=" + homeOperatorNumeric);
            return true;
        }
        String strSubstring = str.substring(0, 3);
        String strSubstring2 = homeOperatorNumeric.substring(0, 3);
        String strCountryCodeForMcc = MccTable.countryCodeForMcc(Integer.parseInt(strSubstring));
        String strCountryCodeForMcc2 = MccTable.countryCodeForMcc(Integer.parseInt(strSubstring2));
        log("inSameCountry, phoneId=" + i + ", homeMCC=" + strSubstring2 + ", networkMCC=" + strSubstring + ", homeCountry=" + strCountryCodeForMcc2 + ", networkCountry=" + strCountryCodeForMcc);
        if (strCountryCodeForMcc.isEmpty() || strCountryCodeForMcc2.isEmpty()) {
            return true;
        }
        boolean zEquals = strCountryCodeForMcc2.equals(strCountryCodeForMcc);
        if (zEquals) {
            return zEquals;
        }
        if ((!"us".equals(strCountryCodeForMcc2) || !"vi".equals(strCountryCodeForMcc)) && ((!"vi".equals(strCountryCodeForMcc2) || !"us".equals(strCountryCodeForMcc)) && (!"cn".equals(strCountryCodeForMcc2) || !"mo".equals(strCountryCodeForMcc)))) {
            z = zEquals;
        }
        log("inSameCountry, phoneId=" + i + ", inSameCountry=" + z);
        return z;
    }

    private static final String getHomeOperatorNumeric(int i) {
        String simOperatorNumericForPhone = TelephonyManager.getDefault().getSimOperatorNumericForPhone(i);
        if (TextUtils.isEmpty(simOperatorNumericForPhone)) {
            simOperatorNumericForPhone = SystemProperties.get("ro.cdma.home.operator.numeric", "");
        }
        if (MtkTelephonyManagerEx.getDefault().isCt3gDualMode(i) && "20404".equals(simOperatorNumericForPhone)) {
            simOperatorNumericForPhone = "46003";
        }
        log("getHomeOperatorNumeric, phoneId=" + i + ", numeric=" + simOperatorNumericForPhone);
        return simOperatorNumericForPhone;
    }

    public Bundle getCellLocationUsingSlotId(int i) {
        enforceFineOrCoarseLocationPermission("getCellLocationUsingSlotId");
        Bundle bundle = new Bundle();
        Phone phone = getPhone(getSubIdBySlot(i));
        if (phone == null) {
            return null;
        }
        phone.getCellLocation((WorkSource) null).fillInNotifierBundle(bundle);
        return bundle;
    }

    private void enforceFineOrCoarseLocationPermission(String str) {
        try {
            this.mApp.enforceCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION", null);
        } catch (SecurityException e) {
            this.mApp.enforceCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION", str);
        }
    }

    private int getSubIdBySlot(int i) {
        int[] subId = SubscriptionManager.getSubId(i);
        if (subId == null || subId.length == 0) {
            return getDefaultSubscription();
        }
        log("getSubIdBySlot, simId " + i + "subId " + subId[0]);
        return subId[0];
    }

    public String getIccAtr(int i) {
        enforceModifyPermissionOrCarrierPrivilege(i);
        log("> getIccAtr , subId = " + i);
        String str = (String) sendRequest(104, null, Integer.valueOf(i));
        log("< getIccAtr: " + str);
        return str;
    }

    public byte[] iccExchangeSimIOEx(int i, int i2, int i3, int i4, int i5, int i6, String str, String str2, String str3) {
        byte[] bArr;
        int length;
        enforceModifyPermissionOrCarrierPrivilege(i);
        log("Exchange SIM_IO Ex " + i2 + ":" + i3 + " " + i4 + " " + i5 + " " + i6 + ":" + str + ", " + str2 + ", " + str3 + ", subId = " + i);
        IccIoResult iccIoResult = (IccIoResult) sendRequest(102, new MtkIccAPDUArgument(-1, i2, i3, i4, i5, i6, str2, str, str3), Integer.valueOf(i));
        StringBuilder sb = new StringBuilder();
        sb.append("Exchange SIM_IO Ex [R]");
        sb.append(iccIoResult);
        log(sb.toString());
        if (iccIoResult.payload != null) {
            length = iccIoResult.payload.length + 2;
            bArr = new byte[length];
            System.arraycopy(iccIoResult.payload, 0, bArr, 0, iccIoResult.payload.length);
        } else {
            bArr = new byte[2];
            length = 2;
        }
        log("Exchange SIM_IO Ex [L] " + length);
        bArr[length + (-1)] = (byte) iccIoResult.sw2;
        bArr[length - 2] = (byte) iccIoResult.sw1;
        return bArr;
    }

    public byte[] loadEFTransparent(int i, int i2, int i3, String str) {
        enforceModifyPermissionOrCarrierPrivilege(getSubIdBySlot(i));
        log("loadEFTransparent slot " + i + " " + i2 + " " + i3 + ":" + str);
        byte[] bArr = (byte[]) ((AsyncResult) sendRequest(108, new MtkIccAPDUArgument(i, i2, -1, i3, 176, 0, 0, 0, str))).result;
        StringBuilder sb = new StringBuilder();
        sb.append("loadEFTransparent ");
        sb.append(bArr);
        log(sb.toString());
        return bArr;
    }

    public List<String> loadEFLinearFixedAll(int i, int i2, int i3, String str) {
        enforceModifyPermissionOrCarrierPrivilege(getSubIdBySlot(i));
        log("loadEFLinearFixedAll slot " + i + " " + i2 + " " + i3 + ":" + str);
        ArrayList arrayList = (ArrayList) ((AsyncResult) sendRequest(110, new MtkIccAPDUArgument(i, i2, -1, i3, 178, 0, 0, 0, str))).result;
        if (arrayList == null) {
            log("loadEFLinearFixedAll return null");
            return null;
        }
        ArrayList arrayList2 = new ArrayList();
        for (int i4 = 0; i4 < arrayList.size(); i4++) {
            if (arrayList.get(i4) != null) {
                arrayList2.add(IccUtils.bytesToHexString((byte[]) arrayList.get(i4)));
            }
        }
        log("loadEFLinearFixedAll " + arrayList2);
        return arrayList2;
    }

    public String getIccCardType(int i) {
        log("getIccCardType  subId=" + i);
        Phone phone = getPhone(i);
        if (phone == null) {
            log("getIccCardType(): phone is null");
            return "";
        }
        return phone.getIccCard().getIccCardType();
    }

    public boolean isAppTypeSupported(int i, int i2) {
        log("isAppTypeSupported  slotId=" + i);
        UiccCard uiccCard = UiccController.getInstance().getUiccCard(i);
        if (uiccCard != null) {
            return uiccCard.getApplicationByType(i2) != null;
        }
        log("isAppTypeSupported(): uiccCard is null");
        return false;
    }

    public boolean isTestIccCard(int i) {
        String str = SystemProperties.get(PROPERTY_RIL_TEST_SIM[i], "");
        log("isTestIccCard(): slot id =" + i + ", iccType = " + str);
        return str != null && str.equals(SettingsConstants.DUA_VAL_ON);
    }

    public int getIccAppFamily(int i) {
        int simCount = TelephonyManager.getDefault().getSimCount();
        int i2 = 0;
        if (i < 0 || i >= simCount) {
            log("getIccAppFamily, invalid slotId:" + i);
            return 0;
        }
        String str = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[i]);
        String[] strArrSplit = str.split(",");
        int i3 = 0;
        for (int i4 = 0; i4 < strArrSplit.length; i4++) {
            if ("USIM".equals(strArrSplit[i4])) {
                i3 |= 2;
            } else if ("SIM".equals(strArrSplit[i4])) {
                i3 |= 1;
            } else if ("CSIM".equals(strArrSplit[i4])) {
                i3 |= 4;
            } else if ("RUIM".equals(strArrSplit[i4])) {
                i3 |= 8;
            }
        }
        if (i3 != 0) {
            int i5 = i3 & 4;
            if (i5 != 0 && (i3 & 2) != 0) {
                i2 = 3;
            } else {
                i2 = (i5 == 0 && (i3 & 8) == 0 && (i3 != 1 || !SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get(PROPERTY_RIL_CT3G[i])))) ? 1 : 2;
            }
        }
        Log.i("MtkPhoneIntfMgrEx", "getIccAppFamily, uiccType[" + i + "] = " + str + "fullType = " + i3 + " iccType = " + i2);
        return i2;
    }

    private void enforceModifyPermissionOrCarrierPrivilege(int i) {
        if (this.mApp.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") == 0) {
            return;
        }
        log("No modify permission, check carrier privilege next.");
        enforceCarrierPrivilege(i);
    }

    private void enforcePrivilegedPhoneStatePermission() {
        this.mApp.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", null);
    }

    private void enforceCarrierPrivilege(int i) {
        if (getCarrierPrivilegeStatus(i) != 1) {
            loge("No Carrier Privilege.");
            throw new SecurityException("No Carrier Privilege.");
        }
    }

    private int getCarrierPrivilegeStatus(int i) {
        Phone phone = getPhone(i);
        if (phone == null) {
            loge("getCarrierPrivilegeStatus: Invalid subId");
            return 0;
        }
        UiccCard uiccCard = UiccController.getInstance().getUiccCard(phone.getPhoneId());
        if (uiccCard == null) {
            loge("getCarrierPrivilegeStatus: No UICC");
            return -1;
        }
        return uiccCard.getCarrierPrivilegeStatusForCurrentTransaction(phone.getContext().getPackageManager());
    }

    private IccCard getUiccCardFromRequest(MainThreadRequest mainThreadRequest) {
        Phone phoneFromRequest = getPhoneFromRequest(mainThreadRequest);
        if (phoneFromRequest == null) {
            return null;
        }
        return phoneFromRequest.getIccCard();
    }

    private int getDefaultSubscription() {
        return this.mSubscriptionController.getDefaultSubId();
    }

    public String getMvnoMatchType(int i) {
        String mvnoMatchType = getPhone(i).getMvnoMatchType();
        log("getMvnoMatchType sub = " + i + " ,type = " + mvnoMatchType);
        return mvnoMatchType;
    }

    public String getMvnoPattern(int i, String str) {
        String mvnoPattern = getPhone(i).getMvnoPattern(str);
        log("getMvnoPattern sub = " + i + " ,pattern = " + mvnoPattern);
        return mvnoPattern;
    }

    public boolean isRadioOffBySimManagement(int i) {
        boolean zContains = true;
        try {
            SharedPreferences sharedPreferences = this.mApp.createPackageContext("com.android.phone", 2).getSharedPreferences("RADIO_STATUS", 0);
            if (SubscriptionController.getInstance() != null) {
                int phoneId = SubscriptionController.getInstance().getPhoneId(i);
                if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
                    String str = SystemProperties.get(ICCRECORD_PROPERTY_ICCID[phoneId], "");
                    if (str != null && sharedPreferences != null) {
                        log("[isRadioOffBySimManagement]SharedPreferences: " + sharedPreferences.getAll().size() + ", IccId: " + str);
                        zContains = sharedPreferences.contains(RadioManager.getInstance().getHashCode(str));
                    }
                }
                log("[isRadioOffBySimManagement]mSlotId: " + phoneId);
                return false;
            }
            log("[isRadioOffBySimManagement]result: " + zContains);
        } catch (PackageManager.NameNotFoundException e) {
            log("Fail to create com.android.phone createPackageContext");
        }
        return zContains;
    }

    public boolean isFdnEnabled(int i) {
        log("isFdnEnabled subId=" + i);
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            loge("Error subId: " + i);
            return false;
        }
        Phone phone = getPhone(i);
        return phone != null && phone.getIccCard() != null && phone.getIccCard().getIccFdnAvailable() && phone.getIccCard().getIccFdnEnabled();
    }

    private boolean canReadPhoneState(String str, String str2) {
        try {
            this.mApp.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", str2);
            return true;
        } catch (SecurityException e) {
            this.mApp.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", str2);
            if (this.mAppOps.noteOp(51, Binder.getCallingUid(), str) == 0) {
                return true;
            }
            return false;
        }
    }

    public String getUimSubscriberId(String str, int i) {
        if (!canReadPhoneState(str, "getUimSubscriberId")) {
            log("getUimImsiBySubId: permission denied");
            return null;
        }
        int phoneId = SubscriptionManager.getPhoneId(i);
        if (phoneId < 0 || phoneId >= PROPERTY_UIM_SUBSCRIBER_ID.length) {
            log("getUimImsiBySubId:invalid phoneId " + phoneId);
            return null;
        }
        return SystemProperties.get(PROPERTY_UIM_SUBSCRIBER_ID[phoneId], "");
    }

    public String getSimSerialNumber(String str, int i) {
        if (!canReadPhoneState(str, "getSimSerialNumber")) {
            log("getSimSerialNumber: permission denied");
            return null;
        }
        return SystemProperties.get(this.PROPERTY_ICCID_SIM[i], "");
    }

    public boolean setRadioCapability(RadioAccessFamily[] radioAccessFamilyArr) {
        try {
            ProxyController.getInstance().setRadioCapability(radioAccessFamilyArr);
            return true;
        } catch (RuntimeException e) {
            Log.w("MtkPhoneIntfMgrEx", "setRadioCapability: Runtime Exception");
            e.printStackTrace();
            return false;
        }
    }

    public boolean isCapabilitySwitching() {
        return ProxyController.getInstance().isCapabilitySwitching();
    }

    public int getMainCapabilityPhoneId() {
        return RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
    }

    public boolean isImsRegistered(int i) {
        Phone phone = getPhone(i);
        if (phone == null) {
            return false;
        }
        boolean zIsImsRegistered = phone.isImsRegistered();
        log("isImsRegistered(" + i + ")=" + zIsImsRegistered);
        return zIsImsRegistered;
    }

    public boolean isVolteEnabled(int i) {
        Phone phone = getPhone(i);
        if (phone == null) {
            return false;
        }
        boolean zIsVolteEnabled = phone.isVolteEnabled();
        log("isVolteEnabled=(" + i + ")=" + zIsVolteEnabled);
        return zIsVolteEnabled;
    }

    public boolean isWifiCallingEnabled(int i) {
        Phone phone = getPhone(i);
        if (phone == null) {
            return false;
        }
        boolean zIsWifiCallingEnabled = phone.isWifiCallingEnabled();
        log("isWifiCallingEnabled(" + i + ")=" + zIsWifiCallingEnabled);
        return zIsWifiCallingEnabled;
    }

    private static class SimAuth extends Thread {
        private Handler mHandler;
        private Phone mTargetPhone;
        private boolean mDone = false;
        private IccIoResult mResponse = null;

        public SimAuth(Phone phone) {
            this.mTargetPhone = phone;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (this) {
                this.mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        if (message.what == 300) {
                            MtkPhoneInterfaceManagerEx.log("SIM_AUTH_GENERAL_COMPLETE");
                            synchronized (SimAuth.this) {
                                if (asyncResult.exception != null) {
                                    MtkPhoneInterfaceManagerEx.log("SIM Auth Fail");
                                    SimAuth.this.mResponse = (IccIoResult) asyncResult.result;
                                } else {
                                    SimAuth.this.mResponse = (IccIoResult) asyncResult.result;
                                }
                                MtkPhoneInterfaceManagerEx.log("SIM_AUTH_GENERAL_COMPLETE result is " + SimAuth.this.mResponse);
                                SimAuth.this.mDone = true;
                                SimAuth.this.notifyAll();
                            }
                        }
                    }
                };
                notifyAll();
            }
            Looper.loop();
        }

        byte[] doGeneralSimAuth(int i, int i2, int i3, int i4, String str, String str2) {
            synchronized (this) {
                while (this.mHandler == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                this.mDone = false;
                byte[] bArr = null;
                this.mResponse = null;
                Message messageObtain = Message.obtain(this.mHandler, TimeConsumingPreferenceActivity.EXCEPTION_ERROR);
                int iccApplicationChannel = UiccController.getInstance().getIccApplicationChannel(i, i2);
                MtkPhoneInterfaceManagerEx.log("family = " + i2 + ", sessionId = " + iccApplicationChannel);
                int[] subId = SubscriptionManager.getSubId(i);
                if (subId == null) {
                    MtkPhoneInterfaceManagerEx.log("slotId = " + i + ", subId is invalid.");
                    return null;
                }
                MtkPhoneInterfaceManagerEx.getPhone(subId[0]).doGeneralSimAuthentication(iccApplicationChannel, i3, i4, str, str2, messageObtain);
                while (!this.mDone) {
                    try {
                        MtkPhoneInterfaceManagerEx.log("wait for done");
                        wait();
                    } catch (InterruptedException e2) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (this.mResponse != null) {
                    int length = (this.mResponse.payload == null ? 0 : this.mResponse.payload.length) + 2;
                    bArr = new byte[length];
                    if (this.mResponse.payload != null) {
                        System.arraycopy(this.mResponse.payload, 0, bArr, 0, this.mResponse.payload.length);
                    }
                    bArr[length - 1] = (byte) this.mResponse.sw2;
                    bArr[length - 2] = (byte) this.mResponse.sw1;
                } else {
                    MtkPhoneInterfaceManagerEx.log("mResponse is null.");
                }
                MtkPhoneInterfaceManagerEx.log("done");
                return bArr;
            }
        }
    }

    public byte[] simAkaAuthentication(int i, int i2, byte[] bArr, byte[] bArr2) {
        enforcePrivilegedPhoneStatePermission();
        String strSubstring = "";
        String strSubstring2 = "";
        log("simAkaAuthentication session is " + i2 + " simId " + i);
        int i3 = 2;
        if (bArr != null && bArr.length > 0) {
            strSubstring = IccUtils.bytesToHexString(bArr).substring(0, bArr.length * 2);
        }
        if (bArr2 != null && bArr2.length > 0) {
            strSubstring2 = IccUtils.bytesToHexString(bArr2).substring(0, bArr2.length * 2);
        }
        log("simAkaAuthentication Randlen " + strSubstring.length() + " strRand is " + strSubstring + ", AutnLen " + strSubstring2.length() + " strAutn " + strSubstring2);
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toHexString(strSubstring.length()));
        sb.append(strSubstring);
        sb.append(Integer.toHexString(strSubstring2.length()));
        sb.append(strSubstring2);
        String string = sb.toString();
        log("akaData: " + string);
        int subIdBySlot = getSubIdBySlot(i);
        switch (i2) {
            case 1:
                break;
            case 2:
                i3 = 4;
                break;
            case 3:
                i3 = 5;
                break;
            default:
                i3 = 0;
                break;
        }
        if (i3 == 0) {
            return null;
        }
        return IccUtils.hexStringToBytes(TelephonyManager.from(this.mPhone.getContext()).getIccAuthentication(subIdBySlot, i3, 128, string));
    }

    public byte[] simGbaAuthBootStrapMode(int i, int i2, byte[] bArr, byte[] bArr2) {
        enforcePrivilegedPhoneStatePermission();
        if (this.mSimAuthThread == null) {
            log("simGbaAuthBootStrapMode new thread");
            this.mSimAuthThread = new SimAuth(this.mPhone);
            this.mSimAuthThread.start();
        } else {
            log("simGbaAuthBootStrapMode thread has been created.");
        }
        String strSubstring = "";
        String strSubstring2 = "";
        log("simGbaAuthBootStrapMode session is " + i2 + " simId " + i);
        if (bArr != null && bArr.length > 0) {
            strSubstring = IccUtils.bytesToHexString(bArr).substring(0, bArr.length * 2);
        }
        String str = strSubstring;
        if (bArr2 != null && bArr2.length > 0) {
            strSubstring2 = IccUtils.bytesToHexString(bArr2).substring(0, bArr2.length * 2);
        }
        String str2 = strSubstring2;
        log("simGbaAuthBootStrapMode strRand is " + str + " strAutn " + str2);
        return this.mSimAuthThread.doGeneralSimAuth(i, i2, 1, 221, str, str2);
    }

    public byte[] simGbaAuthNafMode(int i, int i2, byte[] bArr, byte[] bArr2) {
        enforcePrivilegedPhoneStatePermission();
        if (this.mSimAuthThread == null) {
            log("simGbaAuthNafMode new thread");
            this.mSimAuthThread = new SimAuth(this.mPhone);
            this.mSimAuthThread.start();
        } else {
            log("simGbaAuthNafMode thread has been created.");
        }
        String strSubstring = "";
        String strSubstring2 = "";
        log("simGbaAuthNafMode session is " + i2 + " simId " + i);
        if (bArr != null && bArr.length > 0) {
            strSubstring = IccUtils.bytesToHexString(bArr).substring(0, bArr.length * 2);
        }
        String str = strSubstring;
        if (UiccController.getInstance().getIccApplicationChannel(i, i2) == 0) {
            log("simGbaAuthNafMode ISIM not support.");
            if (bArr2 != null && bArr2.length > 0) {
                strSubstring2 = IccUtils.bytesToHexString(bArr2).substring(0, bArr2.length * 2);
            }
        }
        String str2 = strSubstring2;
        log("simGbaAuthNafMode NAF ID is " + str + " IMPI " + str2);
        return this.mSimAuthThread.doGeneralSimAuth(i, i2, 1, 222, str, str2);
    }

    public boolean setLteAccessStratumReport(boolean z) {
        int phoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId());
        if (PhoneFactory.getPhone(phoneId) == null) {
            loge("setLteAccessStratumReport: phone[" + phoneId + "] is null");
            return false;
        }
        log("setLteAccessStratumReport: enabled = " + z);
        Boolean bool = (Boolean) sendRequest(35, new Boolean(z), new Integer(phoneId));
        log("setLteAccessStratumReport: success = " + bool);
        return bool.booleanValue();
    }

    public boolean setLteUplinkDataTransfer(boolean z, int i) {
        int i2;
        int phoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId());
        if (PhoneFactory.getPhone(phoneId) == null) {
            loge("setLteUplinkDataTransfer: phone[" + phoneId + "] is null");
            return false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("setLteUplinkDataTransfer: isOn = ");
        sb.append(z);
        sb.append(", Tclose timer = ");
        int i3 = i / TimeConsumingPreferenceActivity.STK_CC_SS_TO_DIAL_VIDEO_ERROR;
        sb.append(i3);
        log(sb.toString());
        if (!z) {
            i2 = (i3 << 16) | 0;
        } else {
            i2 = 1;
        }
        Boolean bool = (Boolean) sendRequest(37, new Integer(i2), new Integer(phoneId));
        log("setLteUplinkDataTransfer: success = " + bool);
        return bool.booleanValue();
    }

    public String getLteAccessStratumState() {
        int phoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId());
        Phone phone = PhoneFactory.getPhone(phoneId);
        String lteAccessStratumState = "unknown";
        if (phone == null) {
            loge("getLteAccessStratumState: phone[" + phoneId + "] is null");
        } else {
            lteAccessStratumState = phone.mDcTracker.getLteAccessStratumState();
        }
        log("getLteAccessStratumState: " + lteAccessStratumState);
        return lteAccessStratumState;
    }

    public boolean isSharedDefaultApn() {
        boolean zIsSharedDefaultApn;
        int phoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId());
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null) {
            loge("isSharedDefaultApn: phone[" + phoneId + "] is null");
            zIsSharedDefaultApn = false;
        } else {
            zIsSharedDefaultApn = phone.mDcTracker.isSharedDefaultApn();
        }
        log("isSharedDefaultApn: " + zIsSharedDefaultApn);
        return zIsSharedDefaultApn;
    }

    public int[] getAdnStorageInfo(int i) {
        Log.d("MtkPhoneIntfMgrEx", "getAdnStorageInfo " + i);
        if (SubscriptionManager.isValidSubscriptionId(i)) {
            synchronized (this.mAdnInfoLock) {
                if (this.mAdnInfoThread == null) {
                    Log.d("MtkPhoneIntfMgrEx", "getAdnStorageInfo new thread !");
                    this.mAdnInfoThread = new QueryAdnInfoThread(i);
                    this.mAdnInfoThread.start();
                } else {
                    this.mAdnInfoThread.setSubId(i);
                    Log.d("MtkPhoneIntfMgrEx", "getAdnStorageInfo old thread !");
                }
            }
            return this.mAdnInfoThread.GetAdnStorageInfo();
        }
        Log.d("MtkPhoneIntfMgrEx", "getAdnStorageInfo subId is invalid.");
        return new int[]{0, 0, 0, 0};
    }

    private static class QueryAdnInfoThread extends Thread {
        private boolean mDone = false;
        private Handler mHandler;
        private int mSubId;
        private int[] recordSize;

        public QueryAdnInfoThread(int i) {
            this.mSubId = i;
        }

        public void setSubId(int i) {
            synchronized (this) {
                this.mSubId = i;
                this.mDone = false;
            }
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (this) {
                this.mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        if (message.what == 100) {
                            Log.d("MtkPhoneIntfMgrEx", "EVENT_QUERY_PHB_ADN_INFO");
                            synchronized (QueryAdnInfoThread.this) {
                                QueryAdnInfoThread.this.mDone = true;
                                int[] iArr = (int[]) asyncResult.result;
                                if (iArr == null || iArr.length != 4) {
                                    QueryAdnInfoThread.this.recordSize = new int[4];
                                    QueryAdnInfoThread.this.recordSize[0] = 0;
                                    QueryAdnInfoThread.this.recordSize[1] = 0;
                                    QueryAdnInfoThread.this.recordSize[2] = 0;
                                    QueryAdnInfoThread.this.recordSize[3] = 0;
                                } else {
                                    QueryAdnInfoThread.this.recordSize = new int[4];
                                    QueryAdnInfoThread.this.recordSize[0] = iArr[0];
                                    QueryAdnInfoThread.this.recordSize[1] = iArr[1];
                                    QueryAdnInfoThread.this.recordSize[2] = iArr[2];
                                    QueryAdnInfoThread.this.recordSize[3] = iArr[3];
                                    Log.d("MtkPhoneIntfMgrEx", "recordSize[0]=" + QueryAdnInfoThread.this.recordSize[0] + ",recordSize[1]=" + QueryAdnInfoThread.this.recordSize[1] + ",recordSize[2]=" + QueryAdnInfoThread.this.recordSize[2] + ",recordSize[3]=" + QueryAdnInfoThread.this.recordSize[3]);
                                }
                                QueryAdnInfoThread.this.notifyAll();
                            }
                        }
                    }
                };
                notifyAll();
            }
            Looper.loop();
        }

        public int[] GetAdnStorageInfo() {
            int[] iArr;
            synchronized (this) {
                while (this.mHandler == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                MtkPhoneInterfaceManagerEx.getPhone(this.mSubId).queryPhbStorageInfo(0, Message.obtain(this.mHandler, 100));
                while (!this.mDone) {
                    try {
                        Log.d("MtkPhoneIntfMgrEx", "wait for done");
                        wait();
                    } catch (InterruptedException e2) {
                        Thread.currentThread().interrupt();
                    }
                }
                Log.d("MtkPhoneIntfMgrEx", "done");
                iArr = this.recordSize;
            }
            return iArr;
        }
    }

    public boolean isPhbReady(int i) {
        Phone phone;
        MtkIccPhoneBookInterfaceManager iccPhoneBookInterfaceManager;
        int phoneId = SubscriptionManager.getPhoneId(i);
        if (SubscriptionManager.isValidSlotIndex(SubscriptionManager.getSlotIndex(i)) && (phone = PhoneFactory.getPhone(phoneId)) != null && (iccPhoneBookInterfaceManager = phone.getIccPhoneBookInterfaceManager()) != null && (iccPhoneBookInterfaceManager instanceof MtkIccPhoneBookInterfaceManager)) {
            return iccPhoneBookInterfaceManager.isPhbReady();
        }
        return false;
    }

    private class RxTestObject {
        Object lockObj;
        int[] result;

        private RxTestObject() {
            this.result = null;
            this.lockObj = new Object();
        }
    }

    public int[] setRxTestConfig(int i, int i2) {
        MtkGsmCdmaPhone phone = PhoneFactory.getPhone(i);
        if (phone != null) {
            if (phone.mMtkCi != null) {
                RxTestObject rxTestObject = new RxTestObject();
                synchronized (rxTestObject.lockObj) {
                    phone.setRxTestConfig(i2, this.mMainThreadHandler.obtainMessage(42, rxTestObject));
                    try {
                        rxTestObject.lockObj.wait(5000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                synchronized (rxTestObject.lockObj) {
                    if (rxTestObject.result != null) {
                        log("setRxTestConfig return: " + rxTestObject.result);
                        return rxTestObject.result;
                    }
                    log("setRxTestConfig return: null");
                    return null;
                }
            }
            log("setRxTestConfig phone.mMtkCi = null");
        } else {
            log("setRxTestConfig phone = null");
        }
        return null;
    }

    public int[] getRxTestResult(int i) {
        MtkGsmCdmaPhone phone = PhoneFactory.getPhone(i);
        if (phone != null) {
            if (phone.mMtkCi != null) {
                RxTestObject rxTestObject = new RxTestObject();
                synchronized (rxTestObject.lockObj) {
                    phone.getRxTestResult(this.mMainThreadHandler.obtainMessage(43, rxTestObject));
                    try {
                        rxTestObject.lockObj.wait(5000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                synchronized (rxTestObject.lockObj) {
                    if (rxTestObject.result != null) {
                        log("getRxTestResult return: " + rxTestObject.result);
                        return rxTestObject.result;
                    }
                    log("getRxTestResult return: null");
                    return null;
                }
            }
            log("getRxTestResult mMtkCi.mCi = null");
        } else {
            log("getRxTestResult phone = null");
        }
        return null;
    }

    public int selfActivationAction(int i, Bundle bundle, int i2) {
        int iSelfActivationAction;
        ISelfActivation selfActivationInstance = getPhone(i2).getSelfActivationInstance();
        if (selfActivationInstance != null) {
            iSelfActivationAction = selfActivationInstance.selfActivationAction(i, bundle);
        } else {
            log("null SelfActivation instance");
            iSelfActivationAction = 0;
        }
        log("selfActivationAction: action = " + i + " subId = " + i2 + " retVal = " + iSelfActivationAction);
        return iSelfActivationAction;
    }

    public int getSelfActivateState(int i) {
        int selfActivateState;
        ISelfActivation selfActivationInstance = getPhone(i).getSelfActivationInstance();
        if (selfActivationInstance != null) {
            selfActivateState = selfActivationInstance.getSelfActivateState();
        } else {
            log("null SelfActivation instance");
            selfActivateState = 0;
        }
        log("getSelfActivateState: subId = " + i + " retVal = " + selfActivateState);
        return selfActivateState;
    }

    public int getPCO520State(int i) {
        int pCO520State;
        ISelfActivation selfActivationInstance = getPhone(i).getSelfActivationInstance();
        if (selfActivationInstance != null) {
            pCO520State = selfActivationInstance.getPCO520State();
        } else {
            log("null SelfActivation instance");
            pCO520State = 0;
        }
        log("getPCO520State: subId = " + i + " retVal = " + pCO520State);
        return pCO520State;
    }

    public void setEccInProgress(boolean z) {
        this.mIsEccInProgress = z;
        log("setEccInProgress, mIsEccInProgress:" + this.mIsEccInProgress);
    }

    public boolean isEccInProgress() {
        log("isEccInProgress, mIsEccInProgress:" + this.mIsEccInProgress);
        return this.mIsEccInProgress;
    }

    public boolean exitEmergencyCallbackMode(int i) {
        log("exitEmergencyCallbackMode, subId: " + i);
        if (SubscriptionManager.getPhoneId(i) == -1) {
            log("no corresponding phone id");
            return false;
        }
        EcmExitResult ecmExitResult = (EcmExitResult) sendRequest(44, Integer.valueOf(i));
        if (ecmExitResult.getReceiver() != null) {
            log("unregisterReceiver " + ecmExitResult.getReceiver());
            this.mApp.unregisterReceiver(ecmExitResult.getReceiver());
            return true;
        }
        return true;
    }

    public void setApcModeUsingSlotId(int i, int i2, boolean z, int i3) {
        log("setApcModeUsingSlotId, slotId:" + i + ", mode:" + i2 + ", reportOn:" + z + ", reportInterval:" + i3);
        MtkGsmCdmaPhone phone = getPhone(getSubIdBySlot(i));
        if (phone != null) {
            if (phone.getPhoneType() == 1) {
                phone.setApcMode(i2, z, i3);
                return;
            } else {
                log("setApcModeUsingSlotId: phone type is abnormal");
                return;
            }
        }
        log("setApcModeUsingSlotId, phone or subId: is null");
    }

    public PseudoCellInfo getApcInfoUsingSlotId(int i) {
        log("getApcInfoUsingSlotId, slotId:" + i);
        MtkGsmCdmaPhone phone = getPhone(getSubIdBySlot(i));
        if (phone != null) {
            if (phone.getPhoneType() == 1) {
                return phone.getApcInfo();
            }
            log("getApcInfoUsingSlotId: phone type is abnormal");
        } else {
            log("getApcInfoUsingSlotId, phone or subId: is null");
        }
        return null;
    }

    public int getCdmaSubscriptionActStatus(int i) {
        MtkGsmCdmaPhone phone = getPhone(i);
        if (phone != null) {
            log("getCdmaSubscriptionActStatus, phone type " + phone.getPhoneType());
            if (phone.getPhoneType() == 2) {
                return phone.getCdmaSubscriptionActStatus();
            }
        } else {
            log("fail to getCdmaSubscriptionActStatus due to phone is null");
        }
        return 0;
    }

    public void setIsLastEccIms(boolean z) {
        log("setIsLastEccIms(): " + z);
        this.mIsLastEccIms = z;
    }

    public boolean getIsLastEccIms() {
        log("getIsLastEccIms(): " + this.mIsLastEccIms);
        return this.mIsLastEccIms;
    }

    public int invokeOemRilRequestRaw(byte[] bArr, byte[] bArr2) {
        enforceModifyPermission();
        try {
            AsyncResult asyncResult = (AsyncResult) sendRequest(112, bArr);
            if (asyncResult.exception == null) {
                if (asyncResult.result == null) {
                    return 0;
                }
                byte[] bArr3 = (byte[]) asyncResult.result;
                if (bArr3.length > bArr2.length) {
                    Log.w("MtkPhoneIntfMgrEx", "Buffer to copy response too small: Response length is " + bArr3.length + "bytes. Buffer Size is " + bArr2.length + "bytes.");
                }
                System.arraycopy(bArr3, 0, bArr2, 0, bArr3.length);
                return bArr3.length;
            }
            int iOrdinal = asyncResult.exception.getCommandError().ordinal();
            return iOrdinal > 0 ? iOrdinal * (-1) : iOrdinal;
        } catch (RuntimeException e) {
            Log.w("MtkPhoneIntfMgrEx", "sendOemRilRequestRaw: Runtime Exception");
            int iOrdinal2 = CommandException.Error.GENERIC_FAILURE.ordinal();
            return iOrdinal2 > 0 ? iOrdinal2 * (-1) : iOrdinal2;
        }
    }

    public int invokeOemRilRequestRawBySlot(int i, byte[] bArr, byte[] bArr2) {
        enforceModifyPermission();
        try {
            AsyncResult asyncResult = (AsyncResult) sendRequest(112, bArr, Integer.valueOf(getSubIdBySlot(i)));
            if (asyncResult.exception == null) {
                if (asyncResult.result == null) {
                    return 0;
                }
                byte[] bArr3 = (byte[]) asyncResult.result;
                if (bArr3.length > bArr2.length) {
                    Log.w("MtkPhoneIntfMgrEx", "Buffer to copy response too small: Response length is " + bArr3.length + "bytes. Buffer Size is " + bArr2.length + "bytes.");
                }
                System.arraycopy(bArr3, 0, bArr2, 0, bArr3.length);
                return bArr3.length;
            }
            int iOrdinal = asyncResult.exception.getCommandError().ordinal();
            return iOrdinal > 0 ? iOrdinal * (-1) : iOrdinal;
        } catch (RuntimeException e) {
            Log.w("MtkPhoneIntfMgrEx", "sendOemRilRequestRaw: Runtime Exception");
            int iOrdinal2 = CommandException.Error.GENERIC_FAILURE.ordinal();
            return iOrdinal2 > 0 ? iOrdinal2 * (-1) : iOrdinal2;
        }
    }

    private void enforceModifyPermission() {
        this.mApp.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
    }

    public boolean isInCsCall(int i) {
        log("[isInCsCall] phoneId:" + i);
        Phone phone = PhoneFactory.getPhone(i);
        return (phone == null || phone.getCallTracker() == null || phone.getCallTracker().getState() == PhoneConstants.State.IDLE) ? false : true;
    }

    public List<CellInfo> getAllCellInfo(int i, String str) {
        Phone phone = PhoneFactory.getPhone(i);
        if (phone == null) {
            return null;
        }
        ((AppOpsManager) phone.getContext().getSystemService(AppOpsManager.class)).checkPackage(Binder.getCallingUid(), str);
        if (!LocationAccessPolicy.canAccessCellLocation(phone.getContext(), str, Binder.getCallingUid(), Binder.getCallingPid(), true)) {
            return null;
        }
        return phone.getAllCellInfo(getWorkSource(Binder.getCallingUid(), phone.getContext()));
    }

    private final WorkSource getWorkSource(int i, Context context) {
        if (context == null) {
            return null;
        }
        return new WorkSource(i, context.getPackageManager().getNameForUid(i));
    }

    public String getLocatedPlmn(int i) {
        MtkGsmCdmaPhone phone = PhoneFactory.getPhone(i);
        if (phone != null) {
            return phone.getLocatedPlmn();
        }
        return null;
    }
}
