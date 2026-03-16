package com.android.providers.downloads;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.os.Environment;
import android.os.Process;
import android.provider.Downloads;
import android.system.ErrnoException;
import android.text.TextUtils;
import android.util.Slog;
import com.android.providers.downloads.StorageUtils;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import libcore.io.IoUtils;

public class DownloadIdleService extends JobService {

    private interface OrphanQuery {
        public static final String[] PROJECTION = {"_id", "_data"};
    }

    private interface StaleQuery {
        public static final String[] PROJECTION = {"_id", "status", "lastmod", "is_visible_in_downloads_ui"};
    }

    private class IdleRunnable implements Runnable {
        private JobParameters mParams;

        public IdleRunnable(JobParameters params) {
            this.mParams = params;
        }

        @Override
        public void run() {
            DownloadIdleService.this.cleanStale();
            DownloadIdleService.this.cleanOrphans();
            DownloadIdleService.this.jobFinished(this.mParams, false);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(new IdleRunnable(params)).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public void cleanStale() {
        ContentResolver resolver = getContentResolver();
        long modifiedBefore = System.currentTimeMillis() - 604800000;
        Cursor cursor = resolver.query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, StaleQuery.PROJECTION, "status >= '200' AND lastmod <= '" + modifiedBefore + "' AND is_visible_in_downloads_ui == '0'", null, null);
        int count = 0;
        while (cursor.moveToNext()) {
            try {
                long id = cursor.getLong(0);
                resolver.delete(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id), null, null);
                count++;
            } catch (Throwable th) {
                IoUtils.closeQuietly(cursor);
                throw th;
            }
        }
        IoUtils.closeQuietly(cursor);
        Slog.d("DownloadManager", "Removed " + count + " stale downloads");
    }

    public void cleanOrphans() {
        ContentResolver resolver = getContentResolver();
        HashSet<StorageUtils.ConcreteFile> fromDb = Sets.newHashSet();
        Cursor cursor = resolver.query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, OrphanQuery.PROJECTION, null, null, null);
        while (cursor.moveToNext()) {
            try {
                String path = cursor.getString(1);
                if (!TextUtils.isEmpty(path)) {
                    File file = new File(path);
                    try {
                        fromDb.add(new StorageUtils.ConcreteFile(file));
                    } catch (ErrnoException e) {
                        String state = Environment.getExternalStorageState(file);
                        if ("unknown".equals(state) || "mounted".equals(state)) {
                            long id = cursor.getLong(0);
                            Slog.d("DownloadManager", "Missing " + file + ", deleting " + id);
                            resolver.delete(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id), null, null);
                        }
                    }
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(cursor);
                throw th;
            }
        }
        IoUtils.closeQuietly(cursor);
        int uid = Process.myUid();
        ArrayList<StorageUtils.ConcreteFile> fromDisk = Lists.newArrayList();
        fromDisk.addAll(StorageUtils.listFilesRecursive(getCacheDir(), null, uid));
        fromDisk.addAll(StorageUtils.listFilesRecursive(getFilesDir(), null, uid));
        fromDisk.addAll(StorageUtils.listFilesRecursive(Environment.getDownloadCacheDirectory(), null, uid));
        Slog.d("DownloadManager", "Found " + fromDb.size() + " files in database");
        Slog.d("DownloadManager", "Found " + fromDisk.size() + " files on disk");
        for (StorageUtils.ConcreteFile file2 : fromDisk) {
            if (!fromDb.contains(file2)) {
                Slog.d("DownloadManager", "Missing db entry, deleting " + file2.file);
                file2.file.delete();
            }
        }
    }
}
