package com.android.i18n.phonenumbers.internal;

import com.android.i18n.phonenumbers.Phonemetadata;
import com.android.i18n.phonenumbers.RegexCache;
import java.util.regex.Matcher;

public final class RegexBasedMatcher implements MatcherApi {
    private final RegexCache regexCache = new RegexCache(100);

    public static MatcherApi create() {
        return new RegexBasedMatcher();
    }

    private RegexBasedMatcher() {
    }

    @Override
    public boolean matchesNationalNumber(String nationalNumber, Phonemetadata.PhoneNumberDesc numberDesc, boolean allowPrefixMatch) {
        Matcher nationalNumberPatternMatcher = this.regexCache.getPatternForRegex(numberDesc.getNationalNumberPattern()).matcher(nationalNumber);
        if (nationalNumberPatternMatcher.matches()) {
            return true;
        }
        if (allowPrefixMatch) {
            return nationalNumberPatternMatcher.lookingAt();
        }
        return false;
    }

    @Override
    public boolean matchesPossibleNumber(String nationalNumber, Phonemetadata.PhoneNumberDesc numberDesc) {
        Matcher possibleNumberPatternMatcher = this.regexCache.getPatternForRegex(numberDesc.getPossibleNumberPattern()).matcher(nationalNumber);
        return possibleNumberPatternMatcher.matches();
    }
}
