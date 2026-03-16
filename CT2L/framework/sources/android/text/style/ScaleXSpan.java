package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class ScaleXSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final float mProportion;

    public ScaleXSpan(float proportion) {
        this.mProportion = proportion;
    }

    public ScaleXSpan(Parcel src) {
        this.mProportion = src.readFloat();
    }

    @Override
    public int getSpanTypeId() {
        return 4;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.mProportion);
    }

    public float getScaleX() {
        return this.mProportion;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setTextScaleX(ds.getTextScaleX() * this.mProportion);
    }

    @Override
    public void updateMeasureState(TextPaint ds) {
        ds.setTextScaleX(ds.getTextScaleX() * this.mProportion);
    }
}
