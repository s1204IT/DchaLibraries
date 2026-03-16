package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;
import android.content.Context;
import com.android.contacts.common.Collapser;
import com.android.contacts.common.MoreContactUtils;
import com.android.contacts.common.model.RawContactModifier;

public class DataItem implements Collapser.Collapsible<DataItem> {
    private final ContentValues mContentValues;
    protected DataKind mKind;

    protected DataItem(ContentValues values) {
        this.mContentValues = values;
    }

    public static DataItem createFrom(ContentValues values) {
        String mimeType = values.getAsString("mimetype");
        if ("vnd.android.cursor.item/group_membership".equals(mimeType)) {
            return new GroupMembershipDataItem(values);
        }
        if ("vnd.android.cursor.item/name".equals(mimeType)) {
            return new StructuredNameDataItem(values);
        }
        if ("vnd.android.cursor.item/phone_v2".equals(mimeType)) {
            return new PhoneDataItem(values);
        }
        if ("vnd.android.cursor.item/email_v2".equals(mimeType)) {
            return new EmailDataItem(values);
        }
        if ("vnd.android.cursor.item/postal-address_v2".equals(mimeType)) {
            return new StructuredPostalDataItem(values);
        }
        if ("vnd.android.cursor.item/im".equals(mimeType)) {
            return new ImDataItem(values);
        }
        if ("vnd.android.cursor.item/organization".equals(mimeType)) {
            return new OrganizationDataItem(values);
        }
        if ("vnd.android.cursor.item/nickname".equals(mimeType)) {
            return new NicknameDataItem(values);
        }
        if ("vnd.android.cursor.item/note".equals(mimeType)) {
            return new NoteDataItem(values);
        }
        if ("vnd.android.cursor.item/website".equals(mimeType)) {
            return new WebsiteDataItem(values);
        }
        if ("vnd.android.cursor.item/sip_address".equals(mimeType)) {
            return new SipAddressDataItem(values);
        }
        if ("vnd.android.cursor.item/contact_event".equals(mimeType)) {
            return new EventDataItem(values);
        }
        if ("vnd.android.cursor.item/relation".equals(mimeType)) {
            return new RelationDataItem(values);
        }
        if ("vnd.android.cursor.item/identity".equals(mimeType)) {
            return new IdentityDataItem(values);
        }
        if ("vnd.android.cursor.item/photo".equals(mimeType)) {
            return new PhotoDataItem(values);
        }
        return new DataItem(values);
    }

    public ContentValues getContentValues() {
        return this.mContentValues;
    }

    public void setRawContactId(long rawContactId) {
        this.mContentValues.put("raw_contact_id", Long.valueOf(rawContactId));
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

    public void setMimeType(String mimeType) {
        this.mContentValues.put("mimetype", mimeType);
    }

    public boolean isPrimary() {
        Integer primary = this.mContentValues.getAsInteger("is_primary");
        return (primary == null || primary.intValue() == 0) ? false : true;
    }

    public boolean isSuperPrimary() {
        Integer superPrimary = this.mContentValues.getAsInteger("is_super_primary");
        return (superPrimary == null || superPrimary.intValue() == 0) ? false : true;
    }

    public boolean hasKindTypeColumn(DataKind kind) {
        String key = kind.typeColumn;
        return (key == null || !this.mContentValues.containsKey(key) || this.mContentValues.getAsInteger(key) == null) ? false : true;
    }

    public int getKindTypeColumn(DataKind kind) {
        String key = kind.typeColumn;
        return this.mContentValues.getAsInteger(key).intValue();
    }

    public String buildDataString(Context context, DataKind kind) {
        CharSequence actionBody;
        if (kind.actionBody == null || (actionBody = kind.actionBody.inflateUsing(context, this.mContentValues)) == null) {
            return null;
        }
        return actionBody.toString();
    }

    public String buildDataStringForDisplay(Context context, DataKind kind) {
        return buildDataString(context, kind);
    }

    public void setDataKind(DataKind kind) {
        this.mKind = kind;
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
    public void collapseWith(DataItem that) {
        DataKind thisKind = getDataKind();
        DataKind thatKind = that.getDataKind();
        if ((!hasKindTypeColumn(thisKind) && that.hasKindTypeColumn(thatKind)) || (that.hasKindTypeColumn(thatKind) && RawContactModifier.getTypePrecedence(thisKind, getKindTypeColumn(thisKind)) > RawContactModifier.getTypePrecedence(thatKind, that.getKindTypeColumn(thatKind)))) {
            this.mContentValues.put(thatKind.typeColumn, Integer.valueOf(that.getKindTypeColumn(thatKind)));
            this.mKind = thatKind;
        }
        this.mKind.maxLinesForDisplay = Math.max(thisKind.maxLinesForDisplay, thatKind.maxLinesForDisplay);
        if (isSuperPrimary() || that.isSuperPrimary()) {
            this.mContentValues.put("is_super_primary", (Integer) 1);
            this.mContentValues.put("is_primary", (Integer) 1);
        }
        if (isPrimary() || that.isPrimary()) {
            this.mContentValues.put("is_primary", (Integer) 1);
        }
        this.mContentValues.put("times_used", Integer.valueOf((getTimesUsed() == null ? 0 : getTimesUsed().intValue()) + (that.getTimesUsed() != null ? that.getTimesUsed().intValue() : 0)));
        this.mContentValues.put("last_time_used", Long.valueOf(Math.max(getLastTimeUsed() == null ? 0L : getLastTimeUsed().longValue(), that.getLastTimeUsed() != null ? that.getLastTimeUsed().longValue() : 0L)));
    }

    @Override
    public boolean shouldCollapseWith(DataItem t, Context context) {
        if (this.mKind == null || t.getDataKind() == null) {
            return false;
        }
        return MoreContactUtils.shouldCollapse(getMimeType(), buildDataString(context, this.mKind), t.getMimeType(), t.buildDataString(context, t.getDataKind()));
    }
}
