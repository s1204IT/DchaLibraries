package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;
import java.util.regex.Pattern;

public final class RegexValidator extends InternalValidator implements Validator, Parcelable {
    public static final Parcelable.Creator<RegexValidator> CREATOR = new Parcelable.Creator<RegexValidator>() {
        @Override
        public RegexValidator createFromParcel(Parcel parcel) {
            return new RegexValidator((AutofillId) parcel.readParcelable(null), (Pattern) parcel.readSerializable());
        }

        @Override
        public RegexValidator[] newArray(int i) {
            return new RegexValidator[i];
        }
    };
    private static final String TAG = "RegexValidator";
    private final AutofillId mId;
    private final Pattern mRegex;

    public RegexValidator(AutofillId autofillId, Pattern pattern) {
        this.mId = (AutofillId) Preconditions.checkNotNull(autofillId);
        this.mRegex = (Pattern) Preconditions.checkNotNull(pattern);
    }

    @Override
    public boolean isValid(ValueFinder valueFinder) {
        String strFindByAutofillId = valueFinder.findByAutofillId(this.mId);
        if (strFindByAutofillId == null) {
            Log.w(TAG, "No view for id " + this.mId);
            return false;
        }
        boolean zMatches = this.mRegex.matcher(strFindByAutofillId).matches();
        if (Helper.sDebug) {
            Log.d(TAG, "isValid(): " + zMatches);
        }
        return zMatches;
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        return "RegexValidator: [id=" + this.mId + ", regex=" + this.mRegex + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mId, i);
        parcel.writeSerializable(this.mRegex);
    }
}
