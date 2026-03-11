package com.android.settings.accessibility;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import com.android.settingslib.CustomDialogPreference;

public abstract class ListDialogPreference extends CustomDialogPreference {
    private CharSequence[] mEntryTitles;
    private int[] mEntryValues;
    private int mListItemLayout;
    private OnValueChangedListener mOnValueChangedListener;
    private int mValue;
    private int mValueIndex;
    private boolean mValueSet;

    public interface OnValueChangedListener {
        void onValueChanged(ListDialogPreference listDialogPreference, int i);
    }

    protected abstract void onBindListItem(View view, int i);

    public ListDialogPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void setOnValueChangedListener(OnValueChangedListener onValueChangedListener) {
        this.mOnValueChangedListener = onValueChangedListener;
    }

    public void setListItemLayoutResource(int i) {
        this.mListItemLayout = i;
    }

    public void setValues(int[] iArr) {
        this.mEntryValues = iArr;
        if (this.mValueSet && this.mValueIndex == -1) {
            this.mValueIndex = getIndexForValue(this.mValue);
        }
    }

    public void setTitles(CharSequence[] charSequenceArr) {
        this.mEntryTitles = charSequenceArr;
    }

    protected CharSequence getTitleAt(int i) {
        if (this.mEntryTitles == null || this.mEntryTitles.length <= i) {
            return null;
        }
        return this.mEntryTitles[i];
    }

    protected int getValueAt(int i) {
        return this.mEntryValues[i];
    }

    @Override
    public CharSequence getSummary() {
        if (this.mValueIndex >= 0) {
            return getTitleAt(this.mValueIndex);
        }
        return null;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
        super.onPrepareDialogBuilder(builder, onClickListener);
        Context context = getContext();
        View viewInflate = LayoutInflater.from(context).inflate(getDialogLayoutResource(), (ViewGroup) null);
        ListPreferenceAdapter listPreferenceAdapter = new ListPreferenceAdapter();
        AbsListView absListView = (AbsListView) viewInflate.findViewById(R.id.list);
        absListView.setAdapter((ListAdapter) listPreferenceAdapter);
        absListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                int i2 = (int) j;
                if (ListDialogPreference.this.callChangeListener(Integer.valueOf(i2))) {
                    ListDialogPreference.this.setValue(i2);
                }
                Dialog dialog = ListDialogPreference.this.getDialog();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        int indexForValue = getIndexForValue(this.mValue);
        if (indexForValue != -1) {
            absListView.setSelection(indexForValue);
        }
        builder.setView(viewInflate);
        builder.setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null);
    }

    protected int getIndexForValue(int i) {
        int[] iArr = this.mEntryValues;
        if (iArr != null) {
            int length = iArr.length;
            for (int i2 = 0; i2 < length; i2++) {
                if (iArr[i2] == i) {
                    return i2;
                }
            }
            return -1;
        }
        return -1;
    }

    public void setValue(int i) {
        boolean z;
        if (this.mValue == i) {
            z = false;
        } else {
            z = true;
        }
        if (z || !this.mValueSet) {
            this.mValue = i;
            this.mValueIndex = getIndexForValue(i);
            this.mValueSet = true;
            persistInt(i);
            if (z) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
            if (this.mOnValueChangedListener != null) {
                this.mOnValueChangedListener.onValueChanged(this, i);
            }
        }
    }

    public int getValue() {
        return this.mValue;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray typedArray, int i) {
        return Integer.valueOf(typedArray.getInt(i, 0));
    }

    @Override
    protected void onSetInitialValue(boolean z, Object obj) {
        setValue(z ? getPersistedInt(this.mValue) : ((Integer) obj).intValue());
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelableOnSaveInstanceState = super.onSaveInstanceState();
        if (isPersistent()) {
            return parcelableOnSaveInstanceState;
        }
        SavedState savedState = new SavedState(parcelableOnSaveInstanceState);
        savedState.value = getValue();
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
        setValue(savedState.value);
    }

    private class ListPreferenceAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        private ListPreferenceAdapter() {
        }

        @Override
        public int getCount() {
            return ListDialogPreference.this.mEntryValues.length;
        }

        @Override
        public Integer getItem(int i) {
            return Integer.valueOf(ListDialogPreference.this.mEntryValues[i]);
        }

        @Override
        public long getItemId(int i) {
            return ListDialogPreference.this.mEntryValues[i];
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                if (this.mInflater == null) {
                    this.mInflater = LayoutInflater.from(viewGroup.getContext());
                }
                view = this.mInflater.inflate(ListDialogPreference.this.mListItemLayout, viewGroup, false);
            }
            ListDialogPreference.this.onBindListItem(view, i);
            return view;
        }
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
        public int value;

        public SavedState(Parcel parcel) {
            super(parcel);
            this.value = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.value);
        }

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }
    }
}
