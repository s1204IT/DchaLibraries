package android.widget;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.ViewDebug;
import android.widget.RemoteViews;
import com.android.internal.R;
import java.util.Calendar;
import java.util.TimeZone;
import libcore.icu.LocaleData;

@RemoteViews.RemoteView
public class TextClock extends TextView {
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";
    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";
    private boolean mAttached;

    @ViewDebug.ExportedProperty
    private CharSequence mFormat;
    private CharSequence mFormat12;
    private CharSequence mFormat24;
    private final ContentObserver mFormatChangeObserver;

    @ViewDebug.ExportedProperty
    private boolean mHasSeconds;
    private final BroadcastReceiver mIntentReceiver;
    private boolean mShowCurrentUserTime;
    private final Runnable mTicker;
    private Calendar mTime;
    private String mTimeZone;

    public TextClock(Context context) {
        super(context);
        this.mFormatChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                TextClock.this.chooseFormat();
                TextClock.this.onTimeChanged();
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                TextClock.this.chooseFormat();
                TextClock.this.onTimeChanged();
            }
        };
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (TextClock.this.mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                    String timeZone = intent.getStringExtra("time-zone");
                    TextClock.this.createTime(timeZone);
                }
                TextClock.this.onTimeChanged();
            }
        };
        this.mTicker = new Runnable() {
            @Override
            public void run() {
                TextClock.this.onTimeChanged();
                long now = SystemClock.uptimeMillis();
                long next = now + (1000 - (now % 1000));
                TextClock.this.getHandler().postAtTime(TextClock.this.mTicker, next);
            }
        };
        init();
    }

    public TextClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextClock(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TextClock(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mFormatChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                TextClock.this.chooseFormat();
                TextClock.this.onTimeChanged();
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                TextClock.this.chooseFormat();
                TextClock.this.onTimeChanged();
            }
        };
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (TextClock.this.mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                    String timeZone = intent.getStringExtra("time-zone");
                    TextClock.this.createTime(timeZone);
                }
                TextClock.this.onTimeChanged();
            }
        };
        this.mTicker = new Runnable() {
            @Override
            public void run() {
                TextClock.this.onTimeChanged();
                long now = SystemClock.uptimeMillis();
                long next = now + (1000 - (now % 1000));
                TextClock.this.getHandler().postAtTime(TextClock.this.mTicker, next);
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextClock, defStyleAttr, defStyleRes);
        try {
            this.mFormat12 = a.getText(0);
            this.mFormat24 = a.getText(1);
            this.mTimeZone = a.getString(2);
            a.recycle();
            init();
        } catch (Throwable th) {
            a.recycle();
            throw th;
        }
    }

    private void init() {
        if (this.mFormat12 == null || this.mFormat24 == null) {
            LocaleData ld = LocaleData.get(getContext().getResources().getConfiguration().locale);
            if (this.mFormat12 == null) {
                this.mFormat12 = ld.timeFormat12;
            }
            if (this.mFormat24 == null) {
                this.mFormat24 = ld.timeFormat24;
            }
        }
        createTime(this.mTimeZone);
        chooseFormat(false);
    }

    private void createTime(String timeZone) {
        if (timeZone != null) {
            this.mTime = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        } else {
            this.mTime = Calendar.getInstance();
        }
    }

    @ViewDebug.ExportedProperty
    public CharSequence getFormat12Hour() {
        return this.mFormat12;
    }

    @RemotableViewMethod
    public void setFormat12Hour(CharSequence format) {
        this.mFormat12 = format;
        chooseFormat();
        onTimeChanged();
    }

    @ViewDebug.ExportedProperty
    public CharSequence getFormat24Hour() {
        return this.mFormat24;
    }

    @RemotableViewMethod
    public void setFormat24Hour(CharSequence format) {
        this.mFormat24 = format;
        chooseFormat();
        onTimeChanged();
    }

    public void setShowCurrentUserTime(boolean showCurrentUserTime) {
        this.mShowCurrentUserTime = showCurrentUserTime;
        chooseFormat();
        onTimeChanged();
        unregisterObserver();
        registerObserver();
    }

    public boolean is24HourModeEnabled() {
        return this.mShowCurrentUserTime ? DateFormat.is24HourFormat(getContext(), ActivityManager.getCurrentUser()) : DateFormat.is24HourFormat(getContext());
    }

    public String getTimeZone() {
        return this.mTimeZone;
    }

    @RemotableViewMethod
    public void setTimeZone(String timeZone) {
        this.mTimeZone = timeZone;
        createTime(timeZone);
        onTimeChanged();
    }

    private void chooseFormat() {
        chooseFormat(true);
    }

    public CharSequence getFormat() {
        return this.mFormat;
    }

    private void chooseFormat(boolean handleTicker) {
        boolean format24Requested = is24HourModeEnabled();
        LocaleData ld = LocaleData.get(getContext().getResources().getConfiguration().locale);
        if (format24Requested) {
            this.mFormat = abc(this.mFormat24, this.mFormat12, ld.timeFormat24);
        } else {
            this.mFormat = abc(this.mFormat12, this.mFormat24, ld.timeFormat12);
        }
        boolean hadSeconds = this.mHasSeconds;
        this.mHasSeconds = DateFormat.hasSeconds(this.mFormat);
        if (handleTicker && this.mAttached && hadSeconds != this.mHasSeconds) {
            if (!hadSeconds) {
                this.mTicker.run();
            } else {
                getHandler().removeCallbacks(this.mTicker);
            }
        }
    }

    private static CharSequence abc(CharSequence a, CharSequence b, CharSequence c) {
        return a == null ? b == null ? c : b : a;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mAttached) {
            this.mAttached = true;
            registerReceiver();
            registerObserver();
            createTime(this.mTimeZone);
            if (this.mHasSeconds) {
                this.mTicker.run();
            } else {
                onTimeChanged();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAttached) {
            unregisterReceiver();
            unregisterObserver();
            getHandler().removeCallbacks(this.mTicker);
            this.mAttached = false;
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(this.mIntentReceiver, filter, null, getHandler());
    }

    private void registerObserver() {
        ContentResolver resolver = getContext().getContentResolver();
        if (this.mShowCurrentUserTime) {
            resolver.registerContentObserver(Settings.System.CONTENT_URI, true, this.mFormatChangeObserver, -1);
        } else {
            resolver.registerContentObserver(Settings.System.CONTENT_URI, true, this.mFormatChangeObserver);
        }
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(this.mIntentReceiver);
    }

    private void unregisterObserver() {
        ContentResolver resolver = getContext().getContentResolver();
        resolver.unregisterContentObserver(this.mFormatChangeObserver);
    }

    private void onTimeChanged() {
        this.mTime.setTimeInMillis(System.currentTimeMillis());
        setText(DateFormat.format(this.mFormat, this.mTime));
    }
}
