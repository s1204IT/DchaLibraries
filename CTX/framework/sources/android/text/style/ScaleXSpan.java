package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class ScaleXSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final float mProportion;

    public ScaleXSpan(float f) {
        this.mProportion = f;
    }

    public ScaleXSpan(Parcel parcel) {
        this.mProportion = parcel.readFloat();
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 4;
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
        parcel.writeFloat(this.mProportion);
    }

    public float getScaleX() {
        return this.mProportion;
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        textPaint.setTextScaleX(textPaint.getTextScaleX() * this.mProportion);
    }

    @Override
    public void updateMeasureState(TextPaint textPaint) {
        textPaint.setTextScaleX(textPaint.getTextScaleX() * this.mProportion);
    }
}
