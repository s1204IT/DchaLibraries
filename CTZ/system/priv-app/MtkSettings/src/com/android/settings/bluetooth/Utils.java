package com.android.settings.bluetooth;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.Utils;

/* loaded from: classes.dex */
public final class Utils {
    private static final Utils.ErrorListener mErrorListener = new Utils.ErrorListener() { // from class: com.android.settings.bluetooth.Utils.1
        @Override // com.android.settingslib.bluetooth.Utils.ErrorListener
        public void onShowError(Context context, String str, int i) {
            Utils.showError(context, str, i);
        }
    };
    private static final LocalBluetoothManager.BluetoothManagerCallback mOnInitCallback = new LocalBluetoothManager.BluetoothManagerCallback() { // from class: com.android.settings.bluetooth.Utils.2
        @Override // com.android.settingslib.bluetooth.LocalBluetoothManager.BluetoothManagerCallback
        public void onBluetoothManagerInitialized(Context context, LocalBluetoothManager localBluetoothManager) {
            com.android.settingslib.bluetooth.Utils.setErrorListener(Utils.mErrorListener);
        }
    };

    static AlertDialog showDisconnectDialog(Context context, AlertDialog alertDialog, DialogInterface.OnClickListener onClickListener, CharSequence charSequence, CharSequence charSequence2) {
        if (alertDialog == null) {
            alertDialog = new AlertDialog.Builder(context).setPositiveButton(R.string.ok, onClickListener).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
        } else {
            if (alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
            alertDialog.setButton(-1, context.getText(R.string.ok), onClickListener);
        }
        alertDialog.setTitle(charSequence);
        alertDialog.setMessage(charSequence2);
        alertDialog.show();
        return alertDialog;
    }

    static void showConnectingError(Context context, String str, LocalBluetoothManager localBluetoothManager) {
        FeatureFactory.getFactory(context).getMetricsFeatureProvider().visible(context, 0, 869);
        showError(context, str, com.android.settings.R.string.bluetooth_connecting_error_message, localBluetoothManager);
    }

    static void showError(Context context, String str, int i) {
        showError(context, str, i, getLocalBtManager(context));
    }

    private static void showError(Context context, String str, int i, LocalBluetoothManager localBluetoothManager) {
        String string = context.getString(i, str);
        Context foregroundActivity = localBluetoothManager.getForegroundActivity();
        if (localBluetoothManager.isForegroundActivity()) {
            Log.d("Bluetooth.Utils", "show ErrorDialogFragment, message is " + string);
            ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString("errorMessage", string);
            errorDialogFragment.setArguments(bundle);
            errorDialogFragment.show(((Activity) foregroundActivity).getFragmentManager(), "Error");
            return;
        }
        Toast.makeText(context, string, 0).show();
    }

    public static class ErrorDialogFragment extends DialogFragment {
        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setIcon(R.drawable.ic_dialog_alert).setTitle(com.android.settings.R.string.bluetooth_error_title).setMessage(getArguments().getString("errorMessage")).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
        }
    }

    public static LocalBluetoothManager getLocalBtManager(Context context) {
        return LocalBluetoothManager.getInstance(context, mOnInitCallback);
    }

    public static String createRemoteName(Context context, BluetoothDevice bluetoothDevice) {
        String aliasName = bluetoothDevice != null ? bluetoothDevice.getAliasName() : null;
        if (aliasName == null) {
            return context.getString(com.android.settings.R.string.unknown);
        }
        return aliasName;
    }

    public static boolean isBluetoothScanningEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "ble_scan_always_enabled", 0) == 1;
    }
}
