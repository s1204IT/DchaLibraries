package java.text;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import sun.security.x509.InvalidityDateExtension;

public class MessageFormat extends Format {
    private static final int INITIAL_FORMATS = 10;
    private static final int MODIFIER_CURRENCY = 1;
    private static final int MODIFIER_DEFAULT = 0;
    private static final int MODIFIER_FULL = 4;
    private static final int MODIFIER_INTEGER = 3;
    private static final int MODIFIER_LONG = 3;
    private static final int MODIFIER_MEDIUM = 2;
    private static final int MODIFIER_PERCENT = 2;
    private static final int MODIFIER_SHORT = 1;
    private static final int SEG_INDEX = 1;
    private static final int SEG_MODIFIER = 3;
    private static final int SEG_RAW = 0;
    private static final int SEG_TYPE = 2;
    private static final int TYPE_CHOICE = 4;
    private static final int TYPE_DATE = 2;
    private static final int TYPE_NULL = 0;
    private static final int TYPE_NUMBER = 1;
    private static final int TYPE_TIME = 3;
    private static final long serialVersionUID = 6479157306784022952L;
    private int[] argumentNumbers;
    private Format[] formats;
    private Locale locale;
    private int maxOffset;
    private int[] offsets;
    private String pattern;
    private static final String[] TYPE_KEYWORDS = {"", "number", InvalidityDateExtension.DATE, "time", "choice"};
    private static final String[] NUMBER_MODIFIER_KEYWORDS = {"", "currency", "percent", "integer"};
    private static final String[] DATE_TIME_MODIFIER_KEYWORDS = {"", "short", "medium", "long", "full"};
    private static final int[] DATE_TIME_MODIFIERS = {2, 3, 2, 1, 0};

    public MessageFormat(String str) {
        this.pattern = "";
        this.formats = new Format[10];
        this.offsets = new int[10];
        this.argumentNumbers = new int[10];
        this.maxOffset = -1;
        this.locale = Locale.getDefault(Locale.Category.FORMAT);
        applyPattern(str);
    }

    public MessageFormat(String str, Locale locale) {
        this.pattern = "";
        this.formats = new Format[10];
        this.offsets = new int[10];
        this.argumentNumbers = new int[10];
        this.maxOffset = -1;
        this.locale = locale;
        applyPattern(str);
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void applyPattern(String str) {
        StringBuilder[] sbArr = new StringBuilder[4];
        sbArr[0] = new StringBuilder();
        this.maxOffset = -1;
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        boolean z = false;
        int i4 = 0;
        while (i < str.length()) {
            char cCharAt = str.charAt(i);
            if (i2 == 0) {
                if (cCharAt == '\'') {
                    int i5 = i + 1;
                    if (i5 >= str.length() || str.charAt(i5) != '\'') {
                        z = !z;
                    } else {
                        sbArr[i2].append(cCharAt);
                        i = i5;
                    }
                } else if (cCharAt != '{' || z) {
                    sbArr[i2].append(cCharAt);
                } else {
                    if (sbArr[1] == null) {
                        sbArr[1] = new StringBuilder();
                    }
                    i2 = 1;
                }
            } else if (z) {
                sbArr[i2].append(cCharAt);
                if (cCharAt == '\'') {
                    z = false;
                }
            } else if (cCharAt != ' ') {
                if (cCharAt == '\'') {
                    z = true;
                } else if (cCharAt != ',') {
                    if (cCharAt == '{') {
                        i3++;
                        sbArr[i2].append(cCharAt);
                    } else if (cCharAt == '}') {
                        if (i3 == 0) {
                            makeFormat(i, i4, sbArr);
                            i4++;
                            sbArr[1] = null;
                            sbArr[2] = null;
                            sbArr[3] = null;
                            i2 = 0;
                        } else {
                            i3--;
                            sbArr[i2].append(cCharAt);
                        }
                    }
                } else if (i2 < 3) {
                    i2++;
                    if (sbArr[i2] == null) {
                        sbArr[i2] = new StringBuilder();
                    }
                } else {
                    sbArr[i2].append(cCharAt);
                }
                sbArr[i2].append(cCharAt);
            } else if (i2 != 2 || sbArr[2].length() > 0) {
                sbArr[i2].append(cCharAt);
            }
            i++;
        }
        if (i3 != 0 || i2 == 0) {
            this.pattern = sbArr[0].toString();
        } else {
            this.maxOffset = -1;
            throw new IllegalArgumentException("Unmatched braces in the pattern.");
        }
    }

    public String toPattern() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (int i2 = 0; i2 <= this.maxOffset; i2++) {
            copyAndFixQuotes(this.pattern, i, this.offsets[i2], sb);
            i = this.offsets[i2];
            sb.append('{');
            sb.append(this.argumentNumbers[i2]);
            Format format = this.formats[i2];
            if (format != null) {
                if (format instanceof NumberFormat) {
                    if (format.equals(NumberFormat.getInstance(this.locale))) {
                        sb.append(",number");
                    } else if (format.equals(NumberFormat.getCurrencyInstance(this.locale))) {
                        sb.append(",number,currency");
                    } else if (format.equals(NumberFormat.getPercentInstance(this.locale))) {
                        sb.append(",number,percent");
                    } else if (format.equals(NumberFormat.getIntegerInstance(this.locale))) {
                        sb.append(",number,integer");
                    } else if (format instanceof DecimalFormat) {
                        sb.append(",number,");
                        sb.append(((DecimalFormat) format).toPattern());
                    } else if (format instanceof ChoiceFormat) {
                        sb.append(",choice,");
                        sb.append(((ChoiceFormat) format).toPattern());
                    }
                } else if (format instanceof DateFormat) {
                    int i3 = 0;
                    while (true) {
                        if (i3 >= DATE_TIME_MODIFIERS.length) {
                            break;
                        }
                        if (format.equals(DateFormat.getDateInstance(DATE_TIME_MODIFIERS[i3], this.locale))) {
                            sb.append(",date");
                            break;
                        }
                        if (!format.equals(DateFormat.getTimeInstance(DATE_TIME_MODIFIERS[i3], this.locale))) {
                            i3++;
                        } else {
                            sb.append(",time");
                            break;
                        }
                    }
                    if (i3 >= DATE_TIME_MODIFIERS.length) {
                        if (format instanceof SimpleDateFormat) {
                            sb.append(",date,");
                            sb.append(((SimpleDateFormat) format).toPattern());
                        }
                    } else if (i3 != 0) {
                        sb.append(',');
                        sb.append(DATE_TIME_MODIFIER_KEYWORDS[i3]);
                    }
                }
            }
            sb.append('}');
        }
        copyAndFixQuotes(this.pattern, i, this.pattern.length(), sb);
        return sb.toString();
    }

    public void setFormatsByArgumentIndex(Format[] formatArr) {
        for (int i = 0; i <= this.maxOffset; i++) {
            int i2 = this.argumentNumbers[i];
            if (i2 < formatArr.length) {
                this.formats[i] = formatArr[i2];
            }
        }
    }

    public void setFormats(Format[] formatArr) {
        int length = formatArr.length;
        if (length > this.maxOffset + 1) {
            length = this.maxOffset + 1;
        }
        for (int i = 0; i < length; i++) {
            this.formats[i] = formatArr[i];
        }
    }

    public void setFormatByArgumentIndex(int i, Format format) {
        for (int i2 = 0; i2 <= this.maxOffset; i2++) {
            if (this.argumentNumbers[i2] == i) {
                this.formats[i2] = format;
            }
        }
    }

    public void setFormat(int i, Format format) {
        if (i > this.maxOffset) {
            throw new ArrayIndexOutOfBoundsException(this.maxOffset, i);
        }
        this.formats[i] = format;
    }

    public Format[] getFormatsByArgumentIndex() {
        int i = -1;
        for (int i2 = 0; i2 <= this.maxOffset; i2++) {
            if (this.argumentNumbers[i2] > i) {
                i = this.argumentNumbers[i2];
            }
        }
        Format[] formatArr = new Format[i + 1];
        for (int i3 = 0; i3 <= this.maxOffset; i3++) {
            formatArr[this.argumentNumbers[i3]] = this.formats[i3];
        }
        return formatArr;
    }

    public Format[] getFormats() {
        Format[] formatArr = new Format[this.maxOffset + 1];
        System.arraycopy(this.formats, 0, formatArr, 0, this.maxOffset + 1);
        return formatArr;
    }

    public final StringBuffer format(Object[] objArr, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        return subformat(objArr, stringBuffer, fieldPosition, null);
    }

    public static String format(String str, Object... objArr) {
        return new MessageFormat(str).format(objArr);
    }

    @Override
    public final StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        return subformat((Object[]) obj, stringBuffer, fieldPosition, null);
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        StringBuffer stringBuffer = new StringBuffer();
        ArrayList arrayList = new ArrayList();
        if (obj == null) {
            throw new NullPointerException("formatToCharacterIterator must be passed non-null object");
        }
        subformat((Object[]) obj, stringBuffer, null, arrayList);
        if (arrayList.size() == 0) {
            return createAttributedCharacterIterator("");
        }
        return createAttributedCharacterIterator((AttributedCharacterIterator[]) arrayList.toArray(new AttributedCharacterIterator[arrayList.size()]));
    }

    public Object[] parse(String str, ParsePosition parsePosition) {
        int iIndexOf;
        int i = 0;
        if (str == null) {
            return new Object[0];
        }
        int i2 = -1;
        for (int i3 = 0; i3 <= this.maxOffset; i3++) {
            if (this.argumentNumbers[i3] > i2) {
                i2 = this.argumentNumbers[i3];
            }
        }
        Object[] objArr = new Object[i2 + 1];
        int i4 = parsePosition.index;
        ParsePosition parsePosition2 = new ParsePosition(0);
        int i5 = i4;
        int i6 = 0;
        while (i <= this.maxOffset) {
            int i7 = this.offsets[i] - i6;
            if (i7 == 0 || this.pattern.regionMatches(i6, str, i5, i7)) {
                int i8 = i5 + i7;
                i6 += i7;
                if (this.formats[i] == null) {
                    int length = i != this.maxOffset ? this.offsets[i + 1] : this.pattern.length();
                    if (i6 >= length) {
                        iIndexOf = str.length();
                    } else {
                        iIndexOf = str.indexOf(this.pattern.substring(i6, length), i8);
                    }
                    if (iIndexOf < 0) {
                        parsePosition.errorIndex = i8;
                        return null;
                    }
                    if (!str.substring(i8, iIndexOf).equals("{" + this.argumentNumbers[i] + "}")) {
                        objArr[this.argumentNumbers[i]] = str.substring(i8, iIndexOf);
                    }
                    i5 = iIndexOf;
                } else {
                    parsePosition2.index = i8;
                    objArr[this.argumentNumbers[i]] = this.formats[i].parseObject(str, parsePosition2);
                    if (parsePosition2.index == i8) {
                        parsePosition.errorIndex = i8;
                        return null;
                    }
                    i5 = parsePosition2.index;
                }
                i++;
            } else {
                parsePosition.errorIndex = i5;
                return null;
            }
        }
        int length2 = this.pattern.length() - i6;
        if (length2 == 0 || this.pattern.regionMatches(i6, str, i5, length2)) {
            parsePosition.index = i5 + length2;
            return objArr;
        }
        parsePosition.errorIndex = i5;
        return null;
    }

    public Object[] parse(String str) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        Object[] objArr = parse(str, parsePosition);
        if (parsePosition.index == 0) {
            throw new ParseException("MessageFormat parse error!", parsePosition.errorIndex);
        }
        return objArr;
    }

    @Override
    public Object parseObject(String str, ParsePosition parsePosition) {
        return parse(str, parsePosition);
    }

    @Override
    public Object clone() {
        MessageFormat messageFormat = (MessageFormat) super.clone();
        messageFormat.formats = (Format[]) this.formats.clone();
        for (int i = 0; i < this.formats.length; i++) {
            if (this.formats[i] != null) {
                messageFormat.formats[i] = (Format) this.formats[i].clone();
            }
        }
        messageFormat.offsets = (int[]) this.offsets.clone();
        messageFormat.argumentNumbers = (int[]) this.argumentNumbers.clone();
        return messageFormat;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MessageFormat messageFormat = (MessageFormat) obj;
        if (this.maxOffset == messageFormat.maxOffset && this.pattern.equals(messageFormat.pattern) && (((this.locale != null && this.locale.equals(messageFormat.locale)) || (this.locale == null && messageFormat.locale == null)) && Arrays.equals(this.offsets, messageFormat.offsets) && Arrays.equals(this.argumentNumbers, messageFormat.argumentNumbers) && Arrays.equals(this.formats, messageFormat.formats))) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return this.pattern.hashCode();
    }

    public static class Field extends Format.Field {
        public static final Field ARGUMENT = new Field("message argument field");
        private static final long serialVersionUID = 7899943957617360810L;

        protected Field(String str) {
            super(str);
        }

        @Override
        protected Object readResolve() throws InvalidObjectException {
            if (getClass() != Field.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }
            return ARGUMENT;
        }
    }

    private StringBuffer subformat(Object[] objArr, StringBuffer stringBuffer, FieldPosition fieldPosition, List<AttributedCharacterIterator> list) {
        String string;
        Format dateTimeInstance;
        String str;
        int length = stringBuffer.length();
        int i = 0;
        for (int i2 = 0; i2 <= this.maxOffset; i2++) {
            stringBuffer.append(this.pattern.substring(i, this.offsets[i2]));
            i = this.offsets[i2];
            int i3 = this.argumentNumbers[i2];
            if (objArr == null || i3 >= objArr.length) {
                stringBuffer.append('{');
                stringBuffer.append(i3);
                stringBuffer.append('}');
            } else {
                Object obj = objArr[i3];
                String str2 = null;
                if (obj == null) {
                    string = "null";
                } else {
                    if (this.formats[i2] != null) {
                        Format format = this.formats[i2];
                        if (format instanceof ChoiceFormat) {
                            str = this.formats[i2].format(obj);
                            if (str.indexOf(123) >= 0) {
                                dateTimeInstance = new MessageFormat(str, this.locale);
                                obj = objArr;
                            }
                        } else {
                            str = null;
                        }
                        dateTimeInstance = format;
                        if (list != null) {
                            if (length != stringBuffer.length()) {
                                list.add(createAttributedCharacterIterator(stringBuffer.substring(length)));
                                length = stringBuffer.length();
                            }
                            if (dateTimeInstance != null) {
                                AttributedCharacterIterator toCharacterIterator = dateTimeInstance.formatToCharacterIterator(obj);
                                append(stringBuffer, toCharacterIterator);
                                if (length != stringBuffer.length()) {
                                    list.add(createAttributedCharacterIterator(toCharacterIterator, Field.ARGUMENT, Integer.valueOf(i3)));
                                    length = stringBuffer.length();
                                }
                            } else {
                                str2 = str;
                            }
                            if (str2 != null && str2.length() > 0) {
                                stringBuffer.append(str2);
                                list.add(createAttributedCharacterIterator(str2, Field.ARGUMENT, Integer.valueOf(i3)));
                                length = stringBuffer.length();
                            }
                        } else {
                            if (dateTimeInstance != null) {
                                str = dateTimeInstance.format(obj);
                            }
                            int length2 = stringBuffer.length();
                            stringBuffer.append(str);
                            if (i2 == 0 && fieldPosition != null && Field.ARGUMENT.equals(fieldPosition.getFieldAttribute())) {
                                fieldPosition.setBeginIndex(length2);
                                fieldPosition.setEndIndex(stringBuffer.length());
                            }
                            length = stringBuffer.length();
                        }
                    } else if (obj instanceof Number) {
                        dateTimeInstance = NumberFormat.getInstance(this.locale);
                        obj = obj;
                    } else if (obj instanceof Date) {
                        dateTimeInstance = DateFormat.getDateTimeInstance(3, 3, this.locale);
                        obj = obj;
                    } else if (obj instanceof String) {
                        string = (String) obj;
                    } else {
                        string = obj.toString();
                        if (string == null) {
                            string = "null";
                        }
                    }
                    str = null;
                    if (list != null) {
                    }
                }
                str = string;
                dateTimeInstance = null;
                if (list != null) {
                }
            }
        }
        stringBuffer.append(this.pattern.substring(i, this.pattern.length()));
        if (list != null && length != stringBuffer.length()) {
            list.add(createAttributedCharacterIterator(stringBuffer.substring(length)));
        }
        return stringBuffer;
    }

    private void append(StringBuffer stringBuffer, CharacterIterator characterIterator) {
        if (characterIterator.first() != 65535) {
            stringBuffer.append(characterIterator.first());
            while (true) {
                char next = characterIterator.next();
                if (next != 65535) {
                    stringBuffer.append(next);
                } else {
                    return;
                }
            }
        }
    }

    private void makeFormat(int i, int i2, StringBuilder[] sbArr) {
        String[] strArr = new String[sbArr.length];
        for (int i3 = 0; i3 < sbArr.length; i3++) {
            StringBuilder sb = sbArr[i3];
            strArr[i3] = sb != null ? sb.toString() : "";
        }
        try {
            int i4 = Integer.parseInt(strArr[1]);
            if (i4 < 0) {
                throw new IllegalArgumentException("negative argument number: " + i4);
            }
            if (i2 >= this.formats.length) {
                int length = this.formats.length * 2;
                Format[] formatArr = new Format[length];
                int[] iArr = new int[length];
                int[] iArr2 = new int[length];
                System.arraycopy(this.formats, 0, formatArr, 0, this.maxOffset + 1);
                System.arraycopy((Object) this.offsets, 0, (Object) iArr, 0, this.maxOffset + 1);
                System.arraycopy((Object) this.argumentNumbers, 0, (Object) iArr2, 0, this.maxOffset + 1);
                this.formats = formatArr;
                this.offsets = iArr;
                this.argumentNumbers = iArr2;
            }
            int i5 = this.maxOffset;
            this.maxOffset = i2;
            this.offsets[i2] = strArr[0].length();
            this.argumentNumbers[i2] = i4;
            Format choiceFormat = null;
            if (strArr[2].length() != 0) {
                int iFindKeyword = findKeyword(strArr[2], TYPE_KEYWORDS);
                switch (iFindKeyword) {
                    case 0:
                        break;
                    case 1:
                        switch (findKeyword(strArr[3], NUMBER_MODIFIER_KEYWORDS)) {
                            case 0:
                                choiceFormat = NumberFormat.getInstance(this.locale);
                                break;
                            case 1:
                                choiceFormat = NumberFormat.getCurrencyInstance(this.locale);
                                break;
                            case 2:
                                choiceFormat = NumberFormat.getPercentInstance(this.locale);
                                break;
                            case 3:
                                choiceFormat = NumberFormat.getIntegerInstance(this.locale);
                                break;
                            default:
                                try {
                                    choiceFormat = new DecimalFormat(strArr[3], DecimalFormatSymbols.getInstance(this.locale));
                                } catch (IllegalArgumentException e) {
                                    this.maxOffset = i5;
                                    throw e;
                                }
                                break;
                        }
                        break;
                    case 2:
                    case 3:
                        int iFindKeyword2 = findKeyword(strArr[3], DATE_TIME_MODIFIER_KEYWORDS);
                        if (iFindKeyword2 >= 0 && iFindKeyword2 < DATE_TIME_MODIFIER_KEYWORDS.length) {
                            choiceFormat = iFindKeyword == 2 ? DateFormat.getDateInstance(DATE_TIME_MODIFIERS[iFindKeyword2], this.locale) : DateFormat.getTimeInstance(DATE_TIME_MODIFIERS[iFindKeyword2], this.locale);
                        } else {
                            try {
                                choiceFormat = new SimpleDateFormat(strArr[3], this.locale);
                            } catch (IllegalArgumentException e2) {
                                this.maxOffset = i5;
                                throw e2;
                            }
                        }
                        break;
                    case 4:
                        try {
                            choiceFormat = new ChoiceFormat(strArr[3]);
                        } catch (Exception e3) {
                            this.maxOffset = i5;
                            throw new IllegalArgumentException("Choice Pattern incorrect: " + strArr[3], e3);
                        }
                        break;
                    default:
                        this.maxOffset = i5;
                        throw new IllegalArgumentException("unknown format type: " + strArr[2]);
                }
            }
            this.formats[i2] = choiceFormat;
        } catch (NumberFormatException e4) {
            throw new IllegalArgumentException("can't parse argument number: " + strArr[1], e4);
        }
    }

    private static final int findKeyword(String str, String[] strArr) {
        for (int i = 0; i < strArr.length; i++) {
            if (str.equals(strArr[i])) {
                return i;
            }
        }
        String lowerCase = str.trim().toLowerCase(Locale.ROOT);
        if (lowerCase != str) {
            for (int i2 = 0; i2 < strArr.length; i2++) {
                if (lowerCase.equals(strArr[i2])) {
                    return i2;
                }
            }
            return -1;
        }
        return -1;
    }

    private static final void copyAndFixQuotes(String str, int i, int i2, StringBuilder sb) {
        boolean z = false;
        while (i < i2) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '{') {
                if (!z) {
                    sb.append('\'');
                    z = true;
                }
                sb.append(cCharAt);
            } else if (cCharAt == '\'') {
                sb.append("''");
            } else {
                if (z) {
                    sb.append('\'');
                    z = false;
                }
                sb.append(cCharAt);
            }
            i++;
        }
        if (z) {
            sb.append('\'');
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        boolean z = this.maxOffset >= -1 && this.formats.length > this.maxOffset && this.offsets.length > this.maxOffset && this.argumentNumbers.length > this.maxOffset;
        if (z) {
            int length = this.pattern.length() + 1;
            for (int i = this.maxOffset; i >= 0; i--) {
                if (this.offsets[i] >= 0 && this.offsets[i] <= length) {
                    length = this.offsets[i];
                } else {
                    z = false;
                    break;
                }
            }
        }
        if (!z) {
            throw new InvalidObjectException("Could not reconstruct MessageFormat from corrupt stream.");
        }
    }
}
