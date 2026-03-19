package com.android.certinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.ConfigParser;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class WiFiInstaller extends Activity {
    boolean doNotInstall;
    PasspointConfiguration mPasspointConfig;
    WifiManager mWifiManager;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle extras = getIntent().getExtras();
        String string = extras.getString("wifi-config-file");
        String string2 = extras.getString("wifi-config");
        byte[] byteArray = extras.getByteArray("wifi-config-data");
        StringBuilder sb = new StringBuilder();
        sb.append("WiFi data for wifi-config: ");
        sb.append(string2);
        sb.append(" is ");
        sb.append(byteArray != null ? Integer.valueOf(byteArray.length) : "-");
        Log.d("WifiInstaller", sb.toString());
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        this.mPasspointConfig = ConfigParser.parsePasspointConfig(string2, byteArray);
        dropFile(Uri.parse(string), getApplicationContext());
        if (this.mPasspointConfig == null) {
            Log.w("WifiInstaller", "failed to build passpoint configuration");
            this.doNotInstall = true;
        } else if (this.mPasspointConfig.getHomeSp() == null) {
            Log.w("WifiInstaller", "Passpoint profile missing HomeSP information");
            this.doNotInstall = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        createMainDialog();
    }

    private void createMainDialog() {
        Resources resources = getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View viewInflate = getLayoutInflater().inflate(R.layout.wifi_main_dialog, (ViewGroup) null);
        builder.setView(viewInflate);
        TextView textView = (TextView) viewInflate.findViewById(R.id.wifi_info);
        if (!this.doNotInstall) {
            textView.setText(String.format(getResources().getString(R.string.wifi_installer_detail), this.mPasspointConfig.getHomeSp().getFriendlyName()));
            builder.setTitle(this.mPasspointConfig.getHomeSp().getFriendlyName());
            builder.setIcon(resources.getDrawable(R.drawable.signal_wifi_4_bar_lock_black_24dp));
            builder.setPositiveButton(R.string.wifi_install_label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(WiFiInstaller.this, WiFiInstaller.this.getString(R.string.wifi_installing_label), 1).show();
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            boolean z;
                            try {
                                WiFiInstaller.this.mWifiManager.addOrUpdatePasspointConfiguration(WiFiInstaller.this.mPasspointConfig);
                                z = true;
                            } catch (RuntimeException e) {
                                Log.w("WifiInstaller", "Caught exception while installing wifi config: " + e, e);
                                z = false;
                            }
                            if (z) {
                                Intent intent = new Intent(WiFiInstaller.this.getApplicationContext(), (Class<?>) CredentialsInstallDialog.class);
                                intent.putExtra("network_name", WiFiInstaller.this.mPasspointConfig.getHomeSp().getFriendlyName());
                                intent.putExtra("install_state", 2);
                                WiFiInstaller.this.startActivity(intent);
                            } else {
                                Intent intent2 = new Intent(WiFiInstaller.this.getApplicationContext(), (Class<?>) CredentialsInstallDialog.class);
                                intent2.putExtra("install_state", 1);
                                WiFiInstaller.this.startActivity(intent2);
                            }
                            WiFiInstaller.this.finish();
                        }
                    });
                    dialogInterface.dismiss();
                }
            });
            builder.setNegativeButton(R.string.wifi_cancel_label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    WiFiInstaller.this.finish();
                }
            });
        } else {
            textView.setText(getResources().getString(R.string.wifi_installer_download_error));
            builder.setPositiveButton(R.string.done_label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    WiFiInstaller.this.finish();
                }
            });
        }
        builder.create().show();
    }

    private static void dropFile(Uri uri, Context context) {
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.getContentResolver(), uri);
            } else {
                context.getContentResolver().delete(uri, null, null);
            }
        } catch (Exception e) {
            Log.e("WifiInstaller", "could not delete document " + uri);
        }
    }
}
