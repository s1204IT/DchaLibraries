package com.android.settings.notification;

import android.R;
import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.widget.ToggleSwitch;

/* loaded from: classes.dex */
public class NotificationSwitchBarPreference extends LayoutPreference {
    private boolean mChecked;
    private boolean mEnableSwitch;
    private ToggleSwitch mSwitch;

    public NotificationSwitchBarPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mEnableSwitch = true;
    }

    @Override // com.android.settings.applications.LayoutPreference, android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mSwitch = (ToggleSwitch) preferenceViewHolder.findViewById(R.id.switch_widget);
        if (this.mSwitch != null) {
            this.mSwitch.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.notification.NotificationSwitchBarPreference.1
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    if (!NotificationSwitchBarPreference.this.mSwitch.isEnabled()) {
                        return;
                    }
                    NotificationSwitchBarPreference.this.mChecked = !NotificationSwitchBarPreference.this.mChecked;
                    NotificationSwitchBarPreference.this.setChecked(NotificationSwitchBarPreference.this.mChecked);
                    if (!NotificationSwitchBarPreference.this.callChangeListener(Boolean.valueOf(NotificationSwitchBarPreference.this.mChecked))) {
                        NotificationSwitchBarPreference.this.setChecked(!NotificationSwitchBarPreference.this.mChecked);
                    }
                }
            });
            this.mSwitch.setChecked(this.mChecked);
            this.mSwitch.setEnabled(this.mEnableSwitch);
        }
    }

    public void setChecked(boolean z) {
        this.mChecked = z;
        if (this.mSwitch != null) {
            this.mSwitch.setChecked(z);
        }
    }
}
