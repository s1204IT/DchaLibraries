package com.android.deskclock;

import android.widget.TextView;

public final class TimerTextController {
    private final TextView mTextView;

    public TimerTextController(TextView textView) {
        this.mTextView = textView;
    }

    public void setTimeString(long j) {
        boolean z;
        int i = 0;
        if (j < 0) {
            j = -j;
            z = true;
        } else {
            z = false;
        }
        int i2 = (int) (j / 3600000);
        long j2 = (int) (j % 3600000);
        int i3 = (int) (j2 / 60000);
        long j3 = (int) (j2 % 60000);
        int i4 = (int) (j3 / 1000);
        int i5 = (int) (j3 % 1000);
        if (z || i5 == 0) {
            i = i4;
        } else {
            int i6 = i4 + 1;
            if (i6 == 60) {
                int i7 = i3 + 1;
                if (i7 == 60) {
                    i2++;
                    i3 = 0;
                } else {
                    i3 = i7;
                }
            } else {
                i = i6;
            }
        }
        String timeString = Utils.getTimeString(this.mTextView.getContext(), i2, i3, i);
        if (z && (i2 != 0 || i3 != 0 || i != 0)) {
            timeString = "−" + timeString;
        }
        this.mTextView.setText(timeString);
    }
}
