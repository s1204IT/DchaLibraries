package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.wifi.AccessPoint;

class WifiDialog extends AlertDialog implements WifiConfigUiBase, DialogInterface.OnClickListener {
    private final AccessPoint mAccessPoint;
    private WifiConfigController mController;
    private boolean mHideSubmitButton;
    private final WifiDialogListener mListener;
    private final int mMode;
    private View mView;

    public interface WifiDialogListener {
        void onForget(WifiDialog wifiDialog);

        void onSubmit(WifiDialog wifiDialog);
    }

    public WifiDialog(Context context, WifiDialogListener listener, AccessPoint accessPoint, int mode, boolean hideSubmitButton) {
        this(context, listener, accessPoint, mode);
        this.mHideSubmitButton = hideSubmitButton;
    }

    public WifiDialog(Context context, WifiDialogListener listener, AccessPoint accessPoint, int mode) {
        super(context);
        this.mMode = mode;
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
        this.mController = new WifiConfigController(this, this.mView, this.mAccessPoint, this.mMode);
        super.onCreate(savedInstanceState);
        if (this.mHideSubmitButton) {
            this.mController.hideSubmitButton();
        } else {
            this.mController.enableSubmitIfAppropriate();
        }
        if (this.mAccessPoint != null) {
            return;
        }
        this.mController.hideForgetButton();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.mController.updatePassword();
    }

    @Override
    public void dispatchSubmit() {
        if (this.mListener != null) {
            this.mListener.onSubmit(this);
        }
        dismiss();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int id) {
        if (this.mListener == null) {
            return;
        }
        switch (id) {
            case -3:
                if (WifiSettings.isEditabilityLockedDown(getContext(), this.mAccessPoint.getConfig())) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), RestrictedLockUtils.getDeviceOwner(getContext()));
                } else {
                    this.mListener.onForget(this);
                }
                break;
            case -1:
                this.mListener.onSubmit(this);
                break;
        }
    }

    @Override
    public Button getSubmitButton() {
        return getButton(-1);
    }

    @Override
    public Button getForgetButton() {
        return getButton(-3);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        setButton(-1, text, this);
    }

    @Override
    public void setForgetButton(CharSequence text) {
        setButton(-3, text, this);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        setButton(-2, text, this);
    }
}
