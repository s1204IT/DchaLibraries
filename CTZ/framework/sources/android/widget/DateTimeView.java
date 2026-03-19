package android.widget;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.icu.util.Calendar;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews;
import com.android.internal.R;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import libcore.icu.DateUtilsBridge;

@RemoteViews.RemoteView
public class DateTimeView extends TextView {
    private static final int SHOW_MONTH_DAY_YEAR = 1;
    private static final int SHOW_TIME = 0;
    private static final ThreadLocal<ReceiverInfo> sReceiverInfo = new ThreadLocal<>();
    int mLastDisplay;
    DateFormat mLastFormat;
    private String mNowText;
    private boolean mShowRelativeTime;
    Date mTime;
    long mTimeMillis;
    private long mUpdateTimeMillis;

    public DateTimeView(Context context) {
        this(context, null);
    }

    public DateTimeView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLastDisplay = -1;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.DateTimeView, 0, 0);
        int indexCount = typedArrayObtainStyledAttributes.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
            if (typedArrayObtainStyledAttributes.getIndex(i) == 0) {
                setShowRelativeTime(typedArrayObtainStyledAttributes.getBoolean(i, false));
            }
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ReceiverInfo receiverInfo = sReceiverInfo.get();
        if (receiverInfo == null) {
            receiverInfo = new ReceiverInfo();
            sReceiverInfo.set(receiverInfo);
        }
        receiverInfo.addView(this);
        if (this.mShowRelativeTime) {
            update();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ReceiverInfo receiverInfo = sReceiverInfo.get();
        if (receiverInfo != null) {
            receiverInfo.removeView(this);
        }
    }

    @RemotableViewMethod
    public void setTime(long j) {
        Time time = new Time();
        time.set(j);
        this.mTimeMillis = time.toMillis(false);
        this.mTime = new Date(time.year - 1900, time.month, time.monthDay, time.hour, time.minute, 0);
        update();
    }

    @RemotableViewMethod
    public void setShowRelativeTime(boolean z) {
        this.mShowRelativeTime = z;
        updateNowText();
        update();
    }

    @Override
    @RemotableViewMethod
    public void setVisibility(int i) {
        boolean z = i != 8 && getVisibility() == 8;
        super.setVisibility(i);
        if (z) {
            update();
        }
    }

    void update() {
        DateFormat timeFormat;
        if (this.mTime == null || getVisibility() == 8) {
            return;
        }
        if (this.mShowRelativeTime) {
            updateRelativeTime();
            return;
        }
        Date date = this.mTime;
        Time time = new Time();
        time.set(this.mTimeMillis);
        int i = 0;
        time.second = 0;
        time.hour -= 12;
        long millis = time.toMillis(false);
        time.hour += 12;
        long millis2 = time.toMillis(false);
        time.hour = 0;
        time.minute = 0;
        long millis3 = time.toMillis(false);
        time.monthDay++;
        long millis4 = time.toMillis(false);
        time.set(System.currentTimeMillis());
        time.second = 0;
        long jNormalize = time.normalize(false);
        if ((jNormalize < millis3 || jNormalize >= millis4) && (jNormalize < millis || jNormalize >= millis2)) {
            i = 1;
        }
        if (i == this.mLastDisplay && this.mLastFormat != null) {
            timeFormat = this.mLastFormat;
        } else {
            switch (i) {
                case 0:
                    timeFormat = getTimeFormat();
                    break;
                case 1:
                    timeFormat = DateFormat.getDateInstance(3);
                    break;
                default:
                    throw new RuntimeException("unknown display value: " + i);
            }
            this.mLastFormat = timeFormat;
        }
        setText(timeFormat.format(this.mTime));
        if (i == 0) {
            if (millis2 <= millis4) {
                millis2 = millis4;
            }
            this.mUpdateTimeMillis = millis2;
        } else {
            if (this.mTimeMillis < jNormalize) {
                this.mUpdateTimeMillis = 0L;
                return;
            }
            if (millis >= millis3) {
                millis = millis3;
            }
            this.mUpdateTimeMillis = millis;
        }
    }

    private void updateRelativeTime() {
        int iMax;
        int i;
        String str;
        int i2;
        int i3;
        int i4;
        long jCurrentTimeMillis = System.currentTimeMillis();
        long jAbs = Math.abs(jCurrentTimeMillis - this.mTimeMillis);
        boolean z = jCurrentTimeMillis >= this.mTimeMillis;
        long j = DateUtils.MINUTE_IN_MILLIS;
        if (jAbs < DateUtils.MINUTE_IN_MILLIS) {
            setText(this.mNowText);
            this.mUpdateTimeMillis = this.mTimeMillis + DateUtils.MINUTE_IN_MILLIS + 1;
            return;
        }
        if (jAbs < 3600000) {
            iMax = (int) (jAbs / DateUtils.MINUTE_IN_MILLIS);
            Resources resources = getContext().getResources();
            if (z) {
                i4 = R.plurals.duration_minutes_shortest;
            } else {
                i4 = R.plurals.duration_minutes_shortest_future;
            }
            str = String.format(resources.getQuantityString(i4, iMax), Integer.valueOf(iMax));
        } else {
            j = 86400000;
            if (jAbs < 86400000) {
                iMax = (int) (jAbs / 3600000);
                Resources resources2 = getContext().getResources();
                if (z) {
                    i3 = R.plurals.duration_hours_shortest;
                } else {
                    i3 = R.plurals.duration_hours_shortest_future;
                }
                str = String.format(resources2.getQuantityString(i3, iMax), Integer.valueOf(iMax));
                j = 3600000;
            } else if (jAbs < DateUtils.YEAR_IN_MILLIS) {
                TimeZone timeZone = TimeZone.getDefault();
                iMax = Math.max(Math.abs(dayDistance(timeZone, this.mTimeMillis, jCurrentTimeMillis)), 1);
                Resources resources3 = getContext().getResources();
                if (z) {
                    i2 = R.plurals.duration_days_shortest;
                } else {
                    i2 = R.plurals.duration_days_shortest_future;
                }
                str = String.format(resources3.getQuantityString(i2, iMax), Integer.valueOf(iMax));
                if (z || iMax != 1) {
                    this.mUpdateTimeMillis = computeNextMidnight(timeZone);
                    j = -1;
                }
            } else {
                iMax = (int) (jAbs / DateUtils.YEAR_IN_MILLIS);
                Resources resources4 = getContext().getResources();
                if (z) {
                    i = R.plurals.duration_years_shortest;
                } else {
                    i = R.plurals.duration_years_shortest_future;
                }
                str = String.format(resources4.getQuantityString(i, iMax), Integer.valueOf(iMax));
                j = 31449600000L;
            }
        }
        if (j != -1) {
            if (z) {
                this.mUpdateTimeMillis = this.mTimeMillis + (j * ((long) (iMax + 1))) + 1;
            } else {
                this.mUpdateTimeMillis = (this.mTimeMillis - (j * ((long) iMax))) + 1;
            }
        }
        setText(str);
    }

    private long computeNextMidnight(TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(DateUtilsBridge.icuTimeZone(timeZone));
        calendar.add(5, 1);
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateNowText();
        update();
    }

    private void updateNowText() {
        if (!this.mShowRelativeTime) {
            return;
        }
        this.mNowText = getContext().getResources().getString(R.string.now_string_shortest);
    }

    private static int dayDistance(TimeZone timeZone, long j, long j2) {
        return Time.getJulianDay(j2, timeZone.getOffset(j2) / 1000) - Time.getJulianDay(j, timeZone.getOffset(j) / 1000);
    }

    private DateFormat getTimeFormat() {
        return android.text.format.DateFormat.getTimeFormat(getContext());
    }

    void clearFormatAndUpdate() {
        this.mLastFormat = null;
        update();
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        int i;
        String str;
        int i2;
        int i3;
        int i4;
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        if (this.mShowRelativeTime) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            long jAbs = Math.abs(jCurrentTimeMillis - this.mTimeMillis);
            boolean z = jCurrentTimeMillis >= this.mTimeMillis;
            if (jAbs < DateUtils.MINUTE_IN_MILLIS) {
                str = this.mNowText;
            } else if (jAbs < 3600000) {
                int i5 = (int) (jAbs / DateUtils.MINUTE_IN_MILLIS);
                Resources resources = getContext().getResources();
                if (z) {
                    i4 = R.plurals.duration_minutes_relative;
                } else {
                    i4 = R.plurals.duration_minutes_relative_future;
                }
                str = String.format(resources.getQuantityString(i4, i5), Integer.valueOf(i5));
            } else if (jAbs < 86400000) {
                int i6 = (int) (jAbs / 3600000);
                Resources resources2 = getContext().getResources();
                if (z) {
                    i3 = R.plurals.duration_hours_relative;
                } else {
                    i3 = R.plurals.duration_hours_relative_future;
                }
                str = String.format(resources2.getQuantityString(i3, i6), Integer.valueOf(i6));
            } else if (jAbs < DateUtils.YEAR_IN_MILLIS) {
                int iMax = Math.max(Math.abs(dayDistance(TimeZone.getDefault(), this.mTimeMillis, jCurrentTimeMillis)), 1);
                Resources resources3 = getContext().getResources();
                if (z) {
                    i2 = R.plurals.duration_days_relative;
                } else {
                    i2 = R.plurals.duration_days_relative_future;
                }
                str = String.format(resources3.getQuantityString(i2, iMax), Integer.valueOf(iMax));
            } else {
                int i7 = (int) (jAbs / DateUtils.YEAR_IN_MILLIS);
                Resources resources4 = getContext().getResources();
                if (z) {
                    i = R.plurals.duration_years_relative;
                } else {
                    i = R.plurals.duration_years_relative_future;
                }
                str = String.format(resources4.getQuantityString(i, i7), Integer.valueOf(i7));
            }
            accessibilityNodeInfo.setText(str);
        }
    }

    public static void setReceiverHandler(Handler handler) {
        ReceiverInfo receiverInfo = sReceiverInfo.get();
        if (receiverInfo == null) {
            receiverInfo = new ReceiverInfo();
            sReceiverInfo.set(receiverInfo);
        }
        receiverInfo.setHandler(handler);
    }

    private static class ReceiverInfo {
        private final ArrayList<DateTimeView> mAttachedViews;
        private Handler mHandler;
        private final ContentObserver mObserver;
        private final BroadcastReceiver mReceiver;

        private ReceiverInfo() {
            this.mAttachedViews = new ArrayList<>();
            this.mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Intent.ACTION_TIME_TICK.equals(intent.getAction()) && System.currentTimeMillis() < ReceiverInfo.this.getSoonestUpdateTime()) {
                        return;
                    }
                    ReceiverInfo.this.updateAll();
                }
            };
            this.mObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean z) {
                    ReceiverInfo.this.updateAll();
                }
            };
            this.mHandler = new Handler();
        }

        public void addView(DateTimeView dateTimeView) {
            synchronized (this.mAttachedViews) {
                boolean zIsEmpty = this.mAttachedViews.isEmpty();
                this.mAttachedViews.add(dateTimeView);
                if (zIsEmpty) {
                    register(getApplicationContextIfAvailable(dateTimeView.getContext()));
                }
            }
        }

        public void removeView(DateTimeView dateTimeView) {
            synchronized (this.mAttachedViews) {
                if (this.mAttachedViews.remove(dateTimeView) && this.mAttachedViews.isEmpty()) {
                    unregister(getApplicationContextIfAvailable(dateTimeView.getContext()));
                }
            }
        }

        void updateAll() {
            synchronized (this.mAttachedViews) {
                int size = this.mAttachedViews.size();
                for (int i = 0; i < size; i++) {
                    final DateTimeView dateTimeView = this.mAttachedViews.get(i);
                    dateTimeView.post(new Runnable() {
                        @Override
                        public final void run() {
                            dateTimeView.clearFormatAndUpdate();
                        }
                    });
                }
            }
        }

        long getSoonestUpdateTime() {
            long j;
            synchronized (this.mAttachedViews) {
                int size = this.mAttachedViews.size();
                j = Long.MAX_VALUE;
                for (int i = 0; i < size; i++) {
                    long j2 = this.mAttachedViews.get(i).mUpdateTimeMillis;
                    if (j2 < j) {
                        j = j2;
                    }
                }
            }
            return j;
        }

        static final Context getApplicationContextIfAvailable(Context context) {
            Context applicationContext = context.getApplicationContext();
            return applicationContext != null ? applicationContext : ActivityThread.currentApplication().getApplicationContext();
        }

        void register(Context context) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_TIME_TICK);
            intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
            intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            context.registerReceiver(this.mReceiver, intentFilter, null, this.mHandler);
        }

        void unregister(Context context) {
            context.unregisterReceiver(this.mReceiver);
        }

        public void setHandler(Handler handler) {
            this.mHandler = handler;
            synchronized (this.mAttachedViews) {
                if (!this.mAttachedViews.isEmpty()) {
                    unregister(this.mAttachedViews.get(0).getContext());
                    register(this.mAttachedViews.get(0).getContext());
                }
            }
        }
    }
}
