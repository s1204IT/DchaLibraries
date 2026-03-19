package com.android.storagemanager.deletionhelper;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.storagemanager.R;

public class CollapsibleCheckboxPreferenceGroup extends PreferenceGroup implements View.OnClickListener {
    private boolean mChecked;
    private boolean mCollapsed;
    private boolean mLoaded;
    private ProgressBar mProgressBar;
    private TextView mTextView;
    private View mWidget;

    public CollapsibleCheckboxPreferenceGroup(Context context) {
        this(context, null);
    }

    public CollapsibleCheckboxPreferenceGroup(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setLayoutResource(R.layout.deletion_preference);
        setWidgetLayoutResource(R.layout.preference_widget_checkbox);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(android.R.id.checkbox);
        this.mTextView = (TextView) preferenceViewHolder.findViewById(android.R.id.summary);
        this.mTextView.setActivated(this.mChecked);
        if (viewFindViewById != 0 && (viewFindViewById instanceof Checkable)) {
            ((Checkable) viewFindViewById).setChecked(this.mChecked);
            View view = (View) viewFindViewById.getParent();
            view.setClickable(true);
            view.setFocusable(true);
            view.setOnClickListener(this);
        }
        this.mProgressBar = (ProgressBar) preferenceViewHolder.findViewById(R.id.progress_bar);
        this.mProgressBar.setVisibility(this.mLoaded ? 8 : 0);
        this.mWidget = preferenceViewHolder.findViewById(android.R.id.widget_frame);
        this.mWidget.setVisibility(this.mLoaded ? 0 : 8);
        ((ImageView) preferenceViewHolder.findViewById(android.R.id.icon)).setActivated(!this.mCollapsed);
    }

    @Override
    public boolean addPreference(Preference preference) {
        super.addPreference(preference);
        preference.setVisible(!isCollapsed());
        return true;
    }

    @Override
    protected void onClick() {
        super.onClick();
        setCollapse(!isCollapsed());
    }

    @Override
    public void onClick(View view) {
        super.onClick();
        setChecked(!isChecked());
        ((Checkable) ((ViewGroup) view).findViewById(android.R.id.checkbox)).setChecked(this.mChecked);
        this.mTextView.setActivated(this.mChecked);
    }

    public boolean isCollapsed() {
        return this.mCollapsed;
    }

    public boolean isChecked() {
        return this.mChecked;
    }

    public void setChecked(boolean z) {
        if (this.mChecked != z) {
            this.mChecked = z;
            callChangeListener(Boolean.valueOf(z));
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfoCompat);
        accessibilityNodeInfoCompat.setCheckable(true);
        accessibilityNodeInfoCompat.setChecked(isChecked());
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.checked = isChecked();
        savedState.collapsed = isCollapsed();
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable == null || !SavedState.class.equals(parcelable.getClass())) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        setChecked(savedState.checked);
        setCollapse(savedState.collapsed);
    }

    private void setCollapse(boolean z) {
        if (this.mCollapsed == z) {
            return;
        }
        this.mCollapsed = z;
        setAllPreferencesVisibility(!z);
        notifyChanged();
    }

    private void setAllPreferencesVisibility(boolean z) {
        for (int i = 0; i < getPreferenceCount(); i++) {
            getPreference(i).setVisible(z);
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
        boolean checked;
        boolean collapsed;

        public SavedState(Parcel parcel) {
            super(parcel);
            this.checked = parcel.readInt() != 0;
            this.collapsed = parcel.readInt() != 0;
        }

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.checked ? 1 : 0);
            parcel.writeInt(this.collapsed ? 1 : 0);
        }
    }

    @VisibleForTesting
    void switchSpinnerToCheckboxOrDisablePreference(long j, int i) {
        this.mLoaded = i != 0;
        setEnabled(i != 2);
        if (!isEnabled()) {
            setChecked(false);
        }
        if (this.mProgressBar != null) {
            this.mProgressBar.setVisibility(8);
        }
        if (this.mWidget != null) {
            this.mWidget.setVisibility(0);
        }
    }
}
