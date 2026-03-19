package com.android.vpndialogs;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.IConnectivityManager;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.net.VpnConfig;

public class AlwaysOnDisconnectedDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private IConnectivityManager mService;
    private int mUserId;
    private String mVpnPackage;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
        this.mUserId = UserHandle.myUserId();
        this.mVpnPackage = getAlwaysOnVpnPackage();
        if (this.mVpnPackage == null) {
            finish();
            return;
        }
        View viewInflate = View.inflate(this, R.layout.always_on_disconnected, null);
        TextView textView = (TextView) viewInflate.findViewById(R.id.message);
        textView.setText(getMessage(getIntent().getBooleanExtra("lockdown", false)));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        this.mAlertParams.mTitle = getString(R.string.always_on_disconnected_title);
        this.mAlertParams.mPositiveButtonText = getString(R.string.open_app);
        this.mAlertParams.mPositiveButtonListener = this;
        this.mAlertParams.mNegativeButtonText = getString(R.string.dismiss);
        this.mAlertParams.mNegativeButtonListener = this;
        this.mAlertParams.mCancelable = false;
        this.mAlertParams.mView = viewInflate;
        setupAlert();
        getWindow().setCloseOnTouchOutside(false);
        getWindow().setType(2003);
        getWindow().addFlags(131072);
        getWindow().addPrivateFlags(524288);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case -2:
                finish();
                break;
            case -1:
                Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(this.mVpnPackage);
                if (launchIntentForPackage != null) {
                    launchIntentForPackage.addFlags(268468224);
                    startActivity(launchIntentForPackage);
                }
                finish();
                break;
        }
    }

    private String getAlwaysOnVpnPackage() {
        try {
            return this.mService.getAlwaysOnVpnPackage(this.mUserId);
        } catch (RemoteException e) {
            Log.e("VpnDisconnected", "Can't getAlwaysOnVpnPackage()", e);
            return null;
        }
    }

    private CharSequence getVpnLabel() {
        try {
            return VpnConfig.getVpnLabel(this, this.mVpnPackage);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("VpnDisconnected", "Can't getVpnLabel() for " + this.mVpnPackage, e);
            return this.mVpnPackage;
        }
    }

    private CharSequence getMessage(boolean z) {
        int i;
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        if (z) {
            i = R.string.always_on_disconnected_message_lockdown;
        } else {
            i = R.string.always_on_disconnected_message;
        }
        spannableStringBuilder.append((CharSequence) getString(i, new Object[]{getVpnLabel()}));
        spannableStringBuilder.append((CharSequence) getString(R.string.always_on_disconnected_message_separator));
        spannableStringBuilder.append(getString(R.string.always_on_disconnected_message_settings_link), new VpnSpan(), 0);
        return spannableStringBuilder;
    }

    private class VpnSpan extends ClickableSpan {
        private VpnSpan() {
        }

        @Override
        public void onClick(View view) {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            Intent intent = new Intent("android.settings.VPN_SETTINGS");
            intent.addFlags(268468224);
            AlwaysOnDisconnectedDialog.this.startActivity(intent);
        }
    }
}
