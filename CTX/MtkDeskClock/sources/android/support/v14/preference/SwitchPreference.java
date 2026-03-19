package android.support.v14.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.RestrictTo;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.TwoStatePreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.Switch;

public class SwitchPreference extends TwoStatePreference {
    private final Listener mListener;
    private CharSequence mSwitchOff;
    private CharSequence mSwitchOn;

    private class Listener implements CompoundButton.OnCheckedChangeListener {
        private Listener() {
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!SwitchPreference.this.callChangeListener(Boolean.valueOf(isChecked))) {
                buttonView.setChecked(!isChecked);
            } else {
                SwitchPreference.this.setChecked(isChecked);
            }
        }
    }

    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mListener = new Listener();
        TypedArray a = context.obtainStyledAttributes(attrs, android.support.v7.preference.R.styleable.SwitchPreference, defStyleAttr, defStyleRes);
        setSummaryOn(TypedArrayUtils.getString(a, android.support.v7.preference.R.styleable.SwitchPreference_summaryOn, android.support.v7.preference.R.styleable.SwitchPreference_android_summaryOn));
        setSummaryOff(TypedArrayUtils.getString(a, android.support.v7.preference.R.styleable.SwitchPreference_summaryOff, android.support.v7.preference.R.styleable.SwitchPreference_android_summaryOff));
        setSwitchTextOn(TypedArrayUtils.getString(a, android.support.v7.preference.R.styleable.SwitchPreference_switchTextOn, android.support.v7.preference.R.styleable.SwitchPreference_android_switchTextOn));
        setSwitchTextOff(TypedArrayUtils.getString(a, android.support.v7.preference.R.styleable.SwitchPreference_switchTextOff, android.support.v7.preference.R.styleable.SwitchPreference_android_switchTextOff));
        setDisableDependentsState(TypedArrayUtils.getBoolean(a, android.support.v7.preference.R.styleable.SwitchPreference_disableDependentsState, android.support.v7.preference.R.styleable.SwitchPreference_android_disableDependentsState, false));
        a.recycle();
    }

    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, android.support.v7.preference.R.attr.switchPreferenceStyle, android.R.attr.switchPreferenceStyle));
    }

    public SwitchPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View switchView = holder.findViewById(16908352);
        syncSwitchView(switchView);
        syncSummaryView(holder);
    }

    public void setSwitchTextOn(CharSequence onText) {
        this.mSwitchOn = onText;
        notifyChanged();
    }

    public void setSwitchTextOff(CharSequence offText) {
        this.mSwitchOff = offText;
        notifyChanged();
    }

    public void setSwitchTextOn(int resId) {
        setSwitchTextOn(getContext().getString(resId));
    }

    public void setSwitchTextOff(int resId) {
        setSwitchTextOff(getContext().getString(resId));
    }

    public CharSequence getSwitchTextOn() {
        return this.mSwitchOn;
    }

    public CharSequence getSwitchTextOff() {
        return this.mSwitchOff;
    }

    @Override
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    protected void performClick(View view) {
        super.performClick(view);
        syncViewIfAccessibilityEnabled(view);
    }

    private void syncViewIfAccessibilityEnabled(View view) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        if (!accessibilityManager.isEnabled()) {
            return;
        }
        View switchView = view.findViewById(16908352);
        syncSwitchView(switchView);
        View summaryView = view.findViewById(android.R.id.summary);
        syncSummaryView(summaryView);
    }

    private void syncSwitchView(View view) {
        if (view instanceof Switch) {
            ((Switch) view).setOnCheckedChangeListener(null);
        }
        if (view instanceof Checkable) {
            ((Checkable) view).setChecked(this.mChecked);
        }
        if (view instanceof Switch) {
            Switch switchView = (Switch) view;
            switchView.setTextOn(this.mSwitchOn);
            switchView.setTextOff(this.mSwitchOff);
            switchView.setOnCheckedChangeListener(this.mListener);
        }
    }
}
