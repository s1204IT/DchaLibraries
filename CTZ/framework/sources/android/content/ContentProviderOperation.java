package android.content;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ContentProviderOperation implements Parcelable {
    public static final Parcelable.Creator<ContentProviderOperation> CREATOR = new Parcelable.Creator<ContentProviderOperation>() {
        @Override
        public ContentProviderOperation createFromParcel(Parcel parcel) {
            return new ContentProviderOperation(parcel);
        }

        @Override
        public ContentProviderOperation[] newArray(int i) {
            return new ContentProviderOperation[i];
        }
    };
    private static final String TAG = "ContentProviderOperation";
    public static final int TYPE_ASSERT = 4;
    public static final int TYPE_DELETE = 3;
    public static final int TYPE_INSERT = 1;
    public static final int TYPE_UPDATE = 2;
    private final Integer mExpectedCount;
    private final String mSelection;
    private final String[] mSelectionArgs;
    private final Map<Integer, Integer> mSelectionArgsBackReferences;
    private final int mType;
    private final Uri mUri;
    private final ContentValues mValues;
    private final ContentValues mValuesBackReferences;
    private final boolean mYieldAllowed;

    private ContentProviderOperation(Builder builder) {
        this.mType = builder.mType;
        this.mUri = builder.mUri;
        this.mValues = builder.mValues;
        this.mSelection = builder.mSelection;
        this.mSelectionArgs = builder.mSelectionArgs;
        this.mExpectedCount = builder.mExpectedCount;
        this.mSelectionArgsBackReferences = builder.mSelectionArgsBackReferences;
        this.mValuesBackReferences = builder.mValuesBackReferences;
        this.mYieldAllowed = builder.mYieldAllowed;
    }

    private ContentProviderOperation(Parcel parcel) {
        ContentValues contentValuesCreateFromParcel;
        this.mType = parcel.readInt();
        this.mUri = Uri.CREATOR.createFromParcel(parcel);
        this.mValues = parcel.readInt() != 0 ? ContentValues.CREATOR.createFromParcel(parcel) : null;
        this.mSelection = parcel.readInt() != 0 ? parcel.readString() : null;
        this.mSelectionArgs = parcel.readInt() != 0 ? parcel.readStringArray() : null;
        this.mExpectedCount = parcel.readInt() != 0 ? Integer.valueOf(parcel.readInt()) : null;
        if (parcel.readInt() != 0) {
            contentValuesCreateFromParcel = ContentValues.CREATOR.createFromParcel(parcel);
        } else {
            contentValuesCreateFromParcel = null;
        }
        this.mValuesBackReferences = contentValuesCreateFromParcel;
        this.mSelectionArgsBackReferences = parcel.readInt() != 0 ? new HashMap() : null;
        if (this.mSelectionArgsBackReferences != null) {
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                this.mSelectionArgsBackReferences.put(Integer.valueOf(parcel.readInt()), Integer.valueOf(parcel.readInt()));
            }
        }
        this.mYieldAllowed = parcel.readInt() != 0;
    }

    public ContentProviderOperation(ContentProviderOperation contentProviderOperation, Uri uri) {
        this.mType = contentProviderOperation.mType;
        this.mUri = uri;
        this.mValues = contentProviderOperation.mValues;
        this.mSelection = contentProviderOperation.mSelection;
        this.mSelectionArgs = contentProviderOperation.mSelectionArgs;
        this.mExpectedCount = contentProviderOperation.mExpectedCount;
        this.mSelectionArgsBackReferences = contentProviderOperation.mSelectionArgsBackReferences;
        this.mValuesBackReferences = contentProviderOperation.mValuesBackReferences;
        this.mYieldAllowed = contentProviderOperation.mYieldAllowed;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mType);
        Uri.writeToParcel(parcel, this.mUri);
        if (this.mValues != null) {
            parcel.writeInt(1);
            this.mValues.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        if (this.mSelection != null) {
            parcel.writeInt(1);
            parcel.writeString(this.mSelection);
        } else {
            parcel.writeInt(0);
        }
        if (this.mSelectionArgs != null) {
            parcel.writeInt(1);
            parcel.writeStringArray(this.mSelectionArgs);
        } else {
            parcel.writeInt(0);
        }
        if (this.mExpectedCount != null) {
            parcel.writeInt(1);
            parcel.writeInt(this.mExpectedCount.intValue());
        } else {
            parcel.writeInt(0);
        }
        if (this.mValuesBackReferences != null) {
            parcel.writeInt(1);
            this.mValuesBackReferences.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        if (this.mSelectionArgsBackReferences != null) {
            parcel.writeInt(1);
            parcel.writeInt(this.mSelectionArgsBackReferences.size());
            for (Map.Entry<Integer, Integer> entry : this.mSelectionArgsBackReferences.entrySet()) {
                parcel.writeInt(entry.getKey().intValue());
                parcel.writeInt(entry.getValue().intValue());
            }
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.mYieldAllowed ? 1 : 0);
    }

    public static Builder newInsert(Uri uri) {
        return new Builder(1, uri);
    }

    public static Builder newUpdate(Uri uri) {
        return new Builder(2, uri);
    }

    public static Builder newDelete(Uri uri) {
        return new Builder(3, uri);
    }

    public static Builder newAssertQuery(Uri uri) {
        return new Builder(4, uri);
    }

    public Uri getUri() {
        return this.mUri;
    }

    public boolean isYieldAllowed() {
        return this.mYieldAllowed;
    }

    public int getType() {
        return this.mType;
    }

    public boolean isInsert() {
        return this.mType == 1;
    }

    public boolean isDelete() {
        return this.mType == 3;
    }

    public boolean isUpdate() {
        return this.mType == 2;
    }

    public boolean isAssertQuery() {
        return this.mType == 4;
    }

    public boolean isWriteOperation() {
        return this.mType == 3 || this.mType == 1 || this.mType == 2;
    }

    public boolean isReadOperation() {
        return this.mType == 4;
    }

    public ContentProviderResult apply(ContentProvider contentProvider, ContentProviderResult[] contentProviderResultArr, int i) throws OperationApplicationException {
        int iUpdate;
        ContentValues contentValuesResolveValueBackReferences = resolveValueBackReferences(contentProviderResultArr, i);
        String[] strArrResolveSelectionArgsBackReferences = resolveSelectionArgsBackReferences(contentProviderResultArr, i);
        if (this.mType == 1) {
            Uri uriInsert = contentProvider.insert(this.mUri, contentValuesResolveValueBackReferences);
            if (uriInsert == null) {
                throw new OperationApplicationException("insert failed");
            }
            return new ContentProviderResult(uriInsert);
        }
        if (this.mType == 3) {
            iUpdate = contentProvider.delete(this.mUri, this.mSelection, strArrResolveSelectionArgsBackReferences);
        } else if (this.mType == 2) {
            iUpdate = contentProvider.update(this.mUri, contentValuesResolveValueBackReferences, this.mSelection, strArrResolveSelectionArgsBackReferences);
        } else if (this.mType == 4) {
            String[] strArr = null;
            if (contentValuesResolveValueBackReferences != null) {
                ArrayList arrayList = new ArrayList();
                Iterator<Map.Entry<String, Object>> it = contentValuesResolveValueBackReferences.valueSet().iterator();
                while (it.hasNext()) {
                    arrayList.add(it.next().getKey());
                }
                strArr = (String[]) arrayList.toArray(new String[arrayList.size()]);
            }
            Cursor cursorQuery = contentProvider.query(this.mUri, strArr, this.mSelection, strArrResolveSelectionArgsBackReferences, null);
            try {
                int count = cursorQuery.getCount();
                if (strArr != null) {
                    while (cursorQuery.moveToNext()) {
                        for (int i2 = 0; i2 < strArr.length; i2++) {
                            String string = cursorQuery.getString(i2);
                            String asString = contentValuesResolveValueBackReferences.getAsString(strArr[i2]);
                            if (!TextUtils.equals(string, asString)) {
                                Log.e(TAG, toString());
                                throw new OperationApplicationException("Found value " + string + " when expected " + asString + " for column " + strArr[i2]);
                            }
                        }
                    }
                }
                cursorQuery.close();
                iUpdate = count;
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        } else {
            Log.e(TAG, toString());
            throw new IllegalStateException("bad type, " + this.mType);
        }
        if (this.mExpectedCount != null && this.mExpectedCount.intValue() != iUpdate) {
            Log.e(TAG, toString());
            throw new OperationApplicationException("wrong number of rows: " + iUpdate);
        }
        return new ContentProviderResult(iUpdate);
    }

    public ContentValues resolveValueBackReferences(ContentProviderResult[] contentProviderResultArr, int i) {
        ContentValues contentValues;
        if (this.mValuesBackReferences == null) {
            return this.mValues;
        }
        if (this.mValues == null) {
            contentValues = new ContentValues();
        } else {
            contentValues = new ContentValues(this.mValues);
        }
        Iterator<Map.Entry<String, Object>> it = this.mValuesBackReferences.valueSet().iterator();
        while (it.hasNext()) {
            String key = it.next().getKey();
            Integer asInteger = this.mValuesBackReferences.getAsInteger(key);
            if (asInteger == null) {
                Log.e(TAG, toString());
                throw new IllegalArgumentException("values backref " + key + " is not an integer");
            }
            contentValues.put(key, Long.valueOf(backRefToValue(contentProviderResultArr, i, asInteger)));
        }
        return contentValues;
    }

    public String[] resolveSelectionArgsBackReferences(ContentProviderResult[] contentProviderResultArr, int i) {
        if (this.mSelectionArgsBackReferences == null) {
            return this.mSelectionArgs;
        }
        String[] strArr = new String[this.mSelectionArgs.length];
        System.arraycopy(this.mSelectionArgs, 0, strArr, 0, this.mSelectionArgs.length);
        for (Map.Entry<Integer, Integer> entry : this.mSelectionArgsBackReferences.entrySet()) {
            strArr[entry.getKey().intValue()] = String.valueOf(backRefToValue(contentProviderResultArr, i, Integer.valueOf(entry.getValue().intValue())));
        }
        return strArr;
    }

    public String toString() {
        return "mType: " + this.mType + ", mUri: " + this.mUri + ", mSelection: " + this.mSelection + ", mExpectedCount: " + this.mExpectedCount + ", mYieldAllowed: " + this.mYieldAllowed + ", mValues: " + this.mValues + ", mValuesBackReferences: " + this.mValuesBackReferences + ", mSelectionArgsBackReferences: " + this.mSelectionArgsBackReferences;
    }

    private long backRefToValue(ContentProviderResult[] contentProviderResultArr, int i, Integer num) {
        if (num.intValue() >= i) {
            Log.e(TAG, toString());
            throw new ArrayIndexOutOfBoundsException("asked for back ref " + num + " but there are only " + i + " back refs");
        }
        ContentProviderResult contentProviderResult = contentProviderResultArr[num.intValue()];
        if (contentProviderResult.uri != null) {
            return ContentUris.parseId(contentProviderResult.uri);
        }
        return contentProviderResult.count.intValue();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static class Builder {
        private Integer mExpectedCount;
        private String mSelection;
        private String[] mSelectionArgs;
        private Map<Integer, Integer> mSelectionArgsBackReferences;
        private final int mType;
        private final Uri mUri;
        private ContentValues mValues;
        private ContentValues mValuesBackReferences;
        private boolean mYieldAllowed;

        private Builder(int i, Uri uri) {
            if (uri == null) {
                throw new IllegalArgumentException("uri must not be null");
            }
            this.mType = i;
            this.mUri = uri;
        }

        public ContentProviderOperation build() {
            if (this.mType == 2 && ((this.mValues == null || this.mValues.isEmpty()) && (this.mValuesBackReferences == null || this.mValuesBackReferences.isEmpty()))) {
                throw new IllegalArgumentException("Empty values");
            }
            if (this.mType == 4 && ((this.mValues == null || this.mValues.isEmpty()) && ((this.mValuesBackReferences == null || this.mValuesBackReferences.isEmpty()) && this.mExpectedCount == null))) {
                throw new IllegalArgumentException("Empty values");
            }
            return new ContentProviderOperation(this);
        }

        public Builder withValueBackReferences(ContentValues contentValues) {
            if (this.mType != 1 && this.mType != 2 && this.mType != 4) {
                throw new IllegalArgumentException("only inserts, updates, and asserts can have value back-references");
            }
            this.mValuesBackReferences = contentValues;
            return this;
        }

        public Builder withValueBackReference(String str, int i) {
            if (this.mType != 1 && this.mType != 2 && this.mType != 4) {
                throw new IllegalArgumentException("only inserts, updates, and asserts can have value back-references");
            }
            if (this.mValuesBackReferences == null) {
                this.mValuesBackReferences = new ContentValues();
            }
            this.mValuesBackReferences.put(str, Integer.valueOf(i));
            return this;
        }

        public Builder withSelectionBackReference(int i, int i2) {
            if (this.mType != 2 && this.mType != 3 && this.mType != 4) {
                throw new IllegalArgumentException("only updates, deletes, and asserts can have selection back-references");
            }
            if (this.mSelectionArgsBackReferences == null) {
                this.mSelectionArgsBackReferences = new HashMap();
            }
            this.mSelectionArgsBackReferences.put(Integer.valueOf(i), Integer.valueOf(i2));
            return this;
        }

        public Builder withValues(ContentValues contentValues) {
            if (this.mType != 1 && this.mType != 2 && this.mType != 4) {
                throw new IllegalArgumentException("only inserts, updates, and asserts can have values");
            }
            if (this.mValues == null) {
                this.mValues = new ContentValues();
            }
            this.mValues.putAll(contentValues);
            return this;
        }

        public Builder withValue(String str, Object obj) {
            if (this.mType != 1 && this.mType != 2 && this.mType != 4) {
                throw new IllegalArgumentException("only inserts and updates can have values");
            }
            if (this.mValues == null) {
                this.mValues = new ContentValues();
            }
            if (obj == null) {
                this.mValues.putNull(str);
            } else if (obj instanceof String) {
                this.mValues.put(str, (String) obj);
            } else if (obj instanceof Byte) {
                this.mValues.put(str, (Byte) obj);
            } else if (obj instanceof Short) {
                this.mValues.put(str, (Short) obj);
            } else if (obj instanceof Integer) {
                this.mValues.put(str, (Integer) obj);
            } else if (obj instanceof Long) {
                this.mValues.put(str, (Long) obj);
            } else if (obj instanceof Float) {
                this.mValues.put(str, (Float) obj);
            } else if (obj instanceof Double) {
                this.mValues.put(str, (Double) obj);
            } else if (obj instanceof Boolean) {
                this.mValues.put(str, (Boolean) obj);
            } else if (obj instanceof byte[]) {
                this.mValues.put(str, (byte[]) obj);
            } else {
                throw new IllegalArgumentException("bad value type: " + obj.getClass().getName());
            }
            return this;
        }

        public Builder withSelection(String str, String[] strArr) {
            if (this.mType != 2 && this.mType != 3 && this.mType != 4) {
                throw new IllegalArgumentException("only updates, deletes, and asserts can have selections");
            }
            this.mSelection = str;
            if (strArr == null) {
                this.mSelectionArgs = null;
            } else {
                this.mSelectionArgs = new String[strArr.length];
                System.arraycopy(strArr, 0, this.mSelectionArgs, 0, strArr.length);
            }
            return this;
        }

        public Builder withExpectedCount(int i) {
            if (this.mType != 2 && this.mType != 3 && this.mType != 4) {
                throw new IllegalArgumentException("only updates, deletes, and asserts can have expected counts");
            }
            this.mExpectedCount = Integer.valueOf(i);
            return this;
        }

        public Builder withYieldAllowed(boolean z) {
            this.mYieldAllowed = z;
            return this;
        }
    }
}
