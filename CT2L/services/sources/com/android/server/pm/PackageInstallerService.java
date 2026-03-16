package com.android.server.pm;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.PackageDeleteObserver;
import android.app.PackageInstallObserver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageInstallerCallback;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.UserHandle;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.ExceptionUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageHelper;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.XmlUtils;
import com.android.server.IoThread;
import com.google.android.collect.Sets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PackageInstallerService extends IPackageInstaller.Stub {
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
    private static final String ATTR_MODE = "mode";
    private static final String ATTR_ORIGINATING_URI = "originatingUri";
    private static final String ATTR_PREPARED = "prepared";
    private static final String ATTR_REFERRER_URI = "referrerUri";
    private static final String ATTR_SEALED = "sealed";
    private static final String ATTR_SESSION_ID = "sessionId";
    private static final String ATTR_SESSION_STAGE_CID = "sessionStageCid";
    private static final String ATTR_SESSION_STAGE_DIR = "sessionStageDir";
    private static final String ATTR_SIZE_BYTES = "sizeBytes";
    private static final String ATTR_USER_ID = "userId";
    private static final boolean LOGD = false;
    private static final long MAX_ACTIVE_SESSIONS = 1024;
    private static final long MAX_AGE_MILLIS = 259200000;
    private static final long MAX_HISTORICAL_SESSIONS = 1048576;
    private static final String TAG = "PackageInstaller";
    private static final String TAG_SESSION = "session";
    private static final String TAG_SESSIONS = "sessions";
    private static final FilenameFilter sStageFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return PackageInstallerService.isStageName(name);
        }
    };
    private final AppOpsManager mAppOps;
    private final Callbacks mCallbacks;
    private final Context mContext;
    private final Handler mInstallHandler;
    private final PackageManagerService mPm;
    private final File mSessionsDir;
    private final AtomicFile mSessionsFile;
    private final File mStagingDir;
    private final InternalCallback mInternalCallback = new InternalCallback();
    private final Random mRandom = new SecureRandom();

    @GuardedBy("mSessions")
    private final SparseArray<PackageInstallerSession> mSessions = new SparseArray<>();

    @GuardedBy("mSessions")
    private final SparseArray<PackageInstallerSession> mHistoricalSessions = new SparseArray<>();

    @GuardedBy("mSessions")
    private final SparseBooleanArray mLegacySessions = new SparseBooleanArray();
    private final HandlerThread mInstallThread = new HandlerThread(TAG);

    public PackageInstallerService(Context context, PackageManagerService pm, File stagingDir) {
        this.mContext = context;
        this.mPm = pm;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mStagingDir = stagingDir;
        this.mInstallThread.start();
        this.mInstallHandler = new Handler(this.mInstallThread.getLooper());
        this.mCallbacks = new Callbacks(this.mInstallThread.getLooper());
        this.mSessionsFile = new AtomicFile(new File(Environment.getSystemSecureDirectory(), "install_sessions.xml"));
        this.mSessionsDir = new File(Environment.getSystemSecureDirectory(), "install_sessions");
        this.mSessionsDir.mkdirs();
        synchronized (this.mSessions) {
            readSessionsLocked();
            ArraySet<File> unclaimedStages = Sets.newArraySet(this.mStagingDir.listFiles(sStageFilter));
            ArraySet<File> unclaimedIcons = Sets.newArraySet(this.mSessionsDir.listFiles());
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                unclaimedStages.remove(session.stageDir);
                unclaimedIcons.remove(buildAppIconFile(session.sessionId));
            }
            for (File stage : unclaimedStages) {
                Slog.w(TAG, "Deleting orphan stage " + stage);
                if (stage.isDirectory()) {
                    FileUtils.deleteContents(stage);
                }
                stage.delete();
            }
            for (File icon : unclaimedIcons) {
                Slog.w(TAG, "Deleting orphan icon " + icon);
                icon.delete();
            }
        }
    }

    public void onSecureContainersAvailable() {
        synchronized (this.mSessions) {
            ArraySet<String> unclaimed = new ArraySet<>();
            String[] arr$ = PackageHelper.getSecureContainerList();
            for (String cid : arr$) {
                if (isStageName(cid)) {
                    unclaimed.add(cid);
                }
            }
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                String cid2 = session.stageCid;
                if (unclaimed.remove(cid2)) {
                    PackageHelper.mountSdDir(cid2, PackageManagerService.getEncryptKey(), 1000);
                }
            }
            for (String cid3 : unclaimed) {
                Slog.w(TAG, "Deleting orphan container " + cid3);
                PackageHelper.destroySdDir(cid3);
            }
        }
    }

    public static boolean isStageName(String name) {
        boolean isFile = name.startsWith("vmdl") && name.endsWith(".tmp");
        boolean isContainer = name.startsWith("smdl") && name.endsWith(".tmp");
        boolean isLegacyContainer = name.startsWith("smdl2tmp");
        if (isFile || isContainer || isLegacyContainer) {
            return true;
        }
        return LOGD;
    }

    @Deprecated
    public File allocateInternalStageDirLegacy() throws IOException {
        File stageDir;
        synchronized (this.mSessions) {
            try {
                int sessionId = allocateSessionIdLocked();
                this.mLegacySessions.put(sessionId, true);
                stageDir = buildInternalStageDir(sessionId);
                prepareInternalStageDir(stageDir);
            } catch (IllegalStateException e) {
                throw new IOException(e);
            }
        }
        return stageDir;
    }

    @Deprecated
    public String allocateExternalStageCidLegacy() {
        String str;
        synchronized (this.mSessions) {
            int sessionId = allocateSessionIdLocked();
            this.mLegacySessions.put(sessionId, true);
            str = "smdl" + sessionId + ".tmp";
        }
        return str;
    }

    private void readSessionsLocked() {
        boolean valid;
        this.mSessions.clear();
        FileInputStream fis = null;
        try {
            try {
                fis = this.mSessionsFile.openRead();
                XmlPullParser in = Xml.newPullParser();
                in.setInput(fis, null);
                while (true) {
                    int type = in.next();
                    if (type != 1) {
                        if (type == 2) {
                            String tag = in.getName();
                            if (TAG_SESSION.equals(tag)) {
                                PackageInstallerSession session = readSessionLocked(in);
                                long age = System.currentTimeMillis() - session.createdMillis;
                                if (age >= MAX_AGE_MILLIS) {
                                    Slog.w(TAG, "Abandoning old session first created at " + session.createdMillis);
                                    valid = LOGD;
                                } else if (session.stageDir != null && !session.stageDir.exists()) {
                                    Slog.w(TAG, "Abandoning internal session with missing stage " + session.stageDir);
                                    valid = LOGD;
                                } else {
                                    valid = true;
                                }
                                if (valid) {
                                    this.mSessions.put(session.sessionId, session);
                                } else {
                                    this.mHistoricalSessions.put(session.sessionId, session);
                                }
                            }
                        }
                    } else {
                        IoUtils.closeQuietly(fis);
                        return;
                    }
                }
            } catch (FileNotFoundException e) {
                IoUtils.closeQuietly(fis);
            } catch (IOException e2) {
                Slog.wtf(TAG, "Failed reading install sessions", e2);
                IoUtils.closeQuietly(fis);
            } catch (XmlPullParserException e3) {
                Slog.wtf(TAG, "Failed reading install sessions", e3);
                IoUtils.closeQuietly(fis);
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(fis);
            throw th;
        }
    }

    private PackageInstallerSession readSessionLocked(XmlPullParser in) throws IOException {
        int sessionId = XmlUtils.readIntAttribute(in, ATTR_SESSION_ID);
        int userId = XmlUtils.readIntAttribute(in, ATTR_USER_ID);
        String installerPackageName = XmlUtils.readStringAttribute(in, ATTR_INSTALLER_PACKAGE_NAME);
        int installerUid = XmlUtils.readIntAttribute(in, ATTR_INSTALLER_UID, this.mPm.getPackageUid(installerPackageName, userId));
        long createdMillis = XmlUtils.readLongAttribute(in, ATTR_CREATED_MILLIS);
        String stageDirRaw = XmlUtils.readStringAttribute(in, ATTR_SESSION_STAGE_DIR);
        File stageDir = stageDirRaw != null ? new File(stageDirRaw) : null;
        String stageCid = XmlUtils.readStringAttribute(in, ATTR_SESSION_STAGE_CID);
        boolean prepared = XmlUtils.readBooleanAttribute(in, ATTR_PREPARED, true);
        boolean sealed = XmlUtils.readBooleanAttribute(in, ATTR_SEALED);
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(-1);
        params.mode = XmlUtils.readIntAttribute(in, ATTR_MODE);
        params.installFlags = XmlUtils.readIntAttribute(in, ATTR_INSTALL_FLAGS);
        params.installLocation = XmlUtils.readIntAttribute(in, ATTR_INSTALL_LOCATION);
        params.sizeBytes = XmlUtils.readLongAttribute(in, ATTR_SIZE_BYTES);
        params.appPackageName = XmlUtils.readStringAttribute(in, ATTR_APP_PACKAGE_NAME);
        params.appIcon = XmlUtils.readBitmapAttribute(in, ATTR_APP_ICON);
        params.appLabel = XmlUtils.readStringAttribute(in, ATTR_APP_LABEL);
        params.originatingUri = XmlUtils.readUriAttribute(in, ATTR_ORIGINATING_URI);
        params.referrerUri = XmlUtils.readUriAttribute(in, ATTR_REFERRER_URI);
        params.abiOverride = XmlUtils.readStringAttribute(in, ATTR_ABI_OVERRIDE);
        File appIconFile = buildAppIconFile(sessionId);
        if (appIconFile.exists()) {
            params.appIcon = BitmapFactory.decodeFile(appIconFile.getAbsolutePath());
            params.appIconLastModified = appIconFile.lastModified();
        }
        return new PackageInstallerSession(this.mInternalCallback, this.mContext, this.mPm, this.mInstallThread.getLooper(), sessionId, userId, installerPackageName, installerUid, params, createdMillis, stageDir, stageCid, prepared, sealed);
    }

    private void writeSessionsLocked() throws Throwable {
        FileOutputStream fos = null;
        try {
            fos = this.mSessionsFile.startWrite();
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_SESSIONS);
            int size = this.mSessions.size();
            for (int i = 0; i < size; i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                writeSessionLocked(fastXmlSerializer, session);
            }
            fastXmlSerializer.endTag(null, TAG_SESSIONS);
            fastXmlSerializer.endDocument();
            this.mSessionsFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mSessionsFile.failWrite(fos);
            }
        }
    }

    private void writeSessionLocked(XmlSerializer out, PackageInstallerSession session) throws Throwable {
        FileOutputStream os;
        PackageInstaller.SessionParams params = session.params;
        out.startTag(null, TAG_SESSION);
        XmlUtils.writeIntAttribute(out, ATTR_SESSION_ID, session.sessionId);
        XmlUtils.writeIntAttribute(out, ATTR_USER_ID, session.userId);
        XmlUtils.writeStringAttribute(out, ATTR_INSTALLER_PACKAGE_NAME, session.installerPackageName);
        XmlUtils.writeIntAttribute(out, ATTR_INSTALLER_UID, session.installerUid);
        XmlUtils.writeLongAttribute(out, ATTR_CREATED_MILLIS, session.createdMillis);
        if (session.stageDir != null) {
            XmlUtils.writeStringAttribute(out, ATTR_SESSION_STAGE_DIR, session.stageDir.getAbsolutePath());
        }
        if (session.stageCid != null) {
            XmlUtils.writeStringAttribute(out, ATTR_SESSION_STAGE_CID, session.stageCid);
        }
        XmlUtils.writeBooleanAttribute(out, ATTR_PREPARED, session.isPrepared());
        XmlUtils.writeBooleanAttribute(out, ATTR_SEALED, session.isSealed());
        XmlUtils.writeIntAttribute(out, ATTR_MODE, params.mode);
        XmlUtils.writeIntAttribute(out, ATTR_INSTALL_FLAGS, params.installFlags);
        XmlUtils.writeIntAttribute(out, ATTR_INSTALL_LOCATION, params.installLocation);
        XmlUtils.writeLongAttribute(out, ATTR_SIZE_BYTES, params.sizeBytes);
        XmlUtils.writeStringAttribute(out, ATTR_APP_PACKAGE_NAME, params.appPackageName);
        XmlUtils.writeStringAttribute(out, ATTR_APP_LABEL, params.appLabel);
        XmlUtils.writeUriAttribute(out, ATTR_ORIGINATING_URI, params.originatingUri);
        XmlUtils.writeUriAttribute(out, ATTR_REFERRER_URI, params.referrerUri);
        XmlUtils.writeStringAttribute(out, ATTR_ABI_OVERRIDE, params.abiOverride);
        File appIconFile = buildAppIconFile(session.sessionId);
        if (params.appIcon == null && appIconFile.exists()) {
            appIconFile.delete();
        } else if (params.appIcon != null && appIconFile.lastModified() != params.appIconLastModified) {
            FileOutputStream os2 = null;
            try {
                try {
                    os = new FileOutputStream(appIconFile);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e) {
                e = e;
            }
            try {
                params.appIcon.compress(Bitmap.CompressFormat.PNG, 90, os);
                IoUtils.closeQuietly(os);
                os2 = os;
            } catch (IOException e2) {
                e = e2;
                os2 = os;
                Slog.w(TAG, "Failed to write icon " + appIconFile + ": " + e.getMessage());
                IoUtils.closeQuietly(os2);
            } catch (Throwable th2) {
                th = th2;
                os2 = os;
                IoUtils.closeQuietly(os2);
                throw th;
            }
            params.appIconLastModified = appIconFile.lastModified();
        }
        out.endTag(null, TAG_SESSION);
    }

    private File buildAppIconFile(int sessionId) {
        return new File(this.mSessionsDir, "app_icon." + sessionId + ".png");
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

    public int createSession(PackageInstaller.SessionParams params, String installerPackageName, int userId) {
        try {
            return createSessionInternal(params, installerPackageName, userId);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private int createSessionInternal(PackageInstaller.SessionParams params, String installerPackageName, int userId) throws IOException {
        int sessionId;
        PackageInstallerSession session;
        int callingUid = Binder.getCallingUid();
        this.mPm.enforceCrossUserPermission(callingUid, userId, true, true, "createSession");
        if (this.mPm.isUserRestricted(userId, "no_install_apps")) {
            throw new SecurityException("User restriction prevents installing");
        }
        if (callingUid == 2000 || callingUid == 0) {
            params.installFlags |= 32;
        } else {
            this.mAppOps.checkPackage(callingUid, installerPackageName);
            params.installFlags &= -33;
            params.installFlags &= -65;
            params.installFlags |= 2;
        }
        if (params.appIcon != null) {
            ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
            int iconSize = am.getLauncherLargeIconSize();
            if (params.appIcon.getWidth() > iconSize * 2 || params.appIcon.getHeight() > iconSize * 2) {
                params.appIcon = Bitmap.createScaledBitmap(params.appIcon, iconSize, iconSize, true);
            }
        }
        if (params.mode == 1 || params.mode == 2) {
            long ident = Binder.clearCallingIdentity();
            try {
                int resolved = PackageHelper.resolveInstallLocation(this.mContext, params.appPackageName, params.installLocation, params.sizeBytes, params.installFlags);
                if (resolved == 1) {
                    params.setInstallFlagsInternal();
                } else if (resolved == 2) {
                    params.setInstallFlagsExternal();
                } else {
                    throw new IOException("No storage with enough free space; res=" + resolved);
                }
                Binder.restoreCallingIdentity(ident);
                synchronized (this.mSessions) {
                    int activeCount = getSessionCount(this.mSessions, callingUid);
                    if (activeCount >= MAX_ACTIVE_SESSIONS) {
                        throw new IllegalStateException("Too many active sessions for UID " + callingUid);
                    }
                    int historicalCount = getSessionCount(this.mHistoricalSessions, callingUid);
                    if (historicalCount >= MAX_HISTORICAL_SESSIONS) {
                        throw new IllegalStateException("Too many historical sessions for UID " + callingUid);
                    }
                    long createdMillis = System.currentTimeMillis();
                    sessionId = allocateSessionIdLocked();
                    File stageDir = null;
                    String stageCid = null;
                    if ((params.installFlags & 16) != 0) {
                        stageDir = buildInternalStageDir(sessionId);
                    } else {
                        stageCid = buildExternalStageCid(sessionId);
                    }
                    session = new PackageInstallerSession(this.mInternalCallback, this.mContext, this.mPm, this.mInstallThread.getLooper(), sessionId, userId, installerPackageName, callingUid, params, createdMillis, stageDir, stageCid, LOGD, LOGD);
                    this.mSessions.put(sessionId, session);
                }
                this.mCallbacks.notifySessionCreated(session.sessionId, session.userId);
                writeSessionsAsync();
                return sessionId;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
        throw new IllegalArgumentException("Invalid install mode: " + params.mode);
    }

    public void updateSessionAppIcon(int sessionId, Bitmap appIcon) {
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
            }
            if (appIcon != null) {
                ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
                int iconSize = am.getLauncherLargeIconSize();
                if (appIcon.getWidth() > iconSize * 2 || appIcon.getHeight() > iconSize * 2) {
                    appIcon = Bitmap.createScaledBitmap(appIcon, iconSize, iconSize, true);
                }
            }
            session.params.appIcon = appIcon;
            session.params.appIconLastModified = -1L;
            this.mInternalCallback.onSessionBadgingChanged(session);
        }
    }

    public void updateSessionAppLabel(int sessionId, String appLabel) {
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
            }
            session.params.appLabel = appLabel;
            this.mInternalCallback.onSessionBadgingChanged(session);
        }
    }

    public void abandonSession(int sessionId) {
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
            }
            session.abandon();
        }
    }

    public IPackageInstallerSession openSession(int sessionId) {
        try {
            return openSessionInternal(sessionId);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private IPackageInstallerSession openSessionInternal(int sessionId) throws IOException {
        PackageInstallerSession session;
        synchronized (this.mSessions) {
            session = this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
            }
            session.open();
        }
        return session;
    }

    private int allocateSessionIdLocked() {
        int n = 0;
        while (true) {
            int sessionId = this.mRandom.nextInt(2147483646) + 1;
            if (this.mSessions.get(sessionId) == null && this.mHistoricalSessions.get(sessionId) == null && !this.mLegacySessions.get(sessionId, LOGD)) {
                return sessionId;
            }
            int n2 = n + 1;
            if (n >= 32) {
                throw new IllegalStateException("Failed to allocate session ID");
            }
            n = n2;
        }
    }

    private File buildInternalStageDir(int sessionId) {
        return new File(this.mStagingDir, "vmdl" + sessionId + ".tmp");
    }

    static void prepareInternalStageDir(File stageDir) throws IOException {
        if (stageDir.exists()) {
            throw new IOException("Session dir already exists: " + stageDir);
        }
        try {
            Os.mkdir(stageDir.getAbsolutePath(), 493);
            Os.chmod(stageDir.getAbsolutePath(), 493);
            if (!SELinux.restorecon(stageDir)) {
                throw new IOException("Failed to restorecon session dir: " + stageDir);
            }
        } catch (ErrnoException e) {
            throw new IOException("Failed to prepare session dir: " + stageDir, e);
        }
    }

    private String buildExternalStageCid(int sessionId) {
        return "smdl" + sessionId + ".tmp";
    }

    static void prepareExternalStageCid(String stageCid, long sizeBytes) throws IOException {
        if (PackageHelper.createSdDir(sizeBytes, stageCid, PackageManagerService.getEncryptKey(), 1000, true) == null) {
            throw new IOException("Failed to create session cid: " + stageCid);
        }
    }

    public PackageInstaller.SessionInfo getSessionInfo(int sessionId) {
        PackageInstaller.SessionInfo sessionInfoGenerateInfo;
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            sessionInfoGenerateInfo = session != null ? session.generateInfo() : null;
        }
        return sessionInfoGenerateInfo;
    }

    public ParceledListSlice<PackageInstaller.SessionInfo> getAllSessions(int userId) {
        this.mPm.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, LOGD, "getAllSessions");
        List<PackageInstaller.SessionInfo> result = new ArrayList<>();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                if (session.userId == userId) {
                    result.add(session.generateInfo());
                }
            }
        }
        return new ParceledListSlice<>(result);
    }

    public ParceledListSlice<PackageInstaller.SessionInfo> getMySessions(String installerPackageName, int userId) {
        this.mPm.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, LOGD, "getMySessions");
        this.mAppOps.checkPackage(Binder.getCallingUid(), installerPackageName);
        List<PackageInstaller.SessionInfo> result = new ArrayList<>();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                if (Objects.equals(session.installerPackageName, installerPackageName) && session.userId == userId) {
                    result.add(session.generateInfo());
                }
            }
        }
        return new ParceledListSlice<>(result);
    }

    public void uninstall(String packageName, int flags, IntentSender statusReceiver, int userId) {
        this.mPm.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, true, "uninstall");
        PackageDeleteObserverAdapter adapter = new PackageDeleteObserverAdapter(this.mContext, statusReceiver, packageName);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DELETE_PACKAGES") == 0) {
            this.mPm.deletePackage(packageName, adapter.getBinder(), userId, flags);
            return;
        }
        Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
        intent.setData(Uri.fromParts("package", packageName, null));
        intent.putExtra("android.content.pm.extra.CALLBACK", adapter.getBinder().asBinder());
        adapter.onUserActionRequired(intent);
    }

    public void setPermissionsResult(int sessionId, boolean accepted) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", TAG);
        synchronized (this.mSessions) {
            this.mSessions.get(sessionId).setPermissionsResult(accepted);
        }
    }

    public void registerCallback(IPackageInstallerCallback callback, int userId) {
        this.mPm.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, LOGD, "registerCallback");
        this.mCallbacks.register(callback, userId);
    }

    public void unregisterCallback(IPackageInstallerCallback callback) {
        this.mCallbacks.unregister(callback);
    }

    private static int getSessionCount(SparseArray<PackageInstallerSession> sessions, int installerUid) {
        int count = 0;
        int size = sessions.size();
        for (int i = 0; i < size; i++) {
            PackageInstallerSession session = sessions.valueAt(i);
            if (session.installerUid == installerUid) {
                count++;
            }
        }
        return count;
    }

    private boolean isCallingUidOwner(PackageInstallerSession session) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0) {
            return true;
        }
        if (session == null || callingUid != session.installerUid) {
            return LOGD;
        }
        return true;
    }

    static class PackageDeleteObserverAdapter extends PackageDeleteObserver {
        private final Context mContext;
        private final String mPackageName;
        private final IntentSender mTarget;

        public PackageDeleteObserverAdapter(Context context, IntentSender target, String packageName) {
            this.mContext = context;
            this.mTarget = target;
            this.mPackageName = packageName;
        }

        public void onUserActionRequired(Intent intent) {
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.pm.extra.PACKAGE_NAME", this.mPackageName);
            fillIn.putExtra("android.content.pm.extra.STATUS", -1);
            fillIn.putExtra("android.intent.extra.INTENT", intent);
            try {
                this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null);
            } catch (IntentSender.SendIntentException e) {
            }
        }

        public void onPackageDeleted(String basePackageName, int returnCode, String msg) {
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.pm.extra.PACKAGE_NAME", this.mPackageName);
            fillIn.putExtra("android.content.pm.extra.STATUS", PackageManager.deleteStatusToPublicStatus(returnCode));
            fillIn.putExtra("android.content.pm.extra.STATUS_MESSAGE", PackageManager.deleteStatusToString(returnCode, msg));
            fillIn.putExtra("android.content.pm.extra.LEGACY_STATUS", returnCode);
            try {
                this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null);
            } catch (IntentSender.SendIntentException e) {
            }
        }
    }

    static class PackageInstallObserverAdapter extends PackageInstallObserver {
        private final Context mContext;
        private final int mSessionId;
        private final IntentSender mTarget;

        public PackageInstallObserverAdapter(Context context, IntentSender target, int sessionId) {
            this.mContext = context;
            this.mTarget = target;
            this.mSessionId = sessionId;
        }

        public void onUserActionRequired(Intent intent) {
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.pm.extra.SESSION_ID", this.mSessionId);
            fillIn.putExtra("android.content.pm.extra.STATUS", -1);
            fillIn.putExtra("android.intent.extra.INTENT", intent);
            try {
                this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null);
            } catch (IntentSender.SendIntentException e) {
            }
        }

        public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) {
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.pm.extra.SESSION_ID", this.mSessionId);
            fillIn.putExtra("android.content.pm.extra.STATUS", PackageManager.installStatusToPublicStatus(returnCode));
            fillIn.putExtra("android.content.pm.extra.STATUS_MESSAGE", PackageManager.installStatusToString(returnCode, msg));
            fillIn.putExtra("android.content.pm.extra.LEGACY_STATUS", returnCode);
            if (extras != null) {
                String existing = extras.getString("android.content.pm.extra.FAILURE_EXISTING_PACKAGE");
                if (!TextUtils.isEmpty(existing)) {
                    fillIn.putExtra("android.content.pm.extra.OTHER_PACKAGE_NAME", existing);
                }
            }
            try {
                this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null);
            } catch (IntentSender.SendIntentException e) {
            }
        }
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

        public void register(IPackageInstallerCallback callback, int userId) {
            this.mCallbacks.register(callback, new UserHandle(userId));
        }

        public void unregister(IPackageInstallerCallback callback) {
            this.mCallbacks.unregister(callback);
        }

        @Override
        public void handleMessage(Message msg) {
            int userId = msg.arg2;
            int n = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                IPackageInstallerCallback callback = (IPackageInstallerCallback) this.mCallbacks.getBroadcastItem(i);
                UserHandle user = (UserHandle) this.mCallbacks.getBroadcastCookie(i);
                if (userId == user.getIdentifier()) {
                    try {
                        invokeCallback(callback, msg);
                    } catch (RemoteException e) {
                    }
                }
            }
            this.mCallbacks.finishBroadcast();
        }

        private void invokeCallback(IPackageInstallerCallback callback, Message msg) throws RemoteException {
            int sessionId = msg.arg1;
            switch (msg.what) {
                case 1:
                    callback.onSessionCreated(sessionId);
                    break;
                case 2:
                    callback.onSessionBadgingChanged(sessionId);
                    break;
                case 3:
                    callback.onSessionActiveChanged(sessionId, ((Boolean) msg.obj).booleanValue());
                    break;
                case 4:
                    callback.onSessionProgressChanged(sessionId, ((Float) msg.obj).floatValue());
                    break;
                case 5:
                    callback.onSessionFinished(sessionId, ((Boolean) msg.obj).booleanValue());
                    break;
            }
        }

        private void notifySessionCreated(int sessionId, int userId) {
            obtainMessage(1, sessionId, userId).sendToTarget();
        }

        private void notifySessionBadgingChanged(int sessionId, int userId) {
            obtainMessage(2, sessionId, userId).sendToTarget();
        }

        private void notifySessionActiveChanged(int sessionId, int userId, boolean active) {
            obtainMessage(3, sessionId, userId, Boolean.valueOf(active)).sendToTarget();
        }

        private void notifySessionProgressChanged(int sessionId, int userId, float progress) {
            obtainMessage(4, sessionId, userId, Float.valueOf(progress)).sendToTarget();
        }

        public void notifySessionFinished(int sessionId, int userId, boolean success) {
            obtainMessage(5, sessionId, userId, Boolean.valueOf(success)).sendToTarget();
        }
    }

    void dump(IndentingPrintWriter pw) {
        synchronized (this.mSessions) {
            pw.println("Active install sessions:");
            pw.increaseIndent();
            int N = this.mSessions.size();
            for (int i = 0; i < N; i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                session.dump(pw);
                pw.println();
            }
            pw.println();
            pw.decreaseIndent();
            pw.println("Historical install sessions:");
            pw.increaseIndent();
            int N2 = this.mHistoricalSessions.size();
            for (int i2 = 0; i2 < N2; i2++) {
                PackageInstallerSession session2 = this.mHistoricalSessions.valueAt(i2);
                session2.dump(pw);
                pw.println();
            }
            pw.println();
            pw.decreaseIndent();
            pw.println("Legacy install sessions:");
            pw.increaseIndent();
            pw.println(this.mLegacySessions.toString());
            pw.decreaseIndent();
        }
    }

    class InternalCallback {
        InternalCallback() {
        }

        public void onSessionBadgingChanged(PackageInstallerSession session) {
            PackageInstallerService.this.mCallbacks.notifySessionBadgingChanged(session.sessionId, session.userId);
            PackageInstallerService.this.writeSessionsAsync();
        }

        public void onSessionActiveChanged(PackageInstallerSession session, boolean active) {
            PackageInstallerService.this.mCallbacks.notifySessionActiveChanged(session.sessionId, session.userId, active);
        }

        public void onSessionProgressChanged(PackageInstallerSession session, float progress) {
            PackageInstallerService.this.mCallbacks.notifySessionProgressChanged(session.sessionId, session.userId, progress);
        }

        public void onSessionFinished(final PackageInstallerSession session, boolean success) {
            PackageInstallerService.this.mCallbacks.notifySessionFinished(session.sessionId, session.userId, success);
            PackageInstallerService.this.mInstallHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (PackageInstallerService.this.mSessions) {
                        PackageInstallerService.this.mSessions.remove(session.sessionId);
                        PackageInstallerService.this.mHistoricalSessions.put(session.sessionId, session);
                        File appIconFile = PackageInstallerService.this.buildAppIconFile(session.sessionId);
                        if (appIconFile.exists()) {
                            appIconFile.delete();
                        }
                        PackageInstallerService.this.writeSessionsLocked();
                    }
                }
            });
        }

        public void onSessionPrepared(PackageInstallerSession session) {
            PackageInstallerService.this.writeSessionsAsync();
        }

        public void onSessionSealedBlocking(PackageInstallerSession session) {
            synchronized (PackageInstallerService.this.mSessions) {
                PackageInstallerService.this.writeSessionsLocked();
            }
        }
    }
}
