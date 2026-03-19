package com.android.storagemanager.deletionhelper;

import android.content.Context;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.Formatter;
import android.widget.CheckBox;
import android.widget.TextView;
import com.android.storagemanager.R;

public class NestedDeletionPreference extends CheckBoxPreference {
    private long mAppSize;
    private TextView mSize;

    public NestedDeletionPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_nested);
        setWidgetLayoutResource(R.layout.preference_widget_checkbox);
    }

    @Override
    protected void onClick() {
        super.onClick();
        this.mSize.setActivated(isChecked());
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        CheckBox checkBox = (CheckBox) preferenceViewHolder.findViewById(android.R.id.checkbox);
        checkBox.setVisibility(0);
        this.mSize = (TextView) preferenceViewHolder.findViewById(R.id.deletion_type_size);
        this.mSize.setActivated(checkBox.isChecked());
        this.mSize.setText(getItemSize());
    }

    void setItemSize(long j) {
        this.mAppSize = j;
    }

    public String getItemSize() {
        return Formatter.formatFileSize(getContext(), this.mAppSize);
    }
}
