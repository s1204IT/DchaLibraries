package android.text.style;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class StyleSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final int mStyle;

    public StyleSpan(int i) {
        this.mStyle = i;
    }

    public StyleSpan(Parcel parcel) {
        this.mStyle = parcel.readInt();
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 7;
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
        parcel.writeInt(this.mStyle);
    }

    public int getStyle() {
        return this.mStyle;
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        apply(textPaint, this.mStyle);
    }

    @Override
    public void updateMeasureState(TextPaint textPaint) {
        apply(textPaint, this.mStyle);
    }

    private static void apply(Paint paint, int i) {
        int style;
        Typeface typefaceCreate;
        Typeface typeface = paint.getTypeface();
        if (typeface == null) {
            style = 0;
        } else {
            style = typeface.getStyle();
        }
        int i2 = i | style;
        if (typeface == null) {
            typefaceCreate = Typeface.defaultFromStyle(i2);
        } else {
            typefaceCreate = Typeface.create(typeface, i2);
        }
        int i3 = i2 & (~typefaceCreate.getStyle());
        if ((i3 & 1) != 0) {
            paint.setFakeBoldText(true);
        }
        if ((i3 & 2) != 0) {
            paint.setTextSkewX(-0.25f);
        }
        paint.setTypeface(typefaceCreate);
    }
}
