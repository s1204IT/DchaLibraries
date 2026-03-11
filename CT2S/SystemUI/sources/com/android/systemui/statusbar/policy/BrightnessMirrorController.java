package com.android.systemui.statusbar.policy;

import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowView;

public class BrightnessMirrorController {
    private final View mBrightnessMirror;
    private final View mPanelHolder;
    private final ScrimView mScrimBehind;
    public long TRANSITION_DURATION_OUT = 150;
    public long TRANSITION_DURATION_IN = 200;
    private final int[] mInt2Cache = new int[2];

    public BrightnessMirrorController(StatusBarWindowView statusBarWindow) {
        this.mScrimBehind = (ScrimView) statusBarWindow.findViewById(R.id.scrim_behind);
        this.mBrightnessMirror = statusBarWindow.findViewById(R.id.brightness_mirror);
        this.mPanelHolder = statusBarWindow.findViewById(R.id.panel_holder);
    }

    public void showMirror() {
        this.mBrightnessMirror.setVisibility(0);
        this.mScrimBehind.animateViewAlpha(0.0f, this.TRANSITION_DURATION_OUT, PhoneStatusBar.ALPHA_OUT);
        outAnimation(this.mPanelHolder.animate()).withLayer();
    }

    public void hideMirror() {
        this.mScrimBehind.animateViewAlpha(1.0f, this.TRANSITION_DURATION_IN, PhoneStatusBar.ALPHA_IN);
        inAnimation(this.mPanelHolder.animate()).withLayer().withEndAction(new Runnable() {
            @Override
            public void run() {
                BrightnessMirrorController.this.mBrightnessMirror.setVisibility(8);
            }
        });
    }

    private ViewPropertyAnimator outAnimation(ViewPropertyAnimator a) {
        return a.alpha(0.0f).setDuration(this.TRANSITION_DURATION_OUT).setInterpolator(PhoneStatusBar.ALPHA_OUT);
    }

    private ViewPropertyAnimator inAnimation(ViewPropertyAnimator a) {
        return a.alpha(1.0f).setDuration(this.TRANSITION_DURATION_IN).setInterpolator(PhoneStatusBar.ALPHA_IN);
    }

    public void setLocation(View original) {
        original.getLocationInWindow(this.mInt2Cache);
        int originalY = this.mInt2Cache[1];
        this.mBrightnessMirror.getLocationInWindow(this.mInt2Cache);
        int mirrorY = this.mInt2Cache[1];
        this.mBrightnessMirror.setTranslationY((this.mBrightnessMirror.getTranslationY() + originalY) - mirrorY);
    }

    public View getMirror() {
        return this.mBrightnessMirror;
    }

    public void updateResources() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mBrightnessMirror.getLayoutParams();
        lp.width = this.mBrightnessMirror.getResources().getDimensionPixelSize(R.dimen.notification_panel_width);
        lp.gravity = this.mBrightnessMirror.getResources().getInteger(R.integer.notification_panel_layout_gravity);
        this.mBrightnessMirror.setLayoutParams(lp);
        int padding = this.mBrightnessMirror.getResources().getDimensionPixelSize(R.dimen.notification_side_padding);
        this.mBrightnessMirror.setPadding(padding, this.mBrightnessMirror.getPaddingTop(), padding, this.mBrightnessMirror.getPaddingBottom());
    }
}
