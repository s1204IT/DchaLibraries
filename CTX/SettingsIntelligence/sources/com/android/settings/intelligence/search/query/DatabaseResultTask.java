package com.android.settings.intelligence.search.query;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.util.Pair;
import com.android.settings.intelligence.overlay.FeatureFactory;
import com.android.settings.intelligence.search.SearchFeatureProvider;
import com.android.settings.intelligence.search.SearchResult;
import com.android.settings.intelligence.search.indexing.IndexDatabaseHelper;
import com.android.settings.intelligence.search.query.SearchQueryTask;
import com.android.settings.intelligence.search.sitemap.SiteMapManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DatabaseResultTask extends SearchQueryTask.QueryWorker {
    public final String[] MATCH_COLUMNS_TERTIARY;
    private final CursorToSearchResultConverter mConverter;
    private final SearchFeatureProvider mFeatureProvider;
    public static final String[] SELECT_COLUMNS = {"data_title", "data_summary_on", "data_summary_off", "class_name", "screen_title", "icon", "intent_action", "package", "intent_target_package", "intent_target_class", "data_key_reference", "payload_type", "payload"};
    public static final String[] MATCH_COLUMNS_PRIMARY = {"data_title", "data_title_normalized"};
    public static final String[] MATCH_COLUMNS_SECONDARY = {"data_summary_on", "data_summary_on_normalized", "data_summary_off", "data_summary_off_normalized"};
    static final int[] BASE_RANKS = {1, 3, 7, 9};

    public static SearchQueryTask newTask(Context context, SiteMapManager siteMapManager, String str) {
        return new SearchQueryTask(new DatabaseResultTask(context, siteMapManager, str));
    }

    public DatabaseResultTask(Context context, SiteMapManager siteMapManager, String str) {
        super(context, siteMapManager, str);
        this.MATCH_COLUMNS_TERTIARY = new String[]{"data_keywords", "data_entries"};
        this.mConverter = new CursorToSearchResultConverter(context);
        this.mFeatureProvider = FeatureFactory.get(context).searchFeatureProvider();
    }

    @Override
    protected int getQueryWorkerId() {
        return 13;
    }

    @Override
    protected List<? extends SearchResult> query() {
        if (this.mQuery == null || this.mQuery.isEmpty()) {
            return new ArrayList();
        }
        FutureTask<List<Pair<String, Float>>> rankerTask = this.mFeatureProvider.getRankerTask(this.mContext, this.mQuery);
        if (rankerTask != null) {
            this.mFeatureProvider.getExecutorService().execute(rankerTask);
        }
        HashSet hashSet = new HashSet();
        hashSet.addAll(firstWordQuery(MATCH_COLUMNS_PRIMARY, BASE_RANKS[0]));
        hashSet.addAll(secondaryWordQuery(MATCH_COLUMNS_PRIMARY, BASE_RANKS[1]));
        hashSet.addAll(anyWordQuery(MATCH_COLUMNS_SECONDARY, BASE_RANKS[2]));
        hashSet.addAll(anyWordQuery(this.MATCH_COLUMNS_TERTIARY, BASE_RANKS[3]));
        if (rankerTask != null) {
            try {
                return getDynamicRankedResults(hashSet, rankerTask.get(this.mFeatureProvider.smartSearchRankingTimeoutMs(this.mContext), TimeUnit.MILLISECONDS));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Log.d("DatabaseResultTask", "Error waiting for result scores: " + e);
            }
        }
        ArrayList arrayList = new ArrayList(hashSet);
        Collections.sort(arrayList);
        return arrayList;
    }

    private Set<SearchResult> firstWordQuery(String[] strArr, int i) {
        return query(buildSingleWordWhereClause(strArr), buildSingleWordSelection(this.mQuery + "%", strArr.length), i);
    }

    private Set<SearchResult> secondaryWordQuery(String[] strArr, int i) {
        return query(buildSingleWordWhereClause(strArr), buildSingleWordSelection("% " + this.mQuery + "%", strArr.length), i);
    }

    private Set<SearchResult> anyWordQuery(String[] strArr, int i) {
        return query(buildTwoWordWhereClause(strArr), buildAnyWordSelection(strArr.length * 2), i);
    }

    private Set<SearchResult> query(String str, String[] strArr, int i) {
        Cursor cursorQuery = IndexDatabaseHelper.getInstance(this.mContext).getReadableDatabase().query("prefs_index", SELECT_COLUMNS, str, strArr, null, null, null);
        Throwable th = null;
        try {
            Set<SearchResult> setConvertCursor = this.mConverter.convertCursor(cursorQuery, i, this.mSiteMapManager);
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return setConvertCursor;
        } catch (Throwable th2) {
            if (cursorQuery != null) {
                if (0 != 0) {
                    try {
                        cursorQuery.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    cursorQuery.close();
                }
            }
            throw th2;
        }
    }

    private static String buildSingleWordWhereClause(String[] strArr) {
        StringBuilder sb = new StringBuilder(" (");
        int length = strArr.length;
        for (int i = 0; i < length; i++) {
            sb.append(strArr[i]);
            sb.append(" like ? ");
            if (i < length - 1) {
                sb.append(" OR ");
            }
        }
        sb.append(") AND enabled = 1");
        return sb.toString();
    }

    private static String buildTwoWordWhereClause(String[] strArr) {
        StringBuilder sb = new StringBuilder(" (");
        int length = strArr.length;
        for (int i = 0; i < length; i++) {
            sb.append(strArr[i]);
            sb.append(" like ? OR ");
            sb.append(strArr[i]);
            sb.append(" like ?");
            if (i < length - 1) {
                sb.append(" OR ");
            }
        }
        sb.append(") AND enabled = 1");
        return sb.toString();
    }

    private String[] buildSingleWordSelection(String str, int i) {
        String[] strArr = new String[i];
        for (int i2 = 0; i2 < i; i2++) {
            strArr[i2] = str;
        }
        return strArr;
    }

    private String[] buildAnyWordSelection(int i) {
        String[] strArr = new String[i];
        String str = this.mQuery + "%";
        String str2 = "% " + this.mQuery + "%";
        for (int i2 = 0; i2 < i - 1; i2 += 2) {
            strArr[i2] = str;
            strArr[i2 + 1] = str2;
        }
        return strArr;
    }

    private List<SearchResult> getDynamicRankedResults(Set<SearchResult> set, final List<Pair<String, Float>> list) {
        TreeSet treeSet = new TreeSet(new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult searchResult, SearchResult searchResult2) {
                if (DatabaseResultTask.this.getRankingScoreByKey(list, searchResult.dataKey).floatValue() > DatabaseResultTask.this.getRankingScoreByKey(list, searchResult2.dataKey).floatValue()) {
                    return -1;
                }
                return 1;
            }
        });
        treeSet.addAll(set);
        return new ArrayList(treeSet);
    }

    Float getRankingScoreByKey(List<Pair<String, Float>> list, String str) {
        for (Pair<String, Float> pair : list) {
            if (str.compareTo((String) pair.first) == 0) {
                return (Float) pair.second;
            }
        }
        Log.w("DatabaseResultTask", str + " was not in the ranking scores.");
        return Float.valueOf(-3.4028235E38f);
    }
}
