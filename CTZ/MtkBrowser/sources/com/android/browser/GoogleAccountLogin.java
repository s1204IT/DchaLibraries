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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import libcore.io.Streams;
import libcore.net.http.ResponseUtils;

public class GoogleAccountLogin implements AccountManagerCallback<Bundle>, DialogInterface.OnCancelListener, Runnable {
    private static final boolean DEBUG = Browser.DEBUG;
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
        WebViewTimersControl.getInstance().onBrowserActivityResume(this.mWebView, ((BrowserActivity) this.mActivity).getController());
        this.mWebView.setWebViewClient(new WebViewClient(this) {
            final GoogleAccountLogin this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onPageFinished(WebView webView, String str) {
                this.this$0.done();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String str) {
                return false;
            }
        });
    }

    public void done() {
        synchronized (this) {
            if (this.mRunnable != null) {
                if (DEBUG) {
                    Log.d("BrowserLogin", "Finished login attempt for " + this.mAccount.name);
                }
                this.mActivity.runOnUiThread(this.mRunnable);
                try {
                    this.mProgressDialog.dismiss();
                } catch (Exception e) {
                    Log.w("BrowserLogin", "Failed to dismiss mProgressDialog: " + e.getMessage());
                }
                this.mRunnable = null;
                this.mActivity.runOnUiThread(new Runnable(this) {
                    final GoogleAccountLogin this$0;

                    {
                        this.this$0 = this;
                    }

                    @Override
                    public void run() {
                        this.this$0.mWebView.destroy();
                    }
                });
            }
        }
    }

    private static Account[] getAccounts(Context context) {
        return AccountManager.get(context).getAccountsByType("com.google");
    }

    private void invalidateTokens() {
        AccountManager accountManager = AccountManager.get(this.mActivity);
        accountManager.invalidateAuthToken("com.google", this.mSid);
        accountManager.invalidateAuthToken("com.google", this.mLsid);
        this.mTokensInvalidated = true;
        this.mState = 1;
        accountManager.getAuthToken(this.mAccount, "SID", (Bundle) null, this.mActivity, this, (Handler) null);
    }

    private static boolean isLoggedIn() {
        return BrowserSettings.getInstance().getPreferences().getLong("last_autologin_time", -1L) != -1;
    }

    private void saveLoginTime() {
        SharedPreferences.Editor editorEdit = BrowserSettings.getInstance().getPreferences().edit();
        editorEdit.putLong("last_autologin_time", System.currentTimeMillis());
        editorEdit.apply();
    }

    private void startLogin() {
        saveLoginTime();
        this.mProgressDialog = ProgressDialog.show(this.mActivity, this.mActivity.getString(2131493077), this.mActivity.getString(2131493078, new Object[]{this.mAccount.name}), true, true, this);
        this.mState = 1;
        AccountManager.get(this.mActivity).getAuthToken(this.mAccount, "SID", (Bundle) null, this.mActivity, this, (Handler) null);
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
            new GoogleAccountLogin(activity, accounts[0], runnable).startLogin();
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        done();
    }

    @Override
    public void run() throws Throwable {
        Exception e;
        Throwable th;
        ?? r2 = 0;
        ?? r22 = 0;
        try {
            ?? r0 = (HttpURLConnection) new URL(this.ISSUE_AUTH_TOKEN_URL.buildUpon().appendQueryParameter("SID", this.mSid).appendQueryParameter("LSID", this.mLsid).build().toString()).openConnection(Proxy.NO_PROXY);
            try {
                r0.setRequestMethod("POST");
                r0.setRequestProperty("User-Agent", this.mUserAgent);
                int responseCode = r0.getResponseCode();
                if (responseCode != 200) {
                    Log.d("BrowserLogin", "LOGIN_FAIL: Bad status from auth url " + responseCode + ": " + r0.getResponseMessage());
                    if (responseCode != 403 || this.mTokensInvalidated) {
                        done();
                        r0 = r0;
                        if (r0 != 0) {
                            r0.disconnect();
                            r0 = r0;
                        }
                    } else {
                        Log.d("BrowserLogin", "LOGIN_FAIL: Invalidating tokens...");
                        invalidateTokens();
                        r0 = r0;
                        if (r0 != 0) {
                            r0.disconnect();
                            r0 = r0;
                        }
                    }
                } else {
                    String str = new String(Streams.readFully(r0.getInputStream()), ResponseUtils.responseCharset(r0.getContentType()));
                    if (r0 != 0) {
                        r0.disconnect();
                    }
                    String string = TOKEN_AUTH_URL.buildUpon().appendQueryParameter("source", "android-browser").appendQueryParameter("auth", str).appendQueryParameter("continue", BrowserSettings.getFactoryResetHomeUrl(this.mActivity)).build().toString();
                    this.mActivity.runOnUiThread(new Runnable(this, string) {
                        final GoogleAccountLogin this$0;
                        final String val$newUrl;

                        {
                            this.this$0 = this;
                            this.val$newUrl = string;
                        }

                        @Override
                        public void run() {
                            synchronized (this.this$0) {
                                if (this.this$0.mRunnable == null) {
                                    return;
                                }
                                HashMap map = new HashMap();
                                map.put(Browser.HEADER, Browser.UAPROF);
                                this.this$0.mWebView.loadUrl(this.val$newUrl, map);
                            }
                        }
                    });
                    r0 = string;
                }
            } catch (Exception e2) {
                e = e2;
                r22 = r0;
                try {
                    Log.d("BrowserLogin", "LOGIN_FAIL: Exception acquiring uber token " + e);
                    done();
                    if (r22 != 0) {
                        r22.disconnect();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    r2 = r22;
                    th = th;
                    if (r2 != 0) {
                        r2.disconnect();
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                r2 = r0;
                th = th;
                if (r2 != 0) {
                }
                throw th;
            }
        } catch (Exception e3) {
            e = e3;
        } catch (Throwable th4) {
            th = th4;
            if (r2 != 0) {
            }
            throw th;
        }
    }

    @Override
    public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
        try {
            String string = accountManagerFuture.getResult().getString("authtoken");
            switch (this.mState) {
                case 1:
                    this.mSid = string;
                    this.mState = 2;
                    AccountManager.get(this.mActivity).getAuthToken(this.mAccount, "LSID", (Bundle) null, this.mActivity, this, (Handler) null);
                    return;
                case 2:
                    this.mLsid = string;
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
}
