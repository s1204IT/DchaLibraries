package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class AbsoluteSizeSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final boolean mDip;
    private final int mSize;

    public AbsoluteSizeSpan(int i) {
        this(i, false);
    }

    public AbsoluteSizeSpan(int i, boolean z) {
        this.mSize = i;
        this.mDip = z;
    }

    public AbsoluteSizeSpan(Parcel parcel) {
        this.mSize = parcel.readInt();
        this.mDip = parcel.readInt() != 0;
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 16;
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
        parcel.writeInt(this.mSize);
        parcel.writeInt(this.mDip ? 1 : 0);
    }

    public int getSize() {
        return this.mSize;
    }

    public boolean getDip() {
        return this.mDip;
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        if (this.mDip) {
            textPaint.setTextSize(this.mSize * textPaint.density);
        } else {
            textPaint.setTextSize(this.mSize);
        }
    }

    @Override
    public void updateMeasureState(TextPaint textPaint) {
        if (this.mDip) {
            textPaint.setTextSize(this.mSize * textPaint.density);
        } else {
            textPaint.setTextSize(this.mSize);
        }
    }
}
