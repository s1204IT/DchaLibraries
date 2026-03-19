package com.android.server;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.app.ResolverActivity;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.server.pm.Settings;
import dalvik.system.DexFile;
import dalvik.system.VMRuntime;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PinnerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final int MAX_CAMERA_PIN_SIZE = 83886080;
    private static final int PAGE_SIZE = (int) Os.sysconf(OsConstants._SC_PAGESIZE);
    private static final String PIN_META_FILENAME = "pinlist.meta";
    private static final String TAG = "PinnerService";
    private BinderService mBinderService;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private final ArrayList<PinnedFile> mPinnedCameraFiles;
    private final ArrayList<PinnedFile> mPinnedFiles;
    private PinnerHandler mPinnerHandler;
    private final boolean mShouldPinCamera;

    public PinnerService(Context context) {
        super(context);
        this.mPinnedFiles = new ArrayList<>();
        this.mPinnedCameraFiles = new ArrayList<>();
        this.mPinnerHandler = null;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction() == "android.intent.action.PACKAGE_REPLACED") {
                    String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
                    ArraySet<String> arraySet = new ArraySet<>();
                    arraySet.add(schemeSpecificPart);
                    PinnerService.this.update(arraySet);
                }
            }
        };
        this.mContext = context;
        this.mShouldPinCamera = context.getResources().getBoolean(R.^attr-private.magnifierElevation);
        this.mPinnerHandler = new PinnerHandler(BackgroundThread.get().getLooper());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStart() {
        this.mBinderService = new BinderService();
        publishBinderService("pinner", this.mBinderService);
        publishLocalService(PinnerService.class, this);
        this.mPinnerHandler.obtainMessage(4001).sendToTarget();
        this.mPinnerHandler.obtainMessage(4000, 0, 0).sendToTarget();
    }

    @Override
    public void onSwitchUser(int i) {
        this.mPinnerHandler.obtainMessage(4000, i, 0).sendToTarget();
    }

    public void update(ArraySet<String> arraySet) {
        ApplicationInfo cameraInfo = getCameraInfo(0);
        if (cameraInfo != null && arraySet.contains(cameraInfo.packageName)) {
            Slog.i(TAG, "Updating pinned files.");
            this.mPinnerHandler.obtainMessage(4000, 0, 0).sendToTarget();
        }
    }

    private void handlePinOnStart() throws Throwable {
        for (String str : this.mContext.getResources().getStringArray(R.array.config_backGestureInsetScales)) {
            PinnedFile pinnedFilePinFile = pinFile(str, Integer.MAX_VALUE, false);
            if (pinnedFilePinFile == null) {
                Slog.e(TAG, "Failed to pin file = " + str);
            } else {
                synchronized (this) {
                    this.mPinnedFiles.add(pinnedFilePinFile);
                }
            }
        }
    }

    private void handlePinCamera(int i) throws Throwable {
        if (this.mShouldPinCamera) {
            pinCamera(i);
        }
    }

    private void unpinCameraApp() {
        ArrayList arrayList;
        synchronized (this) {
            arrayList = new ArrayList(this.mPinnedCameraFiles);
            this.mPinnedCameraFiles.clear();
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ((PinnedFile) it.next()).close();
        }
    }

    private boolean isResolverActivity(ActivityInfo activityInfo) {
        return ResolverActivity.class.getName().equals(activityInfo.name);
    }

    private ApplicationInfo getCameraInfo(int i) {
        ResolveInfo resolveInfoResolveActivityAsUser = this.mContext.getPackageManager().resolveActivityAsUser(new Intent("android.media.action.STILL_IMAGE_CAMERA"), 851968, i);
        if (resolveInfoResolveActivityAsUser == null || isResolverActivity(resolveInfoResolveActivityAsUser.activityInfo)) {
            return null;
        }
        return resolveInfoResolveActivityAsUser.activityInfo.applicationInfo;
    }

    private boolean pinCamera(int i) throws Throwable {
        String[] dexFileOutputPaths;
        ApplicationInfo cameraInfo = getCameraInfo(i);
        if (cameraInfo == null) {
            return false;
        }
        unpinCameraApp();
        String str = cameraInfo.sourceDir;
        PinnedFile pinnedFilePinFile = pinFile(str, MAX_CAMERA_PIN_SIZE, true);
        if (pinnedFilePinFile == null) {
            Slog.e(TAG, "Failed to pin " + str);
            return false;
        }
        synchronized (this) {
            this.mPinnedCameraFiles.add(pinnedFilePinFile);
        }
        String str2 = "arm";
        if (cameraInfo.primaryCpuAbi == null) {
            if (VMRuntime.is64BitAbi(Build.SUPPORTED_ABIS[0])) {
                str2 = "arm64";
            }
        } else if (VMRuntime.is64BitAbi(cameraInfo.primaryCpuAbi)) {
            str2 = "arm64";
        }
        try {
            dexFileOutputPaths = DexFile.getDexFileOutputPaths(cameraInfo.getBaseCodePath(), str2);
        } catch (IOException e) {
            dexFileOutputPaths = null;
        }
        if (dexFileOutputPaths == null) {
            return true;
        }
        for (String str3 : dexFileOutputPaths) {
            PinnedFile pinnedFilePinFile2 = pinFile(str3, MAX_CAMERA_PIN_SIZE, false);
            if (pinnedFilePinFile2 != null) {
                synchronized (this) {
                    this.mPinnedCameraFiles.add(pinnedFilePinFile2);
                }
            }
        }
        return true;
    }

    private static PinnedFile pinFile(String str, int i, boolean z) throws Throwable {
        ZipFile zipFileMaybeOpenZip;
        InputStream inputStreamMaybeOpenPinMetaInZip = null;
        if (z) {
            try {
                zipFileMaybeOpenZip = maybeOpenZip(str);
            } catch (Throwable th) {
                th = th;
                zipFileMaybeOpenZip = null;
                safeClose(inputStreamMaybeOpenPinMetaInZip);
                safeClose(zipFileMaybeOpenZip);
                throw th;
            }
        } else {
            zipFileMaybeOpenZip = null;
        }
        if (zipFileMaybeOpenZip != null) {
            try {
                inputStreamMaybeOpenPinMetaInZip = maybeOpenPinMetaInZip(zipFileMaybeOpenZip, str);
            } catch (Throwable th2) {
                th = th2;
                safeClose(inputStreamMaybeOpenPinMetaInZip);
                safeClose(zipFileMaybeOpenZip);
                throw th;
            }
        }
        Slog.d(TAG, "pinRangeStream: " + inputStreamMaybeOpenPinMetaInZip);
        PinnedFile pinnedFilePinFileRanges = pinFileRanges(str, i, inputStreamMaybeOpenPinMetaInZip != null ? new PinRangeSourceStream(inputStreamMaybeOpenPinMetaInZip) : new PinRangeSourceStatic(0, Integer.MAX_VALUE));
        safeClose(inputStreamMaybeOpenPinMetaInZip);
        safeClose(zipFileMaybeOpenZip);
        return pinnedFilePinFileRanges;
    }

    private static ZipFile maybeOpenZip(String str) {
        try {
            return new ZipFile(str);
        } catch (IOException e) {
            Slog.w(TAG, String.format("could not open \"%s\" as zip: pinning as blob", str), e);
            return null;
        }
    }

    private static InputStream maybeOpenPinMetaInZip(ZipFile zipFile, String str) {
        ZipEntry entry = zipFile.getEntry(PIN_META_FILENAME);
        if (entry != null) {
            try {
                return zipFile.getInputStream(entry);
            } catch (IOException e) {
                Slog.w(TAG, String.format("error reading pin metadata \"%s\": pinning as blob", str), e);
            }
        }
        return null;
    }

    private static abstract class PinRangeSource {
        abstract boolean read(PinRange pinRange);

        private PinRangeSource() {
        }
    }

    private static final class PinRangeSourceStatic extends PinRangeSource {
        private boolean mDone;
        private final int mPinLength;
        private final int mPinStart;

        PinRangeSourceStatic(int i, int i2) {
            super();
            this.mDone = false;
            this.mPinStart = i;
            this.mPinLength = i2;
        }

        @Override
        boolean read(PinRange pinRange) {
            pinRange.start = this.mPinStart;
            pinRange.length = this.mPinLength;
            boolean z = this.mDone;
            this.mDone = true;
            return !z;
        }
    }

    private static final class PinRangeSourceStream extends PinRangeSource {
        private boolean mDone;
        private final DataInputStream mStream;

        PinRangeSourceStream(InputStream inputStream) {
            super();
            this.mDone = false;
            this.mStream = new DataInputStream(inputStream);
        }

        @Override
        boolean read(PinRange pinRange) {
            if (!this.mDone) {
                try {
                    pinRange.start = this.mStream.readInt();
                    pinRange.length = this.mStream.readInt();
                } catch (IOException e) {
                    this.mDone = true;
                }
            }
            return !this.mDone;
        }
    }

    private static PinnedFile pinFileRanges(String str, int i, PinRangeSource pinRangeSource) throws Throwable {
        FileDescriptor fileDescriptorOpen;
        long jMmap;
        int iMin;
        int i2;
        int i3;
        FileDescriptor fileDescriptor;
        int i4;
        int i5;
        int i6;
        FileDescriptor fileDescriptor2 = new FileDescriptor();
        int i7 = 0;
        long j = -1;
        try {
            fileDescriptorOpen = Os.open(str, OsConstants.O_RDONLY | OsConstants.O_CLOEXEC | OsConstants.O_NOFOLLOW, 0);
            try {
                iMin = (int) Math.min(Os.fstat(fileDescriptorOpen).st_size, 2147483647L);
            } catch (ErrnoException e) {
                e = e;
                fileDescriptor2 = fileDescriptorOpen;
                i2 = 0;
                try {
                    Slog.e(TAG, "Could not pin file " + str, e);
                    safeClose(fileDescriptor2);
                    if (j >= 0) {
                        safeMunmap(j, i2);
                    }
                    return null;
                } catch (Throwable th) {
                    th = th;
                    fileDescriptorOpen = fileDescriptor2;
                    iMin = i2;
                    jMmap = j;
                    safeClose(fileDescriptorOpen);
                    if (jMmap >= 0) {
                        safeMunmap(jMmap, iMin);
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                jMmap = -1;
                iMin = 0;
                safeClose(fileDescriptorOpen);
                if (jMmap >= 0) {
                }
                throw th;
            }
            try {
                jMmap = Os.mmap(0L, iMin, OsConstants.PROT_READ, OsConstants.MAP_SHARED, fileDescriptorOpen, 0L);
                try {
                    PinRange pinRange = new PinRange();
                    if (i % PAGE_SIZE != 0) {
                        try {
                            i4 = i - (i % PAGE_SIZE);
                        } catch (ErrnoException e2) {
                            e = e2;
                            i2 = iMin;
                            fileDescriptor2 = fileDescriptorOpen;
                            j = jMmap;
                            Slog.e(TAG, "Could not pin file " + str, e);
                            safeClose(fileDescriptor2);
                            if (j >= 0) {
                            }
                            return null;
                        } catch (Throwable th3) {
                            th = th3;
                            safeClose(fileDescriptorOpen);
                            if (jMmap >= 0) {
                            }
                            throw th;
                        }
                    } else {
                        i4 = i;
                    }
                    i5 = 0;
                    while (i5 < i4) {
                        if (!pinRangeSource.read(pinRange)) {
                            break;
                        }
                        int i8 = pinRange.start;
                        int i9 = pinRange.length;
                        int iClamp = clamp(i7, i8, iMin);
                        int i10 = i4 - i5;
                        int iMin2 = Math.min(i10, clamp(i7, i9, iMin - iClamp)) + (iClamp % PAGE_SIZE);
                        int i11 = iClamp - (iClamp % PAGE_SIZE);
                        if (iMin2 % PAGE_SIZE != 0) {
                            iMin2 += PAGE_SIZE - (iMin2 % PAGE_SIZE);
                        }
                        int iClamp2 = clamp(i7, iMin2, i10);
                        if (iClamp2 > 0) {
                            i6 = i4;
                            Os.mlock(((long) i11) + jMmap, iClamp2);
                        } else {
                            i6 = i4;
                        }
                        i5 += iClamp2;
                        i4 = i6;
                        i7 = 0;
                    }
                    i3 = iMin;
                    fileDescriptor = fileDescriptorOpen;
                } catch (ErrnoException e3) {
                    e = e3;
                    i3 = iMin;
                    fileDescriptor = fileDescriptorOpen;
                } catch (Throwable th4) {
                    th = th4;
                }
                try {
                    PinnedFile pinnedFile = new PinnedFile(jMmap, iMin, str, i5);
                    safeClose(fileDescriptor);
                    return pinnedFile;
                } catch (ErrnoException e4) {
                    e = e4;
                    j = jMmap;
                    i2 = i3;
                    fileDescriptor2 = fileDescriptor;
                    Slog.e(TAG, "Could not pin file " + str, e);
                    safeClose(fileDescriptor2);
                    if (j >= 0) {
                    }
                    return null;
                } catch (Throwable th5) {
                    th = th5;
                    iMin = i3;
                    fileDescriptorOpen = fileDescriptor;
                    safeClose(fileDescriptorOpen);
                    if (jMmap >= 0) {
                    }
                    throw th;
                }
            } catch (ErrnoException e5) {
                e = e5;
                i3 = iMin;
                fileDescriptor = fileDescriptorOpen;
            } catch (Throwable th6) {
                th = th6;
                jMmap = j;
                safeClose(fileDescriptorOpen);
                if (jMmap >= 0) {
                }
                throw th;
            }
        } catch (ErrnoException e6) {
            e = e6;
        } catch (Throwable th7) {
            th = th7;
            fileDescriptorOpen = fileDescriptor2;
        }
    }

    private static int clamp(int i, int i2, int i3) {
        return Math.max(i, Math.min(i2, i3));
    }

    private static void safeMunmap(long j, long j2) {
        try {
            Os.munmap(j, j2);
        } catch (ErrnoException e) {
            Slog.w(TAG, "ignoring error in unmap", e);
        }
    }

    private static void safeClose(FileDescriptor fileDescriptor) {
        if (fileDescriptor != null && fileDescriptor.valid()) {
            try {
                Os.close(fileDescriptor);
            } catch (ErrnoException e) {
                if (e.errno == OsConstants.EBADF) {
                    throw new AssertionError(e);
                }
            }
        }
    }

    private static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Slog.w(TAG, "ignoring error closing resource: " + closeable, e);
            }
        }
    }

    private synchronized ArrayList<PinnedFile> snapshotPinnedFiles() {
        ArrayList<PinnedFile> arrayList;
        arrayList = new ArrayList<>(this.mPinnedFiles.size() + this.mPinnedCameraFiles.size());
        arrayList.addAll(this.mPinnedFiles);
        arrayList.addAll(this.mPinnedCameraFiles);
        return arrayList;
    }

    private final class BinderService extends Binder {
        private BinderService() {
        }

        @Override
        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(PinnerService.this.mContext, PinnerService.TAG, printWriter)) {
                long j = 0;
                for (PinnedFile pinnedFile : PinnerService.this.snapshotPinnedFiles()) {
                    printWriter.format("%s %s\n", pinnedFile.fileName, Integer.valueOf(pinnedFile.bytesPinned));
                    j += (long) pinnedFile.bytesPinned;
                }
                printWriter.format("Total size: %s\n", Long.valueOf(j));
            }
        }
    }

    private static final class PinnedFile implements AutoCloseable {
        final int bytesPinned;
        final String fileName;
        private long mAddress;
        final int mapSize;

        PinnedFile(long j, int i, String str, int i2) {
            this.mAddress = j;
            this.mapSize = i;
            this.fileName = str;
            this.bytesPinned = i2;
        }

        @Override
        public void close() {
            if (this.mAddress >= 0) {
                PinnerService.safeMunmap(this.mAddress, this.mapSize);
                this.mAddress = -1L;
            }
        }

        public void finalize() {
            close();
        }
    }

    static final class PinRange {
        int length;
        int start;

        PinRange() {
        }
    }

    final class PinnerHandler extends Handler {
        static final int PIN_CAMERA_MSG = 4000;
        static final int PIN_ONSTART_MSG = 4001;

        public PinnerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) throws Throwable {
            switch (message.what) {
                case PIN_CAMERA_MSG:
                    PinnerService.this.handlePinCamera(message.arg1);
                    break;
                case PIN_ONSTART_MSG:
                    PinnerService.this.handlePinOnStart();
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }
    }
}
