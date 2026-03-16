package com.android.gallery3d.common;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.android.gallery3d.common.Entry;
import java.io.Closeable;
import java.io.File;

public class FileCache implements Closeable {
    private DatabaseHelper mDbHelper;
    private File mRootDir;
    private static final String TABLE_NAME = FileEntry.SCHEMA.getTableName();
    private static final String[] PROJECTION_SIZE_SUM = {String.format("sum(%s)", "size")};
    private static final String[] FREESPACE_PROJECTION = {"_id", "filename", "content_url", "size"};
    private static final String FREESPACE_ORDER_BY = String.format("%s ASC", "last_access");

    @Override
    public void close() {
        this.mDbHelper.close();
    }

    @Entry.Table("files")
    private static class FileEntry extends Entry {
        public static final EntrySchema SCHEMA = new EntrySchema(FileEntry.class);

        @Entry.Column("content_url")
        public String contentUrl;

        @Entry.Column("filename")
        public String filename;

        @Entry.Column(indexed = true, value = "hash_code")
        public long hashCode;

        @Entry.Column(indexed = true, value = "last_access")
        public long lastAccess;

        @Entry.Column("size")
        public long size;

        private FileEntry() {
        }

        public String toString() {
            return "hash_code: " + this.hashCode + ", content_url" + this.contentUrl + ", last_access" + this.lastAccess + ", filename" + this.filename;
        }
    }

    private final class DatabaseHelper extends SQLiteOpenHelper {
        final FileCache this$0;

        @Override
        public void onCreate(SQLiteDatabase db) {
            FileEntry.SCHEMA.createTables(db);
            File[] arr$ = this.this$0.mRootDir.listFiles();
            for (File file : arr$) {
                if (!file.delete()) {
                    Log.w("FileCache", "fail to remove: " + file.getAbsolutePath());
                }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            FileEntry.SCHEMA.dropTables(db);
            onCreate(db);
        }
    }
}
