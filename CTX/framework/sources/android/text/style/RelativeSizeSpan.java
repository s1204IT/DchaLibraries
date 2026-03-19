package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class RelativeSizeSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final float mProportion;

    public RelativeSizeSpan(float f) {
        this.mProportion = f;
    }

    public RelativeSizeSpan(Parcel parcel) {
        this.mProportion = parcel.readFloat();
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 3;
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

    public float getSizeChange() {
        return this.mProportion;
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        textPaint.setTextSize(textPaint.getTextSize() * this.mProportion);
    }

    @Override
    public void updateMeasureState(TextPaint textPaint) {
        textPaint.setTextSize(textPaint.getTextSize() * this.mProportion);
    }
}
