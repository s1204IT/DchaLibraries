package com.android.contacts.model.account;

import android.content.ContentValues;
import android.content.Context;
import com.android.contacts.R;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.CommonDateUtils;
import com.android.contactsbind.FeedbackHelper;
import com.google.common.collect.Lists;
import java.util.Locale;

public class SamsungAccountType extends BaseAccountType {
    public SamsungAccountType(Context context, String str, String str2) {
        this.accountType = str2;
        this.resourcePackageName = null;
        this.syncAdapterPackageName = str;
        try {
            addDataKindStructuredName(context);
            addDataKindName(context);
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
            addDataKindGroupMembership(context);
            addDataKindRelation(context);
            addDataKindEvent(context);
            this.mIsInitialized = true;
        } catch (AccountType.DefinitionException e) {
            FeedbackHelper.sendFeedback(context, "KnownExternalAccount", "Failed to build samsung account type", e);
        }
    }

    public static boolean isSamsungAccountType(Context context, String str, String str2) {
        return "com.osp.app.signin".equals(str) && !ExternalAccountType.hasContactsXml(context, str2);
    }

    @Override
    protected DataKind addDataKindStructuredPostal(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindStructuredPostal = super.addDataKindStructuredPostal(context);
        boolean zEquals = Locale.JAPANESE.getLanguage().equals(Locale.getDefault().getLanguage());
        dataKindAddDataKindStructuredPostal.typeColumn = "data2";
        dataKindAddDataKindStructuredPostal.typeList = Lists.newArrayList();
        dataKindAddDataKindStructuredPostal.typeList.add(buildPostalType(2).setSpecificMax(1));
        dataKindAddDataKindStructuredPostal.typeList.add(buildPostalType(1).setSpecificMax(1));
        dataKindAddDataKindStructuredPostal.typeList.add(buildPostalType(3).setSpecificMax(1));
        dataKindAddDataKindStructuredPostal.fieldList = Lists.newArrayList();
        if (!zEquals) {
            dataKindAddDataKindStructuredPostal.fieldList.add(new AccountType.EditField("data4", R.string.postal_street, 139377));
            dataKindAddDataKindStructuredPostal.fieldList.add(new AccountType.EditField("data7", R.string.postal_city, 139377));
            dataKindAddDataKindStructuredPostal.fieldList.add(new AccountType.EditField("data8", R.string.postal_region, 139377));
            dataKindAddDataKindStructuredPostal.fieldList.add(new AccountType.EditField("data9", R.string.postal_postcode, 139377));
            dataKindAddDataKindStructuredPostal.fieldList.add(new AccountType.EditField("data10", R.string.postal_country, 139377).setOptional(true));
        } else {
            dataKindAddDataKindStructuredPostal.fieldList.add(new AccountType.EditField("data10", R.string.postal_country, 139377).setOptional(true));
            dataKindAddDataKindStructuredPostal.fieldList.add(new AccountType.EditField("data9", R.string.postal_postcode, 139377));
            dataKindAddDataKindStructuredPostal.fieldList.add(new AccountType.EditField("data8", R.string.postal_region, 139377));
            dataKindAddDataKindStructuredPostal.fieldList.add(new AccountType.EditField("data7", R.string.postal_city, 139377));
            dataKindAddDataKindStructuredPostal.fieldList.add(new AccountType.EditField("data4", R.string.postal_street, 139377));
        }
        return dataKindAddDataKindStructuredPostal;
    }

    @Override
    protected DataKind addDataKindPhone(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindPhone = super.addDataKindPhone(context);
        dataKindAddDataKindPhone.typeColumn = "data2";
        dataKindAddDataKindPhone.typeList = Lists.newArrayList();
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(2));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(1));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(3));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(12));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(4).setSecondary(true));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(5).setSecondary(true));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(6).setSecondary(true));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(14).setSecondary(true));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(7));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(0).setSecondary(true).setCustomColumn("data3"));
        dataKindAddDataKindPhone.fieldList = Lists.newArrayList();
        dataKindAddDataKindPhone.fieldList.add(new AccountType.EditField("data1", R.string.phoneLabelsGroup, 3));
        return dataKindAddDataKindPhone;
    }

    @Override
    protected DataKind addDataKindEmail(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindEmail = super.addDataKindEmail(context);
        dataKindAddDataKindEmail.typeColumn = "data2";
        dataKindAddDataKindEmail.typeList = Lists.newArrayList();
        dataKindAddDataKindEmail.typeList.add(buildEmailType(1));
        dataKindAddDataKindEmail.typeList.add(buildEmailType(2));
        dataKindAddDataKindEmail.typeList.add(buildEmailType(3));
        dataKindAddDataKindEmail.typeList.add(buildEmailType(0).setSecondary(true).setCustomColumn("data3"));
        dataKindAddDataKindEmail.fieldList = Lists.newArrayList();
        dataKindAddDataKindEmail.fieldList.add(new AccountType.EditField("data1", R.string.emailLabelsGroup, 33));
        return dataKindAddDataKindEmail;
    }

    private DataKind addDataKindRelation(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/relation", R.string.relationLabelsGroup, BaseAccountType.Weight.WEBSITE, true));
        dataKindAddKind.actionHeader = new BaseAccountType.RelationActionInflater();
        dataKindAddKind.actionBody = new BaseAccountType.SimpleInflater("data1");
        dataKindAddKind.typeColumn = "data2";
        dataKindAddKind.typeList = Lists.newArrayList();
        dataKindAddKind.typeList.add(buildRelationType(1));
        dataKindAddKind.typeList.add(buildRelationType(2));
        dataKindAddKind.typeList.add(buildRelationType(3));
        dataKindAddKind.typeList.add(buildRelationType(4));
        dataKindAddKind.typeList.add(buildRelationType(5));
        dataKindAddKind.typeList.add(buildRelationType(6));
        dataKindAddKind.typeList.add(buildRelationType(7));
        dataKindAddKind.typeList.add(buildRelationType(8));
        dataKindAddKind.typeList.add(buildRelationType(9));
        dataKindAddKind.typeList.add(buildRelationType(10));
        dataKindAddKind.typeList.add(buildRelationType(11));
        dataKindAddKind.typeList.add(buildRelationType(12));
        dataKindAddKind.typeList.add(buildRelationType(13));
        dataKindAddKind.typeList.add(buildRelationType(14));
        dataKindAddKind.typeList.add(buildRelationType(0).setSecondary(true).setCustomColumn("data3"));
        dataKindAddKind.defaultValues = new ContentValues();
        dataKindAddKind.defaultValues.put("data2", (Integer) 14);
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.relationLabelsGroup, 8289));
        return dataKindAddKind;
    }

    private DataKind addDataKindEvent(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/contact_event", R.string.eventLabelsGroup, BaseAccountType.Weight.GROUP_MEMBERSHIP, true));
        dataKindAddKind.actionHeader = new BaseAccountType.EventActionInflater();
        dataKindAddKind.actionBody = new BaseAccountType.SimpleInflater("data1");
        dataKindAddKind.typeColumn = "data2";
        dataKindAddKind.typeList = Lists.newArrayList();
        dataKindAddKind.dateFormatWithoutYear = CommonDateUtils.NO_YEAR_DATE_FORMAT;
        dataKindAddKind.dateFormatWithYear = CommonDateUtils.FULL_DATE_FORMAT;
        dataKindAddKind.typeList.add(buildEventType(3, true).setSpecificMax(1));
        dataKindAddKind.typeList.add(buildEventType(1, false));
        dataKindAddKind.typeList.add(buildEventType(2, false));
        dataKindAddKind.typeList.add(buildEventType(0, false).setSecondary(true).setCustomColumn("data3"));
        dataKindAddKind.defaultValues = new ContentValues();
        dataKindAddKind.defaultValues.put("data2", (Integer) 3);
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.eventLabelsGroup, 1));
        return dataKindAddKind;
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
