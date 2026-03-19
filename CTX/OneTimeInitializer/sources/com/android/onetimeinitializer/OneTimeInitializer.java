package com.android.onetimeinitializer;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import java.net.URISyntaxException;
import java.util.Set;

public class OneTimeInitializer {
    private Context mContext;
    private SharedPreferences mPreferences;
    private static final String TAG = OneTimeInitializer.class.getSimpleName();
    private static final Uri LAUNCHER_CONTENT_URI = Uri.parse("content://com.android.launcher2.settings/favorites?notify=true");

    OneTimeInitializer(Context context) {
        this.mContext = context;
        this.mPreferences = this.mContext.getSharedPreferences("oti", 0);
    }

    void initialize() {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "OneTimeInitializer.initialize");
        }
        int mappingVersion = getMappingVersion();
        if (mappingVersion < 1) {
            if (Log.isLoggable(TAG, 4)) {
                Log.i(TAG, "Updating to version 1.");
            }
            updateDialtactsLauncher();
            mappingVersion = 1;
        }
        updateMappingVersion(mappingVersion);
    }

    private int getMappingVersion() {
        return this.mPreferences.getInt("mapping_version", 0);
    }

    private void updateMappingVersion(int i) {
        SharedPreferences.Editor editorEdit = this.mPreferences.edit();
        editorEdit.putInt("mapping_version", i);
        editorEdit.commit();
    }

    private void updateDialtactsLauncher() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        Cursor cursorQuery = contentResolver.query(LAUNCHER_CONTENT_URI, new String[]{"_id", "intent"}, null, null, null);
        if (cursorQuery == null) {
            return;
        }
        try {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Total launcher icons: " + cursorQuery.getCount());
            }
            while (cursorQuery.moveToNext()) {
                long j = cursorQuery.getLong(0);
                String string = cursorQuery.getString(1);
                if (string != null) {
                    try {
                        Intent uri = Intent.parseUri(string, 0);
                        ComponentName component = uri.getComponent();
                        Set<String> categories = uri.getCategories();
                        if ("android.intent.action.MAIN".equals(uri.getAction()) && component != null && "com.android.contacts".equals(component.getPackageName()) && "com.android.contacts.activities.DialtactsActivity".equals(component.getClassName()) && categories != null && categories.contains("android.intent.category.LAUNCHER")) {
                            ComponentName componentName = new ComponentName("com.android.dialer", "com.android.dialer.DialtactsActivity");
                            uri.setComponent(componentName);
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("intent", uri.toUri(0));
                            contentResolver.update(LAUNCHER_CONTENT_URI, contentValues, "_id=" + j, null);
                            if (Log.isLoggable(TAG, 4)) {
                                Log.i(TAG, "Updated " + component + " to " + componentName);
                            }
                        }
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Problem moving Dialtacts activity", e);
                    } catch (URISyntaxException e2) {
                        Log.e(TAG, "Problem moving Dialtacts activity", e2);
                    }
                }
            }
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }
}
