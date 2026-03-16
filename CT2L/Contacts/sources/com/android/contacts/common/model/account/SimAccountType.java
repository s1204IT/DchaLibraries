package com.android.contacts.common.model.account;

import android.content.ContentValues;
import android.content.Context;
import android.provider.ContactsContract;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.contacts.R;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.BaseAccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.util.SimPhoneBookCommonUtil;
import com.google.common.collect.Lists;

public class SimAccountType extends BaseAccountType {
    public static String ACCOUNT_NAME = "Sim1";
    private int mSlotId;

    public SimAccountType(Context context, int slotId) {
        this.mSlotId = slotId;
        TelephonyManager tm = TelephonyManager.from(context);
        if (slotId == 0) {
            this.accountType = "com.android.contact.sim";
            this.titleRes = tm.isMultiSimEnabled() ? R.string.account_sim1 : R.string.account_sim;
        } else {
            this.accountType = "com.android.contact.sim2";
            this.titleRes = R.string.account_sim2;
        }
        this.resourcePackageName = null;
        this.syncAdapterPackageName = null;
    }

    public static String getSimAccountType(int slotId) {
        return slotId == 0 ? "com.android.contact.sim" : "com.android.contact.sim2";
    }

    public static String getSimAccountName(int slotId) {
        return slotId == 0 ? ACCOUNT_NAME : "Sim2";
    }

    @Override
    public DataKind addDataKindStructuredName(Context context) throws AccountType.DefinitionException {
        DataKind kind = addKind(new DataKind("vnd.android.cursor.item/name", R.string.nameLabelsGroup, -1, true));
        kind.actionHeader = new BaseAccountType.SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new BaseAccountType.SimpleInflater("data1");
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.full_name, 8289));
        kind.fieldList.add(new AccountType.EditField("data4", R.string.name_prefix, 8289).setLongForm(true));
        kind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289).setLongForm(true));
        kind.fieldList.add(new AccountType.EditField("data5", R.string.name_middle, 8289).setLongForm(true));
        kind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289).setLongForm(true));
        kind.fieldList.add(new AccountType.EditField("data6", R.string.name_suffix, 8289).setLongForm(true));
        return kind;
    }

    @Override
    public DataKind addDataKindDisplayName(Context context) throws AccountType.DefinitionException {
        DataKind kind = addKind(new DataKind("#displayName", R.string.nameLabelsGroup, -1, true));
        kind.actionHeader = new BaseAccountType.SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new BaseAccountType.SimpleInflater("data1");
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.full_name, 8289).setShortForm(true));
        context.getResources().getBoolean(R.bool.config_editor_field_order_primary);
        return kind;
    }

    @Override
    public DataKind addDataKindNickname(Context context) throws AccountType.DefinitionException {
        DataKind kind = addKind(new DataKind("vnd.android.cursor.item/nickname", R.string.name_sne, 115, true));
        kind.typeOverallMax = 1;
        kind.actionHeader = new BaseAccountType.SimpleInflater(R.string.name_sne);
        kind.actionBody = new BaseAccountType.SimpleInflater("data1");
        kind.defaultValues = new ContentValues();
        kind.defaultValues.put("data2", (Integer) 1);
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.name_sne, 8289));
        return kind;
    }

    @Override
    public DataKind addDataKindPhone(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindPhone(context);
        TelephonyManager tm = TelephonyManager.from(context);
        int[] subs = SubscriptionManager.getSubId(this.mSlotId);
        kind.typeColumn = "data2";
        kind.typeList = Lists.newArrayList();
        if (2 == tm.getUiccAppType(subs[0])) {
            AccountType.EditType m = buildPhoneType(2).setSpecificMax(1);
            AccountType.EditType h = buildPhoneType(1).setSpecificMax(1);
            kind.typeList.add(m);
            kind.typeList.add(h);
            kind.typeList.set(0, m).labelRes = ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(2);
            kind.typeList.set(1, h).labelRes = ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(1);
            kind.typeOverallMax = 2;
        } else {
            kind.typeList.add(buildPhoneType(2).setSpecificMax(1));
            kind.typeOverallMax = 1;
        }
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.phoneLabelsGroup, 3));
        return kind;
    }

    @Override
    public DataKind addDataKindEmail(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindEmail(context);
        kind.typeColumn = "data2";
        kind.typeList = Lists.newArrayList();
        int size = SimPhoneBookCommonUtil.getEmailFieldSize();
        Log.d("SimAccountType", "USIM email size = " + size);
        switch (size) {
            case 1:
                kind.typeList.add(buildEmailType(4).setSpecificMax(1));
                kind.typeOverallMax = 1;
                break;
            case 2:
                kind.typeList.add(buildEmailType(4).setSpecificMax(1));
                kind.typeList.add(buildEmailType(3).setSpecificMax(1));
                kind.typeOverallMax = 2;
                break;
            default:
                kind.typeList.add(buildEmailType(4).setSpecificMax(1));
                kind.typeOverallMax = 1;
                break;
        }
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.emailLabelsGroup, 33));
        return kind;
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return true;
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }
}
