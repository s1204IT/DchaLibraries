package com.android.bluetooth.opp;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.bluetooth.R;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class BluetoothOppBtEnablingActivity extends AlertActivity {
    private static final int BT_ENABLING_TIMEOUT = 0;
    private static final int BT_ENABLING_TIMEOUT_VALUE = 20000;
    private static final String TAG = "BluetoothOppEnablingActivity";
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;
    private boolean mRegistered = false;
    private final Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                if (BluetoothOppBtEnablingActivity.V) {
                    Log.v(BluetoothOppBtEnablingActivity.TAG, "Received BT_ENABLING_TIMEOUT msg.");
                }
                BluetoothOppBtEnablingActivity.this.cancelSendingProgress();
            }
        }
    };
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothOppBtEnablingActivity.V) {
                Log.v(BluetoothOppBtEnablingActivity.TAG, "Received intent: " + action);
            }
            if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED") && intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE) == 12) {
                BluetoothOppBtEnablingActivity.this.mTimeoutHandler.removeMessages(0);
                BluetoothOppBtEnablingActivity.this.finish();
            }
        }
    };

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            finish();
            return;
        }
        registerReceiver(this.mBluetoothReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
        this.mRegistered = true;
        AlertController.AlertParams alertParams = ((AlertActivity) this).mAlertParams;
        alertParams.mTitle = getString(R.string.enabling_progress_title);
        alertParams.mView = createView();
        setupAlert();
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(0), 20000L);
    }

    private View createView() {
        View viewInflate = getLayoutInflater().inflate(R.layout.bt_enabling_progress, (ViewGroup) null);
        ((TextView) viewInflate.findViewById(R.id.progress_info)).setText(getString(R.string.enabling_progress_content));
        return viewInflate;
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 4) {
            if (D) {
                Log.d(TAG, "onKeyDown() called; Key: back key");
            }
            this.mTimeoutHandler.removeMessages(0);
            cancelSendingProgress();
            return true;
        }
        return true;
    }

    protected void onDestroy() {
        super.onDestroy();
        if (this.mRegistered) {
            unregisterReceiver(this.mBluetoothReceiver);
        }
    }

    private void cancelSendingProgress() {
        BluetoothOppManager bluetoothOppManager = BluetoothOppManager.getInstance(this);
        if (bluetoothOppManager.mSendingFlag) {
            bluetoothOppManager.mSendingFlag = false;
        }
        finish();
    }
}
