package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class SuperscriptSpan extends MetricAffectingSpan implements ParcelableSpan {
    public SuperscriptSpan() {
    }

    public SuperscriptSpan(Parcel src) {
    }

    @Override
    public int getSpanTypeId() {
        return 14;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        tp.baselineShift += (int) (tp.ascent() / 2.0f);
    }

    @Override
    public void updateMeasureState(TextPaint tp) {
        tp.baselineShift += (int) (tp.ascent() / 2.0f);
    }
}
