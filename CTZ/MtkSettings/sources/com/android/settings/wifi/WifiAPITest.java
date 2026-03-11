package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.Editable;
import android.widget.EditText;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class WifiAPITest extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener {
    private Preference mWifiDisableNetwork;
    private Preference mWifiDisconnect;
    private Preference mWifiEnableNetwork;
    private WifiManager mWifiManager;
    private int netid;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mWifiManager = (WifiManager) getSystemService("wifi");
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String str) {
        addPreferencesFromResource(R.layout.wifi_api_test);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mWifiDisconnect = preferenceScreen.findPreference("disconnect");
        this.mWifiDisconnect.setOnPreferenceClickListener(this);
        this.mWifiDisableNetwork = preferenceScreen.findPreference("disable_network");
        this.mWifiDisableNetwork.setOnPreferenceClickListener(this);
        this.mWifiEnableNetwork = preferenceScreen.findPreference("enable_network");
        this.mWifiEnableNetwork.setOnPreferenceClickListener(this);
    }

    @Override
    public int getMetricsCategory() {
        return 89;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        super.onPreferenceTreeClick(preference);
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == this.mWifiDisconnect) {
            this.mWifiManager.disconnect();
            return true;
        }
        if (preference == this.mWifiDisableNetwork) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Input");
            builder.setMessage("Enter Network ID");
            final EditText editText = new EditText(getPrefContext());
            builder.setView(editText);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Editable text = editText.getText();
                    try {
                        WifiAPITest.this.netid = Integer.parseInt(text.toString());
                        WifiAPITest.this.mWifiManager.disableNetwork(WifiAPITest.this.netid);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            builder.show();
            return true;
        }
        if (preference == this.mWifiEnableNetwork) {
            AlertDialog.Builder builder2 = new AlertDialog.Builder(getContext());
            builder2.setTitle("Input");
            builder2.setMessage("Enter Network ID");
            final EditText editText2 = new EditText(getPrefContext());
            builder2.setView(editText2);
            builder2.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Editable text = editText2.getText();
                    WifiAPITest.this.netid = Integer.parseInt(text.toString());
                    WifiAPITest.this.mWifiManager.enableNetwork(WifiAPITest.this.netid, false);
                }
            });
            builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            builder2.show();
            return true;
        }
        return true;
    }
}
