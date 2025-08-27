package com.android.systemui.usb;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.hardware.usb.AccessoryFilter;
import android.hardware.usb.DeviceFilter;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
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
import com.android.internal.util.XmlUtils;
import com.android.systemui.R;

/* loaded from: classes.dex */
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

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.systemui.usb.UsbPermissionActivity */
    /* JADX WARN: Multi-variable type inference failed */
    public void onCreate(Bundle bundle) throws PackageManager.NameNotFoundException {
        super.onCreate(bundle);
        Intent intent = getIntent();
        this.mDevice = (UsbDevice) intent.getParcelableExtra("device");
        this.mAccessory = (UsbAccessory) intent.getParcelableExtra("accessory");
        this.mPendingIntent = (PendingIntent) intent.getParcelableExtra("android.intent.extra.INTENT");
        this.mUid = intent.getIntExtra("android.intent.extra.UID", -1);
        this.mPackageName = intent.getStringExtra("package");
        PackageManager packageManager = getPackageManager();
        try {
            String string = packageManager.getApplicationInfo(this.mPackageName, 0).loadLabel(packageManager).toString();
            AlertController.AlertParams alertParams = this.mAlertParams;
            alertParams.mTitle = string;
            if (this.mDevice == null) {
                alertParams.mMessage = getString(R.string.usb_accessory_permission_prompt, new Object[]{string, this.mAccessory.getDescription()});
                this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mAccessory);
            } else {
                alertParams.mMessage = getString(R.string.usb_device_permission_prompt, new Object[]{string, this.mDevice.getProductName()});
                this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mDevice);
            }
            alertParams.mPositiveButtonText = getString(android.R.string.ok);
            alertParams.mNegativeButtonText = getString(android.R.string.cancel);
            alertParams.mPositiveButtonListener = this;
            alertParams.mNegativeButtonListener = this;
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(this.mPackageName, 129);
                if ((this.mDevice != null && canBeDefault(this.mDevice, packageInfo)) || (this.mAccessory != null && canBeDefault(this.mAccessory, packageInfo))) {
                    alertParams.mView = ((LayoutInflater) getSystemService("layout_inflater")).inflate(android.R.layout.alert_dialog_progress, (ViewGroup) null);
                    this.mAlwaysUse = (CheckBox) alertParams.mView.findViewById(android.R.id.accessibility_permissionDialog_title);
                    if (this.mDevice == null) {
                        this.mAlwaysUse.setText(getString(R.string.always_use_accessory, new Object[]{string, this.mAccessory.getDescription()}));
                    } else {
                        this.mAlwaysUse.setText(getString(R.string.always_use_device, new Object[]{string, this.mDevice.getProductName()}));
                    }
                    this.mAlwaysUse.setOnCheckedChangeListener(this);
                    this.mClearDefaultHint = (TextView) alertParams.mView.findViewById(android.R.id.async);
                    this.mClearDefaultHint.setVisibility(8);
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
            setupAlert();
        } catch (PackageManager.NameNotFoundException e2) {
            Log.e("UsbPermissionActivity", "unable to look up package name", e2);
            finish();
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [173=5] */
    private boolean canBeDefault(UsbDevice usbDevice, PackageInfo packageInfo) {
        ActivityInfo[] activityInfoArr = packageInfo.activities;
        if (activityInfoArr != null) {
            for (ActivityInfo activityInfo : activityInfoArr) {
                try {
                    XmlResourceParser xmlResourceParserLoadXmlMetaData = activityInfo.loadXmlMetaData(getPackageManager(), "android.hardware.usb.action.USB_DEVICE_ATTACHED");
                    Throwable th = null;
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        try {
                            XmlUtils.nextElement(xmlResourceParserLoadXmlMetaData);
                            while (xmlResourceParserLoadXmlMetaData.getEventType() != 1) {
                                if ("usb-device".equals(xmlResourceParserLoadXmlMetaData.getName()) && DeviceFilter.read(xmlResourceParserLoadXmlMetaData).matches(usbDevice)) {
                                    return true;
                                }
                                XmlUtils.nextElement(xmlResourceParserLoadXmlMetaData);
                            }
                            if (xmlResourceParserLoadXmlMetaData != null) {
                                $closeResource(null, xmlResourceParserLoadXmlMetaData);
                            }
                        } finally {
                            if (xmlResourceParserLoadXmlMetaData != null) {
                                $closeResource(th, xmlResourceParserLoadXmlMetaData);
                            }
                        }
                    } else if (xmlResourceParserLoadXmlMetaData != null) {
                        $closeResource(null, xmlResourceParserLoadXmlMetaData);
                    }
                } catch (Exception e) {
                    Log.w("UsbPermissionActivity", "Unable to load component info " + activityInfo.toString(), e);
                }
            }
        }
        return false;
    }

    private static /* synthetic */ void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [216=5] */
    private boolean canBeDefault(UsbAccessory usbAccessory, PackageInfo packageInfo) {
        ActivityInfo[] activityInfoArr = packageInfo.activities;
        if (activityInfoArr != null) {
            for (ActivityInfo activityInfo : activityInfoArr) {
                try {
                    XmlResourceParser xmlResourceParserLoadXmlMetaData = activityInfo.loadXmlMetaData(getPackageManager(), "android.hardware.usb.action.USB_ACCESSORY_ATTACHED");
                    Throwable th = null;
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        try {
                            XmlUtils.nextElement(xmlResourceParserLoadXmlMetaData);
                            while (xmlResourceParserLoadXmlMetaData.getEventType() != 1) {
                                if ("usb-accessory".equals(xmlResourceParserLoadXmlMetaData.getName()) && AccessoryFilter.read(xmlResourceParserLoadXmlMetaData).matches(usbAccessory)) {
                                    return true;
                                }
                                XmlUtils.nextElement(xmlResourceParserLoadXmlMetaData);
                            }
                            if (xmlResourceParserLoadXmlMetaData != null) {
                                $closeResource(null, xmlResourceParserLoadXmlMetaData);
                            }
                        } finally {
                            if (xmlResourceParserLoadXmlMetaData != null) {
                                $closeResource(th, xmlResourceParserLoadXmlMetaData);
                            }
                        }
                    } else if (xmlResourceParserLoadXmlMetaData != null) {
                        $closeResource(null, xmlResourceParserLoadXmlMetaData);
                    }
                } catch (Exception e) {
                    Log.w("UsbPermissionActivity", "Unable to load component info " + activityInfo.toString(), e);
                }
            }
        }
        return false;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: com.android.systemui.usb.UsbPermissionActivity */
    /* JADX WARN: Multi-variable type inference failed */
    public void onDestroy() throws PendingIntent.CanceledException {
        IUsbManager iUsbManagerAsInterface = IUsbManager.Stub.asInterface(ServiceManager.getService("usb"));
        Intent intent = new Intent();
        try {
            if (this.mDevice != null) {
                intent.putExtra("device", this.mDevice);
                if (this.mPermissionGranted) {
                    iUsbManagerAsInterface.grantDevicePermission(this.mDevice, this.mUid);
                    if (this.mAlwaysUse != null && this.mAlwaysUse.isChecked()) {
                        iUsbManagerAsInterface.setDevicePackage(this.mDevice, this.mPackageName, UserHandle.getUserId(this.mUid));
                    }
                }
            }
            if (this.mAccessory != null) {
                intent.putExtra("accessory", this.mAccessory);
                if (this.mPermissionGranted) {
                    iUsbManagerAsInterface.grantAccessoryPermission(this.mAccessory, this.mUid);
                    if (this.mAlwaysUse != null && this.mAlwaysUse.isChecked()) {
                        iUsbManagerAsInterface.setAccessoryPackage(this.mAccessory, this.mPackageName, UserHandle.getUserId(this.mUid));
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

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            this.mPermissionGranted = true;
        }
        finish();
    }

    @Override // android.widget.CompoundButton.OnCheckedChangeListener
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        if (this.mClearDefaultHint == null) {
            return;
        }
        if (z) {
            this.mClearDefaultHint.setVisibility(0);
        } else {
            this.mClearDefaultHint.setVisibility(8);
        }
    }
}
