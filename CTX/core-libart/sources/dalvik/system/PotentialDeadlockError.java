package dalvik.system;

public class PotentialDeadlockError extends VirtualMachineError {
    public PotentialDeadlockError() {
    }

    public PotentialDeadlockError(String str) {
        super(str);
    }
}
