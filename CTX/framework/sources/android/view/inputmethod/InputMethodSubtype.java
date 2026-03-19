package android.view.inputmethod;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.icu.text.DisplayContext;
import android.icu.text.LocaleDisplayNames;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.inputmethod.InputMethodUtils;
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
    private static final String LANGUAGE_TAG_NONE = "";
    private static final int SUBTYPE_ID_NONE = 0;
    private volatile Locale mCachedLocaleObj;
    private volatile HashMap<String, String> mExtraValueHashMapCache;
    private final boolean mIsAsciiCapable;
    private final boolean mIsAuxiliary;
    private final Object mLock;
    private final boolean mOverridesImplicitlyEnabledSubtype;
    private final String mSubtypeExtraValue;
    private final int mSubtypeHashCode;
    private final int mSubtypeIconResId;
    private final int mSubtypeId;
    private final String mSubtypeLanguageTag;
    private final String mSubtypeLocale;
    private final String mSubtypeMode;
    private final int mSubtypeNameResId;
    private static final String TAG = InputMethodSubtype.class.getSimpleName();
    public static final Parcelable.Creator<InputMethodSubtype> CREATOR = new Parcelable.Creator<InputMethodSubtype>() {
        @Override
        public InputMethodSubtype createFromParcel(Parcel parcel) {
            return new InputMethodSubtype(parcel);
        }

        @Override
        public InputMethodSubtype[] newArray(int i) {
            return new InputMethodSubtype[i];
        }
    };

    public static class InputMethodSubtypeBuilder {
        private boolean mIsAuxiliary = false;
        private boolean mOverridesImplicitlyEnabledSubtype = false;
        private boolean mIsAsciiCapable = false;
        private int mSubtypeIconResId = 0;
        private int mSubtypeNameResId = 0;
        private int mSubtypeId = 0;
        private String mSubtypeLocale = "";
        private String mSubtypeLanguageTag = "";
        private String mSubtypeMode = "";
        private String mSubtypeExtraValue = "";

        public InputMethodSubtypeBuilder setIsAuxiliary(boolean z) {
            this.mIsAuxiliary = z;
            return this;
        }

        public InputMethodSubtypeBuilder setOverridesImplicitlyEnabledSubtype(boolean z) {
            this.mOverridesImplicitlyEnabledSubtype = z;
            return this;
        }

        public InputMethodSubtypeBuilder setIsAsciiCapable(boolean z) {
            this.mIsAsciiCapable = z;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeIconResId(int i) {
            this.mSubtypeIconResId = i;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeNameResId(int i) {
            this.mSubtypeNameResId = i;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeId(int i) {
            this.mSubtypeId = i;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeLocale(String str) {
            if (str == null) {
                str = "";
            }
            this.mSubtypeLocale = str;
            return this;
        }

        public InputMethodSubtypeBuilder setLanguageTag(String str) {
            if (str == null) {
                str = "";
            }
            this.mSubtypeLanguageTag = str;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeMode(String str) {
            if (str == null) {
                str = "";
            }
            this.mSubtypeMode = str;
            return this;
        }

        public InputMethodSubtypeBuilder setSubtypeExtraValue(String str) {
            if (str == null) {
                str = "";
            }
            this.mSubtypeExtraValue = str;
            return this;
        }

        public InputMethodSubtype build() {
            return new InputMethodSubtype(this);
        }
    }

    private static InputMethodSubtypeBuilder getBuilder(int i, int i2, String str, String str2, String str3, boolean z, boolean z2, int i3, boolean z3) {
        InputMethodSubtypeBuilder inputMethodSubtypeBuilder = new InputMethodSubtypeBuilder();
        inputMethodSubtypeBuilder.mSubtypeNameResId = i;
        inputMethodSubtypeBuilder.mSubtypeIconResId = i2;
        inputMethodSubtypeBuilder.mSubtypeLocale = str;
        inputMethodSubtypeBuilder.mSubtypeMode = str2;
        inputMethodSubtypeBuilder.mSubtypeExtraValue = str3;
        inputMethodSubtypeBuilder.mIsAuxiliary = z;
        inputMethodSubtypeBuilder.mOverridesImplicitlyEnabledSubtype = z2;
        inputMethodSubtypeBuilder.mSubtypeId = i3;
        inputMethodSubtypeBuilder.mIsAsciiCapable = z3;
        return inputMethodSubtypeBuilder;
    }

    @Deprecated
    public InputMethodSubtype(int i, int i2, String str, String str2, String str3, boolean z, boolean z2) {
        this(i, i2, str, str2, str3, z, z2, 0);
    }

    @Deprecated
    public InputMethodSubtype(int i, int i2, String str, String str2, String str3, boolean z, boolean z2, int i3) {
        this(getBuilder(i, i2, str, str2, str3, z, z2, i3, false));
    }

    private InputMethodSubtype(InputMethodSubtypeBuilder inputMethodSubtypeBuilder) {
        this.mLock = new Object();
        this.mSubtypeNameResId = inputMethodSubtypeBuilder.mSubtypeNameResId;
        this.mSubtypeIconResId = inputMethodSubtypeBuilder.mSubtypeIconResId;
        this.mSubtypeLocale = inputMethodSubtypeBuilder.mSubtypeLocale;
        this.mSubtypeLanguageTag = inputMethodSubtypeBuilder.mSubtypeLanguageTag;
        this.mSubtypeMode = inputMethodSubtypeBuilder.mSubtypeMode;
        this.mSubtypeExtraValue = inputMethodSubtypeBuilder.mSubtypeExtraValue;
        this.mIsAuxiliary = inputMethodSubtypeBuilder.mIsAuxiliary;
        this.mOverridesImplicitlyEnabledSubtype = inputMethodSubtypeBuilder.mOverridesImplicitlyEnabledSubtype;
        this.mSubtypeId = inputMethodSubtypeBuilder.mSubtypeId;
        this.mIsAsciiCapable = inputMethodSubtypeBuilder.mIsAsciiCapable;
        if (this.mSubtypeId != 0) {
            this.mSubtypeHashCode = this.mSubtypeId;
        } else {
            this.mSubtypeHashCode = hashCodeInternal(this.mSubtypeLocale, this.mSubtypeMode, this.mSubtypeExtraValue, this.mIsAuxiliary, this.mOverridesImplicitlyEnabledSubtype, this.mIsAsciiCapable);
        }
    }

    InputMethodSubtype(Parcel parcel) {
        this.mLock = new Object();
        this.mSubtypeNameResId = parcel.readInt();
        this.mSubtypeIconResId = parcel.readInt();
        String string = parcel.readString();
        this.mSubtypeLocale = string == null ? "" : string;
        String string2 = parcel.readString();
        this.mSubtypeLanguageTag = string2 == null ? "" : string2;
        String string3 = parcel.readString();
        this.mSubtypeMode = string3 == null ? "" : string3;
        String string4 = parcel.readString();
        this.mSubtypeExtraValue = string4 == null ? "" : string4;
        this.mIsAuxiliary = parcel.readInt() == 1;
        this.mOverridesImplicitlyEnabledSubtype = parcel.readInt() == 1;
        this.mSubtypeHashCode = parcel.readInt();
        this.mSubtypeId = parcel.readInt();
        this.mIsAsciiCapable = parcel.readInt() == 1;
    }

    public int getNameResId() {
        return this.mSubtypeNameResId;
    }

    public int getIconResId() {
        return this.mSubtypeIconResId;
    }

    @Deprecated
    public String getLocale() {
        return this.mSubtypeLocale;
    }

    public String getLanguageTag() {
        return this.mSubtypeLanguageTag;
    }

    public Locale getLocaleObject() {
        if (this.mCachedLocaleObj != null) {
            return this.mCachedLocaleObj;
        }
        synchronized (this.mLock) {
            if (this.mCachedLocaleObj != null) {
                return this.mCachedLocaleObj;
            }
            if (!TextUtils.isEmpty(this.mSubtypeLanguageTag)) {
                this.mCachedLocaleObj = Locale.forLanguageTag(this.mSubtypeLanguageTag);
            } else {
                this.mCachedLocaleObj = InputMethodUtils.constructLocaleFromString(this.mSubtypeLocale);
            }
            return this.mCachedLocaleObj;
        }
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

    public CharSequence getDisplayName(Context context, String str, ApplicationInfo applicationInfo) {
        DisplayContext displayContext;
        String localeDisplayName;
        if (this.mSubtypeNameResId == 0) {
            return getLocaleDisplayName(getLocaleFromContext(context), getLocaleObject(), DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU);
        }
        CharSequence text = context.getPackageManager().getText(str, this.mSubtypeNameResId, applicationInfo);
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        String string = text.toString();
        if (containsExtraValueKey(EXTRA_KEY_UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME)) {
            localeDisplayName = getExtraValueOf(EXTRA_KEY_UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME);
        } else {
            if (TextUtils.equals(string, "%s")) {
                displayContext = DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU;
            } else if (string.startsWith("%s")) {
                displayContext = DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE;
            } else {
                displayContext = DisplayContext.CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE;
            }
            localeDisplayName = getLocaleDisplayName(getLocaleFromContext(context), getLocaleObject(), displayContext);
        }
        if (localeDisplayName == null) {
            localeDisplayName = "";
        }
        try {
            return String.format(string, localeDisplayName);
        } catch (IllegalFormatException e) {
            Slog.w(TAG, "Found illegal format in subtype name(" + ((Object) text) + "): " + e);
            return "";
        }
    }

    private static Locale getLocaleFromContext(Context context) {
        Configuration configuration;
        if (context == null || context.getResources() == null || (configuration = context.getResources().getConfiguration()) == null) {
            return null;
        }
        return configuration.getLocales().get(0);
    }

    private static String getLocaleDisplayName(Locale locale, Locale locale2, DisplayContext displayContext) {
        if (locale2 == null) {
            return "";
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return LocaleDisplayNames.getInstance(locale, displayContext).localeDisplayName(locale2);
    }

    private HashMap<String, String> getExtraValueHashMap() {
        synchronized (this) {
            HashMap<String, String> map = this.mExtraValueHashMapCache;
            if (map != null) {
                return map;
            }
            HashMap<String, String> map2 = new HashMap<>();
            for (String str : this.mSubtypeExtraValue.split(EXTRA_VALUE_PAIR_SEPARATOR)) {
                String[] strArrSplit = str.split(EXTRA_VALUE_KEY_VALUE_SEPARATOR);
                if (strArrSplit.length == 1) {
                    map2.put(strArrSplit[0], null);
                } else if (strArrSplit.length > 1) {
                    if (strArrSplit.length > 2) {
                        Slog.w(TAG, "ExtraValue has two or more '='s");
                    }
                    map2.put(strArrSplit[0], strArrSplit[1]);
                }
            }
            this.mExtraValueHashMapCache = map2;
            return map2;
        }
    }

    public boolean containsExtraValueKey(String str) {
        return getExtraValueHashMap().containsKey(str);
    }

    public String getExtraValueOf(String str) {
        return getExtraValueHashMap().get(str);
    }

    public int hashCode() {
        return this.mSubtypeHashCode;
    }

    public final boolean hasSubtypeId() {
        return this.mSubtypeId != 0;
    }

    public final int getSubtypeId() {
        return this.mSubtypeId;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof InputMethodSubtype)) {
            return false;
        }
        InputMethodSubtype inputMethodSubtype = (InputMethodSubtype) obj;
        return (inputMethodSubtype.mSubtypeId == 0 && this.mSubtypeId == 0) ? inputMethodSubtype.hashCode() == hashCode() && inputMethodSubtype.getLocale().equals(getLocale()) && inputMethodSubtype.getLanguageTag().equals(getLanguageTag()) && inputMethodSubtype.getMode().equals(getMode()) && inputMethodSubtype.getExtraValue().equals(getExtraValue()) && inputMethodSubtype.isAuxiliary() == isAuxiliary() && inputMethodSubtype.overridesImplicitlyEnabledSubtype() == overridesImplicitlyEnabledSubtype() && inputMethodSubtype.isAsciiCapable() == isAsciiCapable() : inputMethodSubtype.hashCode() == hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSubtypeNameResId);
        parcel.writeInt(this.mSubtypeIconResId);
        parcel.writeString(this.mSubtypeLocale);
        parcel.writeString(this.mSubtypeLanguageTag);
        parcel.writeString(this.mSubtypeMode);
        parcel.writeString(this.mSubtypeExtraValue);
        parcel.writeInt(this.mIsAuxiliary ? 1 : 0);
        parcel.writeInt(this.mOverridesImplicitlyEnabledSubtype ? 1 : 0);
        parcel.writeInt(this.mSubtypeHashCode);
        parcel.writeInt(this.mSubtypeId);
        parcel.writeInt(this.mIsAsciiCapable ? 1 : 0);
    }

    private static int hashCodeInternal(String str, String str2, String str3, boolean z, boolean z2, boolean z3) {
        if (!z3) {
            return Arrays.hashCode(new Object[]{str, str2, str3, Boolean.valueOf(z), Boolean.valueOf(z2)});
        }
        return Arrays.hashCode(new Object[]{str, str2, str3, Boolean.valueOf(z), Boolean.valueOf(z2), Boolean.valueOf(z3)});
    }

    public static List<InputMethodSubtype> sort(Context context, int i, InputMethodInfo inputMethodInfo, List<InputMethodSubtype> list) {
        if (inputMethodInfo == null) {
            return list;
        }
        HashSet hashSet = new HashSet(list);
        ArrayList arrayList = new ArrayList();
        int subtypeCount = inputMethodInfo.getSubtypeCount();
        for (int i2 = 0; i2 < subtypeCount; i2++) {
            InputMethodSubtype subtypeAt = inputMethodInfo.getSubtypeAt(i2);
            if (hashSet.contains(subtypeAt)) {
                arrayList.add(subtypeAt);
                hashSet.remove(subtypeAt);
            }
        }
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            arrayList.add((InputMethodSubtype) it.next());
        }
        return arrayList;
    }
}
