package sun.util.locale;

public class LocaleSyntaxException extends Exception {
    private static final long serialVersionUID = 1;
    private int index;

    public LocaleSyntaxException(String str) {
        this(str, 0);
    }

    public LocaleSyntaxException(String str, int i) {
        super(str);
        this.index = -1;
        this.index = i;
    }

    public int getErrorIndex() {
        return this.index;
    }
}
