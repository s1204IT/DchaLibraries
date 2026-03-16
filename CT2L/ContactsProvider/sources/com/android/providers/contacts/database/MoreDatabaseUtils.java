package com.android.providers.contacts.database;

import android.database.Cursor;
import android.util.Log;

public class MoreDatabaseUtils {
    public static String buildCreateIndexSql(String table, String field) {
        return "CREATE INDEX " + buildIndexName(table, field) + " ON " + table + "(" + field + ")";
    }

    public static String buildDropIndexSql(String table, String field) {
        return "DROP INDEX IF EXISTS " + buildIndexName(table, field);
    }

    public static String buildIndexName(String table, String field) {
        return table + "_" + field + "_index";
    }

    public static String buildBindArgString(int numArgs) {
        StringBuilder sb = new StringBuilder();
        String delimiter = "";
        for (int i = 0; i < numArgs; i++) {
            sb.append(delimiter).append("?");
            delimiter = ",";
        }
        return sb.toString();
    }

    public static final void dumpCursor(String logTag, String name, Cursor c) {
        Log.d(logTag, "Dumping cursor " + name + " containing " + c.getCount() + " rows");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < c.getColumnCount(); i++) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(c.getColumnName(i));
        }
        Log.d(logTag, sb.toString());
        c.moveToPosition(-1);
        while (c.moveToNext()) {
            sb.setLength(0);
            sb.append("row#");
            sb.append(c.getPosition());
            for (int i2 = 0; i2 < c.getColumnCount(); i2++) {
                sb.append(" ");
                String s = c.getString(i2);
                sb.append(s == null ? "{null}" : s.replaceAll("\\s", "{space}"));
            }
            Log.d(logTag, sb.toString());
        }
    }
}
