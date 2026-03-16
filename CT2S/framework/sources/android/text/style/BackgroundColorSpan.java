package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class BackgroundColorSpan extends CharacterStyle implements UpdateAppearance, ParcelableSpan {
    private final int mColor;

    public BackgroundColorSpan(int color) {
        this.mColor = color;
    }

    public BackgroundColorSpan(Parcel src) {
        this.mColor = src.readInt();
    }

    @Override
    public int getSpanTypeId() {
        return 12;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mColor);
    }

    public int getBackgroundColor() {
        return this.mColor;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.bgColor = this.mColor;
    }
}
