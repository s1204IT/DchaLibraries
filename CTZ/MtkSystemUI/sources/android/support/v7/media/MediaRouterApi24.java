package android.support.v7.media;

import android.media.MediaRouter;

final class MediaRouterApi24 {

    public static final class RouteInfo {
        public static int getDeviceType(Object routeObj) {
            return ((MediaRouter.RouteInfo) routeObj).getDeviceType();
        }
    }
}
