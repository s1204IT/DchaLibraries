package com.android.settings.deviceinfo.simstatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.CellBroadcastMessage;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.euicc.EuiccManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaSimStatus;
import com.mediatek.settings.ext.ISettingsMiscExt;

public class SimStatusDialogController implements LifecycleObserver, OnPause, OnResume {
    static final int CELLULAR_NETWORK_STATE = 2131362032;
    static final int CELL_DATA_NETWORK_TYPE_VALUE_ID = 2131362028;
    static final int CELL_ID_INFO_LABEL_ID = 2131361957;
    static final int CELL_ID_INFO_VALUE_ID = 2131361958;
    static final int CELL_VOICE_NETWORK_TYPE_VALUE_ID = 2131362754;
    static final int EID_INFO_VALUE_ID = 2131362111;
    static final int ICCID_INFO_LABEL_ID = 2131362188;
    static final int ICCID_INFO_VALUE_ID = 2131362189;
    static final int IMS_REGISTRATION_STATE_LABEL_ID = 2131362210;
    static final int IMS_REGISTRATION_STATE_VALUE_ID = 2131362211;
    static final int MCC_MNC_INFO_LABEL_ID = 2131362302;
    static final int MCC_MNC_INFO_VALUE_ID = 2131362303;
    static final int NETWORK_PROVIDER_VALUE_ID = 2131362368;
    static final int OPERATOR_INFO_LABEL_ID = 2131362267;
    static final int OPERATOR_INFO_VALUE_ID = 2131362268;
    static final int PHONE_NUMBER_VALUE_ID = 2131362356;
    static final int ROAMING_INFO_VALUE_ID = 2131362480;
    static final int SERVICE_STATE_VALUE_ID = 2131362537;
    static final int SID_NID_INFO_LABEL_ID = 2131362554;
    static final int SID_NID_INFO_VALUE_ID = 2131362555;
    static final int SIGNAL_STRENGTH_LABEL_ID = 2131362557;
    static final int SIGNAL_STRENGTH_VALUE_ID = 2131362558;
    private final BroadcastReceiver mAreaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras;
            CellBroadcastMessage cellBroadcastMessage;
            if (TextUtils.equals(intent.getAction(), "com.android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED") && (extras = intent.getExtras()) != null && (cellBroadcastMessage = (CellBroadcastMessage) extras.get("message")) != null && SimStatusDialogController.this.mSubscriptionInfo.getSubscriptionId() == cellBroadcastMessage.getSubId()) {
                SimStatusDialogController.this.mDialog.setText(R.id.latest_area_info_value, cellBroadcastMessage.getMessageBody());
            }
        }
    };
    private final CarrierConfigManager mCarrierConfigManager;
    private CdmaSimStatus mCdmaSimStatus;
    private final Context mContext;
    private final SimStatusDialogFragment mDialog;
    private final EuiccManager mEuiccManager;
    private ISettingsMiscExt mMiscExt;
    private PhoneStateListener mPhoneStateListener;
    private final Resources mRes;
    private boolean mShowLatestAreaInfo;
    private final SubscriptionInfo mSubscriptionInfo;
    private final TelephonyManager mTelephonyManager;

    public SimStatusDialogController(SimStatusDialogFragment simStatusDialogFragment, Lifecycle lifecycle, int i) {
        this.mDialog = simStatusDialogFragment;
        this.mContext = simStatusDialogFragment.getContext();
        this.mSubscriptionInfo = getPhoneSubscriptionInfo(i);
        this.mCarrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        this.mEuiccManager = (EuiccManager) this.mContext.getSystemService("euicc");
        this.mRes = this.mContext.getResources();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        if (this.mSubscriptionInfo != null) {
            this.mTelephonyManager = TelephonyManager.from(this.mContext).createForSubscriptionId(this.mSubscriptionInfo.getSubscriptionId());
        } else {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        this.mCdmaSimStatus = new CdmaSimStatus(this.mDialog, this.mSubscriptionInfo);
        this.mMiscExt = UtilsExt.getMiscPlugin(this.mContext);
    }

    public void initialize() {
        updateEid();
        if (this.mSubscriptionInfo == null) {
            removeCdmaStatus();
            return;
        }
        this.mPhoneStateListener = getPhoneStateListener();
        ServiceState currentServiceState = getCurrentServiceState();
        updateNetworkProvider(currentServiceState);
        updatePhoneNumber();
        updateLatestAreaInfo();
        updateServiceState(currentServiceState);
        updateSignalStrength(getSignalStrength());
        updateNetworkType();
        updateRoamingStatus(currentServiceState);
        updateIccidNumber();
        updateImsRegistrationState();
        updateCdmaStatus(currentServiceState);
    }

    @Override
    public void onResume() {
        if (this.mSubscriptionInfo == null) {
            return;
        }
        updateDataState(this.mTelephonyManager.getDataState());
        ServiceState currentServiceState = getCurrentServiceState();
        updateNetworkProvider(currentServiceState);
        updateServiceState(currentServiceState);
        updateSignalStrength(getSignalStrength());
        updateNetworkType();
        updateRoamingStatus(currentServiceState);
        updateCdmaStatus(currentServiceState);
        this.mTelephonyManager.listen(this.mPhoneStateListener, 4417);
        if (this.mShowLatestAreaInfo) {
            this.mContext.registerReceiver(this.mAreaInfoReceiver, new IntentFilter("com.android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED"), "android.permission.RECEIVE_EMERGENCY_BROADCAST", null);
            Intent intent = new Intent("com.android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO");
            intent.setPackage("com.android.cellbroadcastreceiver");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.RECEIVE_EMERGENCY_BROADCAST");
        }
    }

    @Override
    public void onPause() {
        if (this.mSubscriptionInfo == null) {
            return;
        }
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        if (this.mShowLatestAreaInfo) {
            this.mContext.unregisterReceiver(this.mAreaInfoReceiver);
        }
    }

    public void updateNetworkProvider(ServiceState serviceState) {
        this.mDialog.setText(R.id.operator_name_value, serviceState.getOperatorAlphaLong());
    }

    private void updatePhoneNumber() {
        this.mDialog.setText(R.id.number_value, BidiFormatter.getInstance().unicodeWrap(getPhoneNumber(), TextDirectionHeuristics.LTR));
    }

    public void updateDataState(int i) {
        String string;
        switch (i) {
            case 0:
                string = this.mRes.getString(R.string.radioInfo_data_disconnected);
                break;
            case 1:
                string = this.mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case 2:
                string = this.mRes.getString(R.string.radioInfo_data_connected);
                break;
            case 3:
                string = this.mRes.getString(R.string.radioInfo_data_suspended);
                break;
            default:
                string = this.mRes.getString(R.string.radioInfo_unknown);
                break;
        }
        this.mDialog.setText(R.id.data_state_value, string);
    }

    private void updateLatestAreaInfo() {
        this.mShowLatestAreaInfo = Resources.getSystem().getBoolean(android.R.^attr-private.modifierState) && this.mTelephonyManager.getPhoneType() != 2;
        if (!this.mShowLatestAreaInfo) {
            this.mDialog.removeSettingFromScreen(R.id.latest_area_info_label);
            this.mDialog.removeSettingFromScreen(R.id.latest_area_info_value);
        }
    }

    public void updateServiceState(ServiceState serviceState) {
        String string;
        Log.d("SimStatusDialogCtrl", "updateServiceState, serviceState=" + serviceState);
        int state = serviceState.getState();
        if (state == 1 || state == 3) {
            resetSignalStrength();
        }
        switch (state) {
            case 0:
                string = this.mRes.getString(R.string.radioInfo_service_in);
                updateSignalStrength(getSignalStrength());
                break;
            case 1:
            case 2:
                string = this.mRes.getString(R.string.radioInfo_service_out);
                break;
            case 3:
                string = this.mRes.getString(R.string.radioInfo_service_off);
                break;
            default:
                string = this.mRes.getString(R.string.radioInfo_unknown);
                break;
        }
        this.mDialog.setText(R.id.service_state_value, string);
        this.mCdmaSimStatus.setServiceState(serviceState);
    }

    public void updateSignalStrength(SignalStrength signalStrength) {
        boolean z;
        PersistableBundle configForSubId = this.mCarrierConfigManager.getConfigForSubId(this.mSubscriptionInfo.getSubscriptionId());
        if (configForSubId != null) {
            z = configForSubId.getBoolean("show_signal_strength_in_sim_status_bool");
        } else {
            z = true;
        }
        if (!z) {
            this.mDialog.removeSettingFromScreen(R.id.signal_strength_label);
            this.mDialog.removeSettingFromScreen(R.id.signal_strength_value);
            return;
        }
        int state = getCurrentServiceState().getState();
        if (1 == state || 3 == state) {
            return;
        }
        int dbm = getDbm(signalStrength);
        int asuLevel = getAsuLevel(signalStrength);
        if (dbm == -1) {
            dbm = 0;
        }
        if (asuLevel == -1) {
            asuLevel = 0;
        }
        this.mDialog.setText(R.id.signal_strength_value, this.mRes.getString(R.string.sim_signal_strength, Integer.valueOf(dbm), Integer.valueOf(asuLevel)));
        this.mCdmaSimStatus.updateSignalStrength(signalStrength, R.id.signal_strength_value, this.mRes.getString(R.string.sim_signal_strength, Integer.valueOf(dbm), Integer.valueOf(asuLevel)));
    }

    private void resetSignalStrength() {
        this.mDialog.setText(R.id.signal_strength_value, "0");
    }

    public void updateNetworkType() {
        String networkTypeName;
        int subscriptionId = this.mSubscriptionInfo.getSubscriptionId();
        int dataNetworkType = this.mTelephonyManager.getDataNetworkType(subscriptionId);
        int voiceNetworkType = this.mTelephonyManager.getVoiceNetworkType(subscriptionId);
        Log.d("SimStatusDialogCtrl", "updateNetworkType, dataType=" + dataNetworkType + ", voiceType=" + voiceNetworkType);
        String networkTypeName2 = null;
        if (dataNetworkType != 0) {
            TelephonyManager telephonyManager = this.mTelephonyManager;
            networkTypeName = TelephonyManager.getNetworkTypeName(dataNetworkType);
        } else {
            networkTypeName = null;
        }
        if (voiceNetworkType != 0) {
            TelephonyManager telephonyManager2 = this.mTelephonyManager;
            networkTypeName2 = TelephonyManager.getNetworkTypeName(voiceNetworkType);
        }
        boolean z = false;
        try {
            Context contextCreatePackageContext = this.mContext.createPackageContext("com.android.systemui", 0);
            z = contextCreatePackageContext.getResources().getBoolean(contextCreatePackageContext.getResources().getIdentifier("config_show4GForLTE", "bool", "com.android.systemui"));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("SimStatusDialogCtrl", "NameNotFoundException for show4GForLTE");
        }
        if (z) {
            if ("LTE".equals(networkTypeName)) {
                networkTypeName = "4G";
            }
            if ("LTE".equals(networkTypeName2)) {
                networkTypeName2 = "4G";
            }
        }
        String networktypeString = this.mMiscExt.getNetworktypeString(networkTypeName, subscriptionId);
        this.mDialog.setText(R.id.voice_network_type_value, this.mMiscExt.getNetworktypeString(networkTypeName2, subscriptionId));
        this.mDialog.setText(R.id.data_network_type_value, networktypeString);
    }

    public void updateRoamingStatus(ServiceState serviceState) {
        if (serviceState.getRoaming()) {
            this.mDialog.setText(R.id.roaming_state_value, this.mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            this.mDialog.setText(R.id.roaming_state_value, this.mRes.getString(R.string.radioInfo_roaming_not));
        }
    }

    public void updateIccidNumber() {
        boolean z;
        int subscriptionId = this.mSubscriptionInfo.getSubscriptionId();
        PersistableBundle configForSubId = this.mCarrierConfigManager.getConfigForSubId(subscriptionId);
        if (configForSubId != null) {
            z = configForSubId.getBoolean("show_iccid_in_sim_status_bool");
        } else {
            z = false;
        }
        Log.i("SimStatusDialogCtrl", "showIccId = " + z);
        if (!z) {
            this.mDialog.removeSettingFromScreen(R.id.icc_id_label);
            this.mDialog.removeSettingFromScreen(R.id.icc_id_value);
            return;
        }
        if (!this.mDialog.isSettingOnScreen(R.id.icc_id_label)) {
            Log.i("SimStatusDialogCtrl", "add setingsTO Screen ");
            this.mDialog.addSettingToScreen(R.id.icc_id_label);
            this.mDialog.addSettingToScreen(R.id.icc_id_value);
        }
        this.mDialog.setText(R.id.icc_id_value, getSimSerialNumber(subscriptionId));
    }

    private void updateEid() {
        this.mDialog.setText(R.id.esim_id_value, this.mEuiccManager.getEid());
    }

    private void updateImsRegistrationState() {
        int subscriptionId = this.mSubscriptionInfo.getSubscriptionId();
        PersistableBundle configForSubId = this.mCarrierConfigManager.getConfigForSubId(subscriptionId);
        if (configForSubId == null ? false : configForSubId.getBoolean("show_ims_registration_status_bool")) {
            this.mDialog.setText(R.id.ims_reg_state_value, this.mRes.getString(this.mTelephonyManager.isImsRegistered(subscriptionId) ? R.string.ims_reg_status_registered : R.string.ims_reg_status_not_registered));
        } else {
            this.mDialog.removeSettingFromScreen(R.id.ims_reg_state_label);
            this.mDialog.removeSettingFromScreen(R.id.ims_reg_state_value);
        }
    }

    private SubscriptionInfo getPhoneSubscriptionInfo(int i) {
        SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = SubscriptionManager.from(this.mContext).getActiveSubscriptionInfoForSimSlotIndex(i);
        StringBuilder sb = new StringBuilder();
        sb.append("getPhoneSubscriptionInfo, slotId=");
        sb.append(i);
        sb.append(", subInfo=");
        sb.append(activeSubscriptionInfoForSimSlotIndex == null ? "null" : activeSubscriptionInfoForSimSlotIndex);
        Log.d("SimStatusDialogCtrl", sb.toString());
        return activeSubscriptionInfoForSimSlotIndex;
    }

    ServiceState getCurrentServiceState() {
        return this.mTelephonyManager.getServiceStateForSubscriber(this.mSubscriptionInfo.getSubscriptionId());
    }

    int getDbm(SignalStrength signalStrength) {
        return signalStrength.getDbm();
    }

    int getAsuLevel(SignalStrength signalStrength) {
        return signalStrength.getAsuLevel();
    }

    PhoneStateListener getPhoneStateListener() {
        return new PhoneStateListener(Integer.valueOf(this.mSubscriptionInfo.getSubscriptionId())) {
            @Override
            public void onDataConnectionStateChanged(int i) {
                if (SimStatusDialogController.this.mDialog.getDialog() != null) {
                    Log.d("SimStatusDialogCtrl", "onDataConnectionStateChanged, state=" + i + ", subInfo=" + SimStatusDialogController.this.mSubscriptionInfo);
                    SimStatusDialogController.this.updateDataState(i);
                    SimStatusDialogController.this.updateNetworkType();
                    return;
                }
                Log.w("SimStatusDialogCtrl", "DataConnectionStateChanged, dialog is null.");
            }

            @Override
            public void onPreciseDataConnectionStateChanged(PreciseDataConnectionState preciseDataConnectionState) {
                if (SimStatusDialogController.this.mDialog.getDialog() == null) {
                    Log.w("SimStatusDialogCtrl", "onPreciseDataConnectionStateChanged, dialog is null.");
                    return;
                }
                String dataConnectionAPNType = preciseDataConnectionState.getDataConnectionAPNType();
                if (dataConnectionAPNType != null && dataConnectionAPNType.equals("preempt")) {
                    int dataConnectionState = preciseDataConnectionState.getDataConnectionState();
                    Log.d("SimStatusDialogCtrl", "onPreciseDataConnectionStateChanged, state=" + dataConnectionState + ", dataConnectionState=" + preciseDataConnectionState);
                    SimStatusDialogController.this.updateDataState(dataConnectionState);
                }
            }

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                if (SimStatusDialogController.this.mDialog.getDialog() == null) {
                    Log.w("SimStatusDialogCtrl", "onSignalStrengthsChanged, dialog is null.");
                    return;
                }
                Log.d("SimStatusDialogCtrl", "onSignalStrengthsChanged, signalStrength=" + signalStrength);
                SimStatusDialogController.this.updateSignalStrength(signalStrength);
            }

            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                if (SimStatusDialogController.this.mDialog.getDialog() == null) {
                    Log.w("SimStatusDialogCtrl", "onServiceStateChanged, dialog is null.");
                    return;
                }
                Log.d("SimStatusDialogCtrl", "onServiceStateChanged, serviceState=" + serviceState);
                SimStatusDialogController.this.updateNetworkProvider(serviceState);
                SimStatusDialogController.this.updateServiceState(serviceState);
                SimStatusDialogController.this.updateRoamingStatus(serviceState);
                SimStatusDialogController.this.updateNetworkType();
                SimStatusDialogController.this.updateCdmaStatus(serviceState);
                SimStatusDialogController.this.updateIccidNumber();
            }
        };
    }

    String getPhoneNumber() {
        return DeviceInfoUtils.getFormattedPhoneNumber(this.mContext, this.mSubscriptionInfo);
    }

    SignalStrength getSignalStrength() {
        return this.mTelephonyManager.getSignalStrength();
    }

    String getSimSerialNumber(int i) {
        return this.mTelephonyManager.getSimSerialNumber(i);
    }

    public void updateCdmaStatus(ServiceState serviceState) {
        int subscriptionId = this.mSubscriptionInfo.getSubscriptionId();
        int currentPhoneType = this.mTelephonyManager.getCurrentPhoneType(subscriptionId);
        Log.d("SimStatusDialogCtrl", "updateCdmaStatus, subId=" + subscriptionId + ", phoneType=" + currentPhoneType);
        if (currentPhoneType == 2) {
            updateMccMnc();
            updateSidNid(serviceState);
            updateCellId();
            return;
        }
        removeCdmaStatus();
    }

    private void removeCdmaStatus() {
        this.mDialog.removeSettingFromScreen(R.id.mcc_mnc_id_label);
        this.mDialog.removeSettingFromScreen(R.id.mcc_mnc_id_value);
        this.mDialog.removeSettingFromScreen(R.id.sid_nid_id_label);
        this.mDialog.removeSettingFromScreen(R.id.sid_nid_id_value);
        this.mDialog.removeSettingFromScreen(R.id.cell_id_label);
        this.mDialog.removeSettingFromScreen(R.id.cell_id_value);
    }

    private void updateMccMnc() {
        int subscriptionId = this.mSubscriptionInfo.getSubscriptionId();
        String simOperator = this.mTelephonyManager.getSimOperator(subscriptionId);
        Log.d("SimStatusDialogCtrl", "updateMccMnc, subId=" + subscriptionId + ", numeric=" + simOperator);
        if (simOperator.length() > 3) {
            String str = simOperator.substring(0, 3) + "," + simOperator.substring(3);
            Log.d("SimStatusDialogCtrl", "updateMccMnc, mccmnc=" + str);
            this.mDialog.setText(R.id.mcc_mnc_id_value, str);
        } else {
            Log.d("SimStatusDialogCtrl", "updateMccMnc, numeric is too short.");
            this.mDialog.setText(R.id.mcc_mnc_id_value, null);
        }
        if (!this.mDialog.isSettingOnScreen(R.id.mcc_mnc_id_label)) {
            this.mDialog.addSettingToScreen(R.id.mcc_mnc_id_label);
            this.mDialog.addSettingToScreen(R.id.mcc_mnc_id_value);
        }
    }

    private void updateSidNid(ServiceState serviceState) {
        Log.d("SimStatusDialogCtrl", "updateSidNid, serviceState=" + serviceState);
        String str = serviceState.getCdmaSystemId() + "," + serviceState.getCdmaNetworkId();
        Log.d("SimStatusDialogCtrl", "updateSidNid, sidnid=" + str);
        this.mDialog.setText(R.id.sid_nid_id_value, str);
        if (!this.mDialog.isSettingOnScreen(R.id.sid_nid_id_label)) {
            this.mDialog.addSettingToScreen(R.id.sid_nid_id_label);
            this.mDialog.addSettingToScreen(R.id.sid_nid_id_value);
        }
    }

    private void updateCellId() {
        CellLocation cellLocation = this.mTelephonyManager.getCellLocation();
        if (cellLocation instanceof CdmaCellLocation) {
            String string = Integer.toString(((CdmaCellLocation) cellLocation).getBaseStationId());
            Log.d("SimStatusDialogCtrl", "updateCellId, cellId=" + string);
            this.mDialog.setText(R.id.cell_id_value, string);
        } else {
            Log.d("SimStatusDialogCtrl", "updateCellId, not CDMA cell location.");
            this.mDialog.setText(R.id.cell_id_value, null);
        }
        if (!this.mDialog.isSettingOnScreen(R.id.cell_id_label)) {
            this.mDialog.addSettingToScreen(R.id.cell_id_label);
            this.mDialog.addSettingToScreen(R.id.cell_id_value);
        }
    }
}
