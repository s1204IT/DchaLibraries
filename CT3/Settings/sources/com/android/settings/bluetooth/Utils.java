package com.android.settings.bluetooth;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.bluetooth.DockService;
import com.android.settings.search.Index;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.Utils;

public final class Utils {
    private static final Utils.ErrorListener mErrorListener = new Utils.ErrorListener() {
        @Override
        public void onShowError(Context context, String name, int messageResId) {
            Utils.showError(context, name, messageResId);
        }
    };
    private static final LocalBluetoothManager.BluetoothManagerCallback mOnInitCallback = new LocalBluetoothManager.BluetoothManagerCallback() {
        @Override
        public void onBluetoothManagerInitialized(Context appContext, LocalBluetoothManager bluetoothManager) {
            bluetoothManager.getEventManager().registerCallback(new DockService.DockBluetoothCallback(appContext));
            com.android.settingslib.bluetooth.Utils.setErrorListener(Utils.mErrorListener);
        }
    };

    private Utils() {
    }

    static AlertDialog showDisconnectDialog(Context context, AlertDialog dialog, DialogInterface.OnClickListener disconnectListener, CharSequence title, CharSequence message) {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(context).setPositiveButton(R.string.ok, disconnectListener).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
        } else {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            CharSequence okText = context.getText(R.string.ok);
            dialog.setButton(-1, okText, disconnectListener);
        }
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    static void showError(Context context, String name, int messageResId) {
        String message = context.getString(messageResId, name);
        LocalBluetoothManager manager = getLocalBtManager(context);
        if (manager == null) {
            return;
        }
        Activity activity = (Activity) manager.getForegroundActivity();
        if (manager.isForegroundActivity()) {
            Log.d("Bluetooth.Utils", "show ErrorDialogFragment, message is " + message);
            ErrorDialogFragment dialog = new ErrorDialogFragment();
            Bundle args = new Bundle();
            args.putString("errorMessage", message);
            dialog.setArguments(args);
            dialog.show(activity.getFragmentManager(), "Error");
            return;
        }
        Toast.makeText(context, message, 0).show();
    }

    public static void updateSearchIndex(Context context, String className, String title, String screenTitle, int iconResId, boolean enabled) {
        SearchIndexableRaw data = new SearchIndexableRaw(context);
        data.className = className;
        data.title = title;
        data.screenTitle = screenTitle;
        data.iconResId = iconResId;
        data.enabled = enabled;
        Index.getInstance(context).updateFromSearchIndexableData(data);
    }

    public static class ErrorDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String message = getArguments().getString("errorMessage");
            return new AlertDialog.Builder(getActivity()).setIcon(R.drawable.ic_dialog_alert).setTitle(com.android.settings.R.string.bluetooth_error_title).setMessage(message).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
        }
    }

    public static LocalBluetoothManager getLocalBtManager(Context context) {
        return LocalBluetoothManager.getInstance(context, mOnInitCallback);
    }
}
