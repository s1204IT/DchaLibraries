package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;

final class OptionalValidators extends InternalValidator {
    public static final Parcelable.Creator<OptionalValidators> CREATOR = new Parcelable.Creator<OptionalValidators>() {
        @Override
        public OptionalValidators createFromParcel(Parcel parcel) {
            return new OptionalValidators((InternalValidator[]) parcel.readParcelableArray(null, InternalValidator.class));
        }

        @Override
        public OptionalValidators[] newArray(int i) {
            return new OptionalValidators[i];
        }
    };
    private static final String TAG = "OptionalValidators";
    private final InternalValidator[] mValidators;

    OptionalValidators(InternalValidator[] internalValidatorArr) {
        this.mValidators = (InternalValidator[]) Preconditions.checkArrayElementsNotNull(internalValidatorArr, "validators");
    }

    @Override
    public boolean isValid(ValueFinder valueFinder) {
        for (InternalValidator internalValidator : this.mValidators) {
            boolean zIsValid = internalValidator.isValid(valueFinder);
            if (Helper.sDebug) {
                Log.d(TAG, "isValid(" + internalValidator + "): " + zIsValid);
            }
            if (zIsValid) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        return "OptionalValidators: [validators=" + this.mValidators + "]";
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
