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
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;

public class DownloadCache {
    private final GalleryApp mApplication;
    private final long mCapacity;
    private final SQLiteDatabase mDatabase;
    private final File mRoot;
    private static final String TABLE_NAME = DownloadEntry.SCHEMA.getTableName();
    private static final String[] QUERY_PROJECTION = {"_id", "_data"};
    private static final String WHERE_HASH_AND_URL = String.format("%s = ? AND %s = ?", "hash_code", "content_url");
    private static final String[] FREESPACE_PROJECTION = {"_id", "_data", "content_url", "_size"};
    private static final String FREESPACE_ORDER_BY = String.format("%s ASC", "last_access");
    private static final String[] SUM_PROJECTION = {String.format("sum(%s)", "_size")};
    private final LruCache<String, Entry> mEntryMap = new LruCache<>(4);
    private final HashMap<String, DownloadTask> mTaskMap = new HashMap<>();
    private long mTotalBytes = 0;
    private boolean mInitialized = false;

    public DownloadCache(GalleryApp application, File root, long capacity) {
        this.mRoot = (File) Utils.checkNotNull(root);
        this.mApplication = (GalleryApp) Utils.checkNotNull(application);
        this.mCapacity = capacity;
        this.mDatabase = new DatabaseHelper(application.getAndroidContext()).getWritableDatabase();
    }

    private Entry findEntryInDatabase(String stringUrl) {
        long hash = Utils.crc64Long(stringUrl);
        String[] whereArgs = {String.valueOf(hash), stringUrl};
        Cursor cursor = this.mDatabase.query(TABLE_NAME, QUERY_PROJECTION, WHERE_HASH_AND_URL, whereArgs, null, null, null);
        try {
            if (cursor.moveToNext()) {
                File file = new File(cursor.getString(1));
                long id = cursor.getInt(0);
                synchronized (this.mEntryMap) {
                    try {
                        Entry entry = this.mEntryMap.get(stringUrl);
                        if (entry == null) {
                            Entry entry2 = new Entry(id, file);
                            try {
                                this.mEntryMap.put(stringUrl, entry2);
                                entry = entry2;
                            } catch (Throwable th) {
                                th = th;
                            }
                        }
                        return entry;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
            cursor.close();
            return null;
        } finally {
            cursor.close();
        }
    }

    public Entry download(ThreadPool.JobContext jc, URL url) {
        if (!this.mInitialized) {
            initialize();
        }
        String stringUrl = url.toString();
        synchronized (this.mEntryMap) {
            Entry entry = this.mEntryMap.get(stringUrl);
            if (entry != null) {
                updateLastAccess(entry.mId);
                return entry;
            }
            TaskProxy proxy = new TaskProxy();
            synchronized (this.mTaskMap) {
                Entry entry2 = findEntryInDatabase(stringUrl);
                if (entry2 != null) {
                    updateLastAccess(entry2.mId);
                    return entry2;
                }
                DownloadTask task = this.mTaskMap.get(stringUrl);
                if (task == null) {
                    task = new DownloadTask(stringUrl);
                    this.mTaskMap.put(stringUrl, task);
                    task.mFuture = this.mApplication.getThreadPool().submit(task, task);
                }
                task.addProxy(proxy);
                return proxy.get(jc);
            }
        }
    }

    private void updateLastAccess(long id) {
        ContentValues values = new ContentValues();
        values.put("last_access", Long.valueOf(System.currentTimeMillis()));
        this.mDatabase.update(TABLE_NAME, values, "_id = ?", new String[]{String.valueOf(id)});
    }

    private synchronized void freeSomeSpaceIfNeed(int maxDeleteFileCount) {
        boolean containsKey;
        if (this.mTotalBytes > this.mCapacity) {
            Cursor cursor = this.mDatabase.query(TABLE_NAME, FREESPACE_PROJECTION, null, null, null, null, FREESPACE_ORDER_BY);
            while (maxDeleteFileCount > 0) {
                try {
                    if (this.mTotalBytes <= this.mCapacity || !cursor.moveToNext()) {
                        break;
                    }
                    long id = cursor.getLong(0);
                    String url = cursor.getString(2);
                    long size = cursor.getLong(3);
                    String path = cursor.getString(1);
                    synchronized (this.mEntryMap) {
                        containsKey = this.mEntryMap.containsKey(url);
                    }
                    if (!containsKey) {
                        maxDeleteFileCount--;
                        this.mTotalBytes -= size;
                        new File(path).delete();
                        this.mDatabase.delete(TABLE_NAME, "_id = ?", new String[]{String.valueOf(id)});
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }

    private synchronized long insertEntry(String url, File file) {
        ContentValues values;
        long size = file.length();
        this.mTotalBytes += size;
        values = new ContentValues();
        String hashCode = String.valueOf(Utils.crc64Long(url));
        values.put("_data", file.getAbsolutePath());
        values.put("hash_code", hashCode);
        values.put("content_url", url);
        values.put("_size", Long.valueOf(size));
        values.put("last_updated", Long.valueOf(System.currentTimeMillis()));
        return this.mDatabase.insert(TABLE_NAME, "", values);
    }

    private synchronized void initialize() {
        if (!this.mInitialized) {
            this.mInitialized = true;
            if (!this.mRoot.isDirectory()) {
                this.mRoot.mkdirs();
            }
            if (!this.mRoot.isDirectory()) {
                throw new RuntimeException("cannot create " + this.mRoot.getAbsolutePath());
            }
            Cursor cursor = this.mDatabase.query(TABLE_NAME, SUM_PROJECTION, null, null, null, null, null);
            this.mTotalBytes = 0L;
            try {
                if (cursor.moveToNext()) {
                    this.mTotalBytes = cursor.getLong(0);
                }
                cursor.close();
                if (this.mTotalBytes > this.mCapacity) {
                    freeSomeSpaceIfNeed(16);
                }
            } catch (Throwable th) {
                cursor.close();
                throw th;
            }
        }
    }

    private final class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "download.db", (SQLiteDatabase.CursorFactory) null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            DownloadEntry.SCHEMA.createTables(db);
            File[] arr$ = DownloadCache.this.mRoot.listFiles();
            for (File file : arr$) {
                if (!file.delete()) {
                    Log.w("DownloadCache", "fail to remove: " + file.getAbsolutePath());
                }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            DownloadEntry.SCHEMA.dropTables(db);
            onCreate(db);
        }
    }

    public class Entry {
        public File cacheFile;
        protected long mId;

        Entry(long id, File cacheFile) {
            this.mId = id;
            this.cacheFile = (File) Utils.checkNotNull(cacheFile);
        }
    }

    private class DownloadTask implements FutureListener<File>, ThreadPool.Job<File> {
        private Future<File> mFuture;
        private HashSet<TaskProxy> mProxySet = new HashSet<>();
        private final String mUrl;

        public DownloadTask(String url) {
            this.mUrl = (String) Utils.checkNotNull(url);
        }

        public void removeProxy(TaskProxy proxy) {
            synchronized (DownloadCache.this.mTaskMap) {
                Utils.assertTrue(this.mProxySet.remove(proxy));
                if (this.mProxySet.isEmpty()) {
                    this.mFuture.cancel();
                    DownloadCache.this.mTaskMap.remove(this.mUrl);
                }
            }
        }

        public void addProxy(TaskProxy proxy) {
            proxy.mTask = this;
            this.mProxySet.add(proxy);
        }

        @Override
        public void onFutureDone(Future<File> future) {
            File file = future.get();
            long id = 0;
            if (file != null) {
                id = DownloadCache.this.insertEntry(this.mUrl, file);
            }
            if (!future.isCancelled()) {
                synchronized (DownloadCache.this.mTaskMap) {
                    Entry entry = null;
                    synchronized (DownloadCache.this.mEntryMap) {
                        if (file != null) {
                            try {
                                Entry entry2 = DownloadCache.this.new Entry(id, file);
                                try {
                                    Utils.assertTrue(DownloadCache.this.mEntryMap.put(this.mUrl, entry2) == null);
                                    entry = entry2;
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        for (TaskProxy proxy : this.mProxySet) {
                            proxy.setResult(entry);
                        }
                        DownloadCache.this.mTaskMap.remove(this.mUrl);
                        DownloadCache.this.freeSomeSpaceIfNeed(16);
                    }
                }
                return;
            }
            Utils.assertTrue(this.mProxySet.isEmpty());
        }

        @Override
        public File run(ThreadPool.JobContext jc) {
            boolean downloaded;
            jc.setMode(2);
            File tempFile = null;
            try {
                URL url = new URL(this.mUrl);
                tempFile = File.createTempFile("cache", ".tmp", DownloadCache.this.mRoot);
                jc.setMode(2);
                downloaded = DownloadUtils.requestDownload(jc, url, tempFile);
                jc.setMode(0);
            } catch (Exception e) {
                Log.e("DownloadCache", String.format("fail to download %s", this.mUrl), e);
            } finally {
                jc.setMode(0);
            }
            if (downloaded) {
                return tempFile;
            }
            if (tempFile != null) {
                tempFile.delete();
            }
            return null;
        }
    }

    public static class TaskProxy {
        private Entry mEntry;
        private boolean mIsCancelled = false;
        private DownloadTask mTask;

        synchronized void setResult(Entry entry) {
            if (!this.mIsCancelled) {
                this.mEntry = entry;
                notifyAll();
            }
        }

        public synchronized Entry get(ThreadPool.JobContext jc) {
            jc.setCancelListener(new ThreadPool.CancelListener() {
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
                    Log.w("DownloadCache", "ignore interrupt", e);
                }
            }
            jc.setCancelListener(null);
            return this.mEntry;
        }
    }
}
