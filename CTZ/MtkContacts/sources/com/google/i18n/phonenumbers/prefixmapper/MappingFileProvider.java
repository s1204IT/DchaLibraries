package com.google.i18n.phonenumbers.prefixmapper;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MappingFileProvider implements Externalizable {
    private static final Map<String, String> LOCALE_NORMALIZATION_MAP;
    private List<Set<String>> availableLanguages;
    private int[] countryCallingCodes;
    private int numOfEntries = 0;

    static {
        HashMap map = new HashMap();
        map.put("zh_TW", "zh_Hant");
        map.put("zh_HK", "zh_Hant");
        map.put("zh_MO", "zh_Hant");
        LOCALE_NORMALIZATION_MAP = Collections.unmodifiableMap(map);
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException {
        this.numOfEntries = objectInput.readInt();
        if (this.countryCallingCodes == null || this.countryCallingCodes.length < this.numOfEntries) {
            this.countryCallingCodes = new int[this.numOfEntries];
        }
        if (this.availableLanguages == null) {
            this.availableLanguages = new ArrayList();
        }
        for (int i = 0; i < this.numOfEntries; i++) {
            this.countryCallingCodes[i] = objectInput.readInt();
            int i2 = objectInput.readInt();
            HashSet hashSet = new HashSet();
            for (int i3 = 0; i3 < i2; i3++) {
                hashSet.add(objectInput.readUTF());
            }
            this.availableLanguages.add(hashSet);
        }
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeInt(this.numOfEntries);
        for (int i = 0; i < this.numOfEntries; i++) {
            objectOutput.writeInt(this.countryCallingCodes[i]);
            Set<String> set = this.availableLanguages.get(i);
            objectOutput.writeInt(set.size());
            Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                objectOutput.writeUTF(it.next());
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.numOfEntries; i++) {
            sb.append(this.countryCallingCodes[i]);
            sb.append('|');
            Iterator it = new TreeSet(this.availableLanguages.get(i)).iterator();
            while (it.hasNext()) {
                sb.append((String) it.next());
                sb.append(',');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    String getFileName(int i, String str, String str2, String str3) {
        int iBinarySearch;
        if (str.length() == 0 || (iBinarySearch = Arrays.binarySearch(this.countryCallingCodes, i)) < 0) {
            return "";
        }
        Set<String> set = this.availableLanguages.get(iBinarySearch);
        if (set.size() > 0) {
            String strFindBestMatchingLanguageCode = findBestMatchingLanguageCode(set, str, str2, str3);
            if (strFindBestMatchingLanguageCode.length() > 0) {
                return i + '_' + strFindBestMatchingLanguageCode;
            }
            return "";
        }
        return "";
    }

    private String findBestMatchingLanguageCode(Set<String> set, String str, String str2, String str3) {
        String string = constructFullLocale(str, str2, str3).toString();
        String str4 = LOCALE_NORMALIZATION_MAP.get(string);
        if (str4 != null && set.contains(str4)) {
            return str4;
        }
        if (set.contains(string)) {
            return string;
        }
        if (onlyOneOfScriptOrRegionIsEmpty(str2, str3)) {
            if (set.contains(str)) {
                return str;
            }
            return "";
        }
        if (str2.length() > 0 && str3.length() > 0) {
            String str5 = str + '_' + str2;
            if (set.contains(str5)) {
                return str5;
            }
            String str6 = str + '_' + str3;
            if (set.contains(str6)) {
                return str6;
            }
            if (set.contains(str)) {
                return str;
            }
            return "";
        }
        return "";
    }

    private boolean onlyOneOfScriptOrRegionIsEmpty(String str, String str2) {
        return (str.length() == 0 && str2.length() > 0) || (str2.length() == 0 && str.length() > 0);
    }

    private StringBuilder constructFullLocale(String str, String str2, String str3) {
        StringBuilder sb = new StringBuilder(str);
        appendSubsequentLocalePart(str2, sb);
        appendSubsequentLocalePart(str3, sb);
        return sb;
    }

    private void appendSubsequentLocalePart(String str, StringBuilder sb) {
        if (str.length() > 0) {
            sb.append('_');
            sb.append(str);
        }
    }
}
