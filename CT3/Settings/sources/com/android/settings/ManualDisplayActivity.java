package com.android.settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import java.io.File;

public class ManualDisplayActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources resources = getResources();
        if (!resources.getBoolean(R.bool.config_show_manual)) {
            finish();
        }
        String path = SystemProperties.get("ro.config.manual_path", "/system/etc/MANUAL.html.gz");
        if (TextUtils.isEmpty(path)) {
            Log.e("SettingsManualActivity", "The system property for the manual is empty");
            showErrorAndFinish();
            return;
        }
        File file = new File(path);
        if (!file.exists() || file.length() == 0) {
            Log.e("SettingsManualActivity", "Manual file " + path + " does not exist");
            showErrorAndFinish();
            return;
        }
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(Uri.fromFile(file), "text/html");
        intent.putExtra("android.intent.extra.TITLE", getString(R.string.settings_manual_activity_title));
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setPackage("com.android.htmlviewer");
        try {
            int dcha_state = BenesseExtension.getDchaState();
            if (dcha_state == 0) {
                startActivity(intent);
            }
            finish();
        } catch (ActivityNotFoundException e) {
            Log.e("SettingsManualActivity", "Failed to find viewer", e);
            showErrorAndFinish();
        }
    }

    private void showErrorAndFinish() {
        Toast.makeText(this, R.string.settings_manual_activity_unavailable, 1).show();
        finish();
    }
}
