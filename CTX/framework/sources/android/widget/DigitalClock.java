package android.widget;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import java.util.Calendar;

@Deprecated
public class DigitalClock extends TextView {
    Calendar mCalendar;
    String mFormat;
    private FormatChangeObserver mFormatChangeObserver;
    private Handler mHandler;
    private Runnable mTicker;
    private boolean mTickerStopped;

    public DigitalClock(Context context) {
        super(context);
        this.mTickerStopped = false;
        initClock();
    }

    public DigitalClock(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTickerStopped = false;
        initClock();
    }

    private void initClock() {
        if (this.mCalendar == null) {
            this.mCalendar = Calendar.getInstance();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        this.mTickerStopped = false;
        super.onAttachedToWindow();
        this.mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, this.mFormatChangeObserver);
        setFormat();
        this.mHandler = new Handler();
        this.mTicker = new Runnable() {
            @Override
            public void run() {
                if (DigitalClock.this.mTickerStopped) {
                    return;
                }
                DigitalClock.this.mCalendar.setTimeInMillis(System.currentTimeMillis());
                DigitalClock.this.setText(DateFormat.format(DigitalClock.this.mFormat, DigitalClock.this.mCalendar));
                DigitalClock.this.invalidate();
                long jUptimeMillis = SystemClock.uptimeMillis();
                DigitalClock.this.mHandler.postAtTime(DigitalClock.this.mTicker, jUptimeMillis + (1000 - (jUptimeMillis % 1000)));
            }
        };
        this.mTicker.run();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mTickerStopped = true;
        getContext().getContentResolver().unregisterContentObserver(this.mFormatChangeObserver);
    }

    private void setFormat() {
        this.mFormat = DateFormat.getTimeFormatString(getContext());
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean z) {
            DigitalClock.this.setFormat();
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return DigitalClock.class.getName();
    }
}
