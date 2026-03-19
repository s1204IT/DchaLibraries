package android.nfc.cardemulation;

import android.app.Activity;
import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.nfc.INfcFCardEmulation;
import android.nfc.NfcAdapter;
import android.os.RemoteException;
import android.util.Log;
import java.util.HashMap;
import java.util.List;

public final class NfcFCardEmulation {
    static final String TAG = "NfcFCardEmulation";
    static INfcFCardEmulation sService;
    final Context mContext;
    static boolean sIsInitialized = false;
    static HashMap<Context, NfcFCardEmulation> sCardEmus = new HashMap<>();

    private NfcFCardEmulation(Context context, INfcFCardEmulation iNfcFCardEmulation) {
        this.mContext = context.getApplicationContext();
        sService = iNfcFCardEmulation;
    }

    public static synchronized NfcFCardEmulation getInstance(NfcAdapter nfcAdapter) {
        NfcFCardEmulation nfcFCardEmulation;
        if (nfcAdapter == null) {
            throw new NullPointerException("NfcAdapter is null");
        }
        Context context = nfcAdapter.getContext();
        if (context == null) {
            Log.e(TAG, "NfcAdapter context is null.");
            throw new UnsupportedOperationException();
        }
        if (!sIsInitialized) {
            IPackageManager packageManager = ActivityThread.getPackageManager();
            if (packageManager == null) {
                Log.e(TAG, "Cannot get PackageManager");
                throw new UnsupportedOperationException();
            }
            try {
                if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF, 0)) {
                    Log.e(TAG, "This device does not support NFC-F card emulation");
                    throw new UnsupportedOperationException();
                }
                sIsInitialized = true;
            } catch (RemoteException e) {
                Log.e(TAG, "PackageManager query failed.");
                throw new UnsupportedOperationException();
            }
        }
        nfcFCardEmulation = sCardEmus.get(context);
        if (nfcFCardEmulation == null) {
            INfcFCardEmulation nfcFCardEmulationService = nfcAdapter.getNfcFCardEmulationService();
            if (nfcFCardEmulationService == null) {
                Log.e(TAG, "This device does not implement the INfcFCardEmulation interface.");
                throw new UnsupportedOperationException();
            }
            nfcFCardEmulation = new NfcFCardEmulation(context, nfcFCardEmulationService);
            sCardEmus.put(context, nfcFCardEmulation);
        }
        return nfcFCardEmulation;
    }

    public String getSystemCodeForService(ComponentName componentName) throws RuntimeException {
        if (componentName == null) {
            throw new NullPointerException("service is null");
        }
        try {
            return sService.getSystemCodeForService(this.mContext.getUserId(), componentName);
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return null;
            }
            try {
                return sService.getSystemCodeForService(this.mContext.getUserId(), componentName);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                e2.rethrowAsRuntimeException();
                return null;
            }
        }
    }

    public boolean registerSystemCodeForService(ComponentName componentName, String str) throws RuntimeException {
        if (componentName == null || str == null) {
            throw new NullPointerException("service or systemCode is null");
        }
        try {
            return sService.registerSystemCodeForService(this.mContext.getUserId(), componentName, str);
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return false;
            }
            try {
                return sService.registerSystemCodeForService(this.mContext.getUserId(), componentName, str);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                e2.rethrowAsRuntimeException();
                return false;
            }
        }
    }

    public boolean unregisterSystemCodeForService(ComponentName componentName) throws RuntimeException {
        if (componentName == null) {
            throw new NullPointerException("service is null");
        }
        try {
            return sService.removeSystemCodeForService(this.mContext.getUserId(), componentName);
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return false;
            }
            try {
                return sService.removeSystemCodeForService(this.mContext.getUserId(), componentName);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                e2.rethrowAsRuntimeException();
                return false;
            }
        }
    }

    public String getNfcid2ForService(ComponentName componentName) throws RuntimeException {
        if (componentName == null) {
            throw new NullPointerException("service is null");
        }
        try {
            return sService.getNfcid2ForService(this.mContext.getUserId(), componentName);
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return null;
            }
            try {
                return sService.getNfcid2ForService(this.mContext.getUserId(), componentName);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                e2.rethrowAsRuntimeException();
                return null;
            }
        }
    }

    public boolean setNfcid2ForService(ComponentName componentName, String str) throws RuntimeException {
        if (componentName == null || str == null) {
            throw new NullPointerException("service or nfcid2 is null");
        }
        try {
            return sService.setNfcid2ForService(this.mContext.getUserId(), componentName, str);
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return false;
            }
            try {
                return sService.setNfcid2ForService(this.mContext.getUserId(), componentName, str);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                e2.rethrowAsRuntimeException();
                return false;
            }
        }
    }

    public boolean enableService(Activity activity, ComponentName componentName) throws RuntimeException {
        if (activity == null || componentName == null) {
            throw new NullPointerException("activity or service is null");
        }
        if (!activity.isResumed()) {
            throw new IllegalArgumentException("Activity must be resumed.");
        }
        try {
            return sService.enableNfcFForegroundService(componentName);
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return false;
            }
            try {
                return sService.enableNfcFForegroundService(componentName);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                e2.rethrowAsRuntimeException();
                return false;
            }
        }
    }

    public boolean disableService(Activity activity) throws RuntimeException {
        if (activity == null) {
            throw new NullPointerException("activity is null");
        }
        if (!activity.isResumed()) {
            throw new IllegalArgumentException("Activity must be resumed.");
        }
        try {
            return sService.disableNfcFForegroundService();
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return false;
            }
            try {
                return sService.disableNfcFForegroundService();
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                e2.rethrowAsRuntimeException();
                return false;
            }
        }
    }

    public List<NfcFServiceInfo> getNfcFServices() {
        try {
            return sService.getNfcFServices(this.mContext.getUserId());
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return null;
            }
            try {
                return sService.getNfcFServices(this.mContext.getUserId());
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                return null;
            }
        }
    }

    public int getMaxNumOfRegisterableSystemCodes() {
        try {
            return sService.getMaxNumOfRegisterableSystemCodes();
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return -1;
            }
            try {
                return sService.getMaxNumOfRegisterableSystemCodes();
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                return -1;
            }
        }
    }

    public static boolean isValidSystemCode(String str) {
        if (str == null) {
            return false;
        }
        if (str.length() != 4) {
            Log.e(TAG, "System Code " + str + " is not a valid System Code.");
            return false;
        }
        if (!str.startsWith("4") || str.toUpperCase().endsWith("FF")) {
            Log.e(TAG, "System Code " + str + " is not a valid System Code.");
            return false;
        }
        try {
            Integer.parseInt(str, 16);
            return true;
        } catch (NumberFormatException e) {
            Log.e(TAG, "System Code " + str + " is not a valid System Code.");
            return false;
        }
    }

    public static boolean isValidNfcid2(String str) {
        if (str == null) {
            return false;
        }
        if (str.length() != 16) {
            Log.e(TAG, "NFCID2 " + str + " is not a valid NFCID2.");
            return false;
        }
        if (!str.toUpperCase().startsWith("02FE")) {
            Log.e(TAG, "NFCID2 " + str + " is not a valid NFCID2.");
            return false;
        }
        try {
            Long.parseLong(str, 16);
            return true;
        } catch (NumberFormatException e) {
            Log.e(TAG, "NFCID2 " + str + " is not a valid NFCID2.");
            return false;
        }
    }

    void recoverService() {
        sService = NfcAdapter.getDefaultAdapter(this.mContext).getNfcFCardEmulationService();
    }
}
