package sun.nio.fs;

import com.sun.nio.file.SensitivityWatchEventModifier;
import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.CloseGuard;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import sun.misc.Unsafe;

class LinuxWatchService extends AbstractWatchService {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private final Poller poller;

    private static native void configureBlocking(int i, boolean z) throws UnixException;

    private static native int[] eventOffsets();

    private static native int eventSize();

    private static native int inotifyAddWatch(int i, long j, int i2) throws UnixException;

    private static native int inotifyInit() throws UnixException;

    private static native void inotifyRmWatch(int i, int i2) throws UnixException;

    private static native int poll(int i, int i2) throws UnixException;

    private static native void socketpair(int[] iArr) throws UnixException;

    LinuxWatchService(UnixFileSystem unixFileSystem) throws IOException {
        String strErrorString;
        try {
            int iInotifyInit = inotifyInit();
            int[] iArr = new int[2];
            try {
                configureBlocking(iInotifyInit, false);
                socketpair(iArr);
                configureBlocking(iArr[0], false);
                this.poller = new Poller(unixFileSystem, this, iInotifyInit, iArr);
                this.poller.start();
            } catch (UnixException e) {
                UnixNativeDispatcher.close(iInotifyInit);
                throw new IOException(e.errorString());
            }
        } catch (UnixException e2) {
            if (e2.errno() != UnixConstants.EMFILE) {
                strErrorString = e2.errorString();
            } else {
                strErrorString = "User limit of inotify instances reached or too many open files";
            }
            throw new IOException(strErrorString);
        }
    }

    @Override
    WatchKey register(Path path, WatchEvent.Kind<?>[] kindArr, WatchEvent.Modifier... modifierArr) throws IOException {
        return this.poller.register(path, kindArr, modifierArr);
    }

    @Override
    void implClose() throws IOException {
        this.poller.close();
    }

    private static class LinuxWatchKey extends AbstractWatchKey {
        private final int ifd;
        private volatile int wd;

        LinuxWatchKey(UnixPath unixPath, LinuxWatchService linuxWatchService, int i, int i2) {
            super(unixPath, linuxWatchService);
            this.ifd = i;
            this.wd = i2;
        }

        int descriptor() {
            return this.wd;
        }

        void invalidate(boolean z) {
            if (z) {
                try {
                    LinuxWatchService.inotifyRmWatch(this.ifd, this.wd);
                } catch (UnixException e) {
                }
            }
            this.wd = -1;
        }

        @Override
        public boolean isValid() {
            return this.wd != -1;
        }

        @Override
        public void cancel() {
            if (isValid()) {
                ((LinuxWatchService) watcher()).poller.cancel(this);
            }
        }
    }

    private static class Poller extends AbstractPoller {
        private static final int BUFFER_SIZE = 8192;
        private static final int IN_ATTRIB = 4;
        private static final int IN_CREATE = 256;
        private static final int IN_DELETE = 512;
        private static final int IN_IGNORED = 32768;
        private static final int IN_MODIFY = 2;
        private static final int IN_MOVED_FROM = 64;
        private static final int IN_MOVED_TO = 128;
        private static final int IN_Q_OVERFLOW = 16384;
        private static final int IN_UNMOUNT = 8192;
        private final UnixFileSystem fs;
        private final int ifd;
        private final int[] socketpair;
        private final LinuxWatchService watcher;
        private static final int SIZEOF_INOTIFY_EVENT = LinuxWatchService.eventSize();
        private static final int[] offsets = LinuxWatchService.eventOffsets();
        private static final int OFFSETOF_WD = offsets[0];
        private static final int OFFSETOF_MASK = offsets[1];
        private static final int OFFSETOF_LEN = offsets[3];
        private static final int OFFSETOF_NAME = offsets[4];

        @ReachabilitySensitive
        private final CloseGuard guard = CloseGuard.get();
        private final Map<Integer, LinuxWatchKey> wdToKey = new HashMap();
        private final long address = LinuxWatchService.unsafe.allocateMemory(8192);

        Poller(UnixFileSystem unixFileSystem, LinuxWatchService linuxWatchService, int i, int[] iArr) {
            this.fs = unixFileSystem;
            this.watcher = linuxWatchService;
            this.ifd = i;
            this.socketpair = iArr;
            this.guard.open("close");
        }

        @Override
        void wakeup() throws IOException {
            try {
                UnixNativeDispatcher.write(this.socketpair[1], this.address, 1);
            } catch (UnixException e) {
                throw new IOException(e.errorString());
            }
        }

        @Override
        Object implRegister(Path path, Set<? extends WatchEvent.Kind<?>> set, WatchEvent.Modifier... modifierArr) {
            UnixPath unixPath = (UnixPath) path;
            int i = 0;
            for (WatchEvent.Kind<?> kind : set) {
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    i |= 384;
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    i |= 576;
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    i |= 6;
                }
            }
            if (modifierArr.length > 0) {
                for (WatchEvent.Modifier modifier : modifierArr) {
                    if (modifier == null) {
                        return new NullPointerException();
                    }
                    if (!(modifier instanceof SensitivityWatchEventModifier)) {
                        return new UnsupportedOperationException("Modifier not supported");
                    }
                }
            }
            try {
                if (!UnixFileAttributes.get(unixPath, true).isDirectory()) {
                    return new NotDirectoryException(unixPath.getPathForExceptionMessage());
                }
                try {
                    NativeBuffer nativeBufferAsNativeBuffer = NativeBuffers.asNativeBuffer(unixPath.getByteArrayForSysCalls());
                    try {
                        int iInotifyAddWatch = LinuxWatchService.inotifyAddWatch(this.ifd, nativeBufferAsNativeBuffer.address(), i);
                        LinuxWatchKey linuxWatchKey = this.wdToKey.get(Integer.valueOf(iInotifyAddWatch));
                        if (linuxWatchKey == null) {
                            LinuxWatchKey linuxWatchKey2 = new LinuxWatchKey(unixPath, this.watcher, this.ifd, iInotifyAddWatch);
                            this.wdToKey.put(Integer.valueOf(iInotifyAddWatch), linuxWatchKey2);
                            return linuxWatchKey2;
                        }
                        return linuxWatchKey;
                    } finally {
                        nativeBufferAsNativeBuffer.release();
                    }
                } catch (UnixException e) {
                    if (e.errno() == UnixConstants.ENOSPC) {
                        return new IOException("User limit of inotify watches reached");
                    }
                    return e.asIOException(unixPath);
                }
            } catch (UnixException e2) {
                return e2.asIOException(unixPath);
            }
        }

        @Override
        void implCancelKey(WatchKey watchKey) {
            LinuxWatchKey linuxWatchKey = (LinuxWatchKey) watchKey;
            if (linuxWatchKey.isValid()) {
                this.wdToKey.remove(Integer.valueOf(linuxWatchKey.descriptor()));
                linuxWatchKey.invalidate(true);
            }
        }

        @Override
        void implCloseAll() {
            this.guard.close();
            Iterator<Map.Entry<Integer, LinuxWatchKey>> it = this.wdToKey.entrySet().iterator();
            while (it.hasNext()) {
                it.next().getValue().invalidate(true);
            }
            this.wdToKey.clear();
            LinuxWatchService.unsafe.freeMemory(this.address);
            UnixNativeDispatcher.close(this.socketpair[0]);
            UnixNativeDispatcher.close(this.socketpair[1]);
            UnixNativeDispatcher.close(this.ifd);
        }

        protected void finalize() throws Throwable {
            try {
                if (this.guard != null) {
                    this.guard.warnIfOpen();
                }
                close();
            } finally {
                super.finalize();
            }
        }

        @Override
        public void run() throws UnixException {
            int i;
            int i2;
            UnixPath unixPath;
            while (true) {
                try {
                    int i3 = 0;
                    int iPoll = LinuxWatchService.poll(this.ifd, this.socketpair[0]);
                    try {
                        i = UnixNativeDispatcher.read(this.ifd, this.address, 8192);
                    } catch (UnixException e) {
                        if (e.errno() != UnixConstants.EAGAIN) {
                            throw e;
                        }
                        i = 0;
                    }
                    if (iPoll > 1 || (iPoll == 1 && i == 0)) {
                        try {
                            UnixNativeDispatcher.read(this.socketpair[0], this.address, 8192);
                            if (processRequests()) {
                                return;
                            }
                        } catch (UnixException e2) {
                            if (e2.errno() != UnixConstants.EAGAIN) {
                                throw e2;
                            }
                        }
                    }
                    int i4 = 0;
                    while (i4 < i) {
                        long j = this.address + ((long) i4);
                        int i5 = LinuxWatchService.unsafe.getInt(((long) OFFSETOF_WD) + j);
                        int i6 = LinuxWatchService.unsafe.getInt(((long) OFFSETOF_MASK) + j);
                        int i7 = LinuxWatchService.unsafe.getInt(((long) OFFSETOF_LEN) + j);
                        if (i7 > 0) {
                            int i8 = i7;
                            while (i8 > 0) {
                                if (LinuxWatchService.unsafe.getByte(((((long) OFFSETOF_NAME) + j) + ((long) i8)) - 1) != 0) {
                                    break;
                                } else {
                                    i8--;
                                }
                            }
                            if (i8 > 0) {
                                byte[] bArr = new byte[i8];
                                int i9 = i3;
                                while (i9 < i8) {
                                    bArr[i9] = LinuxWatchService.unsafe.getByte(((long) OFFSETOF_NAME) + j + ((long) i9));
                                    i9++;
                                    i6 = i6;
                                }
                                i2 = i6;
                                unixPath = new UnixPath(this.fs, bArr);
                            } else {
                                i2 = i6;
                                unixPath = null;
                            }
                        }
                        processEvent(i5, i2, unixPath);
                        i4 += SIZEOF_INOTIFY_EVENT + i7;
                        i3 = 0;
                    }
                } catch (UnixException e3) {
                    e3.printStackTrace();
                    return;
                }
            }
        }

        private WatchEvent.Kind<?> maskToEventKind(int i) {
            if ((i & 2) > 0) {
                return StandardWatchEventKinds.ENTRY_MODIFY;
            }
            if ((i & 4) > 0) {
                return StandardWatchEventKinds.ENTRY_MODIFY;
            }
            if ((i & 256) > 0) {
                return StandardWatchEventKinds.ENTRY_CREATE;
            }
            if ((i & 128) > 0) {
                return StandardWatchEventKinds.ENTRY_CREATE;
            }
            if ((i & 512) > 0) {
                return StandardWatchEventKinds.ENTRY_DELETE;
            }
            if ((i & 64) > 0) {
                return StandardWatchEventKinds.ENTRY_DELETE;
            }
            return null;
        }

        private void processEvent(int i, int i2, UnixPath unixPath) {
            WatchEvent.Kind<?> kindMaskToEventKind;
            if ((i2 & 16384) > 0) {
                Iterator<Map.Entry<Integer, LinuxWatchKey>> it = this.wdToKey.entrySet().iterator();
                while (it.hasNext()) {
                    it.next().getValue().signalEvent(StandardWatchEventKinds.OVERFLOW, null);
                }
                return;
            }
            LinuxWatchKey linuxWatchKey = this.wdToKey.get(Integer.valueOf(i));
            if (linuxWatchKey == null) {
                return;
            }
            if ((32768 & i2) > 0) {
                this.wdToKey.remove(Integer.valueOf(i));
                linuxWatchKey.invalidate(false);
                linuxWatchKey.signal();
            } else if (unixPath != null && (kindMaskToEventKind = maskToEventKind(i2)) != null) {
                linuxWatchKey.signalEvent(kindMaskToEventKind, unixPath);
            }
        }
    }
}
