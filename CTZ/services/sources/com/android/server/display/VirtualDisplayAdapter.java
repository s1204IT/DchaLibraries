package com.android.server.display;

import android.content.Context;
import android.hardware.display.IVirtualDisplayCallback;
import android.media.projection.IMediaProjection;
import android.media.projection.IMediaProjectionCallback;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import java.io.PrintWriter;

@VisibleForTesting
public class VirtualDisplayAdapter extends DisplayAdapter {
    static final boolean DEBUG = false;
    static final String TAG = "VirtualDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "virtual:";
    private final Handler mHandler;
    private final SurfaceControlDisplayFactory mSurfaceControlDisplayFactory;
    private final ArrayMap<IBinder, VirtualDisplayDevice> mVirtualDisplayDevices;

    @VisibleForTesting
    public interface SurfaceControlDisplayFactory {
        IBinder createDisplay(String str, boolean z);
    }

    @Override
    public void dumpLocked(PrintWriter printWriter) {
        super.dumpLocked(printWriter);
    }

    @Override
    public void registerLocked() {
        super.registerLocked();
    }

    public VirtualDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener) {
        this(syncRoot, context, handler, listener, new SurfaceControlDisplayFactory() {
            @Override
            public final IBinder createDisplay(String str, boolean z) {
                return SurfaceControl.createDisplay(str, z);
            }
        });
    }

    @VisibleForTesting
    VirtualDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener, SurfaceControlDisplayFactory surfaceControlDisplayFactory) {
        super(syncRoot, context, handler, listener, TAG);
        this.mVirtualDisplayDevices = new ArrayMap<>();
        this.mHandler = handler;
        this.mSurfaceControlDisplayFactory = surfaceControlDisplayFactory;
    }

    public DisplayDevice createVirtualDisplayLocked(IVirtualDisplayCallback iVirtualDisplayCallback, IMediaProjection iMediaProjection, int i, String str, String str2, int i2, int i3, int i4, Surface surface, int i5, String str3) {
        String str4;
        boolean z;
        boolean z2 = (i5 & 4) != 0;
        IBinder iBinderAsBinder = iVirtualDisplayCallback.asBinder();
        IBinder iBinderCreateDisplay = this.mSurfaceControlDisplayFactory.createDisplay(str2, z2);
        String str5 = UNIQUE_ID_PREFIX + str + "," + i + "," + str2 + ",";
        int nextUniqueIndex = getNextUniqueIndex(str5);
        if (str3 == null) {
            str4 = str5 + nextUniqueIndex;
        } else {
            str4 = UNIQUE_ID_PREFIX + str + ":" + str3;
        }
        VirtualDisplayDevice virtualDisplayDevice = new VirtualDisplayDevice(iBinderCreateDisplay, iBinderAsBinder, i, str, str2, i2, i3, i4, surface, i5, new Callback(iVirtualDisplayCallback, this.mHandler), str4, nextUniqueIndex);
        this.mVirtualDisplayDevices.put(iBinderAsBinder, virtualDisplayDevice);
        if (iMediaProjection != null) {
            try {
                iMediaProjection.registerCallback(new MediaProjectionCallback(iBinderAsBinder));
                z = false;
                try {
                    iBinderAsBinder.linkToDeath(virtualDisplayDevice, 0);
                    return virtualDisplayDevice;
                } catch (RemoteException e) {
                }
            } catch (RemoteException e2) {
                z = false;
            }
        } else {
            z = false;
            iBinderAsBinder.linkToDeath(virtualDisplayDevice, 0);
            return virtualDisplayDevice;
        }
        this.mVirtualDisplayDevices.remove(iBinderAsBinder);
        virtualDisplayDevice.destroyLocked(z);
        return null;
    }

    public void resizeVirtualDisplayLocked(IBinder iBinder, int i, int i2, int i3) {
        VirtualDisplayDevice virtualDisplayDevice = this.mVirtualDisplayDevices.get(iBinder);
        if (virtualDisplayDevice != null) {
            virtualDisplayDevice.resizeLocked(i, i2, i3);
        }
    }

    public void setVirtualDisplaySurfaceLocked(IBinder iBinder, Surface surface) {
        VirtualDisplayDevice virtualDisplayDevice = this.mVirtualDisplayDevices.get(iBinder);
        if (virtualDisplayDevice != null) {
            virtualDisplayDevice.setSurfaceLocked(surface);
        }
    }

    public DisplayDevice releaseVirtualDisplayLocked(IBinder iBinder) {
        VirtualDisplayDevice virtualDisplayDeviceRemove = this.mVirtualDisplayDevices.remove(iBinder);
        if (virtualDisplayDeviceRemove != null) {
            virtualDisplayDeviceRemove.destroyLocked(true);
            iBinder.unlinkToDeath(virtualDisplayDeviceRemove, 0);
        }
        return virtualDisplayDeviceRemove;
    }

    private int getNextUniqueIndex(String str) {
        int i = 0;
        if (this.mVirtualDisplayDevices.isEmpty()) {
            return 0;
        }
        for (VirtualDisplayDevice virtualDisplayDevice : this.mVirtualDisplayDevices.values()) {
            if (virtualDisplayDevice.getUniqueId().startsWith(str) && virtualDisplayDevice.mUniqueIndex >= i) {
                i = virtualDisplayDevice.mUniqueIndex + 1;
            }
        }
        return i;
    }

    private void handleBinderDiedLocked(IBinder iBinder) {
        this.mVirtualDisplayDevices.remove(iBinder);
    }

    private void handleMediaProjectionStoppedLocked(IBinder iBinder) {
        VirtualDisplayDevice virtualDisplayDeviceRemove = this.mVirtualDisplayDevices.remove(iBinder);
        if (virtualDisplayDeviceRemove != null) {
            Slog.i(TAG, "Virtual display device released because media projection stopped: " + virtualDisplayDeviceRemove.mName);
            virtualDisplayDeviceRemove.stopLocked();
        }
    }

    private final class VirtualDisplayDevice extends DisplayDevice implements IBinder.DeathRecipient {
        private static final int PENDING_RESIZE = 2;
        private static final int PENDING_SURFACE_CHANGE = 1;
        private static final float REFRESH_RATE = 60.0f;
        private final IBinder mAppToken;
        private final Callback mCallback;
        private int mDensityDpi;
        private int mDisplayState;
        private final int mFlags;
        private int mHeight;
        private DisplayDeviceInfo mInfo;
        private Display.Mode mMode;
        final String mName;
        final String mOwnerPackageName;
        private final int mOwnerUid;
        private int mPendingChanges;
        private boolean mStopped;
        private Surface mSurface;
        private int mUniqueIndex;
        private int mWidth;

        public VirtualDisplayDevice(IBinder iBinder, IBinder iBinder2, int i, String str, String str2, int i2, int i3, int i4, Surface surface, int i5, Callback callback, String str3, int i6) {
            super(VirtualDisplayAdapter.this, iBinder, str3);
            this.mAppToken = iBinder2;
            this.mOwnerUid = i;
            this.mOwnerPackageName = str;
            this.mName = str2;
            this.mWidth = i2;
            this.mHeight = i3;
            this.mMode = DisplayAdapter.createMode(i2, i3, REFRESH_RATE);
            this.mDensityDpi = i4;
            this.mSurface = surface;
            this.mFlags = i5;
            this.mCallback = callback;
            this.mDisplayState = 0;
            this.mPendingChanges |= 1;
            this.mUniqueIndex = i6;
        }

        @Override
        public void binderDied() {
            synchronized (VirtualDisplayAdapter.this.getSyncRoot()) {
                VirtualDisplayAdapter.this.handleBinderDiedLocked(this.mAppToken);
                Slog.i(VirtualDisplayAdapter.TAG, "Virtual display device released because application token died: " + this.mOwnerPackageName);
                destroyLocked(false);
                VirtualDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 3);
            }
        }

        public void destroyLocked(boolean z) {
            if (this.mSurface != null) {
                this.mSurface.release();
                this.mSurface = null;
            }
            SurfaceControl.destroyDisplay(getDisplayTokenLocked());
            if (z) {
                this.mCallback.dispatchDisplayStopped();
            }
        }

        @Override
        public boolean hasStableUniqueId() {
            return false;
        }

        @Override
        public Runnable requestDisplayStateLocked(int i, int i2) {
            if (i != this.mDisplayState) {
                this.mDisplayState = i;
                if (i == 1) {
                    this.mCallback.dispatchDisplayPaused();
                    return null;
                }
                this.mCallback.dispatchDisplayResumed();
                return null;
            }
            return null;
        }

        @Override
        public void performTraversalLocked(SurfaceControl.Transaction transaction) {
            if ((this.mPendingChanges & 2) != 0) {
                transaction.setDisplaySize(getDisplayTokenLocked(), this.mWidth, this.mHeight);
            }
            if ((this.mPendingChanges & 1) != 0) {
                setSurfaceLocked(transaction, this.mSurface);
            }
            this.mPendingChanges = 0;
        }

        public void setSurfaceLocked(Surface surface) {
            if (!this.mStopped && this.mSurface != surface) {
                if ((this.mSurface != null) != (surface != null)) {
                    VirtualDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
                }
                VirtualDisplayAdapter.this.sendTraversalRequestLocked();
                this.mSurface = surface;
                this.mInfo = null;
                this.mPendingChanges |= 1;
            }
        }

        public void resizeLocked(int i, int i2, int i3) {
            if (this.mWidth != i || this.mHeight != i2 || this.mDensityDpi != i3) {
                VirtualDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
                VirtualDisplayAdapter.this.sendTraversalRequestLocked();
                this.mWidth = i;
                this.mHeight = i2;
                this.mMode = DisplayAdapter.createMode(i, i2, REFRESH_RATE);
                this.mDensityDpi = i3;
                this.mInfo = null;
                this.mPendingChanges |= 2;
            }
        }

        public void stopLocked() {
            setSurfaceLocked(null);
            this.mStopped = true;
        }

        @Override
        public void dumpLocked(PrintWriter printWriter) {
            super.dumpLocked(printWriter);
            printWriter.println("mFlags=" + this.mFlags);
            printWriter.println("mDisplayState=" + Display.stateToString(this.mDisplayState));
            printWriter.println("mStopped=" + this.mStopped);
        }

        @Override
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.name = this.mName;
                this.mInfo.uniqueId = getUniqueId();
                this.mInfo.width = this.mWidth;
                this.mInfo.height = this.mHeight;
                this.mInfo.modeId = this.mMode.getModeId();
                this.mInfo.defaultModeId = this.mMode.getModeId();
                this.mInfo.supportedModes = new Display.Mode[]{this.mMode};
                this.mInfo.densityDpi = this.mDensityDpi;
                this.mInfo.xDpi = this.mDensityDpi;
                this.mInfo.yDpi = this.mDensityDpi;
                this.mInfo.presentationDeadlineNanos = 16666666L;
                this.mInfo.flags = 0;
                if ((this.mFlags & 1) == 0) {
                    this.mInfo.flags |= 48;
                }
                if ((this.mFlags & 16) != 0) {
                    this.mInfo.flags &= -33;
                } else {
                    this.mInfo.flags |= 128;
                }
                if ((this.mFlags & 4) != 0) {
                    this.mInfo.flags |= 4;
                }
                if ((this.mFlags & 2) != 0) {
                    this.mInfo.flags |= 64;
                    if ((this.mFlags & 1) != 0 && "portrait".equals(SystemProperties.get("persist.demo.remoterotation"))) {
                        this.mInfo.rotation = 3;
                    }
                }
                if ((this.mFlags & 32) != 0) {
                    this.mInfo.flags |= 512;
                }
                if ((this.mFlags & 128) != 0) {
                    this.mInfo.flags |= 2;
                }
                if ((this.mFlags & 256) != 0) {
                    this.mInfo.flags |= 1024;
                }
                this.mInfo.type = 5;
                this.mInfo.touch = (this.mFlags & 64) == 0 ? 0 : 3;
                this.mInfo.state = this.mSurface != null ? 2 : 1;
                this.mInfo.ownerUid = this.mOwnerUid;
                this.mInfo.ownerPackageName = this.mOwnerPackageName;
            }
            return this.mInfo;
        }
    }

    private static class Callback extends Handler {
        private static final int MSG_ON_DISPLAY_PAUSED = 0;
        private static final int MSG_ON_DISPLAY_RESUMED = 1;
        private static final int MSG_ON_DISPLAY_STOPPED = 2;
        private final IVirtualDisplayCallback mCallback;

        public Callback(IVirtualDisplayCallback iVirtualDisplayCallback, Handler handler) {
            super(handler.getLooper());
            this.mCallback = iVirtualDisplayCallback;
        }

        @Override
        public void handleMessage(Message message) {
            try {
                switch (message.what) {
                    case 0:
                        this.mCallback.onPaused();
                        break;
                    case 1:
                        this.mCallback.onResumed();
                        break;
                    case 2:
                        this.mCallback.onStopped();
                        break;
                }
            } catch (RemoteException e) {
                Slog.w(VirtualDisplayAdapter.TAG, "Failed to notify listener of virtual display event.", e);
            }
        }

        public void dispatchDisplayPaused() {
            sendEmptyMessage(0);
        }

        public void dispatchDisplayResumed() {
            sendEmptyMessage(1);
        }

        public void dispatchDisplayStopped() {
            sendEmptyMessage(2);
        }
    }

    private final class MediaProjectionCallback extends IMediaProjectionCallback.Stub {
        private IBinder mAppToken;

        public MediaProjectionCallback(IBinder iBinder) {
            this.mAppToken = iBinder;
        }

        public void onStop() {
            synchronized (VirtualDisplayAdapter.this.getSyncRoot()) {
                VirtualDisplayAdapter.this.handleMediaProjectionStoppedLocked(this.mAppToken);
            }
        }
    }
}
