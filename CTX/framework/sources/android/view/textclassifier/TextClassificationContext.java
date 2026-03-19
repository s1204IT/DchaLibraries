package android.view.textclassifier;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
import java.util.Locale;

public final class TextClassificationContext implements Parcelable {
    public static final Parcelable.Creator<TextClassificationContext> CREATOR = new Parcelable.Creator<TextClassificationContext>() {
        @Override
        public TextClassificationContext createFromParcel(Parcel parcel) {
            return new TextClassificationContext(parcel);
        }

        @Override
        public TextClassificationContext[] newArray(int i) {
            return new TextClassificationContext[i];
        }
    };
    private final String mPackageName;
    private final String mWidgetType;
    private final String mWidgetVersion;

    private TextClassificationContext(String str, String str2, String str3) {
        this.mPackageName = (String) Preconditions.checkNotNull(str);
        this.mWidgetType = (String) Preconditions.checkNotNull(str2);
        this.mWidgetVersion = str3;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getWidgetType() {
        return this.mWidgetType;
    }

    public String getWidgetVersion() {
        return this.mWidgetVersion;
    }

    public String toString() {
        return String.format(Locale.US, "TextClassificationContext{packageName=%s, widgetType=%s, widgetVersion=%s}", this.mPackageName, this.mWidgetType, this.mWidgetVersion);
    }

    public static final class Builder {
        private final String mPackageName;
        private final String mWidgetType;
        private String mWidgetVersion;

        public Builder(String str, String str2) {
            this.mPackageName = (String) Preconditions.checkNotNull(str);
            this.mWidgetType = (String) Preconditions.checkNotNull(str2);
        }

        public Builder setWidgetVersion(String str) {
            this.mWidgetVersion = str;
            return this;
        }

        public TextClassificationContext build() {
            return new TextClassificationContext(this.mPackageName, this.mWidgetType, this.mWidgetVersion);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mPackageName);
        parcel.writeString(this.mWidgetType);
        parcel.writeString(this.mWidgetVersion);
    }

    private TextClassificationContext(Parcel parcel) {
        this.mPackageName = parcel.readString();
        this.mWidgetType = parcel.readString();
        this.mWidgetVersion = parcel.readString();
    }
}
