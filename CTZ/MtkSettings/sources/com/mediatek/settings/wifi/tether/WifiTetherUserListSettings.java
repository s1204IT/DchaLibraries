package com.mediatek.settings.wifi.tether;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.mediatek.settings.wifi.tether.WifiTetherUserListSettings;
import java.util.List;
import mediatek.net.wifi.HotspotClient;
import mediatek.net.wifi.WifiHotspotManager;

public class WifiTetherUserListSettings extends RestrictedDashboardFragment {
    private static final IntentFilter WIFI_TETHER_USER_CHANGED_FILTER = new IntentFilter("android.net.wifi.WIFI_HOTSPOT_CLIENTS_IP_READY");
    private WifiHotspotManager mHotspotManager;
    final BroadcastReceiver mReceiver;
    private int mUserMode;

    static {
        WIFI_TETHER_USER_CHANGED_FILTER.addAction("android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED");
    }

    public WifiTetherUserListSettings() {
        super("no_config_tethering");
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiTetherUserListSettings.this.updateWifiApClients();
            }
        };
        this.mUserMode = 0;
    }

    @Override
    public int getMetricsCategory() {
        return 1014;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mHotspotManager = ((WifiManager) context.getSystemService("wifi")).getWifiHotspotManager();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle arguments = getArguments();
        this.mUserMode = arguments != null ? arguments.getInt("usermode") : 0;
        if (this.mUserMode == 0) {
            getPreferenceScreen().setTitle(R.string.wifi_ap_connected_title);
        } else if (this.mUserMode == 1) {
            getPreferenceScreen().setTitle(R.string.wifi_ap_blocked_title);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateWifiApClients();
    }

    @Override
    public void onStart() {
        super.onStart();
        Context context = getContext();
        if (context != null) {
            context.registerReceiver(this.mReceiver, WIFI_TETHER_USER_CHANGED_FILTER);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Context context = getContext();
        if (context != null) {
            context.unregisterReceiver(this.mReceiver);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return -1;
    }

    public void updateWifiApClients() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) {
            return;
        }
        preferenceScreen.removeAll();
        Context context = preferenceScreen.getPreferenceManager().getContext();
        List<HotspotClient> hotspotClients = this.mHotspotManager.getHotspotClients();
        if (hotspotClients != null) {
            Log.d("WifiTetherUserListSettings", "client number is " + hotspotClients.size());
            for (final HotspotClient hotspotClient : hotspotClients) {
                if ((this.mUserMode == 0 && !hotspotClient.isBlocked) || (this.mUserMode == 1 && hotspotClient.isBlocked)) {
                    String clientDeviceName = this.mHotspotManager.getClientDeviceName(hotspotClient.deviceAddress);
                    Preference preference = new Preference(context);
                    preference.setTitle(clientDeviceName);
                    preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public final boolean onPreferenceClick(Preference preference2) {
                            return WifiTetherUserListSettings.lambda$updateWifiApClients$0(this.f$0, hotspotClient, preference2);
                        }
                    });
                    preferenceScreen.addPreference(preference);
                }
            }
        }
    }

    public static boolean lambda$updateWifiApClients$0(WifiTetherUserListSettings wifiTetherUserListSettings, HotspotClient hotspotClient, Preference preference) {
        new WifiTetherClientFragment(hotspotClient, wifiTetherUserListSettings.mHotspotManager).show(wifiTetherUserListSettings.getFragmentManager(), "WifiTetherClientFragment");
        return true;
    }

    public static class WifiTetherClientFragment extends InstrumentedDialogFragment {
        private static HotspotClient sClient;
        private static WifiHotspotManager sHotspotManager;

        public WifiTetherClientFragment() {
        }

        public WifiTetherClientFragment(HotspotClient hotspotClient, WifiHotspotManager wifiHotspotManager) {
            sClient = hotspotClient;
            sHotspotManager = wifiHotspotManager;
        }

        @Override
        public int getMetricsCategory() {
            return 542;
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            AlertDialog alertDialogCreate = new AlertDialog.Builder(getActivity()).setPositiveButton(sClient.isBlocked ? R.string.wifi_ap_client_unblock_title : R.string.wifi_ap_client_block_title, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    WifiTetherUserListSettings.WifiTetherClientFragment.lambda$onCreateDialog$0(dialogInterface, i);
                }
            }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
            View viewInflate = getActivity().getLayoutInflater().inflate(R.layout.wifi_ap_client_dialog, (ViewGroup) null);
            ((TextView) viewInflate.findViewById(R.id.mac_address)).setText(sClient.deviceAddress);
            if (sClient.isBlocked) {
                viewInflate.findViewById(R.id.ip_filed).setVisibility(8);
            } else {
                viewInflate.findViewById(R.id.ip_filed).setVisibility(0);
                ((TextView) viewInflate.findViewById(R.id.ip_address)).setText(sHotspotManager.getClientIp(sClient.deviceAddress));
            }
            alertDialogCreate.setTitle(sHotspotManager.getClientDeviceName(sClient.deviceAddress));
            alertDialogCreate.setView(viewInflate);
            return alertDialogCreate;
        }

        static void lambda$onCreateDialog$0(DialogInterface dialogInterface, int i) {
            if (sClient.isBlocked) {
                Log.d("WifiTetherUserListSettings", "onClick,client is blocked, unblock now");
                sHotspotManager.unblockClient(sClient);
            } else {
                Log.d("WifiTetherUserListSettings", "onClick,client isn't blocked, block now");
                sHotspotManager.blockClient(sClient);
            }
        }
    }

    @Override
    protected String getLogTag() {
        return "WifiTetherUserListSettings";
    }
}
