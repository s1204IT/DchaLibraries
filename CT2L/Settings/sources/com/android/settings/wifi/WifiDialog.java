package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.settings.R;

class WifiDialog extends AlertDialog implements WifiConfigUiBase {
    private final AccessPoint mAccessPoint;
    private WifiConfigController mController;
    private final boolean mEdit;
    private boolean mHideSubmitButton;
    private final DialogInterface.OnClickListener mListener;
    private View mView;

    public WifiDialog(Context context, DialogInterface.OnClickListener listener, AccessPoint accessPoint, boolean edit, boolean hideSubmitButton) {
        this(context, listener, accessPoint, edit);
        this.mHideSubmitButton = hideSubmitButton;
    }

    public WifiDialog(Context context, DialogInterface.OnClickListener listener, AccessPoint accessPoint, boolean edit) {
        super(context);
        this.mEdit = edit;
        this.mListener = listener;
        this.mAccessPoint = accessPoint;
        this.mHideSubmitButton = false;
    }

    public WifiConfigController getController() {
        return this.mController;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.mView = getLayoutInflater().inflate(R.layout.wifi_dialog, (ViewGroup) null);
        setView(this.mView);
        setInverseBackgroundForced(true);
        this.mController = new WifiConfigController(this, this.mView, this.mAccessPoint, this.mEdit);
        super.onCreate(savedInstanceState);
        if (this.mHideSubmitButton) {
            this.mController.hideSubmitButton();
        } else {
            this.mController.enableSubmitIfAppropriate();
        }
    }

    @Override
    public Button getSubmitButton() {
        return getButton(-1);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        setButton(-1, text, this.mListener);
    }

    @Override
    public void setForgetButton(CharSequence text) {
        setButton(-3, text, this.mListener);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        setButton(-2, text, this.mListener);
    }
}
