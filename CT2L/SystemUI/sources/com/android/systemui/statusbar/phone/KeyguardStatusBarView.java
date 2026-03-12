package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.UserInfoController;
import java.text.NumberFormat;

public class KeyguardStatusBarView extends RelativeLayout implements BatteryController.BatteryStateChangeCallback {
    private boolean mBatteryCharging;
    private BatteryController mBatteryController;
    private TextView mBatteryLevel;
    private boolean mBatteryListening;
    private TextView mCarrierLabel;
    private Interpolator mFastOutSlowInInterpolator;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private boolean mKeyguardUserSwitcherShowing;
    private ImageView mMultiUserAvatar;
    private MultiUserSwitch mMultiUserSwitch;
    private View mSystemIconsSuperContainer;
    private int mSystemIconsSwitcherHiddenExpandedMargin;

    public KeyguardStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSystemIconsSuperContainer = findViewById(R.id.system_icons_super_container);
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) findViewById(R.id.multi_user_avatar);
        this.mBatteryLevel = (TextView) findViewById(R.id.battery_level);
        this.mCarrierLabel = (TextView) findViewById(R.id.keyguard_carrier_text);
        loadDimens();
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.fast_out_slow_in);
        updateUserSwitcher();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mCarrierLabel.setTextSize(0, getResources().getDimensionPixelSize(android.R.dimen.config_hoverTapSlop));
        this.mBatteryLevel.setTextSize(0, getResources().getDimensionPixelSize(R.dimen.battery_level_text_size));
    }

    private void loadDimens() {
        this.mSystemIconsSwitcherHiddenExpandedMargin = getResources().getDimensionPixelSize(R.dimen.system_icons_switcher_hidden_expanded_margin);
    }

    private void updateVisibilities() {
        if (this.mMultiUserSwitch.getParent() != this && !this.mKeyguardUserSwitcherShowing) {
            if (this.mMultiUserSwitch.getParent() != null) {
                getOverlay().remove(this.mMultiUserSwitch);
            }
            addView(this.mMultiUserSwitch, 0);
        } else if (this.mMultiUserSwitch.getParent() == this && this.mKeyguardUserSwitcherShowing) {
            removeView(this.mMultiUserSwitch);
        }
        this.mBatteryLevel.setVisibility(this.mBatteryCharging ? 0 : 8);
    }

    private void updateSystemIconsLayoutParams() {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) this.mSystemIconsSuperContainer.getLayoutParams();
        int marginEnd = this.mKeyguardUserSwitcherShowing ? this.mSystemIconsSwitcherHiddenExpandedMargin : 0;
        if (marginEnd != lp.getMarginEnd()) {
            lp.setMarginEnd(marginEnd);
            this.mSystemIconsSuperContainer.setLayoutParams(lp);
        }
    }

    public void setListening(boolean listening) {
        if (listening != this.mBatteryListening) {
            this.mBatteryListening = listening;
            if (this.mBatteryListening) {
                this.mBatteryController.addStateChangedCallback(this);
            } else {
                this.mBatteryController.removeStateChangedCallback(this);
            }
        }
    }

    private void updateUserSwitcher() {
        boolean keyguardSwitcherAvailable = this.mKeyguardUserSwitcher != null;
        this.mMultiUserSwitch.setClickable(keyguardSwitcherAvailable);
        this.mMultiUserSwitch.setFocusable(keyguardSwitcherAvailable);
        this.mMultiUserSwitch.setKeyguardMode(keyguardSwitcherAvailable);
    }

    public void setBatteryController(BatteryController batteryController) {
        this.mBatteryController = batteryController;
        ((BatteryMeterView) findViewById(R.id.battery)).setBatteryController(batteryController);
    }

    public void setUserInfoController(UserInfoController userInfoController) {
        userInfoController.addListener(new UserInfoController.OnUserInfoChangedListener() {
            @Override
            public void onUserInfoChanged(String name, Drawable picture) {
                KeyguardStatusBarView.this.mMultiUserAvatar.setImageDrawable(picture);
            }
        });
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        String percentage = NumberFormat.getPercentInstance().format(((double) level) / 100.0d);
        this.mBatteryLevel.setText(percentage);
        boolean changed = this.mBatteryCharging != charging;
        this.mBatteryCharging = charging;
        if (changed) {
            updateVisibilities();
        }
    }

    @Override
    public void onPowerSaveChanged() {
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
        this.mMultiUserSwitch.setKeyguardUserSwitcher(keyguardUserSwitcher);
        updateUserSwitcher();
    }

    public void setKeyguardUserSwitcherShowing(boolean showing, boolean animate) {
        this.mKeyguardUserSwitcherShowing = showing;
        if (animate) {
            animateNextLayoutChange();
        }
        updateVisibilities();
        updateSystemIconsLayoutParams();
    }

    private void animateNextLayoutChange() {
        final int systemIconsCurrentX = this.mSystemIconsSuperContainer.getLeft();
        final boolean userSwitcherVisible = this.mMultiUserSwitch.getParent() == this;
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                KeyguardStatusBarView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                boolean userSwitcherHiding = userSwitcherVisible && KeyguardStatusBarView.this.mMultiUserSwitch.getParent() != KeyguardStatusBarView.this;
                KeyguardStatusBarView.this.mSystemIconsSuperContainer.setX(systemIconsCurrentX);
                KeyguardStatusBarView.this.mSystemIconsSuperContainer.animate().translationX(0.0f).setDuration(400L).setStartDelay(userSwitcherHiding ? 300L : 0L).setInterpolator(KeyguardStatusBarView.this.mFastOutSlowInInterpolator).start();
                if (userSwitcherHiding) {
                    KeyguardStatusBarView.this.getOverlay().add(KeyguardStatusBarView.this.mMultiUserSwitch);
                    KeyguardStatusBarView.this.mMultiUserSwitch.animate().alpha(0.0f).setDuration(300L).setStartDelay(0L).setInterpolator(PhoneStatusBar.ALPHA_OUT).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            KeyguardStatusBarView.this.mMultiUserSwitch.setAlpha(1.0f);
                            KeyguardStatusBarView.this.getOverlay().remove(KeyguardStatusBarView.this.mMultiUserSwitch);
                        }
                    }).start();
                } else {
                    KeyguardStatusBarView.this.mMultiUserSwitch.setAlpha(0.0f);
                    KeyguardStatusBarView.this.mMultiUserSwitch.animate().alpha(1.0f).setDuration(300L).setStartDelay(200L).setInterpolator(PhoneStatusBar.ALPHA_IN);
                }
                return true;
            }
        });
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != 0) {
            this.mSystemIconsSuperContainer.animate().cancel();
            this.mMultiUserSwitch.animate().cancel();
            this.mMultiUserSwitch.setAlpha(1.0f);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
