package android.support.v7.media;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Build;
import android.support.v7.media.MediaRouteDescriptor;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteProviderDescriptor;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouterApi24;
import android.support.v7.media.MediaRouterJellybean;
import android.support.v7.media.MediaRouterJellybeanMr1;
import android.support.v7.media.MediaRouterJellybeanMr2;
import android.support.v7.mediarouter.R;
import android.view.Display;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class SystemMediaRouteProvider extends MediaRouteProvider {

    /* loaded from: classes.dex */
    public interface SyncCallback {
        void onSystemRouteSelectedByDescriptorId(String str);
    }

    protected SystemMediaRouteProvider(Context context) {
        super(context, new MediaRouteProvider.ProviderMetadata(new ComponentName("android", SystemMediaRouteProvider.class.getName())));
    }

    public static SystemMediaRouteProvider obtain(Context context, SyncCallback syncCallback) {
        if (Build.VERSION.SDK_INT >= 24) {
            return new Api24Impl(context, syncCallback);
        }
        if (Build.VERSION.SDK_INT >= 18) {
            return new JellybeanMr2Impl(context, syncCallback);
        }
        if (Build.VERSION.SDK_INT >= 17) {
            return new JellybeanMr1Impl(context, syncCallback);
        }
        if (Build.VERSION.SDK_INT >= 16) {
            return new JellybeanImpl(context, syncCallback);
        }
        return new LegacyImpl(context);
    }

    public void onSyncRouteAdded(MediaRouter.RouteInfo route) {
    }

    public void onSyncRouteRemoved(MediaRouter.RouteInfo route) {
    }

    public void onSyncRouteChanged(MediaRouter.RouteInfo route) {
    }

    public void onSyncRouteSelected(MediaRouter.RouteInfo route) {
    }

    protected Object getDefaultRoute() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class LegacyImpl extends SystemMediaRouteProvider {
        private static final ArrayList<IntentFilter> CONTROL_FILTERS;
        final AudioManager mAudioManager;
        int mLastReportedVolume;
        private final VolumeChangeReceiver mVolumeChangeReceiver;

        static {
            IntentFilter f = new IntentFilter();
            f.addCategory("android.media.intent.category.LIVE_AUDIO");
            f.addCategory("android.media.intent.category.LIVE_VIDEO");
            CONTROL_FILTERS = new ArrayList<>();
            CONTROL_FILTERS.add(f);
        }

        public LegacyImpl(Context context) {
            super(context);
            this.mLastReportedVolume = -1;
            this.mAudioManager = (AudioManager) context.getSystemService("audio");
            this.mVolumeChangeReceiver = new VolumeChangeReceiver();
            context.registerReceiver(this.mVolumeChangeReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
            publishRoutes();
        }

        void publishRoutes() {
            Resources r = getContext().getResources();
            int maxVolume = this.mAudioManager.getStreamMaxVolume(3);
            this.mLastReportedVolume = this.mAudioManager.getStreamVolume(3);
            MediaRouteDescriptor defaultRoute = new MediaRouteDescriptor.Builder("DEFAULT_ROUTE", r.getString(R.string.mr_system_route_name)).addControlFilters(CONTROL_FILTERS).setPlaybackStream(3).setPlaybackType(0).setVolumeHandling(1).setVolumeMax(maxVolume).setVolume(this.mLastReportedVolume).build();
            MediaRouteProviderDescriptor providerDescriptor = new MediaRouteProviderDescriptor.Builder().addRoute(defaultRoute).build();
            setDescriptor(providerDescriptor);
        }

        @Override // android.support.v7.media.MediaRouteProvider
        public MediaRouteProvider.RouteController onCreateRouteController(String routeId) {
            if (routeId.equals("DEFAULT_ROUTE")) {
                return new DefaultRouteController();
            }
            return null;
        }

        /* loaded from: classes.dex */
        final class DefaultRouteController extends MediaRouteProvider.RouteController {
            DefaultRouteController() {
            }

            @Override // android.support.v7.media.MediaRouteProvider.RouteController
            public void onSetVolume(int volume) {
                LegacyImpl.this.mAudioManager.setStreamVolume(3, volume, 0);
                LegacyImpl.this.publishRoutes();
            }

            @Override // android.support.v7.media.MediaRouteProvider.RouteController
            public void onUpdateVolume(int delta) {
                int volume = LegacyImpl.this.mAudioManager.getStreamVolume(3);
                int maxVolume = LegacyImpl.this.mAudioManager.getStreamMaxVolume(3);
                int newVolume = Math.min(maxVolume, Math.max(0, volume + delta));
                if (newVolume != volume) {
                    LegacyImpl.this.mAudioManager.setStreamVolume(3, volume, 0);
                }
                LegacyImpl.this.publishRoutes();
            }
        }

        /* loaded from: classes.dex */
        final class VolumeChangeReceiver extends BroadcastReceiver {
            VolumeChangeReceiver() {
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int volume;
                if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
                    int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                    if (streamType == 3 && (volume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)) >= 0 && volume != LegacyImpl.this.mLastReportedVolume) {
                        LegacyImpl.this.publishRoutes();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class JellybeanImpl extends SystemMediaRouteProvider implements MediaRouterJellybean.Callback, MediaRouterJellybean.VolumeCallback {
        private static final ArrayList<IntentFilter> LIVE_AUDIO_CONTROL_FILTERS;
        private static final ArrayList<IntentFilter> LIVE_VIDEO_CONTROL_FILTERS;
        protected boolean mActiveScan;
        protected final Object mCallbackObj;
        protected boolean mCallbackRegistered;
        private MediaRouterJellybean.GetDefaultRouteWorkaround mGetDefaultRouteWorkaround;
        protected int mRouteTypes;
        protected final Object mRouterObj;
        private MediaRouterJellybean.SelectRouteWorkaround mSelectRouteWorkaround;
        private final SyncCallback mSyncCallback;
        protected final ArrayList<SystemRouteRecord> mSystemRouteRecords;
        protected final Object mUserRouteCategoryObj;
        protected final ArrayList<UserRouteRecord> mUserRouteRecords;
        protected final Object mVolumeCallbackObj;

        static {
            IntentFilter f = new IntentFilter();
            f.addCategory("android.media.intent.category.LIVE_AUDIO");
            LIVE_AUDIO_CONTROL_FILTERS = new ArrayList<>();
            LIVE_AUDIO_CONTROL_FILTERS.add(f);
            IntentFilter f2 = new IntentFilter();
            f2.addCategory("android.media.intent.category.LIVE_VIDEO");
            LIVE_VIDEO_CONTROL_FILTERS = new ArrayList<>();
            LIVE_VIDEO_CONTROL_FILTERS.add(f2);
        }

        public JellybeanImpl(Context context, SyncCallback syncCallback) {
            super(context);
            this.mSystemRouteRecords = new ArrayList<>();
            this.mUserRouteRecords = new ArrayList<>();
            this.mSyncCallback = syncCallback;
            this.mRouterObj = MediaRouterJellybean.getMediaRouter(context);
            this.mCallbackObj = createCallbackObj();
            this.mVolumeCallbackObj = createVolumeCallbackObj();
            Resources r = context.getResources();
            this.mUserRouteCategoryObj = MediaRouterJellybean.createRouteCategory(this.mRouterObj, r.getString(R.string.mr_user_route_category_name), false);
            updateSystemRoutes();
        }

        @Override // android.support.v7.media.MediaRouteProvider
        public MediaRouteProvider.RouteController onCreateRouteController(String routeId) {
            int index = findSystemRouteRecordByDescriptorId(routeId);
            if (index >= 0) {
                SystemRouteRecord record = this.mSystemRouteRecords.get(index);
                return new SystemRouteController(record.mRouteObj);
            }
            return null;
        }

        @Override // android.support.v7.media.MediaRouteProvider
        public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
            int newRouteTypes = 0;
            boolean newActiveScan = false;
            if (request != null) {
                MediaRouteSelector selector = request.getSelector();
                List<String> categories = selector.getControlCategories();
                int count = categories.size();
                for (int i = 0; i < count; i++) {
                    String category = categories.get(i);
                    if (category.equals("android.media.intent.category.LIVE_AUDIO")) {
                        newRouteTypes |= 1;
                    } else if (category.equals("android.media.intent.category.LIVE_VIDEO")) {
                        newRouteTypes |= 2;
                    } else {
                        newRouteTypes |= 8388608;
                    }
                }
                newActiveScan = request.isActiveScan();
            }
            if (this.mRouteTypes != newRouteTypes || this.mActiveScan != newActiveScan) {
                this.mRouteTypes = newRouteTypes;
                this.mActiveScan = newActiveScan;
                updateSystemRoutes();
            }
        }

        @Override // android.support.v7.media.MediaRouterJellybean.Callback
        public void onRouteAdded(Object routeObj) {
            if (addSystemRouteNoPublish(routeObj)) {
                publishRoutes();
            }
        }

        private void updateSystemRoutes() {
            updateCallback();
            boolean changed = false;
            for (Object routeObj : MediaRouterJellybean.getRoutes(this.mRouterObj)) {
                changed |= addSystemRouteNoPublish(routeObj);
            }
            if (changed) {
                publishRoutes();
            }
        }

        private boolean addSystemRouteNoPublish(Object routeObj) {
            if (getUserRouteRecord(routeObj) == null && findSystemRouteRecord(routeObj) < 0) {
                String id = assignRouteId(routeObj);
                SystemRouteRecord record = new SystemRouteRecord(routeObj, id);
                updateSystemRouteDescriptor(record);
                this.mSystemRouteRecords.add(record);
                return true;
            }
            return false;
        }

        private String assignRouteId(Object routeObj) {
            boolean isDefault = getDefaultRoute() == routeObj;
            String id = isDefault ? "DEFAULT_ROUTE" : String.format(Locale.US, "ROUTE_%08x", Integer.valueOf(getRouteName(routeObj).hashCode()));
            if (findSystemRouteRecordByDescriptorId(id) < 0) {
                return id;
            }
            int i = 2;
            while (true) {
                String newId = String.format(Locale.US, "%s_%d", id, Integer.valueOf(i));
                if (findSystemRouteRecordByDescriptorId(newId) >= 0) {
                    i++;
                } else {
                    return newId;
                }
            }
        }

        @Override // android.support.v7.media.MediaRouterJellybean.Callback
        public void onRouteRemoved(Object routeObj) {
            int index;
            if (getUserRouteRecord(routeObj) == null && (index = findSystemRouteRecord(routeObj)) >= 0) {
                this.mSystemRouteRecords.remove(index);
                publishRoutes();
            }
        }

        @Override // android.support.v7.media.MediaRouterJellybean.Callback
        public void onRouteChanged(Object routeObj) {
            int index;
            if (getUserRouteRecord(routeObj) == null && (index = findSystemRouteRecord(routeObj)) >= 0) {
                SystemRouteRecord record = this.mSystemRouteRecords.get(index);
                updateSystemRouteDescriptor(record);
                publishRoutes();
            }
        }

        @Override // android.support.v7.media.MediaRouterJellybean.Callback
        public void onRouteVolumeChanged(Object routeObj) {
            int index;
            if (getUserRouteRecord(routeObj) == null && (index = findSystemRouteRecord(routeObj)) >= 0) {
                SystemRouteRecord record = this.mSystemRouteRecords.get(index);
                int newVolume = MediaRouterJellybean.RouteInfo.getVolume(routeObj);
                if (newVolume != record.mRouteDescriptor.getVolume()) {
                    record.mRouteDescriptor = new MediaRouteDescriptor.Builder(record.mRouteDescriptor).setVolume(newVolume).build();
                    publishRoutes();
                }
            }
        }

        @Override // android.support.v7.media.MediaRouterJellybean.Callback
        public void onRouteSelected(int type, Object routeObj) {
            if (routeObj != MediaRouterJellybean.getSelectedRoute(this.mRouterObj, 8388611)) {
                return;
            }
            UserRouteRecord userRouteRecord = getUserRouteRecord(routeObj);
            if (userRouteRecord != null) {
                userRouteRecord.mRoute.select();
                return;
            }
            int index = findSystemRouteRecord(routeObj);
            if (index >= 0) {
                SystemRouteRecord record = this.mSystemRouteRecords.get(index);
                this.mSyncCallback.onSystemRouteSelectedByDescriptorId(record.mRouteDescriptorId);
            }
        }

        @Override // android.support.v7.media.MediaRouterJellybean.Callback
        public void onRouteUnselected(int type, Object routeObj) {
        }

        @Override // android.support.v7.media.MediaRouterJellybean.Callback
        public void onRouteGrouped(Object routeObj, Object groupObj, int index) {
        }

        @Override // android.support.v7.media.MediaRouterJellybean.Callback
        public void onRouteUngrouped(Object routeObj, Object groupObj) {
        }

        @Override // android.support.v7.media.MediaRouterJellybean.VolumeCallback
        public void onVolumeSetRequest(Object routeObj, int volume) {
            UserRouteRecord record = getUserRouteRecord(routeObj);
            if (record != null) {
                record.mRoute.requestSetVolume(volume);
            }
        }

        @Override // android.support.v7.media.MediaRouterJellybean.VolumeCallback
        public void onVolumeUpdateRequest(Object routeObj, int direction) {
            UserRouteRecord record = getUserRouteRecord(routeObj);
            if (record != null) {
                record.mRoute.requestUpdateVolume(direction);
            }
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider
        public void onSyncRouteAdded(MediaRouter.RouteInfo route) {
            if (route.getProviderInstance() != this) {
                Object routeObj = MediaRouterJellybean.createUserRoute(this.mRouterObj, this.mUserRouteCategoryObj);
                UserRouteRecord record = new UserRouteRecord(route, routeObj);
                MediaRouterJellybean.RouteInfo.setTag(routeObj, record);
                MediaRouterJellybean.UserRouteInfo.setVolumeCallback(routeObj, this.mVolumeCallbackObj);
                updateUserRouteProperties(record);
                this.mUserRouteRecords.add(record);
                MediaRouterJellybean.addUserRoute(this.mRouterObj, routeObj);
                return;
            }
            int index = findSystemRouteRecord(MediaRouterJellybean.getSelectedRoute(this.mRouterObj, 8388611));
            if (index >= 0 && this.mSystemRouteRecords.get(index).mRouteDescriptorId.equals(route.getDescriptorId())) {
                route.select();
            }
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider
        public void onSyncRouteRemoved(MediaRouter.RouteInfo route) {
            int index;
            if (route.getProviderInstance() != this && (index = findUserRouteRecord(route)) >= 0) {
                UserRouteRecord record = this.mUserRouteRecords.remove(index);
                MediaRouterJellybean.RouteInfo.setTag(record.mRouteObj, null);
                MediaRouterJellybean.UserRouteInfo.setVolumeCallback(record.mRouteObj, null);
                MediaRouterJellybean.removeUserRoute(this.mRouterObj, record.mRouteObj);
            }
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider
        public void onSyncRouteChanged(MediaRouter.RouteInfo route) {
            int index;
            if (route.getProviderInstance() != this && (index = findUserRouteRecord(route)) >= 0) {
                UserRouteRecord record = this.mUserRouteRecords.get(index);
                updateUserRouteProperties(record);
            }
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider
        public void onSyncRouteSelected(MediaRouter.RouteInfo route) {
            if (!route.isSelected()) {
                return;
            }
            if (route.getProviderInstance() != this) {
                int index = findUserRouteRecord(route);
                if (index >= 0) {
                    UserRouteRecord record = this.mUserRouteRecords.get(index);
                    selectRoute(record.mRouteObj);
                    return;
                }
                return;
            }
            int index2 = findSystemRouteRecordByDescriptorId(route.getDescriptorId());
            if (index2 >= 0) {
                SystemRouteRecord record2 = this.mSystemRouteRecords.get(index2);
                selectRoute(record2.mRouteObj);
            }
        }

        protected void publishRoutes() {
            MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
            int count = this.mSystemRouteRecords.size();
            for (int i = 0; i < count; i++) {
                builder.addRoute(this.mSystemRouteRecords.get(i).mRouteDescriptor);
            }
            setDescriptor(builder.build());
        }

        protected int findSystemRouteRecord(Object routeObj) {
            int count = this.mSystemRouteRecords.size();
            for (int i = 0; i < count; i++) {
                if (this.mSystemRouteRecords.get(i).mRouteObj == routeObj) {
                    return i;
                }
            }
            return -1;
        }

        protected int findSystemRouteRecordByDescriptorId(String id) {
            int count = this.mSystemRouteRecords.size();
            for (int i = 0; i < count; i++) {
                if (this.mSystemRouteRecords.get(i).mRouteDescriptorId.equals(id)) {
                    return i;
                }
            }
            return -1;
        }

        protected int findUserRouteRecord(MediaRouter.RouteInfo route) {
            int count = this.mUserRouteRecords.size();
            for (int i = 0; i < count; i++) {
                if (this.mUserRouteRecords.get(i).mRoute == route) {
                    return i;
                }
            }
            return -1;
        }

        protected UserRouteRecord getUserRouteRecord(Object routeObj) {
            Object tag = MediaRouterJellybean.RouteInfo.getTag(routeObj);
            if (tag instanceof UserRouteRecord) {
                return (UserRouteRecord) tag;
            }
            return null;
        }

        protected void updateSystemRouteDescriptor(SystemRouteRecord record) {
            MediaRouteDescriptor.Builder builder = new MediaRouteDescriptor.Builder(record.mRouteDescriptorId, getRouteName(record.mRouteObj));
            onBuildSystemRouteDescriptor(record, builder);
            record.mRouteDescriptor = builder.build();
        }

        protected String getRouteName(Object routeObj) {
            CharSequence name = MediaRouterJellybean.RouteInfo.getName(routeObj, getContext());
            return name != null ? name.toString() : "";
        }

        protected void onBuildSystemRouteDescriptor(SystemRouteRecord record, MediaRouteDescriptor.Builder builder) {
            int supportedTypes = MediaRouterJellybean.RouteInfo.getSupportedTypes(record.mRouteObj);
            if ((supportedTypes & 1) != 0) {
                builder.addControlFilters(LIVE_AUDIO_CONTROL_FILTERS);
            }
            if ((supportedTypes & 2) != 0) {
                builder.addControlFilters(LIVE_VIDEO_CONTROL_FILTERS);
            }
            builder.setPlaybackType(MediaRouterJellybean.RouteInfo.getPlaybackType(record.mRouteObj));
            builder.setPlaybackStream(MediaRouterJellybean.RouteInfo.getPlaybackStream(record.mRouteObj));
            builder.setVolume(MediaRouterJellybean.RouteInfo.getVolume(record.mRouteObj));
            builder.setVolumeMax(MediaRouterJellybean.RouteInfo.getVolumeMax(record.mRouteObj));
            builder.setVolumeHandling(MediaRouterJellybean.RouteInfo.getVolumeHandling(record.mRouteObj));
        }

        protected void updateUserRouteProperties(UserRouteRecord record) {
            MediaRouterJellybean.UserRouteInfo.setName(record.mRouteObj, record.mRoute.getName());
            MediaRouterJellybean.UserRouteInfo.setPlaybackType(record.mRouteObj, record.mRoute.getPlaybackType());
            MediaRouterJellybean.UserRouteInfo.setPlaybackStream(record.mRouteObj, record.mRoute.getPlaybackStream());
            MediaRouterJellybean.UserRouteInfo.setVolume(record.mRouteObj, record.mRoute.getVolume());
            MediaRouterJellybean.UserRouteInfo.setVolumeMax(record.mRouteObj, record.mRoute.getVolumeMax());
            MediaRouterJellybean.UserRouteInfo.setVolumeHandling(record.mRouteObj, record.mRoute.getVolumeHandling());
        }

        protected void updateCallback() {
            if (this.mCallbackRegistered) {
                this.mCallbackRegistered = false;
                MediaRouterJellybean.removeCallback(this.mRouterObj, this.mCallbackObj);
            }
            if (this.mRouteTypes != 0) {
                this.mCallbackRegistered = true;
                MediaRouterJellybean.addCallback(this.mRouterObj, this.mRouteTypes, this.mCallbackObj);
            }
        }

        protected Object createCallbackObj() {
            return MediaRouterJellybean.createCallback(this);
        }

        protected Object createVolumeCallbackObj() {
            return MediaRouterJellybean.createVolumeCallback(this);
        }

        protected void selectRoute(Object routeObj) {
            if (this.mSelectRouteWorkaround == null) {
                this.mSelectRouteWorkaround = new MediaRouterJellybean.SelectRouteWorkaround();
            }
            this.mSelectRouteWorkaround.selectRoute(this.mRouterObj, 8388611, routeObj);
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider
        protected Object getDefaultRoute() {
            if (this.mGetDefaultRouteWorkaround == null) {
                this.mGetDefaultRouteWorkaround = new MediaRouterJellybean.GetDefaultRouteWorkaround();
            }
            return this.mGetDefaultRouteWorkaround.getDefaultRoute(this.mRouterObj);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        /* loaded from: classes.dex */
        public static final class SystemRouteRecord {
            public MediaRouteDescriptor mRouteDescriptor;
            public final String mRouteDescriptorId;
            public final Object mRouteObj;

            public SystemRouteRecord(Object routeObj, String id) {
                this.mRouteObj = routeObj;
                this.mRouteDescriptorId = id;
            }
        }

        /* JADX INFO: Access modifiers changed from: protected */
        /* loaded from: classes.dex */
        public static final class UserRouteRecord {
            public final MediaRouter.RouteInfo mRoute;
            public final Object mRouteObj;

            public UserRouteRecord(MediaRouter.RouteInfo route, Object routeObj) {
                this.mRoute = route;
                this.mRouteObj = routeObj;
            }
        }

        /* loaded from: classes.dex */
        protected static final class SystemRouteController extends MediaRouteProvider.RouteController {
            private final Object mRouteObj;

            public SystemRouteController(Object routeObj) {
                this.mRouteObj = routeObj;
            }

            @Override // android.support.v7.media.MediaRouteProvider.RouteController
            public void onSetVolume(int volume) {
                MediaRouterJellybean.RouteInfo.requestSetVolume(this.mRouteObj, volume);
            }

            @Override // android.support.v7.media.MediaRouteProvider.RouteController
            public void onUpdateVolume(int delta) {
                MediaRouterJellybean.RouteInfo.requestUpdateVolume(this.mRouteObj, delta);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class JellybeanMr1Impl extends JellybeanImpl implements MediaRouterJellybeanMr1.Callback {
        private MediaRouterJellybeanMr1.ActiveScanWorkaround mActiveScanWorkaround;
        private MediaRouterJellybeanMr1.IsConnectingWorkaround mIsConnectingWorkaround;

        public JellybeanMr1Impl(Context context, SyncCallback syncCallback) {
            super(context, syncCallback);
        }

        @Override // android.support.v7.media.MediaRouterJellybeanMr1.Callback
        public void onRoutePresentationDisplayChanged(Object routeObj) {
            int index = findSystemRouteRecord(routeObj);
            if (index >= 0) {
                JellybeanImpl.SystemRouteRecord record = this.mSystemRouteRecords.get(index);
                Display newPresentationDisplay = MediaRouterJellybeanMr1.RouteInfo.getPresentationDisplay(routeObj);
                int newPresentationDisplayId = newPresentationDisplay != null ? newPresentationDisplay.getDisplayId() : -1;
                if (newPresentationDisplayId != record.mRouteDescriptor.getPresentationDisplayId()) {
                    record.mRouteDescriptor = new MediaRouteDescriptor.Builder(record.mRouteDescriptor).setPresentationDisplayId(newPresentationDisplayId).build();
                    publishRoutes();
                }
            }
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider.JellybeanImpl
        protected void onBuildSystemRouteDescriptor(JellybeanImpl.SystemRouteRecord record, MediaRouteDescriptor.Builder builder) {
            super.onBuildSystemRouteDescriptor(record, builder);
            if (!MediaRouterJellybeanMr1.RouteInfo.isEnabled(record.mRouteObj)) {
                builder.setEnabled(false);
            }
            if (isConnecting(record)) {
                builder.setConnecting(true);
            }
            Display presentationDisplay = MediaRouterJellybeanMr1.RouteInfo.getPresentationDisplay(record.mRouteObj);
            if (presentationDisplay != null) {
                builder.setPresentationDisplayId(presentationDisplay.getDisplayId());
            }
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider.JellybeanImpl
        protected void updateCallback() {
            super.updateCallback();
            if (this.mActiveScanWorkaround == null) {
                this.mActiveScanWorkaround = new MediaRouterJellybeanMr1.ActiveScanWorkaround(getContext(), getHandler());
            }
            this.mActiveScanWorkaround.setActiveScanRouteTypes(this.mActiveScan ? this.mRouteTypes : 0);
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider.JellybeanImpl
        protected Object createCallbackObj() {
            return MediaRouterJellybeanMr1.createCallback(this);
        }

        protected boolean isConnecting(JellybeanImpl.SystemRouteRecord record) {
            if (this.mIsConnectingWorkaround == null) {
                this.mIsConnectingWorkaround = new MediaRouterJellybeanMr1.IsConnectingWorkaround();
            }
            return this.mIsConnectingWorkaround.isConnecting(record.mRouteObj);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class JellybeanMr2Impl extends JellybeanMr1Impl {
        public JellybeanMr2Impl(Context context, SyncCallback syncCallback) {
            super(context, syncCallback);
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider.JellybeanMr1Impl, android.support.v7.media.SystemMediaRouteProvider.JellybeanImpl
        protected void onBuildSystemRouteDescriptor(JellybeanImpl.SystemRouteRecord record, MediaRouteDescriptor.Builder builder) {
            super.onBuildSystemRouteDescriptor(record, builder);
            CharSequence description = MediaRouterJellybeanMr2.RouteInfo.getDescription(record.mRouteObj);
            if (description != null) {
                builder.setDescription(description.toString());
            }
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider.JellybeanImpl
        protected void selectRoute(Object routeObj) {
            MediaRouterJellybean.selectRoute(this.mRouterObj, 8388611, routeObj);
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider.JellybeanImpl, android.support.v7.media.SystemMediaRouteProvider
        protected Object getDefaultRoute() {
            return MediaRouterJellybeanMr2.getDefaultRoute(this.mRouterObj);
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider.JellybeanImpl
        protected void updateUserRouteProperties(JellybeanImpl.UserRouteRecord record) {
            super.updateUserRouteProperties(record);
            MediaRouterJellybeanMr2.UserRouteInfo.setDescription(record.mRouteObj, record.mRoute.getDescription());
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider.JellybeanMr1Impl, android.support.v7.media.SystemMediaRouteProvider.JellybeanImpl
        protected void updateCallback() {
            if (this.mCallbackRegistered) {
                MediaRouterJellybean.removeCallback(this.mRouterObj, this.mCallbackObj);
            }
            this.mCallbackRegistered = true;
            MediaRouterJellybeanMr2.addCallback(this.mRouterObj, this.mRouteTypes, this.mCallbackObj, 2 | (this.mActiveScan ? 1 : 0));
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider.JellybeanMr1Impl
        protected boolean isConnecting(JellybeanImpl.SystemRouteRecord record) {
            return MediaRouterJellybeanMr2.RouteInfo.isConnecting(record.mRouteObj);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Api24Impl extends JellybeanMr2Impl {
        public Api24Impl(Context context, SyncCallback syncCallback) {
            super(context, syncCallback);
        }

        @Override // android.support.v7.media.SystemMediaRouteProvider.JellybeanMr2Impl, android.support.v7.media.SystemMediaRouteProvider.JellybeanMr1Impl, android.support.v7.media.SystemMediaRouteProvider.JellybeanImpl
        protected void onBuildSystemRouteDescriptor(JellybeanImpl.SystemRouteRecord record, MediaRouteDescriptor.Builder builder) {
            super.onBuildSystemRouteDescriptor(record, builder);
            builder.setDeviceType(MediaRouterApi24.RouteInfo.getDeviceType(record.mRouteObj));
        }
    }
}
