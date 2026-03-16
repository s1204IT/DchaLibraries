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
    private static final boolean D = true;
    private static final String TAG = "BluetoothOppEnablingActivity";
    private static final boolean V = false;
    private boolean mRegistered = false;
    private final Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    BluetoothOppBtEnablingActivity.this.cancelSendingProgress();
                    break;
            }
        }
    };
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                switch (intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE)) {
                    case 12:
                        BluetoothOppBtEnablingActivity.this.mTimeoutHandler.removeMessages(0);
                        BluetoothOppBtEnablingActivity.this.finish();
                        break;
                }
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter.isEnabled()) {
            finish();
            return;
        }
        IntentFilter filter = new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED");
        registerReceiver(this.mBluetoothReceiver, filter);
        this.mRegistered = true;
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.enabling_progress_title);
        p.mView = createView();
        setupAlert();
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(0), 20000L);
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.bt_enabling_progress, (ViewGroup) null);
        TextView contentView = (TextView) view.findViewById(R.id.progress_info);
        contentView.setText(getString(R.string.enabling_progress_content));
        return view;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            Log.d(TAG, "onKeyDown() called; Key: back key");
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
        BluetoothOppManager mOppManager = BluetoothOppManager.getInstance(this);
        if (mOppManager.mSendingFlag) {
            mOppManager.mSendingFlag = false;
        }
        finish();
    }
}
