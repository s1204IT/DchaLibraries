package android.support.v7.media;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityManagerCompat;
import android.support.v4.hardware.display.DisplayManagerCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.util.ObjectsCompat;
import android.support.v4.util.Pair;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.RegisteredMediaRouteProviderWatcher;
import android.support.v7.media.RemoteControlClientCompat;
import android.support.v7.media.SystemMediaRouteProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MediaRouter {
    static final boolean DEBUG = Log.isLoggable("MediaRouter", 3);
    static GlobalMediaRouter sGlobal;
    final ArrayList<CallbackRecord> mCallbackRecords;
    final Context mContext;

    private MediaRouter(Context context) {
        this.mCallbackRecords = new ArrayList<>();
        this.mContext = context;
    }

    public static MediaRouter getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        checkCallingThread();
        if (sGlobal == null) {
            sGlobal = new GlobalMediaRouter(context.getApplicationContext());
            sGlobal.start();
        }
        return sGlobal.getRouter(context);
    }

    public List<RouteInfo> getRoutes() {
        checkCallingThread();
        return sGlobal.getRoutes();
    }

    public RouteInfo getSelectedRoute() {
        checkCallingThread();
        return sGlobal.getSelectedRoute();
    }

    public void unselect(int reason) {
        if (reason < 0 || reason > 3) {
            throw new IllegalArgumentException("Unsupported reason to unselect route");
        }
        checkCallingThread();
        RouteInfo fallbackRoute = sGlobal.chooseFallbackRoute();
        if (sGlobal.getSelectedRoute() != fallbackRoute) {
            sGlobal.selectRoute(fallbackRoute, reason);
        } else {
            sGlobal.selectRoute(sGlobal.getDefaultRoute(), reason);
        }
    }

    public boolean isRouteAvailable(MediaRouteSelector selector, int flags) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }
        checkCallingThread();
        return sGlobal.isRouteAvailable(selector, flags);
    }

    public void addCallback(MediaRouteSelector selector, Callback callback) {
        addCallback(selector, callback, 0);
    }

    public void addCallback(MediaRouteSelector selector, Callback callback, int flags) {
        CallbackRecord record;
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        checkCallingThread();
        if (DEBUG) {
            Log.d("MediaRouter", "addCallback: selector=" + selector + ", callback=" + callback + ", flags=" + Integer.toHexString(flags));
        }
        int index = findCallbackRecord(callback);
        if (index < 0) {
            record = new CallbackRecord(this, callback);
            this.mCallbackRecords.add(record);
        } else {
            record = this.mCallbackRecords.get(index);
        }
        boolean updateNeeded = false;
        if (((~record.mFlags) & flags) != 0) {
            record.mFlags |= flags;
            updateNeeded = true;
        }
        if (!record.mSelector.contains(selector)) {
            record.mSelector = new MediaRouteSelector.Builder(record.mSelector).addSelector(selector).build();
            updateNeeded = true;
        }
        if (updateNeeded) {
            sGlobal.updateDiscoveryRequest();
        }
    }

    public void removeCallback(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        checkCallingThread();
        if (DEBUG) {
            Log.d("MediaRouter", "removeCallback: callback=" + callback);
        }
        int index = findCallbackRecord(callback);
        if (index >= 0) {
            this.mCallbackRecords.remove(index);
            sGlobal.updateDiscoveryRequest();
        }
    }

    private int findCallbackRecord(Callback callback) {
        int count = this.mCallbackRecords.size();
        for (int i = 0; i < count; i++) {
            if (this.mCallbackRecords.get(i).mCallback == callback) {
                return i;
            }
        }
        return -1;
    }

    public MediaSessionCompat.Token getMediaSessionToken() {
        return sGlobal.getMediaSessionToken();
    }

    static void checkCallingThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("The media router service must only be accessed on the application's main thread.");
        }
    }

    public static class RouteInfo {
        private boolean mCanDisconnect;
        private boolean mConnecting;
        private int mConnectionState;
        private String mDescription;
        MediaRouteDescriptor mDescriptor;
        private final String mDescriptorId;
        private int mDeviceType;
        private boolean mEnabled;
        private Bundle mExtras;
        private Uri mIconUri;
        private String mName;
        private int mPlaybackStream;
        private int mPlaybackType;
        private Display mPresentationDisplay;
        private final ProviderInfo mProvider;
        private IntentSender mSettingsIntent;
        private final String mUniqueId;
        private int mVolume;
        private int mVolumeHandling;
        private int mVolumeMax;
        private final ArrayList<IntentFilter> mControlFilters = new ArrayList<>();
        private int mPresentationDisplayId = -1;

        RouteInfo(ProviderInfo provider, String descriptorId, String uniqueId) {
            this.mProvider = provider;
            this.mDescriptorId = descriptorId;
            this.mUniqueId = uniqueId;
        }

        public ProviderInfo getProvider() {
            return this.mProvider;
        }

        public String getId() {
            return this.mUniqueId;
        }

        public String getName() {
            return this.mName;
        }

        public String getDescription() {
            return this.mDescription;
        }

        public Uri getIconUri() {
            return this.mIconUri;
        }

        public boolean isEnabled() {
            return this.mEnabled;
        }

        public boolean isConnecting() {
            return this.mConnecting;
        }

        public int getConnectionState() {
            return this.mConnectionState;
        }

        public boolean isSelected() {
            MediaRouter.checkCallingThread();
            return MediaRouter.sGlobal.getSelectedRoute() == this;
        }

        public boolean isDefault() {
            MediaRouter.checkCallingThread();
            return MediaRouter.sGlobal.getDefaultRoute() == this;
        }

        public boolean matchesSelector(MediaRouteSelector selector) {
            if (selector == null) {
                throw new IllegalArgumentException("selector must not be null");
            }
            MediaRouter.checkCallingThread();
            return selector.matchesControlFilters(this.mControlFilters);
        }

        public boolean supportsControlCategory(String category) {
            if (category == null) {
                throw new IllegalArgumentException("category must not be null");
            }
            MediaRouter.checkCallingThread();
            int count = this.mControlFilters.size();
            for (int i = 0; i < count; i++) {
                if (this.mControlFilters.get(i).hasCategory(category)) {
                    return true;
                }
            }
            return false;
        }

        public int getPlaybackType() {
            return this.mPlaybackType;
        }

        public int getPlaybackStream() {
            return this.mPlaybackStream;
        }

        public int getDeviceType() {
            return this.mDeviceType;
        }

        public boolean isDefaultOrBluetooth() {
            if (isDefault() || this.mDeviceType == 3) {
                return true;
            }
            return isSystemMediaRouteProvider(this) && supportsControlCategory("android.media.intent.category.LIVE_AUDIO") && !supportsControlCategory("android.media.intent.category.LIVE_VIDEO");
        }

        boolean isSelectable() {
            return this.mDescriptor != null && this.mEnabled;
        }

        private static boolean isSystemMediaRouteProvider(RouteInfo route) {
            return TextUtils.equals(route.getProviderInstance().getMetadata().getPackageName(), "android");
        }

        public int getVolumeHandling() {
            return this.mVolumeHandling;
        }

        public int getVolume() {
            return this.mVolume;
        }

        public int getVolumeMax() {
            return this.mVolumeMax;
        }

        public boolean canDisconnect() {
            return this.mCanDisconnect;
        }

        public void requestSetVolume(int volume) {
            MediaRouter.checkCallingThread();
            MediaRouter.sGlobal.requestSetVolume(this, Math.min(this.mVolumeMax, Math.max(0, volume)));
        }

        public void requestUpdateVolume(int delta) {
            MediaRouter.checkCallingThread();
            if (delta != 0) {
                MediaRouter.sGlobal.requestUpdateVolume(this, delta);
            }
        }

        public int getPresentationDisplayId() {
            return this.mPresentationDisplayId;
        }

        public void select() {
            MediaRouter.checkCallingThread();
            MediaRouter.sGlobal.selectRoute(this);
        }

        public String toString() {
            return "MediaRouter.RouteInfo{ uniqueId=" + this.mUniqueId + ", name=" + this.mName + ", description=" + this.mDescription + ", iconUri=" + this.mIconUri + ", enabled=" + this.mEnabled + ", connecting=" + this.mConnecting + ", connectionState=" + this.mConnectionState + ", canDisconnect=" + this.mCanDisconnect + ", playbackType=" + this.mPlaybackType + ", playbackStream=" + this.mPlaybackStream + ", deviceType=" + this.mDeviceType + ", volumeHandling=" + this.mVolumeHandling + ", volume=" + this.mVolume + ", volumeMax=" + this.mVolumeMax + ", presentationDisplayId=" + this.mPresentationDisplayId + ", extras=" + this.mExtras + ", settingsIntent=" + this.mSettingsIntent + ", providerPackageName=" + this.mProvider.getPackageName() + " }";
        }

        int maybeUpdateDescriptor(MediaRouteDescriptor descriptor) {
            if (this.mDescriptor == descriptor) {
                return 0;
            }
            int changes = updateDescriptor(descriptor);
            return changes;
        }

        int updateDescriptor(MediaRouteDescriptor descriptor) {
            int changes = 0;
            this.mDescriptor = descriptor;
            if (descriptor == null) {
                return 0;
            }
            if (!ObjectsCompat.equals(this.mName, descriptor.getName())) {
                this.mName = descriptor.getName();
                changes = 0 | 1;
            }
            if (!ObjectsCompat.equals(this.mDescription, descriptor.getDescription())) {
                this.mDescription = descriptor.getDescription();
                changes |= 1;
            }
            if (!ObjectsCompat.equals(this.mIconUri, descriptor.getIconUri())) {
                this.mIconUri = descriptor.getIconUri();
                changes |= 1;
            }
            if (this.mEnabled != descriptor.isEnabled()) {
                this.mEnabled = descriptor.isEnabled();
                changes |= 1;
            }
            if (this.mConnecting != descriptor.isConnecting()) {
                this.mConnecting = descriptor.isConnecting();
                changes |= 1;
            }
            if (this.mConnectionState != descriptor.getConnectionState()) {
                this.mConnectionState = descriptor.getConnectionState();
                changes |= 1;
            }
            if (!this.mControlFilters.equals(descriptor.getControlFilters())) {
                this.mControlFilters.clear();
                this.mControlFilters.addAll(descriptor.getControlFilters());
                changes |= 1;
            }
            if (this.mPlaybackType != descriptor.getPlaybackType()) {
                this.mPlaybackType = descriptor.getPlaybackType();
                changes |= 1;
            }
            if (this.mPlaybackStream != descriptor.getPlaybackStream()) {
                this.mPlaybackStream = descriptor.getPlaybackStream();
                changes |= 1;
            }
            if (this.mDeviceType != descriptor.getDeviceType()) {
                this.mDeviceType = descriptor.getDeviceType();
                changes |= 1;
            }
            if (this.mVolumeHandling != descriptor.getVolumeHandling()) {
                this.mVolumeHandling = descriptor.getVolumeHandling();
                changes |= 3;
            }
            if (this.mVolume != descriptor.getVolume()) {
                this.mVolume = descriptor.getVolume();
                changes |= 3;
            }
            if (this.mVolumeMax != descriptor.getVolumeMax()) {
                this.mVolumeMax = descriptor.getVolumeMax();
                changes |= 3;
            }
            if (this.mPresentationDisplayId != descriptor.getPresentationDisplayId()) {
                this.mPresentationDisplayId = descriptor.getPresentationDisplayId();
                this.mPresentationDisplay = null;
                changes |= 5;
            }
            if (!ObjectsCompat.equals(this.mExtras, descriptor.getExtras())) {
                this.mExtras = descriptor.getExtras();
                changes |= 1;
            }
            if (!ObjectsCompat.equals(this.mSettingsIntent, descriptor.getSettingsActivity())) {
                this.mSettingsIntent = descriptor.getSettingsActivity();
                changes |= 1;
            }
            if (this.mCanDisconnect != descriptor.canDisconnectAndKeepPlaying()) {
                this.mCanDisconnect = descriptor.canDisconnectAndKeepPlaying();
                return changes | 5;
            }
            return changes;
        }

        String getDescriptorId() {
            return this.mDescriptorId;
        }

        public MediaRouteProvider getProviderInstance() {
            return this.mProvider.getProviderInstance();
        }
    }

    public static class RouteGroup extends RouteInfo {
        private List<RouteInfo> mRoutes;

        RouteGroup(ProviderInfo provider, String descriptorId, String uniqueId) {
            super(provider, descriptorId, uniqueId);
            this.mRoutes = new ArrayList();
        }

        public List<RouteInfo> getRoutes() {
            return this.mRoutes;
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

        @Override
        int maybeUpdateDescriptor(MediaRouteDescriptor descriptor) {
            boolean changed = false;
            if (this.mDescriptor != descriptor) {
                this.mDescriptor = descriptor;
                if (descriptor != null) {
                    List<String> groupMemberIds = descriptor.getGroupMemberIds();
                    List<RouteInfo> routes = new ArrayList<>();
                    if (groupMemberIds == null) {
                        Log.w("MediaRouter", "groupMemberIds shouldn't be null.");
                        changed = true;
                    } else {
                        changed = groupMemberIds.size() != this.mRoutes.size();
                        for (String groupMemberId : groupMemberIds) {
                            String uniqueId = MediaRouter.sGlobal.getUniqueId(getProvider(), groupMemberId);
                            RouteInfo groupMember = MediaRouter.sGlobal.getRoute(uniqueId);
                            if (groupMember != null) {
                                routes.add(groupMember);
                                if (!changed && !this.mRoutes.contains(groupMember)) {
                                    changed = true;
                                }
                            }
                        }
                    }
                    if (changed) {
                        this.mRoutes = routes;
                    }
                }
            }
            return super.updateDescriptor(descriptor) | (changed ? 1 : 0);
        }
    }

    public static final class ProviderInfo {
        private MediaRouteProviderDescriptor mDescriptor;
        private final MediaRouteProvider.ProviderMetadata mMetadata;
        private final MediaRouteProvider mProviderInstance;
        private final List<RouteInfo> mRoutes = new ArrayList();

        ProviderInfo(MediaRouteProvider provider) {
            this.mProviderInstance = provider;
            this.mMetadata = provider.getMetadata();
        }

        public MediaRouteProvider getProviderInstance() {
            MediaRouter.checkCallingThread();
            return this.mProviderInstance;
        }

        public String getPackageName() {
            return this.mMetadata.getPackageName();
        }

        public ComponentName getComponentName() {
            return this.mMetadata.getComponentName();
        }

        boolean updateDescriptor(MediaRouteProviderDescriptor descriptor) {
            if (this.mDescriptor != descriptor) {
                this.mDescriptor = descriptor;
                return true;
            }
            return false;
        }

        int findRouteByDescriptorId(String id) {
            int count = this.mRoutes.size();
            for (int i = 0; i < count; i++) {
                if (this.mRoutes.get(i).mDescriptorId.equals(id)) {
                    return i;
                }
            }
            return -1;
        }

        public String toString() {
            return "MediaRouter.RouteProviderInfo{ packageName=" + getPackageName() + " }";
        }
    }

    public static abstract class Callback {
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
        }

        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
        }

        public void onRouteUnselected(MediaRouter router, RouteInfo route, int reason) {
            onRouteUnselected(router, route);
        }

        public void onRouteAdded(MediaRouter router, RouteInfo route) {
        }

        public void onRouteRemoved(MediaRouter router, RouteInfo route) {
        }

        public void onRouteChanged(MediaRouter router, RouteInfo route) {
        }

        public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) {
        }

        public void onRoutePresentationDisplayChanged(MediaRouter router, RouteInfo route) {
        }

        public void onProviderAdded(MediaRouter router, ProviderInfo provider) {
        }

        public void onProviderRemoved(MediaRouter router, ProviderInfo provider) {
        }

        public void onProviderChanged(MediaRouter router, ProviderInfo provider) {
        }
    }

    public static abstract class ControlRequestCallback {
        public void onResult(Bundle data) {
        }

        public void onError(String error, Bundle data) {
        }
    }

    private static final class CallbackRecord {
        public final Callback mCallback;
        public int mFlags;
        public final MediaRouter mRouter;
        public MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;

        public CallbackRecord(MediaRouter router, Callback callback) {
            this.mRouter = router;
            this.mCallback = callback;
        }

        public boolean filterRouteEvent(RouteInfo route) {
            return (this.mFlags & 2) != 0 || route.matchesSelector(this.mSelector);
        }
    }

    private static final class GlobalMediaRouter implements RegisteredMediaRouteProviderWatcher.Callback, SystemMediaRouteProvider.SyncCallback {
        final Context mApplicationContext;
        private RouteInfo mBluetoothRoute;
        private MediaSessionCompat mCompatSession;
        private RouteInfo mDefaultRoute;
        private MediaRouteDiscoveryRequest mDiscoveryRequest;
        private final DisplayManagerCompat mDisplayManager;
        private final boolean mLowRam;
        private MediaSessionRecord mMediaSession;
        private RegisteredMediaRouteProviderWatcher mRegisteredProviderWatcher;
        RouteInfo mSelectedRoute;
        private MediaRouteProvider.RouteController mSelectedRouteController;
        final SystemMediaRouteProvider mSystemProvider;
        final ArrayList<WeakReference<MediaRouter>> mRouters = new ArrayList<>();
        private final ArrayList<RouteInfo> mRoutes = new ArrayList<>();
        private final Map<Pair<String, String>, String> mUniqueIdMap = new HashMap();
        private final ArrayList<ProviderInfo> mProviders = new ArrayList<>();
        private final ArrayList<RemoteControlClientRecord> mRemoteControlClients = new ArrayList<>();
        final RemoteControlClientCompat.PlaybackInfo mPlaybackInfo = new RemoteControlClientCompat.PlaybackInfo();
        private final ProviderCallback mProviderCallback = new ProviderCallback();
        final CallbackHandler mCallbackHandler = new CallbackHandler();
        private final Map<String, MediaRouteProvider.RouteController> mRouteControllerMap = new HashMap();
        private MediaSessionCompat.OnActiveChangeListener mSessionActiveListener = new MediaSessionCompat.OnActiveChangeListener() {
        };

        GlobalMediaRouter(Context applicationContext) {
            this.mApplicationContext = applicationContext;
            this.mDisplayManager = DisplayManagerCompat.getInstance(applicationContext);
            this.mLowRam = ActivityManagerCompat.isLowRamDevice((ActivityManager) applicationContext.getSystemService("activity"));
            this.mSystemProvider = SystemMediaRouteProvider.obtain(applicationContext, this);
        }

        public void start() {
            addProvider(this.mSystemProvider);
            this.mRegisteredProviderWatcher = new RegisteredMediaRouteProviderWatcher(this.mApplicationContext, this);
            this.mRegisteredProviderWatcher.start();
        }

        public MediaRouter getRouter(Context context) {
            int i = this.mRouters.size();
            while (true) {
                i--;
                if (i >= 0) {
                    MediaRouter router = this.mRouters.get(i).get();
                    if (router == null) {
                        this.mRouters.remove(i);
                    } else if (router.mContext == context) {
                        return router;
                    }
                } else {
                    MediaRouter router2 = new MediaRouter(context);
                    this.mRouters.add(new WeakReference<>(router2));
                    return router2;
                }
            }
        }

        public void requestSetVolume(RouteInfo route, int volume) {
            MediaRouteProvider.RouteController controller;
            if (route == this.mSelectedRoute && this.mSelectedRouteController != null) {
                this.mSelectedRouteController.onSetVolume(volume);
            } else if (!this.mRouteControllerMap.isEmpty() && (controller = this.mRouteControllerMap.get(route.mDescriptorId)) != null) {
                controller.onSetVolume(volume);
            }
        }

        public void requestUpdateVolume(RouteInfo route, int delta) {
            if (route == this.mSelectedRoute && this.mSelectedRouteController != null) {
                this.mSelectedRouteController.onUpdateVolume(delta);
            }
        }

        public RouteInfo getRoute(String uniqueId) {
            for (RouteInfo info : this.mRoutes) {
                if (info.mUniqueId.equals(uniqueId)) {
                    return info;
                }
            }
            return null;
        }

        public List<RouteInfo> getRoutes() {
            return this.mRoutes;
        }

        RouteInfo getDefaultRoute() {
            if (this.mDefaultRoute == null) {
                throw new IllegalStateException("There is no default route.  The media router has not yet been fully initialized.");
            }
            return this.mDefaultRoute;
        }

        RouteInfo getBluetoothRoute() {
            return this.mBluetoothRoute;
        }

        RouteInfo getSelectedRoute() {
            if (this.mSelectedRoute == null) {
                throw new IllegalStateException("There is no currently selected route.  The media router has not yet been fully initialized.");
            }
            return this.mSelectedRoute;
        }

        void selectRoute(RouteInfo route) {
            selectRoute(route, 3);
        }

        void selectRoute(RouteInfo route, int unselectReason) {
            if (this.mRoutes.contains(route)) {
                if (!route.mEnabled) {
                    Log.w("MediaRouter", "Ignoring attempt to select disabled route: " + route);
                    return;
                }
                setSelectedRouteInternal(route, unselectReason);
                return;
            }
            Log.w("MediaRouter", "Ignoring attempt to select removed route: " + route);
        }

        public boolean isRouteAvailable(MediaRouteSelector selector, int flags) {
            if (selector.isEmpty()) {
                return false;
            }
            if ((flags & 2) == 0 && this.mLowRam) {
                return true;
            }
            int routeCount = this.mRoutes.size();
            for (int i = 0; i < routeCount; i++) {
                RouteInfo route = this.mRoutes.get(i);
                if (((flags & 1) == 0 || !route.isDefaultOrBluetooth()) && route.matchesSelector(selector)) {
                    return true;
                }
            }
            return false;
        }

        public void updateDiscoveryRequest() {
            int j;
            boolean discover = false;
            boolean activeScan = false;
            MediaRouteSelector.Builder builder = new MediaRouteSelector.Builder();
            int i = this.mRouters.size();
            while (true) {
                i--;
                j = 0;
                if (i < 0) {
                    break;
                }
                MediaRouter router = this.mRouters.get(i).get();
                if (router == null) {
                    this.mRouters.remove(i);
                } else {
                    int count = router.mCallbackRecords.size();
                    while (j < count) {
                        CallbackRecord callback = router.mCallbackRecords.get(j);
                        builder.addSelector(callback.mSelector);
                        if ((callback.mFlags & 1) != 0) {
                            activeScan = true;
                            discover = true;
                        }
                        if ((callback.mFlags & 4) != 0 && !this.mLowRam) {
                            discover = true;
                        }
                        if ((callback.mFlags & 8) != 0) {
                            discover = true;
                        }
                        j++;
                    }
                }
            }
            MediaRouteSelector selector = discover ? builder.build() : MediaRouteSelector.EMPTY;
            if (this.mDiscoveryRequest != null && this.mDiscoveryRequest.getSelector().equals(selector) && this.mDiscoveryRequest.isActiveScan() == activeScan) {
                return;
            }
            if (selector.isEmpty() && !activeScan) {
                if (this.mDiscoveryRequest == null) {
                    return;
                } else {
                    this.mDiscoveryRequest = null;
                }
            } else {
                this.mDiscoveryRequest = new MediaRouteDiscoveryRequest(selector, activeScan);
            }
            if (MediaRouter.DEBUG) {
                Log.d("MediaRouter", "Updated discovery request: " + this.mDiscoveryRequest);
            }
            if (discover && !activeScan && this.mLowRam) {
                Log.i("MediaRouter", "Forcing passive route discovery on a low-RAM device, system performance may be affected.  Please consider using CALLBACK_FLAG_REQUEST_DISCOVERY instead of CALLBACK_FLAG_FORCE_DISCOVERY.");
            }
            int providerCount = this.mProviders.size();
            while (j < providerCount) {
                this.mProviders.get(j).mProviderInstance.setDiscoveryRequest(this.mDiscoveryRequest);
                j++;
            }
        }

        @Override
        public void addProvider(MediaRouteProvider providerInstance) {
            int index = findProviderInfo(providerInstance);
            if (index < 0) {
                ProviderInfo provider = new ProviderInfo(providerInstance);
                this.mProviders.add(provider);
                if (MediaRouter.DEBUG) {
                    Log.d("MediaRouter", "Provider added: " + provider);
                }
                this.mCallbackHandler.post(513, provider);
                updateProviderContents(provider, providerInstance.getDescriptor());
                providerInstance.setCallback(this.mProviderCallback);
                providerInstance.setDiscoveryRequest(this.mDiscoveryRequest);
            }
        }

        @Override
        public void removeProvider(MediaRouteProvider providerInstance) {
            int index = findProviderInfo(providerInstance);
            if (index >= 0) {
                providerInstance.setCallback(null);
                providerInstance.setDiscoveryRequest(null);
                ProviderInfo provider = this.mProviders.get(index);
                updateProviderContents(provider, null);
                if (MediaRouter.DEBUG) {
                    Log.d("MediaRouter", "Provider removed: " + provider);
                }
                this.mCallbackHandler.post(514, provider);
                this.mProviders.remove(index);
            }
        }

        void updateProviderDescriptor(MediaRouteProvider providerInstance, MediaRouteProviderDescriptor descriptor) {
            int index = findProviderInfo(providerInstance);
            if (index >= 0) {
                ProviderInfo provider = this.mProviders.get(index);
                updateProviderContents(provider, descriptor);
            }
        }

        private int findProviderInfo(MediaRouteProvider providerInstance) {
            int count = this.mProviders.size();
            for (int i = 0; i < count; i++) {
                if (this.mProviders.get(i).mProviderInstance == providerInstance) {
                    return i;
                }
            }
            return -1;
        }

        private void updateProviderContents(ProviderInfo provider, MediaRouteProviderDescriptor providerDescriptor) {
            boolean selectedRouteDescriptorChanged;
            List<MediaRouteDescriptor> routeDescriptors;
            int routeCount;
            if (provider.updateDescriptor(providerDescriptor)) {
                int targetIndex = 0;
                if (providerDescriptor != null) {
                    if (providerDescriptor.isValid()) {
                        List<MediaRouteDescriptor> routeDescriptors2 = providerDescriptor.getRoutes();
                        int routeCount2 = routeDescriptors2.size();
                        List<Pair<RouteInfo, MediaRouteDescriptor>> addedGroups = new ArrayList<>();
                        List<Pair<RouteInfo, MediaRouteDescriptor>> updatedGroups = new ArrayList<>();
                        selectedRouteDescriptorChanged = false;
                        int targetIndex2 = 0;
                        int targetIndex3 = 0;
                        while (targetIndex3 < routeCount2) {
                            MediaRouteDescriptor routeDescriptor = routeDescriptors2.get(targetIndex3);
                            String id = routeDescriptor.getId();
                            int sourceIndex = provider.findRouteByDescriptorId(id);
                            boolean isGroup = routeDescriptor.getGroupMemberIds() != null;
                            if (sourceIndex < 0) {
                                String uniqueId = assignRouteUniqueId(provider, id);
                                RouteInfo route = isGroup ? new RouteGroup(provider, id, uniqueId) : new RouteInfo(provider, id, uniqueId);
                                routeDescriptors = routeDescriptors2;
                                int targetIndex4 = targetIndex2 + 1;
                                provider.mRoutes.add(targetIndex2, route);
                                this.mRoutes.add(route);
                                if (isGroup) {
                                    addedGroups.add(new Pair<>(route, routeDescriptor));
                                    routeCount = routeCount2;
                                } else {
                                    route.maybeUpdateDescriptor(routeDescriptor);
                                    if (MediaRouter.DEBUG) {
                                        StringBuilder sb = new StringBuilder();
                                        routeCount = routeCount2;
                                        sb.append("Route added: ");
                                        sb.append(route);
                                        Log.d("MediaRouter", sb.toString());
                                    } else {
                                        routeCount = routeCount2;
                                    }
                                    this.mCallbackHandler.post(257, route);
                                }
                                targetIndex2 = targetIndex4;
                            } else {
                                routeDescriptors = routeDescriptors2;
                                routeCount = routeCount2;
                                if (sourceIndex >= targetIndex2) {
                                    RouteInfo route2 = (RouteInfo) provider.mRoutes.get(sourceIndex);
                                    if ((route2 instanceof RouteGroup) != isGroup) {
                                        route2 = isGroup ? new RouteGroup(provider, id, route2.getId()) : new RouteInfo(provider, id, route2.getId());
                                        provider.mRoutes.set(sourceIndex, route2);
                                    }
                                    int targetIndex5 = targetIndex2 + 1;
                                    Collections.swap(provider.mRoutes, sourceIndex, targetIndex2);
                                    if (route2 instanceof RouteGroup) {
                                        updatedGroups.add(new Pair<>(route2, routeDescriptor));
                                    } else if (updateRouteDescriptorAndNotify(route2, routeDescriptor) != 0 && route2 == this.mSelectedRoute) {
                                        selectedRouteDescriptorChanged = true;
                                    }
                                    targetIndex2 = targetIndex5;
                                } else {
                                    Log.w("MediaRouter", "Ignoring route descriptor with duplicate id: " + routeDescriptor);
                                }
                            }
                            targetIndex3++;
                            routeDescriptors2 = routeDescriptors;
                            routeCount2 = routeCount;
                        }
                        for (Pair<RouteInfo, MediaRouteDescriptor> pair : addedGroups) {
                            RouteInfo route3 = pair.first;
                            route3.maybeUpdateDescriptor(pair.second);
                            if (MediaRouter.DEBUG) {
                                Log.d("MediaRouter", "Route added: " + route3);
                            }
                            this.mCallbackHandler.post(257, route3);
                        }
                        for (Pair<RouteInfo, MediaRouteDescriptor> pair2 : updatedGroups) {
                            RouteInfo route4 = pair2.first;
                            if (updateRouteDescriptorAndNotify(route4, pair2.second) != 0 && route4 == this.mSelectedRoute) {
                                selectedRouteDescriptorChanged = true;
                            }
                        }
                        targetIndex = targetIndex2;
                    } else {
                        Log.w("MediaRouter", "Ignoring invalid provider descriptor: " + providerDescriptor);
                        selectedRouteDescriptorChanged = false;
                    }
                } else {
                    selectedRouteDescriptorChanged = false;
                }
                for (int i = provider.mRoutes.size() - 1; i >= targetIndex; i--) {
                    RouteInfo route5 = (RouteInfo) provider.mRoutes.get(i);
                    route5.maybeUpdateDescriptor(null);
                    this.mRoutes.remove(route5);
                }
                updateSelectedRouteIfNeeded(selectedRouteDescriptorChanged);
                for (int i2 = provider.mRoutes.size() - 1; i2 >= targetIndex; i2--) {
                    RouteInfo route6 = (RouteInfo) provider.mRoutes.remove(i2);
                    if (MediaRouter.DEBUG) {
                        Log.d("MediaRouter", "Route removed: " + route6);
                    }
                    this.mCallbackHandler.post(258, route6);
                }
                if (MediaRouter.DEBUG) {
                    Log.d("MediaRouter", "Provider changed: " + provider);
                }
                this.mCallbackHandler.post(515, provider);
            }
        }

        private int updateRouteDescriptorAndNotify(RouteInfo route, MediaRouteDescriptor routeDescriptor) {
            int changes = route.maybeUpdateDescriptor(routeDescriptor);
            if (changes != 0) {
                if ((changes & 1) != 0) {
                    if (MediaRouter.DEBUG) {
                        Log.d("MediaRouter", "Route changed: " + route);
                    }
                    this.mCallbackHandler.post(259, route);
                }
                if ((changes & 2) != 0) {
                    if (MediaRouter.DEBUG) {
                        Log.d("MediaRouter", "Route volume changed: " + route);
                    }
                    this.mCallbackHandler.post(260, route);
                }
                if ((changes & 4) != 0) {
                    if (MediaRouter.DEBUG) {
                        Log.d("MediaRouter", "Route presentation display changed: " + route);
                    }
                    this.mCallbackHandler.post(261, route);
                }
            }
            return changes;
        }

        private String assignRouteUniqueId(ProviderInfo provider, String routeDescriptorId) {
            String componentName = provider.getComponentName().flattenToShortString();
            String uniqueId = componentName + ":" + routeDescriptorId;
            if (findRouteByUniqueId(uniqueId) < 0) {
                this.mUniqueIdMap.put(new Pair<>(componentName, routeDescriptorId), uniqueId);
                return uniqueId;
            }
            Log.w("MediaRouter", "Either " + routeDescriptorId + " isn't unique in " + componentName + " or we're trying to assign a unique ID for an already added route");
            int i = 2;
            while (true) {
                String newUniqueId = String.format(Locale.US, "%s_%d", uniqueId, Integer.valueOf(i));
                if (findRouteByUniqueId(newUniqueId) >= 0) {
                    i++;
                } else {
                    this.mUniqueIdMap.put(new Pair<>(componentName, routeDescriptorId), newUniqueId);
                    return newUniqueId;
                }
            }
        }

        private int findRouteByUniqueId(String uniqueId) {
            int count = this.mRoutes.size();
            for (int i = 0; i < count; i++) {
                if (this.mRoutes.get(i).mUniqueId.equals(uniqueId)) {
                    return i;
                }
            }
            return -1;
        }

        public String getUniqueId(ProviderInfo provider, String routeDescriptorId) {
            String componentName = provider.getComponentName().flattenToShortString();
            return this.mUniqueIdMap.get(new Pair(componentName, routeDescriptorId));
        }

        public void updateSelectedRouteIfNeeded(boolean selectedRouteDescriptorChanged) {
            if (this.mDefaultRoute != null && !this.mDefaultRoute.isSelectable()) {
                Log.i("MediaRouter", "Clearing the default route because it is no longer selectable: " + this.mDefaultRoute);
                this.mDefaultRoute = null;
            }
            if (this.mDefaultRoute == null && !this.mRoutes.isEmpty()) {
                Iterator<RouteInfo> it = this.mRoutes.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    RouteInfo route = it.next();
                    if (isSystemDefaultRoute(route) && route.isSelectable()) {
                        this.mDefaultRoute = route;
                        Log.i("MediaRouter", "Found default route: " + this.mDefaultRoute);
                        break;
                    }
                }
            }
            if (this.mBluetoothRoute != null && !this.mBluetoothRoute.isSelectable()) {
                Log.i("MediaRouter", "Clearing the bluetooth route because it is no longer selectable: " + this.mBluetoothRoute);
                this.mBluetoothRoute = null;
            }
            if (this.mBluetoothRoute == null && !this.mRoutes.isEmpty()) {
                Iterator<RouteInfo> it2 = this.mRoutes.iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        break;
                    }
                    RouteInfo route2 = it2.next();
                    if (isSystemLiveAudioOnlyRoute(route2) && route2.isSelectable()) {
                        this.mBluetoothRoute = route2;
                        Log.i("MediaRouter", "Found bluetooth route: " + this.mBluetoothRoute);
                        break;
                    }
                }
            }
            if (this.mSelectedRoute == null || !this.mSelectedRoute.isSelectable()) {
                Log.i("MediaRouter", "Unselecting the current route because it is no longer selectable: " + this.mSelectedRoute);
                setSelectedRouteInternal(chooseFallbackRoute(), 0);
                return;
            }
            if (selectedRouteDescriptorChanged) {
                if (this.mSelectedRoute instanceof RouteGroup) {
                    List<RouteInfo> routes = ((RouteGroup) this.mSelectedRoute).getRoutes();
                    Set<String> idSet = new HashSet<>();
                    Iterator<RouteInfo> it3 = routes.iterator();
                    while (it3.hasNext()) {
                        idSet.add(it3.next().mDescriptorId);
                    }
                    Iterator<Map.Entry<String, MediaRouteProvider.RouteController>> iter = this.mRouteControllerMap.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<String, MediaRouteProvider.RouteController> entry = iter.next();
                        if (!idSet.contains(entry.getKey())) {
                            MediaRouteProvider.RouteController controller = entry.getValue();
                            controller.onUnselect();
                            controller.onRelease();
                            iter.remove();
                        }
                    }
                    for (RouteInfo route3 : routes) {
                        if (!this.mRouteControllerMap.containsKey(route3.mDescriptorId)) {
                            MediaRouteProvider.RouteController controller2 = route3.getProviderInstance().onCreateRouteController(route3.mDescriptorId, this.mSelectedRoute.mDescriptorId);
                            controller2.onSelect();
                            this.mRouteControllerMap.put(route3.mDescriptorId, controller2);
                        }
                    }
                }
                updatePlaybackInfoFromSelectedRoute();
            }
        }

        RouteInfo chooseFallbackRoute() {
            for (RouteInfo route : this.mRoutes) {
                if (route != this.mDefaultRoute && isSystemLiveAudioOnlyRoute(route) && route.isSelectable()) {
                    return route;
                }
            }
            return this.mDefaultRoute;
        }

        private boolean isSystemLiveAudioOnlyRoute(RouteInfo route) {
            return route.getProviderInstance() == this.mSystemProvider && route.supportsControlCategory("android.media.intent.category.LIVE_AUDIO") && !route.supportsControlCategory("android.media.intent.category.LIVE_VIDEO");
        }

        private boolean isSystemDefaultRoute(RouteInfo route) {
            return route.getProviderInstance() == this.mSystemProvider && route.mDescriptorId.equals("DEFAULT_ROUTE");
        }

        private void setSelectedRouteInternal(RouteInfo route, int unselectReason) {
            if (MediaRouter.sGlobal == null || (this.mBluetoothRoute != null && route.isDefault())) {
                StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < callStack.length; i++) {
                    StackTraceElement caller = callStack[i];
                    sb.append(caller.getClassName());
                    sb.append(".");
                    sb.append(caller.getMethodName());
                    sb.append(":");
                    sb.append(caller.getLineNumber());
                    sb.append("  ");
                }
                if (MediaRouter.sGlobal == null) {
                    Log.w("MediaRouter", "setSelectedRouteInternal is called while sGlobal is null: pkgName=" + this.mApplicationContext.getPackageName() + ", callers=" + sb.toString());
                } else {
                    Log.w("MediaRouter", "Default route is selected while a BT route is available: pkgName=" + this.mApplicationContext.getPackageName() + ", callers=" + sb.toString());
                }
            }
            if (this.mSelectedRoute != route) {
                if (this.mSelectedRoute != null) {
                    if (MediaRouter.DEBUG) {
                        Log.d("MediaRouter", "Route unselected: " + this.mSelectedRoute + " reason: " + unselectReason);
                    }
                    this.mCallbackHandler.post(263, this.mSelectedRoute, unselectReason);
                    if (this.mSelectedRouteController != null) {
                        this.mSelectedRouteController.onUnselect(unselectReason);
                        this.mSelectedRouteController.onRelease();
                        this.mSelectedRouteController = null;
                    }
                    if (!this.mRouteControllerMap.isEmpty()) {
                        for (MediaRouteProvider.RouteController controller : this.mRouteControllerMap.values()) {
                            controller.onUnselect(unselectReason);
                            controller.onRelease();
                        }
                        this.mRouteControllerMap.clear();
                    }
                }
                this.mSelectedRoute = route;
                this.mSelectedRouteController = route.getProviderInstance().onCreateRouteController(route.mDescriptorId);
                if (this.mSelectedRouteController != null) {
                    this.mSelectedRouteController.onSelect();
                }
                if (MediaRouter.DEBUG) {
                    Log.d("MediaRouter", "Route selected: " + this.mSelectedRoute);
                }
                this.mCallbackHandler.post(262, this.mSelectedRoute);
                if (this.mSelectedRoute instanceof RouteGroup) {
                    List<RouteInfo> routes = ((RouteGroup) this.mSelectedRoute).getRoutes();
                    this.mRouteControllerMap.clear();
                    for (RouteInfo r : routes) {
                        MediaRouteProvider.RouteController controller2 = r.getProviderInstance().onCreateRouteController(r.mDescriptorId, this.mSelectedRoute.mDescriptorId);
                        controller2.onSelect();
                        this.mRouteControllerMap.put(r.mDescriptorId, controller2);
                    }
                }
                updatePlaybackInfoFromSelectedRoute();
            }
        }

        @Override
        public void onSystemRouteSelectedByDescriptorId(String id) {
            ProviderInfo provider;
            int routeIndex;
            this.mCallbackHandler.removeMessages(262);
            int providerIndex = findProviderInfo(this.mSystemProvider);
            if (providerIndex >= 0 && (routeIndex = (provider = this.mProviders.get(providerIndex)).findRouteByDescriptorId(id)) >= 0) {
                ((RouteInfo) provider.mRoutes.get(routeIndex)).select();
            }
        }

        public MediaSessionCompat.Token getMediaSessionToken() {
            if (this.mMediaSession != null) {
                return this.mMediaSession.getToken();
            }
            if (this.mCompatSession != null) {
                return this.mCompatSession.getSessionToken();
            }
            return null;
        }

        private void updatePlaybackInfoFromSelectedRoute() {
            if (this.mSelectedRoute != null) {
                this.mPlaybackInfo.volume = this.mSelectedRoute.getVolume();
                this.mPlaybackInfo.volumeMax = this.mSelectedRoute.getVolumeMax();
                this.mPlaybackInfo.volumeHandling = this.mSelectedRoute.getVolumeHandling();
                this.mPlaybackInfo.playbackStream = this.mSelectedRoute.getPlaybackStream();
                this.mPlaybackInfo.playbackType = this.mSelectedRoute.getPlaybackType();
                int count = this.mRemoteControlClients.size();
                for (int i = 0; i < count; i++) {
                    RemoteControlClientRecord record = this.mRemoteControlClients.get(i);
                    record.updatePlaybackInfo();
                }
                if (this.mMediaSession != null) {
                    if (this.mSelectedRoute == getDefaultRoute() || this.mSelectedRoute == getBluetoothRoute()) {
                        this.mMediaSession.clearVolumeHandling();
                        return;
                    }
                    int controlType = 0;
                    if (this.mPlaybackInfo.volumeHandling == 1) {
                        controlType = 2;
                    }
                    this.mMediaSession.configureVolume(controlType, this.mPlaybackInfo.volumeMax, this.mPlaybackInfo.volume);
                    return;
                }
                return;
            }
            if (this.mMediaSession != null) {
                this.mMediaSession.clearVolumeHandling();
            }
        }

        private final class ProviderCallback extends MediaRouteProvider.Callback {
            ProviderCallback() {
            }

            @Override
            public void onDescriptorChanged(MediaRouteProvider provider, MediaRouteProviderDescriptor descriptor) {
                GlobalMediaRouter.this.updateProviderDescriptor(provider, descriptor);
            }
        }

        private final class MediaSessionRecord {
            private int mControlType;
            private int mMaxVolume;
            private final MediaSessionCompat mMsCompat;
            private VolumeProviderCompat mVpCompat;
            final GlobalMediaRouter this$0;

            public void configureVolume(int controlType, int max, int current) {
                if (this.mVpCompat != null && controlType == this.mControlType && max == this.mMaxVolume) {
                    this.mVpCompat.setCurrentVolume(current);
                } else {
                    this.mVpCompat = new VolumeProviderCompat(controlType, max, current) {
                        @Override
                        public void onSetVolumeTo(final int volume) {
                            MediaSessionRecord.this.this$0.mCallbackHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (MediaSessionRecord.this.this$0.mSelectedRoute != null) {
                                        MediaSessionRecord.this.this$0.mSelectedRoute.requestSetVolume(volume);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onAdjustVolume(final int direction) {
                            MediaSessionRecord.this.this$0.mCallbackHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (MediaSessionRecord.this.this$0.mSelectedRoute != null) {
                                        MediaSessionRecord.this.this$0.mSelectedRoute.requestUpdateVolume(direction);
                                    }
                                }
                            });
                        }
                    };
                    this.mMsCompat.setPlaybackToRemote(this.mVpCompat);
                }
            }

            public void clearVolumeHandling() {
                this.mMsCompat.setPlaybackToLocal(this.this$0.mPlaybackInfo.playbackStream);
                this.mVpCompat = null;
            }

            public MediaSessionCompat.Token getToken() {
                return this.mMsCompat.getSessionToken();
            }
        }

        private final class RemoteControlClientRecord {
            private final RemoteControlClientCompat mRccCompat;
            final GlobalMediaRouter this$0;

            public void updatePlaybackInfo() {
                this.mRccCompat.setPlaybackInfo(this.this$0.mPlaybackInfo);
            }
        }

        private final class CallbackHandler extends Handler {
            private final ArrayList<CallbackRecord> mTempCallbackRecords = new ArrayList<>();

            CallbackHandler() {
            }

            public void post(int msg, Object obj) {
                obtainMessage(msg, obj).sendToTarget();
            }

            public void post(int msg, Object obj, int arg) {
                Message message = obtainMessage(msg, obj);
                message.arg1 = arg;
                message.sendToTarget();
            }

            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                Object obj = msg.obj;
                int arg = msg.arg1;
                if (what == 259 && GlobalMediaRouter.this.getSelectedRoute().getId().equals(((RouteInfo) obj).getId())) {
                    GlobalMediaRouter.this.updateSelectedRouteIfNeeded(true);
                }
                syncWithSystemProvider(what, obj);
                try {
                    int i = GlobalMediaRouter.this.mRouters.size();
                    while (true) {
                        i--;
                        if (i < 0) {
                            break;
                        }
                        MediaRouter router = GlobalMediaRouter.this.mRouters.get(i).get();
                        if (router == null) {
                            GlobalMediaRouter.this.mRouters.remove(i);
                        } else {
                            this.mTempCallbackRecords.addAll(router.mCallbackRecords);
                        }
                    }
                    int callbackCount = this.mTempCallbackRecords.size();
                    for (int i2 = 0; i2 < callbackCount; i2++) {
                        invokeCallback(this.mTempCallbackRecords.get(i2), what, obj, arg);
                    }
                } finally {
                    this.mTempCallbackRecords.clear();
                }
            }

            private void syncWithSystemProvider(int what, Object obj) {
                if (what != 262) {
                    switch (what) {
                        case 257:
                            GlobalMediaRouter.this.mSystemProvider.onSyncRouteAdded((RouteInfo) obj);
                            break;
                        case 258:
                            GlobalMediaRouter.this.mSystemProvider.onSyncRouteRemoved((RouteInfo) obj);
                            break;
                        case 259:
                            GlobalMediaRouter.this.mSystemProvider.onSyncRouteChanged((RouteInfo) obj);
                            break;
                    }
                }
                GlobalMediaRouter.this.mSystemProvider.onSyncRouteSelected((RouteInfo) obj);
            }

            private void invokeCallback(CallbackRecord record, int what, Object obj, int arg) {
                MediaRouter router = record.mRouter;
                Callback callback = record.mCallback;
                int i = 65280 & what;
                if (i != 256) {
                    if (i == 512) {
                        ProviderInfo provider = (ProviderInfo) obj;
                        switch (what) {
                            case 513:
                                callback.onProviderAdded(router, provider);
                                break;
                            case 514:
                                callback.onProviderRemoved(router, provider);
                                break;
                            case 515:
                                callback.onProviderChanged(router, provider);
                                break;
                        }
                    }
                    return;
                }
                RouteInfo route = (RouteInfo) obj;
                if (record.filterRouteEvent(route)) {
                    switch (what) {
                        case 257:
                            callback.onRouteAdded(router, route);
                            break;
                        case 258:
                            callback.onRouteRemoved(router, route);
                            break;
                        case 259:
                            callback.onRouteChanged(router, route);
                            break;
                        case 260:
                            callback.onRouteVolumeChanged(router, route);
                            break;
                        case 261:
                            callback.onRoutePresentationDisplayChanged(router, route);
                            break;
                        case 262:
                            callback.onRouteSelected(router, route);
                            break;
                        case 263:
                            callback.onRouteUnselected(router, route, arg);
                            break;
                    }
                }
            }
        }
    }
}
