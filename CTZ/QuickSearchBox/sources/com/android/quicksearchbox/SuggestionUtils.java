package com.android.quicksearchbox;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class SuggestionUtils {
    public static Intent getSuggestionIntent(SuggestionCursor suggestionCursor, Bundle bundle) {
        String suggestionIntentAction = suggestionCursor.getSuggestionIntentAction();
        String suggestionIntentDataString = suggestionCursor.getSuggestionIntentDataString();
        String suggestionQuery = suggestionCursor.getSuggestionQuery();
        String userQuery = suggestionCursor.getUserQuery();
        String suggestionIntentExtraData = suggestionCursor.getSuggestionIntentExtraData();
        Intent intent = new Intent(suggestionIntentAction);
        intent.addFlags(268435456);
        intent.addFlags(67108864);
        if (suggestionIntentDataString != null) {
            intent.setData(Uri.parse(suggestionIntentDataString));
        }
        intent.putExtra("user_query", userQuery);
        if (suggestionQuery != null) {
            intent.putExtra("query", suggestionQuery);
        }
        if (suggestionIntentExtraData != null) {
            intent.putExtra("intent_extra_data_key", suggestionIntentExtraData);
        }
        if (bundle != null) {
            intent.putExtra("app_data", bundle);
        }
        intent.setComponent(suggestionCursor.getSuggestionIntentComponent());
        return intent;
    }

    static String normalizeUrl(String str) {
        int length;
        if (str == null) {
            return str;
        }
        int iIndexOf = str.indexOf("://");
        if (iIndexOf == -1) {
            str = "http://" + str;
            length = "http".length() + "://".length();
        } else {
            length = iIndexOf + "://".length();
        }
        int length2 = str.length();
        return str.substring(0, str.indexOf(47, length) == length2 + (-1) ? length2 - 1 : length2);
    }
}
