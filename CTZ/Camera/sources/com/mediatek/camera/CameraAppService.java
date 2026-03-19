package com.mediatek.camera;

import android.app.Service;
import android.content.Intent;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.IBinder;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.loader.FeatureLoader;
import com.mediatek.camera.common.mode.CameraApiHelper;

public class CameraAppService extends Service {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CameraAppService.class.getSimpleName());

    @Override
    public void onCreate() {
        super.onCreate();
        LogHelper.i(TAG, "[onCreate]");
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                CameraApiHelper.getCameraApiType(null);
                CameraApiHelper.getDeviceSpec(CameraAppService.this.getApplicationContext());
                FeatureLoader.loadBuildInFeatures(CameraAppService.this.getApplicationContext());
                ImageReader.newInstance(1, 1, 256, 1).close();
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogHelper.i(TAG, "[onDestroy]");
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        LogHelper.i(TAG, "[onStartCommand]");
        return 1;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
