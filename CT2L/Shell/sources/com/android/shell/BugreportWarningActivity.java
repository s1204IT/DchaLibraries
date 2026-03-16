package com.android.shell;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class BugreportWarningActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private CheckBox mConfirmRepeat;
    private Intent mSendIntent;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mSendIntent = (Intent) getIntent().getParcelableExtra("android.intent.extra.INTENT");
        this.mSendIntent.hasExtra("android.intent.extra.STREAM");
        AlertController.AlertParams ap = this.mAlertParams;
        ap.mView = LayoutInflater.from(this).inflate(R.layout.confirm_repeat, (ViewGroup) null);
        ap.mPositiveButtonText = getString(android.R.string.ok);
        ap.mNegativeButtonText = getString(android.R.string.cancel);
        ap.mPositiveButtonListener = this;
        ap.mNegativeButtonListener = this;
        this.mConfirmRepeat = (CheckBox) ap.mView.findViewById(android.R.id.checkbox);
        this.mConfirmRepeat.setChecked(BugreportPrefs.getWarningState(this, 0) == 1);
        setupAlert();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            BugreportPrefs.setWarningState(this, this.mConfirmRepeat.isChecked() ? 1 : 2);
            startActivity(this.mSendIntent);
        }
        finish();
    }
}
