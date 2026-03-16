package com.android.photos.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class PhotoDatabase extends SQLiteOpenHelper {
    private static final String TAG = PhotoDatabase.class.getSimpleName();
    private static final String[][] CREATE_PHOTO = {new String[]{"_id", "INTEGER PRIMARY KEY AUTOINCREMENT"}, new String[]{"account_id", "INTEGER NOT NULL"}, new String[]{"width", "INTEGER NOT NULL"}, new String[]{"height", "INTEGER NOT NULL"}, new String[]{"date_taken", "INTEGER NOT NULL"}, new String[]{"album_id", "INTEGER"}, new String[]{"mime_type", "TEXT NOT NULL"}, new String[]{"title", "TEXT"}, new String[]{"date_modified", "INTEGER"}, new String[]{"rotation", "INTEGER"}};
    private static final String[][] CREATE_ALBUM = {new String[]{"_id", "INTEGER PRIMARY KEY AUTOINCREMENT"}, new String[]{"account_id", "INTEGER NOT NULL"}, new String[]{"parent_id", "INTEGER"}, new String[]{"album_type", "TEXT"}, new String[]{"visibility", "INTEGER NOT NULL"}, new String[]{"location_string", "TEXT"}, new String[]{"title", "TEXT NOT NULL"}, new String[]{"summary", "TEXT"}, new String[]{"date_published", "INTEGER"}, new String[]{"date_modified", "INTEGER"}, createUniqueConstraint("parent_id", "title")};
    private static final String[][] CREATE_METADATA = {new String[]{"_id", "INTEGER PRIMARY KEY AUTOINCREMENT"}, new String[]{"photo_id", "INTEGER NOT NULL"}, new String[]{"key", "TEXT NOT NULL"}, new String[]{"value", "TEXT NOT NULL"}, createUniqueConstraint("photo_id", "key")};
    private static final String[][] CREATE_ACCOUNT = {new String[]{"_id", "INTEGER PRIMARY KEY AUTOINCREMENT"}, new String[]{"name", "TEXT UNIQUE NOT NULL"}};

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db, "accounts", getAccountTableDefinition());
        createTable(db, "albums", getAlbumTableDefinition());
        createTable(db, "photos", getPhotoTableDefinition());
        createTable(db, "metadata", getMetadataTableDefinition());
    }

    public PhotoDatabase(Context context, String dbName) {
        super(context, dbName, (SQLiteDatabase.CursorFactory) null, 3);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        recreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        recreate(db);
    }

    private void recreate(SQLiteDatabase db) {
        dropTable(db, "metadata");
        dropTable(db, "photos");
        dropTable(db, "albums");
        dropTable(db, "accounts");
        onCreate(db);
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

    protected static void createTable(SQLiteDatabase db, String table, List<String[]> columns) {
        StringBuilder create = new StringBuilder("CREATE TABLE ");
        create.append(table).append('(');
        boolean first = true;
        for (String[] column : columns) {
            if (!first) {
                create.append(',');
            }
            first = false;
            for (String val : column) {
                create.append(val).append(' ');
            }
        }
        create.append(')');
        db.beginTransaction();
        try {
            db.execSQL(create.toString());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    protected static String[] createUniqueConstraint(String column1, String column2) {
        return new String[]{"UNIQUE(", column1, ",", column2, ")"};
    }

    protected static List<String[]> tableCreationStrings(String[][] createTable) {
        ArrayList<String[]> create = new ArrayList<>(createTable.length);
        for (String[] line : createTable) {
            create.add(line);
        }
        return create;
    }

    protected static void dropTable(SQLiteDatabase db, String table) {
        db.beginTransaction();
        try {
            db.execSQL("drop table if exists " + table);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
