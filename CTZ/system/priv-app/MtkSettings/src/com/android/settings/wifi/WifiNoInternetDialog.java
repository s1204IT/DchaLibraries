package com.android.settings.wifi;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

/* loaded from: classes.dex */
public final class WifiNoInternetDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private String mAction;
    private CheckBox mAlwaysAllow;
    private ConnectivityManager mCM;
    private Network mNetwork;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private String mNetworkName;

    private boolean isKnownAction(Intent intent) {
        return intent.getAction().equals("android.net.conn.PROMPT_UNVALIDATED") || intent.getAction().equals("android.net.conn.PROMPT_LOST_VALIDATION");
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        if (intent == null || !isKnownAction(intent) || !"netId".equals(intent.getScheme())) {
            Log.e("WifiNoInternetDialog", "Unexpected intent " + intent + ", exiting");
            finish();
            return;
        }
        this.mAction = intent.getAction();
        try {
            this.mNetwork = new Network(Integer.parseInt(intent.getData().getSchemeSpecificPart()));
        } catch (NullPointerException | NumberFormatException e) {
            this.mNetwork = null;
        }
        if (this.mNetwork == null) {
            Log.e("WifiNoInternetDialog", "Can't determine network from '" + intent.getData() + "' , exiting");
            finish();
            return;
        }
        NetworkRequest networkRequestBuild = new NetworkRequest.Builder().clearCapabilities().build();
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() { // from class: com.android.settings.wifi.WifiNoInternetDialog.1
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                if (WifiNoInternetDialog.this.mNetwork.equals(network)) {
                    Log.d("WifiNoInternetDialog", "Network " + WifiNoInternetDialog.this.mNetwork + " disconnected");
                    WifiNoInternetDialog.this.finish();
                }
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                if (WifiNoInternetDialog.this.mNetwork.equals(network) && networkCapabilities.hasCapability(16)) {
                    Log.d("WifiNoInternetDialog", "Network " + WifiNoInternetDialog.this.mNetwork + " validated");
                    WifiNoInternetDialog.this.finish();
                }
            }
        };
        this.mCM = (ConnectivityManager) getSystemService("connectivity");
        this.mCM.registerNetworkCallback(networkRequestBuild, this.mNetworkCallback);
        NetworkInfo networkInfo = this.mCM.getNetworkInfo(this.mNetwork);
        NetworkCapabilities networkCapabilities = this.mCM.getNetworkCapabilities(this.mNetwork);
        if (networkInfo == null || !networkInfo.isConnectedOrConnecting() || networkCapabilities == null) {
            Log.d("WifiNoInternetDialog", "Network " + this.mNetwork + " is not connected: " + networkInfo);
            finish();
            return;
        }
        this.mNetworkName = networkCapabilities.getSSID();
        if (this.mNetworkName != null) {
            this.mNetworkName = android.net.wifi.WifiInfo.removeDoubleQuotes(this.mNetworkName);
        }
        createDialog();
    }

    private void createDialog() {
        this.mAlert.setIcon(R.drawable.ic_settings_wireless);
        AlertController.AlertParams alertParams = this.mAlertParams;
        if ("android.net.conn.PROMPT_UNVALIDATED".equals(this.mAction)) {
            alertParams.mTitle = this.mNetworkName;
            alertParams.mMessage = getString(R.string.no_internet_access_text);
            alertParams.mPositiveButtonText = getString(R.string.yes);
            alertParams.mNegativeButtonText = getString(R.string.no);
        } else {
            alertParams.mTitle = getString(R.string.lost_internet_access_title);
            alertParams.mMessage = getString(R.string.lost_internet_access_text);
            alertParams.mPositiveButtonText = getString(R.string.lost_internet_access_switch);
            alertParams.mNegativeButtonText = getString(R.string.lost_internet_access_cancel);
        }
        alertParams.mPositiveButtonListener = this;
        alertParams.mNegativeButtonListener = this;
        View viewInflate = LayoutInflater.from(alertParams.mContext).inflate(android.R.layout.alert_dialog_progress, (ViewGroup) null);
        alertParams.mView = viewInflate;
        this.mAlwaysAllow = (CheckBox) viewInflate.findViewById(android.R.id.accessibility_permissionDialog_title);
        if ("android.net.conn.PROMPT_UNVALIDATED".equals(this.mAction)) {
            this.mAlwaysAllow.setText(getString(R.string.no_internet_access_remember));
        } else {
            this.mAlwaysAllow.setText(getString(R.string.lost_internet_access_persist));
        }
        setupAlert();
    }

    protected void onDestroy() {
        if (this.mNetworkCallback != null) {
            this.mCM.unregisterNetworkCallback(this.mNetworkCallback);
            this.mNetworkCallback = null;
        }
        super.onDestroy();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        String str;
        boolean z;
        String str2;
        if (i == -2 || i == -1) {
            boolean zIsChecked = this.mAlwaysAllow.isChecked();
            if ("android.net.conn.PROMPT_UNVALIDATED".equals(this.mAction)) {
                str = "NO_INTERNET";
                z = i == -1;
                str2 = z ? "Connect" : "Ignore";
                this.mCM.setAcceptUnvalidated(this.mNetwork, z, zIsChecked);
            } else {
                str = "LOST_INTERNET";
                z = i == -1;
                str2 = z ? "Switch away" : "Get stuck";
                if (zIsChecked) {
                    Settings.Global.putString(this.mAlertParams.mContext.getContentResolver(), "network_avoid_bad_wifi", z ? "1" : "0");
                } else if (z) {
                    this.mCM.setAvoidUnvalidated(this.mNetwork);
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(": ");
            sb.append(str2);
            sb.append(" network=");
            sb.append(this.mNetwork);
            sb.append(zIsChecked ? " and remember" : "");
            Log.d("WifiNoInternetDialog", sb.toString());
        }
    }
}
