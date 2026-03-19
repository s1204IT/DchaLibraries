package com.android.settings.intelligence.search.savedqueries;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.android.settings.intelligence.search.indexing.IndexDatabaseHelper;
import com.android.settings.intelligence.utils.AsyncLoader;

public class SavedQueryRecorder extends AsyncLoader<Void> {
    private static long MAX_SAVED_SEARCH_QUERY = 64;
    private final String mQuery;

    public SavedQueryRecorder(Context context, String str) {
        super(context);
        this.mQuery = str;
    }

    @Override
    protected void onDiscardResult(Void r1) {
    }

    @Override
    public Void loadInBackground() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put("query", this.mQuery);
        contentValues.put("timestamp", Long.valueOf(jCurrentTimeMillis));
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if (writableDatabase == null) {
            return null;
        }
        try {
            writableDatabase.delete("saved_queries", "query = ?", new String[]{this.mQuery});
            long jInsertOrThrow = writableDatabase.insertOrThrow("saved_queries", null, contentValues) - MAX_SAVED_SEARCH_QUERY;
            if (jInsertOrThrow > 0) {
                Log.d("SavedQueryRecorder", "Deleted '" + writableDatabase.delete("saved_queries", "rowId <= ?", new String[]{Long.toString(jInsertOrThrow)}) + "' saved Search query(ies)");
            }
        } catch (Exception e) {
            Log.d("SavedQueryRecorder", "Cannot update saved Search queries", e);
        }
        return null;
    }

    private SQLiteDatabase getWritableDatabase() {
        try {
            return IndexDatabaseHelper.getInstance(getContext()).getWritableDatabase();
        } catch (SQLiteException e) {
            Log.e("SavedQueryRecorder", "Cannot open writable database", e);
            return null;
        }
    }
}
