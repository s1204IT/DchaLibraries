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

    public int difference(String s1, String s2) throws EncoderException {
        return SoundexUtils.difference(this, s1, s2);
    }

    public Soundex() {
        this(US_ENGLISH_MAPPING);
    }

    public Soundex(char[] mapping) {
        this.maxLength = 4;
        setSoundexMapping(mapping);
    }

    @Override
    public Object encode(Object obj) throws EncoderException {
        if (obj instanceof String) {
            return soundex(obj);
        }
        throw new EncoderException("Parameter supplied to Soundex encode is not of type java.lang.String");
    }

    @Override
    public String encode(String pString) {
        return soundex(pString);
    }

    private char getMappingCode(String str, int index) {
        char hwChar;
        char mappedChar = map(str.charAt(index));
        if (index > 1 && mappedChar != '0' && ('H' == (hwChar = str.charAt(index - 1)) || 'W' == hwChar)) {
            char preHWChar = str.charAt(index - 2);
            char firstCode = map(preHWChar);
            if (firstCode == mappedChar || 'H' == preHWChar || 'W' == preHWChar) {
                return (char) 0;
            }
        }
        return mappedChar;
    }

    public int getMaxLength() {
        return this.maxLength;
    }

    private char[] getSoundexMapping() {
        return this.soundexMapping;
    }

    private char map(char ch) {
        int index = ch - 'A';
        if (index < 0 || index >= getSoundexMapping().length) {
            throw new IllegalArgumentException("The character is not mapped: " + ch);
        }
        return getSoundexMapping()[index];
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    private void setSoundexMapping(char[] soundexMapping) {
        this.soundexMapping = soundexMapping;
    }

    public String soundex(String str) {
        if (str == null) {
            return null;
        }
        String str2 = SoundexUtils.clean(str);
        if (str2.length() == 0) {
            return str2;
        }
        char[] out = {'0', '0', '0', '0'};
        int incount = 1;
        int count = 1;
        out[0] = str2.charAt(0);
        char last = getMappingCode(str2, 0);
        while (incount < str2.length() && count < out.length) {
            int incount2 = incount + 1;
            char mapped = getMappingCode(str2, incount);
            if (mapped != 0) {
                if (mapped != '0' && mapped != last) {
                    out[count] = mapped;
                    count++;
                }
                last = mapped;
            }
            incount = incount2;
        }
        return new String(out);
    }
}
