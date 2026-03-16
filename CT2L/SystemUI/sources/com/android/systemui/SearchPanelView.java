package com.android.systemui;

import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.StatusBarPanel;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class SearchPanelView extends FrameLayout implements StatusBarPanel {
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private BaseStatusBar mBar;
    private SearchPanelCircleView mCircle;
    private final Context mContext;
    private boolean mDraggedFarEnough;
    private boolean mDragging;
    private boolean mHorizontal;
    private boolean mLaunchPending;
    private boolean mLaunching;
    private ImageView mLogo;
    private View mScrim;
    private float mStartDrag;
    private float mStartTouch;
    private int mThreshold;

    public SearchPanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        this.mThreshold = context.getResources().getDimensionPixelSize(R.dimen.search_panel_threshold);
    }

    private void startAssistActivity() {
        if (this.mBar.isDeviceProvisioned()) {
            this.mBar.animateCollapsePanels(1);
            final Intent intent = ((SearchManager) this.mContext.getSystemService("search")).getAssistIntent(this.mContext, true, -2);
            if (intent != null) {
                try {
                    final ActivityOptions opts = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.search_launch_enter, R.anim.search_launch_exit);
                    intent.addFlags(268435456);
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            SearchPanelView.this.mContext.startActivityAsUser(intent, opts.toBundle(), new UserHandle(-2));
                        }
                    });
                } catch (ActivityNotFoundException e) {
                    Log.w("SearchPanelView", "Activity not found for " + intent.getAction());
                }
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContext.getSystemService("layout_inflater");
        this.mCircle = (SearchPanelCircleView) findViewById(R.id.search_panel_circle);
        this.mLogo = (ImageView) findViewById(R.id.search_logo);
        this.mScrim = findViewById(R.id.search_panel_scrim);
    }

    private void maybeSwapSearchIcon() {
        Intent intent = ((SearchManager) this.mContext.getSystemService("search")).getAssistIntent(this.mContext, false, -2);
        if (intent != null) {
            ComponentName component = intent.getComponent();
            replaceDrawable(this.mLogo, component, "com.android.systemui.action_assist_icon");
        } else {
            this.mLogo.setImageDrawable(null);
        }
    }

    public void replaceDrawable(ImageView v, ComponentName component, String name) {
        int iconResId;
        if (component != null) {
            try {
                PackageManager packageManager = this.mContext.getPackageManager();
                Bundle metaData = packageManager.getActivityInfo(component, 128).metaData;
                if (metaData != null && (iconResId = metaData.getInt(name)) != 0) {
                    Resources res = packageManager.getResourcesForActivity(component);
                    v.setImageDrawable(res.getDrawable(iconResId));
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("SearchPanelView", "Failed to swap drawable; " + component.flattenToShortString() + " not found", e);
            } catch (Resources.NotFoundException nfe) {
                Log.w("SearchPanelView", "Failed to swap drawable from " + component.flattenToShortString(), nfe);
            }
        }
        v.setImageDrawable(null);
    }

    @Override
    public boolean isInContentArea(int x, int y) {
        return true;
    }

    private void vibrate() {
        Context context = getContext();
        if (Settings.System.getIntForUser(context.getContentResolver(), "haptic_feedback_enabled", 1, -2) != 0) {
            Resources res = context.getResources();
            Vibrator vibrator = (Vibrator) context.getSystemService("vibrator");
            vibrator.vibrate(res.getInteger(R.integer.config_search_panel_view_vibration_duration), VIBRATION_ATTRIBUTES);
        }
    }

    public void show(boolean show, boolean animate) {
        if (show) {
            maybeSwapSearchIcon();
            if (getVisibility() != 0) {
                setVisibility(0);
                vibrate();
                if (animate) {
                    startEnterAnimation();
                } else {
                    this.mScrim.setAlpha(1.0f);
                }
            }
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
            return;
        }
        if (animate) {
            startAbortAnimation();
        } else {
            setVisibility(4);
        }
    }

    private void startEnterAnimation() {
        this.mCircle.startEnterAnimation();
        this.mScrim.setAlpha(0.0f);
        this.mScrim.animate().alpha(1.0f).setDuration(300L).setStartDelay(50L).setInterpolator(PhoneStatusBar.ALPHA_IN).start();
    }

    private void startAbortAnimation() {
        this.mCircle.startAbortAnimation(new Runnable() {
            @Override
            public void run() {
                SearchPanelView.this.mCircle.setAnimatingOut(false);
                SearchPanelView.this.setVisibility(4);
            }
        });
        this.mCircle.setAnimatingOut(true);
        this.mScrim.animate().alpha(0.0f).setDuration(300L).setStartDelay(0L).setInterpolator(PhoneStatusBar.ALPHA_OUT);
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    public boolean isShowing() {
        return getVisibility() == 0 && !this.mCircle.isAnimatingOut();
    }

    public void setBar(BaseStatusBar bar) {
        this.mBar = bar;
    }

    public boolean isAssistantAvailable() {
        return ((SearchManager) this.mContext.getSystemService("search")).getAssistIntent(this.mContext, false, -2) != null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mLaunching || this.mLaunchPending) {
            return false;
        }
        int action = event.getActionMasked();
        switch (action) {
            case 0:
                this.mStartTouch = this.mHorizontal ? event.getX() : event.getY();
                this.mDragging = false;
                this.mDraggedFarEnough = false;
                this.mCircle.reset();
                return true;
            case 1:
            case 3:
                if (this.mDraggedFarEnough) {
                    if (this.mCircle.isAnimationRunning(true)) {
                        this.mLaunchPending = true;
                        this.mCircle.setAnimatingOut(true);
                        this.mCircle.performOnAnimationFinished(new Runnable() {
                            @Override
                            public void run() {
                                SearchPanelView.this.startExitAnimation();
                            }
                        });
                        return true;
                    }
                    startExitAnimation();
                    return true;
                }
                startAbortAnimation();
                return true;
            case 2:
                float currentTouch = this.mHorizontal ? event.getX() : event.getY();
                if (getVisibility() == 0 && !this.mDragging && (!this.mCircle.isAnimationRunning(true) || Math.abs(this.mStartTouch - currentTouch) > this.mThreshold)) {
                    this.mStartDrag = currentTouch;
                    this.mDragging = true;
                }
                if (!this.mDragging) {
                    return true;
                }
                float offset = Math.max(this.mStartDrag - currentTouch, 0.0f);
                this.mCircle.setDragDistance(offset);
                this.mDraggedFarEnough = Math.abs(this.mStartTouch - currentTouch) > ((float) this.mThreshold);
                this.mCircle.setDraggedFarEnough(this.mDraggedFarEnough);
                return true;
            default:
                return true;
        }
    }

    private void startExitAnimation() {
        this.mLaunchPending = false;
        if (!this.mLaunching && getVisibility() == 0) {
            this.mLaunching = true;
            startAssistActivity();
            vibrate();
            this.mCircle.setAnimatingOut(true);
            this.mCircle.startExitAnimation(new Runnable() {
                @Override
                public void run() {
                    SearchPanelView.this.mLaunching = false;
                    SearchPanelView.this.mCircle.setAnimatingOut(false);
                    SearchPanelView.this.setVisibility(4);
                }
            });
            this.mScrim.animate().alpha(0.0f).setDuration(300L).setStartDelay(0L).setInterpolator(PhoneStatusBar.ALPHA_OUT);
        }
    }

    public void setHorizontal(boolean horizontal) {
        this.mHorizontal = horizontal;
        this.mCircle.setHorizontal(horizontal);
    }
}
