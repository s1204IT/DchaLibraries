package com.android.settingslib.graph;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import com.android.settingslib.R;
import com.android.settingslib.Utils;

/* loaded from: classes.dex */
public class BatteryMeterDrawableBase extends Drawable {
    public static final String TAG = BatteryMeterDrawableBase.class.getSimpleName();
    protected final Paint mBatteryPaint;
    protected final Paint mBoltPaint;
    private final float[] mBoltPoints;
    protected float mButtonHeightFraction;
    private int mChargeColor;
    private boolean mCharging;
    private final int[] mColors;
    protected final Context mContext;
    private final int mCriticalLevel;
    protected final Paint mFramePaint;
    private int mHeight;
    private final int mIntrinsicHeight;
    private final int mIntrinsicWidth;
    protected final Paint mPlusPaint;
    private final float[] mPlusPoints;
    private boolean mPowerSaveEnabled;
    protected final Paint mPowersavePaint;
    private boolean mShowPercent;
    private float mSubpixelSmoothingLeft;
    private float mSubpixelSmoothingRight;
    private float mTextHeight;
    protected final Paint mTextPaint;
    private String mWarningString;
    private float mWarningTextHeight;
    protected final Paint mWarningTextPaint;
    private int mWidth;
    private int mLevel = -1;
    protected boolean mPowerSaveAsColorError = true;
    private int mIconTint = -1;
    private float mOldDarkIntensity = -1.0f;
    private final Path mBoltPath = new Path();
    private final Path mPlusPath = new Path();
    private final Rect mPadding = new Rect();
    private final RectF mFrame = new RectF();
    private final RectF mButtonFrame = new RectF();
    private final RectF mBoltFrame = new RectF();
    private final RectF mPlusFrame = new RectF();
    private final Path mShapePath = new Path();
    private final Path mOutlinePath = new Path();
    private final Path mTextPath = new Path();

    public BatteryMeterDrawableBase(Context context, int i) throws Resources.NotFoundException {
        this.mContext = context;
        Resources resources = context.getResources();
        TypedArray typedArrayObtainTypedArray = resources.obtainTypedArray(R.array.batterymeter_color_levels);
        TypedArray typedArrayObtainTypedArray2 = resources.obtainTypedArray(R.array.batterymeter_color_values);
        int length = typedArrayObtainTypedArray.length();
        this.mColors = new int[2 * length];
        for (int i2 = 0; i2 < length; i2++) {
            int i3 = 2 * i2;
            this.mColors[i3] = typedArrayObtainTypedArray.getInt(i2, 0);
            if (typedArrayObtainTypedArray2.getType(i2) == 2) {
                this.mColors[i3 + 1] = Utils.getColorAttr(context, typedArrayObtainTypedArray2.getThemeAttributeId(i2, 0));
            } else {
                this.mColors[i3 + 1] = typedArrayObtainTypedArray2.getColor(i2, 0);
            }
        }
        typedArrayObtainTypedArray.recycle();
        typedArrayObtainTypedArray2.recycle();
        this.mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);
        this.mCriticalLevel = this.mContext.getResources().getInteger(android.R.integer.config_batterySaver_full_locationMode);
        this.mButtonHeightFraction = context.getResources().getFraction(R.fraction.battery_button_height_fraction, 1, 1);
        this.mSubpixelSmoothingLeft = context.getResources().getFraction(R.fraction.battery_subpixel_smoothing_left, 1, 1);
        this.mSubpixelSmoothingRight = context.getResources().getFraction(R.fraction.battery_subpixel_smoothing_right, 1, 1);
        this.mFramePaint = new Paint(1);
        this.mFramePaint.setColor(i);
        this.mFramePaint.setDither(true);
        this.mFramePaint.setStrokeWidth(0.0f);
        this.mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mBatteryPaint = new Paint(1);
        this.mBatteryPaint.setDither(true);
        this.mBatteryPaint.setStrokeWidth(0.0f);
        this.mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mTextPaint = new Paint(1);
        this.mTextPaint.setTypeface(Typeface.create("sans-serif-condensed", 1));
        this.mTextPaint.setTextAlign(Paint.Align.CENTER);
        this.mWarningTextPaint = new Paint(1);
        this.mWarningTextPaint.setTypeface(Typeface.create("sans-serif", 1));
        this.mWarningTextPaint.setTextAlign(Paint.Align.CENTER);
        if (this.mColors.length > 1) {
            this.mWarningTextPaint.setColor(this.mColors[1]);
        }
        this.mChargeColor = Utils.getDefaultColor(this.mContext, R.color.meter_consumed_color);
        this.mBoltPaint = new Paint(1);
        this.mBoltPaint.setColor(Utils.getDefaultColor(this.mContext, R.color.batterymeter_bolt_color));
        this.mBoltPoints = loadPoints(resources, R.array.batterymeter_bolt_points);
        this.mPlusPaint = new Paint(1);
        this.mPlusPaint.setColor(Utils.getDefaultColor(this.mContext, R.color.batterymeter_plus_color));
        this.mPlusPoints = loadPoints(resources, R.array.batterymeter_plus_points);
        this.mPowersavePaint = new Paint(1);
        this.mPowersavePaint.setColor(this.mPlusPaint.getColor());
        this.mPowersavePaint.setStyle(Paint.Style.STROKE);
        this.mPowersavePaint.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.battery_powersave_outline_thickness));
        this.mIntrinsicWidth = context.getResources().getDimensionPixelSize(R.dimen.battery_width);
        this.mIntrinsicHeight = context.getResources().getDimensionPixelSize(R.dimen.battery_height);
    }

    @Override // android.graphics.drawable.Drawable
    public int getIntrinsicHeight() {
        return this.mIntrinsicHeight;
    }

    @Override // android.graphics.drawable.Drawable
    public int getIntrinsicWidth() {
        return this.mIntrinsicWidth;
    }

    public void setShowPercent(boolean z) {
        this.mShowPercent = z;
        postInvalidate();
    }

    public void setCharging(boolean z) {
        this.mCharging = z;
        postInvalidate();
    }

    public boolean getCharging() {
        return this.mCharging;
    }

    public void setBatteryLevel(int i) {
        this.mLevel = i;
        postInvalidate();
    }

    public int getBatteryLevel() {
        return this.mLevel;
    }

    protected void postInvalidate() {
        unscheduleSelf(new Runnable() { // from class: com.android.settingslib.graph.-$$Lambda$BatteryMeterDrawableBase$ExJ0HHRzS2_LMtcBJqtFiovbn0w
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.invalidateSelf();
            }
        });
        scheduleSelf(new Runnable() { // from class: com.android.settingslib.graph.-$$Lambda$BatteryMeterDrawableBase$ExJ0HHRzS2_LMtcBJqtFiovbn0w
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.invalidateSelf();
            }
        }, 0L);
    }

    private static float[] loadPoints(Resources resources, int i) throws Resources.NotFoundException {
        int[] intArray = resources.getIntArray(i);
        int iMax = 0;
        int iMax2 = 0;
        for (int i2 = 0; i2 < intArray.length; i2 += 2) {
            iMax = Math.max(iMax, intArray[i2]);
            iMax2 = Math.max(iMax2, intArray[i2 + 1]);
        }
        float[] fArr = new float[intArray.length];
        for (int i3 = 0; i3 < intArray.length; i3 += 2) {
            fArr[i3] = intArray[i3] / iMax;
            fArr[i3 + 1] = intArray[r3] / iMax2;
        }
        return fArr;
    }

    @Override // android.graphics.drawable.Drawable
    public void setBounds(int i, int i2, int i3, int i4) {
        super.setBounds(i, i2, i3, i4);
        updateSize();
    }

    private void updateSize() {
        Rect bounds = getBounds();
        this.mHeight = (bounds.bottom - this.mPadding.bottom) - (bounds.top + this.mPadding.top);
        this.mWidth = (bounds.right - this.mPadding.right) - (bounds.left + this.mPadding.left);
        this.mWarningTextPaint.setTextSize(this.mHeight * 0.75f);
        this.mWarningTextHeight = -this.mWarningTextPaint.getFontMetrics().ascent;
    }

    @Override // android.graphics.drawable.Drawable
    public boolean getPadding(Rect rect) {
        if (this.mPadding.left == 0 && this.mPadding.top == 0 && this.mPadding.right == 0 && this.mPadding.bottom == 0) {
            return super.getPadding(rect);
        }
        rect.set(this.mPadding);
        return true;
    }

    public void setPadding(int i, int i2, int i3, int i4) {
        this.mPadding.left = i;
        this.mPadding.top = i2;
        this.mPadding.right = i3;
        this.mPadding.bottom = i4;
        updateSize();
    }

    private int getColorForLevel(int i) {
        int i2 = 0;
        int i3 = 0;
        while (i2 < this.mColors.length) {
            int i4 = this.mColors[i2];
            int i5 = this.mColors[i2 + 1];
            if (i > i4) {
                i2 += 2;
                i3 = i5;
            } else {
                if (i2 == this.mColors.length - 2) {
                    return this.mIconTint;
                }
                return i5;
            }
        }
        return i3;
    }

    protected int batteryColorForLevel(int i) {
        if (this.mCharging || (this.mPowerSaveEnabled && this.mPowerSaveAsColorError)) {
            return this.mChargeColor;
        }
        return getColorForLevel(i);
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        float f;
        float f2;
        int i = this.mLevel;
        Rect bounds = getBounds();
        if (i == -1) {
            return;
        }
        float f3 = i / 100.0f;
        int i2 = this.mHeight;
        int aspectRatio = (int) (getAspectRatio() * this.mHeight);
        int i3 = (this.mWidth - aspectRatio) / 2;
        float f4 = i2;
        int iRound = Math.round(this.mButtonHeightFraction * f4);
        int i4 = this.mPadding.left + bounds.left;
        float f5 = i4;
        float f6 = (bounds.bottom - this.mPadding.bottom) - i2;
        this.mFrame.set(f5, f6, i4 + aspectRatio, i2 + r3);
        this.mFrame.offset(i3, 0.0f);
        float f7 = aspectRatio * 0.28f;
        float f8 = iRound;
        this.mButtonFrame.set(this.mFrame.left + Math.round(f7), this.mFrame.top, this.mFrame.right - Math.round(f7), this.mFrame.top + f8);
        this.mFrame.top += f8;
        this.mBatteryPaint.setColor(batteryColorForLevel(i));
        if (i < 96) {
            if (i <= this.mCriticalLevel) {
                f3 = 0.0f;
            }
        } else {
            f3 = 1.0f;
        }
        float fHeight = f3 == 1.0f ? this.mButtonFrame.top : this.mFrame.top + (this.mFrame.height() * (1.0f - f3));
        this.mShapePath.reset();
        this.mOutlinePath.reset();
        float radiusRatio = getRadiusRatio() * (this.mFrame.height() + f8);
        this.mShapePath.setFillType(Path.FillType.WINDING);
        this.mShapePath.addRoundRect(this.mFrame, radiusRatio, radiusRatio, Path.Direction.CW);
        this.mShapePath.addRect(this.mButtonFrame, Path.Direction.CW);
        this.mOutlinePath.addRoundRect(this.mFrame, radiusRatio, radiusRatio, Path.Direction.CW);
        Path path = new Path();
        path.addRect(this.mButtonFrame, Path.Direction.CW);
        this.mOutlinePath.op(path, Path.Op.XOR);
        if (this.mCharging) {
            float fWidth = this.mFrame.left + (this.mFrame.width() / 4.0f) + 1.0f;
            float fHeight2 = this.mFrame.top + (this.mFrame.height() / 6.0f);
            float fWidth2 = (this.mFrame.right - (this.mFrame.width() / 4.0f)) + 1.0f;
            float fHeight3 = this.mFrame.bottom - (this.mFrame.height() / 10.0f);
            if (this.mBoltFrame.left != fWidth || this.mBoltFrame.top != fHeight2 || this.mBoltFrame.right != fWidth2 || this.mBoltFrame.bottom != fHeight3) {
                this.mBoltFrame.set(fWidth, fHeight2, fWidth2, fHeight3);
                this.mBoltPath.reset();
                this.mBoltPath.moveTo(this.mBoltFrame.left + (this.mBoltPoints[0] * this.mBoltFrame.width()), this.mBoltFrame.top + (this.mBoltPoints[1] * this.mBoltFrame.height()));
                for (int i5 = 2; i5 < this.mBoltPoints.length; i5 += 2) {
                    this.mBoltPath.lineTo(this.mBoltFrame.left + (this.mBoltPoints[i5] * this.mBoltFrame.width()), this.mBoltFrame.top + (this.mBoltPoints[i5 + 1] * this.mBoltFrame.height()));
                }
                this.mBoltPath.lineTo(this.mBoltFrame.left + (this.mBoltPoints[0] * this.mBoltFrame.width()), this.mBoltFrame.top + (this.mBoltPoints[1] * this.mBoltFrame.height()));
            }
            if (Math.min(Math.max((this.mBoltFrame.bottom - fHeight) / (this.mBoltFrame.bottom - this.mBoltFrame.top), 0.0f), 1.0f) <= 0.3f) {
                canvas.drawPath(this.mBoltPath, this.mBoltPaint);
            } else {
                this.mShapePath.op(this.mBoltPath, Path.Op.DIFFERENCE);
            }
        } else if (this.mPowerSaveEnabled) {
            float fWidth3 = (this.mFrame.width() * 2.0f) / 3.0f;
            float fWidth4 = this.mFrame.left + ((this.mFrame.width() - fWidth3) / 2.0f);
            float fHeight4 = this.mFrame.top + ((this.mFrame.height() - fWidth3) / 2.0f);
            float fWidth5 = this.mFrame.right - ((this.mFrame.width() - fWidth3) / 2.0f);
            float fHeight5 = this.mFrame.bottom - ((this.mFrame.height() - fWidth3) / 2.0f);
            if (this.mPlusFrame.left != fWidth4 || this.mPlusFrame.top != fHeight4 || this.mPlusFrame.right != fWidth5 || this.mPlusFrame.bottom != fHeight5) {
                this.mPlusFrame.set(fWidth4, fHeight4, fWidth5, fHeight5);
                this.mPlusPath.reset();
                this.mPlusPath.moveTo(this.mPlusFrame.left + (this.mPlusPoints[0] * this.mPlusFrame.width()), this.mPlusFrame.top + (this.mPlusPoints[1] * this.mPlusFrame.height()));
                for (int i6 = 2; i6 < this.mPlusPoints.length; i6 += 2) {
                    this.mPlusPath.lineTo(this.mPlusFrame.left + (this.mPlusPoints[i6] * this.mPlusFrame.width()), this.mPlusFrame.top + (this.mPlusPoints[i6 + 1] * this.mPlusFrame.height()));
                }
                this.mPlusPath.lineTo(this.mPlusFrame.left + (this.mPlusPoints[0] * this.mPlusFrame.width()), this.mPlusFrame.top + (this.mPlusPoints[1] * this.mPlusFrame.height()));
            }
            this.mShapePath.op(this.mPlusPath, Path.Op.DIFFERENCE);
            if (this.mPowerSaveAsColorError) {
                canvas.drawPath(this.mPlusPath, this.mPlusPaint);
            }
        }
        String strValueOf = null;
        if (!this.mCharging && !this.mPowerSaveEnabled && i > this.mCriticalLevel && this.mShowPercent) {
            this.mTextPaint.setColor(getColorForLevel(i));
            this.mTextPaint.setTextSize(f4 * (this.mLevel == 100 ? 0.38f : 0.5f));
            this.mTextHeight = -this.mTextPaint.getFontMetrics().ascent;
            strValueOf = String.valueOf(i);
            f = (this.mWidth * 0.5f) + f5;
            f2 = ((this.mHeight + this.mTextHeight) * 0.47f) + f6;
            z = fHeight > f2;
            if (!z) {
                this.mTextPath.reset();
                this.mTextPaint.getTextPath(strValueOf, 0, strValueOf.length(), f, f2, this.mTextPath);
                this.mShapePath.op(this.mTextPath, Path.Op.DIFFERENCE);
            }
        } else {
            f = 0.0f;
            f2 = 0.0f;
        }
        canvas.drawPath(this.mShapePath, this.mFramePaint);
        this.mFrame.top = fHeight;
        canvas.save();
        canvas.clipRect(this.mFrame);
        canvas.drawPath(this.mShapePath, this.mBatteryPaint);
        canvas.restore();
        if (!this.mCharging && !this.mPowerSaveEnabled) {
            if (i <= this.mCriticalLevel) {
                canvas.drawText(this.mWarningString, (this.mWidth * 0.5f) + f5, ((this.mHeight + this.mWarningTextHeight) * 0.48f) + f6, this.mWarningTextPaint);
            } else if (z) {
                canvas.drawText(strValueOf, f, f2, this.mTextPaint);
            }
        }
        if (!this.mCharging && this.mPowerSaveEnabled && this.mPowerSaveAsColorError) {
            canvas.drawPath(this.mOutlinePath, this.mPowersavePaint);
        }
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        this.mFramePaint.setColorFilter(colorFilter);
        this.mBatteryPaint.setColorFilter(colorFilter);
        this.mWarningTextPaint.setColorFilter(colorFilter);
        this.mBoltPaint.setColorFilter(colorFilter);
        this.mPlusPaint.setColorFilter(colorFilter);
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return 0;
    }

    public int getCriticalLevel() {
        return this.mCriticalLevel;
    }

    protected float getAspectRatio() {
        return 0.58f;
    }

    protected float getRadiusRatio() {
        return 0.05882353f;
    }
}
