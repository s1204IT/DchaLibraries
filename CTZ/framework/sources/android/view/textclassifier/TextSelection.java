package android.view.textclassifier;

import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.view.textclassifier.TextClassifier;
import com.android.internal.util.Preconditions;
import java.util.Locale;
import java.util.Map;

public final class TextSelection implements Parcelable {
    public static final Parcelable.Creator<TextSelection> CREATOR = new Parcelable.Creator<TextSelection>() {
        @Override
        public TextSelection createFromParcel(Parcel parcel) {
            return new TextSelection(parcel);
        }

        @Override
        public TextSelection[] newArray(int i) {
            return new TextSelection[i];
        }
    };
    private final int mEndIndex;
    private final EntityConfidence mEntityConfidence;
    private final String mId;
    private final int mStartIndex;

    private TextSelection(int i, int i2, Map<String, Float> map, String str) {
        this.mStartIndex = i;
        this.mEndIndex = i2;
        this.mEntityConfidence = new EntityConfidence(map);
        this.mId = str;
    }

    public int getSelectionStartIndex() {
        return this.mStartIndex;
    }

    public int getSelectionEndIndex() {
        return this.mEndIndex;
    }

    public int getEntityCount() {
        return this.mEntityConfidence.getEntities().size();
    }

    public String getEntity(int i) {
        return this.mEntityConfidence.getEntities().get(i);
    }

    public float getConfidenceScore(String str) {
        return this.mEntityConfidence.getConfidenceScore(str);
    }

    public String getId() {
        return this.mId;
    }

    public String toString() {
        return String.format(Locale.US, "TextSelection {id=%s, startIndex=%d, endIndex=%d, entities=%s}", this.mId, Integer.valueOf(this.mStartIndex), Integer.valueOf(this.mEndIndex), this.mEntityConfidence);
    }

    public static final class Builder {
        private final int mEndIndex;
        private final Map<String, Float> mEntityConfidence = new ArrayMap();
        private String mId;
        private final int mStartIndex;

        public Builder(int i, int i2) {
            Preconditions.checkArgument(i >= 0);
            Preconditions.checkArgument(i2 > i);
            this.mStartIndex = i;
            this.mEndIndex = i2;
        }

        public Builder setEntityType(String str, float f) {
            Preconditions.checkNotNull(str);
            this.mEntityConfidence.put(str, Float.valueOf(f));
            return this;
        }

        public Builder setId(String str) {
            this.mId = str;
            return this;
        }

        public TextSelection build() {
            return new TextSelection(this.mStartIndex, this.mEndIndex, this.mEntityConfidence, this.mId);
        }
    }

    public static final class Request implements Parcelable {
        public static final Parcelable.Creator<Request> CREATOR = new Parcelable.Creator<Request>() {
            @Override
            public Request createFromParcel(Parcel parcel) {
                return new Request(parcel);
            }

            @Override
            public Request[] newArray(int i) {
                return new Request[i];
            }
        };
        private final boolean mDarkLaunchAllowed;
        private final LocaleList mDefaultLocales;
        private final int mEndIndex;
        private final int mStartIndex;
        private final CharSequence mText;

        private Request(CharSequence charSequence, int i, int i2, LocaleList localeList, boolean z) {
            this.mText = charSequence;
            this.mStartIndex = i;
            this.mEndIndex = i2;
            this.mDefaultLocales = localeList;
            this.mDarkLaunchAllowed = z;
        }

        public CharSequence getText() {
            return this.mText;
        }

        public int getStartIndex() {
            return this.mStartIndex;
        }

        public int getEndIndex() {
            return this.mEndIndex;
        }

        public boolean isDarkLaunchAllowed() {
            return this.mDarkLaunchAllowed;
        }

        public LocaleList getDefaultLocales() {
            return this.mDefaultLocales;
        }

        public static final class Builder {
            private boolean mDarkLaunchAllowed;
            private LocaleList mDefaultLocales;
            private final int mEndIndex;
            private final int mStartIndex;
            private final CharSequence mText;

            public Builder(CharSequence charSequence, int i, int i2) {
                TextClassifier.Utils.checkArgument(charSequence, i, i2);
                this.mText = charSequence;
                this.mStartIndex = i;
                this.mEndIndex = i2;
            }

            public Builder setDefaultLocales(LocaleList localeList) {
                this.mDefaultLocales = localeList;
                return this;
            }

            public Builder setDarkLaunchAllowed(boolean z) {
                this.mDarkLaunchAllowed = z;
                return this;
            }

            public Request build() {
                return new Request(this.mText, this.mStartIndex, this.mEndIndex, this.mDefaultLocales, this.mDarkLaunchAllowed);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.mText.toString());
            parcel.writeInt(this.mStartIndex);
            parcel.writeInt(this.mEndIndex);
            parcel.writeInt(this.mDefaultLocales != null ? 1 : 0);
            if (this.mDefaultLocales != null) {
                this.mDefaultLocales.writeToParcel(parcel, i);
            }
        }

        private Request(Parcel parcel) {
            this.mText = parcel.readString();
            this.mStartIndex = parcel.readInt();
            this.mEndIndex = parcel.readInt();
            this.mDefaultLocales = parcel.readInt() == 0 ? null : LocaleList.CREATOR.createFromParcel(parcel);
            this.mDarkLaunchAllowed = false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mStartIndex);
        parcel.writeInt(this.mEndIndex);
        this.mEntityConfidence.writeToParcel(parcel, i);
        parcel.writeString(this.mId);
    }

    private TextSelection(Parcel parcel) {
        this.mStartIndex = parcel.readInt();
        this.mEndIndex = parcel.readInt();
        this.mEntityConfidence = EntityConfidence.CREATOR.createFromParcel(parcel);
        this.mId = parcel.readString();
    }

    public static final class Options {
        private boolean mDarkLaunchAllowed;
        private LocaleList mDefaultLocales;
        private final Request mRequest;
        private final TextClassificationSessionId mSessionId;

        public Options() {
            this(null, null);
        }

        private Options(TextClassificationSessionId textClassificationSessionId, Request request) {
            this.mSessionId = textClassificationSessionId;
            this.mRequest = request;
        }

        public static Options from(TextClassificationSessionId textClassificationSessionId, Request request) {
            Options options = new Options(textClassificationSessionId, request);
            options.setDefaultLocales(request.getDefaultLocales());
            return options;
        }

        public Options setDefaultLocales(LocaleList localeList) {
            this.mDefaultLocales = localeList;
            return this;
        }

        public LocaleList getDefaultLocales() {
            return this.mDefaultLocales;
        }

        public Request getRequest() {
            return this.mRequest;
        }

        public TextClassificationSessionId getSessionId() {
            return this.mSessionId;
        }
    }
}
