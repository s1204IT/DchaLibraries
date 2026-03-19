package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;

final class RequiredValidators extends InternalValidator {
    public static final Parcelable.Creator<RequiredValidators> CREATOR = new Parcelable.Creator<RequiredValidators>() {
        @Override
        public RequiredValidators createFromParcel(Parcel parcel) {
            return new RequiredValidators((InternalValidator[]) parcel.readParcelableArray(null, InternalValidator.class));
        }

        @Override
        public RequiredValidators[] newArray(int i) {
            return new RequiredValidators[i];
        }
    };
    private static final String TAG = "RequiredValidators";
    private final InternalValidator[] mValidators;

    RequiredValidators(InternalValidator[] internalValidatorArr) {
        this.mValidators = (InternalValidator[]) Preconditions.checkArrayElementsNotNull(internalValidatorArr, "validators");
    }

    @Override
    public boolean isValid(ValueFinder valueFinder) {
        for (InternalValidator internalValidator : this.mValidators) {
            boolean zIsValid = internalValidator.isValid(valueFinder);
            if (Helper.sDebug) {
                Log.d(TAG, "isValid(" + internalValidator + "): " + zIsValid);
            }
            if (!zIsValid) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        return "RequiredValidators: [validators=" + this.mValidators + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelableArray(this.mValidators, i);
    }
}
