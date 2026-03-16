package com.android.settings.deviceinfo;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.UserInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.util.SparseLongArray;
import com.android.internal.app.IMediaContainerService;
import com.google.android.collect.Maps;
import com.google.android.collect.Sets;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class StorageMeasurement {
    private long mAvailSize;
    List<FileInfo> mFileInfoForMisc;
    private final MeasurementHandler mHandler;
    private final boolean mIsInternal;
    private final boolean mIsPrimary;
    private volatile WeakReference<MeasurementReceiver> mReceiver;
    private long mTotalSize;
    private final StorageVolume mVolume;
    static final boolean LOGV = Log.isLoggable("StorageMeasurement", 2);
    public static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName("com.android.defcontainer", "com.android.defcontainer.DefaultContainerService");
    private static final Set<String> sMeasureMediaTypes = Sets.newHashSet(new String[]{Environment.DIRECTORY_DCIM, Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES, Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS, Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_PODCASTS, Environment.DIRECTORY_DOWNLOADS, "Android"});
    private static HashMap<StorageVolume, StorageMeasurement> sInstances = Maps.newHashMap();

    public static class MeasurementDetails {
        public long appsSize;
        public long availSize;
        public long cacheSize;
        public long miscSize;
        public long totalSize;
        public HashMap<String, Long> mediaSize = Maps.newHashMap();
        public SparseLongArray usersSize = new SparseLongArray();
    }

    public interface MeasurementReceiver {
        void updateApproximate(StorageMeasurement storageMeasurement, long j, long j2);

        void updateDetails(StorageMeasurement storageMeasurement, MeasurementDetails measurementDetails);
    }

    public static StorageMeasurement getInstance(Context context, StorageVolume volume) {
        StorageMeasurement value;
        synchronized (sInstances) {
            value = sInstances.get(volume);
            if (value == null) {
                value = new StorageMeasurement(context.getApplicationContext(), volume);
                sInstances.put(volume, value);
            }
        }
        return value;
    }

    private StorageMeasurement(Context context, StorageVolume volume) {
        this.mVolume = volume;
        this.mIsInternal = volume == null;
        this.mIsPrimary = volume != null ? volume.isPrimary() : false;
        HandlerThread handlerThread = new HandlerThread("MemoryMeasurement");
        handlerThread.start();
        this.mHandler = new MeasurementHandler(context, handlerThread.getLooper());
    }

    public void setReceiver(MeasurementReceiver receiver) {
        if (this.mReceiver == null || this.mReceiver.get() == null) {
            this.mReceiver = new WeakReference<>(receiver);
        }
    }

    public void measure() {
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.sendEmptyMessage(1);
        }
    }

    public void cleanUp() {
        this.mReceiver = null;
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(3);
    }

    public void invalidate() {
        this.mHandler.sendEmptyMessage(5);
    }

    private void sendInternalApproximateUpdate() {
        MeasurementReceiver receiver = this.mReceiver != null ? this.mReceiver.get() : null;
        if (receiver != null) {
            receiver.updateApproximate(this, this.mTotalSize, this.mAvailSize);
        }
    }

    private void sendExactUpdate(MeasurementDetails details) {
        MeasurementReceiver receiver = this.mReceiver != null ? this.mReceiver.get() : null;
        if (receiver == null) {
            if (LOGV) {
                Log.i("StorageMeasurement", "measurements dropped because receiver is null! wasted effort");
                return;
            }
            return;
        }
        receiver.updateDetails(this, details);
    }

    private static class StatsObserver extends IPackageStatsObserver.Stub {
        private final int mCurrentUser;
        private final MeasurementDetails mDetails;
        private final Message mFinished;
        private final boolean mIsInternal;
        private int mRemaining;

        public StatsObserver(boolean isInternal, MeasurementDetails details, int currentUser, Message finished, int remaining) {
            this.mIsInternal = isInternal;
            this.mDetails = details;
            this.mCurrentUser = currentUser;
            this.mFinished = finished;
            this.mRemaining = remaining;
        }

        public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
            int i;
            synchronized (this.mDetails) {
                if (succeeded) {
                    addStatsLocked(stats);
                    i = this.mRemaining - 1;
                    this.mRemaining = i;
                    if (i == 0) {
                        this.mFinished.sendToTarget();
                    }
                } else {
                    i = this.mRemaining - 1;
                    this.mRemaining = i;
                    if (i == 0) {
                    }
                }
            }
        }

        private void addStatsLocked(PackageStats stats) {
            if (this.mIsInternal) {
                long codeSize = stats.codeSize;
                long dataSize = stats.dataSize;
                long cacheSize = stats.cacheSize;
                if (Environment.isExternalStorageEmulated()) {
                    codeSize += stats.externalCodeSize + stats.externalObbSize;
                    dataSize += stats.externalDataSize + stats.externalMediaSize;
                    cacheSize += stats.externalCacheSize;
                }
                if (stats.userHandle == this.mCurrentUser) {
                    this.mDetails.appsSize += codeSize;
                    this.mDetails.appsSize += dataSize;
                }
                StorageMeasurement.addValue(this.mDetails.usersSize, stats.userHandle, dataSize);
                this.mDetails.cacheSize += cacheSize;
                return;
            }
            this.mDetails.appsSize += stats.externalCodeSize + stats.externalDataSize + stats.externalMediaSize + stats.externalObbSize;
            this.mDetails.cacheSize += stats.externalCacheSize;
        }
    }

    private class MeasurementHandler extends Handler {
        private volatile boolean mBound;
        private MeasurementDetails mCached;
        private final WeakReference<Context> mContext;
        private final ServiceConnection mDefContainerConn;
        private IMediaContainerService mDefaultContainer;
        private Object mLock;

        public MeasurementHandler(Context context, Looper looper) {
            super(looper);
            this.mLock = new Object();
            this.mBound = false;
            this.mDefContainerConn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    IMediaContainerService imcs = IMediaContainerService.Stub.asInterface(service);
                    MeasurementHandler.this.mDefaultContainer = imcs;
                    MeasurementHandler.this.mBound = true;
                    MeasurementHandler.this.sendMessage(MeasurementHandler.this.obtainMessage(2, imcs));
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    MeasurementHandler.this.mBound = false;
                    MeasurementHandler.this.removeMessages(2);
                }
            };
            this.mContext = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            Context context;
            switch (msg.what) {
                case 1:
                    if (this.mCached != null) {
                        StorageMeasurement.this.sendExactUpdate(this.mCached);
                        return;
                    }
                    context = this.mContext != null ? this.mContext.get() : null;
                    if (context != null) {
                        synchronized (this.mLock) {
                            if (this.mBound) {
                                removeMessages(3);
                                sendMessage(obtainMessage(2, this.mDefaultContainer));
                            } else {
                                Intent service = new Intent().setComponent(StorageMeasurement.DEFAULT_CONTAINER_COMPONENT);
                                context.bindServiceAsUser(service, this.mDefContainerConn, 1, UserHandle.OWNER);
                            }
                            break;
                        }
                        return;
                    }
                    return;
                case 2:
                    IMediaContainerService imcs = (IMediaContainerService) msg.obj;
                    measureApproximateStorage(imcs);
                    measureExactStorage(imcs);
                    return;
                case 3:
                    synchronized (this.mLock) {
                        if (this.mBound) {
                            context = this.mContext != null ? this.mContext.get() : null;
                            if (context != null) {
                                this.mBound = false;
                                context.unbindService(this.mDefContainerConn);
                            } else {
                                return;
                            }
                        }
                        return;
                    }
                case 4:
                    this.mCached = (MeasurementDetails) msg.obj;
                    StorageMeasurement.this.sendExactUpdate(this.mCached);
                    return;
                case 5:
                    this.mCached = null;
                    return;
                default:
                    return;
            }
        }

        private void measureApproximateStorage(IMediaContainerService imcs) {
            String path;
            if (StorageMeasurement.this.mVolume != null) {
                path = StorageMeasurement.this.mVolume.getPath();
            } else {
                path = Environment.getDataDirectory().getPath();
            }
            try {
                long[] stats = imcs.getFileSystemStats(path);
                StorageMeasurement.this.mTotalSize = stats[0];
                StorageMeasurement.this.mAvailSize = stats[1];
            } catch (Exception e) {
                Log.w("StorageMeasurement", "Problem in container service", e);
            }
            StorageMeasurement.this.sendInternalApproximateUpdate();
        }

        private void measureExactStorage(IMediaContainerService imcs) {
            Context context = this.mContext != null ? this.mContext.get() : null;
            if (context != null) {
                MeasurementDetails details = new MeasurementDetails();
                Message finished = obtainMessage(4, details);
                details.totalSize = StorageMeasurement.this.mTotalSize;
                details.availSize = StorageMeasurement.this.mAvailSize;
                UserManager userManager = (UserManager) context.getSystemService("user");
                List<UserInfo> users = userManager.getUsers();
                int currentUser = ActivityManager.getCurrentUser();
                Environment.UserEnvironment currentEnv = new Environment.UserEnvironment(currentUser);
                boolean measureMedia = (StorageMeasurement.this.mIsInternal && Environment.isExternalStorageEmulated()) || StorageMeasurement.this.mIsPrimary;
                if (measureMedia) {
                    for (String type : StorageMeasurement.sMeasureMediaTypes) {
                        File path = currentEnv.getExternalStoragePublicDirectory(type);
                        long size = StorageMeasurement.getDirectorySize(imcs, path);
                        details.mediaSize.put(type, Long.valueOf(size));
                    }
                }
                if (measureMedia) {
                    File path2 = StorageMeasurement.this.mIsInternal ? currentEnv.getExternalStorageDirectory() : StorageMeasurement.this.mVolume.getPathFile();
                    details.miscSize = StorageMeasurement.this.measureMisc(imcs, path2);
                }
                for (UserInfo user : users) {
                    Environment.UserEnvironment userEnv = new Environment.UserEnvironment(user.id);
                    long size2 = StorageMeasurement.getDirectorySize(imcs, userEnv.getExternalStorageDirectory());
                    StorageMeasurement.addValue(details.usersSize, user.id, size2);
                }
                PackageManager pm = context.getPackageManager();
                if (StorageMeasurement.this.mIsInternal || StorageMeasurement.this.mIsPrimary) {
                    List<ApplicationInfo> apps = pm.getInstalledApplications(8704);
                    int count = users.size() * apps.size();
                    StatsObserver observer = new StatsObserver(StorageMeasurement.this.mIsInternal, details, currentUser, finished, count);
                    for (UserInfo user2 : users) {
                        for (ApplicationInfo app : apps) {
                            pm.getPackageSizeInfo(app.packageName, user2.id, observer);
                        }
                    }
                    return;
                }
                finished.sendToTarget();
            }
        }
    }

    private static long getDirectorySize(IMediaContainerService imcs, File path) {
        try {
            long size = imcs.calculateDirectorySize(path.toString());
            Log.d("StorageMeasurement", "getDirectorySize(" + path + ") returned " + size);
            return size;
        } catch (Exception e) {
            Log.w("StorageMeasurement", "Could not read memory from default container service for " + path, e);
            return 0L;
        }
    }

    private long measureMisc(IMediaContainerService imcs, File dir) {
        this.mFileInfoForMisc = new ArrayList();
        File[] files = dir.listFiles();
        if (files == null) {
            return 0L;
        }
        long counter = 0;
        long miscSize = 0;
        for (File file : files) {
            String path = file.getAbsolutePath();
            String name = file.getName();
            if (!sMeasureMediaTypes.contains(name)) {
                if (file.isFile()) {
                    long fileSize = file.length();
                    this.mFileInfoForMisc.add(new FileInfo(path, fileSize, counter));
                    miscSize += fileSize;
                    counter++;
                } else if (file.isDirectory()) {
                    long dirSize = getDirectorySize(imcs, file);
                    this.mFileInfoForMisc.add(new FileInfo(path, dirSize, counter));
                    miscSize += dirSize;
                    counter++;
                }
            }
        }
        Collections.sort(this.mFileInfoForMisc);
        return miscSize;
    }

    static class FileInfo implements Comparable<FileInfo> {
        final String mFileName;
        final long mId;
        final long mSize;

        FileInfo(String fileName, long size, long id) {
            this.mFileName = fileName;
            this.mSize = size;
            this.mId = id;
        }

        @Override
        public int compareTo(FileInfo that) {
            if (this == that || this.mSize == that.mSize) {
                return 0;
            }
            return this.mSize < that.mSize ? 1 : -1;
        }

        public String toString() {
            return this.mFileName + " : " + this.mSize + ", id:" + this.mId;
        }
    }

    private static void addValue(SparseLongArray array, int key, long value) {
        array.put(key, array.get(key) + value);
    }
}
