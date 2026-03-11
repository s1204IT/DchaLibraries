package com.mediatek.search;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.UserManager;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.List;

public class SearchExt implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<>();
            UserManager um = (UserManager) context.getSystemService("user");
            boolean isOwnerUser = um.isAdminUser();
            Log.d("SearchExt", "isOwnerUser =" + isOwnerUser);
            Intent intent = new Intent("com.android.settings.SCHEDULE_POWER_ON_OFF_SETTING");
            List<ResolveInfo> apps = context.getPackageManager().queryIntentActivities(intent, 0);
            if (apps != null && apps.size() != 0 && isOwnerUser) {
                Log.d("SearchExt", "schedule power on exist");
                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                indexable.title = context.getString(R.string.schedule_power_on_off_settings_title);
                indexable.intentAction = "com.android.settings.SCHEDULE_POWER_ON_OFF_SETTING";
                indexables.add(indexable);
            }
            return indexables;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> result = new ArrayList<>();
            return result;
        }
    };
}
