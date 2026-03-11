package jp.co.benesse.dcha.systemsettings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import jp.co.benesse.dcha.util.Logger;

class WifiDialog extends AlertDialog implements WifiConfigUiBase {
    private final AccessPoint mAccessPoint;
    private Context mContext;
    private WifiConfigController mController;
    private final boolean mEdit;
    private final DialogInterface.OnClickListener mListener;

    public WifiDialog(Context context, DialogInterface.OnClickListener listener, AccessPoint accessPoint, boolean edit) {
        super(context, R.style.Theme_WifiDialog);
        Logger.d("WifiDialog", "WifiDialog 0001");
        this.mEdit = edit;
        this.mListener = listener;
        this.mAccessPoint = accessPoint;
        this.mContext = context;
        Logger.d("WifiDialog", "WifiDialog 0002");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("WifiDialog", "onCreate 0001");
        View view = getLayoutInflater().inflate(R.layout.wifi_dialog, (ViewGroup) null);
        setView(view);
        setInverseBackgroundForced(false);
        this.mController = new WifiConfigController((NetworkSettingActivity) this.mContext, this, view, this.mAccessPoint, this.mEdit);
        super.onCreate(savedInstanceState);
        this.mController.setSubmitOrEnable();
        Logger.d("WifiDialog", "onCreate 0002");
    }

    public WifiConfigController getController() {
        Logger.d("WifiDialog", "WifiConfigController 0001");
        return this.mController;
    }

    @Override
    public Button getSubmitButton() {
        Logger.d("WifiDialog", "getSubmitButton 0001");
        return getButton(-1);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        Logger.d("WifiDialog", "setSubmitButton 0001");
        setButton(-1, text, this.mListener);
        Logger.d("WifiDialog", "setSubmitButton 0002");
    }

    @Override
    public void setForgetButton(CharSequence text) {
        Logger.d("WifiDialog", "setForgetButton 0001");
        setButton(-3, text, this.mListener);
        Logger.d("WifiDialog", "setForgetButton 0002");
    }

    @Override
    public void setCancelButton(CharSequence text) {
        Logger.d("WifiDialog", "setCancelButton 0001");
        setButton(-2, text, this.mListener);
        Logger.d("WifiDialog", "setCancelButton 0002");
    }
}
