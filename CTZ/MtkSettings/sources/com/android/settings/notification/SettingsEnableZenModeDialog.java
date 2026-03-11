package com.android.settings.notification;

import android.app.Dialog;
import android.os.Bundle;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.notification.EnableZenModeDialog;

public class SettingsEnableZenModeDialog extends InstrumentedDialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return new EnableZenModeDialog(getContext()).createDialog();
    }

    @Override
    public int getMetricsCategory() {
        return 1286;
    }
}
