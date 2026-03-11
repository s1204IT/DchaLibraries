package android.support.v7.media;

import android.content.Context;
import android.media.MediaRouter;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class MediaRouterJellybean {

    public interface Callback {
        void onRouteAdded(Object obj);

        void onRouteChanged(Object obj);

        void onRouteGrouped(Object obj, Object obj2, int i);

        void onRouteRemoved(Object obj);

        void onRouteSelected(int i, Object obj);

        void onRouteUngrouped(Object obj, Object obj2);

        void onRouteUnselected(int i, Object obj);

        void onRouteVolumeChanged(Object obj);
    }

    public interface VolumeCallback {
        void onVolumeSetRequest(Object obj, int i);

        void onVolumeUpdateRequest(Object obj, int i);
    }

    public static Object getMediaRouter(Context context) {
        return context.getSystemService("media_router");
    }

    public static List getRoutes(Object routerObj) {
        android.media.MediaRouter router = (android.media.MediaRouter) routerObj;
        int count = router.getRouteCount();
        List out = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            out.add(router.getRouteAt(i));
        }
        return out;
    }

    public static Object getSelectedRoute(Object routerObj, int type) {
        return ((android.media.MediaRouter) routerObj).getSelectedRoute(type);
    }

    public static void selectRoute(Object routerObj, int types, Object routeObj) {
        ((android.media.MediaRouter) routerObj).selectRoute(types, (MediaRouter.RouteInfo) routeObj);
    }

    public static void addCallback(Object routerObj, int types, Object callbackObj) {
        ((android.media.MediaRouter) routerObj).addCallback(types, (MediaRouter.Callback) callbackObj);
    }

    public static void removeCallback(Object routerObj, Object callbackObj) {
        ((android.media.MediaRouter) routerObj).removeCallback((MediaRouter.Callback) callbackObj);
    }

    public static Object createRouteCategory(Object routerObj, String name, boolean isGroupable) {
        return ((android.media.MediaRouter) routerObj).createRouteCategory(name, isGroupable);
    }

    public static Object createUserRoute(Object routerObj, Object categoryObj) {
        return ((android.media.MediaRouter) routerObj).createUserRoute((MediaRouter.RouteCategory) categoryObj);
    }

    public static void addUserRoute(Object routerObj, Object routeObj) {
        ((android.media.MediaRouter) routerObj).addUserRoute((MediaRouter.UserRouteInfo) routeObj);
    }

    public static void removeUserRoute(Object routerObj, Object routeObj) {
        ((android.media.MediaRouter) routerObj).removeUserRoute((MediaRouter.UserRouteInfo) routeObj);
    }

    public static Object createCallback(Callback callback) {
        return new CallbackProxy(callback);
    }

    public static Object createVolumeCallback(VolumeCallback callback) {
        return new VolumeCallbackProxy(callback);
    }

    public static final class RouteInfo {
        public static CharSequence getName(Object routeObj, Context context) {
            return ((MediaRouter.RouteInfo) routeObj).getName(context);
        }

        public static int getSupportedTypes(Object routeObj) {
            return ((MediaRouter.RouteInfo) routeObj).getSupportedTypes();
        }

        public static int getPlaybackType(Object routeObj) {
            return ((MediaRouter.RouteInfo) routeObj).getPlaybackType();
        }

        public static int getPlaybackStream(Object routeObj) {
            return ((MediaRouter.RouteInfo) routeObj).getPlaybackStream();
        }

        public static int getVolume(Object routeObj) {
            return ((MediaRouter.RouteInfo) routeObj).getVolume();
        }

        public static int getVolumeMax(Object routeObj) {
            return ((MediaRouter.RouteInfo) routeObj).getVolumeMax();
        }

        public static int getVolumeHandling(Object routeObj) {
            return ((MediaRouter.RouteInfo) routeObj).getVolumeHandling();
        }

        public static Object getTag(Object routeObj) {
            return ((MediaRouter.RouteInfo) routeObj).getTag();
        }

        public static void setTag(Object routeObj, Object tag) {
            ((MediaRouter.RouteInfo) routeObj).setTag(tag);
        }

        public static void requestSetVolume(Object routeObj, int volume) {
            ((MediaRouter.RouteInfo) routeObj).requestSetVolume(volume);
        }

        public static void requestUpdateVolume(Object routeObj, int direction) {
            ((MediaRouter.RouteInfo) routeObj).requestUpdateVolume(direction);
        }
    }

    public static final class UserRouteInfo {
        public static void setName(Object routeObj, CharSequence name) {
            ((MediaRouter.UserRouteInfo) routeObj).setName(name);
        }

        public static void setPlaybackType(Object routeObj, int type) {
            ((MediaRouter.UserRouteInfo) routeObj).setPlaybackType(type);
        }

        public static void setPlaybackStream(Object routeObj, int stream) {
            ((MediaRouter.UserRouteInfo) routeObj).setPlaybackStream(stream);
        }

        public static void setVolume(Object routeObj, int volume) {
            ((MediaRouter.UserRouteInfo) routeObj).setVolume(volume);
        }

        public static void setVolumeMax(Object routeObj, int volumeMax) {
            ((MediaRouter.UserRouteInfo) routeObj).setVolumeMax(volumeMax);
        }

        public static void setVolumeHandling(Object routeObj, int volumeHandling) {
            ((MediaRouter.UserRouteInfo) routeObj).setVolumeHandling(volumeHandling);
        }

        public static void setVolumeCallback(Object routeObj, Object volumeCallbackObj) {
            ((MediaRouter.UserRouteInfo) routeObj).setVolumeCallback((MediaRouter.VolumeCallback) volumeCallbackObj);
        }
    }

    public static final class SelectRouteWorkaround {
        private Method mSelectRouteIntMethod;

        public SelectRouteWorkaround() {
            if (Build.VERSION.SDK_INT < 16 || Build.VERSION.SDK_INT > 17) {
                throw new UnsupportedOperationException();
            }
            try {
                this.mSelectRouteIntMethod = android.media.MediaRouter.class.getMethod("selectRouteInt", Integer.TYPE, MediaRouter.RouteInfo.class);
            } catch (NoSuchMethodException e) {
            }
        }

        public void selectRoute(Object routerObj, int types, Object routeObj) {
            android.media.MediaRouter router = (android.media.MediaRouter) routerObj;
            MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) routeObj;
            int routeTypes = route.getSupportedTypes();
            if ((8388608 & routeTypes) == 0) {
                if (this.mSelectRouteIntMethod != null) {
                    try {
                        this.mSelectRouteIntMethod.invoke(router, Integer.valueOf(types), route);
                        return;
                    } catch (IllegalAccessException ex) {
                        Log.w("MediaRouterJellybean", "Cannot programmatically select non-user route.  Media routing may not work.", ex);
                    } catch (InvocationTargetException ex2) {
                        Log.w("MediaRouterJellybean", "Cannot programmatically select non-user route.  Media routing may not work.", ex2);
                    }
                } else {
                    Log.w("MediaRouterJellybean", "Cannot programmatically select non-user route because the platform is missing the selectRouteInt() method.  Media routing may not work.");
                }
            }
            router.selectRoute(types, route);
        }
    }

    public static final class GetDefaultRouteWorkaround {
        private Method mGetSystemAudioRouteMethod;

        public GetDefaultRouteWorkaround() {
            if (Build.VERSION.SDK_INT < 16 || Build.VERSION.SDK_INT > 17) {
                throw new UnsupportedOperationException();
            }
            try {
                this.mGetSystemAudioRouteMethod = android.media.MediaRouter.class.getMethod("getSystemAudioRoute", new Class[0]);
            } catch (NoSuchMethodException e) {
            }
        }

        public Object getDefaultRoute(Object routerObj) {
            android.media.MediaRouter router = (android.media.MediaRouter) routerObj;
            if (this.mGetSystemAudioRouteMethod != null) {
                try {
                    return this.mGetSystemAudioRouteMethod.invoke(router, new Object[0]);
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e2) {
                }
            }
            return router.getRouteAt(0);
        }
    }

    static class CallbackProxy<T extends Callback> extends MediaRouter.Callback {
        protected final T mCallback;

        public CallbackProxy(T callback) {
            this.mCallback = callback;
        }

        @Override
        public void onRouteSelected(android.media.MediaRouter router, int type, MediaRouter.RouteInfo route) {
            this.mCallback.onRouteSelected(type, route);
        }

        @Override
        public void onRouteUnselected(android.media.MediaRouter router, int type, MediaRouter.RouteInfo route) {
            this.mCallback.onRouteUnselected(type, route);
        }

        @Override
        public void onRouteAdded(android.media.MediaRouter router, MediaRouter.RouteInfo route) {
            this.mCallback.onRouteAdded(route);
        }

        @Override
        public void onRouteRemoved(android.media.MediaRouter router, MediaRouter.RouteInfo route) {
            this.mCallback.onRouteRemoved(route);
        }

        @Override
        public void onRouteChanged(android.media.MediaRouter router, MediaRouter.RouteInfo route) {
            this.mCallback.onRouteChanged(route);
        }

        @Override
        public void onRouteGrouped(android.media.MediaRouter router, MediaRouter.RouteInfo route, MediaRouter.RouteGroup group, int index) {
            this.mCallback.onRouteGrouped(route, group, index);
        }

        @Override
        public void onRouteUngrouped(android.media.MediaRouter router, MediaRouter.RouteInfo route, MediaRouter.RouteGroup group) {
            this.mCallback.onRouteUngrouped(route, group);
        }

        @Override
        public void onRouteVolumeChanged(android.media.MediaRouter router, MediaRouter.RouteInfo route) {
            this.mCallback.onRouteVolumeChanged(route);
        }
    }

    static class VolumeCallbackProxy<T extends VolumeCallback> extends MediaRouter.VolumeCallback {
        protected final T mCallback;

        public VolumeCallbackProxy(T callback) {
            this.mCallback = callback;
        }

        @Override
        public void onVolumeSetRequest(MediaRouter.RouteInfo route, int volume) {
            this.mCallback.onVolumeSetRequest(route, volume);
        }

        @Override
        public void onVolumeUpdateRequest(MediaRouter.RouteInfo route, int direction) {
            this.mCallback.onVolumeUpdateRequest(route, direction);
        }
    }
}
