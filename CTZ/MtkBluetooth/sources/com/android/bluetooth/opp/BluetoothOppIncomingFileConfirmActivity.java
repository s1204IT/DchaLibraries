package com.android.bluetooth.opp;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.android.bluetooth.R;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class BluetoothOppIncomingFileConfirmActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private static final int DISMISS_TIMEOUT_DIALOG = 0;
    private static final int DISMISS_TIMEOUT_DIALOG_VALUE = 2000;
    private static final String PREFERENCE_USER_TIMEOUT = "user_timeout";
    private static final String TAG = "BluetoothIncomingFileConfirmActivity";
    private BluetoothOppTransferInfo mTransInfo;
    private ContentValues mUpdateValues;
    private Uri mUri;
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;
    private boolean mTimeout = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothShare.USER_CONFIRMATION_TIMEOUT_ACTION.equals(intent.getAction())) {
                BluetoothOppIncomingFileConfirmActivity.this.onTimeout();
            }
        }
    };
    private final Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                if (BluetoothOppIncomingFileConfirmActivity.V) {
                    Log.v(BluetoothOppIncomingFileConfirmActivity.TAG, "Received DISMISS_TIMEOUT_DIALOG msg.");
                }
                BluetoothOppIncomingFileConfirmActivity.this.finish();
            }
        }
    };

    protected void onCreate(Bundle bundle) {
        setTheme(2131558410);
        if (V) {
            Log.d(TAG, "onCreate(): action = " + getIntent().getAction());
        }
        super.onCreate(bundle);
        this.mUri = getIntent().getData();
        this.mTransInfo = new BluetoothOppTransferInfo();
        this.mTransInfo = BluetoothOppUtility.queryRecord(this, this.mUri);
        if (this.mTransInfo == null) {
            if (V) {
                Log.e(TAG, "Error: Can not get data from db");
            }
            finish();
            return;
        }
        AlertController.AlertParams alertParams = ((AlertActivity) this).mAlertParams;
        alertParams.mTitle = getString(R.string.incoming_file_confirm_content);
        alertParams.mView = createView();
        alertParams.mPositiveButtonText = getString(R.string.incoming_file_confirm_ok);
        alertParams.mPositiveButtonListener = this;
        alertParams.mNegativeButtonText = getString(R.string.incoming_file_confirm_cancel);
        alertParams.mNegativeButtonListener = this;
        setupAlert();
        if (V) {
            Log.v(TAG, "mTimeout: " + this.mTimeout);
        }
        if (this.mTimeout) {
            onTimeout();
        }
        if (V) {
            Log.v(TAG, "BluetoothIncomingFileConfirmActivity: Got uri:" + this.mUri);
        }
        registerReceiver(this.mReceiver, new IntentFilter(BluetoothShare.USER_CONFIRMATION_TIMEOUT_ACTION));
    }

    private View createView() {
        View viewInflate = getLayoutInflater().inflate(R.layout.incoming_dialog, (ViewGroup) null);
        ((TextView) viewInflate.findViewById(R.id.from_content)).setText(this.mTransInfo.mDeviceName);
        ((TextView) viewInflate.findViewById(R.id.filename_content)).setText(this.mTransInfo.mFileName);
        ((TextView) viewInflate.findViewById(R.id.size_content)).setText(Formatter.formatFileSize(this, this.mTransInfo.mTotalBytes));
        return viewInflate;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case -2:
                this.mUpdateValues = new ContentValues();
                this.mUpdateValues.put("confirm", (Integer) 3);
                getContentResolver().update(this.mUri, this.mUpdateValues, null, null);
                break;
            case -1:
                if (!this.mTimeout) {
                    this.mUpdateValues = new ContentValues();
                    this.mUpdateValues.put("confirm", (Integer) 1);
                    getContentResolver().update(this.mUri, this.mUpdateValues, null, null);
                    Toast.makeText((Context) this, (CharSequence) getString(R.string.bt_toast_1), 0).show();
                }
                break;
        }
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 4) {
            if (D) {
                Log.d(TAG, "onKeyDown() called; Key: back key");
            }
            finish();
            return true;
        }
        return false;
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
    }

    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        this.mTimeout = bundle.getBoolean(PREFERENCE_USER_TIMEOUT);
        if (V) {
            Log.v(TAG, "onRestoreInstanceState() mTimeout: " + this.mTimeout);
        }
        if (this.mTimeout) {
            onTimeout();
        }
    }

    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (V) {
            Log.v(TAG, "onSaveInstanceState() mTimeout: " + this.mTimeout);
        }
        bundle.putBoolean(PREFERENCE_USER_TIMEOUT, this.mTimeout);
    }

    private void onTimeout() {
        this.mTimeout = true;
        ((AlertActivity) this).mAlert.setTitle(getString(R.string.incoming_file_confirm_timeout_content, this.mTransInfo.mDeviceName));
        ((AlertActivity) this).mAlert.getButton(-2).setVisibility(8);
        ((AlertActivity) this).mAlert.getButton(-1).setText(getString(R.string.incoming_file_confirm_timeout_ok));
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(0), 2000L);
    }
}
