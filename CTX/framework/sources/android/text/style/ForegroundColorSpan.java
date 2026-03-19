package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class ForegroundColorSpan extends CharacterStyle implements UpdateAppearance, ParcelableSpan {
    private final int mColor;

    public ForegroundColorSpan(int i) {
        this.mColor = i;
    }

    public ForegroundColorSpan(Parcel parcel) {
        this.mColor = parcel.readInt();
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 2;
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
        parcel.writeInt(this.mColor);
    }

    public int getForegroundColor() {
        return this.mColor;
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        textPaint.setColor(this.mColor);
    }
}
