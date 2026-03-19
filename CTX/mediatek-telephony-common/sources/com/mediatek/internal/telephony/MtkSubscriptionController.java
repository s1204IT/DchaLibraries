package com.mediatek.internal.telephony;

import android.R;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.UiccAccessRule;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyPermissions;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.MtkSpnOverride;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MtkSubscriptionController extends SubscriptionController {
    private static final String LOG_TAG = "MtkSubCtrl";
    static final int MAX_LOCAL_LOG_LINES = 500;
    private String[] PROPERTY_ICCID;
    private int lastPhoneId;
    private boolean mIsReady;
    private static final boolean ENGDEBUG = TextUtils.equals(Build.TYPE, "eng");
    private static final boolean sIsOP01 = DataSubConstants.OPERATOR_OP01.equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, ""));
    private static final boolean sIsOP02 = DataSubConstants.OPERATOR_OP02.equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, ""));
    private static MtkSubscriptionController sMtkInstance = null;
    private static Intent sStickyIntent = null;

    protected static MtkSubscriptionController mtkInit(Phone phone) {
        MtkSubscriptionController mtkSubscriptionController;
        synchronized (MtkSubscriptionController.class) {
            if (sMtkInstance == null) {
                sMtkInstance = new MtkSubscriptionController(phone);
                Rlog.d(LOG_TAG, "mtkInit, sMtkInstance = " + sMtkInstance);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sMtkInstance = " + sMtkInstance);
            }
            mtkSubscriptionController = sMtkInstance;
        }
        return mtkSubscriptionController;
    }

    protected static MtkSubscriptionController mtkInit(Context context, CommandsInterface[] commandsInterfaceArr) {
        MtkSubscriptionController mtkSubscriptionController;
        synchronized (MtkSubscriptionController.class) {
            if (sMtkInstance == null) {
                sMtkInstance = new MtkSubscriptionController(context);
                Rlog.d(LOG_TAG, "mtkInit, sMtkInstance = " + sMtkInstance);
                MtkSubscriptionControllerEx.MtkInitStub(context);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sMtkInstance = " + sMtkInstance);
            }
            mtkSubscriptionController = sMtkInstance;
        }
        return mtkSubscriptionController;
    }

    protected MtkSubscriptionController(Context context) {
        super(context);
        this.mIsReady = false;
        this.lastPhoneId = Integer.MAX_VALUE;
        this.PROPERTY_ICCID = new String[]{"vendor.ril.iccid.sim1", "vendor.ril.iccid.sim2", "vendor.ril.iccid.sim3", "vendor.ril.iccid.sim4"};
    }

    public static MtkSubscriptionController getMtkInstance() {
        MtkSubscriptionController mtkSubscriptionController;
        synchronized (MtkSubscriptionController.class) {
            mtkSubscriptionController = sMtkInstance;
        }
        return mtkSubscriptionController;
    }

    protected MtkSubscriptionController(Phone phone) {
        super(phone);
        this.mIsReady = false;
        this.lastPhoneId = Integer.MAX_VALUE;
        this.PROPERTY_ICCID = new String[]{"vendor.ril.iccid.sim1", "vendor.ril.iccid.sim2", "vendor.ril.iccid.sim3", "vendor.ril.iccid.sim4"};
    }

    public void notifySubscriptionInfoChanged() {
        try {
            ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry")).notifySubscriptionInfoChanged();
        } catch (RemoteException e) {
        }
        broadcastSimInfoContentChanged(null);
    }

    protected SubscriptionInfo getSubInfoRecord(Cursor cursor) {
        UiccAccessRule[] uiccAccessRuleArrDecodeRules;
        int i = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
        String string = cursor.getString(cursor.getColumnIndexOrThrow("icc_id"));
        int i2 = cursor.getInt(cursor.getColumnIndexOrThrow("sim_id"));
        String string2 = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
        String string3 = cursor.getString(cursor.getColumnIndexOrThrow("carrier_name"));
        int i3 = cursor.getInt(cursor.getColumnIndexOrThrow("name_source"));
        int i4 = cursor.getInt(cursor.getColumnIndexOrThrow("color"));
        String string4 = cursor.getString(cursor.getColumnIndexOrThrow(PplMessageManager.PendingMessage.KEY_NUMBER));
        int i5 = cursor.getInt(cursor.getColumnIndexOrThrow("data_roaming"));
        Bitmap bitmapDecodeResource = BitmapFactory.decodeResource(this.mContext.getResources(), R.drawable.ic_media_route_connected_light_29_mtrl);
        int i6 = cursor.getInt(cursor.getColumnIndexOrThrow("mcc"));
        int i7 = cursor.getInt(cursor.getColumnIndexOrThrow("mnc"));
        String subscriptionCountryIso = getSubscriptionCountryIso(i);
        boolean z = cursor.getInt(cursor.getColumnIndexOrThrow("is_embedded")) == 1;
        if (z) {
            uiccAccessRuleArrDecodeRules = UiccAccessRule.decodeRules(cursor.getBlob(cursor.getColumnIndexOrThrow("access_rules")));
        } else {
            uiccAccessRuleArrDecodeRules = null;
        }
        UiccAccessRule[] uiccAccessRuleArr = uiccAccessRuleArrDecodeRules;
        String line1Number = this.mTelephonyManager.getLine1Number(i);
        return new MtkSubscriptionInfo(i, string, i2, string2, string3, i3, i4, (TextUtils.isEmpty(line1Number) || line1Number.equals(string4)) ? string4 : line1Number, i5, bitmapDecodeResource, i6, i7, subscriptionCountryIso, z, uiccAccessRuleArr);
    }

    public int addSubInfoRecord(String str, int i) {
        boolean z;
        String cardId;
        logdl("[addSubInfoRecord]+ iccId:" + SubscriptionInfo.givePrintableIccid(str) + " slotIndex:" + i);
        enforceModifyPhoneState("addSubInfoRecord");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (str == null) {
                logdl("[addSubInfoRecord]- null iccId");
                return -1;
            }
            ContentResolver contentResolver = this.mContext.getContentResolver();
            Cursor cursorQuery = contentResolver.query(SubscriptionManager.CONTENT_URI, new String[]{"_id", "sim_id", "name_source", "icc_id", "card_id"}, "icc_id=? OR icc_id=? OR icc_id=?", new String[]{str, IccUtils.getDecimalSubstring(str), str.toLowerCase()}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        int i2 = cursorQuery.getInt(0);
                        int i3 = cursorQuery.getInt(1);
                        int i4 = cursorQuery.getInt(2);
                        String string = cursorQuery.getString(3);
                        String string2 = cursorQuery.getString(4);
                        ContentValues contentValues = new ContentValues();
                        if (i != i3) {
                            contentValues.put("sim_id", Integer.valueOf(i));
                        }
                        z = i4 != 2;
                        if (string != null && ((string.length() < str.length() && string.equals(IccUtils.getDecimalSubstring(str))) || (!str.toLowerCase().equals(str) && str.toLowerCase().equals(string)))) {
                            contentValues.put("icc_id", str);
                        }
                        UiccCard uiccCardForPhone = UiccController.getInstance().getUiccCardForPhone(i);
                        if (uiccCardForPhone != null && (cardId = uiccCardForPhone.getCardId()) != null && cardId != string2) {
                            contentValues.put("card_id", cardId);
                        }
                        if (contentValues.size() > 0) {
                            contentResolver.update(SubscriptionManager.CONTENT_URI, contentValues, "_id=" + Long.toString(i2), null);
                            refreshCachedActiveSubscriptionInfoList();
                        }
                        logdl("[addSubInfoRecord] Record already exists");
                    } else {
                        logdl("[addSubInfoRecord] New record created: " + insertEmptySubInfoRecord(str, i));
                        z = true;
                    }
                } catch (Throwable th) {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            } else {
                logdl("[addSubInfoRecord] New record created: " + insertEmptySubInfoRecord(str, i));
                z = true;
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            Cursor cursorQuery2 = contentResolver.query(SubscriptionManager.CONTENT_URI, null, "sim_id=?", new String[]{String.valueOf(i)}, null);
            if (cursorQuery2 != null) {
                try {
                    if (cursorQuery2.moveToFirst()) {
                        do {
                            int i5 = cursorQuery2.getInt(cursorQuery2.getColumnIndexOrThrow("_id"));
                            sSlotIndexToSubId.put(Integer.valueOf(i), Integer.valueOf(i5));
                            int activeSubInfoCountMax = getActiveSubInfoCountMax();
                            int defaultSubId = getDefaultSubId();
                            logdl("[addSubInfoRecord] sSlotIndexToSubId.size=" + sSlotIndexToSubId.size() + " slotIndex=" + i + " subId=" + i5 + " mDefaultFallbackSubId=" + mDefaultFallbackSubId + " defaultSubId=" + defaultSubId + " simCount=" + activeSubInfoCountMax);
                            if (!SubscriptionManager.isValidSubscriptionId(defaultSubId) || activeSubInfoCountMax == 1 || !isActiveSubId(defaultSubId) || !isActiveSubId(mDefaultFallbackSubId) || (mDefaultFallbackSubId == i5 && this.lastPhoneId != i)) {
                                setDefaultFallbackSubId(i5);
                                this.lastPhoneId = i;
                            }
                            if (activeSubInfoCountMax == 1) {
                                logdl("[addSubInfoRecord] one sim set defaults to subId=" + i5);
                                setDefaultDataSubId(i5);
                                setDefaultSmsSubId(i5);
                                setDefaultVoiceSubId(i5);
                            }
                            logdl("[addSubInfoRecord] hashmap(" + i + "," + i5 + ")");
                        } while (cursorQuery2.moveToNext());
                    }
                } finally {
                    if (cursorQuery2 != null) {
                        cursorQuery2.close();
                    }
                }
            }
            int subIdUsingPhoneId = getSubIdUsingPhoneId(i);
            if (!SubscriptionManager.isValidSubscriptionId(subIdUsingPhoneId)) {
                logdl("[addSubInfoRecord]- getSubId failed invalid subId = " + subIdUsingPhoneId);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return -1;
            }
            if (z) {
                String simOperatorName = this.mTelephonyManager.getSimOperatorName(subIdUsingPhoneId);
                String simOperatorNumeric = this.mTelephonyManager.getSimOperatorNumeric(subIdUsingPhoneId);
                String strLookupOperatorNameForDisplayName = ("20404".equals(simOperatorNumeric) && TextUtils.isEmpty(simOperatorName)) ? "" : MtkSpnOverride.getInstance().lookupOperatorNameForDisplayName(subIdUsingPhoneId, simOperatorNumeric, true, this.mContext);
                if (ENGDEBUG) {
                    logd("[addSubInfoRecord]- simNumeric: " + simOperatorNumeric + ", simMvnoName: " + strLookupOperatorNameForDisplayName);
                }
                if (!TextUtils.isEmpty(strLookupOperatorNameForDisplayName)) {
                    simOperatorName = strLookupOperatorNameForDisplayName;
                } else if (TextUtils.isEmpty(simOperatorName)) {
                    simOperatorName = "CARD " + Integer.toString(i + 1);
                }
                ContentValues contentValues2 = new ContentValues();
                contentValues2.put("display_name", simOperatorName);
                contentResolver.update(SubscriptionManager.CONTENT_URI, contentValues2, "_id=" + Long.toString(subIdUsingPhoneId), null);
                logdl("[addSubInfoRecord] sim name = " + simOperatorName);
            }
            refreshCachedActiveSubscriptionInfoList();
            sPhones[i].updateDataConnectionTracker();
            logdl("[addSubInfoRecord]- info size=" + sSlotIndexToSubId.size());
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return 0;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int getSlotIndex(int i) {
        if (i == Integer.MAX_VALUE) {
            logd("[getSlotIndex]+ subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID");
            i = getDefaultSubId();
        }
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            logd("[getSlotIndex]- subId invalid");
            return -1;
        }
        if (sSlotIndexToSubId.size() == 0) {
            logd("[getSlotIndex]- size == 0, return SIM_NOT_INSERTED instead, subId =" + i);
            return -1;
        }
        for (Map.Entry entry : sSlotIndexToSubId.entrySet()) {
            int iIntValue = ((Integer) entry.getKey()).intValue();
            if (i == ((Integer) entry.getValue()).intValue()) {
                return iIntValue;
            }
        }
        logd("[getSlotIndex]- return INVALID_SIM_SLOT_INDEX, subId = " + i);
        return -1;
    }

    public int getPhoneId(int i) {
        if (i == Integer.MAX_VALUE) {
            i = getDefaultSubId();
            logdl("[getPhoneId] asked for default subId=" + i);
        }
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            if (i > (-2) - getActiveSubInfoCountMax()) {
                return (-2) - i;
            }
            return -1;
        }
        if (sSlotIndexToSubId.size() == 0) {
            int i2 = mDefaultPhoneId;
            logd("[getPhoneId]- no sims, returning default phoneId=" + i2 + ", subId" + i);
            return i2;
        }
        for (Map.Entry entry : sSlotIndexToSubId.entrySet()) {
            int iIntValue = ((Integer) entry.getKey()).intValue();
            if (i == ((Integer) entry.getValue()).intValue()) {
                return iIntValue;
            }
        }
        int i3 = mDefaultPhoneId;
        logdl("[getPhoneId]- subId=" + i + " not found return default phoneId=" + i3);
        return i3;
    }

    public int clearSubInfo() {
        enforceModifyPhoneState("clearSubInfo");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int size = sSlotIndexToSubId.size();
            if (size == 0) {
                logdl("[clearSubInfo]- no simInfo size=" + size);
                return 0;
            }
            setReadyState(false);
            sSlotIndexToSubId.clear();
            logdl("[clearSubInfo]- clear size=" + size);
            return size;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setDefaultDataSubId(int i) {
        setDefaultDataSubIdWithResult(i);
    }

    public void clearDefaultsForInactiveSubIds() {
        enforceModifyPhoneState("clearDefaultsForInactiveSubIds");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            List activeSubscriptionInfoList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            logdl("[clearDefaultsForInactiveSubIds] records: " + activeSubscriptionInfoList);
            if (!sIsOP01 && !sIsOP02 && shouldDefaultBeCleared(activeSubscriptionInfoList, getDefaultDataSubId())) {
                logd("[clearDefaultsForInactiveSubIds] clearing default data sub id");
                setDefaultDataSubId(-1);
            }
            if (shouldDefaultBeCleared(activeSubscriptionInfoList, getDefaultSmsSubId())) {
                logdl("[clearDefaultsForInactiveSubIds] clearing default sms sub id");
                setDefaultSmsSubId(-1);
            }
            if (shouldDefaultBeCleared(activeSubscriptionInfoList, getDefaultVoiceSubId())) {
                logdl("[clearDefaultsForInactiveSubIds] clearing default voice sub id");
                setDefaultVoiceSubId(-1);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int[] getActiveSubIdList() {
        HashSet hashSet = new HashSet(sSlotIndexToSubId.entrySet());
        int[] iArr = new int[getActiveSubInfoCountMax()];
        Iterator it = hashSet.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = ((Integer) ((Map.Entry) it.next()).getValue()).intValue();
            i++;
        }
        int[] iArr2 = new int[i];
        for (int i2 = 0; i2 < i; i2++) {
            iArr2[i2] = iArr[i2];
        }
        return iArr2;
    }

    public void setSubscriptionProperty(int i, String str, String str2) {
        super.setSubscriptionProperty(i, str, str2);
        Intent intent = new Intent("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        intent.putExtra("simDetectStatus", 4);
        intent.putExtra("simPropKey", str);
        notifySubscriptionInfoChanged(intent);
    }

    private void broadcastSimInfoContentChanged(Intent intent) {
        this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE"));
        Intent intent2 = new Intent("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        if (intent == null) {
            intent2.putExtra("simDetectStatus", 4);
            intent2.putExtra("simPropKey", "");
        }
        synchronized (MtkSubscriptionController.class) {
            if (intent == null) {
                intent = intent2;
            }
            sStickyIntent = intent;
            int intExtra = sStickyIntent.getIntExtra("simDetectStatus", 0);
            if (ENGDEBUG) {
                logd("broadcast intent ACTION_SUBINFO_RECORD_UPDATED with detectType:" + intExtra);
            }
            this.mContext.sendStickyBroadcast(sStickyIntent);
        }
    }

    public int clearSubInfoUsingPhoneId(int i) {
        enforceModifyPhoneState("clearSubInfoUsingPhoneId");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (!SubscriptionManager.isValidPhoneId(i)) {
                if (ENGDEBUG) {
                    logd("[clearSubInfoUsingPhoneId]- invalid phoneId=" + i);
                }
                return -1;
            }
            setReadyState(false);
            int size = sSlotIndexToSubId.size();
            if (size != 0) {
                sSlotIndexToSubId.remove(Integer.valueOf(i));
                return 1;
            }
            if (ENGDEBUG) {
                logdl("[clearSubInfoUsingPhoneId]- no simInfo size=" + size);
            }
            return 0;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean setDefaultDataSubIdWithResult(int i) {
        boolean z;
        int minRafSupported;
        enforceModifyPhoneState("setDefaultDataSubIdWithResult");
        if (i == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultDataSubIdWithResult called with DEFAULT_SUB_ID");
        }
        ProxyController proxyController = ProxyController.getInstance();
        int length = sPhones.length;
        logdl("[setDefaultDataSubIdWithResult] num phones=" + length + ", subId=" + i + ", Binder.getCallingPid and Binder.getCallingUid are " + Binder.getCallingPid() + "," + Binder.getCallingUid());
        if (proxyController != null) {
            try {
                if (SubscriptionManager.isValidSubscriptionId(i) || SystemProperties.getInt("ro.vendor.mtk_external_sim_support", 0) == 1) {
                    RadioAccessFamily[] radioAccessFamilyArr = new RadioAccessFamily[length];
                    int i2 = 0;
                    boolean z2 = false;
                    while (i2 < length) {
                        int subId = sPhones[i2].getSubId();
                        if (subId == i) {
                            minRafSupported = proxyController.getMaxRafSupported();
                            z = true;
                        } else {
                            z = z2;
                            minRafSupported = proxyController.getMinRafSupported();
                        }
                        logdl("[setDefaultDataSubIdWithResult] phoneId=" + i2 + " subId=" + subId + " RAF=" + minRafSupported);
                        radioAccessFamilyArr[i2] = new RadioAccessFamily(i2, minRafSupported);
                        i2++;
                        z2 = z;
                    }
                    if (z2) {
                        proxyController.setRadioCapability(radioAccessFamilyArr);
                    } else {
                        logdl("[setDefaultDataSubIdWithResult] no valid subId's found - not updating.");
                    }
                }
            } catch (RuntimeException e) {
                logd("[setDefaultDataSubIdWithResult] setRadioCapability: Runtime Exception");
                e.printStackTrace();
                return false;
            }
        }
        updateAllDataConnectionTrackers();
        Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_data_call", i);
        broadcastDefaultDataSubIdChanged(i);
        return true;
    }

    public MtkSubscriptionInfo getSubscriptionInfo(String str, int i) {
        MtkSubscriptionInfo subInfoRecord;
        if (str == null) {
            str = this.mContext.getOpPackageName();
        }
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getSubscriptionInfo")) {
            return null;
        }
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            logd("[getSubscriptionInfo]- invalid subId, subId =" + i);
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Cursor cursorQuery = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, "_id=?", new String[]{Long.toString(i)}, null);
            try {
                if (cursorQuery == null) {
                    logd("[getSubscriptionInfo]- Query fail");
                } else if (cursorQuery.moveToFirst() && (subInfoRecord = getSubInfoRecord(cursorQuery)) != null) {
                    logd("[getSubscriptionInfo]+ subId=" + i + ", subInfo=" + subInfoRecord);
                    return subInfoRecord;
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                logd("[getSubscriptionInfo]- subId=" + i + ",subInfo=null");
                return null;
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public MtkSubscriptionInfo getSubscriptionInfoForIccId(String str, String str2) {
        if (str == null) {
            str = this.mContext.getOpPackageName();
        }
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, -1, str, "getSubscriptionInfoForIccId")) {
            return null;
        }
        if (str2 == null) {
            logd("[getSubscriptionInfoForIccId]- null iccid");
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Cursor cursorQuery = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, "icc_id=?", new String[]{str2}, null);
            try {
                if (cursorQuery != null) {
                    while (cursorQuery.moveToNext()) {
                        MtkSubscriptionInfo subInfoRecord = getSubInfoRecord(cursorQuery);
                        if (subInfoRecord != null) {
                            logd("[getSubscriptionInfoForIccId]+ iccId=" + str2 + ", subInfo=" + subInfoRecord);
                            return subInfoRecord;
                        }
                    }
                } else {
                    logd("[getSubscriptionInfoForIccId]- Query fail");
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                logd("[getSubscriptionInfoForIccId]- iccId=" + str2 + ",subInfo=null");
                return null;
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setDefaultDataSubIdWithoutCapabilitySwitch(int i) {
        if (i == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultDataSubIdWithoutCapabilitySwitch called with DEFAULT_SUB_ID");
        }
        if (ENGDEBUG) {
            logd("[setDefaultDataSubIdWithoutCapabilitySwitch] subId=" + i + ", Binder.getCallingPid and Binder.getCallingUid are " + Binder.getCallingPid() + "," + Binder.getCallingUid());
        }
        updateAllDataConnectionTrackers();
        Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_data_call", i);
        broadcastDefaultDataSubIdChanged(i);
    }

    public void notifySubscriptionInfoChanged(Intent intent) {
        ITelephonyRegistry iTelephonyRegistryAsInterface = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
        try {
            setReadyState(true);
            iTelephonyRegistryAsInterface.notifySubscriptionInfoChanged();
        } catch (RemoteException e) {
        }
        broadcastSimInfoContentChanged(intent);
    }

    public void removeStickyIntent() {
        synchronized (MtkSubscriptionController.class) {
            if (sStickyIntent != null) {
                logd("removeStickyIntent");
                this.mContext.removeStickyBroadcast(sStickyIntent);
                sStickyIntent = null;
            }
        }
    }

    public boolean isReady() {
        if (ENGDEBUG) {
            logd("[isReady]- " + this.mIsReady);
        }
        return this.mIsReady;
    }

    public void setReadyState(boolean z) {
        if (ENGDEBUG) {
            logd("[setReadyState]- " + z);
        }
        this.mIsReady = z;
    }

    private void logv(String str) {
        Rlog.v(LOG_TAG, str);
    }

    private void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    private void logdl(String str) {
        logd(str);
        this.mLocalLog.log(str);
    }

    public int getDefaultFallbackSubId() {
        return mDefaultFallbackSubId;
    }

    public int getActiveSubCountForSml() {
        if (this.mCacheActiveSubInfoList != null) {
            return this.mCacheActiveSubInfoList.size();
        }
        return 0;
    }
}
