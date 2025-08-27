package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/* loaded from: classes.dex */
public class SetupFingerprintEnrollFindSensor extends FingerprintEnrollFindSensor {
    @Override // com.android.settings.fingerprint.FingerprintEnrollFindSensor
    protected int getContentView() {
        return R.layout.fingerprint_enroll_find_sensor;
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollBase
    protected Intent getEnrollingIntent() {
        Intent intent = new Intent(this, (Class<?>) SetupFingerprintEnrollEnrolling.class);
        intent.putExtra("hw_auth_token", this.mToken);
        if (this.mUserId != -10000) {
            intent.putExtra("android.intent.extra.USER_ID", this.mUserId);
        }
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollFindSensor
    protected void onSkipButtonClick() {
        new SkipFingerprintDialog().show(getFragmentManager());
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollFindSensor, com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 247;
    }

    public static class SkipFingerprintDialog extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 573;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            return onCreateDialogBuilder().create();
        }

        public AlertDialog.Builder onCreateDialogBuilder() {
            return new AlertDialog.Builder(getContext()).setTitle(R.string.setup_fingerprint_enroll_skip_title).setPositiveButton(R.string.skip_anyway_button_label, this).setNegativeButton(R.string.go_back_button_label, this).setMessage(R.string.setup_fingerprint_enroll_skip_after_adding_lock_text);
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            Activity activity;
            if (i == -1 && (activity = getActivity()) != null) {
                activity.setResult(2);
                activity.finish();
            }
        }

        public void show(FragmentManager fragmentManager) {
            show(fragmentManager, "skip_dialog");
        }
    }
}
