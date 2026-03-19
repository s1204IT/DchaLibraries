package com.mediatek.internal.telephony.dataconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.cat.CatService;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.ppl.IPplAgent;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
import java.util.ArrayList;
import java.util.Arrays;

public class DataSubSelector {
    public static final String ACTION_MOBILE_DATA_ENABLE = "android.intent.action.ACTION_MOBILE_DATA_ENABLE";
    static final String ACTION_SHUTDOWN_IPO = "android.intent.action.ACTION_SHUTDOWN_IPO";
    private static final boolean DBG = true;
    public static final String EXTRA_MOBILE_DATA_ENABLE_REASON = "reason";
    private static final String FIRST_TIME_ROAMING = "first_time_roaming";
    private static final int HOME_POLICY = 0;
    private static final String NEED_TO_EXECUTE_ROAMING = "need_to_execute_roaming";
    private static final String NEED_TO_WAIT_IMSI = "persist.radio.need.wait.imsi";
    private static final String NEED_TO_WAIT_UNLOCKED = "persist.radio.unlock";
    private static final String NEED_TO_WAIT_UNLOCKED_ROAMING = "persist.radio.unlock.roaming";
    private static final String NEW_SIM_SLOT = "persist.radio.new.sim.slot";
    private static final String NO_SIM_VALUE = "N/A";
    private static final String OLD_ICCID = "old_iccid";
    private static final String OPERATOR_OM = "OM";
    private static final String OPERATOR_OP01 = "OP01";
    private static final String OPERATOR_OP02 = "OP02";
    private static final String OPERATOR_OP09 = "OP09";
    private static final String OPERATOR_OP18 = "OP18";
    private static final int POLICY_DEFAULT = 1;
    private static final int POLICY_NO_AUTO = 0;
    private static final int POLICY_POLICY1 = 2;
    private static final String PROPERTY_3G_SIM = "persist.radio.simswitch";
    private static final String PROPERTY_DEFAULT_DATA_ICCID = "persist.radio.data.iccid";
    private static final String PROPERTY_DEFAULT_SIMSWITCH_ICCID = "persist.radio.simswitch.iccid";
    private static final String PROPERTY_MOBILE_DATA_ENABLE = "persist.radio.mobile.data";
    public static final String REASON_MOBILE_DATA_ENABLE_SYSTEM = "system";
    public static final String REASON_MOBILE_DATA_ENABLE_USER = "user";
    private static final int ROAMING_POLICY = 1;
    private static final String SEGC = "SEGC";
    private static final String SEGDEFAULT = "SEGDEFAULT";
    private static final String SIM_STATUS = "persist.radio.sim.status";
    private static final String USER_SELECT_DEFAULT_DATA = "persist.radio.user.select.data";
    private static boolean m6mProject;
    private static String mOperatorSpec;
    private boolean mAirplaneModeOn;
    private Context mContext;
    private int mPhoneNum;
    private static final boolean BSP_PACKAGE = SystemProperties.getBoolean("ro.mtk_bsp_package", false);
    private static final boolean MTK_C2K_SUPPORT = SystemProperties.get("ro.boot.opt_c2k_support").equals("1");
    private static final String PROPERTY_CAPABILITY_SWITCH_POLICY = "ro.mtk_sim_switch_policy";
    private static final int capability_switch_policy = SystemProperties.getInt(PROPERTY_CAPABILITY_SWITCH_POLICY, 1);
    private boolean mIsNeedWaitImsi = false;
    private boolean mIsWaitIccid = false;
    private String[] PROPERTY_ICCID_SIM = {"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
    private String[] PROPERTY_ICCID = {"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
    private boolean mIsNeedWaitImsiRoaming = false;
    private Intent mIntent = null;
    private boolean mIsNeedWaitAirplaneModeOff = false;
    private boolean mIsNeedWaitAirplaneModeOffRoaming = false;
    private boolean mIsInRoaming = false;
    private boolean mHasRegisterWorldModeReceiver = false;
    private int mPhoneId = -1;
    private int mPrevDefaultDataSubId = -1;
    private int mLastValidDefaultDataSubId = -1;
    protected BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String plmn;
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                String simStatus = intent.getStringExtra("ss");
                int slotId = intent.getIntExtra("slot", 0);
                DataSubSelector.this.log("slotId: " + slotId + " simStatus: " + simStatus + " mIsNeedWaitImsi: " + DataSubSelector.this.mIsNeedWaitImsi + " mIsNeedWaitUnlock: " + DataSubSelector.this.isNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED));
                if (!simStatus.equals("IMSI")) {
                    if (simStatus.equals("ABSENT")) {
                        RadioCapabilitySwitchUtil.updateSimImsiStatus(slotId, "0");
                        RadioCapabilitySwitchUtil.clearRilMccMnc(slotId);
                        return;
                    } else {
                        if (simStatus.equals("NOT_READY")) {
                            RadioCapabilitySwitchUtil.updateSimImsiStatus(slotId, "0");
                            return;
                        }
                        return;
                    }
                }
                RadioCapabilitySwitchUtil.updateSimImsiStatus(slotId, "1");
                RadioCapabilitySwitchUtil.clearRilMccMnc(slotId);
                if (DataSubSelector.this.isOP01OMSupport()) {
                    DataSubSelector.this.subSelectorForOp01OM();
                    return;
                }
                if (DataSubSelector.this.mIsNeedWaitImsi || DataSubSelector.this.mIsNeedWaitImsiRoaming) {
                    if (DataSubSelector.this.mIsNeedWaitImsi) {
                        DataSubSelector.this.mIsNeedWaitImsi = false;
                        if (DataSubSelector.OPERATOR_OP02.equals(DataSubSelector.mOperatorSpec)) {
                            DataSubSelector.this.log("get imsi and need to check op02 again");
                            if (!DataSubSelector.this.checkOp02CapSwitch(0)) {
                                DataSubSelector.this.mIsNeedWaitImsi = true;
                            }
                        } else if (DataSubSelector.OPERATOR_OP18.equals(DataSubSelector.mOperatorSpec)) {
                            DataSubSelector.this.log("get imsi and need to check op18 again");
                            if (!DataSubSelector.this.checkOp18CapSwitch()) {
                                DataSubSelector.this.mIsNeedWaitImsi = true;
                            }
                        }
                    }
                    if (DataSubSelector.this.mIsNeedWaitImsiRoaming) {
                        DataSubSelector.this.mIsNeedWaitImsiRoaming = false;
                        DataSubSelector.this.log("get imsi and need to check op02Roaming again");
                        if (DataSubSelector.this.checkOp02CapSwitch(1)) {
                            return;
                        }
                        DataSubSelector.this.mIsNeedWaitImsiRoaming = true;
                        return;
                    }
                    return;
                }
                if (!DataSubSelector.this.isNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED) && !DataSubSelector.this.isNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED_ROAMING)) {
                    if (DataSubSelector.this.isNeedWaitImsi(DataSubSelector.NEED_TO_WAIT_IMSI)) {
                        DataSubSelector.this.log("NeedWaitImsi so check again");
                        if (DataSubSelector.OPERATOR_OP01.equals(DataSubSelector.mOperatorSpec)) {
                            DataSubSelector.this.setNeedWaitImsi(DataSubSelector.NEED_TO_WAIT_IMSI, Boolean.toString(false));
                            DataSubSelector.this.subSelectorForOp01(DataSubSelector.this.mIntent);
                            return;
                        }
                        return;
                    }
                    return;
                }
                DataSubSelector.this.log("get imsi because unlock");
                ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
                if (iTelEx != null) {
                    try {
                        if (iTelEx.isCapabilitySwitching()) {
                            return;
                        }
                        if (DataSubSelector.this.isNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED)) {
                            DataSubSelector.this.setNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED, "false");
                            if (DataSubSelector.OPERATOR_OP02.equals(DataSubSelector.mOperatorSpec)) {
                                if (SystemProperties.getBoolean("ro.mtk_disable_cap_switch", false)) {
                                    DataSubSelector.this.subSelectorForOp02(DataSubSelector.this.mIntent);
                                } else {
                                    DataSubSelector.this.subSelectorForOp02();
                                }
                            } else if ("OM".equals(DataSubSelector.mOperatorSpec)) {
                                DataSubSelector.this.subSelectorForOm(DataSubSelector.this.mIntent);
                            } else if (DataSubSelector.this.isOP09ASupport()) {
                                DataSubSelector.this.subSelectorForOp09(DataSubSelector.this.mIntent);
                            } else if (DataSubSelector.OPERATOR_OP18.equals(DataSubSelector.mOperatorSpec)) {
                                DataSubSelector.this.subSelectorForOp18(DataSubSelector.this.mIntent);
                            }
                        }
                        if (DataSubSelector.this.isNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED_ROAMING)) {
                            DataSubSelector.this.setNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED_ROAMING, "false");
                            DataSubSelector.this.checkOp02CapSwitch(1);
                        }
                        if (DataSubSelector.OPERATOR_OP01.equals(DataSubSelector.mOperatorSpec)) {
                            DataSubSelector.this.log("get imsi so check op01 again, do not set mIntent");
                            DataSubSelector.this.subSelectorForOp01(DataSubSelector.this.mIntent);
                            return;
                        }
                        return;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                return;
            }
            if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE") || action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED")) {
                DataSubSelector.this.log("mIsNeedWaitUnlock = " + DataSubSelector.this.isNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED) + ", mIsNeedWaitUnlockRoaming = " + DataSubSelector.this.isNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED_ROAMING));
                if (DataSubSelector.this.isNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED) || DataSubSelector.this.isNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED_ROAMING)) {
                    if (DataSubSelector.this.isNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED)) {
                        DataSubSelector.this.setNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED, "false");
                        if (DataSubSelector.OPERATOR_OP01.equals(DataSubSelector.mOperatorSpec)) {
                            DataSubSelector.this.subSelectorForOp01(DataSubSelector.this.mIntent);
                        } else if (DataSubSelector.OPERATOR_OP02.equals(DataSubSelector.mOperatorSpec)) {
                            if (SystemProperties.getBoolean("ro.mtk_disable_cap_switch", false)) {
                                DataSubSelector.this.subSelectorForOp02(DataSubSelector.this.mIntent);
                            } else {
                                DataSubSelector.this.subSelectorForOp02();
                            }
                        } else if ("OM".equals(DataSubSelector.mOperatorSpec)) {
                            DataSubSelector.this.subSelectorForOm(DataSubSelector.this.mIntent);
                        } else if (DataSubSelector.this.isOP09ASupport()) {
                            DataSubSelector.this.subSelectorForOp09(DataSubSelector.this.mIntent);
                        } else if (DataSubSelector.OPERATOR_OP18.equals(DataSubSelector.mOperatorSpec)) {
                            DataSubSelector.this.subSelectorForOp18(DataSubSelector.this.mIntent);
                        }
                    }
                    if (DataSubSelector.this.isNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED_ROAMING)) {
                        DataSubSelector.this.setNeedWaitUnlock(DataSubSelector.NEED_TO_WAIT_UNLOCKED_ROAMING, "false");
                        DataSubSelector.this.checkOp02CapSwitch(1);
                        return;
                    }
                    return;
                }
                return;
            }
            if (action.equals("mediatek.intent.action.LOCATED_PLMN_CHANGED")) {
                DataSubSelector.this.log("ACTION_LOCATED_PLMN_CHANGED");
                if (SystemProperties.getBoolean("ro.mtk_disable_cap_switch", false)) {
                    return;
                }
                if (!DataSubSelector.OPERATOR_OP02.equals(DataSubSelector.mOperatorSpec)) {
                    if (!"OM".equals(DataSubSelector.mOperatorSpec) || (plmn = intent.getStringExtra(Telephony.CellBroadcasts.PLMN)) == null || UsimPBMemInfo.STRING_NOT_SET.equals(plmn) || ServiceStateTracker.UNACTIVATED_MIN2_VALUE.equals(plmn)) {
                        return;
                    }
                    DataSubSelector.this.log("plmn = " + plmn);
                    if (plmn.startsWith(RadioCapabilitySwitchUtil.CN_MCC)) {
                        DataSubSelector.this.mIsInRoaming = false;
                        return;
                    } else {
                        DataSubSelector.this.mIsInRoaming = true;
                        return;
                    }
                }
                String plmn2 = intent.getStringExtra(Telephony.CellBroadcasts.PLMN);
                if (plmn2 == null || UsimPBMemInfo.STRING_NOT_SET.equals(plmn2) || ServiceStateTracker.UNACTIVATED_MIN2_VALUE.equals(plmn2)) {
                    return;
                }
                DataSubSelector.this.log("plmn = " + plmn2);
                SharedPreferences preference = context.getSharedPreferences(DataSubSelector.FIRST_TIME_ROAMING, 0);
                SharedPreferences.Editor editor = preference.edit();
                boolean firstTimeRoaming = preference.getBoolean(DataSubSelector.NEED_TO_EXECUTE_ROAMING, true);
                if (plmn2.startsWith(RadioCapabilitySwitchUtil.CN_MCC)) {
                    if (firstTimeRoaming) {
                        return;
                    }
                    DataSubSelector.this.log("reset roaming flag");
                    editor.clear();
                    editor.commit();
                    return;
                }
                if (firstTimeRoaming) {
                    if (DataSubSelector.this.mIsNeedWaitImsi) {
                        DataSubSelector.this.mIsNeedWaitImsiRoaming = true;
                        return;
                    } else {
                        DataSubSelector.this.checkOp02CapSwitch(1);
                        return;
                    }
                }
                return;
            }
            if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                DataSubSelector.this.mAirplaneModeOn = intent.getBooleanExtra("state", false);
                DataSubSelector.this.log("ACTION_AIRPLANE_MODE_CHANGED, enabled = " + DataSubSelector.this.mAirplaneModeOn);
                if (DataSubSelector.this.mAirplaneModeOn) {
                    return;
                }
                if (DataSubSelector.this.mIsNeedWaitAirplaneModeOff) {
                    DataSubSelector.this.mIsNeedWaitAirplaneModeOff = false;
                    if (DataSubSelector.OPERATOR_OP01.equals(DataSubSelector.mOperatorSpec)) {
                        DataSubSelector.this.subSelectorForOp01(DataSubSelector.this.mIntent);
                    } else if (DataSubSelector.OPERATOR_OP02.equals(DataSubSelector.mOperatorSpec)) {
                        if (SystemProperties.getBoolean("ro.mtk_disable_cap_switch", false)) {
                            DataSubSelector.this.subSelectorForOp02(DataSubSelector.this.mIntent);
                        } else {
                            DataSubSelector.this.subSelectorForOp02();
                        }
                    } else if ("OM".equals(DataSubSelector.mOperatorSpec)) {
                        DataSubSelector.this.subSelectorForOm(DataSubSelector.this.mIntent);
                    } else if (DataSubSelector.this.isOP09ASupport()) {
                        DataSubSelector.this.subSelectorForOp09(DataSubSelector.this.mIntent);
                    }
                }
                if (DataSubSelector.this.mIsNeedWaitAirplaneModeOffRoaming) {
                    DataSubSelector.this.mIsNeedWaitAirplaneModeOffRoaming = false;
                    DataSubSelector.this.checkOp02CapSwitch(1);
                    return;
                }
                return;
            }
            if ("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED".equals(action)) {
                int nDefaultDataSubId = intent.getIntExtra("subscription", -1);
                boolean userSelected = SystemProperties.getBoolean(DataSubSelector.USER_SELECT_DEFAULT_DATA, false);
                DataSubSelector.this.log("mIsUserConfirmDefaultData/nDefaultDataSubId:" + userSelected + "/" + nDefaultDataSubId);
                if (userSelected && SubscriptionManager.isValidSubscriptionId(nDefaultDataSubId)) {
                    DataSubSelector.this.handleDataEnableForOp02(2);
                    SystemProperties.set(DataSubSelector.USER_SELECT_DEFAULT_DATA, Boolean.toString(false));
                }
                DataSubSelector.this.setLastValidDefaultDataSub(nDefaultDataSubId);
                return;
            }
            if (!"android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(action)) {
                if (action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
                    DataSubSelector.this.log("DataSubSelector receive ACTION_SHUTDOWN_IPO, clear properties");
                    RadioCapabilitySwitchUtil.clearAllSimImsiStatus();
                    RadioCapabilitySwitchUtil.clearAllRilMccMnc(DataSubSelector.this.mPhoneNum);
                    return;
                }
                return;
            }
            int detectedType = intent.getIntExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 4);
            DataSubSelector.this.log("Subinfo Record Update: detectedType = " + detectedType);
            if (DataSubSelector.MTK_C2K_SUPPORT) {
                if (DataSubSelector.mOperatorSpec.equals("OM") || DataSubSelector.this.isOP09CSupport()) {
                    if (DataSubSelector.this.isCanSwitch() && (!DataSubSelector.this.isOP09CSupport() || detectedType == 4)) {
                        DataSubSelector.this.subSelectorForC2k6m(DataSubSelector.this.mIntent);
                    }
                } else if (DataSubSelector.this.mIsWaitIccid && DataSubSelector.mOperatorSpec.equals(DataSubSelector.OPERATOR_OP01) && detectedType == 4) {
                    DataSubSelector.this.mIsWaitIccid = false;
                    DataSubSelector.this.subSelectorForOp01(DataSubSelector.this.mIntent);
                    return;
                } else if (DataSubSelector.mOperatorSpec.equals(DataSubSelector.OPERATOR_OP01) && detectedType == 4) {
                    DataSubSelector.this.subSelectorForC2k6m(DataSubSelector.this.mIntent);
                }
            }
            if (detectedType != 4) {
                DataSubSelector.this.onSubInfoReady(intent);
            }
            if (DataSubSelector.this.isOP01OMSupport()) {
                DataSubSelector.this.subSelectorForOp01OM();
            }
        }
    };
    private BroadcastReceiver mWorldModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            DataSubSelector.this.log("mWorldModeReceiver: action = " + action);
            if (!"android.intent.action.ACTION_WORLD_MODE_CHANGED".equals(action)) {
                return;
            }
            int wmState = intent.getIntExtra("worldModeState", -1);
            DataSubSelector.this.log("wmState: " + wmState);
            if (wmState != 1) {
                return;
            }
            for (int i = 0; i < DataSubSelector.this.mPhoneNum; i++) {
                int subid = SubscriptionManager.getSubIdUsingPhoneId(i);
                DataSubSelector.this.log("mworldModeReceiver subid: " + subid);
                if (subid == DataSubSelector.this.mLastValidDefaultDataSubId) {
                    DataSubSelector.this.setCapability(i);
                    DataSubSelector.this.log("mworldModeReceiver set phone id: " + i);
                    return;
                }
            }
            DataSubSelector.this.setCapability(DataSubSelector.this.mPhoneId);
        }
    };

    public DataSubSelector(Context context, int phoneNum) {
        this.mContext = null;
        this.mAirplaneModeOn = false;
        log("DataSubSelector is created");
        this.mPhoneNum = phoneNum;
        mOperatorSpec = SystemProperties.get("persist.operator.optr", "OM");
        m6mProject = SystemProperties.get("ro.mtk_c2k_support").equals("1");
        log("Operator Spec:" + mOperatorSpec + ", c2k_support:" + m6mProject);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        filter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        filter.addAction("mediatek.intent.action.LOCATED_PLMN_CHANGED");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        filter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        filter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        context.registerReceiver(this.mBroadcastReceiver, filter);
        this.mContext = context;
        this.mAirplaneModeOn = Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) == 1;
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        log("defaultDataSub:" + defaultDataSubId);
        setLastValidDefaultDataSub(defaultDataSubId);
    }

    private void onSubInfoReady(Intent intent) {
        this.mIsNeedWaitImsi = false;
        if (BSP_PACKAGE) {
            log("Don't support BSP Package.");
            return;
        }
        if (mOperatorSpec.equals(OPERATOR_OP01)) {
            subSelectorForOp01(intent);
            return;
        }
        if (mOperatorSpec.equals(OPERATOR_OP02)) {
            if (SystemProperties.getBoolean("ro.mtk_disable_cap_switch", false)) {
                subSelectorForOp02(intent);
                return;
            } else {
                subSelectorForOp02();
                return;
            }
        }
        if (isOP09ASupport()) {
            subSelectorForOp09(intent);
            return;
        }
        if (OPERATOR_OP18.equals(mOperatorSpec)) {
            int detectedType = intent.getIntExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 4);
            log("detectedType:" + detectedType);
            if (detectedType != 4) {
                subSelectorForOp18(intent);
                return;
            } else {
                log("skip auto switch when detectedType is NOCHANGE for OP18 when user may set");
                return;
            }
        }
        subSelectorForOm(intent);
    }

    private void subSelectorForSolution15(Intent intent) {
        log("DataSubSelector for C2K om solution 1.5: capability maybe diff with default data");
        int phoneId = -1;
        String[] currIccId = new String[this.mPhoneNum];
        turnOffNewSimData(intent);
        String capabilityIccid = SystemProperties.get("persist.radio.simswitch.iccid");
        log("capability Iccid = " + capabilityIccid);
        int i = 0;
        while (true) {
            if (i >= this.mPhoneNum) {
                break;
            }
            currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i]) || NO_SIM_VALUE.equals(currIccId[i])) {
                break;
            }
            if (!capabilityIccid.equals(currIccId[i])) {
                i++;
            } else {
                phoneId = i;
                break;
            }
        }
        log("capability  phoneid = " + phoneId);
        if (phoneId == -1) {
            return;
        }
        setCapability(phoneId);
    }

    private void subSelectorForOm(Intent intent) {
        String propStr;
        if (MTK_C2K_SUPPORT) {
            return;
        }
        log("DataSubSelector for OM: only for capability switch; for default data, use google");
        int phoneId = -1;
        String[] currIccId = new String[this.mPhoneNum];
        String defaultIccid = UsimPBMemInfo.STRING_NOT_SET;
        int defDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
        int defDataPhoneId = SubscriptionManager.getPhoneId(defDataSubId);
        if (defDataPhoneId >= 0) {
            if (defDataPhoneId >= this.PROPERTY_ICCID_SIM.length) {
                log("phoneId out of boundary :" + defDataPhoneId);
            } else {
                defaultIccid = SystemProperties.get(this.PROPERTY_ICCID_SIM[defDataPhoneId]);
            }
        }
        log("Default data Iccid = " + defaultIccid);
        if (NO_SIM_VALUE.equals(defaultIccid) || UsimPBMemInfo.STRING_NOT_SET.equals(defaultIccid)) {
            return;
        }
        int i = 0;
        while (true) {
            if (i >= this.mPhoneNum) {
                break;
            }
            currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                break;
            }
            if (defaultIccid.equals(currIccId[i])) {
                phoneId = i;
                break;
            }
            if (NO_SIM_VALUE.equals(currIccId[i])) {
                log("clear mcc.mnc:" + i);
                if (i == 0) {
                    propStr = "gsm.sim.ril.mcc.mnc";
                } else {
                    propStr = "gsm.sim.ril.mcc.mnc." + (i + 1);
                }
                SystemProperties.set(propStr, UsimPBMemInfo.STRING_NOT_SET);
            }
            i++;
        }
        if (RadioCapabilitySwitchUtil.isAnySimLocked(this.mPhoneNum)) {
            log("DataSubSelector for OM: do not switch because of sim locking");
            setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "true");
            this.mIntent = intent;
            setSimStatus(intent);
            setNewSimSlot(intent);
            return;
        }
        log("DataSubSelector for OM: no pin lock");
        setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "false");
        if (this.mAirplaneModeOn) {
            log("DataSubSelector for OM: do not switch because of mAirplaneModeOn");
            this.mIsNeedWaitAirplaneModeOff = true;
            this.mIntent = intent;
            setSimStatus(intent);
            setNewSimSlot(intent);
            return;
        }
        log("Default data phoneid = " + phoneId);
        if (phoneId != -1) {
            setCapabilityIfNeeded(phoneId);
        }
        updateDataEnableProperty();
        resetSimStatus();
        resetNewSimSlot();
    }

    private void subSelectorForC2k6m(Intent intent) {
        int phoneId = -1;
        String[] currIccId = new String[this.mPhoneNum];
        String defaultIccid = UsimPBMemInfo.STRING_NOT_SET;
        int defDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
        int defDataPhoneId = SubscriptionManager.getPhoneId(defDataSubId);
        if (defDataPhoneId >= 0) {
            if (defDataPhoneId >= this.PROPERTY_ICCID_SIM.length) {
                log("C2K6M: phoneId out of boundary :" + defDataPhoneId);
            } else {
                defaultIccid = SystemProperties.get(this.PROPERTY_ICCID_SIM[defDataPhoneId]);
            }
        }
        log("C2K6M: Default data phoneId = " + defDataPhoneId + ", Iccid = " + defaultIccid);
        if (UsimPBMemInfo.STRING_NOT_SET.equals(defaultIccid) || NO_SIM_VALUE.equals(defaultIccid)) {
            return;
        }
        int i = 0;
        while (true) {
            if (i >= this.mPhoneNum) {
                break;
            }
            currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                break;
            }
            if (!defaultIccid.equals(currIccId[i])) {
                i++;
            } else {
                phoneId = i;
                break;
            }
        }
        log("C2K6M: Default data phoneid = " + phoneId);
        if (phoneId == -1) {
            return;
        }
        setCapabilityIfNeeded(phoneId);
    }

    private void subSelectorForOp02(Intent intent) {
        String propStr;
        int phoneId = -1;
        int insertedSimCount = 0;
        int insertedStatus = 0;
        int detectedType = intent == null ? getSimStatus() : intent.getIntExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 0);
        String[] currIccId = new String[this.mPhoneNum];
        log("DataSubSelector for OP02");
        for (int i = 0; i < this.mPhoneNum; i++) {
            currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                log("error: iccid not found, wait for next sub ready");
                return;
            }
            if (!NO_SIM_VALUE.equals(currIccId[i])) {
                insertedSimCount++;
                insertedStatus |= 1 << i;
            } else {
                log("clear mcc.mnc:" + i);
                if (i == 0) {
                    propStr = "gsm.sim.ril.mcc.mnc";
                } else {
                    propStr = "gsm.sim.ril.mcc.mnc." + (i + 1);
                }
                SystemProperties.set(propStr, UsimPBMemInfo.STRING_NOT_SET);
            }
        }
        if (RadioCapabilitySwitchUtil.isAnySimLocked(this.mPhoneNum)) {
            log("DataSubSelector for OP02: do not switch because of sim locking");
            setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "true");
            setSimStatus(intent);
            setNewSimSlot(intent);
            return;
        }
        log("DataSubSelector for OP02: no pin lock");
        setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "false");
        if (this.mAirplaneModeOn) {
            log("DataSubSelector for OP02: do not switch because of mAirplaneModeOn");
            this.mIsNeedWaitAirplaneModeOff = true;
            setSimStatus(intent);
            setNewSimSlot(intent);
            return;
        }
        log("Inserted SIM count: " + insertedSimCount + ", insertedStatus: " + insertedStatus);
        if (detectedType == 4) {
            log("OP02 C0: Inserted status no change, do nothing");
        } else if (insertedSimCount == 0) {
            log("OP02 C1: No SIM inserted, set data unset");
            setDefaultData(-1);
        } else if (insertedSimCount == 1) {
            int i2 = 0;
            while (true) {
                if (i2 >= this.mPhoneNum) {
                    break;
                }
                if (((1 << i2) & insertedStatus) == 0) {
                    i2++;
                } else {
                    phoneId = i2;
                    break;
                }
            }
            log("OP02 C2: Single SIM: Set Default data to phone:" + phoneId);
            setDefaultData(phoneId);
            handleDataEnableForOp02(insertedSimCount);
        } else if (insertedSimCount >= 2) {
            log("OP02 C3: Multi SIM: Set Default data to phone1");
            setDefaultData(0);
            handleDataEnableForOp02(insertedSimCount);
        }
        updateDataEnableProperty();
        resetSimStatus();
        resetNewSimSlot();
    }

    private void subSelectorForOp02() {
        String propStr;
        int phoneId = -1;
        int insertedSimCount = 0;
        int insertedStatus = 0;
        String[] currIccId = new String[this.mPhoneNum];
        log("DataSubSelector for op02 (subSelectorForOp02)");
        for (int i = 0; i < this.mPhoneNum; i++) {
            currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
            log("currIccid[" + i + "] : " + currIccId[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                log("error: iccid not found, wait for next sub ready");
                return;
            }
            if (!NO_SIM_VALUE.equals(currIccId[i])) {
                insertedSimCount++;
                insertedStatus |= 1 << i;
            } else {
                log("clear mcc.mnc:" + i);
                if (i == 0) {
                    propStr = "gsm.sim.ril.mcc.mnc";
                } else {
                    propStr = "gsm.sim.ril.mcc.mnc." + (i + 1);
                }
                SystemProperties.set(propStr, UsimPBMemInfo.STRING_NOT_SET);
                log("sim index: " + i + " not inserted");
            }
        }
        if (RadioCapabilitySwitchUtil.isAnySimLocked(this.mPhoneNum)) {
            log("DataSubSelector for OP02: do not switch because of sim locking");
            setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "true");
            return;
        }
        log("DataSubSelector for OP02: no pin lock");
        setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "false");
        if (this.mAirplaneModeOn) {
            log("DataSubSelector for OP02: do not switch because of mAirplaneModeOn");
            this.mIsNeedWaitAirplaneModeOff = true;
            return;
        }
        log("Inserted SIM count: " + insertedSimCount + ", insertedStatus: " + insertedStatus);
        if (insertedSimCount == 0) {
            log("C0: No SIM inserted: set default data unset");
            setDefaultData(-1);
        } else if (insertedSimCount == 1) {
            int i2 = 0;
            while (true) {
                if (i2 >= this.mPhoneNum) {
                    break;
                }
                if (((1 << i2) & insertedStatus) == 0) {
                    i2++;
                } else {
                    phoneId = i2;
                    break;
                }
            }
            log("C1: Single SIM inserted: set default data to phone: " + phoneId);
            setCapability(phoneId);
            setDefaultData(phoneId);
            handleDataEnableForOp02(insertedSimCount);
        } else if (insertedSimCount >= 2 && !checkOp02CapSwitch(0)) {
            this.mIsNeedWaitImsi = true;
        }
        updateDataEnableProperty();
    }

    private void subSelectorForOp01OM() {
        int phoneId = -1;
        int insertedSimCount = 0;
        int insertedStatus = 0;
        String[] currIccId = new String[this.mPhoneNum];
        for (int i = 0; i < this.mPhoneNum; i++) {
            currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                log("op01OM error: iccid not found, wait for next sub ready");
                return;
            }
            log("op01OM currIccId[" + i + "] : " + currIccId[i]);
            if (!NO_SIM_VALUE.equals(currIccId[i])) {
                insertedSimCount++;
                insertedStatus |= 1 << i;
            }
        }
        log("op01OM: Inserted SIM count: " + insertedSimCount + ", insertedStatus: " + insertedStatus);
        if (insertedSimCount != 1) {
            log("DataSubSelector for OP01OM: do not switch because of SimCount != 1");
            return;
        }
        if (RadioCapabilitySwitchUtil.isAnySimLocked(this.mPhoneNum)) {
            log("DataSubSelector for OP01OM: do not switch because of sim locking");
            return;
        }
        if (this.mAirplaneModeOn) {
            log("DataSubSelector for OP01OM: do not switch because of mAirplaneModeOn");
            return;
        }
        int i2 = 0;
        while (true) {
            if (i2 >= this.mPhoneNum) {
                break;
            }
            if (((1 << i2) & insertedStatus) == 0) {
                i2++;
            } else {
                phoneId = i2;
                break;
            }
        }
        log("OP01OM: Single SIM: Set Default data to phone:" + phoneId);
        if (!setCapability(phoneId)) {
            return;
        }
        setDefaultData(phoneId);
    }

    private void subSelectorForOp01(Intent intent) {
        String propStr;
        int phoneId = -1;
        int insertedSimCount = 0;
        int insertedStatus = 0;
        int detectedType = intent == null ? getSimStatus() : intent.getIntExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 0);
        String[] currIccId = new String[this.mPhoneNum];
        log("DataSubSelector for op01");
        String defaultIccid = UsimPBMemInfo.STRING_NOT_SET;
        int defDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
        int defDataPhoneId = SubscriptionManager.getPhoneId(defDataSubId);
        if (defDataPhoneId >= 0) {
            if (defDataPhoneId >= this.PROPERTY_ICCID_SIM.length) {
                log("phoneId out of boundary :" + defDataPhoneId);
            } else {
                defaultIccid = SystemProperties.get(this.PROPERTY_ICCID_SIM[defDataPhoneId]);
            }
        }
        log("Default data Iccid = " + defaultIccid);
        for (int i = 0; i < this.mPhoneNum; i++) {
            currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                log("error: iccid not found, wait for next sub ready");
                this.mIsWaitIccid = true;
                this.mIntent = intent;
                setSimStatus(intent);
                setNewSimSlot(intent);
                return;
            }
            if (defaultIccid.equals(currIccId[i])) {
                phoneId = i;
            }
            log("currIccId[" + i + "] : " + currIccId[i]);
            if (!NO_SIM_VALUE.equals(currIccId[i])) {
                insertedSimCount++;
                insertedStatus |= 1 << i;
            } else {
                log("clear mcc.mnc:" + i);
                if (i == 0) {
                    propStr = "gsm.sim.ril.mcc.mnc";
                } else {
                    propStr = "gsm.sim.ril.mcc.mnc." + (i + 1);
                }
                SystemProperties.set(propStr, UsimPBMemInfo.STRING_NOT_SET);
            }
        }
        this.mIsWaitIccid = false;
        if (RadioCapabilitySwitchUtil.isAnySimLocked(this.mPhoneNum)) {
            log("DataSubSelector for OP01: do not switch because of sim locking");
            setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "true");
            if (intent != null && "android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(intent.getAction())) {
                this.mIntent = intent;
                setSimStatus(intent);
                setNewSimSlot(intent);
                return;
            }
            return;
        }
        log("DataSubSelector for OP01: no pin lock");
        setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "false");
        if (this.mAirplaneModeOn) {
            log("DataSubSelector for OP01: do not switch because of mAirplaneModeOn");
            this.mIsNeedWaitAirplaneModeOff = true;
            if (intent != null && "android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(intent.getAction())) {
                this.mIntent = intent;
                setSimStatus(intent);
                setNewSimSlot(intent);
                return;
            }
            return;
        }
        log("Inserted SIM count: " + insertedSimCount + ", insertedStatus: " + insertedStatus);
        if (insertedSimCount == 0) {
            log("OP01 C0: No SIM inserted, do nothing");
        } else if (insertedSimCount == 1) {
            int i2 = 0;
            while (true) {
                if (i2 >= this.mPhoneNum) {
                    break;
                }
                if (((1 << i2) & insertedStatus) == 0) {
                    i2++;
                } else {
                    phoneId = i2;
                    break;
                }
            }
            log("OP01 C1: Single SIM: Set Default data to phone:" + phoneId);
            setCapability(phoneId);
            if (detectedType != 4) {
                setDefaultData(phoneId);
            }
            turnOffNewSimData(intent);
        } else if (insertedSimCount >= 2) {
            if (intent != null && "android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(intent.getAction())) {
                this.mIntent = intent;
            }
            if (!checkOp01CapSwitch6m()) {
                setNeedWaitImsi(NEED_TO_WAIT_IMSI, Boolean.toString(true));
                if (intent != null && "android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(intent.getAction())) {
                    this.mIntent = intent;
                    setSimStatus(intent);
                    setNewSimSlot(intent);
                    return;
                }
                return;
            }
        }
        resetSimStatus();
        resetNewSimSlot();
        updateDefaultDataProperty();
    }

    private void handleDefaultDataOp01MultiSims() {
        int sub;
        int phoneId = -1;
        int[] simOpInfo = new int[this.mPhoneNum];
        int[] simType = new int[this.mPhoneNum];
        log("OP01 C2: Multi SIM: E");
        if (!RadioCapabilitySwitchUtil.getSimInfo(simOpInfo, simType, 0)) {
            return;
        }
        boolean hasCMCC = false;
        boolean hasOther = false;
        int iOther = -1;
        int preDataSub = SubscriptionController.getInstance().getDefaultDataSubId();
        log("OP01 C2: Multi SIM: preDataSub=" + preDataSub);
        for (int i = 0; i < this.mPhoneNum; i++) {
            if (simOpInfo[i] == 2) {
                if (!hasCMCC) {
                    hasCMCC = true;
                    phoneId = i;
                }
            } else if (!hasOther) {
                hasOther = true;
                iOther = i;
            }
            int sub2 = SubscriptionManager.getSubIdUsingPhoneId(i);
            log("OP01 C2: sub=" + sub2);
            if (preDataSub != sub2 && sub2 > -1) {
                setDataEnabled(i, false);
            }
        }
        if (hasCMCC && hasOther && (sub = SubscriptionManager.getSubIdUsingPhoneId(phoneId)) > -1 && sub != preDataSub) {
            log("OP01 C2: Multi SIM: CMCC + Other, set default data to CMCC");
            setDefaultData(phoneId);
            setDataEnabled(iOther, false);
        }
        log("OP01 C2: Multi SIM: Turn off non default data");
        if (this.mIntent != null) {
            turnOffNewSimData(this.mIntent);
        }
        updateDataEnableProperty();
    }

    private void subSelectorForOp18(Intent intent) {
        String propStr;
        switch (capability_switch_policy) {
            case 0:
                log("subSelectorForOp18: no auto policy, skip");
                break;
            case 1:
                subSelectorForOm(intent);
                break;
            case 2:
                int phoneId = -1;
                int insertedSimCount = 0;
                int insertedStatus = 0;
                int detectedType = intent == null ? getSimStatus() : intent.getIntExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 0);
                String[] currIccId = new String[this.mPhoneNum];
                log("DataSubSelector for op18");
                for (int i = 0; i < this.mPhoneNum; i++) {
                    currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
                    if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                        log("error: iccid not found, wait for next sub ready");
                    } else {
                        log("currIccId[" + i + "] : " + currIccId[i]);
                        if (!NO_SIM_VALUE.equals(currIccId[i])) {
                            insertedSimCount++;
                            insertedStatus |= 1 << i;
                        } else {
                            log("clear mcc.mnc:" + i);
                            if (i == 0) {
                                propStr = "gsm.sim.ril.mcc.mnc";
                            } else {
                                propStr = "gsm.sim.ril.mcc.mnc." + (i + 1);
                            }
                            SystemProperties.set(propStr, UsimPBMemInfo.STRING_NOT_SET);
                        }
                    }
                    break;
                }
                if (RadioCapabilitySwitchUtil.isAnySimLocked(this.mPhoneNum)) {
                    log("DataSubSelector for OP18: do not switch because of sim locking");
                    setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "true");
                    this.mIntent = intent;
                    break;
                } else {
                    log("DataSubSelector for OP18: no pin lock");
                    setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "false");
                    log("Inserted SIM count: " + insertedSimCount + ", insertedStatus: " + insertedStatus);
                    String defaultIccid = SystemProperties.get(PROPERTY_DEFAULT_DATA_ICCID);
                    log("Default data Iccid = " + defaultIccid);
                    if (insertedSimCount == 0) {
                        log("C0: No SIM inserted, set data unset");
                        setDefaultData(-1);
                        break;
                    } else if (insertedSimCount == 1) {
                        int i2 = 0;
                        while (true) {
                            if (i2 < this.mPhoneNum) {
                                if (((1 << i2) & insertedStatus) == 0) {
                                    i2++;
                                } else {
                                    phoneId = i2;
                                }
                            }
                        }
                        if (detectedType == 1) {
                            log("C1: Single SIM + New SIM: Set Default data to phone:" + phoneId);
                            if (setCapability(phoneId)) {
                                setDefaultData(phoneId);
                            }
                            setDataEnabled(phoneId, true);
                        } else if (defaultIccid == null || UsimPBMemInfo.STRING_NOT_SET.equals(defaultIccid)) {
                            log("C3: Single SIM + Non Data SIM: Set Default data to phone:" + phoneId);
                            if (setCapability(phoneId)) {
                                setDefaultData(phoneId);
                            }
                            setDataEnabled(phoneId, true);
                        } else if (defaultIccid.equals(currIccId[phoneId])) {
                            log("C2: Single SIM + Data SIM: Set Default data to phone:" + phoneId);
                            if (setCapability(phoneId)) {
                                setDefaultData(phoneId);
                            }
                        } else {
                            log("C3: Single SIM + Non Data SIM: Set Default data to phone:" + phoneId);
                            if (setCapability(phoneId)) {
                                setDefaultData(phoneId);
                            }
                            setDataEnabled(phoneId, true);
                        }
                        break;
                    } else if (insertedSimCount >= 2 && !checkOp18CapSwitch()) {
                        this.mIsNeedWaitImsi = true;
                        this.mIntent = intent;
                        break;
                    }
                }
                break;
            default:
                log("subSelectorForOp18: Unknow policy, skip");
                break;
        }
    }

    private boolean checkOp01CapSwitch() {
        int[] simOpInfo = new int[this.mPhoneNum];
        int[] simType = new int[this.mPhoneNum];
        int insertedSimCount = 0;
        int csimRuimCount = 0;
        int nonCsimRuimPhoneId = -1;
        int insertedStatus = 0;
        boolean[] op01Usim = new boolean[this.mPhoneNum];
        boolean[] op01Sim = new boolean[this.mPhoneNum];
        boolean[] overseaUsim = new boolean[this.mPhoneNum];
        boolean[] overseaSim = new boolean[this.mPhoneNum];
        String capabilitySimIccid = SystemProperties.get("persist.radio.simswitch.iccid");
        String[] currIccId = new String[this.mPhoneNum];
        log("checkOp01CapSwitch start");
        for (int i = 0; i < this.mPhoneNum; i++) {
            currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                log("error: iccid not found, wait for next sub ready");
                return false;
            }
            if (!NO_SIM_VALUE.equals(currIccId[i])) {
                insertedSimCount++;
                insertedStatus |= 1 << i;
            }
        }
        log("checkOp01CapSwitch : Inserted SIM count: " + insertedSimCount + ", insertedStatus: " + insertedStatus);
        if (RadioCapabilitySwitchUtil.isAnySimLocked(this.mPhoneNum)) {
            log("checkOp01CapSwitch: sim locked");
            setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "true");
        } else {
            log("checkOp01CapSwitch: no sim locked");
            setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "false");
        }
        if (!RadioCapabilitySwitchUtil.getSimInfo(simOpInfo, simType, insertedStatus)) {
            return false;
        }
        int capabilitySimId = Integer.valueOf(SystemProperties.get(PROPERTY_3G_SIM, "1")).intValue() - 1;
        log("op01: capabilitySimIccid:" + capabilitySimIccid + "capabilitySimId:" + capabilitySimId);
        for (int i2 = 0; i2 < this.mPhoneNum; i2++) {
            if (simOpInfo[i2] == 2) {
                if (simType[i2] != 0) {
                    op01Usim[i2] = true;
                } else {
                    op01Sim[i2] = true;
                }
            } else if (simOpInfo[i2] == 1) {
                if (simType[i2] != 0) {
                    overseaUsim[i2] = true;
                } else {
                    overseaSim[i2] = true;
                }
            }
            if (simType[i2] == 0 || simType[i2] == 1) {
                nonCsimRuimPhoneId = i2;
            } else {
                csimRuimCount++;
            }
        }
        log("op01Usim: " + Arrays.toString(op01Usim));
        log("op01Sim: " + Arrays.toString(op01Sim));
        log("overseaUsim: " + Arrays.toString(overseaUsim));
        log("overseaSim: " + Arrays.toString(overseaSim));
        log("csimRuimCount: " + csimRuimCount);
        log("nonCsimRuimPhoneId: " + nonCsimRuimPhoneId);
        for (int i3 = 0; i3 < this.mPhoneNum; i3++) {
            if (capabilitySimIccid.equals(currIccId[i3])) {
                int targetSim = RadioCapabilitySwitchUtil.getHigherPrioritySimForOp01(i3, op01Usim, op01Sim, overseaUsim, overseaSim);
                log("op01: i = " + i3 + ", currIccId : " + currIccId[i3] + ", targetSim : " + targetSim);
                if (op01Usim[i3]) {
                    log("op01-C1: cur is old op01 USIM, no change");
                    if (capabilitySimId != i3) {
                        log("op01-C1a: old op01 USIM change slot, change!");
                        setCapability(i3);
                        return true;
                    }
                    return true;
                }
                if (op01Sim[i3]) {
                    if (targetSim != -1) {
                        log("op01-C2: cur is old op01 SIM but find op01 USIM, change!");
                        setCapability(targetSim);
                        return true;
                    }
                    if (capabilitySimId != i3) {
                        log("op01-C2a: old op01 SIM change slot, change!");
                        setCapability(i3);
                        return true;
                    }
                    return true;
                }
                if (overseaUsim[i3]) {
                    if (targetSim != -1) {
                        log("op01-C3: cur is old OS USIM but find op01 SIMs, change!");
                        setCapability(targetSim);
                        return true;
                    }
                    if (capabilitySimId != i3) {
                        log("op01-C3a: old OS USIM change slot, change!");
                        setCapability(i3);
                        return true;
                    }
                    return true;
                }
                if (overseaSim[i3]) {
                    if (targetSim != -1) {
                        log("op01-C4: cur is old OS SIM but find op01 SIMs/OS USIM, change!");
                        setCapability(targetSim);
                        return true;
                    }
                    if (capabilitySimId != i3) {
                        log("op01-C4a: old OS SIM change slot, change!");
                        setCapability(i3);
                        return true;
                    }
                    return true;
                }
                if (targetSim != -1) {
                    log("op01-C5: cur is old non-op01 SIM/USIM but find higher SIM, change!");
                    setCapability(targetSim);
                    return true;
                }
                log("op01-C6: no higher priority SIM, no cahnge");
                return true;
            }
        }
        int targetSim2 = RadioCapabilitySwitchUtil.getHigherPrioritySimForOp01(capabilitySimId, op01Usim, op01Sim, overseaUsim, overseaSim);
        log("op01: target SIM :" + targetSim2);
        if (op01Usim[capabilitySimId]) {
            log("op01-C7: cur is new op01 USIM, no change");
            return true;
        }
        if (op01Sim[capabilitySimId]) {
            if (targetSim2 != -1) {
                log("op01-C8: cur is new op01 SIM but find op01 USIM, change!");
                setCapability(targetSim2);
                return true;
            }
            return true;
        }
        if (overseaUsim[capabilitySimId]) {
            if (targetSim2 != -1) {
                log("op01-C9: cur is new OS USIM but find op01 SIMs, change!");
                setCapability(targetSim2);
                return true;
            }
            return true;
        }
        if (overseaSim[capabilitySimId]) {
            if (targetSim2 != -1) {
                log("op01-C10: cur is new OS SIM but find op01 SIMs/OS USIM, change!");
                setCapability(targetSim2);
                return true;
            }
            return true;
        }
        if (targetSim2 != -1) {
            log("op01-C11: cur is non-op01 but find higher priority SIM, change!");
            setCapability(targetSim2);
            return true;
        }
        log("op01-C12: no higher priority SIM, no cahnge");
        return true;
    }

    private boolean checkOp01CapSwitch6m() {
        int[] simOpInfo = new int[this.mPhoneNum];
        int[] simType = new int[this.mPhoneNum];
        int insertedSimCount = 0;
        int insertedStatus = 0;
        String[] currIccId = new String[this.mPhoneNum];
        int[] priority = new int[this.mPhoneNum];
        int defDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
        int defDataPhoneId = SubscriptionManager.getPhoneId(defDataSubId);
        if (defDataPhoneId >= 0 && defDataPhoneId < this.mPhoneNum) {
            log("default data phoneId from sub = " + defDataPhoneId);
        } else {
            log("phoneId out of boundary :" + defDataPhoneId);
            defDataPhoneId = -1;
        }
        log("checkOp01CapSwitch6m start");
        for (int i = 0; i < this.mPhoneNum; i++) {
            currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                log("error: iccid not found, wait for next sub ready");
                return false;
            }
            if (!NO_SIM_VALUE.equals(currIccId[i])) {
                insertedSimCount++;
                insertedStatus |= 1 << i;
            }
        }
        log("checkOp01CapSwitch6m : Inserted SIM count: " + insertedSimCount + ", insertedStatus: " + insertedStatus);
        if (RadioCapabilitySwitchUtil.isAnySimLocked(this.mPhoneNum)) {
            log("checkOp01CapSwitch6m: sim locked");
            setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "true");
        } else {
            log("checkOp01CapSwitch6m: no sim locked");
            setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "false");
        }
        if (!RadioCapabilitySwitchUtil.getSimInfo(simOpInfo, simType, insertedStatus)) {
            return false;
        }
        for (int i2 = 0; i2 < this.mPhoneNum; i2++) {
            if (simOpInfo[i2] == 2) {
                if (simType[i2] == 1) {
                    priority[i2] = 0;
                } else if (simType[i2] == 0) {
                    priority[i2] = 1;
                }
            } else {
                priority[i2] = 2;
            }
        }
        log("priority: " + Arrays.toString(priority));
        int targetPhoneId = RadioCapabilitySwitchUtil.getHighestPriorityPhone(defDataPhoneId, priority);
        log("op01-6m: target phone: " + targetPhoneId);
        if (targetPhoneId != -1) {
            log("op01-6m: highest priority SIM determined, change!");
            setCapability(targetPhoneId);
        } else {
            log("op01-6m: can't determine highest priority SIM, no change");
        }
        if (insertedSimCount >= 2) {
            handleDefaultDataOp01MultiSims();
        }
        return true;
    }

    private boolean checkOp18CapSwitch() {
        if (capability_switch_policy != 2) {
            log("checkOp18CapSwitch: config is not policy1, do nothing");
            return true;
        }
        int[] simOpInfo = new int[this.mPhoneNum];
        int[] simType = new int[this.mPhoneNum];
        int targetSim = -1;
        int insertedSimCount = 0;
        int insertedStatus = 0;
        boolean[] op18Usim = new boolean[this.mPhoneNum];
        String capabilitySimIccid = SystemProperties.get("persist.radio.simswitch.iccid");
        String[] currIccId = new String[this.mPhoneNum];
        log("checkOp18CapSwitch start");
        for (int i = 0; i < this.mPhoneNum; i++) {
            currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                log("error: iccid not found, wait for next sub ready");
                return false;
            }
            if (!NO_SIM_VALUE.equals(currIccId[i])) {
                insertedSimCount++;
                insertedStatus |= 1 << i;
            }
        }
        log("checkOp18CapSwitch : Inserted SIM count: " + insertedSimCount + ", insertedStatus: " + insertedStatus);
        if (!RadioCapabilitySwitchUtil.getSimInfo(simOpInfo, simType, insertedStatus)) {
            return false;
        }
        int i2 = 0;
        while (i2 < this.mPhoneNum) {
            String propStr = i2 == 0 ? "gsm.sim.ril.mcc.mnc" : "gsm.sim.ril.mcc.mnc." + (i2 + 1);
            if (SystemProperties.get(propStr, UsimPBMemInfo.STRING_NOT_SET).equals("sim_lock")) {
                log("checkOp18CapSwitch : phone " + i2 + " is sim lock");
                setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "true");
            }
            i2++;
        }
        int capabilitySimId = Integer.valueOf(SystemProperties.get(PROPERTY_3G_SIM, "1")).intValue() - 1;
        log("op18: capabilitySimIccid:" + capabilitySimIccid + "capabilitySimId:" + capabilitySimId);
        for (int i3 = 0; i3 < this.mPhoneNum; i3++) {
            if (simOpInfo[i3] == 4) {
                op18Usim[i3] = true;
            }
        }
        log("op18Usim: " + Arrays.toString(op18Usim));
        for (int i4 = 0; i4 < this.mPhoneNum; i4++) {
            if (capabilitySimIccid.equals(currIccId[i4])) {
                if (op18Usim[i4]) {
                    targetSim = i4;
                } else {
                    for (int j = 0; j < this.mPhoneNum; j++) {
                        if (op18Usim[j]) {
                            targetSim = j;
                        }
                    }
                }
                log("op18: i = " + i4 + ", currIccId : " + currIccId[i4] + ", targetSim : " + targetSim);
                if (op18Usim[i4]) {
                    log("op18-C1: cur is old op18 USIM, no change");
                    if (capabilitySimId != i4) {
                        log("op18-C1a: old op18 SIM change slot, change!");
                        setCapability(i4);
                    }
                    setDefaultData(i4);
                    setDataEnabled(i4, true);
                    return true;
                }
                if (targetSim == -1) {
                    setDefaultData(capabilitySimId);
                    setDataEnabled(capabilitySimId, true);
                    log("op18-C6: no higher priority SIM, no cahnge");
                    return true;
                }
                log("op18-C2: cur is not op18 SIM but find op18 SIM, change!");
                setCapability(targetSim);
                setDefaultData(targetSim);
                setDataEnabled(targetSim, true);
                return true;
            }
        }
        if (op18Usim[capabilitySimId]) {
            targetSim = capabilitySimId;
        } else {
            for (int i5 = 0; i5 < this.mPhoneNum; i5++) {
                if (op18Usim[i5]) {
                    targetSim = i5;
                }
            }
        }
        log("op18: target SIM :" + targetSim);
        if (op18Usim[capabilitySimId]) {
            log("op18-C7: cur is new op18 USIM, no change");
            setDefaultData(capabilitySimId);
            setDataEnabled(capabilitySimId, true);
            return true;
        }
        if (targetSim == -1) {
            setDefaultData(capabilitySimId);
            setDataEnabled(capabilitySimId, true);
            log("op18-C12: no higher priority SIM, no cahnge");
            return true;
        }
        log("op18-C8: find op18 USIM, change!");
        setCapability(targetSim);
        setDefaultData(targetSim);
        setDataEnabled(targetSim, true);
        return true;
    }

    private void subSelectorForOp09(Intent intent) {
        if (intent == null) {
            log("OP09: intent is null, ignore!");
            return;
        }
        int detectedType = intent.getIntExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, -1);
        int detectedCount = intent.getIntExtra("simCount", -1);
        SubscriptionController subController = SubscriptionController.getInstance();
        int[] subList = subController.getActiveSubIdList();
        int defaultSub = subController.getDefaultDataSubId();
        int insertedSimCount = subList.length;
        boolean pplEnabled = isPplEnabled();
        log("OP09: Inserted SIM count: " + insertedSimCount + " Intent count: " + detectedCount + " detectedType = " + detectedType + " defaultSub = " + defaultSub + " pplEnabled = " + pplEnabled);
        if (detectedCount > -1 && detectedCount != insertedSimCount) {
            log("OP09: Intent count and latest sub count not match, ignore and wait next.");
            return;
        }
        boolean anySimInserted = false;
        int i = 0;
        while (true) {
            if (i >= this.mPhoneNum) {
                break;
            }
            if (isSimInserted(i)) {
                anySimInserted = true;
                break;
            }
            i++;
        }
        if (!anySimInserted) {
            log("OP09: no sims from iccid properties, revise insertedSimCount.");
            insertedSimCount = 0;
        }
        if (insertedSimCount == 0) {
            log("OP09 C0: No SIM inserted, do nothing.");
        } else if (insertedSimCount == 1) {
            switch (detectedType) {
                case 1:
                    int phoneId = subController.getPhoneId(subList[0]);
                    log("OP09 C1: a new sim detected, Set Default slot: " + phoneId);
                    setDefaultData(phoneId);
                    if (pplEnabled) {
                        setDataEnabled(phoneId, false);
                    } else {
                        setDataEnabled(phoneId, true);
                    }
                    setCapabilityIfNeeded(phoneId);
                    break;
                case 2:
                    if (subList[0] == defaultSub) {
                        log("OP09 C2.2: a sim left and it's default sub, do nothing.");
                    } else {
                        int phoneId2 = subController.getPhoneId(subList[0]);
                        log("OP09 C2.1: left a sim not default, Set Default: " + phoneId2);
                        if (pplEnabled) {
                            setDataEnabled(phoneId2, false);
                        } else {
                            setDataEnabled(phoneId2, getDataEnabledFromSetting(defaultSub));
                        }
                        setDefaultData(phoneId2);
                        setCapabilityIfNeeded(phoneId2);
                    }
                    break;
                case 3:
                    if (subList[0] == defaultSub) {
                        log("OP09 C3.2: a sim left with default data on it, do nothing.");
                    } else {
                        log("OP09 C3.1: a sim left and is not default data sim, set it as default data sim.");
                        int phoneId3 = subController.getPhoneId(subList[0]);
                        if (pplEnabled) {
                            setDataEnabled(phoneId3, false);
                        } else {
                            setDataEnabled(phoneId3, getDataEnabledFromSetting(defaultSub));
                        }
                        setDefaultData(phoneId3);
                    }
                    break;
                case 4:
                    log("OP09 C4: a sim exist and is old, do nothing.");
                    break;
                default:
                    log("OP09 C5: ignore unknown detectedType: " + detectedType);
                    break;
            }
        } else if (insertedSimCount == 2) {
            switch (detectedType) {
                case 1:
                    int newSimStatus = intent.getIntExtra("newSIMSlot", 0);
                    log("OP09 C6.0: newSimStatus = " + newSimStatus + " subList[0] = " + subList[0] + " subList[1] = " + subList[1]);
                    if (defaultSub == subList[0]) {
                        log("OP09 C6.1: data on old sim1, turn off SIM2, set capability to SIM1.");
                        if (newSimStatus == 3) {
                            if (pplEnabled) {
                                setDataEnabled(0, false);
                            } else {
                                setDataEnabled(0, true);
                            }
                        }
                        setDataEnabled(1, false);
                        setCapabilityIfNeeded(0);
                    } else if (defaultSub != subList[1]) {
                        log("OP09 C6.3: new + new or new + old, no default, set sim1 as default.");
                        if (newSimStatus == 1 || newSimStatus == 2) {
                            if (pplEnabled) {
                                setDataEnabled(0, false);
                            } else {
                                setDataEnabled(0, getDataEnabledFromSetting(defaultSub));
                            }
                        } else if (pplEnabled) {
                            setDataEnabled(0, false);
                        } else {
                            setDataEnabled(0, true);
                        }
                        setDataEnabled(1, false);
                        setDefaultData(0);
                        setCapabilityIfNeeded(0);
                    } else {
                        log("OP09 C6.2: data on old sim2, turn off SIM1, set capability to SIM2.");
                        if (newSimStatus == 3) {
                            if (pplEnabled) {
                                setDataEnabled(1, false);
                            } else {
                                setDataEnabled(1, true);
                            }
                        }
                        setDataEnabled(0, false);
                        setCapabilityIfNeeded(1);
                    }
                    break;
                case 2:
                    log("OP09 C7: a sim removed and two sim left, not support yet!");
                    break;
                case 3:
                    log("OP09 C8: two sims swap slot location, do nothing.");
                    break;
                case 4:
                    log("OP09 C9: two sims exist and are old, do nothing.");
                    break;
                default:
                    log("OP09 C10: ignore unknown detectedType: " + detectedType);
                    break;
            }
        } else {
            log("OP09 C11: sim count bigger than 2, not support yet!");
        }
        updateDefaultDataProperty();
        updateDataEnableProperty();
    }

    private void updateDefaultDataProperty() {
        int defaultSub = SubscriptionController.getInstance().getDefaultDataSubId();
        SubscriptionInfo subInfo = SubscriptionController.getInstance().getActiveSubscriptionInfo(defaultSub, this.mContext.getOpPackageName());
        if (subInfo != null) {
            String defaultIccid = subInfo.getIccId();
            log("updateDefaultDataProperty, from subscription iccid = " + defaultIccid);
            String iccidFromProperty = SystemProperties.get(PROPERTY_DEFAULT_DATA_ICCID);
            log("updateDefaultDataProperty, iccidFromProperty:" + iccidFromProperty);
            if (TextUtils.equals(iccidFromProperty, defaultIccid)) {
                return;
            }
            SystemProperties.set(PROPERTY_DEFAULT_DATA_ICCID, defaultIccid);
            return;
        }
        loge("updateDefaultDataProperty, subInfo is null! defaultSub:" + defaultSub);
    }

    private void setDataEnabled(int phoneId, boolean enable) {
        log("setDataEnabled: phoneId=" + phoneId + ", enable=" + enable);
        TelephonyManager telephony = TelephonyManager.getDefault();
        if (telephony == null) {
            return;
        }
        if (phoneId == -1) {
            telephony.setDataEnabled(enable);
            return;
        }
        if (!enable) {
            int phoneSubId = PhoneFactory.getPhone(phoneId).getSubId();
            log("Set Sub" + phoneSubId + " to disable");
            telephony.setDataEnabled(phoneSubId, enable);
            return;
        }
        for (int i = 0; i < this.mPhoneNum; i++) {
            int phoneSubId2 = PhoneFactory.getPhone(i).getSubId();
            if (i != phoneId) {
                log("Set Sub" + phoneSubId2 + " to disable");
                telephony.setDataEnabled(phoneSubId2, false);
            } else {
                log("Set Sub" + phoneSubId2 + " to enable");
                telephony.setDataEnabled(phoneSubId2, true);
            }
        }
    }

    private void updateDataEnableProperty() {
        String dataOnIccid;
        TelephonyManager telephony = TelephonyManager.getDefault();
        boolean dataEnabled = false;
        for (int i = 0; i < this.mPhoneNum; i++) {
            int subId = PhoneFactory.getPhone(i).getSubId();
            if (telephony != null) {
                dataEnabled = telephony.getDataEnabled(subId);
            }
            if (dataEnabled) {
                dataOnIccid = SystemProperties.get(this.PROPERTY_ICCID[i], "0");
            } else {
                dataOnIccid = "0";
            }
            log("setUserDataProperty:" + dataOnIccid);
            TelephonyManager.getDefault();
            TelephonyManager.setTelephonyProperty(i, PROPERTY_MOBILE_DATA_ENABLE, dataOnIccid);
        }
    }

    private void setDefaultData(int phoneId) {
        SubscriptionController subController = SubscriptionController.getInstance();
        int sub = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        int currSub = SubscriptionManager.getDefaultDataSubscriptionId();
        this.mPrevDefaultDataSubId = currSub;
        setLastValidDefaultDataSub(currSub);
        log("setDefaultData: " + sub + ", current default sub:" + currSub + "last valid default sub:" + this.mLastValidDefaultDataSubId);
        if (sub != currSub && sub >= -1) {
            subController.setDefaultDataSubIdWithoutCapabilitySwitch(sub);
        } else {
            log("setDefaultData: default data unchanged");
        }
    }

    private void turnOffNewSimData(Intent intent) {
        if (TelephonyManager.getDefault().getSimCount() == 1 && !mOperatorSpec.equals(OPERATOR_OP09)) {
            log("[turnOffNewSimData] Single SIM project, don't change data enable setting");
            return;
        }
        int detectedType = intent == null ? getSimStatus() : intent.getIntExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 0);
        log("turnOffNewSimData detectedType = " + detectedType);
        if (detectedType != 1) {
            return;
        }
        int newSimSlot = intent == null ? getNewSimSlot() : intent.getIntExtra("newSIMSlot", 0);
        log("newSimSlot = " + newSimSlot);
        log("default iccid = " + SystemProperties.get(PROPERTY_DEFAULT_DATA_ICCID));
        for (int i = 0; i < this.mPhoneNum; i++) {
            if (((1 << i) & newSimSlot) != 0) {
                String defaultIccid = SystemProperties.get(PROPERTY_DEFAULT_DATA_ICCID);
                String newSimIccid = SystemProperties.get(this.PROPERTY_ICCID[i]);
                if (!newSimIccid.equals(NO_SIM_VALUE) && !newSimIccid.equals(defaultIccid)) {
                    log("Detect NEW SIM, turn off phone " + i + " data.");
                    setDataEnabled(i, false);
                }
            }
        }
    }

    private boolean setCapabilityIfNeeded(int phoneId) {
        int[] simOpInfo = new int[this.mPhoneNum];
        int[] simType = new int[this.mPhoneNum];
        int insertedState = 0;
        int insertedSimCount = 0;
        int op01SimCount = 0;
        int op02SimCount = 0;
        String[] currIccId = new String[this.mPhoneNum];
        if (isOP09ASupport()) {
            return true;
        }
        if ("OM".equals(mOperatorSpec) && RadioCapabilitySwitchUtil.isPS2SupportLTE()) {
            for (int i = 0; i < this.mPhoneNum; i++) {
                currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
                if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                    log("error: iccid not found, wait for next sub ready");
                    return false;
                }
                if (!NO_SIM_VALUE.equals(currIccId[i])) {
                    insertedSimCount++;
                    insertedState |= 1 << i;
                }
            }
            log("setCapabilityIfNeeded : Inserted SIM count: " + insertedSimCount + ", insertedStatus: " + insertedState);
            if (insertedSimCount == 0 || !RadioCapabilitySwitchUtil.getSimInfo(simOpInfo, simType, insertedState)) {
                return false;
            }
            for (int i2 = 0; i2 < this.mPhoneNum; i2++) {
                if (2 == simOpInfo[i2]) {
                    op01SimCount++;
                } else if (3 == simOpInfo[i2]) {
                    op02SimCount++;
                }
            }
            if ((op02SimCount == 1 && insertedSimCount == 1) || (op02SimCount == 2 && insertedSimCount == 2)) {
                return false;
            }
        }
        return setCapability(phoneId);
    }

    private boolean setCapability(int phoneId) {
        ITelephony iTel;
        ITelephonyEx iTelEx;
        if (isOP09CSupport() && !isCanSwitch()) {
            log("setCapability: isCanSwitch = false");
            return true;
        }
        int[] phoneRat = new int[this.mPhoneNum];
        boolean isSwitchSuccess = true;
        String curr3GSim = SystemProperties.get(PROPERTY_3G_SIM, UsimPBMemInfo.STRING_NOT_SET);
        log("setCapability: " + phoneId + ", current 3G Sim = " + curr3GSim);
        try {
            iTel = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            iTelEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        } catch (RemoteException ex) {
            log("[Exception]Set phone rat fail!!! MaxPhoneRat=" + phoneRat[phoneId]);
            ex.printStackTrace();
            isSwitchSuccess = false;
        }
        if (iTel == null || iTelEx == null) {
            loge("Can not get phone service");
            return false;
        }
        int currRat = iTel.getRadioAccessFamily(phoneId, "phone");
        log("Current phoneRat:" + currRat);
        RadioAccessFamily[] rat = new RadioAccessFamily[this.mPhoneNum];
        for (int i = 0; i < this.mPhoneNum; i++) {
            if (phoneId == i) {
                phoneRat[i] = ProxyController.getInstance().getMaxRafSupported();
            } else {
                phoneRat[i] = ProxyController.getInstance().getMinRafSupported();
            }
            rat[i] = new RadioAccessFamily(i, phoneRat[i]);
        }
        if (WorldPhoneUtil.isWorldPhoneSwitching()) {
            log("world mode switching, don't trigger sim switch.");
            isSwitchSuccess = false;
        } else if (!iTelEx.setRadioCapability(rat)) {
            log("Set phone rat fail!!! MaxPhoneRat=" + phoneRat[phoneId]);
            isSwitchSuccess = false;
        }
        if (!isSwitchSuccess) {
            if (WorldPhoneUtil.isWorldPhoneSwitching()) {
                log("world mode switching!");
                registerWorldModeReceiver();
                this.mPhoneId = phoneId;
            }
        } else if (this.mHasRegisterWorldModeReceiver) {
            unRegisterWorldModeReceiver();
            this.mPhoneId = -1;
        }
        return isSwitchSuccess;
    }

    private boolean checkOp02CapSwitch(int policy) {
        int[] simOpInfo = new int[this.mPhoneNum];
        int[] simType = new int[this.mPhoneNum];
        int insertedStatus = 0;
        int insertedSimCount = 0;
        String[] currIccId = new String[this.mPhoneNum];
        ArrayList<Integer> usimIndexList = new ArrayList<>();
        ArrayList<Integer> simIndexList = new ArrayList<>();
        ArrayList<Integer> op02IndexList = new ArrayList<>();
        ArrayList<Integer> otherIndexList = new ArrayList<>();
        for (int i = 0; i < this.mPhoneNum; i++) {
            currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                log("error: iccid not found, wait for next sub ready");
                return false;
            }
            if (!NO_SIM_VALUE.equals(currIccId[i])) {
                insertedSimCount++;
                insertedStatus |= 1 << i;
            }
        }
        log("checkOp02CapSwitch : Inserted SIM count: " + insertedSimCount + ", insertedStatus: " + insertedStatus);
        if (RadioCapabilitySwitchUtil.isAnySimLocked(this.mPhoneNum)) {
            log("checkOp02CapSwitch: sim locked");
            if (policy == 0) {
                setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "true");
            } else {
                setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED_ROAMING, "true");
            }
        } else {
            log("checkOp02CapSwitch: no sim locked");
            if (policy == 0) {
                setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED, "false");
            } else {
                setNeedWaitUnlock(NEED_TO_WAIT_UNLOCKED_ROAMING, "false");
            }
        }
        if (!RadioCapabilitySwitchUtil.getSimInfo(simOpInfo, simType, insertedStatus)) {
            return false;
        }
        if (this.mAirplaneModeOn) {
            log("DataSubSelector for OP02: do not switch because of mAirplaneModeOn");
            if (policy == 0) {
                this.mIsNeedWaitAirplaneModeOff = true;
            } else if (1 == policy) {
                this.mIsNeedWaitAirplaneModeOffRoaming = true;
            }
        }
        for (int i2 = 0; i2 < this.mPhoneNum; i2++) {
            if (3 == simOpInfo[i2]) {
                op02IndexList.add(Integer.valueOf(i2));
            } else {
                otherIndexList.add(Integer.valueOf(i2));
            }
            if (1 == simType[i2]) {
                usimIndexList.add(Integer.valueOf(i2));
            } else {
                simIndexList.add(Integer.valueOf(i2));
            }
        }
        log("usimIndexList size = " + usimIndexList.size());
        log("op02IndexList size = " + op02IndexList.size());
        log("policy = " + policy);
        SystemProperties.set(USER_SELECT_DEFAULT_DATA, Boolean.toString(false));
        switch (policy) {
            case 0:
                executeOp02HomePolicy(usimIndexList, op02IndexList, simIndexList);
                return true;
            case 1:
                executeOp02RoamingPolocy(usimIndexList, op02IndexList, otherIndexList);
                return true;
            default:
                loge("Should NOT be here");
                return true;
        }
    }

    private void executeOp02HomePolicy(ArrayList<Integer> usimIndexList, ArrayList<Integer> op02IndexList, ArrayList<Integer> simIndexList) {
        int phoneId = -1;
        int op02CardCount = 0;
        log("Enter op02HomePolicy");
        if (usimIndexList.size() >= 2) {
            for (int i = 0; i < usimIndexList.size(); i++) {
                if (op02IndexList.contains(usimIndexList.get(i))) {
                    op02CardCount++;
                    phoneId = i;
                }
            }
            if (op02CardCount == 1) {
                log("C2: Only one OP02 USIM inserted, set default data to phone: " + phoneId);
                setCapability(phoneId);
                setDefaultData(phoneId);
                handleDataEnableForOp02(2);
                return;
            }
            log("C3: More than two OP02 cards or other operator cards inserted,Display dialog");
            this.mPrevDefaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
            log("mPrevDefaultDataSubId:" + this.mPrevDefaultDataSubId);
            SystemProperties.set(USER_SELECT_DEFAULT_DATA, Boolean.toString(true));
            return;
        }
        if (usimIndexList.size() == 1) {
            int phoneId2 = usimIndexList.get(0).intValue();
            log("C4: Only one USIM inserted, set default data to phone: " + phoneId2);
            setCapability(phoneId2);
            setDefaultData(phoneId2);
            handleDataEnableForOp02(2);
            return;
        }
        for (int i2 = 0; i2 < simIndexList.size(); i2++) {
            if (op02IndexList.contains(simIndexList.get(i2))) {
                op02CardCount++;
                phoneId = i2;
            }
        }
        if (op02CardCount == 1) {
            log("C5: OP02 card + otehr op cards inserted, set default data to phone: " + phoneId);
            setCapability(phoneId);
            setDefaultData(phoneId);
            handleDataEnableForOp02(2);
            return;
        }
        log("C6: More than two OP02 cards or other operator cards inserted,display dialog");
        this.mPrevDefaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        SystemProperties.set(USER_SELECT_DEFAULT_DATA, Boolean.toString(true));
    }

    private void executeOp02RoamingPolocy(ArrayList<Integer> usimIndexList, ArrayList<Integer> op02IndexList, ArrayList<Integer> otherIndexList) {
        int phoneId = -1;
        int usimCount = 0;
        log("Enter op02RoamingPolocy");
        if (this.mContext == null) {
            loge("mContext is null, return");
        }
        if (op02IndexList.size() >= 2) {
            for (int i = 0; i < op02IndexList.size(); i++) {
                if (usimIndexList.contains(op02IndexList.get(i))) {
                    usimCount++;
                    phoneId = i;
                }
            }
            if (usimCount == 1) {
                log("C2: Only one OP02 USIM inserted, set default data to phone: " + phoneId);
                setCapability(phoneId);
                setDefaultData(phoneId);
                handleDataEnableForOp02(2);
            } else {
                log("C3: More than two USIM cards or other SIM cards inserted, show dialog");
                this.mPrevDefaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
                SystemProperties.set(USER_SELECT_DEFAULT_DATA, Boolean.toString(true));
            }
        } else if (op02IndexList.size() == 1) {
            int phoneId2 = op02IndexList.get(0).intValue();
            log("C4: OP02 card + other cards inserted, set default data to phone: " + phoneId2);
            setCapability(phoneId2);
            setDefaultData(phoneId2);
            handleDataEnableForOp02(2);
        } else {
            for (int i2 = 0; i2 < otherIndexList.size(); i2++) {
                if (usimIndexList.contains(otherIndexList.get(i2))) {
                    usimCount++;
                    phoneId = i2;
                }
            }
            if (usimCount == 1) {
                log("C5: Other USIM + other SIM cards inserted, set default data to phone: " + phoneId);
                setCapability(phoneId);
                setDefaultData(phoneId);
                handleDataEnableForOp02(2);
            } else {
                log("C6: More than two USIM cards or all SIM cards inserted, diaplay dialog");
                this.mPrevDefaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
                SystemProperties.set(USER_SELECT_DEFAULT_DATA, Boolean.toString(true));
            }
        }
        SharedPreferences preferenceRoaming = this.mContext.getSharedPreferences(FIRST_TIME_ROAMING, 0);
        SharedPreferences.Editor editorRoaming = preferenceRoaming.edit();
        editorRoaming.putBoolean(NEED_TO_EXECUTE_ROAMING, false);
        if (editorRoaming.commit()) {
            return;
        }
        loge("write sharedPreference ERROR");
    }

    private void handleDataEnableForOp02(int insertedSimCount) {
        int nonDefaultPhoneId;
        log("handleDataEnableForOp02: insertedSimCount = " + insertedSimCount);
        TelephonyManager telephony = TelephonyManager.getDefault();
        if (telephony == null) {
            loge("TelephonyManager.getDefault() return null");
            return;
        }
        int nDefaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (!SubscriptionManager.isValidSubscriptionId(this.mPrevDefaultDataSubId)) {
            if (SubscriptionManager.isValidSubscriptionId(this.mLastValidDefaultDataSubId)) {
                boolean enable = getDataEnabledFromSetting(this.mLastValidDefaultDataSubId);
                log("setEnable by lastValidDataSub's setting = " + enable);
                setDataEnabled(SubscriptionManager.getPhoneId(nDefaultDataSubId), enable);
                return;
            }
            setDataEnabled(SubscriptionManager.getPhoneId(nDefaultDataSubId), true);
            return;
        }
        if (!SubscriptionManager.isValidSubscriptionId(this.mPrevDefaultDataSubId) || !SubscriptionManager.isValidSubscriptionId(nDefaultDataSubId)) {
            return;
        }
        if (this.mPrevDefaultDataSubId != nDefaultDataSubId) {
            if (getDataEnabledFromSetting(this.mPrevDefaultDataSubId)) {
                setDataEnabled(SubscriptionManager.getPhoneId(nDefaultDataSubId), true);
                return;
            } else {
                setDataEnabled(SubscriptionManager.getPhoneId(nDefaultDataSubId), false);
                return;
            }
        }
        if (insertedSimCount != 2) {
            return;
        }
        if (SubscriptionManager.getPhoneId(nDefaultDataSubId) == 0) {
            nonDefaultPhoneId = 1;
        } else {
            nonDefaultPhoneId = 0;
        }
        if (!getDataEnabledFromSetting(nDefaultDataSubId)) {
            return;
        }
        setDataEnabled(nonDefaultPhoneId, false);
    }

    private boolean getDataEnabledFromSetting(int nSubId) {
        boolean retVal;
        log("getDataEnabledFromSetting, nSubId = " + nSubId);
        if (this.mContext == null || this.mContext.getContentResolver() == null) {
            log("getDataEnabledFromSetting, context or resolver is null, return");
            return false;
        }
        try {
            retVal = Settings.Global.getInt(this.mContext.getContentResolver(), new StringBuilder().append("mobile_data").append(nSubId).toString()) != 0;
        } catch (Settings.SettingNotFoundException e) {
            retVal = false;
        }
        log("getDataEnabledFromSetting, retVal = " + retVal);
        return retVal;
    }

    private boolean isNeedWaitUnlock(String prop) {
        return SystemProperties.getBoolean(prop, false);
    }

    private void setNeedWaitUnlock(String prop, String value) {
        SystemProperties.set(prop, value);
    }

    private boolean isNeedWaitImsi(String prop) {
        return SystemProperties.getBoolean(prop, false);
    }

    private void setNeedWaitImsi(String prop, String value) {
        SystemProperties.set(prop, value);
    }

    private void setSimStatus(Intent intent) {
        if (intent == null) {
            log("setSimStatus, intent is null => return");
            return;
        }
        log("setSimStatus");
        int detectedType = intent.getIntExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 0);
        SystemProperties.set(SIM_STATUS, Integer.toString(detectedType));
    }

    private void resetSimStatus() {
        log("resetSimStatus");
        SystemProperties.set(SIM_STATUS, UsimPBMemInfo.STRING_NOT_SET);
    }

    private int getSimStatus() {
        log("getSimStatus");
        return SystemProperties.getInt(SIM_STATUS, 0);
    }

    private void setNewSimSlot(Intent intent) {
        if (intent == null) {
            log("setNewSimSlot, intent is null => return");
            return;
        }
        log("setNewSimSlot");
        int newSimStatus = intent.getIntExtra("newSIMSlot", 0);
        SystemProperties.set(NEW_SIM_SLOT, Integer.toString(newSimStatus));
    }

    private void resetNewSimSlot() {
        log("resetNewSimSlot");
        SystemProperties.set(NEW_SIM_SLOT, UsimPBMemInfo.STRING_NOT_SET);
    }

    private int getNewSimSlot() {
        log("getNewSimSlot");
        return SystemProperties.getInt(NEW_SIM_SLOT, 0);
    }

    private void registerWorldModeReceiver() {
        if (this.mContext == null) {
            log("registerWorldModeReceiver, context is null => return");
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_WORLD_MODE_CHANGED");
        this.mContext.registerReceiver(this.mWorldModeReceiver, filter);
        this.mHasRegisterWorldModeReceiver = true;
    }

    private void unRegisterWorldModeReceiver() {
        if (this.mContext == null) {
            log("unRegisterWorldModeReceiver, context is null => return");
        } else {
            this.mContext.unregisterReceiver(this.mWorldModeReceiver);
            this.mHasRegisterWorldModeReceiver = false;
        }
    }

    private void log(String txt) {
        Rlog.d("DataSubSelector", txt);
    }

    private void loge(String txt) {
        Rlog.e("DataSubSelector", txt);
    }

    private void setLastValidDefaultDataSub(int subId) {
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            log("setLastValidDefaultDataSub = " + subId);
            this.mLastValidDefaultDataSubId = subId;
        } else {
            log("because invalid id to set, keep LastValidDefaultDataSub = " + this.mLastValidDefaultDataSubId);
        }
    }

    private boolean isOP09ASupport() {
        if (OPERATOR_OP09.equals(SystemProperties.get("persist.operator.optr", UsimPBMemInfo.STRING_NOT_SET))) {
            return SEGDEFAULT.equals(SystemProperties.get("persist.operator.seg", UsimPBMemInfo.STRING_NOT_SET));
        }
        return false;
    }

    private boolean isOP09Support() {
        return OPERATOR_OP09.equals(SystemProperties.get("persist.operator.optr", UsimPBMemInfo.STRING_NOT_SET));
    }

    private boolean isOP01OMSupport() {
        return SystemProperties.get("ro.cmcc_light_cust_support").equals("1");
    }

    private boolean isOP09CSupport() {
        if (OPERATOR_OP09.equals(SystemProperties.get("persist.operator.optr", UsimPBMemInfo.STRING_NOT_SET))) {
            return SEGC.equals(SystemProperties.get("persist.operator.seg", UsimPBMemInfo.STRING_NOT_SET));
        }
        return false;
    }

    private boolean isCanSwitch() {
        if (this.mAirplaneModeOn) {
            log("DataSubselector,isCanSwitch mAirplaneModeOn = " + this.mAirplaneModeOn);
            return false;
        }
        int simNum = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < simNum; i++) {
            int simState = TelephonyManager.from(this.mContext).getSimState(i);
            if (simState == 2 || simState == 3 || simState == 4 || simState == 6) {
                log("DataSubselector,sim locked ,isCanSwitch simState = " + simState);
                return false;
            }
        }
        return true;
    }

    private boolean isSimInserted(int phoneId) {
        String iccid = SystemProperties.get(this.PROPERTY_ICCID_SIM[phoneId], UsimPBMemInfo.STRING_NOT_SET);
        return (TextUtils.isEmpty(iccid) || NO_SIM_VALUE.equals(iccid)) ? false : true;
    }

    private boolean isPplEnabled() {
        try {
            IBinder binder = ServiceManager.getService("PPLAgent");
            if (binder == null) {
                return false;
            }
            IPplAgent agent = IPplAgent.Stub.asInterface(binder);
            if (agent.needLock() != 1) {
                return false;
            }
            return true;
        } catch (RemoteException e) {
            log("DataSubselector, error in get PPLAgent service.");
            return false;
        }
    }
}
