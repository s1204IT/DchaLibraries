package com.android.internal.telephony;

import android.app.ActivityManager;
import android.app.UserSwitchObserver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.IPackageManager;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.euicc.EuiccProfileInfo;
import android.service.euicc.GetEuiccProfileInfoListResult;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.euicc.EuiccController;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionInfoUpdater extends Handler {
    public static final String CURR_SUBID = "curr_subid";
    protected static final int EVENT_GET_NETWORK_SELECTION_MODE_DONE = 2;
    private static final int EVENT_INVALID = -1;
    private static final int EVENT_REFRESH_EMBEDDED_SUBSCRIPTIONS = 12;
    private static final int EVENT_SIM_ABSENT = 4;
    private static final int EVENT_SIM_IMSI = 11;
    private static final int EVENT_SIM_IO_ERROR = 6;
    protected static final int EVENT_SIM_LOADED = 3;
    private static final int EVENT_SIM_LOCKED = 5;
    private static final int EVENT_SIM_NOT_READY = 9;
    protected static final int EVENT_SIM_READY = 10;
    private static final int EVENT_SIM_RESTRICTED = 8;
    private static final int EVENT_SIM_UNKNOWN = 7;
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
    protected static Phone[] mPhone;
    private CarrierServiceBindHelper mCarrierServiceBindHelper;
    protected int mCurrentlyActiveUserId;
    private EuiccManager mEuiccManager;
    protected IPackageManager mPackageManager;
    protected SubscriptionManager mSubscriptionManager;
    protected static final int PROJECT_SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    protected static Context mContext = null;
    protected static String[] mIccId = new String[PROJECT_SIM_NUM];
    protected static int[] mInsertSimState = new int[PROJECT_SIM_NUM];
    private static int[] sSimCardState = new int[PROJECT_SIM_NUM];
    private static int[] sSimApplicationState = new int[PROJECT_SIM_NUM];

    public SubscriptionInfoUpdater(Looper looper, Context context, Phone[] phoneArr, CommandsInterface[] commandsInterfaceArr) {
        super(looper);
        this.mSubscriptionManager = null;
        logd("Constructor invoked");
        mContext = context;
        mPhone = phoneArr;
        this.mSubscriptionManager = SubscriptionManager.from(mContext);
        this.mEuiccManager = (EuiccManager) mContext.getSystemService("euicc");
        this.mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        this.mCarrierServiceBindHelper = new CarrierServiceBindHelper(mContext);
        initializeCarrierApps();
    }

    private void initializeCarrierApps() {
        this.mCurrentlyActiveUserId = 0;
        try {
            ActivityManager.getService().registerUserSwitchObserver(new UserSwitchObserver() {
                public void onUserSwitching(int i, IRemoteCallback iRemoteCallback) throws RemoteException {
                    SubscriptionInfoUpdater.this.mCurrentlyActiveUserId = i;
                    CarrierAppUtils.disableCarrierAppsUntilPrivileged(SubscriptionInfoUpdater.mContext.getOpPackageName(), SubscriptionInfoUpdater.this.mPackageManager, TelephonyManager.getDefault(), SubscriptionInfoUpdater.mContext.getContentResolver(), SubscriptionInfoUpdater.this.mCurrentlyActiveUserId);
                    if (iRemoteCallback != null) {
                        try {
                            iRemoteCallback.sendResult((Bundle) null);
                        } catch (RemoteException e) {
                        }
                    }
                }
            }, LOG_TAG);
            this.mCurrentlyActiveUserId = ActivityManager.getService().getCurrentUser().id;
        } catch (RemoteException e) {
            logd("Couldn't get current user ID; guessing it's 0: " + e.getMessage());
        }
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(mContext.getOpPackageName(), this.mPackageManager, TelephonyManager.getDefault(), mContext.getContentResolver(), this.mCurrentlyActiveUserId);
    }

    public void updateInternalIccState(String str, String str2, int i) {
        logd("updateInternalIccState to simStatus " + str + " reason " + str2 + " slotId " + i);
        int iInternalIccStateToMessage = internalIccStateToMessage(str);
        if (iInternalIccStateToMessage != -1) {
            sendMessage(obtainMessage(iInternalIccStateToMessage, i, -1, str2));
        }
    }

    private int internalIccStateToMessage(String str) {
        switch (str) {
            case "ABSENT":
                return 4;
            case "UNKNOWN":
                return 7;
            case "CARD_IO_ERROR":
                return 6;
            case "CARD_RESTRICTED":
                return 8;
            case "NOT_READY":
                return 9;
            case "LOCKED":
                return 5;
            case "LOADED":
                return 3;
            case "READY":
                return 10;
            case "IMSI":
                return 11;
            default:
                logd("Ignoring simStatus: " + str);
                return -1;
        }
    }

    protected boolean isAllIccIdQueryDone() {
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mIccId[i] == null) {
                logd("Wait for SIM" + (i + 1) + " IccId");
                return false;
            }
        }
        logd("All IccIds query complete");
        return true;
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 2:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                Integer num = (Integer) asyncResult.userObj;
                if (asyncResult.exception == null && asyncResult.result != null) {
                    if (((int[]) asyncResult.result)[0] == 1) {
                        mPhone[num.intValue()].setNetworkSelectionModeAutomatic(null);
                        return;
                    }
                    return;
                }
                logd("EVENT_GET_NETWORK_SELECTION_MODE_DONE: error getting network mode.");
                return;
            case 3:
                handleSimLoaded(message.arg1);
                return;
            case 4:
                handleSimAbsent(message.arg1);
                return;
            case 5:
                handleSimLocked(message.arg1, (String) message.obj);
                return;
            case 6:
                handleSimError(message.arg1);
                return;
            case 7:
                updateCarrierServices(message.arg1, "UNKNOWN");
                broadcastSimStateChanged(message.arg1, "UNKNOWN", null);
                broadcastSimCardStateChanged(message.arg1, 0);
                broadcastSimApplicationStateChanged(message.arg1, 0);
                return;
            case 8:
                updateCarrierServices(message.arg1, "CARD_RESTRICTED");
                broadcastSimStateChanged(message.arg1, "CARD_RESTRICTED", "CARD_RESTRICTED");
                broadcastSimCardStateChanged(message.arg1, 9);
                broadcastSimApplicationStateChanged(message.arg1, 6);
                return;
            case 9:
                broadcastSimStateChanged(message.arg1, "NOT_READY", null);
                broadcastSimCardStateChanged(message.arg1, 11);
                broadcastSimApplicationStateChanged(message.arg1, 6);
                break;
            case 10:
                broadcastSimStateChanged(message.arg1, "READY", null);
                broadcastSimCardStateChanged(message.arg1, 11);
                broadcastSimApplicationStateChanged(message.arg1, 6);
                return;
            case 11:
                broadcastSimStateChanged(message.arg1, "IMSI", null);
                return;
            case 12:
                break;
            default:
                logd("Unknown msg:" + message.what);
                return;
        }
        if (updateEmbeddedSubscriptions()) {
            SubscriptionController.getInstance().notifySubscriptionInfoChanged();
        }
        if (message.obj != null) {
            ((Runnable) message.obj).run();
        }
    }

    void requestEmbeddedSubscriptionInfoListRefresh(Runnable runnable) {
        sendMessage(obtainMessage(12, runnable));
    }

    protected void handleSimLocked(int i, String str) {
        if (mIccId[i] != null && mIccId[i].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (i + 1) + " hot plug in");
            mIccId[i] = null;
        }
        String str2 = mIccId[i];
        if (str2 == null) {
            IccCard iccCard = mPhone[i].getIccCard();
            if (iccCard == null) {
                logd("handleSimLocked: IccCard null");
                return;
            }
            IccRecords iccRecords = iccCard.getIccRecords();
            if (iccRecords == null) {
                logd("handleSimLocked: IccRecords null");
                return;
            } else {
                if (IccUtils.stripTrailingFs(iccRecords.getFullIccId()) == null) {
                    logd("handleSimLocked: IccID null");
                    return;
                }
                mIccId[i] = IccUtils.stripTrailingFs(iccRecords.getFullIccId());
            }
        } else {
            logd("NOT Querying IccId its already set sIccid[" + i + "]=" + str2);
        }
        if (isAllIccIdQueryDone()) {
            updateSubscriptionInfoByIccId();
        }
        updateCarrierServices(i, "LOCKED");
        broadcastSimStateChanged(i, "LOCKED", str);
        broadcastSimCardStateChanged(i, 11);
        broadcastSimApplicationStateChanged(i, getSimStateFromLockedReason(str));
    }

    protected static int getSimStateFromLockedReason(String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != -1733499378) {
            if (iHashCode != 79221) {
                if (iHashCode != 79590) {
                    b = (iHashCode == 190660331 && str.equals("PERM_DISABLED")) ? (byte) 3 : (byte) -1;
                } else if (str.equals("PUK")) {
                    b = 1;
                }
            } else if (str.equals("PIN")) {
                b = 0;
            }
        } else if (str.equals("NETWORK")) {
            b = 2;
        }
        switch (b) {
            case 0:
                return 2;
            case 1:
                return 3;
            case 2:
                return 4;
            case 3:
                return 7;
            default:
                Rlog.e(LOG_TAG, "Unexpected SIM locked reason " + str);
                return 0;
        }
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
        mIccId[i] = IccUtils.stripTrailingFs(iccRecords.getFullIccId());
        if (isAllIccIdQueryDone()) {
            updateSubscriptionInfoByIccId();
            for (int i2 : this.mSubscriptionManager.getActiveSubscriptionIdList()) {
                TelephonyManager telephonyManager = TelephonyManager.getDefault();
                String simOperatorNumeric = telephonyManager.getSimOperatorNumeric(i2);
                int phoneId = SubscriptionController.getInstance().getPhoneId(i2);
                if (!TextUtils.isEmpty(simOperatorNumeric)) {
                    if (i2 == SubscriptionController.getInstance().getDefaultSubId()) {
                        MccTable.updateMccMncConfiguration(mContext, simOperatorNumeric, false);
                    }
                    SubscriptionController.getInstance().setMccMnc(simOperatorNumeric, i2);
                } else {
                    logd("EVENT_RECORDS_LOADED Operator name is null");
                }
                String line1Number = telephonyManager.getLine1Number(i2);
                mContext.getContentResolver();
                if (line1Number != null) {
                    SubscriptionController.getInstance().setDisplayNumber(line1Number, i2);
                }
                SubscriptionInfo activeSubscriptionInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(i2);
                String simOperatorName = telephonyManager.getSimOperatorName(i2);
                if (activeSubscriptionInfo != null && activeSubscriptionInfo.getNameSource() != 2) {
                    if (TextUtils.isEmpty(simOperatorName)) {
                        simOperatorName = "CARD " + Integer.toString(phoneId + 1);
                    }
                    logd("sim name = " + simOperatorName);
                    SubscriptionController.getInstance().setDisplayName(simOperatorName, i2);
                }
                SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                if (defaultSharedPreferences.getInt(CURR_SUBID + phoneId, -1) != i2) {
                    int intAtIndex = Settings.Global.getInt(mPhone[phoneId].getContext().getContentResolver(), "preferred_network_mode" + i2, -1);
                    if (intAtIndex == -1) {
                        intAtIndex = RILConstants.PREFERRED_NETWORK_MODE;
                        try {
                            intAtIndex = TelephonyManager.getIntAtIndex(mContext.getContentResolver(), "preferred_network_mode", phoneId);
                        } catch (Settings.SettingNotFoundException e) {
                            Rlog.e(LOG_TAG, "Settings Exception Reading Value At Index for Settings.Global.PREFERRED_NETWORK_MODE");
                        }
                        Settings.Global.putInt(mPhone[phoneId].getContext().getContentResolver(), "preferred_network_mode" + i2, intAtIndex);
                    }
                    mPhone[phoneId].setPreferredNetworkType(intAtIndex, null);
                    mPhone[phoneId].getNetworkSelectionMode(obtainMessage(2, new Integer(phoneId)));
                    SharedPreferences.Editor editorEdit = defaultSharedPreferences.edit();
                    editorEdit.putInt(CURR_SUBID + phoneId, i2);
                    editorEdit.apply();
                }
            }
        }
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(mContext.getOpPackageName(), this.mPackageManager, TelephonyManager.getDefault(), mContext.getContentResolver(), this.mCurrentlyActiveUserId);
        broadcastSimStateChanged(i, "LOADED", null);
        broadcastSimCardStateChanged(i, 11);
        broadcastSimApplicationStateChanged(i, 10);
        updateCarrierServices(i, "LOADED");
    }

    protected void updateCarrierServices(int i, String str) {
        ((CarrierConfigManager) mContext.getSystemService("carrier_config")).updateConfigForPhoneId(i, str);
        this.mCarrierServiceBindHelper.updateForPhoneId(i, str);
    }

    protected void handleSimAbsent(int i) {
        if (mIccId[i] != null && !mIccId[i].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (i + 1) + " hot plug out");
        }
        mIccId[i] = ICCID_STRING_FOR_NO_SIM;
        if (isAllIccIdQueryDone()) {
            updateSubscriptionInfoByIccId();
        }
        updateCarrierServices(i, "ABSENT");
        broadcastSimStateChanged(i, "ABSENT", null);
        broadcastSimCardStateChanged(i, 1);
        broadcastSimApplicationStateChanged(i, 6);
    }

    protected void handleSimError(int i) {
        if (mIccId[i] != null && !mIccId[i].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (i + 1) + " Error ");
        }
        mIccId[i] = ICCID_STRING_FOR_NO_SIM;
        if (isAllIccIdQueryDone()) {
            updateSubscriptionInfoByIccId();
        }
        updateCarrierServices(i, "CARD_IO_ERROR");
        broadcastSimStateChanged(i, "CARD_IO_ERROR", "CARD_IO_ERROR");
        broadcastSimCardStateChanged(i, 8);
        broadcastSimApplicationStateChanged(i, 6);
    }

    protected synchronized void updateSubscriptionInfoByIccId() {
        int size;
        logd("updateSubscriptionInfoByIccId:+ Start");
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            mInsertSimState[i] = 0;
        }
        int i2 = PROJECT_SIM_NUM;
        for (int i3 = 0; i3 < PROJECT_SIM_NUM; i3++) {
            if (ICCID_STRING_FOR_NO_SIM.equals(mIccId[i3])) {
                i2--;
                mInsertSimState[i3] = -99;
            }
        }
        logd("insertedSimCount = " + i2);
        if (SubscriptionController.getInstance().getActiveSubIdList().length > i2) {
            SubscriptionController.getInstance().clearSubInfo();
        }
        for (int i4 = 0; i4 < PROJECT_SIM_NUM; i4++) {
            if (mInsertSimState[i4] != -99) {
                int i5 = 2;
                for (int i6 = i4 + 1; i6 < PROJECT_SIM_NUM; i6++) {
                    if (mInsertSimState[i6] == 0 && mIccId[i4].equals(mIccId[i6])) {
                        mInsertSimState[i4] = 1;
                        mInsertSimState[i6] = i5;
                        i5++;
                    }
                }
            }
        }
        ContentResolver contentResolver = mContext.getContentResolver();
        String[] strArr = new String[PROJECT_SIM_NUM];
        String[] strArr2 = new String[PROJECT_SIM_NUM];
        for (int i7 = 0; i7 < PROJECT_SIM_NUM; i7++) {
            strArr[i7] = null;
            List<SubscriptionInfo> subInfoUsingSlotIndexPrivileged = SubscriptionController.getInstance().getSubInfoUsingSlotIndexPrivileged(i7, false);
            strArr2[i7] = IccUtils.getDecimalSubstring(mIccId[i7]);
            if (subInfoUsingSlotIndexPrivileged != null && subInfoUsingSlotIndexPrivileged.size() > 0) {
                strArr[i7] = subInfoUsingSlotIndexPrivileged.get(0).getIccId();
                logd("updateSubscriptionInfoByIccId: oldSubId = " + subInfoUsingSlotIndexPrivileged.get(0).getSubscriptionId());
                if (mInsertSimState[i7] == 0 && !mIccId[i7].equals(strArr[i7]) && (strArr2[i7] == null || !strArr2[i7].equals(strArr[i7]))) {
                    mInsertSimState[i7] = -1;
                }
                if (mInsertSimState[i7] != 0) {
                    ContentValues contentValues = new ContentValues(1);
                    contentValues.put("sim_id", (Integer) (-1));
                    contentResolver.update(SubscriptionManager.CONTENT_URI, contentValues, "_id=" + Integer.toString(subInfoUsingSlotIndexPrivileged.get(0).getSubscriptionId()), null);
                    SubscriptionController.getInstance().refreshCachedActiveSubscriptionInfoList();
                }
            } else {
                if (mInsertSimState[i7] == 0) {
                    mInsertSimState[i7] = -1;
                }
                strArr[i7] = ICCID_STRING_FOR_NO_SIM;
                logd("updateSubscriptionInfoByIccId: No SIM in slot " + i7 + " last time");
            }
        }
        for (int i8 = 0; i8 < PROJECT_SIM_NUM; i8++) {
            logd("updateSubscriptionInfoByIccId: oldIccId[" + i8 + "] = " + strArr[i8] + ", sIccId[" + i8 + "] = " + mIccId[i8]);
        }
        for (int i9 = 0; i9 < PROJECT_SIM_NUM; i9++) {
            if (mInsertSimState[i9] == -99) {
                logd("updateSubscriptionInfoByIccId: No SIM inserted in slot " + i9 + " this time");
            } else {
                if (mInsertSimState[i9] > 0) {
                    this.mSubscriptionManager.addSubscriptionInfoRecord(mIccId[i9] + Integer.toString(mInsertSimState[i9]), i9);
                    logd("SUB" + (i9 + 1) + " has invalid IccId");
                } else {
                    logd("updateSubscriptionInfoByIccId: adding subscription info record: iccid: " + mIccId[i9] + "slot: " + i9);
                    this.mSubscriptionManager.addSubscriptionInfoRecord(mIccId[i9], i9);
                }
                if (isNewSim(mIccId[i9], strArr2[i9], strArr)) {
                    switch (i9) {
                    }
                    mInsertSimState[i9] = -2;
                }
            }
        }
        for (int i10 = 0; i10 < PROJECT_SIM_NUM; i10++) {
            if (mInsertSimState[i10] == -1) {
                mInsertSimState[i10] = -3;
            }
            logd("updateSubscriptionInfoByIccId: sInsertSimState[" + i10 + "] = " + mInsertSimState[i10]);
        }
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList != null) {
            size = activeSubscriptionInfoList.size();
        } else {
            size = 0;
        }
        logd("updateSubscriptionInfoByIccId: nSubCount = " + size);
        for (int i11 = 0; i11 < size; i11++) {
            SubscriptionInfo subscriptionInfo = activeSubscriptionInfoList.get(i11);
            String line1Number = TelephonyManager.getDefault().getLine1Number(subscriptionInfo.getSubscriptionId());
            if (line1Number != null) {
                ContentValues contentValues2 = new ContentValues(1);
                contentValues2.put("number", line1Number);
                contentResolver.update(SubscriptionManager.CONTENT_URI, contentValues2, "_id=" + Integer.toString(subscriptionInfo.getSubscriptionId()), null);
                SubscriptionController.getInstance().refreshCachedActiveSubscriptionInfoList();
            }
        }
        SubscriptionManager subscriptionManager = this.mSubscriptionManager;
        SubscriptionManager subscriptionManager2 = this.mSubscriptionManager;
        subscriptionManager.setDefaultDataSubId(SubscriptionManager.getDefaultDataSubscriptionId());
        updateEmbeddedSubscriptions();
        SubscriptionController.getInstance().notifySubscriptionInfoChanged();
        logd("updateSubscriptionInfoByIccId:- SubscriptionInfo update complete");
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public boolean updateEmbeddedSubscriptions() {
        GetEuiccProfileInfoListResult getEuiccProfileInfoListResultBlockingGetEuiccProfileInfoList;
        EuiccProfileInfo[] euiccProfileInfoArr;
        if (!this.mEuiccManager.isEnabled() || (getEuiccProfileInfoListResultBlockingGetEuiccProfileInfoList = EuiccController.get().blockingGetEuiccProfileInfoList()) == null) {
            return false;
        }
        if (getEuiccProfileInfoListResultBlockingGetEuiccProfileInfoList.getResult() == 0) {
            List profiles = getEuiccProfileInfoListResultBlockingGetEuiccProfileInfoList.getProfiles();
            euiccProfileInfoArr = (profiles == null || profiles.size() == 0) ? new EuiccProfileInfo[0] : (EuiccProfileInfo[]) profiles.toArray(new EuiccProfileInfo[profiles.size()]);
        } else {
            logd("updatedEmbeddedSubscriptions: error " + getEuiccProfileInfoListResultBlockingGetEuiccProfileInfoList.getResult() + " listing profiles");
            euiccProfileInfoArr = new EuiccProfileInfo[0];
        }
        boolean isRemovable = getEuiccProfileInfoListResultBlockingGetEuiccProfileInfoList.getIsRemovable();
        String[] strArr = new String[euiccProfileInfoArr.length];
        for (int i = 0; i < euiccProfileInfoArr.length; i++) {
            strArr[i] = euiccProfileInfoArr[i].getIccid();
        }
        List<SubscriptionInfo> subscriptionInfoListForEmbeddedSubscriptionUpdate = SubscriptionController.getInstance().getSubscriptionInfoListForEmbeddedSubscriptionUpdate(strArr, isRemovable);
        ContentResolver contentResolver = mContext.getContentResolver();
        int length = euiccProfileInfoArr.length;
        int i2 = 0;
        boolean z = false;
        while (i2 < length) {
            EuiccProfileInfo euiccProfileInfo = euiccProfileInfoArr[i2];
            int iFindSubscriptionInfoForIccid = findSubscriptionInfoForIccid(subscriptionInfoListForEmbeddedSubscriptionUpdate, euiccProfileInfo.getIccid());
            if (iFindSubscriptionInfoForIccid < 0) {
                SubscriptionController.getInstance().insertEmptySubInfoRecord(euiccProfileInfo.getIccid(), -1);
            } else {
                subscriptionInfoListForEmbeddedSubscriptionUpdate.remove(iFindSubscriptionInfoForIccid);
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put("is_embedded", (Integer) 1);
            List uiccAccessRules = euiccProfileInfo.getUiccAccessRules();
            contentValues.put("access_rules", uiccAccessRules == null || uiccAccessRules.size() == 0 ? null : UiccAccessRule.encodeRules((UiccAccessRule[]) uiccAccessRules.toArray(new UiccAccessRule[uiccAccessRules.size()])));
            contentValues.put("is_removable", Boolean.valueOf(isRemovable));
            contentValues.put("display_name", euiccProfileInfo.getNickname());
            contentValues.put("name_source", (Integer) 2);
            contentResolver.update(SubscriptionManager.CONTENT_URI, contentValues, "icc_id=\"" + euiccProfileInfo.getIccid() + "\"", null);
            SubscriptionController.getInstance().refreshCachedActiveSubscriptionInfoList();
            i2++;
            z = true;
        }
        if (subscriptionInfoListForEmbeddedSubscriptionUpdate.isEmpty()) {
            return z;
        }
        ArrayList arrayList = new ArrayList();
        for (int i3 = 0; i3 < subscriptionInfoListForEmbeddedSubscriptionUpdate.size(); i3++) {
            SubscriptionInfo subscriptionInfo = subscriptionInfoListForEmbeddedSubscriptionUpdate.get(i3);
            if (subscriptionInfo.isEmbedded()) {
                arrayList.add("\"" + subscriptionInfo.getIccId() + "\"");
            }
        }
        String str = "icc_id IN (" + TextUtils.join(",", arrayList) + ")";
        ContentValues contentValues2 = new ContentValues();
        contentValues2.put("is_embedded", (Integer) 0);
        contentResolver.update(SubscriptionManager.CONTENT_URI, contentValues2, str, null);
        SubscriptionController.getInstance().refreshCachedActiveSubscriptionInfoList();
        return true;
    }

    private static int findSubscriptionInfoForIccid(List<SubscriptionInfo> list, String str) {
        for (int i = 0; i < list.size(); i++) {
            if (TextUtils.equals(str, list.get(i).getIccId())) {
                return i;
            }
        }
        return -1;
    }

    protected boolean isNewSim(String str, String str2, String[] strArr) {
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < PROJECT_SIM_NUM) {
                if (str.equals(strArr[i]) || (str2 != null && str2.equals(strArr[i]))) {
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

    protected void broadcastSimStateChanged(int i, String str, String str2) {
        Intent intent = new Intent("android.intent.action.SIM_STATE_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("phoneName", "Phone");
        intent.putExtra("ss", str);
        intent.putExtra("reason", str2);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, i);
        logd("Broadcasting intent ACTION_SIM_STATE_CHANGED " + str + " reason " + str2 + " for mCardIndex: " + i);
        IntentBroadcaster.getInstance().broadcastStickyIntent(intent, i);
    }

    protected void broadcastSimCardStateChanged(int i, int i2) {
        if (i2 != sSimCardState[i]) {
            sSimCardState[i] = i2;
            Intent intent = new Intent("android.telephony.action.SIM_CARD_STATE_CHANGED");
            intent.addFlags(67108864);
            intent.addFlags(16777216);
            intent.putExtra("android.telephony.extra.SIM_STATE", i2);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, i);
            logd("Broadcasting intent ACTION_SIM_CARD_STATE_CHANGED " + simStateString(i2) + " for phone: " + i);
            mContext.sendBroadcast(intent, "android.permission.READ_PRIVILEGED_PHONE_STATE");
        }
    }

    protected void broadcastSimApplicationStateChanged(int i, int i2) {
        if (i2 != sSimApplicationState[i]) {
            if (i2 != 6 || sSimApplicationState[i] != 0) {
                sSimApplicationState[i] = i2;
                Intent intent = new Intent("android.telephony.action.SIM_APPLICATION_STATE_CHANGED");
                intent.addFlags(16777216);
                intent.addFlags(67108864);
                intent.putExtra("android.telephony.extra.SIM_STATE", i2);
                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, i);
                logd("Broadcasting intent ACTION_SIM_APPLICATION_STATE_CHANGED " + simStateString(i2) + " for phone: " + i);
                mContext.sendBroadcast(intent, "android.permission.READ_PRIVILEGED_PHONE_STATE");
            }
        }
    }

    private static String simStateString(int i) {
        switch (i) {
            case 0:
                return "UNKNOWN";
            case 1:
                return "ABSENT";
            case 2:
                return "PIN_REQUIRED";
            case 3:
                return "PUK_REQUIRED";
            case 4:
                return "NETWORK_LOCKED";
            case 5:
                return "READY";
            case 6:
                return "NOT_READY";
            case 7:
                return "PERM_DISABLED";
            case 8:
                return "CARD_IO_ERROR";
            case 9:
                return "CARD_RESTRICTED";
            case 10:
                return "LOADED";
            case 11:
                return "PRESENT";
            default:
                return "INVALID";
        }
    }

    private void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("SubscriptionInfoUpdater:");
        this.mCarrierServiceBindHelper.dump(fileDescriptor, printWriter, strArr);
    }
}
