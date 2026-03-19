package com.mediatek.gallery3d.video;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Presentation;
import android.content.Context;
import android.media.MediaRouter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.util.Log;

public abstract class RemoteConnection {
    private static final int CONNECTED_DELAY = 500;
    private static final int CREATE_ROUTE_RETYR_TIMES = 3;
    private static final String TAG = "VP_RemoteConnection";
    private Activity mActivity;
    protected Context mContext;
    protected MediaRouter mMediaRouter;
    private MovieView mMovieView;
    protected ConnectionEventListener mOnEventListener;
    private PowerSavingPresentation mPresentation;
    private View mRootView;
    protected final Runnable mSelectMediaRouteRunnable = new Runnable() {
        @Override
        public void run() {
            RemoteConnection.access$008(RemoteConnection.this);
            MediaRouter.RouteInfo selectedRoute = RemoteConnection.this.mMediaRouter.getSelectedRoute(2);
            Display presentationDisplay = null;
            if (Build.VERSION.SDK_INT >= 17 && selectedRoute != null) {
                presentationDisplay = selectedRoute.getPresentationDisplay();
            }
            if (presentationDisplay == null) {
                RemoteConnection.this.mHandler.postDelayed(RemoteConnection.this.mSelectMediaRouteRunnable, 500L);
            }
            if (presentationDisplay != null || RemoteConnection.this.mCreatedRouteTimes >= 3) {
                RemoteConnection.this.mCreatedRouteTimes = 0;
                RemoteConnection.this.updatePresentation();
            }
            Log.v(RemoteConnection.TAG, "mSelectMediaRouteRunnable mCreatedRouteTimes = " + RemoteConnection.this.mCreatedRouteTimes + " presentationDisplay = " + presentationDisplay);
        }
    };
    protected final Runnable mUnselectMediaRouteRunnable = new Runnable() {
        @Override
        public void run() {
            Log.v(RemoteConnection.TAG, "mUnselectMediaRouteRunnable");
            RemoteConnection.this.updatePresentation();
        }
    };
    protected Handler mHandler = new Handler();
    private int mCreatedRouteTimes = 0;

    public interface ConnectionEventListener {
        public static final int EVENT_CONTINUE_PLAY = 1;
        public static final int EVENT_END_POWERSAVING = 4;
        public static final int EVENT_FINISH_NOW = 3;
        public static final int EVENT_START_POWERSAVING = 5;
        public static final int EVENT_STAY_PAUSE = 2;

        void onEvent(int i);
    }

    public abstract void doRelease();

    protected abstract void entreExtensionIfneed();

    public abstract boolean isConnected();

    public abstract boolean isInExtensionDisplay();

    public abstract void refreshConnection(boolean z);

    static int access$008(RemoteConnection remoteConnection) {
        int i = remoteConnection.mCreatedRouteTimes;
        remoteConnection.mCreatedRouteTimes = i + 1;
        return i;
    }

    public RemoteConnection(Activity activity, View view, ConnectionEventListener connectionEventListener) {
        this.mActivity = activity;
        this.mContext = this.mActivity.getApplicationContext();
        this.mRootView = view;
        this.mMovieView = (MovieView) this.mRootView.findViewById(R.id.movie_view);
        this.mMediaRouter = (MediaRouter) this.mContext.getSystemService("media_router");
        this.mOnEventListener = connectionEventListener;
    }

    protected void dismissPresentation() {
        Log.v(TAG, "dismissPresentaion() mPresentation= " + this.mPresentation);
        if (this.mPresentation != null) {
            this.mPresentation.removeSurfaceView();
            this.mPresentation.dismiss();
            this.mPresentation = null;
            ((ViewGroup) this.mRootView).addView(this.mMovieView, 0);
        }
    }

    @TargetApi(17)
    public void updatePresentation() {
        Display presentationDisplay;
        MediaRouter.RouteInfo selectedRoute = this.mMediaRouter.getSelectedRoute(2);
        if (Build.VERSION.SDK_INT >= 17) {
            if (selectedRoute != null) {
                presentationDisplay = selectedRoute.getPresentationDisplay();
            } else {
                presentationDisplay = null;
            }
            if (this.mPresentation != null && this.mPresentation.getDisplay() != presentationDisplay) {
                Log.v(TAG, "Dismissing presentation for the current route disconnected");
                dismissPresentation();
            }
            if (this.mPresentation == null && presentationDisplay != null) {
                Log.v(TAG, "Showing presentation on display");
                if (this.mMovieView.getParent() != null) {
                    ((ViewGroup) this.mMovieView.getParent()).removeView(this.mMovieView);
                }
                this.mPresentation = new PowerSavingPresentation(this.mActivity, presentationDisplay, this.mMovieView);
                try {
                    this.mPresentation.show();
                } catch (WindowManager.InvalidDisplayException e) {
                    Log.v(TAG, "Couldn't show presentation!", e);
                    this.mPresentation = null;
                }
            }
        }
    }

    @TargetApi(17)
    protected final class PowerSavingPresentation extends Presentation {
        private MovieView mMovieView;
        private RelativeLayout mRoot;

        public PowerSavingPresentation(Context context, Display display, MovieView movieView) {
            super(context, display);
            Log.v(RemoteConnection.TAG, "PowerSavingPresentation construct");
            this.mMovieView = movieView;
        }

        @Override
        protected void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            Log.v(RemoteConnection.TAG, "PowerSavingPresentation onCreate");
            setContentView(R.layout.m_presentation_with_media_router_content);
            this.mRoot = (RelativeLayout) findViewById(R.id.view_root);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-1, -1);
            layoutParams.addRule(13);
            this.mRoot.addView(this.mMovieView, layoutParams);
        }

        public void removeSurfaceView() {
            Log.v(RemoteConnection.TAG, "PowerSavingPresentation removeSurfaceView");
            this.mRoot.removeView(this.mMovieView);
        }
    }
}
