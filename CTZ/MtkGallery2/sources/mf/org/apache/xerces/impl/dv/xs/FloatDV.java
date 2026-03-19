package mf.org.apache.xerces.impl.dv.xs;

import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.xs.datatypes.XSFloat;

public class FloatDV extends TypeValidator {
    @Override
    public short getAllowedFacets() {
        return (short) 2552;
    }

    @Override
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        try {
            return new XFloat(content);
        } catch (NumberFormatException e) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, SchemaSymbols.ATTVAL_FLOAT});
        }
    }

    @Override
    public int compare(Object value1, Object value2) {
        return ((XFloat) value1).compareTo((XFloat) value2);
    }

    @Override
    public boolean isIdentical(Object value1, Object obj) {
        if (obj instanceof XFloat) {
            return ((XFloat) value1).isIdentical(obj);
        }
        return false;
    }

    private static final class XFloat implements XSFloat {
        private String canonical;
        private final float value;

        public XFloat(String s) throws NumberFormatException {
            if (DoubleDV.isPossibleFP(s)) {
                this.value = Float.parseFloat(s);
                return;
            }
            if (s.equals("INF")) {
                this.value = Float.POSITIVE_INFINITY;
            } else if (s.equals("-INF")) {
                this.value = Float.NEGATIVE_INFINITY;
            } else {
                if (s.equals("NaN")) {
                    this.value = Float.NaN;
                    return;
                }
                throw new NumberFormatException(s);
            }
        }

        public boolean equals(Object val) {
            if (val == this) {
                return true;
            }
            if (!(val instanceof XFloat)) {
                return false;
            }
            XFloat oval = (XFloat) val;
            if (this.value == oval.value) {
                return true;
            }
            return (this.value == this.value || oval.value == oval.value) ? false : true;
        }

        public int hashCode() {
            if (this.value == 0.0f) {
                return 0;
            }
            return Float.floatToIntBits(this.value);
        }

        public boolean isIdentical(XFloat val) {
            if (val == this) {
                return true;
            }
            return this.value == val.value ? this.value != 0.0f || Float.floatToIntBits(this.value) == Float.floatToIntBits(val.value) : (this.value == this.value || val.value == val.value) ? false : true;
        }

        private int compareTo(XFloat val) {
            float oval = val.value;
            if (this.value < oval) {
                return -1;
            }
            if (this.value > oval) {
                return 1;
            }
            if (this.value == oval) {
                return 0;
            }
            return (this.value == this.value || oval == oval) ? 2 : 0;
        }

        public synchronized String toString() {
            int shift;
            if (this.canonical == null) {
                if (this.value == Float.POSITIVE_INFINITY) {
                    this.canonical = "INF";
                } else if (this.value == Float.NEGATIVE_INFINITY) {
                    this.canonical = "-INF";
                } else if (this.value != this.value) {
                    this.canonical = "NaN";
                } else if (this.value == 0.0f) {
                    this.canonical = "0.0E1";
                } else {
                    this.canonical = Float.toString(this.value);
                    if (this.canonical.indexOf(69) == -1) {
                        int len = this.canonical.length();
                        char[] chars = new char[len + 3];
                        this.canonical.getChars(0, len, chars, 0);
                        int edp = chars[0] == '-' ? 2 : 1;
                        if (this.value >= 1.0f || this.value <= -1.0f) {
                            int dp = this.canonical.indexOf(46);
                            for (int i = dp; i > edp; i--) {
                                chars[i] = chars[i - 1];
                            }
                            chars[edp] = '.';
                            while (chars[len - 1] == '0') {
                                len--;
                            }
                            if (chars[len - 1] == '.') {
                                len++;
                            }
                            int len2 = len + 1;
                            chars[len] = 'E';
                            shift = len2 + 1;
                            chars[len2] = (char) ((dp - edp) + 48);
                        } else {
                            int nzp = edp + 1;
                            while (chars[nzp] == '0') {
                                nzp++;
                            }
                            chars[edp - 1] = chars[nzp];
                            chars[edp] = '.';
                            int i2 = nzp + 1;
                            int j = edp + 1;
                            while (i2 < len) {
                                chars[j] = chars[i2];
                                i2++;
                                j++;
                            }
                            int i3 = nzp - edp;
                            int len3 = len - i3;
                            if (len3 == edp + 1) {
                                chars[len3] = '0';
                                len3++;
                            }
                            int len4 = len3 + 1;
                            chars[len3] = 'E';
                            int len5 = len4 + 1;
                            chars[len4] = '-';
                            int shift2 = nzp - edp;
                            chars[len5] = (char) (shift2 + 48);
                            shift = len5 + 1;
                        }
                        this.canonical = new String(chars, 0, shift);
                    }
                }
            }
            return this.canonical;
        }

        @Override
        public float getValue() {
            return this.value;
        }
    }
}
