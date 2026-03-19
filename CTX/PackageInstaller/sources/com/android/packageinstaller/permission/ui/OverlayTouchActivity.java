package com.android.packageinstaller.permission.ui;

import android.app.Activity;
import android.os.Bundle;

public class OverlayTouchActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        getWindow().addPrivateFlags(524288);
        super.onCreate(bundle);
    }
}
