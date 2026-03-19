package com.android.bluetooth.map;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

@TargetApi(19)
public class SmsMmsContacts {
    private static final String CONTACT_SEL_VISIBLE = "in_visible_group=1";
    private static final String TAG = "SmsMmsContacts";
    private static final Uri ADDRESS_URI = Telephony.MmsSms.CONTENT_URI.buildUpon().appendPath("canonical-addresses").build();
    private static final String[] ADDRESS_PROJECTION = {"_id", "address"};
    private static final int COL_ADDR_ID = Arrays.asList(ADDRESS_PROJECTION).indexOf("_id");
    private static final int COL_ADDR_ADDR = Arrays.asList(ADDRESS_PROJECTION).indexOf("address");
    private static final String[] CONTACT_PROJECTION = {"_id", "display_name"};
    private static final int COL_CONTACT_ID = Arrays.asList(CONTACT_PROJECTION).indexOf("_id");
    private static final int COL_CONTACT_NAME = Arrays.asList(CONTACT_PROJECTION).indexOf("display_name");
    private HashMap<Long, String> mPhoneNumbers = null;
    private final HashMap<String, MapContact> mNames = new HashMap<>(10);

    public String getPhoneNumber(ContentResolver contentResolver, long j) {
        String str;
        if (this.mPhoneNumbers != null && (str = this.mPhoneNumbers.get(Long.valueOf(j))) != null) {
            return str;
        }
        fillPhoneCache(contentResolver);
        return this.mPhoneNumbers.get(Long.valueOf(j));
    }

    public static String getPhoneNumberUncached(ContentResolver contentResolver, long j) {
        Cursor cursorQuery = contentResolver.query(ADDRESS_URI, ADDRESS_PROJECTION, "_id = " + j, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToPosition(0)) {
                    return cursorQuery.getString(COL_ADDR_ADDR);
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        Log.e(TAG, "query failed");
        if (cursorQuery == null) {
            return null;
        }
        cursorQuery.close();
        return null;
    }

    public void clearCache() {
        if (this.mPhoneNumbers != null) {
            this.mPhoneNumbers.clear();
        }
        if (this.mNames != null) {
            this.mNames.clear();
        }
    }

    private void fillPhoneCache(ContentResolver contentResolver) {
        Cursor cursorQuery = contentResolver.query(ADDRESS_URI, ADDRESS_PROJECTION, null, null, null);
        if (this.mPhoneNumbers == null) {
            int count = 0;
            if (cursorQuery != null) {
                count = cursorQuery.getCount();
            }
            this.mPhoneNumbers = new HashMap<>(count);
        } else {
            this.mPhoneNumbers.clear();
        }
        try {
            if (cursorQuery != null) {
                cursorQuery.moveToPosition(-1);
                while (cursorQuery.moveToNext()) {
                    long j = cursorQuery.getLong(COL_ADDR_ID);
                    this.mPhoneNumbers.put(Long.valueOf(j), cursorQuery.getString(COL_ADDR_ADDR));
                }
            } else {
                Log.e(TAG, "query failed");
            }
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    public MapContact getContactNameFromPhone(String str, ContentResolver contentResolver) {
        return getContactNameFromPhone(str, contentResolver, null);
    }

    public MapContact getContactNameFromPhone(String str, ContentResolver contentResolver, String str2) {
        String str3;
        String[] strArr;
        MapContact mapContact = this.mNames.get(str);
        MapContact mapContactCreate = null;
        if (mapContact != null) {
            if (mapContact.getId() < 0) {
                return null;
            }
            if (str2 == null) {
                return mapContact;
            }
            if (!Pattern.compile(Pattern.quote(".*" + str2.replace("*", ".*") + ".*"), 2).matcher(mapContact.getName()).find()) {
                return null;
            }
            return mapContact;
        }
        Uri uriWithAppendedPath = Uri.withAppendedPath(ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI, Uri.encode(str));
        if (str2 == null) {
            str3 = CONTACT_SEL_VISIBLE;
            strArr = null;
        } else {
            String str4 = CONTACT_SEL_VISIBLE + "AND display_name like ?";
            str3 = str4;
            strArr = new String[]{"%" + str2.replace("*", "%") + "%"};
        }
        Cursor cursorQuery = contentResolver.query(uriWithAppendedPath, CONTACT_PROJECTION, str3, strArr, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() >= 1) {
                    cursorQuery.moveToFirst();
                    mapContactCreate = MapContact.create(cursorQuery.getLong(COL_CONTACT_ID), cursorQuery.getString(COL_CONTACT_NAME));
                    this.mNames.put(str, mapContactCreate);
                } else {
                    this.mNames.put(str, MapContact.create(-1L, null));
                }
            } catch (Throwable th) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                throw th;
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return mapContactCreate;
    }
}
