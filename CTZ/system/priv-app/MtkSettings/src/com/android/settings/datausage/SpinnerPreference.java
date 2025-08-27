package com.android.settings.datausage;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.android.settings.R;
import com.android.settings.datausage.CycleAdapter;

/* loaded from: classes.dex */
public class SpinnerPreference extends Preference implements CycleAdapter.SpinnerInterface {
    private CycleAdapter mAdapter;
    private Object mCurrentObject;
    private AdapterView.OnItemSelectedListener mListener;
    private final AdapterView.OnItemSelectedListener mOnSelectedListener;
    private int mPosition;

    public SpinnerPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mOnSelectedListener = new AdapterView.OnItemSelectedListener() { // from class: com.android.settings.datausage.SpinnerPreference.1
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                if (SpinnerPreference.this.mPosition == i) {
                    return;
                }
                SpinnerPreference.this.mPosition = i;
                SpinnerPreference.this.mCurrentObject = SpinnerPreference.this.mAdapter.getItem(i);
                SpinnerPreference.this.mListener.onItemSelected(adapterView, view, i, j);
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> adapterView) {
                SpinnerPreference.this.mListener.onNothingSelected(adapterView);
            }
        };
        setLayoutResource(R.layout.data_usage_cycles);
    }

    @Override // com.android.settings.datausage.CycleAdapter.SpinnerInterface
    public void setAdapter(CycleAdapter cycleAdapter) {
        this.mAdapter = cycleAdapter;
        notifyChanged();
    }

    @Override // com.android.settings.datausage.CycleAdapter.SpinnerInterface
    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener onItemSelectedListener) {
        this.mListener = onItemSelectedListener;
    }

    @Override // com.android.settings.datausage.CycleAdapter.SpinnerInterface
    public Object getSelectedItem() {
        return this.mCurrentObject;
    }

    @Override // com.android.settings.datausage.CycleAdapter.SpinnerInterface
    public void setSelection(int i) {
        this.mPosition = i;
        this.mCurrentObject = this.mAdapter.getItem(this.mPosition);
        notifyChanged();
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        Spinner spinner = (Spinner) preferenceViewHolder.findViewById(R.id.cycles_spinner);
        spinner.setAdapter((SpinnerAdapter) this.mAdapter);
        spinner.setSelection(this.mPosition);
        spinner.setOnItemSelectedListener(this.mOnSelectedListener);
    }

    @Override // android.support.v7.preference.Preference
    protected void performClick(View view) {
        view.findViewById(R.id.cycles_spinner).performClick();
    }
}
