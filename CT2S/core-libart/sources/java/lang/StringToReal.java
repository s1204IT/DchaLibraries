package java.lang;

import dalvik.bytecode.Opcodes;

final class StringToReal {
    private static native double parseDblImpl(String str, int i);

    private static native float parseFltImpl(String str, int i);

    StringToReal() {
    }

    private static final class StringExponentPair {
        long e;
        boolean infinity;
        boolean negative;
        String s;
        boolean zero;

        private StringExponentPair() {
        }

        public float specialValue() {
            return this.infinity ? this.negative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY : this.negative ? -0.0f : 0.0f;
        }
    }

    private static NumberFormatException invalidReal(String s, boolean isDouble) {
        throw new NumberFormatException("Invalid " + (isDouble ? "double" : "float") + ": \"" + s + "\"");
    }

    private static StringExponentPair initialParse(String s, int length, boolean isDouble) {
        String s2;
        StringExponentPair result = new StringExponentPair();
        if (length == 0) {
            throw invalidReal(s, isDouble);
        }
        result.negative = s.charAt(0) == '-';
        char c = s.charAt(length - 1);
        if ((c == 'D' || c == 'd' || c == 'F' || c == 'f') && length - 1 == 0) {
            throw invalidReal(s, isDouble);
        }
        int end = Math.max(s.indexOf(69), s.indexOf(Opcodes.OP_SGET_CHAR));
        if (end != -1) {
            if (end + 1 == length) {
                throw invalidReal(s, isDouble);
            }
            int exponentOffset = end + 1;
            boolean negativeExponent = false;
            char firstExponentChar = s.charAt(exponentOffset);
            if (firstExponentChar == '+' || firstExponentChar == '-') {
                negativeExponent = firstExponentChar == '-';
                exponentOffset++;
            }
            String exponentString = s.substring(exponentOffset, length);
            if (exponentString.isEmpty()) {
                throw invalidReal(s, isDouble);
            }
            for (int i = 0; i < exponentString.length(); i++) {
                char ch = exponentString.charAt(i);
                if (ch < '0' || ch > '9') {
                    throw invalidReal(s, isDouble);
                }
            }
            try {
                result.e = Integer.parseInt(exponentString);
                if (negativeExponent) {
                    result.e = -result.e;
                }
            } catch (NumberFormatException e) {
                if (negativeExponent) {
                    result.zero = true;
                } else {
                    result.infinity = true;
                }
            }
        } else {
            end = length;
        }
        int start = 0;
        char c2 = s.charAt(0);
        if (c2 == '-') {
            start = 0 + 1;
            length--;
            result.negative = true;
        } else if (c2 == '+') {
            start = 0 + 1;
            length--;
        }
        if (length == 0) {
            throw invalidReal(s, isDouble);
        }
        int decimal = -1;
        for (int i2 = start; i2 < end; i2++) {
            char mc = s.charAt(i2);
            if (mc == '.') {
                if (decimal != -1) {
                    throw invalidReal(s, isDouble);
                }
                decimal = i2;
            } else if (mc < '0' || mc > '9') {
                throw invalidReal(s, isDouble);
            }
        }
        if (decimal > -1) {
            result.e -= (long) ((end - decimal) - 1);
            s2 = s.substring(start, decimal) + s.substring(decimal + 1, end);
        } else {
            s2 = s.substring(start, end);
        }
        int length2 = s2.length();
        if (length2 == 0) {
            throw invalidReal(s2, isDouble);
        }
        if (!result.infinity && !result.zero) {
            int end2 = length2;
            while (end2 > 1) {
                if (s2.charAt(end2 - 1) != '0') {
                    break;
                }
                end2--;
            }
            int start2 = 0;
            while (start2 < end2 - 1 && s2.charAt(start2) == '0') {
                start2++;
            }
            if (end2 != length2 || start2 != 0) {
                result.e += (long) (length2 - end2);
                s2 = s2.substring(start2, end2);
            }
            int length3 = s2.length();
            if (length3 > 52 && result.e < -359) {
                int d = Math.min((-359) - ((int) result.e), length3 - 1);
                s2 = s2.substring(0, length3 - d);
                result.e += (long) d;
            }
            if (result.e < -1024) {
                result.zero = true;
            } else if (result.e > 1024) {
                result.infinity = true;
            } else {
                result.s = s2;
            }
        }
        return result;
    }

    private static float parseName(String name, boolean isDouble) {
        boolean negative = false;
        int i = 0;
        int length = name.length();
        char firstChar = name.charAt(0);
        if (firstChar == '-') {
            negative = true;
            i = 0 + 1;
            length--;
        } else if (firstChar == '+') {
            i = 0 + 1;
            length--;
        }
        if (length == 8 && name.regionMatches(false, i, "Infinity", 0, 8)) {
            return negative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        }
        if (length == 3 && name.regionMatches(false, i, "NaN", 0, 3)) {
            return Float.NaN;
        }
        throw invalidReal(name, isDouble);
    }

    public static double parseDouble(String s) {
        String s2 = s.trim();
        int length = s2.length();
        if (length == 0) {
            throw invalidReal(s2, true);
        }
        char last = s2.charAt(length - 1);
        if (last == 'y' || last == 'N') {
            return parseName(s2, true);
        }
        if (s2.indexOf("0x") != -1 || s2.indexOf("0X") != -1) {
            return HexStringParser.parseDouble(s2);
        }
        StringExponentPair info = initialParse(s2, length, true);
        if (info.infinity || info.zero) {
            return info.specialValue();
        }
        double result = parseDblImpl(info.s, (int) info.e);
        if (Double.doubleToRawLongBits(result) == -1) {
            throw invalidReal(s2, true);
        }
        return info.negative ? -result : result;
    }

    public static float parseFloat(String s) {
        String s2 = s.trim();
        int length = s2.length();
        if (length == 0) {
            throw invalidReal(s2, false);
        }
        char last = s2.charAt(length - 1);
        if (last == 'y' || last == 'N') {
            return parseName(s2, false);
        }
        if (s2.indexOf("0x") != -1 || s2.indexOf("0X") != -1) {
            return HexStringParser.parseFloat(s2);
        }
        StringExponentPair info = initialParse(s2, length, false);
        if (info.infinity || info.zero) {
            return info.specialValue();
        }
        float result = parseFltImpl(info.s, (int) info.e);
        if (Float.floatToRawIntBits(result) == -1) {
            throw invalidReal(s2, false);
        }
        return info.negative ? -result : result;
    }
}
