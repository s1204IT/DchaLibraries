package com.android.gallery3d.gadget;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import com.android.gallery3d.common.Utils;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class WidgetDatabaseHelper extends SQLiteOpenHelper {
    private static final String[] PROJECTION = {"widgetType", "imageUri", "photoBlob", "albumPath", "appWidgetId", "relativePath"};

    public static class Entry {
        public String albumPath;
        public byte[] imageData;
        public String imageUri;
        public String relativePath;
        public int type;
        public int widgetId;

        private Entry() {
        }

        private Entry(int id, Cursor cursor) {
            this.widgetId = id;
            this.type = cursor.getInt(0);
            if (this.type == 0) {
                this.imageUri = cursor.getString(1);
                this.imageData = cursor.getBlob(2);
            } else if (this.type == 2) {
                this.albumPath = cursor.getString(3);
                this.relativePath = cursor.getString(5);
            }
        }

        private Entry(Cursor cursor) {
            this(cursor.getInt(4), cursor);
        }
    }

    public WidgetDatabaseHelper(Context context) {
        super(context, "launcher.db", (SQLiteDatabase.CursorFactory) null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE widgets (appWidgetId INTEGER PRIMARY KEY, widgetType INTEGER DEFAULT 0, imageUri TEXT, albumPath TEXT, photoBlob BLOB, relativePath TEXT)");
    }

    private void saveData(SQLiteDatabase db, int oldVersion, ArrayList<Entry> data) {
        Cursor cursor;
        if (oldVersion <= 2) {
            cursor = db.query("photos", new String[]{"appWidgetId", "photoBlob"}, null, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    try {
                        Entry entry = new Entry();
                        entry.type = 0;
                        entry.widgetId = cursor.getInt(0);
                        entry.imageData = cursor.getBlob(1);
                        data.add(entry);
                    } finally {
                    }
                }
                return;
            }
            return;
        }
        if (oldVersion == 3 && (cursor = db.query("photos", new String[]{"appWidgetId", "photoBlob", "imageUri"}, null, null, null, null, null)) != null) {
            while (cursor.moveToNext()) {
                try {
                    Entry entry2 = new Entry();
                    entry2.type = 0;
                    entry2.widgetId = cursor.getInt(0);
                    entry2.imageData = cursor.getBlob(1);
                    entry2.imageUri = cursor.getString(2);
                    data.add(entry2);
                } finally {
                }
            }
        }
    }

    private void restoreData(SQLiteDatabase db, ArrayList<Entry> data) {
        db.beginTransaction();
        try {
            for (Entry entry : data) {
                ContentValues values = new ContentValues();
                values.put("appWidgetId", Integer.valueOf(entry.widgetId));
                values.put("widgetType", Integer.valueOf(entry.type));
                values.put("imageUri", entry.imageUri);
                values.put("photoBlob", entry.imageData);
                values.put("albumPath", entry.albumPath);
                db.insert("widgets", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            ArrayList<Entry> data = new ArrayList<>();
            saveData(db, oldVersion, data);
            Log.w("PhotoDatabaseHelper", "destroying all old data.");
            db.execSQL("DROP TABLE IF EXISTS photos");
            db.execSQL("DROP TABLE IF EXISTS widgets");
            onCreate(db);
            restoreData(db, data);
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE widgets ADD COLUMN relativePath TEXT");
            } catch (Throwable th) {
                Log.e("PhotoDatabaseHelper", "Failed to add the column for relative path.");
            }
        }
    }

    public boolean setPhoto(int appWidgetId, Uri imageUri, Bitmap bitmap) {
        try {
            int size = bitmap.getWidth() * bitmap.getHeight() * 4;
            ByteArrayOutputStream out = new ByteArrayOutputStream(size);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            ContentValues values = new ContentValues();
            values.put("appWidgetId", Integer.valueOf(appWidgetId));
            values.put("widgetType", (Integer) 0);
            values.put("imageUri", imageUri.toString());
            values.put("photoBlob", out.toByteArray());
            SQLiteDatabase db = getWritableDatabase();
            db.replaceOrThrow("widgets", null, values);
            return true;
        } catch (Throwable e) {
            Log.e("PhotoDatabaseHelper", "set widget photo fail", e);
            return false;
        }
    }

    public boolean setWidget(int id, int type, String albumPath, String relativePath) {
        try {
            ContentValues values = new ContentValues();
            values.put("appWidgetId", Integer.valueOf(id));
            values.put("widgetType", Integer.valueOf(type));
            values.put("albumPath", Utils.ensureNotNull(albumPath));
            values.put("relativePath", relativePath);
            getWritableDatabase().replaceOrThrow("widgets", null, values);
            return true;
        } catch (Throwable e) {
            Log.e("PhotoDatabaseHelper", "set widget fail", e);
            return false;
        }
    }

    public Entry getEntry(int appWidgetId) {
        Entry entry;
        Cursor cursor = null;
        try {
            try {
                SQLiteDatabase db = getReadableDatabase();
                cursor = db.query("widgets", PROJECTION, "appWidgetId = ?", new String[]{String.valueOf(appWidgetId)}, null, null, null);
                if (cursor == null || !cursor.moveToNext()) {
                    Log.e("PhotoDatabaseHelper", "query fail: empty cursor: " + cursor + " appWidgetId: " + appWidgetId);
                    Utils.closeSilently(cursor);
                    entry = null;
                } else {
                    entry = new Entry(appWidgetId, cursor);
                }
            } catch (Throwable e) {
                Log.e("PhotoDatabaseHelper", "Could not load photo from database", e);
                Utils.closeSilently(cursor);
                entry = null;
            }
            return entry;
        } finally {
            Utils.closeSilently(cursor);
        }
    }

    public List<Entry> getEntries(int type) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query("widgets", PROJECTION, "widgetType = ?", new String[]{String.valueOf(type)}, null, null, null);
            if (cursor == null) {
                Log.e("PhotoDatabaseHelper", "query fail: null cursor: " + cursor);
                return null;
            }
            ArrayList<Entry> result = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                result.add(new Entry(cursor));
            }
            return result;
        } catch (Throwable e) {
            Log.e("PhotoDatabaseHelper", "Could not load widget from database", e);
            return null;
        } finally {
            Utils.closeSilently(cursor);
        }
    }

    public void updateEntry(Entry entry) {
        deleteEntry(entry.widgetId);
        try {
            ContentValues values = new ContentValues();
            values.put("appWidgetId", Integer.valueOf(entry.widgetId));
            values.put("widgetType", Integer.valueOf(entry.type));
            values.put("albumPath", entry.albumPath);
            values.put("imageUri", entry.imageUri);
            values.put("photoBlob", entry.imageData);
            values.put("relativePath", entry.relativePath);
            getWritableDatabase().insert("widgets", null, values);
        } catch (Throwable e) {
            Log.e("PhotoDatabaseHelper", "set widget fail", e);
        }
    }

    public void deleteEntry(int appWidgetId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete("widgets", "appWidgetId = ?", new String[]{String.valueOf(appWidgetId)});
        } catch (SQLiteException e) {
            Log.e("PhotoDatabaseHelper", "Could not delete photo from database", e);
        }
    }
}
