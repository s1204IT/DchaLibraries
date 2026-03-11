package com.android.systemui.statusbar.phone;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.MotionEvent;
import android.view.View;
import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.R;

public final class NavigationBarTransitions extends BarTransitions {
    private final IStatusBarService mBarService;
    private boolean mLightsOut;
    private final View.OnTouchListener mLightsOutListener;
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

    public void init() {
        applyModeBackground(-1, getMode(), false);
        applyMode(getMode(), false, true);
    }

    @Override
    protected void onTransition(int oldMode, int newMode, boolean animate) {
        super.onTransition(oldMode, newMode, animate);
        applyMode(newMode, animate, false);
    }

    private void applyMode(int mode, boolean animate, boolean force) {
        applyLightsOut(isLightsOut(mode), animate, force);
    }

    public void applyLightsOut(boolean lightsOut, boolean animate, boolean force) {
        if (force || lightsOut != this.mLightsOut) {
            this.mLightsOut = lightsOut;
            View navButtons = this.mView.getCurrentView().findViewById(R.id.nav_buttons);
            navButtons.animate().cancel();
            float navButtonsAlpha = lightsOut ? 0.5f : 1.0f;
            if (!animate) {
                navButtons.setAlpha(navButtonsAlpha);
            } else {
                int duration = lightsOut ? 750 : 250;
                navButtons.animate().alpha(navButtonsAlpha).setDuration(duration).start();
            }
        }
    }
}
