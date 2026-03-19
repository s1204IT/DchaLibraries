package com.android.contacts.model;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ValuesDelta implements Parcelable {
    protected ContentValues mAfter;
    protected ContentValues mBefore;
    private boolean mFromTemplate;
    protected String mIdColumn = "_id";
    protected static int sNextInsertId = -1;
    public static final Parcelable.Creator<ValuesDelta> CREATOR = new Parcelable.Creator<ValuesDelta>() {
        @Override
        public ValuesDelta createFromParcel(Parcel parcel) {
            ValuesDelta valuesDelta = new ValuesDelta();
            valuesDelta.readFromParcel(parcel);
            return valuesDelta;
        }

        @Override
        public ValuesDelta[] newArray(int i) {
            return new ValuesDelta[i];
        }
    };

    protected ValuesDelta() {
    }

    public static ValuesDelta fromBefore(ContentValues contentValues) {
        ValuesDelta valuesDelta = new ValuesDelta();
        valuesDelta.mBefore = contentValues;
        valuesDelta.mAfter = new ContentValues();
        return valuesDelta;
    }

    public static ValuesDelta fromAfter(ContentValues contentValues) {
        ValuesDelta valuesDelta = new ValuesDelta();
        valuesDelta.mBefore = null;
        valuesDelta.mAfter = contentValues;
        ContentValues contentValues2 = valuesDelta.mAfter;
        String str = valuesDelta.mIdColumn;
        int i = sNextInsertId;
        sNextInsertId = i - 1;
        contentValues2.put(str, Integer.valueOf(i));
        return valuesDelta;
    }

    public ContentValues getAfter() {
        return this.mAfter;
    }

    public ContentValues getBefore() {
        return this.mBefore;
    }

    public boolean containsKey(String str) {
        return (this.mAfter != null && this.mAfter.containsKey(str)) || (this.mBefore != null && this.mBefore.containsKey(str));
    }

    public String getAsString(String str) {
        if (this.mAfter != null && this.mAfter.containsKey(str)) {
            return this.mAfter.getAsString(str);
        }
        if (this.mBefore != null && this.mBefore.containsKey(str)) {
            return this.mBefore.getAsString(str);
        }
        return null;
    }

    public byte[] getAsByteArray(String str) {
        if (this.mAfter != null && this.mAfter.containsKey(str)) {
            return this.mAfter.getAsByteArray(str);
        }
        if (this.mBefore != null && this.mBefore.containsKey(str)) {
            return this.mBefore.getAsByteArray(str);
        }
        return null;
    }

    public Long getAsLong(String str) {
        if (this.mAfter != null && this.mAfter.containsKey(str)) {
            return this.mAfter.getAsLong(str);
        }
        if (this.mBefore != null && this.mBefore.containsKey(str)) {
            return this.mBefore.getAsLong(str);
        }
        return null;
    }

    public Integer getAsInteger(String str) {
        return getAsInteger(str, null);
    }

    public Integer getAsInteger(String str, Integer num) {
        if (this.mAfter != null && this.mAfter.containsKey(str)) {
            return this.mAfter.getAsInteger(str);
        }
        if (this.mBefore != null && this.mBefore.containsKey(str)) {
            return this.mBefore.getAsInteger(str);
        }
        return num;
    }

    public boolean isChanged(String str) {
        if (this.mAfter == null || !this.mAfter.containsKey(str)) {
            return false;
        }
        Object obj = this.mAfter.get(str);
        if (this.mBefore.get(str) == null) {
            return obj != null;
        }
        return !r4.equals(obj);
    }

    public String getMimetype() {
        return getAsString("mimetype");
    }

    public Long getId() {
        return getAsLong(this.mIdColumn);
    }

    public void setIdColumn(String str) {
        this.mIdColumn = str;
    }

    public boolean isPrimary() {
        Long asLong = getAsLong("is_primary");
        return (asLong == null || asLong.longValue() == 0) ? false : true;
    }

    public void setFromTemplate(boolean z) {
        this.mFromTemplate = z;
    }

    public boolean isFromTemplate() {
        return this.mFromTemplate;
    }

    public boolean isSuperPrimary() {
        Long asLong = getAsLong("is_super_primary");
        return (asLong == null || asLong.longValue() == 0) ? false : true;
    }

    public boolean beforeExists() {
        return this.mBefore != null && this.mBefore.containsKey(this.mIdColumn);
    }

    public boolean isVisible() {
        return this.mAfter != null;
    }

    public boolean isDelete() {
        return beforeExists() && this.mAfter == null;
    }

    public boolean isTransient() {
        return this.mBefore == null && this.mAfter == null;
    }

    public boolean isUpdate() {
        if (!beforeExists() || this.mAfter == null || this.mAfter.size() == 0) {
            return false;
        }
        for (String str : this.mAfter.keySet()) {
            Object obj = this.mAfter.get(str);
            Object obj2 = this.mBefore.get(str);
            if (obj2 == null) {
                if (obj != null) {
                    return true;
                }
            } else if (!obj2.equals(obj)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNoop() {
        return beforeExists() && this.mAfter != null && this.mAfter.size() == 0;
    }

    public boolean isInsert() {
        return (beforeExists() || this.mAfter == null) ? false : true;
    }

    public void markDeleted() {
        this.mAfter = null;
    }

    private void ensureUpdate() {
        if (this.mAfter == null) {
            this.mAfter = new ContentValues();
        }
    }

    public void put(String str, String str2) {
        ensureUpdate();
        this.mAfter.put(str, str2);
    }

    public void put(String str, byte[] bArr) {
        ensureUpdate();
        this.mAfter.put(str, bArr);
    }

    public void put(String str, int i) {
        ensureUpdate();
        this.mAfter.put(str, Integer.valueOf(i));
    }

    public void put(String str, long j) {
        ensureUpdate();
        this.mAfter.put(str, Long.valueOf(j));
    }

    public void putNull(String str) {
        ensureUpdate();
        this.mAfter.putNull(str);
    }

    public void copyStringFrom(ValuesDelta valuesDelta, String str) {
        ensureUpdate();
        if (containsKey(str) || valuesDelta.containsKey(str)) {
            put(str, valuesDelta.getAsString(str));
        }
    }

    public Set<String> keySet() {
        HashSet hashSetNewHashSet = Sets.newHashSet();
        if (this.mBefore != null) {
            Iterator<Map.Entry<String, Object>> it = this.mBefore.valueSet().iterator();
            while (it.hasNext()) {
                hashSetNewHashSet.add(it.next().getKey());
            }
        }
        if (this.mAfter != null) {
            Iterator<Map.Entry<String, Object>> it2 = this.mAfter.valueSet().iterator();
            while (it2.hasNext()) {
                hashSetNewHashSet.add(it2.next().getKey());
            }
        }
        return hashSetNewHashSet;
    }

    public ContentValues getCompleteValues() {
        ContentValues contentValues = new ContentValues();
        if (this.mBefore != null) {
            contentValues.putAll(this.mBefore);
        }
        if (this.mAfter != null) {
            contentValues.putAll(this.mAfter);
        }
        if (contentValues.containsKey("data1")) {
            contentValues.remove("group_sourceid");
        }
        return contentValues;
    }

    public static ValuesDelta mergeAfter(ValuesDelta valuesDelta, ValuesDelta valuesDelta2) {
        if (valuesDelta == null && (valuesDelta2.isDelete() || valuesDelta2.isTransient())) {
            return null;
        }
        if (valuesDelta == null) {
            valuesDelta = new ValuesDelta();
        }
        if (!valuesDelta.beforeExists()) {
            valuesDelta.mAfter = valuesDelta2.getCompleteValues();
        } else {
            valuesDelta.mAfter = valuesDelta2.mAfter;
        }
        return valuesDelta;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ValuesDelta)) {
            return false;
        }
        ValuesDelta valuesDelta = (ValuesDelta) obj;
        return subsetEquals(valuesDelta) && valuesDelta.subsetEquals(this);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public void toString(StringBuilder sb) {
        sb.append("{ ");
        sb.append("IdColumn=");
        sb.append(this.mIdColumn);
        sb.append(", FromTemplate=");
        sb.append(this.mFromTemplate);
        sb.append(", ");
        for (String str : keySet()) {
            sb.append(str);
            sb.append("=");
            sb.append(getAsString(str));
            sb.append(", ");
        }
        sb.append("}");
    }

    public boolean subsetEquals(ValuesDelta valuesDelta) {
        for (String str : keySet()) {
            String asString = getAsString(str);
            String asString2 = valuesDelta.getAsString(str);
            if (asString == null) {
                if (asString2 != null) {
                    return false;
                }
            } else if (!asString.equals(asString2)) {
                return false;
            }
        }
        return true;
    }

    public ContentProviderOperation.Builder buildDiff(Uri uri) {
        return buildDiffHelper(uri);
    }

    public BuilderWrapper buildDiffWrapper(Uri uri) {
        ContentProviderOperation.Builder builderBuildDiffHelper = buildDiffHelper(uri);
        if (isInsert()) {
            return new BuilderWrapper(builderBuildDiffHelper, 1);
        }
        if (isDelete()) {
            return new BuilderWrapper(builderBuildDiffHelper, 3);
        }
        if (isUpdate()) {
            return new BuilderWrapper(builderBuildDiffHelper, 2);
        }
        return null;
    }

    private ContentProviderOperation.Builder buildDiffHelper(Uri uri) {
        ContentProviderOperation.Builder builderNewInsert = null;
        if (isInsert()) {
            this.mAfter.remove(this.mIdColumn);
            builderNewInsert = ContentProviderOperation.newInsert(uri);
            builderNewInsert.withValues(this.mAfter);
        } else {
            if (isDelete()) {
                ContentProviderOperation.Builder builderNewDelete = ContentProviderOperation.newDelete(uri);
                builderNewDelete.withSelection(this.mIdColumn + "=" + getId(), null);
                return builderNewDelete;
            }
            if (isUpdate()) {
                ContentProviderOperation.Builder builderNewUpdate = ContentProviderOperation.newUpdate(uri);
                builderNewUpdate.withSelection(this.mIdColumn + "=" + getId(), null);
                builderNewUpdate.withValues(this.mAfter);
                return builderNewUpdate;
            }
        }
        return builderNewInsert;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mBefore, i);
        parcel.writeParcelable(this.mAfter, i);
        parcel.writeString(this.mIdColumn);
    }

    public void readFromParcel(Parcel parcel) {
        ClassLoader classLoader = getClass().getClassLoader();
        this.mBefore = (ContentValues) parcel.readParcelable(classLoader);
        this.mAfter = (ContentValues) parcel.readParcelable(classLoader);
        this.mIdColumn = parcel.readString();
    }

    public void setGroupRowId(long j) {
        put("data1", j);
    }

    public Long getGroupRowId() {
        return getAsLong("data1");
    }

    public void setPhoto(byte[] bArr) {
        put("data15", bArr);
    }

    public byte[] getPhoto() {
        return getAsByteArray("data15");
    }

    public void setSuperPrimary(boolean z) {
        if (z) {
            put("is_super_primary", 1);
        } else {
            put("is_super_primary", 0);
        }
    }

    public void setPhoneticFamilyName(String str) {
        put("data9", str);
    }

    public void setPhoneticMiddleName(String str) {
        put("data8", str);
    }

    public void setPhoneticGivenName(String str) {
        put("data7", str);
    }

    public String getPhoneticFamilyName() {
        return getAsString("data9");
    }

    public String getPhoneticMiddleName() {
        return getAsString("data8");
    }

    public String getPhoneticGivenName() {
        return getAsString("data7");
    }

    public String getDisplayName() {
        return getAsString("data1");
    }

    public void setDisplayName(String str) {
        if (str == null) {
            putNull("data1");
        } else {
            put("data1", str);
        }
    }

    public void copyStructuredNameFieldsFrom(ValuesDelta valuesDelta) {
        copyStringFrom(valuesDelta, "data1");
        copyStringFrom(valuesDelta, "data2");
        copyStringFrom(valuesDelta, "data3");
        copyStringFrom(valuesDelta, "data4");
        copyStringFrom(valuesDelta, "data5");
        copyStringFrom(valuesDelta, "data6");
        copyStringFrom(valuesDelta, "data7");
        copyStringFrom(valuesDelta, "data8");
        copyStringFrom(valuesDelta, "data9");
        copyStringFrom(valuesDelta, "data10");
        copyStringFrom(valuesDelta, "data11");
    }

    public String getPhoneNumber() {
        return getAsString("data1");
    }

    public String getPhoneNormalizedNumber() {
        return getAsString("data4");
    }

    public boolean hasPhoneType() {
        return getPhoneType() != null;
    }

    public Integer getPhoneType() {
        return getAsInteger("data2");
    }

    public String getPhoneLabel() {
        return getAsString("data3");
    }

    public String getEmailData() {
        return getAsString("data1");
    }

    public boolean hasEmailType() {
        return getEmailType() != null;
    }

    public Integer getEmailType() {
        return getAsInteger("data2");
    }

    public String getEmailLabel() {
        return getAsString("data3");
    }
}
