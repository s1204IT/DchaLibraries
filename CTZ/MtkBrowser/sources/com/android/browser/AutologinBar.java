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

    public AutologinBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public AutologinBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    void hideAutoLogin(boolean z) {
        this.mTitleBar.hideAutoLogin(z);
    }

    @Override
    public void loginFailed() {
        this.mAutoLoginAccount.setEnabled(true);
        this.mAutoLoginLogin.setEnabled(true);
        this.mAutoLoginProgress.setVisibility(4);
        this.mAutoLoginError.setVisibility(0);
    }

    @Override
    public void onClick(View view) {
        if (this.mAutoLoginCancel == view) {
            if (this.mAutoLoginHandler != null) {
                this.mAutoLoginHandler.cancel();
                this.mAutoLoginHandler = null;
            }
            hideAutoLogin(true);
            return;
        }
        if (this.mAutoLoginLogin != view || this.mAutoLoginHandler == null) {
            return;
        }
        this.mAutoLoginAccount.setEnabled(false);
        this.mAutoLoginLogin.setEnabled(false);
        this.mAutoLoginProgress.setVisibility(0);
        this.mAutoLoginError.setVisibility(8);
        this.mAutoLoginHandler.login(this.mAutoLoginAccount.getSelectedItemPosition(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAutoLoginAccount = (Spinner) findViewById(2131558532);
        this.mAutoLoginLogin = (Button) findViewById(2131558535);
        this.mAutoLoginLogin.setOnClickListener(this);
        this.mAutoLoginProgress = (ProgressBar) findViewById(2131558534);
        this.mAutoLoginError = (TextView) findViewById(2131558536);
        this.mAutoLoginCancel = findViewById(2131558533);
        this.mAutoLoginCancel.setOnClickListener(this);
    }

    public void setTitleBar(TitleBar titleBar) {
        this.mTitleBar = titleBar;
    }

    void showAutoLogin(boolean z) {
        this.mTitleBar.showAutoLogin(z);
    }

    public void updateAutoLogin(Tab tab, boolean z) {
        DeviceAccountLogin deviceAccountLogin = tab.getDeviceAccountLogin();
        if (deviceAccountLogin == null) {
            hideAutoLogin(z);
            return;
        }
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
    }
}
