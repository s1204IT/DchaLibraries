package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.MediaRouter;
import android.media.projection.MediaProjectionInfo;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.CastController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class CastControllerImpl implements CastController {
    private static final boolean DEBUG = Log.isLoggable("CastController", 3);
    private boolean mCallbackRegistered;
    private final Context mContext;
    private boolean mDiscovering;
    private final MediaRouter mMediaRouter;
    private MediaProjectionInfo mProjection;
    private final MediaProjectionManager mProjectionManager;
    private final ArrayList<CastController.Callback> mCallbacks = new ArrayList<>();
    private final ArrayMap<String, MediaRouter.RouteInfo> mRoutes = new ArrayMap<>();
    private final Object mDiscoveringLock = new Object();
    private final Object mProjectionLock = new Object();
    private final MediaRouter.SimpleCallback mMediaCallback = new MediaRouter.SimpleCallback() {
        @Override
        public void onRouteAdded(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteAdded: " + CastControllerImpl.routeToString(routeInfo));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        @Override
        public void onRouteChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteChanged: " + CastControllerImpl.routeToString(routeInfo));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        @Override
        public void onRouteRemoved(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteRemoved: " + CastControllerImpl.routeToString(routeInfo));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        @Override
        public void onRouteSelected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteSelected(" + i + "): " + CastControllerImpl.routeToString(routeInfo));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        @Override
        public void onRouteUnselected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteUnselected(" + i + "): " + CastControllerImpl.routeToString(routeInfo));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }
    };
    private final MediaProjectionManager.Callback mProjectionCallback = new MediaProjectionManager.Callback() {
        public void onStart(MediaProjectionInfo mediaProjectionInfo) {
            CastControllerImpl.this.setProjection(mediaProjectionInfo, true);
        }

        public void onStop(MediaProjectionInfo mediaProjectionInfo) {
            CastControllerImpl.this.setProjection(mediaProjectionInfo, false);
        }
    };

    public CastControllerImpl(Context context) {
        this.mContext = context;
        this.mMediaRouter = (MediaRouter) context.getSystemService("media_router");
        this.mProjectionManager = (MediaProjectionManager) context.getSystemService("media_projection");
        this.mProjection = this.mProjectionManager.getActiveProjectionInfo();
        this.mProjectionManager.addCallback(this.mProjectionCallback, new Handler());
        if (DEBUG) {
            Log.d("CastController", "new CastController()");
        }
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("CastController state:");
        printWriter.print("  mDiscovering=");
        printWriter.println(this.mDiscovering);
        printWriter.print("  mCallbackRegistered=");
        printWriter.println(this.mCallbackRegistered);
        printWriter.print("  mCallbacks.size=");
        printWriter.println(this.mCallbacks.size());
        printWriter.print("  mRoutes.size=");
        printWriter.println(this.mRoutes.size());
        for (int i = 0; i < this.mRoutes.size(); i++) {
            MediaRouter.RouteInfo routeInfoValueAt = this.mRoutes.valueAt(i);
            printWriter.print("    ");
            printWriter.println(routeToString(routeInfoValueAt));
        }
        printWriter.print("  mProjection=");
        printWriter.println(this.mProjection);
    }

    @Override
    public void addCallback(CastController.Callback callback) {
        this.mCallbacks.add(callback);
        fireOnCastDevicesChanged(callback);
        synchronized (this.mDiscoveringLock) {
            handleDiscoveryChangeLocked();
        }
    }

    @Override
    public void removeCallback(CastController.Callback callback) {
        this.mCallbacks.remove(callback);
        synchronized (this.mDiscoveringLock) {
            handleDiscoveryChangeLocked();
        }
    }

    @Override
    public void setDiscovering(boolean z) {
        synchronized (this.mDiscoveringLock) {
            if (this.mDiscovering == z) {
                return;
            }
            this.mDiscovering = z;
            if (DEBUG) {
                Log.d("CastController", "setDiscovering: " + z);
            }
            handleDiscoveryChangeLocked();
        }
    }

    private void handleDiscoveryChangeLocked() {
        if (this.mCallbackRegistered) {
            this.mMediaRouter.removeCallback(this.mMediaCallback);
            this.mCallbackRegistered = false;
        }
        if (this.mDiscovering) {
            this.mMediaRouter.addCallback(4, this.mMediaCallback, 4);
            this.mCallbackRegistered = true;
        } else if (this.mCallbacks.size() != 0) {
            this.mMediaRouter.addCallback(4, this.mMediaCallback, 8);
            this.mCallbackRegistered = true;
        }
    }

    @Override
    public void setCurrentUserId(int i) {
        this.mMediaRouter.rebindAsUser(i);
    }

    @Override
    public Set<CastController.CastDevice> getCastDevices() {
        int i;
        ArraySet arraySet = new ArraySet();
        synchronized (this.mProjectionLock) {
            if (this.mProjection != null) {
                CastController.CastDevice castDevice = new CastController.CastDevice();
                castDevice.id = this.mProjection.getPackageName();
                castDevice.name = getAppName(this.mProjection.getPackageName());
                castDevice.description = this.mContext.getString(R.string.quick_settings_casting);
                castDevice.state = 2;
                castDevice.tag = this.mProjection;
                arraySet.add(castDevice);
                return arraySet;
            }
            synchronized (this.mRoutes) {
                for (MediaRouter.RouteInfo routeInfo : this.mRoutes.values()) {
                    CastController.CastDevice castDevice2 = new CastController.CastDevice();
                    castDevice2.id = routeInfo.getTag().toString();
                    CharSequence name = routeInfo.getName(this.mContext);
                    castDevice2.name = name != null ? name.toString() : null;
                    CharSequence description = routeInfo.getDescription();
                    castDevice2.description = description != null ? description.toString() : null;
                    if (routeInfo.isConnecting()) {
                        i = 1;
                    } else {
                        i = routeInfo.isSelected() ? 2 : 0;
                    }
                    castDevice2.state = i;
                    castDevice2.tag = routeInfo;
                    arraySet.add(castDevice2);
                }
            }
            return arraySet;
        }
    }

    @Override
    public void startCasting(CastController.CastDevice castDevice) {
        if (castDevice == null || castDevice.tag == null) {
            return;
        }
        MediaRouter.RouteInfo routeInfo = (MediaRouter.RouteInfo) castDevice.tag;
        if (DEBUG) {
            Log.d("CastController", "startCasting: " + routeToString(routeInfo));
        }
        this.mMediaRouter.selectRoute(4, routeInfo);
    }

    @Override
    public void stopCasting(CastController.CastDevice castDevice) {
        boolean z = castDevice.tag instanceof MediaProjectionInfo;
        if (DEBUG) {
            Log.d("CastController", "stopCasting isProjection=" + z);
        }
        if (z) {
            MediaProjectionInfo mediaProjectionInfo = (MediaProjectionInfo) castDevice.tag;
            if (Objects.equals(this.mProjectionManager.getActiveProjectionInfo(), mediaProjectionInfo)) {
                this.mProjectionManager.stopActiveProjection();
                return;
            }
            Log.w("CastController", "Projection is no longer active: " + mediaProjectionInfo);
            return;
        }
        this.mMediaRouter.getFallbackRoute().select();
    }

    private void setProjection(MediaProjectionInfo mediaProjectionInfo, boolean z) {
        boolean z2;
        MediaProjectionInfo mediaProjectionInfo2 = this.mProjection;
        synchronized (this.mProjectionLock) {
            boolean zEquals = Objects.equals(mediaProjectionInfo, this.mProjection);
            z2 = true;
            if (z && !zEquals) {
                this.mProjection = mediaProjectionInfo;
            } else if (!z && zEquals) {
                this.mProjection = null;
            } else {
                z2 = false;
            }
        }
        if (z2) {
            if (DEBUG) {
                Log.d("CastController", "setProjection: " + mediaProjectionInfo2 + " -> " + this.mProjection);
            }
            fireOnCastDevicesChanged();
        }
    }

    private String getAppName(String str) {
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(str, 0);
            if (applicationInfo != null) {
                CharSequence charSequenceLoadLabel = applicationInfo.loadLabel(packageManager);
                if (!TextUtils.isEmpty(charSequenceLoadLabel)) {
                    return charSequenceLoadLabel.toString();
                }
            }
            Log.w("CastController", "No label found for package: " + str);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("CastController", "Error getting appName for package: " + str, e);
        }
        return str;
    }

    private void updateRemoteDisplays() {
        synchronized (this.mRoutes) {
            this.mRoutes.clear();
            int routeCount = this.mMediaRouter.getRouteCount();
            for (int i = 0; i < routeCount; i++) {
                MediaRouter.RouteInfo routeAt = this.mMediaRouter.getRouteAt(i);
                if (routeAt.isEnabled() && routeAt.matchesTypes(4)) {
                    ensureTagExists(routeAt);
                    this.mRoutes.put(routeAt.getTag().toString(), routeAt);
                }
            }
            MediaRouter.RouteInfo selectedRoute = this.mMediaRouter.getSelectedRoute(4);
            if (selectedRoute != null && !selectedRoute.isDefault()) {
                ensureTagExists(selectedRoute);
                this.mRoutes.put(selectedRoute.getTag().toString(), selectedRoute);
            }
        }
        fireOnCastDevicesChanged();
    }

    private void ensureTagExists(MediaRouter.RouteInfo routeInfo) {
        if (routeInfo.getTag() == null) {
            routeInfo.setTag(UUID.randomUUID().toString());
        }
    }

    private void fireOnCastDevicesChanged() {
        Iterator<CastController.Callback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            fireOnCastDevicesChanged(it.next());
        }
    }

    private void fireOnCastDevicesChanged(CastController.Callback callback) {
        callback.onCastDevicesChanged();
    }

    private static String routeToString(MediaRouter.RouteInfo routeInfo) {
        if (routeInfo == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(routeInfo.getName());
        sb.append('/');
        sb.append(routeInfo.getDescription());
        sb.append('@');
        sb.append(routeInfo.getDeviceAddress());
        sb.append(",status=");
        sb.append(routeInfo.getStatus());
        if (routeInfo.isDefault()) {
            sb.append(",default");
        }
        if (routeInfo.isEnabled()) {
            sb.append(",enabled");
        }
        if (routeInfo.isConnecting()) {
            sb.append(",connecting");
        }
        if (routeInfo.isSelected()) {
            sb.append(",selected");
        }
        sb.append(",id=");
        sb.append(routeInfo.getTag());
        return sb.toString();
    }
}
