package com.android.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.List;

public class EmergencyActionGroup extends FrameLayout implements View.OnClickListener {
    private static final long HIDE_DELAY = 3000;
    private static final int RIPPLE_DURATION = 600;
    private static final long RIPPLE_PAUSE = 1000;
    private final Interpolator mFastOutLinearInInterpolator;
    private final Runnable mHideRunnable;
    private boolean mHiding;
    private View mLastRevealed;
    private View mLaunchHint;
    private final View.OnLayoutChangeListener mLayoutChangeListener;
    private MotionEvent mPendingTouchEvent;
    private final Runnable mRippleRunnable;
    private View mRippleView;
    private ViewGroup mSelectedContainer;
    private TextView mSelectedLabel;

    public EmergencyActionGroup(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLayoutChangeListener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                EmergencyActionGroup.this.decreaseAutoSizeMinTextSize(view);
            }
        };
        this.mHideRunnable = new Runnable() {
            @Override
            public void run() {
                if (EmergencyActionGroup.this.isAttachedToWindow()) {
                    EmergencyActionGroup.this.hideTheButton();
                }
            }
        };
        this.mRippleRunnable = new Runnable() {
            @Override
            public void run() {
                if (EmergencyActionGroup.this.isAttachedToWindow()) {
                    EmergencyActionGroup.this.startRipple();
                }
            }
        };
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_linear_in);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSelectedContainer = (ViewGroup) findViewById(R.id.selected_container);
        this.mSelectedContainer.setOnClickListener(this);
        this.mSelectedLabel = (TextView) findViewById(R.id.selected_label);
        this.mSelectedLabel.addOnLayoutChangeListener(this.mLayoutChangeListener);
        this.mRippleView = findViewById(R.id.ripple_view);
        this.mLaunchHint = findViewById(R.id.launch_hint);
        this.mLaunchHint.addOnLayoutChangeListener(this.mLayoutChangeListener);
    }

    @Override
    protected void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        if (i == 0) {
            setupAssistActions();
        }
    }

    public void onPreTouchEvent(MotionEvent motionEvent) {
        this.mPendingTouchEvent = motionEvent;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        boolean zDispatchTouchEvent = super.dispatchTouchEvent(motionEvent);
        if (this.mPendingTouchEvent == motionEvent && zDispatchTouchEvent) {
            this.mPendingTouchEvent = null;
        }
        return zDispatchTouchEvent;
    }

    public void onPostTouchEvent(MotionEvent motionEvent) {
        if (this.mPendingTouchEvent != null) {
            hideTheButton();
        }
        this.mPendingTouchEvent = null;
    }

    private void setupAssistActions() {
        boolean z;
        int[] iArr = {R.id.action1, R.id.action2, R.id.action3};
        List<ResolveInfo> listResolveAssistPackageAndQueryActivites = resolveAssistPackageAndQueryActivites();
        for (int i = 0; i < 3; i++) {
            Button button = (Button) findViewById(iArr[i]);
            button.setOnClickListener(this);
            if (listResolveAssistPackageAndQueryActivites == null || listResolveAssistPackageAndQueryActivites.size() <= i || listResolveAssistPackageAndQueryActivites.get(i) == null) {
                z = false;
            } else {
                ResolveInfo resolveInfo = listResolveAssistPackageAndQueryActivites.get(i);
                button.setTag(R.id.tag_intent, new Intent("android.telephony.action.EMERGENCY_ASSISTANCE").setComponent(getComponentName(resolveInfo)));
                button.setText(resolveInfo.loadLabel(getContext().getPackageManager()));
                z = true;
            }
            button.setVisibility(z ? 0 : 8);
        }
    }

    private List<ResolveInfo> resolveAssistPackageAndQueryActivites() {
        List<ResolveInfo> listQueryAssistActivities = queryAssistActivities();
        if (listQueryAssistActivities == null || listQueryAssistActivities.isEmpty()) {
            PackageManager packageManager = getContext().getPackageManager();
            List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(new Intent("android.telephony.action.EMERGENCY_ASSISTANCE"), 0);
            PackageInfo packageInfo = null;
            for (int i = 0; i < listQueryIntentActivities.size(); i++) {
                if (listQueryIntentActivities.get(i).activityInfo != null) {
                    try {
                        PackageInfo packageInfo2 = packageManager.getPackageInfo(listQueryIntentActivities.get(i).activityInfo.packageName, 0);
                        if (isSystemApp(packageInfo2) && (packageInfo == null || packageInfo.firstInstallTime > packageInfo2.firstInstallTime)) {
                            packageInfo = packageInfo2;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
            if (packageInfo == null) {
                return null;
            }
            Settings.Secure.putString(getContext().getContentResolver(), "emergency_assistance_application", packageInfo.packageName);
            return queryAssistActivities();
        }
        return listQueryAssistActivities;
    }

    private List<ResolveInfo> queryAssistActivities() {
        String string = Settings.Secure.getString(getContext().getContentResolver(), "emergency_assistance_application");
        if (!TextUtils.isEmpty(string)) {
            return getContext().getPackageManager().queryIntentActivities(new Intent("android.telephony.action.EMERGENCY_ASSISTANCE").setPackage(string), 0);
        }
        return null;
    }

    private boolean isSystemApp(PackageInfo packageInfo) {
        return (packageInfo.applicationInfo == null || (packageInfo.applicationInfo.flags & 1) == 0) ? false : true;
    }

    private ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return null;
        }
        return new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
    }

    @Override
    public void onClick(View view) {
        Intent intent = (Intent) view.getTag(R.id.tag_intent);
        int id = view.getId();
        if (id != R.id.selected_container) {
            switch (id) {
                case R.id.action1:
                case R.id.action2:
                case R.id.action3:
                    if (AccessibilityManager.getInstance(this.mContext).isTouchExplorationEnabled()) {
                        getContext().startActivity(intent);
                    } else {
                        revealTheButton(view);
                    }
                    break;
            }
        }
        if (!this.mHiding) {
            getContext().startActivity(intent);
        }
    }

    private void revealTheButton(View view) {
        this.mSelectedLabel.setText(((Button) view).getText());
        this.mSelectedLabel.setAutoSizeTextTypeWithDefaults(1);
        this.mSelectedLabel.requestLayout();
        this.mLaunchHint.requestLayout();
        this.mSelectedContainer.setVisibility(0);
        Animator animatorCreateCircularReveal = ViewAnimationUtils.createCircularReveal(this.mSelectedContainer, view.getLeft() + (view.getWidth() / 2), view.getTop() + (view.getHeight() / 2), 0.0f, Math.max(r0, this.mSelectedContainer.getWidth() - r0) + Math.max(r1, this.mSelectedContainer.getHeight() - r1));
        animatorCreateCircularReveal.start();
        animateHintText(this.mSelectedLabel, view, animatorCreateCircularReveal);
        animateHintText(this.mLaunchHint, view, animatorCreateCircularReveal);
        this.mSelectedContainer.setTag(R.id.tag_intent, view.getTag(R.id.tag_intent));
        this.mLastRevealed = view;
        postDelayed(this.mHideRunnable, HIDE_DELAY);
        postDelayed(this.mRippleRunnable, 500L);
        this.mSelectedContainer.requestFocus();
    }

    private void decreaseAutoSizeMinTextSize(View view) {
        if (view != null && (view instanceof TextView)) {
            TextView textView = (TextView) view;
            textView.setEllipsize(TextUtils.TruncateAt.END);
            Layout layout = textView.getLayout();
            if (layout != null && layout.getEllipsisCount(textView.getMaxLines() - 1) > 0) {
                textView.setAutoSizeTextTypeUniformWithConfiguration(8, textView.getAutoSizeMaxTextSize(), textView.getAutoSizeStepGranularity(), 2);
                textView.setGravity(17);
            }
        }
    }

    private void animateHintText(View view, View view2, Animator animator) {
        view.setTranslationX(((view2.getLeft() + (view2.getWidth() / 2)) - (this.mSelectedContainer.getWidth() / 2)) / 5);
        view.animate().setDuration(animator.getDuration() / 3).setStartDelay(animator.getDuration() / 5).translationX(0.0f).setInterpolator(this.mFastOutLinearInInterpolator).start();
    }

    private void hideTheButton() {
        if (this.mHiding || this.mSelectedContainer.getVisibility() != 0) {
            return;
        }
        this.mHiding = true;
        removeCallbacks(this.mHideRunnable);
        View view = this.mLastRevealed;
        Animator animatorCreateCircularReveal = ViewAnimationUtils.createCircularReveal(this.mSelectedContainer, view.getLeft() + (view.getWidth() / 2), view.getTop() + (view.getHeight() / 2), Math.max(r1, this.mSelectedContainer.getWidth() - r1) + Math.max(r2, this.mSelectedContainer.getHeight() - r2), 0.0f);
        animatorCreateCircularReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                EmergencyActionGroup.this.mSelectedContainer.setVisibility(4);
                EmergencyActionGroup.this.removeCallbacks(EmergencyActionGroup.this.mRippleRunnable);
                EmergencyActionGroup.this.mHiding = false;
            }
        });
        animatorCreateCircularReveal.start();
        if (this.mSelectedContainer.isFocused()) {
            view.requestFocus();
        }
    }

    private void startRipple() {
        final View view = this.mRippleView;
        view.animate().cancel();
        view.setVisibility(0);
        Animator animatorCreateCircularReveal = ViewAnimationUtils.createCircularReveal(view, view.getLeft() + (view.getWidth() / 2), view.getTop() + (view.getHeight() / 2), 0.0f, view.getWidth() / 2);
        animatorCreateCircularReveal.setDuration(600L);
        animatorCreateCircularReveal.start();
        view.setAlpha(0.0f);
        view.animate().alpha(1.0f).setDuration(300L).withEndAction(new Runnable() {
            @Override
            public void run() {
                view.animate().alpha(0.0f).setDuration(300L).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        view.setVisibility(4);
                        EmergencyActionGroup.this.postDelayed(EmergencyActionGroup.this.mRippleRunnable, EmergencyActionGroup.RIPPLE_PAUSE);
                    }
                }).start();
            }
        }).start();
    }
}
