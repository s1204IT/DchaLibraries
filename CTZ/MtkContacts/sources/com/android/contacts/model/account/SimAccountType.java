package com.android.contacts.model.account;

import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import com.android.contacts.R;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.google.common.collect.Lists;
import java.util.Collections;

public class SimAccountType extends BaseAccountType {
    public SimAccountType() {
        this.titleRes = R.string.account_sim;
        this.iconRes = R.drawable.quantum_ic_sim_card_vd_theme_24;
    }

    public SimAccountType(Context context) {
        this.titleRes = R.string.account_sim;
        this.iconRes = R.drawable.quantum_ic_sim_card_vd_theme_24;
        try {
            addDataKindStructuredName(context);
            addDataKindName(context);
            DataKind dataKindAddDataKindPhone = addDataKindPhone(context);
            dataKindAddDataKindPhone.typeOverallMax = 1;
            dataKindAddDataKindPhone.typeList = Collections.emptyList();
            this.mIsInitialized = true;
        } catch (AccountType.DefinitionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Drawable getDisplayIcon(Context context) {
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), this.iconRes, null);
        drawable.mutate().setColorFilter(ContextCompat.getColor(context, R.color.actionbar_icon_color_grey), PorterDuff.Mode.SRC_ATOP);
        return drawable;
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return false;
    }

    @Override
    public void initializeFieldsFromAuthenticator(AuthenticatorDescription authenticatorDescription) {
    }

    @Override
    protected DataKind addDataKindStructuredName(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/name", R.string.nameLabelsGroup, -1, true));
        dataKindAddKind.actionHeader = new BaseAccountType.SimpleInflater(R.string.nameLabelsGroup);
        dataKindAddKind.actionBody = new BaseAccountType.SimpleInflater("data1");
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289));
        return dataKindAddKind;
    }

    @Override
    protected DataKind addDataKindName(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_NAME, R.string.nameLabelsGroup, -1, true));
        dataKindAddKind.actionHeader = new BaseAccountType.SimpleInflater(R.string.nameLabelsGroup);
        dataKindAddKind.actionBody = new BaseAccountType.SimpleInflater("data1");
        dataKindAddKind.typeOverallMax = 1;
        boolean z = context.getResources().getBoolean(R.bool.config_editor_field_order_primary);
        dataKindAddKind.fieldList = Lists.newArrayList();
        if (z) {
            dataKindAddKind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289));
            dataKindAddKind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289));
        } else {
            dataKindAddKind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289));
            dataKindAddKind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289));
        }
        return dataKindAddKind;
    }

    protected DataKind addDataKindStructuredName2(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/name", R.string.nameLabelsGroup, -1, true));
        dataKindAddKind.actionHeader = new BaseAccountType.SimpleInflater(R.string.nameLabelsGroup);
        dataKindAddKind.actionBody = new BaseAccountType.SimpleInflater("data1");
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.nameLabelsGroup, 8289));
        return dataKindAddKind;
    }

    protected DataKind addDataKindName2(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_NAME, R.string.nameLabelsGroup, -1, true));
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.nameLabelsGroup, 8289));
        return dataKindAddKind;
    }

    @Override
    public AccountInfo wrapAccount(Context context, AccountWithDataSet accountWithDataSet) {
        return new AccountInfo(new AccountDisplayInfo(accountWithDataSet, getDisplayLabel(context), getDisplayLabel(context), getDisplayIcon(context), true), this);
    }
}
