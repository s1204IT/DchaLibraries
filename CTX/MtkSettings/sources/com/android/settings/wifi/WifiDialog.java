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

public class WifiDialog extends AlertDialog implements DialogInterface.OnClickListener, WifiConfigUiBase {
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

    public static WifiDialog createFullscreen(Context context, WifiDialogListener wifiDialogListener, AccessPoint accessPoint, int i) {
        return new WifiDialog(context, wifiDialogListener, accessPoint, i, R.style.Theme_Settings_NoActionBar, false);
    }

    public static WifiDialog createModal(Context context, WifiDialogListener wifiDialogListener, AccessPoint accessPoint, int i) {
        return new WifiDialog(context, wifiDialogListener, accessPoint, i, 0, i == 0);
    }

    WifiDialog(Context context, WifiDialogListener wifiDialogListener, AccessPoint accessPoint, int i, int i2, boolean z) {
        super(context, i2);
        this.mMode = i;
        this.mListener = wifiDialogListener;
        this.mAccessPoint = accessPoint;
        this.mHideSubmitButton = z;
    }

    public WifiConfigController getController() {
        return this.mController;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        this.mView = getLayoutInflater().inflate(R.layout.wifi_dialog, (ViewGroup) null);
        setView(this.mView);
        setInverseBackgroundForced(true);
        this.mController = new WifiConfigController(this, this.mView, this.mAccessPoint, this.mMode);
        super.onCreate(bundle);
        if (this.mHideSubmitButton) {
            this.mController.hideSubmitButton();
        } else {
            this.mController.enableSubmitIfAppropriate();
        }
        if (this.mAccessPoint == null) {
            this.mController.hideForgetButton();
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
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
    public void onClick(DialogInterface dialogInterface, int i) {
        if (this.mListener != null) {
            if (i != -3) {
                if (i == -1) {
                    this.mListener.onSubmit(this);
                }
            } else if (WifiUtils.isNetworkLockedDown(getContext(), this.mAccessPoint.getConfig())) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), RestrictedLockUtils.getDeviceOwner(getContext()));
            } else {
                this.mListener.onForget(this);
            }
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
    public void setSubmitButton(CharSequence charSequence) {
        setButton(-1, charSequence, this);
    }

    @Override
    public void setForgetButton(CharSequence charSequence) {
        setButton(-3, charSequence, this);
    }

    @Override
    public void setCancelButton(CharSequence charSequence) {
        setButton(-2, charSequence, this);
    }
}
