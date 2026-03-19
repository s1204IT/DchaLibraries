package android.nfc.dta;

import android.content.Context;
import android.nfc.INfcDta;
import android.nfc.NfcAdapter;
import android.os.RemoteException;
import android.util.Log;
import java.util.HashMap;

public final class NfcDta {
    private static final String TAG = "NfcDta";
    private static HashMap<Context, NfcDta> sNfcDtas = new HashMap<>();
    private static INfcDta sService;
    private final Context mContext;

    private NfcDta(Context context, INfcDta iNfcDta) {
        this.mContext = context.getApplicationContext();
        sService = iNfcDta;
    }

    public static synchronized NfcDta getInstance(NfcAdapter nfcAdapter) {
        NfcDta nfcDta;
        if (nfcAdapter == null) {
            throw new NullPointerException("NfcAdapter is null");
        }
        Context context = nfcAdapter.getContext();
        if (context == null) {
            Log.e(TAG, "NfcAdapter context is null.");
            throw new UnsupportedOperationException();
        }
        nfcDta = sNfcDtas.get(context);
        if (nfcDta == null) {
            INfcDta nfcDtaInterface = nfcAdapter.getNfcDtaInterface();
            if (nfcDtaInterface == null) {
                Log.e(TAG, "This device does not implement the INfcDta interface.");
                throw new UnsupportedOperationException();
            }
            nfcDta = new NfcDta(context, nfcDtaInterface);
            sNfcDtas.put(context, nfcDta);
        }
        return nfcDta;
    }

    public boolean enableDta() {
        try {
            sService.enableDta();
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean disableDta() {
        try {
            sService.disableDta();
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean enableServer(String str, int i, int i2, int i3, int i4) {
        try {
            return sService.enableServer(str, i, i2, i3, i4);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean disableServer() {
        try {
            sService.disableServer();
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean enableClient(String str, int i, int i2, int i3) {
        try {
            return sService.enableClient(str, i, i2, i3);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean disableClient() {
        try {
            sService.disableClient();
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean registerMessageService(String str) {
        try {
            return sService.registerMessageService(str);
        } catch (RemoteException e) {
            return false;
        }
    }
}
