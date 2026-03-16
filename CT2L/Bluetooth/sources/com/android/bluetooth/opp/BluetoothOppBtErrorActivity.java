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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String mErrorTitle = intent.getStringExtra("title");
        this.mErrorContent = intent.getStringExtra("content");
        AlertController.AlertParams p = this.mAlertParams;
        p.mIconAttrId = R.attr.alertDialogIcon;
        p.mTitle = mErrorTitle;
        p.mView = createView();
        p.mPositiveButtonText = getString(com.android.bluetooth.R.string.bt_error_btn_ok);
        p.mPositiveButtonListener = this;
        setupAlert();
    }

    private View createView() {
        View view = getLayoutInflater().inflate(com.android.bluetooth.R.layout.confirm_dialog, (ViewGroup) null);
        TextView contentView = (TextView) view.findViewById(com.android.bluetooth.R.id.content);
        contentView.setText(this.mErrorContent);
        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
    }
}
