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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MessageFormat extends UFormat {

    static final boolean f71assertionsDisabled;
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
    private static final String[] dateModifierList;
    private static final String[] modifierList;
    private static final Locale rootLocale;
    static final long serialVersionUID = 7136212545847378652L;
    private static final String[] typeList;
    private transient Map<Integer, Format> cachedFormatters;
    private transient Set<Integer> customFormatArgStarts;
    private transient MessagePattern msgPattern;
    private transient PluralSelectorProvider ordinalProvider;
    private transient PluralSelectorProvider pluralProvider;
    private transient DateFormat stockDateFormatter;
    private transient NumberFormat stockNumberFormatter;
    private transient ULocale ulocale;

    public MessageFormat(String pattern) {
        this.ulocale = ULocale.getDefault(ULocale.Category.FORMAT);
        applyPattern(pattern);
    }

    public MessageFormat(String pattern, Locale locale) {
        this(pattern, ULocale.forLocale(locale));
    }

    public MessageFormat(String pattern, ULocale locale) {
        this.ulocale = locale;
        applyPattern(pattern);
    }

    public void setLocale(Locale locale) {
        setLocale(ULocale.forLocale(locale));
    }

    public void setLocale(ULocale locale) {
        String existingPattern = toPattern();
        this.ulocale = locale;
        this.stockDateFormatter = null;
        this.stockNumberFormatter = null;
        this.pluralProvider = null;
        this.ordinalProvider = null;
        applyPattern(existingPattern);
    }

    public Locale getLocale() {
        return this.ulocale.toLocale();
    }

    public ULocale getULocale() {
        return this.ulocale;
    }

    public void applyPattern(String pttrn) {
        try {
            if (this.msgPattern == null) {
                this.msgPattern = new MessagePattern(pttrn);
            } else {
                this.msgPattern.parse(pttrn);
            }
            cacheExplicitFormats();
        } catch (RuntimeException e) {
            resetPattern();
            throw e;
        }
    }

    public void applyPattern(String pattern, MessagePattern.ApostropheMode aposMode) {
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern(aposMode);
        } else if (aposMode != this.msgPattern.getApostropheMode()) {
            this.msgPattern.clearPatternAndSetApostropheMode(aposMode);
        }
        applyPattern(pattern);
    }

    public MessagePattern.ApostropheMode getApostropheMode() {
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern();
        }
        return this.msgPattern.getApostropheMode();
    }

    public String toPattern() {
        String originalPattern;
        if (this.customFormatArgStarts != null) {
            throw new IllegalStateException("toPattern() is not supported after custom Format objects have been set via setFormat() or similar APIs");
        }
        return (this.msgPattern == null || (originalPattern = this.msgPattern.getPatternString()) == null) ? "" : originalPattern;
    }

    private int nextTopLevelArgStart(int partIndex) {
        MessagePattern.Part.Type type;
        if (partIndex != 0) {
            partIndex = this.msgPattern.getLimitPartIndex(partIndex);
        }
        do {
            partIndex++;
            type = this.msgPattern.getPartType(partIndex);
            if (type == MessagePattern.Part.Type.ARG_START) {
                return partIndex;
            }
        } while (type != MessagePattern.Part.Type.MSG_LIMIT);
        return -1;
    }

    private boolean argNameMatches(int partIndex, String argName, int argNumber) {
        MessagePattern.Part part = this.msgPattern.getPart(partIndex);
        if (part.getType() == MessagePattern.Part.Type.ARG_NAME) {
            return this.msgPattern.partSubstringMatches(part, argName);
        }
        return part.getValue() == argNumber;
    }

    private String getArgName(int partIndex) {
        MessagePattern.Part part = this.msgPattern.getPart(partIndex);
        if (part.getType() == MessagePattern.Part.Type.ARG_NAME) {
            return this.msgPattern.getSubstring(part);
        }
        return Integer.toString(part.getValue());
    }

    public void setFormatsByArgumentIndex(Format[] newFormats) {
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
        }
        int partIndex = 0;
        while (true) {
            partIndex = nextTopLevelArgStart(partIndex);
            if (partIndex < 0) {
                return;
            }
            int argNumber = this.msgPattern.getPart(partIndex + 1).getValue();
            if (argNumber < newFormats.length) {
                setCustomArgStartFormat(partIndex, newFormats[argNumber]);
            }
        }
    }

    public void setFormatsByArgumentName(Map<String, Format> newFormats) {
        int partIndex = 0;
        while (true) {
            partIndex = nextTopLevelArgStart(partIndex);
            if (partIndex < 0) {
                return;
            }
            String key = getArgName(partIndex + 1);
            if (newFormats.containsKey(key)) {
                setCustomArgStartFormat(partIndex, newFormats.get(key));
            }
        }
    }

    public void setFormats(Format[] newFormats) {
        int partIndex = 0;
        for (int formatNumber = 0; formatNumber < newFormats.length && (partIndex = nextTopLevelArgStart(partIndex)) >= 0; formatNumber++) {
            setCustomArgStartFormat(partIndex, newFormats[formatNumber]);
        }
    }

    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
        }
        int partIndex = 0;
        while (true) {
            partIndex = nextTopLevelArgStart(partIndex);
            if (partIndex < 0) {
                return;
            }
            if (this.msgPattern.getPart(partIndex + 1).getValue() == argumentIndex) {
                setCustomArgStartFormat(partIndex, newFormat);
            }
        }
    }

    public void setFormatByArgumentName(String argumentName, Format newFormat) {
        int argNumber = MessagePattern.validateArgumentName(argumentName);
        if (argNumber < -1) {
            return;
        }
        int partIndex = 0;
        while (true) {
            partIndex = nextTopLevelArgStart(partIndex);
            if (partIndex < 0) {
                return;
            }
            if (argNameMatches(partIndex + 1, argumentName, argNumber)) {
                setCustomArgStartFormat(partIndex, newFormat);
            }
        }
    }

    public void setFormat(int formatElementIndex, Format newFormat) {
        int formatNumber = 0;
        int partIndex = 0;
        while (true) {
            partIndex = nextTopLevelArgStart(partIndex);
            if (partIndex >= 0) {
                if (formatNumber == formatElementIndex) {
                    setCustomArgStartFormat(partIndex, newFormat);
                    return;
                }
                formatNumber++;
            } else {
                throw new ArrayIndexOutOfBoundsException(formatElementIndex);
            }
        }
    }

    public Format[] getFormatsByArgumentIndex() {
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
        }
        ArrayList<Format> list = new ArrayList<>();
        int partIndex = 0;
        while (true) {
            partIndex = nextTopLevelArgStart(partIndex);
            if (partIndex >= 0) {
                int argNumber = this.msgPattern.getPart(partIndex + 1).getValue();
                while (argNumber >= list.size()) {
                    list.add(null);
                }
                list.set(argNumber, this.cachedFormatters == null ? null : this.cachedFormatters.get(Integer.valueOf(partIndex)));
            } else {
                return (Format[]) list.toArray(new Format[list.size()]);
            }
        }
    }

    public Format[] getFormats() {
        ArrayList<Format> list = new ArrayList<>();
        int partIndex = 0;
        while (true) {
            partIndex = nextTopLevelArgStart(partIndex);
            if (partIndex >= 0) {
                list.add(this.cachedFormatters == null ? null : this.cachedFormatters.get(Integer.valueOf(partIndex)));
            } else {
                return (Format[]) list.toArray(new Format[list.size()]);
            }
        }
    }

    public Set<String> getArgumentNames() {
        Set<String> result = new HashSet<>();
        int partIndex = 0;
        while (true) {
            partIndex = nextTopLevelArgStart(partIndex);
            if (partIndex >= 0) {
                result.add(getArgName(partIndex + 1));
            } else {
                return result;
            }
        }
    }

    public Format getFormatByArgumentName(String argumentName) {
        int argNumber;
        if (this.cachedFormatters == null || (argNumber = MessagePattern.validateArgumentName(argumentName)) < -1) {
            return null;
        }
        int partIndex = 0;
        do {
            partIndex = nextTopLevelArgStart(partIndex);
            if (partIndex < 0) {
                return null;
            }
        } while (!argNameMatches(partIndex + 1, argumentName, argNumber));
        return this.cachedFormatters.get(Integer.valueOf(partIndex));
    }

    public final StringBuffer format(Object[] arguments, StringBuffer result, FieldPosition pos) {
        format(arguments, null, new AppendableWrapper(result), pos);
        return result;
    }

    public final StringBuffer format(Map<String, Object> arguments, StringBuffer result, FieldPosition pos) {
        format(null, arguments, new AppendableWrapper(result), pos);
        return result;
    }

    public static String format(String pattern, Object... arguments) {
        MessageFormat temp = new MessageFormat(pattern);
        return temp.format(arguments);
    }

    public static String format(String pattern, Map<String, Object> arguments) {
        MessageFormat temp = new MessageFormat(pattern);
        return temp.format(arguments);
    }

    public boolean usesNamedArguments() {
        return this.msgPattern.hasNamedArguments();
    }

    @Override
    public final StringBuffer format(Object arguments, StringBuffer result, FieldPosition pos) {
        format(arguments, new AppendableWrapper(result), pos);
        return result;
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object arguments) {
        if (arguments == null) {
            throw new NullPointerException("formatToCharacterIterator must be passed non-null object");
        }
        StringBuilder result = new StringBuilder();
        AppendableWrapper wrapper = new AppendableWrapper(result);
        wrapper.useAttributes();
        format(arguments, wrapper, (FieldPosition) null);
        AttributedString as = new AttributedString(result.toString());
        for (AttributeAndPosition a : wrapper.attributes) {
            as.addAttribute(a.key, a.value, a.start, a.limit);
        }
        return as.getIterator();
    }

    public Object[] parse(String source, ParsePosition pos) {
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use named argument.");
        }
        int maxArgId = -1;
        int partIndex = 0;
        while (true) {
            partIndex = nextTopLevelArgStart(partIndex);
            if (partIndex < 0) {
                break;
            }
            int argNumber = this.msgPattern.getPart(partIndex + 1).getValue();
            if (argNumber > maxArgId) {
                maxArgId = argNumber;
            }
        }
        Object[] resultArray = new Object[maxArgId + 1];
        int backupStartPos = pos.getIndex();
        parse(0, source, pos, resultArray, null);
        if (pos.getIndex() == backupStartPos) {
            return null;
        }
        return resultArray;
    }

    public Map<String, Object> parseToMap(String source, ParsePosition pos) {
        Map<String, Object> result = new HashMap<>();
        int backupStartPos = pos.getIndex();
        parse(0, source, pos, null, result);
        if (pos.getIndex() == backupStartPos) {
            return null;
        }
        return result;
    }

    public Object[] parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object[] result = parse(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("MessageFormat parse error!", pos.getErrorIndex());
        }
        return result;
    }

    private void parse(int msgStart, String source, ParsePosition pos, Object[] args, Map<String, Object> argsMap) {
        Object argId;
        int next;
        Format formatter;
        if (source == null) {
            return;
        }
        String msgString = this.msgPattern.getPatternString();
        int prevIndex = this.msgPattern.getPart(msgStart).getLimit();
        int sourceOffset = pos.getIndex();
        ParsePosition tempStatus = new ParsePosition(0);
        int i = msgStart + 1;
        while (true) {
            MessagePattern.Part part = this.msgPattern.getPart(i);
            MessagePattern.Part.Type type = part.getType();
            int index = part.getIndex();
            int len = index - prevIndex;
            if (len == 0 || msgString.regionMatches(prevIndex, source, sourceOffset, len)) {
                sourceOffset += len;
                int i2 = prevIndex + len;
                if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                    pos.setIndex(sourceOffset);
                    return;
                }
                if (type == MessagePattern.Part.Type.SKIP_SYNTAX || type == MessagePattern.Part.Type.INSERT_CHAR) {
                    prevIndex = part.getLimit();
                } else {
                    if (!f71assertionsDisabled) {
                        if (!(type == MessagePattern.Part.Type.ARG_START)) {
                            throw new AssertionError("Unexpected Part " + part + " in parsed message.");
                        }
                    }
                    int argLimit = this.msgPattern.getLimitPartIndex(i);
                    MessagePattern.ArgType argType = part.getArgType();
                    int i3 = i + 1;
                    MessagePattern.Part part2 = this.msgPattern.getPart(i3);
                    int argNumber = 0;
                    String key = null;
                    if (args != null) {
                        argNumber = part2.getValue();
                        argId = Integer.valueOf(argNumber);
                    } else {
                        if (part2.getType() == MessagePattern.Part.Type.ARG_NAME) {
                            key = this.msgPattern.getSubstring(part2);
                        } else {
                            key = Integer.toString(part2.getValue());
                        }
                        argId = key;
                    }
                    int i4 = i3 + 1;
                    boolean haveArgResult = false;
                    Object argResult = null;
                    if (this.cachedFormatters != null && (formatter = this.cachedFormatters.get(Integer.valueOf(i4 - 2))) != null) {
                        tempStatus.setIndex(sourceOffset);
                        argResult = formatter.parseObject(source, tempStatus);
                        if (tempStatus.getIndex() == sourceOffset) {
                            pos.setErrorIndex(sourceOffset);
                            return;
                        } else {
                            haveArgResult = true;
                            sourceOffset = tempStatus.getIndex();
                        }
                    } else if (argType == MessagePattern.ArgType.NONE || (this.cachedFormatters != null && this.cachedFormatters.containsKey(Integer.valueOf(i4 - 2)))) {
                        String stringAfterArgument = getLiteralStringUntilNextArgument(argLimit);
                        if (stringAfterArgument.length() != 0) {
                            next = source.indexOf(stringAfterArgument, sourceOffset);
                        } else {
                            next = source.length();
                        }
                        if (next < 0) {
                            pos.setErrorIndex(sourceOffset);
                            return;
                        }
                        String strValue = source.substring(sourceOffset, next);
                        if (!strValue.equals("{" + argId.toString() + "}")) {
                            haveArgResult = true;
                            argResult = strValue;
                        }
                        sourceOffset = next;
                    } else if (argType == MessagePattern.ArgType.CHOICE) {
                        tempStatus.setIndex(sourceOffset);
                        double choiceResult = parseChoiceArgument(this.msgPattern, i4, source, tempStatus);
                        if (tempStatus.getIndex() == sourceOffset) {
                            pos.setErrorIndex(sourceOffset);
                            return;
                        } else {
                            argResult = Double.valueOf(choiceResult);
                            haveArgResult = true;
                            sourceOffset = tempStatus.getIndex();
                        }
                    } else {
                        if (argType.hasPluralStyle() || argType == MessagePattern.ArgType.SELECT) {
                            throw new UnsupportedOperationException("Parsing of plural/select/selectordinal argument is not supported.");
                        }
                        throw new IllegalStateException("unexpected argType " + argType);
                    }
                    if (haveArgResult) {
                        if (args != null) {
                            args[argNumber] = argResult;
                        } else if (argsMap != null) {
                            argsMap.put(key, argResult);
                        }
                    }
                    prevIndex = this.msgPattern.getPart(argLimit).getLimit();
                    i = argLimit;
                }
                i++;
            } else {
                pos.setErrorIndex(sourceOffset);
                return;
            }
        }
    }

    public Map<String, Object> parseToMap(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Map<String, Object> result = new HashMap<>();
        parse(0, source, pos, null, result);
        if (pos.getIndex() == 0) {
            throw new ParseException("MessageFormat parse error!", pos.getErrorIndex());
        }
        return result;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        if (!this.msgPattern.hasNamedArguments()) {
            return parse(source, pos);
        }
        return parseToMap(source, pos);
    }

    @Override
    public Object clone() {
        MessageFormat other = (MessageFormat) super.clone();
        if (this.customFormatArgStarts != null) {
            other.customFormatArgStarts = new HashSet();
            for (Integer key : this.customFormatArgStarts) {
                other.customFormatArgStarts.add(key);
            }
        } else {
            other.customFormatArgStarts = null;
        }
        if (this.cachedFormatters != null) {
            other.cachedFormatters = new HashMap();
            for (Map.Entry<Integer, Format> entry : this.cachedFormatters.entrySet()) {
                other.cachedFormatters.put(entry.getKey(), entry.getValue());
            }
        } else {
            other.cachedFormatters = null;
        }
        other.msgPattern = this.msgPattern == null ? null : (MessagePattern) this.msgPattern.clone();
        other.stockDateFormatter = this.stockDateFormatter == null ? null : (DateFormat) this.stockDateFormatter.clone();
        other.stockNumberFormatter = this.stockNumberFormatter == null ? null : (NumberFormat) this.stockNumberFormatter.clone();
        other.pluralProvider = null;
        other.ordinalProvider = null;
        return other;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MessageFormat other = (MessageFormat) obj;
        if (Utility.objectEquals(this.ulocale, other.ulocale) && Utility.objectEquals(this.msgPattern, other.msgPattern) && Utility.objectEquals(this.cachedFormatters, other.cachedFormatters)) {
            return Utility.objectEquals(this.customFormatArgStarts, other.customFormatArgStarts);
        }
        return false;
    }

    public int hashCode() {
        return this.msgPattern.getPatternString().hashCode();
    }

    public static class Field extends Format.Field {
        public static final Field ARGUMENT = new Field("message argument field");
        private static final long serialVersionUID = 7510380454602616157L;

        protected Field(String name) {
            super(name);
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

    private void format(int msgStart, PluralSelectorContext pluralNumber, Object[] args, Map<String, Object> argsMap, AppendableWrapper dest, FieldPosition fp) {
        Object arg;
        PluralSelectorProvider selector;
        Format formatter;
        String msgString = this.msgPattern.getPatternString();
        int prevIndex = this.msgPattern.getPart(msgStart).getLimit();
        int i = msgStart + 1;
        while (true) {
            MessagePattern.Part part = this.msgPattern.getPart(i);
            MessagePattern.Part.Type type = part.getType();
            int index = part.getIndex();
            dest.append(msgString, prevIndex, index);
            if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                return;
            }
            prevIndex = part.getLimit();
            if (type == MessagePattern.Part.Type.REPLACE_NUMBER) {
                if (pluralNumber.forReplaceNumber) {
                    dest.formatAndAppend(pluralNumber.formatter, pluralNumber.number, pluralNumber.numberString);
                } else {
                    dest.formatAndAppend(getStockNumberFormatter(), pluralNumber.number);
                }
            } else if (type == MessagePattern.Part.Type.ARG_START) {
                int argLimit = this.msgPattern.getLimitPartIndex(i);
                MessagePattern.ArgType argType = part.getArgType();
                int i2 = i + 1;
                MessagePattern.Part part2 = this.msgPattern.getPart(i2);
                boolean noArg = false;
                Object argId = null;
                String argName = this.msgPattern.getSubstring(part2);
                if (args != null) {
                    int argNumber = part2.getValue();
                    if (dest.attributes != null) {
                        argId = Integer.valueOf(argNumber);
                    }
                    if (argNumber >= 0 && argNumber < args.length) {
                        arg = args[argNumber];
                    } else {
                        arg = null;
                        noArg = true;
                    }
                } else {
                    argId = argName;
                    if (argsMap != null && argsMap.containsKey(argName)) {
                        arg = argsMap.get(argName);
                    } else {
                        arg = null;
                        noArg = true;
                    }
                }
                int i3 = i2 + 1;
                int prevDestLength = dest.length;
                if (noArg) {
                    dest.append("{" + argName + "}");
                } else if (arg == null) {
                    dest.append("null");
                } else if (pluralNumber == null || pluralNumber.numberArgIndex != i3 - 2) {
                    if (this.cachedFormatters != null && (formatter = this.cachedFormatters.get(Integer.valueOf(i3 - 2))) != null) {
                        if ((formatter instanceof ChoiceFormat) || (formatter instanceof PluralFormat) || (formatter instanceof SelectFormat)) {
                            String subMsgString = formatter.format(arg);
                            if (subMsgString.indexOf(123) >= 0 || (subMsgString.indexOf(39) >= 0 && !this.msgPattern.jdkAposMode())) {
                                MessageFormat subMsgFormat = new MessageFormat(subMsgString, this.ulocale);
                                subMsgFormat.format(0, null, args, argsMap, dest, null);
                            } else if (dest.attributes == null) {
                                dest.append(subMsgString);
                            } else {
                                dest.formatAndAppend(formatter, arg);
                            }
                        } else {
                            dest.formatAndAppend(formatter, arg);
                        }
                    } else if (argType == MessagePattern.ArgType.NONE || (this.cachedFormatters != null && this.cachedFormatters.containsKey(Integer.valueOf(i3 - 2)))) {
                        if (arg instanceof Number) {
                            dest.formatAndAppend(getStockNumberFormatter(), arg);
                        } else if (arg instanceof Date) {
                            dest.formatAndAppend(getStockDateFormatter(), arg);
                        } else {
                            dest.append(arg.toString());
                        }
                    } else if (argType == MessagePattern.ArgType.CHOICE) {
                        if (!(arg instanceof Number)) {
                            throw new IllegalArgumentException("'" + arg + "' is not a Number");
                        }
                        int subMsgStart = findChoiceSubMessage(this.msgPattern, i3, ((Number) arg).doubleValue());
                        formatComplexSubMessage(subMsgStart, null, args, argsMap, dest);
                    } else if (argType.hasPluralStyle()) {
                        if (!(arg instanceof Number)) {
                            throw new IllegalArgumentException("'" + arg + "' is not a Number");
                        }
                        if (argType == MessagePattern.ArgType.PLURAL) {
                            if (this.pluralProvider == null) {
                                this.pluralProvider = new PluralSelectorProvider(this, PluralRules.PluralType.CARDINAL);
                            }
                            selector = this.pluralProvider;
                        } else {
                            if (this.ordinalProvider == null) {
                                this.ordinalProvider = new PluralSelectorProvider(this, PluralRules.PluralType.ORDINAL);
                            }
                            selector = this.ordinalProvider;
                        }
                        Number number = (Number) arg;
                        double offset = this.msgPattern.getPluralOffset(i3);
                        PluralSelectorContext context = new PluralSelectorContext(i3, argName, number, offset, null);
                        int subMsgStart2 = PluralFormat.findSubMessage(this.msgPattern, i3, selector, context, number.doubleValue());
                        formatComplexSubMessage(subMsgStart2, context, args, argsMap, dest);
                    } else if (argType == MessagePattern.ArgType.SELECT) {
                        int subMsgStart3 = SelectFormat.findSubMessage(this.msgPattern, i3, arg.toString());
                        formatComplexSubMessage(subMsgStart3, null, args, argsMap, dest);
                    } else {
                        throw new IllegalStateException("unexpected argType " + argType);
                    }
                } else if (pluralNumber.offset == 0.0d) {
                    dest.formatAndAppend(pluralNumber.formatter, pluralNumber.number, pluralNumber.numberString);
                } else {
                    dest.formatAndAppend(pluralNumber.formatter, arg);
                }
                fp = updateMetaData(dest, prevDestLength, fp, argId);
                prevIndex = this.msgPattern.getPart(argLimit).getLimit();
                i = argLimit;
            } else {
                continue;
            }
            i++;
        }
    }

    private void formatComplexSubMessage(int msgStart, PluralSelectorContext pluralNumber, Object[] args, Map<String, Object> argsMap, AppendableWrapper dest) {
        int index;
        String subMsgString;
        if (!this.msgPattern.jdkAposMode()) {
            format(msgStart, pluralNumber, args, argsMap, dest, null);
            return;
        }
        String msgString = this.msgPattern.getPatternString();
        StringBuilder sb = null;
        int prevIndex = this.msgPattern.getPart(msgStart).getLimit();
        int i = msgStart;
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
                sb.append((CharSequence) msgString, prevIndex, index);
                if (type == MessagePattern.Part.Type.REPLACE_NUMBER) {
                    if (pluralNumber.forReplaceNumber) {
                        sb.append(pluralNumber.numberString);
                    } else {
                        sb.append(getStockNumberFormatter().format(pluralNumber.number));
                    }
                }
                prevIndex = part.getLimit();
            } else if (type == MessagePattern.Part.Type.ARG_START) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append((CharSequence) msgString, prevIndex, index);
                i = this.msgPattern.getLimitPartIndex(i);
                int index2 = this.msgPattern.getPart(i).getLimit();
                MessagePattern.appendReducedApostrophes(msgString, index, index2, sb);
                prevIndex = index2;
            }
        }
        if (sb == null) {
            subMsgString = msgString.substring(prevIndex, index);
        } else {
            subMsgString = sb.append((CharSequence) msgString, prevIndex, index).toString();
        }
        if (subMsgString.indexOf(123) >= 0) {
            MessageFormat subMsgFormat = new MessageFormat("", this.ulocale);
            subMsgFormat.applyPattern(subMsgString, MessagePattern.ApostropheMode.DOUBLE_REQUIRED);
            subMsgFormat.format(0, null, args, argsMap, dest, null);
            return;
        }
        dest.append(subMsgString);
    }

    private String getLiteralStringUntilNextArgument(int from) {
        StringBuilder b = new StringBuilder();
        String msgString = this.msgPattern.getPatternString();
        int prevIndex = this.msgPattern.getPart(from).getLimit();
        int i = from + 1;
        while (true) {
            MessagePattern.Part part = this.msgPattern.getPart(i);
            MessagePattern.Part.Type type = part.getType();
            int index = part.getIndex();
            b.append((CharSequence) msgString, prevIndex, index);
            if (type == MessagePattern.Part.Type.ARG_START || type == MessagePattern.Part.Type.MSG_LIMIT) {
                break;
            }
            if (!f71assertionsDisabled) {
                if (!(type == MessagePattern.Part.Type.SKIP_SYNTAX || type == MessagePattern.Part.Type.INSERT_CHAR)) {
                    throw new AssertionError("Unexpected Part " + part + " in parsed message.");
                }
            }
            prevIndex = part.getLimit();
            i++;
        }
    }

    private FieldPosition updateMetaData(AppendableWrapper dest, int prevLength, FieldPosition fp, Object argId) {
        if (dest.attributes != null && prevLength < dest.length) {
            dest.attributes.add(new AttributeAndPosition(argId, prevLength, dest.length));
        }
        if (fp != null && Field.ARGUMENT.equals(fp.getFieldAttribute())) {
            fp.setBeginIndex(prevLength);
            fp.setEndIndex(dest.length);
            return null;
        }
        return fp;
    }

    private static int findChoiceSubMessage(MessagePattern pattern, int partIndex, double number) {
        int msgStart;
        int count = pattern.countParts();
        int partIndex2 = partIndex + 2;
        while (true) {
            msgStart = partIndex2;
            int partIndex3 = pattern.getLimitPartIndex(partIndex2) + 1;
            if (partIndex3 < count) {
                int partIndex4 = partIndex3 + 1;
                MessagePattern.Part part = pattern.getPart(partIndex3);
                MessagePattern.Part.Type type = part.getType();
                if (type == MessagePattern.Part.Type.ARG_LIMIT) {
                    break;
                }
                if (!f71assertionsDisabled && !type.hasNumericValue()) {
                    throw new AssertionError();
                }
                double boundary = pattern.getNumericValue(part);
                partIndex2 = partIndex4 + 1;
                int selectorIndex = pattern.getPatternIndex(partIndex4);
                char boundaryChar = pattern.getPatternString().charAt(selectorIndex);
                if (boundaryChar != '<') {
                    if (number < boundary) {
                        break;
                    }
                } else if (number <= boundary) {
                    break;
                }
            } else {
                break;
            }
        }
        return msgStart;
    }

    private static double parseChoiceArgument(MessagePattern pattern, int partIndex, String source, ParsePosition pos) {
        int newIndex;
        int start = pos.getIndex();
        int furthest = start;
        double bestNumber = Double.NaN;
        while (pattern.getPartType(partIndex) != MessagePattern.Part.Type.ARG_LIMIT) {
            double tempNumber = pattern.getNumericValue(pattern.getPart(partIndex));
            int partIndex2 = partIndex + 2;
            int msgLimit = pattern.getLimitPartIndex(partIndex2);
            int len = matchStringUntilLimitPart(pattern, partIndex2, msgLimit, source, start);
            if (len >= 0 && (newIndex = start + len) > furthest) {
                furthest = newIndex;
                bestNumber = tempNumber;
                if (newIndex == source.length()) {
                    break;
                }
            }
            partIndex = msgLimit + 1;
        }
        if (furthest == start) {
            pos.setErrorIndex(start);
        } else {
            pos.setIndex(furthest);
        }
        return bestNumber;
    }

    private static int matchStringUntilLimitPart(MessagePattern pattern, int partIndex, int limitPartIndex, String source, int sourceOffset) {
        int matchingSourceLength = 0;
        String msgString = pattern.getPatternString();
        int prevIndex = pattern.getPart(partIndex).getLimit();
        while (true) {
            partIndex++;
            MessagePattern.Part part = pattern.getPart(partIndex);
            if (partIndex == limitPartIndex || part.getType() == MessagePattern.Part.Type.SKIP_SYNTAX) {
                int index = part.getIndex();
                int length = index - prevIndex;
                if (length != 0 && !source.regionMatches(sourceOffset, msgString, prevIndex, length)) {
                    return -1;
                }
                matchingSourceLength += length;
                if (partIndex == limitPartIndex) {
                    return matchingSourceLength;
                }
                prevIndex = part.getLimit();
            }
        }
    }

    private int findOtherSubMessage(int partIndex) {
        int count = this.msgPattern.countParts();
        if (this.msgPattern.getPart(partIndex).getType().hasNumericValue()) {
            partIndex++;
        }
        do {
            int partIndex2 = partIndex + 1;
            MessagePattern.Part part = this.msgPattern.getPart(partIndex);
            MessagePattern.Part.Type type = part.getType();
            if (type == MessagePattern.Part.Type.ARG_LIMIT) {
                break;
            }
            if (!f71assertionsDisabled) {
                if (!(type == MessagePattern.Part.Type.ARG_SELECTOR)) {
                    throw new AssertionError();
                }
            }
            if (this.msgPattern.partSubstringMatches(part, PluralRules.KEYWORD_OTHER)) {
                return partIndex2;
            }
            partIndex = this.msgPattern.getLimitPartIndex(this.msgPattern.getPartType(partIndex2).hasNumericValue() ? partIndex2 + 1 : partIndex2) + 1;
        } while (partIndex < count);
        return 0;
    }

    private int findFirstPluralNumberArg(int msgStart, String argName) {
        int i = msgStart + 1;
        while (true) {
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
                if (argName.length() != 0 && (argType == MessagePattern.ArgType.NONE || argType == MessagePattern.ArgType.SIMPLE)) {
                    if (this.msgPattern.partSubstringMatches(this.msgPattern.getPart(i + 1), argName)) {
                        return i;
                    }
                }
                i = this.msgPattern.getLimitPartIndex(i);
            }
            i++;
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

        PluralSelectorContext(int start, String name, Number num, double off, PluralSelectorContext pluralSelectorContext) {
            this(start, name, num, off);
        }

        private PluralSelectorContext(int start, String name, Number num, double off) {
            this.startIndex = start;
            this.argName = name;
            if (off == 0.0d) {
                this.number = num;
            } else {
                this.number = Double.valueOf(num.doubleValue() - off);
            }
            this.offset = off;
        }

        public String toString() {
            throw new AssertionError("PluralSelectorContext being formatted, rather than its number");
        }
    }

    private static final class PluralSelectorProvider implements PluralFormat.PluralSelector {

        static final boolean f72assertionsDisabled;
        private MessageFormat msgFormat;
        private PluralRules rules;
        private PluralRules.PluralType type;

        static {
            f72assertionsDisabled = !PluralSelectorProvider.class.desiredAssertionStatus();
        }

        public PluralSelectorProvider(MessageFormat mf, PluralRules.PluralType type) {
            this.msgFormat = mf;
            this.type = type;
        }

        @Override
        public String select(Object ctx, double number) {
            if (this.rules == null) {
                this.rules = PluralRules.forLocale(this.msgFormat.ulocale, this.type);
            }
            PluralSelectorContext context = (PluralSelectorContext) ctx;
            int otherIndex = this.msgFormat.findOtherSubMessage(context.startIndex);
            context.numberArgIndex = this.msgFormat.findFirstPluralNumberArg(otherIndex, context.argName);
            if (context.numberArgIndex > 0 && this.msgFormat.cachedFormatters != null) {
                context.formatter = (Format) this.msgFormat.cachedFormatters.get(Integer.valueOf(context.numberArgIndex));
            }
            if (context.formatter == null) {
                context.formatter = this.msgFormat.getStockNumberFormatter();
                context.forReplaceNumber = true;
            }
            if (!f72assertionsDisabled) {
                if (!(context.number.doubleValue() == number)) {
                    throw new AssertionError();
                }
            }
            context.numberString = context.formatter.format(context.number);
            if (context.formatter instanceof DecimalFormat) {
                PluralRules.FixedDecimal dec = ((DecimalFormat) context.formatter).getFixedDecimal(number);
                return this.rules.select(dec);
            }
            return this.rules.select(number);
        }
    }

    private void format(Object arguments, AppendableWrapper result, FieldPosition fp) {
        if (arguments == null || (arguments instanceof Map)) {
            format(null, (Map) arguments, result, fp);
        } else {
            format((Object[]) arguments, null, result, fp);
        }
    }

    private void format(Object[] arguments, Map<String, Object> argsMap, AppendableWrapper dest, FieldPosition fp) {
        if (arguments != null && this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
        }
        format(0, null, arguments, argsMap, dest, fp);
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

    static {
        f71assertionsDisabled = !MessageFormat.class.desiredAssertionStatus();
        typeList = new String[]{"number", "date", "time", "spellout", "ordinal", "duration"};
        modifierList = new String[]{"", "currency", "percent", "integer"};
        dateModifierList = new String[]{"", "short", "medium", "long", "full"};
        rootLocale = new Locale("");
    }

    private Format createAppropriateFormat(String type, String style) {
        int subformatType = findKeyword(type, typeList);
        switch (subformatType) {
            case 0:
                switch (findKeyword(style, modifierList)) {
                    case 0:
                        Format newFormat = NumberFormat.getInstance(this.ulocale);
                        return newFormat;
                    case 1:
                        Format newFormat2 = NumberFormat.getCurrencyInstance(this.ulocale);
                        return newFormat2;
                    case 2:
                        Format newFormat3 = NumberFormat.getPercentInstance(this.ulocale);
                        return newFormat3;
                    case 3:
                        Format newFormat4 = NumberFormat.getIntegerInstance(this.ulocale);
                        return newFormat4;
                    default:
                        Format newFormat5 = new DecimalFormat(style, new DecimalFormatSymbols(this.ulocale));
                        return newFormat5;
                }
            case 1:
                switch (findKeyword(style, dateModifierList)) {
                    case 0:
                        Format newFormat6 = DateFormat.getDateInstance(2, this.ulocale);
                        return newFormat6;
                    case 1:
                        Format newFormat7 = DateFormat.getDateInstance(3, this.ulocale);
                        return newFormat7;
                    case 2:
                        Format newFormat8 = DateFormat.getDateInstance(2, this.ulocale);
                        return newFormat8;
                    case 3:
                        Format newFormat9 = DateFormat.getDateInstance(1, this.ulocale);
                        return newFormat9;
                    case 4:
                        Format newFormat10 = DateFormat.getDateInstance(0, this.ulocale);
                        return newFormat10;
                    default:
                        Format newFormat11 = new SimpleDateFormat(style, this.ulocale);
                        return newFormat11;
                }
            case 2:
                switch (findKeyword(style, dateModifierList)) {
                    case 0:
                        Format newFormat12 = DateFormat.getTimeInstance(2, this.ulocale);
                        return newFormat12;
                    case 1:
                        Format newFormat13 = DateFormat.getTimeInstance(3, this.ulocale);
                        return newFormat13;
                    case 2:
                        Format newFormat14 = DateFormat.getTimeInstance(2, this.ulocale);
                        return newFormat14;
                    case 3:
                        Format newFormat15 = DateFormat.getTimeInstance(1, this.ulocale);
                        return newFormat15;
                    case 4:
                        Format newFormat16 = DateFormat.getTimeInstance(0, this.ulocale);
                        return newFormat16;
                    default:
                        Format newFormat17 = new SimpleDateFormat(style, this.ulocale);
                        return newFormat17;
                }
            case 3:
                RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(this.ulocale, 1);
                String ruleset = style.trim();
                if (ruleset.length() != 0) {
                    try {
                        rbnf.setDefaultRuleSet(ruleset);
                        break;
                    } catch (Exception e) {
                    }
                }
                return rbnf;
            case 4:
                RuleBasedNumberFormat rbnf2 = new RuleBasedNumberFormat(this.ulocale, 2);
                String ruleset2 = style.trim();
                if (ruleset2.length() != 0) {
                    try {
                        rbnf2.setDefaultRuleSet(ruleset2);
                        break;
                    } catch (Exception e2) {
                    }
                }
                return rbnf2;
            case 5:
                RuleBasedNumberFormat rbnf3 = new RuleBasedNumberFormat(this.ulocale, 3);
                String ruleset3 = style.trim();
                if (ruleset3.length() != 0) {
                    try {
                        rbnf3.setDefaultRuleSet(ruleset3);
                        break;
                    } catch (Exception e3) {
                    }
                }
                return rbnf3;
            default:
                throw new IllegalArgumentException("Unknown format type \"" + type + "\"");
        }
    }

    private static final int findKeyword(String s, String[] list) {
        String s2 = PatternProps.trimWhiteSpace(s).toLowerCase(rootLocale);
        for (int i = 0; i < list.length; i++) {
            if (s2.equals(list[i])) {
                return i;
            }
        }
        return -1;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(this.ulocale.toLanguageTag());
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern();
        }
        out.writeObject(this.msgPattern.getApostropheMode());
        out.writeObject(this.msgPattern.getPatternString());
        if (this.customFormatArgStarts == null || this.customFormatArgStarts.isEmpty()) {
            out.writeInt(0);
        } else {
            out.writeInt(this.customFormatArgStarts.size());
            int formatIndex = 0;
            int partIndex = 0;
            while (true) {
                partIndex = nextTopLevelArgStart(partIndex);
                if (partIndex < 0) {
                    break;
                }
                if (this.customFormatArgStarts.contains(Integer.valueOf(partIndex))) {
                    out.writeInt(formatIndex);
                    out.writeObject(this.cachedFormatters.get(Integer.valueOf(partIndex)));
                }
                formatIndex++;
            }
        }
        out.writeInt(0);
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        String languageTag = (String) in.readObject();
        this.ulocale = ULocale.forLanguageTag(languageTag);
        MessagePattern.ApostropheMode aposMode = (MessagePattern.ApostropheMode) in.readObject();
        if (this.msgPattern == null || aposMode != this.msgPattern.getApostropheMode()) {
            this.msgPattern = new MessagePattern(aposMode);
        }
        String msg = (String) in.readObject();
        if (msg != null) {
            applyPattern(msg);
        }
        for (int numFormatters = in.readInt(); numFormatters > 0; numFormatters--) {
            int formatIndex = in.readInt();
            Format formatter = (Format) in.readObject();
            setFormat(formatIndex, formatter);
        }
        for (int numPairs = in.readInt(); numPairs > 0; numPairs--) {
            in.readInt();
            in.readObject();
        }
    }

    private void cacheExplicitFormats() {
        if (this.cachedFormatters != null) {
            this.cachedFormatters.clear();
        }
        this.customFormatArgStarts = null;
        int limit = this.msgPattern.countParts() - 2;
        int i = 1;
        while (i < limit) {
            MessagePattern.Part part = this.msgPattern.getPart(i);
            if (part.getType() == MessagePattern.Part.Type.ARG_START) {
                MessagePattern.ArgType argType = part.getArgType();
                if (argType == MessagePattern.ArgType.SIMPLE) {
                    int index = i;
                    int i2 = i + 2;
                    int i3 = i2 + 1;
                    String explicitType = this.msgPattern.getSubstring(this.msgPattern.getPart(i2));
                    String style = "";
                    MessagePattern.Part part2 = this.msgPattern.getPart(i3);
                    if (part2.getType() == MessagePattern.Part.Type.ARG_STYLE) {
                        style = this.msgPattern.getSubstring(part2);
                        i = i3 + 1;
                    } else {
                        i = i3;
                    }
                    Format formatter = createAppropriateFormat(explicitType, style);
                    setArgStartFormat(index, formatter);
                }
            }
            i++;
        }
    }

    private void setArgStartFormat(int argStart, Format formatter) {
        if (this.cachedFormatters == null) {
            this.cachedFormatters = new HashMap();
        }
        this.cachedFormatters.put(Integer.valueOf(argStart), formatter);
    }

    private void setCustomArgStartFormat(int argStart, Format formatter) {
        setArgStartFormat(argStart, formatter);
        if (this.customFormatArgStarts == null) {
            this.customFormatArgStarts = new HashSet();
        }
        this.customFormatArgStarts.add(Integer.valueOf(argStart));
    }

    public static String autoQuoteApostrophe(String pattern) {
        StringBuilder buf = new StringBuilder(pattern.length() * 2);
        int state = 0;
        int braceCount = 0;
        int j = pattern.length();
        for (int i = 0; i < j; i++) {
            char c = pattern.charAt(i);
            switch (state) {
                case 0:
                    switch (c) {
                        case '\'':
                            state = 1;
                            break;
                        case '{':
                            state = 3;
                            braceCount++;
                            break;
                    }
                    break;
                case 1:
                    switch (c) {
                        case '\'':
                            state = 0;
                            break;
                        case '{':
                        case '}':
                            state = 2;
                            break;
                        default:
                            buf.append('\'');
                            state = 0;
                            break;
                    }
                    break;
                case 2:
                    switch (c) {
                        case '\'':
                            state = 0;
                            break;
                    }
                    break;
                case 3:
                    switch (c) {
                        case '{':
                            braceCount++;
                            break;
                        case '}':
                            braceCount--;
                            if (braceCount == 0) {
                                state = 0;
                            }
                            break;
                    }
                    break;
            }
            buf.append(c);
        }
        if (state == 1 || state == 2) {
            buf.append('\'');
        }
        return new String(buf);
    }

    private static final class AppendableWrapper {
        private Appendable app;
        private List<AttributeAndPosition> attributes = null;
        private int length;

        public AppendableWrapper(StringBuilder sb) {
            this.app = sb;
            this.length = sb.length();
        }

        public AppendableWrapper(StringBuffer sb) {
            this.app = sb;
            this.length = sb.length();
        }

        public void useAttributes() {
            this.attributes = new ArrayList();
        }

        public void append(CharSequence s) {
            try {
                this.app.append(s);
                this.length += s.length();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void append(CharSequence s, int start, int limit) {
            try {
                this.app.append(s, start, limit);
                this.length += limit - start;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void append(CharacterIterator iterator) {
            this.length += append(this.app, iterator);
        }

        public static int append(Appendable result, CharacterIterator iterator) {
            try {
                int start = iterator.getBeginIndex();
                int limit = iterator.getEndIndex();
                int length = limit - start;
                if (start < limit) {
                    result.append(iterator.first());
                    while (true) {
                        start++;
                        if (start >= limit) {
                            break;
                        }
                        result.append(iterator.next());
                    }
                }
                return length;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void formatAndAppend(Format formatter, Object arg) {
            if (this.attributes == null) {
                append(formatter.format(arg));
                return;
            }
            AttributedCharacterIterator formattedArg = formatter.formatToCharacterIterator(arg);
            int prevLength = this.length;
            append(formattedArg);
            formattedArg.first();
            int start = formattedArg.getIndex();
            int limit = formattedArg.getEndIndex();
            int offset = prevLength - start;
            while (start < limit) {
                Map<AttributedCharacterIterator.Attribute, Object> map = formattedArg.getAttributes();
                int runLimit = formattedArg.getRunLimit();
                if (map.size() != 0) {
                    for (Map.Entry<AttributedCharacterIterator.Attribute, Object> entry : map.entrySet()) {
                        this.attributes.add(new AttributeAndPosition(entry.getKey(), entry.getValue(), offset + start, offset + runLimit));
                    }
                }
                start = runLimit;
                formattedArg.setIndex(runLimit);
            }
        }

        public void formatAndAppend(Format formatter, Object arg, String argString) {
            if (this.attributes == null && argString != null) {
                append(argString);
            } else {
                formatAndAppend(formatter, arg);
            }
        }
    }

    private static final class AttributeAndPosition {
        private AttributedCharacterIterator.Attribute key;
        private int limit;
        private int start;
        private Object value;

        public AttributeAndPosition(Object fieldValue, int startIndex, int limitIndex) {
            init(Field.ARGUMENT, fieldValue, startIndex, limitIndex);
        }

        public AttributeAndPosition(AttributedCharacterIterator.Attribute field, Object fieldValue, int startIndex, int limitIndex) {
            init(field, fieldValue, startIndex, limitIndex);
        }

        public void init(AttributedCharacterIterator.Attribute field, Object fieldValue, int startIndex, int limitIndex) {
            this.key = field;
            this.value = fieldValue;
            this.start = startIndex;
            this.limit = limitIndex;
        }
    }
}
