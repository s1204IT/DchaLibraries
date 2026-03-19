package com.android.providers.contacts.database;

import android.database.Cursor;
import android.util.Log;

public class MoreDatabaseUtils {
    public static String buildCreateIndexSql(String str, String str2) {
        return "CREATE INDEX " + buildIndexName(str, str2) + " ON " + str + "(" + str2 + ")";
    }

    public static String buildDropIndexSql(String str, String str2) {
        return "DROP INDEX IF EXISTS " + buildIndexName(str, str2);
    }

    public static String buildIndexName(String str, String str2) {
        return str + "_" + str2 + "_index";
    }

    public static String buildBindArgString(int i) {
        StringBuilder sb = new StringBuilder();
        String str = "";
        for (int i2 = 0; i2 < i; i2++) {
            sb.append(str);
            sb.append("?");
            str = ",";
        }
        return sb.toString();
    }

    public static final void dumpCursor(String str, String str2, Cursor cursor) {
        Log.d(str, "Dumping cursor " + str2 + " containing " + cursor.getCount() + " rows");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(cursor.getColumnName(i));
        }
        Log.d(str, sb.toString());
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            sb.setLength(0);
            sb.append("row#");
            sb.append(cursor.getPosition());
            for (int i2 = 0; i2 < cursor.getColumnCount(); i2++) {
                sb.append(" ");
                String string = cursor.getString(i2);
                sb.append(string == null ? "{null}" : string.replaceAll("\\s", "{space}"));
            }
            Log.d(str, sb.toString());
        }
    }
}
