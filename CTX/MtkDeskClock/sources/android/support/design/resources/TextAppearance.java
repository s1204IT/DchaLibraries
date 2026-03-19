package android.support.design.resources;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.annotation.FontRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewCompat;
import android.text.TextPaint;
import android.util.Log;

public class TextAppearance {
    private static final String TAG = "TextAppearance";
    private static final int TYPEFACE_MONOSPACE = 3;
    private static final int TYPEFACE_SANS = 1;
    private static final int TYPEFACE_SERIF = 2;

    @Nullable
    private Typeface font;

    @Nullable
    public final String fontFamily;

    @FontRes
    private final int fontFamilyResourceId;
    private boolean fontResolved = false;

    @Nullable
    public final ColorStateList shadowColor;
    public final float shadowDx;
    public final float shadowDy;
    public final float shadowRadius;
    public final boolean textAllCaps;

    @Nullable
    public final ColorStateList textColor;

    @Nullable
    public final ColorStateList textColorHint;

    @Nullable
    public final ColorStateList textColorLink;
    public final float textSize;
    public final int textStyle;
    public final int typeface;

    public TextAppearance(Context context, @StyleRes int id) {
        TypedArray a = context.obtainStyledAttributes(id, R.styleable.TextAppearance);
        this.textSize = a.getDimension(R.styleable.TextAppearance_android_textSize, 0.0f);
        this.textColor = MaterialResources.getColorStateList(context, a, R.styleable.TextAppearance_android_textColor);
        this.textColorHint = MaterialResources.getColorStateList(context, a, R.styleable.TextAppearance_android_textColorHint);
        this.textColorLink = MaterialResources.getColorStateList(context, a, R.styleable.TextAppearance_android_textColorLink);
        this.textStyle = a.getInt(R.styleable.TextAppearance_android_textStyle, 0);
        this.typeface = a.getInt(R.styleable.TextAppearance_android_typeface, 1);
        int fontFamilyIndex = MaterialResources.getIndexWithValue(a, R.styleable.TextAppearance_fontFamily, R.styleable.TextAppearance_android_fontFamily);
        this.fontFamilyResourceId = a.getResourceId(fontFamilyIndex, 0);
        this.fontFamily = a.getString(fontFamilyIndex);
        this.textAllCaps = a.getBoolean(R.styleable.TextAppearance_textAllCaps, false);
        this.shadowColor = MaterialResources.getColorStateList(context, a, R.styleable.TextAppearance_android_shadowColor);
        this.shadowDx = a.getFloat(R.styleable.TextAppearance_android_shadowDx, 0.0f);
        this.shadowDy = a.getFloat(R.styleable.TextAppearance_android_shadowDy, 0.0f);
        this.shadowRadius = a.getFloat(R.styleable.TextAppearance_android_shadowRadius, 0.0f);
        a.recycle();
    }

    @NonNull
    public Typeface getFont(Context context) {
        if (this.fontResolved) {
            return this.font;
        }
        if (!context.isRestricted()) {
            try {
                this.font = ResourcesCompat.getFont(context, this.fontFamilyResourceId);
                if (this.font != null) {
                    this.font = Typeface.create(this.font, this.textStyle);
                }
            } catch (Resources.NotFoundException | UnsupportedOperationException e) {
            } catch (Exception e2) {
                Log.d(TAG, "Error loading font " + this.fontFamily, e2);
            }
        }
        if (this.font == null) {
            this.font = Typeface.create(this.fontFamily, this.textStyle);
        }
        if (this.font == null) {
            switch (this.typeface) {
                case 1:
                    this.font = Typeface.SANS_SERIF;
                    break;
                case 2:
                    this.font = Typeface.SERIF;
                    break;
                case 3:
                    this.font = Typeface.MONOSPACE;
                    break;
                default:
                    this.font = Typeface.DEFAULT;
                    break;
            }
            if (this.font != null) {
                this.font = Typeface.create(this.font, this.textStyle);
            }
        }
        this.fontResolved = true;
        return this.font;
    }

    public void updateDrawState(Context context, TextPaint textPaint) {
        updateMeasureState(context, textPaint);
        textPaint.setColor(this.textColor != null ? this.textColor.getColorForState(textPaint.drawableState, this.textColor.getDefaultColor()) : ViewCompat.MEASURED_STATE_MASK);
        textPaint.setShadowLayer(this.shadowRadius, this.shadowDx, this.shadowDy, this.shadowColor != null ? this.shadowColor.getColorForState(textPaint.drawableState, this.shadowColor.getDefaultColor()) : 0);
    }

    public void updateMeasureState(Context context, TextPaint textPaint) {
        Typeface tf = getFont(context);
        textPaint.setTypeface(tf);
        int fake = this.textStyle & (~tf.getStyle());
        textPaint.setFakeBoldText((fake & 1) != 0);
        textPaint.setTextSkewX((fake & 2) != 0 ? -0.25f : 0.0f);
        textPaint.setTextSize(this.textSize);
    }
}
