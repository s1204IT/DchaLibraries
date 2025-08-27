package com.mediatek.nfcsettingsadapter;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.mediatek.nfcsettingsadapter.INfcSettingsAdapter;
import java.util.HashMap;
import java.util.List;

/* loaded from: classes.dex */
public class NfcSettingsAdapter {
    static HashMap<Context, NfcSettingsAdapter> sNfcSettingsAdapters = new HashMap<>();
    static INfcSettingsAdapter sService;
    final Context mContext;

    NfcSettingsAdapter(Context context) {
        this.mContext = context;
        sService = getServiceInterface();
    }

    public static NfcSettingsAdapter getDefaultAdapter(Context context) {
        if (NfcAdapter.getDefaultAdapter(context) == null) {
            Log.d("NfcSettingsAdapter", "getDefaultAdapter = null");
            return null;
        }
        NfcSettingsAdapter nfcSettingsAdapter = sNfcSettingsAdapters.get(context);
        if (nfcSettingsAdapter == null) {
            nfcSettingsAdapter = new NfcSettingsAdapter(context);
            sNfcSettingsAdapters.put(context, nfcSettingsAdapter);
        }
        if (sService == null) {
            sService = getServiceInterface();
            Log.d("NfcSettingsAdapter", "sService = " + sService);
        }
        Log.d("NfcSettingsAdapter", "adapter = " + nfcSettingsAdapter);
        return nfcSettingsAdapter;
    }

    private static INfcSettingsAdapter getServiceInterface() {
        IBinder service = ServiceManager.getService("nfc_settings");
        Log.d("NfcSettingsAdapter", "b = " + service);
        if (service == null) {
            return null;
        }
        return INfcSettingsAdapter.Stub.asInterface(service);
    }

    public int getModeFlag(int i) {
        try {
            if (sService == null) {
                Log.e("NfcSettingsAdapter", "getModeFlag sService = null");
                return -1;
            }
            Log.d("NfcSettingsAdapter", "sService.getModeFlag(mode)");
            return sService.getModeFlag(i);
        } catch (RemoteException e) {
            Log.e("NfcSettingsAdapter", "getModeFlag e = " + e.toString());
            return -1;
        }
    }

    public void setModeFlag(int i, int i2) {
        try {
            if (sService == null) {
                Log.e("NfcSettingsAdapter", "setModeFlag sService = null");
            } else {
                Log.d("NfcSettingsAdapter", "sService.setModeFlag(mode)");
                sService.setModeFlag(i, i2);
            }
        } catch (RemoteException e) {
            Log.e("NfcSettingsAdapter", "setModeFlag e = " + e.toString());
        }
    }

    public boolean isShowOverflowMenu() {
        try {
            if (!SystemProperties.get("persist.vendor.st_nfc_gsma_support").equals("1")) {
                Log.v("NfcSettingsAdapter", "isShowOverflowMenu GSMA is disabled");
                return false;
            }
            if (sService == null) {
                Log.e("NfcSettingsAdapter", "isShowOverflowMenu sService = null");
                return false;
            }
            Log.d("NfcSettingsAdapter", "sService.isShowOverflowMenu()");
            return sService.isShowOverflowMenu();
        } catch (RemoteException e) {
            Log.e("NfcSettingsAdapter", "isShowOverflowMenu e = " + e.toString());
            return false;
        }
    }

    public List<ServiceEntry> getServiceEntryList(int i) {
        try {
            if (!SystemProperties.get("persist.vendor.st_nfc_gsma_support").equals("1")) {
                Log.v("NfcSettingsAdapter", "getServiceEntryList GSMA is disabled");
                return null;
            }
            if (sService == null) {
                Log.e("NfcSettingsAdapter", "getServiceEntryList sService = null");
                return null;
            }
            Log.d("NfcSettingsAdapter", "sService.getServiceEntryList()");
            return sService.getServiceEntryList(i);
        } catch (RemoteException e) {
            Log.e("NfcSettingsAdapter", "getServiceEntryList e = " + e.toString());
            return null;
        }
    }

    public boolean testServiceEntryList(List<ServiceEntry> list) {
        try {
            if (!SystemProperties.get("persist.vendor.st_nfc_gsma_support").equals("1")) {
                Log.v("NfcSettingsAdapter", "testServiceEntryList GSMA is disabled");
                return false;
            }
            if (sService == null) {
                Log.e("NfcSettingsAdapter", "testServiceEntryList sService = null");
                return false;
            }
            Log.d("NfcSettingsAdapter", "sService.testServiceEntryList()");
            return sService.testServiceEntryList(list);
        } catch (RemoteException e) {
            Log.e("NfcSettingsAdapter", "testServiceEntryList e = " + e.toString());
            return false;
        }
    }

    public void commitServiceEntryList(List<ServiceEntry> list) {
        try {
            if (!SystemProperties.get("persist.vendor.st_nfc_gsma_support").equals("1")) {
                Log.v("NfcSettingsAdapter", "commitServiceEntryList GSMA is disabled");
            } else if (sService == null) {
                Log.e("NfcSettingsAdapter", "commitServiceEntryList sService = null");
            } else {
                Log.d("NfcSettingsAdapter", "sService.commitServiceEntryList()");
                sService.commitServiceEntryList(list);
            }
        } catch (RemoteException e) {
            Log.e("NfcSettingsAdapter", "commitServiceEntryList e = " + e.toString());
        }
    }
}
