package com.android.quicksearchbox;

import android.text.SpannableString;
import android.text.Spanned;
import com.android.quicksearchbox.util.LevenshteinDistance;

public class LevenshteinSuggestionFormatter extends SuggestionFormatter {
    public LevenshteinSuggestionFormatter(TextAppearanceFactory textAppearanceFactory) {
        super(textAppearanceFactory);
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

    @Override
    public Spanned formatSuggestion(String str, String str2) {
        LevenshteinDistance.Token[] tokenArr = tokenize(normalizeQuery(str));
        LevenshteinDistance.Token[] tokenArr2 = tokenize(str2);
        int[] iArrFindMatches = findMatches(tokenArr, tokenArr2);
        SpannableString spannableString = new SpannableString(str2);
        int length = iArrFindMatches.length;
        for (int i = 0; i < length; i++) {
            LevenshteinDistance.Token token = tokenArr2[i];
            int i2 = iArrFindMatches[i];
            int length2 = i2 >= 0 ? tokenArr[i2].length() : 0;
            applySuggestedTextStyle(spannableString, token.mStart + length2, token.mEnd);
            applyQueryTextStyle(spannableString, token.mStart, length2 + token.mStart);
        }
        return spannableString;
    }

    LevenshteinDistance.Token[] tokenize(String str) {
        int length = str.length();
        char[] charArray = str.toCharArray();
        LevenshteinDistance.Token[] tokenArr = new LevenshteinDistance.Token[length];
        int i = 0;
        int i2 = 0;
        while (i2 < length) {
            while (i2 < length && (charArray[i2] == ' ' || charArray[i2] == '\t')) {
                i2++;
            }
            int i3 = i2;
            while (i3 < length && charArray[i3] != ' ' && charArray[i3] != '\t') {
                i3++;
            }
            if (i2 != i3) {
                tokenArr[i] = new LevenshteinDistance.Token(charArray, i2, i3);
                i++;
            }
            i2 = i3;
        }
        LevenshteinDistance.Token[] tokenArr2 = new LevenshteinDistance.Token[i];
        System.arraycopy(tokenArr, 0, tokenArr2, 0, i);
        return tokenArr2;
    }
}
