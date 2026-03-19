package android.view.inputmethod;

import android.os.Parcel;
import android.os.Parcelable;

public class ExtractedTextRequest implements Parcelable {
    public static final Parcelable.Creator<ExtractedTextRequest> CREATOR = new Parcelable.Creator<ExtractedTextRequest>() {
        @Override
        public ExtractedTextRequest createFromParcel(Parcel parcel) {
            ExtractedTextRequest extractedTextRequest = new ExtractedTextRequest();
            extractedTextRequest.token = parcel.readInt();
            extractedTextRequest.flags = parcel.readInt();
            extractedTextRequest.hintMaxLines = parcel.readInt();
            extractedTextRequest.hintMaxChars = parcel.readInt();
            return extractedTextRequest;
        }

        @Override
        public ExtractedTextRequest[] newArray(int i) {
            return new ExtractedTextRequest[i];
        }
    };
    public int flags;
    public int hintMaxChars;
    public int hintMaxLines;
    public int token;

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.token);
        parcel.writeInt(this.flags);
        parcel.writeInt(this.hintMaxLines);
        parcel.writeInt(this.hintMaxChars);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
