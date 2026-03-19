package android.view.textservice;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.ArrayUtils;

public final class SuggestionsInfo implements Parcelable {
    public static final int RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS = 4;
    public static final int RESULT_ATTR_IN_THE_DICTIONARY = 1;
    public static final int RESULT_ATTR_LOOKS_LIKE_TYPO = 2;
    private int mCookie;
    private int mSequence;
    private final String[] mSuggestions;
    private final int mSuggestionsAttributes;
    private final boolean mSuggestionsAvailable;
    private static final String[] EMPTY = (String[]) ArrayUtils.emptyArray(String.class);
    public static final Parcelable.Creator<SuggestionsInfo> CREATOR = new Parcelable.Creator<SuggestionsInfo>() {
        @Override
        public SuggestionsInfo createFromParcel(Parcel parcel) {
            return new SuggestionsInfo(parcel);
        }

        @Override
        public SuggestionsInfo[] newArray(int i) {
            return new SuggestionsInfo[i];
        }
    };

    public SuggestionsInfo(int i, String[] strArr) {
        this(i, strArr, 0, 0);
    }

    public SuggestionsInfo(int i, String[] strArr, int i2, int i3) {
        if (strArr == null) {
            this.mSuggestions = EMPTY;
            this.mSuggestionsAvailable = false;
        } else {
            this.mSuggestions = strArr;
            this.mSuggestionsAvailable = true;
        }
        this.mSuggestionsAttributes = i;
        this.mCookie = i2;
        this.mSequence = i3;
    }

    public SuggestionsInfo(Parcel parcel) {
        this.mSuggestionsAttributes = parcel.readInt();
        this.mSuggestions = parcel.readStringArray();
        this.mCookie = parcel.readInt();
        this.mSequence = parcel.readInt();
        this.mSuggestionsAvailable = parcel.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSuggestionsAttributes);
        parcel.writeStringArray(this.mSuggestions);
        parcel.writeInt(this.mCookie);
        parcel.writeInt(this.mSequence);
        parcel.writeInt(this.mSuggestionsAvailable ? 1 : 0);
    }

    public void setCookieAndSequence(int i, int i2) {
        this.mCookie = i;
        this.mSequence = i2;
    }

    public int getCookie() {
        return this.mCookie;
    }

    public int getSequence() {
        return this.mSequence;
    }

    public int getSuggestionsAttributes() {
        return this.mSuggestionsAttributes;
    }

    public int getSuggestionsCount() {
        if (!this.mSuggestionsAvailable) {
            return -1;
        }
        return this.mSuggestions.length;
    }

    public String getSuggestionAt(int i) {
        return this.mSuggestions[i];
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
