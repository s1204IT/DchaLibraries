package android.support.v14.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.support.v7.preference.R;
import android.support.v7.preference.internal.AbstractMultiSelectListPreference;
import android.util.AttributeSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MultiSelectListPreference extends AbstractMultiSelectListPreference {
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private Set<String> mValues;

    public MultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mValues = new HashSet();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiSelectListPreference, defStyleAttr, defStyleRes);
        this.mEntries = TypedArrayUtils.getTextArray(a, R.styleable.MultiSelectListPreference_entries, R.styleable.MultiSelectListPreference_android_entries);
        this.mEntryValues = TypedArrayUtils.getTextArray(a, R.styleable.MultiSelectListPreference_entryValues, R.styleable.MultiSelectListPreference_android_entryValues);
        a.recycle();
    }

    public MultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MultiSelectListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle, android.R.attr.dialogPreferenceStyle));
    }

    public MultiSelectListPreference(Context context) {
        this(context, null);
    }

    public void setEntries(CharSequence[] entries) {
        this.mEntries = entries;
    }

    @Override
    public CharSequence[] getEntries() {
        return this.mEntries;
    }

    public void setEntryValues(CharSequence[] entryValues) {
        this.mEntryValues = entryValues;
    }

    @Override
    public CharSequence[] getEntryValues() {
        return this.mEntryValues;
    }

    @Override
    public void setValues(Set<String> values) {
        this.mValues.clear();
        this.mValues.addAll(values);
        persistStringSet(values);
    }

    @Override
    public Set<String> getValues() {
        return this.mValues;
    }

    public int findIndexOfValue(String value) {
        if (value != null && this.mEntryValues != null) {
            for (int i = this.mEntryValues.length - 1; i >= 0; i--) {
                if (this.mEntryValues[i].equals(value)) {
                    return i;
                }
            }
            return -1;
        }
        return -1;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        CharSequence[] defaultValues = a.getTextArray(index);
        Set<String> result = new HashSet<>();
        for (CharSequence defaultValue : defaultValues) {
            result.add(defaultValue.toString());
        }
        return result;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValues(restoreValue ? getPersistedStringSet(this.mValues) : (Set) defaultValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        }
        SavedState myState = new SavedState(superState);
        myState.mValues = getValues();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValues(myState.mValues);
    }

    private static class SavedState extends Preference.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        Set<String> mValues;

        SavedState(Parcel source) {
            super(source);
            int size = source.readInt();
            this.mValues = new HashSet();
            String[] strings = new String[size];
            source.readStringArray(strings);
            Collections.addAll(this.mValues, strings);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.mValues.size());
            dest.writeStringArray((String[]) this.mValues.toArray(new String[this.mValues.size()]));
        }
    }
}
