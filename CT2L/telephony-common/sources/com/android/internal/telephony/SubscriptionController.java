package com.android.internal.telephony;

import android.R;
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
import android.provider.Telephony;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.ISub;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.dataconnection.DctController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubscriptionController extends ISub.Stub {
    static final int MAX_LOCAL_LOG_LINES = 500;
    static final boolean VDBG = false;
    protected static PhoneProxy[] sProxyPhones;
    private int[] colorArr;
    protected Context mContext;
    protected TelephonyManager mTelephonyManager;
    static final String LOG_TAG = "SubscriptionController";
    static final boolean DBG = Log.isLoggable(LOG_TAG, 3);
    private static SubscriptionController sInstance = null;
    private static HashMap<Integer, Integer> mSlotIdxToSubId = new HashMap<>();
    private static int mDefaultFallbackSubId = -1;
    private static int mDefaultPhoneId = Integer.MAX_VALUE;
    private ScLocalLog mLocalLog = new ScLocalLog(MAX_LOCAL_LOG_LINES);
    protected final Object mLock = new Object();
    protected CallManager mCM = CallManager.getInstance();

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

    private SubscriptionController(Context c) {
        this.mContext = c;
        this.mTelephonyManager = TelephonyManager.from(this.mContext);
        if (ServiceManager.getService("isub") == null) {
            ServiceManager.addService("isub", this);
        }
        if (DBG) {
            logdl("[SubscriptionController] init by Context");
        }
    }

    private boolean isSubInfoReady() {
        return mSlotIdxToSubId.size() > 0;
    }

    private SubscriptionController(Phone phone) {
        this.mContext = phone.getContext();
        if (ServiceManager.getService("isub") == null) {
            ServiceManager.addService("isub", this);
        }
        if (DBG) {
            logdl("[SubscriptionController] init by Phone");
        }
    }

    private void enforceSubscriptionPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", "Requires READ_PHONE_STATE");
    }

    private void broadcastSimInfoContentChanged() {
        Intent intent = new Intent("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
        this.mContext.sendBroadcast(intent);
        Intent intent2 = new Intent("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        this.mContext.sendBroadcast(intent2);
    }

    private boolean checkNotifyPermission(String method) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") == 0) {
            return true;
        }
        if (DBG) {
            logd("checkNotifyPermission Permission Denial: " + method + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        }
        return false;
    }

    public void notifySubscriptionInfoChanged() {
        if (checkNotifyPermission("notifySubscriptionInfoChanged")) {
            ITelephonyRegistry tr = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
            try {
                if (DBG) {
                    logd("notifySubscriptionInfoChanged:");
                }
                tr.notifySubscriptionInfoChanged();
            } catch (RemoteException e) {
            }
            broadcastSimInfoContentChanged();
        }
    }

    private SubscriptionInfo getSubInfoRecord(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
        String iccId = cursor.getString(cursor.getColumnIndexOrThrow("icc_id"));
        int simSlotIndex = cursor.getInt(cursor.getColumnIndexOrThrow("sim_id"));
        String displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
        String carrierName = cursor.getString(cursor.getColumnIndexOrThrow("carrier_name"));
        int nameSource = cursor.getInt(cursor.getColumnIndexOrThrow("name_source"));
        int iconTint = cursor.getInt(cursor.getColumnIndexOrThrow("color"));
        String number = cursor.getString(cursor.getColumnIndexOrThrow("number"));
        int dataRoaming = cursor.getInt(cursor.getColumnIndexOrThrow("data_roaming"));
        Bitmap iconBitmap = BitmapFactory.decodeResource(this.mContext.getResources(), R.drawable.ic_faster_emergency);
        int mcc = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MCC));
        int mnc = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MNC));
        String countryIso = getSubscriptionCountryIso(id);
        if (DBG) {
            logd("[getSubInfoRecord] id:" + id + " iccid:" + iccId + " simSlotIndex:" + simSlotIndex + " displayName:" + displayName + " nameSource:" + nameSource + " iconTint:" + iconTint + " dataRoaming:" + dataRoaming + " mcc:" + mcc + " mnc:" + mnc + " countIso:" + countryIso);
        }
        String line1Number = this.mTelephonyManager.getLine1NumberForSubscriber(id);
        if (!TextUtils.isEmpty(line1Number) && !line1Number.equals(number)) {
            logd("Line1Number is different: " + line1Number);
            number = line1Number;
        }
        return new SubscriptionInfo(id, iccId, simSlotIndex, displayName, carrierName, nameSource, iconTint, number, dataRoaming, iconBitmap, mcc, mnc, countryIso);
    }

    private String getSubscriptionCountryIso(int subId) {
        int phoneId = getPhoneId(subId);
        return phoneId < 0 ? "" : this.mTelephonyManager.getSimCountryIsoForPhone(phoneId);
    }

    private List<SubscriptionInfo> getSubInfo(String selection, Object queryKey) throws Throwable {
        ArrayList<SubscriptionInfo> subList;
        if (DBG) {
            logd("selection:" + selection + " " + queryKey);
        }
        String[] selectionArgs = null;
        if (queryKey != null) {
            selectionArgs = new String[]{queryKey.toString()};
        }
        ArrayList<SubscriptionInfo> subList2 = null;
        Cursor cursor = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, selection, selectionArgs, null);
        try {
            if (cursor != null) {
                ArrayList<SubscriptionInfo> subList3 = null;
                while (cursor.moveToNext()) {
                    try {
                        SubscriptionInfo subInfo = getSubInfoRecord(cursor);
                        if (subInfo != null) {
                            subList = subList3 == null ? new ArrayList<>() : subList3;
                            subList.add(subInfo);
                        } else {
                            subList = subList3;
                        }
                        subList3 = subList;
                    } catch (Throwable th) {
                        th = th;
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                }
                subList2 = subList3;
            } else if (DBG) {
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

    private int getUnusedColor() throws Throwable {
        List<SubscriptionInfo> availableSubInfos = getActiveSubscriptionInfoList();
        this.colorArr = this.mContext.getResources().getIntArray(R.array.config_allowedGlobalInstantAppSettings);
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

    public SubscriptionInfo getActiveSubscriptionInfo(int subId) throws Throwable {
        enforceSubscriptionPermission();
        List<SubscriptionInfo> subList = getActiveSubscriptionInfoList();
        if (subList != null) {
            for (SubscriptionInfo si : subList) {
                if (si.getSubscriptionId() == subId) {
                    if (DBG) {
                        logd("[getActiveSubInfoForSubscriber]+ subId=" + subId + " subInfo=" + si);
                        return si;
                    }
                    return si;
                }
            }
        }
        if (DBG) {
            logd("[getActiveSubInfoForSubscriber]- subId=" + subId + " subList=" + subList + " subInfo=null");
        }
        return null;
    }

    public SubscriptionInfo getActiveSubscriptionInfoForIccId(String iccId) throws Throwable {
        enforceSubscriptionPermission();
        List<SubscriptionInfo> subList = getActiveSubscriptionInfoList();
        if (subList != null) {
            for (SubscriptionInfo si : subList) {
                if (si.getIccId() == iccId) {
                    if (DBG) {
                        logd("[getActiveSubInfoUsingIccId]+ iccId=" + iccId + " subInfo=" + si);
                        return si;
                    }
                    return si;
                }
            }
        }
        if (DBG) {
            logd("[getActiveSubInfoUsingIccId]+ iccId=" + iccId + " subList=" + subList + " subInfo=null");
        }
        return null;
    }

    public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int slotIdx) throws Throwable {
        enforceSubscriptionPermission();
        List<SubscriptionInfo> subList = getActiveSubscriptionInfoList();
        if (subList != null) {
            for (SubscriptionInfo si : subList) {
                if (si.getSimSlotIndex() == slotIdx) {
                    if (DBG) {
                        logd("[getActiveSubscriptionInfoForSimSlotIndex]+ slotIdx=" + slotIdx + " subId=" + si);
                        return si;
                    }
                    return si;
                }
            }
            if (DBG) {
                logd("[getActiveSubscriptionInfoForSimSlotIndex]+ slotIdx=" + slotIdx + " subId=null");
            }
        } else if (DBG) {
            logd("[getActiveSubscriptionInfoForSimSlotIndex]+ subList=null");
        }
        return null;
    }

    public List<SubscriptionInfo> getAllSubInfoList() throws Throwable {
        if (DBG) {
            logd("[getAllSubInfoList]+");
        }
        enforceSubscriptionPermission();
        List<SubscriptionInfo> subList = getSubInfo(null, null);
        if (subList != null) {
            if (DBG) {
                logd("[getAllSubInfoList]- " + subList.size() + " infos return");
            }
        } else if (DBG) {
            logd("[getAllSubInfoList]- no info return");
        }
        return subList;
    }

    public List<SubscriptionInfo> getActiveSubscriptionInfoList() throws Throwable {
        enforceSubscriptionPermission();
        if (DBG) {
            logdl("[getActiveSubInfoList]+");
        }
        List<SubscriptionInfo> subList = new ArrayList<>();
        if (!isSubInfoReady()) {
            if (DBG) {
                logdl("[getActiveSubInfoList] Sub Controller not ready");
            }
        } else {
            String validSimSelect = " AND sim_id<" + TelephonyManager.getDefault().getPhoneCount();
            List<SubscriptionInfo> subListDataBase = getSubInfo("sim_id>=0" + validSimSelect, null);
            int num = subListDataBase.size();
            for (int i = 0; i < num; i++) {
                int slotId = subListDataBase.get(i).getSimSlotIndex();
                if (mSlotIdxToSubId.containsKey(Integer.valueOf(slotId))) {
                    subList.add(subListDataBase.get(i));
                }
            }
            if (subList != null) {
                Collections.sort(subList, new Comparator<SubscriptionInfo>() {
                    @Override
                    public int compare(SubscriptionInfo arg0, SubscriptionInfo arg1) {
                        int flag = arg0.getSimSlotIndex() - arg1.getSimSlotIndex();
                        if (flag == 0) {
                            return arg0.getSubscriptionId() - arg1.getSubscriptionId();
                        }
                        return flag;
                    }
                });
                if (DBG) {
                    logdl("[getActiveSubInfoList]- " + subList.size() + " infos return");
                }
            } else if (DBG) {
                logdl("[getActiveSubInfoList]- no info return");
            }
        }
        return subList;
    }

    public int getActiveSubInfoCount() throws Throwable {
        if (DBG) {
            logd("[getActiveSubInfoCount]+");
        }
        List<SubscriptionInfo> records = getActiveSubscriptionInfoList();
        if (records == null) {
            if (DBG) {
                logd("[getActiveSubInfoCount] records null");
            }
            return 0;
        }
        if (DBG) {
            logd("[getActiveSubInfoCount]- count: " + records.size());
        }
        return records.size();
    }

    public int getAllSubInfoCount() {
        if (DBG) {
            logd("[getAllSubInfoCount]+");
        }
        enforceSubscriptionPermission();
        Cursor cursor = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            try {
                int count = cursor.getCount();
                if (DBG) {
                    logd("[getAllSubInfoCount]- " + count + " SUB(s) in DB");
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        if (DBG) {
            logd("[getAllSubInfoCount]- no SUB in DB");
        }
        return 0;
    }

    public int getActiveSubInfoCountMax() {
        return this.mTelephonyManager.getSimCount();
    }

    public int addSubInfoRecord(String iccId, int slotId) throws Throwable {
        String nameToSet;
        if (DBG) {
            logdl("[addSubInfoRecord]+ iccId:" + iccId + " slotId:" + slotId);
        }
        enforceSubscriptionPermission();
        if (iccId == null) {
            if (DBG) {
                logdl("[addSubInfoRecord]- null iccId");
            }
            return -1;
        }
        int[] subIds = getSubId(slotId);
        if (subIds == null || subIds.length == 0) {
            if (DBG) {
                logdl("[addSubInfoRecord]- getSubId failed subIds == null || length == 0 subIds=" + subIds);
            }
            return -1;
        }
        String simCarrierName = this.mTelephonyManager.getSimOperatorNameForPhone(slotId);
        if (!TextUtils.isEmpty(simCarrierName)) {
            nameToSet = simCarrierName + Integer.toString(slotId + 1);
        } else {
            nameToSet = "CARD " + Integer.toString(slotId + 1);
        }
        if (DBG) {
            logdl("[addSubInfoRecord] sim name = " + nameToSet);
        }
        if (DBG) {
            logdl("[addSubInfoRecord] carrier name = " + simCarrierName);
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        Cursor cursor = resolver.query(SubscriptionManager.CONTENT_URI, new String[]{"_id", "sim_id", "name_source"}, "icc_id=?", new String[]{iccId}, null);
        int color = getUnusedColor();
        if (cursor != null) {
            try {
                if (!cursor.moveToFirst()) {
                    ContentValues value = new ContentValues();
                    value.put("icc_id", iccId);
                    value.put("color", Integer.valueOf(color));
                    value.put("sim_id", Integer.valueOf(slotId));
                    value.put("display_name", nameToSet);
                    value.put("carrier_name", "");
                    Uri uri = resolver.insert(SubscriptionManager.CONTENT_URI, value);
                    if (DBG) {
                        logdl("[addSubInfoRecord] New record created: " + uri);
                    }
                } else {
                    int subId = cursor.getInt(0);
                    int oldSimInfoId = cursor.getInt(1);
                    int nameSource = cursor.getInt(2);
                    ContentValues value2 = new ContentValues();
                    if (slotId != oldSimInfoId) {
                        value2.put("sim_id", Integer.valueOf(slotId));
                    }
                    if (nameSource != 2) {
                        value2.put("display_name", nameToSet);
                    }
                    if (value2.size() > 0) {
                        resolver.update(SubscriptionManager.CONTENT_URI, value2, "_id=" + Long.toString(subId), null);
                    }
                    if (DBG) {
                        logdl("[addSubInfoRecord] Record already exists");
                    }
                }
            } finally {
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        cursor = resolver.query(SubscriptionManager.CONTENT_URI, null, "sim_id=?", new String[]{String.valueOf(slotId)}, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        int subId2 = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                        Integer currentSubId = mSlotIdxToSubId.get(Integer.valueOf(slotId));
                        if (currentSubId == null || !SubscriptionManager.isValidSubscriptionId(currentSubId.intValue())) {
                            mSlotIdxToSubId.put(Integer.valueOf(slotId), Integer.valueOf(subId2));
                            int subIdCountMax = getActiveSubInfoCountMax();
                            int defaultSubId = getDefaultSubId();
                            if (DBG) {
                                logdl("[addSubInfoRecord] mSlotIdxToSubId.size=" + mSlotIdxToSubId.size() + " slotId=" + slotId + " subId=" + subId2 + " defaultSubId=" + defaultSubId + " simCount=" + subIdCountMax);
                            }
                            if (!SubscriptionManager.isValidSubscriptionId(defaultSubId) || subIdCountMax == 1) {
                                setDefaultFallbackSubId(subId2);
                            }
                            if (subIdCountMax == 1) {
                                if (DBG) {
                                    logdl("[addSubInfoRecord] one sim set defaults to subId=" + subId2);
                                }
                                setDefaultDataSubId(subId2);
                                setDefaultSmsSubId(subId2);
                                setDefaultVoiceSubId(subId2);
                            }
                        } else if (DBG) {
                            logdl("[addSubInfoRecord] currentSubId != null && currentSubId is valid, IGNORE");
                        }
                        if (DBG) {
                            logdl("[addSubInfoRecord] hashmap(" + slotId + "," + subId2 + ")");
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        updateAllDataConnectionTrackers();
        if (DBG) {
            logdl("[addSubInfoRecord]- info size=" + mSlotIdxToSubId.size());
        }
        return 0;
    }

    public boolean setPlmnSpn(int slotId, boolean showPlmn, String plmn, boolean showSpn, String spn) {
        boolean z = false;
        synchronized (this.mLock) {
            int[] subIds = getSubId(slotId);
            if (this.mContext.getPackageManager().resolveContentProvider(SubscriptionManager.CONTENT_URI.getAuthority(), 0) != null && subIds != null && SubscriptionManager.isValidSubscriptionId(subIds[0])) {
                String carrierText = "";
                if (showPlmn) {
                    carrierText = plmn;
                    if (showSpn) {
                        String separator = this.mContext.getString(R.string.mediasize_iso_c0).toString();
                        carrierText = carrierText + separator + spn;
                    }
                } else if (showSpn) {
                    carrierText = spn;
                }
                for (int i : subIds) {
                    setCarrierText(carrierText, i);
                }
                z = true;
            }
        }
        return z;
    }

    private int setCarrierText(String text, int subId) {
        if (DBG) {
            logd("[setCarrierText]+ text:" + text + " subId:" + subId);
        }
        enforceSubscriptionPermission();
        ContentValues value = new ContentValues(1);
        value.put("carrier_name", text);
        int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
        notifySubscriptionInfoChanged();
        return result;
    }

    public int setIconTint(int tint, int subId) {
        if (DBG) {
            logd("[setIconTint]+ tint:" + tint + " subId:" + subId);
        }
        enforceSubscriptionPermission();
        validateSubId(subId);
        ContentValues value = new ContentValues(1);
        value.put("color", Integer.valueOf(tint));
        if (DBG) {
            logd("[setIconTint]- tint:" + tint + " set");
        }
        int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
        notifySubscriptionInfoChanged();
        return result;
    }

    public int setDisplayName(String displayName, int subId) {
        return setDisplayNameUsingSrc(displayName, subId, -1L);
    }

    public int setDisplayNameUsingSrc(String displayName, int subId, long nameSource) {
        String nameToSet;
        if (DBG) {
            logd("[setDisplayName]+  displayName:" + displayName + " subId:" + subId + " nameSource:" + nameSource);
        }
        enforceSubscriptionPermission();
        validateSubId(subId);
        if (displayName == null) {
            nameToSet = this.mContext.getString(R.string.unknownName);
        } else {
            nameToSet = displayName;
        }
        ContentValues value = new ContentValues(1);
        value.put("display_name", nameToSet);
        if (nameSource >= 0) {
            if (DBG) {
                logd("Set nameSource=" + nameSource);
            }
            value.put("name_source", Long.valueOf(nameSource));
        }
        if (DBG) {
            logd("[setDisplayName]- mDisplayName:" + nameToSet + " set");
        }
        int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
        notifySubscriptionInfoChanged();
        return result;
    }

    public int setDisplayNumber(String number, int subId) {
        if (DBG) {
            logd("[setDisplayNumber]+ subId:" + subId);
        }
        enforceSubscriptionPermission();
        validateSubId(subId);
        int phoneId = getPhoneId(subId);
        if (number == null || phoneId < 0 || phoneId >= this.mTelephonyManager.getPhoneCount()) {
            if (DBG) {
                logd("[setDispalyNumber]- fail");
            }
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put("number", number);
        int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
        if (DBG) {
            logd("[setDisplayNumber]- update result :" + result);
        }
        notifySubscriptionInfoChanged();
        return result;
    }

    public int setDataRoaming(int roaming, int subId) {
        if (DBG) {
            logd("[setDataRoaming]+ roaming:" + roaming + " subId:" + subId);
        }
        enforceSubscriptionPermission();
        validateSubId(subId);
        if (roaming < 0) {
            if (DBG) {
                logd("[setDataRoaming]- fail");
            }
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put("data_roaming", Integer.valueOf(roaming));
        if (DBG) {
            logd("[setDataRoaming]- roaming:" + roaming + " set");
        }
        int iUpdate = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
        notifySubscriptionInfoChanged();
        return iUpdate;
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
        if (DBG) {
            logd("[setMccMnc]+ mcc/mnc:" + mcc + "/" + mnc + " subId:" + subId);
        }
        ContentValues value = new ContentValues(2);
        value.put(Telephony.Carriers.MCC, Integer.valueOf(mcc));
        value.put(Telephony.Carriers.MNC, Integer.valueOf(mnc));
        int result = this.mContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value, "_id=" + Long.toString(subId), null);
        notifySubscriptionInfoChanged();
        return result;
    }

    public int getSlotId(int subId) {
        if (subId == Integer.MAX_VALUE) {
            subId = getDefaultSubId();
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            if (DBG) {
                logd("[getSlotId]- subId invalid");
            }
            return -1;
        }
        int size = mSlotIdxToSubId.size();
        if (size == 0) {
            if (DBG) {
                logd("[getSlotId]- size == 0, return SIM_NOT_INSERTED instead");
            }
            return -1;
        }
        for (Map.Entry<Integer, Integer> entry : mSlotIdxToSubId.entrySet()) {
            int iIntValue = entry.getKey().intValue();
            int sub = entry.getValue().intValue();
            if (subId == sub) {
                return iIntValue;
            }
        }
        if (DBG) {
            logd("[getSlotId]- return fail");
        }
        return -1;
    }

    @Deprecated
    public int[] getSubId(int slotIdx) {
        if (slotIdx == Integer.MAX_VALUE) {
            slotIdx = getSlotId(getDefaultSubId());
            if (DBG) {
                logd("[getSubId] map default slotIdx=" + slotIdx);
            }
        }
        if (!SubscriptionManager.isValidSlotId(slotIdx)) {
            if (DBG) {
                logd("[getSubId]- invalid slotIdx=" + slotIdx);
            }
            return null;
        }
        int size = mSlotIdxToSubId.size();
        if (size == 0) {
            if (DBG) {
                logd("[getSubId]- mSlotIdToSubIdMap.size == 0, return DummySubIds slotIdx=" + slotIdx);
            }
            return getDummySubIds(slotIdx);
        }
        ArrayList<Integer> subIds = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : mSlotIdxToSubId.entrySet()) {
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
        if (DBG) {
            logd("[getSubId]- numSubIds == 0, return DummySubIds slotIdx=" + slotIdx);
        }
        return getDummySubIds(slotIdx);
    }

    public int getPhoneId(int subId) {
        if (subId == Integer.MAX_VALUE) {
            subId = getDefaultSubId();
            if (DBG) {
                logdl("[getPhoneId] asked for default subId=" + subId);
            }
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            if (DBG) {
                logdl("[getPhoneId]- invalid subId return=-1");
            }
            return -1;
        }
        int size = mSlotIdxToSubId.size();
        if (size == 0) {
            int phoneId = mDefaultPhoneId;
            if (DBG) {
                logdl("[getPhoneId]- no sims, returning default phoneId=" + phoneId);
                return phoneId;
            }
            return phoneId;
        }
        for (Map.Entry<Integer, Integer> entry : mSlotIdxToSubId.entrySet()) {
            int sim = entry.getKey().intValue();
            int sub = entry.getValue().intValue();
            if (subId == sub) {
                return sim;
            }
        }
        int phoneId2 = mDefaultPhoneId;
        if (DBG) {
            logdl("[getPhoneId]- subId=" + subId + " not found return default phoneId=" + phoneId2);
            return phoneId2;
        }
        return phoneId2;
    }

    private int[] getDummySubIds(int slotIdx) {
        int numSubs = getActiveSubInfoCountMax();
        if (numSubs <= 0) {
            return null;
        }
        int[] dummyValues = new int[numSubs];
        for (int i = 0; i < numSubs; i++) {
            dummyValues[i] = (-2) - slotIdx;
        }
        if (DBG) {
            logd("getDummySubIds: slotIdx=" + slotIdx + " return " + numSubs + " DummySubIds with each subId=" + dummyValues[0]);
            return dummyValues;
        }
        return dummyValues;
    }

    public int clearSubInfo() {
        enforceSubscriptionPermission();
        if (DBG) {
            logd("[clearSubInfo]+");
        }
        int size = mSlotIdxToSubId.size();
        if (size == 0) {
            if (DBG) {
                logdl("[clearSubInfo]- no simInfo size=" + size);
            }
            return 0;
        }
        mSlotIdxToSubId.clear();
        if (DBG) {
            logdl("[clearSubInfo]- clear size=" + size);
            return size;
        }
        return size;
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
        boolean isVoiceCapable = this.mContext.getResources().getBoolean(R.^attr-private.externalRouteEnabledDrawable);
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
        if (subId == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultSmsSubId called with DEFAULT_SUB_ID");
        }
        if (DBG) {
            logdl("[setDefaultSmsSubId] subId=" + subId);
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_sms", subId);
        broadcastDefaultSmsSubIdChanged(subId);
    }

    private void broadcastDefaultSmsSubIdChanged(int subId) {
        if (DBG) {
            logdl("[broadcastDefaultSmsSubIdChanged] subId=" + subId);
        }
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
        if (subId == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultVoiceSubId called with DEFAULT_SUB_ID");
        }
        if (DBG) {
            logdl("[setDefaultVoiceSubId] subId=" + subId);
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_voice_call", subId);
        broadcastDefaultVoiceSubIdChanged(subId);
    }

    private void broadcastDefaultVoiceSubIdChanged(int subId) {
        if (DBG) {
            logdl("[broadcastDefaultVoiceSubIdChanged] subId=" + subId);
        }
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
        int raf;
        if (subId == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultDataSubId called with DEFAULT_SUB_ID");
        }
        if (DBG) {
            logdl("[setDefaultDataSubId] subId=" + subId);
        }
        int len = sProxyPhones.length;
        logdl("[setDefaultDataSubId] num phones=" + len);
        RadioAccessFamily[] rafs = new RadioAccessFamily[len];
        for (int phoneId = 0; phoneId < len; phoneId++) {
            PhoneProxy phone = sProxyPhones[phoneId];
            int raf2 = phone.getRadioAccessFamily();
            int id = phone.getSubId();
            logdl("[setDefaultDataSubId] phoneId=" + phoneId + " subId=" + id + " RAF=" + raf2);
            int raf3 = raf2 | 65536;
            if (id == subId) {
                raf = raf3 | 8;
            } else {
                raf = raf3 & (-9);
            }
            logdl("[setDefaultDataSubId] reqRAF=" + raf);
            int networkType = PhoneFactory.calculatePreferredNetworkType(this.mContext, id);
            logdl("[setDefaultDataSubId] networkType=" + networkType);
            int raf4 = raf & RadioAccessFamily.getRafFromNetworkType(networkType);
            logdl("[setDefaultDataSubId] newRAF=" + raf4);
            rafs[phoneId] = new RadioAccessFamily(phoneId, raf4);
        }
        ProxyController.getInstance().setRadioCapability(rafs);
        updateAllDataConnectionTrackers();
        Settings.Global.putInt(this.mContext.getContentResolver(), "multi_sim_data_call", subId);
        broadcastDefaultDataSubIdChanged(subId);
        int subIdCountMax = getActiveSubInfoCountMax();
        if (subIdCountMax > 1) {
            int phoneId2 = getPhoneId(subId);
            if (!SubscriptionManager.isValidPhoneId(phoneId2)) {
                logd("[setDefaultDataSubId] - invalid phoneId=" + phoneId2);
                return;
            }
            Phone phone2 = PhoneFactory.getPhone(phoneId2);
            if (phone2 != null) {
                phone2.setDataEnabled(true);
                Dsds.MasterUiccPref masterPref = Dsds.getMasterUiccPref(this.mContext, getPhoneId(subId));
                if (masterPref != Dsds.MasterUiccPref.MASTER_UICC_RESTRICTED) {
                    phone2.setPreferredNetworkType(Phone.PREFERRED_NT_MODE, null);
                    PhoneFactory.saveNetworkMode(this.mContext, phoneId2, Phone.PREFERRED_NT_MODE);
                }
            }
        }
    }

    private DctController getDctController() {
        DctController dctc = DctController.makeDctController((PhoneProxy[]) PhoneFactory.getPhones());
        return dctc;
    }

    public int getDataActivePhoneId() {
        return getDctController().getDataActivePhoneId();
    }

    private void updateAllDataConnectionTrackers() {
        int len = sProxyPhones.length;
        if (DBG) {
            logdl("[updateAllDataConnectionTrackers] sProxyPhones.length=" + len);
        }
        for (int phoneId = 0; phoneId < len; phoneId++) {
            if (DBG) {
                logdl("[updateAllDataConnectionTrackers] phoneId=" + phoneId);
            }
            sProxyPhones[phoneId].updateDataConnectionTracker();
        }
    }

    private void broadcastDefaultDataSubIdChanged(int subId) {
        if (DBG) {
            logdl("[broadcastDefaultDataSubIdChanged] subId=" + subId);
        }
        Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intent.addFlags(536870912);
        intent.putExtra("subscription", subId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void setDefaultFallbackSubId(int subId) {
        if (subId == Integer.MAX_VALUE) {
            throw new RuntimeException("setDefaultSubId called with DEFAULT_SUB_ID");
        }
        if (DBG) {
            logdl("[setDefaultFallbackSubId] subId=" + subId);
        }
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            int phoneId = getPhoneId(subId);
            if (phoneId >= 0 && (phoneId < this.mTelephonyManager.getPhoneCount() || this.mTelephonyManager.getSimCount() == 1)) {
                if (DBG) {
                    logdl("[setDefaultFallbackSubId] set mDefaultFallbackSubId=" + subId);
                }
                mDefaultFallbackSubId = subId;
                String defaultMccMnc = this.mTelephonyManager.getSimOperatorNumericForPhone(phoneId);
                MccTable.updateMccMncConfiguration(this.mContext, defaultMccMnc, false);
                Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_SUBSCRIPTION_CHANGED");
                intent.addFlags(536870912);
                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, phoneId, subId);
                if (DBG) {
                    logdl("[setDefaultFallbackSubId] broadcast default subId changed phoneId=" + phoneId + " subId=" + subId);
                }
                this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                return;
            }
            if (DBG) {
                logdl("[setDefaultFallbackSubId] not set invalid phoneId=" + phoneId + " subId=" + subId);
            }
        }
    }

    public void clearDefaultsForInactiveSubIds() throws Throwable {
        List<SubscriptionInfo> records = getActiveSubscriptionInfoList();
        if (DBG) {
            logdl("[clearDefaultsForInactiveSubIds] records: " + records);
        }
        if (shouldDefaultBeCleared(records, getDefaultDataSubId())) {
            if (DBG) {
                logd("[clearDefaultsForInactiveSubIds] clearing default data sub id");
            }
            setDefaultDataSubId(-1);
        }
        if (shouldDefaultBeCleared(records, getDefaultSmsSubId())) {
            if (DBG) {
                logdl("[clearDefaultsForInactiveSubIds] clearing default sms sub id");
            }
            setDefaultSmsSubId(-1);
        }
        if (shouldDefaultBeCleared(records, getDefaultVoiceSubId())) {
            if (DBG) {
                logdl("[clearDefaultsForInactiveSubIds] clearing default voice sub id");
            }
            setDefaultVoiceSubId(-1);
        }
    }

    private boolean shouldDefaultBeCleared(List<SubscriptionInfo> records, int subId) {
        if (DBG) {
            logdl("[shouldDefaultBeCleared: subId] " + subId);
        }
        if (records == null) {
            if (!DBG) {
                return true;
            }
            logdl("[shouldDefaultBeCleared] return true no records subId=" + subId);
            return true;
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            if (DBG) {
                logdl("[shouldDefaultBeCleared] return false only one subId, subId=" + subId);
            }
            return false;
        }
        for (SubscriptionInfo record : records) {
            int id = record.getSubscriptionId();
            if (DBG) {
                logdl("[shouldDefaultBeCleared] Record.id: " + id);
            }
            if (id == subId) {
                logdl("[shouldDefaultBeCleared] return false subId is active, subId=" + subId);
                return false;
            }
        }
        if (!DBG) {
            return true;
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

    public List<SubscriptionInfo> getSubInfoUsingSlotIdWithCheck(int slotId, boolean needCheck) throws Throwable {
        ArrayList<SubscriptionInfo> subList;
        if (DBG) {
            logd("[getSubInfoUsingSlotIdWithCheck]+ slotId:" + slotId);
        }
        enforceSubscriptionPermission();
        if (slotId == Integer.MAX_VALUE) {
            slotId = getSlotId(getDefaultSubId());
        }
        if (!SubscriptionManager.isValidSlotId(slotId)) {
            if (!DBG) {
                return null;
            }
            logd("[getSubInfoUsingSlotIdWithCheck]- invalid slotId");
            return null;
        }
        if (needCheck && !isSubInfoReady()) {
            if (!DBG) {
                return null;
            }
            logd("[getSubInfoUsingSlotIdWithCheck]- not ready");
            return null;
        }
        Cursor cursor = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, "sim_id=?", new String[]{String.valueOf(slotId)}, null);
        ArrayList<SubscriptionInfo> subList2 = null;
        if (cursor != null) {
            while (true) {
                try {
                    subList = subList2;
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
        if (DBG) {
            logd("[getSubInfoUsingSlotId]- null info return");
        }
        return subList2;
    }

    private void validateSubId(int subId) {
        if (DBG) {
            logd("validateSubId subId: " + subId);
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            throw new RuntimeException("Invalid sub id passed as parameter");
        }
        if (subId == Integer.MAX_VALUE) {
            throw new RuntimeException("Default sub id passed as parameter");
        }
    }

    public void updatePhonesAvailability(PhoneProxy[] phones) {
        sProxyPhones = phones;
    }

    public int[] getActiveSubIdList() {
        Set<Map.Entry<Integer, Integer>> simInfoSet = mSlotIdxToSubId.entrySet();
        if (DBG) {
            logdl("[getActiveSubIdList] simInfoSet=" + simInfoSet);
        }
        int[] subIdArr = new int[simInfoSet.size()];
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : simInfoSet) {
            int sub = entry.getValue().intValue();
            subIdArr[i] = sub;
            i++;
        }
        if (DBG) {
            logdl("[getActiveSubIdList] X subIdArr.length=" + subIdArr.length);
        }
        return subIdArr;
    }

    private boolean isActiveSubId(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        Set<Map.Entry<Integer, Integer>> simInfoSet = mSlotIdxToSubId.entrySet();
        for (Map.Entry<Integer, Integer> entry : simInfoSet) {
            if (subId == entry.getValue().intValue()) {
                return true;
            }
        }
        return false;
    }

    public int getSimStateForSubscriber(int subId) {
        IccCardConstants.State simState;
        String err;
        int phoneIdx = getPhoneId(subId);
        if (phoneIdx < 0) {
            simState = IccCardConstants.State.UNKNOWN;
            err = "invalid PhoneIdx";
        } else {
            Phone phone = PhoneFactory.getPhone(phoneIdx);
            if (phone == null) {
                simState = IccCardConstants.State.UNKNOWN;
                err = "phone == null";
            } else {
                IccCard icc = phone.getIccCard();
                if (icc == null) {
                    simState = IccCardConstants.State.UNKNOWN;
                    err = "icc == null";
                } else {
                    simState = icc.getState();
                    err = "";
                }
            }
        }
        if (DBG) {
            logd("getSimStateForSubscriber: " + err + " simState=" + simState + " ordinal=" + simState.ordinal());
        }
        return simState.ordinal();
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
            for (Map.Entry<Integer, Integer> entry : mSlotIdxToSubId.entrySet()) {
                pw.println(" mSlotIdToSubIdMap[" + entry.getKey() + "]: subId=" + entry.getValue());
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
            List<SubscriptionInfo> sirl = getActiveSubscriptionInfoList();
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
            List<SubscriptionInfo> sirl2 = getAllSubInfoList();
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
}
