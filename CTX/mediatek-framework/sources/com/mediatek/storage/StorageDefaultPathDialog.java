package com.mediatek.storage;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class StorageDefaultPathDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String INSERT_OTG = "insert_otg";
    private static final String SD_ACTION = "android.intent.action.MEDIA_BAD_REMOVAL";
    private static final String TAG = "StorageDefaultPathDialog";
    private BroadcastReceiver mReceiver;
    private IntentFilter mSDCardStateFilter;
    String path = null;
    private Boolean mInsertOtg = false;
    private final BroadcastReceiver mSDStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(StorageDefaultPathDialog.SD_ACTION)) {
                StorageDefaultPathDialog.this.finish();
            }
        }
    };

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d(TAG, "StorageDefaultPathDialog onCreate()");
        this.mSDCardStateFilter = new IntentFilter(SD_ACTION);
        this.mSDCardStateFilter.addDataScheme("file");
        this.mReceiver = this.mSDStateReceiver;
        this.mInsertOtg = Boolean.valueOf(getIntent().getBooleanExtra(INSERT_OTG, false));
        createDialog();
    }

    private void createDialog() {
        AlertController.AlertParams alertParams = this.mAlertParams;
        alertParams.mTitle = this.mInsertOtg.booleanValue() ? getString(134545539) : getString(134545527);
        alertParams.mView = createView();
        alertParams.mViewSpacingSpecified = true;
        alertParams.mViewSpacingLeft = 15;
        alertParams.mViewSpacingRight = 15;
        alertParams.mViewSpacingTop = 5;
        alertParams.mViewSpacingBottom = 5;
        alertParams.mPositiveButtonText = getString(R.string.yes);
        alertParams.mPositiveButtonListener = this;
        alertParams.mNegativeButtonText = getString(R.string.no);
        alertParams.mNegativeButtonListener = this;
        setupAlert();
    }

    private View createView() {
        TextView textView = new TextView(this);
        textView.setTextAppearance(textView.getContext(), R.style.TextAppearance.Medium);
        textView.setText(134545528);
        return textView;
    }

    protected void onResume() {
        super.onResume();
        registerReceiver(this.mReceiver, this.mSDCardStateFilter);
    }

    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause entry");
        unregisterReceiver(this.mReceiver);
    }

    private void onOK() {
        if (BenesseExtension.getDchaState() == 0) {
            Intent intent = new Intent();
            intent.setAction("android.settings.INTERNAL_STORAGE_SETTINGS");
            intent.setFlags(1409286144);
            Log.d(TAG, "onOK() start activity");
            startActivity(intent);
        }
        finish();
    }

    private void onCancel() {
        finish();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case -2:
                onCancel();
                break;
            case -1:
                onOK();
                break;
        }
    }
}
