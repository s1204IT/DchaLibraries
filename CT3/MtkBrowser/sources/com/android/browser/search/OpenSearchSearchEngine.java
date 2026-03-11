package com.android.browser.search;

import android.content.Context;
import android.content.Intent;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.browser.R;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import libcore.io.Streams;
import libcore.net.http.ResponseUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenSearchSearchEngine implements SearchEngine {
    private static final String[] COLUMNS = {"_id", "suggest_intent_query", "suggest_icon_1", "suggest_text_1", "suggest_text_2"};
    private static final String[] COLUMNS_WITHOUT_DESCRIPTION = {"_id", "suggest_intent_query", "suggest_icon_1", "suggest_text_1"};
    private final com.mediatek.common.search.SearchEngine mSearchEngine;

    public OpenSearchSearchEngine(Context context, com.mediatek.common.search.SearchEngine searchEngineInfo) {
        this.mSearchEngine = searchEngineInfo;
    }

    @Override
    public String getName() {
        return this.mSearchEngine.getName();
    }

    @Override
    public void startSearch(Context context, String query, Bundle appData, String extraData) {
        String uri = this.mSearchEngine.getSearchUriForQuery(query);
        if (uri == null) {
            Log.e("OpenSearchSearchEngine", "Unable to get search URI for " + this.mSearchEngine);
            return;
        }
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(uri));
        intent.setPackage(context.getPackageName());
        intent.addCategory("android.intent.category.DEFAULT");
        intent.putExtra("query", query);
        if (appData != null) {
            intent.putExtra("app_data", appData);
        }
        if (extraData != null) {
            intent.putExtra("intent_extra_data_key", extraData);
        }
        intent.putExtra("com.android.browser.application_id", context.getPackageName());
        context.startActivity(intent);
    }

    @Override
    public Cursor getSuggestions(Context context, String query) {
        JSONArray suggestions;
        JSONArray descriptions;
        if (TextUtils.isEmpty(query)) {
            return null;
        }
        if (!isNetworkConnected(context)) {
            Log.i("OpenSearchSearchEngine", "Not connected to network.");
            return null;
        }
        String suggestUri = this.mSearchEngine.getSuggestUriForQuery(query);
        if (TextUtils.isEmpty(suggestUri)) {
            return null;
        }
        try {
            String content = readUrl(suggestUri);
            if (content == null) {
                return null;
            }
            if (this.mSearchEngine.getName().equals("baidu")) {
                if (content.length() < 19) {
                    return null;
                }
                JSONObject obj = new JSONObject(content.substring(17, content.length() - 2));
                suggestions = obj.getJSONArray("s");
                descriptions = null;
            } else {
                JSONArray results = new JSONArray(content);
                suggestions = results.getJSONArray(1);
                descriptions = null;
                if (results.length() > 2) {
                    descriptions = results.getJSONArray(2);
                    if (descriptions.length() == 0) {
                        descriptions = null;
                    }
                }
            }
            return new SuggestionsCursor(suggestions, descriptions);
        } catch (JSONException e) {
            Log.w("OpenSearchSearchEngine", "Error", e);
            return null;
        }
    }

    public String readUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Android/1.0");
            urlConnection.setConnectTimeout(1000);
            if (urlConnection.getResponseCode() == 200) {
                try {
                    Charset responseCharset = ResponseUtils.responseCharset(urlConnection.getContentType());
                    byte[] responseBytes = Streams.readFully(urlConnection.getInputStream());
                    return new String(responseBytes, responseCharset);
                } catch (IllegalCharsetNameException icne) {
                    Log.i("OpenSearchSearchEngine", "Illegal response charset", icne);
                    return null;
                } catch (UnsupportedCharsetException ucse) {
                    Log.i("OpenSearchSearchEngine", "Unsupported response charset", ucse);
                    return null;
                }
            }
            Log.i("OpenSearchSearchEngine", "Suggestion request failed");
            return null;
        } catch (IOException e) {
            Log.w("OpenSearchSearchEngine", "Error", e);
            return null;
        }
    }

    @Override
    public boolean supportsSuggestions() {
        return this.mSearchEngine.supportsSuggestions();
    }

    private boolean isNetworkConnected(Context context) {
        NetworkInfo networkInfo = getActiveNetworkInfo(context);
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        return false;
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
                        return null;
                    }
                }
                if (column == 4) {
                    try {
                        return this.mDescriptions.getString(this.mPos);
                    } catch (JSONException e2) {
                        Log.w("OpenSearchSearchEngine", "Error", e2);
                        return null;
                    }
                }
                if (column == 2) {
                    return String.valueOf(R.drawable.magnifying_glass);
                }
                return null;
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
        return "OpenSearchSearchEngine{" + this.mSearchEngine + "}";
    }

    @Override
    public boolean wantsEmptyQuery() {
        return false;
    }
}
