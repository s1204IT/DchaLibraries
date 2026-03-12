package com.android.settings.intelligence.search.savedqueries;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.android.settings.intelligence.search.indexing.IndexDatabaseHelper;
import com.android.settings.intelligence.utils.AsyncLoader;

public class SavedQueryRemover extends AsyncLoader<Void> {
    public SavedQueryRemover(Context context) {
        super(context);
    }

    @Override
    public Void loadInBackground() {
        try {
            getWritableDatabase().delete("saved_queries", null, null);
        } catch (Exception e) {
            Log.d("SavedQueryRemover", "Cannot update saved Search queries", e);
        }
        return null;
    }

    @Override
    public void onDiscardResult(Void r1) {
    }

    private SQLiteDatabase getWritableDatabase() {
        try {
            return IndexDatabaseHelper.getInstance(getContext()).getWritableDatabase();
        } catch (SQLiteException e) {
            Log.e("SavedQueryRemover", "Cannot open writable database", e);
            return null;
        }
    }
}
