package android.support.v17.leanback.widget;

import android.os.Bundle;

public class GuidedDatePickerAction extends GuidedAction {
    long mDate;
    String mDatePickerFormat;
    long mMinDate = Long.MIN_VALUE;
    long mMaxDate = Long.MAX_VALUE;

    public String getDatePickerFormat() {
        return this.mDatePickerFormat;
    }

    public long getDate() {
        return this.mDate;
    }

    public void setDate(long date) {
        this.mDate = date;
    }

    public long getMinDate() {
        return this.mMinDate;
    }

    public long getMaxDate() {
        return this.mMaxDate;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle, String key) {
        bundle.putLong(key, getDate());
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle, String key) {
        setDate(bundle.getLong(key, getDate()));
    }
}
