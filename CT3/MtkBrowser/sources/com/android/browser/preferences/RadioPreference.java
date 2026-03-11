package com.android.browser.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Checkable;
import com.android.browser.R;

public class RadioPreference extends Preference {
    private AccessibilityManager mAccessibilityManager;
    private boolean mChecked;
    private boolean mDisableDependentsState;
    private boolean mSendAccessibilityEventViewClickedType;

    public RadioPreference(Context context) {
        super(context);
        this.mAccessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View viewFindViewById = view.findViewById(R.id.radiobutton);
        if (viewFindViewById == 0 || !(viewFindViewById instanceof Checkable)) {
            return;
        }
        ((Checkable) viewFindViewById).setChecked(this.mChecked);
        if (!this.mSendAccessibilityEventViewClickedType || !this.mAccessibilityManager.isEnabled() || !viewFindViewById.isEnabled()) {
            return;
        }
        this.mSendAccessibilityEventViewClickedType = false;
        viewFindViewById.sendAccessibilityEventUnchecked(AccessibilityEvent.obtain(1));
    }

    @Override
    protected void onClick() {
        super.onClick();
        boolean newValue = !isChecked();
        this.mSendAccessibilityEventViewClickedType = true;
        if (!callChangeListener(Boolean.valueOf(newValue))) {
            return;
        }
        setChecked(newValue);
    }

    public void setChecked(boolean checked) {
        if (this.mChecked == checked) {
            return;
        }
        this.mChecked = checked;
        persistBoolean(checked);
        notifyDependencyChange(shouldDisableDependents());
        notifyChanged();
    }

    public boolean isChecked() {
        return this.mChecked;
    }

    @Override
    public boolean shouldDisableDependents() {
        boolean shouldDisable;
        if (this.mDisableDependentsState) {
            shouldDisable = this.mChecked;
        } else {
            shouldDisable = !this.mChecked;
        }
        if (shouldDisable) {
            return true;
        }
        return super.shouldDisableDependents();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return Boolean.valueOf(a.getBoolean(index, false));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setChecked(restoreValue ? getPersistedBoolean(this.mChecked) : ((Boolean) defaultValue).booleanValue());
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        }
        SavedState myState = new SavedState(superState);
        myState.mSaveStateChecked = isChecked();
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
        setChecked(myState.mSaveStateChecked);
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
        boolean mSaveStateChecked;

        public SavedState(Parcel source) {
            super(source);
            this.mSaveStateChecked = source.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.mSaveStateChecked ? 1 : 0);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }
    }
}
