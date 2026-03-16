package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class UnderlineSpan extends CharacterStyle implements UpdateAppearance, ParcelableSpan {
    public UnderlineSpan() {
    }

    public UnderlineSpan(Parcel src) {
    }

    @Override
    public int getSpanTypeId() {
        return 6;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setUnderlineText(true);
    }
}
