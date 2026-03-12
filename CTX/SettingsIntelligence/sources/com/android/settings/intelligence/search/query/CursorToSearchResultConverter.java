package com.android.settings.intelligence.search.query;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.BadParcelableException;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.intelligence.search.ResultPayload;
import com.android.settings.intelligence.search.ResultPayloadUtils;
import com.android.settings.intelligence.search.SearchResult;
import com.android.settings.intelligence.search.sitemap.SiteMapManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CursorToSearchResultConverter {
    private final int LONG_TITLE_LENGTH = 20;
    private final Context mContext;
    private static final String[] whiteList = {"main_toggle_wifi", "main_toggle_bluetooth", "main_toggle_bluetooth_obsolete", "toggle_airplane", "tether_settings", "battery_saver", "toggle_nfc", "restrict_background", "data_usage_enable", "button_roaming_key"};
    private static final Set<String> prioritySettings = new HashSet(Arrays.asList(whiteList));

    public CursorToSearchResultConverter(Context context) {
        this.mContext = context;
    }

    public Set<SearchResult> convertCursor(Cursor cursor, int i, SiteMapManager siteMapManager) {
        if (cursor == null) {
            return null;
        }
        HashMap map = new HashMap();
        HashSet hashSet = new HashSet();
        while (cursor.moveToNext()) {
            SearchResult searchResultBuildSingleSearchResultFromCursor = buildSingleSearchResultFromCursor(siteMapManager, map, cursor, i);
            if (searchResultBuildSingleSearchResultFromCursor != null) {
                hashSet.add(searchResultBuildSingleSearchResultFromCursor);
            }
        }
        return hashSet;
    }

    public static ResultPayload getUnmarshalledPayload(byte[] bArr, int i) {
        if (i == 0) {
            try {
                return (ResultPayload) ResultPayloadUtils.unmarshall(bArr, ResultPayload.CREATOR);
            } catch (BadParcelableException e) {
                Log.w("CursorConverter", "Error creating parcelable: " + e);
                return null;
            }
        }
        return null;
    }

    private SearchResult buildSingleSearchResultFromCursor(SiteMapManager siteMapManager, Map<String, Context> map, Cursor cursor, int i) {
        String string = cursor.getString(cursor.getColumnIndexOrThrow("package"));
        String string2 = cursor.getString(cursor.getColumnIndexOrThrow("data_title"));
        String string3 = cursor.getString(cursor.getColumnIndex("data_summary_on"));
        String string4 = cursor.getString(cursor.getColumnIndexOrThrow("data_key_reference"));
        String string5 = cursor.getString(cursor.getColumnIndexOrThrow("icon"));
        ResultPayload unmarshalledPayload = getUnmarshalledPayload(cursor.getBlob(cursor.getColumnIndexOrThrow("payload")), cursor.getInt(cursor.getColumnIndexOrThrow("payload_type")));
        List<String> breadcrumbs = getBreadcrumbs(siteMapManager, cursor);
        int rank = getRank(string2, i, string4);
        return new SearchResult.Builder().setDataKey(string4).setTitle(string2).setSummary(string3).addBreadcrumbs(breadcrumbs).setRank(rank).setIcon(getIconForPackage(map, string, string5)).setPayload(unmarshalledPayload).build();
    }

    private Drawable getIconForPackage(Map<String, Context> map, String str, String str2) {
        int i;
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        if (!TextUtils.isEmpty(str2)) {
            i = Integer.parseInt(str2);
        } else {
            i = 0;
        }
        if (i == 0) {
            return null;
        }
        Context contextCreatePackageContext = map.get(str);
        if (contextCreatePackageContext == null) {
            try {
                contextCreatePackageContext = this.mContext.createPackageContext(str, 0);
                map.put(str, contextCreatePackageContext);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("CursorConverter", "Cannot create Context for package: " + str);
                return null;
            }
        }
        try {
            Drawable drawable = contextCreatePackageContext.getDrawable(i);
            Log.d("CursorConverter", "Returning icon, id :" + i);
            return drawable;
        } catch (Resources.NotFoundException e2) {
            Log.w("CursorConverter", "Cannot get icon, pkg/id :" + str + "/" + i);
            return null;
        }
    }

    private List<String> getBreadcrumbs(SiteMapManager siteMapManager, Cursor cursor) {
        String string = cursor.getString(cursor.getColumnIndexOrThrow("screen_title"));
        String string2 = cursor.getString(cursor.getColumnIndexOrThrow("class_name"));
        if (siteMapManager == null) {
            return null;
        }
        return siteMapManager.buildBreadCrumb(this.mContext, string2, string);
    }

    private int getRank(String str, int i, String str2) {
        if (prioritySettings.contains(str2) && i < DatabaseResultTask.BASE_RANKS[1]) {
            return 0;
        }
        if (str.length() > 20) {
            return i + 1;
        }
        return i;
    }
}
