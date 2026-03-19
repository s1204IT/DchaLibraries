package com.android.settings.vpn2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.vpn2.AppDialog;

public class AppDialogFragment extends InstrumentedDialogFragment implements AppDialog.Listener {
    private Listener mListener;
    private PackageInfo mPackageInfo;
    private final IConnectivityManager mService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
    private UserManager mUserManager;

    public interface Listener {
        void onCancel();

        void onForget();
    }

    @Override
    public int getMetricsCategory() {
        return 546;
    }

    public static void show(Fragment fragment, PackageInfo packageInfo, String str, boolean z, boolean z2) {
        if (!z && !z2) {
            return;
        }
        show(fragment, null, packageInfo, str, z, z2);
    }

    public static void show(Fragment fragment, Listener listener, PackageInfo packageInfo, String str, boolean z, boolean z2) {
        if (!fragment.isAdded()) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable("package", packageInfo);
        bundle.putString("label", str);
        bundle.putBoolean("managing", z);
        bundle.putBoolean("connected", z2);
        AppDialogFragment appDialogFragment = new AppDialogFragment();
        appDialogFragment.mListener = listener;
        appDialogFragment.setArguments(bundle);
        appDialogFragment.setTargetFragment(fragment, 0);
        appDialogFragment.show(fragment.getFragmentManager(), "vpnappdialog");
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mUserManager = UserManager.get(getContext());
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle arguments = getArguments();
        String string = arguments.getString("label");
        boolean z = arguments.getBoolean("managing");
        boolean z2 = arguments.getBoolean("connected");
        this.mPackageInfo = (PackageInfo) arguments.getParcelable("package");
        if (z) {
            return new AppDialog(getActivity(), this, this.mPackageInfo, string);
        }
        AlertDialog.Builder negativeButton = new AlertDialog.Builder(getActivity()).setTitle(string).setMessage(getActivity().getString(R.string.vpn_disconnect_confirm)).setNegativeButton(getActivity().getString(R.string.vpn_cancel), (DialogInterface.OnClickListener) null);
        if (z2 && !isUiRestricted()) {
            negativeButton.setPositiveButton(getActivity().getString(R.string.vpn_disconnect), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    AppDialogFragment.this.onDisconnect(dialogInterface);
                }
            });
        }
        return negativeButton.create();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        dismiss();
        if (this.mListener != null) {
            this.mListener.onCancel();
        }
        super.onCancel(dialogInterface);
    }

    @Override
    public void onForget(DialogInterface dialogInterface) {
        if (isUiRestricted()) {
            return;
        }
        int userId = getUserId();
        try {
            this.mService.setVpnPackageAuthorization(this.mPackageInfo.packageName, userId, false);
            onDisconnect(dialogInterface);
        } catch (RemoteException e) {
            Log.e("AppDialogFragment", "Failed to forget authorization of " + this.mPackageInfo.packageName + " for user " + userId, e);
        }
        if (this.mListener != null) {
            this.mListener.onForget();
        }
    }

    private void onDisconnect(DialogInterface dialogInterface) {
        if (isUiRestricted()) {
            return;
        }
        int userId = getUserId();
        try {
            if (this.mPackageInfo.packageName.equals(VpnUtils.getConnectedPackage(this.mService, userId))) {
                this.mService.setAlwaysOnVpnPackage(userId, (String) null, false);
                this.mService.prepareVpn(this.mPackageInfo.packageName, "[Legacy VPN]", userId);
            }
        } catch (RemoteException e) {
            Log.e("AppDialogFragment", "Failed to disconnect package " + this.mPackageInfo.packageName + " for user " + userId, e);
        }
    }

    private boolean isUiRestricted() {
        return this.mUserManager.hasUserRestriction("no_config_vpn", UserHandle.of(getUserId()));
    }

    private int getUserId() {
        return UserHandle.getUserId(this.mPackageInfo.applicationInfo.uid);
    }
}
