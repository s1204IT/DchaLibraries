package com.mediatek.contacts.model.account;

import android.content.Context;
import com.android.contacts.R;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.SimAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.google.android.collect.Lists;
import com.mediatek.contacts.util.Log;

public class GsimAccountType extends SimAccountType {
    public GsimAccountType(Context context, String str) {
        Log.i("GsimAccountType", "[GsimAccountType]resPackageName:" + str);
        this.accountType = "SIM Account";
        this.resourcePackageName = null;
        this.syncAdapterPackageName = str;
        this.titleRes = R.string.account_sim_only;
        this.iconRes = R.drawable.quantum_ic_sim_card_vd_theme_24;
        try {
            addDataKindStructuredName2(context);
            addDataKindName2(context);
            addDataKindPhone(context);
            addDataKindPhoto(context);
            this.mIsInitialized = true;
        } catch (AccountType.DefinitionException e) {
            Log.e("GsimAccountType", "[GsimAccountType]DefinitionException:", e);
        }
    }

    @Override
    protected DataKind addDataKindPhone(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindPhone = super.addDataKindPhone(context);
        dataKindAddDataKindPhone.typeColumn = "data2";
        dataKindAddDataKindPhone.typeList = Lists.newArrayList();
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(2).setSpecificMax(2));
        dataKindAddDataKindPhone.typeOverallMax = 1;
        dataKindAddDataKindPhone.fieldList = Lists.newArrayList();
        dataKindAddDataKindPhone.fieldList.add(new AccountType.EditField("data1", R.string.phoneLabelsGroup, 3));
        return dataKindAddDataKindPhone;
    }

    @Override
    protected DataKind addDataKindPhoto(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindPhoto = super.addDataKindPhoto(context);
        dataKindAddDataKindPhoto.typeOverallMax = 1;
        dataKindAddDataKindPhoto.fieldList = Lists.newArrayList();
        dataKindAddDataKindPhoto.fieldList.add(new AccountType.EditField("data15", -1, -1));
        return dataKindAddDataKindPhoto;
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return false;
    }

    @Override
    public boolean isIccCardAccount() {
        return true;
    }
}
