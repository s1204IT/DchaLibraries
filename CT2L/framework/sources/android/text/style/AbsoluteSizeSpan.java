package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class AbsoluteSizeSpan extends MetricAffectingSpan implements ParcelableSpan {
    private boolean mDip;
    private final int mSize;

    public AbsoluteSizeSpan(int size) {
        this.mSize = size;
    }

    public AbsoluteSizeSpan(int size, boolean dip) {
        this.mSize = size;
        this.mDip = dip;
    }

    public AbsoluteSizeSpan(Parcel src) {
        this.mSize = src.readInt();
        this.mDip = src.readInt() != 0;
    }

    @Override
    public int getSpanTypeId() {
        return 16;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSize);
        dest.writeInt(this.mDip ? 1 : 0);
    }

    public int getSize() {
        return this.mSize;
    }

    public boolean getDip() {
        return this.mDip;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        if (this.mDip) {
            ds.setTextSize(this.mSize * ds.density);
        } else {
            ds.setTextSize(this.mSize);
        }
    }

    @Override
    public void updateMeasureState(TextPaint ds) {
        if (this.mDip) {
            ds.setTextSize(this.mSize * ds.density);
        } else {
            ds.setTextSize(this.mSize);
        }
    }
}
