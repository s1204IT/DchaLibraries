package com.android.contacts.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.model.dataitem.StructuredNameDataItem;
import java.util.Map;
import java.util.TreeMap;

public class NameConverter {
    public static final String[] STRUCTURED_NAME_FIELDS = {"data4", "data2", "data5", "data3", "data6"};

    public static String structuredNameToDisplayName(Context context, ContentValues contentValues) {
        Uri.Builder builderAppendPath = ContactsContract.AUTHORITY_URI.buildUpon().appendPath("complete_name");
        for (String str : STRUCTURED_NAME_FIELDS) {
            if (contentValues.containsKey(str)) {
                appendQueryParameter(builderAppendPath, str, contentValues.getAsString(str));
            }
        }
        return fetchDisplayName(context, builderAppendPath.build());
    }

    private static String fetchDisplayName(Context context, Uri uri) {
        Cursor cursorQuery = context.getContentResolver().query(uri, new String[]{"data1"}, null, null, null);
        String string = null;
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    string = cursorQuery.getString(0);
                }
            } finally {
                cursorQuery.close();
            }
        }
        return string;
    }

    private static void appendQueryParameter(Uri.Builder builder, String str, String str2) {
        if (!TextUtils.isEmpty(str2)) {
            builder.appendQueryParameter(str, str2);
        }
    }

    public static StructuredNameDataItem parsePhoneticName(String str, StructuredNameDataItem structuredNameDataItem) {
        String str2;
        String str3;
        String str4 = null;
        if (!TextUtils.isEmpty(str)) {
            String[] strArrSplit = str.split(" ", 3);
            switch (strArrSplit.length) {
                case 1:
                    str3 = null;
                    str4 = strArrSplit[0];
                    str2 = null;
                    break;
                case 2:
                    String str5 = strArrSplit[0];
                    str2 = strArrSplit[1];
                    str3 = null;
                    str4 = str5;
                    break;
                case 3:
                    str4 = strArrSplit[0];
                    str3 = strArrSplit[1];
                    str2 = strArrSplit[2];
                    break;
                default:
                    str2 = null;
                    str3 = null;
                    break;
            }
        }
        if (structuredNameDataItem == null) {
            structuredNameDataItem = new StructuredNameDataItem();
        }
        structuredNameDataItem.setPhoneticFamilyName(str4);
        structuredNameDataItem.setPhoneticMiddleName(str3);
        structuredNameDataItem.setPhoneticGivenName(str2);
        return structuredNameDataItem;
    }

    public static String buildPhoneticName(String str, String str2, String str3) {
        if (!TextUtils.isEmpty(str) || !TextUtils.isEmpty(str2) || !TextUtils.isEmpty(str3)) {
            StringBuilder sb = new StringBuilder();
            if (!TextUtils.isEmpty(str)) {
                sb.append(str.trim());
                sb.append(' ');
            }
            if (!TextUtils.isEmpty(str2)) {
                sb.append(str2.trim());
                sb.append(' ');
            }
            if (!TextUtils.isEmpty(str3)) {
                sb.append(str3.trim());
                sb.append(' ');
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
        return null;
    }

    public static ContentValues displayNameToStructuredName(Context context, String str, ContentValues contentValues) {
        if (contentValues == null) {
            contentValues = new ContentValues();
        }
        Map<String, String> mapDisplayNameToStructuredName = displayNameToStructuredName(context, str);
        for (String str2 : mapDisplayNameToStructuredName.keySet()) {
            contentValues.put(str2, mapDisplayNameToStructuredName.get(str2));
        }
        return contentValues;
    }

    public static Map<String, String> displayNameToStructuredName(Context context, String str) {
        TreeMap treeMap = new TreeMap();
        Uri.Builder builderAppendPath = ContactsContract.AUTHORITY_URI.buildUpon().appendPath("complete_name");
        appendQueryParameter(builderAppendPath, "data1", str);
        Cursor cursorQuery = context.getContentResolver().query(builderAppendPath.build(), STRUCTURED_NAME_FIELDS, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    for (int i = 0; i < STRUCTURED_NAME_FIELDS.length; i++) {
                        treeMap.put(STRUCTURED_NAME_FIELDS[i], cursorQuery.getString(i));
                    }
                }
            } finally {
                cursorQuery.close();
            }
        }
        return treeMap;
    }
}
