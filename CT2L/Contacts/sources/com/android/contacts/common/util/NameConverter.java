package com.android.contacts.common.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.common.model.dataitem.StructuredNameDataItem;
import java.util.Map;
import java.util.TreeMap;

public class NameConverter {
    public static final String[] STRUCTURED_NAME_FIELDS = {"data4", "data2", "data5", "data3", "data6"};

    public static String structuredNameToDisplayName(Context context, Map<String, String> structuredName) {
        Uri.Builder builder = ContactsContract.AUTHORITY_URI.buildUpon().appendPath("complete_name");
        String[] arr$ = STRUCTURED_NAME_FIELDS;
        for (String key : arr$) {
            if (structuredName.containsKey(key)) {
                appendQueryParameter(builder, key, structuredName.get(key));
            }
        }
        return fetchDisplayName(context, builder.build());
    }

    public static String structuredNameToDisplayName(Context context, ContentValues values) {
        Uri.Builder builder = ContactsContract.AUTHORITY_URI.buildUpon().appendPath("complete_name");
        String[] arr$ = STRUCTURED_NAME_FIELDS;
        for (String key : arr$) {
            if (values.containsKey(key)) {
                appendQueryParameter(builder, key, values.getAsString(key));
            }
        }
        return fetchDisplayName(context, builder.build());
    }

    private static String fetchDisplayName(Context context, Uri uri) {
        String displayName = null;
        Cursor cursor = context.getContentResolver().query(uri, new String[]{"data1"}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return displayName;
    }

    public static Map<String, String> displayNameToStructuredName(Context context, String displayName) {
        Map<String, String> structuredName = new TreeMap<>();
        Uri.Builder builder = ContactsContract.AUTHORITY_URI.buildUpon().appendPath("complete_name");
        appendQueryParameter(builder, "data1", displayName);
        Cursor cursor = context.getContentResolver().query(builder.build(), STRUCTURED_NAME_FIELDS, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    for (int i = 0; i < STRUCTURED_NAME_FIELDS.length; i++) {
                        structuredName.put(STRUCTURED_NAME_FIELDS[i], cursor.getString(i));
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return structuredName;
    }

    public static ContentValues displayNameToStructuredName(Context context, String displayName, ContentValues contentValues) {
        if (contentValues == null) {
            contentValues = new ContentValues();
        }
        Map<String, String> mapValues = displayNameToStructuredName(context, displayName);
        for (String key : mapValues.keySet()) {
            contentValues.put(key, mapValues.get(key));
        }
        return contentValues;
    }

    private static void appendQueryParameter(Uri.Builder builder, String field, String value) {
        if (!TextUtils.isEmpty(value)) {
            builder.appendQueryParameter(field, value);
        }
    }

    public static StructuredNameDataItem parsePhoneticName(String phoneticName, StructuredNameDataItem item) {
        String family = null;
        String middle = null;
        String given = null;
        if (!TextUtils.isEmpty(phoneticName)) {
            String[] strings = phoneticName.split(" ", 3);
            switch (strings.length) {
                case 1:
                    family = strings[0];
                    break;
                case 2:
                    family = strings[0];
                    given = strings[1];
                    break;
                case 3:
                    family = strings[0];
                    middle = strings[1];
                    given = strings[2];
                    break;
            }
        }
        if (item == null) {
            item = new StructuredNameDataItem();
        }
        item.setPhoneticFamilyName(family);
        item.setPhoneticMiddleName(middle);
        item.setPhoneticGivenName(given);
        return item;
    }

    public static String buildPhoneticName(String family, String middle, String given) {
        if (TextUtils.isEmpty(family) && TextUtils.isEmpty(middle) && TextUtils.isEmpty(given)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(family)) {
            sb.append(family.trim()).append(' ');
        }
        if (!TextUtils.isEmpty(middle)) {
            sb.append(middle.trim()).append(' ');
        }
        if (!TextUtils.isEmpty(given)) {
            sb.append(given.trim()).append(' ');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
