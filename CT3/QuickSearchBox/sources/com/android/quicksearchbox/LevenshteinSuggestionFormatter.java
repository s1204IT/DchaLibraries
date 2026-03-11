package com.android.quicksearchbox;

import android.text.SpannableString;
import android.text.Spanned;
import com.android.quicksearchbox.util.LevenshteinDistance;
import com.google.common.annotations.VisibleForTesting;

public class LevenshteinSuggestionFormatter extends SuggestionFormatter {
    public LevenshteinSuggestionFormatter(TextAppearanceFactory spanFactory) {
        super(spanFactory);
    }

    @Override
    public Spanned formatSuggestion(String query, String suggestion) {
        LevenshteinDistance.Token[] queryTokens = tokenize(normalizeQuery(query));
        LevenshteinDistance.Token[] suggestionTokens = tokenize(suggestion);
        int[] matches = findMatches(queryTokens, suggestionTokens);
        SpannableString str = new SpannableString(suggestion);
        int matchesLen = matches.length;
        for (int i = 0; i < matchesLen; i++) {
            LevenshteinDistance.Token t = suggestionTokens[i];
            int sourceLen = 0;
            int thisMatch = matches[i];
            if (thisMatch >= 0) {
                sourceLen = queryTokens[thisMatch].length();
            }
            applySuggestedTextStyle(str, t.mStart + sourceLen, t.mEnd);
            applyQueryTextStyle(str, t.mStart, t.mStart + sourceLen);
        }
        return str;
    }

    private String normalizeQuery(String query) {
        return query.toLowerCase();
    }

    @VisibleForTesting
    int[] findMatches(LevenshteinDistance.Token[] source, LevenshteinDistance.Token[] target) {
        LevenshteinDistance table = new LevenshteinDistance(source, target);
        table.calculate();
        int targetLen = target.length;
        int[] result = new int[targetLen];
        LevenshteinDistance.EditOperation[] ops = table.getTargetOperations();
        for (int i = 0; i < targetLen; i++) {
            if (ops[i].getType() == 3) {
                result[i] = ops[i].getPosition();
            } else {
                result[i] = -1;
            }
        }
        return result;
    }

    @VisibleForTesting
    LevenshteinDistance.Token[] tokenize(String seq) {
        int tokenCount;
        int pos = 0;
        int len = seq.length();
        char[] chars = seq.toCharArray();
        LevenshteinDistance.Token[] tokens = new LevenshteinDistance.Token[len];
        int tokenCount2 = 0;
        while (pos < len) {
            while (pos < len && (chars[pos] == ' ' || chars[pos] == '\t')) {
                pos++;
            }
            int start = pos;
            while (pos < len && chars[pos] != ' ' && chars[pos] != '\t') {
                pos++;
            }
            int end = pos;
            if (start != pos) {
                tokenCount = tokenCount2 + 1;
                tokens[tokenCount2] = new LevenshteinDistance.Token(chars, start, end);
            } else {
                tokenCount = tokenCount2;
            }
            tokenCount2 = tokenCount;
        }
        LevenshteinDistance.Token[] ret = new LevenshteinDistance.Token[tokenCount2];
        System.arraycopy(tokens, 0, ret, 0, tokenCount2);
        return ret;
    }
}
