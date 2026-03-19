package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.BenesseExtension;
import android.os.Handler;
import android.service.notification.ZenModeConfig;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.settingslib.Utils;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.Dependency;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.qs.QSDetail;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusIconContainer;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DateView;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.ZenModeController;
import java.util.Locale;
import java.util.Objects;

public class QuickStatusBarHeader extends RelativeLayout implements View.OnClickListener, NextAlarmController.NextAlarmChangeCallback, ZenModeController.Callback {
    private NextAlarmController mAlarmController;
    private final Runnable mAutoFadeOutTooltipRunnable;
    private BatteryMeterView mBatteryMeterView;
    private Clock mClockView;
    private DateView mDateView;
    private boolean mExpanded;
    private final Handler mHandler;
    protected QuickQSPanel mHeaderQsPanel;
    private TouchAnimator mHeaderTextContainerAlphaAnimator;
    private View mHeaderTextContainerView;
    protected QSTileHost mHost;
    private StatusBarIconController.TintedIconManager mIconManager;
    private boolean mListening;
    private View mLongPressTooltipView;
    private AlarmManager.AlarmClockInfo mNextAlarm;
    private ImageView mNextAlarmIcon;
    private TextView mNextAlarmTextView;
    private boolean mQsDisabled;
    private QSPanel mQsPanel;
    private View mQuickQsStatusIcons;
    private int mRingerMode;
    private ImageView mRingerModeIcon;
    private TextView mRingerModeTextView;
    private final BroadcastReceiver mRingerReceiver;
    private int mShownCount;
    private View mStatusContainer;
    private TouchAnimator mStatusIconsAlphaAnimator;
    private View mStatusSeparator;
    private View mSystemIconsView;
    private ZenModeController mZenController;

    public QuickStatusBarHeader(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHandler = new Handler();
        this.mRingerMode = 2;
        this.mRingerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                QuickStatusBarHeader.this.mRingerMode = intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1);
                QuickStatusBarHeader.this.updateStatusText();
            }
        };
        this.mAutoFadeOutTooltipRunnable = new Runnable() {
            @Override
            public final void run() {
                this.f$0.hideLongPressTooltip(false);
            }
        };
        this.mAlarmController = (NextAlarmController) Dependency.get(NextAlarmController.class);
        this.mZenController = (ZenModeController) Dependency.get(ZenModeController.class);
        this.mShownCount = getStoredShownCount();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHeaderQsPanel = (QuickQSPanel) findViewById(R.id.quick_qs_panel);
        this.mSystemIconsView = findViewById(R.id.quick_status_bar_system_icons);
        this.mQuickQsStatusIcons = findViewById(R.id.quick_qs_status_icons);
        StatusIconContainer statusIconContainer = (StatusIconContainer) findViewById(R.id.statusIcons);
        statusIconContainer.setShouldRestrictIcons(false);
        this.mIconManager = new StatusBarIconController.TintedIconManager(statusIconContainer);
        this.mHeaderTextContainerView = findViewById(R.id.header_text_container);
        this.mLongPressTooltipView = findViewById(R.id.long_press_tooltip);
        this.mStatusContainer = findViewById(R.id.status_container);
        this.mStatusSeparator = findViewById(R.id.status_separator);
        this.mNextAlarmIcon = (ImageView) findViewById(R.id.next_alarm_icon);
        this.mNextAlarmTextView = (TextView) findViewById(R.id.next_alarm_text);
        this.mRingerModeIcon = (ImageView) findViewById(R.id.ringer_mode_icon);
        this.mRingerModeTextView = (TextView) findViewById(R.id.ringer_mode_text);
        updateResources();
        Rect rect = new Rect(0, 0, 0, 0);
        int iFillColorForIntensity = fillColorForIntensity(getColorIntensity(Utils.getColorAttr(getContext(), android.R.attr.colorForeground)), getContext());
        applyDarkness(R.id.clock, rect, 0.0f, -1);
        this.mIconManager.setTint(iFillColorForIntensity);
        this.mBatteryMeterView = (BatteryMeterView) findViewById(R.id.battery);
        this.mBatteryMeterView.setForceShowPercent(true);
        this.mBatteryMeterView.setOnClickListener(this);
        this.mClockView = (Clock) findViewById(R.id.clock);
        this.mClockView.setOnClickListener(this);
        this.mDateView = (DateView) findViewById(R.id.date);
    }

    private void updateStatusText() {
        if (updateRingerStatus() || updateAlarmStatus()) {
            this.mStatusSeparator.setVisibility(((this.mNextAlarmTextView.getVisibility() == 0) && (this.mRingerModeTextView.getVisibility() == 0)) ? 0 : 8);
            updateTooltipShow();
        }
    }

    private boolean updateRingerStatus() {
        boolean z;
        boolean z2 = this.mRingerModeTextView.getVisibility() == 0;
        CharSequence text = this.mRingerModeTextView.getText();
        if (!ZenModeConfig.isZenOverridingRinger(this.mZenController.getZen(), this.mZenController.getConfig())) {
            if (this.mRingerMode == 1) {
                this.mRingerModeIcon.setImageResource(R.drawable.stat_sys_ringer_vibrate);
                this.mRingerModeTextView.setText(R.string.qs_status_phone_vibrate);
            } else {
                if (this.mRingerMode == 0) {
                    this.mRingerModeIcon.setImageResource(R.drawable.stat_sys_ringer_silent);
                    this.mRingerModeTextView.setText(R.string.qs_status_phone_muted);
                }
                z = false;
            }
            z = true;
        } else {
            z = false;
        }
        this.mRingerModeIcon.setVisibility(z ? 0 : 8);
        this.mRingerModeTextView.setVisibility(z ? 0 : 8);
        return (z2 == z && Objects.equals(text, this.mRingerModeTextView.getText())) ? false : true;
    }

    private boolean updateAlarmStatus() {
        boolean z;
        boolean z2 = this.mNextAlarmTextView.getVisibility() == 0;
        CharSequence text = this.mNextAlarmTextView.getText();
        if (this.mNextAlarm != null) {
            this.mNextAlarmTextView.setText(formatNextAlarm(this.mNextAlarm));
            z = true;
        } else {
            z = false;
        }
        this.mNextAlarmIcon.setVisibility(z ? 0 : 8);
        this.mNextAlarmTextView.setVisibility(z ? 0 : 8);
        return (z2 == z && Objects.equals(text, this.mNextAlarmTextView.getText())) ? false : true;
    }

    private void applyDarkness(int i, Rect rect, float f, int i2) {
        KeyEvent.Callback callbackFindViewById = findViewById(i);
        if (callbackFindViewById instanceof DarkIconDispatcher.DarkReceiver) {
            ((DarkIconDispatcher.DarkReceiver) callbackFindViewById).onDarkChanged(rect, f, i2);
        }
    }

    private int fillColorForIntensity(float f, Context context) {
        if (f == 0.0f) {
            return context.getColor(R.color.light_mode_icon_color_single_tone);
        }
        return context.getColor(R.color.dark_mode_icon_color_single_tone);
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateResources();
        boolean z = configuration.orientation == 2;
        this.mBatteryMeterView.useWallpaperTextColor(z);
        this.mClockView.useWallpaperTextColor(z);
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        updateResources();
    }

    private void updateResources() {
        Resources resources = this.mContext.getResources();
        this.mHeaderTextContainerView.getLayoutParams().height = resources.getDimensionPixelSize(R.dimen.qs_header_tooltip_height);
        this.mHeaderTextContainerView.setLayoutParams(this.mHeaderTextContainerView.getLayoutParams());
        ViewGroup.LayoutParams layoutParams = this.mSystemIconsView.getLayoutParams();
        int i = android.R.dimen.emphasized_button_stroke_width;
        layoutParams.height = resources.getDimensionPixelSize(android.R.dimen.emphasized_button_stroke_width);
        this.mSystemIconsView.setLayoutParams(this.mSystemIconsView.getLayoutParams());
        ViewGroup.LayoutParams layoutParams2 = getLayoutParams();
        if (!this.mQsDisabled) {
            i = android.R.dimen.expanded_group_conversation_message_padding;
        }
        layoutParams2.height = resources.getDimensionPixelSize(i);
        setLayoutParams(getLayoutParams());
        updateStatusIconAlphaAnimator();
        updateHeaderTextContainerAlphaAnimator();
    }

    private void updateStatusIconAlphaAnimator() {
        this.mStatusIconsAlphaAnimator = new TouchAnimator.Builder().addFloat(this.mQuickQsStatusIcons, "alpha", 1.0f, 0.0f).build();
    }

    private void updateHeaderTextContainerAlphaAnimator() {
        this.mHeaderTextContainerAlphaAnimator = new TouchAnimator.Builder().addFloat(this.mHeaderTextContainerView, "alpha", 0.0f, 1.0f).setStartDelay(0.5f).build();
    }

    public void setExpanded(boolean z) {
        if (this.mExpanded == z) {
            return;
        }
        this.mExpanded = z;
        this.mHeaderQsPanel.setExpanded(z);
        updateEverything();
    }

    public void setExpansion(boolean z, float f, float f2) {
        float f3 = z ? 1.0f : f;
        if (this.mStatusIconsAlphaAnimator != null) {
            this.mStatusIconsAlphaAnimator.setPosition(f3);
        }
        if (z) {
            this.mHeaderTextContainerView.setTranslationY(f2);
        } else {
            this.mHeaderTextContainerView.setTranslationY(0.0f);
        }
        if (this.mHeaderTextContainerAlphaAnimator != null) {
            this.mHeaderTextContainerAlphaAnimator.setPosition(f3);
        }
        if (f == 1.0f) {
            showLongPressTooltip();
        }
    }

    private int getStoredShownCount() {
        return Prefs.getInt(this.mContext, "QsLongPressTooltipShownCount", 0);
    }

    public void disable(int i, int i2, boolean z) {
        boolean z2 = true;
        if ((i2 & 1) == 0) {
            z2 = false;
        }
        if (z2 == this.mQsDisabled) {
            return;
        }
        this.mQsDisabled = z2;
        this.mHeaderQsPanel.setDisabledByPolicy(z2);
        this.mHeaderTextContainerView.setVisibility(this.mQsDisabled ? 8 : 0);
        this.mQuickQsStatusIcons.setVisibility(this.mQsDisabled ? 8 : 0);
        updateResources();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).addIconGroup(this.mIconManager);
        requestApplyInsets();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        Pair<Integer, Integer> pairCornerCutoutMargins = PhoneStatusBarView.cornerCutoutMargins(windowInsets.getDisplayCutout(), getDisplay());
        if (pairCornerCutoutMargins != null) {
            this.mSystemIconsView.setPadding(((Integer) pairCornerCutoutMargins.first).intValue(), 0, ((Integer) pairCornerCutoutMargins.second).intValue(), 0);
        } else {
            this.mSystemIconsView.setPaddingRelative(getResources().getDimensionPixelSize(R.dimen.status_bar_padding_start), 0, getResources().getDimensionPixelSize(R.dimen.status_bar_padding_end), 0);
        }
        return super.onApplyWindowInsets(windowInsets);
    }

    @Override
    public void onDetachedFromWindow() {
        setListening(false);
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).removeIconGroup(this.mIconManager);
        super.onDetachedFromWindow();
    }

    public void setListening(boolean z) {
        if (z == this.mListening) {
            return;
        }
        this.mHeaderQsPanel.setListening(z);
        this.mListening = z;
        if (z) {
            this.mZenController.addCallback(this);
            this.mAlarmController.addCallback(this);
            this.mContext.registerReceiver(this.mRingerReceiver, new IntentFilter("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION"));
        } else {
            this.mZenController.removeCallback(this);
            this.mAlarmController.removeCallback(this);
            this.mContext.unregisterReceiver(this.mRingerReceiver);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == this.mClockView) {
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(new Intent("android.intent.action.SHOW_ALARMS"), 0);
        } else {
            if (view != this.mBatteryMeterView || BenesseExtension.getDchaState() != 0) {
                return;
            }
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(new Intent("android.intent.action.POWER_USAGE_SUMMARY"), 0);
        }
    }

    @Override
    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo alarmClockInfo) {
        this.mNextAlarm = alarmClockInfo;
        updateStatusText();
    }

    @Override
    public void onZenChanged(int i) {
        updateStatusText();
    }

    @Override
    public void onConfigChanged(ZenModeConfig zenModeConfig) {
        updateStatusText();
    }

    private void updateTooltipShow() {
        if (hasStatusText()) {
            hideLongPressTooltip(true);
        } else {
            hideStatusText();
        }
        updateHeaderTextContainerAlphaAnimator();
    }

    private boolean hasStatusText() {
        return this.mNextAlarmTextView.getVisibility() == 0 || this.mRingerModeTextView.getVisibility() == 0;
    }

    public void showLongPressTooltip() {
        if (!hasStatusText() && this.mShownCount < 2) {
            this.mLongPressTooltipView.animate().cancel();
            this.mLongPressTooltipView.setVisibility(0);
            this.mLongPressTooltipView.animate().alpha(1.0f).setDuration(300L).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    QuickStatusBarHeader.this.mHandler.postDelayed(QuickStatusBarHeader.this.mAutoFadeOutTooltipRunnable, 6000L);
                }
            }).start();
            if (getStoredShownCount() <= this.mShownCount) {
                Context context = this.mContext;
                int i = this.mShownCount + 1;
                this.mShownCount = i;
                Prefs.putInt(context, "QsLongPressTooltipShownCount", i);
            }
        }
    }

    private void hideLongPressTooltip(final boolean z) {
        this.mLongPressTooltipView.animate().cancel();
        if (this.mLongPressTooltipView.getVisibility() == 0 && this.mLongPressTooltipView.getAlpha() != 0.0f) {
            this.mHandler.removeCallbacks(this.mAutoFadeOutTooltipRunnable);
            this.mLongPressTooltipView.animate().alpha(0.0f).setDuration(300L).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    QuickStatusBarHeader.this.mLongPressTooltipView.setVisibility(4);
                    if (z) {
                        QuickStatusBarHeader.this.showStatus();
                    }
                }
            }).start();
        } else {
            this.mLongPressTooltipView.setVisibility(4);
            if (z) {
                showStatus();
            }
        }
    }

    private void showStatus() {
        this.mStatusContainer.setAlpha(0.0f);
        this.mStatusContainer.setVisibility(0);
        this.mStatusContainer.animate().alpha(1.0f).setDuration(300L).setListener(null).start();
    }

    private void hideStatusText() {
        if (this.mStatusContainer.getVisibility() == 0) {
            this.mStatusContainer.animate().alpha(0.0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    QuickStatusBarHeader.this.mStatusContainer.setVisibility(4);
                    QuickStatusBarHeader.this.mStatusContainer.setAlpha(1.0f);
                }
            }).start();
        }
    }

    public void updateEverything() {
        post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.setClickable(false);
            }
        });
    }

    public void setQSPanel(QSPanel qSPanel) {
        this.mQsPanel = qSPanel;
        setupHost(qSPanel.getHost());
    }

    public void setupHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
        this.mHeaderQsPanel.setQSPanelAndHeader(this.mQsPanel, this);
        this.mHeaderQsPanel.setHost(qSTileHost, null);
        this.mBatteryMeterView.setColorsFromContext(this.mHost.getContext());
        this.mBatteryMeterView.onDarkChanged(new Rect(), 0.0f, -1);
    }

    public void setCallback(QSDetail.Callback callback) {
        this.mHeaderQsPanel.setCallback(callback);
    }

    private String formatNextAlarm(AlarmManager.AlarmClockInfo alarmClockInfo) {
        if (alarmClockInfo == null) {
            return "";
        }
        return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(this.mContext, ActivityManager.getCurrentUser()) ? "EHm" : "Ehma"), alarmClockInfo.getTriggerTime()).toString();
    }

    public static float getColorIntensity(int i) {
        return i == -1 ? 0.0f : 1.0f;
    }

    public void setMargins(int i) {
        for (int i2 = 0; i2 < getChildCount(); i2++) {
            View childAt = getChildAt(i2);
            if (childAt != this.mSystemIconsView && childAt != this.mQuickQsStatusIcons && childAt != this.mHeaderQsPanel) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) childAt.getLayoutParams();
                layoutParams.leftMargin = i;
                layoutParams.rightMargin = i;
            }
        }
    }
}
