package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Slog;
import android.view.autofill.AutofillValue;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextValueSanitizer extends InternalSanitizer implements Sanitizer, Parcelable {
    public static final Parcelable.Creator<TextValueSanitizer> CREATOR = new Parcelable.Creator<TextValueSanitizer>() {
        @Override
        public TextValueSanitizer createFromParcel(Parcel parcel) {
            return new TextValueSanitizer((Pattern) parcel.readSerializable(), parcel.readString());
        }

        @Override
        public TextValueSanitizer[] newArray(int i) {
            return new TextValueSanitizer[i];
        }
    };
    private static final String TAG = "TextValueSanitizer";
    private final Pattern mRegex;
    private final String mSubst;

    public TextValueSanitizer(Pattern pattern, String str) {
        this.mRegex = (Pattern) Preconditions.checkNotNull(pattern);
        this.mSubst = (String) Preconditions.checkNotNull(str);
    }

    @Override
    public AutofillValue sanitize(AutofillValue autofillValue) {
        if (autofillValue == null) {
            Slog.w(TAG, "sanitize() called with null value");
            return null;
        }
        if (!autofillValue.isText()) {
            if (Helper.sDebug) {
                Slog.d(TAG, "sanitize() called with non-text value: " + autofillValue);
            }
            return null;
        }
        try {
            Matcher matcher = this.mRegex.matcher(autofillValue.getTextValue());
            if (!matcher.matches()) {
                if (Helper.sDebug) {
                    Slog.d(TAG, "sanitize(): " + this.mRegex + " failed for " + autofillValue);
                }
                return null;
            }
            return AutofillValue.forText(matcher.replaceAll(this.mSubst));
        } catch (Exception e) {
            Slog.w(TAG, "Exception evaluating " + this.mRegex + "/" + this.mSubst + ": " + e);
            return null;
        }
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        return "TextValueSanitizer: [regex=" + this.mRegex + ", subst=" + this.mSubst + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(this.mRegex);
        parcel.writeString(this.mSubst);
    }
}
