package com.android.documentsui.files;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.android.documentsui.R;
import com.android.documentsui.base.SharedMinimal;
import java.util.Iterator;

public class LauncherActivity extends Activity {
    private static final String[] PERSISTENT_BOOLEAN_EXTRAS = {"android.content.extra.SHOW_ADVANCED"};

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        launch();
        finish();
    }

    private void launch() {
        ActivityManager activityManager = (ActivityManager) getSystemService("activity");
        Intent intentFindTask = findTask(activityManager);
        if (intentFindTask != null) {
            if (restoreTask(intentFindTask)) {
                return;
            } else {
                clearTask(activityManager);
            }
        }
        startTask();
    }

    private Intent findTask(ActivityManager activityManager) {
        Iterator<ActivityManager.AppTask> it = activityManager.getAppTasks().iterator();
        while (it.hasNext()) {
            Intent intent = it.next().getTaskInfo().baseIntent;
            if (isLaunchUri(intent.getData())) {
                return intent;
            }
        }
        return null;
    }

    private void startTask() {
        Intent intentCreateLaunchIntent = createLaunchIntent(this);
        intentCreateLaunchIntent.putExtra("com.android.documentsui.taskLabel", R.string.launcher_label);
        intentCreateLaunchIntent.putExtra("com.android.documentsui.taskIcon", R.drawable.launcher_icon);
        intentCreateLaunchIntent.setFlags(getIntent().getFlags());
        if (SharedMinimal.DEBUG) {
            Log.d("LauncherActivity", "Starting new task > " + intentCreateLaunchIntent.getData());
        }
        startActivity(intentCreateLaunchIntent);
    }

    private boolean restoreTask(Intent intent) {
        if (SharedMinimal.DEBUG) {
            Log.d("LauncherActivity", "Restoring existing task > " + intent.getData());
        }
        try {
            startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.w("LauncherActivity", "Failed to restore task > " + intent.getData() + ". Clear all existing tasks and start a new one.", e);
            return false;
        }
    }

    private void clearTask(ActivityManager activityManager) {
        Iterator<ActivityManager.AppTask> it = activityManager.getAppTasks().iterator();
        while (it.hasNext()) {
            it.next().finishAndRemoveTask();
        }
    }

    public static final Intent createLaunchIntent(Activity activity) {
        Intent intent = new Intent(activity, (Class<?>) FilesActivity.class);
        intent.setData(buildLaunchUri());
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        Intent intent2 = activity.getIntent();
        if (intent2 != null) {
            copyExtras(intent2, intent);
            if (intent2.hasExtra("android.intent.extra.TITLE")) {
                intent.putExtra("android.intent.extra.TITLE", intent2.getStringExtra("android.intent.extra.TITLE"));
            }
        }
        return intent;
    }

    private static void copyExtras(Intent intent, Intent intent2) {
        for (String str : PERSISTENT_BOOLEAN_EXTRAS) {
            if (intent.hasExtra(str)) {
                intent2.putExtra(str, intent.getBooleanExtra(str, false));
            }
        }
    }

    private static Uri buildLaunchUri() {
        return new Uri.Builder().authority("com.android.documentsui.launchControl").fragment(String.valueOf(System.currentTimeMillis())).build();
    }

    public static boolean isLaunchUri(Uri uri) {
        return uri != null && "com.android.documentsui.launchControl".equals(uri.getAuthority());
    }
}
