package com.android.internal.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaRouter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import com.android.internal.R;

public class MediaRouteControllerDialog extends Dialog {
    private static final int VOLUME_UPDATE_DELAY_MILLIS = 250;
    private final MediaRouterCallback mCallback;
    private View mControlView;
    private boolean mCreated;
    private Drawable mCurrentIconDrawable;
    private Button mDisconnectButton;
    private Drawable mMediaRouteConnectingDrawable;
    private Drawable mMediaRouteOnDrawable;
    private final MediaRouter.RouteInfo mRoute;
    private final MediaRouter mRouter;
    private boolean mVolumeControlEnabled;
    private LinearLayout mVolumeLayout;
    private SeekBar mVolumeSlider;
    private boolean mVolumeSliderTouched;

    public MediaRouteControllerDialog(Context context, int theme) {
        super(context, theme);
        this.mVolumeControlEnabled = true;
        this.mRouter = (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        this.mCallback = new MediaRouterCallback();
        this.mRoute = this.mRouter.getSelectedRoute();
    }

    public MediaRouter.RouteInfo getRoute() {
        return this.mRoute;
    }

    public View onCreateMediaControlView(Bundle savedInstanceState) {
        return null;
    }

    public View getMediaControlView() {
        return this.mControlView;
    }

    public void setVolumeControlEnabled(boolean enable) {
        if (this.mVolumeControlEnabled != enable) {
            this.mVolumeControlEnabled = enable;
            if (this.mCreated) {
                updateVolume();
            }
        }
    }

    public boolean isVolumeControlEnabled() {
        return this.mVolumeControlEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(3);
        setContentView(R.layout.media_route_controller_dialog);
        this.mVolumeLayout = (LinearLayout) findViewById(R.id.media_route_volume_layout);
        this.mVolumeSlider = (SeekBar) findViewById(R.id.media_route_volume_slider);
        this.mVolumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private final Runnable mStopTrackingTouch = new Runnable() {
                @Override
                public void run() {
                    if (MediaRouteControllerDialog.this.mVolumeSliderTouched) {
                        MediaRouteControllerDialog.this.mVolumeSliderTouched = false;
                        MediaRouteControllerDialog.this.updateVolume();
                    }
                }
            };

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (MediaRouteControllerDialog.this.mVolumeSliderTouched) {
                    MediaRouteControllerDialog.this.mVolumeSlider.removeCallbacks(this.mStopTrackingTouch);
                } else {
                    MediaRouteControllerDialog.this.mVolumeSliderTouched = true;
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MediaRouteControllerDialog.this.mVolumeSlider.postDelayed(this.mStopTrackingTouch, 250L);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MediaRouteControllerDialog.this.mRoute.requestSetVolume(progress);
                }
            }
        });
        this.mDisconnectButton = (Button) findViewById(R.id.media_route_disconnect_button);
        this.mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MediaRouteControllerDialog.this.mRoute.isSelected()) {
                    MediaRouteControllerDialog.this.mRouter.getDefaultRoute().select();
                }
                MediaRouteControllerDialog.this.dismiss();
            }
        });
        this.mCreated = true;
        if (update()) {
            this.mControlView = onCreateMediaControlView(savedInstanceState);
            FrameLayout controlFrame = (FrameLayout) findViewById(R.id.media_route_control_frame);
            if (this.mControlView != null) {
                controlFrame.addView(this.mControlView);
                controlFrame.setVisibility(0);
            } else {
                controlFrame.setVisibility(8);
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mRouter.addCallback(0, this.mCallback, 2);
        update();
    }

    @Override
    public void onDetachedFromWindow() {
        this.mRouter.removeCallback(this.mCallback);
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 25 || keyCode == 24) {
            this.mRoute.requestUpdateVolume(keyCode == 25 ? -1 : 1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 25 || keyCode == 24) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean update() {
        if (!this.mRoute.isSelected() || this.mRoute.isDefault()) {
            dismiss();
            return false;
        }
        setTitle(this.mRoute.getName());
        updateVolume();
        Drawable icon = getIconDrawable();
        if (icon != this.mCurrentIconDrawable) {
            this.mCurrentIconDrawable = icon;
            getWindow().setFeatureDrawable(3, icon);
        }
        return true;
    }

    private Drawable getIconDrawable() {
        if (this.mRoute.isConnecting()) {
            if (this.mMediaRouteConnectingDrawable == null) {
                this.mMediaRouteConnectingDrawable = getContext().getDrawable(R.drawable.ic_media_route_connecting_holo_dark);
            }
            return this.mMediaRouteConnectingDrawable;
        }
        if (this.mMediaRouteOnDrawable == null) {
            this.mMediaRouteOnDrawable = getContext().getDrawable(R.drawable.ic_media_route_on_holo_dark);
        }
        return this.mMediaRouteOnDrawable;
    }

    private void updateVolume() {
        if (!this.mVolumeSliderTouched) {
            if (isVolumeControlAvailable()) {
                this.mVolumeLayout.setVisibility(0);
                this.mVolumeSlider.setMax(this.mRoute.getVolumeMax());
                this.mVolumeSlider.setProgress(this.mRoute.getVolume());
                return;
            }
            this.mVolumeLayout.setVisibility(8);
        }
    }

    private boolean isVolumeControlAvailable() {
        return this.mVolumeControlEnabled && this.mRoute.getVolumeHandling() == 1;
    }

    private final class MediaRouterCallback extends MediaRouter.SimpleCallback {
        private MediaRouterCallback() {
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            MediaRouteControllerDialog.this.update();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            MediaRouteControllerDialog.this.update();
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            if (route == MediaRouteControllerDialog.this.mRoute) {
                MediaRouteControllerDialog.this.updateVolume();
            }
        }

        @Override
        public void onRouteGrouped(MediaRouter router, MediaRouter.RouteInfo info, MediaRouter.RouteGroup group, int index) {
            MediaRouteControllerDialog.this.update();
        }

        @Override
        public void onRouteUngrouped(MediaRouter router, MediaRouter.RouteInfo info, MediaRouter.RouteGroup group) {
            MediaRouteControllerDialog.this.update();
        }
    }
}
