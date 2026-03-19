package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.MtkSuppServContants;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MtkSuppServConf {
    private static final int CARRIER_CONFIG_LOADING_TIME = 5000;
    private static final int EVENT_CARRIER_CONFIG_LOADED = 1;
    private static final int EVENT_CARRIER_CONFIG_LOADED_TIMEOUT = 2;
    private static final int EVENT_ICC_CHANGED = 4;
    private static final int EVENT_INIT = 0;
    private static final int EVENT_RECORDS_LOADED = 3;
    private static final String LOG_TAG = "SuppServConf";
    private Context mContext;
    private MtkGsmCdmaPhone mPhone;
    private SSConfigHandler mSSConfigHandler = null;
    private UiccController mUiccController = null;
    private final AtomicReference<IccRecords> mIccRecords = new AtomicReference<>();
    private HashMap<MtkSuppServContants.CUSTOMIZATION_ITEM, SSConfig> mCustomizationMap = new HashMap<>();
    private int OPERATORUTILS_BOOL_TRUE = 1;
    private int OPERATORUTILS_BOOL_FALSE = 2;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MtkSuppServConf.this.logd("mBroadcastReceiver: action " + intent.getAction());
            if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                int intExtra = intent.getIntExtra("subscription", -1);
                MtkSuppServConf.this.logi("Receive ACTION_CARRIER_CONFIG_CHANGED: subId=" + intExtra + ", mPhone.getSubId()=" + MtkSuppServConf.this.mPhone.getSubId());
                if (intExtra == MtkSuppServConf.this.mPhone.getSubId()) {
                    MtkSuppServConf.this.logi("CarrierConfigLoader is loading complete!");
                    MtkSuppServConf.this.mSSConfigHandler.removeMessages(2);
                    MtkSuppServConf.this.mSSConfigHandler.obtainMessage(1).sendToTarget();
                }
            }
        }
    };

    static class AnonymousClass2 {
        static final int[] $SwitchMap$com$mediatek$internal$telephony$MtkSuppServContants$CUSTOMIZATION_ITEM = new int[MtkSuppServContants.CUSTOMIZATION_ITEM.values().length];
    }

    private class SSConfig {
        public static final int DONE = 1;
        public static final int UNSET = 0;
        public boolean bDefault;
        public boolean bValue;
        public int iDefault;
        public int iValue;
        public String mCarrierConfigKey;
        private int mStatus;
        public int mSystemPropIdx;
        public String sDefault;
        public String sValue;

        public SSConfig(String str, int i) {
            this.mCarrierConfigKey = "";
            this.mSystemPropIdx = -1;
            this.bValue = false;
            this.bDefault = false;
            this.sValue = "";
            this.sDefault = "";
            this.iValue = -1;
            this.iDefault = -1;
            this.mStatus = 0;
            this.mCarrierConfigKey = str;
            this.mSystemPropIdx = i;
        }

        public SSConfig(String str, int i, boolean z) {
            this.mCarrierConfigKey = "";
            this.mSystemPropIdx = -1;
            this.bValue = false;
            this.bDefault = false;
            this.sValue = "";
            this.sDefault = "";
            this.iValue = -1;
            this.iDefault = -1;
            this.mStatus = 0;
            this.mCarrierConfigKey = str;
            this.mSystemPropIdx = i;
            this.bDefault = z;
            this.bValue = this.bDefault;
        }

        public SSConfig(String str, int i, String str2) {
            this.mCarrierConfigKey = "";
            this.mSystemPropIdx = -1;
            this.bValue = false;
            this.bDefault = false;
            this.sValue = "";
            this.sDefault = "";
            this.iValue = -1;
            this.iDefault = -1;
            this.mStatus = 0;
            this.mCarrierConfigKey = str;
            this.mSystemPropIdx = i;
            this.sDefault = str2;
            this.sValue = this.sDefault;
        }

        public SSConfig(String str, int i, int i2) {
            this.mCarrierConfigKey = "";
            this.mSystemPropIdx = -1;
            this.bValue = false;
            this.bDefault = false;
            this.sValue = "";
            this.sDefault = "";
            this.iValue = -1;
            this.iDefault = -1;
            this.mStatus = 0;
            this.mCarrierConfigKey = str;
            this.mSystemPropIdx = i;
            this.iDefault = i2;
            this.iValue = this.iDefault;
        }

        public void setValue(boolean z) {
            this.bValue = z;
            this.mStatus = 1;
        }

        public void setValue(String str) {
            this.sValue = str;
            this.mStatus = 1;
        }

        public void setValue(int i) {
            this.iValue = i;
            this.mStatus = 1;
        }

        public void reset() {
            this.bValue = this.bDefault;
            this.sValue = this.sDefault;
            this.iValue = this.iDefault;
            this.mStatus = 0;
        }

        public String toString() {
            return "bValue: " + this.bValue + ", sValue: " + this.sValue + ", iValue: " + this.iValue;
        }
    }

    public MtkSuppServConf(Context context, Phone phone) {
        this.mContext = null;
        this.mPhone = null;
        this.mContext = context;
        this.mPhone = (MtkGsmCdmaPhone) phone;
        logi("MtkSuppServConf constructor.");
    }

    public void init(Looper looper) {
        logi("MtkSuppServConf init.");
        this.mSSConfigHandler = new SSConfigHandler(looper);
        initConfig();
        registerCarrierConfigIntent();
        registerEvent();
        this.mSSConfigHandler.obtainMessage(0).sendToTarget();
    }

    private void registerCarrierConfigIntent() {
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
    }

    private void unregisterCarrierConfigIntent() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }

    private void registerEvent() {
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this.mSSConfigHandler, 4, (Object) null);
    }

    private void unregisterEvent() {
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.unregisterForIccChanged(this.mSSConfigHandler);
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null) {
            iccRecords.unregisterForRecordsLoaded(this.mSSConfigHandler);
            this.mIccRecords.set(null);
        }
    }

    public void dispose() {
        unregisterCarrierConfigIntent();
        unregisterEvent();
    }

    private void onUpdateIcc() {
        if (this.mUiccController == null) {
            return;
        }
        IccRecords uiccRecords = getUiccRecords(1);
        if (uiccRecords == null && this.mPhone.getPhoneType() == 2) {
            uiccRecords = getUiccRecords(2);
        }
        IccRecords iccRecords = this.mIccRecords.get();
        if (uiccRecords != null && iccRecords != null) {
            logd("onUpdateIcc: newIccRecords=" + uiccRecords + ", r=" + iccRecords);
        }
        if (iccRecords != uiccRecords) {
            if (iccRecords != null) {
                logi("Removing stale icc objects.");
                iccRecords.unregisterForRecordsLoaded(this.mSSConfigHandler);
                this.mIccRecords.set(null);
            }
            if (uiccRecords != null) {
                if (SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
                    logi("New records found.");
                    this.mIccRecords.set(uiccRecords);
                    uiccRecords.registerForRecordsLoaded(this.mSSConfigHandler, 3, (Object) null);
                    return;
                }
                return;
            }
            onSimNotReady();
        }
    }

    private void onSimNotReady() {
        logd("onSimNotReady");
        resetConfig();
    }

    private IccRecords getUiccRecords(int i) {
        return this.mUiccController.getIccRecords(this.mPhone.getPhoneId(), i);
    }

    private class SSConfigHandler extends Handler {
        public SSConfigHandler() {
        }

        public SSConfigHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            MtkSuppServConf.this.logd("handleMessage msg: " + MtkSuppServConf.this.eventToString(message.what));
            switch (message.what) {
                case 0:
                case 1:
                case 2:
                    MtkSuppServConf.this.resetConfig();
                    MtkSuppServConf.this.loadCarrierConfig();
                    MtkSuppServConf.this.printConfig();
                    break;
                case 3:
                    sendMessageDelayed(obtainMessage(2), 5000L);
                    break;
                case 4:
                    MtkSuppServConf.this.onUpdateIcc();
                    break;
            }
        }
    }

    private String eventToString(int i) {
        switch (i) {
            case 0:
                return "EVENT_INIT";
            case 1:
                return "EVENT_CARRIER_CONFIG_LOADED";
            case 2:
                return "EVENT_CARRIER_CONFIG_LOADED_TIMEOUT";
            case 3:
                return "EVENT_RECORDS_LOADED";
            case 4:
                return "EVENT_ICC_CHANGED";
            default:
                return "UNKNOWN_EVENT";
        }
    }

    private void initConfig() {
        logi("initConfig start.");
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.GSM_UT_SUPPORT, new SSConfig("mtk_carrier_ss_gsm_ut_support", 0));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_XCAP, new SSConfig("mtk_carrier_ss_not_support_xcap", 1));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.TBCLIR, new SSConfig("mtk_carrier_ss_tb_clir", 2));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.IMS_NW_CW, new SSConfig("mtk_carrier_ss_ims_nw_cw", 3));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.ENABLE_XCAP_HTTP_RESPONSE_409, new SSConfig("mtk_carrier_ss_enable_xcap_http_response_409", 4));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.TRANSFER_XCAP_404, new SSConfig("mtk_carrier_ss_transfer_xcap_404", 5));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_CALL_IDENTITY, new SSConfig("mtk_carrier_ss_not_support_call_identity", 6));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.RE_REGISTER_FOR_CF, new SSConfig("mtk_carrier_ss_re_register_for_cf", 7));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.SUPPORT_SAVE_CF_NUMBER, new SSConfig("mtk_carrier_ss_support_save_cf_number", 8));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.QUERY_CFU_AGAIN_AFTER_SET, new SSConfig("mtk_carrier_ss_query_cfu_again_after_set", 9));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_OCB, new SSConfig("mtk_carrier_ss_not_support_ocb", 10));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_WFC_UT, new SSConfig("mtk_carrier_ss_not_support_wfc_ut", 11));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.NEED_CHECK_DATA_ENABLE, new SSConfig("mtk_carrier_ss_need_check_data_enable", 12));
        this.mCustomizationMap.put(MtkSuppServContants.CUSTOMIZATION_ITEM.NEED_CHECK_DATA_ROAMING, new SSConfig("mtk_carrier_ss_need_check_data_roaming", 13));
        logi("initConfig end.");
    }

    private void resetConfig() {
        logi("resetConfig start.");
        Iterator<Map.Entry<MtkSuppServContants.CUSTOMIZATION_ITEM, SSConfig>> it = this.mCustomizationMap.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().reset();
        }
        logi("resetConfig end.");
    }

    private void loadCarrierConfig() {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        int subId = this.mPhone.getSubId();
        if (carrierConfigManager != null) {
            configForSubId = carrierConfigManager.getConfigForSubId(subId);
        } else {
            logd("CarrierConfigManager is null.");
            configForSubId = null;
        }
        if (configForSubId != null) {
            loadFromCarrierConfig(configForSubId);
        } else {
            logd("Config is null.");
        }
    }

    private void loadFromCarrierConfig(PersistableBundle persistableBundle) {
        logi("loadFromCarrierConfig start.");
        SSConfig sSConfig = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.GSM_UT_SUPPORT);
        if (sSConfig != null && persistableBundle.containsKey(sSConfig.mCarrierConfigKey)) {
            sSConfig.setValue(persistableBundle.getBoolean(sSConfig.mCarrierConfigKey, sSConfig.bDefault));
        }
        SSConfig sSConfig2 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_XCAP);
        if (sSConfig2 != null && persistableBundle.containsKey(sSConfig2.mCarrierConfigKey)) {
            sSConfig2.setValue(persistableBundle.getBoolean(sSConfig2.mCarrierConfigKey, sSConfig2.bDefault));
        }
        SSConfig sSConfig3 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.TBCLIR);
        if (sSConfig3 != null && persistableBundle.containsKey(sSConfig3.mCarrierConfigKey)) {
            sSConfig3.setValue(persistableBundle.getBoolean(sSConfig3.mCarrierConfigKey, sSConfig3.bDefault));
        }
        SSConfig sSConfig4 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.IMS_NW_CW);
        if (sSConfig4 != null && persistableBundle.containsKey(sSConfig4.mCarrierConfigKey)) {
            sSConfig4.setValue(persistableBundle.getBoolean(sSConfig4.mCarrierConfigKey, sSConfig4.bDefault));
        }
        SSConfig sSConfig5 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.ENABLE_XCAP_HTTP_RESPONSE_409);
        if (sSConfig5 != null && persistableBundle.containsKey(sSConfig5.mCarrierConfigKey)) {
            sSConfig5.setValue(persistableBundle.getBoolean(sSConfig5.mCarrierConfigKey, sSConfig5.bDefault));
        }
        SSConfig sSConfig6 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.TRANSFER_XCAP_404);
        if (sSConfig6 != null && persistableBundle.containsKey(sSConfig6.mCarrierConfigKey)) {
            sSConfig6.setValue(persistableBundle.getBoolean(sSConfig6.mCarrierConfigKey, sSConfig6.bDefault));
        }
        SSConfig sSConfig7 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_CALL_IDENTITY);
        if (sSConfig7 != null && persistableBundle.containsKey(sSConfig7.mCarrierConfigKey)) {
            sSConfig7.setValue(persistableBundle.getBoolean(sSConfig7.mCarrierConfigKey, sSConfig7.bDefault));
        }
        SSConfig sSConfig8 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.RE_REGISTER_FOR_CF);
        if (sSConfig8 != null && persistableBundle.containsKey(sSConfig8.mCarrierConfigKey)) {
            sSConfig8.setValue(persistableBundle.getBoolean(sSConfig8.mCarrierConfigKey, sSConfig8.bDefault));
        }
        SSConfig sSConfig9 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.SUPPORT_SAVE_CF_NUMBER);
        if (sSConfig9 != null && persistableBundle.containsKey(sSConfig9.mCarrierConfigKey)) {
            sSConfig9.setValue(persistableBundle.getBoolean(sSConfig9.mCarrierConfigKey, sSConfig9.bDefault));
        }
        SSConfig sSConfig10 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.QUERY_CFU_AGAIN_AFTER_SET);
        if (sSConfig10 != null && persistableBundle.containsKey(sSConfig10.mCarrierConfigKey)) {
            sSConfig10.setValue(persistableBundle.getBoolean(sSConfig10.mCarrierConfigKey, sSConfig10.bDefault));
        }
        SSConfig sSConfig11 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_OCB);
        if (sSConfig11 != null && persistableBundle.containsKey(sSConfig11.mCarrierConfigKey)) {
            sSConfig11.setValue(persistableBundle.getBoolean(sSConfig11.mCarrierConfigKey, sSConfig11.bDefault));
        }
        SSConfig sSConfig12 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_WFC_UT);
        if (sSConfig12 != null && persistableBundle.containsKey(sSConfig12.mCarrierConfigKey)) {
            sSConfig12.setValue(persistableBundle.getBoolean(sSConfig12.mCarrierConfigKey, sSConfig12.bDefault));
        }
        SSConfig sSConfig13 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.NEED_CHECK_DATA_ENABLE);
        if (sSConfig13 != null && persistableBundle.containsKey(sSConfig13.mCarrierConfigKey)) {
            sSConfig13.setValue(persistableBundle.getBoolean(sSConfig13.mCarrierConfigKey, sSConfig13.bDefault));
        }
        SSConfig sSConfig14 = this.mCustomizationMap.get(MtkSuppServContants.CUSTOMIZATION_ITEM.NEED_CHECK_DATA_ROAMING);
        if (sSConfig14 != null && persistableBundle.containsKey(sSConfig14.mCarrierConfigKey)) {
            sSConfig14.setValue(persistableBundle.getBoolean(sSConfig14.mCarrierConfigKey, sSConfig14.bDefault));
        }
        MtkSuppServHelper suppServHelper = MtkSuppServManager.getSuppServHelper(this.mPhone.getPhoneId());
        if (suppServHelper != null) {
            suppServHelper.notifyCarrierConfigLoaded();
        }
        logi("loadFromCarrierConfig end.");
    }

    private int getSysPropForBool(int i) {
        int i2;
        int i3 = -1;
        if (i > -1 && (i2 = SystemProperties.getInt(MtkSuppServContants.SYS_PROP_BOOL_CONFIG, 0)) > 0) {
            int i4 = 1 << i;
            if ((i2 & i4) != 0) {
                i3 = (SystemProperties.getInt(MtkSuppServContants.SYS_PROP_BOOL_VALUE, 0) & i4) != 0 ? 1 : 0;
                StringBuilder sb = new StringBuilder();
                sb.append("getSysPropForBool idx: ");
                sb.append(i);
                sb.append("=");
                sb.append(i3 == 1 ? "TRUE" : "FALSE");
                logi(sb.toString());
            }
        }
        return i3;
    }

    private void printConfig() {
        for (Map.Entry<MtkSuppServContants.CUSTOMIZATION_ITEM, SSConfig> entry : this.mCustomizationMap.entrySet()) {
            logi("" + MtkSuppServContants.toString(entry.getKey()) + " -> " + entry.getValue().toString());
        }
    }

    public boolean isGsmUtSupport(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.GSM_UT_SUPPORT, str);
    }

    public boolean isNotSupportXcap(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_XCAP, str);
    }

    public boolean isTbClir(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.TBCLIR, str);
    }

    public boolean isImsNwCW(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.IMS_NW_CW, str);
    }

    public boolean isEnableXcapHttpResponse409(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.ENABLE_XCAP_HTTP_RESPONSE_409, str);
    }

    public boolean isTransferXcap404(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.TRANSFER_XCAP_404, str);
    }

    public boolean isNotSupportCallIdentity(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_CALL_IDENTITY, str);
    }

    public boolean isReregisterForCF(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.RE_REGISTER_FOR_CF, str);
    }

    public boolean isSupportSaveCFNumber(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.SUPPORT_SAVE_CF_NUMBER, str);
    }

    public boolean isQueryCFUAgainAfterSet(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.QUERY_CFU_AGAIN_AFTER_SET, str);
    }

    public boolean isNotSupportOCB(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_OCB, str);
    }

    public boolean isNotSupportWFCUt(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.NOT_SUPPORT_WFC_UT, str);
    }

    public boolean isNeedCheckDataEnabled(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.NEED_CHECK_DATA_ENABLE, str);
    }

    public boolean isNeedCheckDataRoaming(String str) {
        return getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM.NEED_CHECK_DATA_ROAMING, str);
    }

    private boolean getBooleanValue(MtkSuppServContants.CUSTOMIZATION_ITEM customization_item, String str) {
        if (this.mCustomizationMap == null || !this.mCustomizationMap.containsKey(customization_item)) {
            logi("Null or Without config: " + MtkSuppServContants.toString(customization_item));
            return false;
        }
        SSConfig sSConfig = this.mCustomizationMap.get(customization_item);
        int sysPropForBool = getSysPropForBool(sSConfig.mSystemPropIdx);
        if (sysPropForBool != -1) {
            StringBuilder sb = new StringBuilder();
            sb.append("");
            sb.append(MtkSuppServContants.toString(customization_item));
            sb.append(": ");
            sb.append(sysPropForBool == 1);
            logi(sb.toString());
            return sysPropForBool == 1;
        }
        logi("" + MtkSuppServContants.toString(customization_item) + ": " + sSConfig.bValue);
        if (sSConfig != null) {
            return sSConfig.bValue;
        }
        return false;
    }

    private String getStringValue(MtkSuppServContants.CUSTOMIZATION_ITEM customization_item, String str) {
        String fromOperatorUtilsString = getFromOperatorUtilsString(customization_item, str);
        if (!fromOperatorUtilsString.equals("")) {
            return fromOperatorUtilsString;
        }
        if (this.mCustomizationMap == null || !this.mCustomizationMap.containsKey(customization_item)) {
            logi("Null or Without config: " + MtkSuppServContants.toString(customization_item));
            return "";
        }
        SSConfig sSConfig = this.mCustomizationMap.get(customization_item);
        logi("" + MtkSuppServContants.toString(customization_item) + ": " + sSConfig.sValue);
        return sSConfig != null ? sSConfig.sValue : "";
    }

    private int getIntValue(MtkSuppServContants.CUSTOMIZATION_ITEM customization_item, String str) {
        if (this.mCustomizationMap == null || !this.mCustomizationMap.containsKey(customization_item)) {
            logi("Null or Without config: " + MtkSuppServContants.toString(customization_item));
            return -1;
        }
        SSConfig sSConfig = this.mCustomizationMap.get(customization_item);
        logi("" + MtkSuppServContants.toString(customization_item) + ": " + sSConfig.iValue);
        if (sSConfig != null) {
            return sSConfig.iValue;
        }
        return -1;
    }

    private String getFromOperatorUtilsString(MtkSuppServContants.CUSTOMIZATION_ITEM customization_item, String str) {
        int i = AnonymousClass2.$SwitchMap$com$mediatek$internal$telephony$MtkSuppServContants$CUSTOMIZATION_ITEM[customization_item.ordinal()];
        return "";
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logw(String str) {
        Rlog.w(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logi(String str) {
        Rlog.i(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logd(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logv(String str) {
        Rlog.v(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }
}
