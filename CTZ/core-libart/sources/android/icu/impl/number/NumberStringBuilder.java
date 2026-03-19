package android.icu.impl.number;

import android.icu.impl.UCharacterProperty;
import android.icu.text.NumberFormat;
import android.icu.text.SymbolTable;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.Format;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NumberStringBuilder implements CharSequence {
    static final boolean $assertionsDisabled = false;
    public static final NumberStringBuilder EMPTY = new NumberStringBuilder();
    private static final Map<NumberFormat.Field, Character> fieldToDebugChar = new HashMap();
    private char[] chars;
    private NumberFormat.Field[] fields;
    private int length;
    private int zero;

    static {
        fieldToDebugChar.put(NumberFormat.Field.SIGN, '-');
        fieldToDebugChar.put(NumberFormat.Field.INTEGER, Character.valueOf(UCharacterProperty.LATIN_SMALL_LETTER_I_));
        fieldToDebugChar.put(NumberFormat.Field.FRACTION, 'f');
        fieldToDebugChar.put(NumberFormat.Field.EXPONENT, 'e');
        fieldToDebugChar.put(NumberFormat.Field.EXPONENT_SIGN, '+');
        fieldToDebugChar.put(NumberFormat.Field.EXPONENT_SYMBOL, 'E');
        fieldToDebugChar.put(NumberFormat.Field.DECIMAL_SEPARATOR, '.');
        fieldToDebugChar.put(NumberFormat.Field.GROUPING_SEPARATOR, ',');
        fieldToDebugChar.put(NumberFormat.Field.PERCENT, '%');
        fieldToDebugChar.put(NumberFormat.Field.PERMILLE, (char) 8240);
        fieldToDebugChar.put(NumberFormat.Field.CURRENCY, Character.valueOf(SymbolTable.SYMBOL_REF));
    }

    public NumberStringBuilder() {
        this(40);
    }

    public NumberStringBuilder(int i) {
        this.chars = new char[i];
        this.fields = new NumberFormat.Field[i];
        this.zero = i / 2;
        this.length = 0;
    }

    public NumberStringBuilder(NumberStringBuilder numberStringBuilder) {
        copyFrom(numberStringBuilder);
    }

    public void copyFrom(NumberStringBuilder numberStringBuilder) {
        this.chars = Arrays.copyOf(numberStringBuilder.chars, numberStringBuilder.chars.length);
        this.fields = (NumberFormat.Field[]) Arrays.copyOf(numberStringBuilder.fields, numberStringBuilder.fields.length);
        this.zero = numberStringBuilder.zero;
        this.length = numberStringBuilder.length;
    }

    @Override
    public int length() {
        return this.length;
    }

    public int codePointCount() {
        return Character.codePointCount(this, 0, length());
    }

    @Override
    public char charAt(int i) {
        return this.chars[this.zero + i];
    }

    public NumberFormat.Field fieldAt(int i) {
        return this.fields[this.zero + i];
    }

    public int getFirstCodePoint() {
        if (this.length == 0) {
            return -1;
        }
        return Character.codePointAt(this.chars, this.zero, this.zero + this.length);
    }

    public int getLastCodePoint() {
        if (this.length == 0) {
            return -1;
        }
        return Character.codePointBefore(this.chars, this.zero + this.length, this.zero);
    }

    public int codePointAt(int i) {
        return Character.codePointAt(this.chars, this.zero + i, this.zero + this.length);
    }

    public int codePointBefore(int i) {
        return Character.codePointBefore(this.chars, this.zero + i, this.zero);
    }

    public NumberStringBuilder clear() {
        this.zero = getCapacity() / 2;
        this.length = 0;
        return this;
    }

    public int appendCodePoint(int i, NumberFormat.Field field) {
        return insertCodePoint(this.length, i, field);
    }

    public int insertCodePoint(int i, int i2, NumberFormat.Field field) {
        int iCharCount = Character.charCount(i2);
        int iPrepareForInsert = prepareForInsert(i, iCharCount);
        Character.toChars(i2, this.chars, iPrepareForInsert);
        this.fields[iPrepareForInsert] = field;
        if (iCharCount == 2) {
            this.fields[iPrepareForInsert + 1] = field;
        }
        return iCharCount;
    }

    public int append(CharSequence charSequence, NumberFormat.Field field) {
        return insert(this.length, charSequence, field);
    }

    public int insert(int i, CharSequence charSequence, NumberFormat.Field field) {
        if (charSequence.length() == 0) {
            return 0;
        }
        if (charSequence.length() == 1) {
            return insertCodePoint(i, charSequence.charAt(0), field);
        }
        return insert(i, charSequence, 0, charSequence.length(), field);
    }

    public int insert(int i, CharSequence charSequence, int i2, int i3, NumberFormat.Field field) {
        int i4 = i3 - i2;
        int iPrepareForInsert = prepareForInsert(i, i4);
        for (int i5 = 0; i5 < i4; i5++) {
            int i6 = iPrepareForInsert + i5;
            this.chars[i6] = charSequence.charAt(i2 + i5);
            this.fields[i6] = field;
        }
        return i4;
    }

    public int append(char[] cArr, NumberFormat.Field[] fieldArr) {
        return insert(this.length, cArr, fieldArr);
    }

    public int insert(int i, char[] cArr, NumberFormat.Field[] fieldArr) {
        int length = cArr.length;
        if (length == 0) {
            return 0;
        }
        int iPrepareForInsert = prepareForInsert(i, length);
        for (int i2 = 0; i2 < length; i2++) {
            int i3 = iPrepareForInsert + i2;
            this.chars[i3] = cArr[i2];
            this.fields[i3] = fieldArr == null ? null : fieldArr[i2];
        }
        return length;
    }

    public int append(NumberStringBuilder numberStringBuilder) {
        return insert(this.length, numberStringBuilder);
    }

    public int insert(int i, NumberStringBuilder numberStringBuilder) {
        if (this == numberStringBuilder) {
            throw new IllegalArgumentException("Cannot call insert/append on myself");
        }
        int i2 = numberStringBuilder.length;
        if (i2 == 0) {
            return 0;
        }
        int iPrepareForInsert = prepareForInsert(i, i2);
        for (int i3 = 0; i3 < i2; i3++) {
            int i4 = iPrepareForInsert + i3;
            this.chars[i4] = numberStringBuilder.charAt(i3);
            this.fields[i4] = numberStringBuilder.fieldAt(i3);
        }
        return i2;
    }

    private int prepareForInsert(int i, int i2) {
        if (i == 0 && this.zero - i2 >= 0) {
            this.zero -= i2;
            this.length += i2;
            return this.zero;
        }
        if (i == this.length && this.zero + this.length + i2 < getCapacity()) {
            this.length += i2;
            return (this.zero + this.length) - i2;
        }
        return prepareForInsertHelper(i, i2);
    }

    private int prepareForInsertHelper(int i, int i2) {
        int capacity = getCapacity();
        int i3 = this.zero;
        char[] cArr = this.chars;
        NumberFormat.Field[] fieldArr = this.fields;
        if (this.length + i2 > capacity) {
            int i4 = (this.length + i2) * 2;
            int i5 = (i4 / 2) - ((this.length + i2) / 2);
            char[] cArr2 = new char[i4];
            NumberFormat.Field[] fieldArr2 = new NumberFormat.Field[i4];
            System.arraycopy(cArr, i3, cArr2, i5, i);
            int i6 = i3 + i;
            int i7 = i5 + i + i2;
            System.arraycopy(cArr, i6, cArr2, i7, this.length - i);
            System.arraycopy(fieldArr, i3, fieldArr2, i5, i);
            System.arraycopy(fieldArr, i6, fieldArr2, i7, this.length - i);
            this.chars = cArr2;
            this.fields = fieldArr2;
            this.zero = i5;
            this.length += i2;
        } else {
            int i8 = (capacity / 2) - ((this.length + i2) / 2);
            System.arraycopy(cArr, i3, cArr, i8, this.length);
            int i9 = i8 + i;
            int i10 = i9 + i2;
            System.arraycopy(cArr, i9, cArr, i10, this.length - i);
            System.arraycopy(fieldArr, i3, fieldArr, i8, this.length);
            System.arraycopy(fieldArr, i9, fieldArr, i10, this.length - i);
            this.zero = i8;
            this.length += i2;
        }
        return this.zero + i;
    }

    private int getCapacity() {
        return this.chars.length;
    }

    @Override
    public CharSequence subSequence(int i, int i2) {
        if (i < 0 || i2 > this.length || i2 < i) {
            throw new IndexOutOfBoundsException();
        }
        NumberStringBuilder numberStringBuilder = new NumberStringBuilder(this);
        numberStringBuilder.zero = this.zero + i;
        numberStringBuilder.length = i2 - i;
        return numberStringBuilder;
    }

    @Override
    public String toString() {
        return new String(this.chars, this.zero, this.length);
    }

    public String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<NumberStringBuilder [");
        sb.append(toString());
        sb.append("] [");
        for (int i = this.zero; i < this.zero + this.length; i++) {
            if (this.fields[i] == null) {
                sb.append('n');
            } else {
                sb.append(fieldToDebugChar.get(this.fields[i]));
            }
        }
        sb.append("]>");
        return sb.toString();
    }

    public char[] toCharArray() {
        return Arrays.copyOfRange(this.chars, this.zero, this.zero + this.length);
    }

    public NumberFormat.Field[] toFieldArray() {
        return (NumberFormat.Field[]) Arrays.copyOfRange(this.fields, this.zero, this.zero + this.length);
    }

    public boolean contentEquals(char[] cArr, NumberFormat.Field[] fieldArr) {
        if (cArr.length != this.length || fieldArr.length != this.length) {
            return false;
        }
        for (int i = 0; i < this.length; i++) {
            if (this.chars[this.zero + i] != cArr[i] || this.fields[this.zero + i] != fieldArr[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean contentEquals(NumberStringBuilder numberStringBuilder) {
        if (this.length != numberStringBuilder.length) {
            return false;
        }
        for (int i = 0; i < this.length; i++) {
            if (charAt(i) != numberStringBuilder.charAt(i) || fieldAt(i) != numberStringBuilder.fieldAt(i)) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        throw new UnsupportedOperationException("Don't call #hashCode() or #equals() on a mutable.");
    }

    public boolean equals(Object obj) {
        throw new UnsupportedOperationException("Don't call #hashCode() or #equals() on a mutable.");
    }

    public void populateFieldPosition(FieldPosition fieldPosition, int i) {
        Format.Field fieldAttribute = fieldPosition.getFieldAttribute();
        if (fieldAttribute == null) {
            if (fieldPosition.getField() != 0) {
                if (fieldPosition.getField() == 1) {
                    fieldAttribute = NumberFormat.Field.FRACTION;
                } else {
                    return;
                }
            } else {
                fieldAttribute = NumberFormat.Field.INTEGER;
            }
        }
        if (!(fieldAttribute instanceof NumberFormat.Field)) {
            throw new IllegalArgumentException("You must pass an instance of android.icu.text.NumberFormat.Field as your FieldPosition attribute.  You passed: " + fieldAttribute.getClass().toString());
        }
        NumberFormat.Field field = (NumberFormat.Field) fieldAttribute;
        boolean z = false;
        int i2 = -1;
        int i3 = this.zero;
        while (i3 <= this.zero + this.length) {
            NumberFormat.Field field2 = i3 < this.zero + this.length ? this.fields[i3] : null;
            if (z && field != field2) {
                if (field != NumberFormat.Field.INTEGER || field2 != NumberFormat.Field.GROUPING_SEPARATOR) {
                    fieldPosition.setEndIndex((i3 - this.zero) + i);
                    break;
                }
            } else {
                if (!z && field == field2) {
                    fieldPosition.setBeginIndex((i3 - this.zero) + i);
                    z = true;
                }
                if (field2 == NumberFormat.Field.INTEGER || field2 == NumberFormat.Field.DECIMAL_SEPARATOR) {
                    i2 = (i3 - this.zero) + 1;
                }
            }
            i3++;
        }
        if (field == NumberFormat.Field.FRACTION && !z) {
            int i4 = i2 + i;
            fieldPosition.setBeginIndex(i4);
            fieldPosition.setEndIndex(i4);
        }
    }

    public AttributedCharacterIterator getIterator() {
        AttributedString attributedString = new AttributedString(toString());
        NumberFormat.Field field = null;
        int i = -1;
        for (int i2 = 0; i2 < this.length; i2++) {
            NumberFormat.Field field2 = this.fields[this.zero + i2];
            if (field == NumberFormat.Field.INTEGER && field2 == NumberFormat.Field.GROUPING_SEPARATOR) {
                attributedString.addAttribute(NumberFormat.Field.GROUPING_SEPARATOR, NumberFormat.Field.GROUPING_SEPARATOR, i2, i2 + 1);
            } else if (field != field2) {
                if (field != null) {
                    attributedString.addAttribute(field, field, i, i2);
                }
                i = i2;
                field = field2;
            }
        }
        if (field != null) {
            attributedString.addAttribute(field, field, i, this.length);
        }
        return attributedString.getIterator();
    }
}
