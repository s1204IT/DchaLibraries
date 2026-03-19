package android.os;

import android.icu.util.ULocale;
import android.os.Parcelable;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public final class LocaleList implements Parcelable {
    private static final int NUM_PSEUDO_LOCALES = 2;
    private static final String STRING_AR_XB = "ar-XB";
    private static final String STRING_EN_XA = "en-XA";
    private final Locale[] mList;
    private final String mStringRepresentation;
    private static final Locale[] sEmptyList = new Locale[0];
    private static final LocaleList sEmptyLocaleList = new LocaleList(new Locale[0]);
    public static final Parcelable.Creator<LocaleList> CREATOR = new Parcelable.Creator<LocaleList>() {
        @Override
        public LocaleList createFromParcel(Parcel parcel) {
            return LocaleList.forLanguageTags(parcel.readString());
        }

        @Override
        public LocaleList[] newArray(int i) {
            return new LocaleList[i];
        }
    };
    private static final Locale LOCALE_EN_XA = new Locale("en", "XA");
    private static final Locale LOCALE_AR_XB = new Locale("ar", "XB");
    private static final Locale EN_LATN = Locale.forLanguageTag("en-Latn");
    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    private static LocaleList sLastExplicitlySetLocaleList = null;

    @GuardedBy("sLock")
    private static LocaleList sDefaultLocaleList = null;

    @GuardedBy("sLock")
    private static LocaleList sDefaultAdjustedLocaleList = null;

    @GuardedBy("sLock")
    private static Locale sLastDefaultLocale = null;

    public Locale get(int i) {
        if (i < 0 || i >= this.mList.length) {
            return null;
        }
        return this.mList[i];
    }

    public boolean isEmpty() {
        return this.mList.length == 0;
    }

    public int size() {
        return this.mList.length;
    }

    public int indexOf(Locale locale) {
        for (int i = 0; i < this.mList.length; i++) {
            if (this.mList[i].equals(locale)) {
                return i;
            }
        }
        return -1;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LocaleList)) {
            return false;
        }
        Locale[] localeArr = ((LocaleList) obj).mList;
        if (this.mList.length != localeArr.length) {
            return false;
        }
        for (int i = 0; i < this.mList.length; i++) {
            if (!this.mList[i].equals(localeArr[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int iHashCode = 1;
        for (int i = 0; i < this.mList.length; i++) {
            iHashCode = this.mList[i].hashCode() + (31 * iHashCode);
        }
        return iHashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < this.mList.length; i++) {
            sb.append(this.mList[i]);
            if (i < this.mList.length - 1) {
                sb.append(',');
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mStringRepresentation);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        for (int i = 0; i < this.mList.length; i++) {
            Locale locale = this.mList[i];
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1138166333441L, locale.getLanguage());
            protoOutputStream.write(1138166333442L, locale.getCountry());
            protoOutputStream.write(1138166333443L, locale.getVariant());
            protoOutputStream.end(jStart);
        }
    }

    public String toLanguageTags() {
        return this.mStringRepresentation;
    }

    public LocaleList(Locale... localeArr) {
        if (localeArr.length == 0) {
            this.mList = sEmptyList;
            this.mStringRepresentation = "";
            return;
        }
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = new HashSet();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < localeArr.length; i++) {
            Locale locale = localeArr[i];
            if (locale == null) {
                throw new NullPointerException("list[" + i + "] is null");
            }
            if (!hashSet.contains(locale)) {
                Locale locale2 = (Locale) locale.clone();
                arrayList.add(locale2);
                sb.append(locale2.toLanguageTag());
                if (i < localeArr.length - 1) {
                    sb.append(',');
                }
                hashSet.add(locale2);
            }
        }
        this.mList = (Locale[]) arrayList.toArray(new Locale[arrayList.size()]);
        this.mStringRepresentation = sb.toString();
    }

    public LocaleList(Locale locale, LocaleList localeList) {
        int length;
        if (locale == null) {
            throw new NullPointerException("topLocale is null");
        }
        if (localeList != null) {
            length = localeList.mList.length;
        } else {
            length = 0;
        }
        int i = 0;
        while (true) {
            if (i < length) {
                if (locale.equals(localeList.mList[i])) {
                    break;
                } else {
                    i++;
                }
            } else {
                i = -1;
                break;
            }
        }
        int i2 = (i == -1 ? 1 : 0) + length;
        Locale[] localeArr = new Locale[i2];
        localeArr[0] = (Locale) locale.clone();
        if (i == -1) {
            int i3 = 0;
            while (i3 < length) {
                int i4 = i3 + 1;
                localeArr[i4] = (Locale) localeList.mList[i3].clone();
                i3 = i4;
            }
        } else {
            int i5 = 0;
            while (i5 < i) {
                int i6 = i5 + 1;
                localeArr[i6] = (Locale) localeList.mList[i5].clone();
                i5 = i6;
            }
            for (int i7 = i + 1; i7 < length; i7++) {
                localeArr[i7] = (Locale) localeList.mList[i7].clone();
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i8 = 0; i8 < i2; i8++) {
            sb.append(localeArr[i8].toLanguageTag());
            if (i8 < i2 - 1) {
                sb.append(',');
            }
        }
        this.mList = localeArr;
        this.mStringRepresentation = sb.toString();
    }

    public static LocaleList getEmptyLocaleList() {
        return sEmptyLocaleList;
    }

    public static LocaleList forLanguageTags(String str) {
        if (str == null || str.equals("")) {
            return getEmptyLocaleList();
        }
        String[] strArrSplit = str.split(",");
        Locale[] localeArr = new Locale[strArrSplit.length];
        for (int i = 0; i < localeArr.length; i++) {
            localeArr[i] = Locale.forLanguageTag(strArrSplit[i]);
        }
        return new LocaleList(localeArr);
    }

    private static String getLikelyScript(Locale locale) {
        String script = locale.getScript();
        if (!script.isEmpty()) {
            return script;
        }
        return ULocale.addLikelySubtags(ULocale.forLocale(locale)).getScript();
    }

    private static boolean isPseudoLocale(String str) {
        return STRING_EN_XA.equals(str) || STRING_AR_XB.equals(str);
    }

    public static boolean isPseudoLocale(Locale locale) {
        return LOCALE_EN_XA.equals(locale) || LOCALE_AR_XB.equals(locale);
    }

    private static int matchScore(Locale locale, Locale locale2) {
        if (locale.equals(locale2)) {
            return 1;
        }
        if (!locale.getLanguage().equals(locale2.getLanguage()) || isPseudoLocale(locale) || isPseudoLocale(locale2)) {
            return 0;
        }
        String likelyScript = getLikelyScript(locale);
        if (!likelyScript.isEmpty()) {
            return likelyScript.equals(getLikelyScript(locale2)) ? 1 : 0;
        }
        String country = locale.getCountry();
        return (country.isEmpty() || country.equals(locale2.getCountry())) ? 1 : 0;
    }

    private int findFirstMatchIndex(Locale locale) {
        for (int i = 0; i < this.mList.length; i++) {
            if (matchScore(locale, this.mList[i]) > 0) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private int computeFirstMatchIndex(Collection<String> collection, boolean z) {
        int iFindFirstMatchIndex;
        if (this.mList.length == 1) {
            return 0;
        }
        if (this.mList.length == 0) {
            return -1;
        }
        if (z) {
            iFindFirstMatchIndex = findFirstMatchIndex(EN_LATN);
            if (iFindFirstMatchIndex == 0) {
                return 0;
            }
            if (iFindFirstMatchIndex >= Integer.MAX_VALUE) {
            }
        } else {
            iFindFirstMatchIndex = Integer.MAX_VALUE;
        }
        Iterator<String> it = collection.iterator();
        while (it.hasNext()) {
            int iFindFirstMatchIndex2 = findFirstMatchIndex(Locale.forLanguageTag(it.next()));
            if (iFindFirstMatchIndex2 == 0) {
                return 0;
            }
            if (iFindFirstMatchIndex2 < iFindFirstMatchIndex) {
                iFindFirstMatchIndex = iFindFirstMatchIndex2;
            }
        }
        if (iFindFirstMatchIndex == Integer.MAX_VALUE) {
            return 0;
        }
        return iFindFirstMatchIndex;
    }

    private Locale computeFirstMatch(Collection<String> collection, boolean z) {
        int iComputeFirstMatchIndex = computeFirstMatchIndex(collection, z);
        if (iComputeFirstMatchIndex == -1) {
            return null;
        }
        return this.mList[iComputeFirstMatchIndex];
    }

    public Locale getFirstMatch(String[] strArr) {
        return computeFirstMatch(Arrays.asList(strArr), false);
    }

    public int getFirstMatchIndex(String[] strArr) {
        return computeFirstMatchIndex(Arrays.asList(strArr), false);
    }

    public Locale getFirstMatchWithEnglishSupported(String[] strArr) {
        return computeFirstMatch(Arrays.asList(strArr), true);
    }

    public int getFirstMatchIndexWithEnglishSupported(Collection<String> collection) {
        return computeFirstMatchIndex(collection, true);
    }

    public int getFirstMatchIndexWithEnglishSupported(String[] strArr) {
        return getFirstMatchIndexWithEnglishSupported(Arrays.asList(strArr));
    }

    public static boolean isPseudoLocalesOnly(String[] strArr) {
        if (strArr == null) {
            return true;
        }
        if (strArr.length > 3) {
            return false;
        }
        for (String str : strArr) {
            if (!str.isEmpty() && !isPseudoLocale(str)) {
                return false;
            }
        }
        return true;
    }

    public static LocaleList getDefault() {
        Locale locale = Locale.getDefault();
        synchronized (sLock) {
            if (!locale.equals(sLastDefaultLocale)) {
                sLastDefaultLocale = locale;
                if (sDefaultLocaleList != null && locale.equals(sDefaultLocaleList.get(0))) {
                    return sDefaultLocaleList;
                }
                sDefaultLocaleList = new LocaleList(locale, sLastExplicitlySetLocaleList);
                sDefaultAdjustedLocaleList = sDefaultLocaleList;
            }
            return sDefaultLocaleList;
        }
    }

    public static LocaleList getAdjustedDefault() {
        LocaleList localeList;
        getDefault();
        synchronized (sLock) {
            localeList = sDefaultAdjustedLocaleList;
        }
        return localeList;
    }

    public static void setDefault(LocaleList localeList) {
        setDefault(localeList, 0);
    }

    public static void setDefault(LocaleList localeList, int i) {
        if (localeList == null) {
            throw new NullPointerException("locales is null");
        }
        if (localeList.isEmpty()) {
            throw new IllegalArgumentException("locales is empty");
        }
        synchronized (sLock) {
            sLastDefaultLocale = localeList.get(i);
            Locale.setDefault(sLastDefaultLocale);
            sLastExplicitlySetLocaleList = localeList;
            sDefaultLocaleList = localeList;
            if (i == 0) {
                sDefaultAdjustedLocaleList = sDefaultLocaleList;
            } else {
                sDefaultAdjustedLocaleList = new LocaleList(sLastDefaultLocale, sDefaultLocaleList);
            }
        }
    }
}
