package android.widget;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
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

    public DigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTickerStopped = false;
        initClock();
    }

    private void initClock() {
        if (this.mCalendar == null) {
            this.mCalendar = Calendar.getInstance();
        }
        this.mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, this.mFormatChangeObserver);
        setFormat();
    }

    @Override
    protected void onAttachedToWindow() {
        this.mTickerStopped = false;
        super.onAttachedToWindow();
        this.mHandler = new Handler();
        this.mTicker = new Runnable() {
            @Override
            public void run() {
                if (!DigitalClock.this.mTickerStopped) {
                    DigitalClock.this.mCalendar.setTimeInMillis(System.currentTimeMillis());
                    DigitalClock.this.setText(DateFormat.format(DigitalClock.this.mFormat, DigitalClock.this.mCalendar));
                    DigitalClock.this.invalidate();
                    long now = SystemClock.uptimeMillis();
                    long next = now + (1000 - (now % 1000));
                    DigitalClock.this.mHandler.postAtTime(DigitalClock.this.mTicker, next);
                }
            }
        };
        this.mTicker.run();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mTickerStopped = true;
    }

    private void setFormat() {
        this.mFormat = DateFormat.getTimeFormatString(getContext());
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            DigitalClock.this.setFormat();
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(DigitalClock.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(DigitalClock.class.getName());
    }
}
