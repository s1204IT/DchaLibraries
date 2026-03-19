package com.mediatek.contacts.quickcontact;

import android.content.Context;
import android.text.TextUtils;
import com.android.contacts.group.GroupMetaData;
import com.android.contacts.model.Contact;
import com.mediatek.contacts.simcontact.PhbInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import java.util.Iterator;
import java.util.List;

public class QuickContactUtils {
    private static String TAG = "QuickContactUtils";
    private static String sSipAddress = null;

    public static String getGroupTitle(List<GroupMetaData> list, long j) {
        if (list == null) {
            Log.w(TAG, "[getGroupTitle]groupMetaData is null,return. ");
            return null;
        }
        Iterator<GroupMetaData> it = list.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            GroupMetaData next = it.next();
            if (next.groupId == j) {
                if (!next.defaultGroup && !next.favorites) {
                    String str = next.groupName;
                    if (!TextUtils.isEmpty(str)) {
                        return str;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isSupportShowEmailData(Contact contact, Context context) {
        Log.sensitive(TAG, "[isSupportShowEmailData] data : " + contact);
        if (contact == null) {
            return false;
        }
        String accountTypeString = contact.getRawContacts().get(0).getAccountTypeString();
        Log.sensitive(TAG, "[isSupportShowEmailData] accoutType : " + accountTypeString);
        if ("SIM Account".equals(accountTypeString) || "RUIM Account".equals(accountTypeString)) {
            Log.i(TAG, "[isSupportShowEmailData] Ruim or sim not support email! ");
            return false;
        }
        if ("USIM Account".equals(accountTypeString)) {
            String accountName = contact.getRawContacts().get(0).getAccountName();
            int subIdBySimAccountName = AccountTypeUtils.getSubIdBySimAccountName(context, accountName);
            int usimEmailCount = PhbInfoUtils.getUsimEmailCount(subIdBySimAccountName);
            Log.sensitive(TAG, "[isSupportShowEmailData] Usim type, accountName: " + accountName + ",subId: " + subIdBySimAccountName + ",emailCount: " + usimEmailCount);
            if (usimEmailCount <= 0) {
                Log.i(TAG, "[isSupportShowEmailData] Usim not support email field,remove it!!");
                return false;
            }
            return true;
        }
        return true;
    }
}
