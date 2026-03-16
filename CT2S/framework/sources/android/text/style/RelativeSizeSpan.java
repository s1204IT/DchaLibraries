package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class RelativeSizeSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final float mProportion;

    public RelativeSizeSpan(float proportion) {
        this.mProportion = proportion;
    }

    public RelativeSizeSpan(Parcel src) {
        this.mProportion = src.readFloat();
    }

    @Override
    public int getSpanTypeId() {
        return 3;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.mProportion);
    }

    public float getSizeChange() {
        return this.mProportion;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setTextSize(ds.getTextSize() * this.mProportion);
    }

    @Override
    public void updateMeasureState(TextPaint ds) {
        ds.setTextSize(ds.getTextSize() * this.mProportion);
    }
}
