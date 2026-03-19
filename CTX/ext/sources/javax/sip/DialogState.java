package javax.sip;

public enum DialogState {
    EARLY,
    CONFIRMED,
    TERMINATED;

    public static final int _EARLY = EARLY.ordinal();
    public static final int _CONFIRMED = CONFIRMED.ordinal();
    public static final int _TERMINATED = TERMINATED.ordinal();

    public static DialogState getObject(int i) {
        try {
            return values()[i];
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid dialog state: " + i);
        }
    }

    public int getValue() {
        return ordinal();
    }
}
