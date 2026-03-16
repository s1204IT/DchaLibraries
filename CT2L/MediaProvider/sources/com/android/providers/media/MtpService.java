package com.android.providers.media;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.mtp.MtpDatabase;
import android.mtp.MtpServer;
import android.mtp.MtpStorage;
import android.os.Environment;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import com.android.providers.media.IMtpService;
import java.io.File;
import java.util.HashMap;

public class MtpService extends Service {
    private static final String[] PTP_DIRECTORIES = {Environment.DIRECTORY_DCIM, Environment.DIRECTORY_PICTURES};
    private MtpDatabase mDatabase;
    private boolean mMtpDisabled;
    private boolean mPtpMode;
    private MtpServer mServer;
    private StorageManager mStorageManager;
    private StorageVolume[] mVolumes;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_PRESENT".equals(action)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (MtpService.this.mBinder) {
                            if (MtpService.this.mMtpDisabled) {
                                MtpService.this.addStorageDevicesLocked();
                                MtpService.this.mMtpDisabled = false;
                            }
                        }
                    }
                }, "addStorageDevices").start();
            }
        }
    };
    private final StorageEventListener mStorageEventListener = new StorageEventListener() {
        public void onStorageStateChanged(String path, String oldState, String newState) {
            StorageVolume volume;
            synchronized (MtpService.this.mBinder) {
                Log.d("MtpService", "onStorageStateChanged " + path + " " + oldState + " -> " + newState);
                if ("mounted".equals(newState)) {
                    MtpService.this.volumeMountedLocked(path);
                } else if ("mounted".equals(oldState) && (volume = (StorageVolume) MtpService.this.mVolumeMap.remove(path)) != null) {
                    MtpService.this.removeStorageLocked(volume);
                }
            }
        }
    };
    private final HashMap<String, StorageVolume> mVolumeMap = new HashMap<>();
    private final HashMap<String, MtpStorage> mStorageMap = new HashMap<>();
    private final IMtpService.Stub mBinder = new IMtpService.Stub() {
        @Override
        public void sendObjectAdded(int objectHandle) {
            synchronized (MtpService.this.mBinder) {
                if (MtpService.this.mServer != null) {
                    MtpService.this.mServer.sendObjectAdded(objectHandle);
                }
            }
        }

        @Override
        public void sendObjectRemoved(int objectHandle) {
            synchronized (MtpService.this.mBinder) {
                if (MtpService.this.mServer != null) {
                    MtpService.this.mServer.sendObjectRemoved(objectHandle);
                }
            }
        }
    };

    private void addStorageDevicesLocked() {
        if (this.mPtpMode) {
            StorageVolume primary = StorageManager.getPrimaryVolume(this.mVolumes);
            String path = primary.getPath();
            if (path != null) {
                String state = this.mStorageManager.getVolumeState(path);
                if ("mounted".equals(state)) {
                    addStorageLocked(this.mVolumeMap.get(path));
                    return;
                }
                return;
            }
            return;
        }
        for (StorageVolume volume : this.mVolumeMap.values()) {
            addStorageLocked(volume);
        }
    }

    @Override
    public void onCreate() {
        registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.USER_PRESENT"));
        this.mStorageManager = StorageManager.from(this);
        synchronized (this.mBinder) {
            updateDisabledStateLocked();
            this.mStorageManager.registerListener(this.mStorageEventListener);
            StorageVolume[] volumes = this.mStorageManager.getVolumeList();
            this.mVolumes = volumes;
            for (StorageVolume storageVolume : volumes) {
                String path = storageVolume.getPath();
                String state = this.mStorageManager.getVolumeState(path);
                if ("mounted".equals(state)) {
                    volumeMountedLocked(path);
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (this.mBinder) {
            updateDisabledStateLocked();
            this.mPtpMode = intent != null ? intent.getBooleanExtra("ptp", false) : false;
            String[] subdirs = null;
            if (this.mPtpMode) {
                int count = PTP_DIRECTORIES.length;
                subdirs = new String[count];
                for (int i = 0; i < count; i++) {
                    File file = Environment.getExternalStoragePublicDirectory(PTP_DIRECTORIES[i]);
                    file.mkdirs();
                    subdirs[i] = file.getPath();
                }
            }
            StorageVolume primary = StorageManager.getPrimaryVolume(this.mVolumes);
            if (this.mDatabase != null) {
                this.mDatabase.setServer((MtpServer) null);
            }
            this.mDatabase = new MtpDatabase(this, "external", primary.getPath(), subdirs);
            manageServiceLocked();
        }
        return 1;
    }

    private void updateDisabledStateLocked() {
        boolean z = true;
        boolean isCurrentUser = UserHandle.myUserId() == ActivityManager.getCurrentUser();
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService("keyguard");
        if ((!keyguardManager.isKeyguardLocked() || !keyguardManager.isKeyguardSecure()) && isCurrentUser) {
            z = false;
        }
        this.mMtpDisabled = z;
        Log.d("MtpService", "updating state; isCurrentUser=" + isCurrentUser + ", mMtpLocked=" + this.mMtpDisabled);
    }

    private void manageServiceLocked() {
        boolean isCurrentUser = UserHandle.myUserId() == ActivityManager.getCurrentUser();
        if (this.mServer == null && isCurrentUser) {
            Log.d("MtpService", "starting MTP server in " + (this.mPtpMode ? "PTP mode" : "MTP mode"));
            this.mServer = new MtpServer(this.mDatabase, this.mPtpMode);
            this.mDatabase.setServer(this.mServer);
            if (!this.mMtpDisabled) {
                addStorageDevicesLocked();
            }
            this.mServer.start();
            return;
        }
        if (this.mServer != null && !isCurrentUser) {
            Log.d("MtpService", "no longer current user; shutting down MTP server");
            this.mServer = null;
            this.mDatabase.setServer((MtpServer) null);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(this.mReceiver);
        this.mStorageManager.unregisterListener(this.mStorageEventListener);
        if (this.mDatabase != null) {
            this.mDatabase.setServer((MtpServer) null);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    private void volumeMountedLocked(String path) {
        for (int i = 0; i < this.mVolumes.length; i++) {
            StorageVolume volume = this.mVolumes[i];
            if (volume.getPath().equals(path)) {
                this.mVolumeMap.put(path, volume);
                if (!this.mMtpDisabled) {
                    if (volume.isPrimary() || !this.mPtpMode) {
                        addStorageLocked(volume);
                        return;
                    }
                    return;
                }
                return;
            }
        }
    }

    private void addStorageLocked(StorageVolume volume) {
        MtpStorage storage = new MtpStorage(volume, getApplicationContext());
        String path = storage.getPath();
        this.mStorageMap.put(path, storage);
        Log.d("MtpService", "addStorageLocked " + storage.getStorageId() + " " + path);
        if (this.mDatabase != null) {
            this.mDatabase.addStorage(storage);
        }
        if (this.mServer != null) {
            this.mServer.addStorage(storage);
        }
    }

    private void removeStorageLocked(StorageVolume volume) {
        MtpStorage storage = this.mStorageMap.remove(volume.getPath());
        if (storage == null) {
            Log.e("MtpService", "no MtpStorage for " + volume.getPath());
            return;
        }
        Log.d("MtpService", "removeStorageLocked " + storage.getStorageId() + " " + storage.getPath());
        if (this.mDatabase != null) {
            this.mDatabase.removeStorage(storage);
        }
        if (this.mServer != null) {
            this.mServer.removeStorage(storage);
        }
    }
}
