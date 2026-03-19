package sun.util.locale;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LocaleMatcher {
    public static List<Locale> filter(List<Locale.LanguageRange> list, Collection<Locale> collection, Locale.FilteringMode filteringMode) {
        if (list.isEmpty() || collection.isEmpty()) {
            return new ArrayList();
        }
        ArrayList arrayList = new ArrayList();
        Iterator<Locale> it = collection.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().toLanguageTag());
        }
        List<String> listFilterTags = filterTags(list, arrayList, filteringMode);
        ArrayList arrayList2 = new ArrayList(listFilterTags.size());
        Iterator<String> it2 = listFilterTags.iterator();
        while (it2.hasNext()) {
            arrayList2.add(Locale.forLanguageTag(it2.next()));
        }
        return arrayList2;
    }

    public static List<String> filterTags(List<Locale.LanguageRange> list, Collection<String> collection, Locale.FilteringMode filteringMode) {
        String strReplaceAll;
        if (list.isEmpty() || collection.isEmpty()) {
            return new ArrayList();
        }
        if (filteringMode == Locale.FilteringMode.EXTENDED_FILTERING) {
            return filterExtended(list, collection);
        }
        ArrayList arrayList = new ArrayList();
        for (Locale.LanguageRange languageRange : list) {
            String range = languageRange.getRange();
            if (range.startsWith("*-") || range.indexOf("-*") != -1) {
                if (filteringMode == Locale.FilteringMode.AUTOSELECT_FILTERING) {
                    return filterExtended(list, collection);
                }
                if (filteringMode == Locale.FilteringMode.MAP_EXTENDED_RANGES) {
                    if (range.charAt(0) == '*') {
                        strReplaceAll = "*";
                    } else {
                        strReplaceAll = range.replaceAll("-[*]", "");
                    }
                    arrayList.add(new Locale.LanguageRange(strReplaceAll, languageRange.getWeight()));
                } else if (filteringMode == Locale.FilteringMode.REJECT_EXTENDED_RANGES) {
                    throw new IllegalArgumentException("An extended range \"" + range + "\" found in REJECT_EXTENDED_RANGES mode.");
                }
            } else {
                arrayList.add(languageRange);
            }
        }
        return filterBasic(arrayList, collection);
    }

    private static List<String> filterBasic(List<Locale.LanguageRange> list, Collection<String> collection) {
        int length;
        ArrayList arrayList = new ArrayList();
        Iterator<Locale.LanguageRange> it = list.iterator();
        while (it.hasNext()) {
            String range = it.next().getRange();
            if (range.equals("*")) {
                return new ArrayList(collection);
            }
            Iterator<String> it2 = collection.iterator();
            while (it2.hasNext()) {
                String lowerCase = it2.next().toLowerCase();
                if (lowerCase.startsWith(range) && (lowerCase.length() == (length = range.length()) || lowerCase.charAt(length) == '-')) {
                    if (!arrayList.contains(lowerCase)) {
                        arrayList.add(lowerCase);
                    }
                }
            }
        }
        return arrayList;
    }

    private static List<String> filterExtended(List<Locale.LanguageRange> list, Collection<String> collection) {
        ArrayList arrayList = new ArrayList();
        Iterator<Locale.LanguageRange> it = list.iterator();
        while (it.hasNext()) {
            String range = it.next().getRange();
            if (range.equals("*")) {
                return new ArrayList(collection);
            }
            String[] strArrSplit = range.split(LanguageTag.SEP);
            Iterator<String> it2 = collection.iterator();
            while (it2.hasNext()) {
                String lowerCase = it2.next().toLowerCase();
                String[] strArrSplit2 = lowerCase.split(LanguageTag.SEP);
                if (strArrSplit[0].equals(strArrSplit2[0]) || strArrSplit[0].equals("*")) {
                    int i = 1;
                    int i2 = 1;
                    while (i < strArrSplit.length && i2 < strArrSplit2.length) {
                        if (strArrSplit[i].equals("*")) {
                            i++;
                        } else if (!strArrSplit[i].equals(strArrSplit2[i2])) {
                            if (strArrSplit2[i2].length() == 1 && !strArrSplit2[i2].equals("*")) {
                                break;
                            }
                            i2++;
                        } else {
                            i++;
                            i2++;
                        }
                    }
                    if (strArrSplit.length == i && !arrayList.contains(lowerCase)) {
                        arrayList.add(lowerCase);
                    }
                }
            }
        }
        return arrayList;
    }

    public static Locale lookup(List<Locale.LanguageRange> list, Collection<Locale> collection) {
        if (list.isEmpty() || collection.isEmpty()) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        Iterator<Locale> it = collection.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().toLanguageTag());
        }
        String strLookupTag = lookupTag(list, arrayList);
        if (strLookupTag == null) {
            return null;
        }
        return Locale.forLanguageTag(strLookupTag);
    }

    public static String lookupTag(List<Locale.LanguageRange> list, Collection<String> collection) {
        if (list.isEmpty() || collection.isEmpty()) {
            return null;
        }
        Iterator<Locale.LanguageRange> it = list.iterator();
        while (it.hasNext()) {
            String range = it.next().getRange();
            if (!range.equals("*")) {
                String strReplace = range.replace("*", "\\p{Alnum}*");
                while (strReplace.length() > 0) {
                    Iterator<String> it2 = collection.iterator();
                    while (it2.hasNext()) {
                        String lowerCase = it2.next().toLowerCase();
                        if (lowerCase.matches(strReplace)) {
                            return lowerCase;
                        }
                    }
                    int iLastIndexOf = strReplace.lastIndexOf(45);
                    if (iLastIndexOf >= 0) {
                        strReplace = strReplace.substring(0, iLastIndexOf);
                        if (strReplace.lastIndexOf(45) == strReplace.length() - 2) {
                            strReplace = strReplace.substring(0, strReplace.length() - 2);
                        }
                    } else {
                        strReplace = "";
                    }
                }
            }
        }
        return null;
    }

    public static List<Locale.LanguageRange> parse(String str) {
        String lowerCase = str.replace(" ", "").toLowerCase();
        if (lowerCase.startsWith("accept-language:")) {
            lowerCase = lowerCase.substring(16);
        }
        String[] strArrSplit = lowerCase.split(",");
        ArrayList arrayList = new ArrayList(strArrSplit.length);
        ArrayList arrayList2 = new ArrayList();
        int length = strArrSplit.length;
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            String str2 = strArrSplit[i2];
            int iIndexOf = str2.indexOf(";q=");
            double d = 1.0d;
            if (iIndexOf != -1) {
                String strSubstring = str2.substring(0, iIndexOf);
                int i3 = iIndexOf + 3;
                try {
                    double d2 = Double.parseDouble(str2.substring(i3));
                    if (d2 < 0.0d || d2 > 1.0d) {
                        throw new IllegalArgumentException("weight=" + d2 + " for language range \"" + strSubstring + "\". It must be between 0.0 and 1.0.");
                    }
                    str2 = strSubstring;
                    d = d2;
                } catch (Exception e) {
                    throw new IllegalArgumentException("weight=\"" + str2.substring(i3) + "\" for language range \"" + strSubstring + "\"");
                }
            }
            if (!arrayList2.contains(str2)) {
                Locale.LanguageRange languageRange = new Locale.LanguageRange(str2, d);
                int i4 = 0;
                while (true) {
                    if (i4 >= i) {
                        i4 = i;
                        break;
                    }
                    if (((Locale.LanguageRange) arrayList.get(i4)).getWeight() < d) {
                        break;
                    }
                    i4++;
                }
                arrayList.add(i4, languageRange);
                i++;
                arrayList2.add(str2);
                String equivalentForRegionAndVariant = getEquivalentForRegionAndVariant(str2);
                if (equivalentForRegionAndVariant != null && !arrayList2.contains(equivalentForRegionAndVariant)) {
                    arrayList.add(i4 + 1, new Locale.LanguageRange(equivalentForRegionAndVariant, d));
                    i++;
                    arrayList2.add(equivalentForRegionAndVariant);
                }
                String[] equivalentsForLanguage = getEquivalentsForLanguage(str2);
                if (equivalentsForLanguage != null) {
                    int i5 = i;
                    for (String str3 : equivalentsForLanguage) {
                        if (!arrayList2.contains(str3)) {
                            arrayList.add(i4 + 1, new Locale.LanguageRange(str3, d));
                            i5++;
                            arrayList2.add(str3);
                        }
                        String equivalentForRegionAndVariant2 = getEquivalentForRegionAndVariant(str3);
                        if (equivalentForRegionAndVariant2 != null && !arrayList2.contains(equivalentForRegionAndVariant2)) {
                            arrayList.add(i4 + 1, new Locale.LanguageRange(equivalentForRegionAndVariant2, d));
                            i5++;
                            arrayList2.add(equivalentForRegionAndVariant2);
                        }
                    }
                    i = i5;
                }
            }
        }
        return arrayList;
    }

    private static String replaceFirstSubStringMatch(String str, String str2, String str3) {
        int iIndexOf = str.indexOf(str2);
        if (iIndexOf == -1) {
            return str;
        }
        return str.substring(0, iIndexOf) + str3 + str.substring(iIndexOf + str2.length());
    }

    private static String[] getEquivalentsForLanguage(String str) {
        String strSubstring = str;
        while (strSubstring.length() > 0) {
            if (LocaleEquivalentMaps.singleEquivMap.containsKey(strSubstring)) {
                return new String[]{replaceFirstSubStringMatch(str, strSubstring, LocaleEquivalentMaps.singleEquivMap.get(strSubstring))};
            }
            if (LocaleEquivalentMaps.multiEquivsMap.containsKey(strSubstring)) {
                String[] strArr = LocaleEquivalentMaps.multiEquivsMap.get(strSubstring);
                String[] strArr2 = new String[strArr.length];
                for (int i = 0; i < strArr.length; i++) {
                    strArr2[i] = replaceFirstSubStringMatch(str, strSubstring, strArr[i]);
                }
                return strArr2;
            }
            int iLastIndexOf = strSubstring.lastIndexOf(45);
            if (iLastIndexOf != -1) {
                strSubstring = strSubstring.substring(0, iLastIndexOf);
            } else {
                return null;
            }
        }
        return null;
    }

    private static String getEquivalentForRegionAndVariant(String str) {
        int extentionKeyIndex = getExtentionKeyIndex(str);
        for (String str2 : LocaleEquivalentMaps.regionVariantEquivMap.keySet()) {
            int iIndexOf = str.indexOf(str2);
            if (iIndexOf != -1 && (extentionKeyIndex == Integer.MIN_VALUE || iIndexOf <= extentionKeyIndex)) {
                int length = iIndexOf + str2.length();
                if (str.length() == length || str.charAt(length) == '-') {
                    return replaceFirstSubStringMatch(str, str2, LocaleEquivalentMaps.regionVariantEquivMap.get(str2));
                }
            }
        }
        return null;
    }

    private static int getExtentionKeyIndex(String str) {
        char[] charArray = str.toCharArray();
        int i = Integer.MIN_VALUE;
        for (int i2 = 1; i2 < charArray.length; i2++) {
            if (charArray[i2] == '-') {
                if (i2 - i != 2) {
                    i = i2;
                } else {
                    return i;
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    public static List<Locale.LanguageRange> mapEquivalents(List<Locale.LanguageRange> list, Map<String, List<String>> map) {
        boolean z;
        if (list.isEmpty()) {
            return new ArrayList();
        }
        if (map == null || map.isEmpty()) {
            return new ArrayList(list);
        }
        HashMap map2 = new HashMap();
        for (String str : map.keySet()) {
            map2.put(str.toLowerCase(), str);
        }
        ArrayList arrayList = new ArrayList();
        for (Locale.LanguageRange languageRange : list) {
            String range = languageRange.getRange();
            String strSubstring = range;
            while (true) {
                z = false;
                if (strSubstring.length() <= 0) {
                    break;
                }
                if (map2.containsKey(strSubstring)) {
                    z = true;
                    List<String> list2 = map.get(map2.get(strSubstring));
                    if (list2 != null) {
                        int length = strSubstring.length();
                        Iterator<String> it = list2.iterator();
                        while (it.hasNext()) {
                            arrayList.add(new Locale.LanguageRange(it.next().toLowerCase() + range.substring(length), languageRange.getWeight()));
                        }
                    }
                } else {
                    int iLastIndexOf = strSubstring.lastIndexOf(45);
                    if (iLastIndexOf == -1) {
                        break;
                    }
                    strSubstring = strSubstring.substring(0, iLastIndexOf);
                }
            }
            if (!z) {
                arrayList.add(languageRange);
            }
        }
        return arrayList;
    }

    private LocaleMatcher() {
    }
}
