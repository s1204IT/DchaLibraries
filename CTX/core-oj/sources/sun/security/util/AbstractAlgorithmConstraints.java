package sun.security.util;

import java.security.AccessController;
import java.security.AlgorithmConstraints;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractAlgorithmConstraints implements AlgorithmConstraints {
    protected final AlgorithmDecomposer decomposer;

    protected AbstractAlgorithmConstraints(AlgorithmDecomposer algorithmDecomposer) {
        this.decomposer = algorithmDecomposer;
    }

    static String[] getAlgorithms(final String str) {
        String[] strArrSplit;
        String strSubstring = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return Security.getProperty(str);
            }
        });
        if (strSubstring != null && !strSubstring.isEmpty()) {
            if (strSubstring.length() >= 2 && strSubstring.charAt(0) == '\"' && strSubstring.charAt(strSubstring.length() - 1) == '\"') {
                strSubstring = strSubstring.substring(1, strSubstring.length() - 1);
            }
            strArrSplit = strSubstring.split(",");
            for (int i = 0; i < strArrSplit.length; i++) {
                strArrSplit[i] = strArrSplit[i].trim();
            }
        } else {
            strArrSplit = null;
        }
        if (strArrSplit == null) {
            return new String[0];
        }
        return strArrSplit;
    }

    static boolean checkAlgorithm(String[] strArr, String str, AlgorithmDecomposer algorithmDecomposer) {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("No algorithm name specified");
        }
        Set<String> setDecompose = null;
        for (String str2 : strArr) {
            if (str2 != null && !str2.isEmpty()) {
                if (str2.equalsIgnoreCase(str)) {
                    return false;
                }
                if (setDecompose == null) {
                    setDecompose = algorithmDecomposer.decompose(str);
                }
                Iterator<String> it = setDecompose.iterator();
                while (it.hasNext()) {
                    if (str2.equalsIgnoreCase(it.next())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
