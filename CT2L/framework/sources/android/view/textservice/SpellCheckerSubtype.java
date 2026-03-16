package android.view.textservice;

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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class SpellCheckerSubtype implements Parcelable {
    private static final String EXTRA_VALUE_KEY_VALUE_SEPARATOR = "=";
    private static final String EXTRA_VALUE_PAIR_SEPARATOR = ",";
    private HashMap<String, String> mExtraValueHashMapCache;
    private final String mSubtypeExtraValue;
    private final int mSubtypeHashCode;
    private final String mSubtypeLocale;
    private final int mSubtypeNameResId;
    private static final String TAG = SpellCheckerSubtype.class.getSimpleName();
    public static final Parcelable.Creator<SpellCheckerSubtype> CREATOR = new Parcelable.Creator<SpellCheckerSubtype>() {
        @Override
        public SpellCheckerSubtype createFromParcel(Parcel source) {
            return new SpellCheckerSubtype(source);
        }

        @Override
        public SpellCheckerSubtype[] newArray(int size) {
            return new SpellCheckerSubtype[size];
        }
    };

    public SpellCheckerSubtype(int nameId, String locale, String extraValue) {
        this.mSubtypeNameResId = nameId;
        this.mSubtypeLocale = locale == null ? ProxyInfo.LOCAL_EXCL_LIST : locale;
        this.mSubtypeExtraValue = extraValue == null ? ProxyInfo.LOCAL_EXCL_LIST : extraValue;
        this.mSubtypeHashCode = hashCodeInternal(this.mSubtypeLocale, this.mSubtypeExtraValue);
    }

    SpellCheckerSubtype(Parcel source) {
        this.mSubtypeNameResId = source.readInt();
        String s = source.readString();
        this.mSubtypeLocale = s == null ? ProxyInfo.LOCAL_EXCL_LIST : s;
        String s2 = source.readString();
        this.mSubtypeExtraValue = s2 == null ? ProxyInfo.LOCAL_EXCL_LIST : s2;
        this.mSubtypeHashCode = hashCodeInternal(this.mSubtypeLocale, this.mSubtypeExtraValue);
    }

    public int getNameResId() {
        return this.mSubtypeNameResId;
    }

    public String getLocale() {
        return this.mSubtypeLocale;
    }

    public String getExtraValue() {
        return this.mSubtypeExtraValue;
    }

    private HashMap<String, String> getExtraValueHashMap() {
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
        if (!(o instanceof SpellCheckerSubtype)) {
            return false;
        }
        SpellCheckerSubtype subtype = (SpellCheckerSubtype) o;
        return subtype.hashCode() == hashCode() && subtype.getNameResId() == getNameResId() && subtype.getLocale().equals(getLocale()) && subtype.getExtraValue().equals(getExtraValue());
    }

    public static Locale constructLocaleFromString(String localeStr) {
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

    public CharSequence getDisplayName(Context context, String packageName, ApplicationInfo appInfo) {
        Locale locale = constructLocaleFromString(this.mSubtypeLocale);
        String localeStr = locale != null ? locale.getDisplayName() : this.mSubtypeLocale;
        if (this.mSubtypeNameResId != 0) {
            CharSequence subtypeName = context.getPackageManager().getText(packageName, this.mSubtypeNameResId, appInfo);
            if (!TextUtils.isEmpty(subtypeName)) {
                return String.format(subtypeName.toString(), localeStr);
            }
            return localeStr;
        }
        return localeStr;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeInt(this.mSubtypeNameResId);
        dest.writeString(this.mSubtypeLocale);
        dest.writeString(this.mSubtypeExtraValue);
    }

    private static int hashCodeInternal(String locale, String extraValue) {
        return Arrays.hashCode(new Object[]{locale, extraValue});
    }

    public static List<SpellCheckerSubtype> sort(Context context, int flags, SpellCheckerInfo sci, List<SpellCheckerSubtype> subtypeList) {
        if (sci != null) {
            HashSet<SpellCheckerSubtype> subtypesSet = new HashSet<>(subtypeList);
            ArrayList<SpellCheckerSubtype> sortedList = new ArrayList<>();
            int N = sci.getSubtypeCount();
            for (int i = 0; i < N; i++) {
                SpellCheckerSubtype subtype = sci.getSubtypeAt(i);
                if (subtypesSet.contains(subtype)) {
                    sortedList.add(subtype);
                    subtypesSet.remove(subtype);
                }
            }
            Iterator<SpellCheckerSubtype> it = subtypesSet.iterator();
            while (it.hasNext()) {
                sortedList.add(it.next());
            }
            return sortedList;
        }
        return subtypeList;
    }
}
