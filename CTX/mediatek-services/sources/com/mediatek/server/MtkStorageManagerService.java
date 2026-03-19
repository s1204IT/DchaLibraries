package com.mediatek.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.storage.VolumeInfo;
import android.util.Slog;
import com.android.server.StorageManagerService;
import com.google.android.collect.Lists;
import java.util.ArrayList;

class MtkStorageManagerService extends StorageManagerService {
    private static final Object FORMAT_LOCK = new Object();
    private static final String PRIVACY_PROTECTION_LOCK = "com.mediatek.ppl.NOTIFY_LOCK";
    private static final String PRIVACY_PROTECTION_UNLOCK = "com.mediatek.ppl.NOTIFY_UNLOCK";
    private static final String PRIVACY_PROTECTION_WIPE = "com.mediatek.ppl.NOTIFY_MOUNT_SERVICE_WIPE";
    private static final String PRIVACY_PROTECTION_WIPE_DONE = "com.mediatek.ppl.MOUNT_SERVICE_WIPE_RESPONSE";
    private static final String TAG = "MtkStorageManagerService";
    private final BroadcastReceiver mPrivacyProtectionReceiver;

    public MtkStorageManagerService(Context context) {
        super(context);
        this.mPrivacyProtectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals(MtkStorageManagerService.PRIVACY_PROTECTION_UNLOCK)) {
                    Slog.i(MtkStorageManagerService.TAG, "Privacy Protection unlock!");
                    return;
                }
                if (action.equals(MtkStorageManagerService.PRIVACY_PROTECTION_LOCK)) {
                    Slog.i(MtkStorageManagerService.TAG, "Privacy Protection lock!");
                } else if (action.equals(MtkStorageManagerService.PRIVACY_PROTECTION_WIPE)) {
                    Slog.i(MtkStorageManagerService.TAG, "Privacy Protection wipe!");
                    MtkStorageManagerService.this.formatPhoneStorageAndExternalSDCard();
                }
            }
        };
        registerPrivacyProtectionReceiver();
    }

    public static class MtkStorageManagerServiceLifecycle extends StorageManagerService.Lifecycle {
        public MtkStorageManagerServiceLifecycle(Context context) {
            super(context);
        }

        public void onStart() {
            this.mStorageManagerService = new MtkStorageManagerService(getContext());
            publishBinderService("mount", this.mStorageManagerService);
            this.mStorageManagerService.start();
        }
    }

    private void registerPrivacyProtectionReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PRIVACY_PROTECTION_LOCK);
        intentFilter.addAction(PRIVACY_PROTECTION_UNLOCK);
        intentFilter.addAction(PRIVACY_PROTECTION_WIPE);
        this.mContext.registerReceiver(this.mPrivacyProtectionReceiver, intentFilter, null, this.mHandler);
    }

    private ArrayList<VolumeInfo> findVolumeListNeedFormat() {
        Slog.i(TAG, "findVolumeListNeedFormat");
        ArrayList<VolumeInfo> arrayListNewArrayList = Lists.newArrayList();
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo volumeInfo = (VolumeInfo) this.mVolumes.valueAt(i);
                if ((!isUSBOTG(volumeInfo) && volumeInfo.isVisible() && volumeInfo.getType() == 0) || (volumeInfo.getType() == 1 && volumeInfo.getDiskId() != null)) {
                    arrayListNewArrayList.add(volumeInfo);
                    Slog.i(TAG, "i will try to format volume= " + volumeInfo);
                }
            }
        }
        return arrayListNewArrayList;
    }

    private void formatPhoneStorageAndExternalSDCard() {
        final ArrayList<VolumeInfo> arrayListFindVolumeListNeedFormat = findVolumeListNeedFormat();
        new Thread() {
            @Override
            public void run() {
                synchronized (MtkStorageManagerService.FORMAT_LOCK) {
                    int unused = MtkStorageManagerService.this.mCurrentUserId;
                    for (int i = 0; i < arrayListFindVolumeListNeedFormat.size(); i++) {
                        VolumeInfo volumeInfo = (VolumeInfo) arrayListFindVolumeListNeedFormat.get(i);
                        if (volumeInfo.getType() == 1 && volumeInfo.getDiskId() != null) {
                            Slog.i(MtkStorageManagerService.TAG, "use partition public to format, volume= " + volumeInfo);
                            MtkStorageManagerService.this.partitionPublic(volumeInfo.getDiskId());
                            if (volumeInfo.getFsUuid() != null) {
                                MtkStorageManagerService.this.forgetVolume(volumeInfo.getFsUuid());
                            }
                        } else {
                            if (volumeInfo.getState() == 1) {
                                Slog.i(MtkStorageManagerService.TAG, "volume is checking, wait..");
                                int i2 = 0;
                                while (true) {
                                    if (i2 >= 30) {
                                        break;
                                    }
                                    try {
                                        sleep(1000L);
                                    } catch (InterruptedException e) {
                                        Slog.e(MtkStorageManagerService.TAG, "Exception when wait!", e);
                                    }
                                    if (volumeInfo.getState() == 1) {
                                        i2++;
                                    } else {
                                        Slog.i(MtkStorageManagerService.TAG, "volume wait checking done!");
                                        break;
                                    }
                                }
                            }
                            if (volumeInfo.getState() == 2) {
                                Slog.i(MtkStorageManagerService.TAG, "volume is mounted, unmount firstly, volume=" + volumeInfo);
                                MtkStorageManagerService.this.unmount(volumeInfo.getId());
                                int i3 = 0;
                                while (true) {
                                    if (i3 >= 30) {
                                        break;
                                    }
                                    try {
                                        sleep(1000L);
                                    } catch (InterruptedException e2) {
                                        Slog.e(MtkStorageManagerService.TAG, "Exception when wait!", e2);
                                    }
                                    if (volumeInfo.getState() != 0) {
                                        i3++;
                                    } else {
                                        Slog.i(MtkStorageManagerService.TAG, "wait unmount done!");
                                        break;
                                    }
                                }
                            }
                            MtkStorageManagerService.this.format(volumeInfo.getId());
                            Slog.d(MtkStorageManagerService.TAG, "format Succeed! volume=" + volumeInfo);
                        }
                    }
                    Intent intent = new Intent(MtkStorageManagerService.PRIVACY_PROTECTION_WIPE_DONE);
                    MtkStorageManagerService.this.mContext.sendBroadcast(intent);
                    Slog.d(MtkStorageManagerService.TAG, "Privacy Protection wipe: send " + intent);
                }
            }
        }.start();
    }

    public boolean isUSBOTG(VolumeInfo volumeInfo) {
        String[] strArrSplit;
        String diskId = volumeInfo.getDiskId();
        if (diskId != null && (strArrSplit = diskId.split(":")) != null && strArrSplit.length == 2 && strArrSplit[1].startsWith("8,")) {
            Slog.d(TAG, "this is a usb otg");
            return true;
        }
        return false;
    }
}
