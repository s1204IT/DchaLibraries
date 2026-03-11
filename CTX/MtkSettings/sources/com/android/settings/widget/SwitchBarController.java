package com.android.settings.widget;

import android.widget.Switch;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedLockUtils;

public class SwitchBarController extends SwitchWidgetController implements SwitchBar.OnSwitchChangeListener {
    private final SwitchBar mSwitchBar;

    public SwitchBarController(SwitchBar switchBar) {
        this.mSwitchBar = switchBar;
    }

    @Override
    public void setupView() {
        this.mSwitchBar.show();
    }

    @Override
    public void teardownView() {
        this.mSwitchBar.hide();
    }

    @Override
    public void updateTitle(boolean z) {
        this.mSwitchBar.setTextViewLabelAndBackground(z);
    }

    @Override
    public void startListening() {
        this.mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void stopListening() {
        this.mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    public void setChecked(boolean z) {
        this.mSwitchBar.setChecked(z);
    }

    @Override
    public boolean isChecked() {
        return this.mSwitchBar.isChecked();
    }

    @Override
    public void setEnabled(boolean z) {
        this.mSwitchBar.setEnabled(z);
    }

    @Override
    public void onSwitchChanged(Switch r1, boolean z) {
        if (this.mListener != null) {
            this.mListener.onSwitchToggled(z);
        }
    }

    @Override
    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        this.mSwitchBar.setDisabledByAdmin(enforcedAdmin);
    }
}
