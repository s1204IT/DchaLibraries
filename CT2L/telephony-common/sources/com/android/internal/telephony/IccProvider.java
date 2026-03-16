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
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.ITelephony;
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
    private static final int GRP = 8;
    private static final int GRP_SUB = 9;
    private static final int MAX_GROUP_SIZE_BYTES = 10;
    protected static final int SDN = 5;
    protected static final int SDN_SUB = 6;
    private static final String STR_EMAIL = "email";
    private static final String STR_NUMBER2 = "number2";
    protected static final String STR_PIN2 = "pin2";
    protected static final String STR_TAG = "tag";
    private static final String TAG = "IccProvider";
    private int[] mGrpNum;
    private SubscriptionManager mSubscriptionManager;
    protected static final String STR_NUMBER = "number";
    protected static final String STR_EMAILS = "emails";
    private static final String[] ADDRESS_BOOK_COLUMN_NAMES = {"name", STR_NUMBER, STR_EMAILS, "anrs", "snes", "grps", "_id"};
    private static final String[] ADDRESS_BOOK_GROUP_COLUMN_NAMES = {"index", "name"};
    private static final UriMatcher URL_MATCHER = new UriMatcher(-1);

    static {
        URL_MATCHER.addURI("icc", "adn", 1);
        URL_MATCHER.addURI("icc", "adn/subId/*", 2);
        URL_MATCHER.addURI("icc", "fdn", 3);
        URL_MATCHER.addURI("icc", "fdn/subId/*", 4);
        URL_MATCHER.addURI("icc", "sdn", 5);
        URL_MATCHER.addURI("icc", "sdn/subId/*", 6);
        URL_MATCHER.addURI("icc", "grp", 8);
        URL_MATCHER.addURI("icc", "grp/subId/*", 9);
    }

    @Override
    public boolean onCreate() {
        this.mSubscriptionManager = SubscriptionManager.from(getContext());
        TelephonyManager tm = (TelephonyManager) getContext().getSystemService("phone");
        int count = tm.getPhoneCount();
        this.mGrpNum = new int[count];
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection, String[] selectionArgs, String sort) {
        log("query");
        switch (URL_MATCHER.match(url)) {
            case 1:
                return loadFromEf(28474, SubscriptionManager.getDefaultSubId());
            case 2:
                return loadFromEf(28474, getRequestSubId(url));
            case 3:
                return loadFromEf(IccConstants.EF_FDN, SubscriptionManager.getDefaultSubId());
            case 4:
                return loadFromEf(IccConstants.EF_FDN, getRequestSubId(url));
            case 5:
                return loadFromEf(IccConstants.EF_SDN, SubscriptionManager.getDefaultSubId());
            case 6:
                return loadFromEf(IccConstants.EF_SDN, getRequestSubId(url));
            case 7:
                return loadAllSimContacts(28474);
            case 8:
                return loadGroupFromEf(IccConstants.EF_PBR, SubscriptionManager.getDefaultSubId());
            case 9:
                return loadGroupFromEf(IccConstants.EF_PBR, getRequestSubId(url));
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    private Cursor loadAllSimContacts(int efType) {
        Cursor[] result;
        List<SubscriptionInfo> subInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList == null || subInfoList.size() == 0) {
            result = new Cursor[0];
        } else {
            int subIdCount = subInfoList.size();
            result = new Cursor[subIdCount];
            for (int i = 0; i < subIdCount; i++) {
                int subId = subInfoList.get(i).getSubscriptionId();
                result[i] = loadFromEf(efType, subId);
                Rlog.i(TAG, "ADN Records loaded for Subscription ::" + subId);
            }
        }
        return new MergeCursor(result);
    }

    @Override
    public String getType(Uri url) {
        switch (URL_MATCHER.match(url)) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                return "vnd.android.cursor.dir/sim-contact";
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        int efType;
        int subId;
        String pin2 = null;
        log("insert");
        int match = URL_MATCHER.match(url);
        switch (match) {
            case 1:
                efType = 28474;
                subId = SubscriptionManager.getDefaultSubId();
                break;
            case 2:
                efType = 28474;
                subId = getRequestSubId(url);
                break;
            case 3:
                efType = IccConstants.EF_FDN;
                subId = SubscriptionManager.getDefaultSubId();
                pin2 = initialValues.getAsString(STR_PIN2);
                break;
            case 4:
                efType = IccConstants.EF_FDN;
                subId = getRequestSubId(url);
                pin2 = initialValues.getAsString(STR_PIN2);
                break;
            default:
                throw new UnsupportedOperationException("Cannot insert into URL: " + url);
        }
        String tag = initialValues.getAsString(STR_TAG);
        String number = initialValues.getAsString(STR_NUMBER);
        String email = initialValues.getAsString(STR_EMAILS);
        String[] emails = {email};
        String number2 = initialValues.getAsString(STR_NUMBER2);
        boolean success = addIccRecordToEf(efType, tag, number, emails, number2, pin2, subId);
        if (!success) {
            return null;
        }
        StringBuilder buf = new StringBuilder("content://icc/");
        switch (match) {
            case 1:
                buf.append("adn/");
                break;
            case 2:
                buf.append("adn/subId/");
                break;
            case 3:
                buf.append("fdn/");
                break;
            case 4:
                buf.append("fdn/subId/");
                break;
        }
        buf.append(0);
        Uri uri = Uri.parse(buf.toString());
        getContext().getContentResolver().notifyChange(url, null);
        return uri;
    }

    private String normalizeValue(String inVal) {
        int len = inVal.length();
        if (len == 0) {
            log("len of input String is 0");
            return inVal;
        }
        String retVal = inVal;
        if (inVal.charAt(0) == '\'' && inVal.charAt(len - 1) == '\'') {
            retVal = inVal.substring(1, len - 1);
        }
        return retVal;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        int efType;
        int subId;
        int match = URL_MATCHER.match(url);
        switch (match) {
            case 1:
                efType = 28474;
                subId = SubscriptionManager.getDefaultSubId();
                break;
            case 2:
                efType = 28474;
                subId = getRequestSubId(url);
                break;
            case 3:
                efType = IccConstants.EF_FDN;
                subId = SubscriptionManager.getDefaultSubId();
                break;
            case 4:
                efType = IccConstants.EF_FDN;
                subId = getRequestSubId(url);
                break;
            default:
                throw new UnsupportedOperationException("Cannot insert into URL: " + url);
        }
        log("delete");
        String tag = null;
        String number = null;
        String email = null;
        String[] emails = null;
        String number2 = null;
        String pin2 = null;
        String[] tokens = where.split("AND");
        int n = tokens.length;
        while (true) {
            n--;
            if (n >= 0) {
                String param = tokens[n];
                log("parsing '" + param + "'");
                String[] pair = param.split("=");
                if (pair.length != 2) {
                    Rlog.e(TAG, "resolve: bad whereClause parameter: " + param);
                } else {
                    String key = pair[0].trim();
                    String val = pair[1].trim();
                    if (STR_TAG.equals(key)) {
                        tag = normalizeValue(val);
                    } else if (STR_NUMBER.equals(key)) {
                        number = normalizeValue(val);
                    } else if (STR_EMAIL.equals(key)) {
                        email = normalizeValue(val);
                    } else if (STR_NUMBER2.equals(key)) {
                        number2 = normalizeValue(val);
                    } else if (STR_PIN2.equals(key)) {
                        pin2 = normalizeValue(val);
                    }
                }
            } else {
                if (email != null) {
                    emails = new String[]{email};
                }
                if (TextUtils.isEmpty(number)) {
                    number = "";
                }
                if (efType == 3 && TextUtils.isEmpty(pin2)) {
                    return 0;
                }
                boolean success = deleteIccRecordFromEf(efType, tag, number, emails, number2, pin2, subId);
                if (!success) {
                    return 0;
                }
                getContext().getContentResolver().notifyChange(url, null);
                return 1;
            }
        }
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        int efType;
        int subId;
        String pin2 = null;
        log("update");
        int match = URL_MATCHER.match(url);
        switch (match) {
            case 1:
                efType = 28474;
                subId = SubscriptionManager.getDefaultSubId();
                break;
            case 2:
                efType = 28474;
                subId = getRequestSubId(url);
                break;
            case 3:
                efType = IccConstants.EF_FDN;
                subId = SubscriptionManager.getDefaultSubId();
                pin2 = values.getAsString(STR_PIN2);
                break;
            case 4:
                efType = IccConstants.EF_FDN;
                subId = getRequestSubId(url);
                pin2 = values.getAsString(STR_PIN2);
                break;
            case 5:
            case 6:
            case 7:
            default:
                throw new UnsupportedOperationException("Cannot insert into URL: " + url);
            case 8:
                int subId2 = SubscriptionManager.getDefaultSubId();
                String grpTag = values.getAsString("grpTag");
                String grpId = values.getAsString("grpId");
                boolean success = updateIccRecordGrpTagInEf(28474, grpId, grpTag, null, subId2);
                if (!success) {
                    return 0;
                }
                return 1;
            case 9:
                int subId3 = getRequestSubId(url);
                String grpTag2 = values.getAsString("grpTag");
                String grpId2 = values.getAsString("grpId");
                boolean success2 = updateIccRecordGrpTagInEf(28474, grpId2, grpTag2, null, subId3);
                if (!success2) {
                    return 0;
                }
                return 1;
        }
        int simId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(simId)) {
            log("Invalid phone ID: " + simId);
            return 0;
        }
        int grpNum = this.mGrpNum[simId];
        if (efType == 28474) {
            log(" grpNum = " + grpNum);
            String tag = values.getAsString(STR_TAG);
            String number = values.getAsString(STR_NUMBER);
            String email = values.getAsString(STR_EMAILS);
            String number2 = values.getAsString(STR_NUMBER2);
            String sne = values.getAsString("sne");
            String group = values.getAsString("grps");
            String[] emails = null;
            log("  email = " + email);
            if (email != null && (emails = email.split(",", 2)) != null) {
                for (String el : emails) {
                    log("  eml = " + el);
                }
            }
            String[] grps = null;
            byte[] oldGs = null;
            if (group != null) {
                grps = group.split(",");
                if (grpNum != 0) {
                    oldGs = new byte[grpNum];
                    for (int i = 0; i < grpNum; i++) {
                        oldGs[i] = 0;
                    }
                    int index = 0;
                    if (grps != null) {
                        int len$ = grps.length;
                        int i$ = 0;
                        while (true) {
                            int index2 = index;
                            if (i$ < len$) {
                                String grp = grps[i$];
                                log(" grp = " + grp);
                                if (grp.equals("0")) {
                                    index = index2;
                                } else if (grp.isEmpty()) {
                                    index = index2;
                                } else {
                                    index = index2 + 1;
                                    oldGs[index2] = (byte) (grp.charAt(0) - '0');
                                }
                                i$++;
                            }
                        }
                    }
                }
            }
            String newTag = values.getAsString("newTag");
            String newNumber = values.getAsString("newNumber");
            String newEmail = values.getAsString("newEmails");
            String newNumber2 = values.getAsString("newNumber2");
            String newSne = values.getAsString("newSne");
            String newGroup = values.getAsString("newGrps");
            String[] newEmails = null;
            log("  newEmail = " + newEmail);
            if (newEmail != null && (newEmails = newEmail.split(",", 2)) != null) {
                for (String el2 : newEmails) {
                    log("  new eml = " + el2);
                }
            }
            byte[] newGs = null;
            if (newGroup != null) {
                String[] newGrps = newGroup.split(",");
                if (grpNum != 0) {
                    newGs = new byte[grpNum];
                    for (int i2 = 0; i2 < grpNum; i2++) {
                        newGs[i2] = 0;
                    }
                    int index3 = 0;
                    if (grps != null) {
                        int len$2 = newGrps.length;
                        int i$2 = 0;
                        while (true) {
                            int index4 = index3;
                            if (i$2 < len$2) {
                                String newGrp = newGrps[i$2];
                                log(" newGrp = " + newGrp);
                                if (newGrp.equals("0")) {
                                    index3 = index4;
                                } else if (newGrp.isEmpty()) {
                                    index3 = index4;
                                } else {
                                    index3 = index4 + 1;
                                    newGs[index4] = (byte) (newGrp.charAt(0) - '0');
                                }
                                i$2++;
                            }
                        }
                    }
                }
            }
            boolean success3 = updateIccRecordInEf(efType, tag, number, emails, number2, sne, oldGs, newTag, newNumber, newEmails, newNumber2, newSne, newGs, pin2, subId);
            if (!success3) {
                return 0;
            }
            return 1;
        }
        if (efType == 28475 && !TextUtils.isEmpty(pin2)) {
            String tag2 = values.getAsString(STR_TAG);
            String number3 = values.getAsString(STR_NUMBER);
            String newTag2 = values.getAsString("newTag");
            String newNumber3 = values.getAsString("newNumber");
            boolean success4 = updateIccRecordInEf(efType, tag2, number3, null, null, null, null, newTag2, newNumber3, null, null, null, null, pin2, subId);
            if (!success4) {
                return 0;
            }
            return 1;
        }
        return 0;
    }

    private MatrixCursor loadFromEf(int efType, int subId) {
        log("loadFromEf: efType=" + efType + ", subscription=" + subId);
        List<AdnRecord> adnRecords = null;
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                adnRecords = iccIpb.getAdnRecordsInEfForSubscriber(subId, efType);
            }
        } catch (RemoteException e) {
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        if (adnRecords != null) {
            int N = adnRecords.size();
            MatrixCursor cursor = new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES, N);
            log("adnRecords.size=" + N);
            for (int i = 0; i < N; i++) {
                loadRecord(adnRecords.get(i), cursor, i);
            }
            return cursor;
        }
        Rlog.w(TAG, "Cannot load ADN records");
        return new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES);
    }

    private MatrixCursor loadGroupFromEf(int efType, int subId) {
        log("loadGroupFromEf: efType=" + efType + ", subscription=" + subId);
        int simId = SubscriptionManager.getPhoneId(subId);
        String[] grpRecords = null;
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                grpRecords = iccIpb.getGrpRecordsUsingSubId(subId, efType);
            }
        } catch (RemoteException e) {
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        if (grpRecords != null && SubscriptionManager.isValidPhoneId(simId)) {
            int N = grpRecords.length;
            this.mGrpNum[simId] = N;
            MatrixCursor cursor = new MatrixCursor(ADDRESS_BOOK_GROUP_COLUMN_NAMES, N);
            log("grpRecords.size= " + N);
            for (int i = 1; i <= N; i++) {
                loadGroups(grpRecords[i - 1], cursor, i);
            }
            return cursor;
        }
        Rlog.w(TAG, "Cannot load GRP records: " + simId);
        return new MatrixCursor(ADDRESS_BOOK_GROUP_COLUMN_NAMES);
    }

    private boolean addIccRecordToEf(int efType, String name, String number, String[] emails, String number2, String pin2, int subId) {
        log("addIccRecordToEf: efType=" + efType + ", name=" + name + ", number=" + number + ", emails=" + emails + ", number2=" + number2 + ", subscription=" + subId);
        boolean success = false;
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfBySearch2UsingSubId(subId, efType, "", "", new String[]{""}, "", "", null, name, number, emails, number2, "", null, pin2);
            }
        } catch (RemoteException e) {
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("addIccRecordToEf: " + success);
        return success;
    }

    private boolean updateIccRecordInEf(int efType, String oldName, String oldNumber, String[] oldEmails, String oldNumber2, String oldSne, byte[] oldGrps, String newName, String newNumber, String[] newEmails, String newNumber2, String newSne, byte[] newGrps, String pin2, int subId) {
        log("updateIccRecordInEf: efType=" + efType + ", oldname=" + oldName + ", oldnumber=" + oldNumber + ", oldEmail =" + oldEmails + ", oldNumber2=" + oldNumber2 + ", oldSne=" + oldSne + ", oldGrps=" + oldGrps + ", newname=" + newName + ", newnumber=" + newNumber + ", newEmail =" + newEmails + ", newNumber2=" + newNumber2 + ", newSne=" + newSne + ", newGrps=" + newGrps + ", subscription=" + subId);
        boolean success = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (2 == phone.getUiccAppType(subId)) {
                if (iccIpb != null) {
                    success = iccIpb.updateAdnRecordsInEfBySearch2UsingSubId(subId, efType, oldName, oldNumber, oldEmails, oldNumber2, oldSne, oldGrps, newName, newNumber, newEmails, newNumber2, newSne, newGrps, pin2);
                }
            } else if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsInEfBySearchForSubscriber(subId, efType, oldName, oldNumber, newName, newNumber, pin2);
            }
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("updateIccRecordInEf: " + success);
        return success;
    }

    private boolean updateIccRecordGrpInEf(int efType, String name, String number, String[] emails, String number2, String oldGrps, String addtogroup, String pin2, int subId) {
        log("updateIccRecordGrpInEf: efType=" + efType + ", name=" + name + ", number=" + number + ", emails=" + emails + ", number2=" + number2 + ", oldGrps = [" + oldGrps + "] , addtogroup=" + addtogroup + ", pin2=" + pin2 + ", subscription=" + subId);
        boolean success = false;
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                String[] strArr = new String[10];
                String[] oldGroups = oldGrps.split(",", 10);
                byte[] grps = new byte[10];
                int i = 0;
                for (String oldgrpString : oldGroups) {
                    if (!oldgrpString.isEmpty()) {
                        grps[i] = (byte) (oldgrpString.charAt(0) - '0');
                        log("grps[i] = " + i + "," + ((int) grps[i]));
                        i++;
                    }
                }
                byte addgrp = (byte) (addtogroup.charAt(0) - '0');
                log("addgrp = " + ((int) addgrp));
                success = iccIpb.updateAdnRecordsGrpInEfBySearchUsingSubId(subId, efType, name, number, emails[0], number2, grps, addgrp, pin2);
            }
        } catch (RemoteException e) {
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("updateIccRecordGrpInEf: " + success);
        return success;
    }

    private boolean updateIccRecordGrpTagInEf(int efType, String grpId, String grpTag, String pin2, int subId) {
        log("updateIccRecordGrpTagInEf: efType=" + efType + ", grpId=" + grpId + ", grpTag=" + grpTag + ", subscription=" + subId);
        boolean success = false;
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                success = iccIpb.updateAdnRecordsGrpTagInEfByIndexUsingSubId(subId, efType, grpId, grpTag, pin2);
            }
        } catch (RemoteException e) {
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("updateIccRecordGrpTagInEf: " + success);
        return success;
    }

    private boolean deleteIccRecordFromEf(int efType, String name, String number, String[] emails, String number2, String pin2, int subId) {
        log("deleteIccRecordFromEf: efType=" + efType + ", name=" + name + ", number=" + number + ", emails=" + emails + ", number2=" + number2 + ", pin2=" + pin2 + ", subscription=" + subId);
        boolean success = false;
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                if (emails != null) {
                    success = iccIpb.updateAdnRecordsInEfBySearch2UsingSubId(subId, efType, name, number, emails, number2, "", null, "", "", new String[]{""}, "", "", null, pin2);
                } else {
                    success = iccIpb.updateAdnRecordsInEfBySearch2UsingSubId(subId, efType, name, number, null, number2, "", null, "", "", new String[]{""}, "", "", null, pin2);
                }
            }
        } catch (RemoteException e) {
        } catch (SecurityException ex) {
            log(ex.toString());
        }
        log("deleteIccRecordFromEf: " + success);
        return success;
    }

    private void loadRecord(AdnRecord record, MatrixCursor cursor, int id) {
        if (!record.isEmpty()) {
            Object[] contact = new Object[ADDRESS_BOOK_COLUMN_NAMES.length];
            String alphaTag = record.getAlphaTag();
            String number = record.getNumber();
            log("loadRecord: " + alphaTag + ", " + number + ",");
            contact[0] = alphaTag;
            contact[1] = number;
            String[] emails = record.getEmails();
            if (emails != null) {
                StringBuilder emailString = new StringBuilder();
                for (String email : emails) {
                    log("Adding email:" + email);
                    if (!email.isEmpty()) {
                        emailString.append(email);
                        emailString.append(",");
                    }
                }
                contact[2] = emailString.toString();
            }
            String[] anrs = record.getAnrs();
            if (anrs != null) {
                StringBuilder anrString = new StringBuilder();
                for (String anr : anrs) {
                    log("Adding anr:" + anr);
                    if (!anr.isEmpty()) {
                        anrString.append(anr);
                        anrString.append(",");
                    }
                }
                contact[3] = anrString.toString();
            }
            String[] snes = record.getSnes();
            if (snes != null) {
                StringBuilder sneString = new StringBuilder();
                for (String sne : snes) {
                    log("Adding anr:" + sne);
                    if (!sne.isEmpty()) {
                        sneString.append(sne);
                        sneString.append(",");
                    }
                }
                contact[4] = sneString.toString();
            }
            byte[] grps = record.getGrps();
            if (grps != null) {
                StringBuilder grpString = new StringBuilder();
                for (byte grp : grps) {
                    if (grp != 0) {
                        log("Adding grp");
                        if (grpString.indexOf(String.valueOf((int) grp)) == -1) {
                            log("Adding grp:" + ((int) grp));
                            grpString.append((int) grp);
                            grpString.append(",");
                        }
                    }
                }
                contact[5] = grpString.toString();
            }
            contact[6] = Integer.valueOf(id);
            cursor.addRow(contact);
        }
    }

    private void loadGroups(String grp, MatrixCursor cursor, int id) {
        if (grp != null) {
            log("loadGroups: " + id + ", [" + grp + "]");
            Object[] groups = {Integer.valueOf(id), grp};
            cursor.addRow(groups);
        }
    }

    private void log(String msg) {
        Rlog.d(TAG, "[IccProvider] " + msg);
    }

    private int getRequestSubId(Uri url) {
        log("getRequestSubId url: " + url);
        try {
            return Integer.parseInt(url.getLastPathSegment());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unknown URL " + url);
        }
    }
}
