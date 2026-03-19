package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.text.Layout;
import android.text.ParcelableSpan;

public interface LeadingMarginSpan extends ParagraphStyle {

    public interface LeadingMarginSpan2 extends LeadingMarginSpan, WrapTogetherSpan {
        int getLeadingMarginLineCount();
    }

    void drawLeadingMargin(Canvas canvas, Paint paint, int i, int i2, int i3, int i4, int i5, CharSequence charSequence, int i6, int i7, boolean z, Layout layout);

    int getLeadingMargin(boolean z);

    public static class Standard implements LeadingMarginSpan, ParcelableSpan {
        private final int mFirst;
        private final int mRest;

        public Standard(int i, int i2) {
            this.mFirst = i;
            this.mRest = i2;
        }

        public Standard(int i) {
            this(i, i);
        }

        public Standard(Parcel parcel) {
            this.mFirst = parcel.readInt();
            this.mRest = parcel.readInt();
        }

        @Override
        public int getSpanTypeId() {
            return getSpanTypeIdInternal();
        }

        @Override
        public int getSpanTypeIdInternal() {
            return 10;
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
            parcel.writeInt(this.mFirst);
            parcel.writeInt(this.mRest);
        }

        @Override
        public int getLeadingMargin(boolean z) {
            return z ? this.mFirst : this.mRest;
        }

        @Override
        public void drawLeadingMargin(Canvas canvas, Paint paint, int i, int i2, int i3, int i4, int i5, CharSequence charSequence, int i6, int i7, boolean z, Layout layout) {
        }
    }
}
