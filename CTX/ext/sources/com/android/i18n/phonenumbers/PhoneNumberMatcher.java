package com.android.i18n.phonenumbers;

import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonemetadata;
import com.android.i18n.phonenumbers.Phonenumber;
import gov.nist.core.Separators;
import java.lang.Character;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PhoneNumberMatcher implements Iterator<PhoneNumberMatch> {
    private static final Pattern LEAD_CLASS;
    private static final Pattern MATCHING_BRACKETS;
    private static final Pattern PATTERN;
    private final PhoneNumberUtil.Leniency leniency;
    private long maxTries;
    private final PhoneNumberUtil phoneUtil;
    private final String preferredRegion;
    private final CharSequence text;
    private static final Pattern PUB_PAGES = Pattern.compile("\\d{1,5}-+\\d{1,5}\\s{0,4}\\(\\d{1,4}");
    private static final Pattern SLASH_SEPARATED_DATES = Pattern.compile("(?:(?:[0-3]?\\d/[01]?\\d)|(?:[01]?\\d/[0-3]?\\d))/(?:[12]\\d)?\\d{2}");
    private static final Pattern TIME_STAMPS = Pattern.compile("[12]\\d{3}[-/]?[01]\\d[-/]?[0-3]\\d +[0-2]\\d$");
    private static final Pattern TIME_STAMPS_SUFFIX = Pattern.compile(":[0-5]\\d");
    private static final Pattern[] INNER_MATCHES = {Pattern.compile("/+(.*)"), Pattern.compile("(\\([^(]*)"), Pattern.compile("(?:\\p{Z}-|-\\p{Z})\\p{Z}*(.+)"), Pattern.compile("[‒-―－]\\p{Z}*(.+)"), Pattern.compile("\\.+\\p{Z}*([^.]+)"), Pattern.compile("\\p{Z}+(\\P{Z}+)")};
    private State state = State.NOT_READY;
    private PhoneNumberMatch lastMatch = null;
    private int searchIndex = 0;

    interface NumberGroupingChecker {
        boolean checkGroups(PhoneNumberUtil phoneNumberUtil, Phonenumber.PhoneNumber phoneNumber, StringBuilder sb, String[] strArr);
    }

    private enum State {
        NOT_READY,
        READY,
        DONE
    }

    static {
        String str = "[^(\\[（［)\\]）］]";
        MATCHING_BRACKETS = Pattern.compile("(?:[(\\[（［])?(?:" + str + "+[)\\]）］])?" + str + "+(?:[(\\[（［]" + str + "+[)\\]）］])" + limit(0, 3) + str + Separators.STAR);
        String strLimit = limit(0, 2);
        String strLimit2 = limit(0, 4);
        String strLimit3 = limit(0, 20);
        String str2 = "[-x‐-―−ー－-／  \u00ad\u200b\u2060\u3000()（）［］.\\[\\]/~⁓∼～]" + strLimit2;
        String str3 = "\\p{Nd}" + limit(1, 20);
        String str4 = "[" + ("(\\[（［+＋") + "]";
        LEAD_CLASS = Pattern.compile(str4);
        PATTERN = Pattern.compile("(?:" + str4 + str2 + Separators.RPAREN + strLimit + str3 + "(?:" + str2 + str3 + Separators.RPAREN + strLimit3 + "(?:" + PhoneNumberUtil.EXTN_PATTERNS_FOR_MATCHING + ")?", 66);
    }

    private static String limit(int i, int i2) {
        if (i < 0 || i2 <= 0 || i2 < i) {
            throw new IllegalArgumentException();
        }
        return "{" + i + Separators.COMMA + i2 + "}";
    }

    PhoneNumberMatcher(PhoneNumberUtil phoneNumberUtil, String str, String str2, PhoneNumberUtil.Leniency leniency, long j) {
        if (phoneNumberUtil == null || leniency == null) {
            throw new NullPointerException();
        }
        if (j < 0) {
            throw new IllegalArgumentException();
        }
        this.phoneUtil = phoneNumberUtil;
        this.text = str == null ? "" : str;
        this.preferredRegion = str2;
        this.leniency = leniency;
        this.maxTries = j;
    }

    private PhoneNumberMatch find(int i) {
        Matcher matcher = PATTERN.matcher(this.text);
        while (this.maxTries > 0 && matcher.find(i)) {
            int iStart = matcher.start();
            CharSequence charSequenceTrimAfterFirstMatch = trimAfterFirstMatch(PhoneNumberUtil.SECOND_NUMBER_START_PATTERN, this.text.subSequence(iStart, matcher.end()));
            PhoneNumberMatch phoneNumberMatchExtractMatch = extractMatch(charSequenceTrimAfterFirstMatch, iStart);
            if (phoneNumberMatchExtractMatch != null) {
                return phoneNumberMatchExtractMatch;
            }
            i = iStart + charSequenceTrimAfterFirstMatch.length();
            this.maxTries--;
        }
        return null;
    }

    private static CharSequence trimAfterFirstMatch(Pattern pattern, CharSequence charSequence) {
        Matcher matcher = pattern.matcher(charSequence);
        if (matcher.find()) {
            return charSequence.subSequence(0, matcher.start());
        }
        return charSequence;
    }

    static boolean isLatinLetter(char c) {
        if (!Character.isLetter(c) && Character.getType(c) != 6) {
            return false;
        }
        Character.UnicodeBlock unicodeBlockOf = Character.UnicodeBlock.of(c);
        return unicodeBlockOf.equals(Character.UnicodeBlock.BASIC_LATIN) || unicodeBlockOf.equals(Character.UnicodeBlock.LATIN_1_SUPPLEMENT) || unicodeBlockOf.equals(Character.UnicodeBlock.LATIN_EXTENDED_A) || unicodeBlockOf.equals(Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL) || unicodeBlockOf.equals(Character.UnicodeBlock.LATIN_EXTENDED_B) || unicodeBlockOf.equals(Character.UnicodeBlock.COMBINING_DIACRITICAL_MARKS);
    }

    private static boolean isInvalidPunctuationSymbol(char c) {
        return c == '%' || Character.getType(c) == 26;
    }

    private PhoneNumberMatch extractMatch(CharSequence charSequence, int i) {
        if (SLASH_SEPARATED_DATES.matcher(charSequence).find()) {
            return null;
        }
        if (TIME_STAMPS.matcher(charSequence).find()) {
            if (TIME_STAMPS_SUFFIX.matcher(this.text.toString().substring(charSequence.length() + i)).lookingAt()) {
                return null;
            }
        }
        PhoneNumberMatch andVerify = parseAndVerify(charSequence, i);
        if (andVerify != null) {
            return andVerify;
        }
        return extractInnerMatch(charSequence, i);
    }

    private PhoneNumberMatch extractInnerMatch(CharSequence charSequence, int i) {
        for (Pattern pattern : INNER_MATCHES) {
            Matcher matcher = pattern.matcher(charSequence);
            boolean z = true;
            while (matcher.find() && this.maxTries > 0) {
                if (z) {
                    PhoneNumberMatch andVerify = parseAndVerify(trimAfterFirstMatch(PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN, charSequence.subSequence(0, matcher.start())), i);
                    if (andVerify != null) {
                        return andVerify;
                    }
                    this.maxTries--;
                    z = false;
                }
                PhoneNumberMatch andVerify2 = parseAndVerify(trimAfterFirstMatch(PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN, matcher.group(1)), matcher.start(1) + i);
                if (andVerify2 != null) {
                    return andVerify2;
                }
                this.maxTries--;
            }
        }
        return null;
    }

    private PhoneNumberMatch parseAndVerify(CharSequence charSequence, int i) {
        if (MATCHING_BRACKETS.matcher(charSequence).matches() && !PUB_PAGES.matcher(charSequence).find()) {
            if (this.leniency.compareTo(PhoneNumberUtil.Leniency.VALID) >= 0) {
                if (i > 0 && !LEAD_CLASS.matcher(charSequence).lookingAt()) {
                    char cCharAt = this.text.charAt(i - 1);
                    if (isInvalidPunctuationSymbol(cCharAt) || isLatinLetter(cCharAt)) {
                        return null;
                    }
                }
                int length = charSequence.length() + i;
                if (length < this.text.length()) {
                    char cCharAt2 = this.text.charAt(length);
                    if (isInvalidPunctuationSymbol(cCharAt2) || isLatinLetter(cCharAt2)) {
                        return null;
                    }
                }
            }
            Phonenumber.PhoneNumber andKeepRawInput = this.phoneUtil.parseAndKeepRawInput(charSequence, this.preferredRegion);
            if ((!this.phoneUtil.getRegionCodeForCountryCode(andKeepRawInput.getCountryCode()).equals("IL") || this.phoneUtil.getNationalSignificantNumber(andKeepRawInput).length() != 4 || (i != 0 && (i <= 0 || this.text.charAt(i - 1) == '*'))) && this.leniency.verify(andKeepRawInput, charSequence, this.phoneUtil)) {
                andKeepRawInput.clearCountryCodeSource();
                andKeepRawInput.clearRawInput();
                andKeepRawInput.clearPreferredDomesticCarrierCode();
                return new PhoneNumberMatch(i, charSequence.toString(), andKeepRawInput);
            }
            return null;
        }
        return null;
    }

    static boolean allNumberGroupsRemainGrouped(PhoneNumberUtil phoneNumberUtil, Phonenumber.PhoneNumber phoneNumber, StringBuilder sb, String[] strArr) {
        int length;
        if (phoneNumber.getCountryCodeSource() != Phonenumber.PhoneNumber.CountryCodeSource.FROM_DEFAULT_COUNTRY) {
            String string = Integer.toString(phoneNumber.getCountryCode());
            length = string.length() + sb.indexOf(string);
        } else {
            length = 0;
        }
        int length2 = length;
        for (int i = 0; i < strArr.length; i++) {
            int iIndexOf = sb.indexOf(strArr[i], length2);
            if (iIndexOf < 0) {
                return false;
            }
            length2 = iIndexOf + strArr[i].length();
            if (i == 0 && length2 < sb.length() && phoneNumberUtil.getNddPrefixForRegion(phoneNumberUtil.getRegionCodeForCountryCode(phoneNumber.getCountryCode()), true) != null && Character.isDigit(sb.charAt(length2))) {
                return sb.substring(length2 - strArr[i].length()).startsWith(phoneNumberUtil.getNationalSignificantNumber(phoneNumber));
            }
        }
        return sb.substring(length2).contains(phoneNumber.getExtension());
    }

    static boolean allNumberGroupsAreExactlyPresent(PhoneNumberUtil phoneNumberUtil, Phonenumber.PhoneNumber phoneNumber, StringBuilder sb, String[] strArr) {
        String[] strArrSplit = PhoneNumberUtil.NON_DIGITS_PATTERN.split(sb.toString());
        int length = phoneNumber.hasExtension() ? strArrSplit.length - 2 : strArrSplit.length - 1;
        if (strArrSplit.length == 1 || strArrSplit[length].contains(phoneNumberUtil.getNationalSignificantNumber(phoneNumber))) {
            return true;
        }
        int length2 = strArr.length - 1;
        while (length2 > 0 && length >= 0) {
            if (!strArrSplit[length].equals(strArr[length2])) {
                return false;
            }
            length2--;
            length--;
        }
        return length >= 0 && strArrSplit[length].endsWith(strArr[0]);
    }

    private static String[] getNationalNumberGroups(PhoneNumberUtil phoneNumberUtil, Phonenumber.PhoneNumber phoneNumber, Phonemetadata.NumberFormat numberFormat) {
        if (numberFormat == null) {
            String str = phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.RFC3966);
            int iIndexOf = str.indexOf(59);
            if (iIndexOf < 0) {
                iIndexOf = str.length();
            }
            return str.substring(str.indexOf(45) + 1, iIndexOf).split("-");
        }
        return phoneNumberUtil.formatNsnUsingPattern(phoneNumberUtil.getNationalSignificantNumber(phoneNumber), numberFormat, PhoneNumberUtil.PhoneNumberFormat.RFC3966).split("-");
    }

    static boolean checkNumberGroupingIsValid(Phonenumber.PhoneNumber phoneNumber, CharSequence charSequence, PhoneNumberUtil phoneNumberUtil, NumberGroupingChecker numberGroupingChecker) {
        StringBuilder sbNormalizeDigits = PhoneNumberUtil.normalizeDigits(charSequence, true);
        if (numberGroupingChecker.checkGroups(phoneNumberUtil, phoneNumber, sbNormalizeDigits, getNationalNumberGroups(phoneNumberUtil, phoneNumber, null))) {
            return true;
        }
        Phonemetadata.PhoneMetadata alternateFormatsForCountry = MetadataManager.getAlternateFormatsForCountry(phoneNumber.getCountryCode());
        if (alternateFormatsForCountry != null) {
            Iterator<Phonemetadata.NumberFormat> it = alternateFormatsForCountry.numberFormats().iterator();
            while (it.hasNext()) {
                if (numberGroupingChecker.checkGroups(phoneNumberUtil, phoneNumber, sbNormalizeDigits, getNationalNumberGroups(phoneNumberUtil, phoneNumber, it.next()))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    static boolean containsMoreThanOneSlashInNationalNumber(Phonenumber.PhoneNumber phoneNumber, String str) {
        int iIndexOf;
        int iIndexOf2 = str.indexOf(47);
        if (iIndexOf2 < 0 || (iIndexOf = str.indexOf(47, iIndexOf2 + 1)) < 0) {
            return false;
        }
        if ((phoneNumber.getCountryCodeSource() == Phonenumber.PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN || phoneNumber.getCountryCodeSource() == Phonenumber.PhoneNumber.CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN) && PhoneNumberUtil.normalizeDigitsOnly(str.substring(0, iIndexOf2)).equals(Integer.toString(phoneNumber.getCountryCode()))) {
            return str.substring(iIndexOf + 1).contains(Separators.SLASH);
        }
        return true;
    }

    static boolean containsOnlyValidXChars(Phonenumber.PhoneNumber phoneNumber, String str, PhoneNumberUtil phoneNumberUtil) {
        int i = 0;
        while (i < str.length() - 1) {
            char cCharAt = str.charAt(i);
            if (cCharAt == 'x' || cCharAt == 'X') {
                int i2 = i + 1;
                char cCharAt2 = str.charAt(i2);
                if (cCharAt2 == 'x' || cCharAt2 == 'X') {
                    if (phoneNumberUtil.isNumberMatch(phoneNumber, str.substring(i2)) != PhoneNumberUtil.MatchType.NSN_MATCH) {
                        return false;
                    }
                    i = i2;
                } else if (!PhoneNumberUtil.normalizeDigitsOnly(str.substring(i)).equals(phoneNumber.getExtension())) {
                    return false;
                }
            }
            i++;
        }
        return true;
    }

    static boolean isNationalPrefixPresentIfRequired(Phonenumber.PhoneNumber phoneNumber, PhoneNumberUtil phoneNumberUtil) {
        Phonemetadata.PhoneMetadata metadataForRegion;
        if (phoneNumber.getCountryCodeSource() != Phonenumber.PhoneNumber.CountryCodeSource.FROM_DEFAULT_COUNTRY || (metadataForRegion = phoneNumberUtil.getMetadataForRegion(phoneNumberUtil.getRegionCodeForCountryCode(phoneNumber.getCountryCode()))) == null) {
            return true;
        }
        Phonemetadata.NumberFormat numberFormatChooseFormattingPatternForNumber = phoneNumberUtil.chooseFormattingPatternForNumber(metadataForRegion.numberFormats(), phoneNumberUtil.getNationalSignificantNumber(phoneNumber));
        if (numberFormatChooseFormattingPatternForNumber == null || numberFormatChooseFormattingPatternForNumber.getNationalPrefixFormattingRule().length() <= 0 || numberFormatChooseFormattingPatternForNumber.getNationalPrefixOptionalWhenFormatting() || PhoneNumberUtil.formattingRuleHasFirstGroupOnly(numberFormatChooseFormattingPatternForNumber.getNationalPrefixFormattingRule())) {
            return true;
        }
        return phoneNumberUtil.maybeStripNationalPrefixAndCarrierCode(new StringBuilder(PhoneNumberUtil.normalizeDigitsOnly(phoneNumber.getRawInput())), metadataForRegion, null);
    }

    @Override
    public boolean hasNext() {
        if (this.state == State.NOT_READY) {
            this.lastMatch = find(this.searchIndex);
            if (this.lastMatch == null) {
                this.state = State.DONE;
            } else {
                this.searchIndex = this.lastMatch.end();
                this.state = State.READY;
            }
        }
        return this.state == State.READY;
    }

    @Override
    public PhoneNumberMatch next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        PhoneNumberMatch phoneNumberMatch = this.lastMatch;
        this.lastMatch = null;
        this.state = State.NOT_READY;
        return phoneNumberMatch;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
