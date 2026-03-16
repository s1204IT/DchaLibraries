package java.security;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class GuardedObject implements Serializable {
    private static final long serialVersionUID = -5240450096227834308L;
    private final Guard guard;
    private final Object object;

    public GuardedObject(Object object, Guard guard) {
        this.object = object;
        this.guard = guard;
    }

    public Object getObject() throws SecurityException {
        if (this.guard != null) {
            this.guard.checkGuard(this.object);
        }
        return this.object;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (this.guard != null) {
            this.guard.checkGuard(this.object);
        }
        out.defaultWriteObject();
    }
}
