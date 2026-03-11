package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.widget.GridLayout;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import java.util.Locale;

public class KeyguardStatusView extends GridLayout {
    private final AlarmManager mAlarmManager;
    TextView mAlarmStatusView;
    TextClock mClockView;
    TextClock mDateView;
    KeyguardUpdateMonitorCallback mInfoCallback;
    private final LockPatternUtils mLockPatternUtils;
    TextView mOwnerInfo;

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
                if (!showing) {
                    return;
                }
                Slog.v("KeyguardStatusView", "refresh statusview showing:" + showing);
                KeyguardStatusView.this.refresh();
                KeyguardStatusView.this.updateOwnerInfo();
            }

            @Override
            public void onStartedWakingUp() {
                KeyguardStatusView.this.setEnableMarquee(true);
            }

            @Override
            public void onFinishedGoingToSleep(int why) {
                KeyguardStatusView.this.setEnableMarquee(false);
            }

            @Override
            public void onUserSwitchComplete(int userId) {
                KeyguardStatusView.this.refresh();
                KeyguardStatusView.this.updateOwnerInfo();
            }
        };
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mLockPatternUtils = new LockPatternUtils(getContext());
    }

    public void setEnableMarquee(boolean enabled) {
        Log.v("KeyguardStatusView", (enabled ? "Enable" : "Disable") + " transport text marquee");
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
        this.mAlarmStatusView = (TextView) findViewById(R$id.alarm_status);
        this.mDateView = (TextClock) findViewById(R$id.date_view);
        this.mClockView = (TextClock) findViewById(R$id.clock_view);
        this.mDateView.setShowCurrentUserTime(true);
        this.mClockView.setShowCurrentUserTime(true);
        this.mOwnerInfo = (TextView) findViewById(R$id.owner_info);
        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive();
        setEnableMarquee(shouldMarquee);
        refresh();
        updateOwnerInfo();
        this.mClockView.setElegantTextHeight(false);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mClockView.setTextSize(0, getResources().getDimensionPixelSize(R$dimen.widget_big_font_size));
        this.mDateView.setTextSize(0, getResources().getDimensionPixelSize(R$dimen.widget_label_font_size));
        if (this.mOwnerInfo == null) {
            return;
        }
        this.mOwnerInfo.setTextSize(0, getResources().getDimensionPixelSize(R$dimen.widget_label_font_size));
    }

    public void refreshTime() {
        this.mDateView.setFormat24Hour(Patterns.dateView);
        this.mDateView.setFormat12Hour(Patterns.dateView);
        this.mClockView.setFormat12Hour(Patterns.clockView12);
        this.mClockView.setFormat24Hour(Patterns.clockView24);
    }

    public void refresh() {
        AlarmManager.AlarmClockInfo nextAlarm = this.mAlarmManager.getNextAlarmClock(-2);
        Patterns.update(this.mContext, nextAlarm != null);
        refreshTime();
        refreshAlarmStatus(nextAlarm);
    }

    void refreshAlarmStatus(AlarmManager.AlarmClockInfo nextAlarm) {
        if (nextAlarm != null) {
            String alarm = formatNextAlarm(this.mContext, nextAlarm);
            this.mAlarmStatusView.setText(alarm);
            this.mAlarmStatusView.setContentDescription(getResources().getString(R$string.keyguard_accessibility_next_alarm, alarm));
            this.mAlarmStatusView.setVisibility(0);
            return;
        }
        this.mAlarmStatusView.setVisibility(8);
    }

    public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
        String skeleton;
        if (info == null) {
            return "";
        }
        if (DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser())) {
            skeleton = "EHm";
        } else {
            skeleton = "Ehma";
        }
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    public void updateOwnerInfo() {
        if (this.mOwnerInfo == null) {
            return;
        }
        String ownerInfo = getOwnerInfo();
        if (!TextUtils.isEmpty(ownerInfo)) {
            this.mOwnerInfo.setVisibility(0);
            this.mOwnerInfo.setText(ownerInfo);
        } else {
            this.mOwnerInfo.setVisibility(8);
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

    private String getOwnerInfo() {
        if (this.mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
            String info = this.mLockPatternUtils.getDeviceOwnerInfo();
            return info;
        }
        boolean ownerInfoEnabled = this.mLockPatternUtils.isOwnerInfoEnabled(KeyguardUpdateMonitor.getCurrentUser());
        if (!ownerInfoEnabled) {
            return null;
        }
        String info2 = this.mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
        return info2;
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

        private Patterns() {
        }

        static void update(Context context, boolean hasAlarm) {
            int i;
            Locale locale = Locale.getDefault();
            Resources res = context.getResources();
            if (hasAlarm) {
                i = R$string.abbrev_wday_month_day_no_year_alarm;
            } else {
                i = R$string.abbrev_wday_month_day_no_year;
            }
            String dateViewSkel = res.getString(i);
            String clockView12Skel = res.getString(R$string.clock_12hr_format);
            String clockView24Skel = res.getString(R$string.clock_24hr_format);
            String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            if (key.equals(cacheKey)) {
                return;
            }
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
