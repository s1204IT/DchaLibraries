package com.android.settings.vpn2;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.security.Credentials;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnProfile;
import com.android.settings.R;

public class ConfigDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private final IConnectivityManager mService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
    private boolean mUnlocking = false;

    public static void show(VpnSettings parent, VpnProfile profile, boolean edit, boolean exists) {
        if (parent.isAdded()) {
            Bundle args = new Bundle();
            args.putParcelable("profile", profile);
            args.putBoolean("editing", edit);
            args.putBoolean("exists", exists);
            ConfigDialogFragment frag = new ConfigDialogFragment();
            frag.setArguments(args);
            frag.setTargetFragment(parent, 0);
            frag.show(parent.getFragmentManager(), "vpnconfigdialog");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!KeyStore.getInstance().isUnlocked()) {
            if (!this.mUnlocking) {
                Credentials.getInstance().unlock(getActivity());
            } else {
                dismiss();
            }
            this.mUnlocking = this.mUnlocking ? false : true;
            return;
        }
        this.mUnlocking = false;
    }

    private static String getStringOrNull(KeyStore keyStore, String key) {
        byte[] value;
        if (keyStore.isUnlocked() && (value = keyStore.get(key)) != null) {
            return new String(value);
        }
        return null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        VpnProfile profile = args.getParcelable("profile");
        boolean editing = args.getBoolean("editing");
        boolean exists = args.getBoolean("exists");
        return new ConfigDialog(getActivity(), this, profile, editing, exists);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        ConfigDialog dialog = (ConfigDialog) getDialog();
        VpnProfile profile = dialog.getProfile();
        if (button == -1) {
            KeyStore.getInstance().put("VPN_" + profile.key, profile.encode(), -1, 0);
            KeyStore keyStore = KeyStore.getInstance();
            String lockdownKey = getStringOrNull(keyStore, "LOCKDOWN_VPN");
            if (lockdownKey == null) {
                disconnect(profile);
            }
            updateLockdownVpn(dialog.isVpnAlwaysOn(), profile);
            if (!dialog.isEditing() && !VpnUtils.isVpnLockdown(profile.key)) {
                try {
                    if (lockdownKey == null) {
                        connect(profile);
                    } else {
                        Toast.makeText(getActivity(), R.string.lockdown_vpn_already_connected, 1).show();
                    }
                } catch (RemoteException e) {
                    Log.e("ConfigDialogFragment", "Failed to connect", e);
                }
            } else {
                String selectedKey = profile.key;
                if (TextUtils.equals(selectedKey, lockdownKey) && !profile.isValidLockdownProfile()) {
                    keyStore.delete("LOCKDOWN_VPN");
                    ConnectivityManager.from(getActivity()).updateLockdownVpn();
                    Toast.makeText(getActivity(), R.string.vpn_lockdown_config_error, 1).show();
                }
            }
        } else if (button == -3) {
            disconnect(profile);
            KeyStore.getInstance().delete("VPN_" + profile.key, -1);
            updateLockdownVpn(false, profile);
        }
        dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dismiss();
        super.onCancel(dialog);
    }

    private void updateLockdownVpn(boolean isVpnAlwaysOn, VpnProfile profile) {
        if (isVpnAlwaysOn) {
            if (!profile.isValidLockdownProfile()) {
                Toast.makeText(getContext(), R.string.vpn_lockdown_config_error, 1).show();
                return;
            }
            ConnectivityManager conn = ConnectivityManager.from(getActivity());
            conn.setAlwaysOnVpnPackageForUser(UserHandle.myUserId(), null, false);
            VpnUtils.setLockdownVpn(getContext(), profile.key);
            return;
        }
        if (!VpnUtils.isVpnLockdown(profile.key)) {
            return;
        }
        VpnUtils.clearLockdownVpn(getContext());
    }

    private void connect(VpnProfile profile) throws RemoteException {
        try {
            this.mService.startLegacyVpn(profile);
        } catch (IllegalStateException e) {
            Toast.makeText(getActivity(), R.string.vpn_no_network, 1).show();
        }
    }

    private void disconnect(VpnProfile profile) {
        try {
            LegacyVpnInfo connected = this.mService.getLegacyVpnInfo(UserHandle.myUserId());
            if (connected == null || !profile.key.equals(connected.key)) {
                return;
            }
            VpnUtils.clearLockdownVpn(getContext());
            this.mService.prepareVpn("[Legacy VPN]", "[Legacy VPN]", UserHandle.myUserId());
        } catch (RemoteException e) {
            Log.e("ConfigDialogFragment", "Failed to disconnect", e);
        }
    }
}
