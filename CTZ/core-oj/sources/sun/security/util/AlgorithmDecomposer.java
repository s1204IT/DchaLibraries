package sun.security.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import sun.util.locale.LanguageTag;

public class AlgorithmDecomposer {
    private static final Pattern transPattern = Pattern.compile("/");
    private static final Pattern pattern = Pattern.compile("with|and", 2);

    private static Set<String> decomposeImpl(String str) {
        String[] strArrSplit = transPattern.split(str);
        HashSet hashSet = new HashSet();
        for (String str2 : strArrSplit) {
            if (str2 != null && str2.length() != 0) {
                for (String str3 : pattern.split(str2)) {
                    if (str3 != null && str3.length() != 0) {
                        hashSet.add(str3);
                    }
                }
            }
        }
        return hashSet;
    }

    public Set<String> decompose(String str) {
        if (str == null || str.length() == 0) {
            return new HashSet();
        }
        Set<String> setDecomposeImpl = decomposeImpl(str);
        if (setDecomposeImpl.contains("SHA1") && !setDecomposeImpl.contains("SHA-1")) {
            setDecomposeImpl.add("SHA-1");
        }
        if (setDecomposeImpl.contains("SHA-1") && !setDecomposeImpl.contains("SHA1")) {
            setDecomposeImpl.add("SHA1");
        }
        if (setDecomposeImpl.contains("SHA224") && !setDecomposeImpl.contains("SHA-224")) {
            setDecomposeImpl.add("SHA-224");
        }
        if (setDecomposeImpl.contains("SHA-224") && !setDecomposeImpl.contains("SHA224")) {
            setDecomposeImpl.add("SHA224");
        }
        if (setDecomposeImpl.contains("SHA256") && !setDecomposeImpl.contains("SHA-256")) {
            setDecomposeImpl.add("SHA-256");
        }
        if (setDecomposeImpl.contains("SHA-256") && !setDecomposeImpl.contains("SHA256")) {
            setDecomposeImpl.add("SHA256");
        }
        if (setDecomposeImpl.contains("SHA384") && !setDecomposeImpl.contains("SHA-384")) {
            setDecomposeImpl.add("SHA-384");
        }
        if (setDecomposeImpl.contains("SHA-384") && !setDecomposeImpl.contains("SHA384")) {
            setDecomposeImpl.add("SHA384");
        }
        if (setDecomposeImpl.contains("SHA512") && !setDecomposeImpl.contains("SHA-512")) {
            setDecomposeImpl.add("SHA-512");
        }
        if (setDecomposeImpl.contains("SHA-512") && !setDecomposeImpl.contains("SHA512")) {
            setDecomposeImpl.add("SHA512");
        }
        return setDecomposeImpl;
    }

    private static void hasLoop(Set<String> set, String str, String str2) {
        if (set.contains(str)) {
            if (!set.contains(str2)) {
                set.add(str2);
            }
            set.remove(str);
        }
    }

    public static Set<String> decomposeOneHash(String str) {
        if (str == null || str.length() == 0) {
            return new HashSet();
        }
        Set<String> setDecomposeImpl = decomposeImpl(str);
        hasLoop(setDecomposeImpl, "SHA-1", "SHA1");
        hasLoop(setDecomposeImpl, "SHA-224", "SHA224");
        hasLoop(setDecomposeImpl, "SHA-256", "SHA256");
        hasLoop(setDecomposeImpl, "SHA-384", "SHA384");
        hasLoop(setDecomposeImpl, "SHA-512", "SHA512");
        return setDecomposeImpl;
    }

    public static String hashName(String str) {
        return str.replace(LanguageTag.SEP, "");
    }
}
