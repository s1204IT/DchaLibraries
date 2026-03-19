package com.android.gallery3d.gadget;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.net.Uri;
import com.android.gallery3d.common.Utils;
import com.mediatek.gallery3d.util.Log;
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

        private Entry(int i, Cursor cursor) {
            this.widgetId = i;
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
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE widgets (appWidgetId INTEGER PRIMARY KEY, widgetType INTEGER DEFAULT 0, imageUri TEXT, albumPath TEXT, photoBlob BLOB, relativePath TEXT)");
    }

    private void saveData(SQLiteDatabase sQLiteDatabase, int i, ArrayList<Entry> arrayList) {
        Cursor cursorQuery;
        if (i <= 2) {
            cursorQuery = sQLiteDatabase.query("photos", new String[]{"appWidgetId", "photoBlob"}, null, null, null, null, null);
            if (cursorQuery == null) {
                return;
            }
            while (cursorQuery.moveToNext()) {
                try {
                    Entry entry = new Entry();
                    entry.type = 0;
                    entry.widgetId = cursorQuery.getInt(0);
                    entry.imageData = cursorQuery.getBlob(1);
                    arrayList.add(entry);
                } finally {
                }
            }
            return;
        }
        if (i != 3 || (cursorQuery = sQLiteDatabase.query("photos", new String[]{"appWidgetId", "photoBlob", "imageUri"}, null, null, null, null, null)) == null) {
            return;
        }
        while (cursorQuery.moveToNext()) {
            try {
                Entry entry2 = new Entry();
                entry2.type = 0;
                entry2.widgetId = cursorQuery.getInt(0);
                entry2.imageData = cursorQuery.getBlob(1);
                entry2.imageUri = cursorQuery.getString(2);
                arrayList.add(entry2);
            } finally {
            }
        }
    }

    private void restoreData(SQLiteDatabase sQLiteDatabase, ArrayList<Entry> arrayList) {
        sQLiteDatabase.beginTransaction();
        try {
            for (Entry entry : arrayList) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("appWidgetId", Integer.valueOf(entry.widgetId));
                contentValues.put("widgetType", Integer.valueOf(entry.type));
                contentValues.put("imageUri", entry.imageUri);
                contentValues.put("photoBlob", entry.imageData);
                contentValues.put("albumPath", entry.albumPath);
                sQLiteDatabase.insert("widgets", null, contentValues);
            }
            sQLiteDatabase.setTransactionSuccessful();
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i < 4) {
            ArrayList<Entry> arrayList = new ArrayList<>();
            saveData(sQLiteDatabase, i, arrayList);
            Log.w("Gallery2/PhotoDatabaseHelper", "destroying all old data.");
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS photos");
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS widgets");
            onCreate(sQLiteDatabase);
            restoreData(sQLiteDatabase, arrayList);
        }
        if (i < 5) {
            try {
                sQLiteDatabase.execSQL("ALTER TABLE widgets ADD COLUMN relativePath TEXT");
            } catch (Throwable th) {
                Log.e("Gallery2/PhotoDatabaseHelper", "Failed to add the column for relative path.");
            }
        }
    }

    public boolean setPhoto(int i, Uri uri, Bitmap bitmap) {
        if (uri == null) {
            Log.e("Gallery2/PhotoDatabaseHelper", "<setPhoto>set widget photo fail, imageUri = null");
            return false;
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bitmap.getWidth() * bitmap.getHeight() * 4);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byteArrayOutputStream.close();
            ContentValues contentValues = new ContentValues();
            contentValues.put("appWidgetId", Integer.valueOf(i));
            contentValues.put("widgetType", (Integer) 0);
            contentValues.put("imageUri", uri.toString());
            contentValues.put("photoBlob", byteArrayOutputStream.toByteArray());
            getWritableDatabase().replaceOrThrow("widgets", null, contentValues);
            return true;
        } catch (Throwable th) {
            Log.e("Gallery2/PhotoDatabaseHelper", "set widget photo fail", th);
            return false;
        }
    }

    public boolean setWidget(int i, int i2, String str, String str2) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("appWidgetId", Integer.valueOf(i));
            contentValues.put("widgetType", Integer.valueOf(i2));
            contentValues.put("albumPath", Utils.ensureNotNull(str));
            contentValues.put("relativePath", str2);
            getWritableDatabase().replaceOrThrow("widgets", null, contentValues);
            return true;
        } catch (Throwable th) {
            Log.e("Gallery2/PhotoDatabaseHelper", "set widget fail", th);
            return false;
        }
    }

    public Entry getEntry(int i) throws Throwable {
        Cursor cursor;
        SQLiteDatabase readableDatabase;
        Cursor cursorQuery;
        try {
            try {
                readableDatabase = getReadableDatabase();
            } catch (Throwable th) {
                th = th;
            }
            try {
                cursorQuery = readableDatabase.query("widgets", PROJECTION, "appWidgetId = ?", new String[]{String.valueOf(i)}, null, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToNext()) {
                            Entry entry = new Entry(i, cursorQuery);
                            Utils.closeSilently(cursorQuery);
                            if (readableDatabase != null) {
                                readableDatabase.close();
                            }
                            return entry;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        Log.e("Gallery2/PhotoDatabaseHelper", "Could not load photo from database", th);
                        Utils.closeSilently(cursorQuery);
                        if (readableDatabase != null) {
                            readableDatabase.close();
                        }
                        return null;
                    }
                }
                Log.e("Gallery2/PhotoDatabaseHelper", "query fail: empty cursor: " + cursorQuery + " appWidgetId: " + i);
                Utils.closeSilently(cursorQuery);
                if (readableDatabase != null) {
                    readableDatabase.close();
                }
                return null;
            } catch (Throwable th3) {
                th = th3;
                cursorQuery = null;
            }
        } catch (Throwable th4) {
            th = th4;
            cursor = null;
            readableDatabase = null;
        }
    }

    public List<Entry> getEntries(int i) throws Throwable {
        Throwable th;
        Cursor cursorQuery;
        try {
            try {
                cursorQuery = getReadableDatabase().query("widgets", PROJECTION, "widgetType = ?", new String[]{String.valueOf((int) i)}, null, null, null);
                try {
                    if (cursorQuery != null) {
                        ArrayList arrayList = new ArrayList(cursorQuery.getCount());
                        while (cursorQuery.moveToNext()) {
                            arrayList.add(new Entry(cursorQuery));
                        }
                        Utils.closeSilently(cursorQuery);
                        return arrayList;
                    }
                    Log.e("Gallery2/PhotoDatabaseHelper", "query fail: null cursor: " + cursorQuery);
                    Utils.closeSilently(cursorQuery);
                    return null;
                } catch (Throwable th2) {
                    th = th2;
                    Log.e("Gallery2/PhotoDatabaseHelper", "Could not load widget from database", th);
                    Utils.closeSilently(cursorQuery);
                    return null;
                }
            } catch (Throwable th3) {
                th = th3;
                Utils.closeSilently((Cursor) i);
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            i = 0;
            Utils.closeSilently((Cursor) i);
            throw th;
        }
    }

    public void updateEntry(Entry entry) {
        deleteEntry(entry.widgetId);
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("appWidgetId", Integer.valueOf(entry.widgetId));
            contentValues.put("widgetType", Integer.valueOf(entry.type));
            contentValues.put("albumPath", entry.albumPath);
            contentValues.put("imageUri", entry.imageUri);
            contentValues.put("photoBlob", entry.imageData);
            contentValues.put("relativePath", entry.relativePath);
            getWritableDatabase().insert("widgets", null, contentValues);
        } catch (Throwable th) {
            Log.e("Gallery2/PhotoDatabaseHelper", "set widget fail", th);
        }
    }

    public void deleteEntry(int i) {
        try {
            getWritableDatabase().delete("widgets", "appWidgetId = ?", new String[]{String.valueOf(i)});
        } catch (SQLiteException e) {
            Log.e("Gallery2/PhotoDatabaseHelper", "Could not delete photo from database", e);
        }
    }
}
