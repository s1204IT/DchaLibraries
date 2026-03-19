package libcore.io;

public final class Libcore {
    public static Os rawOs = new Linux();
    public static Os os = new BlockGuardOs(rawOs);

    private Libcore() {
    }
}
