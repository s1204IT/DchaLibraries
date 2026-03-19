package com.android.gallery3d.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import java.util.HashMap;
import java.util.Map;

public class PhotoPageBottomControls implements View.OnClickListener {
    private ViewGroup mContainer;
    private Context mContext;
    private Delegate mDelegate;
    private ViewGroup mParentLayout;
    private boolean mContainerVisible = false;
    private Map<View, Boolean> mControlsVisible = new HashMap();
    private Animation mContainerAnimIn = new AlphaAnimation(0.0f, 1.0f);
    private Animation mContainerAnimOut = new AlphaAnimation(1.0f, 0.0f);
    private int mLeftButtonId = -1;
    private float mDensity = 0.0f;

    public interface Delegate {
        boolean canDisplayBottomControl(int i);

        boolean canDisplayBottomControls();

        void onBottomControlClicked(int i);

        void onBottomControlCreated();

        void refreshBottomControlsWhenReady();
    }

    private static Animation getControlAnimForVisibility(boolean z) {
        AlphaAnimation alphaAnimation;
        if (z) {
            alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        } else {
            alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        }
        alphaAnimation.setDuration(150L);
        return alphaAnimation;
    }

    public void setup(Delegate delegate, Context context, ViewGroup viewGroup) {
        this.mDelegate = delegate;
        this.mParentLayout = viewGroup;
        this.mContainer = (ViewGroup) ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.photopage_bottom_controls, this.mParentLayout, false);
        this.mContext = context;
        this.mDelegate.onBottomControlCreated();
        this.mParentLayout.addView(this.mContainer);
        for (int childCount = this.mContainer.getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = this.mContainer.getChildAt(childCount);
            childAt.setOnClickListener(this);
            this.mControlsVisible.put(childAt, false);
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
        boolean zCanDisplayBottomControls = this.mDelegate.canDisplayBottomControls();
        boolean z = zCanDisplayBottomControls != this.mContainerVisible;
        if (z) {
            if (zCanDisplayBottomControls) {
                show();
            } else {
                hide();
            }
            this.mContainerVisible = zCanDisplayBottomControls;
        }
        if (!this.mContainerVisible) {
            return;
        }
        for (View view : this.mControlsVisible.keySet()) {
            Boolean bool = this.mControlsVisible.get(view);
            boolean zCanDisplayBottomControl = this.mDelegate.canDisplayBottomControl(view.getId());
            if (bool.booleanValue() != zCanDisplayBottomControl) {
                if (!z) {
                    view.clearAnimation();
                    view.startAnimation(getControlAnimForVisibility(zCanDisplayBottomControl));
                }
                view.setVisibility(zCanDisplayBottomControl ? 0 : 4);
                this.mControlsVisible.put(view, Boolean.valueOf(zCanDisplayBottomControl));
            }
        }
        this.mContainer.requestLayout();
    }

    public void cleanup() {
        this.mParentLayout.removeView(this.mContainer);
        this.mControlsVisible.clear();
    }

    @Override
    public void onClick(View view) {
        Boolean bool = this.mControlsVisible.get(view);
        if (this.mContainerVisible && bool != null && bool.booleanValue()) {
            this.mDelegate.onBottomControlClicked(view.getId());
        }
    }

    public int addButtonToContainer(Drawable drawable) {
        int iGenerateViewId;
        if (this.mLeftButtonId == -1) {
            this.mLeftButtonId = this.mContainer.findViewById(R.id.photopage_bottom_control_edit).getId();
        }
        ImageButton imageButton = new ImageButton(this.mContext);
        if (Build.VERSION.SDK_INT >= 17) {
            iGenerateViewId = View.generateViewId();
        } else {
            iGenerateViewId = FeatureHelper.generateViewId();
        }
        imageButton.setId(iGenerateViewId);
        imageButton.setImageDrawable(drawable);
        if (Build.VERSION.SDK_INT >= 21) {
            imageButton.setBackground(this.mContext.getResources().getDrawable(R.drawable.photopage_bottom_button_background, null));
        } else {
            imageButton.setBackground(this.mContext.getResources().getDrawable(R.drawable.photopage_bottom_button_background));
        }
        imageButton.setPadding(dip2px(15), dip2px(5), dip2px(15), dip2px(5));
        imageButton.setVisibility(8);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-2, -2);
        ((ViewGroup.LayoutParams) layoutParams).height = -2;
        ((ViewGroup.LayoutParams) layoutParams).width = -2;
        layoutParams.addRule(12);
        layoutParams.addRule(1, this.mLeftButtonId);
        layoutParams.setMargins(dip2px(10), 0, 0, 0);
        imageButton.setLayoutParams(layoutParams);
        this.mContainer.addView(imageButton);
        this.mLeftButtonId = iGenerateViewId;
        return iGenerateViewId;
    }

    private int dip2px(int i) {
        if (this.mDensity == 0.0f) {
            this.mDensity = this.mContext.getResources().getDisplayMetrics().density;
        }
        return (int) ((i * this.mDensity) + 0.5f);
    }

    public void hideContainer() {
        this.mContainer.clearAnimation();
        this.mContainer.setVisibility(4);
        this.mContainerVisible = false;
    }
}
