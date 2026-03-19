package sun.util.locale;

class Extension {
    private String id;
    private final char key;
    private String value;

    protected Extension(char c) {
        this.key = c;
    }

    Extension(char c, String str) {
        this.key = c;
        setValue(str);
    }

    protected void setValue(String str) {
        this.value = str;
        this.id = this.key + LanguageTag.SEP + str;
    }

    public char getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }

    public String getID() {
        return this.id;
    }

    public String toString() {
        return getID();
    }
}
