package com.android.contacts.model.dataitem;

import android.content.ContentValues;
import android.content.Context;
import com.android.contacts.Collapser;
import com.android.contacts.MoreContactUtils;
import com.android.contacts.model.RawContactModifier;

public class DataItem implements Collapser.Collapsible<DataItem> {
    private final ContentValues mContentValues;
    protected DataKind mKind;

    protected DataItem(ContentValues contentValues) {
        this.mContentValues = contentValues;
    }

    public static DataItem createFrom(ContentValues contentValues) {
        String asString = contentValues.getAsString("mimetype");
        if ("vnd.android.cursor.item/group_membership".equals(asString)) {
            return new GroupMembershipDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/name".equals(asString)) {
            return new StructuredNameDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/phone_v2".equals(asString)) {
            return new PhoneDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/email_v2".equals(asString)) {
            return new EmailDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/postal-address_v2".equals(asString)) {
            return new StructuredPostalDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/im".equals(asString)) {
            return new ImDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/organization".equals(asString)) {
            return new OrganizationDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/nickname".equals(asString)) {
            return new NicknameDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/note".equals(asString)) {
            return new NoteDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/website".equals(asString)) {
            return new WebsiteDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/sip_address".equals(asString)) {
            return new SipAddressDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/contact_event".equals(asString)) {
            return new EventDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/relation".equals(asString)) {
            return new RelationDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/identity".equals(asString)) {
            return new IdentityDataItem(contentValues);
        }
        if ("vnd.android.cursor.item/photo".equals(asString)) {
            return new PhotoDataItem(contentValues);
        }
        if ("vnd.com.google.cursor.item/contact_user_defined_field".equals(asString)) {
            return new CustomDataItem(contentValues);
        }
        return new DataItem(contentValues);
    }

    public ContentValues getContentValues() {
        return this.mContentValues;
    }

    public void setRawContactId(long j) {
        this.mContentValues.put("raw_contact_id", Long.valueOf(j));
    }

    public Long getRawContactId() {
        return this.mContentValues.getAsLong("raw_contact_id");
    }

    public long getId() {
        return this.mContentValues.getAsLong("_id").longValue();
    }

    public String getMimeType() {
        return this.mContentValues.getAsString("mimetype");
    }

    public void setMimeType(String str) {
        this.mContentValues.put("mimetype", str);
    }

    public boolean isPrimary() {
        Integer asInteger = this.mContentValues.getAsInteger("is_primary");
        return (asInteger == null || asInteger.intValue() == 0) ? false : true;
    }

    public boolean isSuperPrimary() {
        Integer asInteger = this.mContentValues.getAsInteger("is_super_primary");
        return (asInteger == null || asInteger.intValue() == 0) ? false : true;
    }

    public boolean hasKindTypeColumn(DataKind dataKind) {
        String str = dataKind.typeColumn;
        return (str == null || !this.mContentValues.containsKey(str) || this.mContentValues.getAsInteger(str) == null) ? false : true;
    }

    public int getKindTypeColumn(DataKind dataKind) {
        return this.mContentValues.getAsInteger(dataKind.typeColumn).intValue();
    }

    public int getCarrierPresence() {
        Integer asInteger = this.mContentValues.getAsInteger("carrier_presence");
        if (asInteger != null) {
            return asInteger.intValue();
        }
        return 0;
    }

    public String buildDataString(Context context, DataKind dataKind) {
        CharSequence charSequenceInflateUsing;
        if (dataKind.actionBody == null || (charSequenceInflateUsing = dataKind.actionBody.inflateUsing(context, this.mContentValues)) == null) {
            return null;
        }
        return charSequenceInflateUsing.toString();
    }

    public String buildDataStringForDisplay(Context context, DataKind dataKind) {
        return buildDataString(context, dataKind);
    }

    public void setDataKind(DataKind dataKind) {
        this.mKind = dataKind;
    }

    public DataKind getDataKind() {
        return this.mKind;
    }

    public Integer getTimesUsed() {
        return this.mContentValues.getAsInteger("times_used");
    }

    public Long getLastTimeUsed() {
        return this.mContentValues.getAsLong("last_time_used");
    }

    @Override
    public void collapseWith(DataItem dataItem) {
        int iIntValue;
        long jLongValue;
        DataKind dataKind = getDataKind();
        DataKind dataKind2 = dataItem.getDataKind();
        if ((!hasKindTypeColumn(dataKind) && dataItem.hasKindTypeColumn(dataKind2)) || (dataItem.hasKindTypeColumn(dataKind2) && RawContactModifier.getTypePrecedence(dataKind, getKindTypeColumn(dataKind)) > RawContactModifier.getTypePrecedence(dataKind2, dataItem.getKindTypeColumn(dataKind2)))) {
            this.mContentValues.put(dataKind2.typeColumn, Integer.valueOf(dataItem.getKindTypeColumn(dataKind2)));
            this.mKind = dataKind2;
        }
        this.mKind.maxLinesForDisplay = Math.max(dataKind.maxLinesForDisplay, dataKind2.maxLinesForDisplay);
        if (isSuperPrimary() || dataItem.isSuperPrimary()) {
            this.mContentValues.put("is_super_primary", (Integer) 1);
            this.mContentValues.put("is_primary", (Integer) 1);
        }
        if (isPrimary() || dataItem.isPrimary()) {
            this.mContentValues.put("is_primary", (Integer) 1);
        }
        ContentValues contentValues = this.mContentValues;
        if (getTimesUsed() != null) {
            iIntValue = getTimesUsed().intValue();
        } else {
            iIntValue = 0;
        }
        contentValues.put("times_used", Integer.valueOf(iIntValue + (dataItem.getTimesUsed() != null ? dataItem.getTimesUsed().intValue() : 0)));
        ContentValues contentValues2 = this.mContentValues;
        if (getLastTimeUsed() != null) {
            jLongValue = getLastTimeUsed().longValue();
        } else {
            jLongValue = 0;
        }
        contentValues2.put("last_time_used", Long.valueOf(Math.max(jLongValue, dataItem.getLastTimeUsed() != null ? dataItem.getLastTimeUsed().longValue() : 0L)));
    }

    @Override
    public boolean shouldCollapseWith(DataItem dataItem, Context context) {
        if (this.mKind == null || dataItem.getDataKind() == null) {
            return false;
        }
        return MoreContactUtils.shouldCollapse(getMimeType(), buildDataString(context, this.mKind), dataItem.getMimeType(), dataItem.buildDataString(context, dataItem.getDataKind()));
    }
}
