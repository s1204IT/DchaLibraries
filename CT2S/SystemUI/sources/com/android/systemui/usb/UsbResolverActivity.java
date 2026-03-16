package com.android.systemui.usb;

import android.R;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.widget.CheckBox;
import com.android.internal.app.ResolverActivity;
import java.util.ArrayList;

public class UsbResolverActivity extends ResolverActivity {
    private UsbAccessory mAccessory;
    private UsbDevice mDevice;
    private UsbDisconnectedReceiver mDisconnectedReceiver;

    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Parcelable targetParcelable = intent.getParcelableExtra("android.intent.extra.INTENT");
        if (!(targetParcelable instanceof Intent)) {
            Log.w("UsbResolverActivity", "Target is not an intent: " + targetParcelable);
            finish();
            return;
        }
        Intent target = (Intent) targetParcelable;
        ArrayList<ResolveInfo> rList = intent.getParcelableArrayListExtra("rlist");
        CharSequence title = getResources().getText(R.string.gnss_time_update_service);
        super.onCreate(savedInstanceState, target, title, (Intent[]) null, rList, true);
        CheckBox alwaysUse = (CheckBox) findViewById(R.id.default_activity_button);
        if (alwaysUse != null) {
            if (this.mDevice == null) {
                alwaysUse.setText(com.android.systemui.R.string.always_use_accessory);
            } else {
                alwaysUse.setText(com.android.systemui.R.string.always_use_device);
            }
        }
        this.mDevice = (UsbDevice) target.getParcelableExtra("device");
        if (this.mDevice != null) {
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mDevice);
            return;
        }
        this.mAccessory = (UsbAccessory) target.getParcelableExtra("accessory");
        if (this.mAccessory == null) {
            Log.e("UsbResolverActivity", "no device or accessory");
            finish();
        } else {
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mAccessory);
        }
    }

    protected void onDestroy() {
        if (this.mDisconnectedReceiver != null) {
            unregisterReceiver(this.mDisconnectedReceiver);
        }
        super.onDestroy();
    }

    protected void onIntentSelected(ResolveInfo ri, Intent intent, boolean alwaysCheck) {
        try {
            IBinder b = ServiceManager.getService("usb");
            IUsbManager service = IUsbManager.Stub.asInterface(b);
            int uid = ri.activityInfo.applicationInfo.uid;
            int userId = UserHandle.myUserId();
            if (this.mDevice != null) {
                service.grantDevicePermission(this.mDevice, uid);
                if (alwaysCheck) {
                    service.setDevicePackage(this.mDevice, ri.activityInfo.packageName, userId);
                } else {
                    service.setDevicePackage(this.mDevice, (String) null, userId);
                }
            } else if (this.mAccessory != null) {
                service.grantAccessoryPermission(this.mAccessory, uid);
                if (alwaysCheck) {
                    service.setAccessoryPackage(this.mAccessory, ri.activityInfo.packageName, userId);
                } else {
                    service.setAccessoryPackage(this.mAccessory, (String) null, userId);
                }
            }
            try {
                startActivityAsUser(intent, new UserHandle(userId));
            } catch (ActivityNotFoundException e) {
                Log.e("UsbResolverActivity", "startActivity failed", e);
            }
        } catch (RemoteException e2) {
            Log.e("UsbResolverActivity", "onIntentSelected failed", e2);
        }
    }
}
