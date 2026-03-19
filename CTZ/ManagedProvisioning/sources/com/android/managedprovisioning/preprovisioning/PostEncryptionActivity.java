package com.android.managedprovisioning.preprovisioning;

import android.app.Activity;
import android.os.Bundle;

public class PostEncryptionActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        EncryptionController.getInstance(this).resumeProvisioning();
        finish();
    }
}
