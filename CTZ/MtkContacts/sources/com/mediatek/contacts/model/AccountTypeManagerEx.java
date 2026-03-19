package com.mediatek.contacts.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import com.android.contacts.model.account.AccountWithDataSet;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AccountTypeManagerEx {
    public static void registerReceiverOnSimStateAndInfoChanged(Context context, BroadcastReceiver broadcastReceiver) {
        Log.i("AccountTypeManagerEx", "[registerReceiverOnSimStateAndInfoChanged]...");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("mediatek.intent.action.PHB_STATE_CHANGED");
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    public static void loadSimAndLocalAccounts(Set<AccountWithDataSet> set) {
        addLocalAccount(set);
        if (UserHandle.myUserId() == 0) {
            loadIccAccount(set);
        }
    }

    private static void addLocalAccount(Set<AccountWithDataSet> set) {
        if (GlobalEnv.isUsingTwoPanes()) {
            set.add(new AccountWithDataSet("Tablet", "Local Phone Account", null));
        } else {
            set.add(new AccountWithDataSet("Phone", "Local Phone Account", null));
        }
    }

    private static void loadIccAccount(Set<AccountWithDataSet> set) {
        List<SubscriptionInfo> activatedSubInfoList = SubInfoUtils.getActivatedSubInfoList();
        if (activatedSubInfoList != null && activatedSubInfoList.size() > 0) {
            Iterator<SubscriptionInfo> it = activatedSubInfoList.iterator();
            while (it.hasNext()) {
                int subscriptionId = it.next().getSubscriptionId();
                boolean zIsPhoneBookReady = SimCardUtils.isPhoneBookReady(subscriptionId);
                Log.i("AccountTypeManagerEx", "[loadIccAccount]subId: " + subscriptionId + ",phbReady:" + zIsPhoneBookReady);
                if (zIsPhoneBookReady && hasValidSimStorage(subscriptionId)) {
                    addIccAccount(set, subscriptionId);
                }
            }
            return;
        }
        Log.w("AccountTypeManagerEx", "[loadIccAccount]No valid subscriptionInfoList: " + activatedSubInfoList);
    }

    private static void addIccAccount(Set<AccountWithDataSet> set, int i) {
        String accountNameUsingSubId = AccountTypeUtils.getAccountNameUsingSubId(i);
        String accountTypeBySub = AccountTypeUtils.getAccountTypeBySub(i);
        if (!TextUtils.isEmpty(accountNameUsingSubId) && !TextUtils.isEmpty(accountTypeBySub)) {
            set.add(new AccountWithDataSetEx(accountNameUsingSubId, accountTypeBySub, i));
            Log.d("AccountTypeManagerEx", "[addIccAccount] new AccountWithDataSetEx, subId:" + i + ", AccountName:" + Log.anonymize(accountNameUsingSubId) + ", AccountType: " + accountTypeBySub);
        }
    }

    private static boolean hasValidSimStorage(int i) {
        try {
            IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.checkService("phoneEx"));
            if (iMtkTelephonyExAsInterface != null) {
                int[] adnStorageInfo = iMtkTelephonyExAsInterface.getAdnStorageInfo(i);
                if (adnStorageInfo == null) {
                    Log.e("AccountTypeManagerEx", "[hasValidSimStorage]storageInfos = null.");
                    return false;
                }
                if (adnStorageInfo[1] <= 0) {
                    Log.d("AccountTypeManagerEx", "[hasValidSimStorage] not support storage:" + adnStorageInfo[1]);
                    return false;
                }
                return true;
            }
            Log.e("AccountTypeManagerEx", "[hasValidSimStorage] phoneEx is null");
            return false;
        } catch (RemoteException e) {
            Log.e("AccountTypeManagerEx", "[hasValidSimStorage] exception: ", e);
            return false;
        }
    }
}
