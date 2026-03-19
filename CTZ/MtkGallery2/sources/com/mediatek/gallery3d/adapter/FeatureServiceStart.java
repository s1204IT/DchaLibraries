package com.mediatek.gallery3d.adapter;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import com.mediatek.galleryportable.SystemPropertyUtils;
import java.io.File;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class FeatureServiceStart extends BroadcastReceiver {
    private static boolean START_SERVICE = new File(Environment.getExternalStorageDirectory(), "debug.gallery.startservice").exists();
    private static boolean GMO_PROJECT = SchemaSymbols.ATTVAL_TRUE_1.equals(SystemPropertyUtils.get("ro.vendor.mtk_gmo_ram_optimize"));

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("MtkGallery2/FeatureServiceStart", "<onReceive> IsLowRamDevice = " + isLowRamDevice(context) + " GMO_PROJECT = " + GMO_PROJECT);
        if (START_SERVICE) {
            Log.i("MtkGallery2/FeatureServiceStart", "<onReceive> START_SERVICE action = " + action);
            context.startService(new Intent(context, (Class<?>) FeatureService.class));
        }
    }

    private static boolean isLowRamDevice(Context context) {
        if (Build.VERSION.SDK_INT >= 19) {
            return ((ActivityManager) context.getSystemService("activity")).isLowRamDevice();
        }
        return SchemaSymbols.ATTVAL_TRUE.equals(SystemPropertyUtils.get("ro.config.low_ram", SchemaSymbols.ATTVAL_FALSE));
    }
}
