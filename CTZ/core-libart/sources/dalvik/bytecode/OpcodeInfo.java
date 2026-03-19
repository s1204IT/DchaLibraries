package dalvik.bytecode;

import android.icu.text.DateTimePatternGenerator;

public final class OpcodeInfo {
    public static final int MAXIMUM_VALUE = DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
    public static final int MAXIMUM_PACKED_VALUE = 255;

    public static boolean isInvoke(int i) {
        return false;
    }

    private OpcodeInfo() {
    }
}
