package com.android.gallery3d.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import com.android.gallery3d.R;
import java.util.HashMap;
import java.util.Map;

public class PhotoPageBottomControls implements View.OnClickListener {
    private ViewGroup mContainer;
    private Delegate mDelegate;
    private ViewGroup mParentLayout;
    private boolean mContainerVisible = false;
    private Map<View, Boolean> mControlsVisible = new HashMap();
    private Animation mContainerAnimIn = new AlphaAnimation(0.0f, 1.0f);
    private Animation mContainerAnimOut = new AlphaAnimation(1.0f, 0.0f);

    public interface Delegate {
        boolean canDisplayBottomControl(int i);

        boolean canDisplayBottomControls();

        void onBottomControlClicked(int i);

        void refreshBottomControlsWhenReady();
    }

    private static Animation getControlAnimForVisibility(boolean visible) {
        Animation anim = visible ? new AlphaAnimation(0.0f, 1.0f) : new AlphaAnimation(1.0f, 0.0f);
        anim.setDuration(150L);
        return anim;
    }

    public PhotoPageBottomControls(Delegate delegate, Context context, RelativeLayout layout) {
        this.mDelegate = delegate;
        this.mParentLayout = layout;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mContainer = (ViewGroup) inflater.inflate(R.layout.photopage_bottom_controls, this.mParentLayout, false);
        this.mParentLayout.addView(this.mContainer);
        for (int i = this.mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = this.mContainer.getChildAt(i);
            child.setOnClickListener(this);
            this.mControlsVisible.put(child, false);
        }
        this.mContainerAnimIn.setDuration(200L);
        this.mContainerAnimOut.setDuration(200L);
        this.mDelegate.refreshBottomControlsWhenReady();
    }

    private void hide() {
        this.mContainer.clearAnimation();
        this.mContainerAnimOut.reset();
        this.mContainer.startAnimation(this.mContainerAnimOut);
        this.mContainer.setVisibility(4);
    }

    private void show() {
        this.mContainer.clearAnimation();
        this.mContainerAnimIn.reset();
        this.mContainer.startAnimation(this.mContainerAnimIn);
        this.mContainer.setVisibility(0);
    }

    public void refresh() {
        boolean visible = this.mDelegate.canDisplayBottomControls();
        boolean containerVisibilityChanged = visible != this.mContainerVisible;
        if (containerVisibilityChanged) {
            if (visible) {
                show();
            } else {
                hide();
            }
            this.mContainerVisible = visible;
        }
        if (this.mContainerVisible) {
            for (View control : this.mControlsVisible.keySet()) {
                Boolean prevVisibility = this.mControlsVisible.get(control);
                boolean curVisibility = this.mDelegate.canDisplayBottomControl(control.getId());
                if (prevVisibility.booleanValue() != curVisibility) {
                    if (!containerVisibilityChanged) {
                        control.clearAnimation();
                        control.startAnimation(getControlAnimForVisibility(curVisibility));
                    }
                    control.setVisibility(curVisibility ? 0 : 4);
                    this.mControlsVisible.put(control, Boolean.valueOf(curVisibility));
                }
            }
            this.mContainer.requestLayout();
        }
    }

    public void cleanup() {
        this.mParentLayout.removeView(this.mContainer);
        this.mControlsVisible.clear();
    }

    @Override
    public void onClick(View view) {
        Boolean controlVisible = this.mControlsVisible.get(view);
        if (this.mContainerVisible && controlVisible != null && controlVisible.booleanValue()) {
            this.mDelegate.onBottomControlClicked(view.getId());
        }
    }
}
