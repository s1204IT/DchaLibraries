package com.mediatek.nfc;

import android.content.ComponentName;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.widget.CheckBox;
import com.android.settings.R;
import com.mediatek.nfcsettingsadapter.ServiceEntry;

/* loaded from: classes.dex */
public class NfcServicePreference extends Preference {
    private CheckBox mCheckBox;
    ComponentName mComponent;
    boolean mSelected;
    private boolean mShowCheckBox;

    public NfcServicePreference(Context context, ServiceEntry serviceEntry) {
        super(context);
        setLayoutResource(R.layout.nfc_service);
        setIcon(serviceEntry.getIcon(context.getPackageManager()));
        setTitle(serviceEntry.getTitle());
        this.mSelected = serviceEntry.getWantEnabled().booleanValue();
        this.mComponent = serviceEntry.getComponent();
    }

    public void setShowCheckbox(boolean z) {
        this.mShowCheckBox = z;
    }

    public boolean isChecked() {
        return this.mCheckBox.isChecked();
    }

    public void setChecked(boolean z) {
        this.mCheckBox.setChecked(z);
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mCheckBox = (CheckBox) preferenceViewHolder.findViewById(R.id.checkbox);
        this.mCheckBox.setChecked(this.mSelected);
        this.mCheckBox.setVisibility(this.mShowCheckBox ? 0 : 4);
    }
}
