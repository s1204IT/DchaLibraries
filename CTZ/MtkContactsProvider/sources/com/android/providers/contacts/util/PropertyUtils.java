package com.android.providers.contacts.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class PropertyUtils {
    public static void createPropertiesTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE properties (property_key TEXT PRIMARY KEY, property_value TEXT );");
    }

    public static String getProperty(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        String string;
        Cursor cursorQuery = sQLiteDatabase.query("properties", new String[]{"property_value"}, "property_key=?", new String[]{str}, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                string = cursorQuery.getString(0);
            } else {
                string = null;
            }
            return string != null ? string : str2;
        } finally {
            cursorQuery.close();
        }
    }

    public static void setProperty(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("property_key", str);
        contentValues.put("property_value", str2);
        sQLiteDatabase.replace("properties", null, contentValues);
    }
}
