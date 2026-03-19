package com.android.server.pm;

import android.app.admin.DevicePolicyManagerInternal;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.dex.DexMetadataHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.FileBridge;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.RemoteException;
import android.os.RevocableFileDescriptor;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.Int64Ref;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.ExceptionUtils;
import android.util.MathUtils;
import android.util.Slog;
import android.util.apk.ApkSignatureVerifier;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.content.PackageHelper;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageInstallerService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PackageInstallerSession extends IPackageInstallerSession.Stub {
    private static final String ATTR_ABI_OVERRIDE = "abiOverride";

    @Deprecated
    private static final String ATTR_APP_ICON = "appIcon";
    private static final String ATTR_APP_LABEL = "appLabel";
    private static final String ATTR_APP_PACKAGE_NAME = "appPackageName";
    private static final String ATTR_CREATED_MILLIS = "createdMillis";
    private static final String ATTR_INSTALLER_PACKAGE_NAME = "installerPackageName";
    private static final String ATTR_INSTALLER_UID = "installerUid";
    private static final String ATTR_INSTALL_FLAGS = "installFlags";
    private static final String ATTR_INSTALL_LOCATION = "installLocation";
    private static final String ATTR_INSTALL_REASON = "installRason";
    private static final String ATTR_MODE = "mode";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_ORIGINATING_UID = "originatingUid";
    private static final String ATTR_ORIGINATING_URI = "originatingUri";
    private static final String ATTR_PREPARED = "prepared";
    private static final String ATTR_REFERRER_URI = "referrerUri";
    private static final String ATTR_SEALED = "sealed";
    private static final String ATTR_SESSION_ID = "sessionId";
    private static final String ATTR_SESSION_STAGE_CID = "sessionStageCid";
    private static final String ATTR_SESSION_STAGE_DIR = "sessionStageDir";
    private static final String ATTR_SIZE_BYTES = "sizeBytes";
    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_VOLUME_UUID = "volumeUuid";
    private static final boolean LOGD = true;
    private static final int MSG_COMMIT = 1;
    private static final int MSG_EARLY_BIND = 0;
    private static final int MSG_ON_PACKAGE_INSTALLED = 2;
    private static final String PROPERTY_NAME_INHERIT_NATIVE = "pi.inherit_native_on_dont_kill";
    private static final String REMOVE_SPLIT_MARKER_EXTENSION = ".removed";
    private static final String TAG = "PackageInstaller";
    private static final String TAG_GRANTED_RUNTIME_PERMISSION = "granted-runtime-permission";
    static final String TAG_SESSION = "session";
    private static final FileFilter sAddedFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return (file.isDirectory() || file.getName().endsWith(PackageInstallerSession.REMOVE_SPLIT_MARKER_EXTENSION) || DexMetadataHelper.isDexMetadataFile(file)) ? false : true;
        }
    };
    private static final FileFilter sRemovedFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return !file.isDirectory() && file.getName().endsWith(PackageInstallerSession.REMOVE_SPLIT_MARKER_EXTENSION);
        }
    };
    final long createdMillis;
    final int defaultContainerGid;
    private final PackageInstallerService.InternalCallback mCallback;
    private final Context mContext;

    @GuardedBy("mLock")
    private String mFinalMessage;

    @GuardedBy("mLock")
    private int mFinalStatus;
    private final Handler mHandler;

    @GuardedBy("mLock")
    private File mInheritedFilesBase;

    @GuardedBy("mLock")
    private String mInstallerPackageName;

    @GuardedBy("mLock")
    private int mInstallerUid;
    private final int mOriginalInstallerUid;

    @GuardedBy("mLock")
    private String mPackageName;
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
    private PackageParser.SigningDetails mSigningDetails;

    @GuardedBy("mLock")
    private long mVersionCode;
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
    private boolean mSealed = false;

    @GuardedBy("mLock")
    private boolean mCommitted = false;

    @GuardedBy("mLock")
    private boolean mRelinquished = false;

    @GuardedBy("mLock")
    private boolean mDestroyed = false;

    @GuardedBy("mLock")
    private boolean mPermissionsManuallyAccepted = false;

    @GuardedBy("mLock")
    private final ArrayList<RevocableFileDescriptor> mFds = new ArrayList<>();

    @GuardedBy("mLock")
    private final ArrayList<FileBridge> mBridges = new ArrayList<>();

    @GuardedBy("mLock")
    private final List<File> mResolvedStagedFiles = new ArrayList();

    @GuardedBy("mLock")
    private final List<File> mResolvedInheritedFiles = new ArrayList();

    @GuardedBy("mLock")
    private final List<String> mResolvedInstructionSets = new ArrayList();

    @GuardedBy("mLock")
    private final List<String> mResolvedNativeLibPaths = new ArrayList();
    private final Handler.Callback mHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    PackageInstallerSession.this.earlyBindToDefContainer();
                    return true;
                case 1:
                    synchronized (PackageInstallerSession.this.mLock) {
                        try {
                            PackageInstallerSession.this.commitLocked();
                        } catch (PackageManagerException e) {
                            String completeMessage = ExceptionUtils.getCompleteMessage(e);
                            Slog.e(PackageInstallerSession.TAG, "Commit of session " + PackageInstallerSession.this.sessionId + " failed: " + completeMessage);
                            PackageInstallerSession.this.destroyInternal();
                            PackageInstallerSession.this.dispatchSessionFinished(e.error, completeMessage, null);
                        }
                        break;
                    }
                    return true;
                case 2:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    String str = (String) someArgs.arg1;
                    String str2 = (String) someArgs.arg2;
                    Bundle bundle = (Bundle) someArgs.arg3;
                    IPackageInstallObserver2 iPackageInstallObserver2 = (IPackageInstallObserver2) someArgs.arg4;
                    int i = someArgs.argi1;
                    someArgs.recycle();
                    try {
                        iPackageInstallObserver2.onPackageInstalled(str, i, str2, bundle);
                        return true;
                    } catch (RemoteException e2) {
                        return true;
                    }
                default:
                    return true;
            }
        }
    };

    private void earlyBindToDefContainer() {
        this.mPm.earlyBindToDefContainer();
    }

    @GuardedBy("mLock")
    private boolean isInstallerDeviceOwnerOrAffiliatedProfileOwnerLocked() {
        DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        return devicePolicyManagerInternal != null && devicePolicyManagerInternal.isActiveAdminWithPolicy(this.mInstallerUid, -1) && devicePolicyManagerInternal.isUserAffiliatedWithDevice(this.userId);
    }

    @GuardedBy("mLock")
    private boolean needToAskForPermissionsLocked() {
        if (this.mPermissionsManuallyAccepted) {
            return false;
        }
        boolean z = this.mPm.checkUidPermission("android.permission.INSTALL_PACKAGES", this.mInstallerUid) == 0;
        boolean z2 = this.mPm.checkUidPermission("android.permission.INSTALL_SELF_UPDATES", this.mInstallerUid) == 0;
        boolean z3 = this.mPm.checkUidPermission("android.permission.INSTALL_PACKAGE_UPDATES", this.mInstallerUid) == 0;
        int packageUid = this.mPm.getPackageUid(this.mPackageName, 0, this.userId);
        return ((this.params.installFlags & 1024) != 0) || !((z || ((z3 && packageUid != -1) || (z2 && packageUid == this.mInstallerUid))) || (this.mInstallerUid == 0) || (this.mInstallerUid == 1000) || isInstallerDeviceOwnerOrAffiliatedProfileOwnerLocked());
    }

    public PackageInstallerSession(PackageInstallerService.InternalCallback internalCallback, Context context, PackageManagerService packageManagerService, Looper looper, int i, int i2, String str, int i3, PackageInstaller.SessionParams sessionParams, long j, File file, String str2, boolean z, boolean z2) {
        boolean z3;
        this.mPrepared = false;
        this.mCallback = internalCallback;
        this.mContext = context;
        this.mPm = packageManagerService;
        this.mHandler = new Handler(looper, this.mHandlerCallback);
        this.sessionId = i;
        this.userId = i2;
        this.mOriginalInstallerUid = i3;
        this.mInstallerPackageName = str;
        this.mInstallerUid = i3;
        this.params = sessionParams;
        this.createdMillis = j;
        this.stageDir = file;
        this.stageCid = str2;
        if (file != null) {
            z3 = false;
        } else {
            z3 = true;
        }
        if (z3 == (str2 == null)) {
            throw new IllegalArgumentException("Exactly one of stageDir or stageCid stage must be set");
        }
        this.mPrepared = z;
        if (z2) {
            synchronized (this.mLock) {
                try {
                    try {
                        sealAndValidateLocked();
                    } catch (PackageManagerException | IOException e) {
                        destroyInternal();
                        throw new IllegalArgumentException(e);
                    }
                } finally {
                }
            }
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.defaultContainerGid = UserHandle.getSharedAppGid(this.mPm.getPackageUid(PackageManagerService.DEFAULT_CONTAINER_PACKAGE, DumpState.DUMP_DEXOPT, 0));
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            if ((sessionParams.installFlags & 2048) != 0) {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(0));
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private boolean shouldScrubData(int i) {
        return i >= 10000 && getInstallerUid() != i;
    }

    public PackageInstaller.SessionInfo generateInfoForCaller(boolean z, int i) {
        return generateInfoInternal(z, shouldScrubData(i));
    }

    public PackageInstaller.SessionInfo generateInfoScrubbed(boolean z) {
        return generateInfoInternal(z, true);
    }

    private PackageInstaller.SessionInfo generateInfoInternal(boolean z, boolean z2) {
        PackageInstaller.SessionInfo sessionInfo = new PackageInstaller.SessionInfo();
        synchronized (this.mLock) {
            sessionInfo.sessionId = this.sessionId;
            sessionInfo.installerPackageName = this.mInstallerPackageName;
            sessionInfo.resolvedBaseCodePath = this.mResolvedBaseFile != null ? this.mResolvedBaseFile.getAbsolutePath() : null;
            sessionInfo.progress = this.mProgress;
            sessionInfo.sealed = this.mSealed;
            sessionInfo.active = this.mActiveCount.get() > 0;
            sessionInfo.mode = this.params.mode;
            sessionInfo.installReason = this.params.installReason;
            sessionInfo.sizeBytes = this.params.sizeBytes;
            sessionInfo.appPackageName = this.params.appPackageName;
            if (z) {
                sessionInfo.appIcon = this.params.appIcon;
            }
            sessionInfo.appLabel = this.params.appLabel;
            sessionInfo.installLocation = this.params.installLocation;
            if (!z2) {
                sessionInfo.originatingUri = this.params.originatingUri;
            }
            sessionInfo.originatingUid = this.params.originatingUid;
            if (!z2) {
                sessionInfo.referrerUri = this.params.referrerUri;
            }
            sessionInfo.grantedRuntimePermissions = this.params.grantedRuntimePermissions;
            sessionInfo.installFlags = this.params.installFlags;
        }
        return sessionInfo;
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

    @GuardedBy("mLock")
    private void assertPreparedAndNotSealedLocked(String str) {
        assertPreparedAndNotCommittedOrDestroyedLocked(str);
        if (this.mSealed) {
            throw new SecurityException(str + " not allowed after sealing");
        }
    }

    @GuardedBy("mLock")
    private void assertPreparedAndNotCommittedOrDestroyedLocked(String str) {
        assertPreparedAndNotDestroyedLocked(str);
        if (this.mCommitted) {
            throw new SecurityException(str + " not allowed after commit");
        }
    }

    @GuardedBy("mLock")
    private void assertPreparedAndNotDestroyedLocked(String str) {
        if (!this.mPrepared) {
            throw new IllegalStateException(str + " before prepared");
        }
        if (this.mDestroyed) {
            throw new SecurityException(str + " not allowed after destruction");
        }
    }

    @GuardedBy("mLock")
    private File resolveStageDirLocked() throws IOException {
        if (this.mResolvedStageDir == null) {
            if (this.stageDir != null) {
                this.mResolvedStageDir = this.stageDir;
            } else {
                throw new IOException("Missing stageDir");
            }
        }
        return this.mResolvedStageDir;
    }

    public void setClientProgress(float f) {
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            boolean z = this.mClientProgress == 0.0f;
            this.mClientProgress = f;
            computeProgressLocked(z);
        }
    }

    public void addClientProgress(float f) {
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            setClientProgress(this.mClientProgress + f);
        }
    }

    @GuardedBy("mLock")
    private void computeProgressLocked(boolean z) {
        this.mProgress = MathUtils.constrain(this.mClientProgress * 0.8f, 0.0f, 0.8f) + MathUtils.constrain(this.mInternalProgress * 0.2f, 0.0f, 0.2f);
        if (z || Math.abs(this.mProgress - this.mReportedProgress) >= 0.01d) {
            this.mReportedProgress = this.mProgress;
            this.mCallback.onSessionProgressChanged(this, this.mProgress);
        }
    }

    public String[] getNames() {
        String[] list;
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotCommittedOrDestroyedLocked("getNames");
            try {
                list = resolveStageDirLocked().list();
            } catch (IOException e) {
                throw ExceptionUtils.wrap(e);
            }
        }
        return list;
    }

    public void removeSplit(String str) {
        if (TextUtils.isEmpty(this.params.appPackageName)) {
            throw new IllegalStateException("Must specify package name to remove a split");
        }
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotCommittedOrDestroyedLocked("removeSplit");
            try {
                createRemoveSplitMarkerLocked(str);
            } catch (IOException e) {
                throw ExceptionUtils.wrap(e);
            }
        }
    }

    private void createRemoveSplitMarkerLocked(String str) throws IOException {
        try {
            String str2 = str + REMOVE_SPLIT_MARKER_EXTENSION;
            if (!FileUtils.isValidExtFilename(str2)) {
                throw new IllegalArgumentException("Invalid marker: " + str2);
            }
            File file = new File(resolveStageDirLocked(), str2);
            file.createNewFile();
            Os.chmod(file.getAbsolutePath(), 0);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public ParcelFileDescriptor openWrite(String str, long j, long j2) {
        try {
            return doWriteInternal(str, j, j2, null);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    public void write(String str, long j, long j2, ParcelFileDescriptor parcelFileDescriptor) {
        try {
            doWriteInternal(str, j, j2, parcelFileDescriptor);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private ParcelFileDescriptor doWriteInternal(String str, long j, long j2, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        FileBridge fileBridge;
        RevocableFileDescriptor revocableFileDescriptor;
        File fileResolveStageDirLocked;
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotSealedLocked("openWrite");
            if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                RevocableFileDescriptor revocableFileDescriptor2 = new RevocableFileDescriptor();
                this.mFds.add(revocableFileDescriptor2);
                revocableFileDescriptor = revocableFileDescriptor2;
                fileBridge = null;
            } else {
                FileBridge fileBridge2 = new FileBridge();
                this.mBridges.add(fileBridge2);
                fileBridge = fileBridge2;
                revocableFileDescriptor = null;
            }
            fileResolveStageDirLocked = resolveStageDirLocked();
        }
        try {
            if (!FileUtils.isValidExtFilename(str)) {
                throw new IllegalArgumentException("Invalid name: " + str);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                File file = new File(fileResolveStageDirLocked, str);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                FileDescriptor fileDescriptorOpen = Os.open(file.getAbsolutePath(), OsConstants.O_CREAT | OsConstants.O_WRONLY, 420);
                Os.chmod(file.getAbsolutePath(), 420);
                if (fileResolveStageDirLocked != null && j2 > 0) {
                    ((StorageManager) this.mContext.getSystemService(StorageManager.class)).allocateBytes(fileDescriptorOpen, j2, PackageHelper.translateAllocateFlags(this.params.installFlags));
                }
                if (j > 0) {
                    Os.lseek(fileDescriptorOpen, j, OsConstants.SEEK_SET);
                }
                if (parcelFileDescriptor != null) {
                    int callingUid = Binder.getCallingUid();
                    if (callingUid != 0 && callingUid != 2000) {
                        throw new SecurityException("Reverse mode only supported from shell");
                    }
                    try {
                        final Int64Ref int64Ref = new Int64Ref(0L);
                        FileUtils.copy(parcelFileDescriptor.getFileDescriptor(), fileDescriptorOpen, new FileUtils.ProgressListener() {
                            @Override
                            public final void onProgress(long j3) {
                                PackageInstallerSession.lambda$doWriteInternal$0(this.f$0, int64Ref, j3);
                            }
                        }, (CancellationSignal) null, j2);
                        IoUtils.closeQuietly(fileDescriptorOpen);
                        IoUtils.closeQuietly(parcelFileDescriptor);
                        synchronized (this.mLock) {
                            try {
                                if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                                    this.mFds.remove(revocableFileDescriptor);
                                } else {
                                    fileBridge.forceClose();
                                    this.mBridges.remove(fileBridge);
                                }
                            } catch (Throwable th) {
                                throw th;
                            }
                        }
                        return null;
                    } catch (Throwable th2) {
                        IoUtils.closeQuietly(fileDescriptorOpen);
                        IoUtils.closeQuietly(parcelFileDescriptor);
                        synchronized (this.mLock) {
                            if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                                this.mFds.remove(revocableFileDescriptor);
                            } else {
                                fileBridge.forceClose();
                                this.mBridges.remove(fileBridge);
                            }
                            throw th2;
                        }
                    }
                }
                if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                    revocableFileDescriptor.init(this.mContext, fileDescriptorOpen);
                    return revocableFileDescriptor.getRevocableFileDescriptor();
                }
                fileBridge.setTargetFile(fileDescriptorOpen);
                fileBridge.start();
                return new ParcelFileDescriptor(fileBridge.getClientSocket());
            } catch (Throwable th3) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th3;
            }
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public static void lambda$doWriteInternal$0(PackageInstallerSession packageInstallerSession, Int64Ref int64Ref, long j) {
        if (packageInstallerSession.params.sizeBytes > 0) {
            long j2 = j - int64Ref.value;
            int64Ref.value = j;
            packageInstallerSession.addClientProgress(j2 / packageInstallerSession.params.sizeBytes);
        }
    }

    public ParcelFileDescriptor openRead(String str) {
        ParcelFileDescriptor parcelFileDescriptorOpenReadInternalLocked;
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotCommittedOrDestroyedLocked("openRead");
            try {
                parcelFileDescriptorOpenReadInternalLocked = openReadInternalLocked(str);
            } catch (IOException e) {
                throw ExceptionUtils.wrap(e);
            }
        }
        return parcelFileDescriptorOpenReadInternalLocked;
    }

    private ParcelFileDescriptor openReadInternalLocked(String str) throws IOException {
        try {
            if (!FileUtils.isValidExtFilename(str)) {
                throw new IllegalArgumentException("Invalid name: " + str);
            }
            return new ParcelFileDescriptor(Os.open(new File(resolveStageDirLocked(), str).getAbsolutePath(), OsConstants.O_RDONLY, 0));
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    @GuardedBy("mLock")
    private void assertCallerIsOwnerOrRootLocked() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != this.mInstallerUid) {
            throw new SecurityException("Session does not belong to uid " + callingUid);
        }
    }

    @GuardedBy("mLock")
    private void assertNoWriteFileTransfersOpenLocked() {
        Iterator<RevocableFileDescriptor> it = this.mFds.iterator();
        while (it.hasNext()) {
            if (!it.next().isRevoked()) {
                throw new SecurityException("Files still open");
            }
        }
        Iterator<FileBridge> it2 = this.mBridges.iterator();
        while (it2.hasNext()) {
            if (!it2.next().isClosed()) {
                throw new SecurityException("Files still open");
            }
        }
    }

    public void commit(IntentSender intentSender, boolean z) {
        boolean z2;
        Preconditions.checkNotNull(intentSender);
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotDestroyedLocked("commit");
            this.mRemoteObserver = new PackageInstallerService.PackageInstallObserverAdapter(this.mContext, intentSender, this.sessionId, isInstallerDeviceOwnerOrAffiliatedProfileOwnerLocked(), this.userId).getBinder();
            if (z) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
                if (this.mInstallerUid == this.mOriginalInstallerUid) {
                    throw new IllegalArgumentException("Session has not been transferred");
                }
            } else if (this.mInstallerUid != this.mOriginalInstallerUid) {
                throw new IllegalArgumentException("Session has been transferred");
            }
            z2 = this.mSealed;
            if (!this.mSealed) {
                try {
                    sealAndValidateLocked();
                } catch (PackageManagerException e) {
                    destroyInternal();
                    dispatchSessionFinished(e.error, ExceptionUtils.getCompleteMessage(e), null);
                    return;
                } catch (IOException e2) {
                    throw new IllegalArgumentException(e2);
                }
            }
            this.mClientProgress = 1.0f;
            computeProgressLocked(true);
            this.mActiveCount.incrementAndGet();
            this.mCommitted = true;
            this.mHandler.obtainMessage(1).sendToTarget();
        }
        if (!z2) {
            this.mCallback.onSessionSealedBlocking(this);
        }
    }

    @GuardedBy("mLock")
    private void sealAndValidateLocked() throws IOException, PackageManagerException {
        assertNoWriteFileTransfersOpenLocked();
        assertPreparedAndNotDestroyedLocked("sealing of session");
        PackageInfo packageInfo = this.mPm.getPackageInfo(this.params.appPackageName, 67108928, this.userId);
        resolveStageDirLocked();
        this.mSealed = true;
        try {
            validateInstallLocked(packageInfo);
        } catch (PackageManagerException e) {
            throw e;
        } catch (Throwable th) {
            throw new PackageManagerException(th);
        }
    }

    public void transfer(String str) throws ParcelableException {
        Preconditions.checkNotNull(str);
        ApplicationInfo applicationInfo = this.mPm.getApplicationInfo(str, 0, this.userId);
        if (applicationInfo == null) {
            throw new ParcelableException(new PackageManager.NameNotFoundException(str));
        }
        if (this.mPm.checkUidPermission("android.permission.INSTALL_PACKAGES", applicationInfo.uid) != 0) {
            throw new SecurityException("Destination package " + str + " does not have the android.permission.INSTALL_PACKAGES permission");
        }
        if (!this.params.areHiddenOptionsSet()) {
            throw new SecurityException("Can only transfer sessions that use public options");
        }
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotSealedLocked("transfer");
            try {
                sealAndValidateLocked();
                if (!this.mPackageName.equals(this.mInstallerPackageName)) {
                    throw new SecurityException("Can only transfer sessions that update the original installer");
                }
                this.mInstallerPackageName = str;
                this.mInstallerUid = applicationInfo.uid;
            } catch (PackageManagerException e) {
                destroyInternal();
                dispatchSessionFinished(e.error, ExceptionUtils.getCompleteMessage(e), null);
                throw new IllegalArgumentException("Package is not valid", e);
            } catch (IOException e2) {
                throw new IllegalStateException(e2);
            }
        }
        this.mCallback.onSessionSealedBlocking(this);
    }

    @GuardedBy("mLock")
    private void commitLocked() throws Throwable {
        UserHandle userHandle;
        if (this.mDestroyed) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session destroyed");
        }
        if (!this.mSealed) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session not sealed");
        }
        Preconditions.checkNotNull(this.mPackageName);
        Preconditions.checkNotNull(this.mSigningDetails);
        Preconditions.checkNotNull(this.mResolvedBaseFile);
        if (needToAskForPermissionsLocked()) {
            Intent intent = new Intent("android.content.pm.action.CONFIRM_PERMISSIONS");
            intent.setPackage(this.mContext.getPackageManager().getPermissionControllerPackageName());
            intent.putExtra("android.content.pm.extra.SESSION_ID", this.sessionId);
            try {
                this.mRemoteObserver.onUserActionRequired(intent);
            } catch (RemoteException e) {
            }
            closeInternal(false);
            return;
        }
        if (this.params.mode == 2) {
            try {
                List<File> list = this.mResolvedInheritedFiles;
                File fileResolveStageDirLocked = resolveStageDirLocked();
                Slog.d(TAG, "Inherited files: " + this.mResolvedInheritedFiles);
                if (!this.mResolvedInheritedFiles.isEmpty() && this.mInheritedFilesBase == null) {
                    throw new IllegalStateException("mInheritedFilesBase == null");
                }
                if (isLinkPossible(list, fileResolveStageDirLocked)) {
                    if (!this.mResolvedInstructionSets.isEmpty()) {
                        createOatDirs(this.mResolvedInstructionSets, new File(fileResolveStageDirLocked, "oat"));
                    }
                    if (!this.mResolvedNativeLibPaths.isEmpty()) {
                        for (String str : this.mResolvedNativeLibPaths) {
                            int iLastIndexOf = str.lastIndexOf(47);
                            if (iLastIndexOf < 0 || iLastIndexOf >= str.length() - 1) {
                                Slog.e(TAG, "Skipping native library creation for linking due to invalid path: " + str);
                            } else {
                                File file = new File(fileResolveStageDirLocked, str.substring(1, iLastIndexOf));
                                if (!file.exists()) {
                                    NativeLibraryHelper.createNativeLibrarySubdir(file);
                                }
                                NativeLibraryHelper.createNativeLibrarySubdir(new File(file, str.substring(iLastIndexOf + 1)));
                            }
                        }
                    }
                    linkFiles(list, fileResolveStageDirLocked, this.mInheritedFilesBase);
                } else {
                    copyFiles(list, fileResolveStageDirLocked);
                }
            } catch (IOException e2) {
                throw new PackageManagerException(-4, "Failed to inherit existing install", e2);
            }
        }
        this.mInternalProgress = 0.5f;
        computeProgressLocked(true);
        extractNativeLibraries(this.mResolvedStageDir, this.params.abiOverride, mayInheritNativeLibs());
        IPackageInstallObserver2 iPackageInstallObserver2 = new IPackageInstallObserver2.Stub() {
            public void onUserActionRequired(Intent intent2) {
                throw new IllegalStateException();
            }

            public void onPackageInstalled(String str2, int i, String str3, Bundle bundle) {
                PackageInstallerSession.this.destroyInternal();
                PackageInstallerSession.this.dispatchSessionFinished(i, str3, bundle);
            }
        };
        if ((this.params.installFlags & 64) != 0) {
            userHandle = UserHandle.ALL;
        } else {
            userHandle = new UserHandle(this.userId);
        }
        UserHandle userHandle2 = userHandle;
        this.mRelinquished = true;
        this.mPm.installStage(this.mPackageName, this.stageDir, iPackageInstallObserver2, this.params, this.mInstallerPackageName, this.mInstallerUid, userHandle2, this.mSigningDetails);
    }

    private static void maybeRenameFile(File file, File file2) throws PackageManagerException {
        if (!file.equals(file2) && !file.renameTo(file2)) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Could not rename file " + file + " to " + file2);
        }
    }

    private boolean mayInheritNativeLibs() {
        return SystemProperties.getBoolean(PROPERTY_NAME_INHERIT_NATIVE, true) && this.params.mode == 2 && (this.params.installFlags & 1) != 0;
    }

    @GuardedBy("mLock")
    private void validateInstallLocked(PackageInfo packageInfo) throws PackageManagerException {
        File[] fileArrListFiles;
        this.mPackageName = null;
        this.mVersionCode = -1L;
        this.mSigningDetails = PackageParser.SigningDetails.UNKNOWN;
        this.mResolvedBaseFile = null;
        this.mResolvedStagedFiles.clear();
        this.mResolvedInheritedFiles.clear();
        try {
            resolveStageDirLocked();
            File[] fileArrListFiles2 = this.mResolvedStageDir.listFiles(sRemovedFilter);
            ArrayList<String> arrayList = new ArrayList();
            if (!ArrayUtils.isEmpty(fileArrListFiles2)) {
                for (File file : fileArrListFiles2) {
                    String name = file.getName();
                    arrayList.add(name.substring(0, name.length() - REMOVE_SPLIT_MARKER_EXTENSION.length()));
                }
            }
            File[] fileArrListFiles3 = this.mResolvedStageDir.listFiles(sAddedFilter);
            if (ArrayUtils.isEmpty(fileArrListFiles3) && arrayList.size() == 0) {
                throw new PackageManagerException(-2, "No packages staged");
            }
            ArraySet arraySet = new ArraySet();
            for (File file2 : fileArrListFiles3) {
                try {
                    PackageParser.ApkLite apkLite = PackageParser.parseApkLite(file2, 32);
                    if (!arraySet.add(apkLite.splitName)) {
                        throw new PackageManagerException(-2, "Split " + apkLite.splitName + " was defined multiple times");
                    }
                    if (this.mPackageName == null) {
                        this.mPackageName = apkLite.packageName;
                        this.mVersionCode = apkLite.getLongVersionCode();
                    }
                    if (this.mSigningDetails == PackageParser.SigningDetails.UNKNOWN) {
                        this.mSigningDetails = apkLite.signingDetails;
                    }
                    assertApkConsistentLocked(String.valueOf(file2), apkLite);
                    String str = apkLite.splitName == null ? "base.apk" : "split_" + apkLite.splitName + ".apk";
                    if (!FileUtils.isValidExtFilename(str)) {
                        throw new PackageManagerException(-2, "Invalid filename: " + str);
                    }
                    File file3 = new File(this.mResolvedStageDir, str);
                    maybeRenameFile(file2, file3);
                    if (apkLite.splitName == null) {
                        this.mResolvedBaseFile = file3;
                    }
                    this.mResolvedStagedFiles.add(file3);
                    File fileFindDexMetadataForFile = DexMetadataHelper.findDexMetadataForFile(file2);
                    if (fileFindDexMetadataForFile != null) {
                        if (!FileUtils.isValidExtFilename(fileFindDexMetadataForFile.getName())) {
                            throw new PackageManagerException(-2, "Invalid filename: " + fileFindDexMetadataForFile);
                        }
                        File file4 = new File(this.mResolvedStageDir, DexMetadataHelper.buildDexMetadataPathForApk(str));
                        this.mResolvedStagedFiles.add(file4);
                        maybeRenameFile(fileFindDexMetadataForFile, file4);
                    }
                } catch (PackageParser.PackageParserException e) {
                    throw PackageManagerException.from(e);
                }
            }
            if (arrayList.size() > 0) {
                if (packageInfo == null) {
                    throw new PackageManagerException(-2, "Missing existing base package for " + this.mPackageName);
                }
                for (String str2 : arrayList) {
                    if (!ArrayUtils.contains(packageInfo.splitNames, str2)) {
                        throw new PackageManagerException(-2, "Split not found: " + str2);
                    }
                }
                if (this.mPackageName == null) {
                    this.mPackageName = packageInfo.packageName;
                    this.mVersionCode = packageInfo.getLongVersionCode();
                }
                if (this.mSigningDetails == PackageParser.SigningDetails.UNKNOWN) {
                    try {
                        this.mSigningDetails = ApkSignatureVerifier.plsCertsNoVerifyOnlyCerts(packageInfo.applicationInfo.sourceDir, 1);
                    } catch (PackageParser.PackageParserException e2) {
                        throw new PackageManagerException(-2, "Couldn't obtain signatures from base APK");
                    }
                }
            }
            if (this.params.mode == 1) {
                if (!arraySet.contains(null)) {
                    throw new PackageManagerException(-2, "Full install must include a base package");
                }
                return;
            }
            if (packageInfo == null || packageInfo.applicationInfo == null) {
                throw new PackageManagerException(-2, "Missing existing base package for " + this.mPackageName);
            }
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            try {
                PackageParser.PackageLite packageLite = PackageParser.parsePackageLite(new File(applicationInfo.getCodePath()), 0);
                assertApkConsistentLocked("Existing base", PackageParser.parseApkLite(new File(applicationInfo.getBaseCodePath()), 32));
                if (this.mResolvedBaseFile == null) {
                    this.mResolvedBaseFile = new File(applicationInfo.getBaseCodePath());
                    this.mResolvedInheritedFiles.add(this.mResolvedBaseFile);
                    File fileFindDexMetadataForFile2 = DexMetadataHelper.findDexMetadataForFile(this.mResolvedBaseFile);
                    if (fileFindDexMetadataForFile2 != null) {
                        this.mResolvedInheritedFiles.add(fileFindDexMetadataForFile2);
                    }
                }
                if (!ArrayUtils.isEmpty(packageLite.splitNames)) {
                    for (int i = 0; i < packageLite.splitNames.length; i++) {
                        String str3 = packageLite.splitNames[i];
                        File file5 = new File(packageLite.splitCodePaths[i]);
                        boolean zContains = arrayList.contains(str3);
                        if (!arraySet.contains(str3) && !zContains) {
                            this.mResolvedInheritedFiles.add(file5);
                            File fileFindDexMetadataForFile3 = DexMetadataHelper.findDexMetadataForFile(file5);
                            if (fileFindDexMetadataForFile3 != null) {
                                this.mResolvedInheritedFiles.add(fileFindDexMetadataForFile3);
                            }
                        }
                    }
                }
                File parentFile = new File(applicationInfo.getBaseCodePath()).getParentFile();
                this.mInheritedFilesBase = parentFile;
                File file6 = new File(parentFile, "oat");
                if (file6.exists() && (fileArrListFiles = file6.listFiles()) != null && fileArrListFiles.length > 0) {
                    String[] allDexCodeInstructionSets = InstructionSets.getAllDexCodeInstructionSets();
                    for (File file7 : fileArrListFiles) {
                        if (ArrayUtils.contains(allDexCodeInstructionSets, file7.getName())) {
                            this.mResolvedInstructionSets.add(file7.getName());
                            List listAsList = Arrays.asList(file7.listFiles());
                            if (!listAsList.isEmpty()) {
                                this.mResolvedInheritedFiles.addAll(listAsList);
                            }
                        }
                    }
                }
                if (mayInheritNativeLibs() && arrayList.isEmpty()) {
                    for (File file8 : new File[]{new File(parentFile, "lib"), new File(parentFile, "lib64")}) {
                        if (file8.exists() && file8.isDirectory()) {
                            LinkedList linkedList = new LinkedList();
                            for (File file9 : file8.listFiles()) {
                                if (file9.isDirectory()) {
                                    try {
                                        String relativePath = getRelativePath(file9, parentFile);
                                        if (!this.mResolvedNativeLibPaths.contains(relativePath)) {
                                            this.mResolvedNativeLibPaths.add(relativePath);
                                        }
                                        linkedList.addAll(Arrays.asList(file9.listFiles()));
                                    } catch (IOException e3) {
                                        Slog.e(TAG, "Skipping linking of native library directory!", e3);
                                        linkedList.clear();
                                    }
                                }
                            }
                            this.mResolvedInheritedFiles.addAll(linkedList);
                        }
                    }
                }
            } catch (PackageParser.PackageParserException e4) {
                throw PackageManagerException.from(e4);
            }
        } catch (IOException e5) {
            throw new PackageManagerException(-18, "Failed to resolve stage location", e5);
        }
    }

    @GuardedBy("mLock")
    private void assertApkConsistentLocked(String str, PackageParser.ApkLite apkLite) throws PackageManagerException {
        if (!this.mPackageName.equals(apkLite.packageName)) {
            throw new PackageManagerException(-2, str + " package " + apkLite.packageName + " inconsistent with " + this.mPackageName);
        }
        if (this.params.appPackageName != null && !this.params.appPackageName.equals(apkLite.packageName)) {
            throw new PackageManagerException(-2, str + " specified package " + this.params.appPackageName + " inconsistent with " + apkLite.packageName);
        }
        if (this.mVersionCode != apkLite.getLongVersionCode()) {
            throw new PackageManagerException(-2, str + " version code " + apkLite.versionCode + " inconsistent with " + this.mVersionCode);
        }
        if (!this.mSigningDetails.signaturesMatchExactly(apkLite.signingDetails)) {
            throw new PackageManagerException(-2, str + " signatures are inconsistent");
        }
    }

    private boolean isLinkPossible(List<File> list, File file) {
        try {
            StructStat structStatStat = Os.stat(file.getAbsolutePath());
            Iterator<File> it = list.iterator();
            while (it.hasNext()) {
                if (Os.stat(it.next().getAbsolutePath()).st_dev != structStatStat.st_dev) {
                    return false;
                }
            }
            return true;
        } catch (ErrnoException e) {
            Slog.w(TAG, "Failed to detect if linking possible: " + e);
            return false;
        }
    }

    public int getInstallerUid() {
        int i;
        synchronized (this.mLock) {
            i = this.mInstallerUid;
        }
        return i;
    }

    private static String getRelativePath(File file, File file2) throws IOException {
        String absolutePath = file.getAbsolutePath();
        String absolutePath2 = file2.getAbsolutePath();
        if (absolutePath.contains("/.")) {
            throw new IOException("Invalid path (was relative) : " + absolutePath);
        }
        if (absolutePath.startsWith(absolutePath2)) {
            return absolutePath.substring(absolutePath2.length());
        }
        throw new IOException("File: " + absolutePath + " outside base: " + absolutePath2);
    }

    private void createOatDirs(List<String> list, File file) throws PackageManagerException {
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            try {
                this.mPm.mInstaller.createOatDir(file.getAbsolutePath(), it.next());
            } catch (Installer.InstallerException e) {
                throw PackageManagerException.from(e);
            }
        }
    }

    private void linkFiles(List<File> list, File file, File file2) throws IOException {
        Iterator<File> it = list.iterator();
        while (it.hasNext()) {
            String relativePath = getRelativePath(it.next(), file2);
            try {
                this.mPm.mInstaller.linkFile(relativePath, file2.getAbsolutePath(), file.getAbsolutePath());
            } catch (Installer.InstallerException e) {
                throw new IOException("failed linkOrCreateDir(" + relativePath + ", " + file2 + ", " + file + ")", e);
            }
        }
        Slog.d(TAG, "Linked " + list.size() + " files into " + file);
    }

    private static void copyFiles(List<File> list, File file) throws IOException {
        for (File file2 : file.listFiles()) {
            if (file2.getName().endsWith(".tmp")) {
                file2.delete();
            }
        }
        for (File file3 : list) {
            File fileCreateTempFile = File.createTempFile("inherit", ".tmp", file);
            Slog.d(TAG, "Copying " + file3 + " to " + fileCreateTempFile);
            if (!FileUtils.copyFile(file3, fileCreateTempFile)) {
                throw new IOException("Failed to copy " + file3 + " to " + fileCreateTempFile);
            }
            try {
                Os.chmod(fileCreateTempFile.getAbsolutePath(), 420);
                File file4 = new File(file, file3.getName());
                Slog.d(TAG, "Renaming " + fileCreateTempFile + " to " + file4);
                if (!fileCreateTempFile.renameTo(file4)) {
                    throw new IOException("Failed to rename " + fileCreateTempFile + " to " + file4);
                }
            } catch (ErrnoException e) {
                throw new IOException("Failed to chmod " + fileCreateTempFile);
            }
        }
        Slog.d(TAG, "Copied " + list.size() + " files into " + file);
    }

    private static void extractNativeLibraries(File file, String str, boolean z) throws Throwable {
        NativeLibraryHelper.Handle handleCreate;
        File file2 = new File(file, "lib");
        if (!z) {
            NativeLibraryHelper.removeNativeBinariesFromDirLI(file2, true);
        }
        try {
            try {
                handleCreate = NativeLibraryHelper.Handle.create(file);
            } catch (Throwable th) {
                th = th;
                handleCreate = null;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            int iCopyNativeBinariesWithOverride = NativeLibraryHelper.copyNativeBinariesWithOverride(handleCreate, file2, str);
            if (iCopyNativeBinariesWithOverride == 1) {
                IoUtils.closeQuietly(handleCreate);
                return;
            }
            throw new PackageManagerException(iCopyNativeBinariesWithOverride, "Failed to extract native libraries, res=" + iCopyNativeBinariesWithOverride);
        } catch (IOException e2) {
            e = e2;
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Failed to extract native libraries", e);
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly(handleCreate);
            throw th;
        }
    }

    void setPermissionsResult(boolean z) {
        if (!this.mSealed) {
            throw new SecurityException("Must be sealed to accept permissions");
        }
        if (z) {
            synchronized (this.mLock) {
                this.mPermissionsManuallyAccepted = true;
                this.mHandler.obtainMessage(1).sendToTarget();
            }
            return;
        }
        destroyInternal();
        dispatchSessionFinished(-115, "User rejected permissions", null);
    }

    public void open() throws IOException {
        boolean z;
        if (this.mActiveCount.getAndIncrement() == 0) {
            this.mCallback.onSessionActiveChanged(this, true);
        }
        synchronized (this.mLock) {
            z = this.mPrepared;
            if (!this.mPrepared) {
                if (this.stageDir != null) {
                    PackageInstallerService.prepareStageDir(this.stageDir);
                    this.mPrepared = true;
                } else {
                    throw new IllegalArgumentException("stageDir must be set");
                }
            }
        }
        if (!z) {
            this.mCallback.onSessionPrepared(this);
        }
    }

    public void close() {
        closeInternal(true);
    }

    private void closeInternal(boolean z) {
        int iDecrementAndGet;
        synchronized (this.mLock) {
            if (z) {
                try {
                    assertCallerIsOwnerOrRootLocked();
                } catch (Throwable th) {
                    throw th;
                }
            }
            iDecrementAndGet = this.mActiveCount.decrementAndGet();
        }
        if (iDecrementAndGet == 0) {
            this.mCallback.onSessionActiveChanged(this, false);
        }
    }

    public void abandon() {
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            if (this.mRelinquished) {
                Slog.d(TAG, "Ignoring abandon after commit relinquished control");
            } else {
                destroyInternal();
                dispatchSessionFinished(-115, "Session was abandoned", null);
            }
        }
    }

    private void dispatchSessionFinished(int i, String str, Bundle bundle) {
        IPackageInstallObserver2 iPackageInstallObserver2;
        String str2;
        synchronized (this.mLock) {
            this.mFinalStatus = i;
            this.mFinalMessage = str;
            iPackageInstallObserver2 = this.mRemoteObserver;
            str2 = this.mPackageName;
        }
        if (iPackageInstallObserver2 != null) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str2;
            someArgsObtain.arg2 = str;
            someArgsObtain.arg3 = bundle;
            someArgsObtain.arg4 = iPackageInstallObserver2;
            someArgsObtain.argi1 = i;
            this.mHandler.obtainMessage(2, someArgsObtain).sendToTarget();
        }
        boolean z = false;
        boolean z2 = i == 1;
        if (bundle == null || !bundle.getBoolean("android.intent.extra.REPLACING")) {
            z = true;
        }
        if (z2 && z) {
            this.mPm.sendSessionCommitBroadcast(generateInfoScrubbed(true), this.userId);
        }
        this.mCallback.onSessionFinished(this, z2);
    }

    private void destroyInternal() {
        synchronized (this.mLock) {
            this.mSealed = true;
            this.mDestroyed = true;
            Iterator<RevocableFileDescriptor> it = this.mFds.iterator();
            while (it.hasNext()) {
                it.next().revoke();
            }
            Iterator<FileBridge> it2 = this.mBridges.iterator();
            while (it2.hasNext()) {
                it2.next().forceClose();
            }
        }
        if (this.stageDir != null) {
            try {
                this.mPm.mInstaller.rmPackageDir(this.stageDir.getAbsolutePath());
            } catch (Installer.InstallerException e) {
            }
        }
    }

    void dump(IndentingPrintWriter indentingPrintWriter) {
        synchronized (this.mLock) {
            dumpLocked(indentingPrintWriter);
        }
    }

    @GuardedBy("mLock")
    private void dumpLocked(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("Session " + this.sessionId + ":");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.printPair(ATTR_USER_ID, Integer.valueOf(this.userId));
        indentingPrintWriter.printPair("mOriginalInstallerUid", Integer.valueOf(this.mOriginalInstallerUid));
        indentingPrintWriter.printPair("mInstallerPackageName", this.mInstallerPackageName);
        indentingPrintWriter.printPair("mInstallerUid", Integer.valueOf(this.mInstallerUid));
        indentingPrintWriter.printPair(ATTR_CREATED_MILLIS, Long.valueOf(this.createdMillis));
        indentingPrintWriter.printPair("stageDir", this.stageDir);
        indentingPrintWriter.printPair("stageCid", this.stageCid);
        indentingPrintWriter.println();
        this.params.dump(indentingPrintWriter);
        indentingPrintWriter.printPair("mClientProgress", Float.valueOf(this.mClientProgress));
        indentingPrintWriter.printPair("mProgress", Float.valueOf(this.mProgress));
        indentingPrintWriter.printPair("mSealed", Boolean.valueOf(this.mSealed));
        indentingPrintWriter.printPair("mPermissionsManuallyAccepted", Boolean.valueOf(this.mPermissionsManuallyAccepted));
        indentingPrintWriter.printPair("mRelinquished", Boolean.valueOf(this.mRelinquished));
        indentingPrintWriter.printPair("mDestroyed", Boolean.valueOf(this.mDestroyed));
        indentingPrintWriter.printPair("mFds", Integer.valueOf(this.mFds.size()));
        indentingPrintWriter.printPair("mBridges", Integer.valueOf(this.mBridges.size()));
        indentingPrintWriter.printPair("mFinalStatus", Integer.valueOf(this.mFinalStatus));
        indentingPrintWriter.printPair("mFinalMessage", this.mFinalMessage);
        indentingPrintWriter.println();
        indentingPrintWriter.decreaseIndent();
    }

    private static void writeGrantedRuntimePermissionsLocked(XmlSerializer xmlSerializer, String[] strArr) throws IOException {
        if (strArr != null) {
            for (String str : strArr) {
                xmlSerializer.startTag(null, TAG_GRANTED_RUNTIME_PERMISSION);
                XmlUtils.writeStringAttribute(xmlSerializer, "name", str);
                xmlSerializer.endTag(null, TAG_GRANTED_RUNTIME_PERMISSION);
            }
        }
    }

    private static File buildAppIconFile(int i, File file) {
        return new File(file, "app_icon." + i + ".png");
    }

    void write(XmlSerializer xmlSerializer, File file) throws IOException {
        ?? r1;
        FileOutputStream fileOutputStream;
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            xmlSerializer.startTag(null, TAG_SESSION);
            XmlUtils.writeIntAttribute(xmlSerializer, ATTR_SESSION_ID, this.sessionId);
            XmlUtils.writeIntAttribute(xmlSerializer, ATTR_USER_ID, this.userId);
            XmlUtils.writeStringAttribute(xmlSerializer, ATTR_INSTALLER_PACKAGE_NAME, this.mInstallerPackageName);
            XmlUtils.writeIntAttribute(xmlSerializer, ATTR_INSTALLER_UID, this.mInstallerUid);
            XmlUtils.writeLongAttribute(xmlSerializer, ATTR_CREATED_MILLIS, this.createdMillis);
            if (this.stageDir != null) {
                XmlUtils.writeStringAttribute(xmlSerializer, ATTR_SESSION_STAGE_DIR, this.stageDir.getAbsolutePath());
            }
            if (this.stageCid != null) {
                XmlUtils.writeStringAttribute(xmlSerializer, ATTR_SESSION_STAGE_CID, this.stageCid);
            }
            XmlUtils.writeBooleanAttribute(xmlSerializer, ATTR_PREPARED, isPrepared());
            XmlUtils.writeBooleanAttribute(xmlSerializer, ATTR_SEALED, isSealed());
            XmlUtils.writeIntAttribute(xmlSerializer, ATTR_MODE, this.params.mode);
            XmlUtils.writeIntAttribute(xmlSerializer, ATTR_INSTALL_FLAGS, this.params.installFlags);
            XmlUtils.writeIntAttribute(xmlSerializer, ATTR_INSTALL_LOCATION, this.params.installLocation);
            XmlUtils.writeLongAttribute(xmlSerializer, ATTR_SIZE_BYTES, this.params.sizeBytes);
            XmlUtils.writeStringAttribute(xmlSerializer, ATTR_APP_PACKAGE_NAME, this.params.appPackageName);
            XmlUtils.writeStringAttribute(xmlSerializer, ATTR_APP_LABEL, this.params.appLabel);
            XmlUtils.writeUriAttribute(xmlSerializer, ATTR_ORIGINATING_URI, this.params.originatingUri);
            XmlUtils.writeIntAttribute(xmlSerializer, ATTR_ORIGINATING_UID, this.params.originatingUid);
            XmlUtils.writeUriAttribute(xmlSerializer, ATTR_REFERRER_URI, this.params.referrerUri);
            XmlUtils.writeStringAttribute(xmlSerializer, ATTR_ABI_OVERRIDE, this.params.abiOverride);
            XmlUtils.writeStringAttribute(xmlSerializer, ATTR_VOLUME_UUID, this.params.volumeUuid);
            XmlUtils.writeIntAttribute(xmlSerializer, ATTR_INSTALL_REASON, this.params.installReason);
            writeGrantedRuntimePermissionsLocked(xmlSerializer, this.params.grantedRuntimePermissions);
            File fileBuildAppIconFile = buildAppIconFile(this.sessionId, file);
            if (this.params.appIcon == null && fileBuildAppIconFile.exists()) {
                fileBuildAppIconFile.delete();
            } else if (this.params.appIcon != null && fileBuildAppIconFile.lastModified() != this.params.appIconLastModified) {
                try {
                    Slog.w(TAG, "Writing changed icon " + fileBuildAppIconFile);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    fileOutputStream = new FileOutputStream(fileBuildAppIconFile);
                    try {
                        this.params.appIcon.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
                        r1 = fileOutputStream;
                    } catch (IOException e) {
                        e = e;
                        Slog.w(TAG, "Failed to write icon " + fileBuildAppIconFile + ": " + e.getMessage());
                        r1 = fileOutputStream;
                    }
                } catch (IOException e2) {
                    e = e2;
                    fileOutputStream = null;
                } catch (Throwable th2) {
                    th = th2;
                    r1 = 0;
                    IoUtils.closeQuietly((AutoCloseable) r1);
                    throw th;
                }
                IoUtils.closeQuietly((AutoCloseable) r1);
                this.params.appIconLastModified = fileBuildAppIconFile.lastModified();
            }
            xmlSerializer.endTag(null, TAG_SESSION);
        }
    }

    private static String[] readGrantedRuntimePermissions(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        ArrayList arrayList = null;
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4 && TAG_GRANTED_RUNTIME_PERMISSION.equals(xmlPullParser.getName())) {
                String stringAttribute = XmlUtils.readStringAttribute(xmlPullParser, "name");
                if (arrayList == null) {
                    arrayList = new ArrayList();
                }
                arrayList.add(stringAttribute);
            }
        }
        if (arrayList == null) {
            return null;
        }
        String[] strArr = new String[arrayList.size()];
        arrayList.toArray(strArr);
        return strArr;
    }

    public static PackageInstallerSession readFromXml(XmlPullParser xmlPullParser, PackageInstallerService.InternalCallback internalCallback, Context context, PackageManagerService packageManagerService, Looper looper, File file) throws XmlPullParserException, IOException {
        int intAttribute = XmlUtils.readIntAttribute(xmlPullParser, ATTR_SESSION_ID);
        int intAttribute2 = XmlUtils.readIntAttribute(xmlPullParser, ATTR_USER_ID);
        String stringAttribute = XmlUtils.readStringAttribute(xmlPullParser, ATTR_INSTALLER_PACKAGE_NAME);
        int intAttribute3 = XmlUtils.readIntAttribute(xmlPullParser, ATTR_INSTALLER_UID, packageManagerService.getPackageUid(stringAttribute, 8192, intAttribute2));
        long longAttribute = XmlUtils.readLongAttribute(xmlPullParser, ATTR_CREATED_MILLIS);
        String stringAttribute2 = XmlUtils.readStringAttribute(xmlPullParser, ATTR_SESSION_STAGE_DIR);
        File file2 = stringAttribute2 != null ? new File(stringAttribute2) : null;
        String stringAttribute3 = XmlUtils.readStringAttribute(xmlPullParser, ATTR_SESSION_STAGE_CID);
        boolean booleanAttribute = XmlUtils.readBooleanAttribute(xmlPullParser, ATTR_PREPARED, true);
        boolean booleanAttribute2 = XmlUtils.readBooleanAttribute(xmlPullParser, ATTR_SEALED);
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(-1);
        sessionParams.mode = XmlUtils.readIntAttribute(xmlPullParser, ATTR_MODE);
        sessionParams.installFlags = XmlUtils.readIntAttribute(xmlPullParser, ATTR_INSTALL_FLAGS);
        sessionParams.installLocation = XmlUtils.readIntAttribute(xmlPullParser, ATTR_INSTALL_LOCATION);
        sessionParams.sizeBytes = XmlUtils.readLongAttribute(xmlPullParser, ATTR_SIZE_BYTES);
        sessionParams.appPackageName = XmlUtils.readStringAttribute(xmlPullParser, ATTR_APP_PACKAGE_NAME);
        sessionParams.appIcon = XmlUtils.readBitmapAttribute(xmlPullParser, ATTR_APP_ICON);
        sessionParams.appLabel = XmlUtils.readStringAttribute(xmlPullParser, ATTR_APP_LABEL);
        sessionParams.originatingUri = XmlUtils.readUriAttribute(xmlPullParser, ATTR_ORIGINATING_URI);
        sessionParams.originatingUid = XmlUtils.readIntAttribute(xmlPullParser, ATTR_ORIGINATING_UID, -1);
        sessionParams.referrerUri = XmlUtils.readUriAttribute(xmlPullParser, ATTR_REFERRER_URI);
        sessionParams.abiOverride = XmlUtils.readStringAttribute(xmlPullParser, ATTR_ABI_OVERRIDE);
        sessionParams.volumeUuid = XmlUtils.readStringAttribute(xmlPullParser, ATTR_VOLUME_UUID);
        sessionParams.installReason = XmlUtils.readIntAttribute(xmlPullParser, ATTR_INSTALL_REASON);
        sessionParams.grantedRuntimePermissions = readGrantedRuntimePermissions(xmlPullParser);
        File fileBuildAppIconFile = buildAppIconFile(intAttribute, file);
        if (fileBuildAppIconFile.exists()) {
            sessionParams.appIcon = BitmapFactory.decodeFile(fileBuildAppIconFile.getAbsolutePath());
            sessionParams.appIconLastModified = fileBuildAppIconFile.lastModified();
        }
        return new PackageInstallerSession(internalCallback, context, packageManagerService, looper, intAttribute, intAttribute2, stringAttribute, intAttribute3, sessionParams, longAttribute, file2, stringAttribute3, booleanAttribute, booleanAttribute2);
    }
}
