package gov.nist.core;

import java.util.Map;

public class NameValue extends GenericObject implements Map.Entry<String, String> {
    private static final long serialVersionUID = -1857729012596437950L;
    protected final boolean isFlagParameter;
    protected boolean isQuotedString;
    private String name;
    private String quotes;
    private String separator;
    private Object value;

    public NameValue() {
        this.name = null;
        this.value = "";
        this.separator = Separators.EQUALS;
        this.quotes = "";
        this.isFlagParameter = false;
    }

    public NameValue(String str, Object obj, boolean z) {
        this.name = str;
        this.value = obj;
        this.separator = Separators.EQUALS;
        this.quotes = "";
        this.isFlagParameter = z;
    }

    public NameValue(String str, Object obj) {
        this(str, obj, false);
    }

    public void setSeparator(String str) {
        this.separator = str;
    }

    public void setQuotedValue() {
        this.isQuotedString = true;
        this.quotes = Separators.DOUBLE_QUOTE;
    }

    public boolean isValueQuoted() {
        return this.isQuotedString;
    }

    public String getName() {
        return this.name;
    }

    public Object getValueAsObject() {
        return this.isFlagParameter ? "" : this.value;
    }

    public void setName(String str) {
        this.name = str;
    }

    public void setValueAsObject(Object obj) {
        this.value = obj;
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        if (this.name != null && this.value != null && !this.isFlagParameter) {
            if (GenericObject.isMySubclass(this.value.getClass())) {
                GenericObject genericObject = (GenericObject) this.value;
                stringBuffer.append(this.name);
                stringBuffer.append(this.separator);
                stringBuffer.append(this.quotes);
                genericObject.encode(stringBuffer);
                stringBuffer.append(this.quotes);
                return stringBuffer;
            }
            if (GenericObjectList.isMySubclass(this.value.getClass())) {
                GenericObjectList genericObjectList = (GenericObjectList) this.value;
                stringBuffer.append(this.name);
                stringBuffer.append(this.separator);
                stringBuffer.append(genericObjectList.encode());
                return stringBuffer;
            }
            if (this.value.toString().length() == 0) {
                if (this.isQuotedString) {
                    stringBuffer.append(this.name);
                    stringBuffer.append(this.separator);
                    stringBuffer.append(this.quotes);
                    stringBuffer.append(this.quotes);
                    return stringBuffer;
                }
                stringBuffer.append(this.name);
                stringBuffer.append(this.separator);
                return stringBuffer;
            }
            stringBuffer.append(this.name);
            stringBuffer.append(this.separator);
            stringBuffer.append(this.quotes);
            stringBuffer.append(this.value.toString());
            stringBuffer.append(this.quotes);
            return stringBuffer;
        }
        if (this.name == null && this.value != null) {
            if (GenericObject.isMySubclass(this.value.getClass())) {
                ((GenericObject) this.value).encode(stringBuffer);
                return stringBuffer;
            }
            if (GenericObjectList.isMySubclass(this.value.getClass())) {
                stringBuffer.append(((GenericObjectList) this.value).encode());
                return stringBuffer;
            }
            stringBuffer.append(this.quotes);
            stringBuffer.append(this.value.toString());
            stringBuffer.append(this.quotes);
            return stringBuffer;
        }
        if (this.name != null && (this.value == null || this.isFlagParameter)) {
            stringBuffer.append(this.name);
            return stringBuffer;
        }
        return stringBuffer;
    }

    @Override
    public Object clone() {
        NameValue nameValue = (NameValue) super.clone();
        if (this.value != null) {
            nameValue.value = makeClone(this.value);
        }
        return nameValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !obj.getClass().equals(getClass())) {
            return false;
        }
        NameValue nameValue = (NameValue) obj;
        if (this == nameValue) {
            return true;
        }
        if ((this.name == null && nameValue.name != null) || (this.name != null && nameValue.name == null)) {
            return false;
        }
        if (this.name != null && nameValue.name != null && this.name.compareToIgnoreCase(nameValue.name) != 0) {
            return false;
        }
        if ((this.value != null && nameValue.value == null) || (this.value == null && nameValue.value != null)) {
            return false;
        }
        if (this.value == nameValue.value) {
            return true;
        }
        if (this.value instanceof String) {
            if (this.isQuotedString) {
                return this.value.equals(nameValue.value);
            }
            if (((String) this.value).compareToIgnoreCase((String) nameValue.value) != 0) {
                return false;
            }
            return true;
        }
        return this.value.equals(nameValue.value);
    }

    @Override
    public String getKey() {
        return this.name;
    }

    @Override
    public String getValue() {
        if (this.value == null) {
            return null;
        }
        return this.value.toString();
    }

    @Override
    public String setValue(String str) {
        String str2 = this.value == null ? null : str;
        this.value = str;
        return str2;
    }

    @Override
    public int hashCode() {
        return encode().toLowerCase().hashCode();
    }
}
