package com.android.settings.backup;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class BackupSettingsActivity extends Activity implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.backup.BackupSettingsActivity.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
            searchIndexableRaw.title = context.getString(R.string.privacy_settings_title);
            searchIndexableRaw.screenTitle = context.getString(R.string.settings_label);
            searchIndexableRaw.keywords = context.getString(R.string.keywords_backup);
            searchIndexableRaw.intentTargetPackage = context.getPackageName();
            searchIndexableRaw.intentTargetClass = BackupSettingsActivity.class.getName();
            searchIndexableRaw.intentAction = "android.intent.action.MAIN";
            searchIndexableRaw.key = "backup";
            arrayList.add(searchIndexableRaw);
            return arrayList;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            if (UserHandle.myUserId() != 0) {
                if (Log.isLoggable("BackupSettingsActivity", 3)) {
                    Log.d("BackupSettingsActivity", "Not a system user, not indexing the screen");
                }
                nonIndexableKeys.add("backup");
            }
            return nonIndexableKeys;
        }
    };
    private FragmentManager mFragmentManager;

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        BackupSettingsHelper backupSettingsHelper = new BackupSettingsHelper(this);
        if (!backupSettingsHelper.isBackupProvidedByManufacturer()) {
            if (Log.isLoggable("BackupSettingsActivity", 3)) {
                Log.d("BackupSettingsActivity", "No manufacturer settings found, launching the backup settings directly");
            }
            Intent intentForBackupSettings = backupSettingsHelper.getIntentForBackupSettings();
            try {
                getPackageManager().setComponentEnabledSetting(intentForBackupSettings.getComponent(), 1, 1);
            } catch (SecurityException e) {
                Log.w("BackupSettingsActivity", "Trying to enable activity " + intentForBackupSettings.getComponent() + " but couldn't: " + e.getMessage());
            }
            startActivityForResult(intentForBackupSettings, 1);
            finish();
            return;
        }
        if (Log.isLoggable("BackupSettingsActivity", 3)) {
            Log.d("BackupSettingsActivity", "Manufacturer provided backup settings, showing the preference screen");
        }
        if (this.mFragmentManager == null) {
            this.mFragmentManager = getFragmentManager();
        }
        this.mFragmentManager.beginTransaction().replace(android.R.id.content, new BackupSettingsFragment()).commit();
    }

    void setFragmentManager(FragmentManager fragmentManager) {
        this.mFragmentManager = fragmentManager;
    }
}
