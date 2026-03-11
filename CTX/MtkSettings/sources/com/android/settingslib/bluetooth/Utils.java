package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import com.android.settingslib.R;
import com.android.settingslib.graph.BluetoothDeviceLayerDrawable;
import java.util.Iterator;

public class Utils {
    private static ErrorListener sErrorListener;

    public interface ErrorListener {
        void onShowError(Context context, String str, int i);
    }

    public static int getConnectionStateSummary(int i) {
        switch (i) {
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

    static void showError(Context context, String str, int i) {
        if (sErrorListener != null) {
            sErrorListener.onShowError(context, str, i);
        }
    }

    public static void setErrorListener(ErrorListener errorListener) {
        sErrorListener = errorListener;
    }

    public static Pair<Drawable, String> getBtClassDrawableWithDescription(Context context, CachedBluetoothDevice cachedBluetoothDevice) {
        return getBtClassDrawableWithDescription(context, cachedBluetoothDevice, 1.0f);
    }

    public static Pair<Drawable, String> getBtClassDrawableWithDescription(Context context, CachedBluetoothDevice cachedBluetoothDevice, float f) {
        BluetoothClass btClass = cachedBluetoothDevice.getBtClass();
        int batteryLevel = cachedBluetoothDevice.getBatteryLevel();
        if (btClass != null) {
            int majorDeviceClass = btClass.getMajorDeviceClass();
            if (majorDeviceClass == 256) {
                return new Pair<>(getBluetoothDrawable(context, R.drawable.ic_bt_laptop, batteryLevel, f), context.getString(R.string.bluetooth_talkback_computer));
            }
            if (majorDeviceClass == 512) {
                return new Pair<>(getBluetoothDrawable(context, R.drawable.ic_bt_cellphone, batteryLevel, f), context.getString(R.string.bluetooth_talkback_phone));
            }
            if (majorDeviceClass == 1280) {
                return new Pair<>(getBluetoothDrawable(context, HidProfile.getHidClassDrawable(btClass), batteryLevel, f), context.getString(R.string.bluetooth_talkback_input_peripheral));
            }
            if (majorDeviceClass == 1536) {
                return new Pair<>(getBluetoothDrawable(context, R.drawable.ic_settings_print, batteryLevel, f), context.getString(R.string.bluetooth_talkback_imaging));
            }
        }
        Iterator<LocalBluetoothProfile> it = cachedBluetoothDevice.getProfiles().iterator();
        while (it.hasNext()) {
            int drawableResource = it.next().getDrawableResource(btClass);
            if (drawableResource != 0) {
                return new Pair<>(getBluetoothDrawable(context, drawableResource, batteryLevel, f), null);
            }
        }
        if (btClass != null) {
            if (btClass.doesClassMatch(0)) {
                return new Pair<>(getBluetoothDrawable(context, R.drawable.ic_bt_headset_hfp, batteryLevel, f), context.getString(R.string.bluetooth_talkback_headset));
            }
            if (btClass.doesClassMatch(1)) {
                return new Pair<>(getBluetoothDrawable(context, R.drawable.ic_bt_headphones_a2dp, batteryLevel, f), context.getString(R.string.bluetooth_talkback_headphone));
            }
        }
        return new Pair<>(getBluetoothDrawable(context, R.drawable.ic_settings_bluetooth, batteryLevel, f), context.getString(R.string.bluetooth_talkback_bluetooth));
    }

    public static Drawable getBluetoothDrawable(Context context, int i, int i2, float f) {
        if (i2 != -1) {
            return BluetoothDeviceLayerDrawable.createLayerDrawable(context, i, i2, f);
        }
        return context.getDrawable(i);
    }
}
