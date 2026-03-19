package java.text;

import java.text.AttributedCharacterIterator;
import java.util.Map;

class AttributeEntry implements Map.Entry<AttributedCharacterIterator.Attribute, Object> {
    private AttributedCharacterIterator.Attribute key;
    private Object value;

    AttributeEntry(AttributedCharacterIterator.Attribute attribute, Object obj) {
        this.key = attribute;
        this.value = obj;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AttributeEntry)) {
            return false;
        }
        AttributeEntry attributeEntry = (AttributeEntry) obj;
        if (!attributeEntry.key.equals(this.key)) {
            return false;
        }
        if (this.value == null) {
            if (attributeEntry.value != null) {
                return false;
            }
        } else if (!attributeEntry.value.equals(this.value)) {
            return false;
        }
        return true;
    }

    @Override
    public AttributedCharacterIterator.Attribute getKey() {
        return this.key;
    }

    @Override
    public Object getValue() {
        return this.value;
    }

    @Override
    public Object setValue(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return this.key.hashCode() ^ (this.value == null ? 0 : this.value.hashCode());
    }

    public String toString() {
        return this.key.toString() + "=" + this.value.toString();
    }
}
