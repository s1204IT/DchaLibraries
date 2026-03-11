package com.android.systemui.statusbar.phone;

import android.R;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.tuner.TunerService;
import java.io.PrintWriter;
import java.util.ArrayList;

public class StatusBarIconController extends StatusBarIconList implements TunerService.Tunable {
    private BatteryMeterView mBatteryMeterView;
    private BatteryMeterView mBatteryMeterViewKeyguard;
    private TextView mClock;
    private Context mContext;
    private float mDarkIntensity;
    private int mDarkModeIconColorSingleTone;
    private DemoStatusIcons mDemoStatusIcons;
    private final Handler mHandler;
    private final ArraySet<String> mIconBlacklist;
    private int mIconHPadding;
    private int mIconSize;
    private int mIconTint;
    private int mLightModeIconColorSingleTone;
    private NotificationIconAreaController mNotificationIconAreaController;
    private View mNotificationIconAreaInner;
    private float mPendingDarkIntensity;
    private PhoneStatusBar mPhoneStatusBar;
    private SignalClusterView mSignalCluster;
    private LinearLayout mStatusIcons;
    private LinearLayout mStatusIconsKeyguard;
    private LinearLayout mSystemIconArea;
    private ValueAnimator mTintAnimator;
    private final Rect mTintArea;
    private boolean mTintChangePending;
    private boolean mTransitionDeferring;
    private final Runnable mTransitionDeferringDoneRunnable;
    private long mTransitionDeferringDuration;
    private long mTransitionDeferringStartTime;
    private boolean mTransitionPending;
    private static final Rect sTmpRect = new Rect();
    private static final int[] sTmpInt2 = new int[2];

    public StatusBarIconController(Context context, View statusBar, View keyguardStatusBar, PhoneStatusBar phoneStatusBar) {
        super(context.getResources().getStringArray(R.array.config_ambientDarkeningThresholds));
        this.mIconTint = -1;
        this.mTintArea = new Rect();
        this.mIconBlacklist = new ArraySet<>();
        this.mTransitionDeferringDoneRunnable = new Runnable() {
            @Override
            public void run() {
                StatusBarIconController.this.mTransitionDeferring = false;
            }
        };
        this.mContext = context;
        this.mPhoneStatusBar = phoneStatusBar;
        this.mSystemIconArea = (LinearLayout) statusBar.findViewById(com.android.systemui.R.id.system_icon_area);
        this.mStatusIcons = (LinearLayout) statusBar.findViewById(com.android.systemui.R.id.statusIcons);
        this.mSignalCluster = (SignalClusterView) statusBar.findViewById(com.android.systemui.R.id.signal_cluster);
        this.mNotificationIconAreaController = SystemUIFactory.getInstance().createNotificationIconAreaController(context, phoneStatusBar);
        this.mNotificationIconAreaInner = this.mNotificationIconAreaController.getNotificationInnerAreaView();
        ViewGroup notificationIconArea = (ViewGroup) statusBar.findViewById(com.android.systemui.R.id.notification_icon_area);
        notificationIconArea.addView(this.mNotificationIconAreaInner);
        this.mStatusIconsKeyguard = (LinearLayout) keyguardStatusBar.findViewById(com.android.systemui.R.id.statusIcons);
        this.mBatteryMeterView = (BatteryMeterView) statusBar.findViewById(com.android.systemui.R.id.battery);
        this.mBatteryMeterViewKeyguard = (BatteryMeterView) keyguardStatusBar.findViewById(com.android.systemui.R.id.battery);
        scaleBatteryMeterViews(context);
        this.mClock = (TextView) statusBar.findViewById(com.android.systemui.R.id.clock);
        this.mDarkModeIconColorSingleTone = context.getColor(com.android.systemui.R.color.dark_mode_icon_color_single_tone);
        this.mLightModeIconColorSingleTone = context.getColor(com.android.systemui.R.color.light_mode_icon_color_single_tone);
        this.mHandler = new Handler();
        loadDimens();
        TunerService.get(this.mContext).addTunable(this, "icon_blacklist");
    }

    public void setSignalCluster(SignalClusterView signalCluster) {
        this.mSignalCluster = signalCluster;
    }

    private void scaleBatteryMeterViews(Context context) {
        Resources res = context.getResources();
        TypedValue typedValue = new TypedValue();
        res.getValue(com.android.systemui.R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float iconScaleFactor = typedValue.getFloat();
        int batteryHeight = res.getDimensionPixelSize(com.android.systemui.R.dimen.status_bar_battery_icon_height);
        int batteryWidth = res.getDimensionPixelSize(com.android.systemui.R.dimen.status_bar_battery_icon_width);
        int marginBottom = res.getDimensionPixelSize(com.android.systemui.R.dimen.battery_margin_bottom);
        LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams((int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
        scaledLayoutParams.setMarginsRelative(0, 0, 0, marginBottom);
        this.mBatteryMeterView.setLayoutParams(scaledLayoutParams);
        this.mBatteryMeterViewKeyguard.setLayoutParams(scaledLayoutParams);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!"icon_blacklist".equals(key)) {
            return;
        }
        this.mIconBlacklist.clear();
        this.mIconBlacklist.addAll((ArraySet<? extends String>) getIconBlacklist(newValue));
        ArrayList<StatusBarIconView> views = new ArrayList<>();
        for (int i = 0; i < this.mStatusIcons.getChildCount(); i++) {
            views.add((StatusBarIconView) this.mStatusIcons.getChildAt(i));
        }
        for (int i2 = views.size() - 1; i2 >= 0; i2--) {
            removeIcon(views.get(i2).getSlot());
        }
        for (int i3 = 0; i3 < views.size(); i3++) {
            setIcon(views.get(i3).getSlot(), views.get(i3).getStatusBarIcon());
        }
    }

    private void loadDimens() {
        this.mIconSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.action_bar_default_height_material);
        this.mIconHPadding = this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.status_bar_icon_padding);
    }

    private void addSystemIcon(int index, StatusBarIcon icon) {
        String slot = getSlot(index);
        int viewIndex = getViewIndex(index);
        boolean blocked = this.mIconBlacklist.contains(slot);
        StatusBarIconView view = new StatusBarIconView(this.mContext, slot, null, blocked);
        view.set(icon);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, this.mIconSize);
        lp.setMargins(this.mIconHPadding, 0, this.mIconHPadding, 0);
        this.mStatusIcons.addView(view, viewIndex, lp);
        StatusBarIconView view2 = new StatusBarIconView(this.mContext, slot, null, blocked);
        view2.set(icon);
        this.mStatusIconsKeyguard.addView(view2, viewIndex, new LinearLayout.LayoutParams(-2, this.mIconSize));
        applyIconTint();
    }

    public void setIcon(String slot, int resourceId, CharSequence contentDescription) {
        int index = getSlotIndex(slot);
        StatusBarIcon icon = getIcon(index);
        if (icon == null) {
            setIcon(slot, new StatusBarIcon(UserHandle.SYSTEM, this.mContext.getPackageName(), Icon.createWithResource(this.mContext, resourceId), 0, 0, contentDescription));
            return;
        }
        icon.icon = Icon.createWithResource(this.mContext, resourceId);
        icon.contentDescription = contentDescription;
        handleSet(index, icon);
    }

    public void setExternalIcon(String slot) {
        int viewIndex = getViewIndex(getSlotIndex(slot));
        int height = this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.status_bar_icon_drawing_size);
        ImageView imageView = (ImageView) this.mStatusIcons.getChildAt(viewIndex);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        setHeightAndCenter(imageView, height);
        ImageView imageView2 = (ImageView) this.mStatusIconsKeyguard.getChildAt(viewIndex);
        imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView2.setAdjustViewBounds(true);
        setHeightAndCenter(imageView2, height);
    }

    private void setHeightAndCenter(ImageView imageView, int height) {
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        params.height = height;
        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).gravity = 16;
        }
        imageView.setLayoutParams(params);
    }

    public void setIcon(String slot, StatusBarIcon icon) {
        setIcon(getSlotIndex(slot), icon);
    }

    public void removeIcon(String slot) {
        int index = getSlotIndex(slot);
        removeIcon(index);
    }

    public void setIconVisibility(String slot, boolean visibility) {
        int index = getSlotIndex(slot);
        StatusBarIcon icon = getIcon(index);
        if (icon == null || icon.visible == visibility) {
            return;
        }
        icon.visible = visibility;
        handleSet(index, icon);
    }

    @Override
    public void removeIcon(int index) {
        if (getIcon(index) == null) {
            return;
        }
        super.removeIcon(index);
        int viewIndex = getViewIndex(index);
        this.mStatusIcons.removeViewAt(viewIndex);
        this.mStatusIconsKeyguard.removeViewAt(viewIndex);
    }

    @Override
    public void setIcon(int index, StatusBarIcon icon) {
        if (icon == null) {
            removeIcon(index);
            return;
        }
        boolean isNew = getIcon(index) == null;
        super.setIcon(index, icon);
        if (isNew) {
            addSystemIcon(index, icon);
        } else {
            handleSet(index, icon);
        }
    }

    private void handleSet(int index, StatusBarIcon icon) {
        int viewIndex = getViewIndex(index);
        StatusBarIconView view = (StatusBarIconView) this.mStatusIcons.getChildAt(viewIndex);
        view.set(icon);
        StatusBarIconView view2 = (StatusBarIconView) this.mStatusIconsKeyguard.getChildAt(viewIndex);
        view2.set(icon);
        applyIconTint();
    }

    public void updateNotificationIcons(NotificationData notificationData) {
        this.mNotificationIconAreaController.updateNotificationIcons(notificationData);
    }

    public void hideSystemIconArea(boolean animate) {
        animateHide(this.mSystemIconArea, animate);
    }

    public void showSystemIconArea(boolean animate) {
        animateShow(this.mSystemIconArea, animate);
    }

    public void hideNotificationIconArea(boolean animate) {
        animateHide(this.mNotificationIconAreaInner, animate);
    }

    public void showNotificationIconArea(boolean animate) {
        animateShow(this.mNotificationIconAreaInner, animate);
    }

    public void setClockVisibility(boolean visible) {
        this.mClock.setVisibility(visible ? 0 : 8);
    }

    public void dump(PrintWriter pw) {
        int N = this.mStatusIcons.getChildCount();
        pw.println("  system icons: " + N);
        for (int i = 0; i < N; i++) {
            StatusBarIconView ic = (StatusBarIconView) this.mStatusIcons.getChildAt(i);
            pw.println("    [" + i + "] icon=" + ic);
        }
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        if (this.mDemoStatusIcons == null) {
            this.mDemoStatusIcons = new DemoStatusIcons(this.mStatusIcons, this.mIconSize);
        }
        this.mDemoStatusIcons.dispatchDemoCommand(command, args);
    }

    private void animateHide(final View v, boolean animate) {
        v.animate().cancel();
        if (animate) {
            v.animate().alpha(0.0f).setDuration(160L).setStartDelay(0L).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
                @Override
                public void run() {
                    v.setVisibility(4);
                }
            });
        } else {
            v.setAlpha(0.0f);
            v.setVisibility(4);
        }
    }

    private void animateShow(View v, boolean animate) {
        v.animate().cancel();
        v.setVisibility(0);
        if (!animate) {
            v.setAlpha(1.0f);
            return;
        }
        v.animate().alpha(1.0f).setDuration(320L).setInterpolator(Interpolators.ALPHA_IN).setStartDelay(50L).withEndAction(null);
        if (this.mPhoneStatusBar.isKeyguardFadingAway()) {
            v.animate().setDuration(this.mPhoneStatusBar.getKeyguardFadingAwayDuration()).setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).setStartDelay(this.mPhoneStatusBar.getKeyguardFadingAwayDelay()).start();
        }
    }

    public void setIconsDarkArea(Rect darkArea) {
        if (darkArea == null && this.mTintArea.isEmpty()) {
            return;
        }
        if (darkArea == null) {
            this.mTintArea.setEmpty();
        } else {
            this.mTintArea.set(darkArea);
        }
        applyIconTint();
        this.mNotificationIconAreaController.setTintArea(darkArea);
    }

    public void setIconsDark(boolean dark, boolean animate) {
        if (!animate) {
            setIconTintInternal(dark ? 1.0f : 0.0f);
            return;
        }
        if (this.mTransitionPending) {
            deferIconTintChange(dark ? 1.0f : 0.0f);
        } else if (this.mTransitionDeferring) {
            animateIconTint(dark ? 1.0f : 0.0f, Math.max(0L, this.mTransitionDeferringStartTime - SystemClock.uptimeMillis()), this.mTransitionDeferringDuration);
        } else {
            animateIconTint(dark ? 1.0f : 0.0f, 0L, 120L);
        }
    }

    private void animateIconTint(float targetDarkIntensity, long delay, long duration) {
        if (this.mTintAnimator != null) {
            this.mTintAnimator.cancel();
        }
        if (this.mDarkIntensity == targetDarkIntensity) {
            return;
        }
        this.mTintAnimator = ValueAnimator.ofFloat(this.mDarkIntensity, targetDarkIntensity);
        this.mTintAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                StatusBarIconController.this.setIconTintInternal(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        this.mTintAnimator.setDuration(duration);
        this.mTintAnimator.setStartDelay(delay);
        this.mTintAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        this.mTintAnimator.start();
    }

    public void setIconTintInternal(float darkIntensity) {
        this.mDarkIntensity = darkIntensity;
        this.mIconTint = ((Integer) ArgbEvaluator.getInstance().evaluate(darkIntensity, Integer.valueOf(this.mLightModeIconColorSingleTone), Integer.valueOf(this.mDarkModeIconColorSingleTone))).intValue();
        this.mNotificationIconAreaController.setIconTint(this.mIconTint);
        applyIconTint();
    }

    private void deferIconTintChange(float darkIntensity) {
        if (this.mTintChangePending && darkIntensity == this.mPendingDarkIntensity) {
            return;
        }
        this.mTintChangePending = true;
        this.mPendingDarkIntensity = darkIntensity;
    }

    public static int getTint(Rect tintArea, View view, int color) {
        if (isInArea(tintArea, view)) {
            return color;
        }
        return -1;
    }

    public static float getDarkIntensity(Rect tintArea, View view, float intensity) {
        if (isInArea(tintArea, view)) {
            return intensity;
        }
        return 0.0f;
    }

    private static boolean isInArea(Rect area, View view) {
        if (area.isEmpty()) {
            return true;
        }
        sTmpRect.set(area);
        view.getLocationOnScreen(sTmpInt2);
        int left = sTmpInt2[0];
        int intersectStart = Math.max(left, area.left);
        int intersectEnd = Math.min(view.getWidth() + left, area.right);
        int intersectAmount = Math.max(0, intersectEnd - intersectStart);
        boolean coversFullStatusBar = area.top <= 0;
        boolean majorityOfWidth = intersectAmount * 2 > view.getWidth();
        if (majorityOfWidth) {
            return coversFullStatusBar;
        }
        return false;
    }

    private void applyIconTint() {
        for (int i = 0; i < this.mStatusIcons.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) this.mStatusIcons.getChildAt(i);
            v.setImageTintList(ColorStateList.valueOf(getTint(this.mTintArea, v, this.mIconTint)));
        }
        this.mSignalCluster.setIconTint(this.mIconTint, this.mDarkIntensity, this.mTintArea);
        this.mBatteryMeterView.setDarkIntensity(isInArea(this.mTintArea, this.mBatteryMeterView) ? this.mDarkIntensity : 0.0f);
        this.mClock.setTextColor(getTint(this.mTintArea, this.mClock, this.mIconTint));
    }

    public void appTransitionPending() {
        this.mTransitionPending = true;
    }

    public void appTransitionCancelled() {
        if (this.mTransitionPending && this.mTintChangePending) {
            this.mTintChangePending = false;
            animateIconTint(this.mPendingDarkIntensity, 0L, 120L);
        }
        this.mTransitionPending = false;
    }

    public void appTransitionStarting(long startTime, long duration) {
        if (this.mTransitionPending && this.mTintChangePending) {
            this.mTintChangePending = false;
            animateIconTint(this.mPendingDarkIntensity, Math.max(0L, startTime - SystemClock.uptimeMillis()), duration);
        } else if (this.mTransitionPending) {
            this.mTransitionDeferring = true;
            this.mTransitionDeferringStartTime = startTime;
            this.mTransitionDeferringDuration = duration;
            this.mHandler.removeCallbacks(this.mTransitionDeferringDoneRunnable);
            this.mHandler.postAtTime(this.mTransitionDeferringDoneRunnable, startTime);
        }
        this.mTransitionPending = false;
    }

    public static ArraySet<String> getIconBlacklist(String blackListStr) {
        ArraySet<String> ret = new ArraySet<>();
        if (blackListStr == null) {
            blackListStr = "rotate,";
        }
        String[] blacklist = blackListStr.split(",");
        for (String slot : blacklist) {
            if (!TextUtils.isEmpty(slot)) {
                ret.add(slot);
            }
        }
        return ret;
    }

    public void onDensityOrFontScaleChanged() {
        loadDimens();
        this.mNotificationIconAreaController.onDensityOrFontScaleChanged(this.mContext);
        updateClock();
        for (int i = 0; i < this.mStatusIcons.getChildCount(); i++) {
            View child = this.mStatusIcons.getChildAt(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, this.mIconSize);
            lp.setMargins(this.mIconHPadding, 0, this.mIconHPadding, 0);
            child.setLayoutParams(lp);
        }
        for (int i2 = 0; i2 < this.mStatusIconsKeyguard.getChildCount(); i2++) {
            View child2 = this.mStatusIconsKeyguard.getChildAt(i2);
            child2.setLayoutParams(new LinearLayout.LayoutParams(-2, this.mIconSize));
        }
        scaleBatteryMeterViews(this.mContext);
    }

    private void updateClock() {
        FontSizeUtils.updateFontSize(this.mClock, com.android.systemui.R.dimen.status_bar_clock_size);
        this.mClock.setPaddingRelative(this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.status_bar_clock_starting_padding), 0, this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.status_bar_clock_end_padding), 0);
    }
}
