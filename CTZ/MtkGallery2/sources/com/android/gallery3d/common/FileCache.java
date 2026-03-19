package com.android.gallery3d.common;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.android.gallery3d.common.Entry;
import com.android.gallery3d.util.Log;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import java.io.Closeable;
import java.io.File;

public class FileCache implements Closeable {
    private DatabaseHelper mDbHelper;
    private File mRootDir;
    private static final String TABLE_NAME = FileEntry.SCHEMA.getTableName();
    private static final String[] PROJECTION_SIZE_SUM = {String.format("sum(%s)", "size")};
    private static final String[] FREESPACE_PROJECTION = {BookmarkEnhance.COLUMN_ID, "filename", "content_url", "size"};
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
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            FileEntry.SCHEMA.createTables(sQLiteDatabase);
            for (File file : this.this$0.mRootDir.listFiles()) {
                if (!file.delete()) {
                    Log.w("Gallery2/FileCache", "fail to remove: " + file.getAbsolutePath());
                }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            FileEntry.SCHEMA.dropTables(sQLiteDatabase);
            onCreate(sQLiteDatabase);
        }
    }
}
