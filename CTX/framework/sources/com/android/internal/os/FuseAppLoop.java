package com.android.internal.os;

import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ProxyFileDescriptorCallback;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

public class FuseAppLoop implements Handler.Callback {
    private static final int ARGS_POOL_SIZE = 50;
    private static final int FUSE_FSYNC = 20;
    private static final int FUSE_GETATTR = 3;
    private static final int FUSE_LOOKUP = 1;
    private static final int FUSE_MAX_WRITE = 131072;
    private static final int FUSE_OK = 0;
    private static final int FUSE_OPEN = 14;
    private static final int FUSE_READ = 15;
    private static final int FUSE_RELEASE = 18;
    private static final int FUSE_WRITE = 16;
    private static final int MIN_INODE = 2;
    public static final int ROOT_INODE = 1;

    @GuardedBy("mLock")
    private long mInstance;
    private final int mMountPointId;
    private final Thread mThread;
    private static final String TAG = "FuseAppLoop";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final ThreadFactory sDefaultThreadFactory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, FuseAppLoop.TAG);
        }
    };
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final SparseArray<CallbackEntry> mCallbackMap = new SparseArray<>();

    @GuardedBy("mLock")
    private final BytesMap mBytesMap = new BytesMap();

    @GuardedBy("mLock")
    private final LinkedList<Args> mArgsPool = new LinkedList<>();

    @GuardedBy("mLock")
    private int mNextInode = 2;

    public static class UnmountedException extends Exception {
    }

    native void native_delete(long j);

    native long native_new(int i);

    native void native_replyGetAttr(long j, long j2, long j3, long j4);

    native void native_replyLookup(long j, long j2, long j3, long j4);

    native void native_replyOpen(long j, long j2, long j3);

    native void native_replyRead(long j, long j2, int i, byte[] bArr);

    native void native_replySimple(long j, long j2, int i);

    native void native_replyWrite(long j, long j2, int i);

    native void native_start(long j);

    public FuseAppLoop(int i, ParcelFileDescriptor parcelFileDescriptor, ThreadFactory threadFactory) {
        this.mMountPointId = i;
        threadFactory = threadFactory == null ? sDefaultThreadFactory : threadFactory;
        this.mInstance = native_new(parcelFileDescriptor.detachFd());
        this.mThread = threadFactory.newThread(new Runnable() {
            @Override
            public final void run() {
                FuseAppLoop.lambda$new$0(this.f$0);
            }
        });
        this.mThread.start();
    }

    public static void lambda$new$0(FuseAppLoop fuseAppLoop) {
        fuseAppLoop.native_start(fuseAppLoop.mInstance);
        synchronized (fuseAppLoop.mLock) {
            fuseAppLoop.native_delete(fuseAppLoop.mInstance);
            fuseAppLoop.mInstance = 0L;
            fuseAppLoop.mBytesMap.clear();
        }
    }

    public int registerCallback(ProxyFileDescriptorCallback proxyFileDescriptorCallback, Handler handler) throws FuseUnavailableMountException {
        int i;
        synchronized (this.mLock) {
            Preconditions.checkNotNull(proxyFileDescriptorCallback);
            Preconditions.checkNotNull(handler);
            Preconditions.checkState(this.mCallbackMap.size() < 2147483645, "Too many opened files.");
            Preconditions.checkArgument(Thread.currentThread().getId() != handler.getLooper().getThread().getId(), "Handler must be different from the current thread");
            if (this.mInstance == 0) {
                throw new FuseUnavailableMountException(this.mMountPointId);
            }
            do {
                i = this.mNextInode;
                this.mNextInode++;
                if (this.mNextInode < 0) {
                    this.mNextInode = 2;
                }
            } while (this.mCallbackMap.get(i) != null);
            this.mCallbackMap.put(i, new CallbackEntry(proxyFileDescriptorCallback, new Handler(handler.getLooper(), this)));
        }
        return i;
    }

    public void unregisterCallback(int i) {
        synchronized (this.mLock) {
            this.mCallbackMap.remove(i);
        }
    }

    public int getMountPointId() {
        return this.mMountPointId;
    }

    @Override
    public boolean handleMessage(Message message) throws Throwable {
        Object obj;
        Object obj2;
        Args args = (Args) message.obj;
        CallbackEntry callbackEntry = args.entry;
        long j = args.inode;
        long j2 = args.unique;
        int i = args.size;
        long j3 = args.offset;
        byte[] bArr = args.data;
        try {
            int i2 = message.what;
            if (i2 == 1) {
                long jOnGetSize = callbackEntry.callback.onGetSize();
                Object obj3 = this.mLock;
                try {
                    synchronized (obj3) {
                        try {
                            if (this.mInstance != 0) {
                                obj = obj3;
                                native_replyLookup(this.mInstance, j2, j, jOnGetSize);
                            } else {
                                obj = obj3;
                            }
                            recycleLocked(args);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
                throw th;
            }
            if (i2 == 3) {
                long jOnGetSize2 = callbackEntry.callback.onGetSize();
                Object obj4 = this.mLock;
                try {
                    synchronized (obj4) {
                        try {
                            if (this.mInstance != 0) {
                                obj2 = obj4;
                                native_replyGetAttr(this.mInstance, j2, j, jOnGetSize2);
                            } else {
                                obj2 = obj4;
                            }
                            recycleLocked(args);
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
                throw th;
            }
            if (i2 == 18) {
                callbackEntry.callback.onRelease();
                synchronized (this.mLock) {
                    if (this.mInstance != 0) {
                        native_replySimple(this.mInstance, j2, 0);
                    }
                    this.mBytesMap.stopUsing(callbackEntry.getThreadId());
                    recycleLocked(args);
                }
            } else if (i2 != 20) {
                switch (i2) {
                    case 15:
                        int iOnRead = callbackEntry.callback.onRead(j3, i, bArr);
                        synchronized (this.mLock) {
                            if (this.mInstance != 0) {
                                native_replyRead(this.mInstance, j2, iOnRead, bArr);
                            }
                            recycleLocked(args);
                            break;
                        }
                        break;
                    case 16:
                        int iOnWrite = callbackEntry.callback.onWrite(j3, i, bArr);
                        synchronized (this.mLock) {
                            if (this.mInstance != 0) {
                                native_replyWrite(this.mInstance, j2, iOnWrite);
                            }
                            recycleLocked(args);
                            break;
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown FUSE command: " + message.what);
                }
            } else {
                callbackEntry.callback.onFsync();
                synchronized (this.mLock) {
                    if (this.mInstance != 0) {
                        native_replySimple(this.mInstance, j2, 0);
                    }
                    recycleLocked(args);
                }
            }
            return true;
        } catch (Exception e) {
            synchronized (this.mLock) {
                Log.e(TAG, "", e);
                replySimpleLocked(j2, getError(e));
                recycleLocked(args);
                return true;
            }
        }
    }

    private void onCommand(int i, long j, long j2, long j3, int i2, byte[] bArr) {
        Args argsPop;
        synchronized (this.mLock) {
            try {
                if (this.mArgsPool.size() == 0) {
                    argsPop = new Args();
                } else {
                    argsPop = this.mArgsPool.pop();
                }
                argsPop.unique = j;
                argsPop.inode = j2;
                argsPop.offset = j3;
                argsPop.size = i2;
                argsPop.data = bArr;
                argsPop.entry = getCallbackEntryOrThrowLocked(j2);
                if (!argsPop.entry.handler.sendMessage(Message.obtain(argsPop.entry.handler, i, 0, 0, argsPop))) {
                    throw new ErrnoException("onCommand", OsConstants.EBADF);
                }
            } catch (Exception e) {
                replySimpleLocked(j, getError(e));
            }
        }
    }

    private byte[] onOpen(long j, long j2) {
        CallbackEntry callbackEntryOrThrowLocked;
        synchronized (this.mLock) {
            try {
                callbackEntryOrThrowLocked = getCallbackEntryOrThrowLocked(j2);
            } catch (ErrnoException e) {
                replySimpleLocked(j, getError(e));
            }
            if (callbackEntryOrThrowLocked.opened) {
                throw new ErrnoException("onOpen", OsConstants.EMFILE);
            }
            if (this.mInstance != 0) {
                native_replyOpen(this.mInstance, j, j2);
                callbackEntryOrThrowLocked.opened = true;
                return this.mBytesMap.startUsing(callbackEntryOrThrowLocked.getThreadId());
            }
            return null;
        }
    }

    private static int getError(Exception exc) {
        int i;
        if ((exc instanceof ErrnoException) && (i = ((ErrnoException) exc).errno) != OsConstants.ENOSYS) {
            return -i;
        }
        return -OsConstants.EBADF;
    }

    @GuardedBy("mLock")
    private CallbackEntry getCallbackEntryOrThrowLocked(long j) throws ErrnoException {
        CallbackEntry callbackEntry = this.mCallbackMap.get(checkInode(j));
        if (callbackEntry == null) {
            throw new ErrnoException("getCallbackEntryOrThrowLocked", OsConstants.ENOENT);
        }
        return callbackEntry;
    }

    @GuardedBy("mLock")
    private void recycleLocked(Args args) {
        if (this.mArgsPool.size() < 50) {
            this.mArgsPool.add(args);
        }
    }

    @GuardedBy("mLock")
    private void replySimpleLocked(long j, int i) {
        if (this.mInstance != 0) {
            native_replySimple(this.mInstance, j, i);
        }
    }

    private static int checkInode(long j) {
        Preconditions.checkArgumentInRange(j, 2L, 2147483647L, "checkInode");
        return (int) j;
    }

    private static class CallbackEntry {
        final ProxyFileDescriptorCallback callback;
        final Handler handler;
        boolean opened;

        CallbackEntry(ProxyFileDescriptorCallback proxyFileDescriptorCallback, Handler handler) {
            this.callback = (ProxyFileDescriptorCallback) Preconditions.checkNotNull(proxyFileDescriptorCallback);
            this.handler = (Handler) Preconditions.checkNotNull(handler);
        }

        long getThreadId() {
            return this.handler.getLooper().getThread().getId();
        }
    }

    private static class BytesMapEntry {
        byte[] bytes;
        int counter;

        private BytesMapEntry() {
            this.counter = 0;
            this.bytes = new byte[131072];
        }
    }

    private static class BytesMap {
        final Map<Long, BytesMapEntry> mEntries;

        private BytesMap() {
            this.mEntries = new HashMap();
        }

        byte[] startUsing(long j) {
            BytesMapEntry bytesMapEntry = this.mEntries.get(Long.valueOf(j));
            if (bytesMapEntry == null) {
                bytesMapEntry = new BytesMapEntry();
                this.mEntries.put(Long.valueOf(j), bytesMapEntry);
            }
            bytesMapEntry.counter++;
            return bytesMapEntry.bytes;
        }

        void stopUsing(long j) {
            BytesMapEntry bytesMapEntry = this.mEntries.get(Long.valueOf(j));
            Preconditions.checkNotNull(bytesMapEntry);
            bytesMapEntry.counter--;
            if (bytesMapEntry.counter <= 0) {
                this.mEntries.remove(Long.valueOf(j));
            }
        }

        void clear() {
            this.mEntries.clear();
        }
    }

    private static class Args {
        byte[] data;
        CallbackEntry entry;
        long inode;
        long offset;
        int size;
        long unique;

        private Args() {
        }
    }
}
