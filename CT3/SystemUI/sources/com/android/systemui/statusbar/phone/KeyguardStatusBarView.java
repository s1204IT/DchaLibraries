package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.keyguard.CarrierText;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import java.text.NumberFormat;

public class KeyguardStatusBarView extends RelativeLayout implements BatteryController.BatteryStateChangeCallback {
    private boolean mBatteryCharging;
    private BatteryController mBatteryController;
    private TextView mBatteryLevel;
    private boolean mBatteryListening;
    private CarrierText mCarrierLabel;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private boolean mKeyguardUserSwitcherShowing;
    private ImageView mMultiUserAvatar;
    private MultiUserSwitch mMultiUserSwitch;
    private View mSystemIconsContainer;
    private View mSystemIconsSuperContainer;
    private int mSystemIconsSwitcherHiddenExpandedMargin;

    public KeyguardStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSystemIconsSuperContainer = findViewById(R.id.system_icons_super_container);
        this.mSystemIconsContainer = findViewById(R.id.system_icons_container);
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) findViewById(R.id.multi_user_avatar);
        this.mBatteryLevel = (TextView) findViewById(R.id.battery_level);
        this.mCarrierLabel = (CarrierText) findViewById(R.id.keyguard_carrier_text);
        loadDimens();
        updateUserSwitcher();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) this.mMultiUserAvatar.getLayoutParams();
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.multi_user_avatar_keyguard_size);
        lp.height = dimensionPixelSize;
        lp.width = dimensionPixelSize;
        this.mMultiUserAvatar.setLayoutParams(lp);
        ViewGroup.MarginLayoutParams lp2 = (ViewGroup.MarginLayoutParams) this.mMultiUserSwitch.getLayoutParams();
        lp2.width = getResources().getDimensionPixelSize(R.dimen.multi_user_switch_width_keyguard);
        lp2.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.multi_user_switch_keyguard_margin));
        this.mMultiUserSwitch.setLayoutParams(lp2);
        ViewGroup.MarginLayoutParams lp3 = (ViewGroup.MarginLayoutParams) this.mSystemIconsSuperContainer.getLayoutParams();
        lp3.height = getResources().getDimensionPixelSize(R.dimen.status_bar_header_height);
        lp3.setMarginStart(getResources().getDimensionPixelSize(R.dimen.system_icons_super_container_margin_start));
        this.mSystemIconsSuperContainer.setLayoutParams(lp3);
        this.mSystemIconsSuperContainer.setPaddingRelative(this.mSystemIconsSuperContainer.getPaddingStart(), this.mSystemIconsSuperContainer.getPaddingTop(), getResources().getDimensionPixelSize(R.dimen.system_icons_keyguard_padding_end), this.mSystemIconsSuperContainer.getPaddingBottom());
        ViewGroup.MarginLayoutParams lp4 = (ViewGroup.MarginLayoutParams) this.mSystemIconsContainer.getLayoutParams();
        lp4.height = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
        this.mSystemIconsContainer.setLayoutParams(lp4);
        ViewGroup.MarginLayoutParams lp5 = (ViewGroup.MarginLayoutParams) this.mBatteryLevel.getLayoutParams();
        lp5.setMarginStart(getResources().getDimensionPixelSize(R.dimen.header_battery_margin_keyguard));
        this.mBatteryLevel.setLayoutParams(lp5);
        this.mBatteryLevel.setPaddingRelative(this.mBatteryLevel.getPaddingStart(), this.mBatteryLevel.getPaddingTop(), getResources().getDimensionPixelSize(R.dimen.battery_level_padding_end), this.mBatteryLevel.getPaddingBottom());
        this.mBatteryLevel.setTextSize(0, getResources().getDimensionPixelSize(R.dimen.battery_level_text_size));
        this.mCarrierLabel.setTextSize(0, getResources().getDimensionPixelSize(android.R.dimen.config_screenBrightnessSettingMinimumFloat));
        ViewGroup.MarginLayoutParams lp6 = (ViewGroup.MarginLayoutParams) this.mCarrierLabel.getLayoutParams();
        lp6.setMarginStart(getResources().getDimensionPixelSize(R.dimen.keyguard_carrier_text_margin));
        this.mCarrierLabel.setLayoutParams(lp6);
        ViewGroup.MarginLayoutParams lp7 = (ViewGroup.MarginLayoutParams) getLayoutParams();
        lp7.height = getResources().getDimensionPixelSize(R.dimen.status_bar_header_height_keyguard);
        setLayoutParams(lp7);
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
        if (marginEnd == lp.getMarginEnd()) {
            return;
        }
        lp.setMarginEnd(marginEnd);
        this.mSystemIconsSuperContainer.setLayoutParams(lp);
    }

    public void setListening(boolean listening) {
        if (listening == this.mBatteryListening) {
            return;
        }
        this.mBatteryListening = listening;
        if (this.mBatteryListening) {
            this.mBatteryController.addStateChangedCallback(this);
        } else {
            this.mBatteryController.removeStateChangedCallback(this);
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

    public void setUserSwitcherController(UserSwitcherController controller) {
        this.mMultiUserSwitch.setUserSwitcherController(controller);
    }

    public void setUserInfoController(UserInfoController userInfoController) {
        userInfoController.addListener(new UserInfoController.OnUserInfoChangedListener() {
            @Override
            public void onUserInfoChanged(String name, Drawable picture) {
                Log.d("KeyguardStatusBarView", "onUserInfoChanged and set new profile icon");
                KeyguardStatusBarView.this.mMultiUserAvatar.setImageDrawable(picture);
            }
        });
    }

    public void setQSPanel(QSPanel qsp) {
        this.mMultiUserSwitch.setQsPanel(qsp);
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        String percentage = NumberFormat.getPercentInstance().format(((double) level) / 100.0d);
        this.mBatteryLevel.setText(percentage);
        boolean changed = this.mBatteryCharging != charging;
        this.mBatteryCharging = charging;
        if (!changed) {
            return;
        }
        updateVisibilities();
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
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
                KeyguardStatusBarView.this.mSystemIconsSuperContainer.animate().translationX(0.0f).setDuration(400L).setStartDelay(userSwitcherHiding ? 300 : 0).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).start();
                if (userSwitcherHiding) {
                    KeyguardStatusBarView.this.getOverlay().add(KeyguardStatusBarView.this.mMultiUserSwitch);
                    KeyguardStatusBarView.this.mMultiUserSwitch.animate().alpha(0.0f).setDuration(300L).setStartDelay(0L).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            KeyguardStatusBarView.this.mMultiUserSwitch.setAlpha(1.0f);
                            KeyguardStatusBarView.this.getOverlay().remove(KeyguardStatusBarView.this.mMultiUserSwitch);
                        }
                    }).start();
                    return true;
                }
                KeyguardStatusBarView.this.mMultiUserSwitch.setAlpha(0.0f);
                KeyguardStatusBarView.this.mMultiUserSwitch.animate().alpha(1.0f).setDuration(300L).setStartDelay(200L).setInterpolator(Interpolators.ALPHA_IN);
                return true;
            }
        });
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == 0) {
            return;
        }
        this.mSystemIconsSuperContainer.animate().cancel();
        this.mMultiUserSwitch.animate().cancel();
        this.mMultiUserSwitch.setAlpha(1.0f);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
