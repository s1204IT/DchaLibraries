package com.android.browser;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.browser.DeviceAccountLogin;

public class AutologinBar extends LinearLayout implements View.OnClickListener, DeviceAccountLogin.AutoLoginCallback {
    protected ArrayAdapter<String> mAccountsAdapter;
    protected Spinner mAutoLoginAccount;
    protected View mAutoLoginCancel;
    protected TextView mAutoLoginError;
    protected DeviceAccountLogin mAutoLoginHandler;
    protected Button mAutoLoginLogin;
    protected ProgressBar mAutoLoginProgress;
    protected TitleBar mTitleBar;

    public AutologinBar(Context context) {
        super(context);
    }

    public AutologinBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutologinBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAutoLoginAccount = (Spinner) findViewById(R.id.autologin_account);
        this.mAutoLoginLogin = (Button) findViewById(R.id.autologin_login);
        this.mAutoLoginLogin.setOnClickListener(this);
        this.mAutoLoginProgress = (ProgressBar) findViewById(R.id.autologin_progress);
        this.mAutoLoginError = (TextView) findViewById(R.id.autologin_error);
        this.mAutoLoginCancel = findViewById(R.id.autologin_close);
        this.mAutoLoginCancel.setOnClickListener(this);
    }

    public void setTitleBar(TitleBar titleBar) {
        this.mTitleBar = titleBar;
    }

    @Override
    public void onClick(View v) {
        if (this.mAutoLoginCancel == v) {
            if (this.mAutoLoginHandler != null) {
                this.mAutoLoginHandler.cancel();
                this.mAutoLoginHandler = null;
            }
            hideAutoLogin(true);
            return;
        }
        if (this.mAutoLoginLogin != v || this.mAutoLoginHandler == null) {
            return;
        }
        this.mAutoLoginAccount.setEnabled(false);
        this.mAutoLoginLogin.setEnabled(false);
        this.mAutoLoginProgress.setVisibility(0);
        this.mAutoLoginError.setVisibility(8);
        this.mAutoLoginHandler.login(this.mAutoLoginAccount.getSelectedItemPosition(), this);
    }

    public void updateAutoLogin(Tab tab, boolean animate) {
        DeviceAccountLogin login = tab.getDeviceAccountLogin();
        if (login != null) {
            this.mAutoLoginHandler = login;
            ContextThemeWrapper wrapper = new ContextThemeWrapper(this.mContext, android.R.style.Theme.Holo.Light);
            this.mAccountsAdapter = new ArrayAdapter<>(wrapper, android.R.layout.simple_spinner_item, login.getAccountNames());
            this.mAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.mAutoLoginAccount.setAdapter((SpinnerAdapter) this.mAccountsAdapter);
            this.mAutoLoginAccount.setSelection(0);
            this.mAutoLoginAccount.setEnabled(true);
            this.mAutoLoginLogin.setEnabled(true);
            this.mAutoLoginProgress.setVisibility(4);
            this.mAutoLoginError.setVisibility(8);
            switch (login.getState()) {
                case 0:
                    break;
                case 1:
                    this.mAutoLoginProgress.setVisibility(4);
                    this.mAutoLoginError.setVisibility(0);
                    break;
                case 2:
                    this.mAutoLoginAccount.setEnabled(false);
                    this.mAutoLoginLogin.setEnabled(false);
                    this.mAutoLoginProgress.setVisibility(0);
                    break;
                default:
                    throw new IllegalStateException();
            }
            showAutoLogin(animate);
            return;
        }
        hideAutoLogin(animate);
    }

    void showAutoLogin(boolean animate) {
        this.mTitleBar.showAutoLogin(animate);
    }

    void hideAutoLogin(boolean animate) {
        this.mTitleBar.hideAutoLogin(animate);
    }

    @Override
    public void loginFailed() {
        this.mAutoLoginAccount.setEnabled(true);
        this.mAutoLoginLogin.setEnabled(true);
        this.mAutoLoginProgress.setVisibility(4);
        this.mAutoLoginError.setVisibility(0);
    }
}
