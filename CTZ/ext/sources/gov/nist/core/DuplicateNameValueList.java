package gov.nist.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class DuplicateNameValueList implements Serializable, Cloneable {
    private static final long serialVersionUID = -5611332957903796952L;
    private MultiValueMapImpl<NameValue> nameValueMap = new MultiValueMapImpl<>();
    private String separator = Separators.SEMICOLON;

    public void setSeparator(String str) {
        this.separator = str;
    }

    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    public StringBuffer encode(StringBuffer stringBuffer) {
        if (!this.nameValueMap.isEmpty()) {
            Iterator it = this.nameValueMap.values().iterator();
            if (it.hasNext()) {
                while (true) {
                    Object next = it.next();
                    if (next instanceof GenericObject) {
                        ((GenericObject) next).encode(stringBuffer);
                    } else {
                        stringBuffer.append(next.toString());
                    }
                    if (!it.hasNext()) {
                        break;
                    }
                    stringBuffer.append(this.separator);
                }
            }
        }
        return stringBuffer;
    }

    public String toString() {
        return encode();
    }

    public void set(NameValue nameValue) {
        this.nameValueMap.put(nameValue.getName().toLowerCase(), nameValue);
    }

    public void set(String str, Object obj) {
        this.nameValueMap.put(str.toLowerCase(), new NameValue(str, obj));
    }

    public boolean equals(Object obj) {
        if (obj == null || !obj.getClass().equals(getClass())) {
            return false;
        }
        DuplicateNameValueList duplicateNameValueList = (DuplicateNameValueList) obj;
        if (this.nameValueMap.size() != duplicateNameValueList.nameValueMap.size()) {
            return false;
        }
        for (String str : this.nameValueMap.keySet()) {
            Collection nameValue = getNameValue(str);
            List<NameValue> list = duplicateNameValueList.nameValueMap.get((Object) str);
            if (list == null || !list.equals(nameValue)) {
                return false;
            }
        }
        return true;
    }

    public Object getValue(String str) {
        Collection nameValue = getNameValue(str.toLowerCase());
        if (nameValue != null) {
            return nameValue;
        }
        return null;
    }

    public Collection getNameValue(String str) {
        return this.nameValueMap.get((Object) str.toLowerCase());
    }

    public boolean hasNameValue(String str) {
        return this.nameValueMap.containsKey(str.toLowerCase());
    }

    public boolean delete(String str) {
        String lowerCase = str.toLowerCase();
        if (this.nameValueMap.containsKey(lowerCase)) {
            this.nameValueMap.remove((Object) lowerCase);
            return true;
        }
        return false;
    }

    public Object clone() {
        DuplicateNameValueList duplicateNameValueList = new DuplicateNameValueList();
        duplicateNameValueList.setSeparator(this.separator);
        Iterator it = this.nameValueMap.values().iterator();
        while (it.hasNext()) {
            duplicateNameValueList.set((NameValue) ((NameValue) it.next()).clone());
        }
        return duplicateNameValueList;
    }

    public Iterator<NameValue> iterator() {
        return this.nameValueMap.values().iterator();
    }

    public Iterator<String> getNames() {
        return this.nameValueMap.keySet().iterator();
    }

    public String getParameter(String str) {
        Object value = getValue(str);
        if (value == null) {
            return null;
        }
        if (value instanceof GenericObject) {
            return ((GenericObject) value).encode();
        }
        return value.toString();
    }

    public void clear() {
        this.nameValueMap.clear();
    }

    public boolean isEmpty() {
        return this.nameValueMap.isEmpty();
    }

    public NameValue put(String str, NameValue nameValue) {
        return (NameValue) this.nameValueMap.put(str, nameValue);
    }

    public NameValue remove(Object obj) {
        return (NameValue) this.nameValueMap.remove(obj);
    }

    public int size() {
        return this.nameValueMap.size();
    }

    public Collection<NameValue> values() {
        return this.nameValueMap.values();
    }

    public int hashCode() {
        return this.nameValueMap.keySet().hashCode();
    }
}
