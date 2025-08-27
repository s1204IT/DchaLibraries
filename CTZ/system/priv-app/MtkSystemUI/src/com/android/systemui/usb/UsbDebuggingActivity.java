package com.android.systemui.usb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.IUsbManager;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.EventLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Toast;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.systemui.R;

/* loaded from: classes.dex */
public class UsbDebuggingActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private CheckBox mAlwaysAllow;
    private UsbDisconnectedReceiver mDisconnectedReceiver;
    private String mKey;

    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: com.android.systemui.usb.UsbDebuggingActivity */
    /* JADX WARN: Multi-variable type inference failed */
    public void onCreate(Bundle bundle) {
        Window window = getWindow();
        window.addPrivateFlags(524288);
        window.setType(2008);
        super.onCreate(bundle);
        if (SystemProperties.getInt("service.adb.tcp.port", 0) == 0) {
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver(this);
        }
        Intent intent = getIntent();
        String stringExtra = intent.getStringExtra("fingerprints");
        this.mKey = intent.getStringExtra("key");
        if (stringExtra == null || this.mKey == null) {
            finish();
            return;
        }
        AlertController.AlertParams alertParams = this.mAlertParams;
        alertParams.mTitle = getString(R.string.usb_debugging_title);
        alertParams.mMessage = getString(R.string.usb_debugging_message, new Object[]{stringExtra});
        alertParams.mPositiveButtonText = getString(android.R.string.ok);
        alertParams.mNegativeButtonText = getString(android.R.string.cancel);
        alertParams.mPositiveButtonListener = this;
        alertParams.mNegativeButtonListener = this;
        View viewInflate = LayoutInflater.from(alertParams.mContext).inflate(android.R.layout.alert_dialog_progress, (ViewGroup) null);
        this.mAlwaysAllow = (CheckBox) viewInflate.findViewById(android.R.id.accessibility_permissionDialog_title);
        this.mAlwaysAllow.setText(getString(R.string.usb_debugging_always));
        alertParams.mView = viewInflate;
        setupAlert();
        this.mAlert.getButton(-1).setOnTouchListener(new View.OnTouchListener() { // from class: com.android.systemui.usb.-$$Lambda$UsbDebuggingActivity$XWt--qGCtWBJlTLnAvCSF7AuSg8
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return UsbDebuggingActivity.lambda$onCreate$0(view, motionEvent);
            }
        });
    }

    static /* synthetic */ boolean lambda$onCreate$0(View view, MotionEvent motionEvent) {
        if ((motionEvent.getFlags() & 1) == 0 && (motionEvent.getFlags() & 2) == 0) {
            return false;
        }
        if (motionEvent.getAction() == 1) {
            EventLog.writeEvent(1397638484, "62187985");
            Toast.makeText(view.getContext(), R.string.touch_filtered_warning, 0).show();
        }
        return true;
    }

    public void onWindowAttributesChanged(WindowManager.LayoutParams layoutParams) {
        super.onWindowAttributesChanged(layoutParams);
    }

    private class UsbDisconnectedReceiver extends BroadcastReceiver {
        private final Activity mActivity;

        public UsbDisconnectedReceiver(Activity activity) {
            this.mActivity = activity;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.hardware.usb.action.USB_STATE".equals(intent.getAction()) && !intent.getBooleanExtra("connected", false)) {
                this.mActivity.finish();
            }
        }
    }

    public void onStart() {
        super.onStart();
        registerReceiver(this.mDisconnectedReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
    }

    protected void onStop() {
        if (this.mDisconnectedReceiver != null) {
            unregisterReceiver(this.mDisconnectedReceiver);
        }
        super.onStop();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        boolean z = false;
        boolean z2 = i == -1;
        if (z2 && this.mAlwaysAllow.isChecked()) {
            z = true;
        }
        try {
            IUsbManager iUsbManagerAsInterface = IUsbManager.Stub.asInterface(ServiceManager.getService("usb"));
            if (z2) {
                iUsbManagerAsInterface.allowUsbDebugging(z, this.mKey);
            } else {
                iUsbManagerAsInterface.denyUsbDebugging();
            }
        } catch (Exception e) {
            Log.e("UsbDebuggingActivity", "Unable to notify Usb service", e);
        }
        finish();
    }
}
