package com.android.settings.wifi;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class WifiConfigUiForSetupWizardXL implements View.OnFocusChangeListener, WifiConfigUiBase {
    private final WifiSettingsForSetupWizardXL mActivity;
    private Button mCancelButton;
    private Button mConnectButton;
    private WifiConfigController mController;
    private Handler mHandler;
    private LayoutInflater mInflater;
    private final InputMethodManager mInputMethodManager;
    private View mView;

    public void requestFocusAndShowKeyboard(int editViewId) {
        View viewToBeFocused = this.mView.findViewById(editViewId);
        if (viewToBeFocused == null) {
            Log.w("SetupWizard", "password field to be focused not found.");
            return;
        }
        if (!(viewToBeFocused instanceof EditText)) {
            Log.w("SetupWizard", "password field is not EditText");
            return;
        }
        if (viewToBeFocused.isFocused()) {
            Log.i("SetupWizard", "Already focused");
            if (!this.mInputMethodManager.showSoftInput(viewToBeFocused, 0)) {
                Log.w("SetupWizard", "Failed to show SoftInput");
                return;
            }
            return;
        }
        viewToBeFocused.setOnFocusChangeListener(this);
        boolean requestFocusResult = viewToBeFocused.requestFocus();
        Object[] objArr = new Object[1];
        objArr[0] = requestFocusResult ? "successful" : "failed";
        Log.i("SetupWizard", String.format("Focus request: %s", objArr));
        if (!requestFocusResult) {
            viewToBeFocused.setOnFocusChangeListener(null);
        }
    }

    public WifiConfigController getController() {
        return this.mController;
    }

    @Override
    public LayoutInflater getLayoutInflater() {
        return this.mInflater;
    }

    @Override
    public Button getSubmitButton() {
        return this.mConnectButton;
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        this.mConnectButton.setVisibility(0);
        this.mConnectButton.setText(text);
    }

    @Override
    public void setForgetButton(CharSequence text) {
    }

    @Override
    public void setCancelButton(CharSequence text) {
        this.mCancelButton.setVisibility(0);
    }

    @Override
    public Context getContext() {
        return this.mActivity;
    }

    @Override
    public void setTitle(int id) {
        Log.d("SetupWizard", "Ignoring setTitle");
    }

    @Override
    public void setTitle(CharSequence title) {
        Log.d("SetupWizard", "Ignoring setTitle");
    }

    private class FocusRunnable implements Runnable {
        final View mViewToBeFocused;

        public FocusRunnable(View viewToBeFocused) {
            this.mViewToBeFocused = viewToBeFocused;
        }

        @Override
        public void run() {
            boolean showSoftInputResult = WifiConfigUiForSetupWizardXL.this.mInputMethodManager.showSoftInput(this.mViewToBeFocused, 0);
            if (showSoftInputResult) {
                WifiConfigUiForSetupWizardXL.this.mActivity.setPaddingVisibility(8);
            } else {
                Log.w("SetupWizard", "Failed to show software keyboard ");
            }
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        view.setOnFocusChangeListener(null);
        if (hasFocus) {
            this.mHandler.post(new FocusRunnable(view));
        }
    }
}
