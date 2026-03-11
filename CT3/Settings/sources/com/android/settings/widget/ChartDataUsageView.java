package com.android.settings.widget;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.view.MotionEvent;
import com.android.settings.R;
import com.android.settings.widget.ChartSweepView;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;

public class ChartDataUsageView extends ChartView {
    private ChartNetworkSeriesView mDetailSeries;
    private ChartGridView mGrid;
    private Handler mHandler;
    private DataUsageChartListener mListener;
    private ChartNetworkSeriesView mSeries;
    private ChartSweepView mSweepLimit;
    private ChartSweepView mSweepWarning;
    private ChartSweepView.OnSweepListener mVertListener;
    private long mVertMax;

    public interface DataUsageChartListener {
        void onLimitChanged();

        void onWarningChanged();

        void requestLimitEdit();

        void requestWarningEdit();
    }

    public ChartDataUsageView(Context context) {
        this(context, null, 0);
    }

    public ChartDataUsageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartDataUsageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mVertListener = new ChartSweepView.OnSweepListener() {
            @Override
            public void onSweep(ChartSweepView sweep, boolean sweepDone) {
                if (sweepDone) {
                    ChartDataUsageView.this.clearUpdateAxisDelayed(sweep);
                    ChartDataUsageView.this.updateEstimateVisible();
                    if (sweep == ChartDataUsageView.this.mSweepWarning && ChartDataUsageView.this.mListener != null) {
                        ChartDataUsageView.this.mListener.onWarningChanged();
                        return;
                    } else {
                        if (sweep != ChartDataUsageView.this.mSweepLimit || ChartDataUsageView.this.mListener == null) {
                            return;
                        }
                        ChartDataUsageView.this.mListener.onLimitChanged();
                        return;
                    }
                }
                ChartDataUsageView.this.sendUpdateAxisDelayed(sweep, false);
            }

            @Override
            public void requestEdit(ChartSweepView sweep) {
                if (sweep == ChartDataUsageView.this.mSweepWarning && ChartDataUsageView.this.mListener != null) {
                    ChartDataUsageView.this.mListener.requestWarningEdit();
                } else {
                    if (sweep != ChartDataUsageView.this.mSweepLimit || ChartDataUsageView.this.mListener == null) {
                        return;
                    }
                    ChartDataUsageView.this.mListener.requestLimitEdit();
                }
            }
        };
        init(new TimeAxis(), new InvertedChartAxis(new DataAxis()));
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                ChartSweepView sweep = (ChartSweepView) msg.obj;
                ChartDataUsageView.this.updateVertAxisBounds(sweep);
                ChartDataUsageView.this.updateEstimateVisible();
                ChartDataUsageView.this.sendUpdateAxisDelayed(sweep, true);
            }
        };
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mGrid = (ChartGridView) findViewById(R.id.grid);
        this.mSeries = (ChartNetworkSeriesView) findViewById(R.id.series);
        this.mDetailSeries = (ChartNetworkSeriesView) findViewById(R.id.detail_series);
        this.mDetailSeries.setVisibility(8);
        this.mSweepLimit = (ChartSweepView) findViewById(R.id.sweep_limit);
        this.mSweepWarning = (ChartSweepView) findViewById(R.id.sweep_warning);
        this.mSweepWarning.setValidRangeDynamic(null, this.mSweepLimit);
        this.mSweepLimit.setValidRangeDynamic(this.mSweepWarning, null);
        this.mSweepLimit.setNeighbors(this.mSweepWarning);
        this.mSweepWarning.setNeighbors(this.mSweepLimit);
        this.mSweepWarning.addOnSweepListener(this.mVertListener);
        this.mSweepLimit.addOnSweepListener(this.mVertListener);
        this.mSweepWarning.setDragInterval(5242880L);
        this.mSweepLimit.setDragInterval(5242880L);
        this.mGrid.init(this.mHoriz, this.mVert);
        this.mSeries.init(this.mHoriz, this.mVert);
        this.mDetailSeries.init(this.mHoriz, this.mVert);
        this.mSweepWarning.init(this.mVert);
        this.mSweepLimit.init(this.mVert);
        setActivated(false);
    }

    public void updateVertAxisBounds(ChartSweepView activeSweep) {
        long max = this.mVertMax;
        long newMax = 0;
        if (activeSweep != null) {
            int adjustAxis = activeSweep.shouldAdjustAxis();
            if (adjustAxis > 0) {
                newMax = (11 * max) / 10;
            } else if (adjustAxis < 0) {
                newMax = (9 * max) / 10;
            } else {
                newMax = max;
            }
        }
        long maxSweep = Math.max(this.mSweepWarning.getValue(), this.mSweepLimit.getValue());
        long maxSeries = Math.max(this.mSeries.getMaxVisible(), this.mDetailSeries.getMaxVisible());
        long maxVisible = (Math.max(maxSeries, maxSweep) * 12) / 10;
        long maxDefault = Math.max(maxVisible, 52428800L);
        long newMax2 = Math.max(maxDefault, newMax);
        if (newMax2 == this.mVertMax) {
            return;
        }
        this.mVertMax = newMax2;
        boolean changed = this.mVert.setBounds(0L, newMax2);
        this.mSweepWarning.setValidRange(0L, newMax2);
        this.mSweepLimit.setValidRange(0L, newMax2);
        if (changed) {
            this.mSeries.invalidatePath();
            this.mDetailSeries.invalidatePath();
        }
        this.mGrid.invalidate();
        if (activeSweep != null) {
            activeSweep.updateValueFromPosition();
        }
        if (this.mSweepLimit != activeSweep) {
            layoutSweep(this.mSweepLimit);
        }
        if (this.mSweepWarning == activeSweep) {
            return;
        }
        layoutSweep(this.mSweepWarning);
    }

    public void updateEstimateVisible() {
        long maxEstimate = this.mSeries.getMaxEstimate();
        long interestLine = Long.MAX_VALUE;
        if (this.mSweepWarning.isEnabled()) {
            interestLine = this.mSweepWarning.getValue();
        } else if (this.mSweepLimit.isEnabled()) {
            interestLine = this.mSweepLimit.getValue();
        }
        if (interestLine < 0) {
            interestLine = Long.MAX_VALUE;
        }
        boolean estimateVisible = maxEstimate >= (7 * interestLine) / 10;
        this.mSeries.setEstimateVisible(estimateVisible);
    }

    public void sendUpdateAxisDelayed(ChartSweepView sweep, boolean force) {
        if (!force && this.mHandler.hasMessages(100, sweep)) {
            return;
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(100, sweep), 250L);
    }

    public void clearUpdateAxisDelayed(ChartSweepView sweep) {
        this.mHandler.removeMessages(100, sweep);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isActivated()) {
            return false;
        }
        switch (event.getAction()) {
            case DefaultWfcSettingsExt.PAUSE:
                setActivated(true);
                break;
        }
        return false;
    }

    public static class TimeAxis implements ChartAxis {
        private static final int FIRST_DAY_OF_WEEK = Calendar.getInstance().getFirstDayOfWeek() - 1;
        private long mMax;
        private long mMin;
        private float mSize;

        public TimeAxis() {
            long currentTime = System.currentTimeMillis();
            setBounds(currentTime - 2592000000L, currentTime);
        }

        public int hashCode() {
            return Objects.hash(Long.valueOf(this.mMin), Long.valueOf(this.mMax), Float.valueOf(this.mSize));
        }

        @Override
        public boolean setBounds(long min, long max) {
            if (this.mMin != min || this.mMax != max) {
                this.mMin = min;
                this.mMax = max;
                return true;
            }
            return false;
        }

        @Override
        public boolean setSize(float size) {
            if (this.mSize != size) {
                this.mSize = size;
                return true;
            }
            return false;
        }

        @Override
        public float convertToPoint(long value) {
            return (this.mSize * (value - this.mMin)) / (this.mMax - this.mMin);
        }

        @Override
        public long convertToValue(float point) {
            return (long) (this.mMin + (((this.mMax - this.mMin) * point) / this.mSize));
        }

        @Override
        public long buildLabel(Resources res, SpannableStringBuilder builder, long value) {
            builder.replace(0, builder.length(), (CharSequence) Long.toString(value));
            return value;
        }

        @Override
        public float[] getTickPoints() {
            float[] ticks = new float[32];
            int i = 0;
            Time time = new Time();
            time.set(this.mMax);
            time.monthDay -= time.weekDay - FIRST_DAY_OF_WEEK;
            time.second = 0;
            time.minute = 0;
            time.hour = 0;
            time.normalize(true);
            for (long timeMillis = time.toMillis(true); timeMillis > this.mMin; timeMillis = time.toMillis(true)) {
                if (timeMillis <= this.mMax) {
                    ticks[i] = convertToPoint(timeMillis);
                    i++;
                }
                time.monthDay -= 7;
                time.normalize(true);
            }
            return Arrays.copyOf(ticks, i);
        }

        @Override
        public int shouldAdjustAxis(long value) {
            return 0;
        }
    }

    public static class DataAxis implements ChartAxis {
        private static final Object sSpanSize = new Object();
        private static final Object sSpanUnit = new Object();
        private long mMax;
        private long mMin;
        private float mSize;

        public int hashCode() {
            return Objects.hash(Long.valueOf(this.mMin), Long.valueOf(this.mMax), Float.valueOf(this.mSize));
        }

        @Override
        public boolean setBounds(long min, long max) {
            if (this.mMin != min || this.mMax != max) {
                this.mMin = min;
                this.mMax = max;
                return true;
            }
            return false;
        }

        @Override
        public boolean setSize(float size) {
            if (this.mSize != size) {
                this.mSize = size;
                return true;
            }
            return false;
        }

        @Override
        public float convertToPoint(long value) {
            return (this.mSize * (value - this.mMin)) / (this.mMax - this.mMin);
        }

        @Override
        public long convertToValue(float point) {
            return (long) (this.mMin + (((this.mMax - this.mMin) * point) / this.mSize));
        }

        @Override
        public long buildLabel(Resources res, SpannableStringBuilder builder, long value) {
            Formatter.BytesResult result = Formatter.formatBytes(res, MathUtils.constrain(value, 0L, 1099511627776L), 3);
            ChartDataUsageView.setText(builder, sSpanSize, result.value, "^1");
            ChartDataUsageView.setText(builder, sSpanUnit, result.units, "^2");
            return result.roundedBytes;
        }

        @Override
        public float[] getTickPoints() {
            long range = this.mMax - this.mMin;
            long tickJump = ChartDataUsageView.roundUpToPowerOfTwo(range / 16);
            int tickCount = (int) (range / tickJump);
            float[] tickPoints = new float[tickCount];
            long value = this.mMin;
            for (int i = 0; i < tickPoints.length; i++) {
                tickPoints[i] = convertToPoint(value);
                value += tickJump;
            }
            return tickPoints;
        }

        @Override
        public int shouldAdjustAxis(long value) {
            float point = convertToPoint(value);
            if (point < ((double) this.mSize) * 0.1d) {
                return -1;
            }
            if (point > ((double) this.mSize) * 0.85d) {
                return 1;
            }
            return 0;
        }
    }

    public static void setText(SpannableStringBuilder builder, Object key, CharSequence text, String bootstrap) {
        int start = builder.getSpanStart(key);
        int end = builder.getSpanEnd(key);
        if (start == -1) {
            start = TextUtils.indexOf(builder, bootstrap);
            end = start + bootstrap.length();
            builder.setSpan(key, start, end, 18);
        }
        builder.replace(start, end, text);
    }

    public static long roundUpToPowerOfTwo(long i) {
        long i2 = i - 1;
        long i3 = i2 | (i2 >>> 1);
        long i4 = i3 | (i3 >>> 2);
        long i5 = i4 | (i4 >>> 4);
        long i6 = i5 | (i5 >>> 8);
        long i7 = i6 | (i6 >>> 16);
        long i8 = (i7 | (i7 >>> 32)) + 1;
        if (i8 > 0) {
            return i8;
        }
        return Long.MAX_VALUE;
    }
}
