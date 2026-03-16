package java.text;

import dalvik.bytecode.Opcodes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ChoiceFormat extends NumberFormat {
    private static final long serialVersionUID = 1795184449645032964L;
    private String[] choiceFormats;
    private double[] choiceLimits;

    public ChoiceFormat(double[] limits, String[] formats) {
        setChoices(limits, formats);
    }

    public ChoiceFormat(String template) {
        applyPattern(template);
    }

    public void applyPattern(String template) {
        double next;
        double[] limits = new double[5];
        List<String> formats = new ArrayList<>();
        int length = template.length();
        int limitCount = 0;
        int index = 0;
        StringBuffer buffer = new StringBuffer();
        NumberFormat format = NumberFormat.getInstance(Locale.US);
        ParsePosition position = new ParsePosition(0);
        while (true) {
            int index2 = skipWhitespace(template, index);
            if (index2 >= length) {
                if (limitCount == limits.length) {
                    this.choiceLimits = limits;
                } else {
                    this.choiceLimits = new double[limitCount];
                    System.arraycopy(limits, 0, this.choiceLimits, 0, limitCount);
                }
                this.choiceFormats = new String[formats.size()];
                for (int i = 0; i < formats.size(); i++) {
                    this.choiceFormats[i] = formats.get(i);
                }
                return;
            }
            position.setIndex(index2);
            Number value = format.parse(template, position);
            int index3 = skipWhitespace(template, position.getIndex());
            if (position.getErrorIndex() == -1 && index3 < length) {
                int index4 = index3 + 1;
                char ch = template.charAt(index3);
                if (limitCount == limits.length) {
                    double[] newLimits = new double[limitCount * 2];
                    System.arraycopy(limits, 0, newLimits, 0, limitCount);
                    limits = newLimits;
                }
                switch (ch) {
                    case '#':
                    case 8804:
                        next = value.doubleValue();
                        break;
                    case Opcodes.OP_IF_GTZ:
                        next = nextDouble(value.doubleValue());
                        break;
                    default:
                        throw new IllegalArgumentException("Bad character '" + ch + "' in template: " + template);
                }
                if (limitCount > 0 && next <= limits[limitCount - 1]) {
                    throw new IllegalArgumentException("Bad template: " + template);
                }
                buffer.setLength(0);
                position.setIndex(index4);
                upTo(template, position, buffer, '|');
                index = position.getIndex();
                limits[limitCount] = next;
                formats.add(buffer.toString());
                limitCount++;
            }
        }
    }

    @Override
    public Object clone() {
        ChoiceFormat clone = (ChoiceFormat) super.clone();
        clone.choiceLimits = (double[]) this.choiceLimits.clone();
        clone.choiceFormats = (String[]) this.choiceFormats.clone();
        return clone;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ChoiceFormat)) {
            return false;
        }
        ChoiceFormat choice = (ChoiceFormat) object;
        return Arrays.equals(this.choiceLimits, choice.choiceLimits) && Arrays.equals(this.choiceFormats, choice.choiceFormats);
    }

    @Override
    public StringBuffer format(double value, StringBuffer buffer, FieldPosition field) {
        for (int i = this.choiceLimits.length - 1; i >= 0; i--) {
            if (this.choiceLimits[i] <= value) {
                return buffer.append(this.choiceFormats[i]);
            }
        }
        return this.choiceFormats.length != 0 ? buffer.append(this.choiceFormats[0]) : buffer;
    }

    @Override
    public StringBuffer format(long value, StringBuffer buffer, FieldPosition field) {
        return format(value, buffer, field);
    }

    public Object[] getFormats() {
        return this.choiceFormats;
    }

    public double[] getLimits() {
        return this.choiceLimits;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        for (int i = 0; i < this.choiceLimits.length; i++) {
            long v = Double.doubleToLongBits(this.choiceLimits[i]);
            hashCode += ((int) ((v >>> 32) ^ v)) + this.choiceFormats[i].hashCode();
        }
        return hashCode;
    }

    public static final double nextDouble(double value) {
        long bits;
        if (value == Double.POSITIVE_INFINITY) {
            return value;
        }
        if (value == 0.0d) {
            bits = 0;
        } else {
            bits = Double.doubleToLongBits(value);
        }
        return Double.longBitsToDouble(value < 0.0d ? bits - 1 : bits + 1);
    }

    public static double nextDouble(double value, boolean increment) {
        return increment ? nextDouble(value) : previousDouble(value);
    }

    @Override
    public Number parse(String string, ParsePosition position) {
        int offset = position.getIndex();
        for (int i = 0; i < this.choiceFormats.length; i++) {
            if (string.startsWith(this.choiceFormats[i], offset)) {
                position.setIndex(this.choiceFormats[i].length() + offset);
                return new Double(this.choiceLimits[i]);
            }
        }
        position.setErrorIndex(offset);
        return new Double(Double.NaN);
    }

    public static final double previousDouble(double value) {
        long bits;
        if (value == Double.NEGATIVE_INFINITY) {
            return value;
        }
        if (value == 0.0d) {
            bits = Long.MIN_VALUE;
        } else {
            bits = Double.doubleToLongBits(value);
        }
        return Double.longBitsToDouble(value <= 0.0d ? bits + 1 : bits - 1);
    }

    public void setChoices(double[] limits, String[] formats) {
        if (limits.length != formats.length) {
            throw new IllegalArgumentException("limits.length != formats.length: " + limits.length + " != " + formats.length);
        }
        this.choiceLimits = limits;
        this.choiceFormats = formats;
    }

    private int skipWhitespace(String string, int index) {
        int length = string.length();
        while (index < length && Character.isWhitespace(string.charAt(index))) {
            index++;
        }
        return index;
    }

    public String toPattern() {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < this.choiceLimits.length; i++) {
            if (i != 0) {
                buffer.append('|');
            }
            String previous = String.valueOf(previousDouble(this.choiceLimits[i]));
            String limit = String.valueOf(this.choiceLimits[i]);
            if (previous.length() < limit.length()) {
                buffer.append(previous);
                buffer.append('<');
            } else {
                buffer.append(limit);
                buffer.append('#');
            }
            boolean quote = this.choiceFormats[i].indexOf(Opcodes.OP_NOT_INT) != -1;
            if (quote) {
                buffer.append('\'');
            }
            buffer.append(this.choiceFormats[i]);
            if (quote) {
                buffer.append('\'');
            }
        }
        return buffer.toString();
    }
}
