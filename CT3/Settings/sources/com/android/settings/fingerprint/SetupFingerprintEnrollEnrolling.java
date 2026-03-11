package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.setupwizardlib.util.SystemBarHelper;
import com.android.setupwizardlib.view.NavigationBar;

public class SetupFingerprintEnrollEnrolling extends FingerprintEnrollEnrolling implements NavigationBar.NavigationBarListener {
    @Override
    protected Intent getFinishIntent() {
        Intent intent = new Intent(this, (Class<?>) SetupFingerprintEnrollFinish.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        int resid2 = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid2, first);
    }

    @Override
    protected void initViews() {
        SetupWizardUtils.setImmersiveMode(this);
        View buttonBar = findViewById(R.id.button_bar);
        if (buttonBar != null) {
            buttonBar.setVisibility(8);
        }
        NavigationBar navigationBar = getNavigationBar();
        navigationBar.setNavigationBarListener(this);
        navigationBar.getNextButton().setText(R.string.skip_label);
        navigationBar.getBackButton().setVisibility(8);
    }

    @Override
    protected Button getNextButton() {
        return getNavigationBar().getNextButton();
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        new SkipDialog().show(getFragmentManager(), "dialog");
    }

    @Override
    protected int getMetricsCategory() {
        return 246;
    }

    public static class SkipDialog extends DialogFragment {
        @Override
        public void show(FragmentManager manager, String tag) {
            if (manager.findFragmentByTag(tag) != null) {
                return;
            }
            super.show(manager, tag);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.setup_fingerprint_enroll_enrolling_skip_title).setMessage(R.string.setup_fingerprint_enroll_enrolling_skip_message).setCancelable(false).setPositiveButton(R.string.wifi_skip_anyway, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int id) {
                    Activity activity = SkipDialog.this.getActivity();
                    if (activity == null) {
                        return;
                    }
                    activity.setResult(2);
                    activity.finish();
                }
            }).setNegativeButton(R.string.wifi_dont_skip, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int id) {
                }
            }).create();
            SystemBarHelper.hideSystemBars(dialog);
            return dialog;
        }
    }
}
