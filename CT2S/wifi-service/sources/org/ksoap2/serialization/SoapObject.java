package org.ksoap2.serialization;

import java.util.Hashtable;
import java.util.Vector;

public class SoapObject extends AttributeContainer implements KvmSerializable {
    private static final String EMPTY_STRING = "";
    protected String name;
    protected String namespace;
    protected Vector properties;

    public SoapObject() {
        this(EMPTY_STRING, EMPTY_STRING);
    }

    public SoapObject(String namespace, String name) {
        this.properties = new Vector();
        this.namespace = namespace;
        this.name = name;
    }

    public boolean equals(Object obj) {
        int numProperties;
        if (!(obj instanceof SoapObject)) {
            return false;
        }
        SoapObject otherSoapObject = (SoapObject) obj;
        if (!this.name.equals(otherSoapObject.name) || !this.namespace.equals(otherSoapObject.namespace) || (numProperties = this.properties.size()) != otherSoapObject.properties.size()) {
            return false;
        }
        for (int propIndex = 0; propIndex < numProperties; propIndex++) {
            Object thisProp = this.properties.elementAt(propIndex);
            if (!otherSoapObject.isPropertyEqual(thisProp, propIndex)) {
                return false;
            }
        }
        return attributesAreEqual(otherSoapObject);
    }

    public boolean isPropertyEqual(Object otherProp, int index) {
        if (index >= getPropertyCount()) {
            return false;
        }
        Object thisProp = this.properties.elementAt(index);
        if ((otherProp instanceof PropertyInfo) && (thisProp instanceof PropertyInfo)) {
            PropertyInfo otherPropInfo = (PropertyInfo) otherProp;
            PropertyInfo thisPropInfo = (PropertyInfo) thisProp;
            return otherPropInfo.getName().equals(thisPropInfo.getName()) && otherPropInfo.getValue().equals(thisPropInfo.getValue());
        }
        if (!(otherProp instanceof SoapObject) || !(thisProp instanceof SoapObject)) {
            return false;
        }
        SoapObject otherPropSoap = (SoapObject) otherProp;
        SoapObject thisPropSoap = (SoapObject) thisProp;
        return otherPropSoap.equals(thisPropSoap);
    }

    public String getName() {
        return this.name;
    }

    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public Object getProperty(int index) {
        Object prop = this.properties.elementAt(index);
        return prop instanceof PropertyInfo ? ((PropertyInfo) prop).getValue() : (SoapObject) prop;
    }

    public String getPropertyAsString(int index) {
        PropertyInfo propertyInfo = (PropertyInfo) this.properties.elementAt(index);
        return propertyInfo.getValue().toString();
    }

    public Object getProperty(String name) {
        Integer index = propertyIndex(name);
        if (index != null) {
            return getProperty(index.intValue());
        }
        throw new RuntimeException("illegal property: " + name);
    }

    public String getPropertyAsString(String name) {
        Integer index = propertyIndex(name);
        if (index != null) {
            return getProperty(index.intValue()).toString();
        }
        throw new RuntimeException("illegal property: " + name);
    }

    public boolean hasProperty(String name) {
        return propertyIndex(name) != null;
    }

    public Object getPropertySafely(String name) {
        Integer i = propertyIndex(name);
        return i != null ? getProperty(i.intValue()) : new NullSoapObject();
    }

    public String getPropertySafelyAsString(String name) {
        Object foo;
        Integer i = propertyIndex(name);
        if (i == null || (foo = getProperty(i.intValue())) == null) {
            return EMPTY_STRING;
        }
        return foo.toString();
    }

    public Object getPropertySafely(String name, Object defaultThing) {
        Integer i = propertyIndex(name);
        if (i != null) {
            Object defaultThing2 = getProperty(i.intValue());
            return defaultThing2;
        }
        return defaultThing;
    }

    public String getPropertySafelyAsString(String name, Object defaultThing) {
        Integer i = propertyIndex(name);
        if (i != null) {
            Object property = getProperty(i.intValue());
            if (property != null) {
                return property.toString();
            }
            return EMPTY_STRING;
        }
        if (defaultThing != null) {
            return defaultThing.toString();
        }
        return EMPTY_STRING;
    }

    public Object getPrimitiveProperty(String name) {
        Integer index = propertyIndex(name);
        if (index != null) {
            PropertyInfo propertyInfo = (PropertyInfo) this.properties.elementAt(index.intValue());
            if (propertyInfo.getType() != SoapObject.class) {
                return propertyInfo.getValue();
            }
            PropertyInfo propertyInfo2 = new PropertyInfo();
            propertyInfo2.setType(String.class);
            propertyInfo2.setValue(EMPTY_STRING);
            propertyInfo2.setName(name);
            return propertyInfo2.getValue();
        }
        throw new RuntimeException("illegal property: " + name);
    }

    public String getPrimitivePropertyAsString(String name) {
        Integer index = propertyIndex(name);
        if (index != null) {
            PropertyInfo propertyInfo = (PropertyInfo) this.properties.elementAt(index.intValue());
            return propertyInfo.getType() != SoapObject.class ? propertyInfo.getValue().toString() : EMPTY_STRING;
        }
        throw new RuntimeException("illegal property: " + name);
    }

    public Object getPrimitivePropertySafely(String name) {
        Integer index = propertyIndex(name);
        if (index != null) {
            PropertyInfo propertyInfo = (PropertyInfo) this.properties.elementAt(index.intValue());
            if (propertyInfo.getType() != SoapObject.class) {
                return propertyInfo.getValue().toString();
            }
            PropertyInfo propertyInfo2 = new PropertyInfo();
            propertyInfo2.setType(String.class);
            propertyInfo2.setValue(EMPTY_STRING);
            propertyInfo2.setName(name);
            return propertyInfo2.getValue();
        }
        return new NullSoapObject();
    }

    public String getPrimitivePropertySafelyAsString(String name) {
        Integer index = propertyIndex(name);
        if (index != null) {
            PropertyInfo propertyInfo = (PropertyInfo) this.properties.elementAt(index.intValue());
            if (propertyInfo.getType() != SoapObject.class) {
                return propertyInfo.getValue().toString();
            }
            return EMPTY_STRING;
        }
        return EMPTY_STRING;
    }

    private Integer propertyIndex(String name) {
        if (name != null) {
            for (int i = 0; i < this.properties.size(); i++) {
                if (name.equals(((PropertyInfo) this.properties.elementAt(i)).getName())) {
                    return new Integer(i);
                }
            }
        }
        return null;
    }

    @Override
    public int getPropertyCount() {
        return this.properties.size();
    }

    @Override
    public void getPropertyInfo(int index, Hashtable properties, PropertyInfo propertyInfo) {
        getPropertyInfo(index, propertyInfo);
    }

    public void getPropertyInfo(int index, PropertyInfo propertyInfo) {
        Object element = this.properties.elementAt(index);
        if (element instanceof PropertyInfo) {
            PropertyInfo p = (PropertyInfo) element;
            propertyInfo.name = p.name;
            propertyInfo.namespace = p.namespace;
            propertyInfo.flags = p.flags;
            propertyInfo.type = p.type;
            propertyInfo.elementType = p.elementType;
            propertyInfo.value = p.value;
            propertyInfo.multiRef = p.multiRef;
            return;
        }
        propertyInfo.name = null;
        propertyInfo.namespace = null;
        propertyInfo.flags = 0;
        propertyInfo.type = null;
        propertyInfo.elementType = null;
        propertyInfo.value = element;
        propertyInfo.multiRef = false;
    }

    public SoapObject newInstance() {
        SoapObject o = new SoapObject(this.namespace, this.name);
        for (int propIndex = 0; propIndex < this.properties.size(); propIndex++) {
            Object prop = this.properties.elementAt(propIndex);
            if (prop instanceof PropertyInfo) {
                PropertyInfo propertyInfo = (PropertyInfo) this.properties.elementAt(propIndex);
                PropertyInfo propertyInfoClonned = (PropertyInfo) propertyInfo.clone();
                o.addProperty(propertyInfoClonned);
            } else if (prop instanceof SoapObject) {
                o.addSoapObject(((SoapObject) prop).newInstance());
            }
        }
        for (int attribIndex = 0; attribIndex < getAttributeCount(); attribIndex++) {
            AttributeInfo newAI = new AttributeInfo();
            getAttributeInfo(attribIndex, newAI);
            o.addAttribute(newAI);
        }
        return o;
    }

    public void setProperty(int index, Object value) {
        Object prop = this.properties.elementAt(index);
        if (prop instanceof PropertyInfo) {
            ((PropertyInfo) prop).setValue(value);
        }
    }

    public SoapObject addProperty(String name, Object value) {
        PropertyInfo propertyInfo = new PropertyInfo();
        propertyInfo.name = name;
        propertyInfo.type = value == null ? PropertyInfo.OBJECT_CLASS : value.getClass();
        propertyInfo.value = value;
        propertyInfo.namespace = this.namespace;
        return addProperty(propertyInfo);
    }

    public SoapObject addPropertyIfValue(String name, Object value) {
        if (value != null) {
            return addProperty(name, value);
        }
        return this;
    }

    public SoapObject addPropertyIfValue(PropertyInfo propertyInfo, Object value) {
        if (value != null) {
            propertyInfo.setValue(value);
            return addProperty(propertyInfo);
        }
        return this;
    }

    public SoapObject addProperty(PropertyInfo propertyInfo) {
        this.properties.addElement(propertyInfo);
        return this;
    }

    public SoapObject addPropertyIfValue(PropertyInfo propertyInfo) {
        if (propertyInfo.value != null) {
            this.properties.addElement(propertyInfo);
        }
        return this;
    }

    public SoapObject addSoapObject(SoapObject soapObject) {
        this.properties.addElement(soapObject);
        return this;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(EMPTY_STRING + this.name + "{");
        for (int i = 0; i < getPropertyCount(); i++) {
            Object prop = this.properties.elementAt(i);
            if (prop instanceof PropertyInfo) {
                buf.append(EMPTY_STRING).append(((PropertyInfo) prop).getName()).append("=").append(getProperty(i)).append("; ");
            } else {
                buf.append(((SoapObject) prop).toString());
            }
        }
        buf.append("}");
        return buf.toString();
    }
}
