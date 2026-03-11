package com.android.browser;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import libcore.io.Streams;
import libcore.net.http.ResponseUtils;

public class GoogleAccountLogin implements Runnable, AccountManagerCallback<Bundle>, DialogInterface.OnCancelListener {
    private static final Uri TOKEN_AUTH_URL = Uri.parse("https://www.google.com/accounts/TokenAuth");
    private Uri ISSUE_AUTH_TOKEN_URL = Uri.parse("https://www.google.com/accounts/IssueAuthToken?service=gaia&Session=false");
    private final Account mAccount;
    private final Activity mActivity;
    private String mLsid;
    private ProgressDialog mProgressDialog;
    private Runnable mRunnable;
    private String mSid;
    private int mState;
    private boolean mTokensInvalidated;
    private String mUserAgent;
    private final WebView mWebView;

    private GoogleAccountLogin(Activity activity, Account account, Runnable runnable) {
        this.mActivity = activity;
        this.mAccount = account;
        this.mWebView = new WebView(this.mActivity);
        this.mRunnable = runnable;
        this.mUserAgent = this.mWebView.getSettings().getUserAgentString();
        CookieSyncManager.getInstance().startSync();
        WebViewTimersControl.getInstance().onBrowserActivityResume(this.mWebView);
        this.mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                GoogleAccountLogin.this.done();
            }
        });
    }

    private void saveLoginTime() {
        SharedPreferences.Editor ed = BrowserSettings.getInstance().getPreferences().edit();
        ed.putLong("last_autologin_time", System.currentTimeMillis());
        ed.apply();
    }

    @Override
    public void run() {
        String urlString = this.ISSUE_AUTH_TOKEN_URL.buildUpon().appendQueryParameter("SID", this.mSid).appendQueryParameter("LSID", this.mLsid).build().toString();
        HttpURLConnection httpURLConnection = null;
        try {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("User-Agent", this.mUserAgent);
                int status = connection.getResponseCode();
                if (status == 200) {
                    Charset responseCharset = ResponseUtils.responseCharset(connection.getContentType());
                    byte[] responseBytes = Streams.readFully(connection.getInputStream());
                    String authToken = new String(responseBytes, responseCharset);
                    if (connection != null) {
                        connection.disconnect();
                    }
                    final String newUrl = TOKEN_AUTH_URL.buildUpon().appendQueryParameter("source", "android-browser").appendQueryParameter("auth", authToken).appendQueryParameter("continue", BrowserSettings.getFactoryResetHomeUrl(this.mActivity)).build().toString();
                    this.mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (GoogleAccountLogin.this) {
                                if (GoogleAccountLogin.this.mRunnable == null) {
                                    return;
                                }
                                GoogleAccountLogin.this.mWebView.loadUrl(newUrl);
                            }
                        }
                    });
                    return;
                }
                Log.d("BrowserLogin", "LOGIN_FAIL: Bad status from auth url " + status + ": " + connection.getResponseMessage());
                if (status != 403 || this.mTokensInvalidated) {
                    done();
                    if (connection != null) {
                        connection.disconnect();
                        return;
                    }
                    return;
                }
                Log.d("BrowserLogin", "LOGIN_FAIL: Invalidating tokens...");
                invalidateTokens();
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e) {
                Log.d("BrowserLogin", "LOGIN_FAIL: Exception acquiring uber token " + e);
                done();
                if (0 != 0) {
                    httpURLConnection.disconnect();
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                httpURLConnection.disconnect();
            }
            throw th;
        }
    }

    private void invalidateTokens() {
        AccountManager am = AccountManager.get(this.mActivity);
        am.invalidateAuthToken("com.google", this.mSid);
        am.invalidateAuthToken("com.google", this.mLsid);
        this.mTokensInvalidated = true;
        this.mState = 1;
        am.getAuthToken(this.mAccount, "SID", (Bundle) null, this.mActivity, this, (Handler) null);
    }

    @Override
    public void run(AccountManagerFuture<Bundle> value) {
        try {
            String id = value.getResult().getString("authtoken");
            switch (this.mState) {
                case 0:
                default:
                    throw new IllegalStateException("Impossible to get into this state");
                case 1:
                    this.mSid = id;
                    this.mState = 2;
                    AccountManager.get(this.mActivity).getAuthToken(this.mAccount, "LSID", (Bundle) null, this.mActivity, this, (Handler) null);
                    return;
                case 2:
                    this.mLsid = id;
                    new Thread(this).start();
                    return;
            }
        } catch (Exception e) {
            Log.d("BrowserLogin", "LOGIN_FAIL: Exception in state " + this.mState + " " + e);
            done();
        }
    }

    public static void startLoginIfNeeded(Activity activity, Runnable runnable) {
        if (isLoggedIn()) {
            runnable.run();
            return;
        }
        Account[] accounts = getAccounts(activity);
        if (accounts == null || accounts.length == 0) {
            runnable.run();
        } else {
            GoogleAccountLogin login = new GoogleAccountLogin(activity, accounts[0], runnable);
            login.startLogin();
        }
    }

    private void startLogin() {
        saveLoginTime();
        this.mProgressDialog = ProgressDialog.show(this.mActivity, this.mActivity.getString(R.string.pref_autologin_title), this.mActivity.getString(R.string.pref_autologin_progress, new Object[]{this.mAccount.name}), true, true, this);
        this.mState = 1;
        AccountManager.get(this.mActivity).getAuthToken(this.mAccount, "SID", (Bundle) null, this.mActivity, this, (Handler) null);
    }

    private static Account[] getAccounts(Context ctx) {
        return AccountManager.get(ctx).getAccountsByType("com.google");
    }

    private static boolean isLoggedIn() {
        long lastLogin = BrowserSettings.getInstance().getPreferences().getLong("last_autologin_time", -1L);
        if (lastLogin == -1) {
            return false;
        }
        return true;
    }

    public synchronized void done() {
        if (this.mRunnable != null) {
            Log.d("BrowserLogin", "Finished login attempt for " + this.mAccount.name);
            this.mActivity.runOnUiThread(this.mRunnable);
            try {
                this.mProgressDialog.dismiss();
            } catch (Exception e) {
                Log.w("BrowserLogin", "Failed to dismiss mProgressDialog: " + e.getMessage());
            }
            this.mRunnable = null;
            this.mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GoogleAccountLogin.this.mWebView.destroy();
                }
            });
        }
    }

    @Override
    public void onCancel(DialogInterface unused) {
        done();
    }
}
