package java.text;

final class EntryPair {
    public String entryName;
    public boolean fwd;
    public int value;

    public EntryPair(String str, int i) {
        this(str, i, true);
    }

    public EntryPair(String str, int i, boolean z) {
        this.entryName = str;
        this.value = i;
        this.fwd = z;
    }
}
