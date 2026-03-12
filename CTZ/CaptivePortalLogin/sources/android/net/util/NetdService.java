package android.net.util;

import android.net.INetd;
import android.os.ServiceManager;
import android.util.Log;

public class NetdService {
    private static final String TAG = NetdService.class.getSimpleName();

    public static INetd getInstance() {
        INetd iNetdAsInterface = INetd.Stub.asInterface(ServiceManager.getService("netd"));
        if (iNetdAsInterface == null) {
            Log.w(TAG, "WARNING: returning null INetd instance.");
        }
        return iNetdAsInterface;
    }
}
