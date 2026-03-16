package com.android.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.android.gallery3d.util.IntentHelper;

public class CameraActivity extends Activity {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = IntentHelper.getCameraIntent(this);
        intent.setFlags(2097152);
        intent.setFlags(268435456);
        startActivity(intent);
        finish();
    }
}
