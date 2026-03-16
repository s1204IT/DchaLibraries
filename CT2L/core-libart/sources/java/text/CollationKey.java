package java.text;

public abstract class CollationKey implements Comparable<CollationKey> {
    private final String source;

    @Override
    public abstract int compareTo(CollationKey collationKey);

    public abstract byte[] toByteArray();

    protected CollationKey(String source) {
        this.source = source;
    }

    public String getSourceString() {
        return this.source;
    }
}
