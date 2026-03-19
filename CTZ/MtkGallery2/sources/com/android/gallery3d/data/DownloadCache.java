package com.android.gallery3d.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.LruCache;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class DownloadCache {
    private final GalleryApp mApplication;
    private final long mCapacity;
    private final SQLiteDatabase mDatabase;
    private final File mRoot;
    private static final String TABLE_NAME = DownloadEntry.SCHEMA.getTableName();
    private static final String[] QUERY_PROJECTION = {BookmarkEnhance.COLUMN_ID, BookmarkEnhance.COLUMN_DATA};
    private static final String WHERE_HASH_AND_URL = String.format("%s = ? AND %s = ?", "hash_code", "content_url");
    private static final String[] FREESPACE_PROJECTION = {BookmarkEnhance.COLUMN_ID, BookmarkEnhance.COLUMN_DATA, "content_url", "_size"};
    private static final String FREESPACE_ORDER_BY = String.format("%s ASC", "last_access");
    private static final String[] SUM_PROJECTION = {String.format("sum(%s)", "_size")};
    private final LruCache<String, Entry> mEntryMap = new LruCache<>(4);
    private final HashMap<String, DownloadTask> mTaskMap = new HashMap<>();
    private long mTotalBytes = 0;
    private boolean mInitialized = false;

    public DownloadCache(GalleryApp galleryApp, File file, long j) {
        this.mRoot = (File) Utils.checkNotNull(file);
        this.mApplication = (GalleryApp) Utils.checkNotNull(galleryApp);
        this.mCapacity = j;
        this.mDatabase = new DatabaseHelper(galleryApp.getAndroidContext()).getWritableDatabase();
    }

    private Entry findEntryInDatabase(String str) {
        Entry entry;
        Cursor cursorQuery = this.mDatabase.query(TABLE_NAME, QUERY_PROJECTION, WHERE_HASH_AND_URL, new String[]{String.valueOf(Utils.crc64Long(str)), str}, null, null, null);
        try {
            if (cursorQuery.moveToNext()) {
                File file = new File(cursorQuery.getString(1));
                long j = cursorQuery.getInt(0);
                synchronized (this.mEntryMap) {
                    entry = this.mEntryMap.get(str);
                    if (entry == null) {
                        entry = new Entry(j, file);
                        this.mEntryMap.put(str, entry);
                    }
                }
                return entry;
            }
            cursorQuery.close();
            return null;
        } finally {
            cursorQuery.close();
        }
    }

    public Entry download(ThreadPool.JobContext jobContext, URL url) {
        if (!this.mInitialized) {
            initialize();
        }
        String string = url.toString();
        synchronized (this.mEntryMap) {
            Entry entry = this.mEntryMap.get(string);
            if (entry != null) {
                updateLastAccess(entry.mId);
                return entry;
            }
            TaskProxy taskProxy = new TaskProxy();
            synchronized (this.mTaskMap) {
                Entry entryFindEntryInDatabase = findEntryInDatabase(string);
                if (entryFindEntryInDatabase != null) {
                    updateLastAccess(entryFindEntryInDatabase.mId);
                    return entryFindEntryInDatabase;
                }
                DownloadTask downloadTask = this.mTaskMap.get(string);
                if (downloadTask == null) {
                    downloadTask = new DownloadTask(string);
                    this.mTaskMap.put(string, downloadTask);
                    downloadTask.mFuture = this.mApplication.getThreadPool().submit(downloadTask, downloadTask);
                }
                downloadTask.addProxy(taskProxy);
                return taskProxy.get(jobContext);
            }
        }
    }

    private void updateLastAccess(long j) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("last_access", Long.valueOf(System.currentTimeMillis()));
        this.mDatabase.update(TABLE_NAME, contentValues, "_id = ?", new String[]{String.valueOf(j)});
    }

    private synchronized void freeSomeSpaceIfNeed(int i) {
        boolean zContainsKey;
        if (this.mTotalBytes <= this.mCapacity) {
            return;
        }
        Cursor cursorQuery = this.mDatabase.query(TABLE_NAME, FREESPACE_PROJECTION, null, null, null, null, FREESPACE_ORDER_BY);
        while (i > 0) {
            try {
                if (this.mTotalBytes <= this.mCapacity || !cursorQuery.moveToNext()) {
                    break;
                }
                long j = cursorQuery.getLong(0);
                String string = cursorQuery.getString(2);
                long j2 = cursorQuery.getLong(3);
                String string2 = cursorQuery.getString(1);
                synchronized (this.mEntryMap) {
                    zContainsKey = this.mEntryMap.containsKey(string);
                }
                if (!zContainsKey) {
                    i--;
                    this.mTotalBytes -= j2;
                    new File(string2).delete();
                    this.mDatabase.delete(TABLE_NAME, "_id = ?", new String[]{String.valueOf(j)});
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private synchronized long insertEntry(String str, File file) {
        ContentValues contentValues;
        long length = file.length();
        this.mTotalBytes += length;
        contentValues = new ContentValues();
        String strValueOf = String.valueOf(Utils.crc64Long(str));
        contentValues.put(BookmarkEnhance.COLUMN_DATA, file.getAbsolutePath());
        contentValues.put("hash_code", strValueOf);
        contentValues.put("content_url", str);
        contentValues.put("_size", Long.valueOf(length));
        contentValues.put("last_updated", Long.valueOf(System.currentTimeMillis()));
        return this.mDatabase.insert(TABLE_NAME, "", contentValues);
    }

    private synchronized void initialize() {
        if (this.mInitialized) {
            return;
        }
        this.mInitialized = true;
        if (!this.mRoot.isDirectory()) {
            this.mRoot.mkdirs();
        }
        if (!this.mRoot.isDirectory()) {
            throw new RuntimeException("cannot create " + this.mRoot.getAbsolutePath());
        }
        Cursor cursorQuery = this.mDatabase.query(TABLE_NAME, SUM_PROJECTION, null, null, null, null, null);
        this.mTotalBytes = 0L;
        try {
            if (cursorQuery.moveToNext()) {
                this.mTotalBytes = cursorQuery.getLong(0);
            }
            cursorQuery.close();
            if (this.mTotalBytes > this.mCapacity) {
                freeSomeSpaceIfNeed(16);
            }
        } catch (Throwable th) {
            cursorQuery.close();
            throw th;
        }
    }

    private final class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "download.db", (SQLiteDatabase.CursorFactory) null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            DownloadEntry.SCHEMA.createTables(sQLiteDatabase);
            for (File file : DownloadCache.this.mRoot.listFiles()) {
                if (!file.delete()) {
                    Log.w("Gallery2/DownloadCache", "fail to remove: " + file.getAbsolutePath());
                }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            DownloadEntry.SCHEMA.dropTables(sQLiteDatabase);
            onCreate(sQLiteDatabase);
        }
    }

    public class Entry {
        public File cacheFile;
        protected long mId;

        Entry(long j, File file) {
            this.mId = j;
            this.cacheFile = (File) Utils.checkNotNull(file);
        }
    }

    private class DownloadTask implements FutureListener<File>, ThreadPool.Job<File> {
        private Future<File> mFuture;
        private HashSet<TaskProxy> mProxySet = new HashSet<>();
        private final String mUrl;

        public DownloadTask(String str) {
            this.mUrl = (String) Utils.checkNotNull(str);
        }

        public void removeProxy(TaskProxy taskProxy) {
            synchronized (DownloadCache.this.mTaskMap) {
                Utils.assertTrue(this.mProxySet.remove(taskProxy));
                if (this.mProxySet.isEmpty()) {
                    this.mFuture.cancel();
                    DownloadCache.this.mTaskMap.remove(this.mUrl);
                }
            }
        }

        public void addProxy(TaskProxy taskProxy) {
            taskProxy.mTask = this;
            this.mProxySet.add(taskProxy);
        }

        @Override
        public void onFutureDone(Future<File> future) {
            long jInsertEntry;
            File file = future.get();
            if (file != null) {
                jInsertEntry = DownloadCache.this.insertEntry(this.mUrl, file);
            } else {
                jInsertEntry = 0;
            }
            if (!future.isCancelled()) {
                synchronized (DownloadCache.this.mTaskMap) {
                    Entry entry = null;
                    synchronized (DownloadCache.this.mEntryMap) {
                        if (file != null) {
                            try {
                                entry = DownloadCache.this.new Entry(jInsertEntry, file);
                                Utils.assertTrue(DownloadCache.this.mEntryMap.put(this.mUrl, entry) == null);
                            } finally {
                            }
                        }
                    }
                    Iterator<TaskProxy> it = this.mProxySet.iterator();
                    while (it.hasNext()) {
                        it.next().setResult(entry);
                    }
                    DownloadCache.this.mTaskMap.remove(this.mUrl);
                    DownloadCache.this.freeSomeSpaceIfNeed(16);
                }
                return;
            }
            Utils.assertTrue(this.mProxySet.isEmpty());
        }

        @Override
        public File run(ThreadPool.JobContext jobContext) {
            File fileCreateTempFile;
            URL url;
            jobContext.setMode(2);
            try {
                try {
                    url = new URL(this.mUrl);
                    fileCreateTempFile = File.createTempFile("cache", ".tmp", DownloadCache.this.mRoot);
                } catch (Exception e) {
                    e = e;
                    fileCreateTempFile = null;
                }
                try {
                    jobContext.setMode(2);
                    boolean zRequestDownload = DownloadUtils.requestDownload(jobContext, url, fileCreateTempFile);
                    jobContext.setMode(0);
                    if (zRequestDownload) {
                        return fileCreateTempFile;
                    }
                } catch (Exception e2) {
                    e = e2;
                    Log.e("Gallery2/DownloadCache", String.format("fail to download %s", this.mUrl), e);
                }
                if (fileCreateTempFile != null) {
                    fileCreateTempFile.delete();
                }
                return null;
            } finally {
                jobContext.setMode(0);
            }
        }
    }

    public static class TaskProxy {
        private Entry mEntry;
        private boolean mIsCancelled = false;
        private DownloadTask mTask;

        synchronized void setResult(Entry entry) {
            if (this.mIsCancelled) {
                return;
            }
            this.mEntry = entry;
            notifyAll();
        }

        public synchronized Entry get(ThreadPool.JobContext jobContext) {
            jobContext.setCancelListener(new ThreadPool.CancelListener() {
                @Override
                public void onCancel() {
                    TaskProxy.this.mTask.removeProxy(TaskProxy.this);
                    synchronized (TaskProxy.this) {
                        TaskProxy.this.mIsCancelled = true;
                        TaskProxy.this.notifyAll();
                    }
                }
            });
            while (!this.mIsCancelled && this.mEntry == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.w("Gallery2/DownloadCache", "ignore interrupt", e);
                }
            }
            jobContext.setCancelListener(null);
            return this.mEntry;
        }
    }
}
