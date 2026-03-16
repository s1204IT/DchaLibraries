package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.search.Index;
import com.android.settings.search.SearchIndexableRaw;

final class Utils {
    private Utils() {
    }

    public static int getConnectionStateSummary(int connectionState) {
        switch (connectionState) {
            case 0:
                return R.string.bluetooth_disconnected;
            case 1:
                return R.string.bluetooth_connecting;
            case 2:
                return R.string.bluetooth_connected;
            case 3:
                return R.string.bluetooth_disconnecting;
            default:
                return 0;
        }
    }

    static AlertDialog showDisconnectDialog(Context context, AlertDialog dialog, DialogInterface.OnClickListener disconnectListener, CharSequence title, CharSequence message) {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(context).setPositiveButton(android.R.string.ok, disconnectListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        } else {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            CharSequence okText = context.getText(android.R.string.ok);
            dialog.setButton(-1, okText, disconnectListener);
        }
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    static void showError(Context context, String name, int messageResId) {
        String message = context.getString(messageResId, name);
        LocalBluetoothManager manager = LocalBluetoothManager.getInstance(context);
        Context activity = manager.getForegroundActivity();
        if (manager.isForegroundActivity()) {
            new AlertDialog.Builder(activity).setTitle(R.string.bluetooth_error_title).setMessage(message).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
        } else {
            Toast.makeText(context, message, 0).show();
        }
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
}
