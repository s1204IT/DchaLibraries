package java.util.prefs;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EventObject;

public class PreferenceChangeEvent extends EventObject implements Serializable {
    private static final long serialVersionUID = 793724513368024975L;
    private final String key;
    private final Preferences node;
    private final String value;

    public PreferenceChangeEvent(Preferences p, String k, String v) {
        super(p);
        this.node = p;
        this.key = k;
        this.value = v;
    }

    public String getKey() {
        return this.key;
    }

    public String getNewValue() {
        return this.value;
    }

    public Preferences getNode() {
        return this.node;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException();
    }

    private void readObject(ObjectInputStream in) throws IOException {
        throw new NotSerializableException();
    }
}
