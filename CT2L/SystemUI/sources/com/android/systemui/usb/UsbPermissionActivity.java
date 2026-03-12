package com.android.systemui.usb;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.systemui.R;

public class UsbPermissionActivity extends AlertActivity implements DialogInterface.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private UsbAccessory mAccessory;
    private CheckBox mAlwaysUse;
    private TextView mClearDefaultHint;
    private UsbDevice mDevice;
    private UsbDisconnectedReceiver mDisconnectedReceiver;
    private String mPackageName;
    private PendingIntent mPendingIntent;
    private boolean mPermissionGranted;
    private int mUid;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        this.mDevice = (UsbDevice) intent.getParcelableExtra("device");
        this.mAccessory = (UsbAccessory) intent.getParcelableExtra("accessory");
        this.mPendingIntent = (PendingIntent) intent.getParcelableExtra("android.intent.extra.INTENT");
        this.mUid = intent.getIntExtra("android.intent.extra.UID", -1);
        this.mPackageName = intent.getStringExtra("package");
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo aInfo = packageManager.getApplicationInfo(this.mPackageName, 0);
            String appName = aInfo.loadLabel(packageManager).toString();
            AlertController.AlertParams ap = this.mAlertParams;
            ap.mIcon = aInfo.loadIcon(packageManager);
            ap.mTitle = appName;
            if (this.mDevice == null) {
                ap.mMessage = getString(R.string.usb_accessory_permission_prompt, new Object[]{appName});
                this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mAccessory);
            } else {
                ap.mMessage = getString(R.string.usb_device_permission_prompt, new Object[]{appName});
                this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mDevice);
            }
            ap.mPositiveButtonText = getString(android.R.string.ok);
            ap.mNegativeButtonText = getString(android.R.string.cancel);
            ap.mPositiveButtonListener = this;
            ap.mNegativeButtonListener = this;
            LayoutInflater inflater = (LayoutInflater) getSystemService("layout_inflater");
            ap.mView = inflater.inflate(android.R.layout.alert_dialog_button_bar_material, (ViewGroup) null);
            this.mAlwaysUse = (CheckBox) ap.mView.findViewById(android.R.id.default_activity_button);
            if (this.mDevice == null) {
                this.mAlwaysUse.setText(R.string.always_use_accessory);
            } else {
                this.mAlwaysUse.setText(R.string.always_use_device);
            }
            this.mAlwaysUse.setOnCheckedChangeListener(this);
            this.mClearDefaultHint = (TextView) ap.mView.findViewById(android.R.id.default_loading_view);
            this.mClearDefaultHint.setVisibility(8);
            setupAlert();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("UsbPermissionActivity", "unable to look up package name", e);
            finish();
        }
    }

    public void onDestroy() {
        IBinder b = ServiceManager.getService("usb");
        IUsbManager service = IUsbManager.Stub.asInterface(b);
        Intent intent = new Intent();
        try {
            if (this.mDevice != null) {
                intent.putExtra("device", this.mDevice);
                if (this.mPermissionGranted) {
                    service.grantDevicePermission(this.mDevice, this.mUid);
                    if (this.mAlwaysUse.isChecked()) {
                        int userId = UserHandle.getUserId(this.mUid);
                        service.setDevicePackage(this.mDevice, this.mPackageName, userId);
                    }
                }
            }
            if (this.mAccessory != null) {
                intent.putExtra("accessory", this.mAccessory);
                if (this.mPermissionGranted) {
                    service.grantAccessoryPermission(this.mAccessory, this.mUid);
                    if (this.mAlwaysUse.isChecked()) {
                        int userId2 = UserHandle.getUserId(this.mUid);
                        service.setAccessoryPackage(this.mAccessory, this.mPackageName, userId2);
                    }
                }
            }
            intent.putExtra("permission", this.mPermissionGranted);
            this.mPendingIntent.send((Context) this, 0, intent);
        } catch (PendingIntent.CanceledException e) {
            Log.w("UsbPermissionActivity", "PendingIntent was cancelled");
        } catch (RemoteException e2) {
            Log.e("UsbPermissionActivity", "IUsbService connection failed", e2);
        }
        if (this.mDisconnectedReceiver != null) {
            unregisterReceiver(this.mDisconnectedReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            this.mPermissionGranted = true;
        }
        finish();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (this.mClearDefaultHint != null) {
            if (isChecked) {
                this.mClearDefaultHint.setVisibility(0);
            } else {
                this.mClearDefaultHint.setVisibility(8);
            }
        }
    }
}
