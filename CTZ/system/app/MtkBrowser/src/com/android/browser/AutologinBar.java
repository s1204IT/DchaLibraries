package com.android.browser;

import android.content.Context;
import android.content.res.Resources;
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

/* loaded from: classes.dex */
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

    public AutologinBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public AutologinBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // android.view.View
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

    @Override // android.view.View.OnClickListener
    public void onClick(View view) throws Resources.NotFoundException {
        if (this.mAutoLoginCancel == view) {
            if (this.mAutoLoginHandler != null) {
                this.mAutoLoginHandler.cancel();
                this.mAutoLoginHandler = null;
            }
            hideAutoLogin(true);
            return;
        }
        if (this.mAutoLoginLogin == view && this.mAutoLoginHandler != null) {
            this.mAutoLoginAccount.setEnabled(false);
            this.mAutoLoginLogin.setEnabled(false);
            this.mAutoLoginProgress.setVisibility(0);
            this.mAutoLoginError.setVisibility(8);
            this.mAutoLoginHandler.login(this.mAutoLoginAccount.getSelectedItemPosition(), this);
        }
    }

    public void updateAutoLogin(Tab tab, boolean z) {
        DeviceAccountLogin deviceAccountLogin = tab.getDeviceAccountLogin();
        if (deviceAccountLogin != null) {
            this.mAutoLoginHandler = deviceAccountLogin;
            this.mAccountsAdapter = new ArrayAdapter<>(new ContextThemeWrapper(this.mContext, android.R.style.Theme.Holo.Light), android.R.layout.simple_spinner_item, deviceAccountLogin.getAccountNames());
            this.mAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.mAutoLoginAccount.setAdapter((SpinnerAdapter) this.mAccountsAdapter);
            this.mAutoLoginAccount.setSelection(0);
            this.mAutoLoginAccount.setEnabled(true);
            this.mAutoLoginLogin.setEnabled(true);
            this.mAutoLoginProgress.setVisibility(4);
            this.mAutoLoginError.setVisibility(8);
            switch (deviceAccountLogin.getState()) {
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
            showAutoLogin(z);
            return;
        }
        hideAutoLogin(z);
    }

    void showAutoLogin(boolean z) {
        this.mTitleBar.showAutoLogin(z);
    }

    void hideAutoLogin(boolean z) throws Resources.NotFoundException {
        this.mTitleBar.hideAutoLogin(z);
    }

    @Override // com.android.browser.DeviceAccountLogin.AutoLoginCallback
    public void loginFailed() {
        this.mAutoLoginAccount.setEnabled(true);
        this.mAutoLoginLogin.setEnabled(true);
        this.mAutoLoginProgress.setVisibility(4);
        this.mAutoLoginError.setVisibility(0);
    }
}
