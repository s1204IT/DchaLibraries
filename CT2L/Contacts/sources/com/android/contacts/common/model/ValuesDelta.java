package com.android.contacts.common.model;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.common.collect.Sets;
import java.util.HashSet;
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
        public ValuesDelta createFromParcel(Parcel in) {
            ValuesDelta values = new ValuesDelta();
            values.readFromParcel(in);
            return values;
        }

        @Override
        public ValuesDelta[] newArray(int size) {
            return new ValuesDelta[size];
        }
    };

    protected ValuesDelta() {
    }

    public static ValuesDelta fromBefore(ContentValues before) {
        ValuesDelta entry = new ValuesDelta();
        entry.mBefore = before;
        entry.mAfter = new ContentValues();
        return entry;
    }

    public static ValuesDelta fromAfter(ContentValues after) {
        ValuesDelta entry = new ValuesDelta();
        entry.mBefore = null;
        entry.mAfter = after;
        ContentValues contentValues = entry.mAfter;
        String str = entry.mIdColumn;
        int i = sNextInsertId;
        sNextInsertId = i - 1;
        contentValues.put(str, Integer.valueOf(i));
        return entry;
    }

    public ContentValues getAfter() {
        return this.mAfter;
    }

    public boolean containsKey(String key) {
        return (this.mAfter != null && this.mAfter.containsKey(key)) || (this.mBefore != null && this.mBefore.containsKey(key));
    }

    public String getAsString(String key) {
        if (this.mAfter != null && this.mAfter.containsKey(key)) {
            return this.mAfter.getAsString(key);
        }
        if (this.mBefore != null && this.mBefore.containsKey(key)) {
            return this.mBefore.getAsString(key);
        }
        return null;
    }

    public byte[] getAsByteArray(String key) {
        if (this.mAfter != null && this.mAfter.containsKey(key)) {
            return this.mAfter.getAsByteArray(key);
        }
        if (this.mBefore != null && this.mBefore.containsKey(key)) {
            return this.mBefore.getAsByteArray(key);
        }
        return null;
    }

    public Long getAsLong(String key) {
        if (this.mAfter != null && this.mAfter.containsKey(key)) {
            return this.mAfter.getAsLong(key);
        }
        if (this.mBefore != null && this.mBefore.containsKey(key)) {
            return this.mBefore.getAsLong(key);
        }
        return null;
    }

    public Integer getAsInteger(String key) {
        return getAsInteger(key, null);
    }

    public Integer getAsInteger(String key, Integer defaultValue) {
        if (this.mAfter != null && this.mAfter.containsKey(key)) {
            return this.mAfter.getAsInteger(key);
        }
        if (this.mBefore != null && this.mBefore.containsKey(key)) {
            return this.mBefore.getAsInteger(key);
        }
        return defaultValue;
    }

    public String getMimetype() {
        return getAsString("mimetype");
    }

    public Long getId() {
        return getAsLong(this.mIdColumn);
    }

    public void setIdColumn(String idColumn) {
        this.mIdColumn = idColumn;
    }

    public boolean isPrimary() {
        Long isPrimary = getAsLong("is_primary");
        return (isPrimary == null || isPrimary.longValue() == 0) ? false : true;
    }

    public void setFromTemplate(boolean isFromTemplate) {
        this.mFromTemplate = isFromTemplate;
    }

    public boolean isFromTemplate() {
        return this.mFromTemplate;
    }

    public boolean isSuperPrimary() {
        Long isSuperPrimary = getAsLong("is_super_primary");
        return (isSuperPrimary == null || isSuperPrimary.longValue() == 0) ? false : true;
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
        for (String key : this.mAfter.keySet()) {
            Object newValue = this.mAfter.get(key);
            Object oldValue = this.mBefore.get(key);
            if (oldValue == null) {
                if (newValue != null) {
                    return true;
                }
            } else if (!oldValue.equals(newValue)) {
                return true;
            }
        }
        return false;
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

    public void put(String key, String value) {
        ensureUpdate();
        this.mAfter.put(key, value);
    }

    public void put(String key, byte[] value) {
        ensureUpdate();
        this.mAfter.put(key, value);
    }

    public void put(String key, int value) {
        ensureUpdate();
        this.mAfter.put(key, Integer.valueOf(value));
    }

    public void put(String key, long value) {
        ensureUpdate();
        this.mAfter.put(key, Long.valueOf(value));
    }

    public void putNull(String key) {
        ensureUpdate();
        this.mAfter.putNull(key);
    }

    public void copyStringFrom(ValuesDelta from, String key) {
        ensureUpdate();
        put(key, from.getAsString(key));
    }

    public Set<String> keySet() {
        HashSet<String> keys = Sets.newHashSet();
        if (this.mBefore != null) {
            for (Map.Entry<String, Object> entry : this.mBefore.valueSet()) {
                keys.add(entry.getKey());
            }
        }
        if (this.mAfter != null) {
            for (Map.Entry<String, Object> entry2 : this.mAfter.valueSet()) {
                keys.add(entry2.getKey());
            }
        }
        return keys;
    }

    public ContentValues getCompleteValues() {
        ContentValues values = new ContentValues();
        if (this.mBefore != null) {
            values.putAll(this.mBefore);
        }
        if (this.mAfter != null) {
            values.putAll(this.mAfter);
        }
        if (values.containsKey("data1")) {
            values.remove("group_sourceid");
        }
        return values;
    }

    public static ValuesDelta mergeAfter(ValuesDelta local, ValuesDelta remote) {
        if (local == null && (remote.isDelete() || remote.isTransient())) {
            return null;
        }
        if (local == null) {
            local = new ValuesDelta();
        }
        if (!local.beforeExists()) {
            local.mAfter = remote.getCompleteValues();
            return local;
        }
        local.mAfter = remote.mAfter;
        return local;
    }

    public boolean equals(Object object) {
        if (!(object instanceof ValuesDelta)) {
            return false;
        }
        ValuesDelta other = (ValuesDelta) object;
        return subsetEquals(other) && other.subsetEquals(this);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder);
        return builder.toString();
    }

    public void toString(StringBuilder builder) {
        builder.append("{ ");
        builder.append("IdColumn=");
        builder.append(this.mIdColumn);
        builder.append(", FromTemplate=");
        builder.append(this.mFromTemplate);
        builder.append(", ");
        for (String key : keySet()) {
            builder.append(key);
            builder.append("=");
            builder.append(getAsString(key));
            builder.append(", ");
        }
        builder.append("}");
    }

    public boolean subsetEquals(ValuesDelta other) {
        for (String key : keySet()) {
            String ourValue = getAsString(key);
            String theirValue = other.getAsString(key);
            if (ourValue == null) {
                if (theirValue != null) {
                    return false;
                }
            } else if (!ourValue.equals(theirValue)) {
                return false;
            }
        }
        return true;
    }

    public ContentProviderOperation.Builder buildDiff(Uri targetUri) {
        if (isInsert()) {
            this.mAfter.remove(this.mIdColumn);
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(targetUri);
            builder.withValues(this.mAfter);
            return builder;
        }
        if (isDelete()) {
            ContentProviderOperation.Builder builder2 = ContentProviderOperation.newDelete(targetUri);
            builder2.withSelection(this.mIdColumn + "=" + getId(), null);
            return builder2;
        }
        if (!isUpdate()) {
            return null;
        }
        ContentProviderOperation.Builder builder3 = ContentProviderOperation.newUpdate(targetUri);
        builder3.withSelection(this.mIdColumn + "=" + getId(), null);
        builder3.withValues(this.mAfter);
        return builder3;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mBefore, flags);
        dest.writeParcelable(this.mAfter, flags);
        dest.writeString(this.mIdColumn);
    }

    public void readFromParcel(Parcel source) {
        ClassLoader loader = getClass().getClassLoader();
        this.mBefore = (ContentValues) source.readParcelable(loader);
        this.mAfter = (ContentValues) source.readParcelable(loader);
        this.mIdColumn = source.readString();
    }

    public void setGroupRowId(long groupId) {
        put("data1", groupId);
    }

    public Long getGroupRowId() {
        return getAsLong("data1");
    }

    public void setPhoto(byte[] value) {
        put("data15", value);
    }

    public byte[] getPhoto() {
        return getAsByteArray("data15");
    }

    public void setSuperPrimary(boolean val) {
        if (val) {
            put("is_super_primary", 1);
        } else {
            put("is_super_primary", 0);
        }
    }

    public void setPhoneticFamilyName(String value) {
        put("data9", value);
    }

    public void setPhoneticMiddleName(String value) {
        put("data8", value);
    }

    public void setPhoneticGivenName(String value) {
        put("data7", value);
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

    public void setDisplayName(String name) {
        if (name == null) {
            putNull("data1");
        } else {
            put("data1", name);
        }
    }

    public void copyStructuredNameFieldsFrom(ValuesDelta name) {
        copyStringFrom(name, "data1");
        copyStringFrom(name, "data2");
        copyStringFrom(name, "data3");
        copyStringFrom(name, "data4");
        copyStringFrom(name, "data5");
        copyStringFrom(name, "data6");
        copyStringFrom(name, "data7");
        copyStringFrom(name, "data8");
        copyStringFrom(name, "data9");
        copyStringFrom(name, "data10");
        copyStringFrom(name, "data11");
    }

    public String getPhoneNumber() {
        return getAsString("data1");
    }

    public String getPhoneNormalizedNumber() {
        return getAsString("data4");
    }

    public boolean phoneHasType() {
        return containsKey("data2");
    }

    public int getPhoneType() {
        return getAsInteger("data2").intValue();
    }

    public String getPhoneLabel() {
        return getAsString("data3");
    }

    public String getEmailData() {
        return getAsString("data1");
    }

    public boolean emailHasType() {
        return containsKey("data2");
    }

    public int getEmailType() {
        return getAsInteger("data2").intValue();
    }

    public String getEmailLabel() {
        return getAsString("data3");
    }
}
