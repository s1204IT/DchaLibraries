package com.mediatek.contacts.simcontact;

import android.content.Context;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.internal.telephony.ITelephony;
import com.mediatek.contacts.simservice.SimServiceUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import java.util.HashMap;
import java.util.List;

public class SimCardUtils {
    private static final String[] UICCCARD_PROPERTY_TYPE = {"gsm.ril.uicctype", "gsm.ril.uicctype2", "gsm.ril.uicctype3", "gsm.ril.uicctype4"};

    public static boolean isSimInsertedBySlot(int i) {
        ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        boolean zHasIccCardUsingSlotIndex = false;
        if (iTelephonyAsInterface != null) {
            try {
                zHasIccCardUsingSlotIndex = iTelephonyAsInterface.hasIccCardUsingSlotIndex(i);
            } catch (RemoteException e) {
                Log.e("SimCardUtils", "[isSimInserted]catch exception:");
                e.printStackTrace();
            }
        }
        Log.d("SimCardUtils", "[isSimInserted]slotId:" + i + ",isSimInsert:" + zHasIccCardUsingSlotIndex);
        return zHasIccCardUsingSlotIndex;
    }

    public static boolean isSimInsertedBySub(int i) {
        int slotIdUsingSubId = SubInfoUtils.getSlotIdUsingSubId(i);
        if (slotIdUsingSubId == SubInfoUtils.getInvalidSlotId()) {
            return false;
        }
        return isSimInsertedBySlot(slotIdUsingSubId);
    }

    public static boolean isPhoneBookReady(int i) {
        boolean zIsPhbReady;
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (iMtkTelephonyExAsInterface == null) {
            Log.w("SimCardUtils", "[isPhoneBookReady]phoneEx == null");
            return false;
        }
        try {
            zIsPhbReady = iMtkTelephonyExAsInterface.isPhbReady(i);
        } catch (RemoteException e) {
            Log.e("SimCardUtils", "[isPhoneBookReady]catch exception:");
            e.printStackTrace();
            zIsPhbReady = false;
        }
        Log.d("SimCardUtils", "[isPhoneBookReady]subId:" + i + ", isPbReady:" + zIsPhbReady);
        return zIsPhbReady;
    }

    public static boolean isPhoneBookReady(Context context, int i) {
        boolean z;
        int i2;
        if (isPhoneBookReady(i)) {
            z = false;
            i2 = -1;
        } else {
            i2 = R.string.icc_phone_book_invalid;
            z = true;
        }
        if (context == null) {
            Log.w("SimCardUtils", "[isPhoneBookReady] context is null,subId:" + i);
        }
        if (z && context != null) {
            Toast.makeText(context, i2, 1).show();
            Log.d("SimCardUtils", "[isPhoneBookReady] hitError=" + z);
        }
        return !z;
    }

    public static boolean checkPHBStateAndSimStorage(Context context, int i) {
        boolean z;
        int i2;
        int i3;
        System.currentTimeMillis();
        if (!isPhoneBookReady(i)) {
            i3 = R.string.icc_phone_book_invalid;
        } else if (ShowSimCardStorageInfoTask.getAvailableCount(i) == 0) {
            i3 = R.string.storage_full;
        } else {
            z = false;
            i2 = -1;
            if (context == null) {
                Log.w("SimCardUtils", "[checkPHBStateAndSimStorage] context is null,subId:" + i);
            }
            if (z && context != null) {
                Toast.makeText(context, i2, 1).show();
                Log.d("SimCardUtils", "[checkPHBStateAndSimStorage] hitError=" + z);
            }
            return !z;
        }
        i2 = i3;
        z = true;
        if (context == null) {
        }
        if (z) {
            Toast.makeText(context, i2, 1).show();
            Log.d("SimCardUtils", "[checkPHBStateAndSimStorage] hitError=" + z);
        }
        return !z;
    }

    private static String getSimTypeByProperty(int i) {
        String str;
        int slotIdUsingSubId = SubInfoUtils.getSlotIdUsingSubId(i);
        if (slotIdUsingSubId >= 0 && slotIdUsingSubId < 4) {
            str = SystemProperties.get(UICCCARD_PROPERTY_TYPE[slotIdUsingSubId]);
        } else {
            str = null;
        }
        Log.d("SimCardUtils", "[getSimTypeByProperty]slotId=" + slotIdUsingSubId + ", cardType=" + str);
        return str;
    }

    public static String getSimTypeBySubId(int i) {
        String iccCardType;
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (iMtkTelephonyExAsInterface == null) {
            Log.w("SimCardUtils", "[getSimTypeBySubId]iTel == null");
            return "UNKNOWN";
        }
        try {
            iccCardType = iMtkTelephonyExAsInterface.getIccCardType(i);
            if (iccCardType != null) {
                try {
                    if (!iccCardType.isEmpty()) {
                        return iccCardType;
                    }
                } catch (RemoteException e) {
                    e = e;
                    Log.e("SimCardUtils", "[getSimTypeBySubId]catch exception:");
                    e.printStackTrace();
                    return iccCardType;
                }
            }
            return getSimTypeByProperty(i);
        } catch (RemoteException e2) {
            e = e2;
            iccCardType = "UNKNOWN";
        }
    }

    public static boolean isUsimType(int i) {
        return isUsimType(getSimTypeBySubId(i));
    }

    public static boolean isUsimType(String str) {
        if ("USIM".equals(str)) {
            Log.d("SimCardUtils", "[isUsimType] true");
            return true;
        }
        Log.d("SimCardUtils", "[isUsimType] false");
        return false;
    }

    public static boolean isCsimType(String str) {
        if ("CSIM".equals(str)) {
            Log.d("SimCardUtils", "[isCsimType] true");
            return true;
        }
        Log.d("SimCardUtils", "[isCsimType] false");
        return false;
    }

    public static boolean isRuimType(String str) {
        if ("RUIM".equals(str)) {
            Log.d("SimCardUtils", "[isRuimType] true");
            return true;
        }
        Log.d("SimCardUtils", "[isRuimType] false");
        return false;
    }

    public static boolean isSimType(String str) {
        if ("SIM".equals(str)) {
            Log.d("SimCardUtils", "[isSimType] true");
            return true;
        }
        Log.d("SimCardUtils", "[isSimType] false");
        return false;
    }

    public static boolean isUsimOrCsimType(int i) {
        String simTypeBySubId = getSimTypeBySubId(i);
        return isUsimType(simTypeBySubId) || isCsimType(simTypeBySubId);
    }

    public static boolean isSimStateIdle(Context context, int i) {
        Log.i("SimCardUtils", "[isSimStateIdle] subId: " + i);
        if (!SubInfoUtils.checkSubscriber(i)) {
            return false;
        }
        boolean zIsServiceRunning = SimServiceUtils.isServiceRunning(context, i);
        Log.i("SimCardUtils", "[isSimStateIdle], isSimServiceRunning = " + zIsServiceRunning);
        return isPhoneBookReady(i) && !zIsServiceRunning;
    }

    public static class ShowSimCardStorageInfoTask extends AsyncTask<Void, Void, Boolean> {
        private static HashMap<Integer, Integer> sAvailableStorageMap = new HashMap<>();
        private Context mContext;
        private boolean mNeedToastIfFull;
        private int mSubIdToShow;

        public static void showSimCardStorageInfo(Context context) {
            showSimCardStorageInfo(context, false, SubInfoUtils.getInvalidSubId());
        }

        public static void showSimCardStorageInfo(Context context, boolean z, int i) {
            new ShowSimCardStorageInfoTask(context, z, i).execute(new Void[0]);
            Log.i("SimCardUtils", "[showSimCardStorageInfo] needToastIfFull = " + z + ", subIdToShow = " + i);
        }

        public ShowSimCardStorageInfoTask(Context context, boolean z, int i) {
            this.mContext = null;
            this.mSubIdToShow = SubInfoUtils.getInvalidSubId();
            this.mNeedToastIfFull = false;
            this.mContext = context;
            this.mNeedToastIfFull = z;
            this.mSubIdToShow = i;
        }

        @Override
        protected Boolean doInBackground(Void... voidArr) {
            sAvailableStorageMap.clear();
            List<SubscriptionInfo> activatedSubInfoList = SubInfoUtils.getActivatedSubInfoList();
            Log.i("SimCardUtils", "[ShowSimCardStorageInfoTask]: subInfos.size = " + SubInfoUtils.getActivatedSubInfoCount());
            if (activatedSubInfoList == null || activatedSubInfoList.size() <= 0) {
                Log.w("SimCardUtils", "[ShowSimCardStorageInfoTask] no subscriptionInfo");
                return false;
            }
            for (SubscriptionInfo subscriptionInfo : activatedSubInfoList) {
                Log.d("SimCardUtils", "[ShowSimCardStorageInfoTask]; simName = " + ((Object) subscriptionInfo.getDisplayName()) + "; simSlot = " + subscriptionInfo.getSimSlotIndex() + "; subId = " + subscriptionInfo.getSubscriptionId());
                try {
                    IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.checkService("phoneEx"));
                    if (iMtkTelephonyExAsInterface == null) {
                        Log.w("SimCardUtils", "[ShowSimCardStorageInfoTask] phone = null");
                        return false;
                    }
                    if (!iMtkTelephonyExAsInterface.isPhbReady(subscriptionInfo.getSubscriptionId())) {
                        Log.w("SimCardUtils", "[ShowSimCardStorageInfoTask] phb is not ready, subId = " + subscriptionInfo.getSubscriptionId());
                        return false;
                    }
                    int[] adnStorageInfo = iMtkTelephonyExAsInterface.getAdnStorageInfo(subscriptionInfo.getSubscriptionId());
                    if (adnStorageInfo == null) {
                        Log.w("SimCardUtils", "[ShowSimCardStorageInfoTask] storageInfos = null.");
                        return false;
                    }
                    PhbInfoUtils.getUsimAnrCount(subscriptionInfo.getSubscriptionId());
                    Log.i("SimCardUtils", "[ShowSimCardStorageInfoTask], subId:" + subscriptionInfo.getSubscriptionId() + ": storage:" + adnStorageInfo[1] + ", used:" + adnStorageInfo[0]);
                    if (adnStorageInfo[1] > 0) {
                        sAvailableStorageMap.put(Integer.valueOf(subscriptionInfo.getSubscriptionId()), Integer.valueOf(adnStorageInfo[1] - adnStorageInfo[0]));
                    }
                } catch (RemoteException e) {
                    Log.i("SimCardUtils", "[ShowSimCardStorageInfoTask]_exception: " + e);
                    return false;
                }
            }
            if (this.mNeedToastIfFull && this.mSubIdToShow != SubInfoUtils.getInvalidSubId() && getAvailableCount(this.mSubIdToShow) == 0) {
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (bool.booleanValue()) {
                Toast.makeText(this.mContext, R.string.storage_full, 1).show();
            }
        }

        public static int getAvailableCount(int i) {
            if (sAvailableStorageMap != null && sAvailableStorageMap.containsKey(Integer.valueOf(i))) {
                int iIntValue = sAvailableStorageMap.get(Integer.valueOf(i)).intValue();
                Log.d("SimCardUtils", "[getAvailableCount] result : " + iIntValue + ",subId = " + i);
                return iIntValue;
            }
            Log.i("SimCardUtils", "[getAvailableCount] return -1");
            return -1;
        }
    }
}
