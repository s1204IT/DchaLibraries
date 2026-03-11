package com.android.settings.notification;

import android.R;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import java.util.ArrayList;

public class DropDownPreference extends Preference {
    private final ArrayAdapter<String> mAdapter;
    private Callback mCallback;
    private final Context mContext;
    private final Spinner mSpinner;
    private final ArrayList<Object> mValues;

    public interface Callback {
        boolean onItemSelected(int i, Object obj);
    }

    public DropDownPreference(Context context) {
        this(context, null);
    }

    public DropDownPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mValues = new ArrayList<>();
        this.mContext = context;
        this.mAdapter = new ArrayAdapter<>(this.mContext, R.layout.simple_spinner_dropdown_item);
        this.mSpinner = new Spinner(this.mContext);
        this.mSpinner.setVisibility(4);
        this.mSpinner.setAdapter((SpinnerAdapter) this.mAdapter);
        this.mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                DropDownPreference.this.setSelectedItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        setPersistent(false);
        setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DropDownPreference.this.mSpinner.performClick();
                return true;
            }
        });
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void setSelectedItem(int position) {
        Object value = this.mValues.get(position);
        if (this.mCallback == null || this.mCallback.onItemSelected(position, value)) {
            this.mSpinner.setSelection(position);
            setSummary(this.mAdapter.getItem(position));
            boolean disableDependents = value == null;
            notifyDependencyChange(disableDependents);
        }
    }

    public void setSelectedValue(Object value) {
        int i = this.mValues.indexOf(value);
        if (i > -1) {
            setSelectedItem(i);
        }
    }

    public void addItem(int captionResid, Object value) {
        addItem(this.mContext.getResources().getString(captionResid), value);
    }

    public void addItem(String caption, Object value) {
        this.mAdapter.add(caption);
        this.mValues.add(value);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (!view.equals(this.mSpinner.getParent())) {
            if (this.mSpinner.getParent() != null) {
                ((ViewGroup) this.mSpinner.getParent()).removeView(this.mSpinner);
            }
            ViewGroup vg = (ViewGroup) view;
            vg.addView(this.mSpinner, 0);
            ViewGroup.LayoutParams lp = this.mSpinner.getLayoutParams();
            lp.width = 0;
            this.mSpinner.setLayoutParams(lp);
        }
    }
}
