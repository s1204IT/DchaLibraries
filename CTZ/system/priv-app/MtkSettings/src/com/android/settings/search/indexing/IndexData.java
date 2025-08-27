package com.android.settings.search.indexing;

import java.text.Normalizer;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public class IndexData {
    private static final Pattern REMOVE_DIACRITICALS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    public final String updatedSummaryOn;
    public final String updatedTitle;

    public String toString() {
        return this.updatedTitle + ": " + this.updatedSummaryOn;
    }

    public static String normalizeJapaneseString(String str) {
        String strNormalize = Normalizer.normalize(str != null ? str.replaceAll("-", "") : "", Normalizer.Form.NFKD);
        StringBuffer stringBuffer = new StringBuffer();
        int length = strNormalize.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = strNormalize.charAt(i);
            if (cCharAt >= 12353 && cCharAt <= 12438) {
                stringBuffer.append((char) ((cCharAt - 12353) + 12449));
            } else {
                stringBuffer.append(cCharAt);
            }
        }
        return REMOVE_DIACRITICALS_PATTERN.matcher(stringBuffer.toString()).replaceAll("").toLowerCase();
    }
}
