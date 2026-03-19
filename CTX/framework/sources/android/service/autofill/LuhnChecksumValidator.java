package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;
import java.util.Arrays;

public final class LuhnChecksumValidator extends InternalValidator implements Validator, Parcelable {
    public static final Parcelable.Creator<LuhnChecksumValidator> CREATOR = new Parcelable.Creator<LuhnChecksumValidator>() {
        @Override
        public LuhnChecksumValidator createFromParcel(Parcel parcel) {
            return new LuhnChecksumValidator((AutofillId[]) parcel.readParcelableArray(null, AutofillId.class));
        }

        @Override
        public LuhnChecksumValidator[] newArray(int i) {
            return new LuhnChecksumValidator[i];
        }
    };
    private static final String TAG = "LuhnChecksumValidator";
    private final AutofillId[] mIds;

    public LuhnChecksumValidator(AutofillId... autofillIdArr) {
        this.mIds = (AutofillId[]) Preconditions.checkArrayElementsNotNull(autofillIdArr, "ids");
    }

    private static boolean isLuhnChecksumValid(String str) {
        int i = 0;
        boolean z = false;
        for (int length = str.length() - 1; length >= 0; length--) {
            int iCharAt = str.charAt(length) - '0';
            if (iCharAt >= 0 && iCharAt <= 9) {
                if (z && (iCharAt = iCharAt * 2) > 9) {
                    iCharAt -= 9;
                }
                i += iCharAt;
                z = !z;
            }
        }
        return i % 10 == 0;
    }

    @Override
    public boolean isValid(ValueFinder valueFinder) {
        if (this.mIds == null || this.mIds.length == 0) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        for (AutofillId autofillId : this.mIds) {
            String strFindByAutofillId = valueFinder.findByAutofillId(autofillId);
            if (strFindByAutofillId == null) {
                if (Helper.sDebug) {
                    Log.d(TAG, "No partial number for id " + autofillId);
                }
                return false;
            }
            sb.append(strFindByAutofillId);
        }
        String string = sb.toString();
        boolean zIsLuhnChecksumValid = isLuhnChecksumValid(string);
        if (Helper.sDebug) {
            Log.d(TAG, "isValid(" + string.length() + " chars): " + zIsLuhnChecksumValid);
        }
        return zIsLuhnChecksumValid;
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        return "LuhnChecksumValidator: [ids=" + Arrays.toString(this.mIds) + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelableArray(this.mIds, i);
    }
}
