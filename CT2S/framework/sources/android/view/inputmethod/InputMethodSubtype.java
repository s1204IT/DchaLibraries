package android.view.inputmethod;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.ProxyInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class InputMethodSubtype implements Parcelable {
    private static final String EXTRA_KEY_UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME = "UntranslatableReplacementStringInSubtypeName";
    private static final String EXTRA_VALUE_KEY_VALUE_SEPARATOR = "=";
    private static final String EXTRA_VALUE_PAIR_SEPARATOR = ",";
    private volatile HashMap<String, String> mExtraValueHashMapCache;
    private final boolean mIsAsciiCapable;
    private final boolean mIsAuxiliary;
    private final boolean mOverridesImplicitlyEnabledSubtype;
    private final String mSubtypeExtraValue;
    private final int mSubtypeHashCode;
    private final int mSubtypeIconResId;
    private final int mSubtypeId;
    private final String mSubtypeLocale;
    private final String mSubtypeMode;
    private final int mSubtypeNameResId;
    private static final String TAG = InputMethodSubtype.class.getSimpleName();
    public static final Parcelable.Creator<InputMethodSubtype> CREATOR = new Parcelable.Creator<InputMethodSubtype>() {
        @Override
        public InputMethodSubtype createFromParcel(Parcel source) {
            return new InputMethodSubtype(source);
        }

        @Override
        public InputMethodSubtype[] newArray(int size) {
            return new InputMethodSubtype[size];
        }
    };

    public static class InputMethodSubtypeBuilder {
        private boolean mIsAuxiliary = false;
        private boolean mOverridesImplicitlyEnabledSubtype = false;
        private boolean mIsAsciiCapable = false;
        private int mSubtypeIconResId = 0;
        private int mSubtypeNameResId = 0;
        private int mSubtypeId = 0;
        private String mSubtypeLocale = ProxyInfo.LOCAL_EXCL_LIST;
        private String mSubtypeMode = ProxyInfo.LOCAL_EXCL_LIST;
        private String mSubtypeExtraValue = ProxyInfo.LOCAL_EXCL_LIST;

        public InputMethodSubtypeBuilder setIsAuxiliary(boolean isAuxiliary) {
            this.mIsAuxiliary = isAuxiliary;
            return this;
        }

        public InputMethodSubtypeBuilder setOverridesImplicitlyEnabledSubtype(boolean overridesImplicitlyEnabledSubtype) {
            this.mOverridesImplicitlyEnabledSubtype = overridesImplicitlyEnabledSubtype;
            return this;
        }

        public InputMethodSubtypeBuilder setIsAsciiCapable(boolean isAsciiCapable) {
            this.mIsAsciiCapable = isAsciiCapable;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeIconResId(int subtypeIconResId) {
            this.mSubtypeIconResId = subtypeIconResId;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeNameResId(int subtypeNameResId) {
            this.mSubtypeNameResId = subtypeNameResId;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeId(int subtypeId) {
            this.mSubtypeId = subtypeId;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeLocale(String subtypeLocale) {
            if (subtypeLocale == null) {
                subtypeLocale = ProxyInfo.LOCAL_EXCL_LIST;
            }
            this.mSubtypeLocale = subtypeLocale;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeMode(String subtypeMode) {
            if (subtypeMode == null) {
                subtypeMode = ProxyInfo.LOCAL_EXCL_LIST;
            }
            this.mSubtypeMode = subtypeMode;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeExtraValue(String subtypeExtraValue) {
            if (subtypeExtraValue == null) {
                subtypeExtraValue = ProxyInfo.LOCAL_EXCL_LIST;
            }
            this.mSubtypeExtraValue = subtypeExtraValue;
            return this;
        }

        public InputMethodSubtype build() {
            return new InputMethodSubtype(this);
        }
    }

    private static InputMethodSubtypeBuilder getBuilder(int nameId, int iconId, String locale, String mode, String extraValue, boolean isAuxiliary, boolean overridesImplicitlyEnabledSubtype, int id, boolean isAsciiCapable) {
        InputMethodSubtypeBuilder builder = new InputMethodSubtypeBuilder();
        builder.mSubtypeNameResId = nameId;
        builder.mSubtypeIconResId = iconId;
        builder.mSubtypeLocale = locale;
        builder.mSubtypeMode = mode;
        builder.mSubtypeExtraValue = extraValue;
        builder.mIsAuxiliary = isAuxiliary;
        builder.mOverridesImplicitlyEnabledSubtype = overridesImplicitlyEnabledSubtype;
        builder.mSubtypeId = id;
        builder.mIsAsciiCapable = isAsciiCapable;
        return builder;
    }

    public InputMethodSubtype(int nameId, int iconId, String locale, String mode, String extraValue, boolean isAuxiliary, boolean overridesImplicitlyEnabledSubtype) {
        this(nameId, iconId, locale, mode, extraValue, isAuxiliary, overridesImplicitlyEnabledSubtype, 0);
    }

    public InputMethodSubtype(int nameId, int iconId, String locale, String mode, String extraValue, boolean isAuxiliary, boolean overridesImplicitlyEnabledSubtype, int id) {
        this(getBuilder(nameId, iconId, locale, mode, extraValue, isAuxiliary, overridesImplicitlyEnabledSubtype, id, false));
    }

    private InputMethodSubtype(InputMethodSubtypeBuilder builder) {
        this.mSubtypeNameResId = builder.mSubtypeNameResId;
        this.mSubtypeIconResId = builder.mSubtypeIconResId;
        this.mSubtypeLocale = builder.mSubtypeLocale;
        this.mSubtypeMode = builder.mSubtypeMode;
        this.mSubtypeExtraValue = builder.mSubtypeExtraValue;
        this.mIsAuxiliary = builder.mIsAuxiliary;
        this.mOverridesImplicitlyEnabledSubtype = builder.mOverridesImplicitlyEnabledSubtype;
        this.mSubtypeId = builder.mSubtypeId;
        this.mIsAsciiCapable = builder.mIsAsciiCapable;
        this.mSubtypeHashCode = this.mSubtypeId != 0 ? this.mSubtypeId : hashCodeInternal(this.mSubtypeLocale, this.mSubtypeMode, this.mSubtypeExtraValue, this.mIsAuxiliary, this.mOverridesImplicitlyEnabledSubtype, this.mIsAsciiCapable);
    }

    InputMethodSubtype(Parcel source) {
        this.mSubtypeNameResId = source.readInt();
        this.mSubtypeIconResId = source.readInt();
        String s = source.readString();
        this.mSubtypeLocale = s == null ? ProxyInfo.LOCAL_EXCL_LIST : s;
        String s2 = source.readString();
        this.mSubtypeMode = s2 == null ? ProxyInfo.LOCAL_EXCL_LIST : s2;
        String s3 = source.readString();
        this.mSubtypeExtraValue = s3 == null ? ProxyInfo.LOCAL_EXCL_LIST : s3;
        this.mIsAuxiliary = source.readInt() == 1;
        this.mOverridesImplicitlyEnabledSubtype = source.readInt() == 1;
        this.mSubtypeHashCode = source.readInt();
        this.mSubtypeId = source.readInt();
        this.mIsAsciiCapable = source.readInt() == 1;
    }

    public int getNameResId() {
        return this.mSubtypeNameResId;
    }

    public int getIconResId() {
        return this.mSubtypeIconResId;
    }

    public String getLocale() {
        return this.mSubtypeLocale;
    }

    public String getMode() {
        return this.mSubtypeMode;
    }

    public String getExtraValue() {
        return this.mSubtypeExtraValue;
    }

    public boolean isAuxiliary() {
        return this.mIsAuxiliary;
    }

    public boolean overridesImplicitlyEnabledSubtype() {
        return this.mOverridesImplicitlyEnabledSubtype;
    }

    public boolean isAsciiCapable() {
        return this.mIsAsciiCapable;
    }

    public CharSequence getDisplayName(Context context, String packageName, ApplicationInfo appInfo) {
        Locale locale = constructLocaleFromString(this.mSubtypeLocale);
        String localeStr = locale != null ? locale.getDisplayName() : this.mSubtypeLocale;
        if (this.mSubtypeNameResId != 0) {
            CharSequence subtypeName = context.getPackageManager().getText(packageName, this.mSubtypeNameResId, appInfo);
            if (!TextUtils.isEmpty(subtypeName)) {
                String replacementString = containsExtraValueKey(EXTRA_KEY_UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME) ? getExtraValueOf(EXTRA_KEY_UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME) : localeStr;
                try {
                    String string = subtypeName.toString();
                    Object[] objArr = new Object[1];
                    if (replacementString == null) {
                        replacementString = ProxyInfo.LOCAL_EXCL_LIST;
                    }
                    objArr[0] = replacementString;
                    return String.format(string, objArr);
                } catch (IllegalFormatException e) {
                    Slog.w(TAG, "Found illegal format in subtype name(" + ((Object) subtypeName) + "): " + e);
                    return ProxyInfo.LOCAL_EXCL_LIST;
                }
            }
            return localeStr;
        }
        return localeStr;
    }

    private HashMap<String, String> getExtraValueHashMap() {
        if (this.mExtraValueHashMapCache == null) {
            synchronized (this) {
                if (this.mExtraValueHashMapCache == null) {
                    this.mExtraValueHashMapCache = new HashMap<>();
                    String[] pairs = this.mSubtypeExtraValue.split(EXTRA_VALUE_PAIR_SEPARATOR);
                    for (String str : pairs) {
                        String[] pair = str.split(EXTRA_VALUE_KEY_VALUE_SEPARATOR);
                        if (pair.length == 1) {
                            this.mExtraValueHashMapCache.put(pair[0], null);
                        } else if (pair.length > 1) {
                            if (pair.length > 2) {
                                Slog.w(TAG, "ExtraValue has two or more '='s");
                            }
                            this.mExtraValueHashMapCache.put(pair[0], pair[1]);
                        }
                    }
                }
            }
        }
        return this.mExtraValueHashMapCache;
    }

    public boolean containsExtraValueKey(String key) {
        return getExtraValueHashMap().containsKey(key);
    }

    public String getExtraValueOf(String key) {
        return getExtraValueHashMap().get(key);
    }

    public int hashCode() {
        return this.mSubtypeHashCode;
    }

    public boolean equals(Object o) {
        if (!(o instanceof InputMethodSubtype)) {
            return false;
        }
        InputMethodSubtype subtype = (InputMethodSubtype) o;
        return (subtype.mSubtypeId == 0 && this.mSubtypeId == 0) ? subtype.hashCode() == hashCode() && subtype.getLocale().equals(getLocale()) && subtype.getMode().equals(getMode()) && subtype.getExtraValue().equals(getExtraValue()) && subtype.isAuxiliary() == isAuxiliary() && subtype.overridesImplicitlyEnabledSubtype() == overridesImplicitlyEnabledSubtype() && subtype.isAsciiCapable() == isAsciiCapable() : subtype.hashCode() == hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeInt(this.mSubtypeNameResId);
        dest.writeInt(this.mSubtypeIconResId);
        dest.writeString(this.mSubtypeLocale);
        dest.writeString(this.mSubtypeMode);
        dest.writeString(this.mSubtypeExtraValue);
        dest.writeInt(this.mIsAuxiliary ? 1 : 0);
        dest.writeInt(this.mOverridesImplicitlyEnabledSubtype ? 1 : 0);
        dest.writeInt(this.mSubtypeHashCode);
        dest.writeInt(this.mSubtypeId);
        dest.writeInt(this.mIsAsciiCapable ? 1 : 0);
    }

    private static Locale constructLocaleFromString(String localeStr) {
        if (TextUtils.isEmpty(localeStr)) {
            return null;
        }
        String[] localeParams = localeStr.split("_", 3);
        if (localeParams.length == 1) {
            return new Locale(localeParams[0]);
        }
        if (localeParams.length == 2) {
            return new Locale(localeParams[0], localeParams[1]);
        }
        if (localeParams.length == 3) {
            return new Locale(localeParams[0], localeParams[1], localeParams[2]);
        }
        return null;
    }

    private static int hashCodeInternal(String locale, String mode, String extraValue, boolean isAuxiliary, boolean overridesImplicitlyEnabledSubtype, boolean isAsciiCapable) {
        boolean needsToCalculateCompatibleHashCode = !isAsciiCapable;
        return needsToCalculateCompatibleHashCode ? Arrays.hashCode(new Object[]{locale, mode, extraValue, Boolean.valueOf(isAuxiliary), Boolean.valueOf(overridesImplicitlyEnabledSubtype)}) : Arrays.hashCode(new Object[]{locale, mode, extraValue, Boolean.valueOf(isAuxiliary), Boolean.valueOf(overridesImplicitlyEnabledSubtype), Boolean.valueOf(isAsciiCapable)});
    }

    public static List<InputMethodSubtype> sort(Context context, int flags, InputMethodInfo imi, List<InputMethodSubtype> subtypeList) {
        if (imi != null) {
            HashSet<InputMethodSubtype> inputSubtypesSet = new HashSet<>(subtypeList);
            ArrayList<InputMethodSubtype> sortedList = new ArrayList<>();
            int N = imi.getSubtypeCount();
            for (int i = 0; i < N; i++) {
                InputMethodSubtype subtype = imi.getSubtypeAt(i);
                if (inputSubtypesSet.contains(subtype)) {
                    sortedList.add(subtype);
                    inputSubtypesSet.remove(subtype);
                }
            }
            Iterator<InputMethodSubtype> it = inputSubtypesSet.iterator();
            while (it.hasNext()) {
                sortedList.add(it.next());
            }
            return sortedList;
        }
        return subtypeList;
    }
}
