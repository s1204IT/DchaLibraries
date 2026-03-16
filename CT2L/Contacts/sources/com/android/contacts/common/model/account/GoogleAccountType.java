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
import java.util.List;

public class GoogleAccountType extends BaseAccountType {
    private static final List<String> mExtensionPackages = Lists.newArrayList("com.google.android.apps.plus");

    public GoogleAccountType(Context context, String authenticatorPackageName) {
        this.accountType = "com.google";
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
            addDataKindWebsite(context);
            addDataKindSipAddress(context);
            addDataKindGroupMembership(context);
            addDataKindRelation(context);
            addDataKindEvent(context);
            this.mIsInitialized = true;
        } catch (AccountType.DefinitionException e) {
            Log.e("GoogleAccountType", "Problem building account type", e);
        }
    }

    @Override
    public List<String> getExtensionPackageNames() {
        return mExtensionPackages;
    }

    @Override
    protected DataKind addDataKindPhone(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindPhone(context);
        kind.typeColumn = "data2";
        kind.typeList = Lists.newArrayList();
        kind.typeList.add(buildPhoneType(2));
        kind.typeList.add(buildPhoneType(3));
        kind.typeList.add(buildPhoneType(1));
        kind.typeList.add(buildPhoneType(12));
        kind.typeList.add(buildPhoneType(4).setSecondary(true));
        kind.typeList.add(buildPhoneType(5).setSecondary(true));
        kind.typeList.add(buildPhoneType(6).setSecondary(true));
        kind.typeList.add(buildPhoneType(7));
        kind.typeList.add(buildPhoneType(0).setSecondary(true).setCustomColumn("data3"));
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.phoneLabelsGroup, 3));
        return kind;
    }

    @Override
    protected DataKind addDataKindEmail(Context context) throws AccountType.DefinitionException {
        DataKind kind = super.addDataKindEmail(context);
        kind.typeColumn = "data2";
        kind.typeList = Lists.newArrayList();
        kind.typeList.add(buildEmailType(1));
        kind.typeList.add(buildEmailType(2));
        kind.typeList.add(buildEmailType(3));
        kind.typeList.add(buildEmailType(0).setSecondary(true).setCustomColumn("data3"));
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.emailLabelsGroup, 33));
        return kind;
    }

    private DataKind addDataKindRelation(Context context) throws AccountType.DefinitionException {
        DataKind kind = addKind(new DataKind("vnd.android.cursor.item/relation", R.string.relationLabelsGroup, 999, true));
        kind.actionHeader = new BaseAccountType.RelationActionInflater();
        kind.actionBody = new BaseAccountType.SimpleInflater("data1");
        kind.typeColumn = "data2";
        kind.typeList = Lists.newArrayList();
        kind.typeList.add(buildRelationType(1));
        kind.typeList.add(buildRelationType(2));
        kind.typeList.add(buildRelationType(3));
        kind.typeList.add(buildRelationType(4));
        kind.typeList.add(buildRelationType(5));
        kind.typeList.add(buildRelationType(6));
        kind.typeList.add(buildRelationType(7));
        kind.typeList.add(buildRelationType(8));
        kind.typeList.add(buildRelationType(9));
        kind.typeList.add(buildRelationType(10));
        kind.typeList.add(buildRelationType(11));
        kind.typeList.add(buildRelationType(12));
        kind.typeList.add(buildRelationType(13));
        kind.typeList.add(buildRelationType(14));
        kind.typeList.add(buildRelationType(0).setSecondary(true).setCustomColumn("data3"));
        kind.defaultValues = new ContentValues();
        kind.defaultValues.put("data2", (Integer) 14);
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.relationLabelsGroup, 8289));
        return kind;
    }

    private DataKind addDataKindEvent(Context context) throws AccountType.DefinitionException {
        DataKind kind = addKind(new DataKind("vnd.android.cursor.item/contact_event", R.string.eventLabelsGroup, 120, true));
        kind.actionHeader = new BaseAccountType.EventActionInflater();
        kind.actionBody = new BaseAccountType.SimpleInflater("data1");
        kind.typeColumn = "data2";
        kind.typeList = Lists.newArrayList();
        kind.dateFormatWithoutYear = CommonDateUtils.NO_YEAR_DATE_FORMAT;
        kind.dateFormatWithYear = CommonDateUtils.FULL_DATE_FORMAT;
        kind.typeList.add(buildEventType(3, true).setSpecificMax(1));
        kind.typeList.add(buildEventType(1, false));
        kind.typeList.add(buildEventType(2, false));
        kind.typeList.add(buildEventType(0, false).setSecondary(true).setCustomColumn("data3"));
        kind.defaultValues = new ContentValues();
        kind.defaultValues.put("data2", (Integer) 3);
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new AccountType.EditField("data1", R.string.eventLabelsGroup, 1));
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

    @Override
    public String getViewContactNotifyServiceClassName() {
        return "com.google.android.syncadapters.contacts.SyncHighResPhotoIntentService";
    }

    @Override
    public String getViewContactNotifyServicePackageName() {
        return "com.google.android.syncadapters.contacts";
    }
}
