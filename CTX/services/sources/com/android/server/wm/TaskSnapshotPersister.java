package com.android.server.wm;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.os.Process;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.AtomicFile;
import com.android.server.wm.nano.WindowManagerProtos;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;

class TaskSnapshotPersister {
    private static final String BITMAP_EXTENSION = ".jpg";
    private static final long DELAY_MS = 100;
    static final boolean DISABLE_FULL_SIZED_BITMAPS;
    private static final int MAX_STORE_QUEUE_DEPTH = 2;
    private static final String PROTO_EXTENSION = ".proto";
    private static final int QUALITY = 95;
    private static final String REDUCED_POSTFIX = "_reduced";
    static final float REDUCED_SCALE;
    private static final String SNAPSHOTS_DIRNAME = "snapshots";
    private static final String TAG = "WindowManager";
    private final DirectoryResolver mDirectoryResolver;

    @GuardedBy("mLock")
    private boolean mPaused;

    @GuardedBy("mLock")
    private boolean mQueueIdling;
    private boolean mStarted;

    @GuardedBy("mLock")
    private final ArrayDeque<WriteQueueItem> mWriteQueue = new ArrayDeque<>();

    @GuardedBy("mLock")
    private final ArrayDeque<StoreWriteQueueItem> mStoreQueueItems = new ArrayDeque<>();
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final ArraySet<Integer> mPersistedTaskIdsSinceLastRemoveObsolete = new ArraySet<>();
    private Thread mPersister = new Thread("TaskSnapshotPersister") {
        @Override
        public void run() {
            WriteQueueItem writeQueueItem;
            Process.setThreadPriority(10);
            while (true) {
                synchronized (TaskSnapshotPersister.this.mLock) {
                    if (!TaskSnapshotPersister.this.mPaused) {
                        writeQueueItem = (WriteQueueItem) TaskSnapshotPersister.this.mWriteQueue.poll();
                        if (writeQueueItem != null) {
                            writeQueueItem.onDequeuedLocked();
                        }
                    } else {
                        writeQueueItem = null;
                    }
                }
                if (writeQueueItem != null) {
                    writeQueueItem.write();
                    SystemClock.sleep(TaskSnapshotPersister.DELAY_MS);
                }
                synchronized (TaskSnapshotPersister.this.mLock) {
                    boolean zIsEmpty = TaskSnapshotPersister.this.mWriteQueue.isEmpty();
                    if (zIsEmpty || TaskSnapshotPersister.this.mPaused) {
                        try {
                            TaskSnapshotPersister.this.mQueueIdling = zIsEmpty;
                            TaskSnapshotPersister.this.mLock.wait();
                            TaskSnapshotPersister.this.mQueueIdling = false;
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
    };

    interface DirectoryResolver {
        File getSystemDirectoryForUser(int i);
    }

    static {
        REDUCED_SCALE = ActivityManager.isLowRamDeviceStatic() ? 0.6f : 0.5f;
        DISABLE_FULL_SIZED_BITMAPS = ActivityManager.isLowRamDeviceStatic();
    }

    TaskSnapshotPersister(DirectoryResolver directoryResolver) {
        this.mDirectoryResolver = directoryResolver;
    }

    void start() {
        if (!this.mStarted) {
            this.mStarted = true;
            this.mPersister.start();
        }
    }

    void persistSnapshot(int i, int i2, ActivityManager.TaskSnapshot taskSnapshot) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.add(Integer.valueOf(i));
            sendToQueueLocked(new StoreWriteQueueItem(i, i2, taskSnapshot));
        }
    }

    void onTaskRemovedFromRecents(int i, int i2) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.remove(Integer.valueOf(i));
            sendToQueueLocked(new DeleteWriteQueueItem(i, i2));
        }
    }

    void removeObsoleteFiles(ArraySet<Integer> arraySet, int[] iArr) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.clear();
            sendToQueueLocked(new RemoveObsoleteFilesQueueItem(arraySet, iArr));
        }
    }

    void setPaused(boolean z) {
        synchronized (this.mLock) {
            this.mPaused = z;
            if (!z) {
                this.mLock.notifyAll();
            }
        }
    }

    void waitForQueueEmpty() {
        while (true) {
            synchronized (this.mLock) {
                if (this.mWriteQueue.isEmpty() && this.mQueueIdling) {
                    return;
                }
            }
            SystemClock.sleep(DELAY_MS);
        }
    }

    @GuardedBy("mLock")
    private void sendToQueueLocked(WriteQueueItem writeQueueItem) {
        this.mWriteQueue.offer(writeQueueItem);
        writeQueueItem.onQueuedLocked();
        ensureStoreQueueDepthLocked();
        if (!this.mPaused) {
            this.mLock.notifyAll();
        }
    }

    @GuardedBy("mLock")
    private void ensureStoreQueueDepthLocked() {
        while (this.mStoreQueueItems.size() > 2) {
            StoreWriteQueueItem storeWriteQueueItemPoll = this.mStoreQueueItems.poll();
            this.mWriteQueue.remove(storeWriteQueueItemPoll);
            Slog.i("WindowManager", "Queue is too deep! Purged item with taskid=" + storeWriteQueueItemPoll.mTaskId);
        }
    }

    private File getDirectory(int i) {
        return new File(this.mDirectoryResolver.getSystemDirectoryForUser(i), SNAPSHOTS_DIRNAME);
    }

    File getProtoFile(int i, int i2) {
        return new File(getDirectory(i2), i + PROTO_EXTENSION);
    }

    File getBitmapFile(int i, int i2) {
        if (DISABLE_FULL_SIZED_BITMAPS) {
            Slog.wtf("WindowManager", "This device does not support full sized resolution bitmaps.");
            return null;
        }
        return new File(getDirectory(i2), i + BITMAP_EXTENSION);
    }

    File getReducedResolutionBitmapFile(int i, int i2) {
        return new File(getDirectory(i2), i + REDUCED_POSTFIX + BITMAP_EXTENSION);
    }

    private boolean createDirectory(int i) {
        File directory = getDirectory(i);
        return directory.exists() || directory.mkdirs();
    }

    private void deleteSnapshot(int i, int i2) {
        File protoFile = getProtoFile(i, i2);
        File reducedResolutionBitmapFile = getReducedResolutionBitmapFile(i, i2);
        protoFile.delete();
        reducedResolutionBitmapFile.delete();
        if (!DISABLE_FULL_SIZED_BITMAPS) {
            getBitmapFile(i, i2).delete();
        }
    }

    private abstract class WriteQueueItem {
        abstract void write();

        private WriteQueueItem() {
        }

        void onQueuedLocked() {
        }

        void onDequeuedLocked() {
        }
    }

    private class StoreWriteQueueItem extends WriteQueueItem {
        private final ActivityManager.TaskSnapshot mSnapshot;
        private final int mTaskId;
        private final int mUserId;

        StoreWriteQueueItem(int i, int i2, ActivityManager.TaskSnapshot taskSnapshot) {
            super();
            this.mTaskId = i;
            this.mUserId = i2;
            this.mSnapshot = taskSnapshot;
        }

        @Override
        @GuardedBy("mLock")
        void onQueuedLocked() {
            TaskSnapshotPersister.this.mStoreQueueItems.offer(this);
        }

        @Override
        @GuardedBy("mLock")
        void onDequeuedLocked() {
            TaskSnapshotPersister.this.mStoreQueueItems.remove(this);
        }

        @Override
        void write() {
            if (!TaskSnapshotPersister.this.createDirectory(this.mUserId)) {
                Slog.e("WindowManager", "Unable to create snapshot directory for user dir=" + TaskSnapshotPersister.this.getDirectory(this.mUserId));
            }
            boolean z = false;
            if (!writeProto()) {
                z = true;
            }
            if (!writeBuffer()) {
                z = true;
            }
            if (z) {
                TaskSnapshotPersister.this.deleteSnapshot(this.mTaskId, this.mUserId);
            }
        }

        boolean writeProto() {
            FileOutputStream fileOutputStreamStartWrite;
            WindowManagerProtos.TaskSnapshotProto taskSnapshotProto = new WindowManagerProtos.TaskSnapshotProto();
            taskSnapshotProto.orientation = this.mSnapshot.getOrientation();
            taskSnapshotProto.insetLeft = this.mSnapshot.getContentInsets().left;
            taskSnapshotProto.insetTop = this.mSnapshot.getContentInsets().top;
            taskSnapshotProto.insetRight = this.mSnapshot.getContentInsets().right;
            taskSnapshotProto.insetBottom = this.mSnapshot.getContentInsets().bottom;
            taskSnapshotProto.isRealSnapshot = this.mSnapshot.isRealSnapshot();
            taskSnapshotProto.windowingMode = this.mSnapshot.getWindowingMode();
            taskSnapshotProto.systemUiVisibility = this.mSnapshot.getSystemUiVisibility();
            taskSnapshotProto.isTranslucent = this.mSnapshot.isTranslucent();
            byte[] byteArray = WindowManagerProtos.TaskSnapshotProto.toByteArray(taskSnapshotProto);
            File protoFile = TaskSnapshotPersister.this.getProtoFile(this.mTaskId, this.mUserId);
            AtomicFile atomicFile = new AtomicFile(protoFile);
            try {
                fileOutputStreamStartWrite = atomicFile.startWrite();
            } catch (IOException e) {
                e = e;
                fileOutputStreamStartWrite = null;
            }
            try {
                fileOutputStreamStartWrite.write(byteArray);
                atomicFile.finishWrite(fileOutputStreamStartWrite);
                return true;
            } catch (IOException e2) {
                e = e2;
                atomicFile.failWrite(fileOutputStreamStartWrite);
                Slog.e("WindowManager", "Unable to open " + protoFile + " for persisting. " + e);
                return false;
            }
        }

        boolean writeBuffer() {
            Bitmap bitmapCreateScaledBitmap;
            Bitmap bitmapCreateHardwareBitmap = Bitmap.createHardwareBitmap(this.mSnapshot.getSnapshot());
            if (bitmapCreateHardwareBitmap != null) {
                Bitmap bitmapCopy = bitmapCreateHardwareBitmap.copy(Bitmap.Config.ARGB_8888, false);
                File reducedResolutionBitmapFile = TaskSnapshotPersister.this.getReducedResolutionBitmapFile(this.mTaskId, this.mUserId);
                if (!this.mSnapshot.isReducedResolution()) {
                    bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmapCopy, (int) (bitmapCreateHardwareBitmap.getWidth() * TaskSnapshotPersister.REDUCED_SCALE), (int) (bitmapCreateHardwareBitmap.getHeight() * TaskSnapshotPersister.REDUCED_SCALE), true);
                } else {
                    bitmapCreateScaledBitmap = bitmapCopy;
                }
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(reducedResolutionBitmapFile);
                    bitmapCreateScaledBitmap.compress(Bitmap.CompressFormat.JPEG, TaskSnapshotPersister.QUALITY, fileOutputStream);
                    fileOutputStream.close();
                    if (this.mSnapshot.isReducedResolution()) {
                        return true;
                    }
                    File bitmapFile = TaskSnapshotPersister.this.getBitmapFile(this.mTaskId, this.mUserId);
                    try {
                        FileOutputStream fileOutputStream2 = new FileOutputStream(bitmapFile);
                        bitmapCopy.compress(Bitmap.CompressFormat.JPEG, TaskSnapshotPersister.QUALITY, fileOutputStream2);
                        fileOutputStream2.close();
                        return true;
                    } catch (IOException e) {
                        Slog.e("WindowManager", "Unable to open " + bitmapFile + " for persisting.", e);
                        return false;
                    }
                } catch (IOException e2) {
                    Slog.e("WindowManager", "Unable to open " + reducedResolutionBitmapFile + " for persisting.", e2);
                    return false;
                }
            }
            Slog.e("WindowManager", "Invalid task snapshot hw bitmap");
            return false;
        }
    }

    private class DeleteWriteQueueItem extends WriteQueueItem {
        private final int mTaskId;
        private final int mUserId;

        DeleteWriteQueueItem(int i, int i2) {
            super();
            this.mTaskId = i;
            this.mUserId = i2;
        }

        @Override
        void write() {
            TaskSnapshotPersister.this.deleteSnapshot(this.mTaskId, this.mUserId);
        }
    }

    @VisibleForTesting
    class RemoveObsoleteFilesQueueItem extends WriteQueueItem {
        private final ArraySet<Integer> mPersistentTaskIds;
        private final int[] mRunningUserIds;

        @VisibleForTesting
        RemoveObsoleteFilesQueueItem(ArraySet<Integer> arraySet, int[] iArr) {
            super();
            this.mPersistentTaskIds = new ArraySet<>((ArraySet) arraySet);
            this.mRunningUserIds = iArr;
        }

        @Override
        void write() {
            ArraySet arraySet;
            synchronized (TaskSnapshotPersister.this.mLock) {
                arraySet = new ArraySet(TaskSnapshotPersister.this.mPersistedTaskIdsSinceLastRemoveObsolete);
            }
            for (int i : this.mRunningUserIds) {
                File directory = TaskSnapshotPersister.this.getDirectory(i);
                String[] list = directory.list();
                if (list != null) {
                    for (String str : list) {
                        int taskId = getTaskId(str);
                        if (!this.mPersistentTaskIds.contains(Integer.valueOf(taskId)) && !arraySet.contains(Integer.valueOf(taskId))) {
                            new File(directory, str).delete();
                        }
                    }
                }
            }
        }

        @VisibleForTesting
        int getTaskId(String str) {
            int iLastIndexOf;
            if ((!str.endsWith(TaskSnapshotPersister.PROTO_EXTENSION) && !str.endsWith(TaskSnapshotPersister.BITMAP_EXTENSION)) || (iLastIndexOf = str.lastIndexOf(46)) == -1) {
                return -1;
            }
            String strSubstring = str.substring(0, iLastIndexOf);
            if (strSubstring.endsWith(TaskSnapshotPersister.REDUCED_POSTFIX)) {
                strSubstring = strSubstring.substring(0, strSubstring.length() - TaskSnapshotPersister.REDUCED_POSTFIX.length());
            }
            try {
                return Integer.parseInt(strSubstring);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }
}
