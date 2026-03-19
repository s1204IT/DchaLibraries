package com.android.internal.telephony;

import android.R;
import android.app.AppOpsManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.EventLog;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.ISub;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.dataconnection.KeepaliveStatus;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SubscriptionController extends ISub.Stub {
    protected static final boolean DBG = true;
    protected static final boolean DBG_CACHE = false;
    private static final int DEPRECATED_SETTING = -1;
    static final String LOG_TAG = "SubscriptionController";
    static final int MAX_LOCAL_LOG_LINES = 500;
    protected static final boolean VDBG = false;
    protected static Phone[] sPhones;
    private int[] colorArr;
    private AppOpsManager mAppOps;
    protected CallManager mCM;
    protected final List<SubscriptionInfo> mCacheActiveSubInfoList;
    protected Context mContext;
    private long mLastISubServiceRegTime;
    protected ScLocalLog mLocalLog;
    protected final Object mLock;
    protected TelephonyManager mTelephonyManager;
    private static final Comparator<SubscriptionInfo> SUBSCRIPTION_INFO_COMPARATOR = new Comparator() {
        @Override
        public final int compare(Object obj, Object obj2) {
            return SubscriptionController.lambda$static$0((SubscriptionInfo) obj, (SubscriptionInfo) obj2);
        }
    };
    private static SubscriptionController sInstance = null;
    protected static Map<Integer, Integer> sSlotIndexToSubId = new ConcurrentHashMap();
    protected static int mDefaultFallbackSubId = -1;
    protected static int mDefaultPhoneId = KeepaliveStatus.INVALID_HANDLE;

    protected static class ScLocalLog {
        private int mMaxLines;
        private LinkedList<String> mLog = new LinkedList<>();
        private Time mNow = new Time();

        public ScLocalLog(int i) {
            this.mMaxLines = i;
        }

        public synchronized void log(String str) {
            if (this.mMaxLines > 0) {
                int iMyPid = Process.myPid();
                int iMyTid = Process.myTid();
                this.mNow.setToNow();
                this.mLog.add(this.mNow.format("%m-%d %H:%M:%S") + " pid=" + iMyPid + " tid=" + iMyTid + " " + str);
                while (this.mLog.size() > this.mMaxLines) {
                    this.mLog.remove();
                }
            }
        }

        public synchronized void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            int i = 0;
            ListIterator<String> listIterator = this.mLog.listIterator(0);
            while (listIterator.hasNext()) {
                StringBuilder sb = new StringBuilder();
                int i2 = i + 1;
                sb.append(Integer.toString(i));
                sb.append(": ");
                sb.append(listIterator.next());
                printWriter.println(sb.toString());
                if (i2 % 10 == 0) {
                    printWriter.flush();
                }
                i = i2;
            }
        }
    }

    static int lambda$static$0(SubscriptionInfo subscriptionInfo, SubscriptionInfo subscriptionInfo2) {
        int simSlotIndex = subscriptionInfo.getSimSlotIndex() - subscriptionInfo2.getSimSlotIndex();
        if (simSlotIndex == 0) {
            return subscriptionInfo.getSubscriptionId() - subscriptionInfo2.getSubscriptionId();
        }
        return simSlotIndex;
    }

    public static SubscriptionController init(Phone phone) {
        SubscriptionController subscriptionController;
        synchronized (SubscriptionController.class) {
            if (sInstance == null) {
                sInstance = TelephonyComponentFactory.getInstance().makeSubscriptionController(phone);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            subscriptionController = sInstance;
        }
        return subscriptionController;
    }

    public static SubscriptionController init(Context context, CommandsInterface[] commandsInterfaceArr) {
        SubscriptionController subscriptionController;
        synchronized (SubscriptionController.class) {
            if (sInstance == null) {
                sInstance = TelephonyComponentFactory.getInstance().makeSubscriptionController(context, commandsInterfaceArr);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            subscriptionController = sInstance;
        }
        return subscriptionController;
    }

    public static SubscriptionController getInstance() {
        if (sInstance == null) {
            Log.wtf(LOG_TAG, "getInstance null");
        }
        return sInstance;
    }

    protected SubscriptionController(Context context) {
        this.mLocalLog = new ScLocalLog(MAX_LOCAL_LOG_LINES);
        this.mCacheActiveSubInfoList = new ArrayList();
        this.mLock = new Object();
        init(context);
        migrateImsSettings();
    }

    protected void init(Context context) {
        this.mContext = context;
        this.mCM = CallManager.getInstance();
        this.mTelephonyManager = TelephonyManager.from(this.mContext);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        if (ServiceManager.getService("isub") == null) {
            ServiceManager.addService("isub", this);
            this.mLastISubServiceRegTime = System.currentTimeMillis();
        }
        logdl("[SubscriptionController] init by Context");
    }

    private boolean isSubInfoReady() {
        return sSlotIndexToSubId.size() > 0;
    }

    protected SubscriptionController(Phone phone) {
        this.mLocalLog = new ScLocalLog(MAX_LOCAL_LOG_LINES);
        this.mCacheActiveSubInfoList = new ArrayList();
        this.mLock = new Object();
        this.mContext = phone.getContext();
        this.mCM = CallManager.getInstance();
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        if (ServiceManager.getService("isub") == null) {
            ServiceManager.addService("isub", this);
        }
        migrateImsSettings();
        logdl("[SubscriptionController] init by Phone");
    }

    protected void enforceModifyPhoneState(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", str);
    }

    private void enforceReadPrivilegedPhoneState(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", str);
    }

    private void broadcastSimInfoContentChanged() {
        this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE"));
        this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED"));
    }

    public void notifySubscriptionInfoChanged() {
        ITelephonyRegistry iTelephonyRegistryAsInterface = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
        try {
            logd("notifySubscriptionInfoChanged:");
            iTelephonyRegistryAsInterface.notifySubscriptionInfoChanged();
        } catch (RemoteException e) {
        }
        broadcastSimInfoContentChanged();
    }

    protected SubscriptionInfo getSubInfoRecord(Cursor cursor) {
        UiccAccessRule[] uiccAccessRuleArrDecodeRules;
        int i = cursor.getInt(cursor.getColumnIndexOrThrow(HbpcdLookup.ID));
        String string = cursor.getString(cursor.getColumnIndexOrThrow("icc_id"));
        int i2 = cursor.getInt(cursor.getColumnIndexOrThrow("sim_id"));
        String string2 = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
        String string3 = cursor.getString(cursor.getColumnIndexOrThrow("carrier_name"));
        int i3 = cursor.getInt(cursor.getColumnIndexOrThrow("name_source"));
        int i4 = cursor.getInt(cursor.getColumnIndexOrThrow("color"));
        String string4 = cursor.getString(cursor.getColumnIndexOrThrow("number"));
        int i5 = cursor.getInt(cursor.getColumnIndexOrThrow("data_roaming"));
        Bitmap bitmapDecodeResource = BitmapFactory.decodeResource(this.mContext.getResources(), R.drawable.ic_media_route_connected_light_29_mtrl);
        int i6 = cursor.getInt(cursor.getColumnIndexOrThrow("mcc"));
        int i7 = cursor.getInt(cursor.getColumnIndexOrThrow("mnc"));
        String string5 = cursor.getString(cursor.getColumnIndexOrThrow("card_id"));
        String subscriptionCountryIso = getSubscriptionCountryIso(i);
        boolean z = cursor.getInt(cursor.getColumnIndexOrThrow("is_embedded")) == 1;
        if (z) {
            uiccAccessRuleArrDecodeRules = UiccAccessRule.decodeRules(cursor.getBlob(cursor.getColumnIndexOrThrow("access_rules")));
        } else {
            uiccAccessRuleArrDecodeRules = null;
        }
        UiccAccessRule[] uiccAccessRuleArr = uiccAccessRuleArrDecodeRules;
        String line1Number = this.mTelephonyManager.getLine1Number(i);
        return new SubscriptionInfo(i, string, i2, string2, string3, i3, i4, (TextUtils.isEmpty(line1Number) || line1Number.equals(string4)) ? string4 : line1Number, i5, bitmapDecodeResource, i6, i7, subscriptionCountryIso, z, uiccAccessRuleArr, string5);
    }

    protected String getSubscriptionCountryIso(int i) {
        int phoneId = getPhoneId(i);
        if (phoneId < 0) {
            return "";
        }
        return this.mTelephonyManager.getSimCountryIsoForPhone(phoneId);
    }

    private List<SubscriptionInfo> getSubInfo(String str, Object obj) {
        String[] strArr;
        ArrayList arrayList = null;
        if (obj == null) {
            strArr = null;
        } else {
            strArr = new String[]{obj.toString()};
        }
        Cursor cursorQuery = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, str, strArr, null);
        try {
            if (cursorQuery != null) {
                while (cursorQuery.moveToNext()) {
                    SubscriptionInfo subInfoRecord = getSubInfoRecord(cursorQuery);
                    if (subInfoRecord != null) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        arrayList.add(subInfoRecord);
                    }
                }
            } else {
                logd("Query fail");
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return arrayList;
        } catch (Throwable th) {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            throw th;
        }
    }

    private int getUnusedColor(String str) {
        List<SubscriptionInfo> activeSubscriptionInfoList = getActiveSubscriptionInfoList(str);
        this.colorArr = this.mContext.getResources().getIntArray(R.array.config_dropboxLowPriorityTags);
        int size = 0;
        if (activeSubscriptionInfoList != null) {
            for (int i = 0; i < this.colorArr.length; i++) {
                int i2 = 0;
                while (i2 < activeSubscriptionInfoList.size() && this.colorArr[i] != activeSubscriptionInfoList.get(i2).getIconTint()) {
                    i2++;
                }
                if (i2 == activeSubscriptionInfoList.size()) {
                    return this.colorArr[i];
                }
            }
            size = activeSubscriptionInfoList.size() % this.colorArr.length;
        }
        return this.colorArr[size];
    }

    public SubscriptionInfo getActiveSubscriptionInfo(int i, String str) {
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getActiveSubscriptionInfo")) {
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> activeSubscriptionInfoList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            if (activeSubscriptionInfoList != null) {
                for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                    if (subscriptionInfo.getSubscriptionId() == i) {
                        logd("[getActiveSubscriptionInfo]+ subId=" + i + " subInfo=" + subscriptionInfo);
                        return subscriptionInfo;
                    }
                }
            }
            logd("[getActiveSubInfoForSubscriber]- subId=" + i + " subList=" + activeSubscriptionInfoList + " subInfo=null");
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public SubscriptionInfo getActiveSubscriptionInfoForIccId(String str, String str2) {
        SubscriptionInfo activeSubscriptionInfoForIccIdInternal = getActiveSubscriptionInfoForIccIdInternal(str);
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, activeSubscriptionInfoForIccIdInternal != null ? activeSubscriptionInfoForIccIdInternal.getSubscriptionId() : -1, str2, "getActiveSubscriptionInfoForIccId")) {
            return null;
        }
        return activeSubscriptionInfoForIccIdInternal;
    }

    private SubscriptionInfo getActiveSubscriptionInfoForIccIdInternal(String str) {
        if (str == null) {
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> activeSubscriptionInfoList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            if (activeSubscriptionInfoList != null) {
                for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                    if (str.equals(subscriptionInfo.getIccId())) {
                        logd("[getActiveSubInfoUsingIccId]+ iccId=" + str + " subInfo=" + subscriptionInfo);
                        return subscriptionInfo;
                    }
                }
            }
            logd("[getActiveSubInfoUsingIccId]+ iccId=" + str + " subList=" + activeSubscriptionInfoList + " subInfo=null");
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int i, String str) {
        Phone phone = PhoneFactory.getPhone(i);
        if (phone == null) {
            loge("[getActiveSubscriptionInfoForSimSlotIndex] no phone, slotIndex=" + i);
            return null;
        }
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, phone.getSubId(), str, "getActiveSubscriptionInfoForSimSlotIndex")) {
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> activeSubscriptionInfoList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            if (activeSubscriptionInfoList != null) {
                for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                    if (subscriptionInfo.getSimSlotIndex() == i) {
                        logd("[getActiveSubscriptionInfoForSimSlotIndex]+ slotIndex=" + i + " subId=" + subscriptionInfo);
                        return subscriptionInfo;
                    }
                }
                logd("[getActiveSubscriptionInfoForSimSlotIndex]+ slotIndex=" + i + " subId=null");
            } else {
                logd("[getActiveSubscriptionInfoForSimSlotIndex]+ subList=null");
            }
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public List<SubscriptionInfo> getAllSubInfoList(String str) {
        logd("[getAllSubInfoList]+");
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, -1, str, "getAllSubInfoList")) {
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> subInfo = getSubInfo(null, null);
            if (subInfo != null) {
                logd("[getAllSubInfoList]- " + subInfo.size() + " infos return");
            } else {
                logd("[getAllSubInfoList]- no info return");
            }
            return subInfo;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public List<SubscriptionInfo> getActiveSubscriptionInfoList(final String str) {
        boolean zCheckReadPhoneState;
        if (!isSubInfoReady()) {
            logdl("[getActiveSubInfoList] Sub Controller not ready");
            return null;
        }
        try {
            zCheckReadPhoneState = TelephonyPermissions.checkReadPhoneState(this.mContext, -1, Binder.getCallingPid(), Binder.getCallingUid(), str, "getActiveSubscriptionInfoList");
        } catch (SecurityException e) {
            zCheckReadPhoneState = false;
        }
        synchronized (this.mCacheActiveSubInfoList) {
            try {
                if (zCheckReadPhoneState) {
                    return new ArrayList(this.mCacheActiveSubInfoList);
                }
                return (List) this.mCacheActiveSubInfoList.stream().filter(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return SubscriptionController.lambda$getActiveSubscriptionInfoList$1(this.f$0, str, (SubscriptionInfo) obj);
                    }
                }).collect(Collectors.toList());
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public static boolean lambda$getActiveSubscriptionInfoList$1(SubscriptionController subscriptionController, String str, SubscriptionInfo subscriptionInfo) {
        try {
            return TelephonyPermissions.checkCallingOrSelfReadPhoneState(subscriptionController.mContext, subscriptionInfo.getSubscriptionId(), str, "getActiveSubscriptionInfoList");
        } catch (SecurityException e) {
            return false;
        }
    }

    @VisibleForTesting
    public void refreshCachedActiveSubscriptionInfoList() {
        if (!isSubInfoReady()) {
            return;
        }
        synchronized (this.mCacheActiveSubInfoList) {
            this.mCacheActiveSubInfoList.clear();
            List<SubscriptionInfo> subInfo = getSubInfo("sim_id>=0", null);
            if (subInfo != null) {
                subInfo.sort(SUBSCRIPTION_INFO_COMPARATOR);
            }
            if (subInfo != null) {
                this.mCacheActiveSubInfoList.addAll(subInfo);
            }
        }
    }

    public int getActiveSubInfoCount(String str) {
        List<SubscriptionInfo> activeSubscriptionInfoList = getActiveSubscriptionInfoList(str);
        if (activeSubscriptionInfoList == null) {
            return 0;
        }
        return activeSubscriptionInfoList.size();
    }

    public int getAllSubInfoCount(String str) {
        logd("[getAllSubInfoCount]+");
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, -1, str, "getAllSubInfoCount")) {
            return 0;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Cursor cursorQuery = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, null, null, null);
            if (cursorQuery != null) {
                try {
                    int count = cursorQuery.getCount();
                    logd("[getAllSubInfoCount]- " + count + " SUB(s) in DB");
                    return count;
                } finally {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            logd("[getAllSubInfoCount]- no SUB in DB");
            return 0;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int getActiveSubInfoCountMax() {
        return this.mTelephonyManager.getSimCount();
    }

    public List<SubscriptionInfo> getAvailableSubscriptionInfoList(String str) {
        try {
            enforceReadPrivilegedPhoneState("getAvailableSubscriptionInfoList");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (((EuiccManager) this.mContext.getSystemService("euicc")).isEnabled()) {
                    List<SubscriptionInfo> subInfo = getSubInfo("sim_id>=0 OR is_embedded=1", null);
                    if (subInfo != null) {
                        subInfo.sort(SUBSCRIPTION_INFO_COMPARATOR);
                    } else {
                        logdl("[getAvailableSubInfoList]- no info return");
                    }
                    return subInfo;
                }
                logdl("[getAvailableSubInfoList] Embedded subscriptions are disabled");
                return null;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (SecurityException e) {
            try {
                this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", null);
                EventLog.writeEvent(1397638484, "185235454", Integer.valueOf(Binder.getCallingUid()));
            } catch (SecurityException e2) {
            }
            throw new SecurityException("Need READ_PRIVILEGED_PHONE_STATE to call  getAvailableSubscriptionInfoList");
        }
    }

    public List<SubscriptionInfo> getAccessibleSubscriptionInfoList(final String str) {
        if (!((EuiccManager) this.mContext.getSystemService("euicc")).isEnabled()) {
            logdl("[getAccessibleSubInfoList] Embedded subscriptions are disabled");
            return null;
        }
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> subInfo = getSubInfo("is_embedded=1", null);
            if (subInfo == null) {
                logdl("[getAccessibleSubInfoList] No info returned");
                return null;
            }
            return (List) subInfo.stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ((SubscriptionInfo) obj).canManageSubscription(this.f$0.mContext, str);
                }
            }).sorted(SUBSCRIPTION_INFO_COMPARATOR).collect(Collectors.toList());
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public List<SubscriptionInfo> getSubscriptionInfoListForEmbeddedSubscriptionUpdate(String[] strArr, boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append("is_embedded");
        sb.append("=1");
        if (z) {
            sb.append(" AND ");
            sb.append("is_removable");
            sb.append("=1");
        }
        sb.append(") OR ");
        sb.append("icc_id");
        sb.append(" IN (");
        for (int i = 0; i < strArr.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"");
            sb.append(strArr[i]);
            sb.append("\"");
        }
        sb.append(")");
        List<SubscriptionInfo> subInfo = getSubInfo(sb.toString(), null);
        if (subInfo == null) {
            return Collections.emptyList();
        }
        return subInfo;
    }

    public void requestEmbeddedSubscriptionInfoListRefresh() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS", "requestEmbeddedSubscriptionInfoListRefresh");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            PhoneFactory.requestEmbeddedSubscriptionInfoListRefresh(null);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void requestEmbeddedSubscriptionInfoListRefresh(Runnable runnable) {
        PhoneFactory.requestEmbeddedSubscriptionInfoListRefresh(runnable);
    }

    public int addSubInfoRecord(String str, int i) {
        boolean z;
        String cardId;
        int i2;
        logdl("[addSubInfoRecord]+ iccId:" + SubscriptionInfo.givePrintableIccid(str) + " slotIndex:" + i);
        enforceModifyPhoneState("addSubInfoRecord");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (str == null) {
                logdl("[addSubInfoRecord]- null iccId");
                return -1;
            }
            ContentResolver contentResolver = this.mContext.getContentResolver();
            Cursor cursorQuery = contentResolver.query(SubscriptionManager.CONTENT_URI, new String[]{HbpcdLookup.ID, "sim_id", "name_source", "icc_id", "card_id"}, "icc_id=? OR icc_id=?", new String[]{str, IccUtils.getDecimalSubstring(str)}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        int i3 = cursorQuery.getInt(0);
                        int i4 = cursorQuery.getInt(1);
                        int i5 = cursorQuery.getInt(2);
                        String string = cursorQuery.getString(3);
                        String string2 = cursorQuery.getString(4);
                        ContentValues contentValues = new ContentValues();
                        if (i != i4) {
                            contentValues.put("sim_id", Integer.valueOf(i));
                        }
                        z = i5 != 2;
                        if (string != null && string.length() < str.length() && string.equals(IccUtils.getDecimalSubstring(str))) {
                            contentValues.put("icc_id", str);
                        }
                        UiccCard uiccCardForPhone = UiccController.getInstance().getUiccCardForPhone(i);
                        if (uiccCardForPhone != null && (cardId = uiccCardForPhone.getCardId()) != null && cardId != string2) {
                            contentValues.put("card_id", cardId);
                        }
                        if (contentValues.size() > 0) {
                            contentResolver.update(SubscriptionManager.CONTENT_URI, contentValues, "_id=" + Long.toString(i3), null);
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
                            int i6 = cursorQuery2.getInt(cursorQuery2.getColumnIndexOrThrow(HbpcdLookup.ID));
                            Integer num = sSlotIndexToSubId.get(Integer.valueOf(i));
                            if (num != null && num.intValue() == i6 && SubscriptionManager.isValidSubscriptionId(num.intValue())) {
                                logdl("[addSubInfoRecord] currentSubId != null && currentSubId is valid, IGNORE");
                            } else {
                                sSlotIndexToSubId.put(Integer.valueOf(i), Integer.valueOf(i6));
                                int activeSubInfoCountMax = getActiveSubInfoCountMax();
                                int defaultSubId = getDefaultSubId();
                                logdl("[addSubInfoRecord] sSlotIndexToSubId.size=" + sSlotIndexToSubId.size() + " slotIndex=" + i + " subId=" + i6 + " defaultSubId=" + defaultSubId + " simCount=" + activeSubInfoCountMax);
                                if (SubscriptionManager.isValidSubscriptionId(defaultSubId)) {
                                    i2 = 1;
                                    if (activeSubInfoCountMax == 1) {
                                    }
                                    if (activeSubInfoCountMax == i2) {
                                        logdl("[addSubInfoRecord] one sim set defaults to subId=" + i6);
                                        setDefaultDataSubId(i6);
                                        setDefaultSmsSubId(i6);
                                        setDefaultVoiceSubId(i6);
                                    }
                                } else {
                                    i2 = 1;
                                }
                                setDefaultFallbackSubId(i6);
                                if (activeSubInfoCountMax == i2) {
                                }
                            }
                            logdl("[addSubInfoRecord] hashmap(" + i + "," + i6 + ")");
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
                if (TextUtils.isEmpty(simOperatorName)) {
                    simOperatorName = "CARD " + Integer.toString(i + 1);
                }
                ContentValues contentValues2 = new ContentValues();
                contentValues2.put("display_name", simOperatorName);
                contentResolver.update(SubscriptionManager.CONTENT_URI, contentValues2, "_id=" + Long.toString(subIdUsingPhoneId), null);
                refreshCachedActiveSubscriptionInfoList();
                logdl("[addSubInfoRecord] sim name = " + simOperatorName);
            }
            sPhones[i].updateDataConnectionTracker();
            logdl("[addSubInfoRecord]- info size=" + sSlotIndexToSubId.size());
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return 0;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public Uri insertEmptySubInfoRecord(String str, int i) {
        String cardId;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put("icc_id", str);
        contentValues.put("color", Integer.valueOf(getUnusedColor(this.mContext.getOpPackageName())));
        contentValues.put("sim_id", Integer.valueOf(i));
        contentValues.put("carrier_name", "");
        UiccCard uiccCardForPhone = UiccController.getInstance().getUiccCardForPhone(i);
        if (uiccCardForPhone != null && (cardId = uiccCardForPhone.getCardId()) != null) {
            contentValues.put("card_id", cardId);
        } else {
            contentValues.put("card_id", str);
        }
        Uri uriInsert = contentResolver.insert(SubscriptionManager.CONTENT_URI, contentValues);
        refreshCachedActiveSubscriptionInfoList();
        return uriInsert;
    }

    public boolean setPlmnSpn(int i, boolean z, String str, boolean z2, String str2) {
        synchronized (this.mLock) {
            int subIdUsingPhoneId = getSubIdUsingPhoneId(i);
            if (this.mContext.getPackageManager().resolveContentProvider(SubscriptionManager.CONTENT_URI.getAuthority(), 0) != null && SubscriptionManager.isValidSubscriptionId(subIdUsingPhoneId)) {
                if (z) {
                    if (z2 && !Objects.equals(str2, str)) {
                        str = str + this.mContext.getString(R.string.config_wearMediaSessionsPackage).toString() + str2;
                    }
                } else if (!z2) {
                    str = "";
                } else {
                    str = str2;
                }
                setCarrierText(str, subIdUsingPhoneId);
                return true;
            }
            logd("[setPlmnSpn] No valid subscription to store info");
            notifySubscriptionInfoChanged();
            return false;
        }
    }

    private int setCarrierText(String str, int i) {
        logd("[setCarrierText]+ text:" + str + " subId:" + i);
        enforceModifyPhoneState("setCarrierText");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            ContentValues contentValues = new ContentValues(1);
            contentValues.put("carrier_name", str);
            int iUpdate = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, contentValues, "_id=" + Long.toString(i), null);
            refreshCachedActiveSubscriptionInfoList();
            notifySubscriptionInfoChanged();
            return iUpdate;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int setIconTint(int i, int i2) {
        logd("[setIconTint]+ tint:" + i + " subId:" + i2);
        enforceModifyPhoneState("setIconTint");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            validateSubId(i2);
            ContentValues contentValues = new ContentValues(1);
            contentValues.put("color", Integer.valueOf(i));
            logd("[setIconTint]- tint:" + i + " set");
            int iUpdate = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, contentValues, "_id=" + Long.toString(i2), null);
            refreshCachedActiveSubscriptionInfoList();
            notifySubscriptionInfoChanged();
            return iUpdate;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int setDisplayName(String str, int i) {
        return setDisplayNameUsingSrc(str, i, -1L);
    }

    public int setDisplayNameUsingSrc(String str, int i, long j) {
        logd("[setDisplayName]+  displayName:" + str + " subId:" + i + " nameSource:" + j);
        enforceModifyPhoneState("setDisplayNameUsingSrc");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            validateSubId(i);
            if (str == null) {
                str = this.mContext.getString(R.string.unknownName);
            }
            ContentValues contentValues = new ContentValues(1);
            contentValues.put("display_name", str);
            if (j >= 0) {
                logd("Set nameSource=" + j);
                contentValues.put("name_source", Long.valueOf(j));
            }
            logd("[setDisplayName]- mDisplayName:" + str + " set");
            int iUpdate = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, contentValues, "_id=" + Long.toString(i), null);
            refreshCachedActiveSubscriptionInfoList();
            notifySubscriptionInfoChanged();
            return iUpdate;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int setDisplayNumber(String str, int i) {
        logd("[setDisplayNumber]+ subId:" + i);
        enforceModifyPhoneState("setDisplayNumber");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            validateSubId(i);
            int phoneId = getPhoneId(i);
            if (str != null && phoneId >= 0 && phoneId < this.mTelephonyManager.getPhoneCount()) {
                ContentValues contentValues = new ContentValues(1);
                contentValues.put("number", str);
                int iUpdate = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, contentValues, "_id=" + Long.toString(i), null);
                refreshCachedActiveSubscriptionInfoList();
                logd("[setDisplayNumber]- update result :" + iUpdate);
                notifySubscriptionInfoChanged();
                return iUpdate;
            }
            logd("[setDispalyNumber]- fail");
            return -1;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int setDataRoaming(int i, int i2) {
        logd("[setDataRoaming]+ roaming:" + i + " subId:" + i2);
        enforceModifyPhoneState("setDataRoaming");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            validateSubId(i2);
            if (i < 0) {
                logd("[setDataRoaming]- fail");
                return -1;
            }
            ContentValues contentValues = new ContentValues(1);
            contentValues.put("data_roaming", Integer.valueOf(i));
            logd("[setDataRoaming]- roaming:" + i + " set");
            int iUpdate = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, contentValues, "_id=" + Long.toString(i2), null);
            refreshCachedActiveSubscriptionInfoList();
            notifySubscriptionInfoChanged();
            return iUpdate;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int setMccMnc(String str, int i) {
        int i2;
        int i3;
        try {
            i2 = Integer.parseInt(str.substring(0, 3));
            try {
                i3 = Integer.parseInt(str.substring(3));
            } catch (NumberFormatException e) {
                loge("[setMccMnc] - couldn't parse mcc/mnc: " + str);
                i3 = 0;
            }
        } catch (NumberFormatException e2) {
            i2 = 0;
        }
        logd("[setMccMnc]+ mcc/mnc:" + i2 + "/" + i3 + " subId:" + i);
        ContentValues contentValues = new ContentValues(2);
        contentValues.put("mcc", Integer.valueOf(i2));
        contentValues.put("mnc", Integer.valueOf(i3));
        int iUpdate = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, contentValues, "_id=" + Long.toString(i), null);
        refreshCachedActiveSubscriptionInfoList();
        notifySubscriptionInfoChanged();
        return iUpdate;
    }

    public int getSlotIndex(int i) {
        if (i == Integer.MAX_VALUE) {
            i = getDefaultSubId();
        }
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            logd("[getSlotIndex]- subId invalid");
            return -1;
        }
        if (sSlotIndexToSubId.size() == 0) {
            logd("[getSlotIndex]- size == 0, return SIM_NOT_INSERTED instead");
            return -1;
        }
        for (Map.Entry<Integer, Integer> entry : sSlotIndexToSubId.entrySet()) {
            int iIntValue = entry.getKey().intValue();
            if (i == entry.getValue().intValue()) {
                return iIntValue;
            }
        }
        logd("[getSlotIndex]- return fail");
        return -1;
    }

    @Deprecated
    public int[] getSubId(int i) {
        if (i == Integer.MAX_VALUE) {
            i = getSlotIndex(getDefaultSubId());
        }
        if (!SubscriptionManager.isValidSlotIndex(i)) {
            logd("[getSubId]- invalid slotIndex=" + i);
            return null;
        }
        if (sSlotIndexToSubId.size() == 0) {
            return getDummySubIds(i);
        }
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<Integer, Integer> entry : sSlotIndexToSubId.entrySet()) {
            int iIntValue = entry.getKey().intValue();
            int iIntValue2 = entry.getValue().intValue();
            if (i == iIntValue) {
                arrayList.add(Integer.valueOf(iIntValue2));
            }
        }
        int size = arrayList.size();
        if (size > 0) {
            int[] iArr = new int[size];
            for (int i2 = 0; i2 < size; i2++) {
                iArr[i2] = ((Integer) arrayList.get(i2)).intValue();
            }
            return iArr;
        }
        logd("[getSubId]- numSubIds == 0, return DummySubIds slotIndex=" + i);
        return getDummySubIds(i);
    }

    public int getPhoneId(int i) {
        if (i == Integer.MAX_VALUE) {
            i = getDefaultSubId();
            logdl("[getPhoneId] asked for default subId=" + i);
        }
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            return -1;
        }
        if (sSlotIndexToSubId.size() == 0) {
            int i2 = mDefaultPhoneId;
            logdl("[getPhoneId]- no sims, returning default phoneId=" + i2);
            return i2;
        }
        for (Map.Entry<Integer, Integer> entry : sSlotIndexToSubId.entrySet()) {
            int iIntValue = entry.getKey().intValue();
            if (i == entry.getValue().intValue()) {
                return iIntValue;
            }
        }
        int i3 = mDefaultPhoneId;
        logdl("[getPhoneId]- subId=" + i + " not found return default phoneId=" + i3);
        return i3;
    }

    private int[] getDummySubIds(int i) {
        int activeSubInfoCountMax = getActiveSubInfoCountMax();
        if (activeSubInfoCountMax > 0) {
            int[] iArr = new int[activeSubInfoCountMax];
            for (int i2 = 0; i2 < activeSubInfoCountMax; i2++) {
                iArr[i2] = (-2) - i;
            }
            return iArr;
        }
        return null;
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
            sSlotIndexToSubId.clear();
            logdl("[clearSubInfo]- clear size=" + size);
            return size;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void logvl(String str) {
        logv(str);
        this.mLocalLog.log(str);
    }

    private void logv(String str) {
        Rlog.v(LOG_TAG, str);
    }

    private void logdl(String str) {
        logd(str);
        this.mLocalLog.log(str);
    }

    private static void slogd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private void logel(String str) {
        loge(str);
        this.mLocalLog.log(str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    public int getDefaultSubId() {
        int defaultDataSubId;
        if (this.mContext.getResources().getBoolean(R.^attr-private.popupPromptView)) {
            defaultDataSubId = getDefaultVoiceSubId();
        } else {
            defaultDataSubId = getDefaultDataSubId();
        }
        if (!isActiveSubId(defaultDataSubId)) {
            return mDefaultFallbackSubId;
        }
        return defaultDataSubId;
    }

    public void setDefaultSmsSubId(int i) {
        enforceModifyPhoneState("setDefaultSmsSubId");
        if (i == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultSmsSubId called with DEFAULT_SUB_ID");
        }
        logdl("[setDefaultSmsSubId] subId=" + i);
        Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_sms", i);
        broadcastDefaultSmsSubIdChanged(i);
    }

    private void broadcastDefaultSmsSubIdChanged(int i) {
        logdl("[broadcastDefaultSmsSubIdChanged] subId=" + i);
        Intent intent = new Intent("android.telephony.action.DEFAULT_SMS_SUBSCRIPTION_CHANGED");
        intent.addFlags(553648128);
        intent.putExtra("subscription", i);
        intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", i);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public int getDefaultSmsSubId() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "multi_sim_sms", -1);
    }

    public void setDefaultVoiceSubId(int i) {
        enforceModifyPhoneState("setDefaultVoiceSubId");
        if (i == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultVoiceSubId called with DEFAULT_SUB_ID");
        }
        logdl("[setDefaultVoiceSubId] subId=" + i);
        Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_voice_call", i);
        broadcastDefaultVoiceSubIdChanged(i);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void broadcastDefaultVoiceSubIdChanged(int i) {
        logdl("[broadcastDefaultVoiceSubIdChanged] subId=" + i);
        Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED");
        intent.addFlags(553648128);
        intent.putExtra("subscription", i);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public int getDefaultVoiceSubId() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "multi_sim_voice_call", -1);
    }

    public int getDefaultDataSubId() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "multi_sim_data_call", -1);
    }

    public void setDefaultDataSubId(int i) {
        boolean z;
        int minRafSupported;
        enforceModifyPhoneState("setDefaultDataSubId");
        if (i == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultDataSubId called with DEFAULT_SUB_ID");
        }
        ProxyController proxyController = ProxyController.getInstance();
        int length = sPhones.length;
        logdl("[setDefaultDataSubId] num phones=" + length + ", subId=" + i);
        if (SubscriptionManager.isValidSubscriptionId(i)) {
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
                logdl("[setDefaultDataSubId] phoneId=" + i2 + " subId=" + subId + " RAF=" + minRafSupported);
                radioAccessFamilyArr[i2] = new RadioAccessFamily(i2, minRafSupported);
                i2++;
                z2 = z;
            }
            if (z2) {
                proxyController.setRadioCapability(radioAccessFamilyArr);
            } else {
                logdl("[setDefaultDataSubId] no valid subId's found - not updating.");
            }
        }
        updateAllDataConnectionTrackers();
        Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_data_call", i);
        broadcastDefaultDataSubIdChanged(i);
    }

    protected void updateAllDataConnectionTrackers() {
        int length = sPhones.length;
        logdl("[updateAllDataConnectionTrackers] sPhones.length=" + length);
        for (int i = 0; i < length; i++) {
            logdl("[updateAllDataConnectionTrackers] phoneId=" + i);
            sPhones[i].updateDataConnectionTracker();
        }
    }

    public void broadcastDefaultDataSubIdChanged(int i) {
        logdl("[broadcastDefaultDataSubIdChanged] subId=" + i);
        Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intent.addFlags(553648128);
        intent.putExtra("subscription", i);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void setDefaultFallbackSubId(int i) {
        if (i == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultSubId called with DEFAULT_SUB_ID");
        }
        logdl("[setDefaultFallbackSubId] subId=" + i);
        if (SubscriptionManager.isValidSubscriptionId(i)) {
            int phoneId = getPhoneId(i);
            if (phoneId >= 0 && (phoneId < this.mTelephonyManager.getPhoneCount() || this.mTelephonyManager.getSimCount() == 1)) {
                logdl("[setDefaultFallbackSubId] set mDefaultFallbackSubId=" + i);
                mDefaultFallbackSubId = i;
                MccTable.updateMccMncConfiguration(this.mContext, this.mTelephonyManager.getSimOperatorNumericForPhone(phoneId), false);
                Intent intent = new Intent("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED");
                intent.addFlags(553648128);
                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, phoneId, i);
                logdl("[setDefaultFallbackSubId] broadcast default subId changed phoneId=" + phoneId + " subId=" + i);
                this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                return;
            }
            logdl("[setDefaultFallbackSubId] not set invalid phoneId=" + phoneId + " subId=" + i);
        }
    }

    public void clearDefaultsForInactiveSubIds() {
        enforceModifyPhoneState("clearDefaultsForInactiveSubIds");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> activeSubscriptionInfoList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            logdl("[clearDefaultsForInactiveSubIds] records: " + activeSubscriptionInfoList);
            if (shouldDefaultBeCleared(activeSubscriptionInfoList, getDefaultDataSubId())) {
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

    protected boolean shouldDefaultBeCleared(List<SubscriptionInfo> list, int i) {
        logdl("[shouldDefaultBeCleared: subId] " + i);
        if (list == null) {
            logdl("[shouldDefaultBeCleared] return true no records subId=" + i);
            return true;
        }
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            logdl("[shouldDefaultBeCleared] return false only one subId, subId=" + i);
            return false;
        }
        Iterator<SubscriptionInfo> it = list.iterator();
        while (it.hasNext()) {
            int subscriptionId = it.next().getSubscriptionId();
            logdl("[shouldDefaultBeCleared] Record.id: " + subscriptionId);
            if (subscriptionId == i) {
                logdl("[shouldDefaultBeCleared] return false subId is active, subId=" + i);
                return false;
            }
        }
        logdl("[shouldDefaultBeCleared] return true not active subId=" + i);
        return true;
    }

    public int getSubIdUsingPhoneId(int i) {
        int[] subId = getSubId(i);
        if (subId == null || subId.length == 0) {
            return -1;
        }
        return subId[0];
    }

    @VisibleForTesting
    public List<SubscriptionInfo> getSubInfoUsingSlotIndexPrivileged(int i, boolean z) {
        logd("[getSubInfoUsingSlotIndexPrivileged]+ slotIndex:" + i);
        if (i == Integer.MAX_VALUE) {
            i = getSlotIndex(getDefaultSubId());
        }
        ArrayList arrayList = null;
        if (!SubscriptionManager.isValidSlotIndex(i)) {
            logd("[getSubInfoUsingSlotIndexPrivileged]- invalid slotIndex");
            return null;
        }
        if (z && !isSubInfoReady()) {
            logd("[getSubInfoUsingSlotIndexPrivileged]- not ready");
            return null;
        }
        Cursor cursorQuery = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, "sim_id=?", new String[]{String.valueOf(i)}, null);
        if (cursorQuery != null) {
            while (cursorQuery.moveToNext()) {
                try {
                    SubscriptionInfo subInfoRecord = getSubInfoRecord(cursorQuery);
                    if (subInfoRecord != null) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        arrayList.add(subInfoRecord);
                    }
                } finally {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
        }
        logd("[getSubInfoUsingSlotIndex]- null info return");
        return arrayList;
    }

    private void validateSubId(int i) {
        logd("validateSubId subId: " + i);
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            throw new RuntimeException("Invalid sub id passed as parameter");
        }
        if (i == Integer.MAX_VALUE) {
            throw new RuntimeException("Default sub id passed as parameter");
        }
    }

    public void updatePhonesAvailability(Phone[] phoneArr) {
        sPhones = phoneArr;
    }

    public int[] getActiveSubIdList() {
        HashSet hashSet = new HashSet(sSlotIndexToSubId.entrySet());
        int[] iArr = new int[hashSet.size()];
        Iterator it = hashSet.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = ((Integer) ((Map.Entry) it.next()).getValue()).intValue();
            i++;
        }
        return iArr;
    }

    public boolean isActiveSubId(int i) {
        return SubscriptionManager.isValidSubscriptionId(i) && sSlotIndexToSubId.containsValue(Integer.valueOf(i));
    }

    public int getSimStateForSlotIndex(int i) {
        Phone phone;
        IccCard iccCard;
        IccCardConstants.State state;
        if (i < 0 || (phone = PhoneFactory.getPhone(i)) == null || (iccCard = phone.getIccCard()) == null) {
            state = IccCardConstants.State.UNKNOWN;
        } else {
            state = iccCard.getState();
        }
        return state.ordinal();
    }

    public void setSubscriptionProperty(int i, String str, String str2) {
        enforceModifyPhoneState("setSubscriptionProperty");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        setSubscriptionPropertyIntoContentResolver(i, str, str2, this.mContext.getContentResolver());
        refreshCachedActiveSubscriptionInfoList();
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private static void setSubscriptionPropertyIntoContentResolver(int i, String str, String str2, ContentResolver contentResolver) {
        ContentValues contentValues;
        contentValues = new ContentValues();
        switch (str) {
            case "enable_cmas_extreme_threat_alerts":
            case "enable_cmas_severe_threat_alerts":
            case "enable_cmas_amber_alerts":
            case "enable_emergency_alerts":
            case "alert_sound_duration":
            case "alert_reminder_interval":
            case "enable_alert_vibrate":
            case "enable_alert_speech":
            case "enable_etws_test_alerts":
            case "enable_channel_50_alerts":
            case "enable_cmas_test_alerts":
            case "show_cmas_opt_out_dialog":
            case "volte_vt_enabled":
            case "vt_ims_enabled":
            case "wfc_ims_enabled":
            case "wfc_ims_mode":
            case "wfc_ims_roaming_mode":
            case "wfc_ims_roaming_enabled":
                contentValues.put(str, Integer.valueOf(Integer.parseInt(str2)));
                break;
            default:
                slogd("Invalid column name");
                break;
        }
        contentResolver.update(SubscriptionManager.CONTENT_URI, contentValues, "_id=" + Integer.toString(i), null);
    }

    public String getSubscriptionProperty(int i, String str, String str2) {
        String str3 = null;
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str2, "getSubscriptionProperty")) {
            return null;
        }
        byte b = 1;
        Cursor cursorQuery = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, new String[]{str}, InboundSmsHandler.SELECT_BY_ID, new String[]{i + ""}, null);
        try {
            if (cursorQuery != null) {
                if (cursorQuery.moveToFirst()) {
                    switch (str.hashCode()) {
                        case -2000412720:
                            b = !str.equals("enable_alert_vibrate") ? (byte) -1 : (byte) 6;
                            break;
                        case -1950380197:
                            if (str.equals("volte_vt_enabled")) {
                                b = 12;
                                break;
                            }
                            break;
                        case -1555340190:
                            if (str.equals("enable_cmas_extreme_threat_alerts")) {
                                b = 0;
                                break;
                            }
                            break;
                        case -1433878403:
                            if (str.equals("enable_cmas_test_alerts")) {
                                b = 10;
                                break;
                            }
                            break;
                        case -1390801311:
                            if (str.equals("enable_alert_speech")) {
                                b = 7;
                                break;
                            }
                            break;
                        case -1218173306:
                            if (str.equals("wfc_ims_enabled")) {
                                b = 14;
                                break;
                            }
                            break;
                        case -461686719:
                            if (str.equals("enable_emergency_alerts")) {
                                b = 3;
                                break;
                            }
                            break;
                        case -420099376:
                            if (str.equals("vt_ims_enabled")) {
                                b = 13;
                                break;
                            }
                            break;
                        case -349439993:
                            if (str.equals("alert_sound_duration")) {
                                b = 4;
                                break;
                            }
                            break;
                        case 180938212:
                            if (str.equals("wfc_ims_roaming_mode")) {
                                b = 16;
                                break;
                            }
                            break;
                        case 203677434:
                            if (str.equals("enable_cmas_amber_alerts")) {
                                b = 2;
                                break;
                            }
                            break;
                        case 240841894:
                            if (str.equals("show_cmas_opt_out_dialog")) {
                                b = 11;
                                break;
                            }
                            break;
                        case 407275608:
                            if (str.equals("enable_cmas_severe_threat_alerts")) {
                                break;
                            }
                            break;
                        case 462555599:
                            if (str.equals("alert_reminder_interval")) {
                                b = 5;
                                break;
                            }
                            break;
                        case 1270593452:
                            if (str.equals("enable_etws_test_alerts")) {
                                b = 8;
                                break;
                            }
                            break;
                        case 1288054979:
                            if (str.equals("enable_channel_50_alerts")) {
                                b = 9;
                                break;
                            }
                            break;
                        case 1334635646:
                            if (str.equals("wfc_ims_mode")) {
                                b = 15;
                                break;
                            }
                            break;
                        case 1604840288:
                            if (str.equals("wfc_ims_roaming_enabled")) {
                                b = 17;
                                break;
                            }
                            break;
                        default:
                            break;
                    }
                    switch (b) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                        case 16:
                        case 17:
                            str3 = cursorQuery.getInt(0) + "";
                            break;
                        default:
                            logd("Invalid column name");
                            break;
                    }
                } else {
                    logd("Valid row not present in db");
                }
            } else {
                logd("Query failed");
            }
            logd("getSubscriptionProperty Query value = " + str3);
            return str3;
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    protected static void printStackTrace(String str) {
        RuntimeException runtimeException = new RuntimeException();
        slogd("StackTrace - " + str);
        boolean z = true;
        for (StackTraceElement stackTraceElement : runtimeException.getStackTrace()) {
            if (z) {
                z = false;
            } else {
                slogd(stackTraceElement.toString());
            }
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", "Requires DUMP");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            printWriter.println("SubscriptionController:");
            printWriter.println(" mLastISubServiceRegTime=" + this.mLastISubServiceRegTime);
            printWriter.println(" defaultSubId=" + getDefaultSubId());
            printWriter.println(" defaultDataSubId=" + getDefaultDataSubId());
            printWriter.println(" defaultVoiceSubId=" + getDefaultVoiceSubId());
            printWriter.println(" defaultSmsSubId=" + getDefaultSmsSubId());
            printWriter.println(" defaultDataPhoneId=" + SubscriptionManager.from(this.mContext).getDefaultDataPhoneId());
            printWriter.println(" defaultVoicePhoneId=" + SubscriptionManager.getDefaultVoicePhoneId());
            printWriter.println(" defaultSmsPhoneId=" + SubscriptionManager.from(this.mContext).getDefaultSmsPhoneId());
            printWriter.flush();
            for (Map.Entry<Integer, Integer> entry : sSlotIndexToSubId.entrySet()) {
                printWriter.println(" sSlotIndexToSubId[" + entry.getKey() + "]: subId=" + entry.getValue());
            }
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
            List<SubscriptionInfo> activeSubscriptionInfoList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            if (activeSubscriptionInfoList != null) {
                printWriter.println(" ActiveSubInfoList:");
                Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
                while (it.hasNext()) {
                    printWriter.println("  " + it.next().toString());
                }
            } else {
                printWriter.println(" ActiveSubInfoList: is null");
            }
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
            List<SubscriptionInfo> allSubInfoList = getAllSubInfoList(this.mContext.getOpPackageName());
            if (allSubInfoList != null) {
                printWriter.println(" AllSubInfoList:");
                Iterator<SubscriptionInfo> it2 = allSubInfoList.iterator();
                while (it2.hasNext()) {
                    printWriter.println("  " + it2.next().toString());
                }
            } else {
                printWriter.println(" AllSubInfoList: is null");
            }
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
            this.mLocalLog.dump(fileDescriptor, printWriter, strArr);
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
            printWriter.flush();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void migrateImsSettings() {
        migrateImsSettingHelper("volte_vt_enabled", "volte_vt_enabled");
        migrateImsSettingHelper("vt_ims_enabled", "vt_ims_enabled");
        migrateImsSettingHelper("wfc_ims_enabled", "wfc_ims_enabled");
        migrateImsSettingHelper("wfc_ims_mode", "wfc_ims_mode");
        migrateImsSettingHelper("wfc_ims_roaming_mode", "wfc_ims_roaming_mode");
        migrateImsSettingHelper("wfc_ims_roaming_enabled", "wfc_ims_roaming_enabled");
    }

    private void migrateImsSettingHelper(String str, String str2) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        int defaultVoiceSubId = getDefaultVoiceSubId();
        try {
            int i = Settings.Global.getInt(contentResolver, str);
            if (i != -1) {
                setSubscriptionPropertyIntoContentResolver(defaultVoiceSubId, str2, Integer.toString(i), contentResolver);
                Settings.Global.putInt(contentResolver, str, -1);
            }
        } catch (Settings.SettingNotFoundException e) {
        }
    }
}
