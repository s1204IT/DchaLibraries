package com.mediatek.camera.common.storage;

import android.content.Intent;

public interface IStorageService {

    public interface IStorageStateListener {
        void onStateChanged(int i, Intent intent);
    }

    long getCaptureStorageSpace();

    String getFileDirectory();

    long getRecordStorageSpace();

    void registerStorageStateListener(IStorageStateListener iStorageStateListener);

    void unRegisterStorageStateListener(IStorageStateListener iStorageStateListener);
}
