package com.android.deskclock.widget;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import java.util.Calendar;

public class TextTime extends TextView {
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";
    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";
    private boolean mAttached;
    private String mContentDescriptionFormat;
    private CharSequence mFormat;
    private CharSequence mFormat12;
    private CharSequence mFormat24;
    private final ContentObserver mFormatChangeObserver;
    private int mHour;
    private int mMinute;

    public TextTime(Context context) {
        this(context, null);
    }

    public TextTime(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextTime(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFormatChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                TextTime.this.chooseFormat();
                TextTime.this.updateTime();
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                TextTime.this.chooseFormat();
                TextTime.this.updateTime();
            }
        };
        TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.TextTime, defStyle, 0);
        try {
            this.mFormat12 = styledAttributes.getText(0);
            this.mFormat24 = styledAttributes.getText(1);
            styledAttributes.recycle();
            chooseFormat();
        } catch (Throwable th) {
            styledAttributes.recycle();
            throw th;
        }
    }

    public void setFormat12Hour(CharSequence format) {
        this.mFormat12 = format;
        chooseFormat();
        updateTime();
    }

    public void setFormat24Hour(CharSequence format) {
        this.mFormat24 = format;
        chooseFormat();
        updateTime();
    }

    private void chooseFormat() {
        boolean format24Requested = DateFormat.is24HourFormat(getContext());
        if (format24Requested) {
            this.mFormat = this.mFormat24 == null ? DEFAULT_FORMAT_24_HOUR : this.mFormat24;
        } else {
            this.mFormat = this.mFormat12 == null ? DEFAULT_FORMAT_12_HOUR : this.mFormat12;
        }
        this.mContentDescriptionFormat = this.mFormat.toString();
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
        ContentResolver resolver = getContext().getContentResolver();
        resolver.registerContentObserver(Settings.System.CONTENT_URI, true, this.mFormatChangeObserver);
    }

    private void unregisterObserver() {
        ContentResolver resolver = getContext().getContentResolver();
        resolver.unregisterContentObserver(this.mFormatChangeObserver);
    }

    public void setFormat(int amPmFontSize) {
        setFormat12Hour(Utils.get12ModeFormat(amPmFontSize));
        setFormat24Hour(Utils.get24ModeFormat());
    }

    public void setTime(int hour, int minute) {
        this.mHour = hour;
        this.mMinute = minute;
        updateTime();
    }

    private void updateTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(11, this.mHour);
        calendar.set(12, this.mMinute);
        setText(DateFormat.format(this.mFormat, calendar));
        if (this.mContentDescriptionFormat != null) {
            setContentDescription(DateFormat.format(this.mContentDescriptionFormat, calendar));
        } else {
            setContentDescription(DateFormat.format(this.mFormat, calendar));
        }
    }
}
