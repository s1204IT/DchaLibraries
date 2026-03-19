package dalvik.system;

public class AllocationLimitError extends VirtualMachineError {
    public AllocationLimitError() {
    }

    public AllocationLimitError(String str) {
        super(str);
    }
}
