package com.android.settings.wifi;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

public final class WifiNoInternetDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private CheckBox mAlwaysAllow;
    private ConnectivityManager mCM;
    private Network mNetwork;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private String mNetworkName;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null || !intent.getAction().equals("android.net.conn.PROMPT_UNVALIDATED") || !"netId".equals(intent.getScheme())) {
            Log.e("WifiNoInternetDialog", "Unexpected intent " + intent + ", exiting");
            finish();
            return;
        }
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
        NetworkRequest request = new NetworkRequest.Builder().clearCapabilities().build();
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(Network network) {
                if (!WifiNoInternetDialog.this.mNetwork.equals(network)) {
                    return;
                }
                Log.d("WifiNoInternetDialog", "Network " + WifiNoInternetDialog.this.mNetwork + " disconnected");
                WifiNoInternetDialog.this.finish();
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
                if (!WifiNoInternetDialog.this.mNetwork.equals(network) || !nc.hasCapability(16)) {
                    return;
                }
                Log.d("WifiNoInternetDialog", "Network " + WifiNoInternetDialog.this.mNetwork + " validated");
                WifiNoInternetDialog.this.finish();
            }
        };
        this.mCM = (ConnectivityManager) getSystemService("connectivity");
        this.mCM.registerNetworkCallback(request, this.mNetworkCallback);
        NetworkInfo ni = this.mCM.getNetworkInfo(this.mNetwork);
        if (ni == null || !ni.isConnectedOrConnecting()) {
            Log.d("WifiNoInternetDialog", "Network " + this.mNetwork + " is not connected: " + ni);
            finish();
        } else {
            this.mNetworkName = ni.getExtraInfo();
            if (this.mNetworkName != null) {
                this.mNetworkName = this.mNetworkName.replaceAll("^\"|\"$", "");
            }
            createDialog();
        }
    }

    private void createDialog() {
        this.mAlert.setIcon(R.drawable.ic_settings_wireless);
        AlertController.AlertParams ap = this.mAlertParams;
        ap.mTitle = this.mNetworkName;
        ap.mMessage = getString(R.string.no_internet_access_text);
        ap.mPositiveButtonText = getString(R.string.yes);
        ap.mNegativeButtonText = getString(R.string.no);
        ap.mPositiveButtonListener = this;
        ap.mNegativeButtonListener = this;
        LayoutInflater inflater = LayoutInflater.from(ap.mContext);
        View checkbox = inflater.inflate(android.R.layout.alert_dialog_icon_button_watch, (ViewGroup) null);
        ap.mView = checkbox;
        this.mAlwaysAllow = (CheckBox) checkbox.findViewById(android.R.id.flagServiceHandlesDoubleTap);
        this.mAlwaysAllow.setText(getString(R.string.no_internet_access_remember));
        setupAlert();
    }

    protected void onDestroy() {
        if (this.mNetworkCallback != null) {
            this.mCM.unregisterNetworkCallback(this.mNetworkCallback);
            this.mNetworkCallback = null;
        }
        super.onDestroy();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        boolean accept = which == -1;
        String action = accept ? "Connect" : "Ignore";
        boolean always = this.mAlwaysAllow.isChecked();
        switch (which) {
            case -2:
            case -1:
                this.mCM.setAcceptUnvalidated(this.mNetwork, accept, always);
                Log.d("WifiNoInternetDialog", action + " network=" + this.mNetwork + (always ? " and remember" : ""));
                break;
        }
    }
}
