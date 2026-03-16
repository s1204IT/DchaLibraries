package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class ForegroundColorSpan extends CharacterStyle implements UpdateAppearance, ParcelableSpan {
    private final int mColor;

    public ForegroundColorSpan(int color) {
        this.mColor = color;
    }

    public ForegroundColorSpan(Parcel src) {
        this.mColor = src.readInt();
    }

    @Override
    public int getSpanTypeId() {
        return 2;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mColor);
    }

    public int getForegroundColor() {
        return this.mColor;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(this.mColor);
    }
}
