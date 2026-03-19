package com.android.contacts.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.webkit.WebView;
import com.android.contacts.R;

public class LicenseActivity extends AppCompatActivity {
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.licenses);
        this.mWebView = (WebView) findViewById(R.id.webview);
        this.mWebView.loadUrl("file:///android_asset/licenses.html");
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayOptions(4, 4);
        }
    }

    @Override
    protected void onDestroy() {
        this.mWebView.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
