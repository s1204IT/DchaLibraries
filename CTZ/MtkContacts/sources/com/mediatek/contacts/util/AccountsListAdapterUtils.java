package com.mediatek.contacts.util;

import android.content.Context;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.ExchangeAccountType;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.PhbInfoUtils;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import java.util.ArrayList;
import java.util.List;

public final class AccountsListAdapterUtils {
    public static ArrayList<AccountInfo> getGroupAccount(List<AccountInfo> list) {
        ArrayList arrayList = new ArrayList();
        Log.i("AccountsListAdapterUtils", "[getGroupAccount]accountInfoList size:" + list.size());
        for (AccountInfo accountInfo : list) {
            if (accountInfo.getAccount() instanceof AccountWithDataSetEx) {
                int subId = ((AccountWithDataSetEx) accountInfo.getAccount()).getSubId();
                Log.d("AccountsListAdapterUtils", "[getGroupAccount]subId:" + subId);
                if (SimCardUtils.isUsimType(subId)) {
                    Log.d("AccountsListAdapterUtils", "[getGroupAccount]getUsimGroupMaxNameLength:" + PhbInfoUtils.getUsimGroupMaxNameLength(subId));
                    if (PhbInfoUtils.getUsimGroupMaxNameLength(subId) > 0) {
                        arrayList.add(accountInfo);
                    }
                }
            } else {
                arrayList.add(accountInfo);
            }
        }
        return new ArrayList<>(arrayList);
    }

    public static void getViewForName(Context context, AccountWithDataSet accountWithDataSet, AccountType accountType, TextView textView, ImageView imageView) {
        int subId;
        if (accountWithDataSet instanceof AccountWithDataSetEx) {
            AccountWithDataSetEx accountWithDataSetEx = (AccountWithDataSetEx) accountWithDataSet;
            subId = accountWithDataSetEx.getSubId();
            String displayName = accountWithDataSetEx.getDisplayName();
            Log.d("AccountsListAdapterUtils", "[getViewForName]displayName=" + Log.anonymize(displayName));
            if (TextUtils.isEmpty(displayName)) {
                displayName = accountWithDataSet.name;
            }
            textView.setText(displayName);
        } else {
            textView.setText(accountWithDataSet.name);
            subId = -1;
        }
        boolean zIsLocalPhone = AccountWithDataSetEx.isLocalPhone(accountType.accountType);
        Log.d("AccountsListAdapterUtils", "[getViewForName]isLocalPhone:" + zIsLocalPhone + ",activtedSubInfoCount = " + SubInfoUtils.getActivatedSubInfoCount() + "accountType.accountType = " + accountType.accountType + ",account.name = " + Log.anonymize(accountWithDataSet.name));
        if (zIsLocalPhone) {
            textView.setVisibility(8);
        } else {
            textView.setVisibility(0);
        }
        if (ExchangeAccountType.isExchangeType(accountType.accountType)) {
            textView.setVisibility(0);
        }
        textView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        if (accountType != null && accountType.isIccCardAccount()) {
            imageView.setImageDrawable(accountType.getDisplayIconBySubId(context, subId));
        } else {
            imageView.setImageDrawable(accountType.getDisplayIcon(context));
        }
    }
}
