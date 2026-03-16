package com.android.contacts.common.model.account;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import com.android.contacts.R;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.BaseAccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.util.CommonDateUtils;
import com.google.common.collect.Lists;
import java.util.Locale;

public class ExchangeAccountType extends BaseAccountType {
    public ExchangeAccountType(Context context, String authenticatorPackageName, String type) {
        this.accountType = type;
        this.resourcePackageName = null;
        this.syncAdapterPackageName = authenticatorPackageName;
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
            addDataKindEvent(context);
            addDataKindWebsite(context);
            addDataKindGroupMembership(context);
            this.mIsInitialized = true;
        } catch (AccountType.DefinitionException e) {
            Log.e("ExchangeAccountType", "Problem building account type", e);
        }
    }

    public static boolean isExchangeType(String type) {
        return "com.android.exchange".equals(type) || "com.google.android.exchange".equals(type) || "com.google.android.gm.exchange".equals(type);
    }

    @Override
    protected DataKind addDataKindStructuredName(Context context) throws AccountType.DefinitionException {
        DataKind kind = addKind(new DataKind("vnd.android.cursor.item/name", R.string.nameLabelsGroup, -1, true));
        kind.actionHeader = new BaseAccountType.SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new BaseAccountType.SimpleInflater("data1");
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data4", R.string.name_prefix, 8289).setOptional(true));
        kind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289));
        kind.fieldList.add(new AccountType.EditField("data5", R.string.name_middle, 8289));
        kind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289));
        kind.fieldList.add(new AccountType.EditField("data6", R.string.name_suffix, 8289));
        kind.fieldList.add(new AccountType.EditField("data9", R.string.name_phonetic_family, 193));
        kind.fieldList.add(new AccountType.EditField("data7", R.string.name_phonetic_given, 193));
        return kind;
    }

    @Override
    protected DataKind addDataKindDisplayName(Context context) throws AccountType.DefinitionException {
        DataKind kind = addKind(new DataKind("#displayName", R.string.nameLabelsGroup, -1, true));
        boolean displayOrderPrimary = context.getResources().getBoolean(R.bool.config_editor_field_order_primary);
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data4", R.string.name_prefix, 8289).setOptional(true));
        if (!displayOrderPrimary) {
            kind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289));
            kind.fieldList.add(new AccountType.EditField("data5", R.string.name_middle, 8289).setOptional(true));
            kind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289));
        } else {
            kind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289));
            kind.fieldList.add(new AccountType.EditField("data5", R.string.name_middle, 8289).setOptional(true));
            kind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289));
        }
        kind.fieldList.add(new AccountType.EditField("data6", R.string.name_suffix, 8289).setOptional(true));
        return kind;
    }

    @Override
    protected DataKind addDataKindPhoneticName(Context context) throws AccountType.DefinitionException {
        DataKind kind = addKind(new DataKind("#phoneticName", R.string.name_phonetic, -1, true));
        kind.actionHeader = new BaseAccountType.SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new BaseAccountType.SimpleInflater("data1");
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data9", R.string.name_phonetic_family, 193));
        kind.fieldList.add(new AccountType.EditField("data7", R.string.name_phonetic_given, 193));
        return kind;
    }

    @Override
    protected DataKind addDataKindNickname(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindNickname(context);
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.nicknameLabelsGroup, 8289));
        return kind;
    }

    @Override
    protected DataKind addDataKindPhone(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindPhone(context);
        kind.typeColumn = "data2";
        kind.typeList = Lists.newArrayList();
        kind.typeList.add(buildPhoneType(2).setSpecificMax(1));
        kind.typeList.add(buildPhoneType(1).setSpecificMax(2));
        kind.typeList.add(buildPhoneType(3).setSpecificMax(2));
        kind.typeList.add(buildPhoneType(4).setSecondary(true).setSpecificMax(1));
        kind.typeList.add(buildPhoneType(5).setSecondary(true).setSpecificMax(1));
        kind.typeList.add(buildPhoneType(6).setSecondary(true).setSpecificMax(1));
        kind.typeList.add(buildPhoneType(9).setSecondary(true).setSpecificMax(1));
        kind.typeList.add(buildPhoneType(10).setSecondary(true).setSpecificMax(1));
        kind.typeList.add(buildPhoneType(20).setSecondary(true).setSpecificMax(1));
        kind.typeList.add(buildPhoneType(14).setSecondary(true).setSpecificMax(1));
        kind.typeList.add(buildPhoneType(19).setSecondary(true).setSpecificMax(1));
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.phoneLabelsGroup, 3));
        return kind;
    }

    @Override
    protected DataKind addDataKindEmail(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindEmail(context);
        kind.typeOverallMax = 3;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.emailLabelsGroup, 33));
        return kind;
    }

    @Override
    protected DataKind addDataKindStructuredPostal(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindStructuredPostal(context);
        boolean useJapaneseOrder = Locale.JAPANESE.getLanguage().equals(Locale.getDefault().getLanguage());
        kind.typeColumn = "data2";
        kind.typeList = Lists.newArrayList();
        kind.typeList.add(buildPostalType(2).setSpecificMax(1));
        kind.typeList.add(buildPostalType(1).setSpecificMax(1));
        kind.typeList.add(buildPostalType(3).setSpecificMax(1));
        kind.fieldList = Lists.newArrayList();
        if (useJapaneseOrder) {
            kind.fieldList.add(new AccountType.EditField("data10", R.string.postal_country, 139377).setOptional(true));
            kind.fieldList.add(new AccountType.EditField("data9", R.string.postal_postcode, 139377));
            kind.fieldList.add(new AccountType.EditField("data8", R.string.postal_region, 139377));
            kind.fieldList.add(new AccountType.EditField("data7", R.string.postal_city, 139377));
            kind.fieldList.add(new AccountType.EditField("data4", R.string.postal_street, 139377));
        } else {
            kind.fieldList.add(new AccountType.EditField("data4", R.string.postal_street, 139377));
            kind.fieldList.add(new AccountType.EditField("data7", R.string.postal_city, 139377));
            kind.fieldList.add(new AccountType.EditField("data8", R.string.postal_region, 139377));
            kind.fieldList.add(new AccountType.EditField("data9", R.string.postal_postcode, 139377));
            kind.fieldList.add(new AccountType.EditField("data10", R.string.postal_country, 139377).setOptional(true));
        }
        return kind;
    }

    @Override
    protected DataKind addDataKindIm(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindIm(context);
        kind.typeOverallMax = 3;
        kind.defaultValues = new ContentValues();
        kind.defaultValues.put("data2", (Integer) 3);
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.imLabelsGroup, 33));
        return kind;
    }

    @Override
    protected DataKind addDataKindOrganization(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindOrganization(context);
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.ghostData_company, 8193));
        kind.fieldList.add(new AccountType.EditField("data4", R.string.ghostData_title, 8193));
        return kind;
    }

    @Override
    protected DataKind addDataKindPhoto(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindPhoto(context);
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data15", -1, -1));
        return kind;
    }

    @Override
    protected DataKind addDataKindNote(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindNote(context);
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.label_notes, 147457));
        return kind;
    }

    protected DataKind addDataKindEvent(Context context) throws AccountType.DefinitionException {
        DataKind kind = addKind(new DataKind("vnd.android.cursor.item/contact_event", R.string.eventLabelsGroup, 120, true));
        kind.actionHeader = new BaseAccountType.EventActionInflater();
        kind.actionBody = new BaseAccountType.SimpleInflater("data1");
        kind.typeOverallMax = 1;
        kind.typeColumn = "data2";
        kind.typeList = Lists.newArrayList();
        kind.typeList.add(buildEventType(3, false).setSpecificMax(1));
        kind.dateFormatWithYear = CommonDateUtils.DATE_AND_TIME_FORMAT;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.eventLabelsGroup, 1));
        return kind;
    }

    @Override
    protected DataKind addDataKindWebsite(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindWebsite(context);
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.websiteLabelsGroup, 17));
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
