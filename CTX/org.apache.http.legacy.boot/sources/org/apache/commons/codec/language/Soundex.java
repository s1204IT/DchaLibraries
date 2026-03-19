package org.apache.commons.codec.language;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;

@Deprecated
public class Soundex implements StringEncoder {
    private int maxLength;
    private char[] soundexMapping;
    public static final Soundex US_ENGLISH = new Soundex();
    public static final String US_ENGLISH_MAPPING_STRING = "01230120022455012623010202";
    public static final char[] US_ENGLISH_MAPPING = US_ENGLISH_MAPPING_STRING.toCharArray();

    public int difference(String str, String str2) throws EncoderException {
        return SoundexUtils.difference(this, str, str2);
    }

    public Soundex() {
        this(US_ENGLISH_MAPPING);
    }

    public Soundex(char[] cArr) {
        this.maxLength = 4;
        setSoundexMapping(cArr);
    }

    @Override
    public Object encode(Object obj) throws EncoderException {
        if (obj instanceof String) {
            return soundex(obj);
        }
        throw new EncoderException("Parameter supplied to Soundex encode is not of type java.lang.String");
    }

    @Override
    public String encode(String str) {
        return soundex(str);
    }

    private char getMappingCode(String str, int i) {
        char cCharAt;
        char map = map(str.charAt(i));
        if (i > 1 && map != '0' && ('H' == (cCharAt = str.charAt(i - 1)) || 'W' == cCharAt)) {
            char cCharAt2 = str.charAt(i - 2);
            if (map(cCharAt2) == map || 'H' == cCharAt2 || 'W' == cCharAt2) {
                return (char) 0;
            }
        }
        return map;
    }

    public int getMaxLength() {
        return this.maxLength;
    }

    private char[] getSoundexMapping() {
        return this.soundexMapping;
    }

    private char map(char c) {
        int i = c - 'A';
        if (i < 0 || i >= getSoundexMapping().length) {
            throw new IllegalArgumentException("The character is not mapped: " + c);
        }
        return getSoundexMapping()[i];
    }

    public void setMaxLength(int i) {
        this.maxLength = i;
    }

    private void setSoundexMapping(char[] cArr) {
        this.soundexMapping = cArr;
    }

    public String soundex(String str) {
        if (str == null) {
            return null;
        }
        String strClean = SoundexUtils.clean(str);
        if (strClean.length() == 0) {
            return strClean;
        }
        char[] cArr = {'0', '0', '0', '0'};
        cArr[0] = strClean.charAt(0);
        int i = 1;
        char mappingCode = getMappingCode(strClean, 0);
        int i2 = 1;
        while (i < strClean.length() && i2 < cArr.length) {
            int i3 = i + 1;
            char mappingCode2 = getMappingCode(strClean, i);
            if (mappingCode2 != 0) {
                if (mappingCode2 != '0' && mappingCode2 != mappingCode) {
                    cArr[i2] = mappingCode2;
                    i2++;
                }
                mappingCode = mappingCode2;
            }
            i = i3;
        }
        return new String(cArr);
    }
}
