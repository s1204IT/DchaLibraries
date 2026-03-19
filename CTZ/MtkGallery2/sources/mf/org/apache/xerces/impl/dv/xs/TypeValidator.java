package mf.org.apache.xerces.impl.dv.xs;

import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidationContext;

public abstract class TypeValidator {
    public static final short EQUAL = 0;
    public static final short GREATER_THAN = 1;
    public static final short INDETERMINATE = 2;
    public static final short LESS_THAN = -1;

    public abstract Object getActualValue(String str, ValidationContext validationContext) throws InvalidDatatypeValueException;

    public abstract short getAllowedFacets();

    public void checkExtraRules(Object value, ValidationContext context) throws InvalidDatatypeValueException {
    }

    public boolean isIdentical(Object value1, Object value2) {
        return value1.equals(value2);
    }

    public int compare(Object value1, Object value2) {
        return -1;
    }

    public int getDataLength(Object obj) {
        if (obj instanceof String) {
            return obj.length();
        }
        return -1;
    }

    public int getTotalDigits(Object value) {
        return -1;
    }

    public int getFractionDigits(Object value) {
        return -1;
    }

    public static final boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static final int getDigit(char ch) {
        if (isDigit(ch)) {
            return ch - '0';
        }
        return -1;
    }
}
