package com.android.internal.telephony;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import java.util.List;

public class SubscriptionInfoUpdater extends Handler {
    public static final String CURR_SUBID = "curr_subid";
    private static final int EVENT_GET_NETWORK_SELECTION_MODE_DONE = 2;
    private static final int EVENT_SIM_ABSENT = 4;
    private static final int EVENT_SIM_IMSI = 10;
    private static final int EVENT_SIM_LOADED = 3;
    private static final int EVENT_SIM_LOCKED = 5;
    private static final int EVENT_SIM_LOCKED_QUERY_ICCID_DONE = 1;
    private static final int EVENT_SIM_OEM_START = 10;
    private static final String ICCID_STRING_FOR_NO_SIM = "";
    private static final String LOG_TAG = "SubscriptionInfoUpdater";
    public static final int SIM_CHANGED = -1;
    public static final int SIM_NEW = -2;
    public static final int SIM_NOT_CHANGE = 0;
    public static final int SIM_NOT_INSERT = -99;
    public static final int SIM_REPOSITION = -3;
    public static final int STATUS_NO_SIM_INSERTED = 0;
    public static final int STATUS_SIM1_INSERTED = 1;
    public static final int STATUS_SIM2_INSERTED = 2;
    public static final int STATUS_SIM3_INSERTED = 4;
    public static final int STATUS_SIM4_INSERTED = 8;
    private static Phone[] mPhone;
    private SubscriptionManager mSubscriptionManager;
    private final BroadcastReceiver sReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SubscriptionInfoUpdater.this.logd("[Receiver]+");
            String action = intent.getAction();
            SubscriptionInfoUpdater.this.logd("Action: " + action);
            if (action.equals("android.intent.action.SIM_STATE_CHANGED") || action.equals(IccCardProxy.ACTION_INTERNAL_SIM_STATE_CHANGED)) {
                int slotId = intent.getIntExtra("phone", -1);
                SubscriptionInfoUpdater.this.logd("slotId: " + slotId);
                if (slotId != -1) {
                    String simStatus = intent.getStringExtra("ss");
                    SubscriptionInfoUpdater.this.logd("simStatus: " + simStatus);
                    if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                        if ("ABSENT".equals(simStatus)) {
                            SubscriptionInfoUpdater.this.sendMessage(SubscriptionInfoUpdater.this.obtainMessage(4, slotId, -1));
                        } else if (!"IMSI".equals(simStatus)) {
                            SubscriptionInfoUpdater.this.logd("Ignoring simStatus: " + simStatus);
                        } else {
                            SubscriptionInfoUpdater.this.sendMessage(SubscriptionInfoUpdater.this.obtainMessage(10, slotId, -1));
                        }
                    } else if (action.equals(IccCardProxy.ACTION_INTERNAL_SIM_STATE_CHANGED)) {
                        if ("LOCKED".equals(simStatus)) {
                            String reason = intent.getStringExtra("reason");
                            SubscriptionInfoUpdater.this.sendMessage(SubscriptionInfoUpdater.this.obtainMessage(5, slotId, -1, reason));
                        } else if (!"LOADED".equals(simStatus)) {
                            SubscriptionInfoUpdater.this.logd("Ignoring simStatus: " + simStatus);
                        } else {
                            SubscriptionInfoUpdater.this.sendMessage(SubscriptionInfoUpdater.this.obtainMessage(3, slotId, -1));
                        }
                    } else if (action.equals("android.intent.action.SIM_HOT_PLUGGED")) {
                        boolean simTrayPlug = intent.getBooleanExtra("simTrayPlug", false);
                        int slotId2 = intent.getIntExtra("slot", -1);
                        SubscriptionInfoUpdater.this.logd("slotId: " + slotId2 + " simTrayPlug: " + simTrayPlug);
                        if (slotId2 != -1) {
                            if (simTrayPlug) {
                                SubscriptionInfoUpdater.mIccId[slotId2] = null;
                            }
                        } else {
                            return;
                        }
                    }
                    SubscriptionInfoUpdater.this.logd("[Receiver]-");
                }
            }
        }
    };
    private static final int PROJECT_SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static Context mContext = null;
    private static String[] mIccId = new String[PROJECT_SIM_NUM];
    private static int[] mInsertSimState = new int[PROJECT_SIM_NUM];

    public SubscriptionInfoUpdater(Context context, Phone[] phoneProxy, CommandsInterface[] ci) {
        this.mSubscriptionManager = null;
        logd("Constructor invoked");
        mContext = context;
        mPhone = phoneProxy;
        this.mSubscriptionManager = SubscriptionManager.from(mContext);
        mContext.registerReceiver(this.sReceiver, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
        IntentFilter intentFilter = new IntentFilter(IccCardProxy.ACTION_INTERNAL_SIM_STATE_CHANGED);
        intentFilter.addAction("android.intent.action.SIM_HOT_PLUGGED");
        mContext.registerReceiver(this.sReceiver, intentFilter);
    }

    private boolean isAllIccIdQueryDone() {
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mIccId[i] == null) {
                logd("Wait for SIM" + (i + 1) + " IccId");
                return false;
            }
        }
        logd("All IccIds query complete");
        return true;
    }

    public void setDisplayNameForNewSub(String newSubName, int subId, int newNameSource) {
        SubscriptionInfo subInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(subId);
        if (subInfo != null) {
            int oldNameSource = subInfo.getNameSource();
            CharSequence oldSubName = subInfo.getDisplayName();
            logd("[setDisplayNameForNewSub] subId = " + subInfo.getSubscriptionId() + ", oldSimName = " + ((Object) oldSubName) + ", oldNameSource = " + oldNameSource + ", newSubName = " + newSubName + ", newNameSource = " + newNameSource);
            if (oldSubName == null || ((oldNameSource == 0 && newSubName != null) || (oldNameSource == 1 && newSubName != null && !newSubName.equals(oldSubName)))) {
                this.mSubscriptionManager.setDisplayName(newSubName, subInfo.getSubscriptionId(), newNameSource);
                return;
            }
            return;
        }
        logd("SUB" + (subId + 1) + " SubInfo not created yet");
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                AsyncResult ar = (AsyncResult) msg.obj;
                QueryIccIdUserObj uObj = (QueryIccIdUserObj) ar.userObj;
                int slotId = uObj.slotId;
                logd("handleMessage : <EVENT_SIM_LOCKED_QUERY_ICCID_DONE> SIM" + (slotId + 1));
                if (ar.exception == null) {
                    if (ar.result != null) {
                        byte[] data = (byte[]) ar.result;
                        mIccId[slotId] = com.android.internal.telephony.uicc.IccUtils.bcdToString(data, 0, data.length);
                    } else {
                        logd("Null ar");
                        mIccId[slotId] = ICCID_STRING_FOR_NO_SIM;
                    }
                } else {
                    mIccId[slotId] = ICCID_STRING_FOR_NO_SIM;
                    logd("Query IccId fail: " + ar.exception);
                }
                logd("sIccId[" + slotId + "] = " + mIccId[slotId]);
                if (isAllIccIdQueryDone()) {
                    updateSubscriptionInfoByIccId();
                }
                broadcastSimStateChanged(slotId, "LOCKED", uObj.reason);
                break;
            case 2:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                Integer slotId2 = (Integer) ar2.userObj;
                if (ar2.exception == null && ar2.result != null) {
                    int[] modes = (int[]) ar2.result;
                    if (modes[0] == 1) {
                        mPhone[slotId2.intValue()].setNetworkSelectionModeAutomatic(null);
                    }
                } else {
                    logd("EVENT_GET_NETWORK_SELECTION_MODE_DONE: error getting network mode.");
                }
                break;
            case 3:
                handleSimLoaded(msg.arg1);
                break;
            case 4:
                handleSimAbsent(msg.arg1);
                break;
            case 5:
                handleSimLocked(msg.arg1, (String) msg.obj);
                break;
            case 6:
            case 7:
            case 8:
            case 9:
            default:
                logd("Unknown msg:" + msg.what);
                break;
            case 10:
                handleSimImsi(msg.arg1);
                break;
        }
    }

    private static class QueryIccIdUserObj {
        public String reason;
        public int slotId;

        QueryIccIdUserObj(String reason, int slotId) {
            this.reason = reason;
            this.slotId = slotId;
        }
    }

    private void handleSimLocked(int slotId, String reason) {
        if (mIccId[slotId] != null && mIccId[slotId].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (slotId + 1) + " hot plug in");
            mIccId[slotId] = null;
        }
        IccFileHandler fileHandler = mPhone[slotId].getIccCard() != null ? mPhone[slotId].getIccCard().getIccFileHandler() : null;
        if (fileHandler != null) {
            String iccId = mIccId[slotId];
            if (iccId == null) {
                logd("Querying IccId");
                fileHandler.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(1, new QueryIccIdUserObj(reason, slotId)));
                return;
            } else {
                logd("NOT Querying IccId its already set sIccid[" + slotId + "]=" + iccId);
                broadcastSimStateChanged(slotId, "LOCKED", reason);
                return;
            }
        }
        logd("sFh[" + slotId + "] is null, ignore");
    }

    private void handleSimImsi(int slotId) {
        boolean updateSubscriptionInfo = false;
        logd("handleSimImsi: slotId: " + slotId);
        IccRecords records = mPhone[slotId].getIccCard().getIccRecords();
        if (records == null) {
            logd("handleSimImsi: IccRecords null");
            return;
        }
        if (records.getIccId() == null) {
            logd("handleSimImsi: IccID null");
            return;
        }
        if (!records.getIccId().equals(mIccId[slotId])) {
            logd("handleSimImsi: IccID is not updated ready!!");
            updateSubscriptionInfo = true;
            mIccId[slotId] = records.getIccId();
        }
        if (updateSubscriptionInfo && isAllIccIdQueryDone()) {
            updateSubscriptionInfoByIccId();
        }
    }

    private void handleSimLoaded(int slotId) {
        String nameToSet;
        boolean updateSubscriptionInfo = false;
        logd("handleSimStateLoadedInternal: slotId: " + slotId);
        IccRecords records = mPhone[slotId].getIccCard().getIccRecords();
        if (records == null) {
            logd("onRecieve: IccRecords null");
            return;
        }
        if (records.getIccId() == null) {
            logd("onRecieve: IccID null");
            return;
        }
        if (!records.getIccId().equals(mIccId[slotId])) {
            logd("handleSimLoaded: IccID is not updated in IMSI ready!!");
            updateSubscriptionInfo = true;
            mIccId[slotId] = records.getIccId();
        }
        if (updateSubscriptionInfo && isAllIccIdQueryDone()) {
            updateSubscriptionInfoByIccId();
        }
        int subId = Integer.MAX_VALUE;
        int[] subIds = SubscriptionController.getInstance().getSubId(slotId);
        if (subIds != null) {
            subId = subIds[0];
        }
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            String operator = records.getOperatorNumeric();
            if (operator != null) {
                if (subId == SubscriptionController.getInstance().getDefaultSubId()) {
                    MccTable.updateMccMncConfiguration(mContext, operator, false);
                }
                SubscriptionController.getInstance().setMccMnc(operator, subId);
            } else {
                logd("EVENT_RECORDS_LOADED Operator name is null");
            }
            TelephonyManager tm = TelephonyManager.getDefault();
            String msisdn = tm.getLine1NumberForSubscriber(subId);
            ContentResolver contentResolver = mContext.getContentResolver();
            if (msisdn != null) {
                ContentValues number = new ContentValues(1);
                number.put("number", msisdn);
                contentResolver.update(SubscriptionManager.CONTENT_URI, number, "_id=" + Long.toString(subId), null);
            }
            SubscriptionInfo subInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(subId);
            String simCarrierName = tm.getSimOperatorNameForSubscription(subId);
            ContentValues name = new ContentValues(1);
            if (subInfo != null && subInfo.getNameSource() != 2) {
                if (!TextUtils.isEmpty(simCarrierName)) {
                    nameToSet = simCarrierName + Integer.toString(slotId + 1);
                } else {
                    nameToSet = "CARD " + Integer.toString(slotId + 1);
                }
                name.put("display_name", nameToSet);
                logd("sim name = " + nameToSet);
                contentResolver.update(SubscriptionManager.CONTENT_URI, name, "_id=" + Long.toString(subId), null);
            }
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            int storedSubId = sp.getInt(CURR_SUBID + slotId, -1);
            if (storedSubId != subId) {
                int networkType = PhoneFactory.calculatePreferredNetworkType(mContext, subId);
                mPhone[slotId].setPreferredNetworkType(networkType, null);
                PhoneFactory.saveNetworkMode(mPhone[slotId].getContext(), mPhone[slotId].getPhoneId(), networkType);
                mPhone[slotId].getNetworkSelectionMode(obtainMessage(2, new Integer(slotId)));
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt(CURR_SUBID + slotId, subId);
                editor.apply();
            }
        } else {
            logd("Invalid subId, could not update ContentResolver");
        }
        broadcastSimStateChanged(slotId, "LOADED", null);
    }

    private void handleSimAbsent(int slotId) {
        if (mIccId[slotId] != null && !mIccId[slotId].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (slotId + 1) + " hot plug out");
        }
        mIccId[slotId] = ICCID_STRING_FOR_NO_SIM;
        if (isAllIccIdQueryDone()) {
            updateSubscriptionInfoByIccId();
        }
    }

    private synchronized void updateSubscriptionInfoByIccId() {
        logd("updateSubscriptionInfoByIccId:+ Start");
        this.mSubscriptionManager.clearSubscriptionInfo();
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            mInsertSimState[i] = 0;
        }
        int insertedSimCount = PROJECT_SIM_NUM;
        for (int i2 = 0; i2 < PROJECT_SIM_NUM; i2++) {
            if (ICCID_STRING_FOR_NO_SIM.equals(mIccId[i2])) {
                insertedSimCount--;
                mInsertSimState[i2] = -99;
            }
        }
        logd("insertedSimCount = " + insertedSimCount);
        for (int i3 = 0; i3 < PROJECT_SIM_NUM; i3++) {
            if (mInsertSimState[i3] != -99) {
                int index = 2;
                for (int j = i3 + 1; j < PROJECT_SIM_NUM; j++) {
                    if (mInsertSimState[j] == 0 && mIccId[i3].equals(mIccId[j])) {
                        mInsertSimState[i3] = 1;
                        mInsertSimState[j] = index;
                        index++;
                    }
                }
            }
        }
        ContentResolver contentResolver = mContext.getContentResolver();
        String[] oldIccId = new String[PROJECT_SIM_NUM];
        for (int i4 = 0; i4 < PROJECT_SIM_NUM; i4++) {
            oldIccId[i4] = null;
            List<SubscriptionInfo> oldSubInfo = SubscriptionController.getInstance().getSubInfoUsingSlotIdWithCheck(i4, false);
            if (oldSubInfo != null) {
                oldIccId[i4] = oldSubInfo.get(0).getIccId();
                logd("updateSubscriptionInfoByIccId: oldSubId = " + oldSubInfo.get(0).getSubscriptionId());
                if (mInsertSimState[i4] == 0 && !mIccId[i4].equals(oldIccId[i4])) {
                    mInsertSimState[i4] = -1;
                }
                if (mInsertSimState[i4] != 0) {
                    ContentValues value = new ContentValues(1);
                    value.put("sim_id", (Integer) (-1));
                    contentResolver.update(SubscriptionManager.CONTENT_URI, value, "_id=" + Integer.toString(oldSubInfo.get(0).getSubscriptionId()), null);
                }
            } else {
                if (mInsertSimState[i4] == 0) {
                    mInsertSimState[i4] = -1;
                }
                oldIccId[i4] = ICCID_STRING_FOR_NO_SIM;
                logd("updateSubscriptionInfoByIccId: No SIM in slot " + i4 + " last time");
            }
        }
        for (int i5 = 0; i5 < PROJECT_SIM_NUM; i5++) {
            logd("updateSubscriptionInfoByIccId: oldIccId[" + i5 + "] = " + oldIccId[i5] + ", sIccId[" + i5 + "] = " + mIccId[i5]);
        }
        int nNewCardCount = 0;
        int nNewSimStatus = 0;
        for (int i6 = 0; i6 < PROJECT_SIM_NUM; i6++) {
            if (mInsertSimState[i6] == -99) {
                logd("updateSubscriptionInfoByIccId: No SIM inserted in slot " + i6 + " this time");
            } else {
                if (mInsertSimState[i6] > 0) {
                    this.mSubscriptionManager.addSubscriptionInfoRecord(mIccId[i6] + Integer.toString(mInsertSimState[i6]), i6);
                    logd("SUB" + (i6 + 1) + " has invalid IccId");
                } else {
                    this.mSubscriptionManager.addSubscriptionInfoRecord(mIccId[i6], i6);
                }
                if (isNewSim(mIccId[i6], oldIccId)) {
                    nNewCardCount++;
                    switch (i6) {
                        case 0:
                            nNewSimStatus |= 1;
                            break;
                        case 1:
                            nNewSimStatus |= 2;
                            break;
                        case 2:
                            nNewSimStatus |= 4;
                            break;
                    }
                    mInsertSimState[i6] = -2;
                }
            }
        }
        for (int i7 = 0; i7 < PROJECT_SIM_NUM; i7++) {
            if (mInsertSimState[i7] == -1) {
                mInsertSimState[i7] = -3;
            }
            logd("updateSubscriptionInfoByIccId: sInsertSimState[" + i7 + "] = " + mInsertSimState[i7]);
        }
        List<SubscriptionInfo> subInfos = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        int nSubCount = subInfos == null ? 0 : subInfos.size();
        logd("updateSubscriptionInfoByIccId: nSubCount = " + nSubCount);
        for (int i8 = 0; i8 < nSubCount; i8++) {
            SubscriptionInfo temp = subInfos.get(i8);
            String msisdn = TelephonyManager.getDefault().getLine1NumberForSubscriber(temp.getSubscriptionId());
            if (msisdn != null) {
                ContentValues value2 = new ContentValues(1);
                value2.put("number", msisdn);
                contentResolver.update(SubscriptionManager.CONTENT_URI, value2, "_id=" + Integer.toString(temp.getSubscriptionId()), null);
            }
        }
        SubscriptionController.getInstance().notifySubscriptionInfoChanged();
        logd("updateSubscriptionInfoByIccId:- SsubscriptionInfo update complete");
    }

    private boolean isNewSim(String iccId, String[] oldIccId) {
        boolean newSim = true;
        int i = 0;
        while (true) {
            if (i >= PROJECT_SIM_NUM) {
                break;
            }
            if (!iccId.equals(oldIccId[i])) {
                i++;
            } else {
                newSim = false;
                break;
            }
        }
        logd("newSim = " + newSim);
        return newSim;
    }

    private void broadcastSimStateChanged(int slotId, String state, String reason) {
        Intent i = new Intent("android.intent.action.SIM_STATE_CHANGED");
        i.addFlags(67108864);
        i.putExtra("phoneName", "Phone");
        i.putExtra("ss", state);
        i.putExtra("reason", reason);
        SubscriptionManager.putPhoneIdAndSubIdExtra(i, slotId);
        logd("Broadcasting intent ACTION_SIM_STATE_CHANGED " + state + " reason " + reason + " for mCardIndex : " + slotId);
        ActivityManagerNative.broadcastStickyIntent(i, "android.permission.READ_PHONE_STATE", -1);
    }

    public void dispose() {
        logd("[dispose]");
        mContext.unregisterReceiver(this.sReceiver);
    }

    private void logd(String message) {
        Rlog.d(LOG_TAG, message);
    }
}
