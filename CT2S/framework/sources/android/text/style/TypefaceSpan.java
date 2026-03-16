package android.text.style;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class TypefaceSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final String mFamily;

    public TypefaceSpan(String family) {
        this.mFamily = family;
    }

    public TypefaceSpan(Parcel src) {
        this.mFamily = src.readString();
    }

    @Override
    public int getSpanTypeId() {
        return 13;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mFamily);
    }

    public String getFamily() {
        return this.mFamily;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        apply(ds, this.mFamily);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        apply(paint, this.mFamily);
    }

    private static void apply(Paint paint, String family) {
        int oldStyle;
        Typeface old = paint.getTypeface();
        if (old == null) {
            oldStyle = 0;
        } else {
            oldStyle = old.getStyle();
        }
        Typeface tf = Typeface.create(family, oldStyle);
        int fake = oldStyle & (tf.getStyle() ^ (-1));
        if ((fake & 1) != 0) {
            paint.setFakeBoldText(true);
        }
        if ((fake & 2) != 0) {
            paint.setTextSkewX(-0.25f);
        }
        paint.setTypeface(tf);
    }
}
