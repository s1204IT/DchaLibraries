package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
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
import com.android.settingslib.wifi.AccessPoint;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import libcore.icu.LocaleData;

/* loaded from: classes.dex */
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

        void setColors(int[] iArr) {
            this.mColors = iArr;
            this.mPaints = new Paint[iArr.length];
            for (int i = 0; i < iArr.length; i++) {
                this.mPaints[i] = new Paint();
                this.mPaints[i].setColor(iArr[i]);
                this.mPaints[i].setStyle(Paint.Style.FILL);
            }
        }

        void init(int i) {
            if (i > 0) {
                this.mTicks = new int[i * 2];
            } else {
                this.mTicks = null;
            }
            this.mNumTicks = 0;
            this.mLastBin = 0;
        }

        void addTick(int i, int i2) {
            if (i2 != this.mLastBin && this.mNumTicks < this.mTicks.length) {
                this.mTicks[this.mNumTicks] = (i & 65535) | (i2 << 16);
                this.mNumTicks++;
                this.mLastBin = i2;
            }
        }

        void finish(int i) {
            if (this.mLastBin != 0) {
                addTick(i, 0);
            }
        }

        void draw(Canvas canvas, int i, int i2) {
            int i3 = i2 + i;
            int i4 = 0;
            int i5 = 0;
            int i6 = 0;
            while (i4 < this.mNumTicks) {
                int i7 = this.mTicks[i4];
                int i8 = 65535 & i7;
                int i9 = (i7 & (-65536)) >> 16;
                if (i5 != 0) {
                    canvas.drawRect(i6, i, i8, i3, this.mPaints[i5]);
                }
                i4++;
                i5 = i9;
                i6 = i8;
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

        void retrieve(Context context, TypedArray typedArray, int i) throws Resources.NotFoundException {
            TypedArray typedArrayObtainStyledAttributes;
            int resourceId = typedArray.getResourceId(i, -1);
            if (resourceId != -1) {
                typedArrayObtainStyledAttributes = context.obtainStyledAttributes(resourceId, R.styleable.TextAppearance);
            } else {
                typedArrayObtainStyledAttributes = null;
            }
            if (typedArrayObtainStyledAttributes != null) {
                int indexCount = typedArrayObtainStyledAttributes.getIndexCount();
                for (int i2 = 0; i2 < indexCount; i2++) {
                    int index = typedArrayObtainStyledAttributes.getIndex(i2);
                    switch (index) {
                        case 0:
                            this.textSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, this.textSize);
                            break;
                        case 1:
                            this.typefaceIndex = typedArrayObtainStyledAttributes.getInt(index, -1);
                            break;
                        case 2:
                            this.styleIndex = typedArrayObtainStyledAttributes.getInt(index, -1);
                            break;
                        case 3:
                            this.textColor = typedArrayObtainStyledAttributes.getColorStateList(index);
                            break;
                    }
                }
                typedArrayObtainStyledAttributes.recycle();
            }
        }

        void apply(Context context, TextPaint textPaint) {
            Typeface typeface;
            textPaint.density = context.getResources().getDisplayMetrics().density;
            textPaint.setCompatibilityScaling(context.getResources().getCompatibilityInfo().applicationScale);
            textPaint.setColor(this.textColor.getDefaultColor());
            textPaint.setTextSize(this.textSize);
            switch (this.typefaceIndex) {
                case 1:
                    typeface = Typeface.SANS_SERIF;
                    break;
                case 2:
                    typeface = Typeface.SERIF;
                    break;
                case 3:
                    typeface = Typeface.MONOSPACE;
                    break;
                default:
                    typeface = null;
                    break;
            }
            setTypeface(textPaint, typeface, this.styleIndex);
        }

        public void setTypeface(TextPaint textPaint, Typeface typeface, int i) {
            Typeface typefaceCreate;
            if (i > 0) {
                if (typeface == null) {
                    typefaceCreate = Typeface.defaultFromStyle(i);
                } else {
                    typefaceCreate = Typeface.create(typeface, i);
                }
                textPaint.setTypeface(typefaceCreate);
                int i2 = (~(typefaceCreate != null ? typefaceCreate.getStyle() : 0)) & i;
                textPaint.setFakeBoldText((i2 & 1) != 0);
                textPaint.setTextSkewX((i2 & 2) != 0 ? -0.25f : 0.0f);
                return;
            }
            textPaint.setFakeBoldText(false);
            textPaint.setTextSkewX(0.0f);
            textPaint.setTypeface(typeface);
        }
    }

    static class TimeLabel {
        final String label;
        final int width;
        final int x;

        TimeLabel(TextPaint textPaint, int i, Calendar calendar, boolean z) {
            this.x = i;
            this.label = DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), z ? "km" : "ha"), calendar).toString();
            this.width = (int) textPaint.measureText(this.label);
        }
    }

    static class DateLabel {
        final String label;
        final int width;
        final int x;

        DateLabel(TextPaint textPaint, int i, Calendar calendar, boolean z) {
            this.x = i;
            this.label = DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), z ? "dM" : "Md"), calendar).toString();
            this.width = (int) textPaint.measureText(this.label);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [408=6] */
    public BatteryHistoryChart(Context context, AttributeSet attributeSet) throws Resources.NotFoundException {
        super(context, attributeSet);
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
        this.mBatteryWarnLevel = this.mContext.getResources().getInteger(android.R.integer.config_defaultNotificationLedOff);
        this.mBatteryCriticalLevel = this.mContext.getResources().getInteger(android.R.integer.config_batterySaver_full_locationMode);
        this.mThinLineWidth = (int) TypedValue.applyDimension(1, 2.0f, getResources().getDisplayMetrics());
        int colorAccent = Utils.getColorAccent(this.mContext);
        this.mBatteryBackgroundPaint.setColor(colorAccent);
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
        this.mScreenOnPaint.setColor(colorAccent);
        this.mGpsOnPaint.setColor(colorAccent);
        this.mCameraOnPaint.setColor(colorAccent);
        this.mFlashlightOnPaint.setColor(colorAccent);
        this.mWifiRunningPaint.setColor(colorAccent);
        this.mCpuRunningPaint.setColor(colorAccent);
        this.mChargingPaint.setColor(colorAccent);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, com.android.settings.R.styleable.BatteryHistoryChart, 0, 0);
        TextAttrs textAttrs = new TextAttrs();
        TextAttrs textAttrs2 = new TextAttrs();
        textAttrs.retrieve(context, typedArrayObtainStyledAttributes, 0);
        textAttrs2.retrieve(context, typedArrayObtainStyledAttributes, 12);
        int indexCount = typedArrayObtainStyledAttributes.getIndexCount();
        int i = 0;
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 0.0f;
        for (int i2 = 0; i2 < indexCount; i2++) {
            int index = typedArrayObtainStyledAttributes.getIndex(i2);
            switch (index) {
                case 1:
                    textAttrs.textSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, textAttrs.textSize);
                    textAttrs2.textSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, textAttrs2.textSize);
                    break;
                case 2:
                    textAttrs.typefaceIndex = typedArrayObtainStyledAttributes.getInt(index, textAttrs.typefaceIndex);
                    textAttrs2.typefaceIndex = typedArrayObtainStyledAttributes.getInt(index, textAttrs2.typefaceIndex);
                    break;
                case 3:
                    textAttrs.styleIndex = typedArrayObtainStyledAttributes.getInt(index, textAttrs.styleIndex);
                    textAttrs2.styleIndex = typedArrayObtainStyledAttributes.getInt(index, textAttrs2.styleIndex);
                    break;
                case 4:
                    textAttrs.textColor = typedArrayObtainStyledAttributes.getColorStateList(index);
                    textAttrs2.textColor = typedArrayObtainStyledAttributes.getColorStateList(index);
                    break;
                case 5:
                    i = typedArrayObtainStyledAttributes.getInt(index, 0);
                    break;
                case 6:
                    f2 = typedArrayObtainStyledAttributes.getFloat(index, 0.0f);
                    break;
                case 7:
                    f3 = typedArrayObtainStyledAttributes.getFloat(index, 0.0f);
                    break;
                case 8:
                    f = typedArrayObtainStyledAttributes.getFloat(index, 0.0f);
                    break;
                case 9:
                    this.mTimeRemainPaint.setColor(typedArrayObtainStyledAttributes.getInt(index, 0));
                    break;
                case AccessPoint.Speed.MODERATE /* 10 */:
                    this.mBatteryBackgroundPaint.setColor(typedArrayObtainStyledAttributes.getInt(index, 0));
                    this.mScreenOnPaint.setColor(typedArrayObtainStyledAttributes.getInt(index, 0));
                    this.mGpsOnPaint.setColor(typedArrayObtainStyledAttributes.getInt(index, 0));
                    this.mCameraOnPaint.setColor(typedArrayObtainStyledAttributes.getInt(index, 0));
                    this.mFlashlightOnPaint.setColor(typedArrayObtainStyledAttributes.getInt(index, 0));
                    this.mWifiRunningPaint.setColor(typedArrayObtainStyledAttributes.getInt(index, 0));
                    this.mCpuRunningPaint.setColor(typedArrayObtainStyledAttributes.getInt(index, 0));
                    this.mChargingPaint.setColor(typedArrayObtainStyledAttributes.getInt(index, 0));
                    break;
                case 11:
                    this.mChartMinHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 0);
                    break;
            }
        }
        typedArrayObtainStyledAttributes.recycle();
        textAttrs.apply(context, this.mTextPaint);
        textAttrs2.apply(context, this.mHeaderTextPaint);
        this.mDateLinePaint.set(this.mTextPaint);
        this.mDateLinePaint.setStyle(Paint.Style.STROKE);
        this.mDateLinePaint.setStrokeWidth(this.mThinLineWidth / 2 < 1 ? 1 : r3);
        this.mDateLinePaint.setPathEffect(new DashPathEffect(new float[]{this.mThinLineWidth * 2, this.mThinLineWidth * 2}, 0.0f));
        if (i != 0) {
            this.mTextPaint.setShadowLayer(f, f2, f3, i);
            this.mHeaderTextPaint.setShadowLayer(f, f2, f3, i);
        }
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        this.mMaxPercentLabelStringWidth = (int) this.mTextPaint.measureText(this.mMaxPercentLabelString);
        this.mMinPercentLabelStringWidth = (int) this.mTextPaint.measureText(this.mMinPercentLabelString);
        this.mDrainStringWidth = (int) this.mHeaderTextPaint.measureText(this.mDrainString);
        this.mChargeLabelStringWidth = (int) this.mHeaderTextPaint.measureText(this.mInfo.chargeLabel.toString());
        this.mChargeDurationStringWidth = (int) this.mHeaderTextPaint.measureText(this.mChargeDurationString);
        this.mTextAscent = (int) this.mTextPaint.ascent();
        this.mTextDescent = (int) this.mTextPaint.descent();
        this.mHeaderTextAscent = (int) this.mHeaderTextPaint.ascent();
        this.mHeaderTextDescent = (int) this.mHeaderTextPaint.descent();
        this.mHeaderHeight = ((this.mHeaderTextDescent - this.mHeaderTextAscent) * 2) - this.mTextAscent;
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), i), getDefaultSize(this.mChartMinHeight + this.mHeaderHeight, i2));
    }

    void finishPaths(int i, int i2, int i3, int i4, int i5, Path path, int i6, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7, Path path2) {
        if (path != null) {
            if (i6 >= 0 && i6 < i) {
                if (path2 != null) {
                    path2.lineTo(i, i5);
                }
                path.lineTo(i, i5);
            }
            path.lineTo(i, this.mLevelTop + i3);
            path.lineTo(i4, this.mLevelTop + i3);
            path.close();
        }
        if (z) {
            this.mChargingPath.lineTo(i, i2 - this.mChargingOffset);
        }
        if (z2) {
            this.mScreenOnPath.lineTo(i, i2 - this.mScreenOnOffset);
        }
        if (z3) {
            this.mGpsOnPath.lineTo(i, i2 - this.mGpsOnOffset);
        }
        if (z4) {
            this.mFlashlightOnPath.lineTo(i, i2 - this.mFlashlightOnOffset);
        }
        if (z5) {
            this.mCameraOnPath.lineTo(i, i2 - this.mCameraOnOffset);
        }
        if (z6) {
            this.mWifiRunningPath.lineTo(i, i2 - this.mWifiRunningOffset);
        }
        if (z7) {
            this.mCpuRunningPath.lineTo(i, i2 - this.mCpuRunningOffset);
        }
        if (this.mHavePhoneSignal) {
            this.mPhoneSignalChart.finish(i);
        }
    }

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(getContext());
    }

    private boolean isDayFirst() {
        String dateFormat = LocaleData.get(getResources().getConfiguration().locale).getDateFormat(3);
        return dateFormat.indexOf(77) > dateFormat.indexOf(100);
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        int i5;
        long j;
        int i6;
        int i7;
        int i8;
        Path path;
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        Path path2;
        boolean z7;
        Path path3;
        int i9;
        int i10;
        Path path4;
        int i11;
        BatteryHistoryChart batteryHistoryChart;
        int i12;
        boolean z8;
        boolean z9;
        boolean z10;
        boolean z11;
        boolean z12;
        boolean z13;
        boolean z14;
        int i13;
        int i14;
        long j2;
        long j3;
        long j4;
        int i15;
        int i16;
        int i17;
        BatteryStats.HistoryItem historyItem;
        long j5;
        int i18;
        int i19;
        boolean z15;
        boolean z16;
        boolean z17;
        int i20;
        int i21;
        boolean z18;
        boolean z19;
        boolean z20;
        Path path5;
        BatteryHistoryChart batteryHistoryChart2 = this;
        int i22 = i2;
        super.onSizeChanged(i, i2, i3, i4);
        if ((batteryHistoryChart2.mLastWidth == i && batteryHistoryChart2.mLastHeight == i22) || batteryHistoryChart2.mLastWidth == 0 || batteryHistoryChart2.mLastHeight == 0) {
            return;
        }
        batteryHistoryChart2.mLastWidth = i;
        batteryHistoryChart2.mLastHeight = i22;
        batteryHistoryChart2.mBitmap = null;
        batteryHistoryChart2.mCanvas = null;
        int i23 = batteryHistoryChart2.mTextDescent - batteryHistoryChart2.mTextAscent;
        if (i22 > (i23 * 10) + batteryHistoryChart2.mChartMinHeight) {
            batteryHistoryChart2.mLargeMode = true;
            if (i22 > i23 * 15) {
                batteryHistoryChart2.mLineWidth = i23 / 2;
            } else {
                batteryHistoryChart2.mLineWidth = i23 / 3;
            }
        } else {
            batteryHistoryChart2.mLargeMode = false;
            batteryHistoryChart2.mLineWidth = batteryHistoryChart2.mThinLineWidth;
        }
        if (batteryHistoryChart2.mLineWidth <= 0) {
            batteryHistoryChart2.mLineWidth = 1;
        }
        batteryHistoryChart2.mLevelTop = batteryHistoryChart2.mHeaderHeight;
        batteryHistoryChart2.mLevelLeft = batteryHistoryChart2.mMaxPercentLabelStringWidth + (batteryHistoryChart2.mThinLineWidth * 3);
        batteryHistoryChart2.mLevelRight = i;
        int i24 = batteryHistoryChart2.mLevelRight - batteryHistoryChart2.mLevelLeft;
        batteryHistoryChart2.mTextPaint.setStrokeWidth(batteryHistoryChart2.mThinLineWidth);
        batteryHistoryChart2.mBatteryGoodPaint.setStrokeWidth(batteryHistoryChart2.mThinLineWidth);
        batteryHistoryChart2.mBatteryWarnPaint.setStrokeWidth(batteryHistoryChart2.mThinLineWidth);
        batteryHistoryChart2.mBatteryCriticalPaint.setStrokeWidth(batteryHistoryChart2.mThinLineWidth);
        batteryHistoryChart2.mChargingPaint.setStrokeWidth(batteryHistoryChart2.mLineWidth);
        batteryHistoryChart2.mScreenOnPaint.setStrokeWidth(batteryHistoryChart2.mLineWidth);
        batteryHistoryChart2.mGpsOnPaint.setStrokeWidth(batteryHistoryChart2.mLineWidth);
        batteryHistoryChart2.mCameraOnPaint.setStrokeWidth(batteryHistoryChart2.mLineWidth);
        batteryHistoryChart2.mFlashlightOnPaint.setStrokeWidth(batteryHistoryChart2.mLineWidth);
        batteryHistoryChart2.mWifiRunningPaint.setStrokeWidth(batteryHistoryChart2.mLineWidth);
        batteryHistoryChart2.mCpuRunningPaint.setStrokeWidth(batteryHistoryChart2.mLineWidth);
        batteryHistoryChart2.mDebugRectPaint.setStrokeWidth(1.0f);
        int i25 = i23 + batteryHistoryChart2.mLineWidth;
        if (batteryHistoryChart2.mLargeMode) {
            batteryHistoryChart2.mChargingOffset = batteryHistoryChart2.mLineWidth;
            batteryHistoryChart2.mScreenOnOffset = batteryHistoryChart2.mChargingOffset + i25;
            batteryHistoryChart2.mCpuRunningOffset = batteryHistoryChart2.mScreenOnOffset + i25;
            batteryHistoryChart2.mWifiRunningOffset = batteryHistoryChart2.mCpuRunningOffset + i25;
            batteryHistoryChart2.mGpsOnOffset = batteryHistoryChart2.mWifiRunningOffset + (batteryHistoryChart2.mHaveWifi ? i25 : 0);
            batteryHistoryChart2.mFlashlightOnOffset = batteryHistoryChart2.mGpsOnOffset + (batteryHistoryChart2.mHaveGps ? i25 : 0);
            batteryHistoryChart2.mCameraOnOffset = batteryHistoryChart2.mFlashlightOnOffset + (batteryHistoryChart2.mHaveFlashlight ? i25 : 0);
            batteryHistoryChart2.mPhoneSignalOffset = batteryHistoryChart2.mCameraOnOffset + (batteryHistoryChart2.mHaveCamera ? i25 : 0);
            int i26 = batteryHistoryChart2.mPhoneSignalOffset;
            if (!batteryHistoryChart2.mHavePhoneSignal) {
                i25 = 0;
            }
            batteryHistoryChart2.mLevelOffset = i26 + i25 + (batteryHistoryChart2.mLineWidth * 2) + (batteryHistoryChart2.mLineWidth / 2);
            if (batteryHistoryChart2.mHavePhoneSignal) {
                batteryHistoryChart2.mPhoneSignalChart.init(i);
            }
        } else {
            batteryHistoryChart2.mPhoneSignalOffset = 0;
            batteryHistoryChart2.mChargingOffset = 0;
            batteryHistoryChart2.mCpuRunningOffset = 0;
            batteryHistoryChart2.mWifiRunningOffset = 0;
            batteryHistoryChart2.mFlashlightOnOffset = 0;
            batteryHistoryChart2.mCameraOnOffset = 0;
            batteryHistoryChart2.mGpsOnOffset = 0;
            batteryHistoryChart2.mScreenOnOffset = 0;
            batteryHistoryChart2.mLevelOffset = i25 + (batteryHistoryChart2.mThinLineWidth * 4);
            if (batteryHistoryChart2.mHavePhoneSignal) {
                batteryHistoryChart2.mPhoneSignalChart.init(0);
            }
        }
        batteryHistoryChart2.mBatLevelPath.reset();
        batteryHistoryChart2.mBatGoodPath.reset();
        batteryHistoryChart2.mBatWarnPath.reset();
        batteryHistoryChart2.mTimeRemainPath.reset();
        batteryHistoryChart2.mBatCriticalPath.reset();
        batteryHistoryChart2.mScreenOnPath.reset();
        batteryHistoryChart2.mGpsOnPath.reset();
        batteryHistoryChart2.mFlashlightOnPath.reset();
        batteryHistoryChart2.mCameraOnPath.reset();
        batteryHistoryChart2.mWifiRunningPath.reset();
        batteryHistoryChart2.mCpuRunningPath.reset();
        batteryHistoryChart2.mChargingPath.reset();
        batteryHistoryChart2.mTimeLabels.clear();
        batteryHistoryChart2.mDateLabels.clear();
        long j6 = batteryHistoryChart2.mStartWallTime;
        long j7 = batteryHistoryChart2.mEndWallTime > j6 ? batteryHistoryChart2.mEndWallTime - j6 : 1L;
        long j8 = batteryHistoryChart2.mStartWallTime;
        int i27 = batteryHistoryChart2.mBatLow;
        int i28 = batteryHistoryChart2.mBatHigh - batteryHistoryChart2.mBatLow;
        int i29 = (i22 - batteryHistoryChart2.mLevelOffset) - batteryHistoryChart2.mLevelTop;
        batteryHistoryChart2.mLevelBottom = batteryHistoryChart2.mLevelTop + i29;
        int i30 = batteryHistoryChart2.mLevelLeft;
        int i31 = batteryHistoryChart2.mLevelLeft;
        int i32 = batteryHistoryChart2.mNumHist;
        int i33 = -1;
        if (batteryHistoryChart2.mEndDataWallTime <= batteryHistoryChart2.mStartWallTime || !batteryHistoryChart2.mStats.startIteratingHistoryLocked()) {
            i5 = i27;
            j = j6;
            i6 = i24;
            i7 = i31;
            i8 = -1;
            path = null;
            z = false;
            z2 = false;
            z3 = false;
            z4 = false;
            z5 = false;
            z6 = false;
            path2 = null;
            z7 = false;
        } else {
            BatteryStats.HistoryItem historyItem2 = new BatteryStats.HistoryItem();
            long j9 = j8;
            int i34 = i30;
            int i35 = i31;
            long j10 = 0;
            int i36 = -1;
            i8 = -1;
            boolean z21 = false;
            boolean z22 = false;
            boolean z23 = false;
            int i37 = 0;
            Path path6 = null;
            Path path7 = null;
            boolean z24 = false;
            boolean z25 = false;
            boolean z26 = false;
            boolean z27 = false;
            boolean z28 = false;
            int i38 = 0;
            while (true) {
                z8 = z21;
                if (batteryHistoryChart2.mStats.getNextHistoryLocked(historyItem2) && i37 < i32) {
                    if (historyItem2.isDeltaData()) {
                        boolean z29 = z22;
                        j9 += historyItem2.time - j10;
                        long j11 = historyItem2.time;
                        long j12 = j6;
                        int i39 = batteryHistoryChart2.mLevelLeft + ((int) (((j9 - j6) * i24) / j7));
                        if (i39 < 0) {
                            i39 = 0;
                        }
                        int i40 = (batteryHistoryChart2.mLevelTop + i29) - (((historyItem2.batteryLevel - i27) * (i29 - 1)) / i28);
                        if (i8 == i39 || i36 == i40) {
                            i40 = i36;
                        } else {
                            byte b = historyItem2.batteryLevel;
                            if (b <= batteryHistoryChart2.mBatteryCriticalLevel) {
                                path5 = batteryHistoryChart2.mBatCriticalPath;
                            } else {
                                path5 = b <= batteryHistoryChart2.mBatteryWarnLevel ? batteryHistoryChart2.mBatWarnPath : null;
                            }
                            if (path5 != path6) {
                                if (path6 != null) {
                                    path6.lineTo(i39, i40);
                                }
                                if (path5 != null) {
                                    path5.moveTo(i39, i40);
                                }
                                path6 = path5;
                            } else if (path5 != null) {
                                path5.lineTo(i39, i40);
                            }
                            if (path7 == null) {
                                path7 = batteryHistoryChart2.mBatLevelPath;
                                path7.moveTo(i39, i40);
                                i35 = i39;
                            } else {
                                path7.lineTo(i39, i40);
                            }
                            i8 = i39;
                        }
                        Path path8 = path6;
                        Path path9 = path7;
                        if (!batteryHistoryChart2.mLargeMode) {
                            int i41 = i40;
                            boolean z30 = z26;
                            z9 = z24;
                            z10 = z27;
                            z11 = z25;
                            z12 = z30;
                            i34 = i39;
                            z22 = z29;
                            i15 = i32;
                            i16 = i24;
                            i17 = i37;
                            historyItem = historyItem2;
                            z21 = z8;
                            j10 = j11;
                            j5 = j12;
                            i18 = i27;
                            path7 = path9;
                            path6 = path8;
                            i36 = i41;
                            boolean z31 = z11;
                            z27 = z10;
                            z24 = z9;
                            z26 = z12;
                            z25 = z31;
                        } else {
                            boolean z32 = (historyItem2.states & 524288) != 0;
                            if (z32 != z23) {
                                if (z32) {
                                    batteryHistoryChart2.mChargingPath.moveTo(i39, i22 - batteryHistoryChart2.mChargingOffset);
                                } else {
                                    batteryHistoryChart2.mChargingPath.lineTo(i39, i22 - batteryHistoryChart2.mChargingOffset);
                                }
                                z23 = z32;
                            }
                            boolean z33 = (historyItem2.states & 1048576) != 0;
                            if (z33 != z29) {
                                if (z33) {
                                    i19 = i40;
                                    batteryHistoryChart2.mScreenOnPath.moveTo(i39, i22 - batteryHistoryChart2.mScreenOnOffset);
                                } else {
                                    i19 = i40;
                                    batteryHistoryChart2.mScreenOnPath.lineTo(i39, i22 - batteryHistoryChart2.mScreenOnOffset);
                                }
                            } else {
                                i19 = i40;
                                z33 = z29;
                            }
                            boolean z34 = (historyItem2.states & 536870912) != 0;
                            boolean z35 = z8;
                            if (z34 != z35) {
                                if (z34) {
                                    z20 = z34;
                                    batteryHistoryChart2.mGpsOnPath.moveTo(i39, i22 - batteryHistoryChart2.mGpsOnOffset);
                                } else {
                                    z20 = z34;
                                    batteryHistoryChart2.mGpsOnPath.lineTo(i39, i22 - batteryHistoryChart2.mGpsOnOffset);
                                }
                                z35 = z20;
                            }
                            boolean z36 = (historyItem2.states2 & 134217728) != 0;
                            boolean z37 = z27;
                            if (z36 != z37) {
                                if (z36) {
                                    z16 = z36;
                                    z15 = z23;
                                    batteryHistoryChart2.mFlashlightOnPath.moveTo(i39, i22 - batteryHistoryChart2.mFlashlightOnOffset);
                                } else {
                                    z16 = z36;
                                    z15 = z23;
                                    batteryHistoryChart2.mFlashlightOnPath.lineTo(i39, i22 - batteryHistoryChart2.mFlashlightOnOffset);
                                }
                            } else {
                                z15 = z23;
                                z16 = z37;
                            }
                            boolean z38 = (historyItem2.states2 & 2097152) != 0;
                            boolean z39 = z26;
                            if (z38 != z39) {
                                if (z38) {
                                    z17 = z38;
                                    batteryHistoryChart2.mCameraOnPath.moveTo(i39, i22 - batteryHistoryChart2.mCameraOnOffset);
                                } else {
                                    z17 = z38;
                                    batteryHistoryChart2.mCameraOnPath.lineTo(i39, i22 - batteryHistoryChart2.mCameraOnOffset);
                                }
                            } else {
                                z17 = z39;
                            }
                            int i42 = (historyItem2.states2 & 15) >> 0;
                            int i43 = i38;
                            if (i43 != i42) {
                                switch (i42) {
                                    default:
                                        switch (i42) {
                                            case 11:
                                            case 12:
                                                break;
                                            default:
                                                z28 = true;
                                                break;
                                        }
                                    case 0:
                                    case 1:
                                    case 2:
                                    case 3:
                                        z28 = false;
                                        break;
                                }
                            } else {
                                i42 = i43;
                            }
                            boolean z40 = (historyItem2.states & 402718720) != 0 ? true : z28;
                            boolean z41 = z24;
                            if (z40 != z41) {
                                if (z40) {
                                    i20 = i42;
                                    z19 = z40;
                                    batteryHistoryChart2.mWifiRunningPath.moveTo(i39, i22 - batteryHistoryChart2.mWifiRunningOffset);
                                } else {
                                    i20 = i42;
                                    z19 = z40;
                                    batteryHistoryChart2.mWifiRunningPath.lineTo(i39, i22 - batteryHistoryChart2.mWifiRunningOffset);
                                }
                                z24 = z19;
                            } else {
                                i20 = i42;
                                z24 = z41;
                            }
                            boolean z42 = (historyItem2.states & AccessPoint.UNREACHABLE_RSSI) != 0;
                            boolean z43 = z25;
                            if (z42 != z43) {
                                if (z42) {
                                    z18 = z42;
                                    batteryHistoryChart2.mCpuRunningPath.moveTo(i39, i22 - batteryHistoryChart2.mCpuRunningOffset);
                                } else {
                                    z18 = z42;
                                    batteryHistoryChart2.mCpuRunningPath.lineTo(i39, i22 - batteryHistoryChart2.mCpuRunningOffset);
                                }
                                z43 = z18;
                            }
                            if (batteryHistoryChart2.mLargeMode && batteryHistoryChart2.mHavePhoneSignal) {
                                if (((historyItem2.states & 448) >> 6) != 3) {
                                    if ((historyItem2.states & 2097152) == 0) {
                                        i21 = ((historyItem2.states & 56) >> 3) + 2;
                                        batteryHistoryChart2.mPhoneSignalChart.addTick(i39, i21);
                                    } else {
                                        i21 = 1;
                                    }
                                } else {
                                    i21 = 0;
                                }
                                batteryHistoryChart2.mPhoneSignalChart.addTick(i39, i21);
                            }
                            i34 = i39;
                            z25 = z43;
                            z22 = z33;
                            i15 = i32;
                            i16 = i24;
                            i17 = i37;
                            historyItem = historyItem2;
                            z21 = z35;
                            j10 = j11;
                            j5 = j12;
                            z27 = z16;
                            z23 = z15;
                            z26 = z17;
                            i38 = i20;
                            i18 = i27;
                            path7 = path9;
                            path6 = path8;
                            i36 = i19;
                        }
                    } else {
                        long j13 = j6;
                        boolean z44 = z22;
                        boolean z45 = z26;
                        z9 = z24;
                        z10 = z27;
                        z11 = z25;
                        z12 = z45;
                        if (historyItem2.cmd == 5 || historyItem2.cmd == 7) {
                            z13 = z8;
                            z14 = z23;
                            i13 = i36;
                            if (historyItem2.currentTime >= batteryHistoryChart2.mStartWallTime) {
                                j2 = historyItem2.currentTime;
                                i14 = i27;
                            } else {
                                i14 = i27;
                                j2 = batteryHistoryChart2.mStartWallTime + (historyItem2.time - batteryHistoryChart2.mHistStart);
                            }
                            j3 = j2;
                            j4 = historyItem2.time;
                        } else {
                            z13 = z8;
                            z14 = z23;
                            i13 = i36;
                            i14 = i27;
                            j4 = j10;
                            j3 = j9;
                        }
                        if (historyItem2.cmd == 6 || ((historyItem2.cmd == 5 && Math.abs(j9 - j3) <= 3600000) || path7 == null)) {
                            i15 = i32;
                            i16 = i24;
                            i17 = i37;
                            historyItem = historyItem2;
                            j5 = j13;
                            i18 = i14;
                            i36 = i13;
                            path6 = path6;
                            z21 = z13;
                            z22 = z44;
                            z23 = z14;
                            j9 = j3;
                            j10 = j4;
                            boolean z312 = z11;
                            z27 = z10;
                            z24 = z9;
                            z26 = z12;
                            z25 = z312;
                        } else {
                            i18 = i14;
                            j5 = j13;
                            i15 = i32;
                            i16 = i24;
                            i17 = i37;
                            historyItem = historyItem2;
                            batteryHistoryChart2.finishPaths(i34 + 1, i22, i29, i35, i13, path7, i8, z14, z44, z13, z10, z12, z9, z11, path6);
                            i36 = -1;
                            i8 = -1;
                            z24 = z9;
                            j9 = j3;
                            j10 = j4;
                            z21 = false;
                            z22 = false;
                            z23 = false;
                            path6 = null;
                            path7 = null;
                            z25 = false;
                            z26 = false;
                            z27 = false;
                        }
                    }
                    i37 = i17 + 1;
                    i22 = i2;
                    i32 = i15;
                    historyItem2 = historyItem;
                    i27 = i18;
                    j6 = j5;
                    i24 = i16;
                    batteryHistoryChart2 = this;
                }
            }
            z7 = z22;
            i5 = i27;
            j = j6;
            i6 = i24;
            path2 = path6;
            batteryHistoryChart2 = this;
            batteryHistoryChart2.mStats.finishIteratingHistoryLocked();
            i33 = i36;
            path = path7;
            i7 = i35;
            z2 = z8;
            z3 = z27;
            z4 = z26;
            z5 = z24;
            z6 = z25;
            z = z23;
        }
        if (i33 < 0 || i8 < 0) {
            int i44 = batteryHistoryChart2.mLevelLeft;
            int i45 = (batteryHistoryChart2.mLevelTop + i29) - (((batteryHistoryChart2.mInfo.batteryLevel - i5) * (i29 - 1)) / i28);
            byte b2 = (byte) batteryHistoryChart2.mInfo.batteryLevel;
            if (b2 <= batteryHistoryChart2.mBatteryCriticalLevel) {
                path3 = batteryHistoryChart2.mBatCriticalPath;
            } else {
                path3 = b2 <= batteryHistoryChart2.mBatteryWarnLevel ? batteryHistoryChart2.mBatWarnPath : null;
            }
            if (path3 != null) {
                path3.moveTo(i44, i45);
            } else {
                path3 = path2;
            }
            batteryHistoryChart2.mBatLevelPath.moveTo(i44, i45);
            i9 = i44;
            i10 = i45;
            path2 = path3;
            path4 = batteryHistoryChart2.mBatLevelPath;
            i11 = i;
        } else {
            int i46 = batteryHistoryChart2.mLevelLeft + ((int) (((batteryHistoryChart2.mEndDataWallTime - j) * i6) / j7));
            if (i46 < 0) {
                path4 = path;
                i9 = i8;
                i10 = i33;
                i11 = 0;
            } else {
                path4 = path;
                i9 = i8;
                i10 = i33;
                i11 = i46;
            }
        }
        int i47 = i10;
        int i48 = i11;
        batteryHistoryChart2.finishPaths(i11, i2, i29, i7, i10, path4, i9, z, z7, z2, z3, z4, z5, z6, path2);
        if (i48 < i) {
            batteryHistoryChart = this;
            float f = i48;
            batteryHistoryChart.mTimeRemainPath.moveTo(f, i47);
            int i49 = i29 - 1;
            int i50 = (batteryHistoryChart.mLevelTop + i29) - (((100 - i5) * i49) / i28);
            i12 = 0;
            int i51 = (batteryHistoryChart.mLevelTop + i29) - (((0 - i5) * i49) / i28);
            if (batteryHistoryChart.mInfo.discharging) {
                batteryHistoryChart.mTimeRemainPath.lineTo(batteryHistoryChart.mLevelRight, i51);
            } else {
                batteryHistoryChart.mTimeRemainPath.lineTo(batteryHistoryChart.mLevelRight, i50);
                batteryHistoryChart.mTimeRemainPath.lineTo(batteryHistoryChart.mLevelRight, i51);
            }
            batteryHistoryChart.mTimeRemainPath.lineTo(f, i51);
            batteryHistoryChart.mTimeRemainPath.close();
        } else {
            batteryHistoryChart = this;
            i12 = 0;
        }
        if (batteryHistoryChart.mStartWallTime > 0 && batteryHistoryChart.mEndWallTime > batteryHistoryChart.mStartWallTime) {
            boolean zIs24Hour = is24Hour();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(batteryHistoryChart.mStartWallTime);
            calendar.set(14, i12);
            calendar.set(13, i12);
            calendar.set(12, i12);
            long timeInMillis = calendar.getTimeInMillis();
            if (timeInMillis < batteryHistoryChart.mStartWallTime) {
                calendar.set(11, calendar.get(11) + 1);
                timeInMillis = calendar.getTimeInMillis();
            }
            Calendar calendar2 = Calendar.getInstance();
            calendar2.setTimeInMillis(batteryHistoryChart.mEndWallTime);
            calendar2.set(14, i12);
            calendar2.set(13, i12);
            calendar2.set(12, i12);
            long timeInMillis2 = calendar2.getTimeInMillis();
            if (timeInMillis < timeInMillis2) {
                batteryHistoryChart.addTimeLabel(calendar, batteryHistoryChart.mLevelLeft, batteryHistoryChart.mLevelRight, zIs24Hour);
                Calendar calendar3 = Calendar.getInstance();
                long j14 = timeInMillis;
                calendar3.setTimeInMillis(batteryHistoryChart.mStartWallTime + ((batteryHistoryChart.mEndWallTime - batteryHistoryChart.mStartWallTime) / 2));
                calendar3.set(14, i12);
                calendar3.set(13, i12);
                calendar3.set(12, i12);
                long timeInMillis3 = calendar3.getTimeInMillis();
                if (timeInMillis3 > j14 && timeInMillis3 < timeInMillis2) {
                    batteryHistoryChart.addTimeLabel(calendar3, batteryHistoryChart.mLevelLeft, batteryHistoryChart.mLevelRight, zIs24Hour);
                }
                batteryHistoryChart.addTimeLabel(calendar2, batteryHistoryChart.mLevelLeft, batteryHistoryChart.mLevelRight, zIs24Hour);
            }
            if (calendar.get(6) != calendar2.get(6) || calendar.get(1) != calendar2.get(1)) {
                boolean zIsDayFirst = isDayFirst();
                calendar.set(11, i12);
                long timeInMillis4 = calendar.getTimeInMillis();
                if (timeInMillis4 < batteryHistoryChart.mStartWallTime) {
                    calendar.set(6, calendar.get(6) + 1);
                    timeInMillis4 = calendar.getTimeInMillis();
                }
                calendar2.set(11, i12);
                long timeInMillis5 = calendar2.getTimeInMillis();
                if (timeInMillis4 < timeInMillis5) {
                    batteryHistoryChart.addDateLabel(calendar, batteryHistoryChart.mLevelLeft, batteryHistoryChart.mLevelRight, zIsDayFirst);
                    Calendar calendar4 = Calendar.getInstance();
                    calendar4.setTimeInMillis(((timeInMillis5 - timeInMillis4) / 2) + timeInMillis4 + 7200000);
                    calendar4.set(11, i12);
                    calendar4.set(12, i12);
                    long timeInMillis6 = calendar4.getTimeInMillis();
                    if (timeInMillis6 > timeInMillis4 && timeInMillis6 < timeInMillis5) {
                        batteryHistoryChart.addDateLabel(calendar4, batteryHistoryChart.mLevelLeft, batteryHistoryChart.mLevelRight, zIsDayFirst);
                    }
                }
                batteryHistoryChart.addDateLabel(calendar2, batteryHistoryChart.mLevelLeft, batteryHistoryChart.mLevelRight, zIsDayFirst);
            }
        }
        if (batteryHistoryChart.mTimeLabels.size() < 2) {
            batteryHistoryChart.mDurationString = Formatter.formatShortElapsedTime(getContext(), batteryHistoryChart.mEndWallTime - batteryHistoryChart.mStartWallTime);
            batteryHistoryChart.mDurationStringWidth = (int) batteryHistoryChart.mTextPaint.measureText(batteryHistoryChart.mDurationString);
        } else {
            batteryHistoryChart.mDurationString = null;
            batteryHistoryChart.mDurationStringWidth = i12;
        }
    }

    void addTimeLabel(Calendar calendar, int i, int i2, boolean z) {
        long j = this.mStartWallTime;
        this.mTimeLabels.add(new TimeLabel(this.mTextPaint, i + ((int) (((calendar.getTimeInMillis() - j) * (i2 - i)) / (this.mEndWallTime - j))), calendar, z));
    }

    void addDateLabel(Calendar calendar, int i, int i2, boolean z) {
        long j = this.mStartWallTime;
        this.mDateLabels.add(new DateLabel(this.mTextPaint, i + ((int) (((calendar.getTimeInMillis() - j) * (i2 - i)) / (this.mEndWallTime - j))), calendar, z));
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawChart(canvas, getWidth(), getHeight());
    }

    void drawChart(Canvas canvas, int i, int i2) {
        int i3;
        Paint.Align align;
        int i4;
        boolean z;
        int i5;
        Paint.Align align2;
        int i6;
        int i7;
        int i8;
        boolean zIsLayoutRtl = isLayoutRtl();
        int i9 = zIsLayoutRtl ? i : 0;
        int i10 = zIsLayoutRtl ? 0 : i;
        Paint.Align align3 = zIsLayoutRtl ? Paint.Align.RIGHT : Paint.Align.LEFT;
        Paint.Align align4 = zIsLayoutRtl ? Paint.Align.LEFT : Paint.Align.RIGHT;
        canvas.drawPath(this.mBatLevelPath, this.mBatteryBackgroundPaint);
        if (!this.mTimeRemainPath.isEmpty()) {
            canvas.drawPath(this.mTimeRemainPath, this.mTimeRemainPaint);
        }
        boolean z2 = true;
        if (this.mTimeLabels.size() > 1) {
            int i11 = (this.mLevelBottom - this.mTextAscent) + (this.mThinLineWidth * 4);
            int i12 = this.mLevelBottom + this.mThinLineWidth + (this.mThinLineWidth / 2);
            this.mTextPaint.setTextAlign(Paint.Align.LEFT);
            int i13 = 0;
            int i14 = 0;
            while (i13 < this.mTimeLabels.size()) {
                TimeLabel timeLabel = this.mTimeLabels.get(i13);
                if (i13 != 0) {
                    i4 = i11;
                    z = z2;
                    i5 = i10;
                    align2 = align4;
                    i6 = i13;
                    i7 = i12;
                    if (i6 < this.mTimeLabels.size() - 1) {
                        int i15 = timeLabel.x - (timeLabel.width / 2);
                        if (i15 >= i14 + this.mTextAscent && i15 <= (i - this.mTimeLabels.get(i6 + 1).width) - this.mTextAscent) {
                            canvas.drawText(timeLabel.label, i15, i4, this.mTextPaint);
                            canvas.drawLine(timeLabel.x, i7, timeLabel.x, this.mThinLineWidth + i7, this.mTextPaint);
                            i8 = i15 + timeLabel.width;
                        }
                    } else {
                        int i16 = timeLabel.x - (timeLabel.width / 2);
                        if (timeLabel.width + i16 >= i) {
                            i16 = (i - 1) - timeLabel.width;
                        }
                        canvas.drawText(timeLabel.label, i16, i4, this.mTextPaint);
                        canvas.drawLine(timeLabel.x, i7, timeLabel.x, this.mThinLineWidth + i7, this.mTextPaint);
                    }
                    i13 = i6 + 1;
                    i11 = i4;
                    i12 = i7;
                    z2 = z;
                    i10 = i5;
                    align4 = align2;
                } else {
                    int i17 = timeLabel.x - (timeLabel.width / 2);
                    int i18 = i17 < 0 ? 0 : i17;
                    canvas.drawText(timeLabel.label, i18, i11, this.mTextPaint);
                    i5 = i10;
                    i6 = i13;
                    align2 = align4;
                    i4 = i11;
                    i7 = i12;
                    z = true;
                    canvas.drawLine(timeLabel.x, i12, timeLabel.x, this.mThinLineWidth + i12, this.mTextPaint);
                    i8 = i18 + timeLabel.width;
                }
                i14 = i8;
                i13 = i6 + 1;
                i11 = i4;
                i12 = i7;
                z2 = z;
                i10 = i5;
                align4 = align2;
            }
            i3 = i10;
            align = align4;
        } else {
            i3 = i10;
            align = align4;
            if (this.mDurationString != null) {
                int i19 = (this.mLevelBottom - this.mTextAscent) + (this.mThinLineWidth * 4);
                this.mTextPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(this.mDurationString, (this.mLevelLeft + ((this.mLevelRight - this.mLevelLeft) / 2)) - (this.mDurationStringWidth / 2), i19, this.mTextPaint);
            }
        }
        int i20 = (-this.mHeaderTextAscent) + ((this.mHeaderTextDescent - this.mHeaderTextAscent) / 3);
        this.mHeaderTextPaint.setTextAlign(align3);
        float f = i9;
        float f2 = i20;
        canvas.drawText(this.mInfo.chargeLabel.toString(), f, f2, this.mHeaderTextPaint);
        int i21 = this.mChargeDurationStringWidth / 2;
        if (zIsLayoutRtl) {
            i21 = -i21;
        }
        canvas.drawText(this.mChargeDurationString, ((((i - this.mChargeDurationStringWidth) - this.mDrainStringWidth) / 2) + (zIsLayoutRtl ? this.mDrainStringWidth : this.mChargeLabelStringWidth)) - i21, f2, this.mHeaderTextPaint);
        this.mHeaderTextPaint.setTextAlign(align);
        canvas.drawText(this.mDrainString, i3, f2, this.mHeaderTextPaint);
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
            this.mPhoneSignalChart.draw(canvas, (i2 - this.mPhoneSignalOffset) - (this.mLineWidth / 2), this.mLineWidth);
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
            Paint.Align textAlign = this.mTextPaint.getTextAlign();
            this.mTextPaint.setTextAlign(align3);
            if (this.mHavePhoneSignal) {
                canvas.drawText(this.mPhoneSignalLabel, f, (i2 - this.mPhoneSignalOffset) - this.mTextDescent, this.mTextPaint);
            }
            if (this.mHaveGps) {
                canvas.drawText(this.mGpsOnLabel, f, (i2 - this.mGpsOnOffset) - this.mTextDescent, this.mTextPaint);
            }
            if (this.mHaveFlashlight) {
                canvas.drawText(this.mFlashlightOnLabel, f, (i2 - this.mFlashlightOnOffset) - this.mTextDescent, this.mTextPaint);
            }
            if (this.mHaveCamera) {
                canvas.drawText(this.mCameraOnLabel, f, (i2 - this.mCameraOnOffset) - this.mTextDescent, this.mTextPaint);
            }
            if (this.mHaveWifi) {
                canvas.drawText(this.mWifiRunningLabel, f, (i2 - this.mWifiRunningOffset) - this.mTextDescent, this.mTextPaint);
            }
            canvas.drawText(this.mCpuRunningLabel, f, (i2 - this.mCpuRunningOffset) - this.mTextDescent, this.mTextPaint);
            canvas.drawText(this.mChargingLabel, f, (i2 - this.mChargingOffset) - this.mTextDescent, this.mTextPaint);
            canvas.drawText(this.mScreenOnLabel, f, (i2 - this.mScreenOnOffset) - this.mTextDescent, this.mTextPaint);
            this.mTextPaint.setTextAlign(textAlign);
        }
        canvas.drawLine(this.mLevelLeft - this.mThinLineWidth, this.mLevelTop, this.mLevelLeft - this.mThinLineWidth, this.mLevelBottom + (this.mThinLineWidth / 2), this.mTextPaint);
        if (this.mLargeMode) {
            for (int i22 = 0; i22 < 10; i22++) {
                float f3 = this.mLevelTop + (this.mThinLineWidth / 2) + (((this.mLevelBottom - this.mLevelTop) * i22) / 10);
                canvas.drawLine((this.mLevelLeft - (this.mThinLineWidth * 2)) - (this.mThinLineWidth / 2), f3, (this.mLevelLeft - this.mThinLineWidth) - (this.mThinLineWidth / 2), f3, this.mTextPaint);
            }
        }
        canvas.drawText(this.mMaxPercentLabelString, 0.0f, this.mLevelTop, this.mTextPaint);
        canvas.drawText(this.mMinPercentLabelString, this.mMaxPercentLabelStringWidth - this.mMinPercentLabelStringWidth, this.mLevelBottom - this.mThinLineWidth, this.mTextPaint);
        canvas.drawLine(this.mLevelLeft / 2, this.mLevelBottom + this.mThinLineWidth, i, this.mLevelBottom + this.mThinLineWidth, this.mTextPaint);
        if (this.mDateLabels.size() > 0) {
            int i23 = this.mLevelTop + this.mTextAscent;
            int i24 = this.mLevelBottom;
            int i25 = this.mLevelRight;
            this.mTextPaint.setTextAlign(Paint.Align.LEFT);
            for (int size = this.mDateLabels.size() - 1; size >= 0; size--) {
                DateLabel dateLabel = this.mDateLabels.get(size);
                int i26 = dateLabel.x - this.mThinLineWidth;
                int i27 = dateLabel.x + (this.mThinLineWidth * 2);
                if ((dateLabel.width + i27 < i25 || (i26 = (i27 = (dateLabel.x - (this.mThinLineWidth * 2)) - dateLabel.width) - this.mThinLineWidth) < i25) && i26 >= this.mLevelLeft) {
                    this.mDateLinePath.reset();
                    this.mDateLinePath.moveTo(dateLabel.x, i23);
                    this.mDateLinePath.lineTo(dateLabel.x, i24);
                    canvas.drawPath(this.mDateLinePath, this.mDateLinePaint);
                    canvas.drawText(dateLabel.label, i27, i23 - this.mTextAscent, this.mTextPaint);
                }
            }
        }
    }
}
