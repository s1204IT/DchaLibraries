package com.android.packageinstaller;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.android.packageinstaller.PackageUtil;
import java.io.File;
import java.util.List;

public class InstallSuccess extends Activity {
    private static final String LOG_TAG = InstallSuccess.class.getSimpleName();

    @Override
    protected void onCreate(Bundle bundle) {
        PackageUtil.AppSnippet appSnippet;
        List<ResolveInfo> listQueryIntentActivities;
        super.onCreate(bundle);
        if (getIntent().getBooleanExtra("android.intent.extra.RETURN_RESULT", false)) {
            Intent intent = new Intent();
            intent.putExtra("android.intent.extra.INSTALL_RESULT", 1);
            setResult(-1, intent);
            finish();
            return;
        }
        Intent intent2 = getIntent();
        final ApplicationInfo applicationInfo = (ApplicationInfo) intent2.getParcelableExtra("com.android.packageinstaller.applicationInfo");
        Uri data = intent2.getData();
        setContentView(R.layout.install_success);
        PackageManager packageManager = getPackageManager();
        if ("package".equals(data.getScheme())) {
            appSnippet = new PackageUtil.AppSnippet(packageManager.getApplicationLabel(applicationInfo), packageManager.getApplicationIcon(applicationInfo));
        } else {
            appSnippet = PackageUtil.getAppSnippet(this, applicationInfo, new File(data.getPath()));
        }
        PackageUtil.initSnippetForNewApp(this, appSnippet, R.id.app_snippet);
        findViewById(R.id.done_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                InstallSuccess.lambda$onCreate$0(this.f$0, applicationInfo, view);
            }
        });
        final Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(applicationInfo.packageName);
        boolean z = (launchIntentForPackage == null || (listQueryIntentActivities = getPackageManager().queryIntentActivities(launchIntentForPackage, 0)) == null || listQueryIntentActivities.size() <= 0) ? false : true;
        Button button = (Button) findViewById(R.id.launch_button);
        if (z) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    InstallSuccess.lambda$onCreate$1(this.f$0, launchIntentForPackage, view);
                }
            });
        } else {
            button.setEnabled(false);
        }
    }

    public static void lambda$onCreate$0(InstallSuccess installSuccess, ApplicationInfo applicationInfo, View view) {
        if (applicationInfo.packageName != null) {
            Log.i(LOG_TAG, "Finished installing " + applicationInfo.packageName);
        }
        installSuccess.finish();
    }

    public static void lambda$onCreate$1(InstallSuccess installSuccess, Intent intent, View view) {
        try {
            installSuccess.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.e(LOG_TAG, "Could not start activity", e);
        }
        installSuccess.finish();
    }
}
