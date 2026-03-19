package android.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import com.android.internal.R;
import java.util.Arrays;

public class MultiCheckPreference extends DialogPreference {
    private CharSequence[] mEntries;
    private String[] mEntryValues;
    private boolean[] mOrigValues;
    private boolean[] mSetValues;
    private String mSummary;

    public MultiCheckPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ListPreference, i, i2);
        this.mEntries = typedArrayObtainStyledAttributes.getTextArray(0);
        if (this.mEntries != null) {
            setEntries(this.mEntries);
        }
        setEntryValuesCS(typedArrayObtainStyledAttributes.getTextArray(1));
        typedArrayObtainStyledAttributes.recycle();
        TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(attributeSet, R.styleable.Preference, 0, 0);
        this.mSummary = typedArrayObtainStyledAttributes2.getString(7);
        typedArrayObtainStyledAttributes2.recycle();
    }

    public MultiCheckPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public MultiCheckPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842897);
    }

    public MultiCheckPreference(Context context) {
        this(context, null);
    }

    public void setEntries(CharSequence[] charSequenceArr) {
        this.mEntries = charSequenceArr;
        this.mSetValues = new boolean[charSequenceArr.length];
        this.mOrigValues = new boolean[charSequenceArr.length];
    }

    public void setEntries(int i) {
        setEntries(getContext().getResources().getTextArray(i));
    }

    public CharSequence[] getEntries() {
        return this.mEntries;
    }

    public void setEntryValues(String[] strArr) {
        this.mEntryValues = strArr;
        Arrays.fill(this.mSetValues, false);
        Arrays.fill(this.mOrigValues, false);
    }

    public void setEntryValues(int i) {
        setEntryValuesCS(getContext().getResources().getTextArray(i));
    }

    private void setEntryValuesCS(CharSequence[] charSequenceArr) {
        setValues(null);
        if (charSequenceArr != null) {
            this.mEntryValues = new String[charSequenceArr.length];
            for (int i = 0; i < charSequenceArr.length; i++) {
                this.mEntryValues[i] = charSequenceArr[i].toString();
            }
        }
    }

    public String[] getEntryValues() {
        return this.mEntryValues;
    }

    public boolean getValue(int i) {
        return this.mSetValues[i];
    }

    public void setValue(int i, boolean z) {
        this.mSetValues[i] = z;
    }

    public void setValues(boolean[] zArr) {
        if (this.mSetValues != null) {
            Arrays.fill(this.mSetValues, false);
            Arrays.fill(this.mOrigValues, false);
            if (zArr != null) {
                System.arraycopy(zArr, 0, this.mSetValues, 0, zArr.length < this.mSetValues.length ? zArr.length : this.mSetValues.length);
            }
        }
    }

    @Override
    public CharSequence getSummary() {
        if (this.mSummary == null) {
            return super.getSummary();
        }
        return this.mSummary;
    }

    @Override
    public void setSummary(CharSequence charSequence) {
        super.setSummary(charSequence);
        if (charSequence == null && this.mSummary != null) {
            this.mSummary = null;
        } else if (charSequence != null && !charSequence.equals(this.mSummary)) {
            this.mSummary = charSequence.toString();
        }
    }

    public boolean[] getValues() {
        return this.mSetValues;
    }

    public int findIndexOfValue(String str) {
        if (str != null && this.mEntryValues != null) {
            for (int length = this.mEntryValues.length - 1; length >= 0; length--) {
                if (this.mEntryValues[length].equals(str)) {
                    return length;
                }
            }
            return -1;
        }
        return -1;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        if (this.mEntries == null || this.mEntryValues == null) {
            throw new IllegalStateException("ListPreference requires an entries array and an entryValues array.");
        }
        this.mOrigValues = Arrays.copyOf(this.mSetValues, this.mSetValues.length);
        builder.setMultiChoiceItems(this.mEntries, this.mSetValues, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean z) {
                MultiCheckPreference.this.mSetValues[i] = z;
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean z) {
        super.onDialogClosed(z);
        if (z && callChangeListener(getValues())) {
            return;
        }
        System.arraycopy(this.mOrigValues, 0, this.mSetValues, 0, this.mSetValues.length);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray typedArray, int i) {
        return typedArray.getString(i);
    }

    @Override
    protected void onSetInitialValue(boolean z, Object obj) {
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelableOnSaveInstanceState = super.onSaveInstanceState();
        if (isPersistent()) {
            return parcelableOnSaveInstanceState;
        }
        SavedState savedState = new SavedState(parcelableOnSaveInstanceState);
        savedState.values = getValues();
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable == null || !parcelable.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        setValues(savedState.values);
    }

    private static class SavedState extends Preference.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        boolean[] values;

        public SavedState(Parcel parcel) {
            super(parcel);
            this.values = parcel.createBooleanArray();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeBooleanArray(this.values);
        }

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }
    }
}
