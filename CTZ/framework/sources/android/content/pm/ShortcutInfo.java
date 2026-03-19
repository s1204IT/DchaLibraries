package android.content.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

public final class ShortcutInfo implements Parcelable {
    private static final String ANDROID_PACKAGE_NAME = "android";
    public static final int CLONE_REMOVE_FOR_CREATOR = 9;
    public static final int CLONE_REMOVE_FOR_LAUNCHER = 11;
    public static final int CLONE_REMOVE_FOR_LAUNCHER_APPROVAL = 10;
    private static final int CLONE_REMOVE_ICON = 1;
    private static final int CLONE_REMOVE_INTENT = 2;
    public static final int CLONE_REMOVE_NON_KEY_INFO = 4;
    public static final int CLONE_REMOVE_RES_NAMES = 8;
    public static final Parcelable.Creator<ShortcutInfo> CREATOR = new Parcelable.Creator<ShortcutInfo>() {
        @Override
        public ShortcutInfo createFromParcel(Parcel parcel) {
            return new ShortcutInfo(parcel);
        }

        @Override
        public ShortcutInfo[] newArray(int i) {
            return new ShortcutInfo[i];
        }
    };
    public static final int DISABLED_REASON_APP_CHANGED = 2;
    public static final int DISABLED_REASON_BACKUP_NOT_SUPPORTED = 101;
    public static final int DISABLED_REASON_BY_APP = 1;
    public static final int DISABLED_REASON_NOT_DISABLED = 0;
    public static final int DISABLED_REASON_OTHER_RESTORE_ISSUE = 103;
    private static final int DISABLED_REASON_RESTORE_ISSUE_START = 100;
    public static final int DISABLED_REASON_SIGNATURE_MISMATCH = 102;
    public static final int DISABLED_REASON_UNKNOWN = 3;
    public static final int DISABLED_REASON_VERSION_LOWER = 100;
    public static final int FLAG_ADAPTIVE_BITMAP = 512;
    public static final int FLAG_DISABLED = 64;
    public static final int FLAG_DYNAMIC = 1;
    public static final int FLAG_HAS_ICON_FILE = 8;
    public static final int FLAG_HAS_ICON_RES = 4;
    public static final int FLAG_ICON_FILE_PENDING_SAVE = 2048;
    public static final int FLAG_IMMUTABLE = 256;
    public static final int FLAG_KEY_FIELDS_ONLY = 16;
    public static final int FLAG_MANIFEST = 32;
    public static final int FLAG_PINNED = 2;
    public static final int FLAG_RETURNED_BY_SERVICE = 1024;
    public static final int FLAG_SHADOW = 4096;
    public static final int FLAG_STRINGS_RESOLVED = 128;
    private static final int IMPLICIT_RANK_MASK = Integer.MAX_VALUE;
    private static final int RANK_CHANGED_BIT = Integer.MIN_VALUE;
    public static final int RANK_NOT_SET = Integer.MAX_VALUE;
    private static final String RES_TYPE_STRING = "string";
    public static final String SHORTCUT_CATEGORY_CONVERSATION = "android.shortcut.conversation";
    static final String TAG = "Shortcut";
    public static final int VERSION_CODE_UNKNOWN = -1;
    private ComponentName mActivity;
    private String mBitmapPath;
    private ArraySet<String> mCategories;
    private CharSequence mDisabledMessage;
    private int mDisabledMessageResId;
    private String mDisabledMessageResName;
    private int mDisabledReason;
    private PersistableBundle mExtras;
    private int mFlags;
    private Icon mIcon;
    private int mIconResId;
    private String mIconResName;
    private final String mId;
    private int mImplicitRank;
    private PersistableBundle[] mIntentPersistableExtrases;
    private Intent[] mIntents;
    private long mLastChangedTimestamp;
    private final String mPackageName;
    private int mRank;
    private CharSequence mText;
    private int mTextResId;
    private String mTextResName;
    private CharSequence mTitle;
    private int mTitleResId;
    private String mTitleResName;
    private final int mUserId;

    @Retention(RetentionPolicy.SOURCE)
    public @interface CloneFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface DisabledReason {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ShortcutFlags {
    }

    public static String getDisabledReasonDebugString(int i) {
        switch (i) {
            case 0:
                return "[Not disabled]";
            case 1:
                return "[Disabled: by app]";
            case 2:
                return "[Disabled: app changed]";
            default:
                switch (i) {
                    case 100:
                        return "[Disabled: lower version]";
                    case 101:
                        return "[Disabled: backup not supported]";
                    case 102:
                        return "[Disabled: signature mismatch]";
                    case 103:
                        return "[Disabled: unknown restore issue]";
                    default:
                        return "[Disabled: unknown reason:" + i + "]";
                }
        }
    }

    public static String getDisabledReasonForRestoreIssue(Context context, int i) {
        Resources resources = context.getResources();
        if (i != 3) {
            switch (i) {
                case 100:
                    return resources.getString(R.string.shortcut_restored_on_lower_version);
                case 101:
                    return resources.getString(R.string.shortcut_restore_not_supported);
                case 102:
                    return resources.getString(R.string.shortcut_restore_signature_mismatch);
                case 103:
                    return resources.getString(R.string.shortcut_restore_unknown_issue);
                default:
                    return null;
            }
        }
        return resources.getString(R.string.shortcut_disabled_reason_unknown);
    }

    public static boolean isDisabledForRestoreIssue(int i) {
        return i >= 100;
    }

    private ShortcutInfo(Builder builder) {
        this.mUserId = builder.mContext.getUserId();
        this.mId = (String) Preconditions.checkStringNotEmpty(builder.mId, "Shortcut ID must be provided");
        this.mPackageName = builder.mContext.getPackageName();
        this.mActivity = builder.mActivity;
        this.mIcon = builder.mIcon;
        this.mTitle = builder.mTitle;
        this.mTitleResId = builder.mTitleResId;
        this.mText = builder.mText;
        this.mTextResId = builder.mTextResId;
        this.mDisabledMessage = builder.mDisabledMessage;
        this.mDisabledMessageResId = builder.mDisabledMessageResId;
        this.mCategories = cloneCategories(builder.mCategories);
        this.mIntents = cloneIntents(builder.mIntents);
        fixUpIntentExtras();
        this.mRank = builder.mRank;
        this.mExtras = builder.mExtras;
        updateTimestamp();
    }

    private void fixUpIntentExtras() {
        if (this.mIntents == null) {
            this.mIntentPersistableExtrases = null;
            return;
        }
        this.mIntentPersistableExtrases = new PersistableBundle[this.mIntents.length];
        for (int i = 0; i < this.mIntents.length; i++) {
            Intent intent = this.mIntents[i];
            Bundle extras = intent.getExtras();
            if (extras == null) {
                this.mIntentPersistableExtrases[i] = null;
            } else {
                this.mIntentPersistableExtrases[i] = new PersistableBundle(extras);
                intent.replaceExtras((Bundle) null);
            }
        }
    }

    private static ArraySet<String> cloneCategories(Set<String> set) {
        if (set == null) {
            return null;
        }
        ArraySet<String> arraySet = new ArraySet<>(set.size());
        for (String str : set) {
            if (!TextUtils.isEmpty(str)) {
                arraySet.add(str.toString().intern());
            }
        }
        return arraySet;
    }

    private static Intent[] cloneIntents(Intent[] intentArr) {
        if (intentArr == null) {
            return null;
        }
        Intent[] intentArr2 = new Intent[intentArr.length];
        for (int i = 0; i < intentArr2.length; i++) {
            if (intentArr[i] != null) {
                intentArr2[i] = new Intent(intentArr[i]);
            }
        }
        return intentArr2;
    }

    private static PersistableBundle[] clonePersistableBundle(PersistableBundle[] persistableBundleArr) {
        if (persistableBundleArr == null) {
            return null;
        }
        PersistableBundle[] persistableBundleArr2 = new PersistableBundle[persistableBundleArr.length];
        for (int i = 0; i < persistableBundleArr2.length; i++) {
            if (persistableBundleArr[i] != null) {
                persistableBundleArr2[i] = new PersistableBundle(persistableBundleArr[i]);
            }
        }
        return persistableBundleArr2;
    }

    public void enforceMandatoryFields(boolean z) {
        Preconditions.checkStringNotEmpty(this.mId, "Shortcut ID must be provided");
        if (!z) {
            Preconditions.checkNotNull(this.mActivity, "Activity must be provided");
        }
        if (this.mTitle == null && this.mTitleResId == 0) {
            throw new IllegalArgumentException("Short label must be provided");
        }
        Preconditions.checkNotNull(this.mIntents, "Shortcut Intent must be provided");
        Preconditions.checkArgument(this.mIntents.length > 0, "Shortcut Intent must be provided");
    }

    private ShortcutInfo(ShortcutInfo shortcutInfo, int i) {
        this.mUserId = shortcutInfo.mUserId;
        this.mId = shortcutInfo.mId;
        this.mPackageName = shortcutInfo.mPackageName;
        this.mActivity = shortcutInfo.mActivity;
        this.mFlags = shortcutInfo.mFlags;
        this.mLastChangedTimestamp = shortcutInfo.mLastChangedTimestamp;
        this.mDisabledReason = shortcutInfo.mDisabledReason;
        this.mIconResId = shortcutInfo.mIconResId;
        if ((i & 4) == 0) {
            if ((i & 1) == 0) {
                this.mIcon = shortcutInfo.mIcon;
                this.mBitmapPath = shortcutInfo.mBitmapPath;
            }
            this.mTitle = shortcutInfo.mTitle;
            this.mTitleResId = shortcutInfo.mTitleResId;
            this.mText = shortcutInfo.mText;
            this.mTextResId = shortcutInfo.mTextResId;
            this.mDisabledMessage = shortcutInfo.mDisabledMessage;
            this.mDisabledMessageResId = shortcutInfo.mDisabledMessageResId;
            this.mCategories = cloneCategories(shortcutInfo.mCategories);
            if ((i & 2) == 0) {
                this.mIntents = cloneIntents(shortcutInfo.mIntents);
                this.mIntentPersistableExtrases = clonePersistableBundle(shortcutInfo.mIntentPersistableExtrases);
            }
            this.mRank = shortcutInfo.mRank;
            this.mExtras = shortcutInfo.mExtras;
            if ((i & 8) == 0) {
                this.mTitleResName = shortcutInfo.mTitleResName;
                this.mTextResName = shortcutInfo.mTextResName;
                this.mDisabledMessageResName = shortcutInfo.mDisabledMessageResName;
                this.mIconResName = shortcutInfo.mIconResName;
                return;
            }
            return;
        }
        this.mFlags |= 16;
    }

    private CharSequence getResourceString(Resources resources, int i, CharSequence charSequence) {
        try {
            return resources.getString(i);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource for ID=" + i + " not found in package " + this.mPackageName);
            return charSequence;
        }
    }

    public void resolveResourceStrings(Resources resources) {
        this.mFlags |= 128;
        if (this.mTitleResId == 0 && this.mTextResId == 0 && this.mDisabledMessageResId == 0) {
            return;
        }
        if (this.mTitleResId != 0) {
            this.mTitle = getResourceString(resources, this.mTitleResId, this.mTitle);
        }
        if (this.mTextResId != 0) {
            this.mText = getResourceString(resources, this.mTextResId, this.mText);
        }
        if (this.mDisabledMessageResId != 0) {
            this.mDisabledMessage = getResourceString(resources, this.mDisabledMessageResId, this.mDisabledMessage);
        }
    }

    @VisibleForTesting
    public static String lookUpResourceName(Resources resources, int i, boolean z, String str) {
        if (i == 0) {
            return null;
        }
        try {
            String resourceName = resources.getResourceName(i);
            if ("android".equals(getResourcePackageName(resourceName))) {
                return String.valueOf(i);
            }
            return z ? getResourceTypeAndEntryName(resourceName) : getResourceEntryName(resourceName);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource name for ID=" + i + " not found in package " + str + ". Resource IDs may change when the application is upgraded, and the system may not be able to find the correct resource.");
            return null;
        }
    }

    @VisibleForTesting
    public static String getResourcePackageName(String str) {
        int iIndexOf = str.indexOf(58);
        if (iIndexOf < 0) {
            return null;
        }
        return str.substring(0, iIndexOf);
    }

    @VisibleForTesting
    public static String getResourceTypeName(String str) {
        int i;
        int iIndexOf;
        int iIndexOf2 = str.indexOf(58);
        if (iIndexOf2 < 0 || (iIndexOf = str.indexOf(47, (i = iIndexOf2 + 1))) < 0) {
            return null;
        }
        return str.substring(i, iIndexOf);
    }

    @VisibleForTesting
    public static String getResourceTypeAndEntryName(String str) {
        int iIndexOf = str.indexOf(58);
        if (iIndexOf < 0) {
            return null;
        }
        return str.substring(iIndexOf + 1);
    }

    @VisibleForTesting
    public static String getResourceEntryName(String str) {
        int iIndexOf = str.indexOf(47);
        if (iIndexOf < 0) {
            return null;
        }
        return str.substring(iIndexOf + 1);
    }

    @VisibleForTesting
    public static int lookUpResourceId(Resources resources, String str, String str2, String str3) {
        try {
            if (str == null) {
                return 0;
            }
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return resources.getIdentifier(str, str2, str3);
            }
        } catch (Resources.NotFoundException e2) {
            Log.e(TAG, "Resource ID for name=" + str + " not found in package " + str3);
            return 0;
        }
    }

    public void lookupAndFillInResourceNames(Resources resources) {
        if (this.mTitleResId == 0 && this.mTextResId == 0 && this.mDisabledMessageResId == 0 && this.mIconResId == 0) {
            return;
        }
        this.mTitleResName = lookUpResourceName(resources, this.mTitleResId, false, this.mPackageName);
        this.mTextResName = lookUpResourceName(resources, this.mTextResId, false, this.mPackageName);
        this.mDisabledMessageResName = lookUpResourceName(resources, this.mDisabledMessageResId, false, this.mPackageName);
        this.mIconResName = lookUpResourceName(resources, this.mIconResId, true, this.mPackageName);
    }

    public void lookupAndFillInResourceIds(Resources resources) {
        if (this.mTitleResName == null && this.mTextResName == null && this.mDisabledMessageResName == null && this.mIconResName == null) {
            return;
        }
        this.mTitleResId = lookUpResourceId(resources, this.mTitleResName, RES_TYPE_STRING, this.mPackageName);
        this.mTextResId = lookUpResourceId(resources, this.mTextResName, RES_TYPE_STRING, this.mPackageName);
        this.mDisabledMessageResId = lookUpResourceId(resources, this.mDisabledMessageResName, RES_TYPE_STRING, this.mPackageName);
        this.mIconResId = lookUpResourceId(resources, this.mIconResName, null, this.mPackageName);
    }

    public ShortcutInfo clone(int i) {
        return new ShortcutInfo(this, i);
    }

    public void ensureUpdatableWith(ShortcutInfo shortcutInfo, boolean z) {
        if (z) {
            Preconditions.checkState(isVisibleToPublisher(), "[Framework BUG] Invisible shortcuts can't be updated");
        }
        Preconditions.checkState(this.mUserId == shortcutInfo.mUserId, "Owner User ID must match");
        Preconditions.checkState(this.mId.equals(shortcutInfo.mId), "ID must match");
        Preconditions.checkState(this.mPackageName.equals(shortcutInfo.mPackageName), "Package name must match");
        if (isVisibleToPublisher()) {
            Preconditions.checkState(!isImmutable(), "Target ShortcutInfo is immutable");
        }
    }

    public void copyNonNullFieldsFrom(ShortcutInfo shortcutInfo) {
        ensureUpdatableWith(shortcutInfo, true);
        if (shortcutInfo.mActivity != null) {
            this.mActivity = shortcutInfo.mActivity;
        }
        if (shortcutInfo.mIcon != null) {
            this.mIcon = shortcutInfo.mIcon;
            this.mIconResId = 0;
            this.mIconResName = null;
            this.mBitmapPath = null;
        }
        if (shortcutInfo.mTitle != null) {
            this.mTitle = shortcutInfo.mTitle;
            this.mTitleResId = 0;
            this.mTitleResName = null;
        } else if (shortcutInfo.mTitleResId != 0) {
            this.mTitle = null;
            this.mTitleResId = shortcutInfo.mTitleResId;
            this.mTitleResName = null;
        }
        if (shortcutInfo.mText != null) {
            this.mText = shortcutInfo.mText;
            this.mTextResId = 0;
            this.mTextResName = null;
        } else if (shortcutInfo.mTextResId != 0) {
            this.mText = null;
            this.mTextResId = shortcutInfo.mTextResId;
            this.mTextResName = null;
        }
        if (shortcutInfo.mDisabledMessage != null) {
            this.mDisabledMessage = shortcutInfo.mDisabledMessage;
            this.mDisabledMessageResId = 0;
            this.mDisabledMessageResName = null;
        } else if (shortcutInfo.mDisabledMessageResId != 0) {
            this.mDisabledMessage = null;
            this.mDisabledMessageResId = shortcutInfo.mDisabledMessageResId;
            this.mDisabledMessageResName = null;
        }
        if (shortcutInfo.mCategories != null) {
            this.mCategories = cloneCategories(shortcutInfo.mCategories);
        }
        if (shortcutInfo.mIntents != null) {
            this.mIntents = cloneIntents(shortcutInfo.mIntents);
            this.mIntentPersistableExtrases = clonePersistableBundle(shortcutInfo.mIntentPersistableExtrases);
        }
        if (shortcutInfo.mRank != Integer.MAX_VALUE) {
            this.mRank = shortcutInfo.mRank;
        }
        if (shortcutInfo.mExtras != null) {
            this.mExtras = shortcutInfo.mExtras;
        }
    }

    public static Icon validateIcon(Icon icon) {
        int type = icon.getType();
        if (type != 5) {
            switch (type) {
                case 1:
                case 2:
                    break;
                default:
                    throw getInvalidIconException();
            }
        }
        if (icon.hasTint()) {
            throw new IllegalArgumentException("Icons with tints are not supported");
        }
        return icon;
    }

    public static IllegalArgumentException getInvalidIconException() {
        return new IllegalArgumentException("Unsupported icon type: only the bitmap and resource types are supported");
    }

    public static class Builder {
        private ComponentName mActivity;
        private Set<String> mCategories;
        private final Context mContext;
        private CharSequence mDisabledMessage;
        private int mDisabledMessageResId;
        private PersistableBundle mExtras;
        private Icon mIcon;
        private String mId;
        private Intent[] mIntents;
        private int mRank = Integer.MAX_VALUE;
        private CharSequence mText;
        private int mTextResId;
        private CharSequence mTitle;
        private int mTitleResId;

        @Deprecated
        public Builder(Context context) {
            this.mContext = context;
        }

        @Deprecated
        public Builder setId(String str) {
            this.mId = (String) Preconditions.checkStringNotEmpty(str, "id cannot be empty");
            return this;
        }

        public Builder(Context context, String str) {
            this.mContext = context;
            this.mId = (String) Preconditions.checkStringNotEmpty(str, "id cannot be empty");
        }

        public Builder setActivity(ComponentName componentName) {
            this.mActivity = (ComponentName) Preconditions.checkNotNull(componentName, "activity cannot be null");
            return this;
        }

        public Builder setIcon(Icon icon) {
            this.mIcon = ShortcutInfo.validateIcon(icon);
            return this;
        }

        @Deprecated
        public Builder setShortLabelResId(int i) {
            Preconditions.checkState(this.mTitle == null, "shortLabel already set");
            this.mTitleResId = i;
            return this;
        }

        public Builder setShortLabel(CharSequence charSequence) {
            Preconditions.checkState(this.mTitleResId == 0, "shortLabelResId already set");
            this.mTitle = Preconditions.checkStringNotEmpty(charSequence, "shortLabel cannot be empty");
            return this;
        }

        @Deprecated
        public Builder setLongLabelResId(int i) {
            Preconditions.checkState(this.mText == null, "longLabel already set");
            this.mTextResId = i;
            return this;
        }

        public Builder setLongLabel(CharSequence charSequence) {
            Preconditions.checkState(this.mTextResId == 0, "longLabelResId already set");
            this.mText = Preconditions.checkStringNotEmpty(charSequence, "longLabel cannot be empty");
            return this;
        }

        @Deprecated
        public Builder setTitle(CharSequence charSequence) {
            return setShortLabel(charSequence);
        }

        @Deprecated
        public Builder setTitleResId(int i) {
            return setShortLabelResId(i);
        }

        @Deprecated
        public Builder setText(CharSequence charSequence) {
            return setLongLabel(charSequence);
        }

        @Deprecated
        public Builder setTextResId(int i) {
            return setLongLabelResId(i);
        }

        @Deprecated
        public Builder setDisabledMessageResId(int i) {
            Preconditions.checkState(this.mDisabledMessage == null, "disabledMessage already set");
            this.mDisabledMessageResId = i;
            return this;
        }

        public Builder setDisabledMessage(CharSequence charSequence) {
            Preconditions.checkState(this.mDisabledMessageResId == 0, "disabledMessageResId already set");
            this.mDisabledMessage = Preconditions.checkStringNotEmpty(charSequence, "disabledMessage cannot be empty");
            return this;
        }

        public Builder setCategories(Set<String> set) {
            this.mCategories = set;
            return this;
        }

        public Builder setIntent(Intent intent) {
            return setIntents(new Intent[]{intent});
        }

        public Builder setIntents(Intent[] intentArr) {
            Preconditions.checkNotNull(intentArr, "intents cannot be null");
            Preconditions.checkNotNull(Integer.valueOf(intentArr.length), "intents cannot be empty");
            for (Intent intent : intentArr) {
                Preconditions.checkNotNull(intent, "intents cannot contain null");
                Preconditions.checkNotNull(intent.getAction(), "intent's action must be set");
            }
            this.mIntents = ShortcutInfo.cloneIntents(intentArr);
            return this;
        }

        public Builder setRank(int i) {
            Preconditions.checkArgument(i >= 0, "Rank cannot be negative or bigger than MAX_RANK");
            this.mRank = i;
            return this;
        }

        public Builder setExtras(PersistableBundle persistableBundle) {
            this.mExtras = persistableBundle;
            return this;
        }

        public ShortcutInfo build() {
            return new ShortcutInfo(this);
        }
    }

    public String getId() {
        return this.mId;
    }

    public String getPackage() {
        return this.mPackageName;
    }

    public ComponentName getActivity() {
        return this.mActivity;
    }

    public void setActivity(ComponentName componentName) {
        this.mActivity = componentName;
    }

    public Icon getIcon() {
        return this.mIcon;
    }

    @Deprecated
    public CharSequence getTitle() {
        return this.mTitle;
    }

    @Deprecated
    public int getTitleResId() {
        return this.mTitleResId;
    }

    @Deprecated
    public CharSequence getText() {
        return this.mText;
    }

    @Deprecated
    public int getTextResId() {
        return this.mTextResId;
    }

    public CharSequence getShortLabel() {
        return this.mTitle;
    }

    public int getShortLabelResourceId() {
        return this.mTitleResId;
    }

    public CharSequence getLongLabel() {
        return this.mText;
    }

    public int getLongLabelResourceId() {
        return this.mTextResId;
    }

    public CharSequence getDisabledMessage() {
        return this.mDisabledMessage;
    }

    public int getDisabledMessageResourceId() {
        return this.mDisabledMessageResId;
    }

    public void setDisabledReason(int i) {
        this.mDisabledReason = i;
    }

    public int getDisabledReason() {
        return this.mDisabledReason;
    }

    public Set<String> getCategories() {
        return this.mCategories;
    }

    public Intent getIntent() {
        if (this.mIntents == null || this.mIntents.length == 0) {
            return null;
        }
        int length = this.mIntents.length - 1;
        return setIntentExtras(new Intent(this.mIntents[length]), this.mIntentPersistableExtrases[length]);
    }

    public Intent[] getIntents() {
        Intent[] intentArr = new Intent[this.mIntents.length];
        for (int i = 0; i < intentArr.length; i++) {
            intentArr[i] = new Intent(this.mIntents[i]);
            setIntentExtras(intentArr[i], this.mIntentPersistableExtrases[i]);
        }
        return intentArr;
    }

    public Intent[] getIntentsNoExtras() {
        return this.mIntents;
    }

    public PersistableBundle[] getIntentPersistableExtrases() {
        return this.mIntentPersistableExtrases;
    }

    public int getRank() {
        return this.mRank;
    }

    public boolean hasRank() {
        return this.mRank != Integer.MAX_VALUE;
    }

    public void setRank(int i) {
        this.mRank = i;
    }

    public void clearImplicitRankAndRankChangedFlag() {
        this.mImplicitRank = 0;
    }

    public void setImplicitRank(int i) {
        this.mImplicitRank = (i & Integer.MAX_VALUE) | (this.mImplicitRank & Integer.MIN_VALUE);
    }

    public int getImplicitRank() {
        return this.mImplicitRank & Integer.MAX_VALUE;
    }

    public void setRankChanged() {
        this.mImplicitRank |= Integer.MIN_VALUE;
    }

    public boolean isRankChanged() {
        return (this.mImplicitRank & Integer.MIN_VALUE) != 0;
    }

    public PersistableBundle getExtras() {
        return this.mExtras;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public UserHandle getUserHandle() {
        return UserHandle.of(this.mUserId);
    }

    public long getLastChangedTimestamp() {
        return this.mLastChangedTimestamp;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public void replaceFlags(int i) {
        this.mFlags = i;
    }

    public void addFlags(int i) {
        this.mFlags = i | this.mFlags;
    }

    public void clearFlags(int i) {
        this.mFlags = (~i) & this.mFlags;
    }

    public boolean hasFlags(int i) {
        return (this.mFlags & i) == i;
    }

    public boolean isReturnedByServer() {
        return hasFlags(1024);
    }

    public void setReturnedByServer() {
        addFlags(1024);
    }

    public boolean isDynamic() {
        return hasFlags(1);
    }

    public boolean isPinned() {
        return hasFlags(2);
    }

    public boolean isDeclaredInManifest() {
        return hasFlags(32);
    }

    @Deprecated
    public boolean isManifestShortcut() {
        return isDeclaredInManifest();
    }

    public boolean isFloating() {
        return (!isPinned() || isDynamic() || isManifestShortcut()) ? false : true;
    }

    public boolean isOriginallyFromManifest() {
        return hasFlags(256);
    }

    public boolean isDynamicVisible() {
        return isDynamic() && isVisibleToPublisher();
    }

    public boolean isPinnedVisible() {
        return isPinned() && isVisibleToPublisher();
    }

    public boolean isManifestVisible() {
        return isDeclaredInManifest() && isVisibleToPublisher();
    }

    public boolean isImmutable() {
        return hasFlags(256);
    }

    public boolean isEnabled() {
        return !hasFlags(64);
    }

    public boolean isAlive() {
        return hasFlags(2) || hasFlags(1) || hasFlags(32);
    }

    public boolean usesQuota() {
        return hasFlags(1) || hasFlags(32);
    }

    public boolean hasIconResource() {
        return hasFlags(4);
    }

    public boolean hasStringResources() {
        return (this.mTitleResId == 0 && this.mTextResId == 0 && this.mDisabledMessageResId == 0) ? false : true;
    }

    public boolean hasAnyResources() {
        return hasIconResource() || hasStringResources();
    }

    public boolean hasIconFile() {
        return hasFlags(8);
    }

    public boolean hasAdaptiveBitmap() {
        return hasFlags(512);
    }

    public boolean isIconPendingSave() {
        return hasFlags(2048);
    }

    public void setIconPendingSave() {
        addFlags(2048);
    }

    public void clearIconPendingSave() {
        clearFlags(2048);
    }

    public boolean isVisibleToPublisher() {
        return !isDisabledForRestoreIssue(this.mDisabledReason);
    }

    public boolean hasKeyFieldsOnly() {
        return hasFlags(16);
    }

    public boolean hasStringResourcesResolved() {
        return hasFlags(128);
    }

    public void updateTimestamp() {
        this.mLastChangedTimestamp = System.currentTimeMillis();
    }

    public void setTimestamp(long j) {
        this.mLastChangedTimestamp = j;
    }

    public void clearIcon() {
        this.mIcon = null;
    }

    public void setIconResourceId(int i) {
        if (this.mIconResId != i) {
            this.mIconResName = null;
        }
        this.mIconResId = i;
    }

    public int getIconResourceId() {
        return this.mIconResId;
    }

    public String getBitmapPath() {
        return this.mBitmapPath;
    }

    public void setBitmapPath(String str) {
        this.mBitmapPath = str;
    }

    public void setDisabledMessageResId(int i) {
        if (this.mDisabledMessageResId != i) {
            this.mDisabledMessageResName = null;
        }
        this.mDisabledMessageResId = i;
        this.mDisabledMessage = null;
    }

    public void setDisabledMessage(String str) {
        this.mDisabledMessage = str;
        this.mDisabledMessageResId = 0;
        this.mDisabledMessageResName = null;
    }

    public String getTitleResName() {
        return this.mTitleResName;
    }

    public void setTitleResName(String str) {
        this.mTitleResName = str;
    }

    public String getTextResName() {
        return this.mTextResName;
    }

    public void setTextResName(String str) {
        this.mTextResName = str;
    }

    public String getDisabledMessageResName() {
        return this.mDisabledMessageResName;
    }

    public void setDisabledMessageResName(String str) {
        this.mDisabledMessageResName = str;
    }

    public String getIconResName() {
        return this.mIconResName;
    }

    public void setIconResName(String str) {
        this.mIconResName = str;
    }

    public void setIntents(Intent[] intentArr) throws IllegalArgumentException {
        Preconditions.checkNotNull(intentArr);
        Preconditions.checkArgument(intentArr.length > 0);
        this.mIntents = cloneIntents(intentArr);
        fixUpIntentExtras();
    }

    public static Intent setIntentExtras(Intent intent, PersistableBundle persistableBundle) {
        if (persistableBundle == null) {
            intent.replaceExtras((Bundle) null);
        } else {
            intent.replaceExtras(new Bundle(persistableBundle));
        }
        return intent;
    }

    public void setCategories(Set<String> set) {
        this.mCategories = cloneCategories(set);
    }

    private ShortcutInfo(Parcel parcel) {
        ClassLoader classLoader = getClass().getClassLoader();
        this.mUserId = parcel.readInt();
        this.mId = parcel.readString();
        this.mPackageName = parcel.readString();
        this.mActivity = (ComponentName) parcel.readParcelable(classLoader);
        this.mFlags = parcel.readInt();
        this.mIconResId = parcel.readInt();
        this.mLastChangedTimestamp = parcel.readLong();
        this.mDisabledReason = parcel.readInt();
        if (parcel.readInt() == 0) {
            return;
        }
        this.mIcon = (Icon) parcel.readParcelable(classLoader);
        this.mTitle = parcel.readCharSequence();
        this.mTitleResId = parcel.readInt();
        this.mText = parcel.readCharSequence();
        this.mTextResId = parcel.readInt();
        this.mDisabledMessage = parcel.readCharSequence();
        this.mDisabledMessageResId = parcel.readInt();
        this.mIntents = (Intent[]) parcel.readParcelableArray(classLoader, Intent.class);
        this.mIntentPersistableExtrases = (PersistableBundle[]) parcel.readParcelableArray(classLoader, PersistableBundle.class);
        this.mRank = parcel.readInt();
        this.mExtras = (PersistableBundle) parcel.readParcelable(classLoader);
        this.mBitmapPath = parcel.readString();
        this.mIconResName = parcel.readString();
        this.mTitleResName = parcel.readString();
        this.mTextResName = parcel.readString();
        this.mDisabledMessageResName = parcel.readString();
        int i = parcel.readInt();
        if (i == 0) {
            this.mCategories = null;
            return;
        }
        this.mCategories = new ArraySet<>(i);
        for (int i2 = 0; i2 < i; i2++) {
            this.mCategories.add(parcel.readString().intern());
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mUserId);
        parcel.writeString(this.mId);
        parcel.writeString(this.mPackageName);
        parcel.writeParcelable(this.mActivity, i);
        parcel.writeInt(this.mFlags);
        parcel.writeInt(this.mIconResId);
        parcel.writeLong(this.mLastChangedTimestamp);
        parcel.writeInt(this.mDisabledReason);
        if (hasKeyFieldsOnly()) {
            parcel.writeInt(0);
            return;
        }
        parcel.writeInt(1);
        parcel.writeParcelable(this.mIcon, i);
        parcel.writeCharSequence(this.mTitle);
        parcel.writeInt(this.mTitleResId);
        parcel.writeCharSequence(this.mText);
        parcel.writeInt(this.mTextResId);
        parcel.writeCharSequence(this.mDisabledMessage);
        parcel.writeInt(this.mDisabledMessageResId);
        parcel.writeParcelableArray(this.mIntents, i);
        parcel.writeParcelableArray(this.mIntentPersistableExtrases, i);
        parcel.writeInt(this.mRank);
        parcel.writeParcelable(this.mExtras, i);
        parcel.writeString(this.mBitmapPath);
        parcel.writeString(this.mIconResName);
        parcel.writeString(this.mTitleResName);
        parcel.writeString(this.mTextResName);
        parcel.writeString(this.mDisabledMessageResName);
        if (this.mCategories != null) {
            int size = this.mCategories.size();
            parcel.writeInt(size);
            for (int i2 = 0; i2 < size; i2++) {
                parcel.writeString(this.mCategories.valueAt(i2));
            }
            return;
        }
        parcel.writeInt(0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return toStringInner(true, false, null);
    }

    public String toInsecureString() {
        return toStringInner(false, true, null);
    }

    public String toDumpString(String str) {
        return toStringInner(false, true, str);
    }

    private void addIndentOrComma(StringBuilder sb, String str) {
        if (str != null) {
            sb.append("\n  ");
            sb.append(str);
        } else {
            sb.append(", ");
        }
    }

    private String toStringInner(boolean z, boolean z2, String str) {
        StringBuilder sb = new StringBuilder();
        if (str != null) {
            sb.append(str);
        }
        sb.append("ShortcutInfo {");
        sb.append("id=");
        sb.append(z ? "***" : this.mId);
        sb.append(", flags=0x");
        sb.append(Integer.toHexString(this.mFlags));
        sb.append(" [");
        if ((this.mFlags & 4096) != 0) {
            sb.append("Sdw");
        }
        if (!isEnabled()) {
            sb.append("Dis");
        }
        if (isImmutable()) {
            sb.append("Im");
        }
        if (isManifestShortcut()) {
            sb.append("Man");
        }
        if (isDynamic()) {
            sb.append("Dyn");
        }
        if (isPinned()) {
            sb.append("Pin");
        }
        if (hasIconFile()) {
            sb.append("Ic-f");
        }
        if (isIconPendingSave()) {
            sb.append("Pens");
        }
        if (hasIconResource()) {
            sb.append("Ic-r");
        }
        if (hasKeyFieldsOnly()) {
            sb.append("Key");
        }
        if (hasStringResourcesResolved()) {
            sb.append("Str");
        }
        if (isReturnedByServer()) {
            sb.append("Rets");
        }
        sb.append("]");
        addIndentOrComma(sb, str);
        sb.append("packageName=");
        sb.append(this.mPackageName);
        addIndentOrComma(sb, str);
        sb.append("activity=");
        sb.append(this.mActivity);
        addIndentOrComma(sb, str);
        sb.append("shortLabel=");
        sb.append(z ? "***" : this.mTitle);
        sb.append(", resId=");
        sb.append(this.mTitleResId);
        sb.append("[");
        sb.append(this.mTitleResName);
        sb.append("]");
        addIndentOrComma(sb, str);
        sb.append("longLabel=");
        sb.append(z ? "***" : this.mText);
        sb.append(", resId=");
        sb.append(this.mTextResId);
        sb.append("[");
        sb.append(this.mTextResName);
        sb.append("]");
        addIndentOrComma(sb, str);
        sb.append("disabledMessage=");
        sb.append(z ? "***" : this.mDisabledMessage);
        sb.append(", resId=");
        sb.append(this.mDisabledMessageResId);
        sb.append("[");
        sb.append(this.mDisabledMessageResName);
        sb.append("]");
        addIndentOrComma(sb, str);
        sb.append("disabledReason=");
        sb.append(getDisabledReasonDebugString(this.mDisabledReason));
        addIndentOrComma(sb, str);
        sb.append("categories=");
        sb.append(this.mCategories);
        addIndentOrComma(sb, str);
        sb.append("icon=");
        sb.append(this.mIcon);
        addIndentOrComma(sb, str);
        sb.append("rank=");
        sb.append(this.mRank);
        sb.append(", timestamp=");
        sb.append(this.mLastChangedTimestamp);
        addIndentOrComma(sb, str);
        sb.append("intents=");
        if (this.mIntents == null) {
            sb.append("null");
        } else if (z) {
            sb.append("size:");
            sb.append(this.mIntents.length);
        } else {
            int length = this.mIntents.length;
            sb.append("[");
            String str2 = "";
            for (int i = 0; i < length; i++) {
                sb.append(str2);
                str2 = ", ";
                sb.append(this.mIntents[i]);
                sb.append("/");
                sb.append(this.mIntentPersistableExtrases[i]);
            }
            sb.append("]");
        }
        addIndentOrComma(sb, str);
        sb.append("extras=");
        sb.append(this.mExtras);
        if (z2) {
            addIndentOrComma(sb, str);
            sb.append("iconRes=");
            sb.append(this.mIconResId);
            sb.append("[");
            sb.append(this.mIconResName);
            sb.append("]");
            sb.append(", bitmapPath=");
            sb.append(this.mBitmapPath);
        }
        sb.append("}");
        return sb.toString();
    }

    public ShortcutInfo(int i, String str, String str2, ComponentName componentName, Icon icon, CharSequence charSequence, int i2, String str3, CharSequence charSequence2, int i3, String str4, CharSequence charSequence3, int i4, String str5, Set<String> set, Intent[] intentArr, int i5, PersistableBundle persistableBundle, long j, int i6, int i7, String str6, String str7, int i8) {
        this.mUserId = i;
        this.mId = str;
        this.mPackageName = str2;
        this.mActivity = componentName;
        this.mIcon = icon;
        this.mTitle = charSequence;
        this.mTitleResId = i2;
        this.mTitleResName = str3;
        this.mText = charSequence2;
        this.mTextResId = i3;
        this.mTextResName = str4;
        this.mDisabledMessage = charSequence3;
        this.mDisabledMessageResId = i4;
        this.mDisabledMessageResName = str5;
        this.mCategories = cloneCategories(set);
        this.mIntents = cloneIntents(intentArr);
        fixUpIntentExtras();
        this.mRank = i5;
        this.mExtras = persistableBundle;
        this.mLastChangedTimestamp = j;
        this.mFlags = i6;
        this.mIconResId = i7;
        this.mIconResName = str6;
        this.mBitmapPath = str7;
        this.mDisabledReason = i8;
    }
}
