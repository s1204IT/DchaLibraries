package com.android.settings.deviceinfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.telecom.Log;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import com.android.internal.app.IMediaContainerService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class MigrateEstimateTask extends AsyncTask<Void, Void, Long> implements ServiceConnection {
    private static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName("com.android.defcontainer", "com.android.defcontainer.DefaultContainerService");
    private final Context mContext;
    private IMediaContainerService mService;
    private final StorageManager mStorage;
    private final CountDownLatch mConnected = new CountDownLatch(1);
    private long mSizeBytes = -1;

    public abstract void onPostExecute(String str, String str2);

    public MigrateEstimateTask(Context context) {
        this.mContext = context;
        this.mStorage = (StorageManager) context.getSystemService(StorageManager.class);
    }

    public void copyFrom(Intent intent) {
        this.mSizeBytes = intent.getLongExtra("size_bytes", -1L);
    }

    public void copyTo(Intent intent) {
        intent.putExtra("size_bytes", this.mSizeBytes);
    }

    @Override
    public Long doInBackground(Void... params) {
        if (this.mSizeBytes != -1) {
            return Long.valueOf(this.mSizeBytes);
        }
        VolumeInfo privateVol = this.mContext.getPackageManager().getPrimaryStorageCurrentVolume();
        VolumeInfo emulatedVol = this.mStorage.findEmulatedForPrivate(privateVol);
        if (emulatedVol == null) {
            Log.w("StorageSettings", "Failed to find current primary storage", new Object[0]);
            return -1L;
        }
        String path = emulatedVol.getPath().getAbsolutePath();
        Log.d("StorageSettings", "Estimating for current path " + path, new Object[0]);
        Intent intent = new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT);
        this.mContext.bindServiceAsUser(intent, this, 1, UserHandle.SYSTEM);
        try {
            try {
            } catch (Throwable th) {
                try {
                    this.mContext.unbindService(this);
                } catch (IllegalArgumentException e) {
                    Log.w("StorageSettings", "Already unbindService, just exit.", new Object[0]);
                }
                throw th;
            }
        } catch (RemoteException | InterruptedException e2) {
            Log.w("StorageSettings", "Failed to measure " + path, new Object[0]);
            try {
                this.mContext.unbindService(this);
            } catch (IllegalArgumentException e3) {
                Log.w("StorageSettings", "Already unbindService, just exit.", new Object[0]);
            }
        }
        if (!this.mConnected.await(15L, TimeUnit.SECONDS)) {
            try {
                this.mContext.unbindService(this);
            } catch (IllegalArgumentException e4) {
                Log.w("StorageSettings", "Already unbindService, just exit.", new Object[0]);
            }
            return -1L;
        }
        Long lValueOf = Long.valueOf(this.mService.calculateDirectorySize(path));
        try {
            this.mContext.unbindService(this);
        } catch (IllegalArgumentException e5) {
            Log.w("StorageSettings", "Already unbindService, just exit.", new Object[0]);
        }
        return lValueOf;
    }

    @Override
    public void onPostExecute(Long result) {
        this.mSizeBytes = result.longValue();
        long timeMillis = (this.mSizeBytes * 1000) / 10485760;
        long timeMillis2 = Math.max(timeMillis, 1000L);
        String size = Formatter.formatFileSize(this.mContext, this.mSizeBytes);
        String time = DateUtils.formatDuration(timeMillis2).toString();
        onPostExecute(size, time);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.mService = IMediaContainerService.Stub.asInterface(service);
        this.mConnected.countDown();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
}
