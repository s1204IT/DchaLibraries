package com.android.server.storage;

import android.os.ParcelFileDescriptor;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.FuseUnavailableMountException;
import com.android.internal.util.Preconditions;
import com.android.server.NativeDaemonConnectorException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;
import libcore.io.IoUtils;

public class AppFuseBridge implements Runnable {
    private static final String APPFUSE_MOUNT_NAME_TEMPLATE = "/mnt/appfuse/%d_%d";
    public static final String TAG = "AppFuseBridge";

    @GuardedBy("this")
    private final SparseArray<MountScope> mScopes = new SparseArray<>();

    @GuardedBy("this")
    private long mNativeLoop = native_new();

    private native int native_add_bridge(long j, int i, int i2);

    private native void native_delete(long j);

    private native long native_new();

    private native void native_start_loop(long j);

    public ParcelFileDescriptor addBridge(MountScope mountScope) throws FuseUnavailableMountException, NativeDaemonConnectorException {
        ParcelFileDescriptor parcelFileDescriptorAdoptFd;
        try {
            synchronized (this) {
                Preconditions.checkArgument(this.mScopes.indexOfKey(mountScope.mountId) < 0);
                if (this.mNativeLoop == 0) {
                    throw new FuseUnavailableMountException(mountScope.mountId);
                }
                int iNative_add_bridge = native_add_bridge(this.mNativeLoop, mountScope.mountId, mountScope.open().detachFd());
                if (iNative_add_bridge == -1) {
                    throw new FuseUnavailableMountException(mountScope.mountId);
                }
                parcelFileDescriptorAdoptFd = ParcelFileDescriptor.adoptFd(iNative_add_bridge);
                this.mScopes.put(mountScope.mountId, mountScope);
            }
            IoUtils.closeQuietly((AutoCloseable) null);
            return parcelFileDescriptorAdoptFd;
        } catch (Throwable th) {
            IoUtils.closeQuietly(mountScope);
            throw th;
        }
    }

    @Override
    public void run() {
        native_start_loop(this.mNativeLoop);
        synchronized (this) {
            native_delete(this.mNativeLoop);
            this.mNativeLoop = 0L;
        }
    }

    public ParcelFileDescriptor openFile(int i, int i2, int i3, int i4) throws InterruptedException, FuseUnavailableMountException {
        MountScope mountScope;
        synchronized (this) {
            mountScope = this.mScopes.get(i2);
            if (mountScope == null) {
                throw new FuseUnavailableMountException(i2);
            }
        }
        if (mountScope.pid != i) {
            throw new SecurityException("PID does not match");
        }
        if (!mountScope.waitForMount()) {
            throw new FuseUnavailableMountException(i2);
        }
        try {
            return ParcelFileDescriptor.open(new File(mountScope.mountPoint, String.valueOf(i3)), i4);
        } catch (FileNotFoundException e) {
            throw new FuseUnavailableMountException(i2);
        }
    }

    private synchronized void onMount(int i) {
        MountScope mountScope = this.mScopes.get(i);
        if (mountScope != null) {
            mountScope.setMountResultLocked(true);
        }
    }

    private synchronized void onClosed(int i) {
        MountScope mountScope = this.mScopes.get(i);
        if (mountScope != null) {
            mountScope.setMountResultLocked(false);
            IoUtils.closeQuietly(mountScope);
            this.mScopes.remove(i);
        }
    }

    public static abstract class MountScope implements AutoCloseable {
        public final int mountId;
        public final File mountPoint;
        public final int pid;
        public final int uid;
        private final CountDownLatch mMounted = new CountDownLatch(1);
        private boolean mMountResult = false;

        public abstract ParcelFileDescriptor open() throws NativeDaemonConnectorException;

        public MountScope(int i, int i2, int i3) {
            this.uid = i;
            this.pid = i2;
            this.mountId = i3;
            this.mountPoint = new File(String.format(AppFuseBridge.APPFUSE_MOUNT_NAME_TEMPLATE, Integer.valueOf(i), Integer.valueOf(i3)));
        }

        @GuardedBy("AppFuseBridge.this")
        void setMountResultLocked(boolean z) {
            if (this.mMounted.getCount() == 0) {
                return;
            }
            this.mMountResult = z;
            this.mMounted.countDown();
        }

        boolean waitForMount() throws InterruptedException {
            this.mMounted.await();
            return this.mMountResult;
        }
    }
}
