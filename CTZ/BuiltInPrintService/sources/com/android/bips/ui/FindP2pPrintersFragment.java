package com.android.bips.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import com.android.bips.BuiltInPrintService;
import com.android.bips.R;
import com.android.bips.discovery.DiscoveredPrinter;
import com.android.bips.discovery.P2pDiscovery;
import com.android.bips.p2p.P2pPeerListener;
import com.android.bips.ui.AddPrintersActivity;
import com.android.bips.ui.FindP2pPrintersFragment;
import java.util.Iterator;

public class FindP2pPrintersFragment extends PreferenceFragment implements ServiceConnection, AddPrintersActivity.OnPermissionChangeListener {
    private static final String TAG = FindP2pPrintersFragment.class.getSimpleName();
    private PreferenceCategory mAvailableCategory;
    private P2pListener mPeerDiscoveryListener;
    private BuiltInPrintService mPrintService;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.find_p2p_prefs);
        this.mAvailableCategory = (PreferenceCategory) getPreferenceScreen().findPreference("available");
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.wifi_direct_printers);
        getContext().bindService(new Intent(getContext(), (Class<?>) BuiltInPrintService.class), this, 1);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.mPeerDiscoveryListener != null) {
            this.mPrintService.getP2pMonitor().stopDiscover(this.mPeerDiscoveryListener);
            this.mPeerDiscoveryListener = null;
        }
        getContext().unbindService(this);
        this.mPrintService = null;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        this.mPrintService = BuiltInPrintService.getInstance();
        if (this.mPrintService == null) {
            return;
        }
        if (getContext().checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") != 0) {
            getActivity().requestPermissions(new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, 1);
        } else {
            startP2pDiscovery();
        }
    }

    private void startP2pDiscovery() {
        if (this.mPrintService != null && this.mPeerDiscoveryListener == null) {
            this.mPeerDiscoveryListener = new P2pListener();
            this.mPrintService.getP2pMonitor().discover(this.mPeerDiscoveryListener);
        }
    }

    @Override
    public void onPermissionChange() {
        if (getContext().checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == 0) {
            startP2pDiscovery();
        } else {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        this.mPrintService = null;
    }

    private class P2pListener implements P2pPeerListener {
        private P2pListener() {
        }

        @Override
        public void onPeerFound(final WifiP2pDevice wifiP2pDevice) {
            if (FindP2pPrintersFragment.this.mPrintService == null) {
                return;
            }
            DiscoveredPrinter printer = P2pDiscovery.toPrinter(wifiP2pDevice);
            Iterator<DiscoveredPrinter> it = FindP2pPrintersFragment.this.mPrintService.getP2pDiscovery().getSavedPrinters().iterator();
            while (it.hasNext()) {
                if (it.next().path.equals(printer.path)) {
                    return;
                }
            }
            PrinterPreference printerPreference = getPrinterPreference(printer.getUri());
            if (printerPreference != null) {
                printerPreference.updatePrinter(printer);
                return;
            }
            PrinterPreference printerPreference2 = new PrinterPreference(FindP2pPrintersFragment.this.getContext(), FindP2pPrintersFragment.this.mPrintService, printer, true);
            printerPreference2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public final boolean onPreferenceClick(Preference preference) {
                    return FindP2pPrintersFragment.P2pListener.lambda$onPeerFound$0(this.f$0, wifiP2pDevice, preference);
                }
            });
            FindP2pPrintersFragment.this.mAvailableCategory.addPreference(printerPreference2);
        }

        public static boolean lambda$onPeerFound$0(P2pListener p2pListener, WifiP2pDevice wifiP2pDevice, Preference preference) {
            new AddP2pPrinterDialog(FindP2pPrintersFragment.this, FindP2pPrintersFragment.this.mPrintService, wifiP2pDevice).show();
            return true;
        }

        @Override
        public void onPeerLost(WifiP2pDevice wifiP2pDevice) {
            PrinterPreference printerPreference;
            if (FindP2pPrintersFragment.this.mPrintService != null && (printerPreference = getPrinterPreference(P2pDiscovery.toPrinter(wifiP2pDevice).path)) != null) {
                FindP2pPrintersFragment.this.mAvailableCategory.removePreference(printerPreference);
            }
        }

        private PrinterPreference getPrinterPreference(Uri uri) {
            for (int i = 0; i < FindP2pPrintersFragment.this.mAvailableCategory.getPreferenceCount(); i++) {
                ?? preference = FindP2pPrintersFragment.this.mAvailableCategory.getPreference(i);
                if ((preference instanceof PrinterPreference) && preference.getPrinter().path.equals(uri)) {
                    return preference;
                }
            }
            return null;
        }
    }
}
