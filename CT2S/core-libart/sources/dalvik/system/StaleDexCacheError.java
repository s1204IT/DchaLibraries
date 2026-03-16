package dalvik.system;

public class StaleDexCacheError extends VirtualMachineError {
    public StaleDexCacheError() {
    }

    public StaleDexCacheError(String detailMessage) {
        super(detailMessage);
    }
}
