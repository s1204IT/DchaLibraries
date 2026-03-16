package com.android.systemui.usb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.IUsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.Toast;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.systemui.R;

public class UsbDebuggingActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private CheckBox mAlwaysAllow;
    private UsbDisconnectedReceiver mDisconnectedReceiver;
    private String mKey;

    public void onCreate(Bundle icicle) {
        Window window = getWindow();
        window.addPrivateFlags(524288);
        window.setType(2008);
        super.onCreate(icicle);
        if (SystemProperties.getInt("service.adb.tcp.port", 0) == 0) {
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver(this);
        }
        Intent intent = getIntent();
        String fingerprints = intent.getStringExtra("fingerprints");
        this.mKey = intent.getStringExtra("key");
        if (fingerprints == null || this.mKey == null) {
            finish();
            return;
        }
        AlertController.AlertParams ap = this.mAlertParams;
        ap.mTitle = getString(R.string.usb_debugging_title);
        ap.mMessage = getString(R.string.usb_debugging_message, new Object[]{fingerprints});
        ap.mPositiveButtonText = getString(android.R.string.ok);
        ap.mNegativeButtonText = getString(android.R.string.cancel);
        ap.mPositiveButtonListener = this;
        ap.mNegativeButtonListener = this;
        LayoutInflater inflater = LayoutInflater.from(ap.mContext);
        View checkbox = inflater.inflate(android.R.layout.alert_dialog_button_bar_material, (ViewGroup) null);
        this.mAlwaysAllow = (CheckBox) checkbox.findViewById(android.R.id.default_activity_button);
        this.mAlwaysAllow.setText(getString(R.string.usb_debugging_always));
        ap.mView = checkbox;
        setupAlert();
        View.OnTouchListener filterTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getFlags() & 1) == 0) {
                    return false;
                }
                if (event.getAction() != 1) {
                    return true;
                }
                Toast.makeText(v.getContext(), R.string.touch_filtered_warning, 0).show();
                return true;
            }
        };
        this.mAlert.getButton(-1).setOnTouchListener(filterTouchListener);
    }

    private class UsbDisconnectedReceiver extends BroadcastReceiver {
        private final Activity mActivity;

        public UsbDisconnectedReceiver(Activity activity) {
            this.mActivity = activity;
        }

        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if ("android.hardware.usb.action.USB_STATE".equals(action)) {
                boolean connected = intent.getBooleanExtra("connected", false);
                if (!connected) {
                    this.mActivity.finish();
                }
            }
        }
    }

    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_STATE");
        registerReceiver(this.mDisconnectedReceiver, filter);
    }

    protected void onStop() {
        if (this.mDisconnectedReceiver != null) {
            unregisterReceiver(this.mDisconnectedReceiver);
        }
        super.onStop();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        boolean allow = which == -1;
        boolean alwaysAllow = allow && this.mAlwaysAllow.isChecked();
        try {
            IBinder b = ServiceManager.getService("usb");
            IUsbManager service = IUsbManager.Stub.asInterface(b);
            if (allow) {
                service.allowUsbDebugging(alwaysAllow, this.mKey);
            } else {
                service.denyUsbDebugging();
            }
        } catch (Exception e) {
            Log.e("UsbDebuggingActivity", "Unable to notify Usb service", e);
        }
        finish();
    }
}
