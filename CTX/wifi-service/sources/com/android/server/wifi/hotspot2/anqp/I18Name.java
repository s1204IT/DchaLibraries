package com.android.server.wifi.hotspot2.anqp;

import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class I18Name {

    @VisibleForTesting
    public static final int LANGUAGE_CODE_LENGTH = 3;

    @VisibleForTesting
    public static final int MINIMUM_LENGTH = 3;
    private final String mLanguage;
    private final Locale mLocale;
    private final String mText;

    @VisibleForTesting
    public I18Name(String str, Locale locale, String str2) {
        this.mLanguage = str;
        this.mLocale = locale;
        this.mText = str2;
    }

    public static I18Name parse(ByteBuffer byteBuffer) throws ProtocolException {
        int i = byteBuffer.get() & 255;
        if (i < 3) {
            throw new ProtocolException("Invalid length: " + i);
        }
        String strTrim = ByteBufferReader.readString(byteBuffer, 3, StandardCharsets.US_ASCII).trim();
        return new I18Name(strTrim, Locale.forLanguageTag(strTrim), ByteBufferReader.readString(byteBuffer, i - 3, StandardCharsets.UTF_8));
    }

    public String getLanguage() {
        return this.mLanguage;
    }

    public Locale getLocale() {
        return this.mLocale;
    }

    public String getText() {
        return this.mText;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof I18Name)) {
            return false;
        }
        I18Name i18Name = (I18Name) obj;
        return TextUtils.equals(this.mLanguage, i18Name.mLanguage) && TextUtils.equals(this.mText, i18Name.mText);
    }

    public int hashCode() {
        return (31 * this.mLanguage.hashCode()) + this.mText.hashCode();
    }

    public String toString() {
        return this.mText + ':' + this.mLocale.getLanguage();
    }
}
