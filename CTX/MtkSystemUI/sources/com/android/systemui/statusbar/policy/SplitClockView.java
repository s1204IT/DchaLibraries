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

    public SplitClockView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        getContext().registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, intentFilter, null, null);
        updatePatterns();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(this.mIntentReceiver);
    }

    public void updatePatterns() {
        String strSubstring;
        String strSubstring2;
        String timeFormatString = DateFormat.getTimeFormatString(getContext(), ActivityManager.getCurrentUser());
        int amPmPartEndIndex = getAmPmPartEndIndex(timeFormatString);
        if (amPmPartEndIndex == -1) {
            strSubstring2 = "";
            strSubstring = timeFormatString;
        } else {
            strSubstring = timeFormatString.substring(0, amPmPartEndIndex);
            strSubstring2 = timeFormatString.substring(amPmPartEndIndex);
        }
        this.mTimeView.setFormat12Hour(strSubstring);
        this.mTimeView.setFormat24Hour(strSubstring);
        this.mTimeView.setContentDescriptionFormat12Hour(timeFormatString);
        this.mTimeView.setContentDescriptionFormat24Hour(timeFormatString);
        this.mAmPmView.setFormat12Hour(strSubstring2);
        this.mAmPmView.setFormat24Hour(strSubstring2);
    }

    private static int getAmPmPartEndIndex(String str) {
        int length = str.length() - 1;
        boolean z = false;
        for (int i = length; i >= 0; i--) {
            char cCharAt = str.charAt(i);
            boolean z2 = cCharAt == 'a';
            boolean zIsWhitespace = Character.isWhitespace(cCharAt);
            if (z2) {
                z = true;
            }
            if (!z2 && !zIsWhitespace) {
                if (i != length && z) {
                    return i + 1;
                }
                return -1;
            }
        }
        return z ? 0 : -1;
    }
}
