package com.android.providers.downloads;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
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

        public IdleRunnable(JobParameters jobParameters) {
            this.mParams = jobParameters;
        }

        @Override
        public void run() {
            DownloadIdleService.this.cleanStale();
            DownloadIdleService.this.cleanOrphans();
            DownloadIdleService.this.jobFinished(this.mParams, false);
        }
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Helpers.getAsyncHandler().post(new IdleRunnable(jobParameters));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    public static void scheduleIdlePass(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        if (jobScheduler.getPendingJob(-100) == null) {
            jobScheduler.schedule(new JobInfo.Builder(-100, new ComponentName(context, (Class<?>) DownloadIdleService.class)).setPeriodic(43200000L).setRequiresCharging(true).setRequiresDeviceIdle(true).build());
        }
    }

    public void cleanStale() {
        ContentResolver contentResolver = getContentResolver();
        long jCurrentTimeMillis = System.currentTimeMillis() - 604800000;
        Cursor cursorQuery = contentResolver.query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, StaleQuery.PROJECTION, "status >= '200' AND lastmod <= '" + jCurrentTimeMillis + "' AND is_visible_in_downloads_ui == '0'", null, null);
        int i = 0;
        while (cursorQuery.moveToNext()) {
            try {
                contentResolver.delete(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, cursorQuery.getLong(0)), null, null);
                i++;
            } catch (Throwable th) {
                IoUtils.closeQuietly(cursorQuery);
                throw th;
            }
        }
        IoUtils.closeQuietly(cursorQuery);
        Slog.d("DownloadManager", "Removed " + i + " stale downloads");
    }

    public void cleanOrphans() {
        ContentResolver contentResolver = getContentResolver();
        HashSet hashSetNewHashSet = Sets.newHashSet();
        Cursor cursorQuery = contentResolver.query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, OrphanQuery.PROJECTION, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                String string = cursorQuery.getString(1);
                if (!TextUtils.isEmpty(string)) {
                    File file = new File(string);
                    try {
                        hashSetNewHashSet.add(new StorageUtils.ConcreteFile(file));
                    } catch (ErrnoException e) {
                        String externalStorageState = Environment.getExternalStorageState(file);
                        if ("unknown".equals(externalStorageState) || "mounted".equals(externalStorageState)) {
                            long j = cursorQuery.getLong(0);
                            Slog.d("DownloadManager", "Missing " + file + ", deleting " + j);
                            contentResolver.delete(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, j), null, null);
                        }
                    }
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(cursorQuery);
                throw th;
            }
        }
        IoUtils.closeQuietly(cursorQuery);
        int iMyUid = Process.myUid();
        ArrayList<StorageUtils.ConcreteFile> arrayListNewArrayList = Lists.newArrayList();
        arrayListNewArrayList.addAll(StorageUtils.listFilesRecursive(getCacheDir(), null, iMyUid));
        arrayListNewArrayList.addAll(StorageUtils.listFilesRecursive(getFilesDir(), null, iMyUid));
        arrayListNewArrayList.addAll(StorageUtils.listFilesRecursive(Environment.getDownloadCacheDirectory(), null, iMyUid));
        Slog.d("DownloadManager", "Found " + hashSetNewHashSet.size() + " files in database");
        Slog.d("DownloadManager", "Found " + arrayListNewArrayList.size() + " files on disk");
        for (StorageUtils.ConcreteFile concreteFile : arrayListNewArrayList) {
            if (!hashSetNewHashSet.contains(concreteFile)) {
                Slog.d("DownloadManager", "Missing db entry, deleting " + concreteFile.file);
                concreteFile.file.delete();
            }
        }
    }
}
