package jp.co.benesse.dcha.setupwizard;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public class DchaNetworkUtil {
    private static final String TAG = DchaNetworkUtil.class.getSimpleName();

    private DchaNetworkUtil() {
    }

    public static final boolean isConnective(Context context) {
        Logger.d(TAG, "isConnective 0001");
        if (context == null) {
            Logger.d(TAG, "isConnective 0002");
            return false;
        }
        NetworkInfo networkInfo = ((ConnectivityManager) context.getApplicationContext().getSystemService("connectivity")).getNetworkInfo(1);
        if (networkInfo == null) {
            Logger.d(TAG, "isConnective 0003");
            return false;
        }
        boolean zIsConnected = networkInfo.isConnected();
        Logger.d(TAG, "isConnective 0004");
        return zIsConnected;
    }
}
