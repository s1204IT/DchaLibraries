package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.systemui.DemoMode;
import com.android.systemui.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import libcore.icu.LocaleData;

public class Clock extends TextView implements DemoMode {
    private final int mAmPmStyle;
    private boolean mAttached;
    private Calendar mCalendar;
    private SimpleDateFormat mClockFormat;
    private String mClockFormatString;
    private boolean mDemoMode;
    private final BroadcastReceiver mIntentReceiver;
    private Locale mLocale;
    private ContentObserver mObs;

    public Clock(Context context) {
        this(context, null);
    }

    public Clock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Clock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.TIMEZONE_CHANGED")) {
                    String tz = intent.getStringExtra("time-zone");
                    Clock.this.mCalendar = Calendar.getInstance(TimeZone.getTimeZone(tz));
                    if (Clock.this.mClockFormat != null) {
                        Clock.this.mClockFormat.setTimeZone(Clock.this.mCalendar.getTimeZone());
                    }
                } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                    Locale newLocale = Clock.this.getResources().getConfiguration().locale;
                    if (!newLocale.equals(Clock.this.mLocale)) {
                        Clock.this.mLocale = newLocale;
                        Clock.this.mClockFormatString = "";
                    }
                }
                Clock.this.updateClock();
            }
        };
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Clock, 0, 0);
        try {
            this.mAmPmStyle = a.getInt(0, 2);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mAttached) {
            this.mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.TIME_TICK");
            filter.addAction("android.intent.action.TIME_SET");
            filter.addAction("android.intent.action.TIMEZONE_CHANGED");
            filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            filter.addAction("android.intent.action.USER_SWITCHED");
            getContext().registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, filter, null, getHandler());
            this.mObs = new ContentObserver(getHandler()) {
                @Override
                public void onChange(boolean selfChange) {
                    Clock.this.updateClock();
                }
            };
            getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor("dcha_state"), false, this.mObs, -1);
        }
        this.mCalendar = Calendar.getInstance(TimeZone.getDefault());
        updateClock();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAttached) {
            getContext().unregisterReceiver(this.mIntentReceiver);
            getContext().getContentResolver().unregisterContentObserver(this.mObs);
            this.mObs = null;
            this.mAttached = false;
        }
    }

    final void updateClock() {
        if (!this.mDemoMode) {
            this.mCalendar.setTimeInMillis(System.currentTimeMillis());
            setText(getSmallTime());
        }
    }

    private final CharSequence getSmallTime() {
        SimpleDateFormat sdf;
        Context context = getContext();
        boolean is24 = DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser());
        LocaleData d = LocaleData.get(context.getResources().getConfiguration().locale);
        String format = is24 ? d.timeFormat24 : d.timeFormat12;
        if (BenesseExtension.getDchaState() != 0) {
            format = "M月d日aaKK:mm";
        }
        if (!format.equals(this.mClockFormatString)) {
            if (this.mAmPmStyle != 0) {
                int a = -1;
                boolean quoted = false;
                int i = 0;
                while (true) {
                    if (i < format.length()) {
                        char c = format.charAt(i);
                        if (c == '\'') {
                            quoted = !quoted;
                        }
                        if (quoted || c != 'a') {
                            i++;
                        } else {
                            a = i;
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (a >= 0) {
                    int b = a;
                    while (a > 0 && Character.isWhitespace(format.charAt(a - 1))) {
                        a--;
                    }
                    format = format.substring(0, a) + (char) 61184 + format.substring(a, b) + "a\uef01" + format.substring(b + 1);
                }
            }
            sdf = new SimpleDateFormat(format);
            this.mClockFormat = sdf;
            this.mClockFormatString = format;
        } else {
            sdf = this.mClockFormat;
        }
        String result = sdf.format(this.mCalendar.getTime());
        if (this.mAmPmStyle != 0) {
            int magic1 = result.indexOf(61184);
            int magic2 = result.indexOf(61185);
            if (magic1 >= 0 && magic2 > magic1) {
                SpannableStringBuilder formatted = new SpannableStringBuilder(result);
                if (this.mAmPmStyle == 2) {
                    formatted.delete(magic1, magic2 + 1);
                    return formatted;
                }
                if (this.mAmPmStyle == 1) {
                    CharacterStyle style = new RelativeSizeSpan(0.7f);
                    formatted.setSpan(style, magic1, magic2, 34);
                }
                formatted.delete(magic2, magic2 + 1);
                formatted.delete(magic1, magic1 + 1);
                return formatted;
            }
        }
        return result;
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!this.mDemoMode && command.equals("enter")) {
            this.mDemoMode = true;
            return;
        }
        if (this.mDemoMode && command.equals("exit")) {
            this.mDemoMode = false;
            updateClock();
            return;
        }
        if (this.mDemoMode && command.equals("clock")) {
            String millis = args.getString("millis");
            String hhmm = args.getString("hhmm");
            if (millis != null) {
                this.mCalendar.setTimeInMillis(Long.parseLong(millis));
            } else if (hhmm != null && hhmm.length() == 4) {
                int hh = Integer.parseInt(hhmm.substring(0, 2));
                int mm = Integer.parseInt(hhmm.substring(2));
                this.mCalendar.set(10, hh);
                this.mCalendar.set(12, mm);
            }
            setText(getSmallTime());
        }
    }
}
