package com.android.settings.intelligence.search.savedqueries;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.android.settings.intelligence.search.SearchResult;
import com.android.settings.intelligence.search.indexing.IndexDatabaseHelper;
import com.android.settings.intelligence.utils.AsyncLoader;
import java.util.ArrayList;
import java.util.List;

public class SavedQueryLoader extends AsyncLoader<List<? extends SearchResult>> {
    static final int MAX_PROPOSED_SUGGESTIONS = 5;
    private final SQLiteDatabase mDatabase;

    public SavedQueryLoader(Context context) {
        super(context);
        this.mDatabase = IndexDatabaseHelper.getInstance(context).getReadableDatabase();
    }

    @Override
    public void onDiscardResult(List<? extends SearchResult> list) {
    }

    @Override
    public List<? extends SearchResult> loadInBackground() throws Throwable {
        Cursor cursorQuery = this.mDatabase.query("saved_queries", new String[]{"query"}, null, null, null, null, "rowId DESC", String.valueOf(MAX_PROPOSED_SUGGESTIONS));
        try {
            List<SearchResult> listConvertCursorToResult = convertCursorToResult(cursorQuery);
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return listConvertCursorToResult;
        } catch (Throwable th) {
            th = th;
            try {
                throw th;
            } catch (Throwable th2) {
                th = th2;
                if (cursorQuery != null) {
                    if (th != null) {
                        try {
                            cursorQuery.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        cursorQuery.close();
                    }
                }
                throw th;
            }
        }
    }

    private List<SearchResult> convertCursorToResult(Cursor cursor) {
        ArrayList arrayList = new ArrayList();
        while (cursor.moveToNext()) {
            SavedQueryPayload savedQueryPayload = new SavedQueryPayload(cursor.getString(cursor.getColumnIndex("query")));
            arrayList.add(new SearchResult.Builder().setDataKey(savedQueryPayload.query).setTitle(savedQueryPayload.query).setPayload(savedQueryPayload).build());
        }
        return arrayList;
    }
}
