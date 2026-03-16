package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryMeterView extends View implements DemoMode, BatteryController.BatteryStateChangeCallback {
    public static final String TAG = BatteryMeterView.class.getSimpleName();
    private BatteryController mBatteryController;
    private final Paint mBatteryPaint;
    private final RectF mBoltFrame;
    private final Paint mBoltPaint;
    private final Path mBoltPath;
    private final float[] mBoltPoints;
    private final RectF mButtonFrame;
    private float mButtonHeightFraction;
    private final int mChargeColor;
    private final Path mClipPath;
    private final int[] mColors;
    private final int mCriticalLevel;
    private boolean mDemoMode;
    private BatteryTracker mDemoTracker;
    private final RectF mFrame;
    private final Paint mFramePaint;
    private int mHeight;
    private boolean mPowerSaveEnabled;
    private final Path mShapePath;
    boolean mShowPercent;
    private float mSubpixelSmoothingLeft;
    private float mSubpixelSmoothingRight;
    private float mTextHeight;
    private final Paint mTextPaint;
    private final Path mTextPath;
    BatteryTracker mTracker;
    private String mWarningString;
    private float mWarningTextHeight;
    private final Paint mWarningTextPaint;
    private int mWidth;

    private class BatteryTracker extends BroadcastReceiver {
        int health;
        int level;
        int plugType;
        boolean plugged;
        int status;
        String technology;
        int temperature;
        boolean testmode;
        int voltage;

        private BatteryTracker() {
            this.level = -1;
            this.testmode = false;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                if (!this.testmode || intent.getBooleanExtra("testmode", false)) {
                    this.level = (int) ((100.0f * intent.getIntExtra("level", 0)) / intent.getIntExtra("scale", 100));
                    this.plugType = intent.getIntExtra("plugged", 0);
                    this.plugged = this.plugType != 0;
                    this.health = intent.getIntExtra("health", 1);
                    this.status = intent.getIntExtra("status", 1);
                    this.technology = intent.getStringExtra("technology");
                    this.voltage = intent.getIntExtra("voltage", 0);
                    this.temperature = intent.getIntExtra("temperature", 0);
                    BatteryMeterView.this.setContentDescription(context.getString(R.string.accessibility_battery_level, Integer.valueOf(this.level)));
                    BatteryMeterView.this.postInvalidate();
                    return;
                }
                return;
            }
            if (action.equals("com.android.systemui.BATTERY_LEVEL_TEST")) {
                this.testmode = true;
                BatteryMeterView.this.post(new Runnable() {
                    int saveLevel;
                    int savePlugged;
                    int curLevel = 0;
                    int incr = 1;
                    Intent dummy = new Intent("android.intent.action.BATTERY_CHANGED");

                    {
                        this.saveLevel = BatteryTracker.this.level;
                        this.savePlugged = BatteryTracker.this.plugType;
                    }

                    @Override
                    public void run() {
                        if (this.curLevel < 0) {
                            BatteryTracker.this.testmode = false;
                            this.dummy.putExtra("level", this.saveLevel);
                            this.dummy.putExtra("plugged", this.savePlugged);
                            this.dummy.putExtra("testmode", false);
                        } else {
                            this.dummy.putExtra("level", this.curLevel);
                            this.dummy.putExtra("plugged", this.incr > 0 ? 1 : 0);
                            this.dummy.putExtra("testmode", true);
                        }
                        BatteryMeterView.this.getContext().sendBroadcast(this.dummy);
                        if (BatteryTracker.this.testmode) {
                            this.curLevel += this.incr;
                            if (this.curLevel == 100) {
                                this.incr *= -1;
                            }
                            BatteryMeterView.this.postDelayed(this, 200L);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("com.android.systemui.BATTERY_LEVEL_TEST");
        Intent sticky = getContext().registerReceiver(this.mTracker, filter);
        if (sticky != null) {
            this.mTracker.onReceive(getContext(), sticky);
        }
        this.mBatteryController.addStateChangedCallback(this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(this.mTracker);
        this.mBatteryController.removeStateChangedCallback(this);
    }

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mShowPercent = true;
        this.mBoltPath = new Path();
        this.mFrame = new RectF();
        this.mButtonFrame = new RectF();
        this.mBoltFrame = new RectF();
        this.mShapePath = new Path();
        this.mClipPath = new Path();
        this.mTextPath = new Path();
        this.mTracker = new BatteryTracker();
        this.mDemoTracker = new BatteryTracker();
        Resources res = context.getResources();
        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView, defStyle, 0);
        int frameColor = atts.getColor(0, res.getColor(R.color.batterymeter_frame_color));
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
        atts.recycle();
        this.mShowPercent = Settings.System.getInt(context.getContentResolver(), "status_bar_show_battery_percent", 0) != 0;
        this.mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);
        this.mCriticalLevel = this.mContext.getResources().getInteger(android.R.integer.config_carDockKeepsScreenOn);
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
        this.mChargeColor = getResources().getColor(R.color.batterymeter_charge_color);
        this.mBoltPaint = new Paint(1);
        this.mBoltPaint.setColor(res.getColor(R.color.batterymeter_bolt_color));
        this.mBoltPoints = loadBoltPoints(res);
    }

    public void setBatteryController(BatteryController batteryController) {
        this.mBatteryController = batteryController;
        this.mPowerSaveEnabled = this.mBatteryController.isPowerSave();
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
    }

    @Override
    public void onPowerSaveChanged() {
        this.mPowerSaveEnabled = this.mBatteryController.isPowerSave();
        invalidate();
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

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mHeight = h;
        this.mWidth = w;
        this.mWarningTextPaint.setTextSize(h * 0.75f);
        this.mWarningTextHeight = -this.mWarningTextPaint.getFontMetrics().ascent;
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
                return color;
            }
        }
        return color;
    }

    @Override
    public void draw(Canvas c) {
        BatteryTracker tracker = this.mDemoMode ? this.mDemoTracker : this.mTracker;
        int level = tracker.level;
        if (level != -1) {
            float drawFrac = level / 100.0f;
            int pt = getPaddingTop();
            int pl = getPaddingLeft();
            int pr = getPaddingRight();
            int pb = getPaddingBottom();
            int height = (this.mHeight - pt) - pb;
            int width = (this.mWidth - pl) - pr;
            int buttonHeight = (int) (height * this.mButtonHeightFraction);
            this.mFrame.set(0.0f, 0.0f, width, height);
            this.mFrame.offset(pl, pt);
            this.mButtonFrame.set(this.mFrame.left + Math.round(width * 0.25f), this.mFrame.top, this.mFrame.right - Math.round(width * 0.25f), this.mFrame.top + buttonHeight);
            this.mButtonFrame.top += this.mSubpixelSmoothingLeft;
            this.mButtonFrame.left += this.mSubpixelSmoothingLeft;
            this.mButtonFrame.right -= this.mSubpixelSmoothingRight;
            this.mFrame.top += buttonHeight;
            this.mFrame.left += this.mSubpixelSmoothingLeft;
            this.mFrame.top += this.mSubpixelSmoothingLeft;
            this.mFrame.right -= this.mSubpixelSmoothingRight;
            this.mFrame.bottom -= this.mSubpixelSmoothingRight;
            this.mBatteryPaint.setColor(tracker.plugged ? this.mChargeColor : getColorForLevel(level));
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
            if (tracker.plugged) {
                float bl = this.mFrame.left + (this.mFrame.width() / 4.5f);
                float bt = this.mFrame.top + (this.mFrame.height() / 6.0f);
                float br = this.mFrame.right - (this.mFrame.width() / 7.0f);
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
            }
            boolean pctOpaque = false;
            float pctX = 0.0f;
            float pctY = 0.0f;
            String pctText = null;
            if (!tracker.plugged && level > this.mCriticalLevel && this.mShowPercent && tracker.level != 100) {
                this.mTextPaint.setColor(getColorForLevel(level));
                this.mTextPaint.setTextSize((tracker.level == 100 ? 0.38f : 0.5f) * height);
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
            if (!tracker.plugged) {
                if (level <= this.mCriticalLevel) {
                    float x = this.mWidth * 0.5f;
                    float y = (this.mHeight + this.mWarningTextHeight) * 0.48f;
                    c.drawText(this.mWarningString, x, y, this.mWarningTextPaint);
                } else if (pctOpaque) {
                    c.drawText(pctText, pctX, pctY, this.mTextPaint);
                }
            }
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!this.mDemoMode && command.equals("enter")) {
            this.mDemoMode = true;
            this.mDemoTracker.level = this.mTracker.level;
            this.mDemoTracker.plugged = this.mTracker.plugged;
            return;
        }
        if (this.mDemoMode && command.equals("exit")) {
            this.mDemoMode = false;
            postInvalidate();
            return;
        }
        if (this.mDemoMode && command.equals("battery")) {
            String level = args.getString("level");
            String plugged = args.getString("plugged");
            if (level != null) {
                this.mDemoTracker.level = Math.min(Math.max(Integer.parseInt(level), 0), 100);
            }
            if (plugged != null) {
                this.mDemoTracker.plugged = Boolean.parseBoolean(plugged);
            }
            postInvalidate();
        }
    }
}
