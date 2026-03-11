package com.android.systemui.statusbar.phone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.RippleDrawable;
import android.os.BenesseExtension;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.tuner.TunerService;
import java.text.NumberFormat;

public class StatusBarHeaderView extends BaseStatusBarHeader implements View.OnClickListener, BatteryController.BatteryStateChangeCallback, NextAlarmController.NextAlarmChangeCallback, NetworkController.EmergencyListener {
    private ActivityStarter mActivityStarter;
    private boolean mAlarmShowing;
    private TextView mAlarmStatus;
    private boolean mAllowExpand;
    private TextView mAmPm;
    private float mAvatarCollapsedScaleFactor;
    private BatteryController mBatteryController;
    private TextView mBatteryLevel;
    private boolean mCaptureValues;
    private final Rect mClipBounds;
    private View mClock;
    private float mClockCollapsedScaleFactor;
    private int mClockCollapsedSize;
    private int mClockExpandedSize;
    private int mClockMarginBottomCollapsed;
    private int mClockMarginBottomExpanded;
    private int mCollapsedHeight;
    private final LayoutValues mCollapsedValues;
    private float mCurrentT;
    private final LayoutValues mCurrentValues;
    private TextView mDateCollapsed;
    private TextView mDateExpanded;
    private View mDateGroup;
    private boolean mDetailTransitioning;
    private TextView mEmergencyCallsOnly;
    private boolean mExpanded;
    private int mExpandedHeight;
    private final LayoutValues mExpandedValues;
    private boolean mListening;
    private ImageView mMultiUserAvatar;
    private int mMultiUserCollapsedMargin;
    private int mMultiUserExpandedMargin;
    private MultiUserSwitch mMultiUserSwitch;
    private int mMultiUserSwitchWidthCollapsed;
    private int mMultiUserSwitchWidthExpanded;
    private AlarmManager.AlarmClockInfo mNextAlarm;
    private NextAlarmController mNextAlarmController;
    private QSPanel mQSPanel;
    private View mQsDetailHeader;
    private ImageView mQsDetailHeaderProgress;
    private Switch mQsDetailHeaderSwitch;
    private TextView mQsDetailHeaderTitle;
    private final QSPanel.Callback mQsPanelCallback;
    private SettingsButton mSettingsButton;
    private View mSettingsContainer;
    private boolean mShowEmergencyCallsOnly;
    private boolean mShowingDetail;
    private View mSignalCluster;
    private boolean mSignalClusterDetached;
    private LinearLayout mSystemIcons;
    private ViewGroup mSystemIconsContainer;
    private View mSystemIconsSuperContainer;
    private TextView mTime;

    public StatusBarHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutValues layoutValues = null;
        this.mClipBounds = new Rect();
        this.mCollapsedValues = new LayoutValues(layoutValues);
        this.mExpandedValues = new LayoutValues(layoutValues);
        this.mCurrentValues = new LayoutValues(layoutValues);
        this.mAllowExpand = true;
        this.mQsPanelCallback = new QSPanel.Callback() {
            private boolean mScanState;

            @Override
            public void onToggleStateChanged(final boolean state) {
                StatusBarHeaderView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        handleToggleStateChanged(state);
                    }
                });
            }

            @Override
            public void onShowingDetail(final QSTile.DetailAdapter detail, int x, int y) {
                StatusBarHeaderView.this.mDetailTransitioning = true;
                StatusBarHeaderView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        handleShowingDetail(detail);
                    }
                });
            }

            @Override
            public void onScanStateChanged(final boolean state) {
                StatusBarHeaderView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        handleScanStateChanged(state);
                    }
                });
            }

            public void handleToggleStateChanged(boolean state) {
                StatusBarHeaderView.this.mQsDetailHeaderSwitch.setChecked(state);
            }

            public void handleScanStateChanged(boolean state) {
                if (this.mScanState == state) {
                    return;
                }
                this.mScanState = state;
                Animatable anim = (Animatable) StatusBarHeaderView.this.mQsDetailHeaderProgress.getDrawable();
                if (state) {
                    StatusBarHeaderView.this.mQsDetailHeaderProgress.animate().alpha(1.0f);
                    anim.start();
                } else {
                    StatusBarHeaderView.this.mQsDetailHeaderProgress.animate().alpha(0.0f);
                    anim.stop();
                }
            }

            public void handleShowingDetail(final QSTile.DetailAdapter detail) {
                boolean showingDetail = detail != null;
                transition(StatusBarHeaderView.this.mClock, !showingDetail);
                transition(StatusBarHeaderView.this.mDateGroup, !showingDetail);
                if (StatusBarHeaderView.this.mAlarmShowing) {
                    transition(StatusBarHeaderView.this.mAlarmStatus, !showingDetail);
                }
                transition(StatusBarHeaderView.this.mQsDetailHeader, showingDetail);
                StatusBarHeaderView.this.mShowingDetail = showingDetail;
                if (showingDetail) {
                    StatusBarHeaderView.this.mQsDetailHeaderTitle.setText(detail.getTitle());
                    Boolean toggleState = detail.getToggleState();
                    if (toggleState == null) {
                        StatusBarHeaderView.this.mQsDetailHeaderSwitch.setVisibility(4);
                        StatusBarHeaderView.this.mQsDetailHeader.setClickable(false);
                        return;
                    } else {
                        StatusBarHeaderView.this.mQsDetailHeaderSwitch.setVisibility(0);
                        StatusBarHeaderView.this.mQsDetailHeaderSwitch.setChecked(toggleState.booleanValue());
                        StatusBarHeaderView.this.mQsDetailHeader.setClickable(true);
                        StatusBarHeaderView.this.mQsDetailHeader.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                boolean checked = !StatusBarHeaderView.this.mQsDetailHeaderSwitch.isChecked();
                                StatusBarHeaderView.this.mQsDetailHeaderSwitch.setChecked(checked);
                                detail.setToggleState(checked);
                            }
                        });
                        return;
                    }
                }
                StatusBarHeaderView.this.mQsDetailHeader.setClickable(false);
            }

            private void transition(final View v, final boolean in) {
                if (in) {
                    v.bringToFront();
                    v.setVisibility(0);
                }
                if (v.hasOverlappingRendering()) {
                    v.animate().withLayer();
                }
                v.animate().alpha(in ? 1 : 0).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (!in) {
                            v.setVisibility(4);
                        }
                        StatusBarHeaderView.this.mDetailTransitioning = false;
                    }
                }).start();
            }
        };
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSystemIconsSuperContainer = findViewById(R.id.system_icons_super_container);
        this.mSystemIconsContainer = (ViewGroup) findViewById(R.id.system_icons_container);
        this.mSystemIconsSuperContainer.setOnClickListener(this);
        this.mDateGroup = findViewById(R.id.date_group);
        this.mClock = findViewById(R.id.clock);
        this.mTime = (TextView) findViewById(R.id.time_view);
        this.mAmPm = (TextView) findViewById(R.id.am_pm_view);
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) findViewById(R.id.multi_user_avatar);
        this.mDateCollapsed = (TextView) findViewById(R.id.date_collapsed);
        this.mDateExpanded = (TextView) findViewById(R.id.date_expanded);
        this.mSettingsButton = (SettingsButton) findViewById(R.id.settings_button);
        this.mSettingsContainer = findViewById(R.id.settings_button_container);
        this.mSettingsButton.setOnClickListener(this);
        this.mQsDetailHeader = findViewById(R.id.qs_detail_header);
        this.mQsDetailHeader.setAlpha(0.0f);
        this.mQsDetailHeaderTitle = (TextView) this.mQsDetailHeader.findViewById(android.R.id.title);
        this.mQsDetailHeaderSwitch = (Switch) this.mQsDetailHeader.findViewById(android.R.id.toggle);
        this.mQsDetailHeaderProgress = (ImageView) findViewById(R.id.qs_detail_header_progress);
        this.mEmergencyCallsOnly = (TextView) findViewById(R.id.header_emergency_calls_only);
        this.mBatteryLevel = (TextView) findViewById(R.id.battery_level);
        this.mAlarmStatus = (TextView) findViewById(R.id.alarm_status);
        this.mAlarmStatus.setOnClickListener(this);
        this.mSignalCluster = findViewById(R.id.signal_cluster);
        this.mSystemIcons = (LinearLayout) findViewById(R.id.system_icons);
        loadDimens();
        updateVisibilities();
        updateClockScale();
        updateAvatarScale();
        addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right - left != oldRight - oldLeft) {
                    StatusBarHeaderView.this.setClipping(StatusBarHeaderView.this.getHeight());
                }
                boolean rtl = StatusBarHeaderView.this.getLayoutDirection() == 1;
                StatusBarHeaderView.this.mTime.setPivotX(rtl ? StatusBarHeaderView.this.mTime.getWidth() : 0);
                StatusBarHeaderView.this.mTime.setPivotY(StatusBarHeaderView.this.mTime.getBaseline());
                StatusBarHeaderView.this.updateAmPmTranslation();
            }
        });
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRect(StatusBarHeaderView.this.mClipBounds);
            }
        });
        requestCaptureValues();
        ((RippleDrawable) getBackground()).setForceSoftware(true);
        ((RippleDrawable) this.mSettingsButton.getBackground()).setForceSoftware(true);
        ((RippleDrawable) this.mSystemIconsSuperContainer.getBackground()).setForceSoftware(true);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (this.mCaptureValues) {
            if (this.mExpanded) {
                captureLayoutValues(this.mExpandedValues);
            } else {
                captureLayoutValues(this.mCollapsedValues);
            }
            this.mCaptureValues = false;
            updateLayoutValues(this.mCurrentT);
        }
        this.mAlarmStatus.setX(this.mDateGroup.getLeft() + this.mDateCollapsed.getRight());
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this.mBatteryLevel, R.dimen.battery_level_text_size);
        FontSizeUtils.updateFontSize(this.mEmergencyCallsOnly, R.dimen.qs_emergency_calls_only_text_size);
        FontSizeUtils.updateFontSize(this.mDateCollapsed, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(this.mDateExpanded, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(this.mAlarmStatus, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(this, android.R.id.title, R.dimen.qs_detail_header_text_size);
        FontSizeUtils.updateFontSize(this, android.R.id.toggle, R.dimen.qs_detail_header_text_size);
        FontSizeUtils.updateFontSize(this.mAmPm, R.dimen.qs_time_collapsed_size);
        FontSizeUtils.updateFontSize(this, R.id.empty_time_view, R.dimen.qs_time_expanded_size);
        this.mEmergencyCallsOnly.setText(android.R.string.config_notificationHandlerPackage);
        this.mClockCollapsedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size);
        this.mClockExpandedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_expanded_size);
        this.mClockCollapsedScaleFactor = this.mClockCollapsedSize / this.mClockExpandedSize;
        updateClockScale();
        updateClockCollapsedMargin();
    }

    private void updateClockCollapsedMargin() {
        Resources res = getResources();
        int padding = res.getDimensionPixelSize(R.dimen.clock_collapsed_bottom_margin);
        int largePadding = res.getDimensionPixelSize(R.dimen.clock_collapsed_bottom_margin_large_text);
        float largeFactor = (MathUtils.constrain(getResources().getConfiguration().fontScale, 1.0f, 1.3f) - 1.0f) / 0.29999995f;
        this.mClockMarginBottomCollapsed = Math.round(((1.0f - largeFactor) * padding) + (largePadding * largeFactor));
        requestLayout();
    }

    private void requestCaptureValues() {
        this.mCaptureValues = true;
        requestLayout();
    }

    private void loadDimens() {
        this.mCollapsedHeight = getResources().getDimensionPixelSize(R.dimen.status_bar_header_height);
        this.mExpandedHeight = getResources().getDimensionPixelSize(R.dimen.status_bar_header_height_expanded);
        this.mMultiUserExpandedMargin = getResources().getDimensionPixelSize(R.dimen.multi_user_switch_expanded_margin);
        this.mMultiUserCollapsedMargin = getResources().getDimensionPixelSize(R.dimen.multi_user_switch_collapsed_margin);
        this.mClockMarginBottomExpanded = getResources().getDimensionPixelSize(R.dimen.clock_expanded_bottom_margin);
        updateClockCollapsedMargin();
        this.mMultiUserSwitchWidthCollapsed = getResources().getDimensionPixelSize(R.dimen.multi_user_switch_width_collapsed);
        this.mMultiUserSwitchWidthExpanded = getResources().getDimensionPixelSize(R.dimen.multi_user_switch_width_expanded);
        this.mAvatarCollapsedScaleFactor = getResources().getDimensionPixelSize(R.dimen.multi_user_avatar_collapsed_size) / this.mMultiUserAvatar.getLayoutParams().width;
        this.mClockCollapsedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size);
        this.mClockExpandedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_expanded_size);
        this.mClockCollapsedScaleFactor = this.mClockCollapsedSize / this.mClockExpandedSize;
    }

    @Override
    public void setActivityStarter(ActivityStarter activityStarter) {
        this.mActivityStarter = activityStarter;
    }

    @Override
    public int getCollapsedHeight() {
        return this.mCollapsedHeight;
    }

    @Override
    public void setListening(boolean listening) {
        if (listening == this.mListening) {
            return;
        }
        this.mListening = listening;
        updateListeners();
    }

    @Override
    public void setExpanded(boolean expanded) {
        if (!this.mAllowExpand) {
            expanded = false;
        }
        boolean changed = expanded != this.mExpanded;
        this.mExpanded = expanded;
        if (!changed) {
            return;
        }
        updateEverything();
    }

    @Override
    public void updateEverything() {
        updateHeights();
        updateVisibilities();
        updateSystemIconsLayoutParams();
        updateClickTargets();
        updateMultiUserSwitch();
        updateClockScale();
        updateAvatarScale();
        updateClockLp();
        requestCaptureValues();
    }

    private void updateHeights() {
        int height = this.mExpanded ? this.mExpandedHeight : this.mCollapsedHeight;
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp.height == height) {
            return;
        }
        lp.height = height;
        setLayoutParams(lp);
    }

    private void updateVisibilities() {
        Log.d("StatusBarHeaderView", "updateVisibilities: " + this.mExpanded + ", " + this.mAlarmShowing + ", " + this.mShowingDetail + ", " + this.mShowEmergencyCallsOnly);
        this.mDateCollapsed.setVisibility((this.mExpanded && this.mAlarmShowing) ? 0 : 4);
        this.mDateExpanded.setVisibility((this.mExpanded && this.mAlarmShowing) ? 4 : 0);
        this.mAlarmStatus.setVisibility((this.mExpanded && this.mAlarmShowing) ? 0 : 4);
        this.mSettingsContainer.setVisibility(this.mExpanded ? 0 : 4);
        this.mQsDetailHeader.setVisibility((this.mExpanded && this.mShowingDetail) ? 0 : 4);
        if (this.mSignalCluster != null) {
            updateSignalClusterDetachment();
        }
        this.mEmergencyCallsOnly.setVisibility((this.mExpanded && this.mShowEmergencyCallsOnly) ? 0 : 8);
        this.mBatteryLevel.setVisibility(this.mExpanded ? 0 : 8);
        this.mSettingsContainer.findViewById(R.id.tuner_icon).setVisibility(TunerService.isTunerEnabled(this.mContext) ? 0 : 4);
    }

    private void updateSignalClusterDetachment() {
        boolean detached = this.mExpanded;
        if (detached != this.mSignalClusterDetached) {
            if (detached) {
                getOverlay().add(this.mSignalCluster);
            } else {
                reattachSignalCluster();
            }
        }
        this.mSignalClusterDetached = detached;
    }

    private void reattachSignalCluster() {
        getOverlay().remove(this.mSignalCluster);
        this.mSystemIcons.addView(this.mSignalCluster, 1);
    }

    private void updateSystemIconsLayoutParams() {
        int rule;
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) this.mSystemIconsSuperContainer.getLayoutParams();
        if (this.mExpanded) {
            rule = this.mSettingsContainer.getId();
        } else {
            rule = this.mMultiUserSwitch.getId();
        }
        if (rule == lp.getRules()[16]) {
            return;
        }
        lp.addRule(16, rule);
        this.mSystemIconsSuperContainer.setLayoutParams(lp);
    }

    private void updateListeners() {
        if (this.mListening) {
            this.mBatteryController.addStateChangedCallback(this);
            this.mNextAlarmController.addStateChangedCallback(this);
        } else {
            this.mBatteryController.removeStateChangedCallback(this);
            this.mNextAlarmController.removeStateChangedCallback(this);
        }
    }

    private void updateAvatarScale() {
        if (this.mExpanded) {
            this.mMultiUserAvatar.setScaleX(1.0f);
            this.mMultiUserAvatar.setScaleY(1.0f);
        } else {
            this.mMultiUserAvatar.setScaleX(this.mAvatarCollapsedScaleFactor);
            this.mMultiUserAvatar.setScaleY(this.mAvatarCollapsedScaleFactor);
        }
    }

    private void updateClockScale() {
        int i;
        TextView textView = this.mTime;
        if (this.mExpanded) {
            i = this.mClockExpandedSize;
        } else {
            i = this.mClockCollapsedSize;
        }
        textView.setTextSize(0, i);
        this.mTime.setScaleX(1.0f);
        this.mTime.setScaleY(1.0f);
        updateAmPmTranslation();
    }

    public void updateAmPmTranslation() {
        boolean rtl = getLayoutDirection() == 1;
        this.mAmPm.setTranslationX((rtl ? 1 : -1) * this.mTime.getWidth() * (1.0f - this.mTime.getScaleX()));
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        String percentage = NumberFormat.getPercentInstance().format(((double) level) / 100.0d);
        this.mBatteryLevel.setText(percentage);
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
    }

    @Override
    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo nextAlarm) {
        this.mNextAlarm = nextAlarm;
        if (nextAlarm != null) {
            this.mAlarmStatus.setText(KeyguardStatusView.formatNextAlarm(getContext(), nextAlarm));
        }
        this.mAlarmShowing = nextAlarm != null;
        updateEverything();
        requestCaptureValues();
    }

    private void updateClickTargets() {
        boolean z = false;
        this.mMultiUserSwitch.setClickable(this.mExpanded);
        this.mMultiUserSwitch.setFocusable(this.mExpanded);
        this.mSystemIconsSuperContainer.setClickable(this.mExpanded);
        this.mSystemIconsSuperContainer.setFocusable(this.mExpanded);
        TextView textView = this.mAlarmStatus;
        if (this.mNextAlarm != null && this.mNextAlarm.getShowIntent() != null) {
            z = true;
        }
        textView.setClickable(z);
    }

    private void updateClockLp() {
        int marginBottom;
        if (this.mExpanded) {
            marginBottom = this.mClockMarginBottomExpanded;
        } else {
            marginBottom = this.mClockMarginBottomCollapsed;
        }
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) this.mDateGroup.getLayoutParams();
        if (marginBottom == lp.bottomMargin) {
            return;
        }
        lp.bottomMargin = marginBottom;
        this.mDateGroup.setLayoutParams(lp);
    }

    private void updateMultiUserSwitch() {
        int marginEnd;
        int width;
        if (this.mExpanded) {
            marginEnd = this.mMultiUserExpandedMargin;
            width = this.mMultiUserSwitchWidthExpanded;
        } else {
            marginEnd = this.mMultiUserCollapsedMargin;
            width = this.mMultiUserSwitchWidthCollapsed;
        }
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) this.mMultiUserSwitch.getLayoutParams();
        if (marginEnd == lp.getMarginEnd() && lp.width == width) {
            return;
        }
        lp.setMarginEnd(marginEnd);
        lp.width = width;
        this.mMultiUserSwitch.setLayoutParams(lp);
    }

    @Override
    public void setExpansion(float t) {
        if (!this.mExpanded) {
            t = 0.0f;
        }
        this.mCurrentT = t;
        float height = this.mCollapsedHeight + ((this.mExpandedHeight - this.mCollapsedHeight) * t);
        if (height < this.mCollapsedHeight) {
            height = this.mCollapsedHeight;
        }
        if (height > this.mExpandedHeight) {
            height = this.mExpandedHeight;
        }
        setClipping(height);
        updateLayoutValues(t);
    }

    private void updateLayoutValues(float t) {
        if (this.mCaptureValues) {
            return;
        }
        this.mCurrentValues.interpoloate(this.mCollapsedValues, this.mExpandedValues, t);
        applyLayoutValues(this.mCurrentValues);
    }

    public void setClipping(float height) {
        this.mClipBounds.set(getPaddingLeft(), 0, getWidth() - getPaddingRight(), (int) height);
        setClipBounds(this.mClipBounds);
        invalidateOutline();
    }

    @Override
    public void setCallback(QSPanel.Callback qsPanelCallback) {
    }

    @Override
    public void onClick(View v) {
        PendingIntent showIntent;
        if (v == this.mSettingsButton) {
            if (this.mSettingsButton.isTunerClick()) {
                if (TunerService.isTunerEnabled(this.mContext)) {
                    TunerService.showResetRequest(this.mContext, new Runnable() {
                        @Override
                        public void run() {
                            StatusBarHeaderView.this.startSettingsActivity();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), R.string.tuner_toast, 1).show();
                    TunerService.setTunerEnabled(this.mContext, true);
                }
            }
            startSettingsActivity();
            return;
        }
        if (v == this.mSystemIconsSuperContainer) {
            startBatteryActivity();
        } else {
            if (v != this.mAlarmStatus || this.mNextAlarm == null || (showIntent = this.mNextAlarm.getShowIntent()) == null) {
                return;
            }
            this.mActivityStarter.startPendingIntentDismissingKeyguard(showIntent);
        }
    }

    public void startSettingsActivity() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        this.mActivityStarter.startActivity(new Intent("android.settings.SETTINGS"), true);
    }

    private void startBatteryActivity() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        this.mActivityStarter.startActivity(new Intent("android.intent.action.POWER_USAGE_SUMMARY"), true);
    }

    @Override
    public void setQSPanel(QSPanel qsp) {
        this.mQSPanel = qsp;
        if (this.mQSPanel != null) {
            this.mQSPanel.setCallback(this.mQsPanelCallback);
        }
        this.mMultiUserSwitch.setQsPanel(qsp);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    public void setEmergencyCallsOnly(boolean show) {
        Log.d("StatusBarHeaderView", "setShowEmergencyCallsOnly: show= " + show + ", mShowEmergencyCallsOnly= " + this.mShowEmergencyCallsOnly + ", mExpanded= " + this.mExpanded);
        boolean changed = show != this.mShowEmergencyCallsOnly;
        if (changed) {
            this.mShowEmergencyCallsOnly = show;
            if (this.mExpanded) {
                updateEverything();
                requestCaptureValues();
            }
        }
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
    }

    private void captureLayoutValues(LayoutValues target) {
        target.timeScale = this.mExpanded ? 1.0f : this.mClockCollapsedScaleFactor;
        target.clockY = this.mClock.getBottom();
        target.dateY = this.mDateGroup.getTop();
        target.emergencyCallsOnlyAlpha = getAlphaForVisibility(this.mEmergencyCallsOnly);
        target.alarmStatusAlpha = getAlphaForVisibility(this.mAlarmStatus);
        target.dateCollapsedAlpha = getAlphaForVisibility(this.mDateCollapsed);
        target.dateExpandedAlpha = getAlphaForVisibility(this.mDateExpanded);
        target.avatarScale = this.mMultiUserAvatar.getScaleX();
        target.avatarX = this.mMultiUserSwitch.getLeft() + this.mMultiUserAvatar.getLeft();
        target.avatarY = this.mMultiUserSwitch.getTop() + this.mMultiUserAvatar.getTop();
        if (getLayoutDirection() == 0) {
            target.batteryX = this.mSystemIconsSuperContainer.getLeft() + this.mSystemIconsContainer.getRight();
        } else {
            target.batteryX = this.mSystemIconsSuperContainer.getLeft() + this.mSystemIconsContainer.getLeft();
        }
        target.batteryY = this.mSystemIconsSuperContainer.getTop() + this.mSystemIconsContainer.getTop();
        target.batteryLevelAlpha = getAlphaForVisibility(this.mBatteryLevel);
        target.settingsAlpha = getAlphaForVisibility(this.mSettingsContainer);
        target.settingsTranslation = this.mExpanded ? 0 : this.mMultiUserSwitch.getLeft() - this.mSettingsContainer.getLeft();
        target.signalClusterAlpha = this.mSignalClusterDetached ? 0.0f : 1.0f;
        target.settingsRotation = this.mExpanded ? 0.0f : 90.0f;
    }

    private float getAlphaForVisibility(View v) {
        return (v == null || v.getVisibility() == 0) ? 1.0f : 0.0f;
    }

    private void applyAlpha(View v, float alpha) {
        if (v == null || v.getVisibility() == 8) {
            return;
        }
        if (alpha == 0.0f) {
            v.setVisibility(4);
        } else {
            v.setVisibility(0);
            v.setAlpha(alpha);
        }
    }

    private void applyLayoutValues(LayoutValues values) {
        this.mTime.setScaleX(values.timeScale);
        this.mTime.setScaleY(values.timeScale);
        this.mClock.setY(values.clockY - this.mClock.getHeight());
        this.mDateGroup.setY(values.dateY);
        this.mAlarmStatus.setY(values.dateY - this.mAlarmStatus.getPaddingTop());
        this.mMultiUserAvatar.setScaleX(values.avatarScale);
        this.mMultiUserAvatar.setScaleY(values.avatarScale);
        this.mMultiUserAvatar.setX(values.avatarX - this.mMultiUserSwitch.getLeft());
        this.mMultiUserAvatar.setY(values.avatarY - this.mMultiUserSwitch.getTop());
        if (getLayoutDirection() == 0) {
            this.mSystemIconsSuperContainer.setX(values.batteryX - this.mSystemIconsContainer.getRight());
        } else {
            this.mSystemIconsSuperContainer.setX(values.batteryX - this.mSystemIconsContainer.getLeft());
        }
        this.mSystemIconsSuperContainer.setY(values.batteryY - this.mSystemIconsContainer.getTop());
        if (this.mSignalCluster != null && this.mExpanded) {
            if (getLayoutDirection() == 0) {
                this.mSignalCluster.setX(this.mSystemIconsSuperContainer.getX() - this.mSignalCluster.getWidth());
            } else {
                this.mSignalCluster.setX(this.mSystemIconsSuperContainer.getX() + this.mSystemIconsSuperContainer.getWidth());
            }
            this.mSignalCluster.setY((this.mSystemIconsSuperContainer.getY() + (this.mSystemIconsSuperContainer.getHeight() / 2)) - (this.mSignalCluster.getHeight() / 2));
        } else if (this.mSignalCluster != null) {
            this.mSignalCluster.setTranslationX(0.0f);
            this.mSignalCluster.setTranslationY(0.0f);
        }
        if (!this.mSettingsButton.isAnimating()) {
            this.mSettingsContainer.setTranslationY(this.mSystemIconsSuperContainer.getTranslationY());
            this.mSettingsContainer.setTranslationX(values.settingsTranslation);
            this.mSettingsButton.setRotation(values.settingsRotation);
        }
        applyAlpha(this.mEmergencyCallsOnly, values.emergencyCallsOnlyAlpha);
        if (!this.mShowingDetail && !this.mDetailTransitioning) {
            applyAlpha(this.mAlarmStatus, values.alarmStatusAlpha);
        }
        applyAlpha(this.mDateCollapsed, values.dateCollapsedAlpha);
        applyAlpha(this.mDateExpanded, values.dateExpandedAlpha);
        applyAlpha(this.mBatteryLevel, values.batteryLevelAlpha);
        applyAlpha(this.mSettingsContainer, values.settingsAlpha);
        applyAlpha(this.mSignalCluster, values.signalClusterAlpha);
        if (!this.mExpanded) {
            this.mTime.setScaleX(1.0f);
            this.mTime.setScaleY(1.0f);
        }
        updateAmPmTranslation();
    }

    private static final class LayoutValues {
        float alarmStatusAlpha;
        float avatarScale;
        float avatarX;
        float avatarY;
        float batteryLevelAlpha;
        float batteryX;
        float batteryY;
        float clockY;
        float dateCollapsedAlpha;
        float dateExpandedAlpha;
        float dateY;
        float emergencyCallsOnlyAlpha;
        float settingsAlpha;
        float settingsRotation;
        float settingsTranslation;
        float signalClusterAlpha;
        float timeScale;

        LayoutValues(LayoutValues layoutValues) {
            this();
        }

        private LayoutValues() {
            this.timeScale = 1.0f;
        }

        public void interpoloate(LayoutValues v1, LayoutValues v2, float t) {
            this.timeScale = (v1.timeScale * (1.0f - t)) + (v2.timeScale * t);
            this.clockY = (v1.clockY * (1.0f - t)) + (v2.clockY * t);
            this.dateY = (v1.dateY * (1.0f - t)) + (v2.dateY * t);
            this.avatarScale = (v1.avatarScale * (1.0f - t)) + (v2.avatarScale * t);
            this.avatarX = (v1.avatarX * (1.0f - t)) + (v2.avatarX * t);
            this.avatarY = (v1.avatarY * (1.0f - t)) + (v2.avatarY * t);
            this.batteryX = (v1.batteryX * (1.0f - t)) + (v2.batteryX * t);
            this.batteryY = (v1.batteryY * (1.0f - t)) + (v2.batteryY * t);
            this.settingsTranslation = (v1.settingsTranslation * (1.0f - t)) + (v2.settingsTranslation * t);
            float t1 = Math.max(0.0f, t - 0.5f) * 2.0f;
            this.settingsRotation = (v1.settingsRotation * (1.0f - t1)) + (v2.settingsRotation * t1);
            this.emergencyCallsOnlyAlpha = (v1.emergencyCallsOnlyAlpha * (1.0f - t1)) + (v2.emergencyCallsOnlyAlpha * t1);
            float t2 = Math.min(1.0f, 2.0f * t);
            this.signalClusterAlpha = (v1.signalClusterAlpha * (1.0f - t2)) + (v2.signalClusterAlpha * t2);
            float t3 = Math.max(0.0f, t - 0.7f) / 0.3f;
            this.batteryLevelAlpha = (v1.batteryLevelAlpha * (1.0f - t3)) + (v2.batteryLevelAlpha * t3);
            this.settingsAlpha = (v1.settingsAlpha * (1.0f - t3)) + (v2.settingsAlpha * t3);
            this.dateExpandedAlpha = (v1.dateExpandedAlpha * (1.0f - t3)) + (v2.dateExpandedAlpha * t3);
            this.dateCollapsedAlpha = (v1.dateCollapsedAlpha * (1.0f - t3)) + (v2.dateCollapsedAlpha * t3);
            this.alarmStatusAlpha = (v1.alarmStatusAlpha * (1.0f - t3)) + (v2.alarmStatusAlpha * t3);
        }
    }
}
