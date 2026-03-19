package android.icu.impl.number;

import android.icu.impl.coll.Collation;
import android.icu.impl.coll.CollationSettings;
import android.icu.impl.number.Padder;

public class PatternStringParser {
    static final boolean $assertionsDisabled = false;
    public static final int IGNORE_ROUNDING_ALWAYS = 2;
    public static final int IGNORE_ROUNDING_IF_CURRENCY = 1;
    public static final int IGNORE_ROUNDING_NEVER = 0;

    public static class ParsedSubpatternInfo {
        public long groupingSizes = 281474976645120L;
        public int integerLeadingHashSigns = 0;
        public int integerTrailingHashSigns = 0;
        public int integerNumerals = 0;
        public int integerAtSigns = 0;
        public int integerTotal = 0;
        public int fractionNumerals = 0;
        public int fractionHashSigns = 0;
        public int fractionTotal = 0;
        public boolean hasDecimal = false;
        public int widthExceptAffixes = 0;
        public Padder.PadPosition paddingLocation = null;
        public DecimalQuantity_DualStorageBCD rounding = null;
        public boolean exponentHasPlusSign = false;
        public int exponentZeros = 0;
        public boolean hasPercentSign = false;
        public boolean hasPerMilleSign = false;
        public boolean hasCurrencySign = false;
        public boolean hasMinusSign = false;
        public boolean hasPlusSign = false;
        public long prefixEndpoints = 0;
        public long suffixEndpoints = 0;
        public long paddingEndpoints = 0;
    }

    public static ParsedPatternInfo parseToPatternInfo(String str) {
        ParserState parserState = new ParserState(str);
        ParsedPatternInfo parsedPatternInfo = new ParsedPatternInfo(str);
        consumePattern(parserState, parsedPatternInfo);
        return parsedPatternInfo;
    }

    public static DecimalFormatProperties parseToProperties(String str, int i) {
        DecimalFormatProperties decimalFormatProperties = new DecimalFormatProperties();
        parseToExistingPropertiesImpl(str, decimalFormatProperties, i);
        return decimalFormatProperties;
    }

    public static DecimalFormatProperties parseToProperties(String str) {
        return parseToProperties(str, 0);
    }

    public static void parseToExistingProperties(String str, DecimalFormatProperties decimalFormatProperties, int i) {
        parseToExistingPropertiesImpl(str, decimalFormatProperties, i);
    }

    public static void parseToExistingProperties(String str, DecimalFormatProperties decimalFormatProperties) {
        parseToExistingProperties(str, decimalFormatProperties, 0);
    }

    public static class ParsedPatternInfo implements AffixPatternProvider {
        public ParsedSubpatternInfo negative;
        public String pattern;
        public ParsedSubpatternInfo positive;

        private ParsedPatternInfo(String str) {
            this.pattern = str;
        }

        @Override
        public char charAt(int i, int i2) {
            long endpoints = getEndpoints(i);
            int i3 = (int) ((-1) & endpoints);
            int i4 = (int) (endpoints >>> 32);
            if (i2 < 0 || i2 >= i4 - i3) {
                throw new IndexOutOfBoundsException();
            }
            return this.pattern.charAt(i3 + i2);
        }

        @Override
        public int length(int i) {
            return getLengthFromEndpoints(getEndpoints(i));
        }

        public static int getLengthFromEndpoints(long j) {
            return ((int) (j >>> 32)) - ((int) ((-1) & j));
        }

        public String getString(int i) {
            long endpoints = getEndpoints(i);
            int i2 = (int) ((-1) & endpoints);
            int i3 = (int) (endpoints >>> 32);
            if (i2 == i3) {
                return "";
            }
            return this.pattern.substring(i2, i3);
        }

        private long getEndpoints(int i) {
            boolean z = (i & 256) != 0;
            boolean z2 = (i & 512) != 0;
            boolean z3 = (i & 1024) != 0;
            if (z2 && z3) {
                return this.negative.paddingEndpoints;
            }
            if (z3) {
                return this.positive.paddingEndpoints;
            }
            if (z && z2) {
                return this.negative.prefixEndpoints;
            }
            if (z) {
                return this.positive.prefixEndpoints;
            }
            if (z2) {
                return this.negative.suffixEndpoints;
            }
            return this.positive.suffixEndpoints;
        }

        @Override
        public boolean positiveHasPlusSign() {
            return this.positive.hasPlusSign;
        }

        @Override
        public boolean hasNegativeSubpattern() {
            return this.negative != null;
        }

        @Override
        public boolean negativeHasMinusSign() {
            return this.negative.hasMinusSign;
        }

        @Override
        public boolean hasCurrencySign() {
            return this.positive.hasCurrencySign || (this.negative != null && this.negative.hasCurrencySign);
        }

        @Override
        public boolean containsSymbolType(int i) {
            return AffixUtils.containsType(this.pattern, i);
        }
    }

    private static class ParserState {
        int offset = 0;
        final String pattern;

        ParserState(String str) {
            this.pattern = str;
        }

        int peek() {
            if (this.offset == this.pattern.length()) {
                return -1;
            }
            return this.pattern.codePointAt(this.offset);
        }

        int next() {
            int iPeek = peek();
            this.offset += Character.charCount(iPeek);
            return iPeek;
        }

        IllegalArgumentException toParseException(String str) {
            return new IllegalArgumentException("Malformed pattern for ICU DecimalFormat: \"" + this.pattern + "\": " + str + " at position " + this.offset);
        }
    }

    private static void consumePattern(ParserState parserState, ParsedPatternInfo parsedPatternInfo) {
        parsedPatternInfo.positive = new ParsedSubpatternInfo();
        consumeSubpattern(parserState, parsedPatternInfo.positive);
        if (parserState.peek() == 59) {
            parserState.next();
            if (parserState.peek() != -1) {
                parsedPatternInfo.negative = new ParsedSubpatternInfo();
                consumeSubpattern(parserState, parsedPatternInfo.negative);
            }
        }
        if (parserState.peek() != -1) {
            throw parserState.toParseException("Found unquoted special character");
        }
    }

    private static void consumeSubpattern(ParserState parserState, ParsedSubpatternInfo parsedSubpatternInfo) {
        consumePadding(parserState, parsedSubpatternInfo, Padder.PadPosition.BEFORE_PREFIX);
        parsedSubpatternInfo.prefixEndpoints = consumeAffix(parserState, parsedSubpatternInfo);
        consumePadding(parserState, parsedSubpatternInfo, Padder.PadPosition.AFTER_PREFIX);
        consumeFormat(parserState, parsedSubpatternInfo);
        consumeExponent(parserState, parsedSubpatternInfo);
        consumePadding(parserState, parsedSubpatternInfo, Padder.PadPosition.BEFORE_SUFFIX);
        parsedSubpatternInfo.suffixEndpoints = consumeAffix(parserState, parsedSubpatternInfo);
        consumePadding(parserState, parsedSubpatternInfo, Padder.PadPosition.AFTER_SUFFIX);
    }

    private static void consumePadding(ParserState parserState, ParsedSubpatternInfo parsedSubpatternInfo, Padder.PadPosition padPosition) {
        if (parserState.peek() != 42) {
            return;
        }
        if (parsedSubpatternInfo.paddingLocation != null) {
            throw parserState.toParseException("Cannot have multiple pad specifiers");
        }
        parsedSubpatternInfo.paddingLocation = padPosition;
        parserState.next();
        parsedSubpatternInfo.paddingEndpoints |= (long) parserState.offset;
        consumeLiteral(parserState);
        parsedSubpatternInfo.paddingEndpoints |= ((long) parserState.offset) << 32;
    }

    private static long consumeAffix(android.icu.impl.number.PatternStringParser.ParserState r5, android.icu.impl.number.PatternStringParser.ParsedSubpatternInfo r6) {
        r0 = (long) r5.offset;
        while (true) {
            r2 = r5.peek();
            if (r2 == -1 || r2 == 35) {
            } else {
                if (r2 != 37) {
                    if (r2 == 59 || r2 == 64) {
                    } else {
                        if (r2 != 164) {
                            if (r2 != 8240) {
                                switch (r2) {
                                    case 42:
                                    case 44:
                                    case 46:
                                        break;
                                    case 43:
                                        r6.hasPlusSign = true;
                                        break;
                                    case 45:
                                        r6.hasMinusSign = true;
                                        break;
                                    default:
                                        switch (r2) {
                                        }
                                }
                            } else {
                                r6.hasPerMilleSign = true;
                            }
                        } else {
                            r6.hasCurrencySign = true;
                        }
                    }
                } else {
                    r6.hasPercentSign = true;
                }
                consumeLiteral(r5);
            }
        }
        return (((long) r5.offset) << 32) | r0;
    }

    private static void consumeLiteral(ParserState parserState) {
        if (parserState.peek() == -1) {
            throw parserState.toParseException("Expected unquoted literal but found EOL");
        }
        if (parserState.peek() == 39) {
            parserState.next();
            while (parserState.peek() != 39) {
                if (parserState.peek() == -1) {
                    throw parserState.toParseException("Expected quoted literal but found EOL");
                }
                parserState.next();
            }
            parserState.next();
            return;
        }
        parserState.next();
    }

    private static void consumeFormat(ParserState parserState, ParsedSubpatternInfo parsedSubpatternInfo) {
        consumeIntegerFormat(parserState, parsedSubpatternInfo);
        if (parserState.peek() == 46) {
            parserState.next();
            parsedSubpatternInfo.hasDecimal = true;
            parsedSubpatternInfo.widthExceptAffixes++;
            consumeFractionFormat(parserState, parsedSubpatternInfo);
        }
    }

    private static void consumeIntegerFormat(ParserState parserState, ParsedSubpatternInfo parsedSubpatternInfo) {
        while (true) {
            int iPeek = parserState.peek();
            if (iPeek == 35) {
                if (parsedSubpatternInfo.integerNumerals > 0) {
                    throw parserState.toParseException("# cannot follow 0 before decimal point");
                }
                parsedSubpatternInfo.widthExceptAffixes++;
                parsedSubpatternInfo.groupingSizes++;
                if (parsedSubpatternInfo.integerAtSigns > 0) {
                    parsedSubpatternInfo.integerTrailingHashSigns++;
                } else {
                    parsedSubpatternInfo.integerLeadingHashSigns++;
                }
                parsedSubpatternInfo.integerTotal++;
            } else if (iPeek == 44) {
                parsedSubpatternInfo.widthExceptAffixes++;
                parsedSubpatternInfo.groupingSizes <<= 16;
            } else if (iPeek == 64) {
                if (parsedSubpatternInfo.integerNumerals > 0) {
                    throw parserState.toParseException("Cannot mix 0 and @");
                }
                if (parsedSubpatternInfo.integerTrailingHashSigns > 0) {
                    throw parserState.toParseException("Cannot nest # inside of a run of @");
                }
                parsedSubpatternInfo.widthExceptAffixes++;
                parsedSubpatternInfo.groupingSizes++;
                parsedSubpatternInfo.integerAtSigns++;
                parsedSubpatternInfo.integerTotal++;
            } else {
                switch (iPeek) {
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                        if (parsedSubpatternInfo.integerAtSigns > 0) {
                            throw parserState.toParseException("Cannot mix @ and 0");
                        }
                        parsedSubpatternInfo.widthExceptAffixes++;
                        parsedSubpatternInfo.groupingSizes++;
                        parsedSubpatternInfo.integerNumerals++;
                        parsedSubpatternInfo.integerTotal++;
                        if (parserState.peek() != 48 && parsedSubpatternInfo.rounding == null) {
                            parsedSubpatternInfo.rounding = new DecimalQuantity_DualStorageBCD();
                        }
                        if (parsedSubpatternInfo.rounding != null) {
                            parsedSubpatternInfo.rounding.appendDigit((byte) (parserState.peek() - 48), 0, true);
                        }
                        break;
                        break;
                    default:
                        short s = (short) (parsedSubpatternInfo.groupingSizes & 65535);
                        short s2 = (short) ((parsedSubpatternInfo.groupingSizes >>> 16) & 65535);
                        short s3 = (short) (65535 & (parsedSubpatternInfo.groupingSizes >>> 32));
                        if (s == 0 && s2 != -1) {
                            throw parserState.toParseException("Trailing grouping separator is invalid");
                        }
                        if (s2 == 0 && s3 != -1) {
                            throw parserState.toParseException("Grouping width of zero is invalid");
                        }
                        return;
                }
            }
            parserState.next();
        }
    }

    private static void consumeFractionFormat(ParserState parserState, ParsedSubpatternInfo parsedSubpatternInfo) {
        int i = 0;
        while (true) {
            int iPeek = parserState.peek();
            if (iPeek == 35) {
                parsedSubpatternInfo.widthExceptAffixes++;
                parsedSubpatternInfo.fractionHashSigns++;
                parsedSubpatternInfo.fractionTotal++;
                i++;
            } else {
                switch (iPeek) {
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                        if (parsedSubpatternInfo.fractionHashSigns > 0) {
                            throw parserState.toParseException("0 cannot follow # after decimal point");
                        }
                        parsedSubpatternInfo.widthExceptAffixes++;
                        parsedSubpatternInfo.fractionNumerals++;
                        parsedSubpatternInfo.fractionTotal++;
                        if (parserState.peek() == 48) {
                            i++;
                        } else {
                            if (parsedSubpatternInfo.rounding == null) {
                                parsedSubpatternInfo.rounding = new DecimalQuantity_DualStorageBCD();
                            }
                            parsedSubpatternInfo.rounding.appendDigit((byte) (parserState.peek() - 48), i, false);
                            i = 0;
                        }
                        break;
                        break;
                    default:
                        return;
                }
            }
            parserState.next();
        }
    }

    private static void consumeExponent(ParserState parserState, ParsedSubpatternInfo parsedSubpatternInfo) {
        if (parserState.peek() != 69) {
            return;
        }
        if ((parsedSubpatternInfo.groupingSizes & Collation.MAX_PRIMARY) != Collation.MAX_PRIMARY) {
            throw parserState.toParseException("Cannot have grouping separator in scientific notation");
        }
        parserState.next();
        parsedSubpatternInfo.widthExceptAffixes++;
        if (parserState.peek() == 43) {
            parserState.next();
            parsedSubpatternInfo.exponentHasPlusSign = true;
            parsedSubpatternInfo.widthExceptAffixes++;
        }
        while (parserState.peek() == 48) {
            parserState.next();
            parsedSubpatternInfo.exponentZeros++;
            parsedSubpatternInfo.widthExceptAffixes++;
        }
    }

    private static void parseToExistingPropertiesImpl(String str, DecimalFormatProperties decimalFormatProperties, int i) {
        if (str == null || str.length() == 0) {
            decimalFormatProperties.clear();
        } else {
            patternInfoToProperties(decimalFormatProperties, parseToPatternInfo(str), i);
        }
    }

    private static void patternInfoToProperties(DecimalFormatProperties decimalFormatProperties, ParsedPatternInfo parsedPatternInfo, int i) {
        int i2;
        int iMax;
        ParsedSubpatternInfo parsedSubpatternInfo = parsedPatternInfo.positive;
        boolean z = i == 0 ? false : i == 1 ? parsedSubpatternInfo.hasCurrencySign : true;
        short s = (short) (parsedSubpatternInfo.groupingSizes & 65535);
        short s2 = (short) ((parsedSubpatternInfo.groupingSizes >>> 16) & 65535);
        short s3 = (short) (65535 & (parsedSubpatternInfo.groupingSizes >>> 32));
        if (s2 != -1) {
            decimalFormatProperties.setGroupingSize(s);
        } else {
            decimalFormatProperties.setGroupingSize(-1);
        }
        if (s3 != -1) {
            decimalFormatProperties.setSecondaryGroupingSize(s2);
        } else {
            decimalFormatProperties.setSecondaryGroupingSize(-1);
        }
        if (parsedSubpatternInfo.integerTotal == 0 && parsedSubpatternInfo.fractionTotal > 0) {
            iMax = Math.max(1, parsedSubpatternInfo.fractionNumerals);
            i2 = 0;
        } else if (parsedSubpatternInfo.integerNumerals != 0 || parsedSubpatternInfo.fractionNumerals != 0) {
            i2 = parsedSubpatternInfo.integerNumerals;
            iMax = parsedSubpatternInfo.fractionNumerals;
        } else {
            iMax = 0;
            i2 = 1;
        }
        if (parsedSubpatternInfo.integerAtSigns > 0) {
            decimalFormatProperties.setMinimumFractionDigits(-1);
            decimalFormatProperties.setMaximumFractionDigits(-1);
            decimalFormatProperties.setRoundingIncrement(null);
            decimalFormatProperties.setMinimumSignificantDigits(parsedSubpatternInfo.integerAtSigns);
            decimalFormatProperties.setMaximumSignificantDigits(parsedSubpatternInfo.integerAtSigns + parsedSubpatternInfo.integerTrailingHashSigns);
        } else if (parsedSubpatternInfo.rounding != null) {
            if (!z) {
                decimalFormatProperties.setMinimumFractionDigits(iMax);
                decimalFormatProperties.setMaximumFractionDigits(parsedSubpatternInfo.fractionTotal);
                decimalFormatProperties.setRoundingIncrement(parsedSubpatternInfo.rounding.toBigDecimal().setScale(parsedSubpatternInfo.fractionNumerals));
            } else {
                decimalFormatProperties.setMinimumFractionDigits(-1);
                decimalFormatProperties.setMaximumFractionDigits(-1);
                decimalFormatProperties.setRoundingIncrement(null);
            }
            decimalFormatProperties.setMinimumSignificantDigits(-1);
            decimalFormatProperties.setMaximumSignificantDigits(-1);
        } else {
            if (!z) {
                decimalFormatProperties.setMinimumFractionDigits(iMax);
                decimalFormatProperties.setMaximumFractionDigits(parsedSubpatternInfo.fractionTotal);
                decimalFormatProperties.setRoundingIncrement(null);
            } else {
                decimalFormatProperties.setMinimumFractionDigits(-1);
                decimalFormatProperties.setMaximumFractionDigits(-1);
                decimalFormatProperties.setRoundingIncrement(null);
            }
            decimalFormatProperties.setMinimumSignificantDigits(-1);
            decimalFormatProperties.setMaximumSignificantDigits(-1);
        }
        if (parsedSubpatternInfo.hasDecimal && parsedSubpatternInfo.fractionTotal == 0) {
            decimalFormatProperties.setDecimalSeparatorAlwaysShown(true);
        } else {
            decimalFormatProperties.setDecimalSeparatorAlwaysShown(false);
        }
        if (parsedSubpatternInfo.exponentZeros > 0) {
            decimalFormatProperties.setExponentSignAlwaysShown(parsedSubpatternInfo.exponentHasPlusSign);
            decimalFormatProperties.setMinimumExponentDigits(parsedSubpatternInfo.exponentZeros);
            if (parsedSubpatternInfo.integerAtSigns == 0) {
                decimalFormatProperties.setMinimumIntegerDigits(parsedSubpatternInfo.integerNumerals);
                decimalFormatProperties.setMaximumIntegerDigits(parsedSubpatternInfo.integerTotal);
            } else {
                decimalFormatProperties.setMinimumIntegerDigits(1);
                decimalFormatProperties.setMaximumIntegerDigits(-1);
            }
        } else {
            decimalFormatProperties.setExponentSignAlwaysShown(false);
            decimalFormatProperties.setMinimumExponentDigits(-1);
            decimalFormatProperties.setMinimumIntegerDigits(i2);
            decimalFormatProperties.setMaximumIntegerDigits(-1);
        }
        String string = parsedPatternInfo.getString(256);
        String string2 = parsedPatternInfo.getString(0);
        if (parsedSubpatternInfo.paddingLocation != null) {
            decimalFormatProperties.setFormatWidth(parsedSubpatternInfo.widthExceptAffixes + AffixUtils.estimateLength(string) + AffixUtils.estimateLength(string2));
            String string3 = parsedPatternInfo.getString(1024);
            if (string3.length() == 1) {
                decimalFormatProperties.setPadString(string3);
            } else if (string3.length() == 2) {
                if (string3.charAt(0) == '\'') {
                    decimalFormatProperties.setPadString("'");
                } else {
                    decimalFormatProperties.setPadString(string3);
                }
            } else {
                decimalFormatProperties.setPadString(string3.substring(1, string3.length() - 1));
            }
            decimalFormatProperties.setPadPosition(parsedSubpatternInfo.paddingLocation);
        } else {
            decimalFormatProperties.setFormatWidth(-1);
            decimalFormatProperties.setPadString(null);
            decimalFormatProperties.setPadPosition(null);
        }
        decimalFormatProperties.setPositivePrefixPattern(string);
        decimalFormatProperties.setPositiveSuffixPattern(string2);
        if (parsedPatternInfo.negative != null) {
            decimalFormatProperties.setNegativePrefixPattern(parsedPatternInfo.getString(CollationSettings.CASE_FIRST_AND_UPPER_MASK));
            decimalFormatProperties.setNegativeSuffixPattern(parsedPatternInfo.getString(512));
        } else {
            decimalFormatProperties.setNegativePrefixPattern(null);
            decimalFormatProperties.setNegativeSuffixPattern(null);
        }
        if (parsedSubpatternInfo.hasPercentSign) {
            decimalFormatProperties.setMagnitudeMultiplier(2);
        } else if (parsedSubpatternInfo.hasPerMilleSign) {
            decimalFormatProperties.setMagnitudeMultiplier(3);
        } else {
            decimalFormatProperties.setMagnitudeMultiplier(0);
        }
    }
}
