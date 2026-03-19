package com.mediatek.providers.contacts;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.SparseArray;
import com.mediatek.internal.telephony.phb.IMtkIccPhoneBook;

public class SimCardUtils {
    private static final SparseArray<String> SIM_TYPE_ARRAY = new SparseArray<>();

    static {
        SIM_TYPE_ARRAY.put(0, "SIM");
        SIM_TYPE_ARRAY.put(1, "USIM");
        SIM_TYPE_ARRAY.put(2, "RUIM");
        SIM_TYPE_ARRAY.put(3, "CSIM");
    }

    public static String getSimAccountType(int i) {
        return SIM_TYPE_ARRAY.get(i) + " Account";
    }

    private static IMtkIccPhoneBook getIMtkIccPhoneBook() {
        Log.d("ProviderSimCardUtils", "[getIMtkIccPhoneBook]");
        return IMtkIccPhoneBook.Stub.asInterface(ServiceManager.getService("mtksimphonebook"));
    }

    public static String getAASLabel(String str) {
        if (!str.contains("-") || str.indexOf("-") == 0 || str.indexOf("-") == str.length() - 1) {
            Log.w("ProviderSimCardUtils", "[getAASLabel] return;");
            return "";
        }
        String usimAasById = "";
        String[] strArrSplit = str.split("-");
        int iIntValue = Integer.valueOf(strArrSplit[0]).intValue();
        int iIntValue2 = Integer.valueOf(strArrSplit[1]).intValue();
        Log.d("ProviderSimCardUtils", "[getAASLabel] subId: " + iIntValue + ",index: " + iIntValue2);
        if (iIntValue > 0 && iIntValue2 > 0) {
            try {
                IMtkIccPhoneBook iMtkIccPhoneBook = getIMtkIccPhoneBook();
                if (iMtkIccPhoneBook != null) {
                    usimAasById = iMtkIccPhoneBook.getUsimAasById(iIntValue, iIntValue2);
                }
            } catch (RemoteException e) {
                Log.e("ProviderSimCardUtils", "[getAASLabel] catched exception.");
            }
        }
        if (usimAasById == null) {
            usimAasById = "";
        }
        Log.d("ProviderSimCardUtils", "[getAASLabel] aas=" + usimAasById);
        return usimAasById;
    }
}
