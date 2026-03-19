package com.android.vpndialogs;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.net.VpnConfig;

public class ConfirmDialog extends AlertActivity implements DialogInterface.OnClickListener, Html.ImageGetter {
    private String mPackage;
    private IConnectivityManager mService;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mPackage = getCallingPackage();
        this.mService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
        if (prepareVpn()) {
            setResult(-1);
            finish();
            return;
        }
        if (UserManager.get(this).hasUserRestriction("no_config_vpn")) {
            finish();
            return;
        }
        String alwaysOnVpnPackage = getAlwaysOnVpnPackage();
        if (alwaysOnVpnPackage != null && !alwaysOnVpnPackage.equals(this.mPackage)) {
            finish();
            return;
        }
        View viewInflate = View.inflate(this, R.layout.confirm, null);
        ((TextView) viewInflate.findViewById(R.id.warning)).setText(Html.fromHtml(getString(R.string.warning, new Object[]{getVpnLabel()}), this, null));
        this.mAlertParams.mTitle = getText(R.string.prompt);
        this.mAlertParams.mPositiveButtonText = getText(android.R.string.ok);
        this.mAlertParams.mPositiveButtonListener = this;
        this.mAlertParams.mNegativeButtonText = getText(android.R.string.cancel);
        this.mAlertParams.mView = viewInflate;
        setupAlert();
        getWindow().setCloseOnTouchOutside(false);
        getWindow().addPrivateFlags(524288);
        this.mAlert.getButton(-1).setFilterTouchesWhenObscured(true);
    }

    private String getAlwaysOnVpnPackage() {
        try {
            return this.mService.getAlwaysOnVpnPackage(UserHandle.myUserId());
        } catch (RemoteException e) {
            Log.e("VpnConfirm", "fail to call getAlwaysOnVpnPackage", e);
            return null;
        }
    }

    private boolean prepareVpn() {
        try {
            return this.mService.prepareVpn(this.mPackage, (String) null, UserHandle.myUserId());
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    private CharSequence getVpnLabel() {
        try {
            return VpnConfig.getVpnLabel(this, this.mPackage);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Drawable getDrawable(String str) {
        Drawable drawable = getDrawable(R.drawable.ic_vpn_dialog);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    public void onBackPressed() {
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        try {
            if (this.mService.prepareVpn((String) null, this.mPackage, UserHandle.myUserId())) {
                this.mService.setVpnPackageAuthorization(this.mPackage, UserHandle.myUserId(), true);
                setResult(-1);
            }
        } catch (Exception e) {
            Log.e("VpnConfirm", "onClick", e);
        }
    }
}
