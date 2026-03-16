package com.android.gallery3d.filtershow.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
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

    public boolean insertStack(String stackName, byte[] stackBlob) {
        ContentValues val = new ContentValues();
        val.put("stack_id", stackName);
        val.put("stack", stackBlob);
        this.database.beginTransaction();
        try {
            boolean ret = -1 != this.database.insert("filterstack", null, val);
            this.database.setTransactionSuccessful();
            return ret;
        } finally {
            this.database.endTransaction();
        }
    }

    public void updateStackName(int id, String stackName) {
        ContentValues val = new ContentValues();
        val.put("stack_id", stackName);
        this.database.beginTransaction();
        try {
            this.database.update("filterstack", val, "_id = ?", new String[]{"" + id});
            this.database.setTransactionSuccessful();
        } finally {
            this.database.endTransaction();
        }
    }

    public boolean removeStack(int id) {
        this.database.beginTransaction();
        try {
            boolean ret = this.database.delete("filterstack", "_id = ?", new String[]{new StringBuilder().append("").append(id).toString()}) != 0;
            this.database.setTransactionSuccessful();
            return ret;
        } finally {
            this.database.endTransaction();
        }
    }

    public ArrayList<FilterUserPresetRepresentation> getAllUserPresets() {
        ArrayList<FilterUserPresetRepresentation> ret = new ArrayList<>();
        Cursor c = null;
        this.database.beginTransaction();
        try {
            c = this.database.query("filterstack", new String[]{"_id", "stack_id", "stack"}, null, null, null, null, null, null);
            if (c != null) {
                for (boolean loopCheck = c.moveToFirst(); loopCheck; loopCheck = c.moveToNext()) {
                    int id = c.getInt(0);
                    String name = c.isNull(1) ? null : c.getString(1);
                    byte[] b = c.isNull(2) ? null : c.getBlob(2);
                    String json = new String(b);
                    ImagePreset preset = new ImagePreset();
                    preset.readJsonFromString(json);
                    FilterUserPresetRepresentation representation = new FilterUserPresetRepresentation(name, preset, id);
                    ret.add(representation);
                }
            }
            this.database.setTransactionSuccessful();
            return ret;
        } finally {
            if (c != null) {
                c.close();
            }
            this.database.endTransaction();
        }
    }
}
