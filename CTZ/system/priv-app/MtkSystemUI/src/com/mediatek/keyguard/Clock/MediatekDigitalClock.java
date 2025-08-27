package com.mediatek.keyguard.Clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R;
import com.mediatek.keyguard.PowerOffAlarm.Alarms;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.TimeZone;

/* loaded from: classes.dex */
public class MediatekDigitalClock extends LinearLayout {
    private AmPm mAmPm;
    private boolean mAttached;
    private Calendar mCalendar;
    private ContentObserver mFormatChangeObserver;
    private final Handler mHandler;
    private String mHoursFormat;
    private final BroadcastReceiver mIntentReceiver;
    private boolean mLive;
    private TextView mTimeDisplayHours;
    private TextView mTimeDisplayMinutes;
    private String mTimeZoneId;

    static class AmPm {
        private final TextView mAmPm;
        private final String mAmString;
        private final String mPmString;

        AmPm(View view) {
            this.mAmPm = (TextView) view.findViewById(R.id.am_pm);
            this.mAmPm.setPadding(0, 4, 0, 0);
            String[] amPmStrings = new DateFormatSymbols().getAmPmStrings();
            this.mAmString = amPmStrings[0];
            this.mPmString = amPmStrings[1];
        }

        void setShowAmPm(boolean z) {
            this.mAmPm.setVisibility(z ? 0 : 8);
        }

        void setIsMorning(boolean z) {
            this.mAmPm.setText(z ? this.mAmString : this.mPmString);
        }

        CharSequence getAmPmText() {
            return this.mAmPm.getText();
        }
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z) {
            MediatekDigitalClock.this.setDateFormat();
            MediatekDigitalClock.this.updateTime();
        }
    }

    public MediatekDigitalClock(Context context) {
        this(context, null);
    }

    public MediatekDigitalClock(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLive = true;
        this.mHandler = new Handler();
        this.mIntentReceiver = new BroadcastReceiver() { // from class: com.mediatek.keyguard.Clock.MediatekDigitalClock.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (MediatekDigitalClock.this.mLive && intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED")) {
                    MediatekDigitalClock.this.mCalendar = Calendar.getInstance();
                }
                MediatekDigitalClock.this.mHandler.post(new Runnable() { // from class: com.mediatek.keyguard.Clock.MediatekDigitalClock.1.1
                    @Override // java.lang.Runnable
                    public void run() {
                        MediatekDigitalClock.this.updateTime();
                    }
                });
            }
        };
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTimeDisplayHours = (TextView) findViewById(R.id.timeDisplayHours);
        this.mTimeDisplayMinutes = (TextView) findViewById(R.id.timeDisplayMinutes);
        this.mAmPm = new AmPm(this);
        this.mCalendar = Calendar.getInstance();
        setDateFormat();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.v("PowerOffAlarm", "onAttachedToWindow " + this);
        if (this.mAttached) {
            return;
        }
        this.mAttached = true;
        if (this.mLive) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.TIME_TICK");
            intentFilter.addAction("android.intent.action.TIME_SET");
            intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            getContext().registerReceiver(this.mIntentReceiver, intentFilter);
        }
        this.mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, this.mFormatChangeObserver);
        updateTime();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAttached) {
            this.mAttached = false;
            if (this.mLive) {
                getContext().unregisterReceiver(this.mIntentReceiver);
            }
            getContext().getContentResolver().unregisterContentObserver(this.mFormatChangeObserver);
        }
    }

    private void updateTime() {
        if (this.mLive) {
            this.mCalendar.setTimeInMillis(System.currentTimeMillis());
        }
        if (this.mTimeZoneId != null) {
            this.mCalendar.setTimeZone(TimeZone.getTimeZone(this.mTimeZoneId));
        }
        StringBuilder sb = new StringBuilder();
        CharSequence charSequence = DateFormat.format(this.mHoursFormat, this.mCalendar);
        this.mTimeDisplayHours.setText(charSequence);
        sb.append(charSequence);
        CharSequence charSequence2 = DateFormat.format(":mm", this.mCalendar);
        sb.append(charSequence2);
        this.mTimeDisplayMinutes.setText(charSequence2);
        this.mAmPm.setIsMorning(this.mCalendar.get(9) == 0);
        if (!Alarms.get24HourMode(getContext())) {
            sb.append(this.mAmPm.getAmPmText());
        }
        setContentDescription(sb);
    }

    private void setDateFormat() {
        this.mHoursFormat = Alarms.get24HourMode(getContext()) ? "kk" : "h";
        this.mAmPm.setShowAmPm(!Alarms.get24HourMode(getContext()));
    }
}
