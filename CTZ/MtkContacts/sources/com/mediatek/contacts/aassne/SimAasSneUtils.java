package com.mediatek.contacts.aassne;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import com.mediatek.contacts.simcontact.PhbInfoUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.internal.telephony.phb.AlphaTag;
import com.mediatek.internal.telephony.phb.IMtkIccPhoneBook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SimAasSneUtils {
    private static HashMap<Integer, List<AlphaTag>> sAasMap = new HashMap<>(2);
    private static String sCurrentAccount = null;
    private static int sCurSubId = -1;

    public static void setCurrentSubId(int i) {
        sCurSubId = i;
        sCurrentAccount = AccountTypeUtils.getAccountTypeBySub(i);
        Log.d("SimAasSneUtils", "[setCurrentSubId] sCurSubId=" + sCurSubId + " sCurrentAccount=" + Log.anonymize(sCurrentAccount));
    }

    public static String getCurAccount() {
        return sCurrentAccount;
    }

    public static int getCurSubId() {
        return sCurSubId;
    }

    public static boolean refreshAASList(int i) {
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        if (slotIndex <= SubInfoUtils.getInvalidSlotId()) {
            Log.d("SimAasSneUtils", "[refreshAASList] slot=" + slotIndex);
            return false;
        }
        try {
            IMtkIccPhoneBook iMtkIccPhoneBook = getIMtkIccPhoneBook();
            if (iMtkIccPhoneBook != null) {
                Log.d("SimAasSneUtils", "[refreshAASList] subId =" + i);
                List<AlphaTag> usimAasList = iMtkIccPhoneBook.getUsimAasList(i);
                if (usimAasList != null) {
                    Iterator<AlphaTag> it = usimAasList.iterator();
                    Log.d("SimAasSneUtils", "[refreshAASList] success");
                    while (it.hasNext()) {
                        String alphaTag = it.next().getAlphaTag();
                        if (TextUtils.isEmpty(alphaTag)) {
                            it.remove();
                        }
                        Log.d("SimAasSneUtils", "[refreshAASList] tag=" + Log.anonymize(alphaTag));
                    }
                }
                sAasMap.put(Integer.valueOf(slotIndex), usimAasList);
                return true;
            }
            return true;
        } catch (RemoteException e) {
            Log.e("SimAasSneUtils", "[refreshAASList] catched exception.");
            sAasMap.put(Integer.valueOf(slotIndex), null);
            return true;
        }
    }

    public static List<AlphaTag> getAAS(int i) {
        ArrayList arrayList = new ArrayList();
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        if (slotIndex <= SubInfoUtils.getInvalidSlotId()) {
            Log.e("SimAasSneUtils", "[getAAS] slot=" + slotIndex);
            return arrayList;
        }
        Log.d("SimAasSneUtils", "[getAAS] refreshAASList");
        refreshAASList(i);
        List<AlphaTag> list = sAasMap.get(Integer.valueOf(slotIndex));
        return list != null ? list : arrayList;
    }

    public static int getAasIndexFromIndicator(String str) {
        if (!TextUtils.isEmpty(str) && str.contains("-")) {
            return Integer.valueOf(str.split("-")[1]).intValue();
        }
        return 0;
    }

    public static String getAASByIndicator(String str) {
        if (TextUtils.isEmpty(str) || !str.contains("-")) {
            return null;
        }
        String[] strArrSplit = str.split("-");
        if (strArrSplit.length != 2) {
            return null;
        }
        return getAASById(Integer.valueOf(strArrSplit[0]).intValue(), Integer.valueOf(strArrSplit[1]).intValue());
    }

    public static String getAASById(int i, int i2) {
        if (SubscriptionManager.getSlotIndex(i) <= SubInfoUtils.getInvalidSlotId() || i2 < 1) {
            return "";
        }
        String usimAasById = "";
        try {
            IMtkIccPhoneBook iMtkIccPhoneBook = getIMtkIccPhoneBook();
            if (iMtkIccPhoneBook != null) {
                usimAasById = iMtkIccPhoneBook.getUsimAasById(i, i2);
            }
        } catch (RemoteException e) {
            Log.e("SimAasSneUtils", "[getUSIMAASById] catched exception.");
        }
        if (usimAasById == null) {
            usimAasById = "";
        }
        Log.d("SimAasSneUtils", "[getUSIMAASById] aas=" + Log.anonymize(usimAasById));
        return usimAasById;
    }

    public static String buildAASIndicator(String str, int i) {
        int aasIndexByName = getAasIndexByName(str, i);
        if (aasIndexByName == -1) {
            return "";
        }
        return "" + i + "-" + aasIndexByName;
    }

    public static int getAasIndexByName(String str, int i) {
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        if (slotIndex <= SubInfoUtils.getInvalidSlotId() || TextUtils.isEmpty(str)) {
            Log.e("SimAasSneUtils", "[getAasIndexByName] error slotId=" + slotIndex + ",aas=" + Log.anonymize(str));
            return -1;
        }
        Log.d("SimAasSneUtils", "[getAasIndexByName] aas=" + Log.anonymize(str));
        for (AlphaTag alphaTag : getAAS(i)) {
            String alphaTag2 = alphaTag.getAlphaTag();
            if (str.equalsIgnoreCase(alphaTag2)) {
                Log.d("SimAasSneUtils", "[getAasIndexByName] tag=" + Log.anonymize(alphaTag2));
                return alphaTag.getRecordIndex();
            }
        }
        return -1;
    }

    public static int insertUSIMAAS(int i, String str) {
        if (SubscriptionManager.getSlotIndex(i) <= SubInfoUtils.getInvalidSlotId() || TextUtils.isEmpty(str)) {
            return -1;
        }
        try {
            IMtkIccPhoneBook iMtkIccPhoneBook = getIMtkIccPhoneBook();
            if (iMtkIccPhoneBook != null) {
                return iMtkIccPhoneBook.insertUsimAas(i, str);
            }
            return -1;
        } catch (RemoteException e) {
            Log.e("SimAasSneUtils", "[insertUSIMAAS] catched exception.");
            return -1;
        }
    }

    public static boolean updateUSIMAAS(int i, int i2, int i3, String str) {
        boolean zUpdateUsimAas = false;
        try {
            IMtkIccPhoneBook iMtkIccPhoneBook = getIMtkIccPhoneBook();
            if (iMtkIccPhoneBook != null) {
                zUpdateUsimAas = iMtkIccPhoneBook.updateUsimAas(i, i2, i3, str);
            }
        } catch (RemoteException e) {
            Log.e("SimAasSneUtils", "[updateUSIMAAS] catched exception.");
        }
        Log.d("SimAasSneUtils", "[updateUSIMAAS] refreshAASList");
        refreshAASList(i);
        return zUpdateUsimAas;
    }

    public static boolean removeUsimAasById(int i, int i2, int i3) {
        boolean zRemoveUsimAasById = false;
        try {
            IMtkIccPhoneBook iMtkIccPhoneBook = getIMtkIccPhoneBook();
            if (iMtkIccPhoneBook != null) {
                zRemoveUsimAasById = iMtkIccPhoneBook.removeUsimAasById(i, i2, i3);
            }
        } catch (RemoteException e) {
            Log.e("SimAasSneUtils", "[removeUsimAasById] catched exception.");
        }
        Log.d("SimAasSneUtils", "[removeUsimAasById] refreshAASList");
        refreshAASList(i);
        return zRemoveUsimAasById;
    }

    public static boolean isAasTextValid(String str, int i) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        int usimAasMaxNameLength = PhbInfoUtils.getUsimAasMaxNameLength(i);
        try {
            GsmAlphabet.stringToGsm7BitPacked(str);
        } catch (EncodeException e) {
            if (str.length() > ((usimAasMaxNameLength - 1) >> 1)) {
                return false;
            }
        }
        return str.length() <= usimAasMaxNameLength;
    }

    private static IMtkIccPhoneBook getIMtkIccPhoneBook() {
        Log.d("SimAasSneUtils", "[getIMtkIccPhoneBook]");
        return IMtkIccPhoneBook.Stub.asInterface(ServiceManager.getService("mtksimphonebook"));
    }

    public static String getSuffix(int i) {
        if (i <= 0) {
            return "";
        }
        return String.valueOf(i);
    }
}
