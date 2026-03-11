package com.android.systemui.statusbar.phone;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.DockedStackExistsListener;
import com.android.systemui.OverviewProxyService;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.plugins.statusbar.phone.NavGesture;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsOnboarding;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;
import com.android.systemui.statusbar.policy.TintedKeyButtonDrawable;
import com.mediatek.systemui.ext.INavigationBarPlugin;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class NavigationBarView extends FrameLayout implements PluginListener<NavGesture> {
    private KeyButtonDrawable mAccessibilityIcon;
    private KeyButtonDrawable mBackAltCarModeIcon;
    private KeyButtonDrawable mBackAltLandCarModeIcon;
    private Rect mBackButtonBounds;
    private KeyButtonDrawable mBackCarModeIcon;
    private KeyButtonDrawable mBackIcon;
    private KeyButtonDrawable mBackLandCarModeIcon;
    private final NavigationBarTransitions mBarTransitions;
    private final SparseArray<ButtonDispatcher> mButtonDispatchers;
    private Configuration mConfiguration;
    private int mCurrentRotation;
    View mCurrentView;
    private final DeadZone mDeadZone;
    private boolean mDeadZoneConsuming;
    int mDisabledFlags;
    final Display mDisplay;
    private Divider mDivider;
    private KeyButtonDrawable mDockedIcon;
    private final Consumer<Boolean> mDockedListener;
    private boolean mDockedStackExists;
    private int mDownHitTarget;
    private NavGesture.GestureHelper mGestureHelper;
    private H mHandler;
    private Rect mHomeButtonBounds;
    private KeyButtonDrawable mHomeCarModeIcon;
    private KeyButtonDrawable mHomeDefaultIcon;
    private KeyButtonDrawable mImeIcon;
    private final View.OnClickListener mImeSwitcherClickListener;
    private boolean mInCarMode;
    private boolean mLayoutTransitionsEnabled;
    boolean mLongClickableAccessibilityButton;
    private KeyButtonDrawable mMenuIcon;
    private INavigationBarPlugin mNavBarPlugin;
    int mNavigationIconHints;
    private NavigationBarInflaterView mNavigationInflaterView;
    private OnVerticalChangedListener mOnVerticalChangedListener;
    private final OverviewProxyService mOverviewProxyService;
    private NotificationPanelView mPanelView;
    private final View.AccessibilityDelegate mQuickStepAccessibilityDelegate;
    private KeyButtonDrawable mRecentIcon;
    private Rect mRecentsButtonBounds;
    private RecentsComponent mRecentsComponent;
    private RecentsOnboarding mRecentsOnboarding;
    private int mRotateBtnStyle;
    private TintedKeyButtonDrawable mRotateSuggestionIcon;
    View[] mRotatedViews;
    private Rect mRotationButtonBounds;
    boolean mShowAccessibilityButton;
    boolean mShowMenu;
    boolean mShowRotateButton;
    private OpSystemUICustomizationFactoryBase mSystemUICustomizationFactory;
    private int[] mTmpPosition;
    private Rect mTmpRect;
    private final NavTransitionListener mTransitionListener;
    private boolean mUseCarModeUi;
    boolean mVertical;
    private boolean mWakeAndUnlocking;

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean z);
    }

    private class NavTransitionListener implements LayoutTransition.TransitionListener {
        private boolean mBackTransitioning;
        private long mDuration;
        private boolean mHomeAppearing;
        private TimeInterpolator mInterpolator;
        private long mStartDelay;

        private NavTransitionListener() {
        }

        @Override
        public void startTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
            if (view.getId() == R.id.back) {
                this.mBackTransitioning = true;
                return;
            }
            if (view.getId() == R.id.home && i == 2) {
                this.mHomeAppearing = true;
                this.mStartDelay = layoutTransition.getStartDelay(i);
                this.mDuration = layoutTransition.getDuration(i);
                this.mInterpolator = layoutTransition.getInterpolator(i);
            }
        }

        @Override
        public void endTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
            if (view.getId() == R.id.back) {
                this.mBackTransitioning = false;
            } else if (view.getId() == R.id.home && i == 2) {
                this.mHomeAppearing = false;
            }
        }

        public void onBackAltCleared() {
            ButtonDispatcher backButton = NavigationBarView.this.getBackButton();
            if (!this.mBackTransitioning && backButton.getVisibility() == 0 && this.mHomeAppearing && NavigationBarView.this.getHomeButton().getAlpha() == 0.0f) {
                NavigationBarView.this.getBackButton().setAlpha(0.0f);
                ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(backButton, "alpha", 0.0f, 1.0f);
                objectAnimatorOfFloat.setStartDelay(this.mStartDelay);
                objectAnimatorOfFloat.setDuration(this.mDuration);
                objectAnimatorOfFloat.setInterpolator(this.mInterpolator);
                objectAnimatorOfFloat.start();
            }
        }
    }

    class H extends Handler {
        private H() {
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 8686) {
                String str = "" + message.obj;
                int width = NavigationBarView.this.getWidth();
                int height = NavigationBarView.this.getHeight();
                int width2 = NavigationBarView.this.getCurrentView().getWidth();
                int height2 = NavigationBarView.this.getCurrentView().getHeight();
                if (height != height2 || width != width2) {
                    Log.w("StatusBar/NavBarView", String.format("*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)", str, Integer.valueOf(width), Integer.valueOf(height), Integer.valueOf(width2), Integer.valueOf(height2)));
                    NavigationBarView.this.requestLayout();
                }
            }
        }
    }

    public NavigationBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentView = null;
        this.mRotatedViews = new View[4];
        this.mCurrentRotation = -1;
        this.mDisabledFlags = 0;
        this.mNavigationIconHints = 0;
        this.mDownHitTarget = 0;
        this.mHomeButtonBounds = new Rect();
        this.mBackButtonBounds = new Rect();
        this.mRecentsButtonBounds = new Rect();
        this.mRotationButtonBounds = new Rect();
        this.mTmpPosition = new int[2];
        this.mTmpRect = new Rect();
        this.mDeadZoneConsuming = false;
        this.mTransitionListener = new NavTransitionListener();
        this.mLayoutTransitionsEnabled = true;
        this.mUseCarModeUi = false;
        this.mInCarMode = false;
        this.mButtonDispatchers = new SparseArray<>();
        this.mRotateBtnStyle = R.style.RotateButtonCCWStart90;
        this.mSystemUICustomizationFactory = null;
        this.mImeSwitcherClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((InputMethodManager) NavigationBarView.this.mContext.getSystemService(InputMethodManager.class)).showInputMethodPicker(true);
            }
        };
        this.mQuickStepAccessibilityDelegate = new View.AccessibilityDelegate() {
            private AccessibilityNodeInfo.AccessibilityAction mToggleOverviewAction;

            @Override
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
                if (this.mToggleOverviewAction == null) {
                    this.mToggleOverviewAction = new AccessibilityNodeInfo.AccessibilityAction(R.id.action_toggle_overview, NavigationBarView.this.getContext().getString(R.string.quick_step_accessibility_toggle_overview));
                }
                accessibilityNodeInfo.addAction(this.mToggleOverviewAction);
            }

            @Override
            public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
                if (i == R.id.action_toggle_overview) {
                    ((Recents) SysUiServiceProvider.getComponent(NavigationBarView.this.getContext(), Recents.class)).toggleRecentApps();
                    return true;
                }
                return super.performAccessibilityAction(view, i, bundle);
            }
        };
        this.mHandler = new H();
        this.mDockedListener = new Consumer() {
            @Override
            public final void accept(Object obj) {
                NavigationBarView navigationBarView = this.f$0;
                navigationBarView.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        NavigationBarView.lambda$new$0(this.f$0, bool);
                    }
                });
            }
        };
        this.mDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        this.mVertical = false;
        this.mShowMenu = false;
        this.mShowAccessibilityButton = false;
        this.mLongClickableAccessibilityButton = false;
        this.mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        this.mRecentsOnboarding = new RecentsOnboarding(context, this.mOverviewProxyService);
        this.mConfiguration = new Configuration();
        this.mConfiguration.updateFrom(context.getResources().getConfiguration());
        try {
            this.mSystemUICustomizationFactory = OpSystemUICustomizationFactoryBase.getOpFactory(context);
            this.mNavBarPlugin = this.mSystemUICustomizationFactory.makeNavigationBar(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        reloadNavIcons();
        this.mBarTransitions = new NavigationBarTransitions(this);
        this.mButtonDispatchers.put(R.id.back, new ButtonDispatcher(R.id.back));
        this.mButtonDispatchers.put(R.id.home, new ButtonDispatcher(R.id.home));
        this.mButtonDispatchers.put(R.id.recent_apps, new ButtonDispatcher(R.id.recent_apps));
        this.mButtonDispatchers.put(R.id.menu, new ButtonDispatcher(R.id.menu));
        this.mButtonDispatchers.put(R.id.ime_switcher, new ButtonDispatcher(R.id.ime_switcher));
        this.mButtonDispatchers.put(R.id.accessibility_button, new ButtonDispatcher(R.id.accessibility_button));
        this.mButtonDispatchers.put(R.id.rotate_suggestion, new ButtonDispatcher(R.id.rotate_suggestion));
        this.mButtonDispatchers.put(R.id.menu_container, new ButtonDispatcher(R.id.menu_container));
        this.mDeadZone = new DeadZone(this);
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("dcha_state"), false, new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z) {
                NavigationBarView.this.updateNavButtonIcons();
            }
        }, -1);
    }

    public BarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public LightBarTransitionsController getLightTransitionsController() {
        return this.mBarTransitions.getLightTransitionsController();
    }

    public void setComponents(RecentsComponent recentsComponent, Divider divider, NotificationPanelView notificationPanelView) {
        this.mRecentsComponent = recentsComponent;
        this.mDivider = divider;
        this.mPanelView = notificationPanelView;
        if (this.mGestureHelper instanceof NavigationBarGestureHelper) {
            ((NavigationBarGestureHelper) this.mGestureHelper).setComponents(recentsComponent, divider, this);
        }
    }

    public void setOnVerticalChangedListener(OnVerticalChangedListener onVerticalChangedListener) {
        this.mOnVerticalChangedListener = onVerticalChangedListener;
        notifyVerticalChangedListener(this.mVertical);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (shouldDeadZoneConsumeTouchEvents(motionEvent)) {
            return true;
        }
        if (motionEvent.getActionMasked() == 0) {
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();
            this.mDownHitTarget = 0;
            if (getBackButton().isVisible() && this.mBackButtonBounds.contains(x, y)) {
                this.mDownHitTarget = 1;
            } else if (getHomeButton().isVisible() && this.mHomeButtonBounds.contains(x, y)) {
                this.mDownHitTarget = 2;
            } else if (getRecentsButton().isVisible() && this.mRecentsButtonBounds.contains(x, y)) {
                this.mDownHitTarget = 3;
            } else if (getRotateSuggestionButton().isVisible() && this.mRotationButtonBounds.contains(x, y)) {
                this.mDownHitTarget = 4;
            }
        }
        return this.mGestureHelper.onInterceptTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (shouldDeadZoneConsumeTouchEvents(motionEvent) || this.mGestureHelper.onTouchEvent(motionEvent)) {
            return true;
        }
        return super.onTouchEvent(motionEvent);
    }

    private boolean shouldDeadZoneConsumeTouchEvents(MotionEvent motionEvent) {
        if (!this.mDeadZone.onTouchEvent(motionEvent) && !this.mDeadZoneConsuming) {
            return false;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 3) {
            switch (actionMasked) {
                case 0:
                    setSlippery(true);
                    this.mDeadZoneConsuming = true;
                    break;
                case 1:
                    updateSlippery();
                    this.mDeadZoneConsuming = false;
                    break;
            }
        }
        return true;
    }

    public int getDownHitTarget() {
        return this.mDownHitTarget;
    }

    public void abortCurrentGesture() {
        getHomeButton().abortCurrentGesture();
    }

    public View getCurrentView() {
        return this.mCurrentView;
    }

    public ButtonDispatcher getRecentsButton() {
        return this.mButtonDispatchers.get(R.id.recent_apps);
    }

    public ButtonDispatcher getMenuButton() {
        return this.mButtonDispatchers.get(R.id.menu);
    }

    public ButtonDispatcher getBackButton() {
        return this.mButtonDispatchers.get(R.id.back);
    }

    public ButtonDispatcher getHomeButton() {
        return this.mButtonDispatchers.get(R.id.home);
    }

    public ButtonDispatcher getImeSwitchButton() {
        return this.mButtonDispatchers.get(R.id.ime_switcher);
    }

    public ButtonDispatcher getAccessibilityButton() {
        return this.mButtonDispatchers.get(R.id.accessibility_button);
    }

    public ButtonDispatcher getRotateSuggestionButton() {
        return this.mButtonDispatchers.get(R.id.rotate_suggestion);
    }

    public ButtonDispatcher getMenuContainer() {
        return this.mButtonDispatchers.get(R.id.menu_container);
    }

    public SparseArray<ButtonDispatcher> getButtonDispatchers() {
        return this.mButtonDispatchers;
    }

    public boolean isRecentsButtonVisible() {
        return getRecentsButton().getVisibility() == 0;
    }

    public boolean isOverviewEnabled() {
        return (this.mDisabledFlags & 16777216) == 0;
    }

    public boolean isQuickStepSwipeUpEnabled() {
        return this.mOverviewProxyService.shouldShowSwipeUpUI() && isOverviewEnabled();
    }

    public boolean isQuickScrubEnabled() {
        return SystemProperties.getBoolean("persist.quickstep.scrub.enabled", true) && this.mOverviewProxyService.isEnabled() && isOverviewEnabled() && (this.mOverviewProxyService.getInteractionFlags() & 2) == 0;
    }

    private void reloadNavIcons() {
        updateIcons(this.mContext, Configuration.EMPTY, this.mConfiguration);
    }

    private void updateIcons(Context context, Configuration configuration, Configuration configuration2) {
        int themeAttr = Utils.getThemeAttr(context, R.attr.darkIconTheme);
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, Utils.getThemeAttr(context, R.attr.lightIconTheme));
        ContextThemeWrapper contextThemeWrapper2 = new ContextThemeWrapper(context, themeAttr);
        if (configuration.orientation != configuration2.orientation || configuration.densityDpi != configuration2.densityDpi) {
            this.mDockedIcon = getDrawable(contextThemeWrapper, contextThemeWrapper2, R.drawable.ic_sysbar_docked);
            this.mHomeDefaultIcon = getHomeDrawable(contextThemeWrapper, contextThemeWrapper2);
        }
        if (configuration.densityDpi != configuration2.densityDpi || configuration.getLayoutDirection() != configuration2.getLayoutDirection()) {
            this.mBackIcon = getBackDrawable(contextThemeWrapper, contextThemeWrapper2);
            this.mRecentIcon = getDrawable(contextThemeWrapper, contextThemeWrapper2, R.drawable.ic_sysbar_recent);
            this.mMenuIcon = getDrawable(contextThemeWrapper, contextThemeWrapper2, R.drawable.ic_sysbar_menu);
            this.mAccessibilityIcon = getDrawable((Context) contextThemeWrapper, (Context) contextThemeWrapper2, R.drawable.ic_sysbar_accessibility_button, false);
            this.mImeIcon = getDrawable((Context) contextThemeWrapper, (Context) contextThemeWrapper2, R.drawable.ic_ime_switcher_default, false);
            updateRotateSuggestionButtonStyle(this.mRotateBtnStyle, false);
        }
    }

    public KeyButtonDrawable getBackDrawable(Context context, Context context2) {
        KeyButtonDrawable keyButtonDrawableChooseNavigationIconDrawable = chooseNavigationIconDrawable(context, context2, R.drawable.ic_sysbar_back, R.drawable.ic_sysbar_back_quick_step);
        orientBackButton(keyButtonDrawableChooseNavigationIconDrawable);
        return keyButtonDrawableChooseNavigationIconDrawable;
    }

    public KeyButtonDrawable getHomeDrawable(Context context, Context context2) {
        KeyButtonDrawable drawable;
        if (this.mOverviewProxyService.shouldShowSwipeUpUI()) {
            drawable = getDrawable(context, context2, R.drawable.ic_sysbar_home_quick_step);
        } else {
            drawable = getDrawable(context, context2, R.drawable.ic_sysbar_home, false);
        }
        orientHomeButton(drawable);
        return drawable;
    }

    private void orientBackButton(KeyButtonDrawable keyButtonDrawable) {
        boolean z;
        float f;
        if ((this.mNavigationIconHints & 1) == 0) {
            z = false;
        } else {
            z = true;
        }
        if (z) {
            f = -90.0f;
        } else {
            f = getLayoutDirection() == 1 ? 180.0f : 0.0f;
        }
        keyButtonDrawable.setRotation(f);
    }

    private void orientHomeButton(KeyButtonDrawable keyButtonDrawable) {
        keyButtonDrawable.setRotation(this.mVertical ? 90.0f : 0.0f);
    }

    private KeyButtonDrawable chooseNavigationIconDrawable(Context context, Context context2, int i, int i2) {
        if (this.mOverviewProxyService.shouldShowSwipeUpUI()) {
            return getDrawable(context, context2, i2);
        }
        return getDrawable(context, context2, i);
    }

    private KeyButtonDrawable getDrawable(Context context, Context context2, int i) {
        return getDrawable(context, context2, i, true);
    }

    private KeyButtonDrawable getDrawable(Context context, Context context2, int i, boolean z) {
        return KeyButtonDrawable.create(context, context.getDrawable(i), context2.getDrawable(i), z);
    }

    private TintedKeyButtonDrawable getDrawable(Context context, int i, int i2, int i3) {
        return TintedKeyButtonDrawable.create(context.getDrawable(i), i2, i3);
    }

    @Override
    public void setLayoutDirection(int i) {
        reloadNavIcons();
        super.setLayoutDirection(i);
    }

    private KeyButtonDrawable getBackIconWithAlt(boolean z, boolean z2) {
        return z2 ? z ? this.mBackAltLandCarModeIcon : this.mBackIcon : z ? this.mBackAltCarModeIcon : this.mBackIcon;
    }

    private KeyButtonDrawable getBackIcon(boolean z, boolean z2) {
        return z2 ? z ? this.mBackLandCarModeIcon : this.mBackIcon : z ? this.mBackCarModeIcon : this.mBackIcon;
    }

    public void setNavigationIconHints(int i) {
        boolean z;
        if (i == this.mNavigationIconHints) {
            return;
        }
        if ((i & 1) == 0) {
            z = false;
        } else {
            z = true;
        }
        if ((1 & this.mNavigationIconHints) != 0 && !z) {
            this.mTransitionListener.onBackAltCleared();
        }
        this.mNavigationIconHints = i;
        updateNavButtonIcons();
    }

    public void setDisabledFlags(int i) {
        if (this.mDisabledFlags == i) {
            return;
        }
        boolean zIsOverviewEnabled = isOverviewEnabled();
        this.mDisabledFlags = i;
        if (!zIsOverviewEnabled && isOverviewEnabled()) {
            reloadNavIcons();
        }
        updateNavButtonIcons();
        updateSlippery();
        setUpSwipeUpOnboarding(isQuickStepSwipeUpEnabled());
    }

    public void updateNavButtonIcons() {
        KeyButtonDrawable backIcon;
        LayoutTransition layoutTransition;
        boolean z = (this.mNavigationIconHints & 1) != 0;
        if (z) {
            backIcon = getBackIconWithAlt(this.mUseCarModeUi, this.mVertical);
        } else {
            backIcon = getBackIcon(this.mUseCarModeUi, this.mVertical);
        }
        KeyButtonDrawable keyButtonDrawable = this.mUseCarModeUi ? this.mHomeCarModeIcon : this.mHomeDefaultIcon;
        if (!this.mUseCarModeUi) {
            orientBackButton(backIcon);
            orientHomeButton(keyButtonDrawable);
        }
        getHomeButton().setImageDrawable(keyButtonDrawable);
        getBackButton().setImageDrawable(backIcon);
        updateRecentsIcon();
        getImeSwitchButton().setVisibility(!this.mShowAccessibilityButton && !this.mShowRotateButton && (this.mNavigationIconHints & 2) != 0 ? 0 : 4);
        getImeSwitchButton().setImageDrawable(this.mImeIcon);
        setMenuVisibility(this.mShowMenu, true);
        getMenuButton().setImageDrawable(this.mMenuIcon);
        getRotateSuggestionButton().setImageDrawable(this.mRotateSuggestionIcon);
        setAccessibilityButtonState(this.mShowAccessibilityButton, this.mLongClickableAccessibilityButton);
        getAccessibilityButton().setImageDrawable(this.mAccessibilityIcon);
        this.mBarTransitions.reapplyDarkIntensity();
        boolean z2 = (this.mDisabledFlags & 2097152) != 0;
        boolean z3 = this.mUseCarModeUi || !isOverviewEnabled();
        boolean z4 = ((this.mDisabledFlags & 4194304) == 0 || z) ? false : true;
        boolean zIsScreenPinningActive = ActivityManagerWrapper.getInstance().isScreenPinningActive();
        if (this.mOverviewProxyService.isEnabled()) {
            z3 |= (this.mOverviewProxyService.getInteractionFlags() & 4) == 0;
            if (zIsScreenPinningActive) {
                z4 = false;
                z2 = false;
            }
        } else if (zIsScreenPinningActive) {
            z4 = false;
            z3 = false;
        }
        ViewGroup viewGroup = (ViewGroup) getCurrentView().findViewById(R.id.nav_buttons);
        if (viewGroup != null && (layoutTransition = viewGroup.getLayoutTransition()) != null && !layoutTransition.getTransitionListeners().contains(this.mTransitionListener)) {
            layoutTransition.addTransitionListener(this.mTransitionListener);
        }
        boolean z5 = (BenesseExtension.getDchaState() != 0) | z3;
        getBackButton().setVisibility(z4 ? 4 : 0);
        getHomeButton().setVisibility(z2 ? 4 : 0);
        getRecentsButton().setVisibility(z5 ? 4 : 0);
    }

    public boolean inScreenPinning() {
        return ActivityManagerWrapper.getInstance().isScreenPinningActive();
    }

    public void setLayoutTransitionsEnabled(boolean z) {
        this.mLayoutTransitionsEnabled = z;
        updateLayoutTransitionsEnabled();
    }

    public void setWakeAndUnlocking(boolean z) {
        setUseFadingAnimations(z);
        this.mWakeAndUnlocking = z;
        updateLayoutTransitionsEnabled();
    }

    private void updateLayoutTransitionsEnabled() {
        boolean z = !this.mWakeAndUnlocking && this.mLayoutTransitionsEnabled;
        LayoutTransition layoutTransition = ((ViewGroup) getCurrentView().findViewById(R.id.nav_buttons)).getLayoutTransition();
        if (layoutTransition != null) {
            if (z) {
                layoutTransition.enableTransitionType(2);
                layoutTransition.enableTransitionType(3);
                layoutTransition.enableTransitionType(0);
                layoutTransition.enableTransitionType(1);
                return;
            }
            layoutTransition.disableTransitionType(2);
            layoutTransition.disableTransitionType(3);
            layoutTransition.disableTransitionType(0);
            layoutTransition.disableTransitionType(1);
        }
    }

    private void setUseFadingAnimations(boolean z) {
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) ((ViewGroup) getParent()).getLayoutParams();
        if (layoutParams != null) {
            boolean z2 = layoutParams.windowAnimations != 0;
            if (!z2 && z) {
                layoutParams.windowAnimations = 2131886088;
            } else if (z2 && !z) {
                layoutParams.windowAnimations = 0;
            } else {
                return;
            }
            ((WindowManager) getContext().getSystemService("window")).updateViewLayout((View) getParent(), layoutParams);
        }
    }

    public void onNavigationButtonLongPress(View view) {
        this.mGestureHelper.onNavigationButtonLongPress(view);
    }

    public void onPanelExpandedChange(boolean z) {
        updateSlippery();
    }

    public void updateStates() {
        boolean zShouldShowSwipeUpUI = this.mOverviewProxyService.shouldShowSwipeUpUI();
        if (this.mNavigationInflaterView != null) {
            this.mNavigationInflaterView.onLikelyDefaultLayoutChange();
        }
        updateSlippery();
        reloadNavIcons();
        updateNavButtonIcons();
        setUpSwipeUpOnboarding(isQuickStepSwipeUpEnabled());
        WindowManagerWrapper.getInstance().setNavBarVirtualKeyHapticFeedbackEnabled(!zShouldShowSwipeUpUI);
        getHomeButton().setAccessibilityDelegate(zShouldShowSwipeUpUI ? this.mQuickStepAccessibilityDelegate : null);
    }

    private void updateSlippery() {
        setSlippery(!isQuickStepSwipeUpEnabled() || this.mPanelView.isFullyExpanded());
    }

    private void setSlippery(boolean z) {
        ViewGroup viewGroup = (ViewGroup) getParent();
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) viewGroup.getLayoutParams();
        if (layoutParams == null) {
            return;
        }
        boolean z2 = true;
        if (z && (layoutParams.flags & 536870912) == 0) {
            layoutParams.flags |= 536870912;
        } else if (!z && (layoutParams.flags & 536870912) != 0) {
            layoutParams.flags &= -536870913;
        } else {
            z2 = false;
        }
        if (z2) {
            ((WindowManager) getContext().getSystemService("window")).updateViewLayout(viewGroup, layoutParams);
        }
    }

    public void setMenuVisibility(boolean z) {
        setMenuVisibility(z, false);
    }

    public void setMenuVisibility(boolean z, boolean z2) {
        if (z2 || this.mShowMenu != z) {
            this.mShowMenu = z;
            boolean z3 = this.mShowMenu && !this.mShowAccessibilityButton && !this.mShowRotateButton && (this.mNavigationIconHints & 2) == 0;
            getMenuButton().setVisibility(z3 ? 0 : 4);
            getMenuContainer().setVisibility(z3 ? 0 : 4);
        }
    }

    public void setAccessibilityButtonState(boolean z, boolean z2) {
        this.mShowAccessibilityButton = z;
        this.mLongClickableAccessibilityButton = z2;
        if (z) {
            setMenuVisibility(false, true);
            getImeSwitchButton().setVisibility(4);
            setRotateButtonVisibility(false);
        }
        getAccessibilityButton().setVisibility(z ? 0 : 4);
        getAccessibilityButton().setLongClickable(z2);
    }

    public void updateRotateSuggestionButtonStyle(int i, boolean z) {
        this.mRotateBtnStyle = i;
        Context context = getContext();
        int themeAttr = Utils.getThemeAttr(context, R.attr.darkIconTheme);
        int themeAttr2 = Utils.getThemeAttr(context, R.attr.lightIconTheme);
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, themeAttr);
        int colorAttr = Utils.getColorAttr(new ContextThemeWrapper(context, themeAttr2), R.attr.singleToneColor);
        int colorAttr2 = Utils.getColorAttr(contextThemeWrapper, R.attr.singleToneColor);
        ContextThemeWrapper contextThemeWrapper2 = new ContextThemeWrapper(context, i);
        TintedKeyButtonDrawable tintedKeyButtonDrawable = this.mRotateSuggestionIcon;
        this.mRotateSuggestionIcon = getDrawable(contextThemeWrapper2, R.drawable.ic_sysbar_rotate_button, colorAttr, colorAttr2);
        if (tintedKeyButtonDrawable != null && tintedKeyButtonDrawable.isDarkIntensitySet()) {
            this.mRotateSuggestionIcon.setDarkIntensity(tintedKeyButtonDrawable.getDarkIntensity());
        }
        if (z) {
            getRotateSuggestionButton().setImageDrawable(this.mRotateSuggestionIcon);
        }
    }

    public int setRotateButtonVisibility(boolean z) {
        int i;
        if (!(z && !this.mShowAccessibilityButton)) {
            i = 4;
        } else {
            i = 0;
        }
        if (i == getRotateSuggestionButton().getVisibility()) {
            return i;
        }
        getRotateSuggestionButton().setVisibility(i);
        this.mShowRotateButton = z;
        if (!z) {
            Drawable drawable = this.mRotateSuggestionIcon.getDrawable(0);
            if (drawable instanceof AnimatedVectorDrawable) {
                AnimatedVectorDrawable animatedVectorDrawable = (AnimatedVectorDrawable) drawable;
                animatedVectorDrawable.clearAnimationCallbacks();
                animatedVectorDrawable.reset();
            }
        }
        updateNavButtonIcons();
        return i;
    }

    public boolean isRotateButtonVisible() {
        return this.mShowRotateButton;
    }

    ButtonDispatcher getButtonAtPosition(int i, int i2) {
        for (int i3 = 0; i3 < this.mButtonDispatchers.size(); i3++) {
            ButtonDispatcher buttonDispatcherValueAt = this.mButtonDispatchers.valueAt(i3);
            View currentView = buttonDispatcherValueAt.getCurrentView();
            if (currentView != null) {
                currentView.getHitRect(this.mTmpRect);
                offsetDescendantRectToMyCoords(currentView, this.mTmpRect);
                if (this.mTmpRect.contains(i, i2)) {
                    return buttonDispatcherValueAt;
                }
            }
        }
        return null;
    }

    @Override
    public void onFinishInflate() {
        this.mNavigationInflaterView = (NavigationBarInflaterView) findViewById(R.id.navigation_inflater);
        this.mNavigationInflaterView.setButtonDispatchers(this.mButtonDispatchers);
        getImeSwitchButton().setOnClickListener(this.mImeSwitcherClickListener);
        DockedStackExistsListener.register(this.mDockedListener);
        updateRotatedViews();
    }

    public void onDarkIntensityChange(float f) {
        if (this.mGestureHelper != null) {
            this.mGestureHelper.onDarkIntensityChange(f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.mGestureHelper.onDraw(canvas);
        this.mDeadZone.onDraw(canvas);
        super.onDraw(canvas);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        updateButtonLocationOnScreen(getBackButton(), this.mBackButtonBounds);
        updateButtonLocationOnScreen(getHomeButton(), this.mHomeButtonBounds);
        updateButtonLocationOnScreen(getRecentsButton(), this.mRecentsButtonBounds);
        updateButtonLocationOnScreen(getRotateSuggestionButton(), this.mRotationButtonBounds);
        this.mGestureHelper.onLayout(z, i, i2, i3, i4);
        this.mRecentsOnboarding.setNavBarHeight(getMeasuredHeight());
    }

    private void updateButtonLocationOnScreen(ButtonDispatcher buttonDispatcher, Rect rect) {
        View currentView = buttonDispatcher.getCurrentView();
        if (currentView == null) {
            rect.setEmpty();
            return;
        }
        float translationX = currentView.getTranslationX();
        float translationY = currentView.getTranslationY();
        currentView.setTranslationX(0.0f);
        currentView.setTranslationY(0.0f);
        currentView.getLocationInWindow(this.mTmpPosition);
        rect.set(this.mTmpPosition[0], this.mTmpPosition[1], this.mTmpPosition[0] + currentView.getMeasuredWidth(), this.mTmpPosition[1] + currentView.getMeasuredHeight());
        currentView.setTranslationX(translationX);
        currentView.setTranslationY(translationY);
    }

    private void updateRotatedViews() {
        View[] viewArr = this.mRotatedViews;
        View[] viewArr2 = this.mRotatedViews;
        View viewFindViewById = findViewById(R.id.rot0);
        viewArr2[2] = viewFindViewById;
        viewArr[0] = viewFindViewById;
        View[] viewArr3 = this.mRotatedViews;
        View[] viewArr4 = this.mRotatedViews;
        View viewFindViewById2 = findViewById(R.id.rot90);
        viewArr4[1] = viewFindViewById2;
        viewArr3[3] = viewFindViewById2;
        updateCurrentView();
    }

    public boolean needsReorient(int i) {
        return this.mCurrentRotation != i;
    }

    private void updateCurrentView() {
        int rotation = this.mDisplay.getRotation();
        for (int i = 0; i < 4; i++) {
            this.mRotatedViews[i].setVisibility(8);
        }
        this.mCurrentView = this.mRotatedViews[rotation];
        this.mCurrentView.setVisibility(0);
        this.mNavigationInflaterView.setAlternativeOrder(rotation == 1);
        this.mNavigationInflaterView.updateButtonDispatchersCurrentView();
        updateLayoutTransitionsEnabled();
        this.mCurrentRotation = rotation;
    }

    private void updateRecentsIcon() {
        this.mDockedIcon.setRotation((this.mDockedStackExists && this.mVertical) ? 90.0f : 0.0f);
        getRecentsButton().setImageDrawable(this.mDockedStackExists ? this.mDockedIcon : this.mRecentIcon);
        this.mBarTransitions.reapplyDarkIntensity();
    }

    public void reorient() {
        updateCurrentView();
        ((NavigationBarFrame) getRootView()).setDeadZone(this.mDeadZone);
        this.mDeadZone.onConfigurationChanged(this.mCurrentRotation);
        this.mBarTransitions.init();
        setMenuVisibility(this.mShowMenu, true);
        if (!isLayoutDirectionResolved()) {
            resolveLayoutDirection();
        }
        updateTaskSwitchHelper();
        updateNavButtonIcons();
        getHomeButton().setVertical(this.mVertical);
    }

    private void updateTaskSwitchHelper() {
        if (this.mGestureHelper == null) {
            return;
        }
        this.mGestureHelper.setBarState(this.mVertical, getLayoutDirection() == 1);
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        boolean z = i > 0 && i2 > i;
        if (z != this.mVertical) {
            this.mVertical = z;
            reorient();
            notifyVerticalChangedListener(z);
        }
        postCheckForInvalidLayout("sizeChanged");
        super.onSizeChanged(i, i2, i3, i4);
    }

    private void notifyVerticalChangedListener(boolean z) {
        if (this.mOnVerticalChangedListener != null) {
            this.mOnVerticalChangedListener.onVerticalChanged(z);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        boolean zUpdateCarMode = updateCarMode(configuration);
        updateTaskSwitchHelper();
        updateIcons(getContext(), this.mConfiguration, configuration);
        updateRecentsIcon();
        this.mRecentsOnboarding.onConfigurationChanged(configuration);
        if (zUpdateCarMode || this.mConfiguration.densityDpi != configuration.densityDpi || this.mConfiguration.getLayoutDirection() != configuration.getLayoutDirection()) {
            updateNavButtonIcons();
        }
        this.mConfiguration.updateFrom(configuration);
    }

    private boolean updateCarMode(Configuration configuration) {
        if (configuration != null) {
            boolean z = (configuration.uiMode & 15) == 3;
            if (z != this.mInCarMode) {
                this.mInCarMode = z;
                this.mUseCarModeUi = false;
            }
        }
        return false;
    }

    private String getResourceName(int i) {
        if (i != 0) {
            try {
                return getContext().getResources().getResourceName(i);
            } catch (Resources.NotFoundException e) {
                return "(unknown)";
            }
        }
        return "(null)";
    }

    private void postCheckForInvalidLayout(String str) {
        this.mHandler.obtainMessage(8686, 0, 0, str).sendToTarget();
    }

    private static String visibilityToString(int i) {
        if (i == 4) {
            return "INVISIBLE";
        }
        if (i == 8) {
            return "GONE";
        }
        return "VISIBLE";
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestApplyInsets();
        reorient();
        onPluginDisconnected((NavGesture) null);
        ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener((PluginListener) this, NavGesture.class, false);
        setUpSwipeUpOnboarding(isQuickStepSwipeUpEnabled());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((PluginManager) Dependency.get(PluginManager.class)).removePluginListener(this);
        if (this.mGestureHelper != null) {
            this.mGestureHelper.destroy();
        }
        setUpSwipeUpOnboarding(false);
    }

    private void setUpSwipeUpOnboarding(boolean z) {
        if (z) {
            this.mRecentsOnboarding.onConnectedToLauncher();
        } else {
            this.mRecentsOnboarding.onDisconnectedFromLauncher();
        }
    }

    @Override
    public void onPluginConnected(NavGesture navGesture, Context context) {
        this.mGestureHelper = navGesture.getGestureHelper();
        updateTaskSwitchHelper();
    }

    @Override
    public void onPluginDisconnected(NavGesture navGesture) {
        NavigationBarGestureHelper navigationBarGestureHelper = new NavigationBarGestureHelper(getContext());
        navigationBarGestureHelper.setComponents(this.mRecentsComponent, this.mDivider, this);
        if (this.mGestureHelper != null) {
            this.mGestureHelper.destroy();
        }
        this.mGestureHelper = navigationBarGestureHelper;
        updateTaskSwitchHelper();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("NavigationBarView {");
        Rect rect = new Rect();
        Point point = new Point();
        this.mDisplay.getRealSize(point);
        printWriter.println(String.format("      this: " + StatusBar.viewInfo(this) + " " + visibilityToString(getVisibility()), new Object[0]));
        getWindowVisibleDisplayFrame(rect);
        boolean z = rect.right > point.x || rect.bottom > point.y;
        StringBuilder sb = new StringBuilder();
        sb.append("      window: ");
        sb.append(rect.toShortString());
        sb.append(" ");
        sb.append(visibilityToString(getWindowVisibility()));
        sb.append(z ? " OFFSCREEN!" : "");
        printWriter.println(sb.toString());
        printWriter.println(String.format("      mCurrentView: id=%s (%dx%d) %s %f", getResourceName(getCurrentView().getId()), Integer.valueOf(getCurrentView().getWidth()), Integer.valueOf(getCurrentView().getHeight()), visibilityToString(getCurrentView().getVisibility()), Float.valueOf(getCurrentView().getAlpha())));
        Object[] objArr = new Object[3];
        objArr[0] = Integer.valueOf(this.mDisabledFlags);
        objArr[1] = this.mVertical ? "true" : "false";
        objArr[2] = this.mShowMenu ? "true" : "false";
        printWriter.println(String.format("      disabled=0x%08x vertical=%s menu=%s", objArr));
        dumpButton(printWriter, "back", getBackButton());
        dumpButton(printWriter, "home", getHomeButton());
        dumpButton(printWriter, "rcnt", getRecentsButton());
        dumpButton(printWriter, "menu", getMenuButton());
        dumpButton(printWriter, "a11y", getAccessibilityButton());
        this.mRecentsOnboarding.dump(printWriter);
        printWriter.println("    }");
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        setPadding(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(), windowInsets.getSystemWindowInsetRight(), windowInsets.getSystemWindowInsetBottom());
        return super.onApplyWindowInsets(windowInsets);
    }

    private static void dumpButton(PrintWriter printWriter, String str, ButtonDispatcher buttonDispatcher) {
        printWriter.print("      " + str + ": ");
        if (buttonDispatcher == null) {
            printWriter.print("null");
        } else {
            printWriter.print(visibilityToString(buttonDispatcher.getVisibility()) + " alpha=" + buttonDispatcher.getAlpha());
        }
        printWriter.println();
    }

    public static void lambda$new$0(NavigationBarView navigationBarView, Boolean bool) {
        navigationBarView.mDockedStackExists = bool.booleanValue();
        navigationBarView.updateRecentsIcon();
    }
}
