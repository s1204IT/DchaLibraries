package android.content.pm;

import android.annotation.SystemApi;
import android.app.AppGlobals;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IPackageInstallerCallback;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.FileBridge;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.ParcelableException;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.util.ExceptionUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PackageInstaller {
    public static final String ACTION_CONFIRM_PERMISSIONS = "android.content.pm.action.CONFIRM_PERMISSIONS";
    public static final String ACTION_SESSION_COMMITTED = "android.content.pm.action.SESSION_COMMITTED";
    public static final String ACTION_SESSION_DETAILS = "android.content.pm.action.SESSION_DETAILS";
    public static final boolean ENABLE_REVOCABLE_FD = SystemProperties.getBoolean("fw.revocable_fd", false);
    public static final String EXTRA_CALLBACK = "android.content.pm.extra.CALLBACK";
    public static final String EXTRA_LEGACY_BUNDLE = "android.content.pm.extra.LEGACY_BUNDLE";
    public static final String EXTRA_LEGACY_STATUS = "android.content.pm.extra.LEGACY_STATUS";
    public static final String EXTRA_OTHER_PACKAGE_NAME = "android.content.pm.extra.OTHER_PACKAGE_NAME";
    public static final String EXTRA_PACKAGE_NAME = "android.content.pm.extra.PACKAGE_NAME";

    @Deprecated
    public static final String EXTRA_PACKAGE_NAMES = "android.content.pm.extra.PACKAGE_NAMES";
    public static final String EXTRA_SESSION = "android.content.pm.extra.SESSION";
    public static final String EXTRA_SESSION_ID = "android.content.pm.extra.SESSION_ID";
    public static final String EXTRA_STATUS = "android.content.pm.extra.STATUS";
    public static final String EXTRA_STATUS_MESSAGE = "android.content.pm.extra.STATUS_MESSAGE";
    public static final String EXTRA_STORAGE_PATH = "android.content.pm.extra.STORAGE_PATH";
    public static final int STATUS_FAILURE = 1;
    public static final int STATUS_FAILURE_ABORTED = 3;
    public static final int STATUS_FAILURE_BLOCKED = 2;
    public static final int STATUS_FAILURE_CONFLICT = 5;
    public static final int STATUS_FAILURE_INCOMPATIBLE = 7;
    public static final int STATUS_FAILURE_INVALID = 4;
    public static final int STATUS_FAILURE_STORAGE = 6;
    public static final int STATUS_PENDING_USER_ACTION = -1;
    public static final int STATUS_SUCCESS = 0;
    private static final String TAG = "PackageInstaller";
    private final ArrayList<SessionCallbackDelegate> mDelegates = new ArrayList<>();
    private final IPackageInstaller mInstaller;
    private final String mInstallerPackageName;
    private final int mUserId;

    public static abstract class SessionCallback {
        public abstract void onActiveChanged(int i, boolean z);

        public abstract void onBadgingChanged(int i);

        public abstract void onCreated(int i);

        public abstract void onFinished(int i, boolean z);

        public abstract void onProgressChanged(int i, float f);
    }

    public PackageInstaller(IPackageInstaller iPackageInstaller, String str, int i) {
        this.mInstaller = iPackageInstaller;
        this.mInstallerPackageName = str;
        this.mUserId = i;
    }

    public int createSession(SessionParams sessionParams) throws Throwable {
        String str;
        try {
            if (sessionParams.installerPackageName == null) {
                str = this.mInstallerPackageName;
            } else {
                str = sessionParams.installerPackageName;
            }
            return this.mInstaller.createSession(sessionParams, str, this.mUserId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (RuntimeException e2) {
            ExceptionUtils.maybeUnwrapIOException(e2);
            throw e2;
        }
    }

    public Session openSession(int i) throws Throwable {
        try {
            return new Session(this.mInstaller.openSession(i));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (RuntimeException e2) {
            ExceptionUtils.maybeUnwrapIOException(e2);
            throw e2;
        }
    }

    public void updateSessionAppIcon(int i, Bitmap bitmap) {
        try {
            this.mInstaller.updateSessionAppIcon(i, bitmap);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updateSessionAppLabel(int i, CharSequence charSequence) {
        String string;
        if (charSequence == null) {
            string = null;
        } else {
            try {
                string = charSequence.toString();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        this.mInstaller.updateSessionAppLabel(i, string);
    }

    public void abandonSession(int i) {
        try {
            this.mInstaller.abandonSession(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public SessionInfo getSessionInfo(int i) {
        try {
            return this.mInstaller.getSessionInfo(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<SessionInfo> getAllSessions() {
        try {
            return this.mInstaller.getAllSessions(this.mUserId).getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<SessionInfo> getMySessions() {
        try {
            return this.mInstaller.getMySessions(this.mInstallerPackageName, this.mUserId).getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void uninstall(String str, IntentSender intentSender) {
        uninstall(str, 0, intentSender);
    }

    public void uninstall(String str, int i, IntentSender intentSender) {
        uninstall(new VersionedPackage(str, -1), i, intentSender);
    }

    public void uninstall(VersionedPackage versionedPackage, IntentSender intentSender) {
        uninstall(versionedPackage, 0, intentSender);
    }

    public void uninstall(VersionedPackage versionedPackage, int i, IntentSender intentSender) {
        Preconditions.checkNotNull(versionedPackage, "versionedPackage cannot be null");
        try {
            this.mInstaller.uninstall(versionedPackage, this.mInstallerPackageName, i, intentSender, this.mUserId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setPermissionsResult(int i, boolean z) {
        try {
            this.mInstaller.setPermissionsResult(i, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static class SessionCallbackDelegate extends IPackageInstallerCallback.Stub implements Handler.Callback {
        private static final int MSG_SESSION_ACTIVE_CHANGED = 3;
        private static final int MSG_SESSION_BADGING_CHANGED = 2;
        private static final int MSG_SESSION_CREATED = 1;
        private static final int MSG_SESSION_FINISHED = 5;
        private static final int MSG_SESSION_PROGRESS_CHANGED = 4;
        final SessionCallback mCallback;
        final Handler mHandler;

        public SessionCallbackDelegate(SessionCallback sessionCallback, Looper looper) {
            this.mCallback = sessionCallback;
            this.mHandler = new Handler(looper, this);
        }

        @Override
        public boolean handleMessage(Message message) {
            int i = message.arg1;
            switch (message.what) {
                case 1:
                    this.mCallback.onCreated(i);
                    return true;
                case 2:
                    this.mCallback.onBadgingChanged(i);
                    return true;
                case 3:
                    this.mCallback.onActiveChanged(i, message.arg2 != 0);
                    return true;
                case 4:
                    this.mCallback.onProgressChanged(i, ((Float) message.obj).floatValue());
                    return true;
                case 5:
                    this.mCallback.onFinished(i, message.arg2 != 0);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onSessionCreated(int i) {
            this.mHandler.obtainMessage(1, i, 0).sendToTarget();
        }

        @Override
        public void onSessionBadgingChanged(int i) {
            this.mHandler.obtainMessage(2, i, 0).sendToTarget();
        }

        @Override
        public void onSessionActiveChanged(int i, boolean z) {
            this.mHandler.obtainMessage(3, i, z ? 1 : 0).sendToTarget();
        }

        @Override
        public void onSessionProgressChanged(int i, float f) {
            this.mHandler.obtainMessage(4, i, 0, Float.valueOf(f)).sendToTarget();
        }

        @Override
        public void onSessionFinished(int i, boolean z) {
            this.mHandler.obtainMessage(5, i, z ? 1 : 0).sendToTarget();
        }
    }

    @Deprecated
    public void addSessionCallback(SessionCallback sessionCallback) {
        registerSessionCallback(sessionCallback);
    }

    public void registerSessionCallback(SessionCallback sessionCallback) {
        registerSessionCallback(sessionCallback, new Handler());
    }

    @Deprecated
    public void addSessionCallback(SessionCallback sessionCallback, Handler handler) {
        registerSessionCallback(sessionCallback, handler);
    }

    public void registerSessionCallback(SessionCallback sessionCallback, Handler handler) {
        synchronized (this.mDelegates) {
            SessionCallbackDelegate sessionCallbackDelegate = new SessionCallbackDelegate(sessionCallback, handler.getLooper());
            try {
                this.mInstaller.registerCallback(sessionCallbackDelegate, this.mUserId);
                this.mDelegates.add(sessionCallbackDelegate);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @Deprecated
    public void removeSessionCallback(SessionCallback sessionCallback) {
        unregisterSessionCallback(sessionCallback);
    }

    public void unregisterSessionCallback(SessionCallback sessionCallback) {
        synchronized (this.mDelegates) {
            Iterator<SessionCallbackDelegate> it = this.mDelegates.iterator();
            while (it.hasNext()) {
                SessionCallbackDelegate next = it.next();
                if (next.mCallback == sessionCallback) {
                    try {
                        this.mInstaller.unregisterCallback(next);
                        it.remove();
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
        }
    }

    public static class Session implements Closeable {
        private IPackageInstallerSession mSession;

        public Session(IPackageInstallerSession iPackageInstallerSession) {
            this.mSession = iPackageInstallerSession;
        }

        @Deprecated
        public void setProgress(float f) {
            setStagingProgress(f);
        }

        public void setStagingProgress(float f) {
            try {
                this.mSession.setClientProgress(f);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public void addProgress(float f) {
            try {
                this.mSession.addClientProgress(f);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public OutputStream openWrite(String str, long j, long j2) throws Throwable {
            try {
                if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                    return new ParcelFileDescriptor.AutoCloseOutputStream(this.mSession.openWrite(str, j, j2));
                }
                return new FileBridge.FileBridgeOutputStream(this.mSession.openWrite(str, j, j2));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e2) {
                ExceptionUtils.maybeUnwrapIOException(e2);
                throw e2;
            }
        }

        public void write(String str, long j, long j2, ParcelFileDescriptor parcelFileDescriptor) throws Throwable {
            try {
                this.mSession.write(str, j, j2, parcelFileDescriptor);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e2) {
                ExceptionUtils.maybeUnwrapIOException(e2);
                throw e2;
            }
        }

        public void fsync(OutputStream outputStream) throws IOException {
            if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                if (outputStream instanceof ParcelFileDescriptor.AutoCloseOutputStream) {
                    try {
                        Os.fsync(((ParcelFileDescriptor.AutoCloseOutputStream) outputStream).getFD());
                        return;
                    } catch (ErrnoException e) {
                        throw e.rethrowAsIOException();
                    }
                }
                throw new IllegalArgumentException("Unrecognized stream");
            }
            if (outputStream instanceof FileBridge.FileBridgeOutputStream) {
                ((FileBridge.FileBridgeOutputStream) outputStream).fsync();
                return;
            }
            throw new IllegalArgumentException("Unrecognized stream");
        }

        public String[] getNames() throws Throwable {
            try {
                return this.mSession.getNames();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e2) {
                ExceptionUtils.maybeUnwrapIOException(e2);
                throw e2;
            }
        }

        public InputStream openRead(String str) throws Throwable {
            try {
                return new ParcelFileDescriptor.AutoCloseInputStream(this.mSession.openRead(str));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e2) {
                ExceptionUtils.maybeUnwrapIOException(e2);
                throw e2;
            }
        }

        public void removeSplit(String str) throws Throwable {
            try {
                this.mSession.removeSplit(str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (RuntimeException e2) {
                ExceptionUtils.maybeUnwrapIOException(e2);
                throw e2;
            }
        }

        public void commit(IntentSender intentSender) {
            try {
                this.mSession.commit(intentSender, false);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @SystemApi
        public void commitTransferred(IntentSender intentSender) {
            try {
                this.mSession.commit(intentSender, true);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public void transfer(String str) throws Throwable {
            Preconditions.checkNotNull(str);
            try {
                this.mSession.transfer(str);
            } catch (ParcelableException e) {
                e.maybeRethrow(PackageManager.NameNotFoundException.class);
                throw new RuntimeException(e);
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }

        @Override
        public void close() {
            try {
                this.mSession.close();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public void abandon() {
            try {
                this.mSession.abandon();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public static class SessionParams implements Parcelable {
        public static final Parcelable.Creator<SessionParams> CREATOR = new Parcelable.Creator<SessionParams>() {
            @Override
            public SessionParams createFromParcel(Parcel parcel) {
                return new SessionParams(parcel);
            }

            @Override
            public SessionParams[] newArray(int i) {
                return new SessionParams[i];
            }
        };
        public static final int MODE_FULL_INSTALL = 1;
        public static final int MODE_INHERIT_EXISTING = 2;
        public static final int MODE_INVALID = -1;
        public static final int UID_UNKNOWN = -1;
        public String abiOverride;
        public Bitmap appIcon;
        public long appIconLastModified;
        public String appLabel;
        public String appPackageName;
        public String[] grantedRuntimePermissions;
        public int installFlags;
        public int installLocation;
        public int installReason;
        public String installerPackageName;
        public int mode;
        public int originatingUid;
        public Uri originatingUri;
        public Uri referrerUri;
        public long sizeBytes;
        public String volumeUuid;

        public SessionParams(int i) {
            this.mode = -1;
            this.installLocation = 1;
            this.installReason = 0;
            this.sizeBytes = -1L;
            this.appIconLastModified = -1L;
            this.originatingUid = -1;
            this.mode = i;
        }

        public SessionParams(Parcel parcel) {
            this.mode = -1;
            this.installLocation = 1;
            this.installReason = 0;
            this.sizeBytes = -1L;
            this.appIconLastModified = -1L;
            this.originatingUid = -1;
            this.mode = parcel.readInt();
            this.installFlags = parcel.readInt();
            this.installLocation = parcel.readInt();
            this.installReason = parcel.readInt();
            this.sizeBytes = parcel.readLong();
            this.appPackageName = parcel.readString();
            this.appIcon = (Bitmap) parcel.readParcelable(null);
            this.appLabel = parcel.readString();
            this.originatingUri = (Uri) parcel.readParcelable(null);
            this.originatingUid = parcel.readInt();
            this.referrerUri = (Uri) parcel.readParcelable(null);
            this.abiOverride = parcel.readString();
            this.volumeUuid = parcel.readString();
            this.grantedRuntimePermissions = parcel.readStringArray();
            this.installerPackageName = parcel.readString();
        }

        public boolean areHiddenOptionsSet() {
            return ((this.installFlags & 120960) == this.installFlags && this.abiOverride == null && this.volumeUuid == null) ? false : true;
        }

        public void setInstallLocation(int i) {
            this.installLocation = i;
        }

        public void setSize(long j) {
            this.sizeBytes = j;
        }

        public void setAppPackageName(String str) {
            this.appPackageName = str;
        }

        public void setAppIcon(Bitmap bitmap) {
            this.appIcon = bitmap;
        }

        public void setAppLabel(CharSequence charSequence) {
            this.appLabel = charSequence != null ? charSequence.toString() : null;
        }

        public void setOriginatingUri(Uri uri) {
            this.originatingUri = uri;
        }

        public void setOriginatingUid(int i) {
            this.originatingUid = i;
        }

        public void setReferrerUri(Uri uri) {
            this.referrerUri = uri;
        }

        @SystemApi
        public void setGrantedRuntimePermissions(String[] strArr) {
            this.installFlags |= 256;
            this.grantedRuntimePermissions = strArr;
        }

        public void setInstallFlagsInternal() {
            this.installFlags |= 16;
            this.installFlags &= -9;
        }

        @SystemApi
        public void setAllowDowngrade(boolean z) {
            if (z) {
                this.installFlags |= 128;
            } else {
                this.installFlags &= -129;
            }
        }

        public void setInstallFlagsExternal() {
            this.installFlags |= 8;
            this.installFlags &= -17;
        }

        public void setInstallFlagsForcePermissionPrompt() {
            this.installFlags |= 1024;
        }

        @SystemApi
        public void setDontKillApp(boolean z) {
            if (z) {
                this.installFlags |= 4096;
            } else {
                this.installFlags &= -4097;
            }
        }

        @SystemApi
        public void setInstallAsInstantApp(boolean z) {
            if (z) {
                this.installFlags |= 2048;
                this.installFlags &= -16385;
            } else {
                this.installFlags &= -2049;
                this.installFlags |= 16384;
            }
        }

        @SystemApi
        public void setInstallAsVirtualPreload() {
            this.installFlags |= 65536;
        }

        public void setInstallReason(int i) {
            this.installReason = i;
        }

        @SystemApi
        public void setAllocateAggressive(boolean z) {
            if (z) {
                this.installFlags |= 32768;
            } else {
                this.installFlags &= -32769;
            }
        }

        public void setInstallerPackageName(String str) {
            this.installerPackageName = str;
        }

        public void dump(IndentingPrintWriter indentingPrintWriter) {
            indentingPrintWriter.printPair("mode", Integer.valueOf(this.mode));
            indentingPrintWriter.printHexPair("installFlags", this.installFlags);
            indentingPrintWriter.printPair("installLocation", Integer.valueOf(this.installLocation));
            indentingPrintWriter.printPair("sizeBytes", Long.valueOf(this.sizeBytes));
            indentingPrintWriter.printPair("appPackageName", this.appPackageName);
            indentingPrintWriter.printPair("appIcon", Boolean.valueOf(this.appIcon != null));
            indentingPrintWriter.printPair("appLabel", this.appLabel);
            indentingPrintWriter.printPair("originatingUri", this.originatingUri);
            indentingPrintWriter.printPair("originatingUid", Integer.valueOf(this.originatingUid));
            indentingPrintWriter.printPair("referrerUri", this.referrerUri);
            indentingPrintWriter.printPair("abiOverride", this.abiOverride);
            indentingPrintWriter.printPair("volumeUuid", this.volumeUuid);
            indentingPrintWriter.printPair("grantedRuntimePermissions", (Object[]) this.grantedRuntimePermissions);
            indentingPrintWriter.printPair("installerPackageName", this.installerPackageName);
            indentingPrintWriter.println();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mode);
            parcel.writeInt(this.installFlags);
            parcel.writeInt(this.installLocation);
            parcel.writeInt(this.installReason);
            parcel.writeLong(this.sizeBytes);
            parcel.writeString(this.appPackageName);
            parcel.writeParcelable(this.appIcon, i);
            parcel.writeString(this.appLabel);
            parcel.writeParcelable(this.originatingUri, i);
            parcel.writeInt(this.originatingUid);
            parcel.writeParcelable(this.referrerUri, i);
            parcel.writeString(this.abiOverride);
            parcel.writeString(this.volumeUuid);
            parcel.writeStringArray(this.grantedRuntimePermissions);
            parcel.writeString(this.installerPackageName);
        }
    }

    public static class SessionInfo implements Parcelable {
        public static final Parcelable.Creator<SessionInfo> CREATOR = new Parcelable.Creator<SessionInfo>() {
            @Override
            public SessionInfo createFromParcel(Parcel parcel) {
                return new SessionInfo(parcel);
            }

            @Override
            public SessionInfo[] newArray(int i) {
                return new SessionInfo[i];
            }
        };
        public boolean active;
        public Bitmap appIcon;
        public CharSequence appLabel;
        public String appPackageName;
        public String[] grantedRuntimePermissions;
        public int installFlags;
        public int installLocation;
        public int installReason;
        public String installerPackageName;
        public int mode;
        public int originatingUid;
        public Uri originatingUri;
        public float progress;
        public Uri referrerUri;
        public String resolvedBaseCodePath;
        public boolean sealed;
        public int sessionId;
        public long sizeBytes;

        public SessionInfo() {
        }

        public SessionInfo(Parcel parcel) {
            this.sessionId = parcel.readInt();
            this.installerPackageName = parcel.readString();
            this.resolvedBaseCodePath = parcel.readString();
            this.progress = parcel.readFloat();
            this.sealed = parcel.readInt() != 0;
            this.active = parcel.readInt() != 0;
            this.mode = parcel.readInt();
            this.installReason = parcel.readInt();
            this.sizeBytes = parcel.readLong();
            this.appPackageName = parcel.readString();
            this.appIcon = (Bitmap) parcel.readParcelable(null);
            this.appLabel = parcel.readString();
            this.installLocation = parcel.readInt();
            this.originatingUri = (Uri) parcel.readParcelable(null);
            this.originatingUid = parcel.readInt();
            this.referrerUri = (Uri) parcel.readParcelable(null);
            this.grantedRuntimePermissions = parcel.readStringArray();
            this.installFlags = parcel.readInt();
        }

        public int getSessionId() {
            return this.sessionId;
        }

        public String getInstallerPackageName() {
            return this.installerPackageName;
        }

        public float getProgress() {
            return this.progress;
        }

        public boolean isActive() {
            return this.active;
        }

        public boolean isSealed() {
            return this.sealed;
        }

        public int getInstallReason() {
            return this.installReason;
        }

        @Deprecated
        public boolean isOpen() {
            return isActive();
        }

        public String getAppPackageName() {
            return this.appPackageName;
        }

        public Bitmap getAppIcon() {
            if (this.appIcon == null) {
                try {
                    SessionInfo sessionInfo = AppGlobals.getPackageManager().getPackageInstaller().getSessionInfo(this.sessionId);
                    this.appIcon = sessionInfo != null ? sessionInfo.appIcon : null;
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            return this.appIcon;
        }

        public CharSequence getAppLabel() {
            return this.appLabel;
        }

        public Intent createDetailsIntent() {
            Intent intent = new Intent(PackageInstaller.ACTION_SESSION_DETAILS);
            intent.putExtra(PackageInstaller.EXTRA_SESSION_ID, this.sessionId);
            intent.setPackage(this.installerPackageName);
            intent.setFlags(268435456);
            return intent;
        }

        public int getMode() {
            return this.mode;
        }

        public int getInstallLocation() {
            return this.installLocation;
        }

        public long getSize() {
            return this.sizeBytes;
        }

        public Uri getOriginatingUri() {
            return this.originatingUri;
        }

        public int getOriginatingUid() {
            return this.originatingUid;
        }

        public Uri getReferrerUri() {
            return this.referrerUri;
        }

        @SystemApi
        public String[] getGrantedRuntimePermissions() {
            return this.grantedRuntimePermissions;
        }

        @SystemApi
        public boolean getAllowDowngrade() {
            return (this.installFlags & 128) != 0;
        }

        @SystemApi
        public boolean getDontKillApp() {
            return (this.installFlags & 4096) != 0;
        }

        @SystemApi
        public boolean getInstallAsInstantApp(boolean z) {
            return (this.installFlags & 2048) != 0;
        }

        @SystemApi
        public boolean getInstallAsFullApp(boolean z) {
            return (this.installFlags & 16384) != 0;
        }

        @SystemApi
        public boolean getInstallAsVirtualPreload() {
            return (this.installFlags & 65536) != 0;
        }

        @SystemApi
        public boolean getAllocateAggressive() {
            return (this.installFlags & 32768) != 0;
        }

        @Deprecated
        public Intent getDetailsIntent() {
            return createDetailsIntent();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.sessionId);
            parcel.writeString(this.installerPackageName);
            parcel.writeString(this.resolvedBaseCodePath);
            parcel.writeFloat(this.progress);
            parcel.writeInt(this.sealed ? 1 : 0);
            parcel.writeInt(this.active ? 1 : 0);
            parcel.writeInt(this.mode);
            parcel.writeInt(this.installReason);
            parcel.writeLong(this.sizeBytes);
            parcel.writeString(this.appPackageName);
            parcel.writeParcelable(this.appIcon, i);
            parcel.writeString(this.appLabel != null ? this.appLabel.toString() : null);
            parcel.writeInt(this.installLocation);
            parcel.writeParcelable(this.originatingUri, i);
            parcel.writeInt(this.originatingUid);
            parcel.writeParcelable(this.referrerUri, i);
            parcel.writeStringArray(this.grantedRuntimePermissions);
            parcel.writeInt(this.installFlags);
        }
    }
}
