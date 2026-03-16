package com.android.contacts.common.model.account;

import android.content.Context;
import android.util.Log;
import com.android.contacts.R;
import com.android.contacts.common.model.account.AccountType;

public class FallbackAccountType extends BaseAccountType {
    private FallbackAccountType(Context context, String resPackageName) {
        this.accountType = null;
        this.dataSet = null;
        this.titleRes = R.string.account_phone;
        this.iconRes = R.mipmap.ic_contacts_clr_48cv_44dp;
        this.resourcePackageName = resPackageName;
        this.syncAdapterPackageName = resPackageName;
        try {
            addDataKindStructuredName(context);
            addDataKindDisplayName(context);
            addDataKindPhoneticName(context);
            addDataKindNickname(context);
            addDataKindPhone(context);
            addDataKindEmail(context);
            addDataKindStructuredPostal(context);
            addDataKindIm(context);
            addDataKindOrganization(context);
            addDataKindPhoto(context);
            addDataKindNote(context);
            addDataKindWebsite(context);
            addDataKindSipAddress(context);
            this.mIsInitialized = true;
        } catch (AccountType.DefinitionException e) {
            Log.e("FallbackAccountType", "Problem building account type", e);
        }
    }

    public FallbackAccountType(Context context) {
        this(context, null);
    }

    static AccountType createWithPackageNameForTest(Context context, String resPackageName) {
        return new FallbackAccountType(context, resPackageName);
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }
}
