package com.android.quicksearchbox;

import android.text.SpannableString;
import android.text.Spanned;
import com.android.quicksearchbox.util.LevenshteinDistance;

/* loaded from: classes.dex */
public class LevenshteinSuggestionFormatter extends SuggestionFormatter {
    public LevenshteinSuggestionFormatter(TextAppearanceFactory textAppearanceFactory) {
        super(textAppearanceFactory);
    }

    /* JADX DEBUG: Method merged with bridge method: formatSuggestion(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/CharSequence; */
    @Override // com.android.quicksearchbox.SuggestionFormatter
    public Spanned formatSuggestion(String str, String str2) {
        int length;
        LevenshteinDistance.Token[] tokenArr = tokenize(normalizeQuery(str));
        LevenshteinDistance.Token[] tokenArr2 = tokenize(str2);
        int[] iArrFindMatches = findMatches(tokenArr, tokenArr2);
        SpannableString spannableString = new SpannableString(str2);
        int length2 = iArrFindMatches.length;
        for (int i = 0; i < length2; i++) {
            LevenshteinDistance.Token token = tokenArr2[i];
            int i2 = iArrFindMatches[i];
            if (i2 >= 0) {
                length = tokenArr[i2].length();
            } else {
                length = 0;
            }
            applySuggestedTextStyle(spannableString, token.mStart + length, token.mEnd);
            applyQueryTextStyle(spannableString, token.mStart, token.mStart + length);
        }
        return spannableString;
    }

    private String normalizeQuery(String str) {
        return str.toLowerCase();
    }

    int[] findMatches(LevenshteinDistance.Token[] tokenArr, LevenshteinDistance.Token[] tokenArr2) {
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance(tokenArr, tokenArr2);
        levenshteinDistance.calculate();
        int length = tokenArr2.length;
        int[] iArr = new int[length];
        LevenshteinDistance.EditOperation[] targetOperations = levenshteinDistance.getTargetOperations();
        for (int i = 0; i < length; i++) {
            if (targetOperations[i].getType() == 3) {
                iArr[i] = targetOperations[i].getPosition();
            } else {
                iArr[i] = -1;
            }
        }
        return iArr;
    }

    LevenshteinDistance.Token[] tokenize(String str) {
        int length = str.length();
        char[] charArray = str.toCharArray();
        LevenshteinDistance.Token[] tokenArr = new LevenshteinDistance.Token[length];
        int i = 0;
        int i2 = 0;
        while (i < length) {
            while (i < length && (charArray[i] == ' ' || charArray[i] == '\t')) {
                i++;
            }
            int i3 = i;
            while (i3 < length && charArray[i3] != ' ' && charArray[i3] != '\t') {
                i3++;
            }
            if (i != i3) {
                tokenArr[i2] = new LevenshteinDistance.Token(charArray, i, i3);
                i2++;
            }
            i = i3;
        }
        LevenshteinDistance.Token[] tokenArr2 = new LevenshteinDistance.Token[i2];
        System.arraycopy(tokenArr, 0, tokenArr2, 0, i2);
        return tokenArr2;
    }
}
