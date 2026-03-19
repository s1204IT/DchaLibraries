package com.android.bips.ui;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import com.android.bips.BuiltInPrintService;
import com.android.bips.R;
import com.android.bips.discovery.DiscoveredPrinter;
import com.android.bips.p2p.P2pUtils;

public class AddPrintersFragment extends PreferenceFragment implements ServiceConnection {
    private static final String TAG = AddPrintersFragment.class.getSimpleName();
    private Preference mAddPrinterByIpPreference;
    private BuiltInPrintService mPrintService;
    private PreferenceCategory mSavedPrintersCategory;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.add_printers_prefs);
        this.mAddPrinterByIpPreference = getPreferenceScreen().findPreference("add_by_ip");
        getPreferenceScreen().findPreference("find_wifi_direct").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference) {
                return AddPrintersFragment.lambda$onCreate$0(this.f$0, preference);
            }
        });
        if (!getContext().getApplicationContext().getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
            getPreferenceScreen().removePreference(findPreference("find_wifi_direct"));
        }
        this.mSavedPrintersCategory = (PreferenceCategory) getPreferenceScreen().findPreference("saved_printers");
    }

    public static boolean lambda$onCreate$0(AddPrintersFragment addPrintersFragment, Preference preference) {
        addPrintersFragment.getFragmentManager().beginTransaction().replace(android.R.id.content, new FindP2pPrintersFragment()).addToBackStack(null).commit();
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.title_activity_add_printer);
        getContext().bindService(new Intent(getContext(), (Class<?>) BuiltInPrintService.class), this, 1);
    }

    @Override
    public void onStop() {
        super.onStop();
        getContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        this.mPrintService = BuiltInPrintService.getInstance();
        this.mAddPrinterByIpPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference) {
                return AddPrintersFragment.lambda$onServiceConnected$2(this.f$0, preference);
            }
        });
        updateSavedPrinters();
    }

    public static boolean lambda$onServiceConnected$2(final AddPrintersFragment addPrintersFragment, Preference preference) {
        AddManualPrinterDialog addManualPrinterDialog = new AddManualPrinterDialog(addPrintersFragment.getActivity(), addPrintersFragment.mPrintService.getManualDiscovery());
        addManualPrinterDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public final void onDismiss(DialogInterface dialogInterface) {
                this.f$0.updateSavedPrinters();
            }
        });
        addManualPrinterDialog.show();
        return true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        this.mPrintService = null;
    }

    private void updateSavedPrinters() {
        if (this.mPrintService.getDiscovery().getSavedPrinters().size() == 0) {
            if (getPreferenceScreen().findPreference(this.mSavedPrintersCategory.getKey()) != null) {
                getPreferenceScreen().removePreference(this.mSavedPrintersCategory);
                return;
            }
            return;
        }
        if (getPreferenceScreen().findPreference(this.mSavedPrintersCategory.getKey()) == null) {
            getPreferenceScreen().addPreference(this.mSavedPrintersCategory);
        }
        this.mSavedPrintersCategory.removeAll();
        for (final DiscoveredPrinter discoveredPrinter : this.mPrintService.getDiscovery().getSavedPrinters()) {
            PrinterPreference printerPreference = new PrinterPreference(getContext(), this.mPrintService, discoveredPrinter, false);
            printerPreference.setOrder(2);
            printerPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public final boolean onPreferenceClick(Preference preference) {
                    return AddPrintersFragment.lambda$updateSavedPrinters$3(this.f$0, discoveredPrinter, preference);
                }
            });
            this.mSavedPrintersCategory.addPreference(printerPreference);
        }
    }

    public static boolean lambda$updateSavedPrinters$3(AddPrintersFragment addPrintersFragment, DiscoveredPrinter discoveredPrinter, Preference preference) {
        addPrintersFragment.showRemovalDialog(discoveredPrinter);
        return true;
    }

    private void showRemovalDialog(final DiscoveredPrinter discoveredPrinter) {
        String string;
        if (P2pUtils.isP2p(discoveredPrinter)) {
            string = this.mPrintService.getString(R.string.connects_via_wifi_direct);
        } else {
            string = this.mPrintService.getString(R.string.connects_via_network, discoveredPrinter.path);
        }
        new AlertDialog.Builder(getContext()).setTitle(discoveredPrinter.name).setMessage(string).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.forget, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                AddPrintersFragment.lambda$showRemovalDialog$4(this.f$0, discoveredPrinter, dialogInterface, i);
            }
        }).show();
    }

    public static void lambda$showRemovalDialog$4(AddPrintersFragment addPrintersFragment, DiscoveredPrinter discoveredPrinter, DialogInterface dialogInterface, int i) {
        addPrintersFragment.mPrintService.getDiscovery().removeSavedPrinter(discoveredPrinter.path);
        addPrintersFragment.updateSavedPrinters();
    }
}
