package org.ksoap2.serialization;

public class SoapPrimitive extends AttributeContainer {
    String name;
    String namespace;
    String value;

    public SoapPrimitive(String namespace, String name, String value) {
        this.namespace = namespace;
        this.name = name;
        this.value = value;
    }

    public boolean equals(Object o) {
        if (!(o instanceof SoapPrimitive)) {
            return false;
        }
        SoapPrimitive p = (SoapPrimitive) o;
        boolean varsEqual = this.name.equals(p.name) && (this.namespace != null ? this.namespace.equals(p.namespace) : p.namespace == null) && (this.value != null ? this.value.equals(p.value) : p.value == null);
        return varsEqual && attributesAreEqual(p);
    }

    public int hashCode() {
        return (this.namespace == null ? 0 : this.namespace.hashCode()) ^ this.name.hashCode();
    }

    public String toString() {
        return this.value;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getName() {
        return this.name;
    }
}
