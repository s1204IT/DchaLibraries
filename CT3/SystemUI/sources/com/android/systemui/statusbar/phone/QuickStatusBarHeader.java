package com.android.systemui.statusbar.phone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.BenesseExtension;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QuickQSPanel;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.tuner.TunerService;

public class QuickStatusBarHeader extends BaseStatusBarHeader implements NextAlarmController.NextAlarmChangeCallback, View.OnClickListener, UserInfoController.OnUserInfoChangedListener {
    private ActivityStarter mActivityStarter;
    private boolean mAlarmShowing;
    private TextView mAlarmStatus;
    private View mAlarmStatusCollapsed;
    private TouchAnimator mAlarmTranslation;
    private float mDateScaleFactor;
    private TouchAnimator mDateSizeAnimator;
    private ViewGroup mDateTimeAlarmGroup;
    private float mDateTimeAlarmTranslation;
    private ViewGroup mDateTimeGroup;
    private float mDateTimeTranslation;
    private TextView mEmergencyOnly;
    protected ExpandableIndicator mExpandIndicator;
    private boolean mExpanded;
    private float mExpansionAmount;
    private TouchAnimator mFirstHalfAnimator;
    protected float mGearTranslation;
    private QuickQSPanel mHeaderQsPanel;
    private QSTileHost mHost;
    private boolean mListening;
    private ImageView mMultiUserAvatar;
    protected MultiUserSwitch mMultiUserSwitch;
    private AlarmManager.AlarmClockInfo mNextAlarm;
    private NextAlarmController mNextAlarmController;
    private QSPanel mQsPanel;
    private TouchAnimator mSecondHalfAnimator;
    protected TouchAnimator mSettingsAlpha;
    private SettingsButton mSettingsButton;
    protected View mSettingsContainer;
    private boolean mShowEmergencyCallsOnly;
    private boolean mShowFullAlarm;

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mEmergencyOnly = (TextView) findViewById(R.id.header_emergency_calls_only);
        this.mDateTimeAlarmGroup = (ViewGroup) findViewById(R.id.date_time_alarm_group);
        this.mDateTimeAlarmGroup.findViewById(R.id.empty_time_view).setVisibility(8);
        this.mDateTimeGroup = (ViewGroup) findViewById(R.id.date_time_group);
        this.mDateTimeGroup.setPivotX(0.0f);
        this.mDateTimeGroup.setPivotY(0.0f);
        this.mShowFullAlarm = getResources().getBoolean(R.bool.quick_settings_show_full_alarm);
        this.mExpandIndicator = (ExpandableIndicator) findViewById(R.id.expand_indicator);
        this.mHeaderQsPanel = (QuickQSPanel) findViewById(R.id.quick_qs_panel);
        this.mSettingsButton = (SettingsButton) findViewById(R.id.settings_button);
        this.mSettingsContainer = findViewById(R.id.settings_button_container);
        this.mSettingsButton.setOnClickListener(this);
        this.mAlarmStatusCollapsed = findViewById(R.id.alarm_status_collapsed);
        this.mAlarmStatus = (TextView) findViewById(R.id.alarm_status);
        this.mAlarmStatus.setOnClickListener(this);
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) this.mMultiUserSwitch.findViewById(R.id.multi_user_avatar);
        ((RippleDrawable) this.mSettingsButton.getBackground()).setForceSoftware(true);
        ((RippleDrawable) this.mExpandIndicator.getBackground()).setForceSoftware(true);
        updateResources();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateResources();
    }

    private void updateResources() {
        FontSizeUtils.updateFontSize(this.mAlarmStatus, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(this.mEmergencyOnly, R.dimen.qs_emergency_calls_only_text_size);
        this.mGearTranslation = this.mContext.getResources().getDimension(R.dimen.qs_header_gear_translation);
        this.mDateTimeTranslation = this.mContext.getResources().getDimension(R.dimen.qs_date_anim_translation);
        this.mDateTimeAlarmTranslation = this.mContext.getResources().getDimension(R.dimen.qs_date_alarm_anim_translation);
        float dateCollapsedSize = this.mContext.getResources().getDimension(R.dimen.qs_date_collapsed_text_size);
        float dateExpandedSize = this.mContext.getResources().getDimension(R.dimen.qs_date_text_size);
        this.mDateScaleFactor = dateExpandedSize / dateCollapsedSize;
        updateDateTimePosition();
        this.mSecondHalfAnimator = new TouchAnimator.Builder().addFloat(this.mShowFullAlarm ? this.mAlarmStatus : findViewById(R.id.date), "alpha", 0.0f, 1.0f).addFloat(this.mEmergencyOnly, "alpha", 0.0f, 1.0f).setStartDelay(0.5f).build();
        if (this.mShowFullAlarm) {
            this.mFirstHalfAnimator = new TouchAnimator.Builder().addFloat(this.mAlarmStatusCollapsed, "alpha", 1.0f, 0.0f).setEndDelay(0.5f).build();
        }
        this.mDateSizeAnimator = new TouchAnimator.Builder().addFloat(this.mDateTimeGroup, "scaleX", 1.0f, this.mDateScaleFactor).addFloat(this.mDateTimeGroup, "scaleY", 1.0f, this.mDateScaleFactor).setStartDelay(0.36f).build();
        updateSettingsAnimator();
    }

    protected void updateSettingsAnimator() {
        this.mSettingsAlpha = new TouchAnimator.Builder().addFloat(this.mSettingsContainer, "translationY", -this.mGearTranslation, 0.0f).addFloat(this.mMultiUserSwitch, "translationY", -this.mGearTranslation, 0.0f).addFloat(this.mSettingsButton, "rotation", -90.0f, 0.0f).addFloat(this.mSettingsContainer, "alpha", 0.0f, 1.0f).addFloat(this.mMultiUserSwitch, "alpha", 0.0f, 1.0f).setStartDelay(0.7f).build();
        boolean isRtl = isLayoutRtl();
        if (isRtl && this.mDateTimeGroup.getWidth() == 0) {
            this.mDateTimeGroup.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    QuickStatusBarHeader.this.mDateTimeGroup.setPivotX(QuickStatusBarHeader.this.getWidth());
                    QuickStatusBarHeader.this.mDateTimeGroup.removeOnLayoutChangeListener(this);
                }
            });
        } else {
            this.mDateTimeGroup.setPivotX(isRtl ? this.mDateTimeGroup.getWidth() : 0);
        }
    }

    @Override
    public int getCollapsedHeight() {
        return getHeight();
    }

    @Override
    public void setExpanded(boolean expanded) {
        this.mExpanded = expanded;
        this.mHeaderQsPanel.setExpanded(expanded);
        updateEverything();
    }

    @Override
    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo nextAlarm) {
        this.mNextAlarm = nextAlarm;
        if (nextAlarm != null) {
            String alarmString = KeyguardStatusView.formatNextAlarm(getContext(), nextAlarm);
            this.mAlarmStatus.setText(alarmString);
            this.mAlarmStatus.setContentDescription(this.mContext.getString(R.string.accessibility_quick_settings_alarm, alarmString));
            this.mAlarmStatusCollapsed.setContentDescription(this.mContext.getString(R.string.accessibility_quick_settings_alarm, alarmString));
        }
        if (this.mAlarmShowing == (nextAlarm != null)) {
            return;
        }
        this.mAlarmShowing = nextAlarm != null;
        updateEverything();
    }

    @Override
    public void setExpansion(float headerExpansionFraction) {
        this.mExpansionAmount = headerExpansionFraction;
        this.mSecondHalfAnimator.setPosition(headerExpansionFraction);
        if (this.mShowFullAlarm) {
            this.mFirstHalfAnimator.setPosition(headerExpansionFraction);
        }
        this.mDateSizeAnimator.setPosition(headerExpansionFraction);
        this.mAlarmTranslation.setPosition(headerExpansionFraction);
        this.mSettingsAlpha.setPosition(headerExpansionFraction);
        updateAlarmVisibilities();
        this.mExpandIndicator.setExpanded(headerExpansionFraction > 0.93f);
    }

    @Override
    protected void onDetachedFromWindow() {
        setListening(false);
        this.mHost.getUserInfoController().remListener(this);
        this.mHost.getNetworkController().removeEmergencyListener(this);
        super.onDetachedFromWindow();
    }

    private void updateAlarmVisibilities() {
        this.mAlarmStatus.setVisibility((this.mAlarmShowing && this.mShowFullAlarm) ? 0 : 4);
        this.mAlarmStatusCollapsed.setVisibility(this.mAlarmShowing ? 0 : 4);
    }

    private void updateDateTimePosition() {
        TouchAnimator.Builder builder = new TouchAnimator.Builder();
        ViewGroup viewGroup = this.mDateTimeAlarmGroup;
        float[] fArr = new float[2];
        fArr[0] = 0.0f;
        fArr[1] = this.mAlarmShowing ? this.mDateTimeAlarmTranslation : this.mDateTimeTranslation;
        this.mAlarmTranslation = builder.addFloat(viewGroup, "translationY", fArr).build();
        this.mAlarmTranslation.setPosition(this.mExpansionAmount);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening == this.mListening) {
            return;
        }
        this.mHeaderQsPanel.setListening(listening);
        this.mListening = listening;
        updateListeners();
    }

    @Override
    public void updateEverything() {
        updateDateTimePosition();
        updateVisibilities();
        setClickable(false);
    }

    protected void updateVisibilities() {
        updateAlarmVisibilities();
        this.mEmergencyOnly.setVisibility((this.mExpanded && this.mShowEmergencyCallsOnly) ? 0 : 4);
        this.mSettingsContainer.setVisibility(this.mExpanded ? 0 : 4);
        this.mSettingsContainer.findViewById(R.id.tuner_icon).setVisibility(TunerService.isTunerEnabled(this.mContext) ? 0 : 4);
        this.mMultiUserSwitch.setVisibility((this.mExpanded && this.mMultiUserSwitch.hasMultipleUsers()) ? 0 : 4);
    }

    private void updateListeners() {
        if (this.mListening) {
            this.mNextAlarmController.addStateChangedCallback(this);
        } else {
            this.mNextAlarmController.removeStateChangedCallback(this);
        }
    }

    @Override
    public void setActivityStarter(ActivityStarter activityStarter) {
        this.mActivityStarter = activityStarter;
    }

    @Override
    public void setQSPanel(QSPanel qsPanel) {
        this.mQsPanel = qsPanel;
        setupHost(qsPanel.getHost());
        if (this.mQsPanel == null) {
            return;
        }
        this.mMultiUserSwitch.setQsPanel(qsPanel);
    }

    public void setupHost(QSTileHost host) {
        this.mHost = host;
        host.setHeaderView(this.mExpandIndicator);
        this.mHeaderQsPanel.setQSPanelAndHeader(this.mQsPanel, this);
        this.mHeaderQsPanel.setHost(host, null);
        setUserInfoController(host.getUserInfoController());
        setBatteryController(host.getBatteryController());
        setNextAlarmController(host.getNextAlarmController());
        boolean isAPhone = this.mHost.getNetworkController().hasVoiceCallingFeature();
        if (!isAPhone) {
            return;
        }
        this.mHost.getNetworkController().addEmergencyListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == this.mSettingsButton) {
            MetricsLogger.action(this.mContext, 406);
            if (this.mSettingsButton.isTunerClick()) {
                this.mHost.startRunnableDismissingKeyguard(new Runnable() {
                    @Override
                    public void run() {
                        QuickStatusBarHeader.this.m1505x4530d582();
                    }
                });
                return;
            } else {
                m1507x4530d584();
                return;
            }
        }
        if (v != this.mAlarmStatus || this.mNextAlarm == null) {
            return;
        }
        PendingIntent showIntent = this.mNextAlarm.getShowIntent();
        this.mActivityStarter.startPendingIntentDismissingKeyguard(showIntent);
    }

    void m1505x4530d582() {
        post(new Runnable() {
            @Override
            public void run() {
                this.val$this.m1506x4530d583();
            }
        });
    }

    void m1506x4530d583() {
        if (TunerService.isTunerEnabled(this.mContext)) {
            TunerService.showResetRequest(this.mContext, new Runnable() {
                @Override
                public void run() {
                    this.val$this.m1507x4530d584();
                }
            });
        } else {
            Toast.makeText(getContext(), R.string.tuner_toast, 1).show();
            TunerService.setTunerEnabled(this.mContext, true);
        }
        m1507x4530d584();
    }

    public void m1507x4530d584() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        this.mActivityStarter.startActivity(new Intent("android.settings.SETTINGS"), true);
    }

    public void setNextAlarmController(NextAlarmController nextAlarmController) {
        this.mNextAlarmController = nextAlarmController;
    }

    public void setBatteryController(BatteryController batteryController) {
    }

    public void setUserInfoController(UserInfoController userInfoController) {
        userInfoController.addListener(this);
    }

    @Override
    public void setCallback(QSPanel.Callback qsPanelCallback) {
        this.mHeaderQsPanel.setCallback(qsPanelCallback);
    }

    @Override
    public void setEmergencyCallsOnly(boolean show) {
        boolean changed = show != this.mShowEmergencyCallsOnly;
        if (!changed) {
            return;
        }
        this.mShowEmergencyCallsOnly = show;
        if (!this.mExpanded) {
            return;
        }
        updateEverything();
    }

    @Override
    public void onUserInfoChanged(String name, Drawable picture) {
        this.mMultiUserAvatar.setImageDrawable(picture);
    }
}
