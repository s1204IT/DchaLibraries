package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.KeyButtonView;

public final class NavigationBarTransitions extends BarTransitions {
    private final IStatusBarService mBarService;
    private boolean mLightsOut;
    private final View.OnTouchListener mLightsOutListener;
    private int mRequestedMode;
    private boolean mVertical;
    private final NavigationBarView mView;

    public NavigationBarTransitions(NavigationBarView view) {
        super(view, R.drawable.nav_background);
        this.mLightsOutListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                if (ev.getAction() == 0) {
                    NavigationBarTransitions.this.applyLightsOut(false, false, false);
                    try {
                        NavigationBarTransitions.this.mBarService.setSystemUiVisibility(0, 1, "LightsOutListener");
                    } catch (RemoteException e) {
                    }
                }
                return false;
            }
        };
        this.mView = view;
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
    }

    public void init(boolean isVertical) {
        setVertical(isVertical);
        applyModeBackground(-1, getMode(), false);
        applyMode(getMode(), false, true);
    }

    public void setVertical(boolean isVertical) {
        this.mVertical = isVertical;
        transitionTo(this.mRequestedMode, false);
    }

    @Override
    public void transitionTo(int mode, boolean animate) {
        this.mRequestedMode = mode;
        if (this.mVertical) {
            if (mode == 2 || mode == 4) {
                mode = 0;
            } else if (mode == 6) {
                mode = 3;
            }
        }
        super.transitionTo(mode, animate);
    }

    @Override
    protected void onTransition(int oldMode, int newMode, boolean animate) {
        super.onTransition(oldMode, newMode, animate);
        applyMode(newMode, animate, false);
    }

    private void applyMode(int mode, boolean animate, boolean force) {
        float alpha = alphaForMode(mode);
        setKeyButtonViewQuiescentAlpha(this.mView.getHomeButton(), alpha, animate);
        setKeyButtonViewQuiescentAlpha(this.mView.getRecentsButton(), alpha, animate);
        setKeyButtonViewQuiescentAlpha(this.mView.getMenuButton(), alpha, animate);
        setKeyButtonViewQuiescentAlpha(this.mView.getImeSwitchButton(), alpha, animate);
        applyBackButtonQuiescentAlpha(mode, animate);
        applyLightsOut(isLightsOut(mode), animate, force);
    }

    private float alphaForMode(int mode) {
        boolean isOpaque = mode == 0 || mode == 3;
        if (isOpaque) {
        }
        return 1.0f;
    }

    public void applyBackButtonQuiescentAlpha(int mode, boolean animate) {
        float backAlpha = maxVisibleQuiescentAlpha(maxVisibleQuiescentAlpha(maxVisibleQuiescentAlpha(maxVisibleQuiescentAlpha(0.0f, this.mView.getHomeButton()), this.mView.getRecentsButton()), this.mView.getMenuButton()), this.mView.getImeSwitchButton());
        if (backAlpha > 0.0f) {
            setKeyButtonViewQuiescentAlpha(this.mView.getBackButton(), backAlpha, animate);
        }
    }

    private static float maxVisibleQuiescentAlpha(float max, View v) {
        if ((v instanceof KeyButtonView) && v.isShown()) {
            return Math.max(max, ((KeyButtonView) v).getQuiescentAlpha());
        }
        return max;
    }

    private void setKeyButtonViewQuiescentAlpha(View button, float alpha, boolean animate) {
        if (button instanceof KeyButtonView) {
            ((KeyButtonView) button).setQuiescentAlpha(alpha, animate);
        }
    }

    private void applyLightsOut(boolean lightsOut, boolean animate, boolean force) {
        if (force || lightsOut != this.mLightsOut) {
            this.mLightsOut = lightsOut;
            View navButtons = this.mView.getCurrentView().findViewById(R.id.nav_buttons);
            final View lowLights = this.mView.getCurrentView().findViewById(R.id.lights_out);
            navButtons.animate().cancel();
            lowLights.animate().cancel();
            float navButtonsAlpha = lightsOut ? 0.0f : 1.0f;
            float lowLightsAlpha = lightsOut ? 1.0f : 0.0f;
            if (!animate) {
                navButtons.setAlpha(navButtonsAlpha);
                lowLights.setAlpha(lowLightsAlpha);
                lowLights.setVisibility(lightsOut ? 0 : 8);
                return;
            }
            int duration = lightsOut ? 750 : 250;
            navButtons.animate().alpha(navButtonsAlpha).setDuration(duration).start();
            lowLights.setOnTouchListener(this.mLightsOutListener);
            if (lowLights.getVisibility() == 8) {
                lowLights.setAlpha(0.0f);
                lowLights.setVisibility(0);
            }
            lowLights.animate().alpha(lowLightsAlpha).setDuration(duration).setInterpolator(new AccelerateInterpolator(2.0f)).setListener(lightsOut ? null : new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator _a) {
                    lowLights.setVisibility(8);
                }
            }).start();
        }
    }
}
