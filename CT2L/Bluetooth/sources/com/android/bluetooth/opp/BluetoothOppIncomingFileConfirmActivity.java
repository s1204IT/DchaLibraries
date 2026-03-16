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
    private static final boolean D = true;
    private static final int DISMISS_TIMEOUT_DIALOG = 0;
    private static final int DISMISS_TIMEOUT_DIALOG_VALUE = 2000;
    private static final String PREFERENCE_USER_TIMEOUT = "user_timeout";
    private static final String TAG = "BluetoothIncomingFileConfirmActivity";
    private static final boolean V = false;
    private BluetoothOppTransferInfo mTransInfo;
    private ContentValues mUpdateValues;
    private Uri mUri;
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
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    BluetoothOppIncomingFileConfirmActivity.this.finish();
                    break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Material_Settings_Floating);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.mUri = intent.getData();
        this.mTransInfo = new BluetoothOppTransferInfo();
        this.mTransInfo = BluetoothOppUtility.queryRecord(this, this.mUri);
        if (this.mTransInfo == null) {
            finish();
            return;
        }
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.incoming_file_confirm_content);
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.incoming_file_confirm_ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.incoming_file_confirm_cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
        if (this.mTimeout) {
            onTimeout();
        }
        registerReceiver(this.mReceiver, new IntentFilter(BluetoothShare.USER_CONFIRMATION_TIMEOUT_ACTION));
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.incoming_dialog, (ViewGroup) null);
        ((TextView) view.findViewById(R.id.from_content)).setText(this.mTransInfo.mDeviceName);
        ((TextView) view.findViewById(R.id.filename_content)).setText(this.mTransInfo.mFileName);
        ((TextView) view.findViewById(R.id.size_content)).setText(Formatter.formatFileSize(this, this.mTransInfo.mTotalBytes));
        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                this.mUpdateValues = new ContentValues();
                this.mUpdateValues.put(BluetoothShare.USER_CONFIRMATION, (Integer) 3);
                getContentResolver().update(this.mUri, this.mUpdateValues, null, null);
                break;
            case -1:
                if (!this.mTimeout) {
                    this.mUpdateValues = new ContentValues();
                    this.mUpdateValues.put(BluetoothShare.USER_CONFIRMATION, (Integer) 1);
                    getContentResolver().update(this.mUri, this.mUpdateValues, null, null);
                    Toast.makeText((Context) this, (CharSequence) getString(R.string.bt_toast_1), 0).show();
                }
                break;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != 4) {
            return false;
        }
        Log.d(TAG, "onKeyDown() called; Key: back key");
        this.mUpdateValues = new ContentValues();
        this.mUpdateValues.put(BluetoothShare.VISIBILITY, (Integer) 1);
        getContentResolver().update(this.mUri, this.mUpdateValues, null, null);
        Toast.makeText((Context) this, (CharSequence) getString(R.string.bt_toast_2), 0).show();
        finish();
        return true;
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.mTimeout = savedInstanceState.getBoolean(PREFERENCE_USER_TIMEOUT);
        if (this.mTimeout) {
            onTimeout();
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PREFERENCE_USER_TIMEOUT, this.mTimeout);
    }

    private void onTimeout() {
        this.mTimeout = true;
        this.mAlert.setTitle(getString(R.string.incoming_file_confirm_timeout_content, new Object[]{this.mTransInfo.mDeviceName}));
        this.mAlert.getButton(-2).setVisibility(8);
        this.mAlert.getButton(-1).setText(getString(R.string.incoming_file_confirm_timeout_ok));
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(0), 2000L);
    }
}
