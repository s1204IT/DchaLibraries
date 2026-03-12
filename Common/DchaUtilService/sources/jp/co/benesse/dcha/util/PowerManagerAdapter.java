package jp.co.benesse.dcha.util;

import android.content.Context;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;

public class PowerManagerAdapter {
    public static final String TAG = PowerManagerAdapter.class.getSimpleName();

    private PowerManagerAdapter() {
    }

    public static final void disableBatterySaver(Context context) throws RemoteException {
        Logger.d(TAG, "disableBatterySaver 0001");
        try {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            PowerManager powerManager = (PowerManager) context.getSystemService("power");
            ReflectionUtils.invokeDeclaredMethod(loader.loadClass("android.os.PowerManager"), powerManager, "setPowerSaveMode", new Class[]{Boolean.TYPE}, new Object[]{false});
            Settings.Global.putInt(context.getContentResolver(), "low_power_trigger_level", 0);
            Logger.d(TAG, "disableBatterySaver 0003");
        } catch (Exception e) {
            Logger.d(TAG, "sdUnmount 0002", e);
            throw new RemoteException();
        }
    }
}
