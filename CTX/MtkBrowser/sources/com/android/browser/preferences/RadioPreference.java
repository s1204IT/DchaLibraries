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

public class RadioPreference extends Preference {
    private AccessibilityManager mAccessibilityManager;
    private boolean mChecked;
    private boolean mDisableDependentsState;
    private boolean mSendAccessibilityEventViewClickedType;

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
        boolean mSaveStateChecked;

        public SavedState(Parcel parcel) {
            super(parcel);
            this.mSaveStateChecked = parcel.readInt() == 1;
        }

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.mSaveStateChecked ? 1 : 0);
        }
    }

    public RadioPreference(Context context) {
        super(context);
        this.mAccessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
    }

    public boolean isChecked() {
        return this.mChecked;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View viewFindViewById = view.findViewById(2131558510);
        if (viewFindViewById == 0 || !(viewFindViewById instanceof Checkable)) {
            return;
        }
        ((Checkable) viewFindViewById).setChecked(this.mChecked);
        if (this.mSendAccessibilityEventViewClickedType && this.mAccessibilityManager.isEnabled() && viewFindViewById.isEnabled()) {
            this.mSendAccessibilityEventViewClickedType = false;
            viewFindViewById.sendAccessibilityEventUnchecked(AccessibilityEvent.obtain(1));
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        boolean z = !isChecked();
        this.mSendAccessibilityEventViewClickedType = true;
        if (callChangeListener(Boolean.valueOf(z))) {
            setChecked(z);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray typedArray, int i) {
        return Boolean.valueOf(typedArray.getBoolean(i, false));
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable == null || !parcelable.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        setChecked(savedState.mSaveStateChecked);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelableOnSaveInstanceState = super.onSaveInstanceState();
        if (isPersistent()) {
            return parcelableOnSaveInstanceState;
        }
        SavedState savedState = new SavedState(parcelableOnSaveInstanceState);
        savedState.mSaveStateChecked = isChecked();
        return savedState;
    }

    @Override
    protected void onSetInitialValue(boolean z, Object obj) {
        setChecked(z ? getPersistedBoolean(this.mChecked) : ((Boolean) obj).booleanValue());
    }

    public void setChecked(boolean z) {
        if (this.mChecked != z) {
            this.mChecked = z;
            persistBoolean(z);
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    @Override
    public boolean shouldDisableDependents() {
        return (this.mDisableDependentsState ? this.mChecked : !this.mChecked) || super.shouldDisableDependents();
    }
}
