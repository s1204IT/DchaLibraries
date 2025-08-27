package com.android.settings.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.search.Indexable;

/* loaded from: classes.dex */
public class DatabaseIndexingUtils {
    public static Intent buildSearchResultPageIntent(Context context, String str, String str2, String str3) {
        return buildSearchResultPageIntent(context, str, str2, str3, 34);
    }

    public static Intent buildSearchResultPageIntent(Context context, String str, String str2, String str3, int i) {
        Bundle bundle = new Bundle();
        bundle.putString(":settings:fragment_args_key", str2);
        Intent intent = new SubSettingLauncher(context).setDestination(str).setArguments(bundle).setTitle(str3).setSourceMetricsCategory(i).toIntent();
        intent.putExtra(":settings:fragment_args_key", str2).setAction("com.android.settings.SEARCH_RESULT_TRAMPOLINE").setComponent(null);
        return intent;
    }

    public static Indexable.SearchIndexProvider getSearchIndexProvider(Class<?> cls) {
        try {
            return (Indexable.SearchIndexProvider) cls.getField("SEARCH_INDEX_DATA_PROVIDER").get(null);
        } catch (IllegalAccessException e) {
            Log.d("IndexingUtil", "Illegal access to field 'SEARCH_INDEX_DATA_PROVIDER'");
            return null;
        } catch (IllegalArgumentException e2) {
            Log.d("IndexingUtil", "Illegal argument when accessing field 'SEARCH_INDEX_DATA_PROVIDER'");
            return null;
        } catch (NoSuchFieldException e3) {
            Log.d("IndexingUtil", "Cannot find field 'SEARCH_INDEX_DATA_PROVIDER'");
            return null;
        } catch (SecurityException e4) {
            Log.d("IndexingUtil", "Security exception for field 'SEARCH_INDEX_DATA_PROVIDER'");
            return null;
        }
    }
}
