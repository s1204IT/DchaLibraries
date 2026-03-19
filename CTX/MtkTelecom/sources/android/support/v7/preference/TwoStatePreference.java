package android.support.v7.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public abstract class TwoStatePreference extends Preference {
    protected boolean mChecked;
    private boolean mCheckedSet;
    private boolean mDisableDependentsState;
    private CharSequence mSummaryOff;
    private CharSequence mSummaryOn;

    public TwoStatePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public TwoStatePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TwoStatePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    protected void onClick() {
        super.onClick();
        boolean newValue = !isChecked();
        if (callChangeListener(Boolean.valueOf(newValue))) {
            setChecked(newValue);
        }
    }

    public void setChecked(boolean checked) {
        boolean changed = this.mChecked != checked;
        if (changed || !this.mCheckedSet) {
            this.mChecked = checked;
            this.mCheckedSet = true;
            persistBoolean(checked);
            if (changed) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
        }
    }

    public boolean isChecked() {
        return this.mChecked;
    }

    @Override
    public boolean shouldDisableDependents() {
        boolean shouldDisable = this.mDisableDependentsState ? this.mChecked : !this.mChecked;
        return shouldDisable || super.shouldDisableDependents();
    }

    public void setSummaryOn(CharSequence summary) {
        this.mSummaryOn = summary;
        if (isChecked()) {
            notifyChanged();
        }
    }

    public void setSummaryOff(CharSequence summary) {
        this.mSummaryOff = summary;
        if (!isChecked()) {
            notifyChanged();
        }
    }

    public void setDisableDependentsState(boolean disableDependentsState) {
        this.mDisableDependentsState = disableDependentsState;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return Boolean.valueOf(a.getBoolean(index, false));
    }

    protected void syncSummaryView(View view) {
        if (!(view instanceof TextView)) {
            return;
        }
        TextView summaryView = (TextView) view;
        boolean useDefaultSummary = true;
        if (this.mChecked && !TextUtils.isEmpty(this.mSummaryOn)) {
            summaryView.setText(this.mSummaryOn);
            useDefaultSummary = false;
        } else if (!this.mChecked && !TextUtils.isEmpty(this.mSummaryOff)) {
            summaryView.setText(this.mSummaryOff);
            useDefaultSummary = false;
        }
        if (useDefaultSummary) {
            CharSequence summary = getSummary();
            if (!TextUtils.isEmpty(summary)) {
                summaryView.setText(summary);
                useDefaultSummary = false;
            }
        }
        int newVisibility = 8;
        if (!useDefaultSummary) {
            newVisibility = 0;
        }
        if (newVisibility != summaryView.getVisibility()) {
            summaryView.setVisibility(newVisibility);
        }
    }

    static class SavedState extends Preference.BaseSavedState {
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
        boolean checked;

        public SavedState(Parcel source) {
            super(source);
            this.checked = source.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.checked ? 1 : 0);
        }
    }
}
