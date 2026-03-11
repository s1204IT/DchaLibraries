package com.android.browser.search;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.browser.R;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

public class OpenSearchSearchEngine implements SearchEngine {
    private static final String[] COLUMNS = {"_id", "suggest_intent_query", "suggest_icon_1", "suggest_text_1", "suggest_text_2"};
    private static final String[] COLUMNS_WITHOUT_DESCRIPTION = {"_id", "suggest_intent_query", "suggest_icon_1", "suggest_text_1"};
    private final AndroidHttpClient mHttpClient = AndroidHttpClient.newInstance("Android/1.0");
    private final SearchEngineInfo mSearchEngineInfo;

    public OpenSearchSearchEngine(Context context, SearchEngineInfo searchEngineInfo) {
        this.mSearchEngineInfo = searchEngineInfo;
        HttpParams params = this.mHttpClient.getParams();
        params.setLongParameter("http.connection-manager.timeout", 1000L);
    }

    @Override
    public String getName() {
        return this.mSearchEngineInfo.getName();
    }

    @Override
    public CharSequence getLabel() {
        return this.mSearchEngineInfo.getLabel();
    }

    @Override
    public void startSearch(Context context, String query, Bundle appData, String extraData) {
        String uri = this.mSearchEngineInfo.getSearchUriForQuery(query);
        if (uri == null) {
            Log.e("OpenSearchSearchEngine", "Unable to get search URI for " + this.mSearchEngineInfo);
            return;
        }
        Intent intent = new Intent("android.intent.action.WEB_SEARCH");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.putExtra("query", query);
        if (appData != null) {
            intent.putExtra("app_data", appData);
        }
        if (extraData != null) {
            intent.putExtra("intent_extra_data_key", extraData);
        }
        intent.putExtra("com.android.browser.application_id", context.getPackageName());
        Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(uri));
        viewIntent.addFlags(268435456);
        viewIntent.setPackage(context.getPackageName());
        PendingIntent pending = PendingIntent.getActivity(context, 0, viewIntent, 1073741824);
        intent.putExtra("web_search_pendingintent", pending);
        context.startActivity(intent);
    }

    @Override
    public Cursor getSuggestions(Context context, String query) {
        if (TextUtils.isEmpty(query)) {
            return null;
        }
        if (!isNetworkConnected(context)) {
            Log.i("OpenSearchSearchEngine", "Not connected to network.");
            return null;
        }
        String suggestUri = this.mSearchEngineInfo.getSuggestUriForQuery(query);
        if (TextUtils.isEmpty(suggestUri)) {
            return null;
        }
        try {
            String content = readUrl(suggestUri);
            if (content == null) {
                return null;
            }
            JSONArray results = new JSONArray(content);
            JSONArray suggestions = results.getJSONArray(1);
            JSONArray descriptions = null;
            if (results.length() > 2) {
                descriptions = results.getJSONArray(2);
                if (descriptions.length() == 0) {
                    descriptions = null;
                }
            }
            return new SuggestionsCursor(suggestions, descriptions);
        } catch (JSONException e) {
            Log.w("OpenSearchSearchEngine", "Error", e);
            return null;
        }
    }

    public String readUrl(String url) {
        String string = null;
        try {
            HttpGet method = new HttpGet(url);
            HttpResponse response = this.mHttpClient.execute(method);
            if (response.getStatusLine().getStatusCode() == 200) {
                string = EntityUtils.toString(response.getEntity());
            } else {
                Log.i("OpenSearchSearchEngine", "Suggestion request failed");
            }
        } catch (IOException e) {
            Log.w("OpenSearchSearchEngine", "Error", e);
        }
        return string;
    }

    @Override
    public boolean supportsSuggestions() {
        return this.mSearchEngineInfo.supportsSuggestions();
    }

    @Override
    public void close() {
        this.mHttpClient.close();
    }

    private boolean isNetworkConnected(Context context) {
        NetworkInfo networkInfo = getActiveNetworkInfo(context);
        return networkInfo != null && networkInfo.isConnected();
    }

    private NetworkInfo getActiveNetworkInfo(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService("connectivity");
        if (connectivity == null) {
            return null;
        }
        return connectivity.getActiveNetworkInfo();
    }

    private static class SuggestionsCursor extends AbstractCursor {
        private final JSONArray mDescriptions;
        private final JSONArray mSuggestions;

        public SuggestionsCursor(JSONArray suggestions, JSONArray descriptions) {
            this.mSuggestions = suggestions;
            this.mDescriptions = descriptions;
        }

        @Override
        public int getCount() {
            return this.mSuggestions.length();
        }

        @Override
        public String[] getColumnNames() {
            return this.mDescriptions != null ? OpenSearchSearchEngine.COLUMNS : OpenSearchSearchEngine.COLUMNS_WITHOUT_DESCRIPTION;
        }

        @Override
        public String getString(int column) {
            if (this.mPos != -1) {
                if (column == 1 || column == 3) {
                    try {
                        return this.mSuggestions.getString(this.mPos);
                    } catch (JSONException e) {
                        Log.w("OpenSearchSearchEngine", "Error", e);
                    }
                } else if (column == 4) {
                    try {
                        return this.mDescriptions.getString(this.mPos);
                    } catch (JSONException e2) {
                        Log.w("OpenSearchSearchEngine", "Error", e2);
                    }
                } else if (column == 2) {
                    return String.valueOf(R.drawable.magnifying_glass);
                }
            }
            return null;
        }

        @Override
        public double getDouble(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float getFloat(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInt(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLong(int column) {
            if (column == 0) {
                return this.mPos;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public short getShort(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNull(int column) {
            throw new UnsupportedOperationException();
        }
    }

    public String toString() {
        return "OpenSearchSearchEngine{" + this.mSearchEngineInfo + "}";
    }

    @Override
    public boolean wantsEmptyQuery() {
        return false;
    }
}
