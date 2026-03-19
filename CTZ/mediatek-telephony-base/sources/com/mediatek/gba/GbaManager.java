package com.mediatek.gba;

import android.content.Context;
import android.net.Network;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.mediatek.gba.IGbaService;
import com.mediatek.ims.internal.IMtkImsService;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;

public final class GbaManager {
    public static final String IMS_GBA_KS_EXT_NAF = "Ks_ext_NAF";
    public static final String IMS_GBA_KS_NAF = "Ks_NAF";
    public static final int IMS_GBA_ME = 1;
    public static final int IMS_GBA_NONE = 0;
    public static final int IMS_GBA_U = 2;
    public static final String IMS_SERVICE = "ims";
    public static final String MTK_IMS_SERVICE = "mtkIms";
    private static final String TAG = "GbaManager";
    private static int mNetId;
    private static IGbaService mService;
    private final Context mContext;
    private static GbaManager mGbaManager = null;
    public static final byte[] DEFAULT_UA_SECURITY_PROTOCOL_ID0 = {1, 0, 0, 0, 0};
    public static final byte[] DEFAULT_UA_SECURITY_PROTOCOL_ID1 = {1, 0, 0, 0, 1};
    public static final byte[] DEFAULT_UA_SECURITY_PROTOCOL_ID2 = {1, 0, 0, 0, 2};
    public static final byte[] DEFAULT_UA_SECURITY_PROTOCOL_ID3 = {1, 0, 0, 0, 3};
    private static final byte[] DEFAULT_UA_SECURITY_PROTOCOL_ID_HTTP = {1, 0, 0, 0, 2};
    private static final byte[] DEFAULT_UA_SECURITY_PROTOCOL_ID_TLS = {1, 0, 1, 0, 47};

    public static GbaManager getDefaultGbaManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        synchronized (GbaManager.class) {
            if (mGbaManager == null) {
                if (!supportMdAutoSetupIms()) {
                    IBinder service = ServiceManager.getService("GbaService");
                    if (service == null) {
                        Log.i("debug", "The binder is null");
                        return null;
                    }
                    mService = IGbaService.Stub.asInterface(service);
                }
                mGbaManager = new GbaManager(context);
            }
            return mGbaManager;
        }
    }

    GbaManager(Context context) {
        this.mContext = context;
    }

    public int getGbaSupported() {
        try {
            return mService.getGbaSupported();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int getGbaSupported(int i) {
        try {
            return mService.getGbaSupported();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public boolean isGbaKeyExpired(String str, byte[] bArr) {
        try {
            return mService.isGbaKeyExpired(str, bArr);
        } catch (RemoteException e) {
            return true;
        }
    }

    public boolean isGbaKeyExpired(String str, byte[] bArr, int i) {
        try {
            return mService.isGbaKeyExpiredForSubscriber(str, bArr, i);
        } catch (RemoteException e) {
            return true;
        }
    }

    public NafSessionKey runGbaAuthentication(String str, byte[] bArr, boolean z) {
        try {
            if (supportMdAutoSetupIms()) {
                return runNativeGba(str, bArr, z, mNetId, SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultSubscriptionId()));
            }
            return mService.runGbaAuthentication(str, bArr, z);
        } catch (RemoteException e) {
            return null;
        }
    }

    public NafSessionKey runGbaAuthentication(String str, byte[] bArr, boolean z, int i) {
        try {
            if (supportMdAutoSetupIms()) {
                return runNativeGba(str, bArr, z, mNetId, SubscriptionManager.getPhoneId(i));
            }
            return mService.runGbaAuthenticationForSubscriber(str, bArr, z, i);
        } catch (RemoteException e) {
            return null;
        }
    }

    private NafSessionKey runNativeGba(String str, byte[] bArr, boolean z, int i, int i2) {
        IBinder service = ServiceManager.getService("mtkIms");
        if (service == null) {
            Log.e(TAG, "Service is unavailable binder is null");
            return null;
        }
        IMtkImsService iMtkImsServiceAsInterface = IMtkImsService.Stub.asInterface(service);
        if (iMtkImsServiceAsInterface == null) {
            Log.e(TAG, "Service is unavailable mImsService is null");
            return null;
        }
        try {
            return iMtkImsServiceAsInterface.runGbaAuthentication(str, bArr, z, i, i2);
        } catch (RemoteException e) {
            Log.e(TAG, "RemotaException mImsService.runGbaAuthentication()");
            return null;
        }
    }

    public byte[] getNafSecureProtocolId(boolean z) {
        GbaCipherSuite byName;
        byte[] bArr = DEFAULT_UA_SECURITY_PROTOCOL_ID_TLS;
        if (z) {
            String property = System.getProperty("gba.ciper.suite", "");
            if (property.length() > 0 && (byName = GbaCipherSuite.getByName(property)) != null) {
                byte[] code = byName.getCode();
                bArr[3] = code[0];
                bArr[4] = code[1];
                return bArr;
            }
            return bArr;
        }
        return DEFAULT_UA_SECURITY_PROTOCOL_ID_HTTP;
    }

    public void setNetwork(Network network) {
        try {
            mService.setNetwork(network);
            mNetId = network.netId;
        } catch (RemoteException e) {
            Log.e(TAG, "remote expcetion for setNetwork");
        }
    }

    public NafSessionKey getCachedKey(String str, byte[] bArr, int i) {
        try {
            return mService.getCachedKey(str, bArr, i);
        } catch (RemoteException e) {
            Log.e(TAG, "remote expcetion for getCachedKey");
            return null;
        }
    }

    public void updateCachedKey(String str, byte[] bArr, int i, NafSessionKey nafSessionKey) {
        try {
            mService.updateCachedKey(str, bArr, i, nafSessionKey);
        } catch (RemoteException e) {
            Log.e(TAG, "remote expcetion for updateCachedKey");
        }
    }

    private static boolean supportMdAutoSetupIms() {
        if (SystemProperties.get("ro.vendor.md_auto_setup_ims").equals(MtkPhoneNumberUtils.EccEntry.ECC_ALWAYS)) {
            return true;
        }
        return false;
    }
}
