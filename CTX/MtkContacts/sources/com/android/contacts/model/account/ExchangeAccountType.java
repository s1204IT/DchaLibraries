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

public class ExchangeAccountType extends BaseAccountType {
    private static final String ACCOUNT_TYPE_AOSP = "com.android.exchange";
    private static final String ACCOUNT_TYPE_GOOGLE_1 = "com.google.android.exchange";
    private static final String ACCOUNT_TYPE_GOOGLE_2 = "com.google.android.gm.exchange";
    private static final String TAG = "ExchangeAccountType";

    public ExchangeAccountType(Context context, String str, String str2) {
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
            addDataKindEvent(context);
            addDataKindWebsite(context);
            addDataKindGroupMembership(context);
            this.mIsInitialized = true;
        } catch (AccountType.DefinitionException e) {
            FeedbackHelper.sendFeedback(context, TAG, "Failed to build exchange account type", e);
        }
    }

    public static boolean isExchangeType(String str) {
        return ACCOUNT_TYPE_AOSP.equals(str) || ACCOUNT_TYPE_GOOGLE_1.equals(str) || ACCOUNT_TYPE_GOOGLE_2.equals(str);
    }

    @Override
    protected DataKind addDataKindStructuredName(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/name", R.string.nameLabelsGroup, -1, true));
        dataKindAddKind.actionHeader = new BaseAccountType.SimpleInflater(R.string.nameLabelsGroup);
        dataKindAddKind.actionBody = new BaseAccountType.SimpleInflater("data1");
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data4", R.string.name_prefix, 8289).setOptional(true));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data5", R.string.name_middle, 8289));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data6", R.string.name_suffix, 8289));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data9", R.string.name_phonetic_family, 193));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data7", R.string.name_phonetic_given, 193));
        return dataKindAddKind;
    }

    @Override
    protected DataKind addDataKindPhoneticName(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME, R.string.name_phonetic, -1, true));
        dataKindAddKind.actionHeader = new BaseAccountType.SimpleInflater(R.string.nameLabelsGroup);
        dataKindAddKind.actionBody = new BaseAccountType.SimpleInflater("data1");
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data9", R.string.name_phonetic_family, 193));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data7", R.string.name_phonetic_given, 193));
        return dataKindAddKind;
    }

    @Override
    protected DataKind addDataKindNickname(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindNickname = super.addDataKindNickname(context);
        dataKindAddDataKindNickname.typeOverallMax = 1;
        dataKindAddDataKindNickname.fieldList = Lists.newArrayList();
        dataKindAddDataKindNickname.fieldList.add(new AccountType.EditField("data1", R.string.nicknameLabelsGroup, 8289));
        return dataKindAddDataKindNickname;
    }

    @Override
    protected DataKind addDataKindPhone(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindPhone = super.addDataKindPhone(context);
        dataKindAddDataKindPhone.typeColumn = "data2";
        dataKindAddDataKindPhone.typeList = Lists.newArrayList();
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(2).setSpecificMax(1));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(1).setSpecificMax(2));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(3).setSpecificMax(2));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(4).setSecondary(true).setSpecificMax(1));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(5).setSecondary(true).setSpecificMax(1));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(6).setSecondary(true).setSpecificMax(1));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(9).setSecondary(true).setSpecificMax(1));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(10).setSecondary(true).setSpecificMax(1));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(20).setSecondary(true).setSpecificMax(1));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(14).setSecondary(true).setSpecificMax(1));
        dataKindAddDataKindPhone.typeList.add(buildPhoneType(19).setSecondary(true).setSpecificMax(1));
        dataKindAddDataKindPhone.fieldList = Lists.newArrayList();
        dataKindAddDataKindPhone.fieldList.add(new AccountType.EditField("data1", R.string.phoneLabelsGroup, 3));
        return dataKindAddDataKindPhone;
    }

    @Override
    protected DataKind addDataKindEmail(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindEmail = super.addDataKindEmail(context);
        dataKindAddDataKindEmail.typeOverallMax = 3;
        dataKindAddDataKindEmail.fieldList = Lists.newArrayList();
        dataKindAddDataKindEmail.fieldList.add(new AccountType.EditField("data1", R.string.emailLabelsGroup, 33));
        return dataKindAddDataKindEmail;
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
    protected DataKind addDataKindIm(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindIm = super.addDataKindIm(context);
        dataKindAddDataKindIm.typeOverallMax = 3;
        dataKindAddDataKindIm.defaultValues = new ContentValues();
        dataKindAddDataKindIm.defaultValues.put("data2", (Integer) 3);
        dataKindAddDataKindIm.fieldList = Lists.newArrayList();
        dataKindAddDataKindIm.fieldList.add(new AccountType.EditField("data1", R.string.imLabelsGroup, 33));
        return dataKindAddDataKindIm;
    }

    @Override
    protected DataKind addDataKindOrganization(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindOrganization = super.addDataKindOrganization(context);
        dataKindAddDataKindOrganization.typeOverallMax = 1;
        dataKindAddDataKindOrganization.fieldList = Lists.newArrayList();
        dataKindAddDataKindOrganization.fieldList.add(new AccountType.EditField("data1", R.string.ghostData_company, 8193));
        dataKindAddDataKindOrganization.fieldList.add(new AccountType.EditField("data4", R.string.ghostData_title, 8193));
        return dataKindAddDataKindOrganization;
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

    protected DataKind addDataKindEvent(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/contact_event", R.string.eventLabelsGroup, BaseAccountType.Weight.EVENT, true));
        dataKindAddKind.actionHeader = new BaseAccountType.EventActionInflater();
        dataKindAddKind.actionBody = new BaseAccountType.SimpleInflater("data1");
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.typeColumn = "data2";
        dataKindAddKind.typeList = Lists.newArrayList();
        dataKindAddKind.typeList.add(buildEventType(3, false).setSpecificMax(1));
        dataKindAddKind.dateFormatWithYear = CommonDateUtils.DATE_AND_TIME_FORMAT;
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.eventLabelsGroup, 1));
        return dataKindAddKind;
    }

    @Override
    protected DataKind addDataKindWebsite(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddDataKindWebsite = super.addDataKindWebsite(context);
        dataKindAddDataKindWebsite.typeOverallMax = 1;
        dataKindAddDataKindWebsite.fieldList = Lists.newArrayList();
        dataKindAddDataKindWebsite.fieldList.add(new AccountType.EditField("data1", R.string.websiteLabelsGroup, 17));
        return dataKindAddDataKindWebsite;
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
