package android.icu.text;

import android.icu.impl.PatternProps;
import android.icu.impl.Utility;
import android.icu.text.MessagePattern;
import android.icu.text.PluralFormat;
import android.icu.text.PluralRules;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.text.ChoiceFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MessageFormat extends UFormat {
    static final boolean $assertionsDisabled = false;
    private static final char CURLY_BRACE_LEFT = '{';
    private static final char CURLY_BRACE_RIGHT = '}';
    private static final int DATE_MODIFIER_EMPTY = 0;
    private static final int DATE_MODIFIER_FULL = 4;
    private static final int DATE_MODIFIER_LONG = 3;
    private static final int DATE_MODIFIER_MEDIUM = 2;
    private static final int DATE_MODIFIER_SHORT = 1;
    private static final int MODIFIER_CURRENCY = 1;
    private static final int MODIFIER_EMPTY = 0;
    private static final int MODIFIER_INTEGER = 3;
    private static final int MODIFIER_PERCENT = 2;
    private static final char SINGLE_QUOTE = '\'';
    private static final int STATE_INITIAL = 0;
    private static final int STATE_IN_QUOTE = 2;
    private static final int STATE_MSG_ELEMENT = 3;
    private static final int STATE_SINGLE_QUOTE = 1;
    private static final int TYPE_DATE = 1;
    private static final int TYPE_DURATION = 5;
    private static final int TYPE_NUMBER = 0;
    private static final int TYPE_ORDINAL = 4;
    private static final int TYPE_SPELLOUT = 3;
    private static final int TYPE_TIME = 2;
    static final long serialVersionUID = 7136212545847378652L;
    private transient Map<Integer, Format> cachedFormatters;
    private transient Set<Integer> customFormatArgStarts;
    private transient MessagePattern msgPattern;
    private transient PluralSelectorProvider ordinalProvider;
    private transient PluralSelectorProvider pluralProvider;
    private transient DateFormat stockDateFormatter;
    private transient NumberFormat stockNumberFormatter;
    private transient ULocale ulocale;
    private static final String[] typeList = {"number", "date", "time", "spellout", "ordinal", "duration"};
    private static final String[] modifierList = {"", "currency", "percent", "integer"};
    private static final String[] dateModifierList = {"", "short", "medium", "long", "full"};
    private static final Locale rootLocale = new Locale("");

    public MessageFormat(String str) {
        this.ulocale = ULocale.getDefault(ULocale.Category.FORMAT);
        applyPattern(str);
    }

    public MessageFormat(String str, Locale locale) {
        this(str, ULocale.forLocale(locale));
    }

    public MessageFormat(String str, ULocale uLocale) {
        this.ulocale = uLocale;
        applyPattern(str);
    }

    public void setLocale(Locale locale) {
        setLocale(ULocale.forLocale(locale));
    }

    public void setLocale(ULocale uLocale) {
        String pattern = toPattern();
        this.ulocale = uLocale;
        this.stockDateFormatter = null;
        this.stockNumberFormatter = null;
        this.pluralProvider = null;
        this.ordinalProvider = null;
        applyPattern(pattern);
    }

    public Locale getLocale() {
        return this.ulocale.toLocale();
    }

    public ULocale getULocale() {
        return this.ulocale;
    }

    public void applyPattern(String str) {
        try {
            if (this.msgPattern == null) {
                this.msgPattern = new MessagePattern(str);
            } else {
                this.msgPattern.parse(str);
            }
            cacheExplicitFormats();
        } catch (RuntimeException e) {
            resetPattern();
            throw e;
        }
    }

    public void applyPattern(String str, MessagePattern.ApostropheMode apostropheMode) {
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern(apostropheMode);
        } else if (apostropheMode != this.msgPattern.getApostropheMode()) {
            this.msgPattern.clearPatternAndSetApostropheMode(apostropheMode);
        }
        applyPattern(str);
    }

    public MessagePattern.ApostropheMode getApostropheMode() {
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern();
        }
        return this.msgPattern.getApostropheMode();
    }

    public String toPattern() {
        String patternString;
        if (this.customFormatArgStarts == null) {
            return (this.msgPattern == null || (patternString = this.msgPattern.getPatternString()) == null) ? "" : patternString;
        }
        throw new IllegalStateException("toPattern() is not supported after custom Format objects have been set via setFormat() or similar APIs");
    }

    private int nextTopLevelArgStart(int i) {
        MessagePattern.Part.Type partType;
        if (i != 0) {
            i = this.msgPattern.getLimitPartIndex(i);
        }
        do {
            i++;
            partType = this.msgPattern.getPartType(i);
            if (partType == MessagePattern.Part.Type.ARG_START) {
                return i;
            }
        } while (partType != MessagePattern.Part.Type.MSG_LIMIT);
        return -1;
    }

    private boolean argNameMatches(int i, String str, int i2) {
        MessagePattern.Part part = this.msgPattern.getPart(i);
        if (part.getType() == MessagePattern.Part.Type.ARG_NAME) {
            return this.msgPattern.partSubstringMatches(part, str);
        }
        return part.getValue() == i2;
    }

    private String getArgName(int i) {
        MessagePattern.Part part = this.msgPattern.getPart(i);
        if (part.getType() == MessagePattern.Part.Type.ARG_NAME) {
            return this.msgPattern.getSubstring(part);
        }
        return Integer.toString(part.getValue());
    }

    public void setFormatsByArgumentIndex(Format[] formatArr) {
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
        }
        int iNextTopLevelArgStart = 0;
        while (true) {
            iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart);
            if (iNextTopLevelArgStart >= 0) {
                int value = this.msgPattern.getPart(iNextTopLevelArgStart + 1).getValue();
                if (value < formatArr.length) {
                    setCustomArgStartFormat(iNextTopLevelArgStart, formatArr[value]);
                }
            } else {
                return;
            }
        }
    }

    public void setFormatsByArgumentName(Map<String, Format> map) {
        int iNextTopLevelArgStart = 0;
        while (true) {
            iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart);
            if (iNextTopLevelArgStart >= 0) {
                String argName = getArgName(iNextTopLevelArgStart + 1);
                if (map.containsKey(argName)) {
                    setCustomArgStartFormat(iNextTopLevelArgStart, map.get(argName));
                }
            } else {
                return;
            }
        }
    }

    public void setFormats(Format[] formatArr) {
        int iNextTopLevelArgStart = 0;
        for (int i = 0; i < formatArr.length && (iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart)) >= 0; i++) {
            setCustomArgStartFormat(iNextTopLevelArgStart, formatArr[i]);
        }
    }

    public void setFormatByArgumentIndex(int i, Format format) {
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
        }
        int iNextTopLevelArgStart = 0;
        while (true) {
            iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart);
            if (iNextTopLevelArgStart >= 0) {
                if (this.msgPattern.getPart(iNextTopLevelArgStart + 1).getValue() == i) {
                    setCustomArgStartFormat(iNextTopLevelArgStart, format);
                }
            } else {
                return;
            }
        }
    }

    public void setFormatByArgumentName(String str, Format format) {
        int iValidateArgumentName = MessagePattern.validateArgumentName(str);
        if (iValidateArgumentName < -1) {
            return;
        }
        int iNextTopLevelArgStart = 0;
        while (true) {
            iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart);
            if (iNextTopLevelArgStart >= 0) {
                if (argNameMatches(iNextTopLevelArgStart + 1, str, iValidateArgumentName)) {
                    setCustomArgStartFormat(iNextTopLevelArgStart, format);
                }
            } else {
                return;
            }
        }
    }

    public void setFormat(int i, Format format) {
        int iNextTopLevelArgStart = 0;
        int i2 = 0;
        while (true) {
            iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart);
            if (iNextTopLevelArgStart >= 0) {
                if (i2 == i) {
                    setCustomArgStartFormat(iNextTopLevelArgStart, format);
                    return;
                }
                i2++;
            } else {
                throw new ArrayIndexOutOfBoundsException(i);
            }
        }
    }

    public Format[] getFormatsByArgumentIndex() {
        Format format;
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
        }
        ArrayList arrayList = new ArrayList();
        int iNextTopLevelArgStart = 0;
        while (true) {
            iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart);
            if (iNextTopLevelArgStart >= 0) {
                int value = this.msgPattern.getPart(iNextTopLevelArgStart + 1).getValue();
                while (true) {
                    format = null;
                    if (value < arrayList.size()) {
                        break;
                    }
                    arrayList.add(null);
                }
                if (this.cachedFormatters != null) {
                    format = this.cachedFormatters.get(Integer.valueOf(iNextTopLevelArgStart));
                }
                arrayList.set(value, format);
            } else {
                return (Format[]) arrayList.toArray(new Format[arrayList.size()]);
            }
        }
    }

    public Format[] getFormats() {
        ArrayList arrayList = new ArrayList();
        int iNextTopLevelArgStart = 0;
        while (true) {
            iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart);
            if (iNextTopLevelArgStart >= 0) {
                arrayList.add(this.cachedFormatters == null ? null : this.cachedFormatters.get(Integer.valueOf(iNextTopLevelArgStart)));
            } else {
                return (Format[]) arrayList.toArray(new Format[arrayList.size()]);
            }
        }
    }

    public Set<String> getArgumentNames() {
        HashSet hashSet = new HashSet();
        int iNextTopLevelArgStart = 0;
        while (true) {
            iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart);
            if (iNextTopLevelArgStart >= 0) {
                hashSet.add(getArgName(iNextTopLevelArgStart + 1));
            } else {
                return hashSet;
            }
        }
    }

    public Format getFormatByArgumentName(String str) {
        int iValidateArgumentName;
        if (this.cachedFormatters == null || (iValidateArgumentName = MessagePattern.validateArgumentName(str)) < -1) {
            return null;
        }
        int iNextTopLevelArgStart = 0;
        do {
            iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart);
            if (iNextTopLevelArgStart < 0) {
                return null;
            }
        } while (!argNameMatches(iNextTopLevelArgStart + 1, str, iValidateArgumentName));
        return this.cachedFormatters.get(Integer.valueOf(iNextTopLevelArgStart));
    }

    public final StringBuffer format(Object[] objArr, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        format(objArr, null, new AppendableWrapper(stringBuffer), fieldPosition);
        return stringBuffer;
    }

    public final StringBuffer format(Map<String, Object> map, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        format(null, map, new AppendableWrapper(stringBuffer), fieldPosition);
        return stringBuffer;
    }

    public static String format(String str, Object... objArr) {
        return new MessageFormat(str).format(objArr);
    }

    public static String format(String str, Map<String, Object> map) {
        return new MessageFormat(str).format(map);
    }

    public boolean usesNamedArguments() {
        return this.msgPattern.hasNamedArguments();
    }

    @Override
    public final StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        format(obj, new AppendableWrapper(stringBuffer), fieldPosition);
        return stringBuffer;
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (obj == null) {
            throw new NullPointerException("formatToCharacterIterator must be passed non-null object");
        }
        StringBuilder sb = new StringBuilder();
        AppendableWrapper appendableWrapper = new AppendableWrapper(sb);
        appendableWrapper.useAttributes();
        format(obj, appendableWrapper, (FieldPosition) null);
        AttributedString attributedString = new AttributedString(sb.toString());
        for (AttributeAndPosition attributeAndPosition : appendableWrapper.attributes) {
            attributedString.addAttribute(attributeAndPosition.key, attributeAndPosition.value, attributeAndPosition.start, attributeAndPosition.limit);
        }
        return attributedString.getIterator();
    }

    public Object[] parse(String str, ParsePosition parsePosition) {
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use named argument.");
        }
        int i = -1;
        int iNextTopLevelArgStart = 0;
        while (true) {
            iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart);
            if (iNextTopLevelArgStart < 0) {
                break;
            }
            int value = this.msgPattern.getPart(iNextTopLevelArgStart + 1).getValue();
            if (value > i) {
                i = value;
            }
        }
        Object[] objArr = new Object[i + 1];
        int index = parsePosition.getIndex();
        parse(0, str, parsePosition, objArr, null);
        if (parsePosition.getIndex() == index) {
            return null;
        }
        return objArr;
    }

    public Map<String, Object> parseToMap(String str, ParsePosition parsePosition) {
        HashMap map = new HashMap();
        int index = parsePosition.getIndex();
        parse(0, str, parsePosition, null, map);
        if (parsePosition.getIndex() == index) {
            return null;
        }
        return map;
    }

    public Object[] parse(String str) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        Object[] objArr = parse(str, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new ParseException("MessageFormat parse error!", parsePosition.getErrorIndex());
        }
        return objArr;
    }

    private void parse(int i, String str, ParsePosition parsePosition, Object[] objArr, Map<String, Object> map) {
        Object string;
        Object obj;
        int value;
        int length;
        boolean z;
        Object objValueOf;
        Format format;
        if (str == null) {
            return;
        }
        String patternString = this.msgPattern.getPatternString();
        int limit = this.msgPattern.getPart(i).getLimit();
        int index = parsePosition.getIndex();
        ParsePosition parsePosition2 = new ParsePosition(0);
        int i2 = i + 1;
        while (true) {
            MessagePattern.Part part = this.msgPattern.getPart(i2);
            MessagePattern.Part.Type type = part.getType();
            int index2 = part.getIndex() - limit;
            if (index2 == 0 || patternString.regionMatches(limit, str, index, index2)) {
                index += index2;
                if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                    parsePosition.setIndex(index);
                    return;
                }
                if (type == MessagePattern.Part.Type.SKIP_SYNTAX || type == MessagePattern.Part.Type.INSERT_CHAR) {
                    limit = part.getLimit();
                } else {
                    int limitPartIndex = this.msgPattern.getLimitPartIndex(i2);
                    MessagePattern.ArgType argType = part.getArgType();
                    int i3 = i2 + 1;
                    MessagePattern.Part part2 = this.msgPattern.getPart(i3);
                    if (objArr != null) {
                        value = part2.getValue();
                        string = Integer.valueOf(value);
                        obj = null;
                    } else {
                        if (part2.getType() == MessagePattern.Part.Type.ARG_NAME) {
                            string = this.msgPattern.getSubstring(part2);
                        } else {
                            string = Integer.toString(part2.getValue());
                        }
                        obj = string;
                        value = 0;
                    }
                    int i4 = i3 + 1;
                    if (this.cachedFormatters != null && (format = this.cachedFormatters.get(Integer.valueOf(i4 - 2))) != null) {
                        parsePosition2.setIndex(index);
                        objValueOf = format.parseObject(str, parsePosition2);
                        if (parsePosition2.getIndex() == index) {
                            parsePosition.setErrorIndex(index);
                            return;
                        }
                        index = parsePosition2.getIndex();
                    } else if (argType == MessagePattern.ArgType.NONE || (this.cachedFormatters != null && this.cachedFormatters.containsKey(Integer.valueOf(i4 - 2)))) {
                        String literalStringUntilNextArgument = getLiteralStringUntilNextArgument(limitPartIndex);
                        if (literalStringUntilNextArgument.length() != 0) {
                            length = str.indexOf(literalStringUntilNextArgument, index);
                        } else {
                            length = str.length();
                        }
                        if (length < 0) {
                            parsePosition.setErrorIndex(index);
                            return;
                        }
                        String strSubstring = str.substring(index, length);
                        if (strSubstring.equals("{" + string.toString() + "}")) {
                            z = false;
                            strSubstring = null;
                        } else {
                            z = true;
                        }
                        index = length;
                        objValueOf = strSubstring;
                        if (z) {
                            if (objArr != null) {
                                objArr[value] = objValueOf;
                            } else if (map != 0) {
                                map.put(obj, objValueOf);
                            }
                        }
                        limit = this.msgPattern.getPart(limitPartIndex).getLimit();
                        i2 = limitPartIndex;
                    } else {
                        if (argType != MessagePattern.ArgType.CHOICE) {
                            if (argType.hasPluralStyle() || argType == MessagePattern.ArgType.SELECT) {
                                throw new UnsupportedOperationException("Parsing of plural/select/selectordinal argument is not supported.");
                            }
                            throw new IllegalStateException("unexpected argType " + argType);
                        }
                        parsePosition2.setIndex(index);
                        double choiceArgument = parseChoiceArgument(this.msgPattern, i4, str, parsePosition2);
                        if (parsePosition2.getIndex() == index) {
                            parsePosition.setErrorIndex(index);
                            return;
                        } else {
                            objValueOf = Double.valueOf(choiceArgument);
                            index = parsePosition2.getIndex();
                        }
                    }
                    z = true;
                    if (z) {
                    }
                    limit = this.msgPattern.getPart(limitPartIndex).getLimit();
                    i2 = limitPartIndex;
                }
                i2++;
            } else {
                parsePosition.setErrorIndex(index);
                return;
            }
        }
    }

    public Map<String, Object> parseToMap(String str) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        HashMap map = new HashMap();
        parse(0, str, parsePosition, null, map);
        if (parsePosition.getIndex() == 0) {
            throw new ParseException("MessageFormat parse error!", parsePosition.getErrorIndex());
        }
        return map;
    }

    @Override
    public Object parseObject(String str, ParsePosition parsePosition) {
        if (!this.msgPattern.hasNamedArguments()) {
            return parse(str, parsePosition);
        }
        return parseToMap(str, parsePosition);
    }

    @Override
    public Object clone() {
        MessageFormat messageFormat = (MessageFormat) super.clone();
        if (this.customFormatArgStarts != null) {
            messageFormat.customFormatArgStarts = new HashSet();
            Iterator<Integer> it = this.customFormatArgStarts.iterator();
            while (it.hasNext()) {
                messageFormat.customFormatArgStarts.add(it.next());
            }
        } else {
            messageFormat.customFormatArgStarts = null;
        }
        if (this.cachedFormatters != null) {
            messageFormat.cachedFormatters = new HashMap();
            for (Map.Entry<Integer, Format> entry : this.cachedFormatters.entrySet()) {
                messageFormat.cachedFormatters.put(entry.getKey(), entry.getValue());
            }
        } else {
            messageFormat.cachedFormatters = null;
        }
        messageFormat.msgPattern = this.msgPattern == null ? null : (MessagePattern) this.msgPattern.clone();
        messageFormat.stockDateFormatter = this.stockDateFormatter == null ? null : (DateFormat) this.stockDateFormatter.clone();
        messageFormat.stockNumberFormatter = this.stockNumberFormatter == null ? null : (NumberFormat) this.stockNumberFormatter.clone();
        messageFormat.pluralProvider = null;
        messageFormat.ordinalProvider = null;
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
        if (Utility.objectEquals(this.ulocale, messageFormat.ulocale) && Utility.objectEquals(this.msgPattern, messageFormat.msgPattern) && Utility.objectEquals(this.cachedFormatters, messageFormat.cachedFormatters) && Utility.objectEquals(this.customFormatArgStarts, messageFormat.customFormatArgStarts)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return this.msgPattern.getPatternString().hashCode();
    }

    public static class Field extends Format.Field {
        public static final Field ARGUMENT = new Field("message argument field");
        private static final long serialVersionUID = 7510380454602616157L;

        protected Field(String str) {
            super(str);
        }

        @Override
        protected Object readResolve() throws InvalidObjectException {
            if (getClass() != Field.class) {
                throw new InvalidObjectException("A subclass of MessageFormat.Field must implement readResolve.");
            }
            if (getName().equals(ARGUMENT.getName())) {
                return ARGUMENT;
            }
            throw new InvalidObjectException("Unknown attribute name.");
        }
    }

    private DateFormat getStockDateFormatter() {
        if (this.stockDateFormatter == null) {
            this.stockDateFormatter = DateFormat.getDateTimeInstance(3, 3, this.ulocale);
        }
        return this.stockDateFormatter;
    }

    private NumberFormat getStockNumberFormatter() {
        if (this.stockNumberFormatter == null) {
            this.stockNumberFormatter = NumberFormat.getInstance(this.ulocale);
        }
        return this.stockNumberFormatter;
    }

    private void format(int i, PluralSelectorContext pluralSelectorContext, Object[] objArr, Map<String, Object> map, AppendableWrapper appendableWrapper, FieldPosition fieldPosition) {
        Object obj;
        int i2;
        Object obj2;
        int i3;
        FieldPosition fieldPosition2;
        CharSequence charSequence;
        AppendableWrapper appendableWrapper2;
        PluralSelectorProvider pluralSelectorProvider;
        Format format;
        Object objValueOf;
        Map<String, Object> map2 = map;
        AppendableWrapper appendableWrapper3 = appendableWrapper;
        CharSequence patternString = this.msgPattern.getPatternString();
        int limit = this.msgPattern.getPart(i).getLimit();
        int i4 = i + 1;
        FieldPosition fieldPosition3 = fieldPosition;
        while (true) {
            MessagePattern.Part part = this.msgPattern.getPart(i4);
            MessagePattern.Part.Type type = part.getType();
            appendableWrapper3.append(patternString, limit, part.getIndex());
            if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                return;
            }
            limit = part.getLimit();
            if (type == MessagePattern.Part.Type.REPLACE_NUMBER) {
                if (pluralSelectorContext.forReplaceNumber) {
                    appendableWrapper3.formatAndAppend(pluralSelectorContext.formatter, pluralSelectorContext.number, pluralSelectorContext.numberString);
                } else {
                    appendableWrapper3.formatAndAppend(getStockNumberFormatter(), pluralSelectorContext.number);
                }
            } else {
                if (type == MessagePattern.Part.Type.ARG_START) {
                    int limitPartIndex = this.msgPattern.getLimitPartIndex(i4);
                    MessagePattern.ArgType argType = part.getArgType();
                    int i5 = i4 + 1;
                    MessagePattern.Part part2 = this.msgPattern.getPart(i5);
                    boolean z = false;
                    String substring = this.msgPattern.getSubstring(part2);
                    Object obj3 = null;
                    if (objArr != null) {
                        int value = part2.getValue();
                        if (appendableWrapper.attributes != null) {
                            objValueOf = Integer.valueOf(value);
                        } else {
                            objValueOf = null;
                        }
                        if (value >= 0 && value < objArr.length) {
                            obj3 = objArr[value];
                        } else {
                            z = true;
                        }
                        obj = objValueOf;
                    } else if (map2 != null && map2.containsKey(substring)) {
                        obj3 = map2.get(substring);
                        obj = substring;
                    } else {
                        obj = substring;
                        z = true;
                    }
                    int i6 = i5 + 1;
                    int i7 = appendableWrapper.length;
                    if (z) {
                        appendableWrapper3.append("{" + substring + "}");
                    } else if (obj3 == null) {
                        appendableWrapper3.append("null");
                    } else if (pluralSelectorContext == null || pluralSelectorContext.numberArgIndex != i6 - 2) {
                        if (this.cachedFormatters != null && (format = this.cachedFormatters.get(Integer.valueOf(i6 - 2))) != null) {
                            if ((format instanceof ChoiceFormat) || (format instanceof PluralFormat) || (format instanceof SelectFormat)) {
                                String str = format.format(obj3);
                                if (str.indexOf(123) >= 0 || (str.indexOf(39) >= 0 && !this.msgPattern.jdkAposMode())) {
                                    i2 = i7;
                                    obj2 = obj;
                                    new MessageFormat(str, this.ulocale).format(0, null, objArr, map2, appendableWrapper3, null);
                                } else {
                                    if (appendableWrapper.attributes == null) {
                                        appendableWrapper3.append(str);
                                    } else {
                                        appendableWrapper3.formatAndAppend(format, obj3);
                                    }
                                    i2 = i7;
                                    obj2 = obj;
                                }
                                i3 = limitPartIndex;
                                fieldPosition2 = fieldPosition3;
                                charSequence = patternString;
                                appendableWrapper2 = appendableWrapper3;
                                FieldPosition fieldPositionUpdateMetaData = updateMetaData(appendableWrapper2, i2, fieldPosition2, obj2);
                                limit = this.msgPattern.getPart(i3).getLimit();
                                fieldPosition3 = fieldPositionUpdateMetaData;
                            } else {
                                appendableWrapper3.formatAndAppend(format, obj3);
                            }
                        } else {
                            i2 = i7;
                            obj2 = obj;
                            if (argType == MessagePattern.ArgType.NONE || (this.cachedFormatters != null && this.cachedFormatters.containsKey(Integer.valueOf(i6 - 2)))) {
                                i3 = limitPartIndex;
                                fieldPosition2 = fieldPosition3;
                                charSequence = patternString;
                                appendableWrapper2 = appendableWrapper3;
                                if (obj3 instanceof Number) {
                                    appendableWrapper2.formatAndAppend(getStockNumberFormatter(), obj3);
                                } else if (obj3 instanceof Date) {
                                    appendableWrapper2.formatAndAppend(getStockDateFormatter(), obj3);
                                } else {
                                    appendableWrapper2.append(obj3.toString());
                                }
                            } else if (argType == MessagePattern.ArgType.CHOICE) {
                                if (!(obj3 instanceof Number)) {
                                    throw new IllegalArgumentException("'" + obj3 + "' is not a Number");
                                }
                                i3 = limitPartIndex;
                                fieldPosition2 = fieldPosition3;
                                charSequence = patternString;
                                appendableWrapper2 = appendableWrapper3;
                                formatComplexSubMessage(findChoiceSubMessage(this.msgPattern, i6, ((Number) obj3).doubleValue()), null, objArr, map2, appendableWrapper2);
                            } else {
                                i3 = limitPartIndex;
                                fieldPosition2 = fieldPosition3;
                                charSequence = patternString;
                                appendableWrapper2 = appendableWrapper3;
                                if (argType.hasPluralStyle()) {
                                    if (!(obj3 instanceof Number)) {
                                        throw new IllegalArgumentException("'" + obj3 + "' is not a Number");
                                    }
                                    if (argType == MessagePattern.ArgType.PLURAL) {
                                        if (this.pluralProvider == null) {
                                            this.pluralProvider = new PluralSelectorProvider(this, PluralRules.PluralType.CARDINAL);
                                        }
                                        pluralSelectorProvider = this.pluralProvider;
                                    } else {
                                        if (this.ordinalProvider == null) {
                                            this.ordinalProvider = new PluralSelectorProvider(this, PluralRules.PluralType.ORDINAL);
                                        }
                                        pluralSelectorProvider = this.ordinalProvider;
                                    }
                                    Number number = (Number) obj3;
                                    PluralSelectorContext pluralSelectorContext2 = new PluralSelectorContext(i6, substring, number, this.msgPattern.getPluralOffset(i6));
                                    formatComplexSubMessage(PluralFormat.findSubMessage(this.msgPattern, i6, pluralSelectorProvider, pluralSelectorContext2, number.doubleValue()), pluralSelectorContext2, objArr, map, appendableWrapper2);
                                } else if (argType == MessagePattern.ArgType.SELECT) {
                                    formatComplexSubMessage(SelectFormat.findSubMessage(this.msgPattern, i6, obj3.toString()), null, objArr, map, appendableWrapper2);
                                } else {
                                    throw new IllegalStateException("unexpected argType " + argType);
                                }
                            }
                            FieldPosition fieldPositionUpdateMetaData2 = updateMetaData(appendableWrapper2, i2, fieldPosition2, obj2);
                            limit = this.msgPattern.getPart(i3).getLimit();
                            fieldPosition3 = fieldPositionUpdateMetaData2;
                        }
                    } else if (pluralSelectorContext.offset == 0.0d) {
                        appendableWrapper3.formatAndAppend(pluralSelectorContext.formatter, pluralSelectorContext.number, pluralSelectorContext.numberString);
                    } else {
                        appendableWrapper3.formatAndAppend(pluralSelectorContext.formatter, obj3);
                    }
                    i3 = limitPartIndex;
                    charSequence = patternString;
                    i2 = i7;
                    obj2 = obj;
                    fieldPosition2 = fieldPosition3;
                    appendableWrapper2 = appendableWrapper3;
                    FieldPosition fieldPositionUpdateMetaData22 = updateMetaData(appendableWrapper2, i2, fieldPosition2, obj2);
                    limit = this.msgPattern.getPart(i3).getLimit();
                    fieldPosition3 = fieldPositionUpdateMetaData22;
                }
                i4 = i3 + 1;
                map2 = map;
                patternString = charSequence;
                appendableWrapper3 = appendableWrapper2;
            }
            i3 = i4;
            charSequence = patternString;
            appendableWrapper2 = appendableWrapper3;
            i4 = i3 + 1;
            map2 = map;
            patternString = charSequence;
            appendableWrapper3 = appendableWrapper2;
        }
    }

    private void formatComplexSubMessage(int i, PluralSelectorContext pluralSelectorContext, Object[] objArr, Map<String, Object> map, AppendableWrapper appendableWrapper) {
        int index;
        String string;
        if (!this.msgPattern.jdkAposMode()) {
            format(i, pluralSelectorContext, objArr, map, appendableWrapper, null);
            return;
        }
        String patternString = this.msgPattern.getPatternString();
        StringBuilder sb = null;
        int limit = this.msgPattern.getPart(i).getLimit();
        while (true) {
            i++;
            MessagePattern.Part part = this.msgPattern.getPart(i);
            MessagePattern.Part.Type type = part.getType();
            index = part.getIndex();
            if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                break;
            }
            if (type == MessagePattern.Part.Type.REPLACE_NUMBER || type == MessagePattern.Part.Type.SKIP_SYNTAX) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append((CharSequence) patternString, limit, index);
                if (type == MessagePattern.Part.Type.REPLACE_NUMBER) {
                    if (pluralSelectorContext.forReplaceNumber) {
                        sb.append(pluralSelectorContext.numberString);
                    } else {
                        sb.append(getStockNumberFormatter().format(pluralSelectorContext.number));
                    }
                }
                limit = part.getLimit();
            } else if (type == MessagePattern.Part.Type.ARG_START) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append((CharSequence) patternString, limit, index);
                i = this.msgPattern.getLimitPartIndex(i);
                limit = this.msgPattern.getPart(i).getLimit();
                MessagePattern.appendReducedApostrophes(patternString, index, limit, sb);
            }
        }
        if (sb == null) {
            string = patternString.substring(limit, index);
        } else {
            sb.append((CharSequence) patternString, limit, index);
            string = sb.toString();
        }
        if (string.indexOf(123) >= 0) {
            MessageFormat messageFormat = new MessageFormat("", this.ulocale);
            messageFormat.applyPattern(string, MessagePattern.ApostropheMode.DOUBLE_REQUIRED);
            messageFormat.format(0, null, objArr, map, appendableWrapper, null);
            return;
        }
        appendableWrapper.append(string);
    }

    private String getLiteralStringUntilNextArgument(int i) {
        StringBuilder sb = new StringBuilder();
        String patternString = this.msgPattern.getPatternString();
        int limit = this.msgPattern.getPart(i).getLimit();
        while (true) {
            i++;
            MessagePattern.Part part = this.msgPattern.getPart(i);
            MessagePattern.Part.Type type = part.getType();
            sb.append((CharSequence) patternString, limit, part.getIndex());
            if (type == MessagePattern.Part.Type.ARG_START || type == MessagePattern.Part.Type.MSG_LIMIT) {
                break;
            }
            limit = part.getLimit();
        }
        return sb.toString();
    }

    private FieldPosition updateMetaData(AppendableWrapper appendableWrapper, int i, FieldPosition fieldPosition, Object obj) {
        if (appendableWrapper.attributes != null && i < appendableWrapper.length) {
            appendableWrapper.attributes.add(new AttributeAndPosition(obj, i, appendableWrapper.length));
        }
        if (fieldPosition != null && Field.ARGUMENT.equals(fieldPosition.getFieldAttribute())) {
            fieldPosition.setBeginIndex(i);
            fieldPosition.setEndIndex(appendableWrapper.length);
            return null;
        }
        return fieldPosition;
    }

    private static int findChoiceSubMessage(MessagePattern messagePattern, int i, double d) {
        int iCountParts = messagePattern.countParts();
        int i2 = i + 2;
        while (true) {
            int limitPartIndex = messagePattern.getLimitPartIndex(i2) + 1;
            if (limitPartIndex >= iCountParts) {
                break;
            }
            int i3 = limitPartIndex + 1;
            MessagePattern.Part part = messagePattern.getPart(limitPartIndex);
            if (part.getType() == MessagePattern.Part.Type.ARG_LIMIT) {
                break;
            }
            double numericValue = messagePattern.getNumericValue(part);
            int i4 = i3 + 1;
            if (messagePattern.getPatternString().charAt(messagePattern.getPatternIndex(i3)) == '<') {
                if (d <= numericValue) {
                    break;
                }
                i2 = i4;
            } else {
                if (d < numericValue) {
                    break;
                }
                i2 = i4;
            }
        }
        return i2;
    }

    private static double parseChoiceArgument(MessagePattern messagePattern, int i, String str, ParsePosition parsePosition) {
        int i2;
        int index = parsePosition.getIndex();
        double d = Double.NaN;
        int i3 = index;
        while (true) {
            if (messagePattern.getPartType(i) != MessagePattern.Part.Type.ARG_LIMIT) {
                double numericValue = messagePattern.getNumericValue(messagePattern.getPart(i));
                int i4 = i + 2;
                int limitPartIndex = messagePattern.getLimitPartIndex(i4);
                int iMatchStringUntilLimitPart = matchStringUntilLimitPart(messagePattern, i4, limitPartIndex, str, index);
                if (iMatchStringUntilLimitPart >= 0 && (i2 = iMatchStringUntilLimitPart + index) > i3) {
                    if (i2 != str.length()) {
                        i3 = i2;
                        d = numericValue;
                    } else {
                        d = numericValue;
                        break;
                    }
                }
                i = limitPartIndex + 1;
            } else {
                i2 = i3;
                break;
            }
        }
        if (i2 == index) {
            parsePosition.setErrorIndex(index);
        } else {
            parsePosition.setIndex(i2);
        }
        return d;
    }

    private static int matchStringUntilLimitPart(MessagePattern messagePattern, int i, int i2, String str, int i3) {
        String patternString = messagePattern.getPatternString();
        int limit = messagePattern.getPart(i).getLimit();
        int i4 = 0;
        while (true) {
            i++;
            MessagePattern.Part part = messagePattern.getPart(i);
            if (i == i2 || part.getType() == MessagePattern.Part.Type.SKIP_SYNTAX) {
                int index = part.getIndex() - limit;
                if (index != 0 && !str.regionMatches(i3, patternString, limit, index)) {
                    return -1;
                }
                i4 += index;
                if (i == i2) {
                    return i4;
                }
                limit = part.getLimit();
            }
        }
    }

    private int findOtherSubMessage(int i) {
        int iCountParts = this.msgPattern.countParts();
        if (this.msgPattern.getPart(i).getType().hasNumericValue()) {
            i++;
        }
        do {
            int i2 = i + 1;
            MessagePattern.Part part = this.msgPattern.getPart(i);
            if (part.getType() != MessagePattern.Part.Type.ARG_LIMIT) {
                if (this.msgPattern.partSubstringMatches(part, PluralRules.KEYWORD_OTHER)) {
                    return i2;
                }
                if (this.msgPattern.getPartType(i2).hasNumericValue()) {
                    i2++;
                }
                i = this.msgPattern.getLimitPartIndex(i2) + 1;
            } else {
                return 0;
            }
        } while (i < iCountParts);
        return 0;
    }

    private int findFirstPluralNumberArg(int i, String str) {
        while (true) {
            i++;
            MessagePattern.Part part = this.msgPattern.getPart(i);
            MessagePattern.Part.Type type = part.getType();
            if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                return 0;
            }
            if (type == MessagePattern.Part.Type.REPLACE_NUMBER) {
                return -1;
            }
            if (type == MessagePattern.Part.Type.ARG_START) {
                MessagePattern.ArgType argType = part.getArgType();
                if (str.length() != 0 && (argType == MessagePattern.ArgType.NONE || argType == MessagePattern.ArgType.SIMPLE)) {
                    if (this.msgPattern.partSubstringMatches(this.msgPattern.getPart(i + 1), str)) {
                        return i;
                    }
                }
                i = this.msgPattern.getLimitPartIndex(i);
            }
        }
    }

    private static final class PluralSelectorContext {
        String argName;
        boolean forReplaceNumber;
        Format formatter;
        Number number;
        int numberArgIndex;
        String numberString;
        double offset;
        int startIndex;

        private PluralSelectorContext(int i, String str, Number number, double d) {
            this.startIndex = i;
            this.argName = str;
            if (d == 0.0d) {
                this.number = number;
            } else {
                this.number = Double.valueOf(number.doubleValue() - d);
            }
            this.offset = d;
        }

        public String toString() {
            throw new AssertionError("PluralSelectorContext being formatted, rather than its number");
        }
    }

    private static final class PluralSelectorProvider implements PluralFormat.PluralSelector {
        static final boolean $assertionsDisabled = false;
        private MessageFormat msgFormat;
        private PluralRules rules;
        private PluralRules.PluralType type;

        public PluralSelectorProvider(MessageFormat messageFormat, PluralRules.PluralType pluralType) {
            this.msgFormat = messageFormat;
            this.type = pluralType;
        }

        @Override
        public String select(Object obj, double d) {
            if (this.rules == null) {
                this.rules = PluralRules.forLocale(this.msgFormat.ulocale, this.type);
            }
            PluralSelectorContext pluralSelectorContext = (PluralSelectorContext) obj;
            pluralSelectorContext.numberArgIndex = this.msgFormat.findFirstPluralNumberArg(this.msgFormat.findOtherSubMessage(pluralSelectorContext.startIndex), pluralSelectorContext.argName);
            if (pluralSelectorContext.numberArgIndex > 0 && this.msgFormat.cachedFormatters != null) {
                pluralSelectorContext.formatter = (Format) this.msgFormat.cachedFormatters.get(Integer.valueOf(pluralSelectorContext.numberArgIndex));
            }
            if (pluralSelectorContext.formatter == null) {
                pluralSelectorContext.formatter = this.msgFormat.getStockNumberFormatter();
                pluralSelectorContext.forReplaceNumber = true;
            }
            pluralSelectorContext.numberString = pluralSelectorContext.formatter.format(pluralSelectorContext.number);
            if (pluralSelectorContext.formatter instanceof DecimalFormat) {
                return this.rules.select(((DecimalFormat) pluralSelectorContext.formatter).getFixedDecimal(d));
            }
            return this.rules.select(d);
        }
    }

    private void format(Object obj, AppendableWrapper appendableWrapper, FieldPosition fieldPosition) {
        if (obj == null || (obj instanceof Map)) {
            format(null, (Map) obj, appendableWrapper, fieldPosition);
        } else {
            format((Object[]) obj, null, appendableWrapper, fieldPosition);
        }
    }

    private void format(Object[] objArr, Map<String, Object> map, AppendableWrapper appendableWrapper, FieldPosition fieldPosition) {
        if (objArr != null && this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
        }
        format(0, null, objArr, map, appendableWrapper, fieldPosition);
    }

    private void resetPattern() {
        if (this.msgPattern != null) {
            this.msgPattern.clear();
        }
        if (this.cachedFormatters != null) {
            this.cachedFormatters.clear();
        }
        this.customFormatArgStarts = null;
    }

    private Format createAppropriateFormat(String str, String str2) {
        switch (findKeyword(str, typeList)) {
            case 0:
                switch (findKeyword(str2, modifierList)) {
                    case 0:
                        return NumberFormat.getInstance(this.ulocale);
                    case 1:
                        return NumberFormat.getCurrencyInstance(this.ulocale);
                    case 2:
                        return NumberFormat.getPercentInstance(this.ulocale);
                    case 3:
                        return NumberFormat.getIntegerInstance(this.ulocale);
                    default:
                        return new DecimalFormat(str2, new DecimalFormatSymbols(this.ulocale));
                }
            case 1:
                switch (findKeyword(str2, dateModifierList)) {
                    case 0:
                        return DateFormat.getDateInstance(2, this.ulocale);
                    case 1:
                        return DateFormat.getDateInstance(3, this.ulocale);
                    case 2:
                        return DateFormat.getDateInstance(2, this.ulocale);
                    case 3:
                        return DateFormat.getDateInstance(1, this.ulocale);
                    case 4:
                        return DateFormat.getDateInstance(0, this.ulocale);
                    default:
                        return new SimpleDateFormat(str2, this.ulocale);
                }
            case 2:
                switch (findKeyword(str2, dateModifierList)) {
                    case 0:
                        return DateFormat.getTimeInstance(2, this.ulocale);
                    case 1:
                        return DateFormat.getTimeInstance(3, this.ulocale);
                    case 2:
                        return DateFormat.getTimeInstance(2, this.ulocale);
                    case 3:
                        return DateFormat.getTimeInstance(1, this.ulocale);
                    case 4:
                        return DateFormat.getTimeInstance(0, this.ulocale);
                    default:
                        return new SimpleDateFormat(str2, this.ulocale);
                }
            case 3:
                RuleBasedNumberFormat ruleBasedNumberFormat = new RuleBasedNumberFormat(this.ulocale, 1);
                String strTrim = str2.trim();
                if (strTrim.length() != 0) {
                    try {
                        ruleBasedNumberFormat.setDefaultRuleSet(strTrim);
                        return ruleBasedNumberFormat;
                    } catch (Exception e) {
                        return ruleBasedNumberFormat;
                    }
                }
                return ruleBasedNumberFormat;
            case 4:
                RuleBasedNumberFormat ruleBasedNumberFormat2 = new RuleBasedNumberFormat(this.ulocale, 2);
                String strTrim2 = str2.trim();
                if (strTrim2.length() != 0) {
                    try {
                        ruleBasedNumberFormat2.setDefaultRuleSet(strTrim2);
                        return ruleBasedNumberFormat2;
                    } catch (Exception e2) {
                        return ruleBasedNumberFormat2;
                    }
                }
                return ruleBasedNumberFormat2;
            case 5:
                RuleBasedNumberFormat ruleBasedNumberFormat3 = new RuleBasedNumberFormat(this.ulocale, 3);
                String strTrim3 = str2.trim();
                if (strTrim3.length() != 0) {
                    try {
                        ruleBasedNumberFormat3.setDefaultRuleSet(strTrim3);
                        return ruleBasedNumberFormat3;
                    } catch (Exception e3) {
                        return ruleBasedNumberFormat3;
                    }
                }
                return ruleBasedNumberFormat3;
            default:
                throw new IllegalArgumentException("Unknown format type \"" + str + "\"");
        }
    }

    private static final int findKeyword(String str, String[] strArr) {
        String lowerCase = PatternProps.trimWhiteSpace(str).toLowerCase(rootLocale);
        for (int i = 0; i < strArr.length; i++) {
            if (lowerCase.equals(strArr[i])) {
                return i;
            }
        }
        return -1;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(this.ulocale.toLanguageTag());
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern();
        }
        objectOutputStream.writeObject(this.msgPattern.getApostropheMode());
        objectOutputStream.writeObject(this.msgPattern.getPatternString());
        if (this.customFormatArgStarts == null || this.customFormatArgStarts.isEmpty()) {
            objectOutputStream.writeInt(0);
        } else {
            objectOutputStream.writeInt(this.customFormatArgStarts.size());
            int iNextTopLevelArgStart = 0;
            int i = 0;
            while (true) {
                iNextTopLevelArgStart = nextTopLevelArgStart(iNextTopLevelArgStart);
                if (iNextTopLevelArgStart < 0) {
                    break;
                }
                if (this.customFormatArgStarts.contains(Integer.valueOf(iNextTopLevelArgStart))) {
                    objectOutputStream.writeInt(i);
                    objectOutputStream.writeObject(this.cachedFormatters.get(Integer.valueOf(iNextTopLevelArgStart)));
                }
                i++;
            }
        }
        objectOutputStream.writeInt(0);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.ulocale = ULocale.forLanguageTag((String) objectInputStream.readObject());
        MessagePattern.ApostropheMode apostropheMode = (MessagePattern.ApostropheMode) objectInputStream.readObject();
        if (this.msgPattern == null || apostropheMode != this.msgPattern.getApostropheMode()) {
            this.msgPattern = new MessagePattern(apostropheMode);
        }
        String str = (String) objectInputStream.readObject();
        if (str != null) {
            applyPattern(str);
        }
        for (int i = objectInputStream.readInt(); i > 0; i--) {
            setFormat(objectInputStream.readInt(), (Format) objectInputStream.readObject());
        }
        for (int i2 = objectInputStream.readInt(); i2 > 0; i2--) {
            objectInputStream.readInt();
            objectInputStream.readObject();
        }
    }

    private void cacheExplicitFormats() {
        if (this.cachedFormatters != null) {
            this.cachedFormatters.clear();
        }
        this.customFormatArgStarts = null;
        int iCountParts = this.msgPattern.countParts() - 2;
        int i = 1;
        while (i < iCountParts) {
            MessagePattern.Part part = this.msgPattern.getPart(i);
            if (part.getType() == MessagePattern.Part.Type.ARG_START && part.getArgType() == MessagePattern.ArgType.SIMPLE) {
                int i2 = i + 2;
                int i3 = i2 + 1;
                String substring = this.msgPattern.getSubstring(this.msgPattern.getPart(i2));
                String substring2 = "";
                MessagePattern.Part part2 = this.msgPattern.getPart(i3);
                if (part2.getType() == MessagePattern.Part.Type.ARG_STYLE) {
                    substring2 = this.msgPattern.getSubstring(part2);
                    i3++;
                }
                setArgStartFormat(i, createAppropriateFormat(substring, substring2));
                i = i3;
            }
            i++;
        }
    }

    private void setArgStartFormat(int i, Format format) {
        if (this.cachedFormatters == null) {
            this.cachedFormatters = new HashMap();
        }
        this.cachedFormatters.put(Integer.valueOf(i), format);
    }

    private void setCustomArgStartFormat(int i, Format format) {
        setArgStartFormat(i, format);
        if (this.customFormatArgStarts == null) {
            this.customFormatArgStarts = new HashSet();
        }
        this.customFormatArgStarts.add(Integer.valueOf(i));
    }

    public static String autoQuoteApostrophe(String str) {
        StringBuilder sb = new StringBuilder(str.length() * 2);
        int length = str.length();
        char c = 0;
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            switch (c) {
                case 0:
                    if (cCharAt == '\'') {
                        c = 1;
                    } else if (cCharAt == '{') {
                        c = 3;
                        i++;
                    }
                    break;
                case 1:
                    if (cCharAt != '\'') {
                        if (cCharAt != '{' && cCharAt != '}') {
                            sb.append('\'');
                            c = 0;
                        } else {
                            c = 2;
                        }
                    } else {
                        c = 0;
                    }
                    break;
                case 2:
                    if (cCharAt == '\'') {
                        c = 0;
                    }
                    break;
                case 3:
                    if (cCharAt != '{') {
                        if (cCharAt == '}' && i - 1 == 0) {
                            c = 0;
                        }
                    } else {
                        i++;
                    }
                    break;
            }
            sb.append(cCharAt);
        }
        if (c == 1 || c == 2) {
            sb.append('\'');
        }
        return new String(sb);
    }

    private static final class AppendableWrapper {
        private Appendable app;
        private List<AttributeAndPosition> attributes = null;
        private int length;

        public AppendableWrapper(StringBuilder sb) {
            this.app = sb;
            this.length = sb.length();
        }

        public AppendableWrapper(StringBuffer stringBuffer) {
            this.app = stringBuffer;
            this.length = stringBuffer.length();
        }

        public void useAttributes() {
            this.attributes = new ArrayList();
        }

        public void append(CharSequence charSequence) {
            try {
                this.app.append(charSequence);
                this.length += charSequence.length();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void append(CharSequence charSequence, int i, int i2) {
            try {
                this.app.append(charSequence, i, i2);
                this.length += i2 - i;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void append(CharacterIterator characterIterator) {
            this.length += append(this.app, characterIterator);
        }

        public static int append(Appendable appendable, CharacterIterator characterIterator) {
            try {
                int beginIndex = characterIterator.getBeginIndex();
                int endIndex = characterIterator.getEndIndex();
                int i = endIndex - beginIndex;
                if (beginIndex < endIndex) {
                    appendable.append(characterIterator.first());
                    while (true) {
                        beginIndex++;
                        if (beginIndex >= endIndex) {
                            break;
                        }
                        appendable.append(characterIterator.next());
                    }
                }
                return i;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void formatAndAppend(Format format, Object obj) {
            if (this.attributes == null) {
                append(format.format(obj));
                return;
            }
            AttributedCharacterIterator toCharacterIterator = format.formatToCharacterIterator(obj);
            int i = this.length;
            append(toCharacterIterator);
            toCharacterIterator.first();
            int index = toCharacterIterator.getIndex();
            int endIndex = toCharacterIterator.getEndIndex();
            int i2 = i - index;
            while (index < endIndex) {
                Map<AttributedCharacterIterator.Attribute, Object> attributes = toCharacterIterator.getAttributes();
                int runLimit = toCharacterIterator.getRunLimit();
                if (attributes.size() != 0) {
                    for (Map.Entry<AttributedCharacterIterator.Attribute, Object> entry : attributes.entrySet()) {
                        this.attributes.add(new AttributeAndPosition(entry.getKey(), entry.getValue(), i2 + index, i2 + runLimit));
                    }
                }
                toCharacterIterator.setIndex(runLimit);
                index = runLimit;
            }
        }

        public void formatAndAppend(Format format, Object obj, String str) {
            if (this.attributes == null && str != null) {
                append(str);
            } else {
                formatAndAppend(format, obj);
            }
        }
    }

    private static final class AttributeAndPosition {
        private AttributedCharacterIterator.Attribute key;
        private int limit;
        private int start;
        private Object value;

        public AttributeAndPosition(Object obj, int i, int i2) {
            init(Field.ARGUMENT, obj, i, i2);
        }

        public AttributeAndPosition(AttributedCharacterIterator.Attribute attribute, Object obj, int i, int i2) {
            init(attribute, obj, i, i2);
        }

        public void init(AttributedCharacterIterator.Attribute attribute, Object obj, int i, int i2) {
            this.key = attribute;
            this.value = obj;
            this.start = i;
            this.limit = i2;
        }
    }
}
