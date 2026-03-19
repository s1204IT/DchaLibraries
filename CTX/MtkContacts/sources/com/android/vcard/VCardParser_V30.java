package com.android.vcard;

import com.android.vcard.exception.VCardException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class VCardParser_V30 extends VCardParser {
    private final VCardParserImpl_V30 mVCardParserImpl;
    static final Set<String> sKnownPropertyNameSet = Collections.unmodifiableSet(new HashSet(Arrays.asList("BEGIN", "END", "LOGO", "PHOTO", "LABEL", "FN", "TITLE", "SOUND", "VERSION", "TEL", "EMAIL", "TZ", "GEO", "NOTE", "URL", "BDAY", "ROLE", "REV", "UID", "KEY", "MAILER", "NAME", "PROFILE", "SOURCE", "NICKNAME", "CLASS", "SORT-STRING", "CATEGORIES", "PRODID", "IMPP")));
    static final Set<String> sAcceptableEncoding = Collections.unmodifiableSet(new HashSet(Arrays.asList("7BIT", "8BIT", "BASE64", "B")));

    public VCardParser_V30() {
        this.mVCardParserImpl = new VCardParserImpl_V30();
    }

    public VCardParser_V30(int i) {
        this.mVCardParserImpl = new VCardParserImpl_V30(i);
    }

    @Override
    public void addInterpreter(VCardInterpreter vCardInterpreter) {
        this.mVCardParserImpl.addInterpreter(vCardInterpreter);
    }

    @Override
    public void parse(InputStream inputStream) throws VCardException, IOException {
        this.mVCardParserImpl.parse(inputStream);
    }

    @Override
    public void cancel() {
        this.mVCardParserImpl.cancel();
    }
}
