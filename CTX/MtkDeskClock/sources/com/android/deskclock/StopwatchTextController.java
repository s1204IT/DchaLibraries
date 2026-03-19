package com.android.deskclock;

import android.widget.TextView;
import com.android.deskclock.uidata.UiDataModel;

public final class StopwatchTextController {
    private final TextView mHundredthsTextView;
    private long mLastTime = Long.MIN_VALUE;
    private final TextView mMainTextView;

    public StopwatchTextController(TextView textView, TextView textView2) {
        this.mMainTextView = textView;
        this.mHundredthsTextView = textView2;
    }

    public void setTimeString(long j) {
        if (this.mLastTime / 10 == j / 10) {
            return;
        }
        int i = (int) (j / 3600000);
        long j2 = (int) (j % 3600000);
        int i2 = (int) (j2 / 60000);
        long j3 = (int) (j2 % 60000);
        int i3 = (int) (j3 / 1000);
        this.mHundredthsTextView.setText(UiDataModel.getUiDataModel().getFormattedNumber(((int) (j3 % 1000)) / 10, 2));
        if (this.mLastTime / 1000 != j / 1000) {
            this.mMainTextView.setText(Utils.getTimeString(this.mMainTextView.getContext(), i, i2, i3));
        }
        this.mLastTime = j;
    }
}
