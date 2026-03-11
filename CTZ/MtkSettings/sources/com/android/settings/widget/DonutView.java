package com.android.settings.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.icu.text.DecimalFormatSymbols;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;

public class DonutView extends View {
    private Paint mBackgroundCircle;
    private TextPaint mBigNumberPaint;
    private Paint mFilledArc;
    private String mFullString;
    private int mMeterBackgroundColor;
    private int mMeterConsumedColor;
    private double mPercent;
    private String mPercentString;
    private boolean mShowPercentString;
    private float mStrokeWidth;
    private TextPaint mTextPaint;

    public DonutView(Context context) {
        super(context);
        this.mShowPercentString = true;
    }

    public DonutView(Context context, AttributeSet attributeSet) {
        boolean z;
        super(context, attributeSet);
        this.mShowPercentString = true;
        this.mMeterBackgroundColor = context.getColor(R.color.meter_background_color);
        this.mMeterConsumedColor = Utils.getDefaultColor(this.mContext, R.color.meter_consumed_color);
        Resources resources = context.getResources();
        this.mStrokeWidth = resources.getDimension(R.dimen.storage_donut_thickness);
        if (attributeSet != null) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.DonutView);
            this.mMeterBackgroundColor = typedArrayObtainStyledAttributes.getColor(1, this.mMeterBackgroundColor);
            this.mMeterConsumedColor = typedArrayObtainStyledAttributes.getColor(2, this.mMeterConsumedColor);
            z = typedArrayObtainStyledAttributes.getBoolean(0, true);
            this.mShowPercentString = typedArrayObtainStyledAttributes.getBoolean(3, true);
            this.mStrokeWidth = typedArrayObtainStyledAttributes.getDimensionPixelSize(4, (int) this.mStrokeWidth);
            typedArrayObtainStyledAttributes.recycle();
        } else {
            z = true;
        }
        this.mBackgroundCircle = new Paint();
        this.mBackgroundCircle.setAntiAlias(true);
        this.mBackgroundCircle.setStrokeCap(Paint.Cap.BUTT);
        this.mBackgroundCircle.setStyle(Paint.Style.STROKE);
        this.mBackgroundCircle.setStrokeWidth(this.mStrokeWidth);
        this.mBackgroundCircle.setColor(this.mMeterBackgroundColor);
        this.mFilledArc = new Paint();
        this.mFilledArc.setAntiAlias(true);
        this.mFilledArc.setStrokeCap(Paint.Cap.BUTT);
        this.mFilledArc.setStyle(Paint.Style.STROKE);
        this.mFilledArc.setStrokeWidth(this.mStrokeWidth);
        this.mFilledArc.setColor(this.mMeterConsumedColor);
        if (z) {
            PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(Utils.getColorAttr(context, android.R.attr.colorAccent), PorterDuff.Mode.SRC_IN);
            this.mBackgroundCircle.setColorFilter(porterDuffColorFilter);
            this.mFilledArc.setColorFilter(porterDuffColorFilter);
        }
        int i = TextUtils.getLayoutDirectionFromLocale(resources.getConfiguration().locale) == 0 ? 0 : 1;
        this.mTextPaint = new TextPaint();
        this.mTextPaint.setColor(Utils.getColorAccent(getContext()));
        this.mTextPaint.setAntiAlias(true);
        this.mTextPaint.setTextSize(resources.getDimension(R.dimen.storage_donut_view_label_text_size));
        this.mTextPaint.setTextAlign(Paint.Align.CENTER);
        this.mTextPaint.setBidiFlags(i);
        this.mBigNumberPaint = new TextPaint();
        this.mBigNumberPaint.setColor(Utils.getColorAccent(getContext()));
        this.mBigNumberPaint.setAntiAlias(true);
        this.mBigNumberPaint.setTextSize(resources.getDimension(R.dimen.storage_donut_view_percent_text_size));
        this.mBigNumberPaint.setTypeface(Typeface.create(context.getString(android.R.string.aerr_process_repeated), 0));
        this.mBigNumberPaint.setBidiFlags(i);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDonut(canvas);
        if (this.mShowPercentString) {
            drawInnerText(canvas);
        }
    }

    private void drawDonut(Canvas canvas) {
        canvas.drawArc(0.0f + this.mStrokeWidth, 0.0f + this.mStrokeWidth, getWidth() - this.mStrokeWidth, getHeight() - this.mStrokeWidth, -90.0f, 360.0f, false, this.mBackgroundCircle);
        canvas.drawArc(0.0f + this.mStrokeWidth, 0.0f + this.mStrokeWidth, getWidth() - this.mStrokeWidth, getHeight() - this.mStrokeWidth, -90.0f, 360.0f * ((float) this.mPercent), false, this.mFilledArc);
    }

    private void drawInnerText(Canvas canvas) {
        float width = getWidth() / 2;
        float height = getHeight() / 2;
        float textHeight = getTextHeight(this.mTextPaint) + getTextHeight(this.mBigNumberPaint);
        String percentString = new DecimalFormatSymbols().getPercentString();
        canvas.save();
        StaticLayout staticLayout = new StaticLayout(getPercentageStringSpannable(getResources(), this.mPercentString, percentString), this.mBigNumberPaint, getWidth(), Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
        canvas.translate(0.0f, (getHeight() - textHeight) / 2.0f);
        staticLayout.draw(canvas);
        canvas.restore();
        canvas.drawText(this.mFullString, width, (height + (textHeight / 2.0f)) - this.mTextPaint.descent(), this.mTextPaint);
    }

    public void setPercentage(double d) {
        this.mPercent = d;
        this.mPercentString = Utils.formatPercentage(this.mPercent);
        this.mFullString = getContext().getString(R.string.storage_percent_full);
        if (this.mFullString.length() > 10) {
            this.mTextPaint.setTextSize(getContext().getResources().getDimension(R.dimen.storage_donut_view_shrunken_label_text_size));
        }
        setContentDescription(getContext().getString(R.string.join_many_items_middle, this.mPercentString, this.mFullString));
        invalidate();
    }

    public int getMeterBackgroundColor() {
        return this.mMeterBackgroundColor;
    }

    public void setMeterBackgroundColor(int i) {
        this.mMeterBackgroundColor = i;
        this.mBackgroundCircle.setColor(i);
        invalidate();
    }

    public int getMeterConsumedColor() {
        return this.mMeterConsumedColor;
    }

    public void setMeterConsumedColor(int i) {
        this.mMeterConsumedColor = i;
        this.mFilledArc.setColor(i);
        invalidate();
    }

    @VisibleForTesting
    static Spannable getPercentageStringSpannable(Resources resources, String str, String str2) {
        float dimension = resources.getDimension(R.dimen.storage_donut_view_percent_sign_size) / resources.getDimension(R.dimen.storage_donut_view_percent_text_size);
        SpannableString spannableString = new SpannableString(str);
        int iIndexOf = str.indexOf(str2);
        int length = str2.length() + iIndexOf;
        if (iIndexOf < 0) {
            iIndexOf = 0;
            length = str.length();
        }
        spannableString.setSpan(new RelativeSizeSpan(dimension), iIndexOf, length, 34);
        return spannableString;
    }

    private float getTextHeight(TextPaint textPaint) {
        return textPaint.descent() - textPaint.ascent();
    }
}
