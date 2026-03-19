package javax.sip;

public enum TransactionState {
    CALLING,
    TRYING,
    PROCEEDING,
    COMPLETED,
    CONFIRMED,
    TERMINATED;

    public static TransactionState[] valuesCustom() {
        return values();
    }
}
