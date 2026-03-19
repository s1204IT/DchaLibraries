package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class UnderlineSpan extends CharacterStyle implements UpdateAppearance, ParcelableSpan {
    public UnderlineSpan() {
    }

    public UnderlineSpan(Parcel parcel) {
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 6;
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
        textPaint.setUnderlineText(true);
    }
}
