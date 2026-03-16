package com.android.quicksearchbox;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class SuggestionUtils {
    public static Intent getSuggestionIntent(SuggestionCursor suggestion, Bundle appSearchData) {
        String action = suggestion.getSuggestionIntentAction();
        String data = suggestion.getSuggestionIntentDataString();
        String query = suggestion.getSuggestionQuery();
        String userQuery = suggestion.getUserQuery();
        String extraData = suggestion.getSuggestionIntentExtraData();
        Intent intent = new Intent(action);
        intent.addFlags(268435456);
        intent.addFlags(67108864);
        if (data != null) {
            intent.setData(Uri.parse(data));
        }
        intent.putExtra("user_query", userQuery);
        if (query != null) {
            intent.putExtra("query", query);
        }
        if (extraData != null) {
            intent.putExtra("intent_extra_data_key", extraData);
        }
        if (appSearchData != null) {
            intent.putExtra("app_data", appSearchData);
        }
        intent.setComponent(suggestion.getSuggestionIntentComponent());
        return intent;
    }

    static String normalizeUrl(String url) {
        String normalized;
        int start;
        if (url != null) {
            int schemePos = url.indexOf("://");
            if (schemePos == -1) {
                normalized = "http://" + url;
                start = "http".length() + "://".length();
            } else {
                normalized = url;
                start = schemePos + "://".length();
            }
            int end = normalized.length();
            if (normalized.indexOf(47, start) == end - 1) {
                end--;
            }
            return normalized.substring(0, end);
        }
        return url;
    }
}
