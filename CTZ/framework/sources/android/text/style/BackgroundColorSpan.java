package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class BackgroundColorSpan extends CharacterStyle implements UpdateAppearance, ParcelableSpan {
    private final int mColor;

    public BackgroundColorSpan(int i) {
        this.mColor = i;
    }

    public BackgroundColorSpan(Parcel parcel) {
        this.mColor = parcel.readInt();
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 12;
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

    public int getBackgroundColor() {
        return this.mColor;
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        textPaint.bgColor = this.mColor;
    }
}
