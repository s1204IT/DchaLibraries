package com.android.gallery3d.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class ShowGifActivity extends Activity {
    private WebView mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        View mainLayout = buildGifView();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -1);
        setContentView(mainLayout, params);
    }

    private View buildGifView() {
        if (this.mView == null) {
            this.mView = new WebView(this);
            this.mView.setBackgroundColor(0);
        }
        String gifUrl = getUrlFromIntent();
        if (gifUrl != null) {
            Log.d("ShowGifActivity", "Get gif picture url from intent: " + gifUrl);
            String data = "<html><body style=\" padding:0px; margin:0px;\"><div style=\"text-align:center;\"><img src=\"" + gifUrl + "\"></div></body></html>";
            this.mView.loadDataWithBaseURL("file:///", data, "text/html", "UTF-8", null);
        } else {
            Log.e("ShowGifActivity", "No gif picture is found in intent !!");
        }
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(-2, -2);
        params.addRule(13);
        RelativeLayout mainLayout = new RelativeLayout(this);
        mainLayout.addView(this.mView, params);
        return mainLayout;
    }

    private String getUrlFromIntent() {
        String url = getIntent().getStringExtra("gif_url");
        if (url != null) {
            return url;
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
