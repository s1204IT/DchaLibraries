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
import android.os.Build;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import com.android.internal.telephony.ISub;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.uicc.SpnOverride;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SubscriptionController extends ISub.Stub {
    static final boolean DBG = true;
    static final String LOG_TAG = "SubscriptionController";
    static final int MAX_LOCAL_LOG_LINES = 500;
    static final boolean VDBG = false;
    protected static Phone[] sPhones;
    private String[] PROPERTY_ICCID;
    private int[] colorArr;
    private List<SubscriptionInfo> mActiveList;
    private AppOpsManager mAppOps;
    protected CallManager mCM;
    protected Context mContext;
    private boolean mIsOP01;
    private boolean mIsOP09A;
    private boolean mIsReady;
    private ScLocalLog mLocalLog;
    protected final Object mLock;
    protected boolean mSuccess;
    protected TelephonyManager mTelephonyManager;
    static final boolean ENGDEBUG = TextUtils.equals(Build.TYPE, "eng");
    private static Intent sStickyIntent = null;
    private static SubscriptionController sInstance = null;
    private static Map<Integer, Integer> sSlotIdxToSubId = new ConcurrentHashMap();
    private static int mDefaultFallbackSubId = -1;
    private static int mDefaultPhoneId = Integer.MAX_VALUE;

    static class ScLocalLog {
        private int mMaxLines;
        private LinkedList<String> mLog = new LinkedList<>();
        private Time mNow = new Time();

        public ScLocalLog(int maxLines) {
            this.mMaxLines = maxLines;
        }

        public synchronized void log(String msg) {
            if (this.mMaxLines > 0) {
                int pid = Process.myPid();
                int tid = Process.myTid();
                this.mNow.setToNow();
                this.mLog.add(this.mNow.format("%m-%d %H:%M:%S") + " pid=" + pid + " tid=" + tid + " " + msg);
                while (this.mLog.size() > this.mMaxLines) {
                    this.mLog.remove();
                }
            }
        }

        public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            Iterator<String> itr = this.mLog.listIterator(0);
            int i = 0;
            while (true) {
                int i2 = i;
                if (itr.hasNext()) {
                    i = i2 + 1;
                    pw.println(Integer.toString(i2) + ": " + itr.next());
                    if (i % 10 == 0) {
                        pw.flush();
                    }
                }
            }
        }
    }

    public static SubscriptionController init(Phone phone) {
        SubscriptionController subscriptionController;
        synchronized (SubscriptionController.class) {
            if (sInstance == null) {
                sInstance = new SubscriptionController(phone);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            subscriptionController = sInstance;
        }
        return subscriptionController;
    }

    public static SubscriptionController init(Context c, CommandsInterface[] ci) {
        SubscriptionController subscriptionController;
        synchronized (SubscriptionController.class) {
            if (sInstance == null) {
                sInstance = new SubscriptionController(c);
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

    protected SubscriptionController(Context c) {
        this.mLocalLog = new ScLocalLog(MAX_LOCAL_LOG_LINES);
        this.mLock = new Object();
        this.mIsOP01 = false;
        this.mIsOP09A = false;
        this.mIsReady = false;
        this.PROPERTY_ICCID = new String[]{"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
        init(c);
    }

    protected void init(Context c) {
        this.mContext = c;
        this.mCM = CallManager.getInstance();
        this.mTelephonyManager = TelephonyManager.from(this.mContext);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        String operator = SystemProperties.get("persist.operator.optr", IWorldPhone.NO_OP);
        if (operator.equals("OP01")) {
            this.mIsOP01 = true;
            this.colorArr = this.mContext.getResources().getIntArray(R.array.config_allowedSecureInstantAppSettings);
        } else if (operator.equals("OP09") && "SEGDEFAULT".equals(SystemProperties.get("persist.operator.seg", UsimPBMemInfo.STRING_NOT_SET))) {
            this.mIsOP09A = true;
        }
        if (ServiceManager.getService("isub") == null) {
            ServiceManager.addService("isub", this);
        }
        logdl("[SubscriptionController] init by Context");
        if (this.mActiveList == null) {
            return;
        }
        this.mActiveList.clear();
    }

    private boolean isSubInfoReady() {
        return sSlotIdxToSubId.size() > 0;
    }

    private SubscriptionController(Phone phone) {
        this.mLocalLog = new ScLocalLog(MAX_LOCAL_LOG_LINES);
        this.mLock = new Object();
        this.mIsOP01 = false;
        this.mIsOP09A = false;
        this.mIsReady = false;
        this.PROPERTY_ICCID = new String[]{"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
        this.mContext = phone.getContext();
        this.mCM = CallManager.getInstance();
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        if (ServiceManager.getService("isub") == null) {
            ServiceManager.addService("isub", this);
        }
        logdl("[SubscriptionController] init by Phone");
    }

    private boolean canReadPhoneState(String callingPackage, String message) {
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", message);
            return true;
        } catch (SecurityException e) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", message);
            return this.mAppOps.noteOp(51, Binder.getCallingUid(), callingPackage) == 0;
        }
    }

    private void enforceModifyPhoneState(String message) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", message);
    }

    private void broadcastSimInfoContentChanged(Intent intentExt) {
        this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE"));
        Intent intent = new Intent("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        if (intentExt == null) {
            intent.putExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 4);
        }
        synchronized (this.mLock) {
            if (intentExt != null) {
                intent = intentExt;
            }
            sStickyIntent = intent;
            int detectedType = sStickyIntent.getIntExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 0);
            if (ENGDEBUG) {
                logd("broadcast intent ACTION_SUBINFO_RECORD_UPDATED with detectType:" + detectedType);
            }
            this.mContext.sendStickyBroadcast(sStickyIntent);
        }
    }

    public void notifySubscriptionInfoChanged() {
        ITelephonyRegistry tr = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
        try {
            tr.notifySubscriptionInfoChanged();
        } catch (RemoteException e) {
        }
        broadcastSimInfoContentChanged(null);
    }

    private SubscriptionInfo getSubInfoRecord(Cursor cursor) {
        int iconTint;
        int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
        String iccId = cursor.getString(cursor.getColumnIndexOrThrow("icc_id"));
        int simSlotIndex = cursor.getInt(cursor.getColumnIndexOrThrow("sim_id"));
        String displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
        String carrierName = cursor.getString(cursor.getColumnIndexOrThrow("carrier_name"));
        int nameSource = cursor.getInt(cursor.getColumnIndexOrThrow("name_source"));
        if (this.mIsOP01 && simSlotIndex >= 0 && simSlotIndex < getActiveSubInfoCountMax()) {
            iconTint = this.colorArr[simSlotIndex];
        } else {
            iconTint = cursor.getInt(cursor.getColumnIndexOrThrow("color"));
        }
        String number = cursor.getString(cursor.getColumnIndexOrThrow("number"));
        int dataRoaming = cursor.getInt(cursor.getColumnIndexOrThrow("data_roaming"));
        Bitmap iconBitmap = BitmapFactory.decodeResource(this.mContext.getResources(), R.drawable.ic_doc_generic);
        int mcc = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MCC));
        int mnc = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MNC));
        String countryIso = getSubscriptionCountryIso(id);
        int simProvisioningStatus = cursor.getInt(cursor.getColumnIndexOrThrow("sim_provisioning_status"));
        String line1Number = this.mTelephonyManager.getLine1Number(id);
        if (!TextUtils.isEmpty(line1Number) && !line1Number.equals(number)) {
            number = line1Number;
        }
        return new SubscriptionInfo(id, iccId, simSlotIndex, displayName, carrierName, nameSource, iconTint, number, dataRoaming, iconBitmap, mcc, mnc, countryIso, simProvisioningStatus);
    }

    private String getSubscriptionCountryIso(int subId) {
        int phoneId = getPhoneId(subId);
        if (phoneId < 0) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        return this.mTelephonyManager.getSimCountryIsoForPhone(phoneId);
    }

    private List<SubscriptionInfo> getSubInfo(String selection, Object queryKey) throws Throwable {
        ArrayList<SubscriptionInfo> subList;
        String[] selectionArgs = null;
        if (queryKey != null) {
            selectionArgs = new String[]{queryKey.toString()};
        }
        ArrayList<SubscriptionInfo> subList2 = null;
        Cursor cursor = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, selection, selectionArgs, null);
        try {
            if (cursor != null) {
                while (true) {
                    subList = subList2;
                    try {
                        if (!cursor.moveToNext()) {
                            break;
                        }
                        SubscriptionInfo subInfo = getSubInfoRecord(cursor);
                        if (subInfo != null) {
                            subList2 = subList == null ? new ArrayList<>() : subList;
                            subList2.add(subInfo);
                        } else {
                            subList2 = subList;
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                }
                subList2 = subList;
            } else {
                logd("Query fail");
            }
            if (cursor != null) {
                cursor.close();
            }
            return subList2;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private int getUnusedColor(String callingPackage) {
        List<SubscriptionInfo> availableSubInfos = getActiveSubscriptionInfoList(callingPackage);
        this.colorArr = this.mContext.getResources().getIntArray(R.array.config_allowedSecureInstantAppSettings);
        int colorIdx = 0;
        if (availableSubInfos != null) {
            for (int i = 0; i < this.colorArr.length; i++) {
                int j = 0;
                while (j < availableSubInfos.size() && this.colorArr[i] != availableSubInfos.get(j).getIconTint()) {
                    j++;
                }
                if (j == availableSubInfos.size()) {
                    return this.colorArr[i];
                }
            }
            colorIdx = availableSubInfos.size() % this.colorArr.length;
        }
        return this.colorArr[colorIdx];
    }

    public SubscriptionInfo getActiveSubscriptionInfo(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getActiveSubscriptionInfo")) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> subList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            if (subList != null) {
                for (SubscriptionInfo si : subList) {
                    if (si.getSubscriptionId() == subId) {
                        logd("[getActiveSubscriptionInfo]+ subId=" + subId + " subInfo=" + si);
                        return si;
                    }
                }
            }
            logd("[getActiveSubInfoForSubscriber]- subId=" + subId + " subList=" + subList + " subInfo=null");
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public SubscriptionInfo getActiveSubscriptionInfoForIccId(String iccId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getActiveSubscriptionInfoForIccId")) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> subList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            if (subList != null) {
                for (SubscriptionInfo si : subList) {
                    if (si.getIccId() == iccId) {
                        logd("[getActiveSubInfoUsingIccId]+ iccId=" + iccId + " subInfo=" + si);
                        return si;
                    }
                }
            }
            logd("[getActiveSubInfoUsingIccId]+ iccId=" + iccId + " subList=" + subList + " subInfo=null");
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int slotIdx, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getActiveSubscriptionInfoForSimSlotIndex")) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> subList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            if (subList != null) {
                for (SubscriptionInfo si : subList) {
                    if (si.getSimSlotIndex() == slotIdx) {
                        logd("[getActiveSubscriptionInfoForSimSlotIndex]+ slotIdx=" + slotIdx + " subId=" + si);
                        return si;
                    }
                }
                logd("[getActiveSubscriptionInfoForSimSlotIndex]+ slotIdx=" + slotIdx + " subId=null");
            } else {
                logd("[getActiveSubscriptionInfoForSimSlotIndex]+ subList=null");
            }
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public List<SubscriptionInfo> getAllSubInfoList(String callingPackage) {
        logd("[getAllSubInfoList]+");
        if (!canReadPhoneState(callingPackage, "getAllSubInfoList")) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> subList = getSubInfo(null, null);
            if (subList != null) {
                logd("[getAllSubInfoList]- " + subList.size() + " infos return");
            } else {
                logd("[getAllSubInfoList]- no info return");
            }
            return subList;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public List<SubscriptionInfo> getActiveSubscriptionInfoList(String callingPackage) {
        List<SubscriptionInfo> subList;
        if (!canReadPhoneState(callingPackage, "getActiveSubscriptionInfoList")) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            if (!isSubInfoReady()) {
                logdl("[getActiveSubInfoList] Sub Controller not ready");
                return null;
            }
            if (this.mActiveList == null) {
                subList = null;
            } else {
                subList = new ArrayList<>();
                subList.addAll(this.mActiveList);
            }
            if (subList == null) {
                logdl("[getActiveSubInfoList]- no info return");
            }
            return subList;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getActiveSubInfoCount(String callingPackage) {
        logd("[getActiveSubInfoCount]+");
        if (!canReadPhoneState(callingPackage, "getActiveSubInfoCount")) {
            return 0;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> records = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            if (records == null) {
                logd("[getActiveSubInfoCount] records null");
                return 0;
            }
            logd("[getActiveSubInfoCount]- count: " + records.size());
            return records.size();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getAllSubInfoCount(String callingPackage) {
        logd("[getAllSubInfoCount]+");
        if (!canReadPhoneState(callingPackage, "getAllSubInfoCount")) {
            return 0;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            Cursor cursor = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                try {
                    int count = cursor.getCount();
                    logd("[getAllSubInfoCount]- " + count + " SUB(s) in DB");
                    return count;
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            logd("[getAllSubInfoCount]- no SUB in DB");
            return 0;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getActiveSubInfoCountMax() {
        return this.mTelephonyManager.getSimCount();
    }

    public int addSubInfoRecord(String iccId, int slotId) {
        boolean setDisplayName;
        logdl("[addSubInfoRecord]+ iccId:" + SubscriptionInfo.givePrintableIccid(iccId) + " slotId:" + slotId);
        enforceModifyPhoneState("addSubInfoRecord");
        SubscriptionInfo newInfo = null;
        long identity = Binder.clearCallingIdentity();
        try {
            if (iccId == null) {
                logdl("[addSubInfoRecord]- null iccId");
                return -1;
            }
            ContentResolver resolver = this.mContext.getContentResolver();
            Cursor cursor = resolver.query(SubscriptionManager.CONTENT_URI, null, "icc_id=?", new String[]{iccId}, null);
            int color = getUnusedColor(this.mContext.getOpPackageName());
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int subId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                        int oldSimInfoId = cursor.getInt(cursor.getColumnIndexOrThrow("sim_id"));
                        int nameSource = cursor.getInt(cursor.getColumnIndexOrThrow("name_source"));
                        cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
                        ContentValues value = new ContentValues();
                        if (slotId != oldSimInfoId) {
                            value.put("sim_id", Integer.valueOf(slotId));
                            if (this.mIsOP01) {
                                value.put("color", Integer.valueOf(this.colorArr[slotId]));
                            }
                        }
                        setDisplayName = nameSource != 2;
                        if (value.size() > 0) {
                            resolver.update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
                        }
                        logdl("[addSubInfoRecord] Record already exists");
                    } else {
                        setDisplayName = true;
                        ContentValues value2 = new ContentValues();
                        value2.put("icc_id", iccId);
                        value2.put("color", Integer.valueOf(color));
                        value2.put("sim_id", Integer.valueOf(slotId));
                        value2.put("carrier_name", UsimPBMemInfo.STRING_NOT_SET);
                        Uri uri = resolver.insert(SubscriptionManager.CONTENT_URI, value2);
                        logdl("[addSubInfoRecord] New record created: " + uri);
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            Cursor cursor2 = resolver.query(SubscriptionManager.CONTENT_URI, null, "sim_id=?", new String[]{String.valueOf(slotId)}, null);
            if (cursor2 != null) {
                try {
                    if (cursor2.moveToFirst()) {
                        do {
                            int subId2 = cursor2.getInt(cursor2.getColumnIndexOrThrow("_id"));
                            sSlotIdxToSubId.put(Integer.valueOf(slotId), Integer.valueOf(subId2));
                            int subIdCountMax = getActiveSubInfoCountMax();
                            int defaultSubId = getDefaultSubId();
                            logdl("[addSubInfoRecord] sSlotIdxToSubId.size=" + sSlotIdxToSubId.size() + " slotId=" + slotId + " subId=" + subId2 + " mDefaultFallbackSubId=" + mDefaultFallbackSubId + " defaultSubId=" + defaultSubId + " simCount=" + subIdCountMax);
                            if (!SubscriptionManager.isValidSubscriptionId(defaultSubId) || subIdCountMax == 1 || !isActiveSubId(defaultSubId) || !isActiveSubId(mDefaultFallbackSubId)) {
                                setDefaultFallbackSubId(subId2);
                            }
                            if (subIdCountMax == 1) {
                                logdl("[addSubInfoRecord] one sim set defaults to subId=" + subId2);
                                setDefaultDataSubId(subId2);
                                setDefaultSmsSubId(subId2);
                                setDefaultVoiceSubId(subId2);
                            }
                            logdl("[addSubInfoRecord] hashmap(" + slotId + "," + subId2 + ")");
                        } while (cursor2.moveToNext());
                    }
                } finally {
                    if (cursor2 != null) {
                        if (cursor2.moveToFirst()) {
                            getSubInfoRecord(cursor2);
                        }
                        cursor2.close();
                    }
                }
            }
            int[] subIds = getSubId(slotId);
            if (subIds == null || subIds.length == 0) {
                logdl("[addSubInfoRecord]- getSubId failed subIds == null || length == 0 subIds=" + subIds);
                return -1;
            }
            if (setDisplayName) {
                String simCarrierName = this.mTelephonyManager.getSimOperatorName(subIds[0]);
                String simNumeric = this.mTelephonyManager.getSimOperatorNumeric(subIds[0]);
                String simMvnoName = "20404".equals(simNumeric) ? UsimPBMemInfo.STRING_NOT_SET : SpnOverride.getInstance().lookupOperatorNameForDisplayName(subIds[0], simNumeric, true, this.mContext);
                if (ENGDEBUG) {
                    logd("[addSubInfoRecord]- simNumeric: " + simNumeric + ", simMvnoName: " + simMvnoName);
                }
                String nameToSet = !TextUtils.isEmpty(simMvnoName) ? simMvnoName : !TextUtils.isEmpty(simCarrierName) ? simCarrierName : "CARD " + Integer.toString(slotId + 1);
                ContentValues value3 = new ContentValues();
                value3.put("display_name", nameToSet);
                resolver.update(SubscriptionManager.CONTENT_URI, value3, "_id=" + Long.toString(subIds[0]), null);
                if (newInfo != null) {
                    newInfo.setDisplayName(nameToSet);
                }
                logdl("[addSubInfoRecord] sim name = " + nameToSet);
            }
            sPhones[slotId].updateDataConnectionTracker();
            logdl("[addSubInfoRecord]- info size=" + sSlotIdxToSubId.size());
            Binder.restoreCallingIdentity(identity);
            if (this.mActiveList == null) {
                this.mActiveList = new CopyOnWriteArrayList();
            }
            if (newInfo != null) {
                boolean isInsert = true;
                Iterator record$iterator = this.mActiveList.iterator();
                while (true) {
                    if (!record$iterator.hasNext()) {
                        break;
                    }
                    SubscriptionInfo record = (SubscriptionInfo) record$iterator.next();
                    if (newInfo.getSimSlotIndex() == record.getSimSlotIndex()) {
                        isInsert = false;
                        break;
                    }
                }
                if (isInsert) {
                    int insertAt = 0;
                    for (SubscriptionInfo record2 : this.mActiveList) {
                        if (newInfo.getSimSlotIndex() > record2.getSimSlotIndex()) {
                            insertAt++;
                        }
                    }
                    this.mActiveList.add(insertAt, newInfo);
                    logd("[addSubInfoRecord] insertAt=" + insertAt);
                }
            }
            if (!ENGDEBUG) {
                return 0;
            }
            logd("[addSubInfoRecord] Active list size=" + this.mActiveList.size());
            return 0;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean setPlmnSpn(int slotId, boolean showPlmn, String plmn, boolean showSpn, String spn) {
        synchronized (this.mLock) {
            int[] subIds = getSubId(slotId);
            if (this.mContext.getPackageManager().resolveContentProvider(SubscriptionManager.CONTENT_URI.getAuthority(), 0) == null || subIds == null || !SubscriptionManager.isValidSubscriptionId(subIds[0]) || !isReady()) {
                logd("[setPlmnSpn] No valid subscription to store info");
                notifySubscriptionInfoChanged();
                return false;
            }
            String carrierText = UsimPBMemInfo.STRING_NOT_SET;
            if (showPlmn) {
                carrierText = plmn;
                if (showSpn && !Objects.equals(spn, plmn)) {
                    String separator = this.mContext.getString(R.string.install_carrier_app_notification_button).toString();
                    carrierText = plmn + separator + spn;
                }
            } else if (showSpn) {
                carrierText = spn;
            }
            for (int i : subIds) {
                setCarrierText(carrierText, i);
            }
            return true;
        }
    }

    public int setCarrierText(String text, int subId) {
        logd("[setCarrierText]+ text:" + text + " subId:" + subId);
        enforceModifyPhoneState("setCarrierText");
        long identity = Binder.clearCallingIdentity();
        try {
            ContentValues value = new ContentValues(1);
            value.put("carrier_name", text);
            int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
            if (ENGDEBUG) {
                logd("[setCarrierText]- update result :" + result);
            }
            if (this.mActiveList != null && result > 0) {
                for (SubscriptionInfo record : this.mActiveList) {
                    if (record.getSubscriptionId() == subId) {
                        record.setCarrierName(text);
                    }
                }
            }
            notifySubscriptionInfoChanged();
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int setIconTint(int tint, int subId) {
        logd("[setIconTint]+ tint:" + tint + " subId:" + subId);
        enforceModifyPhoneState("setIconTint");
        long identity = Binder.clearCallingIdentity();
        try {
            validateSubId(subId);
            ContentValues value = new ContentValues(1);
            value.put("color", Integer.valueOf(tint));
            logd("[setIconTint]- tint:" + tint + " set");
            int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
            logd("[setIconTint]- update result :" + result);
            if (this.mActiveList != null && result > 0) {
                for (SubscriptionInfo record : this.mActiveList) {
                    if (record.getSubscriptionId() == subId) {
                        record.setIconTint(tint);
                    }
                }
            }
            notifySubscriptionInfoChanged();
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int setDisplayName(String displayName, int subId) {
        return setDisplayNameUsingSrc(displayName, subId, -1L);
    }

    public int setDisplayNameUsingSrc(String displayName, int subId, long nameSource) {
        String nameToSet;
        logd("[setDisplayName]+  displayName:" + displayName + " subId:" + subId + " nameSource:" + nameSource);
        enforceModifyPhoneState("setDisplayNameUsingSrc");
        long identity = Binder.clearCallingIdentity();
        try {
            validateSubId(subId);
            if (displayName == null) {
                nameToSet = this.mContext.getString(R.string.unknownName);
            } else {
                nameToSet = displayName;
            }
            ContentValues value = new ContentValues(1);
            value.put("display_name", nameToSet);
            if (nameSource >= 0) {
                logd("Set nameSource=" + nameSource);
                value.put("name_source", Long.valueOf(nameSource));
            }
            logd("[setDisplayName]- mDisplayName:" + nameToSet + " set");
            int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
            logd("[setDisplayName]- update result :" + result);
            if (this.mActiveList != null && result > 0) {
                for (SubscriptionInfo record : this.mActiveList) {
                    if (record.getSubscriptionId() == subId) {
                        record.setDisplayName(nameToSet);
                        if (nameSource >= 0) {
                            record.setNameSource((int) nameSource);
                        }
                    }
                }
            }
            notifySubscriptionInfoChanged();
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int setDisplayNumber(String number, int subId) {
        logd("[setDisplayNumber]+ subId:" + subId);
        enforceModifyPhoneState("setDisplayNumber");
        long identity = Binder.clearCallingIdentity();
        try {
            validateSubId(subId);
            int phoneId = getPhoneId(subId);
            if (number == null || phoneId < 0 || phoneId >= this.mTelephonyManager.getPhoneCount()) {
                logd("[setDispalyNumber]- fail");
                return -1;
            }
            ContentValues value = new ContentValues(1);
            value.put("number", number);
            int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
            logd("[setDisplayNumber]- update result :" + result);
            if (this.mActiveList != null && result > 0) {
                for (SubscriptionInfo record : this.mActiveList) {
                    if (record.getSubscriptionId() == subId) {
                        record.setNumber(number);
                    }
                }
            }
            notifySubscriptionInfoChanged();
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int setDataRoaming(int roaming, int subId) {
        logd("[setDataRoaming]+ roaming:" + roaming + " subId:" + subId);
        enforceModifyPhoneState("setDataRoaming");
        long identity = Binder.clearCallingIdentity();
        try {
            validateSubId(subId);
            if (roaming < 0) {
                logd("[setDataRoaming]- fail");
                return -1;
            }
            ContentValues value = new ContentValues(1);
            value.put("data_roaming", Integer.valueOf(roaming));
            logd("[setDataRoaming]- roaming:" + roaming + " set");
            int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
            logd("[setDataRoaming]- update result :" + result);
            if (this.mActiveList != null && result > 0) {
                for (SubscriptionInfo record : this.mActiveList) {
                    if (record.getSubscriptionId() == subId) {
                        record.setDataRoaming(roaming);
                    }
                }
            }
            notifySubscriptionInfoChanged();
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int setMccMnc(String mccMnc, int subId) {
        int mcc = 0;
        int mnc = 0;
        try {
            mcc = Integer.parseInt(mccMnc.substring(0, 3));
            mnc = Integer.parseInt(mccMnc.substring(3));
        } catch (NumberFormatException e) {
            loge("[setMccMnc] - couldn't parse mcc/mnc: " + mccMnc);
        }
        logd("[setMccMnc]+ mcc/mnc:" + mcc + "/" + mnc + " subId:" + subId);
        ContentValues value = new ContentValues(2);
        value.put(Telephony.Carriers.MCC, Integer.valueOf(mcc));
        value.put(Telephony.Carriers.MNC, Integer.valueOf(mnc));
        int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
        if (ENGDEBUG) {
            logd("[setMccMnc]- update result :" + result);
        }
        if (this.mActiveList != null && result > 0) {
            for (SubscriptionInfo record : this.mActiveList) {
                if (record.getSubscriptionId() == subId) {
                    record.setMcc(mcc);
                    record.setMnc(mnc);
                }
            }
        }
        notifySubscriptionInfoChanged();
        return result;
    }

    public int setSimProvisioningStatus(int provisioningStatus, int subId) {
        logd("[setSimProvisioningStatus]+ provisioningStatus:" + provisioningStatus + " subId:" + subId);
        enforceModifyPhoneState("setSimProvisioningStatus");
        long identity = Binder.clearCallingIdentity();
        try {
            validateSubId(subId);
            if (provisioningStatus < 0 || provisioningStatus > 2) {
                logd("[setSimProvisioningStatus]- fail with wrong provisioningStatus");
                return -1;
            }
            ContentValues value = new ContentValues(1);
            value.put("sim_provisioning_status", Integer.valueOf(provisioningStatus));
            int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
            notifySubscriptionInfoChanged();
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getSlotId(int subId) {
        if (subId == Integer.MAX_VALUE) {
            logd("[getSlotId]+ subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID");
            subId = getDefaultSubId();
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            logd("[getSlotId]- subId invalid");
            return -1;
        }
        int size = sSlotIdxToSubId.size();
        if (size == 0) {
            logd("[getSlotId]- size == 0, return SIM_NOT_INSERTED instead, subId =" + subId);
            return -1;
        }
        for (Map.Entry<Integer, Integer> entry : sSlotIdxToSubId.entrySet()) {
            int sim = entry.getKey().intValue();
            int sub = entry.getValue().intValue();
            if (subId == sub) {
                return sim;
            }
        }
        logd("[getSlotId]- return INVALID_SIM_SLOT_INDEX, subId = " + subId);
        return -1;
    }

    @Deprecated
    public int[] getSubId(int slotIdx) {
        if (slotIdx == Integer.MAX_VALUE) {
            slotIdx = getSlotId(getDefaultSubId());
        }
        if (!SubscriptionManager.isValidSlotId(slotIdx)) {
            logd("[getSubId]- invalid slotIdx=" + slotIdx);
            return null;
        }
        int size = sSlotIdxToSubId.size();
        if (size == 0) {
            return getDummySubIds(slotIdx);
        }
        ArrayList<Integer> subIds = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : sSlotIdxToSubId.entrySet()) {
            int slot = entry.getKey().intValue();
            int sub = entry.getValue().intValue();
            if (slotIdx == slot) {
                subIds.add(Integer.valueOf(sub));
            }
        }
        int numSubIds = subIds.size();
        if (numSubIds > 0) {
            int[] subIdArr = new int[numSubIds];
            for (int i = 0; i < numSubIds; i++) {
                subIdArr[i] = subIds.get(i).intValue();
            }
            return subIdArr;
        }
        logd("[getSubId]- numSubIds == 0, return DummySubIds slotIdx=" + slotIdx);
        return getDummySubIds(slotIdx);
    }

    public int getPhoneId(int subId) {
        if (subId == Integer.MAX_VALUE) {
            subId = getDefaultSubId();
            logdl("[getPhoneId] asked for default subId=" + subId);
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            if (subId > (-2) - getActiveSubInfoCountMax()) {
                return (-2) - subId;
            }
            return -1;
        }
        int size = sSlotIdxToSubId.size();
        if (size == 0) {
            int phoneId = mDefaultPhoneId;
            logd("[getPhoneId]- no sims, returning default phoneId=" + phoneId + ", subId" + subId);
            return phoneId;
        }
        for (Map.Entry<Integer, Integer> entry : sSlotIdxToSubId.entrySet()) {
            int sim = entry.getKey().intValue();
            int sub = entry.getValue().intValue();
            if (subId == sub) {
                return sim;
            }
        }
        int phoneId2 = mDefaultPhoneId;
        logdl("[getPhoneId]- subId=" + subId + " not found return default phoneId=" + phoneId2);
        return phoneId2;
    }

    private int[] getDummySubIds(int slotIdx) {
        int numSubs = getActiveSubInfoCountMax();
        if (numSubs > 0) {
            int[] dummyValues = new int[numSubs];
            for (int i = 0; i < numSubs; i++) {
                dummyValues[i] = (-2) - slotIdx;
            }
            return dummyValues;
        }
        return null;
    }

    public int clearSubInfo() {
        enforceModifyPhoneState("clearSubInfo");
        long identity = Binder.clearCallingIdentity();
        try {
            int size = sSlotIdxToSubId.size();
            if (size == 0) {
                logdl("[clearSubInfo]- no simInfo size=" + size);
                return 0;
            }
            setReadyState(false);
            sSlotIdxToSubId.clear();
            this.mActiveList.clear();
            logdl("[clearSubInfo]- clear size=" + size);
            return size;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int clearSubInfoUsingPhoneId(int phoneId) {
        enforceModifyPhoneState("clearSubInfoUsingPhoneId");
        long identity = Binder.clearCallingIdentity();
        try {
            int size = sSlotIdxToSubId.size();
            if (size == 0) {
                if (ENGDEBUG) {
                    logdl("[clearSubInfoUsingPhoneId]- no simInfo size=" + size);
                }
                return 0;
            }
            if (!SubscriptionManager.isValidPhoneId(phoneId)) {
                if (ENGDEBUG) {
                    logd("[clearSubInfoUsingPhoneId]- invalid phoneId=" + phoneId);
                }
                return 0;
            }
            setReadyState(false);
            sSlotIdxToSubId.remove(Integer.valueOf(phoneId));
            int i = this.mActiveList.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                SubscriptionInfo record = this.mActiveList.get(i);
                if (record.getSimSlotIndex() == phoneId) {
                    this.mActiveList.remove(i);
                    if (ENGDEBUG) {
                        logdl("[clearSubInfoUsingPhoneId]- clear phoneId =" + phoneId + " i = " + i);
                    }
                } else {
                    i--;
                }
            }
            return 1;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void logvl(String msg) {
        logv(msg);
        this.mLocalLog.log(msg);
    }

    private void logv(String msg) {
        Rlog.v(LOG_TAG, msg);
    }

    private void logdl(String msg) {
        logd(msg);
        this.mLocalLog.log(msg);
    }

    private static void slogd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void logel(String msg) {
        loge(msg);
        this.mLocalLog.log(msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    public int getDefaultSubId() {
        int subId;
        boolean isVoiceCapable = this.mContext.getResources().getBoolean(R.^attr-private.frameDuration);
        if (isVoiceCapable) {
            subId = getDefaultVoiceSubId();
        } else {
            subId = getDefaultDataSubId();
        }
        if (!isActiveSubId(subId)) {
            int subId2 = mDefaultFallbackSubId;
            return subId2;
        }
        return subId;
    }

    public void setDefaultSmsSubId(int subId) {
        enforceModifyPhoneState("setDefaultSmsSubId");
        if (subId == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultSmsSubId called with DEFAULT_SUB_ID");
        }
        logdl("[setDefaultSmsSubId] subId=" + subId);
        Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_sms", subId);
        broadcastDefaultSmsSubIdChanged(subId);
    }

    private void broadcastDefaultSmsSubIdChanged(int subId) {
        logdl("[broadcastDefaultSmsSubIdChanged] subId=" + subId);
        Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED");
        intent.addFlags(536870912);
        intent.putExtra("subscription", subId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public int getDefaultSmsSubId() {
        int subId = Settings.Global.getInt(this.mContext.getContentResolver(), "multi_sim_sms", -1);
        return subId;
    }

    public void setDefaultVoiceSubId(int subId) {
        enforceModifyPhoneState("setDefaultVoiceSubId");
        if (subId == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultVoiceSubId called with DEFAULT_SUB_ID");
        }
        logdl("[setDefaultVoiceSubId] subId=" + subId);
        Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_voice_call", subId);
        broadcastDefaultVoiceSubIdChanged(subId);
    }

    private void broadcastDefaultVoiceSubIdChanged(int subId) {
        logdl("[broadcastDefaultVoiceSubIdChanged] subId=" + subId);
        Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED");
        intent.addFlags(536870912);
        intent.putExtra("subscription", subId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public int getDefaultVoiceSubId() {
        int subId = Settings.Global.getInt(this.mContext.getContentResolver(), "multi_sim_voice_call", -1);
        return subId;
    }

    public int getDefaultDataSubId() {
        int subId = Settings.Global.getInt(this.mContext.getContentResolver(), "multi_sim_data_call", -1);
        return subId;
    }

    public void setDefaultDataSubId(int subId) {
        setDefaultDataSubIdWithResult(subId);
    }

    private boolean isNeedCapabilitySwitch(int phoneId) {
        int mPhoneNum = sPhones.length;
        int[] simOpInfo = new int[mPhoneNum];
        int[] simType = new int[mPhoneNum];
        int insertedState = 0;
        int insertedSimCount = 0;
        int op01SimCount = 0;
        int op02SimCount = 0;
        String[] currIccId = new String[mPhoneNum];
        String opSpec = SystemProperties.get("persist.operator.optr", IWorldPhone.NO_OP);
        if (IWorldPhone.NO_OP.equals(opSpec) && RadioCapabilitySwitchUtil.isPS2SupportLTE()) {
            for (int i = 0; i < mPhoneNum; i++) {
                currIccId[i] = SystemProperties.get(this.PROPERTY_ICCID[i]);
                if (currIccId[i] != null && !UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                    if (!"N/A".equals(currIccId[i])) {
                        insertedSimCount++;
                        insertedState |= 1 << i;
                    }
                } else {
                    logd("error: iccid not found, wait for next sub ready");
                    return false;
                }
            }
            logd("setCapabilityIfNeeded : Inserted SIM count: " + insertedSimCount + ", insertedStatus: " + insertedState);
            if (insertedSimCount == 0 || !RadioCapabilitySwitchUtil.getSimInfo(simOpInfo, simType, insertedState)) {
                return false;
            }
            for (int i2 = 0; i2 < mPhoneNum; i2++) {
                if (2 == simOpInfo[i2]) {
                    op01SimCount++;
                } else if (3 == simOpInfo[i2]) {
                    op02SimCount++;
                }
            }
            if (op02SimCount != 1 || insertedSimCount != 1) {
                if (op02SimCount == 2 && insertedSimCount == 2) {
                    return false;
                }
                return true;
            }
            return false;
        }
        return true;
    }

    public boolean setDefaultDataSubIdWithResult(int subId) {
        int raf;
        enforceModifyPhoneState("setDefaultDataSubId");
        if (subId == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultDataSubId called with DEFAULT_SUB_ID");
        }
        ProxyController proxyController = ProxyController.getInstance();
        int len = sPhones.length;
        logdl("[setDefaultDataSubId] num phones=" + len + ", subId=" + subId);
        try {
            if (SubscriptionManager.isValidSubscriptionId(subId) || SystemProperties.getInt("ro.mtk_external_sim_support", 0) == 1) {
                RadioAccessFamily[] rafs = new RadioAccessFamily[len];
                int targetPhoneId = 0;
                boolean atLeastOneMatch = false;
                for (int phoneId = 0; phoneId < len; phoneId++) {
                    Phone phone = sPhones[phoneId];
                    int id = phone.getSubId();
                    if (id == subId) {
                        raf = proxyController.getMaxRafSupported();
                        atLeastOneMatch = true;
                        targetPhoneId = phoneId;
                    } else {
                        raf = proxyController.getMinRafSupported();
                    }
                    logdl("[setDefaultDataSubId] phoneId=" + phoneId + " subId=" + id + " RAF=" + raf);
                    rafs[phoneId] = new RadioAccessFamily(phoneId, raf);
                }
                if (atLeastOneMatch) {
                    if (isNeedCapabilitySwitch(targetPhoneId)) {
                        proxyController.setRadioCapability(rafs);
                    } else {
                        logdl("[setDefaultDataSubId] no need to capability switch on L+L.");
                    }
                } else {
                    logdl("[setDefaultDataSubId] no valid subId's found - not updating.");
                }
            }
            updateAllDataConnectionTrackers();
            Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_data_call", subId);
            broadcastDefaultDataSubIdChanged(subId);
            return true;
        } catch (RuntimeException e) {
            logd("[setDefaultDataSubId] setRadioCapability: Runtime Exception");
            e.printStackTrace();
            return false;
        }
    }

    private void updateAllDataConnectionTrackers() {
        int len = sPhones.length;
        logdl("[updateAllDataConnectionTrackers] sPhones.length=" + len);
        for (int phoneId = 0; phoneId < len; phoneId++) {
            sPhones[phoneId].updateDataConnectionTracker();
        }
    }

    private void broadcastDefaultDataSubIdChanged(int subId) {
        logdl("[broadcastDefaultDataSubIdChanged] subId=" + subId);
        Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intent.addFlags(536870912);
        intent.putExtra("subscription", subId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void setDefaultFallbackSubId(int subId) {
        if (subId == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultSubId called with DEFAULT_SUB_ID");
        }
        logdl("[setDefaultFallbackSubId] subId=" + subId);
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return;
        }
        int phoneId = getPhoneId(subId);
        if (phoneId >= 0 && (phoneId < this.mTelephonyManager.getPhoneCount() || this.mTelephonyManager.getSimCount() == 1)) {
            logdl("[setDefaultFallbackSubId] set mDefaultFallbackSubId=" + subId);
            mDefaultFallbackSubId = subId;
            String defaultMccMnc = this.mTelephonyManager.getSimOperatorNumericForPhone(phoneId);
            MccTable.updateMccMncConfiguration(this.mContext, defaultMccMnc, false);
            Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_SUBSCRIPTION_CHANGED");
            intent.addFlags(536870912);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, phoneId, subId);
            logdl("[setDefaultFallbackSubId] broadcast default subId changed phoneId=" + phoneId + " subId=" + subId);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            return;
        }
        logdl("[setDefaultFallbackSubId] not set invalid phoneId=" + phoneId + " subId=" + subId);
    }

    public void clearDefaultsForInactiveSubIds() {
        enforceModifyPhoneState("clearDefaultsForInactiveSubIds");
        long identity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> records = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            logdl("[clearDefaultsForInactiveSubIds] records: " + records);
            if ((this.mIsOP09A || "OP02".equals(SystemProperties.get("persist.operator.optr"))) ? true : "OP01".equals(SystemProperties.get("persist.operator.optr"))) {
                logd("clearDefaultsForInactiveSubIds, don't set default data for customization!");
            } else if (shouldDefaultBeCleared(records, getDefaultDataSubId())) {
                logd("[clearDefaultsForInactiveSubIds] clearing default data sub id");
                setDefaultDataSubId(-1);
            }
            if (shouldDefaultBeCleared(records, getDefaultSmsSubId())) {
                logdl("[clearDefaultsForInactiveSubIds] clearing default sms sub id");
                setDefaultSmsSubId(-1);
            }
            if (shouldDefaultBeCleared(records, getDefaultVoiceSubId())) {
                logdl("[clearDefaultsForInactiveSubIds] clearing default voice sub id");
                setDefaultVoiceSubId(-1);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean shouldDefaultBeCleared(List<SubscriptionInfo> records, int subId) {
        logdl("[shouldDefaultBeCleared: subId] " + subId);
        if (records == null) {
            logdl("[shouldDefaultBeCleared] return true no records subId=" + subId);
            return true;
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            logdl("[shouldDefaultBeCleared] return false only one subId, subId=" + subId);
            return false;
        }
        for (SubscriptionInfo record : records) {
            int id = record.getSubscriptionId();
            logdl("[shouldDefaultBeCleared] Record.id: " + id);
            if (id == subId) {
                logdl("[shouldDefaultBeCleared] return false subId is active, subId=" + subId);
                return false;
            }
        }
        logdl("[shouldDefaultBeCleared] return true not active subId=" + subId);
        return true;
    }

    public int getSubIdUsingPhoneId(int phoneId) {
        int[] subIds = getSubId(phoneId);
        if (subIds == null || subIds.length == 0) {
            return -1;
        }
        return subIds[0];
    }

    public int[] getSubIdUsingSlotId(int slotId) {
        return getSubId(slotId);
    }

    public List<SubscriptionInfo> getSubInfoUsingSlotIdWithCheck(int slotId, boolean needCheck, String callingPackage) {
        ArrayList<SubscriptionInfo> subList;
        logd("[getSubInfoUsingSlotIdWithCheck]+ slotId:" + slotId);
        if (!canReadPhoneState(callingPackage, "getSubInfoUsingSlotIdWithCheck")) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        if (slotId == Integer.MAX_VALUE) {
            try {
                slotId = getSlotId(getDefaultSubId());
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        if (!SubscriptionManager.isValidSlotId(slotId)) {
            logd("[getSubInfoUsingSlotIdWithCheck]- invalid slotId");
            return null;
        }
        if (needCheck && !isSubInfoReady()) {
            logd("[getSubInfoUsingSlotIdWithCheck]- not ready");
            return null;
        }
        Cursor cursor = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, "sim_id=?", new String[]{String.valueOf(slotId)}, null);
        ArrayList<SubscriptionInfo> subList2 = null;
        if (cursor != null) {
            while (true) {
                subList = subList2;
                try {
                    if (!cursor.moveToNext()) {
                        break;
                    }
                    SubscriptionInfo subInfo = getSubInfoRecord(cursor);
                    if (subInfo != null) {
                        subList2 = subList == null ? new ArrayList<>() : subList;
                        try {
                            subList2.add(subInfo);
                        } catch (Throwable th) {
                            th = th;
                            if (cursor != null) {
                                cursor.close();
                            }
                            throw th;
                        }
                    } else {
                        subList2 = subList;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            subList2 = subList;
        }
        if (cursor != null) {
            cursor.close();
        }
        logd("[getSubInfoUsingSlotId]- null info return");
        return subList2;
    }

    private void validateSubId(int subId) {
        logd("validateSubId subId: " + subId);
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            throw new RuntimeException("Invalid sub id passed as parameter");
        }
        if (subId != Integer.MAX_VALUE) {
        } else {
            throw new RuntimeException("Default sub id passed as parameter");
        }
    }

    public void updatePhonesAvailability(Phone[] phones) {
        sPhones = phones;
    }

    public int[] getActiveSubIdList() {
        Set<Map.Entry<Integer, Integer>> simInfoSet = sSlotIdxToSubId.entrySet();
        int[] subIdArr = new int[simInfoSet.size()];
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : simInfoSet) {
            int sub = entry.getValue().intValue();
            subIdArr[i] = sub;
            i++;
        }
        return subIdArr;
    }

    public boolean isActiveSubId(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        boolean retVal = sSlotIdxToSubId.containsValue(Integer.valueOf(subId));
        return retVal;
    }

    public int getSimStateForSlotIdx(int slotIdx) {
        Phone phone;
        IccCard icc;
        IccCardConstants.State simState;
        if (slotIdx < 0 || (phone = PhoneFactory.getPhone(slotIdx)) == null || (icc = phone.getIccCard()) == null) {
            simState = IccCardConstants.State.UNKNOWN;
        } else {
            simState = icc.getState();
        }
        return simState.ordinal();
    }

    public void setSubscriptionProperty(int subId, String propKey, String propValue) {
        enforceModifyPhoneState("setSubscriptionProperty");
        long token = Binder.clearCallingIdentity();
        ContentResolver resolver = this.mContext.getContentResolver();
        ContentValues value = new ContentValues();
        if (propKey.equals("enable_cmas_extreme_threat_alerts") || propKey.equals("enable_cmas_severe_threat_alerts") || propKey.equals("enable_cmas_amber_alerts") || propKey.equals("enable_emergency_alerts") || propKey.equals("alert_sound_duration") || propKey.equals("alert_reminder_interval") || propKey.equals("enable_alert_vibrate") || propKey.equals("enable_alert_speech") || propKey.equals("enable_etws_test_alerts") || propKey.equals("enable_channel_50_alerts") || propKey.equals("enable_cmas_test_alerts") || propKey.equals("show_cmas_opt_out_dialog")) {
            value.put(propKey, Integer.valueOf(Integer.parseInt(propValue)));
        } else {
            logd("Invalid column name");
        }
        resolver.update(SubscriptionManager.CONTENT_URI, value, "_id=" + Integer.toString(subId), null);
        Binder.restoreCallingIdentity(token);
    }

    public String getSubscriptionProperty(int subId, String propKey, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getSubInfoUsingSlotIdWithCheck")) {
            return null;
        }
        String resultValue = null;
        ContentResolver resolver = this.mContext.getContentResolver();
        Cursor cursor = resolver.query(SubscriptionManager.CONTENT_URI, new String[]{propKey}, InboundSmsHandler.SELECT_BY_ID, new String[]{subId + UsimPBMemInfo.STRING_NOT_SET}, null);
        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    if (propKey.equals("enable_cmas_extreme_threat_alerts") || propKey.equals("enable_cmas_severe_threat_alerts") || propKey.equals("enable_cmas_amber_alerts") || propKey.equals("enable_emergency_alerts") || propKey.equals("alert_sound_duration") || propKey.equals("alert_reminder_interval") || propKey.equals("enable_alert_vibrate") || propKey.equals("enable_alert_speech") || propKey.equals("enable_etws_test_alerts") || propKey.equals("enable_channel_50_alerts") || propKey.equals("enable_cmas_test_alerts") || propKey.equals("show_cmas_opt_out_dialog")) {
                        resultValue = cursor.getInt(0) + UsimPBMemInfo.STRING_NOT_SET;
                    } else {
                        logd("Invalid column name");
                    }
                } else {
                    logd("Valid row not present in db");
                }
            } else {
                logd("Query failed");
            }
            if (cursor != null) {
                cursor.close();
            }
            logd("getSubscriptionProperty Query value = " + resultValue);
            return resultValue;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    private static void printStackTrace(String msg) {
        RuntimeException re = new RuntimeException();
        slogd("StackTrace - " + msg);
        StackTraceElement[] st = re.getStackTrace();
        boolean first = true;
        for (StackTraceElement ste : st) {
            if (first) {
                first = false;
            } else {
                slogd(ste.toString());
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", "Requires DUMP");
        long token = Binder.clearCallingIdentity();
        try {
            pw.println("SubscriptionController:");
            pw.println(" defaultSubId=" + getDefaultSubId());
            pw.println(" defaultDataSubId=" + getDefaultDataSubId());
            pw.println(" defaultVoiceSubId=" + getDefaultVoiceSubId());
            pw.println(" defaultSmsSubId=" + getDefaultSmsSubId());
            pw.println(" defaultDataPhoneId=" + SubscriptionManager.from(this.mContext).getDefaultDataPhoneId());
            pw.println(" defaultVoicePhoneId=" + SubscriptionManager.getDefaultVoicePhoneId());
            pw.println(" defaultSmsPhoneId=" + SubscriptionManager.from(this.mContext).getDefaultSmsPhoneId());
            pw.flush();
            for (Map.Entry<Integer, Integer> entry : sSlotIdxToSubId.entrySet()) {
                pw.println(" sSlotIdxToSubId[" + entry.getKey() + "]: subId=" + entry.getValue());
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
            List<SubscriptionInfo> sirl = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            if (sirl != null) {
                pw.println(" ActiveSubInfoList:");
                for (SubscriptionInfo entry2 : sirl) {
                    pw.println("  " + entry2.toString());
                }
            } else {
                pw.println(" ActiveSubInfoList: is null");
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
            List<SubscriptionInfo> sirl2 = getAllSubInfoList(this.mContext.getOpPackageName());
            if (sirl2 != null) {
                pw.println(" AllSubInfoList:");
                for (SubscriptionInfo entry3 : sirl2) {
                    pw.println("  " + entry3.toString());
                }
            } else {
                pw.println(" AllSubInfoList: is null");
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
            this.mLocalLog.dump(fd, pw, args);
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
            pw.flush();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public SubscriptionInfo getSubscriptionInfo(int subId) {
        SubscriptionInfo si;
        if (!canReadPhoneState(this.mContext.getOpPackageName(), "getSubscriptionInfo")) {
            return null;
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            logd("[getSubscriptionInfo]- invalid subId, subId =" + subId);
            return null;
        }
        Cursor cursor = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, InboundSmsHandler.SELECT_BY_ID, new String[]{Long.toString(subId)}, null);
        try {
            if (cursor != null) {
                if (cursor.moveToFirst() && (si = getSubInfoRecord(cursor)) != null) {
                    logd("[getSubscriptionInfo]+ subId=" + subId + ", subInfo=" + si);
                    return si;
                }
            } else {
                logd("[getSubscriptionInfo]- Query fail");
            }
            if (cursor != null) {
                cursor.close();
            }
            logd("[getSubscriptionInfo]- subId=" + subId + ",subInfo=null");
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public SubscriptionInfo getSubscriptionInfoForIccId(String iccId) {
        if (!canReadPhoneState(this.mContext.getOpPackageName(), "getSubscriptionInfo")) {
            return null;
        }
        if (iccId == null) {
            logd("[getSubscriptionInfoForIccId]- null iccid");
            return null;
        }
        Cursor cursor = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, "icc_id=?", new String[]{iccId}, null);
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    SubscriptionInfo si = getSubInfoRecord(cursor);
                    if (si != null) {
                        logd("[getSubscriptionInfoForIccId]+ iccId=" + iccId + ", subInfo=" + si);
                        return si;
                    }
                }
            } else {
                logd("[getSubscriptionInfoForIccId]- Query fail");
            }
            if (cursor != null) {
                cursor.close();
            }
            logd("[getSubscriptionInfoForIccId]- iccId=" + iccId + ",subInfo=null");
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void setDefaultDataSubIdWithoutCapabilitySwitch(int subId) {
        if (subId == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultDataSubId called with DEFAULT_SUB_ID");
        }
        if (ENGDEBUG) {
            logd("[setDefaultDataSubId] subId=" + subId);
        }
        updateAllDataConnectionTrackers();
        Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_data_call", subId);
        broadcastDefaultDataSubIdChanged(subId);
    }

    public void notifySubscriptionInfoChanged(Intent intent) {
        ITelephonyRegistry tr = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
        try {
            tr.notifySubscriptionInfoChanged();
        } catch (RemoteException e) {
        }
        setReadyState(true);
        broadcastSimInfoContentChanged(intent);
    }

    public void removeStickyIntent() {
        synchronized (this.mLock) {
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

    public void setReadyState(boolean isReady) {
        if (ENGDEBUG) {
            logd("[setReadyState]- " + isReady);
        }
        this.mIsReady = isReady;
    }
}
