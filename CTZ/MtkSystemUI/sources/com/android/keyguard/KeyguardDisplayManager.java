package com.android.keyguard;

import android.app.Presentation;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.media.MediaRouter;
import android.os.Bundle;
import android.util.Slog;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

public class KeyguardDisplayManager {
    private static boolean DEBUG = KeyguardConstants.DEBUG;
    private final ViewMediatorCallback mCallback;
    private final Context mContext;
    private final MediaRouter mMediaRouter;
    private final MediaRouter.SimpleCallback mMediaRouterCallback = new MediaRouter.SimpleCallback() {
        @Override
        public void onRouteSelected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {
            if (KeyguardDisplayManager.DEBUG) {
                Slog.d("KeyguardDisplayManager", "onRouteSelected: type=" + i + ", info=" + routeInfo);
            }
            KeyguardDisplayManager.this.updateDisplays(KeyguardDisplayManager.this.mShowing);
        }

        @Override
        public void onRouteUnselected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {
            if (KeyguardDisplayManager.DEBUG) {
                Slog.d("KeyguardDisplayManager", "onRouteUnselected: type=" + i + ", info=" + routeInfo);
            }
            KeyguardDisplayManager.this.updateDisplays(KeyguardDisplayManager.this.mShowing);
        }

        @Override
        public void onRoutePresentationDisplayChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            if (KeyguardDisplayManager.DEBUG) {
                Slog.d("KeyguardDisplayManager", "onRoutePresentationDisplayChanged: info=" + routeInfo);
            }
            KeyguardDisplayManager.this.updateDisplays(KeyguardDisplayManager.this.mShowing);
        }
    };
    private DialogInterface.OnDismissListener mOnDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            KeyguardDisplayManager.this.mPresentation = null;
        }
    };
    Presentation mPresentation;
    private boolean mShowing;

    public KeyguardDisplayManager(Context context, ViewMediatorCallback viewMediatorCallback) {
        this.mContext = context;
        this.mCallback = viewMediatorCallback;
        this.mMediaRouter = (MediaRouter) this.mContext.getSystemService("media_router");
    }

    public void show() {
        if (!this.mShowing) {
            if (DEBUG) {
                Slog.v("KeyguardDisplayManager", "show");
            }
            this.mMediaRouter.addCallback(4, this.mMediaRouterCallback, 8);
            updateDisplays(true);
        }
        this.mShowing = true;
    }

    public void hide() {
        if (this.mShowing) {
            if (DEBUG) {
                Slog.v("KeyguardDisplayManager", "hide");
            }
            this.mMediaRouter.removeCallback(this.mMediaRouterCallback);
            updateDisplays(false);
        }
        this.mShowing = false;
    }

    protected void updateDisplays(boolean z) {
        Presentation presentation = this.mPresentation;
        if (z) {
            MediaRouter.RouteInfo selectedRoute = this.mMediaRouter.getSelectedRoute(4);
            Display presentationDisplay = selectedRoute != null && selectedRoute.getPlaybackType() == 1 ? selectedRoute.getPresentationDisplay() : null;
            if (this.mPresentation != null && this.mPresentation.getDisplay() != presentationDisplay) {
                if (DEBUG) {
                    Slog.v("KeyguardDisplayManager", "Display gone: " + this.mPresentation.getDisplay());
                }
                this.mPresentation.dismiss();
                this.mPresentation = null;
            }
            if (this.mPresentation == null && presentationDisplay != null) {
                if (DEBUG) {
                    Slog.i("KeyguardDisplayManager", "Keyguard enabled on display: " + presentationDisplay);
                }
                this.mPresentation = new KeyguardPresentation(this.mContext, presentationDisplay, com.android.systemui.R.style.keyguard_presentation_theme);
                this.mPresentation.setOnDismissListener(this.mOnDismissListener);
                try {
                    this.mPresentation.show();
                } catch (WindowManager.InvalidDisplayException e) {
                    Slog.w("KeyguardDisplayManager", "Invalid display:", e);
                    this.mPresentation = null;
                }
            }
        } else if (this.mPresentation != null) {
            this.mPresentation.dismiss();
            this.mPresentation = null;
        }
        if (this.mPresentation != presentation) {
            this.mCallback.onSecondaryDisplayShowingChanged(this.mPresentation != null ? this.mPresentation.getDisplay().getDisplayId() : -1);
        }
    }

    private static final class KeyguardPresentation extends Presentation {
        private View mClock;
        private int mMarginLeft;
        private int mMarginTop;
        Runnable mMoveTextRunnable;
        private int mUsableHeight;
        private int mUsableWidth;

        public KeyguardPresentation(Context context, Display display, int i) {
            super(context, display, i);
            this.mMoveTextRunnable = new Runnable() {
                @Override
                public void run() {
                    int iRandom = KeyguardPresentation.this.mMarginLeft + ((int) (Math.random() * ((double) (KeyguardPresentation.this.mUsableWidth - KeyguardPresentation.this.mClock.getWidth()))));
                    int iRandom2 = KeyguardPresentation.this.mMarginTop + ((int) (Math.random() * ((double) (KeyguardPresentation.this.mUsableHeight - KeyguardPresentation.this.mClock.getHeight()))));
                    if (KeyguardDisplayManager.DEBUG) {
                        Slog.d("KeyguardDisplayManager", "mMarginLeft = " + KeyguardPresentation.this.mMarginLeft + ", mUsableWidth = " + KeyguardPresentation.this.mUsableWidth + " , mClock.getWidth() = " + KeyguardPresentation.this.mClock.getWidth() + " and final X = " + iRandom);
                        Slog.d("KeyguardDisplayManager", "mMarginTop = " + KeyguardPresentation.this.mMarginTop + ", mUsableHeight = " + KeyguardPresentation.this.mUsableHeight + " , mClock.getHeight() = " + KeyguardPresentation.this.mClock.getHeight() + " and final y = " + iRandom2);
                    }
                    KeyguardPresentation.this.mClock.setX(iRandom);
                    KeyguardPresentation.this.mClock.setY(iRandom2);
                    KeyguardPresentation.this.mClock.postDelayed(KeyguardPresentation.this.mMoveTextRunnable, 10000L);
                }
            };
            getWindow().setType(2009);
        }

        @Override
        public void onDetachedFromWindow() {
            this.mClock.removeCallbacks(this.mMoveTextRunnable);
        }

        @Override
        protected void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            Point point = new Point();
            getDisplay().getSize(point);
            this.mUsableWidth = (point.x * 80) / 100;
            this.mUsableHeight = (80 * point.y) / 100;
            this.mMarginLeft = (point.x * 20) / 200;
            this.mMarginTop = (20 * point.y) / 200;
            setContentView(com.android.systemui.R.layout.keyguard_presentation);
            this.mClock = findViewById(com.android.systemui.R.id.clock);
            this.mClock.post(this.mMoveTextRunnable);
        }
    }
}
