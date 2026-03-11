package com.android.settings.vpn2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.security.Credentials;
import android.security.KeyStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnProfile;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.vpn2.ConfirmLockdownFragment;

public class ConfigDialogFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnShowListener, View.OnClickListener, ConfirmLockdownFragment.ConfirmLockdownListener {
    private Context mContext;
    private final IConnectivityManager mService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
    private boolean mUnlocking = false;

    @Override
    public int getMetricsCategory() {
        return 545;
    }

    public static void show(VpnSettings vpnSettings, VpnProfile vpnProfile, boolean z, boolean z2) {
        if (vpnSettings.isAdded()) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("profile", vpnProfile);
            bundle.putBoolean("editing", z);
            bundle.putBoolean("exists", z2);
            ConfigDialogFragment configDialogFragment = new ConfigDialogFragment();
            configDialogFragment.setArguments(bundle);
            configDialogFragment.setTargetFragment(vpnSettings, 0);
            configDialogFragment.show(vpnSettings.getFragmentManager(), "vpnconfigdialog");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!KeyStore.getInstance().isUnlocked()) {
            if (!this.mUnlocking) {
                Credentials.getInstance().unlock(this.mContext);
            } else {
                dismiss();
            }
            this.mUnlocking = !this.mUnlocking;
            return;
        }
        this.mUnlocking = false;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle arguments = getArguments();
        ConfigDialog configDialog = new ConfigDialog(getActivity(), this, arguments.getParcelable("profile"), arguments.getBoolean("editing"), arguments.getBoolean("exists"));
        configDialog.setOnShowListener(this);
        return configDialog;
    }

    @Override
    public void onShow(DialogInterface dialogInterface) {
        AlertDialog alertDialog = (AlertDialog) getDialog();
        if (alertDialog == null) {
            dismiss();
        } else {
            alertDialog.getButton(-1).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        onClick(getDialog(), -1);
    }

    @Override
    public void onConfirmLockdown(Bundle bundle, boolean z, boolean z2) {
        connect((VpnProfile) bundle.getParcelable("profile"), z);
        dismiss();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        boolean z;
        ConfigDialog configDialog = (ConfigDialog) getDialog();
        Parcelable profile = configDialog.getProfile();
        if (i == -1) {
            boolean zIsVpnAlwaysOn = configDialog.isVpnAlwaysOn();
            if (zIsVpnAlwaysOn || !configDialog.isEditing()) {
                z = true;
            } else {
                z = false;
            }
            boolean zIsAnyLockdownActive = VpnUtils.isAnyLockdownActive(this.mContext);
            try {
                boolean zIsVpnActive = VpnUtils.isVpnActive(this.mContext);
                if (z && !isConnected(profile) && ConfirmLockdownFragment.shouldShow(zIsVpnActive, zIsAnyLockdownActive, zIsVpnAlwaysOn)) {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("profile", profile);
                    ConfirmLockdownFragment.show(this, zIsVpnActive, zIsVpnAlwaysOn, zIsAnyLockdownActive, zIsVpnAlwaysOn, bundle);
                } else if (z) {
                    connect(profile, zIsVpnAlwaysOn);
                } else {
                    save(profile, false);
                }
            } catch (RemoteException e) {
                Log.w("ConfigDialogFragment", "Failed to check active VPN state. Skipping.", e);
            }
        } else if (i == -3) {
            if (!disconnect(profile)) {
                Log.e("ConfigDialogFragment", "Failed to disconnect VPN. Leaving profile in keystore.");
                return;
            }
            KeyStore.getInstance().delete("VPN_" + ((VpnProfile) profile).key, -1);
            updateLockdownVpn(false, profile);
        }
        dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        dismiss();
        super.onCancel(dialogInterface);
    }

    private void updateLockdownVpn(boolean z, VpnProfile vpnProfile) {
        if (z) {
            if (!vpnProfile.isValidLockdownProfile()) {
                Toast.makeText(this.mContext, R.string.vpn_lockdown_config_error, 1).show();
                return;
            } else {
                ConnectivityManager.from(this.mContext).setAlwaysOnVpnPackageForUser(UserHandle.myUserId(), null, false);
                VpnUtils.setLockdownVpn(this.mContext, vpnProfile.key);
                return;
            }
        }
        if (VpnUtils.isVpnLockdown(vpnProfile.key)) {
            VpnUtils.clearLockdownVpn(this.mContext);
        }
    }

    private void save(VpnProfile vpnProfile, boolean z) {
        KeyStore.getInstance().put("VPN_" + vpnProfile.key, vpnProfile.encode(), -1, 0);
        disconnect(vpnProfile);
        updateLockdownVpn(z, vpnProfile);
    }

    private void connect(VpnProfile vpnProfile, boolean z) {
        save(vpnProfile, z);
        if (!VpnUtils.isVpnLockdown(vpnProfile.key)) {
            VpnUtils.clearLockdownVpn(this.mContext);
            try {
                this.mService.startLegacyVpn(vpnProfile);
            } catch (RemoteException e) {
                Log.e("ConfigDialogFragment", "Failed to connect", e);
            } catch (IllegalStateException e2) {
                Toast.makeText(this.mContext, R.string.vpn_no_network, 1).show();
            }
        }
    }

    private boolean disconnect(VpnProfile vpnProfile) {
        try {
            if (!isConnected(vpnProfile)) {
                return true;
            }
            return VpnUtils.disconnectLegacyVpn(getContext());
        } catch (RemoteException e) {
            Log.e("ConfigDialogFragment", "Failed to disconnect", e);
            return false;
        }
    }

    private boolean isConnected(VpnProfile vpnProfile) throws RemoteException {
        LegacyVpnInfo legacyVpnInfo = this.mService.getLegacyVpnInfo(UserHandle.myUserId());
        return legacyVpnInfo != null && vpnProfile.key.equals(legacyVpnInfo.key);
    }
}
