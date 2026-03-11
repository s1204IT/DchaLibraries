package com.android.settingslib.bluetooth;

import android.content.Context;
import com.android.settingslib.R$string;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class Utils {
    private static ErrorListener sErrorListener;

    public interface ErrorListener {
        void onShowError(Context context, String str, int i);
    }

    public static int getConnectionStateSummary(int connectionState) {
        switch (connectionState) {
            case DefaultWfcSettingsExt.RESUME:
                return R$string.bluetooth_disconnected;
            case DefaultWfcSettingsExt.PAUSE:
                return R$string.bluetooth_connecting;
            case DefaultWfcSettingsExt.CREATE:
                return R$string.bluetooth_connected;
            case DefaultWfcSettingsExt.DESTROY:
                return R$string.bluetooth_disconnecting;
            default:
                return 0;
        }
    }

    static void showError(Context context, String name, int messageResId) {
        if (sErrorListener == null) {
            return;
        }
        sErrorListener.onShowError(context, name, messageResId);
    }

    public static void setErrorListener(ErrorListener listener) {
        sErrorListener = listener;
    }
}
