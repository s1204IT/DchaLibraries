package java.text;

import java.text.Format;

public class FieldPosition {
    private Format.Field attribute;
    int beginIndex;
    int endIndex;
    int field;

    public FieldPosition(int i) {
        this.field = 0;
        this.endIndex = 0;
        this.beginIndex = 0;
        this.field = i;
    }

    public FieldPosition(Format.Field field) {
        this(field, -1);
    }

    public FieldPosition(Format.Field field, int i) {
        this.field = 0;
        this.endIndex = 0;
        this.beginIndex = 0;
        this.attribute = field;
        this.field = i;
    }

    public Format.Field getFieldAttribute() {
        return this.attribute;
    }

    public int getField() {
        return this.field;
    }

    public int getBeginIndex() {
        return this.beginIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public void setBeginIndex(int i) {
        this.beginIndex = i;
    }

    public void setEndIndex(int i) {
        this.endIndex = i;
    }

    Format.FieldDelegate getFieldDelegate() {
        return new Delegate();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FieldPosition)) {
            return false;
        }
        FieldPosition fieldPosition = (FieldPosition) obj;
        if (this.attribute == null) {
            if (fieldPosition.attribute != null) {
                return false;
            }
        } else if (!this.attribute.equals(fieldPosition.attribute)) {
            return false;
        }
        return this.beginIndex == fieldPosition.beginIndex && this.endIndex == fieldPosition.endIndex && this.field == fieldPosition.field;
    }

    public int hashCode() {
        return (this.field << 24) | (this.beginIndex << 16) | this.endIndex;
    }

    public String toString() {
        return getClass().getName() + "[field=" + this.field + ",attribute=" + ((Object) this.attribute) + ",beginIndex=" + this.beginIndex + ",endIndex=" + this.endIndex + ']';
    }

    private boolean matchesField(Format.Field field) {
        if (this.attribute != null) {
            return this.attribute.equals(field);
        }
        return false;
    }

    private boolean matchesField(Format.Field field, int i) {
        if (this.attribute != null) {
            return this.attribute.equals(field);
        }
        return i == this.field;
    }

    private class Delegate implements Format.FieldDelegate {
        private boolean encounteredField;

        private Delegate() {
        }

        @Override
        public void formatted(Format.Field field, Object obj, int i, int i2, StringBuffer stringBuffer) {
            if (!this.encounteredField && FieldPosition.this.matchesField(field)) {
                FieldPosition.this.setBeginIndex(i);
                FieldPosition.this.setEndIndex(i2);
                this.encounteredField = i != i2;
            }
        }

        @Override
        public void formatted(int i, Format.Field field, Object obj, int i2, int i3, StringBuffer stringBuffer) {
            if (!this.encounteredField && FieldPosition.this.matchesField(field, i)) {
                FieldPosition.this.setBeginIndex(i2);
                FieldPosition.this.setEndIndex(i3);
                this.encounteredField = i2 != i3;
            }
        }
    }
}
