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
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

public class GoogleAccountLogin implements AccountManagerCallback<Bundle>, DialogInterface.OnCancelListener, Runnable {
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
        String url = this.ISSUE_AUTH_TOKEN_URL.buildUpon().appendQueryParameter("SID", this.mSid).appendQueryParameter("LSID", this.mLsid).build().toString();
        AndroidHttpClient client = AndroidHttpClient.newInstance(this.mUserAgent);
        HttpPost request = new HttpPost(url);
        try {
            HttpResponse response = client.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                Log.d("BrowserLogin", "LOGIN_FAIL: Bad status from auth url " + status + ": " + response.getStatusLine().getReasonPhrase());
                if (status != 403 || this.mTokensInvalidated) {
                    done();
                    client.close();
                } else {
                    Log.d("BrowserLogin", "LOGIN_FAIL: Invalidating tokens...");
                    invalidateTokens();
                }
            } else {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    Log.d("BrowserLogin", "LOGIN_FAIL: Null entity in response");
                    done();
                    client.close();
                } else {
                    String result = EntityUtils.toString(entity, "UTF-8");
                    client.close();
                    final String newUrl = TOKEN_AUTH_URL.buildUpon().appendQueryParameter("source", "android-browser").appendQueryParameter("auth", result).appendQueryParameter("continue", BrowserSettings.getFactoryResetHomeUrl(this.mActivity)).build().toString();
                    this.mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (GoogleAccountLogin.this) {
                                if (GoogleAccountLogin.this.mRunnable != null) {
                                    GoogleAccountLogin.this.mWebView.loadUrl(newUrl);
                                }
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.d("BrowserLogin", "LOGIN_FAIL: Exception acquiring uber token " + e);
            request.abort();
            done();
        } finally {
            client.close();
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
                case 1:
                    this.mSid = id;
                    this.mState = 2;
                    AccountManager.get(this.mActivity).getAuthToken(this.mAccount, "LSID", (Bundle) null, this.mActivity, this, (Handler) null);
                    return;
                case 2:
                    this.mLsid = id;
                    new Thread(this).start();
                    return;
                default:
                    throw new IllegalStateException("Impossible to get into this state");
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
        return lastLogin != -1;
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
