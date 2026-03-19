package com.android.gallery3d.filtershow.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import java.util.ArrayList;

public class FilterStackSource {
    private SQLiteDatabase database = null;
    private final FilterStackDBHelper dbHelper;

    public FilterStackSource(Context context) {
        this.dbHelper = new FilterStackDBHelper(context);
    }

    public void open() {
        try {
            this.database = this.dbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            Log.w("FilterStackSource", "could not open database", e);
        }
    }

    public void close() {
        this.database = null;
        this.dbHelper.close();
    }

    public boolean insertStack(String str, byte[] bArr) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("stack_id", str);
        contentValues.put("stack", bArr);
        this.database.beginTransaction();
        try {
            boolean z = -1 != this.database.insert("filterstack", null, contentValues);
            this.database.setTransactionSuccessful();
            return z;
        } finally {
            this.database.endTransaction();
        }
    }

    public void updateStackName(int i, String str) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("stack_id", str);
        this.database.beginTransaction();
        try {
            this.database.update("filterstack", contentValues, "_id = ?", new String[]{"" + i});
            this.database.setTransactionSuccessful();
        } finally {
            this.database.endTransaction();
        }
    }

    public boolean removeStack(int i) {
        this.database.beginTransaction();
        try {
            boolean z = true;
            if (this.database.delete("filterstack", "_id = ?", new String[]{"" + i}) == 0) {
                z = false;
            }
            this.database.setTransactionSuccessful();
            return z;
        } finally {
            this.database.endTransaction();
        }
    }

    public ArrayList<FilterUserPresetRepresentation> getAllUserPresets() throws Throwable {
        Cursor cursorQuery;
        String string;
        byte[] blob;
        ArrayList<FilterUserPresetRepresentation> arrayList = new ArrayList<>();
        this.database.beginTransaction();
        try {
            cursorQuery = this.database.query("filterstack", new String[]{BookmarkEnhance.COLUMN_ID, "stack_id", "stack"}, null, null, null, null, null, null);
            if (cursorQuery != null) {
                try {
                    for (boolean zMoveToFirst = cursorQuery.moveToFirst(); zMoveToFirst; zMoveToFirst = cursorQuery.moveToNext()) {
                        int i = cursorQuery.getInt(0);
                        if (!cursorQuery.isNull(1)) {
                            string = cursorQuery.getString(1);
                        } else {
                            string = null;
                        }
                        if (!cursorQuery.isNull(2)) {
                            blob = cursorQuery.getBlob(2);
                        } else {
                            blob = null;
                        }
                        String str = new String(blob);
                        ImagePreset imagePreset = new ImagePreset();
                        imagePreset.readJsonFromString(str);
                        arrayList.add(new FilterUserPresetRepresentation(string, imagePreset, i));
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    this.database.endTransaction();
                    throw th;
                }
            }
            this.database.setTransactionSuccessful();
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            this.database.endTransaction();
            return arrayList;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }
}
