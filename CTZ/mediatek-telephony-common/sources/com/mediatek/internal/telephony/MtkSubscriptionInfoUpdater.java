package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CarrierAppUtils;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.SubscriptionInfoUpdater;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.uicc.MtkSpnOverride;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.List;

public class MtkSubscriptionInfoUpdater extends SubscriptionInfoUpdater {
    private static final String COMMON_SLOT_PROPERTY = "ro.vendor.mtk_sim_hot_swap_common_slot";
    private static final boolean DBG = true;
    private static final int EVENT_RADIO_AVAILABLE = 101;
    private static final int EVENT_RADIO_UNAVAILABLE = 102;
    private static final int EVENT_SIM_NO_CHANGED = 103;
    private static final int EVENT_SIM_PLUG_OUT = 105;
    private static final int EVENT_TRAY_PLUG_IN = 104;
    private static final String ICCID_STRING_FOR_NO_SIM = "N/A";
    private static final String LOG_TAG = "MtkSubscriptionInfoUpdater";
    private static final String PROPERTY_SML_MODE = "ro.vendor.sim_me_lock_mode";
    private static final int sReadICCID_retry_time = 1000;
    private CommandsInterface[] mCis;
    private boolean mCommonSlotResetDone;
    private boolean mIsSmlLockMode;
    private int[] mIsUpdateAvailable;
    private final Object mLock;
    private final Object mLockUpdateNew;
    private final Object mLockUpdateOld;
    private final BroadcastReceiver mMtkReceiver;
    private int mReadIccIdCount;
    private Runnable mReadIccIdPropertyRunnable;
    private boolean mSimMountChangeState;
    private int[] newSmlInfo;
    private int[] oldSmlInfo;
    private static MtkSubscriptionInfoUpdater sInstance = null;
    private static final boolean MTK_FLIGHTMODE_POWEROFF_MD_SUPPORT = "1".equals(SystemProperties.get("ro.vendor.mtk_flight_mode_power_off_md"));
    private static final String[] PROPERTY_ICCID_SIM = {"vendor.ril.iccid.sim1", "vendor.ril.iccid.sim2", "vendor.ril.iccid.sim3", "vendor.ril.iccid.sim4"};

    static int access$304(MtkSubscriptionInfoUpdater mtkSubscriptionInfoUpdater) {
        int i = mtkSubscriptionInfoUpdater.mReadIccIdCount + 1;
        mtkSubscriptionInfoUpdater.mReadIccIdCount = i;
        return i;
    }

    public MtkSubscriptionInfoUpdater(Looper looper, Context context, Phone[] phoneArr, CommandsInterface[] commandsInterfaceArr) {
        super(looper, context, phoneArr, commandsInterfaceArr);
        this.mLock = new Object();
        this.mLockUpdateNew = new Object();
        this.mLockUpdateOld = new Object();
        this.newSmlInfo = new int[]{-1, -1, -1, -1};
        this.oldSmlInfo = new int[]{-1, -1, -1, -1};
        this.mSimMountChangeState = false;
        this.mIsSmlLockMode = SystemProperties.get(PROPERTY_SML_MODE, "").equals("3");
        this.mCis = null;
        this.mIsUpdateAvailable = new int[PROJECT_SIM_NUM];
        this.mReadIccIdCount = 0;
        this.mCommonSlotResetDone = false;
        this.mMtkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                MtkSubscriptionInfoUpdater.this.logd("onReceive, Action: " + action);
                if (!action.equals("com.mediatek.phone.ACTION_COMMON_SLOT_NO_CHANGED") && !action.equals("android.intent.action.LOCALE_CHANGED")) {
                    return;
                }
                if (action.equals("android.intent.action.LOCALE_CHANGED")) {
                    for (int i : MtkSubscriptionInfoUpdater.this.mSubscriptionManager.getActiveSubscriptionIdList()) {
                        MtkSubscriptionInfoUpdater.this.updateSubName(i);
                    }
                    return;
                }
                if (action.equals("com.mediatek.phone.ACTION_COMMON_SLOT_NO_CHANGED")) {
                    int intExtra = intent.getIntExtra("phone", -1);
                    MtkSubscriptionInfoUpdater.this.logd("[Common Slot] NO_CHANTED, slotId: " + intExtra);
                    MtkSubscriptionInfoUpdater.this.sendMessage(MtkSubscriptionInfoUpdater.this.obtainMessage(MtkSubscriptionInfoUpdater.EVENT_SIM_NO_CHANGED, intExtra, -1));
                }
            }
        };
        this.mReadIccIdPropertyRunnable = new Runnable() {
            @Override
            public void run() {
                MtkSubscriptionInfoUpdater.access$304(MtkSubscriptionInfoUpdater.this);
                if (MtkSubscriptionInfoUpdater.this.mReadIccIdCount <= 10) {
                    if (!MtkSubscriptionInfoUpdater.this.checkAllIccIdReady()) {
                        MtkSubscriptionInfoUpdater.this.postDelayed(MtkSubscriptionInfoUpdater.this.mReadIccIdPropertyRunnable, 1000L);
                    } else {
                        MtkSubscriptionInfoUpdater.this.updateSubscriptionInfoIfNeed();
                    }
                }
            }
        };
        logd("MtkSubscriptionInfoUpdater created");
        this.mCis = commandsInterfaceArr;
        IntentFilter intentFilter = new IntentFilter("com.mediatek.phone.ACTION_COMMON_SLOT_NO_CHANGED");
        if (DataSubConstants.OPERATOR_OP09.equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR))) {
            intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        }
        mContext.registerReceiver(this.mMtkReceiver, intentFilter);
        for (int i = 0; i < this.mCis.length; i++) {
            Integer num = new Integer(i);
            this.mCis[i].registerForNotAvailable(this, 102, num);
            this.mCis[i].registerForAvailable(this, 101, num);
            if (SystemProperties.get(COMMON_SLOT_PROPERTY).equals("1")) {
                this.mCis[i].registerForSimTrayPlugIn(this, 104, num);
                this.mCis[i].registerForSimPlugOut(this, 105, num);
            }
        }
    }

    protected boolean isAllIccIdQueryDone() {
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mIccId[i] == null || mIccId[i].equals("")) {
                logd("Wait for SIM" + (i + 1) + " IccId");
                return false;
            }
        }
        logd("All IccIds query complete");
        return true;
    }

    public void handleMessage(Message message) {
        Integer ciIndex = getCiIndex(message);
        int i = message.what;
        if (i == 10) {
            if (checkAllIccIdReady()) {
                updateSubscriptionInfoIfNeed();
            }
            super.handleMessage(message);
        }
        switch (i) {
            case 101:
                logd("handleMessage : <EVENT_RADIO_AVAILABLE> SIM" + (ciIndex.intValue() + 1));
                this.mIsUpdateAvailable[ciIndex.intValue()] = 1;
                if (checkIsAvailable()) {
                    this.mReadIccIdCount = 0;
                    if (!checkAllIccIdReady()) {
                        postDelayed(this.mReadIccIdPropertyRunnable, 1000L);
                    } else {
                        updateSubscriptionInfoIfNeed();
                    }
                }
                break;
            case 102:
                logd("handleMessage : <EVENT_RADIO_UNAVAILABLE> SIM" + (ciIndex.intValue() + 1));
                this.mIsUpdateAvailable[ciIndex.intValue()] = 0;
                if (SystemProperties.get(COMMON_SLOT_PROPERTY).equals("1")) {
                    logd("[Common slot] reset mCommonSlotResetDone in EVENT_RADIO_UNAVAILABLE");
                    this.mCommonSlotResetDone = false;
                }
                break;
            case EVENT_SIM_NO_CHANGED:
                if (checkAllIccIdReady()) {
                    updateSubscriptionInfoIfNeed();
                } else {
                    mIccId[ciIndex.intValue()] = "N/A";
                    logd("case SIM_NO_CHANGED: set N/A for slot" + ciIndex);
                    this.mReadIccIdCount = 0;
                    postDelayed(this.mReadIccIdPropertyRunnable, 1000L);
                }
                break;
            case 104:
                logd("[Common Slot] handle EVENT_TRAY_PLUG_IN " + this.mCommonSlotResetDone);
                if (!this.mCommonSlotResetDone) {
                    this.mCommonSlotResetDone = true;
                    int i2 = 0;
                    for (int i3 = 0; i3 < PROJECT_SIM_NUM; i3++) {
                        TelephonyManager.getDefault();
                        String telephonyProperty = TelephonyManager.getTelephonyProperty(i3, "vendor.gsm.external.sim.enabled", "0");
                        if (telephonyProperty.length() == 0) {
                            telephonyProperty = "0";
                        }
                        logd("vsimEnabled[" + i3 + "]: (" + telephonyProperty + ")");
                        try {
                            if ("0".equals(telephonyProperty)) {
                                logd("[Common Slot] reset mIccId[" + i3 + "] to empty.");
                                mIccId[i3] = "";
                            } else {
                                i2 |= 1 << i3;
                            }
                        } catch (NumberFormatException e) {
                            logd("[Common Slot] NumberFormatException, reset mIccId[" + i3 + "] to empty.");
                            mIccId[i3] = "";
                        }
                    }
                    if (i2 == 0) {
                        this.mReadIccIdCount = 0;
                        if (!checkAllIccIdReady()) {
                            postDelayed(this.mReadIccIdPropertyRunnable, 1000L);
                        } else {
                            updateSubscriptionInfoIfNeed();
                        }
                    }
                }
                break;
            case 105:
                logd("[Common Slot] handle EVENT_SIM_PLUG_OUT " + this.mCommonSlotResetDone);
                if (SystemProperties.get(COMMON_SLOT_PROPERTY).equals("1")) {
                    this.mReadIccIdCount = 0;
                    postDelayed(this.mReadIccIdPropertyRunnable, 1000L);
                }
                this.mCommonSlotResetDone = false;
                break;
            default:
                super.handleMessage(message);
                break;
        }
    }

    protected void handleSimLocked(int i, String str) {
        if (mIccId[i] != null && mIccId[i].equals("N/A")) {
            logd("SIM" + (i + 1) + " hot plug in");
            mIccId[i] = null;
        }
        String str2 = mIccId[i];
        if (str2 == null) {
            IccCard iccCard = mPhone[i].getIccCard();
            if (iccCard == null) {
                logd("handleSimLocked: IccCard null");
                return;
            } else if (iccCard.getIccRecords() == null) {
                logd("handleSimLocked: IccRecords null");
                return;
            }
        } else {
            logd("NOT Querying IccId its already set sIccid[" + i + "]=" + SubscriptionInfo.givePrintableIccid(str2));
            String str3 = SystemProperties.get(PROPERTY_ICCID_SIM[i], "");
            if (MTK_FLIGHTMODE_POWEROFF_MD_SUPPORT && !checkAllIccIdReady() && !str3.equals(mIccId[i])) {
                logd("All iccids are not ready and iccid changed");
                mIccId[i] = null;
                this.mSubscriptionManager.clearSubscriptionInfo();
            }
        }
        if (checkAllIccIdReady()) {
            updateSubscriptionInfoIfNeed();
        }
        updateCarrierServices(i, "LOCKED");
        broadcastSimStateChanged(i, "LOCKED", str);
        broadcastSimCardStateChanged(i, 11);
        broadcastSimApplicationStateChanged(i, getSimStateFromLockedReason(str));
    }

    protected void handleSimLoaded(int i) {
        logd("handleSimLoaded: slotId: " + i);
        IccCard iccCard = mPhone[i].getIccCard();
        if (iccCard == null) {
            logd("handleSimLoaded: IccCard null");
            return;
        }
        IccRecords iccRecords = iccCard.getIccRecords();
        if (iccRecords == null) {
            logd("handleSimLoaded: IccRecords null");
            return;
        }
        if (IccUtils.stripTrailingFs(iccRecords.getFullIccId()) == null) {
            logd("handleSimLoaded: IccID null");
            return;
        }
        if (checkAllIccIdReady()) {
            updateSubscriptionInfoIfNeed();
            for (int i2 : this.mSubscriptionManager.getActiveSubscriptionIdList()) {
                int phoneId = MtkSubscriptionController.getMtkInstance().getPhoneId(i2);
                if (i == phoneId) {
                    TelephonyManager telephonyManagerFrom = TelephonyManager.from(mContext);
                    String simOperatorNumeric = telephonyManagerFrom.getSimOperatorNumeric(i2);
                    if (!TextUtils.isEmpty(simOperatorNumeric)) {
                        if (i2 == MtkSubscriptionController.getMtkInstance().getDefaultSubId()) {
                            MccTable.updateMccMncConfiguration(mContext, simOperatorNumeric, false);
                        }
                        MtkSubscriptionController.getMtkInstance().setMccMnc(simOperatorNumeric, i2);
                    } else {
                        logd("EVENT_RECORDS_LOADED Operator name is null");
                    }
                    String line1Number = telephonyManagerFrom.getLine1Number(i2);
                    mContext.getContentResolver();
                    if (line1Number != null) {
                        MtkSubscriptionController.getMtkInstance().setDisplayNumber(line1Number, i2);
                    }
                    SubscriptionInfo activeSubscriptionInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(i2);
                    String simOperatorName = telephonyManagerFrom.getSimOperatorName(i2);
                    if (activeSubscriptionInfo != null && activeSubscriptionInfo.getNameSource() != 2) {
                        String simOperatorNumeric2 = telephonyManagerFrom.getSimOperatorNumeric(i2);
                        String strLookupOperatorNameForDisplayName = MtkSpnOverride.getInstance().lookupOperatorNameForDisplayName(i2, simOperatorNumeric2, true, mContext);
                        logd("[handleSimLoaded]- simNumeric: " + simOperatorNumeric2 + ", simMvnoName: " + strLookupOperatorNameForDisplayName + ", simCarrierName: " + simOperatorName);
                        if (TextUtils.isEmpty(strLookupOperatorNameForDisplayName)) {
                            if (TextUtils.isEmpty(simOperatorName)) {
                                simOperatorName = "CARD " + Integer.toString(phoneId + 1);
                            }
                        } else {
                            simOperatorName = strLookupOperatorNameForDisplayName;
                        }
                        logd("sim name = " + simOperatorName);
                        MtkSubscriptionController.getMtkInstance().setDisplayName(simOperatorName, i2);
                    }
                    SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                    if (defaultSharedPreferences.getInt("curr_subid" + phoneId, -1) != i2) {
                        int intAtIndex = Settings.Global.getInt(mPhone[phoneId].getContext().getContentResolver(), "preferred_network_mode" + i2, -1);
                        if (intAtIndex == -1) {
                            intAtIndex = RILConstants.PREFERRED_NETWORK_MODE;
                            try {
                                intAtIndex = TelephonyManager.getIntAtIndex(mContext.getContentResolver(), "preferred_network_mode", phoneId);
                            } catch (Settings.SettingNotFoundException e) {
                                Rlog.e(LOG_TAG, "Settings Exception Reading Value At Index for Settings.Global.PREFERRED_NETWORK_MODE");
                            }
                        }
                        Settings.Global.putInt(mPhone[phoneId].getContext().getContentResolver(), "preferred_network_mode" + i2, intAtIndex);
                        mPhone[phoneId].getNetworkSelectionMode(obtainMessage(2, new Integer(phoneId)));
                        SharedPreferences.Editor editorEdit = defaultSharedPreferences.edit();
                        editorEdit.putInt("curr_subid" + phoneId, i2);
                        editorEdit.apply();
                    }
                }
            }
            CarrierAppUtils.disableCarrierAppsUntilPrivileged(mContext.getOpPackageName(), this.mPackageManager, TelephonyManager.getDefault(), mContext.getContentResolver(), this.mCurrentlyActiveUserId);
            broadcastSimStateChanged(i, "LOADED", null);
            broadcastSimCardStateChanged(i, 11);
            broadcastSimApplicationStateChanged(i, 10);
            updateCarrierServices(i, "LOADED");
            return;
        }
        logd("[handleSimLoaded] checkAllIccIdReady is false, retry it in slot[" + i + "]");
        sendMessageDelayed(obtainMessage(3, i, -1), 100L);
    }

    protected void handleSimAbsent(int i) {
        if (mIccId[i] != null && !mIccId[i].equals("N/A")) {
            logd("SIM" + (i + 1) + " hot plug out");
        }
        if (checkAllIccIdReady()) {
            updateSubscriptionInfoIfNeed();
        }
        updateCarrierServices(i, "ABSENT");
        broadcastSimStateChanged(i, "ABSENT", null);
        broadcastSimCardStateChanged(i, 1);
        broadcastSimApplicationStateChanged(i, 6);
    }

    protected void handleSimError(int i) {
        if (mIccId[i] != null && !mIccId[i].equals("N/A")) {
            logd("SIM" + (i + 1) + " Error ");
        }
        mIccId[i] = "N/A";
        if (isAllIccIdQueryDone()) {
            updateSubscriptionInfoByIccId();
        }
        updateCarrierServices(i, "CARD_IO_ERROR");
        broadcastSimStateChanged(i, "CARD_IO_ERROR", "CARD_IO_ERROR");
        broadcastSimCardStateChanged(i, 8);
        broadcastSimApplicationStateChanged(i, 6);
    }

    protected synchronized void updateSubscriptionInfoByIccId() {
        int i;
        int size;
        boolean z;
        int i2;
        Intent updatedData;
        logd("updateSubscriptionInfoByIccId:+ Start");
        if (isAllIccIdQueryDone()) {
            this.mCommonSlotResetDone = false;
            int i3 = 0;
            boolean z2 = false;
            while (true) {
                i = 1;
                if (i3 >= PROJECT_SIM_NUM) {
                    break;
                }
                mInsertSimState[i3] = 0;
                int simState = TelephonyManager.from(mContext).getSimState(i3);
                if (simState == 2 || simState == 3 || simState == 4 || simState == 6 || simState == 0 || (simState == 1 && !"N/A".equals(mIccId[i3]))) {
                    logd("skipCapabilitySwitch = " + z2);
                    z2 = true;
                }
                i3++;
            }
            int i4 = PROJECT_SIM_NUM;
            for (int i5 = 0; i5 < PROJECT_SIM_NUM; i5++) {
                if ("N/A".equals(mIccId[i5])) {
                    i4--;
                    mInsertSimState[i5] = -99;
                }
            }
            logd("insertedSimCount = " + i4);
            for (int i6 = 0; i6 < PROJECT_SIM_NUM; i6++) {
                if (mInsertSimState[i6] != -99) {
                    int i7 = 2;
                    for (int i8 = i6 + 1; i8 < PROJECT_SIM_NUM; i8++) {
                        if (mInsertSimState[i8] == 0 && mIccId[i6].equals(mIccId[i8])) {
                            mInsertSimState[i6] = 1;
                            mInsertSimState[i8] = i7;
                            i7++;
                        }
                    }
                }
            }
            ContentResolver contentResolver = mContext.getContentResolver();
            String[] strArr = new String[PROJECT_SIM_NUM];
            String[] strArr2 = new String[PROJECT_SIM_NUM];
            int i9 = 0;
            while (i9 < PROJECT_SIM_NUM) {
                strArr[i9] = null;
                List subInfoUsingSlotIndexPrivileged = MtkSubscriptionController.getMtkInstance().getSubInfoUsingSlotIndexPrivileged(i9, false);
                strArr2[i9] = IccUtils.getDecimalSubstring(mIccId[i9]);
                if (subInfoUsingSlotIndexPrivileged != null && subInfoUsingSlotIndexPrivileged.size() > 0) {
                    strArr[i9] = ((SubscriptionInfo) subInfoUsingSlotIndexPrivileged.get(0)).getIccId();
                    logd("updateSubscriptionInfoByIccId: oldSubId = " + ((SubscriptionInfo) subInfoUsingSlotIndexPrivileged.get(0)).getSubscriptionId());
                    if (mInsertSimState[i9] == 0 && !mIccId[i9].equals(strArr[i9]) && ((strArr2[i9] == null || !strArr2[i9].equals(strArr[i9])) && !mIccId[i9].toLowerCase().equals(strArr[i9]))) {
                        mInsertSimState[i9] = -1;
                    }
                    if (mInsertSimState[i9] != 0) {
                        MtkSubscriptionController.getMtkInstance().clearSubInfoUsingPhoneId(i9);
                        logd("updateSubscriptionInfoByIccId: clearSubInfoUsingPhoneId phoneId = " + i9);
                        try {
                            ContentValues contentValues = new ContentValues(i);
                            contentValues.put("sim_id", (Integer) (-1));
                            contentResolver.update(SubscriptionManager.CONTENT_URI, contentValues, "_id=" + Integer.toString(((SubscriptionInfo) subInfoUsingSlotIndexPrivileged.get(0)).getSubscriptionId()), null);
                            SubscriptionController.getInstance().refreshCachedActiveSubscriptionInfoList();
                        } catch (IllegalArgumentException e) {
                            throw e;
                        }
                    } else {
                        continue;
                    }
                } else {
                    if (mInsertSimState[i9] == 0) {
                        mInsertSimState[i9] = -1;
                    }
                    MtkSubscriptionController.getMtkInstance().clearSubInfoUsingPhoneId(i9);
                    logd("updateSubscriptionInfoByIccId: clearSubInfoUsingPhoneId phoneId = " + i9);
                    strArr[i9] = "N/A";
                    logd("updateSubscriptionInfoByIccId: No SIM in slot " + i9 + " last time");
                }
                i9++;
                i = 1;
            }
            for (int i10 = 0; i10 < PROJECT_SIM_NUM; i10++) {
                logd("updateSubscriptionInfoByIccId: oldIccId[" + i10 + "] = " + SubscriptionInfo.givePrintableIccid(strArr[i10]) + ", sIccId[" + i10 + "] = " + SubscriptionInfo.givePrintableIccid(mIccId[i10]));
            }
            int i11 = 0;
            int i12 = 0;
            for (int i13 = 0; i13 < PROJECT_SIM_NUM; i13++) {
                if (mInsertSimState[i13] == -99) {
                    logd("updateSubscriptionInfoByIccId: No SIM inserted in slot " + i13 + " this time");
                } else {
                    if (mInsertSimState[i13] > 0) {
                        this.mSubscriptionManager.addSubscriptionInfoRecord(mIccId[i13] + Integer.toString(mInsertSimState[i13]), i13);
                        logd("SUB" + (i13 + 1) + " has invalid IccId");
                    } else {
                        this.mSubscriptionManager.addSubscriptionInfoRecord(mIccId[i13], i13);
                    }
                    if (isNewSim(mIccId[i13], strArr2[i13], strArr)) {
                        i11++;
                        switch (i13) {
                            case 0:
                                i12 |= 1;
                                break;
                            case 1:
                                i12 |= 2;
                                break;
                            case 2:
                                i12 |= 4;
                                break;
                        }
                        mInsertSimState[i13] = -2;
                    }
                }
            }
            for (int i14 = 0; i14 < PROJECT_SIM_NUM; i14++) {
                if (mInsertSimState[i14] == -1) {
                    mInsertSimState[i14] = -3;
                }
                logd("updateSubscriptionInfoByIccId: sInsertSimState[" + i14 + "] = " + mInsertSimState[i14]);
            }
            List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
            if (activeSubscriptionInfoList != null) {
                size = activeSubscriptionInfoList.size();
            } else {
                size = 0;
            }
            logd("updateSubscriptionInfoByIccId: nSubCount = " + size);
            for (int i15 = 0; i15 < size; i15++) {
                SubscriptionInfo subscriptionInfo = activeSubscriptionInfoList.get(i15);
                String line1Number = TelephonyManager.from(mContext).getLine1Number(subscriptionInfo.getSubscriptionId());
                if (line1Number != null) {
                    MtkSubscriptionController.getMtkInstance().setDisplayNumber(line1Number, subscriptionInfo.getSubscriptionId());
                    SubscriptionController.getInstance().refreshCachedActiveSubscriptionInfoList();
                }
            }
            MtkDefaultSmsSimSettings.setSmsTalkDefaultSim(activeSubscriptionInfoList, mContext);
            int i16 = 0;
            while (true) {
                if (i16 < PROJECT_SIM_NUM) {
                    if (mIccId[i16] == null || !mIccId[i16].equals("N/A") || strArr[i16].equals("N/A")) {
                        i16++;
                    } else {
                        z = true;
                    }
                } else {
                    z = false;
                }
            }
            if (i11 == 0) {
                if (z) {
                    int i17 = 0;
                    while (true) {
                        if (i17 < PROJECT_SIM_NUM) {
                            if (mInsertSimState[i17] != -3) {
                                i17++;
                            } else {
                                logd("No new SIM detected and SIM repositioned");
                                updatedData = setUpdatedData(3, size, i12);
                            }
                        } else {
                            updatedData = null;
                        }
                    }
                    if (i17 == PROJECT_SIM_NUM) {
                        logd("No new SIM detected and SIM removed");
                        updatedData = setUpdatedData(2, size, i12);
                    }
                } else {
                    int i18 = 0;
                    while (true) {
                        if (i18 < PROJECT_SIM_NUM) {
                            if (mInsertSimState[i18] != -3) {
                                i18++;
                            } else {
                                logd("No new SIM detected and SIM repositioned");
                                updatedData = setUpdatedData(3, size, i12);
                            }
                        } else {
                            updatedData = null;
                        }
                    }
                    if (i18 == PROJECT_SIM_NUM) {
                        logd("[updateSimInfoByIccId] All SIM inserted into the same slot");
                        updatedData = setUpdatedData(4, size, i12);
                    }
                }
                i2 = 1;
            } else {
                logd("New SIM detected");
                i2 = 1;
                updatedData = setUpdatedData(1, size, i12);
            }
            if (PROJECT_SIM_NUM > i2) {
                if (!z2) {
                    SubscriptionManager subscriptionManager = this.mSubscriptionManager;
                    SubscriptionManager subscriptionManager2 = this.mSubscriptionManager;
                    subscriptionManager.setDefaultDataSubId(SubscriptionManager.getDefaultDataSubscriptionId());
                } else {
                    MtkSubscriptionController mtkInstance = MtkSubscriptionController.getMtkInstance();
                    SubscriptionManager subscriptionManager3 = this.mSubscriptionManager;
                    mtkInstance.setDefaultDataSubIdWithoutCapabilitySwitch(SubscriptionManager.getDefaultDataSubscriptionId());
                }
            }
            if (SystemProperties.getInt("ro.vendor.mtk_external_sim_support", 0) == 1 && SystemProperties.getInt("ro.vendor.mtk_non_dsda_rsim_support", 0) == 1) {
                int i19 = SystemProperties.getInt("vendor.gsm.prefered.rsim.slot", -1);
                int[] subId = MtkSubscriptionController.getMtkInstance().getSubId(i19);
                if (i19 >= 0 && i19 < PROJECT_SIM_NUM && subId != null && subId.length != 0) {
                    MtkSubscriptionController.getMtkInstance().setDefaultDataSubId(subId[0]);
                }
            }
            updateEmbeddedSubscriptions();
            MtkSubscriptionController.getMtkInstance().notifySubscriptionInfoChanged(updatedData);
            logd("updateSubscriptionInfoByIccId:- SubscriptionInfo update complete");
            if (this.mIsSmlLockMode) {
                resetSimMountChangeState();
            }
        }
    }

    protected boolean isNewSim(String str, String str2, String[] strArr) {
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < PROJECT_SIM_NUM) {
                if ((str != null && strArr[i] != null && strArr[i].indexOf(str) == 0) || (str2 != null && str2.equals(strArr[i]))) {
                    break;
                }
                i++;
            } else {
                z = true;
                break;
            }
        }
        logd("newSim = " + z);
        return z;
    }

    public void dispose() {
        logd("[dispose]");
        mContext.unregisterReceiver(this.mMtkReceiver);
    }

    private Intent setUpdatedData(int i, int i2, int i3) {
        Intent intent = new Intent("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        logd("[setUpdatedData]+ ");
        if (i == 1) {
            intent.putExtra("simDetectStatus", 1);
            intent.putExtra("simCount", i2);
            intent.putExtra("newSIMSlot", i3);
        } else if (i == 3) {
            intent.putExtra("simDetectStatus", 3);
            intent.putExtra("simCount", i2);
        } else if (i == 2) {
            intent.putExtra("simDetectStatus", 2);
            intent.putExtra("simCount", i2);
        } else if (i == 4) {
            intent.putExtra("simDetectStatus", 4);
        }
        intent.putExtra("simPropKey", "");
        logd("[setUpdatedData]- [" + i + ", " + i2 + ", " + i3 + "]");
        if (this.mIsSmlLockMode) {
            int iCheckValidCard = MtkTelephonyManagerEx.getDefault().checkValidCard(0);
            int iCheckValidCard2 = MtkTelephonyManagerEx.getDefault().checkValidCard(1);
            logd("[setUpdatedData]- [" + iCheckValidCard + ", " + iCheckValidCard2 + "]");
            updateNewSmlInfo(i, i2, iCheckValidCard, iCheckValidCard2);
        }
        return intent;
    }

    private boolean checkAllIccIdReady() {
        logd("checkAllIccIdReady +, retry_count = " + this.mReadIccIdCount);
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            String str = SystemProperties.get(PROPERTY_ICCID_SIM[i], "");
            if (str.length() == 3) {
                logd("No SIM insert :" + i);
            }
            if (str.equals("")) {
                return false;
            }
            logd("iccId[" + i + "] = " + SubscriptionInfo.givePrintableIccid(str));
        }
        return true;
    }

    private void updateSubscriptionInfoIfNeed() {
        boolean z = false;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mIccId[i] == null || !mIccId[i].equals(SystemProperties.get(PROPERTY_ICCID_SIM[i], ""))) {
                mIccId[i] = SystemProperties.get(PROPERTY_ICCID_SIM[i], "");
                logd("[updateSubscriptionInfoIfNeed] icc id change, slot[" + i + "] needUpdate: true");
                z = true;
            }
        }
        if (isAllIccIdQueryDone() && z) {
            updateSubscriptionInfoByIccId();
        }
    }

    private Integer getCiIndex(Message message) {
        Integer num = new Integer(0);
        if (message != null) {
            if (message.obj != null && (message.obj instanceof Integer)) {
                return (Integer) message.obj;
            }
            if (message.obj != null && (message.obj instanceof AsyncResult)) {
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.userObj != null && (asyncResult.userObj instanceof Integer)) {
                    return (Integer) asyncResult.userObj;
                }
                return num;
            }
            return num;
        }
        return num;
    }

    private boolean checkIsAvailable() {
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < PROJECT_SIM_NUM) {
                if (this.mIsUpdateAvailable[i] > 0) {
                    i++;
                } else {
                    logd("mIsUpdateAvailable[" + i + "] = " + this.mIsUpdateAvailable[i]);
                    break;
                }
            } else {
                z = true;
                break;
            }
        }
        logd("checkIsAvailable result = " + z);
        return z;
    }

    private void updateSubName(int i) {
        String strLookupOperatorName;
        MtkSubscriptionInfo subInfo = MtkSubscriptionManager.getSubInfo((String) null, i);
        if (subInfo != null && subInfo.getNameSource() != 2) {
            MtkSpnOverride mtkSpnOverride = MtkSpnOverride.getInstance();
            String simOperator = TelephonyManager.getDefault().getSimOperator(i);
            int slotIndex = SubscriptionManager.getSlotIndex(i);
            logd("updateSubName, carrierName = " + simOperator + ", subId = " + i);
            if (SubscriptionManager.isValidSlotIndex(slotIndex)) {
                if (mtkSpnOverride.containsCarrierEx(simOperator)) {
                    strLookupOperatorName = mtkSpnOverride.lookupOperatorName(i, simOperator, true, mContext);
                    logd("SPN found, name = " + strLookupOperatorName);
                } else {
                    strLookupOperatorName = "CARD " + Integer.toString(slotIndex + 1);
                    logd("SPN not found, set name to " + strLookupOperatorName);
                }
                this.mSubscriptionManager.setDisplayName(strLookupOperatorName, i);
            }
        }
    }

    public void resetSimMountChangeState() {
        boolean z;
        int i = 0;
        while (true) {
            if (i < 4) {
                if (this.newSmlInfo[i] == this.oldSmlInfo[i]) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (z) {
            int i2 = this.newSmlInfo[0];
            int i3 = this.newSmlInfo[1];
            int i4 = this.newSmlInfo[2];
            int i5 = this.newSmlInfo[3];
            Intent intent = new Intent("com.mediatek.phone.ACTION_SIM_SLOT_SIM_MOUNT_CHANGE");
            intent.putExtra("DETECTED_TYPE", i2);
            intent.putExtra("SML_SIM_COUNT", i3);
            intent.putExtra("SML_SIM1_VALID", i4);
            intent.putExtra("SML_SIM2_VALID", i5);
            logd("Broadcasting ACTION_SIM_SLOT_SIM_MOUNT_CHANGE,  detected type: " + i2 + ", newSubCount: " + i3 + ", SIM 1 valid" + i4 + ", SIM 2 valid" + i5);
            mContext.sendBroadcast(intent);
            updateOldSmlInfo(i2, i3, i4, i5);
            return;
        }
        logd("resetSimMountChangeState no  need report ");
    }

    public void updateNewSmlInfo(int i, int i2, int i3, int i4) {
        synchronized (this.mLockUpdateNew) {
            this.newSmlInfo[0] = i;
            this.newSmlInfo[1] = i2;
            this.newSmlInfo[2] = i3;
            this.newSmlInfo[3] = i4;
        }
    }

    public void updateOldSmlInfo(int i, int i2, int i3, int i4) {
        synchronized (this.mLockUpdateOld) {
            this.oldSmlInfo[0] = i;
            this.oldSmlInfo[1] = i2;
            this.oldSmlInfo[2] = i3;
            this.oldSmlInfo[3] = i4;
        }
    }

    public int getOldDetectedType() {
        return this.oldSmlInfo[0];
    }

    private void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }
}
