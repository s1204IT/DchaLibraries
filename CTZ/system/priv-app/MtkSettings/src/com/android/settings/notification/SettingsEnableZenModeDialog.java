package com.android.settings.notification;

import android.app.Dialog;
import android.os.Bundle;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.notification.EnableZenModeDialog;

/* loaded from: classes.dex */
public class SettingsEnableZenModeDialog extends InstrumentedDialogFragment {
    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        return new EnableZenModeDialog(getContext()).createDialog();
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1286;
    }
}
