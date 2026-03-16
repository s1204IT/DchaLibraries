package com.android.bluetooth.opp;

import android.R;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.vcard.VCardConfig;

public class BluetoothOppBtEnableActivity extends AlertActivity implements DialogInterface.OnClickListener {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertController.AlertParams p = this.mAlertParams;
        p.mIconAttrId = R.attr.alertDialogIcon;
        p.mTitle = getString(com.android.bluetooth.R.string.bt_enable_title);
        p.mView = createView();
        p.mPositiveButtonText = getString(com.android.bluetooth.R.string.bt_enable_ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(com.android.bluetooth.R.string.bt_enable_cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private View createView() {
        View view = getLayoutInflater().inflate(com.android.bluetooth.R.layout.confirm_dialog, (ViewGroup) null);
        TextView contentView = (TextView) view.findViewById(com.android.bluetooth.R.id.content);
        contentView.setText(getString(com.android.bluetooth.R.string.bt_enable_line1) + "\n\n" + getString(com.android.bluetooth.R.string.bt_enable_line2) + "\n");
        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                finish();
                break;
            case -1:
                BluetoothOppManager mOppManager = BluetoothOppManager.getInstance(this);
                mOppManager.enableBluetooth();
                mOppManager.mSendingFlag = true;
                Toast.makeText((Context) this, (CharSequence) getString(com.android.bluetooth.R.string.enabling_progress_content), 0).show();
                Intent in = new Intent((Context) this, (Class<?>) BluetoothOppBtEnablingActivity.class);
                in.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
                startActivity(in);
                finish();
                break;
        }
    }
}
