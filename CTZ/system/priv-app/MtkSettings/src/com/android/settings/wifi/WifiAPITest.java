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

/* loaded from: classes.dex */
public class WifiAPITest extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener {
    private Preference mWifiDisableNetwork;
    private Preference mWifiDisconnect;
    private Preference mWifiEnableNetwork;
    private WifiManager mWifiManager;
    private int netid;

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mWifiManager = (WifiManager) getSystemService("wifi");
    }

    @Override // com.android.settings.core.InstrumentedPreferenceFragment, android.support.v14.preference.PreferenceFragment
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

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 89;
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        super.onPreferenceTreeClick(preference);
        return false;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
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
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() { // from class: com.android.settings.wifi.WifiAPITest.1
                @Override // android.content.DialogInterface.OnClickListener
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
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() { // from class: com.android.settings.wifi.WifiAPITest.2
                @Override // android.content.DialogInterface.OnClickListener
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
            builder2.setPositiveButton("Ok", new DialogInterface.OnClickListener() { // from class: com.android.settings.wifi.WifiAPITest.3
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    Editable text = editText2.getText();
                    WifiAPITest.this.netid = Integer.parseInt(text.toString());
                    WifiAPITest.this.mWifiManager.enableNetwork(WifiAPITest.this.netid, false);
                }
            });
            builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() { // from class: com.android.settings.wifi.WifiAPITest.4
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            builder2.show();
            return true;
        }
        return true;
    }
}
