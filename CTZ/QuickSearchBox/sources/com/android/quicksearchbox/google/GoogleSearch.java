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

    private Intent createLaunchUriIntentFromSearchIntent(Intent intent) {
        String stringExtra = intent.getStringExtra("query");
        if (TextUtils.isEmpty(stringExtra)) {
            Log.w("GoogleSearch", "Got search intent with no query.");
            return null;
        }
        Bundle bundleExtra = intent.getBundleExtra("app_data");
        String string = bundleExtra != null ? bundleExtra.getString("source") : "unknown";
        String stringExtra2 = intent.getStringExtra("com.android.browser.application_id");
        if (stringExtra2 == null) {
            stringExtra2 = getPackageName();
        }
        try {
            Intent intent2 = new Intent("android.intent.action.VIEW", Uri.parse(this.mSearchDomainHelper.getSearchBaseUrl() + "&source=android-" + string + "&q=" + URLEncoder.encode(stringExtra, "UTF-8")));
            intent2.putExtra("com.android.browser.application_id", stringExtra2);
            intent2.addFlags(268435456);
            return intent2;
        } catch (UnsupportedEncodingException e) {
            Log.w("GoogleSearch", "Error", e);
            return null;
        }
    }

    public static String getLanguage(Locale locale) {
        String language = locale.getLanguage();
        StringBuilder sb = new StringBuilder(language);
        String country = locale.getCountry();
        if (!TextUtils.isEmpty(country) && useLangCountryHl(language, country)) {
            sb.append('-');
            sb.append(country);
        }
        return sb.toString();
    }

    private void handleWebSearchIntent(Intent intent) {
        Intent intentCreateLaunchUriIntentFromSearchIntent = createLaunchUriIntentFromSearchIntent(intent);
        PendingIntent pendingIntent = (PendingIntent) intent.getParcelableExtra("web_search_pendingintent");
        if (pendingIntent == null || !launchPendingIntent(pendingIntent, intentCreateLaunchUriIntentFromSearchIntent)) {
            launchIntent(intentCreateLaunchUriIntentFromSearchIntent);
        }
    }

    private void launchIntent(Intent intent) {
        try {
            Log.i("GoogleSearch", "Launching intent: " + intent.toUri(0));
            if (Settings.System.getInt(getApplicationContext().getContentResolver(), "dcha_state", 0) == 0) {
                startActivity(intent);
            }
        } catch (ActivityNotFoundException e) {
            Log.w("GoogleSearch", "No activity found to handle: " + intent);
        }
    }

    private boolean launchPendingIntent(PendingIntent pendingIntent, Intent intent) {
        try {
            pendingIntent.send(this, -1, intent);
            return true;
        } catch (PendingIntent.CanceledException e) {
            Log.i("GoogleSearch", "Pending intent cancelled: " + pendingIntent);
            return false;
        }
    }

    private static boolean useLangCountryHl(String str, String str2) {
        if ("en".equals(str)) {
            return "GB".equals(str2);
        }
        if ("zh".equals(str)) {
            return "CN".equals(str2) || "TW".equals(str2);
        }
        if ("pt".equals(str)) {
            return "BR".equals(str2) || "PT".equals(str2);
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;
        this.mSearchDomainHelper = QsbApplication.get(this).getSearchBaseUrlHelper();
        if ("android.intent.action.WEB_SEARCH".equals(action) || "android.intent.action.SEARCH".equals(action)) {
            handleWebSearchIntent(intent);
        }
        finish();
    }
}
