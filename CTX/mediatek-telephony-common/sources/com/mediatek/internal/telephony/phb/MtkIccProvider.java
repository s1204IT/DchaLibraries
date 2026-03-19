package com.mediatek.internal.telephony.phb;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import com.android.internal.telephony.IccInternalInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.phb.IMtkIccPhoneBook;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class MtkIccProvider implements IccInternalInterface {
    private static final int ADDRESS_SUPPORT_AAS = 8;
    private static final int ADDRESS_SUPPORT_SNE = 9;
    protected static final int ADN = 1;
    protected static final int ADN_ALL = 9;
    protected static final int ADN_SUB = 2;
    protected static final int FDN = 3;
    protected static final int FDN_SUB = 4;
    protected static final int SDN = 5;
    protected static final int SDN_SUB = 6;
    protected static final String STR_ANR = "anr";
    protected static final String STR_NUMBER = "number";
    protected static final String STR_PIN2 = "pin2";
    protected static final String STR_TAG = "tag";
    private static final String TAG = "MtkIccProvider";
    protected static final int UPB = 7;
    protected static final int UPB_SUB = 8;
    private static UriMatcher URL_MATCHER;
    private Context mContext;
    private static final boolean DBG = !SystemProperties.get("ro.build.type").equals(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
    protected static final String STR_INDEX = "index";
    protected static final String STR_EMAILS = "emails";
    private static final String[] ADDRESS_BOOK_COLUMN_NAMES = {STR_INDEX, "name", "number", STR_EMAILS, "additionalNumber", "groupIds", "_id", "aas", "sne"};

    public MtkIccProvider(UriMatcher uriMatcher, Context context) {
        logi("MtkIccProvider URL_MATCHER " + uriMatcher);
        uriMatcher.addURI("icc", "pbr", 7);
        uriMatcher.addURI("icc", "pbr/subId/#", 8);
        URL_MATCHER = uriMatcher;
        this.mContext = context;
    }

    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        logi("query " + uri);
        switch (URL_MATCHER.match(uri)) {
            case 1:
                return loadFromEf(28474, SubscriptionManager.getDefaultSubscriptionId());
            case 2:
                return loadFromEf(28474, getRequestSubId(uri));
            case 3:
                return loadFromEf(28475, SubscriptionManager.getDefaultSubscriptionId());
            case 4:
                return loadFromEf(28475, getRequestSubId(uri));
            case 5:
                return loadFromEf(28489, SubscriptionManager.getDefaultSubscriptionId());
            case 6:
                return loadFromEf(28489, getRequestSubId(uri));
            case 7:
                return loadFromEf(20272, SubscriptionManager.getDefaultSubscriptionId());
            case 8:
                return loadFromEf(20272, getRequestSubId(uri));
            case 9:
                return loadAllSimContacts(28474);
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    private Cursor loadAllSimContacts(int i) {
        int[] activeSubscriptionIdList = SubscriptionManager.from(this.mContext).getActiveSubscriptionIdList();
        Cursor[] cursorArr = new Cursor[activeSubscriptionIdList.length];
        int length = activeSubscriptionIdList.length;
        int i2 = 0;
        int i3 = 0;
        while (i2 < length) {
            int i4 = activeSubscriptionIdList[i2];
            cursorArr[i3] = loadFromEf(i, i4);
            Rlog.i(TAG, "loadAllSimContacts: subId=" + i4);
            i2++;
            i3++;
        }
        return new MergeCursor(cursorArr);
    }

    private MatrixCursor loadFromEf(int i, int i2) throws RemoteException {
        if (DBG) {
            log("loadFromEf: efType=0x" + Integer.toHexString(i).toUpperCase() + ", subscription=" + i2);
        }
        List<MtkAdnRecord> adnRecordsInEfForSubscriber = null;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                adnRecordsInEfForSubscriber = iccPhbService.getAdnRecordsInEfForSubscriber(i2, i);
            }
        } catch (RemoteException e) {
            if (DBG) {
                log(e.toString());
            }
        } catch (SecurityException e2) {
            if (DBG) {
                log(e2.toString());
            }
        }
        if (adnRecordsInEfForSubscriber != null) {
            int size = adnRecordsInEfForSubscriber.size();
            MatrixCursor matrixCursor = new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES, size);
            if (DBG) {
                log("adnRecords.size=" + size);
            }
            for (int i3 = 0; i3 < size; i3++) {
                loadRecord(adnRecordsInEfForSubscriber.get(i3), matrixCursor, i3);
            }
            logi("query success, size = " + size);
            return matrixCursor;
        }
        Rlog.w(TAG, "Cannot load ADN records");
        return new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES);
    }

    public Uri insert(Uri uri, ContentValues contentValues) throws RemoteException {
        int defaultSubscriptionId;
        int i;
        int defaultSubscriptionId2;
        String asString;
        String str;
        int defaultSubscriptionId3;
        int iAddUsimRecordToEf;
        logi("insert " + uri);
        int iMatch = URL_MATCHER.match(uri);
        int i2 = 28474;
        String[] strArr = null;
        switch (iMatch) {
            case 1:
                defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
                i = defaultSubscriptionId;
                str = null;
                String asString2 = contentValues.getAsString(STR_TAG);
                String asString3 = contentValues.getAsString("number");
                if (7 != iMatch || 8 == iMatch) {
                    String asString4 = contentValues.getAsString("gas");
                    String asString5 = contentValues.getAsString(STR_ANR);
                    String asString6 = contentValues.getAsString(STR_EMAILS);
                    if (ADDRESS_BOOK_COLUMN_NAMES.length < 8) {
                        Integer asInteger = contentValues.getAsInteger("aas");
                        if (asString3 == null) {
                            asString3 = "";
                        }
                        if (asString2 == null) {
                            asString2 = "";
                        }
                        MtkAdnRecord mtkAdnRecord = new MtkAdnRecord(i2, 0, asString2, asString3);
                        mtkAdnRecord.setAnr(asString5);
                        if (contentValues.containsKey("anr2")) {
                            String asString7 = contentValues.getAsString("anr2");
                            if (DBG) {
                                log("insert anr2: " + asString7);
                            }
                            mtkAdnRecord.setAnr(asString7, 1);
                        }
                        if (contentValues.containsKey("anr3")) {
                            String asString8 = contentValues.getAsString("anr3");
                            if (DBG) {
                                log("insert anr3: " + asString8);
                            }
                            mtkAdnRecord.setAnr(asString8, 2);
                        }
                        mtkAdnRecord.setGrpIds(asString4);
                        if (asString6 != null && !asString6.equals("")) {
                            strArr = new String[]{asString6};
                        }
                        mtkAdnRecord.setEmails(strArr);
                        if (asInteger != null) {
                            mtkAdnRecord.setAasIndex(asInteger.intValue());
                        }
                        if (ADDRESS_BOOK_COLUMN_NAMES.length >= 9) {
                            mtkAdnRecord.setSne(contentValues.getAsString("sne"));
                        }
                        logi("updateUsimPBRecordsBySearchWithError ");
                        iAddUsimRecordToEf = updateUsimPBRecordsBySearchWithError(i2, new MtkAdnRecord("", "", ""), mtkAdnRecord, i);
                    } else {
                        logi("addUsimRecordToEf ");
                        iAddUsimRecordToEf = addUsimRecordToEf(i2, asString2, asString3, asString5, asString6, asString4, i);
                    }
                    if (iAddUsimRecordToEf > 0) {
                        updatePhbStorageInfo(1, i);
                    }
                } else {
                    logi("addIccRecordToEf ");
                    iAddUsimRecordToEf = addIccRecordToEf(i2, asString2, asString3, null, str, i);
                }
                StringBuilder sb = new StringBuilder("content://icc/");
                if (iAddUsimRecordToEf > 0) {
                    sb.append("error/");
                    sb.append(iAddUsimRecordToEf);
                } else {
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
                        case 5:
                        case 6:
                        default:
                            throw new UnsupportedOperationException("Cannot insert into URL: " + uri);
                        case 7:
                            sb.append("pbr/");
                            break;
                        case 8:
                            sb.append("pbr/subId/");
                            break;
                    }
                    sb.append(iAddUsimRecordToEf);
                }
                Uri uri2 = Uri.parse(sb.toString());
                logi(uri2.toString());
                return uri2;
            case 2:
                defaultSubscriptionId = getRequestSubId(uri);
                i = defaultSubscriptionId;
                str = null;
                String asString22 = contentValues.getAsString(STR_TAG);
                String asString32 = contentValues.getAsString("number");
                if (7 != iMatch) {
                    String asString42 = contentValues.getAsString("gas");
                    String asString52 = contentValues.getAsString(STR_ANR);
                    String asString62 = contentValues.getAsString(STR_EMAILS);
                    if (ADDRESS_BOOK_COLUMN_NAMES.length < 8) {
                    }
                    if (iAddUsimRecordToEf > 0) {
                    }
                }
                StringBuilder sb2 = new StringBuilder("content://icc/");
                if (iAddUsimRecordToEf > 0) {
                }
                Uri uri22 = Uri.parse(sb2.toString());
                logi(uri22.toString());
                return uri22;
            case 3:
                defaultSubscriptionId2 = SubscriptionManager.getDefaultSubscriptionId();
                asString = contentValues.getAsString(STR_PIN2);
                i = defaultSubscriptionId2;
                i2 = 28475;
                str = asString;
                String asString222 = contentValues.getAsString(STR_TAG);
                String asString322 = contentValues.getAsString("number");
                if (7 != iMatch) {
                }
                StringBuilder sb22 = new StringBuilder("content://icc/");
                if (iAddUsimRecordToEf > 0) {
                }
                Uri uri222 = Uri.parse(sb22.toString());
                logi(uri222.toString());
                return uri222;
            case 4:
                defaultSubscriptionId2 = getRequestSubId(uri);
                asString = contentValues.getAsString(STR_PIN2);
                i = defaultSubscriptionId2;
                i2 = 28475;
                str = asString;
                String asString2222 = contentValues.getAsString(STR_TAG);
                String asString3222 = contentValues.getAsString("number");
                if (7 != iMatch) {
                }
                StringBuilder sb222 = new StringBuilder("content://icc/");
                if (iAddUsimRecordToEf > 0) {
                }
                Uri uri2222 = Uri.parse(sb222.toString());
                logi(uri2222.toString());
                return uri2222;
            case 5:
            case 6:
            default:
                throw new UnsupportedOperationException("Cannot insert into URL: " + uri);
            case 7:
                defaultSubscriptionId3 = SubscriptionManager.getDefaultSubscriptionId();
                i = defaultSubscriptionId3;
                i2 = 20272;
                str = null;
                String asString22222 = contentValues.getAsString(STR_TAG);
                String asString32222 = contentValues.getAsString("number");
                if (7 != iMatch) {
                }
                StringBuilder sb2222 = new StringBuilder("content://icc/");
                if (iAddUsimRecordToEf > 0) {
                }
                Uri uri22222 = Uri.parse(sb2222.toString());
                logi(uri22222.toString());
                return uri22222;
            case 8:
                defaultSubscriptionId3 = getRequestSubId(uri);
                i = defaultSubscriptionId3;
                i2 = 20272;
                str = null;
                String asString222222 = contentValues.getAsString(STR_TAG);
                String asString322222 = contentValues.getAsString("number");
                if (7 != iMatch) {
                }
                StringBuilder sb22222 = new StringBuilder("content://icc/");
                if (iAddUsimRecordToEf > 0) {
                }
                Uri uri222222 = Uri.parse(sb22222.toString());
                logi(uri222222.toString());
                return uri222222;
        }
    }

    private String normalizeValue(String str) {
        int length = str.length();
        if (length == 0) {
            if (DBG) {
                log("len of input String is 0");
            }
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

    public int delete(Uri uri, String str, String[] strArr) throws RemoteException {
        int defaultSubscriptionId;
        int i;
        int defaultSubscriptionId2;
        int defaultSubscriptionId3;
        int length;
        int iDeleteUsimRecordFromEf;
        int iDeleteUsimRecordFromEfByIndex;
        logi("delete " + uri);
        int iMatch = URL_MATCHER.match(uri);
        int i2 = 28474;
        switch (iMatch) {
            case 1:
                defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
                i = defaultSubscriptionId;
                String strNormalizeValue = null;
                String[] strArrSplit = str.split("AND");
                length = strArrSplit.length;
                String strNormalizeValue2 = "";
                String strNormalizeValue3 = "";
                int i3 = -1;
                while (true) {
                    length--;
                    if (length >= 0) {
                        if (i3 > 0) {
                            logi("delete index is " + i3);
                            if (7 == iMatch || 8 == iMatch) {
                                logi("deleteUsimRecordFromEfByIndex ");
                                iDeleteUsimRecordFromEfByIndex = deleteUsimRecordFromEfByIndex(i2, i3, i);
                                if (iDeleteUsimRecordFromEfByIndex > 0) {
                                    updatePhbStorageInfo(-1, i);
                                }
                            } else {
                                logi("deleteIccRecordFromEfByIndex ");
                                iDeleteUsimRecordFromEfByIndex = deleteIccRecordFromEfByIndex(i2, i3, strNormalizeValue, i);
                            }
                            logi("delete result = " + iDeleteUsimRecordFromEfByIndex);
                            return iDeleteUsimRecordFromEfByIndex;
                        }
                        if (i2 == 28475 && TextUtils.isEmpty(strNormalizeValue)) {
                            return -5;
                        }
                        if (strNormalizeValue3.length() == 0 && strNormalizeValue2.length() == 0) {
                            return 0;
                        }
                        if (7 == iMatch || 8 == iMatch) {
                            if (ADDRESS_BOOK_COLUMN_NAMES.length >= 8) {
                                logi("updateUsimPBRecordsBySearchWithError ");
                                iDeleteUsimRecordFromEf = updateUsimPBRecordsBySearchWithError(i2, new MtkAdnRecord(strNormalizeValue3, strNormalizeValue2, ""), new MtkAdnRecord("", "", ""), i);
                            } else {
                                logi("deleteUsimRecordFromEf ");
                                iDeleteUsimRecordFromEf = deleteUsimRecordFromEf(i2, strNormalizeValue3, strNormalizeValue2, null, i);
                            }
                            if (iDeleteUsimRecordFromEf > 0) {
                                updatePhbStorageInfo(-1, i);
                            }
                        } else {
                            logi("deleteIccRecordFromEf ");
                            iDeleteUsimRecordFromEf = deleteIccRecordFromEf(i2, strNormalizeValue3, strNormalizeValue2, null, strNormalizeValue, i);
                        }
                        logi("delete result = " + iDeleteUsimRecordFromEf);
                        return iDeleteUsimRecordFromEf;
                    }
                    String str2 = strArrSplit[length];
                    if (DBG) {
                        log("parsing '" + str2 + "'");
                    }
                    int iIndexOf = str2.indexOf(61);
                    if (iIndexOf == -1) {
                        Rlog.e(TAG, "resolve: bad whereClause parameter: " + str2);
                    } else {
                        String strTrim = str2.substring(0, iIndexOf).trim();
                        String strTrim2 = str2.substring(iIndexOf + 1).trim();
                        if (DBG) {
                            log("parsing key is " + strTrim + " index of = is " + iIndexOf + " val is " + strTrim2);
                        }
                        if (STR_INDEX.equals(strTrim)) {
                            i3 = Integer.parseInt(strTrim2);
                        } else if (STR_TAG.equals(strTrim)) {
                            strNormalizeValue3 = normalizeValue(strTrim2);
                        } else if ("number".equals(strTrim)) {
                            strNormalizeValue2 = normalizeValue(strTrim2);
                        } else if (!STR_EMAILS.equals(strTrim) && STR_PIN2.equals(strTrim)) {
                            strNormalizeValue = normalizeValue(strTrim2);
                        }
                    }
                }
                break;
            case 2:
                defaultSubscriptionId = getRequestSubId(uri);
                i = defaultSubscriptionId;
                String strNormalizeValue4 = null;
                String[] strArrSplit2 = str.split("AND");
                length = strArrSplit2.length;
                String strNormalizeValue22 = "";
                String strNormalizeValue32 = "";
                int i32 = -1;
                while (true) {
                    length--;
                    if (length >= 0) {
                    }
                }
                break;
            case 3:
                defaultSubscriptionId2 = SubscriptionManager.getDefaultSubscriptionId();
                i = defaultSubscriptionId2;
                i2 = 28475;
                String strNormalizeValue42 = null;
                String[] strArrSplit22 = str.split("AND");
                length = strArrSplit22.length;
                String strNormalizeValue222 = "";
                String strNormalizeValue322 = "";
                int i322 = -1;
                while (true) {
                    length--;
                    if (length >= 0) {
                    }
                }
                break;
            case 4:
                defaultSubscriptionId2 = getRequestSubId(uri);
                i = defaultSubscriptionId2;
                i2 = 28475;
                String strNormalizeValue422 = null;
                String[] strArrSplit222 = str.split("AND");
                length = strArrSplit222.length;
                String strNormalizeValue2222 = "";
                String strNormalizeValue3222 = "";
                int i3222 = -1;
                while (true) {
                    length--;
                    if (length >= 0) {
                    }
                }
                break;
            case 5:
            case 6:
            default:
                throw new UnsupportedOperationException("Cannot insert into URL: " + uri);
            case 7:
                defaultSubscriptionId3 = SubscriptionManager.getDefaultSubscriptionId();
                i = defaultSubscriptionId3;
                i2 = 20272;
                String strNormalizeValue4222 = null;
                String[] strArrSplit2222 = str.split("AND");
                length = strArrSplit2222.length;
                String strNormalizeValue22222 = "";
                String strNormalizeValue32222 = "";
                int i32222 = -1;
                while (true) {
                    length--;
                    if (length >= 0) {
                    }
                }
                break;
            case 8:
                defaultSubscriptionId3 = getRequestSubId(uri);
                i = defaultSubscriptionId3;
                i2 = 20272;
                String strNormalizeValue42222 = null;
                String[] strArrSplit22222 = str.split("AND");
                length = strArrSplit22222.length;
                String strNormalizeValue222222 = "";
                String strNormalizeValue322222 = "";
                int i322222 = -1;
                while (true) {
                    length--;
                    if (length >= 0) {
                    }
                }
                break;
        }
    }

    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) throws RemoteException {
        int defaultSubscriptionId;
        int i;
        int defaultSubscriptionId2;
        String asString;
        String str2;
        int defaultSubscriptionId3;
        String asString2;
        String asString3;
        Integer asInteger;
        int iIntValue;
        String asString4;
        Integer asInteger2;
        String asString5;
        String str3;
        String[] strArr2;
        int iUpdateUsimRecordInEf;
        logi("update " + uri);
        int iMatch = URL_MATCHER.match(uri);
        int i2 = 28474;
        switch (iMatch) {
            case 1:
                defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
                i = defaultSubscriptionId;
                str2 = null;
                String asString6 = contentValues.getAsString(STR_TAG);
                String asString7 = contentValues.getAsString("number");
                asString2 = contentValues.getAsString("newTag");
                asString3 = contentValues.getAsString("newNumber");
                asInteger = contentValues.getAsInteger(STR_INDEX);
                if (asInteger != null) {
                    iIntValue = 0;
                } else {
                    iIntValue = asInteger.intValue();
                }
                logi("update: index=" + iIntValue);
                if (7 != iMatch || 8 == iMatch) {
                    String asString8 = contentValues.getAsString("newAnr");
                    asString4 = contentValues.getAsString("newEmails");
                    asInteger2 = contentValues.getAsInteger("aas");
                    asString5 = contentValues.getAsString("sne");
                    if (asString3 == null) {
                        asString3 = "";
                    }
                    if (asString2 == null) {
                        asString2 = "";
                    }
                    MtkAdnRecord mtkAdnRecord = new MtkAdnRecord(i2, 0, asString2, asString3);
                    mtkAdnRecord.setAnr(asString8);
                    if (!contentValues.containsKey("newAnr2")) {
                        String asString9 = contentValues.getAsString("newAnr2");
                        if (DBG) {
                            StringBuilder sb = new StringBuilder();
                            str3 = asString6;
                            sb.append("update newAnr2: ");
                            sb.append(asString9);
                            log(sb.toString());
                        } else {
                            str3 = asString6;
                        }
                        mtkAdnRecord.setAnr(asString9, 1);
                    } else {
                        str3 = asString6;
                    }
                    if (contentValues.containsKey("newAnr3")) {
                        String asString10 = contentValues.getAsString("newAnr3");
                        if (DBG) {
                            log("update newAnr3: " + asString10);
                        }
                        mtkAdnRecord.setAnr(asString10, 2);
                    }
                    if (asString4 == null && !asString4.equals("")) {
                        strArr2 = new String[]{asString4};
                    } else {
                        strArr2 = null;
                    }
                    mtkAdnRecord.setEmails(strArr2);
                    if (asInteger2 != null) {
                        mtkAdnRecord.setAasIndex(asInteger2.intValue());
                    }
                    if (asString5 != null) {
                        mtkAdnRecord.setSne(asString5);
                    }
                    if (iIntValue <= 0) {
                        if (ADDRESS_BOOK_COLUMN_NAMES.length >= 8) {
                            logi("updateUsimPBRecordsByIndexWithError");
                            iUpdateUsimRecordInEf = updateUsimPBRecordsByIndexWithError(i2, mtkAdnRecord, iIntValue, i);
                        } else {
                            logi("updateUsimRecordInEfByIndex");
                            iUpdateUsimRecordInEf = updateUsimRecordInEfByIndex(i2, iIntValue, asString2, asString3, asString8, asString4, i);
                        }
                    } else if (ADDRESS_BOOK_COLUMN_NAMES.length >= 8) {
                        logi("updateUsimPBRecordsBySearchWithError");
                        iUpdateUsimRecordInEf = updateUsimPBRecordsBySearchWithError(i2, new MtkAdnRecord(str3, asString7, ""), mtkAdnRecord, i);
                    } else {
                        logi("updateUsimRecordInEf");
                        iUpdateUsimRecordInEf = updateUsimRecordInEf(i2, str3, asString7, asString2, asString3, asString8, asString4, i);
                    }
                } else if (iIntValue > 0) {
                    logi("updateIccRecordInEfByIndex");
                    iUpdateUsimRecordInEf = updateIccRecordInEfByIndex(i2, iIntValue, asString2, asString3, str2, i);
                } else {
                    logi("updateIccRecordInEf");
                    iUpdateUsimRecordInEf = updateIccRecordInEf(i2, asString6, asString7, asString2, asString3, str2, i);
                }
                logi("update result = " + iUpdateUsimRecordInEf);
                return iUpdateUsimRecordInEf;
            case 2:
                defaultSubscriptionId = getRequestSubId(uri);
                i = defaultSubscriptionId;
                str2 = null;
                String asString62 = contentValues.getAsString(STR_TAG);
                String asString72 = contentValues.getAsString("number");
                asString2 = contentValues.getAsString("newTag");
                asString3 = contentValues.getAsString("newNumber");
                asInteger = contentValues.getAsInteger(STR_INDEX);
                if (asInteger != null) {
                }
                logi("update: index=" + iIntValue);
                if (7 != iMatch) {
                    String asString82 = contentValues.getAsString("newAnr");
                    asString4 = contentValues.getAsString("newEmails");
                    asInteger2 = contentValues.getAsInteger("aas");
                    asString5 = contentValues.getAsString("sne");
                    if (asString3 == null) {
                    }
                    if (asString2 == null) {
                    }
                    MtkAdnRecord mtkAdnRecord2 = new MtkAdnRecord(i2, 0, asString2, asString3);
                    mtkAdnRecord2.setAnr(asString82);
                    if (!contentValues.containsKey("newAnr2")) {
                    }
                    if (contentValues.containsKey("newAnr3")) {
                    }
                    if (asString4 == null) {
                        strArr2 = null;
                        mtkAdnRecord2.setEmails(strArr2);
                        if (asInteger2 != null) {
                        }
                        if (asString5 != null) {
                        }
                        if (iIntValue <= 0) {
                        }
                    }
                }
                logi("update result = " + iUpdateUsimRecordInEf);
                return iUpdateUsimRecordInEf;
            case 3:
                defaultSubscriptionId2 = SubscriptionManager.getDefaultSubscriptionId();
                asString = contentValues.getAsString(STR_PIN2);
                i = defaultSubscriptionId2;
                str2 = asString;
                i2 = 28475;
                String asString622 = contentValues.getAsString(STR_TAG);
                String asString722 = contentValues.getAsString("number");
                asString2 = contentValues.getAsString("newTag");
                asString3 = contentValues.getAsString("newNumber");
                asInteger = contentValues.getAsInteger(STR_INDEX);
                if (asInteger != null) {
                }
                logi("update: index=" + iIntValue);
                if (7 != iMatch) {
                }
                logi("update result = " + iUpdateUsimRecordInEf);
                return iUpdateUsimRecordInEf;
            case 4:
                defaultSubscriptionId2 = getRequestSubId(uri);
                asString = contentValues.getAsString(STR_PIN2);
                i = defaultSubscriptionId2;
                str2 = asString;
                i2 = 28475;
                String asString6222 = contentValues.getAsString(STR_TAG);
                String asString7222 = contentValues.getAsString("number");
                asString2 = contentValues.getAsString("newTag");
                asString3 = contentValues.getAsString("newNumber");
                asInteger = contentValues.getAsInteger(STR_INDEX);
                if (asInteger != null) {
                }
                logi("update: index=" + iIntValue);
                if (7 != iMatch) {
                }
                logi("update result = " + iUpdateUsimRecordInEf);
                return iUpdateUsimRecordInEf;
            case 5:
            case 6:
            default:
                throw new IllegalArgumentException("Unknown URL " + iMatch);
            case 7:
                defaultSubscriptionId3 = SubscriptionManager.getDefaultSubscriptionId();
                i = defaultSubscriptionId3;
                i2 = 20272;
                str2 = null;
                String asString62222 = contentValues.getAsString(STR_TAG);
                String asString72222 = contentValues.getAsString("number");
                asString2 = contentValues.getAsString("newTag");
                asString3 = contentValues.getAsString("newNumber");
                asInteger = contentValues.getAsInteger(STR_INDEX);
                if (asInteger != null) {
                }
                logi("update: index=" + iIntValue);
                if (7 != iMatch) {
                }
                logi("update result = " + iUpdateUsimRecordInEf);
                return iUpdateUsimRecordInEf;
            case 8:
                defaultSubscriptionId3 = getRequestSubId(uri);
                i = defaultSubscriptionId3;
                i2 = 20272;
                str2 = null;
                String asString622222 = contentValues.getAsString(STR_TAG);
                String asString722222 = contentValues.getAsString("number");
                asString2 = contentValues.getAsString("newTag");
                asString3 = contentValues.getAsString("newNumber");
                asInteger = contentValues.getAsInteger(STR_INDEX);
                if (asInteger != null) {
                }
                logi("update: index=" + iIntValue);
                if (7 != iMatch) {
                }
                logi("update result = " + iUpdateUsimRecordInEf);
                return iUpdateUsimRecordInEf;
        }
    }

    private int addIccRecordToEf(int i, String str, String str2, String[] strArr, String str3, int i2) throws RemoteException {
        String str4;
        String str5;
        int i3;
        int iUpdateAdnRecordsInEfBySearchWithError = 0;
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("addIccRecordToEf: efType=0x");
            sb.append(Integer.toHexString(i).toUpperCase());
            sb.append(", name=");
            str4 = str;
            sb.append(str4);
            sb.append(", number=");
            str5 = str2;
            sb.append(str5);
            sb.append(", emails=");
            sb.append(strArr == null ? "null" : strArr[0]);
            sb.append(", subscription=");
            i3 = i2;
            sb.append(i3);
            log(sb.toString());
        } else {
            str4 = str;
            str5 = str2;
            i3 = i2;
        }
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateAdnRecordsInEfBySearchWithError = iccPhbService.updateAdnRecordsInEfBySearchWithError(i3, i, "", "", str4, str5, str3);
            }
        } catch (RemoteException e) {
            if (DBG) {
                log(e.toString());
            }
        } catch (SecurityException e2) {
            if (DBG) {
                log(e2.toString());
            }
        }
        if (DBG) {
            log("addIccRecordToEf: " + iUpdateAdnRecordsInEfBySearchWithError);
        }
        return iUpdateAdnRecordsInEfBySearchWithError;
    }

    private int addUsimRecordToEf(int i, String str, String str2, String str3, String str4, String str5, int i2) throws RemoteException {
        int i3;
        String str6;
        String str7;
        String str8;
        int i4;
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("addUSIMRecordToEf: efType=");
            i3 = i;
            sb.append(i3);
            sb.append(", name=");
            str6 = str;
            sb.append(str6);
            sb.append(", number=");
            str7 = str2;
            sb.append(str7);
            sb.append(", anr =");
            str8 = str3;
            sb.append(str8);
            sb.append(", emails=");
            sb.append(str4);
            sb.append(", subId=");
            i4 = i2;
            sb.append(i4);
            log(sb.toString());
        } else {
            i3 = i;
            str6 = str;
            str7 = str2;
            str8 = str3;
            i4 = i2;
        }
        String[] strArr = null;
        int iUpdateUsimPBRecordsInEfBySearchWithError = 0;
        if (str4 != null && !str4.equals("")) {
            strArr = new String[]{str4};
        }
        String[] strArr2 = strArr;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateUsimPBRecordsInEfBySearchWithError = iccPhbService.updateUsimPBRecordsInEfBySearchWithError(i4, i3, "", "", "", null, null, str6, str7, str8, null, strArr2);
            }
        } catch (RemoteException e) {
            log(e.toString());
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        int i5 = iUpdateUsimPBRecordsInEfBySearchWithError;
        log("addUsimRecordToEf: " + i5);
        return i5;
    }

    private int updateIccRecordInEf(int i, String str, String str2, String str3, String str4, String str5, int i2) throws RemoteException {
        String str6;
        String str7;
        String str8;
        String str9;
        int i3;
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateIccRecordInEf: efType=0x");
            sb.append(Integer.toHexString(i).toUpperCase());
            sb.append(", oldname=");
            str6 = str;
            sb.append(str6);
            sb.append(", oldnumber=");
            str7 = str2;
            sb.append(str7);
            sb.append(", newname=");
            str8 = str3;
            sb.append(str8);
            sb.append(", newnumber=");
            str9 = str4;
            sb.append(str9);
            sb.append(", subscription=");
            i3 = i2;
            sb.append(i3);
            log(sb.toString());
        } else {
            str6 = str;
            str7 = str2;
            str8 = str3;
            str9 = str4;
            i3 = i2;
        }
        int iUpdateAdnRecordsInEfBySearchWithError = 0;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateAdnRecordsInEfBySearchWithError = iccPhbService.updateAdnRecordsInEfBySearchWithError(i3, i, str6, str7, str8, str9, str5);
            }
        } catch (RemoteException e) {
            if (DBG) {
                log(e.toString());
            }
        } catch (SecurityException e2) {
            if (DBG) {
                log(e2.toString());
            }
        }
        if (DBG) {
            log("updateIccRecordInEf: " + iUpdateAdnRecordsInEfBySearchWithError);
        }
        return iUpdateAdnRecordsInEfBySearchWithError;
    }

    private int updateIccRecordInEfByIndex(int i, int i2, String str, String str2, String str3, int i3) throws RemoteException {
        if (DBG) {
            log("updateIccRecordInEfByIndex: efType=" + i + ", index=" + i2 + ", newname=" + str + ", newnumber=" + str2);
        }
        int iUpdateAdnRecordsInEfByIndexWithError = 0;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateAdnRecordsInEfByIndexWithError = iccPhbService.updateAdnRecordsInEfByIndexWithError(i3, i, str, str2, i2, str3);
            }
        } catch (RemoteException e) {
            log(e.toString());
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        log("updateIccRecordInEfByIndex: " + iUpdateAdnRecordsInEfByIndexWithError);
        return iUpdateAdnRecordsInEfByIndexWithError;
    }

    private int updateUsimRecordInEf(int i, String str, String str2, String str3, String str4, String str5, String str6, int i2) throws RemoteException {
        int i3;
        String str7;
        String str8;
        String str9;
        String str10;
        String str11;
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateUsimRecordInEf: efType=");
            i3 = i;
            sb.append(i3);
            sb.append(", oldname=");
            str7 = str;
            sb.append(str7);
            sb.append(", oldnumber=");
            str8 = str2;
            sb.append(str8);
            sb.append(", newname=");
            str9 = str3;
            sb.append(str9);
            sb.append(", newnumber=");
            str10 = str4;
            sb.append(str10);
            sb.append(", anr =");
            str11 = str5;
            sb.append(str11);
            sb.append(", emails=");
            sb.append(str6);
            log(sb.toString());
        } else {
            i3 = i;
            str7 = str;
            str8 = str2;
            str9 = str3;
            str10 = str4;
            str11 = str5;
        }
        String[] strArr = null;
        int iUpdateUsimPBRecordsInEfBySearchWithError = 0;
        if (str6 != null) {
            strArr = new String[]{str6};
        }
        String[] strArr2 = strArr;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateUsimPBRecordsInEfBySearchWithError = iccPhbService.updateUsimPBRecordsInEfBySearchWithError(i2, i3, str7, str8, "", null, null, str9, str10, str11, null, strArr2);
            }
        } catch (RemoteException e) {
            log(e.toString());
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        int i4 = iUpdateUsimPBRecordsInEfBySearchWithError;
        log("updateUsimRecordInEf: " + i4);
        return i4;
    }

    private int updateUsimRecordInEfByIndex(int i, int i2, String str, String str2, String str3, String str4, int i3) throws RemoteException {
        int i4;
        int i5;
        String str5;
        String str6;
        String str7;
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateUsimRecordInEfByIndex: efType=");
            i4 = i;
            sb.append(i4);
            sb.append(", Index=");
            i5 = i2;
            sb.append(i5);
            sb.append(", newname=");
            str5 = str;
            sb.append(str5);
            sb.append(", newnumber=");
            str6 = str2;
            sb.append(str6);
            sb.append(", anr =");
            str7 = str3;
            sb.append(str7);
            sb.append(", emails=");
            sb.append(str4);
            log(sb.toString());
        } else {
            i4 = i;
            i5 = i2;
            str5 = str;
            str6 = str2;
            str7 = str3;
        }
        String[] strArr = null;
        int iUpdateUsimPBRecordsInEfByIndexWithError = 0;
        if (str4 != null) {
            strArr = new String[]{str4};
        }
        String[] strArr2 = strArr;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateUsimPBRecordsInEfByIndexWithError = iccPhbService.updateUsimPBRecordsInEfByIndexWithError(i3, i4, str5, str6, str7, null, strArr2, i5);
            }
        } catch (RemoteException e) {
            log(e.toString());
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        log("updateUsimRecordInEfByIndex: " + iUpdateUsimPBRecordsInEfByIndexWithError);
        return iUpdateUsimPBRecordsInEfByIndexWithError;
    }

    private int deleteIccRecordFromEf(int i, String str, String str2, String[] strArr, String str3, int i2) throws RemoteException {
        if (DBG) {
            log("deleteIccRecordFromEf: efType=0x" + Integer.toHexString(i).toUpperCase() + ", name=" + str + ", number=" + str2 + ", pin2=" + str3 + ", subscription=" + i2);
        }
        int iUpdateAdnRecordsInEfBySearchWithError = 0;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateAdnRecordsInEfBySearchWithError = iccPhbService.updateAdnRecordsInEfBySearchWithError(i2, i, str, str2, "", "", str3);
            }
        } catch (RemoteException e) {
            if (DBG) {
                log(e.toString());
            }
        } catch (SecurityException e2) {
            if (DBG) {
                log(e2.toString());
            }
        }
        if (DBG) {
            log("deleteIccRecordFromEf: " + iUpdateAdnRecordsInEfBySearchWithError);
        }
        return iUpdateAdnRecordsInEfBySearchWithError;
    }

    private int deleteIccRecordFromEfByIndex(int i, int i2, String str, int i3) throws RemoteException {
        if (DBG) {
            log("deleteIccRecordFromEfByIndex: efType=" + i + ", index=" + i2 + ", pin2=" + str);
        }
        int iUpdateAdnRecordsInEfByIndexWithError = 0;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateAdnRecordsInEfByIndexWithError = iccPhbService.updateAdnRecordsInEfByIndexWithError(i3, i, "", "", i2, str);
            }
        } catch (RemoteException e) {
            log(e.toString());
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        log("deleteIccRecordFromEfByIndex: " + iUpdateAdnRecordsInEfByIndexWithError);
        return iUpdateAdnRecordsInEfByIndexWithError;
    }

    private int deleteUsimRecordFromEf(int i, String str, String str2, String[] strArr, int i2) throws RemoteException {
        int i3;
        String str3;
        String str4;
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("deleteUsimRecordFromEf: efType=");
            i3 = i;
            sb.append(i3);
            sb.append(", name=");
            str3 = str;
            sb.append(str3);
            sb.append(", number=");
            str4 = str2;
            sb.append(str4);
            log(sb.toString());
        } else {
            i3 = i;
            str3 = str;
            str4 = str2;
        }
        int iUpdateUsimPBRecordsInEfBySearchWithError = 0;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateUsimPBRecordsInEfBySearchWithError = iccPhbService.updateUsimPBRecordsInEfBySearchWithError(i2, i3, str3, str4, "", null, null, "", "", "", null, null);
            }
        } catch (RemoteException e) {
            log(e.toString());
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        int i4 = iUpdateUsimPBRecordsInEfBySearchWithError;
        log("deleteUsimRecordFromEf: " + i4);
        return i4;
    }

    private int deleteUsimRecordFromEfByIndex(int i, int i2, int i3) throws RemoteException {
        if (DBG) {
            log("deleteUsimRecordFromEfByIndex: efType=" + i + ", index=" + i2);
        }
        int iUpdateUsimPBRecordsInEfByIndexWithError = 0;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateUsimPBRecordsInEfByIndexWithError = iccPhbService.updateUsimPBRecordsInEfByIndexWithError(i3, i, "", "", "", null, null, i2);
            }
        } catch (RemoteException e) {
            log(e.toString());
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        log("deleteUsimRecordFromEfByIndex: " + iUpdateUsimPBRecordsInEfByIndexWithError);
        return iUpdateUsimPBRecordsInEfByIndexWithError;
    }

    private void loadRecord(MtkAdnRecord mtkAdnRecord, MatrixCursor matrixCursor, int i) {
        int length = ADDRESS_BOOK_COLUMN_NAMES.length;
        if (!mtkAdnRecord.isEmpty()) {
            Object[] objArr = new Object[length];
            String alphaTag = mtkAdnRecord.getAlphaTag();
            String number = mtkAdnRecord.getNumber();
            String[] emails = mtkAdnRecord.getEmails();
            String grpIds = mtkAdnRecord.getGrpIds();
            String string = Integer.toString(mtkAdnRecord.getRecId());
            if (length >= 8) {
                objArr[7] = Integer.valueOf(mtkAdnRecord.getAasIndex());
            }
            if (length >= 9) {
                objArr[8] = mtkAdnRecord.getSne();
            }
            if (DBG) {
                log("loadRecord: record:" + mtkAdnRecord);
            }
            objArr[0] = string;
            objArr[1] = alphaTag;
            objArr[2] = number;
            if (SystemProperties.get("ro.vendor.mtk_kor_customization").equals("1") && alphaTag.length() >= 2 && alphaTag.charAt(0) == 65278) {
                String str = "";
                try {
                    str = new String(alphaTag.substring(1).getBytes("utf-16be"), "KSC5601");
                } catch (UnsupportedEncodingException e) {
                    if (DBG) {
                        log("Implausible UnsupportedEncodingException : " + e);
                    }
                }
                int length2 = str.length();
                while (length2 > 0 && str.charAt(length2 - 1) == 63735) {
                    length2--;
                }
                objArr[1] = str.substring(0, length2);
                if (DBG) {
                    log("Decode ADN using KSC5601 : " + objArr[1]);
                }
            }
            if (emails != null) {
                StringBuilder sb = new StringBuilder();
                for (String str2 : emails) {
                    sb.append(str2);
                    sb.append(",");
                }
                objArr[3] = sb.toString();
            }
            objArr[4] = mtkAdnRecord.getAdditionalNumber();
            objArr[5] = grpIds;
            objArr[6] = Integer.valueOf(i);
            matrixCursor.addRow(objArr);
        }
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private void logi(String str) {
        Rlog.i(TAG, str);
    }

    private IMtkIccPhoneBook getIccPhbService() {
        return IMtkIccPhoneBook.Stub.asInterface(ServiceManager.getService("mtksimphonebook"));
    }

    private int getRequestSubId(Uri uri) {
        if (DBG) {
            log("getRequestSubId url: " + uri);
        }
        try {
            return Integer.parseInt(uri.getLastPathSegment());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    private int updateUsimPBRecordsBySearchWithError(int i, MtkAdnRecord mtkAdnRecord, MtkAdnRecord mtkAdnRecord2, int i2) throws RemoteException {
        if (DBG) {
            log("updateUsimPBRecordsBySearchWithError subId:" + i2 + ",oldAdn:" + mtkAdnRecord + ",newAdn:" + mtkAdnRecord2);
        }
        int iUpdateUsimPBRecordsBySearchWithError = 0;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateUsimPBRecordsBySearchWithError = iccPhbService.updateUsimPBRecordsBySearchWithError(i2, i, mtkAdnRecord, mtkAdnRecord2);
            }
        } catch (RemoteException e) {
            log(e.toString());
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        log("updateUsimPBRecordsBySearchWithError: " + iUpdateUsimPBRecordsBySearchWithError);
        return iUpdateUsimPBRecordsBySearchWithError;
    }

    private int updateUsimPBRecordsByIndexWithError(int i, MtkAdnRecord mtkAdnRecord, int i2, int i3) throws RemoteException {
        if (DBG) {
            log("updateUsimPBRecordsByIndexWithError subId:" + i3 + ",index:" + i2 + ",newAdn:" + mtkAdnRecord);
        }
        int iUpdateUsimPBRecordsByIndexWithError = 0;
        try {
            IMtkIccPhoneBook iccPhbService = getIccPhbService();
            if (iccPhbService != null) {
                iUpdateUsimPBRecordsByIndexWithError = iccPhbService.updateUsimPBRecordsByIndexWithError(i3, i, mtkAdnRecord, i2);
            }
        } catch (RemoteException e) {
            log(e.toString());
        } catch (SecurityException e2) {
            log(e2.toString());
        }
        log("updateUsimPBRecordsByIndexWithError: " + iUpdateUsimPBRecordsByIndexWithError);
        return iUpdateUsimPBRecordsByIndexWithError;
    }

    private void updatePhbStorageInfo(int i, int i2) {
        boolean zUpdatePhbStorageInfo = false;
        try {
            Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i2));
            if (phone != null) {
                if (!CsimPhbUtil.hasModemPhbEnhanceCapability(phone.getIccFileHandler())) {
                    zUpdatePhbStorageInfo = CsimPhbUtil.updatePhbStorageInfo(i);
                } else {
                    log("[updatePhbStorageInfo] is not a csim card");
                }
            }
        } catch (SecurityException e) {
            log(e.toString());
        }
        log("[updatePhbStorageInfo] res = " + zUpdatePhbStorageInfo);
    }
}
