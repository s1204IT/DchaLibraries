package android.text.style;

import android.os.Parcel;
import android.text.Layout;
import android.text.ParcelableSpan;

public interface AlignmentSpan extends ParagraphStyle {
    Layout.Alignment getAlignment();

    public static class Standard implements AlignmentSpan, ParcelableSpan {
        private final Layout.Alignment mAlignment;

        public Standard(Layout.Alignment alignment) {
            this.mAlignment = alignment;
        }

        public Standard(Parcel parcel) {
            this.mAlignment = Layout.Alignment.valueOf(parcel.readString());
        }

        @Override
        public int getSpanTypeId() {
            return getSpanTypeIdInternal();
        }

        @Override
        public int getSpanTypeIdInternal() {
            return 1;
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
            parcel.writeString(this.mAlignment.name());
        }

        @Override
        public Layout.Alignment getAlignment() {
            return this.mAlignment;
        }
    }
}
