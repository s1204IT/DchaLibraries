package org.apache.harmony.security.utils;

public class Array {
    private Array() {
    }

    public static String getBytesAsString(byte[] data) {
        StringBuilder result = new StringBuilder(data.length * 3);
        for (byte b : data) {
            result.append(Byte.toHexString(b, false));
            result.append(' ');
        }
        return result.toString();
    }

    public static String toString(byte[] array, String prefix) {
        String[] offsetPrefix = {"", "000", "00", "0", ""};
        StringBuilder sb = new StringBuilder();
        StringBuilder charForm = new StringBuilder();
        int i = 0;
        while (i < array.length) {
            if (i % 16 == 0) {
                sb.append(prefix);
                String offset = Integer.toHexString(i);
                sb.append(offsetPrefix[offset.length()]);
                sb.append(offset);
                charForm.delete(0, charForm.length());
            }
            sb.append(' ');
            sb.append(Byte.toHexString(array[i], false));
            int currentByte = array[i] & Character.DIRECTIONALITY_UNDEFINED;
            char currentChar = (char) (65535 & currentByte);
            if (Character.isISOControl(currentChar)) {
                currentChar = '.';
            }
            charForm.append(currentChar);
            if ((i + 1) % 8 == 0) {
                sb.append(' ');
            }
            if ((i + 1) % 16 == 0) {
                sb.append(' ');
                sb.append(charForm.toString());
                sb.append('\n');
            }
            i++;
        }
        if (i % 16 != 0) {
            int ws2add = 16 - (i % 16);
            for (int j = 0; j < ws2add; j++) {
                sb.append("   ");
            }
            if (ws2add > 8) {
                sb.append(' ');
            }
            sb.append("  ");
            sb.append(charForm.toString());
            sb.append('\n');
        }
        return sb.toString();
    }
}
