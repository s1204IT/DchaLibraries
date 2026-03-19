package com.mediatek.usp;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.util.Preconditions;
import com.mediatek.internal.R;
import com.mediatek.usp.IUspService;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UspServiceImpl extends IUspService.Stub {
    private static final String CUSTOM_PATH = "/custom/usp";
    private static final int MAX_AT_CMD_RESPONSE = 2048;
    private static final int PROP_CFG_CTRL_FLAG_CONFIG_STATE_INVALID = 3;
    private static final int PROP_CFG_CTRL_FLAG_FIRST_SIM_ONLY_DONE = 2;
    private static final int PROP_CFG_CTRL_FLAG_NOT_FIRST_BOOT = 1;
    private static final int PROP_CFG_CTRL_FLAG_POPUP_HANDLED = 4;
    private static final String PROP_CXP_CONFIG_CTRL = "persist.mtk_usp_cfg_ctrl";
    private static final String PROP_GSM_SIM_OPERATOR_NUMERIC = "gsm.sim.operator.numeric";
    private static final String PROP_PERSIST_BOOTANIM_MNC = "persist.bootanim.mnc";
    private static final String SYSTEM_PATH = "/system/usp";
    private static final String USP_INFO_FILE = "usp-info.txt";
    private static final String VENDOR_PATH = "/vendor/usp";
    private static Map<String, String> sOperatorMapInfo = new HashMap();
    private Context mContext;
    private AlertDialog mDialog;
    private PackageManager mPm;
    private TaskHandler mTaskHandler;
    private MyHandler mUiHandler;
    private final String TAG = "UspServiceImpl";
    private final boolean DEBUG = true;
    private final boolean TESTING_PURPOSE = true;
    private int mConfigState = 0;
    private List<String> mPendingEnableDisableReq = new ArrayList();
    protected BroadcastReceiver mEnableDisableRespReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] data = intent.getData().toString().split(":");
            String packageName = data[data.length - 1];
            UspServiceImpl.this.mPendingEnableDisableReq.remove(packageName);
            Log.d("UspServiceImpl", "mEnableDisableRespReceiver, got response for package name=" + packageName);
            Log.d("UspServiceImpl", "Dump mPendingEnableDisableReq List of Size:" + UspServiceImpl.this.mPendingEnableDisableReq.size() + Arrays.toString(UspServiceImpl.this.mPendingEnableDisableReq.toArray()));
            if (!UspServiceImpl.this.mPendingEnableDisableReq.isEmpty()) {
                return;
            }
            Log.d("UspServiceImpl", "mEnableDisableRespReceiver,mPendingEnableDisableReq empty So Calling rebootAndroidSystem");
            UspServiceImpl.this.mContext.unregisterReceiver(UspServiceImpl.this.mEnableDisableRespReceiver);
        }
    };
    protected BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        private static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
        private static final String INTENT_KEY_ICC_STATE = "ss";
        private static final String INTENT_VALUE_ICC_LOADED = "LOADED";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("UspServiceImpl", "BroadcastReceiver(), SIM state change, action=" + action);
            if (UspServiceImpl.this.mTaskHandler.hasMessages(3)) {
                Log.d("UspServiceImpl", "removeMessages, TaskHandler.EARLY_READ_FAILED");
                UspServiceImpl.this.mTaskHandler.removeMessages(3);
            }
            if (action == null || !action.equals(ACTION_SIM_STATE_CHANGED)) {
                return;
            }
            String newState = intent.getStringExtra(INTENT_KEY_ICC_STATE);
            Log.d("UspServiceImpl", "BroadcastReceiver(), SIM state change, new state=" + newState);
            if (!newState.equals(INTENT_VALUE_ICC_LOADED) || UspServiceImpl.this.mConfigState == 1) {
                return;
            }
            String mccMnc = UspServiceImpl.this.readMCCMNCFromProperty(false);
            if (mccMnc.length() < 5) {
                Log.d("UspServiceImpl", "Invalid mccMnc " + mccMnc);
            } else {
                String optr = UspServiceImpl.this.getOperatorPackForSim(mccMnc);
                UspServiceImpl.this.handleSwitchOperator(optr);
            }
        }
    };

    public static native int ccciEnterDeepFlight();

    public static native int ccciLeaveDeepFlight();

    public static native int freezeFrame();

    public static native int unfreezeFrame();

    static {
        sOperatorMapInfo.put("la_OP15", "Movistar");
        sOperatorMapInfo.put("OP01", "CMCC");
        sOperatorMapInfo.put("OP02", "CU");
        sOperatorMapInfo.put("OP03", "Orange");
        sOperatorMapInfo.put("OP05", "TMO EU");
        sOperatorMapInfo.put("OP06", "Vodafone");
        sOperatorMapInfo.put("OP07", "AT&T");
        sOperatorMapInfo.put("OP08", "TMO US");
        sOperatorMapInfo.put("OP09", "CT");
        sOperatorMapInfo.put("OP11", "H3G");
        sOperatorMapInfo.put("OP12", "Verizon");
        sOperatorMapInfo.put("OP15", "Telefonica");
        sOperatorMapInfo.put("OP16", "EE");
        sOperatorMapInfo.put("OP17", "DoCoMo");
        sOperatorMapInfo.put("OP18", "Reliance");
        sOperatorMapInfo.put("OP19", "Telstra");
        sOperatorMapInfo.put("OP20", "Sprint");
        sOperatorMapInfo.put("OP50", "Softbank");
        sOperatorMapInfo.put("OP100", "CSL");
        sOperatorMapInfo.put("OP101", "PCCW");
        sOperatorMapInfo.put("OP102", "SMT");
        sOperatorMapInfo.put("OP103", "SingTel");
        sOperatorMapInfo.put("OP104", "Starhub");
        sOperatorMapInfo.put("OP105", "AMX");
        sOperatorMapInfo.put("OP106", "3HK");
        sOperatorMapInfo.put("OP107", "SFR");
        sOperatorMapInfo.put("OP108", "TWN");
        sOperatorMapInfo.put("OP109", "CHT");
        sOperatorMapInfo.put("OP110", "FET");
        sOperatorMapInfo.put("OP112", "TelCel");
        sOperatorMapInfo.put("OP113", "Beeline");
        sOperatorMapInfo.put("OP114", "KT");
        sOperatorMapInfo.put("OP115", "SKT");
        sOperatorMapInfo.put("OP116", "U+");
        sOperatorMapInfo.put("OP117", "Smartfren");
        sOperatorMapInfo.put("OP118", "YTL");
        sOperatorMapInfo.put("OP119", "Natcom");
        sOperatorMapInfo.put("OP120", "Claro");
        sOperatorMapInfo.put("OP121", "Bell");
        sOperatorMapInfo.put("OP122", "AIS");
        sOperatorMapInfo.put("OP124", "APTG");
        sOperatorMapInfo.put("OP125", "DTAC");
        sOperatorMapInfo.put("OP126", "Avea");
        sOperatorMapInfo.put("OP127", "Megafon");
        sOperatorMapInfo.put("OP128", "DNA");
        sOperatorMapInfo.put("OP129", "KDDI");
        sOperatorMapInfo.put("OP130", "TIM");
        sOperatorMapInfo.put("OP131", "TrueMove");
        sOperatorMapInfo.put("OP1001", "Ericsson");
        System.loadLibrary("usp_native");
    }

    public UspServiceImpl(Context context) {
        Log.d("UspServiceImpl", "UspServiceImpl");
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing Context");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mContext.registerReceiver(this.mIntentReceiver, filter);
        this.mUiHandler = new MyHandler(this, null);
        this.mTaskHandler = null;
        this.mDialog = null;
    }

    public void start() {
        Log.d("UspServiceImpl", "start");
        HandlerThread ht = new HandlerThread("HandlerThread");
        ht.start();
        this.mTaskHandler = new TaskHandler(ht.getLooper());
        if (getConfigCtrlFlag(3, null)) {
            String opPack = SystemProperties.get("persist.operator.optr");
            if (!isOperatorValid(opPack)) {
                Log.d("UspServiceImpl", "Operator pack not valid: " + opPack);
                setConfigCtrlFlag(3, false, null);
            } else {
                Log.d("UspServiceImpl", "start reconfiguring as last config was not complete");
                startConfiguringOpPack(opPack, true);
                return;
            }
        }
        if (!getConfigCtrlFlag(1, null)) {
            this.mTaskHandler.sendMessage(this.mTaskHandler.obtainMessage(0));
        }
        String mccMnc = readMCCMNCFromProperty(true);
        if (mccMnc.length() < 5) {
            Log.d("UspServiceImpl", "Invalid mccMnc: " + mccMnc + "scheduled after some time");
            this.mTaskHandler.sendMessageDelayed(this.mTaskHandler.obtainMessage(3), 500L);
        } else {
            String optr = getOperatorPackForSim(mccMnc);
            handleSwitchOperator(optr);
        }
    }

    void firstBootConfigure() {
        Log.d("UspServiceImpl", "firstBootConfigure");
        String optr = SystemProperties.get("persist.operator.optr");
        if (optr == null || optr.length() <= 0) {
            Log.d("UspServiceImpl", "firstBootConfigure: OM config");
            enabledDisableApps("OM");
        } else {
            Log.d("UspServiceImpl", "firstBootConfigure: OP config" + optr);
            enabledDisableApps(optr);
        }
    }

    boolean getConfigCtrlFlag(int prop, String optr) {
        int propValue = SystemProperties.getInt(PROP_CXP_CONFIG_CTRL, 0);
        switch (prop) {
            case 1:
                return (propValue & 1) == 1;
            case 2:
                return (propValue & 2) == 2;
            case 3:
                return (propValue & 4) == 4;
            case 4:
                int numStored = ((-65536) & propValue) >> 16;
                String numOptrStr = optr.substring(2, optr.length());
                try {
                    int numOptr = Integer.parseInt(numOptrStr);
                    Log.d("UspServiceImpl", "saved: " + numStored + "cur: " + numOptr);
                    if (numOptr == numStored) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    Log.d("UspServiceImpl", "getConfigCtrlFlag: 2" + e.toString());
                    break;
                }
            default:
                return false;
        }
    }

    void setConfigCtrlFlag(int prop, boolean flag, String optr) {
        int propValue = SystemProperties.getInt(PROP_CXP_CONFIG_CTRL, 0);
        switch (prop) {
            case 1:
                propValue &= -2;
                if (flag) {
                    propValue |= 1;
                }
                break;
            case 2:
                propValue &= -3;
                if (flag) {
                    propValue |= 2;
                }
                break;
            case 3:
                propValue &= -5;
                if (flag) {
                    propValue |= 4;
                }
                break;
            case 4:
                if (optr != null && optr.length() >= 3) {
                    String numStr = optr.substring(2, optr.length());
                    try {
                        int num = Integer.parseInt(numStr);
                        propValue = (propValue & 65535) | (num << 16);
                    } catch (NumberFormatException e) {
                        Log.d("UspServiceImpl", "setConfigCtrlFlag: 2" + e.toString());
                    }
                }
                break;
        }
        SystemProperties.set(PROP_CXP_CONFIG_CTRL, "" + propValue);
    }

    boolean isFirstValidSimConfigured() {
        String simSwitchMode = SystemProperties.get("ro.mtk_cxp_switch_mode");
        if (simSwitchMode != null && simSwitchMode.equals("2") && getConfigCtrlFlag(2, null)) {
            return true;
        }
        return false;
    }

    void handleSwitchOperator(String optr) {
        if (isFirstValidSimConfigured()) {
            Log.d("UspServiceImpl", "isFirstValidSimConfigured: true");
            return;
        }
        if (!isOperatorValid(optr)) {
            Log.d("UspServiceImpl", "Operator pack not valid: " + optr);
            return;
        }
        if (optr.equals(getActiveOpPack())) {
            Log.d("UspServiceImpl", "same active operator: " + optr);
            String simSwitchMode = SystemProperties.get("ro.mtk_cxp_switch_mode");
            if (simSwitchMode != null && simSwitchMode.equals("2")) {
                setConfigCtrlFlag(2, true, null);
                Log.d("UspServiceImpl", "set first valid sim configured");
                return;
            }
            return;
        }
        if (this.mConfigState == 1) {
            return;
        }
        String simSwitchMode2 = SystemProperties.get("ro.mtk_cxp_switch_mode");
        if (simSwitchMode2 != null && simSwitchMode2.equals("2")) {
            setConfigCtrlFlag(2, true, null);
            Log.d("UspServiceImpl", "set first valid sim configured");
            startConfiguringOpPack(optr, false);
        } else {
            if (getConfigCtrlFlag(4, optr)) {
                return;
            }
            if (this.mDialog != null && this.mDialog.isShowing()) {
                Log.d("UspServiceImpl", "configuration dialog already being displayed");
            } else {
                new UspUserDialog(optr).showDialog();
            }
        }
    }

    public String getActiveOpPack() {
        String optr = SystemProperties.get("persist.operator.optr");
        return optr;
    }

    public String getOpPackFromSimInfo(String mccMnc) {
        if (mccMnc != null && mccMnc.length() > 0) {
            return getOperatorPackForSim(mccMnc);
        }
        return "";
    }

    public void setOpPackActive(String opPack) {
        Log.i("UspServiceImpl", "setOpPackActive" + opPack);
        String simSwitchMode = SystemProperties.get("ro.mtk_cxp_switch_mode");
        if (simSwitchMode != null && simSwitchMode.equals("2")) {
            Log.d("UspServiceImpl", "First valid sim is enabled: ");
            return;
        }
        if (!isOperatorValid(opPack)) {
            Log.d("UspServiceImpl", "Operator pack not valid: " + opPack);
        } else if (opPack.equals(getActiveOpPack())) {
            Log.d("UspServiceImpl", "same active operator: " + opPack);
        } else {
            if (this.mConfigState == 1) {
                return;
            }
            startConfiguringOpPack(opPack, false);
        }
    }

    boolean isOperatorValid(String optr) {
        if (optr == null || optr.length() <= 0) {
            Log.d("UspServiceImpl", "error in operator: " + optr);
            return false;
        }
        if (getAllOpList().contains(optr)) {
            return true;
        }
        Log.d("UspServiceImpl", "Operator not found in all op pack list");
        return false;
    }

    List<String> getAllOpList() {
        String ops = getRegionalOpPack();
        List<String> opList = new ArrayList<>();
        try {
            String[] opSplit = ops.split(" ");
            for (int count = 0; count < opSplit.length; count++) {
                if (opSplit[count] != null && opSplit[count].length() > 0) {
                    int firstUnderscoreIndex = opSplit[count].indexOf("_");
                    opList.add(opSplit[count].substring(0, firstUnderscoreIndex));
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e("UspServiceImpl", "illegal string passed to splitString: " + e.toString());
        }
        return opList;
    }

    public Map<String, String> getAllOpPackList() {
        String ops = getRegionalOpPack();
        Map<String, String> operatorMapInfo = new HashMap<>();
        try {
            String[] opSplit = ops.split(" ");
            for (int count = 0; count < opSplit.length; count++) {
                if (opSplit[count] != null && opSplit[count].length() > 0) {
                    int firstUnderscoreIndex = opSplit[count].indexOf("_");
                    operatorMapInfo.put(opSplit[count].substring(0, firstUnderscoreIndex), getOperatorNameFromPack(opSplit[count].substring(0, firstUnderscoreIndex)));
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e("UspServiceImpl", "illegal string passed to splitString: " + e.toString());
        }
        return operatorMapInfo;
    }

    private String getOperatorPackForSim(String mccMnc) {
        int mccMncNum;
        String[] operatorList;
        try {
            mccMncNum = Integer.parseInt(mccMnc);
            operatorList = Resources.getSystem().getStringArray(R.array.operator_map_list);
        } catch (Resources.NotFoundException | IndexOutOfBoundsException | NumberFormatException e) {
            Log.e("UspServiceImpl", "getOperatorPackForSim Exception: " + e.toString());
        }
        for (String item : operatorList) {
            String[] opSplit = item.split("\\s*,\\s*");
            if (mccMncNum >= Integer.parseInt(opSplit[0]) && mccMncNum <= Integer.parseInt(opSplit[1])) {
                Log.d("UspServiceImpl", "getOperatorPackForSim optr: " + opSplit[2]);
                return "OP" + opSplit[2];
            }
            Log.d("UspServiceImpl", "getOperatorPackForSim optr NOT FOUND");
            return "";
        }
        Log.d("UspServiceImpl", "getOperatorPackForSim optr NOT FOUND");
        return "";
    }

    private void startConfiguringOpPack(String opPack, boolean isReconfig) {
        Log.d("UspServiceImpl", "startConfiguringOpPack: " + opPack);
        this.mConfigState = 1;
        this.mUiHandler.sendMessage(this.mUiHandler.obtainMessage(0, Boolean.valueOf(isReconfig)));
        this.mTaskHandler.sendMessage(this.mTaskHandler.obtainMessage(1, opPack));
        this.mTaskHandler.sendMessageDelayed(this.mTaskHandler.obtainMessage(2, opPack), 500L);
    }

    private void runningConfigurationTask(String opPack) {
        Log.d("UspServiceImpl", "runningConfigurationTask " + opPack);
        setConfigCtrlFlag(3, true, null);
        SystemProperties.set("gsm.ril.eboot", "1");
        SystemProperties.set("cdma.ril.eboot", "1");
        sendMdPowerOffCmd();
        if (ccciEnterDeepFlight() < 0) {
            Log.d("UspServiceImpl", "ccciEnterDeepFlight failed");
        }
        setProperties(opPack);
        setMdSbpProperty(opPack);
        enabledDisableApps(opPack);
    }

    private String sendMdPowerOffCmd() {
        String atCmd = new String("AT+EPOF\r\n");
        try {
            TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            byte[] rawData = atCmd.getBytes();
            byte[] cmdByte = new byte[rawData.length + 1];
            byte[] cmdResp = new byte[2048];
            System.arraycopy(rawData, 0, cmdByte, 0, rawData.length);
            cmdByte[cmdByte.length - 1] = 0;
            Log.d("UspServiceImpl", "sendMdPowerOffCmd:" + atCmd);
            int ret = telephonyManager.invokeOemRilRequestRaw(cmdByte, cmdResp);
            if (ret != -1) {
                cmdResp[ret] = 0;
                return new String(cmdResp);
            }
            return "";
        } catch (NullPointerException ee) {
            ee.printStackTrace();
            return "";
        }
    }

    private void showWaitingScreen(boolean isReconfig) {
        AlertDialog dialog = new AlertDialog(this.mContext) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                return true;
            }

            @Override
            public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                return true;
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                return true;
            }

            @Override
            public boolean dispatchTrackballEvent(MotionEvent ev) {
                return true;
            }

            @Override
            public boolean dispatchGenericMotionEvent(MotionEvent ev) {
                return true;
            }

            @Override
            public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
                return true;
            }
        };
        if (isReconfig) {
            dialog.setMessage(this.mContext.getResources().getString(R.string.reconfig_dialog_message));
        } else {
            dialog.setMessage(this.mContext.getResources().getString(R.string.reboot_dialog_message));
        }
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setType(2010);
        dialog.getWindow().getAttributes().privateFlags |= 16;
        dialog.show();
        Log.d("UspServiceImpl", "showing WaitingScreen");
    }

    private void freeze() {
        this.mUiHandler.sendMessageDelayed(this.mUiHandler.obtainMessage(1), 500L);
    }

    private void rebootAndroidSystem() {
        int i = 0;
        while (true) {
            if (i >= 25) {
                break;
            }
            try {
                if (this.mPendingEnableDisableReq.isEmpty() && !this.mUiHandler.hasMessages(1)) {
                    break;
                }
                if (this.mUiHandler.hasMessages(1)) {
                    Log.d("UspServiceImpl", "FREEZE_FRAME still not handled, so wait for .5 sec");
                }
                Thread.sleep(500L);
                if (i == 24) {
                    Log.e("UspServiceImpl", "Enable Disable May Have Not Completed");
                }
                i++;
            } catch (Exception e) {
                Log.d("UspServiceImpl", "when sleep exception happened");
            }
        }
        this.mConfigState = 2;
        if (ccciLeaveDeepFlight() < 0) {
            Log.d("UspServiceImpl", "ccciLeaveDeepFlight failed");
        }
        SystemProperties.set("persist.mtk_usp_native_start", "1");
    }

    public void unfreezeScreen() {
        if (getConfigCtrlFlag(3, null)) {
            Log.e("UspServiceImpl", "unfreezeScreen during configuration, so skipped");
        } else {
            if (unfreezeFrame() >= 0) {
                return;
            }
            Log.e("UspServiceImpl", "UNFREEZING FRAME FAILED.....WE ARE DEAD :(");
        }
    }

    private String readMCCMNCFromProperty(boolean isFromBootanim) {
        String value = readMCCMNCFromPropertyForTesting(isFromBootanim);
        Log.d("UspServiceImpl", "readMCCMNCFromPropertyForTesting " + value);
        return value;
    }

    private String readMCCMNCFromPropertyForTesting(boolean isFromBootanim) {
        String dummyMccMnc = SystemProperties.get("persist.simulate_cxp_sim");
        if (isFromBootanim) {
            String mccMnc = SystemProperties.get(PROP_PERSIST_BOOTANIM_MNC);
            if (mccMnc != null && mccMnc.length() > 4) {
                Log.d("UspServiceImpl", "read mcc mnc property from boot anim: persist.bootanim.mnc");
                return (dummyMccMnc == null || dummyMccMnc.length() <= 4) ? mccMnc : dummyMccMnc;
            }
        } else {
            String mccMnc2 = SystemProperties.get(PROP_GSM_SIM_OPERATOR_NUMERIC);
            if (mccMnc2 != null && mccMnc2.length() > 4) {
                Log.d("UspServiceImpl", "read mcc mnc property from gsm.sim.operator.numeric");
                return (dummyMccMnc == null || dummyMccMnc.length() <= 4) ? mccMnc2 : dummyMccMnc;
            }
        }
        Log.d("UspServiceImpl", "failed to read mcc mnc from property");
        return "";
    }

    private class UspUserDialog implements DialogInterface.OnClickListener {
        private String mOptr;

        UspUserDialog(String optr) {
            this.mOptr = optr;
        }

        void showDialog() {
            String content = Resources.getSystem().getString(R.string.usp_config_confirm);
            String operatorName = UspServiceImpl.this.getOperatorNameFromPack(this.mOptr);
            StringBuilder message = new StringBuilder("[" + operatorName + "] " + content);
            UspServiceImpl.this.mDialog = new AlertDialog.Builder(UspServiceImpl.this.mContext).setMessage(message.toString()).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).create();
            UspServiceImpl.this.mDialog.getWindow().setType(2010);
            UspServiceImpl.this.mDialog.getWindow().getAttributes().privateFlags |= 16;
            UspServiceImpl.this.mDialog.setCanceledOnTouchOutside(false);
            UspServiceImpl.this.mDialog.show();
            Log.d("UspServiceImpl", "showDialog " + UspServiceImpl.this.mDialog);
        }

        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            if (-1 == whichButton) {
                Log.d("UspServiceImpl", "Click for yes");
                UspServiceImpl.this.startConfiguringOpPack(this.mOptr, false);
            }
            UspServiceImpl.this.setConfigCtrlFlag(4, true, this.mOptr);
            UspServiceImpl.this.mDialog.dismiss();
            UspServiceImpl.this.mDialog = null;
        }
    }

    private String getOperatorNameFromPack(String optr) {
        String cxpPack = SystemProperties.get("ro.mtk_carrierexpress_pack");
        if (sOperatorMapInfo.containsKey(cxpPack + "_" + optr)) {
            Log.d("UspServiceImpl", "getOperatorNameFromPack for optr: " + cxpPack + "_" + optr);
            return sOperatorMapInfo.get(cxpPack + "_" + optr);
        }
        if (sOperatorMapInfo.containsKey(optr)) {
            Log.d("UspServiceImpl", "getOperatorNameFromPack for optr: " + optr);
            return sOperatorMapInfo.get(optr);
        }
        return new String("Unknown");
    }

    private void setMdSbpProperty(String optr) {
        String val = new String(optr.substring(2, optr.length()));
        Log.d("UspServiceImpl", "setMdSbpProperty value: " + val);
        SystemProperties.set("persist.mtk_usp_md_sbp_code", val);
    }

    private void setProperties(String optr) {
        File customGlobalDir;
        if (new File(CUSTOM_PATH).exists()) {
            customGlobalDir = new File(CUSTOM_PATH);
        } else if (new File(VENDOR_PATH).exists()) {
            customGlobalDir = new File(VENDOR_PATH);
        } else {
            Log.e("UspServiceImpl", "none of custom/usp or vendor/usp exists");
            return;
        }
        String propFileName = "usp-content-" + optr + ".txt";
        File customPropFile = new File(customGlobalDir, propFileName);
        List<String> opPropertyList = readFromFile(customPropFile, "[Property-start]", "[Property-end]");
        for (int i = 0; i < opPropertyList.size(); i++) {
            String key = getKey(opPropertyList.get(i).trim());
            String value = getValue(opPropertyList.get(i).trim());
            Log.d("UspServiceImpl", "setting property " + key + "  TO  " + value);
            set(this.mContext, key, value);
        }
    }

    private String getRegionalOpPack() {
        File customGlobalDir;
        Log.d("UspServiceImpl", "getRegionalOpPack ");
        if (new File(CUSTOM_PATH).exists()) {
            customGlobalDir = new File(CUSTOM_PATH);
        } else if (new File(VENDOR_PATH).exists()) {
            customGlobalDir = new File(VENDOR_PATH);
        } else {
            Log.e("UspServiceImpl", "none of custom/usp or vendor/usp exists");
            return "";
        }
        File customFile = new File(customGlobalDir, USP_INFO_FILE);
        List<String> data = readFromFile(customFile);
        for (int i = 0; i < data.size(); i++) {
            String key = getKey(data.get(i).trim());
            Log.d("UspServiceImpl", "MTK_REGIONAL_OP_PACK = " + key);
            if (key.equals("MTK_REGIONAL_OP_PACK")) {
                String value = getValue(data.get(i).trim());
                Log.d("UspServiceImpl", "MTK_REGIONAL_OP_PACK = " + value);
                return value;
            }
        }
        return "";
    }

    private void set(Context context, String key, String val) throws IllegalArgumentException {
        try {
            SystemProperties.set(key, val);
        } catch (IllegalArgumentException iAE) {
            throw iAE;
        } catch (Exception e) {
        }
    }

    private void enabledDisableApps(String optr) {
        File customGlobalDir;
        String isInstSupport = SystemProperties.get("ro.mtk_carrierexpress_inst_sup");
        if (isInstSupport != null && isInstSupport.equals("1")) {
            Log.d("UspServiceImpl", "Install/uninstall apk is enabled");
            return;
        }
        if (new File(CUSTOM_PATH).exists()) {
            customGlobalDir = new File(CUSTOM_PATH);
        } else {
            if (!new File(VENDOR_PATH).exists()) {
                Log.e("UspServiceImpl", "none of custom/usp or vendor/usp exists");
                return;
            }
            customGlobalDir = new File(VENDOR_PATH);
        }
        customGlobalDir.list();
        String opFileName = "usp-content-" + optr + ".txt";
        File customAllFile = new File(customGlobalDir, "usp-packages-all.txt");
        File customOpFile = new File(customGlobalDir, opFileName);
        this.mPm = this.mContext.getPackageManager();
        List<String> allPackageList = readFromFile(customAllFile);
        Log.d("UspServiceImpl", "enabledDisableApps ALL File First content" + allPackageList.get(0));
        List<String> opPackageList = readFromFile(customOpFile, "[Package-start]", "[Package-end]");
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiver(this.mEnableDisableRespReceiver, packageFilter);
        for (int i = 0; i < allPackageList.size(); i++) {
            Log.d("UspServiceImpl", allPackageList.get(i) + " not in OP File " + (!opPackageList.contains(allPackageList.get(i))) + " EnabledState: " + getPackageEnabledState(allPackageList.get(i), false));
            if (!opPackageList.contains(allPackageList.get(i)) && getPackageEnabledState(allPackageList.get(i), false)) {
                this.mPendingEnableDisableReq.add(allPackageList.get(i));
                disableApps(allPackageList.get(i));
            }
        }
        for (int i2 = 0; i2 < opPackageList.size(); i2++) {
            Log.d("UspServiceImpl", opPackageList.get(i2) + " EnabledState: " + getPackageEnabledState(opPackageList.get(i2), true));
            if (!getPackageEnabledState(opPackageList.get(i2), true)) {
                this.mPendingEnableDisableReq.add(opPackageList.get(i2));
                enableApps(opPackageList.get(i2));
            }
        }
    }

    private boolean getPackageEnabledState(String packageName, boolean defaultState) {
        try {
            ApplicationInfo ai = this.mPm.getApplicationInfo(packageName, 0);
            return ai.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.e("UspServiceImpl", "getPackageEnabledState, packageNotFound: " + packageName);
            return defaultState;
        }
    }

    private String getKey(String toBeSplit) {
        int assignmentIndex = toBeSplit.indexOf("=");
        try {
            String key = toBeSplit.substring(0, assignmentIndex);
            return key;
        } catch (IndexOutOfBoundsException e) {
            Log.e("UspServiceImpl", "illegal property string: " + e.toString());
            return null;
        }
    }

    private String getValue(String toBeSplit) {
        int assignmentIndex = toBeSplit.indexOf("=");
        try {
            String value = toBeSplit.substring(assignmentIndex + 1, toBeSplit.length());
            return value;
        } catch (IndexOutOfBoundsException e) {
            Log.e("UspServiceImpl", "illegal property string: " + e.toString());
            return null;
        }
    }

    private List<String> readFromFile(File customGlobalFile) {
        int length = (int) customGlobalFile.length();
        byte[] bArr = new byte[length];
        List<String> fileContents = new ArrayList<>();
        try {
            FileInputStream inputStream = new FileInputStream(customGlobalFile);
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                while (true) {
                    String receiveString = bufferedReader.readLine();
                    if (receiveString == null) {
                        break;
                    }
                    fileContents.add(receiveString);
                }
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.e("UspServiceImpl", "File not found: " + e.toString());
        } catch (IOException e2) {
            Log.e("UspServiceImpl", "Can not read file: " + e2.toString());
        }
        return fileContents;
    }

    private List<String> readFromFile(File customGlobalFile, String startTag, String endTag) {
        int length = (int) customGlobalFile.length();
        byte[] bArr = new byte[length];
        List<String> fileContents = new ArrayList<>();
        try {
            FileInputStream inputStream = new FileInputStream(customGlobalFile);
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                boolean isSect = false;
                while (true) {
                    String receiveString = bufferedReader.readLine();
                    if (receiveString == null) {
                        break;
                    }
                    if (startTag.equals(receiveString)) {
                        isSect = true;
                    } else {
                        if (endTag.equals(receiveString)) {
                            break;
                        }
                        if (isSect) {
                            fileContents.add(receiveString);
                        }
                    }
                }
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.e("UspServiceImpl", "File not found: " + e.toString());
        } catch (IOException e2) {
            Log.e("UspServiceImpl", "Can not read file: " + e2.toString());
        }
        return fileContents;
    }

    private void enableApps(String appPackage) {
        Log.d("UspServiceImpl", "enablingApp :" + appPackage);
        try {
            this.mPm.setApplicationEnabledSetting(appPackage, 1, 1);
        } catch (IllegalArgumentException e) {
            Log.e("UspServiceImpl", "enabling illegal package: " + e.toString());
        }
    }

    private void disableApps(String appPackage) {
        Log.d("UspServiceImpl", "disablingApp :" + appPackage);
        try {
            this.mPm.setApplicationEnabledSetting(appPackage, 2, 1);
        } catch (IllegalArgumentException e) {
            Log.e("UspServiceImpl", "disabling illegal package: " + e.toString());
        }
    }

    private class MyHandler extends Handler {
        static final int FREEZE_FRAME = 1;
        static final int REBOOT_DIALOG = 0;

        MyHandler(UspServiceImpl this$0, MyHandler myHandler) {
            this();
        }

        private MyHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    UspServiceImpl.this.showWaitingScreen(((Boolean) msg.obj).booleanValue());
                    UspServiceImpl.this.freeze();
                    break;
                case 1:
                    if (UspServiceImpl.freezeFrame() < 0) {
                        Log.e("UspServiceImpl", "FREEZE FRAME FAILED...NOW WHAT TO DO...:(");
                    } else {
                        Log.d("UspServiceImpl", "showWaitingScreen Freezed");
                    }
                    break;
                default:
                    Log.d("UspServiceImpl", "Wrong message reason");
                    break;
            }
        }
    }

    private class TaskHandler extends Handler {
        static final int EARLY_READ_FAILED = 3;
        static final int REBOOT_SYSTEM = 2;
        static final int START_CONFIG = 1;
        static final int START_FIRST_CONFIG = 0;

        public TaskHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d("UspServiceImpl", "TaskHandler message:" + msg.what);
            switch (msg.what) {
                case 0:
                    UspServiceImpl.this.firstBootConfigure();
                    UspServiceImpl.this.setConfigCtrlFlag(1, true, null);
                    break;
                case 1:
                    if (!UspServiceImpl.this.mPendingEnableDisableReq.isEmpty()) {
                        UspServiceImpl.this.mPendingEnableDisableReq.clear();
                        UspServiceImpl.this.mContext.unregisterReceiver(UspServiceImpl.this.mEnableDisableRespReceiver);
                    }
                    UspServiceImpl.this.runningConfigurationTask((String) msg.obj);
                    break;
                case 2:
                    UspServiceImpl.this.rebootAndroidSystem();
                    break;
                case 3:
                    String mccMnc = UspServiceImpl.this.readMCCMNCFromProperty(true);
                    if (mccMnc.length() < 5) {
                        Log.d("UspServiceImpl", "Invalid mccMnc " + mccMnc);
                        UspServiceImpl.this.mTaskHandler.sendMessageDelayed(UspServiceImpl.this.mTaskHandler.obtainMessage(3), 500L);
                    } else {
                        String optr = UspServiceImpl.this.getOperatorPackForSim(mccMnc);
                        UspServiceImpl.this.handleSwitchOperator(optr);
                    }
                    break;
                default:
                    Log.d("UspServiceImpl", "Wrong message reason");
                    break;
            }
        }
    }
}
