package javax.annotation.meta;

public enum When {
    ALWAYS,
    UNKNOWN,
    MAYBE,
    NEVER;

    public static When[] valuesCustom() {
        return values();
    }
}
