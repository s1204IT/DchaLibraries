package java.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.text.AttributedCharacterIterator;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import libcore.util.EmptyArray;

public class MessageFormat extends Format {
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("argumentNumbers", (Class<?>) int[].class), new ObjectStreamField("formats", (Class<?>) Format[].class), new ObjectStreamField("locale", (Class<?>) Locale.class), new ObjectStreamField("maxOffset", Integer.TYPE), new ObjectStreamField("offsets", (Class<?>) int[].class), new ObjectStreamField("pattern", (Class<?>) String.class)};
    private static final long serialVersionUID = 6479157306784022952L;
    private int[] argumentNumbers;
    private Format[] formats;
    private Locale locale;
    private transient int maxArgumentIndex;
    private int maxOffset;
    private transient String[] strings;

    public MessageFormat(String template, Locale locale) {
        this.locale = locale;
        applyPattern(template);
    }

    public MessageFormat(String template) {
        this(template, Locale.getDefault());
    }

    public void applyPattern(String template) {
        int offset;
        int length = template.length();
        StringBuffer buffer = new StringBuffer();
        ParsePosition position = new ParsePosition(0);
        ArrayList<String> localStrings = new ArrayList<>();
        int argCount = 0;
        int[] args = new int[10];
        int maxArg = -1;
        ArrayList<Format> localFormats = new ArrayList<>();
        loop0: while (position.getIndex() < length) {
            if (Format.upTo(template, position, buffer, '{')) {
                int arg = 0;
                int offset2 = position.getIndex();
                if (offset2 >= length) {
                    throw new IllegalArgumentException("Invalid argument number");
                }
                while (true) {
                    offset = offset2 + 1;
                    char ch = template.charAt(offset2);
                    if (ch == '}' || ch == ',') {
                        break;
                    }
                    if (ch < '0' && ch > '9') {
                        throw new IllegalArgumentException("Invalid argument number");
                    }
                    arg = (arg * 10) + (ch - '0');
                    if (arg < 0 || offset >= length) {
                        break loop0;
                    } else {
                        offset2 = offset;
                    }
                }
                position.setIndex(offset - 1);
                localFormats.add(parseVariable(template, position));
                if (argCount >= args.length) {
                    int[] newArgs = new int[args.length * 2];
                    System.arraycopy(args, 0, newArgs, 0, args.length);
                    args = newArgs;
                }
                int argCount2 = argCount + 1;
                args[argCount] = arg;
                if (arg > maxArg) {
                    maxArg = arg;
                    argCount = argCount2;
                } else {
                    argCount = argCount2;
                }
            }
            localStrings.add(buffer.toString());
            buffer.setLength(0);
        }
        this.strings = (String[]) localStrings.toArray(new String[localStrings.size()]);
        this.argumentNumbers = args;
        this.formats = (Format[]) localFormats.toArray(new Format[argCount]);
        this.maxOffset = argCount - 1;
        this.maxArgumentIndex = maxArg;
    }

    @Override
    public Object clone() {
        MessageFormat clone = (MessageFormat) super.clone();
        Format[] array = new Format[this.formats.length];
        int i = this.formats.length;
        while (true) {
            i--;
            if (i >= 0) {
                if (this.formats[i] != null) {
                    array[i] = (Format) this.formats[i].clone();
                }
            } else {
                clone.formats = array;
                return clone;
            }
        }
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MessageFormat)) {
            return false;
        }
        MessageFormat format = (MessageFormat) object;
        if (this.maxOffset != format.maxOffset) {
            return false;
        }
        for (int i = 0; i <= this.maxOffset; i++) {
            if (this.argumentNumbers[i] != format.argumentNumbers[i]) {
                return false;
            }
        }
        return this.locale.equals(format.locale) && Arrays.equals(this.strings, format.strings) && Arrays.equals(this.formats, format.formats);
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object object) {
        if (object == null) {
            throw new NullPointerException("object == null");
        }
        StringBuffer buffer = new StringBuffer();
        ArrayList<FieldContainer> fields = new ArrayList<>();
        formatImpl((Object[]) object, buffer, new FieldPosition(0), fields);
        AttributedString as = new AttributedString(buffer.toString());
        for (FieldContainer fc : fields) {
            as.addAttribute(fc.attribute, fc.value, fc.start, fc.end);
        }
        return as.getIterator();
    }

    public final StringBuffer format(Object[] objects, StringBuffer buffer, FieldPosition field) {
        return formatImpl(objects, buffer, field, null);
    }

    private StringBuffer formatImpl(Object[] objects, StringBuffer buffer, FieldPosition position, List<FieldContainer> fields) {
        FieldPosition passedField = new FieldPosition(0);
        for (int i = 0; i <= this.maxOffset; i++) {
            buffer.append(this.strings[i]);
            int begin = buffer.length();
            if (objects != null && this.argumentNumbers[i] < objects.length) {
                Object arg = objects[this.argumentNumbers[i]];
                Format format = this.formats[i];
                if (format == null || arg == null) {
                    if (arg instanceof Number) {
                        format = NumberFormat.getInstance();
                    } else if (arg instanceof Date) {
                        format = DateFormat.getInstance();
                    } else {
                        buffer.append(arg);
                        handleArgumentField(begin, buffer.length(), this.argumentNumbers[i], position, fields);
                    }
                    if (!(format instanceof ChoiceFormat)) {
                        String result = format.format(arg);
                        MessageFormat mf = new MessageFormat(result);
                        mf.setLocale(this.locale);
                        mf.format(objects, buffer, passedField);
                        handleArgumentField(begin, buffer.length(), this.argumentNumbers[i], position, fields);
                        handleFormat(format, arg, begin, fields);
                    } else {
                        format.format(arg, buffer, passedField);
                        handleArgumentField(begin, buffer.length(), this.argumentNumbers[i], position, fields);
                        handleFormat(format, arg, begin, fields);
                    }
                } else if (!(format instanceof ChoiceFormat)) {
                }
            } else {
                buffer.append('{');
                buffer.append(this.argumentNumbers[i]);
                buffer.append('}');
                handleArgumentField(begin, buffer.length(), this.argumentNumbers[i], position, fields);
            }
        }
        if (this.maxOffset + 1 < this.strings.length) {
            buffer.append(this.strings[this.maxOffset + 1]);
        }
        return buffer;
    }

    private void handleArgumentField(int begin, int end, int argIndex, FieldPosition position, List<FieldContainer> fields) {
        if (fields != null) {
            fields.add(new FieldContainer(begin, end, Field.ARGUMENT, Integer.valueOf(argIndex)));
        } else if (position != null && position.getFieldAttribute() == Field.ARGUMENT && position.getEndIndex() == 0) {
            position.setBeginIndex(begin);
            position.setEndIndex(end);
        }
    }

    private static class FieldContainer {
        AttributedCharacterIterator.Attribute attribute;
        int end;
        int start;
        Object value;

        public FieldContainer(int start, int end, AttributedCharacterIterator.Attribute attribute, Object value) {
            this.start = start;
            this.end = end;
            this.attribute = attribute;
            this.value = value;
        }
    }

    private void handleFormat(Format format, Object arg, int begin, List<FieldContainer> fields) {
        if (fields != null) {
            AttributedCharacterIterator iterator = format.formatToCharacterIterator(arg);
            while (iterator.getIndex() != iterator.getEndIndex()) {
                int start = iterator.getRunStart();
                int end = iterator.getRunLimit();
                for (AttributedCharacterIterator.Attribute attribute : iterator.getAttributes().keySet()) {
                    Object value = iterator.getAttribute(attribute);
                    fields.add(new FieldContainer(begin + start, begin + end, attribute, value));
                }
                iterator.setIndex(end);
            }
        }
    }

    @Override
    public final StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
        return format((Object[]) object, buffer, field);
    }

    public static String format(String format, Object... args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    args[i] = "null";
                }
            }
        }
        return new MessageFormat(format).format(args);
    }

    public Format[] getFormats() {
        return (Format[]) this.formats.clone();
    }

    public Format[] getFormatsByArgumentIndex() {
        Format[] answer = new Format[this.maxArgumentIndex + 1];
        for (int i = 0; i < this.maxOffset + 1; i++) {
            answer[this.argumentNumbers[i]] = this.formats[i];
        }
        return answer;
    }

    public void setFormatByArgumentIndex(int argIndex, Format format) {
        for (int i = 0; i < this.maxOffset + 1; i++) {
            if (this.argumentNumbers[i] == argIndex) {
                this.formats[i] = format;
            }
        }
    }

    public void setFormatsByArgumentIndex(Format[] formats) {
        for (int j = 0; j < formats.length; j++) {
            for (int i = 0; i < this.maxOffset + 1; i++) {
                if (this.argumentNumbers[i] == j) {
                    this.formats[i] = formats[j];
                }
            }
        }
    }

    public Locale getLocale() {
        return this.locale;
    }

    public int hashCode() {
        int hashCode = 0;
        for (int i = 0; i <= this.maxOffset; i++) {
            hashCode += this.argumentNumbers[i] + this.strings[i].hashCode();
            if (this.formats[i] != null) {
                hashCode += this.formats[i].hashCode();
            }
        }
        if (this.maxOffset + 1 < this.strings.length) {
            hashCode += this.strings[this.maxOffset + 1].hashCode();
        }
        if (this.locale != null) {
            return hashCode + this.locale.hashCode();
        }
        return hashCode;
    }

    public Object[] parse(String string) throws ParseException {
        ParsePosition position = new ParsePosition(0);
        Object[] result = parse(string, position);
        if (position.getIndex() == 0) {
            throw new ParseException("Parse failure", position.getErrorIndex());
        }
        return result;
    }

    public Object[] parse(String string, ParsePosition position) {
        Object object;
        if (string == null) {
            return EmptyArray.OBJECT;
        }
        ParsePosition internalPos = new ParsePosition(0);
        int offset = position.getIndex();
        Object[] result = new Object[this.maxArgumentIndex + 1];
        for (int i = 0; i <= this.maxOffset; i++) {
            String sub = this.strings[i];
            if (!string.startsWith(sub, offset)) {
                position.setErrorIndex(offset);
                return null;
            }
            int offset2 = offset + sub.length();
            Format format = this.formats[i];
            if (format == null) {
                if (i + 1 < this.strings.length) {
                    int next = string.indexOf(this.strings[i + 1], offset2);
                    if (next == -1) {
                        position.setErrorIndex(offset2);
                        return null;
                    }
                    object = string.substring(offset2, next);
                    offset = next;
                } else {
                    object = string.substring(offset2);
                    offset = string.length();
                }
            } else {
                internalPos.setIndex(offset2);
                object = format.parseObject(string, internalPos);
                if (internalPos.getErrorIndex() != -1) {
                    position.setErrorIndex(offset2);
                    return null;
                }
                offset = internalPos.getIndex();
            }
            result[this.argumentNumbers[i]] = object;
        }
        if (this.maxOffset + 1 < this.strings.length) {
            String sub2 = this.strings[this.maxOffset + 1];
            if (!string.startsWith(sub2, offset)) {
                position.setErrorIndex(offset);
                return null;
            }
            offset += sub2.length();
        }
        position.setIndex(offset);
        return result;
    }

    @Override
    public Object parseObject(String string, ParsePosition position) {
        return parse(string, position);
    }

    private int match(String string, ParsePosition position, boolean last, String[] tokens) {
        char ch;
        int length = string.length();
        int offset = position.getIndex();
        int token = -1;
        while (offset < length && Character.isWhitespace(string.charAt(offset))) {
            offset++;
        }
        int i = tokens.length;
        while (true) {
            i--;
            if (i < 0) {
                break;
            }
            if (string.regionMatches(true, offset, tokens[i], 0, tokens[i].length())) {
                token = i;
                break;
            }
        }
        if (token == -1) {
            return -1;
        }
        int offset2 = offset + tokens[token].length();
        while (offset2 < length && Character.isWhitespace(string.charAt(offset2))) {
            offset2++;
        }
        if (offset2 >= length || ((ch = string.charAt(offset2)) != '}' && (last || ch != ','))) {
            return -1;
        }
        position.setIndex(offset2 + 1);
        return token;
    }

    private Format parseVariable(String string, ParsePosition position) {
        int length = string.length();
        int offset = position.getIndex();
        if (offset < length) {
            int offset2 = offset + 1;
            char ch = string.charAt(offset);
            if (ch == '}' || ch == ',') {
                position.setIndex(offset2);
                if (ch == '}') {
                    return null;
                }
                int type = match(string, position, false, new String[]{"time", "date", "number", "choice"});
                if (type == -1) {
                    throw new IllegalArgumentException("Unknown element format");
                }
                StringBuffer buffer = new StringBuffer();
                char ch2 = string.charAt(position.getIndex() - 1);
                switch (type) {
                    case 0:
                    case 1:
                        if (ch2 == '}') {
                            return type == 1 ? DateFormat.getDateInstance(2, this.locale) : DateFormat.getTimeInstance(2, this.locale);
                        }
                        int dateStyle = match(string, position, true, new String[]{"full", "long", "medium", "short"});
                        if (dateStyle == -1) {
                            Format.upToWithQuotes(string, position, buffer, '}', '{');
                            return new SimpleDateFormat(buffer.toString(), this.locale);
                        }
                        switch (dateStyle) {
                            case 0:
                                dateStyle = 0;
                                break;
                            case 1:
                                dateStyle = 1;
                                break;
                            case 2:
                                dateStyle = 2;
                                break;
                            case 3:
                                dateStyle = 3;
                                break;
                        }
                        return type == 1 ? DateFormat.getDateInstance(dateStyle, this.locale) : DateFormat.getTimeInstance(dateStyle, this.locale);
                    case 2:
                        if (ch2 == '}') {
                            return NumberFormat.getInstance(this.locale);
                        }
                        int numberStyle = match(string, position, true, new String[]{"currency", "percent", "integer"});
                        if (numberStyle == -1) {
                            Format.upToWithQuotes(string, position, buffer, '}', '{');
                            return new DecimalFormat(buffer.toString(), new DecimalFormatSymbols(this.locale));
                        }
                        switch (numberStyle) {
                            case 0:
                                return NumberFormat.getCurrencyInstance(this.locale);
                            case 1:
                                return NumberFormat.getPercentInstance(this.locale);
                            default:
                                return NumberFormat.getIntegerInstance(this.locale);
                        }
                    default:
                        try {
                            Format.upToWithQuotes(string, position, buffer, '}', '{');
                            break;
                        } catch (IllegalArgumentException e) {
                        }
                        return new ChoiceFormat(buffer.toString());
                }
            }
        }
        throw new IllegalArgumentException("Missing element format");
    }

    public void setFormat(int offset, Format format) {
        this.formats[offset] = format;
    }

    public void setFormats(Format[] formats) {
        int min = this.formats.length;
        if (formats.length < min) {
            min = formats.length;
        }
        for (int i = 0; i < min; i++) {
            this.formats[i] = formats[i];
        }
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        for (int i = 0; i <= this.maxOffset; i++) {
            Format format = this.formats[i];
            if (format instanceof DecimalFormat) {
                try {
                    this.formats[i] = new DecimalFormat(((DecimalFormat) format).toPattern(), new DecimalFormatSymbols(locale));
                } catch (NullPointerException e) {
                    this.formats[i] = null;
                }
            } else if (format instanceof SimpleDateFormat) {
                try {
                    this.formats[i] = new SimpleDateFormat(((SimpleDateFormat) format).toPattern(), locale);
                } catch (NullPointerException e2) {
                    this.formats[i] = null;
                }
            }
        }
    }

    private String decodeDecimalFormat(StringBuffer buffer, Format format) {
        buffer.append(",number");
        if (!format.equals(NumberFormat.getNumberInstance(this.locale))) {
            if (format.equals(NumberFormat.getIntegerInstance(this.locale))) {
                buffer.append(",integer");
            } else if (format.equals(NumberFormat.getCurrencyInstance(this.locale))) {
                buffer.append(",currency");
            } else if (format.equals(NumberFormat.getPercentInstance(this.locale))) {
                buffer.append(",percent");
            } else {
                buffer.append(',');
                return ((DecimalFormat) format).toPattern();
            }
        }
        return null;
    }

    private String decodeSimpleDateFormat(StringBuffer buffer, Format format) {
        if (format.equals(DateFormat.getTimeInstance(2, this.locale))) {
            buffer.append(",time");
        } else if (format.equals(DateFormat.getDateInstance(2, this.locale))) {
            buffer.append(",date");
        } else if (format.equals(DateFormat.getTimeInstance(3, this.locale))) {
            buffer.append(",time,short");
        } else if (format.equals(DateFormat.getDateInstance(3, this.locale))) {
            buffer.append(",date,short");
        } else if (format.equals(DateFormat.getTimeInstance(1, this.locale))) {
            buffer.append(",time,long");
        } else if (format.equals(DateFormat.getDateInstance(1, this.locale))) {
            buffer.append(",date,long");
        } else if (format.equals(DateFormat.getTimeInstance(0, this.locale))) {
            buffer.append(",time,full");
        } else if (format.equals(DateFormat.getDateInstance(0, this.locale))) {
            buffer.append(",date,full");
        } else {
            buffer.append(",date,");
            return ((SimpleDateFormat) format).toPattern();
        }
        return null;
    }

    public String toPattern() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i <= this.maxOffset; i++) {
            appendQuoted(buffer, this.strings[i]);
            buffer.append('{');
            buffer.append(this.argumentNumbers[i]);
            Format format = this.formats[i];
            String pattern = null;
            if (format instanceof ChoiceFormat) {
                buffer.append(",choice,");
                pattern = ((ChoiceFormat) format).toPattern();
            } else if (format instanceof DecimalFormat) {
                pattern = decodeDecimalFormat(buffer, format);
            } else if (format instanceof SimpleDateFormat) {
                pattern = decodeSimpleDateFormat(buffer, format);
            } else if (format != null) {
                throw new IllegalArgumentException("Unknown format");
            }
            if (pattern != null) {
                boolean quote = false;
                int length = pattern.length();
                int count = 0;
                int index = 0;
                while (index < length) {
                    int index2 = index + 1;
                    char ch = pattern.charAt(index);
                    if (ch == '\'') {
                        quote = !quote;
                    }
                    if (!quote) {
                        if (ch == '{') {
                            count++;
                        }
                        if (ch == '}') {
                            if (count > 0) {
                                count--;
                            } else {
                                buffer.append("'}");
                                ch = '\'';
                            }
                        }
                    }
                    buffer.append(ch);
                    index = index2;
                }
            }
            buffer.append('}');
        }
        if (this.maxOffset + 1 < this.strings.length) {
            appendQuoted(buffer, this.strings[this.maxOffset + 1]);
        }
        return buffer.toString();
    }

    private void appendQuoted(StringBuffer buffer, String string) {
        int length = string.length();
        for (int i = 0; i < length; i++) {
            char ch = string.charAt(i);
            if (ch == '{' || ch == '}') {
                buffer.append('\'');
                buffer.append(ch);
                buffer.append('\'');
            } else {
                buffer.append(ch);
            }
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("argumentNumbers", this.argumentNumbers);
        Format[] compatibleFormats = this.formats;
        fields.put("formats", compatibleFormats);
        fields.put("locale", this.locale);
        fields.put("maxOffset", this.maxOffset);
        int offset = 0;
        int offsetsLength = this.maxOffset + 1;
        int[] offsets = new int[offsetsLength];
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i <= this.maxOffset; i++) {
            offset += this.strings[i].length();
            offsets[i] = offset;
            pattern.append(this.strings[i]);
        }
        if (this.maxOffset + 1 < this.strings.length) {
            pattern.append(this.strings[this.maxOffset + 1]);
        }
        fields.put("offsets", offsets);
        fields.put("pattern", pattern.toString());
        stream.writeFields();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        this.argumentNumbers = (int[]) fields.get("argumentNumbers", (Object) null);
        this.formats = (Format[]) fields.get("formats", (Object) null);
        this.locale = (Locale) fields.get("locale", (Object) null);
        this.maxOffset = fields.get("maxOffset", 0);
        int[] offsets = (int[]) fields.get("offsets", (Object) null);
        String pattern = (String) fields.get("pattern", (Object) null);
        if (this.maxOffset < 0) {
            if (pattern.length() <= 0) {
                length = 0;
            }
        } else {
            int i = this.maxOffset;
            length = offsets[this.maxOffset] != pattern.length() ? 2 : 1;
            length += i;
        }
        this.strings = new String[length];
        int last = 0;
        for (int i2 = 0; i2 <= this.maxOffset; i2++) {
            this.strings[i2] = pattern.substring(last, offsets[i2]);
            last = offsets[i2];
        }
        if (this.maxOffset + 1 < this.strings.length) {
            this.strings[this.strings.length - 1] = pattern.substring(last, pattern.length());
        }
    }

    public static class Field extends Format.Field {
        public static final Field ARGUMENT = new Field("message argument field");
        private static final long serialVersionUID = 7899943957617360810L;

        protected Field(String fieldName) {
            super(fieldName);
        }
    }
}
