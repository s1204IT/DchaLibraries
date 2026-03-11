package com.android.browser;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebView;

public class DeviceAccountLogin implements AccountManagerCallback<Bundle> {
    private final AccountManager mAccountManager;
    Account[] mAccounts;
    private final Activity mActivity;
    private String mAuthToken;
    private AutoLoginCallback mCallback;
    private int mState = 0;
    private final Tab mTab;
    private final WebView mWebView;
    private final WebViewController mWebViewController;

    public interface AutoLoginCallback {
        void loginFailed();
    }

    public DeviceAccountLogin(Activity activity, WebView view, Tab tab, WebViewController controller) {
        this.mActivity = activity;
        this.mWebView = view;
        this.mTab = tab;
        this.mWebViewController = controller;
        this.mAccountManager = AccountManager.get(activity);
    }

    public void handleLogin(String realm, String account, String args) {
        this.mAccounts = this.mAccountManager.getAccountsByType(realm);
        this.mAuthToken = "weblogin:" + args;
        if (this.mAccounts.length == 0) {
            return;
        }
        for (Account a : this.mAccounts) {
            if (a.name.equals(account)) {
                this.mAccountManager.getAuthToken(a, this.mAuthToken, (Bundle) null, this.mActivity, this, (Handler) null);
                return;
            }
        }
        displayLoginUi();
    }

    @Override
    public void run(AccountManagerFuture<Bundle> value) {
        try {
            String result = value.getResult().getString("authtoken");
            if (result == null) {
                loginFailed();
            } else {
                this.mWebView.loadUrl(result);
                this.mTab.setDeviceAccountLogin(null);
                if (this.mTab.inForeground()) {
                    this.mWebViewController.hideAutoLogin(this.mTab);
                }
            }
        } catch (Exception e) {
            loginFailed();
        }
    }

    public int getState() {
        return this.mState;
    }

    private void loginFailed() {
        this.mState = 1;
        if (this.mTab.getDeviceAccountLogin() == null) {
            displayLoginUi();
        } else {
            if (this.mCallback == null) {
                return;
            }
            this.mCallback.loginFailed();
        }
    }

    private void displayLoginUi() {
        this.mTab.setDeviceAccountLogin(this);
        if (!this.mTab.inForeground()) {
            return;
        }
        this.mWebViewController.showAutoLogin(this.mTab);
    }

    public void cancel() {
        this.mTab.setDeviceAccountLogin(null);
    }

    public void login(int accountIndex, AutoLoginCallback cb) {
        this.mState = 2;
        this.mCallback = cb;
        this.mAccountManager.getAuthToken(this.mAccounts[accountIndex], this.mAuthToken, (Bundle) null, this.mActivity, this, (Handler) null);
    }

    public String[] getAccountNames() {
        String[] names = new String[this.mAccounts.length];
        for (int i = 0; i < this.mAccounts.length; i++) {
            names[i] = this.mAccounts[i].name;
        }
        return names;
    }
}
