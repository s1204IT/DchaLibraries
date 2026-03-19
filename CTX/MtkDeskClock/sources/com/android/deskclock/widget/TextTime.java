package com.android.deskclock.widget;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;
import java.util.Calendar;
import java.util.TimeZone;

public class TextTime extends TextView {
    private boolean mAttached;
    private CharSequence mFormat;
    private CharSequence mFormat12;
    private CharSequence mFormat24;
    private final ContentObserver mFormatChangeObserver;
    private int mHour;
    private int mMinute;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @VisibleForTesting(otherwise = 2)
    static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";

    @VisibleForTesting(otherwise = 2)
    static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";

    public TextTime(Context context) {
        this(context, null);
    }

    public TextTime(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TextTime(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mFormatChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z) {
                TextTime.this.chooseFormat();
                TextTime.this.updateTime();
            }

            @Override
            public void onChange(boolean z, Uri uri) {
                TextTime.this.chooseFormat();
                TextTime.this.updateTime();
            }
        };
        setFormat12Hour(Utils.get12ModeFormat(0.3f, false));
        setFormat24Hour(Utils.get24ModeFormat(false));
        chooseFormat();
    }

    public CharSequence getFormat12Hour() {
        return this.mFormat12;
    }

    public void setFormat12Hour(CharSequence charSequence) {
        this.mFormat12 = charSequence;
        chooseFormat();
        updateTime();
    }

    public CharSequence getFormat24Hour() {
        return this.mFormat24;
    }

    public void setFormat24Hour(CharSequence charSequence) {
        this.mFormat24 = charSequence;
        chooseFormat();
        updateTime();
    }

    private void chooseFormat() {
        if (DataModel.getDataModel().is24HourFormat()) {
            this.mFormat = this.mFormat24 == null ? DEFAULT_FORMAT_24_HOUR : this.mFormat24;
        } else {
            this.mFormat = this.mFormat12 == null ? DEFAULT_FORMAT_12_HOUR : this.mFormat12;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mAttached) {
            this.mAttached = true;
            registerObserver();
            updateTime();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAttached) {
            unregisterObserver();
            this.mAttached = false;
        }
    }

    private void registerObserver() {
        getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, this.mFormatChangeObserver);
    }

    private void unregisterObserver() {
        getContext().getContentResolver().unregisterContentObserver(this.mFormatChangeObserver);
    }

    public void setTime(int i, int i2) {
        this.mHour = i;
        this.mMinute = i2;
        updateTime();
    }

    private void updateTime() {
        Calendar calendar = DataModel.getDataModel().getCalendar();
        calendar.setTimeZone(UTC);
        calendar.set(11, this.mHour);
        calendar.set(12, this.mMinute);
        CharSequence charSequence = DateFormat.format(this.mFormat, calendar);
        setText(charSequence);
        setContentDescription(charSequence.toString());
    }
}
