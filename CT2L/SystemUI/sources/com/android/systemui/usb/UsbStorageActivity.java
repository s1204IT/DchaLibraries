package com.android.systemui.usb;

import android.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.List;

public class UsbStorageActivity extends Activity implements DialogInterface.OnCancelListener, View.OnClickListener {
    private Handler mAsyncStorageHandler;
    private TextView mBanner;
    private boolean mDestroyed;
    private ImageView mIcon;
    private TextView mMessage;
    private Button mMountButton;
    private ProgressBar mProgressBar;
    private Handler mUIHandler;
    private Button mUnmountButton;
    private StorageManager mStorageManager = null;
    private BroadcastReceiver mUsbStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.hardware.usb.action.USB_STATE")) {
                UsbStorageActivity.this.handleUsbStateChanged(intent);
            }
        }
    };
    private StorageEventListener mStorageListener = new StorageEventListener() {
        public void onStorageStateChanged(String path, String oldState, String newState) {
            boolean on = newState.equals("shared");
            UsbStorageActivity.this.switchDisplay(on);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.mStorageManager == null) {
            this.mStorageManager = (StorageManager) getSystemService("storage");
            if (this.mStorageManager == null) {
                Log.w("UsbStorageActivity", "Failed to get StorageManager");
            }
        }
        this.mUIHandler = new Handler();
        HandlerThread thr = new HandlerThread("SystemUI UsbStorageActivity");
        thr.start();
        this.mAsyncStorageHandler = new Handler(thr.getLooper());
        getWindow().addFlags(4194304);
        if (Environment.isExternalStorageRemovable()) {
            getWindow().addFlags(524288);
        }
        setContentView(R.layout.notification_template_material_compact_heads_up_base);
        this.mIcon = (ImageView) findViewById(R.id.icon);
        this.mBanner = (TextView) findViewById(R.id.location);
        this.mMessage = (TextView) findViewById(R.id.message);
        this.mMountButton = (Button) findViewById(R.id.lock_screen);
        this.mMountButton.setOnClickListener(this);
        this.mUnmountButton = (Button) findViewById(R.id.locked);
        this.mUnmountButton.setOnClickListener(this);
        this.mProgressBar = (ProgressBar) findViewById(R.id.progress);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mDestroyed = true;
    }

    public void switchDisplay(final boolean usbStorageInUse) {
        this.mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                UsbStorageActivity.this.switchDisplayAsync(usbStorageInUse);
            }
        });
    }

    public void switchDisplayAsync(boolean usbStorageInUse) {
        if (usbStorageInUse) {
            this.mProgressBar.setVisibility(8);
            this.mUnmountButton.setVisibility(0);
            this.mMountButton.setVisibility(8);
            this.mIcon.setImageResource(R.drawable.perm_group_display);
            this.mBanner.setText(R.string.indeterminate_progress_32);
            this.mMessage.setText(R.string.indeterminate_progress_33);
            return;
        }
        this.mProgressBar.setVisibility(8);
        this.mUnmountButton.setVisibility(8);
        this.mMountButton.setVisibility(0);
        this.mIcon.setImageResource(R.drawable.perm_group_device_alarms);
        this.mBanner.setText(R.string.indeterminate_progress_24);
        this.mMessage.setText(R.string.indeterminate_progress_25);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mStorageManager.registerListener(this.mStorageListener);
        registerReceiver(this.mUsbStateReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        try {
            this.mAsyncStorageHandler.post(new Runnable() {
                @Override
                public void run() {
                    UsbStorageActivity.this.switchDisplay(UsbStorageActivity.this.mStorageManager.isUsbMassStorageEnabled());
                }
            });
        } catch (Exception ex) {
            Log.e("UsbStorageActivity", "Failed to read UMS enable state", ex);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.mUsbStateReceiver);
        if (this.mStorageManager == null && this.mStorageListener != null) {
            this.mStorageManager.unregisterListener(this.mStorageListener);
        }
    }

    public void handleUsbStateChanged(Intent intent) {
        boolean connected = intent.getExtras().getBoolean("connected");
        if (!connected) {
            finish();
        }
    }

    private IMountService getMountService() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IMountService.Stub.asInterface(service);
        }
        return null;
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case 1:
                return new AlertDialog.Builder(this).setTitle(R.string.indeterminate_progress_36).setPositiveButton(R.string.indeterminate_progress_39, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UsbStorageActivity.this.switchUsbMassStorage(true);
                    }
                }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setMessage(R.string.indeterminate_progress_37).setOnCancelListener(this).create();
            case 2:
                return new AlertDialog.Builder(this).setTitle(R.string.indeterminate_progress_38).setNeutralButton(R.string.indeterminate_progress_39, (DialogInterface.OnClickListener) null).setMessage(R.string.indeterminate_progress_27).setOnCancelListener(this).create();
            default:
                return null;
        }
    }

    private void scheduleShowDialog(final int id) {
        this.mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!UsbStorageActivity.this.mDestroyed) {
                    UsbStorageActivity.this.removeDialog(id);
                    UsbStorageActivity.this.showDialog(id);
                }
            }
        });
    }

    public void switchUsbMassStorage(final boolean on) {
        this.mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                UsbStorageActivity.this.mUnmountButton.setVisibility(8);
                UsbStorageActivity.this.mMountButton.setVisibility(8);
                UsbStorageActivity.this.mProgressBar.setVisibility(0);
            }
        });
        this.mAsyncStorageHandler.post(new Runnable() {
            @Override
            public void run() {
                if (on) {
                    UsbStorageActivity.this.mStorageManager.enableUsbMassStorage();
                } else {
                    UsbStorageActivity.this.mStorageManager.disableUsbMassStorage();
                }
            }
        });
    }

    private void checkStorageUsers() {
        this.mAsyncStorageHandler.post(new Runnable() {
            @Override
            public void run() {
                UsbStorageActivity.this.checkStorageUsersAsync();
            }
        });
    }

    public void checkStorageUsersAsync() {
        IMountService ims = getMountService();
        if (ims == null) {
            scheduleShowDialog(2);
        }
        String extStoragePath = Environment.getExternalStorageDirectory().toString();
        boolean showDialog = false;
        try {
            int[] stUsers = ims.getStorageUsers(extStoragePath);
            if (stUsers != null && stUsers.length > 0) {
                showDialog = true;
            } else {
                ActivityManager am = (ActivityManager) getSystemService("activity");
                List<ApplicationInfo> infoList = am.getRunningExternalApplications();
                if (infoList != null) {
                    if (infoList.size() > 0) {
                        showDialog = true;
                    }
                }
            }
        } catch (RemoteException e) {
            scheduleShowDialog(2);
        }
        if (showDialog) {
            scheduleShowDialog(1);
        } else {
            switchUsbMassStorage(true);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == this.mMountButton) {
            checkStorageUsers();
        } else if (v == this.mUnmountButton) {
            switchUsbMassStorage(false);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }
}
