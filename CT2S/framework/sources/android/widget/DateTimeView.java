package android.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.widget.RemoteViews;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@RemoteViews.RemoteView
public class DateTimeView extends TextView {
    private static final int SHOW_MONTH_DAY_YEAR = 1;
    private static final int SHOW_TIME = 0;
    private static final String TAG = "DateTimeView";
    private static final long TWELVE_HOURS_IN_MINUTES = 720;
    private static final long TWENTY_FOUR_HOURS_IN_MILLIS = 86400000;
    private static final ThreadLocal<ReceiverInfo> sReceiverInfo = new ThreadLocal<>();
    int mLastDisplay;
    DateFormat mLastFormat;
    Date mTime;
    long mTimeMillis;
    private long mUpdateTimeMillis;

    public DateTimeView(Context context) {
        super(context);
        this.mLastDisplay = -1;
    }

    public DateTimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLastDisplay = -1;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ReceiverInfo ri = sReceiverInfo.get();
        if (ri == null) {
            ri = new ReceiverInfo();
            sReceiverInfo.set(ri);
        }
        ri.addView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ReceiverInfo ri = sReceiverInfo.get();
        if (ri != null) {
            ri.removeView(this);
        }
    }

    @RemotableViewMethod
    public void setTime(long time) {
        Time t = new Time();
        t.set(time);
        t.second = 0;
        this.mTimeMillis = t.toMillis(false);
        this.mTime = new Date(t.year - 1900, t.month, t.monthDay, t.hour, t.minute, 0);
        update();
    }

    void update() {
        int display;
        DateFormat format;
        if (this.mTime != null) {
            System.nanoTime();
            Date date = this.mTime;
            Time t = new Time();
            t.set(this.mTimeMillis);
            t.second = 0;
            t.hour -= 12;
            long twelveHoursBefore = t.toMillis(false);
            t.hour += 12;
            long twelveHoursAfter = t.toMillis(false);
            t.hour = 0;
            t.minute = 0;
            long midnightBefore = t.toMillis(false);
            t.monthDay++;
            long midnightAfter = t.toMillis(false);
            t.set(System.currentTimeMillis());
            t.second = 0;
            long nowMillis = t.normalize(false);
            if ((nowMillis >= midnightBefore && nowMillis < midnightAfter) || (nowMillis >= twelveHoursBefore && nowMillis < twelveHoursAfter)) {
                display = 0;
            } else {
                display = 1;
            }
            if (display == this.mLastDisplay && this.mLastFormat != null) {
                format = this.mLastFormat;
            } else {
                switch (display) {
                    case 0:
                        format = getTimeFormat();
                        break;
                    case 1:
                        format = getDateFormat();
                        break;
                    default:
                        throw new RuntimeException("unknown display value: " + display);
                }
                this.mLastFormat = format;
            }
            String text = format.format(this.mTime);
            setText(text);
            if (display == 0) {
                if (twelveHoursAfter <= midnightAfter) {
                    twelveHoursAfter = midnightAfter;
                }
                this.mUpdateTimeMillis = twelveHoursAfter;
            } else if (this.mTimeMillis < nowMillis) {
                this.mUpdateTimeMillis = 0L;
            } else {
                if (twelveHoursBefore >= midnightBefore) {
                    twelveHoursBefore = midnightBefore;
                }
                this.mUpdateTimeMillis = twelveHoursBefore;
            }
            System.nanoTime();
        }
    }

    private DateFormat getTimeFormat() {
        return android.text.format.DateFormat.getTimeFormat(getContext());
    }

    private DateFormat getDateFormat() {
        String format = Settings.System.getString(getContext().getContentResolver(), Settings.System.DATE_FORMAT);
        if (format == null || ProxyInfo.LOCAL_EXCL_LIST.equals(format)) {
            return DateFormat.getDateInstance(3);
        }
        try {
            return new SimpleDateFormat(format);
        } catch (IllegalArgumentException e) {
            return DateFormat.getDateInstance(3);
        }
    }

    void clearFormatAndUpdate() {
        this.mLastFormat = null;
        update();
    }

    private static class ReceiverInfo {
        private final ArrayList<DateTimeView> mAttachedViews;
        private final ContentObserver mObserver;
        private final BroadcastReceiver mReceiver;

        private ReceiverInfo() {
            this.mAttachedViews = new ArrayList<>();
            this.mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (!Intent.ACTION_TIME_TICK.equals(action) || System.currentTimeMillis() >= ReceiverInfo.this.getSoonestUpdateTime()) {
                        ReceiverInfo.this.updateAll();
                    }
                }
            };
            this.mObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    ReceiverInfo.this.updateAll();
                }
            };
        }

        public void addView(DateTimeView v) {
            boolean register = this.mAttachedViews.isEmpty();
            this.mAttachedViews.add(v);
            if (register) {
                register(v.getContext().getApplicationContext());
            }
        }

        public void removeView(DateTimeView v) {
            this.mAttachedViews.remove(v);
            if (this.mAttachedViews.isEmpty()) {
                unregister(v.getContext().getApplicationContext());
            }
        }

        void updateAll() {
            int count = this.mAttachedViews.size();
            for (int i = 0; i < count; i++) {
                this.mAttachedViews.get(i).clearFormatAndUpdate();
            }
        }

        long getSoonestUpdateTime() {
            long result = Long.MAX_VALUE;
            int count = this.mAttachedViews.size();
            for (int i = 0; i < count; i++) {
                long time = this.mAttachedViews.get(i).mUpdateTimeMillis;
                if (time < result) {
                    result = time;
                }
            }
            return result;
        }

        void register(Context context) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            context.registerReceiver(this.mReceiver, filter);
            Uri uri = Settings.System.getUriFor(Settings.System.DATE_FORMAT);
            context.getContentResolver().registerContentObserver(uri, true, this.mObserver);
        }

        void unregister(Context context) {
            context.unregisterReceiver(this.mReceiver);
            context.getContentResolver().unregisterContentObserver(this.mObserver);
        }
    }
}
