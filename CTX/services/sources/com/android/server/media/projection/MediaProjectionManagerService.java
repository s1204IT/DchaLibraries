package com.android.server.media.projection;

import android.app.AppOpsManager;
import android.content.Context;
import android.media.MediaRouter;
import android.media.projection.IMediaProjection;
import android.media.projection.IMediaProjectionCallback;
import android.media.projection.IMediaProjectionManager;
import android.media.projection.IMediaProjectionWatcherCallback;
import android.media.projection.MediaProjectionInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

public final class MediaProjectionManagerService extends SystemService implements Watchdog.Monitor {
    private static final String TAG = "MediaProjectionManagerService";
    private final AppOpsManager mAppOps;
    private final CallbackDelegate mCallbackDelegate;
    private final Context mContext;
    private final Map<IBinder, IBinder.DeathRecipient> mDeathEaters;
    private final Object mLock;
    private MediaRouter.RouteInfo mMediaRouteInfo;
    private final MediaRouter mMediaRouter;
    private final MediaRouterCallback mMediaRouterCallback;
    private MediaProjection mProjectionGrant;
    private IBinder mProjectionToken;

    public MediaProjectionManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mContext = context;
        this.mDeathEaters = new ArrayMap();
        this.mCallbackDelegate = new CallbackDelegate();
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mMediaRouter = (MediaRouter) this.mContext.getSystemService("media_router");
        this.mMediaRouterCallback = new MediaRouterCallback();
        Watchdog.getInstance().addMonitor(this);
    }

    @Override
    public void onStart() {
        publishBinderService("media_projection", new BinderService(), false);
        this.mMediaRouter.addCallback(4, this.mMediaRouterCallback, 8);
    }

    @Override
    public void onSwitchUser(int i) {
        this.mMediaRouter.rebindAsUser(i);
        synchronized (this.mLock) {
            if (this.mProjectionGrant != null) {
                this.mProjectionGrant.stop();
            }
        }
    }

    @Override
    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    private void startProjectionLocked(MediaProjection mediaProjection) {
        if (this.mProjectionGrant != null) {
            this.mProjectionGrant.stop();
        }
        if (this.mMediaRouteInfo != null) {
            this.mMediaRouter.getFallbackRoute().select();
        }
        this.mProjectionToken = mediaProjection.asBinder();
        this.mProjectionGrant = mediaProjection;
        dispatchStart(mediaProjection);
    }

    private void stopProjectionLocked(MediaProjection mediaProjection) {
        this.mProjectionToken = null;
        this.mProjectionGrant = null;
        dispatchStop(mediaProjection);
    }

    private void addCallback(final IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback) {
        IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                MediaProjectionManagerService.this.removeCallback(iMediaProjectionWatcherCallback);
            }
        };
        synchronized (this.mLock) {
            this.mCallbackDelegate.add(iMediaProjectionWatcherCallback);
            linkDeathRecipientLocked(iMediaProjectionWatcherCallback, deathRecipient);
        }
    }

    private void removeCallback(IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback) {
        synchronized (this.mLock) {
            unlinkDeathRecipientLocked(iMediaProjectionWatcherCallback);
            this.mCallbackDelegate.remove(iMediaProjectionWatcherCallback);
        }
    }

    private void linkDeathRecipientLocked(IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback, IBinder.DeathRecipient deathRecipient) {
        try {
            IBinder iBinderAsBinder = iMediaProjectionWatcherCallback.asBinder();
            iBinderAsBinder.linkToDeath(deathRecipient, 0);
            this.mDeathEaters.put(iBinderAsBinder, deathRecipient);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to link to death for media projection monitoring callback", e);
        }
    }

    private void unlinkDeathRecipientLocked(IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback) {
        IBinder iBinderAsBinder = iMediaProjectionWatcherCallback.asBinder();
        IBinder.DeathRecipient deathRecipientRemove = this.mDeathEaters.remove(iBinderAsBinder);
        if (deathRecipientRemove != null) {
            iBinderAsBinder.unlinkToDeath(deathRecipientRemove, 0);
        }
    }

    private void dispatchStart(MediaProjection mediaProjection) {
        this.mCallbackDelegate.dispatchStart(mediaProjection);
    }

    private void dispatchStop(MediaProjection mediaProjection) {
        this.mCallbackDelegate.dispatchStop(mediaProjection);
    }

    private boolean isValidMediaProjection(IBinder iBinder) {
        synchronized (this.mLock) {
            if (this.mProjectionToken != null) {
                return this.mProjectionToken.equals(iBinder);
            }
            return false;
        }
    }

    private MediaProjectionInfo getActiveProjectionInfo() {
        synchronized (this.mLock) {
            if (this.mProjectionGrant == null) {
                return null;
            }
            return this.mProjectionGrant.getProjectionInfo();
        }
    }

    private void dump(PrintWriter printWriter) {
        printWriter.println("MEDIA PROJECTION MANAGER (dumpsys media_projection)");
        synchronized (this.mLock) {
            printWriter.println("Media Projection: ");
            if (this.mProjectionGrant != null) {
                this.mProjectionGrant.dump(printWriter);
            } else {
                printWriter.println("null");
            }
        }
    }

    private final class BinderService extends IMediaProjectionManager.Stub {
        private BinderService() {
        }

        public boolean hasProjectionPermission(int i, String str) {
            boolean z;
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (!checkPermission(str, "android.permission.CAPTURE_VIDEO_OUTPUT")) {
                    z = MediaProjectionManagerService.this.mAppOps.noteOpNoThrow(46, i, str) == 0;
                }
                return z | false;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public IMediaProjection createProjection(int i, String str, int i2, boolean z) {
            if (MediaProjectionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_MEDIA_PROJECTION") != 0) {
                throw new SecurityException("Requires MANAGE_MEDIA_PROJECTION in order to grant projection permission");
            }
            if (str == null || str.isEmpty()) {
                throw new IllegalArgumentException("package name must not be empty");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MediaProjection mediaProjection = MediaProjectionManagerService.this.new MediaProjection(i2, i, str);
                if (z) {
                    MediaProjectionManagerService.this.mAppOps.setMode(46, mediaProjection.uid, mediaProjection.packageName, 0);
                }
                return mediaProjection;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isValidMediaProjection(IMediaProjection iMediaProjection) {
            return MediaProjectionManagerService.this.isValidMediaProjection(iMediaProjection.asBinder());
        }

        public MediaProjectionInfo getActiveProjectionInfo() {
            if (MediaProjectionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_MEDIA_PROJECTION") != 0) {
                throw new SecurityException("Requires MANAGE_MEDIA_PROJECTION in order to add projection callbacks");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return MediaProjectionManagerService.this.getActiveProjectionInfo();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void stopActiveProjection() {
            if (MediaProjectionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_MEDIA_PROJECTION") != 0) {
                throw new SecurityException("Requires MANAGE_MEDIA_PROJECTION in order to add projection callbacks");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (MediaProjectionManagerService.this.mProjectionGrant != null) {
                    MediaProjectionManagerService.this.mProjectionGrant.stop();
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void addCallback(IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback) {
            if (MediaProjectionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_MEDIA_PROJECTION") != 0) {
                throw new SecurityException("Requires MANAGE_MEDIA_PROJECTION in order to add projection callbacks");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MediaProjectionManagerService.this.addCallback(iMediaProjectionWatcherCallback);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void removeCallback(IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback) {
            if (MediaProjectionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_MEDIA_PROJECTION") != 0) {
                throw new SecurityException("Requires MANAGE_MEDIA_PROJECTION in order to remove projection callbacks");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MediaProjectionManagerService.this.removeCallback(iMediaProjectionWatcherCallback);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(MediaProjectionManagerService.this.mContext, MediaProjectionManagerService.TAG, printWriter)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    MediaProjectionManagerService.this.dump(printWriter);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        private boolean checkPermission(String str, String str2) {
            return MediaProjectionManagerService.this.mContext.getPackageManager().checkPermission(str2, str) == 0;
        }
    }

    private final class MediaProjection extends IMediaProjection.Stub {
        private IMediaProjectionCallback mCallback;
        private IBinder.DeathRecipient mDeathEater;
        private IBinder mToken;
        private int mType;
        public final String packageName;
        public final int uid;
        public final UserHandle userHandle;

        public MediaProjection(int i, int i2, String str) {
            this.mType = i;
            this.uid = i2;
            this.packageName = str;
            this.userHandle = new UserHandle(UserHandle.getUserId(i2));
        }

        public boolean canProjectVideo() {
            return this.mType == 1 || this.mType == 0;
        }

        public boolean canProjectSecureVideo() {
            return false;
        }

        public boolean canProjectAudio() {
            return this.mType == 1 || this.mType == 2;
        }

        public int applyVirtualDisplayFlags(int i) {
            if (this.mType == 0) {
                return (i & (-9)) | 18;
            }
            if (this.mType == 1) {
                return (i & (-18)) | 10;
            }
            if (this.mType == 2) {
                return (i & (-9)) | 19;
            }
            throw new RuntimeException("Unknown MediaProjection type");
        }

        public void start(final IMediaProjectionCallback iMediaProjectionCallback) {
            if (iMediaProjectionCallback != null) {
                synchronized (MediaProjectionManagerService.this.mLock) {
                    if (MediaProjectionManagerService.this.isValidMediaProjection(asBinder())) {
                        throw new IllegalStateException("Cannot start already started MediaProjection");
                    }
                    this.mCallback = iMediaProjectionCallback;
                    registerCallback(this.mCallback);
                    try {
                        this.mToken = iMediaProjectionCallback.asBinder();
                        this.mDeathEater = new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                MediaProjectionManagerService.this.mCallbackDelegate.remove(iMediaProjectionCallback);
                                MediaProjection.this.stop();
                            }
                        };
                        this.mToken.linkToDeath(this.mDeathEater, 0);
                        MediaProjectionManagerService.this.startProjectionLocked(this);
                    } catch (RemoteException e) {
                        Slog.w(MediaProjectionManagerService.TAG, "MediaProjectionCallbacks must be valid, aborting MediaProjection", e);
                        return;
                    }
                }
                return;
            }
            throw new IllegalArgumentException("callback must not be null");
        }

        public void stop() {
            synchronized (MediaProjectionManagerService.this.mLock) {
                if (MediaProjectionManagerService.this.isValidMediaProjection(asBinder())) {
                    MediaProjectionManagerService.this.stopProjectionLocked(this);
                    this.mToken.unlinkToDeath(this.mDeathEater, 0);
                    this.mToken = null;
                    unregisterCallback(this.mCallback);
                    this.mCallback = null;
                    return;
                }
                Slog.w(MediaProjectionManagerService.TAG, "Attempted to stop inactive MediaProjection (uid=" + Binder.getCallingUid() + ", pid=" + Binder.getCallingPid() + ")");
            }
        }

        public void registerCallback(IMediaProjectionCallback iMediaProjectionCallback) {
            if (iMediaProjectionCallback != null) {
                MediaProjectionManagerService.this.mCallbackDelegate.add(iMediaProjectionCallback);
                return;
            }
            throw new IllegalArgumentException("callback must not be null");
        }

        public void unregisterCallback(IMediaProjectionCallback iMediaProjectionCallback) {
            if (iMediaProjectionCallback != null) {
                MediaProjectionManagerService.this.mCallbackDelegate.remove(iMediaProjectionCallback);
                return;
            }
            throw new IllegalArgumentException("callback must not be null");
        }

        public MediaProjectionInfo getProjectionInfo() {
            return new MediaProjectionInfo(this.packageName, this.userHandle);
        }

        public void dump(PrintWriter printWriter) {
            printWriter.println("(" + this.packageName + ", uid=" + this.uid + "): " + MediaProjectionManagerService.typeToString(this.mType));
        }
    }

    private class MediaRouterCallback extends MediaRouter.SimpleCallback {
        private MediaRouterCallback() {
        }

        @Override
        public void onRouteSelected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {
            synchronized (MediaProjectionManagerService.this.mLock) {
                if ((i & 4) != 0) {
                    try {
                        MediaProjectionManagerService.this.mMediaRouteInfo = routeInfo;
                        if (MediaProjectionManagerService.this.mProjectionGrant != null) {
                            MediaProjectionManagerService.this.mProjectionGrant.stop();
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
        }

        @Override
        public void onRouteUnselected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {
            if (MediaProjectionManagerService.this.mMediaRouteInfo == routeInfo) {
                MediaProjectionManagerService.this.mMediaRouteInfo = null;
            }
        }
    }

    private static class CallbackDelegate {
        private Object mLock = new Object();
        private Handler mHandler = new Handler(Looper.getMainLooper(), null, true);
        private Map<IBinder, IMediaProjectionCallback> mClientCallbacks = new ArrayMap();
        private Map<IBinder, IMediaProjectionWatcherCallback> mWatcherCallbacks = new ArrayMap();

        public void add(IMediaProjectionCallback iMediaProjectionCallback) {
            synchronized (this.mLock) {
                this.mClientCallbacks.put(iMediaProjectionCallback.asBinder(), iMediaProjectionCallback);
            }
        }

        public void add(IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback) {
            synchronized (this.mLock) {
                this.mWatcherCallbacks.put(iMediaProjectionWatcherCallback.asBinder(), iMediaProjectionWatcherCallback);
            }
        }

        public void remove(IMediaProjectionCallback iMediaProjectionCallback) {
            synchronized (this.mLock) {
                this.mClientCallbacks.remove(iMediaProjectionCallback.asBinder());
            }
        }

        public void remove(IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback) {
            synchronized (this.mLock) {
                this.mWatcherCallbacks.remove(iMediaProjectionWatcherCallback.asBinder());
            }
        }

        public void dispatchStart(MediaProjection mediaProjection) {
            if (mediaProjection == null) {
                Slog.e(MediaProjectionManagerService.TAG, "Tried to dispatch start notification for a null media projection. Ignoring!");
                return;
            }
            synchronized (this.mLock) {
                for (IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback : this.mWatcherCallbacks.values()) {
                    this.mHandler.post(new WatcherStartCallback(mediaProjection.getProjectionInfo(), iMediaProjectionWatcherCallback));
                }
            }
        }

        public void dispatchStop(MediaProjection mediaProjection) {
            if (mediaProjection == null) {
                Slog.e(MediaProjectionManagerService.TAG, "Tried to dispatch stop notification for a null media projection. Ignoring!");
                return;
            }
            synchronized (this.mLock) {
                Iterator<IMediaProjectionCallback> it = this.mClientCallbacks.values().iterator();
                while (it.hasNext()) {
                    this.mHandler.post(new ClientStopCallback(it.next()));
                }
                for (IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback : this.mWatcherCallbacks.values()) {
                    this.mHandler.post(new WatcherStopCallback(mediaProjection.getProjectionInfo(), iMediaProjectionWatcherCallback));
                }
            }
        }
    }

    private static final class WatcherStartCallback implements Runnable {
        private IMediaProjectionWatcherCallback mCallback;
        private MediaProjectionInfo mInfo;

        public WatcherStartCallback(MediaProjectionInfo mediaProjectionInfo, IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback) {
            this.mInfo = mediaProjectionInfo;
            this.mCallback = iMediaProjectionWatcherCallback;
        }

        @Override
        public void run() {
            try {
                this.mCallback.onStart(this.mInfo);
            } catch (RemoteException e) {
                Slog.w(MediaProjectionManagerService.TAG, "Failed to notify media projection has stopped", e);
            }
        }
    }

    private static final class WatcherStopCallback implements Runnable {
        private IMediaProjectionWatcherCallback mCallback;
        private MediaProjectionInfo mInfo;

        public WatcherStopCallback(MediaProjectionInfo mediaProjectionInfo, IMediaProjectionWatcherCallback iMediaProjectionWatcherCallback) {
            this.mInfo = mediaProjectionInfo;
            this.mCallback = iMediaProjectionWatcherCallback;
        }

        @Override
        public void run() {
            try {
                this.mCallback.onStop(this.mInfo);
            } catch (RemoteException e) {
                Slog.w(MediaProjectionManagerService.TAG, "Failed to notify media projection has stopped", e);
            }
        }
    }

    private static final class ClientStopCallback implements Runnable {
        private IMediaProjectionCallback mCallback;

        public ClientStopCallback(IMediaProjectionCallback iMediaProjectionCallback) {
            this.mCallback = iMediaProjectionCallback;
        }

        @Override
        public void run() {
            try {
                this.mCallback.onStop();
            } catch (RemoteException e) {
                Slog.w(MediaProjectionManagerService.TAG, "Failed to notify media projection has stopped", e);
            }
        }
    }

    private static String typeToString(int i) {
        switch (i) {
            case 0:
                return "TYPE_SCREEN_CAPTURE";
            case 1:
                return "TYPE_MIRRORING";
            case 2:
                return "TYPE_PRESENTATION";
            default:
                return Integer.toString(i);
        }
    }
}
