package java.util.prefs;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EventObject;

public class NodeChangeEvent extends EventObject implements Serializable {
    private static final long serialVersionUID = 8068949086596572957L;
    private final Preferences child;
    private final Preferences parent;

    public NodeChangeEvent(Preferences p, Preferences c) {
        super(p);
        this.parent = p;
        this.child = c;
    }

    public Preferences getParent() {
        return this.parent;
    }

    public Preferences getChild() {
        return this.child;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        throw new NotSerializableException();
    }
}
