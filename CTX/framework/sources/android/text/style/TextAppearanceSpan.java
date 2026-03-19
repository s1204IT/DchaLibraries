package android.text.style;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.LeakyTypefaceStorage;
import android.graphics.Typeface;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import com.android.internal.R;

public class TextAppearanceSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final String mFamilyName;
    private final int mStyle;
    private final ColorStateList mTextColor;
    private final ColorStateList mTextColorLink;
    private final int mTextSize;
    private final Typeface mTypeface;

    public TextAppearanceSpan(Context context, int i) {
        this(context, i, -1);
    }

    public TextAppearanceSpan(Context context, int i, int i2) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(i, R.styleable.TextAppearance);
        ColorStateList colorStateList = typedArrayObtainStyledAttributes.getColorStateList(3);
        this.mTextColorLink = typedArrayObtainStyledAttributes.getColorStateList(6);
        this.mTextSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(0, -1);
        this.mStyle = typedArrayObtainStyledAttributes.getInt(2, 0);
        if (!context.isRestricted() && context.canLoadUnsafeResources()) {
            this.mTypeface = typedArrayObtainStyledAttributes.getFont(12);
        } else {
            this.mTypeface = null;
        }
        if (this.mTypeface != null) {
            this.mFamilyName = null;
        } else {
            String string = typedArrayObtainStyledAttributes.getString(12);
            if (string != null) {
                this.mFamilyName = string;
            } else {
                switch (typedArrayObtainStyledAttributes.getInt(1, 0)) {
                    case 1:
                        this.mFamilyName = "sans";
                        break;
                    case 2:
                        this.mFamilyName = "serif";
                        break;
                    case 3:
                        this.mFamilyName = "monospace";
                        break;
                    default:
                        this.mFamilyName = null;
                        break;
                }
            }
        }
        typedArrayObtainStyledAttributes.recycle();
        if (i2 >= 0) {
            TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(16973829, R.styleable.Theme);
            colorStateList = typedArrayObtainStyledAttributes2.getColorStateList(i2);
            typedArrayObtainStyledAttributes2.recycle();
        }
        this.mTextColor = colorStateList;
    }

    public TextAppearanceSpan(String str, int i, int i2, ColorStateList colorStateList, ColorStateList colorStateList2) {
        this.mFamilyName = str;
        this.mStyle = i;
        this.mTextSize = i2;
        this.mTextColor = colorStateList;
        this.mTextColorLink = colorStateList2;
        this.mTypeface = null;
    }

    public TextAppearanceSpan(Parcel parcel) {
        this.mFamilyName = parcel.readString();
        this.mStyle = parcel.readInt();
        this.mTextSize = parcel.readInt();
        if (parcel.readInt() != 0) {
            this.mTextColor = ColorStateList.CREATOR.createFromParcel(parcel);
        } else {
            this.mTextColor = null;
        }
        if (parcel.readInt() != 0) {
            this.mTextColorLink = ColorStateList.CREATOR.createFromParcel(parcel);
        } else {
            this.mTextColorLink = null;
        }
        this.mTypeface = LeakyTypefaceStorage.readTypefaceFromParcel(parcel);
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 17;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        writeToParcelInternal(parcel, i);
    }

    @Override
    public void writeToParcelInternal(Parcel parcel, int i) {
        parcel.writeString(this.mFamilyName);
        parcel.writeInt(this.mStyle);
        parcel.writeInt(this.mTextSize);
        if (this.mTextColor != null) {
            parcel.writeInt(1);
            this.mTextColor.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        if (this.mTextColorLink != null) {
            parcel.writeInt(1);
            this.mTextColorLink.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        LeakyTypefaceStorage.writeTypefaceToParcel(this.mTypeface, parcel);
    }

    public String getFamily() {
        return this.mFamilyName;
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
    public void updateDrawState(TextPaint textPaint) {
        updateMeasureState(textPaint);
        if (this.mTextColor != null) {
            textPaint.setColor(this.mTextColor.getColorForState(textPaint.drawableState, 0));
        }
        if (this.mTextColorLink != null) {
            textPaint.linkColor = this.mTextColorLink.getColorForState(textPaint.drawableState, 0);
        }
    }

    @Override
    public void updateMeasureState(TextPaint textPaint) {
        Typeface typefaceCreate;
        if (this.mTypeface != null) {
            style = this.mStyle;
            typefaceCreate = Typeface.create(this.mTypeface, style);
        } else if (this.mFamilyName != null || this.mStyle != 0) {
            Typeface typeface = textPaint.getTypeface();
            style = (typeface != null ? typeface.getStyle() : 0) | this.mStyle;
            if (this.mFamilyName != null) {
                typefaceCreate = Typeface.create(this.mFamilyName, style);
            } else if (typeface == null) {
                typefaceCreate = Typeface.defaultFromStyle(style);
            } else {
                typefaceCreate = Typeface.create(typeface, style);
            }
        } else {
            typefaceCreate = null;
        }
        if (typefaceCreate != null) {
            int i = style & (~typefaceCreate.getStyle());
            if ((i & 1) != 0) {
                textPaint.setFakeBoldText(true);
            }
            if ((i & 2) != 0) {
                textPaint.setTextSkewX(-0.25f);
            }
            textPaint.setTypeface(typefaceCreate);
        }
        if (this.mTextSize > 0) {
            textPaint.setTextSize(this.mTextSize);
        }
    }
}
