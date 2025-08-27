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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;

/* loaded from: classes.dex */
public class ChartDataUsageView extends ChartView {
    private ChartNetworkSeriesView mDetailSeries;
    private ChartGridView mGrid;
    private Handler mHandler;
    private long mInspectEnd;
    private long mInspectStart;
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

    public ChartDataUsageView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ChartDataUsageView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mVertListener = new ChartSweepView.OnSweepListener() { // from class: com.android.settings.widget.ChartDataUsageView.2
            @Override // com.android.settings.widget.ChartSweepView.OnSweepListener
            public void onSweep(ChartSweepView chartSweepView, boolean z) {
                if (z) {
                    ChartDataUsageView.this.clearUpdateAxisDelayed(chartSweepView);
                    ChartDataUsageView.this.updateEstimateVisible();
                    if (chartSweepView != ChartDataUsageView.this.mSweepWarning || ChartDataUsageView.this.mListener == null) {
                        if (chartSweepView == ChartDataUsageView.this.mSweepLimit && ChartDataUsageView.this.mListener != null) {
                            ChartDataUsageView.this.mListener.onLimitChanged();
                            return;
                        }
                        return;
                    }
                    ChartDataUsageView.this.mListener.onWarningChanged();
                    return;
                }
                ChartDataUsageView.this.sendUpdateAxisDelayed(chartSweepView, false);
            }

            @Override // com.android.settings.widget.ChartSweepView.OnSweepListener
            public void requestEdit(ChartSweepView chartSweepView) {
                if (chartSweepView != ChartDataUsageView.this.mSweepWarning || ChartDataUsageView.this.mListener == null) {
                    if (chartSweepView == ChartDataUsageView.this.mSweepLimit && ChartDataUsageView.this.mListener != null) {
                        ChartDataUsageView.this.mListener.requestLimitEdit();
                        return;
                    }
                    return;
                }
                ChartDataUsageView.this.mListener.requestWarningEdit();
            }
        };
        init(new TimeAxis(), new InvertedChartAxis(new DataAxis()));
        this.mHandler = new Handler() { // from class: com.android.settings.widget.ChartDataUsageView.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                ChartSweepView chartSweepView = (ChartSweepView) message.obj;
                ChartDataUsageView.this.updateVertAxisBounds(chartSweepView);
                ChartDataUsageView.this.updateEstimateVisible();
                ChartDataUsageView.this.sendUpdateAxisDelayed(chartSweepView, true);
            }
        };
    }

    @Override // android.view.View
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

    public void setListener(DataUsageChartListener dataUsageChartListener) {
        this.mListener = dataUsageChartListener;
    }

    private void updateVertAxisBounds(ChartSweepView chartSweepView) {
        long j = this.mVertMax;
        if (chartSweepView != null) {
            int iShouldAdjustAxis = chartSweepView.shouldAdjustAxis();
            if (iShouldAdjustAxis > 0) {
                j = (j * 11) / 10;
            } else if (iShouldAdjustAxis < 0) {
                j = (j * 9) / 10;
            }
        } else {
            j = 0;
        }
        long jMax = Math.max(Math.max((Math.max(Math.max(this.mSeries.getMaxVisible(), this.mDetailSeries.getMaxVisible()), Math.max(this.mSweepWarning.getValue(), this.mSweepLimit.getValue())) * 12) / 10, 52428800L), j);
        if (jMax != this.mVertMax) {
            this.mVertMax = jMax;
            boolean bounds = this.mVert.setBounds(0L, jMax);
            this.mSweepWarning.setValidRange(0L, jMax);
            this.mSweepLimit.setValidRange(0L, jMax);
            if (bounds) {
                this.mSeries.invalidatePath();
                this.mDetailSeries.invalidatePath();
            }
            this.mGrid.invalidate();
            if (chartSweepView != null) {
                chartSweepView.updateValueFromPosition();
            }
            if (this.mSweepLimit != chartSweepView) {
                layoutSweep(this.mSweepLimit);
            }
            if (this.mSweepWarning != chartSweepView) {
                layoutSweep(this.mSweepWarning);
            }
        }
    }

    private void updateEstimateVisible() {
        long value;
        long maxEstimate = this.mSeries.getMaxEstimate();
        if (this.mSweepWarning.isEnabled()) {
            value = this.mSweepWarning.getValue();
        } else if (this.mSweepLimit.isEnabled()) {
            value = this.mSweepLimit.getValue();
        } else {
            value = Long.MAX_VALUE;
        }
        this.mSeries.setEstimateVisible(maxEstimate >= ((value >= 0 ? value : Long.MAX_VALUE) * 7) / 10);
    }

    private void sendUpdateAxisDelayed(ChartSweepView chartSweepView, boolean z) {
        if (z || !this.mHandler.hasMessages(100, chartSweepView)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(100, chartSweepView), 250L);
        }
    }

    private void clearUpdateAxisDelayed(ChartSweepView chartSweepView) {
        this.mHandler.removeMessages(100, chartSweepView);
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (isActivated()) {
            return false;
        }
        switch (motionEvent.getAction()) {
            case 1:
                setActivated(true);
                break;
        }
        return false;
    }

    public long getInspectStart() {
        return this.mInspectStart;
    }

    public long getInspectEnd() {
        return this.mInspectEnd;
    }

    public long getWarningBytes() {
        return this.mSweepWarning.getLabelValue();
    }

    public long getLimitBytes() {
        return this.mSweepLimit.getLabelValue();
    }

    public static class TimeAxis implements ChartAxis {
        private static final int FIRST_DAY_OF_WEEK = Calendar.getInstance().getFirstDayOfWeek() - 1;
        private long mMax;
        private long mMin;
        private float mSize;

        public TimeAxis() {
            long jCurrentTimeMillis = System.currentTimeMillis();
            setBounds(jCurrentTimeMillis - 2592000000L, jCurrentTimeMillis);
        }

        public int hashCode() {
            return Objects.hash(Long.valueOf(this.mMin), Long.valueOf(this.mMax), Float.valueOf(this.mSize));
        }

        @Override // com.android.settings.widget.ChartAxis
        public boolean setBounds(long j, long j2) {
            if (this.mMin != j || this.mMax != j2) {
                this.mMin = j;
                this.mMax = j2;
                return true;
            }
            return false;
        }

        @Override // com.android.settings.widget.ChartAxis
        public boolean setSize(float f) {
            if (this.mSize != f) {
                this.mSize = f;
                return true;
            }
            return false;
        }

        @Override // com.android.settings.widget.ChartAxis
        public float convertToPoint(long j) {
            return (this.mSize * (j - this.mMin)) / (this.mMax - this.mMin);
        }

        @Override // com.android.settings.widget.ChartAxis
        public long convertToValue(float f) {
            return (long) (this.mMin + ((f * (this.mMax - this.mMin)) / this.mSize));
        }

        @Override // com.android.settings.widget.ChartAxis
        public long buildLabel(Resources resources, SpannableStringBuilder spannableStringBuilder, long j) {
            spannableStringBuilder.replace(0, spannableStringBuilder.length(), (CharSequence) Long.toString(j));
            return j;
        }

        @Override // com.android.settings.widget.ChartAxis
        public float[] getTickPoints() {
            float[] fArr = new float[32];
            Time time = new Time();
            time.set(this.mMax);
            time.monthDay -= time.weekDay - FIRST_DAY_OF_WEEK;
            int i = 0;
            time.second = 0;
            time.minute = 0;
            time.hour = 0;
            time.normalize(true);
            for (long millis = time.toMillis(true); millis > this.mMin; millis = time.toMillis(true)) {
                if (millis <= this.mMax) {
                    fArr[i] = convertToPoint(millis);
                    i++;
                }
                time.monthDay -= 7;
                time.normalize(true);
            }
            return Arrays.copyOf(fArr, i);
        }

        @Override // com.android.settings.widget.ChartAxis
        public int shouldAdjustAxis(long j) {
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

        @Override // com.android.settings.widget.ChartAxis
        public boolean setBounds(long j, long j2) {
            if (this.mMin != j || this.mMax != j2) {
                this.mMin = j;
                this.mMax = j2;
                return true;
            }
            return false;
        }

        @Override // com.android.settings.widget.ChartAxis
        public boolean setSize(float f) {
            if (this.mSize != f) {
                this.mSize = f;
                return true;
            }
            return false;
        }

        @Override // com.android.settings.widget.ChartAxis
        public float convertToPoint(long j) {
            return (this.mSize * (j - this.mMin)) / (this.mMax - this.mMin);
        }

        @Override // com.android.settings.widget.ChartAxis
        public long convertToValue(float f) {
            return (long) (this.mMin + ((f * (this.mMax - this.mMin)) / this.mSize));
        }

        @Override // com.android.settings.widget.ChartAxis
        public long buildLabel(Resources resources, SpannableStringBuilder spannableStringBuilder, long j) {
            Formatter.BytesResult bytes = Formatter.formatBytes(resources, MathUtils.constrain(j, 0L, 1099511627776L), 3);
            ChartDataUsageView.setText(spannableStringBuilder, sSpanSize, bytes.value, "^1");
            ChartDataUsageView.setText(spannableStringBuilder, sSpanUnit, bytes.units, "^2");
            return bytes.roundedBytes;
        }

        @Override // com.android.settings.widget.ChartAxis
        public float[] getTickPoints() {
            long j = this.mMax - this.mMin;
            long jRoundUpToPowerOfTwo = ChartDataUsageView.roundUpToPowerOfTwo(j / 16);
            float[] fArr = new float[(int) (j / jRoundUpToPowerOfTwo)];
            long j2 = this.mMin;
            for (int i = 0; i < fArr.length; i++) {
                fArr[i] = convertToPoint(j2);
                j2 += jRoundUpToPowerOfTwo;
            }
            return fArr;
        }

        @Override // com.android.settings.widget.ChartAxis
        public int shouldAdjustAxis(long j) {
            double dConvertToPoint = convertToPoint(j);
            if (dConvertToPoint < this.mSize * 0.1d) {
                return -1;
            }
            if (dConvertToPoint > this.mSize * 0.85d) {
                return 1;
            }
            return 0;
        }
    }

    private static void setText(SpannableStringBuilder spannableStringBuilder, Object obj, CharSequence charSequence, String str) {
        int spanStart = spannableStringBuilder.getSpanStart(obj);
        int spanEnd = spannableStringBuilder.getSpanEnd(obj);
        if (spanStart == -1) {
            spanStart = TextUtils.indexOf(spannableStringBuilder, str);
            spanEnd = spanStart + str.length();
            spannableStringBuilder.setSpan(obj, spanStart, spanEnd, 18);
        }
        spannableStringBuilder.replace(spanStart, spanEnd, charSequence);
    }

    private static long roundUpToPowerOfTwo(long j) {
        long j2 = j - 1;
        long j3 = j2 | (j2 >>> 1);
        long j4 = j3 | (j3 >>> 2);
        long j5 = j4 | (j4 >>> 4);
        long j6 = j5 | (j5 >>> 8);
        long j7 = j6 | (j6 >>> 16);
        long j8 = (j7 | (j7 >>> 32)) + 1;
        if (j8 > 0) {
            return j8;
        }
        return Long.MAX_VALUE;
    }
}
