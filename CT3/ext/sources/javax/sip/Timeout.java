package javax.sip;

public enum Timeout {
    RETRANSMIT,
    TRANSACTION;

    public static Timeout[] valuesCustom() {
        return values();
    }
}
