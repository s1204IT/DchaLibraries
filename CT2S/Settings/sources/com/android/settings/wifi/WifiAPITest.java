package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.widget.EditText;
import com.android.settings.R;

public class WifiAPITest extends PreferenceActivity implements Preference.OnPreferenceClickListener {
    private Preference mWifiDisableNetwork;
    private Preference mWifiDisconnect;
    private Preference mWifiEnableNetwork;
    private WifiManager mWifiManager;
    private int netid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreatePreferences();
        this.mWifiManager = (WifiManager) getSystemService("wifi");
    }

    private void onCreatePreferences() {
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
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref == this.mWifiDisconnect) {
            this.mWifiManager.disconnect();
            return true;
        }
        if (pref == this.mWifiDisableNetwork) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Input");
            alert.setMessage("Enter Network ID");
            final EditText input = new EditText(this);
            alert.setView(input);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    Editable value = input.getText();
                    WifiAPITest.this.netid = Integer.parseInt(value.toString());
                    WifiAPITest.this.mWifiManager.disableNetwork(WifiAPITest.this.netid);
                }
            });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
            alert.show();
            return true;
        }
        if (pref == this.mWifiEnableNetwork) {
            AlertDialog.Builder alert2 = new AlertDialog.Builder(this);
            alert2.setTitle("Input");
            alert2.setMessage("Enter Network ID");
            final EditText input2 = new EditText(this);
            alert2.setView(input2);
            alert2.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    Editable value = input2.getText();
                    WifiAPITest.this.netid = Integer.parseInt(value.toString());
                    WifiAPITest.this.mWifiManager.enableNetwork(WifiAPITest.this.netid, false);
                }
            });
            alert2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
            alert2.show();
            return true;
        }
        return true;
    }
}
