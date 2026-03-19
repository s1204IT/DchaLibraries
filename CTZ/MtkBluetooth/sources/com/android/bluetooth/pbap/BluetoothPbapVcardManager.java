package com.android.bluetooth.pbap;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWindowAllocationException;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.bluetooth.R;
import com.android.bluetooth.util.DevicePolicyUtils;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;
import com.android.vcard.VCardConstants;
import com.android.vcard.VCardPhoneNumberTranslationCallback;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import javax.obex.Operation;
import javax.obex.ServerOperation;

public class BluetoothPbapVcardManager {
    static final String CALLLOG_SORT_ORDER = "_id DESC";
    private static final int CALLS_ID_COLUMN_INDEX = 3;
    private static final int CALLS_NAME_COLUMN_INDEX = 1;
    private static final int CALLS_NUMBER_COLUMN_INDEX = 0;
    private static final int CALLS_NUMBER_PRESENTATION_COLUMN_INDEX = 2;
    static final int CONTACTS_ID_COLUMN_INDEX = 0;
    static final int CONTACTS_NAME_COLUMN_INDEX = 1;
    private static final int ID_COLUMN_INDEX = 0;
    private static final int NEED_SEND_BODY = -1;
    private static final int PHONE_NUMBER_COLUMN_INDEX = 3;
    static final String SORT_ORDER_PHONE_NUMBER = "data1 ASC";
    private static final String TAG = "BluetoothPbapVcardManager";
    static long sLastFetchedTimeStamp;
    private Context mContext;
    private ContentResolver mResolver;
    private boolean mSearchCallLogOn = true;
    private static final boolean V = BluetoothPbapService.VERBOSE;
    private static final boolean isBuildTypeUser = BluetoothPbapService.USER_MODE;
    static final String[] PHONES_CONTACTS_PROJECTION = {"contact_id", "display_name"};
    static final String[] PHONE_LOOKUP_PROJECTION = {"_id", "display_name"};
    private static final String[] CALLLOG_PROJECTION = {"_id"};

    public BluetoothPbapVcardManager(Context context) {
        this.mContext = context;
        this.mResolver = this.mContext.getContentResolver();
        sLastFetchedTimeStamp = System.currentTimeMillis();
    }

    private String getOwnerPhoneNumberVcardFromProfile(boolean z, byte[] bArr) {
        int i;
        if (z) {
            i = VCardConfig.VCARD_TYPE_V21_GENERIC;
        } else {
            i = VCardConfig.VCARD_TYPE_V30_GENERIC;
        }
        if (!BluetoothPbapConfig.includePhotosInVcard()) {
            i |= 8388608;
        }
        return BluetoothPbapUtils.createProfileVCard(this.mContext, i, bArr);
    }

    public final String getOwnerPhoneNumberVcard(boolean z, byte[] bArr) {
        String ownerPhoneNumberVcardFromProfile;
        if (BluetoothPbapConfig.useProfileForOwnerVcard() && (ownerPhoneNumberVcardFromProfile = getOwnerPhoneNumberVcardFromProfile(z, bArr)) != null && ownerPhoneNumberVcardFromProfile.length() != 0) {
            return ownerPhoneNumberVcardFromProfile;
        }
        return new BluetoothPbapCallLogComposer(this.mContext).composeVCardForPhoneOwnNumber(2, BluetoothPbapService.getLocalPhoneName(), BluetoothPbapService.getLocalPhoneNum(), z);
    }

    public final int getPhonebookSize(int i) {
        int contactsSize;
        if (i == 1) {
            contactsSize = getContactsSize();
        } else {
            contactsSize = getCallHistorySize(i);
        }
        if (V) {
            Log.v(TAG, "getPhonebookSize size = " + contactsSize + " type = " + i);
        }
        return contactsSize;
    }

    public final int getContactsSize() throws Throwable {
        Cursor cursor = null;
        try {
            try {
                Cursor cursorQuery = this.mResolver.query(DevicePolicyUtils.getEnterprisePhoneUri(this.mContext), new String[]{"contact_id"}, null, null, "contact_id");
                if (cursorQuery == null) {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return 0;
                }
                try {
                    int distinctContactIdSize = getDistinctContactIdSize(cursorQuery) + 1;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return distinctContactIdSize;
                } catch (CursorWindowAllocationException e) {
                    cursor = cursorQuery;
                    Log.e(TAG, "CursorWindowAllocationException while getting Contacts size");
                    if (cursor != null) {
                        cursor.close();
                    }
                    return 0;
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            } catch (CursorWindowAllocationException e2) {
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public final int getCallHistorySize(int i) throws Throwable {
        Uri uri = CallLog.Calls.CONTENT_URI;
        String strCreateSelectionPara = BluetoothPbapObexServer.createSelectionPara(i);
        int count = 0;
        Cursor cursor = null;
        try {
            try {
                Cursor cursorQuery = this.mResolver.query(uri, null, strCreateSelectionPara, null, "date DESC");
                if (cursorQuery != null) {
                    try {
                        count = cursorQuery.getCount();
                    } catch (CursorWindowAllocationException e) {
                        cursor = cursorQuery;
                        Log.e(TAG, "CursorWindowAllocationException while getting CallHistory size");
                        if (cursor != null) {
                            cursor.close();
                        }
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
            } catch (CursorWindowAllocationException e2) {
            }
            return count;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public final SparseArray<String> loadCallHistoryList(int i, String str, String str2) {
        String str3;
        Cursor cursorQuery;
        Cursor cursorQuery2;
        String str4;
        String str5;
        Uri uri = CallLog.Calls.CONTENT_URI;
        String strCreateSelectionPara = BluetoothPbapObexServer.createSelectionPara(i);
        if (!this.mSearchCallLogOn || TextUtils.isEmpty(str)) {
            str3 = strCreateSelectionPara;
        } else {
            if (strCreateSelectionPara != null) {
                str4 = strCreateSelectionPara + " AND ";
            } else {
                str4 = new String();
            }
            if (str2.equals("name")) {
                str5 = str4 + "name LIKE '%" + str + "%'";
            } else {
                str5 = str4 + "number LIKE '%" + str + "%'";
            }
            str3 = str5;
        }
        String[] strArr = {"number", "name", "presentation", "_id"};
        SparseArray<String> sparseArray = new SparseArray<>();
        ArrayList arrayList = new ArrayList();
        Cursor cursor = null;
        try {
            cursorQuery2 = this.mResolver.query(uri, strArr, str3, null, CALLLOG_SORT_ORDER);
            try {
                cursorQuery = this.mResolver.query(uri, strArr, strCreateSelectionPara, null, CALLLOG_SORT_ORDER);
                if (cursorQuery != null) {
                    try {
                        cursorQuery.moveToFirst();
                        while (!cursorQuery.isAfterLast()) {
                            arrayList.add(Integer.valueOf(cursorQuery.getInt(3)));
                            cursorQuery.moveToNext();
                        }
                    } catch (CursorWindowAllocationException e) {
                        cursor = cursorQuery2;
                        try {
                            Log.e(TAG, "CursorWindowAllocationException while loading CallHistory");
                            if (cursor != null) {
                                cursor.close();
                            }
                            if (cursorQuery != null) {
                            }
                            return sparseArray;
                        } catch (Throwable th) {
                            th = th;
                            cursorQuery2 = cursor;
                            if (cursorQuery2 != null) {
                                cursorQuery2.close();
                            }
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (cursorQuery2 != null) {
                        }
                        if (cursorQuery != null) {
                        }
                        throw th;
                    }
                }
                if (cursorQuery2 != null) {
                    cursorQuery2.moveToFirst();
                    while (!cursorQuery2.isAfterLast()) {
                        String string = cursorQuery2.getString(1);
                        if (TextUtils.isEmpty(string)) {
                            string = cursorQuery2.getInt(2) != 1 ? this.mContext.getString(R.string.unknownNumber) : cursorQuery2.getString(0);
                        }
                        sparseArray.put(arrayList.indexOf(Integer.valueOf(cursorQuery2.getInt(3))) + 1, string);
                        cursorQuery2.moveToNext();
                    }
                }
                if (cursorQuery2 != null) {
                    cursorQuery2.close();
                }
            } catch (CursorWindowAllocationException e2) {
                cursorQuery = null;
            } catch (Throwable th3) {
                th = th3;
                cursorQuery = null;
            }
        } catch (CursorWindowAllocationException e3) {
            cursorQuery = null;
        } catch (Throwable th4) {
            th = th4;
            cursorQuery = null;
            cursorQuery2 = null;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return sparseArray;
    }

    public final ArrayList<String> getPhonebookNameList(int i) {
        ?? r2;
        ArrayList<String> arrayList = new ArrayList<>();
        ?? r22 = 0;
        r22 = 0;
        r22 = 0;
        r22 = 0;
        String profileName = BluetoothPbapConfig.useProfileForOwnerVcard() ? BluetoothPbapUtils.getProfileName(this.mContext) : null;
        if (profileName == null || profileName.length() == 0) {
            profileName = BluetoothPbapService.getLocalPhoneName();
        }
        arrayList.add(profileName);
        try {
            try {
                Cursor cursorQuery = this.mResolver.query(DevicePolicyUtils.getEnterprisePhoneUri(this.mContext), PHONES_CONTACTS_PROJECTION, null, null, i == 1 ? "display_name" : "contact_id");
                if (cursorQuery != null) {
                    try {
                        r22 = 17039374;
                        appendDistinctNameIdList(arrayList, this.mContext.getString(android.R.string.unknownName), cursorQuery);
                    } catch (CursorWindowAllocationException e) {
                        r22 = cursorQuery;
                        Log.e(TAG, "CursorWindowAllocationException while getting phonebook name list");
                        r22 = r22;
                        if (r22 != 0) {
                            r22.close();
                        }
                    } catch (Exception e2) {
                        r22 = cursorQuery;
                        e = e2;
                        Log.e(TAG, "Exception while getting phonebook name list", e);
                        if (r22 != 0) {
                            r22 = r22;
                            r22.close();
                        }
                    } catch (Throwable th) {
                        r2 = cursorQuery;
                        th = th;
                        if (r2 != 0) {
                            r2.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (CursorWindowAllocationException e3) {
            } catch (Exception e4) {
                e = e4;
            }
            return arrayList;
        } catch (Throwable th2) {
            th = th2;
            r2 = r22;
        }
    }

    final ArrayList<String> getSelectedPhonebookNameList(int i, boolean z, int i2, int i3, byte[] bArr, String str) {
        ?? Query;
        ArrayList<String> arrayList = new ArrayList<>();
        PropertySelector propertySelector = new PropertySelector(bArr);
        ?? r0 = 0;
        r0 = 0;
        r0 = 0;
        ?? CreateFilteredVCardComposer = BluetoothPbapUtils.createFilteredVCardComposer(this.mContext, z ? VCardConfig.VCARD_TYPE_V21_GENERIC : VCardConfig.VCARD_TYPE_V30_GENERIC, null);
        CreateFilteredVCardComposer.setPhoneNumberTranslationCallback(new VCardPhoneNumberTranslationCallback() {
            @Override
            public String onValueReceived(String str2, int i4, String str3, boolean z2) {
                return str2.replace(',', 'p').replace(';', 'w');
            }
        });
        String profileName = BluetoothPbapConfig.useProfileForOwnerVcard() ? BluetoothPbapUtils.getProfileName(this.mContext) : null;
        if (profileName == null || profileName.length() == 0) {
            profileName = BluetoothPbapService.getLocalPhoneName();
        }
        arrayList.add(profileName);
        try {
            try {
                Query = this.mResolver.query(DevicePolicyUtils.getEnterprisePhoneUri(this.mContext), PHONES_CONTACTS_PROJECTION, null, null, "contact_id");
                if (Query != 0) {
                    try {
                        if (!CreateFilteredVCardComposer.initWithCallback(Query, new EnterpriseRawContactEntitlesInfoCallback())) {
                            if (Query != 0) {
                                Query.close();
                            }
                            return arrayList;
                        }
                        while (true) {
                            boolean zIsAfterLast = CreateFilteredVCardComposer.isAfterLast();
                            if (!zIsAfterLast) {
                                String strCreateOneEntry = CreateFilteredVCardComposer.createOneEntry();
                                if (strCreateOneEntry == null) {
                                    Log.e(TAG, "Failed to read a contact. Error reason: " + CreateFilteredVCardComposer.getErrorReason());
                                    if (Query != 0) {
                                        Query.close();
                                    }
                                    return arrayList;
                                }
                                if (strCreateOneEntry.isEmpty()) {
                                    Log.i(TAG, "Contact may have been deleted during operation");
                                } else {
                                    if (V) {
                                        Log.v(TAG, "Checking selected bits in the vcard composer" + strCreateOneEntry);
                                    }
                                    if (propertySelector.checkVCardSelector(strCreateOneEntry, str)) {
                                        String name = propertySelector.getName(strCreateOneEntry);
                                        if (TextUtils.isEmpty(name)) {
                                            name = this.mContext.getString(android.R.string.unknownName);
                                        }
                                        arrayList.add(name);
                                    } else {
                                        Log.e(TAG, "vcard selector check fail");
                                    }
                                }
                            } else if (i == 0) {
                                r0 = zIsAfterLast;
                                if (V) {
                                    Log.v(TAG, "getPhonebookNameList, order by index");
                                    r0 = zIsAfterLast;
                                }
                            } else {
                                r0 = zIsAfterLast;
                                if (i == 1) {
                                    if (V) {
                                        Log.v(TAG, "getPhonebookNameList, order by alpha");
                                    }
                                    Collections.sort(arrayList);
                                    r0 = zIsAfterLast;
                                }
                            }
                        }
                    } catch (CursorWindowAllocationException e) {
                        r0 = Query;
                        Log.e(TAG, "CursorWindowAllocationException while getting Phonebook name list");
                        if (r0 != 0) {
                            r0.close();
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (Query != 0) {
                            Query.close();
                        }
                        throw th;
                    }
                }
                if (Query != 0) {
                    Query.close();
                }
            } catch (Throwable th2) {
                th = th2;
                Query = r0;
            }
        } catch (CursorWindowAllocationException e2) {
        }
        return arrayList;
    }

    public final ArrayList<String> getContactNamesByNumber(String str) {
        Uri uriWithAppendedPath;
        String[] strArr;
        Cursor cursorQuery;
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayList arrayList2 = new ArrayList();
        if (TextUtils.isEmpty(str)) {
            uriWithAppendedPath = DevicePolicyUtils.getEnterprisePhoneUri(this.mContext);
            strArr = PHONES_CONTACTS_PROJECTION;
        } else {
            uriWithAppendedPath = Uri.withAppendedPath(getPhoneLookupFilterUri(), Uri.encode(str));
            strArr = PHONE_LOOKUP_PROJECTION;
        }
        Uri uri = uriWithAppendedPath;
        Cursor cursor = null;
        try {
            try {
                cursorQuery = this.mResolver.query(uri, strArr, null, null, "contact_id");
                if (cursorQuery != null) {
                    try {
                        appendDistinctNameIdList(arrayList, this.mContext.getString(android.R.string.unknownName), cursorQuery);
                        if (V) {
                            Iterator<String> it = arrayList.iterator();
                            while (it.hasNext()) {
                                Log.v(TAG, "got name " + it.next() + " by number " + str);
                            }
                        }
                    } catch (CursorWindowAllocationException e) {
                        cursor = cursorQuery;
                        Log.e(TAG, "CursorWindowAllocationException while getting contact names");
                        if (cursor != null) {
                            cursor.close();
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (CursorWindowAllocationException e2) {
            }
            int size = arrayList2.size();
            for (int i = 0; i < size; i++) {
                String str2 = (String) arrayList2.get(i);
                if (!arrayList.contains(str2)) {
                    arrayList.add(str2);
                }
            }
            return arrayList;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = cursor;
        }
    }

    byte[] getCallHistoryPrimaryFolderVersion(int i) throws Throwable {
        long j;
        Uri uri = CallLog.Calls.CONTENT_URI;
        String str = BluetoothPbapObexServer.createSelectionPara(i) + " AND date >= " + sLastFetchedTimeStamp;
        Log.d(TAG, "LAST_FETCHED_TIME_STAMP is " + sLastFetchedTimeStamp);
        new ArrayList();
        Cursor cursor = null;
        try {
            try {
                Cursor cursorQuery = this.mResolver.query(uri, null, str, null, null);
                j = 0;
                while (cursorQuery != null) {
                    try {
                        if (!cursorQuery.moveToNext()) {
                            break;
                        }
                        j++;
                    } catch (Exception e) {
                        cursor = cursorQuery;
                        Log.e(TAG, "exception while fetching callHistory pvc");
                        if (cursor != null) {
                            cursor.close();
                        }
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
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Exception e2) {
            j = 0;
        }
        sLastFetchedTimeStamp = System.currentTimeMillis();
        Log.d(TAG, "getCallHistoryPrimaryFolderVersion count is " + j + " type is " + i);
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
        byteBufferAllocate.putLong(0L);
        Log.d(TAG, "primaryVersionCounter is " + BluetoothPbapUtils.sPrimaryVersionCounter);
        byteBufferAllocate.putLong(j);
        return byteBufferAllocate.array();
    }

    final int composeAndSendSelectedCallLogVcards(int i, Operation operation, int i2, int i3, boolean z, int i4, int i5, boolean z2, byte[] bArr, byte[] bArr2, String str, boolean z3) throws Throwable {
        Cursor cursorQuery;
        long j;
        String str2;
        if (i2 < 1 || i2 > i3) {
            Log.e(TAG, "internal error: startPoint or endPoint is not correct.");
            return 208;
        }
        String strCreateSelectionPara = BluetoothPbapObexServer.createSelectionPara(i);
        Cursor cursor = null;
        long j2 = 0;
        try {
            try {
                cursorQuery = this.mResolver.query(CallLog.Calls.CONTENT_URI, CALLLOG_PROJECTION, strCreateSelectionPara, null, CALLLOG_SORT_ORDER);
                if (cursorQuery != null) {
                    try {
                        try {
                            cursorQuery.moveToPosition(i2 - 1);
                            j = cursorQuery.getLong(0);
                        } catch (CursorWindowAllocationException e) {
                            cursor = cursorQuery;
                            j = 0;
                            Log.e(TAG, "CursorWindowAllocationException while composing calllog vcards");
                            if (cursor != null) {
                                cursor.close();
                            }
                            if (i2 != i3) {
                            }
                            if (strCreateSelectionPara != null) {
                            }
                            String str3 = str2;
                            if (V) {
                            }
                            return composeCallLogsAndSendSelectedVCards(operation, str3, z, i4, i5, null, z2, bArr, bArr2, str, z3);
                        }
                        try {
                            if (V) {
                                Log.v(TAG, "Call Log query startPointId = " + j);
                            }
                            if (i2 == i3) {
                                j2 = j;
                            } else {
                                cursorQuery.moveToPosition(i3 - 1);
                                j2 = cursorQuery.getLong(0);
                            }
                            if (V) {
                                Log.v(TAG, "Call log query endPointId = " + j2);
                            }
                        } catch (CursorWindowAllocationException e2) {
                            cursor = cursorQuery;
                            Log.e(TAG, "CursorWindowAllocationException while composing calllog vcards");
                            if (cursor != null) {
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                } else {
                    j = 0;
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (CursorWindowAllocationException e3) {
            }
            if (i2 != i3) {
                str2 = "_id=" + j;
            } else {
                str2 = "_id>=" + j2 + " AND _id<=" + j;
            }
            if (strCreateSelectionPara != null) {
                str2 = "(" + strCreateSelectionPara + ") AND (" + str2 + ")";
            }
            String str32 = str2;
            if (V) {
                Log.v(TAG, "Call log query selection is: " + str32);
            }
            return composeCallLogsAndSendSelectedVCards(operation, str32, z, i4, i5, null, z2, bArr, bArr2, str, z3);
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = cursor;
        }
    }

    final int composeAndSendPhonebookVcards(Operation operation, int i, int i2, boolean z, String str, int i3, int i4, boolean z2, byte[] bArr, byte[] bArr2, String str2, boolean z3) throws Throwable {
        if (i < 1 || i > i2) {
            Log.e(TAG, "internal error: startPoint or endPoint is not correct.");
            return 208;
        }
        Uri enterprisePhoneUri = DevicePolicyUtils.getEnterprisePhoneUri(this.mContext);
        Cursor cursor = null;
        Cursor matrixCursor = new MatrixCursor(new String[]{"contact_id"});
        try {
            try {
                Cursor cursorQuery = this.mResolver.query(enterprisePhoneUri, PHONES_CONTACTS_PROJECTION, null, null, "contact_id");
                if (cursorQuery != null) {
                    try {
                        matrixCursor = ContactCursorFilter.filterByRange(cursorQuery, i, i2);
                    } catch (CursorWindowAllocationException e) {
                        cursor = cursorQuery;
                        Log.e(TAG, "CursorWindowAllocationException while composing phonebook vcards");
                        if (cursor != null) {
                            cursor.close();
                        }
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
            } catch (CursorWindowAllocationException e2) {
            }
            Cursor cursor2 = matrixCursor;
            return z3 ? composeContactsAndSendSelectedVCards(operation, cursor2, z, str, i3, i4, z2, bArr, bArr2, str2) : composeContactsAndSendVCards(operation, cursor2, z, str, z2, bArr);
        } catch (Throwable th2) {
            th = th2;
        }
    }

    final int composeAndSendPhonebookOneVcard(Operation operation, int i, boolean z, String str, int i2, boolean z2, byte[] bArr) {
        if (i < 1) {
            Log.e(TAG, "Internal error: offset is not correct.");
            return 208;
        }
        Uri enterprisePhoneUri = DevicePolicyUtils.getEnterprisePhoneUri(this.mContext);
        Cursor matrixCursor = new MatrixCursor(new String[]{"contact_id"});
        try {
            try {
                Cursor cursorQuery = this.mResolver.query(enterprisePhoneUri, PHONES_CONTACTS_PROJECTION, null, null, i2 == 1 ? "display_name" : "contact_id");
                if (cursorQuery != null) {
                    Cursor cursorFilterByOffset = ContactCursorFilter.filterByOffset(cursorQuery, i);
                    cursorQuery.close();
                    matrixCursor = cursorFilterByOffset;
                }
            } catch (CursorWindowAllocationException e) {
                Log.e(TAG, "CursorWindowAllocationException while composing phonebook one vcard");
            }
            return composeContactsAndSendVCards(operation, matrixCursor, z, str, z2, bArr);
        } catch (Throwable th) {
            throw th;
        }
    }

    private static final class ContactCursorFilter {
        private ContactCursorFilter() {
        }

        public static Cursor filterByOffset(Cursor cursor, int i) {
            return filterByRange(cursor, i, i);
        }

        public static Cursor filterByRange(Cursor cursor, int i, int i2) {
            int columnIndex = cursor.getColumnIndex("contact_id");
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"contact_id"});
            long j = -1;
            int i3 = 1;
            while (cursor.moveToNext() && i3 <= i2) {
                long j2 = cursor.getLong(columnIndex);
                if (j != j2) {
                    if (i3 >= i) {
                        matrixCursor.addRow(new Long[]{Long.valueOf(j2)});
                        if (BluetoothPbapVcardManager.V) {
                            Log.v(BluetoothPbapVcardManager.TAG, "contactIdsCursor.addRow: " + j2);
                        }
                    }
                    i3++;
                    j = j2;
                }
            }
            return matrixCursor;
        }
    }

    private static class EnterpriseRawContactEntitlesInfoCallback implements VCardComposer.RawContactEntitlesInfoCallback {
        private EnterpriseRawContactEntitlesInfoCallback() {
        }

        @Override
        public VCardComposer.RawContactEntitlesInfo getRawContactEntitlesInfo(long j) {
            if (ContactsContract.Contacts.isEnterpriseContactId(j)) {
                return new VCardComposer.RawContactEntitlesInfo(ContactsContract.RawContactsEntity.CORP_CONTENT_URI, j - ContactsContract.Contacts.ENTERPRISE_CONTACT_ID_BASE);
            }
            return new VCardComposer.RawContactEntitlesInfo(ContactsContract.RawContactsEntity.CONTENT_URI, j);
        }
    }

    private int composeContactsAndSendVCards(Operation operation, Cursor cursor, boolean z, String str, boolean z2, byte[] bArr) throws Throwable {
        VCardComposer vCardComposerCreateFilteredVCardComposer;
        HandlerForStringBuffer handlerForStringBuffer;
        long jCurrentTimeMillis = V ? System.currentTimeMillis() : 0L;
        if (z2) {
            bArr = null;
        }
        VCardFilter vCardFilter = new VCardFilter(bArr);
        int i = z ? -671088640 : VCardConfig.VCARD_TYPE_V30_GENERIC;
        try {
            if (!vCardFilter.isPhotoEnabled()) {
                i |= 8388608;
            }
            vCardComposerCreateFilteredVCardComposer = BluetoothPbapUtils.createFilteredVCardComposer(this.mContext, i, null);
            try {
                vCardComposerCreateFilteredVCardComposer.setPhoneNumberTranslationCallback(new VCardPhoneNumberTranslationCallback() {
                    @Override
                    public String onValueReceived(String str2, int i2, String str3, boolean z3) {
                        return str2.replace(',', 'p').replace(';', 'w');
                    }
                });
                handlerForStringBuffer = new HandlerForStringBuffer(operation, str);
                try {
                    Log.v(TAG, "contactIdCursor size: " + cursor.getCount());
                    if (vCardComposerCreateFilteredVCardComposer.initWithCallback(cursor, new EnterpriseRawContactEntitlesInfoCallback()) && handlerForStringBuffer.onInit(this.mContext)) {
                        while (true) {
                            if (vCardComposerCreateFilteredVCardComposer.isAfterLast()) {
                                break;
                            }
                            if (BluetoothPbapObexServer.sIsAborted) {
                                ((ServerOperation) operation).isAborted = true;
                                BluetoothPbapObexServer.sIsAborted = false;
                                break;
                            }
                            String strCreateOneEntry = vCardComposerCreateFilteredVCardComposer.createOneEntry();
                            if (strCreateOneEntry == null) {
                                Log.e(TAG, "Failed to read a contact. Error reason: " + vCardComposerCreateFilteredVCardComposer.getErrorReason());
                                if (vCardComposerCreateFilteredVCardComposer != null) {
                                    vCardComposerCreateFilteredVCardComposer.terminate();
                                }
                                handlerForStringBuffer.onTerminate();
                                return 208;
                            }
                            if (strCreateOneEntry.isEmpty()) {
                                Log.i(TAG, "Contact may have been deleted during operation");
                            } else {
                                if (V && !isBuildTypeUser) {
                                    Log.v(TAG, "vCard from composer: " + strCreateOneEntry);
                                }
                                String strModifyNamePosition = modifyNamePosition(stripTelephoneNumber(vCardFilter.apply(strCreateOneEntry, z)));
                                if (V && !isBuildTypeUser) {
                                    Log.v(TAG, "vCard after cleanup: " + strModifyNamePosition);
                                }
                                if (!handlerForStringBuffer.onEntryCreated(strModifyNamePosition)) {
                                    if (vCardComposerCreateFilteredVCardComposer != null) {
                                        vCardComposerCreateFilteredVCardComposer.terminate();
                                    }
                                    handlerForStringBuffer.onTerminate();
                                    return 208;
                                }
                            }
                        }
                    }
                    if (vCardComposerCreateFilteredVCardComposer != null) {
                        vCardComposerCreateFilteredVCardComposer.terminate();
                    }
                    handlerForStringBuffer.onTerminate();
                    return 208;
                } catch (Throwable th) {
                    th = th;
                    if (vCardComposerCreateFilteredVCardComposer != null) {
                        vCardComposerCreateFilteredVCardComposer.terminate();
                    }
                    if (handlerForStringBuffer != null) {
                        handlerForStringBuffer.onTerminate();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                handlerForStringBuffer = null;
            }
        } catch (Throwable th3) {
            th = th3;
            vCardComposerCreateFilteredVCardComposer = null;
            handlerForStringBuffer = null;
        }
    }

    private int composeContactsAndSendSelectedVCards(Operation operation, Cursor cursor, boolean z, String str, int i, int i2, boolean z2, byte[] bArr, byte[] bArr2, String str2) throws Throwable {
        VCardComposer vCardComposerCreateFilteredVCardComposer;
        HandlerForStringBuffer handlerForStringBuffer;
        long jCurrentTimeMillis = V ? System.currentTimeMillis() : 0L;
        VCardFilter vCardFilter = new VCardFilter(z2 ? null : bArr);
        PropertySelector propertySelector = new PropertySelector(bArr2);
        int i3 = z ? VCardConfig.VCARD_TYPE_V21_GENERIC : VCardConfig.VCARD_TYPE_V30_GENERIC;
        try {
            if (!vCardFilter.isPhotoEnabled()) {
                i3 |= 8388608;
            }
            vCardComposerCreateFilteredVCardComposer = BluetoothPbapUtils.createFilteredVCardComposer(this.mContext, i3, null);
            try {
                vCardComposerCreateFilteredVCardComposer.setPhoneNumberTranslationCallback(new VCardPhoneNumberTranslationCallback() {
                    @Override
                    public String onValueReceived(String str3, int i4, String str4, boolean z3) {
                        return str3.replace(',', 'p').replace(';', 'w');
                    }
                });
                handlerForStringBuffer = new HandlerForStringBuffer(operation, str);
                try {
                    Log.v(TAG, "contactIdCursor size: " + cursor.getCount());
                    int i4 = 208;
                    if (vCardComposerCreateFilteredVCardComposer.initWithCallback(cursor, new EnterpriseRawContactEntitlesInfoCallback()) && handlerForStringBuffer.onInit(this.mContext)) {
                        int i5 = i2;
                        while (true) {
                            if (vCardComposerCreateFilteredVCardComposer.isAfterLast()) {
                                break;
                            }
                            if (BluetoothPbapObexServer.sIsAborted) {
                                ((ServerOperation) operation).isAborted = true;
                                BluetoothPbapObexServer.sIsAborted = false;
                                break;
                            }
                            String strCreateOneEntry = vCardComposerCreateFilteredVCardComposer.createOneEntry();
                            if (strCreateOneEntry == null) {
                                Log.e(TAG, "Failed to read a contact. Error reason: " + vCardComposerCreateFilteredVCardComposer.getErrorReason());
                                if (vCardComposerCreateFilteredVCardComposer != null) {
                                    vCardComposerCreateFilteredVCardComposer.terminate();
                                }
                                handlerForStringBuffer.onTerminate();
                                return i4;
                            }
                            if (strCreateOneEntry.isEmpty()) {
                                Log.i(TAG, "Contact may have been deleted during operation");
                            } else {
                                if (V) {
                                    Log.v(TAG, "Checking selected bits in the vcard composer" + strCreateOneEntry);
                                }
                                if (propertySelector.checkVCardSelector(strCreateOneEntry, str2)) {
                                    Log.e(TAG, "vcard selector check pass");
                                    if (i == -1) {
                                        String strModifyNamePosition = modifyNamePosition(stripTelephoneNumber(vCardFilter.apply(strCreateOneEntry, z)));
                                        if (V) {
                                            Log.v(TAG, "vCard after cleanup: " + strModifyNamePosition);
                                        }
                                        if (!handlerForStringBuffer.onEntryCreated(strModifyNamePosition)) {
                                            if (vCardComposerCreateFilteredVCardComposer != null) {
                                                vCardComposerCreateFilteredVCardComposer.terminate();
                                            }
                                            handlerForStringBuffer.onTerminate();
                                            return 208;
                                        }
                                    } else {
                                        continue;
                                    }
                                } else {
                                    Log.e(TAG, "vcard selector check fail");
                                    i5--;
                                }
                            }
                            i4 = 208;
                        }
                    }
                    if (vCardComposerCreateFilteredVCardComposer != null) {
                        vCardComposerCreateFilteredVCardComposer.terminate();
                    }
                    handlerForStringBuffer.onTerminate();
                    return 208;
                } catch (Throwable th) {
                    th = th;
                    if (vCardComposerCreateFilteredVCardComposer != null) {
                        vCardComposerCreateFilteredVCardComposer.terminate();
                    }
                    if (handlerForStringBuffer != null) {
                        handlerForStringBuffer.onTerminate();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                handlerForStringBuffer = null;
            }
        } catch (Throwable th3) {
            th = th3;
            vCardComposerCreateFilteredVCardComposer = null;
            handlerForStringBuffer = null;
        }
    }

    private int composeCallLogsAndSendSelectedVCards(Operation operation, String str, boolean z, int i, int i2, String str2, boolean z2, byte[] bArr, byte[] bArr2, String str3, boolean z3) throws Throwable {
        BluetoothPbapCallLogComposer bluetoothPbapCallLogComposer;
        HandlerForStringBuffer handlerForStringBuffer;
        VCardFilter vCardFilter;
        PropertySelector propertySelector;
        long jCurrentTimeMillis = V ? System.currentTimeMillis() : 0L;
        try {
            vCardFilter = new VCardFilter(z2 ? null : bArr);
            propertySelector = new PropertySelector(bArr2);
            bluetoothPbapCallLogComposer = new BluetoothPbapCallLogComposer(this.mContext);
            try {
                handlerForStringBuffer = new HandlerForStringBuffer(operation, str2);
            } catch (Throwable th) {
                th = th;
                handlerForStringBuffer = null;
            }
        } catch (Throwable th2) {
            th = th2;
            bluetoothPbapCallLogComposer = null;
            handlerForStringBuffer = null;
        }
        try {
            if (bluetoothPbapCallLogComposer.init(CallLog.Calls.CONTENT_URI, str, null, CALLLOG_SORT_ORDER) && handlerForStringBuffer.onInit(this.mContext)) {
                int i3 = i2;
                while (true) {
                    if (bluetoothPbapCallLogComposer.isAfterLast()) {
                        break;
                    }
                    if (BluetoothPbapObexServer.sIsAborted) {
                        ((ServerOperation) operation).isAborted = true;
                        BluetoothPbapObexServer.sIsAborted = false;
                        break;
                    }
                    String strCreateOneEntry = bluetoothPbapCallLogComposer.createOneEntry(z);
                    if (z3) {
                        if (!propertySelector.checkVCardSelector(strCreateOneEntry, str3)) {
                            Log.e(TAG, "Checking vcard selector for call log");
                            i3--;
                        } else if (i != -1) {
                            continue;
                        } else {
                            if (strCreateOneEntry == null) {
                                Log.e(TAG, "Failed to read a contact. Error reason: " + bluetoothPbapCallLogComposer.getErrorReason());
                                bluetoothPbapCallLogComposer.terminate();
                                handlerForStringBuffer.onTerminate();
                                return 208;
                            }
                            if (strCreateOneEntry.isEmpty()) {
                                Log.i(TAG, "Call Log may have been deleted during operation");
                            } else {
                                String strApply = vCardFilter.apply(strCreateOneEntry, z);
                                if (V) {
                                    Log.v(TAG, "Vcard Entry:");
                                    Log.v(TAG, strApply);
                                }
                                handlerForStringBuffer.onEntryCreated(strApply);
                            }
                        }
                    } else {
                        if (strCreateOneEntry == null) {
                            Log.e(TAG, "Failed to read a contact. Error reason: " + bluetoothPbapCallLogComposer.getErrorReason());
                            bluetoothPbapCallLogComposer.terminate();
                            handlerForStringBuffer.onTerminate();
                            return 208;
                        }
                        if (V) {
                            Log.v(TAG, "Vcard Entry:");
                            Log.v(TAG, strCreateOneEntry);
                        }
                        handlerForStringBuffer.onEntryCreated(strCreateOneEntry);
                    }
                }
            }
            bluetoothPbapCallLogComposer.terminate();
            handlerForStringBuffer.onTerminate();
            return 208;
        } catch (Throwable th3) {
            th = th3;
            if (bluetoothPbapCallLogComposer != null) {
                bluetoothPbapCallLogComposer.terminate();
            }
            if (handlerForStringBuffer != null) {
                handlerForStringBuffer.onTerminate();
            }
            throw th;
        }
    }

    public String stripTelephoneNumber(String str) {
        String[] strArrSplit = str.split(System.getProperty("line.separator"));
        String strConcat = "";
        for (int i = 0; i < strArrSplit.length; i++) {
            if (strArrSplit[i].startsWith(VCardConstants.PROPERTY_TEL)) {
                String[] strArrSplit2 = strArrSplit[i].split(":", 2);
                int length = strArrSplit2[1].length();
                strArrSplit2[1] = strArrSplit2[1].replace("-", "").replace("(", "").replace(")", "").replace(" ", "");
                if (strArrSplit2[1].length() < length) {
                    if (V && !isBuildTypeUser) {
                        Log.v(TAG, "Fixing vCard TEL to " + strArrSplit2[1]);
                    }
                    strArrSplit[i] = strArrSplit2[0] + ":" + strArrSplit2[1];
                }
            }
        }
        for (int i2 = 0; i2 < strArrSplit.length; i2++) {
            if (!strArrSplit[i2].isEmpty()) {
                strConcat = strConcat.concat(strArrSplit[i2] + "\n");
            }
        }
        if (V && !isBuildTypeUser) {
            Log.v(TAG, "vCard with stripped telephone no.: " + strConcat);
        }
        return strConcat;
    }

    private String modifyNamePosition(String str) {
        String[] strArrSplit = str.split(System.getProperty("line.separator"));
        String strConcat = "";
        String str2 = "";
        int i = 0;
        while (true) {
            if (i >= strArrSplit.length) {
                break;
            }
            if (strArrSplit[i].startsWith("VERSION:3.0")) {
                str2 = VCardConstants.VERSION_V30;
            } else if (strArrSplit[i].startsWith("VERSION:2.1")) {
                str2 = VCardConstants.VERSION_V21;
            }
            if (str2 == VCardConstants.VERSION_V30 && strArrSplit[i].trim().matches("N:[\\u4E00-\\u9FA5]+?;[\\u4E00-\\u9FA5]+?;[\\u4E00-\\u9FA5]+?;;")) {
                String[] strArrSplit2 = strArrSplit[i].split(";");
                if (strArrSplit2.length == 5) {
                    strArrSplit[i] = "".concat(strArrSplit2[0] + ";").concat(strArrSplit2[2] + strArrSplit2[1] + ";;").concat(strArrSplit2[3] + ";" + strArrSplit2[4]);
                }
            } else if (str2 != VCardConstants.VERSION_V21 || !strArrSplit[i].trim().matches("N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:[^;]+?;[^;]+?;[^;]+?;;")) {
                i++;
            } else {
                String[] strArrSplit3 = strArrSplit[i].split(";");
                if (strArrSplit3.length == 7) {
                    String strConcat2 = "".concat(strArrSplit3[0] + ";" + strArrSplit3[1] + ";" + strArrSplit3[2] + ";");
                    StringBuilder sb = new StringBuilder();
                    sb.append(strArrSplit3[4]);
                    sb.append(strArrSplit3[3]);
                    sb.append(";;");
                    strArrSplit[i] = strConcat2.concat(sb.toString()).concat(strArrSplit3[5] + ";" + strArrSplit3[6]);
                }
            }
        }
        for (int i2 = 0; i2 < strArrSplit.length; i2++) {
            if (!strArrSplit[i2].equals("")) {
                strConcat = strConcat.concat(strArrSplit[i2] + "\n");
            }
        }
        if (V && !isBuildTypeUser) {
            Log.v(TAG, "Vcard with name changed: " + strConcat);
        }
        return strConcat;
    }

    public class HandlerForStringBuffer {
        private Operation mOperation;
        private OutputStream mOutputStream;
        private String mPhoneOwnVCard;

        public HandlerForStringBuffer(Operation operation, String str) {
            this.mPhoneOwnVCard = null;
            this.mOperation = operation;
            if (str != null) {
                this.mPhoneOwnVCard = str;
                if (BluetoothPbapVcardManager.V) {
                    Log.v(BluetoothPbapVcardManager.TAG, "phone own number vcard:");
                }
                if (BluetoothPbapVcardManager.V && !BluetoothPbapVcardManager.isBuildTypeUser) {
                    Log.v(BluetoothPbapVcardManager.TAG, this.mPhoneOwnVCard);
                }
            }
        }

        private boolean write(String str) {
            if (str != null) {
                try {
                    this.mOutputStream.write(str.getBytes());
                    return true;
                } catch (IOException e) {
                    Log.e(BluetoothPbapVcardManager.TAG, "write outputstrem failed" + e.toString());
                    return false;
                }
            }
            return false;
        }

        public boolean onInit(Context context) {
            try {
                this.mOutputStream = this.mOperation.openOutputStream();
                if (this.mPhoneOwnVCard != null) {
                    return write(this.mPhoneOwnVCard);
                }
                return true;
            } catch (IOException e) {
                Log.e(BluetoothPbapVcardManager.TAG, "open outputstrem failed" + e.toString());
                return false;
            }
        }

        public boolean onEntryCreated(String str) {
            return write(str);
        }

        public void onTerminate() {
            if (!BluetoothPbapObexServer.closeStream(this.mOutputStream, this.mOperation)) {
                if (BluetoothPbapVcardManager.V) {
                    Log.v(BluetoothPbapVcardManager.TAG, "CloseStream failed!");
                }
            } else if (BluetoothPbapVcardManager.V) {
                Log.v(BluetoothPbapVcardManager.TAG, "CloseStream ok!");
            }
        }
    }

    public static class VCardFilter {
        private static final String SEPARATOR = System.getProperty("line.separator");
        private final byte[] mFilter;

        private enum FilterBit {
            FN(1, VCardConstants.PROPERTY_FN, true, false),
            PHOTO(3, VCardConstants.PROPERTY_PHOTO, false, false),
            BDAY(4, VCardConstants.PROPERTY_BDAY, false, false),
            ADR(5, VCardConstants.PROPERTY_ADR, false, false),
            EMAIL(8, "EMAIL", false, false),
            TITLE(12, VCardConstants.PROPERTY_TITLE, false, false),
            ORG(16, VCardConstants.PROPERTY_ORG, false, false),
            NOTE(17, VCardConstants.PROPERTY_NOTE, false, false),
            SOUND(19, VCardConstants.PROPERTY_SOUND, false, false),
            URL(20, VCardConstants.PROPERTY_URL, false, false),
            NICKNAME(23, VCardConstants.PROPERTY_NICKNAME, false, true),
            DATETIME(28, "X-IRMC-CALL-DATETIME", false, false);

            public final boolean excludeForV21;
            public final boolean onlyCheckV21;
            public final int pos;
            public final String prop;

            FilterBit(int i, String str, boolean z, boolean z2) {
                this.pos = i;
                this.prop = str;
                this.onlyCheckV21 = z;
                this.excludeForV21 = z2;
            }
        }

        private boolean isFilteredIn(FilterBit filterBit, boolean z) {
            int i = (filterBit.pos / 8) + 1;
            int i2 = filterBit.pos % 8;
            if (!z && filterBit.onlyCheckV21) {
                return true;
            }
            if (z && filterBit.excludeForV21) {
                return false;
            }
            return this.mFilter == null || i >= this.mFilter.length || ((this.mFilter[this.mFilter.length - i] >> i2) & 1) != 0;
        }

        VCardFilter(byte[] bArr) {
            this.mFilter = bArr;
        }

        public boolean isPhotoEnabled() {
            return isFilteredIn(FilterBit.PHOTO, false);
        }

        public String apply(String str, boolean z) {
            boolean zIsFilteredIn;
            if (this.mFilter == null) {
                return str;
            }
            String[] strArrSplit = str.split(SEPARATOR);
            StringBuilder sb = new StringBuilder();
            boolean z2 = false;
            for (String str2 : strArrSplit) {
                if (!Character.isWhitespace(str2.charAt(0)) && !str2.startsWith("=") && !str2.startsWith(";")) {
                    String str3 = str2.split("[;:]")[0];
                    FilterBit[] filterBitArrValues = FilterBit.values();
                    int length = filterBitArrValues.length;
                    int i = 0;
                    while (true) {
                        if (i < length) {
                            FilterBit filterBit = filterBitArrValues[i];
                            if (!filterBit.prop.equals(str3)) {
                                i++;
                            } else {
                                zIsFilteredIn = isFilteredIn(filterBit, z);
                                break;
                            }
                        } else {
                            zIsFilteredIn = true;
                            break;
                        }
                    }
                    if (str3.startsWith("X-")) {
                        z2 = str3.equals("X-IRMC-CALL-DATETIME");
                    } else {
                        z2 = zIsFilteredIn;
                    }
                }
                if (z2) {
                    sb.append(str2 + SEPARATOR);
                }
            }
            return sb.toString();
        }
    }

    private static class PropertySelector {
        private static final String SEPARATOR = System.getProperty("line.separator");
        private final byte[] mSelector;

        private enum PropertyMask {
            VERSION(0, VCardConstants.PROPERTY_VERSION),
            FN(1, VCardConstants.PROPERTY_FN),
            NAME(2, VCardConstants.PROPERTY_N),
            PHOTO(3, VCardConstants.PROPERTY_PHOTO),
            BDAY(4, VCardConstants.PROPERTY_BDAY),
            ADR(5, VCardConstants.PROPERTY_ADR),
            LABEL(6, "LABEL"),
            TEL(7, VCardConstants.PROPERTY_TEL),
            EMAIL(8, "EMAIL"),
            TITLE(12, VCardConstants.PROPERTY_TITLE),
            ORG(16, VCardConstants.PROPERTY_ORG),
            NOTE(17, VCardConstants.PROPERTY_NOTE),
            URL(20, VCardConstants.PROPERTY_URL),
            NICKNAME(23, VCardConstants.PROPERTY_NICKNAME),
            DATETIME(28, "DATETIME");

            public final int pos;
            public final String prop;

            PropertyMask(int i, String str) {
                this.pos = i;
                this.prop = str;
            }
        }

        PropertySelector(byte[] bArr) {
            this.mSelector = bArr;
        }

        private boolean checkbit(int i, byte[] bArr) {
            if (((bArr[(bArr.length - 1) - (i / 8)] >> (i % 8)) & 1) != 0) {
                return true;
            }
            return false;
        }

        private boolean checkprop(String str, String str2) {
            for (String str3 : str.split(SEPARATOR)) {
                if (!Character.isWhitespace(str3.charAt(0)) && !str3.startsWith("=") && str2.equals(str3.split("[;:]")[0])) {
                    Log.d(BluetoothPbapVcardManager.TAG, "bit.prop.equals current prop :" + str2);
                    return true;
                }
            }
            return false;
        }

        private boolean checkVCardSelector(String str, String str2) {
            boolean z = true;
            for (PropertyMask propertyMask : PropertyMask.values()) {
                if (checkbit(propertyMask.pos, this.mSelector)) {
                    Log.d(BluetoothPbapVcardManager.TAG, "checking for prop :" + propertyMask.prop);
                    if (str2.equals("0")) {
                        if (!checkprop(str, propertyMask.prop)) {
                            z = false;
                        } else {
                            Log.d(BluetoothPbapVcardManager.TAG, "bit.prop.equals current prop :" + propertyMask.prop);
                            return true;
                        }
                    } else if (!str2.equals("1")) {
                        continue;
                    } else if (checkprop(str, propertyMask.prop)) {
                        z = true;
                    } else {
                        Log.d(BluetoothPbapVcardManager.TAG, "bit.prop.notequals current prop" + propertyMask.prop);
                        return false;
                    }
                }
            }
            return z;
        }

        private String getName(String str) {
            String strSubstring = "";
            for (String str2 : str.split(SEPARATOR)) {
                if (!Character.isWhitespace(str2.charAt(0)) && !str2.startsWith("=") && str2.startsWith("N:")) {
                    strSubstring = str2.substring(str2.lastIndexOf(58), str2.length());
                }
            }
            Log.d(BluetoothPbapVcardManager.TAG, "returning name: " + strSubstring);
            return strSubstring;
        }
    }

    private static Uri getPhoneLookupFilterUri() {
        return ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI;
    }

    private static int getDistinctContactIdSize(Cursor cursor) {
        int columnIndex = cursor.getColumnIndex("contact_id");
        int columnIndex2 = cursor.getColumnIndex("_id");
        cursor.moveToPosition(-1);
        long j = -1;
        int i = 0;
        while (cursor.moveToNext()) {
            long j2 = cursor.getLong(columnIndex != -1 ? columnIndex : columnIndex2);
            if (j != j2) {
                i++;
                j = j2;
            }
        }
        if (V) {
            Log.i(TAG, "getDistinctContactIdSize result: " + i);
        }
        return i;
    }

    private static void appendDistinctNameIdList(ArrayList<String> arrayList, String str, Cursor cursor) {
        int columnIndex = cursor.getColumnIndex("contact_id");
        int columnIndex2 = cursor.getColumnIndex("_id");
        int columnIndex3 = cursor.getColumnIndex("display_name");
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            long j = cursor.getLong(columnIndex != -1 ? columnIndex : columnIndex2);
            String string = columnIndex3 != -1 ? cursor.getString(columnIndex3) : str;
            if (TextUtils.isEmpty(string)) {
                string = str;
            }
            String str2 = string + "," + j;
            if (!arrayList.contains(str2)) {
                arrayList.add(str2);
            }
        }
        if (V) {
            Iterator<String> it = arrayList.iterator();
            while (it.hasNext()) {
                Log.i(TAG, "appendDistinctNameIdList result: " + it.next());
            }
        }
    }
}
