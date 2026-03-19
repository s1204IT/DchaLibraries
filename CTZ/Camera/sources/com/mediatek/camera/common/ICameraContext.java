package com.mediatek.camera.common;

import android.app.Activity;
import android.location.Location;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureProvider;
import com.mediatek.camera.common.relation.DataStore;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.SettingManagerFactory;
import com.mediatek.camera.common.sound.ISoundPlayback;
import com.mediatek.camera.common.storage.IStorageService;
import com.mediatek.camera.common.storage.MediaSaver;

public interface ICameraContext {
    void create(IApp iApp, Activity activity);

    void destroy();

    DataStore getDataStore();

    CameraDeviceManager getDeviceManager(CameraDeviceManagerFactory.CameraApi cameraApi);

    FeatureProvider getFeatureProvider();

    Location getLocation();

    MediaSaver getMediaSaver();

    SettingManagerFactory getSettingManagerFactory();

    ISoundPlayback getSoundPlayback();

    StatusMonitor getStatusMonitor(String str);

    IStorageService getStorageService();

    void pause();

    void resume();
}
