package com.mediatek.internal.telephony.dataconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneSwitcher;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccRecords;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.List;

public class DataConnectionHelper extends Handler {
    private static final String DATA_CONFIG = "ro.mtk_data_config";
    private static final boolean DBG = true;
    private static final int EVENT_ID_INTVL = 10;
    private static final int EVENT_NOTIFICATION_RC_CHANGED = 20;
    private static final int EVENT_VOICE_CALL_ENDED = 10;
    private static final int EVENT_VOICE_CALL_STARTED = 0;
    private static final String INVALID_ICCID = "N/A";
    private static final String LOG_TAG = "DcHelper";
    private static final int MAX_ACTIVE_PHONES_DUAL = 2;
    private static final int MAX_ACTIVE_PHONES_SINGLE = 1;
    private static final boolean MTK_SRLTE_SUPPORT;
    public static final boolean MTK_SVLTE_SUPPORT;
    private static final String OPERATOR_OP09 = "OP09";
    private static String[] PROPERTY_ICCID_SIM = null;
    private static final String[] PROPERTY_RIL_CT3G;
    static final String PROPERTY_RIL_DATA_ICCID = "persist.radio.data.iccid";
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE;
    private static final String[] PROPERTY_RIL_TEST_SIM;
    private static final String PROP_MTK_CDMA_LTE_MODE = "ro.boot.opt_c2k_lte_mode";
    private static final String SEGDEFAULT = "SEGDEFAULT";
    private static final boolean VDBG;
    private static DataConnectionHelper sDataConnectionHelper;
    private Context mContext;
    private int mPhoneNum;
    private PhoneSwitcher mPhoneSwitcher;
    private Phone[] mPhones;
    private int mCallingPhone = -1;
    private int[][] MTK_SBP_TABLE = {new int[]{44007, 44008, 129}, new int[]{44050, 44056, 129}, new int[]{44070, 44079, 129}, new int[]{44088, 44089, 129}, new int[]{44170, 44170, 129}};
    private Handler mRspHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int phoneId = msg.what % 10;
            int eventId = msg.what - phoneId;
            switch (eventId) {
                case 0:
                    DataConnectionHelper.logd("EVENT_PHONE" + phoneId + "_VOICE_CALL_STARTED.");
                    DataConnectionHelper.this.mCallingPhone = phoneId;
                    DataConnectionHelper.logd("mCallingPhone = " + DataConnectionHelper.this.mCallingPhone);
                    DataConnectionHelper.this.onVoiceCallStarted();
                    break;
                case 10:
                    DataConnectionHelper.logd("EVENT_PHONE" + phoneId + "_VOICE_CALL_ENDED.");
                    DataConnectionHelper.logd("mCallingPhone = " + DataConnectionHelper.this.mCallingPhone);
                    DataConnectionHelper.this.onVoiceCallEnded();
                    DataConnectionHelper.this.mCallingPhone = -1;
                    break;
                case 20:
                    DataConnectionHelper.logd("EVENT_PHONE" + phoneId + "_EVENT_NOTIFICATION_RC_CHANGED.");
                    DataConnectionHelper.this.onCheckIfRetriggerDataAllowed(phoneId);
                    break;
                default:
                    DataConnectionHelper.logd("Unhandled message with number: " + msg.what);
                    break;
            }
        }
    };
    private final BroadcastReceiver mModeStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                DataConnectionHelper.logd("mModeStateReceiver: Action is null");
            } else {
                if (!"android.intent.action.RADIO_TECHNOLOGY".equals(action)) {
                    return;
                }
                int defDataPhoneId = SubscriptionManager.getPhoneId(SubscriptionController.getInstance().getDefaultDataSubId());
                DataConnectionHelper.logd("mModeStateReceiver: update DSDS/DSDA mode, action=" + action + " defDataPhoneId = " + defDataPhoneId);
                DataConnectionHelper.this.updateMaxActivePhoneSvlte(defDataPhoneId);
                DataConnectionHelper.this.mPhoneSwitcher.onModeChanged();
            }
        }
    };

    static {
        VDBG = SystemProperties.get("ro.build.type").equals("eng");
        MTK_SVLTE_SUPPORT = SystemProperties.getInt(PROP_MTK_CDMA_LTE_MODE, 0) == 1;
        MTK_SRLTE_SUPPORT = SystemProperties.getInt(PROP_MTK_CDMA_LTE_MODE, 0) == 2;
        PROPERTY_ICCID_SIM = new String[]{"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
        PROPERTY_RIL_TEST_SIM = new String[]{"gsm.sim.ril.testsim", "gsm.sim.ril.testsim.2", "gsm.sim.ril.testsim.3", "gsm.sim.ril.testsim.4"};
        PROPERTY_RIL_FULL_UICC_TYPE = new String[]{"gsm.ril.fulluicctype", "gsm.ril.fulluicctype.2", "gsm.ril.fulluicctype.3", "gsm.ril.fulluicctype.4"};
        PROPERTY_RIL_CT3G = new String[]{"gsm.ril.ct3g", "gsm.ril.ct3g.2", "gsm.ril.ct3g.3", "gsm.ril.ct3g.4"};
    }

    private DataConnectionHelper(Context context, Phone[] phones, PhoneSwitcher phoneSwitcher) {
        this.mContext = context;
        this.mPhoneSwitcher = phoneSwitcher;
        this.mPhones = phones;
        this.mPhoneNum = phones.length;
        registerEvents();
    }

    public void dispose() {
        logd("DataConnectionHelper.dispose");
        unregisterEvents();
    }

    public static DataConnectionHelper makeDataConnectionHelper(Context context, Phone[] phones, PhoneSwitcher phoneSwitcher) {
        if (context == null || phones == null || phoneSwitcher == null) {
            throw new RuntimeException("param is null");
        }
        if (sDataConnectionHelper == null) {
            logd("makeDataConnectionHelper: phones.length=" + phones.length);
            sDataConnectionHelper = new DataConnectionHelper(context, phones, phoneSwitcher);
        }
        logd("makesDataConnectionHelper: X sDataConnectionHelper =" + sDataConnectionHelper);
        return sDataConnectionHelper;
    }

    public static DataConnectionHelper getInstance() {
        if (sDataConnectionHelper == null) {
            throw new RuntimeException("Should not be called before makesDataConnectionHelper");
        }
        return sDataConnectionHelper;
    }

    public static boolean isMultiPsAttachSupport() {
        int config = SystemProperties.getInt(DATA_CONFIG, 0);
        if (config != 1) {
            return false;
        }
        return true;
    }

    public void reRegisterPsNetwork() {
        this.mPhoneSwitcher.reRegisterPsNetwork();
    }

    private void registerEvents() {
        logd("registerEvents");
        for (int i = 0; i < this.mPhoneNum; i++) {
            this.mPhones[i].getCallTracker().registerForVoiceCallStarted(this.mRspHandler, i + 0, null);
            this.mPhones[i].getCallTracker().registerForVoiceCallEnded(this.mRspHandler, i + 10, null);
            if (this.mPhones[i].getImsPhone() != null) {
                this.mPhones[i].getImsPhone().getCallTracker().registerForVoiceCallStarted(this.mRspHandler, i + 0, null);
                this.mPhones[i].getImsPhone().getCallTracker().registerForVoiceCallEnded(this.mRspHandler, i + 10, null);
            } else {
                logd("Not register IMS phone calling state yet.");
            }
            this.mPhones[i].registerForRadioCapabilityChanged(this.mRspHandler, i + 20, null);
        }
        if (!MTK_SVLTE_SUPPORT) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.RADIO_TECHNOLOGY");
        this.mContext.registerReceiver(this.mModeStateReceiver, filter);
        logd("registered mModeStateReceiver.");
    }

    private void unregisterEvents() {
        logd("unregisterEvents");
        for (int i = 0; i < this.mPhoneNum; i++) {
            this.mPhones[i].getCallTracker().unregisterForVoiceCallStarted(this.mRspHandler);
            this.mPhones[i].getCallTracker().unregisterForVoiceCallEnded(this.mRspHandler);
            if (this.mPhones[i].getImsPhone() != null) {
                this.mPhones[i].getImsPhone().getCallTracker().unregisterForVoiceCallStarted(this.mRspHandler);
                this.mPhones[i].getImsPhone().getCallTracker().unregisterForVoiceCallEnded(this.mRspHandler);
            } else {
                logd("Not unregister IMS phone calling state yet.");
            }
            this.mPhones[i].unregisterForRadioCapabilityChanged(this.mRspHandler);
        }
        if (!MTK_SVLTE_SUPPORT) {
            return;
        }
        this.mContext.unregisterReceiver(this.mModeStateReceiver);
        logd("unregistered mModeStateReceiver.");
    }

    private void onVoiceCallStarted() {
        for (int i = 0; i < this.mPhoneNum; i++) {
            logd("onVoiceCallStarted: mPhone[ " + i + "]");
            this.mPhones[i].mDcTracker.onVoiceCallStarted();
        }
    }

    private void onVoiceCallEnded() {
        for (int i = 0; i < this.mPhoneNum; i++) {
            logd("onVoiceCallEnded: mPhone[ " + i + "]");
            this.mPhones[i].mDcTracker.onVoiceCallEnded();
        }
    }

    public boolean isDataSupportConcurrent(int phoneId) {
        if (this.mCallingPhone == -1) {
            logd("isDataSupportConcurrent: invalid calling phone!");
            return false;
        }
        if (phoneId == this.mCallingPhone) {
            boolean isConcurrent = this.mPhones[phoneId].getServiceStateTracker().isConcurrentVoiceAndDataAllowed();
            logd("isDataSupportConcurrent:(PS&CS on the same phone) isConcurrent= " + isConcurrent + "phoneId= " + phoneId + " mCallingPhone = " + this.mCallingPhone);
            return isConcurrent;
        }
        if (MTK_SRLTE_SUPPORT) {
            logd("isDataSupportConcurrent: support SRLTE ");
            return false;
        }
        if (!MTK_SVLTE_SUPPORT) {
            logd("isDataSupportConcurrent: not SRLTE or SVLTE ");
            return false;
        }
        int phoneType = this.mPhones[this.mCallingPhone].getPhoneType();
        if (phoneType == 2) {
            return true;
        }
        int rilRat = this.mPhones[phoneId].getServiceState().getRilDataRadioTechnology();
        logd("isDataSupportConcurrent: support SVLTE RilRat = " + rilRat + "calling phoneType: " + phoneType);
        return ServiceState.isCdma(rilRat);
    }

    public boolean isAllCallingStateIdle() {
        PhoneConstants.State[] state = new PhoneConstants.State[this.mPhoneNum];
        boolean allCallingState = false;
        for (int i = 0; i < this.mPhoneNum; i++) {
            state[i] = this.mPhones[i].getCallTracker().getState();
            if (state[i] != null && state[i] == PhoneConstants.State.IDLE) {
                allCallingState = true;
            } else {
                allCallingState = false;
                break;
            }
        }
        if (!allCallingState && VDBG) {
            for (int i2 = 0; i2 < this.mPhoneNum; i2++) {
                logd("isAllCallingStateIdle: state[" + i2 + "]=" + state[i2] + " allCallingState = " + allCallingState);
            }
        }
        return allCallingState;
    }

    private boolean isCdmaCard(int phoneId) {
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            logd("isCdmaCard invalid phoneId:" + phoneId);
            return false;
        }
        String cardType = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[phoneId]);
        boolean isCdmaSim = cardType.indexOf("CSIM") >= 0 || cardType.indexOf("RUIM") >= 0;
        if (!isCdmaSim && "SIM".equals(cardType)) {
            String uimDualMode = SystemProperties.get(PROPERTY_RIL_CT3G[phoneId]);
            if ("1".equals(uimDualMode)) {
                return true;
            }
            return isCdmaSim;
        }
        return isCdmaSim;
    }

    public boolean isSimInserted(int phoneId) {
        logd("isSimInserted:phoneId =" + phoneId);
        String iccid = SystemProperties.get(PROPERTY_ICCID_SIM[phoneId], UsimPBMemInfo.STRING_NOT_SET);
        return (TextUtils.isEmpty(iccid) || INVALID_ICCID.equals(iccid)) ? false : true;
    }

    public boolean isTestIccCard(int phoneId) {
        String testCard = SystemProperties.get(PROPERTY_RIL_TEST_SIM[phoneId], UsimPBMemInfo.STRING_NOT_SET);
        if (VDBG) {
            logd("isTestIccCard: phoneId id = " + phoneId + ", iccType = " + testCard);
        }
        if (testCard != null) {
            return testCard.equals("1");
        }
        return false;
    }

    private void onCheckIfRetriggerDataAllowed(int phoneId) {
        int defDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
        int defDataPhoneId = SubscriptionManager.getPhoneId(defDataSubId);
        int mainCapPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        logd("onCheckIfRetriggerDataAllowed: defDataSubId = " + defDataSubId + "defDataPhoneId =" + defDataPhoneId + "mainCapPhoneId = " + mainCapPhoneId + "mPhoneNum =" + this.mPhoneNum);
        if ((defDataPhoneId >= this.mPhoneNum || defDataPhoneId < 0 || defDataPhoneId == mainCapPhoneId) && !isMultiPsAttachSupport()) {
            logd("onCheckIfRetriggerDataAllowed: phoneId out of boundary :" + defDataPhoneId);
        } else {
            logd("onCheckIfRetriggerDataAllowed: retriggerDataAllowed: mPhone[" + phoneId + "]");
            this.mPhoneSwitcher.resendDataAllowed(phoneId);
        }
    }

    public void updateMaxActivePhoneSvlte(int defDataPhoneId) {
        int cdmaSlot = -1;
        int simCount = 0;
        int cSimCount = 0;
        int mainSlot = defDataPhoneId;
        for (int i = 0; i < this.mPhones.length; i++) {
            if (isSimInserted(i)) {
                simCount++;
            }
            if (isCdmaCard(i)) {
                cSimCount++;
            }
            if (this.mPhones[i].getPhoneType() == 2) {
                cdmaSlot = i;
            }
        }
        int oldActive = this.mPhoneSwitcher.getMaxActivePhonesCount();
        if (!SubscriptionManager.isValidPhoneId(defDataPhoneId)) {
            logd("updateMaxActivePhoneSvlte: default sub not in device, use 4G slot.");
            mainSlot = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        }
        int newActive = (isOP09ASupport() || !SubscriptionManager.isValidPhoneId(cdmaSlot) || !SubscriptionManager.isValidPhoneId(mainSlot) || simCount != 2 || cSimCount == 2 || cdmaSlot == mainSlot) ? 1 : 2;
        logd("updateMaxActivePhoneSvlte: cdma slot:" + cdmaSlot + ", main slot:" + mainSlot + ", oldActive:" + oldActive + ", now:" + newActive + ", cSimCount:" + cSimCount);
        if (newActive != oldActive) {
            this.mPhoneSwitcher.setMaxActivePhones(newActive);
        }
    }

    public void updateActivePhonesSvlte(List<Integer> phones) {
        int count = this.mPhoneSwitcher.getMaxActivePhonesCount();
        if (count == 2) {
            phones.clear();
            for (int i = 0; i < this.mPhones.length; i++) {
                phones.add(Integer.valueOf(i));
            }
            return;
        }
        if (count != 1 || !phones.isEmpty()) {
            return;
        }
        int cdmaPhone = -1;
        int simCount = 0;
        for (int i2 = 0; i2 < this.mPhones.length; i2++) {
            if (isSimInserted(i2)) {
                simCount++;
                if (this.mPhones[i2].getPhoneType() == 2) {
                    cdmaPhone = i2;
                }
            }
        }
        if (simCount != 1 || cdmaPhone < 0) {
            return;
        }
        logd("updateActivePhonesSvlte: add CDMA phone as active phone.");
        phones.add(Integer.valueOf(cdmaPhone));
    }

    public static void updateDefaultDataIccid(int dataSub) {
        SubscriptionInfo subInfo;
        int dataPhoneId = SubscriptionManager.getPhoneId(dataSub);
        String defaultIccid = UsimPBMemInfo.STRING_NOT_SET;
        if (dataPhoneId >= 0 && dataPhoneId < PROPERTY_ICCID_SIM.length) {
            defaultIccid = SystemProperties.get(PROPERTY_ICCID_SIM[dataPhoneId]);
            logd("updateDefaultDataIccid, Iccid = " + SubscriptionInfo.givePrintableIccid(defaultIccid) + ", dataPhoneId:" + dataPhoneId);
            if ((defaultIccid.equals(UsimPBMemInfo.STRING_NOT_SET) || defaultIccid.equals(INVALID_ICCID)) && (subInfo = SubscriptionController.getInstance().getSubscriptionInfo(dataSub)) != null) {
                defaultIccid = subInfo.getIccId();
            }
        } else {
            logd("updateDefaultDataIccid, invalid dataPhoneId:" + dataPhoneId);
        }
        logd("updateDefaultDataIccid: Set " + SubscriptionInfo.givePrintableIccid(defaultIccid));
        SystemProperties.set(PROPERTY_RIL_DATA_ICCID, defaultIccid);
    }

    private static void logd(String s) {
        Rlog.d(LOG_TAG, s);
    }

    public int getSbpIdFromNetworkOperator(int PhoneId) {
        return getSbpId(TelephonyManager.getDefault().getNetworkOperatorForPhone(PhoneId));
    }

    public int getSbpIdFromSimOperator(IccRecords r) {
        return getSbpId(r != null ? r.getOperatorNumeric() : UsimPBMemInfo.STRING_NOT_SET);
    }

    private int getSbpId(String strMccMnc) {
        try {
            if (TextUtils.isEmpty(strMccMnc)) {
                return -1;
            }
            int mccmnc = Integer.parseInt(strMccMnc);
            for (int[] sbpEntry : this.MTK_SBP_TABLE) {
                if (mccmnc >= sbpEntry[0] && mccmnc <= sbpEntry[1]) {
                    int sbpId = sbpEntry[2];
                    return sbpId;
                }
            }
            return -1;
        } catch (NumberFormatException e) {
            logd("getSbpId: e=" + e);
            return -1;
        }
    }

    private boolean isOP09ASupport() {
        if (OPERATOR_OP09.equals(SystemProperties.get("persist.operator.optr", UsimPBMemInfo.STRING_NOT_SET))) {
            return SEGDEFAULT.equals(SystemProperties.get("persist.operator.seg", UsimPBMemInfo.STRING_NOT_SET));
        }
        return false;
    }
}
