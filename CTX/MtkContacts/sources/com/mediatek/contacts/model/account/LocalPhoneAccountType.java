package com.mediatek.contacts.model.account;

import android.content.Context;
import com.android.contacts.R;
import com.android.contacts.model.account.DeviceLocalAccountType;
import com.mediatek.contacts.util.Log;

public class LocalPhoneAccountType extends DeviceLocalAccountType {
    public LocalPhoneAccountType(Context context, String str) {
        super(context, true);
        Log.i("LocalPhoneAccountType", "[LocalPhoneAccountType]resPackageName:" + str);
        this.accountType = "Local Phone Account";
        this.resourcePackageName = null;
        this.syncAdapterPackageName = str;
        this.titleRes = R.string.account_phone_only;
    }
}
