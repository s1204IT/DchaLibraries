package android.app;

import android.content.Context;
import android.media.MediaRouter;
import android.util.Log;
import android.view.ActionProvider;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;

public class MediaRouteActionProvider extends ActionProvider {
    private static final String TAG = "MediaRouteActionProvider";
    private MediaRouteButton mButton;
    private final MediaRouterCallback mCallback;
    private final Context mContext;
    private View.OnClickListener mExtendedSettingsListener;
    private int mRouteTypes;
    private final MediaRouter mRouter;

    public MediaRouteActionProvider(Context context) {
        super(context);
        this.mContext = context;
        this.mRouter = (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        this.mCallback = new MediaRouterCallback(this);
        setRouteTypes(1);
    }

    public void setRouteTypes(int types) {
        if (this.mRouteTypes == types) {
            return;
        }
        if (this.mRouteTypes != 0) {
            this.mRouter.removeCallback(this.mCallback);
        }
        this.mRouteTypes = types;
        if (types != 0) {
            this.mRouter.addCallback(types, this.mCallback, 8);
        }
        refreshRoute();
        if (this.mButton == null) {
            return;
        }
        this.mButton.setRouteTypes(this.mRouteTypes);
    }

    public void setExtendedSettingsClickListener(View.OnClickListener listener) {
        this.mExtendedSettingsListener = listener;
        if (this.mButton == null) {
            return;
        }
        this.mButton.setExtendedSettingsClickListener(listener);
    }

    @Override
    public View onCreateActionView() {
        throw new UnsupportedOperationException("Use onCreateActionView(MenuItem) instead.");
    }

    @Override
    public View onCreateActionView(MenuItem item) {
        if (this.mButton != null) {
            Log.e(TAG, "onCreateActionView: this ActionProvider is already associated with a menu item. Don't reuse MediaRouteActionProvider instances! Abandoning the old one...");
        }
        this.mButton = new MediaRouteButton(this.mContext);
        this.mButton.setCheatSheetEnabled(true);
        this.mButton.setRouteTypes(this.mRouteTypes);
        this.mButton.setExtendedSettingsClickListener(this.mExtendedSettingsListener);
        this.mButton.setLayoutParams(new ViewGroup.LayoutParams(-2, -1));
        return this.mButton;
    }

    @Override
    public boolean onPerformDefaultAction() {
        if (this.mButton != null) {
            return this.mButton.showDialogInternal();
        }
        return false;
    }

    @Override
    public boolean overridesItemVisibility() {
        return true;
    }

    @Override
    public boolean isVisible() {
        return this.mRouter.isRouteAvailable(this.mRouteTypes, 1);
    }

    private void refreshRoute() {
        refreshVisibility();
    }

    private static class MediaRouterCallback extends MediaRouter.SimpleCallback {
        private final WeakReference<MediaRouteActionProvider> mProviderWeak;

        public MediaRouterCallback(MediaRouteActionProvider provider) {
            this.mProviderWeak = new WeakReference<>(provider);
        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoute(router);
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoute(router);
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoute(router);
        }

        private void refreshRoute(MediaRouter router) {
            MediaRouteActionProvider provider = this.mProviderWeak.get();
            if (provider != null) {
                provider.refreshRoute();
            } else {
                router.removeCallback(this);
            }
        }
    }
}
