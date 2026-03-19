package com.mediatek.contacts.model.account;

import android.content.Context;
import com.android.contacts.R;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.SimAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.google.android.collect.Lists;
import com.mediatek.contacts.util.Log;

public class UsimAccountType extends SimAccountType {
    public UsimAccountType(Context context, String str) {
        Log.i("UsimAccountType", "[UsimAccountType]resPackageName:" + str);
        this.accountType = "USIM Account";
        this.resourcePackageName = null;
        this.syncAdapterPackageName = str;
        this.titleRes = R.string.account_usim_only;
        this.iconRes = R.drawable.quantum_ic_sim_card_vd_theme_24;
        try {
            addDataKindStructuredName2(context);
            addDataKindName2(context);
            addDataKindPhone(context);
            addDataKindEmail(context);
            addDataKindPhoto(context);
            addDataKindGroupMembership(context);
            this.mIsInitialized = true;
        } catch (AccountType.DefinitionException e) {
            Log.e("UsimAccountType", "[UsimAccountType]DefinitionException:" + e);
        }
    }

    @Override
    protected DataKind addDataKindPhone(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindPhone = super.addDataKindPhone(context);
        dataKindAddDataKindPhone.typeColumn = "data2";
        dataKindAddDataKindPhone.typeList = Lists.newArrayList();
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(2).setSpecificMax(-1));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(7).setSpecificMax(-1));
        dataKindAddDataKindPhone.typeOverallMax = -1;
        dataKindAddDataKindPhone.fieldList = Lists.newArrayList();
        dataKindAddDataKindPhone.fieldList.add(new AccountType.EditField("data1", R.string.phoneLabelsGroup, 3));
        return dataKindAddDataKindPhone;
    }

    @Override
    protected DataKind addDataKindEmail(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindEmail = super.addDataKindEmail(context);
        dataKindAddDataKindEmail.typeOverallMax = 1;
        dataKindAddDataKindEmail.typeColumn = "data2";
        dataKindAddDataKindEmail.typeList = Lists.newArrayList();
        Log.e("UsimAccountType", "[addDataKindEmail]for AAS.");
        dataKindAddDataKindEmail.fieldList = Lists.newArrayList();
        dataKindAddDataKindEmail.fieldList.add(new AccountType.EditField("data1", R.string.emailLabelsGroup, 33));
        return dataKindAddDataKindEmail;
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
    protected DataKind addDataKindNote(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindNote = super.addDataKindNote(context);
        dataKindAddDataKindNote.fieldList = Lists.newArrayList();
        dataKindAddDataKindNote.fieldList.add(new AccountType.EditField("data1", R.string.label_notes, 147457));
        return dataKindAddDataKindNote;
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return true;
    }

    @Override
    public boolean isIccCardAccount() {
        return true;
    }
}
