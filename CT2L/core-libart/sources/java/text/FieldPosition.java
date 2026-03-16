package java.text;

import java.text.Format;

public class FieldPosition {
    private Format.Field attribute;
    private int beginIndex;
    private int endIndex;
    private int field;

    public FieldPosition(int field) {
        this.field = field;
    }

    public FieldPosition(Format.Field attribute) {
        this.attribute = attribute;
        this.field = -1;
    }

    public FieldPosition(Format.Field attribute, int field) {
        this.attribute = attribute;
        this.field = field;
    }

    public boolean equals(Object object) {
        if (!(object instanceof FieldPosition)) {
            return false;
        }
        FieldPosition pos = (FieldPosition) object;
        return this.field == pos.field && this.attribute == pos.attribute && this.beginIndex == pos.beginIndex && this.endIndex == pos.endIndex;
    }

    public int getBeginIndex() {
        return this.beginIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public int getField() {
        return this.field;
    }

    public Format.Field getFieldAttribute() {
        return this.attribute;
    }

    public int hashCode() {
        int attributeHash = this.attribute == null ? 0 : this.attribute.hashCode();
        return (this.field * 10) + attributeHash + (this.beginIndex * 100) + this.endIndex;
    }

    public void setBeginIndex(int index) {
        this.beginIndex = index;
    }

    public void setEndIndex(int index) {
        this.endIndex = index;
    }

    public String toString() {
        return getClass().getName() + "[attribute=" + this.attribute + ",field=" + this.field + ",beginIndex=" + this.beginIndex + ",endIndex=" + this.endIndex + "]";
    }
}
