package android.view.textservice;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.SpellCheckSpan;

public final class TextInfo implements Parcelable {
    public static final Parcelable.Creator<TextInfo> CREATOR = new Parcelable.Creator<TextInfo>() {
        @Override
        public TextInfo createFromParcel(Parcel parcel) {
            return new TextInfo(parcel);
        }

        @Override
        public TextInfo[] newArray(int i) {
            return new TextInfo[i];
        }
    };
    private static final int DEFAULT_COOKIE = 0;
    private static final int DEFAULT_SEQUENCE_NUMBER = 0;
    private final CharSequence mCharSequence;
    private final int mCookie;
    private final int mSequenceNumber;

    public TextInfo(String str) {
        this(str, 0, getStringLengthOrZero(str), 0, 0);
    }

    public TextInfo(String str, int i, int i2) {
        this(str, 0, getStringLengthOrZero(str), i, i2);
    }

    private static int getStringLengthOrZero(String str) {
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        return str.length();
    }

    public TextInfo(CharSequence charSequence, int i, int i2, int i3, int i4) {
        if (TextUtils.isEmpty(charSequence)) {
            throw new IllegalArgumentException("charSequence is empty");
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(charSequence, i, i2);
        for (SpellCheckSpan spellCheckSpan : (SpellCheckSpan[]) spannableStringBuilder.getSpans(0, spannableStringBuilder.length(), SpellCheckSpan.class)) {
            spannableStringBuilder.removeSpan(spellCheckSpan);
        }
        this.mCharSequence = spannableStringBuilder;
        this.mCookie = i3;
        this.mSequenceNumber = i4;
    }

    public TextInfo(Parcel parcel) {
        this.mCharSequence = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.mCookie = parcel.readInt();
        this.mSequenceNumber = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        TextUtils.writeToParcel(this.mCharSequence, parcel, i);
        parcel.writeInt(this.mCookie);
        parcel.writeInt(this.mSequenceNumber);
    }

    public String getText() {
        if (this.mCharSequence == null) {
            return null;
        }
        return this.mCharSequence.toString();
    }

    public CharSequence getCharSequence() {
        return this.mCharSequence;
    }

    public int getCookie() {
        return this.mCookie;
    }

    public int getSequence() {
        return this.mSequenceNumber;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
