package com.android.internal.os.storage;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.R;

public class ExternalStorageFormatter extends Service implements DialogInterface.OnCancelListener {
    public static final ComponentName COMPONENT_NAME = new ComponentName(ZenModeConfig.SYSTEM_AUTHORITY, ExternalStorageFormatter.class.getName());
    public static final String EXTRA_ALWAYS_RESET = "always_reset";
    public static final String FORMAT_AND_FACTORY_RESET = "com.android.internal.os.storage.FORMAT_AND_FACTORY_RESET";
    public static final String FORMAT_ONLY = "com.android.internal.os.storage.FORMAT_ONLY";
    static final String TAG = "ExternalStorageFormatter";
    private StorageVolume mStorageVolume;
    private PowerManager.WakeLock mWakeLock;
    private IMountService mMountService = null;
    private StorageManager mStorageManager = null;
    private ProgressDialog mProgressDialog = null;
    private boolean mFactoryReset = false;
    private boolean mAlwaysReset = false;
    private String mReason = null;
    StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            Log.i(ExternalStorageFormatter.TAG, "Received storage state changed notification that " + path + " changed state from " + oldState + " to " + newState);
            ExternalStorageFormatter.this.updateProgressState();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (this.mStorageManager == null) {
            this.mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            this.mStorageManager.registerListener(this.mStorageListener);
        }
        this.mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(1, TAG);
        this.mWakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (FORMAT_AND_FACTORY_RESET.equals(intent.getAction())) {
            this.mFactoryReset = true;
        }
        if (intent.getBooleanExtra(EXTRA_ALWAYS_RESET, false)) {
            this.mAlwaysReset = true;
        }
        this.mReason = intent.getStringExtra(Intent.EXTRA_REASON);
        this.mStorageVolume = (StorageVolume) intent.getParcelableExtra(StorageVolume.EXTRA_STORAGE_VOLUME);
        if (this.mProgressDialog == null) {
            this.mProgressDialog = new ProgressDialog(this);
            this.mProgressDialog.setIndeterminate(true);
            this.mProgressDialog.setCancelable(true);
            this.mProgressDialog.getWindow().setType(2003);
            if (!this.mAlwaysReset) {
                this.mProgressDialog.setOnCancelListener(this);
            }
            updateProgressState();
            this.mProgressDialog.show();
            return 3;
        }
        return 3;
    }

    @Override
    public void onDestroy() {
        if (this.mStorageManager != null) {
            this.mStorageManager.unregisterListener(this.mStorageListener);
        }
        if (this.mProgressDialog != null) {
            this.mProgressDialog.dismiss();
        }
        this.mWakeLock.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        IMountService mountService = getMountService();
        String extStoragePath = this.mStorageVolume == null ? Environment.getLegacyExternalStorageDirectory().toString() : this.mStorageVolume.getPath();
        try {
            mountService.mountVolume(extStoragePath);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed talking with mount service", e);
        }
        stopSelf();
    }

    void fail(int msg) {
        Toast.makeText(this, msg, 1).show();
        if (this.mAlwaysReset) {
            Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
            intent.addFlags(268435456);
            intent.putExtra(Intent.EXTRA_REASON, this.mReason);
            sendBroadcast(intent);
        }
        stopSelf();
    }

    void updateProgressState() {
        String status = this.mStorageVolume == null ? Environment.getExternalStorageState() : this.mStorageManager.getVolumeState(this.mStorageVolume.getPath());
        if (Environment.MEDIA_MOUNTED.equals(status) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(status)) {
            updateProgressDialog(R.string.progress_unmounting);
            IMountService mountService = getMountService();
            String extStoragePath = this.mStorageVolume == null ? Environment.getLegacyExternalStorageDirectory().toString() : this.mStorageVolume.getPath();
            try {
                mountService.unmountVolume(extStoragePath, true, this.mFactoryReset);
                return;
            } catch (RemoteException e) {
                Log.w(TAG, "Failed talking with mount service", e);
                return;
            }
        }
        if (Environment.MEDIA_NOFS.equals(status) || Environment.MEDIA_UNMOUNTED.equals(status) || Environment.MEDIA_UNMOUNTABLE.equals(status)) {
            updateProgressDialog(R.string.progress_erasing);
            final IMountService mountService2 = getMountService();
            final String extStoragePath2 = this.mStorageVolume == null ? Environment.getLegacyExternalStorageDirectory().toString() : this.mStorageVolume.getPath();
            if (mountService2 != null) {
                new Thread() {
                    @Override
                    public void run() {
                        boolean success = false;
                        try {
                            mountService2.formatVolume(extStoragePath2);
                            success = true;
                        } catch (Exception e2) {
                            Toast.makeText(ExternalStorageFormatter.this, R.string.format_error, 1).show();
                        }
                        if (success && ExternalStorageFormatter.this.mFactoryReset) {
                            Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
                            intent.addFlags(268435456);
                            intent.putExtra(Intent.EXTRA_REASON, ExternalStorageFormatter.this.mReason);
                            ExternalStorageFormatter.this.sendBroadcast(intent);
                            ExternalStorageFormatter.this.stopSelf();
                            return;
                        }
                        if (!success && ExternalStorageFormatter.this.mAlwaysReset) {
                            Intent intent2 = new Intent(Intent.ACTION_MASTER_CLEAR);
                            intent2.addFlags(268435456);
                            intent2.putExtra(Intent.EXTRA_REASON, ExternalStorageFormatter.this.mReason);
                            ExternalStorageFormatter.this.sendBroadcast(intent2);
                        } else {
                            try {
                                mountService2.mountVolume(extStoragePath2);
                            } catch (RemoteException e3) {
                                Log.w(ExternalStorageFormatter.TAG, "Failed talking with mount service", e3);
                            }
                        }
                        ExternalStorageFormatter.this.stopSelf();
                    }
                }.start();
                return;
            } else {
                Log.w(TAG, "Unable to locate IMountService");
                return;
            }
        }
        if (Environment.MEDIA_BAD_REMOVAL.equals(status)) {
            fail(R.string.media_bad_removal);
            return;
        }
        if (Environment.MEDIA_CHECKING.equals(status)) {
            fail(R.string.media_checking);
            return;
        }
        if (Environment.MEDIA_REMOVED.equals(status)) {
            fail(R.string.media_removed);
        } else {
            if ("shared".equals(status)) {
                fail(R.string.media_shared);
                return;
            }
            fail(R.string.media_unknown_state);
            Log.w(TAG, "Unknown storage state: " + status);
            stopSelf();
        }
    }

    public void updateProgressDialog(int msg) {
        if (this.mProgressDialog == null) {
            this.mProgressDialog = new ProgressDialog(this);
            this.mProgressDialog.setIndeterminate(true);
            this.mProgressDialog.setCancelable(false);
            this.mProgressDialog.getWindow().setType(2003);
            this.mProgressDialog.show();
        }
        this.mProgressDialog.setMessage(getText(msg));
    }

    IMountService getMountService() {
        if (this.mMountService == null) {
            IBinder service = ServiceManager.getService("mount");
            if (service != null) {
                this.mMountService = IMountService.Stub.asInterface(service);
            } else {
                Log.e(TAG, "Can't get mount service");
            }
        }
        return this.mMountService;
    }
}
