package java.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HexStringParser {
    private static final String BINARY_EXPONENT = "[pP]([+-]?\\d+)";
    private static final int DOUBLE_EXPONENT_WIDTH = 11;
    private static final int DOUBLE_MANTISSA_WIDTH = 52;
    private static final int FLOAT_EXPONENT_WIDTH = 8;
    private static final int FLOAT_MANTISSA_WIDTH = 23;
    private static final String FLOAT_TYPE_SUFFIX = "[fFdD]?";
    private static final int HEX_RADIX = 16;
    private static final String HEX_SIGNIFICANT = "0[xX](\\p{XDigit}+\\.?|\\p{XDigit}*\\.\\p{XDigit}+)";
    private static final int MAX_SIGNIFICANT_LENGTH = 15;
    private final long EXPONENT_BASE;
    private final int EXPONENT_WIDTH;
    private final long MANTISSA_MASK;
    private final int MANTISSA_WIDTH;
    private final long MAX_EXPONENT;
    private final long MIN_EXPONENT;
    private String abandonedNumber = "";
    private long exponent;
    private long mantissa;
    private long sign;
    private static final String HEX_PATTERN = "[\\x00-\\x20]*([+-]?)0[xX](\\p{XDigit}+\\.?|\\p{XDigit}*\\.\\p{XDigit}+)[pP]([+-]?\\d+)[fFdD]?[\\x00-\\x20]*";
    private static final Pattern PATTERN = Pattern.compile(HEX_PATTERN);

    public HexStringParser(int exponentWidth, int mantissaWidth) {
        this.EXPONENT_WIDTH = exponentWidth;
        this.MANTISSA_WIDTH = mantissaWidth;
        this.EXPONENT_BASE = ((-1) << (exponentWidth - 1)) ^ (-1);
        this.MAX_EXPONENT = ((-1) << exponentWidth) ^ (-1);
        this.MIN_EXPONENT = -(this.MANTISSA_WIDTH + 1);
        this.MANTISSA_MASK = ((-1) << mantissaWidth) ^ (-1);
    }

    public static double parseDouble(String hexString) {
        HexStringParser parser = new HexStringParser(11, 52);
        long result = parser.parse(hexString, true);
        return Double.longBitsToDouble(result);
    }

    public static float parseFloat(String hexString) {
        HexStringParser parser = new HexStringParser(8, 23);
        int result = (int) parser.parse(hexString, false);
        return Float.intBitsToFloat(result);
    }

    private long parse(String hexString, boolean isDouble) {
        Matcher matcher = PATTERN.matcher(hexString);
        if (!matcher.matches()) {
            throw new NumberFormatException("Invalid hex " + (isDouble ? "double" : "float") + ":" + hexString);
        }
        String signStr = matcher.group(1);
        String significantStr = matcher.group(2);
        String exponentStr = matcher.group(3);
        parseHexSign(signStr);
        parseExponent(exponentStr);
        parseMantissa(significantStr);
        this.sign <<= this.MANTISSA_WIDTH + this.EXPONENT_WIDTH;
        this.exponent <<= this.MANTISSA_WIDTH;
        return this.sign | this.exponent | this.mantissa;
    }

    private void parseHexSign(String signStr) {
        this.sign = signStr.equals("-") ? 1L : 0L;
    }

    private void parseExponent(String exponentStr) {
        char leadingChar = exponentStr.charAt(0);
        int expSign = leadingChar == '-' ? -1 : 1;
        if (!Character.isDigit(leadingChar)) {
            exponentStr = exponentStr.substring(1);
        }
        try {
            this.exponent = ((long) expSign) * Long.parseLong(exponentStr);
            checkedAddExponent(this.EXPONENT_BASE);
        } catch (NumberFormatException e) {
            this.exponent = ((long) expSign) * Long.MAX_VALUE;
        }
    }

    private void parseMantissa(String significantStr) {
        String[] strings = significantStr.split("\\.");
        String strIntegerPart = strings[0];
        String strDecimalPart = strings.length > 1 ? strings[1] : "";
        String significand = getNormalizedSignificand(strIntegerPart, strDecimalPart);
        if (significand.equals("0")) {
            setZero();
            return;
        }
        int offset = getOffset(strIntegerPart, strDecimalPart);
        checkedAddExponent(offset);
        if (this.exponent >= this.MAX_EXPONENT) {
            setInfinite();
            return;
        }
        if (this.exponent <= this.MIN_EXPONENT) {
            setZero();
            return;
        }
        if (significand.length() > 15) {
            this.abandonedNumber = significand.substring(15);
            significand = significand.substring(0, 15);
        }
        this.mantissa = Long.parseLong(significand, 16);
        if (this.exponent >= 1) {
            processNormalNumber();
        } else {
            processSubNormalNumber();
        }
    }

    private void setInfinite() {
        this.exponent = this.MAX_EXPONENT;
        this.mantissa = 0L;
    }

    private void setZero() {
        this.exponent = 0L;
        this.mantissa = 0L;
    }

    private void checkedAddExponent(long offset) {
        long result = this.exponent + offset;
        int expSign = Long.signum(this.exponent);
        if (Long.signum(offset) * expSign > 0 && Long.signum(result) * expSign < 0) {
            this.exponent = ((long) expSign) * Long.MAX_VALUE;
        } else {
            this.exponent = result;
        }
    }

    private void processNormalNumber() {
        int desiredWidth = this.MANTISSA_WIDTH + 2;
        fitMantissaInDesiredWidth(desiredWidth);
        round();
        this.mantissa &= this.MANTISSA_MASK;
    }

    private void processSubNormalNumber() {
        int desiredWidth = this.MANTISSA_WIDTH + 1;
        int desiredWidth2 = desiredWidth + ((int) this.exponent);
        this.exponent = 0L;
        fitMantissaInDesiredWidth(desiredWidth2);
        round();
        this.mantissa &= this.MANTISSA_MASK;
    }

    private void fitMantissaInDesiredWidth(int desiredWidth) {
        int bitLength = countBitsLength(this.mantissa);
        if (bitLength > desiredWidth) {
            discardTrailingBits(bitLength - desiredWidth);
        } else {
            this.mantissa <<= desiredWidth - bitLength;
        }
    }

    private void discardTrailingBits(long num) {
        long mask = ((-1) << ((int) num)) ^ (-1);
        this.abandonedNumber += (this.mantissa & mask);
        this.mantissa >>= (int) num;
    }

    private void round() {
        String result = this.abandonedNumber.replaceAll("0+", "");
        boolean moreThanZero = result.length() > 0;
        int lastDiscardedBit = (int) (this.mantissa & 1);
        this.mantissa >>= 1;
        int tailBitInMantissa = (int) (this.mantissa & 1);
        if (lastDiscardedBit == 1) {
            if (moreThanZero || tailBitInMantissa == 1) {
                int oldLength = countBitsLength(this.mantissa);
                this.mantissa++;
                int newLength = countBitsLength(this.mantissa);
                if (oldLength >= this.MANTISSA_WIDTH && newLength > oldLength) {
                    checkedAddExponent(1L);
                }
            }
        }
    }

    private String getNormalizedSignificand(String strIntegerPart, String strDecimalPart) {
        String significand = (strIntegerPart + strDecimalPart).replaceFirst("^0+", "");
        if (significand.length() == 0) {
            return "0";
        }
        return significand;
    }

    private int getOffset(String strIntegerPart, String strDecimalPart) {
        String strIntegerPart2 = strIntegerPart.replaceFirst("^0+", "");
        if (strIntegerPart2.length() != 0) {
            String leadingNumber = strIntegerPart2.substring(0, 1);
            return (((strIntegerPart2.length() - 1) * 4) + countBitsLength(Long.parseLong(leadingNumber, 16))) - 1;
        }
        int i = 0;
        while (i < strDecimalPart.length() && strDecimalPart.charAt(i) == '0') {
            i++;
        }
        if (i == strDecimalPart.length()) {
            return 0;
        }
        String leadingNumber2 = strDecimalPart.substring(i, i + 1);
        return ((((-i) - 1) * 4) + countBitsLength(Long.parseLong(leadingNumber2, 16))) - 1;
    }

    private int countBitsLength(long value) {
        int leadingZeros = Long.numberOfLeadingZeros(value);
        return 64 - leadingZeros;
    }
}
