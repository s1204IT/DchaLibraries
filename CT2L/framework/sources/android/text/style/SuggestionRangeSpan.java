package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class SuggestionRangeSpan extends CharacterStyle implements ParcelableSpan {
    private int mBackgroundColor;

    public SuggestionRangeSpan() {
        this.mBackgroundColor = 0;
    }

    public SuggestionRangeSpan(Parcel src) {
        this.mBackgroundColor = src.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mBackgroundColor);
    }

    @Override
    public int getSpanTypeId() {
        return 21;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.mBackgroundColor = backgroundColor;
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        tp.bgColor = this.mBackgroundColor;
    }
}
