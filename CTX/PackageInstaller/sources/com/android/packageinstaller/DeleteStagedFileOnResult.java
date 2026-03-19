package com.android.packageinstaller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import java.io.File;

public class DeleteStagedFileOnResult extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle == null) {
            Intent intent = new Intent(getIntent());
            intent.setClass(this, PackageInstallerActivity.class);
            intent.setFlags(65536);
            startActivityForResult(intent, 0);
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        new File(getIntent().getData().getPath()).delete();
        setResult(i2, intent);
        finish();
    }
}
