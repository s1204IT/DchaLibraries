package android.text.style;

import android.graphics.LeakyTypefaceStorage;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class TypefaceSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final String mFamily;
    private final Typeface mTypeface;

    public TypefaceSpan(String str) {
        this(str, null);
    }

    public TypefaceSpan(Typeface typeface) {
        this(null, typeface);
    }

    public TypefaceSpan(Parcel parcel) {
        this.mFamily = parcel.readString();
        this.mTypeface = LeakyTypefaceStorage.readTypefaceFromParcel(parcel);
    }

    private TypefaceSpan(String str, Typeface typeface) {
        this.mFamily = str;
        this.mTypeface = typeface;
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 13;
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
        parcel.writeString(this.mFamily);
        LeakyTypefaceStorage.writeTypefaceToParcel(this.mTypeface, parcel);
    }

    public String getFamily() {
        return this.mFamily;
    }

    public Typeface getTypeface() {
        return this.mTypeface;
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        updateTypeface(textPaint);
    }

    @Override
    public void updateMeasureState(TextPaint textPaint) {
        updateTypeface(textPaint);
    }

    private void updateTypeface(Paint paint) {
        if (this.mTypeface != null) {
            paint.setTypeface(this.mTypeface);
        } else if (this.mFamily != null) {
            applyFontFamily(paint, this.mFamily);
        }
    }

    private void applyFontFamily(Paint paint, String str) {
        int style;
        Typeface typeface = paint.getTypeface();
        if (typeface == null) {
            style = 0;
        } else {
            style = typeface.getStyle();
        }
        Typeface typefaceCreate = Typeface.create(str, style);
        int i = style & (~typefaceCreate.getStyle());
        if ((i & 1) != 0) {
            paint.setFakeBoldText(true);
        }
        if ((i & 2) != 0) {
            paint.setTextSkewX(-0.25f);
        }
        paint.setTypeface(typefaceCreate);
    }
}
