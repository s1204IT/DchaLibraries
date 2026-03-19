package com.android.i18n.phonenumbers.internal;

import com.android.i18n.phonenumbers.Phonemetadata;

public interface MatcherApi {
    boolean matchesNationalNumber(String str, Phonemetadata.PhoneNumberDesc phoneNumberDesc, boolean z);

    boolean matchesPossibleNumber(String str, Phonemetadata.PhoneNumberDesc phoneNumberDesc);
}
