package com.android.bluetooth.opp;

import android.R;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.vcard.VCardConfig;

public class BluetoothOppBtEnableActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String TAG = "BluetoothOppBtEnableActivity";
    private BluetoothOppManager mOppManager;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mOppManager = BluetoothOppManager.getInstance(this);
        this.mOppManager.mSendingFlag = false;
        AlertController.AlertParams alertParams = ((AlertActivity) this).mAlertParams;
        alertParams.mIconAttrId = R.attr.alertDialogIcon;
        alertParams.mTitle = getString(com.android.bluetooth.R.string.bt_enable_title);
        alertParams.mView = createView();
        alertParams.mPositiveButtonText = getString(com.android.bluetooth.R.string.bt_enable_ok);
        alertParams.mPositiveButtonListener = this;
        alertParams.mNegativeButtonText = getString(com.android.bluetooth.R.string.bt_enable_cancel);
        alertParams.mNegativeButtonListener = this;
        setupAlert();
    }

    private View createView() {
        View viewInflate = getLayoutInflater().inflate(com.android.bluetooth.R.layout.confirm_dialog, (ViewGroup) null);
        ((TextView) viewInflate.findViewById(com.android.bluetooth.R.id.content)).setText(getString(com.android.bluetooth.R.string.bt_enable_line1) + "\n\n" + getString(com.android.bluetooth.R.string.bt_enable_line2) + "\n");
        return viewInflate;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case -2:
                this.mOppManager.cleanUpSendingFileInfo();
                finish();
                break;
            case -1:
                this.mOppManager.enableBluetooth();
                this.mOppManager.mSendingFlag = true;
                Toast.makeText((Context) this, (CharSequence) getString(com.android.bluetooth.R.string.enabling_progress_content), 0).show();
                Intent intent = new Intent((Context) this, (Class<?>) BluetoothOppBtEnablingActivity.class);
                intent.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
                startActivity(intent);
                finish();
                break;
        }
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        Log.i(TAG, "onConfigurationChanged ++");
    }
}
