package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.GridLayout;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import java.util.Locale;

public class KeyguardStatusView extends GridLayout {
    private TextView mAlarmStatusView;
    private TextClock mClockView;
    private TextClock mDateView;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    private LockPatternUtils mLockPatternUtils;
    private TextView mOwnerInfo;

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onTimeChanged() {
                KeyguardStatusView.this.refresh();
            }

            @Override
            public void onKeyguardVisibilityChanged(boolean showing) {
                if (showing) {
                    KeyguardStatusView.this.refresh();
                    KeyguardStatusView.this.updateOwnerInfo();
                }
            }

            @Override
            public void onScreenTurnedOn() {
                KeyguardStatusView.this.setEnableMarquee(true);
            }

            @Override
            public void onScreenTurnedOff(int why) {
                KeyguardStatusView.this.setEnableMarquee(false);
            }

            @Override
            public void onUserSwitchComplete(int userId) {
                KeyguardStatusView.this.refresh();
                KeyguardStatusView.this.updateOwnerInfo();
            }
        };
    }

    private void setEnableMarquee(boolean enabled) {
        if (this.mAlarmStatusView != null) {
            this.mAlarmStatusView.setSelected(enabled);
        }
        if (this.mOwnerInfo != null) {
            this.mOwnerInfo.setSelected(enabled);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAlarmStatusView = (TextView) findViewById(R.id.alarm_status);
        this.mDateView = (TextClock) findViewById(R.id.date_view);
        this.mClockView = (TextClock) findViewById(R.id.clock_view);
        this.mDateView.setShowCurrentUserTime(true);
        this.mClockView.setShowCurrentUserTime(true);
        this.mOwnerInfo = (TextView) findViewById(R.id.owner_info);
        this.mLockPatternUtils = new LockPatternUtils(getContext());
        boolean screenOn = KeyguardUpdateMonitor.getInstance(this.mContext).isScreenOn();
        setEnableMarquee(screenOn);
        refresh();
        updateOwnerInfo();
        this.mClockView.setElegantTextHeight(false);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mClockView.setTextSize(0, getResources().getDimensionPixelSize(R.dimen.widget_big_font_size));
        this.mDateView.setTextSize(0, getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        this.mOwnerInfo.setTextSize(0, getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
    }

    public void refreshTime() {
        this.mDateView.setFormat24Hour(Patterns.dateView);
        this.mDateView.setFormat12Hour(Patterns.dateView);
        this.mClockView.setFormat12Hour(Patterns.clockView12);
        this.mClockView.setFormat24Hour(Patterns.clockView24);
    }

    private void refresh() {
        AlarmManager.AlarmClockInfo nextAlarm = this.mLockPatternUtils.getNextAlarm();
        Patterns.update(this.mContext, nextAlarm != null);
        refreshTime();
        refreshAlarmStatus(nextAlarm);
    }

    void refreshAlarmStatus(AlarmManager.AlarmClockInfo nextAlarm) {
        if (nextAlarm != null) {
            String alarm = formatNextAlarm(this.mContext, nextAlarm);
            this.mAlarmStatusView.setText(alarm);
            this.mAlarmStatusView.setContentDescription(getResources().getString(R.string.keyguard_accessibility_next_alarm, alarm));
            this.mAlarmStatusView.setVisibility(0);
            return;
        }
        this.mAlarmStatusView.setVisibility(8);
    }

    public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
        if (info == null) {
            return "";
        }
        String skeleton = DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser()) ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    private void updateOwnerInfo() {
        if (this.mOwnerInfo != null) {
            String ownerInfo = getOwnerInfo();
            if (!TextUtils.isEmpty(ownerInfo)) {
                this.mOwnerInfo.setVisibility(0);
                this.mOwnerInfo.setText(ownerInfo);
            } else {
                this.mOwnerInfo.setVisibility(8);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mInfoCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mInfoCallback);
    }

    public int getAppWidgetId() {
        return -2;
    }

    private String getOwnerInfo() {
        getContext().getContentResolver();
        boolean ownerInfoEnabled = this.mLockPatternUtils.isOwnerInfoEnabled();
        if (!ownerInfoEnabled) {
            return null;
        }
        String info = this.mLockPatternUtils.getOwnerInfo(this.mLockPatternUtils.getCurrentUser());
        return info;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private static final class Patterns {
        static String cacheKey;
        static String clockView12;
        static String clockView24;
        static String dateView;

        static void update(Context context, boolean hasAlarm) {
            Locale locale = Locale.getDefault();
            Resources res = context.getResources();
            String dateViewSkel = res.getString(hasAlarm ? R.string.abbrev_wday_month_day_no_year_alarm : R.string.abbrev_wday_month_day_no_year);
            String clockView12Skel = res.getString(R.string.clock_12hr_format);
            String clockView24Skel = res.getString(R.string.clock_24hr_format);
            String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            if (!key.equals(cacheKey)) {
                dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);
                clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
                if (!clockView12Skel.contains("a")) {
                    clockView12 = clockView12.replaceAll("a", "").trim();
                }
                clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);
                clockView24 = clockView24.replace(':', (char) 60929);
                clockView12 = clockView12.replace(':', (char) 60929);
                cacheKey = key;
            }
        }
    }
}
