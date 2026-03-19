package com.android.providers.media;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.usb.IUsbManager;
import android.mtp.MtpDatabase;
import android.mtp.MtpServer;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import com.android.providers.media.IMtpService;
import java.io.File;
import java.io.FileDescriptor;
import java.util.HashMap;
import java.util.Iterator;

public class MtpService extends Service {
    private static final String[] PTP_DIRECTORIES = {Environment.DIRECTORY_DCIM, Environment.DIRECTORY_PICTURES};

    @GuardedBy("MtpService.class")
    private static ServerHolder sServerHolder;

    @GuardedBy("this")
    private boolean mPtpMode;
    private StorageManager mStorageManager;

    @GuardedBy("this")
    private boolean mUnlocked;

    @GuardedBy("this")
    private HashMap<String, StorageVolume> mVolumeMap;

    @GuardedBy("this")
    private StorageVolume[] mVolumes;
    private final StorageEventListener mStorageEventListener = new StorageEventListener() {
        public void onStorageStateChanged(String str, String str2, String str3) {
            synchronized (MtpService.this) {
                Log.d("MtpService", "onStorageStateChanged " + str + " " + str2 + " -> " + str3);
                if ("mounted".equals(str3)) {
                    int i = 0;
                    while (true) {
                        if (i >= MtpService.this.mVolumes.length) {
                            break;
                        }
                        StorageVolume storageVolume = MtpService.this.mVolumes[i];
                        if (storageVolume.getPath().equals(str)) {
                            MtpService.this.mVolumeMap.put(str, storageVolume);
                            if (MtpService.this.mUnlocked && (storageVolume.isPrimary() || !MtpService.this.mPtpMode)) {
                                MtpService.this.addStorage(storageVolume);
                            }
                        } else {
                            i++;
                        }
                    }
                } else if ("mounted".equals(str2) && MtpService.this.mVolumeMap.containsKey(str)) {
                    MtpService.this.removeStorage((StorageVolume) MtpService.this.mVolumeMap.remove(str));
                }
            }
        }
    };
    private final IMtpService.Stub mBinder = new IMtpService.Stub() {
    };

    @Override
    public void onCreate() {
        this.mVolumes = StorageManager.getVolumeList(getUserId(), 0);
        this.mVolumeMap = new HashMap<>();
        this.mStorageManager = (StorageManager) getSystemService(StorageManager.class);
        this.mStorageManager.registerListener(this.mStorageEventListener);
    }

    @Override
    public void onDestroy() {
        this.mStorageManager.unregisterListener(this.mStorageEventListener);
        synchronized (MtpService.class) {
            if (sServerHolder != null) {
                sServerHolder.database.setServer((MtpServer) null);
            }
        }
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int i, int i2) {
        this.mUnlocked = intent.getBooleanExtra("unlocked", false);
        this.mPtpMode = intent.getBooleanExtra("ptp", false);
        for (StorageVolume storageVolume : this.mVolumes) {
            if (storageVolume.getState().equals("mounted")) {
                this.mVolumeMap.put(storageVolume.getPath(), storageVolume);
            }
        }
        String[] strArr = null;
        if (this.mPtpMode) {
            Environment.UserEnvironment userEnvironment = new Environment.UserEnvironment(getUserId());
            int length = PTP_DIRECTORIES.length;
            String[] strArr2 = new String[length];
            for (int i3 = 0; i3 < length; i3++) {
                File file = userEnvironment.buildExternalStoragePublicDirs(PTP_DIRECTORIES[i3])[0];
                file.mkdirs();
                strArr2[i3] = file.getName();
            }
            strArr = strArr2;
        }
        startServer(StorageManager.getPrimaryVolume(this.mVolumes), strArr);
        return 3;
    }

    private synchronized void startServer(StorageVolume storageVolume, String[] strArr) {
        ParcelFileDescriptor controlFd;
        FileDescriptor fileDescriptor;
        if (UserHandle.myUserId() != ActivityManager.getCurrentUser()) {
            return;
        }
        synchronized (MtpService.class) {
            if (sServerHolder != null) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("starting MTP server in ");
            sb.append(this.mPtpMode ? "PTP mode" : "MTP mode");
            sb.append(" with storage ");
            sb.append(storageVolume.getPath());
            sb.append(this.mUnlocked ? " unlocked" : "");
            sb.append(" as user ");
            sb.append(UserHandle.myUserId());
            Log.d("MtpService", sb.toString());
            MtpDatabase mtpDatabase = new MtpDatabase(this, "external", strArr);
            String serial = Build.getSerial();
            if ("unknown".equals(serial)) {
                serial = "????????";
            }
            String str = serial;
            try {
                controlFd = IUsbManager.Stub.asInterface(ServiceManager.getService("usb")).getControlFd(this.mPtpMode ? 16L : 4L);
            } catch (RemoteException e) {
                Log.e("MtpService", "Error communicating with UsbManager: " + e);
                controlFd = null;
            }
            if (controlFd == null) {
                Log.i("MtpService", "Couldn't get control FD!");
                fileDescriptor = null;
            } else {
                fileDescriptor = controlFd.getFileDescriptor();
            }
            MtpServer mtpServer = new MtpServer(mtpDatabase, fileDescriptor, this.mPtpMode, new OnServerTerminated(), Build.MANUFACTURER, Build.MODEL, "1.0", str);
            mtpDatabase.setServer(mtpServer);
            sServerHolder = new ServerHolder(mtpServer, mtpDatabase);
            if (this.mUnlocked) {
                if (this.mPtpMode) {
                    addStorage(storageVolume);
                } else {
                    Iterator<StorageVolume> it = this.mVolumeMap.values().iterator();
                    while (it.hasNext()) {
                        addStorage(it.next());
                    }
                }
            }
            mtpServer.start();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    private void addStorage(StorageVolume storageVolume) {
        Log.v("MtpService", "Adding MTP storage:" + storageVolume.getPath());
        synchronized (this) {
            if (sServerHolder != null) {
                sServerHolder.database.addStorage(storageVolume);
            }
        }
    }

    private void removeStorage(StorageVolume storageVolume) {
        synchronized (MtpService.class) {
            if (sServerHolder != null) {
                sServerHolder.database.removeStorage(storageVolume);
            }
        }
    }

    private static class ServerHolder {
        final MtpDatabase database;
        final MtpServer server;

        ServerHolder(MtpServer mtpServer, MtpDatabase mtpDatabase) {
            Preconditions.checkNotNull(mtpServer);
            Preconditions.checkNotNull(mtpDatabase);
            this.server = mtpServer;
            this.database = mtpDatabase;
        }

        void close() {
            this.database.setServer((MtpServer) null);
        }
    }

    private class OnServerTerminated implements Runnable {
        private OnServerTerminated() {
        }

        @Override
        public void run() {
            synchronized (MtpService.class) {
                if (MtpService.sServerHolder != null) {
                    MtpService.sServerHolder.close();
                    ServerHolder unused = MtpService.sServerHolder = null;
                } else {
                    Log.e("MtpService", "sServerHolder is unexpectedly null.");
                }
            }
        }
    }
}
