package com.android.server;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.view.IGraphicsStats;
import android.view.ThreadedRenderer;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class GraphicsStatsService extends IGraphicsStats.Stub {
    private static final int ASHMEM_SIZE = 464;
    public static final String GRAPHICS_STATS_SERVICE = "graphicsstats";
    private static final int HISTORY_SIZE = 20;
    private static final String TAG = "GraphicsStatsService";
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private final Object mLock = new Object();
    private ArrayList<ActiveBuffer> mActive = new ArrayList<>();
    private HistoricalData[] mHistoricalLog = new HistoricalData[20];
    private int mNextHistoricalSlot = 0;
    private byte[] mTempBuffer = new byte[ASHMEM_SIZE];

    public GraphicsStatsService(Context context) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
    }

    public ParcelFileDescriptor requestBufferForProcess(String packageName, IBinder token) throws RemoteException {
        ParcelFileDescriptor pfd;
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAppOps.checkPackage(uid, packageName);
            synchronized (this.mLock) {
                pfd = requestBufferForProcessLocked(token, uid, pid, packageName);
            }
            return pfd;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private ParcelFileDescriptor getPfd(MemoryFile file) {
        try {
            return new ParcelFileDescriptor(file.getFileDescriptor());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to get PFD from memory file", ex);
        }
    }

    private ParcelFileDescriptor requestBufferForProcessLocked(IBinder token, int uid, int pid, String packageName) throws RemoteException {
        ActiveBuffer buffer = fetchActiveBuffersLocked(token, uid, pid, packageName);
        return getPfd(buffer.mProcessBuffer);
    }

    private void processDied(ActiveBuffer buffer) {
        HistoricalData historicalData = null;
        synchronized (this.mLock) {
            this.mActive.remove(buffer);
            Log.d("GraphicsStats", "Buffer count: " + this.mActive.size());
        }
        HistoricalData data = buffer.mPreviousData;
        buffer.mPreviousData = null;
        if (data == null && (data = this.mHistoricalLog[this.mNextHistoricalSlot]) == null) {
            data = new HistoricalData(historicalData);
        }
        data.update(buffer.mPackageName, buffer.mUid, buffer.mProcessBuffer);
        buffer.closeAllBuffers();
        this.mHistoricalLog[this.mNextHistoricalSlot] = data;
        this.mNextHistoricalSlot = (this.mNextHistoricalSlot + 1) % this.mHistoricalLog.length;
    }

    private ActiveBuffer fetchActiveBuffersLocked(IBinder token, int uid, int pid, String packageName) throws RemoteException {
        int size = this.mActive.size();
        for (int i = 0; i < size; i++) {
            ActiveBuffer buffers = this.mActive.get(i);
            if (buffers.mPid == pid && buffers.mUid == uid) {
                return buffers;
            }
        }
        try {
            ActiveBuffer buffers2 = new ActiveBuffer(token, uid, pid, packageName);
            this.mActive.add(buffers2);
            return buffers2;
        } catch (IOException e) {
            throw new RemoteException("Failed to allocate space");
        }
    }

    private HistoricalData removeHistoricalDataLocked(int uid, String packageName) {
        for (int i = 0; i < this.mHistoricalLog.length; i++) {
            HistoricalData data = this.mHistoricalLog[i];
            if (data != null && data.mUid == uid && data.mPackageName.equals(packageName)) {
                if (i == this.mNextHistoricalSlot) {
                    this.mHistoricalLog[i] = null;
                } else {
                    this.mHistoricalLog[i] = this.mHistoricalLog[this.mNextHistoricalSlot];
                    this.mHistoricalLog[this.mNextHistoricalSlot] = null;
                }
                return data;
            }
        }
        return null;
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        synchronized (this.mLock) {
            for (int i = 0; i < this.mActive.size(); i++) {
                ActiveBuffer buffer = this.mActive.get(i);
                fout.print("Package: ");
                fout.print(buffer.mPackageName);
                fout.flush();
                try {
                    buffer.mProcessBuffer.readBytes(this.mTempBuffer, 0, 0, ASHMEM_SIZE);
                    ThreadedRenderer.dumpProfileData(this.mTempBuffer, fd);
                } catch (IOException e) {
                    fout.println("Failed to dump");
                }
                fout.println();
            }
            for (HistoricalData buffer2 : this.mHistoricalLog) {
                if (buffer2 != null) {
                    fout.print("Package: ");
                    fout.print(buffer2.mPackageName);
                    fout.flush();
                    ThreadedRenderer.dumpProfileData(buffer2.mBuffer, fd);
                    fout.println();
                }
            }
        }
    }

    private final class ActiveBuffer implements IBinder.DeathRecipient {
        final String mPackageName;
        final int mPid;
        HistoricalData mPreviousData;
        MemoryFile mProcessBuffer;
        final IBinder mToken;
        final int mUid;

        ActiveBuffer(IBinder token, int uid, int pid, String packageName) throws RemoteException, IOException {
            this.mUid = uid;
            this.mPid = pid;
            this.mPackageName = packageName;
            this.mToken = token;
            this.mToken.linkToDeath(this, 0);
            this.mProcessBuffer = new MemoryFile("GFXStats-" + uid, GraphicsStatsService.ASHMEM_SIZE);
            this.mPreviousData = GraphicsStatsService.this.removeHistoricalDataLocked(this.mUid, this.mPackageName);
            if (this.mPreviousData == null) {
                return;
            }
            this.mProcessBuffer.writeBytes(this.mPreviousData.mBuffer, 0, 0, GraphicsStatsService.ASHMEM_SIZE);
        }

        @Override
        public void binderDied() {
            this.mToken.unlinkToDeath(this, 0);
            GraphicsStatsService.this.processDied(this);
        }

        void closeAllBuffers() {
            if (this.mProcessBuffer == null) {
                return;
            }
            this.mProcessBuffer.close();
            this.mProcessBuffer = null;
        }
    }

    private static final class HistoricalData {
        final byte[] mBuffer;
        String mPackageName;
        int mUid;

        HistoricalData(HistoricalData historicalData) {
            this();
        }

        private HistoricalData() {
            this.mBuffer = new byte[GraphicsStatsService.ASHMEM_SIZE];
        }

        void update(String packageName, int uid, MemoryFile file) {
            this.mUid = uid;
            this.mPackageName = packageName;
            try {
                file.readBytes(this.mBuffer, 0, 0, GraphicsStatsService.ASHMEM_SIZE);
            } catch (IOException e) {
            }
        }
    }
}
