package com.mediatek.camera.common;

import android.app.Activity;
import android.location.Location;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureProvider;
import com.mediatek.camera.common.location.LocationManager;
import com.mediatek.camera.common.relation.DataStore;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.relation.StatusMonitorFactory;
import com.mediatek.camera.common.setting.SettingManagerFactory;
import com.mediatek.camera.common.sound.ISoundPlayback;
import com.mediatek.camera.common.sound.SoundPlaybackImpl;
import com.mediatek.camera.common.storage.IStorageService;
import com.mediatek.camera.common.storage.MediaSaver;
import com.mediatek.camera.common.storage.StorageServiceImpl;
import com.mediatek.camera.common.thermal.ThermalThrottle;

public class CameraContext implements ICameraContext {
    private Activity mActivity;
    private CameraDeviceManagerFactory mCameraDeviceManagerFactory;
    private DataStore mDataStore;
    private FeatureProvider mFeatureProvider;
    private LocationManager mLocationManager;
    private MediaSaver mMediaSaver;
    private SettingManagerFactory mSettingManagerFactory;
    private SoundPlaybackImpl mSoundPlayback;
    private StatusMonitorFactory mStatusMonitorFactory;
    private StorageServiceImpl mStorageService;
    private ThermalThrottle mThermalThrottle;

    @Override
    public void create(IApp iApp, Activity activity) {
        this.mActivity = activity;
        this.mSoundPlayback = new SoundPlaybackImpl(activity);
        this.mLocationManager = new LocationManager(activity);
        this.mMediaSaver = new MediaSaver(activity);
        this.mDataStore = new DataStore(activity);
        this.mStatusMonitorFactory = new StatusMonitorFactory();
        this.mFeatureProvider = new FeatureProvider(iApp);
        this.mCameraDeviceManagerFactory = CameraDeviceManagerFactory.getInstance();
        this.mSettingManagerFactory = new SettingManagerFactory(iApp, this);
        this.mStorageService = new StorageServiceImpl(iApp, this);
        this.mThermalThrottle = new ThermalThrottle(iApp);
    }

    @Override
    public void resume() {
        this.mStorageService.resume();
        this.mLocationManager.recordLocation(true);
        this.mThermalThrottle.resume();
    }

    @Override
    public void pause() {
        this.mStorageService.pause();
        this.mLocationManager.recordLocation(false);
        this.mSoundPlayback.pause();
        this.mThermalThrottle.pause();
    }

    @Override
    public void destroy() {
        this.mSoundPlayback.release();
        this.mThermalThrottle.destroy();
    }

    @Override
    public DataStore getDataStore() {
        return this.mDataStore;
    }

    @Override
    public IStorageService getStorageService() {
        return this.mStorageService;
    }

    @Override
    public CameraDeviceManager getDeviceManager(CameraDeviceManagerFactory.CameraApi cameraApi) {
        return this.mCameraDeviceManagerFactory.getCameraDeviceManager(this.mActivity, cameraApi);
    }

    @Override
    public SettingManagerFactory getSettingManagerFactory() {
        return this.mSettingManagerFactory;
    }

    @Override
    public MediaSaver getMediaSaver() {
        return this.mMediaSaver;
    }

    @Override
    public FeatureProvider getFeatureProvider() {
        return this.mFeatureProvider;
    }

    @Override
    public StatusMonitor getStatusMonitor(String str) {
        return this.mStatusMonitorFactory.getStatusMonitor(str);
    }

    @Override
    public ISoundPlayback getSoundPlayback() {
        return this.mSoundPlayback;
    }

    @Override
    public Location getLocation() {
        return this.mLocationManager.getCurrentLocation();
    }
}
