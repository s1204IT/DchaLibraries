package android.text.style;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import com.android.internal.R;

public class TextAppearanceSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final int mStyle;
    private final ColorStateList mTextColor;
    private final ColorStateList mTextColorLink;
    private final int mTextSize;
    private final String mTypeface;

    public TextAppearanceSpan(Context context, int appearance) {
        this(context, appearance, -1);
    }

    public TextAppearanceSpan(Context context, int appearance, int colorList) {
        TypedArray a = context.obtainStyledAttributes(appearance, R.styleable.TextAppearance);
        ColorStateList textColor = a.getColorStateList(3);
        this.mTextColorLink = a.getColorStateList(6);
        this.mTextSize = a.getDimensionPixelSize(0, -1);
        this.mStyle = a.getInt(2, 0);
        String family = a.getString(12);
        if (family != null) {
            this.mTypeface = family;
        } else {
            int tf = a.getInt(1, 0);
            switch (tf) {
                case 1:
                    this.mTypeface = "sans";
                    break;
                case 2:
                    this.mTypeface = "serif";
                    break;
                case 3:
                    this.mTypeface = "monospace";
                    break;
                default:
                    this.mTypeface = null;
                    break;
            }
        }
        a.recycle();
        if (colorList >= 0) {
            TypedArray a2 = context.obtainStyledAttributes(16973829, R.styleable.Theme);
            textColor = a2.getColorStateList(colorList);
            a2.recycle();
        }
        this.mTextColor = textColor;
    }

    public TextAppearanceSpan(String family, int style, int size, ColorStateList color, ColorStateList linkColor) {
        this.mTypeface = family;
        this.mStyle = style;
        this.mTextSize = size;
        this.mTextColor = color;
        this.mTextColorLink = linkColor;
    }

    public TextAppearanceSpan(Parcel src) {
        this.mTypeface = src.readString();
        this.mStyle = src.readInt();
        this.mTextSize = src.readInt();
        if (src.readInt() != 0) {
            this.mTextColor = ColorStateList.CREATOR.createFromParcel(src);
        } else {
            this.mTextColor = null;
        }
        if (src.readInt() != 0) {
            this.mTextColorLink = ColorStateList.CREATOR.createFromParcel(src);
        } else {
            this.mTextColorLink = null;
        }
    }

    @Override
    public int getSpanTypeId() {
        return 17;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mTypeface);
        dest.writeInt(this.mStyle);
        dest.writeInt(this.mTextSize);
        if (this.mTextColor != null) {
            dest.writeInt(1);
            this.mTextColor.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        if (this.mTextColorLink != null) {
            dest.writeInt(1);
            this.mTextColorLink.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
    }

    public String getFamily() {
        return this.mTypeface;
    }

    public ColorStateList getTextColor() {
        return this.mTextColor;
    }

    public ColorStateList getLinkTextColor() {
        return this.mTextColorLink;
    }

    public int getTextSize() {
        return this.mTextSize;
    }

    public int getTextStyle() {
        return this.mStyle;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        updateMeasureState(ds);
        if (this.mTextColor != null) {
            ds.setColor(this.mTextColor.getColorForState(ds.drawableState, 0));
        }
        if (this.mTextColorLink != null) {
            ds.linkColor = this.mTextColorLink.getColorForState(ds.drawableState, 0);
        }
    }

    @Override
    public void updateMeasureState(TextPaint ds) {
        Typeface tf;
        if (this.mTypeface != null || this.mStyle != 0) {
            Typeface tf2 = ds.getTypeface();
            int style = 0;
            if (tf2 != null) {
                style = tf2.getStyle();
            }
            int style2 = style | this.mStyle;
            if (this.mTypeface != null) {
                tf = Typeface.create(this.mTypeface, style2);
            } else if (tf2 == null) {
                tf = Typeface.defaultFromStyle(style2);
            } else {
                tf = Typeface.create(tf2, style2);
            }
            int fake = style2 & (tf.getStyle() ^ (-1));
            if ((fake & 1) != 0) {
                ds.setFakeBoldText(true);
            }
            if ((fake & 2) != 0) {
                ds.setTextSkewX(-0.25f);
            }
            ds.setTypeface(tf);
        }
        if (this.mTextSize > 0) {
            ds.setTextSize(this.mTextSize);
        }
    }
}
