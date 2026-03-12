package com.android.settings.intelligence.search.indexing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class DatabaseIndexingUtils {
    public static Intent buildSearchTrampolineIntent(Context context, String str, String str2, String str3) {
        Intent intent = new Intent("com.android.settings.SEARCH_RESULT_TRAMPOLINE");
        intent.putExtra(":settings:show_fragment", str).putExtra(":settings:show_fragment_title", str3).putExtra(":settings:source_metrics", 34).putExtra(":settings:fragment_args_key", str2);
        return intent;
    }

    public static Intent buildDirectSearchResultIntent(String str, String str2, String str3, String str4) {
        Intent intentPutExtra = new Intent(str).putExtra(":settings:fragment_args_key", str4);
        if (!TextUtils.isEmpty(str2) && !TextUtils.isEmpty(str3)) {
            intentPutExtra.setComponent(new ComponentName(str2, str3));
        }
        return intentPutExtra;
    }
}
