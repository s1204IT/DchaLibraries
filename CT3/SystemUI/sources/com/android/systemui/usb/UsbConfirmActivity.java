package com.android.systemui.usb;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.IBinder;
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

public class UsbConfirmActivity extends AlertActivity implements DialogInterface.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private UsbAccessory mAccessory;
    private CheckBox mAlwaysUse;
    private TextView mClearDefaultHint;
    private UsbDevice mDevice;
    private UsbDisconnectedReceiver mDisconnectedReceiver;
    private ResolveInfo mResolveInfo;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        this.mDevice = (UsbDevice) intent.getParcelableExtra("device");
        this.mAccessory = (UsbAccessory) intent.getParcelableExtra("accessory");
        this.mResolveInfo = (ResolveInfo) intent.getParcelableExtra("rinfo");
        PackageManager packageManager = getPackageManager();
        String appName = this.mResolveInfo.loadLabel(packageManager).toString();
        AlertController.AlertParams ap = this.mAlertParams;
        ap.mIcon = this.mResolveInfo.loadIcon(packageManager);
        ap.mTitle = appName;
        if (this.mDevice == null) {
            ap.mMessage = getString(R.string.usb_accessory_confirm_prompt, new Object[]{appName});
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mAccessory);
        } else {
            ap.mMessage = getString(R.string.usb_device_confirm_prompt, new Object[]{appName});
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mDevice);
        }
        ap.mPositiveButtonText = getString(android.R.string.ok);
        ap.mNegativeButtonText = getString(android.R.string.cancel);
        ap.mPositiveButtonListener = this;
        ap.mNegativeButtonListener = this;
        LayoutInflater inflater = (LayoutInflater) getSystemService("layout_inflater");
        ap.mView = inflater.inflate(android.R.layout.alert_dialog_icon_button_watch, (ViewGroup) null);
        this.mAlwaysUse = (CheckBox) ap.mView.findViewById(android.R.id.flagServiceHandlesDoubleTap);
        if (this.mDevice == null) {
            this.mAlwaysUse.setText(R.string.always_use_accessory);
        } else {
            this.mAlwaysUse.setText(R.string.always_use_device);
        }
        this.mAlwaysUse.setOnCheckedChangeListener(this);
        this.mClearDefaultHint = (TextView) ap.mView.findViewById(android.R.id.floatType);
        this.mClearDefaultHint.setVisibility(8);
        setupAlert();
    }

    protected void onDestroy() {
        if (this.mDisconnectedReceiver != null) {
            unregisterReceiver(this.mDisconnectedReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            try {
                IBinder b = ServiceManager.getService("usb");
                IUsbManager service = IUsbManager.Stub.asInterface(b);
                int uid = this.mResolveInfo.activityInfo.applicationInfo.uid;
                int userId = UserHandle.myUserId();
                boolean alwaysUse = this.mAlwaysUse.isChecked();
                Intent intent = null;
                if (this.mDevice != null) {
                    intent = new Intent("android.hardware.usb.action.USB_DEVICE_ATTACHED");
                    intent.putExtra("device", this.mDevice);
                    service.grantDevicePermission(this.mDevice, uid);
                    if (alwaysUse) {
                        service.setDevicePackage(this.mDevice, this.mResolveInfo.activityInfo.packageName, userId);
                    } else {
                        service.setDevicePackage(this.mDevice, (String) null, userId);
                    }
                } else if (this.mAccessory != null) {
                    intent = new Intent("android.hardware.usb.action.USB_ACCESSORY_ATTACHED");
                    intent.putExtra("accessory", this.mAccessory);
                    service.grantAccessoryPermission(this.mAccessory, uid);
                    if (alwaysUse) {
                        service.setAccessoryPackage(this.mAccessory, this.mResolveInfo.activityInfo.packageName, userId);
                    } else {
                        service.setAccessoryPackage(this.mAccessory, (String) null, userId);
                    }
                }
                intent.addFlags(268435456);
                intent.setComponent(new ComponentName(this.mResolveInfo.activityInfo.packageName, this.mResolveInfo.activityInfo.name));
                startActivityAsUser(intent, new UserHandle(userId));
            } catch (Exception e) {
                Log.e("UsbConfirmActivity", "Unable to start activity", e);
            }
        }
        finish();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (this.mClearDefaultHint == null) {
            return;
        }
        if (isChecked) {
            this.mClearDefaultHint.setVisibility(0);
        } else {
            this.mClearDefaultHint.setVisibility(8);
        }
    }
}
