package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.BatteryStats;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import com.android.internal.R;
import com.android.settings.Utils;
import com.android.settingslib.BatteryInfo;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import libcore.icu.LocaleData;

public class BatteryHistoryChart extends View {
    final Path mBatCriticalPath;
    final Path mBatGoodPath;
    int mBatHigh;
    final Path mBatLevelPath;
    int mBatLow;
    final Path mBatWarnPath;
    final Paint mBatteryBackgroundPaint;
    int mBatteryCriticalLevel;
    final Paint mBatteryCriticalPaint;
    final Paint mBatteryGoodPaint;
    int mBatteryWarnLevel;
    final Paint mBatteryWarnPaint;
    Bitmap mBitmap;
    String mCameraOnLabel;
    int mCameraOnOffset;
    final Paint mCameraOnPaint;
    final Path mCameraOnPath;
    Canvas mCanvas;
    String mChargeDurationString;
    int mChargeDurationStringWidth;
    int mChargeLabelStringWidth;
    String mChargingLabel;
    int mChargingOffset;
    final Paint mChargingPaint;
    final Path mChargingPath;
    int mChartMinHeight;
    String mCpuRunningLabel;
    int mCpuRunningOffset;
    final Paint mCpuRunningPaint;
    final Path mCpuRunningPath;
    final ArrayList<DateLabel> mDateLabels;
    final Paint mDateLinePaint;
    final Path mDateLinePath;
    final Paint mDebugRectPaint;
    String mDrainString;
    int mDrainStringWidth;
    String mDurationString;
    int mDurationStringWidth;
    long mEndDataWallTime;
    long mEndWallTime;
    String mFlashlightOnLabel;
    int mFlashlightOnOffset;
    final Paint mFlashlightOnPaint;
    final Path mFlashlightOnPath;
    String mGpsOnLabel;
    int mGpsOnOffset;
    final Paint mGpsOnPaint;
    final Path mGpsOnPath;
    boolean mHaveCamera;
    boolean mHaveFlashlight;
    boolean mHaveGps;
    boolean mHavePhoneSignal;
    boolean mHaveWifi;
    int mHeaderHeight;
    int mHeaderTextAscent;
    int mHeaderTextDescent;
    final TextPaint mHeaderTextPaint;
    long mHistStart;
    BatteryInfo mInfo;
    boolean mLargeMode;
    int mLastHeight;
    int mLastWidth;
    int mLevelBottom;
    int mLevelLeft;
    int mLevelOffset;
    int mLevelRight;
    int mLevelTop;
    int mLineWidth;
    String mMaxPercentLabelString;
    int mMaxPercentLabelStringWidth;
    String mMinPercentLabelString;
    int mMinPercentLabelStringWidth;
    int mNumHist;
    final ChartData mPhoneSignalChart;
    String mPhoneSignalLabel;
    int mPhoneSignalOffset;
    String mScreenOnLabel;
    int mScreenOnOffset;
    final Paint mScreenOnPaint;
    final Path mScreenOnPath;
    long mStartWallTime;
    BatteryStats mStats;
    int mTextAscent;
    int mTextDescent;
    final TextPaint mTextPaint;
    int mThinLineWidth;
    final ArrayList<TimeLabel> mTimeLabels;
    final Paint mTimeRemainPaint;
    final Path mTimeRemainPath;
    String mWifiRunningLabel;
    int mWifiRunningOffset;
    final Paint mWifiRunningPaint;
    final Path mWifiRunningPath;

    static class ChartData {
        int[] mColors;
        int mLastBin;
        int mNumTicks;
        Paint[] mPaints;
        int[] mTicks;

        ChartData() {
        }

        void setColors(int[] colors) {
            this.mColors = colors;
            this.mPaints = new Paint[colors.length];
            for (int i = 0; i < colors.length; i++) {
                this.mPaints[i] = new Paint();
                this.mPaints[i].setColor(colors[i]);
                this.mPaints[i].setStyle(Paint.Style.FILL);
            }
        }

        void init(int width) {
            if (width > 0) {
                this.mTicks = new int[width * 2];
            } else {
                this.mTicks = null;
            }
            this.mNumTicks = 0;
            this.mLastBin = 0;
        }

        void addTick(int x, int bin) {
            if (bin == this.mLastBin || this.mNumTicks >= this.mTicks.length) {
                return;
            }
            this.mTicks[this.mNumTicks] = (65535 & x) | (bin << 16);
            this.mNumTicks++;
            this.mLastBin = bin;
        }

        void finish(int width) {
            if (this.mLastBin == 0) {
                return;
            }
            addTick(width, 0);
        }

        void draw(Canvas canvas, int top, int height) {
            int lastBin = 0;
            int lastX = 0;
            int bottom = top + height;
            for (int i = 0; i < this.mNumTicks; i++) {
                int tick = this.mTicks[i];
                int x = tick & 65535;
                int bin = ((-65536) & tick) >> 16;
                if (lastBin != 0) {
                    canvas.drawRect(lastX, top, x, bottom, this.mPaints[lastBin]);
                }
                lastBin = bin;
                lastX = x;
            }
        }
    }

    static class TextAttrs {
        ColorStateList textColor = null;
        int textSize = 15;
        int typefaceIndex = -1;
        int styleIndex = -1;

        TextAttrs() {
        }

        void retrieve(Context context, TypedArray from, int index) {
            TypedArray appearance = null;
            int ap = from.getResourceId(index, -1);
            if (ap != -1) {
                appearance = context.obtainStyledAttributes(ap, R.styleable.TextAppearance);
            }
            if (appearance == null) {
                return;
            }
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);
                switch (attr) {
                    case DefaultWfcSettingsExt.RESUME:
                        this.textSize = appearance.getDimensionPixelSize(attr, this.textSize);
                        break;
                    case DefaultWfcSettingsExt.PAUSE:
                        this.typefaceIndex = appearance.getInt(attr, -1);
                        break;
                    case DefaultWfcSettingsExt.CREATE:
                        this.styleIndex = appearance.getInt(attr, -1);
                        break;
                    case DefaultWfcSettingsExt.DESTROY:
                        this.textColor = appearance.getColorStateList(attr);
                        break;
                }
            }
            appearance.recycle();
        }

        void apply(Context context, TextPaint paint) {
            paint.density = context.getResources().getDisplayMetrics().density;
            paint.setCompatibilityScaling(context.getResources().getCompatibilityInfo().applicationScale);
            paint.setColor(this.textColor.getDefaultColor());
            paint.setTextSize(this.textSize);
            Typeface tf = null;
            switch (this.typefaceIndex) {
                case DefaultWfcSettingsExt.PAUSE:
                    tf = Typeface.SANS_SERIF;
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    tf = Typeface.SERIF;
                    break;
                case DefaultWfcSettingsExt.DESTROY:
                    tf = Typeface.MONOSPACE;
                    break;
            }
            setTypeface(paint, tf, this.styleIndex);
        }

        public void setTypeface(TextPaint paint, Typeface tf, int style) {
            Typeface tf2;
            if (style > 0) {
                if (tf == null) {
                    tf2 = Typeface.defaultFromStyle(style);
                } else {
                    tf2 = Typeface.create(tf, style);
                }
                paint.setTypeface(tf2);
                int typefaceStyle = tf2 != null ? tf2.getStyle() : 0;
                int need = style & (~typefaceStyle);
                paint.setFakeBoldText((need & 1) != 0);
                paint.setTextSkewX((need & 2) != 0 ? -0.25f : 0.0f);
                return;
            }
            paint.setFakeBoldText(false);
            paint.setTextSkewX(0.0f);
            paint.setTypeface(tf);
        }
    }

    static class TimeLabel {
        final String label;
        final int width;
        final int x;

        TimeLabel(TextPaint paint, int x, Calendar cal, boolean use24hr) {
            this.x = x;
            String bestFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(), use24hr ? "km" : "ha");
            this.label = DateFormat.format(bestFormat, cal).toString();
            this.width = (int) paint.measureText(this.label);
        }
    }

    static class DateLabel {
        final String label;
        final int width;
        final int x;

        DateLabel(TextPaint paint, int x, Calendar cal, boolean dayFirst) {
            this.x = x;
            String bestFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(), dayFirst ? "dM" : "Md");
            this.label = DateFormat.format(bestFormat, cal).toString();
            this.width = (int) paint.measureText(this.label);
        }
    }

    public BatteryHistoryChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBatteryBackgroundPaint = new Paint(1);
        this.mBatteryGoodPaint = new Paint(1);
        this.mBatteryWarnPaint = new Paint(1);
        this.mBatteryCriticalPaint = new Paint(1);
        this.mTimeRemainPaint = new Paint(1);
        this.mChargingPaint = new Paint();
        this.mScreenOnPaint = new Paint();
        this.mGpsOnPaint = new Paint();
        this.mFlashlightOnPaint = new Paint();
        this.mCameraOnPaint = new Paint();
        this.mWifiRunningPaint = new Paint();
        this.mCpuRunningPaint = new Paint();
        this.mDateLinePaint = new Paint();
        this.mPhoneSignalChart = new ChartData();
        this.mTextPaint = new TextPaint(1);
        this.mHeaderTextPaint = new TextPaint(1);
        this.mDebugRectPaint = new Paint();
        this.mBatLevelPath = new Path();
        this.mBatGoodPath = new Path();
        this.mBatWarnPath = new Path();
        this.mBatCriticalPath = new Path();
        this.mTimeRemainPath = new Path();
        this.mChargingPath = new Path();
        this.mScreenOnPath = new Path();
        this.mGpsOnPath = new Path();
        this.mFlashlightOnPath = new Path();
        this.mCameraOnPath = new Path();
        this.mWifiRunningPath = new Path();
        this.mCpuRunningPath = new Path();
        this.mDateLinePath = new Path();
        this.mLastWidth = -1;
        this.mLastHeight = -1;
        this.mTimeLabels = new ArrayList<>();
        this.mDateLabels = new ArrayList<>();
        this.mBatteryWarnLevel = this.mContext.getResources().getInteger(android.R.integer.config_debugSystemServerPssThresholdBytes);
        this.mBatteryCriticalLevel = this.mContext.getResources().getInteger(android.R.integer.config_datause_threshold_bytes);
        this.mThinLineWidth = (int) TypedValue.applyDimension(1, 2.0f, getResources().getDisplayMetrics());
        this.mBatteryBackgroundPaint.setColor(-16738680);
        this.mBatteryBackgroundPaint.setStyle(Paint.Style.FILL);
        this.mBatteryGoodPaint.setARGB(128, 0, 128, 0);
        this.mBatteryGoodPaint.setStyle(Paint.Style.STROKE);
        this.mBatteryWarnPaint.setARGB(128, 128, 128, 0);
        this.mBatteryWarnPaint.setStyle(Paint.Style.STROKE);
        this.mBatteryCriticalPaint.setARGB(192, 128, 0, 0);
        this.mBatteryCriticalPaint.setStyle(Paint.Style.STROKE);
        this.mTimeRemainPaint.setColor(-3221573);
        this.mTimeRemainPaint.setStyle(Paint.Style.FILL);
        this.mChargingPaint.setStyle(Paint.Style.STROKE);
        this.mScreenOnPaint.setStyle(Paint.Style.STROKE);
        this.mGpsOnPaint.setStyle(Paint.Style.STROKE);
        this.mCameraOnPaint.setStyle(Paint.Style.STROKE);
        this.mFlashlightOnPaint.setStyle(Paint.Style.STROKE);
        this.mWifiRunningPaint.setStyle(Paint.Style.STROKE);
        this.mCpuRunningPaint.setStyle(Paint.Style.STROKE);
        this.mPhoneSignalChart.setColors(Utils.BADNESS_COLORS);
        this.mDebugRectPaint.setARGB(255, 255, 0, 0);
        this.mDebugRectPaint.setStyle(Paint.Style.STROKE);
        this.mScreenOnPaint.setColor(-16738680);
        this.mGpsOnPaint.setColor(-16738680);
        this.mCameraOnPaint.setColor(-16738680);
        this.mFlashlightOnPaint.setColor(-16738680);
        this.mWifiRunningPaint.setColor(-16738680);
        this.mCpuRunningPaint.setColor(-16738680);
        this.mChargingPaint.setColor(-16738680);
        TypedArray a = context.obtainStyledAttributes(attrs, com.android.settings.R.styleable.BatteryHistoryChart, 0, 0);
        TextAttrs mainTextAttrs = new TextAttrs();
        TextAttrs headTextAttrs = new TextAttrs();
        mainTextAttrs.retrieve(context, a, 0);
        headTextAttrs.retrieve(context, a, 9);
        int shadowcolor = 0;
        float dx = 0.0f;
        float dy = 0.0f;
        float r = 0.0f;
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case DefaultWfcSettingsExt.PAUSE:
                    mainTextAttrs.textSize = a.getDimensionPixelSize(attr, mainTextAttrs.textSize);
                    headTextAttrs.textSize = a.getDimensionPixelSize(attr, headTextAttrs.textSize);
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    mainTextAttrs.typefaceIndex = a.getInt(attr, mainTextAttrs.typefaceIndex);
                    headTextAttrs.typefaceIndex = a.getInt(attr, headTextAttrs.typefaceIndex);
                    break;
                case DefaultWfcSettingsExt.DESTROY:
                    mainTextAttrs.styleIndex = a.getInt(attr, mainTextAttrs.styleIndex);
                    headTextAttrs.styleIndex = a.getInt(attr, headTextAttrs.styleIndex);
                    break;
                case DefaultWfcSettingsExt.CONFIG_CHANGE:
                    mainTextAttrs.textColor = a.getColorStateList(attr);
                    headTextAttrs.textColor = a.getColorStateList(attr);
                    break;
                case 5:
                    shadowcolor = a.getInt(attr, 0);
                    break;
                case 6:
                    dx = a.getFloat(attr, 0.0f);
                    break;
                case 7:
                    dy = a.getFloat(attr, 0.0f);
                    break;
                case 8:
                    r = a.getFloat(attr, 0.0f);
                    break;
                case 10:
                    this.mBatteryBackgroundPaint.setColor(a.getInt(attr, 0));
                    this.mScreenOnPaint.setColor(a.getInt(attr, 0));
                    this.mGpsOnPaint.setColor(a.getInt(attr, 0));
                    this.mCameraOnPaint.setColor(a.getInt(attr, 0));
                    this.mFlashlightOnPaint.setColor(a.getInt(attr, 0));
                    this.mWifiRunningPaint.setColor(a.getInt(attr, 0));
                    this.mCpuRunningPaint.setColor(a.getInt(attr, 0));
                    this.mChargingPaint.setColor(a.getInt(attr, 0));
                    break;
                case 11:
                    this.mTimeRemainPaint.setColor(a.getInt(attr, 0));
                    break;
                case 12:
                    this.mChartMinHeight = a.getDimensionPixelSize(attr, 0);
                    break;
            }
        }
        a.recycle();
        mainTextAttrs.apply(context, this.mTextPaint);
        headTextAttrs.apply(context, this.mHeaderTextPaint);
        this.mDateLinePaint.set(this.mTextPaint);
        this.mDateLinePaint.setStyle(Paint.Style.STROKE);
        int hairlineWidth = this.mThinLineWidth / 2;
        this.mDateLinePaint.setStrokeWidth(hairlineWidth < 1 ? 1 : hairlineWidth);
        this.mDateLinePaint.setPathEffect(new DashPathEffect(new float[]{this.mThinLineWidth * 2, this.mThinLineWidth * 2}, 0.0f));
        if (shadowcolor == 0) {
            return;
        }
        this.mTextPaint.setShadowLayer(r, dx, dy, shadowcolor);
        this.mHeaderTextPaint.setShadowLayer(r, dx, dy, shadowcolor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mMaxPercentLabelStringWidth = (int) this.mTextPaint.measureText(this.mMaxPercentLabelString);
        this.mMinPercentLabelStringWidth = (int) this.mTextPaint.measureText(this.mMinPercentLabelString);
        this.mDrainStringWidth = (int) this.mHeaderTextPaint.measureText(this.mDrainString);
        this.mChargeLabelStringWidth = (int) this.mHeaderTextPaint.measureText(this.mInfo.mChargeLabelString);
        this.mChargeDurationStringWidth = (int) this.mHeaderTextPaint.measureText(this.mChargeDurationString);
        this.mTextAscent = (int) this.mTextPaint.ascent();
        this.mTextDescent = (int) this.mTextPaint.descent();
        this.mHeaderTextAscent = (int) this.mHeaderTextPaint.ascent();
        this.mHeaderTextDescent = (int) this.mHeaderTextPaint.descent();
        int headerTextHeight = this.mHeaderTextDescent - this.mHeaderTextAscent;
        this.mHeaderHeight = (headerTextHeight * 2) - this.mTextAscent;
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), getDefaultSize(this.mChartMinHeight + this.mHeaderHeight, heightMeasureSpec));
    }

    void finishPaths(int w, int h, int levelh, int startX, int y, Path curLevelPath, int lastX, boolean lastCharging, boolean lastScreenOn, boolean lastGpsOn, boolean lastFlashlightOn, boolean lastCameraOn, boolean lastWifiRunning, boolean lastCpuRunning, Path lastPath) {
        if (curLevelPath != null) {
            if (lastX >= 0 && lastX < w) {
                if (lastPath != null) {
                    lastPath.lineTo(w, y);
                }
                curLevelPath.lineTo(w, y);
            }
            curLevelPath.lineTo(w, this.mLevelTop + levelh);
            curLevelPath.lineTo(startX, this.mLevelTop + levelh);
            curLevelPath.close();
        }
        if (lastCharging) {
            this.mChargingPath.lineTo(w, h - this.mChargingOffset);
        }
        if (lastScreenOn) {
            this.mScreenOnPath.lineTo(w, h - this.mScreenOnOffset);
        }
        if (lastGpsOn) {
            this.mGpsOnPath.lineTo(w, h - this.mGpsOnOffset);
        }
        if (lastFlashlightOn) {
            this.mFlashlightOnPath.lineTo(w, h - this.mFlashlightOnOffset);
        }
        if (lastCameraOn) {
            this.mCameraOnPath.lineTo(w, h - this.mCameraOnOffset);
        }
        if (lastWifiRunning) {
            this.mWifiRunningPath.lineTo(w, h - this.mWifiRunningOffset);
        }
        if (lastCpuRunning) {
            this.mCpuRunningPath.lineTo(w, h - this.mCpuRunningOffset);
        }
        if (!this.mHavePhoneSignal) {
            return;
        }
        this.mPhoneSignalChart.finish(w);
    }

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(getContext());
    }

    private boolean isDayFirst() {
        String value = LocaleData.get(getResources().getConfiguration().locale).getDateFormat(3);
        return value.indexOf(77) > value.indexOf(100);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int x;
        boolean wifiRunning;
        int bin;
        super.onSizeChanged(w, h, oldw, oldh);
        if ((this.mLastWidth == w && this.mLastHeight == h) || this.mLastWidth == 0 || this.mLastHeight == 0) {
            return;
        }
        this.mLastWidth = w;
        this.mLastHeight = h;
        this.mBitmap = null;
        this.mCanvas = null;
        int textHeight = this.mTextDescent - this.mTextAscent;
        if (h > (textHeight * 10) + this.mChartMinHeight) {
            this.mLargeMode = true;
            if (h > textHeight * 15) {
                this.mLineWidth = textHeight / 2;
            } else {
                this.mLineWidth = textHeight / 3;
            }
        } else {
            this.mLargeMode = false;
            this.mLineWidth = this.mThinLineWidth;
        }
        if (this.mLineWidth <= 0) {
            this.mLineWidth = 1;
        }
        this.mLevelTop = this.mHeaderHeight;
        this.mLevelLeft = this.mMaxPercentLabelStringWidth + (this.mThinLineWidth * 3);
        this.mLevelRight = w;
        int levelWidth = this.mLevelRight - this.mLevelLeft;
        this.mTextPaint.setStrokeWidth(this.mThinLineWidth);
        this.mBatteryGoodPaint.setStrokeWidth(this.mThinLineWidth);
        this.mBatteryWarnPaint.setStrokeWidth(this.mThinLineWidth);
        this.mBatteryCriticalPaint.setStrokeWidth(this.mThinLineWidth);
        this.mChargingPaint.setStrokeWidth(this.mLineWidth);
        this.mScreenOnPaint.setStrokeWidth(this.mLineWidth);
        this.mGpsOnPaint.setStrokeWidth(this.mLineWidth);
        this.mCameraOnPaint.setStrokeWidth(this.mLineWidth);
        this.mFlashlightOnPaint.setStrokeWidth(this.mLineWidth);
        this.mWifiRunningPaint.setStrokeWidth(this.mLineWidth);
        this.mCpuRunningPaint.setStrokeWidth(this.mLineWidth);
        this.mDebugRectPaint.setStrokeWidth(1.0f);
        int fullBarOffset = textHeight + this.mLineWidth;
        if (this.mLargeMode) {
            this.mChargingOffset = this.mLineWidth;
            this.mScreenOnOffset = this.mChargingOffset + fullBarOffset;
            this.mCpuRunningOffset = this.mScreenOnOffset + fullBarOffset;
            this.mWifiRunningOffset = this.mCpuRunningOffset + fullBarOffset;
            this.mGpsOnOffset = (this.mHaveWifi ? fullBarOffset : 0) + this.mWifiRunningOffset;
            this.mFlashlightOnOffset = (this.mHaveGps ? fullBarOffset : 0) + this.mGpsOnOffset;
            this.mCameraOnOffset = (this.mHaveFlashlight ? fullBarOffset : 0) + this.mFlashlightOnOffset;
            this.mPhoneSignalOffset = (this.mHaveCamera ? fullBarOffset : 0) + this.mCameraOnOffset;
            int i = this.mPhoneSignalOffset;
            if (!this.mHavePhoneSignal) {
                fullBarOffset = 0;
            }
            this.mLevelOffset = i + fullBarOffset + (this.mLineWidth * 2) + (this.mLineWidth / 2);
            if (this.mHavePhoneSignal) {
                this.mPhoneSignalChart.init(w);
            }
        } else {
            this.mPhoneSignalOffset = 0;
            this.mChargingOffset = 0;
            this.mCpuRunningOffset = 0;
            this.mWifiRunningOffset = 0;
            this.mFlashlightOnOffset = 0;
            this.mCameraOnOffset = 0;
            this.mGpsOnOffset = 0;
            this.mScreenOnOffset = 0;
            this.mLevelOffset = (this.mThinLineWidth * 4) + fullBarOffset;
            if (this.mHavePhoneSignal) {
                this.mPhoneSignalChart.init(0);
            }
        }
        this.mBatLevelPath.reset();
        this.mBatGoodPath.reset();
        this.mBatWarnPath.reset();
        this.mTimeRemainPath.reset();
        this.mBatCriticalPath.reset();
        this.mScreenOnPath.reset();
        this.mGpsOnPath.reset();
        this.mFlashlightOnPath.reset();
        this.mCameraOnPath.reset();
        this.mWifiRunningPath.reset();
        this.mCpuRunningPath.reset();
        this.mChargingPath.reset();
        this.mTimeLabels.clear();
        this.mDateLabels.clear();
        long walltimeStart = this.mStartWallTime;
        long walltimeChange = this.mEndWallTime > walltimeStart ? this.mEndWallTime - walltimeStart : 1L;
        long curWalltime = this.mStartWallTime;
        long lastRealtime = 0;
        int batLow = this.mBatLow;
        int batChange = this.mBatHigh - this.mBatLow;
        int levelh = (h - this.mLevelOffset) - this.mLevelTop;
        this.mLevelBottom = this.mLevelTop + levelh;
        int x2 = this.mLevelLeft;
        int startX = this.mLevelLeft;
        int lastX = -1;
        int lastY = -1;
        int i2 = 0;
        Path curLevelPath = null;
        Path lastLinePath = null;
        boolean lastCharging = false;
        boolean lastScreenOn = false;
        boolean lastGpsOn = false;
        boolean lastFlashlightOn = false;
        boolean lastCameraOn = false;
        boolean lastWifiRunning = false;
        boolean lastWifiSupplRunning = false;
        boolean lastCpuRunning = false;
        int lastWifiSupplState = 0;
        int N = this.mNumHist;
        if (this.mEndDataWallTime > this.mStartWallTime && this.mStats.startIteratingHistoryLocked()) {
            BatteryStats.HistoryItem rec = new BatteryStats.HistoryItem();
            while (true) {
                int x3 = x2;
                if (this.mStats.getNextHistoryLocked(rec) && i2 < N) {
                    if (rec.isDeltaData()) {
                        curWalltime += rec.time - lastRealtime;
                        lastRealtime = rec.time;
                        x2 = this.mLevelLeft + ((int) (((curWalltime - walltimeStart) * ((long) levelWidth)) / walltimeChange));
                        if (x2 < 0) {
                            x2 = 0;
                        }
                        int y = (this.mLevelTop + levelh) - (((rec.batteryLevel - batLow) * (levelh - 1)) / batChange);
                        if (lastX != x2 && lastY != y) {
                            byte value = rec.batteryLevel;
                            Path path = value <= this.mBatteryCriticalLevel ? this.mBatCriticalPath : value <= this.mBatteryWarnLevel ? this.mBatWarnPath : null;
                            if (path != lastLinePath) {
                                if (lastLinePath != null) {
                                    lastLinePath.lineTo(x2, y);
                                }
                                if (path != null) {
                                    path.moveTo(x2, y);
                                }
                                lastLinePath = path;
                            } else if (path != null) {
                                path.lineTo(x2, y);
                            }
                            if (curLevelPath == null) {
                                curLevelPath = this.mBatLevelPath;
                                curLevelPath.moveTo(x2, y);
                                startX = x2;
                            } else {
                                curLevelPath.lineTo(x2, y);
                            }
                            lastX = x2;
                            lastY = y;
                        }
                        if (this.mLargeMode) {
                            boolean charging = (rec.states & 524288) != 0;
                            if (charging != lastCharging) {
                                if (charging) {
                                    this.mChargingPath.moveTo(x2, h - this.mChargingOffset);
                                } else {
                                    this.mChargingPath.lineTo(x2, h - this.mChargingOffset);
                                }
                                lastCharging = charging;
                            }
                            boolean screenOn = (rec.states & 1048576) != 0;
                            if (screenOn != lastScreenOn) {
                                if (screenOn) {
                                    this.mScreenOnPath.moveTo(x2, h - this.mScreenOnOffset);
                                } else {
                                    this.mScreenOnPath.lineTo(x2, h - this.mScreenOnOffset);
                                }
                                lastScreenOn = screenOn;
                            }
                            boolean gpsOn = (rec.states & 536870912) != 0;
                            if (gpsOn != lastGpsOn) {
                                if (gpsOn) {
                                    this.mGpsOnPath.moveTo(x2, h - this.mGpsOnOffset);
                                } else {
                                    this.mGpsOnPath.lineTo(x2, h - this.mGpsOnOffset);
                                }
                                lastGpsOn = gpsOn;
                            }
                            boolean flashlightOn = (rec.states2 & 134217728) != 0;
                            if (flashlightOn != lastFlashlightOn) {
                                if (flashlightOn) {
                                    this.mFlashlightOnPath.moveTo(x2, h - this.mFlashlightOnOffset);
                                } else {
                                    this.mFlashlightOnPath.lineTo(x2, h - this.mFlashlightOnOffset);
                                }
                                lastFlashlightOn = flashlightOn;
                            }
                            boolean cameraOn = (rec.states2 & 2097152) != 0;
                            if (cameraOn != lastCameraOn) {
                                if (cameraOn) {
                                    this.mCameraOnPath.moveTo(x2, h - this.mCameraOnOffset);
                                } else {
                                    this.mCameraOnPath.lineTo(x2, h - this.mCameraOnOffset);
                                }
                                lastCameraOn = cameraOn;
                            }
                            int wifiSupplState = (rec.states2 & 15) >> 0;
                            if (lastWifiSupplState != wifiSupplState) {
                                lastWifiSupplState = wifiSupplState;
                                switch (wifiSupplState) {
                                    case DefaultWfcSettingsExt.RESUME:
                                    case DefaultWfcSettingsExt.PAUSE:
                                    case DefaultWfcSettingsExt.CREATE:
                                    case DefaultWfcSettingsExt.DESTROY:
                                    case 11:
                                    case 12:
                                        lastWifiSupplRunning = false;
                                        wifiRunning = false;
                                        break;
                                    case DefaultWfcSettingsExt.CONFIG_CHANGE:
                                    case 5:
                                    case 6:
                                    case 7:
                                    case 8:
                                    case 9:
                                    case 10:
                                    default:
                                        lastWifiSupplRunning = true;
                                        wifiRunning = true;
                                        break;
                                }
                            } else {
                                wifiRunning = lastWifiSupplRunning;
                            }
                            if ((rec.states & 402718720) != 0) {
                                wifiRunning = true;
                            }
                            if (wifiRunning != lastWifiRunning) {
                                if (wifiRunning) {
                                    this.mWifiRunningPath.moveTo(x2, h - this.mWifiRunningOffset);
                                } else {
                                    this.mWifiRunningPath.lineTo(x2, h - this.mWifiRunningOffset);
                                }
                                lastWifiRunning = wifiRunning;
                            }
                            boolean cpuRunning = (rec.states & Integer.MIN_VALUE) != 0;
                            if (cpuRunning != lastCpuRunning) {
                                if (cpuRunning) {
                                    this.mCpuRunningPath.moveTo(x2, h - this.mCpuRunningOffset);
                                } else {
                                    this.mCpuRunningPath.lineTo(x2, h - this.mCpuRunningOffset);
                                }
                                lastCpuRunning = cpuRunning;
                            }
                            if (this.mLargeMode && this.mHavePhoneSignal) {
                                if (((rec.states & 448) >> 6) == 3) {
                                    bin = 0;
                                } else if ((rec.states & 2097152) != 0) {
                                    bin = 1;
                                } else {
                                    int bin2 = (rec.states & 56) >> 3;
                                    bin = bin2 + 2;
                                }
                                this.mPhoneSignalChart.addTick(x2, bin);
                            }
                        }
                    } else {
                        long lastWalltime = curWalltime;
                        if (rec.cmd == 5 || rec.cmd == 7) {
                            curWalltime = rec.currentTime >= this.mStartWallTime ? rec.currentTime : this.mStartWallTime + (rec.time - this.mHistStart);
                            lastRealtime = rec.time;
                        }
                        if (rec.cmd == 6 || (rec.cmd == 5 && Math.abs(lastWalltime - curWalltime) <= 3600000)) {
                            x2 = x3;
                        } else if (curLevelPath != null) {
                            finishPaths(x3 + 1, h, levelh, startX, lastY, curLevelPath, lastX, lastCharging, lastScreenOn, lastGpsOn, lastFlashlightOn, lastCameraOn, lastWifiRunning, lastCpuRunning, lastLinePath);
                            lastY = -1;
                            lastX = -1;
                            curLevelPath = null;
                            lastLinePath = null;
                            lastCpuRunning = false;
                            lastCameraOn = false;
                            lastFlashlightOn = false;
                            lastGpsOn = false;
                            lastScreenOn = false;
                            lastCharging = false;
                            x2 = x3;
                        } else {
                            x2 = x3;
                        }
                    }
                    i2++;
                }
            }
            this.mStats.finishIteratingHistoryLocked();
        }
        if (lastY < 0 || lastX < 0) {
            lastX = this.mLevelLeft;
            lastY = (this.mLevelTop + levelh) - (((this.mInfo.mBatteryLevel - batLow) * (levelh - 1)) / batChange);
            byte value2 = (byte) this.mInfo.mBatteryLevel;
            Path path2 = value2 <= this.mBatteryCriticalLevel ? this.mBatCriticalPath : value2 <= this.mBatteryWarnLevel ? this.mBatWarnPath : null;
            if (path2 != null) {
                path2.moveTo(lastX, lastY);
                lastLinePath = path2;
            }
            this.mBatLevelPath.moveTo(lastX, lastY);
            curLevelPath = this.mBatLevelPath;
            x = w;
        } else {
            x = this.mLevelLeft + ((int) (((this.mEndDataWallTime - walltimeStart) * ((long) levelWidth)) / walltimeChange));
            if (x < 0) {
                x = 0;
            }
        }
        finishPaths(x, h, levelh, startX, lastY, curLevelPath, lastX, lastCharging, lastScreenOn, lastGpsOn, lastFlashlightOn, lastCameraOn, lastWifiRunning, lastCpuRunning, lastLinePath);
        if (x < w) {
            this.mTimeRemainPath.moveTo(x, lastY);
            int fullY = (this.mLevelTop + levelh) - (((100 - batLow) * (levelh - 1)) / batChange);
            int emptyY = (this.mLevelTop + levelh) - (((0 - batLow) * (levelh - 1)) / batChange);
            if (this.mInfo.mDischarging) {
                this.mTimeRemainPath.lineTo(this.mLevelRight, emptyY);
            } else {
                this.mTimeRemainPath.lineTo(this.mLevelRight, fullY);
                this.mTimeRemainPath.lineTo(this.mLevelRight, emptyY);
            }
            this.mTimeRemainPath.lineTo(x, emptyY);
            this.mTimeRemainPath.close();
        }
        if (this.mStartWallTime > 0 && this.mEndWallTime > this.mStartWallTime) {
            boolean is24hr = is24Hour();
            Calendar calStart = Calendar.getInstance();
            calStart.setTimeInMillis(this.mStartWallTime);
            calStart.set(14, 0);
            calStart.set(13, 0);
            calStart.set(12, 0);
            long startRoundTime = calStart.getTimeInMillis();
            if (startRoundTime < this.mStartWallTime) {
                calStart.set(11, calStart.get(11) + 1);
                startRoundTime = calStart.getTimeInMillis();
            }
            Calendar calEnd = Calendar.getInstance();
            calEnd.setTimeInMillis(this.mEndWallTime);
            calEnd.set(14, 0);
            calEnd.set(13, 0);
            calEnd.set(12, 0);
            long endRoundTime = calEnd.getTimeInMillis();
            if (startRoundTime < endRoundTime) {
                addTimeLabel(calStart, this.mLevelLeft, this.mLevelRight, is24hr);
                Calendar calMid = Calendar.getInstance();
                calMid.setTimeInMillis(this.mStartWallTime + ((this.mEndWallTime - this.mStartWallTime) / 2));
                calMid.set(14, 0);
                calMid.set(13, 0);
                calMid.set(12, 0);
                long calMidMillis = calMid.getTimeInMillis();
                if (calMidMillis > startRoundTime && calMidMillis < endRoundTime) {
                    addTimeLabel(calMid, this.mLevelLeft, this.mLevelRight, is24hr);
                }
                addTimeLabel(calEnd, this.mLevelLeft, this.mLevelRight, is24hr);
            }
            if (calStart.get(6) != calEnd.get(6) || calStart.get(1) != calEnd.get(1)) {
                boolean isDayFirst = isDayFirst();
                calStart.set(11, 0);
                long startRoundTime2 = calStart.getTimeInMillis();
                if (startRoundTime2 < this.mStartWallTime) {
                    calStart.set(6, calStart.get(6) + 1);
                    startRoundTime2 = calStart.getTimeInMillis();
                }
                calEnd.set(11, 0);
                long endRoundTime2 = calEnd.getTimeInMillis();
                if (startRoundTime2 < endRoundTime2) {
                    addDateLabel(calStart, this.mLevelLeft, this.mLevelRight, isDayFirst);
                    Calendar calMid2 = Calendar.getInstance();
                    calMid2.setTimeInMillis(((endRoundTime2 - startRoundTime2) / 2) + startRoundTime2 + 7200000);
                    calMid2.set(11, 0);
                    calMid2.set(12, 0);
                    long calMidMillis2 = calMid2.getTimeInMillis();
                    if (calMidMillis2 > startRoundTime2 && calMidMillis2 < endRoundTime2) {
                        addDateLabel(calMid2, this.mLevelLeft, this.mLevelRight, isDayFirst);
                    }
                }
                addDateLabel(calEnd, this.mLevelLeft, this.mLevelRight, isDayFirst);
            }
        }
        if (this.mTimeLabels.size() < 2) {
            this.mDurationString = Formatter.formatShortElapsedTime(getContext(), this.mEndWallTime - this.mStartWallTime);
            this.mDurationStringWidth = (int) this.mTextPaint.measureText(this.mDurationString);
        } else {
            this.mDurationString = null;
            this.mDurationStringWidth = 0;
        }
    }

    void addTimeLabel(Calendar cal, int levelLeft, int levelRight, boolean is24hr) {
        long walltimeStart = this.mStartWallTime;
        long walltimeChange = this.mEndWallTime - walltimeStart;
        this.mTimeLabels.add(new TimeLabel(this.mTextPaint, ((int) (((cal.getTimeInMillis() - walltimeStart) * ((long) (levelRight - levelLeft))) / walltimeChange)) + levelLeft, cal, is24hr));
    }

    void addDateLabel(Calendar cal, int levelLeft, int levelRight, boolean isDayFirst) {
        long walltimeStart = this.mStartWallTime;
        long walltimeChange = this.mEndWallTime - walltimeStart;
        this.mDateLabels.add(new DateLabel(this.mTextPaint, ((int) (((cal.getTimeInMillis() - walltimeStart) * ((long) (levelRight - levelLeft))) / walltimeChange)) + levelLeft, cal, isDayFirst));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        drawChart(canvas, width, height);
    }

    void drawChart(Canvas canvas, int width, int height) {
        boolean layoutRtl = isLayoutRtl();
        int textStartX = layoutRtl ? width : 0;
        int textEndX = layoutRtl ? 0 : width;
        Paint.Align textAlignLeft = layoutRtl ? Paint.Align.RIGHT : Paint.Align.LEFT;
        Paint.Align textAlignRight = layoutRtl ? Paint.Align.LEFT : Paint.Align.RIGHT;
        canvas.drawPath(this.mBatLevelPath, this.mBatteryBackgroundPaint);
        if (!this.mTimeRemainPath.isEmpty()) {
            canvas.drawPath(this.mTimeRemainPath, this.mTimeRemainPaint);
        }
        if (this.mTimeLabels.size() > 1) {
            int y = (this.mLevelBottom - this.mTextAscent) + (this.mThinLineWidth * 4);
            int ytick = this.mLevelBottom + this.mThinLineWidth + (this.mThinLineWidth / 2);
            this.mTextPaint.setTextAlign(Paint.Align.LEFT);
            int lastX = 0;
            for (int i = 0; i < this.mTimeLabels.size(); i++) {
                TimeLabel label = this.mTimeLabels.get(i);
                if (i == 0) {
                    int x = label.x - (label.width / 2);
                    if (x < 0) {
                        x = 0;
                    }
                    canvas.drawText(label.label, x, y, this.mTextPaint);
                    canvas.drawLine(label.x, ytick, label.x, this.mThinLineWidth + ytick, this.mTextPaint);
                    lastX = x + label.width;
                } else if (i < this.mTimeLabels.size() - 1) {
                    int x2 = label.x - (label.width / 2);
                    if (x2 >= this.mTextAscent + lastX) {
                        TimeLabel nextLabel = this.mTimeLabels.get(i + 1);
                        if (x2 <= (width - nextLabel.width) - this.mTextAscent) {
                            canvas.drawText(label.label, x2, y, this.mTextPaint);
                            canvas.drawLine(label.x, ytick, label.x, this.mThinLineWidth + ytick, this.mTextPaint);
                            lastX = x2 + label.width;
                        }
                    }
                } else {
                    int x3 = label.x - (label.width / 2);
                    if (label.width + x3 >= width) {
                        x3 = (width - 1) - label.width;
                    }
                    canvas.drawText(label.label, x3, y, this.mTextPaint);
                    canvas.drawLine(label.x, ytick, label.x, this.mThinLineWidth + ytick, this.mTextPaint);
                }
            }
        } else if (this.mDurationString != null) {
            int y2 = (this.mLevelBottom - this.mTextAscent) + (this.mThinLineWidth * 4);
            this.mTextPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(this.mDurationString, (this.mLevelLeft + ((this.mLevelRight - this.mLevelLeft) / 2)) - (this.mDurationStringWidth / 2), y2, this.mTextPaint);
        }
        int headerTop = (-this.mHeaderTextAscent) + ((this.mHeaderTextDescent - this.mHeaderTextAscent) / 3);
        this.mHeaderTextPaint.setTextAlign(textAlignLeft);
        canvas.drawText(this.mInfo.mChargeLabelString, textStartX, headerTop, this.mHeaderTextPaint);
        int stringHalfWidth = this.mChargeDurationStringWidth / 2;
        if (layoutRtl) {
            stringHalfWidth = -stringHalfWidth;
        }
        int headerCenter = (((width - this.mChargeDurationStringWidth) - this.mDrainStringWidth) / 2) + (layoutRtl ? this.mDrainStringWidth : this.mChargeLabelStringWidth);
        canvas.drawText(this.mChargeDurationString, headerCenter - stringHalfWidth, headerTop, this.mHeaderTextPaint);
        this.mHeaderTextPaint.setTextAlign(textAlignRight);
        canvas.drawText(this.mDrainString, textEndX, headerTop, this.mHeaderTextPaint);
        if (!this.mBatGoodPath.isEmpty()) {
            canvas.drawPath(this.mBatGoodPath, this.mBatteryGoodPaint);
        }
        if (!this.mBatWarnPath.isEmpty()) {
            canvas.drawPath(this.mBatWarnPath, this.mBatteryWarnPaint);
        }
        if (!this.mBatCriticalPath.isEmpty()) {
            canvas.drawPath(this.mBatCriticalPath, this.mBatteryCriticalPaint);
        }
        if (this.mHavePhoneSignal) {
            int top = (height - this.mPhoneSignalOffset) - (this.mLineWidth / 2);
            this.mPhoneSignalChart.draw(canvas, top, this.mLineWidth);
        }
        if (!this.mScreenOnPath.isEmpty()) {
            canvas.drawPath(this.mScreenOnPath, this.mScreenOnPaint);
        }
        if (!this.mChargingPath.isEmpty()) {
            canvas.drawPath(this.mChargingPath, this.mChargingPaint);
        }
        if (this.mHaveGps && !this.mGpsOnPath.isEmpty()) {
            canvas.drawPath(this.mGpsOnPath, this.mGpsOnPaint);
        }
        if (this.mHaveFlashlight && !this.mFlashlightOnPath.isEmpty()) {
            canvas.drawPath(this.mFlashlightOnPath, this.mFlashlightOnPaint);
        }
        if (this.mHaveCamera && !this.mCameraOnPath.isEmpty()) {
            canvas.drawPath(this.mCameraOnPath, this.mCameraOnPaint);
        }
        if (this.mHaveWifi && !this.mWifiRunningPath.isEmpty()) {
            canvas.drawPath(this.mWifiRunningPath, this.mWifiRunningPaint);
        }
        if (!this.mCpuRunningPath.isEmpty()) {
            canvas.drawPath(this.mCpuRunningPath, this.mCpuRunningPaint);
        }
        if (this.mLargeMode) {
            Paint.Align align = this.mTextPaint.getTextAlign();
            this.mTextPaint.setTextAlign(textAlignLeft);
            if (this.mHavePhoneSignal) {
                canvas.drawText(this.mPhoneSignalLabel, textStartX, (height - this.mPhoneSignalOffset) - this.mTextDescent, this.mTextPaint);
            }
            if (this.mHaveGps) {
                canvas.drawText(this.mGpsOnLabel, textStartX, (height - this.mGpsOnOffset) - this.mTextDescent, this.mTextPaint);
            }
            if (this.mHaveFlashlight) {
                canvas.drawText(this.mFlashlightOnLabel, textStartX, (height - this.mFlashlightOnOffset) - this.mTextDescent, this.mTextPaint);
            }
            if (this.mHaveCamera) {
                canvas.drawText(this.mCameraOnLabel, textStartX, (height - this.mCameraOnOffset) - this.mTextDescent, this.mTextPaint);
            }
            if (this.mHaveWifi) {
                canvas.drawText(this.mWifiRunningLabel, textStartX, (height - this.mWifiRunningOffset) - this.mTextDescent, this.mTextPaint);
            }
            canvas.drawText(this.mCpuRunningLabel, textStartX, (height - this.mCpuRunningOffset) - this.mTextDescent, this.mTextPaint);
            canvas.drawText(this.mChargingLabel, textStartX, (height - this.mChargingOffset) - this.mTextDescent, this.mTextPaint);
            canvas.drawText(this.mScreenOnLabel, textStartX, (height - this.mScreenOnOffset) - this.mTextDescent, this.mTextPaint);
            this.mTextPaint.setTextAlign(align);
        }
        canvas.drawLine(this.mLevelLeft - this.mThinLineWidth, this.mLevelTop, this.mLevelLeft - this.mThinLineWidth, this.mLevelBottom + (this.mThinLineWidth / 2), this.mTextPaint);
        if (this.mLargeMode) {
            for (int i2 = 0; i2 < 10; i2++) {
                int y3 = this.mLevelTop + (this.mThinLineWidth / 2) + (((this.mLevelBottom - this.mLevelTop) * i2) / 10);
                canvas.drawLine((this.mLevelLeft - (this.mThinLineWidth * 2)) - (this.mThinLineWidth / 2), y3, (this.mLevelLeft - this.mThinLineWidth) - (this.mThinLineWidth / 2), y3, this.mTextPaint);
            }
        }
        canvas.drawText(this.mMaxPercentLabelString, 0.0f, this.mLevelTop, this.mTextPaint);
        canvas.drawText(this.mMinPercentLabelString, this.mMaxPercentLabelStringWidth - this.mMinPercentLabelStringWidth, this.mLevelBottom - this.mThinLineWidth, this.mTextPaint);
        canvas.drawLine(this.mLevelLeft / 2, this.mLevelBottom + this.mThinLineWidth, width, this.mLevelBottom + this.mThinLineWidth, this.mTextPaint);
        if (this.mDateLabels.size() <= 0) {
            return;
        }
        int ytop = this.mLevelTop + this.mTextAscent;
        int ybottom = this.mLevelBottom;
        int lastLeft = this.mLevelRight;
        this.mTextPaint.setTextAlign(Paint.Align.LEFT);
        for (int i3 = this.mDateLabels.size() - 1; i3 >= 0; i3--) {
            DateLabel label2 = this.mDateLabels.get(i3);
            int left = label2.x - this.mThinLineWidth;
            int x4 = label2.x + (this.mThinLineWidth * 2);
            if (label2.width + x4 >= lastLeft) {
                x4 = (label2.x - (this.mThinLineWidth * 2)) - label2.width;
                left = x4 - this.mThinLineWidth;
                if (left < lastLeft) {
                    if (left >= this.mLevelLeft) {
                        this.mDateLinePath.reset();
                        this.mDateLinePath.moveTo(label2.x, ytop);
                        this.mDateLinePath.lineTo(label2.x, ybottom);
                        canvas.drawPath(this.mDateLinePath, this.mDateLinePaint);
                        canvas.drawText(label2.label, x4, ytop - this.mTextAscent, this.mTextPaint);
                    }
                }
            }
        }
    }
}
