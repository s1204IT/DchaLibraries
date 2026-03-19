package libcore.util;

public class SneakyThrow {
    public static void sneakyThrow(Throwable th) throws Throwable {
        sneakyThrow_(th);
    }

    private static <T extends Throwable> void sneakyThrow_(Throwable th) throws Throwable {
        throw th;
    }
}
