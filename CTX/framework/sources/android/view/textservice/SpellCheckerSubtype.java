package android.view.textservice;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.inputmethod.InputMethodUtils;
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
    public static final int SUBTYPE_ID_NONE = 0;
    private static final String SUBTYPE_LANGUAGE_TAG_NONE = "";
    private HashMap<String, String> mExtraValueHashMapCache;
    private final String mSubtypeExtraValue;
    private final int mSubtypeHashCode;
    private final int mSubtypeId;
    private final String mSubtypeLanguageTag;
    private final String mSubtypeLocale;
    private final int mSubtypeNameResId;
    private static final String TAG = SpellCheckerSubtype.class.getSimpleName();
    public static final Parcelable.Creator<SpellCheckerSubtype> CREATOR = new Parcelable.Creator<SpellCheckerSubtype>() {
        @Override
        public SpellCheckerSubtype createFromParcel(Parcel parcel) {
            return new SpellCheckerSubtype(parcel);
        }

        @Override
        public SpellCheckerSubtype[] newArray(int i) {
            return new SpellCheckerSubtype[i];
        }
    };

    public SpellCheckerSubtype(int i, String str, String str2, String str3, int i2) {
        this.mSubtypeNameResId = i;
        this.mSubtypeLocale = str == null ? "" : str;
        this.mSubtypeLanguageTag = str2 == null ? "" : str2;
        this.mSubtypeExtraValue = str3 == null ? "" : str3;
        this.mSubtypeId = i2;
        this.mSubtypeHashCode = this.mSubtypeId != 0 ? this.mSubtypeId : hashCodeInternal(this.mSubtypeLocale, this.mSubtypeExtraValue);
    }

    @Deprecated
    public SpellCheckerSubtype(int i, String str, String str2) {
        this(i, str, "", str2, 0);
    }

    SpellCheckerSubtype(Parcel parcel) {
        this.mSubtypeNameResId = parcel.readInt();
        String string = parcel.readString();
        this.mSubtypeLocale = string == null ? "" : string;
        String string2 = parcel.readString();
        this.mSubtypeLanguageTag = string2 == null ? "" : string2;
        String string3 = parcel.readString();
        this.mSubtypeExtraValue = string3 == null ? "" : string3;
        this.mSubtypeId = parcel.readInt();
        this.mSubtypeHashCode = this.mSubtypeId != 0 ? this.mSubtypeId : hashCodeInternal(this.mSubtypeLocale, this.mSubtypeExtraValue);
    }

    public int getNameResId() {
        return this.mSubtypeNameResId;
    }

    @Deprecated
    public String getLocale() {
        return this.mSubtypeLocale;
    }

    public String getLanguageTag() {
        return this.mSubtypeLanguageTag;
    }

    public String getExtraValue() {
        return this.mSubtypeExtraValue;
    }

    private HashMap<String, String> getExtraValueHashMap() {
        if (this.mExtraValueHashMapCache == null) {
            this.mExtraValueHashMapCache = new HashMap<>();
            for (String str : this.mSubtypeExtraValue.split(EXTRA_VALUE_PAIR_SEPARATOR)) {
                String[] strArrSplit = str.split(EXTRA_VALUE_KEY_VALUE_SEPARATOR);
                if (strArrSplit.length == 1) {
                    this.mExtraValueHashMapCache.put(strArrSplit[0], null);
                } else if (strArrSplit.length > 1) {
                    if (strArrSplit.length > 2) {
                        Slog.w(TAG, "ExtraValue has two or more '='s");
                    }
                    this.mExtraValueHashMapCache.put(strArrSplit[0], strArrSplit[1]);
                }
            }
        }
        return this.mExtraValueHashMapCache;
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

    public boolean equals(Object obj) {
        if (!(obj instanceof SpellCheckerSubtype)) {
            return false;
        }
        SpellCheckerSubtype spellCheckerSubtype = (SpellCheckerSubtype) obj;
        return (spellCheckerSubtype.mSubtypeId == 0 && this.mSubtypeId == 0) ? spellCheckerSubtype.hashCode() == hashCode() && spellCheckerSubtype.getNameResId() == getNameResId() && spellCheckerSubtype.getLocale().equals(getLocale()) && spellCheckerSubtype.getLanguageTag().equals(getLanguageTag()) && spellCheckerSubtype.getExtraValue().equals(getExtraValue()) : spellCheckerSubtype.hashCode() == hashCode();
    }

    public Locale getLocaleObject() {
        if (!TextUtils.isEmpty(this.mSubtypeLanguageTag)) {
            return Locale.forLanguageTag(this.mSubtypeLanguageTag);
        }
        return InputMethodUtils.constructLocaleFromString(this.mSubtypeLocale);
    }

    public CharSequence getDisplayName(Context context, String str, ApplicationInfo applicationInfo) {
        Locale localeObject = getLocaleObject();
        String displayName = localeObject != null ? localeObject.getDisplayName() : this.mSubtypeLocale;
        if (this.mSubtypeNameResId == 0) {
            return displayName;
        }
        CharSequence text = context.getPackageManager().getText(str, this.mSubtypeNameResId, applicationInfo);
        return !TextUtils.isEmpty(text) ? String.format(text.toString(), displayName) : displayName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSubtypeNameResId);
        parcel.writeString(this.mSubtypeLocale);
        parcel.writeString(this.mSubtypeLanguageTag);
        parcel.writeString(this.mSubtypeExtraValue);
        parcel.writeInt(this.mSubtypeId);
    }

    private static int hashCodeInternal(String str, String str2) {
        return Arrays.hashCode(new Object[]{str, str2});
    }

    public static List<SpellCheckerSubtype> sort(Context context, int i, SpellCheckerInfo spellCheckerInfo, List<SpellCheckerSubtype> list) {
        if (spellCheckerInfo == null) {
            return list;
        }
        HashSet hashSet = new HashSet(list);
        ArrayList arrayList = new ArrayList();
        int subtypeCount = spellCheckerInfo.getSubtypeCount();
        for (int i2 = 0; i2 < subtypeCount; i2++) {
            SpellCheckerSubtype subtypeAt = spellCheckerInfo.getSubtypeAt(i2);
            if (hashSet.contains(subtypeAt)) {
                arrayList.add(subtypeAt);
                hashSet.remove(subtypeAt);
            }
        }
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            arrayList.add((SpellCheckerSubtype) it.next());
        }
        return arrayList;
    }
}
