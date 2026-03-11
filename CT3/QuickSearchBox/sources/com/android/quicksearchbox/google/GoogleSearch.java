package com.android.quicksearchbox.google;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.quicksearchbox.QsbApplication;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class GoogleSearch extends Activity {
    private SearchBaseUrlHelper mSearchDomainHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;
        this.mSearchDomainHelper = QsbApplication.get(this).getSearchBaseUrlHelper();
        if ("android.intent.action.WEB_SEARCH".equals(action) || "android.intent.action.SEARCH".equals(action)) {
            handleWebSearchIntent(intent);
        }
        finish();
    }

    public static String getLanguage(Locale locale) {
        String language = locale.getLanguage();
        StringBuilder hl = new StringBuilder(language);
        String country = locale.getCountry();
        if (!TextUtils.isEmpty(country) && useLangCountryHl(language, country)) {
            hl.append('-');
            hl.append(country);
        }
        return hl.toString();
    }

    private static boolean useLangCountryHl(String language, String country) {
        if ("en".equals(language)) {
            return "GB".equals(country);
        }
        if ("zh".equals(language)) {
            if ("CN".equals(country)) {
                return true;
            }
            return "TW".equals(country);
        }
        if ("pt".equals(language)) {
            if ("BR".equals(country)) {
                return true;
            }
            return "PT".equals(country);
        }
        return false;
    }

    private void handleWebSearchIntent(Intent intent) {
        Intent launchUriIntent = createLaunchUriIntentFromSearchIntent(intent);
        PendingIntent pending = (PendingIntent) intent.getParcelableExtra("web_search_pendingintent");
        if (pending != null && launchPendingIntent(pending, launchUriIntent)) {
            return;
        }
        launchIntent(launchUriIntent);
    }

    private Intent createLaunchUriIntentFromSearchIntent(Intent intent) {
        String query = intent.getStringExtra("query");
        if (TextUtils.isEmpty(query)) {
            Log.w("GoogleSearch", "Got search intent with no query.");
            return null;
        }
        Bundle appSearchData = intent.getBundleExtra("app_data");
        String source = appSearchData != null ? appSearchData.getString("source") : "unknown";
        String applicationId = intent.getStringExtra("com.android.browser.application_id");
        if (applicationId == null) {
            applicationId = getPackageName();
        }
        try {
            String searchUri = this.mSearchDomainHelper.getSearchBaseUrl() + "&source=android-" + source + "&q=" + URLEncoder.encode(query, "UTF-8");
            Intent launchUriIntent = new Intent("android.intent.action.VIEW", Uri.parse(searchUri));
            launchUriIntent.putExtra("com.android.browser.application_id", applicationId);
            launchUriIntent.addFlags(268435456);
            return launchUriIntent;
        } catch (UnsupportedEncodingException e) {
            Log.w("GoogleSearch", "Error", e);
            return null;
        }
    }

    private void launchIntent(Intent intent) {
        try {
            Log.i("GoogleSearch", "Launching intent: " + intent.toUri(0));
            int dcha_state = Settings.System.getInt(getApplicationContext().getContentResolver(), "dcha_state", 0);
            if (dcha_state != 0) {
                return;
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w("GoogleSearch", "No activity found to handle: " + intent);
        }
    }

    private boolean launchPendingIntent(PendingIntent pending, Intent fillIn) {
        try {
            pending.send(this, -1, fillIn);
            return true;
        } catch (PendingIntent.CanceledException e) {
            Log.i("GoogleSearch", "Pending intent cancelled: " + pending);
            return false;
        }
    }
}
