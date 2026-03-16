package com.android.vpndialogs;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.IConnectivityManager;
import android.os.ServiceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.net.VpnConfig;

public class ConfirmDialog extends AlertActivity implements DialogInterface.OnClickListener, Html.ImageGetter {
    private Button mButton;
    private String mPackage;
    private IConnectivityManager mService;

    protected void onResume() {
        super.onResume();
        try {
            this.mPackage = getCallingPackage();
            this.mService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
            if (this.mService.prepareVpn(this.mPackage, (String) null)) {
                setResult(-1);
                finish();
            } else {
                View view = View.inflate(this, R.layout.confirm, null);
                ((TextView) view.findViewById(R.id.warning)).setText(Html.fromHtml(getString(R.string.warning, new Object[]{VpnConfig.getVpnLabel(this, this.mPackage)}), this, null));
                this.mAlertParams.mTitle = getText(R.string.prompt);
                this.mAlertParams.mPositiveButtonText = getText(android.R.string.ok);
                this.mAlertParams.mPositiveButtonListener = this;
                this.mAlertParams.mNegativeButtonText = getText(android.R.string.cancel);
                this.mAlertParams.mView = view;
                setupAlert();
                getWindow().setCloseOnTouchOutside(false);
                this.mButton = this.mAlert.getButton(-1);
                this.mButton.setFilterTouchesWhenObscured(true);
            }
        } catch (Exception e) {
            Log.e("VpnConfirm", "onResume", e);
            finish();
        }
    }

    @Override
    public Drawable getDrawable(String source) {
        Drawable icon = getDrawable(R.drawable.ic_vpn_dialog);
        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        return icon;
    }

    public void onBackPressed() {
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        try {
            if (this.mService.prepareVpn((String) null, this.mPackage)) {
                this.mService.setVpnPackageAuthorization(true);
                setResult(-1);
            }
        } catch (Exception e) {
            Log.e("VpnConfirm", "onClick", e);
        }
    }
}
