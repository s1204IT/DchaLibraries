package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;

final class NegationValidator extends InternalValidator {
    public static final Parcelable.Creator<NegationValidator> CREATOR = new Parcelable.Creator<NegationValidator>() {
        @Override
        public NegationValidator createFromParcel(Parcel parcel) {
            return new NegationValidator((InternalValidator) parcel.readParcelable(null));
        }

        @Override
        public NegationValidator[] newArray(int i) {
            return new NegationValidator[i];
        }
    };
    private final InternalValidator mValidator;

    NegationValidator(InternalValidator internalValidator) {
        this.mValidator = (InternalValidator) Preconditions.checkNotNull(internalValidator);
    }

    @Override
    public boolean isValid(ValueFinder valueFinder) {
        return !this.mValidator.isValid(valueFinder);
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        return "NegationValidator: [validator=" + this.mValidator + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mValidator, i);
    }
}
