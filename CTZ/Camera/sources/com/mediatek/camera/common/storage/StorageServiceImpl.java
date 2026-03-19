package com.mediatek.camera.common.storage;

import android.content.Intent;
import android.net.Uri;
import com.mediatek.camera.common.CameraContext;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.storage.IStorageService;
import com.mediatek.camera.common.storage.MediaSaver;

public class StorageServiceImpl implements IStorageService {
    private final IApp mApp;
    private final IAppUi mAppUi;
    private final Storage mStorage;
    private IAppUi.HintInfo mStorageHint;
    private final StorageMonitor mStorageMonitor;
    private MediaSaver.MediaSaverListener mMediaSaverListener = new MediaSaver.MediaSaverListener() {
        @Override
        public void onFileSaved(Uri uri) {
            StorageServiceImpl.this.updateStorageHint();
        }
    };
    protected IStorageService.IStorageStateListener mStorageStateListener = new IStorageService.IStorageStateListener() {
        @Override
        public void onStateChanged(int i, Intent intent) {
            StorageServiceImpl.this.updateStorageHint();
        }
    };

    public StorageServiceImpl(IApp iApp, CameraContext cameraContext) {
        this.mStorage = Storage.getStorage(iApp.getActivity());
        this.mStorageMonitor = new StorageMonitor(iApp.getActivity(), this.mStorage);
        this.mStorage.updateDefaultDirectory();
        this.mApp = iApp;
        this.mAppUi = iApp.getAppUi();
        cameraContext.getMediaSaver().addMediaSaverListener(this.mMediaSaverListener);
        this.mStorageHint = new IAppUi.HintInfo();
        int identifier = iApp.getActivity().getResources().getIdentifier("hint_text_background", "drawable", iApp.getActivity().getPackageName());
        this.mStorageHint.mBackground = iApp.getActivity().getDrawable(identifier);
        this.mStorageHint.mType = IAppUi.HintType.TYPE_ALWAYS_BOTTOM;
    }

    public void resume() {
        updateStorageHint();
        this.mStorageMonitor.registerIntentFilter();
        this.mStorageMonitor.registerStorageStateListener(this.mStorageStateListener);
    }

    public void pause() {
        this.mAppUi.hideScreenHint(this.mStorageHint);
        this.mStorageMonitor.unRegisterStorageStateListener(this.mStorageStateListener);
        this.mStorageMonitor.unregisterIntentFilter();
    }

    @Override
    public String getFileDirectory() {
        return this.mStorage.getFileDirectory();
    }

    @Override
    public long getCaptureStorageSpace() {
        long availableSpace = this.mStorage.getAvailableSpace();
        if (availableSpace > this.mStorage.getCaptureThreshold()) {
            return availableSpace - this.mStorage.getCaptureThreshold();
        }
        if (availableSpace > 0) {
            return 0L;
        }
        return availableSpace;
    }

    @Override
    public long getRecordStorageSpace() {
        long availableSpace = this.mStorage.getAvailableSpace();
        if (availableSpace > this.mStorage.getRecordThreshold()) {
            return availableSpace - this.mStorage.getRecordThreshold();
        }
        if (availableSpace > 0) {
            return 0L;
        }
        return availableSpace;
    }

    @Override
    public void registerStorageStateListener(IStorageService.IStorageStateListener iStorageStateListener) {
        this.mStorageMonitor.registerStorageStateListener(iStorageStateListener);
    }

    @Override
    public void unRegisterStorageStateListener(IStorageService.IStorageStateListener iStorageStateListener) {
        this.mStorageMonitor.unRegisterStorageStateListener(iStorageStateListener);
    }

    private void updateStorageHint() {
        long jComputeStorage = computeStorage(this.mStorage.getAvailableSpace());
        if (jComputeStorage < 0) {
            int identifier = this.mApp.getActivity().getResources().getIdentifier("can_not_use_storage", "string", this.mApp.getActivity().getPackageName());
            this.mStorageHint.mHintText = this.mApp.getActivity().getString(identifier);
            this.mAppUi.showScreenHint(this.mStorageHint);
            return;
        }
        if (jComputeStorage == 0) {
            int identifier2 = this.mApp.getActivity().getResources().getIdentifier("storage_full", "string", this.mApp.getActivity().getPackageName());
            this.mStorageHint.mHintText = this.mApp.getActivity().getString(identifier2);
            this.mAppUi.showScreenHint(this.mStorageHint);
            return;
        }
        this.mAppUi.hideScreenHint(this.mStorageHint);
    }

    private long computeStorage(long j) {
        if (j > this.mStorage.getCaptureThreshold()) {
            return j - this.mStorage.getCaptureThreshold();
        }
        if (j > 0) {
            return 0L;
        }
        return j;
    }
}
