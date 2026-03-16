package com.android.providers.downloads;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.providers.downloads.DownloadInfo;
import com.google.android.collect.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadService extends Service {
    private static ComponentName sCleanupServiceName = new ComponentName(DownloadIdleService.class.getPackage().getName(), DownloadIdleService.class.getName());
    private AlarmManager mAlarmManager;
    private volatile int mLastStartId;
    private DownloadNotifier mNotifier;
    private DownloadManagerContentObserver mObserver;
    private DownloadScanner mScanner;
    SystemFacade mSystemFacade;
    private Handler mUpdateHandler;
    private HandlerThread mUpdateThread;

    @GuardedBy("mDownloads")
    private final Map<Long, DownloadInfo> mDownloads = Maps.newHashMap();
    private final ExecutorService mExecutor = buildDownloadExecutor();
    private Handler.Callback mUpdateCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            boolean isActive;
            Process.setThreadPriority(10);
            int startId = msg.arg1;
            synchronized (DownloadService.this.mDownloads) {
                isActive = DownloadService.this.updateLocked();
            }
            if (msg.what == 2) {
                for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                    if (entry.getKey().getName().startsWith("pool")) {
                        Log.d("DownloadManager", entry.getKey() + ": " + Arrays.toString(entry.getValue()));
                    }
                }
                DownloadService.this.mNotifier.dumpSpeeds();
                Log.wtf("DownloadManager", "Final update pass triggered, isActive=" + isActive + "; someone didn't update correctly.");
            }
            if (isActive) {
                DownloadService.this.enqueueFinalUpdate();
                return true;
            }
            if (DownloadService.this.stopSelfResult(startId)) {
                DownloadService.this.getContentResolver().unregisterContentObserver(DownloadService.this.mObserver);
                DownloadService.this.mScanner.shutdown();
                DownloadService.this.mUpdateThread.quit();
                return true;
            }
            return true;
        }
    };

    private static ExecutorService buildDownloadExecutor() {
        int maxConcurrent = Resources.getSystem().getInteger(android.R.integer.config_defaultPictureInPictureGravity);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(maxConcurrent, maxConcurrent, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (t == null && (r instanceof Future)) {
                    try {
                        ((Future) r).get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (CancellationException ce) {
                        t = ce;
                    } catch (ExecutionException ee) {
                        t = ee.getCause();
                    }
                }
                if (t != null) {
                    Log.w("DownloadManager", "Uncaught exception", t);
                }
            }
        };
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private class DownloadManagerContentObserver extends ContentObserver {
        public DownloadManagerContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            DownloadService.this.enqueueUpdate();
        }
    }

    @Override
    public IBinder onBind(Intent i) {
        throw new UnsupportedOperationException("Cannot bind to Download Manager Service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Constants.LOGVV) {
            Log.v("DownloadManager", "Service onCreate");
        }
        if (this.mSystemFacade == null) {
            this.mSystemFacade = new RealSystemFacade(this);
        }
        this.mAlarmManager = (AlarmManager) getSystemService("alarm");
        this.mUpdateThread = new HandlerThread("DownloadManager-UpdateThread");
        this.mUpdateThread.start();
        this.mUpdateHandler = new Handler(this.mUpdateThread.getLooper(), this.mUpdateCallback);
        this.mScanner = new DownloadScanner(this);
        this.mNotifier = new DownloadNotifier(this);
        this.mNotifier.cancelAll();
        this.mObserver = new DownloadManagerContentObserver();
        getContentResolver().registerContentObserver(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, true, this.mObserver);
        JobScheduler js = (JobScheduler) getSystemService("jobscheduler");
        if (needToScheduleCleanup(js)) {
            JobInfo job = new JobInfo.Builder(1, sCleanupServiceName).setPeriodic(86400000L).setRequiresCharging(true).setRequiresDeviceIdle(true).build();
            js.schedule(job);
        }
    }

    private boolean needToScheduleCleanup(JobScheduler js) {
        List<JobInfo> myJobs = js.getAllPendingJobs();
        int N = myJobs.size();
        for (int i = 0; i < N; i++) {
            if (myJobs.get(i).getId() == 1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int returnValue = super.onStartCommand(intent, flags, startId);
        if (Constants.LOGVV) {
            Log.v("DownloadManager", "Service onStart");
        }
        this.mLastStartId = startId;
        enqueueUpdate();
        return returnValue;
    }

    @Override
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(this.mObserver);
        this.mScanner.shutdown();
        this.mUpdateThread.quit();
        if (Constants.LOGVV) {
            Log.v("DownloadManager", "Service onDestroy");
        }
        super.onDestroy();
    }

    public void enqueueUpdate() {
        if (this.mUpdateHandler != null) {
            this.mUpdateHandler.removeMessages(1);
            this.mUpdateHandler.obtainMessage(1, this.mLastStartId, -1).sendToTarget();
        }
    }

    private void enqueueFinalUpdate() {
        this.mUpdateHandler.removeMessages(2);
        this.mUpdateHandler.sendMessageDelayed(this.mUpdateHandler.obtainMessage(2, this.mLastStartId, -1), 300000L);
    }

    private boolean updateLocked() {
        long now = this.mSystemFacade.currentTimeMillis();
        boolean isActive = false;
        long nextActionMillis = Long.MAX_VALUE;
        Set<Long> staleIds = Sets.newHashSet(this.mDownloads.keySet());
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, null, null, null, null);
        try {
            DownloadInfo.Reader reader = new DownloadInfo.Reader(resolver, cursor);
            int idColumn = cursor.getColumnIndexOrThrow("_id");
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                staleIds.remove(Long.valueOf(id));
                DownloadInfo info = this.mDownloads.get(Long.valueOf(id));
                if (info != null) {
                    updateDownload(reader, info, now);
                } else {
                    info = insertDownloadLocked(reader, now);
                }
                if (info.mDeleted) {
                    if (!TextUtils.isEmpty(info.mMediaProviderUri)) {
                        resolver.delete(Uri.parse(info.mMediaProviderUri), null, null);
                    }
                    deleteFileIfExists(info.mFileName);
                    resolver.delete(info.getAllDownloadsUri(), null, null);
                } else {
                    boolean activeDownload = info.startDownloadIfReady(this.mExecutor);
                    boolean activeScan = info.startScanIfReady(this.mScanner);
                    isActive = isActive | activeDownload | activeScan;
                }
                nextActionMillis = Math.min(info.nextActionMillis(now), nextActionMillis);
            }
            cursor.close();
            Iterator<Long> it = staleIds.iterator();
            while (it.hasNext()) {
                deleteDownloadLocked(it.next().longValue());
            }
            this.mNotifier.updateWith(this.mDownloads.values());
            if (nextActionMillis > 0 && nextActionMillis < Long.MAX_VALUE) {
                if (Constants.LOGV) {
                    Log.v("DownloadManager", "scheduling start in " + nextActionMillis + "ms");
                }
                Intent intent = new Intent("android.intent.action.DOWNLOAD_WAKEUP");
                intent.setClass(this, DownloadReceiver.class);
                this.mAlarmManager.set(0, now + nextActionMillis, PendingIntent.getBroadcast(this, 0, intent, 1073741824));
            }
            return isActive;
        } catch (Throwable th) {
            cursor.close();
            throw th;
        }
    }

    private DownloadInfo insertDownloadLocked(DownloadInfo.Reader reader, long now) {
        DownloadInfo info = reader.newDownloadInfo(this, this.mSystemFacade, this.mNotifier);
        this.mDownloads.put(Long.valueOf(info.mId), info);
        if (Constants.LOGVV) {
            Log.v("DownloadManager", "processing inserted download " + info.mId);
        }
        return info;
    }

    private void updateDownload(DownloadInfo.Reader reader, DownloadInfo info, long now) {
        reader.updateFromDatabase(info);
        if (Constants.LOGVV) {
            Log.v("DownloadManager", "processing updated download " + info.mId + ", status: " + info.mStatus);
        }
    }

    private void deleteDownloadLocked(long id) {
        DownloadInfo info = this.mDownloads.get(Long.valueOf(id));
        if (info.mStatus == 192) {
            info.mStatus = 490;
        }
        if (info.mDestination != 0 && info.mFileName != null) {
            if (Constants.LOGVV) {
                Log.d("DownloadManager", "deleteDownloadLocked() deleting " + info.mFileName);
            }
            deleteFileIfExists(info.mFileName);
        }
        this.mDownloads.remove(Long.valueOf(info.mId));
    }

    private void deleteFileIfExists(String path) {
        if (!TextUtils.isEmpty(path)) {
            if (Constants.LOGVV) {
                Log.d("DownloadManager", "deleteFileIfExists() deleting " + path);
            }
            File file = new File(path);
            if (file.exists() && !file.delete()) {
                Log.w("DownloadManager", "file: '" + path + "' couldn't be deleted");
            }
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        synchronized (this.mDownloads) {
            List<Long> ids = Lists.newArrayList(this.mDownloads.keySet());
            Collections.sort(ids);
            for (Long id : ids) {
                DownloadInfo info = this.mDownloads.get(id);
                info.dump(pw);
            }
        }
    }
}
