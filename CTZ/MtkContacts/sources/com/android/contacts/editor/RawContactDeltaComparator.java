package com.android.contacts.editor;

import android.content.Context;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.GoogleAccountType;
import java.util.Comparator;

class RawContactDeltaComparator implements Comparator<RawContactDelta> {
    private Context mContext;

    public RawContactDeltaComparator(Context context) {
        this.mContext = context;
    }

    @Override
    public int compare(RawContactDelta rawContactDelta, RawContactDelta rawContactDelta2) {
        int iCompareTo;
        int iCompareTo2;
        if (rawContactDelta.equals(rawContactDelta2)) {
            return 0;
        }
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(this.mContext);
        AccountType accountType = accountTypeManager.getAccountType(rawContactDelta.getValues().getAsString("account_type"), rawContactDelta.getValues().getAsString("data_set"));
        AccountType accountType2 = accountTypeManager.getAccountType(rawContactDelta2.getValues().getAsString("account_type"), rawContactDelta2.getValues().getAsString("data_set"));
        if (!accountType.areContactsWritable() && accountType2.areContactsWritable()) {
            return 1;
        }
        if (accountType.areContactsWritable() && !accountType2.areContactsWritable()) {
            return -1;
        }
        boolean z = accountType instanceof GoogleAccountType;
        boolean z2 = accountType2 instanceof GoogleAccountType;
        if (z && !z2) {
            return -1;
        }
        if (!z && z2) {
            return 1;
        }
        if (!(z && z2)) {
            if (accountType.accountType != null && accountType2.accountType == null) {
                return -1;
            }
            if (accountType.accountType == null && accountType2.accountType != null) {
                return 1;
            }
            if (accountType.accountType != null && accountType2.accountType != null && (iCompareTo2 = accountType.accountType.compareTo(accountType2.accountType)) != 0) {
                return iCompareTo2;
            }
            if (accountType.dataSet != null && accountType2.dataSet == null) {
                return -1;
            }
            if (accountType.dataSet == null && accountType2.dataSet != null) {
                return 1;
            }
            if (accountType.dataSet != null && accountType2.dataSet != null && (iCompareTo = accountType.dataSet.compareTo(accountType2.dataSet)) != 0) {
                return iCompareTo;
            }
        }
        String accountName = rawContactDelta.getAccountName();
        if (accountName == null) {
            accountName = "";
        }
        String accountName2 = rawContactDelta2.getAccountName();
        if (accountName2 == null) {
            accountName2 = "";
        }
        int iCompareTo3 = accountName.compareTo(accountName2);
        if (iCompareTo3 != 0) {
            return iCompareTo3;
        }
        Long rawContactId = rawContactDelta.getRawContactId();
        Long rawContactId2 = rawContactDelta2.getRawContactId();
        if (rawContactId == null && rawContactId2 == null) {
            return 0;
        }
        if (rawContactId == null) {
            return -1;
        }
        if (rawContactId2 == null) {
            return 1;
        }
        return Long.compare(rawContactId.longValue(), rawContactId2.longValue());
    }
}
