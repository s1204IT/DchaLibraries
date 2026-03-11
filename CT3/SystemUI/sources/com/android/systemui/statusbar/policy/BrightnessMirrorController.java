package com.android.systemui.statusbar.policy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

public class BrightnessMirrorController {
    private View mBrightnessMirror;
    private final View mNotificationPanel;
    private final ScrimView mScrimBehind;
    private final NotificationStackScrollLayout mStackScroller;
    private final StatusBarWindowView mStatusBarWindow;
    public long TRANSITION_DURATION_OUT = 150;
    public long TRANSITION_DURATION_IN = 200;
    private final int[] mInt2Cache = new int[2];

    public BrightnessMirrorController(StatusBarWindowView statusBarWindow) {
        this.mStatusBarWindow = statusBarWindow;
        this.mScrimBehind = (ScrimView) statusBarWindow.findViewById(R.id.scrim_behind);
        this.mBrightnessMirror = statusBarWindow.findViewById(R.id.brightness_mirror);
        this.mNotificationPanel = statusBarWindow.findViewById(R.id.notification_panel);
        this.mStackScroller = (NotificationStackScrollLayout) statusBarWindow.findViewById(R.id.notification_stack_scroller);
    }

    public void showMirror() {
        this.mBrightnessMirror.setVisibility(0);
        this.mStackScroller.setFadingOut(true);
        this.mScrimBehind.animateViewAlpha(0.0f, this.TRANSITION_DURATION_OUT, Interpolators.ALPHA_OUT);
        outAnimation(this.mNotificationPanel.animate()).withLayer();
    }

    public void hideMirror() {
        this.mScrimBehind.animateViewAlpha(1.0f, this.TRANSITION_DURATION_IN, Interpolators.ALPHA_IN);
        inAnimation(this.mNotificationPanel.animate()).withLayer().withEndAction(new Runnable() {
            @Override
            public void run() {
                BrightnessMirrorController.this.mBrightnessMirror.setVisibility(4);
                BrightnessMirrorController.this.mStackScroller.setFadingOut(false);
            }
        });
    }

    private ViewPropertyAnimator outAnimation(ViewPropertyAnimator a) {
        return a.alpha(0.0f).setDuration(this.TRANSITION_DURATION_OUT).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(null);
    }

    private ViewPropertyAnimator inAnimation(ViewPropertyAnimator a) {
        return a.alpha(1.0f).setDuration(this.TRANSITION_DURATION_IN).setInterpolator(Interpolators.ALPHA_IN);
    }

    public void setLocation(View original) {
        original.getLocationInWindow(this.mInt2Cache);
        int originalX = this.mInt2Cache[0] + (original.getWidth() / 2);
        int originalY = this.mInt2Cache[1] + (original.getHeight() / 2);
        this.mBrightnessMirror.setTranslationX(0.0f);
        this.mBrightnessMirror.setTranslationY(0.0f);
        this.mBrightnessMirror.getLocationInWindow(this.mInt2Cache);
        int mirrorX = this.mInt2Cache[0] + (this.mBrightnessMirror.getWidth() / 2);
        int mirrorY = this.mInt2Cache[1] + (this.mBrightnessMirror.getHeight() / 2);
        this.mBrightnessMirror.setTranslationX(originalX - mirrorX);
        this.mBrightnessMirror.setTranslationY(originalY - mirrorY);
    }

    public View getMirror() {
        return this.mBrightnessMirror;
    }

    public void updateResources() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mBrightnessMirror.getLayoutParams();
        lp.width = this.mBrightnessMirror.getResources().getDimensionPixelSize(R.dimen.notification_panel_width);
        lp.gravity = this.mBrightnessMirror.getResources().getInteger(R.integer.notification_panel_layout_gravity);
        this.mBrightnessMirror.setLayoutParams(lp);
    }

    public void onDensityOrFontScaleChanged() {
        int index = this.mStatusBarWindow.indexOfChild(this.mBrightnessMirror);
        this.mStatusBarWindow.removeView(this.mBrightnessMirror);
        this.mBrightnessMirror = LayoutInflater.from(this.mBrightnessMirror.getContext()).inflate(R.layout.brightness_mirror, (ViewGroup) this.mStatusBarWindow, false);
        this.mStatusBarWindow.addView(this.mBrightnessMirror, index);
    }
}
