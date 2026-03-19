package com.android.photos.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.plugin.builder.PluginDescriptorBuilder;
import java.util.ArrayList;
import java.util.List;

public class PhotoDatabase extends SQLiteOpenHelper {
    private static final String TAG = PhotoDatabase.class.getSimpleName();
    private static final String[][] CREATE_PHOTO = {new String[]{BookmarkEnhance.COLUMN_ID, "INTEGER PRIMARY KEY AUTOINCREMENT"}, new String[]{"account_id", "INTEGER NOT NULL"}, new String[]{"width", "INTEGER NOT NULL"}, new String[]{"height", "INTEGER NOT NULL"}, new String[]{"date_taken", "INTEGER NOT NULL"}, new String[]{"album_id", "INTEGER"}, new String[]{BookmarkEnhance.COLUMN_MEDIA_TYPE, "TEXT NOT NULL"}, new String[]{"title", "TEXT"}, new String[]{"date_modified", "INTEGER"}, new String[]{"rotation", "INTEGER"}};
    private static final String[][] CREATE_ALBUM = {new String[]{BookmarkEnhance.COLUMN_ID, "INTEGER PRIMARY KEY AUTOINCREMENT"}, new String[]{"account_id", "INTEGER NOT NULL"}, new String[]{"parent_id", "INTEGER"}, new String[]{"album_type", "TEXT"}, new String[]{"visibility", "INTEGER NOT NULL"}, new String[]{"location_string", "TEXT"}, new String[]{"title", "TEXT NOT NULL"}, new String[]{"summary", "TEXT"}, new String[]{"date_published", "INTEGER"}, new String[]{"date_modified", "INTEGER"}, createUniqueConstraint("parent_id", "title")};
    private static final String[][] CREATE_METADATA = {new String[]{BookmarkEnhance.COLUMN_ID, "INTEGER PRIMARY KEY AUTOINCREMENT"}, new String[]{"photo_id", "INTEGER NOT NULL"}, new String[]{"key", "TEXT NOT NULL"}, new String[]{"value", "TEXT NOT NULL"}, createUniqueConstraint("photo_id", "key")};
    private static final String[][] CREATE_ACCOUNT = {new String[]{BookmarkEnhance.COLUMN_ID, "INTEGER PRIMARY KEY AUTOINCREMENT"}, new String[]{PluginDescriptorBuilder.VALUE_NAME, "TEXT UNIQUE NOT NULL"}};

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        createTable(sQLiteDatabase, "accounts", getAccountTableDefinition());
        createTable(sQLiteDatabase, "albums", getAlbumTableDefinition());
        createTable(sQLiteDatabase, "photos", getPhotoTableDefinition());
        createTable(sQLiteDatabase, "metadata", getMetadataTableDefinition());
    }

    public PhotoDatabase(Context context, String str) {
        super(context, str, (SQLiteDatabase.CursorFactory) null, 3);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        recreate(sQLiteDatabase);
    }

    @Override
    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        recreate(sQLiteDatabase);
    }

    private void recreate(SQLiteDatabase sQLiteDatabase) {
        dropTable(sQLiteDatabase, "metadata");
        dropTable(sQLiteDatabase, "photos");
        dropTable(sQLiteDatabase, "albums");
        dropTable(sQLiteDatabase, "accounts");
        onCreate(sQLiteDatabase);
    }

    protected List<String[]> getAlbumTableDefinition() {
        return tableCreationStrings(CREATE_ALBUM);
    }

    protected List<String[]> getPhotoTableDefinition() {
        return tableCreationStrings(CREATE_PHOTO);
    }

    protected List<String[]> getMetadataTableDefinition() {
        return tableCreationStrings(CREATE_METADATA);
    }

    protected List<String[]> getAccountTableDefinition() {
        return tableCreationStrings(CREATE_ACCOUNT);
    }

    protected static void createTable(SQLiteDatabase sQLiteDatabase, String str, List<String[]> list) {
        StringBuilder sb = new StringBuilder("CREATE TABLE ");
        sb.append(str);
        sb.append('(');
        boolean z = true;
        for (String[] strArr : list) {
            if (!z) {
                sb.append(',');
            }
            for (String str2 : strArr) {
                sb.append(str2);
                sb.append(' ');
            }
            z = false;
        }
        sb.append(')');
        sQLiteDatabase.beginTransaction();
        try {
            sQLiteDatabase.execSQL(sb.toString());
            sQLiteDatabase.setTransactionSuccessful();
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    protected static String[] createUniqueConstraint(String str, String str2) {
        return new String[]{"UNIQUE(", str, ",", str2, ")"};
    }

    protected static List<String[]> tableCreationStrings(String[][] strArr) {
        ArrayList arrayList = new ArrayList(strArr.length);
        for (String[] strArr2 : strArr) {
            arrayList.add(strArr2);
        }
        return arrayList;
    }

    protected static void dropTable(SQLiteDatabase sQLiteDatabase, String str) {
        sQLiteDatabase.beginTransaction();
        try {
            sQLiteDatabase.execSQL("drop table if exists " + str);
            sQLiteDatabase.setTransactionSuccessful();
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }
}
