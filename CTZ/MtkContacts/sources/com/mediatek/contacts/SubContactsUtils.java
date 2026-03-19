package com.mediatek.contacts;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.ContactsUtils;
import com.android.contacts.model.account.AccountWithDataSet;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Set;

public class SubContactsUtils extends ContactsUtils {
    public static long queryForRawContactId(ContentResolver contentResolver, long j) throws Throwable {
        long j2;
        Log.i("SubContactsUtils", "[queryForRawContactId]contactId:" + j);
        Cursor cursor = null;
        try {
            Cursor cursorQuery = contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"_id"}, "contact_id=" + j, null, null);
            if (cursorQuery != null) {
                try {
                    j2 = cursorQuery.moveToFirst() ? cursorQuery.getLong(0) : -1L;
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return j2;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public static Uri insertToDB(AccountWithDataSet accountWithDataSet, String str, String str2, String str3, String str4, ContentResolver contentResolver, long j, String str5, long j2, Set<Long> set) {
        ArrayList arrayList = new ArrayList();
        buildInsertOperation(arrayList, accountWithDataSet, str, str2, str3, str4, contentResolver, (int) j, str5, j2, set);
        return insertToDBApplyBatch(contentResolver, arrayList);
    }

    public static void buildInsertOperation(ArrayList<ContentProviderOperation> arrayList, AccountWithDataSet accountWithDataSet, String str, String str2, String str3, String str4, ContentResolver contentResolver, int i, String str5, long j, Set<Long> set) {
        Log.i("SubContactsUtils", "[buildInsertOperation]name:" + Log.anonymize(str) + ", number :" + Log.anonymize(str2) + ", email:" + Log.anonymize(str3) + ", additionalNumber = " + Log.anonymize(str4) + ", indicate:" + i + ", simType:" + str5 + ",indexInSim:" + j);
        if (arrayList == null) {
            Log.w("SubContactsUtils", "[buildInsertOperation]operationList is null!");
            return;
        }
        int size = arrayList.size();
        insertRawContacts(arrayList, accountWithDataSet, i, j);
        String str6 = "";
        if (!TextUtils.isEmpty(str)) {
            NamePhoneTypePair namePhoneTypePair = new NamePhoneTypePair(str);
            str = namePhoneTypePair.name;
            int i2 = namePhoneTypePair.phoneType;
            str6 = namePhoneTypePair.phoneTypeSuffix;
        }
        insertPhoneNumber(str2, arrayList, size, str6);
        insertName(arrayList, str, size);
        if ("USIM".equals(str5) || "CSIM".equals(str5)) {
            insertEmail(arrayList, str3, size);
            if (!GlobalEnv.getSimAasEditor().updateOperationList(accountWithDataSet, arrayList, size)) {
                Log.i("SubContactsUtils", "[buildInsertOperation]Plugin handled host do nothing");
                insertAdditionalNumber(arrayList, str4, size);
            }
            insertGroup(arrayList, set, size);
        }
    }

    public static class NamePhoneTypePair {
        public String name;
        public int phoneType;
        public String phoneTypeSuffix;

        public NamePhoneTypePair(String str) {
            int length = str.length();
            int i = length - 2;
            if (i >= 0 && str.charAt(i) == '/') {
                int i2 = length - 1;
                char upperCase = Character.toUpperCase(str.charAt(i2));
                this.phoneTypeSuffix = String.valueOf(str.charAt(i2));
                if (upperCase == 'W') {
                    this.phoneType = 3;
                } else if (upperCase == 'M' || upperCase == 'O') {
                    this.phoneType = 2;
                } else if (upperCase == 'H') {
                    this.phoneType = 1;
                } else {
                    this.phoneType = 7;
                }
                this.name = str.substring(0, i);
                return;
            }
            this.phoneTypeSuffix = "";
            this.phoneType = 7;
            this.name = str;
        }
    }

    public static void insertPhoneNumber(String str, ArrayList<ContentProviderOperation> arrayList, int i, String str2) {
        Log.i("SubContactsUtils", "[insertPhoneNumber]phoneTypeSuffix is " + Log.anonymize(str2) + ",backRef:" + i + ",number = " + Log.anonymize(str));
        if (!TextUtils.isEmpty(str)) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
            builderNewInsert.withValue("data1", str);
            builderNewInsert.withValue("data2", 102);
            if (!TextUtils.isEmpty(str2)) {
                builderNewInsert.withValue("data15", str2);
            }
            arrayList.add(builderNewInsert.build());
        }
    }

    public static void insertAdditionalNumber(ArrayList<ContentProviderOperation> arrayList, String str, int i) {
        Log.i("SubContactsUtils", "[insertAdditionalNumber]additionalNumber is " + Log.anonymize(str) + ",backRef:" + i);
        if (!TextUtils.isEmpty(str)) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
            builderNewInsert.withValue("data2", 7);
            builderNewInsert.withValue("data1", str);
            builderNewInsert.withValue("is_additional_number", 1);
            arrayList.add(builderNewInsert.build());
        }
    }

    public static void insertName(ArrayList<ContentProviderOperation> arrayList, String str, int i) {
        Log.i("SubContactsUtils", "[insertName]name is " + Log.anonymize(str) + ",backRef:" + i);
        if (!TextUtils.isEmpty(str)) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/name");
            builderNewInsert.withValue("data1", str);
            arrayList.add(builderNewInsert.build());
        }
    }

    public static void insertEmail(ArrayList<ContentProviderOperation> arrayList, String str, int i) {
        Log.i("SubContactsUtils", "[insertEmail]email is " + Log.anonymize(str) + ",backRef:" + i);
        if (!TextUtils.isEmpty(str)) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/email_v2");
            builderNewInsert.withValue("data1", str);
            arrayList.add(builderNewInsert.build());
        }
    }

    public static void insertGroup(ArrayList<ContentProviderOperation> arrayList, Set<Long> set, int i) {
        Log.i("SubContactsUtils", "[insertGroup]backRef:" + i);
        if (set != null && set.size() > 0) {
            for (Long l : (Long[]) set.toArray(new Long[0])) {
                ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                builderNewInsert.withValueBackReference("raw_contact_id", i);
                builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/group_membership");
                builderNewInsert.withValue("data1", l);
                arrayList.add(builderNewInsert.build());
            }
        }
    }

    public static void insertRawContacts(ArrayList<ContentProviderOperation> arrayList, AccountWithDataSet accountWithDataSet, int i, long j) {
        Log.i("SubContactsUtils", "[insertRawContacts]indexInSim:" + j);
        ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI);
        ContentValues contentValues = new ContentValues();
        contentValues.put("account_name", accountWithDataSet.name);
        contentValues.put("account_type", accountWithDataSet.type);
        contentValues.put("indicate_phone_or_sim_contact", Integer.valueOf(i));
        contentValues.put("aggregation_mode", (Integer) 3);
        contentValues.put("index_in_sim", Long.valueOf(j));
        builderNewInsert.withValues(contentValues);
        arrayList.add(builderNewInsert.build());
    }

    public static Uri insertToDBApplyBatch(ContentResolver contentResolver, ArrayList<ContentProviderOperation> arrayList) {
        Uri uri;
        Log.i("SubContactsUtils", "[insertToDBApplyBatch]..");
        try {
            uri = contentResolver.applyBatch("com.android.contacts", arrayList)[0].uri;
            try {
                Log.d("SubContactsUtils", "[insertToDBApplyBatch]rawContactUri:" + Log.anonymize(uri));
            } catch (OperationApplicationException e) {
                e = e;
                Log.e("SubContactsUtils", "[insertToDBApplyBatch]OperationApplicationException:" + e);
            } catch (RemoteException e2) {
                e = e2;
                Log.e("SubContactsUtils", "[insertToDBApplyBatch]RemoteException:" + e);
            }
        } catch (OperationApplicationException e3) {
            e = e3;
            uri = null;
        } catch (RemoteException e4) {
            e = e4;
            uri = null;
        }
        return uri;
    }
}
