package com.android.internal.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CarrierIdentifier extends Handler {
    private static final int CARRIER_ID_DB_UPDATE_EVENT = 6;
    private static final boolean DBG = true;
    private static final int ICC_CHANGED_EVENT = 4;
    private static final String OPERATOR_BRAND_OVERRIDE_PREFIX = "operator_branding_";
    private static final int PREFER_APN_UPDATE_EVENT = 5;
    private static final int SIM_ABSENT_EVENT = 2;
    private static final int SIM_LOAD_EVENT = 1;
    private static final int SPN_OVERRIDE_EVENT = 3;
    private String mCarrierName;
    private Context mContext;
    private IccRecords mIccRecords;
    private Phone mPhone;
    private String mPreferApn;
    private final TelephonyManager mTelephonyMgr;
    private UiccProfile mUiccProfile;
    private static final String LOG_TAG = CarrierIdentifier.class.getSimpleName();
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, 2);
    private static final Uri CONTENT_URL_PREFER_APN = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "preferapn");
    private List<CarrierMatchingRule> mCarrierMatchingRulesOnMccMnc = new ArrayList();
    private int mCarrierId = -1;
    private String mSpn = "";
    private final LocalLog mCarrierIdLocalLog = new LocalLog(20);
    private final SubscriptionsChangedListener mOnSubscriptionsChangedListener = new SubscriptionsChangedListener();
    private final ContentObserver mContentObserver = new ContentObserver(this) {
        @Override
        public void onChange(boolean z, Uri uri) {
            if (CarrierIdentifier.CONTENT_URL_PREFER_APN.equals(uri.getLastPathSegment())) {
                CarrierIdentifier.logd("onChange URI: " + uri);
                CarrierIdentifier.this.sendEmptyMessage(5);
                return;
            }
            if (Telephony.CarrierId.All.CONTENT_URI.equals(uri)) {
                CarrierIdentifier.logd("onChange URI: " + uri);
                CarrierIdentifier.this.sendEmptyMessage(6);
            }
        }
    };

    private class SubscriptionsChangedListener extends SubscriptionManager.OnSubscriptionsChangedListener {
        final AtomicInteger mPreviousSubId;

        private SubscriptionsChangedListener() {
            this.mPreviousSubId = new AtomicInteger(-1);
        }

        @Override
        public void onSubscriptionsChanged() {
            int subId = CarrierIdentifier.this.mPhone.getSubId();
            if (this.mPreviousSubId.getAndSet(subId) != subId) {
                CarrierIdentifier.logd("SubscriptionListener.onSubscriptionInfoChanged subId: " + this.mPreviousSubId);
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    CarrierIdentifier.this.sendEmptyMessage(1);
                } else {
                    CarrierIdentifier.this.sendEmptyMessage(2);
                }
            }
        }
    }

    public CarrierIdentifier(Phone phone) {
        logd("Creating CarrierIdentifier[" + phone.getPhoneId() + "]");
        this.mContext = phone.getContext();
        this.mPhone = phone;
        this.mTelephonyMgr = TelephonyManager.from(this.mContext);
        this.mContext.getContentResolver().registerContentObserver(CONTENT_URL_PREFER_APN, false, this.mContentObserver);
        this.mContext.getContentResolver().registerContentObserver(Telephony.CarrierId.All.CONTENT_URI, false, this.mContentObserver);
        SubscriptionManager.from(this.mContext).addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        UiccController.getInstance().registerForIccChanged(this, 4, null);
    }

    @Override
    public void handleMessage(Message message) {
        if (VDBG) {
            logd("handleMessage: " + message.what);
        }
        switch (message.what) {
            case 1:
            case 6:
                this.mSpn = this.mTelephonyMgr.getSimOperatorNameForPhone(this.mPhone.getPhoneId());
                this.mPreferApn = getPreferApn();
                loadCarrierMatchingRulesOnMccMnc();
                break;
            case 2:
                this.mCarrierMatchingRulesOnMccMnc.clear();
                this.mSpn = null;
                this.mPreferApn = null;
                updateCarrierIdAndName(-1, null);
                break;
            case 3:
                String simOperatorNameForPhone = this.mTelephonyMgr.getSimOperatorNameForPhone(this.mPhone.getPhoneId());
                if (!equals(this.mSpn, simOperatorNameForPhone, true)) {
                    logd("[updateSpn] from:" + this.mSpn + " to:" + simOperatorNameForPhone);
                    this.mSpn = simOperatorNameForPhone;
                    matchCarrier();
                }
                break;
            case 4:
                IccRecords iccRecords = UiccController.getInstance().getIccRecords(this.mPhone.getPhoneId(), 1);
                if (this.mIccRecords != iccRecords) {
                    if (this.mIccRecords != null) {
                        logd("Removing stale icc objects.");
                        this.mIccRecords.unregisterForRecordsLoaded(this);
                        this.mIccRecords.unregisterForRecordsOverride(this);
                        this.mIccRecords = null;
                    }
                    if (iccRecords != null) {
                        logd("new Icc object");
                        iccRecords.registerForRecordsLoaded(this, 1, null);
                        iccRecords.registerForRecordsOverride(this, 1, null);
                        this.mIccRecords = iccRecords;
                    }
                }
                UiccProfile uiccProfileForPhone = UiccController.getInstance().getUiccProfileForPhone(this.mPhone.getPhoneId());
                if (this.mUiccProfile != uiccProfileForPhone) {
                    if (this.mUiccProfile != null) {
                        logd("unregister operatorBrandOverride");
                        this.mUiccProfile.unregisterForOperatorBrandOverride(this);
                        this.mUiccProfile = null;
                    }
                    if (uiccProfileForPhone != null) {
                        logd("register operatorBrandOverride");
                        uiccProfileForPhone.registerForOpertorBrandOverride(this, 3, null);
                        this.mUiccProfile = uiccProfileForPhone;
                    }
                }
                break;
            case 5:
                String preferApn = getPreferApn();
                if (!equals(this.mPreferApn, preferApn, true)) {
                    logd("[updatePreferApn] from:" + this.mPreferApn + " to:" + preferApn);
                    this.mPreferApn = preferApn;
                    matchCarrier();
                }
                break;
            default:
                loge("invalid msg: " + message.what);
                break;
        }
    }

    private void loadCarrierMatchingRulesOnMccMnc() {
        try {
            String simOperatorNumericForPhone = this.mTelephonyMgr.getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
            Cursor cursorQuery = this.mContext.getContentResolver().query(Telephony.CarrierId.All.CONTENT_URI, null, "mccmnc=?", new String[]{simOperatorNumericForPhone}, null);
            if (cursorQuery != null) {
                try {
                    if (VDBG) {
                        logd("[loadCarrierMatchingRules]- " + cursorQuery.getCount() + " Records(s) in DB mccmnc: " + simOperatorNumericForPhone);
                    }
                    this.mCarrierMatchingRulesOnMccMnc.clear();
                    while (cursorQuery.moveToNext()) {
                        this.mCarrierMatchingRulesOnMccMnc.add(makeCarrierMatchingRule(cursorQuery));
                    }
                    matchCarrier();
                } finally {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
        } catch (Exception e) {
            loge("[loadCarrierMatchingRules]- ex: " + e);
        }
    }

    private String getPreferApn() {
        Cursor cursorQuery = this.mContext.getContentResolver().query(Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "preferapn/subId/" + this.mPhone.getSubId()), new String[]{"apn"}, null, null, null);
        if (cursorQuery != null) {
            try {
                try {
                    if (VDBG) {
                        logd("[getPreferApn]- " + cursorQuery.getCount() + " Records(s) in DB");
                    }
                    if (cursorQuery.moveToNext()) {
                        String string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("apn"));
                        logd("[getPreferApn]- " + string);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return string;
                    }
                    if (cursorQuery == null) {
                        return null;
                    }
                } catch (Exception e) {
                    loge("[getPreferApn]- exception: " + e);
                    if (cursorQuery == null) {
                        return null;
                    }
                }
            } catch (Throwable th) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                throw th;
            }
        } else if (cursorQuery == null) {
        }
        cursorQuery.close();
        return null;
    }

    private void updateCarrierIdAndName(int i, String str) {
        boolean z;
        if (equals(str, this.mCarrierName, true)) {
            z = false;
        } else {
            logd("[updateCarrierName] from:" + this.mCarrierName + " to:" + str);
            this.mCarrierName = str;
            z = true;
        }
        if (i != this.mCarrierId) {
            logd("[updateCarrierId] from:" + this.mCarrierId + " to:" + i);
            this.mCarrierId = i;
            z = true;
        }
        if (z) {
            this.mCarrierIdLocalLog.log("[updateCarrierIdAndName] cid:" + this.mCarrierId + " name:" + this.mCarrierName);
            Intent intent = new Intent("android.telephony.action.SUBSCRIPTION_CARRIER_IDENTITY_CHANGED");
            intent.putExtra("android.telephony.extra.CARRIER_ID", this.mCarrierId);
            intent.putExtra("android.telephony.extra.CARRIER_NAME", this.mCarrierName);
            intent.putExtra("android.telephony.extra.SUBSCRIPTION_ID", this.mPhone.getSubId());
            this.mContext.sendBroadcast(intent);
            ContentValues contentValues = new ContentValues();
            contentValues.put("carrier_id", Integer.valueOf(this.mCarrierId));
            contentValues.put("carrier_name", this.mCarrierName);
            this.mContext.getContentResolver().update(Uri.withAppendedPath(Telephony.CarrierId.CONTENT_URI, Integer.toString(this.mPhone.getSubId())), contentValues, null, null);
        }
    }

    private CarrierMatchingRule makeCarrierMatchingRule(Cursor cursor) {
        return new CarrierMatchingRule(cursor.getString(cursor.getColumnIndexOrThrow("mccmnc")), cursor.getString(cursor.getColumnIndexOrThrow("imsi_prefix_xpattern")), cursor.getString(cursor.getColumnIndexOrThrow("iccid_prefix")), cursor.getString(cursor.getColumnIndexOrThrow("gid1")), cursor.getString(cursor.getColumnIndexOrThrow("gid2")), cursor.getString(cursor.getColumnIndexOrThrow("plmn")), cursor.getString(cursor.getColumnIndexOrThrow("spn")), cursor.getString(cursor.getColumnIndexOrThrow("apn")), cursor.getInt(cursor.getColumnIndexOrThrow("carrier_id")), cursor.getString(cursor.getColumnIndexOrThrow("carrier_name")));
    }

    private static class CarrierMatchingRule {
        private static final int SCORE_APN = 1;
        private static final int SCORE_GID1 = 16;
        private static final int SCORE_GID2 = 8;
        private static final int SCORE_ICCID_PREFIX = 32;
        private static final int SCORE_IMSI_PREFIX = 64;
        private static final int SCORE_INVALID = -1;
        private static final int SCORE_MCCMNC = 128;
        private static final int SCORE_PLMN = 4;
        private static final int SCORE_SPN = 2;
        private String mApn;
        private int mCid;
        private String mGid1;
        private String mGid2;
        private String mIccidPrefix;
        private String mImsiPrefixPattern;
        private String mMccMnc;
        private String mName;
        private String mPlmn;
        private int mScore = 0;
        private String mSpn;

        CarrierMatchingRule(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, int i, String str9) {
            this.mMccMnc = str;
            this.mImsiPrefixPattern = str2;
            this.mIccidPrefix = str3;
            this.mGid1 = str4;
            this.mGid2 = str5;
            this.mPlmn = str6;
            this.mSpn = str7;
            this.mApn = str8;
            this.mCid = i;
            this.mName = str9;
        }

        public void match(CarrierMatchingRule carrierMatchingRule) {
            this.mScore = 0;
            if (this.mMccMnc != null) {
                if (!CarrierIdentifier.equals(carrierMatchingRule.mMccMnc, this.mMccMnc, false)) {
                    this.mScore = -1;
                    return;
                }
                this.mScore += 128;
            }
            if (this.mImsiPrefixPattern != null) {
                if (!imsiPrefixMatch(carrierMatchingRule.mImsiPrefixPattern, this.mImsiPrefixPattern)) {
                    this.mScore = -1;
                    return;
                }
                this.mScore += 64;
            }
            if (this.mIccidPrefix != null) {
                if (!iccidPrefixMatch(carrierMatchingRule.mIccidPrefix, this.mIccidPrefix)) {
                    this.mScore = -1;
                    return;
                }
                this.mScore += 32;
            }
            if (this.mGid1 != null) {
                if (!CarrierIdentifier.equals(carrierMatchingRule.mGid1, this.mGid1, true)) {
                    this.mScore = -1;
                    return;
                }
                this.mScore += 16;
            }
            if (this.mGid2 != null) {
                if (!CarrierIdentifier.equals(carrierMatchingRule.mGid2, this.mGid2, true)) {
                    this.mScore = -1;
                    return;
                }
                this.mScore += 8;
            }
            if (this.mPlmn != null) {
                if (!CarrierIdentifier.equals(carrierMatchingRule.mPlmn, this.mPlmn, true)) {
                    this.mScore = -1;
                    return;
                }
                this.mScore += 4;
            }
            if (this.mSpn != null) {
                if (!CarrierIdentifier.equals(carrierMatchingRule.mSpn, this.mSpn, true)) {
                    this.mScore = -1;
                    return;
                }
                this.mScore += 2;
            }
            if (this.mApn != null) {
                if (!CarrierIdentifier.equals(carrierMatchingRule.mApn, this.mApn, true)) {
                    this.mScore = -1;
                } else {
                    this.mScore++;
                }
            }
        }

        private boolean imsiPrefixMatch(String str, String str2) {
            if (TextUtils.isEmpty(str2)) {
                return true;
            }
            if (TextUtils.isEmpty(str) || str.length() < str2.length()) {
                return false;
            }
            for (int i = 0; i < str2.length(); i++) {
                if (str2.charAt(i) != 'x' && str2.charAt(i) != 'X' && str2.charAt(i) != str.charAt(i)) {
                    return false;
                }
            }
            return true;
        }

        private boolean iccidPrefixMatch(String str, String str2) {
            if (str == null || str2 == null) {
                return false;
            }
            return str.startsWith(str2);
        }

        public String toString() {
            return "[CarrierMatchingRule] - mccmnc: " + this.mMccMnc + " gid1: " + this.mGid1 + " gid2: " + this.mGid2 + " plmn: " + this.mPlmn + " imsi_prefix: " + this.mImsiPrefixPattern + " iccid_prefix" + this.mIccidPrefix + " spn: " + this.mSpn + " apn: " + this.mApn + " name: " + this.mName + " cid: " + this.mCid + " score: " + this.mScore;
        }
    }

    private void matchCarrier() {
        String str;
        if (!SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
            logd("[matchCarrier]skip before sim records loaded");
            return;
        }
        String simOperatorNumericForPhone = this.mTelephonyMgr.getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
        String iccSerialNumber = this.mPhone.getIccSerialNumber();
        String groupIdLevel1 = this.mPhone.getGroupIdLevel1();
        String groupIdLevel2 = this.mPhone.getGroupIdLevel2();
        String subscriberId = this.mPhone.getSubscriberId();
        String plmn = this.mPhone.getPlmn();
        String str2 = this.mSpn;
        String str3 = this.mPreferApn;
        if (VDBG) {
            logd("[matchCarrier] mnnmnc:" + simOperatorNumericForPhone + " gid1: " + groupIdLevel1 + " gid2: " + groupIdLevel2 + " imsi: " + Rlog.pii(LOG_TAG, subscriberId) + " iccid: " + Rlog.pii(LOG_TAG, iccSerialNumber) + " plmn: " + plmn + " spn: " + str2 + " apn: " + str3);
        }
        CarrierMatchingRule carrierMatchingRule = new CarrierMatchingRule(simOperatorNumericForPhone, subscriberId, iccSerialNumber, groupIdLevel1, groupIdLevel2, plmn, str2, str3, -1, null);
        String str4 = null;
        int i = -1;
        CarrierMatchingRule carrierMatchingRule2 = null;
        for (CarrierMatchingRule carrierMatchingRule3 : this.mCarrierMatchingRulesOnMccMnc) {
            carrierMatchingRule3.match(carrierMatchingRule);
            if (carrierMatchingRule3.mScore > i) {
                i = carrierMatchingRule3.mScore;
                carrierMatchingRule2 = carrierMatchingRule3;
            }
        }
        if (i != -1) {
            logd("[matchCarrier] cid: " + carrierMatchingRule2.mCid + " name: " + carrierMatchingRule2.mName);
            updateCarrierIdAndName(carrierMatchingRule2.mCid, carrierMatchingRule2.mName);
        } else {
            logd("[matchCarrier - no match] cid: -1 name: " + ((Object) null));
            updateCarrierIdAndName(-1, null);
        }
        int i2 = i & 16;
        if (i2 == 0 && !TextUtils.isEmpty(carrierMatchingRule.mGid1)) {
            str = carrierMatchingRule.mGid1;
        } else {
            str = null;
        }
        if ((i == -1 || i2 == 0) && !TextUtils.isEmpty(carrierMatchingRule.mMccMnc)) {
            str4 = carrierMatchingRule.mMccMnc;
        }
        TelephonyMetrics.getInstance().writeCarrierIdMatchingEvent(this.mPhone.getPhoneId(), getCarrierListVersion(), this.mCarrierId, str4, str);
    }

    public int getCarrierListVersion() {
        Cursor cursorQuery = this.mContext.getContentResolver().query(Uri.withAppendedPath(Telephony.CarrierId.All.CONTENT_URI, "get_version"), null, null, null);
        cursorQuery.moveToFirst();
        return cursorQuery.getInt(0);
    }

    public int getCarrierId() {
        return this.mCarrierId;
    }

    public String getCarrierName() {
        return this.mCarrierName;
    }

    private static boolean equals(String str, String str2, boolean z) {
        if (str == null && str2 == null) {
            return true;
        }
        if (str == null || str2 == null) {
            return false;
        }
        return z ? str.equalsIgnoreCase(str2) : str.equals(str2);
    }

    private static void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private static void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println("mCarrierIdLocalLogs:");
        indentingPrintWriter.increaseIndent();
        this.mCarrierIdLocalLog.dump(fileDescriptor, printWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("mCarrierId: " + this.mCarrierId);
        indentingPrintWriter.println("mCarrierName: " + this.mCarrierName);
        indentingPrintWriter.println("version: " + getCarrierListVersion());
        indentingPrintWriter.println("mCarrierMatchingRules on mccmnc: " + this.mTelephonyMgr.getSimOperatorNumericForPhone(this.mPhone.getPhoneId()));
        indentingPrintWriter.increaseIndent();
        Iterator<CarrierMatchingRule> it = this.mCarrierMatchingRulesOnMccMnc.iterator();
        while (it.hasNext()) {
            indentingPrintWriter.println(it.next().toString());
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("mSpn: " + this.mSpn);
        indentingPrintWriter.println("mPreferApn: " + this.mPreferApn);
        indentingPrintWriter.flush();
    }
}
