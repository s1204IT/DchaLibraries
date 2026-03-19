package java.time.format;

public enum SignStyle {
    NORMAL,
    ALWAYS,
    NEVER,
    NOT_NEGATIVE,
    EXCEEDS_PAD;

    boolean parse(boolean z, boolean z2, boolean z3) {
        int iOrdinal = ordinal();
        if (iOrdinal != 4) {
            switch (iOrdinal) {
                case 0:
                    if (!z || !z2) {
                    }
                    break;
                case 1:
                    break;
                default:
                    if (!z2 && !z3) {
                        break;
                    }
                    break;
            }
            return true;
        }
        return true;
    }
}
