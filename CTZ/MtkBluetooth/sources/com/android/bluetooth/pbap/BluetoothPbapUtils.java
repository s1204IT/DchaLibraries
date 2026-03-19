package com.android.bluetooth.pbap;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import com.android.bluetooth.opp.BluetoothShare;
import com.android.vcard.VCardComposer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

class BluetoothPbapUtils {
    private static final int FILTER_PHOTO = 3;
    private static final long QUERY_CONTACT_RETRY_INTERVAL = 4000;
    private static final String TAG = "BluetoothPbapUtils";
    private static final String TYPE_ADDRESS = "address";
    private static final String TYPE_EMAIL = "email";
    private static final String TYPE_NAME = "name";
    private static final String TYPE_PHONE = "phone";
    private static final boolean V = BluetoothPbapService.VERBOSE;
    static AtomicLong sDbIdentifier = new AtomicLong();
    static long sPrimaryVersionCounter = 0;
    static long sSecondaryVersionCounter = 0;
    private static long sTotalContacts = 0;
    private static long sTotalFields = 0;
    private static long sTotalSvcFields = 0;
    private static long sContactsLastUpdated = 0;
    private static HashMap<String, ContactData> sContactDataset = new HashMap<>();
    private static HashSet<String> sContactSet = new HashSet<>();

    BluetoothPbapUtils() {
    }

    private static class ContactData {
        private ArrayList<String> mAddress;
        private ArrayList<String> mEmail;
        private String mName;
        private ArrayList<String> mPhone;

        ContactData() {
            this.mPhone = new ArrayList<>();
            this.mEmail = new ArrayList<>();
            this.mAddress = new ArrayList<>();
        }

        ContactData(String str, ArrayList<String> arrayList, ArrayList<String> arrayList2, ArrayList<String> arrayList3) {
            this.mName = str;
            this.mPhone = arrayList;
            this.mEmail = arrayList2;
            this.mAddress = arrayList3;
        }
    }

    private static boolean hasFilter(byte[] bArr) {
        return bArr != null && bArr.length > 0;
    }

    private static boolean isFilterBitSet(byte[] bArr, int i) {
        if (hasFilter(bArr)) {
            int i2 = 7 - (i / 8);
            return i2 < bArr.length && (bArr[i2] & (1 << (i % 8))) > 0;
        }
        return false;
    }

    static VCardComposer createFilteredVCardComposer(Context context, int i, byte[] bArr) {
        boolean z;
        if (!BluetoothPbapConfig.includePhotosInVcard() || (hasFilter(bArr) && !isFilterBitSet(bArr, 3))) {
            z = false;
        } else {
            z = true;
        }
        if (!z) {
            if (V) {
                Log.v(TAG, "Excluding images from VCardComposer...");
            }
            i |= 8388608;
        }
        return new VCardComposer(context, i, true);
    }

    public static String getProfileName(Context context) {
        String string;
        Cursor cursorQuery = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, new String[]{"display_name"}, null, null, null);
        if (cursorQuery != null && cursorQuery.moveToFirst()) {
            string = cursorQuery.getString(0);
        } else {
            string = null;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return string;
    }

    static String createProfileVCard(Context context, int i, byte[] bArr) {
        VCardComposer vCardComposerCreateFilteredVCardComposer;
        String strCreateOneEntry = null;
        try {
            vCardComposerCreateFilteredVCardComposer = createFilteredVCardComposer(context, i, bArr);
            try {
                if (vCardComposerCreateFilteredVCardComposer.init(ContactsContract.Profile.CONTENT_URI, null, null, null, null, Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.RawContactsEntity.CONTENT_URI.getLastPathSegment()))) {
                    strCreateOneEntry = vCardComposerCreateFilteredVCardComposer.createOneEntry();
                } else {
                    Log.e(TAG, "Unable to create profile vcard. Error initializing composer: " + vCardComposerCreateFilteredVCardComposer.getErrorReason());
                }
            } catch (Throwable th) {
                th = th;
                Log.e(TAG, "Unable to create profile vcard.", th);
            }
        } catch (Throwable th2) {
            th = th2;
            vCardComposerCreateFilteredVCardComposer = null;
        }
        if (vCardComposerCreateFilteredVCardComposer != null) {
            vCardComposerCreateFilteredVCardComposer.terminate();
        }
        return strCreateOneEntry;
    }

    static void savePbapParams(Context context) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long j = sDbIdentifier.get();
        SharedPreferences.Editor editorEdit = defaultSharedPreferences.edit();
        editorEdit.putLong("primary", sPrimaryVersionCounter);
        editorEdit.putLong("secondary", sSecondaryVersionCounter);
        editorEdit.putLong("dbIdentifier", j);
        editorEdit.putLong("totalContacts", sTotalContacts);
        editorEdit.putLong("lastUpdatedTimestamp", sContactsLastUpdated);
        editorEdit.putLong("totalFields", sTotalFields);
        editorEdit.putLong("totalSvcFields", sTotalSvcFields);
        editorEdit.apply();
        if (V) {
            Log.v(TAG, "Saved Primary:" + sPrimaryVersionCounter + ", Secondary:" + sSecondaryVersionCounter + ", Database Identifier: " + j);
        }
    }

    static void fetchPbapParams(Context context) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long timeInMillis = Calendar.getInstance().getTimeInMillis();
        sDbIdentifier.set(defaultSharedPreferences.getLong("DbIdentifier", timeInMillis));
        sPrimaryVersionCounter = defaultSharedPreferences.getLong("primary", 0L);
        sSecondaryVersionCounter = defaultSharedPreferences.getLong("secondary", 0L);
        sTotalFields = defaultSharedPreferences.getLong("totalContacts", 0L);
        sContactsLastUpdated = defaultSharedPreferences.getLong("lastUpdatedTimestamp", timeInMillis);
        sTotalFields = defaultSharedPreferences.getLong("totalFields", 0L);
        sTotalSvcFields = defaultSharedPreferences.getLong("totalSvcFields", 0L);
        if (V) {
            Log.v(TAG, " fetchPbapParams " + defaultSharedPreferences.getAll());
        }
    }

    static void loadAllContacts(Context context, Handler handler) {
        if (V) {
            Log.v(TAG, "Loading Contacts ...");
        }
        sTotalContacts = fetchAndSetContacts(context, handler, new String[]{"contact_id", "data1", BluetoothShare.MIMETYPE}, null, null, true);
        if (sTotalContacts < 0) {
            sTotalContacts = 0L;
        } else {
            handler.sendMessage(handler.obtainMessage(5));
        }
    }

    static void updateSecondaryVersionCounter(Context context, Handler handler) {
        char c;
        int i;
        byte b;
        ArrayList<String> arrayList = new ArrayList();
        HashSet hashSet = new HashSet();
        Cursor cursorQuery = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, new String[]{"_id", "contact_last_updated_timestamp"}, null, null, null);
        if (cursorQuery == null) {
            Log.d(TAG, "Failed to fetch data from contact database");
            return;
        }
        while (true) {
            c = 0;
            i = 1;
            if (!cursorQuery.moveToNext()) {
                break;
            }
            String string = cursorQuery.getString(0);
            if (cursorQuery.getLong(1) > sContactsLastUpdated) {
                arrayList.add(string);
            }
            hashSet.add(string);
        }
        int count = cursorQuery.getCount();
        cursorQuery.close();
        if (V) {
            Log.v(TAG, "updated list =" + arrayList);
        }
        String[] strArr = {"contact_id", "data1", BluetoothShare.MIMETYPE};
        long j = count;
        if (j > sTotalContacts) {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                fetchAndSetContacts(context, handler, strArr, "contact_id=?", new String[]{(String) it.next()}, false);
                sSecondaryVersionCounter++;
                sPrimaryVersionCounter++;
                sTotalContacts = j;
            }
        } else if (j < sTotalContacts) {
            sTotalContacts = j;
            ArrayList arrayList2 = new ArrayList(Arrays.asList("vnd.android.cursor.item/name", "vnd.android.cursor.item/phone_v2", "vnd.android.cursor.item/email_v2", "vnd.android.cursor.item/postal-address_v2"));
            HashSet<String> hashSet2 = new HashSet(sContactSet);
            hashSet2.removeAll(hashSet);
            sPrimaryVersionCounter += (long) hashSet2.size();
            sSecondaryVersionCounter += (long) hashSet2.size();
            if (V) {
                Log.v(TAG, "Deleted Contacts : " + hashSet2);
            }
            for (String str : hashSet2) {
                sContactSet.remove(str);
                Cursor cursorQuery2 = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, strArr, "contact_id=?", new String[]{str}, null);
                if (cursorQuery2 == null) {
                    Log.d(TAG, "Failed to fetch data from contact database");
                    return;
                }
                while (cursorQuery2.moveToNext()) {
                    if (arrayList2.contains(cursorQuery2.getString(cursorQuery2.getColumnIndex(BluetoothShare.MIMETYPE)))) {
                        sTotalSvcFields--;
                    }
                    sTotalFields--;
                }
                cursorQuery2.close();
            }
        } else {
            for (String str2 : arrayList) {
                sPrimaryVersionCounter++;
                ArrayList arrayList3 = new ArrayList();
                ArrayList arrayList4 = new ArrayList();
                ArrayList arrayList5 = new ArrayList();
                String str3 = null;
                String[] strArr2 = new String[i];
                strArr2[c] = str2;
                Cursor cursorQuery3 = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, strArr, "contact_id=?", strArr2, null);
                if (cursorQuery3 == null) {
                    Log.d(TAG, "Failed to fetch data from contact database");
                    return;
                }
                int columnIndex = cursorQuery3.getColumnIndex("data1");
                int columnIndex2 = cursorQuery3.getColumnIndex(BluetoothShare.MIMETYPE);
                while (cursorQuery3.moveToNext()) {
                    String string2 = cursorQuery3.getString(columnIndex);
                    String string3 = cursorQuery3.getString(columnIndex2);
                    int iHashCode = string3.hashCode();
                    if (iHashCode != -1569536764) {
                        if (iHashCode != -1079224304) {
                            if (iHashCode != -601229436) {
                                b = (iHashCode == 684173810 && string3.equals("vnd.android.cursor.item/phone_v2")) ? (byte) 1 : (byte) -1;
                            } else if (string3.equals("vnd.android.cursor.item/postal-address_v2")) {
                                b = 2;
                            }
                        } else if (string3.equals("vnd.android.cursor.item/name")) {
                            b = 3;
                        }
                    } else if (string3.equals("vnd.android.cursor.item/email_v2")) {
                        b = 0;
                    }
                    switch (b) {
                        case 0:
                            arrayList4.add(string2);
                            break;
                        case 1:
                            arrayList3.add(string2);
                            break;
                        case 2:
                            arrayList5.add(string2);
                            break;
                        case 3:
                            str3 = string2;
                            break;
                    }
                }
                ContactData contactData = new ContactData(str3, arrayList3, arrayList4, arrayList5);
                cursorQuery3.close();
                ContactData contactData2 = sContactDataset.get(str2);
                if (contactData2 == null) {
                    Log.e(TAG, "Null contact in the updateList: " + str2);
                } else if (!Objects.equals(str3, contactData2.mName) || checkFieldUpdates(contactData2.mPhone, arrayList3) || checkFieldUpdates(contactData2.mEmail, arrayList4) || checkFieldUpdates(contactData2.mAddress, arrayList5)) {
                    sSecondaryVersionCounter++;
                    sContactDataset.put(str2, contactData);
                }
                c = 0;
                i = 1;
            }
        }
        Log.d(TAG, "primaryVersionCounter = " + sPrimaryVersionCounter + ", secondaryVersionCounter=" + sSecondaryVersionCounter);
        if (sSecondaryVersionCounter < 0 || sPrimaryVersionCounter < 0) {
            handler.sendMessage(handler.obtainMessage(7));
        }
    }

    private static boolean checkFieldUpdates(ArrayList<String> arrayList, ArrayList<String> arrayList2) {
        if (arrayList2 != null && arrayList != null) {
            if (arrayList2.size() != arrayList.size()) {
                sTotalSvcFields += (long) Math.abs(arrayList2.size() - arrayList.size());
                sTotalFields += (long) Math.abs(arrayList2.size() - arrayList.size());
                return true;
            }
            Iterator<String> it = arrayList2.iterator();
            while (it.hasNext()) {
                if (!arrayList.contains(it.next())) {
                    return true;
                }
            }
            return false;
        }
        if (arrayList2 == null && arrayList != null && arrayList.size() > 0) {
            sTotalSvcFields += (long) arrayList.size();
            sTotalFields += (long) arrayList.size();
            return true;
        }
        if (arrayList == null && arrayList2 != null && arrayList2.size() > 0) {
            sTotalSvcFields += (long) arrayList2.size();
            sTotalFields += (long) arrayList2.size();
            return true;
        }
        return false;
    }

    private static int fetchAndSetContacts(Context context, Handler handler, String[] strArr, String str, String[] strArr2, boolean z) {
        byte b;
        Cursor cursorQuery = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, strArr, str, strArr2, null);
        if (cursorQuery == null) {
            Log.d(TAG, "Failed to fetch contacts data from database..");
            if (z) {
                handler.sendMessageDelayed(handler.obtainMessage(4), QUERY_CONTACT_RETRY_INTERVAL);
            }
            return -1;
        }
        int columnIndex = cursorQuery.getColumnIndex("contact_id");
        int columnIndex2 = cursorQuery.getColumnIndex("data1");
        int columnIndex3 = cursorQuery.getColumnIndex(BluetoothShare.MIMETYPE);
        long j = 0;
        long j2 = 0;
        while (cursorQuery.moveToNext()) {
            String string = cursorQuery.getString(columnIndex);
            String string2 = cursorQuery.getString(columnIndex2);
            String string3 = cursorQuery.getString(columnIndex3);
            int iHashCode = string3.hashCode();
            if (iHashCode != -1569536764) {
                if (iHashCode != -1079224304) {
                    if (iHashCode != -601229436) {
                        b = (iHashCode == 684173810 && string3.equals("vnd.android.cursor.item/phone_v2")) ? (byte) 0 : (byte) -1;
                    } else if (string3.equals("vnd.android.cursor.item/postal-address_v2")) {
                        b = 2;
                    }
                } else if (string3.equals("vnd.android.cursor.item/name")) {
                    b = 3;
                }
            } else if (string3.equals("vnd.android.cursor.item/email_v2")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    setContactFields(TYPE_PHONE, string, string2);
                    j2++;
                    break;
                case 1:
                    setContactFields("email", string, string2);
                    j2++;
                    break;
                case 2:
                    setContactFields(TYPE_ADDRESS, string, string2);
                    j2++;
                    break;
                case 3:
                    setContactFields("name", string, string2);
                    j2++;
                    break;
            }
            sContactSet.add(string);
            j++;
        }
        cursorQuery.close();
        if (z && j != sTotalFields) {
            sPrimaryVersionCounter += Math.abs(sTotalContacts - ((long) sContactSet.size()));
            if (j2 != sTotalSvcFields) {
                if (sTotalContacts != sContactSet.size()) {
                    sSecondaryVersionCounter += Math.abs(sTotalContacts - ((long) sContactSet.size()));
                } else {
                    sSecondaryVersionCounter++;
                }
            }
            if (sPrimaryVersionCounter < 0 || sSecondaryVersionCounter < 0) {
                rolloverCounters();
            }
            sTotalFields = j;
            sTotalSvcFields = j2;
            sContactsLastUpdated = System.currentTimeMillis();
            Log.d(TAG, "Contacts updated between last BT OFF and currentPbap Connect, primaryVersionCounter=" + sPrimaryVersionCounter + ", secondaryVersionCounter=" + sSecondaryVersionCounter);
        } else if (!z) {
            sTotalFields++;
            sTotalSvcFields++;
        }
        return sContactSet.size();
    }

    private static void setContactFields(String str, String str2, String str3) {
        ContactData contactData;
        if (sContactDataset.containsKey(str2)) {
            contactData = sContactDataset.get(str2);
        } else {
            contactData = new ContactData();
        }
        byte b = -1;
        int iHashCode = str.hashCode();
        if (iHashCode != -1147692044) {
            if (iHashCode != 3373707) {
                if (iHashCode != 96619420) {
                    if (iHashCode == 106642798 && str.equals(TYPE_PHONE)) {
                        b = 1;
                    }
                } else if (str.equals("email")) {
                    b = 2;
                }
            } else if (str.equals("name")) {
                b = 0;
            }
        } else if (str.equals(TYPE_ADDRESS)) {
            b = 3;
        }
        switch (b) {
            case 0:
                contactData.mName = str3;
                break;
            case 1:
                contactData.mPhone.add(str3);
                break;
            case 2:
                contactData.mEmail.add(str3);
                break;
            case 3:
                contactData.mAddress.add(str3);
                break;
        }
        sContactDataset.put(str2, contactData);
    }

    static void rolloverCounters() {
        sDbIdentifier.set(Calendar.getInstance().getTimeInMillis());
        sPrimaryVersionCounter = sPrimaryVersionCounter < 0 ? 0L : sPrimaryVersionCounter;
        sSecondaryVersionCounter = sSecondaryVersionCounter >= 0 ? sSecondaryVersionCounter : 0L;
        if (V) {
            Log.v(TAG, "DbIdentifier rolled over to:" + sDbIdentifier);
        }
    }
}
