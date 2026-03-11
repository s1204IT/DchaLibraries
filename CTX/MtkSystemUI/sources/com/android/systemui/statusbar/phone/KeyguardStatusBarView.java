package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.DisplayCutout;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.settingslib.Utils;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.ScreenDecorations;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.phone.KeyguardStatusBarView;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserSwitcherController;

public class KeyguardStatusBarView extends RelativeLayout implements BatteryController.BatteryStateChangeCallback, ConfigurationController.ConfigurationListener, UserInfoController.OnUserInfoChangedListener {
    private boolean mBatteryCharging;
    private BatteryController mBatteryController;
    private boolean mBatteryListening;
    private BatteryMeterView mBatteryView;
    private TextView mCarrierLabel;
    private int mCutoutSideNudge;
    private View mCutoutSpace;
    private StatusBarIconController.TintedIconManager mIconManager;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private boolean mKeyguardUserSwitcherShowing;
    private int mLayoutState;
    private ImageView mMultiUserAvatar;
    private MultiUserSwitch mMultiUserSwitch;
    private ViewGroup mStatusIconArea;
    private int mSystemIconsBaseMargin;
    private View mSystemIconsContainer;
    private int mSystemIconsSwitcherHiddenExpandedMargin;
    private UserSwitcherController mUserSwitcherController;

    public KeyguardStatusBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLayoutState = 0;
        this.mCutoutSideNudge = 0;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSystemIconsContainer = findViewById(R.id.system_icons_container);
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) findViewById(R.id.multi_user_avatar);
        this.mCarrierLabel = (TextView) findViewById(R.id.keyguard_carrier_text);
        this.mBatteryView = (BatteryMeterView) this.mSystemIconsContainer.findViewById(R.id.battery);
        this.mCutoutSpace = findViewById(R.id.cutout_space_view);
        this.mStatusIconArea = (ViewGroup) findViewById(R.id.status_icon_area);
        loadDimens();
        updateUserSwitcher();
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mMultiUserAvatar.getLayoutParams();
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.multi_user_avatar_keyguard_size);
        marginLayoutParams.height = dimensionPixelSize;
        marginLayoutParams.width = dimensionPixelSize;
        this.mMultiUserAvatar.setLayoutParams(marginLayoutParams);
        ViewGroup.MarginLayoutParams marginLayoutParams2 = (ViewGroup.MarginLayoutParams) this.mMultiUserSwitch.getLayoutParams();
        marginLayoutParams2.width = getResources().getDimensionPixelSize(R.dimen.multi_user_switch_width_keyguard);
        marginLayoutParams2.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.multi_user_switch_keyguard_margin));
        this.mMultiUserSwitch.setLayoutParams(marginLayoutParams2);
        ViewGroup.MarginLayoutParams marginLayoutParams3 = (ViewGroup.MarginLayoutParams) this.mSystemIconsContainer.getLayoutParams();
        marginLayoutParams3.setMarginStart(getResources().getDimensionPixelSize(R.dimen.system_icons_super_container_margin_start));
        this.mSystemIconsContainer.setLayoutParams(marginLayoutParams3);
        this.mSystemIconsContainer.setPaddingRelative(this.mSystemIconsContainer.getPaddingStart(), this.mSystemIconsContainer.getPaddingTop(), getResources().getDimensionPixelSize(R.dimen.system_icons_keyguard_padding_end), this.mSystemIconsContainer.getPaddingBottom());
        this.mCarrierLabel.setTextSize(0, getResources().getDimensionPixelSize(android.R.dimen.indeterminate_progress_alpha_01));
        ViewGroup.MarginLayoutParams marginLayoutParams4 = (ViewGroup.MarginLayoutParams) this.mCarrierLabel.getLayoutParams();
        marginLayoutParams4.setMarginStart(getResources().getDimensionPixelSize(R.dimen.keyguard_carrier_text_margin));
        this.mCarrierLabel.setLayoutParams(marginLayoutParams4);
        ViewGroup.MarginLayoutParams marginLayoutParams5 = (ViewGroup.MarginLayoutParams) getLayoutParams();
        marginLayoutParams5.height = getResources().getDimensionPixelSize(R.dimen.status_bar_header_height_keyguard);
        setLayoutParams(marginLayoutParams5);
    }

    private void loadDimens() {
        Resources resources = getResources();
        this.mSystemIconsSwitcherHiddenExpandedMargin = resources.getDimensionPixelSize(R.dimen.system_icons_switcher_hidden_expanded_margin);
        this.mSystemIconsBaseMargin = resources.getDimensionPixelSize(R.dimen.system_icons_super_container_avatarless_margin_end);
        this.mCutoutSideNudge = getResources().getDimensionPixelSize(R.dimen.display_cutout_margin_consumption);
    }

    private void updateVisibilities() {
        if (this.mMultiUserSwitch.getParent() != this.mStatusIconArea && !this.mKeyguardUserSwitcherShowing) {
            if (this.mMultiUserSwitch.getParent() != null) {
                getOverlay().remove(this.mMultiUserSwitch);
            }
            this.mStatusIconArea.addView(this.mMultiUserSwitch, 0);
        } else if (this.mMultiUserSwitch.getParent() == this.mStatusIconArea && this.mKeyguardUserSwitcherShowing) {
            this.mStatusIconArea.removeView(this.mMultiUserSwitch);
        }
        if (this.mKeyguardUserSwitcher == null) {
            if (this.mUserSwitcherController != null && this.mUserSwitcherController.getSwitchableUserCount() > 1) {
                this.mMultiUserSwitch.setVisibility(0);
            } else {
                this.mMultiUserSwitch.setVisibility(8);
            }
        }
        this.mBatteryView.setForceShowPercent(this.mBatteryCharging);
    }

    private void updateSystemIconsLayoutParams() {
        int i;
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mSystemIconsContainer.getLayoutParams();
        if (this.mMultiUserSwitch.getVisibility() == 8) {
            i = this.mSystemIconsBaseMargin;
        } else {
            i = 0;
        }
        if (this.mKeyguardUserSwitcherShowing) {
            i = this.mSystemIconsSwitcherHiddenExpandedMargin;
        }
        if (i != layoutParams.getMarginEnd()) {
            layoutParams.setMarginEnd(i);
            this.mSystemIconsContainer.setLayoutParams(layoutParams);
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        this.mLayoutState = 0;
        if (updateLayoutConsideringCutout()) {
            requestLayout();
        }
        return super.onApplyWindowInsets(windowInsets);
    }

    private boolean updateLayoutConsideringCutout() {
        DisplayCutout displayCutout = getRootWindowInsets().getDisplayCutout();
        Pair<Integer, Integer> pairCornerCutoutMargins = PhoneStatusBarView.cornerCutoutMargins(displayCutout, getDisplay());
        updateCornerCutoutPadding(pairCornerCutoutMargins);
        if (displayCutout == null || pairCornerCutoutMargins != null) {
            return updateLayoutParamsNoCutout();
        }
        return updateLayoutParamsForCutout(displayCutout);
    }

    private void updateCornerCutoutPadding(Pair<Integer, Integer> pair) {
        if (pair != null) {
            setPadding(((Integer) pair.first).intValue(), 0, ((Integer) pair.second).intValue(), 0);
        } else {
            setPadding(0, 0, 0, 0);
        }
    }

    private boolean updateLayoutParamsNoCutout() {
        if (this.mLayoutState == 2) {
            return false;
        }
        this.mLayoutState = 2;
        if (this.mCutoutSpace != null) {
            this.mCutoutSpace.setVisibility(8);
        }
        ((RelativeLayout.LayoutParams) this.mCarrierLabel.getLayoutParams()).addRule(16, R.id.status_icon_area);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mStatusIconArea.getLayoutParams();
        layoutParams.removeRule(1);
        layoutParams.width = -2;
        ((LinearLayout.LayoutParams) this.mSystemIconsContainer.getLayoutParams()).setMarginStart(getResources().getDimensionPixelSize(R.dimen.system_icons_super_container_margin_start));
        return true;
    }

    private boolean updateLayoutParamsForCutout(DisplayCutout displayCutout) {
        if (this.mLayoutState == 1) {
            return false;
        }
        this.mLayoutState = 1;
        if (this.mCutoutSpace == null) {
            updateLayoutParamsNoCutout();
        }
        Rect rect = new Rect();
        ScreenDecorations.DisplayCutoutView.boundsFromDirection(displayCutout, 48, rect);
        this.mCutoutSpace.setVisibility(0);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mCutoutSpace.getLayoutParams();
        rect.left += this.mCutoutSideNudge;
        rect.right -= this.mCutoutSideNudge;
        layoutParams.width = rect.width();
        layoutParams.height = rect.height();
        layoutParams.addRule(13);
        ((RelativeLayout.LayoutParams) this.mCarrierLabel.getLayoutParams()).addRule(16, R.id.cutout_space_view);
        RelativeLayout.LayoutParams layoutParams2 = (RelativeLayout.LayoutParams) this.mStatusIconArea.getLayoutParams();
        layoutParams2.addRule(1, R.id.cutout_space_view);
        layoutParams2.width = -1;
        ((LinearLayout.LayoutParams) this.mSystemIconsContainer.getLayoutParams()).setMarginStart(0);
        return true;
    }

    public void setListening(boolean z) {
        if (z == this.mBatteryListening) {
            return;
        }
        this.mBatteryListening = z;
        if (this.mBatteryListening) {
            this.mBatteryController.addCallback(this);
        } else {
            this.mBatteryController.removeCallback(this);
        }
    }

    private void updateUserSwitcher() {
        boolean z = this.mKeyguardUserSwitcher != null;
        this.mMultiUserSwitch.setClickable(z);
        this.mMultiUserSwitch.setFocusable(z);
        this.mMultiUserSwitch.setKeyguardMode(z);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        UserInfoController userInfoController = (UserInfoController) Dependency.get(UserInfoController.class);
        userInfoController.addCallback(this);
        this.mUserSwitcherController = (UserSwitcherController) Dependency.get(UserSwitcherController.class);
        this.mMultiUserSwitch.setUserSwitcherController(this.mUserSwitcherController);
        userInfoController.reloadUserInfo();
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        this.mIconManager = new StatusBarIconController.TintedIconManager((ViewGroup) findViewById(R.id.statusIcons));
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).addIconGroup(this.mIconManager);
        onThemeChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((UserInfoController) Dependency.get(UserInfoController.class)).removeCallback(this);
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).removeIconGroup(this.mIconManager);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    @Override
    public void onUserInfoChanged(String str, Drawable drawable, String str2) {
        this.mMultiUserAvatar.setImageDrawable(drawable);
    }

    public void setQSPanel(QSPanel qSPanel) {
        this.mMultiUserSwitch.setQsPanel(qSPanel);
    }

    @Override
    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        if (this.mBatteryCharging != z2) {
            this.mBatteryCharging = z2;
            updateVisibilities();
        }
    }

    @Override
    public void onPowerSaveChanged(boolean z) {
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
        this.mMultiUserSwitch.setKeyguardUserSwitcher(keyguardUserSwitcher);
        updateUserSwitcher();
    }

    public void setKeyguardUserSwitcherShowing(boolean z, boolean z2) {
        this.mKeyguardUserSwitcherShowing = z;
        if (z2) {
            animateNextLayoutChange();
        }
        updateVisibilities();
        updateLayoutConsideringCutout();
        updateSystemIconsLayoutParams();
    }

    private void animateNextLayoutChange() {
        getViewTreeObserver().addOnPreDrawListener(new AnonymousClass1(this.mMultiUserSwitch.getParent() == this.mStatusIconArea, this.mSystemIconsContainer.getLeft()));
    }

    class AnonymousClass1 implements ViewTreeObserver.OnPreDrawListener {
        final int val$systemIconsCurrentX;
        final boolean val$userSwitcherVisible;

        AnonymousClass1(boolean z, int i) {
            this.val$userSwitcherVisible = z;
            this.val$systemIconsCurrentX = i;
        }

        @Override
        public boolean onPreDraw() {
            boolean z;
            KeyguardStatusBarView.this.getViewTreeObserver().removeOnPreDrawListener(this);
            if (!this.val$userSwitcherVisible || KeyguardStatusBarView.this.mMultiUserSwitch.getParent() == KeyguardStatusBarView.this.mStatusIconArea) {
                z = false;
            } else {
                z = true;
            }
            KeyguardStatusBarView.this.mSystemIconsContainer.setX(this.val$systemIconsCurrentX);
            KeyguardStatusBarView.this.mSystemIconsContainer.animate().translationX(0.0f).setDuration(400L).setStartDelay(z ? 300L : 0L).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).start();
            if (z) {
                KeyguardStatusBarView.this.getOverlay().add(KeyguardStatusBarView.this.mMultiUserSwitch);
                KeyguardStatusBarView.this.mMultiUserSwitch.animate().alpha(0.0f).setDuration(300L).setStartDelay(0L).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
                    @Override
                    public final void run() {
                        KeyguardStatusBarView.AnonymousClass1.lambda$onPreDraw$0(this.f$0);
                    }
                }).start();
            } else {
                KeyguardStatusBarView.this.mMultiUserSwitch.setAlpha(0.0f);
                KeyguardStatusBarView.this.mMultiUserSwitch.animate().alpha(1.0f).setDuration(300L).setStartDelay(200L).setInterpolator(Interpolators.ALPHA_IN);
            }
            return true;
        }

        public static void lambda$onPreDraw$0(AnonymousClass1 anonymousClass1) {
            KeyguardStatusBarView.this.mMultiUserSwitch.setAlpha(1.0f);
            KeyguardStatusBarView.this.getOverlay().remove(KeyguardStatusBarView.this.mMultiUserSwitch);
        }
    }

    @Override
    public void setVisibility(int i) {
        super.setVisibility(i);
        if (i != 0) {
            this.mSystemIconsContainer.animate().cancel();
            this.mSystemIconsContainer.setTranslationX(0.0f);
            this.mMultiUserSwitch.animate().cancel();
            this.mMultiUserSwitch.setAlpha(1.0f);
            return;
        }
        updateVisibilities();
        updateSystemIconsLayoutParams();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void onThemeChanged() {
        int i;
        int colorAttr = Utils.getColorAttr(this.mContext, R.attr.wallpaperTextColor);
        Context context = this.mContext;
        if (Color.luminance(colorAttr) < 0.5d) {
            i = R.color.dark_mode_icon_color_single_tone;
        } else {
            i = R.color.light_mode_icon_color_single_tone;
        }
        int defaultColor = Utils.getDefaultColor(context, i);
        float f = colorAttr == -1 ? 0.0f : 1.0f;
        this.mCarrierLabel.setTextColor(defaultColor);
        this.mBatteryView.setFillColor(defaultColor);
        this.mIconManager.setTint(defaultColor);
        Rect rect = new Rect(0, 0, 0, 0);
        applyDarkness(R.id.battery, rect, f, defaultColor);
        applyDarkness(R.id.clock, rect, f, defaultColor);
        ((UserInfoControllerImpl) Dependency.get(UserInfoController.class)).onDensityOrFontScaleChanged();
    }

    private void applyDarkness(int i, Rect rect, float f, int i2) {
        KeyEvent.Callback callbackFindViewById = findViewById(i);
        if (callbackFindViewById instanceof DarkIconDispatcher.DarkReceiver) {
            ((DarkIconDispatcher.DarkReceiver) callbackFindViewById).onDarkChanged(rect, f, i2);
        }
    }
}
