package com.android.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.android.gallery3d.util.IntentHelper;

public class CameraActivity extends Activity {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent cameraIntent = IntentHelper.getCameraIntent(this);
        cameraIntent.setFlags(2097152);
        cameraIntent.setFlags(268435456);
        startActivity(cameraIntent);
        finish();
    }
}
