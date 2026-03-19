package com.android.server.pm;

import android.R;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PackageDeleteObserver;
import android.app.PackageInstallObserver;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageInstallerCallback;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.VersionedPackage;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.ExceptionUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageHelper;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.ImageUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.pm.permission.PermissionManagerInternal;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.powerhal.PowerHalManager;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PackageInstallerService extends IPackageInstaller.Stub {
    private static final boolean LOGD = false;
    private static final long MAX_ACTIVE_SESSIONS = 1024;
    private static final long MAX_AGE_MILLIS = 259200000;
    private static final long MAX_HISTORICAL_SESSIONS = 1048576;
    private static final long MAX_SESSION_AGE_ON_LOW_STORAGE_MILLIS = 28800000;
    private static final String TAG = "PackageInstaller";
    private static final String TAG_SESSIONS = "sessions";
    private static final FilenameFilter sStageFilter = new FilenameFilter() {
        @Override
        public boolean accept(File file, String str) {
            return PackageInstallerService.isStageName(str);
        }
    };
    private AppOpsManager mAppOps;
    private final Callbacks mCallbacks;
    private final Context mContext;
    private final Handler mInstallHandler;
    private final PackageManagerService mPm;
    private final File mSessionsDir;
    private final AtomicFile mSessionsFile;
    private PowerHalManager mPowerHalManager = MtkSystemServiceFactory.getInstance().makePowerHalManager();
    private final InternalCallback mInternalCallback = new InternalCallback();
    private final Random mRandom = new SecureRandom();

    @GuardedBy("mSessions")
    private final SparseBooleanArray mAllocatedSessions = new SparseBooleanArray();

    @GuardedBy("mSessions")
    private final SparseArray<PackageInstallerSession> mSessions = new SparseArray<>();

    @GuardedBy("mSessions")
    private final List<String> mHistoricalSessions = new ArrayList();

    @GuardedBy("mSessions")
    private final SparseIntArray mHistoricalSessionsByInstaller = new SparseIntArray();

    @GuardedBy("mSessions")
    private final SparseBooleanArray mLegacySessions = new SparseBooleanArray();
    private final PermissionManagerInternal mPermissionManager = (PermissionManagerInternal) LocalServices.getService(PermissionManagerInternal.class);
    private final HandlerThread mInstallThread = new HandlerThread(TAG);

    public PackageInstallerService(Context context, PackageManagerService packageManagerService) {
        this.mContext = context;
        this.mPm = packageManagerService;
        this.mInstallThread.start();
        this.mInstallHandler = new Handler(this.mInstallThread.getLooper());
        this.mCallbacks = new Callbacks(this.mInstallThread.getLooper());
        this.mSessionsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "install_sessions.xml"), "package-session");
        this.mSessionsDir = new File(Environment.getDataSystemDirectory(), "install_sessions");
        this.mSessionsDir.mkdirs();
    }

    public void systemReady() {
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        synchronized (this.mSessions) {
            readSessionsLocked();
            reconcileStagesLocked(StorageManager.UUID_PRIVATE_INTERNAL, false);
            reconcileStagesLocked(StorageManager.UUID_PRIVATE_INTERNAL, true);
            ArraySet<File> arraySetNewArraySet = newArraySet(this.mSessionsDir.listFiles());
            for (int i = 0; i < this.mSessions.size(); i++) {
                arraySetNewArraySet.remove(buildAppIconFile(this.mSessions.valueAt(i).sessionId));
            }
            for (File file : arraySetNewArraySet) {
                Slog.w(TAG, "Deleting orphan icon " + file);
                file.delete();
            }
        }
    }

    @GuardedBy("mSessions")
    private void reconcileStagesLocked(String str, boolean z) {
        ArraySet<File> stagingDirsOnVolume = getStagingDirsOnVolume(str, z);
        for (int i = 0; i < this.mSessions.size(); i++) {
            stagingDirsOnVolume.remove(this.mSessions.valueAt(i).stageDir);
        }
        removeStagingDirs(stagingDirsOnVolume);
    }

    private ArraySet<File> getStagingDirsOnVolume(String str, boolean z) {
        return newArraySet(buildStagingDir(str, z).listFiles(sStageFilter));
    }

    private void removeStagingDirs(ArraySet<File> arraySet) {
        for (File file : arraySet) {
            Slog.w(TAG, "Deleting orphan stage " + file);
            synchronized (this.mPm.mInstallLock) {
                this.mPm.removeCodePathLI(file);
            }
        }
    }

    public void freeStageDirs(String str, boolean z) {
        ArraySet<File> stagingDirsOnVolume = getStagingDirsOnVolume(str, z);
        long jCurrentTimeMillis = System.currentTimeMillis();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession packageInstallerSessionValueAt = this.mSessions.valueAt(i);
                if (stagingDirsOnVolume.contains(packageInstallerSessionValueAt.stageDir)) {
                    if (jCurrentTimeMillis - packageInstallerSessionValueAt.createdMillis >= MAX_SESSION_AGE_ON_LOW_STORAGE_MILLIS) {
                        packageInstallerSessionValueAt.abandon();
                    } else {
                        stagingDirsOnVolume.remove(packageInstallerSessionValueAt.stageDir);
                    }
                }
            }
        }
        removeStagingDirs(stagingDirsOnVolume);
    }

    public void onPrivateVolumeMounted(String str) {
        synchronized (this.mSessions) {
            reconcileStagesLocked(str, false);
        }
    }

    public static boolean isStageName(String str) {
        return (str.startsWith("vmdl") && str.endsWith(".tmp")) || (str.startsWith("smdl") && str.endsWith(".tmp")) || str.startsWith("smdl2tmp");
    }

    @Deprecated
    public File allocateStageDirLegacy(String str, boolean z) throws IOException {
        File fileBuildStageDir;
        synchronized (this.mSessions) {
            try {
                try {
                    int iAllocateSessionIdLocked = allocateSessionIdLocked();
                    this.mLegacySessions.put(iAllocateSessionIdLocked, true);
                    fileBuildStageDir = buildStageDir(str, iAllocateSessionIdLocked, z);
                    prepareStageDir(fileBuildStageDir);
                } catch (IllegalStateException e) {
                    throw new IOException(e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return fileBuildStageDir;
    }

    @Deprecated
    public String allocateExternalStageCidLegacy() {
        String str;
        synchronized (this.mSessions) {
            int iAllocateSessionIdLocked = allocateSessionIdLocked();
            this.mLegacySessions.put(iAllocateSessionIdLocked, true);
            str = "smdl" + iAllocateSessionIdLocked + ".tmp";
        }
        return str;
    }

    @GuardedBy("mSessions")
    private void readSessionsLocked() throws Throwable {
        Throwable th;
        FileInputStream fileInputStreamOpenRead;
        Throwable e;
        boolean z;
        this.mSessions.clear();
        try {
            try {
                fileInputStreamOpenRead = this.mSessionsFile.openRead();
                try {
                    try {
                        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                        while (true) {
                            int next = xmlPullParserNewPullParser.next();
                            if (next == 1) {
                                break;
                            }
                            if (next == 2 && "session".equals(xmlPullParserNewPullParser.getName())) {
                                try {
                                    PackageInstallerSession fromXml = PackageInstallerSession.readFromXml(xmlPullParserNewPullParser, this.mInternalCallback, this.mContext, this.mPm, this.mInstallThread.getLooper(), this.mSessionsDir);
                                    if (System.currentTimeMillis() - fromXml.createdMillis >= MAX_AGE_MILLIS) {
                                        Slog.w(TAG, "Abandoning old session first created at " + fromXml.createdMillis);
                                        z = false;
                                    } else {
                                        z = true;
                                    }
                                    if (z) {
                                        this.mSessions.put(fromXml.sessionId, fromXml);
                                    } else {
                                        addHistoricalSessionLocked(fromXml);
                                    }
                                    this.mAllocatedSessions.put(fromXml.sessionId, true);
                                } catch (Exception e2) {
                                    Slog.e(TAG, "Could not read session", e2);
                                }
                            }
                        }
                    } catch (FileNotFoundException e3) {
                    }
                } catch (IOException | XmlPullParserException e4) {
                    e = e4;
                    Slog.wtf(TAG, "Failed reading install sessions", e);
                }
            } catch (Throwable th2) {
                th = th2;
                IoUtils.closeQuietly((AutoCloseable) null);
                throw th;
            }
        } catch (FileNotFoundException e5) {
            fileInputStreamOpenRead = null;
        } catch (IOException | XmlPullParserException e6) {
            fileInputStreamOpenRead = null;
            e = e6;
        } catch (Throwable th3) {
            th = th3;
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
        IoUtils.closeQuietly(fileInputStreamOpenRead);
    }

    @GuardedBy("mSessions")
    private void addHistoricalSessionLocked(PackageInstallerSession packageInstallerSession) {
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        packageInstallerSession.dump(new IndentingPrintWriter(charArrayWriter, "    "));
        this.mHistoricalSessions.add(charArrayWriter.toString());
        int installerUid = packageInstallerSession.getInstallerUid();
        this.mHistoricalSessionsByInstaller.put(installerUid, this.mHistoricalSessionsByInstaller.get(installerUid) + 1);
    }

    @GuardedBy("mSessions")
    private void writeSessionsLocked() {
        FileOutputStream fileOutputStreamStartWrite;
        try {
            fileOutputStreamStartWrite = this.mSessionsFile.startWrite();
        } catch (IOException e) {
            fileOutputStreamStartWrite = null;
        }
        try {
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_SESSIONS);
            int size = this.mSessions.size();
            for (int i = 0; i < size; i++) {
                this.mSessions.valueAt(i).write(fastXmlSerializer, this.mSessionsDir);
            }
            fastXmlSerializer.endTag(null, TAG_SESSIONS);
            fastXmlSerializer.endDocument();
            this.mSessionsFile.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e2) {
            if (fileOutputStreamStartWrite != null) {
                this.mSessionsFile.failWrite(fileOutputStreamStartWrite);
            }
        }
    }

    private File buildAppIconFile(int i) {
        return new File(this.mSessionsDir, "app_icon." + i + ".png");
    }

    private void writeSessionsAsync() {
        IoThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                synchronized (PackageInstallerService.this.mSessions) {
                    PackageInstallerService.this.writeSessionsLocked();
                }
            }
        });
    }

    public int createSession(PackageInstaller.SessionParams sessionParams, String str, int i) {
        try {
            return createSessionInternal(sessionParams, str, i);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private int createSessionInternal(PackageInstaller.SessionParams sessionParams, String str, int i) throws IOException {
        String str2;
        int iAllocateSessionIdLocked;
        String strBuildExternalStageCid;
        File fileBuildStageDir;
        int launcherLargeIconSize;
        int launcherLargeIconSize2;
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, true, "createSession");
        if (this.mPm.isUserRestricted(i, "no_install_apps")) {
            throw new SecurityException("User restriction prevents installing");
        }
        if (callingUid == 2000 || callingUid == 0) {
            str2 = str;
            sessionParams.installFlags |= 32;
        } else {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") != 0) {
                str2 = str;
                this.mAppOps.checkPackage(callingUid, str2);
            } else {
                str2 = str;
            }
            sessionParams.installFlags &= -33;
            sessionParams.installFlags &= -65;
            sessionParams.installFlags &= -5;
            sessionParams.installFlags |= 2;
            if ((sessionParams.installFlags & 65536) != 0 && !this.mPm.isCallerVerifier(callingUid)) {
                sessionParams.installFlags &= -65537;
            }
        }
        if ((sessionParams.installFlags & 256) != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS") == -1) {
            throw new SecurityException("You need the android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS permission to use the PackageManager.INSTALL_GRANT_RUNTIME_PERMISSIONS flag");
        }
        if ((sessionParams.installFlags & 1) != 0 || (sessionParams.installFlags & 8) != 0) {
            throw new IllegalArgumentException("New installs into ASEC containers no longer supported");
        }
        if (sessionParams.appIcon != null && (sessionParams.appIcon.getWidth() > (launcherLargeIconSize2 = (launcherLargeIconSize = ((ActivityManager) this.mContext.getSystemService("activity")).getLauncherLargeIconSize()) * 2) || sessionParams.appIcon.getHeight() > launcherLargeIconSize2)) {
            sessionParams.appIcon = Bitmap.createScaledBitmap(sessionParams.appIcon, launcherLargeIconSize, launcherLargeIconSize, true);
        }
        switch (sessionParams.mode) {
            case 1:
            case 2:
                if ((sessionParams.installFlags & 16) != 0) {
                    if (!PackageHelper.fitsOnInternal(this.mContext, sessionParams)) {
                        throw new IOException("No suitable internal storage available");
                    }
                } else if ((sessionParams.installFlags & 8) != 0) {
                    if (!PackageHelper.fitsOnExternal(this.mContext, sessionParams)) {
                        throw new IOException("No suitable external storage available");
                    }
                } else if ((sessionParams.installFlags & 512) != 0) {
                    sessionParams.setInstallFlagsInternal();
                } else {
                    sessionParams.setInstallFlagsInternal();
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        sessionParams.volumeUuid = PackageHelper.resolveInstallVolume(this.mContext, sessionParams);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
                synchronized (this.mSessions) {
                    if (getSessionCount(this.mSessions, callingUid) >= MAX_ACTIVE_SESSIONS) {
                        throw new IllegalStateException("Too many active sessions for UID " + callingUid);
                    }
                    if (this.mHistoricalSessionsByInstaller.get(callingUid) >= MAX_HISTORICAL_SESSIONS) {
                        throw new IllegalStateException("Too many historical sessions for UID " + callingUid);
                    }
                    iAllocateSessionIdLocked = allocateSessionIdLocked();
                }
                long jCurrentTimeMillis = System.currentTimeMillis();
                if ((sessionParams.installFlags & 16) != 0) {
                    fileBuildStageDir = buildStageDir(sessionParams.volumeUuid, iAllocateSessionIdLocked, (sessionParams.installFlags & 2048) != 0);
                    strBuildExternalStageCid = null;
                } else {
                    strBuildExternalStageCid = buildExternalStageCid(iAllocateSessionIdLocked);
                    fileBuildStageDir = null;
                }
                PackageInstallerSession packageInstallerSession = new PackageInstallerSession(this.mInternalCallback, this.mContext, this.mPm, this.mInstallThread.getLooper(), iAllocateSessionIdLocked, i, str2, callingUid, sessionParams, jCurrentTimeMillis, fileBuildStageDir, strBuildExternalStageCid, false, false);
                synchronized (this.mSessions) {
                    this.mSessions.put(iAllocateSessionIdLocked, packageInstallerSession);
                    break;
                }
                this.mCallbacks.notifySessionCreated(packageInstallerSession.sessionId, packageInstallerSession.userId);
                writeSessionsAsync();
                return iAllocateSessionIdLocked;
            default:
                throw new IllegalArgumentException("Invalid install mode: " + sessionParams.mode);
        }
    }

    public void updateSessionAppIcon(int i, Bitmap bitmap) {
        int launcherLargeIconSize;
        int launcherLargeIconSize2;
        synchronized (this.mSessions) {
            PackageInstallerSession packageInstallerSession = this.mSessions.get(i);
            if (packageInstallerSession == null || !isCallingUidOwner(packageInstallerSession)) {
                throw new SecurityException("Caller has no access to session " + i);
            }
            if (bitmap != null && (bitmap.getWidth() > (launcherLargeIconSize2 = (launcherLargeIconSize = ((ActivityManager) this.mContext.getSystemService("activity")).getLauncherLargeIconSize()) * 2) || bitmap.getHeight() > launcherLargeIconSize2)) {
                bitmap = Bitmap.createScaledBitmap(bitmap, launcherLargeIconSize, launcherLargeIconSize, true);
            }
            packageInstallerSession.params.appIcon = bitmap;
            packageInstallerSession.params.appIconLastModified = -1L;
            this.mInternalCallback.onSessionBadgingChanged(packageInstallerSession);
        }
    }

    public void updateSessionAppLabel(int i, String str) {
        synchronized (this.mSessions) {
            PackageInstallerSession packageInstallerSession = this.mSessions.get(i);
            if (packageInstallerSession == null || !isCallingUidOwner(packageInstallerSession)) {
                throw new SecurityException("Caller has no access to session " + i);
            }
            packageInstallerSession.params.appLabel = str;
            this.mInternalCallback.onSessionBadgingChanged(packageInstallerSession);
        }
    }

    public void abandonSession(int i) {
        synchronized (this.mSessions) {
            PackageInstallerSession packageInstallerSession = this.mSessions.get(i);
            if (packageInstallerSession == null || !isCallingUidOwner(packageInstallerSession)) {
                throw new SecurityException("Caller has no access to session " + i);
            }
            packageInstallerSession.abandon();
        }
    }

    public IPackageInstallerSession openSession(int i) {
        try {
            if (this.mPowerHalManager != null) {
                this.mPowerHalManager.setInstallationBoost(true);
            }
            return openSessionInternal(i);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private IPackageInstallerSession openSessionInternal(int i) throws IOException {
        PackageInstallerSession packageInstallerSession;
        synchronized (this.mSessions) {
            packageInstallerSession = this.mSessions.get(i);
            if (packageInstallerSession == null || !isCallingUidOwner(packageInstallerSession)) {
                throw new SecurityException("Caller has no access to session " + i);
            }
            packageInstallerSession.open();
        }
        return packageInstallerSession;
    }

    @GuardedBy("mSessions")
    private int allocateSessionIdLocked() {
        int i = 0;
        while (true) {
            int iNextInt = this.mRandom.nextInt(2147483646) + 1;
            if (!this.mAllocatedSessions.get(iNextInt, false)) {
                this.mAllocatedSessions.put(iNextInt, true);
                return iNextInt;
            }
            int i2 = i + 1;
            if (i < 32) {
                i = i2;
            } else {
                throw new IllegalStateException("Failed to allocate session ID");
            }
        }
    }

    private File buildStagingDir(String str, boolean z) {
        return Environment.getDataAppDirectory(str);
    }

    private File buildStageDir(String str, int i, boolean z) {
        return new File(buildStagingDir(str, z), "vmdl" + i + ".tmp");
    }

    static void prepareStageDir(File file) throws IOException {
        if (file.exists()) {
            throw new IOException("Session dir already exists: " + file);
        }
        try {
            Os.mkdir(file.getAbsolutePath(), 493);
            Os.chmod(file.getAbsolutePath(), 493);
            if (!SELinux.restorecon(file)) {
                throw new IOException("Failed to restorecon session dir: " + file);
            }
        } catch (ErrnoException e) {
            throw new IOException("Failed to prepare session dir: " + file, e);
        }
    }

    private String buildExternalStageCid(int i) {
        return "smdl" + i + ".tmp";
    }

    public PackageInstaller.SessionInfo getSessionInfo(int i) {
        PackageInstaller.SessionInfo sessionInfoGenerateInfoForCaller;
        synchronized (this.mSessions) {
            PackageInstallerSession packageInstallerSession = this.mSessions.get(i);
            if (packageInstallerSession != null) {
                sessionInfoGenerateInfoForCaller = packageInstallerSession.generateInfoForCaller(true, Binder.getCallingUid());
            } else {
                sessionInfoGenerateInfoForCaller = null;
            }
        }
        return sessionInfoGenerateInfoForCaller;
    }

    public ParceledListSlice<PackageInstaller.SessionInfo> getAllSessions(int i) {
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, false, "getAllSessions");
        ArrayList arrayList = new ArrayList();
        synchronized (this.mSessions) {
            for (int i2 = 0; i2 < this.mSessions.size(); i2++) {
                PackageInstallerSession packageInstallerSessionValueAt = this.mSessions.valueAt(i2);
                if (packageInstallerSessionValueAt.userId == i) {
                    arrayList.add(packageInstallerSessionValueAt.generateInfoForCaller(false, callingUid));
                }
            }
        }
        return new ParceledListSlice<>(arrayList);
    }

    public ParceledListSlice<PackageInstaller.SessionInfo> getMySessions(String str, int i) {
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i, true, false, "getMySessions");
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        ArrayList arrayList = new ArrayList();
        synchronized (this.mSessions) {
            for (int i2 = 0; i2 < this.mSessions.size(); i2++) {
                PackageInstallerSession packageInstallerSessionValueAt = this.mSessions.valueAt(i2);
                PackageInstaller.SessionInfo sessionInfoGenerateInfoForCaller = packageInstallerSessionValueAt.generateInfoForCaller(false, 1000);
                if (Objects.equals(sessionInfoGenerateInfoForCaller.getInstallerPackageName(), str) && packageInstallerSessionValueAt.userId == i) {
                    arrayList.add(sessionInfoGenerateInfoForCaller);
                }
            }
        }
        return new ParceledListSlice<>(arrayList);
    }

    public void uninstall(VersionedPackage versionedPackage, String str, int i, IntentSender intentSender, int i2) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, true, true, "uninstall");
        if (callingUid != 2000 && callingUid != 0) {
            this.mAppOps.checkPackage(callingUid, str);
        }
        int userId = UserHandle.getUserId(callingUid);
        DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        boolean z = devicePolicyManagerInternal != null && devicePolicyManagerInternal.isActiveAdminWithPolicy(callingUid, -1) && devicePolicyManagerInternal.isUserAffiliatedWithDevice(userId);
        PackageDeleteObserverAdapter packageDeleteObserverAdapter = new PackageDeleteObserverAdapter(this.mContext, intentSender, versionedPackage.getPackageName(), z, i2);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DELETE_PACKAGES") == 0) {
            this.mPm.deletePackageVersioned(versionedPackage, packageDeleteObserverAdapter.getBinder(), i2, i);
            return;
        }
        if (z) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mPm.deletePackageVersioned(versionedPackage, packageDeleteObserverAdapter.getBinder(), i2, i);
                return;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        if (this.mPm.getApplicationInfo(str, 0, i2).targetSdkVersion >= 28) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.REQUEST_DELETE_PACKAGES", null);
        }
        Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
        intent.setData(Uri.fromParts(Settings.ATTR_PACKAGE, versionedPackage.getPackageName(), null));
        intent.putExtra("android.content.pm.extra.CALLBACK", packageDeleteObserverAdapter.getBinder().asBinder());
        packageDeleteObserverAdapter.onUserActionRequired(intent);
    }

    public void setPermissionsResult(int i, boolean z) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", TAG);
        synchronized (this.mSessions) {
            PackageInstallerSession packageInstallerSession = this.mSessions.get(i);
            if (packageInstallerSession != null) {
                packageInstallerSession.setPermissionsResult(z);
            }
        }
    }

    public void registerCallback(IPackageInstallerCallback iPackageInstallerCallback, int i) {
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i, true, false, "registerCallback");
        this.mCallbacks.register(iPackageInstallerCallback, i);
    }

    public void unregisterCallback(IPackageInstallerCallback iPackageInstallerCallback) {
        this.mCallbacks.unregister(iPackageInstallerCallback);
    }

    private static int getSessionCount(SparseArray<PackageInstallerSession> sparseArray, int i) {
        int size = sparseArray.size();
        int i2 = 0;
        for (int i3 = 0; i3 < size; i3++) {
            if (sparseArray.valueAt(i3).getInstallerUid() == i) {
                i2++;
            }
        }
        return i2;
    }

    private boolean isCallingUidOwner(PackageInstallerSession packageInstallerSession) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0) {
            return true;
        }
        return packageInstallerSession != null && callingUid == packageInstallerSession.getInstallerUid();
    }

    static class PackageDeleteObserverAdapter extends PackageDeleteObserver {
        private final Context mContext;
        private final Notification mNotification;
        private final String mPackageName;
        private final IntentSender mTarget;

        public PackageDeleteObserverAdapter(Context context, IntentSender intentSender, String str, boolean z, int i) {
            this.mContext = context;
            this.mTarget = intentSender;
            this.mPackageName = str;
            if (z) {
                this.mNotification = PackageInstallerService.buildSuccessNotification(this.mContext, this.mContext.getResources().getString(R.string.factory_reset_warning), str, i);
            } else {
                this.mNotification = null;
            }
        }

        public void onUserActionRequired(Intent intent) {
            Intent intent2 = new Intent();
            intent2.putExtra("android.content.pm.extra.PACKAGE_NAME", this.mPackageName);
            intent2.putExtra("android.content.pm.extra.STATUS", -1);
            intent2.putExtra("android.intent.extra.INTENT", intent);
            try {
                this.mTarget.sendIntent(this.mContext, 0, intent2, null, null);
            } catch (IntentSender.SendIntentException e) {
            }
        }

        public void onPackageDeleted(String str, int i, String str2) {
            if (1 == i && this.mNotification != null) {
                ((NotificationManager) this.mContext.getSystemService("notification")).notify(str, 21, this.mNotification);
            }
            Intent intent = new Intent();
            intent.putExtra("android.content.pm.extra.PACKAGE_NAME", this.mPackageName);
            intent.putExtra("android.content.pm.extra.STATUS", PackageManager.deleteStatusToPublicStatus(i));
            intent.putExtra("android.content.pm.extra.STATUS_MESSAGE", PackageManager.deleteStatusToString(i, str2));
            intent.putExtra("android.content.pm.extra.LEGACY_STATUS", i);
            try {
                this.mTarget.sendIntent(this.mContext, 0, intent, null, null);
            } catch (IntentSender.SendIntentException e) {
            }
        }
    }

    static class PackageInstallObserverAdapter extends PackageInstallObserver {
        private final Context mContext;
        private final int mSessionId;
        private final boolean mShowNotification;
        private final IntentSender mTarget;
        private final int mUserId;

        public PackageInstallObserverAdapter(Context context, IntentSender intentSender, int i, boolean z, int i2) {
            this.mContext = context;
            this.mTarget = intentSender;
            this.mSessionId = i;
            this.mShowNotification = z;
            this.mUserId = i2;
        }

        public void onUserActionRequired(Intent intent) {
            Intent intent2 = new Intent();
            intent2.putExtra("android.content.pm.extra.SESSION_ID", this.mSessionId);
            intent2.putExtra("android.content.pm.extra.STATUS", -1);
            intent2.putExtra("android.intent.extra.INTENT", intent);
            try {
                this.mTarget.sendIntent(this.mContext, 0, intent2, null, null);
            } catch (IntentSender.SendIntentException e) {
            }
        }

        public void onPackageInstalled(String str, int i, String str2, Bundle bundle) {
            if (1 == i && this.mShowNotification) {
                Notification notificationBuildSuccessNotification = PackageInstallerService.buildSuccessNotification(this.mContext, this.mContext.getResources().getString(bundle != null && bundle.getBoolean("android.intent.extra.REPLACING") ? R.string.factorytest_no_action : R.string.factorytest_failed), str, this.mUserId);
                if (notificationBuildSuccessNotification != null) {
                    ((NotificationManager) this.mContext.getSystemService("notification")).notify(str, 21, notificationBuildSuccessNotification);
                }
            }
            Intent intent = new Intent();
            intent.putExtra("android.content.pm.extra.PACKAGE_NAME", str);
            intent.putExtra("android.content.pm.extra.SESSION_ID", this.mSessionId);
            intent.putExtra("android.content.pm.extra.STATUS", PackageManager.installStatusToPublicStatus(i));
            intent.putExtra("android.content.pm.extra.STATUS_MESSAGE", PackageManager.installStatusToString(i, str2));
            intent.putExtra("android.content.pm.extra.LEGACY_STATUS", i);
            if (bundle != null) {
                String string = bundle.getString("android.content.pm.extra.FAILURE_EXISTING_PACKAGE");
                if (!TextUtils.isEmpty(string)) {
                    intent.putExtra("android.content.pm.extra.OTHER_PACKAGE_NAME", string);
                }
            }
            try {
                this.mTarget.sendIntent(this.mContext, 0, intent, null, null);
            } catch (IntentSender.SendIntentException e) {
            }
        }
    }

    private static Notification buildSuccessNotification(Context context, String str, String str2, int i) {
        PackageInfo packageInfo;
        try {
            packageInfo = AppGlobals.getPackageManager().getPackageInfo(str2, 67108864, i);
        } catch (RemoteException e) {
            packageInfo = null;
        }
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            Slog.w(TAG, "Notification not built for package: " + str2);
            return null;
        }
        PackageManager packageManager = context.getPackageManager();
        return new Notification.Builder(context, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(R.drawable.divider_horizontal_holo_dark).setColor(context.getResources().getColor(R.color.car_colorPrimary)).setContentTitle(packageInfo.applicationInfo.loadLabel(packageManager)).setContentText(str).setStyle(new Notification.BigTextStyle().bigText(str)).setLargeIcon(ImageUtils.buildScaledBitmap(packageInfo.applicationInfo.loadIcon(packageManager), context.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_width), context.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_height))).build();
    }

    public static <E> ArraySet<E> newArraySet(E... eArr) {
        ArraySet<E> arraySet = new ArraySet<>();
        if (eArr != null) {
            arraySet.ensureCapacity(eArr.length);
            Collections.addAll(arraySet, eArr);
        }
        return arraySet;
    }

    private static class Callbacks extends Handler {
        private static final int MSG_SESSION_ACTIVE_CHANGED = 3;
        private static final int MSG_SESSION_BADGING_CHANGED = 2;
        private static final int MSG_SESSION_CREATED = 1;
        private static final int MSG_SESSION_FINISHED = 5;
        private static final int MSG_SESSION_PROGRESS_CHANGED = 4;
        private final RemoteCallbackList<IPackageInstallerCallback> mCallbacks;

        public Callbacks(Looper looper) {
            super(looper);
            this.mCallbacks = new RemoteCallbackList<>();
        }

        public void register(IPackageInstallerCallback iPackageInstallerCallback, int i) {
            this.mCallbacks.register(iPackageInstallerCallback, new UserHandle(i));
        }

        public void unregister(IPackageInstallerCallback iPackageInstallerCallback) {
            this.mCallbacks.unregister(iPackageInstallerCallback);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.arg2;
            int iBeginBroadcast = this.mCallbacks.beginBroadcast();
            for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
                IPackageInstallerCallback iPackageInstallerCallback = (IPackageInstallerCallback) this.mCallbacks.getBroadcastItem(i2);
                if (i == ((UserHandle) this.mCallbacks.getBroadcastCookie(i2)).getIdentifier()) {
                    try {
                        invokeCallback(iPackageInstallerCallback, message);
                    } catch (RemoteException e) {
                    }
                }
            }
            this.mCallbacks.finishBroadcast();
        }

        private void invokeCallback(IPackageInstallerCallback iPackageInstallerCallback, Message message) throws RemoteException {
            int i = message.arg1;
            switch (message.what) {
                case 1:
                    iPackageInstallerCallback.onSessionCreated(i);
                    break;
                case 2:
                    iPackageInstallerCallback.onSessionBadgingChanged(i);
                    break;
                case 3:
                    iPackageInstallerCallback.onSessionActiveChanged(i, ((Boolean) message.obj).booleanValue());
                    break;
                case 4:
                    iPackageInstallerCallback.onSessionProgressChanged(i, ((Float) message.obj).floatValue());
                    break;
                case 5:
                    iPackageInstallerCallback.onSessionFinished(i, ((Boolean) message.obj).booleanValue());
                    break;
            }
        }

        private void notifySessionCreated(int i, int i2) {
            obtainMessage(1, i, i2).sendToTarget();
        }

        private void notifySessionBadgingChanged(int i, int i2) {
            obtainMessage(2, i, i2).sendToTarget();
        }

        private void notifySessionActiveChanged(int i, int i2, boolean z) {
            obtainMessage(3, i, i2, Boolean.valueOf(z)).sendToTarget();
        }

        private void notifySessionProgressChanged(int i, int i2, float f) {
            obtainMessage(4, i, i2, Float.valueOf(f)).sendToTarget();
        }

        public void notifySessionFinished(int i, int i2, boolean z) {
            obtainMessage(5, i, i2, Boolean.valueOf(z)).sendToTarget();
        }
    }

    void dump(IndentingPrintWriter indentingPrintWriter) {
        synchronized (this.mSessions) {
            indentingPrintWriter.println("Active install sessions:");
            indentingPrintWriter.increaseIndent();
            int size = this.mSessions.size();
            for (int i = 0; i < size; i++) {
                this.mSessions.valueAt(i).dump(indentingPrintWriter);
                indentingPrintWriter.println();
            }
            indentingPrintWriter.println();
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println("Historical install sessions:");
            indentingPrintWriter.increaseIndent();
            int size2 = this.mHistoricalSessions.size();
            for (int i2 = 0; i2 < size2; i2++) {
                indentingPrintWriter.print(this.mHistoricalSessions.get(i2));
                indentingPrintWriter.println();
            }
            indentingPrintWriter.println();
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println("Legacy install sessions:");
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.println(this.mLegacySessions.toString());
            indentingPrintWriter.decreaseIndent();
        }
    }

    class InternalCallback {
        InternalCallback() {
        }

        public void onSessionBadgingChanged(PackageInstallerSession packageInstallerSession) {
            PackageInstallerService.this.mCallbacks.notifySessionBadgingChanged(packageInstallerSession.sessionId, packageInstallerSession.userId);
            PackageInstallerService.this.writeSessionsAsync();
        }

        public void onSessionActiveChanged(PackageInstallerSession packageInstallerSession, boolean z) {
            PackageInstallerService.this.mCallbacks.notifySessionActiveChanged(packageInstallerSession.sessionId, packageInstallerSession.userId, z);
        }

        public void onSessionProgressChanged(PackageInstallerSession packageInstallerSession, float f) {
            PackageInstallerService.this.mCallbacks.notifySessionProgressChanged(packageInstallerSession.sessionId, packageInstallerSession.userId, f);
        }

        public void onSessionFinished(final PackageInstallerSession packageInstallerSession, boolean z) {
            PackageInstallerService.this.mCallbacks.notifySessionFinished(packageInstallerSession.sessionId, packageInstallerSession.userId, z);
            PackageInstallerService.this.mInstallHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (PackageInstallerService.this.mSessions) {
                        PackageInstallerService.this.mSessions.remove(packageInstallerSession.sessionId);
                        PackageInstallerService.this.addHistoricalSessionLocked(packageInstallerSession);
                        File fileBuildAppIconFile = PackageInstallerService.this.buildAppIconFile(packageInstallerSession.sessionId);
                        if (fileBuildAppIconFile.exists()) {
                            fileBuildAppIconFile.delete();
                        }
                        PackageInstallerService.this.writeSessionsLocked();
                    }
                }
            });
        }

        public void onSessionPrepared(PackageInstallerSession packageInstallerSession) {
            PackageInstallerService.this.writeSessionsAsync();
        }

        public void onSessionSealedBlocking(PackageInstallerSession packageInstallerSession) {
            synchronized (PackageInstallerService.this.mSessions) {
                PackageInstallerService.this.writeSessionsLocked();
            }
        }
    }
}
