package com.android.systemui;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryMeterDrawable extends Drawable implements BatteryController.BatteryStateChangeCallback {
    public static final String TAG = BatteryMeterDrawable.class.getSimpleName();
    private BatteryController mBatteryController;
    private final Paint mBatteryPaint;
    private final Paint mBoltPaint;
    private final float[] mBoltPoints;
    private float mButtonHeightFraction;
    private int mChargeColor;
    private boolean mCharging;
    private final int[] mColors;
    private final Context mContext;
    private final int mCriticalLevel;
    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;
    private final Paint mFramePaint;
    private final Handler mHandler;
    private int mHeight;
    private final int mIntrinsicHeight;
    private final int mIntrinsicWidth;
    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;
    private boolean mListening;
    private boolean mPluggedIn;
    private final Paint mPlusPaint;
    private final float[] mPlusPoints;
    private boolean mPowerSaveEnabled;
    private boolean mShowPercent;
    private float mSubpixelSmoothingLeft;
    private float mSubpixelSmoothingRight;
    private float mTextHeight;
    private final Paint mTextPaint;
    private String mWarningString;
    private float mWarningTextHeight;
    private final Paint mWarningTextPaint;
    private int mWidth;
    private int mIconTint = -1;
    private float mOldDarkIntensity = 0.0f;
    private final Path mBoltPath = new Path();
    private final Path mPlusPath = new Path();
    private final RectF mFrame = new RectF();
    private final RectF mButtonFrame = new RectF();
    private final RectF mBoltFrame = new RectF();
    private final RectF mPlusFrame = new RectF();
    private final Path mShapePath = new Path();
    private final Path mClipPath = new Path();
    private final Path mTextPath = new Path();
    private final SettingObserver mSettingObserver = new SettingObserver();
    private int mLevel = -1;

    public BatteryMeterDrawable(Context context, Handler handler, int frameColor) {
        this.mContext = context;
        this.mHandler = handler;
        Resources res = context.getResources();
        TypedArray levels = res.obtainTypedArray(R.array.batterymeter_color_levels);
        TypedArray colors = res.obtainTypedArray(R.array.batterymeter_color_values);
        int N = levels.length();
        this.mColors = new int[N * 2];
        for (int i = 0; i < N; i++) {
            this.mColors[i * 2] = levels.getInt(i, 0);
            this.mColors[(i * 2) + 1] = colors.getColor(i, 0);
        }
        levels.recycle();
        colors.recycle();
        updateShowPercent();
        this.mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);
        this.mCriticalLevel = this.mContext.getResources().getInteger(android.R.integer.config_datause_threshold_bytes);
        this.mButtonHeightFraction = context.getResources().getFraction(R.fraction.battery_button_height_fraction, 1, 1);
        this.mSubpixelSmoothingLeft = context.getResources().getFraction(R.fraction.battery_subpixel_smoothing_left, 1, 1);
        this.mSubpixelSmoothingRight = context.getResources().getFraction(R.fraction.battery_subpixel_smoothing_right, 1, 1);
        this.mFramePaint = new Paint(1);
        this.mFramePaint.setColor(frameColor);
        this.mFramePaint.setDither(true);
        this.mFramePaint.setStrokeWidth(0.0f);
        this.mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mBatteryPaint = new Paint(1);
        this.mBatteryPaint.setDither(true);
        this.mBatteryPaint.setStrokeWidth(0.0f);
        this.mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mTextPaint = new Paint(1);
        Typeface font = Typeface.create("sans-serif-condensed", 1);
        this.mTextPaint.setTypeface(font);
        this.mTextPaint.setTextAlign(Paint.Align.CENTER);
        this.mWarningTextPaint = new Paint(1);
        this.mWarningTextPaint.setColor(this.mColors[1]);
        Typeface font2 = Typeface.create("sans-serif", 1);
        this.mWarningTextPaint.setTypeface(font2);
        this.mWarningTextPaint.setTextAlign(Paint.Align.CENTER);
        this.mChargeColor = context.getColor(R.color.batterymeter_charge_color);
        this.mBoltPaint = new Paint(1);
        this.mBoltPaint.setColor(context.getColor(R.color.batterymeter_bolt_color));
        this.mBoltPoints = loadBoltPoints(res);
        this.mPlusPaint = new Paint(this.mBoltPaint);
        this.mPlusPoints = loadPlusPoints(res);
        this.mDarkModeBackgroundColor = context.getColor(R.color.dark_mode_icon_color_dual_tone_background);
        this.mDarkModeFillColor = context.getColor(R.color.dark_mode_icon_color_dual_tone_fill);
        this.mLightModeBackgroundColor = context.getColor(R.color.light_mode_icon_color_dual_tone_background);
        this.mLightModeFillColor = context.getColor(R.color.light_mode_icon_color_dual_tone_fill);
        this.mIntrinsicWidth = context.getResources().getDimensionPixelSize(R.dimen.battery_width);
        this.mIntrinsicHeight = context.getResources().getDimensionPixelSize(R.dimen.battery_height);
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mIntrinsicHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mIntrinsicWidth;
    }

    public void startListening() {
        this.mListening = true;
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_show_battery_percent"), false, this.mSettingObserver);
        updateShowPercent();
        this.mBatteryController.addStateChangedCallback(this);
    }

    public void stopListening() {
        this.mListening = false;
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingObserver);
        this.mBatteryController.removeStateChangedCallback(this);
    }

    public void disableShowPercent() {
        this.mShowPercent = false;
        postInvalidate();
    }

    public void postInvalidate() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                BatteryMeterDrawable.this.invalidateSelf();
            }
        });
    }

    public void setBatteryController(BatteryController batteryController) {
        this.mBatteryController = batteryController;
        this.mPowerSaveEnabled = this.mBatteryController.isPowerSave();
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        this.mLevel = level;
        this.mPluggedIn = pluggedIn;
        this.mCharging = charging;
        Log.d(TAG, "onBatteryLevelChanged level:" + level + ",plugedIn:" + pluggedIn + ",charging:" + charging);
        postInvalidate();
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        this.mPowerSaveEnabled = isPowerSave;
        invalidateSelf();
    }

    private static float[] loadBoltPoints(Resources res) {
        int[] pts = res.getIntArray(R.array.batterymeter_bolt_points);
        int maxX = 0;
        int maxY = 0;
        for (int i = 0; i < pts.length; i += 2) {
            maxX = Math.max(maxX, pts[i]);
            maxY = Math.max(maxY, pts[i + 1]);
        }
        float[] ptsF = new float[pts.length];
        for (int i2 = 0; i2 < pts.length; i2 += 2) {
            ptsF[i2] = pts[i2] / maxX;
            ptsF[i2 + 1] = pts[i2 + 1] / maxY;
        }
        return ptsF;
    }

    private static float[] loadPlusPoints(Resources res) {
        int[] pts = res.getIntArray(R.array.batterymeter_plus_points);
        int maxX = 0;
        int maxY = 0;
        for (int i = 0; i < pts.length; i += 2) {
            maxX = Math.max(maxX, pts[i]);
            maxY = Math.max(maxY, pts[i + 1]);
        }
        float[] ptsF = new float[pts.length];
        for (int i2 = 0; i2 < pts.length; i2 += 2) {
            ptsF[i2] = pts[i2] / maxX;
            ptsF[i2 + 1] = pts[i2 + 1] / maxY;
        }
        return ptsF;
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        this.mHeight = bottom - top;
        this.mWidth = right - left;
        this.mWarningTextPaint.setTextSize(this.mHeight * 0.75f);
        this.mWarningTextHeight = -this.mWarningTextPaint.getFontMetrics().ascent;
    }

    public void updateShowPercent() {
        this.mShowPercent = Settings.System.getInt(this.mContext.getContentResolver(), "status_bar_show_battery_percent", 0) != 0;
    }

    private int getColorForLevel(int percent) {
        if (this.mPowerSaveEnabled) {
            return this.mColors[this.mColors.length - 1];
        }
        int color = 0;
        for (int i = 0; i < this.mColors.length; i += 2) {
            int thresh = this.mColors[i];
            color = this.mColors[i + 1];
            if (percent <= thresh) {
                if (i == this.mColors.length - 2) {
                    return this.mIconTint;
                }
                return color;
            }
        }
        return color;
    }

    public void setDarkIntensity(float darkIntensity) {
        if (darkIntensity == this.mOldDarkIntensity) {
            return;
        }
        int backgroundColor = getBackgroundColor(darkIntensity);
        int fillColor = getFillColor(darkIntensity);
        this.mIconTint = fillColor;
        this.mFramePaint.setColor(backgroundColor);
        this.mBoltPaint.setColor(fillColor);
        this.mChargeColor = fillColor;
        invalidateSelf();
        this.mOldDarkIntensity = darkIntensity;
    }

    private int getBackgroundColor(float darkIntensity) {
        return getColorForDarkIntensity(darkIntensity, this.mLightModeBackgroundColor, this.mDarkModeBackgroundColor);
    }

    private int getFillColor(float darkIntensity) {
        return getColorForDarkIntensity(darkIntensity, this.mLightModeFillColor, this.mDarkModeFillColor);
    }

    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return ((Integer) ArgbEvaluator.getInstance().evaluate(darkIntensity, Integer.valueOf(lightColor), Integer.valueOf(darkColor))).intValue();
    }

    @Override
    public void draw(Canvas c) {
        int level = this.mLevel;
        if (level == -1) {
            return;
        }
        float drawFrac = level / 100.0f;
        int height = this.mHeight;
        int width = (int) (this.mHeight * 0.6551724f);
        int px = (this.mWidth - width) / 2;
        int buttonHeight = (int) (height * this.mButtonHeightFraction);
        this.mFrame.set(0.0f, 0.0f, width, height);
        this.mFrame.offset(px, 0.0f);
        this.mButtonFrame.set(this.mFrame.left + Math.round(width * 0.25f), this.mFrame.top, this.mFrame.right - Math.round(width * 0.25f), this.mFrame.top + buttonHeight);
        this.mButtonFrame.top += this.mSubpixelSmoothingLeft;
        this.mButtonFrame.left += this.mSubpixelSmoothingLeft;
        this.mButtonFrame.right -= this.mSubpixelSmoothingRight;
        this.mFrame.top += buttonHeight;
        this.mFrame.left += this.mSubpixelSmoothingLeft;
        this.mFrame.top += this.mSubpixelSmoothingLeft;
        this.mFrame.right -= this.mSubpixelSmoothingRight;
        this.mFrame.bottom -= this.mSubpixelSmoothingRight;
        this.mBatteryPaint.setColor(this.mPluggedIn ? this.mChargeColor : getColorForLevel(level));
        if (level >= 96) {
            drawFrac = 1.0f;
        } else if (level <= this.mCriticalLevel) {
            drawFrac = 0.0f;
        }
        float levelTop = drawFrac == 1.0f ? this.mButtonFrame.top : this.mFrame.top + (this.mFrame.height() * (1.0f - drawFrac));
        this.mShapePath.reset();
        this.mShapePath.moveTo(this.mButtonFrame.left, this.mButtonFrame.top);
        this.mShapePath.lineTo(this.mButtonFrame.right, this.mButtonFrame.top);
        this.mShapePath.lineTo(this.mButtonFrame.right, this.mFrame.top);
        this.mShapePath.lineTo(this.mFrame.right, this.mFrame.top);
        this.mShapePath.lineTo(this.mFrame.right, this.mFrame.bottom);
        this.mShapePath.lineTo(this.mFrame.left, this.mFrame.bottom);
        this.mShapePath.lineTo(this.mFrame.left, this.mFrame.top);
        this.mShapePath.lineTo(this.mButtonFrame.left, this.mFrame.top);
        this.mShapePath.lineTo(this.mButtonFrame.left, this.mButtonFrame.top);
        if (this.mPluggedIn && this.mCharging) {
            float bl = this.mFrame.left + (this.mFrame.width() / 4.0f);
            float bt = this.mFrame.top + (this.mFrame.height() / 6.0f);
            float br = this.mFrame.right - (this.mFrame.width() / 4.0f);
            float bb = this.mFrame.bottom - (this.mFrame.height() / 10.0f);
            if (this.mBoltFrame.left != bl || this.mBoltFrame.top != bt || this.mBoltFrame.right != br || this.mBoltFrame.bottom != bb) {
                this.mBoltFrame.set(bl, bt, br, bb);
                this.mBoltPath.reset();
                this.mBoltPath.moveTo(this.mBoltFrame.left + (this.mBoltPoints[0] * this.mBoltFrame.width()), this.mBoltFrame.top + (this.mBoltPoints[1] * this.mBoltFrame.height()));
                for (int i = 2; i < this.mBoltPoints.length; i += 2) {
                    this.mBoltPath.lineTo(this.mBoltFrame.left + (this.mBoltPoints[i] * this.mBoltFrame.width()), this.mBoltFrame.top + (this.mBoltPoints[i + 1] * this.mBoltFrame.height()));
                }
                this.mBoltPath.lineTo(this.mBoltFrame.left + (this.mBoltPoints[0] * this.mBoltFrame.width()), this.mBoltFrame.top + (this.mBoltPoints[1] * this.mBoltFrame.height()));
            }
            float boltPct = (this.mBoltFrame.bottom - levelTop) / (this.mBoltFrame.bottom - this.mBoltFrame.top);
            if (Math.min(Math.max(boltPct, 0.0f), 1.0f) <= 0.3f) {
                c.drawPath(this.mBoltPath, this.mBoltPaint);
            } else {
                this.mShapePath.op(this.mBoltPath, Path.Op.DIFFERENCE);
            }
        } else if (this.mPowerSaveEnabled) {
            float pw = (this.mFrame.width() * 2.0f) / 3.0f;
            float pl = this.mFrame.left + ((this.mFrame.width() - pw) / 2.0f);
            float pt = this.mFrame.top + ((this.mFrame.height() - pw) / 2.0f);
            float pr = this.mFrame.right - ((this.mFrame.width() - pw) / 2.0f);
            float pb = this.mFrame.bottom - ((this.mFrame.height() - pw) / 2.0f);
            if (this.mPlusFrame.left != pl || this.mPlusFrame.top != pt || this.mPlusFrame.right != pr || this.mPlusFrame.bottom != pb) {
                this.mPlusFrame.set(pl, pt, pr, pb);
                this.mPlusPath.reset();
                this.mPlusPath.moveTo(this.mPlusFrame.left + (this.mPlusPoints[0] * this.mPlusFrame.width()), this.mPlusFrame.top + (this.mPlusPoints[1] * this.mPlusFrame.height()));
                for (int i2 = 2; i2 < this.mPlusPoints.length; i2 += 2) {
                    this.mPlusPath.lineTo(this.mPlusFrame.left + (this.mPlusPoints[i2] * this.mPlusFrame.width()), this.mPlusFrame.top + (this.mPlusPoints[i2 + 1] * this.mPlusFrame.height()));
                }
                this.mPlusPath.lineTo(this.mPlusFrame.left + (this.mPlusPoints[0] * this.mPlusFrame.width()), this.mPlusFrame.top + (this.mPlusPoints[1] * this.mPlusFrame.height()));
            }
            float boltPct2 = (this.mPlusFrame.bottom - levelTop) / (this.mPlusFrame.bottom - this.mPlusFrame.top);
            if (Math.min(Math.max(boltPct2, 0.0f), 1.0f) <= 0.3f) {
                c.drawPath(this.mPlusPath, this.mPlusPaint);
            } else {
                this.mShapePath.op(this.mPlusPath, Path.Op.DIFFERENCE);
            }
        }
        boolean pctOpaque = false;
        float pctX = 0.0f;
        float pctY = 0.0f;
        String pctText = null;
        if (!this.mPluggedIn && !this.mPowerSaveEnabled && level > this.mCriticalLevel && this.mShowPercent) {
            this.mTextPaint.setColor(getColorForLevel(level));
            this.mTextPaint.setTextSize((this.mLevel == 100 ? 0.38f : 0.5f) * height);
            this.mTextHeight = -this.mTextPaint.getFontMetrics().ascent;
            pctText = String.valueOf(level);
            pctX = this.mWidth * 0.5f;
            pctY = (this.mHeight + this.mTextHeight) * 0.47f;
            pctOpaque = levelTop > pctY;
            if (!pctOpaque) {
                this.mTextPath.reset();
                this.mTextPaint.getTextPath(pctText, 0, pctText.length(), pctX, pctY, this.mTextPath);
                this.mShapePath.op(this.mTextPath, Path.Op.DIFFERENCE);
            }
        }
        c.drawPath(this.mShapePath, this.mFramePaint);
        this.mFrame.top = levelTop;
        this.mClipPath.reset();
        this.mClipPath.addRect(this.mFrame, Path.Direction.CCW);
        this.mShapePath.op(this.mClipPath, Path.Op.INTERSECT);
        c.drawPath(this.mShapePath, this.mBatteryPaint);
        if (this.mPluggedIn || this.mPowerSaveEnabled) {
            return;
        }
        if (level <= this.mCriticalLevel) {
            float x = this.mWidth * 0.5f;
            float y = (this.mHeight + this.mWarningTextHeight) * 0.48f;
            c.drawText(this.mWarningString, x, y, this.mWarningTextPaint);
        } else {
            if (!pctOpaque) {
                return;
            }
            c.drawText(pctText, pctX, pctY, this.mTextPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            BatteryMeterDrawable.this.updateShowPercent();
            BatteryMeterDrawable.this.postInvalidate();
        }
    }
}
