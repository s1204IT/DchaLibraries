package com.android.onetimeinitializer;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import java.net.URISyntaxException;
import java.util.Set;

public class OneTimeInitializerService extends IntentService {
    private SharedPreferences mPreferences;
    private static final String TAG = OneTimeInitializerService.class.getSimpleName().substring(0, 22);
    private static final Uri LAUNCHER_CONTENT_URI = Uri.parse("content://com.android.launcher2.settings/favorites?notify=true");

    public OneTimeInitializerService() {
        super("OneTimeInitializer Service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mPreferences = getSharedPreferences("oti", 0);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "OneTimeInitializerService.onHandleIntent");
        }
        int currentVersion = getMappingVersion();
        int newVersion = currentVersion;
        if (currentVersion < 1) {
            if (Log.isLoggable(TAG, 4)) {
                Log.i(TAG, "Updating to version 1.");
            }
            updateDialtactsLauncher();
            newVersion = 1;
        }
        updateMappingVersion(newVersion);
    }

    private int getMappingVersion() {
        return this.mPreferences.getInt("mapping_version", 0);
    }

    private void updateMappingVersion(int version) {
        SharedPreferences.Editor ed = this.mPreferences.edit();
        ed.putInt("mapping_version", version);
        ed.commit();
    }

    private void updateDialtactsLauncher() {
        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(LAUNCHER_CONTENT_URI, new String[]{"_id", "intent"}, null, null, null);
        if (c != null) {
            try {
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "Total launcher icons: " + c.getCount());
                }
                while (c.moveToNext()) {
                    long favoriteId = c.getLong(0);
                    String intentUri = c.getString(1);
                    if (intentUri != null) {
                        try {
                            Intent intent = Intent.parseUri(intentUri, 0);
                            ComponentName componentName = intent.getComponent();
                            Set<String> categories = intent.getCategories();
                            if ("android.intent.action.MAIN".equals(intent.getAction()) && componentName != null && "com.android.contacts".equals(componentName.getPackageName()) && "com.android.contacts.activities.DialtactsActivity".equals(componentName.getClassName()) && categories != null && categories.contains("android.intent.category.LAUNCHER")) {
                                ComponentName newName = new ComponentName("com.android.dialer", "com.android.dialer.DialtactsActivity");
                                intent.setComponent(newName);
                                ContentValues values = new ContentValues();
                                values.put("intent", intent.toUri(0));
                                String updateWhere = "_id=" + favoriteId;
                                cr.update(LAUNCHER_CONTENT_URI, values, updateWhere, null);
                                if (Log.isLoggable(TAG, 4)) {
                                    Log.i(TAG, "Updated " + componentName + " to " + newName);
                                }
                            }
                        } catch (RuntimeException ex) {
                            Log.e(TAG, "Problem moving Dialtacts activity", ex);
                        } catch (URISyntaxException e) {
                            Log.e(TAG, "Problem moving Dialtacts activity", e);
                        }
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
    }
}
