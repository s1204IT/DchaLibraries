package android.service.autofill;

import android.icu.text.DateFormat;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.autofill.Helper;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.Date;

public final class DateTransformation extends InternalTransformation implements Transformation, Parcelable {
    public static final Parcelable.Creator<DateTransformation> CREATOR = new Parcelable.Creator<DateTransformation>() {
        @Override
        public DateTransformation createFromParcel(Parcel parcel) {
            return new DateTransformation((AutofillId) parcel.readParcelable(null), (DateFormat) parcel.readSerializable());
        }

        @Override
        public DateTransformation[] newArray(int i) {
            return new DateTransformation[i];
        }
    };
    private static final String TAG = "DateTransformation";
    private final DateFormat mDateFormat;
    private final AutofillId mFieldId;

    public DateTransformation(AutofillId autofillId, DateFormat dateFormat) {
        this.mFieldId = (AutofillId) Preconditions.checkNotNull(autofillId);
        this.mDateFormat = (DateFormat) Preconditions.checkNotNull(dateFormat);
    }

    @Override
    public void apply(ValueFinder valueFinder, RemoteViews remoteViews, int i) throws Exception {
        AutofillValue autofillValueFindRawValueByAutofillId = valueFinder.findRawValueByAutofillId(this.mFieldId);
        if (autofillValueFindRawValueByAutofillId == null) {
            Log.w(TAG, "No value for id " + this.mFieldId);
            return;
        }
        if (!autofillValueFindRawValueByAutofillId.isDate()) {
            Log.w(TAG, "Value for " + this.mFieldId + " is not date: " + autofillValueFindRawValueByAutofillId);
            return;
        }
        try {
            Date date = new Date(autofillValueFindRawValueByAutofillId.getDateValue());
            String str = this.mDateFormat.format(date);
            if (Helper.sDebug) {
                Log.d(TAG, "Transformed " + date + " to " + str);
            }
            remoteViews.setCharSequence(i, "setText", str);
        } catch (Exception e) {
            Log.w(TAG, "Could not apply " + this.mDateFormat + " to " + autofillValueFindRawValueByAutofillId + ": " + e);
        }
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        return "DateTransformation: [id=" + this.mFieldId + ", format=" + this.mDateFormat + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mFieldId, i);
        parcel.writeSerializable(this.mDateFormat);
    }
}
