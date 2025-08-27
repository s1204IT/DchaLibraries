package com.mediatek.settings.wifi.tether;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.wifi.tether.WifiTetherBasePreferenceController;
import com.mediatek.settings.wifi.tether.WifiTetherResetPreferenceController;

/* loaded from: classes.dex */
public class WifiTetherResetPreferenceController extends WifiTetherBasePreferenceController {
    private final FragmentManager mFragmentManager;
    private Preference mResetNetworkPref;

    public WifiTetherResetPreferenceController(Context context, WifiTetherBasePreferenceController.OnTetherConfigUpdateListener onTetherConfigUpdateListener, FragmentManager fragmentManager) {
        super(context, onTetherConfigUpdateListener);
        this.mFragmentManager = fragmentManager;
    }

    @Override // com.android.settings.wifi.tether.WifiTetherBasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "wifi_tether_network_reset";
    }

    @Override // com.android.settings.wifi.tether.WifiTetherBasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mResetNetworkPref = preferenceScreen.findPreference("wifi_tether_network_reset");
        if (this.mResetNetworkPref == null) {
            return;
        }
        this.mResetNetworkPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.mediatek.settings.wifi.tether.-$$Lambda$WifiTetherResetPreferenceController$mfWCzapxXAW8MEUEh6X4wIuj8EU
            @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
            public final boolean onPreferenceClick(Preference preference) {
                return WifiTetherResetPreferenceController.lambda$displayPreference$0(this.f$0, preference);
            }
        });
    }

    public static /* synthetic */ boolean lambda$displayPreference$0(WifiTetherResetPreferenceController wifiTetherResetPreferenceController, Preference preference) {
        new ResetNetworkFragment(wifiTetherResetPreferenceController.mListener).show(wifiTetherResetPreferenceController.mFragmentManager, "wifi_tether_network_reset");
        return true;
    }

    @Override // com.android.settings.wifi.tether.WifiTetherBasePreferenceController
    public void updateDisplay() {
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        return true;
    }

    public static class ResetNetworkFragment extends InstrumentedDialogFragment {
        private static WifiTetherBasePreferenceController.OnTetherConfigUpdateListener sListener;

        public ResetNetworkFragment() {
        }

        public ResetNetworkFragment(WifiTetherBasePreferenceController.OnTetherConfigUpdateListener onTetherConfigUpdateListener) {
            sListener = onTetherConfigUpdateListener;
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 542;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            AlertDialog alertDialogCreate = new AlertDialog.Builder(getActivity()).setPositiveButton(R.string.wifi_ap_reset_OOB, new DialogInterface.OnClickListener() { // from class: com.mediatek.settings.wifi.tether.-$$Lambda$WifiTetherResetPreferenceController$ResetNetworkFragment$TLRVfJsPOAiLfdkMhgnEvyCRCv8
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    WifiTetherResetPreferenceController.ResetNetworkFragment.sListener.onNetworkReset();
                }
            }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
            alertDialogCreate.setTitle(R.string.wifi_ap_reset_OOB);
            alertDialogCreate.setMessage(getActivity().getString(R.string.wifi_ap_reset_OOB_title));
            return alertDialogCreate;
        }
    }
}
