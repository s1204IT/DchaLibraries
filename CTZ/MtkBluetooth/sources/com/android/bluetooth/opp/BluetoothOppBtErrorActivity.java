package com.android.bluetooth.opp;

import android.R;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class BluetoothOppBtErrorActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private String mErrorContent;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        String stringExtra = intent.getStringExtra("title");
        this.mErrorContent = intent.getStringExtra("content");
        AlertController.AlertParams alertParams = ((AlertActivity) this).mAlertParams;
        alertParams.mIconAttrId = R.attr.alertDialogIcon;
        alertParams.mTitle = stringExtra;
        alertParams.mView = createView();
        alertParams.mPositiveButtonText = getString(com.android.bluetooth.R.string.bt_error_btn_ok);
        alertParams.mPositiveButtonListener = this;
        setupAlert();
    }

    private View createView() {
        View viewInflate = getLayoutInflater().inflate(com.android.bluetooth.R.layout.confirm_dialog, (ViewGroup) null);
        ((TextView) viewInflate.findViewById(com.android.bluetooth.R.id.content)).setText(this.mErrorContent);
        return viewInflate;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
    }
}
