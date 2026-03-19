package java.util.prefs;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EventObject;

public class PreferenceChangeEvent extends EventObject {
    private static final long serialVersionUID = 793724513368024975L;
    private String key;
    private String newValue;

    public PreferenceChangeEvent(Preferences preferences, String str, String str2) {
        super(preferences);
        this.key = str;
        this.newValue = str2;
    }

    public Preferences getNode() {
        return (Preferences) getSource();
    }

    public String getKey() {
        return this.key;
    }

    public String getNewValue() {
        return this.newValue;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws NotSerializableException {
        throw new NotSerializableException("Not serializable.");
    }

    private void readObject(ObjectInputStream objectInputStream) throws NotSerializableException {
        throw new NotSerializableException("Not serializable.");
    }
}
