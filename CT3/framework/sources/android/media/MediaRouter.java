package android.media;

import android.Manifest;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplayStatus;
import android.media.AudioAttributes;
import android.media.IAudioRoutesObserver;
import android.media.IAudioService;
import android.media.IMediaRouterClient;
import android.media.IMediaRouterService;
import android.media.IRemoteVolumeObserver;
import android.media.MediaRouterClientState;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class MediaRouter {

    static final boolean f14assertionsDisabled;
    public static final int AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE = 1;
    public static final int CALLBACK_FLAG_PASSIVE_DISCOVERY = 8;
    public static final int CALLBACK_FLAG_PERFORM_ACTIVE_SCAN = 1;
    public static final int CALLBACK_FLAG_REQUEST_DISCOVERY = 4;
    public static final int CALLBACK_FLAG_UNFILTERED_EVENTS = 2;
    private static final boolean DEBUG;
    private static final boolean LOGD;
    static final int ROUTE_TYPE_ANY = 8388615;
    public static final int ROUTE_TYPE_LIVE_AUDIO = 1;
    public static final int ROUTE_TYPE_LIVE_VIDEO = 2;
    public static final int ROUTE_TYPE_REMOTE_DISPLAY = 4;
    public static final int ROUTE_TYPE_USER = 8388608;
    private static final String TAG = "MediaRouter";
    static final HashMap<Context, MediaRouter> sRouters;
    static Static sStatic;

    public static abstract class VolumeCallback {
        public abstract void onVolumeSetRequest(RouteInfo routeInfo, int i);

        public abstract void onVolumeUpdateRequest(RouteInfo routeInfo, int i);
    }

    static {
        f14assertionsDisabled = !MediaRouter.class.desiredAssertionStatus();
        LOGD = "eng".equals(Build.TYPE);
        DEBUG = Log.isLoggable(TAG, 3) ? true : LOGD;
        sRouters = new HashMap<>();
    }

    static class Static implements DisplayManager.DisplayListener {
        boolean mActivelyScanningWifiDisplays;
        final Context mAppContext;
        final IAudioService mAudioService;
        RouteInfo mBluetoothA2dpRoute;
        final boolean mCanConfigureWifiDisplays;
        IMediaRouterClient mClient;
        MediaRouterClientState mClientState;
        RouteInfo mDefaultAudioVideo;
        boolean mDiscoverRequestActiveScan;
        int mDiscoveryRequestRouteTypes;
        final DisplayManager mDisplayService;
        final Handler mHandler;
        final IMediaRouterService mMediaRouterService;
        String mPreviousActiveWifiDisplayAddress;
        RouteInfo mSelectedRoute;
        final RouteCategory mSystemCategory;
        final CopyOnWriteArrayList<CallbackInfo> mCallbacks = new CopyOnWriteArrayList<>();
        final ArrayList<RouteInfo> mRoutes = new ArrayList<>();
        final ArrayList<RouteCategory> mCategories = new ArrayList<>();
        final AudioRoutesInfo mCurAudioRoutesInfo = new AudioRoutesInfo();
        int mCurrentUserId = -1;
        final IAudioRoutesObserver.Stub mAudioRoutesObserver = new IAudioRoutesObserver.Stub() {
            @Override
            public void dispatchAudioRoutesChanged(final AudioRoutesInfo newRoutes) {
                Static.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Static.this.updateAudioRoutes(newRoutes);
                    }
                });
            }
        };
        final Resources mResources = Resources.getSystem();

        Static(Context appContext) {
            this.mAppContext = appContext;
            this.mHandler = new Handler(appContext.getMainLooper());
            IBinder b = ServiceManager.getService(Context.AUDIO_SERVICE);
            this.mAudioService = IAudioService.Stub.asInterface(b);
            this.mDisplayService = (DisplayManager) appContext.getSystemService(Context.DISPLAY_SERVICE);
            this.mMediaRouterService = IMediaRouterService.Stub.asInterface(ServiceManager.getService(Context.MEDIA_ROUTER_SERVICE));
            this.mSystemCategory = new RouteCategory(17040621, 3, false);
            this.mSystemCategory.mIsSystem = true;
            this.mCanConfigureWifiDisplays = appContext.checkPermission(Manifest.permission.CONFIGURE_WIFI_DISPLAY, Process.myPid(), Process.myUid()) == 0;
        }

        void startMonitoringRoutes(Context appContext) {
            this.mDefaultAudioVideo = new RouteInfo(this.mSystemCategory);
            this.mDefaultAudioVideo.mNameResId = 17040617;
            this.mDefaultAudioVideo.mSupportedTypes = 3;
            this.mDefaultAudioVideo.updatePresentationDisplay();
            MediaRouter.addRouteStatic(this.mDefaultAudioVideo);
            MediaRouter.updateWifiDisplayStatus(this.mDisplayService.getWifiDisplayStatus());
            appContext.registerReceiver(new WifiDisplayStatusChangedReceiver(), new IntentFilter(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED));
            appContext.registerReceiver(new VolumeChangeReceiver(), new IntentFilter(AudioManager.VOLUME_CHANGED_ACTION));
            this.mDisplayService.registerDisplayListener(this, this.mHandler);
            AudioRoutesInfo newAudioRoutes = null;
            try {
                newAudioRoutes = this.mAudioService.startWatchingRoutes(this.mAudioRoutesObserver);
            } catch (RemoteException e) {
            }
            if (newAudioRoutes != null) {
                updateAudioRoutes(newAudioRoutes);
            }
            rebindAsUser(UserHandle.myUserId());
            if (this.mSelectedRoute != null) {
                return;
            }
            MediaRouter.selectDefaultRouteStatic();
        }

        void updateAudioRoutes(AudioRoutesInfo newRoutes) {
            int name;
            Log.v(MediaRouter.TAG, "Updating audio routes: " + newRoutes);
            if (newRoutes.mainType != this.mCurAudioRoutesInfo.mainType) {
                this.mCurAudioRoutesInfo.mainType = newRoutes.mainType;
                if ((newRoutes.mainType & 2) != 0 || (newRoutes.mainType & 1) != 0) {
                    name = 17040618;
                } else if ((newRoutes.mainType & 4) != 0) {
                    name = 17040619;
                } else if ((newRoutes.mainType & 8) != 0) {
                    name = 17040620;
                } else {
                    name = 17040617;
                }
                MediaRouter.sStatic.mDefaultAudioVideo.mNameResId = name;
                MediaRouter.dispatchRouteChanged(MediaRouter.sStatic.mDefaultAudioVideo);
            }
            int mainType = this.mCurAudioRoutesInfo.mainType;
            if (!TextUtils.equals(newRoutes.bluetoothName, this.mCurAudioRoutesInfo.bluetoothName)) {
                this.mCurAudioRoutesInfo.bluetoothName = newRoutes.bluetoothName;
                if (this.mCurAudioRoutesInfo.bluetoothName != null) {
                    if (MediaRouter.sStatic.mBluetoothA2dpRoute == null) {
                        RouteInfo info = new RouteInfo(MediaRouter.sStatic.mSystemCategory);
                        info.mName = this.mCurAudioRoutesInfo.bluetoothName;
                        info.mDescription = MediaRouter.sStatic.mResources.getText(17040622);
                        info.mSupportedTypes = 1;
                        info.mDeviceType = 3;
                        MediaRouter.sStatic.mBluetoothA2dpRoute = info;
                        MediaRouter.addRouteStatic(MediaRouter.sStatic.mBluetoothA2dpRoute);
                    } else {
                        MediaRouter.sStatic.mBluetoothA2dpRoute.mName = this.mCurAudioRoutesInfo.bluetoothName;
                        MediaRouter.dispatchRouteChanged(MediaRouter.sStatic.mBluetoothA2dpRoute);
                    }
                } else if (MediaRouter.sStatic.mBluetoothA2dpRoute != null) {
                    MediaRouter.removeRouteStatic(MediaRouter.sStatic.mBluetoothA2dpRoute);
                    MediaRouter.sStatic.mBluetoothA2dpRoute = null;
                }
            }
            if (this.mBluetoothA2dpRoute == null) {
                return;
            }
            boolean a2dpEnabled = isBluetoothA2dpOn();
            if (mainType != 0 && this.mSelectedRoute == this.mBluetoothA2dpRoute && !a2dpEnabled) {
                MediaRouter.selectRouteStatic(1, this.mDefaultAudioVideo, false);
            } else {
                if ((this.mSelectedRoute != this.mDefaultAudioVideo && this.mSelectedRoute != null) || !a2dpEnabled) {
                    return;
                }
                MediaRouter.selectRouteStatic(1, this.mBluetoothA2dpRoute, false);
            }
        }

        boolean isBluetoothA2dpOn() {
            try {
                return this.mAudioService.isBluetoothA2dpOn();
            } catch (RemoteException e) {
                Log.e(MediaRouter.TAG, "Error querying Bluetooth A2DP state", e);
                return false;
            }
        }

        void updateDiscoveryRequest() {
            int routeTypes = 0;
            int passiveRouteTypes = 0;
            boolean activeScan = false;
            boolean activeScanWifiDisplay = false;
            int count = this.mCallbacks.size();
            for (int i = 0; i < count; i++) {
                CallbackInfo cbi = this.mCallbacks.get(i);
                if ((cbi.flags & 5) == 0 && (cbi.flags & 8) != 0) {
                    passiveRouteTypes |= cbi.type;
                } else {
                    routeTypes |= cbi.type;
                }
                if ((cbi.flags & 1) != 0) {
                    activeScan = true;
                    if ((cbi.type & 4) != 0) {
                        activeScanWifiDisplay = true;
                    }
                }
            }
            if (routeTypes != 0 || activeScan) {
                routeTypes |= passiveRouteTypes;
            }
            if (this.mCanConfigureWifiDisplays) {
                if (this.mSelectedRoute != null && this.mSelectedRoute.matchesTypes(4)) {
                    activeScanWifiDisplay = false;
                }
                if (activeScanWifiDisplay) {
                    if (!this.mActivelyScanningWifiDisplays) {
                        this.mActivelyScanningWifiDisplays = true;
                        this.mDisplayService.startWifiDisplayScan();
                    }
                } else if (this.mActivelyScanningWifiDisplays) {
                    this.mActivelyScanningWifiDisplays = false;
                    this.mDisplayService.stopWifiDisplayScan();
                }
            }
            if (routeTypes == this.mDiscoveryRequestRouteTypes && activeScan == this.mDiscoverRequestActiveScan) {
                return;
            }
            this.mDiscoveryRequestRouteTypes = routeTypes;
            this.mDiscoverRequestActiveScan = activeScan;
            publishClientDiscoveryRequest();
        }

        @Override
        public void onDisplayAdded(int displayId) {
            updatePresentationDisplays(displayId);
        }

        @Override
        public void onDisplayChanged(int displayId) {
            updatePresentationDisplays(displayId);
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            updatePresentationDisplays(displayId);
        }

        public Display[] getAllPresentationDisplays() {
            return this.mDisplayService.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        }

        private void updatePresentationDisplays(int changedDisplayId) {
            int count = this.mRoutes.size();
            for (int i = 0; i < count; i++) {
                RouteInfo route = this.mRoutes.get(i);
                if (route.updatePresentationDisplay() || (route.mPresentationDisplay != null && route.mPresentationDisplay.getDisplayId() == changedDisplayId)) {
                    MediaRouter.dispatchRoutePresentationDisplayChanged(route);
                }
            }
        }

        void setSelectedRoute(RouteInfo info, boolean explicit) {
            this.mSelectedRoute = info;
            publishClientSelectedRoute(explicit);
        }

        void rebindAsUser(int userId) {
            if (this.mCurrentUserId == userId && userId >= 0 && this.mClient != null) {
                return;
            }
            if (this.mClient != null) {
                try {
                    this.mMediaRouterService.unregisterClient(this.mClient);
                } catch (RemoteException ex) {
                    Log.e(MediaRouter.TAG, "Unable to unregister media router client.", ex);
                }
                this.mClient = null;
            }
            this.mCurrentUserId = userId;
            try {
                Client client = new Client();
                this.mMediaRouterService.registerClientAsUser(client, this.mAppContext.getPackageName(), userId);
                this.mClient = client;
            } catch (RemoteException ex2) {
                Log.e(MediaRouter.TAG, "Unable to register media router client.", ex2);
            }
            publishClientDiscoveryRequest();
            publishClientSelectedRoute(false);
            updateClientState();
        }

        void publishClientDiscoveryRequest() {
            if (this.mClient == null) {
                return;
            }
            try {
                this.mMediaRouterService.setDiscoveryRequest(this.mClient, this.mDiscoveryRequestRouteTypes, this.mDiscoverRequestActiveScan);
            } catch (RemoteException ex) {
                Log.e(MediaRouter.TAG, "Unable to publish media router client discovery request.", ex);
            }
        }

        void publishClientSelectedRoute(boolean explicit) {
            if (this.mClient == null) {
                return;
            }
            try {
                this.mMediaRouterService.setSelectedRoute(this.mClient, this.mSelectedRoute != null ? this.mSelectedRoute.mGlobalRouteId : null, explicit);
            } catch (RemoteException ex) {
                Log.e(MediaRouter.TAG, "Unable to publish media router client selected route.", ex);
            }
        }

        void updateClientState() {
            this.mClientState = null;
            if (this.mClient != null) {
                try {
                    this.mClientState = this.mMediaRouterService.getState(this.mClient);
                } catch (RemoteException ex) {
                    Log.e(MediaRouter.TAG, "Unable to retrieve media router client state.", ex);
                }
            }
            ArrayList<MediaRouterClientState.RouteInfo> arrayList = this.mClientState != null ? this.mClientState.routes : null;
            String str = this.mClientState != null ? this.mClientState.globallySelectedRouteId : null;
            int globalRouteCount = arrayList != null ? arrayList.size() : 0;
            for (int i = 0; i < globalRouteCount; i++) {
                MediaRouterClientState.RouteInfo globalRoute = arrayList.get(i);
                RouteInfo route = findGlobalRoute(globalRoute.id);
                if (route == null) {
                    MediaRouter.addRouteStatic(makeGlobalRoute(globalRoute));
                } else {
                    updateGlobalRoute(route, globalRoute);
                }
            }
            if (str != null) {
                RouteInfo route2 = findGlobalRoute(str);
                if (route2 == null) {
                    Log.w(MediaRouter.TAG, "Could not find new globally selected route: " + str);
                } else if (route2 != this.mSelectedRoute) {
                    if (MediaRouter.DEBUG) {
                        Log.d(MediaRouter.TAG, "Selecting new globally selected route: " + route2);
                    }
                    MediaRouter.selectRouteStatic(route2.mSupportedTypes, route2, false);
                }
            } else if (this.mSelectedRoute != null && this.mSelectedRoute.mGlobalRouteId != null) {
                if (MediaRouter.DEBUG) {
                    Log.d(MediaRouter.TAG, "Unselecting previous globally selected route: " + this.mSelectedRoute);
                }
                MediaRouter.selectDefaultRouteStatic();
            }
            int i2 = this.mRoutes.size();
            while (true) {
                int i3 = i2;
                i2 = i3 - 1;
                if (i3 <= 0) {
                    return;
                }
                RouteInfo route3 = this.mRoutes.get(i2);
                String globalRouteId = route3.mGlobalRouteId;
                if (globalRouteId != null) {
                    int j = 0;
                    while (true) {
                        if (j < globalRouteCount) {
                            if (globalRouteId.equals(arrayList.get(j).id)) {
                                break;
                            } else {
                                j++;
                            }
                        } else {
                            MediaRouter.removeRouteStatic(route3);
                            break;
                        }
                    }
                }
            }
        }

        void requestSetVolume(RouteInfo route, int volume) {
            if (route.mGlobalRouteId == null || this.mClient == null) {
                return;
            }
            try {
                this.mMediaRouterService.requestSetVolume(this.mClient, route.mGlobalRouteId, volume);
            } catch (RemoteException ex) {
                Log.w(MediaRouter.TAG, "Unable to request volume change.", ex);
            }
        }

        void requestUpdateVolume(RouteInfo route, int direction) {
            if (route.mGlobalRouteId == null || this.mClient == null) {
                return;
            }
            try {
                this.mMediaRouterService.requestUpdateVolume(this.mClient, route.mGlobalRouteId, direction);
            } catch (RemoteException ex) {
                Log.w(MediaRouter.TAG, "Unable to request volume change.", ex);
            }
        }

        RouteInfo makeGlobalRoute(MediaRouterClientState.RouteInfo globalRoute) {
            RouteInfo route = new RouteInfo(MediaRouter.sStatic.mSystemCategory);
            route.mGlobalRouteId = globalRoute.id;
            route.mName = globalRoute.name;
            route.mDescription = globalRoute.description;
            route.mSupportedTypes = globalRoute.supportedTypes;
            route.mDeviceType = globalRoute.deviceType;
            route.mEnabled = globalRoute.enabled;
            route.setRealStatusCode(globalRoute.statusCode);
            route.mPlaybackType = globalRoute.playbackType;
            route.mPlaybackStream = globalRoute.playbackStream;
            route.mVolume = globalRoute.volume;
            route.mVolumeMax = globalRoute.volumeMax;
            route.mVolumeHandling = globalRoute.volumeHandling;
            route.mPresentationDisplayId = globalRoute.presentationDisplayId;
            route.updatePresentationDisplay();
            return route;
        }

        void updateGlobalRoute(RouteInfo route, MediaRouterClientState.RouteInfo globalRoute) {
            boolean changed = false;
            boolean volumeChanged = false;
            boolean presentationDisplayChanged = false;
            if (!Objects.equals(route.mName, globalRoute.name)) {
                route.mName = globalRoute.name;
                changed = true;
            }
            if (!Objects.equals(route.mDescription, globalRoute.description)) {
                route.mDescription = globalRoute.description;
                changed = true;
            }
            int oldSupportedTypes = route.mSupportedTypes;
            if (oldSupportedTypes != globalRoute.supportedTypes) {
                route.mSupportedTypes = globalRoute.supportedTypes;
                changed = true;
            }
            if (route.mEnabled != globalRoute.enabled) {
                route.mEnabled = globalRoute.enabled;
                changed = true;
            }
            if (route.mRealStatusCode != globalRoute.statusCode) {
                route.setRealStatusCode(globalRoute.statusCode);
                changed = true;
            }
            if (route.mPlaybackType != globalRoute.playbackType) {
                route.mPlaybackType = globalRoute.playbackType;
                changed = true;
            }
            if (route.mPlaybackStream != globalRoute.playbackStream) {
                route.mPlaybackStream = globalRoute.playbackStream;
                changed = true;
            }
            if (route.mVolume != globalRoute.volume) {
                route.mVolume = globalRoute.volume;
                changed = true;
                volumeChanged = true;
            }
            if (route.mVolumeMax != globalRoute.volumeMax) {
                route.mVolumeMax = globalRoute.volumeMax;
                changed = true;
                volumeChanged = true;
            }
            if (route.mVolumeHandling != globalRoute.volumeHandling) {
                route.mVolumeHandling = globalRoute.volumeHandling;
                changed = true;
                volumeChanged = true;
            }
            if (route.mPresentationDisplayId != globalRoute.presentationDisplayId) {
                route.mPresentationDisplayId = globalRoute.presentationDisplayId;
                route.updatePresentationDisplay();
                changed = true;
                presentationDisplayChanged = true;
            }
            if (changed) {
                MediaRouter.dispatchRouteChanged(route, oldSupportedTypes);
            }
            if (volumeChanged) {
                MediaRouter.dispatchRouteVolumeChanged(route);
            }
            if (!presentationDisplayChanged) {
                return;
            }
            MediaRouter.dispatchRoutePresentationDisplayChanged(route);
        }

        RouteInfo findGlobalRoute(String globalRouteId) {
            int count = this.mRoutes.size();
            for (int i = 0; i < count; i++) {
                RouteInfo route = this.mRoutes.get(i);
                if (globalRouteId.equals(route.mGlobalRouteId)) {
                    return route;
                }
            }
            return null;
        }

        final class Client extends IMediaRouterClient.Stub {
            Client() {
            }

            @Override
            public void onStateChanged() {
                Static.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (Client.this != Static.this.mClient) {
                            return;
                        }
                        Static.this.updateClientState();
                    }
                });
            }
        }
    }

    static String typesToString(int types) {
        StringBuilder result = new StringBuilder();
        if ((types & 1) != 0) {
            result.append("ROUTE_TYPE_LIVE_AUDIO ");
        }
        if ((types & 2) != 0) {
            result.append("ROUTE_TYPE_LIVE_VIDEO ");
        }
        if ((types & 4) != 0) {
            result.append("ROUTE_TYPE_REMOTE_DISPLAY ");
        }
        if ((8388608 & types) != 0) {
            result.append("ROUTE_TYPE_USER ");
        }
        return result.toString();
    }

    public MediaRouter(Context context) {
        synchronized (Static.class) {
            if (sStatic == null) {
                Context appContext = context.getApplicationContext();
                sStatic = new Static(appContext);
                sStatic.startMonitoringRoutes(appContext);
            }
        }
    }

    public RouteInfo getDefaultRoute() {
        return sStatic.mDefaultAudioVideo;
    }

    public RouteCategory getSystemCategory() {
        return sStatic.mSystemCategory;
    }

    public RouteInfo getSelectedRoute() {
        return getSelectedRoute(ROUTE_TYPE_ANY);
    }

    public RouteInfo getSelectedRoute(int type) {
        if (sStatic.mSelectedRoute != null && (sStatic.mSelectedRoute.mSupportedTypes & type) != 0) {
            return sStatic.mSelectedRoute;
        }
        if (type == 8388608) {
            return null;
        }
        return sStatic.mDefaultAudioVideo;
    }

    public boolean isRouteAvailable(int types, int flags) {
        int count = sStatic.mRoutes.size();
        for (int i = 0; i < count; i++) {
            RouteInfo route = sStatic.mRoutes.get(i);
            if (route.matchesTypes(types) && ((flags & 1) == 0 || route != sStatic.mDefaultAudioVideo)) {
                return true;
            }
        }
        return false;
    }

    public void addCallback(int types, Callback cb) {
        addCallback(types, cb, 0);
    }

    public void addCallback(int types, Callback cb, int flags) {
        int index = findCallbackInfo(cb);
        if (index >= 0) {
            CallbackInfo info = sStatic.mCallbacks.get(index);
            info.type |= types;
            info.flags |= flags;
        } else {
            sStatic.mCallbacks.add(new CallbackInfo(cb, types, flags, this));
        }
        sStatic.updateDiscoveryRequest();
    }

    public void removeCallback(Callback cb) {
        int index = findCallbackInfo(cb);
        if (index >= 0) {
            sStatic.mCallbacks.remove(index);
            sStatic.updateDiscoveryRequest();
        } else {
            Log.w(TAG, "removeCallback(" + cb + "): callback not registered");
        }
    }

    private int findCallbackInfo(Callback cb) {
        int count = sStatic.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            CallbackInfo info = sStatic.mCallbacks.get(i);
            if (info.cb == cb) {
                return i;
            }
        }
        return -1;
    }

    public void selectRoute(int types, RouteInfo route) {
        if (route == null) {
            throw new IllegalArgumentException("Route cannot be null.");
        }
        selectRouteStatic(types, route, true);
    }

    public void selectRouteInt(int types, RouteInfo route, boolean explicit) {
        selectRouteStatic(types, route, explicit);
    }

    static void selectRouteStatic(int types, RouteInfo route, boolean explicit) {
        Log.v(TAG, "Selecting route: " + route);
        if (!f14assertionsDisabled) {
            if (!(route != null)) {
                throw new AssertionError();
            }
        }
        if (DEBUG) {
            Log.d(TAG, "selectRouteStatic types: " + types + " route: " + route + " explicit: " + explicit);
        }
        RouteInfo oldRoute = sStatic.mSelectedRoute;
        if (!route.matchesTypes(types)) {
            Log.w(TAG, "selectRoute ignored; cannot select route with supported types " + typesToString(route.getSupportedTypes()) + " into route types " + typesToString(types));
            return;
        }
        WifiDisplay activeDisplay = sStatic.mDisplayService.getWifiDisplayStatus().getActiveDisplay();
        boolean oldRouteHasAddress = (oldRoute == null || oldRoute.mDeviceAddress == null) ? false : true;
        boolean newRouteHasAddress = route.mDeviceAddress != null;
        boolean shouldConnectWfd = false;
        boolean shouldDisconnectWfd = false;
        if (activeDisplay != null || oldRouteHasAddress || newRouteHasAddress) {
            if (!newRouteHasAddress || matchesDeviceAddress(activeDisplay, route)) {
                if (activeDisplay != null && !newRouteHasAddress) {
                    shouldDisconnectWfd = true;
                }
            } else if (sStatic.mCanConfigureWifiDisplays) {
                shouldConnectWfd = true;
            } else {
                Log.e(TAG, "Cannot connect to wifi displays because this process is not allowed to do so.");
            }
        }
        if (DEBUG) {
            Log.d(TAG, "selectRouteStatic shouldConnectWfd: " + shouldConnectWfd + " shouldDisconnectWfd: " + shouldDisconnectWfd);
        }
        if (oldRoute == route) {
            if (!shouldConnectWfd && !shouldDisconnectWfd) {
                if (DEBUG) {
                    Log.d(TAG, "selectRouteStatic route2 is already selected, skip it");
                    return;
                }
                return;
            } else if (oldRoute.getStatusCode() == 2 || oldRoute.getStatusCode() == 6) {
                if (DEBUG) {
                    Log.d(TAG, "selectRouteStatic route1 is already selected, skip it");
                    return;
                }
                return;
            }
        }
        RouteInfo btRoute = sStatic.mBluetoothA2dpRoute;
        if (btRoute != null && (types & 1) != 0 && (route == btRoute || route == sStatic.mDefaultAudioVideo)) {
            try {
                if (sStatic.mAudioService.isBluetoothA2dpOn()) {
                    if (route == btRoute) {
                        shouldDisconnectWfd = false;
                    }
                    Log.d(TAG, "selectRouteStatic shouldDisconnectWfd: " + shouldDisconnectWfd);
                } else {
                    Log.d(TAG, "selectRouteStatic: a2dp off with btRoute = " + btRoute);
                    sStatic.mAudioService.setBluetoothA2dpOn(route == btRoute);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error changing Bluetooth A2DP state", e);
            }
        }
        if (shouldConnectWfd) {
            sStatic.mDisplayService.connectWifiDisplay(route.mDeviceAddress);
        } else if (shouldDisconnectWfd) {
            sStatic.mDisplayService.disconnectWifiDisplay();
        }
        sStatic.setSelectedRoute(route, explicit);
        if (oldRoute != null) {
            if (DEBUG) {
                Log.d(TAG, "selectRouteStatic oldRoute: " + oldRoute);
            }
            dispatchRouteUnselected(oldRoute.getSupportedTypes() & types, oldRoute);
            if (oldRoute.resolveStatusCode()) {
                dispatchRouteChanged(oldRoute);
            }
        }
        if (route != null) {
            if (DEBUG) {
                Log.d(TAG, "selectRouteStatic newRoute: " + route);
            }
            if (shouldConnectWfd || shouldDisconnectWfd) {
                if (route.resolveStatusCodeForWfd()) {
                    dispatchRouteChanged(route);
                }
            } else if (route.resolveStatusCode()) {
                dispatchRouteChanged(route);
            }
            dispatchRouteSelected(route.getSupportedTypes() & types, route);
        }
        sStatic.updateDiscoveryRequest();
    }

    static void selectDefaultRouteStatic() {
        if (sStatic.mSelectedRoute != sStatic.mBluetoothA2dpRoute && sStatic.mBluetoothA2dpRoute != null && sStatic.isBluetoothA2dpOn()) {
            selectRouteStatic(ROUTE_TYPE_ANY, sStatic.mBluetoothA2dpRoute, false);
        } else {
            selectRouteStatic(ROUTE_TYPE_ANY, sStatic.mDefaultAudioVideo, false);
        }
    }

    static boolean matchesDeviceAddress(WifiDisplay display, RouteInfo info) {
        boolean routeHasAddress = (info == null || info.mDeviceAddress == null) ? false : true;
        if (display == null && !routeHasAddress) {
            return true;
        }
        if (display != null && routeHasAddress) {
            return display.getDeviceAddress().equals(info.mDeviceAddress);
        }
        return false;
    }

    public void addUserRoute(UserRouteInfo info) {
        addRouteStatic(info);
    }

    public void addRouteInt(RouteInfo info) {
        addRouteStatic(info);
    }

    static void addRouteStatic(RouteInfo info) {
        Log.v(TAG, "Adding route: " + info);
        RouteCategory cat = info.getCategory();
        if (!sStatic.mCategories.contains(cat)) {
            sStatic.mCategories.add(cat);
        }
        if (cat.isGroupable() && !(info instanceof RouteGroup)) {
            RouteGroup group = new RouteGroup(info.getCategory());
            group.mSupportedTypes = info.mSupportedTypes;
            sStatic.mRoutes.add(group);
            dispatchRouteAdded(group);
            group.addRoute(info);
            return;
        }
        sStatic.mRoutes.add(info);
        dispatchRouteAdded(info);
    }

    public void removeUserRoute(UserRouteInfo info) {
        removeRouteStatic(info);
    }

    public void clearUserRoutes() {
        int i = 0;
        while (i < sStatic.mRoutes.size()) {
            RouteInfo info = sStatic.mRoutes.get(i);
            if ((info instanceof UserRouteInfo) || (info instanceof RouteGroup)) {
                removeRouteStatic(info);
                i--;
            }
            i++;
        }
    }

    public void removeRouteInt(RouteInfo info) {
        removeRouteStatic(info);
    }

    static void removeRouteStatic(RouteInfo info) {
        Log.v(TAG, "Removing route: " + info);
        if (!sStatic.mRoutes.remove(info)) {
            return;
        }
        RouteCategory removingCat = info.getCategory();
        int count = sStatic.mRoutes.size();
        boolean found = false;
        int i = 0;
        while (true) {
            if (i >= count) {
                break;
            }
            RouteCategory cat = sStatic.mRoutes.get(i).getCategory();
            if (removingCat != cat) {
                i++;
            } else {
                found = true;
                break;
            }
        }
        if (info.isSelected()) {
            selectDefaultRouteStatic();
        }
        if (!found) {
            sStatic.mCategories.remove(removingCat);
        }
        dispatchRouteRemoved(info);
    }

    public int getCategoryCount() {
        return sStatic.mCategories.size();
    }

    public RouteCategory getCategoryAt(int index) {
        return sStatic.mCategories.get(index);
    }

    public int getRouteCount() {
        return sStatic.mRoutes.size();
    }

    public RouteInfo getRouteAt(int index) {
        return sStatic.mRoutes.get(index);
    }

    static int getRouteCountStatic() {
        return sStatic.mRoutes.size();
    }

    static RouteInfo getRouteAtStatic(int index) {
        return sStatic.mRoutes.get(index);
    }

    public UserRouteInfo createUserRoute(RouteCategory category) {
        return new UserRouteInfo(category);
    }

    public RouteCategory createRouteCategory(CharSequence name, boolean isGroupable) {
        return new RouteCategory(name, 8388608, isGroupable);
    }

    public RouteCategory createRouteCategory(int nameResId, boolean isGroupable) {
        return new RouteCategory(nameResId, 8388608, isGroupable);
    }

    public void rebindAsUser(int userId) {
        sStatic.rebindAsUser(userId);
    }

    static void updateRoute(RouteInfo info) {
        dispatchRouteChanged(info);
    }

    static void dispatchRouteSelected(int type, RouteInfo info) {
        if (DEBUG) {
            Log.d(TAG, "dispatchRouteSelected info: " + info + " type: " + type);
        }
        for (CallbackInfo cbi : sStatic.mCallbacks) {
            if (cbi.filterRouteEvent(info)) {
                cbi.cb.onRouteSelected(cbi.router, type, info);
            }
        }
    }

    static void dispatchRouteUnselected(int type, RouteInfo info) {
        if (DEBUG) {
            Log.d(TAG, "dispatchRouteUnselected info: " + info + " type: " + type);
        }
        for (CallbackInfo cbi : sStatic.mCallbacks) {
            if (cbi.filterRouteEvent(info)) {
                cbi.cb.onRouteUnselected(cbi.router, type, info);
            }
        }
    }

    static void dispatchRouteChanged(RouteInfo info) {
        dispatchRouteChanged(info, info.mSupportedTypes);
    }

    static void dispatchRouteChanged(RouteInfo info, int oldSupportedTypes) {
        Log.v(TAG, "Dispatching route change: " + info);
        int newSupportedTypes = info.mSupportedTypes;
        for (CallbackInfo cbi : sStatic.mCallbacks) {
            boolean oldVisibility = cbi.filterRouteEvent(oldSupportedTypes);
            boolean newVisibility = cbi.filterRouteEvent(newSupportedTypes);
            if (DEBUG) {
                Log.d(TAG, "dispatchRouteChanged oldVisibility: " + oldVisibility + "newVisibility: " + newVisibility);
            }
            if (!oldVisibility && newVisibility) {
                cbi.cb.onRouteAdded(cbi.router, info);
                if (info.isSelected()) {
                    cbi.cb.onRouteSelected(cbi.router, newSupportedTypes, info);
                }
            }
            if (oldVisibility || newVisibility) {
                cbi.cb.onRouteChanged(cbi.router, info);
            }
            if (oldVisibility && !newVisibility) {
                if (info.isSelected()) {
                    cbi.cb.onRouteUnselected(cbi.router, oldSupportedTypes, info);
                }
                cbi.cb.onRouteRemoved(cbi.router, info);
            }
        }
    }

    static void dispatchRouteAdded(RouteInfo info) {
        for (CallbackInfo cbi : sStatic.mCallbacks) {
            if (cbi.filterRouteEvent(info)) {
                cbi.cb.onRouteAdded(cbi.router, info);
            }
        }
    }

    static void dispatchRouteRemoved(RouteInfo info) {
        for (CallbackInfo cbi : sStatic.mCallbacks) {
            if (cbi.filterRouteEvent(info)) {
                cbi.cb.onRouteRemoved(cbi.router, info);
            }
        }
    }

    static void dispatchRouteGrouped(RouteInfo info, RouteGroup group, int index) {
        for (CallbackInfo cbi : sStatic.mCallbacks) {
            if (cbi.filterRouteEvent(group)) {
                cbi.cb.onRouteGrouped(cbi.router, info, group, index);
            }
        }
    }

    static void dispatchRouteUngrouped(RouteInfo info, RouteGroup group) {
        for (CallbackInfo cbi : sStatic.mCallbacks) {
            if (cbi.filterRouteEvent(group)) {
                cbi.cb.onRouteUngrouped(cbi.router, info, group);
            }
        }
    }

    static void dispatchRouteVolumeChanged(RouteInfo info) {
        for (CallbackInfo cbi : sStatic.mCallbacks) {
            if (cbi.filterRouteEvent(info)) {
                cbi.cb.onRouteVolumeChanged(cbi.router, info);
            }
        }
    }

    static void dispatchRoutePresentationDisplayChanged(RouteInfo info) {
        for (CallbackInfo cbi : sStatic.mCallbacks) {
            if (cbi.filterRouteEvent(info)) {
                cbi.cb.onRoutePresentationDisplayChanged(cbi.router, info);
            }
        }
    }

    static void systemVolumeChanged(int newValue) {
        RouteInfo selectedRoute = sStatic.mSelectedRoute;
        if (selectedRoute == null) {
            return;
        }
        if (selectedRoute == sStatic.mBluetoothA2dpRoute || selectedRoute == sStatic.mDefaultAudioVideo) {
            dispatchRouteVolumeChanged(selectedRoute);
            return;
        }
        if (sStatic.mBluetoothA2dpRoute != null) {
            try {
                dispatchRouteVolumeChanged(sStatic.mAudioService.isBluetoothA2dpOn() ? sStatic.mBluetoothA2dpRoute : sStatic.mDefaultAudioVideo);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Error checking Bluetooth A2DP state to report volume change", e);
                return;
            }
        }
        dispatchRouteVolumeChanged(sStatic.mDefaultAudioVideo);
    }

    static void updateWifiDisplayStatus(WifiDisplayStatus status) {
        WifiDisplay[] displays;
        WifiDisplay activeDisplay;
        WifiDisplay d;
        if (DEBUG) {
            Log.d(TAG, "updateWifiDisplayStatus status: " + status);
        }
        if (status.getFeatureState() == 3) {
            displays = status.getDisplays();
            activeDisplay = status.getActiveDisplay();
            if (!sStatic.mCanConfigureWifiDisplays) {
                displays = activeDisplay != null ? new WifiDisplay[]{activeDisplay} : WifiDisplay.EMPTY_ARRAY;
            }
        } else {
            displays = WifiDisplay.EMPTY_ARRAY;
            activeDisplay = null;
        }
        Log.v(TAG, "activeDisplay = " + activeDisplay);
        String deviceAddress = activeDisplay != null ? activeDisplay.getDeviceAddress() : null;
        Log.v(TAG, "activeDisplay = " + activeDisplay + ", activeDisplayAddress = " + deviceAddress + ", displays.length = " + displays.length);
        if (displays.length == 0 || sStatic.mDisplayService.isSinkEnabled()) {
            sStatic.mDefaultAudioVideo.mDeviceAddress = deviceAddress;
        }
        for (WifiDisplay d2 : displays) {
            if (DEBUG) {
                Log.d(TAG, "updateWifiDisplayStatus display: " + d2);
            }
            if (shouldShowWifiDisplay(d2, activeDisplay)) {
                RouteInfo route = findWifiDisplayRoute(d2);
                if (route == null) {
                    route = makeWifiDisplayRoute(d2, status);
                    addRouteStatic(route);
                } else {
                    String address = d2.getDeviceAddress();
                    boolean disconnected = !address.equals(deviceAddress) ? address.equals(sStatic.mPreviousActiveWifiDisplayAddress) : false;
                    updateWifiDisplayRoute(route, d2, status, disconnected);
                }
                if (d2.equals(activeDisplay)) {
                    selectRouteStatic(route.getSupportedTypes(), route, false);
                }
            }
        }
        int i = sStatic.mRoutes.size();
        while (true) {
            int i2 = i;
            i = i2 - 1;
            if (i2 <= 0) {
                sStatic.mPreviousActiveWifiDisplayAddress = deviceAddress;
                return;
            }
            RouteInfo route2 = sStatic.mRoutes.get(i);
            if (route2.mDeviceAddress != null && ((d = findWifiDisplay(displays, route2.mDeviceAddress)) == null || !shouldShowWifiDisplay(d, activeDisplay))) {
                removeRouteStatic(route2);
            }
        }
    }

    private static boolean shouldShowWifiDisplay(WifiDisplay d, WifiDisplay activeDisplay) {
        if (d.isRemembered()) {
            return true;
        }
        return d.equals(activeDisplay);
    }

    static int getWifiDisplayStatusCode(WifiDisplay d, WifiDisplayStatus wfdStatus) {
        int newStatus;
        if (wfdStatus.getScanState() == 1) {
            newStatus = 1;
        } else if (d.isAvailable()) {
            newStatus = d.canConnect() ? 3 : 5;
        } else {
            newStatus = 4;
        }
        if (d.equals(wfdStatus.getActiveDisplay())) {
            int activeState = wfdStatus.getActiveDisplayState();
            switch (activeState) {
                case 0:
                    Log.e(TAG, "Active display is not connected!");
                    break;
            }
            return newStatus;
        }
        return newStatus;
    }

    static boolean isWifiDisplayEnabled(WifiDisplay d, WifiDisplayStatus wfdStatus) {
        if (!d.isAvailable()) {
            return false;
        }
        if (d.canConnect()) {
            return true;
        }
        return d.equals(wfdStatus.getActiveDisplay());
    }

    static RouteInfo makeWifiDisplayRoute(WifiDisplay display, WifiDisplayStatus wfdStatus) {
        RouteInfo newRoute = new RouteInfo(sStatic.mSystemCategory);
        newRoute.mDeviceAddress = display.getDeviceAddress();
        newRoute.mSupportedTypes = 7;
        newRoute.mVolumeHandling = 0;
        newRoute.mPlaybackType = 1;
        newRoute.setRealStatusCode(getWifiDisplayStatusCode(display, wfdStatus));
        newRoute.mEnabled = isWifiDisplayEnabled(display, wfdStatus);
        newRoute.mName = display.getFriendlyDisplayName();
        newRoute.mDescription = sStatic.mResources.getText(17040623);
        newRoute.updatePresentationDisplay();
        newRoute.mDeviceType = 1;
        return newRoute;
    }

    private static void updateWifiDisplayRoute(RouteInfo route, WifiDisplay display, WifiDisplayStatus wfdStatus, boolean disconnected) {
        boolean changed = false;
        String newName = display.getFriendlyDisplayName();
        if (!route.getName().equals(newName)) {
            route.mName = newName;
            changed = true;
        }
        boolean enabled = isWifiDisplayEnabled(display, wfdStatus);
        boolean changed2 = changed | (route.mEnabled != enabled);
        route.mEnabled = enabled;
        if (DEBUG) {
            Log.d(TAG, "updateWifiDisplayRoute changed: " + changed2 + " enabled: " + enabled + "  route.isSelected(): " + route.isSelected() + " route: " + route);
        }
        if (changed2 | route.setRealStatusCodeForWfd(getWifiDisplayStatusCode(display, wfdStatus))) {
            dispatchRouteChanged(route);
        }
        if ((!enabled || disconnected) && route.isSelected()) {
            selectDefaultRouteStatic();
        }
    }

    private static WifiDisplay findWifiDisplay(WifiDisplay[] displays, String deviceAddress) {
        for (WifiDisplay d : displays) {
            if (d.getDeviceAddress().equals(deviceAddress)) {
                return d;
            }
        }
        return null;
    }

    private static RouteInfo findWifiDisplayRoute(WifiDisplay d) {
        int count = sStatic.mRoutes.size();
        for (int i = 0; i < count; i++) {
            RouteInfo info = sStatic.mRoutes.get(i);
            if (d.getDeviceAddress().equals(info.mDeviceAddress)) {
                return info;
            }
        }
        return null;
    }

    public static class RouteInfo {
        public static final int DEVICE_TYPE_BLUETOOTH = 3;
        public static final int DEVICE_TYPE_SPEAKER = 2;
        public static final int DEVICE_TYPE_TV = 1;
        public static final int DEVICE_TYPE_UNKNOWN = 0;
        public static final int PLAYBACK_TYPE_LOCAL = 0;
        public static final int PLAYBACK_TYPE_REMOTE = 1;
        public static final int PLAYBACK_VOLUME_FIXED = 0;
        public static final int PLAYBACK_VOLUME_VARIABLE = 1;
        public static final int STATUS_AVAILABLE = 3;
        public static final int STATUS_CONNECTED = 6;
        public static final int STATUS_CONNECTING = 2;
        public static final int STATUS_IN_USE = 5;
        public static final int STATUS_NONE = 0;
        public static final int STATUS_NOT_AVAILABLE = 4;
        public static final int STATUS_SCANNING = 1;
        final RouteCategory mCategory;
        CharSequence mDescription;
        String mDeviceAddress;
        String mGlobalRouteId;
        RouteGroup mGroup;
        Drawable mIcon;
        CharSequence mName;
        int mNameResId;
        Display mPresentationDisplay;
        private int mRealStatusCode;
        private int mResolvedStatusCode;
        private CharSequence mStatus;
        int mSupportedTypes;
        private Object mTag;
        VolumeCallbackInfo mVcb;
        int mPlaybackType = 0;
        int mVolumeMax = 15;
        int mVolume = 15;
        int mVolumeHandling = 1;
        int mPlaybackStream = 3;
        int mPresentationDisplayId = -1;
        boolean mEnabled = true;
        final IRemoteVolumeObserver.Stub mRemoteVolObserver = new IRemoteVolumeObserver.Stub() {
            @Override
            public void dispatchRemoteVolumeUpdate(final int direction, final int value) {
                MediaRouter.sStatic.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (RouteInfo.this.mVcb == null) {
                            return;
                        }
                        if (direction != 0) {
                            RouteInfo.this.mVcb.vcb.onVolumeUpdateRequest(RouteInfo.this.mVcb.route, direction);
                        } else {
                            RouteInfo.this.mVcb.vcb.onVolumeSetRequest(RouteInfo.this.mVcb.route, value);
                        }
                    }
                });
            }
        };
        int mDeviceType = 0;

        RouteInfo(RouteCategory category) {
            this.mCategory = category;
        }

        public CharSequence getName() {
            return getName(MediaRouter.sStatic.mResources);
        }

        public CharSequence getName(Context context) {
            return getName(context.getResources());
        }

        CharSequence getName(Resources res) {
            if (this.mNameResId != 0) {
                CharSequence text = res.getText(this.mNameResId);
                this.mName = text;
                return text;
            }
            return this.mName;
        }

        public CharSequence getDescription() {
            return this.mDescription;
        }

        public CharSequence getStatus() {
            return this.mStatus;
        }

        boolean setRealStatusCode(int statusCode) {
            if (this.mRealStatusCode != statusCode) {
                this.mRealStatusCode = statusCode;
                return resolveStatusCode();
            }
            return false;
        }

        boolean resolveStatusCode() {
            int resId;
            int statusCode = this.mRealStatusCode;
            if (isSelected()) {
                switch (statusCode) {
                    case 1:
                    case 3:
                        statusCode = 2;
                        break;
                }
            }
            if (this.mResolvedStatusCode == statusCode) {
                return false;
            }
            this.mResolvedStatusCode = statusCode;
            switch (statusCode) {
                case 1:
                    resId = 17040630;
                    break;
                case 2:
                    resId = 17040631;
                    break;
                case 3:
                    resId = 17040632;
                    break;
                case 4:
                    resId = 17040633;
                    break;
                case 5:
                    resId = 17040634;
                    break;
                default:
                    resId = 0;
                    break;
            }
            this.mStatus = resId != 0 ? MediaRouter.sStatic.mResources.getText(resId) : null;
            return true;
        }

        boolean setRealStatusCodeForWfd(int statusCode) {
            if (MediaRouter.DEBUG) {
                Log.d(MediaRouter.TAG, "resolveStatusCode statusCode: " + statusCode + " mRealStatusCode: " + this.mRealStatusCode);
            }
            if (this.mRealStatusCode != statusCode) {
                this.mRealStatusCode = statusCode;
                return resolveStatusCodeForWfd();
            }
            return false;
        }

        boolean resolveStatusCodeForWfd() {
            int resId;
            int statusCode = this.mRealStatusCode;
            if (MediaRouter.DEBUG) {
                Log.d(MediaRouter.TAG, "resolveStatusCode isSelected: " + isSelected() + " mRealStatusCode: " + this.mRealStatusCode + " this: " + this);
            }
            if (this.mResolvedStatusCode == statusCode) {
                return false;
            }
            this.mResolvedStatusCode = statusCode;
            switch (statusCode) {
                case 1:
                    resId = 17040630;
                    break;
                case 2:
                    resId = 17040631;
                    break;
                case 3:
                    resId = 17040632;
                    break;
                case 4:
                    resId = 17040633;
                    break;
                case 5:
                    resId = 17040634;
                    break;
                default:
                    resId = 0;
                    break;
            }
            this.mStatus = resId != 0 ? MediaRouter.sStatic.mResources.getText(resId) : null;
            return true;
        }

        public int getStatusCode() {
            return this.mResolvedStatusCode;
        }

        public int getSupportedTypes() {
            return this.mSupportedTypes;
        }

        public int getDeviceType() {
            return this.mDeviceType;
        }

        public boolean matchesTypes(int types) {
            return (this.mSupportedTypes & types) != 0;
        }

        public RouteGroup getGroup() {
            return this.mGroup;
        }

        public RouteCategory getCategory() {
            return this.mCategory;
        }

        public Drawable getIconDrawable() {
            return this.mIcon;
        }

        public void setTag(Object tag) {
            this.mTag = tag;
            routeUpdated();
        }

        public Object getTag() {
            return this.mTag;
        }

        public int getPlaybackType() {
            return this.mPlaybackType;
        }

        public int getPlaybackStream() {
            return this.mPlaybackStream;
        }

        public int getVolume() {
            if (this.mPlaybackType == 0) {
                try {
                    int vol = MediaRouter.sStatic.mAudioService.getStreamVolume(this.mPlaybackStream);
                    return vol;
                } catch (RemoteException e) {
                    Log.e(MediaRouter.TAG, "Error getting local stream volume", e);
                    return 0;
                }
            }
            return this.mVolume;
        }

        public void requestSetVolume(int volume) {
            if (this.mPlaybackType == 0) {
                try {
                    MediaRouter.sStatic.mAudioService.setStreamVolume(this.mPlaybackStream, volume, 0, ActivityThread.currentPackageName());
                    return;
                } catch (RemoteException e) {
                    Log.e(MediaRouter.TAG, "Error setting local stream volume", e);
                    return;
                }
            }
            MediaRouter.sStatic.requestSetVolume(this, volume);
        }

        public void requestUpdateVolume(int direction) {
            if (this.mPlaybackType == 0) {
                try {
                    int volume = Math.max(0, Math.min(getVolume() + direction, getVolumeMax()));
                    MediaRouter.sStatic.mAudioService.setStreamVolume(this.mPlaybackStream, volume, 0, ActivityThread.currentPackageName());
                    return;
                } catch (RemoteException e) {
                    Log.e(MediaRouter.TAG, "Error setting local stream volume", e);
                    return;
                }
            }
            MediaRouter.sStatic.requestUpdateVolume(this, direction);
        }

        public int getVolumeMax() {
            if (this.mPlaybackType == 0) {
                try {
                    int volMax = MediaRouter.sStatic.mAudioService.getStreamMaxVolume(this.mPlaybackStream);
                    return volMax;
                } catch (RemoteException e) {
                    Log.e(MediaRouter.TAG, "Error getting local stream volume", e);
                    return 0;
                }
            }
            return this.mVolumeMax;
        }

        public int getVolumeHandling() {
            return this.mVolumeHandling;
        }

        public Display getPresentationDisplay() {
            return this.mPresentationDisplay;
        }

        boolean updatePresentationDisplay() {
            Display display = choosePresentationDisplay();
            if (this.mPresentationDisplay != display) {
                this.mPresentationDisplay = display;
                return true;
            }
            return false;
        }

        private Display choosePresentationDisplay() {
            int i = 0;
            if ((this.mSupportedTypes & 2) != 0) {
                Display[] displays = MediaRouter.sStatic.getAllPresentationDisplays();
                if (this.mPresentationDisplayId >= 0) {
                    int length = displays.length;
                    while (i < length) {
                        Display display = displays[i];
                        if (display.getDisplayId() != this.mPresentationDisplayId) {
                            i++;
                        } else {
                            return display;
                        }
                    }
                    return null;
                }
                if (this.mDeviceAddress != null) {
                    int length2 = displays.length;
                    while (i < length2) {
                        Display display2 = displays[i];
                        if (display2.getType() != 3 || !this.mDeviceAddress.equals(display2.getAddress())) {
                            i++;
                        } else {
                            return display2;
                        }
                    }
                    return null;
                }
                if (this == MediaRouter.sStatic.mDefaultAudioVideo && displays.length > 0) {
                    return displays[0];
                }
            }
            return null;
        }

        public String getDeviceAddress() {
            return this.mDeviceAddress;
        }

        public boolean isEnabled() {
            return this.mEnabled;
        }

        public boolean isConnecting() {
            return this.mResolvedStatusCode == 2;
        }

        public boolean isSelected() {
            return this == MediaRouter.sStatic.mSelectedRoute;
        }

        public boolean isDefault() {
            return this == MediaRouter.sStatic.mDefaultAudioVideo;
        }

        public void select() {
            MediaRouter.selectRouteStatic(this.mSupportedTypes, this, true);
        }

        void setStatusInt(CharSequence status) {
            if (status.equals(this.mStatus)) {
                return;
            }
            this.mStatus = status;
            if (this.mGroup != null) {
                this.mGroup.memberStatusChanged(this, status);
            }
            routeUpdated();
        }

        void routeUpdated() {
            MediaRouter.updateRoute(this);
        }

        public String toString() {
            String supportedTypes = MediaRouter.typesToString(getSupportedTypes());
            return getClass().getSimpleName() + "{ name=" + getName() + ", description=" + getDescription() + ", status=" + getStatus() + ", category=" + getCategory() + ", supportedTypes=" + supportedTypes + ", presentationDisplay=" + this.mPresentationDisplay + " }";
        }
    }

    public static class UserRouteInfo extends RouteInfo {
        RemoteControlClient mRcc;
        SessionVolumeProvider mSvp;

        UserRouteInfo(RouteCategory category) {
            super(category);
            this.mSupportedTypes = 8388608;
            this.mPlaybackType = 1;
            this.mVolumeHandling = 0;
        }

        public void setName(CharSequence name) {
            this.mName = name;
            routeUpdated();
        }

        public void setName(int resId) {
            this.mNameResId = resId;
            this.mName = null;
            routeUpdated();
        }

        public void setDescription(CharSequence description) {
            this.mDescription = description;
            routeUpdated();
        }

        public void setStatus(CharSequence status) {
            setStatusInt(status);
        }

        public void setRemoteControlClient(RemoteControlClient rcc) {
            this.mRcc = rcc;
            updatePlaybackInfoOnRcc();
        }

        public RemoteControlClient getRemoteControlClient() {
            return this.mRcc;
        }

        public void setIconDrawable(Drawable icon) {
            this.mIcon = icon;
        }

        public void setIconResource(int resId) {
            setIconDrawable(MediaRouter.sStatic.mResources.getDrawable(resId));
        }

        public void setVolumeCallback(VolumeCallback vcb) {
            this.mVcb = new VolumeCallbackInfo(vcb, this);
        }

        public void setPlaybackType(int type) {
            if (this.mPlaybackType == type) {
                return;
            }
            this.mPlaybackType = type;
            configureSessionVolume();
        }

        public void setVolumeHandling(int volumeHandling) {
            if (this.mVolumeHandling == volumeHandling) {
                return;
            }
            this.mVolumeHandling = volumeHandling;
            configureSessionVolume();
        }

        public void setVolume(int volume) {
            int volume2 = Math.max(0, Math.min(volume, getVolumeMax()));
            if (this.mVolume == volume2) {
                return;
            }
            this.mVolume = volume2;
            if (this.mSvp != null) {
                this.mSvp.setCurrentVolume(this.mVolume);
            }
            MediaRouter.dispatchRouteVolumeChanged(this);
            if (this.mGroup == null) {
                return;
            }
            this.mGroup.memberVolumeChanged(this);
        }

        @Override
        public void requestSetVolume(int volume) {
            if (this.mVolumeHandling != 1) {
                return;
            }
            if (this.mVcb == null) {
                Log.e(MediaRouter.TAG, "Cannot requestSetVolume on user route - no volume callback set");
            } else {
                this.mVcb.vcb.onVolumeSetRequest(this, volume);
            }
        }

        @Override
        public void requestUpdateVolume(int direction) {
            if (this.mVolumeHandling != 1) {
                return;
            }
            if (this.mVcb == null) {
                Log.e(MediaRouter.TAG, "Cannot requestChangeVolume on user route - no volumec callback set");
            } else {
                this.mVcb.vcb.onVolumeUpdateRequest(this, direction);
            }
        }

        public void setVolumeMax(int volumeMax) {
            if (this.mVolumeMax == volumeMax) {
                return;
            }
            this.mVolumeMax = volumeMax;
            configureSessionVolume();
        }

        public void setPlaybackStream(int stream) {
            if (this.mPlaybackStream == stream) {
                return;
            }
            this.mPlaybackStream = stream;
            configureSessionVolume();
        }

        private void updatePlaybackInfoOnRcc() {
            configureSessionVolume();
        }

        private void configureSessionVolume() {
            if (this.mRcc == null) {
                if (MediaRouter.DEBUG) {
                    Log.d(MediaRouter.TAG, "No Rcc to configure volume for route " + this.mName);
                    return;
                }
                return;
            }
            MediaSession session = this.mRcc.getMediaSession();
            if (session == null) {
                if (MediaRouter.DEBUG) {
                    Log.d(MediaRouter.TAG, "Rcc has no session to configure volume");
                    return;
                }
                return;
            }
            if (this.mPlaybackType == 1) {
                int volumeControl = 0;
                switch (this.mVolumeHandling) {
                    case 1:
                        volumeControl = 2;
                        break;
                }
                if (this.mSvp != null && this.mSvp.getVolumeControl() == volumeControl && this.mSvp.getMaxVolume() == this.mVolumeMax) {
                    return;
                }
                this.mSvp = new SessionVolumeProvider(volumeControl, this.mVolumeMax, this.mVolume);
                session.setPlaybackToRemote(this.mSvp);
                return;
            }
            AudioAttributes.Builder bob = new AudioAttributes.Builder();
            bob.setLegacyStreamType(this.mPlaybackStream);
            session.setPlaybackToLocal(bob.build());
            this.mSvp = null;
        }

        class SessionVolumeProvider extends VolumeProvider {
            public SessionVolumeProvider(int volumeControl, int maxVolume, int currentVolume) {
                super(volumeControl, maxVolume, currentVolume);
            }

            @Override
            public void onSetVolumeTo(final int volume) {
                MediaRouter.sStatic.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (UserRouteInfo.this.mVcb == null) {
                            return;
                        }
                        UserRouteInfo.this.mVcb.vcb.onVolumeSetRequest(UserRouteInfo.this.mVcb.route, volume);
                    }
                });
            }

            @Override
            public void onAdjustVolume(final int direction) {
                MediaRouter.sStatic.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (UserRouteInfo.this.mVcb == null) {
                            return;
                        }
                        UserRouteInfo.this.mVcb.vcb.onVolumeUpdateRequest(UserRouteInfo.this.mVcb.route, direction);
                    }
                });
            }
        }
    }

    public static class RouteGroup extends RouteInfo {
        final ArrayList<RouteInfo> mRoutes;
        private boolean mUpdateName;

        RouteGroup(RouteCategory category) {
            super(category);
            this.mRoutes = new ArrayList<>();
            this.mGroup = this;
            this.mVolumeHandling = 0;
        }

        @Override
        CharSequence getName(Resources res) {
            if (this.mUpdateName) {
                updateName();
            }
            return super.getName(res);
        }

        public void addRoute(RouteInfo route) {
            if (route.getGroup() != null) {
                throw new IllegalStateException("Route " + route + " is already part of a group.");
            }
            if (route.getCategory() != this.mCategory) {
                throw new IllegalArgumentException("Route cannot be added to a group with a different category. (Route category=" + route.getCategory() + " group category=" + this.mCategory + ")");
            }
            int at = this.mRoutes.size();
            this.mRoutes.add(route);
            route.mGroup = this;
            this.mUpdateName = true;
            updateVolume();
            routeUpdated();
            MediaRouter.dispatchRouteGrouped(route, this, at);
        }

        public void addRoute(RouteInfo route, int insertAt) {
            if (route.getGroup() != null) {
                throw new IllegalStateException("Route " + route + " is already part of a group.");
            }
            if (route.getCategory() != this.mCategory) {
                throw new IllegalArgumentException("Route cannot be added to a group with a different category. (Route category=" + route.getCategory() + " group category=" + this.mCategory + ")");
            }
            this.mRoutes.add(insertAt, route);
            route.mGroup = this;
            this.mUpdateName = true;
            updateVolume();
            routeUpdated();
            MediaRouter.dispatchRouteGrouped(route, this, insertAt);
        }

        public void removeRoute(RouteInfo route) {
            if (route.getGroup() != this) {
                throw new IllegalArgumentException("Route " + route + " is not a member of this group.");
            }
            this.mRoutes.remove(route);
            route.mGroup = null;
            this.mUpdateName = true;
            updateVolume();
            MediaRouter.dispatchRouteUngrouped(route, this);
            routeUpdated();
        }

        public void removeRoute(int index) {
            RouteInfo route = this.mRoutes.remove(index);
            route.mGroup = null;
            this.mUpdateName = true;
            updateVolume();
            MediaRouter.dispatchRouteUngrouped(route, this);
            routeUpdated();
        }

        public int getRouteCount() {
            return this.mRoutes.size();
        }

        public RouteInfo getRouteAt(int index) {
            return this.mRoutes.get(index);
        }

        public void setIconDrawable(Drawable icon) {
            this.mIcon = icon;
        }

        public void setIconResource(int resId) {
            setIconDrawable(MediaRouter.sStatic.mResources.getDrawable(resId));
        }

        @Override
        public void requestSetVolume(int volume) {
            int maxVol = getVolumeMax();
            if (maxVol == 0) {
                return;
            }
            float scaledVolume = volume / maxVol;
            int routeCount = getRouteCount();
            for (int i = 0; i < routeCount; i++) {
                RouteInfo route = getRouteAt(i);
                int routeVol = (int) (route.getVolumeMax() * scaledVolume);
                route.requestSetVolume(routeVol);
            }
            if (volume == this.mVolume) {
                return;
            }
            this.mVolume = volume;
            MediaRouter.dispatchRouteVolumeChanged(this);
        }

        @Override
        public void requestUpdateVolume(int direction) {
            int maxVol = getVolumeMax();
            if (maxVol == 0) {
                return;
            }
            int routeCount = getRouteCount();
            int volume = 0;
            for (int i = 0; i < routeCount; i++) {
                RouteInfo route = getRouteAt(i);
                route.requestUpdateVolume(direction);
                int routeVol = route.getVolume();
                if (routeVol > volume) {
                    volume = routeVol;
                }
            }
            if (volume == this.mVolume) {
                return;
            }
            this.mVolume = volume;
            MediaRouter.dispatchRouteVolumeChanged(this);
        }

        void memberNameChanged(RouteInfo info, CharSequence name) {
            this.mUpdateName = true;
            routeUpdated();
        }

        void memberStatusChanged(RouteInfo info, CharSequence status) {
            setStatusInt(status);
        }

        void memberVolumeChanged(RouteInfo info) {
            updateVolume();
        }

        void updateVolume() {
            int routeCount = getRouteCount();
            int volume = 0;
            for (int i = 0; i < routeCount; i++) {
                int routeVol = getRouteAt(i).getVolume();
                if (routeVol > volume) {
                    volume = routeVol;
                }
            }
            if (volume == this.mVolume) {
                return;
            }
            this.mVolume = volume;
            MediaRouter.dispatchRouteVolumeChanged(this);
        }

        @Override
        void routeUpdated() {
            int types = 0;
            int count = this.mRoutes.size();
            if (count == 0) {
                MediaRouter.removeRouteStatic(this);
                return;
            }
            int maxVolume = 0;
            boolean isLocal = true;
            boolean isFixedVolume = true;
            for (int i = 0; i < count; i++) {
                RouteInfo route = this.mRoutes.get(i);
                types |= route.mSupportedTypes;
                int routeMaxVolume = route.getVolumeMax();
                if (routeMaxVolume > maxVolume) {
                    maxVolume = routeMaxVolume;
                }
                isLocal &= route.getPlaybackType() == 0;
                isFixedVolume &= route.getVolumeHandling() == 0;
            }
            this.mPlaybackType = isLocal ? 0 : 1;
            this.mVolumeHandling = isFixedVolume ? 0 : 1;
            this.mSupportedTypes = types;
            this.mVolumeMax = maxVolume;
            this.mIcon = count == 1 ? this.mRoutes.get(0).getIconDrawable() : null;
            super.routeUpdated();
        }

        void updateName() {
            StringBuilder sb = new StringBuilder();
            int count = this.mRoutes.size();
            for (int i = 0; i < count; i++) {
                RouteInfo info = this.mRoutes.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(info.mName);
            }
            this.mName = sb.toString();
            this.mUpdateName = false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString());
            sb.append('[');
            int count = this.mRoutes.size();
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(this.mRoutes.get(i));
            }
            sb.append(']');
            return sb.toString();
        }
    }

    public static class RouteCategory {
        final boolean mGroupable;
        boolean mIsSystem;
        CharSequence mName;
        int mNameResId;
        int mTypes;

        RouteCategory(CharSequence name, int types, boolean groupable) {
            this.mName = name;
            this.mTypes = types;
            this.mGroupable = groupable;
        }

        RouteCategory(int nameResId, int types, boolean groupable) {
            this.mNameResId = nameResId;
            this.mTypes = types;
            this.mGroupable = groupable;
        }

        public CharSequence getName() {
            return getName(MediaRouter.sStatic.mResources);
        }

        public CharSequence getName(Context context) {
            return getName(context.getResources());
        }

        CharSequence getName(Resources res) {
            if (this.mNameResId != 0) {
                return res.getText(this.mNameResId);
            }
            return this.mName;
        }

        public List<RouteInfo> getRoutes(List<RouteInfo> out) {
            if (out == null) {
                out = new ArrayList<>();
            } else {
                out.clear();
            }
            int count = MediaRouter.getRouteCountStatic();
            for (int i = 0; i < count; i++) {
                RouteInfo route = MediaRouter.getRouteAtStatic(i);
                if (route.mCategory == this) {
                    out.add(route);
                }
            }
            return out;
        }

        public int getSupportedTypes() {
            return this.mTypes;
        }

        public boolean isGroupable() {
            return this.mGroupable;
        }

        public boolean isSystem() {
            return this.mIsSystem;
        }

        public String toString() {
            return "RouteCategory{ name=" + this.mName + " types=" + MediaRouter.typesToString(this.mTypes) + " groupable=" + this.mGroupable + " }";
        }
    }

    static class CallbackInfo {
        public final Callback cb;
        public int flags;
        public final MediaRouter router;
        public int type;

        public CallbackInfo(Callback cb, int type, int flags, MediaRouter router) {
            this.cb = cb;
            this.type = type;
            this.flags = flags;
            this.router = router;
        }

        public boolean filterRouteEvent(RouteInfo route) {
            return filterRouteEvent(route.mSupportedTypes);
        }

        public boolean filterRouteEvent(int supportedTypes) {
            return ((this.flags & 2) == 0 && (this.type & supportedTypes) == 0) ? false : true;
        }
    }

    public static abstract class Callback {
        public abstract void onRouteAdded(MediaRouter mediaRouter, RouteInfo routeInfo);

        public abstract void onRouteChanged(MediaRouter mediaRouter, RouteInfo routeInfo);

        public abstract void onRouteGrouped(MediaRouter mediaRouter, RouteInfo routeInfo, RouteGroup routeGroup, int i);

        public abstract void onRouteRemoved(MediaRouter mediaRouter, RouteInfo routeInfo);

        public abstract void onRouteSelected(MediaRouter mediaRouter, int i, RouteInfo routeInfo);

        public abstract void onRouteUngrouped(MediaRouter mediaRouter, RouteInfo routeInfo, RouteGroup routeGroup);

        public abstract void onRouteUnselected(MediaRouter mediaRouter, int i, RouteInfo routeInfo);

        public abstract void onRouteVolumeChanged(MediaRouter mediaRouter, RouteInfo routeInfo);

        public void onRoutePresentationDisplayChanged(MediaRouter router, RouteInfo info) {
        }
    }

    public static class SimpleCallback extends Callback {
        @Override
        public void onRouteSelected(MediaRouter router, int type, RouteInfo info) {
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, RouteInfo info) {
        }

        @Override
        public void onRouteAdded(MediaRouter router, RouteInfo info) {
        }

        @Override
        public void onRouteRemoved(MediaRouter router, RouteInfo info) {
        }

        @Override
        public void onRouteChanged(MediaRouter router, RouteInfo info) {
        }

        @Override
        public void onRouteGrouped(MediaRouter router, RouteInfo info, RouteGroup group, int index) {
        }

        @Override
        public void onRouteUngrouped(MediaRouter router, RouteInfo info, RouteGroup group) {
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, RouteInfo info) {
        }
    }

    static class VolumeCallbackInfo {
        public final RouteInfo route;
        public final VolumeCallback vcb;

        public VolumeCallbackInfo(VolumeCallback vcb, RouteInfo route) {
            this.vcb = vcb;
            this.route = route;
        }
    }

    static class VolumeChangeReceiver extends BroadcastReceiver {
        VolumeChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(AudioManager.VOLUME_CHANGED_ACTION)) {
                return;
            }
            int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
            if (streamType != 3) {
                return;
            }
            int newVolume = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0);
            int oldVolume = intent.getIntExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, 0);
            if (newVolume == oldVolume) {
                return;
            }
            MediaRouter.systemVolumeChanged(newVolume);
        }
    }

    static class WifiDisplayStatusChangedReceiver extends BroadcastReceiver {
        WifiDisplayStatusChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED)) {
                return;
            }
            MediaRouter.updateWifiDisplayStatus((WifiDisplayStatus) intent.getParcelableExtra(DisplayManager.EXTRA_WIFI_DISPLAY_STATUS));
        }
    }
}
