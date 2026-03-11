package com.android.settings.wifi;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.SetupWizardUtils;
import com.android.settings.wifi.WifiDialog;
import com.android.settingslib.wifi.AccessPoint;
import com.android.setupwizardlib.util.WizardManagerHelper;

public class WifiDialogActivity extends Activity implements WifiDialog.WifiDialogListener, DialogInterface.OnDismissListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (WizardManagerHelper.isSetupWizardIntent(intent)) {
            setTheme(SetupWizardUtils.getTransparentTheme(intent));
        }
        super.onCreate(savedInstanceState);
        Bundle accessPointState = intent.getBundleExtra("access_point_state");
        AccessPoint accessPoint = null;
        if (accessPointState != null) {
            accessPoint = new AccessPoint(this, accessPointState);
        }
        WifiDialog dialog = new WifiDialog(this, this, accessPoint, 1);
        dialog.show();
        dialog.setOnDismissListener(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onForget(WifiDialog dialog) {
        WifiManager wifiManager = (WifiManager) getSystemService(WifiManager.class);
        AccessPoint accessPoint = dialog.getController().getAccessPoint();
        if (accessPoint != null) {
            if (!accessPoint.isSaved()) {
                if (accessPoint.getNetworkInfo() != null && accessPoint.getNetworkInfo().getState() != NetworkInfo.State.DISCONNECTED) {
                    wifiManager.disableEphemeralNetwork(AccessPoint.convertToQuotedString(accessPoint.getSsidStr()));
                } else {
                    Log.e("WifiDialogActivity", "Failed to forget invalid network " + accessPoint.getConfig());
                }
            } else {
                wifiManager.forget(accessPoint.getConfig().networkId, null);
            }
        }
        Intent resultData = new Intent();
        if (accessPoint != null) {
            Bundle accessPointState = new Bundle();
            accessPoint.saveWifiState(accessPointState);
            resultData.putExtra("access_point_state", accessPointState);
        }
        setResult(2);
        finish();
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
        NetworkInfo networkInfo;
        WifiConfiguration config = dialog.getController().getConfig();
        AccessPoint accessPoint = dialog.getController().getAccessPoint();
        WifiManager wifiManager = (WifiManager) getSystemService(WifiManager.class);
        if (config == null) {
            if (accessPoint != null && accessPoint.isSaved()) {
                wifiManager.connect(accessPoint.getConfig(), null);
            }
        } else {
            wifiManager.save(config, null);
            if (accessPoint != null && ((networkInfo = accessPoint.getNetworkInfo()) == null || !networkInfo.isConnected())) {
                wifiManager.connect(config, null);
            }
        }
        Intent resultData = new Intent();
        if (accessPoint != null) {
            Bundle accessPointState = new Bundle();
            accessPoint.saveWifiState(accessPointState);
            resultData.putExtra("access_point_state", accessPointState);
        }
        if (config != null) {
            resultData.putExtra("wifi_configuration", config);
        }
        setResult(1, resultData);
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        finish();
    }
}
