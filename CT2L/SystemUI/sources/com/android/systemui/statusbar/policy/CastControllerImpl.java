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
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteAdded: " + CastControllerImpl.routeToString(route));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteChanged: " + CastControllerImpl.routeToString(route));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteRemoved: " + CastControllerImpl.routeToString(route));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        @Override
        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo route) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteSelected(" + type + "): " + CastControllerImpl.routeToString(route));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo route) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteUnselected(" + type + "): " + CastControllerImpl.routeToString(route));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }
    };
    private final MediaProjectionManager.Callback mProjectionCallback = new MediaProjectionManager.Callback() {
        public void onStart(MediaProjectionInfo info) {
            CastControllerImpl.this.setProjection(info, true);
        }

        public void onStop(MediaProjectionInfo info) {
            CastControllerImpl.this.setProjection(info, false);
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

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CastController state:");
        pw.print("  mDiscovering=");
        pw.println(this.mDiscovering);
        pw.print("  mCallbackRegistered=");
        pw.println(this.mCallbackRegistered);
        pw.print("  mCallbacks.size=");
        pw.println(this.mCallbacks.size());
        pw.print("  mRoutes.size=");
        pw.println(this.mRoutes.size());
        for (int i = 0; i < this.mRoutes.size(); i++) {
            MediaRouter.RouteInfo route = this.mRoutes.valueAt(i);
            pw.print("    ");
            pw.println(routeToString(route));
        }
        pw.print("  mProjection=");
        pw.println(this.mProjection);
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
    public void setDiscovering(boolean request) {
        synchronized (this.mDiscoveringLock) {
            if (this.mDiscovering != request) {
                this.mDiscovering = request;
                if (DEBUG) {
                    Log.d("CastController", "setDiscovering: " + request);
                }
                handleDiscoveryChangeLocked();
            }
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
    public void setCurrentUserId(int currentUserId) {
        this.mMediaRouter.rebindAsUser(currentUserId);
    }

    @Override
    public Set<CastController.CastDevice> getCastDevices() {
        int i;
        ArraySet<CastController.CastDevice> devices = new ArraySet<>();
        synchronized (this.mProjectionLock) {
            if (this.mProjection != null) {
                CastController.CastDevice device = new CastController.CastDevice();
                device.id = this.mProjection.getPackageName();
                device.name = getAppName(this.mProjection.getPackageName());
                device.description = this.mContext.getString(R.string.quick_settings_casting);
                device.state = 2;
                device.tag = this.mProjection;
                devices.add(device);
            } else {
                synchronized (this.mRoutes) {
                    for (MediaRouter.RouteInfo route : this.mRoutes.values()) {
                        CastController.CastDevice device2 = new CastController.CastDevice();
                        device2.id = route.getTag().toString();
                        CharSequence name = route.getName(this.mContext);
                        device2.name = name != null ? name.toString() : null;
                        CharSequence description = route.getDescription();
                        device2.description = description != null ? description.toString() : null;
                        if (route.isConnecting()) {
                            i = 1;
                        } else {
                            i = route.isSelected() ? 2 : 0;
                        }
                        device2.state = i;
                        device2.tag = route;
                        devices.add(device2);
                    }
                }
            }
        }
        return devices;
    }

    @Override
    public void startCasting(CastController.CastDevice device) {
        if (device != null && device.tag != null) {
            MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) device.tag;
            if (DEBUG) {
                Log.d("CastController", "startCasting: " + routeToString(route));
            }
            this.mMediaRouter.selectRoute(4, route);
        }
    }

    @Override
    public void stopCasting(CastController.CastDevice device) {
        boolean isProjection = device.tag instanceof MediaProjectionInfo;
        if (DEBUG) {
            Log.d("CastController", "stopCasting isProjection=" + isProjection);
        }
        if (isProjection) {
            MediaProjectionInfo projection = (MediaProjectionInfo) device.tag;
            if (Objects.equals(this.mProjectionManager.getActiveProjectionInfo(), projection)) {
                this.mProjectionManager.stopActiveProjection();
                return;
            } else {
                Log.w("CastController", "Projection is no longer active: " + projection);
                return;
            }
        }
        this.mMediaRouter.getDefaultRoute().select();
    }

    private void setProjection(MediaProjectionInfo projection, boolean started) {
        boolean changed = false;
        MediaProjectionInfo oldProjection = this.mProjection;
        synchronized (this.mProjectionLock) {
            boolean isCurrent = Objects.equals(projection, this.mProjection);
            if (started && !isCurrent) {
                this.mProjection = projection;
                changed = true;
            } else if (!started && isCurrent) {
                this.mProjection = null;
                changed = true;
            }
        }
        if (changed) {
            if (DEBUG) {
                Log.d("CastController", "setProjection: " + oldProjection + " -> " + this.mProjection);
            }
            fireOnCastDevicesChanged();
        }
    }

    private String getAppName(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            if (appInfo != null) {
                CharSequence label = appInfo.loadLabel(pm);
                if (!TextUtils.isEmpty(label)) {
                    packageName = label.toString();
                } else {
                    Log.w("CastController", "No label found for package: " + packageName);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("CastController", "Error getting appName for package: " + packageName, e);
        }
        return packageName;
    }

    private void updateRemoteDisplays() {
        synchronized (this.mRoutes) {
            this.mRoutes.clear();
            int n = this.mMediaRouter.getRouteCount();
            for (int i = 0; i < n; i++) {
                MediaRouter.RouteInfo route = this.mMediaRouter.getRouteAt(i);
                if (route.isEnabled() && route.matchesTypes(4)) {
                    ensureTagExists(route);
                    this.mRoutes.put(route.getTag().toString(), route);
                }
            }
            MediaRouter.RouteInfo selected = this.mMediaRouter.getSelectedRoute(4);
            if (selected != null && !selected.isDefault()) {
                ensureTagExists(selected);
                this.mRoutes.put(selected.getTag().toString(), selected);
            }
        }
        fireOnCastDevicesChanged();
    }

    private void ensureTagExists(MediaRouter.RouteInfo route) {
        if (route.getTag() == null) {
            route.setTag(UUID.randomUUID().toString());
        }
    }

    private void fireOnCastDevicesChanged() {
        for (CastController.Callback callback : this.mCallbacks) {
            fireOnCastDevicesChanged(callback);
        }
    }

    private void fireOnCastDevicesChanged(CastController.Callback callback) {
        callback.onCastDevicesChanged();
    }

    private static String routeToString(MediaRouter.RouteInfo route) {
        if (route == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder().append(route.getName()).append('/').append(route.getDescription()).append('@').append(route.getDeviceAddress()).append(",status=").append(route.getStatus());
        if (route.isDefault()) {
            sb.append(",default");
        }
        if (route.isEnabled()) {
            sb.append(",enabled");
        }
        if (route.isConnecting()) {
            sb.append(",connecting");
        }
        if (route.isSelected()) {
            sb.append(",selected");
        }
        return sb.append(",id=").append(route.getTag()).toString();
    }
}
