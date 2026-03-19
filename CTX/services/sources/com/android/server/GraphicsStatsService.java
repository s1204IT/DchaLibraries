package com.android.server;

import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.util.Log;
import android.view.IGraphicsStats;
import android.view.IGraphicsStatsCallback;
import com.android.internal.util.DumpUtils;
import com.android.server.job.controllers.JobStatus;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.TimeZone;

public class GraphicsStatsService extends IGraphicsStats.Stub {
    private static final int DELETE_OLD = 2;
    public static final String GRAPHICS_STATS_SERVICE = "graphicsstats";
    private static final int SAVE_BUFFER = 1;
    private static final String TAG = "GraphicsStatsService";
    private final AlarmManager mAlarmManager;
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private Handler mWriteOutHandler;
    private final int ASHMEM_SIZE = nGetAshmemSize();
    private final byte[] ZERO_DATA = new byte[this.ASHMEM_SIZE];
    private final Object mLock = new Object();
    private ArrayList<ActiveBuffer> mActive = new ArrayList<>();
    private final Object mFileAccessLock = new Object();
    private boolean mRotateIsScheduled = false;
    private File mGraphicsStatsDir = new File(new File(Environment.getDataDirectory(), "system"), GRAPHICS_STATS_SERVICE);

    private static native void nAddToDump(long j, String str);

    private static native void nAddToDump(long j, String str, String str2, long j2, long j3, long j4, byte[] bArr);

    private static native long nCreateDump(int i, boolean z);

    private static native void nFinishDump(long j);

    private static native int nGetAshmemSize();

    private static native void nSaveBuffer(String str, String str2, long j, long j2, long j3, byte[] bArr);

    public GraphicsStatsService(Context context) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        this.mGraphicsStatsDir.mkdirs();
        if (!this.mGraphicsStatsDir.exists()) {
            throw new IllegalStateException("Graphics stats directory does not exist: " + this.mGraphicsStatsDir.getAbsolutePath());
        }
        HandlerThread handlerThread = new HandlerThread("GraphicsStats-disk", 10);
        handlerThread.start();
        this.mWriteOutHandler = new Handler(handlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        GraphicsStatsService.this.saveBuffer((HistoricalBuffer) message.obj);
                        break;
                    case 2:
                        GraphicsStatsService.this.deleteOldBuffers();
                        break;
                }
                return true;
            }
        });
    }

    private void scheduleRotateLocked() {
        if (this.mRotateIsScheduled) {
            return;
        }
        this.mRotateIsScheduled = true;
        Calendar calendarNormalizeDate = normalizeDate(System.currentTimeMillis());
        calendarNormalizeDate.add(5, 1);
        this.mAlarmManager.setExact(1, calendarNormalizeDate.getTimeInMillis(), TAG, new AlarmManager.OnAlarmListener() {
            @Override
            public final void onAlarm() {
                this.f$0.onAlarm();
            }
        }, this.mWriteOutHandler);
    }

    private void onAlarm() {
        ActiveBuffer[] activeBufferArr;
        synchronized (this.mLock) {
            this.mRotateIsScheduled = false;
            scheduleRotateLocked();
            activeBufferArr = (ActiveBuffer[]) this.mActive.toArray(new ActiveBuffer[0]);
        }
        for (ActiveBuffer activeBuffer : activeBufferArr) {
            try {
                activeBuffer.mCallback.onRotateGraphicsStatsBuffer();
            } catch (RemoteException e) {
                Log.w(TAG, String.format("Failed to notify '%s' (pid=%d) to rotate buffers", activeBuffer.mInfo.packageName, Integer.valueOf(activeBuffer.mPid)), e);
            }
        }
        this.mWriteOutHandler.sendEmptyMessageDelayed(2, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    public ParcelFileDescriptor requestBufferForProcess(String str, IGraphicsStatsCallback iGraphicsStatsCallback) throws RemoteException {
        ParcelFileDescriptor parcelFileDescriptorRequestBufferForProcessLocked;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                this.mAppOps.checkPackage(callingUid, str);
                PackageInfo packageInfoAsUser = this.mContext.getPackageManager().getPackageInfoAsUser(str, 0, UserHandle.getUserId(callingUid));
                synchronized (this.mLock) {
                    parcelFileDescriptorRequestBufferForProcessLocked = requestBufferForProcessLocked(iGraphicsStatsCallback, callingUid, callingPid, str, packageInfoAsUser.getLongVersionCode());
                }
                return parcelFileDescriptorRequestBufferForProcessLocked;
            } catch (PackageManager.NameNotFoundException e) {
                throw new RemoteException("Unable to find package: '" + str + "'");
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private ParcelFileDescriptor getPfd(MemoryFile memoryFile) {
        try {
            if (!memoryFile.getFileDescriptor().valid()) {
                throw new IllegalStateException("Invalid file descriptor");
            }
            return new ParcelFileDescriptor(memoryFile.getFileDescriptor());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get PFD from memory file", e);
        }
    }

    private ParcelFileDescriptor requestBufferForProcessLocked(IGraphicsStatsCallback iGraphicsStatsCallback, int i, int i2, String str, long j) throws RemoteException {
        ActiveBuffer activeBufferFetchActiveBuffersLocked = fetchActiveBuffersLocked(iGraphicsStatsCallback, i, i2, str, j);
        scheduleRotateLocked();
        return getPfd(activeBufferFetchActiveBuffersLocked.mProcessBuffer);
    }

    private Calendar normalizeDate(long j) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(j);
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar;
    }

    private File pathForApp(BufferInfo bufferInfo) {
        return new File(this.mGraphicsStatsDir, String.format("%d/%s/%d/total", Long.valueOf(normalizeDate(bufferInfo.startTime).getTimeInMillis()), bufferInfo.packageName, Long.valueOf(bufferInfo.versionCode)));
    }

    private void saveBuffer(HistoricalBuffer historicalBuffer) {
        if (Trace.isTagEnabled(524288L)) {
            Trace.traceBegin(524288L, "saving graphicsstats for " + historicalBuffer.mInfo.packageName);
        }
        synchronized (this.mFileAccessLock) {
            File filePathForApp = pathForApp(historicalBuffer.mInfo);
            File parentFile = filePathForApp.getParentFile();
            parentFile.mkdirs();
            if (!parentFile.exists()) {
                Log.w(TAG, "Unable to create path: '" + parentFile.getAbsolutePath() + "'");
                return;
            }
            nSaveBuffer(filePathForApp.getAbsolutePath(), historicalBuffer.mInfo.packageName, historicalBuffer.mInfo.versionCode, historicalBuffer.mInfo.startTime, historicalBuffer.mInfo.endTime, historicalBuffer.mData);
            Trace.traceEnd(524288L);
        }
    }

    private void deleteRecursiveLocked(File file) {
        if (file.isDirectory()) {
            for (File file2 : file.listFiles()) {
                deleteRecursiveLocked(file2);
            }
        }
        if (!file.delete()) {
            Log.w(TAG, "Failed to delete '" + file.getAbsolutePath() + "'!");
        }
    }

    private void deleteOldBuffers() {
        Trace.traceBegin(524288L, "deleting old graphicsstats buffers");
        synchronized (this.mFileAccessLock) {
            File[] fileArrListFiles = this.mGraphicsStatsDir.listFiles();
            if (fileArrListFiles != null && fileArrListFiles.length > 3) {
                long[] jArr = new long[fileArrListFiles.length];
                for (int i = 0; i < fileArrListFiles.length; i++) {
                    try {
                        jArr[i] = Long.parseLong(fileArrListFiles[i].getName());
                    } catch (NumberFormatException e) {
                    }
                }
                if (jArr.length <= 3) {
                    return;
                }
                Arrays.sort(jArr);
                for (int i2 = 0; i2 < jArr.length - 3; i2++) {
                    deleteRecursiveLocked(new File(this.mGraphicsStatsDir, Long.toString(jArr[i2])));
                }
                Trace.traceEnd(524288L);
            }
        }
    }

    private void addToSaveQueue(ActiveBuffer activeBuffer) {
        try {
            Message.obtain(this.mWriteOutHandler, 1, new HistoricalBuffer(activeBuffer)).sendToTarget();
        } catch (IOException e) {
            Log.w(TAG, "Failed to copy graphicsstats from " + activeBuffer.mInfo.packageName, e);
        }
        activeBuffer.closeAllBuffers();
    }

    private void processDied(ActiveBuffer activeBuffer) {
        synchronized (this.mLock) {
            this.mActive.remove(activeBuffer);
        }
        addToSaveQueue(activeBuffer);
    }

    private ActiveBuffer fetchActiveBuffersLocked(IGraphicsStatsCallback iGraphicsStatsCallback, int i, int i2, String str, long j) throws RemoteException {
        int i3;
        int size = this.mActive.size();
        long timeInMillis = normalizeDate(System.currentTimeMillis()).getTimeInMillis();
        int i4 = 0;
        while (true) {
            if (i4 < size) {
                ActiveBuffer activeBuffer = this.mActive.get(i4);
                i3 = i2;
                if (activeBuffer.mPid == i3 && activeBuffer.mUid == (i = i)) {
                    if (activeBuffer.mInfo.startTime < timeInMillis) {
                        activeBuffer.binderDied();
                    } else {
                        return activeBuffer;
                    }
                }
                i4++;
            } else {
                int i5 = i;
                i3 = i2;
                break;
            }
        }
    }

    private HashSet<File> dumpActiveLocked(long j, ArrayList<HistoricalBuffer> arrayList) {
        HashSet<File> hashSet = new HashSet<>(arrayList.size());
        for (int i = 0; i < arrayList.size(); i++) {
            HistoricalBuffer historicalBuffer = arrayList.get(i);
            File filePathForApp = pathForApp(historicalBuffer.mInfo);
            hashSet.add(filePathForApp);
            nAddToDump(j, filePathForApp.getAbsolutePath(), historicalBuffer.mInfo.packageName, historicalBuffer.mInfo.versionCode, historicalBuffer.mInfo.startTime, historicalBuffer.mInfo.endTime, historicalBuffer.mData);
        }
        return hashSet;
    }

    private void dumpHistoricalLocked(long j, HashSet<File> hashSet) {
        for (File file : this.mGraphicsStatsDir.listFiles()) {
            for (File file2 : file.listFiles()) {
                for (File file3 : file2.listFiles()) {
                    File file4 = new File(file3, "total");
                    if (!hashSet.contains(file4)) {
                        nAddToDump(j, file4.getAbsolutePath());
                    }
                }
            }
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        boolean z;
        ArrayList<HistoricalBuffer> arrayList;
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, printWriter)) {
            int length = strArr.length;
            int i = 0;
            while (true) {
                if (i < length) {
                    if (!PriorityDump.PROTO_ARG.equals(strArr[i])) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            synchronized (this.mLock) {
                arrayList = new ArrayList<>(this.mActive.size());
                for (int i2 = 0; i2 < this.mActive.size(); i2++) {
                    try {
                        arrayList.add(new HistoricalBuffer(this.mActive.get(i2)));
                    } catch (IOException e) {
                    }
                }
            }
            long jNCreateDump = nCreateDump(fileDescriptor.getInt$(), z);
            try {
                synchronized (this.mFileAccessLock) {
                    HashSet<File> hashSetDumpActiveLocked = dumpActiveLocked(jNCreateDump, arrayList);
                    arrayList.clear();
                    dumpHistoricalLocked(jNCreateDump, hashSetDumpActiveLocked);
                }
            } finally {
                nFinishDump(jNCreateDump);
            }
        }
    }

    private final class BufferInfo {
        long endTime;
        final String packageName;
        long startTime;
        final long versionCode;

        BufferInfo(String str, long j, long j2) {
            this.packageName = str;
            this.versionCode = j;
            this.startTime = j2;
        }
    }

    private final class ActiveBuffer implements IBinder.DeathRecipient {
        final IGraphicsStatsCallback mCallback;
        final BufferInfo mInfo;
        final int mPid;
        MemoryFile mProcessBuffer;
        final IBinder mToken;
        final int mUid;

        ActiveBuffer(IGraphicsStatsCallback iGraphicsStatsCallback, int i, int i2, String str, long j) throws RemoteException, IOException {
            this.mInfo = GraphicsStatsService.this.new BufferInfo(str, j, System.currentTimeMillis());
            this.mUid = i;
            this.mPid = i2;
            this.mCallback = iGraphicsStatsCallback;
            this.mToken = this.mCallback.asBinder();
            this.mToken.linkToDeath(this, 0);
            this.mProcessBuffer = new MemoryFile("GFXStats-" + i2, GraphicsStatsService.this.ASHMEM_SIZE);
            this.mProcessBuffer.writeBytes(GraphicsStatsService.this.ZERO_DATA, 0, 0, GraphicsStatsService.this.ASHMEM_SIZE);
        }

        @Override
        public void binderDied() {
            this.mToken.unlinkToDeath(this, 0);
            GraphicsStatsService.this.processDied(this);
        }

        void closeAllBuffers() {
            if (this.mProcessBuffer != null) {
                this.mProcessBuffer.close();
                this.mProcessBuffer = null;
            }
        }
    }

    private final class HistoricalBuffer {
        final byte[] mData;
        final BufferInfo mInfo;

        HistoricalBuffer(ActiveBuffer activeBuffer) throws IOException {
            this.mData = new byte[GraphicsStatsService.this.ASHMEM_SIZE];
            this.mInfo = activeBuffer.mInfo;
            this.mInfo.endTime = System.currentTimeMillis();
            activeBuffer.mProcessBuffer.readBytes(this.mData, 0, 0, GraphicsStatsService.this.ASHMEM_SIZE);
        }
    }
}
