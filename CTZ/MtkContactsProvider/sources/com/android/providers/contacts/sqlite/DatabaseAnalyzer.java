package com.android.providers.contacts.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.android.providers.contacts.AbstractContactsProvider;
import java.util.ArrayList;
import java.util.List;

public class DatabaseAnalyzer {
    private static final boolean VERBOSE_LOGGING = AbstractContactsProvider.VERBOSE_LOGGING;

    private DatabaseAnalyzer() {
    }

    private static List<String> findTablesAndViews(SQLiteDatabase sQLiteDatabase) {
        ArrayList arrayList = new ArrayList();
        Throwable th = null;
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type in (\"table\", \"view\")", null);
        while (cursorRawQuery.moveToNext()) {
            try {
                try {
                    arrayList.add(cursorRawQuery.getString(0).toLowerCase());
                } catch (Throwable th2) {
                    if (cursorRawQuery != null) {
                        if (th != null) {
                            try {
                                cursorRawQuery.close();
                            } catch (Throwable th3) {
                                th.addSuppressed(th3);
                            }
                        } else {
                            cursorRawQuery.close();
                        }
                    }
                    throw th2;
                }
            } finally {
            }
        }
        if (cursorRawQuery != null) {
            cursorRawQuery.close();
        }
        return arrayList;
    }

    private static List<String> findColumns(SQLiteDatabase sQLiteDatabase, String str) {
        ArrayList arrayList = new ArrayList();
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT * FROM " + str + " WHERE 0 LIMIT 0", null);
        for (int i = 0; i < cursorRawQuery.getColumnCount(); i++) {
            try {
                arrayList.add(cursorRawQuery.getColumnName(i).toLowerCase());
            } finally {
                cursorRawQuery.close();
            }
        }
        return arrayList;
    }

    public static List<String> findTableViewsAllowingColumns(SQLiteDatabase sQLiteDatabase) {
        List<String> listFindTablesAndViews = findTablesAndViews(sQLiteDatabase);
        if (VERBOSE_LOGGING) {
            Log.d("DatabaseAnalyzer", "Tables and views:");
        }
        ArrayList arrayList = new ArrayList(listFindTablesAndViews);
        for (String str : listFindTablesAndViews) {
            if (VERBOSE_LOGGING) {
                Log.d("DatabaseAnalyzer", "  " + str);
            }
            List<String> listFindColumns = findColumns(sQLiteDatabase, str);
            if (VERBOSE_LOGGING) {
                Log.d("DatabaseAnalyzer", "    Columns: " + listFindColumns);
            }
            for (String str2 : listFindColumns) {
                if (arrayList.remove(str2)) {
                    Log.d("DatabaseAnalyzer", "Removing [" + str2 + "] from disallow list");
                }
            }
        }
        return arrayList;
    }
}
