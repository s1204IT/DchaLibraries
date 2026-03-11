package jp.co.benesse.dcha.systemsettings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import jp.co.benesse.dcha.util.Logger;
import jp.co.benesse.dcha.util.RestrictedLockUtils;
import jp.co.benesse.dcha.util.WifiSettings;

class WifiDialog extends AlertDialog implements WifiConfigUiBase, DialogInterface.OnClickListener {
    private final AccessPoint mAccessPoint;
    private Context mContext;
    private WifiConfigController mController;
    private boolean mHideSubmitButton;
    private final WifiDialogListener mListener;
    private final int mMode;

    public interface WifiDialogListener {
        void onForget(WifiDialog wifiDialog);

        void onSubmit(WifiDialog wifiDialog);
    }

    public WifiDialog(Context context, WifiDialogListener listener, AccessPoint accessPoint, int mode) {
        super(context, R.style.Theme_WifiDialog);
        Logger.d("WifiDialog", "WifiDialog 0003");
        this.mMode = mode;
        this.mListener = listener;
        this.mAccessPoint = accessPoint;
        this.mHideSubmitButton = false;
        this.mContext = context;
        Logger.d("WifiDialog", "WifiDialog 0004");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("WifiDialog", "onCreate 0001");
        View view = getLayoutInflater().inflate(R.layout.wifi_dialog, (ViewGroup) null);
        setView(view);
        setInverseBackgroundForced(false);
        this.mController = new WifiConfigController((NetworkSettingActivity) this.mContext, this, view, this.mAccessPoint, this.mMode);
        super.onCreate(savedInstanceState);
        this.mController.setSubmitOrEnable();
        if (this.mHideSubmitButton) {
            Logger.d("WifiDialog", "onCreate 0002");
            this.mController.hideSubmitButton();
        } else {
            Logger.d("WifiDialog", "onCreate 0003");
            this.mController.enableSubmitIfAppropriate();
        }
        if (this.mAccessPoint == null) {
            Logger.d("WifiDialog", "onCreate 0004");
            this.mController.hideForgetButton();
        }
        Logger.d("WifiDialog", "onCreate 0005");
    }

    public WifiConfigController getController() {
        Logger.d("WifiDialog", "WifiConfigController 0001");
        return this.mController;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Logger.d("WifiDialog", "onRestoreInstanceState 0001");
        super.onRestoreInstanceState(savedInstanceState);
        this.mController.updatePassword();
        Logger.d("WifiDialog", "onRestoreInstanceState 0002");
    }

    @Override
    public void dispatchSubmit() {
        Logger.d("WifiDialog", "dispatchSubmit 0001");
        if (this.mListener != null) {
            Logger.d("WifiDialog", "dispatchSubmit 0002");
            this.mListener.onSubmit(this);
        }
        dismiss();
        Logger.d("WifiDialog", "dispatchSubmit 0003");
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int id) {
        Logger.d("WifiDialog", "onClick 0001");
        if (this.mListener != null) {
            Logger.d("WifiDialog", "onClick 0002");
            switch (id) {
                case -3:
                    Logger.d("WifiDialog", "onClick 0004");
                    if (WifiSettings.isEditabilityLockedDown(getContext(), this.mAccessPoint.getConfig())) {
                        Logger.d("WifiDialog", "onClick 0005");
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), RestrictedLockUtils.getDeviceOwner(getContext()));
                        return;
                    }
                    this.mListener.onForget(this);
                    break;
                case -1:
                    Logger.d("WifiDialog", "onClick 0003");
                    this.mListener.onSubmit(this);
                    break;
            }
        }
        Logger.d("WifiDialog", "onClick 0006");
    }

    @Override
    public Button getSubmitButton() {
        Logger.d("WifiDialog", "getSubmitButton 0001");
        return getButton(-1);
    }

    @Override
    public Button getForgetButton() {
        Logger.d("WifiDialog", "getForgetButton 0001");
        return getButton(-3);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        Logger.d("WifiDialog", "setSubmitButton 0001");
        setButton(-1, text, this);
        Logger.d("WifiDialog", "setSubmitButton 0002");
    }

    @Override
    public void setForgetButton(CharSequence text) {
        Logger.d("WifiDialog", "setForgetButton 0001");
        setButton(-3, text, this);
        Logger.d("WifiDialog", "setForgetButton 0002");
    }

    @Override
    public void setCancelButton(CharSequence text) {
        Logger.d("WifiDialog", "setCancelButton 0001");
        setButton(-2, text, this);
        Logger.d("WifiDialog", "setCancelButton 0002");
    }
}
