package java.text;

public class Annotation {
    private Object value;

    public Annotation(Object attribute) {
        this.value = attribute;
    }

    public Object getValue() {
        return this.value;
    }

    public String toString() {
        return getClass().getName() + "[value=" + this.value + ']';
    }
}
