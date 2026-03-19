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
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.ViewDebug;
import android.view.ViewHierarchyEncoder;
import android.widget.RemoteViews;
import com.android.internal.R;
import java.util.Calendar;
import java.util.TimeZone;
import libcore.icu.LocaleData;

@RemoteViews.RemoteView
public class TextClock extends TextView {

    @Deprecated
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";

    @Deprecated
    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";
    private CharSequence mDescFormat;
    private CharSequence mDescFormat12;
    private CharSequence mDescFormat24;

    @ViewDebug.ExportedProperty
    private CharSequence mFormat;
    private CharSequence mFormat12;
    private CharSequence mFormat24;
    private ContentObserver mFormatChangeObserver;

    @ViewDebug.ExportedProperty
    private boolean mHasSeconds;
    private final BroadcastReceiver mIntentReceiver;
    private boolean mRegistered;
    private boolean mShouldRunTicker;
    private boolean mShowCurrentUserTime;
    private boolean mStopTicking;
    private final Runnable mTicker;
    private Calendar mTime;
    private String mTimeZone;

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z) {
            TextClock.this.chooseFormat();
            TextClock.this.onTimeChanged();
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            TextClock.this.chooseFormat();
            TextClock.this.onTimeChanged();
        }
    }

    public TextClock(Context context) {
        super(context);
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!TextClock.this.mStopTicking) {
                    if (TextClock.this.mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                        TextClock.this.createTime(intent.getStringExtra("time-zone"));
                    }
                    TextClock.this.onTimeChanged();
                }
            }
        };
        this.mTicker = new Runnable() {
            @Override
            public void run() {
                if (!TextClock.this.mStopTicking) {
                    TextClock.this.onTimeChanged();
                    long jUptimeMillis = SystemClock.uptimeMillis();
                    TextClock.this.getHandler().postAtTime(TextClock.this.mTicker, jUptimeMillis + (1000 - (jUptimeMillis % 1000)));
                }
            }
        };
        init();
    }

    public TextClock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TextClock(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TextClock(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!TextClock.this.mStopTicking) {
                    if (TextClock.this.mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                        TextClock.this.createTime(intent.getStringExtra("time-zone"));
                    }
                    TextClock.this.onTimeChanged();
                }
            }
        };
        this.mTicker = new Runnable() {
            @Override
            public void run() {
                if (!TextClock.this.mStopTicking) {
                    TextClock.this.onTimeChanged();
                    long jUptimeMillis = SystemClock.uptimeMillis();
                    TextClock.this.getHandler().postAtTime(TextClock.this.mTicker, jUptimeMillis + (1000 - (jUptimeMillis % 1000)));
                }
            }
        };
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TextClock, i, i2);
        try {
            this.mFormat12 = typedArrayObtainStyledAttributes.getText(0);
            this.mFormat24 = typedArrayObtainStyledAttributes.getText(1);
            this.mTimeZone = typedArrayObtainStyledAttributes.getString(2);
            typedArrayObtainStyledAttributes.recycle();
            init();
        } catch (Throwable th) {
            typedArrayObtainStyledAttributes.recycle();
            throw th;
        }
    }

    private void init() {
        if (this.mFormat12 == null || this.mFormat24 == null) {
            LocaleData localeData = LocaleData.get(getContext().getResources().getConfiguration().locale);
            if (this.mFormat12 == null) {
                this.mFormat12 = localeData.timeFormat_hm;
            }
            if (this.mFormat24 == null) {
                this.mFormat24 = localeData.timeFormat_Hm;
            }
        }
        createTime(this.mTimeZone);
        chooseFormat();
    }

    private void createTime(String str) {
        if (str != null) {
            this.mTime = Calendar.getInstance(TimeZone.getTimeZone(str));
        } else {
            this.mTime = Calendar.getInstance();
        }
    }

    @ViewDebug.ExportedProperty
    public CharSequence getFormat12Hour() {
        return this.mFormat12;
    }

    @RemotableViewMethod
    public void setFormat12Hour(CharSequence charSequence) {
        this.mFormat12 = charSequence;
        chooseFormat();
        onTimeChanged();
    }

    public void setContentDescriptionFormat12Hour(CharSequence charSequence) {
        this.mDescFormat12 = charSequence;
        chooseFormat();
        onTimeChanged();
    }

    @ViewDebug.ExportedProperty
    public CharSequence getFormat24Hour() {
        return this.mFormat24;
    }

    @RemotableViewMethod
    public void setFormat24Hour(CharSequence charSequence) {
        this.mFormat24 = charSequence;
        chooseFormat();
        onTimeChanged();
    }

    public void setContentDescriptionFormat24Hour(CharSequence charSequence) {
        this.mDescFormat24 = charSequence;
        chooseFormat();
        onTimeChanged();
    }

    public void setShowCurrentUserTime(boolean z) {
        this.mShowCurrentUserTime = z;
        chooseFormat();
        onTimeChanged();
        unregisterObserver();
        registerObserver();
    }

    public void refresh() {
        onTimeChanged();
        invalidate();
    }

    public boolean is24HourModeEnabled() {
        if (this.mShowCurrentUserTime) {
            return DateFormat.is24HourFormat(getContext(), ActivityManager.getCurrentUser());
        }
        return DateFormat.is24HourFormat(getContext());
    }

    public String getTimeZone() {
        return this.mTimeZone;
    }

    @RemotableViewMethod
    public void setTimeZone(String str) {
        this.mTimeZone = str;
        createTime(str);
        onTimeChanged();
    }

    public CharSequence getFormat() {
        return this.mFormat;
    }

    private void chooseFormat() {
        boolean zIs24HourModeEnabled = is24HourModeEnabled();
        LocaleData localeData = LocaleData.get(getContext().getResources().getConfiguration().locale);
        if (zIs24HourModeEnabled) {
            this.mFormat = abc(this.mFormat24, this.mFormat12, localeData.timeFormat_Hm);
            this.mDescFormat = abc(this.mDescFormat24, this.mDescFormat12, this.mFormat);
        } else {
            this.mFormat = abc(this.mFormat12, this.mFormat24, localeData.timeFormat_hm);
            this.mDescFormat = abc(this.mDescFormat12, this.mDescFormat24, this.mFormat);
        }
        boolean z = this.mHasSeconds;
        this.mHasSeconds = DateFormat.hasSeconds(this.mFormat);
        if (this.mShouldRunTicker && z != this.mHasSeconds) {
            if (!z) {
                this.mTicker.run();
            } else {
                getHandler().removeCallbacks(this.mTicker);
            }
        }
    }

    private static CharSequence abc(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        return charSequence == null ? charSequence2 == null ? charSequence3 : charSequence2 : charSequence;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mRegistered) {
            this.mRegistered = true;
            registerReceiver();
            registerObserver();
            createTime(this.mTimeZone);
        }
    }

    @Override
    public void onVisibilityAggregated(boolean z) {
        super.onVisibilityAggregated(z);
        if (!this.mShouldRunTicker && z) {
            this.mShouldRunTicker = true;
            if (this.mHasSeconds) {
                this.mTicker.run();
                return;
            } else {
                onTimeChanged();
                return;
            }
        }
        if (this.mShouldRunTicker && !z) {
            this.mShouldRunTicker = false;
            getHandler().removeCallbacks(this.mTicker);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mRegistered) {
            unregisterReceiver();
            unregisterObserver();
            this.mRegistered = false;
        }
    }

    public void disableClockTick() {
        this.mStopTicking = true;
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiverAsUser(this.mIntentReceiver, Process.myUserHandle(), intentFilter, null, getHandler());
    }

    private void registerObserver() {
        if (this.mRegistered) {
            if (this.mFormatChangeObserver == null) {
                this.mFormatChangeObserver = new FormatChangeObserver(getHandler());
            }
            ContentResolver contentResolver = getContext().getContentResolver();
            Uri uriFor = Settings.System.getUriFor(Settings.System.TIME_12_24);
            if (this.mShowCurrentUserTime) {
                contentResolver.registerContentObserver(uriFor, true, this.mFormatChangeObserver, -1);
            } else {
                contentResolver.registerContentObserver(uriFor, true, this.mFormatChangeObserver);
            }
        }
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(this.mIntentReceiver);
    }

    private void unregisterObserver() {
        if (this.mFormatChangeObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(this.mFormatChangeObserver);
        }
    }

    private void onTimeChanged() {
        if (this.mShouldRunTicker) {
            this.mTime.setTimeInMillis(System.currentTimeMillis());
            setText(DateFormat.format(this.mFormat, this.mTime));
            setContentDescription(DateFormat.format(this.mDescFormat, this.mTime));
        }
    }

    @Override
    protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
        super.encodeProperties(viewHierarchyEncoder);
        CharSequence format12Hour = getFormat12Hour();
        viewHierarchyEncoder.addProperty("format12Hour", format12Hour == null ? null : format12Hour.toString());
        CharSequence format24Hour = getFormat24Hour();
        viewHierarchyEncoder.addProperty("format24Hour", format24Hour == null ? null : format24Hour.toString());
        viewHierarchyEncoder.addProperty("format", this.mFormat != null ? this.mFormat.toString() : null);
        viewHierarchyEncoder.addProperty("hasSeconds", this.mHasSeconds);
    }
}
