package com.android.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.settingslib.license.LicenseHtmlLoader;
import java.io.File;

public class SettingsLicenseActivity extends Activity implements LoaderManager.LoaderCallbacks<File> {
    @Override
    protected void onCreate(Bundle bundle) {
        ActionBar actionBar;
        super.onCreate(bundle);
        if (BenesseExtension.getDchaState() != 0 && (actionBar = getActionBar()) != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        String str = SystemProperties.get("ro.config.license_path", "/system/etc/NOTICE.html.gz");
        if (isFilePathValid(str)) {
            showSelectedFile(str);
        } else {
            showHtmlFromDefaultXmlFiles();
        }
    }

    @Override
    public Loader<File> onCreateLoader(int i, Bundle bundle) {
        return new LicenseHtmlLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<File> loader, File file) {
        showGeneratedHtmlFile(file);
    }

    @Override
    public void onLoaderReset(Loader<File> loader) {
    }

    private void showHtmlFromDefaultXmlFiles() {
        getLoaderManager().initLoader(0, Bundle.EMPTY, this);
    }

    Uri getUriFromGeneratedHtmlFile(File file) {
        return FileProvider.getUriForFile(this, "com.android.settings.files", file);
    }

    private void showGeneratedHtmlFile(File file) {
        if (file != null) {
            showHtmlFromUri(getUriFromGeneratedHtmlFile(file));
        } else {
            Log.e("SettingsLicenseActivity", "Failed to generate.");
            showErrorAndFinish();
        }
    }

    private void showSelectedFile(String str) {
        if (TextUtils.isEmpty(str)) {
            Log.e("SettingsLicenseActivity", "The system property for the license file is empty");
            showErrorAndFinish();
            return;
        }
        File file = new File(str);
        if (!isFileValid(file)) {
            Log.e("SettingsLicenseActivity", "License file " + str + " does not exist");
            showErrorAndFinish();
            return;
        }
        showHtmlFromUri(Uri.fromFile(file));
    }

    private void showHtmlFromUri(Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(uri, "text/html");
        intent.putExtra("android.intent.extra.TITLE", getString(R.string.settings_license_activity_title));
        if ("content".equals(uri.getScheme())) {
            intent.addFlags(1);
        }
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setPackage("com.android.htmlviewer");
        try {
            startActivity(intent);
            finish();
        } catch (ActivityNotFoundException e) {
            Log.e("SettingsLicenseActivity", "Failed to find viewer", e);
            showErrorAndFinish();
        }
    }

    private void showErrorAndFinish() {
        Toast.makeText(this, R.string.settings_license_activity_unavailable, 1).show();
        finish();
    }

    private boolean isFilePathValid(String str) {
        return !TextUtils.isEmpty(str) && isFileValid(new File(str));
    }

    boolean isFileValid(File file) {
        return file.exists() && file.length() != 0;
    }
}
