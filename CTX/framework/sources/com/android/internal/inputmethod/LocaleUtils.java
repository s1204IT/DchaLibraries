package com.android.internal.inputmethod;

import android.icu.util.ULocale;
import android.os.LocaleList;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public final class LocaleUtils {

    @VisibleForTesting
    public interface LocaleExtractor<T> {
        Locale get(T t);
    }

    private static byte calculateMatchingSubScore(ULocale uLocale, ULocale uLocale2) {
        if (uLocale.equals(uLocale2)) {
            return (byte) 3;
        }
        String script = uLocale.getScript();
        if (script.isEmpty() || !script.equals(uLocale2.getScript())) {
            return (byte) 1;
        }
        String country = uLocale.getCountry();
        return (country.isEmpty() || !country.equals(uLocale2.getCountry())) ? (byte) 2 : (byte) 3;
    }

    private static final class ScoreEntry implements Comparable<ScoreEntry> {
        public int mIndex = -1;
        public final byte[] mScore;

        ScoreEntry(byte[] bArr, int i) {
            this.mScore = new byte[bArr.length];
            set(bArr, i);
        }

        private void set(byte[] bArr, int i) {
            for (int i2 = 0; i2 < this.mScore.length; i2++) {
                this.mScore[i2] = bArr[i2];
            }
            this.mIndex = i;
        }

        public void updateIfBetter(byte[] bArr, int i) {
            if (compare(this.mScore, bArr) == -1) {
                set(bArr, i);
            }
        }

        private static int compare(byte[] bArr, byte[] bArr2) {
            for (int i = 0; i < bArr.length; i++) {
                if (bArr[i] > bArr2[i]) {
                    return 1;
                }
                if (bArr[i] < bArr2[i]) {
                    return -1;
                }
            }
            return 0;
        }

        @Override
        public int compareTo(ScoreEntry scoreEntry) {
            return (-1) * compare(this.mScore, scoreEntry.mScore);
        }
    }

    @VisibleForTesting
    public static <T> void filterByLanguage(List<T> list, LocaleExtractor<T> localeExtractor, LocaleList localeList, ArrayList<T> arrayList) {
        if (localeList.isEmpty()) {
            return;
        }
        int size = localeList.size();
        HashMap map = new HashMap();
        byte[] bArr = new byte[size];
        ULocale[] uLocaleArr = new ULocale[size];
        int size2 = list.size();
        for (int i = 0; i < size2; i++) {
            Locale locale = localeExtractor.get(list.get(i));
            if (locale != null) {
                boolean z = true;
                for (int i2 = 0; i2 < size; i2++) {
                    Locale locale2 = localeList.get(i2);
                    if (!TextUtils.equals(locale.getLanguage(), locale2.getLanguage())) {
                        bArr[i2] = 0;
                    } else {
                        if (uLocaleArr[i2] == null) {
                            uLocaleArr[i2] = ULocale.addLikelySubtags(ULocale.forLocale(locale2));
                        }
                        bArr[i2] = calculateMatchingSubScore(uLocaleArr[i2], ULocale.addLikelySubtags(ULocale.forLocale(locale)));
                        if (z && bArr[i2] != 0) {
                            z = false;
                        }
                    }
                }
                if (!z) {
                    String language = locale.getLanguage();
                    ScoreEntry scoreEntry = (ScoreEntry) map.get(language);
                    if (scoreEntry == null) {
                        map.put(language, new ScoreEntry(bArr, i));
                    } else {
                        scoreEntry.updateIfBetter(bArr, i);
                    }
                }
            }
        }
        ScoreEntry[] scoreEntryArr = (ScoreEntry[]) map.values().toArray(new ScoreEntry[map.size()]);
        Arrays.sort(scoreEntryArr);
        for (ScoreEntry scoreEntry2 : scoreEntryArr) {
            arrayList.add(list.get(scoreEntry2.mIndex));
        }
    }
}
