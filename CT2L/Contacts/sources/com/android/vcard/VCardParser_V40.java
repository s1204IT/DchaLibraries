package com.android.vcard;

import com.android.vcard.exception.VCardException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class VCardParser_V40 extends VCardParser {
    private final VCardParserImpl_V40 mVCardParserImpl;
    static final Set<String> sKnownPropertyNameSet = Collections.unmodifiableSet(new HashSet(Arrays.asList("BEGIN", "END", "VERSION", "SOURCE", "KIND", "FN", "N", "NICKNAME", "PHOTO", "BDAY", "ANNIVERSARY", "GENDER", "ADR", "TEL", "EMAIL", "IMPP", "LANG", "TZ", "GEO", "TITLE", "ROLE", "LOGO", "ORG", "MEMBER", "RELATED", "CATEGORIES", "NOTE", "PRODID", "REV", "SOUND", "UID", "CLIENTPIDMAP", "URL", "KEY", "FBURL", "CALENDRURI", "CALURI", "XML")));
    static final Set<String> sAcceptableEncoding = Collections.unmodifiableSet(new HashSet(Arrays.asList("8BIT", "B")));

    @Override
    public void addInterpreter(VCardInterpreter interpreter) {
        this.mVCardParserImpl.addInterpreter(interpreter);
    }

    @Override
    public void parse(InputStream is) throws VCardException, IOException {
        this.mVCardParserImpl.parse(is);
    }

    @Override
    public void cancel() {
        this.mVCardParserImpl.cancel();
    }
}
