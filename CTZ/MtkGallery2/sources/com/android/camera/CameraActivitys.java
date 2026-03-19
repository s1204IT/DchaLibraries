package com.android.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.android.gallery3d.util.IntentHelper;
import com.mediatek.gallery3d.util.Log;

public class CameraActivitys extends Activity {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.e("CameraActivity2", "<onCreate> CameraActivity2 on create!");
        Intent cameraIntent = IntentHelper.getCameraIntent(this);
        cameraIntent.setFlags(2097152);
        startActivity(cameraIntent);
        finish();
    }
}
