package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextClock;
import com.android.systemui.R;

public class SplitClockView extends LinearLayout {
    private TextClock mAmPmView;
    private BroadcastReceiver mIntentReceiver;
    private TextClock mTimeView;

    public SplitClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action) || "android.intent.action.LOCALE_CHANGED".equals(action) || "android.intent.action.CONFIGURATION_CHANGED".equals(action) || "android.intent.action.USER_SWITCHED".equals(action)) {
                    SplitClockView.this.updatePatterns();
                }
            }
        };
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTimeView = (TextClock) findViewById(R.id.time_view);
        this.mAmPmView = (TextClock) findViewById(R.id.am_pm_view);
        this.mTimeView.setShowCurrentUserTime(true);
        this.mAmPmView.setShowCurrentUserTime(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        getContext().registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, filter, null, null);
        updatePatterns();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(this.mIntentReceiver);
    }

    public void updatePatterns() {
        String timeString;
        String amPmString;
        String formatString = DateFormat.getTimeFormatString(getContext(), ActivityManager.getCurrentUser());
        int index = getAmPmPartEndIndex(formatString);
        if (index == -1) {
            timeString = formatString;
            amPmString = "";
        } else {
            timeString = formatString.substring(0, index);
            amPmString = formatString.substring(index);
        }
        this.mTimeView.setFormat12Hour(timeString);
        this.mTimeView.setFormat24Hour(timeString);
        this.mAmPmView.setFormat12Hour(amPmString);
        this.mAmPmView.setFormat24Hour(amPmString);
    }

    private static int getAmPmPartEndIndex(String formatString) {
        boolean hasAmPm = false;
        int length = formatString.length();
        for (int i = length - 1; i >= 0; i--) {
            char c = formatString.charAt(i);
            boolean isAmPm = c == 'a';
            boolean isWhitespace = Character.isWhitespace(c);
            if (isAmPm) {
                hasAmPm = true;
            }
            if (!isAmPm && !isWhitespace) {
                if (i != length - 1 && hasAmPm) {
                    return i + 1;
                }
                return -1;
            }
        }
        return !hasAmPm ? -1 : 0;
    }
}
