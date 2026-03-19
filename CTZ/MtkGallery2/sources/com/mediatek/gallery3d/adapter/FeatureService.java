package com.mediatek.gallery3d.adapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.mediatek.gallerybasic.util.MediaUtils;

public class FeatureService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        Log.d("MtkGallery2/FeatureService", "<onStartCommand>");
        return 1;
    }

    @Override
    public void onCreate() {
        Log.d("MtkGallery2/FeatureService", "<onCreate>");
        super.onCreate();
        FeatureManager.setup(getApplicationContext());
        MediaUtils.getImageColumns(getApplicationContext());
        MediaUtils.getVideoColumns(getApplicationContext());
    }
}
