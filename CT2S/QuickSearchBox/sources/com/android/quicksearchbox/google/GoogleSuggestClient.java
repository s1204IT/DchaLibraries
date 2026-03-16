package com.android.quicksearchbox.google;

import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.android.quicksearchbox.Config;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

public class GoogleSuggestClient extends AbstractGoogleSource {
    private static final String USER_AGENT = "Android/" + Build.VERSION.RELEASE;
    private final HttpClient mHttpClient;
    private String mSuggestUri;

    public GoogleSuggestClient(Context context, Handler uiThread, NamedTaskExecutor iconLoader, Config config) {
        super(context, uiThread, iconLoader);
        this.mHttpClient = AndroidHttpClient.newInstance(USER_AGENT, context);
        HttpParams params = this.mHttpClient.getParams();
        params.setLongParameter("http.conn-manager.timeout", config.getHttpConnectTimeout());
        this.mSuggestUri = null;
    }

    @Override
    public ComponentName getIntentComponent() {
        return new ComponentName(getContext(), (Class<?>) GoogleSearch.class);
    }

    @Override
    public SourceResult queryInternal(String query) {
        return query(query);
    }

    @Override
    public SourceResult queryExternal(String query) {
        return query(query);
    }

    private SourceResult query(String query) {
        if (TextUtils.isEmpty(query)) {
            return null;
        }
        if (!isNetworkConnected()) {
            Log.i("GoogleSearch", "Not connected to network.");
            return null;
        }
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            if (this.mSuggestUri == null) {
                Locale l = Locale.getDefault();
                String language = GoogleSearch.getLanguage(l);
                this.mSuggestUri = getContext().getResources().getString(R.string.google_suggest_base, language);
            }
            String suggestUri = this.mSuggestUri + encodedQuery;
            HttpGet method = new HttpGet(suggestUri);
            HttpResponse response = this.mHttpClient.execute(method);
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONArray results = new JSONArray(EntityUtils.toString(response.getEntity()));
                JSONArray suggestions = results.getJSONArray(1);
                JSONArray popularity = results.getJSONArray(2);
                return new GoogleSuggestCursor(this, query, suggestions, popularity);
            }
        } catch (UnsupportedEncodingException e) {
            Log.w("GoogleSearch", "Error", e);
        } catch (IOException e2) {
            Log.w("GoogleSearch", "Error", e2);
        } catch (JSONException e3) {
            Log.w("GoogleSearch", "Error", e3);
        }
        return null;
    }

    @Override
    public SuggestionCursor refreshShortcut(String shortcutId, String oldExtraData) {
        return null;
    }

    private boolean isNetworkConnected() {
        NetworkInfo networkInfo = getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivity = (ConnectivityManager) getContext().getSystemService("connectivity");
        if (connectivity == null) {
            return null;
        }
        return connectivity.getActiveNetworkInfo();
    }

    private static class GoogleSuggestCursor extends AbstractGoogleSourceResult {
        private final JSONArray mPopularity;
        private final JSONArray mSuggestions;

        public GoogleSuggestCursor(Source source, String userQuery, JSONArray suggestions, JSONArray popularity) {
            super(source, userQuery);
            this.mSuggestions = suggestions;
            this.mPopularity = popularity;
        }

        @Override
        public int getCount() {
            return this.mSuggestions.length();
        }

        @Override
        public String getSuggestionQuery() {
            try {
                return this.mSuggestions.getString(getPosition());
            } catch (JSONException e) {
                Log.w("GoogleSearch", "Error parsing response: " + e);
                return null;
            }
        }

        @Override
        public String getSuggestionText2() {
            try {
                return this.mPopularity.getString(getPosition());
            } catch (JSONException e) {
                Log.w("GoogleSearch", "Error parsing response: " + e);
                return null;
            }
        }
    }
}
