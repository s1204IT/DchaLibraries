package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class SuperscriptSpan extends MetricAffectingSpan implements ParcelableSpan {
    public SuperscriptSpan() {
    }

    public SuperscriptSpan(Parcel parcel) {
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 14;
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
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        textPaint.baselineShift += (int) (textPaint.ascent() / 2.0f);
    }

    @Override
    public void updateMeasureState(TextPaint textPaint) {
        textPaint.baselineShift += (int) (textPaint.ascent() / 2.0f);
    }
}
