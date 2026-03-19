package com.android.internal.telephony;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.IccConstants;
import java.util.List;

public class IccProvider extends ContentProvider {
    protected static final int ADN = 1;
    protected static final int ADN_ALL = 7;
    protected static final int ADN_SUB = 2;
    private static final boolean DBG = true;
    protected static final int FDN = 3;
    protected static final int FDN_SUB = 4;
    protected static final int SDN = 5;
    protected static final int SDN_SUB = 6;
    protected static final String STR_PIN2 = "pin2";
    protected static final String STR_TAG = "tag";
    private static final String TAG = "IccProvider";
    private SubscriptionManager mSubscriptionManager;
    protected static final String STR_NUMBER = "number";
    protected static final String STR_EMAILS = "emails";
    private static final String[] ADDRESS_BOOK_COLUMN_NAMES = {"name", STR_NUMBER, STR_EMAILS, HbpcdLookup.ID};
    static IccInternalInterface sMtkIccProvider = null;
    private static final UriMatcher URL_MATCHER = new UriMatcher(-1);

    static {
        URL_MATCHER.addURI("icc", "adn", 1);
        URL_MATCHER.addURI("icc", "adn/subId/#", 2);
        URL_MATCHER.addURI("icc", "fdn", 3);
        URL_MATCHER.addURI("icc", "fdn/subId/#", 4);
        URL_MATCHER.addURI("icc", "sdn", 5);
        URL_MATCHER.addURI("icc", "sdn/subId/#", 6);
    }

    @Override
    public boolean onCreate() {
        this.mSubscriptionManager = SubscriptionManager.from(getContext());
        sMtkIccProvider = TelephonyComponentFactory.getInstance().makeIccProvider(URL_MATCHER, getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        if (sMtkIccProvider != null) {
            return sMtkIccProvider.query(uri, strArr, str, strArr2, str2);
        }
        log("query");
        switch (URL_MATCHER.match(uri)) {
            case 1:
                return loadFromEf(28474, SubscriptionManager.getDefaultSubscriptionId());
            case 2:
                return loadFromEf(28474, getRequestSubId(uri));
            case 3:
                return loadFromEf(IccConstants.EF_FDN, SubscriptionManager.getDefaultSubscriptionId());
            case 4:
                return loadFromEf(IccConstants.EF_FDN, getRequestSubId(uri));
            case 5:
                return loadFromEf(IccConstants.EF_SDN, SubscriptionManager.getDefaultSubscriptionId());
            case 6:
                return loadFromEf(IccConstants.EF_SDN, getRequestSubId(uri));
            case 7:
                return loadAllSimContacts(28474);
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    private Cursor loadAllSimContacts(int i) {
        Cursor[] cursorArr;
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList == null || activeSubscriptionInfoList.size() == 0) {
            cursorArr = new Cursor[0];
        } else {
            int size = activeSubscriptionInfoList.size();
            cursorArr = new Cursor[size];
            for (int i2 = 0; i2 < size; i2++) {
                int subscriptionId = activeSubscriptionInfoList.get(i2).getSubscriptionId();
                cursorArr[i2] = loadFromEf(i, subscriptionId);
                Rlog.i(TAG, "ADN Records loaded for Subscription ::" + subscriptionId);
            }
        }
        return new MergeCursor(cursorArr);
    }

    @Override
    public String getType(Uri uri) {
        switch (URL_MATCHER.match(uri)) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                return "vnd.android.cursor.dir/sim-contact";
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int defaultSubscriptionId;
        String str;
        int i;
        int i2;
        int defaultSubscriptionId2;
        String asString;
        if (sMtkIccProvider != null) {
            Uri uriInsert = sMtkIccProvider.insert(uri, contentValues);
            getContext().getContentResolver().notifyChange(uri, null);
            return uriInsert;
        }
        log("insert");
        int iMatch = URL_MATCHER.match(uri);
        switch (iMatch) {
            case 1:
                defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
                str = null;
                i = 28474;
                i2 = defaultSubscriptionId;
                if (addIccRecordToEf(i, contentValues.getAsString(STR_TAG), contentValues.getAsString(STR_NUMBER), null, str, i2)) {
                    return null;
                }
                StringBuilder sb = new StringBuilder("content://icc/");
                switch (iMatch) {
                    case 1:
                        sb.append("adn/");
                        break;
                    case 2:
                        sb.append("adn/subId/");
                        break;
                    case 3:
                        sb.append("fdn/");
                        break;
                    case 4:
                        sb.append("fdn/subId/");
                        break;
                }
                sb.append(0);
                Uri uri2 = Uri.parse(sb.toString());
                getContext().getContentResolver().notifyChange(uri, null);
                return uri2;
            case 2:
                defaultSubscriptionId = getRequestSubId(uri);
                str = null;
                i = 28474;
                i2 = defaultSubscriptionId;
                if (addIccRecordToEf(i, contentValues.getAsString(STR_TAG), contentValues.getAsString(STR_NUMBER), null, str, i2)) {
                }
                break;
            case 3:
                defaultSubscriptionId2 = SubscriptionManager.getDefaultSubscriptionId();
                asString = contentValues.getAsString(STR_PIN2);
                i2 = defaultSubscriptionId2;
                i = 28475;
                str = asString;
                if (addIccRecordToEf(i, contentValues.getAsString(STR_TAG), contentValues.getAsString(STR_NUMBER), null, str, i2)) {
                }
                break;
            case 4:
                defaultSubscriptionId2 = getRequestSubId(uri);
                asString = contentValues.getAsString(STR_PIN2);
                i2 = defaultSubscriptionId2;
                i = 28475;
                str = asString;
                if (addIccRecordToEf(i, contentValues.getAsString(STR_TAG), contentValues.getAsString(STR_NUMBER), null, str, i2)) {
                }
                break;
            default:
                throw new UnsupportedOperationException("Cannot insert into URL: " + uri);
        }
    }

    private String normalizeValue(String str) {
        int length = str.length();
        if (length == 0) {
            log("len of input String is 0");
            return str;
        }
        if (str.charAt(0) != '\'') {
            return str;
        }
        int i = length - 1;
        if (str.charAt(i) == '\'') {
            return str.substring(1, i);
        }
        return str;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int defaultSubscriptionId;
        int i;
        int i2;
        int defaultSubscriptionId2;
        int length;
        if (sMtkIccProvider != null) {
            int iDelete = sMtkIccProvider.delete(uri, str, strArr);
            if (iDelete <= 0) {
                return iDelete;
            }
            getContext().getContentResolver().notifyChange(uri, null);
            return iDelete;
        }
        switch (URL_MATCHER.match(uri)) {
            case 1:
                defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
                i = defaultSubscriptionId;
                i2 = 28474;
                log("delete");
                String[] strArrSplit = str.split("AND");
                length = strArrSplit.length;
                String strNormalizeValue = null;
                String strNormalizeValue2 = null;
                String strNormalizeValue3 = null;
                while (true) {
                    length--;
                    if (length < 0) {
                        String str2 = strArrSplit[length];
                        log("parsing '" + str2 + "'");
                        String[] strArrSplit2 = str2.split("=", 2);
                        if (strArrSplit2.length != 2) {
                            Rlog.e(TAG, "resolve: bad whereClause parameter: " + str2);
                        } else {
                            String strTrim = strArrSplit2[0].trim();
                            String strTrim2 = strArrSplit2[1].trim();
                            if (STR_TAG.equals(strTrim)) {
                                strNormalizeValue = normalizeValue(strTrim2);
                            } else if (STR_NUMBER.equals(strTrim)) {
                                strNormalizeValue2 = normalizeValue(strTrim2);
                            } else if (!STR_EMAILS.equals(strTrim) && STR_PIN2.equals(strTrim)) {
                                strNormalizeValue3 = normalizeValue(strTrim2);
                            }
                        }
                    } else {
                        if ((i2 == 3 && TextUtils.isEmpty(strNormalizeValue3)) || !deleteIccRecordFromEf(i2, strNormalizeValue, strNormalizeValue2, null, strNormalizeValue3, i)) {
                            return 0;
                        }
                        getContext().getContentResolver().notifyChange(uri, null);
                        return 1;
                    }
                }
                break;
            case 2:
                defaultSubscriptionId = getRequestSubId(uri);
                i = defaultSubscriptionId;
                i2 = 28474;
                log("delete");
                String[] strArrSplit3 = str.split("AND");
                length = strArrSplit3.length;
                String strNormalizeValue4 = null;
                String strNormalizeValue22 = null;
                String strNormalizeValue32 = null;
                while (true) {
                    length--;
                    if (length < 0) {
                    }
                }
                break;
            case 3:
                defaultSubscriptionId2 = SubscriptionManager.getDefaultSubscriptionId();
                i = defaultSubscriptionId2;
                i2 = 28475;
                log("delete");
                String[] strArrSplit32 = str.split("AND");
                length = strArrSplit32.length;
                String strNormalizeValue42 = null;
                String strNormalizeValue222 = null;
                String strNormalizeValue322 = null;
                while (true) {
                    length--;
                    if (length < 0) {
                    }
                }
                break;
            case 4:
                defaultSubscriptionId2 = getRequestSubId(uri);
                i = defaultSubscriptionId2;
                i2 = 28475;
                log("delete");
                String[] strArrSplit322 = str.split("AND");
                length = strArrSplit322.length;
                String strNormalizeValue422 = null;
                String strNormalizeValue2222 = null;
                String strNormalizeValue3222 = null;
                while (true) {
                    length--;
                    if (length < 0) {
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Cannot insert into URL: " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int defaultSubscriptionId;
        int i;
        int i2;
        String str2;
        int defaultSubscriptionId2;
        String asString;
        if (sMtkIccProvider != null) {
            int iUpdate = sMtkIccProvider.update(uri, contentValues, str, strArr);
            if (iUpdate <= 0) {
                return iUpdate;
            }
            getContext().getContentResolver().notifyChange(uri, null);
            return iUpdate;
        }
        log("update");
        switch (URL_MATCHER.match(uri)) {
            case 1:
                defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
                i = defaultSubscriptionId;
                i2 = 28474;
                str2 = null;
                if (updateIccRecordInEf(i2, contentValues.getAsString(STR_TAG), contentValues.getAsString(STR_NUMBER), contentValues.getAsString("newTag"), contentValues.getAsString("newNumber"), str2, i)) {
                    return 0;
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return 1;
            case 2:
                defaultSubscriptionId = getRequestSubId(uri);
                i = defaultSubscriptionId;
                i2 = 28474;
                str2 = null;
                if (updateIccRecordInEf(i2, contentValues.getAsString(STR_TAG), contentValues.getAsString(STR_NUMBER), contentValues.getAsString("newTag"), contentValues.getAsString("newNumber"), str2, i)) {
                }
                break;
            case 3:
                defaultSubscriptionId2 = SubscriptionManager.getDefaultSubscriptionId();
                asString = contentValues.getAsString(STR_PIN2);
                i = defaultSubscriptionId2;
                str2 = asString;
                i2 = 28475;
                if (updateIccRecordInEf(i2, contentValues.getAsString(STR_TAG), contentValues.getAsString(STR_NUMBER), contentValues.getAsString("newTag"), contentValues.getAsString("newNumber"), str2, i)) {
                }
                break;
            case 4:
                defaultSubscriptionId2 = getRequestSubId(uri);
                asString = contentValues.getAsString(STR_PIN2);
                i = defaultSubscriptionId2;
                str2 = asString;
                i2 = 28475;
                if (updateIccRecordInEf(i2, contentValues.getAsString(STR_TAG), contentValues.getAsString(STR_NUMBER), contentValues.getAsString("newTag"), contentValues.getAsString("newNumber"), str2, i)) {
                }
                break;
            default:
                throw new UnsupportedOperationException("Cannot insert into URL: " + uri);
        }
    }

    private MatrixCursor loadFromEf(int i, int i2) throws RemoteException {
        log("loadFromEf: efType=0x" + Integer.toHexString(i).toUpperCase() + ", subscription=" + i2);
        List<AdnRecord> adnRecordsInEfForSubscriber = null;
        try {
            IIccPhoneBook iIccPhoneBookAsInterface = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iIccPhoneBookAsInterface != null) {
                adnRecordsInEfForSubscriber = iIccPhoneBookAsInterface.getAdnRecordsInEfForSubscriber(i2, i);
            }
        } catch (RemoteException e) {
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        if (adnRecordsInEfForSubscriber != null) {
            int size = adnRecordsInEfForSubscriber.size();
            MatrixCursor matrixCursor = new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES, size);
            log("adnRecords.size=" + size);
            for (int i3 = 0; i3 < size; i3++) {
                loadRecord(adnRecordsInEfForSubscriber.get(i3), matrixCursor, i3);
            }
            return matrixCursor;
        }
        Rlog.w(TAG, "Cannot load ADN records");
        return new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES);
    }

    private boolean addIccRecordToEf(int i, String str, String str2, String[] strArr, String str3, int i2) throws RemoteException {
        log("addIccRecordToEf: efType=0x" + Integer.toHexString(i).toUpperCase() + ", name=" + Rlog.pii(TAG, str) + ", number=" + Rlog.pii(TAG, str2) + ", emails=" + Rlog.pii(TAG, strArr) + ", subscription=" + i2);
        boolean zUpdateAdnRecordsInEfBySearchForSubscriber = false;
        try {
            IIccPhoneBook iIccPhoneBookAsInterface = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iIccPhoneBookAsInterface != null) {
                zUpdateAdnRecordsInEfBySearchForSubscriber = iIccPhoneBookAsInterface.updateAdnRecordsInEfBySearchForSubscriber(i2, i, "", "", str, str2, str3);
            }
        } catch (RemoteException e) {
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        log("addIccRecordToEf: " + zUpdateAdnRecordsInEfBySearchForSubscriber);
        return zUpdateAdnRecordsInEfBySearchForSubscriber;
    }

    private boolean updateIccRecordInEf(int i, String str, String str2, String str3, String str4, String str5, int i2) throws RemoteException {
        log("updateIccRecordInEf: efType=0x" + Integer.toHexString(i).toUpperCase() + ", oldname=" + Rlog.pii(TAG, str) + ", oldnumber=" + Rlog.pii(TAG, str2) + ", newname=" + Rlog.pii(TAG, str3) + ", newnumber=" + Rlog.pii(TAG, str3) + ", subscription=" + i2);
        boolean zUpdateAdnRecordsInEfBySearchForSubscriber = false;
        try {
            IIccPhoneBook iIccPhoneBookAsInterface = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iIccPhoneBookAsInterface != null) {
                zUpdateAdnRecordsInEfBySearchForSubscriber = iIccPhoneBookAsInterface.updateAdnRecordsInEfBySearchForSubscriber(i2, i, str, str2, str3, str4, str5);
            }
        } catch (RemoteException e) {
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        log("updateIccRecordInEf: " + zUpdateAdnRecordsInEfBySearchForSubscriber);
        return zUpdateAdnRecordsInEfBySearchForSubscriber;
    }

    private boolean deleteIccRecordFromEf(int i, String str, String str2, String[] strArr, String str3, int i2) throws RemoteException {
        log("deleteIccRecordFromEf: efType=0x" + Integer.toHexString(i).toUpperCase() + ", name=" + Rlog.pii(TAG, str) + ", number=" + Rlog.pii(TAG, str2) + ", emails=" + Rlog.pii(TAG, strArr) + ", pin2=" + Rlog.pii(TAG, str3) + ", subscription=" + i2);
        boolean zUpdateAdnRecordsInEfBySearchForSubscriber = false;
        try {
            IIccPhoneBook iIccPhoneBookAsInterface = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iIccPhoneBookAsInterface != null) {
                zUpdateAdnRecordsInEfBySearchForSubscriber = iIccPhoneBookAsInterface.updateAdnRecordsInEfBySearchForSubscriber(i2, i, str, str2, "", "", str3);
            }
        } catch (RemoteException e) {
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        log("deleteIccRecordFromEf: " + zUpdateAdnRecordsInEfBySearchForSubscriber);
        return zUpdateAdnRecordsInEfBySearchForSubscriber;
    }

    private void loadRecord(AdnRecord adnRecord, MatrixCursor matrixCursor, int i) {
        if (!adnRecord.isEmpty()) {
            Object[] objArr = new Object[4];
            String alphaTag = adnRecord.getAlphaTag();
            String number = adnRecord.getNumber();
            log("loadRecord: " + alphaTag + ", " + Rlog.pii(TAG, number));
            objArr[0] = alphaTag;
            objArr[1] = number;
            String[] emails = adnRecord.getEmails();
            if (emails != null) {
                StringBuilder sb = new StringBuilder();
                for (String str : emails) {
                    log("Adding email:" + Rlog.pii(TAG, str));
                    sb.append(str);
                    sb.append(",");
                }
                objArr[2] = sb.toString();
            }
            objArr[3] = Integer.valueOf(i);
            matrixCursor.addRow(objArr);
        }
    }

    private void log(String str) {
        Rlog.d(TAG, "[IccProvider] " + str);
    }

    private int getRequestSubId(Uri uri) {
        log("getRequestSubId url: " + uri);
        try {
            return Integer.parseInt(uri.getLastPathSegment());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }
}
