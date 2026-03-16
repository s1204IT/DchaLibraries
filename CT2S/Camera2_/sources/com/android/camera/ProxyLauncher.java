package com.android.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ProxyLauncher extends Activity {
    public static final int RESULT_USER_CANCELED = -2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Intent intent = (Intent) getIntent().getParcelableExtra("android.intent.extra.INTENT");
            startActivityForResult(intent, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0) {
            resultCode = -2;
        }
        setResult(resultCode, data);
        finish();
    }
}
