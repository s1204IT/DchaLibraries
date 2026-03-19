package com.android.shell;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class BugreportWarningActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private CheckBox mConfirmRepeat;
    private Intent mSendIntent;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSendIntent = (Intent) getIntent().getParcelableExtra("android.intent.extra.INTENT");
        this.mSendIntent.hasExtra("android.intent.extra.STREAM");
        AlertController.AlertParams alertParams = this.mAlertParams;
        alertParams.mView = LayoutInflater.from(this).inflate(R.layout.confirm_repeat, (ViewGroup) null);
        alertParams.mPositiveButtonText = getString(android.R.string.ok);
        alertParams.mNegativeButtonText = getString(android.R.string.cancel);
        alertParams.mPositiveButtonListener = this;
        alertParams.mNegativeButtonListener = this;
        this.mConfirmRepeat = (CheckBox) alertParams.mView.findViewById(android.R.id.checkbox);
        boolean z = false;
        int warningState = BugreportPrefs.getWarningState(this, 0);
        if (!Build.IS_USER ? warningState != 1 : warningState == 2) {
            z = true;
        }
        this.mConfirmRepeat.setChecked(z);
        setupAlert();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            BugreportPrefs.setWarningState(this, this.mConfirmRepeat.isChecked() ? 2 : 1);
            BugreportProgressService.sendShareIntent(this, this.mSendIntent);
        }
        finish();
    }
}
