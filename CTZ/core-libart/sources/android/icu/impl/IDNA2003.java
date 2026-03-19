package android.icu.impl;

import android.icu.text.StringPrep;
import android.icu.text.StringPrepParseException;
import android.icu.text.UCharacterIterator;
import android.icu.util.ULocale;

public final class IDNA2003 {
    private static final int CAPITAL_A = 65;
    private static final int CAPITAL_Z = 90;
    private static final int FULL_STOP = 46;
    private static final int HYPHEN = 45;
    private static final int LOWER_CASE_DELTA = 32;
    private static final int MAX_DOMAIN_NAME_LENGTH = 255;
    private static final int MAX_LABEL_LENGTH = 63;
    private static char[] ACE_PREFIX = {ULocale.PRIVATE_USE_EXTENSION, 'n', '-', '-'};
    private static final StringPrep namePrep = StringPrep.getInstance(0);

    private static boolean startsWithPrefix(StringBuffer stringBuffer) {
        if (stringBuffer.length() < ACE_PREFIX.length) {
            return false;
        }
        boolean z = true;
        for (int i = 0; i < ACE_PREFIX.length; i++) {
            if (toASCIILower(stringBuffer.charAt(i)) != ACE_PREFIX[i]) {
                z = false;
            }
        }
        return z;
    }

    private static char toASCIILower(char c) {
        if ('A' <= c && c <= 'Z') {
            return (char) (c + ' ');
        }
        return c;
    }

    private static StringBuffer toASCIILower(CharSequence charSequence) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < charSequence.length(); i++) {
            stringBuffer.append(toASCIILower(charSequence.charAt(i)));
        }
        return stringBuffer;
    }

    private static int compareCaseInsensitiveASCII(StringBuffer stringBuffer, StringBuffer stringBuffer2) {
        int aSCIILower;
        for (int i = 0; i != stringBuffer.length(); i++) {
            char cCharAt = stringBuffer.charAt(i);
            char cCharAt2 = stringBuffer2.charAt(i);
            if (cCharAt != cCharAt2 && (aSCIILower = toASCIILower(cCharAt) - toASCIILower(cCharAt2)) != 0) {
                return aSCIILower;
            }
        }
        return 0;
    }

    private static int getSeparatorIndex(char[] cArr, int i, int i2) {
        while (i < i2) {
            if (!isLabelSeparator(cArr[i])) {
                i++;
            } else {
                return i;
            }
        }
        return i;
    }

    private static boolean isLDHChar(int i) {
        if (i > 122) {
            return false;
        }
        if (i == 45) {
            return true;
        }
        if (48 <= i && i <= 57) {
            return true;
        }
        if (65 <= i && i <= 90) {
            return true;
        }
        if (97 > i || i > 122) {
            return false;
        }
        return true;
    }

    private static boolean isLabelSeparator(int i) {
        if (i == 46 || i == 12290 || i == 65294 || i == 65377) {
            return true;
        }
        return false;
    }

    public static StringBuffer convertToASCII(UCharacterIterator uCharacterIterator, int i) throws StringPrepParseException {
        StringBuffer stringBuffer;
        boolean z = (i & 2) != 0;
        boolean z2 = true;
        while (true) {
            int next = uCharacterIterator.next();
            if (next == -1) {
                break;
            }
            if (next > 127) {
                z2 = false;
            }
        }
        uCharacterIterator.setToStart();
        if (!z2) {
            stringBuffer = namePrep.prepare(uCharacterIterator, i);
        } else {
            stringBuffer = new StringBuffer(uCharacterIterator.getText());
        }
        int length = stringBuffer.length();
        if (length == 0) {
            throw new StringPrepParseException("Found zero length lable after NamePrep.", 10);
        }
        StringBuffer stringBuffer2 = new StringBuffer();
        boolean z3 = true;
        int i2 = -1;
        boolean z4 = true;
        for (int i3 = 0; i3 < length; i3++) {
            char cCharAt = stringBuffer.charAt(i3);
            if (cCharAt > 127) {
                z3 = false;
            } else if (!isLDHChar(cCharAt)) {
                z4 = false;
                i2 = i3;
            }
        }
        if (!z || (z4 && stringBuffer.charAt(0) != '-' && stringBuffer.charAt(stringBuffer.length() - 1) != '-')) {
            if (!z3) {
                if (!startsWithPrefix(stringBuffer)) {
                    StringBuffer aSCIILower = toASCIILower(Punycode.encode(stringBuffer, new boolean[length]));
                    stringBuffer2.append(ACE_PREFIX, 0, ACE_PREFIX.length);
                    stringBuffer2.append(aSCIILower);
                    stringBuffer = stringBuffer2;
                } else {
                    throw new StringPrepParseException("The input does not start with the ACE Prefix.", 6, stringBuffer.toString(), 0);
                }
            }
            if (stringBuffer.length() > 63) {
                throw new StringPrepParseException("The labels in the input are too long. Length > 63.", 8, stringBuffer.toString(), 0);
            }
            return stringBuffer;
        }
        if (z4) {
            if (stringBuffer.charAt(0) == '-') {
                throw new StringPrepParseException("The input does not conform to the STD 3 ASCII rules", 5, stringBuffer.toString(), 0);
            }
            String string = stringBuffer.toString();
            if (length > 0) {
                length--;
            }
            throw new StringPrepParseException("The input does not conform to the STD 3 ASCII rules", 5, string, length);
        }
        String string2 = stringBuffer.toString();
        if (i2 > 0) {
            i2--;
        }
        throw new StringPrepParseException("The input does not conform to the STD 3 ASCII rules", 5, string2, i2);
    }

    public static StringBuffer convertIDNToASCII(String str, int i) throws StringPrepParseException {
        char[] charArray = str.toCharArray();
        StringBuffer stringBuffer = new StringBuffer();
        int i2 = 0;
        int i3 = 0;
        while (true) {
            int separatorIndex = getSeparatorIndex(charArray, i2, charArray.length);
            String str2 = new String(charArray, i3, separatorIndex - i3);
            if (str2.length() != 0 || separatorIndex != charArray.length) {
                stringBuffer.append(convertToASCII(UCharacterIterator.getInstance(str2), i));
            }
            if (separatorIndex == charArray.length) {
                break;
            }
            i3 = separatorIndex + 1;
            stringBuffer.append('.');
            i2 = i3;
        }
        if (stringBuffer.length() > 255) {
            throw new StringPrepParseException("The output exceed the max allowed length.", 11);
        }
        return stringBuffer;
    }

    public static StringBuffer convertToUnicode(UCharacterIterator uCharacterIterator, int i) throws StringPrepParseException {
        StringBuffer stringBufferPrepare;
        StringBuffer stringBuffer;
        int index = uCharacterIterator.getIndex();
        boolean z = true;
        while (true) {
            int next = uCharacterIterator.next();
            if (next == -1) {
                break;
            }
            if (next > 127) {
                z = false;
            }
        }
        if (!z) {
            try {
                uCharacterIterator.setIndex(index);
                stringBufferPrepare = namePrep.prepare(uCharacterIterator, i);
            } catch (StringPrepParseException e) {
                return new StringBuffer(uCharacterIterator.getText());
            }
        } else {
            stringBufferPrepare = new StringBuffer(uCharacterIterator.getText());
        }
        if (startsWithPrefix(stringBufferPrepare)) {
            StringBuffer stringBuffer2 = null;
            try {
                stringBuffer = new StringBuffer(Punycode.decode(stringBufferPrepare.substring(ACE_PREFIX.length, stringBufferPrepare.length()), null));
            } catch (StringPrepParseException e2) {
                stringBuffer = null;
            }
            if (stringBuffer == null || compareCaseInsensitiveASCII(stringBufferPrepare, convertToASCII(UCharacterIterator.getInstance(stringBuffer), i)) == 0) {
                stringBuffer2 = stringBuffer;
            }
            if (stringBuffer2 != null) {
                return stringBuffer2;
            }
        }
        return new StringBuffer(uCharacterIterator.getText());
    }

    public static StringBuffer convertIDNToUnicode(String str, int i) throws StringPrepParseException {
        char[] charArray = str.toCharArray();
        StringBuffer stringBuffer = new StringBuffer();
        int i2 = 0;
        int i3 = 0;
        while (true) {
            int separatorIndex = getSeparatorIndex(charArray, i2, charArray.length);
            String str2 = new String(charArray, i3, separatorIndex - i3);
            if (str2.length() == 0 && separatorIndex != charArray.length) {
                throw new StringPrepParseException("Found zero length lable after NamePrep.", 10);
            }
            stringBuffer.append(convertToUnicode(UCharacterIterator.getInstance(str2), i));
            if (separatorIndex != charArray.length) {
                stringBuffer.append(charArray[separatorIndex]);
                i3 = separatorIndex + 1;
                i2 = i3;
            } else {
                if (stringBuffer.length() > 255) {
                    throw new StringPrepParseException("The output exceed the max allowed length.", 11);
                }
                return stringBuffer;
            }
        }
    }

    public static int compare(String str, String str2, int i) throws StringPrepParseException {
        return compareCaseInsensitiveASCII(convertIDNToASCII(str, i), convertIDNToASCII(str2, i));
    }
}
