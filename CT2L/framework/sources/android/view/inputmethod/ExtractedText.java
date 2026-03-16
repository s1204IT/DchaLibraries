package android.view.inputmethod;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class ExtractedText implements Parcelable {
    public static final Parcelable.Creator<ExtractedText> CREATOR = new Parcelable.Creator<ExtractedText>() {
        @Override
        public ExtractedText createFromParcel(Parcel source) {
            ExtractedText res = new ExtractedText();
            res.text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            res.startOffset = source.readInt();
            res.partialStartOffset = source.readInt();
            res.partialEndOffset = source.readInt();
            res.selectionStart = source.readInt();
            res.selectionEnd = source.readInt();
            res.flags = source.readInt();
            return res;
        }

        @Override
        public ExtractedText[] newArray(int size) {
            return new ExtractedText[size];
        }
    };
    public static final int FLAG_SELECTING = 2;
    public static final int FLAG_SINGLE_LINE = 1;
    public int flags;
    public int partialEndOffset;
    public int partialStartOffset;
    public int selectionEnd;
    public int selectionStart;
    public int startOffset;
    public CharSequence text;

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        TextUtils.writeToParcel(this.text, dest, flags);
        dest.writeInt(this.startOffset);
        dest.writeInt(this.partialStartOffset);
        dest.writeInt(this.partialEndOffset);
        dest.writeInt(this.selectionStart);
        dest.writeInt(this.selectionEnd);
        dest.writeInt(this.flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
