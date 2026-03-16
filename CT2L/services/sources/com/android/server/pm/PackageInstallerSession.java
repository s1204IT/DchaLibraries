package com.android.server.pm;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageParser;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.FileBridge;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.util.ArraySet;
import android.util.ExceptionUtils;
import android.util.MathUtils;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.content.PackageHelper;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.pm.PackageInstallerService;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;
import libcore.io.Libcore;

public class PackageInstallerSession extends IPackageInstallerSession.Stub {
    private static final boolean LOGD = true;
    private static final int MSG_COMMIT = 0;
    private static final String TAG = "PackageInstaller";
    final long createdMillis;
    final String installerPackageName;
    final int installerUid;
    private final PackageInstallerService.InternalCallback mCallback;
    private final Context mContext;
    private String mFinalMessage;
    private int mFinalStatus;
    private final Handler mHandler;
    private String mPackageName;

    @GuardedBy("mLock")
    private boolean mPermissionsAccepted;
    private final PackageManagerService mPm;

    @GuardedBy("mLock")
    private boolean mPrepared;

    @GuardedBy("mLock")
    private IPackageInstallObserver2 mRemoteObserver;

    @GuardedBy("mLock")
    private File mResolvedBaseFile;

    @GuardedBy("mLock")
    private File mResolvedStageDir;

    @GuardedBy("mLock")
    private boolean mSealed;
    private Signature[] mSignatures;
    private int mVersionCode;
    final PackageInstaller.SessionParams params;
    final int sessionId;
    final String stageCid;
    final File stageDir;
    final int userId;
    private final AtomicInteger mActiveCount = new AtomicInteger();
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private float mClientProgress = 0.0f;

    @GuardedBy("mLock")
    private float mInternalProgress = 0.0f;

    @GuardedBy("mLock")
    private float mProgress = 0.0f;

    @GuardedBy("mLock")
    private float mReportedProgress = -1.0f;

    @GuardedBy("mLock")
    private boolean mDestroyed = false;

    @GuardedBy("mLock")
    private ArrayList<FileBridge> mBridges = new ArrayList<>();

    @GuardedBy("mLock")
    private final List<File> mResolvedStagedFiles = new ArrayList();

    @GuardedBy("mLock")
    private final List<File> mResolvedInheritedFiles = new ArrayList();
    private final Handler.Callback mHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            synchronized (PackageInstallerSession.this.mLock) {
                if (msg.obj != null) {
                    PackageInstallerSession.this.mRemoteObserver = (IPackageInstallObserver2) msg.obj;
                }
                try {
                    PackageInstallerSession.this.commitLocked();
                } catch (PackageManagerException e) {
                    String completeMsg = ExceptionUtils.getCompleteMessage(e);
                    Slog.e(PackageInstallerSession.TAG, "Commit of session " + PackageInstallerSession.this.sessionId + " failed: " + completeMsg);
                    PackageInstallerSession.this.destroyInternal();
                    PackageInstallerSession.this.dispatchSessionFinished(e.error, completeMsg, null);
                }
            }
            return PackageInstallerSession.LOGD;
        }
    };

    public PackageInstallerSession(PackageInstallerService.InternalCallback callback, Context context, PackageManagerService pm, Looper looper, int sessionId, int userId, String installerPackageName, int installerUid, PackageInstaller.SessionParams params, long createdMillis, File stageDir, String stageCid, boolean prepared, boolean sealed) {
        this.mPrepared = false;
        this.mSealed = false;
        this.mPermissionsAccepted = false;
        this.mCallback = callback;
        this.mContext = context;
        this.mPm = pm;
        this.mHandler = new Handler(looper, this.mHandlerCallback);
        this.sessionId = sessionId;
        this.userId = userId;
        this.installerPackageName = installerPackageName;
        this.installerUid = installerUid;
        this.params = params;
        this.createdMillis = createdMillis;
        this.stageDir = stageDir;
        this.stageCid = stageCid;
        if ((stageDir == null) == (stageCid == null ? LOGD : false)) {
            throw new IllegalArgumentException("Exactly one of stageDir or stageCid stage must be set");
        }
        this.mPrepared = prepared;
        this.mSealed = sealed;
        if (this.mPm.checkUidPermission("android.permission.INSTALL_PACKAGES", installerUid) == 0 || installerUid == 0) {
            this.mPermissionsAccepted = LOGD;
        } else {
            this.mPermissionsAccepted = false;
        }
    }

    public PackageInstaller.SessionInfo generateInfo() {
        PackageInstaller.SessionInfo info = new PackageInstaller.SessionInfo();
        synchronized (this.mLock) {
            info.sessionId = this.sessionId;
            info.installerPackageName = this.installerPackageName;
            info.resolvedBaseCodePath = this.mResolvedBaseFile != null ? this.mResolvedBaseFile.getAbsolutePath() : null;
            info.progress = this.mProgress;
            info.sealed = this.mSealed;
            info.active = this.mActiveCount.get() > 0 ? LOGD : false;
            info.mode = this.params.mode;
            info.sizeBytes = this.params.sizeBytes;
            info.appPackageName = this.params.appPackageName;
            info.appIcon = this.params.appIcon;
            info.appLabel = this.params.appLabel;
        }
        return info;
    }

    public boolean isPrepared() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPrepared;
        }
        return z;
    }

    public boolean isSealed() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSealed;
        }
        return z;
    }

    private void assertPreparedAndNotSealed(String cookie) {
        synchronized (this.mLock) {
            if (!this.mPrepared) {
                throw new IllegalStateException(cookie + " before prepared");
            }
            if (this.mSealed) {
                throw new SecurityException(cookie + " not allowed after commit");
            }
        }
    }

    private File resolveStageDir() throws IOException {
        File file;
        synchronized (this.mLock) {
            if (this.mResolvedStageDir == null) {
                if (this.stageDir != null) {
                    this.mResolvedStageDir = this.stageDir;
                } else {
                    String path = PackageHelper.getSdDir(this.stageCid);
                    if (path != null) {
                        this.mResolvedStageDir = new File(path);
                    } else {
                        throw new IOException("Failed to resolve path to container " + this.stageCid);
                    }
                }
            }
            file = this.mResolvedStageDir;
        }
        return file;
    }

    public void setClientProgress(float progress) {
        synchronized (this.mLock) {
            boolean forcePublish = this.mClientProgress == 0.0f ? LOGD : false;
            this.mClientProgress = progress;
            computeProgressLocked(forcePublish);
        }
    }

    public void addClientProgress(float progress) {
        synchronized (this.mLock) {
            setClientProgress(this.mClientProgress + progress);
        }
    }

    private void computeProgressLocked(boolean forcePublish) {
        this.mProgress = MathUtils.constrain(this.mClientProgress * 0.8f, 0.0f, 0.8f) + MathUtils.constrain(this.mInternalProgress * 0.2f, 0.0f, 0.2f);
        if (forcePublish || Math.abs(this.mProgress - this.mReportedProgress) >= 0.01d) {
            this.mReportedProgress = this.mProgress;
            this.mCallback.onSessionProgressChanged(this, this.mProgress);
        }
    }

    public String[] getNames() {
        assertPreparedAndNotSealed("getNames");
        try {
            return resolveStageDir().list();
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    public ParcelFileDescriptor openWrite(String name, long offsetBytes, long lengthBytes) {
        try {
            return openWriteInternal(name, offsetBytes, lengthBytes);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private ParcelFileDescriptor openWriteInternal(String name, long offsetBytes, long lengthBytes) throws IOException {
        FileBridge bridge;
        synchronized (this.mLock) {
            assertPreparedAndNotSealed("openWrite");
            bridge = new FileBridge();
            this.mBridges.add(bridge);
        }
        try {
            if (!FileUtils.isValidExtFilename(name)) {
                throw new IllegalArgumentException("Invalid name: " + name);
            }
            File target = new File(resolveStageDir(), name);
            FileDescriptor targetFd = Libcore.os.open(target.getAbsolutePath(), OsConstants.O_CREAT | OsConstants.O_WRONLY, 420);
            Os.chmod(target.getAbsolutePath(), 420);
            if (lengthBytes > 0) {
                StructStat stat = Libcore.os.fstat(targetFd);
                long deltaBytes = lengthBytes - stat.st_size;
                if (this.stageDir != null && deltaBytes > 0) {
                    this.mPm.freeStorage(deltaBytes);
                }
                Libcore.os.posix_fallocate(targetFd, 0L, lengthBytes);
            }
            if (offsetBytes > 0) {
                Libcore.os.lseek(targetFd, offsetBytes, OsConstants.SEEK_SET);
            }
            bridge.setTargetFile(targetFd);
            bridge.start();
            return new ParcelFileDescriptor(bridge.getClientSocket());
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public ParcelFileDescriptor openRead(String name) {
        try {
            return openReadInternal(name);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private ParcelFileDescriptor openReadInternal(String name) throws IOException {
        assertPreparedAndNotSealed("openRead");
        try {
            if (!FileUtils.isValidExtFilename(name)) {
                throw new IllegalArgumentException("Invalid name: " + name);
            }
            File target = new File(resolveStageDir(), name);
            FileDescriptor targetFd = Libcore.os.open(target.getAbsolutePath(), OsConstants.O_RDONLY, 0);
            return new ParcelFileDescriptor(targetFd);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public void commit(IntentSender statusReceiver) {
        boolean wasSealed;
        Preconditions.checkNotNull(statusReceiver);
        synchronized (this.mLock) {
            wasSealed = this.mSealed;
            if (!this.mSealed) {
                for (FileBridge bridge : this.mBridges) {
                    if (!bridge.isClosed()) {
                        throw new SecurityException("Files still open");
                    }
                }
                this.mSealed = LOGD;
            }
            this.mClientProgress = 1.0f;
            computeProgressLocked(LOGD);
        }
        if (!wasSealed) {
            this.mCallback.onSessionSealedBlocking(this);
        }
        this.mActiveCount.incrementAndGet();
        PackageInstallerService.PackageInstallObserverAdapter adapter = new PackageInstallerService.PackageInstallObserverAdapter(this.mContext, statusReceiver, this.sessionId);
        this.mHandler.obtainMessage(0, adapter.getBinder()).sendToTarget();
    }

    private void commitLocked() throws PackageManagerException {
        UserHandle user;
        if (this.mDestroyed) {
            throw new PackageManagerException(-110, "Session destroyed");
        }
        if (!this.mSealed) {
            throw new PackageManagerException(-110, "Session not sealed");
        }
        try {
            resolveStageDir();
            validateInstallLocked();
            Preconditions.checkNotNull(this.mPackageName);
            Preconditions.checkNotNull(this.mSignatures);
            Preconditions.checkNotNull(this.mResolvedBaseFile);
            if (!this.mPermissionsAccepted) {
                Intent intent = new Intent("android.content.pm.action.CONFIRM_PERMISSIONS");
                intent.setPackage("com.android.packageinstaller");
                intent.putExtra("android.content.pm.extra.SESSION_ID", this.sessionId);
                try {
                    this.mRemoteObserver.onUserActionRequired(intent);
                } catch (RemoteException e) {
                }
                close();
                return;
            }
            if (this.stageCid != null) {
                long finalSize = calculateInstalledSize();
                resizeContainer(this.stageCid, finalSize);
            }
            if (this.params.mode == 2) {
                try {
                    List<File> fromFiles = this.mResolvedInheritedFiles;
                    File toDir = resolveStageDir();
                    if (isLinkPossible(fromFiles, toDir)) {
                        linkFiles(fromFiles, toDir);
                    } else {
                        copyFiles(fromFiles, toDir);
                    }
                } catch (IOException e2) {
                    throw new PackageManagerException(-4, "Failed to inherit existing install", e2);
                }
            }
            this.mInternalProgress = 0.5f;
            computeProgressLocked(LOGD);
            extractNativeLibraries(this.mResolvedStageDir, this.params.abiOverride);
            if (this.stageCid != null) {
                finalizeAndFixContainer(this.stageCid);
            }
            IPackageInstallObserver2 localObserver = new IPackageInstallObserver2.Stub() {
                public void onUserActionRequired(Intent intent2) {
                    throw new IllegalStateException();
                }

                public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) {
                    PackageInstallerSession.this.destroyInternal();
                    PackageInstallerSession.this.dispatchSessionFinished(returnCode, msg, extras);
                }
            };
            if ((this.params.installFlags & 64) != 0) {
                user = UserHandle.ALL;
            } else {
                user = new UserHandle(this.userId);
            }
            this.mPm.installStage(this.mPackageName, this.stageDir, this.stageCid, localObserver, this.params, this.installerPackageName, this.installerUid, user);
        } catch (IOException e3) {
            throw new PackageManagerException(-18, "Failed to resolve stage location", e3);
        }
    }

    private void validateInstallLocked() throws PackageManagerException {
        String targetName;
        this.mPackageName = null;
        this.mVersionCode = -1;
        this.mSignatures = null;
        this.mResolvedBaseFile = null;
        this.mResolvedStagedFiles.clear();
        this.mResolvedInheritedFiles.clear();
        File[] files = this.mResolvedStageDir.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            throw new PackageManagerException(-2, "No packages staged");
        }
        ArraySet<String> stagedSplits = new ArraySet<>();
        for (File file : files) {
            if (!file.isDirectory()) {
                try {
                    PackageParser.ApkLite apk = PackageParser.parseApkLite(file, PackageManagerService.DumpState.DUMP_VERIFIERS);
                    if (!stagedSplits.add(apk.splitName)) {
                        throw new PackageManagerException(-2, "Split " + apk.splitName + " was defined multiple times");
                    }
                    if (this.mPackageName == null) {
                        this.mPackageName = apk.packageName;
                        this.mVersionCode = apk.versionCode;
                    }
                    if (this.mSignatures == null) {
                        this.mSignatures = apk.signatures;
                    }
                    assertApkConsistent(String.valueOf(file), apk);
                    if (apk.splitName == null) {
                        targetName = "base.apk";
                    } else {
                        targetName = "split_" + apk.splitName + ".apk";
                    }
                    if (!FileUtils.isValidExtFilename(targetName)) {
                        throw new PackageManagerException(-2, "Invalid filename: " + targetName);
                    }
                    File targetFile = new File(this.mResolvedStageDir, targetName);
                    if (!file.equals(targetFile)) {
                        file.renameTo(targetFile);
                    }
                    if (apk.splitName == null) {
                        this.mResolvedBaseFile = targetFile;
                    }
                    this.mResolvedStagedFiles.add(targetFile);
                } catch (PackageParser.PackageParserException e) {
                    throw PackageManagerException.from(e);
                }
            }
        }
        if (this.params.mode == 1) {
            if (!stagedSplits.contains(null)) {
                throw new PackageManagerException(-2, "Full install must include a base package");
            }
            return;
        }
        ApplicationInfo app = this.mPm.getApplicationInfo(this.mPackageName, 0, this.userId);
        if (app == null) {
            throw new PackageManagerException(-2, "Missing existing base package for " + this.mPackageName);
        }
        try {
            PackageParser.PackageLite existing = PackageParser.parsePackageLite(new File(app.getCodePath()), 0);
            PackageParser.ApkLite existingBase = PackageParser.parseApkLite(new File(app.getBaseCodePath()), PackageManagerService.DumpState.DUMP_VERIFIERS);
            assertApkConsistent("Existing base", existingBase);
            if (this.mResolvedBaseFile == null) {
                this.mResolvedBaseFile = new File(app.getBaseCodePath());
                this.mResolvedInheritedFiles.add(this.mResolvedBaseFile);
            }
            if (!ArrayUtils.isEmpty(existing.splitNames)) {
                for (int i = 0; i < existing.splitNames.length; i++) {
                    String splitName = existing.splitNames[i];
                    File splitFile = new File(existing.splitCodePaths[i]);
                    if (!stagedSplits.contains(splitName)) {
                        this.mResolvedInheritedFiles.add(splitFile);
                    }
                }
            }
        } catch (PackageParser.PackageParserException e2) {
            throw PackageManagerException.from(e2);
        }
    }

    private void assertApkConsistent(String tag, PackageParser.ApkLite apk) throws PackageManagerException {
        if (!this.mPackageName.equals(apk.packageName)) {
            throw new PackageManagerException(-2, tag + " package " + apk.packageName + " inconsistent with " + this.mPackageName);
        }
        if (this.mVersionCode != apk.versionCode) {
            throw new PackageManagerException(-2, tag + " version code " + apk.versionCode + " inconsistent with " + this.mVersionCode);
        }
        if (!Signature.areExactMatch(this.mSignatures, apk.signatures)) {
            throw new PackageManagerException(-2, tag + " signatures are inconsistent");
        }
    }

    private long calculateInstalledSize() throws PackageManagerException {
        Preconditions.checkNotNull(this.mResolvedBaseFile);
        try {
            PackageParser.ApkLite baseApk = PackageParser.parseApkLite(this.mResolvedBaseFile, 0);
            List<String> splitPaths = new ArrayList<>();
            for (File file : this.mResolvedStagedFiles) {
                if (!this.mResolvedBaseFile.equals(file)) {
                    splitPaths.add(file.getAbsolutePath());
                }
            }
            for (File file2 : this.mResolvedInheritedFiles) {
                if (!this.mResolvedBaseFile.equals(file2)) {
                    splitPaths.add(file2.getAbsolutePath());
                }
            }
            PackageParser.PackageLite pkg = new PackageParser.PackageLite((String) null, baseApk, (String[]) null, (String[]) splitPaths.toArray(new String[splitPaths.size()]), (int[]) null);
            boolean isForwardLocked = (this.params.installFlags & 1) != 0 ? LOGD : false;
            try {
                return PackageHelper.calculateInstalledSize(pkg, isForwardLocked, this.params.abiOverride);
            } catch (IOException e) {
                throw new PackageManagerException(-2, "Failed to calculate install size", e);
            }
        } catch (PackageParser.PackageParserException e2) {
            throw PackageManagerException.from(e2);
        }
    }

    private boolean isLinkPossible(List<File> fromFiles, File toDir) {
        try {
            StructStat toStat = Os.stat(toDir.getAbsolutePath());
            for (File fromFile : fromFiles) {
                StructStat fromStat = Os.stat(fromFile.getAbsolutePath());
                if (fromStat.st_dev != toStat.st_dev) {
                    return false;
                }
            }
            return LOGD;
        } catch (ErrnoException e) {
            Slog.w(TAG, "Failed to detect if linking possible: " + e);
            return false;
        }
    }

    private static void linkFiles(List<File> fromFiles, File toDir) throws IOException {
        for (File fromFile : fromFiles) {
            File toFile = new File(toDir, fromFile.getName());
            try {
                Slog.d(TAG, "Linking " + fromFile + " to " + toFile);
                Os.link(fromFile.getAbsolutePath(), toFile.getAbsolutePath());
            } catch (ErrnoException e) {
                throw new IOException("Failed to link " + fromFile + " to " + toFile, e);
            }
        }
        Slog.d(TAG, "Linked " + fromFiles.size() + " files into " + toDir);
    }

    private static void copyFiles(List<File> fromFiles, File toDir) throws IOException {
        File[] arr$ = toDir.listFiles();
        for (File file : arr$) {
            if (file.getName().endsWith(".tmp")) {
                file.delete();
            }
        }
        for (File fromFile : fromFiles) {
            File tmpFile = File.createTempFile("inherit", ".tmp", toDir);
            Slog.d(TAG, "Copying " + fromFile + " to " + tmpFile);
            if (!FileUtils.copyFile(fromFile, tmpFile)) {
                throw new IOException("Failed to copy " + fromFile + " to " + tmpFile);
            }
            try {
                Os.chmod(tmpFile.getAbsolutePath(), 420);
                File toFile = new File(toDir, fromFile.getName());
                Slog.d(TAG, "Renaming " + tmpFile + " to " + toFile);
                if (!tmpFile.renameTo(toFile)) {
                    throw new IOException("Failed to rename " + tmpFile + " to " + toFile);
                }
            } catch (ErrnoException e) {
                throw new IOException("Failed to chmod " + tmpFile);
            }
        }
        Slog.d(TAG, "Copied " + fromFiles.size() + " files into " + toDir);
    }

    private static void extractNativeLibraries(File packageDir, String abiOverride) throws PackageManagerException {
        File libDir = new File(packageDir, "lib");
        NativeLibraryHelper.removeNativeBinariesFromDirLI(libDir, LOGD);
        NativeLibraryHelper.Handle handle = null;
        try {
            try {
                handle = NativeLibraryHelper.Handle.create(packageDir);
                int res = NativeLibraryHelper.copyNativeBinariesWithOverride(handle, libDir, abiOverride);
                if (res != 1) {
                    throw new PackageManagerException(res, "Failed to extract native libraries, res=" + res);
                }
            } catch (IOException e) {
                throw new PackageManagerException(-110, "Failed to extract native libraries", e);
            }
        } finally {
            IoUtils.closeQuietly(handle);
        }
    }

    private static void resizeContainer(String cid, long targetSize) throws PackageManagerException {
        String path = PackageHelper.getSdDir(cid);
        if (path == null) {
            throw new PackageManagerException(-18, "Failed to find mounted " + cid);
        }
        long currentSize = new File(path).getTotalSpace();
        if (currentSize > targetSize) {
            Slog.w(TAG, "Current size " + currentSize + " is larger than target size " + targetSize + "; skipping resize");
        } else {
            if (!PackageHelper.unMountSdDir(cid)) {
                throw new PackageManagerException(-18, "Failed to unmount " + cid + " before resize");
            }
            if (!PackageHelper.resizeSdDir(targetSize, cid, PackageManagerService.getEncryptKey())) {
                throw new PackageManagerException(-18, "Failed to resize " + cid + " to " + targetSize + " bytes");
            }
            if (PackageHelper.mountSdDir(cid, PackageManagerService.getEncryptKey(), 1000, false) == null) {
                throw new PackageManagerException(-18, "Failed to mount " + cid + " after resize");
            }
        }
    }

    private void finalizeAndFixContainer(String cid) throws PackageManagerException {
        if (!PackageHelper.finalizeSdDir(cid)) {
            throw new PackageManagerException(-18, "Failed to finalize container " + cid);
        }
        int uid = this.mPm.getPackageUid("com.android.defcontainer", 0);
        int gid = UserHandle.getSharedAppGid(uid);
        if (!PackageHelper.fixSdPermissions(cid, gid, (String) null)) {
            throw new PackageManagerException(-18, "Failed to fix permissions on container " + cid);
        }
    }

    void setPermissionsResult(boolean accepted) {
        if (!this.mSealed) {
            throw new SecurityException("Must be sealed to accept permissions");
        }
        if (accepted) {
            this.mPermissionsAccepted = LOGD;
            this.mHandler.obtainMessage(0).sendToTarget();
        } else {
            destroyInternal();
            dispatchSessionFinished(-115, "User rejected permissions", null);
        }
    }

    public void open() throws IOException {
        if (this.mActiveCount.getAndIncrement() == 0) {
            this.mCallback.onSessionActiveChanged(this, LOGD);
        }
        synchronized (this.mLock) {
            if (!this.mPrepared) {
                if (this.stageDir != null) {
                    PackageInstallerService.prepareInternalStageDir(this.stageDir);
                } else if (this.stageCid != null) {
                    PackageInstallerService.prepareExternalStageCid(this.stageCid, this.params.sizeBytes);
                    this.mInternalProgress = 0.25f;
                    computeProgressLocked(LOGD);
                } else {
                    throw new IllegalArgumentException("Exactly one of stageDir or stageCid stage must be set");
                }
                this.mPrepared = LOGD;
                this.mCallback.onSessionPrepared(this);
            }
        }
    }

    public void close() {
        if (this.mActiveCount.decrementAndGet() == 0) {
            this.mCallback.onSessionActiveChanged(this, false);
        }
    }

    public void abandon() {
        destroyInternal();
        dispatchSessionFinished(-115, "Session was abandoned", null);
    }

    private void dispatchSessionFinished(int returnCode, String msg, Bundle extras) {
        boolean success = LOGD;
        this.mFinalStatus = returnCode;
        this.mFinalMessage = msg;
        if (this.mRemoteObserver != null) {
            try {
                this.mRemoteObserver.onPackageInstalled(this.mPackageName, returnCode, msg, extras);
            } catch (RemoteException e) {
            }
        }
        if (returnCode != 1) {
            success = false;
        }
        this.mCallback.onSessionFinished(this, success);
    }

    private void destroyInternal() {
        synchronized (this.mLock) {
            this.mSealed = LOGD;
            this.mDestroyed = LOGD;
            for (FileBridge bridge : this.mBridges) {
                bridge.forceClose();
            }
        }
        if (this.stageDir != null) {
            FileUtils.deleteContents(this.stageDir);
            this.stageDir.delete();
        }
        if (this.stageCid != null) {
            PackageHelper.destroySdDir(this.stageCid);
        }
    }

    void dump(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            dumpLocked(pw);
        }
    }

    private void dumpLocked(IndentingPrintWriter pw) {
        pw.println("Session " + this.sessionId + ":");
        pw.increaseIndent();
        pw.printPair("userId", Integer.valueOf(this.userId));
        pw.printPair("installerPackageName", this.installerPackageName);
        pw.printPair("installerUid", Integer.valueOf(this.installerUid));
        pw.printPair("createdMillis", Long.valueOf(this.createdMillis));
        pw.printPair("stageDir", this.stageDir);
        pw.printPair("stageCid", this.stageCid);
        pw.println();
        this.params.dump(pw);
        pw.printPair("mClientProgress", Float.valueOf(this.mClientProgress));
        pw.printPair("mProgress", Float.valueOf(this.mProgress));
        pw.printPair("mSealed", Boolean.valueOf(this.mSealed));
        pw.printPair("mPermissionsAccepted", Boolean.valueOf(this.mPermissionsAccepted));
        pw.printPair("mDestroyed", Boolean.valueOf(this.mDestroyed));
        pw.printPair("mBridges", Integer.valueOf(this.mBridges.size()));
        pw.printPair("mFinalStatus", Integer.valueOf(this.mFinalStatus));
        pw.printPair("mFinalMessage", this.mFinalMessage);
        pw.println();
        pw.decreaseIndent();
    }
}
