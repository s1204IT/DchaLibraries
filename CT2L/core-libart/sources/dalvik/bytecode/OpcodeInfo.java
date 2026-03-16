package dalvik.bytecode;

public final class OpcodeInfo {
    public static final int MAXIMUM_VALUE = 65535;
    public static final int MAXIMUM_PACKED_VALUE = Opcodes.OP_CONST_CLASS_JUMBO;

    public static boolean isInvoke(int packedOpcode) {
        return false;
    }

    private OpcodeInfo() {
    }
}
