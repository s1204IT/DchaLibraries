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
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.SettingsStringUtil;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import com.android.internal.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class MediaRouter {
    static final boolean $assertionsDisabled = false;
    public static final int AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE = 1;
    public static final int CALLBACK_FLAG_PASSIVE_DISCOVERY = 8;
    public static final int CALLBACK_FLAG_PERFORM_ACTIVE_SCAN = 1;
    public static final int CALLBACK_FLAG_REQUEST_DISCOVERY = 4;
    public static final int CALLBACK_FLAG_UNFILTERED_EVENTS = 2;
    private static final boolean DEBUG;
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
        DEBUG = Log.isLoggable(TAG, 3) || !"user".equals(Build.TYPE);
        sRouters = new HashMap<>();
    }

    static class Static implements DisplayManager.DisplayListener {
        boolean mActivelyScanningWifiDisplays;
        RouteInfo mBluetoothA2dpRoute;
        final boolean mCanConfigureWifiDisplays;
        IMediaRouterClient mClient;
        MediaRouterClientState mClientState;
        RouteInfo mDefaultAudioVideo;
        boolean mDiscoverRequestActiveScan;
        int mDiscoveryRequestRouteTypes;
        final DisplayManager mDisplayService;
        final Handler mHandler;
        final String mPackageName;
        String mPreviousActiveWifiDisplayAddress;
        final Resources mResources;
        RouteInfo mSelectedRoute;
        final CopyOnWriteArrayList<CallbackInfo> mCallbacks = new CopyOnWriteArrayList<>();
        final ArrayList<RouteInfo> mRoutes = new ArrayList<>();
        final ArrayList<RouteCategory> mCategories = new ArrayList<>();
        final AudioRoutesInfo mCurAudioRoutesInfo = new AudioRoutesInfo();
        int mCurrentUserId = -1;
        final IAudioRoutesObserver.Stub mAudioRoutesObserver = new IAudioRoutesObserver.Stub() {
            @Override
            public void dispatchAudioRoutesChanged(final AudioRoutesInfo audioRoutesInfo) {
                Static.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Static.this.updateAudioRoutes(audioRoutesInfo);
                    }
                });
            }
        };
        final IAudioService mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        final IMediaRouterService mMediaRouterService = IMediaRouterService.Stub.asInterface(ServiceManager.getService(Context.MEDIA_ROUTER_SERVICE));
        final RouteCategory mSystemCategory = new RouteCategory(R.string.default_audio_route_category_name, 3, false);

        Static(Context context) {
            this.mPackageName = context.getPackageName();
            this.mResources = context.getResources();
            this.mHandler = new Handler(context.getMainLooper());
            this.mDisplayService = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            this.mSystemCategory.mIsSystem = true;
            this.mCanConfigureWifiDisplays = context.checkPermission(Manifest.permission.CONFIGURE_WIFI_DISPLAY, Process.myPid(), Process.myUid()) == 0;
        }

        void startMonitoringRoutes(Context context) throws RemoteException {
            this.mDefaultAudioVideo = new RouteInfo(this.mSystemCategory);
            this.mDefaultAudioVideo.mNameResId = R.string.default_audio_route_name;
            this.mDefaultAudioVideo.mSupportedTypes = 3;
            this.mDefaultAudioVideo.updatePresentationDisplay();
            if (((AudioManager) context.getSystemService("audio")).isVolumeFixed()) {
                this.mDefaultAudioVideo.mVolumeHandling = 0;
            }
            MediaRouter.addRouteStatic(this.mDefaultAudioVideo);
            MediaRouter.updateWifiDisplayStatus(this.mDisplayService.getWifiDisplayStatus());
            context.registerReceiver(new WifiDisplayStatusChangedReceiver(), new IntentFilter(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED));
            context.registerReceiver(new VolumeChangeReceiver(), new IntentFilter(AudioManager.VOLUME_CHANGED_ACTION));
            this.mDisplayService.registerDisplayListener(this, this.mHandler);
            AudioRoutesInfo audioRoutesInfoStartWatchingRoutes = null;
            try {
                audioRoutesInfoStartWatchingRoutes = this.mAudioService.startWatchingRoutes(this.mAudioRoutesObserver);
            } catch (RemoteException e) {
            }
            if (audioRoutesInfoStartWatchingRoutes != null) {
                updateAudioRoutes(audioRoutesInfoStartWatchingRoutes);
            }
            rebindAsUser(UserHandle.myUserId());
            if (this.mSelectedRoute == null) {
                MediaRouter.selectDefaultRouteStatic();
            }
        }

        void updateAudioRoutes(AudioRoutesInfo audioRoutesInfo) {
            boolean z;
            boolean z2;
            int i;
            if (audioRoutesInfo.mainType != this.mCurAudioRoutesInfo.mainType) {
                this.mCurAudioRoutesInfo.mainType = audioRoutesInfo.mainType;
                if ((audioRoutesInfo.mainType & 2) != 0 || (audioRoutesInfo.mainType & 1) != 0) {
                    i = R.string.default_audio_route_name_headphones;
                } else if ((audioRoutesInfo.mainType & 4) != 0) {
                    i = R.string.default_audio_route_name_dock_speakers;
                } else if ((audioRoutesInfo.mainType & 8) != 0) {
                    i = R.string.default_audio_route_name_hdmi;
                } else if ((audioRoutesInfo.mainType & 16) != 0) {
                    i = R.string.default_audio_route_name_usb;
                } else {
                    i = R.string.default_audio_route_name;
                }
                this.mDefaultAudioVideo.mNameResId = i;
                MediaRouter.dispatchRouteChanged(this.mDefaultAudioVideo);
                z2 = (audioRoutesInfo.mainType & 19) != 0;
                z = true;
            } else {
                z = false;
                z2 = false;
            }
            if (!TextUtils.equals(audioRoutesInfo.bluetoothName, this.mCurAudioRoutesInfo.bluetoothName)) {
                this.mCurAudioRoutesInfo.bluetoothName = audioRoutesInfo.bluetoothName;
                if (this.mCurAudioRoutesInfo.bluetoothName != null) {
                    if (this.mBluetoothA2dpRoute == null) {
                        RouteInfo routeInfo = new RouteInfo(this.mSystemCategory);
                        routeInfo.mName = this.mCurAudioRoutesInfo.bluetoothName;
                        routeInfo.mDescription = this.mResources.getText(R.string.bluetooth_a2dp_audio_route_name);
                        routeInfo.mSupportedTypes = 1;
                        routeInfo.mDeviceType = 3;
                        this.mBluetoothA2dpRoute = routeInfo;
                        MediaRouter.addRouteStatic(this.mBluetoothA2dpRoute);
                    } else {
                        this.mBluetoothA2dpRoute.mName = this.mCurAudioRoutesInfo.bluetoothName;
                        MediaRouter.dispatchRouteChanged(this.mBluetoothA2dpRoute);
                    }
                } else if (this.mBluetoothA2dpRoute != null) {
                    MediaRouter.removeRouteStatic(this.mBluetoothA2dpRoute);
                    this.mBluetoothA2dpRoute = null;
                }
                z2 = false;
                z = true;
            }
            if (z) {
                Log.v(MediaRouter.TAG, "Audio routes updated: " + audioRoutesInfo + ", a2dp=" + isBluetoothA2dpOn());
                if (this.mSelectedRoute == null || this.mSelectedRoute == this.mDefaultAudioVideo || this.mSelectedRoute == this.mBluetoothA2dpRoute) {
                    if (z2 || this.mBluetoothA2dpRoute == null) {
                        MediaRouter.selectRouteStatic(1, this.mDefaultAudioVideo, false);
                    } else {
                        MediaRouter.selectRouteStatic(1, this.mBluetoothA2dpRoute, false);
                    }
                }
            }
        }

        boolean isBluetoothA2dpOn() {
            try {
                if (this.mBluetoothA2dpRoute != null) {
                    return this.mAudioService.isBluetoothA2dpOn();
                }
                return false;
            } catch (RemoteException e) {
                Log.e(MediaRouter.TAG, "Error querying Bluetooth A2DP state", e);
                return false;
            }
        }

        void updateDiscoveryRequest() {
            int size = this.mCallbacks.size();
            int i = 0;
            boolean z = false;
            int i2 = 0;
            boolean z2 = false;
            for (int i3 = 0; i3 < size; i3++) {
                CallbackInfo callbackInfo = this.mCallbacks.get(i3);
                if ((callbackInfo.flags & 5) == 0 && (callbackInfo.flags & 8) != 0) {
                    i2 |= callbackInfo.type;
                } else {
                    i |= callbackInfo.type;
                }
                if ((callbackInfo.flags & 1) != 0) {
                    if ((callbackInfo.type & 4) != 0) {
                        z = true;
                        z2 = true;
                    } else {
                        z = true;
                    }
                }
            }
            if (i != 0 || z) {
                i |= i2;
            }
            if (this.mCanConfigureWifiDisplays) {
                if (this.mSelectedRoute != null && this.mSelectedRoute.matchesTypes(4)) {
                    z2 = false;
                }
                if (z2) {
                    if (!this.mActivelyScanningWifiDisplays) {
                        this.mActivelyScanningWifiDisplays = true;
                        this.mDisplayService.startWifiDisplayScan();
                    }
                } else if (this.mActivelyScanningWifiDisplays) {
                    this.mActivelyScanningWifiDisplays = false;
                    this.mDisplayService.stopWifiDisplayScan();
                }
            }
            if (i != this.mDiscoveryRequestRouteTypes || z != this.mDiscoverRequestActiveScan) {
                this.mDiscoveryRequestRouteTypes = i;
                this.mDiscoverRequestActiveScan = z;
                publishClientDiscoveryRequest();
            }
        }

        @Override
        public void onDisplayAdded(int i) {
            updatePresentationDisplays(i);
        }

        @Override
        public void onDisplayChanged(int i) {
            updatePresentationDisplays(i);
        }

        @Override
        public void onDisplayRemoved(int i) {
            updatePresentationDisplays(i);
        }

        public Display[] getAllPresentationDisplays() {
            return this.mDisplayService.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        }

        private void updatePresentationDisplays(int i) {
            int size = this.mRoutes.size();
            for (int i2 = 0; i2 < size; i2++) {
                RouteInfo routeInfo = this.mRoutes.get(i2);
                if (routeInfo.updatePresentationDisplay() || (routeInfo.mPresentationDisplay != null && routeInfo.mPresentationDisplay.getDisplayId() == i)) {
                    MediaRouter.dispatchRoutePresentationDisplayChanged(routeInfo);
                }
            }
        }

        void setSelectedRoute(RouteInfo routeInfo, boolean z) {
            this.mSelectedRoute = routeInfo;
            publishClientSelectedRoute(z);
        }

        void rebindAsUser(int i) {
            if (this.mCurrentUserId != i || i < 0 || this.mClient == null) {
                if (this.mClient != null) {
                    try {
                        this.mMediaRouterService.unregisterClient(this.mClient);
                    } catch (RemoteException e) {
                        Log.e(MediaRouter.TAG, "Unable to unregister media router client.", e);
                    }
                    this.mClient = null;
                }
                this.mCurrentUserId = i;
                try {
                    Client client = new Client();
                    this.mMediaRouterService.registerClientAsUser(client, this.mPackageName, i);
                    this.mClient = client;
                } catch (RemoteException e2) {
                    Log.e(MediaRouter.TAG, "Unable to register media router client.", e2);
                }
                publishClientDiscoveryRequest();
                publishClientSelectedRoute(false);
                updateClientState();
            }
        }

        void publishClientDiscoveryRequest() {
            if (this.mClient != null) {
                try {
                    this.mMediaRouterService.setDiscoveryRequest(this.mClient, this.mDiscoveryRequestRouteTypes, this.mDiscoverRequestActiveScan);
                } catch (RemoteException e) {
                    Log.e(MediaRouter.TAG, "Unable to publish media router client discovery request.", e);
                }
            }
        }

        void publishClientSelectedRoute(boolean z) {
            if (this.mClient != null) {
                try {
                    this.mMediaRouterService.setSelectedRoute(this.mClient, this.mSelectedRoute != null ? this.mSelectedRoute.mGlobalRouteId : null, z);
                } catch (RemoteException e) {
                    Log.e(MediaRouter.TAG, "Unable to publish media router client selected route.", e);
                }
            }
        }

        void updateClientState() {
            this.mClientState = null;
            if (this.mClient != null) {
                try {
                    this.mClientState = this.mMediaRouterService.getState(this.mClient);
                } catch (RemoteException e) {
                    Log.e(MediaRouter.TAG, "Unable to retrieve media router client state.", e);
                }
            }
            ArrayList<MediaRouterClientState.RouteInfo> arrayList = this.mClientState != null ? this.mClientState.routes : null;
            int size = arrayList != null ? arrayList.size() : 0;
            for (int i = 0; i < size; i++) {
                MediaRouterClientState.RouteInfo routeInfo = arrayList.get(i);
                RouteInfo routeInfoFindGlobalRoute = findGlobalRoute(routeInfo.id);
                if (routeInfoFindGlobalRoute == null) {
                    MediaRouter.addRouteStatic(makeGlobalRoute(routeInfo));
                } else {
                    updateGlobalRoute(routeInfoFindGlobalRoute, routeInfo);
                }
            }
            int size2 = this.mRoutes.size();
            while (true) {
                int i2 = size2 - 1;
                if (size2 > 0) {
                    RouteInfo routeInfo2 = this.mRoutes.get(i2);
                    String str = routeInfo2.mGlobalRouteId;
                    if (str != null) {
                        int i3 = 0;
                        while (true) {
                            if (i3 < size) {
                                if (str.equals(arrayList.get(i3).id)) {
                                    break;
                                } else {
                                    i3++;
                                }
                            } else {
                                MediaRouter.removeRouteStatic(routeInfo2);
                                break;
                            }
                        }
                    }
                    size2 = i2;
                } else {
                    return;
                }
            }
        }

        void requestSetVolume(RouteInfo routeInfo, int i) {
            if (routeInfo.mGlobalRouteId != null && this.mClient != null) {
                try {
                    this.mMediaRouterService.requestSetVolume(this.mClient, routeInfo.mGlobalRouteId, i);
                } catch (RemoteException e) {
                    Log.w(MediaRouter.TAG, "Unable to request volume change.", e);
                }
            }
        }

        void requestUpdateVolume(RouteInfo routeInfo, int i) {
            if (routeInfo.mGlobalRouteId != null && this.mClient != null) {
                try {
                    this.mMediaRouterService.requestUpdateVolume(this.mClient, routeInfo.mGlobalRouteId, i);
                } catch (RemoteException e) {
                    Log.w(MediaRouter.TAG, "Unable to request volume change.", e);
                }
            }
        }

        RouteInfo makeGlobalRoute(MediaRouterClientState.RouteInfo routeInfo) {
            RouteInfo routeInfo2 = new RouteInfo(this.mSystemCategory);
            routeInfo2.mGlobalRouteId = routeInfo.id;
            routeInfo2.mName = routeInfo.name;
            routeInfo2.mDescription = routeInfo.description;
            routeInfo2.mSupportedTypes = routeInfo.supportedTypes;
            routeInfo2.mDeviceType = routeInfo.deviceType;
            routeInfo2.mEnabled = routeInfo.enabled;
            routeInfo2.setRealStatusCode(routeInfo.statusCode);
            routeInfo2.mPlaybackType = routeInfo.playbackType;
            routeInfo2.mPlaybackStream = routeInfo.playbackStream;
            routeInfo2.mVolume = routeInfo.volume;
            routeInfo2.mVolumeMax = routeInfo.volumeMax;
            routeInfo2.mVolumeHandling = routeInfo.volumeHandling;
            routeInfo2.mPresentationDisplayId = routeInfo.presentationDisplayId;
            routeInfo2.updatePresentationDisplay();
            return routeInfo2;
        }

        void updateGlobalRoute(RouteInfo routeInfo, MediaRouterClientState.RouteInfo routeInfo2) {
            boolean z;
            boolean z2;
            boolean z3 = false;
            if (Objects.equals(routeInfo.mName, routeInfo2.name)) {
                z = false;
            } else {
                routeInfo.mName = routeInfo2.name;
                z = true;
            }
            if (!Objects.equals(routeInfo.mDescription, routeInfo2.description)) {
                routeInfo.mDescription = routeInfo2.description;
                z = true;
            }
            int i = routeInfo.mSupportedTypes;
            if (i != routeInfo2.supportedTypes) {
                routeInfo.mSupportedTypes = routeInfo2.supportedTypes;
                z = true;
            }
            if (routeInfo.mEnabled != routeInfo2.enabled) {
                routeInfo.mEnabled = routeInfo2.enabled;
                z = true;
            }
            if (routeInfo.mRealStatusCode != routeInfo2.statusCode) {
                routeInfo.setRealStatusCode(routeInfo2.statusCode);
                z = true;
            }
            if (routeInfo.mPlaybackType != routeInfo2.playbackType) {
                routeInfo.mPlaybackType = routeInfo2.playbackType;
                z = true;
            }
            if (routeInfo.mPlaybackStream != routeInfo2.playbackStream) {
                routeInfo.mPlaybackStream = routeInfo2.playbackStream;
                z = true;
            }
            if (routeInfo.mVolume != routeInfo2.volume) {
                routeInfo.mVolume = routeInfo2.volume;
                z = true;
                z2 = true;
            } else {
                z2 = false;
            }
            if (routeInfo.mVolumeMax != routeInfo2.volumeMax) {
                routeInfo.mVolumeMax = routeInfo2.volumeMax;
                z = true;
                z2 = true;
            }
            if (routeInfo.mVolumeHandling != routeInfo2.volumeHandling) {
                routeInfo.mVolumeHandling = routeInfo2.volumeHandling;
                z = true;
                z2 = true;
            }
            if (routeInfo.mPresentationDisplayId != routeInfo2.presentationDisplayId) {
                routeInfo.mPresentationDisplayId = routeInfo2.presentationDisplayId;
                routeInfo.updatePresentationDisplay();
                z = true;
                z3 = true;
            }
            if (z) {
                MediaRouter.dispatchRouteChanged(routeInfo, i);
            }
            if (z2) {
                MediaRouter.dispatchRouteVolumeChanged(routeInfo);
            }
            if (z3) {
                MediaRouter.dispatchRoutePresentationDisplayChanged(routeInfo);
            }
        }

        RouteInfo findGlobalRoute(String str) {
            int size = this.mRoutes.size();
            for (int i = 0; i < size; i++) {
                RouteInfo routeInfo = this.mRoutes.get(i);
                if (str.equals(routeInfo.mGlobalRouteId)) {
                    return routeInfo;
                }
            }
            return null;
        }

        boolean isPlaybackActive() {
            if (this.mClient != null) {
                try {
                    return this.mMediaRouterService.isPlaybackActive(this.mClient);
                } catch (RemoteException e) {
                    Log.e(MediaRouter.TAG, "Unable to retrieve playback active state.", e);
                    return false;
                }
            }
            return false;
        }

        final class Client extends IMediaRouterClient.Stub {
            Client() {
            }

            @Override
            public void onStateChanged() {
                Static.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (Client.this == Static.this.mClient) {
                            Static.this.updateClientState();
                        }
                    }
                });
            }

            @Override
            public void onRestoreRoute() {
                Static.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (Client.this != Static.this.mClient || Static.this.mSelectedRoute == null) {
                            return;
                        }
                        if (Static.this.mSelectedRoute != Static.this.mDefaultAudioVideo && Static.this.mSelectedRoute != Static.this.mBluetoothA2dpRoute) {
                            return;
                        }
                        Log.v(MediaRouter.TAG, "onRestoreRoute() : route=" + Static.this.mSelectedRoute);
                        Static.this.mSelectedRoute.select();
                    }
                });
            }
        }
    }

    static String typesToString(int i) {
        StringBuilder sb = new StringBuilder();
        if ((i & 1) != 0) {
            sb.append("ROUTE_TYPE_LIVE_AUDIO ");
        }
        if ((i & 2) != 0) {
            sb.append("ROUTE_TYPE_LIVE_VIDEO ");
        }
        if ((i & 4) != 0) {
            sb.append("ROUTE_TYPE_REMOTE_DISPLAY ");
        }
        if ((i & 8388608) != 0) {
            sb.append("ROUTE_TYPE_USER ");
        }
        return sb.toString();
    }

    public MediaRouter(Context context) {
        synchronized (Static.class) {
            if (sStatic == null) {
                Context applicationContext = context.getApplicationContext();
                sStatic = new Static(applicationContext);
                sStatic.startMonitoringRoutes(applicationContext);
            }
        }
    }

    public RouteInfo getDefaultRoute() {
        return sStatic.mDefaultAudioVideo;
    }

    public RouteInfo getFallbackRoute() {
        return sStatic.mBluetoothA2dpRoute != null ? sStatic.mBluetoothA2dpRoute : sStatic.mDefaultAudioVideo;
    }

    public RouteCategory getSystemCategory() {
        return sStatic.mSystemCategory;
    }

    public RouteInfo getSelectedRoute() {
        return getSelectedRoute(8388615);
    }

    public RouteInfo getSelectedRoute(int i) {
        if (sStatic.mSelectedRoute != null && (sStatic.mSelectedRoute.mSupportedTypes & i) != 0) {
            return sStatic.mSelectedRoute;
        }
        if (i == 8388608) {
            return null;
        }
        return sStatic.mDefaultAudioVideo;
    }

    public boolean isRouteAvailable(int i, int i2) {
        int size = sStatic.mRoutes.size();
        for (int i3 = 0; i3 < size; i3++) {
            RouteInfo routeInfo = sStatic.mRoutes.get(i3);
            if (routeInfo.matchesTypes(i) && ((i2 & 1) == 0 || routeInfo != sStatic.mDefaultAudioVideo)) {
                return true;
            }
        }
        return false;
    }

    public void addCallback(int i, Callback callback) {
        addCallback(i, callback, 0);
    }

    public void addCallback(int i, Callback callback, int i2) {
        int iFindCallbackInfo = findCallbackInfo(callback);
        if (iFindCallbackInfo >= 0) {
            CallbackInfo callbackInfo = sStatic.mCallbacks.get(iFindCallbackInfo);
            callbackInfo.type = i | callbackInfo.type;
            callbackInfo.flags |= i2;
        } else {
            sStatic.mCallbacks.add(new CallbackInfo(callback, i, i2, this));
        }
        sStatic.updateDiscoveryRequest();
    }

    public void removeCallback(Callback callback) {
        int iFindCallbackInfo = findCallbackInfo(callback);
        if (iFindCallbackInfo >= 0) {
            sStatic.mCallbacks.remove(iFindCallbackInfo);
            sStatic.updateDiscoveryRequest();
            return;
        }
        Log.w(TAG, "removeCallback(" + callback + "): callback not registered");
    }

    private int findCallbackInfo(Callback callback) {
        int size = sStatic.mCallbacks.size();
        for (int i = 0; i < size; i++) {
            if (sStatic.mCallbacks.get(i).cb == callback) {
                return i;
            }
        }
        return -1;
    }

    public void selectRoute(int i, RouteInfo routeInfo) {
        if (routeInfo == null) {
            throw new IllegalArgumentException("Route cannot be null.");
        }
        selectRouteStatic(i, routeInfo, true);
    }

    public void selectRouteInt(int i, RouteInfo routeInfo, boolean z) {
        selectRouteStatic(i, routeInfo, z);
    }

    static void selectRouteStatic(int i, RouteInfo routeInfo, boolean z) {
        Log.v(TAG, "Selecting route: " + routeInfo);
        RouteInfo routeInfo2 = sStatic.mSelectedRoute;
        RouteInfo routeInfo3 = sStatic.isBluetoothA2dpOn() ? sStatic.mBluetoothA2dpRoute : sStatic.mDefaultAudioVideo;
        boolean z2 = routeInfo2 == sStatic.mDefaultAudioVideo || routeInfo2 == sStatic.mBluetoothA2dpRoute;
        if (routeInfo2 == routeInfo && (!z2 || routeInfo == routeInfo3)) {
            return;
        }
        if (!routeInfo.matchesTypes(i)) {
            Log.w(TAG, "selectRoute ignored; cannot select route with supported types " + typesToString(routeInfo.getSupportedTypes()) + " into route types " + typesToString(i));
            return;
        }
        RouteInfo routeInfo4 = sStatic.mBluetoothA2dpRoute;
        if (sStatic.isPlaybackActive() && routeInfo4 != null && (i & 1) != 0 && (routeInfo == routeInfo4 || routeInfo == sStatic.mDefaultAudioVideo)) {
            try {
                sStatic.mAudioService.setBluetoothA2dpOn(routeInfo == routeInfo4);
                if (routeInfo != routeInfo4) {
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    StringBuffer stringBuffer = new StringBuffer();
                    for (int i2 = 3; i2 < stackTrace.length; i2++) {
                        StackTraceElement stackTraceElement = stackTrace[i2];
                        stringBuffer.append(stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + SettingsStringUtil.DELIMITER + stackTraceElement.getLineNumber());
                        stringBuffer.append("  ");
                    }
                    Log.w(TAG, "Default route is selected while a BT route is available: pkgName=" + sStatic.mPackageName + ", callers=" + stringBuffer.toString());
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error changing Bluetooth A2DP state", e);
            }
        }
        WifiDisplay activeDisplay = sStatic.mDisplayService.getWifiDisplayStatus().getActiveDisplay();
        boolean z3 = (routeInfo2 == null || routeInfo2.mDeviceAddress == null) ? false : true;
        boolean z4 = routeInfo.mDeviceAddress != null;
        if (activeDisplay != null || z3 || z4) {
            if (z4 && !matchesDeviceAddress(activeDisplay, routeInfo)) {
                if (sStatic.mCanConfigureWifiDisplays) {
                    sStatic.mDisplayService.connectWifiDisplay(routeInfo.mDeviceAddress);
                } else {
                    Log.e(TAG, "Cannot connect to wifi displays because this process is not allowed to do so.");
                }
            } else if (activeDisplay != null && !z4) {
                sStatic.mDisplayService.disconnectWifiDisplay();
            }
        }
        sStatic.setSelectedRoute(routeInfo, z);
        if (routeInfo2 != null) {
            dispatchRouteUnselected(routeInfo2.getSupportedTypes() & i, routeInfo2);
            if (routeInfo2.resolveStatusCode()) {
                dispatchRouteChanged(routeInfo2);
            }
        }
        if (routeInfo != null) {
            if (routeInfo.resolveStatusCode()) {
                dispatchRouteChanged(routeInfo);
            }
            dispatchRouteSelected(i & routeInfo.getSupportedTypes(), routeInfo);
        }
        sStatic.updateDiscoveryRequest();
    }

    static void selectDefaultRouteStatic() {
        if (sStatic.mSelectedRoute != sStatic.mBluetoothA2dpRoute && sStatic.isBluetoothA2dpOn()) {
            selectRouteStatic(8388615, sStatic.mBluetoothA2dpRoute, false);
        } else {
            selectRouteStatic(8388615, sStatic.mDefaultAudioVideo, false);
        }
    }

    static boolean matchesDeviceAddress(WifiDisplay wifiDisplay, RouteInfo routeInfo) {
        boolean z = (routeInfo == null || routeInfo.mDeviceAddress == null) ? false : true;
        if (wifiDisplay == null && !z) {
            return true;
        }
        if (wifiDisplay == null || !z) {
            return false;
        }
        return wifiDisplay.getDeviceAddress().equals(routeInfo.mDeviceAddress);
    }

    public void addUserRoute(UserRouteInfo userRouteInfo) {
        addRouteStatic(userRouteInfo);
    }

    public void addRouteInt(RouteInfo routeInfo) {
        addRouteStatic(routeInfo);
    }

    static void addRouteStatic(RouteInfo routeInfo) {
        Log.v(TAG, "Adding route: " + routeInfo);
        RouteCategory category = routeInfo.getCategory();
        if (!sStatic.mCategories.contains(category)) {
            sStatic.mCategories.add(category);
        }
        if (category.isGroupable() && !(routeInfo instanceof RouteGroup)) {
            RouteGroup routeGroup = new RouteGroup(routeInfo.getCategory());
            routeGroup.mSupportedTypes = routeInfo.mSupportedTypes;
            sStatic.mRoutes.add(routeGroup);
            dispatchRouteAdded(routeGroup);
            routeGroup.addRoute(routeInfo);
            return;
        }
        sStatic.mRoutes.add(routeInfo);
        dispatchRouteAdded(routeInfo);
    }

    public void removeUserRoute(UserRouteInfo userRouteInfo) {
        removeRouteStatic(userRouteInfo);
    }

    public void clearUserRoutes() {
        int i = 0;
        while (i < sStatic.mRoutes.size()) {
            RouteInfo routeInfo = sStatic.mRoutes.get(i);
            if ((routeInfo instanceof UserRouteInfo) || (routeInfo instanceof RouteGroup)) {
                removeRouteStatic(routeInfo);
                i--;
            }
            i++;
        }
    }

    public void removeRouteInt(RouteInfo routeInfo) {
        removeRouteStatic(routeInfo);
    }

    static void removeRouteStatic(RouteInfo routeInfo) {
        Log.v(TAG, "Removing route: " + routeInfo);
        if (sStatic.mRoutes.remove(routeInfo)) {
            RouteCategory category = routeInfo.getCategory();
            int size = sStatic.mRoutes.size();
            boolean z = false;
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                if (category != sStatic.mRoutes.get(i).getCategory()) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            }
            if (routeInfo.isSelected()) {
                selectDefaultRouteStatic();
            }
            if (!z) {
                sStatic.mCategories.remove(category);
            }
            dispatchRouteRemoved(routeInfo);
        }
    }

    public int getCategoryCount() {
        return sStatic.mCategories.size();
    }

    public RouteCategory getCategoryAt(int i) {
        return sStatic.mCategories.get(i);
    }

    public int getRouteCount() {
        return sStatic.mRoutes.size();
    }

    public RouteInfo getRouteAt(int i) {
        return sStatic.mRoutes.get(i);
    }

    static int getRouteCountStatic() {
        return sStatic.mRoutes.size();
    }

    static RouteInfo getRouteAtStatic(int i) {
        return sStatic.mRoutes.get(i);
    }

    public UserRouteInfo createUserRoute(RouteCategory routeCategory) {
        return new UserRouteInfo(routeCategory);
    }

    public RouteCategory createRouteCategory(CharSequence charSequence, boolean z) {
        return new RouteCategory(charSequence, 8388608, z);
    }

    public RouteCategory createRouteCategory(int i, boolean z) {
        return new RouteCategory(i, 8388608, z);
    }

    public void rebindAsUser(int i) {
        sStatic.rebindAsUser(i);
    }

    static void updateRoute(RouteInfo routeInfo) {
        dispatchRouteChanged(routeInfo);
    }

    static void dispatchRouteSelected(int i, RouteInfo routeInfo) {
        if (DEBUG) {
            Log.d(TAG, "dispatchRouteSelected info: " + routeInfo + " type: " + i);
        }
        for (CallbackInfo callbackInfo : sStatic.mCallbacks) {
            if (callbackInfo.filterRouteEvent(routeInfo)) {
                callbackInfo.cb.onRouteSelected(callbackInfo.router, i, routeInfo);
            }
        }
    }

    static void dispatchRouteUnselected(int i, RouteInfo routeInfo) {
        if (DEBUG) {
            Log.d(TAG, "dispatchRouteUnselected info: " + routeInfo + " type: " + i);
        }
        for (CallbackInfo callbackInfo : sStatic.mCallbacks) {
            if (callbackInfo.filterRouteEvent(routeInfo)) {
                callbackInfo.cb.onRouteUnselected(callbackInfo.router, i, routeInfo);
            }
        }
    }

    static void dispatchRouteChanged(RouteInfo routeInfo) {
        dispatchRouteChanged(routeInfo, routeInfo.mSupportedTypes);
    }

    static void dispatchRouteChanged(RouteInfo routeInfo, int i) {
        if (DEBUG) {
            Log.d(TAG, "Dispatching route change: " + routeInfo);
        }
        int i2 = routeInfo.mSupportedTypes;
        for (CallbackInfo callbackInfo : sStatic.mCallbacks) {
            boolean zFilterRouteEvent = callbackInfo.filterRouteEvent(i);
            boolean zFilterRouteEvent2 = callbackInfo.filterRouteEvent(i2);
            if (DEBUG) {
                Log.d(TAG, "dispatchRouteChanged oldVisibility: " + zFilterRouteEvent + "newVisibility: " + zFilterRouteEvent2);
            }
            if (!zFilterRouteEvent && zFilterRouteEvent2) {
                callbackInfo.cb.onRouteAdded(callbackInfo.router, routeInfo);
                if (routeInfo.isSelected()) {
                    callbackInfo.cb.onRouteSelected(callbackInfo.router, i2, routeInfo);
                }
            }
            if (zFilterRouteEvent || zFilterRouteEvent2) {
                callbackInfo.cb.onRouteChanged(callbackInfo.router, routeInfo);
            }
            if (zFilterRouteEvent && !zFilterRouteEvent2) {
                if (routeInfo.isSelected()) {
                    callbackInfo.cb.onRouteUnselected(callbackInfo.router, i, routeInfo);
                }
                callbackInfo.cb.onRouteRemoved(callbackInfo.router, routeInfo);
            }
        }
    }

    static void dispatchRouteAdded(RouteInfo routeInfo) {
        for (CallbackInfo callbackInfo : sStatic.mCallbacks) {
            if (callbackInfo.filterRouteEvent(routeInfo)) {
                callbackInfo.cb.onRouteAdded(callbackInfo.router, routeInfo);
            }
        }
    }

    static void dispatchRouteRemoved(RouteInfo routeInfo) {
        for (CallbackInfo callbackInfo : sStatic.mCallbacks) {
            if (callbackInfo.filterRouteEvent(routeInfo)) {
                callbackInfo.cb.onRouteRemoved(callbackInfo.router, routeInfo);
            }
        }
    }

    static void dispatchRouteGrouped(RouteInfo routeInfo, RouteGroup routeGroup, int i) {
        for (CallbackInfo callbackInfo : sStatic.mCallbacks) {
            if (callbackInfo.filterRouteEvent(routeGroup)) {
                callbackInfo.cb.onRouteGrouped(callbackInfo.router, routeInfo, routeGroup, i);
            }
        }
    }

    static void dispatchRouteUngrouped(RouteInfo routeInfo, RouteGroup routeGroup) {
        for (CallbackInfo callbackInfo : sStatic.mCallbacks) {
            if (callbackInfo.filterRouteEvent(routeGroup)) {
                callbackInfo.cb.onRouteUngrouped(callbackInfo.router, routeInfo, routeGroup);
            }
        }
    }

    static void dispatchRouteVolumeChanged(RouteInfo routeInfo) {
        for (CallbackInfo callbackInfo : sStatic.mCallbacks) {
            if (callbackInfo.filterRouteEvent(routeInfo)) {
                callbackInfo.cb.onRouteVolumeChanged(callbackInfo.router, routeInfo);
            }
        }
    }

    static void dispatchRoutePresentationDisplayChanged(RouteInfo routeInfo) {
        for (CallbackInfo callbackInfo : sStatic.mCallbacks) {
            if (callbackInfo.filterRouteEvent(routeInfo)) {
                callbackInfo.cb.onRoutePresentationDisplayChanged(callbackInfo.router, routeInfo);
            }
        }
    }

    static void systemVolumeChanged(int i) {
        RouteInfo routeInfo = sStatic.mSelectedRoute;
        if (routeInfo == null) {
            return;
        }
        if (routeInfo == sStatic.mBluetoothA2dpRoute || routeInfo == sStatic.mDefaultAudioVideo) {
            dispatchRouteVolumeChanged(routeInfo);
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

    static void updateWifiDisplayStatus(WifiDisplayStatus wifiDisplayStatus) {
        WifiDisplay[] displays;
        WifiDisplay activeDisplay;
        WifiDisplay wifiDisplayFindWifiDisplay;
        if (DEBUG) {
            Log.d(TAG, "updateWifiDisplayStatus status: " + wifiDisplayStatus);
        }
        if (wifiDisplayStatus.getFeatureState() == 3) {
            displays = wifiDisplayStatus.getDisplays();
            activeDisplay = wifiDisplayStatus.getActiveDisplay();
            if (!sStatic.mCanConfigureWifiDisplays) {
                if (activeDisplay != null) {
                    displays = new WifiDisplay[]{activeDisplay};
                } else {
                    displays = WifiDisplay.EMPTY_ARRAY;
                }
            }
        } else {
            displays = WifiDisplay.EMPTY_ARRAY;
            activeDisplay = null;
        }
        String deviceAddress = activeDisplay != null ? activeDisplay.getDeviceAddress() : null;
        for (WifiDisplay wifiDisplay : displays) {
            if (DEBUG) {
                Log.d(TAG, "updateWifiDisplayStatus display: " + wifiDisplay);
            }
            if (shouldShowWifiDisplay(wifiDisplay, activeDisplay)) {
                RouteInfo routeInfoFindWifiDisplayRoute = findWifiDisplayRoute(wifiDisplay);
                if (routeInfoFindWifiDisplayRoute == null) {
                    routeInfoFindWifiDisplayRoute = makeWifiDisplayRoute(wifiDisplay, wifiDisplayStatus);
                    addRouteStatic(routeInfoFindWifiDisplayRoute);
                } else {
                    String deviceAddress2 = wifiDisplay.getDeviceAddress();
                    updateWifiDisplayRoute(routeInfoFindWifiDisplayRoute, wifiDisplay, wifiDisplayStatus, !deviceAddress2.equals(deviceAddress) && deviceAddress2.equals(sStatic.mPreviousActiveWifiDisplayAddress));
                }
                if (wifiDisplay.equals(activeDisplay)) {
                    selectRouteStatic(routeInfoFindWifiDisplayRoute.getSupportedTypes(), routeInfoFindWifiDisplayRoute, false);
                }
            }
        }
        int size = sStatic.mRoutes.size();
        while (true) {
            int i = size - 1;
            if (size > 0) {
                RouteInfo routeInfo = sStatic.mRoutes.get(i);
                if (routeInfo.mDeviceAddress != null && ((wifiDisplayFindWifiDisplay = findWifiDisplay(displays, routeInfo.mDeviceAddress)) == null || !shouldShowWifiDisplay(wifiDisplayFindWifiDisplay, activeDisplay))) {
                    removeRouteStatic(routeInfo);
                }
                size = i;
            } else {
                sStatic.mPreviousActiveWifiDisplayAddress = deviceAddress;
                return;
            }
        }
    }

    private static boolean shouldShowWifiDisplay(WifiDisplay wifiDisplay, WifiDisplay wifiDisplay2) {
        return wifiDisplay.isRemembered() || wifiDisplay.equals(wifiDisplay2);
    }

    static int getWifiDisplayStatusCode(WifiDisplay wifiDisplay, WifiDisplayStatus wifiDisplayStatus) {
        int i = 1;
        if (wifiDisplayStatus.getScanState() != 1) {
            if (wifiDisplay.isAvailable()) {
                i = wifiDisplay.canConnect() ? 3 : 5;
            } else {
                i = 4;
            }
        }
        if (wifiDisplay.equals(wifiDisplayStatus.getActiveDisplay())) {
            switch (wifiDisplayStatus.getActiveDisplayState()) {
                case 0:
                    Log.e(TAG, "Active display is not connected!");
                    break;
            }
            return i;
        }
        return i;
    }

    static boolean isWifiDisplayEnabled(WifiDisplay wifiDisplay, WifiDisplayStatus wifiDisplayStatus) {
        return wifiDisplay.isAvailable() && (wifiDisplay.canConnect() || wifiDisplay.equals(wifiDisplayStatus.getActiveDisplay()));
    }

    static RouteInfo makeWifiDisplayRoute(WifiDisplay wifiDisplay, WifiDisplayStatus wifiDisplayStatus) {
        RouteInfo routeInfo = new RouteInfo(sStatic.mSystemCategory);
        routeInfo.mDeviceAddress = wifiDisplay.getDeviceAddress();
        routeInfo.mSupportedTypes = 7;
        routeInfo.mVolumeHandling = 0;
        routeInfo.mPlaybackType = 1;
        routeInfo.setRealStatusCode(getWifiDisplayStatusCode(wifiDisplay, wifiDisplayStatus));
        routeInfo.mEnabled = isWifiDisplayEnabled(wifiDisplay, wifiDisplayStatus);
        routeInfo.mName = wifiDisplay.getFriendlyDisplayName();
        routeInfo.mDescription = sStatic.mResources.getText(R.string.wireless_display_route_description);
        routeInfo.updatePresentationDisplay();
        routeInfo.mDeviceType = 1;
        return routeInfo;
    }

    private static void updateWifiDisplayRoute(RouteInfo routeInfo, WifiDisplay wifiDisplay, WifiDisplayStatus wifiDisplayStatus, boolean z) {
        boolean z2;
        String friendlyDisplayName = wifiDisplay.getFriendlyDisplayName();
        if (routeInfo.getName().equals(friendlyDisplayName)) {
            z2 = false;
        } else {
            routeInfo.mName = friendlyDisplayName;
            z2 = true;
        }
        boolean zIsWifiDisplayEnabled = isWifiDisplayEnabled(wifiDisplay, wifiDisplayStatus);
        boolean z3 = z2 | (routeInfo.mEnabled != zIsWifiDisplayEnabled);
        routeInfo.mEnabled = zIsWifiDisplayEnabled;
        if (DEBUG) {
            Log.d(TAG, "updateWifiDisplayRoute changed: " + z3 + " enabled: " + zIsWifiDisplayEnabled + "  route.isSelected(): " + routeInfo.isSelected() + " route: " + routeInfo);
        }
        if (routeInfo.setRealStatusCode(getWifiDisplayStatusCode(wifiDisplay, wifiDisplayStatus)) | z3) {
            dispatchRouteChanged(routeInfo);
        }
        if ((!zIsWifiDisplayEnabled || z) && routeInfo.isSelected()) {
            selectDefaultRouteStatic();
        }
    }

    private static WifiDisplay findWifiDisplay(WifiDisplay[] wifiDisplayArr, String str) {
        for (WifiDisplay wifiDisplay : wifiDisplayArr) {
            if (wifiDisplay.getDeviceAddress().equals(str)) {
                return wifiDisplay;
            }
        }
        return null;
    }

    private static RouteInfo findWifiDisplayRoute(WifiDisplay wifiDisplay) {
        int size = sStatic.mRoutes.size();
        for (int i = 0; i < size; i++) {
            RouteInfo routeInfo = sStatic.mRoutes.get(i);
            if (wifiDisplay.getDeviceAddress().equals(routeInfo.mDeviceAddress)) {
                return routeInfo;
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
            public void dispatchRemoteVolumeUpdate(final int i, final int i2) {
                MediaRouter.sStatic.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (RouteInfo.this.mVcb != null) {
                            if (i != 0) {
                                RouteInfo.this.mVcb.vcb.onVolumeUpdateRequest(RouteInfo.this.mVcb.route, i);
                            } else {
                                RouteInfo.this.mVcb.vcb.onVolumeSetRequest(RouteInfo.this.mVcb.route, i2);
                            }
                        }
                    }
                });
            }
        };
        int mDeviceType = 0;

        @Retention(RetentionPolicy.SOURCE)
        public @interface DeviceType {
        }

        @Retention(RetentionPolicy.SOURCE)
        public @interface PlaybackType {
        }

        @Retention(RetentionPolicy.SOURCE)
        private @interface PlaybackVolume {
        }

        RouteInfo(RouteCategory routeCategory) {
            this.mCategory = routeCategory;
        }

        public CharSequence getName() {
            return getName(MediaRouter.sStatic.mResources);
        }

        public CharSequence getName(Context context) {
            return getName(context.getResources());
        }

        CharSequence getName(Resources resources) {
            if (this.mNameResId != 0) {
                return resources.getText(this.mNameResId);
            }
            return this.mName;
        }

        public CharSequence getDescription() {
            return this.mDescription;
        }

        public CharSequence getStatus() {
            return this.mStatus;
        }

        boolean setRealStatusCode(int i) {
            if (this.mRealStatusCode != i) {
                this.mRealStatusCode = i;
                return resolveStatusCode();
            }
            return false;
        }

        boolean resolveStatusCode() {
            int i = this.mRealStatusCode;
            if (isSelected() && (i == 1 || i == 3)) {
                i = 2;
            }
            int i2 = 0;
            if (this.mResolvedStatusCode == i) {
                return false;
            }
            this.mResolvedStatusCode = i;
            switch (i) {
                case 1:
                    i2 = R.string.media_route_status_scanning;
                    break;
                case 2:
                    i2 = R.string.media_route_status_connecting;
                    break;
                case 3:
                    i2 = R.string.media_route_status_available;
                    break;
                case 4:
                    i2 = R.string.media_route_status_not_available;
                    break;
                case 5:
                    i2 = R.string.media_route_status_in_use;
                    break;
            }
            this.mStatus = i2 != 0 ? MediaRouter.sStatic.mResources.getText(i2) : null;
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

        public boolean matchesTypes(int i) {
            return (i & this.mSupportedTypes) != 0;
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

        public void setTag(Object obj) {
            this.mTag = obj;
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
                    return MediaRouter.sStatic.mAudioService.getStreamVolume(this.mPlaybackStream);
                } catch (RemoteException e) {
                    Log.e(MediaRouter.TAG, "Error getting local stream volume", e);
                    return 0;
                }
            }
            return this.mVolume;
        }

        public void requestSetVolume(int i) {
            if (this.mPlaybackType == 0) {
                try {
                    MediaRouter.sStatic.mAudioService.setStreamVolume(this.mPlaybackStream, i, 0, ActivityThread.currentPackageName());
                    return;
                } catch (RemoteException e) {
                    Log.e(MediaRouter.TAG, "Error setting local stream volume", e);
                    return;
                }
            }
            MediaRouter.sStatic.requestSetVolume(this, i);
        }

        public void requestUpdateVolume(int i) {
            if (this.mPlaybackType == 0) {
                try {
                    MediaRouter.sStatic.mAudioService.setStreamVolume(this.mPlaybackStream, Math.max(0, Math.min(getVolume() + i, getVolumeMax())), 0, ActivityThread.currentPackageName());
                    return;
                } catch (RemoteException e) {
                    Log.e(MediaRouter.TAG, "Error setting local stream volume", e);
                    return;
                }
            }
            MediaRouter.sStatic.requestUpdateVolume(this, i);
        }

        public int getVolumeMax() {
            if (this.mPlaybackType == 0) {
                try {
                    return MediaRouter.sStatic.mAudioService.getStreamMaxVolume(this.mPlaybackStream);
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
            Display displayChoosePresentationDisplay = choosePresentationDisplay();
            if (this.mPresentationDisplay != displayChoosePresentationDisplay) {
                this.mPresentationDisplay = displayChoosePresentationDisplay;
                return true;
            }
            return false;
        }

        private Display choosePresentationDisplay() {
            if ((this.mSupportedTypes & 2) != 0) {
                Display[] allPresentationDisplays = MediaRouter.sStatic.getAllPresentationDisplays();
                int i = 0;
                if (this.mPresentationDisplayId >= 0) {
                    int length = allPresentationDisplays.length;
                    while (i < length) {
                        Display display = allPresentationDisplays[i];
                        if (display.getDisplayId() != this.mPresentationDisplayId) {
                            i++;
                        } else {
                            return display;
                        }
                    }
                    return null;
                }
                if (this.mDeviceAddress != null) {
                    int length2 = allPresentationDisplays.length;
                    while (i < length2) {
                        Display display2 = allPresentationDisplays[i];
                        if (display2.getType() != 3 || !this.mDeviceAddress.equals(display2.getAddress())) {
                            i++;
                        } else {
                            return display2;
                        }
                    }
                    return null;
                }
                if (this == MediaRouter.sStatic.mDefaultAudioVideo && allPresentationDisplays.length > 0) {
                    return allPresentationDisplays[0];
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

        public boolean isBluetooth() {
            return this == MediaRouter.sStatic.mBluetoothA2dpRoute;
        }

        public void select() {
            MediaRouter.selectRouteStatic(this.mSupportedTypes, this, true);
        }

        void setStatusInt(CharSequence charSequence) {
            if (!charSequence.equals(this.mStatus)) {
                this.mStatus = charSequence;
                if (this.mGroup != null) {
                    this.mGroup.memberStatusChanged(this, charSequence);
                }
                routeUpdated();
            }
        }

        void routeUpdated() {
            MediaRouter.updateRoute(this);
        }

        public String toString() {
            return getClass().getSimpleName() + "{ name=" + ((Object) getName()) + ", description=" + ((Object) getDescription()) + ", status=" + ((Object) getStatus()) + ", category=" + getCategory() + ", supportedTypes=" + MediaRouter.typesToString(getSupportedTypes()) + ", presentationDisplay=" + this.mPresentationDisplay + " }";
        }
    }

    public static class UserRouteInfo extends RouteInfo {
        RemoteControlClient mRcc;
        SessionVolumeProvider mSvp;

        UserRouteInfo(RouteCategory routeCategory) {
            super(routeCategory);
            this.mSupportedTypes = 8388608;
            this.mPlaybackType = 1;
            this.mVolumeHandling = 0;
        }

        public void setName(CharSequence charSequence) {
            this.mNameResId = 0;
            this.mName = charSequence;
            routeUpdated();
        }

        public void setName(int i) {
            this.mNameResId = i;
            this.mName = null;
            routeUpdated();
        }

        public void setDescription(CharSequence charSequence) {
            this.mDescription = charSequence;
            routeUpdated();
        }

        public void setStatus(CharSequence charSequence) {
            setStatusInt(charSequence);
        }

        public void setRemoteControlClient(RemoteControlClient remoteControlClient) {
            this.mRcc = remoteControlClient;
            updatePlaybackInfoOnRcc();
        }

        public RemoteControlClient getRemoteControlClient() {
            return this.mRcc;
        }

        public void setIconDrawable(Drawable drawable) {
            this.mIcon = drawable;
        }

        public void setIconResource(int i) {
            setIconDrawable(MediaRouter.sStatic.mResources.getDrawable(i));
        }

        public void setVolumeCallback(VolumeCallback volumeCallback) {
            this.mVcb = new VolumeCallbackInfo(volumeCallback, this);
        }

        public void setPlaybackType(int i) {
            if (this.mPlaybackType != i) {
                this.mPlaybackType = i;
                configureSessionVolume();
            }
        }

        public void setVolumeHandling(int i) {
            if (this.mVolumeHandling != i) {
                this.mVolumeHandling = i;
                configureSessionVolume();
            }
        }

        public void setVolume(int i) {
            int iMax = Math.max(0, Math.min(i, getVolumeMax()));
            if (this.mVolume != iMax) {
                this.mVolume = iMax;
                if (this.mSvp != null) {
                    this.mSvp.setCurrentVolume(this.mVolume);
                }
                MediaRouter.dispatchRouteVolumeChanged(this);
                if (this.mGroup != null) {
                    this.mGroup.memberVolumeChanged(this);
                }
            }
        }

        @Override
        public void requestSetVolume(int i) {
            if (this.mVolumeHandling == 1) {
                if (this.mVcb == null) {
                    Log.e(MediaRouter.TAG, "Cannot requestSetVolume on user route - no volume callback set");
                } else {
                    this.mVcb.vcb.onVolumeSetRequest(this, i);
                }
            }
        }

        @Override
        public void requestUpdateVolume(int i) {
            if (this.mVolumeHandling == 1) {
                if (this.mVcb == null) {
                    Log.e(MediaRouter.TAG, "Cannot requestChangeVolume on user route - no volumec callback set");
                } else {
                    this.mVcb.vcb.onVolumeUpdateRequest(this, i);
                }
            }
        }

        public void setVolumeMax(int i) {
            if (this.mVolumeMax != i) {
                this.mVolumeMax = i;
                configureSessionVolume();
            }
        }

        public void setPlaybackStream(int i) {
            if (this.mPlaybackStream != i) {
                this.mPlaybackStream = i;
                configureSessionVolume();
            }
        }

        private void updatePlaybackInfoOnRcc() {
            configureSessionVolume();
        }

        private void configureSessionVolume() {
            if (this.mRcc == null) {
                if (MediaRouter.DEBUG) {
                    Log.d(MediaRouter.TAG, "No Rcc to configure volume for route " + ((Object) getName()));
                    return;
                }
                return;
            }
            MediaSession mediaSession = this.mRcc.getMediaSession();
            if (mediaSession == null) {
                if (MediaRouter.DEBUG) {
                    Log.d(MediaRouter.TAG, "Rcc has no session to configure volume");
                    return;
                }
                return;
            }
            if (this.mPlaybackType == 1) {
                int i = 0;
                if (this.mVolumeHandling == 1) {
                    i = 2;
                }
                if (this.mSvp == null || this.mSvp.getVolumeControl() != i || this.mSvp.getMaxVolume() != this.mVolumeMax) {
                    this.mSvp = new SessionVolumeProvider(i, this.mVolumeMax, this.mVolume);
                    mediaSession.setPlaybackToRemote(this.mSvp);
                    return;
                }
                return;
            }
            AudioAttributes.Builder builder = new AudioAttributes.Builder();
            builder.setLegacyStreamType(this.mPlaybackStream);
            mediaSession.setPlaybackToLocal(builder.build());
            this.mSvp = null;
        }

        class SessionVolumeProvider extends VolumeProvider {
            public SessionVolumeProvider(int i, int i2, int i3) {
                super(i, i2, i3);
            }

            @Override
            public void onSetVolumeTo(final int i) {
                MediaRouter.sStatic.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (UserRouteInfo.this.mVcb != null) {
                            UserRouteInfo.this.mVcb.vcb.onVolumeSetRequest(UserRouteInfo.this.mVcb.route, i);
                        }
                    }
                });
            }

            @Override
            public void onAdjustVolume(final int i) {
                MediaRouter.sStatic.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (UserRouteInfo.this.mVcb != null) {
                            UserRouteInfo.this.mVcb.vcb.onVolumeUpdateRequest(UserRouteInfo.this.mVcb.route, i);
                        }
                    }
                });
            }
        }
    }

    public static class RouteGroup extends RouteInfo {
        final ArrayList<RouteInfo> mRoutes;
        private boolean mUpdateName;

        RouteGroup(RouteCategory routeCategory) {
            super(routeCategory);
            this.mRoutes = new ArrayList<>();
            this.mGroup = this;
            this.mVolumeHandling = 0;
        }

        @Override
        CharSequence getName(Resources resources) {
            if (this.mUpdateName) {
                updateName();
            }
            return super.getName(resources);
        }

        public void addRoute(RouteInfo routeInfo) {
            if (routeInfo.getGroup() != null) {
                throw new IllegalStateException("Route " + routeInfo + " is already part of a group.");
            }
            if (routeInfo.getCategory() != this.mCategory) {
                throw new IllegalArgumentException("Route cannot be added to a group with a different category. (Route category=" + routeInfo.getCategory() + " group category=" + this.mCategory + ")");
            }
            int size = this.mRoutes.size();
            this.mRoutes.add(routeInfo);
            routeInfo.mGroup = this;
            this.mUpdateName = true;
            updateVolume();
            routeUpdated();
            MediaRouter.dispatchRouteGrouped(routeInfo, this, size);
        }

        public void addRoute(RouteInfo routeInfo, int i) {
            if (routeInfo.getGroup() != null) {
                throw new IllegalStateException("Route " + routeInfo + " is already part of a group.");
            }
            if (routeInfo.getCategory() != this.mCategory) {
                throw new IllegalArgumentException("Route cannot be added to a group with a different category. (Route category=" + routeInfo.getCategory() + " group category=" + this.mCategory + ")");
            }
            this.mRoutes.add(i, routeInfo);
            routeInfo.mGroup = this;
            this.mUpdateName = true;
            updateVolume();
            routeUpdated();
            MediaRouter.dispatchRouteGrouped(routeInfo, this, i);
        }

        public void removeRoute(RouteInfo routeInfo) {
            if (routeInfo.getGroup() != this) {
                throw new IllegalArgumentException("Route " + routeInfo + " is not a member of this group.");
            }
            this.mRoutes.remove(routeInfo);
            routeInfo.mGroup = null;
            this.mUpdateName = true;
            updateVolume();
            MediaRouter.dispatchRouteUngrouped(routeInfo, this);
            routeUpdated();
        }

        public void removeRoute(int i) {
            RouteInfo routeInfoRemove = this.mRoutes.remove(i);
            routeInfoRemove.mGroup = null;
            this.mUpdateName = true;
            updateVolume();
            MediaRouter.dispatchRouteUngrouped(routeInfoRemove, this);
            routeUpdated();
        }

        public int getRouteCount() {
            return this.mRoutes.size();
        }

        public RouteInfo getRouteAt(int i) {
            return this.mRoutes.get(i);
        }

        public void setIconDrawable(Drawable drawable) {
            this.mIcon = drawable;
        }

        public void setIconResource(int i) {
            setIconDrawable(MediaRouter.sStatic.mResources.getDrawable(i));
        }

        @Override
        public void requestSetVolume(int i) {
            int volumeMax = getVolumeMax();
            if (volumeMax == 0) {
                return;
            }
            float f = i / volumeMax;
            int routeCount = getRouteCount();
            for (int i2 = 0; i2 < routeCount; i2++) {
                getRouteAt(i2).requestSetVolume((int) (r3.getVolumeMax() * f));
            }
            if (i != this.mVolume) {
                this.mVolume = i;
                MediaRouter.dispatchRouteVolumeChanged(this);
            }
        }

        @Override
        public void requestUpdateVolume(int i) {
            if (getVolumeMax() == 0) {
                return;
            }
            int routeCount = getRouteCount();
            int i2 = 0;
            for (int i3 = 0; i3 < routeCount; i3++) {
                RouteInfo routeAt = getRouteAt(i3);
                routeAt.requestUpdateVolume(i);
                int volume = routeAt.getVolume();
                if (volume > i2) {
                    i2 = volume;
                }
            }
            if (i2 != this.mVolume) {
                this.mVolume = i2;
                MediaRouter.dispatchRouteVolumeChanged(this);
            }
        }

        void memberNameChanged(RouteInfo routeInfo, CharSequence charSequence) {
            this.mUpdateName = true;
            routeUpdated();
        }

        void memberStatusChanged(RouteInfo routeInfo, CharSequence charSequence) {
            setStatusInt(charSequence);
        }

        void memberVolumeChanged(RouteInfo routeInfo) {
            updateVolume();
        }

        void updateVolume() {
            int routeCount = getRouteCount();
            int i = 0;
            for (int i2 = 0; i2 < routeCount; i2++) {
                int volume = getRouteAt(i2).getVolume();
                if (volume > i) {
                    i = volume;
                }
            }
            if (i != this.mVolume) {
                this.mVolume = i;
                MediaRouter.dispatchRouteVolumeChanged(this);
            }
        }

        @Override
        void routeUpdated() {
            int size = this.mRoutes.size();
            if (size == 0) {
                MediaRouter.removeRouteStatic(this);
                return;
            }
            boolean z = true;
            boolean z2 = true;
            int i = 0;
            int i2 = 0;
            for (int i3 = 0; i3 < size; i3++) {
                RouteInfo routeInfo = this.mRoutes.get(i3);
                i |= routeInfo.mSupportedTypes;
                int volumeMax = routeInfo.getVolumeMax();
                if (volumeMax > i2) {
                    i2 = volumeMax;
                }
                z &= routeInfo.getPlaybackType() == 0;
                z2 &= routeInfo.getVolumeHandling() == 0;
            }
            this.mPlaybackType = z ? 0 : 1;
            this.mVolumeHandling = z2 ? 0 : 1;
            this.mSupportedTypes = i;
            this.mVolumeMax = i2;
            this.mIcon = size == 1 ? this.mRoutes.get(0).getIconDrawable() : null;
            super.routeUpdated();
        }

        void updateName() {
            StringBuilder sb = new StringBuilder();
            int size = this.mRoutes.size();
            for (int i = 0; i < size; i++) {
                RouteInfo routeInfo = this.mRoutes.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(routeInfo.getName());
            }
            this.mName = sb.toString();
            this.mUpdateName = false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString());
            sb.append('[');
            int size = this.mRoutes.size();
            for (int i = 0; i < size; i++) {
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

        RouteCategory(CharSequence charSequence, int i, boolean z) {
            this.mName = charSequence;
            this.mTypes = i;
            this.mGroupable = z;
        }

        RouteCategory(int i, int i2, boolean z) {
            this.mNameResId = i;
            this.mTypes = i2;
            this.mGroupable = z;
        }

        public CharSequence getName() {
            return getName(MediaRouter.sStatic.mResources);
        }

        public CharSequence getName(Context context) {
            return getName(context.getResources());
        }

        CharSequence getName(Resources resources) {
            if (this.mNameResId != 0) {
                return resources.getText(this.mNameResId);
            }
            return this.mName;
        }

        public List<RouteInfo> getRoutes(List<RouteInfo> list) {
            if (list == null) {
                list = new ArrayList<>();
            } else {
                list.clear();
            }
            int routeCountStatic = MediaRouter.getRouteCountStatic();
            for (int i = 0; i < routeCountStatic; i++) {
                RouteInfo routeAtStatic = MediaRouter.getRouteAtStatic(i);
                if (routeAtStatic.mCategory == this) {
                    list.add(routeAtStatic);
                }
            }
            return list;
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
            return "RouteCategory{ name=" + ((Object) getName()) + " types=" + MediaRouter.typesToString(this.mTypes) + " groupable=" + this.mGroupable + " }";
        }
    }

    static class CallbackInfo {
        public final Callback cb;
        public int flags;
        public final MediaRouter router;
        public int type;

        public CallbackInfo(Callback callback, int i, int i2, MediaRouter mediaRouter) {
            this.cb = callback;
            this.type = i;
            this.flags = i2;
            this.router = mediaRouter;
        }

        public boolean filterRouteEvent(RouteInfo routeInfo) {
            return filterRouteEvent(routeInfo.mSupportedTypes);
        }

        public boolean filterRouteEvent(int i) {
            return ((this.flags & 2) == 0 && (i & this.type) == 0) ? false : true;
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

        public void onRoutePresentationDisplayChanged(MediaRouter mediaRouter, RouteInfo routeInfo) {
        }
    }

    public static class SimpleCallback extends Callback {
        @Override
        public void onRouteSelected(MediaRouter mediaRouter, int i, RouteInfo routeInfo) {
        }

        @Override
        public void onRouteUnselected(MediaRouter mediaRouter, int i, RouteInfo routeInfo) {
        }

        @Override
        public void onRouteAdded(MediaRouter mediaRouter, RouteInfo routeInfo) {
        }

        @Override
        public void onRouteRemoved(MediaRouter mediaRouter, RouteInfo routeInfo) {
        }

        @Override
        public void onRouteChanged(MediaRouter mediaRouter, RouteInfo routeInfo) {
        }

        @Override
        public void onRouteGrouped(MediaRouter mediaRouter, RouteInfo routeInfo, RouteGroup routeGroup, int i) {
        }

        @Override
        public void onRouteUngrouped(MediaRouter mediaRouter, RouteInfo routeInfo, RouteGroup routeGroup) {
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter mediaRouter, RouteInfo routeInfo) {
        }
    }

    static class VolumeCallbackInfo {
        public final RouteInfo route;
        public final VolumeCallback vcb;

        public VolumeCallbackInfo(VolumeCallback volumeCallback, RouteInfo routeInfo) {
            this.vcb = volumeCallback;
            this.route = routeInfo;
        }
    }

    static class VolumeChangeReceiver extends BroadcastReceiver {
        VolumeChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra;
            if (intent.getAction().equals(AudioManager.VOLUME_CHANGED_ACTION) && intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1) == 3 && (intExtra = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0)) != intent.getIntExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, 0)) {
                MediaRouter.systemVolumeChanged(intExtra);
            }
        }
    }

    static class WifiDisplayStatusChangedReceiver extends BroadcastReceiver {
        WifiDisplayStatusChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED)) {
                MediaRouter.updateWifiDisplayStatus((WifiDisplayStatus) intent.getParcelableExtra(DisplayManager.EXTRA_WIFI_DISPLAY_STATUS));
            }
        }
    }
}
