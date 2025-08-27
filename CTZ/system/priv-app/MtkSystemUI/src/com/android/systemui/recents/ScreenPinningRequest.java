package com.android.systemui.recents;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Binder;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settingslib.Utils;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.R;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.util.leak.RotationUtils;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class ScreenPinningRequest implements View.OnClickListener {
    private final AccessibilityManager mAccessibilityService;
    private final Context mContext;
    private RequestWindowView mRequestWindow;
    private final WindowManager mWindowManager;
    private int taskId;

    public ScreenPinningRequest(Context context) {
        this.mContext = context;
        this.mAccessibilityService = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
    }

    public void clearPrompt() {
        if (this.mRequestWindow != null) {
            this.mWindowManager.removeView(this.mRequestWindow);
            this.mRequestWindow = null;
        }
    }

    public void showPrompt(int i, boolean z) {
        try {
            clearPrompt();
        } catch (IllegalArgumentException e) {
        }
        this.taskId = i;
        this.mRequestWindow = new RequestWindowView(this.mContext, z);
        this.mRequestWindow.setSystemUiVisibility(256);
        this.mWindowManager.addView(this.mRequestWindow, getWindowLayoutParams());
    }

    public void onConfigurationChanged() {
        if (this.mRequestWindow != null) {
            this.mRequestWindow.onConfigurationChanged();
        }
    }

    private WindowManager.LayoutParams getWindowLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2024, 264, -3);
        layoutParams.token = new Binder();
        layoutParams.privateFlags |= 16;
        layoutParams.setTitle("ScreenPinningConfirmation");
        layoutParams.gravity = R.styleable.AppCompatTheme_windowMinWidthMinor;
        return layoutParams;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view.getId() == com.android.systemui.R.id.screen_pinning_ok_button || this.mRequestWindow == view) {
            try {
                ActivityManager.getService().startSystemLockTaskMode(this.taskId);
            } catch (RemoteException e) {
            }
        }
        clearPrompt();
    }

    public FrameLayout.LayoutParams getRequestLayoutParams(int i) {
        int i2;
        if (i == 2) {
            i2 = 19;
        } else {
            i2 = i == 1 ? 21 : 81;
        }
        return new FrameLayout.LayoutParams(-2, -2, i2);
    }

    private class RequestWindowView extends FrameLayout {
        private final ColorDrawable mColor;
        private ValueAnimator mColorAnim;
        private ViewGroup mLayout;
        private final BroadcastReceiver mReceiver;
        private boolean mShowCancel;
        private final Runnable mUpdateLayoutRunnable;

        public RequestWindowView(Context context, boolean z) {
            super(context);
            this.mColor = new ColorDrawable(0);
            this.mUpdateLayoutRunnable = new Runnable() { // from class: com.android.systemui.recents.ScreenPinningRequest.RequestWindowView.2
                @Override // java.lang.Runnable
                public void run() {
                    if (RequestWindowView.this.mLayout != null && RequestWindowView.this.mLayout.getParent() != null) {
                        RequestWindowView.this.mLayout.setLayoutParams(ScreenPinningRequest.this.getRequestLayoutParams(RotationUtils.getRotation(RequestWindowView.this.mContext)));
                    }
                }
            };
            this.mReceiver = new BroadcastReceiver() { // from class: com.android.systemui.recents.ScreenPinningRequest.RequestWindowView.3
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context2, Intent intent) {
                    if (intent.getAction().equals("android.intent.action.CONFIGURATION_CHANGED")) {
                        RequestWindowView.this.post(RequestWindowView.this.mUpdateLayoutRunnable);
                    } else if (intent.getAction().equals("android.intent.action.USER_SWITCHED") || intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                        ScreenPinningRequest.this.clearPrompt();
                    }
                }
            };
            setClickable(true);
            setOnClickListener(ScreenPinningRequest.this);
            setBackground(this.mColor);
            this.mShowCancel = z;
        }

        @Override // android.view.ViewGroup, android.view.View
        public void onAttachedToWindow() {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ScreenPinningRequest.this.mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
            float f = displayMetrics.density;
            int rotation = RotationUtils.getRotation(this.mContext);
            inflateView(rotation);
            int color = this.mContext.getColor(com.android.systemui.R.color.screen_pinning_request_window_bg);
            if (ActivityManager.isHighEndGfx()) {
                this.mLayout.setAlpha(0.0f);
                if (rotation != 2) {
                    if (rotation == 1) {
                        this.mLayout.setTranslationX(96.0f * f);
                    } else {
                        this.mLayout.setTranslationY(96.0f * f);
                    }
                } else {
                    this.mLayout.setTranslationX((-96.0f) * f);
                }
                this.mLayout.animate().alpha(1.0f).translationX(0.0f).translationY(0.0f).setDuration(300L).setInterpolator(new DecelerateInterpolator()).start();
                this.mColorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), 0, Integer.valueOf(color));
                this.mColorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.recents.ScreenPinningRequest.RequestWindowView.1
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        RequestWindowView.this.mColor.setColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
                    }
                });
                this.mColorAnim.setDuration(1000L);
                this.mColorAnim.start();
            } else {
                this.mColor.setColor(color);
            }
            IntentFilter intentFilter = new IntentFilter("android.intent.action.CONFIGURATION_CHANGED");
            intentFilter.addAction("android.intent.action.USER_SWITCHED");
            intentFilter.addAction("android.intent.action.SCREEN_OFF");
            this.mContext.registerReceiver(this.mReceiver, intentFilter);
        }

        private void inflateView(int i) {
            int i2;
            int i3;
            Context context = getContext();
            boolean z = true;
            if (i == 2) {
                i2 = com.android.systemui.R.layout.screen_pinning_request_sea_phone;
            } else {
                i2 = i == 1 ? com.android.systemui.R.layout.screen_pinning_request_land_phone : com.android.systemui.R.layout.screen_pinning_request;
            }
            this.mLayout = (ViewGroup) View.inflate(context, i2, null);
            this.mLayout.setClickable(true);
            this.mLayout.setLayoutDirection(0);
            this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_text_area).setLayoutDirection(3);
            View viewFindViewById = this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_buttons);
            if (Recents.getSystemServices() != null && Recents.getSystemServices().hasSoftNavigationBar()) {
                viewFindViewById.setLayoutDirection(3);
                swapChildrenIfRtlAndVertical(viewFindViewById);
            } else {
                viewFindViewById.setVisibility(8);
            }
            ((Button) this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_ok_button)).setOnClickListener(ScreenPinningRequest.this);
            if (this.mShowCancel) {
                ((Button) this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_cancel_button)).setOnClickListener(ScreenPinningRequest.this);
            } else {
                ((Button) this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_cancel_button)).setVisibility(4);
            }
            StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
            NavigationBarView navigationBarView = statusBar != null ? statusBar.getNavigationBarView() : null;
            if (navigationBarView == null || !navigationBarView.isRecentsButtonVisible()) {
                z = false;
            }
            boolean zIsTouchExplorationEnabled = ScreenPinningRequest.this.mAccessibilityService.isTouchExplorationEnabled();
            if (z) {
                this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_recents_group).setVisibility(0);
                this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_home_bg_light).setVisibility(4);
                this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_home_bg).setVisibility(4);
                if (zIsTouchExplorationEnabled) {
                    i3 = com.android.systemui.R.string.screen_pinning_description_accessible;
                } else {
                    i3 = com.android.systemui.R.string.screen_pinning_description;
                }
            } else {
                this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_recents_group).setVisibility(4);
                this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_home_bg_light).setVisibility(0);
                this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_home_bg).setVisibility(0);
                if (zIsTouchExplorationEnabled) {
                    i3 = com.android.systemui.R.string.screen_pinning_description_recents_invisible_accessible;
                } else {
                    i3 = com.android.systemui.R.string.screen_pinning_description_recents_invisible;
                }
            }
            if (navigationBarView != null) {
                int themeAttr = Utils.getThemeAttr(getContext(), com.android.systemui.R.attr.darkIconTheme);
                ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(getContext(), Utils.getThemeAttr(getContext(), com.android.systemui.R.attr.lightIconTheme));
                ContextThemeWrapper contextThemeWrapper2 = new ContextThemeWrapper(getContext(), themeAttr);
                ((ImageView) this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_back_icon)).setImageDrawable(navigationBarView.getBackDrawable(contextThemeWrapper, contextThemeWrapper2));
                ((ImageView) this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_home_icon)).setImageDrawable(navigationBarView.getHomeDrawable(contextThemeWrapper, contextThemeWrapper2));
            }
            ((TextView) this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_description)).setText(i3);
            int i4 = zIsTouchExplorationEnabled ? 4 : 0;
            this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_back_bg).setVisibility(i4);
            this.mLayout.findViewById(com.android.systemui.R.id.screen_pinning_back_bg_light).setVisibility(i4);
            addView(this.mLayout, ScreenPinningRequest.this.getRequestLayoutParams(i));
        }

        private void swapChildrenIfRtlAndVertical(View view) {
            if (this.mContext.getResources().getConfiguration().getLayoutDirection() != 1) {
                return;
            }
            LinearLayout linearLayout = (LinearLayout) view;
            if (linearLayout.getOrientation() == 1) {
                int childCount = linearLayout.getChildCount();
                ArrayList arrayList = new ArrayList(childCount);
                for (int i = 0; i < childCount; i++) {
                    arrayList.add(linearLayout.getChildAt(i));
                }
                linearLayout.removeAllViews();
                for (int i2 = childCount - 1; i2 >= 0; i2--) {
                    linearLayout.addView((View) arrayList.get(i2));
                }
            }
        }

        @Override // android.view.ViewGroup, android.view.View
        public void onDetachedFromWindow() {
            this.mContext.unregisterReceiver(this.mReceiver);
        }

        protected void onConfigurationChanged() {
            removeAllViews();
            inflateView(RotationUtils.getRotation(this.mContext));
        }
    }
}
