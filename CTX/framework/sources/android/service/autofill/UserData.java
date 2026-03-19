package android.service.autofill;

import android.app.ActivityThread;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;
import java.io.PrintWriter;
import java.util.ArrayList;

public final class UserData implements Parcelable {
    public static final Parcelable.Creator<UserData> CREATOR = new Parcelable.Creator<UserData>() {
        @Override
        public UserData createFromParcel(Parcel parcel) {
            String string = parcel.readString();
            String[] stringArray = parcel.readStringArray();
            String[] stringArray2 = parcel.readStringArray();
            Builder fieldClassificationAlgorithm = new Builder(string, stringArray2[0], stringArray[0]).setFieldClassificationAlgorithm(parcel.readString(), parcel.readBundle());
            for (int i = 1; i < stringArray.length; i++) {
                fieldClassificationAlgorithm.add(stringArray2[i], stringArray[i]);
            }
            return fieldClassificationAlgorithm.build();
        }

        @Override
        public UserData[] newArray(int i) {
            return new UserData[i];
        }
    };
    private static final int DEFAULT_MAX_CATEGORY_COUNT = 10;
    private static final int DEFAULT_MAX_FIELD_CLASSIFICATION_IDS_SIZE = 10;
    private static final int DEFAULT_MAX_USER_DATA_SIZE = 50;
    private static final int DEFAULT_MAX_VALUE_LENGTH = 100;
    private static final int DEFAULT_MIN_VALUE_LENGTH = 3;
    private static final String TAG = "UserData";
    private final String mAlgorithm;
    private final Bundle mAlgorithmArgs;
    private final String[] mCategoryIds;
    private final String mId;
    private final String[] mValues;

    private UserData(Builder builder) {
        this.mId = builder.mId;
        this.mAlgorithm = builder.mAlgorithm;
        this.mAlgorithmArgs = builder.mAlgorithmArgs;
        this.mCategoryIds = new String[builder.mCategoryIds.size()];
        builder.mCategoryIds.toArray(this.mCategoryIds);
        this.mValues = new String[builder.mValues.size()];
        builder.mValues.toArray(this.mValues);
    }

    public String getFieldClassificationAlgorithm() {
        return this.mAlgorithm;
    }

    public String getId() {
        return this.mId;
    }

    public Bundle getAlgorithmArgs() {
        return this.mAlgorithmArgs;
    }

    public String[] getCategoryIds() {
        return this.mCategoryIds;
    }

    public String[] getValues() {
        return this.mValues;
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("id: ");
        printWriter.print(this.mId);
        printWriter.print(str);
        printWriter.print("Algorithm: ");
        printWriter.print(this.mAlgorithm);
        printWriter.print(" Args: ");
        printWriter.println(this.mAlgorithmArgs);
        printWriter.print(str);
        printWriter.print("Field ids size: ");
        printWriter.println(this.mCategoryIds.length);
        for (int i = 0; i < this.mCategoryIds.length; i++) {
            printWriter.print(str);
            printWriter.print(str);
            printWriter.print(i);
            printWriter.print(": ");
            printWriter.println(Helper.getRedacted(this.mCategoryIds[i]));
        }
        printWriter.print(str);
        printWriter.print("Values size: ");
        printWriter.println(this.mValues.length);
        for (int i2 = 0; i2 < this.mValues.length; i2++) {
            printWriter.print(str);
            printWriter.print(str);
            printWriter.print(i2);
            printWriter.print(": ");
            printWriter.println(Helper.getRedacted(this.mValues[i2]));
        }
    }

    public static void dumpConstraints(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("maxUserDataSize: ");
        printWriter.println(getMaxUserDataSize());
        printWriter.print(str);
        printWriter.print("maxFieldClassificationIdsSize: ");
        printWriter.println(getMaxFieldClassificationIdsSize());
        printWriter.print(str);
        printWriter.print("maxCategoryCount: ");
        printWriter.println(getMaxCategoryCount());
        printWriter.print(str);
        printWriter.print("minValueLength: ");
        printWriter.println(getMinValueLength());
        printWriter.print(str);
        printWriter.print("maxValueLength: ");
        printWriter.println(getMaxValueLength());
    }

    public static final class Builder {
        private String mAlgorithm;
        private Bundle mAlgorithmArgs;
        private final ArrayList<String> mCategoryIds;
        private boolean mDestroyed;
        private final String mId;
        private final ArraySet<String> mUniqueCategoryIds;
        private final ArrayList<String> mValues;

        public Builder(String str, String str2, String str3) {
            this.mId = checkNotEmpty(Instrumentation.REPORT_KEY_IDENTIFIER, str);
            checkNotEmpty("categoryId", str3);
            checkValidValue(str2);
            int maxUserDataSize = UserData.getMaxUserDataSize();
            this.mCategoryIds = new ArrayList<>(maxUserDataSize);
            this.mValues = new ArrayList<>(maxUserDataSize);
            this.mUniqueCategoryIds = new ArraySet<>(UserData.getMaxCategoryCount());
            addMapping(str2, str3);
        }

        public Builder setFieldClassificationAlgorithm(String str, Bundle bundle) {
            throwIfDestroyed();
            this.mAlgorithm = str;
            this.mAlgorithmArgs = bundle;
            return this;
        }

        public Builder add(String str, String str2) {
            throwIfDestroyed();
            checkNotEmpty("categoryId", str2);
            checkValidValue(str);
            if (!this.mUniqueCategoryIds.contains(str2)) {
                Preconditions.checkState(this.mUniqueCategoryIds.size() < UserData.getMaxCategoryCount(), "already added " + this.mUniqueCategoryIds.size() + " unique category ids");
            }
            Preconditions.checkState(!this.mValues.contains(str), "already has entry with same value");
            Preconditions.checkState(this.mValues.size() < UserData.getMaxUserDataSize(), "already added " + this.mValues.size() + " elements");
            addMapping(str, str2);
            return this;
        }

        private void addMapping(String str, String str2) {
            this.mCategoryIds.add(str2);
            this.mValues.add(str);
            this.mUniqueCategoryIds.add(str2);
        }

        private String checkNotEmpty(String str, String str2) {
            Preconditions.checkNotNull(str2);
            Preconditions.checkArgument(!TextUtils.isEmpty(str2), "%s cannot be empty", str);
            return str2;
        }

        private void checkValidValue(String str) {
            Preconditions.checkNotNull(str);
            int length = str.length();
            Preconditions.checkArgumentInRange(length, UserData.getMinValueLength(), UserData.getMaxValueLength(), "value length (" + length + ")");
        }

        public UserData build() {
            throwIfDestroyed();
            this.mDestroyed = true;
            return new UserData(this);
        }

        private void throwIfDestroyed() {
            if (this.mDestroyed) {
                throw new IllegalStateException("Already called #build()");
            }
        }
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder sb = new StringBuilder("UserData: [id=");
        sb.append(this.mId);
        sb.append(", algorithm=");
        sb.append(this.mAlgorithm);
        sb.append(", categoryIds=");
        Helper.appendRedacted(sb, this.mCategoryIds);
        sb.append(", values=");
        Helper.appendRedacted(sb, this.mValues);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mId);
        parcel.writeStringArray(this.mCategoryIds);
        parcel.writeStringArray(this.mValues);
        parcel.writeString(this.mAlgorithm);
        parcel.writeBundle(this.mAlgorithmArgs);
    }

    public static int getMaxUserDataSize() {
        return getInt(Settings.Secure.AUTOFILL_USER_DATA_MAX_USER_DATA_SIZE, 50);
    }

    public static int getMaxFieldClassificationIdsSize() {
        return getInt(Settings.Secure.AUTOFILL_USER_DATA_MAX_FIELD_CLASSIFICATION_IDS_SIZE, 10);
    }

    public static int getMaxCategoryCount() {
        return getInt(Settings.Secure.AUTOFILL_USER_DATA_MAX_CATEGORY_COUNT, 10);
    }

    public static int getMinValueLength() {
        return getInt(Settings.Secure.AUTOFILL_USER_DATA_MIN_VALUE_LENGTH, 3);
    }

    public static int getMaxValueLength() {
        return getInt(Settings.Secure.AUTOFILL_USER_DATA_MAX_VALUE_LENGTH, 100);
    }

    private static int getInt(String str, int i) {
        ContentResolver contentResolver;
        ActivityThread activityThreadCurrentActivityThread = ActivityThread.currentActivityThread();
        if (activityThreadCurrentActivityThread != null) {
            contentResolver = activityThreadCurrentActivityThread.getApplication().getContentResolver();
        } else {
            contentResolver = null;
        }
        if (contentResolver == null) {
            Log.w(TAG, "Could not read from " + str + "; hardcoding " + i);
            return i;
        }
        return Settings.Secure.getInt(contentResolver, str, i);
    }
}
